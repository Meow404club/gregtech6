package gregtech6.datagen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * The set of material prefix items both client providers consume, as
 * (prefix, material, itemId) triples.
 *
 * <p>Primary source is the registration bridge index ({@link GTMaterialItems#items()}) — by
 * construction exactly the registered set, and the item id is taken from the registered
 * {@link RegistryObject#getId()} so model ids cannot drift from registry ids. The criteria walk
 * below is the headless fallback replicating the bridge filter chain in the same order
 * (GTMaterialItems.registerItems: MATERIAL_ARRAY ascending-mID order, same-name re-registration
 * merge via MaterialRegistry.get (MaterialRegistry.java:182-185), isGeneratingItem
 * (OreDictPrefix.java:285), per-prefix and per-id dedup).
 */
final class GT6DatagenItems {

    /** One datagen-able material prefix item. */
    record Entry(OreDictPrefix prefix, OreDictMaterial material, String itemId) {}

    private GT6DatagenItems() {
    }

    /** All registered material prefix items; registration bridge data when available, walk otherwise. */
    static List<Entry> collect() {
        Map<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> tIndex = GTMaterialItems.items();
        if (!tIndex.isEmpty()) {
            List<Entry> rEntries = new ArrayList<>(tIndex.size());
            for (Map.Entry<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> tPair : tIndex.entrySet()) {
                rEntries.add(new Entry(tPair.getKey().prefix(), tPair.getKey().material(), tPair.getValue().getId().getPath()));
            }
            return rEntries;
        }
        return walk();
    }

    /** Fallback enumeration, filter chain identical to GTMaterialItems.registerItems. */
    private static List<Entry> walk() {
        List<Entry> rEntries = new ArrayList<>();
        Set<String> tIds = new HashSet<>();
        for (OreDictPrefix tPrefix : whitelistPrefixes()) {
            Set<OreDictMaterial> tSeen = new HashSet<>();
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                // Same merge as the bridge: alias slots collapse onto the registration target
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
                if (tMaterial == null || tMaterial.mID < 0) continue;
                if (!tSeen.add(tMaterial)) continue;
                if (!tPrefix.isGeneratingItem(tMaterial)) continue;
                String tId = MaterialPrefixItem.snakeCase(tPrefix.mNameInternal) + "_" + MaterialPrefixItem.snakeCase(tMaterial.mNameInternal);
                if (!tIds.add(tId)) continue;
                rEntries.add(new Entry(tPrefix, tMaterial, tId));
            }
        }
        return rEntries;
    }

    /** The phase-2 whitelist prefix handles (ADR-P2-3), resolved after OP.init(). */
    private static List<OreDictPrefix> whitelistPrefixes() {
        return List.of(
            Objects.requireNonNull(OP.ingot, "OP.ingot not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.dust, "OP.dust not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.gem, "OP.gem not initialised - ConstructMod segment missing?"),
            Objects.requireNonNull(OP.plate, "OP.plate not initialised - ConstructMod segment missing?"));
    }
}
