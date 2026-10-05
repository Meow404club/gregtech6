package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.Biome;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The small-ore (single-block scatter) worldgen constant table (task w6-small-ore-datagen).
 * Pure data — offline-safe by construction (suppliers + ResourceKey interns only, the
 * {@link GT6OreBlocks#WORLDGEN_ORES} posture); the datagen band (GT6WorldgenDatagen ore
 * band) and the offline parity test both consume this class.
 *
 * <p><b>The 130-row table</b> is the upstream small-ore universe verbatim: the 53
 * always-on {@code WorldgenOresSmall} rows (Loader_Worldgen.java:800-852) + the
 * {@code !mHidden} nikolite row (:875) + the RANDOM_SMALL_GEM_ORE pool loop (:877-878,
 * one row per flagged material — 61 rows, task b-gem-pool-extension) + the 10
 * StoneLayer boundary-blob translations (:481-571, task worldgen-edge-ores-b2-orphans —
 * the 10 EDGE orphans' only ungated natural source, Y bands verbatim) + the 5
 * stone-layer/lens anchor rows (:84/:486/:491/:532/:901, task worldgen-axis-batch2 —
 * the TF-domain RockOres census's off-axis remainder, anchor bands verbatim).
 * Per-row fields
 * = upstream ctor order
 * (name, minY, maxY, amount, material) plus the vanilla-dimension projection of the
 * row's GEN_* flag list ({@code dims} — GEN_OVERWORLD/GEN_NETHER/GEN_END; the
 * mod-dimension flags CW2/A97/Erebus/Atum/Aether/Mars/... have no modern carrier and
 * are dropped with the dim-coverage defer). The mod-gated rows (:854-874) and the
 * large-vein table (:886-925, the t3 card) stay out — the ore-1 registration axis
 * rulings (the pool loop joined in r7-b: the loop carries no axis filter, so all 61
 * flagged members get rows; its GEN_GEMS domain projects to overworld only,
 * CS.java:965; the boundary blobs joined in worldgen-edge-ores-b2-orphans, the
 * declared deviations on the rows below).
 *
 * <p><b>Placement = one (row, dim) pair each</b> (coordinator ruling 2026-09-17, the
 * verbatim-flag translation): overworld 114 + nether 20 + end 33 = 167. The one
 * ancientdebris row (:852) stays in the table with its {@code NETHER} dim but is
 * placement-gated (GT6OreBlocks.java:328-332 口径: the upstream gate
 * {@code !IL.Ancient_Debris.exists()} is a PLACEMENT-time compat check and vanilla
 * 1.20.1 ships ancient debris) — hence 21 table nether rows minus the gate = 20.
 *
 * <p>mdh-5 axis-takeover census (task mdh-5-block-worldgen-axis, CLOSED — ruling (a),
 * static terminal state): exactly FIVE atlas-PRIMARY materials carry rows here —
 * eudialyte/azurite (tropicraft), fluorite (rotarycraft, CaF2), jade (erebus), dolamide
 * (mo, whose :843 row has no vanilla dim) — all on ALWAYS-ON upstream rows; the 21
 * mod-gated rows (:854-874) stay out as the compat pool. That is four live overworld
 * placements out of the 152 pairs; consumers guard (GT6OreLootTables.java:236 loop-head
 * null-drop), so no dead reference exists and no driver gate is taken on
 * {@link #placementPairs}. Cross-table + per-face rulings: GT6AxisTakeoverCensusTest,
 * decisions.mdh-5-axis-rulings.
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
 * size={@link #ORE_SIZE} (see the constant's dead-zone deviation note) over the
 * WD.setSmallOre host face
 * (WD.java:765-780) — overworld rows 21 targets (stone/deepslate tags + the 17 GT
 * stone anchors + gravel/sand fallbacks; redsand/mud are NOT upstream small-ore
 * hosts), nether rows the base_stone_nether tag, end rows end_stone (the
 * OreFeatures.java:49-60 dual-target canon; the y-domain split rides the HOST tags,
 * no per-y feature pairs — the research ruling). Placed chain = Count + InSquare +
 * HeightRange uniform + BiomeFilter with count = the {@link #veinCount} CONSTANT
 * max(1, amount/2) (the declared cross-leg deviation — density-exact at the mean
 * ~0.75 amount of the upstream :61 {@code max(1, mAmount/2 + nextInt(1+mAmount)/2)}
 * UniformInt range, see the method javadoc) and the upstream [minY, maxY] band —
 * nether rows clamp maxY at 127 (the
 * 1.7.10 nether is 128 tall; a higher band would be dead attempts, distribution
 * unchanged), overworld/end bands fit the modern heights as-is (max 250 < 256/319).
 *
 * <p><b>The deep-band mirror face</b> (task c2-deep-band): a selected subset
 * ({@link #DEEP_MIRROR_TAILS}) grows a SECOND, placed-only overworld placement whose band
 * is the upstream band shifted into the modern deepslate band ({@link #DEEP_SHIFT}) — the
 * upstream mNoDeep deep-slate protection layer semantics (WorldgenStoneLayers.java:77),
 * filling the y&lt;0 content vacuum that the #32 de-vanilla option would open.
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
        row("ore.small.nikolite"    ,  10,  40,  4, () -> MT.Nikolite               , Dim.OVERWORLD, Dim.NETHER, Dim.END),  // :875
        // -- :877-878, the RANDOM_SMALL_GEM_ORE pool loop (task b-gem-pool-extension) ------------
        // the upstream loop walks the whole flagged pool with no axis filter and creates
        // `WorldgenOresSmall("ore.small."+mNameInternal.toLowerCase(), T, 5, 250, 1, tGem,
        // GEN_GEMS)` per member — 61 rows (48 factory members + 13 inline flags), all
        // GEN_OVERWORLD-carried (GEN_GEMS = CS.java:965, the nine-domain list whose other
        // eight carriers have no modern dimension). Row tails = the sanitized internal names
        // (OreDictMaterial.sanitize strips spaces/apostrophes: "Red Jasper" -> "redjasper").
        // Family-grouped below (the upstream iteration is MATERIAL_ARRAY id order — order is
        // consumer-invisible here, the per-row feature keys are distinct either way).
        // sapphire family, 7 (MT.java:1380-1386)
        row("ore.small.sapphire"        ,   5, 250,  1, () -> MT.Sapphire               , Dim.OVERWORLD),                  // :877-878
        row("ore.small.ruby"            ,   5, 250,  1, () -> MT.Ruby                   , Dim.OVERWORLD),
        row("ore.small.bluesapphire"    ,   5, 250,  1, () -> MT.BlueSapphire           , Dim.OVERWORLD),
        row("ore.small.greensapphire"   ,   5, 250,  1, () -> MT.GreenSapphire          , Dim.OVERWORLD),
        row("ore.small.purplesapphire"  ,   5, 250,  1, () -> MT.PurpleSapphire         , Dim.OVERWORLD),
        row("ore.small.yellowsapphire"  ,   5, 250,  1, () -> MT.YellowSapphire         , Dim.OVERWORLD),
        row("ore.small.orangesapphire"  ,   5, 250,  1, () -> MT.OrangeSapphire         , Dim.OVERWORLD),
        // emerald family, 7 (MT.java:1371-1377)
        row("ore.small.emerald"         ,   5, 250,  1, () -> MT.Emerald                , Dim.OVERWORLD),
        row("ore.small.aquamarine"      ,   5, 250,  1, () -> MT.Aquamarine             , Dim.OVERWORLD),
        row("ore.small.morganite"       ,   5, 250,  1, () -> MT.Morganite              , Dim.OVERWORLD),
        row("ore.small.heliodor"        ,   5, 250,  1, () -> MT.Heliodor               , Dim.OVERWORLD),
        row("ore.small.goshenite"       ,   5, 250,  1, () -> MT.Goshenite              , Dim.OVERWORLD),
        row("ore.small.bixbite"         ,   5, 250,  1, () -> MT.Bixbite                , Dim.OVERWORLD),
        row("ore.small.maxixe"          ,   5, 250,  1, () -> MT.Maxixe                 , Dim.OVERWORLD),
        // garnet family, 6 (MT.java:1393-1398)
        row("ore.small.almandine"       ,   5, 250,  1, () -> MT.Almandine              , Dim.OVERWORLD),
        row("ore.small.grossular"       ,   5, 250,  1, () -> MT.Grossular              , Dim.OVERWORLD),
        row("ore.small.pyrope"          ,   5, 250,  1, () -> MT.Pyrope                 , Dim.OVERWORLD),
        row("ore.small.spessartine"     ,   5, 250,  1, () -> MT.Spessartine            , Dim.OVERWORLD),
        row("ore.small.andradite"       ,   5, 250,  1, () -> MT.Andradite              , Dim.OVERWORLD),
        row("ore.small.uvarovite"       ,   5, 250,  1, () -> MT.Uvarovite              , Dim.OVERWORLD),
        // jasper family, 6 (MT.java:1401-1406)
        row("ore.small.redjasper"       ,   5, 250,  1, () -> MT.Jasper                 , Dim.OVERWORLD),
        row("ore.small.oceanjasper"     ,   5, 250,  1, () -> MT.JasperOcean            , Dim.OVERWORLD),
        row("ore.small.rainforestjasper",   5, 250,  1, () -> MT.JasperRainforest       , Dim.OVERWORLD),
        row("ore.small.bluejasper"      ,   5, 250,  1, () -> MT.JasperBlue             , Dim.OVERWORLD),
        row("ore.small.greenjasper"     ,   5, 250,  1, () -> MT.JasperGreen            , Dim.OVERWORLD),
        row("ore.small.yellowjasper"    ,   5, 250,  1, () -> MT.JasperYellow           , Dim.OVERWORLD),
        // tigereye family, 6 (MT.java:1409-1414)
        row("ore.small.tigereye"        ,   5, 250,  1, () -> MT.TigerEyeYellow         , Dim.OVERWORLD),
        row("ore.small.catseye"         ,   5, 250,  1, () -> MT.TigerEyeGreen          , Dim.OVERWORLD),
        row("ore.small.dragoneye"       ,   5, 250,  1, () -> MT.TigerEyeRed            , Dim.OVERWORLD),
        row("ore.small.hawkseye"        ,   5, 250,  1, () -> MT.TigerEyeBlue           , Dim.OVERWORLD),
        row("ore.small.blackeye"        ,   5, 250,  1, () -> MT.TigerEyeBlack          , Dim.OVERWORLD),
        row("ore.small.tigeriron"       ,   5, 250,  1, () -> MT.TigerIron              , Dim.OVERWORLD),
        // aventurine family, 6 (MT.java:1417-1422)
        row("ore.small.greenaventurine" ,   5, 250,  1, () -> MT.AventurineGreen        , Dim.OVERWORLD),
        row("ore.small.brownaventurine" ,   5, 250,  1, () -> MT.AventurineBrown        , Dim.OVERWORLD),
        row("ore.small.yellowaventurine",   5, 250,  1, () -> MT.AventurineYellow       , Dim.OVERWORLD),
        row("ore.small.blackaventurine" ,   5, 250,  1, () -> MT.AventurineBlack        , Dim.OVERWORLD),
        row("ore.small.blueaventurine"  ,   5, 250,  1, () -> MT.AventurineBlue         , Dim.OVERWORLD),
        row("ore.small.redaventurine"   ,   5, 250,  1, () -> MT.AventurineRed          , Dim.OVERWORLD),
        // fluorite family, 10 (MT.java:1109-1118; CaF2 carries the internal name "Fluorite")
        row("ore.small.fluorite"        ,   5, 250,  1, () -> MT.CaF2                   , Dim.OVERWORLD),
        row("ore.small.redfluorite"     ,   5, 250,  1, () -> MT.FluoriteRed            , Dim.OVERWORLD),
        row("ore.small.pinkfluorite"    ,   5, 250,  1, () -> MT.FluoritePink           , Dim.OVERWORLD),
        row("ore.small.bluefluorite"    ,   5, 250,  1, () -> MT.FluoriteBlue           , Dim.OVERWORLD),
        row("ore.small.greenfluorite"   ,   5, 250,  1, () -> MT.FluoriteGreen          , Dim.OVERWORLD),
        row("ore.small.blackfluorite"   ,   5, 250,  1, () -> MT.FluoriteBlack          , Dim.OVERWORLD),
        row("ore.small.whitefluorite"   ,   5, 250,  1, () -> MT.FluoriteWhite          , Dim.OVERWORLD),
        row("ore.small.yellowfluorite"  ,   5, 250,  1, () -> MT.FluoriteYellow         , Dim.OVERWORLD),
        row("ore.small.orangefluorite"  ,   5, 250,  1, () -> MT.FluoriteOrange         , Dim.OVERWORLD),
        row("ore.small.magentafluorite" ,   5, 250,  1, () -> MT.FluoriteMagenta        , Dim.OVERWORLD),
        // inline flags, 13 (MT.java:1425-1443)
        row("ore.small.spinel"          ,   5, 250,  1, () -> MT.Spinel                 , Dim.OVERWORLD),
        row("ore.small.balasruby"       ,   5, 250,  1, () -> MT.BalasRuby              , Dim.OVERWORLD),
        row("ore.small.topaz"           ,   5, 250,  1, () -> MT.Topaz                  , Dim.OVERWORLD),
        row("ore.small.bluetopaz"       ,   5, 250,  1, () -> MT.BlueTopaz              , Dim.OVERWORLD),
        row("ore.small.tanzanite"       ,   5, 250,  1, () -> MT.Tanzanite              , Dim.OVERWORLD),
        row("ore.small.amazonite"       ,   5, 250,  1, () -> MT.Amazonite              , Dim.OVERWORLD),
        row("ore.small.opal"            ,   5, 250,  1, () -> MT.Opal                   , Dim.OVERWORLD),
        row("ore.small.onyxred"         ,   5, 250,  1, () -> MT.OnyxRed                , Dim.OVERWORLD),
        row("ore.small.onyxblack"       ,   5, 250,  1, () -> MT.OnyxBlack              , Dim.OVERWORLD),
        row("ore.small.peridot"         ,   5, 250,  1, () -> MT.Peridot                , Dim.OVERWORLD),
        row("ore.small.amethyst"        ,   5, 250,  1, () -> MT.Amethyst               , Dim.OVERWORLD),
        row("ore.small.dioptase"        ,   5, 250,  1, () -> MT.Dioptase               , Dim.OVERWORLD),
        row("ore.small.jade"            ,   5, 250,  1, () -> MT.Jade                   , Dim.OVERWORLD),
        // -- :481-571, the StoneLayer boundary blobs (task worldgen-edge-ores-b2-orphans,
        // research.stonelayer-edge-ores route B): the 10 EDGE orphans' only ungated
        // natural source, translated as small-ore rows with the VERBATIM Y bands. amount
        // = 1 (the gem-pool precedent — the blob chance column StoneLayerOres.java:76 is
        // a per-boundary-position 1-in-N roll, the seam enrichment is strata-mode
        // deferred); the biome gates (:484 SHROOM / :563 JUNGLE) are not carried
        // (SmallOreRow has no biome face, the distribution deferral covers them); never
        // deep-mirrored (a strata-band phenomenon, not the c2 lower-column rule). The
        // mica/trona namesakes of the IHL-gated :871/:874 rows stay out of the compat
        // pool — the boundary IS their ungated source.
        row("ore.small.dominicanamber"  ,  30,  70,  1, () -> MT.AmberDominican         , Dim.OVERWORLD),         // :481-495 (BIOMES_SHROOM)
        row("ore.small.perlite"         ,   0,  16,  1, () -> MT.OREMATS.Perlite        , Dim.OVERWORLD),         // :496-501
        row("ore.small.diatomite"       ,  16,  64,  1, () -> MT.OREMATS.Diatomite      , Dim.OVERWORLD),         // :506-509
        row("ore.small.alunite"         ,  32,  80,  1, () -> MT.OREMATS.Alunite        , Dim.OVERWORLD),         // :529-531
        row("ore.small.mirabilite"      ,  16,  64,  1, () -> MT.OREMATS.Mirabilite     , Dim.OVERWORLD),         // :532-535
        row("ore.small.trona"           ,  16,  64,  1, () -> MT.OREMATS.Trona          , Dim.OVERWORLD),         // :532-535
        row("ore.small.vermiculite"     ,  48,  80,  1, () -> MT.OREMATS.Vermiculite    , Dim.OVERWORLD),         // :547-550
        row("ore.small.mica"            ,  16,  48,  1, () -> MT.OREMATS.Mica           , Dim.OVERWORLD),         // :551-554
        row("ore.small.biotite"         ,  16,  48,  1, () -> MT.Biotite                , Dim.OVERWORLD),         // :551-554
        row("ore.small.pinkdiamond"     ,   0,  32,  1, () -> MT.DiamondPink            , Dim.OVERWORLD),         // :561-565 (BIOMES_JUNGLE)
        // -- :84/:371-486/:491/:532/:901, the stone-layer/lens anchors (task
        // worldgen-axis-batch2): the TF-domain RockOres census's off-axis remainder.
        // Each band cites ITS anchor call verbatim — Bauxite :84 (the EtFu
        // deepslate-list StoneLayerOres row, own band; EtFu host + BIOMES_PLAINS
        // gates not carried), Lignite :486 / Oilshale :491 / Gypsum :532 (the
        // strata are depth-driven with no band of their own, so the layer's
        // BOUNDARY call's band rides verbatim — the same lines that fed the b2
        // amber/mirabilite/trona rows), MilkyQuartz :901 (the ore.large.quartz lens
        // band; the lens face revives through the axis, this row is the scatter
        // face). amount = 1 and never deep-mirrored, the b2 grammar; the Gypsum
        // tail shares its name with the IHL-gated :872 row (compat pool, the b2
        // mica/trona posture).
        row("ore.small.bauxite"         ,  16,  32,  1, () -> MT.OREMATS.Bauxite        , Dim.OVERWORLD),         // :84
        row("ore.small.lignite"         ,  30,  70,  1, () -> MT.Lignite                , Dim.OVERWORLD),         // :371 stratum, :486 boundary band
        row("ore.small.oilshale"        ,  30,  70,  1, () -> MT.Oilshale               , Dim.OVERWORLD),         // stratum, :491 boundary band
        row("ore.small.gypsum"          ,  16,  64,  1, () -> MT.Gypsum                 , Dim.OVERWORLD),         // stratum host, :532 boundary band
        row("ore.small.milkyquartz"     ,  40,  80,  1, () -> MT.MilkyQuartz            , Dim.OVERWORLD)          // :901 lens band
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
     * The 167 placement pairs, ROWS order × {@link Dim} order, minus the
     * {@link #PLACEMENT_GATED} rows: overworld 114 + nether 20 + end 33.
     */
    public static List<Placement> placementPairs() {
        List<Placement> rPairs = new ArrayList<>(167);
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

    /** The 167 configured keys, placementPairs() order. */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> CONFIGURED_KEYS =
            placementPairs().stream().map(tPair -> configuredKey(tPair.row(), tPair.dim())).toList();

    /** The 167 placed keys, same order (placed[i] hangs off configured[i]). */
    public static final List<ResourceKey<PlacedFeature>> PLACED_KEYS =
            placementPairs().stream().map(tPair -> placedKey(tPair.row(), tPair.dim())).toList();

    // ---------------------------------------------------------------- placement translation

    /**
     * The vanilla OreConfiguration "size". THE CARD SPEC'S size=1 IS MATHEMATICALLY
     * INERT and this card ships size=4 instead — declared deviation, live-calibrated
     * (2026-09-17, coordinator-notified): an OreFeature walk point always sits at an
     * INTEGER y (both walk endpoints are {@code origin.y + nextInt(3) - 2}) with the x
     * offset confined to {@code [0, sin·size/8]}, so the walk sphere
     * ({@code (1 + nextDouble·size/16)/2} radius) can never reach a block center until
     * size >= 3 — sizes 1 and 2 place NOTHING, ever (natural origins are integers too,
     * so dead-letter JSONs; live census: size=1 0/20, size=2 0/8 /place attempts on a
     * forced stone pad, vanilla ore_coal and the p26 marble blob place at the same
     * spot). The fresh-world calibration curve (blocks placed per /place attempt on a
     * re-stoned pad): size=2 0/8; size=3 4/8, mean 0.625; size=4 6/8, mean 1.5;
     * size=5 7/8, mean 3.25; size=6 6/6, mean 5; size=8 6/6, mean 5.5; size=12 6/6,
     * mean 10.3; size=16 6/6, mean 9.8. Upstream semantics = exactly 1 block per
     * attempt (WorldgenOresSmall.java:61), so the per-chunk density under the pinned
     * count chain is ~1.5x upstream at size=4 (0.63x at size=3, ~0 at sizes 1-2) — 4
     * is the nearest-to-faithful reliable point; the granularity gap is inherent to
     * vanilla OreFeature (the exact single-block scatter is the L1 custom-feature
     * card's domain).
     */
    public static final int ORE_SIZE = 4;

    /**
     * The per-chunk vein count of a row, as a CONSTANT = max(1, amount/2) — the lower
     * bound of the upstream :61 value range. DECLARED DEVIATION from the pinned
     * count=UniformInt[max(1, amount/2), amount]: the IntProvider dispatch serializes
     * DIFFERENTLY per leg (1.20.1/DFU 6 wraps the payload in "value", 21.1/DFU 8 is
     * inline — live evidence: the 21.1 world load of the forge-written canonical tree
     * threw "Failed to load registries ... Not a number: {type:uniform, value:{...}}"),
     * and the canonical single-producer tree must stay byte-identical across legs. The
     * constant keeps the density EXACTLY at the upstream MEAN (j_min × 1 block = amount/2
     * per chunk with the size=4 vein mean 1.5 → 0.75·amount, the :61 range's
     * expectation); the chunk-to-chunk variance of j is lost — the datapack count field
     * stays player-editable, and the exact per-attempt scatter is the L1 custom-feature
     * card's domain.
     */
    public static int veinCount(SmallOreRow aRow) {
        return Math.max(1, aRow.amount() / 2);
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
     * <li>overworld [0..2] = BlockMatchTest(vanilla granite/diorite/andesite)
     *     → ore_small_granite/diorite/andesite_&lt;m&gt; (the GT same-name families — the
     *     modern-complement rows, ahead of the tag so the tag does not flatten the vanilla
     *     three-stones onto the stone base; see oreTargets)</li>
     * <li>overworld [3] = TagMatchTest(#stone_ore_replaceables) → ore_small_stone_&lt;m&gt;</li>
     * <li>overworld [4] = TagMatchTest(#deepslate_ore_replaceables) → ore_small_deepslate_&lt;m&gt;</li>
     * <li>overworld [5..21] = BlockMatchTest(GT stone STONE variant, FAMILIES order)
     *     → ore_small_&lt;snake&gt;_&lt;m&gt;</li>
     * <li>overworld [22]/[23] = BlockMatchTest(gravel/sand) → ore_small_gravel/sand_&lt;m&gt;
     *     (the :774-775 fallbacks; redsand/mud are NOT upstream small-ore hosts)</li>
     * <li>nether = TagMatchTest(#base_stone_nether) → ore_small_netherrack_&lt;m&gt;</li>
     * <li>end = BlockMatchTest(end_stone) → ore_small_endstone_&lt;m&gt;</li>
     * </ol>
     * Overworld = 24 targets, nether/end = 1 (the acceptance target counts 24/1/1).
     */
    public static List<String> hostPaths(OreDictMaterial aMaterial, Dim aDim) {
        if (aDim == Dim.NETHER) return List.of(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("netherrack"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        if (aDim == Dim.END) return List.of(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("endstone"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        List<String> rPaths = new ArrayList<>(24);
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("granite"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("diorite"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
        rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(oreFamily("andesite"), GT6OreBlocks.FormKind.SMALL, aMaterial)));
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

    // ---------------------------------------------------------------- deep-band mirror face (task c2-deep-band)

    /**
     * The deep-band mirror shift: a mirrored row generates a SECOND placement whose band
     * is the upstream band translated down 64 ({@code [minY, maxY] -> [minY - 64, maxY - 64]}).
     * The rule is the bedrock-anchored whole-column translation — the 1.7.10 column
     * [0, 128] sits at modern [-64, +64] under the same shift — so a mirrored band keeps
     * its exact thickness and its depth ORDER, and every mirrored band lands WHOLLY in the
     * modern deepslate band (y &lt; 0): the modern face of the upstream mNoDeep deep-slate
     * protection layer (WorldgenStoneLayers.java:77/:196 force DEEPSLATE below y24;
     * Loader_Worldgen.java:61). The selection only mirrors {@code maxY <= 64} rows, so the
     * deepest mirrored upper edge is 64 - 60 = -4 &lt; 0 (pinned by the mirror test).
     */
    public static final int DEEP_SHIFT = 64;

    /**
     * The deep-band mirror selection (task c2-deep-band spec ①), the row TAILS that
     * grow a {@code ore_small_deep} placement. The card rule: overworld rows whose upstream
     * band sits in the upstream LOWER half ({@code maxY <= 64}) AND whose material family
     * is a major METAL ORE or GEM — the depth counterparts of what a 1.7.10 player met in
     * and below the deep slate. NOT mirrored (each exclusion declared): the upper-half
     * rows (maxY &gt; 64: copper/tin/chalcopyrite/zinc-family/malachite/lead/galena/
     * hematite/bismuth — their upstream semantic is the mid/surface column), the fuels
     * (coal/graphite/amber — 1.7.10 semantics keep carbon near the surface), the
     * dye/evaporite/industrial minerals (azurite/borax/asbestos/sulfur/niter/nikolite),
     * the wide-span lottery rows (craponite/pollucite/zeolite) and the nether/end-only
     * rows (the deep band is an overworld deepslate semantic). NETHER/END pairs never
     * mirror even when the row has them (silver/gold/scheelite ride OVERWORLD only here).
     */
    public static final List<String> DEEP_MIRROR_TAILS = List.of(
        "sphalerite", "smithsonite", "stibnite", "silver", "gold", "pyrite",
        "pyrolusite", "garnierite", "pentlandite", "scheelite", "diamond", "redstone",
        "redcinnabar", "lapis", "eudialyte");

    /** The mirror predicate: the tail is selected AND the row generates an overworld pair. */
    public static boolean hasDeepMirror(SmallOreRow aRow) {
        return aRow.dims().contains(Dim.OVERWORLD) && DEEP_MIRROR_TAILS.contains(aRow.tail());
    }

    /** The deep-band mirror rows, ROWS order (15 of the 54). */
    public static List<SmallOreRow> deepMirrorRows() {
        return ROWS.stream().filter(GTOreWorldgen::hasDeepMirror).toList();
    }

    /** The mirrored band's lower anchor ({@link #DEEP_SHIFT} translation). */
    public static int deepMinY(SmallOreRow aRow) {
        return aRow.minY() - DEEP_SHIFT;
    }

    /** The mirrored band's upper anchor (no nether clamp — the mirror never rides NETHER). */
    public static int deepMaxY(SmallOreRow aRow) {
        return aRow.maxY() - DEEP_SHIFT;
    }

    /**
     * The deep placed-feature key {@code gt6:ore_small_deep/&lt;tail&gt;}: the mirror is a
     * PLACED-ONLY face. It references the row's OVERWORLD configured feature verbatim (the
     * 24-target host walk, whose [4] arm is the #deepslate_ore_replaceables tag → the
     * deepslate-family ore block — the host tag picks the deep form below y0, the same
     * OreFeatures.java:49-60 dual-target canon), and the Y-domain split rides the PLACED
     * band ({@link #deepMinY}/{@link #deepMaxY}). "deep" is a key directory, NOT a
     * {@link Dim} — the pairs hang off the OVERWORLD biome modifier.
     */
    public static ResourceKey<PlacedFeature> deepPlacedKey(SmallOreRow aRow) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath("gt6", "ore_small_deep/" + aRow.tail()));
    }

    /** The 15 deep placed keys, deepMirrorRows() order. */
    public static final List<ResourceKey<PlacedFeature>> DEEP_PLACED_KEYS =
            deepMirrorRows().stream().map(GTOreWorldgen::deepPlacedKey).toList();

    // ---------------------------------------------------------------- twilight RockOres band (task twilight-adaptation-pilot)

    /**
     * One upstream twilight {@code WorldgenOresVanilla} row (Loader_Worldgen.java:666-673,
     * the {@code BlocksGT.RockOres} block): the identifying triple only — all eight rows
     * share the {@link WorldgenBlob} columns amount=1/size=50/probability=100/Y16-32 (the
     * ctor bind, WorldgenBlob.java:53-57), pinned as the band constants below. The meta is
     * the RockOres block meta; the material mapping is BlockRockOres.ORE_MATERIALS
     * (BlockRockOres.java:36) — Coal/Lignite/NaCl/KCl/OREMATS.Bauxite/Oilshale/Gypsum/
     * MilkyQuartz for metas 0-7.
     */
    public record TwilightOreRow(String name, int meta, java.util.function.Supplier<OreDictMaterial> material) {

        /** The upstream config name's tail = the feature-key segment (the small-ore band rule). */
        public String tail() {
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }

    /**
     * The 8 RockOres rows, Loader_Worldgen.java:666-673 order, verbatim (the census table —
     * the axis gate below decides which rows EMIT; the whole table ships as data either way,
     * the molybdenum large-vein precedent: an off-axis row waits for its axis extension and
     * lights up then, the JSON never changes).
     */
    public static final List<TwilightOreRow> TWILIGHT_ORE_ROWS = List.of(
        new TwilightOreRow("twilight.ore.anthracite" , 0, () -> MT.Coal               ),  // :666
        new TwilightOreRow("twilight.ore.lignite"    , 1, () -> MT.Lignite            ),  // :667
        new TwilightOreRow("twilight.ore.salt"       , 2, () -> MT.NaCl               ),  // :668
        new TwilightOreRow("twilight.ore.rocksalt"   , 3, () -> MT.KCl                ),  // :669
        new TwilightOreRow("twilight.ore.bauxite"    , 4, () -> MT.OREMATS.Bauxite    ),  // :670
        new TwilightOreRow("twilight.ore.oilshale"   , 5, () -> MT.Oilshale           ),  // :671
        new TwilightOreRow("twilight.ore.gypsum"     , 6, () -> MT.Gypsum             ),  // :672
        new TwilightOreRow("twilight.ore.milkyquartz", 7, () -> MT.MilkyQuartz        )); // :673

    /** The shared WorldgenOresVanilla columns (WorldgenBlob.java:53-57 config binds, all 8 rows identical). */
    public static final int TWILIGHT_ORE_AMOUNT = 1, TWILIGHT_ORE_SIZE = 50,
            TWILIGHT_ORE_PROBABILITY = 100, TWILIGHT_ORE_MIN_Y = 16, TWILIGHT_ORE_MAX_Y = 32;

    /** The Twilight Forest modid — the conditions trigger AND the biome-tag namespace ({@link #twilightBiomeTag}). */
    public static final String TWILIGHT_MODID = "twilightforest";

    /**
     * The tag the twilight modifier hangs off: {@code #twilightforest:in_twilight_forest} —
     * TF's own tag, shipped by TF, never emitted here. The datagen lookup resolves to an
     * EMPTY named holder (RegistrySetBuilder.EmptyTagLookup.get:174-183 — any tag key) that
     * serializes as the {@code #...} string, the {@code #gt6:trees/*} precedent both legs
     * pin; at RUNTIME the row's mod_loaded condition gates the entry BEFORE the tag resolves
     * (forge ICondition.java:24-30 shouldRegisterEntry) — TF absent = zero mounts, zero
     * errors, the TF-absence semantics.
     */
    public static TagKey<Biome> twilightBiomeTag() {
        return TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(TWILIGHT_MODID, "in_twilight_forest"));
    }

    /**
     * The axis gate (the GTVeinConfig validity face over a row): the row emits JSON only
     * when its resolved material has registrable ore blocks — an identity scan over
     * {@link GT6OreBlocks#materialAxis()} (the same canonical instances the resolve walk
     * produces; OreDictMaterial has no equals override, identity IS the comparison).
     */
    public static boolean twilightOnAxis(TwilightOreRow aRow) {
        OreDictMaterial tMaterial = aRow.material().get();
        if (tMaterial == null || tMaterial.mID < 0) return false;
        tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target
        if (tMaterial == null || tMaterial.mID < 0) return false;
        for (OreDictMaterial tAxis : GT6OreBlocks.materialAxis()) {
            if (tAxis == tMaterial) return true;
        }
        return false;
    }

    /** The axis-valid rows in table order — the 3-row emission set (Coal/NaCl/KCl today). */
    public static List<TwilightOreRow> twilightOnAxisRows() {
        List<TwilightOreRow> rRows = new ArrayList<>(3);
        for (TwilightOreRow tRow : TWILIGHT_ORE_ROWS) {
            if (twilightOnAxis(tRow)) rRows.add(tRow);
        }
        return rRows;
    }

    /** The configured-feature key of a twilight row ({@code gt6:twilight_ore/<tail>}). */
    public static ResourceKey<ConfiguredFeature<?, ?>> twilightConfiguredKey(TwilightOreRow aRow) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, twilightEntryLocation(aRow));
    }

    /** The placed-feature key of a twilight row (same path as its configured sibling). */
    public static ResourceKey<PlacedFeature> twilightPlacedKey(TwilightOreRow aRow) {
        return ResourceKey.create(Registries.PLACED_FEATURE, twilightEntryLocation(aRow));
    }

    private static ResourceLocation twilightEntryLocation(TwilightOreRow aRow) {
        return ResourceLocation.fromNamespaceAndPath("gt6", "twilight_ore/" + aRow.tail());
    }

    private GTOreWorldgen() {
    }
}
