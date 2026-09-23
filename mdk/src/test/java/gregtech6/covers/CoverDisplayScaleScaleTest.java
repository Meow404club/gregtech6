package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverDisplayEnergy;
import gregtech6.covers.covers.CoverScaleEnergy;
import gregtech6.covers.covers.CoverScaleProgress;
import gregtech6.util.UT6;

/**
 * The display/scale acceptance tables (task p35-covers-display-scale-6) — the energy
 * display (:36-68), the progress sensor (:35-51) and the energy sensor (:36-53), plus
 * the Scale base emission machinery (:39-69):
 *
 * <ul>
 * <li>the gauge formula (:44) and the scale formulas (:42/:44) — the empty/full/mid
 *     rows over the oven's energy and progress lanes, including the mMinEnergy
 *     normalisation of the :1018/:1019 mapping;</li>
 * <li>the OPPOSITION pins (互反对拍) — the same host quantity lands on OPPOSITE lanes
 *     (the display paints the visual lane, the sensor writes the value lane), the
 *     display never touches the host's redstone exits (the attachment default passes
 *     the machine default) while the sensor's value IS the exit (the :60-67 pair), and
 *     the two output modes (the cutter strong bit, the screwdriver invert bit) flip
 *     the exits without touching the value lane;</li>
 * <li>the sprite picks — the 11 pre-composited gauge plates, the two sensor circuits.</li>
 * </ul>
 */
public class CoverDisplayScaleScaleTest extends GTCoverTestBase {

	private static final byte FACE = (byte) net.minecraft.core.Direction.UP.get3DDataValue(); // 1
	private static final byte MACHINE_DEFAULT = 3; // a fake "the machine itself emits 3" default

	private static boolean installOn(TileEntityOvenCoverProbe aOven, ICover aCover) {
		CoverRegistry.put(COVER_ITEMS[FACE], aCover);
		return aOven.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true);
	}

	// ---------------------------------------------------------------------------
	// the placement gates — the machine forms admit, the plain coverable refuses
	// ---------------------------------------------------------------------------

	@Test
	public void gatesAdmitMachinesAndRefusePlainHosts() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverDisplayEnergy()), "the oven admits the energy display");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		assertTrue(installOn(tOven, new CoverScaleEnergy()), "the oven admits the energy sensor");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		assertTrue(installOn(tOven, new CoverScaleProgress()), "the oven admits the progress sensor");
		tOven.setCoverItem(FACE, ItemStack.EMPTY, null, true, false);
		// the plain coverable host (a non machine form) refuses all three
		CoverDisplayScaleControllerTest.PlainProbe tPlain = new CoverDisplayScaleControllerTest.PlainProbe();
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverDisplayEnergy());
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true), "the plain host refuses the display");
		assertNull(tPlain.getCovers(), "no store behind the refused display");
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverScaleEnergy());
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true), "the plain host refuses the energy sensor");
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverScaleProgress());
		assertFalse(tPlain.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true), "the plain host refuses the progress sensor");
	}

	// ---------------------------------------------------------------------------
	// the energy display — the :44 gauge formula
	// ---------------------------------------------------------------------------

	@Test
	public void energyDisplayGaugeFormula() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverDisplayEnergy()), "install accepted");
		CoverData tData = tOven.getCovers();
		// empty: stored 0 -> visual 0 (and the NBT omits the zero lane)
		tData.tickPost(10, true, false, false);
		assertEquals(0, tData.mVisuals[FACE], ":44 — stored 0 reads gauge 0");
		// the input-max oven: stored == capacity 64 -> gauge 10
		tOven.mEnergy = 64;
		tData.tickPost(11, true, false, false);
		assertEquals(10, tData.mVisuals[FACE], ":44 — full reads gauge 10");
		// stored half capacity: 9 - clamp((64-32)*9/64, 0..8) = 9 - clamp(4.5 -> 4) = 5
		tOven.mEnergy = 32;
		tData.tickPost(12, true, false, false);
		assertEquals(5, tData.mVisuals[FACE], ":44 — the half tank sits mid-gauge");
		// a sliver: 9 - clamp((64-1)*9/64=8, 0..8) = 1
		tOven.mEnergy = 1;
		tData.tickPost(13, true, false, false);
		assertEquals(1, tData.mVisuals[FACE], ":44 — a sliver reads gauge 1");
		// the client tick never paints
		tOven.mEnergy = 64;
		tData.tickPost(14, false, false, false);
		assertEquals(1, tData.mVisuals[FACE], "the client-side tick left the stale gauge");
		// the sprite pick rides the CURRENT gauge level (the pre-composited plates)
		assertEquals("gt6:block/energy_display/1", new CoverDisplayEnergy().getCoverTextureSurface(FACE, tData).toString());
		tOven.mEnergy = 0;
		tData.tickPost(15, true, false, false);
		assertEquals("gt6:block/energy_display/0", new CoverDisplayEnergy().getCoverTextureSurface(FACE, tData).toString(), "the empty gauge plate");
		assertEquals("gt6:block/energy_display/5", CoverDisplayEnergy.spriteOf(5).toString(), "the mid gauge plate exists");
		assertEquals("gt6:block/energy_display/10", CoverDisplayEnergy.spriteOf(10).toString(), "the full gauge plate exists");
		assertTrue(new CoverDisplayEnergy().needsVisualsSaved(FACE, tData), "the display base :30 — the gauge saves");
	}

	// ---------------------------------------------------------------------------
	// the scales — the :42/:44 formulas + the mMinEnergy normalisation
	// ---------------------------------------------------------------------------

	@Test
	public void scaleProgressFormulaAndNormalisation() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverScaleProgress()), "install accepted");
		CoverData tData = tOven.getCovers();
		// idle: progress 0/max 0 -> value 0
		tData.tickPost(10, true, false, false);
		assertEquals(0, tData.mValues[FACE], ":42 — idle reads 0");
		// done: progress >= max -> 15
		tOven.mProgress = 100;
		tOven.mMaxProgress = 100;
		tData.tickPost(11, true, false, false);
		assertEquals(15, tData.mValues[FACE], ":42 — done reads 15");
		// mid: max 100 progress 50 -> 14 - clamp(50*14/100=7) = 7
		tOven.mProgress = 50;
		tData.tickPost(12, true, false, false);
		assertEquals(7, tData.mValues[FACE], ":42 — the halfway batch reads 7");
		// the :1018/:1019 normalisation — mMinEnergy 16 folds the raw lanes: value = divup(50,16)=4, max = max(1, divup(100,16)=7)
		// -> 14 - clamp((7-4)*14/7=6) = 8
		tOven.mMinEnergy = 16;
		tData.tickPost(13, true, false, false);
		assertEquals(8, tData.mValues[FACE], ":1018 — the normalised pair reads 8");
		tOven.mMinEnergy = 0;
		// the successful batch pins the value at full (the :1018 success clamp)
		tOven.mProgress = 0;
		tOven.mSuccessful = true;
		tData.tickPost(14, true, false, false);
		assertEquals(15, tData.mValues[FACE], ":1018 — the successful batch reads the max");
		tOven.mSuccessful = false;
		// the value writes sync with the block update (the T third argument — the change fires the host mark)
		int tBefore = tOven.mChangedCount;
		tOven.mProgress = 20;
		tData.tickPost(15, true, false, false);
		assertTrue(tOven.mChangedCount > tBefore, "the scale write fires the host block update");
		assertEquals("gt6:block/progress_redstone/circuit", new CoverScaleProgress().getCoverTextureSurface(FACE, tData).toString(), ":50 — the sensor art");
	}

	@Test
	public void scaleEnergyFormulaMatchesTheGaugeShape() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverScaleEnergy()), "install accepted");
		CoverData tData = tOven.getCovers();
		// empty 0, full 15, the mid rows on the 14-step scale
		tData.tickPost(10, true, false, false);
		assertEquals(0, tData.mValues[FACE], ":44 — empty reads 0");
		tOven.mEnergy = 64;
		tData.tickPost(11, true, false, false);
		assertEquals(15, tData.mValues[FACE], ":44 — full reads 15");
		tOven.mEnergy = 32;
		tData.tickPost(12, true, false, false);
		assertEquals(7, tData.mValues[FACE], ":44 — 14 - clamp(32*14/64=7) = 7");
		tOven.mEnergy = 1;
		tData.tickPost(13, true, false, false);
		assertEquals(1, tData.mValues[FACE], ":44 — a sliver reads 1");
		assertEquals("gt6:block/energy_redstone/circuit", new CoverScaleEnergy().getCoverTextureSurface(FACE, tData).toString(), ":52 — the sensor art");
	}

	// ---------------------------------------------------------------------------
	// the OPPOSITION pins — opposite lanes, opposite redstone faces
	// ---------------------------------------------------------------------------

	@Test
	public void displayAndSensorLandOnOppositeLanes() {
		TileEntityOvenCoverProbe tDisplayOven = leveledOven();
		assertTrue(installOn(tDisplayOven, new CoverDisplayEnergy()), "the display install");
		TileEntityOvenCoverProbe tSensorOven = leveledOven();
		assertTrue(installOn(tSensorOven, new CoverScaleEnergy()), "the sensor install");
		// the same host state...
		tDisplayOven.mEnergy = 32;
		tSensorOven.mEnergy = 32;
		tDisplayOven.getCovers().tickPost(10, true, false, false);
		tSensorOven.getCovers().tickPost(10, true, false, false);
		// ...lands on OPPOSITE lanes: the display paints the VISUAL lane (gauge 5), the
		// sensor writes the VALUE lane (scale 7), the other lane stays untouched
		assertEquals(5, tDisplayOven.getCovers().mVisuals[FACE], "the display drives the visual lane");
		assertEquals(0, tDisplayOven.getCovers().mValues[FACE], "the display never touches the value lane");
		assertEquals(7, tSensorOven.getCovers().mValues[FACE], "the sensor drives the value lane");
		assertEquals(0, tSensorOven.getCovers().mVisuals[FACE], "the sensor never touches the visual lane (bit 0 modes aside)");
	}

	@Test
	public void displayLeavesTheRedstoneExitsAloneAndTheSensorDrivesThem() {
		TileEntityOvenCoverProbe tDisplayOven = leveledOven();
		assertTrue(installOn(tDisplayOven, new CoverDisplayEnergy()), "the display install");
		tDisplayOven.mEnergy = 64;
		tDisplayOven.getCovers().tickPost(10, true, false, false);
		// the display cover has no emission override — the host exit passes the machine default
		assertEquals(MACHINE_DEFAULT, tDisplayOven.getRedstoneOutWeak(UT6.OPOS[FACE], MACHINE_DEFAULT), "the display exit is the untouched default");
		assertEquals(MACHINE_DEFAULT, tDisplayOven.getRedstoneOutStrong(UT6.OPOS[FACE], MACHINE_DEFAULT), "the strong exit too");
		// the sensor cover's exit IS its value lane
		TileEntityOvenCoverProbe tSensorOven = leveledOven();
		assertTrue(installOn(tSensorOven, new CoverScaleEnergy()), "the sensor install");
		tSensorOven.mEnergy = 64;
		tSensorOven.getCovers().tickPost(11, true, false, false);
		assertEquals(15, tSensorOven.getRedstoneOutWeak(UT6.OPOS[FACE], MACHINE_DEFAULT), ":65-67 — the weak exit reads the scale");
		// the strong exit stays silent until the cutter bit arms it (:60-62)
		assertEquals(0, tSensorOven.getRedstoneOutStrong(UT6.OPOS[FACE], MACHINE_DEFAULT), ":61 — no cutter bit, no strong emission");
	}

	@Test
	public void scaleOutputModesFlipTheExitsNotTheValue() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(installOn(tOven, new CoverScaleEnergy()), "install accepted");
		ICover tSensor = tOven.getCovers().mBehaviours[FACE];
		CoverData tData = tOven.getCovers();
		tOven.mEnergy = 64;
		tData.tickPost(10, true, false, false);
		assertEquals(15, tData.mValues[FACE], "the fresh scale reads 15");
		// the screwdriver flips the invert bit (visual bit 1): the weak exit inverts, the value lane rides
		assertEquals(1000, tSensor.onToolClick(FACE, tData, ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":47 — the toggle damage");
		assertEquals(2, tData.mVisuals[FACE] & 2, ":45 — bit 1 set");
		assertEquals(15, tData.mValues[FACE], "the value lane is untouched by the mode bits");
		assertEquals(0, tOven.getRedstoneOutWeak(UT6.OPOS[FACE], MACHINE_DEFAULT), ":66 — 15 - 15 inverts to 0");
		assertEquals(1000, tSensor.onToolClick(FACE, tData, ICover.TOOL_SCREWDRIVER, 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":47 again");
		assertEquals(15, tOven.getRedstoneOutWeak(UT6.OPOS[FACE], MACHINE_DEFAULT), "the invert off restores the scale");
		// the cutter flips the strong bit (visual bit 0): the strong exit mirrors the weak
		assertEquals(1000, tSensor.onToolClick(FACE, tData, "cutter", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":42 — the toggle damage");
		assertEquals(1, tData.mVisuals[FACE] & 1, ":40 — bit 0 set");
		assertEquals(15, tOven.getRedstoneOutStrong(UT6.OPOS[FACE], MACHINE_DEFAULT), ":61 — the strong exit mirrors the weak");
		// the magnifyingglass read arm: damage 1, no lane change (:49-55) — the mode bits
		// are bit 0 (cutter) only, the screwdriver bit was toggled back off above
		assertEquals(1, tSensor.onToolClick(FACE, tData, "magnifyingglass", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), ":54 — the read arm damage");
		assertEquals(1, tData.mVisuals[FACE] & 3, "the read arm left the mode bits alone");
		// unknown ids answer 0
		assertEquals(0, tSensor.onToolClick(FACE, tData, "crowbar-substitute-not", 0, null, false, FACE, 0.5F, 0.5F, 0.5F), "unknown ids are not answered");
		// the mode bits save with the cover (the :69 gate)
		assertTrue(tSensor.needsVisualsSaved(FACE, tData), ":69 — the mode bits save");
		// the attachment flags (:35-40): non-opaque, non-sealable, clicks fall through
		assertFalse(tSensor.isOpaque(FACE, tData), ":39");
		assertFalse(tSensor.isSealable(FACE, tData), ":40");
		assertFalse(tSensor.interceptClickLeft(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":37");
		assertFalse(tSensor.interceptClickRight(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":38");
	}
}
