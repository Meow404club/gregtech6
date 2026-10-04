/**
 * The mortar BE semantic pins (task mortar-family, the GT6AnvilBlockEntityTest shape):
 * the working click consumes FROM the held stack and delivers to the bag (upstream
 * MultiTileEntityMortar.java:86-92), a non-recipe held stack is rejected with the held
 * stack untouched (:105 — the silent upstream swallow, reported), the exhaustion divisor
 * is the verbatim 250 (:91), the top-face corner is the server-swallow/client-arm split
 * (:81/:99, the INCLUSIVE {@code <= PX_P[4]} bound — the anvil's strict {@code <} is the
 * anvil's variant, not the mortar's), the null player is the RCON report arm (:84), and
 * the bag give is all-or-nothing (the anvil form). The map rows are a SYNTHETIC row
 * injected straight into the map (hermetic, no JSON seams — the shipped-mortar.json pour
 * itself is the GT6MortarRowsPourTest's acceptance): one 1-glass -> 9-dust row with the
 * glass row's real magnitudes (16 EUt x 32 t = 512), the upstream :674 face.
 */
package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;

class GT6MortarBlockEntityTest extends gregtech6.tileentity.GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	/** The synthetic row's faces: 1 glass -> 9 glass dust, 16 EUt x 32 t (the :674 magnitudes).
	 *  Assigned in @BeforeAll — a static INITIALIZER would dereference Items in <clinit>,
	 *  before the base boot, poisoning the whole worker JVM (the run-order lottery). */
	private static Item GLASS;
	private static Item DUST; // a stand-in output distinct from the input

	private static BlockEntityType<GT6MortarBlockEntity> sMortarType;

	@BeforeAll
	static void buildOfflineFixtures() {
		GLASS = Items.GLASS;
		DUST = Items.SAND;
		// the offline holders (the GT6AnvilNeiCornerTest form — the BE-type registry
		// write window reopened by the base class)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MortarBlockEntity>[] tHolder = (BlockEntityType<GT6MortarBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MortarBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sMortarType = tHolder[0];
	}

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // reset-then-init order first (the neo boot-pour lesson)
		GT6RecipeMaps.init();
		// the synthetic :674 face — one row, deterministic (no loader seams, no chances)
		GT6RecipeMaps.MORTAR.mRecipeList.add(new Recipe(true,
				new ItemStack[] {new ItemStack(GLASS, 1)},
				new ItemStack[] {new ItemStack(DUST, 9)},
				null, null, 32, 16, 0));
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMaps.reset(); // never leak the synthetic row into other tests
	}

	static GT6MortarBlockEntity mortar() {
		return new GT6MortarBlockEntity(sMortarType, POS, Blocks.STONE.defaultBlockState());
	}

	/** Captures the viewer jump (the seam the routing pins drive). */
	static class CapturingMortar extends GT6MortarBlockEntity {
		int tOpened;
		CapturingMortar() { super(sMortarType, POS, Blocks.STONE.defaultBlockState()); }
		@Override protected void openNei() { tOpened++; }
	}

	/** Captures the jump and flags the BE client-side (the activateChain client arm). */
	static class ClientMortar extends CapturingMortar {
		@Override public boolean isServerSide() { return false; }
	}

	// ---------------------------------------------------------------- the corner (the anvil-corner 4 pins)

	/** The client arm opens the viewer from the top-face corner ONLY (upstream :97-103 — the top gate the anvil lacks). */
	@Test
	void theClientArmRoutesTheTopCornerClickToTheViewer() {
		ClientMortar tMortar = new ClientMortar();
		tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.1F, 0.5F, 0.1F);
		assertEquals(1, tMortar.tOpened, "top face + the corner quadrant -> the viewer jump fires");
		tMortar.activateChain(null, (byte) 3, ItemStack.EMPTY, 0.1F, 0.5F, 0.1F);
		assertEquals(1, tMortar.tOpened, "a side face never jumps (the mortar arm HAS the top gate, :97 SIDES_TOP)");
		tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F);
		assertEquals(1, tMortar.tOpened, "the working surface never jumps");
	}

	/** The corner bound is the INCLUSIVE upstream variant: exactly 4px IS the corner (:81/:99 {@code <= PX_P[4]}). */
	@Test
	void theCornerBoundIsTheInclusiveUpstream4Px() {
		ClientMortar tMortar = new ClientMortar();
		tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 4.0F / 16.0F, 0.5F, 4.0F / 16.0F);
		assertEquals(1, tMortar.tOpened, "exactly 4px IS the corner (the inclusive <= of :81, unlike the anvil's strict <)");
		tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 4.0001F / 16.0F, 0.5F, 0.1F);
		assertEquals(1, tMortar.tOpened, "just past 4px falls through");
		assertEquals(4.0F / 16.0F, GT6MortarBlockEntity.CORNER_BOUND, "the bound is PX_P[4]");
		assertTrue(GT6MortarBlockEntity.neiCorner(0.0F, 0.0F), "the min corner is a corner");
		assertFalse(GT6MortarBlockEntity.neiCorner(0.5F, 0.1F), "the centre is not");
	}

	/** The server arm keeps swallowing the corner — no chain action, no jump (upstream :79-82). */
	@Test
	void theServerArmStillSwallowsTheCorner() {
		CapturingMortar tMortar = new CapturingMortar();
		String tReport = tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.1F, 0.5F, 0.1F);
		assertEquals("NEI corner (the recipe-viewer jump is the client arm)", tReport, "the corner report names the arm split");
		assertEquals(0, tMortar.tOpened, "the SERVER arm never jumps — the viewer opens client-side only");
	}

	/** activateChain routes its client side through the NEI arm. */
	@Test
	void activateChainRoutesTheClientClickThroughTheArm() {
		ClientMortar tMortar = new ClientMortar();
		assertEquals("client side", tMortar.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.1F, 0.5F, 0.1F));
		assertEquals(1, tMortar.tOpened, "the client corner click reaches the viewer seam");
	}

	// ---------------------------------------------------------------- the working click

	/** The working click: the held stack pays (:89), the output lands in the bag (:90), the report names it. */
	@Test
	void theWorkingClickGrindsTheHeldStackIntoTheBag() {
		GT6MortarBlockEntity tMortar = mortar();
		GT6AnvilBlockEntityTest.BagPlayer tPlayer = emptyBagPlayer();
		ItemStack tHeld = new ItemStack(GLASS, 1);
		String tReport = tMortar.activateChain(tPlayer, (byte) 1, tHeld, 0.5F, 0.5F, 0.5F);
		assertTrue(tReport.startsWith("ground: 9x "), "the glass row's 9-dust output names the report: " + tReport);
		assertTrue(tHeld.isEmpty(), "the input paid FROM the held stack (:89 isRecipeInputEqual(T, F))");
		assertEquals(9, tPlayer.getInventory().items.get(0).getCount(),
				"the 9 dust outputs rode the bag give (:90 ST.give)");
		assertEquals(DUST, tPlayer.getInventory().items.get(0).getItem(), "the output item");
	}

	/** A held stack with no recipe row is rejected and untouched (upstream :105 — the silent swallow, reported). */
	@Test
	void theNonRecipeHeldStackIsRejectedUntouched() {
		GT6MortarBlockEntity tMortar = mortar();
		GT6AnvilBlockEntityTest.BagPlayer tPlayer = emptyBagPlayer();
		ItemStack tHeld = new ItemStack(Items.DIRT, 3);
		assertEquals("no matching recipe", tMortar.activateChain(tPlayer, (byte) 1, tHeld, 0.5F, 0.5F, 0.5F),
				"a non-recipe held stack reports the rejection");
		assertEquals(3, tHeld.getCount(), "the held stack is untouched");
	}

	/** The :84 isPlayer gate — the null player is the RCON report arm, no processing. */
	@Test
	void theNullPlayerArmReportsWithoutProcessing() {
		GT6MortarBlockEntity tMortar = mortar();
		ItemStack tHeld = new ItemStack(GLASS, 1);
		assertEquals("only a player can work the mortar", tMortar.activateChain(null, (byte) 1, tHeld, 0.5F, 0.5F, 0.5F),
				"the fake-player swallow (:84) reports");
		assertEquals(1, tHeld.getCount(), "nothing processed");
	}

	/** An empty hand reports before the recipe walk (the hopper-less input face). */
	@Test
	void theEmptyHandReports() {
		GT6MortarBlockEntity tMortar = mortar();
		GT6AnvilBlockEntityTest.BagPlayer tPlayer = emptyBagPlayer();
		assertEquals("nothing held", tMortar.activateChain(tPlayer, (byte) 1, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F));
	}

	// ---------------------------------------------------------------- the exhaustion seam

	/** The exhaustion divisor is the verbatim 250 (:91 — NO clamp; the anvil's max(1,..) is the anvil's variant). */
	@Test
	void theGrindingExhaustionIsTotalPowerOver250() {
		assertEquals(250, GT6MortarBlockEntity.EXHAUST_DIVISOR, "the :91 divisor verbatim");
		Recipe tRow = GT6RecipeMaps.MORTAR.mRecipeList.iterator().next(); // the fresh generation holds exactly the one synthetic row
		assertEquals(512, tRow.getAbsoluteTotalPower(), "16 EUt x 32 t (the :674 magnitudes)");
		assertEquals(512 / 250.0F, GT6MortarBlockEntity.exhaustOf(tRow), 0.0F,
				"the exhaustOf seam is the exact :91 formula — the live causeFoodExhaustion call rides it (the bag player's invulnerable arm skips the vanilla add, the cup-card field_test posture)");
	}

	// ---------------------------------------------------------------- the bag give boundary

	/** The all-or-nothing give: a full bag refuses the whole stack (the anvil R1 boundary form). */
	@Test
	void giveToPlayerRefusesThePartialFitAllOrNothing() {
		GT6MortarBlockEntity tMortar = mortar();
		ItemStack[] tSlots = new ItemStack[36];
		tSlots[0] = new ItemStack(Items.IRON_NUGGET, 63);
		for (int i = 1; i < 36; i++) tSlots[i] = new ItemStack(Items.DIRT, 64);
		GT6AnvilBlockEntityTest.BagPlayer tPlayer = GT6AnvilBlockEntityTest.BagPlayer.withInventory(tSlots);
		ItemStack tFive = new ItemStack(Items.IRON_NUGGET, 5);
		assertFalse(tMortar.giveToPlayer(tPlayer, tFive), "63 + 5 > 64 — the full bag refuses");
		assertEquals(63, tPlayer.getInventory().items.get(0).getCount(), "nothing moved (the partial-fill would show 64)");
	}

	/** The give takes the whole stack when it fits (the empty-bag happy face). */
	@Test
	void giveToPlayerTakesTheWholeStackWhenItFits() {
		GT6MortarBlockEntity tMortar = mortar();
		GT6AnvilBlockEntityTest.BagPlayer tPlayer = emptyBagPlayer();
		assertTrue(tMortar.giveToPlayer(tPlayer, new ItemStack(Items.IRON_NUGGET, 9)));
		assertEquals(9, tPlayer.getInventory().items.get(0).getCount());
	}

	/** An all-empty 36-slot bag (the bagPlayer form with zeroed slots). */
	static GT6AnvilBlockEntityTest.BagPlayer emptyBagPlayer() {
		return GT6AnvilBlockEntityTest.BagPlayer.withInventory(new ItemStack[36]);
	}
}
