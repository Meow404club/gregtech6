package gregtech6.tileentity.connectors;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.covers.CoverData;
import gregtech6.covers.ICoverableTE;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.machines.ITileEntitySwitchableMode;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of gregapi/tileentity/connectors/MultiTileEntityWireElectric.java
 * (249 lines) — the trimmed direct translation (task p7-d2-cable spec ①/②, ADR
 * 2026-08-31-p7-energy-network ruling 2). The upstream class extended
 * TileEntityBase10ConnectorRendered; this port mounts the same
 * {@link TileEntityBase09Connector} base the fluid pipe already uses (read-only reuse,
 * zero base-class changes — the "no seam extraction" ruling), and implements the D1
 * {@link ITileEntityEnergy} root interface directly.
 *
 * <p>Transport semantics kept verbatim — there is NO network object, every wire segment
 * is its own relay that pushes packets to its neighbours on demand (upstream
 * transferElectricity :170-189):
 * <ul>
 * <li>{@link #transferElectricity} — the per-segment pump: a packet smaller than the
 *     loss {@code mLoss} dies (:171), the loss is subtracted respecting the sign of the
 *     voltage (:172), the six sides except the packet's own side are cycled behind the
 *     {@link #canEmitEnergyTo} gate (:175), the remaining amperage breaks the loop
 *     (:176), a {@link HashSetNoNulls} seeded with {@code this} prevents recursion loops
 *     (:178), wire neighbours recurse (:179-182) and everything else goes through
 *     {@link ITileEntityEnergy.Util#insertEnergyInto} (:184 — the root Util with the
 *     neighbour BE and its back-side, the DelegatorTileEntity pair in pure-data form);
 *     the return value books the transfer through {@link #addToEnergyTransferred} only
 *     when amperes actually flowed (:188 — with no consumer NOTHING is recorded, not
 *     even burn);</li>
 * <li>{@link #addToEnergyTransferred} (:191-199) — the bookkeeping and the overload
 *     detector: over-voltage (|v| &gt; mVoltage) or over-current (accumulated amperes
 *     &gt; mAmperage) strike the {@link #mBurnCounter} (capped at 16, :195) and report
 *     the packet as fully consumed;</li>
 * <li>{@link #onTick} server side (:145-168 trimmed) — 16 strikes burn the wire off
 *     ({@link #setToFire}), one strike heals every 512 ticks (:152), the transferred
 *     wattage lags into {@link #mWattageLast} and the per-tick counters clear
 *     (:153-155); the IC2 IEnergySource pull branch (:156-165) is cut with EnergyCompat
 *     (ADR ruling 5, pool);</li>
 * <li>the EU face family (:205-230) — EU only, emitting/accepting strictly behind the
 *     connection mask ({@link #canEmitEnergyTo}/{@link #canAcceptEnergyFrom} = the
 *     upstream {@code connected} passthrough :229-230), the size band [0, mVoltage] on
 *     both directions (:212-217), no extraction (:210) and zero demand/offer (the Root
 *     defaults, overridden per the card);</li>
 * <li>connection — the base handshake with {@link TD.Connectors#WIRE_ELECTRIC} types
 *     (:243); non-connector neighbours connect when they are
 *     {@link ITileEntityEnergy} acceptors (the upstream
 *     {@code EnergyCompat.canConnectElectricity} :201 form, the aTheoretical=true
 *     conductor probe);</li>
 * <li>{@link #onPlaced} — the upstream TileEntityBase09Connector.onPlaced (:82-96):
 *     the clicked support face connects (the OPOS flip :84) plus the symmetric
 *     back-connect loop over neighbours that already face this wire (:90-94). Duplicated
 *     from the pipe BE verbatim: the base file is frozen (zero-seam ruling) and 09Connector
 *     owns no onPlaced in this port.</li>
 * </ul>
 *
 * <p>Cuts and placeholders after task p9-wire-family-w1: the material spectrum itself now
 * EXISTS (620 GTWireSpecs variants share THIS ONE BE class over the block carrier — the
 * upstream "one TE class, many material rows" shape), but the material/insulation conductor
 * data (ITileEntityEnergyDataConductor), the texture/render passes,
 * the tooltips and the electrometer tool click, and the IC2 pull remain pool items. The two
 * standing placeholders are declared at their exact positions below: the IC2-pull cut on
 * {@link #onTick} (with the reserved external-energy-bridge seam) and the bundled-channel
 * {@code aChannel} parameter on {@link #transferElectricity}. The contact damage JOINED in
 * task p10-wire-contact-damage ({@link #applyElectricityDamage}, the :203 hook over the
 * block-side entityInside). Persistence: the upstream writeToNBT2 (:121-123) is an empty
 * body — the ratings travel on the block carrier ({@link GTWireBlock}, the GTBarrelBlock
 * registration-carrier precedent), so this BE adds NO NBT beyond the base
 * {@code mConnections} and the redstone family keys; {@link #mWattageLast} is TRANSIENT by
 * the same upstream body (fresh load = 0 = the wire cannot bite until power flows again —
 * the restart-safe property of the shock gate), the p7 card's NBT test pins that contract.
 *
 * <p>Covers + the selector dial (task p34-pool-cover-hosts): the BE implements
 * {@link ICoverableTE} by composition (the {@link GTFluidPipeBlockEntity} twin of the same
 * card) and {@link ITileEntitySwitchableMode} over {@link #mMode} (upstream
 * MultiTileEntityWireRedstoneInsulated :177/:178 passthrough) — the FIRST live host of
 * both faces, the landing the p34-covers-gameplay-10 cards declared. The torch/repeater
 * covers drive the visual lane from {@link #mRedstone} in their tickPost arm, the four
 * selector covers drive {@link #setStateMode}; {@link #getRedstoneOut} routes the vanilla
 * emission bridge through the cover exits (upstream 04Covers :427-438 — the plate on the
 * emission face answers before the wire's own emission). Admission stays the interface
 * default: the cover-side gates ({@code AbstractCoverAttachmentTorch} redstone-FAMILY
 * check — since p35 the torch gate narrows to {@link #isRedstone()} rows, the selector
 * ITileEntitySwitchableMode check) are the delivered admission policy and the host adds
 * no narrowing of its own.
 */
public class GTWireBlockEntity extends TileEntityBase09Connector implements ITileEntityEnergy, GTWireRedstoneNode, ICoverableTE, ITileEntitySwitchableMode {

	/**
	 * Upstream :64 — transfer bookkeeping and the wire rating defaults: 32 EU packets,
	 * 1 A of amperage, 1 EU loss per segment. The constructor overwrites the three
	 * ratings from the block carrier (GTWireBlock, the vanilla-block fallbacks are exactly
	 * these upstream defaults). The redstone family reuses {@code mLoss} for its
	 * NBT_PIPELOSS (upstream MultiTileEntityWireRedstoneInsulated :55 declares the same
	 * field name on its own class); the EU ratings stay 0 on redstone rows and the whole
	 * EU face family is gated off there (see {@link #isEnergyType}).
	 */
	public long mTransferredAmperes = 0, mTransferredWattage = 0, mWattageLast = 0, mLoss = 1, mAmperage = 1, mVoltage = 32;

	/** Upstream :65 — the overload strike counter; 16 strikes burn the wire (mRenderType is a render cut). */
	public byte mBurnCounter = 0;

	// task p32-qu-laser-domain — the laser family transfer counters (upstream
	// MultiTileEntityWireLaser :49, mTransferred/mTransferredLast; TRANSIENT like the
	// electric family's wattage window — upstream writes no NBT on the laser rows)

	/** Upstream :49 — the LU mass booked during the CURRENT tick window (|frequency × used| per hop). */
	public long mTransferred = 0;
	/** Upstream :49/:61 — the previous window's booking, lagged by the laser tick arm. */
	public long mTransferredLast = 0;

	// ---------------------------------------------------------------------------
	// the redstone family state (task p10 — upstream MultiTileEntityWireRedstoneInsulated :53-57)
	// ---------------------------------------------------------------------------

	/** The single signal channel this port mounts (upstream :53 REDSTONE_ID = -1; bundled channels are a pool item). */
	public static final int REDSTONE_ID = -1;

	/** Upstream CS.java:522 SIDE_UNDEFINED = 6 — the "no input side" sentinel of {@link #mReceived}. */
	public static final byte SIDE_UNDEFINED = 6;

	/**
	 * Upstream :55 — the accumulated signal value, FULL-RANGE 0..{@code MAX_RANGE}
	 * (2^31-1, ITileEntityRedstoneWire :32 — NOT the vanilla 0..15). Also the vanilla
	 * emission source: {@code bind4(divup(mRedstone, MAX_RANGE))} (:144).
	 */
	public long mRedstone = 0;

	/**
	 * Upstream MultiTileEntityWireRedstone :36 — the visual/emission byte,
	 * {@code bind4(divup(mRedstone, MAX_RANGE))} 0..15 (:53). THE LIGHT VALUE (task
	 * p11-wire-brightness: {@code GTWireBlock.getLightEmission} reads it through
	 * {@link #mRedstone}) and the texture-fullbright data pin upstream (:81-82, the render
	 * side is a declared deviation). Server side it refreshes in {@link #onTickCheck}
	 * (:51-58 — the dispatcher consumes a {@code true} return as the visual-sync trigger);
	 * client side it refreshes in {@link #load} — the {@code setVisualData} :62-67 landing
	 * over the vanilla two-channel sync.
	 *
	 * <p>DEVIATION: upstream persists it (NBT_STATE :41/:47) because the upstream
	 * {@code gt.mredstone} reload truncates to a byte — this port round-trips the full
	 * long (the declared p10 load deviation), so {@code mState} stays transient and is
	 * re-derived from {@link #mRedstone} on every load/tick.
	 */
	public byte mState = 0;

	/**
	 * Upstream :56 — the side the strongest input came from (GT6 side order, or
	 * {@link #SIDE_UNDEFINED}); updateRedstone :126-130 keeps it and updateRedstone's
	 * own baseline excludes it from the rescan (ALL_SIDES_VALID_BUT[oReceived]).
	 */
	public byte mReceived = SIDE_UNDEFINED;

    /**
     * Upstream :56 — the constant-strength source mode: updateRedstone :124 baselines the
     * wire at {@code mMode * MAX_RANGE - mLoss}. Switched through {@link #setStateMode} —
     * upstream only by the CoverSelectorRedstone cover (CoverSelectorRedstone.java:42-51),
     * this port by the four selector covers (task p34-covers-gameplay-10) whose FIRST live
     * host this BE now is (task p34-pool-cover-hosts, upstream :177/:178 passthrough).
     */
    public byte mMode = 0;

    // ---------------------------------------------------------------------------
    // covers + the selector dial (task p34-pool-cover-hosts — the composition
    // attachment, the GTFluidPipeBlockEntity twin: the store lives here, the 06Covers
    // behaviour comes from the ICoverableTE defaults; the base-class chain stays untouched)
    // ---------------------------------------------------------------------------

    /** Upstream 06Covers :63 mCovers — {@code null} while no face carries a cover. */
    @Nullable
    public CoverData mCovers = null;

    @Override
    public CoverData getCovers() {
        return mCovers;
    }

    @Override
    public void setCovers(@Nullable CoverData aCoverData) {
        mCovers = aCoverData;
    }

    /** Upstream MultiTileEntityWireRedstoneInsulated :177 verbatim — the dial write, the return rides the visual mirror. */
    @Override
    public byte setStateMode(byte aMode) {
        mMode = aMode;
        return mMode;
    }

    /** Upstream :178 verbatim. */
    @Override
    public byte getStateMode() {
        return mMode;
    }

	/**
	 * Upstream :56 — the per-side vanilla input cache (byte[7] like ALL_SIDES); onTick2
	 * :102 resets all seven entries to -1 every server tick and getRedstoneAtSide :117
	 * refills lazily ({@code MAX_RANGE * level - mLoss} :118).
	 */
	public final byte[] mVanillaSides = {-1, -1, -1, -1, -1, -1, -1};

	/**
	 * Upstream :57 — whether any connected side faces a NON-wire neighbour (the
	 * causeBlockUpdate gate of updateRedstone :131); recomputed by
	 * {@link #updateConnectionStatus} on the onTickFirst2 :87 / onConnectionChange :93 /
	 * mBlockUpdated :103 triggers.
	 */
	public boolean mConnectedToNonWire = true;

	/** The NBT keys, upstream :62-64 verbatim ("gt.mreceived" / "gt.mredstone" / NBT_MODE). */
	public static final String NBT_MRECEIVED = "gt.mreceived", NBT_MREDSTONE = "gt.mredstone", NBT_MODE = "gt.mode";

	/**
	 * The family flag — a redstone-family row (GTWireSpecs.Family.REDSTONE) mounted on this
	 * shared BE class. The two families share the carrier class but never share semantics:
	 * the EU face family and the burn gate are dead on redstone rows (spec 7: the redstone
	 * registration has no CONTACTDAMAGE and no burning — MultiTileEntityWireRedstoneInsulated
	 * carries no ITileEntityEnergy at all upstream).
	 */
	protected final boolean mRedstoneFamily;

	/**
	 * The family flag — a laser-family row (GTWireSpecs.Family.LASER, task
	 * p10-wire-laser-placeholder) mounted on this shared BE class. INERT at the p10
	 * placeholder landing; since task p32-qu-laser-domain it carries the LIVE LU carrier
	 * (the p10 revival condition): the family answers LU on the energy face family
	 * ({@link #isEnergyType}, MultiTileEntityWireLaser :94), the doEnergyInjection LU arm
	 * floods {@link #transferLaser} (:66-86), and the canConnect LU probe (:89-92) is live.
	 * It still cannot burn (nothing strikes the overload counter on a lossless wire) and it
	 * cannot shock (GTWireBlock.contactDamageOf is family-gated false — NBT_CONTACTDAMAGE F
	 * on the Loader:1815 registration).
	 */
	protected final boolean mLaserFamily;

	/**
	 * Upstream CS.java:1366 REDSTONE_SINKS — vanilla blocks whose redstone output this wire
	 * REFUSES to accept (getRedstoneAtSide :114, the anti-feedback gate: droppers/dispensers
	 * re-emitting what we feed them). 1.20.1 mapping: Blocks.trapdoor/wooden_door are the
	 * modern OAK_DOOR/OAK_TRAPDOOR, golden_rail is POWERED_RAIL, and the 1.7.10
	 * redstone_lamp/lit_redstone_lamp PAIR is the single LIT-property REDSTONE_LAMP block
	 * (one entry covers both states).
	 */
	public static final HashSetNoNulls<Block> REDSTONE_SINKS = new HashSetNoNulls<>(false,
			Blocks.TNT, Blocks.POWERED_RAIL, Blocks.NOTE_BLOCK, Blocks.OAK_TRAPDOOR, Blocks.OAK_DOOR,
			Blocks.IRON_DOOR, Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.DISPENSER, Blocks.DROPPER, Blocks.REDSTONE_LAMP);

	/** Upstream CS.java:667 ALL_SIDES — the seven entries incl. SIDE_UNDEFINED (the mVanillaSides reset range, :102). */
	private static final byte[] ALL_SIDES = {0, 1, 2, 3, 4, 5, 6};

	/**
	 * Upstream CS.java ALL_SIDES_VALID_BUT — the six sides minus the packet's own side,
	 * in the GT6 side order. Carried locally like the D1 Util.ALL_SIDES_VALID (root CS
	 * increment policy). The seventh entry is CS.java:675's SIDE_UNDEFINED row (all six
	 * sides valid) — updateRedstone :130 indexes it with {@code oReceived} while
	 * {@link #mReceived} is still SIDE_UNDEFINED (first scan = all sides).
	 */
	private static final byte[][] ALL_SIDES_VALID_BUT = {
			{1, 2, 3, 4, 5}, // DOWN
			{0, 2, 3, 4, 5}, // UP
			{0, 1, 3, 4, 5}, // NORTH
			{0, 1, 2, 4, 5}, // SOUTH
			{0, 1, 2, 3, 5}, // WEST
			{0, 1, 2, 3, 4}, // EAST
			{0, 1, 2, 3, 4, 5}, // SIDE_UNDEFINED (CS.java:675, seventh row)
	};

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTWireBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET. The ratings come
	 * from the block carrier (the 1.20.1 counterpart of the registration NBT_PIPESIZE /
	 * NBT_PIPEBANDWIDTH / NBT_PIPELOSS, upstream :114-116) — vanilla blocks fall back to
	 * the upstream field defaults (:64). Since task p10 the carrier family also arms the
	 * redstone state (a REDSTONE-family {@link GTWireBlock} row mounts the push-BFS
	 * signal semantics; electric rows mount the EU pump, untouched).
	 *
	 * <p>THE FALLBACK IS FAMILY-AWARE (the RCON diagnostic round lesson): the
	 * {@code BlockEntityType.Builder.of(GTWireBlockEntity::new, ...)} method reference
	 * binds the (pos, state) constructor, so a redstone BET factory lands HERE with a null
	 * type — a hardcoded electric fallback would stamp every redstone BE with the
	 * WIRE_ELECTRIC_BE type, and the Level ticker gate (GTEntityBlock.getTicker,
	 * {@code aType != tickerType()}) would silently kill the whole tick chain (mVanillaSides
	 * stayed -1 forever, the RCON live proof). The fallback therefore resolves the BET by
	 * the carrier family; it runs at world-load time, long after both BETs registered.
	 */
	public GTWireBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : tickerTypeOf(aState), aPos, aState);
		mRedstoneFamily = aState.getBlock() instanceof GTWireBlock tWire && tWire.family() == GTWireSpecs.Row.Family.REDSTONE;
		mLaserFamily = aState.getBlock() instanceof GTWireBlock tWire && tWire.family() == GTWireSpecs.Row.Family.LASER;
		if (aState.getBlock() instanceof GTWireBlock tWire) {
			mVoltage = tWire.voltageL();
			mAmperage = tWire.amperageL();
			mLoss = tWire.lossL();
		}
	}

	/** The family-resolved default BET (the fallback of the full constructor, see its javadoc). */
	private static BlockEntityType<? extends GTWireBlockEntity> tickerTypeOf(BlockState aState) {
		if (aState.getBlock() instanceof GTWireBlock tWire) {
			if (tWire.family() == GTWireSpecs.Row.Family.REDSTONE) return gregtech6.registry.GTWires.WIRE_REDSTONE_BE.get();
			if (tWire.family() == GTWireSpecs.Row.Family.LASER) return gregtech6.registry.GTWires.WIRE_LASER_BE.get(); // task p10-wire-laser-placeholder
		}
		return GTBlockEntities.WIRE_ELECTRIC_BE.get();
	}

	/** The family gate for everything redstone on this shared class (task p10). */
	public boolean isRedstone() {
		return mRedstoneFamily;
	}

	/** The family gate for everything laser on this shared class (task p10-wire-laser-placeholder). */
	public boolean isLaser() {
		return mLaserFamily;
	}

	@Override
	public String getTileEntityName() {
		return isRedstone() ? "wire_redstone" : isLaser() ? "wire_laser" : "wire_electric"; // the BET registry path twins (GTWires.WIRE_REDSTONE_BE / GTWires.WIRE_LASER_BE / GTBlockEntities.WIRE_ELECTRIC_BE)
	}

	// ---------------------------------------------------------------------------
	// tick (upstream onTick2 :145-168, server branch trimmed)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream onTick2 :145-168, server branch. PLACEHOLDER DECLARATION (task p9-wire-family-w1
	 * spec 5a): the IC2 IEnergySource pull branch (upstream :156-165 — the wire TICKS actively
	 * pull from IC2 energy sources adjacent to it: EnergyCompat.IC_ENERGY gate, EnergyNet
	 * unwrap, {@code getOfferedEnergy}, drawEnergy only after a successful transfer) is NOT
	 * replicated — this port has no IC2. The cut position IS the reserved seam for a future
	 * external-energy sink bridge (FE↔EU, the GTCEu EUToFEProvider precedent): an active pull
	 * of neighbour-offered energy into {@link #transferElectricity} belongs exactly here, in
	 * this loop position, after the burn-gate. No bridge interface, no code — javadoc only,
	 * the transport math below stays byte-frozen (the RCON 27/27 semantic lock).
	 */
	/**
	 * The per-family tick. Electric rows: upstream onTick2 :145-168 server branch (the IC2
	 * pull cut declaration below is untouched, task p9-wire-family-w1 spec 5a).
	 *
	 * <p>Redstone rows (task p10): upstream onTick2 :98-106 server branch verbatim — the
	 * per-tick CONVERGENCE trigger of the push-BFS network: the vanilla input cache clears
	 * (:102), a received block update re-scans the connections (:103) and the signal
	 * rescan (updateRedstone :122-133) fans a changed value out through
	 * {@link #doRedstoneUpdate} (:104). SPEC 7: NO burn gate here — the redstone
	 * registration carries no overload machinery at all (mBurnCounter stays 0 forever, the
	 * electric branch is unreachable on redstone rows).
	 *
	 * <p>Laser rows (task p10-wire-laser-placeholder, revived by task p32-qu-laser-domain):
	 * upstream onTick2 (MultiTileEntityWireLaser :57-64) only lags the mTransferred transfer
	 * counter into mTransferredLast and clears the window (:61-62) — now that the LU carrier
	 * machines exist the lag is live bookkeeping again. The burn gate stays OUT (the laser
	 * registration carries no overload machinery — nothing can burn a lossless wire, the
	 * p10 spec-7 redstone posture).
	 */
	/**
	 * The per-family tick (see {@link #tickFamilies}) wrapped in the cover tick pair —
	 * upstream 06Covers :196/:200, the pipe-BE shape (task p34-pool-cover-hosts): the
	 * torch family drives its visual lane from {@link #mRedstone} in tickPost, the
	 * Tag selector re-asserts its constructor mode from tickPre.
	 */
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (hasCovers()) getCovers().tickPre(aTimer, aIsServerSide, mBlockUpdated, false);
		tickFamilies(aTimer, aIsServerSide);
		if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, false);
	}

	private void tickFamilies(long aTimer, boolean aIsServerSide) {
		if (isRedstone()) { // upstream onTick2 :98-106, server branch
			if (aIsServerSide) {
				for (int i : ALL_SIDES) mVanillaSides[i] = -1; // :102 — the vanilla input cache is a per-tick cache
				if (mBlockUpdated) {
					validateConnections(); // p11 — the prune runs first so :103/:104 read post-prune masks
					updateConnectionStatus(); // :103 — upstream onBlockUpdated -> mBlockUpdated
				}
				if (updateRedstone(REDSTONE_ID)) GTWireRedstoneNode.doRedstoneUpdate(this, REDSTONE_ID); // :104 — the convergence trigger
			}
			return;
		}
		if (isLaser()) { // upstream :57-64 — the transfer window lag (the p32 LU revival)
			if (aIsServerSide) {
				if (mBlockUpdated) validateConnections(); // p11 — the deferred re-check (laser bits prune too)
				mTransferredLast = mTransferred; // :61
				mTransferred = 0; // :62
			}
			return;
		}
		if (aIsServerSide) { // upstream :148
			if (mBlockUpdated) validateConnections(); // p11 — the deferred re-check of the sync prune (upstream never
			// consumed mBlockUpdated on the electric rows at all — the exact gap behind the stale mask)
			if (mBurnCounter >= 16) {
				setToFire(); // upstream :149-150
			} else {
				if (aTimer % 512 == 2 && mBurnCounter > 0) mBurnCounter--; // upstream :152 — one strike heals per 512 ticks
				mWattageLast = mTransferredWattage; // upstream :153-155 — the wattage lags one window
				mTransferredWattage = 0;
				mTransferredAmperes = 0;
				// upstream :156-165 — the IC2 IEnergySource pull branch: EnergyCompat has no
				// 1.20.1 counterpart (ADR 2026-08-31-p7-energy-network ruling 5), pool.
			}
		}
	}

	/** Upstream onTickFirst2 :85-88 (redstone rows) — the first-tick connection scan. */
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		// upstream 06Covers :191 — the validity sweep rides onTickFirst before the wire business
		checkCoverValidity();
		if (isRedstone()) updateConnectionStatus(); // :87
	}

	/**
	 * Upstream MultiTileEntityWireRedstone onTickCheck :51-58, verbatim shape (task
	 * p11-wire-brightness): the per-tick refresh of {@link #mState} — the change DETECTOR of
	 * the visual/emission byte. A change does two things, exactly the upstream pair:
	 * <ul>
	 * <li>{@code if (mIsGlowing) updateLightValue()} (:55) → {@link #refreshGlowLight} — the
	 *     {@code level.getLightEngine().checkBlock(pos)} modern form (the
	 *     {@code updateLightValue} TileEntityBase01Root :549-554 landing; the official
	 *     {@code LevelSensitiveLightBlockTest} :119 pattern), gated by {@link #glowingWire()};</li>
	 * <li>{@code return T} (:56) → the dispatcher ({@code TileEntityBase03TicksAndSync.updateEntity})
	 *     calls {@code sendClientData()} → {@code level.sendBlockUpdated} → the ChunkHolder
	 *     broadcast (:222/:238-251) ships {@link #getUpdatePacket} whose tag IS
	 *     {@link #getUpdateTag} = {@code saveWithoutMetadata()} → carries {@code gt.mredstone}
	 *     (written by {@link #saveAdditional}) → the client {@code load()} re-derives the state
	 *     and re-checks its own light engine. THE EXISTING visual-sync channel — no new
	 *     protocol, this override is the only piece the chain was missing.</li>
	 * </ul>
	 * The upstream call order is preserved: {@code onTick} runs the value scan (:104) BEFORE
	 * this check reads it (the dispatcher phase order), so the byte lags the BFS by at most
	 * one tick — upstream-identical.
	 */
	@Override
	public boolean onTickCheck(long aTimer) {
		if (isRedstone()) { // the bare-class body (MultiTileEntityWireRedstone :51-58); electric/laser keep the base
			byte tOldState = mState;
			mState = UT6.bind4(UT6.divup(mRedstone, GTWireSpecs.MAX_RANGE)); // :53
			if (tOldState != mState) {
				if (glowingWire()) refreshGlowLight(); // :55 — the updateLightValue chain, mIsGlowing-gated
				return true; // :56 — the visual sync (dispatcher sendClientData), NOT light-gated
			}
			return super.onTickCheck(aTimer); // :58
		}
		return super.onTickCheck(aTimer);
	}

	/**
	 * The upstream {@code mIsGlowing} gate of the light chain, as the port can ask it: the
	 * material GLOWING flag rides the BLOCK ({@link GTWireBlock#luminous()}) AND the light
	 * must respect the upstream CLASS split — only the BARE wire class implements
	 * {@code IMTE_GetLightValue} (MultiTileEntityWireRedstone :35/:79), the insulated parent
	 * (MultiTileEntityWireRedstoneInsulated) never answers the query, so a glowing cable
	 * ({@code aInsulated} true) stays dark: {@code getLightValue} never fires on it upstream,
	 * {@link #refreshGlowLight} never fires on it here.
	 */
	public boolean glowingWire() {
		return isRedstone() && getBlockState().getBlock() instanceof GTWireBlock tWire
				&& tWire.luminous() && !tWire.insulated();
	}

	/**
	 * The upstream {@code updateLightValue} (TileEntityBase01Root :549-554 — set the own cell
	 * and re-light the six neighbours) collapsed to the 1.20.1 one-liner: the light engine's
	 * {@code checkBlock(pos)} re-samples the block through {@code GTWireBlock.getLightEmission}
	 * and re-propagates to the neighbours itself (the engine owns the spread now). PROTECTED
	 * SEAM: the offline tests override this to record the trigger — a minimal Level double has
	 * no chunk source to hand {@code getLightEngine()} (Level.java:333 →
	 * {@code getChunkSource().getLightEngine()}).
	 */
	protected void refreshGlowLight() {
		if (hasLevel()) getLevel().getLightEngine().checkBlock(getBlockPos());
	}

	/**
	 * Upstream TileEntityBase01Root.setToFire — the wire burns off the network: the block
	 * becomes fire, which replaces the block and retires this BE through the vanilla
	 * setBlock chain (no onRemove involvement, the id59 discipline).
	 */
	private void setToFire() {
		if (hasLevel() && isServerSide()) {
			getLevel().setBlockAndUpdate(getBlockPos(), Blocks.FIRE.defaultBlockState());
		}
	}

	// ---------------------------------------------------------------------------
	// the contact damage (task p10-wire-contact-damage — upstream :203 + UT.Entities)
	// ---------------------------------------------------------------------------

	/**
	 * The shock payload, upstream UT.Entities.applyElectricityDamage :3024-3031 mapped onto
	 * the BE that owns {@link #mWattageLast}:
	 * {@code damage = tierMax(wattage) * 4} (UT6.tierMax, the UT.java:1388-1393 verbatim
	 * port over the V table), applied only when {@code damage > 0 && LivingEntity && alive}
	 * — the {@code damage > 0} clause IS the "unpowered wires don't bite" gate:
	 * {@code tierMax(0) == 0} and {@link #mWattageLast} only becomes non-zero after a tick
	 * window during which amperes actually flowed (onTick :153-155 lag), so a wire that
	 * never transferred — including EVERY fresh world load, since the p7 NBT contract keeps
	 * {@code mWattageLast} transient (upstream writeToNBT2 :121-123 empty body) — is
	 * harmless. Damage 0 at 8 EU (tier 0), 4 at 32 EU (tier 1), 16 at 2048 (tier 4),
	 * 60 at 8589934592 (tier 15); there is NO sneak reduction and NO custom cooldown — the
	 * vanilla invulnerability frames inside {@code LivingEntity.hurt} are the throttle,
	 * exactly upstream's {@code attackEntityFrom} cadence.
	 *
	 * <p>Damage source: upstream {@code DamageSources.getElectricDamage()} is the IC2
	 * {@code DMG_ELECTRIC} source with a heat fallback (gregapi/damage/DamageSources.java:34-38);
	 * with no IC2 in 1.20.1 this port uses the vanilla engine's own electric damage
	 * {@code lightningBolt} ({@code Level.damageSources().lightningBolt()}) — the same
	 * family GTCEu Modern puts its ELECTRIC type in ({@code DamageTypeTags.IS_LIGHTNING},
	 * GTDamageTypes.java:20-22). A gt6-custom DamageType would need a datapack bootstrap
	 * provider outside this card's file scope; declared deviation.
	 *
	 * <p>Immunity: creative mode ports (the {@code isCreative(aEntity)} clause of
	 * upstream isWearingFullElectroHazmat, UT.java:2904-2908). The ARMOR half of that check
	 * (four equipment slots of {@code ArmorsGT.HAZMATS_LIGHTNING}) has no counterpart — this
	 * port mounts no armor system at all (zero ArmorItem/ArmorMaterial surface) — declared
	 * cut: no armor grants lightning-hazmat immunity here, the damage always applies to
	 * non-creative LivingEntities.
	 *
	 * @return whether the damage went through (the upstream boolean, unused by the :203 caller)
	 */
	public boolean applyElectricityDamage(Entity aEntity) {
		long tDamage = pendingContactDamage();
		if (tDamage <= 0 || !(aEntity instanceof LivingEntity tLiving) || !tLiving.isAlive() || isContactImmune(tLiving)) return false;
		return tLiving.hurt(getLevel().damageSources().lightningBolt(), tDamage); // TFC_DAMAGE_MULTIPLIER == 1 (no TFC)
	}

	/**
	 * The gated {@code tierMax(mWattageLast) * 4} accessor — the pure decision half of
	 * {@link #applyElectricityDamage} so the offline truth tables drive the exact formula
	 * the live hook applies. The {@link #isRedstone()} gate is the family lock: redstone
	 * rows mount no shock semantics at all (the redstone registration carries no
	 * NBT_CONTACTDAMAGE — Loader:1893-1902 — and upstream keeps the families on separate
	 * classes, the shock hook existing only on MultiTileEntityWireElectric :203).
	 */
	public long pendingContactDamage() {
		return isRedstone() ? 0 : UT6.tierMax(mWattageLast) * 4L;
	}

	/** The creative clause of upstream isWearingFullElectroHazmat (UT.java:2904-2908); the armor half is a declared cut (see above). */
	public static boolean isContactImmune(Entity aEntity) {
		return aEntity instanceof Player tPlayer && tPlayer.isCreative();
	}

	// ---------------------------------------------------------------------------
	// transferLaser (task p32-qu-laser-domain — the LU revival the p10 placeholder
	// declared, MultiTileEntityWireLaser :66-86 verbatim over the port's pure-data
	// adjacency form)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream MultiTileEntityWireLaser :66-86 — a LOSSLESS recursive FLOOD of the LU
	 * packet. For every valid side but the packet's own (:69, behind
	 * {@code canEmitEnergyTo} = the connected-mask passthrough :118), while the strength
	 * budget lasts (:70): the neighbour enters the {@code aAlreadyPassed} HashSetNoNulls
	 * cycle guard (:72), WIRE neighbours that accept LU recurse with the remaining
	 * strength (:73-76) and non-wire endpoints go through
	 * {@code ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.LU, ...)} (:78); the
	 * actually-used strength is booked as {@code mTransferred += |frequency * used|}
	 * (:83) and returned. The entry point is the LU arm of doEnergyInjection (:100 —
	 * simulate answers {@code aAmount}, the real call seeds a fresh set with
	 * {@code this}); the frequency ({@code aSize}) is the beam strength and
	 * {@code aStrength} ({@code aAmount}) the per-tick amount in the LU sense
	 * (TD.java:110-116).
	 *
	 * @param aSide         the side the packet came in through (skipped by the :69 loop)
	 * @param aFrequency    the beam strength in the LU sense (upstream aFrequency)
	 * @param aStrength     the per-tick amount offered (upstream aStrength)
	 * @param aChannel      the bundled-channel placeholder (upstream passes -1, :100)
	 * @param aAlreadyPassed the :72 cycle guard, seeded with the originating wire
	 * @return the strength actually used downstream
	 */
	public long transferLaser(byte aSide, long aFrequency, long aStrength, long aChannel, HashSetNoNulls<BlockEntity> aAlreadyPassed) {
		long rUsedStrength = 0;

		for (byte tSide : ALL_SIDES_VALID_BUT[aSide]) if (canEmitEnergyTo(tSide)) { // :69
			if (aStrength <= rUsedStrength) break; // :70
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (aAlreadyPassed.add(tNeighbor)) { // :72 — HashSetNoNulls silently skips the nulls of unconnected/empty spots
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tNeighbor instanceof GTWireBlockEntity tWire) {
					if (tWire.isEnergyAcceptingFrom(TD.Energy.LU, tOpposite, false)) { // :74
						rUsedStrength += tWire.transferLaser(tOpposite, aFrequency, aStrength - rUsedStrength, aChannel, aAlreadyPassed); // :75
					}
				} else {
					rUsedStrength += ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.LU, tOpposite, aFrequency, aStrength - rUsedStrength, this, tNeighbor); // :78
				}
			}
		}

		mTransferred += Math.abs(aFrequency * rUsedStrength); // :83

		return rUsedStrength;
	}

	// ---------------------------------------------------------------------------
	// transferElectricity (upstream :170-189 verbatim)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :170-189. Pushes {@code aAmperage} packets of {@code aVoltage} EU out of
	 * every connected side but {@code aSide} (the side the packet came in through), one
	 * loss subtracted per segment. Wire neighbours recurse; everything else goes through
	 * the root {@link ITileEntityEnergy.Util#insertEnergyInto}. {@code aChannel} is the
	 * bundled-channel placeholder upstream passes as -1 (the parameter survives the
	 * verbatim signature; bundled wires are a pool item).
	 *
	 * @return the amperage actually used downstream (0 with no consumer — the bookkeeping
	 *         at :188 only runs when amperes flowed).
	 */
	public long transferElectricity(byte aSide, long aVoltage, long aAmperage, long aChannel, HashSetNoNulls<BlockEntity> aAlreadyPassed) {
		if (mTimer < 1 || Math.abs(aVoltage) <= mLoss) return 0; // upstream :171 — unticked wires and sub-loss packets carry nothing
		if (aVoltage > 0) aVoltage -= mLoss; else aVoltage += mLoss; // upstream :172 — the per-segment loss, sign preserving

		long rUsedAmperes = 0;
		for (byte tSide : ALL_SIDES_VALID_BUT[aSide]) if (canEmitEnergyTo(tSide)) { // upstream :175
			if (aAmperage <= rUsedAmperes) break; // upstream :176
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (aAlreadyPassed.add(tNeighbor)) { // upstream :178 — HashSetNoNulls silently skips the nulls of unconnected/empty spots
				// the DelegatorTileEntity pair in pure-data form: the neighbour BE plus the
				// side of it that faces us (upstream mSideOfTileEntity)
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tNeighbor instanceof GTWireBlockEntity tWire) {
					if (tWire.isEnergyAcceptingFrom(TD.Energy.EU, tOpposite, false)) { // upstream :180
						rUsedAmperes += tWire.transferElectricity(tOpposite, aVoltage, aAmperage - rUsedAmperes, aChannel, aAlreadyPassed); // upstream :181
					}
				} else {
					rUsedAmperes += ITileEntityEnergy.Util.insertEnergyInto(TD.Energy.EU, tOpposite, aVoltage, aAmperage - rUsedAmperes, this, tNeighbor); // upstream :184
				}
			}
		}
		// upstream :188 — with no consumer nothing is recorded (rUsedAmperes == 0), an
		// overload only strikes when amperes actually flowed through addToEnergyTransferred
		return rUsedAmperes > 0 ? addToEnergyTransferred(aVoltage, rUsedAmperes) ? rUsedAmperes : aAmperage : 0;
	}

	/** Upstream :191-199 — the bookkeeping and the overload strike. */
	public boolean addToEnergyTransferred(long aVoltage, long aAmperage) {
		mTransferredAmperes += aAmperage;
		mTransferredWattage += Math.abs(aVoltage * aAmperage);
		if (Math.abs(aVoltage) > mVoltage || mTransferredAmperes > mAmperage) {
			if (mBurnCounter < 16) mBurnCounter++;
			return false;
		}
		return true;
	}

	// ---------------------------------------------------------------------------
	// EU face family (upstream :205-230) + the LU face family (task p32-qu-laser-domain,
	// the MultiTileEntityWireLaser :94-:100 revival the p10 placeholder declared)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :205 with the family gate (task p10): the redstone rows mount NO energy type
	 * at all — upstream keeps the two families on separate classes (the redstone wire never
	 * implements ITileEntityEnergy), and this shared BE reproduces that separation by
	 * gating the whole EU face family off on redstone rows. The gate is what keeps an
	 * electric neighbour from pumping EU into a redstone wire (transferElectricity :207-210
	 * checks isEnergyAcceptingFrom, which reaches this method).
	 *
	 * <p>Laser rows (task p32-qu-laser-domain): upstream the laser wire conducts LU only
	 * (MultiTileEntityWireLaser :94) and REFUSES everything else — the p10 inertness is
	 * gone now that the LU carrier machines exist (the p10 revival condition): laser rows
	 * answer LU on this face (the :94 arm) and the electric rows keep answering EU. The EU
	 * exclusion on laser rows stays exact (the reference-equality gate keeps an electric
	 * neighbour from pumping EU into a laser wire, the :94 LU-only ruling).
	 */
	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		if (isLaser()) return aEnergyType == TD.Energy.LU; // MultiTileEntityWireLaser :94
		return !isRedstone() && aEnergyType == TD.Energy.EU; // upstream :205
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		if (isLaser()) return TD.Energy.LU.AS_LIST; // MultiTileEntityWireLaser :95
		return isRedstone() ? List.of() : TD.Energy.EU.AS_LIST; // upstream :206
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, true) && canEmitEnergyTo(aSide); // upstream :208 / WireLaser :97
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, false) && canAcceptEnergyFrom(aSide); // upstream :209 / WireLaser :98
	}

	@Override
	public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return 0; // upstream :210 / WireLaser :99
	}

	@Override
	public synchronized long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		// the WireLaser :100 LU arm beside the upstream :211 EU arm — same shape, the flood
		// entry; the simulate call (aDoInject=false) reports the full amount taken
		if (isLaser()) {
			return aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)
					? aDoInject ? transferLaser(aSide, aSize, aAmount, -1, new HashSetNoNulls<BlockEntity>(false, this)) : aAmount
					: 0;
		}
		// upstream :211 verbatim — the implementor checks its own acceptance; the simulate
		// call (aDoInject=false) reports the full amount taken without transferring
		return aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)
				? aDoInject ? transferElectricity(aSide, aSize, aAmount, -1, new HashSetNoNulls<BlockEntity>(false, this)) : aAmount
				: 0;
	}

	/** Upstream :212 with the WireLaser :101 laser arm — the laser wire's band is the MAX_VALUE carrier (GTWireSpecs.LASER_CAPACITY). */
	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return isLaser() ? Long.MAX_VALUE : mVoltage;} // upstream :212 / :101

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;} // upstream :213 / :102

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return isLaser() ? Long.MAX_VALUE : mVoltage;} // upstream :214 / :103

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return isLaser() ? Long.MAX_VALUE : mVoltage;} // upstream :215 / :104

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;} // upstream :216 / :102

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return isLaser() ? Long.MAX_VALUE : mVoltage;} // upstream :217 / :106

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;} // the Root default, overridden per the card

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;} // the Root default, overridden per the card

	/** Upstream :229 — a wire emits towards connected sides only. */
	public boolean canEmitEnergyTo(byte aSide) {
		return connected(aSide);
	}

	/** Upstream :230 — a wire accepts from connected sides only. */
	public boolean canAcceptEnergyFrom(byte aSide) {
		return connected(aSide);
	}

	// ---------------------------------------------------------------------------
	// connection (upstream :201 canConnect / :243 connector types / :82-96 onPlaced)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :172 verbatim in the base handshake — BUT the base connect (:141) routes
	 * non-BE neighbours through air/liquid ONLY in this port, while upstream still offers
	 * {@code canConnect(aSide, delegator)} to them (the delegator carries the BLOCK even
	 * when the tile entity is null). For the redstone family that difference is the whole
	 * vanilla interface: the wire must ATTACH to lamps, redstone blocks and plain blocks
	 * (canEmitRedstoneToVanilla/canAcceptRedstoneFromVanilla gate on {@code connected}),
	 * so this override completes the upstream :141 disjunction for the REDSTONE rows by
	 * connecting to any solid non-BE neighbour. Electric rows are untouched (their
	 * canConnect needs a BE, the air/liquid routing is exactly the upstream behaviour).
	 *
	 * <p>THE :130-140 BRANCH (task p10-wire-contact-damage ride-along, the R1 review
	 * handoff): upstream gives a redstone wire a SECOND escape hatch inside the connector
	 * neighbourhood — when the neighbour IS an {@code ITileEntityConnector} but the two
	 * connector-type sets share NO element (TileEntityBase09Connector.java:130-140, the
	 * arm after the :118 intersection gate), the redstone wire still connects. That arm is
	 * what visually joins a redstone wire to an ELECTRIC wire (and any other connector of
	 * a different family): WIRE_REDSTONE and WIRE_ELECTRIC never intersect, so without it
	 * the two families render as disconnected blocks even when they touch. The connection
	 * is ONE-SIDED and purely visual/mask-level: the upstream :130-140 body sets the own
	 * bit and fires the change chain but never calls the partner back (no :126 reciprocal),
	 * and the partner's mask stays clean — a pure appearance difference, exactly upstream.
	 * Intersecting connector neighbours still fall through to the base handshake (the
	 * symmetric + notify form); electric rows never reach this branch.
	 */
	@Override
	public boolean connect(byte aSide, boolean aNotify) {
		if (isRedstone() && aSide >= 0 && aSide < 6 && !connected(aSide) && hasLevel()) {
			BlockPos tTarget = getBlockPos().relative(Direction.from3DDataValue(aSide));
			BlockEntity tNeighbor = getLevel().getBlockEntity(tTarget);
			// the isAirOrLiquid form of the base (private there), negated: the solid non-BE slot
			BlockState tTargetState = getLevel().getBlockState(tTarget);
			FluidState tTargetFluid = tTargetState.getFluidState();
			if (tNeighbor == null && !tTargetState.isAir() && (tTargetFluid == null || tTargetFluid.isEmpty())) {
				setConnectionBit(aSide); // the upstream :142-151 body (bit + notify + block update + onConnectionChange)
				return true;
			}
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) { // upstream :116 — the connector neighbourhood
				byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
				if (!haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(aSide))) {
					setConnectionBit(aSide); // upstream :130-140 — the type-divergent visual connect, one-sided
					return true;
				}
				// intersecting types: fall through to the base handshake (the :118 symmetric + notify form)
			}
		}
		return super.connect(aSide, aNotify);
	}

	/**
	 * Upstream :201 → EnergyCompat.canConnectElectricity :102 — the DOUBLE probe,
	 * accepting {@code ||} emitting, both probed theoretically (the conductor visual
	 * connect). The emitting branch is the p8-d4-energy-source backfill of the upstream
	 * :102 verbatim pair: a pure emitter BE (e.g. the test energy source, whose
	 * isEnergyAcceptingFrom is permanently false) must be wire-connectable, or the
	 * gen→wire chain can never form (the D2 review handoff obligation).
	 *
	 * <p>Redstone rows (task p10): upstream MultiTileEntityWireRedstoneInsulated :172 —
	 * {@code canConnect} returns TRUE unconditionally, the redstone wire attaches to every
	 * neighbour BE (visual connection to whatever it feeds; vanilla blocks still need the
	 * air/liquid slot of the base connect). No ITileEntityEnergy probe on this family.
	 *
		 * <p>Laser rows (task p32-qu-laser-domain): upstream MultiTileEntityWireLaser
		 * :89-92 — the probe accepts a neighbour that accepts-or-emits LU, both probed
		 * theoretically (the aTheoretical conductor visual connect). The p10 inert posture
		 * (permanently false) is gone with the LU carrier revival: the probe is LIVE again,
		 * which is what lets a laser wire visually attach to the CO2 Laser's emission face
		 * and the Laser Absorber's input face (and refuse EU-only neighbours — the probe
		 * answers the LU question, not the EU one, exactly upstream :89-92). A
		 * laser-to-laser visual chain still forms through the connector-type intersection
		 * of the base handshake ({@link #getConnectorTypes} = WIRE_LASER on both ends).
		 */
		@Override
		public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
			if (isRedstone()) return true; // upstream :172 verbatim
			if (isLaser()) { // MultiTileEntityWireLaser :89-92 — the LU accept-or-emit double probe
				if (!(aNeighbor instanceof ITileEntityEnergy tEnergy)) return false;
				byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
				return tEnergy.isEnergyAcceptingFrom(TD.Energy.LU, tOpposite, true)
						|| tEnergy.isEnergyEmittingTo(TD.Energy.LU, tOpposite, true);
			}
		if (!(aNeighbor instanceof ITileEntityEnergy tEnergy)) {
			// no foreign connections: the EU->FE outbound bridge was cut (task
			// p28-cut-eu-fe-bridge), the wire attaches to ITileEntityEnergy neighbours only —
			// the behaviour this port shipped with before the p26 bridge, restored.
			return false;
		}
		byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
		return tEnergy.isEnergyAcceptingFrom(TD.Energy.EU, tOpposite, true)
				|| tEnergy.isEnergyEmittingTo(TD.Energy.EU, tOpposite, true);
	}

	@Override
	public Collection<TagData> getConnectorTypes(byte aSide) {
		return isRedstone() ? TD.Connectors.WIRE_REDSTONE.AS_LIST // upstream :180
				: isLaser() ? TD.Connectors.WIRE_LASER.AS_LIST // MultiTileEntityWireLaser :124 (task p10-wire-laser-placeholder)
				: TD.Connectors.WIRE_ELECTRIC.AS_LIST; // upstream :243
	}

	/**
	 * The mask becomes the CONNECTIONS BlockState — the 1.20.1 visual counterpart of the
	 * connection data (the GTFluidPipeBlockEntity.onConnectionChange twin). Redstone rows
	 * append the upstream onConnectionChange :91-95 verbatim tail: the connection re-scan
	 * plus the CONNECTION-CHANGE trigger of the push-BFS network (:94 — updateRedstone
	 * returning true fans out through {@link #doRedstoneUpdate}).
	 */
	@Override
	public void onConnectionChange(byte aPreviousConnections) {
		super.onConnectionChange(aPreviousConnections);
		if (hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(GTWireBlock.CONNECTIONS)
					&& tState.getValue(GTWireBlock.CONNECTIONS) != (int)getConnections()) {
				getLevel().setBlock(getBlockPos(), tState.setValue(GTWireBlock.CONNECTIONS, (int)getConnections()),
						Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
			}
		}
		if (isRedstone()) {
			updateConnectionStatus(); // upstream :93
			if (updateRedstone(REDSTONE_ID)) GTWireRedstoneNode.doRedstoneUpdate(this, REDSTONE_ID); // upstream :94
		}
	}

	/**
	 * Upstream TileEntityBase09Connector.onPlaced (:82-96), server side, driven by the
	 * BlockItem place chain (GTWireBlockItem.placeBlock — the hook that holds both the
	 * live BE and the BlockPlaceContext). aSide is the CLICKED face
	 * (context.getClickedFace(), 0..5): the wire side that touches the support is its
	 * opposite — the upstream OPOS flip (:84). After the support connect, the :90-94 loop
	 * back-connects every neighbour connector that already faces this wire. Duplicated
	 * from the pipe BE: the base file is frozen and owns no onPlaced in this port.
	 */
	public void onPlaced(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel() || !isServerSide()) return;
		connect(UT6.OPOS[aSide], true); // upstream :84/:88
		for (byte tSide = 0; tSide < 6; tSide++) { // upstream :90-94
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tConnector.connected(tOpposite)
						&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(tSide))) {
					connect(tSide, true); // upstream :93
				}
			}
		}
	}

	// ===========================================================================
	// the stale-mask prune (task p11-connector-stale-mask)
	// ===========================================================================

	/**
	 * The neighbour-change maintenance path the connection mask never had. UPSTREAM
	 * ARCHAEOLOGY: 1.7.10 maintains {@code mConnections} ONLY through the connect/disconnect
	 * handshakes — the connector base (TileEntityBase09Connector.java:111-153/:156-172),
	 * {@code onPlaced} (:82-96) and the cutter tool click (:70-79) — with NO rescan anywhere:
	 * {@code MultiTileEntityWireElectric.onTick2} (:145-168) never consumes
	 * {@code mBlockUpdated}, and the redstone wire's consumption
	 * (MultiTileEntityWireRedstoneInsulated :103 → {@code updateConnectionStatus} :135-138)
	 * recomputes only {@code mConnectedToNonWire}, never the mask. The stale bit therefore
	 * exists upstream too — but it was INVISIBLE there: the upstream mask only drove textures
	 * and gates, and a bit into air was indistinguishable from a legitimate open end (the
	 * base connect accepts air, :141). THIS port promotes the mask to the CONNECTIONS
	 * BlockState (the W2 render + the redstone gating read it), where a bit pointing at a
	 * foreign-family wire is a visible, contract-violating lie: the port's own connect()
	 * (the upstream :118 intersection gate — the one-sided :130-140 arm exists ONLY for
	 * {@code this instanceof ITileEntityRedstoneWire}) makes an electric-towards-redstone bit
	 * unreachable through every legitimate path, so it can ONLY be residue of a neighbour
	 * that changed underneath the wire (the P10 E1 RCON finding: swap the neighbour wire's
	 * family via {@code /setblock} and the old bit survives forever).
	 *
	 * <p>This method re-derives the connect decision for every CONNECTED side against the
	 * CURRENT neighbour ({@link #canStayConnected}) and drops the bits that no longer hold
	 * ({@code disconnect(aSide, false)} — no notify: the partner is by definition either not
	 * a connector at all or a divergent-family connector whose own one-sided bit is none of
	 * this wire's business). PRUNE-ONLY: it never auto-connects, so a deliberate manual
	 * disconnect can never be resurrected by a neighbour change, and the redstone push-BFS
	 * triggers stay exactly the upstream pair (the connection change :93-94, run from inside
	 * the {@code disconnect} → {@code onConnectionChange} chain that already exists).
	 *
	 * <p>Driven synchronously from {@link GTWireBlock#updateShape} (the only seam
	 * {@code /setblock} reaches — its flags=2 carry no {@code neighborChanged}) and from
	 * {@link GTWireBlock#neighborChanged} (player break/place), with the
	 * {@code mBlockUpdated} per-tick consumption in {@link #onTick} as the deferred
	 * catch-all (the headless-server reality: loaded-but-not-entity-ticking chunks never
	 * reach onTick, so the sync paths carry the fix). Safe inside the vanilla neighbour
	 * cascade: the only level write is the {@code disconnect} → {@code onConnectionChange}
	 * state update, which the 1.19.3+ neighbour-update queue contains, and unloaded
	 * neighbour spots are skipped (the {@code isLoaded} guard in {@link #canStayConnected})
	 * so a chunk-border wire can never prune a bit it cannot judge — nor trigger a
	 * synchronous chunk load.
	 */
	public void validateConnections() {
		if (!hasLevel() || !isServerSide()) return;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (connected(tSide) && !canStayConnected(tSide)) disconnect(tSide, false);
		}
	}

	/**
	 * The {@code p11} validity half: would {@link #connect(byte, boolean)} accept the
	 * CURRENT neighbour on {@code aSide} as a NEW connection? Mirrors the connect decision
	 * branch for branch, read-only:
	 * <ul>
	 * <li>redstone rows answer TRUE unconditionally — upstream the redstone wire accepts
	 *     every neighbour: {@code canConnect} is the verbatim TRUE
	 *     (MultiTileEntityWireRedstoneInsulated :172), divergent connectors still connect
	 *     one-sidedly (the base :130-140 arm), solids through the same arm and air/liquid
	 *     through the base :141 arm — so NO redstone bit can ever be stale;</li>
	 * <li>a connector neighbour needs the type intersection (upstream :118) — a
	 *     divergent-family wire (electric↔redstone/laser) is exactly the P10 E1 residue;</li>
	 * <li>a non-connector BE needs the {@link #canConnect} probe (upstream :141 — the EU
	 *     acceptor/emitter double probe on the electric rows, permanently false on laser);</li>
	 * <li>no BE: air/liquid stays a valid open end (upstream :141 — this is what keeps a
	 *     cut/cleared neighbour from pruning the wire's open end, the NOT-a-bug half of the
	 *     repro matrix), a solid block is not.</li>
	 * </ul>
	 * Unloaded spots (the chunk-border case) return TRUE — keep the bit, judge nothing.
	 */
	public boolean canStayConnected(byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		if (!hasLevel()) return true; // offline/persistence view — nothing to re-derive against
		if (isRedstone()) return true; // upstream :172 + :130-140 + :141 — the redstone wire accepts everything
		BlockPos tTarget = getBlockPos().relative(Direction.from3DDataValue(aSide));
		if (!getLevel().isLoaded(tTarget)) return true; // unloaded neighbour: no verdict, no sync chunk load
		BlockEntity tNeighbor = getLevel().getBlockEntity(tTarget);
		if (tNeighbor instanceof TileEntityBase09Connector tConnector) { // upstream :116-118
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(aSide));
		}
		if (tNeighbor != null) return canConnect(aSide, tNeighbor); // upstream :141 — the non-connector BE arm
		BlockState tState = getLevel().getBlockState(tTarget); // the isAirOrLiquid form of the base (private there)
		FluidState tFluid = tState.getFluidState();
		return tState.isAir() || (tFluid != null && !tFluid.isEmpty()); // upstream :141 — open ends stay
	}

	// ===========================================================================
	// the redstone family core (task p10-wire-redstone-family) — the verbatim port
	// of gregapi/tileentity/connectors/ITileEntityRedstoneWire +
	// MultiTileEntityWireRedstoneInsulated. PUSH semantics: the wire PUSHES changed
	// values along the wire chain (GTWireRedstoneNode.doRedstoneUpdate layer-order
	// flood), it is NOT the vanilla PULL scheme (RedStoneWireBlock.updatePowerStrength
	// :266 recalculates from neighbours on demand — deliberately NOT ported, the
	// research card ② ruling).
	// ===========================================================================

	/**
	 * Upstream MultiTileEntityWireRedstoneInsulated :108-119 verbatim — what this wire
	 * SEES on one side. Wire neighbours answer through the chain protocol
	 * (getRedstoneMinusLoss, the :111 double handshake); everything else is a vanilla
	 * input: rejected on unconnected sides (:112) and on the REDSTONE_SINKS
	 * anti-feedback set (:114 — droppers/dispensers re-emitting what we feed them), then
	 * amplified from the vanilla 0..15 into the full range ({@code MAX_RANGE * level -
	 * mLoss}, :118) behind the per-tick cache (:117). Upstream :116 (the Thermal
	 * Expansion Glowstone Illuminator anti-flicker special case) has no counterpart in
	 * this port — no TE, declared cut.
	 */
	public long getRedstoneAtSide(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel()) return 0; // SIDES_INVALID[aSide] :109
		Direction tDirection = Direction.from3DDataValue(aSide);
		BlockPos tTarget = getBlockPos().relative(tDirection);
		BlockEntity tNeighbor = getLevel().getBlockEntity(tTarget);
		if (tNeighbor instanceof GTWireBlockEntity tWire && tWire.isRedstone()) { // :111, the wire branch
			byte tOpposite = (byte)tDirection.getOpposite().get3DDataValue();
			return canAcceptRedstoneFromWire(aSide, REDSTONE_ID) && tWire.canEmitRedstoneToWire(tOpposite, REDSTONE_ID)
					? tWire.getRedstoneMinusLoss(tOpposite, REDSTONE_ID) : 0;
		}
		// Do not accept Redstone coming from any Redstone Sink! (Such as Droppers or Dispensers) — :113-114
		if (!canAcceptRedstoneFromVanilla(aSide)) return 0; // :112
		if (REDSTONE_SINKS.contains(getLevel().getBlockState(tTarget).getBlock())) return 0; // :114
		// upstream :116 — the TE anti-flicker special case: no Thermal Expansion in this port, cut.
		if (mVanillaSides[aSide] < 0) mVanillaSides[aSide] = getRedstoneIncoming(aSide); // :117
		return GTWireSpecs.MAX_RANGE * mVanillaSides[aSide] - mLoss; // :118 — the vanilla 0..15 amplification
	}

	/**
	 * Upstream TileEntityBase06Covers.getRedstoneIncoming :401 (the no-cover branch —
	 * this port mounts no covers on wires): the vanilla signal seen on one side, bound
	 * 0..15. The 1.20.1 equivalent of {@code getIndirectPowerLevelTo} (WorldAndCoords
	 * :139-149, the STRONG-ONLY semantics research card ① pins) is
	 * {@code Level.getSignal(neighbourPos, direction)}.
	 */
	public byte getRedstoneIncoming(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel()) return 0;
		return UT6.bind4(getLevel().getSignal(getBlockPos().relative(Direction.from3DDataValue(aSide)),
				Direction.from3DDataValue(aSide)));
	}

	/**
	 * Upstream :122-133 VERBATIM — the value recomputation. The baseline is the
	 * constant-strength mode ({@code mMode * MAX_RANGE - mLoss}, :124 — see the
	 * {@link #mMode} javadoc for the declared cover cut); the remembered strongest source
	 * side is re-checked first (:126) and the remaining sides race it (:130), keeping
	 * {@link #mReceived} pointing at the winner. A changed value triggers the vanilla
	 * neighbour update behind the {@link #mConnectedToNonWire} gate (:131) and reports
	 * the change to the BFS.
	 */
	@Override
	public boolean updateRedstone(int aRedstoneID) {
		if (aRedstoneID != REDSTONE_ID) return false; // :123
		long oRedstone = mRedstone, tRedstone = mMode * GTWireSpecs.MAX_RANGE - mLoss; // :124
		byte oReceived = mReceived; // :125
		if ((mRedstone = getRedstoneAtSide(oReceived)) <= tRedstone) { // :126
			mRedstone = tRedstone; // :127 — the mMode baseline floor
			mReceived = SIDE_UNDEFINED; // :128
		}
		for (byte tSide : ALL_SIDES_VALID_BUT[oReceived]) if ((tRedstone = getRedstoneAtSide(tSide)) > mRedstone) {mRedstone = tRedstone; mReceived = tSide;} // :130
		if (mRedstone != oRedstone) {if (mConnectedToNonWire) causeBlockUpdate(); return true;} // :131
		return false; // :132
	}

	/** Upstream :135-138 — the mConnectedToNonWire re-scan (any connected side that is not a redstone wire). */
	public void updateConnectionStatus() {
		mConnectedToNonWire = false;
		if (!hasLevel()) return;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (!canAcceptRedstoneFromVanilla(tSide)) continue; // :137, the connected() gate first
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (!(tNeighbor instanceof GTWireBlockEntity tWire && tWire.isRedstone())) mConnectedToNonWire = true;
		}
	}

	// -- the vanilla-facing emission side (upstream :140-152, driven by the Block bridge) --

	/**
	 * Upstream isProvidingWeakPower2 :140-145 / isProvidingStrongPower2 :147-152 — ONE
	 * body (weak == strong verbatim; the aStrong parameter is accepted for bridge
	 * symmetry with GTOvenBlock.bridgeSignal and deliberately ignored). aQuerySide is the
	 * vanilla query direction (the side the RECEIVER sees us from, the
	 * ICoverableTE.redstoneOut :397-399 convention); upstream flips it to the emission
	 * face with OPOS (:142). The neighbour correction (:144) mirrors the vanilla wire
	 * self-subtraction: a RedStoneWireBlock or a redstone conductor on the emission face
	 * costs one strength point. The emission NEVER back-feeds the remembered source side
	 * and NEVER outputs towards redstone-wire neighbours (:162 — the wire chain is
	 * BFS-internal).
	 *
	 * <p>The cover exits (task p34-pool-cover-hosts, upstream 04Covers :427-438): the
	 * plate on the emission face answers BEFORE the wire's own emission — the query-side
	 * convention of {@link ICoverableTE#getRedstoneOutWeak}/{@link ICoverableTE#getRedstoneOutStrong}
	 * is exactly the aQuerySide this bridge receives, and a bare face passes the wire's
	 * own value through unchanged (zero diff without covers).
	 */
	public byte getRedstoneOut(byte aQuerySide, boolean aStrong) {
		if (!hasLevel()) return 0;
		byte aSide = UT6.OPOS[aQuerySide]; // :142/:149 — the query direction flips to the emission face
		byte tOwn = 0;
		if (canEmitRedstoneToVanilla(aSide) && mRedstone > 0) {
			BlockPos tTarget = getBlockPos().relative(Direction.from3DDataValue(aSide));
			BlockState tState = getLevel().getBlockState(tTarget);
			boolean tCorrection = tState.getBlock() instanceof RedStoneWireBlock || tState.isRedstoneConductor(getLevel(), tTarget); // :144
			tOwn = emissionValue(mRedstone, tCorrection);
		}
		return (byte) (aStrong ? getRedstoneOutStrong(aQuerySide, tOwn) : getRedstoneOutWeak(aQuerySide, tOwn));
	}

	/**
	 * The :144 emission expression, exact — {@code bind4(divup(mRedstone, MAX_RANGE) -
	 * correction)} — lifted as a static so the offline truth tables drive the vanilla
	 * output formula (including the neighbour-correction branch) with no Level.
	 */
	public static byte emissionValue(long aRedstone, boolean aNeighbourCorrection) {
		return UT6.bind4(UT6.divup(aRedstone, GTWireSpecs.MAX_RANGE) - (aNeighbourCorrection ? 1 : 0));
	}

	/**
	 * Upstream :155-157 — the comparator reading of the wire, {@code bind4(mRedstone /
	 * MAX_RANGE)} (floor division, NOT divup — the comparator lags the wire output by the
	 * rounding, upstream literal).
	 */
	public byte getComparatorOut() {
		return UT6.bind4(mRedstone / GTWireSpecs.MAX_RANGE);
	}

	// -- the wire-chain protocol (:162-170) --

	/**
	 * Upstream :162 — emission towards vanilla only: never into the remembered source
	 * side (anti-backfeed), only through connected sides, never towards redstone-wire
	 * neighbours (the wire chain is BFS-internal, not vanilla-facing).
	 */
	public boolean canEmitRedstoneToVanilla(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel()) return false;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
		return aSide != mReceived && connected(aSide) && !(tNeighbor instanceof GTWireBlockEntity tWire && tWire.isRedstone());
	}

	/** Upstream :163 — vanilla inputs arrive through connected sides only. */
	public boolean canAcceptRedstoneFromVanilla(byte aSide) {
		return connected(aSide);
	}

	@Override
	public boolean canEmitRedstoneToWire(byte aSide, int aRedstoneID) {
		return isRedstone() && aRedstoneID == REDSTONE_ID && connected(aSide); // upstream :165
	}

	@Override
	public boolean canAcceptRedstoneFromWire(byte aSide, int aRedstoneID) {
		return isRedstone() && aRedstoneID == REDSTONE_ID && connected(aSide); // upstream :166
	}

	/** Upstream :168 — the redstone loss of this wire (the range denominator). */
	@Override
	public long getRedstoneLoss(int aRedstoneID) {
		return aRedstoneID == REDSTONE_ID ? mLoss : GTWireSpecs.MAX_RANGE;
	}

	/** Upstream :169 — the raw value towards the wire chain (aRedstoneID-gated). */
	@Override
	public long getRedstoneValue(byte aSide, int aRedstoneID) {
		return aRedstoneID == REDSTONE_ID ? mRedstone : 0;
	}

	/** Upstream :170 — what a wire NEIGHBOUR receives: this value minus this loss (the per-segment cost). */
	@Override
	public long getRedstoneMinusLoss(byte aSide, int aRedstoneID) {
		return aRedstoneID == REDSTONE_ID ? mRedstone - mLoss : 0;
	}

	// -- the RedstoneWireNode face (the BFS adjacency, the DelegatorTileEntity pure-data form) --

	@Override
	public GTWireRedstoneNode.Adjacent adjacent(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel()) return new GTWireRedstoneNode.Adjacent(null, aSide);
		Direction tDirection = Direction.from3DDataValue(aSide);
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
		byte tOpposite = (byte)tDirection.getOpposite().get3DDataValue();
		return tNeighbor instanceof GTWireBlockEntity tWire && tWire.isRedstone()
				? new GTWireRedstoneNode.Adjacent(tWire, tOpposite) : new GTWireRedstoneNode.Adjacent(null, tOpposite);
	}

	// -- persistence (upstream readFromNBT2 :60-67 / writeToNBT2 :70-75, redstone rows) --

	/**
	 * DEVIATION DECLARATION (task p10-wire-contact-damage ride-along, the R1 review
	 * handoff): on {@code gt.mredstone} this port deliberately does NOT reproduce the
	 * upstream read — upstream :63 reloads with {@code aNBT.getByte("gt.mredstone")}, a
	 * TRUNCATED 8-bit read of a value written as a full long (:74,
	 * {@code UT.NBT.setNumber}): any stored signal above 127 reloads wrong upstream. The
	 * full-range signal (0..{@code GTWireSpecs.MAX_RANGE} = 2^31-1) can never fit a byte,
	 * so the wide {@code getLong} here is the correct round trip and is kept as a declared
	 * deviation, not an accident of translation.
	 */
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		writeCoversToNBT(aNBT); // upstream 06Covers :74 (task p34-pool-cover-hosts — BEFORE the family gate, covers ride every row)
		if (!isRedstone()) return;
		if (mMode != 0) aNBT.putByte(NBT_MODE, mMode); // :72
		aNBT.putByte(NBT_MRECEIVED, mReceived); // :73
		aNBT.putLong(NBT_MREDSTONE, mRedstone); // :74 (UT.NBT.setNumber writes the long)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		readCoversFromNBT(aNBT); // upstream 06Covers :68 (task p34-pool-cover-hosts — BEFORE the family gate)
		if (!isRedstone()) return;
		if (aNBT.contains(NBT_MRECEIVED, Tag.TAG_ANY_NUMERIC)) mReceived = aNBT.getByte(NBT_MRECEIVED); // :62
		if (aNBT.contains(NBT_MREDSTONE, Tag.TAG_ANY_NUMERIC)) mRedstone = aNBT.getLong(NBT_MREDSTONE); // :63 — DEVIATION, see below
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE); // :64
		// :65 (NBT_PIPELOSS) — the loss rides the block carrier in this port (GTWireBlock.lossL), not NBT.
		// BOTH sync channels converge here on the client (chunk data = handleUpdateTag default,
		// block-update = onDataPacket default) — the upstream setVisualData :62-67 landing (task
		// p11-wire-brightness): re-derive mState from the fresh signal and re-check the client
		// light engine when it moved (the packet that carries gt.mredstone is exactly the one
		// onTickCheck's `true` return triggers — see the onTickCheck javadoc).
		if (isClientSide()) {
			byte tOldState = mState;
			mState = UT6.bind4(UT6.divup(mRedstone, GTWireSpecs.MAX_RANGE)); // :64
			if (tOldState != mState && glowingWire()) refreshGlowLight(); // :65, the mIsGlowing gate
			scheduleCoverRenderRefresh(); // task p34-pool-cover-hosts — both sync channels land here
		}
	}

	// ---------------------------------------------------------------------------
	// the cover render face (task p34-pool-cover-hosts — the TileEntityBase08Barrel
	// template: the per-face sprite snapshot for the CoverPlateModel, and the client
	// render refresh on every sync landing; a bare wire keeps ModelData.EMPTY)
	// ---------------------------------------------------------------------------

	private void scheduleCoverRenderRefresh() {
		if (hasCovers() && hasLevel() && isClientSide())
			gregtech6.client.render.GTRenderUpdates.scheduleRenderUpdate(this);
	}

	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		if (!hasCovers()) return super.getModelData();
		java.util.Map<Direction, net.minecraft.resources.ResourceLocation> tSprites = new java.util.EnumMap<>(Direction.class);
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (getCovers().mBehaviours[tSide] == null) continue;
			net.minecraft.resources.ResourceLocation tSprite = getCovers().mBehaviours[tSide].getCoverTextureSurface(tSide, getCovers());
			if (tSprite != null) tSprites.put(Direction.from3DDataValue(tSide), tSprite);
		}
		if (tSprites.isEmpty()) return super.getModelData();
		return gregtech6.client.render.GTModelProperties.derive(super.getModelData())
				.with(gregtech6.client.render.GTModelProperties.RENDER_SNAPSHOT, new gregtech6.covers.GTCoverRenderSnapshot(tSprites))
				.build();
	}
}
