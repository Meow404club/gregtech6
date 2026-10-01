/**
 * Tests for task wood-planks-register: the OP.plank prefix registration (the second
 * block-path item-path adaptation, after the casingMachine quartet of task
 * casing-machine-register).
 *
 * <p>Upstream grounding (the itemPathPrefixes javadoc carries the full face): upstream
 * OP.plank has NO setCondition (OP.java:394 verbatim, default condition TRUE) and NO
 * PrefixItem — planks upstream are BLOCKS (Loader_Woods.java:62-65 BlockTreePlanks/2,
 * the IL.Plank generic face = BlocksGT.Planks meta 9 "Wood Planks", Loader_Woods.java:74
 * + BlockTreePlanks.java:49). The port registers flat plank items under the
 * port-authority {@link TD.Properties#WOOD} material gate (generatesItemPathItem) —
 * the domain the card declares because an unconditioned walk would flood every material.
 *
 * <p><b>The absorb seam (NOT re-poured here)</b>: the sawing increment card (task
 * sawing-plank-concrete-increment, the DECLARED IDENTITY MAPPING in its sawing.json
 * comment) rides IL.Plank -> minecraft:oak_planks until a plank registration card
 * lands, then re-pours onto the generic plank face (gt6:plank_wood — this card's
 * MT.Wood item). This card owns the ITEM face only: no recipe JSON changes, the
 * re-pour is that card's follow-up.
 *
 * <p>Texture face: the only upstream plank art is the BLOCK iconset
 * (blocks/iconsets/PLANKS_WOOD.png, the meta-9 block face) — the items materialicons
 * domain carries zero plank sprites (census, assets/README.md). The borrow is one
 * sprite per reachable material set, tinted at runtime (tintIndex 0, the
 * material_sets seat method), pinned here by digest.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class GT6PlankRegistrationTest {

    /** The upstream generic plank block face, the one plank art upstream ships (BLOCK iconset, PLANKS_WOOD.png, 16x16 RGBA8). */
    private static final String UPSTREAM_PLANKS_WOOD_SHA256 =
        "f7acb1dfd1f99ef5181b3a420992099f2113adfc3972e0da5ad7fa012129838f";

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    /** Independent recount of the plank domain: alias-merged materials x the WOOD gate. */
    private static List<OreDictMaterial> plankDomain() {
        Set<OreDictMaterial> tSeen = new HashSet<>();
        List<OreDictMaterial> rDomain = new java.util.ArrayList<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
            if (tMaterial == null || tMaterial.mID < 0) continue;
            if (!tSeen.add(tMaterial)) continue;
            if (!tMaterial.contains(TD.Properties.WOOD)) continue; // the declared gate, re-walked independently
            rDomain.add(tMaterial);
        }
        return rDomain;
    }

    @Test
    public void plankDomainIsTheWoodFamily() {
        // the production walk and the independent walk must agree exactly
        Set<String> tProduction = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.plank) tProduction.add(tPair.material().mNameInternal);
        }
        Set<String> tIndependent = new TreeSet<>();
        for (OreDictMaterial tMaterial : plankDomain()) tIndependent.add(tMaterial.mNameInternal);
        assertEquals(tIndependent, tProduction, "the registered plank set must equal the independent WOOD-gate walk");
        assertFalse(tProduction.isEmpty(), "the plank domain is non-empty (Wood leads it)");
    }

    /**
     * Plain-JVM iconset resolution (the {@link GT6ItemModels#iconsetOf} logic re-walked with
     * {@link GTMaterialItems#snakeCase}: MaterialPrefixItem extends Item and drags vanilla
     * registry statics into class init, so the datagen accessor itself is not plain-JVM
     * callable — the GTMaterialItems snakeCase copy exists for exactly this reason).
     */
    private static String iconsetOfPlain(OreDictMaterial aMaterial) {
        List<String> tSets = aMaterial.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : GTMaterialItems.snakeCase(tSets.get(0));
    }

    @Test
    public void plankCensusPinsTheDomainSize() {
        long tCount = GTMaterialItems.registrationOrder().stream().filter(tPair -> tPair.prefix() == OP.plank).count();
        Map<String, Long> tSets = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() == OP.plank) tSets.merge(iconsetOfPlain(tPair.material()), 1L, Long::sum);
        }
        // measured 2026-10-01 (offline walk, the census dump run): 130 WOOD-tagged materials over
        // 4 texture sets — the Wood-led family (Wood/its 10 grade woods/species/magical/mod woods),
        // Bark (rough), Steeleaf+Fireleaf (leaf), Marshmallow (fine)
        assertEquals(130, tCount, "the plank domain census — re-pin deliberately on material-tree drift");
        assertEquals(Map.of("wood", 126L, "rough", 1L, "leaf", 2L, "fine", 1L), tSets,
            "the per-texture-set distribution — the borrow face list (every set here must hold plank.png)");
    }

    @Test
    public void thePortGateIsNotAnUpstreamCondition() {
        // upstream verbatim: OP.plank's own condition is TRUE for every material (no setCondition,
        // OP.java:394) — the restriction is the PORT gate, not the OP condition. Pinning both ways
        // proves the deviation is where the javadoc says it is.
        assertTrue(OP.plank.isGeneratingItem(MT.Iron), "upstream default condition passes iron (no setCondition, OP.java:394)");
        assertFalse(GTMaterialItems.generatesItemPathItem(OP.plank, MT.Iron), "the port gate refuses iron (the declared port-authority ruling)");
        assertTrue(GTMaterialItems.generatesItemPathItem(OP.plank, MT.Wood), "the gate admits Wood (the IL.Plank identity material)");
        assertTrue(GTMaterialItems.generatesItemPathItem(OP.plank, MT.WOODS.Rainbowood), "the gate admits the species woods (the upstream OP.plank.dat carrier, BlockTreePlanks.java:57)");
        // non-plank prefixes keep the plain isGeneratingItem face (the gate is plank-scoped)
        assertEquals(OP.plate.isGeneratingItem(MT.Iron), GTMaterialItems.generatesItemPathItem(OP.plate, MT.Iron),
            "the gate is transparent for every non-plank prefix");
    }

    @Test
    public void plankIdsAreUniqueAndOutsideTheTreePlankFamily() {
        Set<String> tIds = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            assertTrue(tIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                "duplicate id " + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        }
        // the generic plank face exists under the prefix-item id scheme (the sawing absorb target)
        assertTrue(tIds.contains("plank_wood"), "gt6:plank_wood must exist (the IL.Plank identity face, the sawing re-pour target)");
        assertTrue(tIds.contains("plank_wood_treated"), "gt6:plank_wood_treated must exist (the Bath treated-leg output)");
        assertTrue(tIds.contains("plank_wood_polished"), "gt6:plank_wood_polished must exist (the Bath polished-leg output)");
        // and none of them collides with the gt-tree-planks BLOCK items (gt6:<snake>_planks,
        // GT6TreeBlocks.PLANK_ITEMS — different objects, the task card's do-not-confuse face)
        for (String tPlankId : tIds) {
            if (tPlankId.startsWith("plank_")) assertFalse(tPlankId.endsWith("_planks"),
                "prefix-item ids never share the tree-plank <snake>_planks shape");
        }
    }

    @Test
    public void everyPlankItemResolvesABorrowedSprite() throws IOException {
        // the tint face: layer0 = gt6:item/material_sets/<iconset>/plank must exist for every
        // registered plank pair (the GT6ItemModels walk emits exactly these refs)
        Map<String, Long> tSets = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            if (tPair.prefix() != OP.plank) continue;
            tSets.merge(iconsetOfPlain(tPair.material()), 1L, Long::sum);
        }
        Path tMdk = mdkRoot();
        for (String tSet : tSets.keySet()) {
            Path tPng = tMdk.resolve("src/main/resources/assets/gt6/textures/item/material_sets/" + tSet + "/plank.png");
            assertTrue(Files.isRegularFile(tPng), "missing borrowed sprite for set " + tSet + " (" + tSets.get(tSet) + " items)");
        }
        assertFalse(tSets.containsKey("none"), "no plank material may resolve to SET_NONE");
    }

    @Test
    public void everyBorrowedSpriteIsTheUpstreamBlockFace() throws IOException {
        // the borrow cmp: every material_sets/plank.png byte-matches the one upstream plank art
        MessageDigest tSha256 = null;
        try {
            tSha256 = MessageDigest.getInstance("SHA-256");
        } catch (Exception aE) {
            throw new IllegalStateException(aE);
        }
        Path tMdk = mdkRoot();
        int tChecked = 0;
        try (var tWalk = Files.walk(tMdk.resolve("src/main/resources/assets/gt6/textures/item/material_sets"))) {
            for (Path tPng : tWalk.filter(tPath -> tPath.getFileName().toString().equals("plank.png")).toList()) {
                String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
                assertEquals(UPSTREAM_PLANKS_WOOD_SHA256, tHex, tPng + " must be the PLANKS_WOOD block-iconset borrow byte-identical");
                tChecked++;
            }
        }
        assertTrue(tChecked > 0, "the borrow cmp must never pass vacuously");
    }

    /** mdk root, walked up from the leg-dependent test working dir (the GT6TextureCensusTest shape). */
    private static Path mdkRoot() {
        for (Path tPath = Path.of("").toAbsolutePath(); tPath != null; tPath = tPath.getParent()) {
            if (Files.isRegularFile(tPath.resolve("tools").resolve("gen_textures.py"))) return tPath;
        }
        throw new AssertionError("mdk root not found upward from " + Path.of("").toAbsolutePath());
    }
}
