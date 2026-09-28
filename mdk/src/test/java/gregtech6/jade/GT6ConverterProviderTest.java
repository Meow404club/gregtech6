package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity;
import gregtech6.tileentity.energy.GT6ElectricDynamoBlockEntity;
import gregtech6.tileentity.energy.GT6ElectricTransformerBlockEntity;
import gregtech6.tileentity.energy.GT6WaterWheelBlockEntity;
import gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.generators.GTGeneratorLiquidBlockEntity;

/**
 * Offline gate for task r5-jade-converters: the converter-family Jade tag contract + the
 * status/stored/rate display lines, over the SAME {@code writeFamilyData} seam the live
 * {@code appendServerData} reads through (the BlockAccessor wrapper itself is live-only —
 * the GT6CrucibleProviderTest posture). Five representative BEs across the three SPEC
 * families (引擎 steam/diesel, 变压器 electric, dynamo electric, 发电机 burning box,
 * 电池箱 battery box) + the ZPM decharger's QU lane + the water-wheel skip boundary.
 *
 * <p>Four groups: the engine trio (status two-state + stored/capacity/rate values), the
 * transformer/dynamo storage faces, the burning-box bare-stored form + the no-status FE
 * lane absence, and the line functions (key + %s args + the GREEN/RED state colours).
 * The lang VALUES (en/zh) are pinned by the datagen faces + runData, not here.
 */
public class GT6ConverterProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GTSteamEngineBlockEntity> sEngineType;
	static BlockEntityType<GTDieselEngineBlockEntity> sDieselType;
	static BlockEntityType<GT6ElectricTransformerBlockEntity> sTransformerType;
	static BlockEntityType<GT6ElectricDynamoBlockEntity> sDynamoType;
	static BlockEntityType<GTGeneratorLiquidBlockEntity> sBurningType;
	static BlockEntityType<GT6BatteryBoxBlockEntity> sBoxType;
	static BlockEntityType<GT6ZpmDechargerBlockEntity> sZpmType;
	static BlockEntityType<GT6WaterWheelBlockEntity> sWheelType;

	@BeforeAll
	static void fixtures() {
		sEngineType = offlineType(GTSteamEngineBlockEntity::new, Blocks.STONE, Blocks.OAK_STAIRS);
		sDieselType = offlineType(GTDieselEngineBlockEntity::new, Blocks.STONE);
		sTransformerType = offlineType(GT6ElectricTransformerBlockEntity::new, Blocks.STONE);
		sDynamoType = offlineType(GT6ElectricDynamoBlockEntity::new, Blocks.STONE);
		sBurningType = offlineType(GTGeneratorLiquidBlockEntity::new, Blocks.STONE);
		sBoxType = offlineType(GT6BatteryBoxBlockEntity::new, Blocks.STONE);
		sZpmType = offlineType((aType, aPos, aState) -> new GT6ZpmDechargerBlockEntity(aType, aPos, aState,
				gregapi.data.TD.Energy.EU), Blocks.STONE);
		sWheelType = offlineType(GT6WaterWheelBlockEntity::new, Blocks.STONE);
	}

	/** The offline BET factory seam (the self-referencing holder, GT6CrucibleProviderTest.fixture shape). */
	@FunctionalInterface
	interface BEFactory<T extends net.minecraft.world.level.block.entity.BlockEntity> {
		T create(BlockEntityType<T> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState);
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> offlineType(
			BEFactory<T> aFactory, net.minecraft.world.level.block.Block... aValid) {
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> aFactory.create(tHolder[0], aPos, aState), aValid).build(null);
		return tHolder[0];
	}

	// ------------------------------------------------------------------------------------
	// group ① the engines — status two-state + the full stored/capacity/rate trio
	// ------------------------------------------------------------------------------------

	@Test
	public void steamEngineRidesTheFullTrio() {
		GTSteamEngineBlockEntity tEngine = new GTSteamEngineBlockEntity(sEngineType, POS, Blocks.STONE.defaultBlockState());
		tEngine.mEnergy = 128000;
		tEngine.mActive = true;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tEngine);
		// the field trio verbatim (GTSteamEngineBlockEntity.java:129/:131/:133) + the running latch
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_HAS_STATUS));
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_ACTIVE));
		assertFalse(tTag.getBoolean(GT6ConverterProvider.KEY_STOPPED));
		assertEquals(128000L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		assertEquals(640000L, tTag.getLong(GT6ConverterProvider.KEY_CAPACITY), "the :131 field default on a stone fixture");
		assertEquals(64L, tTag.getLong(GT6ConverterProvider.KEY_RATE));
		assertEquals("KU", tTag.getString(GT6ConverterProvider.KEY_UNIT));
		assertEquals("KU", tTag.getString(GT6ConverterProvider.KEY_RATE_UNIT));
		// the wire is typed long/boolean/string like the crucible contract
		assertEquals(Tag.TAG_LONG, tTag.getTagType(GT6ConverterProvider.KEY_STORED));
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6ConverterProvider.KEY_ACTIVE));
		assertEquals(Tag.TAG_STRING, tTag.getTagType(GT6ConverterProvider.KEY_UNIT));
	}

	@Test
	public void softStopOutranksRunningAndDieselCapsAtTheBurnGate() {
		GTDieselEngineBlockEntity tDiesel = new GTDieselEngineBlockEntity(sDieselType, POS, Blocks.STONE.defaultBlockState());
		tDiesel.mEnergy = 40;
		tDiesel.mRate = 32;
		tDiesel.mActive = true;
		tDiesel.mStopped = true;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tDiesel);
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_STOPPED));
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_ACTIVE));
		assertEquals(40L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		// the ceiling = mRate*2, the :113/:235 burn gate's working band (a declared soft cap)
		assertEquals(64L, tTag.getLong(GT6ConverterProvider.KEY_CAPACITY));
		assertEquals(32L, tTag.getLong(GT6ConverterProvider.KEY_RATE));
		assertEquals("RU", tTag.getString(GT6ConverterProvider.KEY_UNIT));
	}

	// ------------------------------------------------------------------------------------
	// group ② the transformer / dynamo storage faces
	// ------------------------------------------------------------------------------------

	@Test
	public void electricTransformerPinsStorageAndCapMirror() {
		GT6ElectricTransformerBlockEntity tTrans = new GT6ElectricTransformerBlockEntity(
				sTransformerType, POS, Blocks.STONE.defaultBlockState());
		tTrans.mStorage = 500;
		tTrans.mActive = true;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tTrans);
		assertEquals(500L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		assertEquals(tTrans.capacity(), tTag.getLong(GT6ConverterProvider.KEY_CAPACITY), "the :292 capacity() read face");
		assertEquals("EU", tTag.getString(GT6ConverterProvider.KEY_UNIT));
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_ACTIVE));
		// no single throughput field on this family — the rate face stays dark
		assertEquals(0L, tTag.getLong(GT6ConverterProvider.KEY_RATE));
	}

	@Test
	public void electricDynamoStoresRuAndEmitsEu() {
		GT6ElectricDynamoBlockEntity tDynamo = new GT6ElectricDynamoBlockEntity(sDynamoType, POS, Blocks.STONE.defaultBlockState());
		tDynamo.mStorage = 32;
		tDynamo.mOutput = 22;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tDynamo);
		assertEquals(32L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		assertEquals(tDynamo.capacity(), tTag.getLong(GT6ConverterProvider.KEY_CAPACITY), "the base :227 mInput*2 face");
		assertEquals(22L, tTag.getLong(GT6ConverterProvider.KEY_RATE));
		assertEquals("RU", tTag.getString(GT6ConverterProvider.KEY_UNIT), "the capacitor stores conversion units");
		assertEquals("EU", tTag.getString(GT6ConverterProvider.KEY_RATE_UNIT), "the electric row's :84 emitted type");
	}

	// ------------------------------------------------------------------------------------
	// group ③ the burning box bare-stored form + the no-status/no-row boundaries
	// ------------------------------------------------------------------------------------

	@Test
	public void burningBoxShowsBareStoredAndRunning() {
		GTGeneratorLiquidBlockEntity tBox = new GTGeneratorLiquidBlockEntity(sBurningType, POS, Blocks.STONE.defaultBlockState());
		tBox.mBurning = true;
		tBox.mEnergy = 250;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tBox);
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_HAS_STATUS));
		assertTrue(tTag.getBoolean(GT6ConverterProvider.KEY_ACTIVE), "mBurning is the family's status bit (:128)");
		assertFalse(tTag.getBoolean(GT6ConverterProvider.KEY_STOPPED));
		assertEquals(250L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		assertEquals(0L, tTag.getLong(GT6ConverterProvider.KEY_CAPACITY), "no hard ceiling — the bare-stored form");
		assertEquals("HU", tTag.getString(GT6ConverterProvider.KEY_UNIT));
		assertEquals("HU", tTag.getString(GT6ConverterProvider.KEY_RATE_UNIT));
	}

	@Test
	public void batteryBoxAndZpmSplitTheEuAndQuLanes() {
		GT6BatteryBoxBlockEntity tBox = new GT6BatteryBoxBlockEntity(sBoxType, POS, Blocks.STONE.defaultBlockState(), 0, 4);
		tBox.mEnergy = 1000;
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tBox);
		assertEquals(1000L, tTag.getLong(GT6ConverterProvider.KEY_STORED));
		assertEquals(tBox.capacity(), tTag.getLong(GT6ConverterProvider.KEY_CAPACITY), "the :338 mInput*320*slots face");
		assertEquals("EU", tTag.getString(GT6ConverterProvider.KEY_UNIT));

		GT6ZpmDechargerBlockEntity tZpm = new GT6ZpmDechargerBlockEntity(sZpmType, POS,
				Blocks.STONE.defaultBlockState(), gregapi.data.TD.Energy.EU);
		CompoundTag tZpmTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tZpmTag, tZpm);
		assertEquals("QU", tZpmTag.getString(GT6ConverterProvider.KEY_UNIT), "the decharger's QU capacitor lane");
		assertEquals(gregtech6.jade.GT6ConverterProvider.shortType(tZpm.mEnergyTypeOut),
				tZpmTag.getString(GT6ConverterProvider.KEY_RATE_UNIT));
	}

	@Test
	public void skippedFamiliesWriteNoKeys() {
		// the water wheel: no readable state fields — the census skip must stay a wire zero
		GT6WaterWheelBlockEntity tWheel = new GT6WaterWheelBlockEntity(sWheelType, POS, Blocks.STONE.defaultBlockState());
		CompoundTag tTag = new CompoundTag();
		GT6ConverterProvider.writeFamilyData(tTag, tWheel);
		assertFalse(tTag.contains(GT6ConverterProvider.KEY_UNIT), "the client gate stays shut for skipped BEs");
	}

	// ------------------------------------------------------------------------------------
	// group ④ the display-line pure functions
	// ------------------------------------------------------------------------------------

	@Test
	public void statusLineKeysTheStateWordsAndPaintsTheLadder() {
		// Running rides GREEN, Stopped RED, Idle stays unstyled (the crucible meltdown colour posture)
		net.minecraft.network.chat.Component tRunning = GT6ConverterProvider.statusLine(true, false);
		assertEquals(GT6ConverterProvider.LANG_STATUS, ((net.minecraft.network.chat.contents.TranslatableContents) tRunning.getContents()).getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tRunning.getStyle().getColor());
		net.minecraft.network.chat.Component tStopped = GT6ConverterProvider.statusLine(false, true);
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tStopped.getStyle().getColor());
		net.minecraft.network.chat.Component tIdle = GT6ConverterProvider.statusLine(false, false);
		assertNotEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tIdle.getStyle().getColor());
		assertNotEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tIdle.getStyle().getColor());
	}

	@Test
	public void storedAndRateLinesCarryTheKeyedArgs() {
		net.minecraft.network.chat.contents.TranslatableContents tStored =
				(net.minecraft.network.chat.contents.TranslatableContents) GT6ConverterProvider.storedLine(100, 200, "KU").getContents();
		assertEquals(GT6ConverterProvider.LANG_STORED, tStored.getKey());
		assertEquals(100L, tStored.getArgs()[0]);
		assertEquals(200L, tStored.getArgs()[1]);
		assertEquals("KU", tStored.getArgs()[2]);
		net.minecraft.network.chat.contents.TranslatableContents tBare =
				(net.minecraft.network.chat.contents.TranslatableContents) GT6ConverterProvider.storedBareLine(250, "HU").getContents();
		assertEquals(GT6ConverterProvider.LANG_STORED_BARE, tBare.getKey());
		assertEquals("HU", tBare.getArgs()[1]);
		net.minecraft.network.chat.contents.TranslatableContents tRate =
				(net.minecraft.network.chat.contents.TranslatableContents) GT6ConverterProvider.rateLine(64, "EU").getContents();
		assertEquals(GT6ConverterProvider.LANG_RATE, tRate.getKey());
		assertEquals(64L, tRate.getArgs()[0]);
	}

	@Test
	public void shortTypeNamesTheFamilyUnits() {
		assertEquals("EU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.EU));
		assertEquals("RU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.RU));
		assertEquals("KU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.KU));
		assertEquals("HU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.HU));
		assertEquals("RF", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.RF));
		assertEquals("QU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.QU));
		assertEquals("LU", GT6ConverterProvider.shortType(gregapi.data.TD.Energy.LU));
	}
}
