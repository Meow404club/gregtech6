package gregtech6.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.block.GTBlockProperties;
import gregtech6.block.GTEntityBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTItemPipes.ItemPipeRow;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;
import gregtech6.util.UT6;
import net.minecraft.world.entity.Entity;
import gregtech6.covers.ICoverableTE;

/**
 * The GT6 item pipe block (task pipe-item spec ⑤) — the block carrier of the row
 * family over the shared BET (ADR-P3-1), the {@link gregtech6.block.pipe.GTFluidPipeBlock}
 * shape with the item-pipe interaction layer:
 * <ul>
 * <li>wrench-key stack right click = the per-face connection toggle (the wrench layer,
 *     {@code GT6ToolActions.isWrenchInteractionKey} — the formal wrench or the
 *     vanilla-hoe substitute, the shared key task wrench-interaction-key wired in;
 *     {@link gregtech6.tileentity.connectors.TileEntityBase09Connector#connect} — the
 *     upstream getFacingTool TOOL_wrench face, MultiTileEntityPipeItem.java:288);</li>
 * <li>shift + wrench key = the monkeywrench face-disable cycle (upstream onToolClick2
 *     TOOL_monkeywrench :128-153, via {@link GTItemPipeBlockEntity#monkeyWrench(byte)} —
 *     refused between two item pipes, :130-133);</li>
 * <li>the target face is {@code UT6.getSideWrenching} over the 0..1 hit offsets
 *     (upstream UT.java:1776-1798, the fluid-pipe block precedent verbatim).</li>
 * </ul>
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask BlockState — the same visual
 * counterpart contract as the fluid pipe (the BE writes it in onConnectionChange). The
 * row ({@link ItemPipeRow}) rides the block instance: stepSize/invSize are read by the
 * BE from its block (the 1.20.1 counterpart of the registration NBT pair NBT_PIPESIZE/
 * NBT_INV_SIZE, MultiTileEntityPipeItem.java:94 + Loader :1823-1825). NO onRemove
 * override (the id59 red line).
 */
public class GTItemPipeBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final IntegerProperty CONNECTIONS = GTBlockProperties.CONNECTIONS;

	private final ItemPipeRow mRow;

	public GTItemPipeBlock(ItemPipeRow aRow, Properties aProperties) {
		super(aProperties);
		mRow = aRow;
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config (GTFluidPipeBlock fork verbatim).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTItemPipeBlock> codec() {
		return simpleCodec(aProperties -> new GTItemPipeBlock(GTItemPipes.ROWS.get(0), aProperties));
	}
	*///?}

	/** The row this block carries (stepSize/invSize/metaId/display). */
	public ItemPipeRow row() {
		return mRow;
	}

	@Override
	protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
		return GTItemPipes.ITEM_PIPE_BE.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(CONNECTIONS);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/**
	 * The connection-aware thin envelope (task rod-render-pool) — the
	 * {@link GTFluidPipeBlock#getShape} form over the item-pipe diameters (NBT_DIAMETER
	 * PX_P[8/12/16], MultiTileEntityPipeItem.java:76-82 — the restrictive twins share
	 * their base diameter, the upstream :79-82 rows carry the same PX_P column).
	 *
	 * <p>Under the wrench-interaction key the outline is the FULL cube (task
	 * wrench-pipe-fullcube-targeting — the {@link GTFluidPipeBlock#getShape} wrench arm
	 * verbatim, same seam/deviation/collision-inertness contract: the item pipe is
	 * clickable through the key, GTItemPipeBlock.use, so it must be AIMABLE through it
	 * too).
	 */
	@Override
	public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel,
			BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (GT6ToolActions.isWrenchInteractionContext(aContext)) return net.minecraft.world.phys.shapes.Shapes.block();
		int tMask = aState.getValue(CONNECTIONS);
		double tInset = (16 - mRow.variant().diameterPx) / 32.0;
		return net.minecraft.world.phys.shapes.Shapes.box(
			(tMask & (1 << net.minecraft.core.Direction.WEST.get3DDataValue())) != 0 ? 0 : tInset,
			(tMask & (1 << net.minecraft.core.Direction.DOWN.get3DDataValue())) != 0 ? 0 : tInset,
			(tMask & (1 << net.minecraft.core.Direction.NORTH.get3DDataValue())) != 0 ? 0 : tInset,
			(tMask & (1 << net.minecraft.core.Direction.EAST.get3DDataValue())) != 0 ? 1 : 1 - tInset,
			(tMask & (1 << net.minecraft.core.Direction.UP.get3DDataValue())) != 0 ? 1 : 1 - tInset,
			(tMask & (1 << net.minecraft.core.Direction.SOUTH.get3DDataValue())) != 0 ? 1 : 1 - tInset);
	}

	/** The composed row name (the BoilerTankBlock.getName posture — the item stack name delegates here). */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return mRow.displayName();
	}

	/**
	 * The row's ore-dict material off the carried {@link ItemPipeRow} (the loader line's
	 * MT argument; upstream registers NBT_MATERIAL + NBT_COLOR = getRGBInt(fRGBaSolid),
	 * MultiTileEntityPipeItem.java:76-82). The tex-pipe-textures tint dispatch seam
	 * (the {@code GTBasicMachineBlock.materialOf} shape): null for any other block, so
	 * the {@code GTMachinePaintTint.tintMaterialOf} gate keeps every foreign domain
	 * byte-identical.
	 */
	public static gregapi.oredict.OreDictMaterial materialOf(Block aBlock) {
		return aBlock instanceof GTItemPipeBlock tPipe ? tPipe.mRow.material().oreDictMaterial() : null;
	}

	// ---------------------------------------------------------------------------
	// the two-layer wrench interaction (the GTFluidPipeBlock.use shape)
	// ---------------------------------------------------------------------------

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (the GTFluidPipeBlock fork verbatim)
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		if (!GT6ToolActions.isWrenchInteractionKey(tStack)) return InteractionResult.PASS; // the shared wrench key (task wrench-interaction-key — same judgment as the fluid pipe and the grid overlay)
		if (aLevel.isClientSide) return InteractionResult.CONSUME; // claim the interaction, the BE executes server-side

		BlockEntity tTile = aLevel.getBlockEntity(aPos);
		if (!(tTile instanceof GTItemPipeBlockEntity tPipe)) return InteractionResult.PASS;
		byte tClickedSide = (byte)aHit.getDirection().get3DDataValue();
		// the 0..1 in-face offsets — upstream passes the raw hit fractions of the clicked face
		float tHitX = (float)(aHit.getLocation().x - aPos.getX());
		float tHitY = (float)(aHit.getLocation().y - aPos.getY());
		float tHitZ = (float)(aHit.getLocation().z - aPos.getZ());
		byte tTargetSide = UT6.getSideWrenching(tClickedSide, tHitX, tHitY, tHitZ);

		if (aPlayer.isShiftKeyDown()) {
			tPipe.monkeyWrench(tTargetSide); // shift = the monkeywrench face-disable cycle (:128-153)
		} else {
			tPipe.toggleConnection(tTargetSide); // plain = the wrench connection toggle (getFacingTool :288)
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		super.stepOn(aLevel, aPos, aState, aEntity);
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) tCoverable.onCoverWalkOver(aEntity); // MultiTileEntityBlock.java:306 -> 06Covers:428 (covers-crafting-asphalt)
	}
}
