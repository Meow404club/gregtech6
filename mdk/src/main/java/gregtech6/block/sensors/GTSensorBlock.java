package gregtech6.block.sensors;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.block.GTEntityBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.sensors.GTSensorBlockEntity;
import gregtech6.tileentity.sensors.GTSensorLogic;
import gregtech6.util.UT6;

/**
 * The sensor block (task p26-sensors-core) — the block-side of the sensor family, the
 * GTOvenBlock A-tier shape: the BlockState carries the display face (FACING, the upstream
 * byte mFacing in the GT6 side order), the probe face (mSecondFacing) is BE-only (it has
 * no model impact upstream either), and the emitted redstone reads THROUGH to the BE
 * ({@link #getSignal} — the GTOvenBlock.bridgeSignal pass-through convention, the vanilla
 * query direction taken as the GT6 side).
 *
 * <p>use() is the interaction stack of the upstream onBlockActivated3 + onToolClick2 pair
 * (MultiTileEntitySensorTE.java:164-200 + MultiTileEntitySensor.java:100-105 +
 * SensorTE:204-250), NO GUI (upstream the NO_GUI_CLICK_TO_INTERACT tooltip, Sensor:91 —
 * the face is pure world interaction):
 * <ul>
 * <li>wrench ({@link GT6ToolActions#WRENCH}) — the wrench-grid side pick
 *     ({@link UT6#getSideWrenching}) sets the display face and folds the probe face to its
 *     opposite (upstream :102);</li>
 * <li>screwdriver ({@link GT6ToolActions#SCREWDRIVER}) — display strip toggles the
 *     hexadecimal display, the keypad buttons resize the averaging window, anything else
 *     cycles the mode (upstream SensorTE:208-240);</li>
 * <li>bare hand on the front face — the 3x3 keypad (threshold modes only, upstream
 *     :164-200). The monkey-wrench and soft-hammer items are not ported — their BE
 *     mutations are reachable through the /gt6sensor second|reset arms (the gearbox
 *     monkey-wrench / card soft-hammer RCON stand-in ruling).</li>
 * </ul>
 */
public class GTSensorBlock extends GTEntityBlock {

	/** The display-face property — full 6-directional (upstream SIDES_VALID wrenching). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GTSensorBlock(Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the GTOvenBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTSensorBlock> codec() {
		return simpleCodec(aProperties -> new GTSensorBlock(mTickerType, aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE (the GTOvenBlock note)
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction (the GTOvenBlock.setPlacedBy shape,
		// upstream onPlaced :128-131 the SIDES_HORIZONTAL pick)
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aLevel.getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor) {
			tSensor.wrenchSetFacing((byte) aState.getValue(FACING).get3DDataValue()); // the facing mirror + the OPOS probe fold
		}
	}

	@Override
	public int getSignal(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection) {
		// upstream Sensor.isProvidingWeakPower2 :247 — weak-only emission (no
		// getDirectSignal override: the vanilla Block default 0 stands for strong power).
		if (aLevel.getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor) {
			return tSensor.redstoneOut((byte) aDirection.get3DDataValue());
		}
		return 0;
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (the GTOvenBlock fork).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(aLevel.getBlockEntity(aPos) instanceof GTSensorBlockEntity tSensor)) {
			return InteractionResult.PASS;
		}
		byte tSide = (byte) aHit.getDirection().get3DDataValue();
		float tHitX = (float) (aHit.getLocation().x - aPos.getX());
		float tHitY = (float) (aHit.getLocation().y - aPos.getY());
		float tHitZ = (float) (aHit.getLocation().z - aPos.getZ());
		ItemStack tHeld = aPlayer.getItemInHand(aHand);

		// the wrench arm (upstream Sensor:102) — the wrench-grid side pick
		if (!tHeld.isEmpty() && tHeld.canPerformAction(GT6ToolActions.WRENCH)) {
			byte tTargetSide = UT6.getSideWrenching(tSide, tHitX, tHitY, tHitZ);
			tSensor.wrenchSetFacing(tTargetSide);
			return InteractionResult.CONSUME;
		}

		// the screwdriver arm (upstream SensorTE:208-240)
		if (!tHeld.isEmpty() && tHeld.canPerformAction(GT6ToolActions.SCREWDRIVER)) {
			float[] tCoords = GTSensorLogic.getFacingCoordsClicked(tSide, tHitX, tHitY, tHitZ);
			if (tSide == tSensor.getFacing() && GTSensorLogic.hasHitDisplay(tCoords[0], tCoords[1])) {
				tSensor.screwdriverToggleHex(); // the display strip = the hex toggle
			} else {
				int[] tButton = GTSensorLogic.keypadHit(tCoords[0], tCoords[1]);
				if (tButton != null) {
					tSensor.screwdriverResizeAveraging(tButton[0], tButton[1]); // the keypad = the averaging window
				} else {
					tSensor.screwdriverCycleMode(); // anything else = the mode cycle
				}
			}
			return InteractionResult.CONSUME;
		}

		// the bare-hand keypad (upstream SensorTE:164-200) — front face, threshold modes
		if (tHeld.isEmpty() && tSide == tSensor.getFacing()) {
			float[] tCoords = GTSensorLogic.getFacingCoordsClicked(tSide, tHitX, tHitY, tHitZ);
			int[] tButton = GTSensorLogic.keypadHit(tCoords[0], tCoords[1]);
			if (tButton != null && tSensor.keypadClick(tButton[0], tButton[1])) {
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}
}
