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
import gregtech6.fluid.FluidTankGT;

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
			// the onPlaced handshake (upstream :82-96) run on the first tick: idempotent, and it
			// also covers /setblock placement (setPlacedBy never fires there)
			for (byte tSide = 0; tSide < 6; tSide++) connect(tSide, true);
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

			// :401-410 — any other fluid handler, probed with 1 L and then the full stack (both simulate)
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
	}
}
