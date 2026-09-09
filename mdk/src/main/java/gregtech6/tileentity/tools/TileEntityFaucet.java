package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.attachment.GTAttachmentSmallBlock;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.attachment.GTAttachmentSmallBlockEntity;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;

/**
 * 1.20.1 counterpart of gregtech/tileentity/tools/MultiTileEntityFaucet.java (task
 * p26-crucible-mold-faucet spec ④) — the crucible faucet attachment: mounted on one
 * horizontal face of a crucible it pulls melt into the mold BELOW itself.
 *
 * <p>Upstream surface, translated branch by branch:
 * <ul>
 * <li>{@code implements ITileEntityMold} (:54) — the faucet is itself a mold DELEGATE:
 *     the crucible's {@code fillMoldAtSide} calls back into {@link #fillMold} and the
 *     chain walks DOWN from here (the {@link #mFacing} side is the input face, :93-95);</li>
 * <li>{@code onTick2} (:81-90) — the auto-pull: every 20 ticks when wrench-enabled, or
 *     on a redstone edge in redstone mode, the faced neighbour is offered a pull through
 *     {@code ITileEntityCrucible.fillMoldAtSide(this, sideOfCrucible, mFacing)};</li>
 * <li>{@code getMoldMaxTemperature} (:98-100) — the row material's melting point ×
 *     {@link #HEAT_RESISTANCE_BONUS}; a hotter pour destroys the faucet (fizzle +
 *     lava, :113-116);</li>
 * <li>{@code getMoldRequiredMaterialUnits} (:103-108) — the delegate BELOW answers;
 *     the BathingPot/MixingBowl fluid branch (:106) is the deferred pool cut (neither
 *     machine is ported, the branch is unreachable);</li>
 * <li>{@code fillMold} (:111-135) — the pour chain: guards (side/null/acid), the
 *     over-temperature self-destruct, then the DOWN walk through stacked faucets and
 *     collision-free cells (the :117-120 {@code WD.hasCollide} loop) until the first
 *     {@link ITileEntityMold}, which answers for real; the pot/bowl fluid arm (:122-133)
 *     is the same deferred cut;</li>
 * <li>{@code onBlockActivated3} (:138-146) — the right-click pull, one shot at the
 *     faced neighbour; the {@link #activateChain} report string is the RCON channel
 *     (the p12 tap precedent: a null player is the acceptance arm);</li>
 * <li>{@code onToolClick2} (:149-162) — monkey wrench toggles auto-pull, soft hammer
 *     resets to redstone mode; the port keeps the state machine on the BE
 *     ({@link #toggleAutoPull} / {@link #resetToRedstone}) and the physical wrench arm
 *     rides the command/RCON channel like every pre-tool-system machine.</li>
 * </ul>
 *
 * <p>The mount semantics are the p12 attachment family (the block carrier is a
 * {@link FaucetBlock}, a {@link GTAttachmentSmallBlock} over a TAP-family row: the four
 * horizontals are exactly the faucet's valid mounts — the faucet class carries no
 * valid-sides override upstream, so the 10Attachment default SIDES_HORIZONTAL governs,
 * TileEntityBase10Attachment.java:51). The block carries the ROW (material for the heat
 * verdict — the registration-NBT-carrier pattern, the acidProof precedent).
 */
public class TileEntityFaucet extends GTAttachmentSmallBlockEntity implements ITileEntityMold {

	/** Upstream MultiTileEntityFaucet.java:55 HEAT_RESISTANCE_BONUS (the mold shares the 1.25 factor). */
	public static final double HEAT_RESISTANCE_BONUS = 1.25;

	/** The upstream NBT_MODE key ("gt.mode" in 1.7.10, the trimmed port key like "acidproof"). */
	public static final String NBT_MODE = "mode";

	/** Upstream :57 mAutoPull — wrench-toggled automatic pull (redstone mode when false). */
	public boolean mAutoPull = false;

	public TileEntityFaucet(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public TileEntityFaucet(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6Molds.FAUCET_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "faucet"; // BET registry path mirrors it (GT6Molds.FAUCET_BE)
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :60-64 / writeToNBT2 :67-70 — the mode flag only;
	// the acid-proof flag rides the block carrier like every attachment)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE)) mAutoPull = aNBT.getBoolean(NBT_MODE);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_MODE, mAutoPull);
	}

	// ---------------------------------------------------------------------------
	// the row material (the block-carrier pattern — the acidProof precedent)
	// ---------------------------------------------------------------------------

	/** The row material of this faucet (upstream the NBT_MATERIAL BE field). */
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof FaucetBlock tBlock) return tBlock.faucetRow().material().get();
		return gregapi.data.MT.Stone; // the /setblock-without-row fallback: the entry material
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :81-90)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncFacingFromBlock(); // the /setblock path never ran setPlacedBy
		if (mAutoPull ? aTimer % 20 == 5 : mBlockUpdated && redstoneIncoming()) {
			pullFromCrucible();
		}
	}

	/**
	 * The one pull attempt at the faced neighbour (upstream :84-87): the neighbour is the
	 * crucible, its side toward the faucet is the facing's opposite. Returns the action
	 * report (the acceptance channel).
	 */
	public String pullFromCrucible() {
		BlockEntity tTarget = adjacent();
		if (!(tTarget instanceof ITileEntityCrucible tCrucible)) return "no crucible on the facing side";
		byte tSideOfCrucible = sideFacingBack(mFacing);
		return tCrucible.fillMoldAtSide(this, tSideOfCrucible, mFacing)
				? "poured into the mold"
				: "the crucible refused the pour";
	}

	/** Upstream hasRedstoneIncoming — the vanilla all-sides signal check (the oven mRedstoneStopped form). */
	public boolean redstoneIncoming() {
		return hasLevel() && getLevel().hasNeighborSignal(getBlockPos());
	}

	/** The facing mirror re-sync from the blockstate (the attachment base package seam is not reachable cross-package; the same 3 lines keyed on the PROPERTY). */
	private void syncFacingFromBlock() {
		BlockState tState = getBlockState();
		if (tState.hasProperty(GTAttachmentSmallBlock.FACING)) {
			mFacing = (byte)tState.getValue(GTAttachmentSmallBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the ITileEntityMold delegate face (:93-135)
	// ---------------------------------------------------------------------------

	/** Upstream :93-95 — the mount face (toward the crucible) is the only input face. */
	@Override
	public boolean isMoldInputSide(byte aSide) {
		return aSide == mFacing;
	}

	/** Upstream :98-100 — the row material's melting point × 1.25. */
	@Override
	public long getMoldMaxTemperature() {
		return (long)(material().mMeltingPoint * HEAT_RESISTANCE_BONUS);
	}

	/**
	 * Upstream :103-108 — the delegate BELOW answers for itself. The BathingPot/MixingBowl
	 * arm (:106, the flat U answer) is the deferred-pool cut: neither machine is ported,
	 * the branch is unreachable in this port.
	 */
	@Override
	public long getMoldRequiredMaterialUnits() {
		BlockEntity tBelow = hasLevel() ? getLevel().getBlockEntity(getBlockPos().below()) : null;
		if (tBelow instanceof ITileEntityMold tMold) return tMold.getMoldRequiredMaterialUnits();
		return 0;
	}

	/**
	 * Upstream :111-135 — the pour chain. Guards first (:112: side, nulls, acid), the
	 * over-temperature self-destruct (:113-116), then the DOWN walk (:117-120) through
	 * stacked faucets and collision-free cells to the first real mold. Returns the amount
	 * of material consumed from {@code aMaterial} (0 = refused).
	 */
	@Override
	public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
		if (aSide != mFacing || aMaterial == null || aMaterial.mMaterial == null
				|| (!isAcidProof() && aMaterial.mMaterial.contains(gregapi.data.TD.Properties.ACID))) return 0;
		if (aTemperature > getMoldMaxTemperature()) {
			// :114-115 — the faucet melts: fizzle + lava (the mold fizz form)
			if (hasLevel()) {
				getLevel().levelEvent(1501, getBlockPos(), 0);
				getLevel().setBlock(getBlockPos(), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
			}
		}
		if (!hasLevel()) return 0;
		BlockEntity tWalk = hasLevel() ? getLevel().getBlockEntity(getBlockPos().below()) : null;
		BlockPos tWalkPos = getBlockPos().below();
		// :117-120 — descend through faucets and collision-free cells (the hasCollide loop)
		while (tWalkPos.getY() > getLevel().getMinBuildHeight() && walkContinues(tWalk, getLevel().getBlockState(tWalkPos))) {
			tWalkPos = tWalkPos.below();
			tWalk = getLevel().getBlockEntity(tWalkPos);
		}
		if (tWalk instanceof ITileEntityMold tMold) {
			// :121 — the mold answers with the side of it that faces the walker (UP)
			byte tSideOfMold = (byte)Direction.UP.get3DDataValue();
			return tMold.fillMold(aMaterial, aTemperature, tSideOfMold);
		}
		return 0; // (:122-133 the pot/bowl fluid arm is the deferred pool cut)
	}

	/**
	 * The :118 loop condition half — a faucet cell continues the walk, a mold stops it,
	 * a collision-free non-mold cell falls through. Static over the carried state so the
	 * offline tests drive it without a Level (the {@code cauldronFillPlan} pure-seam form).
	 */
	static boolean walkContinues(@Nullable BlockEntity aWalk, net.minecraft.world.level.block.state.BlockState aState) {
		if (aWalk instanceof TileEntityFaucet) return true;
		if (aWalk instanceof ITileEntityMold) return false;
		// upstream WD.hasCollide — the cell's collision shape is not empty (offline-safe getter)
		return aState.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO).isEmpty();
	}

	// ---------------------------------------------------------------------------
	// the interaction face (:138-146 activation, :149-162 tool toggles)
	// ---------------------------------------------------------------------------

	/** The per-family chain body — the right-click pull (upstream onBlockActivated3 :138-146). */
	@Override
	protected String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld) {
		if (!isServerSide()) return "client side";
		return pullFromCrucible();
	}

	/** Upstream :156-159 monkey wrench — flip the auto-pull flag. Returns the chat report. */
	public String toggleAutoPull() {
		mAutoPull = !mAutoPull;
		setChanged();
		return mAutoPull ? "Crucible Auto-Input: AUTOMATIC" : "Crucible Auto-Input: REDSTONE";
	}

	/** Upstream :151-155 soft hammer — back to redstone mode. Returns the chat report. */
	public String resetToRedstone() {
		mAutoPull = false;
		setChanged();
		return "Crucible Auto-Input: REDSTONE";
	}

	// ---------------------------------------------------------------------------
	// the block carrier — the attachment block over a TAP-family row (horizontal mounts)
	// ---------------------------------------------------------------------------

	/**
	 * The faucet block: the p12 attachment carrier reused with a faucet BET (the ticker
	 * comes from {@link GT6Molds#FAUCET_BE}) and the faucet name. The TAP family row gives
	 * the four-horizontal mount validity, the sturdy-face check, the unmount-on-host-break
	 * and the thin-plate shape — the upstream faucet has no valid-sides override either
	 * (TileEntityBase10Attachment.java:51 default).
	 */
	public static class FaucetBlock extends GTAttachmentSmallBlock {

		private final GT6Molds.FaucetRow mRow;
		private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

		public FaucetBlock(GT6Molds.FaucetRow aRow,
				java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
			// a synthetic TAP-family row: same mount algebra, faucet identity kept on this class
			super(new gregtech6.registry.GT6Attachments.AttachmentRow(
					aRow.path(), aRow.matDisplay(), GTAttachmentSmallBlock.Family.TAP,
					aRow.acidProof(), 1.0F, aRow.acidProof() ? 6.0F : 5.0F, aRow.sound()),
					aRow.acidProof(), aTickerType, aProperties);
			mRow = aRow;
			mTickerType = aTickerType;
		}

		/** The faucet registration row (the material/acid-proof carrier; named apart from the base's AttachmentRow accessor). */
		public GT6Molds.FaucetRow faucetRow() {
			return mRow;
		}

		@Override
		public BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return mTickerType.get();
		}

		/** The faucet ticks (the GTEntityBlock dispatch — the attachment base carries no ticker of its own). */
		@Override
		@Nullable
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
			if (aType != tickerType()) return null;
			return (aTickerLevel, aPos, aTickerState, aTile) -> {
				if (aTile instanceof TileEntityBase03TicksAndSync tTile && tTile.canUpdate() && !tTile.isRemoved()) {
					tTile.updateEntity();
				}
			};
		}

		/** The composed faucet name (the tap/funnel compose shape over the faucet template). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable(GT6Molds.FAUCET_DISPLAY_KEY,
					Component.translatable(GT6Molds.faucetMatUnitKeyOf(mRow)));
		}
	}
}
