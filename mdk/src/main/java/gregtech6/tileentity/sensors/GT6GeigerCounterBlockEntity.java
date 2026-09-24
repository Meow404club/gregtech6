package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Geiger Counter Sensor (task p37-sensors-3, the pool closure) — the port of
 * MultiTileEntityGeigerCounter.java:40-86. Upstream reads the fission reactor core:
 * the value is the sum of the last-tick per-rod neutron counts (:46-48,
 * {@code oNeutronCounts}), the max the sum of the {@code IItemReactorRod} neutron
 * maximums over the filled slots (:53-66, slot 0 plus 1-3 on the 2x2 core). The reactor
 * system (MultiTileEntityReactorCore 1x1/2x2) is NOT ported — research.p37-gap-scan.s2
 * true-gap ① (the reactor rows never registered here, the census does not carry them;
 * the Fusion Reactor is a different domain with no neutron bookkeeping). DECLARED ARM
 * (the p31-bedrock-ore "drill field face pre-left" precedent, coordinator-approved for
 * this card): the non-reactor answer is the upstream :49/:65 literal 0 — with no reactor
 * core in the port every neighbour is a non-reactor neighbour, so the 0/0 read is 100%
 * upstream-faithful, zero deviation. The reactor arm lights up when the reactor card
 * lands (its study card cites this class as the consumer seam).
 */
public class GT6GeigerCounterBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6GeigerCounterBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6GeigerCounterBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.GEIGERCOUNTER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "geigercounter";
	}

	// DECLARED: the reactor arms (upstream :45-50/:52-66) stay on the s2 true-gap pool —
	// the reactor card extends these two reads with the oNeutronCounts/rod-maximum sums.

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :49 — every non-reactor neighbour, the only kind this port has
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 0; // upstream :65
	}
}
