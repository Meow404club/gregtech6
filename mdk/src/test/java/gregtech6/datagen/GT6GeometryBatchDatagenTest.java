/*
 * Offline pinned tests for task r11-geometry-batch: the three render families leave
 * the full-cube placeholders for the upstream shapes, asserted against the committed
 * generated tree (the {@link GT6StorageTexDatagenTest} shape). Upstream semantics
 * pinned:
 * <ul>
 * <li>the bookshelf frame (MultiTileEntityBookShelf.setBlockBounds2 :294-299): six
 *     plank boxes, front AND back open, the ten plank leaves over the vanilla plank
 *     tiles (the PLANK_ICONS = IconContainerCopied(Blocks.planks) reference semantic,
 *     GT_API_Proxy_Client.java:188);</li>
 * <li>the bottlecrate frame (MultiTileEntityBottleCrate.setBlockBounds2 :160-168):
 *     nine plank boxes, half height (walls y 0..8), the 3x3 divider grid, front/back
 *     rails y 5..7 — outline AND collision ride the same 8px envelope (task
 *     shelf-crate-2px-realign, the three-way user ruling; the upstream 6px/10px
 *     split :212-214 stays the deliberate deviation);</li>
 * <li>the hopper funnel (MultiTileEntityHopper.setBlockBounds2 :263-277): rim y
 *     10..16 + middle 4..10 with its top face hidden, the per-direction spout models
 *     (down 0..4 / north 0..4 z / up SPOULESS — the :268-274 no-UP-arm quirk), the
 *     mouth face skipped, the 0.01 overlay twins over the FACES_TBS art;</li>
 * <li>the bottle-display BER seams (the slot grid px mapping, the upstream
 *     :170-196 world-fixed 3x3);</li>
 * <li>the card's item 5 — the kitchen texture CONTENT census: the bathing-pot/
 *     mixing-bowl/juicer art stays the borrowed upstream colored tiles (the sha
 *     ledger row per file) and stays NON-FLAT (>= 8 distinct colors — the plank
 *     seams, barrel hoops and stone bands live in the art; the flat-plate report
 *     pins against a placeholder regression).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.phys.AABB;

class GT6GeometryBatchDatagenTest {

    @BeforeAll
    static void bootOffline() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
    }

    private static InputStream resource(String aPath) {
        return GT6GeometryBatchDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The element footprint list as px strings (from/to pairs, read order). */
    private static List<String> boxes(String aModel) throws IOException {
        return json("assets/gt6/models/block/" + aModel + ".json").getAsJsonArray("elements").asList().stream()
                .map(GT6GeometryBatchDatagenTest::boxLabel).toList();
    }

    private static String boxLabel(JsonElement aElement) {
        JsonObject tElement = aElement.getAsJsonObject();
        return tElement.getAsJsonArray("from") + " -> " + tElement.getAsJsonArray("to");
    }

    // ------------------------------------------------------------------
    // the bookshelf frame (upstream :294-299)
    // ------------------------------------------------------------------

    @Test
    public void bookshelfFrameIsTheOpenSixBoxShelf() throws IOException {
        assertEquals(List.of(
                "[0,0,0] -> [16,1,16]",     // the bottom slab (:294)
                "[0,15,0] -> [16,16,16]",   // the top slab (:295)
                "[0,1,0] -> [1,15,16]",     // the west wall (:296)
                "[15,1,0] -> [16,15,16]",   // the east wall (:297)
                "[1,1,7] -> [15,15,9]",     // the centre spine (:298)
                "[1,7,1] -> [15,9,15]"),    // the middle shelf (:299)
                boxes("gt6_bookshelf_frame"), "the bookshelf frame = the six upstream boxes");
        // the front AND back stay open: no element spans the full z range at both z=0..16
        // while covering the mid band (the niche faces are the point)
    }

    @Test
    public void bookshelfLeavesBindTheTenVanillaPlankTiles() throws IOException {
        assertEquals(10, gregtech6.registry.GT6StaticStorages.PLANKS.size(), "the plank ladder stays 10");
        for (var tPlank : gregtech6.registry.GT6StaticStorages.PLANKS) {
            JsonObject tLeaf = json("assets/gt6/models/block/gt6_bookshelf_" + tPlank.slug() + ".json");
            assertEquals("gt6:block/gt6_bookshelf_frame", tLeaf.get("parent").getAsString(),
                    tPlank.slug() + ": the frame parent");
            assertEquals("minecraft:block/" + tPlank.slug() + "_planks",
                    tLeaf.getAsJsonObject("textures").get("plank").getAsString(),
                    tPlank.slug() + ": the vanilla plank reference (zero borrow)");
            assertFalse(tLeaf.has("elements"), tPlank.slug() + ": the elements ride the parent");
        }
    }

    // ------------------------------------------------------------------
    // the bottlecrate frame (upstream :160-168) + the shape split (:212-214)
    // ------------------------------------------------------------------

    @Test
    public void bottlecrateFrameIsTheHalfHeightNineBoxCrate() throws IOException {
        List<String> tBoxes = boxes("gt6_bottlecrate_frame");
        assertEquals(List.of(
                "[1,0,1] -> [15,1,15]",     // the bottom board (:160)
                "[1,1,5] -> [15,5,6]",      // the divider rail z 5..6 (:161)
                "[1,1,10] -> [15,5,11]",    // the divider rail z 10..11 (:162)
                "[5,1,1] -> [6,4,15]",      // the divider rail x 5..6 (:163)
                "[10,1,1] -> [11,4,15]",    // the divider rail x 10..11 (:164)
                "[0,0,0] -> [1,8,16]",      // the west wall (the :165 intent)
                "[15,0,0] -> [16,8,16]",    // the east wall (:166)
                "[1,5,0] -> [15,7,1]",      // the front rail (:167)
                "[1,5,15] -> [15,7,16]"),   // the back rail (:168)
                tBoxes, "the crate frame = the nine upstream boxes");
        for (String tBox : tBoxes) {
            assertFalse(tBox.contains("16,16]"), "half height: no element reaches y=16 — " + tBox);
        }
    }

    @Test
    public void crateOutlineAndCollisionRideTheModelEnvelope() {
        // task shelf-crate-2px-realign — the three-way user ruling: outline = collision =
        // the (0,0,0)-(16,8,16) model body; the upstream 6px selection (:213-214,
        // PX_P[6]) / 10px collision (:212, PX_N[6]) split is the deliberate deviation
        AABB tSelection = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.CRATE_SELECTION_SHAPE.bounds();
        AABB tCollision = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.CRATE_COLLISION_SHAPE.bounds();
        assertEquals(8.0 / 16.0, tSelection.maxY, 1e-9, "the crate outline tops at the 8px walls");
        assertEquals(8.0 / 16.0, tCollision.maxY, 1e-9, "the crate collision tops at the 8px walls");
        assertEquals(tSelection.minX, tCollision.minX, 1e-9, "collision west = outline west");
        assertEquals(tSelection.maxX, tCollision.maxX, 1e-9, "collision east = outline east");
        assertEquals(tSelection.minZ, tCollision.minZ, 1e-9, "collision north = outline north");
        assertEquals(tSelection.maxZ, tCollision.maxZ, 1e-9, "collision south = outline south");
    }

    @Test
    public void bookshelfOutlineIsTheFullCubeEnvelope() {
        // task shelf-crate-2px-realign — the three-way user ruling: the shelf outline is
        // the full cube (the model envelope; the named SHELF_SHAPE — a Block instance
        // cannot be constructed offline, the frozen block registry); the upstream
        // 2px-inset slab (:349-350) outlined a hole 2px inside the visible mass and is
        // the deliberate deviation; the collision keeps the untouched vanilla cube,
        // numerically the same box
        AABB tOutline = gregtech6.registry.GT6StaticStorages.GT6StorageBlock.SHELF_SHAPE.bounds();
        AABB tVanillaCube = net.minecraft.world.phys.shapes.Shapes.block().bounds();
        assertEquals(0.0, tOutline.minX, 1e-9, "the outline hugs the west wall");
        assertEquals(1.0, tOutline.maxX, 1e-9, "the outline hugs the east wall");
        assertEquals(0.0, tOutline.minY, 1e-9, "the outline hugs the bottom");
        assertEquals(1.0, tOutline.maxY, 1e-9, "the outline hugs the top");
        assertEquals(0.0, tOutline.minZ, 1e-9, "the outline hugs the north wall");
        assertEquals(1.0, tOutline.maxZ, 1e-9, "the outline hugs the south wall");
        assertEquals(tVanillaCube.minX, tOutline.minX, 1e-9, "collision (vanilla cube) = outline west");
        assertEquals(tVanillaCube.maxX, tOutline.maxX, 1e-9, "collision (vanilla cube) = outline east");
        assertEquals(tVanillaCube.maxY, tOutline.maxY, 1e-9, "collision (vanilla cube) = outline top");
    }

    @Test
    public void crateLeavesBindTheTenVanillaPlankTiles() throws IOException {
        for (var tPlank : gregtech6.registry.GT6StaticStorages.PLANKS) {
            JsonObject tLeaf = json("assets/gt6/models/block/gt6_bottlecrate_" + tPlank.slug() + ".json");
            assertEquals("gt6:block/gt6_bottlecrate_frame", tLeaf.get("parent").getAsString(),
                    tPlank.slug() + ": the frame parent");
            assertEquals("minecraft:block/" + tPlank.slug() + "_planks",
                    tLeaf.getAsJsonObject("textures").get("plank").getAsString(),
                    tPlank.slug() + ": the vanilla plank reference (zero borrow)");
        }
    }

    // ------------------------------------------------------------------
    // the hopper funnel (upstream :263-277)
    // ------------------------------------------------------------------

    @Test
    public void hopperFunnelModelsCarryTheThreeSpoutForms() throws IOException {
        String tNorthSpout = "[6,4,0] -> [10,8,4]";
        String tDownSpout = "[6,0,6] -> [10,4,10]";
        for (String tBand : List.of("hopper", "queuehopper")) {
            var tNorth = boxes("gt6_" + tBand);
            assertEquals(6, tNorth.size(), tBand + ": rim+overlay, middle+overlay, spout+overlay");
            assertEquals(tNorthSpout, tNorth.get(4), tBand + ": the north spout (pass 2 SIDE_Z_NEG :270)");
            var tDown = boxes("gt6_" + tBand + "_down");
            assertEquals(tDownSpout, tDown.get(4), tBand + ": the down spout (pass 2 SIDE_Y_NEG :269)");
            var tTop = boxes("gt6_" + tBand + "_top");
            assertEquals(4, tTop.size(), tBand + ": rim+middle twins only — NO spout (the :268-274 no-UP-arm)");
            for (String tBox : tTop) {
                assertFalse(tBox.equals(tNorthSpout) || tBox.equals(tDownSpout),
                        tBand + ": the up form stays spoutless — " + tBox);
            }
            // the geometry bands: the rim y 10..16, the middle 4..10
            assertTrue(tNorth.get(0).startsWith("[0,10,0]"), tBand + ": the rim (:265)");
            assertTrue(tNorth.get(2).startsWith("[4,4,4]"), tBand + ": the middle (:266)");
        }
    }

    @Test
    public void hopperFacesRideTheTbsMappingAndSkipTheMouth() throws IOException {
        for (String tModel : List.of("gt6_hopper", "gt6_queuehopper")) {
            var tElements = json("assets/gt6/models/block/" + tModel + ".json").getAsJsonArray("elements");
            JsonObject tSpout = tElements.get(4).getAsJsonObject();
            var tFaces = tSpout.getAsJsonObject("faces");
            assertFalse(tFaces.has("north"), tModel + ": the spout MOUTH face is skipped (upstream aSide != mFacing)");
            assertEquals("#top", tFaces.get("up").getAsJsonObject().get("texture").getAsString(),
                    tModel + ": the spout top rides the TBS top art");
            assertEquals("#side", tFaces.get("south").getAsJsonObject().get("texture").getAsString(),
                    tModel + ": the spout sides ride the TBS side art");
            // the middle box hides its up face under the rim (upstream pass 1 skips SIDES_TOP :281)
            var tMiddle = tElements.get(2).getAsJsonObject().getAsJsonObject("faces");
            assertFalse(tMiddle.has("up"), tModel + ": the middle top face is hidden");
            // the overlay twins are the 0.01-inflated pairs (the boiler grammar)
            JsonObject tRimOverlay = tElements.get(1).getAsJsonObject();
            assertEquals(-0.01, tRimOverlay.getAsJsonArray("from").get(0).getAsDouble(), 1e-9,
                    tModel + ": the overlay twin inflates 0.01");
            assertEquals("#overlay_side", tRimOverlay.getAsJsonObject("faces").get("north")
                    .getAsJsonObject().get("texture").getAsString(), tModel + ": the overlay band");
            // the tint seat stays OFF (the unpaint deviation — no face carries a tintindex)
            for (var tElement : tElements) {
                for (var tFace : tElement.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    assertFalse(tFace.getValue().getAsJsonObject().has("tintindex"),
                            tModel + " face " + tFace.getKey() + " stays untinted");
                }
            }
        }
    }

    @Test
    public void hopperBlockstatesDropThePistonRotations() throws IOException {
        for (String tBand : List.of("hopper_lead", "queue_hopper_lead")) {
            var tVariants = json("assets/gt6/blockstates/" + tBand + ".json").getAsJsonObject("variants");
            assertFalse(tVariants.getAsJsonObject("facing=down").has("x"),
                    tBand + ": the down model is authored facing down — no x=90");
            assertFalse(tVariants.getAsJsonObject("facing=up").has("x"),
                    tBand + ": the up model is authored spoutless — no x=270");
        }
    }

    // ------------------------------------------------------------------
    // the retired placeholder art stays dead
    // ------------------------------------------------------------------

    @Test
    public void theFourWoodenPlaceholdersAreGoneFromDisk() throws IOException {
        Path tMdk = mdkRoot();
        for (String tDead : List.of("bookshelf_front.png", "bookshelf_side.png",
                "bottlecrate_front.png", "bottlecrate_side.png")) {
            assertFalse(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block").resolve(tDead)),
                    "the retired placeholder must stay deleted: " + tDead);
        }
        try (Stream<Path> tWalk = Files.walk(tMdk.resolve("src/generated/resources/assets/gt6"))) {
            for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String tContent = Files.readString(tFile, StandardCharsets.UTF_8);
                for (String tRef : List.of("block/bookshelf_front", "block/bookshelf_side",
                        "block/bottlecrate_front", "block/bottlecrate_side")) {
                    assertFalse(tContent.contains("\"gt6:" + tRef + "\""),
                            tFile.getFileName() + " still references the retired texture " + tRef);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // the bottle-display BER seams (the pure statics, the contentTopPx form)
    // ------------------------------------------------------------------

    @Test
    public void bottleSlotGridIsTheWorldFixedThreeByThree() {
        // upstream :170-196 verbatim — the bottles do NOT consume mFacing; the cells sit
        // at 1..5 / 6..10 / 11..15, centres 3/8/13 px
        assertEquals(3.0F, gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(0), 1e-6F);
        assertEquals(8.0F, gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(1), 1e-6F);
        assertEquals(13.0F, gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(2), 1e-6F);
        assertEquals(3.0F, gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(3), 1e-6F);
        assertEquals(13.0F, gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(8), 1e-6F);
        assertEquals(3.0F, gregtech6.client.render.GTBottleCrateRenderer.slotRowPx(0), 1e-6F);
        assertEquals(8.0F, gregtech6.client.render.GTBottleCrateRenderer.slotRowPx(4), 1e-6F);
        assertEquals(13.0F, gregtech6.client.render.GTBottleCrateRenderer.slotRowPx(8), 1e-6F);
        // the column half wraps every row (slot 3 = col 0, row 1 — the :105 click mapping)
        assertEquals(gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(0),
                gregtech6.client.render.GTBottleCrateRenderer.slotCenterPx(3), 1e-6F);
    }

    // ------------------------------------------------------------------
    // the kitchen texture CONTENT census (the card's item 5 — the mixbowl/bathing-pot
    // "flat plate" report): the port art must BE the upstream colored art, byte for
    // byte (the sha ledger), and must NOT be a flat plate (pixel variance) — the
    // bathing-pot hoops and the bowl bands live in the art itself
    // ------------------------------------------------------------------

    /** The borrowed kitchen art families (the block/tools bands over the upstream colored/ tiles). */
    private static final List<String> KITCHEN_FAMILIES = List.of("bathing_pot", "bathing_pot_wood", "mixing_bowl", "juicer");

    @Test
    public void kitchenArtIsTheUpstreamContentNotFlatPlates() throws Exception {
        Path tMdk = mdkRoot();
        int tChecked = 0;
        for (String tFamily : KITCHEN_FAMILIES) {
            Path tDir = tMdk.resolve("src/main/resources/assets/gt6/textures/block/tools").resolve(tFamily);
            assertTrue(Files.isDirectory(tDir), tFamily + ": the texture band exists");
            try (Stream<Path> tWalk = Files.walk(tDir)) {
                for (Path tPng : tWalk.filter(p -> p.toString().endsWith(".png")).toList()) {
                    byte[] tBytes = Files.readAllBytes(tPng);
                    // the sha ledger: every file's sha256 must have its README row
                    String tSha = sha256(tBytes);
                    String tRel = "block/tools/" + tFamily + "/" + tPng.getFileName();
                    assertTrue(Files.readString(tMdk.resolve("src/main/resources/assets/README.md"),
                                    StandardCharsets.UTF_8).contains("`" + tRel + "` — `" + tSha + "`"),
                            tRel + " must carry its sha256 ledger row (got " + tSha + ")");
                    // the content census: NOT a flat plate — the upstream art carries plank
                    // seams, barrel hoops and stone bands; the flat-plate placeholder is a
                    // single-color tile (the real set floors at 7 — the simple plank art)
                    int tColors = distinctColors(tBytes);
                    assertTrue(tColors >= 4, tRel + " is a flat plate (" + tColors + " distinct colors) — "
                            + "the upstream art content is missing");
                    tChecked++;
                }
            }
        }
        assertEquals(18, tChecked, "the kitchen art census stays 18 tiles");
    }

    /** sha256 hex of the bytes. */
    private static String sha256(byte[] aBytes) throws Exception {
        byte[] tDigest = java.security.MessageDigest.getInstance("SHA-256").digest(aBytes);
        StringBuilder r = new StringBuilder();
        for (byte tB : tDigest) r.append(String.format("%02x", tB));
        return r.toString();
    }

    /** Distinct RGBA colors of a PNG (ImageIO decode, the offline census face). */
    private static int distinctColors(byte[] aBytes) throws IOException {
        java.awt.image.BufferedImage tImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(aBytes));
        assertNotNull(tImage, "the PNG must decode");
        java.util.Set<Integer> tColors = new java.util.HashSet<>();
        for (int y = 0, h = tImage.getHeight(); y < h; y++) {
            for (int x = 0, w = tImage.getWidth(); x < w; x++) {
                tColors.add(tImage.getRGB(x, y));
            }
        }
        return tColors.size();
    }

    /** Location of the mdk project root (the census walk). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }
}
