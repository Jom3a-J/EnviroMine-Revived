package enviromine_revived.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

class HydrationModelTest {
    private static final double EPS = 1e-9;
    private final HydrationTuning tuning = HydrationTuning.defaults();
    private final HydrationModel model = new HydrationModel(tuning);

    @Test
    void idleDrainsBaseAmount() {
        HydrationModel.Step s = model.step(100, 37, HydrationModel.Activity.IDLE);
        assertEquals(100 - tuning.baseDrain(), s.hydration(), EPS);
        assertEquals(0, s.bodyTempChange(), EPS);
    }

    @Test
    void fullToEmptyTakesAboutAnHourWhenIdle() {
        double hydration = 100;
        int updates = 0;
        while (hydration > 0) {
            hydration = model.step(hydration, 37, HydrationModel.Activity.IDLE).hydration();
            updates++;
        }
        double minutes = updates * 1.5 / 60.0;
        assertTrue(minutes > 45 && minutes < 90, "minutes " + minutes);
    }

    @Test
    void sweatingDrainsMoreAndCoolsOnlyWhileHydrated() {
        HydrationModel.Step wellHydrated = model.step(90, 38.5, HydrationModel.Activity.IDLE);
        assertEquals(90 - tuning.baseDrain() - tuning.sweatDrain(), wellHydrated.hydration(), EPS);
        assertEquals(-tuning.sweatCooling(), wellHydrated.bodyTempChange(), EPS);

        HydrationModel.Step thirsty = model.step(50, 38.5, HydrationModel.Activity.IDLE);
        assertEquals(0, thirsty.bodyTempChange(), EPS);
    }

    @Test
    void activitiesAddUp() {
        HydrationModel.Activity busy = new HydrationModel.Activity(true, true, true, true, 0.2);
        double expected = tuning.baseDrain() + tuning.sprintDrain() + tuning.nearLavaDrain()
                + tuning.aridDayDrain() + tuning.hungerEffectDrain() + 0.2;
        assertEquals(100 - expected, model.step(100, 37, busy).hydration(), EPS);
    }

    @Test
    void negativeExtraDrainIsIgnored() {
        HydrationModel.Activity odd = new HydrationModel.Activity(false, false, false, false, -5);
        assertEquals(100 - tuning.baseDrain(), model.step(100, 37, odd).hydration(), EPS);
    }

    @Test
    void multiplierScalesDrainOnce() {
        HydrationTuning doubled = new HydrationTuning(100, 0.04, 38, 0.1, 0.01, 75, 0.05, 0.05, 0.05, 0.1, 2.0);
        HydrationModel m = new HydrationModel(doubled);
        HydrationModel.Activity sprint = new HydrationModel.Activity(true, false, false, false, 0);
        assertEquals(100 - 2 * (0.04 + 0.05), m.step(100, 37, sprint).hydration(), EPS);
        // ...and never affects drinking.
        assertEquals(60, m.drink(50, 37, WaterType.CLEAN.defaults(), new SplittableRandom(1)).hydration(), EPS);
    }

    @Test
    void neverBelowZero() {
        assertEquals(0, model.step(0.01, 40, HydrationModel.Activity.IDLE).hydration(), EPS);
    }

    @Test
    void drinkingCleanWater() {
        HydrationModel.Drink d = model.drink(50, 37, WaterType.CLEAN.defaults(), new SplittableRandom(1));
        assertEquals(60, d.hydration(), EPS);
        assertEquals(0, d.bodyTempChange(), EPS); // 37 is below the cooling threshold
        assertFalse(d.hunger() || d.poison());
        assertEquals(0, d.thirstSeconds());

        assertEquals(100, model.drink(95, 37, WaterType.CLEAN.defaults(), new SplittableRandom(1)).hydration(), EPS);
    }

    @Test
    void drinkingCoolsAWarmBodyButNeverBelowTheThreshold() {
        WaterType.Properties cold = WaterType.COLD.defaults();
        assertEquals(-cold.cooling(), model.drink(50, 39, cold, new SplittableRandom(1)).bodyTempChange(), EPS);
        double nearThreshold = cold.coolOnlyAbove() + 0.02;
        double change = model.drink(50, nearThreshold, cold, new SplittableRandom(1)).bodyTempChange();
        assertEquals(cold.coolOnlyAbove(), nearThreshold + change, EPS);
        // Cold water no longer chills an already cold player (the original cooled down to 30 °C).
        assertEquals(0, model.drink(50, 33, cold, new SplittableRandom(1)).bodyTempChange(), EPS);
    }

    @Test
    void dirtyWaterSometimesMakesYouSick() {
        SplittableRandom rng = new SplittableRandom(42);
        WaterType.Properties dirty = WaterType.DIRTY.defaults();
        int n = 20_000, hunger = 0, poison = 0;
        for (int i = 0; i < n; i++) {
            HydrationModel.Drink d = model.drink(50, 37, dirty, rng);
            if (d.hunger()) hunger++;
            if (d.poison()) poison++;
        }
        assertEquals(dirty.hungerChance(), hunger / (double) n, 0.02);
        assertEquals(dirty.poisonChance(), poison / (double) n, 0.02);
    }

    @Test
    void saltyWaterGivesLessAndCausesThirst() {
        HydrationModel.Drink d = model.drink(50, 37, WaterType.SALTY.defaults(), new SplittableRandom(1));
        assertTrue(d.hydration() - 50 < WaterType.CLEAN.defaults().hydration());
        assertTrue(d.thirstSeconds() > 0);
    }

    @Test
    void cannotDrinkWhenFull() {
        assertFalse(model.canDrink(100));
        assertTrue(model.canDrink(99.9));
    }
}
