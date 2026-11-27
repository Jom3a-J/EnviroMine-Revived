package enviromine_revived.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SeverityTest {
    @ParameterizedTest
    @CsvSource({
            "25, -3", "30, -3", "30.01, -2", "32, -2", "32.5, -1", "35, -1",
            "35.01, 0", "37, 0", "38.99, 0",
            "39, 1", "41, 2", "42.9, 2", "43, 3", "50, 3"
    })
    void temperatureLevels(double body, int expected) {
        assertEquals(expected, Severity.temperature(body, Severity.TemperatureThresholds.defaults()));
    }

    @ParameterizedTest
    @CsvSource({"100, 0", "30.01, 0", "30, 1", "10.5, 1", "10, 2", "0.01, 2", "0, 3"})
    void hydrationLevels(double hydration, int expected) {
        assertEquals(expected, Severity.hydration(hydration, Severity.HydrationThresholds.defaults()));
    }
}
