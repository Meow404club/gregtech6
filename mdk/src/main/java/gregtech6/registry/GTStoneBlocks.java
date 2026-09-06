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
 * Registration home of the GT6 stone universe (task p19-stoneblocks-registry): the 17
 * upstream BlockStones families (CS.java:1668 declaration order —
 * GraniteBlack..Shale), each ONE Block carrying 16 {@link StoneVariant} states, in one
 * self-contained {@code @EventBusSubscriber(MOD)} listener (the GTMaterialBlocks.java:59
 * shape; {@code @SubscribeEvent} on the method is load-bearing, GTMaterialBlocks.java:75-77).
 * The stone set is the literal CS.java:1668 array — NOT all of MT.STONES (the 85xx
 * mod-stone tail of the class is upstream-excluded), the research card's census ruling.
 *
 * <p>The four mapping tables (CHISEL/FILE/HAMMER/MOSS, BlockStones.java:81-84) are
 * transcribed 1:1, indexed by the {@link StoneVariant} declaration order; they are the
 * seams the chisel/file/hammer tool cards and the recipes card (p19-chisel-recipes)
 * consume — upstream BlockStones.onToolClick :573-584 reads exactly these.
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
 * <p>Intermediate state (ADR-P8 ④ precedent, mirrored from GTMaterialBlocks.java:53-57):
 * between this card and the render card the blocks exist WITHOUT blockstate/model/loot
 * JSONs — a declared working state; dedicated servers never bake models, so runServer
 * stays zero-ERROR. No creative tab: the upstream meta type joined the vanilla blocks tab
 * (BlockMetaType.java:63) and this port declares NO tab wiring this card (the machine
 * family precedent).
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

    /** Runtime index snake -> block handle (registration order). */
    private static final Map<String, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    /** Runtime index snake -> block-item handle (registration order). */
    private static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    /** Defensive dedup across re-fired RegisterEvents (the GTMaterialBlocks.java:67 ADR-P2-2 fix 1 shape), per registry. */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS = new HashSet<>();

    private GTStoneBlocks() {
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
        for (StoneSpec tStone : STONES) {
            ResourceLocation tLoc = gtId(tStone.snake());
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
            event.register(Registries.BLOCK, tLoc, () -> newBlock(tStone));
            BLOCKS.put(tStone.snake(), tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone blocks (16 variants each)", BLOCKS.size());
        LOGGER.info("stone block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** One block instance per stone spec (fields verbatim from the Loader_Rocks row). */
    private static GTStoneBlock newBlock(StoneSpec aStone) {
        return new GTStoneBlock(aStone.snake(), aStone.material().get(), aStone.hardnessMultiplier(),
                aStone.resistanceMultiplier(), aStone.harvestLevel(), aStone.witherProof());
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (StoneSpec tStone : STONES) { // same walk, same order
            ResourceLocation tLoc = gtId(tStone.snake());
            if (BLOCKS.get(tStone.snake()) == null || !REGISTERED_ITEM_IDS.add(tLoc)) { // per-registry dedup
                continue;
            }
            //? if forge {
            RegistryObject<Item> tHandle = RegistryObject.create(tLoc, Registries.ITEM, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle =
                net.neoforged.neoforge.registries.DeferredHolder.create(Registries.ITEM, tLoc);
            *///?}
            // Plain BlockItem, default properties (declared: no tab, no extra behaviour);
            // getName delegates to the block (BlockItem.java:187-188) — the GTStoneBlock
            // description-id override carries the item name for free.
            event.register(Registries.ITEM, tLoc, () ->
                    new net.minecraft.world.item.BlockItem(BLOCKS.get(tStone.snake()).get(), new Item.Properties()));
            ITEMS.put(tStone.snake(), tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone block items", ITEMS.size());
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

    /** The block handle of a stone snake id, or null before registration (the GTMaterialBlocks.get seam shape). */
    public static RegistryObject<Block> block(String aSnake) {
        return BLOCKS.get(aSnake);
    }

    /** The block-item handle of a stone snake id, or null before registration. */
    public static RegistryObject<Item> item(String aSnake) {
        return ITEMS.get(aSnake);
    }

    /** All 17 registered blocks, registration order — the render card's datagen walk (p19-stoneblocks-render). */
    public static List<Block> blockArray() {
        List<Block> rBlocks = new ArrayList<>();
        for (RegistryObject<Block> tHandle : BLOCKS.values()) rBlocks.add(tHandle.get());
        return rBlocks;
    }

    /**
     * The 272 (stone, variant) pairs, stone-major in CS.java:1668 order and variant-major in
     * meta order — the offline-safe census/walk unit for this card's test and the render
     * card's 272 blockstate rows. No registry access: computable before any RegisterEvent.
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
