package gregtech6.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMachines.CraftingTableRow;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;

/**
 * The Advanced/Charging Crafting Table block (task act-machine → act-matrix) — the
 * GTOvenBlock shape over the facing axis ONLY, now the row-carrier cube (the
 * GT6Hoppers.GT6HopperBlock form): the ACT is zero-energy and player-click driven, so
 * there is no ACTIVE/RUNNING visual payload (the upstream machine carries no state
 * decals — the texture set has no overlay_active/overlay_running layers, the
 * craftingtables/advanced group). FACING is the shared
 * BlockStateProperties.HORIZONTAL_FACING instance (the ADR-P16-2 single-owner alias —
 * the datagen variant builder reads it through the same GTOvenBlock.FACING reference).
 * The row (material + charging kind) rides the instance; the composed display name
 * resolves through {@link GTMachines#displayOf}.
 *
 * <p>Rendering stays the TRANSITIONAL shared model for all 120 rows (task act-matrix
 * declared state): the upstream per-material mRGBa tint and the charging texture family
 * (craftingtables/charging, MultiTileEntityChargingCraftingTable.java:61-80 getTexture2)
 * ride the ⑩B render/GUI card.
 *
	 * <p>use() routes the upstream double-GUI split (task act-dual-gui,
	 * MultiTileEntityAdvancedCraftingTable.java:115-117 verbatim semantics): top face =
	 * crafting GUI 0, front/back = belt/charging GUI 1, other faces no GUI — each GUI
	 * through its own ModularUI factory (the GT6BumbliaryMUI.Factory form, no MenuType).
	 */
public class GTAdvancedCraftingTableBlock extends GTEntityBlock {

	/** Facing property (horizontal — the shared single instance, the ADR-P16-2 alias). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final CraftingTableRow mRow;

	public GTAdvancedCraftingTableBlock(CraftingTableRow aRow, Properties aProperties) {
		super(aProperties);
		mRow = aRow;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GTOvenBlock simpleCodec precedent verbatim; the representative row
	// carries no live config (the GT6HopperBlock codec shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTAdvancedCraftingTableBlock> codec() {
		return simpleCodec(aProperties -> new GTAdvancedCraftingTableBlock(GTMachines.CRAFTING_TABLE_ROWS.get(0), aProperties));
	}
	*///?}

	/** The registration row (the block-carrier config read, the hopper row() seam). */
	public CraftingTableRow row() {
		return mRow;
	}

	/** The composed row name (task i18n-compose-rows: the {@link GTMachines#displayOf} carrier). */
	@Override
	public MutableComponent getName() {
		return GTMachines.displayOf(mRow);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer (the GT6PlacementFacing canon, task
		// singleblock-facing-canon) — the setFacingFromPlacement twin (the oven :88 shape)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mRow.charging() ? GTMachines.CHARGING_CRAFTING_TABLE_BE.get() : GTMachines.ADVANCED_CRAFTING_TABLE_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default is INVISIBLE (the oven :98 note)
	}

	/**
	 * The upstream onBlockActivated3 face table (:115-117 verbatim semantics):
	 * SIDES_TOP → GUI 0 (crafting), the two along-axis vertical faces
	 * (ALONG_AXIS[aSide][mFacing] = front + back) → GUI 1 (the
	 * {@code ContainerCommonDefault(…, 35, 36)} belt/charging GUI), everything else →
	 * no GUI (upstream returns F). Pure so the use() router and the offline pins share
	 * one table.
	 *
	 * @return 0 = crafting GUI, 1 = belt/charging GUI, -1 = no GUI
	 */
	public static int guiIdFor(Direction aHitFace, Direction aFacing) {
		if (aHitFace == Direction.UP) return 0; // SIDES_TOP :115
		if (aHitFace == aFacing || aHitFace == aFacing.getOpposite()) return 1; // ALONG_AXIS :116
		return -1; // :117
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GTOvenBlock fork shape).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// the double-GUI split (:115-117): each GUI opens through its own factory (the
		// GT6BumbliaryMUI.Factory form — the factory identity rides the OpenGuiPacket
		// wire, no MenuType; the BE's IUIHolder face stays the /gt6act open arm's GUI 0).
		int tGui = guiIdFor(aHit.getDirection(), aState.getValue(FACING));
		if (tGui < 0) return InteractionResult.PASS; // upstream :117 return F
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityAdvancedCraftingTable tTable && aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer) {
			if (tGui == 0) gregtech6.menu.act.GTActMenu.Factory.CRAFT.open(tServerPlayer, tTable);
			else gregtech6.menu.act.GTActMenu.Factory.BELT.open(tServerPlayer, tTable);
			return InteractionResult.CONSUME; // upstream openGUI :115/:116
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityAdvancedCraftingTable tTable) {
			tTable.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131 (the oven :210 shape)
		}
	}
}
