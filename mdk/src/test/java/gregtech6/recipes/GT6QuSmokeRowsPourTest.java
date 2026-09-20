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
			// the p31-massfab Ender rows' material items — enumerated EXACTLY so a typo in a
			// shipped row stays a LOUD bad row (the default miss)
			case "dust_div72_dilithium", "dust_tiny_dilithium", "dust_small_dilithium", "dust_dilithium",
					"gem_dilithium", "block_dust_dilithium", "block_gem_dilithium",
					"dust_div72_ancient_debris", "dust_tiny_ancient_debris", "dust_small_ancient_debris",
					"dust_ancient_debris", "ingot_ancient_debris", "block_dust_ancient_debris",
					"block_ingot_ancient_debris" -> Items.IRON_INGOT; // identity stand-ins
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

	/** The p31-massfab massfab file = the 14 Ender smoke rows (Loader_Recipes_Other.java:915-928 verbatim constants). */
	@Test
	void theThreeShippedSmokeFilesPourIntoTheirMaps() throws Exception {
		pourShipped("massfab");
		assertEquals(14, GT6RecipeMapJsonLoader.pouredCount("massfab"), "massfab: the poured row count — the :915-928 Ender walk");
		assertNotNull(GT6RecipeMapJsonLoader.mapFor("massfab"), "massfab: the whitelist key resolves its map");
		assertEquals(14, GT6RecipeMapJsonLoader.mapFor("massfab").mRecipeList.size(), "massfab: the map holds its 14 rows");
		for (String tKey : new String[] {"replicator", "scannermolecular"}) {
			pourShipped(tKey);
			assertEquals(1, GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": the poured row count");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
			assertEquals(1, GT6RecipeMapJsonLoader.mapFor(tKey).mRecipeList.size(), tKey + ": the map holds its smoke row");
		}
	}

	/**
	 * The massfab smoke rows = the Ender walk (:915-928): the :915 baseline row (dustDiv72
	 * Dilithium → 144 mB, dur 144, eut 16) and the :921 blockGem top row (L×12960). The
	 * qu-a iron-disintegration stand-in retired with the card-C dynamic walk
	 * (GT6RecipesMassfab) — the JSON rows would have duplicated the walked iron row.
	 */
	@Test
	void theMassfabRowsCarryTheEnderConstants() throws Exception {
		pourShipped("massfab");
		Recipe tBaseline = null, tTop = null;
		for (Recipe tRow : GT6RecipeMaps.MASSFAB.mRecipeList) {
			if (tRow.mDuration == 144 && tRow.mInputs.length == 1) tBaseline = tRow;
			if (tRow.mDuration == 186624) tTop = tRow;
		}
		assertNotNull(tBaseline, "the :915 baseline row (dustDiv72 → L, dur 144)");
		assertNotNull(tTop, "the :921 blockGem top row (L*12960, dur 144*1296)");
		assertEquals(16L, tBaseline.mEUt, "the :915 eut 16");
		assertEquals(144, tBaseline.mFluidOutputs[0].getAmount(), "one L-unit of molten enderpearls = 144 mB (the FL.Ender per-unit)");
		assertEquals(0, tBaseline.mOutputs.length, "zero item outputs — ender fluid only (ZL_IS)");
		assertEquals(1, tBaseline.mFluidOutputs.length, "one fluid output per row");
		assertEquals(16L, tTop.mEUt, "the :921 eut 16");
		assertEquals(1866240, tTop.mFluidOutputs[0].getAmount(), "L*12960 = 1866240 mB (the :921 blockGem arm)");
	}

	/** The replicator smoke row = the :929 molten-enderpearl → ender-pearl row verbatim. */
	@Test
	void theReplicatorRowCarriesTheEnderReplicationConstants() throws Exception {
		pourShipped("replicator");
		// task p32-qu-scanner-replicator — the map now carries the :929 row PLUS the
		// :941-946 molten-redstone six; the ender row is found by its output (the set is
		// unordered, the amount-144 redstone row would be a coin-flip on iterator().next()).
		// The redstone constants themselves are pinned exhaustively in GT6QuMachinesTest.
		Recipe tRow = null;
		for (Recipe tScan : GT6RecipeMaps.REPLICATOR.mRecipeList) {
			if (tScan.mOutputs.length == 1 && tScan.mOutputs[0].getItem() == Items.ENDER_PEARL) tRow = tScan;
		}
		assertNotNull(tRow, "the :929 ender row");
		assertEquals(16L, tRow.mEUt, "the :929 eut 16");
		assertEquals(144L, tRow.mDuration, "the :929 duration 144");
		assertEquals(1, tRow.mFluidInputs.length, "one fluid input");
		assertEquals(144, tRow.mFluidInputs[0].getAmount(), "one L-unit of molten enderpearls = 144 mB (the FL.Ender per-unit)");
		assertEquals(1, tRow.mOutputs.length, "one item output");
		assertSame(Items.ENDER_PEARL, tRow.mOutputs[0].getItem(), "the replicated ender pearl (the gem-form stand-in)");
	}

	/**
	 * The Ender rows' item ids are the GTMaterialItems id-composition outputs (task
	 * p31-massfab): every hand-typed id in the shipped massfab.json must equal
	 * {@code itemIdOf(prefix, material)} so the LIVE pour resolves them — a typo here is
	 * a LOUD bad row at load and a silently missing NEI row.
	 */
	@Test
	void theMassfabEnderRowItemIdsMatchTheRegistryComposition() {
		gregtech6.registry.GTMaterialItems.initMaterials(); // idempotent — the offline universe
		gregapi.oredict.OreDictMaterial[] tMaterials = {gregapi.data.MT.Dilithium, gregapi.data.MT.AncientDebris};
		// the PATH forms — itemIdOf composes the registry path; the shipped rows carry the
		// same paths under the gt6: namespace
		String[][] tArms = {
				{"dust_div72_dilithium", "dust_tiny_dilithium", "dust_small_dilithium", "dust_dilithium", "gem_dilithium", "block_dust_dilithium", "block_gem_dilithium"},
				{"dust_div72_ancient_debris", "dust_tiny_ancient_debris", "dust_small_ancient_debris", "dust_ancient_debris", "ingot_ancient_debris", "block_dust_ancient_debris", "block_ingot_ancient_debris"}};
		gregapi.oredict.OreDictPrefix[][] tPrefixes = {
				{gregapi.data.OP.dustDiv72, gregapi.data.OP.dustTiny, gregapi.data.OP.dustSmall, gregapi.data.OP.dust, gregapi.data.OP.gem, gregapi.data.OP.blockDust, gregapi.data.OP.blockGem},
				{gregapi.data.OP.dustDiv72, gregapi.data.OP.dustTiny, gregapi.data.OP.dustSmall, gregapi.data.OP.dust, gregapi.data.OP.ingot, gregapi.data.OP.blockDust, gregapi.data.OP.blockIngot}};
		for (int m = 0; m < 2; m++) {
			for (int a = 0; a < 7; a++) {
				assertEquals(tArms[m][a], gregtech6.registry.GTMaterialItems.itemIdOf(tPrefixes[m][a], tMaterials[m]),
						"the shipped row id matches the composition (" + tArms[m][a] + ")");
			}
		}
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
