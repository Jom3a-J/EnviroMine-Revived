package enviromine_revived.model;

/**
 * Every number the hydration model uses. Hydration runs from 0 to {@link #max()}; amounts are per
 * model update (1.5 seconds). Original EnviroMine values are noted where the default differs.
 * Each component's {@link Doc} becomes an entry in the server config.
 */
public record HydrationTuning(
        @Doc(value = "Full hydration.", min = 1, max = 10000) double max,
        @Doc(value = "Hydration lost every update (1.5 s), always.", min = 0, max = 1000) double baseDrain,
        @Doc(value = "Body temperature at or above which you sweat.", min = 0, max = 100) double sweatBodyTemp,
        @Doc(value = "Extra hydration lost per update while sweating.", min = 0, max = 1000) double sweatDrain,
        @Doc(value = "Sweating cools the body by this much per update...", min = 0, max = 100) double sweatCooling,
        @Doc(value = "...but only while hydration is at least this.", min = 0, max = 10000) double sweatCoolingMinHydration,
        @Doc(value = "Extra hydration lost per update while sprinting.", min = 0, max = 1000) double sprintDrain,
        @Doc(value = "Extra hydration lost per update near lava.", min = 0, max = 1000) double nearLavaDrain,
        @Doc(value = "Extra hydration lost per update in biomes without rain, by day.", min = 0, max = 1000) double aridDayDrain,
        @Doc(value = "Extra hydration lost per update with the Hunger effect.", min = 0, max = 1000) double hungerEffectDrain,
        @Doc(value = "Multiplies every hydration loss above.", min = 0, max = 100) double drainMultiplier,
        @Doc(value = "Extra hydration lost per update and level of the Thirst effect (salt water).", min = 0, max = 1000) double thirstEffectDrain
) {
    public static HydrationTuning defaults() {
        return new HydrationTuning(
                100.0,  // max
                0.04,   // baseDrain (original 0.05: full to empty in ~50 min; now ~62 min)
                38.0,   // sweatBodyTemp (original 38.02)
                0.1,    // sweatDrain
                0.01,   // sweatCooling
                75.0,   // sweatCoolingMinHydration
                0.05,   // sprintDrain
                0.05,   // nearLavaDrain
                0.05,   // aridDayDrain
                0.1,    // hungerEffectDrain
                1.0,    // drainMultiplier
                // thirstEffectDrain (original: 1 per update and level, so one salty sip cost ~20;
                // now 30 s of Thirst costs ~8, a little more than the sip gave)
                0.4
        );
    }

    /** A copy with a different global drain multiplier. */
    public HydrationTuning withDrainMultiplier(double multiplier) {
        return new HydrationTuning(max, baseDrain, sweatBodyTemp, sweatDrain, sweatCooling,
                sweatCoolingMinHydration, sprintDrain, nearLavaDrain, aridDayDrain, hungerEffectDrain,
                multiplier, thirstEffectDrain);
    }
}
