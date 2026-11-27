package enviromine_revived.registry;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import enviromine_revived.EnviroMineRevived;
import enviromine_revived.model.WaterType;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components.
 *
 * <p>{@code enviromine_revived:water_type} marks a vanilla water bottle as dirty, salty or cold.
 * Clean bottles never get it, so they stay identical to vanilla water bottles (brewing, trading
 * and other mods treat them normally), and any water bottle without it counts as clean.
 */
public final class ModComponents {
    private ModComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnviroMineRevived.MOD_ID);

    /** Saved as "dirty", "salty", "cold" (or "clean"). Do not rename: part of the save format. */
    public static final Codec<WaterType> WATER_TYPE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(WaterType.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown water type: " + name);
                }
            },
            type -> type.name().toLowerCase(Locale.ROOT));

    public static final StreamCodec<ByteBuf, WaterType> WATER_TYPE_STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> WaterType.values()[i], WaterType::ordinal);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WaterType>> WATER_TYPE =
            COMPONENTS.registerComponentType("water_type",
                    builder -> builder.persistent(WATER_TYPE_CODEC).networkSynchronized(WATER_TYPE_STREAM_CODEC));
}
