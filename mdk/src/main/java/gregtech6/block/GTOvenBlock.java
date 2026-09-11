package gregtech6.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.ToolActions;
//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

import gregtech6.covers.ICoverableTE;
import gregtech6.registry.GTMachines;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.util.UT6;

/**
 * The Oven block (task p4-machine-oven, spec 7/8) — the block-side of the first real
 * processing machine. BlockState carries the machine's whole visual payload: FACING
 * (upstream byte mFacing, the GT6 side order == Direction horizontal subset) plus
 * ACTIVE/RUNNING, the two visual bits of upstream getVisualData :1010-1011
 * ({@code (mActive?1:0)|(mRunning?2:0)}) re-expressed as properties (A-tier rendering:
 * 16 datagen blockstate variants (4 horizontal facings), no dynamic model yet).
 *
 * <p>use() is the chest/open-GUI chain (GTExampleChestBlock.use, upstream
 * onBlockActivated3 :483-486 → openGUI) without the blocked-above guard (a furnace-style
 * machine has no lid). Between the cover consumption and the GUI open sits the GTCEu
 * onWrenchClick :509-525 rotation layer (task p6-oven-rotation): a hoe held with shift
 * rotates the front through the grid edge cells (shift never opens the GUI), everything
 * else opens the GUI unchanged. setPlacedBy mirrors onPlaced (:128-131): the BE's facing
 * becomes the player's horizontal look direction, double-written NBT + BlockState.
 */
public class GTOvenBlock extends GTEntityBlock {

	/** Facing property (horizontal — the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Actually processing (upstream mActive, visual bit 0) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final BooleanProperty ACTIVE = GTBlockProperties.ACTIVE;

	/** Powered / has work (upstream mRunning, visual bit 1) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final BooleanProperty RUNNING = GTBlockProperties.RUNNING;

	/**
	 * The row index of this block in the Oven Heat_T ladder (task p27-oven-heat-t-ladder,
	 * 0 = T1 .. 3 = T4): the tier rows are compile-time constants upstream
	 * (NBT_INPUT/NBT_HARDNESS, Loader_MultiTileEntities.java:1288-1291), so the block
	 * identity IS the config selector — the GTBasicMachineBlock tierOf ruling carried as
	 * block data (the MachineRow "block-carrier projection" shape, minus the record: the
	 * oven's menu/recipe/energy columns are family constants of {@code TileEntityOven}).
	 */
	private final int mTier;

	/**
	 * The composed-name carrier of the ladder rows (task p27-oven-heat-t-ladder): the
	 * GTBasicMachineBlock tier-ladder form (:137-148) verbatim — a pre-composed
	 * {@code "Oven (<material word>)"} supplier the registrations supply, resolved through
	 * {@link #getName()} (the single compose point the GTComposedNameItem delegation and
	 * the TileEntityOven GUI title both read).
	 */
	@javax.annotation.Nullable
	private final java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> mComposedName;

	public GTOvenBlock(Properties aProperties) {
		this(aProperties, 0, null);
	}

	/** The tier-ladder form (task p27-oven-heat-t-ladder): a row index plus the composed name. */
	public GTOvenBlock(Properties aProperties, int aTier,
			@javax.annotation.Nullable java.util.function.Supplier<net.minecraft.network.chat.MutableComponent> aComposedName) {
		super(aProperties);
		mTier = aTier;
		mComposedName = aComposedName;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(RUNNING, false));
	}

	/**
	 * The tier index of the ladder block in {@code aState} — 0 for a non-oven block (the
	 * offline BRICKS-state fixtures keep the T1 field defaults, the zero-regression guard).
	 */
	public static int tier(BlockState aState) {
		return aState.getBlock() instanceof GTOvenBlock tOven ? tOven.mTier : 0;
	}

	/**
	 * The ladder row material (task p27-machine-material-tint-fidelity): the Heat_T[1..4]
	 * column of {@link GTMachines#OVEN_ROWS} at this block's tier — the upstream
	 * NBT_MATERIAL (Loader_MultiTileEntities.java:1288-1291,
	 * {@code aMat = MT.DATA.Heat_T[1..4]}) the 1.7.10 registration derives the render
	 * colour from (MultiTileEntityClassContainer.java:51). The lazy row supplier resolves
	 * against {@code MT.init()} at call time (render/unpaint — never class-load).
	 */
	@javax.annotation.Nullable
	public gregapi.oredict.OreDictMaterial material() {
		if (mTier < 0 || mTier >= GTMachines.OVEN_ROWS.size()) return null;
		return GTMachines.OVEN_ROWS.get(mTier).material().get();
	}

	/**
	 * The composed name (task p27-oven-heat-t-ladder): a ladder row hands back its
	 * pre-composed {@code "Oven (<material word>)"} supplier, the legacy single-oven shape
	 * keeps the vanilla atomic-key lookup.
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		if (mComposedName != null) return mComposedName.get();
		return super.getName();
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config (tier 0, no composed
	// name — the GTBasicMachineBlock codec note); world save/load never runs through
	// this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTOvenBlock> codec() {
		return simpleCodec(aProperties -> new GTOvenBlock(aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE, RUNNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the same side setFacingFromPlacement writes
		// (chest precedent onPlaced :128-131, UT.Code.getSideForPlayerPlacing SIDES_HORIZONTAL)
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMachines.OVEN_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	@Override
	public int getSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		// upstream 04Covers :427-431 isProvidingWeakPower — the vanilla query direction is
		// the side the RECEIVER sees the machine from; the emission face is UT6.OPOS of it
		// (folded inside the ICoverableTE exit). super.getSignal (the vanilla Block
		// default, 0 for the oven) rides in as the machine-default argument the cover may
		// override (task p9-redstone-hooks; the P6/P8 multiblock bases are deliberately
		// NOT bridged — ADR FORBIDDEN ④, the multiblock per-face dispatch is unresearched).
		return bridgeSignal(aLevel, aPos, aDirection, super.getSignal(aState, aLevel, aPos, aDirection), false);
	}

	@Override
	public int getDirectSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		// upstream :433-438 isProvidingStrongPower — same bridge shape as getSignal.
		return bridgeSignal(aLevel, aPos, aDirection, super.getDirectSignal(aState, aLevel, aPos, aDirection), true);
	}

	@Override
	public void neighborChanged(BlockState aState, Level aLevel, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aIsMoving) {
		// upstream TileEntityBase06Covers.java:382 onNeighborBlockChange — the host-side
		// half of the cover dispatch (the 1.20.1 counterpart of the 1.7.10
		// onNeighborBlockChange notification is this Block hook, BlockBehaviour.java:138).
		// Every cover on the block receives onBlockUpdate — the seam the redstone
		// conductor OUT face refreshes its cached value through (task
		// p10-cover-conductor-redstone; the seam existed in CoverData.onBlockUpdate with
		// zero callers until this override). super keeps the vanilla debug-packet default.
		super.neighborChanged(aState, aLevel, aPos, aBlock, aFromPos, aIsMoving);
		dispatchCoverBlockUpdate(aLevel, aPos);
	}

	/**
	 * The :382 dispatch body — every cover on a coverable BE at {@code aPos} receives
	 * {@code onBlockUpdate}. Static so the offline truth tables can drive it without
	 * constructing the block (the {@link #bridgeSignal} precedent — the live
	 * notification path is covered by the RCON chain). Covers gate their own writes
	 * server-side (the emitter :151 shape), so the dispatch stays unguarded like
	 * upstream.
	 */
	public static void dispatchCoverBlockUpdate(BlockGetter aLevel, BlockPos aPos) {
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable && tCoverable.hasCovers()) {
			tCoverable.getCovers().onBlockUpdate();
		}
	}

	/**
	 * The :427-438 bridge body, shared by both exits. The {@code aMachineDefault} is the
	 * vanilla Block emission (0 for the oven); a coverable BE at {@code aPos} may
	 * override it through the ICoverableTE exit. Static so the offline truth tables can
	 * drive it without constructing the block (the registry-freeze precedent,
	 * TileEntityOvenFacingTest) — the live dispatch stays covered by the RCON chain.
	 */
	public static int bridgeSignal(BlockGetter aLevel, BlockPos aPos, Direction aDirection, int aMachineDefault, boolean aStrong) {
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) {
			byte tSide = (byte) aDirection.get3DDataValue();
			return aStrong ? tCoverable.getRedstoneOutStrong(tSide, aMachineDefault) : tCoverable.getRedstoneOutWeak(tSide, aMachineDefault);
		}
		return aMachineDefault;
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249); the game loop drives MAIN_HAND first.
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// AbstractFurnaceBlock.use :39-45 shape, upstream onBlockActivated3 :483-486
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityOven tOven && aPlayer instanceof ServerPlayer tServerPlayer) {
			// upstream onBlockActivated2 :106-133 — the cover machinery consumes the click first
			// (the covered-face intercepts, then the attachCoversFirst install branch :118-123);
			// false falls through to the GUI open (upstream onBlockActivated3 = :124)
			if (tOven.onCoverUse(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()), (float) (aHit.getLocation().z - aPos.getZ()))) {
				return InteractionResult.CONSUME;
			}
			// task p6-oven-rotation — the GTCEu onWrenchClick :509-525 layer, between the
			// cover consumption above and the GUI open below (upstream onBlockActivated3
			// :124): a hoe held with shift rotates the machine through the grid edge cells.
			// The side resolution is the same UT6.getSideWrenching pick the wrench grid
			// overlay draws (GTFluidPipeBlock.use:109-114 shape — not the bare hit
			// direction), so the cell clicked is constructively the cell shown. A valid
			// side rotates; an invalid one (vertical / the front / SIDE_INVALID) is the
			// GTCEu FAIL :518-519 no-op — shift never opens the GUI; without shift the
			// GUI path is unchanged (PASS :524).
			ItemStack tHeld = aPlayer.getItemInHand(aHand);
			if (!tHeld.isEmpty() && tHeld.canPerformAction(ToolActions.HOE_DIG) && aPlayer.isShiftKeyDown()) {
				tOven.setFrontFacing(UT6.getSideWrenching((byte) aHit.getDirection().get3DDataValue(),
						(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
						(float) (aHit.getLocation().z - aPos.getZ())));
				return InteractionResult.CONSUME;
			}
			//? if forge {
			NetworkHooks.openScreen(tServerPlayer, tOven, aPos); // upstream openGUI
			//?} else {
			/*tServerPlayer.openMenu(tOven, tBuf -> tBuf.writeBlockPos(aPos)); // 21.1: NetworkHooks deleted — ServerPlayer.openMenu(MenuProvider, buf) carries the pos payload (the command-file precedent)
			*///?}
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityOven tOven) {
			tOven.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
