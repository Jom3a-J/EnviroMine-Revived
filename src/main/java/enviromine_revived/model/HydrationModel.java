package enviromine_revived.model;

import java.util.random.RandomGenerator;

/**
 * Hydration (thirst) model. No Minecraft classes.
 *
 * <p>Follows the original {@code EnviroDataTracker.updateData} hydration block: a constant base
 * drain, extra drain for sweating, sprinting, lava, arid days and the Hunger effect, and a little
 * sweat cooling while well hydrated.
 */
public final class HydrationModel {
    private final HydrationTuning t;

    public HydrationModel(HydrationTuning tuning) {
        this.t = tuning;
    }

    public HydrationTuning tuning() {
        return t;
    }

    /** What is happening to the player this update, as far as thirst is concerned. */
    public record Activity(
            boolean sprinting,
            boolean nearLava,
            boolean aridDaytime,
            boolean hungerEffect,
            /** Extra drain from data-driven sources (biome overrides, the Thirst effect, ...). */
            double extraDrain
    ) {
        public static final Activity IDLE = new Activity(false, false, false, false, 0);
    }

    /** Result of one update. {@code bodyTempChange} is to be added to body temperature. */
    public record Step(double hydration, double bodyTempChange) {}

    /** Result of one drink. Effect flags are already rolled; the Minecraft side applies them. */
    public record Drink(
            double hydration,
            double bodyTempChange,
            boolean hunger,
            boolean poison,
            int thirstSeconds
    ) {}

    /**
     * One update.
     *
     * <p>Unlike the original, the global multiplier is applied once (the original applied it twice
     * to the activity drain) and only to drains, never to drinking.
     */
    public Step step(double hydration, double bodyTemp, Activity activity) {
        double drain = t.baseDrain();
        double bodyChange = 0;

        if (bodyTemp >= t.sweatBodyTemp()) {
            drain += t.sweatDrain();
            if (hydration >= t.sweatCoolingMinHydration()) {
                bodyChange -= t.sweatCooling();
            }
        }
        if (activity.sprinting()) {
            drain += t.sprintDrain();
        }
        if (activity.nearLava()) {
            drain += t.nearLavaDrain();
        }
        if (activity.aridDaytime()) {
            drain += t.aridDayDrain();
        }
        if (activity.hungerEffect()) {
            drain += t.hungerEffectDrain();
        }
        drain += Math.max(0, activity.extraDrain());

        double next = clamp(hydration - drain * t.drainMultiplier());
        return new Step(next, bodyChange);
    }

    /**
     * Drinking once.
     *
     * @param random source for the effect rolls (Minecraft's RandomSource is adapted to this in M4;
     *               tests pass a seeded generator)
     */
    public Drink drink(double hydration, double bodyTemp, WaterType.Properties water,
                       RandomGenerator random) {
        double next = clamp(hydration + water.hydration());
        double bodyChange = 0;
        if (bodyTemp >= water.coolOnlyAbove()) {
            // Never cool below the threshold by drinking.
            bodyChange = -Math.min(water.cooling(), bodyTemp - water.coolOnlyAbove());
        }
        boolean hunger = water.hungerChance() > 0 && random.nextDouble() < water.hungerChance();
        boolean poison = water.poisonChance() > 0 && random.nextDouble() < water.poisonChance();
        return new Drink(next, bodyChange, hunger, poison, water.thirstSeconds());
    }

    /** Whether drinking is possible at all (the original refused at full hydration). */
    public boolean canDrink(double hydration) {
        return hydration < t.max();
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(t.max(), v));
    }
}
