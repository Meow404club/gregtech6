package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The CPU Storage display — 1.20.1 port of gregapi/cover/covers/CoverLogisticsDisplayCPUStorage.java
 * (:33-55, upstream item id 1086), task p33-logistics-covers-12. The Core drives the
 * value (redstone 0..15) and visual (bar 0..10) lanes per second (:311-312).
 */
public class CoverLogisticsDisplayCPUStorage extends AbstractCoverLogisticsDisplay {

	public static final CoverLogisticsDisplayCPUStorage INSTANCE = new CoverLogisticsDisplayCPUStorage();

	public static final String FAMILY = "cpu_storage";

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return displayTexture(aData, aCoverSide, FAMILY); // upstream :38 — the BlockTextureMulti row over sTextures
	}
}
