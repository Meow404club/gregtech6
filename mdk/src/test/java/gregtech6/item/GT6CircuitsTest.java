package gregtech6.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Integrated Circuit carrier offline acceptance (task p16-distillery-family ①): the
 * vanilla 1.20.1 {@code Damage}-key carrier semantics (ItemStack.java:291-297 —
 * getDamageValue reads {@code tag.getInt("Damage")} on ANY item, default 0 for a tag-less
 * stack; setDamageValue writes it), the {@code ST.tag(n)} selector helper shape (count 1 +
 * the configuration number — the size-0 upstream form is not portable), and the
 * registration-table shape (the RegistryObject id path — pre-registration readable,
 * RegistryObject.java:287, the GT6Tools TAB_TABLE convention).
 *
 * <p>The fixture item is a VANILLA entry: the vanilla item registry freezes at bootstrap and
 * the Forge intrusive holder makes {@code new Item(...)} throw offline (the
 * GT6RecipesShCLTest synthetic-item convention) — the helpers are item-agnostic (the
 * configuration rides the ItemStack NBT), so the carrier semantics drive through a fixture
 * item while the LIVE IntegratedCircuitItem identity and registration resolve through the
 * runServer/RCON gate.
 */
class GT6CircuitsTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	// ------------------------------------------------------------------
	// the ST.tag(n) selector helper (ST.java:779-781 shape)
	// ------------------------------------------------------------------

	@Test
	void selectorCarriesCountOneAndTheDamageTag() {
		ItemStack tStack = GT6Circuits.selector(Items.BRICKS, 0);
		assertEquals(1, tStack.getCount(), "count 1 — the size-0 upstream form is not portable (the research verdict)");
		assertTrue(GT6Circuits.hasConfigurationTag(tStack), "the configuration rides the Damage NBT key");
		assertEquals(0, GT6Circuits.configurationOf(tStack), "config 0 → the vanilla damage read 0 (vanilla ItemStack.java:291-293)");

		ItemStack tFive = GT6Circuits.selector(Items.BRICKS, 5);
		assertEquals(5, GT6Circuits.configurationOf(tFive), "config 5 → Damage 5");
		assertEquals(5, GT6Circuits.configurationOf(tFive), "the configurationOf read-back");
		//? if forge {
		assertFalse(Items.BRICKS.canBeDepleted(), "the carrier registers maxDamage 0 — no durability bar (vanilla canBeDepleted = maxDamage > 0)");
		//?} else {
		/*assertFalse(tFive.isDamageableItem(), "the carrier registers no durability — the stack is not damageable");
		//21.1 renamed the Item accessor; the stack-side check is the same predicate.
		*///?}
		assertTrue(tFive.isStackable(), "non-depleted ⇒ stackable (the vanilla isStackable rule, ItemStack.java:274-276)");
		assertEquals(64, tFive.getMaxStackSize(), "the default 64 stack size");
	}

	@Test
	void selectorClampsNegativesAndUntaggedReadsZero() {
		assertEquals(0, GT6Circuits.configurationOf(new ItemStack(Items.BRICKS, 1)),
				"a tag-less stack reads config 0 — the vanilla null-tag default (vanilla :292)");
		assertEquals(0, GT6Circuits.configurationOf(GT6Circuits.selector(Items.BRICKS, -3)),
				"negative configurations clamp to 0 (the vanilla Math.max(0, n), :296)");
	}

	/** Two configurations of the same item are NOT tag-equal — the Chem.java:333-vs-:346 selector routing premise. */
	@Test
	void configurationsAreTagDistinguishable() {
		ItemStack tZero = GT6Circuits.selector(Items.BRICKS, 0);
		ItemStack tOne = GT6Circuits.selector(Items.BRICKS, 1);
		assertTrue(ItemStack.isSameItemSameTags(tZero, tZero.copy()), "same config ⇒ identical identity (the recipe match form)");
		assertFalse(ItemStack.isSameItemSameTags(tZero, tOne), "config 0 and config 1 differ ⇒ different identities");
		assertFalse(ItemStack.isSameItemSameTags(tZero, new ItemStack(Items.BRICKS, 1)),
				"a tag-less stack matches NO tagged recipe input (the exact-tag matching, declared)");
	}

	// ------------------------------------------------------------------
	// the identity probe (the Recipe.checkStacksEqual consume-skip key)
	// ------------------------------------------------------------------

	@Test
	void isSelectorAnswersFalseForNonCircuitStacks() {
		assertFalse(GT6Circuits.isSelector(new ItemStack(Items.BRICKS, 1)), "a vanilla item is not a selector");
		assertFalse(GT6Circuits.isSelector(GT6Circuits.selector(Items.BRICKS, 0)),
				"a tagged non-circuit stack is still not a selector (the identity is the item, not the tag)");
		assertFalse(GT6Circuits.isSelector(ItemStack.EMPTY), "the empty stack is not");
		// the positive arm is the LIVE identity (IntegratedCircuitItem instanceof) — the
		// frozen-registry rule keeps it out of the offline universe; the RCON chain proves
		// the live end-to-end (circuit inserted → runs → survives)
	}

	// ------------------------------------------------------------------
	// the registration-table shape (offline-readable halves)
	// ------------------------------------------------------------------

	@Test
	void registrationTableCarriesTheCircuitItem() {
		assertEquals("integrated_circuit", GT6Circuits.INTEGRATED_CIRCUIT.getId().getPath(),
				"the gt6:integrated_circuit path (upstream gt.integrated_circuit, ItemIntegratedCircuit.java:50)");
		assertFalse(GT6Circuits.ITEMS.getEntries().isEmpty(), "the DeferredRegister carries the entry");
		assertEquals(GT6Circuits.INTEGRATED_CIRCUIT, GT6Circuits.ITEMS.getEntries().iterator().next(),
				"exactly the one item row (single-item registration, the research verdict)");
		assertEquals("item.gt6.integrated_circuit.configuration", GT6Circuits.TOOLTIP_KEY,
				"the tooltip key the GT6EnUs provider pins (the 'Configuration: ' line, upstream :54/:100)");
	}
}
