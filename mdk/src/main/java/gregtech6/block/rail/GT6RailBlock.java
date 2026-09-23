package gregtech6.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Rails;

/**
 * The GT6 normal material rail (task p35-rails-31-blocks) — the upstream
 * {@code BlockBaseRail(power=F, detector=F)} third, one block per rail material. The
 * upstream class is a 1.7.10 BlockRailBase subclass (gregapi/block/misc/BlockBaseRail.java)
 * whose shape/curve/slope settlement, waterlogging and drop faces are all the vanilla rail
 * engine — so the modern class extends the vanilla {@link RailBlock} and ports ONLY the two
 * faces vanilla does not carry:
 * <ul>
 * <li>the per-material speed ladder — {@link #getRailMaxSpeed} overrides the per-rail hook
 *     (Forge 1.20.1 IForgeBaseRailBlock.java:62 default, consumed at
 *     AbstractMinecart.patch:240; NeoForge 21.1 IBaseRailBlockExtension.getRailMaxSpeed
 *     default, consumed at AbstractMinecart.getMaxSpeedWithRail — the census erratum: the
 *     "1.20.1 has no per-rail hook" TRAPS edge was proven WRONG on BOTH legs, the ladder is
 *     a direct translation with zero declared deviation) through the shared pure seam
 *     {@link GT6Rails#ladderSpeed};</li>
 * <li>the explosion resistance column — the loader row's per-material value
 *     (Loader_Rails.java:41-50), carried by {@code Properties.strength}.</li>
 * </ul>
 *
 * <p>Declared cut: the upstream crowbar shape-rotation arm (BlockBaseRail.onToolClick :147-153)
 * rides the IBlockToolable broadcast, which the modern crowbar ({@code GTCrowbarItem}) does
 * not implement — its rails arm is the mining half only (the p10-tool-crowbar-mining ruling).
 */
public class GT6RailBlock extends RailBlock {

	/** The per-material top speed (blocks/tick) — the loader {@code aSpeed} column. */
	private final float mSpeed;

	public GT6RailBlock(float aSpeed, Properties aProperties) {
		super(aProperties); // RailBlock = super(false, props): the flexible (curve-making) rail
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
