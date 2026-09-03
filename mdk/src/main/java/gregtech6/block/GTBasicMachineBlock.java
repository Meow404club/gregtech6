package gregtech6.block;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.network.NetworkHooks;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The basic-machine block (task p7-basicmachine-family ③) — the GTOvenBlock shape with the
 * cover machinery and the wrench-rotation layer cut (the machine family registers no cover
 * surface and the side-configuration stays a carrier concern): BlockState carries the whole
 * visual payload — FACING (the upstream byte mFacing, horizontal subset) plus ACTIVE/RUNNING,
 * the two visual bits of upstream getVisualData :1010-1011 re-expressed as properties
 * (16 datagen blockstate variants per machine, the A-tier furnace idiom). use() is the
 * pure open-GUI chain (upstream onBlockActivated3 :483-486 → openGUI), setPlacedBy mirrors
 * onPlaced (:128-131). No onRemove override (id59).
 *
 * <p>One class, many registered blocks (Shredder/Crusher/Lathe + the Dryer ladder) — each
 * instance resolves its own BlockEntityType through the supplier captured at registration
 * (ADR-P3-1: the ticker asks the live instance every tick). Since task p14-dryer-family a
 * block may carry a {@link MachineRow} — the block-carrier projection of one upstream
 * aRegistry.add line (the GT6Boilers BoilerRow shape): the tier ladder is then data on the
 * placed BlockState's block (the factory reads {@link #row()}), not a tierOf dispatch. The
 * legacy three families stay row-less (the 2-arg constructor) with byte-identical behaviour.
 */
public class GTBasicMachineBlock extends GTEntityBlock {

	/** Facing property (horizontal — the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	// the CS.java:612 connectivity bit values (the port carries no SBIT constant copy — the
	// masks are row data; bit order == the machine-relative side order of GTSideTables).
	public static final byte SBIT_D = 1, SBIT_U = 2, SBIT_L = 4, SBIT_F = 8, SBIT_R = 16, SBIT_B = 32, SBIT_A = 64;

	/**
	 * One registration row — the block-carrier projection of one upstream aRegistry.add
	 * line (task p14-dryer-family, the GT6Boilers BoilerRow record precedent). Every column
	 * of the Dryer rows (Loader_MultiTileEntities.java:1477-1480) has a named field:
	 *
	 * @param path               the gt6 registry path (the blockstate/model/lang key tail)
	 * @param displayName        the upstream row name verbatim, "Dryer (" + aMat.getLocal() + ")"
	 * @param metaId             the upstream MultiTileEntity id (20311-20314), the zero-diff yardstick
	 * @param material           the row material local name (MT.DATA.Heat_T[1..4] = Steel/Invar/
	 *                           Titanium/Tungsten Carbide, MT.java:3689)
	 * @param hardness           the NBT_HARDNESS column (6/4/9/12.5 — NBT_RESISTANCE == hardness)
	 * @param tier               the tier index 0..3 (the TIER_INPUTS selector — the NBT_INPUT
	 *                           column 32/128/512/2048 through the :126 conversion lives in
	 *                           GTMachines.TIER_INPUTS)
	 * @param parallel           the NBT_PARALLEL column (8/16/32/64 — clamped at :130)
	 * @param parallelDuration   the NBT_PARALLEL_DURATION column (T on all four rows)
	 * @param recipes            the NBT_RECIPEMAP column as a supplier — RM.Drying is the
	 *                           volatile GT6RecipeMaps.DRYING, read at BE creation time
	 * @param energyType         the NBT_ENERGY_ACCEPTED column (TD.Energy.HU)
	 * @param texture            the NBT_TEXTURE column ("dryer", all four rows — the ladder
	 *                           shares the T1 front textures, the p8 ruling)
	 * @param energySides        NBT_ENERGY_ACCEPTED_SIDES (SBIT_D — the :151 read ORs SBIT_A)
	 * @param fluidIn            NBT_TANK_SIDE_IN (SBIT_B|SBIT_L — the :143 read ORs SBIT_A)
	 * @param fluidOut           NBT_TANK_SIDE_OUT (SBIT_U — the :144 read ORs SBIT_A)
	 * @param itemIn             NBT_INV_SIDE_IN (SBIT_B|SBIT_L — the :137 read ORs SBIT_A);
	 *                           data-only for now, the item-face gate rides the item-IO pool
	 * @param itemOut            NBT_INV_SIDE_OUT (SBIT_R — the :138 read ORs SBIT_A); data-only, same pool
	 * @param fluidAutoIn        NBT_TANK_SIDE_AUTO_IN (SIDE_BACK); data-only, the auto-IO pool
	 * @param fluidAutoOut       NBT_TANK_SIDE_AUTO_OUT (SIDE_TOP); data-only, the auto-IO pool
	 * @param itemAutoIn         NBT_INV_SIDE_AUTO_IN (SIDE_LEFT); data-only, the auto-IO pool
	 * @param itemAutoOut        NBT_INV_SIDE_AUTO_OUT (SIDE_RIGHT); data-only, the auto-IO pool
	 * @param menu               the port-owned MenuType supplier (null = the menu-less
	 *                           carrier: use() stays inert instead of constructing — the
	 *                           dryer menu registration is the GUI pool card's surface)
	 * @param cheapOverclocking  the NBT_CHEAP_OVERCLOCKING column (T on all four rows — the
	 *                           port overclock loop :773 runs unconditionally, "no config
	 *                           source, always T")
	 */
	public record MachineRow(String path, String displayName, int metaId, String material, float hardness,
			int tier, int parallel, boolean parallelDuration,
			java.util.function.Supplier<gregtech6.recipes.RecipeMap> recipes, gregapi.code.TagData energyType,
			String texture,
			byte energySides, byte fluidIn, byte fluidOut, byte itemIn, byte itemOut,
			byte fluidAutoIn, byte fluidAutoOut, byte itemAutoIn, byte itemAutoOut,
			@Nullable java.util.function.Supplier<net.minecraft.world.inventory.MenuType<gregtech6.gui.machines.GTBasicMachineMenu>> menu,
			boolean cheapOverclocking) {

		/** The block properties (hardness == resistance on every row; the METAL machine sound). */
		public net.minecraft.world.level.block.state.BlockBehaviour.Properties properties() {
			return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
					.strength(hardness, hardness)
					.sound(net.minecraft.world.level.block.SoundType.METAL);
		}
	}

	/**
	 * The carried registration row (null = the legacy row-less families — Shredder/Crusher/
	 * Lathe keep the 2-arg constructor and the tierOf dispatch of the p8 shape).
	 */
	private final MachineRow mRow;

	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTickerType, null);
	}

	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, @Nullable MachineRow aRow) {
		super(aProperties);
		mTickerType = aTickerType;
		mRow = aRow;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(RUNNING, false));
	}

	/** The carried row, or null for the legacy row-less families. */
	@Nullable
	public MachineRow row() {
		return mRow;
	}

	/**
	 * Actually processing (upstream mActive, visual bit 0). Interning note: BooleanProperty
	 * .create caches by name (BooleanProperty.java BY_NAME), so this is the SAME instance as
	 * GTOvenBlock.ACTIVE — the shared datagen helper reads either interchangeably.
	 */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/** Powered / has work (upstream mRunning, visual bit 1). */
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE, RUNNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the same side setFacingFromPlacement writes
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// AbstractFurnaceBlock.use :39-45 shape, upstream onBlockActivated3 :483-486
		// (the cover consumption and the wrench rotation of GTOvenBlock.use are cut —
		// the machine family registers no covers)
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityBasicMachine tMachine && aPlayer instanceof ServerPlayer tServerPlayer && menuBound()) {
			NetworkHooks.openScreen(tServerPlayer, tMachine, aPos); // upstream openGUI
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * The open-GUI gate: the legacy row-less families keep the always-open behaviour; a
	 * row carrier with no MenuType supplier (the dryer until the GUI pool card registers
	 * its {@code gt6:dryer} menu) stays INERT — createMenu would throw the documented
	 * "no MenuType bound" IllegalStateException on every right-click.
	 */
	private boolean menuBound() {
		return mRow == null || mRow.menu() != null;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityBasicMachine tMachine) {
			tMachine.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
