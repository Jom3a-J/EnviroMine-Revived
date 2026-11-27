package enviromine_revived;

import java.util.random.RandomGenerator;

import enviromine_revived.config.Settings;
import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.WaterType;
import enviromine_revived.network.DrinkPayload;
import enviromine_revived.registry.ModComponents;
import enviromine_revived.registry.ModEffects;
import enviromine_revived.world.WaterSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Everything about drinking and filling bottles. All changes happen on the server; client-side
 * handlers only cancel vanilla's prediction so the two sides agree.
 *
 * <ul>
 *   <li>Empty hand + water: a sip (the client sends {@link DrinkPayload}).</li>
 *   <li>Empty hand + water cauldron: a sip, lowers the cauldron by one level.</li>
 *   <li>Glass bottle + water or cauldron: a water bottle marked with its water type
 *       (clean water is left to vanilla, which gives an ordinary bottle).</li>
 *   <li>Drinking a water bottle: hydration by its water type.</li>
 * </ul>
 */
public final class Drinking {
    private Drinking() {}

    /** Ticks between sips, so holding right-click does not drain a lake in a second. */
    public static final int SIP_COOLDOWN_TICKS = 10;

    // Effect durations from the original, except poison (10 s there, 5 s here).
    private static final int HUNGER_TICKS_SIP = 10 * 20;
    private static final int HUNGER_TICKS_BOTTLE = 30 * 20;
    private static final int POISON_TICKS = 5 * 20;

    // ------------------------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------------------------

    /** Glass bottle used on a water source (vanilla BottleItem.use, but marking the water type). */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return;
        }
        Player player = event.getEntity();
        Level level = player.level();
        // Collecting dragon's breath takes priority, as in vanilla.
        boolean dragonBreath = !level.getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox().inflate(2.0),
                cloud -> cloud.isAlive() && cloud.getOwner() instanceof EnderDragon).isEmpty();
        if (dragonBreath) {
            return;
        }
        BlockHitResult hit = WaterSources.lookTarget(player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
            return;
        }
        WaterType type = WaterSources.ofWater(level, pos);
        if (type == WaterType.CLEAN) {
            return; // vanilla gives exactly the right bottle
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!level.isClientSide()) {
            fillBottle(player, event.getHand(), stack, type);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
    }

    /** Water cauldron: fill a bottle, or drink with an empty hand. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.is(Items.GLASS_BOTTLE)) {
            WaterType type = WaterSources.ofCauldron(level, pos);
            if (type == WaterType.CLEAN) {
                return; // vanilla cauldron interaction
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            if (!level.isClientSide()) {
                fillBottle(player, event.getHand(), stack, type);
                player.awardStat(Stats.USE_CAULDRON);
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
        } else if (stack.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND && canDrinkByHand(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            if (player instanceof ServerPlayer serverPlayer && offCooldown(serverPlayer)) {
                sip(serverPlayer, WaterSources.ofCauldron(level, pos));
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            }
        }
    }

    /** Finished drinking a water bottle. */
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack drunk = event.getItem(); // a copy from before it was used
        PotionContents contents = drunk.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (!drunk.is(Items.POTION) || contents == null || !contents.is(Potions.WATER)) {
            return;
        }
        WaterType type = drunk.getOrDefault(ModComponents.WATER_TYPE.get(), WaterType.CLEAN);
        drink(player, type, true);
    }

    /** The client says the player right-clicked water with an empty hand. */
    public static void onDrinkPayload(DrinkPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || player.isEyeInFluid(FluidTags.WATER)
                || !canDrinkByHand(player) || !offCooldown(player)) {
            return;
        }
        BlockHitResult hit = WaterSources.lookTarget(player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Level level = player.level();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
            return;
        }
        sip(player, WaterSources.ofWater(level, pos));
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    // ------------------------------------------------------------------------------------------

    /** Hydration is on and not already full. Checked on both sides, so it only uses synced data. */
    public static boolean wantsToDrink(Player player) {
        Settings.Snapshot settings = Settings.get();
        return settings.features().hydration()
                && settings.hydration().canDrink(player.getData(ModAttachments.CLIMATE).hydration());
    }

    /** Drinking straight from water or a cauldron is allowed by the server config. */
    public static boolean canDrinkByHand(Player player) {
        return Settings.get().features().drinkByHand() && wantsToDrink(player);
    }

    private static boolean offCooldown(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now < player.getData(ModAttachments.DRINK_COOLDOWN)) {
            return false;
        }
        player.setData(ModAttachments.DRINK_COOLDOWN, now + SIP_COOLDOWN_TICKS);
        return true;
    }

    private static void sip(ServerPlayer player, WaterType type) {
        drink(player, type, false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /** Apply one drink: hydration, cooling and any side effects of the water. */
    public static void drink(ServerPlayer player, WaterType type, boolean fromBottle) {
        Settings.Snapshot settings = Settings.get();
        if (!settings.features().hydration()) {
            return;
        }
        ClimateData data = player.getData(ModAttachments.CLIMATE);
        RandomGenerator random = player.getRandom()::nextLong;
        HydrationModel.Drink result = settings.hydration().drink(
                data.hydration(), data.bodyTemp(), settings.water(type), fromBottle, random);
        player.setData(ModAttachments.CLIMATE,
                new ClimateData(data.bodyTemp() + result.bodyTempChange(), result.hydration(), data.ambient()));

        if (result.hunger()) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, fromBottle ? HUNGER_TICKS_BOTTLE : HUNGER_TICKS_SIP));
        }
        if (result.poison()) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS));
        }
        if (result.thirstSeconds() > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.THIRST, result.thirstSeconds() * 20));
        }
    }

    private static void fillBottle(Player player, InteractionHand hand, ItemStack bottles, WaterType type) {
        ItemStack filled = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        filled.set(ModComponents.WATER_TYPE.get(), type);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
        player.setItemInHand(hand, ItemUtils.createFilledResult(bottles, player, filled));
    }
}
