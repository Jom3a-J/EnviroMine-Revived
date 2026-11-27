package enviromine_revived.model;

/**
 * How bad a stat is, used for the HUD colour and for which effects apply (M4).
 *
 * <p>Temperature: negative levels are cold, positive are hot. The thresholds are the original's:
 * hypothermia at 35 / 32 / 30 °C and heatstroke at 39 / 41 / 43 °C. Hydration has only the
 * "too low" side.
 */
public final class Severity {
    private Severity() {}

    /** Body temperature thresholds, ordered mild to severe on each side. */
    public record TemperatureThresholds(double cold1, double cold2, double cold3,
                                        double hot1, double hot2, double hot3) {
        public static TemperatureThresholds defaults() {
            return new TemperatureThresholds(35, 32, 30, 39, 41, 43);
        }
    }

    /** Hydration thresholds, ordered mild to severe. */
    public record HydrationThresholds(double level1, double level2, double level3) {
        /** Thirsty at 30, parched at 10, dehydrated at 0. */
        public static HydrationThresholds defaults() {
            return new HydrationThresholds(30, 10, 0);
        }
    }

    /** -3 (severe cold) .. 0 (fine) .. 3 (severe heat). Boundaries count as the worse level. */
    public static int temperature(double bodyTemp, TemperatureThresholds th) {
        if (bodyTemp <= th.cold3()) return -3;
        if (bodyTemp <= th.cold2()) return -2;
        if (bodyTemp <= th.cold1()) return -1;
        if (bodyTemp >= th.hot3()) return 3;
        if (bodyTemp >= th.hot2()) return 2;
        if (bodyTemp >= th.hot1()) return 1;
        return 0;
    }

    /** 0 (fine) .. 3 (dehydrated). Boundaries count as the worse level. */
    public static int hydration(double hydration, HydrationThresholds th) {
        if (hydration <= th.level3()) return 3;
        if (hydration <= th.level2()) return 2;
        if (hydration <= th.level1()) return 1;
        return 0;
    }
}
