package gregtech6.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Rails;

/**
 * The GT6 detector rail (task p35-rails-31-blocks) — the upstream
 * {@code BlockBaseRail(power=F, detector=T)} third. The upstream detection face
 * (BlockBaseRail.java:214-254: the AABB cart scan, the meta-8 powered bit, the weak/strong
 * power split, the 20-tick reschedule) and comparator face (:262-275: the command-block
 * cart's success count, the inventory carts' redstone level) ARE the vanilla
 * {@link DetectorRailBlock} engine in modern — re-use, not a third port. The modern class
 * carries ONLY the per-material speed ladder through {@link GT6Rails#ladderSpeed} (the
 * detector rides the same material speeds as its normal/booster siblings, the loader rows
 * :63-72) plus the resistance column via {@code Properties.strength}.
 */
public class GT6DetectorRailBlock extends DetectorRailBlock {

	/** The per-material top speed (blocks/tick) — the loader {@code aSpeed} column. */
	private final float mSpeed;

	public GT6DetectorRailBlock(float aSpeed, Properties aProperties) {
		super(aProperties);
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
