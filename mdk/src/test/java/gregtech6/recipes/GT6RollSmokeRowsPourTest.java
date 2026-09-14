package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The p29-w1-kinetic-roll-ladder smoke-row pour test (the GT6RecipeMapJsonLoaderTest
 * fixture posture): the FOUR data/gt6/recipe_maps JSON files this card ships are read
 * VERBATIM off the classpath (the main resources ride the test runtime classpath) and
 * poured through the real {@link GT6RecipeMapJsonLoader} seam — the whitelist keys, the
 * map resolution and the row schema all validated against the shipped bytes, not a test
 * re-statement of them. The gt6: item ids resolve through the injected resolver seam
 * onto distinct vanilla stand-ins (the synthetic-universe convention — identity is all
 * the recipe mechanics compare).
 */
public class GT6RollSmokeRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The iron-pair stand-ins, rebuilt per test — distinct entries, one per shipped gt6: id (identity only). The Items references stay OUT of class init (the fixture map is built inside the test lifecycle, after the JVM boot the suite's launcher listener runs). */
	private Map<String, Item> mStandins;

	@BeforeEach
	void freshGeneration() {
		mStandins = Map.ofEntries(
				Map.entry("gt6:ingot_iron", Items.IRON_INGOT),
				Map.entry("gt6:plate_iron", Items.IRON_TRAPDOOR),
				Map.entry("gt6:plate_curved_iron", Items.IRON_BARS),
				Map.entry("gt6:stick_iron", Items.IRON_SHOVEL),
				Map.entry("gt6:stick_long_iron", Items.IRON_AXE),
				Map.entry("gt6:ring_iron", Items.IRON_HELMET),
				Map.entry("gt6:spring_iron", Items.IRON_HORSE_ARMOR),
				Map.entry("gt6:rail_gt_iron", Items.RAIL),
				Map.entry("gt6:foil_iron", Items.PAPER),
				Map.entry("minecraft:clay_ball", Items.CLAY_BALL),
				Map.entry("gt6:plate_clay", Items.BRICK));
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> mStandins.get(aId.toString());
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.EMPTY;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped smoke-row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6RollSmokeRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped smoke-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	@Test
	void theFourShippedSmokeFilesPourIntoTheirMaps() throws Exception {
		for (String tKey : new String[] {"rollingmill", "rollbender", "rollformer", "clustermill"}) {
			pourShipped(tKey);
			int tExpectedRows = "rollingmill".equals(tKey) ? 2 : "rollbender".equals(tKey) ? 3 : 1; // rollingmill carries the ULV-rung clay leg; rollbender the W3 plate+stick+stickLong trio
			assertEquals(tExpectedRows, GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": the smoke rows poured");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
		}
		assertEquals(2, GT6RecipeMaps.ROLLING_MILL.mRecipeList.size(), "the rollingmill map holds the iron row + the ULV-rung clay row");
		assertEquals(3, GT6RecipeMaps.ROLL_BENDER.mRecipeList.size(), "the rollbender map holds the plate/stick/stickLong trio (task p29-w3-heat-smelter)");
		assertEquals(1, GT6RecipeMaps.ROLL_FORMER.mRecipeList.size(), "the rollformer map holds the corrected plate row");
		assertEquals(1, GT6RecipeMaps.CLUSTER_MILL.mRecipeList.size(), "the clustermill map holds the smoke row");
	}

	/**
	 * Task p29-w3-heat-smelter — the row-semantics correction pin: the Bender family rows
	 * are plate->plateCurved / stick->ring x2 / stickLong->spring x1 (Loader_Recipes_
	 * Handlers.java:298-300) and the RollFormer row is plate->railGt x4 (:314) — the
	 * shipped rollformer row had fed a stick while citing the OP.railGt product class.
	 */
	@Test
	void theBenderFamilyRowsMatchTheUpstreamHandlers() throws Exception {
		pourShipped("rollbender");
		pourShipped("rollformer");
		Recipe tRing = GT6RecipeMaps.ROLL_BENDER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == mStandins.get("gt6:stick_iron"))
				.findFirst().orElseThrow();
		assertEquals(2, tRing.mOutputs[0].getCount(), "stick -> ring x2 (:299)");
		Recipe tSpring = GT6RecipeMaps.ROLL_BENDER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == mStandins.get("gt6:stick_long_iron"))
				.findFirst().orElseThrow();
		assertEquals(1, tSpring.mOutputs[0].getCount(), "stickLong -> spring x1 (:300)");
		Recipe tRail = GT6RecipeMaps.ROLL_FORMER.mRecipeList.iterator().next();
		assertEquals(mStandins.get("gt6:plate_iron"), tRail.mInputs[0].getItem(), "the rollformer row consumes a PLATE (:314)");
		assertEquals(4, tRail.mOutputs[0].getCount(), "plate -> railGt x4 (:314)");
	}

	@Test
	void theRollingmillSmokeRowSharesTheUlvRungsMap() throws Exception {
		// the same-map co-existence at the row level: the RU smoke row and the p28 ULV
		// electric rung consume ONE ROLLING_MILL map — the port pours the row, both
		// machine domains find it through the same mRecipeList
		pourShipped("rollingmill");
		Recipe tRow = GT6RecipeMaps.ROLLING_MILL.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1 && r.mInputs[0].getItem() == mStandins.get("gt6:ingot_iron"))
				.findFirst().orElseThrow();
		assertEquals(16L, tRow.mEUt, "the smoke row's eut 16 (the upstream RollingMill row shape, MultiItemFood.java:167)");
		assertEquals(32L, tRow.mDuration, "the smoke row's duration 32");
		assertEquals(GT6RecipeMaps.ROLLING_MILL, GTMachinesSmokeMapAccess.rollingMillMap(),
				"the map the row carrier's lazy supplier resolves IS the current (poured) map instance");
	}
}

/** The tiny accessor the same-map pin rides (the row carrier's supplier is a lambda). */
final class GTMachinesSmokeMapAccess {
	static RecipeMap rollingMillMap() {
		return gregtech6.registry.GTMachines.ROLLINGMILL_RU_ROWS.get(0).recipes().get();
	}
}
