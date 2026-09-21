package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The Filtered Logistics Storage Bus (Fluid) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsFluidStorage.java (upstream item id 1092), task p33-logistics-covers-12.
 * The Core registers the covered-face adjacency into the fluid STORAGE tier lists
 * (:347-357) — both an export source (the paired table) and a defrag/backup target.
 */
public class CoverLogisticsFluidStorage extends AbstractCoverLogisticsFluid {

	public static final CoverLogisticsFluidStorage INSTANCE = new CoverLogisticsFluidStorage();

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/fluid/storage"); // upstream (the family texture path)
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/fluid/storage"); // upstream (the family texture path)
		 *///?}
	}
}
