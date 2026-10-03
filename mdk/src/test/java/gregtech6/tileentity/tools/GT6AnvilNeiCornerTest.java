/**
 * The anvil NEI legs-band routing pins (task manual-nei-four-family, the
 * GT6KitchenNeiCornerTest shape): the lower-4px band click is the recipe-viewer jump —
 * the CLIENT arm calls {@code openNei()} (the upstream {@code RM.Anvil.openNEI()},
 * MultiTileEntityAnvil.java:272), the SERVER arm keeps swallowing the band (:220 — no
 * chain action for the legs). The upstream anvil arm has NO side gate (any face's foot
 * band jumps — the kitchen pot/bowl SIDES_TOP gate is the pot family's shape, not the
 * anvil's), and the bound is STRICT: {@code aHitY < PX_P[4]} (:271/:220 — exactly 4px
 * falls through to the normal chain BOTH sides). The offline driver: {@code
 * isServerSide()} is true with no level (TileEntityBase01Root.java:188), so the client
 * arm is reached through an overridden fixture — the LIVE click face is the field_test
 * (the JEI/EMI runtime cannot run offline).
 */
package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

class GT6AnvilNeiCornerTest extends gregtech6.tileentity.GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	static BlockEntityType<GT6AnvilBlockEntity> sAnvilType;

	@BeforeAll
	static void buildOfflineFixtures() {
		// the offline holders (the GT6KitchenNeiCornerTest form — the BE-type registry
		// write window reopened by the base class)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6AnvilBlockEntity>[] tHolder = (BlockEntityType<GT6AnvilBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6AnvilBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sAnvilType = tHolder[0];
	}

	/** Captures the viewer jump (the seam the routing pins drive). */
	static class CapturingAnvil extends GT6AnvilBlockEntity {
		int tOpened;
		CapturingAnvil() { super(sAnvilType, POS, Blocks.STONE.defaultBlockState()); }
		@Override protected void openNei() { tOpened++; }
	}

	/** Captures the jump and flags the BE client-side (the activateChain client arm). */
	static class ClientAnvil extends CapturingAnvil {
		@Override public boolean isServerSide() { return false; }
	}

	/** The client arm opens the viewer from the foot band on ANY face (upstream :271 — no side gate). */
	@Test
	void theClientArmRoutesTheLegsBandClickToTheViewer() {
		ClientAnvil tAnvil = new ClientAnvil();
		tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.5F);
		assertEquals(1, tAnvil.tOpened, "top face + the foot band → the viewer jump fires");
		tAnvil.activateChain(null, (byte) 3, ItemStack.EMPTY, 0.5F, 0.0F, 0.5F);
		assertEquals(2, tAnvil.tOpened, "a side face jumps too (the upstream arm has no side gate)");
		tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F);
		assertEquals(2, tAnvil.tOpened, "the working surface never jumps");
	}

	/** The band is the strict upstream 4px variant (aHitY < PX_P[4], MultiTileEntityAnvil.java:271). */
	@Test
	void theLegsBandIsTheStrictUpstream4PxVariant() {
		ClientAnvil tAnvil = new ClientAnvil();
		tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 3.9999F / 16.0F, 0.5F);
		assertEquals(1, tAnvil.tOpened, "just under 4px → opens");
		tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 4.0F / 16.0F, 0.5F);
		assertEquals(1, tAnvil.tOpened, "exactly 4px falls through (the strict < of :271/:220)");
		assertEquals(4.0F / 16.0F, GT6AnvilBlockEntity.LEGS_BOUND, "the band bound is PX_P[4]");
	}

	/** The server arm keeps swallowing the band — no chain action, no jump (upstream :220). */
	@Test
	void theServerArmStillSwallowsTheLegsBand() {
		CapturingAnvil tAnvil = new CapturingAnvil();
		String tReport = tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.05F, 0.5F);
		assertEquals("the anvil legs (the recipe-viewer jump is the client arm)", tReport, "the legs report names the arm split");
		assertEquals(0, tAnvil.tOpened, "the SERVER arm never jumps — the viewer opens client-side only");
	}

	/** activateChain routes its client side through the NEI arm. */
	@Test
	void activateChainRoutesTheClientClickThroughTheArm() {
		ClientAnvil tAnvil = new ClientAnvil();
		assertEquals("client side", tAnvil.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.0F, 0.5F));
		assertEquals(1, tAnvil.tOpened, "the client foot-band click reaches the viewer seam");
		ClientAnvil tSurface = new ClientAnvil();
		tSurface.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F);
		assertEquals(0, tSurface.tOpened, "the client surface click does not jump");
	}
}
