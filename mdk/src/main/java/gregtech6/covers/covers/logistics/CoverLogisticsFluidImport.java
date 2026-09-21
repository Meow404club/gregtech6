package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The Filtered Logistics Import Bus (Fluid) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsFluidImport.java (upstream item id 1091), task p33-logistics-covers-12.
 * The Core registers the covered-face adjacency as a fluid IMPORT target (:336-346).
 */
public class CoverLogisticsFluidImport extends AbstractCoverLogisticsFluid {

	public static final CoverLogisticsFluidImport INSTANCE = new CoverLogisticsFluidImport();

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/fluid/import"); // upstream (the family texture path)
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/fluid/import"); // upstream (the family texture path)
		 *///?}
	}
}
