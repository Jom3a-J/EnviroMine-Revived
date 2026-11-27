package enviromine_revived.model;

/**
 * Decides which kind of water a place gives, from plain facts about it.
 *
 * <p>Same order as the original {@code EM_EventManager.getWaterType}: dirty beats salty beats cold.
 * The Minecraft side fills in the hints from biome tags (swamps, oceans and beaches, snowy biomes).
 */
public final class WaterClassifier {
    private WaterClassifier() {}

    /**
     * @param dirtyBiome swamp-like biome
     * @param saltyBiome ocean or beach
     * @param coldBiome  snowy or frozen biome
     * @param y          height of the water block
     * @param seaLevel   the dimension's sea level (63 in the overworld)
     * @param heated     a cauldron with a heat source under it: boiled water is always clean
     */
    public record Hints(boolean dirtyBiome, boolean saltyBiome, boolean coldBiome,
                        double y, int seaLevel, boolean heated) {}

    /**
     * Water below this fraction of sea level counts as dirty (stagnant cave water).
     *
     * <p>Fix: the original divided by 0.75 (y below ~85, so nearly all surface water was dirty)
     * where it clearly meant to multiply (y below ~47).
     */
    public static final double UNDERGROUND_FRACTION = 0.75;

    /** Water above this multiple of sea level counts as cold (mountain springs). */
    public static final double HIGH_ALTITUDE_FACTOR = 2.0;

    public static WaterType classify(Hints h) {
        if (h.heated()) {
            return WaterType.CLEAN;
        }
        if (h.dirtyBiome() || h.y() < h.seaLevel() * UNDERGROUND_FRACTION) {
            return WaterType.DIRTY;
        }
        if (h.saltyBiome()) {
            return WaterType.SALTY;
        }
        if (h.coldBiome() || h.y() > h.seaLevel() * HIGH_ALTITUDE_FACTOR) {
            return WaterType.COLD;
        }
        return WaterType.CLEAN;
    }
}
