/*
 * Offline pinned-census tests for task tex-pipe-textures: the three pipe connector
 * families (2 wood fluid rows, the item pipe matrix — 126 rows since item-pipe-matrix,
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
 * <li>the four shared models keep the two-layer shape — tintindex-0 body over the set
 *     art, UNTINTED overlay bands (no tintindex key — FaceBuilder default -1), the
 *     cutout render_type, the restrictive twin stacking the restrictor band;</li>
 * <li>the set picks ride the row materials: the fluid rows resolve WOOD (the MT wood
 *     factory = SET_WOOD), the item rows the shared COPPER pair (clloymachine = SET_COPPER;
 *     MT.java:716/788), the logistics wire the dedicated pair (NBT_MATERIAL = MT.NULL,
 *     Loader :1819) — the tint-value/dispatch face of that seam is
 *     {@code gregtech6.client.render.GTMachinePaintTintTest#pipeCarriersRideTheCombinedDispatch};</li>
 * <li>all 129 pipe blockstates (2 fluid + 126 item + the logistics wire) map every
 *     CONNECTIONS variant (0..63) onto the right shared model, and the BlockItem models
 *     parent those shared models — the 18 new materials ride the copper pair too, the
 *     per-set art face (shiny/dull/metallic) is the render-pool card's domain;</li>
 * <li>the retired placeholders are DEAD — no generated or static JSON references
 *     fluid_pipe_wood / item_pipe_restrictive / the block-root logistics_wire art any
 *     more, and the three PNGs are gone from the static tree (item_pipe.png STAYS: the
 *     16 LD wire placeholder rows still consume it, a separate card's domain);</li>
 * <li>the seven borrowed PNGs exist on the static tree (byte identity is the README
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

    /** The four shared models (the tintedPipeModel outputs, texture paths as model names). */
    private static final String WOOD_MODEL = "materialicons/wood/pipe_side";
    private static final String COPPER_MODEL = "materialicons/copper/pipe_side";
    private static final String COPPER_RESTRICTIVE_MODEL = "materialicons/copper/pipe_side_restrictive";
    private static final String LOGISTICS_MODEL = "iconsets/logistics_wire";

    /**
     * The pipe blockstate registry paths (task item-pipe-matrix: the item material slugs
     * walk the GTItemPipes table — 21 loader lines × 6 variants — so the datagen shape
     * stays pinned over the whole matrix, not a slug snapshot) and the 280 fluid row
     * paths (task fluid-pipe-matrix) with the declared RENDER TRANSITION picks: WOODEN-
     * block rows ride the wood family, everything else the copper family (the only
     * borrowed pipe art in the repo — the per-material colour rides the tint chain;
     * connection-aware geometry is the rod-render-pool card).
     */
    private static Map<String, String> fluidModelPicks() {
        Map<String, String> rPicks = new LinkedHashMap<>();
        for (gregtech6.registry.GTFluidPipes.FluidPipeRow tRow : gregtech6.registry.GTFluidPipes.ROWS) {
            rPicks.put(tRow.path(),
                    tRow.material().blockFamily() == gregtech6.registry.GTFluidPipes.PipeBlockFamily.WOODEN ? WOOD_MODEL : COPPER_MODEL);
        }
        return rPicks;
    }

    private static final List<String> RESTRICTIVE_TAILS = List.of("restrictive_medium", "restrictive_large", "restrictive_huge");
    private static final List<String> ITEM_MATERIALS = gregtech6.registry.GTItemPipes.MATERIALS.stream()
            .map(gregtech6.registry.GTItemPipes.ItemPipeMaterial::slug).toList();

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
        for (String tModel : List.of(WOOD_MODEL, COPPER_MODEL, COPPER_RESTRICTIVE_MODEL, LOGISTICS_MODEL)) {
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
                "the fluid rows ride the WOOD set (the MT wood factory = SET_WOOD)");
        assertEquals("gt6:block/materialicons/copper/pipe_side",
                treeJson(modelRelPath(COPPER_MODEL)).getAsJsonObject("textures").get("all").getAsString(),
                "the item rows ride the COPPER set (clloymachine = SET_COPPER)");
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
        for (String tMat : ITEM_MATERIALS) {
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
        for (String tMat : ITEM_MATERIALS) {
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
        String[] tBorrows = {
                "materialicons/wood/pipe_side.png",
                "materialicons/wood/pipe_side_overlay.png",
                "materialicons/copper/pipe_side.png",
                "materialicons/copper/pipe_side_overlay.png",
                "iconsets/logistics_wire.png",
                "iconsets/logistics_wire_overlay.png",
                "iconsets/pipe_restrictor.png"};
        for (String tBorrow : tBorrows) {
            Path tPath = mdkRoot().resolve("src/main/resources/assets/gt6/textures/block").resolve(tBorrow);
            try {
                assertTrue(Files.isRegularFile(tPath) && Files.size(tPath) > 0,
                        "the borrowed PNG is missing: " + tBorrow);
            } catch (IOException tErr) {
                throw new AssertionError(tBorrow, tErr);
            }
        }
    }
}
