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
import gregtech6.worldgen.GT6Worldgen;

/**
 * Registration home of the GT6 tree universe (task p30-w6-t1-trees-nine): <b>27 per-pair
 * Block+BlockItem registrations</b> — sapling/log/leaves x the 9 {@link GT6TreeKind} rows
 * (the upstream Saplings_AB meta 0-7 + Saplings_CD meta 0 face, Loader_Woods.java:67-70,
 * fully expanded per the P8 ADR ④ / GTStoneBlocks / GTGrassBlocks precedent: the canopy
 * Feature, the loot tables and the lang rows all key per-species identities). The id scheme
 * is the single-source snake composition {@code <kind.snake>_sapling/_log/_leaves}
 * (GTGrassBlocks.PATHS rule), and the growsThrough face hands each sapling its
 * {@link GT6Worldgen#TREE_CONFIGURED_KEYS} entry.
 *
 * <p>Creative tabs: logs join {@code BUILDING_BLOCKS} (the upstream tabBlock join,
 * BlockBase.java:63), saplings+leaves join {@code NATURAL_BLOCKS} (the vanilla
 * sapling/leaves tab, CreativeModeTabs.java:635) — wired through the platform
 * {@code BuildCreativeModeTabContentsEvent} (the GTGrassBlocks.onBuildTabContents shape).
 *
 * <p>Self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event (GTGrassBlocks.onModConstruct shape; ADR-P3-4: GT6Mod stays untouched).
 * The KJS registration face is declared DEFER to the kjs-binding card (the task-card
 * declaration); the worldgen JSON face is datapack-native and needs zero adaptation.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6TreeBlocks {

    public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
    public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

    /** The 9 species rows in upstream meta order (Saplings_AB meta 0-7, then CD meta 0). */
    public static final List<GT6TreeKind> KINDS = List.of(GT6TreeKind.values());

    /** The 9 saplings, kind order. */
    public static final List<RegistryObject<Block>> SAPLINGS = registerSaplings();
    /** The 9 logs, kind order. */
    public static final List<RegistryObject<Block>> LOGS = registerLogs();
    /** The 9 leaves, kind order. */
    public static final List<RegistryObject<Block>> LEAVES = registerLeaves();

    /** The 27 block items, registration order (sapling-major, then log, then leaves). */
    public static final List<RegistryObject<Item>> ITEMS = registerItems();

    /** The 9 sapling block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> SAPLING_ITEMS = ITEMS.subList(0, KINDS.size());
    /** The 9 log block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> LOG_ITEMS = ITEMS.subList(KINDS.size(), 2 * KINDS.size());
    /** The 9 leaves block items, kind order (the tab walks). */
    public static final List<RegistryObject<Item>> LEAF_ITEMS = ITEMS.subList(2 * KINDS.size(), 3 * KINDS.size());

    /** The registry id of one family member: {@code <snake>_sapling} / {@code _log} / {@code _leaves}. */
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

    private static List<RegistryObject<Item>> registerItems() {
        List<RegistryObject<Item>> rList = new ArrayList<>(3 * KINDS.size());
        List<RegistryObject<Block>>[] tFamilies = new List[] {SAPLINGS, LOGS, LEAVES};
        for (List<RegistryObject<Block>> tFamily : tFamilies) {
            for (int i = 0; i < tFamily.size(); i++) {
                int tIndex = i;
                RegistryObject<Block> tBlock = tFamily.get(tIndex);
                rList.add(ITEMS_REG.register(tBlock.getId().getPath(),
                        () -> new BlockItem(tBlock.get(), new Item.Properties())));
            }
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

    /** The tab joins (GTGrassBlocks.onBuildTabContents shape): logs = building, saplings+leaves = natural. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
        if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (RegistryObject<Item> tItem : LOG_ITEMS) {
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
