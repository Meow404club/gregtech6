package gregtech6.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
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

import javax.annotation.Nullable;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;

/**
 * The Diesel Engine block (task p12-engine-diesel) — the facing cube carrier over the
 * shared BET (ADR-P3-1), the GTCrankBlock facing shape minus the interaction (the engine
 * has NO GUI and NO click arm — the upstream NO_GUI_FUNNEL_TAP_TO_TANK tooltip
 * MultiTileEntityMotorLiquid.java:101; supply is the funnel/tap face, ported as the
 * {@code /gt6engine fuel} channel + the BE funnelFill/tapDrain methods). The block
 * instance carries the tier spec ({@link #spec}, the GTAxleBlock form) — the 1.20.1
 * carrier of the upstream registration NBT NBT_OUTPUT — and the FACING property is the
 * emit side (the front, upstream getDefaultSide SIDE_FRONT :184; the exhaust leaves the
 * back OPOS[mFacing]).
 *
 * <p>NO onRemove override — the BaseEntityBlock kill+recreate lesson (remember id59), the
 * crank/axle form.
 */
public class GTDieselEngineBlock extends GTEntityBlock {

	/** Facing property (horizontal — the emit/front side, the machine-side is this Direction). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The tier row this block mounts (GT6Kinetics.DIESEL_SPECS, the Loader :721-729 row). */
	public final GT6Kinetics.DieselSpec spec;

	public GTDieselEngineBlock(Properties aProperties, GT6Kinetics.DieselSpec aSpec) {
		super(aProperties);
		spec = aSpec;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The composed diesel-engine name (task p20-i18n-compose-rows): the {@link GT6Kinetics#dieselDisplayOf} carrier. */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return GT6Kinetics.dieselDisplayOf(spec);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTDieselEngineBlock> codec() {
		return simpleCodec(aProperties -> new GTDieselEngineBlock(aProperties, GT6Kinetics.DIESEL_SPECS.get(0)));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the emit/front side points away from the
		// player (the crank getStateForPlacement precedent)
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.DIESEL_ENGINE_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, @Nullable LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof net.minecraft.world.entity.player.Player tPlayer
				&& aLevel.getBlockEntity(aPos) instanceof GTDieselEngineBlockEntity tEngine) {
			tEngine.setFacingFromPlacement(tPlayer); // the crank placement-mirror form
		}
	}
}
