package gregtech6.tileentity.connectors;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTBlockEntities;
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
 * <p>Cuts (pool, per the card): the 16-wire/5-cable material spectrum and its registration
 * row (W1 ships two voltage-carried variants over the block carrier), the material/
 * insulation conductor data (ITileEntityEnergyDataConductor), the contact damage and the
 * texture/render passes, the tooltips and the electrometer tool click, the IC2 pull.
 * Persistence: the upstream writeToNBT2 (:121-123) is an empty body — the ratings travel
 * on the block carrier ({@link GTWireBlock}, the GTBarrelBlock registration-carrier
 * precedent), so this BE adds NO NBT beyond the base {@code mConnections}.
 */
public class GTWireBlockEntity extends TileEntityBase09Connector implements ITileEntityEnergy {

	/**
	 * Upstream :64 — transfer bookkeeping and the wire rating defaults: 32 EU packets,
	 * 1 A of amperage, 1 EU loss per segment. The constructor overwrites the three
	 * ratings from the block carrier (GTWireBlock, the vanilla-block fallbacks are exactly
	 * these upstream defaults).
	 */
	public long mTransferredAmperes = 0, mTransferredWattage = 0, mWattageLast = 0, mLoss = 1, mAmperage = 1, mVoltage = 32;

	/** Upstream :65 — the overload strike counter; 16 strikes burn the wire (mRenderType is a render cut). */
	public byte mBurnCounter = 0;

	/**
	 * Upstream CS.java ALL_SIDES_VALID_BUT — the six sides minus the packet's own side,
	 * in the GT6 side order. Carried locally like the D1 Util.ALL_SIDES_VALID (root CS
	 * increment policy).
	 */
	private static final byte[][] ALL_SIDES_VALID_BUT = {
			{1, 2, 3, 4, 5}, // DOWN
			{0, 2, 3, 4, 5}, // UP
			{0, 1, 3, 4, 5}, // NORTH
			{0, 1, 2, 4, 5}, // SOUTH
			{0, 1, 2, 3, 5}, // WEST
			{0, 1, 2, 3, 4}, // EAST
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
	 * the upstream field defaults (:64).
	 */
	public GTWireBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.WIRE_ELECTRIC_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTWireBlock tWire) {
			mVoltage = tWire.voltageL();
			mAmperage = tWire.amperageL();
			mLoss = tWire.lossL();
		}
	}

	@Override
	public String getTileEntityName() {
		return "wire_electric"; // BET registry path mirrors it (GTBlockEntities.WIRE_ELECTRIC_BE)
	}

	// ---------------------------------------------------------------------------
	// tick (upstream onTick2 :145-168, server branch trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
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

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == TD.Energy.EU; // upstream :205
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.EU.AS_LIST; // upstream :206
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
	 */
	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (!(aNeighbor instanceof ITileEntityEnergy tEnergy)) return false;
		byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
		return tEnergy.isEnergyAcceptingFrom(TD.Energy.EU, tOpposite, true)
				|| tEnergy.isEnergyEmittingTo(TD.Energy.EU, tOpposite, true);
	}

	@Override
	public Collection<TagData> getConnectorTypes(byte aSide) {
		return TD.Connectors.WIRE_ELECTRIC.AS_LIST; // upstream :243
	}

	/**
	 * The mask becomes the CONNECTIONS BlockState — the 1.20.1 visual counterpart of the
	 * connection data (the GTFluidPipeBlockEntity.onConnectionChange twin).
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
}
