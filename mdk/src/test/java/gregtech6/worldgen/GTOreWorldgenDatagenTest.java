/**
 * Tests for task p30-w6-small-ore-datagen: the 54-row small-ore table + the 91
 * (row, dim) placement pairs — the acceptance's offline parity unit.
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_Worldgen.java:800-852 + :875 — the 54 always-on WorldgenOresSmall rows,
 *     ctor order (name, minY, maxY, amount, material) + the row's GEN_* vanilla-dim
 *     projection; every row below cites its upstream line.</li>
 * <li>WorldgenOresSmall.java:61 — the per-chunk count; the declared constant deviation
 *     pins max(1, amount/2) veins per chunk (the range lower bound; see the
 *     GTOreWorldgen.veinCount deviation note for the cross-leg dispatch evidence).</li>
 * <li>WD.java:765-780 — setSmallOre host face: stone/deepslate tags + 17 GT stones +
 *     gravel/sand (overworld), netherrack (nether), endstone (end); redsand/mud are
 *     NOT upstream small-ore hosts.</li>
 * <li>GT6OreBlocks.java:328-332 — the ancientdebris PLACEMENT-time gate (vanilla
 *     1.20.1 ships ancient debris → the row stays in the table, generates nothing).</li>
 * <li>Coordinator ruling 2026-09-17 — the task card's "63 placement rows" was an
 *     architect arithmetic slip; the verbatim flag walk is 91 = 38 + 20 + 33 (the
 *     21-table-nether-rows − 1 gated).</li>
 * </ul>
 *
 * <p>Offline-safe by construction (the GT6WorldgenDatagenTest posture): ResourceKey
 * interns, string paths, resolved materials after {@code initMaterials()} — no
 * RegisterEvent, no BlockState construction, no feature instantiation.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

class GTOreWorldgenDatagenTest {

    @BeforeAll
    static void boot() {
        // the material system must exist before resolve()/materialAxis() dereference
        // (GT6OreBlocksRegistrationTest posture); vanilla bootstrap bracket for the
        // ResourceKey/registry-key classes (offline throwables ignored).
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /**
     * The 54 rows pinned line-by-line: name → {upstream line, minY, maxY, amount, dims}.
     * Every entry transcribed from its Loader_Worldgen.java row text.
     */
    private static Map<String, String> UPSTREAM_ROWS = Map.ofEntries(
        Map.entry("ore.small.copper"      , "800 60 120 16 OVERWORLD END"),
        Map.entry("ore.small.chalcopyrite", "801 60 120 16 OVERWORLD END"),
        Map.entry("ore.small.malachite"   , "802 40 70 8 OVERWORLD END"),
        Map.entry("ore.small.tin"         , "803 60 120 16 OVERWORLD END"),
        Map.entry("ore.small.cassiterite" , "804 60 120 16 OVERWORLD NETHER END"),
        Map.entry("ore.small.zinc"        , "805 40 70 4 OVERWORLD END"),
        Map.entry("ore.small.sphalerite"  , "806 30 60 12 OVERWORLD END"),
        Map.entry("ore.small.smithsonite" , "807 30 60 2 OVERWORLD END"),
        Map.entry("ore.small.stibnite"    , "808 20 40 2 OVERWORLD END"),
        Map.entry("ore.small.bismuth"     , "809 80 120 8 OVERWORLD NETHER"),
        Map.entry("ore.small.lead"        , "810 40 80 16 OVERWORLD END"),
        Map.entry("ore.small.galena"      , "811 40 80 16 OVERWORLD END"),
        Map.entry("ore.small.silver"      , "812 20 40 4 OVERWORLD END"),
        Map.entry("ore.small.gold"        , "813 20 40 4 OVERWORLD NETHER END"),
        Map.entry("ore.small.pyrite"      , "814 20 40 4 OVERWORLD END"),
        Map.entry("ore.small.hematite"    , "815 40 80 24 OVERWORLD END"),
        Map.entry("ore.small.pyrolusite"  , "816 20 40 4 OVERWORLD END"),
        Map.entry("ore.small.garnierite"  , "817 20 40 4 OVERWORLD END"),
        Map.entry("ore.small.pentlandite" , "818 20 40 4 OVERWORLD END"),
        Map.entry("ore.small.scheelite"   , "819 5 50 1 OVERWORLD NETHER END"),
        Map.entry("ore.small.salt"        , "820 40 80 6 OVERWORLD NETHER END"),
        Map.entry("ore.small.rocksalt"    , "821 40 80 6 OVERWORLD NETHER END"),
        Map.entry("ore.small.borax"       , "822 10 40 4 OVERWORLD NETHER END"),
        Map.entry("ore.small.asbestos"    , "823 20 40 8 OVERWORLD NETHER"),
        Map.entry("ore.small.diamond"     , "824 5 10 2 OVERWORLD NETHER"),
        Map.entry("ore.small.amber"       , "825 5 70 1 OVERWORLD"),
        Map.entry("ore.small.craponite"   , "826 5 250 2 OVERWORLD NETHER END"),
        Map.entry("ore.small.redstone"    , "827 5 20 16 OVERWORLD NETHER"),
        Map.entry("ore.small.redcinnabar" , "828 5 20 4 OVERWORLD NETHER"),
        Map.entry("ore.small.lapis"       , "829 20 40 8 OVERWORLD"),
        Map.entry("ore.small.eudialyte"   , "830 20 40 4 OVERWORLD"),
        Map.entry("ore.small.azurite"     , "831 20 40 4 OVERWORLD"),
        Map.entry("ore.small.coal"        , "832 40 100 36 OVERWORLD"),
        Map.entry("ore.small.graphite"    , "833 5 10 2 OVERWORLD NETHER"),
        Map.entry("ore.small.pollucite"   , "834 1 250 1 OVERWORLD NETHER"),
        Map.entry("ore.small.zeolite"     , "835 1 250 1 OVERWORLD NETHER"),
        Map.entry("ore.small.coltan"      , "836 1 250 4 NETHER END"),
        Map.entry("ore.small.platinum"    , "837 20 40 6 END"),
        Map.entry("ore.small.iridium"     , "838 20 40 6 END"),
        Map.entry("ore.small.sperrylite"  , "839 20 40 4 END"),
        Map.entry("ore.small.cooperite"   , "840 20 40 4 END"),
        Map.entry("ore.small.naquadah"    , "841 10 80 6 END"),
        Map.entry("ore.small.trinium"     , "842 10 80 12 END"),
        Map.entry("ore.small.dolamide"    , "843 5 250 8 -"),   // asteroids/planets only
        Map.entry("ore.small.endium"      , "844 10 80 32 END"),
        Map.entry("ore.small.sugilite"    , "845 10 80 16 END"),
        Map.entry("ore.small.ambrosium"   , "846 30 120 64 -"), // aether only
        Map.entry("ore.small.zanite"      , "847 30 120 16 -"), // aether only
        Map.entry("ore.small.sulfur"      , "848 5 15 8 OVERWORLD"),
        Map.entry("ore.small.niter"       , "849 10 120 32 NETHER"),
        Map.entry("ore.small.efrine"      , "850 90 120 8 NETHER"),
        Map.entry("ore.small.cinnabar"    , "851 5 250 16 NETHER"),
        Map.entry("ore.small.ancientdebris", "852 5 90 16 NETHER"), // placement-gated
        Map.entry("ore.small.nikolite"    , "875 10 40 4 OVERWORLD NETHER END"));

    private static String dimsOf(GTOreWorldgen.SmallOreRow aRow) {
        StringBuilder r = new StringBuilder();
        for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
            if (aRow.dims().contains(tDim)) r.append(' ').append(tDim.name());
        }
        return r.isEmpty() ? "-" : r.substring(1);
    }

    /** The 54-row table, pinned row-by-row against the Loader_Worldgen.java transcriptions. */
    @Test
    void rowTableIsPinned() {
        assertEquals(54, GTOreWorldgen.ROWS.size(), "53 always-on rows (:800-852) + nikolite (:875)");
        Set<String> tSeen = new HashSet<>();
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            String tUpstream = UPSTREAM_ROWS.get(tRow.name());
            assertTrue(tUpstream != null, "unpinned row: " + tRow.name());
            assertTrue(tSeen.add(tRow.name()), "duplicate row name: " + tRow.name());
            String[] tParts = tUpstream.split(" ");
            int tLine = Integer.parseInt(tParts[0]);
            int tMinY = Integer.parseInt(tParts[1]);
            int tMaxY = Integer.parseInt(tParts[2]);
            int tAmount = Integer.parseInt(tParts[3]);
            String tDims = tParts.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(tParts, 4, tParts.length)) : "-";
            assertTrue(tLine >= 800 && tLine <= 875, tRow.name() + " cites the Loader_Worldgen.java row range");
            assertEquals(tMinY, tRow.minY(), tRow.name() + " minY (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tMaxY, tRow.maxY(), tRow.name() + " maxY (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tAmount, tRow.amount(), tRow.name() + " amount (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tDims, dimsOf(tRow), tRow.name() + " dims (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tRow.name().substring(tRow.name().lastIndexOf('.') + 1), tRow.tail(),
                    tRow.name() + " tail = the config-name tail (the key segment)");
        }
        assertEquals(UPSTREAM_ROWS.size(), tSeen.size(), "every transcription consumed");
    }

    /** The 91 placement pairs = the verbatim GEN-flag walk: 38 overworld + 20 nether + 33 end. */
    @Test
    void placementPairsArePinned() {
        List<GTOreWorldgen.Placement> tPairs = GTOreWorldgen.placementPairs();
        assertEquals(91, tPairs.size(), "38 OW + 20 NETHER + 33 END (the coordinator-ruled verbatim walk)");
        assertEquals(38, tPairs.stream().filter(tPair -> tPair.dim() == GTOreWorldgen.Dim.OVERWORLD).count(), "overworld pairs");
        assertEquals(20, tPairs.stream().filter(tPair -> tPair.dim() == GTOreWorldgen.Dim.NETHER).count(), "nether pairs (21 table rows − ancientdebris gate)");
        assertEquals(33, tPairs.stream().filter(tPair -> tPair.dim() == GTOreWorldgen.Dim.END).count(), "end pairs");
        // the gate: the ancientdebris row keeps its NETHER dim in the table but produces no pair
        assertTrue(GTOreWorldgen.PLACEMENT_GATED.contains("ancientdebris"), "the :852 gate face");
        GTOreWorldgen.SmallOreRow tDebris = GTOreWorldgen.ROWS.stream()
                .filter(tRow -> tRow.tail().equals("ancientdebris")).findFirst().orElseThrow();
        assertTrue(tDebris.dims().contains(GTOreWorldgen.Dim.NETHER), "the row stays faithful in the table");
        assertTrue(tPairs.stream().noneMatch(tPair -> tPair.row() == tDebris), "the gate kills every ancientdebris pair");
        // pair order = ROWS x Dim order, keys distinct (the redcinnabar/cinnabar row-tail case)
        assertEquals(GTOreWorldgen.CONFIGURED_KEYS.size(), tPairs.size(), "one configured key per pair");
        assertEquals(GTOreWorldgen.PLACED_KEYS.size(), tPairs.size(), "one placed key per pair");
        assertEquals(GTOreWorldgen.CONFIGURED_KEYS.size(), new HashSet<>(GTOreWorldgen.CONFIGURED_KEYS).size(),
                "configured keys distinct — the row-tail scheme de-collides the Cinnabar pair");
        assertEquals(GTOreWorldgen.PLACED_KEYS.size(), new HashSet<>(GTOreWorldgen.PLACED_KEYS).size(), "placed keys distinct");
    }

    /** The feature-key face: gt6:ore_small_<dim>/<tail>, the redcinnabar/cinnabar row-tail de-collision pinned. */
    @Test
    void keySchemeIsPinned() {
        GTOreWorldgen.SmallOreRow tCopper = rowOf("ore.small.copper");
        assertEquals("gt6:ore_small_overworld/copper",
                GTOreWorldgen.configuredKey(tCopper, GTOreWorldgen.Dim.OVERWORLD).location().toString(),
                "the RCON acceptance key form");
        assertEquals("minecraft:worldgen/configured_feature",
                GTOreWorldgen.configuredKey(tCopper, GTOreWorldgen.Dim.OVERWORLD).registry().toString(),
                "configured keys live in the configured_feature registry");
        assertEquals("gt6:ore_small_nether/copper",
                GTOreWorldgen.placedKey(tCopper, GTOreWorldgen.Dim.NETHER).location().toString(),
                "placed keys share the path in the placed_feature registry");
        // the Cinnabar pair: two rows, one material, two distinct nether keys
        GTOreWorldgen.SmallOreRow tRed = rowOf("ore.small.redcinnabar");
        GTOreWorldgen.SmallOreRow tPlain = rowOf("ore.small.cinnabar");
        assertEquals("gt6:ore_small_nether/redcinnabar",
                GTOreWorldgen.placedKey(tRed, GTOreWorldgen.Dim.NETHER).location().toString(),
                "the :828 row keeps its own tail");
        assertEquals("gt6:ore_small_nether/cinnabar",
                GTOreWorldgen.placedKey(tPlain, GTOreWorldgen.Dim.NETHER).location().toString(),
                "the :851 row keeps its own tail — a material-snake scheme would collide these");
        assertEquals(GTOreWorldgen.resolve(tRed), GTOreWorldgen.resolve(tPlain),
                "both rows resolve to the same registration material (the ore-1 collapse)");
    }

    /** The declared count deviation: constant max(1, amount/2) per row (the :61 range lower bound), boundaries pinned. */
    @Test
    void countBoundsArePinned() {
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            assertEquals(Math.max(1, tRow.amount() / 2), GTOreWorldgen.veinCount(tRow),
                    tRow.name() + " veinCount = max(1, amount/2) (the constant-count deviation)");
        }
        assertEquals(1, GTOreWorldgen.veinCount(rowOf("ore.small.scheelite")), "amount 1 clamps to 1");
        assertEquals(18, GTOreWorldgen.veinCount(rowOf("ore.small.coal")), "coal 36 → 18 veins/chunk");
        assertEquals(8, GTOreWorldgen.veinCount(rowOf("ore.small.copper")), "copper 16 → 8 veins/chunk");
    }

    /** The nether y clamp: maxY>127 clamps at 127 for nether pairs only — exactly craponite/pollucite/zeolite/cinnabar. */
    @Test
    void yClampIsPinned() {
        List<String> tClamped = GTOreWorldgen.ROWS.stream()
                .filter(tRow -> tRow.dims().contains(GTOreWorldgen.Dim.NETHER))
                .filter(tRow -> tRow.maxY() > GTOreWorldgen.NETHER_MAX_Y)
                .map(GTOreWorldgen.SmallOreRow::tail).toList();
        assertEquals(List.of("craponite", "pollucite", "zeolite", "coltan", "cinnabar"), tClamped,
                "the declared clamp rows (the :826/:834/:835/:836/:851 band-250s; ancientdebris maxY 90 needs none)");
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
                int tExpected = tDim == GTOreWorldgen.Dim.NETHER
                        ? Math.min(tRow.maxY(), GTOreWorldgen.NETHER_MAX_Y) : tRow.maxY();
                assertEquals(tExpected, GTOreWorldgen.placedMaxY(tRow, tDim),
                        tRow.name() + "/" + tDim + " height anchor");
            }
        }
        assertEquals(250, GTOreWorldgen.placedMaxY(rowOf("ore.small.craponite"), GTOreWorldgen.Dim.END),
                "end bands fit the 256-tall end as-is — overworld/end rows never clamp");
    }

    /** The WD.setSmallOre host face: 21 overworld targets / 1 nether / 1 end, rule order pinned. */
    @Test
    void hostTargetFacesArePinned() {
        assertEquals(26, GT6OreBlocks.FAMILIES.size(), "precondition: the ore-1 26-family layout");
        assertEquals(9, GTOreWorldgen.GT_STONE_FAMILY_START, "5 three-form + 4 two-form vanilla anchors precede the 17 GT stones");
        OreDictMaterial tCopper = GTOreWorldgen.resolve(rowOf("ore.small.copper"));
        List<String> tOverworld = GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.OVERWORLD);
        assertEquals(21, tOverworld.size(), "the overworld target count (acceptance 21/1/1)");
        assertEquals("ore_small_stone_copper", tOverworld.get(0), "[0] = the stone tag target");
        assertEquals("ore_small_deepslate_copper", tOverworld.get(1), "[1] = the deepslate tag target (the y<0 split rides the host tag)");
        assertEquals("ore_small_blackgranite_copper", tOverworld.get(2), "[2] = the first GT stone (FAMILIES order)");
        assertEquals("ore_small_shale_copper", tOverworld.get(18), "[18] = the last GT stone");
        assertEquals("ore_small_gravel_copper", tOverworld.get(19), "[19] = the gravel fallback (WD.java:774)");
        assertEquals("ore_small_sand_copper", tOverworld.get(20), "[20] = the sand fallback (WD.java:775)");
        assertEquals(List.of("ore_small_netherrack_copper"),
                GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.NETHER), "the nether host face");
        assertEquals(List.of("ore_small_endstone_copper"),
                GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.END), "the end host face");
        // every placement row's material yields the 21/1/1 face and stays inside the
        // registered ore_small universe (zero new blocks — the axis membership is the
        // offline face of block existence)
        Set<OreDictMaterial> tAxis = new HashSet<>(GT6OreBlocks.materialAxis());
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tRow);
            assertTrue(tMaterial != null && tAxis.contains(tMaterial),
                    tRow.name() + " resolves into the registered material axis");
            for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
                if (!tRow.dims().contains(tDim)) continue;
                int tExpected = tDim == GTOreWorldgen.Dim.OVERWORLD ? 21 : 1;
                assertEquals(tExpected, GTOreWorldgen.hostPaths(tMaterial, tDim).size(),
                        tRow.name() + "/" + tDim + " target count");
            }
        }
        assertFalse(GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.OVERWORLD).contains("ore_small_redsand_copper"),
                "redsand/mud are NOT upstream small-ore hosts (the spec exclusion)");
    }

    /** The 3 small-ore biome-modifier keys, Dim order, in the leg's biome_modifier registry. */
    @Test
    void oreBiomeModifierKeysArePinned() {
        assertEquals(3, GT6WorldgenDatagen.ORE_BIOME_MODIFIER_KEYS.size(), "one per vanilla dimension tag");
        List<String> tPaths = GT6WorldgenDatagen.ORE_BIOME_MODIFIER_KEYS.stream()
                .map(tKey -> tKey.location().getPath()).toList();
        assertEquals(List.of("ore_small_overworld", "ore_small_nether", "ore_small_end"), tPaths,
                "the key paths follow the Dim order");
        for (int i = 0; i < 3; i++) {
            assertEquals(GT6WorldgenDatagen.biomeModifierRegistryKey().location().toString(),
                    GT6WorldgenDatagen.ORE_BIOME_MODIFIER_KEYS.get(i).registry().toString(),
                    "modifier key " + i + " lives in the leg's biome_modifier registry");
        }
    }

    /** The declared ORE_SIZE=4 deviation: sizes 1-2 are mathematically inert (the dead-zone). */
    @Test
    void oreSizeDeviationIsPinned() {
        assertEquals(4, GTOreWorldgen.ORE_SIZE, "size=4 — the nearest-to-faithful reliable point of the live curve");
        // the dead-zone arithmetic, pinned: the walk point sits at an integer y (both
        // endpoints y + nextInt(3) - 2) with x within [0, sin·size/8], so small sizes'
        // sphere radius (1 + u·size/16)/2 can never reach a block center — nearest center
        // distance² >= 0.1406(x) + 0.25(y) + 0.1406(z) = 0.531, while size=1 r² <= 0.282
        // and size=2 r² <= 0.316. Live census (fresh world, forced stone pad, raw):
        // size=1 0/20; size=2 0/8; size=3 4/8 mean .625; size=4 6/8 mean 1.5; size=5 7/8
        // mean 3.25; size=6 6/6 mean 5; size=8 6/6 mean 5.5; size=12 6/6 mean 10.3;
        // size=16 6/6 mean 9.8 — per-chunk density ~1.5x upstream at size=4 (declared).
    }

    private static GTOreWorldgen.SmallOreRow rowOf(String aName) {
        return GTOreWorldgen.ROWS.stream().filter(tRow -> tRow.name().equals(aName)).findFirst().orElseThrow();
    }
}
