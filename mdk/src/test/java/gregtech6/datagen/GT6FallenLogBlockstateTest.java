/**
 * Offline pin for task 26-fallenlog-rotation (GitHub #26) — the four fallen-log
 * blockstates carry the vanilla {@code axisBlock} rotation map (the addAxles band
 * form: X = x90/y90, Y = none, Z = x90/y180). The previous map (x-only 90 / y-only
 * 90) tipped the Y-column cube_column model onto Z for axis=x and spun the still-
 * upright log in place for axis=z — the fallen rows rendered standing up. Reads the
 * committed generated tree (the {@link GT6DynamoBowlRenderDatagenTest} classpath
 * form — no registry, no datagen run).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6FallenLogBlockstateTest {

    /** The four fallen-log rows (GT6SurfaceBlocks.FALLEN_LOGS paths, read-only). */
    private static final List<String> ROWS = List.of("dead_log", "rotten_log", "mossy_log", "frozen_log");

    /** rotationX/rotationY per axis variant, ordered {axis=x, axis=y, axis=z} (0 = omitted key). */
    private static final int[] X_ROT = {90, 0, 90};
    private static final int[] Y_ROT = {90, 0, 180};

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6FallenLogBlockstateTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The three axis variants, each pinned to the axle-band rotation values. */
    @Test
    void fallenLogBlockstatesPinTheAxisRotationMap() throws Exception {
        for (String tRow : ROWS) {
            JsonObject tState = generatedJson("assets/gt6/blockstates/" + tRow + ".json");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(3, tVariants.size(), tRow + ": exactly the three axis variants");
            String[] tAxes = {"axis=x", "axis=y", "axis=z"};
            for (int i = 0; i < 3; i++) {
                JsonObject tVariant = tVariants.getAsJsonObject(tAxes[i]);
                assertEquals("gt6:block/" + tRow, tVariant.get("model").getAsString(),
                        tRow + " " + tAxes[i] + ": the shared cube_column model");
                assertEquals(X_ROT[i],
                        tVariant.has("x") ? tVariant.get("x").getAsInt() : 0,
                        tRow + " " + tAxes[i] + ": the rotationX");
                assertEquals(Y_ROT[i],
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow + " " + tAxes[i] + ": the rotationY");
            }
        }
    }

    /** axis=y (the standing stub variant, upstream WorldgenLogDry case 0) stays rotation-free. */
    @Test
    void axisYVariantCarriesNoRotationKeys() throws Exception {
        JsonObject tVariant = generatedJson("assets/gt6/blockstates/dead_log.json")
                .getAsJsonObject("variants").getAsJsonObject("axis=y");
        assertFalse(tVariant.has("x"), "axis=y must not carry an x rotation");
        assertFalse(tVariant.has("y"), "axis=y must not carry a y rotation");
    }
}
