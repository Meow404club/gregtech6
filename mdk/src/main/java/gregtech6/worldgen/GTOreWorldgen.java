package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The small-ore (single-block scatter) worldgen constant table (task p30-w6-small-ore-datagen).
 * Pure data — offline-safe by construction (suppliers + ResourceKey interns only, the
 * {@link GT6OreBlocks#WORLDGEN_ORES} posture); the datagen band (GT6WorldgenDatagen ore
 * band) and the offline parity test both consume this class.
 *
 * <p><b>The 54-row table</b> is the upstream small-ore universe verbatim: the 53
 * always-on {@code WorldgenOresSmall} rows (Loader_Worldgen.java:800-852) + the
 * {@code !mHidden} nikolite row (:875). Per-row fields = upstream ctor order
 * (name, minY, maxY, amount, material) plus the vanilla-dimension projection of the
 * row's GEN_* flag list ({@code dims} — GEN_OVERWORLD/GEN_NETHER/GEN_END; the
 * mod-dimension flags CW2/A97/Erebus/Atum/Aether/Mars/... have no modern carrier and
 * are dropped with the dim-coverage defer). The mod-gated rows (:854-874), the
 * RANDOM_SMALL_GEM loop (:877-880) and the large-vein table (:886-925, the t3 card)
 * stay out — the ore-1 registration axis rulings.
 *
 * <p><b>Placement = one (row, dim) pair each</b> (coordinator ruling 2026-09-17, the
 * verbatim-flag translation — the task card's "63" was an architect arithmetic slip,
 * the flagged walk measures 91): overworld 38 + nether 20 + end 33. The one
 * ancientdebris row (:852) stays in the table with its {@code NETHER} dim but is
 * placement-gated (GT6OreBlocks.java:328-332 口径: the upstream gate
 * {@code !IL.Ancient_Debris.exists()} is a PLACEMENT-time compat check and vanilla
 * 1.20.1 ships ancient debris) — hence 21 table nether rows minus the gate = 20.
 *
 * <p><b>Feature keys</b> {@code ore_small_<dim>/<row-tail>} (spec ⑤): the key segment
 * is the upstream config name's tail ({@code ore.small.redcinnabar} →
 * {@code ore_small_nether/redcinnabar}) — the material snake for 53 of 54 rows, and
 * deliberately the ROW tail for the redcinnabar/cinnabar pair (both rows carry
 * MT.OREMATS.Cinnabar; the nether flag list has them both, so a material-snake key
 * would collide the y5-20/a4 row with the y5-250/a16 row — the row tail keeps all 91
 * pairs distinct and is the upstream-faithful name, the GT6Worldgen.entryPath
 * dot-flattening rule applied to the config name itself).
 *
 * <p><b>Translation</b> (per feature): configured = vanilla {@code Feature.ORE}
 * size={@link #ORE_SIZE} (the upstream attempt places exactly one block —
 * WorldgenOresSmall.java:61; size=1 is mathematically inert, see the constant) over the
 * WD.setSmallOre host face
 * (WD.java:765-780) — overworld rows 21 targets (stone/deepslate tags + the 17 GT
 * stone anchors + gravel/sand fallbacks; redsand/mud are NOT upstream small-ore
 * hosts), nether rows the base_stone_nether tag, end rows end_stone (the
 * OreFeatures.java:49-60 dual-target canon; the y-domain split rides the HOST tags,
 * no per-y feature pairs — the research ruling). Placed chain = Count + InSquare +
 * HeightRange uniform + BiomeFilter with count = UniformInt[max(1, amount/2), amount]
 * (the :61 {@code max(1, mAmount/2 + nextInt(1+mAmount)/2)} value range, mean ~0.75
 * amount) and the upstream [minY, maxY] band — nether rows clamp maxY at 127 (the
 * 1.7.10 nether is 128 tall; a higher band would be dead attempts, distribution
 * unchanged), overworld/end bands fit the modern heights as-is (max 250 < 256/319).
 */
public final class GTOreWorldgen {

    /** The vanilla carrier dimensions of the upstream GEN_* flag lists. */
    public enum Dim {
        OVERWORLD("overworld"), NETHER("nether"), END("end");

        /** The feature-key directory segment ({@code ore_small_<segment>/<tail>}). */
        public final String segment;

        Dim(String aSegment) {
            this.segment = aSegment;
        }
    }

    /**
     * One upstream {@code WorldgenOresSmall} row (Loader_Worldgen.java ctor order
     * name/minY/maxY/amount/material + the vanilla-dim projection of the row's GEN_*
     * flags). The material rides a supplier: this class loads before OP.init() (the
     * GT6OreBlocks.WORLDGEN_ORES lesson).
     */
    public record SmallOreRow(String name, int minY, int maxY, int amount,
            java.util.function.Supplier<OreDictMaterial> material, Set<Dim> dims) {

        /** The upstream config name's tail = the feature-key segment ({@link GTOreWorldgen} javadoc). */
        public String tail() {
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }

    /** The 54 upstream rows, Loader_Worldgen.java order (:800-852 + :875; per-row line cite). */
    public static final List<SmallOreRow> ROWS = List.of(
        // -- :800-819, the shared-list block --------------------------------------------------------
        row("ore.small.copper"      ,  60, 120, 16, () -> MT.Cu                     , Dim.OVERWORLD, Dim.END),              // :800
        row("ore.small.chalcopyrite",  60, 120, 16, () -> MT.OREMATS.Chalcopyrite   , Dim.OVERWORLD, Dim.END),              // :801
        row("ore.small.malachite"   ,  40,  70,  8, () -> MT.OREMATS.Malachite      , Dim.OVERWORLD, Dim.END),              // :802
        row("ore.small.tin"         ,  60, 120, 16, () -> MT.Sn                     , Dim.OVERWORLD, Dim.END),              // :803
        row("ore.small.cassiterite" ,  60, 120, 16, () -> MT.OREMATS.Cassiterite    , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :804
        row("ore.small.zinc"        ,  40,  70,  4, () -> MT.Zn                     , Dim.OVERWORLD, Dim.END),              // :805
        row("ore.small.sphalerite"  ,  30,  60, 12, () -> MT.OREMATS.Sphalerite     , Dim.OVERWORLD, Dim.END),              // :806
        row("ore.small.smithsonite" ,  30,  60,  2, () -> MT.OREMATS.Smithsonite    , Dim.OVERWORLD, Dim.END),              // :807
        row("ore.small.stibnite"    ,  20,  40,  2, () -> MT.OREMATS.Stibnite       , Dim.OVERWORLD, Dim.END),              // :808
        row("ore.small.bismuth"     ,  80, 120,  8, () -> MT.Bi                     , Dim.OVERWORLD, Dim.NETHER),           // :809
        row("ore.small.lead"        ,  40,  80, 16, () -> MT.Pb                     , Dim.OVERWORLD, Dim.END),              // :810
        row("ore.small.galena"      ,  40,  80, 16, () -> MT.OREMATS.Galena         , Dim.OVERWORLD, Dim.END),              // :811
        row("ore.small.silver"      ,  20,  40,  4, () -> MT.Ag                     , Dim.OVERWORLD, Dim.END),              // :812
        row("ore.small.gold"        ,  20,  40,  4, () -> MT.Au                     , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :813
        row("ore.small.pyrite"      ,  20,  40,  4, () -> MT.Pyrite                 , Dim.OVERWORLD, Dim.END),              // :814
        row("ore.small.hematite"    ,  40,  80, 24, () -> MT.Fe2O3                  , Dim.OVERWORLD, Dim.END),              // :815
        row("ore.small.pyrolusite"  ,  20,  40,  4, () -> MT.MnO2                   , Dim.OVERWORLD, Dim.END),              // :816
        row("ore.small.garnierite"  ,  20,  40,  4, () -> MT.OREMATS.Garnierite     , Dim.OVERWORLD, Dim.END),              // :817
        row("ore.small.pentlandite" ,  20,  40,  4, () -> MT.OREMATS.Pentlandite    , Dim.OVERWORLD, Dim.END),              // :818
        row("ore.small.scheelite"   ,   5,  50,  1, () -> MT.OREMATS.Scheelite      , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :819
        // -- :820-835 ------------------------------------------------------------------------------
        row("ore.small.salt"        ,  40,  80,  6, () -> MT.NaCl                   , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :820
        row("ore.small.rocksalt"    ,  40,  80,  6, () -> MT.KCl                    , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :821
        row("ore.small.borax"       ,  10,  40,  4, () -> MT.OREMATS.Borax          , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :822
        row("ore.small.asbestos"    ,  20,  40,  8, () -> MT.Asbestos               , Dim.OVERWORLD, Dim.NETHER),           // :823
        row("ore.small.diamond"     ,   5,  10,  2, () -> MT.Diamond                , Dim.OVERWORLD, Dim.NETHER),           // :824
        row("ore.small.amber"       ,   5,  70,  1, () -> MT.Amber                  , Dim.OVERWORLD),                       // :825
        row("ore.small.craponite"   ,   5, 250,  2, () -> MT.Craponite              , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :826
        row("ore.small.redstone"    ,   5,  20, 16, () -> MT.Redstone               , Dim.OVERWORLD, Dim.NETHER),           // :827
        row("ore.small.redcinnabar" ,   5,  20,  4, () -> MT.OREMATS.Cinnabar       , Dim.OVERWORLD, Dim.NETHER),           // :828
        row("ore.small.lapis"       ,  20,  40,  8, () -> MT.Lapis                  , Dim.OVERWORLD),                       // :829
        row("ore.small.eudialyte"   ,  20,  40,  4, () -> MT.Eudialyte              , Dim.OVERWORLD),                       // :830
        row("ore.small.azurite"     ,  20,  40,  4, () -> MT.Azurite                , Dim.OVERWORLD),                       // :831
        row("ore.small.coal"        ,  40, 100, 36, () -> MT.Coal                   , Dim.OVERWORLD),                       // :832
        row("ore.small.graphite"    ,   5,  10,  2, () -> MT.Graphite               , Dim.OVERWORLD, Dim.NETHER),           // :833
        row("ore.small.pollucite"   ,   1, 250,  1, () -> MT.OREMATS.Pollucite      , Dim.OVERWORLD, Dim.NETHER),           // :834
        row("ore.small.zeolite"     ,   1, 250,  1, () -> MT.OREMATS.Zeolite        , Dim.OVERWORLD, Dim.NETHER),           // :835
        // -- :836-852, the dim-exclusive blocks -----------------------------------------------------
        row("ore.small.coltan"      ,   1, 250,  4, () -> MT.OREMATS.Coltan         , Dim.NETHER, Dim.END),                 // :836
        row("ore.small.platinum"    ,  20,  40,  6, () -> MT.Pt                     , Dim.END),                             // :837
        row("ore.small.iridium"     ,  20,  40,  6, () -> MT.Ir                     , Dim.END),                             // :838
        row("ore.small.sperrylite"  ,  20,  40,  4, () -> MT.OREMATS.Sperrylite     , Dim.END),                             // :839
        row("ore.small.cooperite"   ,  20,  40,  4, () -> MT.OREMATS.Cooperite      , Dim.END),                             // :840
        row("ore.small.naquadah"    ,  10,  80,  6, () -> MT.Nq                     , Dim.END),                             // :841
        row("ore.small.trinium"     ,  10,  80, 12, () -> MT.Ke                     , Dim.END),                             // :842
        row("ore.small.dolamide"    ,   5, 250,  8, () -> MT.Dolamide               ),                                      // :843 (asteroids/planets only)
        row("ore.small.endium"      ,  10,  80, 32, () -> MT.Endium                 , Dim.END),                             // :844
        row("ore.small.sugilite"    ,  10,  80, 16, () -> MT.Sugilite               , Dim.END),                             // :845
        row("ore.small.ambrosium"   ,  30, 120, 64, () -> MT.Ambrosium              ),                                      // :846 (aether only)
        row("ore.small.zanite"      ,  30, 120, 16, () -> MT.Zanite                 ),                                      // :847 (aether only)
        row("ore.small.sulfur"      ,   5,  15,  8, () -> MT.S                      , Dim.OVERWORLD),                       // :848
        row("ore.small.niter"       ,  10, 120, 32, () -> MT.Niter                  , Dim.NETHER),                          // :849
        row("ore.small.efrine"      ,  90, 120,  8, () -> MT.Efrine                 , Dim.NETHER),                          // :850
        row("ore.small.cinnabar"    ,   5, 250, 16, () -> MT.OREMATS.Cinnabar       , Dim.NETHER),                          // :851
        row("ore.small.ancientdebris",  5,  90, 16, () -> MT.AncientDebris          , Dim.NETHER),                          // :852 — placement-gated
        // -- :875, the !mHidden closer --------------------------------------------------------------
        row("ore.small.nikolite"    ,  10,  40,  4, () -> MT.Nikolite               , Dim.OVERWORLD, Dim.NETHER, Dim.END)   // :875
    );

    /** The MT.* supplier behind the row literals (direct field refs, the WORLDGEN_ORES form). */
    private static SmallOreRow row(String aName, int aMinY, int aMaxY, int aAmount, java.util.function.Supplier<OreDictMaterial> aMaterial, Dim... aDims) {
        return new SmallOreRow(aName, aMinY, aMaxY, aAmount, aMaterial, Set.of(aDims));
    }

    /**
     * The placement-time compat gate (the {@link GTOreWorldgen} javadoc): row tails whose
     * upstream row carries a {@code !IL.*.exists()}/mod-presence PLACEMENT gate that is
     * false on this port. Exactly the ancientdebris row (:852 — vanilla 1.20.1 ships
     * ancient debris), the GT6OreBlocks.java:328-332 rationale.
     */
    public static final Set<String> PLACEMENT_GATED = Set.of("ancientdebris");

    /** One (row, dim) feature pair — the placement unit. */
    public record Placement(SmallOreRow row, Dim dim) {}

    /**
     * The 91 placement pairs, ROWS order × {@link Dim} order, minus the
     * {@link #PLACEMENT_GATED} rows: overworld 38 + nether 20 + end 33.
     */
    public static List<Placement> placementPairs() {
        List<Placement> rPairs = new ArrayList<>(91);
        for (SmallOreRow tRow : ROWS) {
            if (PLACEMENT_GATED.contains(tRow.tail())) continue;
            for (Dim tDim : Dim.values()) {
                if (tRow.dims().contains(tDim)) rPairs.add(new Placement(tRow, tDim));
            }
        }
        return rPairs;
    }

    /** The row's resolved registration material (the alias walk of GT6OreBlocks.materialAxis, per row). */
    public static OreDictMaterial resolve(SmallOreRow aRow) {
        OreDictMaterial tMaterial = aRow.material().get();
        if (tMaterial == null || tMaterial.mID < 0) return null;
        return MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
    }

    // ---------------------------------------------------------------- key face

    /** The configured-feature key of a placement pair ({@code gt6:ore_small_<dim>/<tail>}). */
    public static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(SmallOreRow aRow, Dim aDim) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, entryLocation(aRow, aDim));
    }

    /** The placed-feature key of a placement pair (same path as its configured sibling). */
    public static ResourceKey<PlacedFeature> placedKey(SmallOreRow aRow, Dim aDim) {
        return ResourceKey.create(Registries.PLACED_FEATURE, entryLocation(aRow, aDim));
    }

    private static ResourceLocation entryLocation(SmallOreRow aRow, Dim aDim) {
        return ResourceLocation.fromNamespaceAndPath("gt6", "ore_small_" + aDim.segment + "/" + aRow.tail());
    }

    /** The 91 configured keys, placementPairs() order. */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> CONFIGURED_KEYS =
            placementPairs().stream().map(tPair -> configuredKey(tPair.row(), tPair.dim())).toList();

    /** The 91 placed keys, same order (placed[i] hangs off configured[i]). */
    public static final List<ResourceKey<PlacedFeature>> PLACED_KEYS =
            placementPairs().stream().map(tPair -> placedKey(tPair.row(), tPair.dim())).toList();

    // ---------------------------------------------------------------- placement translation

    /**
     * The vanilla OreConfiguration "size". THE CARD SPEC'S size=1 IS MATHEMATICALLY
     * INERT and this card ships size=2 instead — the minimal faithful value, declared
     * deviation (live-run finding 2026-09-17): an OreFeature walk point is always at an
     * INTEGER y (the two walk endpoints are {@code origin.y + nextInt(3) - 2}) with the
     * x offset confined to {@code [0, sin·size/8]}, so for size=1 the walk sphere radius
     * {@code (1 + nextDouble·size/16)/2 ≈ 0.5} can never reach a block CENTER (nearest
     * center distance² ≥ 0.1406 + 0.25 + 0.1406 = 0.531 > r² ≤ 0.282) — a size=1 ore
     * places NOTHING, ever (0/20 live /place + natural origins are integers too, so the
     * whole size=1 band would be dead-letter JSONs). Calibration (world-datapack census,
     * 8 attempts × sizes 2/4/8/16 on a forced stone pad): EVERY size ≥ 2 places EXACTLY
     * ONE block per attempt — the upstream WorldgenOresSmall.java:61 semantic verbatim.
     * 2 is the smallest value clear of the dead zone.
     */
    public static final int ORE_SIZE = 2;

    /** WorldgenOresSmall.java:61 count range lower bound — max(1, amount/2 + rnd(1+amount)/2) >= this. */
    public static int countMin(SmallOreRow aRow) {
        return Math.max(1, aRow.amount() / 2);
    }

    /** WorldgenOresSmall.java:61 count range upper bound — the raw amount (mAmount >= 1 upstream, :47). */
    public static int countMax(SmallOreRow aRow) {
        return aRow.amount();
    }

    /** The 1.7.10 nether is 128 tall (y 0..127) — a higher band would be dead attempts. */
    public static final int NETHER_MAX_Y = 127;

    /**
     * The placed HeightRange upper anchor: the upstream maxY, clamped at 127 for nether
     * rows only (craponite/pollucite/zeolite/cinnabar). Overworld bands fit under the
     * modern 319 ceiling, end bands under 256, as-is.
     */
    public static int placedMaxY(SmallOreRow aRow, Dim aDim) {
        return aDim == Dim.NETHER ? Math.min(aRow.maxY(), NETHER_MAX_Y) : aRow.maxY();
    }

    // ---------------------------------------------------------------- host-target face

    /**
     * The WD.setSmallOre host face per dimension, as the TARGET block paths in RULE order
     * (WD.java:765-780; the paths are the GT6OreBlocks.path small-ore universe, zero new
     * blocks). The datagen binds the RuleTest by position:
     * <ol>
     * <li>overworld [0] = TagMatchTest(#stone_ore_replaceables) → ore_small_stone_&lt;m&gt;</li>
     * <li>overworld [1] = TagMatchTest(#deepslate_ore_replaceables) → ore_small_deepslate_&lt;m&gt;</li>
     * <li>overworld [2..18] = BlockMatchTest(GT stone STONE variant, FAMILIES order)
     *     → ore_small_&lt;snake&gt;_&lt;m&gt;</li>
     * <li>overworld [19]/[20] = BlockMatchTest(gravel/sand) → ore_small_gravel/sand_&lt;m&gt;
     *     (the :774-775 fallbacks; redsand/mud are NOT upstream small-ore hosts)</li>
     * <li>nether = TagMatchTest(#base_stone_nether) → ore_small_netherrack_&lt;m&gt;</li>
     * <li>end = BlockMatchTest(end_stone) → ore_small_endstone_&lt;m&gt;</li>
     * </ol>
     * Overworld = 21 targets, nether/end = 1 (the acceptance target counts 21/1/1).
     */
    public static List<String> hostPaths(OreDictMaterial aMaterial, Dim aDim) {
        if (aDim == Dim.NETHER) return List.of(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("netherrack"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        if (aDim == Dim.END) return List.of(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("endstone"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        List<String> rPaths = new ArrayList<>(21);
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("stone"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("deepslate"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        for (int i = GT_STONE_FAMILY_START; i < GT6OreBlocks.FAMILIES.size(); i++) {
            rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(GT6OreBlocks.FAMILIES.get(i), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        }
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("gravel"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("sand"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        return rPaths;
    }

    /**
     * FAMILIES layout: the first 9 rows are the vanilla anchors (5 three-form + 4
     * two-form dust), the remaining 17 the GT stones in Loader_Rocks order — the
     * GT6OreBlocks.FAMILIES javadoc pin (kept as a constant so the host walk cites it).
     */
    public static final int GT_STONE_FAMILY_START = 9;

    /** The family by snake (the datagen's block-handle lookups ride this). */
    public static GT6OreBlocks.OreFamily oreFamily(String aSnake) {
        for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            if (tFamily.snake().equals(aSnake)) return tFamily;
        }
        throw new IllegalArgumentException("not an ore family snake: " + aSnake);
    }

    private GTOreWorldgen() {
    }
}
