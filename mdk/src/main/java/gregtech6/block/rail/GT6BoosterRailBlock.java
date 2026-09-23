package gregtech6.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Rails;

/**
 * The GT6 booster rail (task p35-rails-31-blocks) — the upstream
 * {@code BlockBaseRail(power=T, detector=F)} third. The upstream onMinecartPass
 * (gregapi/block/misc/BlockBaseRail.java:292-322) is the vanilla powered-rail behaviour
 * written out: powered + moving = accelerate, powered + still = the 0.02 start push toward
 * the adjacent solid block, unpowered = the 0.03-stop/0.5-halve brake — ALL of it is the
 * vanilla {@link PoweredRailBlock} engine in modern (AbstractMinecart.moveAlongTrack :442-449
 * brake and :504-540 boost, byte-shape identical to the upstream arms), so the modern class
 * extends the vanilla one and ports ONLY the per-material speed ladder through
 * {@link GT6Rails#ladderSpeed}. The redstone propagation chain (the upstream
 * func_150057/150058 copies, :158-209) IS the vanilla findPoweredRailSignal/updateState
 * machinery here — re-use, not a third port.
 *
 * <p>Declared deviation (one line): the upstream powered-boost multiplies the motion by 2
 * per tick (:298-299) ON TOP of the vanilla 0.06-add curve; the modern class carries only
 * the vanilla curve. Both converge on the per-rail cap ({@code getMaxSpeedWithRail}
 * clamps every tick) — the terminal speed is the ladder value either way, only the
 * ramp-up ticks differ.
 */
public class GT6BoosterRailBlock extends PoweredRailBlock {

	/** The per-material top speed (blocks/tick) — the loader {@code aSpeed} column. */
	private final float mSpeed;

	public GT6BoosterRailBlock(float aSpeed, Properties aProperties) {
		// TRUE = the powered (booster) rail class — the vanilla Blocks.java bytecode registers
		// POWERED_RAIL with (props, true) and ACTIVATOR_RAIL with the one-arg ctor; FALSE
		// classifies the block as an activator rail (no boost, no brake — the RCON race stall)
		super(aProperties, true);
		mSpeed = aSpeed;
	}

	/** The material's top speed (the offline pin seam). */
	public float railSpeed() {
		return mSpeed;
	}

	@Override
	public float getRailMaxSpeed(BlockState aState, Level aLevel, BlockPos aPos, AbstractMinecart aCart) {
		return GT6Rails.ladderSpeed(mSpeed, aState, aLevel, aPos);
	}

}
