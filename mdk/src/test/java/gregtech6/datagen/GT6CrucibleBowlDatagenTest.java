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

import net.minecraft.client.renderer.block.model.BlockModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.registry.GT6MaterialTestSupport;

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
        GT6MaterialTestSupport.materials(); // the hermetic bracket (task hermetic-pour-tests)
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

    /**
     * The content box: the h/292.571428 formula verbatim at the bucket floor, top face only —
     * and the r11a-crucible-filled-shell pin: the filled model carries the FULL element set
     * (the shell + the one content box) in its OWN elements array. Vanilla
     * {@code BlockModel.getElements} (1.20.1 BlockModel.java:104-105) never walks the parent
     * chain once the child carries elements — the former "candle idiom" parent + one-element
     * form made the game drop the bowl shell (the user report: the filled crucible rendered
     * as a floating content panel).
     */
    @Test
    void contentBoxHeightRidesTheUpstreamFormula() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            int tShellCount = generatedJson("assets/gt6/models/block/" + tRow.path() + "_empty.json")
                    .getAsJsonArray("elements").size();
            for (int tLevel = 1; tLevel <= 8; tLevel++) {
                JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + "_filled_" + tLevel + ".json");
                assertEquals("gt6:block/" + tRow.path() + "_empty", tModel.get("parent").getAsString(),
                        tRow.path() + " level " + tLevel + ": the parent stays (the texture map)");
                assertEquals(tShellCount + 1, tModel.getAsJsonArray("elements").size(),
                        tRow.path() + " level " + tLevel + ": the FULL shell + the one content element — "
                                + "vanilla getElements never merges the parent (BlockModel.java:104-105)");
                // the game's own accessor — the exact parent-fallback decision point — must see
                // the full set offline (ponytail: the stand-in for the quad bake, which needs
                // the atlas + ModelBakery; the layer-level view stays field_test)
                BlockModel tVanilla = BlockModel.fromString(tModel.toString());
                assertEquals(tShellCount + 1, tVanilla.getElements().size(),
                        tRow.path() + " level " + tLevel + ": vanilla BlockModel.getElements sees the shell+content set");
                // the :603 verbatim — 0.125 block units = 2px, the bucket floor h = L*255/8
                float tExpectedTop = 2.0F + (tLevel * 255.0F / 8.0F) / 292.571428F * 16.0F;
                JsonObject tBox = elementAt(tModel, new float[] {0, 2, 0}, new float[] {16, tExpectedTop, 16});
                assertNotNull(tBox, tRow.path() + " level " + tLevel + ": the content box element");
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

    /**
     * The 9 LIQUID_LEVEL buckets × the 2 MOLTEN phases map to distinct models (task
     * crucible-render-followup, the symptom-B nail): level 0 the empty bowl in BOTH
     * phases, level L molten=true the molten-art model, level L molten=false the solid-art
     * model — the cooled-charge face (the upstream pass-5 gate renders whenever the fill
     * census is non-zero, MultiTileEntitySmeltery.java:616, and the cooled texture choice
     * rides the mDisplayedFluid validity :587-591).
     */
    @Test
    void liquidLevelTimesMoltenAreTheVariantCensus() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            JsonObject tVariants = generatedJson("assets/gt6/blockstates/" + tRow.path() + ".json")
                    .getAsJsonObject("variants");
            assertEquals(18, tVariants.size(), tRow.path() + ": the 9 buckets x 2 phases census");
            Set<String> tRefs = new HashSet<>();
            for (int tLevel = 0; tLevel <= 8; tLevel++) {
                for (boolean tMolten : new boolean[] {false, true}) {
                    JsonObject tVariant = tVariants.getAsJsonObject("gt_liquid_level=" + tLevel + ",molten=" + tMolten);
                    assertNotNull(tVariant, tRow.path() + ": the level " + tLevel + " molten=" + tMolten + " variant");
                    String tRef = tVariant.get("model").getAsString();
                    tRefs.add(tRef);
                    if (tLevel == 0) {
                        assertEquals("gt6:block/" + tRow.path() + "_empty", tRef,
                                tRow.path() + ": level 0 is the empty bowl in both phases");
                    } else if (tMolten) {
                        assertEquals("gt6:block/" + tRow.path() + "_filled_" + tLevel, tRef,
                                tRow.path() + ": level " + tLevel + " molten rides the molten-art model");
                    } else {
                        assertEquals("gt6:block/" + tRow.path() + "_filled_" + tLevel + "_solid", tRef,
                                tRow.path() + ": level " + tLevel + " cooled rides the solid-art model");
                    }
                }
            }
            assertEquals(17, tRefs.size(), tRow.path() + ": 8 molten + 8 solid + the shared empty = 17 DISTINCT models");
        }
    }

    /**
     * The solid-phase filled model (task crucible-render-followup — the symptom-B face):
     * the FULL shell+content element set (the r11a rule — vanilla getElements never merges
     * the parent), the content seat riding the BODY art (#all) at tintindex 1 — the cooled
     * charge renders the blockSolid grain recoloured by the lightest content's mRGBaSolid,
     * the upstream cooled semantics minus the gray-NULL placeholder (the declared port
     * face, the user ruling "solid = the material's own colour").
     */
    @Test
    void solidFilledModelsCarryTheBodyArtContentSeat() throws Exception {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            int tShellCount = generatedJson("assets/gt6/models/block/" + tRow.path() + "_empty.json")
                    .getAsJsonArray("elements").size();
            for (int tLevel = 1; tLevel <= 8; tLevel++) {
                JsonObject tModel = generatedJson("assets/gt6/models/block/" + tRow.path() + "_filled_" + tLevel + "_solid.json");
                assertEquals(tShellCount + 1, tModel.getAsJsonArray("elements").size(),
                        tRow.path() + " solid level " + tLevel + ": the FULL shell + the one content element");
                BlockModel tVanilla = BlockModel.fromString(tModel.toString());
                assertEquals(tShellCount + 1, tVanilla.getElements().size(),
                        tRow.path() + " solid level " + tLevel + ": vanilla BlockModel.getElements sees the full set");
                float tExpectedTop = 2.0F + (tLevel * 255.0F / 8.0F) / 292.571428F * 16.0F;
                JsonObject tBox = elementAt(tModel, new float[] {0, 2, 0}, new float[] {16, tExpectedTop, 16});
                assertNotNull(tBox, tRow.path() + " solid level " + tLevel + ": the content box element");
                JsonObject tUp = tBox.getAsJsonObject("faces").getAsJsonObject("up");
                assertEquals("#all", tUp.get("texture").getAsString(),
                        tRow.path() + " solid level " + tLevel + ": the content seat rides the BODY art");
                assertEquals(1, tUp.get("tintindex").getAsInt(),
                        tRow.path() + " solid level " + tLevel + ": the content seat stays tintindex 1");
            }
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

    /**
     * The colour-ruling seam: solid = the blockSolid face + mRGBaSolid, molten = the
     * borrowed per-set molten art + mRGBaLiquid (task r11b-crucible-molten-art — the
     * upstream getTextureMolten shape, OreDictMaterial.java:990-999, the flat
     * smeltery_content placeholder retired).
     */
    @Test
    void contentFaceDispatchMatchesTheColourRuling() {
        // SOLID arm — the exact faces the bowl shell renderers draw (unchanged by r11b)
        GT6CrucibleDatagen.ContentFace tStone = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Stone, false);
        assertEquals("minecraft:block/smooth_stone", tStone.texture(), "stone rides the vanilla finished texture");
        assertEquals(-1, tStone.tintARGB(), "the finished texture takes no tint");
        GT6CrucibleDatagen.ContentFace tCeramic = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Ceramic, false);
        assertEquals("gt6:block/materialicons/rough/block_solid", tCeramic.texture(), "ceramic rides the rough borrow");
        assertEquals(CERAMIC_SOLID_TINT, tCeramic.tintARGB(), "the solid tint is the mRGBaSolid pack");
        // MOLTEN arm — the per-set molten grayscale + the liquid colour (the Steel setRGBaLiquid tail verbatim)
        GT6CrucibleDatagen.ContentFace tSteelMolten = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Steel, true);
        assertTrue(tSteelMolten.texture().startsWith("gt6:block/materialicons/") && tSteelMolten.texture().endsWith("/molten"),
                "the molten face rides the borrowed per-set molten art, got " + tSteelMolten.texture());
        assertEquals(STEEL_LIQUID_TINT, tSteelMolten.tintARGB(), "the molten tint is the mRGBaLiquid pack");
        GT6CrucibleDatagen.ContentFace tBronzeMolten = GT6CrucibleDatagen.contentFace(gregapi.data.MT.Bronze, true);
        assertTrue(tBronzeMolten.texture().startsWith("gt6:block/materialicons/") && tBronzeMolten.texture().endsWith("/molten"),
                "the molten face rides the borrowed per-set molten art, got " + tBronzeMolten.texture());
        assertFalse(tSteelMolten.tintARGB() == tBronzeMolten.tintARGB(), "the molten tint is per-material");
    }
}
