/**
 * Pure-JUnit item-model structure census for task p27-tool-model-layers — pins the
 * multi-layer wave offline (no client, no datagen run).
 *
 * <p>What it nails:</p>
 * <ul>
 *   <li>the tool rows carry the upstream four-pass icon as layer0..3
 *       (ToolStats.java:267-287: head base / head OVERLAY / handle base / handle OVERLAY;
 *       the vanilla layer number IS the tint index, ItemModelGenerator.java:15,35-44):
 *       builder_wand + saw + file + chisel + screwdriver = exactly 4 layers with pinned
 *       texture paths; hammer = the soft-hammer metallic-head shape, also 4 layers;
 *       the VOID-handle iconset tools (crowbar/cutter/wrench/bending_cylinder_small,
 *       passes 2/3 draw nothing upstream) = exactly 2 layers;</li>
 *   <li>the p38-issue6-tool-4layer-tint supersession: the former composed singles
 *       (screwdriver/hammer, the p24/p25 census-erratum ruling) are RETIRED — both tools
 *       now carry the upstream four-pass structure; the composed single-layer pin is
 *       gone with the ruling it guarded;</li>
 *   <li>every material prefix model obeys the existence-gated overlay rule: layer1 is
 *       present IF AND ONLY IF {@code material_sets/<set>/<prefix>_overlay.png} exists
 *       on the static ∪ generated face (research.p27-render-three-fixes F2: the gate is
 *       file existence, never prefix/set inference — BRICK's rockGt base is the full
 *       cobble, DULL/METALLIC's body lives entirely in the overlay);</li>
 *   <li>the F2 visual-fix carriers exist as real layers: a rock_gt model resolves a
 *       rock_gt_overlay layer1 and a chemtube model resolves a chemtube_overlay layer1
 *       (the offline half of the runClient目验 owe-list).</li>
 * </ul>
 *
 * <p>Same posture as {@link GT6TextureCensusTest}: read-only over the two committed
 * resource trees, writes nothing, never passes vacuously (the prefix walk must cover
 * thousands of models).</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6ItemModelLayersTest {

    /** The datagen product tree (GT6ItemModels writes here; also a texture face). */
    private static final Path GENERATED_TREE = Path.of("src", "generated", "resources");

    /** The static borrow tree (assets/README.md face; textures live here). */
    private static final Path STATIC_TREE = Path.of("src", "main", "resources");

    private static final String ITEM_MODELS = "assets/gt6/models/item";
    private static final String TEXTURES_PREFIX = "assets/gt6/textures";

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

    /**
     * The parsed textures map of one generated item model, or null when the model is a
     * parent-reference form (BlockItem rows) with no textures of its own.
     */
    private static Map<String, String> texturesOf(Path itemModels, String modelId) throws IOException {
        Path model = itemModels.resolve(modelId + ".json");
        assertTrue(Files.isRegularFile(model), "generated item model missing: " + model);
        JsonObject textures = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("textures");
        if (textures == null) return null;
        Map<String, String> rMap = new LinkedHashMap<>();
        for (var entry : textures.entrySet()) {
            rMap.put(entry.getKey(), entry.getValue().getAsString());
        }
        return rMap;
    }

    /** The layer0..layerN keys of a textures map, in layer order. */
    private static List<String> layerKeys(Map<String, String> textures) {
        List<String> rKeys = new ArrayList<>();
        for (int i = 0; ; i++) {
            String key = "layer" + i;
            if (!textures.containsKey(key)) break;
            rKeys.add(key);
        }
        return rKeys;
    }

    /** Every texture relative path present in one tree; absent tree = empty. */
    private static Set<String> textureRels(Path mdk, Path treeRoot) throws IOException {
        Set<String> rels = new java.util.HashSet<>();
        Path textures = mdk.resolve(treeRoot).resolve(TEXTURES_PREFIX);
        if (!Files.isDirectory(textures)) return rels;
        try (Stream<Path> walk = Files.walk(textures)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".png"))
                .forEach(p -> rels.add(TEXTURES_PREFIX + "/"
                    + textures.relativize(p).toString().replace('\\', '/')));
        }
        return rels;
    }

    /** "gt6:item/material_sets/ruby/rock_gt" -> "assets/gt6/textures/item/material_sets/ruby/rock_gt.png". */
    private static String textureRel(String resource) {
        return TEXTURES_PREFIX + "/" + resource.substring("gt6:".length()) + ".png";
    }

    /** Every generated item model JSON filename (no directory recursion — flat walk). */
    private static List<String> allItemModels(Path itemModels) throws IOException {
        try (Stream<Path> walk = Files.list(itemModels)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                .map(p -> p.getFileName().toString())
                .toList();
        }
    }

    private static void assertLayer(Map<String, String> textures, int index, String expectedResource,
                                    String modelId) {
        String key = "layer" + index;
        assertEquals(expectedResource, textures.get(key),
            modelId + " " + key + " drifted from the pinned path");
    }

    /**
     * The four-pass tools: head pair on the metallic set (default primary Steel) + the
     * HANDLE_* iconset pair for saw/file/chisel; the builder wand's EMERALD head pair +
     * the Scorched(wood-set) stick pair. Exactly 4 layers, every path pinned.
     */
    @Test
    void fourPassToolsCarryExactlyFourLayers() throws IOException {
        Path itemModels = mdkRoot().resolve(GENERATED_TREE).resolve(ITEM_MODELS);
        assertTrue(Files.isDirectory(itemModels));

        // saw/file/chisel/screwdriver: layer0/1 = the toolHead* materialicon pair
        // (Steel = metallic), layer2/3 = the HANDLE_* iconset pair borrows
        // (GT_Tool_Saw.java:186-188, GT_Tool_File.java:91-93, GT_Tool_Chisel.java:87-89,
        // GT_Tool_Screwdriver.java:115-117 — the p38-issue6 four-layer migration).
        for (String head : new String[] {"saw", "file", "chisel", "screwdriver"}) {
            Map<String, String> textures = texturesOf(itemModels, head);
            assertEquals(4, layerKeys(textures).size(), head + " must carry exactly 4 layers");
            assertLayer(textures, 0, "gt6:item/material_sets/metallic/tool_head_" + head, head);
            assertLayer(textures, 1, "gt6:item/material_sets/metallic/tool_head_" + head + "_overlay", head);
            assertLayer(textures, 2, "gt6:item/" + head, head);
            assertLayer(textures, 3, "gt6:item/" + head + "_overlay", head);
        }

        // builder_wand: EMERALD head pair + the Scorched(SET_WOOD) stick pair
        // (GT_Tool_Builderwand.java:51-53; layer0 keeps the byte-identical borrow).
        Map<String, String> wand = texturesOf(itemModels, "builder_wand");
        assertEquals(4, layerKeys(wand).size(), "builder_wand must carry exactly 4 layers");
        assertLayer(wand, 0, "gt6:item/builder_wand", "builder_wand");
        assertLayer(wand, 1, "gt6:item/material_sets/emerald/tool_head_builderwand_overlay", "builder_wand");
        assertLayer(wand, 2, "gt6:item/material_sets/wood/stick", "builder_wand");
        assertLayer(wand, 3, "gt6:item/material_sets/wood/stick_overlay", "builder_wand");

        // soft_hammer (task p29-w5-t3-machine-face-four): the RUBBER-set toolHeadHammer
        // pair (the upstream ANY.Rubber primary, GT_Tool_SoftHammer.getIcon :120) + the
        // wood stick pair (the MT.WOODS.Spruce secondary, the same row) — the wand shape,
        // all four layers material_sets borrows (zero new sprites).
        Map<String, String> softHammer = texturesOf(itemModels, "soft_hammer");
        assertEquals(4, layerKeys(softHammer).size(), "soft_hammer must carry exactly 4 layers");
        assertLayer(softHammer, 0, "gt6:item/material_sets/rubber/tool_head_hammer", "soft_hammer");
        assertLayer(softHammer, 1, "gt6:item/material_sets/rubber/tool_head_hammer_overlay", "soft_hammer");
        assertLayer(softHammer, 2, "gt6:item/material_sets/wood/stick", "soft_hammer");
        assertLayer(softHammer, 3, "gt6:item/material_sets/wood/stick_overlay", "soft_hammer");

        // hard hammer (task p38-issue6-tool-4layer-tint, supersedes the p25 composed
        // single): the soft-hammer row shape over the METALLIC head pair (the default
        // primary Steel) + the wood stick pair (the secondary MT.WOODS.Spruce ride,
        // GT_Tool_HardHammer.getIcon :123) — zero new sprites, all in-tree borrows.
        Map<String, String> hammer = texturesOf(itemModels, "hammer");
        assertEquals(4, layerKeys(hammer).size(), "hammer must carry exactly 4 layers");
        assertLayer(hammer, 0, "gt6:item/material_sets/metallic/tool_head_hammer", "hammer");
        assertLayer(hammer, 1, "gt6:item/material_sets/metallic/tool_head_hammer_overlay", "hammer");
        assertLayer(hammer, 2, "gt6:item/material_sets/wood/stick", "hammer");
        assertLayer(hammer, 3, "gt6:item/material_sets/wood/stick_overlay", "hammer");
    }

    /** The VOID-handle iconset tools: base + overlay, exactly 2 layers. */
    @Test
    void voidHandleToolsCarryExactlyTwoLayers() throws IOException {
        Path itemModels = mdkRoot().resolve(GENERATED_TREE).resolve(ITEM_MODELS);
        for (String tool : new String[] {"crowbar", "cutter", "wrench", "bending_cylinder_small", "monkey_wrench", "magnifying_glass", "pincers"}) {
            Map<String, String> textures = texturesOf(itemModels, tool);
            assertEquals(2, layerKeys(textures).size(), tool + " must carry exactly 2 layers");
            assertLayer(textures, 0, "gt6:item/" + tool, tool);
            assertLayer(textures, 1, "gt6:item/" + tool + "_overlay", tool);
        }
    }

    // RETIRED with the ruling it guarded (task p38-issue6-tool-4layer-tint): the former
    // composedSinglesStaySingleLayer test ("screwdriver/hammer stay exactly 1 layer",
    // the census erratum) is superseded by the four-layer migration — both tools are
    // pinned in fourPassToolsCarryExactlyFourLayers above.

    /** The tool ids with their own model rows (pinned by the dedicated tests above). */
    private static final Set<String> TOOL_MODEL_IDS = Set.of(
        "crowbar", "cutter", "chisel", "file", "saw", "builder_wand", "screwdriver", "hammer",
        "sword", "knife", "butchery_knife", "club", "axe", "axe_double", // task p29-w5-t2-blade-six — the blade rows carry their own multi-layer pins
        // task p31-dig-ladder — the dig band's restored 4-layer rows (head pair + stick pair):
        "pickaxe", "pickaxe_gem", "pickaxe_construction", "shovel", "spade", "universal_spade", "hoe",
        "soft_hammer", // task p29-w5-t3-machine-face-four — the 4-layer rubber-head pin
        // task p38-c1-dynamo-bowl-models — the p29-w5-t4 un-laddered quartet's restored
        // rows (plow/sense = the 4-layer head-pair + stick-pair row; branch_cutter /
        // hand_drill = the 2-layer iconset pair — their own pins live in
        // GT6DynamoBowlRenderDatagenTest.caughtItemModelsPinParentsAndLayers)
        "plow", "sense", "branch_cutter", "hand_drill",
        // task p29-w5-t6-electric-nineteen — the electric rows whose layer0 rides the
        // material_sets head sprite: their layer1 is the POWER-UNIT/HANDLE pass (the
        // upstream getIcon(true) pass), NOT the base's _overlay sibling — the deliberate
        // exemption, the t3 machine-face-four TOOL_MODEL_IDS ruling
        "mining_drill_lv", "mining_drill_mv", "mining_drill_hv",
        "chainsaw_lv", "chainsaw_mv", "chainsaw_hv",
        "wrench_lv", "wrench_mv", "wrench_hv",
        "monkey_wrench_lv", "monkey_wrench_mv", "monkey_wrench_hv",
        "buzzsaw_lv", "screwdriver_lv");

    /**
     * The existence-gated overlay rule, walked over EVERY generated item model that
     * carries a material_sets layer0: layer1 present IF AND ONLY IF the
     * {@code <prefix>_overlay} PNG exists on the static ∪ generated face — and no
     * layer1 may ever point at a non-existent file.
     */
    @Test
    void everyPrefixModelObeysTheExistenceGate() throws IOException {
        Path mdk = mdkRoot();
        Path itemModels = mdk.resolve(GENERATED_TREE).resolve(ITEM_MODELS);
        Set<String> resolvable = textureRels(mdk, STATIC_TREE);
        resolvable.addAll(textureRels(mdk, GENERATED_TREE));

        List<String> violations = new ArrayList<>();
        int checked = 0;
        int withOverlay = 0;
        for (String file : allItemModels(itemModels)) {
            if (TOOL_MODEL_IDS.contains(file.substring(0, file.length() - ".json".length()))) {
                continue; // the tool rows carry their own 2/4-layer pins — not prefix models
            }
            Map<String, String> textures = texturesOf(itemModels, file.substring(0, file.length() - ".json".length()));
            if (textures == null) continue; // parent-reference form (BlockItem rows), no textures of its own
            String layer0 = textures.get("layer0");
            if (layer0 == null || !layer0.startsWith("gt6:item/material_sets/")) continue;
            checked++;
            List<String> layers = layerKeys(textures);
            boolean hasLayer1 = layers.contains("layer1");
            String overlayRel = textureRel(layer0.substring(0, layer0.lastIndexOf('/') + 1)
                + layer0.substring(layer0.lastIndexOf('/') + 1) + "_overlay");
            boolean overlayOnDisk = resolvable.contains(overlayRel);
            if (hasLayer1 != overlayOnDisk) {
                violations.add(file + ": layer1=" + hasLayer1 + " but " + overlayRel + " exists=" + overlayOnDisk);
            }
            if (hasLayer1) {
                withOverlay++;
                String layer1 = textures.get("layer1");
                if (!resolvable.contains(textureRel(layer1))) {
                    violations.add(file + ": layer1 -> unresolvable " + layer1);
                }
                // the overlay layer must be the sibling _overlay sprite of the base
                String expected = layer0 + "_overlay";
                assertEquals(expected, layer1, file + ": layer1 is not the base's overlay sibling");
            }
            // no layer beyond the overlay pass on prefix models (tint ladder = later card)
            assertTrue(layers.size() <= 2, file + ": unexpected extra layers " + layers);
        }
        assertTrue(checked > 50000, "prefix walk too small (" + checked + ") — the census must never pass vacuously");
        assertTrue(withOverlay > 50000, "overlay layer coverage too small (" + withOverlay + ")");
        assertTrue(violations.isEmpty(), "existence-gate violations: " + violations.size() + "\n  "
            + String.join("\n  ", violations.subList(0, Math.min(10, violations.size()))));
    }

    /**
     * The F2 visual-fix carriers: at least one rock_gt model and one chemtube model
     * actually resolve their overlay layer (the offline half of the runClient owe-list:
     * rockGt body + chemtube glass wall live entirely in the OVERLAY sprite).
     */
    @Test
    void rockGtAndChemtubeResolveTheirOverlayLayers() throws IOException {
        Path itemModels = mdkRoot().resolve(GENERATED_TREE).resolve(ITEM_MODELS);
        Map<String, String> rockGt = texturesOf(itemModels, "rock_gt_andradite");
        assertEquals("gt6:item/material_sets/ruby/rock_gt_overlay", rockGt.get("layer1"),
            "rock_gt_andradite must carry the rock_gt_overlay body layer");
        String chemtube = allItemModels(itemModels).stream()
            .filter(f -> f.startsWith("chemtube_"))
            .findFirst().orElse(null);
        assertNotNull(chemtube, "no chemtube prefix model found");
        Map<String, String> textures = texturesOf(itemModels, chemtube.substring(0, chemtube.length() - ".json".length()));
        assertTrue(textures.get("layer1") != null && textures.get("layer1").endsWith("chemtube_overlay"),
            chemtube + " must carry the chemtube_overlay glass-wall layer");
    }
}
