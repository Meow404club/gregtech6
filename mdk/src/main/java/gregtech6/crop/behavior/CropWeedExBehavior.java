package gregtech6.crop.behavior;

import gregtech6.crop.GT6CropBlockEntity;

/**
 * The Weed-Ex spray arm  --  card cbc-4-crop-tools hook ②. Upstream anchor
 * GT_Spray_Bug_Item.java:65-68 ({@code getWeedExStorage} / consume one use /
 * {@code setWeedExStorage(before+100)}).
 *
 * <p><b>Cap ruling (declared)</b>: that 1.7.10 class is ENTIRELY commented out (dead
 * code  --  the whole file body sits in a block comment), so the ADR-CB1 mechanism
 * authority governs: IC2 1.12 decompiled {@code TileEntityCrop.applyWeedEx}
 * (:1213-1226)  --  manual limit 100, automatic (cropmatron) limit 150, fill-to-limit.
 * The dose stays the upstream +100; at/over the limit the spray refuses (upstream
 * :67 {@code tCropBefore <= 100} gate in spirit  --  the canonical cap clamps instead
 * of overshooting to 200). The automatic face is the same tile contract
 * ({@code applyWeedEx(dose, manual=false)}); the cropmatron machine itself is not part
 * of the A-plan.
 */
public final class CropWeedExBehavior {

	/** The upstream :68 dose (+100 per spray, both faces). */
	public static final int DOSE = 100;

	private CropWeedExBehavior() {}

	/**
	 * The manual spray  --  one use consumed upstream through
	 * {@code damageOrDechargeItem(aStack, 1, 1000, aPlayer)} (:67); here the caller pays
	 * the use, the tile fills to the manual cap 100. False = refused (cap reached),
	 * nothing consumed.
	 */
	public static boolean spray(GT6CropBlockEntity aCrop) {
		return aCrop.applyWeedEx(DOSE, true);
	}

	/** The automatic (cropmatron) face  --  the same dose under the 150 cap. */
	public static boolean sprayAutomatic(GT6CropBlockEntity aCrop) {
		return aCrop.applyWeedEx(DOSE, false);
	}
}
