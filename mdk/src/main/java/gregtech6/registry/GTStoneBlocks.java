package gregtech6.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;

/**
 * Registration home of the GT6 stone universe (task p21-stoneblocks-16item-registry-split,
 * superseding the P19 single-block-per-stone shape): the 17 upstream BlockStones families
 * (CS.java:1668 declaration order — GraniteBlack..Shale) x the 16 {@link StoneVariant}
 * metas = <b>272 per-pair Block+BlockItem registrations</b>, in one self-contained
 * {@code @EventBusSubscriber(MOD)} listener (the GTMaterialBlocks.java:59 shape;
 * {@code @SubscribeEvent} on the method is load-bearing, GTMaterialBlocks.java:75-77).
 * The per-pair granularity is the P8 ADR ④ precedent (GTMaterialBlocks/
 * GTMaterialPrefixBlock): the upstream universe is per-meta item ids ({@code
 * ST.make(this, 1, aMeta)} — the :731 getDrops swap, the GT_Tool_Chisel.java:73-77
 * CHISEL_MAPPINGS drop conversion), which the P19 EnumProperty form could not express.
 *
 * <p><b>id scheme</b> (the ADR ruling ②): variant 0 (STONE) keeps the P19 id
 * {@code gt6:<snake>} (the pre-existing references and generated-JSON keys see a minimal
 * diff); the other 15 variants register {@code gt6:<snake>_<variant snake>}, the suffix
 * segment exactly the model/texture path segment the P19 render card landed
 * ({@code stones/<stone>/<variant>}, BlockStones.java:93-108 icon names lower-cased).
 * {@link #path} is the single definition site of that composition.
 *
 * <p>The four mapping tables (CHISEL/FILE/HAMMER/MOSS, BlockStones.java:81-84) are
 * transcribed 1:1 exactly as the P19 card landed them — byte arrays indexed by the
 * {@link StoneVariant} declaration (= meta) order, so a consumer maps (stone, variantFrom)
 * -> (stone, StoneVariant.VALUES[table[variantFrom.meta()]]); the tables are stone-blind
 * (the same 16 entries serve every stone, upstream included). CHISEL_MAPPINGS[6]==[7]==CHISL
 * is the self-mapping pin. They are the seams the chisel/file/hammer tool cards and the
 * recipes card (p19-chisel-recipes) consume — upstream BlockStones.onToolClick :573-584
 * reads exactly these.
 *
 * <p>Upstream oredict face (BlockStones.java:135-194 {@code OM.reg_}/registerOre_
 * equivalents) is captured as the {@link #oreDictMappings()} equivalence table: this port
 * has no ItemStack->oredict registration surface in mdk (P8 ADR ⑥ precedent), so the
 * table declares what WOULD be registered per prefix, in OP.java:456-465 declaration
 * order, and the OM landing card consumes it. Not portable verbatim, declared
 * deviations: the blockSolid retarget/disableItemGeneration (:135-136), the OM.data
 * composition rows for RNFBR/RSTBR (:138-139) and the equal-block sets (:196) have no
 * port counterpart; the mTextureSolid/Smooth icon copies (:198-199) are render plumbing
 * (the render card's datagen replaces them).
 *
 * <p>No creative tab: the upstream meta type joined the vanilla blocks tab
 * (BlockMetaType.java:63) and this port declares NO tab wiring (the machine family
 * precedent; task p21 keeps the P19 no-tab status quo, the tab ruling stays pooled).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GTStoneBlocks {

    /**
     * One upstream stone row (Loader_Rocks.java:56-139, BlockStonesGT.java:37 ctor args
     * minus the name which decomposes into {@code snake}): multipliers keep the upstream
     * ctor order-independent names — vanilla hardness = hardnessMultiplier * 1.5, blast
     * resistance = resistanceMultiplier * 10 (BlockMetaType.java:61-62). The material is a
     * supplier so this class initialises offline (before GTMaterialItems.initMaterials()).
     */
    public record StoneSpec(String snake, Supplier<OreDictMaterial> material, float hardnessMultiplier,
            float resistanceMultiplier, int harvestLevel, boolean witherProof) {}

    /** One (stone, variant) pair — the 272-element registration/walk unit. */
    public record VariantKey(StoneSpec stone, StoneVariant variant) {}

    /**
     * The 17 stones in CS.java:1668 declaration order (the array literal transcribed;
     * Loader_Rocks.java:56-139 assigns exactly these, bit by bit). Columns are the
     * Loader_Rocks rows verbatim (hardness, resistance, harvest level, witherProof — the
     * upstream ctor's leading two floats are resistance-then-hardness, BlockStonesGT.java:37).
     */
    public static final List<StoneSpec> STONES = List.of(
        new StoneSpec("granite_black",   () -> MT.STONES.GraniteBlack,   3.00F, 6.00F, 3, true),   // :56
        new StoneSpec("granite_red",     () -> MT.STONES.GraniteRed,     3.00F, 6.00F, 3, true),   // :61
        new StoneSpec("basalt",          () -> MT.STONES.Basalt,         2.00F, 3.00F, 2, false),  // :66
        new StoneSpec("marble",          () -> MT.STONES.Marble,         0.50F, 0.75F, 0, false),  // :71
        new StoneSpec("limestone",       () -> MT.STONES.Limestone,      0.50F, 0.75F, 0, false),  // :76
        new StoneSpec("granite",         () -> MT.STONES.Granite,        1.00F, 2.00F, 1, false),  // :81
        new StoneSpec("diorite",         () -> MT.STONES.Diorite,        0.50F, 0.75F, 0, false),  // :86
        new StoneSpec("andesite",        () -> MT.STONES.Andesite,       0.50F, 0.75F, 0, false),  // :91
        new StoneSpec("komatiite",       () -> MT.STONES.Komatiite,      2.00F, 3.00F, 2, false),  // :96
        new StoneSpec("greenschist",     () -> MT.STONES.Greenschist,    0.50F, 0.75F, 0, false),  // :101
        new StoneSpec("blueschist",      () -> MT.STONES.Blueschist,     0.50F, 0.75F, 0, false),  // :106
        new StoneSpec("kimberlite",      () -> MT.STONES.Kimberlite,     2.00F, 3.00F, 2, false),  // :111
        new StoneSpec("quartzite",       () -> MT.STONES.Quartzite,      0.50F, 0.75F, 0, false),  // :116
        new StoneSpec("prismarine_light",() -> MT.PrismarineLight,       0.50F, 0.75F, 0, false),  // :121
        new StoneSpec("prismarine_dark", () -> MT.PrismarineDark,        0.50F, 0.75F, 1, false),  // :126
        new StoneSpec("slate",           () -> MT.STONES.Slate,          0.50F, 0.75F, 1, false),  // :131
        new StoneSpec("shale",           () -> MT.STONES.Shale,          0.50F, 0.75F, 0, false)   // :136
    );

    /** CHISEL: what each variant becomes under the chisel (BlockStones.java:81); [6]==[7]==CHISL is the self-mapping pin. */
    public static final byte[] CHISEL_MAPPINGS = {
        StoneVariant.SMOTH.meta(), StoneVariant.COBBL.meta(), StoneVariant.MCOBL.meta(), StoneVariant.CRACK.meta(),
        StoneVariant.COBBL.meta(), StoneVariant.MCOBL.meta(), StoneVariant.CHISL.meta(), StoneVariant.CHISL.meta(),
        StoneVariant.RNFBR.meta(), StoneVariant.RSTBR.meta(), StoneVariant.STILE.meta(), StoneVariant.STILE.meta(),
        StoneVariant.STILE.meta(), StoneVariant.WINDB.meta(), StoneVariant.WINDA.meta(), StoneVariant.STILE.meta()};

    /** FILE: what each variant becomes under the file (BlockStones.java:82). */
    public static final byte[] FILE_MAPPINGS = {
        StoneVariant.SMOTH.meta(), StoneVariant.COBBL.meta(), StoneVariant.MCOBL.meta(), StoneVariant.SBRIK.meta(),
        StoneVariant.CRACK.meta(), StoneVariant.MBRIK.meta(), StoneVariant.CHISL.meta(), StoneVariant.SMOTH.meta(),
        StoneVariant.RNFBR.meta(), StoneVariant.RSTBR.meta(), StoneVariant.STILE.meta(), StoneVariant.STILE.meta(),
        StoneVariant.STILE.meta(), StoneVariant.WINDB.meta(), StoneVariant.WINDA.meta(), StoneVariant.STILE.meta()};

    /** HAMMER: stone maps to itself for prospecting compatibility (BlockStones.java:83 comment). */
    public static final byte[] HAMMER_MAPPINGS = {
        StoneVariant.STONE.meta(), StoneVariant.COBBL.meta(), StoneVariant.MCOBL.meta(), StoneVariant.CRACK.meta(),
        StoneVariant.COBBL.meta(), StoneVariant.CRACK.meta(), StoneVariant.CRACK.meta(), StoneVariant.COBBL.meta(),
        StoneVariant.RNFBR.meta(), StoneVariant.RSTBR.meta(), StoneVariant.CRACK.meta(), StoneVariant.CRACK.meta(),
        StoneVariant.CRACK.meta(), StoneVariant.CRACK.meta(), StoneVariant.CRACK.meta(), StoneVariant.CRACK.meta()};

    /** MOSS: what each variant becomes when mossed (BlockStones.java:84, BlockStones.java:563 read). */
    public static final byte[] MOSS_MAPPINGS = {
        StoneVariant.STONE.meta(), StoneVariant.MCOBL.meta(), StoneVariant.MCOBL.meta(), StoneVariant.MBRIK.meta(),
        StoneVariant.MBRIK.meta(), StoneVariant.MBRIK.meta(), StoneVariant.CHISL.meta(), StoneVariant.SMOTH.meta(),
        StoneVariant.RNFBR.meta(), StoneVariant.RSTBR.meta(), StoneVariant.TILES.meta(), StoneVariant.STILE.meta(),
        StoneVariant.SBRIK.meta(), StoneVariant.WINDA.meta(), StoneVariant.WINDB.meta(), StoneVariant.QBRIK.meta()};

    /** Runtime index composite-path -> block handle (registration order, stone-major variant-major). */
    private static final Map<String, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    /** Runtime index composite-path -> block-item handle (registration order, stone-major variant-major). */
    private static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    /** Defensive dedup across re-fired RegisterEvents (the GTMaterialBlocks.java:67 ADR-P2-2 fix 1 shape), per registry. */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS = new HashSet<>();

    private GTStoneBlocks() {
    }

    /**
     * The single definition site of the per-pair id scheme (ADR ruling ②): variant 0 keeps
     * the bare P19 id {@code gt6:<snake>}, the other 15 variants suffix the variant's
     * serialized name — exactly the model/texture path segment the render card landed
     * ({@code stones/<stone>/<variant>}, so the JSON key face diffs minimally).
     */
    public static String path(String aStoneSnake, StoneVariant aVariant) {
        return aVariant == StoneVariant.STONE ? aStoneSnake : aStoneSnake + "_" + aVariant.snake;
    }

    /** RegisterEvent, LOW priority: BLOCK segment -> ITEM segment (GTMaterialBlocks.java:79-88 shape; no tab). */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.BLOCK) {
            registerBlocks(event);
        } else if (event.getRegistryKey() == Registries.ITEM) {
            registerItems(event);
        }
    }

    private static void registerBlocks(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (VariantKey tKey : registrationOrder()) { // stone-major, variant-major
            String tPath = path(tKey.stone().snake(), tKey.variant());
            ResourceLocation tLoc = gtId(tPath);
            if (!REGISTERED_BLOCK_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate stone block id {}", tLoc);
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
            BLOCKS.put(tPath, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone blocks (17 stones x 16 variants, per-pair)", BLOCKS.size());
        LOGGER.info("stone block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** One block instance per (stone, variant) pair (fields verbatim from the Loader_Rocks row). */
    private static GTStoneBlock newBlock(VariantKey aKey) {
        return new GTStoneBlock(aKey.stone().snake(), aKey.variant(), aKey.stone().material().get(),
                aKey.stone().hardnessMultiplier(), aKey.stone().resistanceMultiplier(),
                aKey.stone().harvestLevel(), aKey.stone().witherProof());
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (VariantKey tKey : registrationOrder()) { // same walk, same order
            String tPath = path(tKey.stone().snake(), tKey.variant());
            ResourceLocation tLoc = gtId(tPath);
            if (BLOCKS.get(tPath) == null || !REGISTERED_ITEM_IDS.add(tLoc)) { // per-registry dedup
                continue;
            }
            //? if forge {
            RegistryObject<Item> tHandle = RegistryObject.create(tLoc, Registries.ITEM, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle =
                net.neoforged.neoforge.registries.DeferredHolder.create(Registries.ITEM, tLoc);
            *///?}
            // The composed-name BlockItem (task p20-i18n-compose-rows): the stack name
            // delegates to the block compose (vanilla BlockItem only delegates the
            // descriptionId — BlockItem.java:186-189 — whose lang keys retired with the
            // B2 shrink). Per-pair, each block composes its OWN variant template.
            event.register(Registries.ITEM, tLoc, () ->
                    new gregtech6.block.GTComposedNameItem(BLOCKS.get(tPath).get(), new Item.Properties()));
            ITEMS.put(tPath, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone block items (one per variant block)", ITEMS.size());
        LOGGER.info("stone block item registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** gt6 namespaced id (GTMaterialBlocks.java:234 form). */
    private static ResourceLocation gtId(String path) {
        //? if forge {
        return new ResourceLocation("gt6", path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath("gt6", path); // 21.1: the (namespace, path) ctor is private
        *///?}
    }

    /** The block handle of a composite path ({@link #path} form; variant 0 accepts the bare snake), or null before registration. */
    public static RegistryObject<Block> block(String aPath) {
        return BLOCKS.get(aPath);
    }

    /** The block handle of a (stone, variant) pair, or null before registration. */
    public static RegistryObject<Block> block(String aStoneSnake, StoneVariant aVariant) {
        return BLOCKS.get(path(aStoneSnake, aVariant));
    }

    /** The block-item handle of a composite path ({@link #path} form; variant 0 accepts the bare snake), or null before registration. */
    public static RegistryObject<Item> item(String aPath) {
        return ITEMS.get(aPath);
    }

    /** The block-item handle of a (stone, variant) pair, or null before registration. */
    public static RegistryObject<Item> item(String aStoneSnake, StoneVariant aVariant) {
        return ITEMS.get(path(aStoneSnake, aVariant));
    }

    /** All 272 registered blocks, registration order — the datagen walk (the render card's consumer shape). */
    public static List<Block> blockArray() {
        List<Block> rBlocks = new ArrayList<>();
        for (RegistryObject<Block> tHandle : BLOCKS.values()) rBlocks.add(tHandle.get());
        return rBlocks;
    }

    /**
     * The 272 (stone, variant) pairs, stone-major in CS.java:1668 order and variant-major in
     * meta order — the offline-safe census/walk unit for this card's test, the registration
     * walk and the datagen. No registry access: computable before any RegisterEvent.
     */
    public static List<VariantKey> registrationOrder() {
        List<VariantKey> rOrder = new ArrayList<>(STONES.size() * StoneVariant.VALUES.length);
        for (StoneSpec tStone : STONES) {
            for (StoneVariant tVariant : StoneVariant.VALUES) {
                rOrder.add(new VariantKey(tStone, tVariant));
            }
        }
        return rOrder;
    }

    /**
     * The upstream oredict equivalence face (BlockStones.java:135-194): prefix -> the
     * variants that upstream registers under it via {@code OM.reg_}, per stone. The
     * {@code mMaterial != ANY.Stone} segment (:141-170) and the unconditional segment
     * (:173-194) are merged — the union is what the oredict actually answers. Keys in
     * OP.java:456-465 declaration order; the set is identical for all 17 stones (every
     * stone material passes the :141 gate), so the table is per-prefix rather than
     * per-stone. OP.stoneChiseled -> [CHISL] is the p19-chisel-recipes anchor.
     */
    public static Map<OreDictPrefix, List<StoneVariant>> oreDictMappings() {
        Map<OreDictPrefix, List<StoneVariant>> rMap = new LinkedHashMap<>();
        rMap.put(OP.stoneCobble,      List.of(StoneVariant.COBBL, StoneVariant.MCOBL));                              // :158/:174-175/:193
        rMap.put(OP.stoneSmooth,      List.of(StoneVariant.STONE));                                                   // :157/:173
        rMap.put(OP.stoneMossyBricks, List.of(StoneVariant.MBRIK));                                                   // :180
        rMap.put(OP.stoneMossy,       List.of(StoneVariant.MCOBL, StoneVariant.MBRIK));                               // :159/:176/:181
        rMap.put(OP.stoneBricks,      List.of(StoneVariant.BRICK, StoneVariant.CRACK, StoneVariant.MBRIK,
                                               StoneVariant.CHISL, StoneVariant.TILES, StoneVariant.STILE,
                                               StoneVariant.SBRIK, StoneVariant.WINDA, StoneVariant.WINDB,
                                               StoneVariant.QBRIK));                                                  // :160/:162/:165-170/:177-191
        rMap.put(OP.stoneCracked,     List.of(StoneVariant.CRACK));                                                   // :161/:178
        rMap.put(OP.stoneChiseled,    List.of(StoneVariant.CHISL));                                                   // :163/:183 — the stoneChiseled carrier
        rMap.put(OP.stonePolished,    List.of(StoneVariant.SMOTH));                                                   // :164/:185
        rMap.put(OP.stone,            List.of(StoneVariant.STONE, StoneVariant.COBBL, StoneVariant.MCOBL,
                                               StoneVariant.BRICK, StoneVariant.CRACK, StoneVariant.MBRIK,
                                               StoneVariant.CHISL, StoneVariant.SMOTH, StoneVariant.TILES,
                                               StoneVariant.STILE, StoneVariant.SBRIK, StoneVariant.WINDA,
                                               StoneVariant.WINDB, StoneVariant.QBRIK));                              // :142-155/:194 — RNFBR/RSTBR excluded upstream
        rMap.put(OP.cobblestone,      List.of(StoneVariant.COBBL));                                                  // :193
        return rMap;
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");
}
