package gregtech6.tileentity.energy;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GTCrankBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of the GT6 Hand Crank — task p12-engine-crank, ported from
 * gregtech/tileentity/tools/MultiTileEntityCrank.java (:51-198, Loader_MultiTileEntities.java
 * :2106 meta 32111 ANY.Iron single variant) as the first REAL RU source of the fluid-engine
 * chain: cranking pushes constant-sign (negative = counterclockwise) RU direct current —
 * no fuel, no fluid, no GUI (the upstream {@code NO_GUI_CLICK_TO_INTERACT} tooltip :68).
 *
 * <p>Packet math (upstream :78 player arm, the :82-91 villager arm is CROPPED per the task
 * card): {@code size = -divup(8 * pot2Strength, pot1Weakness)}, {@code amount = pot1Haste}
 * — the potion scalers of UT.Entities.pot1/pot2 (UT.java:3087-3089: pot = amplifier bind6,
 * pot1 = pot+2 = base 1, pot2 = pot+3 = base 2), so the potionless player pushes
 * {@code -16 x 1} per tick (matching the constant :194-197 output band 16/16/16 exactly —
 * a Weakness-tier player pushes below the Shredder T1 input minimum 16 and the Root
 * EnergyGate swallows the packets; upstream-faithful, no extra gate added).
 *
 * <p>Emit shape: the activation window lives in the TICK, not the face (the
 * GeneratorSolid :102-104 family pattern, also the p8-d4 rig form). Upstream
 * {@code onTick2} (:72-100) clears {@code mActive} at tick head and re-arms it from the
 * {@code UT.Entities.getPlayersWithLastTarget} registry — the still-holding-right-click
 * players. This port collapses that registry to the arming click itself
 * ({@link GTCrankBlock#use} → {@link #onPlayerCrank}, each use re-arms exactly one
 * window; vanilla's hold-right-click use repetition sustains it — DECLARED deviation,
 * the per-player multi-crank stacking of :76-80 and the :78-79 exhaust/swing side effects
 * ride the dropped targeting registry). The {@code /gt6engine crank <pos> <ticks>} window
 * ({@link #setDriveTicks}) is the second arming channel — the RCON acceptance
 * counterfactual of "a potionless player keeps cranking", no upstream line.
 *
 * <p>The emit itself is the upstream :78 call re-expressed through the p8-d4 adjacency
 * seam: {@code Util.emitEnergyToNetwork(TD.Energy.RU, size, amount, this, adjacency())}
 * — the {@link #isEnergyEmittingTo} face gate (:193 verbatim: only {@code mFacing})
 * collapses the six-side loop to the upstream single-side {@code emitEnergyToSide(mFacing)}
 * call, and the packet LEAVES on mFacing (the machine sits on the facing side; the handle
 * side the player cranks from is the placement-opposite side).
 *
 * <p>Face family (upstream :192-198 verbatim; everything else rides the 01Root defaults,
 * exactly the upstream override set): accepting = the Root default consults
 * {@link #isEnergyType} with {@code aEmitting=false} → false (a pure source);
 * doEnergyInjection → the Root/EnergyGate refuse on it → 0. The :193 gate is
 * aTheoretical-insensitive like upstream — the static capability probe (the wire
 * conductor's EnergyCompat :102 theoretical probe, the p8-d4 ruling 1 concern) sees the
 * face permanently, the activation gate stays out of the face.
 *
 * <p>Facing: the BlockState {@code FACING} is the display/command authority (written at
 * placement from the player's look direction — the oven/GTBasicMachineBlock
 * {@code getStateForPlacement} precedent, and the {@code /setblock gt6:crank[facing=...]}
 * RCON path), {@link #mFacing} is the BE runtime mirror: re-synced from the state at each
 * server tick head ({@link #syncFacingFromState}), written alongside by
 * {@link #setFacingFromPlacement}. Not persisted (the state persists it — no dual
 * authority); the plain field stays for the offline tests.
 *
 * <p>Cropped with the targeting registry: the redstone emission
 * (:114-122 {@code isProvidingWeakPower2/StrongPower2} — mActive on the opposite side)
 * and the :97-99 client ambience sound; both ride the redstone-hooks / audio pool cards.
 */
public class GTCrankBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {


	/** The upstream NBT key (CS NBT_ACTIVE, :57/:63). */
	public static final String NBT_ACTIVE = "active";
	/** The rig keys (the /gt6engine acceptance channel state; no upstream line). */
	public static final String NBT_DRIVE_TICKS = "driveticks";
	public static final String NBT_PACKET_SIZE = "packetsize";
	public static final String NBT_PACKET_AMOUNT = "packetamount";

	/** The potionless-player scalers (pot2Strength/pot1Weakness/pot1Haste at pot == -1). */
	public static final long DEFAULT_POT2_STRENGTH = 2;
	public static final long DEFAULT_POT1_WEAKNESS = 1;
	public static final long DEFAULT_POT1_HASTE = 1;

	/** The constant output band (upstream :195-197 — also the potionless packet magnitude). */
	public static final long OUTPUT_SIZE = 16;

	/**
	 * The use-armed window (upstream :52 mActive): set by {@link #onPlayerCrank}, consumed
	 * (cleared + emitted once) by the next server tick — the upstream :74-75 form.
	 */
	public boolean mActive = false;

	/**
	 * The /gt6engine drive window (the RCON acceptance channel): {@code > 0} = the tick
	 * pushes one packet and decrements, no upstream line. Persisted so a restart
	 * mid-chain cannot silently stop the drive.
	 */
	public int mDriveTicks = 0;

	/** The pending packet (upstream :78 forms, armed per use / per drive arming). */
	public long mPacketSize = -16;
	public long mPacketAmount = 1;

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue). */
	public byte mFacing = 2; // NORTH

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTCrankBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET.
	 */
	public GTCrankBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.CRANK_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "crank"; // BET registry path mirrors it (GTBlockEntities.CRANK_BE)
	}

	// ---------------------------------------------------------------------------
	// the tick emit (upstream onTick2 :72-100, trimmed per the class doc)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // upstream :73 server branch (the :98 client sound is cropped)
		syncFacingFromState();
		boolean oActive = mActive; // upstream :74
		mActive = false; // upstream :75
		if (mDriveTicks > 0) { // the rig window (no upstream line — the /gt6engine acceptance channel)
			mDriveTicks--;
			emit();
		} else if (oActive) { // the player-use arm (upstream :76-80, the per-player loop collapsed)
			emit();
		}
		if (mActive != oActive) updateClientData(); // upstream :93-94 (causeBlockUpdate rides the redstone pool)
	}

	/**
	 * The per-tick emit, split out so the offline tests can drive it directly (the live
	 * entry is {@link #onTick}). The packet size arrives NEGATIVE (counterclockwise) —
	 * the invariant the task card pins; consumers re-derive the direction semantics.
	 */
	void emit() {
		// the :78 call — the face gate (:193) collapses Util's six-side loop to the
		// single mFacing side, the upstream emitEnergyToSide(TD.Energy.RU, mFacing, ...) form
		ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.RU, mPacketSize, mPacketAmount, this, adjacency());
	}

	/**
	 * The D1 adjacency seam, live-resolved per packet like the p8-d4 rig
	 * (GTEnergySourceBlockEntity.adjacency): the neighbour BE plus the side of it that
	 * faces us. {@code mAdjacencyOverride} is the offline test seam.
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

	// ---------------------------------------------------------------------------
	// the face family (upstream :192-198 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == TD.Energy.RU; // upstream :192
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aSide == mFacing && aEnergyType == TD.Energy.RU; // upstream :193 (aTheoretical-insensitive like upstream)
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		return (mActive || mDriveTicks > 0) ? OUTPUT_SIZE : 0; // upstream :194 mActive ? 16 : 0, + the rig window
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return OUTPUT_SIZE;} // upstream :195

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return OUTPUT_SIZE;} // upstream :196

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return OUTPUT_SIZE;} // upstream :197

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.RU.AS_LIST; // upstream :198
	}

	// ---------------------------------------------------------------------------
	// the arming channels (upstream onBlockActivated3 :103-112 + the rig window)
	// ---------------------------------------------------------------------------

	/**
	 * The player right-click arm (upstream onBlockActivated3 :104-108 server branch):
	 * sets the one-tick window and the player-scaled packet (:78). The :108 sound and the
	 * :78-79 exhaust/swing are cropped with the targeting registry (class doc).
	 */
	public void onPlayerCrank(Player aPlayer) {
		mActive = true; // upstream :105
		mPacketSize = packetSize(pot2Strength(aPlayer), pot1Weakness(aPlayer)); // upstream :78 size form
		mPacketAmount = pot1Haste(aPlayer); // upstream :78 amount form
		setChanged();
		updateClientData(); // upstream :106
	}

	/**
	 * The RCON drive window (the acceptance channel, no upstream line): {@code ticks} of
	 * potionless-player cranking; 0 stops a running window. The packet params are pinned
	 * to the :78 potionless defaults so the command is the deterministic counterfactual
	 * of "a plain player keeps cranking".
	 */
	public void setDriveTicks(int aTicks) {
		mDriveTicks = Math.max(0, aTicks);
		if (mDriveTicks > 0) {
			mPacketSize = packetSize(DEFAULT_POT2_STRENGTH, DEFAULT_POT1_WEAKNESS);
			mPacketAmount = DEFAULT_POT1_HASTE;
		}
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// the packet math (upstream :78 verbatim forms + the UT.Entities.pot1/pot2 scalers)
	// ---------------------------------------------------------------------------

	/** The :78 size form — NEGATIVE by construction (counterclockwise = the传动 direction semantics the axle/gearbox cards consume). */
	public static long packetSize(long aPot2Strength, long aPot1Weakness) {
		return -UT6.divup(8L * aPot2Strength, aPot1Weakness);
	}

	/** The :78 amount form — the haste scaler verbatim. */
	public static long packetAmount(long aPot1Haste) {
		return aPot1Haste;
	}

	/** UT.Entities.pot2Strength (UT.java:3116 → pot2 = pot+3, base 2; the amplifier bind6-limited). */
	public static long pot2Strength(Player aPlayer) {
		return pot(aPlayer, MobEffects.DAMAGE_BOOST) + 3;
	}

	/** UT.Entities.pot1Weakness (UT.java:3109 → pot1 = pot+2, base 1). */
	public static long pot1Weakness(Player aPlayer) {
		return pot(aPlayer, MobEffects.WEAKNESS) + 2;
	}

	/** UT.Entities.pot1Haste (UT.java:3110 → pot1 = pot+2, base 1). */
	public static long pot1Haste(Player aPlayer) {
		return pot(aPlayer, MobEffects.DIG_SPEED) + 2;
	}

	/** UT.Entities.pot (UT.java:3079-3086) — -1 without the effect, the amplifier bind6-clamped otherwise. */
	private static int pot(Player aPlayer, MobEffect aEffect) {
		if (!aPlayer.hasEffect(aEffect)) return -1;
		return Math.min(63, Math.max(0, aPlayer.getEffect(aEffect).getAmplifier()));
	}

	// ---------------------------------------------------------------------------
	// facing (the BlockState is the command-side authority, the field the runtime mirror)
	// ---------------------------------------------------------------------------

	/**
	 * Chest/oven precedent onPlaced :128-131 (TileEntityOven.setFacingFromPlacement :632-635
	 * form) — the BE mirror of the placement-facing the BlockState already carries.
	 */
	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = (byte)aPlayer.getDirection().get3DDataValue();
		setChanged();
	}

	/**
	 * The /setblock RCON path: the state string carries the facing and the BE is created
	 * with the default mirror — re-sync from the state before any emit decides a side.
	 * Keyed on the PROPERTY (not the block class) so the offline test can drive it with a
	 * vanilla state carrying {@link BlockStateProperties#HORIZONTAL_FACING}; a state
	 * without the property (the STONE test fixture) leaves the mirror alone.
	 */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GTCrankBlock.FACING)) {
			mFacing = (byte)getBlockState().getValue(GTCrankBlock.FACING).get3DDataValue();
		}
	}

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// NBT (the upstream mActive key :57/:63 + the rig window keys)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_ACTIVE, mActive);
		aNBT.putInt(NBT_DRIVE_TICKS, mDriveTicks);
		aNBT.putLong(NBT_PACKET_SIZE, mPacketSize);
		aNBT.putLong(NBT_PACKET_AMOUNT, mPacketAmount);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE);
		if (aNBT.contains(NBT_DRIVE_TICKS, Tag.TAG_ANY_NUMERIC)) mDriveTicks = aNBT.getInt(NBT_DRIVE_TICKS);
		if (aNBT.contains(NBT_PACKET_SIZE, Tag.TAG_ANY_NUMERIC)) mPacketSize = aNBT.getLong(NBT_PACKET_SIZE);
		if (aNBT.contains(NBT_PACKET_AMOUNT, Tag.TAG_ANY_NUMERIC)) mPacketAmount = aNBT.getLong(NBT_PACKET_AMOUNT);
	}
}
