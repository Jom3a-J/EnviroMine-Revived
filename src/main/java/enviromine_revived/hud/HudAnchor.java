package enviromine_revived.hud;

/** Where the HUD is drawn. The offsets in the client config move it from there. */
public enum HudAnchor {
    /** Stacked above the hunger bar (and air bubbles), like a vanilla status bar. */
    STATUS_BARS,
    /** Left of the hotbar, past the off-hand slot. */
    HOTBAR_LEFT,
    /** Right of the hotbar, past the off-hand slot. */
    HOTBAR_RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
