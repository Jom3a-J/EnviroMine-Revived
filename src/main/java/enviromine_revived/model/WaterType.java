package enviromine_revived.model;

/**
 * The four kinds of drinkable water, with their gentle default properties.
 *
 * <p>Which water a block counts as is decided on the Minecraft side from biome tags (M4); these
 * defaults can be overridden by data packs (M5) by building a different {@link Properties}.
 */
public enum WaterType {
    //                         hydration, cooling, coolAbove, hunger, poison, thirst seconds
    CLEAN(new Properties(10.0, 0.05, 37.05, 0.0, 0.0, 0)),
    /** Original cooled the body down to 30.1 °C, which made drinking it while freezing harmful. */
    COLD(new Properties(10.0, 0.10, 37.05, 0.0, 0.0, 0)),
    /** Original: 50 % Hunger and 25 % Poison. Poison chance lowered for vanilla-plus. */
    DIRTY(new Properties(10.0, 0.05, 37.05, 0.5, 0.1, 0)),
    /** Salt water barely quenches and makes you thirsty for a while (the "Thirst" effect, M4). */
    SALTY(new Properties(5.0, 0.05, 37.05, 0.0, 0.0, 30));

    private final Properties defaults;

    WaterType(Properties defaults) {
        this.defaults = defaults;
    }

    public Properties defaults() {
        return defaults;
    }

    /**
     * @param hydration       hydration restored per drink
     * @param cooling         body temperature removed per drink...
     * @param coolOnlyAbove   ...but only while the body is at least this warm
     * @param hungerChance    chance (0..1) of the vanilla Hunger effect
     * @param poisonChance    chance (0..1) of the vanilla Poison effect
     * @param thirstSeconds   seconds of the Thirst effect (faster hydration loss); 0 for none
     */
    public record Properties(
            double hydration,
            double cooling,
            double coolOnlyAbove,
            double hungerChance,
            double poisonChance,
            int thirstSeconds
    ) {}
}
