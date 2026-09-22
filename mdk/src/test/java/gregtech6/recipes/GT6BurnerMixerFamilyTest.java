package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
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
 * The p34-machines-burner-plantalyzer Burner Mixer acceptance test: the row-table pins
 * (the 4-ladder, Loader_MultiTileEntities.java:1595-1598 — the RU carrier, the 4/8/16/32
 * parallel band with NBT_PARALLEL_DURATION T, the "burnmixer" texture, the Kinetic_T
 * hardness ladder, the ignition-family BET), the RM.java:76 map constants (the MIXER :74
 * shape), the 33-row JSON pour (Loader_Recipes_Chem.java:220-259 — the H/T burns, the
 * five-member coal loop x3, the sixteen TiO2/Ilmenite chlorination rows; the fluid
 * conversion 1 material U = 1000 mB per the GT6RecipesFusion calibration, cross-checked
 * on the :220 stoichiometry 1000 H + 500 O2 -> 1500 water), and the ignition gate face
 * (the TileEntityBurnerMixer subclass: ignite() = the TOOL_igniter branch, the cold
 * machine probes only, the running one keeps the mIgnited=40 keep-alive).
 */
public class GT6BurnerMixerFamilyTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	private final Set<String> tRequestedItems = new HashSet<>();
	private final Set<String> tRequestedFluids = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		tRequestedItems.clear();
		tRequestedFluids.clear();
		// the gt6: dust/fluid ids are not registered offline — stand-ins, identity only
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			tRequestedItems.add(aId.getNamespace() + ":" + aId.getPath());
			return Items.COAL;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> {
			tRequestedFluids.add(aId.getNamespace() + ":" + aId.getPath());
			return Fluids.WATER;
		};
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems;
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	// ---------------------------------------------------------------------------
	// the row-table pins (Loader :1595-1598)
	// ---------------------------------------------------------------------------

	@Test
	void theFourBurnerMixerRowsCarryTheUpstreamColumns() {
		assertEquals(4, gregtech6.registry.GTMachines.BURNER_MIXER_ROWS.size(), "the 4-ladder");
		String[] tPaths = {"burner_mixer", "burner_mixer_t2", "burner_mixer_t3", "burner_mixer_t4"};
		int[] tIds = {20521, 20522, 20523, 20524};
		float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F}; // the Kinetic_T hardness ladder
		int[] tParallels = {4, 8, 16, 32}; // the NBT_PARALLEL band (:1595-1598)
		String[] tMats = {"bronze", "steel", "titanium", "tungstensteel"};
		int[] tWindows = {32, 128, 512, 2048}; // the NBT_INPUT column through TIER_INPUTS
		for (int i = 0; i < 4; i++) {
			GTBasicMachineBlockProxy tRow = new GTBasicMachineBlockProxy(gregtech6.registry.GTMachines.BURNER_MIXER_ROWS.get(i));
			assertEquals(tPaths[i], tRow.path(), "the registry path of rung " + i);
			assertEquals(tIds[i], tRow.metaId(), "the meta id of rung " + i);
			assertEquals(tHardness[i], tRow.hardness(), "the hardness of rung " + i);
			assertEquals(tParallels[i], tRow.parallel(), "the NBT_PARALLEL band of rung " + i);
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T (:1595-1598)");
			assertEquals("burnmixer", tRow.texture(), "the NBT_TEXTURE column");
			assertEquals(tMats[i], tRow.matSlug(), "the Kinetic_T material word of rung " + i);
			assertTrue(tRow.energyType() == TD.Energy.RU, "the RU carrier — CARD ERRATUM: the task card said HU, the :1595-1598 rows read TD.Energy.RU");
			assertEquals(i, tRow.tier(), "the tier IS the rung index (the TIER_INPUTS selector)");
			assertEquals(tWindows[i], gregtech6.registry.GTMachines.TIER_INPUTS[i][1], "the NBT_INPUT column of rung " + i);
			assertNotNull(tRow.recipes(), "the recipe-map supplier armed");
		}
		// the ignition face: the family BET instantiates the ignition subclass (the card
		// erratum's second half — the coordinator ruling 2026-09-22)
		assertTrue(gregtech6.registry.GTMachines.BURNER_MIXER_PARALLEL.length == 4, "the parallel ladder carrier");
	}

	@Test
	void theBurnerMixerMapCarriesTheRm76Constants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.BURN_MIXER, "the BURN_MIXER map registered");
		assertEquals("gt.recipe.burnmixer", GT6RecipeMaps.BURN_MIXER.mNameInternal, "the RM.java:76 internal name");
		assertEquals("Burner Mixer", GT6RecipeMaps.BURN_MIXER.mNameLocal, "the local name");
		assertEquals(6, GT6RecipeMaps.BURN_MIXER.mInputItemsCount, "items 6/1/0 — the input slots");
		assertEquals(1, GT6RecipeMaps.BURN_MIXER.mOutputItemsCount, "items 6/1/0 — the output slots");
		assertEquals(0, GT6RecipeMaps.BURN_MIXER.mMinimalInputItems, "items 6/1/0 — the item minimum");
		assertEquals(6, GT6RecipeMaps.BURN_MIXER.mInputFluidCount, "fluids 6/2/0 — the input slots");
		assertEquals(2, GT6RecipeMaps.BURN_MIXER.mOutputFluidCount, "fluids 6/2/0 — the output slots");
		assertEquals(0, GT6RecipeMaps.BURN_MIXER.mMinimalInputFluids, "fluids 6/2/0 — the fluid minimum");
		assertEquals(2, GT6RecipeMaps.BURN_MIXER.mMinimalInputs, "MIN 2 — the total-input gate");
	}

	// ---------------------------------------------------------------------------
	// the 33-row pour (the shipped burnmixer.json verbatim)
	// ---------------------------------------------------------------------------

	@Test
	void theThirtyThreeRowsPourWithTheUpstreamParameters() throws Exception {
		String tPath = "/data/gt6/recipe_maps/burnmixer.json";
		try (InputStream tStream = GT6BurnerMixerFamilyTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped burnmixer.json rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "burnmixer"), JsonParser.parseString(tJson)));
		}
		assertEquals(33, GT6RecipeMapJsonLoader.pouredCount("burnmixer"), "2 H/T burns + 5-member coal loop x3 + 16 Ti chlorination rows");
		assertEquals(33, GT6RecipeMaps.BURN_MIXER.mRecipeList.size(), "the 33 rows poured under the burnmixer key");

		// the fluid conversions (1000 mB/U) and the item ids the rows consume
		assertTrue(tRequestedFluids.contains("gt6:hydrogen"), "the :220 H gas");
		assertTrue(tRequestedFluids.contains("gt6:oxygen"), "the FL.make oxygen literal");
		assertTrue(tRequestedFluids.contains("gt6:distilled_water"), "the :220 DistW output");
		assertTrue(tRequestedFluids.contains("gt6:tritium"), "the :221 T gas");
		assertTrue(tRequestedFluids.contains("gt6:tritiatedwater"), "the T2O.liquid(U2*3) = 1500 — the p29-w4 closure carrier");
		assertTrue(tRequestedFluids.contains("gt6:carbondioxide"), "the :237-238 CO2 6000");
		assertTrue(tRequestedFluids.contains("gt6:carbonmonoxide"), "the :239 CO 6000 + the Ti rows");
		assertTrue(tRequestedFluids.contains("gt6:sodiumcarbonate_molten"), "the :239 Na2CO3.liquid(U*6) = 6000 — the createMolten MT.Na2CO3 carrier");
		assertTrue(tRequestedFluids.contains("gt6:chlorine"), "the :244-259 Cl gas");
		assertTrue(tRequestedFluids.contains("gt6:calcite_molten"), "the CaCO3.liquid(U) = 1000 — the createMolten MT.CaCO3 carrier");
		assertTrue(tRequestedFluids.contains("gt6:titaniumtetrachloride"), "the TiCl4.liquid(U*5) = 5000 — the createLiquid MT.TiCl4 carrier");
		// the five coal-family members x the three loops (ANY.Coal.mToThis minus Graphene)
		Set<String> tCoals = Set.of("gt6:dust_carbon", "gt6:dust_graphite", "gt6:dust_coal_coke", "gt6:dust_coal", "gt6:dust_charcoal");
		for (String tCoal : tCoals) {
			assertTrue(tRequestedItems.contains(tCoal), "the :237-239 coal-family member " + tCoal);
		}
		assertTrue(tRequestedItems.contains("gt6:dust_pet_coke"), "the :256-259 PetCoke reductant");
		assertTrue(tRequestedItems.contains("gt6:dust_na2_so4"), "the sulfate input");
		assertTrue(tRequestedItems.contains("gt6:dust_k2_so4"), "the :238 sulfate input");
		assertTrue(tRequestedItems.contains("gt6:dust_ilmenite"), "the :246-247/:250-251/:254-255/:258-259 ilmenite input");

		// the representative-row pins: group the rows by their fluid-amount signature
		Map<Integer, int[]> tBurn = new HashMap<>(); // duration -> {fluidIn0, fluidIn1, fluidOut}
		Set<String> tTiCl4 = new HashSet<>(); // the "TiCl4->COx" pair signatures
		int tTiRows = 0, tSulfateRows = 0, tSodaRows = 0;
		for (Recipe tRow : GT6RecipeMaps.BURN_MIXER.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "every :220-259 row runs 16 EUt");
			if (tRow.mDuration == 24) {
				// the :220/:221 burns — two fluid inputs, one fluid output, no items
				assertEquals(0, tRow.mInputs.length, "the burn rows are fluid-only inputs");
				assertEquals(2, tRow.mFluidInputs.length, "gas + oxygen");
				assertEquals(1, tRow.mFluidOutputs.length, "the water-family output");
				assertEquals(0, tRow.mOutputs.length, "no item output");
				tBurn.put(24, new int[] {tRow.mFluidInputs[0].getAmount(), tRow.mFluidInputs[1].getAmount(), tRow.mFluidOutputs[0].getAmount()});
			} else if (tRow.mDuration == 144) {
				// the :237-238 sulfate roasts — dust 7U + coal 2U -> CO2 6000 + sulfide 3U
				tSulfateRows++;
				assertEquals(2, tRow.mInputs.length, "sulfate + coal dust");
				assertEquals(0, tRow.mFluidInputs.length, "no fluid input");
				assertEquals(1, tRow.mFluidOutputs.length, "the CO2 output");
				assertEquals(6000, tRow.mFluidOutputs[0].getAmount(), "CO2.gas(U*6) = 6000");
				assertEquals(1, tRow.mOutputs.length, "the sulfide output");
			} else if (tRow.mDuration == 128) {
				// the :239 soda roasts — coal 2U + Na2CO3 6000 -> CO 6000 + Na 2U
				tSodaRows++;
				assertEquals(1, tRow.mInputs.length, "the coal dust");
				assertEquals(1, tRow.mFluidInputs.length, "the Na2CO3 liquid");
				assertEquals(6000, tRow.mFluidInputs[0].getAmount(), "Na2CO3.liquid(U*6) = 6000");
				assertEquals(6000, tRow.mFluidOutputs[0].getAmount(), "CO.gas(U*6) = 6000");
			} else {
				// the :244-259 chlorination rows — 256 ticks, two fluid inputs, two outputs
				tTiRows++;
				assertEquals(256L, tRow.mDuration, "the chlorination tick");
				assertEquals(2, tRow.mFluidInputs.length, "Cl + CaCO3");
				assertEquals(2, tRow.mFluidOutputs.length, "TiCl4 + CO/CO2");
				tTiCl4.add(tRow.mFluidOutputs[0].getAmount() + "->" + tRow.mFluidOutputs[1].getAmount());
			}
		}
		assertTrue(tBurn.containsKey(24), "both :220/:221 burns carry the 24-tick signature"); // the int[] face
		assertEquals(1500, tBurn.get(24)[0] + tBurn.get(24)[1], "the :220 stoichiometry: H 1000 + O2 500 -> 1500");
		assertEquals(1500, tBurn.get(24)[2], "the water output 1500 (DistW/T2O)");
		assertEquals(10, tSulfateRows, "5 coal members x the :237+:238 pair");
		assertEquals(5, tSodaRows, "5 coal members x :239");
		assertEquals(16, tTiRows, "4 reductants x 4 scale rungs");
		// the scale ladder: TiO2 1U -> TiCl4 5000 (CO2 3000); TiO2 2U -> 10000 (CO2 6000);
		// Ilmenite 5U -> TiCl4 5000 (CO 6000); Ilmenite 10U -> TiCl4 10000 (CO 12000)
		assertEquals(Set.of("5000->3000", "10000->6000", "5000->6000", "10000->12000"), tTiCl4, "the four (TiCl4 -> COx) signatures");
	}

	// ---------------------------------------------------------------------------
	// the ignition gate face (the TileEntityBurnerMixer subclass)
	// ---------------------------------------------------------------------------

	@Test
	void theIgnitionGateFoldsTheUpstreamHalf() throws Exception {
		// the BE is constructible offline (the oven test-fixture posture, Builder.of without
		// a registry): a fresh Burner Mixer carries the registration constant, ignite() arms
		// the 40-tick window, the mRequiresIgnition fold blocks a cold start.
		GT6RecipeMaps.init();
		var tType = net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
				(aPos, aState) -> null, net.minecraft.world.level.block.Blocks.STONE).build(null);
		var tPos = new net.minecraft.core.BlockPos(0, 1, 0);
		var tState = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
		gregtech6.tileentity.machines.TileEntityBurnerMixer tBurner =
				new gregtech6.tileentity.machines.TileEntityBurnerMixer(tType, tPos, tState, GT6RecipeMaps.BURN_MIXER, 4, true, null);
		assertTrue(tBurner.requiresIgnition(), "NBT_NEEDS_IGNITION T (:1595)");
		assertEquals(0, tBurner.mIgnited, "a cold machine");
		tBurner.ignite(); // the TOOL_igniter branch :373-379
		assertEquals(40, tBurner.mIgnited, "mIgnited = 40 (the :375 arm)");
		// the gate: cold + unignited -> the probe face (no consume); ignited -> consume
		// (the base is the SAME checkRecipe with aApplyRecipe folded at the entry — the
		// offline pin drives the LIVE fold through the base slot inventory, empty here so
		// both calls answer DID_NOT_FIND_RECIPE; the fold placement argument lives in the
		// class doc, the consume-face proof rides the RCON chain)
		int tCold = tBurner.checkRecipe(true, false);
		assertEquals(gregtech6.tileentity.machines.TileEntityBasicMachine.DID_NOT_FIND_RECIPE, tCold, "the empty-slot probe stays DID_NOT_FIND_RECIPE");
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
