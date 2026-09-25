package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * Base for multiblock controller blocks — the block-side of
 * {@link TileEntityBase10MultiBlockBase}. The visual payload rides the base-owned
 * properties (the FACING mirror + the FORMED bit replacing upstream direction-data bit 3,
 * TileEntityBase10MultiBlockBase.java:188-189); the BE writes both (applyFormedBlockState /
 * setFacingFromPlacement double-write, the GTOvenBlock pattern).
 *
 * <p>Controllers tick (the 600-tick poll), so the GTEntityBlock ticker and newBlockEntity
 * wiring is inherited untouched ({@code tickerType()} stays abstract per machine family).
 */
public abstract class GTMultiBlockControllerBlock extends GTEntityBlock {

	/**
	 * The block's upstream {@code NBT_MATERIAL} column (task p38-c2-controller-tint — the
	 * tint colour source, the {@link GTMultiBlockPartBlock} lazy-Supplier form): the mains
	 * families hand their row material up through the new ctor. Null = the material-less
	 * controllers (coke oven, boiler mains, ...) — the white no-tint identity.
	 */
	@Nullable
	private final java.util.function.Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	protected GTMultiBlockControllerBlock(Properties aProperties) {
		this(aProperties, null);
	}

	/** The material-carrier form (task p38-c2-controller-tint): the row feeds the tint colour source. */
	protected GTMultiBlockControllerBlock(Properties aProperties,
			@Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties);
		mMaterial = aMaterial;
		registerDefaultState(this.stateDefinition.any()
				.setValue(TileEntityBase10MultiBlockBase.FACING, Direction.NORTH)
				.setValue(TileEntityBase10MultiBlockBase.FORMED, false));
	}

	/**
	 * The block's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier
	 * (MT.init runs after class-load — the GTBarrels MetalDrumRow form); null = the
	 * material-less controllers (the white identity, upstream UNCOLORED CS.java:327).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The controller-domain material dispatch (task p38-c2-controller-tint, the
	 * {@code GTMultiBlockPartBlock.materialOf} mirror shape): only the carrier blocks
	 * resolve a material — every other block is null here.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GTMultiBlockControllerBlock tController ? tController.material() : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(TileEntityBase10MultiBlockBase.FACING, TileEntityBase10MultiBlockBase.FORMED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer (task p27-cokeoven-facing-fix — the placementFacing
		// table: the view OPPOSITE, the vanilla furnace idiom, GT6StaticStorages.java:265
		// precedent) — the same side setPlacedBy/setFacingFromPlacement writes, so the
		// client prediction and the server pair-write agree with no flicker.
		return defaultBlockState().setValue(TileEntityBase10MultiBlockBase.FACING,
				aContext.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, @Nullable LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer
				&& aLevel.getBlockEntity(aPos) instanceof TileEntityBase10MultiBlockBase tController) {
			tController.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131 (GTOvenBlock precedent)
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default is INVISIBLE (BER assumption)
	}
}
