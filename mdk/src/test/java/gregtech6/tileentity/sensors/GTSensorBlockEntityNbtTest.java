package gregtech6.tileentity.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The sensor NBT round-trip (task p26-sensors-core offline acceptance — the "NBT 往返"
 * arm): every key of the double base rides the upstream verbatim name
 * (GTSensorBlockEntity.java:64-83 — CS.NBT_MODE/NBT_VISUAL/NBT_VALUE/NBT_CONNECTION/
 * NBT_REDSTONE + the SensorTE :66-87 quartet "gt.sensor.max/value/index/array"), and the
 * load face replays the upstream :61-72 branch table including the unsigned-short fold
 * ({@code unsignS}, UT.java:1228) and the two degenerate guards. The fixture is the
 * GTCoverTestBase offline-BET shape: a hand-built BlockEntityType over BRICKS so the
 * registry-backed fallback ctor never runs offline.
 *
 * <p>The base is {@link GTOfflineTestBase} — NOT the machines stratum: its boot carries
 * the 1.21.1 {@code unfreezeBlockEntityTypeRegistry()} re-open, without which the FML
 * pre-frozen BET registry rejects the fixture {@code build(null)} ("Registry is already
 * frozen" — the machines-base order-lottery, first exposed by this class in isolation;
 * the machines stratum relies on an earlier-in-suite unfreezer surviving).
 */
public class GTSensorBlockEntityNbtTest extends GTOfflineTestBase {

	static final BlockPos SENSOR_POS = new BlockPos(3, 2, 1);

	static BlockEntityType<GT6ProgressmeterBlockEntity> sSensorType;

	@BeforeAll
	static void buildSensorFixture() {
		// the boot (tryDetectVersion/bootStrap/unfreeze) rode the superclass @BeforeAll;
		// only the fixture remains — the self-referencing holder lets the factory resolve
		// the type after the assignment (the GTCoverTestBase.buildCoverOvenFixture shape)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6ProgressmeterBlockEntity>[] tHolder = (BlockEntityType<GT6ProgressmeterBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ProgressmeterBlockEntity(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sSensorType = tHolder[0];
	}

	/** A fresh offline probe with the fixture type (no level — the pure NBT face). */
	static GT6ProgressmeterBlockEntity probe() {
		return new GT6ProgressmeterBlockEntity(sSensorType, SENSOR_POS, Blocks.BRICKS.defaultBlockState());
	}

	/** A probe loaded from {@code aTag}. */
	static GT6ProgressmeterBlockEntity reloaded(CompoundTag aTag) {
		GT6ProgressmeterBlockEntity tBack = probe();
		tBack.load(aTag);
		return tBack;
	}

	@Test
	public void everyKeyRoundTripsUnderTheUpstreamVerbatimName() {
		GT6ProgressmeterBlockEntity tBe = probe();
		tBe.mMode = GTSensorLogic.MODE_HEX_FLAG | GTSensorLogic.MODE_GREATER; // the packed byte
		tBe.mDisplayedNumber = 4567;
		tBe.mSetNumber = 1234;
		tBe.mRedstone = 9;
		tBe.mSecondFacing = 5;
		tBe.mCurrentValue = 28;
		tBe.mCurrentMax = 100;
		tBe.mIndex = 2;
		tBe.mValues = new int[] {3, 1, 4, 1, 5};

		CompoundTag tSaved = tBe.saveWithoutMetadata();

		// the verbatim names, typed as written (SensorTE :65-87)
		assertEquals((byte) (GTSensorLogic.MODE_HEX_FLAG | GTSensorLogic.MODE_GREATER), tSaved.getByte(GTSensorBlockEntity.NBT_MODE), "gt.mode rides the packed byte");
		assertEquals((short) 4567, tSaved.getShort(GTSensorBlockEntity.NBT_VISUAL), "gt.visual rides the displayed short");
		assertEquals((short) 1234, tSaved.getShort(GTSensorBlockEntity.NBT_VALUE), "gt.value rides the setpoint short");
		assertEquals((byte) 5, tSaved.getByte(GTSensorBlockEntity.NBT_CONNECTION), "gt.connection rides the probe face");
		assertEquals((byte) 9, tSaved.getByte(GTSensorBlockEntity.NBT_REDSTONE), "gt.redstone rides the output");
		assertEquals(100, tSaved.getInt(GTSensorBlockEntity.NBT_SENSOR_MAX), "gt.sensor.max (:83)");
		assertEquals(28, tSaved.getInt(GTSensorBlockEntity.NBT_SENSOR_VALUE), "gt.sensor.value (:84)");
		assertEquals(2, tSaved.getInt(GTSensorBlockEntity.NBT_SENSOR_INDEX), "gt.sensor.index (:85)");
		assertEquals(5, tSaved.getIntArray(GTSensorBlockEntity.NBT_SENSOR_ARRAY).length, "gt.sensor.array (:86)");

		GT6ProgressmeterBlockEntity tBack = reloaded(tSaved);
		// mMode is an int fed by getByte — the sign-extended -126 IS the restored form, and
		// it is what the upstream mMode < 0 hex test (SensorTE:169) is defined against
		assertEquals((int) (byte) (GTSensorLogic.MODE_HEX_FLAG | GTSensorLogic.MODE_GREATER), tBack.mMode, "the packed byte (hex flag included) restores sign-extended");
		assertEquals(2, GTSensorLogic.modeOf(tBack.mMode), "the mode index reads clean through the mask");
		assertTrue(GTSensorLogic.isHexMode(tBack.mMode), "the hex flag survives the byte fold");
		assertEquals(4567, tBack.mDisplayedNumber);
		assertEquals(1234, tBack.mSetNumber);
		assertEquals(9, tBack.mRedstone);
		assertEquals(5, tBack.mSecondFacing, "the probe face is BE-only but persisted (upstream :64)");
		assertEquals(28, tBack.mCurrentValue);
		assertEquals(100, tBack.mCurrentMax);
		assertEquals(2, tBack.mIndex);
		assertEquals(5, tBack.mValues.length);
		assertEquals(1, tBack.mValues[3], "the window content rides the int array verbatim");
	}

	@Test
	public void unsignedShortFoldSurvivesTheRoundTrip() {
		GT6ProgressmeterBlockEntity tBe = probe();
		tBe.mDisplayedNumber = 65535;
		tBe.mSetNumber = 65535;

		CompoundTag tSaved = tBe.saveWithoutMetadata();
		assertEquals(-1, tSaved.getShort(GTSensorBlockEntity.NBT_VISUAL), "65535 rides the signed short -1 (upstream putShort :71)");

		GT6ProgressmeterBlockEntity tBack = reloaded(tSaved);
		assertEquals(65535, tBack.mDisplayedNumber, "the load face folds back through unsignS (:62)");
		assertEquals(65535, tBack.mSetNumber, "the setpoint folds the same way (:63)");
	}

	@Test
	public void absentValueKeyAdoptsTheLoadedDisplay() {
		GT6ProgressmeterBlockEntity tBe = probe();
		tBe.mDisplayedNumber = 777;
		tBe.mSetNumber = 1234;
		CompoundTag tSaved = tBe.saveWithoutMetadata();
		tSaved.remove(GTSensorBlockEntity.NBT_VALUE); // the pre-setpoint save (an older world)

		GT6ProgressmeterBlockEntity tBack = reloaded(tSaved);
		assertEquals(777, tBack.mDisplayedNumber);
		assertEquals(777, tBack.mSetNumber, "the :63 else — the setpoint adopts the loaded display");
	}

	@Test
	public void degenerateArrayGuardRebuildsTheWindow() {
		GT6ProgressmeterBlockEntity tBe = probe();
		tBe.mValues = new int[] {3, 1, 4};
		CompoundTag tSaved = tBe.saveWithoutMetadata();
		tSaved.remove(GTSensorBlockEntity.NBT_SENSOR_ARRAY);

		GT6ProgressmeterBlockEntity tBack = reloaded(tSaved);
		assertEquals(1, tBack.mValues.length, "the :72 guard — a missing array rebuilds the one-slot window");
		assertEquals(0, tBack.mValues[0]);

		// and an EMPTY array in the tag takes the same guard (the getIntArray empty default)
		CompoundTag tEmpty = probe().saveWithoutMetadata();
		tEmpty.putIntArray(GTSensorBlockEntity.NBT_SENSOR_ARRAY, new int[0]);
		assertEquals(1, reloaded(tEmpty).mValues.length, "the length<1 branch catches the empty array too");
	}

	@Test
	public void averagingResizeDiscardsTheWindow() {
		// the upstream :217-227 resize is mValues = new int[len] — the whole window is
		// discarded, NOT a content-preserving copy (the takeover fix: the arraycopy the
		// interim revision carried contradicted both the upstream and its own javadoc)
		GT6ProgressmeterBlockEntity tBe = probe();
		tBe.mValues = new int[] {10, 20, 30};
		tBe.mIndex = 2;
		tBe.screwdriverResizeAveraging(2, 1); // the +1 button → 4 slots
		assertEquals(4, tBe.mValues.length);
		for (int i = 0; i < tBe.mValues.length; i++) {
			assertEquals(0, tBe.mValues[i], "slot " + i + " is a fresh zero — the mean drags until the window refills");
		}
		// the shrink arm discards the same way (the :225 -1 button)
		tBe.mValues = new int[] {7, 8, 9};
		tBe.screwdriverResizeAveraging(2, 0);
		assertEquals(2, tBe.mValues.length);
		assertEquals(0, tBe.mValues[0]);
		assertEquals(0, tBe.mValues[1]);
	}

	@Test
	public void loadToleratesABareTag() {
		// a tag with none of the sensor keys (a foreign payload) — every read branches
		// through contains/defaults and the BE survives with the placement defaults
		GT6ProgressmeterBlockEntity tBack = reloaded(new CompoundTag());
		assertEquals(0, tBack.mMode);
		assertEquals(0, tBack.mDisplayedNumber);
		assertEquals(0, tBack.mSetNumber);
		assertEquals(3, tBack.mSecondFacing, "the placement default probe face (OPOS of facing 2)");
		assertEquals(0, tBack.mRedstone);
		assertEquals(1, tBack.mValues.length);
	}
}
