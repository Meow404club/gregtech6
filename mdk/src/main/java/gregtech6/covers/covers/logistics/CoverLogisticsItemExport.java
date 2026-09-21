package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The Filtered Logistics Export Bus (Item) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsItemExport.java (:41-107, upstream item id 1093), task
 * p33-logistics-covers-12. The Core registers the covered-face adjacency as an item
 * EXPORT source (:358-369), tiered by the priority bits; the filter stack joins the
 * network-wide protected set (the Dump exclusion).
 */
public class CoverLogisticsItemExport extends AbstractCoverLogisticsFiltered {

	public static final CoverLogisticsItemExport INSTANCE = new CoverLogisticsItemExport();

	public CoverLogisticsItemExport() {
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
