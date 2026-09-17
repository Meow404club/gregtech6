package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
 * The p31-qu-a-foundation smoke-row pour test (the GT6HeatSmelterSmokeRowsPourTest
 * fixture posture): the THREE data/gt6/recipe_maps JSON files this card ships (massfab /
 * replicator / scannermolecular) are read VERBATIM off the classpath and poured through
 * the real {@link GT6RecipeMapJsonLoader} seam — the new POURABLE keys, the map
 * resolution and the row schema all validated against the shipped bytes. The gt6
 * matter/ender fluid ids resolve through the injected resolver seam onto vanilla
 * stand-ins (the synthetic-universe convention — identity is all the recipe mechanics
 * compare); the vanilla iron_ingot/ender_pearl/paper ids are the real registry entries.
 * EMPTY stays the miss so an unregistered id in a shipped row is a LOUD bad row, not a
 * silent stand-in.
 */
public class GT6QuSmokeRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> switch (aId.getPath()) {
			case "iron_ingot" -> Items.IRON_INGOT;
			case "ender_pearl" -> Items.ENDER_PEARL;
			case "paper" -> Items.PAPER;
			default -> Items.AIR;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> switch (aId.getPath()) {
			case "chargedmatter", "neutralmatter", "enderpearl_molten" -> Fluids.LAVA; // identity stand-ins
			default -> Fluids.EMPTY; // a miss is LOUD (the unregistered-id bad row)
		};
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped smoke-row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6QuSmokeRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped smoke-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	@Test
	void theThreeShippedSmokeFilesPourIntoTheirMaps() throws Exception {
		for (String tKey : new String[] {"massfab", "replicator", "scannermolecular"}) {
			pourShipped(tKey);
			assertEquals(1, GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": the poured row count");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
			assertEquals(1, GT6RecipeMapJsonLoader.mapFor(tKey).mRecipeList.size(), tKey + ": the map holds its smoke row");
		}
	}

	/** The massfab smoke row = the iron disintegration stand-in (Loader_Recipes_Other.java:971-972 constants over MT.java:414 Fe 26p/30n). */
	@Test
	void theMassfabRowCarriesTheIronDisintegrationConstants() throws Exception {
		pourShipped("massfab");
		Recipe tRow = GT6RecipeMaps.MASSFAB.mRecipeList.iterator().next();
		assertEquals(1L, tRow.mEUt, "the disintegration voltage 1 (Loader_Recipes_Other.java:971)");
		assertEquals(7340032L, tRow.mDuration, "(protons 26 + neutrons 30) × 131072 — the :971 duration formula over MT.java:414");
		assertEquals(1, tRow.mInputs.length, "one item input (the iron_ingot stand-in)");
		assertEquals(0, tRow.mOutputs.length, "zero item outputs — matter only");
		assertEquals(2, tRow.mFluidOutputs.length, "the two matter carriers out");
		assertEquals(26, tRow.mFluidOutputs[0].getAmount(), "1 mB = 1 proton: Fe charges to 26 mB");
		assertEquals(30, tRow.mFluidOutputs[1].getAmount(), "1 mB = 1 neutron: Fe neutrals to 30 mB");
	}

	/** The replicator smoke row = the :929 molten-enderpearl → ender-pearl row verbatim. */
	@Test
	void theReplicatorRowCarriesTheEnderReplicationConstants() throws Exception {
		pourShipped("replicator");
		Recipe tRow = GT6RecipeMaps.REPLICATOR.mRecipeList.iterator().next();
		assertEquals(16L, tRow.mEUt, "the :929 eut 16");
		assertEquals(144L, tRow.mDuration, "the :929 duration 144");
		assertEquals(1, tRow.mFluidInputs.length, "one fluid input");
		assertEquals(144, tRow.mFluidInputs[0].getAmount(), "one L-unit of molten enderpearls = 144 mB (the FL.Ender per-unit)");
		assertEquals(1, tRow.mOutputs.length, "one item output");
		assertSame(Items.ENDER_PEARL, tRow.mOutputs[0].getItem(), "the replicated ender pearl (the gem-form stand-in)");
	}

	/** The scannermolecular smoke row = the DECLARED 2-in/1-out stand-in of the runtime USB synthesis. */
	@Test
	void theScannerRowKeepsTheTwoInOneOutMapShape() throws Exception {
		pourShipped("scannermolecular");
		Recipe tRow = GT6RecipeMaps.SCANNER_MOLECULAR.mRecipeList.iterator().next();
		assertEquals(512L, tRow.mEUt, "the scan eut 512 (RecipeMapScannerMolecular.java:57 power face)");
		assertEquals(512L, tRow.mDuration, "the declared stand-in duration");
		assertEquals(2, tRow.mInputs.length, "the map's 2-in shape (scanned item + data medium stand-ins)");
		assertEquals(1, tRow.mOutputs.length, "the map's 1-out shape (the medium back, scan NBT being v1-schema-out)");
	}
}
