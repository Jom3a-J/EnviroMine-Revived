package enviromine_revived.network;

import enviromine_revived.EnviroMineRevived;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server: "I right-clicked water with an empty hand". It carries no data on purpose:
 * the server does its own look check, so a client cannot claim to drink water it is not facing.
 *
 * <p>Needed because right-clicking empty space is only noticed on the client.
 */
public record DrinkPayload() implements CustomPacketPayload {
    public static final DrinkPayload INSTANCE = new DrinkPayload();
    public static final Type<DrinkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, "drink"));
    public static final StreamCodec<ByteBuf, DrinkPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
