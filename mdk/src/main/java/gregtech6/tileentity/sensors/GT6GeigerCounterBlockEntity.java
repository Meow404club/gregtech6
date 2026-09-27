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
 * the Fusion Reactor is a different domain with no neutron bookkeeping). The reactor
 * ARM landed with task debt-reactor-b-2x2-be: the 2x2 core BE ({@code
 * GT6ReactorCore2x2BlockEntity}) answers both reads below (the p31-bedrock-ore
 * "pre-left field face" declaration paid off); the 1x1 core stays deferred (the
 * upstream default config OFF ruling), so the 2x2 instanceof is the full reactor face.
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

	// REACTOR ARM LIVE (task debt-reactor-b-2x2-be): the 2x2 core BE landed, so the two
	// upstream reads (:45-50/:52-66) are wired below against it. The 1x1 core stays
	// deferred (the upstream default config OFF ruling), so only the 2x2 instanceof
	// answers — every other neighbour keeps the upstream :49/:65 literal 0.

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof gregtech6.tileentity.energy.reactors.GT6ReactorCore2x2BlockEntity tCore) {
			return tCore.neutronSum(); // upstream :46-48 — UT.Code.sum(oNeutronCounts)
		}
		return 0; // upstream :49 — every non-reactor neighbour
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof gregtech6.tileentity.energy.reactors.GT6ReactorCore2x2BlockEntity tCore) {
			return tCore.neutronMaximumSum(); // upstream :53-66 — the per-slot neutron-maximum sum (0 on non-fuel rods, Base:90)
		}
		return 0; // upstream :65
	}
}
