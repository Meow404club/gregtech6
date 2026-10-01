package gregtech6.crop.behavior;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

import gregtech6.crop.GT6CropBlockEntity;

/**
 * The Cropnalyzer scan arm  --  card cbc-4-crop-tools hook ③. Upstream
 * Behavior_Cropnalyzer.java:68-100: first scan bumps {@code scanLevel} to 4
 * (:78-80, the seed-revelation mechanic), the readout is the four-line chat block
 * (:77 Type / :89 Plant / :94 Environment / :99 Attributes).
 *
 * <p><b>Declared cut</b>: the EU charge ({@code V[6]=4096} first scan / {@code V[3]=64}
 * repeat, :79/:82) rides the item's energy stat  --  the port has no item-EU system, so
 * the scan runs free; the constants below carry the anchor for the item's own card.
 * The chat-delivery face (upstream sendchat) is the driver's business  --  the command
 * arm (/gt6crop scan) and a future item hook both consume {@link #scan} directly.
 */
public final class CropnalyzerBehavior {

	/** Upstream :79 V[6]  --  the first-scan cost in EU (charged by the future item face). */
	public static final long COST_FIRST_SCAN = 4096;

	/** Upstream :82 V[3]  --  the repeat-scan cost in EU (charged by the future item face). */
	public static final long COST_RESCAN = 64;

	private CropnalyzerBehavior() {}

	/**
	 * The scan  --  bumps {@code scanLevel} to 4 when below (:78-80) and returns the
	 * upstream readout lines. The tile must be planted ({@code getCrop() != null};
	 * upstream reads {@code getCrop()} unguarded).
	 */
	public static List<String> scan(GT6CropBlockEntity aCrop, BlockPos aPos) {
		if (aCrop.getScanLevel() < 4) aCrop.setScanLevel(4);
		List<String> rLines = new ArrayList<>();
		rLines.add("--- X: " + aPos.getX() + " Y: " + aPos.getY() + " Z: " + aPos.getZ() + " ---");
		rLines.add("Type -- Name: " + aCrop.getCrop().name()
				+ "   Growth: " + aCrop.getStatGrowth()
				+ "   Gain: " + aCrop.getStatGain()
				+ "   Resistance: " + aCrop.getStatResistance());
		rLines.add("Plant -- Fertilizer: " + aCrop.getStorageNutrients()
				+ "   Water: " + aCrop.getStorageWater()
				+ "   Weed-Ex: " + aCrop.getStorageWeedEX());
		rLines.add("Environment -- Nutrients: " + aCrop.getTerrainNutrients()
				+ "   Humidity: " + aCrop.getTerrainHumidity()
				+ "   Air-Quality: " + aCrop.getTerrainAirQuality());
		String tAttributes = "";
		for (String tAttribute : aCrop.getCrop().attributes()) tAttributes += ", " + tAttribute;
		rLines.add("Attributes:" + tAttributes.replaceFirst(",", "")); // the :99-100 shape verbatim
		return rLines;
	}
}
