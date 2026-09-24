package gregtech6.block.portals;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.portals.GTMiniPortalBlockEntity;
import gregtech6.tileentity.portals.GTMiniPortalEndBlockEntity;
import gregtech6.tileentity.portals.GTMiniPortalNetherBlockEntity;
import net.minecraft.world.entity.Entity;
import gregtech6.covers.ICoverableTE;

/**
 * The miniature portal block (task p35-portals-mini-nether-end) — the GTSensorBlock
 * shape: the blockstate carries the ACTIVE visual (the upstream 13-pass frame render
 * collapsed to a frame/portal cube swap — declared cosmetic deviation), the redstone and
 * comparator emission read THROUGH to the BE (the GTOvenBlock bridge convention), and
 * use() carries the two upstream activation faces:
 * <ul>
 * <li>Nether (upstream onToolClick :116-128, TOOL_igniter): flint-and-steel or the GT6
 *     flint-and-tinder toggles the portal — the TOOL_igniter item stand-in (no IGNITER
 *     ToolAction exists in this port; the /gt6portal command is the console stand-in);</li>
 * <li>End (upstream onBlockActivated2 :112-123): a right click holding an Ender Eye
 *     activates and consumes one (creative exempt).</li>
 * </ul>
 */
public class GTMiniPortalBlock extends GTEntityBlock {

	/** The visual/relay state (the mActive mirror — written by the BE, read by the model swap). */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GTMiniPortalBlock(Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the GTOvenBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTMiniPortalBlock> codec() {
		return simpleCodec(aProperties -> new GTMiniPortalBlock(mTickerType, aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(ACTIVE);
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE (the GTOvenBlock note)
	}

	/** Upstream isProvidingWeakPower :346-348 — weak-only emission (no getDirectSignal override), the query side folds through OPOS. */
	@Override
	public int getSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		if (aLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal && aDirection.get3DDataValue() < 6) {
			return tPortal.redstoneOut((byte) aDirection.get3DDataValue());
		}
		return 0;
	}

	/**
	 * Upstream getComparatorInputOverride :341-343 was per-face; the vanilla analog bridge
	 * ({@code getAnalogOutputSignal(BlockState, BlockGetter, BlockPos)}) is direction-less,
	 * so the fold is the maximum over the six faces (declared deviation — single-comparator
	 * rigs read identically; ponytail: per-face resolution needs a comparator-side probe
	 * vanilla does not offer).
	 */
	@Override
	public int getAnalogOutputSignal(BlockState aState, Level aLevel, BlockPos aPos) {
		if (aLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal) {
			int tMax = 0;
			for (byte tSide = 0; tSide < 6; tSide++) tMax = Math.max(tMax, tPortal.comparatorOut(tSide));
			return tMax;
		}
		return 0;
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (the GTSensorBlock fork).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(aLevel.getBlockEntity(aPos) instanceof GTMiniPortalBlockEntity tPortal)) {
			return InteractionResult.PASS;
		}
		ItemStack tHeld = aPlayer.getItemInHand(aHand);

		// the End arm (upstream End.java:112-123): the Ender Eye activation
		if (tPortal instanceof GTMiniPortalEndBlockEntity tEnd && tEnd.activateWithEye(aPlayer, aHand)) {
			return InteractionResult.CONSUME;
		}

		// the Nether arm (upstream Nether.java:116-128): the igniter toggle — vanilla
		// flint-and-steel and the GT6 tinder are the TOOL_igniter stand-in items
		if (tPortal instanceof GTMiniPortalNetherBlockEntity tNether
				&& (tHeld.is(Items.FLINT_AND_STEEL) || tHeld.is(GT6Tools.FLINT_AND_TINDER.get()))) {
			tNether.igniteToggle();
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		super.stepOn(aLevel, aPos, aState, aEntity);
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) tCoverable.onCoverWalkOver(aEntity); // MultiTileEntityBlock.java:306 -> 06Covers:428 (p37-covers-crafting-asphalt)
	}
}
