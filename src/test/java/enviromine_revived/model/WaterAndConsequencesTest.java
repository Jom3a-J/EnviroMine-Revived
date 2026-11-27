package enviromine_revived.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaterAndConsequencesTest {
    private static WaterType water(boolean dirty, boolean salty, boolean cold, double y, boolean heated) {
        return WaterClassifier.classify(new WaterClassifier.Hints(dirty, salty, cold, y, 63, heated));
    }

    @Test
    void ordinarySurfaceWaterIsClean() {
        // Regression test: the original's y < seaLevel / 0.75 made this dirty.
        assertEquals(WaterType.CLEAN, water(false, false, false, 62, false));
        assertEquals(WaterType.CLEAN, water(false, false, false, 80, false));
    }

    @Test
    void biomesDecideTheType() {
        assertEquals(WaterType.DIRTY, water(true, false, false, 62, false));
        assertEquals(WaterType.SALTY, water(false, true, false, 62, false));
        assertEquals(WaterType.COLD, water(false, false, true, 62, false));
    }

    @Test
    void heightMatters() {
        assertEquals(WaterType.DIRTY, water(false, false, false, 40, false)); // stagnant cave water
        assertEquals(WaterType.CLEAN, water(false, false, false, 48, false));
        assertEquals(WaterType.COLD, water(false, false, false, 130, false)); // mountain spring
    }

    @Test
    void dirtyBeatsSaltyBeatsCold() {
        assertEquals(WaterType.DIRTY, water(true, true, true, 62, false));
        assertEquals(WaterType.SALTY, water(false, true, true, 62, false));
    }

    @Test
    void boiledWaterIsAlwaysClean() {
        assertEquals(WaterType.CLEAN, water(true, true, true, 10, true));
    }

    @Test
    void levelOneIsOnlyAWarning() {
        assertEquals(Consequences.Outcome.NONE, Consequences.decide(1, 1, false));
        assertEquals(Consequences.Outcome.NONE, Consequences.decide(-1, 0, false));
        assertEquals(Consequences.Outcome.NONE, Consequences.decide(0, 0, false));
    }

    @Test
    void effectsEscalate() {
        assertEquals(new Consequences.Outcome(0, -1, -1, Consequences.Hazard.NONE), Consequences.decide(2, 0, false));
        assertEquals(new Consequences.Outcome(1, -1, -1, Consequences.Hazard.HEAT), Consequences.decide(3, 0, false));
        assertEquals(new Consequences.Outcome(-1, 0, -1, Consequences.Hazard.NONE), Consequences.decide(-2, 0, false));
        assertEquals(new Consequences.Outcome(-1, 1, -1, Consequences.Hazard.COLD), Consequences.decide(-3, 0, false));
        assertEquals(new Consequences.Outcome(-1, -1, 0, Consequences.Hazard.NONE), Consequences.decide(0, 2, false));
        assertEquals(new Consequences.Outcome(-1, -1, 1, Consequences.Hazard.THIRST), Consequences.decide(0, 3, false));
    }

    @Test
    void temperatureDamageTakesPriorityOverThirst() {
        Consequences.Outcome both = Consequences.decide(3, 3, false);
        assertEquals(Consequences.Hazard.HEAT, both.damage());
        assertEquals(1, both.dehydrationAmplifier());
    }

    @Test
    void fireResistancePreventsHeatstroke() {
        Consequences.Outcome o = Consequences.decide(3, 0, true);
        assertEquals(-1, o.heatstrokeAmplifier());
        assertEquals(Consequences.Hazard.NONE, o.damage());
    }

    @Test
    void damageFollowsVanillaStarvationRules() {
        assertFalse(Consequences.damageAllowed(0, 20));   // peaceful
        assertTrue(Consequences.damageAllowed(1, 11));    // easy, above 5 hearts
        assertFalse(Consequences.damageAllowed(1, 10));
        assertTrue(Consequences.damageAllowed(2, 2));     // normal, down to half a heart
        assertFalse(Consequences.damageAllowed(2, 1));
        assertTrue(Consequences.damageAllowed(3, 1));     // hard
    }

    @Test
    void warningsOnlyWhenGettingWorse() {
        assertTrue(Consequences.worsened(0, 1));
        assertTrue(Consequences.worsened(1, 2));
        assertTrue(Consequences.worsened(0, -1));
        assertTrue(Consequences.worsened(-2, -3));
        assertFalse(Consequences.worsened(2, 2));
        assertFalse(Consequences.worsened(2, 1));
        assertFalse(Consequences.worsened(-1, 0));
        assertTrue(Consequences.worsened(1, -2)); // a swing from hot to cold is warned about too
        assertFalse(Consequences.worsened(3, 0));
    }
}
