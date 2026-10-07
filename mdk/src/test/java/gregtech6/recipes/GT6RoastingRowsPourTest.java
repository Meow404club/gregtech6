package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import gregapi.data.TD;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The w4-eu-bridge Roasting acceptance test: the row-table pins (the 4 Roasting Oven
 * MachineRow rows, Loader_MultiTileEntities.java:1386-1389 — the NON-standard parallel
 * ladder {1, 2, 4, 8} with NO NBT_PARALLEL_DURATION key, the HU carrier, the "roaster"
 * texture, the Heat_T hardness ladder) + the RM.java:79 map constants + the Boudouard
 * rows pour (the shipped roasting.json verbatim through the real JSON loader seam —
 * dust + CO2 in → CO out, the Recipe.java:187 arm; the amounts 3U/4U/6U/8U/12U/16U at
 * the gas-native 1000 mB/U face — the b2-residual-maps DECLARED CORRECTION: the gas
 * carrier native amount is 1000 (Loader_Fluids.java:660 walk -> FL.java:1080
 * AmountPerUnit 1000 -> FL.java:1124 STATE_GASEOUS bind -> OreDictMaterial.java:1315-1319
 * units(aAmount, mGasUnit=U, native)); the original U=144 numbers came from the :390
 * LiCl.liquid arm, whose createMolten native IS 144 — correct for liquids, misapplied
 * to the gas accessor, a 6.944x understatement). The test also rides the b2-residual-maps
 * additions (the :423-:427 oxide rows and the :435-:440 combustion rows). The gt6:
 * chemical fluids resolve onto vanilla stand-ins (the synthetic-universe convention —
 * identity is all the row mechanics compare); the input dust ids are recorded and pinned.
 */
public class GT6RoastingRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	private final Set<String> tRequestedItems = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		tRequestedItems.clear();
		// the gt6: chemical fluids are not registered offline — stand-ins, identity only
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			tRequestedItems.add(aId.getNamespace() + ":" + aId.getPath());
			return Items.COAL; // identity only — the row mechanics never read item properties
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> aId.getPath().equals("carbonmonoxide") ? Fluids.LAVA : Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	// ---------------------------------------------------------------------------
	// the row-table pins (Loader :1386-1389)
	// ---------------------------------------------------------------------------

	@Test
	void theFourRoastingRowsCarryTheUpstreamColumns() {
		assertEquals(4, gregtech6.registry.GTMachines.ROASTING_ROWS.size(), "the 4-ladder");
		String[] tPaths = {"roasting_oven", "roasting_oven_t2", "roasting_oven_t3", "roasting_oven_t4"};
		int[] tIds = {20171, 20172, 20173, 20174};
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F}; // the Heat_T hardness ladder, the T2 special case
		int[] tParallels = {1, 2, 4, 8}; // the NON-standard ladder (the CENTRIFUGE_PARALLEL shape, NOT the 4/8/16/32 band)
		String[] tMats = {"steel", "invar", "titanium", "tungsten_carbide"};
		for (int i = 0; i < 4; i++) {
			GTBasicMachineBlockProxy tRow = new GTBasicMachineBlockProxy(gregtech6.registry.GTMachines.ROASTING_ROWS.get(i));
			assertEquals(tPaths[i], tRow.path(), "the registry path of rung " + i);
			assertEquals(tIds[i], tRow.metaId(), "the meta id of rung " + i);
			assertEquals(tHardness[i], tRow.hardness(), "the hardness of rung " + i);
			assertEquals(tParallels[i], tRow.parallel(), "the parallel ladder of rung " + i + " — the 1/2/4/8 对拍");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION key on the Roasting rows (:1386-1389)");
			assertEquals("roaster", tRow.texture(), "the NBT_TEXTURE column");
			assertEquals(tMats[i], tRow.matSlug(), "the Heat_T material word of rung " + i);
			assertSameType(TD.Energy.HU, tRow.energyType(), "the HU carrier of rung " + i);
			assertEquals(i, tRow.tier(), "the tier IS the rung index (the TIER_INPUTS selector)");
			assertNotNull(tRow.recipes(), "the recipe-map supplier armed");
		}
	}

	@Test
	void theRoastingMapCarriesTheRm79Constants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.ROASTING, "the ROASTING map registered");
		assertEquals("gt.recipe.roaster", GT6RecipeMaps.ROASTING.mNameInternal, "the RM.java:79 internal name");
		assertEquals("Roaster", GT6RecipeMaps.ROASTING.mNameLocal, "the local name");
		assertEquals(1, GT6RecipeMaps.ROASTING.mInputItemsCount, "items 1/3/1 — the input slots");
		assertEquals(3, GT6RecipeMaps.ROASTING.mOutputItemsCount, "items 1/3/1 — the output slots");
		assertEquals(1, GT6RecipeMaps.ROASTING.mMinimalInputItems, "items 1/3/1 — the item minimum");
		assertEquals(1, GT6RecipeMaps.ROASTING.mInputFluidCount, "fluids 1/1/1 — the input slots");
		assertEquals(1, GT6RecipeMaps.ROASTING.mOutputFluidCount, "fluids 1/1/1 — the output slots");
		assertEquals(1, GT6RecipeMaps.ROASTING.mMinimalInputFluids, "fluids 1/1/1 — the fluid minimum");
		assertEquals(2, GT6RecipeMaps.ROASTING.mMinimalInputs, "MIN 2 — the total-input gate the row shape satisfies");
	}

	// ---------------------------------------------------------------------------
	// the Boudouard rows pour (the shipped roasting.json verbatim)
	// ---------------------------------------------------------------------------

	@Test
	void theFiveBoudouardRowsPourWithTheUpstreamParameters() throws Exception {
		String tPath = "/data/gt6/recipe_maps/roasting.json";
		try (InputStream tStream = GT6RoastingRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped roasting.json rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", "roasting"), JsonParser.parseString(tJson)));
		}
		// the b2-residual-maps additions grew the file: 5 corrected Boudouard + 5 oxide + 5 combustion
		assertEquals(15, GT6RecipeMapJsonLoader.pouredCount("roasting"), "the fifteen rows poured under the roasting key");
		assertEquals(15, GT6RecipeMaps.ROASTING.mRecipeList.size(), "the fifteen rows");
		// the input ids: the five base carbon dusts (the mToThis-walk fold) + the five oxide metals
		Set<String> tExpected = Set.of("gt6:dust_carbon", "gt6:dust_charcoal", "gt6:dust_coal", "gt6:dust_coal_coke", "gt6:dust_diamond",
				"gt6:dust_lithium", "gt6:dust_vanadium", "gt6:dust_niobium", "gt6:dust_tantalum", "gt6:dust_chromium",
				// and the five oxide outputs (the resolver records both legs)
				"gt6:dust_small_lithium_oxide", "gt6:dust_small_vanadium_pentoxide", "gt6:dust_small_niobium_pentoxide",
				"gt6:dust_small_tantalum_pentoxide", "gt6:dust_chromium_dioxide");
		assertEquals(tExpected, tRequestedItems, "the concrete dust ids (the v1 no-tag form)");
		// the per-row pairs, split by the output fluid; the GAS UNIT CORRECTION (b2-residual-maps):
		// the gas carrier native amount is 1000 mB per U (FL.java:1080/:1124 -> OreDictMaterial.java:1315-1319),
		// so the Boudouard pairs are 3000→4000 / 6000→8000 / 12000→16000 (the former 432-family numbers
		// misapplied the liquid-arm U=144 to the gas accessor)
		Map<Long, Long> tBoudouard = new java.util.HashMap<>(), tCombustion = new java.util.HashMap<>();
		int tOxides = 0;
		for (Recipe tRow : GT6RecipeMaps.ROASTING.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "the row eut 16 (Loader_Recipes_Chem.java:398-440)");
			assertEquals(1, tRow.mInputs.length, "one dust input");
			if (tRow.mFluidOutputs.length == 1 && tRow.mFluidOutputs[0].getFluid() == Fluids.LAVA) {
				// the resolver stand-in for carbonmonoxide (the Boudouard output fluid)
				assertEquals(16L, tRow.mDuration, "the Boudouard duration 16");
				assertEquals(1, tRow.mFluidInputs.length, "ONE fluid INPUT — the CO2 (the Recipe.java:187 arm)");
				assertEquals(0, tRow.mOutputs.length, "no item output");
				tBoudouard.put((long) tRow.mFluidInputs[0].getAmount(), (long) tRow.mFluidOutputs[0].getAmount());
			} else if (tRow.mFluidOutputs.length == 1) {
				// the :435-:440 combustion run: O2 in -> CO2 out, same 16-tick shape
				assertEquals(16L, tRow.mDuration, "the combustion duration 16");
				assertEquals(0, tRow.mOutputs.length, "no item output");
				tCombustion.put((long) tRow.mFluidInputs[0].getAmount(), (long) tRow.mFluidOutputs[0].getAmount());
			} else {
				// the :423-:427 oxide run: O2 in -> oxide dust out, 128 ticks
				assertEquals(128L, tRow.mDuration, "the oxide duration 128");
				assertEquals(1, tRow.mFluidInputs.length, "the O2 input");
				assertEquals(1, tRow.mOutputs.length, "one oxide dust output");
				tOxides++;
			}
		}
		assertEquals(3, tBoudouard.size(), "three distinct CO2 amounts (the 3U/6U/12U ladder)");
		assertEquals(4000L, tBoudouard.get(3000L).longValue(), "3U CO2 -> 4U CO at the 1000 mB/U gas face");
		assertEquals(8000L, tBoudouard.get(6000L).longValue(), "6U CO2 -> 8U CO");
		assertEquals(16000L, tBoudouard.get(12000L).longValue(), "12U CO2 -> 16U CO");
		assertEquals(3, tCombustion.size(), "three distinct O2 amounts (the 2000/4000/8000 ladder)");
		assertEquals(3000L, tCombustion.get(2000L).longValue(), ":435/:436 O2 2000 -> 3U CO2");
		assertEquals(6000L, tCombustion.get(4000L).longValue(), ":437/:438 O2 4000 -> 6U CO2");
		assertEquals(12000L, tCombustion.get(8000L).longValue(), ":440 O2 8000 -> 12U CO2");
		assertEquals(5, tOxides, "the five :423-:427 oxide rows");
	}

	// ---------------------------------------------------------------------------
	// the bridge ladders pin (the ① half-rate table's row face)
	// ---------------------------------------------------------------------------

	@Test
	void theBridgeLaddersCarryTheHalfRatePairs() {
		long[][] tLadder = {
				{32, 16}, {128, 64}, {512, 256}, {2048, 1024}, {8192, 4096}};
		assertEquals(5, gregtech6.registry.GTMachines.BRIDGE_INPUTS.length, "the five rungs (VN[1..5])");
		for (int i = 0; i < 5; i++) {
			assertEquals(tLadder[i][0], gregtech6.registry.GTMachines.BRIDGE_INPUTS[i], "the in column of rung " + i);
			assertEquals(tLadder[i][1], gregtech6.registry.GTMachines.BRIDGE_OUTPUTS[i], "the out column of rung " + i + " = in/2 exactly");
			assertEquals(gregtech6.registry.GTMachines.BRIDGE_INPUTS[i], gregtech6.registry.GTMachines.BRIDGE_OUTPUTS[i] * 2, "the in×2=out 对拍 (the units() half-rate)");
		}
		// the Roasting Oven ladder rides the TIER_INPUTS windows (32/128/512/2048, the smelter shape)
		int[] tWindows = {32, 128, 512, 2048};
		for (int i = 0; i < 4; i++) {
			assertEquals(tWindows[i], gregtech6.registry.GTMachines.TIER_INPUTS[i][1], "the roasting NBT_INPUT column of rung " + i);
		}
	}

	private static void assertSameType(Object aExpected, Object aActual, String aMessage) {
		org.junit.jupiter.api.Assertions.assertSame(aExpected, aActual, aMessage);
	}

	/** The narrow accessor over {@code GTBasicMachineBlock.MachineRow} (the record accessors, no package dance). */
	private record GTBasicMachineBlockProxy(gregtech6.block.GTBasicMachineBlock.MachineRow tRow) {
		String path() { return tRow.path(); }
		int metaId() { return tRow.metaId(); }
		float hardness() { return tRow.hardness(); }
		int parallel() { return tRow.parallel(); }
		boolean parallelDuration() { return tRow.parallelDuration(); }
		String texture() { return tRow.texture(); }
		String matSlug() { return tRow.matSlug(); }
		Object energyType() { return tRow.energyType(); }
		int tier() { return tRow.tier(); }
		Object recipes() { return tRow.recipes(); }
	}
}
