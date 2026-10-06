/*
 * Offline pinned-census tests for task tex-pipe-textures (+ the pipe-render-closeout
 * SET-derivation upgrade): the three pipe connector
 * families (280 fluid rows / the item pipe matrix — 126 rows since item-pipe-matrix /
 * the logistics wire) leave the
 * cube_all single-placeholder era for the upstream material-set DUAL-LAYER tinted form
 * — every material-icon render is two passes (pass 0 = the set art multiplied by mRGBa,
 * pass 1 = the untinted <SET>_OVERLAY black outline, TextureSet.java:145-181), the side
 * segment = INDEX_BLOCK_PIPE_SIDE 'pipeSide' (GT_API.java:158 addToAll), the logistics
 * wire its own dedicated iconsets pair (MultiTileEntityWireLogistics.java:48-49), and
 * the six restrictive item rows add the third upstream pass — the PIPE_RESTRICTOR plate
 * (MultiTileEntityPipeItem.java:280-281, the NBT_PIPERENDER 1 registration rows :76-82).
 *
 * <p>Coverage (asserted against the committed generated + static trees):
 * <ul>
 * <li>the shared models keep the two-layer shape — tintindex-0 body over the set
 *     art, UNTINTED overlay bands (no tintindex key — FaceBuilder default -1), the
 *     cutout render_type, the restrictive twin stacking the restrictor band;</li>
 * <li>the per-row art pick is the ZERO-PARALLEL-TABLE SET derivation
 *     ({@code GTWireTextures.pipeSideSprite}, task pipe-render-closeout): the 61 pipe
 *     materials' SET census is pinned slug by slug, and the art dispatch collapses to
 *     wood / rubber / the shared copper copy by the upstream byte-identity census
 *     (pipeSide.png hashes one art everywhere except WOOD and RUBBER); the connected
 *     arms ride the per-diameter arts over the upstream :265 diameter selector
 *     ({@code GTWireTextures.pipeArmSprite});</li>
 * <li>the visual census: the three live side arts are NOT flat plates (distinct-color
 *     counts pinned at the sha-anchored bytes: wood 59 / rubber 148 / copper 42) and
 *     every borrowed PNG carries its sha256 README ledger row;</li>
 * <li>all 407 pipe blockstates (280 fluid + 126 item + the logistics wire) map every
 *     CONNECTIONS variant (0..63) onto the right shared model, and the BlockItem models
 *     parent those shared models;</li>
 * <li>the retired placeholders are DEAD — no generated or static JSON references
 *     fluid_pipe_wood / item_pipe_restrictive / the block-root logistics_wire art any
 *     more, and the three PNGs are gone from the static tree (item_pipe.png STAYS: the
 *     16 LD wire placeholder rows still consume it, a separate card's domain);</li>
 * <li>the borrowed pipe PNGs exist on the static tree (byte identity is the README
 *     sha256 ledger's face — the upstream snapshot is absent from coder worktrees).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6PipeTextureDatagenTest {

    /** The shared models (the tintedPipeModel outputs, texture paths as model names). */
    private static final String WOOD_MODEL = "materialicons/wood/pipe_side";
    private static final String RUBBER_MODEL = "materialicons/rubber/pipe_side";
    private static final String COPPER_MODEL = "materialicons/copper/pipe_side";
    private static final String COPPER_RESTRICTIVE_MODEL = "materialicons/copper/pipe_side_restrictive";
    private static final String LOGISTICS_MODEL = "iconsets/logistics_wire";

    /**
     * The pipe blockstate registry paths (task item-pipe-matrix + fluid-pipe-matrix),
     * walking the SAME seam the datagen consumes — the per-material SET art dispatch
     * ({@code GTWireTextures.pipeSideSprite}, task pipe-render-closeout): wood-set rows
     * the wood art, the rubber row the rubber art, everything else the shared copper
     * copy (the upstream pipeSide byte-identity census, assets/README.md).
     */
    private static Map<String, String> fluidModelPicks() {
        Map<String, String> rPicks = new LinkedHashMap<>();
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            rPicks.put(tRow.path(), artModelOf(tRow.material().oreDictMaterial()));
        }
        return rPicks;
    }

    /** The model path of one material's pipe art (the dispatch under test, mirrored). */
    private static String artModelOf(gregapi.oredict.OreDictMaterial aMaterial) {
        return "materialicons/" + gregtech6.client.wire.GTWireTextures.pipeArtSetOf(aMaterial) + "/pipe_side";
    }

    private static final List<String> RESTRICTIVE_TAILS = List.of("restrictive_medium", "restrictive_large", "restrictive_huge");

    /**
     * The item pipe slugs (the live registry walk) — LAZY accessor per the
     * GTOfflineTestBase class-init rule: a static-final init here class-inits
     * {@code GTItemPipes} (ForgeRegistries → BuiltInRegistries) at class-load, BEFORE the
     * {@code @BeforeAll} bootstrap — when this class executes first in the shared test JVM
     * (the run-order lottery), that poisons every later registry consumer with
     * NoClassDefFoundError. Every call site runs after the boot, inside test methods.
     */
    private static List<String> itemMaterials() {
        return gregtech6.registry.GTItemPipes.MATERIALS.stream()
                .map(gregtech6.registry.GTItemPipes.ItemPipeMaterial::slug).toList();
    }

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the converter census shape — the tint
        // pins class-init the MT statics)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        gregtech6.registry.GTMaterialItems.initMaterials();
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

    /** The file off the generated ∪ static trees (the census shape). */
    private static Path treeFile(String aRelPath) {
        for (String tTree : new String[] {"src/generated/resources", "src/main/resources"}) {
            Path tPath = mdkRoot().resolve(tTree).resolve(aRelPath);
            if (Files.isRegularFile(tPath)) return tPath;
        }
        return null;
    }

    private static JsonObject treeJson(String aRelPath) throws IOException {
        Path tPath = treeFile(aRelPath);
        assertNotNull(tPath, "not on the generated ∪ static trees: " + aRelPath);
        return JsonParser.parseString(Files.readString(tPath, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String modelRelPath(String aModel) {
        return "assets/gt6/models/block/" + aModel + ".json";
    }

    // ------------------------------------------------------------------
    // the two-layer model shape (the wire grammar + the decal bands)
    // ------------------------------------------------------------------

    @Test
    public void sharedModelsKeepTheTwoLayerShape() throws Exception {
        for (String tModel : List.of(WOOD_MODEL, RUBBER_MODEL, COPPER_MODEL, COPPER_RESTRICTIVE_MODEL, LOGISTICS_MODEL)) {
            JsonObject tJson = treeJson(modelRelPath(tModel));
            assertEquals("minecraft:cutout", tJson.get("render_type").getAsString(),
                    tModel + " must declare the alpha-discarding chunk layer (the #8 fix shape)");
            var tElements = tJson.getAsJsonArray("elements");
            assertTrue(tElements.size() >= 2, tModel + " lost the decal band");
            // element 0 = the tintindex-0 body cube over #all
            JsonObject tBody = tElements.get(0).getAsJsonObject();
            for (var tEntry : tBody.getAsJsonObject("faces").entrySet()) {
                JsonElement tTint = tEntry.getValue().getAsJsonObject().get("tintindex");
                assertNotNull(tTint, tModel + " body face " + tEntry.getKey() + " lost tintindex 0");
                assertEquals(0, tTint.getAsInt());
            }
            // the decal bands carry NO tintindex (the UNCOLOURED second layer)
            for (int i = 1; i < tElements.size(); i++) {
                for (var tEntry : tElements.get(i).getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    assertTrue(tEntry.getValue().getAsJsonObject().get("tintindex") == null,
                            tModel + " decal face " + tEntry.getKey() + " carries a tintindex — the paint chain would tint it");
                }
            }
        }
    }

    @Test
    public void restrictiveTwinStacksTheRestrictorBand() throws Exception {
        JsonObject tJson = treeJson(modelRelPath(COPPER_RESTRICTIVE_MODEL));
        var tTextures = tJson.getAsJsonObject("textures");
        // the base + set overlay identical to the plain twin, the third pass = the restrictor
        assertEquals("gt6:block/materialicons/copper/pipe_side", tTextures.get("all").getAsString());
        assertEquals("gt6:block/materialicons/copper/pipe_side_overlay", tTextures.get("overlay0").getAsString());
        assertEquals("gt6:block/iconsets/pipe_restrictor", tTextures.get("overlay1").getAsString());
        // 1 body + 2 decal bands x 6 faces
        assertEquals(13, tJson.getAsJsonArray("elements").size());
    }

    // ------------------------------------------------------------------
    // the per-row set picks
    // ------------------------------------------------------------------

    @Test
    public void texturePicksRideTheRowMaterialSets() throws Exception {
        assertEquals("gt6:block/materialicons/wood/pipe_side",
                treeJson(modelRelPath(WOOD_MODEL)).getAsJsonObject("textures").get("all").getAsString(),
                "the wood rows ride the WOOD art (the MT wood factory = SET_WOOD)");
        assertEquals("gt6:block/materialicons/rubber/pipe_side",
                treeJson(modelRelPath(RUBBER_MODEL)).getAsJsonObject("textures").get("all").getAsString(),
                "the rubber row rides the RUBBER art (SET_RUBBER — the only byte-distinct pipeSide besides wood)");
        assertEquals("gt6:block/materialicons/copper/pipe_side",
                treeJson(modelRelPath(COPPER_MODEL)).getAsJsonObject("textures").get("all").getAsString(),
                "every other SET rides the shared copper copy (the byte-identity census)");
        // the overlay band: one shared id on every pipe model (byte-identical upstream)
        for (String tModel : List.of(WOOD_MODEL, RUBBER_MODEL, COPPER_MODEL, COPPER_RESTRICTIVE_MODEL)) {
            assertEquals("gt6:block/materialicons/copper/pipe_side_overlay",
                    treeJson(modelRelPath(tModel)).getAsJsonObject("textures").get("overlay0").getAsString(),
                    tModel + " rides the shared overlay band");
        }
        assertEquals("gt6:block/iconsets/logistics_wire",
                treeJson(modelRelPath(LOGISTICS_MODEL)).getAsJsonObject("textures").get("all").getAsString(),
                "the logistics wire rides its dedicated iconsets pair, NOT the pipeSide set art");
        assertEquals("gt6:block/iconsets/logistics_wire_overlay",
                treeJson(modelRelPath(LOGISTICS_MODEL)).getAsJsonObject("textures").get("overlay0").getAsString());
    }

    @Test
    public void blockstatesMapEveryConnectionsVariantOntoTheSharedModels() throws Exception {
        record Expect(List<String> paths, String model) {}
        List<String> tItemPaths = new ArrayList<>();
        List<String> tRestrictivePaths = new ArrayList<>();
        for (String tMat : itemMaterials()) {
            for (String tTail : RESTRICTIVE_TAILS) tRestrictivePaths.add(tMat + "_item_pipe_" + tTail);
            tItemPaths.add(tMat + "_item_pipe_medium");
            tItemPaths.add(tMat + "_item_pipe_large");
            tItemPaths.add(tMat + "_item_pipe_huge");
        }
        for (Expect tExpect : List.of(
                new Expect(tItemPaths, "gt6:block/" + COPPER_MODEL),
                new Expect(tRestrictivePaths, "gt6:block/" + COPPER_RESTRICTIVE_MODEL),
                new Expect(List.of("logistics_wire"), "gt6:block/" + LOGISTICS_MODEL))) {
            for (String tPath : tExpect.paths()) {
                JsonObject tJson = treeJson("assets/gt6/blockstates/" + tPath + ".json");
                var tVariants = tJson.getAsJsonObject("variants");
                assertEquals(64, tVariants.size(), tPath + " lost the 64 CONNECTIONS variants");
                for (var tEntry : tVariants.entrySet()) {
                    // the variant value is a bare model object (no rotation on the pipe form)
                    JsonObject tVariant = tEntry.getValue().getAsJsonObject().get("model") != null
                            ? tEntry.getValue().getAsJsonObject()
                            : tEntry.getValue().getAsJsonArray().get(0).getAsJsonObject();
                    assertEquals(tExpect.model(), tVariant.get("model").getAsString(),
                            tPath + " variant " + tEntry.getKey() + " drifted off the shared model");
                }
            }
        }
        // the fluid matrix: per-row family picks over the 64-variant shape (the datagen
        // loop mirrors fluidModelPicks — the zero-drift walk face)
        for (Map.Entry<String, String> tFluid : fluidModelPicks().entrySet()) {
            JsonObject tJson = treeJson("assets/gt6/blockstates/" + tFluid.getKey() + ".json");
            var tVariants = tJson.getAsJsonObject("variants");
            assertEquals(64, tVariants.size(), tFluid.getKey() + " lost the 64 CONNECTIONS variants");
            for (var tEntry : tVariants.entrySet()) {
                JsonObject tVariant = tEntry.getValue().getAsJsonObject().get("model") != null
                        ? tEntry.getValue().getAsJsonObject()
                        : tEntry.getValue().getAsJsonArray().get(0).getAsJsonObject();
                assertEquals("gt6:block/" + tFluid.getValue(), tVariant.get("model").getAsString(),
                        tFluid.getKey() + " variant " + tEntry.getKey() + " drifted off the family pick");
            }
        }
    }

    @Test
    public void itemModelsParentTheSharedModels() throws Exception {
        for (Map.Entry<String, String> tFluid : fluidModelPicks().entrySet()) {
            assertEquals("gt6:block/" + tFluid.getValue(),
                    treeJson("assets/gt6/models/item/" + tFluid.getKey() + ".json").get("parent").getAsString());
        }
        assertEquals("gt6:block/" + LOGISTICS_MODEL,
                treeJson("assets/gt6/models/item/logistics_wire.json").get("parent").getAsString());
        for (String tMat : itemMaterials()) {
            for (String tTail : RESTRICTIVE_TAILS) {
                assertEquals("gt6:block/" + COPPER_RESTRICTIVE_MODEL,
                        treeJson("assets/gt6/models/item/" + tMat + "_item_pipe_" + tTail + ".json").get("parent").getAsString());
            }
            for (String tTail : new String[] {"medium", "large", "huge"}) {
                assertEquals("gt6:block/" + COPPER_MODEL,
                        treeJson("assets/gt6/models/item/" + tMat + "_item_pipe_" + tTail + ".json").get("parent").getAsString());
            }
        }
    }

    // ------------------------------------------------------------------
    // the SET census (task pipe-render-closeout) — 61 slugs pinned + the art collapse
    // ------------------------------------------------------------------

    /** The slug → block-SET census over the whole pipe material axis (61 rows), probed
     * live via the ported gregapi ({@code mTextureSetsBlock} first entry — the upstream
     * MT.java factory/SET_ seeding) and pinned here. */
    private static final Map<String, String> SET_CENSUS = Map.ofEntries(
            // the 40 fluid loader lines (Loader_MultiTileEntities :1846-1885)
            Map.entry("wood", "wood"), Map.entry("wood_treated", "wood"), Map.entry("iron_wood", "wood"),
            Map.entry("plastic", "dull"), Map.entry("rubber", "rubber"), Map.entry("copper", "copper"),
            Map.entry("gold", "shiny"), Map.entry("aluminium", "copper"), Map.entry("tin_alloy", "copper"),
            Map.entry("bronze", "copper"), Map.entry("invar", "metallic"), Map.entry("steel", "metallic"),
            Map.entry("desh", "dull"), Map.entry("chromium", "shiny"), Map.entry("hsla", "metallic"),
            Map.entry("efrine", "metallic"), Map.entry("galvanized_steel", "copper"),
            Map.entry("stainless_steel", "shiny"), Map.entry("tungsten_alloy", "metallic"),
            Map.entry("titanium", "metallic"), Map.entry("netherite", "metallic"),
            Map.entry("workers_alloy", "metallic"), Map.entry("tungsten", "metallic"),
            Map.entry("palladium", "shiny"), Map.entry("vanadium_steel", "metallic"),
            Map.entry("tungstensteel", "metallic"), Map.entry("tungsten_carbide", "metallic"),
            Map.entry("iridium", "dull"), Map.entry("gaia_spirit", "shiny"), Map.entry("draconium", "metallic"),
            Map.entry("awakened_draconium", "metallic"), Map.entry("infinity", "shiny"),
            Map.entry("adamantium", "shiny"), Map.entry("bedrock_hsla_alloy", "brick"),
            Map.entry("thaumium", "metallic"), Map.entry("manasteel", "shiny"), Map.entry("void_metal", "metallic"),
            Map.entry("terrasteel", "shiny"), Map.entry("carbon", "fine"),
            Map.entry("tantalum_hafnium_carbide", "metallic"),
            // the 21 item loader lines (Loader_MultiTileEntities :1823-1843)
            Map.entry("brass", "copper"), Map.entry("constantan", "copper"), Map.entry("cobalt_brass", "copper"),
            Map.entry("germanium", "copper"), Map.entry("arsenic_copper", "copper"),
            Map.entry("arsenic_bronze", "copper"), Map.entry("electrum", "shiny"),
            Map.entry("sterling_silver", "shiny"), Map.entry("rose_gold", "shiny"), Map.entry("angmallen", "shiny"),
            Map.entry("black_bronze", "copper"), Map.entry("aluminium_brass", "copper"),
            Map.entry("manyullyn", "copper"), Map.entry("magnalium", "dull"), Map.entry("platinum", "shiny"),
            Map.entry("osmium", "metallic"), Map.entry("enderium", "copper"), Map.entry("ultimet", "shiny"),
            Map.entry("elementium", "shiny"), Map.entry("osmiridium", "metallic"),
            Map.entry("vibranium_silver", "shiny"));

    @Test
    public void pipeSetCensusPinnedOverAll61Materials() {
        // the census table itself covers the whole axis, no slug missing
        java.util.Set<String> tLive = new java.util.HashSet<>();
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            if (tRow.variant() != gregtech6.registry.GTFluidPipes.FluidPipeVariant.TINY) continue;
            tLive.add(tRow.material().slug());
        }
        for (gregtech6.registry.GTItemPipes.ItemPipeRow tRow : gregtech6.registry.GTItemPipes.ROWS) {
            if (tRow.variant() != gregtech6.registry.GTItemPipes.ItemPipeVariant.MEDIUM) continue;
            tLive.add(tRow.material().slug());
        }
        assertEquals(SET_CENSUS.keySet(), tLive, "the SET census table = the live pipe material axis");
        // every row's material resolves to its pinned SET through the zero-parallel-table seam
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            if (tRow.variant() != gregtech6.registry.GTFluidPipes.FluidPipeVariant.TINY) continue;
            assertEquals(SET_CENSUS.get(tRow.material().slug()),
                    gregtech6.client.wire.GTWireTextures.blockSetOf(tRow.material().oreDictMaterial()),
                    "fluid slug " + tRow.material().slug() + " drifted off the pinned SET");
        }
        for (gregtech6.registry.GTItemPipes.ItemPipeRow tRow : gregtech6.registry.GTItemPipes.ROWS) {
            if (tRow.variant() != gregtech6.registry.GTItemPipes.ItemPipeVariant.MEDIUM) continue;
            assertEquals(SET_CENSUS.get(tRow.material().slug()),
                    gregtech6.client.wire.GTWireTextures.blockSetOf(tRow.material().oreDictMaterial()),
                    "item slug " + tRow.material().slug() + " drifted off the pinned SET");
        }
    }

    @Test
    public void pipeArtDispatchCollapsesByTheByteIdentityCensus() {
        // only wood/rubber SETs carry distinct pipeSide art; every other SET maps to the
        // shared copper copy — the sprite id IS the dispatch face
        assertEquals("gt6:block/materialicons/wood/pipe_side",
                gregtech6.client.wire.GTWireTextures.pipeSideSprite(gregtech6.registry.GTFluidPipes.MAT_WOOD.oreDictMaterial()).toString());
        assertEquals("gt6:block/materialicons/rubber/pipe_side",
                gregtech6.client.wire.GTWireTextures.pipeSideSprite(gregtech6.registry.GTFluidPipes.MAT_RUBBER.oreDictMaterial()).toString());
        for (String tSlug : new String[] {"gold", "steel", "plastic", "iridium", "magnalium",
                "bedrock_hsla_alloy", "carbon", "brass", "electrum", "osmium"}) {
            assertEquals("gt6:block/materialicons/copper/pipe_side",
                    gregtech6.client.wire.GTWireTextures.pipeSideSprite(materialBySlug(tSlug)).toString(),
                    tSlug + " (SET " + SET_CENSUS.get(tSlug) + ") must ride the shared copper copy");
        }
        assertEquals("gt6:block/materialicons/copper/pipe_side_overlay",
                gregtech6.client.wire.GTWireTextures.PIPE_SIDE_OVERLAY_SPRITE.toString(),
                "one shared overlay band id (byte-identical across every set)");
    }

    /** The census slug → the live row material (the registry walk, both families). */
    private static gregapi.oredict.OreDictMaterial materialBySlug(String aSlug) {
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            if (tRow.material().slug().equals(aSlug)) return tRow.material().oreDictMaterial();
        }
        for (gregtech6.registry.GTItemPipes.ItemPipeRow tRow : gregtech6.registry.GTItemPipes.ROWS) {
            if (tRow.material().slug().equals(aSlug)) return tRow.material().oreDictMaterial();
        }
        throw new AssertionError("no live pipe material for slug " + aSlug);
    }

    /**
     * The visual census (task pipe-render-closeout): the three live side arts are NOT
     * flat plates — the distinct-color counts pinned at the sha-anchored bytes (wood 59
     * / rubber 148 / copper 42, 16x16 RGBA opaque) — and every borrowed pipe PNG carries
     * its sha256 README ledger row. The overlay band is byte-faithfully EMPTY upstream
     * (all 256 px alpha 0), so its face is the ledger row alone, not a color count.
     */
    @Test
    public void pipeArtVisualCensusNotFlatPlates() throws Exception {
        Map<String, Integer> tExpected = Map.of(WOOD_MODEL, 59, RUBBER_MODEL, 148, COPPER_MODEL, 42);
        String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"),
                StandardCharsets.UTF_8);
        for (Map.Entry<String, Integer> tArt : tExpected.entrySet()) {
            Path tPath = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block").resolve(tArt.getKey() + ".png");
            byte[] tBytes = Files.readAllBytes(tPath);
            assertEquals(tArt.getValue().intValue(), distinctColors(tBytes),
                    tArt.getKey() + " drifted off the pinned visual census");
            String tSha = sha256(tBytes);
            assertTrue(tReadme.contains("`" + tSha + "`"), tArt.getKey() + " must carry its sha256 ledger row");
        }
        for (String tOverlay : new String[] {WOOD_MODEL, COPPER_MODEL}) {
            Path tPath = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block")
                    .resolve(tOverlay + "_overlay.png");
            byte[] tBytes = Files.readAllBytes(tPath);
            assertTrue(tReadme.contains("`" + sha256(tBytes) + "`"),
                    tOverlay + "_overlay must carry its sha256 ledger row");
        }
    }

    /** sha256 hex of the bytes (the kitchen census form). */
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

    // ------------------------------------------------------------------
    // the placeholder retirement
    // ------------------------------------------------------------------

    @Test
    public void retiredPlaceholdersAreDead() throws Exception {
        // the TEXTURE references are dead on both trees (the token is the texture path
        // fragment, not the registry path — the item models legitimately keep the
        // *_item_pipe_restrictive_* registry names)
        List<String> tDeadTextures = List.of("block/fluid_pipe_wood", "block/item_pipe_restrictive");
        for (String tTree : new String[] {"src/generated/resources", "src/main/resources"}) {
            List<String> tOffenders = new ArrayList<>();
            try (Stream<Path> tWalk = Files.walk(mdkRoot().resolve(tTree))) {
                for (Path p : tWalk.filter(f -> f.toString().endsWith(".json")).toList()) {
                    String tContent = Files.readString(p, StandardCharsets.UTF_8);
                    for (String tDead : tDeadTextures) {
                        if (tContent.contains(tDead)) tOffenders.add(p + " -> " + tDead);
                    }
                }
            }
            assertTrue(tOffenders.isEmpty(), tTree + " still references a retired placeholder texture: " + tOffenders);
        }
        // the retired PNGs are gone; item_pipe.png STAYS — the 16 LD wire placeholder
        // rows still consume it (that family's card owns its retirement)
        assertFalse(Files.exists(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/fluid_pipe_wood.png")));
        assertFalse(Files.exists(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/item_pipe_restrictive.png")));
        assertFalse(Files.exists(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/logistics_wire.png")));
        assertTrue(Files.exists(mdkRoot().resolve("src/main/resources/assets/gt6/textures/block/item_pipe.png")),
                "item_pipe.png is the LD wire placeholder until that family's card — do not retire it");
        // the block-root logistics_wire MODEL reference is dead too (the iconsets pair replaced it)
        try (Stream<Path> tWalk = Files.walk(mdkRoot().resolve("src/generated/resources"))) {
            List<String> tOffenders = new ArrayList<>();
            for (Path p : tWalk.filter(f -> f.toString().endsWith(".json")).toList()) {
                if (Files.readString(p, StandardCharsets.UTF_8).contains("\"gt6:block/logistics_wire\"")) {
                    tOffenders.add(p.toString());
                }
            }
            assertTrue(tOffenders.isEmpty(),
                    "generated JSONs still reference the retired block-root logistics_wire art: " + tOffenders);
        }
    }

    @Test
    public void borrowedPngsExistOnTheStaticTree() {
        List<String> tBorrows = new ArrayList<>(List.of(
                "materialicons/wood/pipe_side.png",
                "materialicons/wood/pipe_side_overlay.png",
                "materialicons/copper/pipe_side.png",
                "materialicons/copper/pipe_side_overlay.png",
                "materialicons/rubber/pipe_side.png",
                "iconsets/logistics_wire.png",
                "iconsets/logistics_wire_overlay.png",
                "iconsets/pipe_restrictor.png"));
        // task pipe-render-closeout — the per-diameter connected-arm arts (the upstream
        // getIconIndexConnected selector, TileEntityBase10ConnectorRendered :265), the
        // wood / rubber / shared-copper folders by the byte-identity census
        for (String tSet : new String[] {"wood", "rubber", "copper"}) {
            for (String tSize : new String[] {"tiny", "small", "medium", "large", "huge"}) {
                tBorrows.add("materialicons/" + tSet + "/pipe_" + tSize + ".png");
            }
        }
        for (String tBorrow : tBorrows) {
            Path tPath = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block").resolve(tBorrow);
            try {
                assertTrue(Files.isRegularFile(tPath) && Files.size(tPath) > 0,
                        "the borrowed PNG is missing: " + tBorrow);
            } catch (IOException tErr) {
                throw new AssertionError(tBorrow, tErr);
            }
        }
        // every diameter borrow carries its sha256 README ledger row (the id1502 face —
        // the upstream snapshot is absent from coder worktrees, the ledger is the witness)
        try {
            String tReadme = Files.readString(
                    mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
            for (String tSet : new String[] {"wood", "rubber", "copper"}) {
                for (String tSize : new String[] {"tiny", "small", "medium", "large", "huge"}) {
                    byte[] tBytes = Files.readAllBytes(mdkRoot()
                            .resolve("src/main/resources/assets/gt6/textures/block/materialicons/" + tSet + "/pipe_" + tSize + ".png"));
                    assertTrue(tReadme.contains("`" + sha256(tBytes) + "`"),
                            "materialicons/" + tSet + "/pipe_" + tSize + ".png must carry its sha256 ledger row");
                }
            }
        } catch (Exception tErr) {
            throw new AssertionError(tErr);
        }
    }

    /**
     * The per-diameter arm selector pin (task pipe-render-closeout): the upstream
     * {@code getIconIndexConnected} thresholds (TileEntityBase10ConnectorRendered :265,
     * blocks = px/16 — 4→tiny, 6→small, 8→medium, 12→large, 16→huge and the
     * quadruple/nonuple rows ride huge) over the shared art-set dispatch (wood rows the
     * wood folder, the rubber row the rubber folder, the rest the shared copper copy).
     */
    @Test
    public void pipeArmArtRidesTheUpstreamDiameterSelector() {
        gregapi.oredict.OreDictMaterial tWood = gregtech6.registry.GTFluidPipes.MAT_WOOD.oreDictMaterial();
        gregapi.oredict.OreDictMaterial tRubber = gregtech6.registry.GTFluidPipes.MAT_RUBBER.oreDictMaterial();
        assertEquals("gt6:block/materialicons/wood/pipe_tiny",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(tWood, 4).toString());
        assertEquals("gt6:block/materialicons/wood/pipe_small",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(tWood, 6).toString());
        assertEquals("gt6:block/materialicons/rubber/pipe_medium",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(tRubber, 8).toString());
        assertEquals("gt6:block/materialicons/rubber/pipe_large",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(tRubber, 12).toString());
        assertEquals("gt6:block/materialicons/copper/pipe_huge",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(materialBySlug("steel"), 16).toString());
        assertEquals("gt6:block/materialicons/copper/pipe_huge",
                gregtech6.client.wire.GTWireTextures.pipeArmSprite(materialBySlug("brass"), 16).toString(),
                "the quadruple/nonuple rows (PX_P[16]) ride huge like upstream");
        // every live pipe row's arm art resolves (no registry-path drift)
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            assertNotNull(gregtech6.client.wire.GTWireTextures.pipeArmSprite(
                    tRow.material().oreDictMaterial(), tRow.variant().diameterPx));
        }
        for (gregtech6.registry.GTItemPipes.ItemPipeRow tRow : gregtech6.registry.GTItemPipes.ROWS) {
            assertNotNull(gregtech6.client.wire.GTWireTextures.pipeArmSprite(
                    tRow.material().oreDictMaterial(), tRow.variant().diameterPx));
        }
    }
}
