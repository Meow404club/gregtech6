package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Dried (hardened) C-Foam SLAB — the mSlabs form of {@link GT6CFoamBlock} (upstream
 * BlockCFoam.java:44-46 {@code makeSlab}), the spec_rulings.ruling_slab vanilla
 * {@link SlabBlock} ruling: hardness 4.0 / resistance 1.5 / stone sound, drops itself,
 * the dried foam face (applyFoam/dryFoam constant false, hasFoam/driedFoam true,
 * removeFoam → air).
 */
public class GT6CFoamSlabBlock extends SlabBlock implements IBlockFoamable {

	public GT6CFoamSlabBlock(BlockBehaviour.Properties aProperties) {
		super(aProperties);
		registerDefaultState(stateDefinition.any()
				.setValue(GT6CFoamFreshBlock.COLOR, 0)
				.setValue(TYPE, SlabType.BOTTOM));
	}

	/** The registration properties — the dried numbers over the vanilla slab base. */
	public static BlockBehaviour.Properties driedSlabProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(4.0F, 1.5F)
				.sound(SoundType.STONE);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder); // TYPE + WATERLOGGED
		aBuilder.add(GT6CFoamFreshBlock.COLOR);
	}

	// -------------------------------------------------------------------------
	// the IBlockFoamable face (the BlockCFoam.java:53-75 slab forms)
	// -------------------------------------------------------------------------

	@Override
	public boolean applyFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide, int aRGB, int aDyeIndex) {
		return false;
	}

	@Override
	public boolean dryFoam(Level aLevel, BlockPos aPos, @Nullable Direction aSide) {
		return false;
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
		return true;
	}
}
