package enviromine_revived.hud;

/**
 * Where the HUD goes and how its bars are drawn. Plain Java with no Minecraft classes, so it can
 * be unit tested; {@code client.ClimateHud} does the actual drawing.
 *
 * <p>All values are in scaled GUI pixels, as Minecraft's GUI uses.
 */
public final class HudLayout {
    private HudLayout() {}

    /** Same width as the vanilla hunger bar (9 icons of 8 px plus 1 px), so it lines up above it. */
    public static final int WIDTH = 81;
    /** One status-bar row. */
    public static final int HEIGHT = 9;
    /** How much the row raises later status bars when stacked like a vanilla bar. */
    public static final int ROW_STEP = 10;

    public static final int ICON = 9;
    public static final int GAP = 2;
    /** Space between the temperature half and the hydration half. */
    public static final int MIDDLE_GAP = 3;
    /** Width of each bar: what is left of the row after the two icons and gaps. */
    public static final int BAR_WIDTH = (WIDTH - 2 * ICON - 2 * GAP - MIDDLE_GAP) / 2;
    public static final int BAR_HEIGHT = 5;

    private static final int HOTBAR_HALF_WIDTH = 91;
    /** Off-hand slot plus a little space, so the HUD does not cover it. */
    private static final int OFFHAND_SPACE = 29 + 4;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int EDGE = 2;

    public record Point(int x, int y) {}

    /**
     * Top-left corner of the HUD row.
     *
     * @param rightHeight vanilla's running height of the right-hand status bars (Gui.rightHeight),
     *                    only used by {@link HudAnchor#STATUS_BARS}
     */
    public static Point position(HudAnchor anchor, int screenWidth, int screenHeight,
                                 int offsetX, int offsetY, int rightHeight) {
        int centre = screenWidth / 2;
        int x;
        int y;
        switch (anchor) {
            case STATUS_BARS -> {
                x = centre + HOTBAR_HALF_WIDTH - WIDTH;
                y = screenHeight - rightHeight;
            }
            case HOTBAR_LEFT -> {
                x = centre - HOTBAR_HALF_WIDTH - OFFHAND_SPACE - WIDTH;
                y = screenHeight - HOTBAR_HEIGHT + (HOTBAR_HEIGHT - HEIGHT) / 2;
            }
            case HOTBAR_RIGHT -> {
                x = centre + HOTBAR_HALF_WIDTH + OFFHAND_SPACE;
                y = screenHeight - HOTBAR_HEIGHT + (HOTBAR_HEIGHT - HEIGHT) / 2;
            }
            case TOP_LEFT -> {
                x = EDGE;
                y = EDGE;
            }
            case TOP_RIGHT -> {
                x = screenWidth - WIDTH - EDGE;
                y = EDGE;
            }
            case BOTTOM_LEFT -> {
                x = EDGE;
                y = screenHeight - HEIGHT - EDGE;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - WIDTH - EDGE;
                y = screenHeight - HEIGHT - EDGE;
            }
            default -> throw new IllegalArgumentException(anchor.name());
        }
        // Offsets: positive x moves right, positive y moves down. Never leave the screen.
        x = clamp(x + offsetX, 0, Math.max(0, screenWidth - WIDTH));
        y = clamp(y + offsetY, 0, Math.max(0, screenHeight - HEIGHT));
        return new Point(x, y);
    }

    // ------------------------------------------------------------------------------------------
    // Temperature bar
    // ------------------------------------------------------------------------------------------

    /** Body temperatures shown on the bar. Values outside are pinned to the ends. */
    public static final double SCALE_MIN = 29.0;
    public static final double SCALE_MAX = 44.0;

    /** Pixel column (0 .. width-1) of a body temperature on a bar {@code width} pixels wide. */
    public static int markerColumn(double bodyTemp, int width) {
        double t = (clamp(bodyTemp, SCALE_MIN, SCALE_MAX) - SCALE_MIN) / (SCALE_MAX - SCALE_MIN);
        return clamp((int) Math.round(t * (width - 1)), 0, width - 1);
    }

    /** One coloured stretch of the temperature bar. {@code to} is exclusive. */
    public record Segment(int from, int to, int argb) {}

    /**
     * The bar's colour zones, from severe cold (dark blue) through normal (grey-green) to severe
     * heat (dark red), split at the severity thresholds so the colours match the levels.
     */
    public static Segment[] temperatureSegments(int width, double[] thresholds, int[] colours) {
        // thresholds: cold3, cold2, cold1, hot1, hot2, hot3 (ascending); colours: 7 zones
        Segment[] out = new Segment[colours.length];
        int start = 0;
        for (int i = 0; i < colours.length; i++) {
            int end = i < thresholds.length ? markerColumn(thresholds[i], width) : width;
            if (i == colours.length - 1) {
                end = width;
            }
            end = Math.max(end, start);
            out[i] = new Segment(start, end, colours[i]);
            start = end;
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // Hydration bar
    // ------------------------------------------------------------------------------------------

    /** Filled pixels of a hydration bar. Any hydration above 0 shows at least one pixel. */
    public static int hydrationFill(double hydration, double max, int width) {
        if (hydration <= 0) {
            return 0;
        }
        return clamp((int) Math.round(hydration / max * width), 1, width);
    }

    // ------------------------------------------------------------------------------------------
    // Warnings and text
    // ------------------------------------------------------------------------------------------

    /**
     * Whether an icon is in the "off" half of its flash. Level 2 flashes slowly and level 3 fast,
     * so a worsening condition is noticeable without reading numbers. Levels 0 and 1 never flash.
     */
    public static boolean flashOff(int severity, long gameTime) {
        int level = Math.abs(severity);
        if (level < 2) {
            return false;
        }
        int period = level >= 3 ? 10 : 20; // ticks
        return (gameTime % period) >= period / 2;
    }

    /**
     * Text for number mode, e.g. "37.2°" (Celsius) or "99.0°" (Fahrenheit). The unit letter is left
     * out so the text fits in the bar's space; the unit is the player's own setting.
     */
    public static String formatTemperature(double celsius, TemperatureUnit unit) {
        double value = unit == TemperatureUnit.FAHRENHEIT ? celsius * 9.0 / 5.0 + 32.0 : celsius;
        return String.format(java.util.Locale.ROOT, "%.1f°", value);
    }

    /** Text for number mode, e.g. "87%". */
    public static String formatHydration(double hydration, double max) {
        return Math.round(hydration / max * 100) + "%";
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
