package gregtech6.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.block.stone.GTStoneSlabBlock;
import gregtech6.block.stone.StoneVariant;

/**
 * Registration home of the GT6 stone-slab universe (task debt-slab-gap): the upstream
 * {@code mSlabs[0]} (bottom-slab) face of every stone family — one slab per (stone,
 * variant) pair over the SAME {@link GTStoneBlocks#registrationOrder()} walk, <b>272
 * per-pair Block+BlockItem registrations</b> in the GTStoneBlocks shape (the
 * {@code @EventBusSubscriber(MOD)} listener, the LOW-priority BLOCK-then-ITEM segments,
 * the defensive per-registry dedup).
 *
 * <p><b>id scheme</b>: the slab id is the paired full block's composite path suffixed
 * {@code _slab} ({@link #slabPath}) — {@code gt6:<snake>_slab} for variant 0,
 * {@code gt6:<snake>_<variant>_slab} for the other 15 (the vanilla {@code <block>_slab}
 * naming idiom; the GTStoneBlocks.path composition is the single upstream definition
 * site, only the suffix is added here).
 *
 * <p>Upstream evidence: the slab blocks exist for EVERY meta of the family
 * (BlockMetaType.java:74-81 {@code maxMeta()} count), and the generic conversion rows
 * (BlockMetaType.java:89/:93) walk all 16 metas — so the full 272 is the faithful
 * surface, not a COBBL-only cut. No creative tab (the GTStoneBlocks no-tab status quo).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GTStoneSlabBlocks {

    /** Runtime index composite-path -> slab block handle (registration order, stone-major variant-major). */
    private static final Map<String, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    /** Runtime index composite-path -> slab block-item handle (registration order, stone-major variant-major). */
    private static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    /** Defensive dedup across re-fired RegisterEvents (the GTMaterialBlocks.java:67 ADR-P2-2 fix 1 shape), per registry. */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS = new HashSet<>();

    private GTStoneSlabBlocks() {
    }

    /**
     * The slab id of a (stone, variant) pair: the paired full block's composite path
     * (variant 0 = the bare snake, the other 15 = {@code snake_<variant>}) suffixed
     * {@code _slab} — the single definition site.
     */
    public static String slabPath(String aStoneSnake, StoneVariant aVariant) {
        return GTStoneBlocks.path(aStoneSnake, aVariant) + "_slab";
    }

    /** RegisterEvent, LOW priority: BLOCK segment -> ITEM segment (the GTStoneBlocks.java:160 shape). */
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
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) { // the same 272-pair walk
            String tPath = slabPath(tKey.stone().snake(), tKey.variant());
            ResourceLocation tLoc = gtId(tPath);
            if (!REGISTERED_BLOCK_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate stone slab block id {}", tLoc);
                continue;
            }
            //? if forge {
            RegistryObject<Block> tHandle = RegistryObject.create(tLoc, Registries.BLOCK, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Block, Block> tHandle =
                net.neoforged.neoforge.registries.DeferredHolder.create(Registries.BLOCK, tLoc);
            //21.1: RegistryObject died with the class; DeferredHolder.create(key, id) is the same lazy handle.
            *///?}
            event.register(Registries.BLOCK, tLoc, () -> new GTStoneSlabBlock(tKey.stone().snake(),
                    tKey.variant(), tKey.stone().material().get(),
                    tKey.stone().hardnessMultiplier(), tKey.stone().resistanceMultiplier()));
            BLOCKS.put(tPath, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone slab blocks (17 stones x 16 variants, per-pair)", BLOCKS.size());
        GT6Mod.LOGGER.info("stone slab block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) { // same walk, same order
            String tPath = slabPath(tKey.stone().snake(), tKey.variant());
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
            // The composed-name BlockItem (the GTStoneBlocks form): the stack name
            // delegates to the slab block compose (GTStoneSlabBlock.getName).
            event.register(Registries.ITEM, tLoc, () ->
                    new gregtech6.block.GTComposedNameItem(BLOCKS.get(tPath).get(), new Item.Properties()));
            ITEMS.put(tPath, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} GT6 stone slab block items (one per variant slab)", ITEMS.size());
        GT6Mod.LOGGER.info("stone slab block item registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** gt6 namespaced id (GTStoneBlocks.java:227 form). */
    private static ResourceLocation gtId(String path) {
        //? if forge {
        return new ResourceLocation("gt6", path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath("gt6", path); // 21.1: the (namespace, path) ctor is private
        *///?}
    }

    /** The slab block handle of a composite path ({@link #slabPath} form), or null before registration. */
    public static RegistryObject<Block> block(String aPath) {
        return BLOCKS.get(aPath);
    }

    /** The slab block handle of a (stone, variant) pair, or null before registration. */
    public static RegistryObject<Block> block(String aStoneSnake, StoneVariant aVariant) {
        return BLOCKS.get(slabPath(aStoneSnake, aVariant));
    }

    /** The slab block-item handle of a composite path ({@link #slabPath} form), or null before registration. */
    public static RegistryObject<Item> item(String aPath) {
        return ITEMS.get(aPath);
    }

    /** The slab block-item handle of a (stone, variant) pair, or null before registration. */
    public static RegistryObject<Item> item(String aStoneSnake, StoneVariant aVariant) {
        return ITEMS.get(slabPath(aStoneSnake, aVariant));
    }

    /** All 272 registered slab blocks, registration order — the datagen walk (the GTStoneBlocks.blockArray shape). */
    public static List<Block> blockArray() {
        List<Block> rBlocks = new ArrayList<>();
        for (RegistryObject<Block> tHandle : BLOCKS.values()) rBlocks.add(tHandle.get());
        return rBlocks;
    }
}
