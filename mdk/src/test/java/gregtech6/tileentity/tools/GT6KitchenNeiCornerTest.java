/**
 * The kitchen NEI corner routing pins (task kitchen-nei-corner-jump): the top-face
 * corner-quadrant click is the recipe-viewer jump — the CLIENT arm calls
 * {@code openNei()} (the upstream {@code mRecipes.openNEI()}, MixingBowl.java:284 /
 * Juicer.java:170), the SERVER arm keeps swallowing the quadrant (:190/:210 — no
 * processing round for the corner). The quadrant bounds follow the upstream pixel
 * tables: the pot/bowl pair rides {@code PX_P[2]} = 2px (:283, BathingPot.java:262),
 * the Juicer the {@code PX_P[4]} = 4px variant (MultiTileEntityJuicer.java:164). The
 * offline driver: {@code isServerSide()} is true with no level (TileEntityBase01Root
 * .java:188), so the client arm is reached through an overridden fixture — the LIVE
 * click face is the field_test (the JEI/EMI runtime cannot run offline).
 */
package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

class GT6KitchenNeiCornerTest extends gregtech6.tileentity.GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	static BlockEntityType<GT6MixingBowlBlockEntity> sBowlType;
	static BlockEntityType<GT6JuicerBlockEntity> sJuicerType;

	@org.junit.jupiter.api.BeforeEach
	void armTheMaps() {
		// activateChain opens with ensureTanks() before the corner check — arm the map
		// generation the tank arrays read (the GT6KitchenBlockEntityTest convention)
		gregtech6.recipes.GT6RecipeMaps.init();
		gregtech6.recipes.GT6RecipeMaps.reset();
		gregtech6.recipes.GT6RecipeMaps.init();
	}

	@org.junit.jupiter.api.AfterEach
	void dropTheMaps() {
		gregtech6.recipes.GT6RecipeMaps.reset();
	}

	@BeforeAll
	static void buildOfflineFixtures() {
		// the offline holders (the GT6KitchenBlockEntityTest form — the BE-type registry
		// write window reopened by the base class)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MixingBowlBlockEntity>[] tBowlHolder = (BlockEntityType<GT6MixingBowlBlockEntity>[]) new BlockEntityType<?>[1];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6JuicerBlockEntity>[] tJuicerHolder = (BlockEntityType<GT6JuicerBlockEntity>[]) new BlockEntityType<?>[1];
		tBowlHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MixingBowlBlockEntity(tBowlHolder[0], aPos, aState), Blocks.STONE).build(null);
		tJuicerHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6JuicerBlockEntity(tJuicerHolder[0], aPos, aState), Blocks.STONE).build(null);
		sBowlType = tBowlHolder[0];
		sJuicerType = tJuicerHolder[0];
	}

	/** Captures the viewer jump (the seam the routing pins drive). */
	static class CapturingBowl extends GT6MixingBowlBlockEntity {
		int tOpened;
		CapturingBowl() { super(sBowlType, POS, Blocks.STONE.defaultBlockState()); }
		@Override protected void openNei() { tOpened++; }
	}

	/** Captures the jump and flags the BE client-side (the activateChain client arm). */
	static class ClientBowl extends CapturingBowl {
		@Override public boolean isServerSide() { return false; }
	}

	static class CapturingJuicer extends GT6JuicerBlockEntity {
		int tOpened;
		CapturingJuicer() { super(sJuicerType, POS, Blocks.STONE.defaultBlockState()); }
		@Override protected void openNei() { tOpened++; }
	}

	/** The client arm opens the viewer ONLY for the top-face corner quadrant. */
	@Test
	void theClientArmRoutesTheTopCornerClickToTheViewer() {
		CapturingBowl tBowl = new CapturingBowl();
		tBowl.clientNeiArm((byte) 1, 0.0F, 0.0F);
		assertEquals(1, tBowl.tOpened, "top face + corner → the viewer jump fires");
		tBowl.clientNeiArm((byte) 3, 0.0F, 0.0F);
		assertEquals(1, tBowl.tOpened, "a side face never jumps (upstream gates on SIDES_TOP)");
		tBowl.clientNeiArm((byte) 1, 0.5F, 0.5F);
		assertEquals(1, tBowl.tOpened, "the centre quadrant never jumps");
	}

	/** The bowl/pot quadrant is the upstream 2px variant (PX_P[2]), inclusive bound. */
	@Test
	void theBowlCornerQuadrantIsThe2PxVariant() {
		CapturingBowl tBowl = new CapturingBowl();
		tBowl.clientNeiArm((byte) 1, 2.0F / 16.0F, 0.05F);
		assertEquals(1, tBowl.tOpened, "2px = PX_P[2] inclusive → opens");
		tBowl.clientNeiArm((byte) 1, 2.0001F / 16.0F, 0.05F);
		assertEquals(1, tBowl.tOpened, "one tenth of a pixel past the bound is out");
		assertEquals(2.0F / 16.0F, tBowl.cornerBound(), "the bowl corner bound is PX_P[2]");
		assertEquals(14.0F / 16.0F, tBowl.innerBound(), "the bowl centre bound is PX_N[2] (MixingBowl.java:238)");
	}

	/** The Juicer quadrant is the 4px variant (PX_P[4]) — the family's other corner table. */
	@Test
	void theJuicerCornerQuadrantIsThe4PxVariant() {
		CapturingJuicer tJuicer = new CapturingJuicer();
		tJuicer.clientNeiArm((byte) 1, 4.0F / 16.0F, 0.05F);
		assertEquals(1, tJuicer.tOpened, "4px = PX_P[4] inclusive → opens");
		tJuicer.clientNeiArm((byte) 1, 4.0001F / 16.0F, 0.05F);
		assertEquals(1, tJuicer.tOpened, "past the 4px bound is out");
		tJuicer.clientNeiArm((byte) 1, 0.05F, 4.0001F / 16.0F);
		assertEquals(1, tJuicer.tOpened, "the Z axis carries the same bound");
		assertEquals(4.0F / 16.0F, tJuicer.cornerBound(), "the juicer corner bound is PX_P[4]");
		assertEquals(12.0F / 16.0F, tJuicer.innerBound(), "the juicer centre bound is the symmetric PX_N[4]");
	}

	/** The server arm keeps swallowing the corner click — no processing round, no jump (upstream :190/:210). */
	@Test
	void theServerArmStillSwallowsTheCornerClick() {
		CapturingBowl tBowl = new CapturingBowl();
		String tReport = tBowl.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.05F, 0.05F, 0.05F);
		assertTrue(tReport.startsWith("NEI corner"), "the corner report names the quadrant: " + tReport);
		assertEquals(0, tBowl.tOpened, "the SERVER arm never jumps — the viewer opens client-side only");
	}

	/** activateChain routes its client side through the NEI arm (the missing no-op's replacement). */
	@Test
	void activateChainRoutesTheClientCornerClickThroughTheArm() {
		ClientBowl tBowl = new ClientBowl();
		assertEquals("client side", tBowl.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.05F, 0.05F, 0.05F));
		assertEquals(1, tBowl.tOpened, "the client corner click reaches the viewer seam");
		ClientBowl tCentre = new ClientBowl();
		tCentre.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.05F, 0.5F);
		assertEquals(0, tCentre.tOpened, "the client centre click does not jump");
	}
}
