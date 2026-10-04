/**
 * Tests for task w6-small-ore-datagen: the small-ore table + the (row, dim)
 * placement pairs — the acceptance's offline parity unit. The table grew 54 → 115
 * rows and the pairs 91 → 152 with b-gem-pool-extension (the RANDOM_SMALL_GEM_ORE
 * pool loop joined), and 115 → 125 rows / 152 → 162 pairs with
 * worldgen-edge-ores-b2-orphans (the 10 StoneLayer boundary-blob translations,
 * :481-571 — see {@link #edgeOrphanRowsArePinned}).
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced):
 * <ul>
 * <li>Loader_Worldgen.java:800-852 + :875 — the 54 always-on WorldgenOresSmall rows,
 *     ctor order (name, minY, maxY, amount, material) + the row's GEN_* vanilla-dim
 *     projection; every row below cites its upstream line.</li>
 * <li>Loader_Worldgen.java:877-878 + CS.java:965 — the 61 RANDOM_SMALL_GEM_ORE pool
 *     rows (b-gem-pool-extension): the loop carries no axis filter, one row per
 *     flagged material, all (T, 5, 250, 1, GEN_GEMS); GEN_GEMS projects to OVERWORLD
 *     only (the other eight domains have no modern carrier).</li>
 * <li>WorldgenOresSmall.java:61 — the per-chunk count; the declared constant deviation
 *     pins max(1, amount/2) veins per chunk (the range lower bound; see the
 *     GTOreWorldgen.veinCount deviation note for the cross-leg dispatch evidence).</li>
 * <li>WD.java:765-780 — setSmallOre host face: vanilla granite/diorite/andesite rows
 *     (the modern-complement fix, ahead of the tag) + stone/deepslate tags + 17 GT stones +
 *     gravel/sand (overworld), netherrack (nether), endstone (end); redsand/mud are
 *     NOT upstream small-ore hosts.</li>
 * <li>GT6OreBlocks.java:328-332 — the ancientdebris PLACEMENT-time gate (vanilla
 *     1.20.1 ships ancient debris → the row stays in the table, generates nothing).</li>
 * <li>Coordinator ruling 2026-09-17 — the task card's "63 placement rows" was an
 *     architect arithmetic slip; the verbatim flag walk is 91 = 38 + 20 + 33 over the
 *     54-row table (the 21-table-nether-rows − 1 gated); 152 = 99 + 20 + 33 over the
 *     115-row table since r7-b (the 61 gem rows are overworld-only).</li>
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
     * The 115 rows pinned line-by-line: name → {upstream line, minY, maxY, amount, dims}.
     * Every entry transcribed from its Loader_Worldgen.java row text; the 61 gem-pool rows
     * (b-gem-pool-extension) all cite the :877-878 loop with the loop's literal
     * (T, 5, 250, 1, GEN_GEMS→OVERWORLD) parameters.
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
        Map.entry("ore.small.nikolite"    , "875 10 40 4 OVERWORLD NETHER END"),
        // -- :877-878, the RANDOM_SMALL_GEM_ORE pool loop (b-gem-pool-extension) —
        // 61 rows, all "877 5 250 1 OVERWORLD" (the loop literal; tails = the sanitized
        // internal names). Pinned name-by-name against the upstream MT.java flag walk.
        Map.entry("ore.small.sapphire"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.ruby"            , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.bluesapphire"    , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.greensapphire"   , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.purplesapphire"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.yellowsapphire"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.orangesapphire"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.emerald"         , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.aquamarine"      , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.morganite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.heliodor"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.goshenite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.bixbite"         , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.maxixe"          , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.almandine"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.grossular"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.pyrope"          , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.spessartine"     , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.andradite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.uvarovite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.redjasper"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.oceanjasper"     , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.rainforestjasper", "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.bluejasper"      , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.greenjasper"     , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.yellowjasper"    , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.tigereye"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.catseye"         , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.dragoneye"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.hawkseye"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.blackeye"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.tigeriron"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.greenaventurine" , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.brownaventurine" , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.yellowaventurine", "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.blackaventurine" , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.blueaventurine"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.redaventurine"   , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.fluorite"        , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.redfluorite"     , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.pinkfluorite"    , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.bluefluorite"    , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.greenfluorite"   , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.blackfluorite"   , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.whitefluorite"   , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.yellowfluorite"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.orangefluorite"  , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.magentafluorite" , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.spinel"          , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.balasruby"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.topaz"           , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.bluetopaz"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.tanzanite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.amazonite"       , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.opal"            , "877 5 250 1 OVERWORLD"),
        Map.entry("ore.small.onyxred"         , "877 5 250 1 OVERWORLD"),
          Map.entry("ore.small.onyxblack"       , "877 5 250 1 OVERWORLD"),
          Map.entry("ore.small.peridot"         , "877 5 250 1 OVERWORLD"),
          Map.entry("ore.small.amethyst"        , "877 5 250 1 OVERWORLD"),
          Map.entry("ore.small.dioptase"        , "877 5 250 1 OVERWORLD"),
          Map.entry("ore.small.jade"            , "877 5 250 1 OVERWORLD"),
          // -- :481-571, the StoneLayer boundary blobs (worldgen-edge-ores-b2-orphans) —
          // 10 rows, each citing its bothsides/topbottom call line; amount 1 + OVERWORLD
          // (the declared deviations on edgeOrphanRowsArePinned).
          Map.entry("ore.small.dominicanamber"  , "484 30 70 1 OVERWORLD"),
          Map.entry("ore.small.perlite"         , "497 0 16 1 OVERWORLD"),
          Map.entry("ore.small.diatomite"       , "508 16 64 1 OVERWORLD"),
          Map.entry("ore.small.alunite"         , "530 32 80 1 OVERWORLD"),
          Map.entry("ore.small.mirabilite"      , "533 16 64 1 OVERWORLD"),
          Map.entry("ore.small.trona"           , "534 16 64 1 OVERWORLD"),
          Map.entry("ore.small.vermiculite"     , "548 48 80 1 OVERWORLD"),
          Map.entry("ore.small.mica"            , "552 16 48 1 OVERWORLD"),
          Map.entry("ore.small.biotite"         , "553 16 48 1 OVERWORLD"),
          Map.entry("ore.small.pinkdiamond"     , "563 0 32 1 OVERWORLD"));

    private static String dimsOf(GTOreWorldgen.SmallOreRow aRow) {
        StringBuilder r = new StringBuilder();
        for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
            if (aRow.dims().contains(tDim)) r.append(' ').append(tDim.name());
        }
        return r.isEmpty() ? "-" : r.substring(1);
    }

    /** The 125-row table, pinned row-by-row against the Loader_Worldgen.java transcriptions. */
    @Test
    void rowTableIsPinned() {
        assertEquals(125, GTOreWorldgen.ROWS.size(), "53 always-on rows (:800-852) + nikolite (:875) + the 61 gem-pool rows (:877-878) + the 10 boundary rows (:481-571)");
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
            assertTrue(tLine >= 481 && tLine <= 878, tRow.name() + " cites the Loader_Worldgen.java row range (the :800-878 small rows + the :481-571 boundary rows)");
            assertEquals(tMinY, tRow.minY(), tRow.name() + " minY (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tMaxY, tRow.maxY(), tRow.name() + " maxY (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tAmount, tRow.amount(), tRow.name() + " amount (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tDims, dimsOf(tRow), tRow.name() + " dims (Loader_Worldgen.java:" + tLine + ")");
            assertEquals(tRow.name().substring(tRow.name().lastIndexOf('.') + 1), tRow.tail(),
                    tRow.name() + " tail = the config-name tail (the key segment)");
        }
        assertEquals(UPSTREAM_ROWS.size(), tSeen.size(), "every transcription consumed");
    }

    /** The 162 placement pairs = the verbatim GEN-flag walk: 109 overworld + 20 nether + 33 end. */
    @Test
    void placementPairsArePinned() {
        List<GTOreWorldgen.Placement> tPairs = GTOreWorldgen.placementPairs();
        assertEquals(162, tPairs.size(), "109 OW + 20 NETHER + 33 END (the coordinator-ruled verbatim walk, r7-b pool + b2 boundary rows included)");
        assertEquals(109, tPairs.stream().filter(tPair -> tPair.dim() == GTOreWorldgen.Dim.OVERWORLD).count(), "overworld pairs");
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

    /** The WD.setSmallOre host face: 24 overworld targets / 1 nether / 1 end, rule order pinned. */
    @Test
    void hostTargetFacesArePinned() {
        assertEquals(26, GT6OreBlocks.FAMILIES.size(), "precondition: the ore-1 26-family layout");
        assertEquals(9, GTOreWorldgen.GT_STONE_FAMILY_START, "5 three-form + 4 two-form vanilla anchors precede the 17 GT stones");
        OreDictMaterial tCopper = GTOreWorldgen.resolve(rowOf("ore.small.copper"));
        List<String> tOverworld = GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.OVERWORLD);
        assertEquals(24, tOverworld.size(), "the overworld target count (acceptance 24/1/1)");
        // the vanilla three-stone rows precede the tag: #stone_ore_replaceables contains
        // stone/granite/diorite/andesite (vanilla 1.20.1 tags/blocks/stone_ore_replaceables.json:3-6),
        // so a tag-first order would flatten the vanilla rocks onto the stone base — the
        // modern-complement fix (research.issues-r3-ore appendix B)
        assertEquals("ore_small_granite_copper", tOverworld.get(0), "[0] = vanilla granite → the GT granite family");
        assertEquals("ore_small_diorite_copper", tOverworld.get(1), "[1] = vanilla diorite → the GT diorite family");
        assertEquals("ore_small_andesite_copper", tOverworld.get(2), "[2] = vanilla andesite → the GT andesite family");
        assertEquals("ore_small_stone_copper", tOverworld.get(3), "[3] = the stone tag target");
        assertEquals("ore_small_deepslate_copper", tOverworld.get(4), "[4] = the deepslate tag target (the y<0 split rides the host tag)");
        assertEquals("ore_small_blackgranite_copper", tOverworld.get(5), "[5] = the first GT stone (FAMILIES order)");
        assertEquals("ore_small_shale_copper", tOverworld.get(21), "[21] = the last GT stone");
        assertEquals("ore_small_gravel_copper", tOverworld.get(22), "[22] = the gravel fallback (WD.java:774)");
        assertEquals("ore_small_sand_copper", tOverworld.get(23), "[23] = the sand fallback (WD.java:775)");
        // the GT same-name families the vanilla rows bind to exist and are distinct blocks
        // from the vanilla anchors (minecraft:granite vs gt6:granite — the family walk's
        // [10]/[11]/[12] entries are the GT blob anchors, the same ore blocks these rows emit)
        assertEquals("ore_small_granite_copper", tOverworld.get(10), "[10] = the GT granite blob anchor row");
        assertEquals("ore_small_diorite_copper", tOverworld.get(11), "[11] = the GT diorite blob anchor row");
        assertEquals("ore_small_andesite_copper", tOverworld.get(12), "[12] = the GT andesite blob anchor row");
        assertEquals(List.of("ore_small_netherrack_copper"),
                GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.NETHER), "the nether host face");
        assertEquals(List.of("ore_small_endstone_copper"),
                GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.END), "the end host face");
        // every placement row's material yields the 24/1/1 face and stays inside the
        // registered ore_small universe (zero new blocks — the axis membership is the
        // offline face of block existence)
        Set<OreDictMaterial> tAxis = new HashSet<>(GT6OreBlocks.materialAxis());
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.ROWS) {
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tRow);
            assertTrue(tMaterial != null && tAxis.contains(tMaterial),
                    tRow.name() + " resolves into the registered material axis");
            for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
                if (!tRow.dims().contains(tDim)) continue;
                int tExpected = tDim == GTOreWorldgen.Dim.OVERWORLD ? 24 : 1;
                assertEquals(tExpected, GTOreWorldgen.hostPaths(tMaterial, tDim).size(),
                        tRow.name() + "/" + tDim + " target count");
            }
        }
        assertFalse(GTOreWorldgen.hostPaths(tCopper, GTOreWorldgen.Dim.OVERWORLD).contains("ore_small_redsand_copper"),
                "redsand/mud are NOT upstream small-ore hosts (the spec exclusion)");
    }

    /**
     * The 2 small-ore biome-modifier keys (nether/end — the dims with no lens chain),
     * in the leg's biome_modifier registry. The overworld face rides the strata_lenses
     * chain instead (task lens-ore-base-order): a standalone ore_small_overworld
     * modifier's landing order was the uncontracted datapack load order, so the band
     * could run before the lens and ship stone-based ore inside the lens stone.
     */
    @Test
    void oreBiomeModifierKeysArePinned() {
        assertEquals(2, GT6WorldgenDatagen.ORE_BIOME_MODIFIER_KEYS.size(), "nether/end keep their own modifiers");
        List<String> tPaths = GT6WorldgenDatagen.ORE_BIOME_MODIFIER_KEYS.stream()
                .map(tKey -> tKey.location().getPath()).toList();
        assertEquals(List.of("ore_small_nether", "ore_small_end"), tPaths,
                "the key paths follow the Dim order (no overworld row)");
        for (int i = 0; i < 2; i++) {
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

    /**
     * The deep-band mirror band (task c2-deep-band): the 15 selected rows grow a
     * PLACED-only overworld mirror whose band is the upstream band shifted -64 (the
     * GTOreWorldgen.DEEP_SHIFT rule), wholly below y0 — the modern face of the upstream
     * mNoDeep deep-slate layer (WorldgenStoneLayers.java:77/:196).
     */
    @Test
    void deepBandMirrorsArePinned() {
        List<GTOreWorldgen.SmallOreRow> tMirrors = GTOreWorldgen.deepMirrorRows();
        assertEquals(GTOreWorldgen.DEEP_MIRROR_TAILS.size(), tMirrors.size(),
                "every selected tail resolves to exactly one row (no dim-only or tail-only stragglers)");
        assertEquals(GTOreWorldgen.DEEP_PLACED_KEYS.size(), tMirrors.size(), "one deep placed key per mirror row");
        assertEquals(tMirrors.size(), new HashSet<>(GTOreWorldgen.DEEP_PLACED_KEYS).size(),
                "deep placed keys distinct — ore_small_deep never collides the ore_small_overworld face");
        // the pinned band table: tail -> (minY-64, maxY-64), DEEP_MIRROR_TAILS order
        List<String> tExpectedBands = List.of(
            "-34 -4",   // sphalerite   :806 30-60
            "-34 -4",   // smithsonite  :807 30-60
            "-44 -24",  // stibnite     :808 20-40
            "-44 -24",  // silver       :812 20-40
            "-44 -24",  // gold         :813 20-40
            "-44 -24",  // pyrite       :814 20-40
            "-44 -24",  // pyrolusite   :816 20-40
            "-44 -24",  // garnierite   :817 20-40
            "-44 -24",  // pentlandite  :818 20-40
            "-59 -14",  // scheelite    :819 5-50
            "-59 -54",  // diamond      :824 5-10
            "-59 -44",  // redstone     :827 5-20
            "-59 -44",  // redcinnabar  :828 5-20
            "-44 -24",  // lapis        :829 20-40
            "-44 -24"); // eudialyte    :830 20-40
        Set<String> tSeen = new HashSet<>();
        int i = 0;
        for (GTOreWorldgen.SmallOreRow tRow : tMirrors) {
            assertTrue(tSeen.add(tRow.tail()), "duplicate mirror row: " + tRow.tail());
            assertTrue(tRow.dims().contains(GTOreWorldgen.Dim.OVERWORLD), tRow.name() + " mirrors ride the overworld dim");
            assertTrue(tRow.maxY() <= 64, tRow.name() + " mirrors only from the upstream lower half (maxY <= 64)");
            assertEquals(tExpectedBands.get(i), GTOreWorldgen.deepMinY(tRow) + " " + GTOreWorldgen.deepMaxY(tRow),
                    tRow.name() + " deep band = the upstream band shifted -64");
            assertTrue(GTOreWorldgen.deepMaxY(tRow) < 0, tRow.name() + " deep band wholly below y0");
            assertTrue(GTOreWorldgen.deepMinY(tRow) >= -63,
                    tRow.name() + " keeps the OreFeature walk inside the world floor (-64)");
            assertEquals("gt6:ore_small_deep/" + tRow.tail(),
                    GTOreWorldgen.deepPlacedKey(tRow).location().toString(),
                    tRow.name() + " deep placed key face");
            i++;
        }
        // the selection edges: the deep gem/metal anchors in, the declared exclusions out
        assertTrue(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("diamond"), "the deep gem anchor mirrors");
        assertTrue(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("redstone"), "the vanilla-deep-semantics row mirrors");
        assertFalse(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("coal"), "fuels never mirror (the card rule)");
        assertFalse(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("copper"), "upper-half rows (60-120) never mirror");
        assertFalse(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("azurite"), "dye-class minerals never mirror");
        assertFalse(GTOreWorldgen.DEEP_MIRROR_TAILS.contains("graphite"), "carbon/fuel-family never mirrors");
        // the mirror is PLACED-ONLY and references the row's overworld configured feature —
        // whose [4] target is the deepslate tag arm, so the deep band resolves the
        // deepslate family block (the host face is SHARED with the surface pair)
        OreDictMaterial tSilver = GTOreWorldgen.resolve(rowOf("ore.small.silver"));
        assertEquals("ore_small_deepslate_silver",
                GTOreWorldgen.hostPaths(tSilver, GTOreWorldgen.Dim.OVERWORLD).get(4),
                "[4] = the deepslate tag target — the deep mirror's host arm");
    }

    /**
     * The 10 stone-layer EDGE orphan rows (task worldgen-edge-ores-b2-orphans): the
     * boundary materials whose ONLY ungated upstream source is a
     * {@code StoneLayer.bothsides/topbottom} blob (Loader_Worldgen.java:481-571) — no
     * WorldgenOresSmall row, no large-vein slot (research.stonelayer-edge-ores route B).
     * Each gains the material-axis membership AND a small-ore row whose Y band is the
     * boundary call's, VERBATIM; the row tail = the sanitized internal name (the :878
     * convention — "Dominican Amber" → dominicanamber, "Pink Diamond" → pinkdiamond).
     *
     * <p>Declared deviations (the card's "分布面偏差归 strata-mode deferred" ruling):
     * <ul>
     * <li>amount = 1 (the gem-pool row precedent): the blob's chance column (U4..U32 of
     *     U — StoneLayerOres.java:76/:87-95, a 1-in-N roll per BOUNDARY position, not a
     *     density) has no small-ore analogue; the seam-enrichment face is the strata-mode
     *     card's.</li>
     * <li>the biome gates (AmberDominican BIOMES_SHROOM :484, DiamondPink
     *     BIOMES_JUNGLE :563) are not carried — SmallOreRow has no biome face and the
     *     distribution deferral covers them; a biome-gate card would extend the placed
     *     chain (BiomeFilter predicate per row).</li>
     * <li>never deep-mirrored: the blob is a strata-band phenomenon tied to the layer
     *     boundaries, not the c2 lower-column rule — even DiamondPink, whose band
     *     maxes at 32, stays out of {@link GTOreWorldgen#DEEP_MIRROR_TAILS}.</li>
     * </ul>
     */
    @Test
    void edgeOrphanRowsArePinned() {
        // tail -> "line minY maxY amount DIMS" — every band transcribed from its
        // StoneLayer.bothsides/topbottom call in Loader_Worldgen.java:481-571
        Map<String, String> tEdge = Map.of(
            "dominicanamber" , "484 30 70 1 OVERWORLD",  // :481-495 bothsides(Coal|Lignite|Oilshale, Stone) x3, BIOMES_SHROOM
            "perlite"        , "497 0 16 1 OVERWORLD",   // :496-501 bothsides(Komatiite|Gabbro, Basalt) x2
            "diatomite"      , "508 16 64 1 OVERWORLD",  // :506-509 topbottom(Dolomite, Diorite)
            "alunite"        , "530 32 80 1 OVERWORLD",  // :529-531 bothsides(Rhyolite, Quartzite)
            "mirabilite"     , "533 16 64 1 OVERWORLD",  // :532-535 bothsides(Gneiss, Gypsum)
            "trona"          , "534 16 64 1 OVERWORLD",  // :532-535 bothsides(Gneiss, Gypsum)
            "vermiculite"    , "548 48 80 1 OVERWORLD",  // :547-550 bothsides(GraniteRed, Gneiss)
            "mica"           , "552 16 48 1 OVERWORLD",  // :551-554 bothsides(GraniteBlack, Gneiss)
            "biotite"        , "553 16 48 1 OVERWORLD",  // :551-554 bothsides(GraniteBlack, Gneiss)
            "pinkdiamond"    , "563 0 32 1 OVERWORLD");  // :561-565 topbottom(GraniteBlack, Basalt), BIOMES_JUNGLE
        Set<OreDictMaterial> tAxis = new HashSet<>(GT6OreBlocks.materialAxis());
        for (Map.Entry<String, String> tEntry : tEdge.entrySet()) {
            GTOreWorldgen.SmallOreRow tRow = rowOf("ore.small." + tEntry.getKey());
            String[] tParts = tEntry.getValue().split(" ");
            assertEquals(Integer.parseInt(tParts[1]), tRow.minY(), tRow.name() + " minY (Loader_Worldgen.java:" + tParts[0] + ")");
            assertEquals(Integer.parseInt(tParts[2]), tRow.maxY(), tRow.name() + " maxY (Loader_Worldgen.java:" + tParts[0] + ")");
            assertEquals(Integer.parseInt(tParts[3]), tRow.amount(), tRow.name() + " amount = 1 (the gem-pool precedent; the blob chance is strata-mode deferred)");
            assertEquals(tParts[4], dimsOf(tRow), tRow.name() + " overworld-only (the stone layers are an overworld face)");
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tRow);
            assertTrue(tMaterial != null && tAxis.contains(tMaterial),
                    tRow.name() + " material joined the registered axis (route B)");
            assertTrue(GTOreWorldgen.placementPairs().stream().anyMatch(tPair -> tPair.row() == tRow
                            && tPair.dim() == GTOreWorldgen.Dim.OVERWORLD),
                    tRow.name() + " has its overworld placement pair");
            assertFalse(GTOreWorldgen.DEEP_MIRROR_TAILS.contains(tRow.tail()),
                    tRow.name() + " never deep-mirrors (the strata-band ruling)");
        }
        // the two IHL-gated namesakes (:871/:874, MD.IHL) stay OUT — the boundary row IS
        // the ungated natural source of mica/trona on this port (the :854-874 compat pool)
        assertEquals(10, tEdge.size());
    }

    private static GTOreWorldgen.SmallOreRow rowOf(String aName) {
        return GTOreWorldgen.ROWS.stream().filter(tRow -> tRow.name().equals(aName)).findFirst().orElseThrow();
    }
}
