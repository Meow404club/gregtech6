package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The CPU Control display — 1.20.1 port of gregapi/cover/covers/CoverLogisticsDisplayCPUControl.java
 * (:33-55, upstream item id 1086), task p33-logistics-covers-12. The Core drives the
 * value (redstone 0..15) and visual (bar 0..10) lanes per second (:306-307).
 */
public class CoverLogisticsDisplayCPUControl extends AbstractCoverLogisticsDisplay {

	public static final CoverLogisticsDisplayCPUControl INSTANCE = new CoverLogisticsDisplayCPUControl();

	public static final String FAMILY = "cpu_control";

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return displayTexture(aData, aCoverSide, FAMILY); // upstream :38 — the BlockTextureMulti row over sTextures
	}
}
