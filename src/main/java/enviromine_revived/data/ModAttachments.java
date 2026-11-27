package enviromine_revived.data;

import java.util.function.Supplier;

import enviromine_revived.EnviroMineRevived;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments registered by this mod. */
public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EnviroMineRevived.MOD_ID);

    /**
     * The player's body temperature and hydration.
     *
     * <ul>
     *   <li>Saved with the player (codec).</li>
     *   <li>Synced automatically whenever the server calls {@code setData}, but only to the player
     *       it belongs to: other players have no reason to know your stats.</li>
     *   <li>Not copied on death, so you respawn with fresh stats, like vanilla hunger. It is copied
     *       when returning from the End, which NeoForge treats as a non-death respawn.</li>
     * </ul>
     */
    public static final Supplier<AttachmentType<ClimateData>> CLIMATE = ATTACHMENT_TYPES.register("climate",
            () -> AttachmentType.builder(() -> ClimateData.DEFAULT)
                    .serialize(ClimateData.CODEC)
                    .sync(ModAttachments::shouldSyncClimate, ClimateData.STREAM_CODEC)
                    .build());

    /**
     * Game time until which the player cannot take another sip. Server only: not saved (a
     * fraction of a second does not need to survive a restart) and not synced.
     */
    public static final Supplier<AttachmentType<Long>> DRINK_COOLDOWN = ATTACHMENT_TYPES.register("drink_cooldown",
            () -> AttachmentType.builder(() -> 0L).build());

    /**
     * Only the owning player, and only if their connection can receive NeoForge's attachment
     * sync packet. Without the second check, a connection that never negotiated that channel
     * (a vanilla client, or the fake players used by GameTests) would raise an error on every
     * update.
     */
    private static boolean shouldSyncClimate(IAttachmentHolder holder, ServerPlayer toPlayer) {
        return holder == toPlayer
                && toPlayer.connection != null
                && toPlayer.connection.hasChannel(SyncAttachmentsPayload.TYPE);
    }
}
