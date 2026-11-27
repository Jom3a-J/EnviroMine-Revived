package enviromine_revived.model;

/**
 * How one worn armour piece changes the ambient temperature the player feels.
 *
 * <p>Like the original, a piece has a separate effect in the sun (day, open sky, ambient above 0),
 * in the shade (day otherwise) and at night. The multipliers of all pieces are combined as
 * {@code ambient *= 1 + sum(mult - 1)} and then the additions are summed on top.
 */
public record ArmorModifier(
        double sunAdd, double shadeAdd, double nightAdd,
        double sunMult, double shadeMult, double nightMult
) {
    public static final ArmorModifier NONE = new ArmorModifier(0, 0, 0, 1, 1, 1);

    /** Same addition and multiplier in all conditions. */
    public static ArmorModifier uniform(double add, double mult) {
        return new ArmorModifier(add, add, add, mult, mult, mult);
    }

    double add(Exposure exposure) {
        return switch (exposure) {
            case SUN -> sunAdd;
            case SHADE -> shadeAdd;
            case NIGHT -> nightAdd;
        };
    }

    double mult(Exposure exposure) {
        return switch (exposure) {
            case SUN -> sunMult;
            case SHADE -> shadeMult;
            case NIGHT -> nightMult;
        };
    }

    enum Exposure { SUN, SHADE, NIGHT }
}
