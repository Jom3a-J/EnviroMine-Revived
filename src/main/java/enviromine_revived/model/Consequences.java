package enviromine_revived.model;

/**
 * What happens to a player at each severity level. Gentler than the original and shaped like
 * vanilla hunger: first a warning, then a slowing effect, then slow damage that respects
 * difficulty.
 *
 * <table>
 *   <caption>Levels</caption>
 *   <tr><th>Level</th><th>Heat / cold / thirst</th></tr>
 *   <tr><td>1</td><td>Warning only (HUD colour and an action-bar message)</td></tr>
 *   <tr><td>2</td><td>Heatstroke / Hypothermia / Dehydration effect I</td></tr>
 *   <tr><td>3</td><td>The same effect at II, plus damage every few seconds</td></tr>
 * </table>
 *
 * <p>The original applied its effects from level 1 and dealt 2 hearts every 1.5 seconds at the
 * worst levels (and at zero hydration), with no difficulty limits.
 */
public final class Consequences {
    private Consequences() {}

    /** What kind of harm, used to pick the damage type (and so the death message). */
    public enum Hazard { NONE, HEAT, COLD, THIRST }

    /**
     * @param heatstrokeAmplifier  -1 for none, otherwise the effect amplifier (0 = level I)
     * @param hypothermiaAmplifier -1 for none
     * @param dehydrationAmplifier -1 for none
     * @param damage               what (if anything) deals damage this time
     */
    public record Outcome(int heatstrokeAmplifier, int hypothermiaAmplifier,
                          int dehydrationAmplifier, Hazard damage) {
        public static final Outcome NONE = new Outcome(-1, -1, -1, Hazard.NONE);
    }

    /**
     * @param temperatureLevel {@link Severity#temperature}: -3 (cold) .. 3 (hot)
     * @param thirstLevel      {@link Severity#hydration}: 0 .. 3
     * @param fireResistant    Fire Resistance prevents heatstroke entirely, as in the original
     */
    public static Outcome decide(int temperatureLevel, int thirstLevel, boolean fireResistant) {
        int heat = fireResistant ? 0 : Math.max(0, temperatureLevel);
        int cold = Math.max(0, -temperatureLevel);

        int heatstroke = heat >= 2 ? heat - 2 : -1;
        int hypothermia = cold >= 2 ? cold - 2 : -1;
        int dehydration = thirstLevel >= 2 ? thirstLevel - 2 : -1;

        // At most one source of damage per interval; body temperature is the more urgent one.
        Hazard damage = Hazard.NONE;
        if (heat >= 3) {
            damage = Hazard.HEAT;
        } else if (cold >= 3) {
            damage = Hazard.COLD;
        } else if (thirstLevel >= 3) {
            damage = Hazard.THIRST;
        }
        return new Outcome(heatstroke, hypothermia, dehydration, damage);
    }

    /** Damage per hit: half a heart, like vanilla starvation. */
    public static final float DAMAGE = 1.0f;

    /** Ticks between hits: 90 (4.5 s), a little slower than vanilla starvation's 80. */
    public static final int DAMAGE_INTERVAL_TICKS = 90;

    /**
     * Whether damage may be dealt, mirroring vanilla starvation: never on Peaceful, down to
     * 5 hearts on Easy, down to half a heart on Normal, and all the way on Hard.
     *
     * @param difficulty 0 Peaceful, 1 Easy, 2 Normal, 3 Hard (Minecraft's Difficulty ids)
     */
    public static boolean damageAllowed(int difficulty, float health) {
        return switch (difficulty) {
            case 0 -> false;
            case 1 -> health > 10.0f;
            case 2 -> health > 1.0f;
            default -> true;
        };
    }

    /**
     * Whether a warning should be shown: when a stat gets worse, or swings from hot to cold (or
     * back). Recovering or staying at the same level never repeats the message.
     */
    public static boolean worsened(int oldLevel, int newLevel) {
        if (newLevel == 0) {
            return false;
        }
        return Integer.signum(newLevel) != Integer.signum(oldLevel) || Math.abs(newLevel) > Math.abs(oldLevel);
    }
}
