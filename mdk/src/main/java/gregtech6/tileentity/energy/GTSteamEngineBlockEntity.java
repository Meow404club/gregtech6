package gregtech6.tileentity.energy;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Steam Engine — task p12-engine-steam, the full-semantics
 * port of gregtech/tileentity/energy/converters/MultiTileEntityEngineSteam.java
 * (Loader_MultiTileEntities.java:583-612, the Steam + Strong Steam Engine rows) as the
 * first REAL KU source: steam in the back, an AC square wave of KU out the front.
 *
 * <p>Steam-to-KU conversion (upstream :118-131, spec ①/②): every server tick while not
 * stopped, {@code tConversions = mTank.amount() / STEAM_PER_WATER} whole batches of
 * {@link GTFluids#STEAM_PER_WATER 200} L each convert to
 * {@code units(tConversions * STEAM_PER_WATER / STEAM_PER_EU, 10000, mEfficiency, F)}
 * KU ({@link GTFluids#STEAM_PER_EU 2} L steam per EU-unit — CS.java:240 — scaled by the
 * per-row efficiency in ten-thousandths); the batch litres leave the tank and each batch
 * yields 1 L of distilled water pushed to the four FACING-perpendicular sides (upstream
 * :125-130 {@code FACING_SIDES[mFacing]}, the CS.java:555 ring — remainder trashed, the
 * GarbageGT :130 call in a world without a fluid void). The fill-side gate (:239) is the
 * capability wrapper's: steam ONLY, on the BACK face ({@code OPOS[mFacing]}), only while
 * not stopped.
 *
 * <p>Heat state and output (spec ③/④): every SYNC_SECOND the heat state
 * {@code mState = min(31, scale(mEnergy, mCapacity, 32, F))} re-derives from the stored
 * KU (upstream :135; the port folds the global {@code SERVER_TIME % 20} pulse into the
 * per-BE {@code mTimer % 20} — the 03 dispatcher's timer, same 20-tick cadence). The
 * packet amplitude is {@code tOutput = mOutput * (mState + 1) / 16} (:138) and the
 * activation gate {@code mActive = !mStopped && mEnergy > tOutput && tOutput * 2 > mOutput}
 * (:141 — the formula verbatim; for whole-ratio outputs it bites at mState &gt;= 8).
 *
 * <p>The AC square wave (spec ⑤): the 2-bit piston phase advances by the :113-114 form
 * ({@code mPiston += 1; mPiston &= 3;}) once every {@code 32 - mState} ticks while active
 * (hotter engine = faster piston), and every active tick the engine pushes ONE packet of
 * {@code mPiston > 1 ? -tOutput : tOutput} KU (:146) — the +/+/-/- signed square wave
 * whose positive-to-non-positive transitions drive the machine :815 alternating arm (the
 * TileEntityBasicMachine.java:373 gate, ALL_ALTERNATING=(F,KU) reference equality). Unlike
 * the P11 test rig ({@code GTEnergySourceBlockEntity}, which advanced the phase once per
 * emit and did not persist it — the declared rig simplification), the REAL machine runs
 * the timer-gated :113 advance and PERSISTS {@code mPiston} across reloads (upstream
 * writeToNBT2 :88 NBT_PISTON) — the task card's declared fidelity difference.
 *
 * <p>Dissipation and overheat (spec ⑥/⑦): the stored energy leaves EVERY active tick
 * regardless of acceptance (:147 — pushing into a wall dissipates it, upstream-faithful).
 * When {@code mEnergy >= mCapacity} (:152-161): the store clamps to capacity-1, and if the
 * heat state is above 30 the engine stops itself and voids the steam tank (the :155-156
 * fizz+vent, the sound cropped with the SFX pool); otherwise the state pins to 31. While
 * stopped the store bleeds {@code max(1, mCapacity/64)} per tick (:164).
 *
 * <p>The soft-hammer toggle (:175) is a POOL cut (no tool interaction face on this card);
 * the on/off gate is the {@code /gt6engine mode <pos> on|off} command — the
 * {@code ITileEntityAdjacentOnOff.setStateOnOff} seam (upstream :225) in command form.
 *
 * <p>Port-side feed seam (declared, no upstream line): upstream steam arrived pushed by
 * pipes/funnels into {@code getFluidTankFillable2}; this port adds a back-face PULL —
 * each server tick the engine drains up to its free tank space from the adjacent fluid
 * handler on its back face (steam only, the :239 gate), consulted through the NEIGHBOUR's
 * top face (see {@link #mFluidAdjacency}). The RCON acceptance chain's {@code /gt6tank
 * fill}-ed metal drum plays the pipe. POWER_CONDUCTING caveat (the
 * p12-research-steam-barrel-gasproof verdict): upstream NO tank holds steam — the
 * steam flag set carries POWER_CONDUCTING, so a filled tank is destroyed the next tick
 * (wood/plastic melt at their 340/370 K ceilings, metal/Logistics fizz away); the port
 * barrels carry no such destruction chain, a DECLARED deviation (steam persists here),
 * and the metal drum is the closest-to-legal acceptance carrier. The capability fill
 * door ({@link EngineFluidHandler}) stays open for real pipe carriers.
 *
 * <p>Facing: the BlockState {@code FACING} is the command-side authority and
 * {@link #mFacing} the BE runtime mirror, the GTCrankBlockEntity form — re-synced from the
 * state at each server tick head, written alongside by placement. Emission face =
 * mFacing only (upstream :232), steam face = its opposite.
 *
 * <p>Row configuration (efficiency/capacity/output/hardness) rides the BLOCK carrier
 * ({@link GT6Kinetics.SteamEngineBlock#row()}, the GTBarrelBlock capacityL pattern) —
 * upstream wrote them as registration NBT read back at :75-78; the port re-derives them
 * from the placed block instead of persisting them (a stateless re-read, no deviation in
 * behaviour). Non-engine blocks (offline test fixtures) keep the class defaults.
 */
public class GTSteamEngineBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The NBT keys (upstream CS names flattened to the plain lowercase port convention; piston/state are the :87-88 persistence). */
	public static final String NBT_ENERGY = "energy";
	public static final String NBT_VISUAL = "visual";
	public static final String NBT_PISTON = "piston";
	public static final String NBT_ACTIVE = "active";
	public static final String NBT_STOPPED = "stopped";
	public static final String NBT_EMITTING = "emitting";
	public static final String NBT_TANK = "tank";

	/** The emitted energy type (upstream :64; the loader rows pass NBT_ENERGY_EMITTED = TD.Energy.KU). */
	public static final TagData EMITTED_TYPE = TD.Energy.KU;

	/** The stored KU (upstream :63, NBT_ENERGY :70/:86). */
	public long mEnergy = 0;
	/** The KU store ceiling (upstream :63; the loader row NBT_CAPACITY → the block row here). */
	public long mCapacity = 640000;
	/** The nominal KU/t output (upstream :63; NBT_OUTPUT → the block row here, 8..256 over the family). */
	public long mOutput = 64;
	/** The efficiency in ten-thousandths (upstream :62; NBT_EFFICIENCY → the block row here, 3000..6450). */
	public short mEfficiency = 10000;
	/** The heat state 0..31 (upstream :61 — the synced visual byte). */
	public byte mState = 0;
	/** The 2-bit piston phase (upstream :61 — PERSISTED, the card's fidelity point). */
	public byte mPiston = 0;
	/** The running latch (upstream :60). */
	public boolean mActive = false;
	/** The soft-stopped latch (upstream :60 — the command gate + the overheat stop). */
	public boolean mStopped = false;
	/** Whether the last tick's emit was accepted (upstream :60 {@code mEmitsEnergy}). */
	public boolean mEmitsEnergy = false;
	/** The BE runtime facing mirror (the BlockState FACING is the authority; the crank form). */
	public byte mFacing = 2; // NORTH

	/** The steam tank (upstream :65 — capacity re-derived at :80 as STEAM_PER_WATER * mOutput * 2). */
	public final FluidTankGT mTank = new FluidTankGT(GTFluids.STEAM_PER_WATER * 64L * 2);

	// The o-pairs of the :195-205 change check (the sync trigger).
	private byte oState = 0;
	private boolean oActive = false;

	// ---------------------------------------------------------------------------
	// the offline fluid test seams (live paths never see these; no upstream line)
	// ---------------------------------------------------------------------------

	/**
	 * The steam identity test (upstream {@code FL.Steam.is}, :239). Overridable because the
	 * gt6 steam fluid does not exist in the offline test registry — the tests plug a vanilla
	 * stand-in through {@link #setFluidSeams}.
	 */
	public Function<Fluid, Boolean> mSteamMatch = f -> f == GTFluids.STEAM.source.get();

	/** The byproduct stack maker (upstream {@code FL.DistW.make}, :125) — the same offline seam. */
	public IntSupplier mByproductAmount = () -> 1;
	public Function<Integer, FluidStack> mByproductMake = amount -> new FluidStack(GTFluids.DISTILLED_WATER.source.get(), amount);

	/**
	 * The live neighbour fluid handler lookup, per side of THIS engine; null when absent.
	 * The neighbour is consulted through its TOP face — the TileEntityCokeOven
	 * .fluidHandlerAt :193 pinned-UP precedent: the p5 barrel side rules let a tank GIVE
	 * fluids only from bottom (heavier than air) / top (lighter than air) faces, and steam
	 * (density −100) leaves through the top; the byproduct push re-uses the same handle
	 * (fills are admitted from any face). Offline-replaceable (the test seams key on this
	 * engine's own side, the live lambda only routes the POSITION).
	 */
	public Function<Byte, IFluidHandler> mFluidAdjacency = aSide -> {
		if (!hasLevel()) return null;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
				Direction.UP).orElse(null);
	};

	/** The offline seam setter (the setAdjacencyOverride shape of the P8/P11 rigs). */
	public void setFluidSeams(Function<Fluid, Boolean> aSteamMatch, Function<Integer, FluidStack> aByproductMake) {
		if (aSteamMatch != null) mSteamMatch = aSteamMatch;
		if (aByproductMake != null) mByproductMake = aByproductMake;
	}

	// ---------------------------------------------------------------------------
	// construction (the crank/BET-factory shape)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTSteamEngineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime. A placed family block injects its registration row
	 * (the GTBarrelBlockEntity block-carrier read); foreign blocks keep the defaults.
	 */
	public GTSteamEngineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.STEAM_ENGINE_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6Kinetics.SteamEngineBlock tBlock) {
			GT6Kinetics.SteamEngineRow tRow = tBlock.row();
			mEfficiency = tRow.efficiency();
			mCapacity = tRow.energyCapacity();
			setOutput(tRow.outputKU());
		}
	}

	@Override
	public String getTileEntityName() {
		return "steam_engine"; // BET registry path mirrors it (GTBlockEntities.STEAM_ENGINE_BE)
	}

	/** The upstream :80 tank formula — 200 L of tank per 2 EU/t of nominal output, ×2 headroom. */
	public long steamTankCapacity() {
		return GTFluids.STEAM_PER_WATER * mOutput * 2;
	}

	/** The output setter (row load + the offline tests) — re-derives the tank capacity, the :80 pairing. */
	public void setOutput(long aOutput) {
		mOutput = Math.max(1, aOutput);
		mTank.setCapacity(steamTankCapacity());
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :112-166, the full-semantics port)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// the piston phase — upstream :113-116, OUTSIDE the server branch (both sides tick
		// it, the client copy rides the synced mActive/mState); the :196 bind rides along
		mState = bindState(mState);
		if (mActive && aTimer % Math.max(1, 32 - mState) == 0) {
			mPiston += 1; mPiston &= 3; // upstream :114-115
		}
		if (!aIsServerSide) return;

		syncFacingFromState(); // the crank form: the /setblock RCON path re-syncs the mirror

		// the port-side back-face steam feed (the class-doc seam) — respects the :239 gate
		feedSteamFromBack();

		// Convert Steam to Energy — upstream :119-132
		if (!mStopped) {
			long tConversions = mTank.amount() / GTFluids.STEAM_PER_WATER;
			if (tConversions > 0) {
				mEnergy += units(tConversions * GTFluids.STEAM_PER_WATER / GTFluids.STEAM_PER_EU, 10000, mEfficiency, false); // :123
				mTank.remove(tConversions * GTFluids.STEAM_PER_WATER); // :124
				pushByproduct((int) tConversions); // :125-130
			}
		}

		// Set State — upstream :135, the SYNC_SECOND pulse folded to the per-BE timer
		if (aTimer % 20 == 0) mState = bindState((byte)Math.min(31, scale(mEnergy, mCapacity, 32, false)));

		// Set the output depending on how "hot" the state of the Engine is — upstream :138
		long tOutput = (mOutput * (mState + 1)) / 16;

		// Checks if this Engine is supposed to be active — upstream :141 verbatim
		mActive = (!mStopped && mEnergy > tOutput && tOutput * 2 > mOutput);
		mEmitsEnergy = false; // :142

		// Emit Energy — upstream :145-149 (the :148 structural check is pool)
		if (mActive) {
			mEmitsEnergy = (ITileEntityEnergy.Util.emitEnergyToNetwork(EMITTED_TYPE, mPiston > 1 ? -tOutput : tOutput, 1, this, adjacency()) > 0);
			mEnergy -= tOutput; // :147 — UNCONDITIONAL: the dissipation into an unaccepted push
		}

		// Well the Engine stops if it has too much Steam and just vents everything — :152-161
		if (mEnergy >= mCapacity) {
			mEnergy = mCapacity - 1;
			if (mState > 30) {
				mStopped = true; // :155 — the overheat stop
				mTank.setEmpty(); // :156 — the vent
				// the :157 fizz sound rides the SFX pool
			} else {
				mState = 31; // :159
			}
		}

		// Release the Energy when inactive — upstream :164
		if (mStopped && mEnergy > 0) mEnergy = Math.max(0, mEnergy - Math.max(1, mCapacity / 64));

		// the :195-198 change check → the vanilla two-channel sync (the crank form)
		if (mActive != oActive || mState != oState) updateClientData();
		oActive = mActive;
		oState = mState;
	}

	/**
	 * The port-side back-face steam feed: drain up to the free tank space from the back
	 * neighbour's fluid handler — steam only, only while not stopped (the :239 gate the
	 * capability wrapper shares). The neighbour offers through its own handler; a refusal
	 * (wrong fluid / stopped / full tank / no handler) is a silent no-op, exactly like a
	 * pipe hitting the :239 null.
	 */
	private void feedSteamFromBack() {
		if (mStopped) return; // :239 — the stopped engine refuses fill
		IFluidHandler tSource = mFluidAdjacency.apply(backSide());
		if (tSource == null) return;
		long tFree = mTank.capacity() - mTank.amount();
		if (tFree <= 0) return;
		FluidStack tOffered = tSource.drain(FluidTankGT.bindInt(tFree), FluidAction.SIMULATE);
		if (tOffered == null || tOffered.isEmpty() || !mSteamMatch.apply(tOffered.getFluid())) return;
		FluidStack tDrawn = tSource.drain(tOffered.getAmount(), FluidAction.EXECUTE);
		if (tDrawn == null || tDrawn.isEmpty()) return;
		mTank.add(tDrawn.getAmount(), tDrawn);
	}

	/**
	 * The :125-130 byproduct push — {@code tConversions} litres of distilled water into the
	 * four FACING-perpendicular sides (CS.java:555 {@code FACING_SIDES} ring), first handler
	 * with room wins per litre-batch; the remainder voids (the :130 GarbageGT call).
	 */
	private void pushByproduct(int aConversions) {
		int tLeft = aConversions * mByproductAmount.getAsInt();
		byte[] tRing = facingSideRing(mFacing);
		for (byte tDir : tRing) {
			if (tLeft <= 0) break;
			IFluidHandler tTarget = mFluidAdjacency.apply(tDir);
			if (tTarget == null) continue;
			FluidStack tPush = mByproductMake.apply(tLeft);
			if (tPush == null || tPush.isEmpty()) return;
			int tFilled = tTarget.fill(tPush, FluidAction.EXECUTE);
			tLeft -= tFilled;
		}
		// whatever no side accepted is trashed (upstream :130)
	}

	// ---------------------------------------------------------------------------
	// the pure conversion math (the upstream UT.Code / :135/:138/:141 expressions,
	// statics so the offline tests can pin the truth tables — the
	// TileEntityBasicMachine.units :854 local-copy precedent)
	// ---------------------------------------------------------------------------

	/** UT.Code.units (UT.java:1677-1683 verbatim — the root UT.Code copy lacks it; the mdk local-copy precedent). */
	public static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
		if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	/** UT.Code.scale (UT.java:1534-1537 verbatim). */
	public static long scale(long aValue, long aMax, long aScale, boolean aInvert) {
		long rScale = (aValue <= 0 ? 0 : aValue >= aMax ? aScale : aScale <= 2 ? 1 : 1 + (aValue * (aScale-1)) / aMax);
		return aInvert ? aScale - rScale : rScale;
	}

	/** The :135 state form (the min-31 clamp included; the :196 bind rides the callers). */
	public static byte bindState(byte aState) {
		return (byte)Math.max(0, Math.min(31, aState));
	}

	/** The :138 amplitude form. */
	public static long outputAt(long aOutput, byte aState) {
		return (aOutput * (bindState(aState) + 1)) / 16;
	}

	/** The :141 activation gate form. */
	public static boolean activationGate(boolean aStopped, long aEnergy, long aOutput, byte aState) {
		long tOutput = outputAt(aOutput, aState);
		return !aStopped && aEnergy > tOutput && tOutput * 2 > aOutput;
	}

	// ---------------------------------------------------------------------------
	// facing (the crank shape: state is the authority, the field the runtime mirror)
	// ---------------------------------------------------------------------------

	/** The emission face (the KU square wave leaves here, upstream :232). */
	public byte frontSide() {
		return mFacing;
	}

	/** The steam face (upstream {@code OPOS[mFacing]}, :239). */
	public byte backSide() {
		return (byte)Direction.from3DDataValue(mFacing).getOpposite().get3DDataValue();
	}

	/** CS.java:555 FACING_SIDES — the four sides perpendicular to the facing axis (the byproduct ring). */
	public static byte[] facingSideRing(byte aFacing) {
		return switch (aFacing) {
			case 0, 1 -> new byte[] {2, 3, 4, 5};
			case 2, 3 -> new byte[] {0, 1, 4, 5};
			case 4, 5 -> new byte[] {0, 1, 2, 3};
			default -> new byte[] {};
		};
	}

	/** The placement mirror (the GTCrankBlockEntity.setFacingFromPlacement form). */
	public void setFacingFromPlacement(net.minecraft.world.entity.player.Player aPlayer) {
		mFacing = (byte)aPlayer.getDirection().get3DDataValue();
		setChanged();
	}

	/** The /setblock RCON path re-sync (keyed on the PROPERTY instance, the crank form). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6Kinetics.SteamEngineBlock.FACING)) {
			mFacing = (byte)getBlockState().getValue(GT6Kinetics.SteamEngineBlock.FACING).get3DDataValue();
		}
	}

	public byte getFacing() {
		return mFacing;
	}

	/** The command-side on/off gate (upstream :225 setStateOnOff in command form; the :175 soft hammer is pool). */
	public void setStopped(boolean aStopped) {
		mStopped = aStopped;
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the energy adjacency seam (the P8/P11/P12-crank form, verbatim)
	// ---------------------------------------------------------------------------

	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	private IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// ---------------------------------------------------------------------------
	// the face family (the upstream override set :230-237 verbatim; accepting and the
	// injection door ride the 01Root defaults — the upstream non-overrides)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == EMITTED_TYPE; // upstream :230
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aSide == mFacing && aEnergyType == EMITTED_TYPE; // upstream :232 (aTheoretical-insensitive like upstream)
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mOutput; // upstream :233
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return EMITTED_TYPE.AS_LIST; // upstream :236
	}

	// the :234/:235 stored/capacity pair rides the capacitor subsystem the port cut
	// (TileEntityBase01Root.java:331, ADR D1 ruling 1) — no interface surface, no override.

	// ---------------------------------------------------------------------------
	// the fluid capability door (the :239/:240/:241 trio in 1.20.1 capability form)
	// ---------------------------------------------------------------------------

	private final IFluidHandler mFluidHandler = new EngineFluidHandler();

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// the :239 side gate lives HERE — only the back face exposes the fill door
			// (the side-less query stays open, the BarrelFluidHandler convention)
			if (aSide != null && aSide.get3DDataValue() != backSide()) {
				return LazyOptional.empty();
			}
			return LazyOptional.of(() -> mFluidHandler).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	/**
	 * The tank door behind the back-face capability: fill = the :239 fluid+mode gate (the
	 * FACE half lives in {@link #getCapability}), drain = the :240 null (the engine tank is
	 * not drainable). The PULL path never comes through here (it reads the NEIGHBOUR's
	 * handler directly).
	 */
	private class EngineFluidHandler implements IFluidHandler {
		@Override public int getTanks() {return mTank.AS_ARRAY.length;}
		@Override public FluidStack getFluidInTank(int aTank) {return aTank == 0 && mTank.get() != null ? mTank.get() : FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return aTank == 0 ? mTank.getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return aTank == 0 && mTank.isFluidValid(aStack);}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			// upstream :239 — the tank only accepts steam on the BACK face while not stopped;
			// the face routing happens in getCapability (the side-wrapped handler below)
			if (mStopped || !mSteamMatch.apply(aResource.getFluid())) return 0;
			return mTank.fill(aResource, aAction);
		}

		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;} // upstream :240
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;} // upstream :240
	}

	// ---------------------------------------------------------------------------
	// NBT (the upstream :84-94 set; the row config rides the block carrier instead)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // upstream :86
		aNBT.putByte(NBT_VISUAL, mState); // upstream :87 (NBT_VISUAL)
		aNBT.putByte(NBT_PISTON, mPiston); // upstream :88 — the PERSISTED piston phase
		aNBT.putBoolean(NBT_ACTIVE, mActive); // upstream :89
		aNBT.putBoolean(NBT_STOPPED, mStopped); // upstream :90
		aNBT.putBoolean(NBT_EMITTING, mEmitsEnergy); // upstream :91
		mTank.writeToNBT(aNBT, NBT_TANK); // upstream :93 (NBT_TANK+".0")
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // upstream :70
		if (aNBT.contains(NBT_VISUAL, Tag.TAG_ANY_NUMERIC)) mState = bindState(aNBT.getByte(NBT_VISUAL)); // upstream :71
		if (aNBT.contains(NBT_PISTON, Tag.TAG_ANY_NUMERIC)) mPiston = (byte)(aNBT.getByte(NBT_PISTON) & 3); // upstream :72 — the reload fidelity point
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE); // upstream :73
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // upstream :74
		if (aNBT.contains(NBT_EMITTING, Tag.TAG_ANY_NUMERIC)) mEmitsEnergy = aNBT.getBoolean(NBT_EMITTING); // upstream :76
		mTank.readFromNBT(aNBT, NBT_TANK); // upstream :80 half one
		mTank.setCapacity(steamTankCapacity()); // upstream :80 half two
	}
}
