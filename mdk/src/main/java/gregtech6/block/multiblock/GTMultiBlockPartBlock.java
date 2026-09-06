package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;

/**
 * The multiblock part block — the 1.20.1 body of the GT6 multiblock part MTE family (the
 * "one part TE class, many part blocks" shape: the shared {@link GTMultiBlocks#MULTIBLOCK_PART_BE},
 * one Block instance per part type, ADR-P3-1).
 *
 * <p>Extends {@link BaseEntityBlock} DIRECTLY, not GTEntityBlock: the part BE is the notick
 * chain (MultiBlockPartBlockEntity extends TileEntityBase01Root, no dispatcher), while
 * GTEntityBlock hard-types its ticker to TileEntityBase03TicksAndSync. BaseEntityBlock's
 * default {@code getTicker} is null — exactly the notick-chain equivalent, no override needed.
 *
 * <p>Change propagation (upstream IMTE_OnBlockAdded :187-197 / IMTE_BreakBlock :176-184 →
 * controller.onStructureChange, task card ⑤): the 1.20.1 hooks are {@link #onPlace} and
 * {@link #playerWillDestroy}. NEVER onRemove — the remembered BaseEntityBlock trap (a
 * self-written onRemove removes the BE on same-block state changes and drives setBlock-based
 * state writes into a kill+recreate loop; LevelChunk.setBlockState:292 CHECK branch keeps the
 * BE across flips). The part has no state properties anyway.
 *
 * <p>onPlace ordering note: LevelChunk.setBlockState calls onPlace (:282) BEFORE the new
 * block entity is attached (:286-292) — the propagation here touches only NEIGHBOUR cells
 * (whose BEs exist), never this block's own.
 */
public class GTMultiBlockPartBlock extends BaseEntityBlock {

	/** The carried part row (task p13-large-boiler record; null = the rows without a composed name — the coke-oven bricks). */
	@Nullable
	private final gregtech6.registry.GTMultiBlocks.MultiblockPartRow mRow;

	public GTMultiBlockPartBlock(Properties aProperties) {
		super(aProperties);
		this.mRow = null;
	}

	/** The wall-carrier form (task p20-i18n-compose-rows): the row feeds the composed Dense Wall name. */
	public GTMultiBlockPartBlock(Properties aProperties, gregtech6.registry.GTMultiBlocks.MultiblockPartRow aRow) {
		super(aProperties);
		this.mRow = aRow;
	}

	/**
	 * The composed Dense Wall name (task p20-i18n-compose-rows): the row-carried form fills
	 * the {@code gt6.row.dense_wall.display} template over the gt6.row.mat small unit; the
	 * row-less forms (the bricks) keep the vanilla atomic-key lookup.
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		if (mRow == null) return super.getName();
		return net.minecraft.network.chat.Component.translatable(gregtech6.registry.GTMultiBlocks.DENSE_WALL_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(gregtech6.registry.GTMultiBlocks.wallMatUnitKeyOf(mRow)));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTMultiBlockPartBlock> codec() {
		return simpleCodec(aProperties -> new GTMultiBlockPartBlock(aProperties));
	}
	*///?}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default is INVISIBLE (BER assumption)
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		// BlockEntityType.create -> factory (BlockEntityType.java:288-290)
		return GTMultiBlocks.MULTIBLOCK_PART_BE.get().create(aPos, aState);
	}

	/** Upstream onBlockAdded :187-197 — flag every adjacent part's controller and every adjacent controller. */
	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aMoving);
		if (aLevel.isClientSide() || aState.is(aOldState.getBlock())) return; // same-block flip guard (belt & braces)
		for (Direction tSide : Direction.values()) {
			BlockEntity tNeighbor = aLevel.getBlockEntity(aPos.relative(tSide));
			if (tNeighbor instanceof MultiBlockPartBlockEntity tPart) {
				ITileEntityMultiBlockController tController = tPart.getTarget(false);
				if (tController != null) tController.onStructureChange();
			} else if (tNeighbor instanceof ITileEntityMultiBlockController tController) {
				tController.onStructureChange();
			}
		}
	}

	/** Upstream breakBlock :176-184 — release the claim, then force the controller recheck. */
	//? if forge {
	@Override
	public void playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
		super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
		if (aLevel.isClientSide()) return;
	//?} else {
	/*@Override
	public BlockState playerWillDestroy(Level aLevel, BlockPos aPos, BlockState aState, Player aPlayer) {
	//21.1: BlockBehaviour.playerWillDestroy returns BlockState (void on 1.20.1, javap Block
	//21.1.249) — the override return type drifts; the tail hands back the state untouched.
		super.playerWillDestroy(aLevel, aPos, aState, aPlayer);
		if (aLevel.isClientSide()) return aState;
	*///?}
		if (aLevel.getBlockEntity(aPos) instanceof MultiBlockPartBlockEntity tPart) {
			ITileEntityMultiBlockController tTarget = tPart.getTarget(false);
			if (tTarget != null) {
				tPart.clearTarget();
				tTarget.onStructureChange();
			}
		}
		//? if neoforge {
		/*return aState;
		*///?}
	}
}
