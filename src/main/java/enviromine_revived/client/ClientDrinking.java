package enviromine_revived.client;

import enviromine_revived.Drinking;
import enviromine_revived.model.WaterType;
import enviromine_revived.network.DrinkPayload;
import enviromine_revived.registry.ModComponents;
import enviromine_revived.world.WaterSources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of drinking: noticing clicks on water, and the water-type tooltip. */
public final class ClientDrinking {
    private ClientDrinking() {}

    /** Right-click with an empty hand on nothing solid: water is not a "block" to the game's click. */
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            tryDrink(event.getEntity());
        }
    }

    /** Right-click on a block (e.g. a riverbed) with water in front of it. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()) {
            return;
        }
        BlockHitResult water = waterInView(event.getEntity());
        if (water != null && !water.getBlockPos().equals(event.getPos())) {
            PacketDistributor.sendToServer(DrinkPayload.INSTANCE);
            event.setCanceled(true);
        }
    }

    private static void tryDrink(Player player) {
        if (player.getMainHandItem().isEmpty() && waterInView(player) != null) {
            PacketDistributor.sendToServer(DrinkPayload.INSTANCE);
        }
    }

    /** The water the player is looking at, if it is the first thing in view and they are thirsty. */
    private static BlockHitResult waterInView(Player player) {
        if (player.isEyeInFluid(FluidTags.WATER) || !Drinking.canDrinkByHand(player)) {
            return null;
        }
        BlockHitResult hit = WaterSources.lookTarget(player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.BLOCK && player.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return hit;
        }
        return null;
    }

    /** "Dirty water" etc. under a marked water bottle's name. */
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        WaterType type = stack.get(ModComponents.WATER_TYPE.get());
        if (type == null || type == WaterType.CLEAN) {
            return;
        }
        ChatFormatting colour = switch (type) {
            case DIRTY -> ChatFormatting.GOLD;
            case SALTY -> ChatFormatting.GRAY;
            case COLD -> ChatFormatting.AQUA;
            case CLEAN -> ChatFormatting.WHITE;
        };
        event.getToolTip().add(1, Component.translatable(
                "item.enviromine_revived.water_type." + type.name().toLowerCase(java.util.Locale.ROOT)).withStyle(colour));
    }
}
