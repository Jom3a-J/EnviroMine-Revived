package enviromine_revived.hud;

/** Unit for the HUD's number mode. The mod always works in Celsius internally. */
public enum TemperatureUnit {
    CELSIUS("C"),
    FAHRENHEIT("F");

    private final String symbol;

    TemperatureUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
