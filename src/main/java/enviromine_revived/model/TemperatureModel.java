package enviromine_revived.model;

/**
 * Body temperature model. No Minecraft classes: everything comes in through {@link Environment}.
 *
 * <p>The structure follows the original EnviroMine ({@code EM_StatusManager.getSurroundingData} and
 * {@code EnviroDataTracker.updateData}): first work out the ambient temperature the player feels
 * and how fast the body may heat up or cool down, then move the body temperature towards
 * {@code ambient + comfortOffset}. See docs/ORIGINAL_FEATURES.md for the original formulas and
 * CHANGES.md for where this version deliberately differs.
 */
public final class TemperatureModel {
    private final TemperatureTuning t;

    public TemperatureModel(TemperatureTuning tuning) {
        this.t = tuning;
    }

    public TemperatureTuning tuning() {
        return t;
    }

    /**
     * What the environment feels like and how quickly the body responds to it.
     *
     * @param ambient  felt ambient temperature; the body drifts towards ambient + comfortOffset
     * @param riseRate degrees per update the body may warm (before amplification)
     * @param fallRate degrees per update the body may cool (before amplification)
     */
    public record Ambient(double ambient, double riseRate, double fallRate) {
        public double targetBodyTemp(TemperatureTuning tuning) {
            return ambient + tuning.comfortOffset();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Helpers the Minecraft side uses while sampling the world
    // ------------------------------------------------------------------------------------------

    /**
     * Converts a vanilla biome temperature (about -0.7 to 2.0) to an ambient temperature in °C,
     * using the original's sine curve: 0.8 (plains) is about 26 °C, 2.0 (desert) is the maximum.
     *
     * <p>Fix: the original multiplied the sine of a negative value by the (negative) cold minimum,
     * which made freezing biomes warmer than temperature 0. Here negative values get colder.
     */
    public static double biomeAmbient(double vanillaTemp, double hotMax, double coldMin) {
        double clamped = Math.max(-2.0, Math.min(2.0, vanillaTemp));
        double s = Math.sin(Math.toRadians(clamped * 45.0)); // -1 at -2.0, +1 at 2.0
        return s >= 0 ? s * hotMax : -s * coldMin;
    }

    /** {@link #biomeAmbient(double, double, double)} with this model's tuning. */
    public double biomeAmbient(double vanillaTemp) {
        return biomeAmbient(vanillaTemp, t.biomeHotMax(), t.biomeColdMin());
    }

    /**
     * Heat felt from a source of temperature {@code temp} at distance {@code dist}, falling off with
     * the square of the distance and reaching 0 at the corner of the scanned cube of half-size
     * {@code range} (same formula as the original {@code getTempFalloff}).
     */
    public static double heatFalloff(double temp, double dist, int range) {
        double max = Math.sqrt(3.0 * range * range);
        if (dist >= max) {
            return 0;
        }
        return temp - temp * (dist * dist) / (max * max);
    }

    /**
     * How far through the warm part of the day it is: 1 in the warmest part (early afternoon),
     * 0 in the coldest part of the night.
     */
    public static double dayWarmth(long dayTime, double scale, double offsetDegrees) {
        double fraction = Math.floorMod(dayTime, 24000L) / 24000.0;
        double wave = Math.sin(Math.toRadians(fraction * 360.0 - offsetDegrees)) * 0.5 + 0.5;
        return clamp(wave * scale, 0, 1);
    }

    // ------------------------------------------------------------------------------------------
    // Main calculation
    // ------------------------------------------------------------------------------------------

    /**
     * Felt ambient temperature and drift rates.
     *
     * @param env      the sampled surroundings
     * @param bodyTemp current body temperature (Fire Protection only slows warming while warm)
     */
    public Ambient ambient(Environment env, double bodyTemp) {
        double temp = env.biomeAmbient();
        double rise = t.baseRate();
        double fall = t.baseRate();

        temp = applyAltitude(temp, env);
        temp -= env.cooling();

        if (env.sleeping()) {
            temp += t.sleepingWarmth();
        }

        // Rain only cools you while you are out in it (the original cooled even indoors).
        boolean exposedToRain = env.raining() && !env.arid() && env.canSeeSky();
        if (exposedToRain) {
            temp -= t.rainCooling();
            fall = Math.max(fall, t.exposedRainRate());
        }

        if (env.isDay() && !env.canSeeSky() && !env.raining()) {
            temp -= t.shadeCooling();
        }

        if (env.hasDaylightCycle()) {
            double night = 1.0 - dayWarmth(env.dayTime(), t.dayCurveScale(), t.dayCurveOffsetDegrees());
            temp -= night * t.nightCooling();
            if (env.arid()) {
                temp -= night * t.aridNightExtraCooling();
            }
        }

        // Huddling: warm mobs close by pull a cold ambient half-way up.
        if (!Double.isNaN(env.nearbyMobBodyTemp())) {
            double mobWarmth = env.nearbyMobBodyTemp() - t.huddleOffset();
            if (temp < mobWarmth) {
                temp = (temp + mobWarmth) / 2.0;
            }
        }

        temp = applyArmor(temp, env);

        if (env.inWater()) {
            if (temp > t.waterCoolCap() + t.waterHotReduction()) {
                temp -= t.waterHotReduction();
            } else if (temp > t.waterCoolCap()) {
                temp = t.waterCoolCap();
            }
            fall = Math.max(fall, t.waterRate());
        }

        // A heat source warmer than the surroundings meets them half-way. 0 or less means "no heat
        // source" (the original started from -999 for the same reason): without this check a
        // freezing ambient would be warmed half-way to 0 by a source that is not there.
        if (env.heatSourceTemp() > 0 && env.heatSourceTemp() > temp) {
            if (env.heatSourceTemp() > temp + t.strongHeatMargin()) {
                rise = Math.max(rise, t.strongHeatRate());
            }
            temp = (temp + env.heatSourceTemp()) / 2.0;
        }

        if (env.nearLava()) {
            rise = Math.max(rise, t.nearLavaRate());
        }

        if (!env.fireResistant()) {
            if (env.inLava()) {
                temp = t.lavaTemp();
                rise = t.lavaRate();
            } else if (env.onFire()) {
                temp = Math.max(temp, t.burningMinTemp());
                rise = Math.max(rise, t.burningRate());
            }
        }

        if (env.sprinting()) {
            temp += t.sprintingWarmth();
            rise = Math.max(rise, t.sprintRate());
        }

        // Fire Resistance and Fire Protection only act on heat above the comfortable ambient.
        // (The original used 37 here, an ambient that still gives a target body temp of 49.)
        double comfortable = t.comfortableAmbient();
        double fireProtection = fireProtectionFactor(env.fireProtectionLevels());
        if (temp > comfortable) {
            temp = env.fireResistant() ? comfortable : comfortable + (temp - comfortable) * fireProtection;
        }
        if (bodyTemp >= t.normalBodyTemp()) {
            rise *= fireProtection;
        }

        return new Ambient(temp, rise * t.rateMultiplier(), fall * t.rateMultiplier());
    }

    /**
     * One update of body temperature: move towards the target without overshooting it. The step
     * grows with the distance to the target, so extreme surroundings act faster.
     */
    public double drift(double bodyTemp, Ambient ambient) {
        double target = ambient.targetBodyTemp(t);
        double diff = target - bodyTemp;
        double distance = Math.abs(diff);
        double amplify = distance > t.driftAmplifyDistance() ? distance / t.driftAmplifyDistance() : 1.0;

        double next;
        if (diff > 0) {
            next = bodyTemp + Math.min(ambient.riseRate() * amplify, distance);
        } else if (diff < 0) {
            next = bodyTemp - Math.min(ambient.fallRate() * amplify, distance);
        } else {
            next = bodyTemp;
        }
        return clamp(next, t.minBodyTemp(), t.maxBodyTemp());
    }

    /** Convenience: ambient + drift in one call. */
    public double step(double bodyTemp, Environment env) {
        return drift(bodyTemp, ambient(env, bodyTemp));
    }

    // ------------------------------------------------------------------------------------------

    private double applyAltitude(double temp, Environment env) {
        if (!env.hasDaylightCycle()) {
            return temp; // like the original: no altitude effect in sky-less dimensions
        }
        double y = env.y();
        if (y < t.undergroundStartY() && temp < t.undergroundTemp()) {
            double f = fraction(y, t.undergroundStartY(), t.undergroundFullY());
            temp += (t.undergroundTemp() - temp) * f;
        } else if (y > t.highAltitudeStartY() && temp > t.highAltitudeTemp()) {
            double f = fraction(y, t.highAltitudeStartY(), t.highAltitudeFullY());
            temp -= (temp - t.highAltitudeTemp()) * f;
        }
        return temp;
    }

    private double applyArmor(double temp, Environment env) {
        if (env.armor().isEmpty()) {
            return temp;
        }
        ArmorModifier.Exposure exposure;
        if (!env.isDay()) {
            exposure = ArmorModifier.Exposure.NIGHT;
        } else if (env.canSeeSky() && temp > 0) {
            exposure = ArmorModifier.Exposure.SUN;
        } else {
            exposure = ArmorModifier.Exposure.SHADE;
        }
        double multSum = 0;
        double addSum = 0;
        for (ArmorModifier piece : env.armor()) {
            multSum += piece.mult(exposure) - 1.0;
            addSum += piece.add(exposure);
        }
        return temp * (1.0 + multSum) + addSum;
    }

    /** 1 = no protection, 0 = full protection. */
    double fireProtectionFactor(int levels) {
        return clamp(1.0 - levels / t.fireProtectionLevelsForFull(), 0, 1);
    }

    /** 0 at {@code start}, 1 at {@code full}, clamped; works whether full is above or below start. */
    private static double fraction(double y, double start, double full) {
        return clamp((y - start) / (full - start), 0, 1);
    }

    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
