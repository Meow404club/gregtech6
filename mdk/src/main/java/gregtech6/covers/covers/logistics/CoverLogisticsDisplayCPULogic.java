package gregtech6.covers.covers.logistics;

import net.minecraft.resources.ResourceLocation;

import gregtech6.covers.CoverData;

/**
 * The CPU Logic display — 1.20.1 port of gregapi/cover/covers/CoverLogisticsDisplayCPULogic.java
 * (:33-55, upstream item id 1086), task p33-logistics-covers-12. The Core drives the
 * value (redstone 0..15) and visual (bar 0..10) lanes per second (:301-302).
 */
public class CoverLogisticsDisplayCPULogic extends AbstractCoverLogisticsDisplay {

	public static final CoverLogisticsDisplayCPULogic INSTANCE = new CoverLogisticsDisplayCPULogic();

	public static final String FAMILY = "cpu_logic";

	@Override
	public ResourceLocation getCoverTextureSurface(byte aCoverSide, CoverData aData) {
		return displayTexture(aData, aCoverSide, FAMILY); // upstream :38 — the BlockTextureMulti row over sTextures
	}
}
