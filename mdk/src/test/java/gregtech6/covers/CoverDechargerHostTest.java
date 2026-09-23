package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverDisplayEnergy;
import gregtech6.covers.covers.CoverScaleEnergy;
import gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity;
import gregapi.data.TD;

/**
 * The decharger cover-host arming (task p36-energy-zpm-dechargers, the card acceptance ⑤
 * offline half): the p35 covers card documented the non-zero gauge face as STRUCTURALLY
 * UNREACHABLE — "no coverable capacitor host exists in this port yet". The ZPM decharger
 * IS that host (upstream the display/scale energy covers admit the
 * {@code ITileEntityEnergyDataCapacitor} battery boxes): the admission opens, both covers
 * install, and the gauge/scale read NON-ZERO off the internal buffer lane
 * ({@code mEnergy}/{@code capacity()} — the covers never read the inventory slot, so the
 * offline fixture carries no artifact).
 */
public class CoverDechargerHostTest extends GTCoverTestBase {

	private static final byte FACE = (byte) Direction.UP.get3DDataValue(); // 1

	static BlockEntityType<GT6ZpmDechargerBlockEntity> sDechType;
	static final BlockPos POS = new BlockPos(4, 2, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildDechargerFixture() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// the offline boot noise; registries are ready
		}
		BlockEntityType<GT6ZpmDechargerBlockEntity>[] tHolder = (BlockEntityType<GT6ZpmDechargerBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ZpmDechargerBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sDechType = tHolder[0];
	}

	/** The Electric-row decharger fixture (the QU in lane set off the STONE fallback). */
	private GT6ZpmDechargerBlockEntity decharger() {
		GT6ZpmDechargerBlockEntity rDech = new GT6ZpmDechargerBlockEntity(sDechType, POS, Blocks.STONE.defaultBlockState(),
				TD.Energy.EU);
		rDech.mEnergyType = TD.Energy.QU;
		return rDech;
	}

	@Test
	public void theAdmissionOpensAndTheGaugeReadsNonZero() {
		GT6ZpmDechargerBlockEntity tDech = decharger();
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverDisplayEnergy());
		assertTrue(tDech.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true),
				"the decharger admits the energy display (the capacitor host)");
		CoverData tData = tDech.getCovers();
		// the empty buffer reads gauge 0
		tData.tickPost(10, true, false, false);
		assertEquals(0, tData.mVisuals[FACE], ":44 — stored 0 reads gauge 0");
		// the NON-ZERO face, unreachable since p35: the mid buffer reads a live mid gauge
		tDech.mEnergy = tDech.capacity() / 2;
		tData.tickPost(11, true, false, false);
		assertEquals(5, tData.mVisuals[FACE], ":44 — the half buffer reads gauge 5");
		assertEquals("gt6:block/energy_display/5", new CoverDisplayEnergy().getCoverTextureSurface(FACE, tData).toString(),
				"the mid gauge plate rides the live lane");
		// full buffer: gauge 10
		tDech.mEnergy = tDech.capacity();
		tData.tickPost(12, true, false, false);
		assertEquals(10, tData.mVisuals[FACE], ":44 — the full buffer reads gauge 10");
	}

	@Test
	public void theScaleReadsTheCapacitorLane() {
		GT6ZpmDechargerBlockEntity tDech = decharger();
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverScaleEnergy());
		assertTrue(tDech.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true),
				"the decharger admits the energy sensor");
		CoverData tData = tDech.getCovers();
		tData.tickPost(10, true, false, false);
		assertEquals(0, tData.mValues[FACE], ":44 — the empty buffer reads scale 0");
		tDech.mEnergy = tDech.capacity() / 2;
		tData.tickPost(11, true, false, false);
		assertEquals(7, tData.mValues[FACE], ":44 — 14 - clamp((cap/2)*14/cap = 7) — the NON-ZERO scale");
		assertTrue(tData.mValues[FACE] > 0, "the live reading is non-zero (the card acceptance ⑤ carrier)");
	}

	@Test
	public void theHostHandlesTheStoreLifecycle() {
		GT6ZpmDechargerBlockEntity tDech = decharger();
		CoverRegistry.put(COVER_ITEMS[FACE], new CoverDisplayEnergy());
		assertTrue(tDech.setCoverItem(FACE, new ItemStack(COVER_ITEMS[FACE]), null, false, true), "install");
		// the store survives the empty reinstall (the 06Covers :313-:317 dissolution)
		assertTrue(tDech.setCoverItem(FACE, ItemStack.EMPTY, null, true, false), "the removal passes");
		assertFalse(tDech.hasCovers(), "the all-empty store dissolved");
	}
}
