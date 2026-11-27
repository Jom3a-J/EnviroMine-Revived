package enviromine_revived.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HudLayoutTest {
    private static final int W = 427, H = 240; // GUI size of a 1280x720 window at GUI scale 3

    @Test
    void rowAddsUp() {
        assertEquals(HudLayout.WIDTH,
                2 * HudLayout.ICON + 2 * HudLayout.GAP + HudLayout.MIDDLE_GAP + 2 * HudLayout.BAR_WIDTH);
    }

    @Test
    void statusBarsSitsAboveTheHungerBar() {
        // Vanilla draws hunger at guiHeight - rightHeight with rightHeight = 39, right-aligned to the hotbar.
        HudLayout.Point p = HudLayout.position(HudAnchor.STATUS_BARS, W, H, 0, 0, 49);
        assertEquals(W / 2 + 91 - HudLayout.WIDTH, p.x());
        assertEquals(H - 49, p.y());
    }

    @Test
    void hotbarAnchorsKeepClearOfTheHotbarAndOffhand() {
        HudLayout.Point left = HudLayout.position(HudAnchor.HOTBAR_LEFT, W, H, 0, 0, 39);
        HudLayout.Point right = HudLayout.position(HudAnchor.HOTBAR_RIGHT, W, H, 0, 0, 39);
        assertTrue(left.x() + HudLayout.WIDTH <= W / 2 - 91 - 29);
        assertTrue(right.x() >= W / 2 + 91 + 29);
    }

    @Test
    void offsetsMoveTheHud() {
        HudLayout.Point base = HudLayout.position(HudAnchor.TOP_LEFT, W, H, 0, 0, 39);
        HudLayout.Point moved = HudLayout.position(HudAnchor.TOP_LEFT, W, H, 10, 20, 39);
        assertEquals(base.x() + 10, moved.x());
        assertEquals(base.y() + 20, moved.y());
    }

    @ParameterizedTest
    @EnumSource(HudAnchor.class)
    void neverLeavesTheScreen(HudAnchor anchor) {
        for (int[] size : new int[][] {{W, H}, {320, 180}, {200, 120}}) {
            for (int[] offset : new int[][] {{0, 0}, {-1000, -1000}, {1000, 1000}}) {
                HudLayout.Point p = HudLayout.position(anchor, size[0], size[1], offset[0], offset[1], 39);
                assertTrue(p.x() >= 0 && p.x() + HudLayout.WIDTH <= size[0], anchor + " x " + p);
                assertTrue(p.y() >= 0 && p.y() + HudLayout.HEIGHT <= size[1], anchor + " y " + p);
            }
        }
    }

    @Test
    void markerColumnCoversTheBar() {
        int width = HudLayout.BAR_WIDTH;
        assertEquals(0, HudLayout.markerColumn(HudLayout.SCALE_MIN, width));
        assertEquals(width - 1, HudLayout.markerColumn(HudLayout.SCALE_MAX, width));
        assertEquals(0, HudLayout.markerColumn(-100, width));
        assertEquals(width - 1, HudLayout.markerColumn(100, width));
        assertTrue(HudLayout.markerColumn(36, width) < HudLayout.markerColumn(38, width));
    }

    @Test
    void temperatureSegmentsTileTheBarInOrder() {
        int width = HudLayout.BAR_WIDTH;
        double[] thresholds = {30, 32, 35, 39, 41, 43};
        int[] colours = {1, 2, 3, 4, 5, 6, 7};
        HudLayout.Segment[] segments = HudLayout.temperatureSegments(width, thresholds, colours);
        assertEquals(7, segments.length);
        assertEquals(0, segments[0].from());
        assertEquals(width, segments[6].to());
        for (int i = 1; i < segments.length; i++) {
            assertEquals(segments[i - 1].to(), segments[i].from());
        }
        // Normal body temperature falls in the middle (normal) zone.
        int normal = HudLayout.markerColumn(37, width);
        assertTrue(normal >= segments[3].from() && normal < segments[3].to());
    }

    @Test
    void hydrationFill() {
        assertEquals(0, HudLayout.hydrationFill(0, 100, 28));
        assertEquals(1, HudLayout.hydrationFill(0.1, 100, 28));
        assertEquals(14, HudLayout.hydrationFill(50, 100, 28));
        assertEquals(28, HudLayout.hydrationFill(100, 100, 28));
        assertEquals(28, HudLayout.hydrationFill(150, 100, 28));
    }

    @Test
    void onlySevereLevelsFlash() {
        for (long t = 0; t < 40; t++) {
            assertFalse(HudLayout.flashOff(0, t));
            assertFalse(HudLayout.flashOff(1, t));
            assertFalse(HudLayout.flashOff(-1, t));
        }
        assertTrue(Stream.iterate(0L, t -> t + 1).limit(20).anyMatch(t -> HudLayout.flashOff(2, t)));
        assertTrue(Stream.iterate(0L, t -> t + 1).limit(10).anyMatch(t -> HudLayout.flashOff(-3, t)));
    }

    @Test
    void textFormats() {
        assertEquals("37.0°", HudLayout.formatTemperature(37, TemperatureUnit.CELSIUS));
        assertEquals("98.6°", HudLayout.formatTemperature(37, TemperatureUnit.FAHRENHEIT));
        assertEquals("87%", HudLayout.formatHydration(87.2, 100));
    }

    @Test
    void hudPackageHasNoGameDependencies() throws IOException {
        Path dir = Path.of("src/main/java/enviromine_revived/hud");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("net.minecraft") || source.contains("net.neoforged"),
                        file.getFileName() + " imports game classes");
            }
        }
    }
}
