package gregtech6.crop.behavior;

import gregtech6.crop.GT6CropBlockEntity;

/**
 * The watering arm  --  card cbc-4-crop-tools hook ①. The upstream water-can face
 * (Behavior_Watering_Crops.java:49-57) and the water-holding-tank face
 * (TileEntityBase08FluidContainer.java:251-258, the jug/measuring-pot
 * {@code canWaterCrops} arm) share the identical math; this is its port.
 *
 * <p><b>Verbatim semantics</b>: hydration storage caps at 200; one drain unit is
 * {@code tDrained = min((200 - hydration) / 10, availableWater)} millibuckets and each
 * flowed mB pays 10 hydration (:253/:256  --  the GT6 1:10 boost over the IC2 1:1
 * {@code applyHydration} carrier face). The integer-division floor is upstream too: a
 * tile at 195 cannot top off the last 5 (the /10 rounds to 0, nothing flows, the click
 * still belonged to the crop). The 1.7.10 water-can behavior was never attached to any
 * item (dead INSTANCE), so the live carrier here is the jug face  --  its item landed on
 * main AFTER this card's cbc-1 baseline, the one-line {@code waterCrop} hookup rides the
 * merge queue (declared at the card report).
 */
public final class CropWateringBehavior {

	private CropWateringBehavior() {}

	/** The upstream :253 expression verbatim  --  mB that flow for the given state. */
	public static int drainForHydration(int aHydration, int aAvailableWater) {
		return Math.min((200 - aHydration) / 10, aAvailableWater);
	}

	/**
	 * The apply face  --  :254-256: anything above zero flows through the tile's
	 * {@code applyHydration} (the decompiled :1198-1211 200-cap carrier cbc-1 built)
	 * at the 1:10 rate; true when water actually moved.
	 */
	public static boolean waterCrop(GT6CropBlockEntity aCrop, int aAvailableWater) {
		int tDrained = drainForHydration(aCrop.storageWater(), aAvailableWater);
		if (tDrained <= 0) return false;
		return aCrop.applyHydration(tDrained * 10);
	}
}
