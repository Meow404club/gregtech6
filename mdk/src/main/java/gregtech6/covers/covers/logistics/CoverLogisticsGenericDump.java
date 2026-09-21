package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;

/**
 * The Logistics Dump Bus (Item) — 1.20.1 port of gregapi/cover/covers/
 * CoverLogisticsGenericDump.java (:31-40, upstream item id 1099), task
 * p33-logistics-covers-12. The Core's LAST routing arm: generic item storage drains
 * into the dump targets (:478-494), excluded for every item in the network-wide
 * protected set {@code tFilteredFor} (all set filters across every Export/Import/
 * Storage cover and storage endpoint). No priority lane (:37 upstream usePriorities=F).
 */
public class CoverLogisticsGenericDump extends AbstractCoverAttachmentLogistics {

	public static final CoverLogisticsGenericDump INSTANCE = new CoverLogisticsGenericDump();

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		//? if forge {
		return new ResourceLocation("gt6", "block/logistics/generic/dump"); // upstream :39
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "block/logistics/generic/dump"); // upstream :39
		 *///?}
	}

	@Override
	public boolean usePriorities() {
		return false; // upstream :37
	}
}
