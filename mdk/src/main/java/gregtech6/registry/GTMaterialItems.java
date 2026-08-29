package gregtech6.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.item.MaterialPrefixItem;

/**
 * Registration bridge (ADR-P2-2 / ADR-P2-3): floods the GT6 material system during
 * FMLConstructModEvent.enqueueWork, then injects the whitelist prefix items directly into the
 * RegisterEvent stream (LOW priority, GTCEu GTRegistrate.java:148-151 precedent) plus one
 * creative tab in the same listener. FMLCommonSetupEvent then completes the alloy reverse
 * references (GT6 postInit equivalent, GT_API_Post.java:816-820) — that part lives in GT6Mod.
 *
 * <p>Item ids follow the GT6 oredict reading order: {@code gt6:<prefix_snake>_<material_snake>}
 * (e.g. gt6:ingot_iron) — deliberately the reverse of GTCEu's material_prefix shape (ADR-P2-2).
 */
public final class GTMaterialItems {

    /** Phase-2 whitelist prefixes (ADR-P2-3); their OP conditions are pure TD groups. */
    private static final List<String> WHITELIST_PREFIXES = List.of("ingot", "dust", "gem", "plate");

    /** Runtime index (prefix, material) -> handle, in registration order (creative tab display order). */
    private static final Map<PrefixMaterial, RegistryObject<Item>> INDEX = new LinkedHashMap<>();
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1). */
    private static final Set<ResourceLocation> REGISTERED_IDS = new HashSet<>();

    /** Index key: an OreDictPrefix x OreDictMaterial item pair. */
    public record PrefixMaterial(OreDictPrefix prefix, OreDictMaterial material) {}

    private GTMaterialItems() {
    }

    /** Segment 1 (FMLConstructModEvent.enqueueWork): full material system refill, registry open -> closed. */
    public static void initMaterials() {
        MaterialRegistry.INSTANCE.open(); // createMaterial with a valid ID requires open (MaterialRegistry.java:130-131)
        MT.init();  // MT.java:2695, per-generation full refill (reg0000..reg0038 + AM/ANY/TECH/OREMATS/WOODS/UNUSED)
        OP.init();  // OP.java:621, idempotent; requires PrefixRegistry open (OreDictPrefix.createPrefix :117)
        MaterialRegistry.INSTANCE.close(); // ADR-P2-2 open->closed; GTCEu CommonProxy.java:185-229 unfreeze->init->freeze isomorph
        GT6Mod.LOGGER.info("GT6 material system initialised: {} materials, {} prefixes", MaterialRegistry.INSTANCE.MATERIAL_MAP.size(), OreDictPrefix.VALUES.size());
    }

    /** Segment 2 (RegisterEvent, LOW priority): items and the creative tab, one listener for both (task card). */
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.ITEM) {
            registerItems(event);
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            registerCreativeTab(event);
        }
    }

    /**
     * Item generation criterion: {@code prefix.isGeneratingItem(material)} — upstream GT6 registers
     * its oredict entries in PrefixItem.run() with exactly this filter (PrefixItem.java:102-108;
     * OreDictPrefix.java:363-366 = forced minus blacklist minus mCondition). Materials are scanned
     * in MATERIAL_ARRAY order (= ascending mID), mirroring the upstream default material list.
     */
    private static void registerItems(RegisterEvent event) {
        long tTotal = 0;
        for (OreDictPrefix tPrefix : whitelistPrefixes()) {
            long tCount = 0;
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                // Same-name re-registrations (old ID + new ID, createMaterial NOTICE path) live in two
                // array slots; the item id must belong to the current registration target
                // (MaterialRegistry.java:182-185 = upstream OreDictMaterial.java:199-202), merging
                // deprecated aliases onto one item.
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
                final OreDictMaterial fMaterial = tMaterial;
                if (fMaterial == null || fMaterial.mID < 0) continue;
                if (!tPrefix.isGeneratingItem(fMaterial)) continue; // PrefixItem.java:104
                PrefixMaterial tKey = new PrefixMaterial(tPrefix, fMaterial);
                if (INDEX.containsKey(tKey)) continue; // alias slot: the registration target already owns the item
                String tId = MaterialPrefixItem.snakeCase(tPrefix.mNameInternal) + "_" + MaterialPrefixItem.snakeCase(fMaterial.mNameInternal);
                ResourceLocation tLoc = gtId(tId);
                if (!REGISTERED_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                    GT6Mod.LOGGER.warn("GT6 skipped duplicate item id {}", tLoc);
                    continue;
                }
                RegistryObject<Item> tHandle = RegistryObject.create(tLoc, Registries.ITEM, "gt6"); // RegistryObject.java:62
                event.register(Registries.ITEM, tLoc, () -> new MaterialPrefixItem(new Item.Properties(), tPrefix, fMaterial)); // RegisterEvent.java:54-63
                INDEX.put(tKey, tHandle);
                tCount++;
                tTotal++;
            }
            GT6Mod.LOGGER.info("GT6 registered {} '{}' items", tCount, tPrefix.mNameInternal);
        }
        GT6Mod.LOGGER.info("GT6 registered {} material prefix items in total", tTotal);
    }

    /** One "GT Materials" tab (ADR-P2-3); icon = ingot_iron, display filter mirrors PrefixItem.getSubItems (PrefixItem.java:114). */
    private static void registerCreativeTab(RegisterEvent event) {
        RegistryObject<Item> tIcon = get(OP.ingot, MT.Iron); // MT.java:2611 Iron = Fe; INGOTS-group materials get ingots
        event.register(Registries.CREATIVE_MODE_TAB, gtId("materials"), () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0) // vanilla CreativeModeTab.java:46-48, public builder(Row, int)
                .title(Component.translatable("itemGroup.gt6.materials"))
                .icon(() -> new ItemStack(tIcon.get()))
                .displayItems((tParameters, tOutput) -> {
                    for (Map.Entry<PrefixMaterial, RegistryObject<Item>> tEntry : INDEX.entrySet()) {
                        if (tEntry.getKey().prefix().contains(TD.Creative.HIDDEN)) continue; // PrefixItem.java:114 filter chain
                        if (tEntry.getKey().material().mHidden) continue;
                        tOutput.accept(new ItemStack(tEntry.getValue().get())); // CreativeModeTab.Output.accept, CreativeModeTab.java:262-264
                    }
                })
                .build());
        GT6Mod.LOGGER.info("GT6 creative tab 'gt6:materials' registered with {} items", INDEX.size());
    }

    /** gt6 namespaced id. The two-arg constructor is the vanilla 1.20.1 form (ResourceLocation.java:37); Forge userdev backports a removal deprecation onto it. */
    private static ResourceLocation gtId(String path) {
        return new ResourceLocation("gt6", path);
    }

    /** The whitelist prefix handles, resolved after OP.init() (OP.java:242/254/258/279). */
    private static List<OreDictPrefix> whitelistPrefixes() {
        return List.of(
            Objects.requireNonNull(OP.ingot, "OP.ingot not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.dust, "OP.dust not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.gem, "OP.gem not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.plate, "OP.plate not initialised - ConstructMod segment missing?"));
    }

    /** Query API for later cards: the handle of a prefix x material item, or null if not registered. */
    public static RegistryObject<Item> get(OreDictPrefix prefix, OreDictMaterial material) {
        return INDEX.get(new PrefixMaterial(prefix, material));
    }

    /** All registered handles (unmodifiable, registration order). */
    public static Map<PrefixMaterial, RegistryObject<Item>> items() {
        return Collections.unmodifiableMap(INDEX);
    }

    /** All registered items as an array, for ItemColors.register(ItemColor, ItemLike...) (client seam). */
    public static Item[] itemArray() {
        return INDEX.values().stream().map(RegistryObject::get).toArray(Item[]::new);
    }
}
