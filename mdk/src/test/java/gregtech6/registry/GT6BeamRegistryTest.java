/**
 * Tests for task beam-blocks-register: the 8 wood-beam block universe — the census/audit
 * unit in the GT6TreeBlocksCensusTest posture (offline-safe by construction: enum walks,
 * string paths, classpath JSON/PNG reads; no RegisterEvent, no bootstrapped registries).
 *
 * <p>Upstream anchors: the vanilla LIST_BEAMS subset — Beam1 meta 0-3
 * (LoaderWoodDictionary.java:51-54 Oak/Spruce/Birch/Jungle), Beam2 meta 0-1 (:55-56
 * Acacia/DarkOak) + the IL.Beam default face (:66 "Wood Beam") + the Rubber Wood face
 * (:175); the display rows BlockTreeBeam1.java:31-48 / BlockTreeBeam2.java:31-48; the
 * 1.7.10 zh dump faces (tmp/gregtech.lang gt.block.beam.1.0-.3 / 2.0-.3); the geometry
 * (BlockBaseBeam.java:60 PILLAR_RENDER = CS.java:764 render 31, the vanilla log renderer
 * — full cube, no reduced cross-section; :65 the TOP/SIDE icon pair).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.tree.GT6BeamKind;

class GT6BeamRegistryTest {

    /** The 8 kinds, upstream order (Beam1 0-3, then Beam2 0-3). */
    private static final List<String> SNAKES = List.of("oak", "spruce", "birch", "jungle",
            "acacia", "dark_oak", "rubber_wood", "wood");

    /** rotationX/rotationY per axis variant, ordered {axis=x, axis=y, axis=z} (0 = omitted key) — the vanilla axisBlock map. */
    private static final int[] X_ROT = {90, 0, 90};
    private static final int[] Y_ROT = {90, 0, 180};

    /** The 16 borrowed-PNG digests (the assets README beam section, task beam-blocks-register). */
    private static final List<String> TEXTURE_DIGESTS = List.of(
            "2fe75b7f799bc0c905c9c5ebbd0da7b9ff4d3e962f2cb0a9c041d800fcdaf8a0",
            "067570cc996ad9db3705aa8fb0a31a33abe25868cd3798a87d1e2a519ab6bef5",
            "cc2df784eccb6d32bb3243f317457aae0ae5039cbf56133ebca81c87e8cff008",
            "6f4ac051ead59da193ea625cf8211adbcfc7caa321b29c76943d3f9aeda14b87",
            "c9ac39e363aa066e49c479ac2a9c9640232a1ead0b82d17a5f1a1e9014d3b467",
            "183e6ab631723e7eeca90f70931ece7138bacf625cfe862058e6f127760bc836",
            "0b6085ee35cd549b67a4a87ec1317c34afcb17c10d3dc6e935d697ae48d7f393",
            "a2e9094402a7cd7961baeaedff848a80ed972f452d9623ff690449f81c69860e",
            "a5298f2af6ebd8a1116eb2ab457cc7431e45129b86fbffe17fe50510cb419829",
            "aee8da2ef0dc3e1094d26ae49c6f07acc9d02243f7a6c1b50e8f9cb3d8c4967c",
            "bbe055a3c023cd3241095396a791e837576476c992c2bba40cd55b7df53b02e5",
            "c5553ce6336fc5f17aaeb8665eca26d274c194dd8c68a7b3af5e674632b9feef",
            "f0d9cc40fdcd94e7e46e296a92e3ebd121d7a471fe47708f603ce01b81125a9d",
            "ac01916d990e06e5b1b1d8889f56ea6b87b72f39f9d469d6fbe05a64682ccceb",
            "109cf88ed3e8c9710e1f23ed19c7e60d3508a32ba42dd377ce6fbfe63bf5deb0",
            "32abe24f27715308c9f3f613f50c9c2437499e4943a87986a360a8472adb6072");

    /** The census: 8 rows in the upstream LIST_BEAMS vanilla-subset order, snakes distinct. */
    @Test
    void kindsOrderIsTheUpstreamListBeamsOrder() {
        assertEquals(8, GT6BeamBlocks.KINDS.size(), "8 vanilla-subset beams (Beam1x4 + Beam2x4, research.prefix-gap-survey)");
        for (int i = 0; i < SNAKES.size(); i++) {
            assertEquals(SNAKES.get(i), GT6BeamBlocks.KINDS.get(i).snake(),
                    "kind " + i + " must follow the upstream meta order");
        }
        assertEquals(8, GT6BeamBlocks.BLOCKS.size(), "8 block registrations");
        assertEquals(8, GT6BeamBlocks.ITEMS.size(), "8 block-item registrations");
        Set<String> tPaths = new HashSet<>();
        for (GT6BeamKind tKind : GT6BeamBlocks.KINDS) {
            assertTrue(tPaths.add(GT6BeamBlocks.path(tKind)), "path ids must be distinct: " + GT6BeamBlocks.path(tKind));
        }
        assertEquals(8, tPaths.size(), "8 distinct <snake>_beam ids");
    }

    /** The spot pins: the first/last/wood rows carry their registration ids. */
    @Test
    void spotPathsPinTheRegistryIds() {
        assertEquals("oak_beam", GT6BeamBlocks.path(GT6BeamKind.OAK), "Beam1 meta 0");
        assertEquals("dark_oak_beam", GT6BeamBlocks.path(GT6BeamKind.DARK_OAK), "Beam2 meta 1");
        assertEquals("rubber_wood_beam", GT6BeamBlocks.path(GT6BeamKind.RUBBER_WOOD), "Beam2 meta 2 (the :175 rubber face)");
        assertEquals("wood_beam", GT6BeamBlocks.path(GT6BeamKind.WOOD), "Beam2 meta 3 (the :66 IL.Beam default face)");
    }

    /** The en display names are the upstream LH rows verbatim (BlockTreeBeam1:31-48 / BlockTreeBeam2:31-48). */
    @Test
    void enNamesAreTheUpstreamLhRows() {
        assertEquals("Oak Beam", GT6BeamKind.OAK.enName(), "BlockTreeBeam1.java:31");
        assertEquals("Spruce Beam", GT6BeamKind.SPRUCE.enName(), ":36");
        assertEquals("Birch Beam", GT6BeamKind.BIRCH.enName(), ":41");
        assertEquals("Jungle Beam", GT6BeamKind.JUNGLE.enName(), ":46");
        assertEquals("Acacia Beam", GT6BeamKind.ACACIA.enName(), "BlockTreeBeam2.java:31");
        assertEquals("Dark Oak Beam", GT6BeamKind.DARK_OAK.enName(), ":36");
        assertEquals("Rubber Wood Beam", GT6BeamKind.RUBBER_WOOD.enName(), ":41");
        assertEquals("Wood Beam", GT6BeamKind.WOOD.enName(), ":46");
    }

    /** The zh display names are the 1.7.10 dump faces verbatim (gt.block.beam.1.0-.3 / 2.0-.3). */
    @Test
    void zhNamesAreTheDumpFaces() {
        assertEquals("橡木梁", GT6BeamKind.OAK.zhName(), "dump gt.block.beam.1.0");
        assertEquals("云杉木梁", GT6BeamKind.SPRUCE.zhName(), "gt.block.beam.1.1");
        assertEquals("白桦木梁", GT6BeamKind.BIRCH.zhName(), "gt.block.beam.1.2");
        assertEquals("丛林木梁", GT6BeamKind.JUNGLE.zhName(), "gt.block.beam.1.3");
        assertEquals("金合欢木梁", GT6BeamKind.ACACIA.zhName(), "dump gt.block.beam.2.0");
        assertEquals("深色橡木梁", GT6BeamKind.DARK_OAK.zhName(), "gt.block.beam.2.1");
        assertEquals("橡胶木梁", GT6BeamKind.RUBBER_WOOD.zhName(), "gt.block.beam.2.2");
        assertEquals("木梁", GT6BeamKind.WOOD.zhName(), "gt.block.beam.2.3");
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6BeamRegistryTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The 8 blockstates pin the vanilla axisBlock rotation map (the addAxles band form: X=x90/y90, Y=none, Z=x90/y180). */
    @Test
    void beamBlockstatesPinTheAxisRotationMap() throws Exception {
        for (String tSnake : SNAKES) {
            String tPath = tSnake + "_beam.json";
            JsonObject tState = generatedJson("assets/gt6/blockstates/" + tPath);
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(3, tVariants.size(), tPath + ": exactly the three axis variants");
            String[] tAxes = {"axis=x", "axis=y", "axis=z"};
            for (int i = 0; i < 3; i++) {
                JsonObject tVariant = tVariants.getAsJsonObject(tAxes[i]);
                assertEquals("gt6:block/" + tSnake + "_beam", tVariant.get("model").getAsString(),
                        tPath + " " + tAxes[i] + ": the per-kind cube_column model");
                assertEquals(X_ROT[i],
                        tVariant.has("x") ? tVariant.get("x").getAsInt() : 0,
                        tPath + " " + tAxes[i] + ": the rotationX");
                assertEquals(Y_ROT[i],
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tPath + " " + tAxes[i] + ": the rotationY");
            }
        }
    }

    /** axis=y stays rotation-free (the placed-by-hand default). */
    @Test
    void axisYVariantCarriesNoRotationKeys() throws Exception {
        JsonObject tVariant = generatedJson("assets/gt6/blockstates/oak_beam.json")
                .getAsJsonObject("variants").getAsJsonObject("axis=y");
        assertFalse(tVariant.has("x"), "axis=y must not carry an x rotation");
        assertFalse(tVariant.has("y"), "axis=y must not carry a y rotation");
    }

    /** The models: cube_column parent over the borrowed top/side pair per kind. */
    @Test
    void beamModelsPinTheCubeColumnFace() throws Exception {
        for (String tSnake : SNAKES) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tSnake + "_beam.json");
            assertEquals("minecraft:block/cube_column", tModel.get("parent").getAsString(),
                    tSnake + ": the cube_column parent");
            assertEquals("gt6:block/beam_top_" + tSnake, tModel.getAsJsonObject("textures").get("end").getAsString(),
                    tSnake + ": the end grain = the borrowed BEAM_TOP art");
            assertEquals("gt6:block/beam_side_" + tSnake, tModel.getAsJsonObject("textures").get("side").getAsString(),
                    tSnake + ": the side = the borrowed BEAM_SIDE art");
        }
    }

    /** The item forms parent the block models (the axle band row). */
    @Test
    void beamItemModelsParentTheBlockModel() throws Exception {
        for (String tSnake : SNAKES) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tSnake + "_beam.json");
            assertEquals("gt6:block/" + tSnake + "_beam", tModel.get("parent").getAsString(),
                    tSnake + ": the item parents the block model");
        }
    }

    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    private static String sha256(Path aFile) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(aFile)));
    }

    /** The 16 borrowed PNGs stay the documented ledger bytes (the assets README beam section). */
    @Test
    void borrowedTexturesAreTheDocumentedLedgerBytes() throws Exception {
        Path tDir = mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6", "textures", "block"));
        int tIndex = 0;
        for (String tFace : List.of("side", "top")) {
            for (String tSnake : List.of("acacia", "birch", "dark_oak", "jungle", "oak", "rubber_wood", "spruce", "wood")) {
                Path tFile = tDir.resolve("beam_" + tFace + "_" + tSnake + ".png");
                assertTrue(Files.isRegularFile(tFile), "the borrowed PNG exists: " + tFile.getFileName());
                assertEquals(TEXTURE_DIGESTS.get(tIndex++), sha256(tFile),
                        tFile.getFileName() + ": the README-ledger borrow bytes");
            }
        }
        assertEquals(16, tIndex, "16 PNGs = 8 kinds x top+side");
    }
}
