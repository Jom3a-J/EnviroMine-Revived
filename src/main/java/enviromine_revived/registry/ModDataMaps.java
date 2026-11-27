package enviromine_revived.registry;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import enviromine_revived.EnviroMineRevived;
import enviromine_revived.model.ArmorModifier;
import enviromine_revived.model.WaterType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Data-pack values (NeoForge data maps). Files live at
 * {@code data/<namespace>/data_maps/<registry>/<name>.json}; see docs/DATAPACKS.md for examples.
 * Entries accept tags ({@code "#minecraft:campfires"}), and later packs override earlier ones.
 *
 * <ul>
 *   <li>{@code block/block_temperature.json}: heat or cold given off by blocks</li>
 *   <li>{@code item/armor_temperature.json}: how worn armour changes the felt temperature</li>
 *   <li>{@code worldgen/biome/biome_climate.json}: per-biome overrides</li>
 * </ul>
 *
 * These are only read on the server, so they are not synced to clients.
 */
public final class ModDataMaps {
    private ModDataMaps() {}

    /**
     * @param temperature heat (positive) or cold (negative) before distance falloff
     * @param onlyWhenLit for blocks with a "lit" state (furnaces, campfires): only while lit
     */
    public record BlockTemperature(double temperature, boolean onlyWhenLit) {
        public static final Codec<BlockTemperature> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("temperature").forGetter(BlockTemperature::temperature),
                Codec.BOOL.optionalFieldOf("only_when_lit", true).forGetter(BlockTemperature::onlyWhenLit)
        ).apply(i, BlockTemperature::new));
    }

    /** Same fields as {@link ArmorModifier}; anything left out has no effect. */
    public static final Codec<ArmorModifier> ARMOR_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("sun_add", 0.0).forGetter(ArmorModifier::sunAdd),
            Codec.DOUBLE.optionalFieldOf("shade_add", 0.0).forGetter(ArmorModifier::shadeAdd),
            Codec.DOUBLE.optionalFieldOf("night_add", 0.0).forGetter(ArmorModifier::nightAdd),
            Codec.DOUBLE.optionalFieldOf("sun_mult", 1.0).forGetter(ArmorModifier::sunMult),
            Codec.DOUBLE.optionalFieldOf("shade_mult", 1.0).forGetter(ArmorModifier::shadeMult),
            Codec.DOUBLE.optionalFieldOf("night_mult", 1.0).forGetter(ArmorModifier::nightMult)
    ).apply(i, ArmorModifier::new));

    /**
     * @param ambient    replaces the biome's ambient temperature (°C), e.g. for modded biomes whose
     *                   vanilla temperature value is misleading
     * @param water      forces the kind of water found in the biome
     * @param extraDrain extra hydration lost per update while in the biome
     */
    public record BiomeClimate(Optional<Double> ambient, Optional<WaterType> water, double extraDrain) {
        public static final Codec<BiomeClimate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("ambient").forGetter(BiomeClimate::ambient),
                ModComponents.WATER_TYPE_CODEC.optionalFieldOf("water").forGetter(BiomeClimate::water),
                Codec.DOUBLE.optionalFieldOf("extra_drain", 0.0).forGetter(BiomeClimate::extraDrain)
        ).apply(i, BiomeClimate::new));
    }

    public static final DataMapType<Block, BlockTemperature> BLOCK_TEMPERATURE = DataMapType
            .builder(id("block_temperature"), Registries.BLOCK, BlockTemperature.CODEC).build();

    public static final DataMapType<Item, ArmorModifier> ARMOR_TEMPERATURE = DataMapType
            .builder(id("armor_temperature"), Registries.ITEM, ARMOR_CODEC).build();

    public static final DataMapType<Biome, BiomeClimate> BIOME_CLIMATE = DataMapType
            .builder(id("biome_climate"), Registries.BIOME, BiomeClimate.CODEC).build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BLOCK_TEMPERATURE);
        event.register(ARMOR_TEMPERATURE);
        event.register(BIOME_CLIMATE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, path);
    }
}
