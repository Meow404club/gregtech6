package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.sensors.GTSensorLogic;
import gregtech6.tileentity.sensors.GT6ProgressmeterBlockEntity;

/**
 * Offline gate for the sensor Jade face (task r8-jade-sensor-provider): the tag contract
 * over the SAME static seam the live {@code appendServerData} reads through —
 * {@code writeSensorData} dispatches on the abstract {@code GTSensorBlockEntity} (ONE
 * instanceof covers the 21-sensor family, the public-getter face GTSensorBlockEntity
 * .java:258-300, the protected fields are untouched per the card ruling) — plus the row
 * pure functions. The real-BE arm runs a {@link GT6ProgressmeterBlockEntity} fixture (the
 * pioneer sensor, the GT6BoilerProviderTest fixture posture); the non-sensor zero-key arm
 * runs a plain vanilla {@code BlockEntity} (the GT6ConverterProviderTest
 * skippedFamiliesWriteNoKeys contract). The BlockAccessor wrapper itself is live-only (the
 * GT6MachineProviderTest posture) and so are the {@code showDetails()} gate and the
 * tooltip element assembly — the sneak band (sample + probe, exactly two rows) is pinned
 * through its two pure functions.
 *
 * <p>The pin set: the SIX wire keys (design payload_rules sensor=6), the hex modifier as
 * the {@code 0x%04X}-formatted slot (no second reading key), the mode row nesting one of
 * the EIGHT mode-name keys (with the modeOf mask + the %8 wrap for garbage NBT bytes) and
 * the probe face riding the Direction name (the /gt6sensor read channel wording).
 */
public class GT6SensorProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);

	static BlockEntityType<FixtureSensor> sSensorType;

	/** The concrete test BE — the progressmeter (a real family member) over a vanilla-block BET. */
	public static final class FixtureSensor extends GT6ProgressmeterBlockEntity {
		public FixtureSensor(BlockPos aPos, BlockState aState) {
			super(sSensorType, aPos, aState);
		}

		/** The test arm: the protected field face only the subclass can write (the BE stays read-only to the provider). */
		void arm(int aMode, int aDisplayed, int aValue, int aMax, byte aRedstone, byte aProbe) {
			mMode = aMode;
			mDisplayedNumber = aDisplayed;
			mCurrentValue = aValue;
			mCurrentMax = aMax;
			mRedstone = aRedstone;
			mSecondFacing = aProbe;
		}
	}

	/** The vanilla-island BE — the non-sensor arm of the zero-key contract (BlockEntity is abstract, the minimal subclass). */
	public static final class PlainBlockEntity extends BlockEntity {
		public PlainBlockEntity(BlockPos aPos, BlockState aState) {
			super(sPlainType, aPos, aState);
		}
	}

	static BlockEntityType<PlainBlockEntity> sPlainType;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<FixtureSensor>[] tHolder = (BlockEntityType<FixtureSensor>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FixtureSensor::new, Blocks.STONE).build(null);
		sSensorType = tHolder[0];
		BlockEntityType<PlainBlockEntity>[] tPlainHolder = (BlockEntityType<PlainBlockEntity>[]) new BlockEntityType<?>[1];
		tPlainHolder[0] = BlockEntityType.Builder.of(PlainBlockEntity::new, Blocks.STONE).build(null);
		sPlainType = tPlainHolder[0];
	}

	// ------------------------------------------------------------------------------------
	// group ① the wire contract
	// ------------------------------------------------------------------------------------

	@Test
	public void sensorFieldsRideTheSixKeys() {
		FixtureSensor tSensor = new FixtureSensor(POS, Blocks.STONE.defaultBlockState());
		tSensor.arm(0x80 | GTSensorLogic.MODE_SCALE, 65000, 1234, 4000, (byte) 13, (byte) 3);
		CompoundTag tTag = new CompoundTag();
		GT6SensorProvider.writeSensorData(tTag, tSensor);
		assertEquals(65000, tTag.getInt(GT6SensorProvider.KEY_DISPLAYED));
		assertEquals(0x80 | GTSensorLogic.MODE_SCALE, tTag.getInt(GT6SensorProvider.KEY_MODE), "the packed byte verbatim — the hex bit rides it");
		assertEquals(13, tTag.getByte(GT6SensorProvider.KEY_REDSTONE));
		assertEquals(1234, tTag.getInt(GT6SensorProvider.KEY_VALUE));
		assertEquals(4000, tTag.getInt(GT6SensorProvider.KEY_MAX));
		assertEquals(3, tTag.getByte(GT6SensorProvider.KEY_PROBE));
		// the key faces: four ints + two bytes
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6SensorProvider.KEY_DISPLAYED));
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6SensorProvider.KEY_MODE));
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6SensorProvider.KEY_VALUE));
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6SensorProvider.KEY_MAX));
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6SensorProvider.KEY_REDSTONE));
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6SensorProvider.KEY_PROBE));
	}

	@Test
	public void nonSensorWritesZeroKeys() {
		// the vanilla island: the instanceof stays shut, the tag carries NOTHING — the
		// client gate (KEY_DISPLAYED) never opens for non-sensors
		CompoundTag tTag = new CompoundTag();
		GT6SensorProvider.writeSensorData(tTag, new PlainBlockEntity(POS, Blocks.STONE.defaultBlockState()));
		assertFalse(tTag.contains(GT6SensorProvider.KEY_DISPLAYED), "the client gate stays shut for non-sensors");
		assertEquals(0, tTag.getAllKeys().size(), "the wire stays silent for non-sensors");
	}

	// ------------------------------------------------------------------------------------
	// group ② the reading line (the hex modifier rides the slot, not a second key)
	// ------------------------------------------------------------------------------------

	@Test
	public void readingLineCarriesTheHexModifier() {
		TranslatableContents tDecimal = (TranslatableContents) GT6SensorProvider.readingLine(1234, false).getContents();
		assertEquals(GT6SensorProvider.LANG_READING, tDecimal.getKey());
		assertEquals(1, tDecimal.getArgs().length);
		assertEquals("1234", tDecimal.getArgs()[0]);
		TranslatableContents tHex = (TranslatableContents) GT6SensorProvider.readingLine(65000, true).getContents();
		assertEquals(GT6SensorProvider.LANG_READING, tHex.getKey(), "the same key — hex is a slot format, not a second face");
		assertEquals("0xFDE8", tHex.getArgs()[0], "the 4-digit uppercase hex modifier (the upstream display-strip hex face)");
		assertEquals("0x0000", ((TranslatableContents) GT6SensorProvider.readingLine(0, true).getContents()).getArgs()[0]);
	}

	// ------------------------------------------------------------------------------------
	// group ③ the mode line (the nested eight-name face)
	// ------------------------------------------------------------------------------------

	@Test
	public void modeLineNestsTheEightModeNames() {
		String[] tExpected = {
				  "gt6.jade.sensor.mode.display"
				, "gt6.jade.sensor.mode.percent"
				, "gt6.jade.sensor.mode.greater"
				, "gt6.jade.sensor.mode.equal"
				, "gt6.jade.sensor.mode.smaller"
				, "gt6.jade.sensor.mode.scale"
				, "gt6.jade.sensor.mode.full"
				, "gt6.jade.sensor.mode.not_full"
		};
		for (int i = 0; i < GTSensorLogic.MODE_COUNT; i++) {
			TranslatableContents tMode = (TranslatableContents) GT6SensorProvider.modeLine(i).getContents();
			assertEquals(GT6SensorProvider.LANG_MODE, tMode.getKey());
			assertEquals(1, tMode.getArgs().length);
			TranslatableContents tName = (TranslatableContents) ((Component) tMode.getArgs()[0]).getContents();
			assertEquals(tExpected[i], tName.getKey(), "mode " + i + " nests its own name key");
			assertEquals(0, tName.getArgs().length, "the name faces are slotless constants");
		}
		// the hex flag does not disturb the mode resolution (the packed-byte mask)
		TranslatableContents tPacked = (TranslatableContents) ((Component) ((TranslatableContents) GT6SensorProvider
				.modeLine(0x80 | GTSensorLogic.MODE_EQUAL).getContents()).getArgs()[0]).getContents();
		assertEquals(tExpected[GTSensorLogic.MODE_EQUAL], tPacked.getKey());
	}

	@Test
	public void modeLineWrapsGarbagePackedModes() {
		// NBT garbage beyond the 8-entry ring folds back mod 8 (never a raw-key face)
		TranslatableContents tGarbage = (TranslatableContents) ((Component) ((TranslatableContents) GT6SensorProvider
				.modeLine(127).getContents()).getArgs()[0]).getContents();
		assertEquals("gt6.jade.sensor.mode.not_full", tGarbage.getKey(), "127 & 127 = 127, 127 % 8 = 7 (NOT_FULL)");
		TranslatableContents tGarbageHex = (TranslatableContents) ((Component) ((TranslatableContents) GT6SensorProvider
				.modeLine(0x80 | 9).getContents()).getArgs()[0]).getContents();
		assertEquals("gt6.jade.sensor.mode.percent", tGarbageHex.getKey(), "9 % 8 = 1 (PERCENT), the hex bit ignored");
	}

	// ------------------------------------------------------------------------------------
	// group ④ the redstone line + the sneak pair (exactly two rows)
	// ------------------------------------------------------------------------------------

	@Test
	public void redstoneLineCarriesTheStrength() {
		TranslatableContents tContents = (TranslatableContents) GT6SensorProvider.redstoneLine((byte) 13).getContents();
		assertEquals(GT6SensorProvider.LANG_REDSTONE, tContents.getKey());
		assertEquals(1, tContents.getArgs().length);
		assertEquals(13, tContents.getArgs()[0]);
	}

	@Test
	public void theSneakPairIsSamplePlusProbe() {
		// the sample row: mCurrentValue / mCurrentMax (the design sneak band row 1)
		TranslatableContents tSample = (TranslatableContents) GT6SensorProvider.sampleLine(1234, 4000).getContents();
		assertEquals(GT6SensorProvider.LANG_SAMPLE, tSample.getKey());
		assertEquals(2, tSample.getArgs().length);
		assertEquals(1234, tSample.getArgs()[0]);
		assertEquals(4000, tSample.getArgs()[1]);
		// the probe row: the Direction name (the /gt6sensor read channel wording, side 3 = south)
		TranslatableContents tProbe = (TranslatableContents) GT6SensorProvider.probeLine((byte) 3).getContents();
		assertEquals(GT6SensorProvider.LANG_PROBE, tProbe.getKey());
		assertEquals("south", tProbe.getArgs()[0]);
		assertTrue(GT6SensorProvider.LANG_SAMPLE.startsWith("gt6.jade.sensor."), "both sneak rows stay in the keyed domain");
	}

}
