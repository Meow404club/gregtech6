package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GT6JugBlock;
import gregtech6.registry.GT6Jugs;

/**
 * The Ceramic Jug — the port counterpart of upstream {@code MultiTileEntityJug}
 * (tmp/gt6-1.7.10 .../tanks/MultiTileEntityJug.java, the "Fluid Containers" row
 * Loader_MultiTileEntities.java:2095: 2000 L, LIQUIDPROOF T, everything else F,
 * stack 16, aUtilStone). The SECOND concrete of the small-tank base
 * {@link TileEntityBase10FluidContainerSmall} — every behaviour face (the
 * admission door, the drink seam, the rain collector, the NBT projection, the
 * capability) is the base's; this class adds only the row constants and the two
 * jug-only interaction flags.
 *
 * <p>The row door: the base :419-421 gate over the pair LIQUIDPROOF T (the liquid
 * band admits — water AND lava, the scoop face needs the lava half) + GASPROOF F.
 * MAGICPROOF F rides the base default (no override — the cup's T column is the
 * exception, not this row). NBT_TEMPERATURE = the Ceramic melting point 2000 K
 * (MT.java:1279 {@code heat(2000)}, recorded-only under the dataset cut).
 *
 * <p>The two live flags upstream rides that the cup does not carry
 * (MultiTileEntityJug.java:87-88, defaults F in the base class): the crop-watering
 * face (the cauldron tiers, consumed by {@link gregtech6.item.GT6JugBlockItem}
 * {@code useOn}) and the source-scooping face (consumed by {@code use}). The drink
 * face narrows to the top side only ({@code canDrinkFromSide → SIDES_TOP},
 * :90) — the block carrier's gate, not the BE's.
 */
public class GT6JugBlockEntity extends TileEntityBase10FluidContainerSmall {

	/** Upstream Loader :2095 NBT_TANK_CAPACITY — 2000. */
	public static final long CAPACITY = 2000;

	/** Upstream :2095 registration stack column — 16 (the base :423 content gate rides the item). */
	public static final int STACK_SIZE = 16;

	/** Upstream :2095 NBT_TEMPERATURE = aMat.mMeltingPoint — the Ceramic heat(2000) row (recorded-only). */
	public static final long TEMPERATURE_MAX = 2000;

	/** Upstream MultiTileEntityJug.java:87 {@code canWaterCrops() → T} — the cauldron-watering gate (item face). */
	public static final boolean CAN_WATER_CROPS = true;

	/** Upstream MultiTileEntityJug.java:88 {@code canPickUpFluids() → T} — the source-scooping gate (item face). */
	public static final boolean CAN_PICK_UP_FLUIDS = true;

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime (the cup form). */
	public GT6JugBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6Jugs.JUG_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the cup explicit-type form). */
	public GT6JugBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState, CAPACITY);
		// every proof flag rides the base default (LIQUIDPROOF T / the rest F) — the :2095 row needs no override
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.jug"; // upstream MultiTileEntityJug.java:93 verbatim
	}

	@Override
	public boolean canFillWithRain() {
		return true; // upstream MultiTileEntityJug.java:89 verbatim
	}

	// ---------------------------------------------------------------------------
	// the fill-level display face (the cup form over the jug block property)
	// ---------------------------------------------------------------------------

	@Override
	public void onTankChanged() {
		super.onTankChanged();
		syncLevelProperty();
	}

	/** The 0..8 fill bucket — 1 + (amount-1)*8/capacity (the cell formula, 2000 L bands of 250 L). */
	public int levelBucket() {
		long tAmount = mTank.amount();
		return tAmount <= 0 ? 0 : (int)(1 + (tAmount - 1) * 8 / mTank.capacity());
	}

	/** The property ride (the cup shape, UPDATE_CLIENTS only). */
	private void syncLevelProperty() {
		if (!hasLevel()) return;
		BlockState tState = getBlockState();
		if (tState.hasProperty(GT6JugBlock.LIQUID_LEVEL) && tState.getValue(GT6JugBlock.LIQUID_LEVEL) != levelBucket()) {
			getLevel().setBlock(getBlockPos(), tState.setValue(GT6JugBlock.LIQUID_LEVEL, levelBucket()), Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------------------
	// the row door (the liquid-only static form — the item handler shares it)
	// ---------------------------------------------------------------------------

	/** The static jug door: the :2095 pair LIQUIDPROOF T / GASPROOF F — the item face rides this. */
	static boolean admits(@Nullable String aFluidName) {
		return admissionGate(aFluidName, false, true);
	}
}
