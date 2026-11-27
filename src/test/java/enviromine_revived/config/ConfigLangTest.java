package enviromine_revived.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import enviromine_revived.model.Doc;
import enviromine_revived.model.HydrationTuning;
import enviromine_revived.model.Severity;
import enviromine_revived.model.TemperatureTuning;
import enviromine_revived.model.WaterType;

/**
 * Every server config entry comes from a tuning record component, so this checks that each one
 * has a {@code @Doc} (needed to build the config) and a name and tooltip in the lang file (needed
 * for the in-game config screen).
 */
class ConfigLangTest {
    private static final Class<?>[] TUNING = {
            TemperatureTuning.class, HydrationTuning.class,
            Severity.TemperatureThresholds.class, Severity.HydrationThresholds.class,
            WaterType.Properties.class
    };

    @Test
    void keysAreSnakeCase() {
        assertEquals("normal_body_temp", ConfigNames.key("normalBodyTemp"));
        assertEquals("cold1", ConfigNames.key("cold1"));
    }

    @Test
    void everyTuningValueIsDocumentedAndTranslated() throws IOException {
        String lang = Files.readString(Path.of("src/main/resources/assets/enviromine_revived/lang/en_us.json"));
        for (Class<?> type : TUNING) {
            for (RecordComponent component : type.getRecordComponents()) {
                String where = type.getSimpleName() + "." + component.getName();
                Doc doc = component.getAnnotation(Doc.class);
                assertNotNull(doc, where + " has no @Doc");
                String key = "enviromine_revived.configuration." + ConfigNames.key(component.getName());
                assertTrue(lang.contains('"' + key + "\":"), "lang is missing " + key);
                assertTrue(lang.contains('"' + key + ".tooltip\":"), "lang is missing " + key + ".tooltip");
            }
        }
    }

    @Test
    void defaultsAreWithinTheirDocumentedRanges() throws ReflectiveOperationException {
        Object[] defaults = {
                TemperatureTuning.defaults(), HydrationTuning.defaults(),
                Severity.TemperatureThresholds.defaults(), Severity.HydrationThresholds.defaults()
        };
        for (Object record : defaults) {
            checkRanges(record);
        }
        for (WaterType type : WaterType.values()) {
            checkRanges(type.defaults());
        }
    }

    private static void checkRanges(Object record) throws ReflectiveOperationException {
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            Doc doc = component.getAnnotation(Doc.class);
            double value = ((Number) component.getAccessor().invoke(record)).doubleValue();
            assertTrue(value >= doc.min() && value <= doc.max(),
                    record.getClass().getSimpleName() + "." + component.getName() + " = " + value
                            + " is outside " + doc.min() + ".." + doc.max());
        }
    }
}
