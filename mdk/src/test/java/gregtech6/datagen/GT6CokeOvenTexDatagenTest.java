/*
 * Offline pinned tests for task coke-oven-texture: the last placeholder multiblock
 * main swaps the script-generated white placeholders for the official 6.10.20 asset
 * pack's coke oven art (upstream {@code machines/basicmachines/cokeoven/}, the
 * standard basicmachine four-state scheme — the declared-deviation narrative and the
 * per-file sha256 ledger live in assets/README.md). Asserted against the committed
 * trees (the GT6MultiblockMainsTexDatagenTest shape):
 * <ul>
 * <li>the five borrowed PNGs hash to the two pinned upstream digests (the six colored
 *     faces share ONE body image, the window decal is the overlay/front borrow);</li>
 * <li>the FORMED dual-model era is dead: the front_formed PNG is gone from disk and no
 *     generated JSON references the formed model names;</li>
 * <li>the controller model is the two-layer window form (tintindex-0 body + the
 *     untinted 0.01 north window plate, cutout) and the 8 FACING x FORMED states share
 *     it (the graagg single-band form);</li>
 * <li>the block rides the GTMultiBlockControllerBlock material carrier (MT.Ceramic,
 *     Loader :1193 — the same column the bricks share) and tints through the seam;</li>
 * <li>the four-state census: colored + overlay/front borrowed, overlay_active +
 *     overlay_running fronts deferred (no ACTIVE/RUNNING channel), pinned absent.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregtech6.block.multiblock.GTCokeOvenBlock;
import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.client.render.GTMachinePaintTint;
import gregtech6.registry.GTMaterialItems;

class GT6CokeOvenTexDatagenTest {

    /** All six upstream {@code basicmachines/cokeoven/colored/} faces hash to this one shared brick body. */
    private static final String UPSTREAM_COLORED_BODY =
            "b0a5a36bbc4a8f392f602efcc7e87e3761ee27097d35feae2045f6a1c535947f";

    /** The upstream {@code overlay/front.png} window (also the overlay_running/front bytes — the unlit window). */
    private static final String UPSTREAM_WINDOW =
            "429da649536bd4fdb733fedfcdfbe62f4772854923b218b63272fcbaa78d41dc";

    /**
     * The upstream {@code overlay_active/front.png} (the lit window) — DEFERRED, not
     * shipped: the port's controllers carry no ACTIVE/RUNNING render channel. Pinned
     * here so the defer is auditable against the upstream snapshot; the README names
     * the defer without the digest (the whole-ledger grounding pin would reject an
     * unshipped hex).
     */
    private static final String UPSTREAM_ACTIVE_WINDOW =
            "74dc68c748b5fbb4b9e4384379874afba75150d411490accc817afe5db661665";

    /** The four borrowed colored faces (side←left; all six upstream faces are the same image). */
    private static final List<String> COLORED_FILES = List.of(
            "multiblock_coke_oven_bottom.png", "multiblock_coke_oven_top.png",
            "multiblock_coke_oven_side.png", "multiblock_coke_oven_front.png");

    /** facing → rotationY (the addMultiBlocks y table — NORTH identity). */
    private static final Map<String, Integer> ROT_Y = Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    @BeforeAll
    static void bootMaterialsThenUnfreeze() {
        // the vanilla bootstrap + material system (the multiblockmains-census shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        GTMaterialItems.initMaterials();
        // the BLOCK write window (the GT6SingleBlockFacingIntegrityTest recipe): the
        // carrier-block fixture constructs post-freeze, and the Forge-patched Block ctor
        // registers its intrusive holder — reopen the registry so `new` stays legal offline
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    private static InputStream resource(String aPath) {
        return GT6CokeOvenTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String sha256(Path aFile) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(aFile)));
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

    private static Path blockTexture(String aName) {
        return mdkRoot().resolve("src/main/resources/assets/gt6/textures/block").resolve(aName);
    }

    // ------------------------------------------------------------------
    // the borrowed art (the README ledger's grounding face)
    // ------------------------------------------------------------------

    @Test
    public void borrowedTexturesCarryTheUpstreamBytes() throws Exception {
        for (String tFile : COLORED_FILES) {
            Path tPng = blockTexture(tFile);
            assertTrue(Files.isRegularFile(tPng), tFile + " must be borrowed");
            assertEquals(UPSTREAM_COLORED_BODY, sha256(tPng),
                    tFile + " drifts from the shared colored body (README ledger row)");
        }
        assertEquals(UPSTREAM_WINDOW, sha256(blockTexture("multiblock_coke_oven_overlay_front.png")),
                "the window decal drifts from the overlay/front borrow (README ledger row)");
    }

    /**
     * The four-state census: the borrowed states (colored + the static window) are on
     * disk, the deferred states (overlay_active/front, overlay_running/front) are NOT —
     * the port has no ACTIVE/RUNNING channel, the README declares the defer. The static
     * window IS the running art (the two upstream files are byte-identical), so the
     * unformed look already carries the official window.
     */
    @Test
    public void fourStateCensusPinsTheBorrowAndTheDefer() {
        for (String tFile : COLORED_FILES) {
            assertTrue(Files.isRegularFile(blockTexture(tFile)), "borrowed: " + tFile);
        }
        assertTrue(Files.isRegularFile(blockTexture("multiblock_coke_oven_overlay_front.png")),
                "borrowed: the overlay/front window decal");
        assertFalse(Files.exists(blockTexture("multiblock_coke_oven_overlay_active_front.png")),
                "deferred: no ACTIVE channel, the lit window must not ship");
        assertFalse(Files.exists(blockTexture("multiblock_coke_oven_overlay_running_front.png")),
                "deferred: no RUNNING channel (and its bytes equal the borrowed window)");
    }

    // ------------------------------------------------------------------
    // the FORMED dual-model retirement
    // ------------------------------------------------------------------

    /** The retired placeholder is gone from disk and no generated JSON references it. */
    @Test
    public void retiredFormedArtifactsAreDeadEverywhere() throws Exception {
        assertFalse(Files.exists(blockTexture("multiblock_coke_oven_front_formed.png")),
                "the retired front_formed placeholder must be deleted");
        Path tGenerated = mdkRoot().resolve("src/generated/resources/assets/gt6");
        assertTrue(Files.isDirectory(tGenerated), "runData must have produced the generated tree");
        try (Stream<Path> tWalk = Files.walk(tGenerated)) {
            for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String tContent = Files.readString(tFile, StandardCharsets.UTF_8);
                assertFalse(tContent.contains("multiblock_coke_oven_formed"),
                        tFile.getFileName() + " still references the retired FORMED model");
                assertFalse(tContent.contains("multiblock_coke_oven_front_formed"),
                        tFile.getFileName() + " still references the retired front_formed art");
            }
        }
    }

    // ------------------------------------------------------------------
    // the two-layer window model + the 8-state single-band blockstate
    // ------------------------------------------------------------------

    @Test
    public void controllerModelKeepsTheTwoLayerWindowForm() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/multiblock_coke_oven.json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), "cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                "cutout — the window's transparent texels discard");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/multiblock_coke_oven_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/multiblock_coke_oven_top", tTextures.get("up").getAsString());
        assertEquals("gt6:block/multiblock_coke_oven_front", tTextures.get("north").getAsString(),
                "north IS the model-space front");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/multiblock_coke_oven_side", tTextures.get(tSide).getAsString(),
                    tSide + " = the shared side art");
        }
        assertEquals("gt6:block/multiblock_coke_oven_overlay_front", tTextures.get("window").getAsString(),
                "the window key rides the overlay/front borrow");
        // the two-layer split: element 0 the tinted body (six faces, tintindex 0 — the
        // Ceramic seat), element 1 the untinted 0.01 north window plate
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(2, tElements.size(), "body + the single window decal");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(6, tBodyFaces.size(), "the body cube is complete");
        for (String tFace : tBodyFaces.keySet()) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    "body face " + tFace + " carries tintindex 0 (the material tint seat)");
        }
        var tWindow = tElements.get(1).getAsJsonObject();
        assertEquals(-0.01F, tWindow.getAsJsonArray("from").get(2).getAsFloat(), "the window plate floats north");
        var tWindowFaces = tWindow.getAsJsonObject("faces");
        assertEquals(1, tWindowFaces.size(), "the decal is the single north plate");
        JsonObject tNorthFace = tWindowFaces.getAsJsonObject("north");
        assertNotNull(tNorthFace, "the window faces north");
        assertEquals("#window", tNorthFace.get("texture").getAsString());
        assertFalse(tNorthFace.has("tintindex"), "the window stays untinted (the P22 decal split)");
        assertEquals("north", tNorthFace.get("cullface").getAsString());
    }

    @Test
    public void allEightFacingFormedStatesShareTheOneModel() throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/multiblock_coke_oven.json").getAsJsonObject("variants");
        assertEquals(8, tVariants.size(), "exactly the 4 facing x 2 formed variants");
        for (String tFacing : ROT_Y.keySet()) {
            for (boolean tFormed : new boolean[] {false, true}) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing + ",formed=" + tFormed);
                assertEquals("gt6:block/multiblock_coke_oven", tVariant.get("model").getAsString(),
                        "facing=" + tFacing + " formed=" + tFormed + ": the single band model "
                                + "(the FORMED look retired, the property stays the RCON surface)");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        "facing=" + tFacing + ": the rotationY table");
            }
        }
    }

    // ------------------------------------------------------------------
    // the tint chain: the Ceramic row carrier + the walk seat
    // ------------------------------------------------------------------

    @Test
    public void controllerCarriesTheCeramicRowAndTintsThroughTheSeam() {
        // the null-material ctor is gone: the block resolves the :1193 NBT_MATERIAL row
        // (MT.Ceramic — the same column the bricks share through the census constant)
        var tProps = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        assertSame(MT.Ceramic, GTMultiBlockControllerBlock.materialOf(new GTCokeOvenBlock(tProps)),
                "the coke oven controller rides the material-carrier ctor (Loader :1193)");
        assertSame(gregtech6.registry.GTMultiBlocks.COKE_BRICKS_MATERIAL.get(),
                GTMultiBlockControllerBlock.materialOf(new GTCokeOvenBlock(tProps)),
                "the carrier resolves the same census constant the bricks share");
        // and the colour flows through the shared seam (the walk's dispatch arm)
        int tTint = GTMachinePaintTint.tintARGB(
                net.minecraftforge.client.model.data.ModelData.EMPTY, MT.Ceramic, 0);
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(MT.Ceramic), tTint & 0xFFFFFF,
                "the tintindex-0 seat reads the Ceramic row colour through tintARGB");
    }
}
