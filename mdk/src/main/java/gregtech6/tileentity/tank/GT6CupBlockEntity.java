package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GT6CupBlock;
import gregtech6.registry.GT6Cups;

/**
 * The Porcelain Cup — the port counterpart of upstream {@code MultiTileEntityCup}
 * (tmp/gt6-1.7.10 .../tanks/MultiTileEntityCup.java, the "Fluid Containers" row
 * Loader_MultiTileEntities.java:2094: 250 L, LIQUIDPROOF T + MAGICPROOF T, stack 16,
 * aUtilStone). The FIRST concrete of the small-tank base {@link TileEntityBase10FluidContainerSmall}
 * — the jug card (2000 L, canWaterCrops/canPickUpFluids) reuses the same base.
 *
 * <p>The row door: the base :419-421 gate over the pair LIQUIDPROOF T (the liquid band
 * admits) + GASPROOF F (gases refuse) — the mirror of the cell's gas-only pair. The
 * MAGICPROOF T column rides the row verbatim (vacuous under the dataset cut — the base
 * doc); NBT_TEMPERATURE = the Porcelain melting point 1800 K (MT.java:2139 {@code heat(1800)},
 * recorded-only). The rain collector + the any-side drink face are the two live flags
 * (MultiTileEntityCup.java:84-85 — {@code canFillWithRain=T}, {@code canDrinkFromSide=T}).
 *
 * <p>The fill-level display rides the static per-level {@code LIQUID_LEVEL} 0..8 blockstate
 * (the cell/crucible form, event-driven from {@link #onTankChanged}).
 */
public class GT6CupBlockEntity extends TileEntityBase10FluidContainerSmall {

	/** Upstream Loader :2094 NBT_TANK_CAPACITY — 250. */
	public static final long CAPACITY = 250;

	/** Upstream Loader :2094 registration stack column — 16 (the base :423 content gate rides the item). */
	public static final int STACK_SIZE = 16;

	/** Upstream :2094 NBT_TEMPERATURE = aMat.mMeltingPoint — the Porcelain heat(1800) row (recorded-only). */
	public static final long TEMPERATURE_MAX = 1800;

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the cell form). */
	public GT6CupBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6Cups.CUP_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the cell explicit-type form). */
	public GT6CupBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState, CAPACITY);
		mMagicProof = true; // the :2094 NBT_MAGICPROOF T column
		// mLiquidProof stays the base T, mGasProof the base F — the row pair lands on the defaults
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.cup"; // upstream MultiTileEntityCup.java:88 verbatim
	}

	@Override
	public boolean canFillWithRain() {
		return true; // upstream MultiTileEntityCup.java:84 verbatim
	}

	// ---------------------------------------------------------------------------
	// the fill-level display face (the cell form over the cup block property)
	// ---------------------------------------------------------------------------

	@Override
	public void onTankChanged() {
		super.onTankChanged();
		syncLevelProperty();
	}

	/** The 0..8 fill bucket — 1 + (amount-1)*8/capacity (the cell formula, 250 L bands of ~31.25 L). */
	public int levelBucket() {
		long tAmount = mTank.amount();
		return tAmount <= 0 ? 0 : (int)(1 + (tAmount - 1) * 8 / mTank.capacity());
	}

	/** The property ride (the cell shape, UPDATE_CLIENTS only). */
	private void syncLevelProperty() {
		if (!hasLevel()) return;
		BlockState tState = getBlockState();
		if (tState.hasProperty(GT6CupBlock.LIQUID_LEVEL) && tState.getValue(GT6CupBlock.LIQUID_LEVEL) != levelBucket()) {
			getLevel().setBlock(getBlockPos(), tState.setValue(GT6CupBlock.LIQUID_LEVEL, levelBucket()), Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------------------
	// the row door (the liquid-only static form — the item handler shares it)
	// ---------------------------------------------------------------------------

	/** The static cup door: the :2094 pair LIQUIDPROOF T / GASPROOF F — the item face rides this. */
	static boolean admits(@Nullable String aFluidName) {
		return admissionGate(aFluidName, false, true);
	}
}
