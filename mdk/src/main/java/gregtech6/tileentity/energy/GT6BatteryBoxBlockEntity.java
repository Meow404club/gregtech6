package gregtech6.tileentity.energy;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.item.energy.GT6BatteryItem;
import gregtech6.item.energy.IItemEnergy;
import gregtech6.registry.GT6Batteries;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The BatteryBox BE (task p29-w4-battery-storage ④) — the
 * {@code TileEntityBase10EnergyBatBox} transcription over the port BE tree (Base10 :50-236,
 * the EU-only shape the Loader rows register: {@code NBT_ENERGY_EMITTED/EU,
 * NBT_INPUT, V[i], NBT_OUTPUT, V[i]}). One class over the twelve blocks; the 4/16 slot
 * families ride two BETs, the slot count is read off the block (the NBT_INV_SIZE column).
 *
 * <h2>The energy mechanics (the transcription map)</h2>
 * <ul>
 * <li><b>The internal buffer</b> {@link #mEnergy} caps at {@code mInput * 320 * slots}
 *     (the :185/:216 progress-max term — 320 packets of headroom per slot);</li>
 * <li><b>The battery exchange</b> (the :108-124 20-tick phase, {@code SERVER_TIME % 20 == 1}
 *     → the port BE timer's {@code aTimer % 20 == 1}): the buffer fill ratio
 *     {@code mEnergy / (mInput * 40 * slots)} rides {@code UT.Code.bind3} (upstream
 *     UT.java:1555 = clamp 0..7) — band 0 (bottom eighth): pull 40 packets per battery
 *     into the buffer; band 1: pull 20; band 6: push 20 into the batteries; band 7:
 *     push 40; bands 2..5 idle. The per-battery packet counts ride the IItemEnergy
 *     doEnergyExtraction/doEnergyInjection faces (the :111-114 branches; the COMPAT_EU_ITEM
 *     fallback branches of :118-:121 have no port counterpart — the port's IItemEnergy
 *     seam is the only battery language);</li>
 * <li><b>The emit</b> (the :141-151): while {@code mEnergy >= mOutput} and not stopped,
 *     emit up to {@code mBatteryCount} packets ({@code mMode} caps it when 1..15, the
 *     selector-cover lane collapsed to the mMode field) of {@code mOutput} EU out of the
 *     FRONT face; the buffer pays {@code mOutput * acceptedPackets};</li>
 * <li><b>The intake</b> (the doInject :178-193 verbatim): the packet band is the Root
 *     defaults over {@code getEnergySizeInputRecommended = mInput} — min V[i]/2 (smaller
 *     packets are swallowed by the Root gate), max V[i]*2; ABOVE max the packet is
 *     consumed-but-not-stored and the overload ladder runs (the Base10 :140-148 hundred
 *     soft strikes, then overcharge — the GT6ElectricTransformerBlockEntity form). The
 *     {@code mReceivablePower} bookkeeping (the :153/:187-:190 chargeable-battery headroom
 *     limiter) rides verbatim.</li>
 * </ul>
 *
 * <h2>The item face</h2>
 * The inventory takes ONLY IItemEnergy items whose type matches the box's EU domain
 * (the canInsertItem2 :199 — {@code isEnergyType(EU, stack, any-direction)}), one stack
 * per slot (the getInventoryStackLimit :218). getEnergyStored/getEnergyCapacity sum the
 * inventory batteries (:210-:211) — the box's own buffer is INTERNAL, not part of the
 * sum (upstream sums only the batteries).
 *
 * <h2>The storage sums and the charge dial</h2>
 * {@link #getEnergyStored}/{@link #getEnergyCapacity} walk the slots through
 * {@link IItemEnergy} (the :210-:211 verbatim); the EU/LU mutual rejection is structural:
 * the box's mEnergyType is EU (the row column), so LU batteries fail the canInsertItem2
 * gate and never enter, and an LU getEnergyStored sum reads 0 per-battery.
 *
 * <p>NBT (the upstream literal keys, CS.java): {@code gt.energy} (the buffer), {@code
 * gt.mode}, {@code gt.active}, {@code gt.stopped}, {@code gt.active.energy} (the
 * {@link #mEmitsEnergy} display lane), {@code gt.input}/{@code gt.output} (the row
 * constants, read-guarded like :65-:66), {@code gt.energy.accepted} (the type, the :68
 * read form) plus the port's {@code facing}/{@code inventory} carriers (the hopper
 * shape). KJS surface: none (behavior + registration face is deferred — the KJS pool).
 */
public class GT6BatteryBoxBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	// upstream CS.java literals
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_ACTIVE = "gt.active";
	public static final String NBT_ACTIVE_ENERGY = "gt.active.energy";
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_MODE = "gt.mode";
	public static final String NBT_INPUT = "gt.input";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_ENERGY_ACCEPTED = "gt.energy.accepted";
	// the port carriers (the hopper shape)
	public static final String NBT_FACING = "facing";
	public static final String NBT_INVENTORY = "inventory";

	/** The stored EU (the :52 field). */
	public long mEnergy = 0;
	/** The packet size IN (the NBT_INPUT column, V[tier]). */
	public long mInput = 32;
	/** The packet size OUT (the NBT_OUTPUT column, V[tier]). */
	public long mOutput = 32;
	/** The selector-cover packet cap (the :52/:143 mode lane, 0 = emit as much as possible). */
	public byte mMode = 0;
	/** The soft-hammer/adjacent stop (:51 — the :142 gate). */
	public boolean mStopped = false;
	/** Emitted on the last emit pass (:51, the mEmitsEnergy display lane). */
	public boolean mEmitsEnergy = false;
	/** The :126 activity flag (buffer >= mOutput). */
	public boolean mActive = false;
	/** The :52 cache pair (+ the :153 headroom), recounted each exchange phase. */
	public long mBatteryCount = -1, mChargeableCount = -1, mReceivablePower = 0;
	/** The explosion-prevention strikes (the :140-148 ladder, the transformer form). */
	public int mExplosionPrevention = 0;
	/** The energy domain (the NBT_ENERGY_ACCEPTED column — EU on the battery boxes, LU on the Crystal Chargers, task p35). */
	public TagData mEnergyType = TD.Energy.EU;
	/** The BE runtime facing mirror (the FRONT = output face; the state is the authority). */
	public byte mFacing = 2; // NORTH

	/** The family slot count (the NBT_INV_SIZE column; block-derived, or the test ctor's). */
	private final int mSlots;

	/** The offline test seam (the transformer mAdjacencyOverride form) — the EU emit side. */
	private IEnergyAdjacency mAdjacencyOverride = null;

	/** BET factory for BlockEntityType.Builder.of (the slot count resolves off the block state). */
	public GT6BatteryBoxBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-ctor precedent). The row data (tier/slots/BET) resolves off the block state at runtime. */
	public GT6BatteryBoxBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		this(aType, aPos, aState, resolveTier(aState), resolveSlots(aState));
	}

	/** The explicit-row constructor (the offline fixture — vanilla STONE states carry no GT6 block). */
	public GT6BatteryBoxBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState, int aTier, int aSlots) {
		this(aType, aPos, aState, aTier, aSlots, resolveBet(aType, aState));
	}

	private GT6BatteryBoxBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState, int aTier, int aSlots,
			BlockEntityType<? extends TileEntityBase03TicksAndSync> aResolved) {
		super(true, aResolved, aPos, aState);
		mSlots = aSlots;
		mInput = mOutput = gregtech6.registry.GTWireSpecs.V[aTier]; // the NBT_INPUT/NBT_OUTPUT columns, V[tier] both directions
		mEnergyType = resolveEnergyType(aState); // task p35 — the block's domain column (EU boxes / LU chargers)
		setInventory(new GTItemStackHandler(aSlots) {
			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				// the canInsertItem2 :199 verbatim — IItemEnergy with a matching type
				return aStack.getItem() instanceof IItemEnergy tEnergy
						&& (tEnergy.isEnergyType(mEnergyType, aStack, false) || tEnergy.isEnergyType(mEnergyType, aStack, true));
			}

			@Override
			public int getSlotLimit(int aSlot) {
				return 1; // the :218 getInventoryStackLimit verbatim
			}
		});
	}

	/** The family slot count off the block (the NBT_INV_SIZE column; 4 = the STONE/offline fallback). */
	static int resolveSlots(BlockState aState) {
		return aState.getBlock() instanceof GT6BatteryBoxBlock tBox ? tBox.slots() : 4;
	}

	/** The tier ladder index off the block (the V[tier] seat; 1 = the STONE/offline fallback). */
	static int resolveTier(BlockState aState) {
		return aState.getBlock() instanceof GT6BatteryBoxBlock tBox ? tBox.tier() : 1;
	}

	/** The energy domain off the block (task p35; EU = the STONE/offline fallback). */
	static TagData resolveEnergyType(BlockState aState) {
		return aState.getBlock() instanceof GT6BatteryBoxBlock tBox ? tBox.energyType().get() : TD.Energy.EU;
	}

	/** The BET resolution — small/large off the block's slot family (runtime: both registered; offline: the explicit type). */
	static BlockEntityType<? extends TileEntityBase03TicksAndSync> resolveBet(@Nullable BlockEntityType<?> aType, BlockState aState) {
		if (aType != null) {
			@SuppressWarnings("unchecked")
			BlockEntityType<? extends TileEntityBase03TicksAndSync> tTyped = (BlockEntityType<? extends TileEntityBase03TicksAndSync>) aType;
			return tTyped;
		}
		return aState.getBlock() instanceof GT6BatteryBoxBlock tBox && tBox.slots() == 16
				? GT6Batteries.BATTERY_BOX_LARGE_BE.get() : GT6Batteries.BATTERY_BOX_BE.get();
	}

	@Override
	public String getTileEntityName() {
		return mSlots == 16 ? "battery_box_large" : "battery_box"; // the BET registry paths
	}

	/** The family slot count (the :216 invsize term). */
	public int slots() {
		return mSlots;
	}

	GTItemStackHandler inv() { // package-private: the offline test seeding seam
		return mInventory; // the Root carrier
	}

	// ---------------------------------------------------------------------------
	// the tick (the :105-155 onTick2 shape, the 20-tick battery phase + the emit)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncFacingFromState();
		// the :108 20-tick throttle — the item callback is deliberately slow-paced upstream
		if (aTimer % 20 == 1) doBatteryPhase();
		doEmit();
	}

	/** The /setblock RCON path: the state is the authority (the transformer syncFacingFromState form). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6BatteryBoxBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6BatteryBoxBlock.FACING).get3DDataValue();
		}
	}

	/** Upstream {@code UT.Code.bind3} (UT.java:1555 verbatim) — clamp 0..7. */
	static long bind3(long aValue) {
		return Math.max(0, Math.min(7, aValue));
	}

	/** The buffer fill ratio numerator band (the :110/:117 divisor). */
	long bandScale() {
		return mInput * 40 * slots();
	}

	/**
	 * The :108-124 battery phase — the bind3 band arms, the IItemEnergy branches only
	 * (the COMPAT_EU_ITEM fallback of :118-:121 is the declared cut). The recount rides
	 * the same phase (upstream recounts on mInventoryChanged; the 20-tick cadence makes
	 * the flag redundant at these slot counts — the declared simplification).
	 */
	void doBatteryPhase() {
		recountBatteries();
		long tBand = bind3(mEnergy / bandScale());
		switch ((int) tBand) {
			case 0: for (int i = 0; i < slots(); i++) exchange(i, mOutput, 40, true); break;  // :111
			case 1: for (int i = 0; i < slots(); i++) exchange(i, mOutput, 20, true); break;  // :112
			case 6: for (int i = 0; i < slots(); i++) exchange(i, mInput , 20, false); break; // :113
			case 7: for (int i = 0; i < slots(); i++) exchange(i, mInput , 40, false); break; // :114
			default: break; // bands 2..5 idle
		}
	}

	/** One slot's pull (aPull=true: battery→buffer, the :111-:112 arm) or push (the :113-:114 arm). */
	private void exchange(int aSlot, long aSize, long aPackets, boolean aPull) {
		ItemStack tStack = inv().getStackInSlot(aSlot);
		if (tStack.isEmpty() || !(tStack.getItem() instanceof IItemEnergy tEnergy)) return;
		if (aPull) {
			mEnergy += aSize * tEnergy.doEnergyExtraction(mEnergyType, tStack, aSize, aPackets, true);
		} else {
			mEnergy -= aSize * tEnergy.doEnergyInjection(mEnergyType, tStack, aSize, aPackets, true);
		}
	}

	/** The :128-:139 recount (the can* faces decide the counts, the :153 headroom term). */
	void recountBatteries() {
		mBatteryCount = 0;
		mChargeableCount = 0;
		for (int i = 0; i < slots(); i++) {
			ItemStack tStack = inv().getStackInSlot(i);
			if (tStack.isEmpty() || !(tStack.getItem() instanceof IItemEnergy tEnergy)) continue;
			if (tEnergy.canEnergyInjection(mEnergyType, tStack, mInput)) mChargeableCount++;
			if (tEnergy.canEnergyExtraction(mEnergyType, tStack, mOutput)) mBatteryCount++;
		}
		mReceivablePower = mChargeableCount * mInput * 2; // :153
	}

	/** The :141-:151 emit — packets of mOutput out of the FRONT, buffer pays. */
	void doEmit() {
		mActive = mEnergy >= mOutput; // :126
		if (!mActive || mStopped) {
			mEmitsEnergy = false;
			return;
		}
		long tOutput = mMode == 0 ? mBatteryCount : Math.min(mMode, mBatteryCount); // :143
		if (tOutput > 0) {
			long tEmittedPackets = ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyType, mOutput, tOutput, this, adjacency());
			mEmitsEnergy = tEmittedPackets > 0; // :146
			mEnergy -= mOutput * tEmittedPackets; // :147
		}
	}

	// ---------------------------------------------------------------------------
	// the intake (the doInject :178-:193 verbatim + the :140-:148 overload ladder)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mReceivablePower <= 0) return 0; // :179
		aSize = Math.abs(aSize); // :180
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) { // :181 — the V[i]*2 gate
			if (aDoInject) overload(aSize, aEnergyType); // :182
			return aAmount; // consumed-but-not-stored (the upstream overcharge shape)
		}
		if (mEnergy >= capacity()) return 0; // :185
		long tInput = Math.min(capacity() - mEnergy, aSize * aAmount); // :186
		long tConsumed = Math.min(aAmount, (tInput / aSize) + (tInput % aSize != 0 ? 1 : 0)); // :186
		while (tConsumed > 1 && (tConsumed - 1) * aSize > mReceivablePower) tConsumed--; // :187
		if (aDoInject) {
			mReceivablePower -= tConsumed * aSize; // :189
			mEnergy += tConsumed * aSize; // :190
		}
		return tConsumed;
	}

	/** The overload ladder (the Base10 :140-:148 verbatim): 100 soft strikes, then the Root overcharge. */
	private void overload(long aSize, TagData aEnergyType) {
		if (mExplosionPrevention < 100) {
			mExplosionPrevention++;
			return;
		}
		overcharge(aSize, aEnergyType); // the Root :330 explosion family (offline: log-only)
	}

	/** The internal buffer cap (the :185/:216 term — 320 packets of headroom per slot). */
	public long capacity() {
		return mInput * 320 * slots();
	}

	// ---------------------------------------------------------------------------
	// the type face (the :202-:213 set, EU domain)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == mEnergyType; // :202 — the mEnergyType/mEnergyTypeOut lanes fold (every port row is EU/EU)
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyType.AS_LIST; // :212 folded — both lanes carry one type
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aSide == 6 || isInput(aSide)) && isEnergyType(aEnergyType, aSide, false); // :204 (the Root surface term folds — no covers)
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aTheoretical || !mStopped) && (aSide == 6 || isOutput(aSide)) && isEnergyType(aEnergyType, aSide, true); // :205
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return mOutput;}      // :206
	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mOutput;} // :207
	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mOutput;}      // :208
	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mInput;}   // :209
	// inputMin (V/2) and inputMax (V*2) ride the Root defaults over :209 — the doInject :181 gate reads them

	// The :210-:211 stored/capacity sums — the upstream face has NO port interface slot
	// (the port ITileEntityEnergy carries no getEnergyStored/getEnergyCapacity pair), so
	// these stay class-public readouts (the data-display/Jade seat).

	public long getEnergyStored(TagData aEnergyType, byte aSide) {
		long rAmount = 0; // :210 — the INVENTORY batteries (the buffer is internal)
		for (int i = 0; i < slots(); i++) {
			ItemStack tStack = inv().getStackInSlot(i);
			if (!tStack.isEmpty() && tStack.getItem() instanceof IItemEnergy tEnergy) rAmount += tEnergy.getEnergyStored(aEnergyType, tStack);
		}
		return rAmount;
	}

	public long getEnergyCapacity(TagData aEnergyType, byte aSide) {
		long rAmount = 0; // :211
		for (int i = 0; i < slots(); i++) {
			ItemStack tStack = inv().getStackInSlot(i);
			if (!tStack.isEmpty() && tStack.getItem() instanceof IItemEnergy tEnergy) rAmount += tEnergy.getEnergyCapacity(aEnergyType, tStack);
		}
		return rAmount;
	}

	// ---------------------------------------------------------------------------
	// the geometry + NBT
	// ---------------------------------------------------------------------------

	/** The :232-:233 convention — FRONT = output, ALL-BUT-FRONT = input. */
	public boolean isInput(byte aSide) {return aSide != mFacing;}
	public boolean isOutput(byte aSide) {return aSide == mFacing;}

	/** The adjacency seam (the transformer D1 form — the offline override + the live walk). */
	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	protected IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :75
		aNBT.putBoolean(NBT_ACTIVE, mActive); // :76
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :77
		aNBT.putBoolean(NBT_ACTIVE_ENERGY, mEmitsEnergy); // :78
		if (mMode != 0) aNBT.putByte(NBT_MODE, mMode); // :74
		aNBT.putByte(NBT_FACING, mFacing);
		aNBT.put(NBT_INVENTORY, serializeInventory());
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :60
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE); // :61
		if (aNBT.contains(NBT_ACTIVE_ENERGY, Tag.TAG_ANY_NUMERIC)) mEmitsEnergy = aNBT.getBoolean(NBT_ACTIVE_ENERGY); // :62
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :63
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE); // :64
		if (aNBT.contains(NBT_INPUT, Tag.TAG_ANY_NUMERIC)) mInput = aNBT.getLong(NBT_INPUT); // :65
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) mOutput = aNBT.getLong(NBT_OUTPUT); // :66
		if (aNBT.contains(NBT_ENERGY_ACCEPTED, Tag.TAG_STRING)) {
			TagData tParsed = gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType(aNBT.getString(NBT_ENERGY_ACCEPTED));
			if (tParsed != null) mEnergyType = tParsed; // :68 (the no-rogue-mint lookup)
		}
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) deserializeInventory(aNBT.getCompound(NBT_INVENTORY));
	}

	private CompoundTag serializeInventory() {
		//? if forge {
		return mInventory.serializeNBT();
		//?} else {
		/*return mInventory.serializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS); // 21.1: provider-first
		 *///?}
	}

	private void deserializeInventory(CompoundTag aTag) {
		//? if forge {
		mInventory.deserializeNBT(aTag);
		//?} else {
		/*mInventory.deserializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, aTag); // 21.1: provider-first
		 *///?}
	}

	/** The slot views the tests drive (the Root mInventory carrier, package-visible). */
	List<ItemStack> slotViews() {
		List<ItemStack> rStacks = new java.util.ArrayList<>(slots());
		for (int i = 0; i < slots(); i++) rStacks.add(inv().getStackInSlot(i));
		return rStacks;
	}

	//? if forge {
	// (the forge leg answers the ItemHandler face through the Root getCapability override
	// over the mInventory carrier — zero extra code here.)
	//?} else {
	/*// (21.1 seam: NeoForge removed BlockEntity#getCapability — the W4 registerBlockEntity
	// delegates to this member; no @Override. The whole-inventory view IS the battery
	// access face — the upstream getAccessibleSlotsFromSide2 answers every side the same
	// way, UT.Code.getAscendingArray. The GT6HopperBaseBlockEntity member shape.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) mInventory;
		}
		return null;
	}
	 *///?}

	// the MACHINES-tab seats and the command faces stay the W2/pool cards' surface — this
	// class carries the energy behavior only.
}
