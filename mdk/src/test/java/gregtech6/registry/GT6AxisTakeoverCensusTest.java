/**
 * The mdh-5 block/worldgen-axis takeover census (task mdh-5-block-worldgen-axis, the
 * closure card): pins the cross-table of the GT6ForeignMaterialAtlas rows against the
 * FOUR worldgen/block intersection faces and the closure rulings that keep every face on
 * the static terminal state (ruling (a) — zero behaviour change, no driver gate on the
 * axes; the full account lives in state decisions.mdh-5-axis-rulings).
 *
 * <p>The four faces and their census basis:
 * <ol>
 * <li>block axis — {@link GT6OreBlocks#materialAxis()} (159, the :486 isGeneratingItem
 * filter is its only gate; 154 through b2+B1, 159 since worldgen-axis-batch2);</li>
 * <li>bedrock axis — {@link GT6BedrockOreBlocks#materialAxis()} (45, no driver gate);</li>
 * <li>worldgen rows — {@link GTOreWorldgen#ROWS} (130 rows / 167 placement pairs, the
 * mod-gated Loader_Worldgen.java:854-874 rows stay out as the compat pool; 125/162
 * through b2, 130/167 since worldgen-axis-batch2);</li>
 * <li>lens/large-vein/bedrock tables — GT6WorldgenDatagen LENS_ORE_TABLE (22) +
 * LARGE_VEIN_TABLE (40) + BEDROCK_ORE_TABLE (46).</li>
 * </ol>
 *
 * <p>Census verdict pinned here: the atlas-PRIMARY live faces are exactly the five
 * always-on block-axis members (Azurite/Eudialyte/CaF2/Jade/Dolamide) plus the two
 * offworld bedrock-axis members (Dolamide/Adamantine); the HBM Columbite triple-intersect
 * candidate is COMMON_SECONDARY (never hidden); every consumption face self-guards
 * (GT6OreLootTables.java:236 loop-head null-drop, the batch2 sweep account), so no dead
 * reference exists anywhere and the mdh-5b driver-gate escape hatch is NOT triggered.
 * The javadoc census interlinks in the four face classes are grep-pinned too.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GT6ForeignMaterialAtlas.AttributionKind;
import gregtech6.registry.GT6ForeignMaterialAtlas.Row;
import gregtech6.worldgen.GTBedrockOreConfig;
import gregtech6.worldgen.GTOreWorldgen;
import gregtech6.worldgen.GTVeinConfig;

public class GT6AxisTakeoverCensusTest {

    @BeforeAll
    public static void initMaterialSystem() {
        // the material system must exist before any MT dereference; vanilla bootstrap
        // bracket for the GT6OreBlocks/GTOreWorldgen/GT6WorldgenDatagen <clinit> faces
        // (SoundType/ResourceKey interns — the GTOreWorldgenDatagenTest.boot posture).
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Face-set helpers (each face walked live, never a hand-copied list).
    // ------------------------------------------------------------------

    /** Face 1: the block axis, keyed by internal name. */
    private static Map<String, OreDictMaterial> blockAxis() {
        Map<String, OreDictMaterial> rAxis = new LinkedHashMap<>();
        for (OreDictMaterial tMaterial : GT6OreBlocks.materialAxis()) rAxis.put(tMaterial.mNameInternal, tMaterial);
        return rAxis;
    }

    /** Face 2: the bedrock axis, keyed by internal name. */
    private static Map<String, OreDictMaterial> bedrockAxis() {
        Map<String, OreDictMaterial> rAxis = new LinkedHashMap<>();
        for (OreDictMaterial tMaterial : GT6BedrockOreBlocks.materialAxis()) rAxis.put(tMaterial.mNameInternal, tMaterial);
        return rAxis;
    }

    /** The resolved atlas PRIMARY rows (material must resolve; the atlas test pins resolution itself). */
    private static List<Map.Entry<Row, OreDictMaterial>> primaryRows() {
        List<Map.Entry<Row, OreDictMaterial>> rRows = new ArrayList<>();
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            if (tRow.kind() != AttributionKind.PRIMARY) continue;
            OreDictMaterial tMaterial = tRow.material().get();
            assertNotNull(tMaterial, "atlas PRIMARY material must resolve: " + tRow);
            rRows.add(Map.entry(tRow, tMaterial));
        }
        return rRows;
    }

    /** Face 3: the worldgen rows' resolved materials keyed by row tail. */
    private static Map<String, OreDictMaterial> rowMaterials() {
        Map<String, OreDictMaterial> rRows = new LinkedHashMap<>();
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tRow);
            assertNotNull(tMaterial, "worldgen row material must resolve: " + tRow.name());
            rRows.put(tRow.tail(), tMaterial);
        }
        return rRows;
    }

    /** Face 4a: the lens rows' resolved materials keyed by "lens/material". */
    private static Map<String, OreDictMaterial> lensMaterials() {
        Map<String, OreDictMaterial> rRows = new LinkedHashMap<>();
        for (GT6WorldgenDatagen.LensOreRow tRow : GT6WorldgenDatagen.LENS_ORE_TABLE) {
            OreDictMaterial tMaterial = GT6WorldgenDatagen.lensOreResolve(tRow);
            assertNotNull(tMaterial, "lens row material must resolve: " + tRow.lens());
            rRows.put(tRow.lens() + "/" + tMaterial.mNameInternal, tMaterial);
        }
        return rRows;
    }

    /** All four vein slots of one row (the PRIMARY-slot walk). */
    private static List<OreDictMaterial> veinSlots(GTVeinConfig aRow) {
        return List.of(aRow.oreTop(), aRow.oreBottom(), aRow.oreBetween(), aRow.oreSpread());
    }

    /** The census statement under test: which atlas PRIMARY rows hit a material set. */
    private static Set<String> primaryHitsByInternalName(Set<String> aFaceNames) {
        Set<String> rHits = new HashSet<>();
        for (Map.Entry<Row, OreDictMaterial> tEntry : primaryRows()) {
            if (aFaceNames.contains(tEntry.getValue().mNameInternal)) {
                rHits.add(tEntry.getValue().mNameInternal + "@" + tEntry.getKey().domain());
            }
        }
        return rHits;
    }

    // ------------------------------------------------------------------
    // The census basis: face sizes (the cross-table's row counts).
    // ------------------------------------------------------------------

    @Test
    public void faceCounts_pinTheCensusBasis() {
        assertEquals(441, GT6ForeignMaterialAtlas.rows().size(), "atlas universe: 164 batch-1 + 277 batch-2");
        assertEquals(159, GT6OreBlocks.materialAxis().size(), "block axis: 53 + 13 + 15 + 56 + 22 (the 15 EDGE = b2's 10 boundary blobs + batch2's 5 stone-layer/lens anchors)");
        assertEquals(45, GT6BedrockOreBlocks.materialAxis().size(), "bedrock axis: 46 table rows, gold.a/b share");
        assertEquals(130, GTOreWorldgen.ROWS.size(), "54 always-on rows + 61 gem-pool rows + the 10 boundary-blob rows (b2) + the 5 stone-layer/lens anchor rows (batch2)");
        assertEquals(167, GTOreWorldgen.placementPairs().size(), "overworld 114 + nether 20 + end 33, ancientdebris gated (b2 boundary + batch2 anchor rows joined)");
        assertEquals(22, GT6WorldgenDatagen.LENS_ORE_TABLE.size(), "5-lens companion table");
        assertEquals(40, GT6WorldgenDatagen.LARGE_VEIN_TABLE.size(), "large-vein table :886-925");
        assertEquals(46, GT6WorldgenDatagen.BEDROCK_ORE_TABLE.size(), "bedrock table :725-770, HEX row cut");
    }

    // ------------------------------------------------------------------
    // Face 1 ruling: the seven always-on PRIMARY block-axis members (five
    // pre-B1, + Zircon/Nd since worldgen-edge-ores-b1 admitted the :898/:911
    // vein rows to the axis).
    // ------------------------------------------------------------------

    @Test
    public void blockAxis_primaryHits_areExactlyTheAlwaysOnMaterials() {
        Set<String> tHits = primaryHitsByInternalName(blockAxis().keySet());
        Set<String> tExpected = Set.of(
                "Azurite@" + MT.MD.TROPIC.mID,      // :2527, WORLDGEN_ORES :831 row
                "Eudialyte@" + MT.MD.TROPIC.mID,    // :2528, WORLDGEN_ORES :830 row
                "Fluorite@" + MT.MD.RoC.mID,        // :2420, CaF2's internal name, GEM_POOL :1109 factory head
                "Jade@" + MT.MD.ERE.mID,            // :2301, GEM_POOL inline flag :1443
                "Dolamide@" + MT.MD.MO.mID,         // :2608, WORLDGEN_ORES :843 row
                "Zircon@" + MT.MD.TROPIC.mID,       // :2526, LARGE_VEIN_ORES :911 titanium between (worldgen-edge-ores-b1)
                "Neodymium@" + MT.MD.HBM.mID);      // :2403, LARGE_VEIN_ORES :898 monazite spread (worldgen-edge-ores-b1)
        assertEquals(tExpected, tHits,
                "block-axis PRIMARY face is exactly the seven always-on materials (census verdict; the B1 reopen declared, a further drift reopens mdh-5)");
    }

    // ------------------------------------------------------------------
    // Face 2 ruling: the two offworld PRIMARY bedrock-axis members.
    // ------------------------------------------------------------------

    @Test
    public void bedrockAxis_primaryHits_areTheTwoOffworldRowMaterials() {
        Set<String> tHits = primaryHitsByInternalName(bedrockAxis().keySet());
        Set<String> tExpected = Set.of(
                "Dolamide@" + MT.MD.MO.mID,          // :2608, bedrock table :767 (offworld)
                "Adamantine@" + MT.MD.MET.mID);      // :2689, bedrock table :768 (offworld)
        assertEquals(tExpected, tHits, "bedrock-axis PRIMARY face is exactly Dolamide + Adamantine (both offworld rows)");
    }

    // ------------------------------------------------------------------
    // Face 3 ruling: the worldgen-ROW face mirrors the block-axis five of the
    // small-ore universe — Zircon/Nd (B1) have no small-ore rows, so the row
    // face stays five; dolamide's row carries no vanilla dim, so the live
    // placements are 4.
    // ------------------------------------------------------------------

    @Test
    public void worldgenRows_primaryHits_mirrorTheBlockAxisFive_withFourLivePlacements() {
        Map<String, OreDictMaterial> tRows = rowMaterials();
        Set<String> tPrimaryNames = primaryRows().stream().map(e -> e.getValue().mNameInternal).collect(Collectors.toSet());
        Set<String> tHits = tRows.entrySet().stream().filter(e -> tPrimaryNames.contains(e.getValue().mNameInternal))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        assertEquals(Set.of("eudialyte", "azurite", "fluorite", "jade", "dolamide"), tHits,
                "the row face's PRIMARY rows are exactly the block-axis five (always-on rows; pool rows carry no PRIMARY beyond these)");

        GTOreWorldgen.SmallOreRow tDolamide = GTOreWorldgen.ROWS.stream()
                .filter(r -> r.tail().equals("dolamide")).findFirst().orElse(null);
        assertNotNull(tDolamide, "the dolamide row exists");
        assertTrue(tDolamide.dims().isEmpty(), "dolamide :843 is asteroids/planets-only upstream — no vanilla dim, no placement");

        long tLivePlacements = GTOreWorldgen.placementPairs().stream()
                .filter(p -> tPrimaryNames.contains(GTOreWorldgen.resolve(p.row()).mNameInternal)).count();
        assertEquals(4, tLivePlacements, "azurite/eudialyte/fluorite/jade = four live overworld placements");
    }

    // ------------------------------------------------------------------
    // Face 4a ruling: the lens face has ZERO PRIMARY rows; the spec's HBM
    // Columbite triple-intersect candidate is COMMON_SECONDARY (stays).
    // ------------------------------------------------------------------

    @Test
    public void lensFace_zeroPrimary_andTheHbmColumbiteTripleIntersectionIsSecondary() {
        Set<String> tPrimaryNames = primaryRows().stream().map(e -> e.getValue().mNameInternal).collect(Collectors.toSet());
        long tPrimaryLensRows = lensMaterials().values().stream().filter(m -> tPrimaryNames.contains(m.mNameInternal)).count();
        assertEquals(0, tPrimaryLensRows, "the lens companion table carries no atlas-PRIMARY row");

        // The triple intersection: STONE_LAYER_ORES member + granite_red lens row + atlas hbm row.
        OreDictMaterial tColumbite = MT.OREMATS.Columbite;
        assertTrue(blockAxis().containsKey(tColumbite.mNameInternal), "Columbite is a STONE_LAYER_ORES axis member");
        assertTrue(lensMaterials().containsKey("granite_red/" + tColumbite.mNameInternal),
                "the granite_red tantalite/columbite/coltan HBM arm rows ship (Loader_Worldgen.java:362-364)");
        Row tAtlasRow = GT6ForeignMaterialAtlas.rows().stream()
                .filter(r -> r.material().get() == tColumbite && r.domain().equals(MT.MD.HBM.mID)).findFirst().orElse(null);
        assertNotNull(tAtlasRow, "Columbite carries the atlas hbm row");
        assertEquals(AttributionKind.COMMON_SECONDARY, tAtlasRow.kind(),
                "upstream :2398 COMMON_ORE — the triple intersection is SECONDARY, never hidden, no takeover");
    }

    // ------------------------------------------------------------------
    // Face 4b ruling: vein/bedrock tables carry PRIMARY slots only in dead
    // or offworld positions; the one live PRIMARY slot (Azurite in the
    // lapis vein) is consumer-guarded, not axis-gated.
    // ------------------------------------------------------------------

    @Test
    public void veinAndBedrockTables_primarySlotsStayDeadOrOffworld() {
        Set<String> tPrimaryNames = primaryRows().stream().map(e -> e.getValue().mNameInternal).collect(Collectors.toSet());
        Map<String, OreDictMaterial> tAxis = blockAxis();

        Set<String> tPrimaryVeinRows = GT6WorldgenDatagen.LARGE_VEIN_TABLE.stream()
                .filter(v -> veinSlots(v).stream().anyMatch(m -> m != null && tPrimaryNames.contains(m.mNameInternal)))
                .map(GTVeinConfig::name).collect(Collectors.toSet());
        assertEquals(Set.of("ore.large.apatite", "ore.large.lapis", "ore.large.monazite",
                "ore.large.titanium", "ore.large.adamantium", "ore.large.dolamide"), tPrimaryVeinRows,
                "six vein rows carry PRIMARY slots: apatite+phosphorusBlue (FR), azurite (TROPIC), nd (HBM), zircon (TROPIC), adamantine (MET), dolamide (MO)");

        // The only LIVE primary slot: an overworld row whose slot material sits on the block axis.
        List<String> tLivePrimarySlots = new ArrayList<>();
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            if (!tVein.overworld()) continue;
            for (OreDictMaterial tSlot : veinSlots(tVein)) {
                if (tSlot != null && tPrimaryNames.contains(tSlot.mNameInternal) && tAxis.containsKey(tSlot.mNameInternal)) {
                    tLivePrimarySlots.add(tVein.name() + "/" + tSlot.mNameInternal);
                }
            }
        }
        assertEquals(List.of("ore.large.lapis/Azurite", "ore.large.monazite/Neodymium", "ore.large.titanium/Zircon"),
                tLivePrimarySlots,
                "three live PRIMARY slots since worldgen-edge-ores-b1 (Azurite pre-existing + the B1-admitted monazite/titanium rows; consumer-guarded; dead slots skip per GTVeinConfig validity)");

        Set<String> tPrimaryBedrockRows = GT6WorldgenDatagen.BEDROCK_ORE_TABLE.stream()
                .filter(b -> tPrimaryNames.contains(b.material().mNameInternal))
                .map(b -> b.name() + (b.overworld() ? "" : "~offworld")).collect(Collectors.toSet());
        assertEquals(Set.of("ore.bedrock.dolamide~offworld", "ore.bedrock.adamantine~offworld"), tPrimaryBedrockRows,
                "bedrock-table PRIMARY rows are exactly dolamide/adamantine, both offworld (:767/:768)");
    }

    // ------------------------------------------------------------------
    // The compat-pool declaration: the mod-gated :854-874 rows' PRIMARY
    // materials stay OUT of every port face; HEX stays cut; nikolite stays
    // a COMMON_SECONDARY rider on its always-on faces.
    // ------------------------------------------------------------------

    @Test
    public void compatPoolStaysOutOfEveryFace_andNikoliteStaysSecondary() {
        Set<String> tPool = Set.of(
                "BlackQuartz",    // AA :2358, the :854-874 pool
                "InfusedVis",     // TC :2441, the :856-866 pool block
                "DarkThaumium",   // TCTE :2452
                "SiC");           // IHL :2226
        Set<String> tAllFaces = new HashSet<>();
        tAllFaces.addAll(blockAxis().keySet());
        tAllFaces.addAll(bedrockAxis().keySet());
        tAllFaces.addAll(rowMaterials().values().stream().map(m -> m.mNameInternal).collect(Collectors.toSet()));
        tAllFaces.addAll(lensMaterials().values().stream().map(m -> m.mNameInternal).collect(Collectors.toSet()));
        for (GTVeinConfig tVein : GT6WorldgenDatagen.LARGE_VEIN_TABLE) {
            for (OreDictMaterial tSlot : veinSlots(tVein)) {
                if (tSlot != null) tAllFaces.add(tSlot.mNameInternal);
            }
        }
        for (GTBedrockOreConfig tBedrock : GT6WorldgenDatagen.BEDROCK_ORE_TABLE) tAllFaces.add(tBedrock.material().mNameInternal);
        for (String tMaterial : tPool) {
            assertFalse(tAllFaces.contains(tMaterial), "compat-pool PRIMARY stays out of every face: " + tMaterial);
        }

        // HEX stays cut: no Hexorium material on the bedrock face it once rode.
        assertTrue(bedrockAxis().keySet().stream().noneMatch(n -> n.contains("Hexorium")), "the MD.HEX hexorium row cut holds (axis)");
        assertTrue(GT6WorldgenDatagen.BEDROCK_ORE_TABLE.stream().noneMatch(b -> b.material().mNameInternal.contains("Hexorium")),
                "the MD.HEX hexorium row cut holds (bedrock table)");

        // nikolite: RP secondary attribution over an always-on row + axis membership.
        Row tNikolite = GT6ForeignMaterialAtlas.rows().stream()
                .filter(r -> r.material().get() == MT.Nikolite).findFirst().orElse(null);
        assertNotNull(tNikolite, "nikolite carries the atlas RP row");
        assertEquals(AttributionKind.COMMON_SECONDARY, tNikolite.kind(), "nikolite = RP COMMON_ORE :2654 — secondary, stays");
        assertTrue(blockAxis().containsKey("Nikolite"), "nikolite rides the axis (:875 !mHidden row)");
        assertTrue(rowMaterials().containsKey("nikolite"), "nikolite rides the worldgen row face (:875)");
    }

    // ------------------------------------------------------------------
    // The javadoc census interlinks (acceptance: grep-pinnable) — each of
    // the four face classes must name this census and the decisions key.
    // ------------------------------------------------------------------

    @Test
    public void javadocCensusCrossrefs_existInAllFourFaceClasses() throws IOException {
        for (String tSource : List.of(
                "src/main/java/gregtech6/registry/GT6OreBlocks.java",
                "src/main/java/gregtech6/registry/GT6BedrockOreBlocks.java",
                "src/main/java/gregtech6/worldgen/GTOreWorldgen.java",
                "src/main/java/gregtech6/datagen/GT6WorldgenDatagen.java")) {
            String tText = Files.readString(mdkRoot().resolve(tSource));
            assertTrue(tText.contains("mdh-5 axis-takeover census"), "census ruling paragraph missing: " + tSource);
            assertTrue(tText.contains("GT6AxisTakeoverCensusTest"), "census test interlink missing: " + tSource);
            assertTrue(tText.contains("decisions.mdh-5-axis-rulings"), "decisions-key interlink missing: " + tSource);
        }
    }

    /** Climb from the working directory to the mdk root (the GTEntityBlockRenderShapeCensusTest shape). */
    private static Path mdkRoot() {
        Path tDir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
            if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/registry/GT6OreBlocks.java"))) return tDir;
        }
        throw new IllegalStateException("mdk root not found above " + Paths.get("").toAbsolutePath());
    }
}
