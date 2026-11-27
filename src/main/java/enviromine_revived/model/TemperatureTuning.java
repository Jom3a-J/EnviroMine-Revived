package enviromine_revived.model;

/**
 * Every number the temperature model uses. Units are degrees Celsius, and "per update" means once
 * per model update (the server runs one update every 30 ticks, 1.5 seconds, like the original).
 *
 * <p>{@link #defaults()} gives the gentle vanilla-plus defaults. Where a default differs from the
 * original EnviroMine value, the original is given in the comment so the change is easy to undo.
 * In M5 these values will come from the server config file instead.
 */
public record TemperatureTuning(
        // --- Body ---
        /** Body temperature everyone starts at and the model treats as healthy. */
        double normalBodyTemp,
        /** Body temperature settles at ambient + this. With 12, an ambient of 25 is comfortable. */
        double comfortOffset,
        /** Hard limits so a bug or extreme modifier can never produce absurd values. */
        double minBodyTemp,
        double maxBodyTemp,

        // --- Biome ---
        /** Ambient temperature for the hottest vanilla biome temperature (2.0, desert). */
        double biomeHotMax,
        /** Ambient temperature for the coldest vanilla biome temperature (-2.0 and below). */
        double biomeColdMin,

        // --- Altitude ---
        /** Ambient that deep underground is pulled towards (only ever warms, like the original). */
        double undergroundTemp,
        double undergroundStartY,
        double undergroundFullY,
        /** Ambient that very high places are pulled towards (only ever cools). */
        double highAltitudeTemp,
        double highAltitudeStartY,
        double highAltitudeFullY,

        // --- Weather, time and activity (subtracted from or added to ambient) ---
        double rainCooling,
        double shadeCooling,
        double nightCooling,
        /** Extra night cooling in biomes without precipitation (deserts get cold at night). */
        double aridNightExtraCooling,
        /** Values above 1 make the hottest and coldest parts of the day plateau. */
        double dayCurveScale,
        /** Shifts the daily curve so the warmest point is after noon, in degrees of the day cycle. */
        double dayCurveOffsetDegrees,
        double sleepingWarmth,
        double sprintingWarmth,
        /** Nearby warm-blooded mobs warm you if you are colder than their body temp minus this. */
        double huddleOffset,

        // --- Water, fire, lava ---
        /** In water, any ambient above this is reduced to it... */
        double waterCoolCap,
        /** ...unless it is above waterCoolCap + this, in which case this much is subtracted. */
        double waterHotReduction,
        double lavaTemp,
        double burningMinTemp,
        /** Fire Protection levels (summed over armour) that would fully cancel heat. */
        double fireProtectionLevelsForFull,

        // --- Drift rates (degrees per update, before amplification) ---
        double baseRate,
        double exposedRainRate,
        double waterRate,
        /** Used when a heat source is more than strongHeatMargin warmer than ambient. */
        double strongHeatRate,
        double strongHeatMargin,
        double nearLavaRate,
        double burningRate,
        double lavaRate,
        double sprintRate,
        /** When body and target differ by more than this, the rate is multiplied by diff / this. */
        double driftAmplifyDistance,
        /** Global multiplier on all drift rates (original config: tempMult). */
        double rateMultiplier
) {
    public static TemperatureTuning defaults() {
        return new TemperatureTuning(
                37.0,   // normalBodyTemp
                12.0,   // comfortOffset
                20.0,   // minBodyTemp (original: unbounded)
                50.0,   // maxBodyTemp (original: unbounded)

                45.0,   // biomeHotMax
                -15.0,  // biomeColdMin

                24.0,   // undergroundTemp (original 30: made deep caves hot enough for heatstroke)
                48.0,   // undergroundStartY
                0.0,    // undergroundFullY
                -30.0,  // highAltitudeTemp
                120.0,  // highAltitudeStartY (original 90: modern hills reach far higher)
                320.0,  // highAltitudeFullY (original 256: the 1.18+ build limit is 320)

                6.0,    // rainCooling (original 10, and it applied even under a roof)
                2.5,    // shadeCooling
                8.0,    // nightCooling (original 10)
                15.0,   // aridNightExtraCooling (original 30)
                1.25,   // dayCurveScale
                30.0,   // dayCurveOffsetDegrees
                10.0,   // sleepingWarmth
                2.0,    // sprintingWarmth
                12.0,   // huddleOffset

                25.0,   // waterCoolCap
                50.0,   // waterHotReduction
                200.0,  // lavaTemp
                75.0,   // burningMinTemp
                18.0,   // fireProtectionLevelsForFull

                0.001,  // baseRate
                0.01,   // exposedRainRate
                0.01,   // waterRate
                0.005,  // strongHeatRate
                5.0,    // strongHeatMargin
                0.005,  // nearLavaRate
                0.1,    // burningRate
                1.0,    // lavaRate
                0.01,   // sprintRate
                10.0,   // driftAmplifyDistance
                1.0     // rateMultiplier
        );
    }

    /** The ambient temperature at which body temperature settles at normal. */
    public double comfortableAmbient() {
        return normalBodyTemp - comfortOffset;
    }
}
