package enviromine_revived.config;

import java.util.EnumMap;
import java.util.Map;

import enviromine_revived.EnviroMineRevived;
import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.HydrationTuning;
import enviromine_revived.model.Severity;
import enviromine_revived.model.TemperatureModel;
import enviromine_revived.model.TemperatureTuning;
import enviromine_revived.model.WaterType;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * The settings in force right now. Defaults until the server config loads, then rebuilt whenever
 * it is loaded or edited (the file is watched, so changes apply without a restart). On a client
 * this holds the server's values, which NeoForge sends on join.
 *
 * <p>Always read through {@link #get()} and keep the snapshot for the length of one operation, so
 * a reload in the middle cannot mix old and new values.
 */
public final class Settings {
    private Settings() {}

    public record Features(boolean temperature, boolean hydration, boolean effects, boolean damage,
                           boolean drinkByHand) {
        public static final Features ALL = new Features(true, true, true, true, true);
    }

    public record Snapshot(
            TemperatureModel temperature,
            HydrationModel hydration,
            Severity.TemperatureThresholds temperatureThresholds,
            Severity.HydrationThresholds hydrationThresholds,
            Map<WaterType, WaterType.Properties> waterProperties,
            Features features
    ) {
        public Snapshot {
            waterProperties = Map.copyOf(waterProperties);
        }

        public WaterType.Properties water(WaterType type) {
            return waterProperties.get(type);
        }

        public int temperatureLevel(double bodyTemp) {
            return features.temperature() ? Severity.temperature(bodyTemp, temperatureThresholds) : 0;
        }

        public int thirstLevel(double hydration) {
            return features.hydration() ? Severity.hydration(hydration, hydrationThresholds) : 0;
        }

        public static Snapshot defaults() {
            Map<WaterType, WaterType.Properties> water = new EnumMap<>(WaterType.class);
            for (WaterType type : WaterType.values()) {
                water.put(type, type.defaults());
            }
            return new Snapshot(
                    new TemperatureModel(TemperatureTuning.defaults()),
                    new HydrationModel(HydrationTuning.defaults()),
                    Severity.TemperatureThresholds.defaults(),
                    Severity.HydrationThresholds.defaults(),
                    water,
                    Features.ALL);
        }
    }

    private static volatile Snapshot current = Snapshot.defaults();

    public static Snapshot get() {
        return current;
    }

    /** Mod-bus listener for Loading, Reloading and Unloading of any of this mod's configs. */
    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ServerConfig.SPEC) {
            return;
        }
        if (event instanceof ModConfigEvent.Unloading) {
            current = Snapshot.defaults();
        } else {
            current = ServerConfig.read();
            EnviroMineRevived.LOGGER.debug("Server config {}", event instanceof ModConfigEvent.Reloading ? "reloaded" : "loaded");
        }
    }
}
