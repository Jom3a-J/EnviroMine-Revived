package enviromine_revived.client;

import enviromine_revived.ClimateTicker;
import enviromine_revived.EnviroMineRevived;
import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.hud.HudAnchor;
import enviromine_revived.hud.HudLayout;
import enviromine_revived.model.Severity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Draws the temperature and hydration HUD row. Everything about where and how big things are
 * lives in {@link HudLayout}; this class only reads the synced stats and draws.
 *
 * <pre>
 *  [thermometer][== temperature scale ==]   [droplet][==== hydration ====]
 * </pre>
 *
 * The temperature scale is coloured from severe cold (left) to severe heat (right). The white
 * marker is the body temperature; the faint marker shows where it is heading.
 */
public final class ClimateHud {
    private ClimateHud() {}

    public static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, "climate");
    private static final ResourceLocation THERMOMETER =
            ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, "hud/thermometer");
    private static final ResourceLocation DROPLET =
            ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, "hud/droplet");

    // ARGB colours.
    private static final int BACKGROUND = 0xA0000000;
    private static final int BODY_MARKER = 0xFFFFFFFF;
    private static final int TARGET_MARKER = 0x90FFFFFF;
    /** Severe cold .. severe heat, one per temperature zone. */
    private static final int[] TEMPERATURE_ZONES = {
            0xFF1C3A8C, 0xFF2F6BD6, 0xFF7FB2F0, 0xFF6E9E6E, 0xFFE89B3C, 0xFFD5452B, 0xFF8E1B12
    };
    private static final int WATER = 0xFF3F76E4;
    private static final int WATER_LOW = 0xFFE89B3C;
    private static final int WATER_CRITICAL = 0xFFD5452B;
    private static final int TEXT = 0xFFFFFFFF;

    /** Draw after the air bubbles, so the row stacks above hunger and air like a vanilla bar. */
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, LAYER_ID, ClimateHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || !ClientConfig.HUD_ENABLED.get() || player.isSpectator()) {
            return;
        }
        // canHurtPlayer() is true in survival and adventure, the modes where stats change.
        boolean survivalHud = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        if (!survivalHud && !ClientConfig.SHOW_IN_CREATIVE.get()) {
            return;
        }

        ClimateData data = player.getData(ModAttachments.CLIMATE);
        HudAnchor anchor = ClientConfig.HUD_ANCHOR.get();
        HudLayout.Point at = HudLayout.position(anchor, graphics.guiWidth(), graphics.guiHeight(),
                ClientConfig.HUD_OFFSET_X.get(), ClientConfig.HUD_OFFSET_Y.get(), mc.gui.rightHeight);

        long time = player.level().getGameTime();
        int heatLevel = Severity.temperature(data.bodyTemp(), Severity.TemperatureThresholds.defaults());
        int thirstLevel = Severity.hydration(data.hydration(), Severity.HydrationThresholds.defaults());
        boolean numbers = ClientConfig.SHOW_NUMBERS.get();

        int x = at.x();
        int y = at.y();
        if (!HudLayout.flashOff(heatLevel, time)) {
            graphics.blitSprite(THERMOMETER, x, y, HudLayout.ICON, HudLayout.ICON);
        }
        x += HudLayout.ICON + HudLayout.GAP;
        if (numbers) {
            String text = HudLayout.formatTemperature(data.bodyTemp(), ClientConfig.TEMPERATURE_UNIT.get());
            graphics.drawString(mc.font, text, x, y + 1, TEXT, true);
        } else {
            drawTemperatureBar(graphics, x, y + (HudLayout.HEIGHT - HudLayout.BAR_HEIGHT) / 2, data);
        }
        x += HudLayout.BAR_WIDTH + HudLayout.MIDDLE_GAP;

        if (!HudLayout.flashOff(thirstLevel, time)) {
            graphics.blitSprite(DROPLET, x, y, HudLayout.ICON, HudLayout.ICON);
        }
        x += HudLayout.ICON + HudLayout.GAP;
        double max = ClimateTicker.HYDRATION.tuning().max();
        if (numbers) {
            graphics.drawString(mc.font, HudLayout.formatHydration(data.hydration(), max), x, y + 1, TEXT, true);
        } else {
            drawHydrationBar(graphics, x, y + (HudLayout.HEIGHT - HudLayout.BAR_HEIGHT) / 2, data.hydration(), max, thirstLevel);
        }

        if (anchor == HudAnchor.STATUS_BARS) {
            mc.gui.rightHeight += HudLayout.ROW_STEP; // later status bars stack above us
        }
    }

    private static void drawTemperatureBar(GuiGraphics g, int x, int y, ClimateData data) {
        int w = HudLayout.BAR_WIDTH;
        int h = HudLayout.BAR_HEIGHT;
        Severity.TemperatureThresholds th = Severity.TemperatureThresholds.defaults();
        double[] edges = {th.cold3(), th.cold2(), th.cold1(), th.hot1(), th.hot2(), th.hot3()};

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BACKGROUND);
        for (HudLayout.Segment s : HudLayout.temperatureSegments(w, edges, TEMPERATURE_ZONES)) {
            if (s.to() > s.from()) {
                g.fill(x + s.from(), y + 1, x + s.to(), y + h - 1, s.argb());
            }
        }
        double target = data.ambient() + ClimateTicker.TEMPERATURE.tuning().comfortOffset();
        int targetCol = HudLayout.markerColumn(target, w);
        int bodyCol = HudLayout.markerColumn(data.bodyTemp(), w);
        if (targetCol != bodyCol) {
            g.fill(x + targetCol, y, x + targetCol + 1, y + h, TARGET_MARKER);
        }
        g.fill(x + bodyCol, y - 1, x + bodyCol + 1, y + h + 1, BODY_MARKER);
    }

    private static void drawHydrationBar(GuiGraphics g, int x, int y, double hydration, double max, int level) {
        int w = HudLayout.BAR_WIDTH;
        int h = HudLayout.BAR_HEIGHT;
        int colour = level >= 2 ? WATER_CRITICAL : level == 1 ? WATER_LOW : WATER;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BACKGROUND);
        int fill = HudLayout.hydrationFill(hydration, max, w);
        if (fill > 0) {
            g.fill(x, y, x + fill, y + h, colour);
        }
    }
}
