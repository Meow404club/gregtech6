/*
 * Offline tests for task p21-paintable-tint-render: the GTMachinePaintTint value mapping —
 * the pure tintARGB seam over card_A's PAINT model data. ModelData/ModelProperty are pure
 * data classes (Guava only), offline-testable per GTOfflineRenderTestBase; the world-side
 * BlockColor lambda is covered on its null-guard arms (a live Level+BE needs a running
 * client — the RCON visual chain is the optional live check, not a gate).
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraftforge.client.model.data.ModelData;

class GTMachinePaintTintTest extends GTOfflineRenderTestBase {

	/** A painted colour as card_A stores it (0xRRGGBB, the direct-storage ruling). */
	private static final int PAINT_RED = 0xFF0000;

	/** The 03 base supplies PAINT while painted — the exact derived-snapshot shape. */
	private static ModelData paintedData(int aRGB) {
		return GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(aRGB))
				.build();
	}

	/** Acceptance 2a: a present PAINT property returns the paint as opaque ARGB (index 0). */
	@Test
	void paintedSnapshotTintsWithTheStoredColour() {
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), 0),
				"red paint 0xFF0000 -> ARGB 0xFFFF0000");
		assertEquals(0xFF202020, GTMachinePaintTint.tintARGB(paintedData(0x202020), 0),
				"the CS DYE_Black row value tints dark gray");
	}

	/** Acceptance 2b: an absent PAINT property (unpainted, ModelData.EMPTY) returns white. */
	@Test
	void unpaintedSnapshotReturnsWhite() {
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(ModelData.EMPTY, 0),
				"white 0xFFFFFF bound full-alpha = 0xFFFFFFFF");
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(null, 0),
				"the null-snapshot guard is white too");
		// The class-doc identity: full-alpha white IS the vanilla -1 no-tint sentinel.
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, 0),
				"0xFFFFFFFF == -1 — white and the no-tint sentinel are the same int");
	}

	/** Acceptance 2c: a non-zero tint index is never tinted, even on a painted snapshot. */
	@Test
	void nonZeroTintIndexIsNeverTinted() {
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), 1));
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), 3));
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, 1));
	}

	/** The BlockColor lambda's guard arms (null level/pos and a non-zero index return no tint). */
	@Test
	void blockColorLambdaGuardArms() {
		assertEquals(-1, GTMachinePaintTint.blockColor().getColor(null, null, null, 1),
				"a non-zero index short-circuits before any world access");
		assertEquals(-1, GTMachinePaintTint.blockColor().getColor(null, null, null, 0),
				"the null level/pos arm is the no-tint sentinel (== full-alpha white)");
	}
}
