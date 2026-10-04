/**
 * Tests for task beam-blocks-register + beam-fireproof-closeout: the wood-beam block
 * universe — the census/audit unit in the GT6TreeBlocksCensusTest posture (offline-safe
 * by construction: enum walks, string paths, classpath JSON/PNG reads; no RegisterEvent,
 * no bootstrapped registries).
 *
 * <p>Upstream anchors: the FULL LIST_BEAMS block set — Beam1 meta 0-3
 * (LoaderWoodDictionary.java:51-54 Oak/Spruce/Birch/Jungle), Beam2 meta 0-1 (:55-56
 * Acacia/DarkOak) + the IL.Beam default face (:66 "Wood Beam") + the Rubber Wood face
 * (:175), and (task beam-fireproof-closeout) the residual families Beam3 meta 0-3
 * (Loader_Woods.java:60 the Greatwood/Silverwood/Skyroot/Darkwood block,
 * BlockTreeBeam3.java:31-48) + BeamA meta 0-3 (:48, the GT-tree 0-3 faces,
 * BlockTreeBeamA.java:31-48) + BeamB meta 0-3 (:50, the GT-tree 4-7 faces,
 * BlockTreeBeamB.java:31-48) + BeamC meta 0 (:52, the GT-tree 8 face, the single-meta
 * family BlockTreeBeamC.java:35 max-meta 1); the display rows BlockTreeBeam1.java:31-48
 * / BlockTreeBeam2.java:31-48; the 1.7.10 zh dump faces (tmp/gregtech.lang
 * gt.block.beam.1.0-.3 / 2.0-.3 / 3.0-.3 / a.0-.3 / b.0-.3 / c.0); the geometry
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

    /** The 21 kinds, upstream order (Beam1 0-3, Beam2 0-3, Beam3 0-3, BeamA 0-3, BeamB 0-3, BeamC 0 — Loader_Woods.java:48-61). */
    private static final List<String> SNAKES = List.of("oak", "spruce", "birch", "jungle",
            "acacia", "dark_oak", "rubber_wood", "wood",
            // task beam-fireproof-closeout — the residual families (Loader_Woods.java:60-61 Beam3, :48-53 BeamA/B/C)
            "greatwood", "silverwood", "skyroot", "darkwood", // Beam3: the mod-wood faces (BlockTreeBeam3.java:31-48)
            "rubber", "maple", "willow", "blue_mahoe", // BeamA: the GT-tree 0-3 faces (BlockTreeBeamA.java:31-48)
            "hazel", "cinnamon", "coconut", "rainbowood", // BeamB: the GT-tree 4-7 faces (BlockTreeBeamB.java:31-48)
            "blue_spruce"); // BeamC: the GT-tree 8 face, the single-meta family (BlockTreeBeamC.java:35-40)

    /** rotationX/rotationY per axis variant, ordered {axis=x, axis=y, axis=z} (0 = omitted key) — the vanilla axisBlock map. */
    private static final int[] X_ROT = {90, 0, 90};
    private static final int[] Y_ROT = {90, 0, 180};

    /** The 42 borrowed-PNG digests, side then top per kind, ALPHABETICAL snake order (the assets README beam section). */
    private static final List<String> TEXTURE_DIGESTS = List.of(
            // acacia / birch / dark_oak / jungle / oak / rubber_wood / spruce / wood — the beam-blocks-register 16
            "2fe75b7f799bc0c905c9c5ebbd0da7b9ff4d3e962f2cb0a9c041d800fcdaf8a0",
            "a5298f2af6ebd8a1116eb2ab457cc7431e45129b86fbffe17fe50510cb419829",
            "067570cc996ad9db3705aa8fb0a31a33abe25868cd3798a87d1e2a519ab6bef5",
            "aee8da2ef0dc3e1094d26ae49c6f07acc9d02243f7a6c1b50e8f9cb3d8c4967c",
            "cc2df784eccb6d32bb3243f317457aae0ae5039cbf56133ebca81c87e8cff008",
            "bbe055a3c023cd3241095396a791e837576476c992c2bba40cd55b7df53b02e5",
            "6f4ac051ead59da193ea625cf8211adbcfc7caa321b29c76943d3f9aeda14b87",
            "c5553ce6336fc5f17aaeb8665eca26d274c194dd8c68a7b3af5e674632b9feef",
            "c9ac39e363aa066e49c479ac2a9c9640232a1ead0b82d17a5f1a1e9014d3b467",
            "f0d9cc40fdcd94e7e46e296a92e3ebd121d7a471fe47708f603ce01b81125a9d",
            "183e6ab631723e7eeca90f70931ece7138bacf625cfe862058e6f127760bc836",
            "ac01916d990e06e5b1b1d8889f56ea6b87b72f39f9d469d6fbe05a64682ccceb",
            "0b6085ee35cd549b67a4a87ec1317c34afcb17c10d3dc6e935d697ae48d7f393",
            "109cf88ed3e8c9710e1f23ed19c7e60d3508a32ba42dd377ce6fbfe63bf5deb0",
            "a2e9094402a7cd7961baeaedff848a80ed972f452d9623ff690449f81c69860e",
            "32abe24f27715308c9f3f613f50c9c2437499e4943a87986a360a8472adb6072",
            // blue_mahoe / blue_spruce / cinnamon / coconut / darkwood / greatwood / hazel / maple
            // / rainbowood / rubber / silverwood / skyroot / willow — the beam-fireproof-closeout 26 (side then top each)
            "110a41ae8f5af400f66b9e5c4dbc55664c1e74767ece3b3154346e49889373a5",
            "ab2e8dcf880d9f0ca779923499be07f062722cbf0be3e9c1465a4e335dcbe418",
            "f74a0ff1cee6ae2f355e15493588bd885a788d70e92f57d3f50e7b4d9532bd65",
            "df4d3dfb0edd02fa35bc9c0caee5e08f95641f5c67387d0c9b693d2c89cf3523",
            "844a6c6d51e21de8ce85a542b91cf49ef4b0efff0327192a673012cec31b146b",
            "04a9fb7728ef8b5ee4ed8f287306db92f3b672b8f2b0f80f90ca3ac2387acb63",
            "5f51ca29b5f7ceef08ee1a041f3e48ff0827de5613cebfdfbde764e176ec1eb6",
            "1fdb9df130b18537010b29689976903bc2d72a964be66f3d39b56b6fbc3d74d1",
            "31d4b195c85005120c83d0d2844f6d284c83138f90797fe3fe46d227b1e4357e",
            "0b5683a7ada26e801c3795a464ef4c6387fdcb2a60c80bbe471e2068cf3569c4",
            "c23abe01f43e5b8d251ffa59e9c32ab710ca1443b82fb155b1fc3cbbe20bce5f",
            "56b8f023f1f98802867786fe9cb4fc23a1493097b1af61719f3eac23f7233a78",
            "2094a04f2b5287fec284e1a696efd72d911d550eb040d750f75f4af74e59fed0",
            "28bfea218176e32facc65820881952d3fc902a8eb09444005f8a9c7b03dc056f",
            "ed2aa38252d965b3043ba8f323c01884b47833c557677b33d6bb0813b7f842b7",
            "7bbc36e69096bfd9ef7ccc259c69c912e154a2eac63eca763d9a90aba3f1f685",
            "763fc6956b26c0d5f8dda05d8952e12810694449a0ec9d234fa7d5b222a0fa33",
            "2aaa8076abaac37fe1f9f8056b8de5938cc7a5b464b94a97e1c9ef03b955754b",
            "a27ce4309b6b4a466156af7772d8d6ca92183f3a41f2f501c6de5b2e33b301af",
            "330a5aad910068fbd7aeafe8bb54c4018bec7ad00a21ad6638e5cf87173a3a1a",
            "c727b99851c6431f2ffbedba98e5f6c2e4f19273781d8ba5868104d946e9b748",
            "245cf31f55c081ac012f3948c67904ffd8ce385d3d571b582b97ffd525c25312",
            "dc6876d991a3b6365c3b1a9a1e06a199ef05fa903487112fb2e1a651e4459728",
            "16626fddb57cb4310647ca79e57399f90ae9303645fe54c2356844e242b61f14",
            "9e64da2f487333ba753b3b669abd70f10b894b1c44ecde998a8e10373928439d",
            "769ac3953c79fe5a25dabbe0c71642525f5b1dbf9111083b7435e899f0f3d159");

    /** The kind for a snake, or null (the walk-match form lets the red pins fail loudly on a missing row). */
    private static GT6BeamKind bySnake(String aSnake) {
        for (GT6BeamKind tKind : GT6BeamKind.values()) {
            if (tKind.snake().equals(aSnake)) return tKind;
        }
        return null;
    }

    /** The census: 21 rows in the upstream LIST_BEAMS block order, snakes distinct. */
    @Test
    void kindsOrderIsTheUpstreamListBeamsOrder() {
        assertEquals(21, GT6BeamBlocks.KINDS.size(), "21 beams (Beam1x4 + Beam2x4 + Beam3x4 + BeamAx4 + BeamBx4 + BeamCx1, Loader_Woods.java:48-61)");
        for (int i = 0; i < SNAKES.size(); i++) {
            assertEquals(SNAKES.get(i), GT6BeamBlocks.KINDS.get(i).snake(),
                    "kind " + i + " must follow the upstream meta order");
        }
        assertEquals(21, GT6BeamBlocks.BLOCKS.size(), "21 block registrations");
        assertEquals(21, GT6BeamBlocks.ITEMS.size(), "21 block-item registrations");
        Set<String> tPaths = new HashSet<>();
        for (GT6BeamKind tKind : GT6BeamBlocks.KINDS) {
            assertTrue(tPaths.add(GT6BeamBlocks.path(tKind)), "path ids must be distinct: " + GT6BeamBlocks.path(tKind));
        }
        assertEquals(21, tPaths.size(), "21 distinct <snake>_beam ids");
    }

    /** The spot pins: the first/last/wood rows carry their registration ids. */
    @Test
    void spotPathsPinTheRegistryIds() {
        assertEquals("oak_beam", GT6BeamBlocks.path(GT6BeamKind.OAK), "Beam1 meta 0");
        assertEquals("dark_oak_beam", GT6BeamBlocks.path(GT6BeamKind.DARK_OAK), "Beam2 meta 1");
        assertEquals("rubber_wood_beam", GT6BeamBlocks.path(GT6BeamKind.RUBBER_WOOD), "Beam2 meta 2 (the :175 rubber face)");
        assertEquals("wood_beam", GT6BeamBlocks.path(GT6BeamKind.WOOD), "Beam2 meta 3 (the :66 IL.Beam default face)");
        // task beam-fireproof-closeout — the residual family spots
        assertEquals("greatwood_beam", GT6BeamBlocks.path(bySnake("greatwood")), "Beam3 meta 0 (Loader_Woods.java:60)");
        assertEquals("rubber_beam", GT6BeamBlocks.path(bySnake("rubber")), "BeamA meta 0 (:48, the GT-tree rubber face)");
        assertEquals("rainbowood_beam", GT6BeamBlocks.path(bySnake("rainbowood")), "BeamB meta 3 (:50)");
        assertEquals("blue_spruce_beam", GT6BeamBlocks.path(bySnake("blue_spruce")), "BeamC meta 0 (:52, the single-meta family)");
    }

    /** The en display names are the upstream LH rows verbatim (BlockTreeBeam1/2/3/A/B/C:31-48). */
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
        // task beam-fireproof-closeout — the residual families
        assertEquals("Greatwood Beam", bySnake("greatwood").enName(), "BlockTreeBeam3.java:31");
        assertEquals("Silverwood Beam", bySnake("silverwood").enName(), ":36");
        assertEquals("Skyroot Beam", bySnake("skyroot").enName(), ":41");
        assertEquals("Darkwood Beam", bySnake("darkwood").enName(), ":46");
        assertEquals("Rubber Beam", bySnake("rubber").enName(), "BlockTreeBeamA.java:31");
        assertEquals("Maple Beam", bySnake("maple").enName(), ":36");
        assertEquals("Willow Beam", bySnake("willow").enName(), ":41");
        assertEquals("Blue Mahoe Beam", bySnake("blue_mahoe").enName(), ":46");
        assertEquals("Hazel Beam", bySnake("hazel").enName(), "BlockTreeBeamB.java:31");
        assertEquals("Cinnamon Beam", bySnake("cinnamon").enName(), ":36");
        assertEquals("Coconut Beam", bySnake("coconut").enName(), ":41");
        assertEquals("Rainbowood Beam", bySnake("rainbowood").enName(), ":46");
        assertEquals("Blue Spruce Beam", bySnake("blue_spruce").enName(), "BlockTreeBeamC.java:37");
    }

    /** The zh display names are the 1.7.10 dump faces verbatim (gt.block.beam.1/2/3/a/b/c). */
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
        // task beam-fireproof-closeout — the residual families
        assertEquals("宏伟之木梁", bySnake("greatwood").zhName(), "dump gt.block.beam.3.0");
        assertEquals("银树梁", bySnake("silverwood").zhName(), "gt.block.beam.3.1");
        assertEquals("天根木梁", bySnake("skyroot").zhName(), "gt.block.beam.3.2");
        assertEquals("黑树梁", bySnake("darkwood").zhName(), "gt.block.beam.3.3");
        assertEquals("橡胶梁", bySnake("rubber").zhName(), "gt.block.beam.a.0");
        assertEquals("枫树梁", bySnake("maple").zhName(), "gt.block.beam.a.1");
        assertEquals("柳树梁", bySnake("willow").zhName(), "gt.block.beam.a.2");
        assertEquals("高红槿梁", bySnake("blue_mahoe").zhName(), "gt.block.beam.a.3");
        assertEquals("榛树梁", bySnake("hazel").zhName(), "gt.block.beam.b.0");
        assertEquals("肉桂梁", bySnake("cinnamon").zhName(), "gt.block.beam.b.1");
        assertEquals("椰子树木梁", bySnake("coconut").zhName(), "gt.block.beam.b.2");
        assertEquals("彩虹树梁", bySnake("rainbowood").zhName(), "gt.block.beam.b.3");
        assertEquals("北美云杉梁", bySnake("blue_spruce").zhName(), "gt.block.beam.c.0");
    }

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6BeamRegistryTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The 21 blockstates pin the vanilla axisBlock rotation map (the addAxles band form: X=x90/y90, Y=none, Z=x90/y180). */
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

    /** The 42 borrowed PNGs stay the documented ledger bytes (the assets README beam section). */
    @Test
    void borrowedTexturesAreTheDocumentedLedgerBytes() throws Exception {
        Path tDir = mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6", "textures", "block"));
        List<String> tAlpha = SNAKES.stream().sorted().toList();
        int tIndex = 0;
        for (String tFace : List.of("side", "top")) {
            for (String tSnake : tAlpha) {
                Path tFile = tDir.resolve("beam_" + tFace + "_" + tSnake + ".png");
                assertTrue(Files.isRegularFile(tFile), "the borrowed PNG exists: " + tFile.getFileName());
                assertEquals(TEXTURE_DIGESTS.get(tIndex++), sha256(tFile),
                        tFile.getFileName() + ": the README-ledger borrow bytes");
            }
        }
        assertEquals(42, tIndex, "42 PNGs = 21 kinds x top+side");
    }
}
