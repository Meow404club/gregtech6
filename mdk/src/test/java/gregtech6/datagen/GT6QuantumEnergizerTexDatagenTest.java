/*
 * Offline pinned-census tests for task quantum-energizer-tint-overlay (the user-named
 * symptom26 close-out): the 5 Quantum Energizer rungs (Loader_MultiTileEntities.java
 * :961-966, ids 10121-10125, every row NBT_MATERIAL MT.Osmiridium) leave the derived
 * amber two-face posture for the byte-verbatim upstream borrows — the two-layer
 * addBridgeFamily grammar:
 * <ul>
 * <li>the grayscale {@code colored/} trio is the mRGBa tint seat (upstream
 *     MultiTileEntityQuantumEnergizerLaser.java:45 {@code BlockTextureDefault(
 *     sColoreds[aIndex], mRGBa)} — all three faces are ONE byte-identical upstream
 *     PNG);</li>
 * <li>the {@code overlay/} trio rides untinted and the {@code overlay_active/} trio
 *     swaps in on the ACTIVE channel (:45 {@code mActivity.mState>0 ? sOverlaysActive
 *     : sOverlays} — the port switch is the GT6DynamoBlock ACTIVE property + the
 *     GT6DynamoBlockEntity.syncActiveToState arm, the blockstate pins it here);</li>
 * <li>the former self-made amber front/side pair is DEAD (the tex-bridge-kinetic
 *     "alpha-0 empty overlay layers" probe verdict was wrong: the overlays carry
 *     108/88/144 opaque texels of real art each).</li>
 * </ul>
 *
 * <p>Coverage (asserted against the committed generated + static trees, the
 * {@link GT6CompositeEnergyTexDatagenTest} shape): the two-layer model pair keeps the
 * colored/overlay texture split over the flat band layout, the cutout render_type and
 * the untinted decals; all 5 blockstates carry exactly the 12 six-way x active variants
 * with the dispenser-form rotation map — the ACTIVE variant swaps to the _active model;
 * the tint seam resolves the Osmiridium column (the literal 0xFF6464FF, MT.java:2524);
 * the 9 borrowed PNGs exist on the static tree, every one grounded in assets/README.md
 * by real sha256 AND hard-pinned to the upstream digest; the 2 retired amber files are
 * gone. KJS face: none — resources + datagen only.
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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

class GT6QuantumEnergizerTexDatagenTest extends GTOfflineTestBase {

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** facing → rotationY (the FRONT band; FRONT on north in the model). */
    private static final Map<String, Integer> ROT_Y = Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90, "down", 0, "up", 0);

    /** facing → rotationX (the dispenser band — the verticals). */
    private static final Map<String, Integer> ROT_X = Map.of(
            "down", 90, "up", 270, "north", 0, "south", 0, "west", 0, "east", 0);

    /** The five rung paths, upstream line order :961-966. */
    private static final List<String> ROW_PATHS = List.of(
            "quantum_energizer", "quantum_energizer_t2", "quantum_energizer_t3",
            "quantum_energizer_t4", "quantum_energizer_t5");

    /**
     * The 9 borrowed PNGs hard-pinned to their UPSTREAM sha256 (the sha ledger — the
     * id1502 rule: upstream art moves verbatim, never re-authored). colored front/back/
     * side are ONE byte-identical upstream file (quantum_laser/colored/{front,back,side}
     * all hash d3dd56be…).
     */
    private static final Map<String, String> BORROWED_PNG_SHA256 = Map.ofEntries(
            Map.entry("quantum_energizer_colored_front.png",
                    "d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e"),
            Map.entry("quantum_energizer_colored_back.png",
                    "d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e"),
            Map.entry("quantum_energizer_colored_side.png",
                    "d3dd56bee9e777c0800831c570e523300b4c100a75b23a1b6c9ed3948e38b25e"),
            Map.entry("quantum_energizer_overlay_front.png",
                    "d1152f690913d11bc286fec25b44a44cf4ce38ded8c72c3da2b404f353ebdbeb"),
            Map.entry("quantum_energizer_overlay_back.png",
                    "8782f3171417fe0bd61a410129ee779b9df26ff796e530beea7502afe01f1e06"),
            Map.entry("quantum_energizer_overlay_side.png",
                    "5a1aa7f1375203df87905992547a6cb21fd81f90ae327e86c16ed77fc7b13514"),
            Map.entry("quantum_energizer_overlay_active_front.png",
                    "8aa1e3925d65fd057270593e8f56b445ebb8287f872fe12f743c695a28aae8db"),
            Map.entry("quantum_energizer_overlay_active_back.png",
                    "0fe48976578fe023b4ad2ec97d7839ebf3bb53825ad67c5e6ce376462c371112"),
            Map.entry("quantum_energizer_overlay_active_side.png",
                    "fef048ade7a8eb479f27b587ffc4648e536059f1b9ad471f0ae3c15723b79bc4"));

    /** The 2 retired self-made amber faces (the qu-energizer derived pair this card deletes). */
    private static final List<String> RETIRED_PNGS = List.of(
            "quantum_energizer_front.png", "quantum_energizer_side.png");

    @BeforeAll
    static void bootMaterials() {
        // the material system after the base-class bootstrap (the composite census shape)
        GTMaterialItems.initMaterials();
        // the BLOCK registry write window (the GT6CompositeEnergyTexDatagenTest recipe:
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
        try (InputStream tStream = GT6QuantumEnergizerTexDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer model pair (the flat band layout)
    // ------------------------------------------------------------------

    @Test
    public void modelPairKeepsTheTwoLayerGrammar() throws Exception {
        String tBase = "gt6:block/quantum_energizer";
        JsonObject tModel = json("assets/gt6/models/block/quantum_energizer.json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), "cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals(tBase + "_colored_front", tTextures.get("north").getAsString(),
                "north = the grayscale colored front (the mRGBa tint seat)");
        assertEquals(tBase + "_colored_back", tTextures.get("south").getAsString(),
                "south = the colored back (the upstream OPOS face)");
        for (String tSide : List.of("east", "west", "up", "down")) {
            assertEquals(tBase + "_colored_side", tTextures.get(tSide).getAsString(),
                    tSide + " = the colored side");
        }
        assertEquals(tBase + "_overlay_front", tTextures.get("overlay_front").getAsString());
        assertEquals(tBase + "_overlay_back", tTextures.get("overlay_back").getAsString());
        assertEquals(tBase + "_overlay_side", tTextures.get("overlay_side").getAsString());
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), "body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    "body face " + tFace + " carries tintindex 0 (the Osmiridium seat)");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertFalse(tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        "decal " + i + " face " + tFace + " stays untinted (the overlay pass)");
            }
        }
        // the ACTIVE child: the inactive parent with the overlay_active trio overrides —
        // the upstream mActivity.mState>0 arm (MultiTileEntityQuantumEnergizerLaser :45)
        JsonObject tActive = json("assets/gt6/models/block/quantum_energizer_active.json");
        assertEquals("gt6:block/quantum_energizer", tActive.get("parent").getAsString(),
                "_active: the inactive parent");
        assertEquals("minecraft:cutout", tActive.get("render_type").getAsString(),
                "_active: cutout re-declared");
        JsonObject tOverride = tActive.getAsJsonObject("textures");
        assertEquals(tBase + "_overlay_active_front", tOverride.get("overlay_front").getAsString(),
                "_active: the front shell swaps to overlay_active");
        assertEquals(tBase + "_overlay_active_back", tOverride.get("overlay_back").getAsString(),
                "_active: the back shell");
        assertEquals(tBase + "_overlay_active_side", tOverride.get("overlay_side").getAsString(),
                "_active: the side shell");
        assertFalse(tActive.has("elements"), "_active: the body elements stay inherited");
    }

    // ------------------------------------------------------------------
    // the 5 blockstates: the 12-variant six-way x active form — the ACTIVE switch pin
    // ------------------------------------------------------------------

    @Test
    public void allFiveBlockstatesPinTheSixWayTwelveVariants() throws Exception {
        assertEquals(5, GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS.size(), "the rung census stays 5");
        for (String tPath : ROW_PATHS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            assertEquals(12, tVariants.size(), tPath + ": exactly the 12 facing x active variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tActive : new boolean[] {false, true}) {
                    String tKey = "active=" + tActive + ",facing=" + tFacing;
                    assertTrue(tVariants.has(tKey), tPath + ": the variant key " + tKey);
                    JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                    assertEquals("gt6:block/quantum_energizer" + (tActive ? "_active" : ""),
                            tVariant.get("model").getAsString(), tPath + " " + tKey + ": the model");
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0, tPath + " " + tKey + ": the rotationY");
                    assertEquals(ROT_X.get(tFacing).intValue(),
                            tVariant.has("x") ? tVariant.get("x").getAsInt() : 0, tPath + " " + tKey + ": the rotationX");
                }
            }
        }
    }

    /** The item models parent the family model (the addBridgeFamily form). */
    @Test
    public void itemModelsParentTheFamilyModel() throws Exception {
        for (String tPath : ROW_PATHS) {
            String tExpected = tPath.equals("quantum_energizer")
                    ? "gt6:block/quantum_energizer" : "gt6:item/quantum_energizer";
            assertEquals(tExpected, json("assets/gt6/models/item/" + tPath + ".json")
                    .get("parent").getAsString(), tPath + ": the family model parent");
        }
    }

    // ------------------------------------------------------------------
    // the tint seam (the Osmiridium column + the carrier dispatch)
    // ------------------------------------------------------------------

    @Test
    public void rowsCarryTheOsmiridiumColumnAndTheDispatchResolvesIt() {
        // Loader :961-966 — every row NBT_MATERIAL MT.Osmiridium (the same material on
        // all five rungs; the tint ARGB is the material colour, MT.java:2524 = 100,100,255)
        for (var tRow : GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS) {
            assertSame(gregapi.data.MT.Osmiridium, tRow.material().get(),
                    tRow.path() + " carries the Osmiridium NBT_MATERIAL column");
        }
        // the GT6DynamoBlock carrier dispatch (the materialOf arm the walks consume)
        GT6DynamoBlock tCarrier = new GT6DynamoBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(),
                0, () -> null, () -> gregapi.data.MT.Osmiridium);
        assertSame(gregapi.data.MT.Osmiridium, tCarrier.material(), "the carrier resolves its material");
        assertSame(gregapi.data.MT.Osmiridium, GT6DynamoBlock.materialOf(tCarrier), "the dispatch resolves the carrier");
        assertNull(GT6DynamoBlock.materialOf(net.minecraft.world.level.block.Blocks.STONE), "a non-carrier stays null");
        // the tint literal: Osmiridium = (100,100,255) → 0xFF6464FF (MT.java:2524), full alpha
        int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Osmiridium, 0);
        assertEquals(0xFF6464FF, tTint, "the Osmiridium tint literal (100,100,255)");
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.Osmiridium) | 0xFF000000,
                tTint, "the tint rides the material colour, not a hardcode");
    }

    // ------------------------------------------------------------------
    // the static tree: the 9 borrows hard-pinned + grounded, the 2 ambers dead
    // ------------------------------------------------------------------

    @Test
    public void allNineBorrowsAreByteVerbatimUpstreamAndLedgered() throws Exception {
        assertEquals(9, BORROWED_PNG_SHA256.size(), "the borrow census");
        Path tMdkRoot = mdkRoot();
        String tReadme = Files.readString(tMdkRoot.resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
        MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
        List<String> tViolations = new java.util.ArrayList<>();
        for (var tEntry : BORROWED_PNG_SHA256.entrySet()) {
            Path tPng = tMdkRoot.resolve("src/main/resources/assets/gt6/textures/block").resolve(tEntry.getKey());
            assertTrue(Files.isRegularFile(tPng), "the borrowed PNG must exist on the static tree: " + tEntry.getKey());
            String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
            if (!tHex.equals(tEntry.getValue())) {
                tViolations.add(tEntry.getKey() + " — hashes to " + tHex + ", the upstream pin is " + tEntry.getValue());
            }
            if (!tReadme.contains(tEntry.getKey())) {
                tViolations.add(tEntry.getKey() + " — filename absent from assets/README.md");
            }
        }
        assertTrue(tViolations.isEmpty(), String.join("\n", tViolations));
    }

    @Test
    public void retiredAmberFacesAreDead() {
        assertEquals(2, RETIRED_PNGS.size(), "the retirement census");
        for (String tRel : RETIRED_PNGS) {
            try (InputStream tStream = GT6QuantumEnergizerTexDatagenTest.class.getClassLoader()
                    .getResourceAsStream("assets/gt6/textures/block/" + tRel)) {
                assertNull(tStream, "the retired amber face must be GONE from the static tree: " + tRel);
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
