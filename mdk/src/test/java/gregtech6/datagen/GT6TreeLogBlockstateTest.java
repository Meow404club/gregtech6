/**
 * Offline pin for task r4-26b-tree-log-rotation (GitHub #26) — the nine standing
 * tree log blockstates carry the vanilla {@code axisBlock} rotation map (the
 * addAxles/addSurfacePlants band form: X = x90/y90, Y = none, Z = x90/y180). The
 * previous map (x-only 90 / y-only 90) tipped the Y-column cube_column model onto
 * Z for axis=x and spun the still-upright log in place for axis=z — the same bug
 * the r4-26 fallen-log fix ({@link GT6FallenLogBlockstateTest}) already pinned for
 * the surface rows; the standing rows were player-reachable by manual placement.
 * Reads the committed generated tree (the {@link GT6FallenLogBlockstateTest}
 * classpath form — no registry, no datagen run).
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

class GT6TreeLogBlockstateTest {

    /** The nine standing tree rows (GT6TreeKind snakes, read-only). */
    private static final List<String> ROWS = List.of("blue_mahoe_log", "blue_spruce_log", "cinnamon_log",
            "coconut_log", "hazel_log", "maple_log", "rainbowood_log", "rubber_log", "willow_log");

    /** rotationX/rotationY per axis variant, ordered {axis=x, axis=y, axis=z} (0 = omitted key). */
    private static final int[] X_ROT = {90, 0, 90};
    private static final int[] Y_ROT = {90, 0, 180};

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6TreeLogBlockstateTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The three axis variants, each pinned to the axle-band rotation values. */
    @Test
    void treeLogBlockstatesPinTheAxisRotationMap() throws Exception {
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

    /** axis=y (the worldgen placement face — the only axis the tree feature plants) stays rotation-free. */
    @Test
    void axisYVariantCarriesNoRotationKeys() throws Exception {
        JsonObject tVariant = generatedJson("assets/gt6/blockstates/maple_log.json")
                .getAsJsonObject("variants").getAsJsonObject("axis=y");
        assertFalse(tVariant.has("x"), "axis=y must not carry an x rotation");
        assertFalse(tVariant.has("y"), "axis=y must not carry a y rotation");
    }
}
