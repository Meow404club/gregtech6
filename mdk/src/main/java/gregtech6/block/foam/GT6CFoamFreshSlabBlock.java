package gregtech6.block.foam;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;

/**
 * Fresh (wet) C-Foam SLAB — the mSlabs form of {@link GT6CFoamFreshBlock} (upstream
 * BlockCFoamFresh.java:51-53 {@code makeSlab}), the spec_rulings.ruling_slab ruling:
 * a vanilla {@link SlabBlock} subclass, every fresh trait carried over (passable, no
 * drops, the 100+rand(5900) dry schedule) with the upstream vertical-slab capability
 * (mSlabs[2..5]) DEGRADED to the vanilla bottom/top halves — the declared deviation.
 *
 * <p>The dry transition copies BOTH the half type and the colour (upstream :110-112
 * {@code mSlabs[mSide]} — the same-position slab-to-slab form).
 */
public class GT6CFoamFreshSlabBlock extends SlabBlock implements IBlockFoamable {

	/** The dried-slab target, resolved at dry time (post-registration). */
	private final java.util.function.Supplier<SlabBlock> mDriedTarget;

	public GT6CFoamFreshSlabBlock(BlockBehaviour.Properties aProperties, java.util.function.Supplier<SlabBlock> aDriedTarget) {
		super(aProperties);
		mDriedTarget = aDriedTarget;
		registerDefaultState(stateDefinition.any()
				.setValue(GT6CFoamFreshBlock.COLOR, 0)
				.setValue(TYPE, SlabType.BOTTOM));
	}

	/** The registration properties — the fresh traits over the vanilla slab base (the light-path note on the full block). */
	public static BlockBehaviour.Properties freshSlabProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOL)
				.strength(1.0F, 0.0F)
				.sound(SoundType.WOOL)
				.noOcclusion()
				.noCollission();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder); // TYPE + WATERLOGGED (the vanilla slab pair)
		aBuilder.add(GT6CFoamFreshBlock.COLOR);
	}

	/** Upstream :60-62 — the placed wet slab schedules its drying. */
	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aIsMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aIsMoving);
		if (!aLevel.isClientSide) {
			aLevel.scheduleTick(aPos, this, GT6CFoamFreshBlock.dryDelay(aLevel.random));
		}
	}

	/** Upstream :64-67 — the scheduled tick dries the slab (the public widening, the fresh block's tick note). */
	@Override
	public void tick(BlockState aState, ServerLevel aLevel, BlockPos aPos, RandomSource aRandom) {
		dryFoam(aLevel, aPos, null);
	}

	// -------------------------------------------------------------------------
	// the IBlockFoamable face (the :105-127 slab forms)
	// -------------------------------------------------------------------------

	@Override
	public boolean applyFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide, int aRGB, int aDyeIndex) {
		return false;
	}

	@Override
	public boolean dryFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		// upstream :110-112 — the dried slab, the half type AND the colour carried over
		BlockState tState = aLevel.getBlockState(aPos);
		BlockState tDried = mDriedTarget.get().defaultBlockState()
				.setValue(TYPE, tState.getValue(TYPE))
				.setValue(GT6CFoamFreshBlock.COLOR, tState.getValue(GT6CFoamFreshBlock.COLOR));
		return aLevel.setBlock(aPos, tDried, 3);
	}

	@Override
	public boolean removeFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		aLevel.removeBlock(aPos, false);
		return true;
	}

	@Override
	public boolean hasFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return true;
	}

	@Override
	public boolean driedFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return false;
	}
}
