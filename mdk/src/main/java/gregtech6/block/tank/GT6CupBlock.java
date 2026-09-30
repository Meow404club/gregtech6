package gregtech6.block.tank;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
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
import gregtech6.tileentity.tank.GT6CupBlockEntity;
import gregtech6.tileentity.tank.TileEntityBase10FluidContainerSmall;

/**
 * The Porcelain Cup block carrier (task small-tank-cup) — the block side of the cup row
 * (Loader_MultiTileEntities.java:2094: hardness 0.5 / resistance 6.0, aUtilStone →
 * STONE sound), the {@link GT6CellBlock} carrier pattern over the shared small-tank base
 * {@link TileEntityBase10FluidContainerSmall}. The upstream {@code onlyPlaceableWhenSneaking}
 * (TileEntityBase08FluidContainer:173) and {@code IMTE_IgnorePlayerCollisionWhenPlacing}
 * (MultiTileEntityCup.java:86) flags have no BlockBehaviour seam — declared deviations
 * (the cell-card ruling; the sub-cube collision makes the collision flag near-invisible).
 *
 * <p>Geometry: the upstream collision/selection pool MultiTileEntityCup.java:78-80 —
 * box(PX_P[5], 0, PX_P[5] → PX_N[5], PX_N[11], PX_N[5]) px = the 6x5x6 bowl silhouette.
 * Rendering rides the static per-level elements model ({@link #LIQUID_LEVEL} 0..8, the
 * crucible-bowl/cell form: four 1px walls + the bottom slab over the colored band, the
 * per-level fluid box riding the smeltery_content placeholder — the declared ceiling).
 *
 * <p>{@code use} is the container chain + the drink seam: an EMPTY hand on a drinkable
 * cup drinks first (the barrel tryTankDrink form — a held container keeps the FluidUtil
 * bucket behaviour, the upstream :147 fill-first/:158 drink-later union), otherwise the
 * FluidUtil chain (the GTBarrelBlock.use shape; the liquid-only admission gate lives on
 * the handler, so a gas cell refuses). The upstream tap/funnel face (ITileEntityTapFillable,
 * the top-gated :407-413 pair) has no port consumer — declared cut (the cell-card ruling).
 */
public class GT6CupBlock extends GTEntityBlock {

	/** The upstream collision/selection pool :78-80 — box(5,0,5 → 11,5,11) px (6x5x6, the bowl silhouette). */
	public static final VoxelShape SHAPE = Shapes.box(5.0 / 16.0, 0.0, 5.0 / 16.0, 11.0 / 16.0, 5.0 / 16.0, 11.0 / 16.0);

	/** The fill-level bucket 0..8 (the crucible {@code gt_liquid_level} property form — 9 variants, 9 shared models). */
	public static final IntegerProperty LIQUID_LEVEL = IntegerProperty.create("gt_liquid_level", 0, 8);

	/** The row properties (upstream :2094 NBT_HARDNESS 0.5, NBT_RESISTANCE 6.0, aUtilStone → STONE). */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties cupProperties() {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(0.5F, 6.0F).sound(SoundType.STONE)
				.noOcclusion().isViewBlocking(GT6CupBlock::never);
	}

	/** The sub-cube rider (the cell seam body — the bowl never blocks the view). */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6CupBlock(java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(getStateDefinition().any().setValue(LIQUID_LEVEL, 0));
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GT6CupBlock> codec() {
		return simpleCodec(aProperties -> new GT6CupBlock(mTickerType, aProperties));
	}
	 *///?}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<
			net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {
		aBuilder.add(LIQUID_LEVEL);
	}

	/** The sub-cube bowl silhouette (selection AND collision — the vanilla getShape delegation, the cell form). */
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
		// the drink seam (the barrel form — an empty hand on a drinkable cup drinks; a held
		// container keeps the FluidUtil bucket behaviour below)
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase10FluidContainerSmall tTank
				&& aPlayer.getItemInHand(aHand).isEmpty()) {
			if (tTank.tryTankDrink(aPlayer)) return InteractionResult.CONSUME;
			return InteractionResult.PASS;
		}
		// the container chain (the GTBarrelBlock.use FluidUtil shape — the liquid-only gate
		// lives on the handler, gas refuses)
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
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase10FluidContainerSmall tTank
				&& aPlayer.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			if (tTank.tryTankDrink(aPlayer)) return InteractionResult.CONSUME;
			return InteractionResult.PASS;
		}
		return FluidUtil.interactWithFluidHandler(aPlayer, InteractionHand.MAIN_HAND, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
	 *///?}

	/**
	 * The break-drop face — the GT6CellBlock.getDrops seam over the cup BE: exactly one
	 * item, the registry stack carrying the BE's tank (the upstream writeItemNBT2 item
	 * face; the write gate keeps the key present iff content is, so an empty cup drops
	 * tag-less and stacks clean at 16).
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tBE instanceof GT6CupBlockEntity tCup)) return super.getDrops(aState, aBuilder);
		List<ItemStack> tDrops = new ArrayList<>(1); // upstream :158 rList = ST.arraylist()
		//? if forge {
		ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCup.writeItemNBT(tTag);
		tStack.setTag(tTag.isEmpty() ? null : tTag); // the empty cup keeps the tag-less drop shape
		tDrops.add(tStack);
		//?} else {
		/*ItemStack tStack = new ItemStack(this.asItem());
		CompoundTag tTag = new CompoundTag();
		tCup.writeItemNBT(tTag);
		if (!tTag.isEmpty()) net.minecraft.world.item.component.CustomData.set(
				gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tStack, tTag); // the opaque small-tank envelope, the payload keys unchanged
		tDrops.add(tStack);
		 *///?}
		return tDrops;
	}
}
