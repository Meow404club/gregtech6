/*
 * Offline tests for task p26-w1-press-extruder-molds: the GT6ExtruderMolds registration
 * home — the row0 MINIMAL subset (the plate mold + the rod mold), the GT6FoodCansTest
 * posture over the pure table + registry-wiring data.
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the registry-wiring data
 * ({@code DeferredRegister.getEntries()} / {@code RegistryObject.getId()}), the
 * {@link GT6ExtruderMolds#sMoldTest} seam behaviour over fixture items (the offline JVM
 * resolves no tags — the sTagTest stub contract), and the datagen JSON existence (the
 * crafting rows + the tag, read off the generated tree — the GT6TextureCensusTest path
 * posture).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class GT6ExtruderMoldsTest {

	private static java.util.function.Predicate<ItemStack> sMoldTestRestore;

	/** The offline boot BEFORE the first GT6ExtruderMolds touch (the GT6FoodCansTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@AfterEach
	void restoreSeams() {
		if (sMoldTestRestore != null) {
			GT6ExtruderMolds.sMoldTest = sMoldTestRestore;
			sMoldTestRestore = null;
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** The mold DR targets the vanilla item registry (the GT6FoodCans shape). */
	@Test
	public void moldRegistryKeyIsTheVanillaItemRegistry() {
		assertEquals(Registries.ITEM, GT6ExtruderMolds.ITEMS.getRegistryKey());
	}

	/** The row0 pair: exactly two molds, upstream meta order :186 plate before :212 rod. */
	@Test
	public void row0SubsetIsPlateThenRod() {
		assertEquals(2, GT6ExtruderMolds.MOLDS.size(), "the row0 minimal subset is exactly two molds");
		assertSame(GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE, GT6ExtruderMolds.MOLDS.get(0), "row 0 is the plate mold");
		assertSame(GT6ExtruderMolds.SHAPE_EXTRUDER_ROD, GT6ExtruderMolds.MOLDS.get(1), "row 1 is the rod mold");
		assertEquals(rl("shape_extruder_plate"), GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.getId(), "upstream meta 10001");
		assertEquals(rl("shape_extruder_rod"), GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.getId(), "upstream meta 10027");
	}

	/** The DR entry ids are exactly the two mold paths (nothing else rides this register). */
	@Test
	public void deferredRegisterEntriesAreExactlyTheMoldPair() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		GT6ExtruderMolds.ITEMS.getEntries().forEach(tEntry -> tIds.add(tEntry.getKey().location()));
		Set<ResourceLocation> tExpected = Set.of(rl("shape_extruder_plate"), rl("shape_extruder_rod"));
		assertEquals(tExpected, tIds, "the mold register carries exactly the row0 pair");
	}

	/**
	 * The sMoldTest seam over fixture items: the stub answers the negatives exactly as
	 * production does (a non-mold is a non-mold under both, empty/null stacks included);
	 * the positives the stub grants are exactly what the RCON live chain re-proves with the
	 * real registry tag (the Recipe.VANILLA_TAG_TEST two-contract rule). Fixtures are
	 * EXISTING vanilla items (mod items are not constructible offline — the intrusive
	 * vanilla item registry freezes at bootstrap, the GT6RecipesShCLTest convention).
	 */
	@Test
	public void moldIdentitySeamAnswersFixtureItems() {
		Item tPlateFixture = net.minecraft.world.item.Items.IRON_INGOT;
		Item tRodFixture = net.minecraft.world.item.Items.GOLD_INGOT;
		Item tOtherFixture = net.minecraft.world.item.Items.STICK;
		sMoldTestRestore = GT6ExtruderMolds.sMoldTest;
		GT6ExtruderMolds.sMoldTest = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.getItem() == tPlateFixture || aStack.getItem() == tRodFixture);
		assertTrue(GT6ExtruderMolds.isMold(new ItemStack(tPlateFixture)), "the plate fixture is a mold");
		assertTrue(GT6ExtruderMolds.isMold(new ItemStack(tRodFixture)), "the rod fixture is a mold");
		assertFalse(GT6ExtruderMolds.isMold(new ItemStack(tOtherFixture)), "a non-mold stays a non-mold");
		assertFalse(GT6ExtruderMolds.isMold(ItemStack.EMPTY), "the empty stack is not a mold");
		assertFalse(GT6ExtruderMolds.isMold(null), "null is not a mold");
	}

	/**
	 * The Recipe.sNotConsumable composition (task p26-w1-press-extruder-molds extension):
	 * the production default consumes nothing it claims — the fixture swap pins the
	 * composition face (circuit OR mold), reading the field at method entry (every swap in
	 * this class is restored by {@link #restoreSeams()}, so the entry value IS the
	 * production default initializer).
	 */
	@Test
	public void notConsumableDefaultIncludesTheMoldIdentity() {
		java.util.function.Predicate<ItemStack> tDefault = gregtech6.recipes.Recipe.sNotConsumable;
		Item tPlateFixture = net.minecraft.world.item.Items.IRON_INGOT;
		sMoldTestRestore = GT6ExtruderMolds.sMoldTest;
		GT6ExtruderMolds.sMoldTest = aStack -> aStack != null && !aStack.isEmpty() && aStack.getItem() == tPlateFixture;
		assertTrue(tDefault.test(new ItemStack(tPlateFixture)),
				"the production default claims the mold (the upstream size-0 marker port)");
		assertFalse(tDefault.test(new ItemStack(net.minecraft.world.item.Items.STICK)),
				"a plain vanilla item is consumable under the production default");
		// the circuit half of the composition stays load-bearing (the p16 face, pinning that
		// the mold extension did not displace it): under the fixture swap the circuit item is
		// NOT claimed by the mold arm, so the default's answer rides its own circuit identity
		// — unresolvable offline (the registry freeze), pinned live by the RCON chains.
		assertFalse(GT6ExtruderMolds.isMold(new ItemStack(net.minecraft.world.item.Items.STICK)),
				"the mold arm alone stays narrow (fixtures only, no vanilla-item bleed)");
	}

	/**
	 * The datagen JSON existence: the two crafting rows + the mold tag ship in the generated
	 * tree. Read off the CLASSPATH (the GT6MachinePaintRenderDatagenTest getResourceAsStream
	 * form — src/generated/resources is a test resource dir, so the assertion is
	 * working-directory independent; the stonecutter versioned nodes run from
	 * mdk/versions/&lt;node&gt;/ where a relative src/ path would miss).
	 */
	@Test
	public void craftingJsonAndTagJsonExistInTheGeneratedTree() throws Exception {
		assertTrue(classpathHas("data/gt6/recipes/shape_extruder_plate.json"),
				"the plate-mold crafting JSON must ship (the tier-a vanilla datagen row)");
		assertTrue(classpathHas("data/gt6/recipes/shape_extruder_rod.json"),
				"the rod-mold crafting JSON must ship (the tier-a vanilla datagen row)");
		assertTrue(classpathHas("data/gt6/tags/items/extruder_shapes.json"),
				"the mold-family tag JSON must ship (the not-consumable predicate's read face)");
		assertTrue(classpathHas("assets/gt6/models/item/shape_extruder_plate.json"),
				"the plate-mold item model JSON must ship");
		assertTrue(classpathHas("assets/gt6/models/item/shape_extruder_rod.json"),
				"the rod-mold item model JSON must ship");
	}

	private static boolean classpathHas(String aResource) {
		return GT6ExtruderMoldsTest.class.getClassLoader().getResourceAsStream(aResource) != null;
	}
}
