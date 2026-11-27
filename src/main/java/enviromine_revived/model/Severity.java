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
    public record TemperatureThresholds(
            @Doc(value = "Body temperature at or below which you are cold (warning).", min = 0, max = 100) double cold1,
            @Doc(value = "...freezing (Hypothermia I).", min = 0, max = 100) double cold2,
            @Doc(value = "...severely hypothermic (Hypothermia II and damage).", min = 0, max = 100) double cold3,
            @Doc(value = "Body temperature at or above which you are hot (warning).", min = 0, max = 100) double hot1,
            @Doc(value = "...overheating (Heatstroke I).", min = 0, max = 100) double hot2,
            @Doc(value = "...in severe heatstroke (Heatstroke II and damage).", min = 0, max = 100) double hot3) {
        public static TemperatureThresholds defaults() {
            return new TemperatureThresholds(35, 32, 30, 39, 41, 43);
        }

        /** Must run cold3 < cold2 < cold1 < hot1 < hot2 < hot3, or levels would be skipped. */
        public boolean isOrdered() {
            return cold3 < cold2 && cold2 < cold1 && cold1 < hot1 && hot1 < hot2 && hot2 < hot3;
        }
    }

    /** Hydration thresholds, ordered mild to severe. */
    public record HydrationThresholds(
            @Doc(value = "Hydration at or below which you are thirsty (warning).", min = 0, max = 10000) double level1,
            @Doc(value = "...parched (Dehydration I).", min = 0, max = 10000) double level2,
            @Doc(value = "...dehydrated (Dehydration II and damage).", min = 0, max = 10000) double level3) {
        /** Thirsty at 30, parched at 10, dehydrated at 0. */
        public static HydrationThresholds defaults() {
            return new HydrationThresholds(30, 10, 0);
        }

        /** Must run level3 < level2 < level1. */
        public boolean isOrdered() {
            return level3 < level2 && level2 < level1;
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
