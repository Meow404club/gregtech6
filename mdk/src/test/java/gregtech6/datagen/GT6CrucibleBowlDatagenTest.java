/**
 * Offline pin for task crucible-bowl-model — the small-crucible open-top BOWL.
 * Four faces pinned (the card acceptance):
 * <ol>
 * <li>the bowl shell geometry = the upstream six-pass setBlockBounds2 verbatim
 *     (MultiTileEntitySmeltery.java:596-606): four 2px walls full height + the 2px floor,
 *     with the getTexture2 null-gate face sets (:610-619 — no two shell faces coplanar);</li>
 * <li>the content box height formula verbatim (:603 {@code 0.125F + h/292.571428F}, the
 *     bucket L carrying the census floor h = L*255/8; the top-face-only gate :616);</li>
 * <li>the 9 LIQUID_LEVEL census — nine DISTINCT models per row, the former
 *     empty+one-flat-cube collapse retired;</li>
 * <li>the item models re-parent to the bowl shell, and the contentFace colour seam
 *     dispatches solid = the bodyTexture blockSolid face + mRGBaSolid tint / molten = the
 *     smeltery_content face + mRGBaLiquid (the 2026-09-30 colour ruling).</li>
 * </ol>
 * Reads the committed generated tree on the classpath (the
 * {@link GT6ClayCrucibleDatagenTest} form — no datagen run).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6CrucibleBowlDatagenTest extends GTOfflineTestBase {

    /** The upstream h/292.571428 constant verbatim (MultiTileEntitySmeltery.java:603). */
    private static final double HEIGHT_DIVISOR = 292.571428;

    /** The Steel molten colour — the MT.java:1713 setRGBaLiquid(255, 20, 10, 255) tail. Shared with the solid-face matrix test (task crucible-solid-face-matrix). */
    static final int STEEL_LIQUID_TINT = 0xFFFF140A;

    /** The Ceramic solid tint (the GT6MoldTintDatagenTest literal, mRGBaSolid 220/130/70). Shared with the solid-face matrix test (task crucible-solid-face-matrix). */
    static final int CERAMIC_SOLID_TINT = 0xFFDC8246;

    @BeforeAll
    static void bootMaterials() {
        // the seam dispatch dereferences MT statics (the GT6MoldTintDatagenTest shape)
        gregapi.data.MT.init();
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6CrucibleBowlDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The element with the exact from/to bounds, or null. */
    private static JsonObject elementAt(JsonObject aModel, float[] aFrom, float[] aTo) {
        for (JsonElement tElement : aModel.getAsJsonArray("elements")) {
            JsonObject tCandidate = tElement.getAsJsonObject();
            if (boundsMatch(tCandidate.getAsJsonArray("from"), aFrom) && boundsMatch(tCandidate.getAsJsonArray("to"), aTo)) {
                return tCandidate;
            }
        }
        return null;
    }

    private static boolean boundsMatch(JsonArray aBounds, float[] aExpected) {
        if (aBounds.size() != aExpected.length) return false;
        for (int i = 0; i < aExpected.length; i++) {
            if (Math.abs(aBounds.get(i).getAsFloat() - aExpected[i]) > 0.001F) return false;
        }
        return true;
    }

    private static void assertFaces(JsonObject aElement, String aLabel, String... aExpectedFaces) {
        JsonObject tFaces = aElement.getAsJsonObject("faces");
        assertEquals(aExpectedFaces.length, tFaces.size(), aLabel + ": the face set (the :610-619 null-gate)");
        for (String tFace : aExpectedFaces) {
            assertTrue(tFaces.has(tFace), aLabel + ": the " + tFace + " face exists");
            assertEquals("#all", tFaces.getAsJsonObject(tFace).get("texture").getAsString(),
                    aLabel + ": the " + tFace + " face rides the body texture");
        }
    }

    /** The bowl shell = the upstream six passes: four 2px walls + the 2px floor, the null-gate face sets. */
    @Test
    void bowlShellIsTheUpstreamSixPassBoxSet() throws Exception {
        assertTrue(GT6Crucibles.ROWS.size() >= 4, "the family walk broke — never pass vacuously");
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + "_empty.json");
            assertEquals(5, tModel.getAsJsonArray("elements").size(), tRow.path() + ": four walls + the floor");
            // the four walls (passes 0-3, :598-601): 2px thick, full height, the interior face + the rim
            JsonObject tWest = elementAt(tModel, new float[] {0, 0, 0}, new float[] {2, 16, 16});
            assertNotNull(tWest, tRow.path() + ": the west wall (pass 0)");
            assertFaces(tWest, tRow.path() + " west", "west", "east", "up");
            assertEquals("west", tWest.getAsJsonObject("faces").getAsJsonObject("west").get("cullface").getAsString(),
                    tRow.path() + ": the west exterior culls against the neighbour");
            JsonObject tEast = elementAt(tModel, new float[] {14, 0, 0}, new float[] {16, 16, 16});
            assertNotNull(tEast, tRow.path() + ": the east wall (pass 2)");
            assertFaces(tEast, tRow.path() + " east", "west", "east", "up");
            JsonObject tNorth = elementAt(tModel, new float[] {0, 0, 0}, new float[] {16, 16, 2});
            assertNotNull(tNorth, tRow.path() + ": the north wall (pass 1)");
            assertFaces(tNorth, tRow.path() + " north", "north", "south", "up");
            JsonObject tSouth = elementAt(tModel, new float[] {0, 0, 14}, new float[] {16, 16, 16});
            assertNotNull(tSouth, tRow.path() + ": the south wall (pass 3)");
            assertFaces(tSouth, tRow.path() + " south", "north", "south", "up");
            // the floor (pass 4, :602): the 2px plate, up + down only
            JsonObject tFloor = elementAt(tModel, new float[] {0, 0, 0}, new float[] {16, 2, 16});
            assertNotNull(tFloor, tRow.path() + ": the floor plate (pass 4)");
            assertFaces(tFloor, tRow.path() + " floor", "up", "down");
        }
    }

    /** The content box: the h/292.571428 formula verbatim at the bucket floor, top face only. */
    @Test
    void contentBoxHeightRidesTheUpstreamFormula() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            for (int tLevel = 1; tLevel <= 8; tLevel++) {
                JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + "_filled_" + tLevel + ".json");
                assertEquals("gt6:block/" + tRow.path() + "_empty", tModel.get("parent").getAsString(),
                        tRow.path() + " level " + tLevel + ": the shell lives in the parent, the child appends the box");
                assertEquals(1, tModel.getAsJsonArray("elements").size(),
                        tRow.path() + " level " + tLevel + ": exactly the one content element");
                JsonObject tBox = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
                // the :603 verbatim — 0.125 block units = 2px, the bucket floor h = L*255/8
                float tExpectedTop = 2.0F + (tLevel * 255.0F / 8.0F) / 292.571428F * 16.0F;
                assertEquals(2.0F, tBox.getAsJsonArray("from").get(1).getAsFloat(),
                        tRow.path() + " level " + tLevel + ": the box sits on the floor plate");
                assertEquals(tExpectedTop, tBox.getAsJsonArray("to").get(1).getAsFloat(), 0.001F,
                        tRow.path() + " level " + tLevel + ": the h/292.571428 height verbatim");
                assertTrue(tExpectedTop < 16.0F, tRow.path() + " level " + tLevel + ": never a full cube");
                JsonObject tFaces = tBox.getAsJsonObject("faces");
                assertEquals(1, tFaces.size(), tRow.path() + " level " + tLevel + ": the :616 top-face-only gate");
                assertEquals("#content", tFaces.getAsJsonObject("up").get("texture").getAsString(),
                        tRow.path() + " level " + tLevel + ": the molten-indicator sprite");
                // task crucible-large-ber: the content seat is tintindex 1 — GT6MoldTintListener
                // answers the BE's synced displayed material through the ContentFace dispatch
                assertEquals(1, tFaces.getAsJsonObject("up").get("tintindex").getAsInt(),
                        tRow.path() + " level " + tLevel + ": the content seat is tintindex 1");
            }
        }
    }

    /** The 9 LIQUID_LEVEL variants map to nine DISTINCT models — the collapse retired. */
    @Test
    void nineLiquidLevelsAreNineDistinctModels() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            JsonObject tVariants = generatedJson("assets/gt6/blockstates/" + tRow.path() + ".json")
                    .getAsJsonObject("variants");
            assertEquals(9, tVariants.size(), tRow.path() + ": the 9-bucket census");
            Set<String> tRefs = new HashSet<>();
            for (int tLevel = 0; tLevel <= 8; tLevel++) {
                JsonObject tVariant = tVariants.getAsJsonObject("gt_liquid_level=" + tLevel);
                assertNotNull(tVariant, tRow.path() + ": the level " + tLevel + " variant");
                String tRef = tVariant.get("model").getAsString();
                tRefs.add(tRef);
                if (tLevel == 0) {
                    assertEquals("gt6:block/" + tRow.path() + "_empty", tRef, tRow.path() + ": level 0 is the empty bowl");
                } else {
                    assertEquals("gt6:block/" + tRow.path() + "_filled_" + tLevel, tRef,
                            tRow.path() + ": level " + tLevel + " carries its own model");
                }
            }
            assertEquals(9, tRefs.size(), tRow.path() + ": nine DISTINCT models, no visual collapse");
        }
    }

    /** The item model rides the bowl shell (the held/rendered form matches the placed bowl). */
    @Test
    void itemModelsParentTheBowlShell() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            JsonObject tItemModel = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("gt6:block/" + tRow.path() + "_empty", tItemModel.get("parent").getAsString(),
                    tRow.path() + ": the item parents the bowl shell, not a flat cube");
        }
    }

    /** The colour-ruling seam: solid = the blockSolid face + mRGBaSolid, molten = the smeltery_content face + mRGBaLiquid. */
    @Test
    void contentFaceDispatchMatchesTheColourRuling() {
        // SOLID arm — the exact faces the bowl shell renders
        GT6CrucibleDatagen.ContentFace tStone = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Stone, false);
        assertEquals("minecraft:block/smooth_stone", tStone.texture(), "stone rides the vanilla finished texture");
        assertEquals(-1, tStone.tintARGB(), "the finished texture takes no tint");
        GT6CrucibleDatagen.ContentFace tCeramic = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Ceramic, false);
        assertEquals("gt6:block/materialicons/rough/block_solid", tCeramic.texture(), "ceramic rides the rough borrow");
        assertEquals(CERAMIC_SOLID_TINT, tCeramic.tintARGB(), "the solid tint is the mRGBaSolid pack");
        // MOLTEN arm — the molten face + the liquid colour (the Steel setRGBaLiquid tail verbatim)
        GT6CrucibleDatagen.ContentFace tSteelMolten = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Steel, true);
        assertEquals("gt6:block/smeltery_content", tSteelMolten.texture(), "the molten face is the smeltery_content sprite");
        assertEquals(STEEL_LIQUID_TINT, tSteelMolten.tintARGB(), "the molten tint is the mRGBaLiquid pack");
        GT6CrucibleDatagen.ContentFace tBronzeMolten = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Bronze, true);
        assertEquals("gt6:block/smeltery_content", tBronzeMolten.texture(), "the molten face is material-independent");
        assertFalse(tSteelMolten.tintARGB() == tBronzeMolten.tintARGB(), "the molten tint is per-material");
    }
}
