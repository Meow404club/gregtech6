package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The four RCON process-chain pinned rows are LIVE canon rows (task
 * process-cluster-diesel-rig-red, the OFFLINE half). The red: the recipe-data cards
 * rebuilt cutter/squeezer/sluice/pressurewasher from the upstream walks and declared
 * the p29 smoke rows REPLACED (6b65aa8a4f / 53c36c07c3 / ddbd805b80 / 9c45644e04),
 * while the chains kept feeding the dead inputs — checkRecipe correctly answered
 * DID_NOT_FIND (progress=0/0, the coolant tank untouched) and all four chains went
 * red with the machine-side energy faces fully green (the diesel burned, the
 * machine held energy=mInputMin). The chains re-pin to live rows; THIS test is the
 * ratchet that keeps them welded to the canon: every pinned row must
 * <ol>
 * <li>survive the shipped-file pour (the loader's real row seam), and</li>
 * <li>answer {@link RecipeMap#findRecipe} with the stand-in stack of the chain's
 * data-merge feed (the FOUND half of checkRecipe, offline), at the exact
 * eUt/duration the chains' tick ladders are computed from, and</li>
 * <li>keep the determinism shape the chains' verdicts assert (the chance-free
 * output legs; the sluice head output chance-free — its by-product tail legs are
 * 3% and deliberately unpinned there).</li>
 * </ol>
 *
 * <p>The pinned faces (chain file : row): buzzsaw : cutter stick-&gt;bolt x4
 * (Loader_Recipes_Handlers.java:635, the minecraft:water leg — eUt 32 is why the
 * chain's T1 diesel rig is the STEEL engine); squeezer : the :816 sunflower
 * statement (eUt 16 duration 16 — the ladder numbers carry over byte-identical);
 * sluice : the :385-420 ore-walk head row (crushed_lithium, water 900, 144t);
 * pressurewasher : the RESTORED p29 fixture row (coarse_dirt, water 100, 16t) — the
 * b2c-walk rows WERE declared-out live on the unregistered bark dust (the
 * GT6ModDrivers seeded-JVM divergence; see the wash file head) and REVIVED with task
 * material-foreign-domain-registration (ruling id1745-4: the bark dust registers
 * unconditionally), so the fixture now rides beside 28 live walk rows.
 *
 * <p>Hermetic form (the hermetic-pour-tests law): the generation reset participates
 * before the init/pour and the live seams are restored after each test. The stand-in
 * resolvers resolve EVERY id (gt6 catchall), with the fluid fixture pinning
 * minecraft:water apart from the gt6 water family — so within each walked file only
 * the true minecraft:water leg can answer the water-tank query.
 */
public class GT6ProcessChainPinnedRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset(); // hermetic: the reset participates BEFORE the init (the pour window reopens)
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			String tId = aId.toString();
			return switch (tId) {
				case "gt6:stick_wood" -> Items.STICK; // the buzzsaw feed stand-in
				case "gt6:crushed_lithium" -> Items.RAW_IRON; // the sluice feed stand-in
				case "minecraft:sunflower" -> Items.SUNFLOWER; // the squeezer feed, real
				case "minecraft:coarse_dirt" -> Items.COARSE_DIRT; // the washer feed, real
				default -> aId.getNamespace().equals("gt6") ? Items.IRON_INGOT : Items.BRICK; // identity catchall
			};
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId ->
				"minecraft:water".equals(aId.toString()) ? Fluids.WATER : Fluids.LAVA; // water pinned apart from the gt6 family
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads the shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6ProcessChainPinnedRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The row keyed on its first input item (plus the minecraft:water leg where the walk fans 4 fluid legs). */
	private JsonObject findRow(JsonArray aRows, String aInput, boolean aWaterLeg) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.has("inputs")) continue;
			JsonArray tInputs = tRow.getAsJsonArray("inputs");
			if (tInputs.size() <= 0 || !aInput.equals(tInputs.get(0).getAsJsonObject().get("item").getAsString())) continue;
			if (aWaterLeg) {
				if (!tRow.has("fluidInputs")) continue;
				boolean tWater = false;
				for (JsonElement tFluid : tRow.getAsJsonArray("fluidInputs")) {
					if ("minecraft:water".equals(tFluid.getAsJsonObject().get("fluid").getAsString())) tWater = true;
				}
				if (!tWater) continue;
			}
			return tRow;
		}
		return null;
	}

	private static void assertChanceFree(JsonArray aOutputs, String aLabel) {
		for (JsonElement tOut : aOutputs) {
			JsonObject tSlot = tOut.getAsJsonObject();
			assertTrue(!tSlot.has("chance") || tSlot.get("chance").getAsInt() >= 10000,
					aLabel + ": the chain-asserted output leg must be deterministic, got " + tSlot);
		}
	}

	/** The map keys resolve to their own RMs (the pour seam's family constants). */
	@Test
	public void theFourKeysResolveToTheirOwnMaps() {
		assertSame(GT6RecipeMaps.CUTTER, GT6RecipeMapJsonLoader.mapFor("cutter"));
		assertSame(GT6RecipeMaps.SQUEEZER, GT6RecipeMapJsonLoader.mapFor("squeezer"));
		assertSame(GT6RecipeMaps.SLUICE, GT6RecipeMapJsonLoader.mapFor("sluice"));
		assertSame(GT6RecipeMaps.PRESSURE_WASHER, GT6RecipeMapJsonLoader.mapFor("pressurewasher"));
	}

	/** buzzsaw: the stick-&gt;bolt row is live at eUt 32 / 192t, water-mandatory, one deterministic x4 output. */
	@Test
	public void theBuzzsawPinnedRowFindsAndDrains() throws Exception {
		JsonArray tRows = pourShipped("cutter");
		JsonObject tRow = findRow(tRows, "gt6:stick_wood", true);
		assertNotNull(tRow, "the cutter stick->bolt minecraft:water leg seats in the shipped file");
		assertEquals(32, tRow.get("eut").getAsLong(), "the walk's fixed eUt (the steel-diesel rig number)");
		assertEquals(192, tRow.get("duration").getAsLong(), "the tick-ladder duration");
		JsonArray tOutputs = tRow.getAsJsonArray("outputs");
		assertEquals(1, tOutputs.size(), "one output leg");
		assertEquals(4, tOutputs.get(0).getAsJsonObject().get("count").getAsInt(), "the x4 bolt leg");
		assertChanceFree(tOutputs, "cutter stick->bolt");

		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("cutter");
		Recipe tHit = tMap.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack(Items.STICK));
		assertNotNull(tHit, "the chain feed finds the row (the FOUND half of checkRecipe)");
		assertEquals(32, tHit.mEUt, "the found row's eUt");
		assertEquals(192, tHit.mDuration, "the found row's duration");
		assertNull(tMap.findRecipe(null, 64, ItemStack.EMPTY, new FluidStack[0], new ItemStack(Items.STICK)),
				"the dry machine finds nothing (the coolant-mandatory face the T1 negative drives)");
	}

	/** squeezer: the :816 sunflower row is live at 16/16, one dye-x2 leg + the 100 L oil leg, both deterministic. */
	@Test
	public void theSqueezerPinnedRowFindsAndDrains() throws Exception {
		JsonArray tRows = pourShipped("squeezer");
		JsonObject tRow = findRow(tRows, "minecraft:sunflower", false);
		assertNotNull(tRow, "the :816 sunflower row seats in the shipped file");
		assertEquals(16, tRow.get("eut").getAsLong(), "the upstream 16 EUt (the ladder numbers ride it)");
		assertEquals(16, tRow.get("duration").getAsLong(), "the upstream 16t");
		JsonArray tOutputs = tRow.getAsJsonArray("outputs");
		assertEquals(1, tOutputs.size(), "one item leg");
		assertEquals("minecraft:yellow_dye", tOutputs.get(0).getAsJsonObject().get("item").getAsString(), "the dye carrier");
		assertEquals(2, tOutputs.get(0).getAsJsonObject().get("count").getAsInt(), "the x2 dye leg");
		assertChanceFree(tOutputs, "squeezer sunflower");
		assertEquals("gt6:sunfloweroil", tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(),
				"the oil leg the fluid stat reads");

		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("squeezer");
		Recipe tHit = tMap.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[0], new ItemStack(Items.SUNFLOWER));
		assertNotNull(tHit, "the chain feed finds the row");
		assertEquals(16, tHit.mEUt, "the found row's eUt");
		assertEquals(16, tHit.mDuration, "the found row's duration");
	}

	/** sluice: the crushed-lithium head row is live at 16/144, water 900, the head output chance-free. */
	@Test
	public void theSluicePinnedRowFindsAndDrains() throws Exception {
		JsonArray tRows = pourShipped("sluice");
		JsonObject tRow = findRow(tRows, "gt6:crushed_lithium", true);
		assertNotNull(tRow, "the ore-walk head row seats in the shipped file");
		assertEquals(16, tRow.get("eut").getAsLong(), "the walk eUt (the bronze-diesel rig number)");
		assertEquals(144, tRow.get("duration").getAsLong(), "the tick-ladder duration");
		assertEquals(900, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the water leg the tank verdict reads (1000 - 900 = 100)");
		JsonArray tOutputs = tRow.getAsJsonArray("outputs");
		assertTrue(tOutputs.size() >= 1, "at least the head output");
		JsonObject tHead = tOutputs.get(0).getAsJsonObject();
		assertEquals("gt6:crushed_purified_lithium", tHead.get("item").getAsString(), "the head carrier");
		assertTrue(!tHead.has("chance") || tHead.get("chance").getAsInt() >= 10000,
				"the head output the chains assert must be deterministic (the 3% tails are unpinned)");

		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("sluice");
		Recipe tHit = tMap.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack(Items.RAW_IRON));
		assertNotNull(tHit, "the chain feed finds the row");
		assertEquals(16, tHit.mEUt, "the found row's eUt");
		assertEquals(144, tHit.mDuration, "the found row's duration");
	}

	/** pressurewasher: the restored p29 fixture row is the map's ONE live row at 16/16, water 100, a deterministic dirt leg. */
	@Test
	public void thePressureWasherPinnedRowFindsAndDrains() throws Exception {
		JsonArray tRows = pourShipped("pressurewasher");
		JsonObject tRow = findRow(tRows, "minecraft:coarse_dirt", true);
		assertNotNull(tRow, "the restored fixture row seats in the shipped file");
		assertEquals(16, tRow.get("eut").getAsLong(), "the fixture eUt (the bronze-diesel rig number)");
		assertEquals(16, tRow.get("duration").getAsLong(), "the tick-ladder duration");
		assertEquals(100, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the water leg the consumption verdict reads (1000 - 2x100 = 800)");
		JsonArray tOutputs = tRow.getAsJsonArray("outputs");
		assertEquals(1, tOutputs.size(), "one dirt leg");
		assertChanceFree(tOutputs, "pressurewasher fixture");

		// the walk rows WERE declared-out LIVE (gt6:dust_bark never registered in a seeded JVM
		// — the BINNIE-domain foreign material, GT6ModDrivers.seedFromEnvironment) and REVIVED
		// with task material-foreign-domain-registration (ruling id1745-4: the bark dust
		// registers unconditionally) — the fixture row keeps its unique coarse_dirt input,
		// so the FOUND half still answers for IT alone.
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor("pressurewasher");
		Recipe tHit = tMap.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack(Items.COARSE_DIRT));
		assertNotNull(tHit, "the chain feed finds the fixture row (the FOUND half of checkRecipe)");
		assertEquals(16, tHit.mEUt, "the found row's eUt");
		assertEquals(16, tHit.mDuration, "the found row's duration");
		assertNull(tMap.findRecipe(null, 64, ItemStack.EMPTY, new FluidStack[0], new ItemStack(Items.COARSE_DIRT)),
				"the dry machine finds nothing (the coolant-mandatory face the T2 negative drives)");
	}
}
