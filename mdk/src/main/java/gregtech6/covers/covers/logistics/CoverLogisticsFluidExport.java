package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The Filtered Logistics Export Bus (Fluid) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsFluidExport.java (upstream item id 1090), task p33-logistics-covers-12.
 * The Core registers the covered-face adjacency as a fluid EXPORT source (:325-335),
 * tiered by the priority bits of the value lane.
 */
public class CoverLogisticsFluidExport extends AbstractCoverLogisticsFluid {

	public static final CoverLogisticsFluidExport INSTANCE = new CoverLogisticsFluidExport();

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/fluid/export"); // upstream :111
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/fluid/export"); // upstream :111
		 *///?}
	}
}
