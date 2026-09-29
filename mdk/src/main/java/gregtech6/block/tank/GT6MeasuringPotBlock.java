package gregtech6.block.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import java.util.function.Supplier;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.GT6MeasuringPotBlockEntity;

/**
 * The Measuring Pot block carrier — the block side of task r8-issue45-c3 (issue #45),
 * the GTBarrelBlock/GTKitchenBlock carrier pattern: the block carries the upstream
 * registration row's values (Loader_MultiTileEntities.java:2096 "Ceramic Measuring Pot",
 * category "Fluid Containers": hardness 0.5 / resistance 6.0, aUtilStone) plus the BET,
 * and the BE carries the tank.
 *
 * <p>{@code use} is the two-arm click face (upstream onBlockActivated3 :73-93):
 * <ul>
 * <li>EMPTY hand on a HORIZONTAL side → the hitY fill-limit re-bind
 *     ({@link GT6MeasuringPotBlockEntity#limitChain} — the :74 SIDES_HORIZONTAL +
 *     null-held gate, the GT6 signature interaction);</li>
 * <li>everything else → {@code FluidUtil.interactWithFluidHandler} through the BE's
 *     FLUID_HANDLER capability (the GTBarrelBlock.use :233 shape — the held-container
 *     fill/drain clicks, the upstream Base08 :142-171 container chain counterpart; the
 *     drink arm :158-169 rides the declared pool cut). An empty hand on a vertical side
 *     falls through to PASS (the upstream no-op T arm with the drink face cut).</li>
 * </ul>
 *
 * <p>Geometry: the sub-cube tub (the upstream collision pool :136-138, the 8px-wide
 * 4px-inset footprint, 8px tall) over the {@code noOcclusion()} properties seam (the
 * kitchen family's #9 X-ray fix shape). Rendering rides the two-layer element model
 * (the datagen colored+overlay grammar, the bumbliary form); the grayscale colored band
 * ships UN-TINTED — the r7-40-41 crucible bodyTexture declared deviation
 * ({@code ponytail:} the tintindex-0 + dispatch row would land it, the model needs no
 * change when the render pool gets to it).
 */
public class GT6MeasuringPotBlock extends GTEntityBlock {

	/** The upstream collision pool :136-138 — box(4,0,4 → 12,8,12) px (the walls at 4px inset, 8px tall). */
	public static final VoxelShape SHAPE_POT = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.5, 0.75);

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6MeasuringPotBlock(Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6MeasuringPotBlock> codec() {
		return simpleCodec(aProperties -> new GT6MeasuringPotBlock(mTickerType, aProperties));
	}
	 *///?}

	/** The registration properties (upstream :2096 hardness 0.5 / resistance 6.0, aUtilStone; the kitchen seam chain). */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties potProperties() {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(0.5F, 6.0F).sound(SoundType.STONE)
				.noOcclusion().isViewBlocking(GT6MeasuringPotBlock::never);
	}

	/** The fog-only rider (the GT6Kitchen seam body — the sub-cube vessel never blocks the view). */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	/** The sub-cube tub shape (selection AND collision — the vanilla getShape delegation, the GTKitchenBlock form). */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE_POT;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6MeasuringPotBlockEntity tPot
				&& aPlayer.getItemInHand(aHand).isEmpty()
				&& aHit.getDirection().getAxis().isHorizontal()) { // :74 SIDES_HORIZONTAL + null-held gate
			tPot.limitChain(aPlayer, (float) (aHit.getLocation().y - aPos.getY()));
			return InteractionResult.SUCCESS;
		}
		// the container chain (the GTBarrelBlock.use FluidUtil shape — both sides, the server pass authoritative)
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); MAIN_HAND stands in, the GTBarrelBlock
	// 1.21.1 declared deviation.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6MeasuringPotBlockEntity tPot
				&& aPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
				&& aHit.getDirection().getAxis().isHorizontal()) {
			tPot.limitChain(aPlayer, (float) (aHit.getLocation().y - aPos.getY()));
			return InteractionResult.SUCCESS;
		}
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}
}
