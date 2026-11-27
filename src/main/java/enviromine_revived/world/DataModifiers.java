package enviromine_revived.world;

import enviromine_revived.model.ArmorModifier;
import enviromine_revived.registry.ModDataMaps;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Looks up block, armour and biome values from the data maps (see {@link ModDataMaps}). The mod's
 * own defaults are in data/enviromine_revived/data_maps; data packs can change or add entries.
 */
public final class DataModifiers {
    private DataModifiers() {}

    /** Heat (positive) or cold (negative) of a block before distance falloff; 0 means no effect. */
    public static double blockTemperature(BlockState state) {
        ModDataMaps.BlockTemperature entry = state.getBlockHolder().getData(ModDataMaps.BLOCK_TEMPERATURE);
        if (entry == null) {
            return 0;
        }
        if (entry.onlyWhenLit() && state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
            return 0; // unlit furnace or campfire
        }
        return entry.temperature();
    }

    /** How a worn item changes the felt temperature; {@link ArmorModifier#NONE} if it has no entry. */
    public static ArmorModifier armor(ItemStack stack) {
        Holder<Item> item = stack.getItemHolder();
        ArmorModifier entry = item.getData(ModDataMaps.ARMOR_TEMPERATURE);
        return entry == null ? ArmorModifier.NONE : entry;
    }

    /** Per-biome overrides, or null. */
    public static ModDataMaps.BiomeClimate biome(Holder<Biome> biome) {
        return biome.getData(ModDataMaps.BIOME_CLIMATE);
    }
}
