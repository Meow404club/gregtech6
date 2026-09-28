/*
 * Offline pinned-census tests for task r8-tex-tank-family: the barrel family
 * (wood/plastic/metal + the 12 high-tier drums) joins the two-layer per-face grammar —
 * the tintindex-0 grayscale colored body is the mRGBa/paint seat (upstream
 * MultiTileEntityBarrelWood.java:44-55 BlockTextureDefault(colored, mRGBa)), the six
 * 0.01-plate overlay decals stay untinted — and the 25 tank valve controllers
 * (Loader :1195-1222) join the Base10 front-layer-pair form
 * (TileEntityBase10MultiBlockBase.java:192-194: front face = colored_front +
 * overlay_front, the rest = colored + overlay).
 *
 * <p>Coverage (asserted against the committed generated tree, the
 * {@link GT6ConverterPaintRenderDatagenTest} shape):
 * <ul>
 * <li>the three barrel families' models carry the per-face texture spread —
 *     down/up/side reference three DISTINCT colored borrows and three DISTINCT overlay
 *     borrows (the flat single-PNG regression killer), body tintindex 0, decals
 *     untinted, cutout declared;</li>
 * <li>the 12 high-tier drums share the drum family's barrel_parts set (upstream
 *     registers the ladder over the same icon set) — 16 barrel models total, the three
 *     retired {@code barrel_<material>.png} single textures gone off the classpath
 *     while the p12 {@code barrel_logistics.png} row stays;</li>
 * <li>the two valve models (tank_wood/tank_metal) keep the front layer pair on north —
 *     colored_front_side body + overlay_front_side decal — with the colored/overlay set
 *     on the remaining faces, and the 12 front-pair PNGs exist on disk;</li>
 * <li>the 25 valve blockstates carry the four-way FACING y band over both FORMED
 *     states, the item models parent the family models, and the row material columns
 *     tint pairwise distinct (the wood row = WoodTreated, the six plain steel rungs
 *     distinct);</li>
 * <li>the retired deviation claims are dead in the datagen source (the "no overlay
 *     decal pool" barrel note and the "no multiblockmains tankwood/tankmetal group"
 *     valve note — both PROVEN FALSE by the r8-tex-r1 probes).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Tanks;

class GT6TankFamilyPaintRenderDatagenTest {

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** facing → rotationY (the front-bearing band, the addLargeBoiler form; FRONT on north in the model). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    /** The three borrowed barrel families (the upstream machines/tanks iconset names). */
    private static final List<String> BARREL_FAMILIES = List.of("barrel", "plasticcan", "drum");

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the controller census shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        gregtech6.registry.GTMaterialItems.initMaterials();
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6TankFamilyPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** A borrowed PNG must exist on the main-resource classpath and be non-empty. */
    private static void assertPng(String aPath) throws IOException {
        try (InputStream tStream = GT6TankFamilyPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the borrowed PNG must be on the classpath: " + aPath);
            assertTrue(tStream.readAllBytes().length > 0, "the borrowed PNG must be non-empty: " + aPath);
        }
    }

    /** The mdk root walk (the census-test shape — tools/gen_textures.py marker). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    // ------------------------------------------------------------------
    // the barrel families: the per-face two-layer grammar
    // ------------------------------------------------------------------

    private void assertBarrelFamilyModel(String aModelName, String aFamily) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aModelName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aModelName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        String tBand = "gt6:block/barrel_parts/" + aFamily + "/";
        // the per-face colored spread: the three face arts are three DISTINCT references
        assertEquals(tBand + "colored_bottom", tTextures.get("down").getAsString(), aModelName + ": bottom = colored_bottom");
        assertEquals(tBand + "colored_top", tTextures.get("up").getAsString(), aModelName + ": top = colored_top");
        assertEquals(tBand + "colored_side", tTextures.get("north").getAsString(), aModelName + ": side = colored_side");
        assertEquals(3, Set.of(tTextures.get("down").getAsString(), tTextures.get("up").getAsString(),
                tTextures.get("north").getAsString()).size(), aModelName + ": the top/bottom/side refs stay distinct");
        // the overlay borrows ride their own keys
        assertEquals(tBand + "overlay_bottom", tTextures.get("overlay_bottom").getAsString(), aModelName + ": overlay_bottom");
        assertEquals(tBand + "overlay_top", tTextures.get("overlay_top").getAsString(), aModelName + ": overlay_top");
        assertEquals(tBand + "overlay_side", tTextures.get("overlay_side").getAsString(), aModelName + ": overlay_side");
        // body + six decals; body tinted, decals not
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aModelName + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aModelName + " body face " + tFace + " carries tintindex 0");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertTrue(!tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        aModelName + " decal " + i + " face " + tFace + " stays untinted");
            }
        }
    }

    @Test
    public void barrelFamilyModelsKeepThePerFaceTwoLayerGrammar() throws Exception {
        assertBarrelFamilyModel("barrel_wood", "barrel");
        assertBarrelFamilyModel("barrel_plastic", "plasticcan");
        assertBarrelFamilyModel("barrel_metal", "drum");
    }

    /**
     * The 16-model census: the 12 high-tier drums share the drum family's barrel_parts
     * set (upstream registers the :2159-2170 ladder over the same icon set), the item
     * models parent the block models, and the retired single-PNG borrows are gone off
     * the classpath while the p12 logistics row stays.
     */
    @Test
    public void highTierDrumsShareTheDrumFamilyAndTheOldPngsAreRetired() throws Exception {
        Set<String> tTextureSets = new HashSet<>();
        List<String> tDrums = gregtech6.registry.GTBarrels.HIGH_TIER_METAL_DRUMS.stream().map(r -> r.path()).toList();
        assertEquals(12, tDrums.size(), "the high-tier drum census stays 12");
        for (String tDrum : tDrums) {
            JsonObject tModel = json("assets/gt6/models/block/" + tDrum + ".json");
            JsonObject tTextures = tModel.getAsJsonObject("textures");
            tTextureSets.add(tTextures.get("down").getAsString() + "|" + tTextures.get("up").getAsString()
                    + "|" + tTextures.get("north").getAsString() + "|" + tTextures.get("overlay_side").getAsString());
            JsonObject tItem = json("assets/gt6/models/item/" + tDrum + ".json");
            assertEquals("gt6:block/" + tDrum, tItem.get("parent").getAsString(), tDrum + ": the item parents the block model");
        }
        assertEquals(1, tTextureSets.size(), "all 12 drums share the ONE drum barrel_parts set (upstream so)");
        for (String tRetired : List.of("barrel_wood", "barrel_plastic", "barrel_metal")) {
            assertNull(GT6TankFamilyPaintRenderDatagenTest.class.getClassLoader()
                    .getResourceAsStream("assets/gt6/textures/block/" + tRetired + ".png"),
                    tRetired + ".png must be retired (the two-layer borrow replaced the single texture)");
        }
        assertPng("assets/gt6/textures/block/barrel_logistics.png");
    }

    // ------------------------------------------------------------------
    // the valve front layer pair
    // ------------------------------------------------------------------

    private void assertValveModel(String aModelName, String aFamily) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aModelName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aModelName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        String tBand = "gt6:block/tank_valves/" + aFamily + "_";
        // the FRONT pair on north (the Base10 getTexture2 front semantics)
        assertEquals(tBand + "colored_front_side", tTextures.get("north").getAsString(),
                aModelName + ": north = the colored_front front art");
        assertEquals(tBand + "overlay_front_side", tTextures.get("overlay_front_side").getAsString(),
                aModelName + ": the overlay_front front decal");
        // the remaining faces ride the plain colored/overlay set
        assertEquals(tBand + "colored_bottom", tTextures.get("down").getAsString(), aModelName + ": bottom");
        assertEquals(tBand + "colored_top", tTextures.get("up").getAsString(), aModelName + ": top");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals(tBand + "colored_side", tTextures.get(tSide).getAsString(), aModelName + ": " + tSide);
        }
        // body tinted, the front decal and its siblings not
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aModelName + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aModelName + " body face " + tFace + " carries tintindex 0");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertTrue(!tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        aModelName + " decal " + i + " face " + tFace + " stays untinted");
            }
        }
    }

    @Test
    public void valveModelsKeepTheFrontLayerPair() throws Exception {
        assertValveModel("tank_wood", "tankwood");
        assertValveModel("tank_metal", "tankmetal");
    }

    /** The 12 front-pair PNGs (2 families x colored_front/overlay_front x bottom/top/side) exist on disk. */
    @Test
    public void twelveFrontPairPngsExist() throws IOException {
        for (String tFamily : List.of("tankwood", "tankmetal")) {
            for (String tLayer : List.of("colored_front", "overlay_front")) {
                for (String tFace : List.of("bottom", "top", "side")) {
                    assertPng("assets/gt6/textures/block/tank_valves/" + tFamily + "_" + tLayer + "_" + tFace + ".png");
                }
            }
        }
    }

    /** The 18 barrel_parts PNGs exist on disk (3 families x colored/overlay x bottom/top/side). */
    @Test
    public void eighteenBarrelPartsPngsExist() throws IOException {
        for (String tFamily : BARREL_FAMILIES) {
            for (String tLayer : List.of("colored", "overlay")) {
                for (String tFace : List.of("bottom", "top", "side")) {
                    assertPng("assets/gt6/textures/block/barrel_parts/" + tFamily + "/" + tLayer + "_" + tFace + ".png");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // the valve blockstates: the four-way facing band over FORMED
    // ------------------------------------------------------------------

    @Test
    public void valveBlockstatesCarryTheFacingBand() throws Exception {
        assertEquals(25, GT6Tanks.ROWS.size(), "the valve census stays 25");
        for (GT6Tanks.TankValveRow tRow : GT6Tanks.ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tRow.path() + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tFormed : new boolean[] {false, true}) {
                    String tKey = "facing=" + tFacing + ",formed=" + tFormed;
                    assertTrue(tVariants.has(tKey), tRow.path() + ": the variant key " + tKey);
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariants.getAsJsonObject(tKey).has("y")
                                    ? tVariants.getAsJsonObject(tKey).get("y").getAsInt() : 0,
                            tRow.path() + " " + tKey + ": the rotationY band");
                }
            }
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("gt6:block/" + (tRow.flammable() ? "tank_wood" : "tank_metal"),
                    tItem.get("parent").getAsString(), tRow.path() + ": the item parents the family model");
        }
    }

    // ------------------------------------------------------------------
    // the tint dispatch (the row material columns)
    // ------------------------------------------------------------------

    /**
     * The valve rows carry the NBT_MATERIAL column the tint resolves through the
     * controller gate: the wood row = WoodTreated (the :1195 row), the six plain steel
     * rungs resolve pairwise distinct colours (the all-gray regression killer, the
     * converter-ladder shape).
     */
    @Test
    public void valveRowMaterialsTintPairwiseDistinct() {
        assertSame(gregapi.data.MT.WoodTreated, GT6Tanks.ROWS.get(0).material().get(), "the wood row is the :1195 WoodTreated row");
        Set<Integer> tSeen = new HashSet<>();
        for (int i = 1; i <= 6; i++) { // the plain small 3x3x3 six — :1196-1201
            gregapi.oredict.OreDictMaterial tMat = GT6Tanks.ROWS.get(i).material().get();
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, "rung " + i + " binds full alpha");
            assertEquals(tMat.fRGBaSolid[0], (tTint >> 16) & 0xFF, "rung " + i + " R = fRGBaSolid[0]");
            assertEquals(tMat.fRGBaSolid[1], (tTint >> 8) & 0xFF, "rung " + i + " G = fRGBaSolid[1]");
            assertEquals(tMat.fRGBaSolid[2], tTint & 0xFF, "rung " + i + " B = fRGBaSolid[2]");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    "rung " + i + " colour " + Integer.toHexString(tTint) + " is distinct — the gray-white valve regression stays dead");
        }
    }

    // ------------------------------------------------------------------
    // the retired declarations stay dead
    // ------------------------------------------------------------------

    /**
     * The two proven-false claims the r8-tex-r1 probes killed must not return to the
     * datagen source: the p23 "no overlay decal pool" barrel deviation and the p29
     * "no multiblockmains tankwood/tankmetal group" valve note.
     */
    @Test
    public void retiredDeviationClaimsAreDead() throws IOException {
        String tSource = Files.readString(mdkRoot()
                .resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"), StandardCharsets.UTF_8);
        assertFalse(tSource.contains("no overlay decal pool"),
                "the p23 single-layer barrel deviation claim must stay retired (the overlay borrows landed)");
        assertFalse(tSource.contains("has no multiblockmains"),
                "the p29 no-tankwood/tankmetal-group claim must stay retired (the tank_valves borrows landed)");
        assertFalse(tSource.contains("barrel_metal\""), "the retired barrel_metal single-texture tail must not return");
    }
}
