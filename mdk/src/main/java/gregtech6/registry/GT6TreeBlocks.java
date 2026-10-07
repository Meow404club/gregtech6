package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.block.tree.GT6TreeLeavesBlock;
import gregtech6.block.tree.GT6TreeLogBlock;
import gregtech6.block.tree.GT6TreeSaplingBlock;
import gregtech6.block.tree.GT6TreeXmasItem;
import gregtech6.worldgen.GT6Worldgen;

/**
 * Registration home of the GT6 tree universe (task w6-t1-trees-nine): <b>27 per-pair
 * Block+BlockItem registrations</b> — sapling/log/leaves x the 9 {@link GT6TreeKind} rows
 * (the upstream Saplings_AB meta 0-7 + Saplings_CD meta 0 face, Loader_Woods.java:67-70,
 * fully expanded per the P8 ADR ④ / GTStoneBlocks / GTGrassBlocks precedent: the canopy
 * Feature, the loot tables and the lang rows all key per-species identities). The id scheme
 * is the single-source snake composition {@code <kind.snake>_sapling/_log/_leaves}
 * (GTGrassBlocks.PATHS rule), and the growsThrough face hands each sapling its
 * {@link GT6Worldgen#TREE_CONFIGURED_KEYS} entry.
 *
 * <p>Creative tabs: logs+planks join {@code BUILDING_BLOCKS} (the upstream tabBlock join,
 * BlockBase.java:63), saplings+leaves join {@code NATURAL_BLOCKS} (the vanilla
 * sapling/leaves tab, CreativeModeTabs.java:635) — wired through the platform
 * {@code BuildCreativeModeTabContentsEvent} (the GTGrassBlocks.onBuildTabContents shape).
 *
 * <p>Self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event (GTGrassBlocks.onModConstruct shape; ADR-P3-4: GT6Mod stays untouched).
 *
 * <p>Task gt-tree-planks appends the 9 plank cubes ({@code <snake>_planks}, the upstream
 * BlockTreePlanks meta 0-7 + BlockTreePlanks2 meta 0 face, LoaderWoodDictionary.java:69-113)
 * to the same universe — 36 registrations total. Task planks-blockification appends the 8
 * GENERIC plank cubes ({@link #GENERIC_PLANKS}, the BlockTreePlanks metas 8-15 face:
 * Compressed/Wood/Treated/Crate/Dead/Rotten/Mossy/Frozen, the ids the retired
 * wood-planks-register prefix items carried) — 44 registrations total. The KJS registration
 * face is declared DEFER to the kjs-binding card (the task-card declaration); the worldgen
 * JSON face is datapack-native and needs zero adaptation.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6TreeBlocks {

    public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
    public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

    /** The 9 species rows in upstream meta order (Saplings_AB meta 0-7, then CD meta 0). */
    public static final List<GT6TreeKind> KINDS = List.of(GT6TreeKind.values());

    /**
     * One generic (non-species) plank row of the upstream BlockTreePlanks 16-meta block
     * (task planks-blockification). {@code path} is the registry id — deliberately the SAME
     * path the retired wood-planks-register prefix item carried (the plank_wood family),
     * so the recipe JSON rows that pour onto the generic plank face (sawing.json/unboxinator.json,
     * task plank-mapping-sweep) keep their output ids across the item-to-BlockItem convergence.
     * {@code enName} is the upstream LH row verbatim (BlockTreePlanks.java:48-55), and
     * {@code hardness} rides the {@code getBlockHardness} split (BlockTreePlanks.java:90 —
     * metas 0-11 ×1.0 over the 1.5F base, metas 12-15 ×0.5).
     */
    public record GenericPlank(String path, String enName, float hardness) {}

    /**
     * The 8 generic rows in upstream meta order (BlockTreePlanks.java:48-55 LH rows):
     * meta 8 Compressed Wood, 9 Wood (the IL.Plank identity face, Loader_Woods.java:74),
     * 10 Treated, 11 Crate, 12 Dead, 13 Rotten, 14 Mossy, 15 Frozen. The Planks2 face has
     * no generic rows (Blue Spruce is the only meta and rides the KINDS walk).
     * Declared before the registration fields that consume it (static-init order).
     */
    public static final List<GenericPlank> GENERIC_PLANK_ROWS = List.of(
        new GenericPlank("plank_wood_compressed", "Compressed Wood Planks", gregtech6.block.tree.GT6PlankBlock.HARDNESS),       // meta 8
        new GenericPlank("plank_wood",            "Wood Planks",            gregtech6.block.tree.GT6PlankBlock.HARDNESS),       // meta 9 = IL.Plank
        new GenericPlank("plank_wood_treated",    "Treated Planks",         gregtech6.block.tree.GT6PlankBlock.HARDNESS),       // meta 10
        new GenericPlank("crate",                 "Crate",                  gregtech6.block.tree.GT6PlankBlock.HARDNESS),       // meta 11
        new GenericPlank("plank_wood_dead",       "Dead Planks",            gregtech6.block.tree.GT6PlankBlock.HARDNESS_SOFT),  // meta 12
        new GenericPlank("plank_wood_rotten",     "Rotten Planks",          gregtech6.block.tree.GT6PlankBlock.HARDNESS_SOFT),  // meta 13
        new GenericPlank("plank_wood_mossy",      "Mossy Planks",           gregtech6.block.tree.GT6PlankBlock.HARDNESS_SOFT),  // meta 14
        new GenericPlank("plank_wood_frozen",     "Frozen Planks",          gregtech6.block.tree.GT6PlankBlock.HARDNESS_SOFT)); // meta 15

    /** The 9 saplings, kind order. */
    public static final List<RegistryObject<Block>> SAPLINGS = registerSaplings();
    /** The 9 logs, kind order. */
    public static final List<RegistryObject<Block>> LOGS = registerLogs();
    /** The 9 leaves, kind order. */
    public static final List<RegistryObject<Block>> LEAVES = registerLeaves();
    /** The 9 planks, kind order (task gt-tree-planks — the upstream BlockTreePlanks meta 0-7 + BlockTreePlanks2 meta 0 face, LoaderWoodDictionary.java:69-113). */
    public static final List<RegistryObject<Block>> PLANKS = registerPlanks();
    /** The 8 generic planks, upstream meta order (task planks-blockification — the BlockTreePlanks metas 8-15 face). */
    public static final List<RegistryObject<Block>> GENERIC_PLANKS = registerGenericPlanks();

    /** The 44 block items, registration order (sapling-major, then log, then leaves, then planks, then the generic planks). */
    public static final List<RegistryObject<Item>> ITEMS = registerItems();

    /** The 9 sapling block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> SAPLING_ITEMS = ITEMS.subList(0, KINDS.size());
    /** The 9 log block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> LOG_ITEMS = ITEMS.subList(KINDS.size(), 2 * KINDS.size());
    /** The 9 leaves block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> LEAF_ITEMS = ITEMS.subList(2 * KINDS.size(), 3 * KINDS.size());
    /** The 9 plank block items, kind order (task gt-tree-planks — the tab walks). */
    public static final List<RegistryObject<Item>> PLANK_ITEMS = ITEMS.subList(3 * KINDS.size(), 4 * KINDS.size());
    /** The 8 generic plank block items, upstream meta order (task planks-blockification — the tab walks). */
    public static final List<RegistryObject<Item>> GENERIC_PLANK_ITEMS = ITEMS.subList(4 * KINDS.size(), ITEMS.size());

    /** The registry id of one family member: {@code <snake>_sapling} / {@code _log} / {@code _leaves} / {@code _planks}. */
    public static String path(GT6TreeKind aKind, String aSuffix) {
        return aKind.snake() + aSuffix;
    }

    private static List<RegistryObject<Block>> registerSaplings() {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (int i = 0; i < KINDS.size(); i++) {
            GT6TreeKind tKind = KINDS.get(i);
            rList.add(BLOCKS_REG.register(path(tKind, "_sapling"),
                    () -> new GT6TreeSaplingBlock(tKind, GT6Worldgen.treeConfiguredKey(tKind.snake()))));
        }
        return List.copyOf(rList);
    }

    private static List<RegistryObject<Block>> registerLogs() {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (GT6TreeKind tKind : KINDS) {
            rList.add(BLOCKS_REG.register(path(tKind, "_log"), () -> new GT6TreeLogBlock(tKind)));
        }
        return List.copyOf(rList);
    }

    private static List<RegistryObject<Block>> registerLeaves() {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (GT6TreeKind tKind : KINDS) {
            rList.add(BLOCKS_REG.register(path(tKind, "_leaves"), () -> new GT6TreeLeavesBlock(tKind)));
        }
        return List.copyOf(rList);
    }

    /** The 9 planks (task gt-tree-planks): the 2x1 crafting-table plank face per species. */
    private static List<RegistryObject<Block>> registerPlanks() {
        List<RegistryObject<Block>> rList = new ArrayList<>(KINDS.size());
        for (GT6TreeKind tKind : KINDS) {
            rList.add(BLOCKS_REG.register(path(tKind, "_planks"), () -> new gregtech6.block.tree.GT6PlankBlock(gregtech6.block.tree.GT6PlankBlock.HARDNESS)));
        }
        return List.copyOf(rList);
    }

    /** The 8 generic planks (task planks-blockification): the BlockTreePlanks metas 8-15 face, one cube per row. */
    private static List<RegistryObject<Block>> registerGenericPlanks() {
        List<RegistryObject<Block>> rList = new ArrayList<>(GENERIC_PLANK_ROWS.size());
        for (GenericPlank tRow : GENERIC_PLANK_ROWS) {
            float tHardness = tRow.hardness();
            rList.add(BLOCKS_REG.register(tRow.path(), () -> new gregtech6.block.tree.GT6PlankBlock(tHardness)));
        }
        return List.copyOf(rList);
    }

    private static List<RegistryObject<Item>> registerItems() {
        List<RegistryObject<Item>> rList = new ArrayList<>(4 * KINDS.size() + GENERIC_PLANK_ROWS.size());
        List<RegistryObject<Block>>[] tFamilies = new List[] {SAPLINGS, LOGS, LEAVES, PLANKS};
        for (List<RegistryObject<Block>> tFamily : tFamilies) {
            for (int i = 0; i < tFamily.size(); i++) {
                int tIndex = i;
                RegistryObject<Block> tBlock = tFamily.get(tIndex);
                // task easter-s3-xmas-seasonal — the Blue Spruce rows carry the Christmas-in-July
                // tooltip item (upstream BlockTreePlanks2.java:96-97 + its seven family twins;
                // the meta==0 guard collapses: the per-pair item IS the Blue Spruce row). The
                // other four upstream carriers (the fireproof twins + the Blue Spruce beams)
                // have no port domain (GT6BeamKind = the vanilla-subset 8).
                boolean tXmas = KINDS.get(tIndex) == GT6TreeKind.BLUE_SPRUCE;
                rList.add(ITEMS_REG.register(tBlock.getId().getPath(),
                        tXmas ? () -> new GT6TreeXmasItem(tBlock.get(), new Item.Properties())
                              : () -> new BlockItem(tBlock.get(), new Item.Properties())));
            }
        }
        // task planks-blockification — the generic plank BlockItems (the retired
        // wood-planks-register prefix-item ids now place cubes; plain BlockItems, the
        // Xmas face is the Blue Spruce row only, BlockTreePlanks2.java:96-97)
        for (int i = 0; i < GENERIC_PLANK_ROWS.size(); i++) {
            GenericPlank tRow = GENERIC_PLANK_ROWS.get(i);
            RegistryObject<Block> tBlock = GENERIC_PLANKS.get(i);
            rList.add(ITEMS_REG.register(tRow.path(), () -> new BlockItem(tBlock.get(), new Item.Properties())));
        }
        return List.copyOf(rList);
    }

    // ------------------------------------------------------------------ lifecycle

    private GT6TreeBlocks() {
    }

    /** FMLConstructModEvent = the first mod-bus lifecycle stage (GTGrassBlocks.onModConstruct shape). */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
        //? if forge {
        net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
        //?} else {
        /*net.minecraftforge.eventbus.api.IEventBus tModBus =
                net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
         *///?}
        BLOCKS_REG.register(tModBus);
        ITEMS_REG.register(tModBus);
    }

    /** The tab joins (GTGrassBlocks.onBuildTabContents shape): logs+planks = building, saplings+leaves = natural. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
        if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (RegistryObject<Item> tItem : LOG_ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
            for (RegistryObject<Item> tItem : PLANK_ITEMS) { // task gt-tree-planks — the vanilla planks tab row
                aEvent.accept(new ItemStack(tItem.get()));
            }
            for (RegistryObject<Item> tItem : GENERIC_PLANK_ITEMS) { // task planks-blockification — the same tab row
                aEvent.accept(new ItemStack(tItem.get()));
            }
        } else if (aEvent.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            for (RegistryObject<Item> tItem : SAPLING_ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
            for (RegistryObject<Item> tItem : LEAF_ITEMS) {
                aEvent.accept(new ItemStack(tItem.get()));
            }
        }
    }
}
