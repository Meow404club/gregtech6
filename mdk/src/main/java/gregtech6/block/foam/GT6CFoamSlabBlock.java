package gregtech6.block.foam;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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
		// the WATERLOGGED=false pin is LOAD-BEARING (issue #42): BooleanProperty's first
		// value is TRUE (BooleanProperty.java:9 ImmutableSet.of(true, false)), the vanilla
		// SlabBlock ctor pins false for exactly this reason (SlabBlock.java:32-33) — this
		// override drops the vanilla pin, and the FRESH slab's dryFoam lands on THIS
		// defaultBlockState, so every slab dried in water's presence stayed waterlogged.
		registerDefaultState(stateDefinition.any()
				.setValue(GT6CFoamFreshBlock.COLOR, 0)
				.setValue(TYPE, SlabType.BOTTOM)
				.setValue(WATERLOGGED, Boolean.FALSE));
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
	// the water seal (issue #42 — the upstream sealing semantics)
	// -------------------------------------------------------------------------

	/**
	 * The foam seal (issue #42): upstream foam is a sealing material — 1.7.10 vanilla water
	 * cannot flow into it (Material.sponge/rock take the {@code blocksMovement()}=true
	 * default and the BlockDynamicLiquid flow-into gate is {@code !blocksMovement()},
	 * func_149809_q reading func_149807_p). The vanilla {@link SlabBlock} super here
	 * accepts waterlogging (SimpleWaterloggedBlock, SlabBlock.java:26), which the fluid
	 * tick exploits the other way around: FlowingFluid.canHoldFluid:377-380 →
	 * spreadTo:238-239 → the slab placeLiquid (SlabBlock.java:100-101) — a slab sprayed
	 * beside water comes out WATERLOGGED=true on the very next fluid tick (the #42
	 * symptom). Refuse every liquid.
	 */
	@Override
	public boolean placeLiquid(LevelAccessor aLevel, BlockPos aPos, BlockState aState, FluidState aFluid) {
		return false;
	}

	//? if forge {
	@Override
	public boolean canPlaceLiquid(BlockGetter aLevel, BlockPos aPos, BlockState aState, Fluid aFluid) {
		return false;
	}
	//?} else {
	/*@Override
	public boolean canPlaceLiquid(@Nullable net.minecraft.world.entity.player.Player aPlayer, BlockGetter aLevel, BlockPos aPos, BlockState aState, Fluid aFluid) {
		return false;
	}
	*///?}

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
