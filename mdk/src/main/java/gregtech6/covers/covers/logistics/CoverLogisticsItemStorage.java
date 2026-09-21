package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The Filtered Logistics Export Bus (Item) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsItemStorage.java (:41-107, upstream item id 1095), task
 * p33-logistics-covers-12. The Core registers the covered-face adjacency as an item
 * EXPORT source (:382-392), tiered by the priority bits; the filter stack joins the
 * network-wide protected set (the Dump exclusion).
 */
public class CoverLogisticsItemStorage extends AbstractCoverLogisticsFiltered {

	public static final CoverLogisticsItemStorage INSTANCE = new CoverLogisticsItemStorage();

	public CoverLogisticsItemStorage() {
		super(FILTER_KEY_ITEM);
	}

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/item/export"); // upstream :106
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/item/export"); // upstream :106
		 *///?}
	}
}
