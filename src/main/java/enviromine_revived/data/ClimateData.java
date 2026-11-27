package enviromine_revived.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's climate stats, stored as a NeoForge data attachment (see ModAttachments).
 *
 * <p>Immutable on purpose: the server replaces it with {@code player.setData(...)}, and NeoForge
 * only syncs an attachment to the client when it is set, not when an object inside it changes.
 *
 * @param bodyTemp  body temperature in °C (normal 37)
 * @param hydration 0..100
 * @param ambient   the felt ambient temperature from the last update, kept for the HUD and debug
 *                  screen so the client never has to work it out itself
 */
public record ClimateData(double bodyTemp, double hydration, double ambient) {
    public static final ClimateData DEFAULT = new ClimateData(37.0, 100.0, 25.0);

    /** Saved with the player. Field names are part of the save format: do not rename them. */
    public static final Codec<ClimateData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("body_temp").forGetter(ClimateData::bodyTemp),
            Codec.DOUBLE.fieldOf("hydration").forGetter(ClimateData::hydration),
            Codec.DOUBLE.optionalFieldOf("ambient", DEFAULT.ambient()).forGetter(ClimateData::ambient)
    ).apply(i, ClimateData::new));

    /** Sent to the owning client. */
    public static final StreamCodec<ByteBuf, ClimateData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ClimateData::bodyTemp,
            ByteBufCodecs.DOUBLE, ClimateData::hydration,
            ByteBufCodecs.DOUBLE, ClimateData::ambient,
            ClimateData::new);

    public ClimateData withBodyTemp(double value) {
        return new ClimateData(value, hydration, ambient);
    }

    public ClimateData withHydration(double value) {
        return new ClimateData(bodyTemp, value, ambient);
    }

    public ClimateData withAmbient(double value) {
        return new ClimateData(bodyTemp, hydration, value);
    }
}
