package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.item.GT6Circuits;

/**
 * The loader v2 selector-config-face suite (task circuit-config-face, the A1 ruling of
 * research.circuit-domain-census): the optional input {@code "config": n} member maps to
 * the upstream {@code ST.tag(n)} selector carrier ({@link GT6Circuits#selector}, the
 * Damage=n stack), whose matching routes through {@code Recipe.isSameItemAndTag}'s
 * exact-tag arm (Recipe.java:391-399, the p25 ADR) and whose never-consumed marker is the
 * {@code Recipe.sNotConsumable} production default's circuit arm (Recipe.java:108-109).
 *
 * <p>The three acceptance faces: (1) the v1 zero-change pin — a slot without the member is
 * built tag-less, so {@code checkStacksEqual} keeps its ignore-NBT arm for every existing
 * row (Recipe.java:363-365 {@code tIgnoreNBT = mNoNBTChecks || !tInput.hasTag()}); (2) the
 * config parse pins — the legal 0..255 domain, the bad-row rejections; (3) the freezer
 * backfill reachability — the six collision-group rows of the shipped freezer.json
 * (Loader_Recipes_Vanilla.java:662/:663 water250 + :665-:668 water1000, same-input
 * first-hit-wins) each answer only their own config number, making all 7 walk rows
 * reachable through the real {@code findRecipe} index path. Every registry face rides
 * identity stand-ins (the GT6RecipeMapDataB1RowsPourTest convention).
 */
public class GT6RecipeConfigFaceTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/**
	 * The item stand-in: gt6 ids resolve to iron ingot (the integrated_circuit selector and
	 * the dust outputs share it — inputs are what matching reads), everything else to brick
	 * (the minecraft:snowball of the seated smoke row). The two identities stay distinct so
	 * the seated row and the selector rows cannot cross-match.
	 */
	private static final java.util.function.Function<ResourceLocation, Item> ITEM_STANDIN = aId ->
			"gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK;

	/**
	 * The fluid stand-in keeps the three freezer input fluids DISTINCT (water / oxygen /
	 * mercury): an everything-to-water fixture would let the fluid-only oxygen row :214
	 * answer the water1000 lookups and fake a reachable row. Only input-leg identity is
	 * load-bearing here.
	 */
	private static final java.util.function.Function<ResourceLocation, Fluid> FLUID_STANDIN = aId ->
			switch (aId.getPath()) {
				case "water" -> Fluids.WATER;
				case "oxygen" -> Fluids.LAVA;
				default -> Fluids.FLOWING_LAVA; // mercury, ice, liquidoxygen — never a water-lookup candidate
			};

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // hermetic form: the neoforge junit FML boot pre-pours at modloading
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = ITEM_STANDIN;
		GT6RecipeMapJsonLoader.sFluidResolver = FLUID_STANDIN;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	private static ResourceLocation rl(String aId) {
		return ResourceLocation.parse(aId);
	}

	private static JsonObject json(String aJson) {
		return JsonParser.parseString(aJson).getAsJsonObject();
	}

	private void pourRow(String aRow) {
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"),
				json("{\"recipes\": [" + aRow + "]}")));
	}

	/** The machine-side selector stand-in: the same carrier item the poured rows hold (iron ingot), Damage=n. */
	private static ItemStack sel(int aConfig) {
		return GT6Circuits.selector(Items.IRON_INGOT, aConfig);
	}

	private static FluidStack[] water(int aAmount) {
		return new FluidStack[] {new FluidStack(Fluids.WATER, aAmount)};
	}

	private static int configOf(Recipe aRow) {
		return GT6Circuits.configurationOf(aRow.mInputs[0]);
	}

	/** Face 1 — the v1 zero-change pin: no config member = the plain tag-less stack, ignore-NBT matching verbatim. */
	@Test
	public void v1SlotsStayPlainAndTagTolerant() {
		pourRow("{\"inputs\":[{\"item\":\"minecraft:stick\"}],\"outputs\":[{\"item\":\"minecraft:stick\"}],\"duration\":16}");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"));
		Recipe tRow = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();
		assertFalse(GT6Circuits.hasConfigurationTag(tRow.mInputs[0]), "a v1 slot carries NO Damage tag — the exact-tag arm never engages for it (the plain v1 construction writes no payload on either leg)");

		// the ignore-NBT matching face: a machine input carrying a payload still matches the
		// tag-less v1 row (the stand-in identity of "minecraft:stick" is BRICK). The payload
		// rides the dual-leg GT6Circuits carrier — a Damage tag on the forge leg, the
		// CUSTOM_DATA envelope on 21.1 (Recipe.java:363-365: tIgnoreNBT reads the RECIPE input).
		ItemStack tTagged = GT6Circuits.selector(Items.BRICK, 3);
		assertTrue(GT6Circuits.hasConfigurationTag(tTagged), "the machine-side probe input really carries a payload");
		assertNotNull(GT6RecipeMaps.SHREDDER.findRecipe(null, 32L, null, new FluidStack[0], tTagged),
				"the v1 row answers an NBT-bearing input (the tInput.hasTag() ignore-NBT arm, Recipe.java:363-365)");
	}

	/** Face 2a — the config parse: the slot becomes the Damage=n selector carrier at count 1, config 0 included. */
	@Test
	public void configSlotsBecomeTheSelectorCarrier() {
		pourRow("{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":5}],\"outputs\":[{\"item\":\"minecraft:stick\"}],\"duration\":16}");
		Recipe tFive = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();
		assertTrue(GT6Circuits.hasConfigurationTag(tFive.mInputs[0]), "config:5 writes the Damage carrier tag");
		assertEquals(5, configOf(tFive), "the configuration number rides verbatim");
		assertEquals(1, tFive.mInputs[0].getCount(), "the marker rides count 1 (the upstream size-0 form is not portable)");

		// the hermetic re-generation: the map itself must be recreated, not just the tracker
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		pourRow("{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":0}],\"outputs\":[{\"item\":\"minecraft:stick\"}],\"duration\":16}");
		Recipe tZero = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();
		assertTrue(GT6Circuits.hasConfigurationTag(tZero.mInputs[0]), "config:0 is a REAL tag (distinct from the absent-member plain stack) — upstream ST.tag(0)");
		assertEquals(0, configOf(tZero), "config 0 rides Damage 0");
	}

	/** Face 2b — the bad-config contract: non-number, out-of-domain, count>1 — WARN + skip, the good row still pours. */
	@Test
	public void badConfigSlotsAreSkippedWithoutKillingTheFile() {
		pourRow("{\"inputs\":[{\"item\":\"minecraft:stick\"}],\"outputs\":[{\"item\":\"minecraft:stick\"}],\"duration\":16}," +
				"{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":\"high\"}],\"outputs\":[],\"duration\":16}," +
				"{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":-1}],\"outputs\":[],\"duration\":16}," +
				"{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":256}],\"outputs\":[],\"duration\":16}," +
				"{\"inputs\":[{\"item\":\"minecraft:stick\",\"config\":2,\"count\":3}],\"outputs\":[],\"duration\":16}");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"), "exactly the v1 good row poured — four bad config rows WARN-skipped");
	}

	/**
	 * The never-consumed face, loader-side: the config-built slot IS the selector carrier the
	 * production default claims — {@code Recipe.sNotConsumable} (Recipe.java:108-109) binds
	 * {@code GT6Circuits::isSelector}, an item-identity instanceof, so the loader needs (and
	 * has) no per-row marking; the true arm against a REAL IntegratedCircuitItem is the ACT
	 * suite's pin (GTAdvancedCraftingTableTest registerCircuitFixture) and the fixture-predicated
	 * consume pins of the Distillery/Implosion machine suites. Here: the production predicate
	 * object is the live default (unswapped) and its circuit arm routes by identity.
	 */
	@Test
	public void theProductionNeverConsumableDefaultIsTheLivePredicate() {
		assertFalse(Recipe.sNotConsumable.test(GT6Circuits.selector(Items.IRON_INGOT, 3)),
				"the production default (unswapped) reads item identity — a non-circuit carrier stack is not claimed");
		assertFalse(Recipe.sNotConsumable.test(new ItemStack(Items.IRON_INGOT, 1)), "a plain stack stays consumable");
	}

	/**
	 * Face 3 — the shipped freezer.json reachability matrix (the backfill's whole point): the
	 * six collision-group rows answer ONLY their own upstream ST.tag number, the seated snow
	 * row stays reachable, and the circuit-less water lookups answer nothing config-carrying —
	 * through the real indexed {@code findRecipe} path. NOTE the probe semantics: the
	 * {@code firstMatch} probe runs with {@code aDontCheckStackSizes=true} (RecipeMap.java),
	 * so the fluid-AMOUNT axis is invariant in findRecipe — the config number is the
	 * discriminator, exactly the upstream ST.tag routing (an item-less row like :664
	 * legitimately tolerates whatever else the machine holds).
	 */
	@Test
	public void everyFreezerWaterRowIsReachableAfterTheBackfill() throws Exception {
		InputStream tStream = GT6RecipeConfigFaceTest.class.getResourceAsStream("/data/gt6/recipe_maps/freezer.json");
		assertNotNull(tStream, "the shipped freezer.json rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:freezer"), tDoc.deepCopy()));
		assertEquals(10, GT6RecipeMaps.FREEZER.mRecipeList.size(), "the whole file pours — 10 rows, zero skips (the b1 census unchanged)");
		assertEquals(10, GT6RecipeMapJsonLoader.pouredCount("freezer"));

		// the water250 pair (:662 ice fluid / :663 snowball — the census's first-hit shadow)
		Recipe tIce = GT6RecipeMaps.FREEZER.findRecipe(null, 32L, null, water(250), sel(0));
		assertNotNull(tIce, ":662 answers config 0");
		assertEquals(0, configOf(tIce), "the :662 row's carrier");
		assertEquals(1, tIce.mFluidOutputs.length, ":662 = the ice-FLUID row (its discriminator)");
		assertEquals(0, tIce.mOutputs.length);
		Recipe tSnowball = GT6RecipeMaps.FREEZER.findRecipe(null, 32L, null, water(250), sel(1));
		assertNotNull(tSnowball, ":663 answers config 1 — reachable again (was first-hit shadowed)");
		assertEquals(1, configOf(tSnowball), "the :663 row's carrier");
		assertEquals(1, tSnowball.mOutputs.length, ":663 = the snowball row");

		// the water1000 four (:665 snow block / :666 ice block / :667 snow dust / :668 ice dust)
		long[][] tMatrix = {{3, 64}, {4, 128}, {9, 64}, {8, 128}};
		for (long[] tExpected : tMatrix) {
			Recipe tRow = GT6RecipeMaps.FREEZER.findRecipe(null, 32L, null, water(1000), sel((int) tExpected[0]));
			assertNotNull(tRow, ":665-:668 answer config " + tExpected[0]);
			assertEquals((int) tExpected[0], configOf(tRow), "the exact-damage routing: config " + tExpected[0] + " gets its own row");
			assertEquals(tExpected[1], tRow.mDuration, "the :665-:668 duration column at config " + tExpected[0]);
		}

		// the seated smoke row (snowball + water1000 -> ice) — reachable again now the
		// water1000 rows stopped answering selector-less. BEFORE the circuit-less lookup:
		// the map-level oRecipe buffer would otherwise keep serving the item-less :664 for
		// this lookup too (an empty-input row matches any item inputs it is probed with).
		Recipe tSeated = GT6RecipeMaps.FREEZER.findRecipe(null, 32L, null, water(1000), new ItemStack(Items.BRICK, 1));
		assertNotNull(tSeated, "the seated snowball row answers (the brick stand-in)");
		assertFalse(GT6Circuits.hasConfigurationTag(tSeated.mInputs[0]), "the seated row carries no selector leg");
		assertEquals(Items.BRICK, tSeated.mInputs[0].getItem());
		assertEquals(16, tSeated.mDuration);

		// circuit-less water answers ONLY the item-less :664 (water500 -> snow layer, the
		// declared deviation) — no config-carrying row ever answers selector-less: the
		// pre-backfill first-hit shadow (:665 swallowing :666-:668 and the seated row) is gone
		Recipe tLayer = GT6RecipeMaps.FREEZER.findRecipe(null, 32L, null, water(1000));
		assertNotNull(tLayer, ":664 stays reachable circuit-less");
		assertEquals(0, tLayer.mInputs.length, "the circuit-less answer is the item-less :664, never a config row");
		assertEquals(32, tLayer.mDuration);
	}
}
