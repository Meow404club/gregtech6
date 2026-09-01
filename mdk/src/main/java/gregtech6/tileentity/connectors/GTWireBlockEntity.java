package gregtech6.tileentity.connectors;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTWireSpecs;
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
 * data (ITileEntityEnergyDataConductor), the contact damage and the texture/render passes,
 * the tooltips and the electrometer tool click, and the IC2 pull remain pool items. The two
 * standing placeholders are declared at their exact positions below: the IC2-pull cut on
 * {@link #onTick} (with the reserved external-energy-bridge seam) and the bundled-channel
 * {@code aChannel} parameter on {@link #transferElectricity}.
 * Persistence: the upstream writeToNBT2 (:121-123) is an empty body — the ratings travel
 * on the block carrier ({@link GTWireBlock}, the GTBarrelBlock registration-carrier
 * precedent), so this BE adds NO NBT beyond the base {@code mConnections}.
 */
public class GTWireBlockEntity extends TileEntityBase09Connector implements ITileEntityEnergy, GTWireRedstoneNode {

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
	 * Upstream :56 — the side the strongest input came from (GT6 side order, or
	 * {@link #SIDE_UNDEFINED}); updateRedstone :126-130 keeps it and updateRedstone's
	 * own baseline excludes it from the rescan (ALL_SIDES_VALID_BUT[oReceived]).
	 */
	public byte mReceived = SIDE_UNDEFINED;

	/**
	 * Upstream :56 — the constant-strength source mode: updateRedstone :124 baselines the
	 * wire at {@code mMode * MAX_RANGE - mLoss}. Upstream switches it only through
	 * setStateMode from the CoverSelectorRedstone cover (CoverSelectorRedstone.java:42-51);
	 * that cover is a declared pool item this phase (it depends on the unported
	 * SwitchableMode surface), so {@code mMode} persists at its default 0 with the
	 * field + formula carried verbatim — the semantics stay complete for the cover card.
	 */
	public byte mMode = 0;

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
	 */
	public GTWireBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.WIRE_ELECTRIC_BE.get(), aPos, aState);
		mRedstoneFamily = aState.getBlock() instanceof GTWireBlock tWire && tWire.family() == GTWireSpecs.Row.Family.REDSTONE;
		if (aState.getBlock() instanceof GTWireBlock tWire) {
			mVoltage = tWire.voltageL();
			mAmperage = tWire.amperageL();
			mLoss = tWire.lossL();
		}
	}

	/** The family gate for everything redstone on this shared class (task p10). */
	public boolean isRedstone() {
		return mRedstoneFamily;
	}

	@Override
	public String getTileEntityName() {
		return isRedstone() ? "wire_redstone" : "wire_electric"; // the BET registry path twin (GTWires.WIRE_REDSTONE_BE / GTBlockEntities.WIRE_ELECTRIC_BE)
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
	 */
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (isRedstone()) { // upstream onTick2 :98-106, server branch
			if (aIsServerSide) {
				for (int i : ALL_SIDES) mVanillaSides[i] = -1; // :102 — the vanilla input cache is a per-tick cache
				if (mBlockUpdated) updateConnectionStatus(); // :103 — upstream onBlockUpdated -> mBlockUpdated
				if (updateRedstone(REDSTONE_ID)) GTWireRedstoneNode.doRedstoneUpdate(this, REDSTONE_ID); // :104 — the convergence trigger
			}
			return;
		}
		if (aIsServerSide) { // upstream :148
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
		if (isRedstone()) updateConnectionStatus(); // :87
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
	// EU face family (upstream :205-230)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :205 with the family gate (task p10): the redstone rows mount NO energy type
	 * at all — upstream keeps the two families on separate classes (the redstone wire never
	 * implements ITileEntityEnergy), and this shared BE reproduces that separation by
	 * gating the whole EU face family off on redstone rows. The gate is what keeps an
	 * electric neighbour from pumping EU into a redstone wire (transferElectricity :207-210
	 * checks isEnergyAcceptingFrom, which reaches this method).
	 */
	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !isRedstone() && aEnergyType == TD.Energy.EU; // upstream :205
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return isRedstone() ? List.of() : TD.Energy.EU.AS_LIST; // upstream :206
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, true) && canEmitEnergyTo(aSide); // upstream :208
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, false) && canAcceptEnergyFrom(aSide); // upstream :209
	}

	@Override
	public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return 0; // upstream :210
	}

	@Override
	public synchronized long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		// upstream :211 verbatim — the implementor checks its own acceptance; the simulate
		// call (aDoInject=false) reports the full amount taken without transferring
		return aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)
				? aDoInject ? transferElectricity(aSide, aSize, aAmount, -1, new HashSetNoNulls<BlockEntity>(false, this)) : aAmount
				: 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :212

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;} // upstream :213

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :214

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :215

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;} // upstream :216

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return mVoltage;} // upstream :217

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
	 */
	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (isRedstone()) return true; // upstream :172 verbatim
		if (!(aNeighbor instanceof ITileEntityEnergy tEnergy)) return false;
		byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
		return tEnergy.isEnergyAcceptingFrom(TD.Energy.EU, tOpposite, true)
				|| tEnergy.isEnergyEmittingTo(TD.Energy.EU, tOpposite, true);
	}

	@Override
	public Collection<TagData> getConnectorTypes(byte aSide) {
		return isRedstone() ? TD.Connectors.WIRE_REDSTONE.AS_LIST // upstream :180
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
	 */
	public byte getRedstoneOut(byte aQuerySide, boolean aStrong) {
		if (!hasLevel()) return 0;
		byte aSide = UT6.OPOS[aQuerySide]; // :142/:149 — the query direction flips to the emission face
		if (!canEmitRedstoneToVanilla(aSide) || mRedstone <= 0) return 0;
		BlockPos tTarget = getBlockPos().relative(Direction.from3DDataValue(aSide));
		BlockState tState = getLevel().getBlockState(tTarget);
		boolean tCorrection = tState.getBlock() instanceof RedStoneWireBlock || tState.isRedstoneConductor(getLevel(), tTarget); // :144
		return emissionValue(mRedstone, tCorrection);
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

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (!isRedstone()) return;
		if (mMode != 0) aNBT.putByte(NBT_MODE, mMode); // :72
		aNBT.putByte(NBT_MRECEIVED, mReceived); // :73
		aNBT.putLong(NBT_MREDSTONE, mRedstone); // :74 (UT.NBT.setNumber writes the long)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (!isRedstone()) return;
		if (aNBT.contains(NBT_MRECEIVED, Tag.TAG_ANY_NUMERIC)) mReceived = aNBT.getByte(NBT_MRECEIVED); // :62
		if (aNBT.contains(NBT_MREDSTONE, Tag.TAG_ANY_NUMERIC)) mRedstone = aNBT.getLong(NBT_MREDSTONE); // :63 (upstream reads the wide form)
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE); // :64
		// :65 (NBT_PIPELOSS) — the loss rides the block carrier in this port (GTWireBlock.lossL), not NBT.
	}
}
