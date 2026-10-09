package gregtech6.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.block.ore.GTOreFallingBlock;
import gregtech6.item.GTMaterialPrefixBlockItem;

/**
 * Registration home of the GT6 ore BLOCK universe (task ore-1-mech, rulings in
 * decisions.p30-ore-rulings): the <b>26 stone families</b> (9 vanilla-anchored + the 17
 * GT stones) x the material axis M x the family's forms, as per-pair Block+BlockItem
 * registrations (ADR-P8 ④ per-pair precedent, the GTStoneBlocks/GTMaterialBlocks shape)
 * plus the one upstream-visible creative tab.
 *
 * <p><b>The 26 families</b> (decisions.p30-ore-rulings.ore-families, verbatim rows):
 * <ul>
 * <li>5 three-form vanilla anchors — stone (Loader_Ores.java:61/:56/:69), deepslate
 *     (OP.oreDeepslate, the modern first-classing: upstream has no GT deepslate block,
 *     parameters = the EtFu rockset default row Loader_Ores.java:212 + :439 — 1.0/1.0/h0,
 *     broken = half, Loader_Ores.java:474), netherrack (:63/:58/:71), endstone
 *     (:64/:59/:72, the ender-proof + XP 2-3 row :80), sandstone (:62/:57/:70);</li>
 * <li>4 two-form broken≡normal dust families — gravel (:65/:73), sand (:66/:74), redsand
 *     (:67/:75), mud (:121-122, the Diggables→vanilla mud deviation declared by the
 *     ruling); no separate broken block — stoneToBrokenOres maps the same block;</li>
 * <li>17 GT stones x 3 forms in Loader_Rocks.java:57-139 order (granite_black..shale),
 *     each broken row = hardness/resistance halved, level offset -1 with minimum
 *     level-1 (clamped at 0, PrefixBlock.java:176), gravity true.</li>
 * </ul>
 * That is <b>74 form-rows per material</b> (22x3 + 4x2), the architect's pinned
 * enumeration (tasks.p30-arch-ore-registration.enumeration.block_rows_per_material).
 *
 * <p><b>Material axis M</b> (the reviewer-corrected口径, 2026-09-16, extended 2026-09-28 by
 * task a-ore-axis-extension and task b-gem-pool-extension, then 2026-10-05 by tasks
 * worldgen-edge-ores-b2-orphans, worldgen-edge-ores-b1-vein-axis and worldgen-axis-batch2): the upstream always-on
 * worldgen small-ore materials
 * (Loader_Worldgen.java:800-852 + :875 — {@link #WORLDGEN_ORES}) UNION the stone-layer
 * companion materials ({@link #STONE_LAYER_ORES}, the r6-c3 lens preconditions) UNION
 * the stone-layer EDGE orphan materials ({@link #EDGE_ORES}, the boundary-blob route B)
 * UNION
 * the RANDOM_SMALL_GEM_ORE pool gap ({@link #GEM_POOL_ORES}, the r7-b second seam) UNION
 * the large-vein compensation materials ({@link #LARGE_VEIN_ORES}, the B1 third seam), each
 * passing the authoritative {@link OP#ore}
 * {@code isGeneratingItem} filter (OP.java:1098 setCondition(ORES); the same
 * per-material criterion as the GTMaterialItems.java:146 item walk), UNIFIED across
 * all 26 families and all three forms. M = 53 + 13 + 15 + 56 + 22 = 159 distinct materials; total
 * blocks = 74 x 159 = 11766, pinned by
 * GT6OreBlocksRegistrationTest. (The bare isGeneratingItem walk over the whole
 * MATERIAL_ARRAY measures 618 — nine tenths of it materials no ore placement ever
 * references; that over-registration face was REJECTED in review and removed.)
 *
 * <p><b>id scheme</b> {@code gt6:ore[_broken|_small]_<family>_<material>} — the normal
 * form carries no form segment; family snakes are the upstream internal names (blackgranite
 * .. lightprismarine, Loader_Rocks.java:57-139 name tails — underscore-free tokens, which
 * also keeps the composed ids unambiguous against material snakes like RedFluorite). Normal
 * and broken share the family prefix upstream (same-prefix different-block,
 * Loader_Ores.java:56 vs :61 both OP.oreVanillastone), so the id needs the form segment;
 * small ores share OP.oreSmall across all families upstream (Loader_Rocks.java:59 et seq.),
 * hence the per-family id segment.
 *
 * <p><b>Creative tabs</b> (upstream verbatim): PrefixBlockItem.java:62-64 hides every
 * ore-prefix block from creative unless SHOW_ORE_BLOCK_PREFIXES (a debug flag, default
 * false) EXCEPT {@code gt.meta.ore.normal.default} — the stone family's normal block.
 * So exactly ONE tab: the stone family's normal form (upstream tab = the prefix's
 * CreativeTab, name/colour = mNameInternal/mNameCategory, CreativeTab.java:28-35).
 *
 * <p><b>Intermediate state (ADR ④)</b>: no blockstate/model/loot JSONs belong to this
 * card (the p30 wave cards ② textures ③ datagen ④ loot consume
 * {@link #registrationOrder()}); texture-missing faces are the texture card's problem —
 * the blocks themselves are registrable and dedicated-server clean today. KJS face
 * declared on the task card: registration face defers to the kjs binding card.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6OreBlocks {

    /** normal / broken / small — the three form semantics of an upstream ore family row triple. */
    public enum FormKind { NORMAL, BROKEN, SMALL }

    /**
     * One form row of a family (the PrefixBlock_ ctor columns verbatim): aBaseHardness,
     * aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum (already clamped at 0 the
     * way PrefixBlock.java:176 clamps it), aGravity (FallingBlock row). The harvest tool
     * rides the family (pickaxe/shovel is family-uniform upstream).
     */
    public record Form(float hardness, float resistance, int harvestLevelOffset, int harvestLevelMinimum, boolean gravity) {}

    /**
     * One ore family row: the prefix (normal+broken share it upstream), the verbatim
     * SoundType / vanilla-Material stand-in MapColor / harvest tool / ender-proof /
     * XP columns, the two-or-three form rows (broken == null means broken≡normal, the
     * 2-form dust families), and the stone anchors for the stoneToNormalOres semantics
     * (upstream Loader_Ores.java:85-107 + Loader_Rocks.java:145-147 map the base rock —
     * broken anchors: the cobble form for stone/deepslate, the same rock for every other
     * family). SUPPLIERS for the prefix and the anchors: this class is loaded by the
     * {@code @EventBusSubscriber} scan at mod construct, BEFORE OP.init() — the eager
     * dereference NPE (GTMaterialPrefixBlockItem.java:51 prefix null on the first live
     * item) is the GTStoneBlocks.java:74-77 supplier lesson verbatim.
     */
    public record OreFamily(String snake, Supplier<OreDictPrefix> prefixSupplier, String tool, SoundType sound,
            MapColor color, boolean enderProof, int xpMin, int xpMax,
            Form normal, Form broken, Form small,
            Supplier<Block> stoneAnchor, Supplier<Block> brokenAnchor) {

        /** The family prefix, resolved lazily (post-OP.init). */
        public OreDictPrefix prefix() {
            return prefixSupplier.get();
        }

        /** The oredict prefix a kind registers under (small ores share OP.oreSmall across all families upstream). */
        public OreDictPrefix prefix(FormKind kind) {
            return kind == FormKind.SMALL ? OP.oreSmall : prefixSupplier.get();
        }

        /** The form row of a kind, or null when the family has no separate broken block (broken≡normal). */
        public Form form(FormKind kind) {
            return switch (kind) {
                case NORMAL -> normal;
                case BROKEN -> broken;
                case SMALL -> small;
            };
        }

        /** The family forms in walk order: normal, [broken when separate], small. */
        public List<FormKind> kinds() {
            return broken == null ? List.of(FormKind.NORMAL, FormKind.SMALL)
                                  : List.of(FormKind.NORMAL, FormKind.BROKEN, FormKind.SMALL);
        }
    }

    /** One (family, form, material) triple — the registration/walk unit. */
    public record OreKey(OreFamily family, FormKind kind, OreDictMaterial material) {}

    /**
     * The 26 families, walk order: the 5 three-form vanilla anchors, the 4 two-form dust
     * families, then the 17 GT stones in Loader_Rocks order. Every literal below cites its
     * upstream row; the GT17 rows expand via {@link #gtStone} (the row-pair form, the
     * upstream LITERALS).
     */
    public static final List<OreFamily> FAMILIES = List.of(
        // -- vanilla anchors, three-form (normal/broken/small) --------------------------------------
        new OreFamily("stone", () -> OP.oreVanillastone, "pickaxe", SoundType.STONE, MapColor.STONE, false, 0, 1,
                new Form(1.0F, 1.0F, 0, 0, false),      // :61
                new Form(0.5F, 0.5F, -1, 0, true),      // :56
                new Form(1.0F, 1.0F, -1, 0, false),     // :69
                () -> net.minecraft.world.level.block.Blocks.STONE,
                () -> net.minecraft.world.level.block.Blocks.COBBLESTONE),
        new OreFamily("deepslate", () -> OP.oreDeepslate, "pickaxe", SoundType.DEEPSLATE, MapColor.DEEPSLATE, false, 0, 1,
                new Form(1.0F, 1.0F, 0, 0, false),      // rockset defaults, Loader_Ores.java:439 via :212
                new Form(0.5F, 0.5F, -1, 0, true),      // :474 (hardness/resistance halved, gravity)
                new Form(1.0F, 1.0F, -1, 0, false),     // :475
                () -> net.minecraft.world.level.block.Blocks.DEEPSLATE,
                () -> net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE),
        new OreFamily("netherrack", () -> OP.oreNetherrack, "pickaxe", SoundType.STONE, MapColor.STONE, false, 0, 1,
                new Form(0.5F, 0.5F, 0, 0, false),      // :63
                new Form(0.2F, 0.2F, -1, 0, true),      // :58
                new Form(0.5F, 0.5F, -1, 0, false),     // :71
                () -> net.minecraft.world.level.block.Blocks.NETHERRACK,
                () -> net.minecraft.world.level.block.Blocks.NETHERRACK),
        new OreFamily("endstone", () -> OP.oreEndstone, "pickaxe", SoundType.STONE, MapColor.STONE, true, 2, 3,
                new Form(1.0F, 2.0F, 0, 0, false),      // :64 — ender-proof + XP 2-3 (:80)
                new Form(0.5F, 1.0F, -1, 0, true),      // :59
                new Form(1.0F, 2.0F, -1, 0, false),     // :72
                () -> net.minecraft.world.level.block.Blocks.END_STONE,
                () -> net.minecraft.world.level.block.Blocks.END_STONE),
        new OreFamily("sandstone", () -> OP.oreSandstone, "pickaxe", SoundType.STONE, MapColor.STONE, false, 0, 1,
                new Form(0.6F, 0.8F, 0, 0, false),      // :62
                new Form(0.3F, 0.4F, -1, 0, true),      // :57
                new Form(0.6F, 0.8F, -1, 0, false),     // :70
                () -> net.minecraft.world.level.block.Blocks.SANDSTONE,
                () -> net.minecraft.world.level.block.Blocks.SANDSTONE),
        // -- vanilla anchors, two-form broken≡normal (the dust families, gravity rows) --------------
        new OreFamily("gravel", () -> OP.oreGravel, "shovel", SoundType.GRAVEL, MapColor.SAND, false, 0, 1,
                new Form(0.6F, 0.8F, 0, 0, true),       // :65
                null,                                    // broken≡normal (stoneToBrokenOres -> the same block, :96)
                new Form(0.6F, 0.8F, -1, 0, true),      // :73
                () -> net.minecraft.world.level.block.Blocks.GRAVEL,
                () -> net.minecraft.world.level.block.Blocks.GRAVEL),
        new OreFamily("sand", () -> OP.oreSand, "shovel", SoundType.SAND, MapColor.SAND, false, 0, 1,
                new Form(0.4F, 0.6F, 0, 0, true),       // :66
                null,
                new Form(0.4F, 0.6F, -1, 0, true),      // :74
                () -> net.minecraft.world.level.block.Blocks.SAND,
                () -> net.minecraft.world.level.block.Blocks.SAND),
        new OreFamily("redsand", () -> OP.oreRedSand, "shovel", SoundType.SAND, MapColor.SAND, false, 0, 1,
                new Form(0.4F, 0.6F, 0, 0, true),       // :67
                null,
                new Form(0.4F, 0.6F, -1, 0, true),      // :75
                () -> net.minecraft.world.level.block.Blocks.RED_SAND,
                () -> net.minecraft.world.level.block.Blocks.RED_SAND),
        new OreFamily("mud", () -> OP.oreMud, "shovel", SoundType.GRAVEL, MapColor.DIRT, false, 0, 1,
                new Form(0.3F, 0.5F, 0, 0, true),       // :121
                null,
                new Form(0.3F, 0.5F, -1, 0, true),      // :122
                () -> net.minecraft.world.level.block.Blocks.MUD, // the Diggables→vanilla mud deviation (ruling)
                () -> net.minecraft.world.level.block.Blocks.MUD),
        // -- the 17 GT stones, three-form, Loader_Rocks.java:57-139 order (upstream internal names) --
        gtStone("blackgranite",    3.0F, 6.0F, 1.5F, 3.0F, 3),    // :57-59
        gtStone("redgranite",      3.0F, 6.0F, 1.5F, 3.0F, 3),    // :62-64
        gtStone("basalt",          2.0F, 3.0F, 1.0F, 1.5F, 2),    // :67-69
        gtStone("marble",          0.5F, 0.75F, 0.25F, 0.37F, 0), // :72-74
        gtStone("limestone",       0.5F, 0.75F, 0.25F, 0.37F, 0), // :77-79
        gtStone("granite",         1.0F, 1.5F, 0.5F, 0.75F, 1),   // :82-84 (OP.oreVanillagranite)
        gtStone("diorite",         0.5F, 0.75F, 0.25F, 0.37F, 0), // :87-89
        gtStone("andesite",        0.5F, 0.75F, 0.25F, 0.37F, 0), // :92-94
        gtStone("komatiite",       2.0F, 3.0F, 1.0F, 1.5F, 2),    // :97-99
        gtStone("greenschist",     0.5F, 0.75F, 0.25F, 0.37F, 0), // :102-104
        gtStone("blueschist",      0.5F, 0.75F, 0.25F, 0.37F, 0), // :107-109
        gtStone("kimberlite",      2.0F, 3.0F, 1.0F, 1.5F, 2),    // :112-114
        gtStone("quartzite",       0.5F, 0.75F, 0.25F, 0.37F, 0), // :117-119
        gtStone("lightprismarine", 0.5F, 0.75F, 0.25F, 0.37F, 0), // :122-124
        gtStone("darkprismarine",  0.5F, 0.75F, 0.25F, 0.37F, 1), // :127-129
        gtStone("slate",           0.5F, 0.75F, 0.25F, 0.37F, 1), // :132-134
        gtStone("shale",           0.5F, 0.75F, 0.25F, 0.37F, 0)  // :137-139
    );

    /**
     * One GT-stone family from its Loader_Rocks row pair (normal hardness/resistance,
     * broken hardness/resistance — the upstream LITERALS, Loader_Rocks.java:57-139, NOT
     * computed halves: the 0.75-resistance stones carry the 0.37F literal — and the harvest
     * level): normal = (h, r, 0, level, F), broken = (hb, rb, -1, max(0, level-1), T)
     * (the minimum clamp, PrefixBlock.java:176), small = (h, r, -1, level, F); XP
     * 0..max(1, level) (the :144 Drops row); anchor = the GTStoneBlocks STONE variant
     * (upstream :145-147 map the single stone block into all three maps).
     */
    private static OreFamily gtStone(String snake, float hardness, float resistance, float brokenHardness,
            float brokenResistance, int level) {
        Supplier<OreDictPrefix> prefix = () -> prefixOfSnake(snake);
        Form normal = new Form(hardness, resistance, 0, level, false);
        Form broken = new Form(brokenHardness, brokenResistance, -1, Math.max(0, level - 1), true);
        Form small = new Form(hardness, resistance, -1, level, false);
        String tStoneSnake = stoneBlockSnake(snake); // the GTStoneBlocks id for the anchor handle
        return new OreFamily(snake, prefix, "pickaxe", SoundType.STONE, MapColor.STONE, false,
                0, Math.max(1, level), normal, broken, small,
                () -> GTStoneBlocks.block(tStoneSnake, gregtech6.block.stone.StoneVariant.STONE).get(),
                () -> GTStoneBlocks.block(tStoneSnake, gregtech6.block.stone.StoneVariant.STONE).get());
    }

    /** The family snake -> the GTStoneBlocks id carrying the stone (the granite/prismarine naming splits). */
    private static String stoneBlockSnake(String snake) {
        return switch (snake) {
            case "blackgranite" -> "granite_black";
            case "redgranite" -> "granite_red";
            case "lightprismarine" -> "prismarine_light";
            case "darkprismarine" -> "prismarine_dark";
            default -> snake;
        };
    }

    /** The compile-checked OP field for a GT-stone snake (the granite exception called out). */
    private static OreDictPrefix prefixOfSnake(String snake) {
        return switch (snake) {
            case "blackgranite" -> OP.oreBlackgranite;
            case "redgranite" -> OP.oreRedgranite;
            case "basalt" -> OP.oreBasalt;
            case "marble" -> OP.oreMarble;
            case "limestone" -> OP.oreLimestone;
            case "granite" -> OP.oreVanillagranite;
            case "diorite" -> OP.oreDiorite;
            case "andesite" -> OP.oreAndesite;
            case "komatiite" -> OP.oreKomatiite;
            case "greenschist" -> OP.oreGreenschist;
            case "blueschist" -> OP.oreBlueschist;
            case "kimberlite" -> OP.oreKimberlite;
            case "quartzite" -> OP.oreQuartzite;
            case "lightprismarine" -> OP.oreLightprismarine;
            case "darkprismarine" -> OP.oreDarkprismarine;
            case "slate" -> OP.oreSlate;
            case "shale" -> OP.oreShale;
            default -> throw new IllegalArgumentException("not a GT-stone snake: " + snake);
        };
    }

    /** The upstream-visible family: the stone normal form (PrefixBlockItem.java:63 gate, class javadoc). */
    public static final OreFamily TAB_FAMILY = FAMILIES.get(0);
    /** The tab title key — card ③'s lang face (task card: zh ratchet 0 this card). */
    public static final String TAB_TITLE_KEY = "itemGroup.gt6.ore_vanillastone";

    /** Runtime index (family, kind, material) -> block handle, in registration order. */
    //? if forge {
    private static final Map<OreKey, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    /** Runtime index (family, kind, material) -> block-item handle, in registration order. */
    private static final Map<OreKey, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    //?} else {
    /*private static final Map<OreKey, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> BLOCKS = new LinkedHashMap<>();
    private static final Map<OreKey, net.neoforged.neoforge.registries.DeferredHolder<Item, Item>> ITEMS = new LinkedHashMap<>();
     *///?}
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1), per registry. */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS = new HashSet<>();

    private GT6OreBlocks() {
    }

    /**
     * The upstream always-on small-ore materials — the material axis M (the architect
     * enumeration table's own basis, tasks.p30-arch-ore-registration; the reviewer
     * correction 2026-09-16 restores this口径 after the 45732-block over-registration
     * was rejected): the 53 unconditional WorldgenOresSmall rows (Loader_Worldgen.java
     * :800-852 — every gate-T row; redcinnabar :828 and cinnabar :851 both carry
     * MT.OREMATS.Cinnabar, so the rows collapse to 51 distinct materials) + nikolite
     * (:875, the !mHidden row) = <b>53 distinct materials</b> — the small-ore half of the
     * axis (the stone-layer companions of {@link #STONE_LAYER_ORES} union in separately, so
     * this list keeps its small-ore-row meaning pure). The 21 mod-gated rows (:854-874,
     * MD.AA/AE/ARS/HEX/TC/IHL) and the
     * RANDOM_SMALL_GEM loop (:877-880, GEN_GEMS) and the large-vein materials (:886-925,
     * the t3 card's consumption) stay OUT — the mod-gated faces are the compat cards'
     * pool, upstream's own out-of-scope ruling.
     *
     * <p>The ancientdebris row (:852) is one of the 53 always-on rows and stays in: its
     * {@code !IL.Ancient_Debris.exists()} gate is a PLACEMENT-time compat check (vanilla
     * 1.20.1 ships ancient debris, so the worldgen card will not place the GT row), not a
     * registration-time one — upstream registers every family's metas regardless of the
     * per-row placement gates.
     *
     * <p>Suppliers again (post-OP.init resolution — the {@link OreFamily} lesson).
     *
     * <p>Driver-face interlink (mdh-4 closeout): the 21 mod-gated rows' MD.* gates
     * (AA/AE/ARS/HEX/TC/IHL) are port-time static rulings kept as history — the unified
     * mod-driver face is GT6ModDrivers (mdh series; isLoaded/visibilityGate); those six
     * domains are mdh-2 atlas (GT6ForeignMaterialAtlas) takeover candidates.
     */
    public static final List<Supplier<OreDictMaterial>> WORLDGEN_ORES = List.of(
        () -> MT.Cu,                        () -> MT.OREMATS.Chalcopyrite, () -> MT.OREMATS.Malachite,     // :800-802
        () -> MT.Sn,                        () -> MT.OREMATS.Cassiterite,  () -> MT.Zn,                    // :803-805
        () -> MT.OREMATS.Sphalerite,        () -> MT.OREMATS.Smithsonite,  () -> MT.OREMATS.Stibnite,      // :806-808
        () -> MT.Bi,                        () -> MT.Pb,                   () -> MT.OREMATS.Galena,        // :809-811
        () -> MT.Ag,                        () -> MT.Au,                   () -> MT.Pyrite,                // :812-814
        () -> MT.Fe2O3,                     () -> MT.MnO2,                 () -> MT.OREMATS.Garnierite,    // :815-817
        () -> MT.OREMATS.Pentlandite,       () -> MT.OREMATS.Scheelite,    () -> MT.NaCl,                  // :818-820
        () -> MT.KCl,                       () -> MT.OREMATS.Borax,        () -> MT.Asbestos,              // :821-823
        () -> MT.Diamond,                   () -> MT.Amber,                () -> MT.Craponite,             // :824-826
        () -> MT.Redstone,                  () -> MT.OREMATS.Cinnabar,     () -> MT.Lapis,                 // :827-829 (redcinnabar)
        () -> MT.Eudialyte,                 () -> MT.Azurite,              () -> MT.Coal,                  // :830-832
        () -> MT.Graphite,                  () -> MT.OREMATS.Pollucite,    () -> MT.OREMATS.Zeolite,       // :833-835
        () -> MT.OREMATS.Coltan,            () -> MT.Pt,                   () -> MT.Ir,                    // :836-838
        () -> MT.OREMATS.Sperrylite,        () -> MT.OREMATS.Cooperite,    () -> MT.Nq,                    // :839-841
        () -> MT.Ke,                        () -> MT.Dolamide,             () -> MT.Endium,                // :842-844
        () -> MT.Sugilite,                  () -> MT.Ambrosium,            () -> MT.Zanite,                // :845-847
        () -> MT.S,                         () -> MT.Niter,                () -> MT.Efrine,                // :848-850
        () -> MT.OREMATS.Cinnabar,          () -> MT.AncientDebris,        () -> MT.Nikolite               // :851, :852, :875
    );

    /**
     * The stone-layer companion materials (task a-ore-axis-extension, the r6-c3 lens
     * precondition, user C-tier "complete & self-consistent" ruling, plan B): the 13
     * distinct companion materials the upstream stone-layer rows place as REAL ore blocks —
     * StoneLayerOres.normal/small → {@code placeBlock(mMaterial.mID)} (StoneLayer.java:124-126,
     * {@code mOre = BlocksGT.stoneToNormalOres}) — that sit OUTSIDE {@link #WORLDGEN_ORES}.
     * Upstream rows (the STONE_LAYER_ORES registration table, Loader_Worldgen.java:179-364):
     * basalt Peridot/Uvarovite/Grossular/Chromite (:247-252); kimberlite Spinel/BalasRuby
     * (:225-229); komatiite MgCO3 (:217-222); marble Stannite/Kesterite (:288-295);
     * granite_red Pitchblende/Uraninite + Tantalite/Columbite (:358-364, the HBM-gated rows
     * register unconditionally in the port — no HBM). Coltan is NOT here (already in
     * WORLDGEN_ORES :836); Columbite (9246) is a distinct material and joins the axis here.
     *
     * <p>Union semantics: NOT always-on small ores — the 13 gain no WorldgenOresSmall rows
     * and {@link #WORLDGEN_ORES} keeps its small-ore-row meaning pure. They only become
     * registrable block faces (26 families x 74 form-rows each) and valid large-vein slots
     * (GT6VeinGenerator.valid filters on this axis — the pitchblende/garnet/peridot rows
     * revive, declared on the task card).
     *
     * <p>Suppliers again (post-OP.init resolution — the {@link OreFamily} lesson).
     */
    public static final List<Supplier<OreDictMaterial>> STONE_LAYER_ORES = List.of(
        () -> MT.Peridot,                   () -> MT.Uvarovite,            () -> MT.Grossular,             // basalt companions (:248-250)
        () -> MT.OREMATS.Chromite,          () -> MT.Spinel,               () -> MT.BalasRuby,             // basalt :251 / kimberlite :227-228 companions
        () -> MT.OREMATS.Pitchblende,       () -> MT.OREMATS.Uraninite,                                    // granite_red companions
        () -> MT.OREMATS.Tantalite,         () -> MT.OREMATS.Columbite,    () -> MT.MgCO3,                 // Coltan-family + carbonate
        () -> MT.OREMATS.Stannite,          () -> MT.OREMATS.Kesterite                                     // the copper-tin-sulfide pair
    );

    /**
     * The stone-layer EDGE orphan materials (task worldgen-edge-ores-b2-orphans, the
     * research.stonelayer-edge-ores route B): the 10 boundary materials whose ONLY
     * ungated upstream source is a {@code StoneLayer.bothsides/topbottom} blob
     * (Loader_Worldgen.java:481-571) — no WorldgenOresSmall row, no large-vein slot.
     * Upstream rows (the boundary calls, Y bands verbatim): AmberDominican
     * (U8, 30-70, BIOMES_SHROOM, the Coal/Lignite/Oilshale-Stone boundaries :481-495);
     * Perlite (U4, 0-16, Komatiite/Gabbro-Basalt :496-501); Diatomite (U16, 16-64,
     * topbottom Dolomite-Diorite :506-509); Alunite (U4, 32-80, Rhyolite-Quartzite
     * :529-531); Mirabilite + Trona (U8, 16-64, Gneiss-Gypsum :532-535); Vermiculite
     * (U8, 48-80, GraniteRed-Gneiss :547-550); Mica + Biotite (U8/U16, 16-48,
     * GraniteBlack-Gneiss :551-554); DiamondPink (U32, 0-32, BIOMES_JUNGLE,
     * topbottom GraniteBlack-Basalt :561-565).
     *
     * <p>Mica/Trona also have IHL-gated WorldgenOresSmall namesakes (:871/:874, MD.IHL)
     * which stay in the :854-874 compat pool — the boundary IS their ungated natural
     * source. Deviations declared on the rows (GTOreWorldgen): amount 1 (the blob
     * chance column is a per-boundary-position 1-in-N roll, not a density — the seam
     * enrichment is strata-mode deferred), the biome gates (:484/:563) not carried,
     * never deep-mirrored. Unlike {@link #STONE_LAYER_ORES} these DO gain small-ore
     * rows ({@code GTOreWorldgen.ROWS}, 10 overworld pairs).
     *
     * <p>worldgen-axis-batch2 appended the 5 fuel/evaporite/quartz anchors (axis
     * 132→137): Bauxite (:84, the EtFu deepslate-list {@code StoneLayerOres} row whose
     * OWN band 16-32 rides the call; the EtFu host block + BIOMES_PLAINS gates are the
     * b2 biome-gate grammar, not carried), Lignite (:371 stratum, boundary band
     * :486), Oilshale (:491 boundary band on its stratum), Gypsum (:532 boundary band
     * on its stratum) and MilkyQuartz (:901, the ore.large.quartz LENS row whose band
     * 40-80 rides the call — the lens face itself revives through the axis, the row is
     * the scatter face). The strata carry no Y numbers of their own (depth-driven),
     * so the three stratum anchors cite their layer's boundary call verbatim — the
     * same lines that fed b2's amber/mirabilite/trona rows. Unlike
     * {@link #STONE_LAYER_ORES} these DO gain small-ore rows (5 more overworld pairs).
     *
     * <p>Suppliers again (post-OP.init resolution — the {@link OreFamily} lesson).
     */
    public static final List<Supplier<OreDictMaterial>> EDGE_ORES = List.of(
        () -> MT.AmberDominican,            () -> MT.OREMATS.Perlite,      () -> MT.OREMATS.Diatomite,     // :484/:497/:508
        () -> MT.OREMATS.Alunite,           () -> MT.OREMATS.Mirabilite,   () -> MT.OREMATS.Trona,         // :530/:533/:534
        () -> MT.OREMATS.Vermiculite,       () -> MT.OREMATS.Mica,         () -> MT.Biotite,               // :548/:552/:553
        () -> MT.DiamondPink,                                                                                             // :563
        // -- worldgen-axis-batch2: the 5 fuel/evaporite/quartz anchors, anchor-line order (:84/:371-486/:491/:532/:901)
        () -> MT.OREMATS.Bauxite,           () -> MT.Lignite,              () -> MT.Oilshale,              // :84 stone-layer row / :371 stratum / :491 stratum
        () -> MT.Gypsum,                    () -> MT.MilkyQuartz                                                                           // :532 stratum host / :901 lens
    );

    /**
     * The RANDOM_SMALL_GEM_ORE pool materials (task b-gem-pool-extension): the 56 pool
     * members that sit OUTSIDE {@link #WORLDGEN_ORES} and {@link #STONE_LAYER_ORES} — the
     * second axis seam (the r7-a review finding made quantitative). Upstream pool census
     * = 61 flagged materials (offline walk over MT.java, 2026-09-28): the 7 gem factories
     * 48 members — sapphire 7 (MT.java:1380-1386, SET_GEM_VERTICAL), emerald 7 (:1371-1377,
     * SET_EMERALD), garnet 6 (:1393-1398, SET_RUBY), jasper 6 (:1401-1406, SET_GLASS),
     * tigereye 6 (:1409-1414, SET_GLASS), aventurine 6 (:1417-1422, SET_GLASS), fluorite
     * 10 (:1109-1118, SET_RUBY, CaF2 = "Fluorite" internally) — plus 13 inline flags
     * (:1425-1443). Five of the 61 are already axis members via {@link #STONE_LAYER_ORES}
     * (Peridot/Uvarovite/Grossular/Spinel/BalasRuby), so THIS list carries exactly the
     * 56-material gap; 61 - 5 = 56, axis M = 66 + 56 = 122, blocks 74 x 122 = 9028.
     *
     * <p>Union semantics: UNLIKE {@link #STONE_LAYER_ORES} these materials DO gain
     * WorldgenOresSmall rows — the upstream pool loop (Loader_Worldgen.java:877-878)
     * creates one {@code WorldgenOresSmall(name, T, 5, 250, 1, tGem, GEN_GEMS)} row per
     * flagged material, ported as the 61 GEN_GEMS rows of {@code GTOreWorldgen.ROWS}
     * (61, not 56: the loop iterates the whole pool with no axis filter, so the five
     * stone-layer companions carry upstream small-ore rows too — coordinator ruling
     * 2026-09-28). All rows ride GEN_OVERWORLD only (GEN_GEMS = CS.java:965 is a
     * nine-domain list whose other eight carriers have no modern dimension).
     *
     * <p>Suppliers again (post-OP.init resolution — the {@link OreFamily} lesson).
     */
    public static final List<Supplier<OreDictMaterial>> GEM_POOL_ORES = List.of(
        // -- sapphire family, 7 (MT.java:1380-1386; Ruby :1381) -------------------------------------
        () -> MT.Sapphire,                  () -> MT.Ruby,                 () -> MT.BlueSapphire,          // :1380-1382
        () -> MT.GreenSapphire,             () -> MT.PurpleSapphire,       () -> MT.YellowSapphire,        // :1383-1385
        () -> MT.OrangeSapphire,                                                                                           // :1386
        // -- emerald family, 7 (MT.java:1371-1377) ------------------------------------------------------------------
        () -> MT.Emerald,                   () -> MT.Aquamarine,           () -> MT.Morganite,             // :1371-1373
        () -> MT.Heliodor,                  () -> MT.Goshenite,            () -> MT.Bixbite,               // :1374-1376
        () -> MT.Maxixe,                                                                                                   // :1377
        // -- garnet family remainder, 4 (MT.java:1393-1398; Grossular/Uvarovite already axis) ------------------------
        () -> MT.Almandine,                 () -> MT.Pyrope,               () -> MT.Spessartine,           // :1393-1396
        () -> MT.Andradite,                                                                                                // :1397
        // -- jasper family, 6 (MT.java:1401-1406) -------------------------------------------------------------------
        () -> MT.Jasper,                    () -> MT.JasperOcean,          () -> MT.JasperRainforest,      // :1401-1403
        () -> MT.JasperBlue,                () -> MT.JasperGreen,          () -> MT.JasperYellow,          // :1404-1406
        // -- tigereye family, 6 (MT.java:1409-1414) -----------------------------------------------------------------
        () -> MT.TigerEyeYellow,            () -> MT.TigerEyeGreen,        () -> MT.TigerEyeRed,           // :1409-1411
        () -> MT.TigerEyeBlue,              () -> MT.TigerEyeBlack,        () -> MT.TigerIron,             // :1412-1414
        // -- aventurine family, 6 (MT.java:1417-1422) ---------------------------------------------------------------
        () -> MT.AventurineGreen,           () -> MT.AventurineBrown,      () -> MT.AventurineYellow,      // :1417-1419
        () -> MT.AventurineBlack,           () -> MT.AventurineBlue,       () -> MT.AventurineRed,         // :1420-1422
        // -- fluorite family, 10 (MT.java:1109-1118; CaF2 carries the internal name "Fluorite") ----------------------
        () -> MT.CaF2,                      () -> MT.FluoriteRed,          () -> MT.FluoritePink,          // :1109-1111
        () -> MT.FluoriteBlue,              () -> MT.FluoriteGreen,        () -> MT.FluoriteBlack,         // :1112-1114
        () -> MT.FluoriteWhite,             () -> MT.FluoriteYellow,       () -> MT.FluoriteOrange,        // :1115-1117
        () -> MT.FluoriteMagenta,                                                                                          // :1118
        // -- inline flags, 10 (MT.java:1425-1443; Spinel/BalasRuby/Peridot already axis) -----------------------------
        () -> MT.Topaz,                     () -> MT.BlueTopaz,            () -> MT.Tanzanite,             // :1425-1427
        () -> MT.Amazonite,                 () -> MT.Opal,                 () -> MT.OnyxRed,               // :1429-1432
        () -> MT.OnyxBlack,                 () -> MT.Amethyst,             () -> MT.Dioptase,              // :1433-1437
        () -> MT.Jade                                                                                                      // :1443
    );

    /**
     * The large-vein compensation materials (task worldgen-edge-ores-b1-vein-axis, the
     * research.stonelayer-edge-ores route-B B1 card): the 22 distinct slot materials of
     * the upstream compensation rows Loader_Worldgen.java:889-911 — ported VERBATIM into
     * {@link gregtech6.datagen.GT6WorldgenDatagen#LARGE_VEIN_TABLE} (+ the deep mirrors)
     * — that sit OUTSIDE the three lists above. Their rows failed ONLY the
     * registration-axis validity gate (GT6VeinGenerator.valid), so extending the axis
     * revives them with ZERO row edits (the a-ore-axis-extension B-plan declare,
     * GT6WorldgenDatagen "these rows start generating with zero edits here"):
     * Lazurite/Sodalite (:889 lapis top/bottom), Bauxite + Ilmenite spread (:890 — the
     * whole-dead bauxite row), IodineSalt top (:891, MT.KIO3), Lepidolite between /
     * Spodumene spread (:892), Talc bottom (:893), Bastnasite + Monazite between + Nd
     * spread (:898 — the whole-dead monazite row), MilkyQuartz + Barite + CertusQuartz
     * (:901 — the whole-dead quartz row), Kyanite top / Glauconite spread (:902),
     * Wulfenite + Molybdenite + Mo + Powellite (:905 — the whole-dead ORE_END row),
     * Rutile (MT.TiO2) + Zircon (:911 — the fifth whole-dead row, titanium).
     * Gypsum (:893 between) stays OUTSIDE the axis by the B1 ruling — the asbestos row
     * already drew via Chromite/Asbestos, and Gypsum's only revival face is the
     * mod-gated small-ore pool (upstream :872 MD.IHL), another card's territory.
     *
     * <p>List order = upstream :889-911 first appearance (the axis tail order pinned by
     * GT6VeinAxisExtensionTest). Union semantics: the 22 gain no small-ore rows (their
     * compensation rows are {@code WorldgenOresLarge} only) and
     * {@link #WORLDGEN_ORES} keeps its small-ore-row meaning pure — the same shape as
     * {@link #STONE_LAYER_ORES}. mdh-5 interlink: the extension consciously reopens the
     * atlas-PRIMARY static state for exactly TWO materials — Zircon (TROPIC,
     * GT6ForeignMaterialAtlas :2526) and Nd (HBM, :2403) — the census faces re-pinned in
     * GT6AxisTakeoverCensusTest.
     *
     * <p>Suppliers again (post-OP.init resolution — the {@link OreFamily} lesson).
     */
    public static final List<Supplier<OreDictMaterial>> LARGE_VEIN_ORES = List.of(
        () -> MT.Lazurite,                  () -> MT.Sodalite,                                             // :889 lapis top/bottom
        () -> MT.OREMATS.Bauxite,           () -> MT.OREMATS.Ilmenite,                                     // :890 bauxite top+spread
        () -> MT.KIO3,                                                                                     // :891 iodinesalt top
        () -> MT.OREMATS.Lepidolite,        () -> MT.OREMATS.Spodumene,                                    // :892 rocksalt between/spread
        () -> MT.Talc,                                                                                     // :893 asbestos bottom
        () -> MT.OREMATS.Bastnasite,        () -> MT.Monazite,             () -> MT.Nd,                    // :898 monazite top/between/spread
        () -> MT.MilkyQuartz,               () -> MT.OREMATS.Barite,       () -> MT.CertusQuartz,          // :901 quartz top/bottom/between
        () -> MT.OREMATS.Kyanite,           () -> MT.OREMATS.Glauconite,                                   // :902 peridot top/spread
        () -> MT.OREMATS.Wulfenite,         () -> MT.OREMATS.Molybdenite,  () -> MT.Mo,                    // :905 molybdenum top/bottom/between
        () -> MT.OREMATS.Powellite,                                                                        // :905 molybdenum spread
        () -> MT.TiO2,                      () -> MT.Zircon                                                                // :911 titanium top+bottom/between
    );

    /**
     * The material axis M (the reviewer-corrected口径, 2026-09-16, extended by
     * a-ore-axis-extension, b-gem-pool-extension, worldgen-edge-ores-b2-orphans,
     * worldgen-edge-ores-b1-vein-axis and worldgen-axis-batch2): the upstream always-on
     * worldgen small-ore
     * materials ({@link #WORLDGEN_ORES},
     * Loader_Worldgen.java:800-852 + :875) UNION the stone-layer companion materials
     * ({@link #STONE_LAYER_ORES}, the r6-c3 lens preconditions) UNION the stone-layer
     * EDGE orphan materials ({@link #EDGE_ORES}, the boundary-blob route B — 10 since b2,
     * +5 stone-layer/lens anchors since batch2) UNION the
     * RANDOM_SMALL_GEM_ORE pool gap ({@link #GEM_POOL_ORES}, the r7-b second seam)
     * UNION the large-vein compensation materials ({@link #LARGE_VEIN_ORES}, the B1
     * third seam), each passing the authoritative oredict filter {@link OP#ore}
     * {@code isGeneratingItem} (OP.java:1098 setCondition(ORES) — the same
     * per-material criterion as the GTMaterialItems.java:146 item walk, whose
     * resolve/dedup shape this walk mirrors), unified across all families and forms.
     * M = 53 + 13 + 15 + 56 + 22 = 159, total 74 x 159 = 11766, pinned by
     * GT6OreBlocksRegistrationTest.
     *
     * <p>mdh-5 axis-takeover census (task mdh-5-block-worldgen-axis, CLOSED — ruling (a),
     * static terminal state; the B1 axis extension reopened TWO faces, re-pinned):
     * the atlas (GT6ForeignMaterialAtlas) cross-table finds SEVEN PRIMARY members on
     * this axis — Azurite/Eudialyte (tropicraft), CaF2 "Fluorite" (rotarycraft), Jade
     * (erebus), Dolamide (mo), Zircon (tropicraft :2526) and Nd (HBM :2403) since B1 —
     * the first five riding ALWAYS-ON upstream rows (:800-852 + :875 + the :877-880 gem
     * loop), the last two riding the B1-admitted :898/:911 vein rows, never the
     * mod-gated :854-874 pool. Under mdh-3 ABSENT seeds their item universe hides while
     * these block faces stay self-consistent (the block items register HERE, not
     * through the driver-gated GTMaterialItems.enumerate; consumers guard —
     * GT6OreLootTables.java:236 loop-head null-drop, the batch2 sweep account), so no
     * dead reference exists and the driver gate on {@link #addAxisMember} (the :486
     * isGeneratingItem filter stays the only criterion) is NOT taken. Cross-table +
     * per-face rulings: GT6AxisTakeoverCensusTest, decisions.mdh-5-axis-rulings.
     */
    public static List<OreDictMaterial> materialAxis() {
        Set<OreDictMaterial> tSeen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<OreDictMaterial> rAxis = new ArrayList<>();
        for (Supplier<OreDictMaterial> tSupply : WORLDGEN_ORES) {
            addAxisMember(tSupply, tSeen, rAxis);
        }
        for (Supplier<OreDictMaterial> tSupply : STONE_LAYER_ORES) {
            addAxisMember(tSupply, tSeen, rAxis);
        }
        for (Supplier<OreDictMaterial> tSupply : EDGE_ORES) {
            addAxisMember(tSupply, tSeen, rAxis);
        }
        for (Supplier<OreDictMaterial> tSupply : GEM_POOL_ORES) {
            addAxisMember(tSupply, tSeen, rAxis);
        }
        for (Supplier<OreDictMaterial> tSupply : LARGE_VEIN_ORES) {
            addAxisMember(tSupply, tSeen, rAxis);
        }
        return rAxis;
    }

    /** One axis-member resolve: null/alias/isGeneratingItem gates + identity dedup (the GTMaterialItems walk shape). */
    private static void addAxisMember(Supplier<OreDictMaterial> aSupply, Set<OreDictMaterial> aSeen, List<OreDictMaterial> aAxis) {
        OreDictMaterial tMaterial = aSupply.get();
        if (tMaterial == null || tMaterial.mID < 0) return;
        tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
        if (tMaterial == null || tMaterial.mID < 0 || !aSeen.add(tMaterial)) return;
        if (!OP.ore.isGeneratingItem(tMaterial)) return; // OP.java:1098 setCondition(ORES), the authoritative filter
        aAxis.add(tMaterial);
    }

    /**
     * The registration-axis validity, in the JVM-warmth-independent form (task
     * datagen-axis-lottery-fix — the known_bugs.datagen_axis_emission_jvm_lottery root fix,
     * fix_direction (a) "别名解析的确定性化"). Two modes:
     * <ul>
     * <li><b>boot witness</b> (BLOCKS non-empty — a RegisterEvent ran): membership = a block
     * was REGISTERED for {@code (stone, NORMAL, material)} — the map keys were frozen at boot
     * ({@link #registerBlocks} walks {@link #materialAxis()} once, inside the loader's
     * registry stage, strictly before any datagen provider) against the SAME material
     * generation the emission walks resolve (nothing re-floods between: the {@code MT.init}
     * generation guard, MT.java:2711, holds from boot on), so the answer cannot drift with
     * provider order or mid-datagen ambient state. Every JVM that boots the mod takes this
     * mode — live game AND both legs' datagen JVMs: forge 1.20.1 {@code DatagenModLoader.begin}
     * → {@code gatherAndInitializeMods} (DatagenModLoader.java:43) dispatches the GATHER
     * states incl. LOAD_REGISTRIES = {@code GameData.postRegisterEvents}
     * (ForgeStatesProvider.java:25); neoforge {@code CommonModLoader.begin} runs the
     * "Registry initialization" initTask (CommonModLoader.java:46-53). Measured on this
     * repo's forge runData: flood line "GT6 material system initialised: 2200 materials"
     * precedes "GT6 registered 11618 ore blocks (26 families x 157 materials x 74 ...)" —
     * registration AFTER the flood, both before datagen (/tmp/dalf_rundata_forge1.log:69-70).
     * The neo leg's FML-booted test JVM also takes this mode (measured: the pre-fix pin
     * asserting BLOCKS-empty ran red only there).</li>
     * <li><b>ambient axis</b> (BLOCKS empty — no RegisterEvent ran: a plain-JUnit offline
     * test JVM, e.g. the forge leg's test runner, which never boots FML): fall back to a
     * fresh {@link #materialAxis()} identity scan — the same computation the pre-fix gate
     * did, correct on a single-threaded JVM where the inline warm-up
     * ({@code GTOreWorldgen.twilightOnAxisRows} → {@code initMaterials}) floods before the
     * first walk.</li>
     * </ul>
     *
     * <p>The lottery this closes (the 8-row twilight_ores / 137-feature strata_lenses faces,
     * seat19/20): the pre-fix gate re-derived the axis from MT/OP statics AT EMISSION TIME,
     * and that ambient re-derivation proved datagen-unstable in the seat19/20 runs (boot
     * registration measured FULL on the same JVMs, the emission-time walk short by the
     * A-band rows). Task emission-window-mutator closed the follow-up archaeology
     * (2026-10-09, base fe6f30980e): <b>no intra-JVM GT6 mutator exists on the current
     * tree</b> — a five-point probe across the whole emission window (boot registration /
     * GatherDataEvent / the three {@code GTOreWorldgen.twilightOnAxisRows} bootstrap walks /
     * the tail mirror provider) measured the ambient axis 157/157, alias/tag/condition clean
     * end to end on BOTH legs (/tmp/ewm-rundata-forge.log, /tmp/ewm-rundata-neo1.log), and
     * the mutator census is empty: {@code disableItemGeneration} has zero main-code callers;
     * the {@code ITEMGENERATOR.*} tags are stamped once at the flood (the {@code put} tag
     * groups, MT.java) and never removed; {@code mCondition} is assigned at OP clinit only
     * (OP.java:1098 and the ore-family chains — every runtime {@code setOreStats} caller is
     * that same clinit); the {@code MaterialRegistry} open/close/reset callers are the boot
     * {@code GTMaterialItems.initMaterials} (GTMaterialItems.java:104-111) plus the
     * idempotent warm-up re-entry (the MT.init generation guard MT.java:2711 and the OP.init
     * mInitialized gate OP.java:622 hold; the force rows are contains-guarded,
     * GTMaterialItems.java:171-177). What the probe DID pin is the emission-window topology,
     * which is platform-owned: the worldgen bootstrap — the only ambient consumer cluster —
     * runs inside the DatagenModLoader lookup future. Forge 1.20.1 chains
     * {@code registries.thenApply(constructRegistries)} in the
     * DatapackBuiltinEntriesProvider CONSTRUCTOR (DatapackBuiltinEntriesProvider.java:53),
     * so the bootstraps execute on a background worker (RegistrySetBuilder$RegistryStub:250)
     * CONCURRENTLY with the main-thread provider chain — measured: the three twilight walks
     * on Worker-Main-1 while Item Models ran on main (/tmp/ewm-rundata-forge.log:102-114);
     * neo 21.1 runs the same bootstraps synchronously on main before the first provider
     * (RegistrySetBuilder$RegistryStub:404, /tmp/ewm-rundata-neo1.log:105-116). The
     * seat19/20 flips (neo-leg-only, per-run nondeterministic) therefore close as
     * PLATFORM-INHERENT (id1467 class: the datagen JVM's thread/lifecycle semantics), not a
     * fixable GT6 state mutation. The witness stays the correct shape regardless: it severs
     * emission-time dependence on ALL ambient state — any future mutator, and JMM
     * visibility across the platform's threads. Future consumers: any new emission-time
     * gate MUST read this witness (or the boot maps directly), never re-derive ambient
     * state at emission; the twilight band enforces it with the
     * {@code GTOreWorldgen.emissionWindowCanary} re-offense pin. A forced guard-bypassing
     * re-flood (fix_direction (b)) was evaluated and DROPPED: re-creating the materials
     * would move them OUT of the generation the boot-registered GT6OreBlocks keys hold and
     * turn every {@code GT6OreBlocks.get(...).get()} emission lookup
     * (GT6WorldgenDatagen.java:1606) into a miss — the same id1427 generation-flip class.
     */
    public static boolean axisWitness(OreDictMaterial aMaterial) {
        if (aMaterial == null) return false;
        if (BLOCKS.isEmpty()) {
            for (OreDictMaterial tAxis : materialAxis()) if (tAxis == aMaterial) return true;
            return false;
        }
        return get(TAB_FAMILY, FormKind.NORMAL, aMaterial) != null; // map membership IS the witness (get returns null when absent)
    }

    /** The single definition site of the per-pair id scheme: {@code ore[_broken|_small]_<family>_<material>}. */
    public static String path(OreKey key) {
        String tForm = switch (key.kind()) {
            case NORMAL -> "ore_";
            case BROKEN -> "ore_broken_";
            case SMALL -> "ore_small_";
        };
        return tForm + key.family().snake() + "_" + GTMaterialItems.snakeCase(key.material().mNameInternal);
    }

    /**
     * The 74 x M registration walk: family-major (FAMILIES order), kind-major
     * (normal/broken/small), material-major (axis ascending-mID). Offline-safe — no
     * registry access, computable before any RegisterEvent (the census walk).
     */
    public static List<OreKey> registrationOrder() {
        List<OreDictMaterial> tAxis = materialAxis();
        List<OreKey> rOrder = new ArrayList<>(FAMILIES.size() * 3 * tAxis.size());
        for (OreFamily tFamily : FAMILIES) {
            for (FormKind tKind : tFamily.kinds()) {
                for (OreDictMaterial tMaterial : tAxis) {
                    rOrder.add(new OreKey(tFamily, tKind, tMaterial));
                }
            }
        }
        return rOrder;
    }

    /** RegisterEvent, LOW priority: BLOCK segment -> ITEM segment -> CREATIVE_MODE_TAB segment (GTMaterialBlocks.java:79-88 shape). */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.BLOCK) {
            registerBlocks(event);
        } else if (event.getRegistryKey() == Registries.ITEM) {
            registerItems(event);
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            registerCreativeTab(event);
        }
    }

    private static void registerBlocks(RegisterEvent event) {
        long tStart = System.nanoTime();
        List<OreDictMaterial> tAxis = materialAxis();
        for (OreKey tKey : registrationOrder()) {
            ResourceLocation tLoc = gtId(path(tKey));
            if (!REGISTERED_BLOCK_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate ore block id {}", tLoc);
                continue;
            }
            //? if forge {
            RegistryObject<Block> tHandle = RegistryObject.create(tLoc, Registries.BLOCK, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Block, Block> tHandle =
                net.neoforged.neoforge.registries.DeferredHolder.create(Registries.BLOCK, tLoc);
            //21.1: RegistryObject died with the class; DeferredHolder.create(key, id) is the same lazy handle.
            *///?}
            event.register(Registries.BLOCK, tLoc, () -> newBlock(tKey));
            BLOCKS.put(tKey, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} ore blocks (26 families x {} materials x 74 form-rows per material, per-pair)",
                BLOCKS.size(), tAxis.size());
        LOGGER.info("ore block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** One block instance per (family, kind, material): gravity rows become the FallingBlock subclass. */
    private static Block newBlock(OreKey aKey) {
        OreDictPrefix tPrefix = aKey.family().prefix(aKey.kind());
        if (aKey.family().form(aKey.kind()).gravity()) {
            return new GTOreFallingBlock(aKey.family(), aKey.kind(), aKey.family().form(aKey.kind()), tPrefix, aKey.material());
        }
        return new GTOreBlock(aKey.family(), aKey.kind(), aKey.family().form(aKey.kind()), tPrefix, aKey.material());
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (OreKey tKey : registrationOrder()) { // same walk, same order
            ResourceLocation tLoc = gtId(path(tKey));
            RegistryObject<Block> tBlock = BLOCKS.get(tKey); // null when the BLOCK id collided and was skipped
            if (tBlock == null || !REGISTERED_ITEM_IDS.add(tLoc)) { // per-registry dedup
                continue;
            }
            event.register(Registries.ITEM, tLoc, () ->
                    new GTMaterialPrefixBlockItem(new Item.Properties(), tKey.family().prefix(tKey.kind()), tKey.material(), tBlock.get()));
            //? if forge {
            ITEMS.put(tKey, RegistryObject.create(tLoc, Registries.ITEM, "gt6"));
            //?} else {
            /*ITEMS.put(tKey, net.neoforged.neoforge.registries.DeferredHolder.create(Registries.ITEM, tLoc));
            *///?}
        }
        GT6Mod.LOGGER.info("GT6 registered {} ore block items", ITEMS.size());
        LOGGER.info("ore block item registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /**
     * The one upstream-visible ore tab (PrefixBlockItem.java:62-64: every ore-prefix block
     * hides unless SHOW_ORE_BLOCK_PREFIXES, EXCEPT the stone normal family — class javadoc).
     * Title key {@link #TAB_TITLE_KEY} = the prefix's mNameCategory ("Stone Ores"); the lang
     * value is card ③'s face.
     */
    private static void registerCreativeTab(RegisterEvent event) {
        long tStart = System.nanoTime();
        List<OreDictMaterial> tAxis = materialAxis();
        OreKey tFirst = new OreKey(TAB_FAMILY, FormKind.NORMAL, tAxis.get(0));
        //? if forge {
        RegistryObject<Item> tIcon = ITEMS.get(tFirst); // upstream icon = the first family member (CreativeTab.java:28-35 form)
        //?} else {
        /*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tIcon = ITEMS.get(tFirst);
        *///?}
        event.register(Registries.CREATIVE_MODE_TAB, gtId("ore_vanillastone"), () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable(TAB_TITLE_KEY)) // upstream LH.add("itemGroup."+mNameInternal, mNameCategory)
                .icon(() -> new ItemStack(tIcon.get()))
                .displayItems((tParameters, tOutput) -> {
                    for (OreDictMaterial tMaterial : materialAxis()) {
                        if (tMaterial.mHidden) continue; // PrefixBlockItem.java:114, SHOW_HIDDEN_MATERIALS=false default
                        tOutput.accept(new ItemStack(ITEMS.get(new OreKey(TAB_FAMILY, FormKind.NORMAL, tMaterial)).get()));
                    }
                })
                .build());
        GT6Mod.LOGGER.info("GT6 registered 1 ore creative tab (the stone family, upstream SHOW_ORE_BLOCK_PREFIXES=false)");
        LOGGER.info("ore tab registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** gt6 namespaced id (the GTStoneBlocks.java:227 fork form). */
    private static ResourceLocation gtId(String path) {
        //? if forge {
        return ResourceLocation.fromNamespaceAndPath("gt6", path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath("gt6", path); // 21.1: the (namespace, path) ctor is private
        *///?}
    }

    /** Query API: the block handle of a (family, kind, material) triple, or null if not registered. */
    //? if forge {
    public static RegistryObject<Block> get(OreFamily family, FormKind kind, OreDictMaterial material) {
        return BLOCKS.get(new OreKey(family, kind, material));
    }

    /** All registered block handles (unmodifiable, registration order) — the datagen cards' walk. */
    public static Map<OreKey, RegistryObject<Block>> blocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    /** All registered block-item handles (unmodifiable, registration order). */
    public static Map<OreKey, RegistryObject<Item>> items() {
        return Collections.unmodifiableMap(ITEMS);
    }
    //?} else {
    /*public static net.neoforged.neoforge.registries.DeferredHolder<Block, Block> get(OreFamily family, FormKind kind, OreDictMaterial material) {
        return BLOCKS.get(new OreKey(family, kind, material));
    }

    // All registered block handles (unmodifiable, registration order) — the datagen cards' walk.
    public static Map<OreKey, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> blocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    // All registered block-item handles (unmodifiable, registration order).
    public static Map<OreKey, net.neoforged.neoforge.registries.DeferredHolder<Item, Item>> items() {
        return Collections.unmodifiableMap(ITEMS);
    }
     *///?}

    /**
     * The stoneToNormalOres semantics (upstream Loader_Ores.java:85-107, Loader_Rocks.java:145-147):
     * base rock -> the family whose normal/broken/small blocks replace it (the per-form
     * targets ride {@link OreFamily}; the three upstream maps collapse onto one map here —
     * upstream keys the SAME rock into all three). LIVE-ONLY: the GT17 anchors resolve the
     * GTStoneBlocks handles. The t3 vein card and the small-ore card consume this.
     */
    public static Map<Block, OreFamily> stoneToOreFamilies() {
        Map<Block, OreFamily> rMap = new LinkedHashMap<>();
        for (OreFamily tFamily : FAMILIES) {
            rMap.put(tFamily.stoneAnchor().get(), tFamily);
        }
        return rMap;
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");
}
