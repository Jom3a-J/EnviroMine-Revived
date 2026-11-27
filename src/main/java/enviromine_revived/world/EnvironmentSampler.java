package enviromine_revived.world;

import java.util.ArrayList;
import java.util.List;

import enviromine_revived.model.ArmorModifier;
import enviromine_revived.model.Environment;
import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.TemperatureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

/**
 * Reads the world around a player and turns it into the plain values the model needs.
 *
 * <p>This is the only place where Minecraft and the model meet on the server. Keep game logic out
 * of here: it should only observe the world, and the model decides what it means.
 */
public final class EnvironmentSampler {
    private EnvironmentSampler() {}

    /** Half-size of the scanned cube (11×11×11 blocks), same as the original. */
    public static final int RANGE = 5;

    /** Body temperature assumed for nearby animals when they huddle with you. */
    private static final double ANIMAL_BODY_TEMP = 37.0;

    /** Both inputs the models need for one update. */
    public record Sample(Environment environment, HydrationModel.Activity activity) {}

    public static Sample sample(ServerPlayer player, TemperatureModel model) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        // Use the head block for sky checks, so a snow layer or carpet underfoot does not count as a roof.
        BlockPos head = BlockPos.containing(player.getEyePosition());

        DimensionType dimension = level.dimensionType();
        boolean daylightCycle = dimension.hasSkyLight() && !dimension.hasFixedTime();
        Biome biome = level.getBiome(feet).value();
        boolean arid = !biome.hasPrecipitation();
        boolean canSeeSky = level.canSeeSky(head);
        boolean isDay = level.isDay();

        ScanResult blocks = scanBlocks(level, player);

        boolean animalsNearby = !level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(2.5)).isEmpty();

        Holder<Enchantment> fireProtection = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);
        List<ArmorModifier> armor = new ArrayList<>(4);
        int fireProtectionLevels = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                armor.add(DefaultModifiers.armor(armorItem.getMaterial()));
            }
            fireProtectionLevels += EnchantmentHelper.getItemEnchantmentLevel(fireProtection, stack);
        }

        Environment environment = Environment.builder()
                .biomeAmbient(averageBiomeAmbient(level, feet, model))
                .arid(arid)
                .hasDaylightCycle(daylightCycle)
                .dayTime(level.getDayTime())
                .isDay(isDay) // after dayTime(): use the game's own day check
                .raining(level.isRaining())
                .canSeeSky(canSeeSky)
                .y(player.getY())
                .inWater(player.isInWater())
                .inLava(player.isInLava())
                .onFire(player.isOnFire())
                .fireResistant(player.hasEffect(MobEffects.FIRE_RESISTANCE))
                .sprinting(player.isSprinting())
                .sleeping(player.isSleeping())
                .heatSourceTemp(blocks.heat)
                .cooling(blocks.cooling)
                .nearLava(blocks.nearLava)
                .nearbyMobBodyTemp(animalsNearby ? ANIMAL_BODY_TEMP : Double.NaN)
                .armor(armor)
                .fireProtectionLevels(fireProtectionLevels)
                .build();

        HydrationModel.Activity activity = new HydrationModel.Activity(
                player.isSprinting(),
                blocks.nearLava,
                arid && isDay && daylightCycle,
                player.hasEffect(MobEffects.HUNGER),
                0);

        return new Sample(environment, activity);
    }

    /**
     * Average biome ambient over a 3×3 grid of points {@link #RANGE} blocks apart, so walking over a
     * biome border changes temperature gradually. (The original sampled all 121 columns; 9 points
     * give nearly the same result for far fewer lookups.)
     */
    private static double averageBiomeAmbient(ServerLevel level, BlockPos center, TemperatureModel model) {
        double sum = 0;
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Biome biome = level.getBiome(center.offset(dx * RANGE, 0, dz * RANGE)).value();
                sum += model.biomeAmbient(biome.getBaseTemperature());
                count++;
            }
        }
        return sum / count;
    }

    private static final class ScanResult {
        double heat = 0;
        double cooling = 0;
        boolean nearLava = false;
    }

    /** Hottest heat source (after falloff), summed cooling, and whether lava is in range. */
    private static ScanResult scanBlocks(ServerLevel level, ServerPlayer player) {
        ScanResult result = new ScanResult();
        BlockPos feet = player.blockPosition();
        Vec3 from = player.position();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    pos.set(feet.getX() + x, feet.getY() + y, feet.getZ() + z);
                    // Never load or generate chunks just to measure temperature.
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    double temp = DefaultModifiers.blockTemperature(state);
                    if (temp == 0) {
                        continue;
                    }
                    if (state.getFluidState().is(FluidTags.LAVA)) {
                        result.nearLava = true;
                    }
                    double dist = from.distanceTo(Vec3.atCenterOf(pos));
                    if (temp > 0) {
                        result.heat = Math.max(result.heat, TemperatureModel.heatFalloff(temp, dist, RANGE));
                    } else {
                        result.cooling += TemperatureModel.heatFalloff(-temp, dist, RANGE);
                    }
                }
            }
        }
        return result;
    }
}
