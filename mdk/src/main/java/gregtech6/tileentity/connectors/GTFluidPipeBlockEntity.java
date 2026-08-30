package gregtech6.tileentity.connectors;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.client.render.GTRenderUpdates;
import gregtech6.fluid.FluidTankGT;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of gregapi/tileentity/connectors/MultiTileEntityPipeFluid.java
 * (533 lines) — the trimmed direct translation (task p4-fluid-pipes spec ③).
 *
 * <p>The GT6 fluid transport semantics are kept verbatim: there is NO network object —
 * every pipe segment is its own tank+ pump that pushes fluid to its neighbours every few
 * ticks (upstream class doc and distribute :341-429). Port scope:
 * <ul>
 * <li>{@code mTanks} — a {@link FluidTankGT} per tank slot (upstream :77, W1 ships the
 *     single-tank wood tiers, tank count is block-derived where upstream read
 *     NBT_TANK_COUNT/:116-126);</li>
 * <li>{@link #distribute} — the pressure-equalising push (:341-429): targets are adjacent
 *     pipes whose receiving tank holds LESS than ours (:387, pressure driven) and adjacent
 *     IFluidHandlers probed with two simulated fills (:403); the total is split with
 *     divup over targets+self (:415), pipes filled before tanks (:417/:421) and the
 *     leftover above the bandwidth {@code mCapacity/2} pushed a second time (:427-428);</li>
 * <li>anti-backflow — {@code mLastReceivedFrom[tankIndex]} SBIT mask (:74): external
 *     fills record their source side (SideFluidHandler :487), pushes record the receiving
 *     neighbour's mask (:389), and distribute skips the masked sides (:378) until the
 *     mask clears at the end of the round (:331);</li>
 * <li>phase staggering — the upstream SERVER_TICK_PRE/PR2 coordinate-parity split
 *     (:128-135/:234-245) becomes a BE-ticker gate {@code (timer + offset) % 5 == 0} with
 *     a random per-segment offset, the GTCEu FREQUENCY+offset precedent
 *     (FluidPipeBlockEntity.java:67/:75/:154);</li>
 * <li>connection — the {@link TileEntityBase09Connector} handshake with
 *     {@link TD.Connectors#PIPE_FLUID} types (:528) and the fluid-handler canConnect hook
 *     (:493-508, minus the extenders/cauldron branches);</li>
 * <li>capability — the side-wrapped {@link SideFluidHandler} via
 *     {@code getCapability(FLUID_HANDLER, Direction)} (spec ⑤).</li>
 * </ul>
 *
 * <p>Cuts (pool, per the card): corrosion leaks, over-temperature ignition and the entity
 * damage loop (:285-327), the vanilla cauldron filling speciality (:343-364), the break
 * dump (:432-445), the gibbl/temperature/progress telemetry interfaces, the magnifier
 * network dump and the plunger. The bandwidth rule survives inside the leftover push.
 */
public class GTFluidPipeBlockEntity extends TileEntityBase09Connector {

	/** Distribution rounds per tick-window (GTCEu FluidPipeBlockEntity FREQUENCY=5, :67). */
	public static final int DISTRIBUTION_PERIOD = 5;

	/** NBT keys — the upstream "gt.tank.i"/"gt.mlast.i"/"gt.mtransfer" family (:120-121/:142-145) in the in-repo plain key form. */
	public static final String NBT_TANK_PREFIX = "tank.";
	public static final String NBT_LAST_PREFIX = "last.";
	public static final String NBT_TRANSFERRED = "transferred";

	/** The ioMask NBT key (task p4-pipe-flow-control spec ⑤ — the only new key of the card). */
	public static final String NBT_IO_MASK = "ioMask";

	/** Upstream :74 — one 6-bit source mask per tank. */
	public byte[] mLastReceivedFrom = new byte[0];

	/** Upstream :75 — the per-round transferred amount (persisted, upstream :107/:145). */
	public long mTransferredAmount = 0;

	/** Upstream :75/:114 — per-tank capacity; the bandwidth is mCapacity/2 (:427). */
	public long mCapacity = 1000;

	/** Upstream :77. */
	public FluidTankGT[] mTanks = new FluidTankGT[0];

	/** The random phase offset within {@link #DISTRIBUTION_PERIOD} (assigned on the first server tick, GTCEu offset :75). */
	private int mPhaseOffset = 0;
	private boolean mPhaseAssigned = false;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTFluidPipeBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET. The tank shape comes
	 * from the block tier (the 1.20.1 counterpart of the registration NBT_TANK_CAPACITY,
	 * upstream :114) — vanilla blocks fall back to the upstream default 1000 (:75).
	 */
	public GTFluidPipeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : gregtech6.registry.GTFluidPipes.FLUID_PIPE_BE.get(), aPos, aState);
		mCapacity = aState.getBlock() instanceof gregtech6.block.pipe.GTFluidPipeBlock tPipe ? tPipe.capacityPerTank() : 1000;
		mTanks = new FluidTankGT[] {new FluidTankGT(mCapacity).setIndex(0)};
		mLastReceivedFrom = new byte[1];
	}

	@Override
	public String getTileEntityName() {
		return "fluid_pipe"; // BET registry path mirrors it (GTFluidPipes.FLUID_PIPE_BE)
	}

	// ---------------------------------------------------------------------------
	// tick: phase-gated distribution (upstream onServerTickPre :257-338, trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide && hasLevel()) {
			// the random stagger of the two-phase upstream tick lists (:128-135) — GTCEu offset style
			mPhaseOffset = getLevel().random.nextInt(DISTRIBUTION_PERIOD);
			mPhaseAssigned = true;
			// NO auto-handshake here (task p4-pipe-flow-control spec ② — the new baseline is
			// "GT6 pipes never auto-connect"): upstream does its placement connect in onPlaced
			// (TileEntityBase09Connector.java:82-96, driven here by the BlockItem place chain),
			// everything after that is manual per-face work. The W1 first-tick all-sides
			// handshake would resurrect connections the user manually disconnected (mConnections
			// persists across chunk loads, upstream :53-62).
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide && mPhaseAssigned && (aTimer + mPhaseOffset) % DISTRIBUTION_PERIOD == 0) {
			mTransferredAmount = 0; // upstream :258
			for (FluidTankGT tTank : mTanks) {
				if (tTank.has()) distribute(tTank);
				mLastReceivedFrom[tTank.mIndex] = 0; // upstream :331 — the mask guards exactly one round
			}
		}
	}

	// ---------------------------------------------------------------------------
	// distribute (upstream :341-429, cauldron/cover branches cut)
	// ---------------------------------------------------------------------------

	/** Upstream :415 UT.Code.divup — ceiling division. */
	public static long divup(long aNumber, long aDivider) {
		return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);
	}

	/**
	 * Upstream :341-429. Pushes from aTank into every adjacent target: connected pipes
	 * whose fillable tank holds less than ours, and fluid handlers that accept the fluid.
	 */
	public void distribute(FluidTankGT aTank) {
		if (aTank.isEmpty() || !hasLevel()) return;

		List<FluidTankGT> tPipes = new ArrayList<>();
		List<IFluidHandler> tTanks = new ArrayList<>();
		long tAmount = aTank.amount(); // :372
		int tTargetCount = 1;         // :374 — includes THIS for even distribution

		for (byte tSide = 0; tSide < 6; tSide++) {
			// :378 — don't you dare flow backwards!
			if ((mLastReceivedFrom[aTank.mIndex] & SBIT[tSide]) != 0) continue;
			if (!canEmitFluidsTo(tSide)) continue; // :380

			Direction tDirection = Direction.from3DDataValue(tSide);
			byte tOpposite = (byte)tDirection.getOpposite().get3DDataValue();
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
			if (tNeighbor == null) continue;

			// :384-398 — a neighbouring pipe with a fillable, less-full tank
			if (tNeighbor instanceof GTFluidPipeBlockEntity tPipe) {
				FluidTankGT tTank = tPipe.getFluidTankFillable(tOpposite, aTank.get());
				if (tTank != null && tTank.amount() < aTank.amount()) {
					tPipe.mLastReceivedFrom[tTank.mIndex] |= SBIT[tOpposite]; // :389 — mark the receiver
					tPipes.add(getLevel().random.nextInt(tPipes.size() + 1), tTank); // :391 random position
					tAmount += tTank.amount(); // :393
					tTargetCount++;
				}
				continue;
			}

			// :401-410 — any other fluid handler, probed with 1 L and then the full stack (both simulate).
			// spec ③ gate: the external push runs ONLY through faces carrying the output arrow
			// (isOutputFace = ioMask bit AND connected — the explicit pump-valve semantics of
			// task p4-pipe-flow-control); pipe-to-pipe equalisation above is NOT gated.
			if (!isOutputFace(tSide)) continue;
			IFluidHandler tHandler = tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, tDirection.getOpposite()).orElse(null);
			if (tHandler == null) continue;
			FluidStack tProbe1 = aTank.get(1), tProbeAll = aTank.get(Long.MAX_VALUE);
			if ((tProbe1 != null && tHandler.fill(tProbe1, IFluidHandler.FluidAction.SIMULATE) > 0)
					|| (tProbeAll != null && tHandler.fill(tProbeAll, IFluidHandler.FluidAction.SIMULATE) > 0)) {
				tTanks.add(getLevel().random.nextInt(tTanks.size() + 1), tHandler); // :405 random position
				tTargetCount++;
			}
		}

		if (tTargetCount <= 1) return; // :413
		tAmount = divup(tAmount, tTargetCount); // :415

		// :417 — pipes first
		for (FluidTankGT tPipe : tPipes) {
			mTransferredAmount += aTank.remove(tPipe.add(aTank.amount(tAmount - tPipe.amount()), aTank.get()));
		}
		if (aTank.isEmpty()) return; // :419

		// :421 — tanks afterwards
		for (IFluidHandler tHandler : tTanks) {
			FluidStack tOffer = aTank.get(tAmount);
			if (tOffer == null) break;
			mTransferredAmount += aTank.remove(tHandler.fill(tOffer, IFluidHandler.FluidAction.EXECUTE));
			if (aTank.isEmpty()) break; // upstream :423 between-targets guard
		}
		if (aTank.isEmpty()) return;

		// :425-428 — leftover pressure above the bandwidth (mCapacity/2 L) pushes to pipes again
		if (tPipes.isEmpty()) return;
		long tLeftover = (aTank.amount() - mCapacity / 2) / tPipes.size();
		if (tLeftover > 0) for (FluidTankGT tPipe : tPipes) {
			mTransferredAmount += aTank.remove(tPipe.add(aTank.amount(tLeftover), aTank.get()));
		}
	}

	// ---------------------------------------------------------------------------
	// fill/drain tank lookups (upstream getFluidTankFillable2 :463-468 / getFluidTankDrainable2 :471-475)
	// ---------------------------------------------------------------------------

	/** Upstream :463-468 — a containing tank first, then an empty one; side-gated by canAcceptFluidsFrom. */
	@Nullable
	public FluidTankGT getFluidTankFillable(byte aSide, @Nullable FluidStack aFluid) {
		if (aSide >= 0 && aSide < 6 && !canAcceptFluidsFrom(aSide)) return null;
		if (aFluid == null || aFluid.isEmpty()) return null;
		for (FluidTankGT tTank : mTanks) if (tTank.contains(aFluid)) return tTank;
		for (FluidTankGT tTank : mTanks) if (tTank.isEmpty()) return tTank;
		return null;
	}

	/** Upstream :471-475 — a containing tank only; side-gated by canEmitFluidsTo. */
	@Nullable
	public FluidTankGT getFluidTankDrainable(byte aSide, @Nullable FluidStack aFluid) {
		if (aSide >= 0 && aSide < 6 && !canEmitFluidsTo(aSide)) return null;
		if (aFluid == null || aFluid.isEmpty()) return null;
		for (FluidTankGT tTank : mTanks) if (tTank.contains(aFluid)) return tTank;
		return null;
	}

	/** Upstream :485-488 — the external-fill record: source side into the mask, then dirty + client sync. */
	public void onFilledFrom(byte aSide, FluidTankGT aTank) {
		if (aSide >= 0 && aSide < 6) {
			mLastReceivedFrom[aTank.mIndex] |= SBIT[aSide];
		}
		setChanged();
		updateClientData();
	}

	/** Upstream :510. */
	public boolean canEmitFluidsTo(byte aSide) {
		return connected(aSide);
	}

	/** Upstream :511. */
	public boolean canAcceptFluidsFrom(byte aSide) {
		return connected(aSide);
	}

	// ---------------------------------------------------------------------------
	// connection (upstream :493-511/:528)
	// ---------------------------------------------------------------------------

	/** Upstream :493-508 — non-connector neighbours: fluid handlers with at least one tank. */
	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (aNeighbor == null) return false;
		IFluidHandler tHandler = aNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.from3DDataValue(aSide).getOpposite()).orElse(null);
		return tHandler != null && tHandler.getTanks() > 0;
	}

	@Override
	public List<TagData> getConnectorTypes(byte aSide) {
		return TD.Connectors.PIPE_FLUID.AS_LIST; // upstream :528
	}

	/** The mask becomes the CONNECTS BlockState — the 1.20.1 visual counterpart of the connection data. */
	@Override
	public void onConnectionChange(byte aPreviousConnections) {
		super.onConnectionChange(aPreviousConnections);
		if (hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS)
					&& tState.getValue(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS) != (int)getConnections()) {
				getLevel().setBlock(getBlockPos(), tState.setValue(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS, (int)getConnections()),
						Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// placement (upstream onPlaced :82-96 — task p4-pipe-flow-control spec ②)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream TileEntityBase09Connector.onPlaced (:82-96), server side, driven by the
	 * BlockItem place chain (GTFluidPipeBlockItem.placeBlock — the only vanilla hook that
	 * holds both the live BE and the BlockPlaceContext; the architect ruling rejects a
	 * BE.onLoad first-check because every chunk load would replay the support-side connect
	 * and resurrect manually disconnected pipes). aSide is the CLICKED face
	 * (context.getClickedFace(), 0..5): the pipe side that touches the neighbour is its
	 * opposite — the upstream OPOS flip (:84). A support block that is not a pipe/fluid
	 * container fails the connect → all six sides stay unconnected (upstream :87).
	 *
	 * <p>The upstream :86 allowInteraction ownership gate is cut (no ownership chain in
	 * this port, deviation recorded on the card). /setblock placement never reaches here —
	 * the seam is accepted on the card (GT6 does not auto-connect anyway); headless
	 * acceptance drives this method explicitly via /gt6pipe place.
	 */
	public void onPlaced(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel() || !isServerSide()) return;
		connect(UT6.OPOS[aSide], true); // upstream :84/:87
		// upstream :88-93 — symmetric back-connect to neighbouring connectors that already
		// connect towards this pipe (the Delegator mSideOfTileEntity validity check is the
		// opposite-side tautology and folds away with the direct BE access)
		for (byte tSide = 0; tSide < 6; tSide++) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tConnector.connected(tOpposite)
						&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(tSide))) {
					connect(tSide, true); // upstream :91
				}
			}
		}
	}

	// ---------------------------------------------------------------------------
	// per-face output arrows (spec ①/③ — the monkeywrench output layer of
	// MultiTileEntityPipeItem.java:128-153, single-layered onto one mask)
	// ---------------------------------------------------------------------------

	/**
	 * The 6-bit output-arrow mask (task p4-pipe-flow-control spec ①): a set bit = the face
	 * pushes to external fluid handlers. Decoupled from the connection state — any face can
	 * carry the arrow, the gate is applied at runtime ({@link #isOutputFace}).
	 */
	public byte mIoMask = 0;

	/** The clamp mirrors getConnections() — the whole 6-bit space. */
	public byte getIoMask() {
		return (byte)(mIoMask & 63);
	}

	/**
	 * spec ③ — the distribute gate for the external IFluidHandler branch: the face carries
	 * the arrow AND is connected. Pipe-to-pipe equalisation is never gated.
	 */
	public boolean isOutputFace(byte aSide) {
		return aSide >= 0 && aSide < 6 && (mIoMask & SBIT[aSide]) != 0 && connected(aSide);
	}

	/**
	 * The shift-right-click toggle (the monkeywrench :128-153 single-layered): the side
	 * flips, marked → unmarked or the reverse; the other bits stay untouched; several faces
	 * can be marked at once; no three-state cycle. /gt6pipe output shares this entry.
	 */
	public boolean toggleOutput(byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		mIoMask ^= SBIT[aSide];
		outputChanged();
		return true;
	}

	/** /gt6pipe clear — drop every arrow on the pipe. */
	public void clearOutputs() {
		if (mIoMask == 0) return;
		mIoMask = 0;
		outputChanged();
	}

	/**
	 * The right-click (hoe = the wrench substitute, ToolActions.HOE_DIG — the cover
	 * onCoverToolClick precedent) connection toggle of upstream onToolClick2
	 * (TileEntityBase09Connector.java:70-79): connected → disconnect, else connect. The
	 * air/liquid branch of connect (upstream :141, mdk isAirOrLiquid) stays the open-end
	 * "manual pipe mouth" semantics. /gt6pipe toggle shares this entry.
	 */
	public boolean toggleConnection(byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		if (connected(aSide)) return disconnect(aSide, true);
		return connect(aSide, true);
	}

	/** spec ①/⑤ — every arrow change: dirty + the client sync window + the render-update pair. */
	private void outputChanged() {
		setChanged();
		updateClientData();
		GTRenderUpdates.scheduleRenderUpdate(this);
	}

	// ---------------------------------------------------------------------------
	// arrow render + client sync (spec ⑤ — the two 03 sync channels carry the ioMask
	// through saveAdditional/load; the client refreshes the render snapshot on each landing)
	// ---------------------------------------------------------------------------

	@Override
	public void onLoad() {
		super.onLoad();
		scheduleFlowRenderRefresh();
	}

	@Override
	public void handleUpdateTag(CompoundTag aTag) {
		super.handleUpdateTag(aTag);
		scheduleFlowRenderRefresh(); // chunk-data channel (login/chunk load)
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection aNet, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket aPacket) {
		super.onDataPacket(aNet, aPacket);
		scheduleFlowRenderRefresh(); // block-update channel (arrow changes)
	}

	private void scheduleFlowRenderRefresh() {
		if (mIoMask != 0 && hasLevel() && isClientSide()) GTRenderUpdates.scheduleRenderUpdate(this);
	}

	/**
	 * The C-grade render hook (IForgeBlockEntity.java:174): a pipe with output arrows
	 * hands the render thread the immutable {@link gregtech6.client.render.PipeFlowSnapshot};
	 * unmarked pipes keep {@code ModelData.EMPTY} and render through the plain blockstate
	 * model (spec ④ — the zero-blockstate overlay, the oven cover snapshot form).
	 */
	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		byte tMask = getIoMask();
		if (tMask == 0) return super.getModelData();
		return gregtech6.client.render.GTModelProperties.derive(super.getModelData())
				.with(gregtech6.client.render.GTModelProperties.RENDER_SNAPSHOT, new gregtech6.client.render.PipeFlowSnapshot(tMask))
				.build();
	}

	// ---------------------------------------------------------------------------
	// capability (spec ⑤ — the side wrapper per getCapability call, GTCEu IOFluidHandlerList form)
	// ---------------------------------------------------------------------------

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// fresh per-call wrapper: the side is part of the handler identity
			return LazyOptional.of(() -> new SideFluidHandler(this, aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream :105-146, tank contents only — the tank shape is block-derived)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		for (int i = 0; i < mTanks.length; i++) {
			mTanks[i].writeToNBT(aNBT, NBT_TANK_PREFIX + i);
			aNBT.putByte(NBT_LAST_PREFIX + i, mLastReceivedFrom[i]);
		}
		aNBT.putLong(NBT_TRANSFERRED, mTransferredAmount);
		aNBT.putByte(NBT_IO_MASK, mIoMask); // spec ⑤ — the plain-key byte (bit0-5)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		for (int i = 0; i < mTanks.length; i++) {
			if (aNBT.contains(NBT_TANK_PREFIX + i, Tag.TAG_COMPOUND)) {
				mTanks[i].readFromNBT(aNBT, NBT_TANK_PREFIX + i);
			}
			mLastReceivedFrom[i] = aNBT.getByte(NBT_LAST_PREFIX + i);
		}
		if (aNBT.contains(NBT_TRANSFERRED, Tag.TAG_ANY_NUMERIC)) {
			mTransferredAmount = aNBT.getLong(NBT_TRANSFERRED);
		}
		if (aNBT.contains(NBT_IO_MASK, Tag.TAG_ANY_NUMERIC)) {
			mIoMask = (byte)(aNBT.getByte(NBT_IO_MASK) & 63); // bit0-5 clamp, the mConnections form
		}
	}
}
