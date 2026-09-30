package gregtech6.block.tank;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.GT6JugBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase10FluidContainerSmall;

/**
 * The Ceramic Jug block carrier (task small-tank-jug) — the block side of the jug row
 * (Loader_MultiTileEntities.java:2095: hardness 0.5 / resistance 6.0, aUtilStone →
 * STONE sound), the {@link gregtech6.block.tank.GT6CupBlock} carrier pattern over the
 * shared small-tank base {@link TileEntityBase10FluidContainerSmall}. The one
 * behavioural difference from the cup: the drink seam narrows to the TOP face
 * (upstream {@code canDrinkFromSide → SIDES_TOP}, MultiTileEntityJug.java:90 — the
 * cup drank from any side); the FluidUtil container chain stays all-sides (the cup
 * form — the tap/funnel top-gating pair has no port consumer, the cell-card cut).
 *
 * <p>Geometry: the upstream collision/selection pool MultiTileEntityJug.java:81-83 —
 * box(PX_P[3], 0, PX_P[3] → PX_N[3], PX_N[2], PX_N[3]) px = the 10x14x10 thick-walled
 * cross silhouette. Rendering rides the static per-level elements model
 * ({@link #LIQUID_LEVEL} 0..8, the cup/crucible form: four 1px rim walls over the body
 * slab, the per-level fluid box riding the smeltery_content placeholder — the declared
 * ceiling).
 */
public class GT6JugBlock extends GTEntityBlock {

	/** The upstream collision/selection pool :81-83 — box(3,0,3 → 13,14,13) px (10x14x10, the thick-walled cross). */
	public static final VoxelShape SHAPE = Shapes.box(3.0 / 16.0, 0.0, 3.0 / 16.0, 13.0 / 16.0, 14.0 / 16.0, 13.0 / 16.0);

	/** The fill-level bucket 0..8 (the cup {@code gt_liquid_level} property form — 9 variants, 9 shared models). */
	public static final IntegerProperty LIQUID_LEVEL = IntegerProperty.create("gt_liquid_level", 0, 8);

	/**
	 * The upstream drink-face gate :90 {@code canDrinkFromSide → SIDES_TOP[aSide]} — the
	 * jug drinks from the top face ONLY (the cup drank from any side; this is the one
	 * behavioural override of the card). Pure so the offline test pins it.
	 */
	public static boolean drinksFromSide(Direction aSide) {
		return aSide == Direction.UP;
	}

	/** The row properties (upstream :2095 NBT_HARDNESS 0.5, NBT_RESISTANCE 6.0, aUtilStone → STONE). */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties jugProperties() {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(0.5F, 6.0F).sound(SoundType.STONE)
				.noOcclusion().isViewBlocking(GT6JugBlock::never);
	}

	/** The sub-cube rider (the cup seam body — the jug never blocks the view). */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6JugBlock(java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(getStateDefinition().any().setValue(LIQUID_LEVEL, 0));
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6JugBlock> codec() {
		return simpleCodec(aProperties -> new GT6JugBlock(mTickerType, aProperties));
	}
	 *///?}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<
			net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {
		aBuilder.add(LIQUID_LEVEL);
	}

	/** The sub-cube jug silhouette (selection AND collision — the vanilla getShape delegation, the cup form). */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
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
		// the drink seam, TOP-FACE GATED (the :90 SIDES_TOP form — the cup difference):
		// an empty hand on the top of a drinkable jug drinks; a held container keeps the
		// FluidUtil bucket behaviour below
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase10FluidContainerSmall tTank
				&& aPlayer.getItemInHand(aHand).isEmpty()) {
			if (drinksFromSide(aHit.getDirection()) && tTank.tryTankDrink(aPlayer)) return InteractionResult.CONSUME;
			return InteractionResult.PASS;
		}
		// the container chain (the GTBarrelBlock.use FluidUtil shape — all-sides, the cup form)
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); MAIN_HAND stands in, the GT6CupBlock
	// 1.21.1 declared deviation.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase10FluidContainerSmall tTank
				&& aPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			if (drinksFromSide(aHit.getDirection()) && tTank.tryTankDrink(aPlayer)) return InteractionResult.CONSUME;
			return InteractionResult.PASS;
		}
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}

	/**
	 * The break-drop face — the cup getDrops seam over the jug BE: exactly one item, the
	 * registry stack carrying the BE's tank (the write gate keeps the key present iff
	 * content is, so an empty jug drops tag-less and stacks clean at 16).
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tBE instanceof GT6JugBlockEntity tJug)) return super.getDrops(aState, aBuilder);
		List<ItemStack> tDrops = new ArrayList<>(1); // upstream :158 rList = ST.arraylist()
		//? if forge {
		ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tJug.writeItemNBT(tTag);
		tStack.setTag(tTag.isEmpty() ? null : tTag); // the empty jug keeps the tag-less drop shape
		tDrops.add(tStack);
		//?} else {
		/*ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tJug.writeItemNBT(tTag);
		if (!tTag.isEmpty()) net.minecraft.world.item.component.CustomData.set(
				gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tTag); // the opaque small-tank envelope, the payload keys unchanged
		tDrops.add(tStack);
		 *///?}
		return tDrops;
	}
}
