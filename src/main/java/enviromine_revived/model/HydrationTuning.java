package enviromine_revived.model;

/**
 * Every number the hydration model uses. Hydration runs from 0 to {@link #max()}; amounts are per
 * model update (1.5 seconds). Original EnviroMine values are noted where the default differs.
 */
public record HydrationTuning(
        double max,
        /** Always drained, every update. */
        double baseDrain,
        /** Body temperature at or above which the player sweats. */
        double sweatBodyTemp,
        /** Extra drain while sweating. */
        double sweatDrain,
        /** Sweating cools the body by this much per update, but only while well hydrated. */
        double sweatCooling,
        double sweatCoolingMinHydration,
        double sprintDrain,
        double nearLavaDrain,
        /** Extra drain in arid biomes during the day. */
        double aridDayDrain,
        /** Extra drain while the vanilla Hunger effect is active. */
        double hungerEffectDrain,
        /** Global multiplier on all drains (original config: hydrationMult). */
        double drainMultiplier
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
                1.0     // drainMultiplier
        );
    }

    /** A copy with a different global drain multiplier. */
    public HydrationTuning withDrainMultiplier(double multiplier) {
        return new HydrationTuning(max, baseDrain, sweatBodyTemp, sweatDrain, sweatCooling,
                sweatCoolingMinHydration, sprintDrain, nearLavaDrain, aridDayDrain, hungerEffectDrain,
                multiplier);
    }
}
