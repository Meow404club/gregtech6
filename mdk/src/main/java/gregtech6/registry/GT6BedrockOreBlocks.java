package gregtech6.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.item.GTMaterialPrefixBlockItem;

/**
 * Registration home of the GT6 BEDROCK-ore block band (task p31-bedrock-ore-worldgen
 * spec ①): the two upstream forms split per-pair over the bedrock row-table material
 * axis — {@code gt6:ore_bedrock_<material>} (OP.oreBedrock, the large form) and
 * {@code gt6:ore_small_bedrock_<material>} (OP.oreSmall, the upstream name segment
 * "gt.meta.ore.small.bedrock"), Loader_Ores.java:44-45 verbatim columns on every block
 * ({@link GTBedrockOreBlock}).
 *
 * <p><b>The material axis M = 45</b>: the DISTINCT materials of the upstream BedrockOres
 * row table (Loader_Worldgen.java:725-776 — 46 port rows, gold.a/gold.b sharing MT.Au),
 * the spec's "+约 106 块" corrected to its table-driven face: 45 x 2 = 90 blocks. The
 * MD.HEX-gated hexorium row (Loader_Worldgen.java:772, its 5-sub-material display block)
 * rides the SAME mod-gated compat pool the 53-axis ruling set for the p30 ore universe —
 * the port's row table simply omits it. Deliberately NOT the 53-material small-ore axis:
 * 24 of the 45 table materials (Tungstate..Syrmorite) sit outside it, and the row table
 * must resolve every row to blocks.
 *
 * <p>NO creative tab (upstream: PrefixBlockItem.java:62-64 hides every ore-prefix block
 * except the stone normal family — both bedrock prefixes hidden; the items exist for the
 * RM.BedrockOreList display rows and the future drill card). NO loot tables
 * ({@code noLootTable} = the upstream Drops_None). NO blockstate-model atlas face: plain
 * bedrock cubes, the shared model section in GT6OreBlockStates' bedrock band.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BedrockOreBlocks {

    /** One row-table material slot — the supplier indirection keeps static init pre-OP.init-safe. */
    public interface MaterialSlot extends java.util.function.Supplier<OreDictMaterial> {}

    /**
     * The bedrock row-table material axis — the 45 DISTINCT materials of the upstream
     * BedrockOres table, first-appearance order (Loader_Worldgen.java:725-771; the two
     * gold rows share MT.Au). Suppliers again (post-OP.init resolution — the
     * {@code GT6OreBlocks.OreFamily} lesson: this class loads at the mod-construct scan,
     * BEFORE OP.init dereferences anything).
     */
    public static final List<MaterialSlot> MATERIALS = List.of(
        () -> MT.Diamond,                        // ore.bedrock.diamond, :725
        () -> MT.OREMATS.Tungstate,              // :726
        () -> MT.OREMATS.Ferberite,              // :727
        () -> MT.OREMATS.Wolframite,             // :728
        () -> MT.OREMATS.Stolzite,               // :729
        () -> MT.OREMATS.Scheelite,              // :730
        () -> MT.OREMATS.Huebnerite,             // :731
        () -> MT.OREMATS.Russellite,             // :732
        () -> MT.OREMATS.Pinalite,               // :733
        () -> MT.OREMATS.Uraninite,              // :734
        () -> MT.OREMATS.Pitchblende,            // :735
        () -> MT.Au,                             // :736-737 gold.a/gold.b share one material
        () -> MT.OREMATS.Cooperite,              // :738
        () -> MT.Cu,                             // :739
        () -> MT.Monazite,                       // :740
        () -> MT.OREMATS.Powellite,              // :741
        () -> MT.OREMATS.Bastnasite,             // :742
        () -> MT.OREMATS.Arsenopyrite,           // :743 (the stibnite-named arsenopyrite row)
        () -> MT.Redstone,                       // :744
        () -> MT.V2O5,                           // :745
        () -> MT.OREMATS.Galena,                 // :746
        () -> MT.Coal,                           // :747
        () -> MT.Graphite,                       // :748
        () -> MT.OREMATS.Stibnite,               // :749
        () -> MT.Fe2O3,                          // :750
        () -> MT.OREMATS.Sphalerite,             // :751
        () -> MT.OREMATS.Smithsonite,            // :752
        () -> MT.OREMATS.Pentlandite,            // :753
        () -> MT.Niter,                          // :754
        () -> MT.OREMATS.Bauxite,                // :755
        () -> MT.OREMATS.Cassiterite,            // :756
        () -> MT.OREMATS.Chalcopyrite,           // :757
        () -> MT.VoidQuartz,                     // :758
        () -> MT.Glowstone,                      // :759
        () -> MT.Gloomstone,                     // :760
        () -> MT.Efrine,                         // :761
        () -> MT.NetherQuartz,                   // :762
        () -> MT.Firestone,                      // :763
        () -> MT.AncientDebris,                  // :764
        () -> MT.Nq,                             // :765
        () -> MT.Desh,                           // :766
        () -> MT.Dolamide,                       // :767
        () -> MT.Adamantine,                     // :768
        () -> MT.Octine,                         // :769
        () -> MT.Syrmorite                       // :770
    );

    /** The form flag: false = the large bedrock ore, true = the small bedrock ore. */
    public static final boolean LARGE = false, SMALL = true;

    /** The registration walk unit: (form, material). */
    public record BedrockKey(boolean small, OreDictMaterial material) {}

    /** The material axis resolved (mID > 0, alias-deduped, MATERIALS order) — the census walk. */
    public static List<OreDictMaterial> materialAxis() {
        Set<OreDictMaterial> tSeen = new HashSet<>();
        List<OreDictMaterial> rAxis = new ArrayList<>(MATERIALS.size());
        for (MaterialSlot tSlot : MATERIALS) {
            OreDictMaterial tMaterial = tSlot.get();
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
            if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial)) continue;
            rAxis.add(tMaterial);
        }
        return rAxis;
    }

    /** The single definition site of the per-pair id scheme: {@code ore[_small]_bedrock_<material>}. */
    public static String path(boolean aSmall, OreDictMaterial aMaterial) {
        return (aSmall ? "ore_small_bedrock_" : "ore_bedrock_")
                + GTMaterialItems.snakeCase(aMaterial.mNameInternal);
    }

    /** The 2 x M registration walk: form-major (large, small), material-major (axis order). Offline-safe. */
    public static List<BedrockKey> registrationOrder() {
        List<OreDictMaterial> tAxis = materialAxis();
        List<BedrockKey> rOrder = new ArrayList<>(2 * tAxis.size());
        for (boolean tSmall : new boolean[] {LARGE, SMALL}) {
            for (OreDictMaterial tMaterial : tAxis) {
                rOrder.add(new BedrockKey(tSmall, tMaterial));
            }
        }
        return rOrder;
    }

    /** Runtime index (form, material) -> block handle, in registration order. */
    //? if forge {
    private static final java.util.Map<BedrockKey, RegistryObject<Block>> BLOCKS = new java.util.LinkedHashMap<>();
    /** Runtime index (form, material) -> block-item handle, in registration order. */
    private static final java.util.Map<BedrockKey, RegistryObject<Item>> ITEMS = new java.util.LinkedHashMap<>();
    //?} else {
    /*private static final java.util.Map<BedrockKey, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> BLOCKS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<BedrockKey, net.neoforged.neoforge.registries.DeferredHolder<Item, Item>> ITEMS = new java.util.LinkedHashMap<>();
     *///?}
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1), per registry. */
    private static final Set<ResourceLocation> REGISTERED_BLOCK_IDS = new HashSet<>();
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS = new HashSet<>();

    private GT6BedrockOreBlocks() {
    }

    /** RegisterEvent, LOW priority: BLOCK segment -> ITEM segment (GT6OreBlocks.java:408-418 shape; no tab segment). */
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
        for (BedrockKey tKey : registrationOrder()) {
            ResourceLocation tLoc = gtId(path(tKey.small(), tKey.material()));
            if (!REGISTERED_BLOCK_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate bedrock ore block id {}", tLoc);
                continue;
            }
            //? if forge {
            RegistryObject<Block> tHandle = RegistryObject.create(tLoc, Registries.BLOCK, "gt6");
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Block, Block> tHandle =
                net.neoforged.neoforge.registries.DeferredHolder.create(Registries.BLOCK, tLoc);
            //21.1: RegistryObject died with the class; DeferredHolder.create(key, id) is the same lazy handle.
            *///?}
            event.register(Registries.BLOCK, tLoc, () ->
                    new GTBedrockOreBlock(tKey.small() ? OP.oreSmall : OP.oreBedrock, tKey.material(), tKey.small()));
            BLOCKS.put(tKey, tHandle);
        }
        GT6Mod.LOGGER.info("GT6 registered {} bedrock ore blocks (2 forms x {} row-table materials, per-pair)",
                BLOCKS.size(), materialAxis().size());
        GT6Mod.LOGGER.info("bedrock ore block registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    private static void registerItems(RegisterEvent event) {
        long tStart = System.nanoTime();
        for (BedrockKey tKey : registrationOrder()) { // same walk, same order
            ResourceLocation tLoc = gtId(path(tKey.small(), tKey.material()));
            //? if forge {
            RegistryObject<Block> tBlock = BLOCKS.get(tKey); // null when the BLOCK id collided and was skipped
            //?} else {
            /*net.neoforged.neoforge.registries.DeferredHolder<Block, Block> tBlock = BLOCKS.get(tKey);
            *///?}
            if (tBlock == null || !REGISTERED_ITEM_IDS.add(tLoc)) { // per-registry dedup
                continue;
            }
            event.register(Registries.ITEM, tLoc, () ->
                    new GTMaterialPrefixBlockItem(new Item.Properties(),
                            tKey.small() ? OP.oreSmall : OP.oreBedrock, tKey.material(), tBlock.get()));
            //? if forge {
            ITEMS.put(tKey, RegistryObject.create(tLoc, Registries.ITEM, "gt6"));
            //?} else {
            /*ITEMS.put(tKey, net.neoforged.neoforge.registries.DeferredHolder.create(Registries.ITEM, tLoc));
            *///?}
        }
        GT6Mod.LOGGER.info("GT6 registered {} bedrock ore block items", ITEMS.size());
        GT6Mod.LOGGER.info("bedrock ore item registration took {} ms", (System.nanoTime() - tStart) / 1_000_000);
    }

    /** gt6 namespaced id (the GTStoneBlocks.java fork form). */
    private static ResourceLocation gtId(String path) {
        //? if forge {
        return new ResourceLocation("gt6", path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath("gt6", path); // 21.1: the (namespace, path) ctor is private
        *///?}
    }

    /** Query API: the block handle of a (form, material) pair, or null if not registered. */
    //? if forge {
    public static RegistryObject<Block> get(boolean aSmall, OreDictMaterial aMaterial) {
        return BLOCKS.get(new BedrockKey(aSmall, aMaterial));
    }

    /** All registered block handles (unmodifiable, registration order) — the datagen band's walk. */
    public static java.util.Map<BedrockKey, RegistryObject<Block>> blocks() {
        return java.util.Collections.unmodifiableMap(BLOCKS);
    }

    /** All registered block-item handles (unmodifiable, registration order). */
    public static java.util.Map<BedrockKey, RegistryObject<Item>> items() {
        return java.util.Collections.unmodifiableMap(ITEMS);
    }
    //?} else {
    /*public static net.neoforged.neoforge.registries.DeferredHolder<Block, Block> get(boolean aSmall, OreDictMaterial aMaterial) {
        return BLOCKS.get(new BedrockKey(aSmall, aMaterial));
    }

    // All registered block handles (unmodifiable, registration order) — the datagen band's walk.
    public static java.util.Map<BedrockKey, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> blocks() {
        return java.util.Collections.unmodifiableMap(BLOCKS);
    }

    // All registered block-item handles (unmodifiable, registration order).
    public static java.util.Map<BedrockKey, net.neoforged.neoforge.registries.DeferredHolder<Item, Item>> items() {
        return java.util.Collections.unmodifiableMap(ITEMS);
    }
     *///?}
}
