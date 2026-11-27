package enviromine_revived.config;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.HydrationTuning;
import enviromine_revived.model.Severity;
import enviromine_revived.model.TemperatureModel;
import enviromine_revived.model.TemperatureTuning;
import enviromine_revived.model.WaterType;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay settings: {@code config/enviromine_revived-server.toml} (on NeoForge 21.1 server configs
 * live in the config folder) and sent to every client that joins, so the HUD agrees with the server. Every balance number
 * from the tuning records is here; block, armour and biome values are in data packs instead.
 */
public final class ServerConfig {
    private ServerConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue TEMPERATURE_ENABLED;
    private static final ModConfigSpec.BooleanValue HYDRATION_ENABLED;
    private static final ModConfigSpec.BooleanValue EFFECTS_ENABLED;
    private static final ModConfigSpec.BooleanValue DAMAGE_ENABLED;
    private static final ModConfigSpec.BooleanValue DRINK_BY_HAND;

    private static final RecordConfig<TemperatureTuning> TEMPERATURE;
    private static final RecordConfig<HydrationTuning> HYDRATION;
    private static final RecordConfig<Severity.TemperatureThresholds> TEMPERATURE_THRESHOLDS;
    private static final RecordConfig<Severity.HydrationThresholds> HYDRATION_THRESHOLDS;
    private static final Map<WaterType, RecordConfig<WaterType.Properties>> WATER = new EnumMap<>(WaterType.class);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Turn whole systems on or off.").push("features");
        TEMPERATURE_ENABLED = b.comment("Body temperature. When off, it stays normal.").define("temperature", true);
        HYDRATION_ENABLED = b.comment("Hydration (thirst). When off, it stays full and drinking does nothing.").define("hydration", true);
        EFFECTS_ENABLED = b.comment("Heatstroke, Hypothermia and Dehydration effects (and the damage below).").define("effects", true);
        DAMAGE_ENABLED = b.comment("Damage at the most severe levels.").define("damage", true);
        DRINK_BY_HAND = b.comment("Drink by right-clicking water or a cauldron with an empty hand.").define("drink_by_hand", true);
        b.pop();

        b.comment("Body temperature model. Temperatures in °C; one update is 1.5 seconds.").push("temperature");
        TEMPERATURE = new RecordConfig<>(TemperatureTuning.class, TemperatureTuning.defaults(), b);
        b.pop();

        b.comment("Hydration model. One update is 1.5 seconds.").push("hydration");
        HYDRATION = new RecordConfig<>(HydrationTuning.class, HydrationTuning.defaults(), b);
        b.pop();

        b.comment("Where warnings, effects and damage start.").push("thresholds");
        b.push("body_temperature");
        TEMPERATURE_THRESHOLDS = new RecordConfig<>(Severity.TemperatureThresholds.class,
                Severity.TemperatureThresholds.defaults(), b);
        b.pop();
        b.push("hydration_levels");
        HYDRATION_THRESHOLDS = new RecordConfig<>(Severity.HydrationThresholds.class,
                Severity.HydrationThresholds.defaults(), b);
        b.pop();
        b.pop();

        b.comment("What each kind of water does. Which water a place gives is set by biome tags and data packs.").push("water");
        for (WaterType type : WaterType.values()) {
            b.push(type.name().toLowerCase(Locale.ROOT));
            WATER.put(type, new RecordConfig<>(WaterType.Properties.class, type.defaults(), b));
            b.pop();
        }
        b.pop();

        SPEC = b.build();
    }

    /** Builds the live settings from the loaded config. */
    static Settings.Snapshot read() {
        Map<WaterType, WaterType.Properties> water = new EnumMap<>(WaterType.class);
        WATER.forEach((type, config) -> water.put(type, config.read()));
        return new Settings.Snapshot(
                new TemperatureModel(TEMPERATURE.read()),
                new HydrationModel(HYDRATION.read()),
                TEMPERATURE_THRESHOLDS.read(),
                HYDRATION_THRESHOLDS.read(),
                water,
                new Settings.Features(TEMPERATURE_ENABLED.get(), HYDRATION_ENABLED.get(),
                        EFFECTS_ENABLED.get(), DAMAGE_ENABLED.get(), DRINK_BY_HAND.get()));
    }
}
