/*
 * Offline tests for the machine paint tint value mapping: the pure tintARGB seam over the
 * PAINT model data (task p21-paintable-tint-render) and the ROW MATERIAL fallback (task
 * p27-machine-material-tint-fidelity). ModelData/ModelProperty are pure data classes (Guava
 * only), offline-testable per GTOfflineRenderTestBase; the world-side BlockColor lambda is
 * covered on its null-guard arms (a live Level+BE needs a running client — the RCON visual
 * chain is the optional live check, not a gate).
 *
 * <p>THE SEMANTICS OF "UNPAINTED" CHANGED with the fidelity card (the expected regression
 * face, declared in the task): unpainted machines now render their NBT_MATERIAL row colour
 * (upstream MultiTileEntityClassContainer.java:51 derives NBT_COLOR from fRGBaSolid) — the
 * former white-default assertions here pin the MATERIAL-LESS arm only (barrels, MT.NULL
 * rows, vanilla states), which keeps the white identity byte-identical.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraftforge.client.model.data.ModelData;

import gregtech6.registry.GTMaterialItems;

class GTMachinePaintTintTest extends GTOfflineRenderTestBase {

	/** A painted colour as card_A stores it (0xRRGGBB, the direct-storage ruling). */
	private static final int PAINT_RED = 0xFF0000;

	@BeforeAll
	static void bootMaterials() {
		// the row materials resolve through MT.init (the GTWireTintTest shape)
		GTMaterialItems.initMaterials();
	}

	/** A painted colour as card_A stores it (0xRRGGBB, the direct-storage ruling). */
	private static ModelData paintedData(int aRGB) {
		return GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(aRGB))
				.build();
	}

	/** Acceptance: a present PAINT property returns the paint as opaque ARGB (index 0) — the spray override wins over the row material. */
	@Test
	void paintedSnapshotTintsWithTheStoredColour() {
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 0),
				"red paint 0xFF0000 -> ARGB 0xFFFF0000");
		assertEquals(0xFF202020, GTMachinePaintTint.tintARGB(paintedData(0x202020), null, 0),
				"the CS DYE_Black row value tints dark gray");
		// the spray-paint override: even a material row renders the PAINT value
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), gregapi.data.MT.Cu, 0),
				"painted wins over the row material (upstream Paintable:85 override)");
	}

	/**
	 * Acceptance (the fidelity card): an UNPAINTED machine tints with its row material —
	 * the registration derivation of MultiTileEntityClassContainer.java:51
	 * (getRGBInt over fRGBaSolid, OreDictMaterial.java:111). The representative pair of
	 * research.p27-machine-tint-reresearch: Cu orange-red, Steel gray-white.
	 */
	@Test
	void unpaintedMachineTintsWithTheRowMaterial() {
		assertEquals(0xFFFF825A, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Cu, 0),
				"the Cu row renders orange-red 255,130,90 (the copper-machine observation)");
		assertEquals(0xFF828282, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Steel, 0),
				"the Steel row renders gray-white 130,130,130 (the steel-machine observation)");
		// the encoding derivation — every arm rides fRGBaSolid exactly
		for (gregapi.oredict.OreDictMaterial tMat : java.util.List.of(gregapi.data.MT.Cu, gregapi.data.MT.Steel, gregapi.data.MT.Invar)) {
			int tColor = GTMachinePaintTint.tintARGB(null, tMat, 0);
			assertEquals(0xFF000000, tColor & 0xFF000000, tMat.mNameInternal + " binds full alpha");
			assertEquals(tMat.fRGBaSolid[0], (tColor >> 16) & 0xFF, tMat.mNameInternal + " R");
			assertEquals(tMat.fRGBaSolid[1], (tColor >> 8) & 0xFF, tMat.mNameInternal + " G");
			assertEquals(tMat.fRGBaSolid[2], tColor & 0xFF, tMat.mNameInternal + " B");
		}
		// the null-SNAPSHOT arm carries the material too (the item-half read shape)
		assertEquals(0xFFFF825A, GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Cu, 0),
				"a null snapshot is the unpainted arm, not the no-tint arm");
	}

	/**
	 * The material-LESS arm keeps the white identity (the P21 contract, byte-identical for
	 * the P23 barrel co-registration): null material AND MT.NULL rows stay the vanilla -1
	 * no-tint sentinel — the laser-style negative assertion of the task card.
	 */
	@Test
	void materialLessArmsStayTheWhiteNoTintIdentity() {
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 0),
				"white 0xFFFFFF bound full-alpha = 0xFFFFFFFF (the material-less fallback)");
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(null, null, 0),
				"the null-snapshot null-material guard is white too");
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 0),
				"0xFFFFFFFF == -1 — white and the no-tint sentinel are the same int");
		// MT.NULL rows: the material resolves white → the identity, never a colour
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.NULL, 0),
				"an MT.NULL material row stays the no-tint identity (the laser-style rows)");
	}

	/** Acceptance: a non-zero tint index is never tinted, painted or not. */
	@Test
	void nonZeroTintIndexIsNeverTinted() {
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 1));
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 3));
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 1));
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Cu, 1),
				"the row material never leaks onto an overlay index");
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
