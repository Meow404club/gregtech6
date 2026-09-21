package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;

/**
 * The Generic Logistics Export Bus — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsGenericExport.java (:31-41, upstream item id 1096), task
 * p33-logistics-covers-12. The unfiltered bus: the covered-face adjacency exports BOTH
 * items and fluids (:409-416 of the Core — the fluid arm skips when the adjacency is a
 * semi-filtered ITileEntityLogisticsSemiFilteredItem), tiered by the priority bits; the
 * target stacksize lane (bits 2-8) rides the item half (:394).
 */
public class CoverLogisticsGenericExport extends AbstractCoverAttachmentLogistics {

	public static final CoverLogisticsGenericExport INSTANCE = new CoverLogisticsGenericExport();

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/generic/export"); // upstream :40
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/generic/export"); // upstream :40
		 *///?}
	}

	@Override
	public boolean useTargetStackSize() {
		return true; // upstream :38
	}
}
