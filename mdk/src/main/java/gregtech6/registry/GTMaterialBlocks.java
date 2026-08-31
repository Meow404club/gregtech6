package gregtech6.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.item.GTMaterialPrefixBlockItem;

/**
 * Registration home of the material prefix BLOCK universe (task p8-prefixblock-registry,
 * ADR 2026-08-31-p8-prefixblocks): the seven upstream storage-block prefixes
 * (Loader_PrefixBlocks.java:40-46 blockRaw/Gem/Dust/Ingot/Plate/PlateGem/Solid) as per-pair
 * blocks + block items + one creative tab per non-empty prefix, in one self-contained
 * {@code @EventBusSubscriber(MOD)} listener (ADR-P3-4 — GT6Mod/GTModBusListener stay
 * untouched; GTBarrels.java:62 shape). The enumeration mirrors
 * {@link GTMaterialItems#registrationOrder()} with the block path swapped in:
 * {@link OreDictPrefix#VALUES} x MATERIAL_ARRAY, alias-merged and pair-deduped, gated by
 * {@link OreDictPrefix#isGeneratingItem} (upstream OreDictPrefix.java:364-366 — the same
 * criterion as the item side, since the block* prefixes carry setCondition(basePrefix)
 * chains, upstream OP.java:345-351). Census (this card's pinned yardstick,
 * GTMaterialBlocksRegistrationTest): 3773 pairs.
 *
 * <p>Ids follow the single composition rule {@code gt6:<prefix_snake>_<material_snake>}
 * ({@link GTMaterialItems#itemIdOf}) with the same <b>first-wins</b> collision policy
 * (GTMaterialItems.java:53-57), plus a defensive REGISTERED_IDS set across re-fired
 * RegisterEvents (ADR-P2-2 fix 1). The {@link #get} seam is the query path for later
 * consumers (cokeoven resolver fallback, /gt6multiblock input); the render card
 * (p8-prefixblock-render) consumes {@link #registrationOrder()}/{@link #blockArray()} for
 * its datagen. Material system initialisation is NOT owned here — GTMaterialItems
 * .initMaterials() already runs at ConstructMod (GT6Mod wiring, OP.init idempotent).
 *
 * <p><b>Intermediate state (ADR ④)</b>: between this card and the render card the blocks
 * exist WITHOUT blockstate/model/loot JSONs — a declared working state; dedicated servers
 * never bake models, so runServer stays zero-ERROR. No model/loot/tint datagen belongs to
 * this class (ExistingFileHelper would fail the BlockItem parent resolution that needs the
 * block model in the same provider pass — the ADR veto).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GTMaterialBlocks {

    /** Runtime index (prefix, material) -> block item handle, in registration order (creative tab display order). */
    private static final Map<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> INDEX = new LinkedHashMap<>();
    /** Runtime index (prefix, material) -> block handle (item suppliers + the render card's blockArray). */
    private static final Map<GTMaterialItems.PrefixMaterial, RegistryObject<Block>> BLOCK_INDEX = new LinkedHashMap<>();
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1). */
    private static final Set<ResourceLocation> REGISTERED_IDS = new HashSet<>();

    private GTMaterialBlocks() {
    }

    /** RegisterEvent, LOW priority: BLOCK segment -> ITEM segment -> CREATIVE_MODE_TAB segment (GTMaterialItems:90-96 shape). */
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.BLOCK) {
            registerBlocks(event);
        } else if (event.getRegistryKey() == Registries.ITEM) {
            registerItems(event);
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            registerCreativeTabs(event);
        }
    }

    /**
     * The upstream storage-block prefixes: exactly the seven prefixes upstream constructs a
     * PrefixBlock_ for in Loader_PrefixBlocks.java:40-46, verbatim file order; OP field
     * references keep it compile-checked. NOT a port-authority invention — casing/crate
     * families (:48-67) stay pooled (ADR notes).
     */
    public static List<OreDictPrefix> blockPathPrefixes() {
        return List.of(gregapi.data.OP.blockRaw, gregapi.data.OP.blockGem, gregapi.data.OP.blockDust,
                gregapi.data.OP.blockIngot, gregapi.data.OP.blockPlate, gregapi.data.OP.blockPlateGem,
                gregapi.data.OP.blockSolid);
    }

    /**
     * The single enumeration source for "what material prefix blocks exist": the registration
     * bridge and the render card's datagen all take this one walk. Order:
     * {@link OreDictPrefix#VALUES} x MATERIAL_ARRAY ascending-mID (GTMaterialItems:106-108 mirror).
     */
    public static List<GTMaterialItems.PrefixMaterial> registrationOrder() {
        return enumerate().kept();
    }

    /** One enumeration pass: kept pairs (registration order) plus the first-wins/deferral bookkeeping. */
    record Enumeration(List<GTMaterialItems.PrefixMaterial> kept, int duplicatePairDrops, int duplicateIdDrops,
            int nonBlockPathPrefixes) {}

    /** Full walk over ALL prefixes; non-block-path prefixes are skipped whole (aggregate counter). */
    static Enumeration enumerate() {
        Set<OreDictPrefix> tBlockPath = new HashSet<>(blockPathPrefixes());
        List<GTMaterialItems.PrefixMaterial> tRaw = new ArrayList<>();
        int tNonBlockPath = 0, tDuplicatePairs = 0;
        Set<GTMaterialItems.PrefixMaterial> tSeenPairs = new HashSet<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (!tBlockPath.contains(tPrefix)) {tNonBlockPath++; continue;} // item/MTE/parse-only path, not this card
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                // Same-name re-registrations live in two array slots; the block belongs to the
                // current registration target (MaterialRegistry.java:182-185, the item-side form).
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
                if (tMaterial == null || tMaterial.mID < 0) continue;
                GTMaterialItems.PrefixMaterial tPair = new GTMaterialItems.PrefixMaterial(tPrefix, tMaterial);
                if (!tSeenPairs.add(tPair)) {tDuplicatePairs++; continue;} // alias slot: the target already owns the block
                if (!tPrefix.isGeneratingItem(tMaterial)) continue; // upstream OreDictPrefix.java:364-366
                tRaw.add(tPair);
            }
        }
        GTMaterialItems.FirstWins tFirstWins = GTMaterialItems.firstWinsById(tRaw); // the shared first-wins rule
        return new Enumeration(tFirstWins.kept(), tDuplicatePairs, tFirstWins.drops(), tNonBlockPath);
    }

    private static void registerBlocks(RegisterEvent event) {
        long tStart = System.nanoTime();
        Enumeration tSet = enumerate();
        int tTotal = 0;
        for (GTMaterialItems.PrefixMaterial tPair : tSet.kept()) {
            ResourceLocation tLoc = gtId(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
            if (!REGISTERED_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate block id {}", tLoc);
                continue;
            }
            OreDictPrefix tPrefix = tPair.prefix();
            RegistryObject<Block> tHandle = RegistryObject.create(tLoc, Registries.BLOCK, "gt6");
            event.register(Registries.BLOCK, tLoc, () -> new GTMaterialPrefixBlock(tPrefix, tPair.material()));
            BLOCK_INDEX.put(tPair, tHandle);
            tTotal++;
        }
        GT6Mod.LOGGER.info("GT6 registered {} material prefix blocks in total (first-wins id drops: {}, duplicate pair drops: {}, non-block-path prefixes skipped: {})",
                tTotal, tSet.duplicateIdDrops(), tSet.duplicatePairDrops(), tSet.nonBlockPathPrefixes());
        LOGGER.info("prefix block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        int tTotal = 0;
        for (GTMaterialItems.PrefixMaterial tPair : enumerate().kept()) { // same walk, same order
            ResourceLocation tLoc = gtId(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
            if (!REGISTERED_IDS.add(tLoc)) { // the block id collided first — skip its item too
                continue;
            }
            RegistryObject<Block> tBlock = BLOCK_INDEX.get(tPair); // resolved: BLOCK fires before ITEM
            OreDictPrefix tPrefix = tPair.prefix();
            event.register(Registries.ITEM, tLoc,
                    () -> new GTMaterialPrefixBlockItem(new Item.Properties(), tPrefix, tPair.material(), tBlock.get()));
            INDEX.put(tPair, RegistryObject.create(tLoc, Registries.ITEM, "gt6"));
            tTotal++;
        }
        GT6Mod.LOGGER.info("GT6 registered {} material prefix block items in total", tTotal);
        LOGGER.info("prefix block item registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** The creative-visible block prefixes: block path, not HIDDEN (PrefixBlockItem.java:65), holding >= 1 block; loader order. */
    public static List<OreDictPrefix> tabPrefixes() {
        List<OreDictPrefix> rTabs = new ArrayList<>();
        for (OreDictPrefix tPrefix : blockPathPrefixes()) {
            if (tPrefix.contains(TD.Creative.HIDDEN)) continue;
            for (GTMaterialItems.PrefixMaterial tPair : registrationOrder()) {
                if (tPair.prefix() == tPrefix) {rTabs.add(tPrefix); break;} // >= 1 block, empty families get no tab
            }
        }
        return rTabs;
    }

    /** One tab per creative-visible block prefix (upstream PrefixBlockItem.java:66-67, one per block* prefix). */
    private static void registerCreativeTabs(RegisterEvent event) {
        long tStart = System.nanoTime();
        Map<OreDictPrefix, List<GTMaterialItems.PrefixMaterial>> tGroups = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : registrationOrder()) tGroups.computeIfAbsent(tPair.prefix(), tKey -> new ArrayList<>()).add(tPair);
        int tColumn = 0;
        int tTabCount = 0;
        for (OreDictPrefix tPrefix : tabPrefixes()) {
            List<GTMaterialItems.PrefixMaterial> tTabItems = tGroups.get(tPrefix);
            RegistryObject<Item> tIcon = INDEX.get(tTabItems.get(0)); // upstream icon = the first family member (CreativeTab.java:28-35 form)
            String tSnake = GTMaterialItems.snakeCase(tPrefix.mNameInternal);
            final int tColumnF = tColumn;
            event.register(Registries.CREATIVE_MODE_TAB, gtId(tSnake), () ->
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, tColumnF)
                    .title(Component.translatable("itemGroup.gt6." + tSnake)) // upstream CreativeTab(mNameInternal, mNameCategory...), PrefixBlockItem.java:66
                    .icon(() -> new ItemStack(tIcon.get()))
                    .displayItems((tParameters, tOutput) -> {
                        for (GTMaterialItems.PrefixMaterial tPair : tTabItems) {
                            if (tPair.material().mHidden) continue; // PrefixBlockItem.java:78, SHOW_HIDDEN_MATERIALS=false default
                            tOutput.accept(new ItemStack(INDEX.get(tPair).get()));
                        }
                    })
                    .build());
            tColumn++;
            tTabCount++;
        }
        GT6Mod.LOGGER.info("GT6 registered {} per-prefix block creative tabs", tTabCount);
        LOGGER.info("prefix block tab registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** gt6 namespaced id (the GTMaterialItems:281 form; Forge backports a removal deprecation onto it). */
    private static ResourceLocation gtId(String path) {
        return new ResourceLocation("gt6", path);
    }

    /** Query API: the block ITEM handle of a prefix x material pair, or null if not registered (GTMaterialItems:287 mirror). */
    public static RegistryObject<Item> get(OreDictPrefix prefix, OreDictMaterial material) {
        return INDEX.get(new GTMaterialItems.PrefixMaterial(prefix, material));
    }

    /** All registered block-item handles (unmodifiable, registration order). */
    public static Map<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> items() {
        return Collections.unmodifiableMap(INDEX);
    }

    /** All registered blocks as an array, for the render card's datagen (blockstate/block/model walks). */
    public static Block[] blockArray() {
        return BLOCK_INDEX.values().stream().map(RegistryObject::get).toArray(Block[]::new);
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");
}
