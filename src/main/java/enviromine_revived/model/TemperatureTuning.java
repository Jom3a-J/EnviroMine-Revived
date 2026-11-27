package enviromine_revived.model;

/**
 * Every number the temperature model uses. Units are degrees Celsius, and "per update" means once
 * per model update (the server runs one update every 30 ticks, 1.5 seconds, like the original).
 *
 * <p>{@link #defaults()} gives the gentle vanilla-plus defaults. Where a default differs from the
 * original EnviroMine value, the original is given in the comment so the change is easy to undo.
 * Each component's {@link Doc} becomes an entry in the server config.
 */
public record TemperatureTuning(
        // --- Body ---
        @Doc(value = "Healthy body temperature.", min = 30, max = 45) double normalBodyTemp,
        @Doc(value = "Body temperature settles at ambient + this. With 12, an ambient of 25 is comfortable.", min = 0, max = 40) double comfortOffset,
        @Doc(value = "Lowest possible body temperature.", min = 0, max = 37) double minBodyTemp,
        @Doc(value = "Highest possible body temperature.", min = 37, max = 100) double maxBodyTemp,

        // --- Biome ---
        @Doc("Ambient temperature of the hottest vanilla biomes (biome temperature 2.0).") double biomeHotMax,
        @Doc("Ambient temperature of the coldest vanilla biomes (biome temperature -2.0).") double biomeColdMin,

        // --- Altitude ---
        @Doc("Deep underground is pulled towards this ambient temperature (it only ever warms).") double undergroundTemp,
        @Doc(value = "Height where the underground effect starts.", min = -2048, max = 2048) double undergroundStartY,
        @Doc(value = "Height where the underground effect is complete.", min = -2048, max = 2048) double undergroundFullY,
        @Doc("High places are pulled towards this ambient temperature (it only ever cools).") double highAltitudeTemp,
        @Doc(value = "Height where high-altitude cooling starts.", min = -2048, max = 2048) double highAltitudeStartY,
        @Doc(value = "Height where high-altitude cooling is complete.", min = -2048, max = 2048) double highAltitudeFullY,

        // --- Weather, time and activity ---
        @Doc("Cooling while out in the rain.") double rainCooling,
        @Doc("Cooling in the shade during the day.") double shadeCooling,
        @Doc("Cooling at the coldest part of the night.") double nightCooling,
        @Doc("Extra night cooling in biomes without rain (deserts get cold at night).") double aridNightExtraCooling,
        @Doc(value = "Values above 1 make the warmest and coldest parts of the day last longer.", min = 0.1, max = 10) double dayCurveScale,
        @Doc(value = "Shifts the warmest point of the day after noon, in degrees of the day cycle.", min = -180, max = 180) double dayCurveOffsetDegrees,
        @Doc("Warmth while sleeping in a bed.") double sleepingWarmth,
        @Doc("Warmth while sprinting.") double sprintingWarmth,
        @Doc("Nearby animals warm you if you are colder than their body temperature minus this.") double huddleOffset,

        // --- Water, fire, lava ---
        @Doc("In water, warmer ambient temperatures are reduced to this...") double waterCoolCap,
        @Doc("...unless they are this much hotter still, in which case this much is subtracted.") double waterHotReduction,
        @Doc("Ambient temperature while standing in lava.") double lavaTemp,
        @Doc("Minimum ambient temperature while on fire.") double burningMinTemp,
        @Doc(value = "Fire Protection levels (summed over armour) that cancel all heat.", min = 1, max = 1000) double fireProtectionLevelsForFull,

        // --- Drift rates (degrees per update, before amplification) ---
        @Doc(value = "Normal speed at which body temperature changes, per update.", min = 0, max = 100) double baseRate,
        @Doc(value = "Cooling speed while out in the rain.", min = 0, max = 100) double exposedRainRate,
        @Doc(value = "Cooling speed while in water.", min = 0, max = 100) double waterRate,
        @Doc(value = "Warming speed near a strong heat source.", min = 0, max = 100) double strongHeatRate,
        @Doc(value = "A heat source counts as strong when it is this much warmer than the ambient.", min = 0, max = 1000) double strongHeatMargin,
        @Doc(value = "Warming speed near lava.", min = 0, max = 100) double nearLavaRate,
        @Doc(value = "Warming speed while on fire.", min = 0, max = 100) double burningRate,
        @Doc(value = "Warming speed while in lava.", min = 0, max = 100) double lavaRate,
        @Doc(value = "Warming speed while sprinting.", min = 0, max = 100) double sprintRate,
        @Doc(value = "When body and target differ by more than this, changes speed up proportionally.", min = 0.1, max = 1000) double driftAmplifyDistance,
        @Doc(value = "Multiplies every speed above.", min = 0, max = 100) double rateMultiplier
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

    /**
     * A copy with a different global rate multiplier. (The server config builds whole records
     * from its values, so nothing else needs to call the long constructor by position.)
     */
    public TemperatureTuning withRateMultiplier(double multiplier) {
        return new TemperatureTuning(normalBodyTemp, comfortOffset, minBodyTemp, maxBodyTemp,
                biomeHotMax, biomeColdMin,
                undergroundTemp, undergroundStartY, undergroundFullY,
                highAltitudeTemp, highAltitudeStartY, highAltitudeFullY,
                rainCooling, shadeCooling, nightCooling, aridNightExtraCooling,
                dayCurveScale, dayCurveOffsetDegrees, sleepingWarmth, sprintingWarmth, huddleOffset,
                waterCoolCap, waterHotReduction, lavaTemp, burningMinTemp, fireProtectionLevelsForFull,
                baseRate, exposedRainRate, waterRate, strongHeatRate, strongHeatMargin,
                nearLavaRate, burningRate, lavaRate, sprintRate, driftAmplifyDistance,
                multiplier);
    }

    /** Normal body temperature must lie strictly between the limits. */
    public boolean isConsistent() {
        return minBodyTemp < normalBodyTemp && normalBodyTemp < maxBodyTemp;
    }

    /** The ambient temperature at which body temperature settles at normal. */
    public double comfortableAmbient() {
        return normalBodyTemp - comfortOffset;
    }
}
