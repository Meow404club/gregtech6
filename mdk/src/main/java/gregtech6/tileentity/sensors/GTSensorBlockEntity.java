package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.util.UT6;

/**
 * The sensor double base (task p26-sensors-core) — the 1.20.1 counterpart of the upstream
 * chain {@code MultiTileEntitySensorTE} (gregapi/tileentity/machines/MultiTileEntitySensorTE
 * .java:46) extends {@code MultiTileEntitySensor} (:53) extends
 * {@code TileEntityBase10FacingDouble}. The generic FacingSingle/FacingDouble BE base
 * classes are not ported (the p26-arch-wave4 ruling), so this BE extends the folded
 * {@link TileEntityBase03TicksAndSync} stratum and carries BOTH facings itself:
 * {@link #mFacing} = the display/keypad face (mirrored into the block's FACING property —
 * the GTOvenBlock A-tier shape) and {@link #mSecondFacing} = the probe face, the side the
 * sample is read from (BE-only, upstream NBT_CONNECTION "gt.connection").
 *
 * <p>Port scope:
 * <ul>
 * <li>the 8 modes / sliding average / keypad / screwdriver / soft-hammer semantics ride
 *     {@link GTSensorLogic} (the MC-free numeric core, per-function upstream anchors
 *     there);</li>
 * <li>the sample tick — upstream {@code onServerTickPost} (:124-160) — lands in the
 *     {@link #onTick} phase gated by {@link #getTickRate()} ({@code SERVER_TIME % rate}
 *     :125 becomes {@code mTimer % rate}: both counters advance once per tick, so the
 *     periodic-gate semantics are identical up to the placement phase, which a periodic
 *     sampler cannot observe);</li>
 * <li>the display resync — upstream {@code onTickCheck} {@code |diff| > 49} (Sensor
 *     :108-111) — overrides the same dispatcher hook here, and the sync CHANNEL is the
 *     declared visual-state ruling: the upstream {@code IMTE_SyncDataShort} short packet
 *     (Sensor:120-123 {@code getClientDataPacketShort(F, mDisplayedNumber)}) maps onto the
 *     vanilla two-channel BE sync the 03 base already carries ({@code getUpdatePacket()} =
 *     ClientboundBlockEntityDataPacket over {@code getUpdateTag() = saveWithoutMetadata()},
 *     the ChunkHolder.java:247 broadcast). A BlockState numeric property was the
 *     alternative and is REJECTED: the displayed number is a full 0..65535 unsigned short,
 *     an IntegerProperty carrier would explode the state space by four orders of magnitude
 *     and the number is not part of the block model (no digit renderer exists — the
 *     upstream 7-pass digit icon stack, Sensor:143-228, is the render pool). The number
 *     rides the NBT keys verbatim and the model stays static.</li>
 * </ul>
 *
 * <p>Cuts (declared): the {@code GT_API_Proxy.SERVER_TICK_PO2T} timer self-registration
 * (:74-77/:110-121 — the port's EntityBlock ticker drives the dispatcher already); the
 * computerizable face (SensorTE:293-309 — no computer system in the port); the item-NBT
 * write ({@code writeItemNBT2} :90-95 — the picked-block NBT face is the data-component
 * card's surface); the tooltips (Sensor:88-97 — the lang card's face); the 6-pass digit
 * render stack and the thin-plate box shape (Sensor:143-261 — the render pool; the port
 * block is a full oriented cube, the boiler-tank assets precedent).
 */
public abstract class GTSensorBlockEntity extends TileEntityBase03TicksAndSync {

	// upstream CS NBT key constants (CS.java:1172/:1194/:1216/:1218/:1232) — verbatim names
	/** Upstream NBT_MODE. */
	public static final String NBT_MODE = "gt.mode";
	/** Upstream NBT_VISUAL. */
	public static final String NBT_VISUAL = "gt.visual";
	/** Upstream NBT_VALUE. */
	public static final String NBT_VALUE = "gt.value";
	/** Upstream NBT_CONNECTION. */
	public static final String NBT_CONNECTION = "gt.connection";
	/** Upstream NBT_REDSTONE. */
	public static final String NBT_REDSTONE = "gt.redstone";

	// upstream MultiTileEntitySensorTE NBT keys (:66-87) — verbatim names
	/** Upstream "gt.sensor.max". */
	public static final String NBT_SENSOR_MAX = "gt.sensor.max";
	/** Upstream "gt.sensor.value". */
	public static final String NBT_SENSOR_VALUE = "gt.sensor.value";
	/** Upstream "gt.sensor.index". */
	public static final String NBT_SENSOR_INDEX = "gt.sensor.index";
	/** Upstream "gt.sensor.array". */
	public static final String NBT_SENSOR_ARRAY = "gt.sensor.array";

	/**
	 * The packed mode byte (upstream {@code mMode}, Sensor:54): the low 7 bits are the
	 * mode index ({@link GTSensorLogic#modeOf}), bit 7 the hexadecimal-display flag
	 * ({@link GTSensorLogic#isHexMode} — the upstream {@code mMode < 0} sign test).
	 */
	protected int mMode = 0;

	/** Upstream Sensor:55 — the displayed / previously-synced / keypad-setpoint numbers. */
	protected int mDisplayedNumber = 0, oDisplayedNumber = 0, mSetNumber = 0;

	/** Upstream Sensor:56 — the emitted redstone strength (0..15, bind4-clamped). */
	protected byte mRedstone = 0;

	/** Upstream SensorTE:62-63 — the sliding-average ring and the sampled pair. */
	protected int[] mValues = new int[] {0};
	protected int mIndex = 0, mCurrentValue = 0, mCurrentMax = 0;

	/** The display face (upstream TileEntityBase10FacingDouble.mFacing, the GT6 side order). */
	protected byte mFacing = 2;

	/** The probe face (upstream mSecondFacing — the neighbour the sample is read from). */
	protected byte mSecondFacing = 3;

	protected GTSensorBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	/** Upstream SensorTE:282-284 — the sample period in ticks. */
	public long getTickRate() {
		return 1;
	}

	// -------------------------------------------------------------------------
	// persistence (upstream Sensor readFromNBT2/writeToNBT2 :58-75 + SensorTE :65-87)
	// -------------------------------------------------------------------------

	//? if forge {
	@Override
	//?}
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE); // upstream :61
		if (aNBT.contains(NBT_VISUAL, Tag.TAG_ANY_NUMERIC)) mDisplayedNumber = GTSensorLogic.unsignS(aNBT.getShort(NBT_VISUAL)); // upstream :62
		if (aNBT.contains(NBT_VALUE, Tag.TAG_ANY_NUMERIC)) mSetNumber = GTSensorLogic.unsignS(aNBT.getShort(NBT_VALUE)); else mSetNumber = mDisplayedNumber; // upstream :63
		if (aNBT.contains(NBT_CONNECTION, Tag.TAG_ANY_NUMERIC)) mSecondFacing = aNBT.getByte(NBT_CONNECTION); // upstream :64
		if (aNBT.contains(NBT_REDSTONE, Tag.TAG_ANY_NUMERIC)) mRedstone = aNBT.getByte(NBT_REDSTONE); // upstream :65
		mCurrentMax = aNBT.getInt(NBT_SENSOR_MAX); // upstream :68
		mCurrentValue = aNBT.getInt(NBT_SENSOR_VALUE); // upstream :69
		mIndex = aNBT.getInt(NBT_SENSOR_INDEX); // upstream :70
		mValues = aNBT.getIntArray(NBT_SENSOR_ARRAY); // upstream :71
		if (mValues.length < 1) mValues = new int[] {0}; // upstream :72
	}

	//? if forge {
	@Override
	//?}
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putShort(NBT_VISUAL, (short) mDisplayedNumber); // upstream :71
		aNBT.putShort(NBT_VALUE, (short) mSetNumber); // upstream :72
		aNBT.putByte(NBT_MODE, (byte) mMode); // upstream :73
		aNBT.putByte(NBT_REDSTONE, mRedstone); // upstream :74
		aNBT.putInt(NBT_SENSOR_MAX, mCurrentMax); // upstream :83
		aNBT.putInt(NBT_SENSOR_VALUE, mCurrentValue); // upstream :84
		aNBT.putInt(NBT_SENSOR_INDEX, mIndex); // upstream :85
		aNBT.putIntArray(NBT_SENSOR_ARRAY, mValues); // upstream :86
		aNBT.putByte(NBT_CONNECTION, mSecondFacing); // upstream the NBT_CONNECTION read counterpart
	}

	// -------------------------------------------------------------------------
	// the tick (upstream SensorTE.onServerTickPost :124-160 on the dispatcher's onTick phase)
	// -------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (aIsServerSide && (getTickRate() < 2 || aTimer % getTickRate() == 0)) {
			sampleTick();
		}
	}

	/**
	 * Upstream :125-160 verbatim in the port's seams: rotate the ring, seed the display
	 * with the setpoint (:127), resolve the probe target (the multiblock-part
	 * {@code mTarget} forward :130-134 rides {@link MultiBlockPartBlockEntity#getTarget}),
	 * store the bound sample and average (:136-137), bind the max (:138), run the mode
	 * switch, bind4 the redstone and {@code causeBlockUpdate()} on change (:152-157), and
	 * fold the display back through the unsigned-short gate (:159).
	 */
	protected void sampleTick() {
		mIndex = (mIndex + 1) % mValues.length; // upstream :126
		mDisplayedNumber = mSetNumber = GTSensorLogic.bind16(mSetNumber); // upstream :127

		BlockEntity tTarget = resolveTarget(); // upstream :129-134
		mValues[mIndex] = GTSensorLogic.bindInt(getCurrentValue(tTarget)); // upstream :136
		mCurrentValue = (int) GTSensorLogic.average(mValues); // upstream :137
		mCurrentMax = GTSensorLogic.bindInt(getCurrentMax(tTarget)); // upstream :138

		GTSensorLogic.Sample tSample = GTSensorLogic.sample(mMode, mCurrentValue, mCurrentMax, mSetNumber); // upstream :141-150
		if (tSample.displayedSet()) mDisplayedNumber = tSample.displayed();

		byte tRedstone = tSample.redstoneSet() ? UT6.bind4(tSample.redstone()) : mRedstone; // upstream :152
		if (tRedstone != mRedstone) { // upstream :154-157
			mRedstone = tRedstone;
			causeBlockUpdate();
		}

		mDisplayedNumber = GTSensorLogic.unsignS((short) mDisplayedNumber); // upstream :159
	}

	/**
	 * Upstream :129-134 — the probe target: the neighbour at {@link #mSecondFacing}, with
	 * the multiblock-part forward (a part block resolves to its formed controller).
	 */
	@Nullable
	protected BlockEntity resolveTarget() {
		if (!hasLevel()) return null;
		BlockPos tPos = getBlockPos().relative(Direction.from3DDataValue(mSecondFacing));
		BlockEntity tTile = getLevel().getBlockEntity(tPos);
		if (tTile instanceof MultiBlockPartBlockEntity tPart) {
			// upstream :131-132 read the raw mTarget field — the no-validity-recheck form
			BlockEntity tController = (BlockEntity) tPart.getTarget(false);
			if (tController != null) return tController;
		}
		return tTile;
	}

	/**
	 * The per-sensor sample read (upstream the abstract pair :288-289, each implementation
	 * ≈40 lines — the Progressmeter.java:43-59 shape). The target may be null (unloaded /
	 * air neighbour — both reads answer 0 then, the upstream Delegator null-tolerance).
	 */
	public abstract long getCurrentValue(@Nullable BlockEntity aTarget);

	/** The per-sensor scale maximum (upstream {@code getCurrentMax}). */
	public abstract long getCurrentMax(@Nullable BlockEntity aTarget);

	// -------------------------------------------------------------------------
	// the display resync gate (upstream Sensor.onTickCheck/onTickChecked :107-117)
	// -------------------------------------------------------------------------

	@Override
	public boolean onTickCheck(long aTimer) {
		mDisplayedNumber = GTSensorLogic.bind16(mDisplayedNumber); // upstream :109
		return super.onTickCheck(aTimer) || GTSensorLogic.shouldSyncDisplayed(mDisplayedNumber, oDisplayedNumber); // upstream :110
	}

	@Override
	public void onTickChecked(long aTimer) {
		super.onTickChecked(aTimer);
		oDisplayedNumber = mDisplayedNumber; // upstream :116
	}

	// -------------------------------------------------------------------------
	// the redstone emission (upstream Sensor.isProvidingWeakPower2 :247)
	// -------------------------------------------------------------------------

	/**
	 * Upstream :247 verbatim — weak power on every side EXCEPT the one opposite the probe
	 * face (the sensor never feeds back into the block it samples). The block's
	 * {@code getSignal} passes the vanilla query direction straight through as the GT6
	 * side (the GTOvenBlock.bridgeSignal convention).
	 */
	public byte redstoneOut(byte aSide) {
		return aSide == UT6.OPOS[mSecondFacing] ? 0 : mRedstone;
	}

	// -------------------------------------------------------------------------
	// the tool arms (upstream Sensor.onToolClick2 :100-105 + SensorTE.onToolClick2 :204-250,
	// routed from GTSensorBlock.use — the GTOvenBlock shift-hoe dispatch shape)
	// -------------------------------------------------------------------------

	/** The hard-read of the display number (the /gt6sensor read channel and the tests). */
	public int getDisplayedNumber() {
		return mDisplayedNumber;
	}

	/** The current sampled value (upstream mCurrentValue). */
	public int getCurrentValue() {
		return mCurrentValue;
	}

	/** The current sampled maximum (upstream mCurrentMax). */
	public int getCurrentMax() {
		return mCurrentMax;
	}

	/** The keypad setpoint (upstream mSetNumber). */
	public int getSetNumber() {
		return mSetNumber;
	}

	/** The emitted redstone strength (upstream mRedstone). */
	public byte getRedstone() {
		return mRedstone;
	}

	/** The packed mode byte (upstream mMode). */
	public int getMode() {
		return mMode;
	}

	/** The sliding-average window length (upstream mValues.length). */
	public int getAveragingLength() {
		return mValues.length;
	}

	/** The display face (the GT6 side order). */
	public byte getFacing() {
		return mFacing;
	}

	/** The probe face (the GT6 side order). */
	public byte getSecondFacing() {
		return mSecondFacing;
	}

	/**
	 * Upstream Sensor:102 — the wrench arm: the clicked side becomes the display face AND
	 * the probe face folds to its opposite ({@code mSecondFacing = OPOS[mFacing = side]}).
	 * Invalid sides (the wrench-grid SIDE_INVALID pick) are refused. The BlockState mirror
	 * rides the write (the setFrontFacing shape) so the model re-orients.
	 */
	public boolean wrenchSetFacing(byte aTargetSide) {
		if (aTargetSide < 0 || aTargetSide > 5) return false;
		mFacing = aTargetSide;
		mSecondFacing = UT6.OPOS[aTargetSide];
		writeFacingState();
		updateClientData();
		causeBlockUpdate();
		return true;
	}

	/**
	 * Upstream Sensor:103 — the monkey-wrench arm: the probe face alone. The monkey-wrench
	 * ITEM is not ported (the GT6Tools census), so the world arm is the /gt6sensor second
	 * RCON stand-in (the gearbox monkey-wrench ruling); the BE mutation is the upstream one.
	 */
	public boolean monkeyWrenchSetSecondFacing(byte aTargetSide) {
		if (aTargetSide < 0 || aTargetSide > 5 || aTargetSide == mFacing) return false;
		mSecondFacing = aTargetSide;
		updateClientData();
		causeBlockUpdate();
		return true;
	}

	/**
	 * Upstream SensorTE:208-213 — the screwdriver on the display strip toggles the
	 * hexadecimal display (the packed bit-7 flip).
	 */
	public boolean screwdriverToggleHex() {
		mMode = GTSensorLogic.toggleHex(mMode);
		updateClientData();
		return true;
	}

	/**
	 * Upstream SensorTE:230-240 — the screwdriver elsewhere cycles the mode (the hex flag
	 * preserved) and zeroes a stale redstone output with the block-update arm.
	 */
	public boolean screwdriverCycleMode() {
		mMode = GTSensorLogic.cycleMode(mMode);
		if (mRedstone != 0) {
			mRedstone = 0;
			causeBlockUpdate();
		}
		updateClientData();
		return true;
	}

	/**
	 * Upstream SensorTE:217-227 — the screwdriver on the keypad buttons resizes the
	 * sliding-average window (the keypad step table, clamped 1..MAX_AVERAGING_VALUES).
	 * Enlarging clears stale tail slots implicitly (the fresh zeros drag the mean — the
	 * upstream {@code new int[len]} semantics, NOT a content-preserving copy).
	 */
	public boolean screwdriverResizeAveraging(int aRow, int aCol) {
		int tLength = GTSensorLogic.averagingStep(GTSensorLogic.isHexMode(mMode), aRow, aCol, mValues.length);
		int[] tValues = new int[tLength];
		System.arraycopy(mValues, 0, tValues, 0, Math.min(mValues.length, tLength));
		mValues = tValues;
		mIndex %= tLength;
		return true;
	}

	/**
	 * Upstream SensorTE:164-200 — the bare-hand keypad click on the front face: one 3x3
	 * button step on the setpoint (threshold modes only). The {@code oDisplayedNumber =
	 * Short.MIN_VALUE} write (:171-193) forces the next diff-gate sync through any value.
	 */
	public boolean keypadClick(int aRow, int aCol) {
		if (!GTSensorLogic.isThresholdMode(mMode)) return false;
		mSetNumber = GTSensorLogic.keypadStep(GTSensorLogic.isHexMode(mMode), aRow, aCol, mSetNumber);
		oDisplayedNumber = Short.MIN_VALUE;
		return true;
	}

	/**
	 * Upstream SensorTE:242-248 — the soft-hammer reset: everything zeroed, the window
	 * back to one slot, block-update + sync armed. The soft-hammer ITEM is not ported —
	 * the world arm is the /gt6sensor reset RCON stand-in (the card's declared arm).
	 */
	public void softHammerReset() {
		mCurrentValue = mCurrentMax = mIndex = mDisplayedNumber = mSetNumber = mMode = 0;
		mRedstone = 0;
		mValues = new int[] {0};
		oDisplayedNumber = 0;
		causeBlockUpdate();
		updateClientData();
	}

	/** The FACING-property mirror write (the TileEntityOven.setFrontFacing shape). */
	protected void writeFacingState() {
		if (hasLevel()) {
			BlockState tState = getLevel().getBlockState(getBlockPos());
			if (tState.getBlock() instanceof GTSensorBlock && tState.hasProperty(GTSensorBlock.FACING)) {
				getLevel().setBlock(getBlockPos(), tState.setValue(GTSensorBlock.FACING, Direction.from3DDataValue(mFacing)), 3);
			}
		}
	}
}
