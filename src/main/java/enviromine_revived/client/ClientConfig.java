package enviromine_revived.client;

import enviromine_revived.hud.HudAnchor;
import enviromine_revived.hud.TemperatureUnit;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only settings (config/enviromine_revived-client.toml). Each player chooses these for
 * themselves; they never affect gameplay. Editable in game: Mods → EnviroMine Revived → Config.
 */
public final class ClientConfig {
    private ClientConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.EnumValue<HudAnchor> HUD_ANCHOR;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ModConfigSpec.BooleanValue SHOW_NUMBERS;
    public static final ModConfigSpec.EnumValue<TemperatureUnit> TEMPERATURE_UNIT;
    public static final ModConfigSpec.BooleanValue SHOW_IN_CREATIVE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD_ENABLED = b.comment("Show the temperature and hydration HUD.")
                .define("enabled", true);
        HUD_ANCHOR = b.comment("Where the HUD is drawn. STATUS_BARS stacks it above the hunger bar like a vanilla bar.")
                .defineEnum("anchor", HudAnchor.STATUS_BARS);
        HUD_OFFSET_X = b.comment("Moves the HUD right (positive) or left (negative), in GUI pixels.")
                .defineInRange("offset_x", 0, -1000, 1000);
        HUD_OFFSET_Y = b.comment("Moves the HUD down (positive) or up (negative), in GUI pixels.")
                .defineInRange("offset_y", 0, -1000, 1000);
        SHOW_NUMBERS = b.comment("Show numbers (e.g. 37.2° and 87%) instead of bars.")
                .define("show_numbers", false);
        TEMPERATURE_UNIT = b.comment("Unit for the numbers.")
                .defineEnum("temperature_unit", TemperatureUnit.CELSIUS);
        SHOW_IN_CREATIVE = b.comment("Also show the HUD in creative mode (stats do not change there).")
                .define("show_in_creative", false);
        b.pop();
        SPEC = b.build();
    }
}
