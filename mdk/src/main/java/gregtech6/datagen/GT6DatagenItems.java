package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * The set of material prefix items both client providers consume, as
 * (prefix, material, itemId) triples.
 *
 * <p>Primary source is the registration bridge index ({@link GTMaterialItems#items()}) — by
 * construction exactly the registered set, and the item id is taken from the registered
 * {@link RegistryObject#getId()} so model ids cannot drift from registry ids.
 *
 * <p>The headless fallback is {@link GTMaterialItems#registrationOrder()} — since the
 * p3-fullprefix-creativetab criterion convergence there is ONE enumeration walk (universe
 * x isGeneratingItem, PrefixItem.java:104; same-name merge via MaterialRegistry.get,
 * MaterialRegistry.java:182-185; first-wins id dedup), and this class is a thin adapter over
 * it. The bridge and datagen can no longer drift apart by construction.
 */
final class GT6DatagenItems {

    /** One datagen-able material prefix item. */
    record Entry(OreDictPrefix prefix, OreDictMaterial material, String itemId) {}

    private GT6DatagenItems() {
    }

    /** All registered material prefix items; registration bridge data when available, shared walk otherwise. */
    static List<Entry> collect() {
        Map<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> tIndex = GTMaterialItems.items();
        if (!tIndex.isEmpty()) {
            List<Entry> rEntries = new ArrayList<>(tIndex.size());
            for (Map.Entry<GTMaterialItems.PrefixMaterial, RegistryObject<Item>> tPair : tIndex.entrySet()) {
                rEntries.add(new Entry(tPair.getKey().prefix(), tPair.getKey().material(), tPair.getValue().getId().getPath()));
            }
            return rEntries;
        }
        List<Entry> rEntries = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            rEntries.add(new Entry(tPair.prefix(), tPair.material(), GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())));
        }
        return rEntries;
    }
}
