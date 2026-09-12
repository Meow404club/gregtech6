package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
 *///?}

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.GT6Mod;
import gregtech6.registry.GT6FeConverters;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The FE→EU converter core (task p28-b-fe-converter-machine) — the DECLARED DEVIATION
 * machine of decisions.p28-eu-inbound-converter: upstream 1.7.10 deliberately has NO
 * RF→EU converter (Greg wall RF — the Flux family converts RF→HU/KU/RU/LU only,
 * Loader_MultiTileEntities.java:824-957, and EU machines reject RF by type equality), so
 * this block is a GTCEu-form ADDITION, not an upstream port. The semantic anchor is still
 * upstream: the conversion core of
 * {@code gregapi.tileentity.behavior.TE_Behavior_Energy_Converter.doConversion}
 * (:61-94, ridden by TileEntityBase10EnergyConverter) fused with the GTCEu ConverterTrait
 * FE intake face (ConverterTrait.java:98-148). The reference for the pull-side math is
 * root {@link EnergyBridge#extractFe} (task p28-a-fe-inbound-math, merged 89d4cced) — the
 * conversion core below drives it per tick; the machine, its buffer and its overload
 * explosion live BEHIND that seam, not in it.
 *
 * <h2>The ULV balance ruling (the user's mid-card correction, 2026-09-12)</h2>
 *
 * ONE machine, not a ladder: ULV = {@code V[0]} = 8 EU, one amp. RF is only ever allowed
 * to enter the energy chain at its very bottom — the steam-boiler → kinetic → LV+-grid
 * progression wall stays intact, because an 8 EU packet is BELOW the input minimum of
 * every ported EU consumer (the oven T1 demands 16, the EnergyGate.gateInjection :50
 * small-packet arm swallows the offer), so ULV power drives nothing but the outbound
 * bridge face. The amps stay pinned at 1 — more amps would sum back to LV-class
 * throughput and break the wall. The lossless 4:1 ratio (CS.RF_PER_EU) is unchanged.
 * The min-max door (one whole packet buffered) plus the one-packet-per-tick cap pin the
 * throughput at exactly 8 EU/t.
 *
 * <h2>The core, per server tick (the upstream doConversion shape)</h2>
 * <ol>
 * <li><b>Pull</b> — the FE intake's dual support, half 2 (GTCEu is push-only:
 *     ConverterTrait.extractEnergy returns 0 and canExtract is false, :124-126/:141-143 —
 *     issue #2089 starves such machines behind pull-style FE networks). Whole GT packet
 *     trains are pulled out of adjacent foreign FE sources through
 *     {@link EnergyBridge#extractFe} at the machine's packet size, capped at
 *     {@link #AMPS} packet per tick across all sides. Each pulled packet lands in the
 *     buffer as packetFe FE (the source already lost the FE — exact whole-packet
 *     accounting, the floor-aligned 4:1; a hostile source's non-aligned remainder stays
 *     IN the source).</li>
 * <li><b>Push</b> — half 1 (live): the platform FE capability
 *     ({@code IEnergyStorage} on both legs) accepts foreign-cable pushes into the buffer,
 *     floor-aligned at the ratio (the GTCEu {@code received -= received % ratio} form,
 *     ConverterTrait.java:108).</li>
 * <li><b>Emit</b> — the standard GT emit push (the ITileEntityEnergy face is the existing
 *     one, GT consumer machines untouched): whole EU packets of 8 EU, at most
 *     {@link #AMPS} per tick, only when at least ONE whole packet is buffered (the
 *     upstream min gate {@code mCanEmitEnergy = tOutput >= mMin}, :64, with the port's
 *     mMin = one packet). The buffer is charged ONLY for packets the network actually
 *     used (the upstream 只扣实收 arm, :87).</li>
 * <li><b>Overload</b> — the upstream overload ladder of doConversion (:68-77): a buffer
 *     above the design capacity is unreachable through the intake faces (both clamp), so
 *     it can only arrive persisted from a foreign writer — exactly the upstream
 *     "Machine overloaded on Chunkload" scenario. The 2-tick grace is upstream verbatim
 *     (:72 {@code if (aTimer > 2)}): within it the buffer is dumped with the DEB log;
 *     after it the machine overcharges ({@code overcharge(aSize, mEnergyOUT.mType)},
 *     TileEntityBase10EnergyConverter :122-126 → Root :330) — the GT-side machine
 *     explosion is NOT a trim target here (research.p28-r-eu-inbound risk ③), unlike the
 *     p26 OUTBOUND bridge where the FOREIGN-side checkOverCharge was cut. DECLARED
 *     COLLAPSE within the ladder: upstream counts 100 soft overloads (dump only) before
 *     the overcharge arms (TileEntityBase10EnergyConverter :140-148) — that ladder exists
 *     for multiplier misconfiguration, a path the FE face cannot have (FE carries no
 *     voltage/multiplier), so the port goes straight to the overcharge call.</li>
 * </ol>
 *
 * <p>Sizing: the buffer is the GTCEu capacitor (ConverterTrait.java:38-42,
 * {@code V[tier] * 16 * amps} EU) carried in FE units: capacityFe = 8 * 1 * 16 * 4 =
 * 512 FE (= 128 EU = 16 packets).
 *
 * <p>DECLARED SIMPLIFICATIONS (the research card's design freedom — "面几何/邻接数/节流
 * 是设计自由度"): FE intake on ALL six sides (no facing split — the GTCEu front/output
 * split buys nothing for a cube rig), EU emit to ALL six sides (the
 * GTEnergySourceBlockEntity all-sides emit shape), no mode gate (the converter runs
 * continuously like the upstream family; the upstream mStopped/mWasteEnergy/multiplier
 * NBT surface is the pool cut).
 *
 * <p>The BE extends the 03 tick family (the GTEnergySourceBlockEntity precedent) — a
 * GT-family identity, so GT emitters reach it through doEnergyInjection dispatch and the
 * wire handshakes through the ITileEntityEnergy probes; the FE capability below is its
 * OWN intake face, never the outbound bridge (01Root-family receivers never reach
 * EnergyBridge, ITileEntityEnergy.java:287).
 *
 * <p>Persistence: the buffered FE rides "fe" (the 01Root "te_name" key rides the base).
 */
public class GT6FeConverterBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The stored-FE NBT key. */
	public static final String NBT_FE = "fe";

	/**
	 * The packet count per tick (the upstream mMultiplier/NBT_MULTIPLIER slot and the
	 * GTCEu amps constructor param, pinned to 1 — the ULV balance ruling: more amps would
	 * sum back to LV-class throughput and break the progression wall).
	 */
	public static final long AMPS = 1;

	/** The GTCEu capacitor depth in ticks (ConverterTrait.java:38-42 {@code V[tier] * 16 * amps}). */
	public static final long BUFFER_TICKS = 16;

	/**
	 * The tier voltage in EU — ULV, {@code V[0]} = 8, the ONLY tier (the balance ruling).
	 * A field rather than a constant so the offline tests read the live value through the
	 * same face the core math uses; the machine is registered with exactly this tier.
	 */
	public long mVoltage = GT6FeConverters.VOLTAGE_ULV;

	/** The buffered FE (always a multiple of {@link CS#RF_PER_EU} — both intake faces align). */
	public long mBufferFe = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6FeConverterBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET (the
	 * GTEnergySourceBlockEntity dual-constructor precedent). Single tier: no per-block
	 * state to read.
	 */
	public GT6FeConverterBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6FeConverters.FE_CONVERTER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "fe_converter"; // BET registry path mirrors it (GT6FeConverters.FE_CONVERTER_BE)
	}

	// ---------------------------------------------------------------------------
	// the sizing math (MC-free, unit-tested)
	// ---------------------------------------------------------------------------

	/** One EU packet of this machine's tier, in FE (8 EU x 4 = 32 FE). */
	public long packetFe() {
		return Math.abs(mVoltage) * CS.RF_PER_EU;
	}

	/** The capacitor capacity in FE (the GTCEu V*16*amps EU buffer, carried in FE units = 512 FE). */
	public long capacityFe() {
		return Math.abs(mVoltage) * AMPS * BUFFER_TICKS * CS.RF_PER_EU;
	}

	// ---------------------------------------------------------------------------
	// the FE push intake (the capability delegate — MC-free math, unit-tested)
	// ---------------------------------------------------------------------------

	/**
	 * The push half of the dual-support intake: a foreign cable calls the platform
	 * capability, the capability delegates here. Acceptance is capacity-clamped and
	 * floor-aligned at the ratio (the GTCEu {@code received -= received % ratio} form,
	 * ConverterTrait.java:108) so the buffer only ever holds whole EU units — a partial
	 * FE tail stays in the cable (GTCEu semantics; the packet-level alignment happens at
	 * EMIT time, where a too-small tail simply never forms a whole packet).
	 */
	public int pushFe(int aAmount, boolean aSimulate) {
		if (aAmount <= 0) return 0;
		long tAccepted = Math.min((long)aAmount, capacityFe() - mBufferFe);
		tAccepted -= tAccepted % CS.RF_PER_EU; // ConverterTrait.java:108 form, ratio unit
		if (tAccepted <= 0) return 0;
		if (!aSimulate) {
			mBufferFe += tAccepted;
			setChanged();
		}
		return (int)tAccepted;
	}

	// ---------------------------------------------------------------------------
	// the FE pull intake (the EnergyBridge.extractFe driver — the p28-a seam)
	// ---------------------------------------------------------------------------

	/**
	 * The offline test seam (the setAdjacencyOverride precedent): when set, it replaces
	 * the live neighbour resolution of {@link #pullOnce}.
	 */
	private EnergyBridge.IFESource mPullSourceOverride = null;

	void setPullSourceOverride(@Nullable EnergyBridge.IFESource aSource) {
		mPullSourceOverride = aSource;
	}

	/**
	 * The pull half of the dual-support intake, driven once per server tick: whole packet
	 * trains out of the adjacent foreign FE sources, at most {@link #AMPS} packet per
	 * tick across all sides, each landed in the buffer as packetFe FE. A side is a pull
	 * target exactly when it exposes an FE storage that canExtract (the push-only sink
	 * fixtures stay untouched). Pull requires one whole packet of buffer room — a nearly
	 * full capacitor never strands a pulled packet against the capacity.
	 */
	void pullOnce() {
		long tSpace = capacityFe() - mBufferFe;
		if (tSpace < packetFe()) return;
		long tAmps = AMPS;
		for (byte tSide = 0; tSide < 6 && tAmps > 0; tSide++) {
			EnergyBridge.IFESource tSource = mPullSourceOverride != null ? mPullSourceOverride : resolveFeSource(tSide);
			if (tSource == null) continue;
			long tPulled = EnergyBridge.extractFe(tSource, mVoltage, tAmps); // the 4:1 floor-aligned packets
			if (tPulled > 0) {
				mBufferFe += tPulled * packetFe();
				tAmps -= tPulled;
				setChanged();
			}
			if (mPullSourceOverride != null) break; // the offline seam serves one synthetic source
		}
	}

	/** The live neighbour query — per-leg hunks (the GT6FeBatteries arm shape), null when the side is not an extractable FE source. */
	//? if forge {
	@Nullable
	private EnergyBridge.IFESource resolveFeSource(byte aSide) {
		if (!hasLevel()) return null;
		Direction tDirection = Direction.from3DDataValue(aSide);
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		IEnergyStorage tStorage = tNeighbor.getCapability(ForgeCapabilities.ENERGY, tDirection.getOpposite()).orElse(null);
		if (tStorage == null || !tStorage.canExtract()) return null;
		return tStorage::extractEnergy; // the two-argument lambda shape (EnergyBridge.IFESource)
	}
	//?} else {
	/* @Nullable
	private EnergyBridge.IFESource resolveFeSource(byte aSide) {
		if (!hasLevel()) return null;
		Direction tDirection = Direction.from3DDataValue(aSide);
		// 21.1: the level BlockCapability query (the GT6FeBatteries neo arm shape; 21.1.249
		// deleted BlockEntity#getCapability)
		IEnergyStorage tStorage = getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
				getBlockPos().relative(tDirection), tDirection.getOpposite());
		if (tStorage == null || !tStorage.canExtract()) return null;
		return tStorage::extractEnergy;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// the GT emit push (the existing ITileEntityEnergy face — consumer machines untouched)
	// ---------------------------------------------------------------------------

	/**
	 * The D1 adjacency seam, verbatim the GTEnergySourceBlockEntity shape: live-resolved
	 * per packet, with the offline test override.
	 */
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

	/**
	 * The emit arm: whole 8 EU packets into the GT network, at most {@link #AMPS} per
	 * tick, gated on one whole packet buffered (the upstream min gate), deducting ONLY
	 * the packets the network actually used (the upstream 只扣实收 arm,
	 * TE_Behavior_Energy_Converter.java:87). Split out for the offline tests (the
	 * emitOnce precedent).
	 */
	void emitOnce() {
		long tPackets = Math.min(AMPS, mBufferFe / packetFe()); // the min gate: one whole packet minimum
		if (tPackets <= 0) return;
		long tUsed = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, mVoltage, tPackets, this, adjacency());
		if (tUsed > 0) {
			mBufferFe -= tUsed * packetFe();
			setChanged();
		}
	}

	// ---------------------------------------------------------------------------
	// the overload ladder (the upstream doConversion :68-77 shape; see the class doc)
	// ---------------------------------------------------------------------------

	void checkOverload(long aTimer) {
		if (mBufferFe <= capacityFe()) return;
		if (aTimer <= 2) { // the upstream chunkload grace, :72 verbatim shape (the DEB line as the logger line, the Root :342 form)
			GT6Mod.LOGGER.info("Machine overloaded on Chunkload with: " + mBufferFe
					+ " " + TD.Energy.EU.getLocalisedNameLong());
			mBufferFe = 0;
			setChanged();
			return;
		}
		overcharge(mBufferFe, TD.Energy.EU); // the base :122-126 call, Root :330 — the GT explosion stays
		mBufferFe = 0;
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :106-109 — doConversion per server tick)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		pullOnce();
		emitOnce();
		checkOverload(aTimer);
	}

	// ---------------------------------------------------------------------------
	// the GT energy face (the pure-source shape — the GTEnergySourceBlockEntity family)
	// The output size band is the Root default pair (min = rec/2, max = rec*2) which is
	// EXACTLY the upstream converter readEnergyBehavior :76-77 form (tOutput/2, tOutput,
	// tOutput*2) — only the recommended size carries the tier voltage.
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == TD.Energy.EU; // emits EU, accepts nothing (the FE face is the intake)
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.EU.AS_LIST;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mVoltage; // the emitted packet size (8 EU)
	}

	// ---------------------------------------------------------------------------
	// the platform FE capability face (the GT6FeBatteryBlockEntity two-seam model)
	// ---------------------------------------------------------------------------

	/**
	 * The intake wrapper — the platform {@code IEnergyStorage} reference shape (the
	 * javap-verified six-method twins on both legs). canExtract is FALSE (the GTCEu
	 * ConverterTrait :141-143 verbatim semantics): the intake is never a drain face —
	 * pull-style FE networks must not steal the buffer; the machine drains its own buffer
	 * by emitting EU. The class lives in the per-leg hunks because the interface package
	 * is the one split the swap table does not carry (net.minecraftforge.energy vs
	 * net.neoforged.neoforge.energy — the GT6FeBatteryBlockEntity fork record).
	 */
	//? if forge {
	private final FeIntake mFeStorage = new FeIntake();
	private final LazyOptional<IEnergyStorage> mFeCap = LazyOptional.of(() -> mFeStorage);

	private final class FeIntake implements IEnergyStorage {
		@Override public int receiveEnergy(int aAmount, boolean aSimulate) {return pushFe(aAmount, aSimulate);}
		@Override public int extractEnergy(int aAmount, boolean aSimulate) {return 0;} // GTCEu :124-126
		@Override public boolean canExtract() {return false;} // GTCEu :141-143
		@Override public boolean canReceive() {return true;}
		@Override public int getEnergyStored() {return (int)Math.min(Integer.MAX_VALUE, mBufferFe);}
		@Override public int getMaxEnergyStored() {return (int)Math.min(Integer.MAX_VALUE, capacityFe());}
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		// the FE intake serves on ALL sides (the declared simplification); the ITEM_HANDLER
		// arm stays the Root's own (super carries it)
		if (aCapability == ForgeCapabilities.ENERGY) {
			return mFeCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mFeCap.invalidate();
	}

	/** Raw capability handle (package-private test seam — ForgeCapabilities cannot class-init offline). */
	LazyOptional<IEnergyStorage> feCapability() {
		return mFeCap;
	}
	//?} else {
	/*private final FeIntake mFeStorage = new FeIntake();

	private final class FeIntake implements IEnergyStorage {
		@Override public int receiveEnergy(int aAmount, boolean aSimulate) {return pushFe(aAmount, aSimulate);}
		@Override public int extractEnergy(int aAmount, boolean aSimulate) {return 0;} // GTCEu :124-126
		@Override public boolean canExtract() {return false;} // GTCEu :141-143
		@Override public boolean canReceive() {return true;}
		@Override public int getEnergyStored() {return (int)Math.min(Integer.MAX_VALUE, mBufferFe);}
		@Override public int getMaxEnergyStored() {return (int)Math.min(Integer.MAX_VALUE, capacityFe());}
	}

	// 21.1: no getCapability to override — the intake face rides the
	// GT6CapabilityWiring.registerFeConverters row (registerBlockEntity over the BET, the
	// GT6FeBatteryBlockEntity two-seam model), reading this wrapper.
	public IEnergyStorage energyStorage() {
		return mFeStorage;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (the buffered FE). The tree convention (the W4 record, Root :110-155): the
	// keys ride the PLAIN load(CompoundTag)/saveAdditional(CompoundTag) pair — on 21.1
	// the Root provider hooks delegate into exactly these plain members, so one form
	// serves both legs with no hunk.
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FE, Tag.TAG_ANY_NUMERIC)) {
			mBufferFe = Math.max(0, aNBT.getLong(NBT_FE));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_FE, mBufferFe);
	}
}
