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

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.block.ore.GTOreFallingBlock;
import gregtech6.item.GTMaterialPrefixBlockItem;

/**
 * Registration home of the GT6 ore BLOCK universe (task p30-ore-1-mech, rulings in
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
 * <p><b>Material axis M</b> (decisions.p30-ore-rulings.ore-material-axis): the
 * {@link OP#ore} {@code isGeneratingItem} walk (OP.java:1098 setCondition(ORES); the
 * same criterion as GTMaterialItems enumerate, GTMaterialItems.java:146), UNIFIED across
 * all 26 families and all three forms — upstream's per-prefix conditions are
 * family-equivalent here (every family prefix gets ORES via setOreStats, and per-family
 * blacklist deltas are declared unified by the ruling). Total blocks = 74 x M, pinned by
 * GT6OreBlocksRegistrationTest.
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
     * The material axis M (decisions.p30-ore-rulings.ore-material-axis): the
     * {@link OP#ore} isGeneratingItem walk over MATERIAL_ARRAY with the registration-target
     * merge (the GTMaterialItems.java:137-146 shape), unified across all families and forms.
     */
    public static List<OreDictMaterial> materialAxis() {
        Set<OreDictMaterial> tSeen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<OreDictMaterial> rAxis = new ArrayList<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
            if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial)) continue;
            if (!OP.ore.isGeneratingItem(tMaterial)) continue; // OP.java:1098 setCondition(ORES)
            rAxis.add(tMaterial);
        }
        return rAxis;
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
        return new ResourceLocation("gt6", path);
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
