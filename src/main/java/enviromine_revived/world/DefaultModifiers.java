package enviromine_revived.world;

import java.util.Map;

import enviromine_revived.model.ArmorModifier;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Built-in block and armour temperature values.
 *
 * <p><b>Temporary (M2):</b> in M5 these move to data-pack files (NeoForge data maps) so servers and
 * packs can change them without code. Until then they live here.
 */
public final class DefaultModifiers {
    private DefaultModifiers() {}

    /**
     * Heat (positive) or cold (negative) of a block before distance falloff. Lit-able blocks only
     * count while lit.
     *
     * <p>Gentler than the original, which gave torches 75 (as hot as fire): a base full of torches
     * could give heatstroke. Lanterns did not exist in 1.7.10.
     */
    private static final Map<Block, Double> BLOCK_TEMPERATURES = Map.ofEntries(
            Map.entry(Blocks.FIRE, 75.0),
            Map.entry(Blocks.SOUL_FIRE, 75.0),
            Map.entry(Blocks.CAMPFIRE, 75.0),
            Map.entry(Blocks.SOUL_CAMPFIRE, 60.0),
            Map.entry(Blocks.FURNACE, 50.0),
            Map.entry(Blocks.BLAST_FURNACE, 50.0),
            Map.entry(Blocks.SMOKER, 50.0),
            Map.entry(Blocks.MAGMA_BLOCK, 50.0),
            Map.entry(Blocks.TORCH, 30.0),
            Map.entry(Blocks.WALL_TORCH, 30.0),
            Map.entry(Blocks.SOUL_TORCH, 30.0),
            Map.entry(Blocks.SOUL_WALL_TORCH, 30.0),
            Map.entry(Blocks.LANTERN, 30.0),
            Map.entry(Blocks.SOUL_LANTERN, 30.0),
            // Cold blocks: values are summed over every block in range, so they are small.
            Map.entry(Blocks.SNOW, -0.02),
            Map.entry(Blocks.SNOW_BLOCK, -0.02),
            Map.entry(Blocks.POWDER_SNOW, -0.05),
            Map.entry(Blocks.ICE, -0.05),
            Map.entry(Blocks.PACKED_ICE, -0.1),
            Map.entry(Blocks.BLUE_ICE, -0.2)
    );

    public static final double LAVA_TEMPERATURE = 200.0;

    /** 0 means "no effect". */
    public static double blockTemperature(BlockState state) {
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return LAVA_TEMPERATURE;
        }
        Double value = BLOCK_TEMPERATURES.get(state.getBlock());
        if (value == null) {
            return 0;
        }
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
            return 0; // unlit furnace or campfire
        }
        return value;
    }

    /**
     * Per-piece armour values from the original EnviroMine (leather warm, metals heat up in the sun,
     * diamond reflects a little). Materials that did not exist in 1.7.10 have no effect for now.
     */
    public static ArmorModifier armor(Holder<ArmorMaterial> material) {
        if (material.is(ArmorMaterials.LEATHER)) {
            return new ArmorModifier(1, 1, 1, 1, 1, 1);
        } else if (material.is(ArmorMaterials.IRON)) {
            return new ArmorModifier(2, 0, -1, 1.1, 1, 1);
        } else if (material.is(ArmorMaterials.GOLD)) {
            return new ArmorModifier(2.5, 0, 0, 1.2, 1, 1);
        } else if (material.is(ArmorMaterials.DIAMOND)) {
            return new ArmorModifier(0, 0, 0, 0.9, 1, 1);
        }
        return ArmorModifier.NONE;
    }
}
