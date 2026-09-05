/**
 * The identity pins of the shared BlockState Property constants (ADR-P16-2, task
 * p16-blockstates-2111-prop-intern). GTOvenBlock/GTBasicMachineBlock ACTIVE+RUNNING and
 * GTWireBlock/GTFluidPipeBlock CONNECTIONS must all be aliases of the same
 * GTBlockProperties instances: 1.21.x StateHolder looks properties up by identity
 * (Reference2ObjectArrayMap), so a same-named foreign instance throws "Cannot get
 * property ... as it does not exist" on any cross-class read (the 1.21.1 runData crash at
 * GT6BlockStates.addMachine :275-276). On pre-fix main this test is RED on BOTH nodes —
 * 1.20.1 never interned either, it just matched foreign instances by value equality —
 * which is exactly why the pin exists. The name pins guard the datagen contract: the
 * property names are part of the blockstate JSON variant keys, so they must never drift.
 */
package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.wire.GTWireBlock;

public class GTBlockPropertyIdentityTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the property constants need no registries.
		}
	}

	@Test
	public void activeAndRunningAreOneInstanceAcrossOvenAndMachineBlocks() {
		assertSame(GTBlockProperties.ACTIVE, GTOvenBlock.ACTIVE);
		assertSame(GTBlockProperties.ACTIVE, GTBasicMachineBlock.ACTIVE);
		assertSame(GTOvenBlock.ACTIVE, GTBasicMachineBlock.ACTIVE);
		assertSame(GTBlockProperties.RUNNING, GTOvenBlock.RUNNING);
		assertSame(GTBlockProperties.RUNNING, GTBasicMachineBlock.RUNNING);
		assertSame(GTOvenBlock.RUNNING, GTBasicMachineBlock.RUNNING);
	}

	@Test
	public void connectionsIsOneInstanceAcrossWireAndPipeBlocks() {
		assertSame(GTBlockProperties.CONNECTIONS, GTWireBlock.CONNECTIONS);
		assertSame(GTBlockProperties.CONNECTIONS, GTFluidPipeBlock.CONNECTIONS);
		assertSame(GTWireBlock.CONNECTIONS, GTFluidPipeBlock.CONNECTIONS);
	}

	@Test
	public void propertyNamesAreUnchangedForBlockstateJsonStability() {
		assertEquals("active", GTBlockProperties.ACTIVE.getName());
		assertEquals("running", GTBlockProperties.RUNNING.getName());
		assertEquals("connections", GTBlockProperties.CONNECTIONS.getName());
	}
}
