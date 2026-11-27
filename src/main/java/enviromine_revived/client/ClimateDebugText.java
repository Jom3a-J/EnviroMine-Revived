package enviromine_revived.client;

import java.util.Locale;

import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * Adds a line to the F3 debug screen with the stats the client has received from the server.
 * It shows that syncing works; the real HUD comes in M3.
 *
 * <p>Client only: registered from {@code EnviroMineRevived} only when running on a client.
 */
public final class ClimateDebugText {
    private ClimateDebugText() {}

    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            return;
        }
        ClimateData d = player.getData(ModAttachments.CLIMATE);
        event.getLeft().add("");
        event.getLeft().add(String.format(Locale.ROOT,
                "[EnviroMine] Body %.2f °C  Hydration %.1f  Ambient %.1f °C",
                d.bodyTemp(), d.hydration(), d.ambient()));
    }
}
