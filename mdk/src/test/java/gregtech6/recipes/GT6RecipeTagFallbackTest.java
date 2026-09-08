package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;

/**
 * The machine-input material-tag fallback (task p25-tag-input-machine-fallback): the
 * three acceptance arms of the card plus the two rulings seams.
 *
 * <ul>
 * <li><b>Positive</b>: a vanilla IRON_INGOT feeds a recipe row whose input is the GT
 *     {@code (ingot, Iron)} MaterialPrefixItem — the family tag
 *     {@code <platform>:ingots/iron} bridges the two item identities (the modern
 *     primitive of the upstream unification semantics, OreDictManager.equal_
 *     :628-634).</li>
 * <li><b>Negative</b>: GOLD_INGOT / COPPER_INGOT stay OUT of the iron-ingot row;
 *     non-material recipe inputs never resolve the tag at all; a family-less prefix
 *     (dustSmall) declines before the tag test. The negatives hold under BOTH the
 *     production binding and the injected stub — the same-value discipline of the
 *     ruling (decisions.p25-tag-input-fallback-rulings ②).</li>
 * <li><b>Regression</b>: the exact branch routes NBT-tagged inputs exactly as before
 *     (the circuit {@code Damage} configuration tag), count gating and consume
 *     semantics are untouched on the fallback arm, and the poured map's linear scan
 *     picks the row up (the fallback lives inside {@code checkStacksEqual}, so it
 *     flows through findRecipe unchanged — RecipeMap.java:100-151).</li>
 * </ul>
 *
 * <p><b>The seam</b> (rulings ②): {@link Recipe#sTagTest} is production-bound to
 * {@code ItemStack::is}; offline the tag manager never boots — every tag reads empty
 * (Holder.Reference.is = {@code tags.contains}, Holder.java:156-158 over an unbound
 * set) — so the tests inject a membership stub encoding the forge-shipped default-tag
 * members (forge-1.20.1 generated tree: {@code ingots/iron = minecraft:iron_ingot}) plus
 * the GT family faces the datagen emits. The positive under the stub is exactly the
 * verdict the RCON runServer live chain re-proves on the real registry (the second,
 * real-path proof of the same production binding).
 *
 * <p><b>Direction rule</b> (card acceptance 5): the TagKey derives from the RECIPE
 * input alone; the machine input is only ever tested. The reverse-case test constructs
 * {@code tInput = GT plate iron} + {@code aInput = GT ingot iron} (same material,
 * member of the sibling family) and pins BOTH the decline and the consulted tag —
 * a reversed derivation (tag from the machine side) would wrongly match.
 */
class GT6RecipeTagFallbackTest extends GTRecipesOfflineTestBase {

	/** The probe (ingot, Iron) item — the recipe-input side of the positive arm. */
	private static MaterialPrefixItem INGOT_IRON;
	/** The probe (plate, Iron) item — the recipe-input side of the direction-rule arm. */
	private static MaterialPrefixItem PLATE_IRON;
	/** The probe (ingot, Copper) item — the wrong-material negative. */
	private static MaterialPrefixItem INGOT_COPPER;
	/** The probe (dustSmall, Iron) item — the family-less prefix negative. */
	private static MaterialPrefixItem DUST_SMALL_IRON;

	/** The consulted (tagPath &lt;- item) record of the stub — the direction-rule evidence. */
	private static final List<String> sConsulted = new ArrayList<>();

	@BeforeAll
	static void buildProbeItems() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		INGOT_IRON = probeItem("tagfb_probe_ingot_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Iron));
		PLATE_IRON = probeItem("tagfb_probe_plate_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.plate, MT.Iron));
		INGOT_COPPER = probeItem("tagfb_probe_ingot_copper", () -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Copper));
		DUST_SMALL_IRON = probeItem("tagfb_probe_dust_small_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.dustSmall, MT.Iron));
	}

	@AfterEach
	void restoreSeam() {
		Recipe.sTagTest = Recipe.VANILLA_TAG_TEST;
		sConsulted.clear();
	}

	// ------------------------------------------------------------------
	// the seam (rulings ②): production binding identity + offline red / stub green
	// ------------------------------------------------------------------

	/** The production binding is {@code ItemStack::is} — the identity, not a re-derivable copy. */
	@Test
	void seamDefaultBindingIsTheProductionItemStackIs() {
		assertSame(Recipe.VANILLA_TAG_TEST, Recipe.sTagTest, "the shipped default IS the production binding");
	}

	/**
	 * The red-green of the seam: under the production binding the positive arm declines
	 * offline (the tag manager never boots — every tag reads empty, the seam's reason to
	 * exist); the injected stub grants it, and the SAME negative arms decline under both
	 * bindings (the same-value agreement where offline resolution is meaningful).
	 */
	@Test
	void seamRedUnderProductionBindingAndGreenUnderTheInjectedStub() {
		Recipe tRow = ironIngotRow();
		ItemStack[] tForeign = {new ItemStack(Items.IRON_INGOT, 2)};

		// red: production binding, offline reality — unbound tag set (Holder.java:156-158)
		assertFalse(tRow.isRecipeInputEqual(false, false, null, tForeign),
				"offline the production ItemStack::is reads the empty tag universe — the positive arm declines");

		// green: the injected stub — the arm matches; the RCON live chain re-proves this
		// verdict on the real registry (the second proof of the same production binding)
		Recipe.sTagTest = membershipStub();
		assertTrue(tRow.isRecipeInputEqual(false, false, null, tForeign),
				"the stub grants exactly the verdict the live chain proves");

		// same-value: the negative arm declines under BOTH bindings
		ItemStack[] tGold = {new ItemStack(Items.GOLD_INGOT, 2)};
		Recipe.sTagTest = membershipStub();
		assertFalse(tRow.isRecipeInputEqual(false, false, null, tGold), "gold ∉ ingots/iron (stub)");
		Recipe.sTagTest = Recipe.VANILLA_TAG_TEST;
		assertFalse(tRow.isRecipeInputEqual(false, false, null, tGold), "gold ∉ ingots/iron (production, offline empty tags)");
	}

	// ------------------------------------------------------------------
	// arm 1: positive — vanilla IRON_INGOT feeds the GT (ingot, Iron) row
	// ------------------------------------------------------------------

	@Test
	void positiveForeignIngotMatchesThroughTheFamilyTag() {
		Recipe.sTagTest = membershipStub();
		Recipe tRow = ironIngotRow();

		// the tag the fallback consults is the composed platform family tag, and the
		// namespace is the leg seam (forge on 1.20.1 / c on 21.1)
		TagKey<Item> tTag = GT6ItemTags.materialTag(GT6ItemTags.INGOTS_FAMILY, MT.Iron);
		assertEquals("ingots/iron", tTag.location().getPath());
		assertEquals(GT6ItemTags.MATERIALS_NAMESPACE, tTag.location().getNamespace(),
				"the fallback tag rides MATERIALS_NAMESPACE — the //? forge/c seam");

		// two-stage contract: probe does not consume, consume shrinks the FOREIGN stack
		ItemStack[] tProbe = {new ItemStack(Items.IRON_INGOT, 2)};
		assertTrue(tRow.isRecipeInputEqual(false, false, null, tProbe),
				"the vanilla iron ingot (forge:ingots/iron member) matches the GT (ingot, Iron) row");
		assertEquals(2, tProbe[0].getCount(), "the probe must not consume");
		ItemStack[] tConsume = {new ItemStack(Items.IRON_INGOT, 2)};
		assertTrue(tRow.isRecipeInputEqual(true, false, null, tConsume),
				"the fallback match consumes like an exact match");
		assertEquals(0, tConsume[0].getCount(), "one pass consumes exactly the required 2");
	}

	/** The exact stage short-circuits: an identity match must never consult the tag seam. */
	@Test
	void exactStageShortCircuitsBeforeTheTagTest() {
		Recipe.sTagTest = recordingStubThatDeniesEverything();
		Recipe tRow = ironIngotRow();

		assertTrue(tRow.isRecipeInputEqual(false, false, null, new ItemStack(INGOT_IRON, 2)),
				"the exact branch still matches the probe item itself");
		assertTrue(sConsulted.isEmpty(), "an exact match never reaches the tag seam");
	}

	// ------------------------------------------------------------------
	// arm 2: negatives — wrong metal, non-material inputs, family-less prefix
	// ------------------------------------------------------------------

	@Test
	void negativeForeignItemsStayOut() {
		Recipe.sTagTest = membershipStub();
		Recipe tRow = ironIngotRow();

		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack(Items.GOLD_INGOT, 2)),
				"gold ingot ∉ forge:ingots/iron — the iron row must not swallow it");
		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack(Items.COPPER_INGOT, 2)),
				"copper ingot ∉ forge:ingots/iron");
		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack(INGOT_COPPER, 2)),
				"even the GT copper ingot (its OWN family face is ingots/copper) stays out of the iron row");
	}

	/** The machine-input side NEVER resolves: a MaterialPrefixItem on the machine side must not open the fallback. */
	@Test
	void nonMaterialRecipeInputsNeverResolveTheTag() {
		Recipe.sTagTest = recordingStubThatDeniesEverything();

		// recipe input vanilla, machine input the probe prefix item — the direction rule
		// forbids deriving anything from the machine side
		Recipe tVanillaRow = new Recipe(true, new ItemStack[] {new ItemStack(Items.BONE)}, new ItemStack[] {new ItemStack(Items.BONE_MEAL)}, null, null, 32, 16, 0);
		assertFalse(tVanillaRow.isRecipeInputEqual(false, false, null, new ItemStack(INGOT_IRON, 2)),
				"a MaterialPrefixItem machine input must not match a vanilla-input row");
		assertTrue(sConsulted.isEmpty(), "the tag seam is never consulted when the recipe input is not a prefix item");

		// and the mirror: recipe input prefix item, machine input vanilla-but-tagless
		assertFalse(ironIngotRow().isRecipeInputEqual(false, false, null, new ItemStack(Items.BONE, 2)),
				"bone is no ingots/iron member");
	}

	/** A prefix without a platform family declines BEFORE the tag test (the second gate). */
	@Test
	void familylessPrefixDeclinesBeforeTheTagTest() {
		Recipe.sTagTest = recordingStubThatDeniesEverything();
		Recipe tRow = new Recipe(true, new ItemStack[] {new ItemStack(DUST_SMALL_IRON)}, new ItemStack[] {new ItemStack(Items.IRON_NUGGET)}, null, null, 32, 16, 0);

		// dustSmall carries no P0 platform family (GT6ItemTags.itemTagFamily → null), so
		// even a genuine forge:ingots/iron member must not match it
		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack(Items.IRON_INGOT, 2)),
				"a family-less prefix recipe keeps the exact-only semantics");
		assertTrue(sConsulted.isEmpty(), "the family gate fires before the tag test");
	}

	// ------------------------------------------------------------------
	// card acceptance 5: the direction rule
	// ------------------------------------------------------------------

	/**
	 * Reverse case: {@code tInput = GT (plate, Iron)}, {@code aInput = GT (ingot, Iron)} —
	 * same material, the machine input IS a member of the sibling family tag. A reversed
	 * derivation (tag from the machine side: ingots/iron, tested with the plate) would
	 * wrongly match; the rule pins the decline and shows the consulted tag is the RECIPE
	 * input's family face tested with the MACHINE input.
	 */
	@Test
	void directionRuleTagDerivesFromTheRecipeInputOnly() {
		Recipe.sTagTest = membershipStub();
		Recipe tPlateRow = new Recipe(true, new ItemStack[] {new ItemStack(PLATE_IRON)}, new ItemStack[] {new ItemStack(Items.IRON_NUGGET)}, null, null, 64, 16, 0);

		assertFalse(tPlateRow.isRecipeInputEqual(false, false, null, new ItemStack(INGOT_IRON, 2)),
				"a plateIron row must not swallow an ingot — the direction rule");
		assertEquals(List.of("plates/iron <- " + INGOT_IRON), sConsulted,
				"the consulted tag is the RECIPE input's family face, tested with the MACHINE input item");

		// the mirror direction: an ingot row must not swallow the plate either
		sConsulted.clear();
		assertFalse(ironIngotRow().isRecipeInputEqual(false, false, null, new ItemStack(PLATE_IRON, 2)),
				"an ingotIron row must not swallow a plate");
		assertEquals(List.of("ingots/iron <- " + PLATE_IRON), sConsulted);
	}

	// ------------------------------------------------------------------
	// arm 3: regression — the exact branch, count gate and consume semantics unchanged
	// ------------------------------------------------------------------

	/** The circuit {@code Damage}-tag routing still rides the exact branch (p14/p16 deviation preserved). */
	@Test
	void exactBranchStillRoutesTaggedInputsExactly() {
		Recipe.sTagTest = recordingStubThatDeniesEverything();
		Recipe tRow = new Recipe(true, new ItemStack[] {tagged(1)}, new ItemStack[] {new ItemStack(Items.GLASS_BOTTLE)}, null, null, 32, 16, 0);

		assertTrue(tRow.isRecipeInputEqual(false, false, null, new ItemStack[] {tagged(1)}),
				"same item, same tag → the exact branch matches");
		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack[] {tagged(2)}),
				"same item, different tag → the exact branch declines; the fallback MUST NOT rescue it "
						+ "(the input is not a prefix item — the circuit Damage routing is untouched)");
		assertTrue(sConsulted.isEmpty(), "the fallback never fires for non-material inputs");
	}

	/** The count gate guards BOTH stages; the consume pass shrinks by the recipe-input count only. */
	@Test
	void countGateAndConsumeSemanticsUnchangedOnTheFallbackArm() {
		Recipe.sTagTest = membershipStub();
		Recipe tRow = ironIngotRow();

		assertFalse(tRow.isRecipeInputEqual(false, false, null, new ItemStack(Items.IRON_INGOT, 1)),
				"1 foreign ingot < the required 2 — the count gate refuses the fallback arm too");
		ItemStack[] tFeed = {new ItemStack(Items.IRON_INGOT, 3)};
		assertTrue(tRow.isRecipeInputEqual(true, false, null, tFeed));
		assertEquals(1, tFeed[0].getCount(), "the consume pass shrinks exactly by the recipe-input count");
	}

	/** The fallback lives inside checkStacksEqual, so the poured-map linear scan picks the row up. */
	@Test
	void fallbackFlowsThroughTheRecipeMapLinearScan() {
		Recipe.sTagTest = membershipStub();
		RecipeMap tMap = new RecipeMap(new LinkedHashSet<>(), "gt.recipe.tagfb", "Tag FB", null, 0, 1, "gt6:textures/gui/tagfb", 1, 1, 1, 0, 0, 0, 0, 1);
		tMap.addRecipe(ironIngotRow());

		assertNotNull(tMap.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null, new ItemStack(Items.IRON_INGOT, 2)),
				"findRecipe's isRecipeInputEqual probe rides the same two-stage equality");
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, null, new ItemStack(Items.GOLD_INGOT, 2)),
				"the map-level negative stays negative");
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	/** The (ingot, Iron) row: input = the GT probe prefix item, 2x. */
	private static Recipe ironIngotRow() {
		return new Recipe(true, new ItemStack[] {new ItemStack(INGOT_IRON, 2)}, new ItemStack[] {new ItemStack(INGOT_IRON, 1)}, null, null, 32, 16, 0);
	}

	/** A tag-carrying vanilla stack ({code Damage:n}) — the exact-branch NBT pair driver. */
	private static ItemStack tagged(int aDamage) {
		ItemStack tStack = new ItemStack(Items.BONE, 1);
		//? if forge {
		tStack.getOrCreateTag().putInt("Damage", aDamage);
		//?} else {
		/*CompoundTag tTag = new CompoundTag();
		tTag.putInt("Damage", aDamage);
		tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		*///?}
		return tStack;
	}

	/**
	 * The offline membership stub: the forge-shipped default-tag members used by this
	 * suite (forge-1.20.1 generated tree: {@code ingots/iron = [minecraft:iron_ingot]}) plus
	 * the GT family faces the datagen emits (gt6:ingot_iron ∈ ingots/iron —
	 * GT6ItemTags.addFamilyFace). Everything else declines.
	 */
	private static java.util.function.BiPredicate<ItemStack, TagKey<Item>> membershipStub() {
		return (aInput, aTag) -> {
			sConsulted.add(aTag.location().getPath() + " <- " + aInput.getItem());
			String tPath = aTag.location().getPath();
			Item tItem = aInput.getItem();
			if ("ingots/iron".equals(tPath)) return tItem == Items.IRON_INGOT || tItem == INGOT_IRON;
			if ("plates/iron".equals(tPath)) return tItem == PLATE_IRON;
			return false;
		};
	}

	/** A deny-everything recording stub — proves the DECLINE cases never even consult the seam. */
	private static java.util.function.BiPredicate<ItemStack, TagKey<Item>> recordingStubThatDeniesEverything() {
		return (aInput, aTag) -> {
			sConsulted.add(aTag.location().getPath() + " <- " + aInput.getItem());
			return false;
		};
	}

	/**
	 * The offline probe item (the FileSawTest/ScrewdriverTest precedent): the Forge
	 * intrusive holder makes {@code new MaterialPrefixItem(...)} throw while the vanilla
	 * item registry is frozen, and an ItemStack constructor resolves the registry delegate
	 * eagerly — so the probe item is registered under a dedicated probe id and never
	 * reaches any committed data.
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Supplier<I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the Forge runtime shape: THREE locks must open (the FileSawTest walk —
			// the vanilla frozen flag, the delegate ForgeRegistry.isFrozen, the
			// NamespacedWrapper.locked register gate)
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			// the 21.1 runtime shape: the plain vanilla DefaultedMappedRegistry — a single
			// frozen flag guards both the intrusive-holder construction and Registry.register
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
	private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}
}
