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
public class GT6RollSmokeRowsPourTest {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The iron-pair stand-ins — distinct entries, one per shipped gt6: id (identity only). */
	private static final Map<String, Item> IRON_PAIR = Map.of(
			"gt6:ingot_iron", Items.IRON_INGOT,
			"gt6:plate_iron", Items.IRON_TRAPDOOR,
			"gt6:plate_curved_iron", Items.IRON_BARS,
			"gt6:rod_iron", Items.IRON_SHOVEL,
			"gt6:rail_gt_iron", Items.RAIL,
			"gt6:foil_iron", Items.PAPER);

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> IRON_PAIR.get(aId.toString());
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
			assertEquals(1, GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": exactly the one smoke row poured");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
		}
		assertEquals(1, GT6RecipeMaps.ROLLING_MILL.mRecipeList.size(), "the rollingmill map holds the smoke row");
		assertEquals(1, GT6RecipeMaps.ROLL_BENDER.mRecipeList.size(), "the rollbender map holds the smoke row");
		assertEquals(1, GT6RecipeMaps.ROLL_FORMER.mRecipeList.size(), "the rollformer map holds the smoke row");
		assertEquals(1, GT6RecipeMaps.CLUSTER_MILL.mRecipeList.size(), "the clustermill map holds the smoke row");
	}

	@Test
	void theRollingmillSmokeRowSharesTheUlvRungsMap() throws Exception {
		// the same-map co-existence at the row level: the RU smoke row and the p28 ULV
		// electric rung consume ONE ROLLING_MILL map — the port pours the row, both
		// machine domains find it through the same mRecipeList
		pourShipped("rollingmill");
		Recipe tRow = GT6RecipeMaps.ROLLING_MILL.mRecipeList.iterator().next();
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
