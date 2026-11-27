package enviromine_revived.model;

/**
 * The four kinds of drinkable water, with their gentle default properties.
 *
 * <p>Which water a place gives is decided by {@link WaterClassifier}; these defaults can be
 * overridden by data packs (M5) by building a different {@link Properties}.
 */
public enum WaterType {
    //                         sip, bottle, cooling, coolAbove, hunger, poison, thirst seconds
    CLEAN(new Properties(10.0, 25.0, 0.05, 37.05, 0.0, 0.0, 0)),
    /** Original cooled the body down to 30.1 °C, which made drinking it while freezing harmful. */
    COLD(new Properties(10.0, 25.0, 0.10, 37.05, 0.0, 0.0, 0)),
    /** Original: 50 % Hunger and 25 % Poison. Poison chance lowered for vanilla-plus. */
    DIRTY(new Properties(10.0, 25.0, 0.05, 37.05, 0.5, 0.1, 0)),
    /** Salt water barely quenches and makes you thirsty for a while (the Thirst effect). */
    SALTY(new Properties(5.0, 10.0, 0.05, 37.05, 0.0, 0.0, 30));

    private final Properties defaults;

    WaterType(Properties defaults) {
        this.defaults = defaults;
    }

    public Properties defaults() {
        return defaults;
    }

    /**
     * @param sipHydration    hydration from drinking straight from water or a cauldron
     * @param bottleHydration hydration from a whole bottle (the original gave 25, or 10 if salty)
     * @param cooling         body temperature removed per drink...
     * @param coolOnlyAbove   ...but only while the body is at least this warm
     * @param hungerChance    chance (0..1) of the vanilla Hunger effect
     * @param poisonChance    chance (0..1) of the vanilla Poison effect
     * @param thirstSeconds   seconds of the Thirst effect (faster hydration loss); 0 for none
     */
    public record Properties(
            double sipHydration,
            double bottleHydration,
            double cooling,
            double coolOnlyAbove,
            double hungerChance,
            double poisonChance,
            int thirstSeconds
    ) {
        public double hydration(boolean fromBottle) {
            return fromBottle ? bottleHydration : sipHydration;
        }
    }
}
