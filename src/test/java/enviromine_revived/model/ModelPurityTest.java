package enviromine_revived.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Keeps the model package free of Minecraft and NeoForge classes, so it stays unit-testable.
 * Gradle runs tests with the project directory as the working directory.
 */
class ModelPurityTest {
    private static final List<String> FORBIDDEN = List.of("net.minecraft", "net.neoforged", "com.mojang");

    @Test
    void modelHasNoGameDependencies() throws IOException {
        Path dir = Path.of("src/main/java/enviromine_revived/model");
        assertTrue(Files.isDirectory(dir), "model sources not found at " + dir.toAbsolutePath());
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String prefix : FORBIDDEN) {
                    assertFalse(source.contains(prefix), file.getFileName() + " mentions " + prefix);
                }
            }
        }
    }
}
