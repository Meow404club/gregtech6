package gregtech6.tileentity.energy;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the simplest GT6 generator shape — task p8-d4-energy-source,
 * ADR 2026-08-31-p7-energy-network ruling 4. Ported from
 * gregtech/tileentity/energy/generators/MultiTileEntitySolarPanelElectric.java
 * (:82-86/:125-167) as a command-driven test generator: no fuel, no recipe, no fluid,
 * no sky check — the RCON command (/gt6energy) flips {@link #mEmitting} and the server
 * tick pushes EU into whatever neighbours accept it. The real fuel-bound generator
 * family is a later card (the ADR "true generators bind the fuel system elsewhere"
 * split).
 *
 * <p>Emit shape — the upstream evidence line (:160-162) is a single
 * {@code ITileEntityEnergy.Util.emitEnergyToNetwork(...)} call inside the server tick.
 * Upstream picks size/amount per the {@code TD.Energy.ALL_SIZE_IRRELEVANT} branch pair
 * (size=1 x amount=mEnergy, or size=mEnergy x amount=1); this rig pins the single
 * size=mVoltage x amount=mAmperage mode — the DECLARED test-rig simplification of the
 * task card. The family gating form is GeneratorSolid :102-104 ({@code if (mBurning) ...
 * emit} — the mode gate lives in the tick, not the face); here {@code if (mEmitting)}
 * takes exactly that spot.
 *
 * <p>Task p11-rotor-source-flip extensions (the rotor-family source regime): the emitted
 * TYPE is parameterised ({@link #mEnergyType}, default EU — the p8-d4 card form) and an
 * ALTERNATING mode ({@link #mAlternating}) reproduces the upstream Steam Engine :146
 * piston-phase ±alternation verbatim: a 2-bit phase counter (the :113-114
 * {@code mPiston += 1; mPiston &= 3;} form, ticked once per emit) signs the packet
 * {@code mPiston > 1 ? -mVoltage : mVoltage} — the +/+/-/- square wave whose
 * positive→non-positive transitions drive the machine :815 alternating arm (KU delivers
 * only on the zero-crossing tick). The phase byte is NOT persisted (the rig re-derives a
 * square-wave phase from zero on reload — a declared test-rig simplification; the upstream
 * engine does persist mPiston). EU machines (the oven) ignore RU/KU packets through their
 * :501 type gate; RU/KU machines (Shredder/Crusher/Lathe) accept ONLY the matching type,
 * so the rig type must be set to the machine's carrier.
 *
 * <p>Emitted-type face family (upstream :104-110, the type now a field):
 * <ul>
 * <li>{@link #isEnergyType} = {@code aEmitting && type == mEnergyType} (upstream :104
 *     verbatim with the emitted-type field read);</li>
 * <li>{@link #isEnergyEmittingTo} = the upstream Root default shape
 *     (TileEntityBase01Root.java:714 — aTheoretical-insensitive, all six sides here,
 *     the {@code getSurfaceSizeAttachable} term has no cover layer in this port) with
 *     the CURRENT mode required only on the non-theoretical probe:
 *     {@code isEnergyType(T) && (aTheoretical || mEmitting)}. On the real emit path
 *     (Util :246 probes aTheoretical=false) this is exactly the card formula — the
 *     mode gate rides the face too; on the theoretical conductor probe
 *     (EnergyCompat.canConnectElectricity :102) the face reports the static
 *     capability, the interface's stated purpose of aTheoretical ("so that the
 *     Conductor doesn't visually toggle on/off") and the reason the wire's
 *     {@code canConnect} emitting branch can reach this BE at all;</li>
 * <li>{@link #isEnergyAcceptingFrom} = false — a pure source (upstream :104: the
 *     accepting probe asks {@code aEmitting=false}, which is constant false here);</li>
 * <li>no injection, no extraction, zero demand/offer (the Root defaults, overridden
 *     per the card); size band [0, mVoltage] on both directions, recommended=max=mVoltage
 *     (upstream :108-109 carry mOutput; the card flattens the /8 min to 0).</li>
 * </ul>
 *
 * <p>Persistence: the card's three plain keys "emitting"/"voltage"/"amperage" plus the
 * p11 keys "energytype"/"alternating" ride {@code saveAdditional}/{@code load} (the
 * te_name key is the 01Root base).
 */
public class GTEnergySourceBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The card's NBT keys. */
	public static final String NBT_EMITTING = "emitting";
	public static final String NBT_VOLTAGE = "voltage";
	public static final String NBT_AMPERAGE = "amperage";
	/** The p11 keys: the emitted energy type (persisted as the TagData mName) and the ±alternating mode. */
	public static final String NBT_ENERGY_TYPE = "energytype";
	public static final String NBT_ALTERNATING = "alternating";

	/** The packet size in EU pushed per packet (the upstream NBT_OUTPUT carrier, :58). */
	public long mVoltage = 32;

	/** The packet count pushed per tick (the upstream mEnergy packet stock, single-mode form). */
	public long mAmperage = 1;

	/** The RCON-driven mode gate — the mStopped/mBurning slot of the upstream family (:84/:102). */
	public boolean mEmitting = false;

	/**
	 * The emitted energy type (p11: parameterised off the p8-d4 EU pin). The :501 machine
	 * gate is reference-equality, so this MUST be a shared TD.Energy constant — set only
	 * through {@link #setEnergyType} / {@link #resolveEnergyType} (the registered-instance
	 * lookup).
	 */
	public TagData mEnergyType = TD.Energy.EU;

	/**
	 * The ±alternating mode (p11): on = the emit signs the packet by the 2-bit piston phase
	 * (upstream EngineSteam :146 {@code mPiston > 1 ? -tOutput : tOutput}) — the square wave
	 * whose zero-crossings drive the machine :815 alternating arm. Off (default, the p8-d4
	 * form) = every packet positive.
	 */
	public boolean mAlternating = false;

	/**
	 * The 2-bit piston phase (upstream EngineSteam :113-114 {@code mPiston += 1; mPiston &= 3;}
	 * shape) — advanced once per alternating emit, so the sign sequence is +/+/-/- (+ = the
	 * phase pair 0-1, - = 2-3). NOT persisted: the rig re-derives the phase from zero on
	 * reload (declared test-rig simplification).
	 */
	public byte mPiston = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTEnergySourceBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET.
	 */
	public GTEnergySourceBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.ENERGY_SOURCE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "energy_source"; // BET registry path mirrors it (GTBlockEntities.ENERGY_SOURCE_BE)
	}

	// ---------------------------------------------------------------------------
	// the tick emit (upstream onTick2 :82-86 -> generateEnergy :157-162, trimmed to the rig)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// upstream :83-84 (server branch) + :157/:162 — the single Util call of the evidence
		// line, size=mVoltage x amount=mAmperage (the declared single-mode simplification);
		// p11: the alternating mode signs the size by the 2-bit piston phase (the upstream
		// EngineSteam :113-114 advance + :146 sign form verbatim)
		if (aIsServerSide && mEmitting) {
			long tSize = mVoltage;
			if (mAlternating) {
				mPiston += 1; mPiston &= 3; // upstream :113-114 (the 2-bit phase)
				tSize = mPiston > 1 ? -mVoltage : mVoltage; // upstream :146 sign form
			}
			ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyType, tSize, mAmperage, this, adjacency());
		}
	}

	/**
	 * The D1 adjacency seam, live-resolved per packet like the wire relay
	 * (GTWireBlockEntity.transferElectricity :187): the neighbour BE plus the side of it
	 * that faces us (the upstream DelegatorTileEntity pair in pure-data form). Null for
	 * unloaded/absent neighbours — Util.emitEnergyToSide returns 0 for those.
	 */
	private IEnergyAdjacency adjacency() {
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// ---------------------------------------------------------------------------
	// emitted-type face family (upstream :104-110, the type a p11 field)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == mEnergyType; // upstream :104, the emitted-type field
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyType.AS_LIST; // upstream :110
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// all six sides (no facing — the simple-cube rig); see the class doc for the
		// aTheoretical split vs the card formula
		return isEnergyType(aEnergyType, aSide, true) && (aTheoretical || mEmitting);
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return false; // pure source
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		return 0;
	}

	@Override
	public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :109

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :108

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mVoltage;}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return mVoltage;}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;} // upstream :107 carries mOutput/8 — flattened per the card

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

	// ---------------------------------------------------------------------------
	// NBT (the card's three plain keys + the p11 energytype/alternating pair)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_EMITTING, mEmitting);
		aNBT.putLong(NBT_VOLTAGE, mVoltage);
		aNBT.putLong(NBT_AMPERAGE, mAmperage);
		aNBT.putString(NBT_ENERGY_TYPE, mEnergyType.mName);
		aNBT.putBoolean(NBT_ALTERNATING, mAlternating);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_EMITTING, Tag.TAG_ANY_NUMERIC)) mEmitting = aNBT.getBoolean(NBT_EMITTING);
		if (aNBT.contains(NBT_VOLTAGE, Tag.TAG_ANY_NUMERIC)) mVoltage = aNBT.getLong(NBT_VOLTAGE);
		if (aNBT.contains(NBT_AMPERAGE, Tag.TAG_ANY_NUMERIC)) mAmperage = aNBT.getLong(NBT_AMPERAGE);
		if (aNBT.contains(NBT_ENERGY_TYPE, Tag.TAG_STRING)) {
			TagData tParsed = resolveEnergyType(aNBT.getString(NBT_ENERGY_TYPE));
			if (tParsed != null) mEnergyType = tParsed; // an unknown name keeps the current type (no rogue mint)
		}
		if (aNBT.contains(NBT_ALTERNATING, Tag.TAG_ANY_NUMERIC)) mAlternating = aNBT.getBoolean(NBT_ALTERNATING);
	}

	/**
	 * The name → shared-instance lookup (the :501 machine gate is reference equality).
	 * Deliberately NOT {@link TagData#createTagData(String)}: that call MINTS AND REGISTERS
	 * a new TagData for an unknown name (TagData.java:77-80), so a typo would silently
	 * create a type nobody accepts — here an unknown name returns null and the caller
	 * (the /gt6energy type command) reports the failure; the NBT load keeps the current type.
	 */
	public static TagData resolveEnergyType(String aName) {
		for (TagData tTag : TagData.TAGS) if (tTag.mName.equalsIgnoreCase(aName)) return tTag;
		return null;
	}

	/** The mode setters arm persistence (the wire/oven command set form). */
	public void setEmitting(boolean aEmitting) {
		mEmitting = aEmitting;
		setChanged();
	}

	public void setVoltage(long aVoltage) {
		mVoltage = aVoltage;
		setChanged();
	}

	public void setAmperage(long aAmperage) {
		mAmperage = aAmperage;
		setChanged();
	}

	public void setEnergyType(TagData aEnergyType) {
		mEnergyType = aEnergyType;
		setChanged();
	}

	public void setAlternating(boolean aAlternating) {
		mAlternating = aAlternating;
		setChanged();
	}
}
