package gregtech6.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Rails;

/**
 * The Road Stripe (task p35-rails-31-blocks) — the upstream {@code BlockRailRoad} single
 * block (gregtech/blocks/BlockRailRoad.java), the unconditional booster: the onMinecartPass
 * (:100-115) accelerates with NO redstone gate, the power-propagation chain is killed
 * (func_150057_a :51-59 returns F, func_150048_a :72-74 NO-OP) and the placement face
 * (:130) only ever lays straight shapes with the bit-8 texture toggle.
 *
 * <p>Modern mapping: extend {@link PoweredRailBlock} with the default state forced
 * {@code POWERED=true} (the vanilla engine then boosts every cart and never brakes — the
 * flag chain {@code instanceof PoweredRailBlock && !isActivatorRail()} reads the state) and
 * the redstone {@code updateState} re-evaluation overridden to a NO-OP (the upstream killed
 * chain: the stripe never listens to neighbours, its POWERED is welded true). The boost
 * curve is the vanilla one — the same declared deviation as {@link GT6BoosterRailBlock}
 * (upstream ×2 vs vanilla +0.06 add; the cap clamps identically).
 *
 * <p>Declared cut: the upstream bit-8 reflector-texture toggle (the crowbar/chisel/shears/
 * knife onToolClick :92-97) rode the IBlockToolable broadcast the modern crowbar does not
 * implement — one texture, no ACTIVE property, the blockstate stays 2 shapes.
 */
public class GT6RoadRailBlock extends PoweredRailBlock {

	public GT6RoadRailBlock(Properties aProperties) {
		super(aProperties, false); // false = NOT an activator rail
		// the super ctor calls the virtual registerDefaultState() -> the override below ran
	}

	@Override
	protected void registerDefaultState() {
		registerDefaultState(stateDefinition.any()
				.setValue(SHAPE, RailShape.NORTH_SOUTH)
				.setValue(POWERED, Boolean.TRUE) // the unconditional boost (upstream has no power gate)
				.setValue(WATERLOGGED, Boolean.FALSE));
	}

	/** The stripe's top speed (the offline pin seam — the loader :39 {@code 0.50F} column). */
	public float railSpeed() {
		return GT6Rails.ROAD_SPEED;
	}

	/** The upstream killed chain — the stripe never re-evaluates redstone, POWERED stays welded true. */
	@Override
	protected void updateState(BlockState aState, Level aLevel, BlockPos aPos, Block aBlock) {
		// NO-OP
	}

	@Override
	public float getRailMaxSpeed(BlockState aState, Level aLevel, BlockPos aPos, AbstractMinecart aCart) {
		return GT6Rails.ladderSpeed(railSpeed(), aState, aLevel, aPos);
	}

}
