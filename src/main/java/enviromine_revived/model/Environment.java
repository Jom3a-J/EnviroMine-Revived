package enviromine_revived.model;

import java.util.List;

/**
 * A snapshot of everything around the player that affects temperature, as plain values.
 *
 * <p>The Minecraft side (M2) samples the world and fills this in. Keeping it a plain record means
 * the model can be tested without starting Minecraft. Use {@link #builder()}: its defaults describe
 * a mild plains biome at noon, standing still, in clear weather.
 *
 * @param biomeAmbient       ambient from the biome, already converted with
 *                           {@link TemperatureModel#biomeAmbient} and averaged over nearby columns
 * @param arid               the biome has no precipitation (deserts, badlands, savanna)
 * @param hasDaylightCycle   the dimension has a sky and a day/night cycle (false in the Nether)
 * @param dayTime            {@code Level#getDayTime()}, any value; only {@code % 24000} matters
 * @param isDay              it is daytime and the dimension has a sky
 * @param raining            it is raining (or snowing) in the world
 * @param canSeeSky          open sky directly above the player
 * @param y                  the player's height
 * @param heatSourceTemp     hottest nearby heat source after distance falloff
 *                           ({@link TemperatureModel#heatFalloff}), or 0 if none; values of 0 or
 *                           less are ignored
 * @param cooling            summed cooling from nearby cold blocks after falloff (0 if none)
 * @param nearLava           lava is within the scan range
 * @param nearbyMobBodyTemp  average body temperature of nearby warm-blooded mobs, or NaN if none
 * @param armor              modifiers of the worn armour pieces (empty if none)
 * @param fireProtectionLevels sum of Fire Protection levels over worn armour
 */
public record Environment(
        double biomeAmbient,
        boolean arid,
        boolean hasDaylightCycle,
        long dayTime,
        boolean isDay,
        boolean raining,
        boolean canSeeSky,
        double y,
        boolean inWater,
        boolean inLava,
        boolean onFire,
        boolean fireResistant,
        boolean sprinting,
        boolean sleeping,
        double heatSourceTemp,
        double cooling,
        boolean nearLava,
        double nearbyMobBodyTemp,
        List<ArmorModifier> armor,
        int fireProtectionLevels
) {
    public Environment {
        armor = List.copyOf(armor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.biomeAmbient = biomeAmbient;
        b.arid = arid;
        b.hasDaylightCycle = hasDaylightCycle;
        b.dayTime = dayTime;
        b.isDay = isDay;
        b.raining = raining;
        b.canSeeSky = canSeeSky;
        b.y = y;
        b.inWater = inWater;
        b.inLava = inLava;
        b.onFire = onFire;
        b.fireResistant = fireResistant;
        b.sprinting = sprinting;
        b.sleeping = sleeping;
        b.heatSourceTemp = heatSourceTemp;
        b.cooling = cooling;
        b.nearLava = nearLava;
        b.nearbyMobBodyTemp = nearbyMobBodyTemp;
        b.armor = armor;
        b.fireProtectionLevels = fireProtectionLevels;
        return b;
    }

    /** Mutable builder; every setter returns this so calls can be chained. */
    public static final class Builder {
        private double biomeAmbient = TemperatureModel.biomeAmbient(0.8, 45.0, -15.0); // plains
        private boolean arid = false;
        private boolean hasDaylightCycle = true;
        private long dayTime = 6000; // noon
        private boolean isDay = true;
        private boolean raining = false;
        private boolean canSeeSky = true;
        private double y = 70;
        private boolean inWater = false;
        private boolean inLava = false;
        private boolean onFire = false;
        private boolean fireResistant = false;
        private boolean sprinting = false;
        private boolean sleeping = false;
        private double heatSourceTemp = 0;
        private double cooling = 0;
        private boolean nearLava = false;
        private double nearbyMobBodyTemp = Double.NaN;
        private List<ArmorModifier> armor = List.of();
        private int fireProtectionLevels = 0;

        private Builder() {}

        public Builder biomeAmbient(double v) { biomeAmbient = v; return this; }
        public Builder arid(boolean v) { arid = v; return this; }
        public Builder hasDaylightCycle(boolean v) { hasDaylightCycle = v; return this; }
        /** Sets the time and derives isDay from it (day is 0 to 12999, like vanilla's day check). */
        public Builder dayTime(long v) { dayTime = v; isDay = Math.floorMod(v, 24000L) < 13000; return this; }
        public Builder isDay(boolean v) { isDay = v; return this; }
        public Builder raining(boolean v) { raining = v; return this; }
        public Builder canSeeSky(boolean v) { canSeeSky = v; return this; }
        public Builder y(double v) { y = v; return this; }
        public Builder inWater(boolean v) { inWater = v; return this; }
        public Builder inLava(boolean v) { inLava = v; return this; }
        public Builder onFire(boolean v) { onFire = v; return this; }
        public Builder fireResistant(boolean v) { fireResistant = v; return this; }
        public Builder sprinting(boolean v) { sprinting = v; return this; }
        public Builder sleeping(boolean v) { sleeping = v; return this; }
        public Builder heatSourceTemp(double v) { heatSourceTemp = v; return this; }
        public Builder cooling(double v) { cooling = v; return this; }
        public Builder nearLava(boolean v) { nearLava = v; return this; }
        public Builder nearbyMobBodyTemp(double v) { nearbyMobBodyTemp = v; return this; }
        public Builder armor(List<ArmorModifier> v) { armor = v; return this; }
        public Builder fireProtectionLevels(int v) { fireProtectionLevels = v; return this; }

        public Environment build() {
            return new Environment(biomeAmbient, arid, hasDaylightCycle, dayTime, isDay, raining,
                    canSeeSky, y, inWater, inLava, onFire, fireResistant, sprinting, sleeping,
                    heatSourceTemp, cooling, nearLava, nearbyMobBodyTemp, armor, fireProtectionLevels);
        }
    }
}
