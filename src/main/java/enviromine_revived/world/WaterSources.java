package enviromine_revived.world;

import enviromine_revived.model.WaterClassifier;
import enviromine_revived.model.WaterType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/** Turns a water block or cauldron in the world into a {@link WaterType}. */
public final class WaterSources {
    private WaterSources() {}

    /** Water in a source block or stream. */
    public static WaterType ofWater(Level level, BlockPos pos) {
        return classify(level, pos, pos.getY(), false);
    }

    /**
     * Water in a cauldron. Its height does not matter (it was carried there), but its biome does,
     * and a heat source under it boils it clean, as in the original.
     */
    public static WaterType ofCauldron(Level level, BlockPos pos) {
        return classify(level, pos, level.getSeaLevel(), isHeatSource(level.getBlockState(pos.below())));
    }

    private static WaterType classify(Level level, BlockPos pos, double y, boolean heated) {
        Holder<Biome> biome = level.getBiome(pos);
        boolean dirty = biome.is(Tags.Biomes.IS_SWAMP);
        boolean salty = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)
                || biome.is(Tags.Biomes.IS_OCEAN) || biome.is(Tags.Biomes.IS_BEACH);
        boolean cold = biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_ICY)
                || biome.value().coldEnoughToSnow(pos);
        return WaterClassifier.classify(new WaterClassifier.Hints(dirty, salty, cold, y, level.getSeaLevel(), heated));
    }

    /** Fire, soul fire, a lit campfire, magma or lava. */
    public static boolean isHeatSource(BlockState state) {
        if (state.getFluidState().is(FluidTags.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return true;
        }
        return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && state.getValue(BlockStateProperties.LIT);
    }

    /** What the player is looking at within reach, stopping at fluids as chosen. */
    public static BlockHitResult lookTarget(Player player, ClipContext.Fluid fluids) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(player.blockInteractionRange()));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluids, player));
    }
}
