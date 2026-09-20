package gregtech6.tileentity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Sandwich block carrier (task p32-placeables) — the upstream row "Sandwich"
 * (Loader_MultiTileEntities.java:2032, MTE 32105, the aUtilWool row). The model is the
 * fixed 12/16 layered box (the upstream {@code mSize} variable height pins at the default
 * sandwich — the per-ingredient model band is the declared render cut); the click face is
 * the BITE (the upstream add-ingredient arm is the food-domain pool — see the BE javadoc);
 * the comparator reads the bites (upstream {@code IMTE_GetComparatorInputOverride}, the
 * BE {@code bind4(mSize-1)}). Wool-ish softness (the aUtilWool row), the base hardness
 * 0.25 (MultiTileEntityPlaceable.java:138).
 */
public class GT6SandwichBlock extends GTEntityBlock {

	/** The default-sandwich box: full footprint, 12/16 tall (the layered profile model maps onto it). */
	protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

	public GT6SandwichBlock(Properties aProperties) {
		super(aProperties);
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the GTSensorBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6SandwichBlock> codec() {
		return simpleCodec(GT6SandwichBlock::new);
	}
	*///?}

	/**
	 * The carrier properties — the aUtilWool row numbers (Loader_MultiTileEntities.java:2032
	 * = {@code Block.soundTypeCloth}, the modern WOOL seat), the sandwich hardness 0.25F
	 * (MultiTileEntitySandwich.java:387). NOT named {@code properties()} — the 21.1 static
	 * BlockBehaviour.properties() clash.
	 */
	public static Properties newProperties() {
		return Block.Properties.of().strength(0.25F).sound(SoundType.WOOL);
	}

	/**
	 * The solid-floor door (upstream {@code canPlace}, MultiTileEntitySandwich.java:242-244:
	 * the block below must carry a solid TOP side) — the review-seam restore: the card's
	 * item javadoc had folded this into "the vanilla replaceable walk", which gates a
	 * different face. The placement refuses mid-air/wall seats exactly like upstream.
	 */
	@Override
	public boolean canSurvive(BlockState aState, LevelReader aLevel, BlockPos aPos) {
		return aLevel.getBlockState(aPos.below()).isFaceSturdy(aLevel, aPos.below(), Direction.UP);
	}

	/** The vanilla BaseEntityBlock INVISIBLE default beaten back to MODEL (the GT6BumbleHiveBlock form). */
	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Placeables.SANDWICH_BE.get();
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	/** The bites comparator (upstream MultiTileEntitySandwich :194). */
	@Override
	public boolean hasAnalogOutputSignal(BlockState aState) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState aState, Level aLevel, BlockPos aPos) {
		return aLevel.getBlockEntity(aPos) instanceof GT6SandwichBlockEntity tSandwich ? tSandwich.comparatorValue() : 0;
	}

	/** The placed bite face — one click, one bite; the last bite removes the block with no drops. */
	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GT6SurfaceRockBlock fork precedent).
	 *///?}
		if (aLevel.getBlockEntity(aPos) instanceof GT6SandwichBlockEntity tSandwich && tSandwich.bite(aPlayer)) {
			if (tSandwich.isEmpty()) {
				aLevel.removeBlock(aPos, false);
			} else {
				aLevel.updateNeighbourForOutputSignal(aPos, this); // the comparator re-read face
			}
			return InteractionResult.sidedSuccess(aLevel.isClientSide());
		}
		return InteractionResult.PASS;
	}
}
