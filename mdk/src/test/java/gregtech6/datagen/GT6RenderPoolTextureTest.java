/*
 * Offline pinned tests for task p36-render-texture-bake — the render pool stand-in
 * retirement (the three self-declared stand-ins of GT6BlockStates.java, the
 * p35-energy-tail wave): the generated JSON face and the borrowed-PNG ledger face,
 * both asserted against the committed trees (the GT6MachinePaintRenderDatagenTest
 * classpath split + the GT6TextureCensusTest pin-e digest form).
 *
 * <p>NARROWED by task r8-tex-composite-family: the LD-transformer and crystal-charger
 * segments of the wave moved to the true two-layer borrows — their pins live in
 * {@link GT6CompositeEnergyTexDatagenTest} now (the 13 src-over composites this class
 * used to pin are retired). What stays here is the LD WIRE domain: the 16 wire metas
 * (out of the composite card's scope) and their 5 tier sprites + the ledger pin.</p>
 *
 * <p>Acceptance pins:</p>
 * <ul>
 *   <li>the 16 LD wire metas are FULLY covered: each meta's blockstate/model/item
 *       chain exists and binds its TIER sprite — the five distinct LONG_DIST_WIRES_01
 *       iconset art (upstream Textures.java:638-655: metas 0-1=EV, 2=IV, 3-7=LuV,
 *       8-11=ZPM, 12-15=UV; the same split as the tier-byte table Loader_Blocks.java:160,
 *       transcribed into GT6LongDistWires.ROWS) — the wire_electric stand-in is gone;</li>
 *   <li>EVERY wire wave PNG is ledgered in assets/README.md with its real sha256
 *       (the name check alone can pass while the bytes drifted — the
 *       GT6TextureCensusTest pin-e lesson).</li>
 * </ul>
 *
 * <p>KJS face: none — this card's output is resources + datagen only.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6RenderPoolTextureTest {

    /** The meta -> LONG_DIST_WIRES_01 sprite token pin (Textures.java:638-655 over Loader_Blocks.java:160). */
    private static final Map<Integer, String> WIRE_ART = Map.ofEntries(
            Map.entry(0, "ev"), Map.entry(1, "ev"), Map.entry(2, "iv"),
            Map.entry(3, "luv"), Map.entry(4, "luv"), Map.entry(5, "luv"), Map.entry(6, "luv"), Map.entry(7, "luv"),
            Map.entry(8, "zpm"), Map.entry(9, "zpm"), Map.entry(10, "zpm"), Map.entry(11, "zpm"),
            Map.entry(12, "uv"), Map.entry(13, "uv"), Map.entry(14, "uv"), Map.entry(15, "uv"));

    /** The wire wave's 5 PNGs (the p36 bake products still on the tree — the composite 13 retired r8-tex-composite-family). */
    private static final List<String> WAVE_PNGS = List.of(
            "long_dist_wire_ev.png", "long_dist_wire_iv.png", "long_dist_wire_luv.png",
            "long_dist_wire_zpm.png", "long_dist_wire_uv.png");

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6RenderPoolTextureTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertTextureOnTree(String aTextureRef) {
        assertTrue(aTextureRef.startsWith("gt6:block/"), "block-model texture refs are gt6:block/: " + aTextureRef);
        String rel = "assets/gt6/textures/block/" + aTextureRef.substring("gt6:block/".length()) + ".png";
        try (InputStream tStream = GT6RenderPoolTextureTest.class.getClassLoader().getResourceAsStream(rel)) {
            assertNotNull(tStream, "the wave texture must exist on the static tree: " + rel);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** One LD wire meta: blockstate -> model -> tier sprite -> PNG, plus the item's 2D icon form. */
    private static void assertWireMetaCovered(int aMeta) throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/long_dist_wire_" + aMeta + ".json");
        JsonElement tVariant = tState.getAsJsonObject("variants").get("");
        assertNotNull(tVariant, "the wire blockstate keeps the property-less wildcard variant: meta " + aMeta);
        String tModelRef = tVariant.getAsJsonObject().get("model").getAsString();
        assertEquals("gt6:block/long_dist_wire_" + aMeta, tModelRef, "meta " + aMeta + " blockstate model ref");

        JsonObject tModel = json("assets/gt6/models/block/long_dist_wire_" + aMeta + ".json");
        assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString(), "meta " + aMeta + " cube-all parent");
        String tTexture = tModel.getAsJsonObject("textures").get("all").getAsString();
        assertEquals("gt6:block/long_dist_wire_" + WIRE_ART.get(aMeta),
                tTexture, "meta " + aMeta + " binds its TIER sprite (the LONG_DIST_WIRES_01 table)");
        assertTextureOnTree(tTexture);

        JsonObject tItem = json("assets/gt6/models/item/long_dist_wire_" + aMeta + ".json");
        // r8-tex-itemform-a band A: the BlockItem left the cubeAll-parent anti-pattern for
        // the 2D item/generated icon over the same tier sprite (the world face is untouched).
        assertEquals("minecraft:item/generated", tItem.get("parent").getAsString(),
                "meta " + aMeta + " item rides the 2D icon form");
        assertEquals("gt6:block/long_dist_wire_" + WIRE_ART.get(aMeta),
                tItem.getAsJsonObject("textures").get("layer0").getAsString(),
                "meta " + aMeta + " item layer0 binds its TIER sprite (the LONG_DIST_WIRES_01 table)");
    }

    @Test
    void ldWireSixteenMetasFullyCovered() throws Exception {
        for (int tMeta = 0; tMeta < 16; tMeta++) {
            assertWireMetaCovered(tMeta);
        }
        assertEquals(16, WIRE_ART.size(), "the tier-art table covers every meta");
    }

    /**
     * The wave ledger pin (the GT6TextureCensusTest pin-e form): every wave PNG exists
     * on the static tree AND its real bytes hash to a sha256 recorded in the README
     * section (the digest check travels across machines because the colored casing layer
     * is one shared grayscale — a wrong-layer composite would still be a 16x16 PNG).
     */
    @Test
    void waveTexturesCarryFullSha256Attribution() throws Exception {
        Path tMdkRoot = mdkRoot();
        String tReadme = Files.readString(tMdkRoot.resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
        MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
        List<String> tViolations = new java.util.ArrayList<>();
        for (String tName : WAVE_PNGS) {
            Path tPng = tMdkRoot.resolve("src/main/resources/assets/gt6/textures/block").resolve(tName);
            assertTrue(Files.isRegularFile(tPng), "the wave PNG must exist on the static tree: " + tName);
            String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
            if (!tReadme.contains(tName)) {
                tViolations.add(tName + " — filename absent from assets/README.md");
            }
            if (!tReadme.contains(tHex)) {
                tViolations.add(tName + " — file bytes hash to " + tHex + ", not grounded in assets/README.md");
            }
        }
        assertTrue(tViolations.isEmpty(), String.join("\n", tViolations));
        assertTrue(tReadme.contains("p36-energy-zpm-dechargers binds `block/zpm_decharger"),
            "the consumer-card surface declaration (the battery card binds these paths on its rebase)");
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
