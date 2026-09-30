/*
 * Offline pinned-census tests for task tex-composite-family (the R2 batch-3 swap):
 * the composite-energy four families (the 12 battery boxes, the 20 crystal chargers,
 * the 2 ZPM dechargers, the 5 LD transformer endpoints) leave the p29/p36 baked
 * composites for the two-layer grammar — the tintindex-0 grayscale colored body is the
 * mRGBa seat (upstream getTexture2 BlockTextureMulti(colored x mRGBa,
 * sOverlays[mActiveState & 3]): MultiTileEntityBatteryBox :31-:33 /
 * CrystalCharger :33-:36 / ZPMDechargerEU :39-:44 / LongDistanceTransformer
 * :284-:299), the six 0.01-plate overlay decals stay untinted, and ACTIVE switches
 * the shell to the overlay_active art (the trinary collapsed to the boolean — the
 * blinking third state stays the #18 defer).
 *
 * <p>Coverage (asserted against the committed generated + static trees, the
 * {@link GT6ConverterPaintRenderDatagenTest} shape):
 * <ul>
 * <li>the seven two-layer model pairs (7 dirs x {model, model_active}) keep the
 *     colored/overlay texture split over the SUBDIRECTORY layout, the cutout
 *     render_type and the untinted decals; the battery/crystal pair rides side on
 *     south (upstream has no back trio), the zpm/LD pair the real colored_back;</li>
 * <li>all 39 blockstates carry exactly the 12 six-way x active variants with the
 *     dispenser-form rotation map — the ACTIVE variant swaps to the _active model;</li>
 * <li>the tint seam: the battery tier ladder (Electric_T[0..5]) tints pairwise
 *     distinct, the carrier dispatch resolves the material column and non-carriers
 *     stay null; the T9 charger rung rides Trinaquadalloy (the upstream
 *     Electric_T[9] member the port ladder stops short of) and the ZPM rows ride
 *     Osmiridium (Loader :1000-:1001);</li>
 * <li>the 51 borrowed PNGs exist on the static tree and every one is grounded in
 *     assets/README.md by real sha256 (the GT6TextureCensusTest pin-e form);</li>
 * <li>the 15 retired src-over composites are DEAD (gone from the static tree — the
 *     old bake can never come back silently).</li>
 * </ul>
 *
 * <p>KJS face: none — this card's output is resources + datagen only.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6CrystalChargers;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.registry.GT6ZpmDechargers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

class GT6CompositeEnergyTexDatagenTest extends GTOfflineTestBase {

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** facing → rotationY (the FRONT band; FRONT on north in the model). */
    private static final Map<String, Integer> ROT_Y = Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90, "down", 0, "up", 0);

    /** facing → rotationX (the dispenser band — the verticals). */
    private static final Map<String, Integer> ROT_X = Map.of(
            "down", 90, "up", 270, "north", 0, "south", 0, "west", 0, "east", 0);

    /** The seven texture/model dirs: family -> has a real back face (the upstream getTexture2 trios). */
    private static final Map<String, Boolean> FAMILY_DIRS = Map.of(
            "battery_box", false, "battery_box_large", false,
            "crystal_charger", false, "crystal_charger_large", false,
            "zpm_decharger", true, "zpm_decharger_quantum", true,
            "long_distance_transformer", true);

    /** The 51 borrowed PNGs (tex-composite-family commit 1, the README tail manifest). */
    private static final List<String> BORROWED_PNGS = List.of(
            "battery_box/colored_front.png", "battery_box/colored_side.png",
            "battery_box/overlay_front.png", "battery_box/overlay_side.png",
            "battery_box/overlay_active_front.png", "battery_box/overlay_active_side.png",
            "battery_box_large/colored_front.png", "battery_box_large/colored_side.png",
            "battery_box_large/overlay_front.png", "battery_box_large/overlay_side.png",
            "battery_box_large/overlay_active_front.png", "battery_box_large/overlay_active_side.png",
            "crystal_charger/colored_front.png", "crystal_charger/colored_side.png",
            "crystal_charger/overlay_front.png", "crystal_charger/overlay_side.png",
            "crystal_charger/overlay_active_front.png", "crystal_charger/overlay_active_side.png",
            "crystal_charger_large/colored_front.png", "crystal_charger_large/colored_side.png",
            "crystal_charger_large/overlay_front.png", "crystal_charger_large/overlay_side.png",
            "crystal_charger_large/overlay_active_front.png", "crystal_charger_large/overlay_active_side.png",
            "zpm_decharger/colored_front.png", "zpm_decharger/colored_back.png", "zpm_decharger/colored_side.png",
            "zpm_decharger/overlay_front.png", "zpm_decharger/overlay_back.png", "zpm_decharger/overlay_side.png",
            "zpm_decharger/overlay_active_front.png", "zpm_decharger/overlay_active_back.png", "zpm_decharger/overlay_active_side.png",
            "zpm_decharger_quantum/colored_front.png", "zpm_decharger_quantum/colored_back.png", "zpm_decharger_quantum/colored_side.png",
            "zpm_decharger_quantum/overlay_front.png", "zpm_decharger_quantum/overlay_back.png", "zpm_decharger_quantum/overlay_side.png",
            "zpm_decharger_quantum/overlay_active_front.png", "zpm_decharger_quantum/overlay_active_back.png", "zpm_decharger_quantum/overlay_active_side.png",
            "long_distance_transformer/colored_front.png", "long_distance_transformer/colored_back.png", "long_distance_transformer/colored_side.png",
            "long_distance_transformer/overlay_front.png", "long_distance_transformer/overlay_back.png", "long_distance_transformer/overlay_side.png",
            "long_distance_transformer/overlay_active_front.png", "long_distance_transformer/overlay_active_back.png", "long_distance_transformer/overlay_active_side.png");

    /** The 15 retired src-over composites (the p29/p36 bakes this card deletes). */
    private static final List<String> RETIRED_PNGS = List.of(
            "battery_box.png", "battery_box_large.png",
            "crystal_charger_front.png", "crystal_charger_side.png",
            "crystal_charger_large_front.png", "crystal_charger_large_side.png",
            "long_distance_transformer_front.png", "long_distance_transformer_back.png", "long_distance_transformer_side.png",
            "zpm_decharger_front.png", "zpm_decharger_back.png", "zpm_decharger_side.png",
            "zpm_decharger_quantum_front.png", "zpm_decharger_quantum_back.png", "zpm_decharger_quantum_side.png");

    @BeforeAll
    static void bootMaterials() {
        // the material system after the base-class bootstrap (the converter census shape)
        GTMaterialItems.initMaterials();
        // the BLOCK registry write window (the GT6SingleBlockFacingIntegrityTest recipe:
        // the Block ctor registers its intrusive holder, NamespacedWrapper.validateWrite
        // rejects a frozen registry — the carrier-dispatch test constructs blocks)
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6CompositeEnergyTexDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertPngOnTree(String aRel) {
        try (InputStream tStream = GT6CompositeEnergyTexDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/textures/block/" + aRel)) {
            assertNotNull(tStream, "the borrowed PNG must exist on the static tree: " + aRel);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // ------------------------------------------------------------------
    // the seven two-layer model pairs
    // ------------------------------------------------------------------

    private void assertTwoLayerPair(String aFamily, boolean aBack) throws Exception {
        String tBase = "gt6:block/" + aFamily;
        String tModelRef = tBase + "/" + aFamily; // the model path doubles the dir, the texture refs do not
        JsonObject tModel = json("assets/gt6/models/block/" + aFamily + "/" + aFamily + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aFamily + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aFamily + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals(tBase + "/colored_front", tTextures.get("north").getAsString(),
                aFamily + ": north = the grayscale colored front (the tint seat)");
        assertEquals(tBase + (aBack ? "/colored_back" : "/colored_side"), tTextures.get("south").getAsString(),
                aFamily + ": south = " + (aBack ? "the colored back (the upstream OPOS seat)" : "the colored side (no back trio)"));
        for (String tSide : List.of("east", "west", "up", "down")) {
            assertEquals(tBase + "/colored_side", tTextures.get(tSide).getAsString(),
                    aFamily + ": " + tSide + " = the colored side");
        }
        assertEquals(tBase + "/overlay_front", tTextures.get("overlay_front").getAsString());
        assertEquals(tBase + (aBack ? "/overlay_back" : "/overlay_side"), tTextures.get("overlay_back").getAsString());
        assertEquals(tBase + "/overlay_side", tTextures.get("overlay_side").getAsString());
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aFamily + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aFamily + " body face " + tFace + " carries tintindex 0");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertFalse(tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        aFamily + " decal " + i + " face " + tFace + " stays untinted");
            }
        }
        // the ACTIVE child: the inactive parent with the overlay_active trio overrides
        JsonObject tActive = json("assets/gt6/models/block/" + aFamily + "/" + aFamily + "_active.json");
        assertEquals(tModelRef, tActive.get("parent").getAsString(), aFamily + "_active: the inactive parent");
        assertEquals("minecraft:cutout", tActive.get("render_type").getAsString(), aFamily + "_active: cutout re-declared");
        JsonObject tOverride = tActive.getAsJsonObject("textures");
        assertEquals(tBase + "/overlay_active_front", tOverride.get("overlay_front").getAsString(),
                aFamily + "_active: the front shell swaps to overlay_active");
        assertEquals(tBase + (aBack ? "/overlay_active_back" : "/overlay_active_side"), tOverride.get("overlay_back").getAsString(),
                aFamily + "_active: the back shell");
        assertEquals(tBase + "/overlay_active_side", tOverride.get("overlay_side").getAsString());
        assertFalse(tActive.has("elements"), aFamily + "_active: the body elements stay inherited");
    }

    @Test
    public void sevenFamilyModelPairsKeepTheTwoLayerGrammar() throws Exception {
        assertEquals(7, FAMILY_DIRS.size(), "the seven texture dirs (2 battery + 2 crystal + 2 zpm + 1 LD)");
        for (var tFamily : FAMILY_DIRS.entrySet()) {
            assertTwoLayerPair(tFamily.getKey(), tFamily.getValue());
        }
    }

    // ------------------------------------------------------------------
    // the 39 blockstates: the 12-variant six-way x active form
    // ------------------------------------------------------------------

    private void assertTwelveVariants(String aBlockPath, String aFamily) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aBlockPath + ".json").getAsJsonObject("variants");
        assertEquals(12, tVariants.size(), aBlockPath + ": exactly the 12 facing x active variants");
        for (String tFacing : ROT_Y.keySet()) {
            for (boolean tActive : new boolean[] {false, true}) {
                String tKey = "active=" + tActive + ",facing=" + tFacing;
                assertTrue(tVariants.has(tKey), aBlockPath + ": the variant key " + tKey);
                JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                assertEquals("gt6:block/" + aFamily + "/" + aFamily + (tActive ? "_active" : ""),
                        tVariant.get("model").getAsString(), aBlockPath + " " + tKey + ": the model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0, aBlockPath + " " + tKey + ": the rotationY");
                assertEquals(ROT_X.get(tFacing).intValue(),
                        tVariant.has("x") ? tVariant.get("x").getAsInt() : 0, aBlockPath + " " + tKey + ": the rotationX");
            }
        }
    }

    @Test
    public void allThirtyNineBlockstatesPinTheSixWayTwelveVariants() throws Exception {
        int tPinned = 0;
        for (GT6Batteries.BoxRow tRow : GT6Batteries.BOX_ROWS) {
            assertTwelveVariants(tRow.path(), tRow.slots() == 16 ? "battery_box_large" : "battery_box");
            tPinned++;
        }
        for (GT6CrystalChargers.ChargerRow tRow : GT6CrystalChargers.ROWS) {
            assertTwelveVariants(tRow.path(), tRow.slots() == 16 ? "crystal_charger_large" : "crystal_charger");
            tPinned++;
        }
        for (GT6ZpmDechargers.DechargerRow tRow : GT6ZpmDechargers.ROWS) {
            assertTwelveVariants(tRow.path(), tRow.path().equals("zpm_decharger_electric") ? "zpm_decharger" : tRow.path());
            tPinned++;
        }
        for (GT6LongDistanceTransformers.LDRow tRow : GT6LongDistanceTransformers.ROWS) {
            assertTwelveVariants(tRow.path(), "long_distance_transformer");
            tPinned++;
        }
        assertEquals(39, tPinned, "the 12 + 20 + 2 + 5 blockstate census");
    }

    /** The item models parent the family model (the ok3D form — the battery 2D transitional icons retired). */
    @Test
    public void itemModelsParentTheFamilyModel() throws Exception {
        for (GT6Batteries.BoxRow tRow : GT6Batteries.BOX_ROWS) {
            String tDir = tRow.slots() == 16 ? "battery_box_large" : "battery_box";
            assertEquals("gt6:block/" + tDir + "/" + tDir, json("assets/gt6/models/item/" + tRow.path() + ".json")
                    .get("parent").getAsString(), tRow.path() + ": the ok3D block-model parent");
        }
        for (GT6CrystalChargers.ChargerRow tRow : GT6CrystalChargers.ROWS) {
            String tDir = tRow.slots() == 16 ? "crystal_charger_large" : "crystal_charger";
            assertEquals("gt6:block/" + tDir + "/" + tDir, json("assets/gt6/models/item/" + tRow.path() + ".json")
                    .get("parent").getAsString(), tRow.path() + ": the item parents the family model");
        }
        for (GT6ZpmDechargers.DechargerRow tRow : GT6ZpmDechargers.ROWS) {
            String tDir = tRow.path().equals("zpm_decharger_electric") ? "zpm_decharger" : tRow.path();
            assertEquals("gt6:block/" + tDir + "/" + tDir, json("assets/gt6/models/item/" + tRow.path() + ".json")
                    .get("parent").getAsString(), tRow.path() + ": the item parents the family model");
        }
        for (GT6LongDistanceTransformers.LDRow tRow : GT6LongDistanceTransformers.ROWS) {
            assertEquals("gt6:block/long_distance_transformer/long_distance_transformer",
                    json("assets/gt6/models/item/" + tRow.path() + ".json").get("parent").getAsString(),
                    tRow.path() + ": the item parents the family model");
        }
    }

    // ------------------------------------------------------------------
    // the tint seam (the row material columns + the carrier dispatch)
    // ------------------------------------------------------------------

    @Test
    public void batteryTierLadderTintsPairwiseDistinct() {
        assertEquals(12, GT6Batteries.BOX_ROWS.size(), "the battery-box census stays 12");
        Set<Integer> tSeen = new HashSet<>();
        for (int tTier = 0; tTier <= 5; tTier++) {
            gregapi.oredict.OreDictMaterial tMat = gregtech6.registry.GT6ElectricTransformers.CASING_LADDER.get(tTier).get();
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, "tier " + tTier + " binds full alpha");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    "tier " + tTier + " colour " + Integer.toHexString(tTint) + " is distinct — the all-gray bake is gone");
        }
    }

    @Test
    public void tintLiteralsAnchorTheOddRows() {
        // the ZPM rows ride Osmiridium (Loader :1000-:1001 NBT_MATERIAL, both rows) and
        // the charger T9 rung rides Trinaquadalloy (the upstream Electric_T[9] member,
        // MT.java:3691 — the port CASING_LADDER stops at [8])
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.Osmiridium) | 0xFF000000,
                gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Osmiridium, 0),
                "the Osmiridium tint (the ZPM rows)");
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.Trinaquadalloy) | 0xFF000000,
                gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Trinaquadalloy, 0),
                "the Trinaquadalloy tint (the charger T9 rung)");
        // the Trinaquadalloy rung is NOT on the shared ladder (the converter card's 9-member
        // copy stays — the charger family resolves its own tail)
        assertSame(gregapi.data.MT.Trinitanium,
                gregtech6.registry.GT6ElectricTransformers.CASING_LADDER.get(8).get(), "the shared ladder ends at [8]");
    }

    @Test
    public void batteryBoxMaterialCarrierDispatch() {
        GT6BatteryBoxBlock tCarrier = new GT6BatteryBoxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(),
                0, 4, () -> null, () -> gregapi.data.TD.Energy.EU, () -> gregapi.data.MT.StainlessSteel);
        assertSame(gregapi.data.MT.StainlessSteel, tCarrier.material(), "the carrier resolves its material");
        assertSame(gregapi.data.MT.StainlessSteel, GT6BatteryBoxBlock.materialOf(tCarrier), "the dispatch resolves the carrier");
        assertNull(GT6BatteryBoxBlock.materialOf(new GT6BatteryBoxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), 0, 4, () -> null)),
                "a material-less box stays null");
        assertNull(GT6BatteryBoxBlock.materialOf(net.minecraft.world.level.block.Blocks.STONE), "a non-carrier stays null");
    }

    /** The tint-walk arrays the GTMachineTintModel walk consumes total 12 + 20 + 2 + 5 blocks. */
    @Test
    public void paintableArraysTotalTheRowTables() {
        assertEquals(12, GT6Batteries.BOX_ROWS.size(), "the battery walk total");
        assertEquals(20, GT6CrystalChargers.ROWS.size(), "the charger walk total");
        assertEquals(2, GT6ZpmDechargers.ROWS.size(), "the ZPM walk total");
        assertEquals(5, GT6LongDistanceTransformers.ROWS.size(), "the LD walk total");
    }

    // ------------------------------------------------------------------
    // the static tree: the 51 borrows grounded, the 15 composites dead
    // ------------------------------------------------------------------

    @Test
    public void allFiftyOneBorrowedPngsExistAndAreLedgered() throws Exception {
        assertEquals(51, BORROWED_PNGS.size(), "the borrow census");
        Path tMdkRoot = mdkRoot();
        String tReadme = Files.readString(tMdkRoot.resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
        MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
        List<String> tViolations = new java.util.ArrayList<>();
        for (String tRel : BORROWED_PNGS) {
            assertPngOnTree(tRel);
            Path tPng = tMdkRoot.resolve("src/main/resources/assets/gt6/textures/block").resolve(tRel);
            String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
            if (!tReadme.contains(tRel)) {
                tViolations.add(tRel + " — filename absent from assets/README.md");
            }
            if (!tReadme.contains(tHex)) {
                tViolations.add(tRel + " — file bytes hash to " + tHex + ", not grounded in assets/README.md");
            }
        }
        assertTrue(tViolations.isEmpty(), String.join("\n", tViolations));
    }

    @Test
    public void retiredCompositesAreDead() {
        assertEquals(15, RETIRED_PNGS.size(), "the retirement census");
        for (String tRel : RETIRED_PNGS) {
            try (InputStream tStream = GT6CompositeEnergyTexDatagenTest.class.getClassLoader()
                    .getResourceAsStream("assets/gt6/textures/block/" + tRel)) {
                assertNull(tStream, "the retired composite must be GONE from the static tree: " + tRel);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    /** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
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
