package enviromine_revived.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TemperatureModelTest {
    private static final double EPS = 1e-9;
    private final TemperatureTuning tuning = TemperatureTuning.defaults();
    private final TemperatureModel model = new TemperatureModel(tuning);

    /** Plains, noon, clear, open sky, standing still (the builder defaults). */
    private Environment.Builder plainsNoon() {
        return Environment.builder();
    }

    /** Desert (45 °C, no precipitation), noon, open sky. A fresh builder each call. */
    private Environment.Builder desertNoon() {
        return Environment.builder().biomeAmbient(45).arid(true);
    }

    private double ambient(Environment.Builder env) {
        return model.ambient(env.build(), tuning.normalBodyTemp()).ambient();
    }

    @Nested
    class Biome {
        @Test
        void knownValues() {
            assertEquals(45.0, model.biomeAmbient(2.0), EPS);
            assertEquals(0.0, model.biomeAmbient(0.0), EPS);
            assertEquals(45.0 * Math.sin(Math.toRadians(36)), model.biomeAmbient(0.8), EPS);
            assertEquals(-15.0, model.biomeAmbient(-2.0), EPS);
        }

        @Test
        void colderVanillaTemperatureIsNeverWarmer() {
            double previous = Double.NEGATIVE_INFINITY;
            for (double v = -2.0; v <= 2.0001; v += 0.05) {
                double now = model.biomeAmbient(v);
                assertTrue(now >= previous, "not monotonic at vanilla temp " + v);
                previous = now;
            }
        }

        @Test
        void frozenBiomesAreBelowZero() {
            // Regression test for the original's sign bug, which made -0.5 come out at +5.7.
            assertTrue(model.biomeAmbient(-0.5) < 0);
        }
    }

    @Nested
    class TimeAndWeather {
        @Test
        void dayCurveIsWarmestInAfternoonAndColdestAtNight() {
            assertEquals(1.0, TemperatureModel.dayWarmth(8000, 1.25, 30), EPS);
            assertEquals(0.0, TemperatureModel.dayWarmth(20000, 1.25, 30), EPS);
            assertEquals(TemperatureModel.dayWarmth(8000, 1.25, 30),
                    TemperatureModel.dayWarmth(8000 + 24000L * 5, 1.25, 30), EPS);
        }

        @Test
        void plainsNoonIsComfortable() {
            double target = model.ambient(plainsNoon().build(), 37).targetBodyTemp(tuning);
            assertTrue(target > 36 && target < 39, "target " + target);
        }

        @Test
        void nightIsColderAndAridNightsMuchColder() {
            double day = ambient(plainsNoon());
            double night = ambient(plainsNoon().dayTime(18000));
            double aridNight = ambient(plainsNoon().dayTime(18000).arid(true));
            assertTrue(night < day);
            assertEquals(night - tuning.aridNightExtraCooling() * (1 - TemperatureModel.dayWarmth(18000, 1.25, 30)),
                    aridNight, EPS);
        }

        @Test
        void rainCoolsOnlyWhenOutInIt() {
            double dry = ambient(plainsNoon());
            double rainOutside = ambient(plainsNoon().raining(true));
            double rainUnderRoof = ambient(plainsNoon().raining(true).canSeeSky(false));
            assertEquals(dry - tuning.rainCooling(), rainOutside, EPS);
            assertEquals(dry, rainUnderRoof, EPS);

            TemperatureModel.Ambient wet = model.ambient(plainsNoon().raining(true).build(), 37);
            assertEquals(tuning.exposedRainRate(), wet.fallRate(), EPS);
        }

        @Test
        void noRainCoolingInAridBiomes() {
            assertEquals(ambient(plainsNoon().arid(true)), ambient(plainsNoon().arid(true).raining(true)), EPS);
        }

        @Test
        void shadeCoolsDuringTheDay() {
            assertEquals(ambient(plainsNoon()) - tuning.shadeCooling(),
                    ambient(plainsNoon().canSeeSky(false)), EPS);
        }

        @Test
        void desertShadeStillCoolsWhileItRainsElsewhere() {
            // Regression test: world rain never falls in an arid biome, so the sun is still out.
            Environment.Builder desertShade = plainsNoon().biomeAmbient(45).arid(true).canSeeSky(false);
            double dry = ambient(desertShade);
            assertEquals(dry, ambient(desertShade.raining(true)), EPS);
            assertEquals(45 - tuning.shadeCooling(), dry, EPS);
        }

        @Test
        void sleepingAndSprintingWarm() {
            double base = ambient(plainsNoon());
            assertEquals(base + tuning.sleepingWarmth(), ambient(plainsNoon().sleeping(true)), EPS);
            assertEquals(base + tuning.sprintingWarmth(), ambient(plainsNoon().sprinting(true)), EPS);
        }

        @Test
        void noDayCycleOrAltitudeWithoutSky() {
            // Nether-like: no day/night or altitude effect.
            Environment.Builder nether = plainsNoon().hasDaylightCycle(false).isDay(false).canSeeSky(false)
                    .biomeAmbient(45);
            assertEquals(45.0, ambient(nether.y(10)), EPS);
            assertEquals(45.0, ambient(nether.y(120)), EPS);
        }
    }

    @Nested
    class Altitude {
        @Test
        void deepCavesArePulledTowardsCaveTemperature() {
            Environment.Builder cold = plainsNoon().biomeAmbient(0);
            assertEquals(tuning.undergroundTemp(), ambient(cold.y(0)), EPS);
            assertEquals(tuning.undergroundTemp(), ambient(cold.y(-60)), EPS); // clamped below full depth
            assertEquals(tuning.undergroundTemp() / 2, ambient(cold.y(24)), EPS);
            assertEquals(0.0, ambient(cold.y(60)), EPS);
        }

        @Test
        void undergroundNeverCoolsAHotBiome() {
            assertEquals(45.0, ambient(plainsNoon().biomeAmbient(45).y(0)), EPS);
        }

        @Test
        void highUpIsColder() {
            Environment.Builder b = plainsNoon().biomeAmbient(20);
            assertEquals(20.0, ambient(b.y(tuning.highAltitudeStartY())), EPS);
            assertEquals((20.0 + tuning.highAltitudeTemp()) / 2, ambient(b.y(220)), EPS);
            assertEquals(tuning.highAltitudeTemp(), ambient(b.y(400)), EPS);
        }
    }

    @Nested
    class HeatAndCold {
        @Test
        void heatSourceMeetsAmbientHalfWay() {
            double base = ambient(plainsNoon());
            TemperatureModel.Ambient a = model.ambient(plainsNoon().heatSourceTemp(75).build(), 37);
            assertEquals((base + 75) / 2, a.ambient(), EPS);
            assertEquals(tuning.strongHeatRate(), a.riseRate(), EPS);
        }

        @Test
        void noHeatSourceDoesNotWarmAFreezingPlace() {
            // Regression test: "no heat source" is 0, which must not pull -8 up to -4.
            Environment.Builder freezingNight = plainsNoon().biomeAmbient(0).dayTime(20000);
            assertEquals(-tuning.nightCooling(), ambient(freezingNight), EPS);
            assertEquals(tuning.baseRate(), model.ambient(freezingNight.build(), 37).riseRate(), EPS);
        }

        @Test
        void weakHeatSourceIsIgnored() {
            assertEquals(ambient(plainsNoon()), ambient(plainsNoon().heatSourceTemp(5)), EPS);
        }

        @Test
        void heatFalloffIsFullAtZeroAndGoneAtCubeCorner() {
            assertEquals(75.0, TemperatureModel.heatFalloff(75, 0, 5), EPS);
            assertEquals(0.0, TemperatureModel.heatFalloff(75, Math.sqrt(75), 5), EPS);
            assertTrue(TemperatureModel.heatFalloff(75, 2, 5) > TemperatureModel.heatFalloff(75, 4, 5));
        }

        @Test
        void coolingBlocksSubtract() {
            assertEquals(ambient(plainsNoon()) - 3, ambient(plainsNoon().cooling(3)), EPS);
        }

        @Test
        void lavaAndFire() {
            TemperatureModel.Ambient inLava = model.ambient(plainsNoon().inLava(true).build(), 37);
            assertEquals(tuning.lavaTemp(), inLava.ambient(), EPS);
            assertEquals(tuning.lavaRate(), inLava.riseRate(), EPS);

            TemperatureModel.Ambient burning = model.ambient(plainsNoon().onFire(true).build(), 37);
            assertEquals(tuning.burningMinTemp(), burning.ambient(), EPS);
            assertEquals(tuning.burningRate(), burning.riseRate(), EPS);
        }

        @Test
        void fireResistanceCancelsHeat() {
            TemperatureModel.Ambient a = model.ambient(plainsNoon().inLava(true).fireResistant(true).build(), 37);
            assertEquals(tuning.comfortableAmbient(), a.ambient(), EPS);
            assertEquals(tuning.normalBodyTemp(), a.targetBodyTemp(tuning), EPS);
        }

        @Test
        void fireProtectionScalesHeatAboveComfortable() {
            double comfortable = tuning.comfortableAmbient();
            Environment halfProtected = desertNoon().fireProtectionLevels(9).build();
            Environment fullyProtected = desertNoon().fireProtectionLevels(40).build();

            assertEquals(comfortable + (45 - comfortable) * 0.5, model.ambient(halfProtected, 37).ambient(), EPS);
            assertEquals(comfortable, model.ambient(fullyProtected, 37).ambient(), EPS);
            // It only slows warming once the body is already at or above normal.
            assertEquals(tuning.baseRate(), model.ambient(halfProtected, 36).riseRate(), EPS);
            assertEquals(tuning.baseRate() * 0.5, model.ambient(halfProtected, 38).riseRate(), EPS);
            assertEquals(0.0, model.ambient(fullyProtected, 38).riseRate(), EPS);
        }

        @Test
        void waterCoolsWarmAmbient() {
            assertEquals(tuning.waterCoolCap(), ambient(plainsNoon().biomeAmbient(45).inWater(true)), EPS);
            assertEquals(20.0, ambient(plainsNoon().biomeAmbient(20).inWater(true)), EPS);
            TemperatureModel.Ambient hot = model.ambient(plainsNoon().heatSourceTemp(200).inWater(true).build(), 37);
            assertTrue(hot.fallRate() >= tuning.waterRate());
        }

        @Test
        void huddlingWarmsACold() {
            Environment.Builder coldNight = plainsNoon().biomeAmbient(0).dayTime(20000);
            double alone = ambient(coldNight);
            double mobWarmth = 37 - tuning.huddleOffset();
            assertEquals((alone + mobWarmth) / 2, ambient(coldNight.nearbyMobBodyTemp(37)), EPS);
            // No effect when it is already warmer than the mobs offer.
            assertEquals(ambient(plainsNoon()), ambient(plainsNoon().nearbyMobBodyTemp(37)), EPS);
        }
    }

    @Nested
    class Armor {
        @Test
        void additionsAndMultipliersCombine() {
            double base = ambient(plainsNoon());
            ArmorModifier warm = ArmorModifier.uniform(1, 1);
            assertEquals(base + 4, ambient(plainsNoon().armor(List.of(warm, warm, warm, warm))), EPS);

            ArmorModifier shiny = new ArmorModifier(0, 0, 0, 1.1, 1, 1); // iron-like in the sun
            assertEquals(base * 1.2, ambient(plainsNoon().armor(List.of(shiny, shiny))), EPS);
        }

        @Test
        void insulationWarmsTheColdButNeverPastComfortable() {
            List<ArmorModifier> leather = List.of(ArmorModifier.insulating(3), ArmorModifier.insulating(3),
                    ArmorModifier.insulating(3), ArmorModifier.insulating(3));
            double comfortable = tuning.comfortableAmbient();

            Environment.Builder cold = plainsNoon().biomeAmbient(0);
            assertEquals(12.0, ambient(cold.armor(leather)), EPS);

            Environment.Builder cool = plainsNoon().biomeAmbient(20);
            assertEquals(comfortable, ambient(cool.armor(leather)), EPS);

            // Regression test for the M5 balance pass: warm clothing must not heat you in a desert.
            assertEquals(ambient(desertNoon()), ambient(desertNoon().armor(leather)), EPS);
        }

        @Test
        void usesSunShadeOrNightValue() {
            ArmorModifier piece = new ArmorModifier(3, 2, 1, 1, 1, 1);
            List<ArmorModifier> worn = List.of(piece);
            assertEquals(ambient(plainsNoon()) + 3, ambient(plainsNoon().armor(worn)), EPS);
            assertEquals(ambient(plainsNoon().canSeeSky(false)) + 2,
                    ambient(plainsNoon().canSeeSky(false).armor(worn)), EPS);
            assertEquals(ambient(plainsNoon().dayTime(18000)) + 1,
                    ambient(plainsNoon().dayTime(18000).armor(worn)), EPS);
        }
    }

    @Nested
    class Drift {
        @Test
        void movesTowardsTargetAtBaseRate() {
            TemperatureModel.Ambient a = new TemperatureModel.Ambient(30, 0.001, 0.002); // target 42
            assertEquals(37.001, model.drift(37, a), EPS);
            TemperatureModel.Ambient cold = new TemperatureModel.Ambient(20, 0.001, 0.002); // target 32
            assertEquals(36.998, model.drift(37, cold), EPS);
        }

        @Test
        void farTargetsAreAmplified() {
            TemperatureModel.Ambient a = new TemperatureModel.Ambient(45, 0.001, 0.001); // target 57, 20 away
            assertEquals(37.002, model.drift(37, a), EPS);
        }

        @Test
        void neverOvershoots() {
            TemperatureModel.Ambient a = new TemperatureModel.Ambient(25.5, 1.0, 1.0); // target 37.5
            assertEquals(37.5, model.drift(37, a), EPS);
            assertEquals(37.5, model.drift(37.5, a), EPS);
        }

        @Test
        void bodyTempStaysWithinHardLimits() {
            Environment lava = plainsNoon().inLava(true).build();
            double body = 37;
            for (int i = 0; i < 1000; i++) {
                body = model.step(body, lava);
            }
            assertEquals(tuning.maxBodyTemp(), body, EPS);
        }

        @Test
        void rateMultiplierScalesRates() {
            TemperatureTuning fast = tuning.withRateMultiplier(3.0);
            TemperatureModel.Ambient a = new TemperatureModel(fast).ambient(plainsNoon().build(), 37);
            assertEquals(tuning.baseRate() * 3.0, a.riseRate(), EPS);
            assertEquals(tuning.baseRate() * 3.0, a.fallRate(), EPS);
        }
    }

    /** Checks that the defaults really are gentle, using whole-day simulations. */
    @Nested
    class GentleDefaults {
        private static final int UPDATES_PER_MINUTE = 40; // one update every 30 ticks
        private final HydrationModel hydrationModel = new HydrationModel(HydrationTuning.defaults());

        @Test
        void aFullDayInPlainsNeedsNoCare() {
            double body = 37;
            double hydration = 100;
            // 20 real minutes = one Minecraft day; drink whenever thirsty, as a player would.
            for (int i = 0; i < 20 * UPDATES_PER_MINUTE; i++) {
                long time = (long) (i * 30L) % 24000L;
                Environment env = plainsNoon().dayTime(time).build();
                body = model.step(body, env);
                HydrationModel.Step s = hydrationModel.step(hydration, body, HydrationModel.Activity.IDLE);
                hydration = s.hydration();
                body += s.bodyTempChange();
                assertEquals(0, Severity.temperature(body, Severity.TemperatureThresholds.defaults()),
                        "body " + body + " at update " + i);
            }
        }

        @Test
        void desertNoonIsSafeForTenMinutes() {
            Environment desert = plainsNoon().biomeAmbient(45).arid(true).build();
            double body = 37;
            for (int i = 0; i < 10 * UPDATES_PER_MINUTE; i++) {
                body = model.step(body, desert);
            }
            assertTrue(Severity.temperature(body, Severity.TemperatureThresholds.defaults()) < 2, "body " + body);
        }

        @Test
        void standingInLavaHeatsQuickly() {
            Environment lava = plainsNoon().inLava(true).build();
            double body = model.step(37, lava);
            assertTrue(body > 40, "body " + body);
        }
    }
}
