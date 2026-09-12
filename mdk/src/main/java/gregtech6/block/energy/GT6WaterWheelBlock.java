package gregtech6.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Water Wheel block (task p28-c-water-wheel) — the AXIS-axis carrier over the shared
 * BET, the {@link GTAxleBlock} property shape: the wheel spins about one axis and pushes
 * RU packets out of BOTH axis ends (the kTFRUAddon WaterMill axial-output semantics,
 * clean-room re-expression — WaterMill.java:101-105 inserts along mFacing and OPOS[mFacing]
 * with the packet sign flipping across the axis; AGPL code never copied).
 *
 * <p>Placement = the clicked face's axis (the vanilla RotatedPillarBlock
 * {@code getStateForPlacement} form, the axle precedent): a wheel placed against a
 * floor/wall runs along that face's axis, so the axle line attaches with one placement.
 * The RCON path pins it with {@code /setblock gt6:water_wheel[axis=x]}.
 *
 * <p>Properties from the kTFRUAddon registration row semantics (tileEntityInit0.java:112
 * "Water Mill", WoodTreated hardness 1.5): a wooden machine — hardness 1.5 / resistance
 * 6.0 (the port resistance convention), the WOOD sound. {@code noOcclusion} — the wheel
 * sits in open water (the pipe-block convention); the flammability half is a later card
 * (the axle registration note).
 *
 * <p>NO waterlogged state (the declared v1 crop — 1.7.10 had no waterloggable blocks and
 * the upstream wheel stood ADJACENT to the flow, scanning its neighbours; the wheel's own
 * cell is dry). NO use override (no GUI, no tool face — the empty-hand torque readout of
 * WaterMill.java:157-161 is the interaction pool), NO onRemove override (the
 * BaseEntityBlock kill+recreate lesson).
 */
public class GT6WaterWheelBlock extends GTEntityBlock {

	/** The axis property — the shared vanilla instance (the property-interning convention). */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	public GT6WaterWheelBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the vanilla RotatedPillarBlock :48-50 form (the axle precedent)
		return defaultBlockState().setValue(AXIS, aContext.getClickedFace().getAxis());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.WATER_WHEEL_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6WaterWheelBlock> codec() {
		return simpleCodec(GT6WaterWheelBlock::new);
	}
	*///?}

	/**
	 * The wood-machine registration properties (class doc): hardness 1.5 / resistance
	 * 6.0 / WOOD. Named {@code blockProperties} — 21.1's BlockBehaviour already carries a
	 * {@code properties()} instance face a static of the same name would shadow-confuse.
	 */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties() {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.strength(1.5F, 6.0F).sound(SoundType.WOOD).noOcclusion();
	}
}
