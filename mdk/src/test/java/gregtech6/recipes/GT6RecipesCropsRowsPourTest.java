package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;

/**
 * The crop consumption chain tests (task cbc-5-crop-consumption) — the recipe-data family
 * method over {@link GT6RecipesCrops}: the DATA census (table sizes + the walk membership
 * ratchets), the TRUE NEGATIVE / declared-pool pins (the SKIPPED_UPSTREAM ledger + the
 * zero-face assertions), the fluid-id universe check, and the POUR FACE with injected
 * fixtures (the Bath test form — the same load() pours the whole table, proving live code).
 */
class GT6RecipesCropsRowsPourTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootTheMaterialUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (the family-walk prerequisite)
	}

	@BeforeEach
	void armTheMaps() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipesCrops.resetForTest();
	}

	@AfterEach
	void restoreTheLiveSeams() {
		GT6RecipesCrops.sMaterialItemResolver = GT6RecipesMixer::resolveItem;
		GT6RecipesCrops.sIdItemResolver = GT6RecipesCrops::resolveItemById;
		GT6RecipesCrops.sFluidResolver = GT6RecipesCrops::resolveFluidById;
		GT6RecipeMaps.reset();
	}

	// ------------------------------------------------------------------ the data census

	/** The RM.biomass walk: 66 legs, and the walk memberships verbatim. */
	@Test
	void biomassWalkCensus() {
		List<GT6RecipesCrops.BiomassLeg> tLegs = GT6RecipesCrops.biomassLegs();
		assertEquals(GT6RecipesCrops.BIOMASS_LEG_COUNT, tLegs.size(), "RM.java:688-703 — 2+4+1+4+47+3+3+2 legs");
		assertEquals(47, GT6RecipesCrops.JUICE_WALK.size(), "FluidsGT.JUICE = the FL.java:187-234 rows, 47 members");
		assertEquals(4, GT6RecipesCrops.MILK_WALK.size(), "FluidsGT.MILK = FL.java:134-137");
		assertEquals(3, GT6RecipesCrops.HONEY_WALK.size(), "FluidsGT.HONEY = FL.java:139-141 (for/grc/generic)");
		assertEquals(4, GT6RecipesCrops.WATER_WALK.size(), "FL.waters = Water/MnWtr/DistW/SpDew (FL.java:689)");
		// the upstream-verbatim singleton repeat: :692 re-adds MnWtr after the WATER walk
		long tMnWtr = tLegs.stream().filter(aLeg -> aLeg.inFluidId().equals("gt6:mnwtr")).count();
		assertEquals(2, tMnWtr, "RM.java:691 walk member + :692 singleton = the verbatim double");
		// the special-juice trio rides its own legs at the :697-699 multipliers
		GT6RecipesCrops.BiomassLeg tIduns = tLegs.stream().filter(aLeg -> aLeg.note().equals("RM.java:697")).findFirst().get();
		assertEquals(210600, tIduns.outUnits(), "RM.java:697 — the Idun's apple multiplier");
		GT6RecipesCrops.BiomassLeg tRoyal = tLegs.stream().filter(aLeg -> aLeg.note().equals("RM.java:703")).findFirst().get();
		assertEquals(12560, tRoyal.outUnits(), "RM.java:703 — the royal-jelly multiplier");
	}

	/** The table census ratchet: the enumerated row families and their exact sizes. */
	@Test
	void tableCensusRatchet() {
		List<GT6RecipesCrops.Row> tRows = GT6RecipesCrops.table();
		int tFlour = ANY.FlourGrains.mToThis.size();
		assertTrue(tFlour >= 8, "the FlourGrains family is live (7 grains + potato = 8): " + tFlour);
		// flour walk: 2 feeders x 66 legs per material; wool seam 1; fodder: 4x66 + 1; grains:
		// 4x73; wheat listener: 1+4+2+66+1 = 74; baleWheat: 1+66+1 = 68; the food band: 2+2+2+2+4
		int tExpected = tFlour * 132 + 1 + (4 * 66 + 1) + (4 * 73) + (1 + 4 + 2 + 66 + 1) + (1 + 66 + 1) + (2 + 2 + 2 + 2 + 4);
		assertEquals(tExpected, tRows.size(), "the enumerated table ratchet (" + tRows.size() + ")");
		// map destinations census
		long tFermenter = tRows.stream().filter(aRow -> aRow.map().equals("fermenter")).count();
		assertEquals((tFlour * 2 + 10) * 66, tFermenter, "the biomass legs: flour feeders + fodder + grains + wheat + hay");
		assertEquals(1, tRows.stream().filter(aRow -> aRow.note().contains(":719")).count(), "the wool seam live row (white shred)");
	}

	/** Verbatim spot checks: the exact upstream integer-division amounts and durations. */
	@Test
	void verbatimArithmetic() {
		String tWheat = MT.Wheat.mNameInternal;
		// :42 RM.biomass(dust x9) — tSize 9, speed 64: the water leg = in 1080/9, out 1080/9, dur 256/9
		GT6RecipesCrops.Row tWheatWater = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":41 dust x9 " + tWheat) && aRow.note().contains("RM.java:690")
						&& aRow.fluidIn().id().equals("minecraft:water")).findFirst().get();
		assertEquals(120, tWheatWater.fluidIn().amount(), "1080/9");
		assertEquals(120, tWheatWater.fluidOut().amount(), "1080/9");
		assertEquals(28, tWheatWater.duration(), "(64*4)/9 — the upstream long division");
		assertEquals(16, tWheatWater.eUt(), "RM.biomass eut");
		assertFalse(tWheatWater.buffered(), "RM.biomass pours addRecipe1(F, ...) verbatim");
		// :703 royal jelly at tSize 9: 12560/9 truncates
		GT6RecipesCrops.Row tWheatRoyal = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":41 dust x9 " + tWheat) && aRow.note().contains("RM.java:703")).findFirst().get();
		assertEquals(1395, tWheatRoyal.fluidOut().amount(), "12560/9 = 1395 (truncation)");
		// :249-kin baleWheat biomass rides the hay block at tSize 1: full amounts, dur (64*4)/1
		GT6RecipesCrops.Row tHayWater = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":250 baleWheat") && aRow.fluidIn().id().equals("minecraft:water")).findFirst().get();
		assertEquals(1080, tHayWater.fluidIn().amount(), "1080/1");
		assertEquals(256, tHayWater.duration(), "(64*4)/1");
		// :500 melon juicer discount: 250 - (1+1)*25 = 200
		GT6RecipesCrops.Row tMelonJuicer = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().equals(":500 cropMelon Juicer")).findFirst().get();
		assertEquals(200, tMelonJuicer.fluidOut().amount(), "RM.java:773 — aAmount-(1+(aAmount/250))*25");
		assertEquals(6000, tMelonJuicer.chances()[0], "the Juicer keeps the full aChance");
		// :495 apple squeezer: chance-1000
		GT6RecipesCrops.Row tAppleSqueezer = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().equals(":495 cropApple Squeezer")).findFirst().get();
		assertEquals(6000, tAppleSqueezer.chances()[0], "RM.java:772 — aChance-1000");
		// :719 the wool shred: eut 16 / dur 16 / chance 9000 / white wool -> 4 string
		GT6RecipesCrops.Row tWool = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":719")).findFirst().get();
		assertEquals("minecraft:white_wool", tWool.inputs()[0].id());
		assertEquals("minecraft:string", tWool.outputs()[0].id());
		assertEquals(4, tWool.outputs()[0].count());
		assertEquals(9000, tWool.chances()[0], "Vanilla:719 — the 9000 chance");
		// :251 the hay-block unpack: 9 wheat back out
		GT6RecipesCrops.Row tUnpack = GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().equals(":251 baleWheat unpack")).findFirst().get();
		assertEquals("minecraft:hay_block", tUnpack.inputs()[0].id());
		assertEquals(9, tUnpack.outputs()[0].count());
	}

	// ------------------------------------------------------------------ the declared faces

	/** The plantGtFiber seam faces: ZERO registered items (no PLANTS material) — the dyed
	 * shred/generify legs are the declared pool; the white-wool row is the one live row. */
	@Test
	void plantGtFiberFacesAreTheDeclaredPool() {
		assertEquals(0, GT6RecipesCrops.table().stream()
				.filter(aRow -> aRow.note().contains(":719") && !aRow.inputs()[0].id().equals("minecraft:white_wool")).count(),
				"only the white-wool row is enumerated from :719 (the dyed legs ride the Dye_Materials pool)");
		assertTrue(GT6RecipesCrops.SKIPPED_UPSTREAM.stream().anyMatch(aEntry -> aEntry.contains(":975-990")),
				"the generify fiber→string legs are declared");
		assertTrue(GT6RecipesCrops.SKIPPED_UPSTREAM.stream().anyMatch(aEntry -> aEntry.contains("not PLANTS-flag materials")),
				"the dye-color fiber face is pinned");
		// the blockDust arm of :41-44 is enumerated but the prefix is not an item path
		assertFalse(GTMaterialItems.registrationOrder().stream().anyMatch(aPair -> aPair.prefix() == OP.blockDust),
				"blockDust is not an item-path prefix — the :43 arm pours zero");
		// the FlourGrains family is the :41-44 walk, verbatim wired
		assertTrue(ANY.FlourGrains.mToThis.contains(MT.Wheat), "Wheat is a FlourGrains member (the grain() put)");
		assertTrue(ANY.FlourGrains.mToThis.contains(MT.Potato), "Potato rides the addReRegistrationToThis wiring");
	}

	/** The SKIPPED_UPSTREAM ledger: the TRUE NEGATIVE and declared-pool pins (the t1b 家法). */
	@Test
	void skippedUpstreamLedgerPins() {
		List<String> tLedger = GT6RecipesCrops.SKIPPED_UPSTREAM;
		assertTrue(tLedger.size() >= 18, "the ledger ratchet: " + tLedger.size() + " entries");
		String tAll = String.join("\n", tLedger);
		assertTrue(tAll.contains(":48-71"), "the four flower listeners");
		assertTrue(tAll.contains(":80-124"), "the baleGrass family");
		assertTrue(tAll.contains(":256-305"), "the seed fan (juicer.json fold)");
		assertTrue(tAll.contains(":309-870") || tAll.contains(":350-870"), "the foreign crop-food band");
		assertTrue(tAll.contains(":876-925"), "the four generic listeners");
		assertTrue(tAll.contains("GT6RecipesCompressor.java:217"), "the P10 ruling pointer");
		assertTrue(tAll.contains(":493"), "the molten-gold Bath leg");
		assertTrue(tAll.contains(":742"), "the HaC grape leg");
		assertTrue(tAll.contains("Dye_Materials"), "the dye-array pool face");
		assertTrue(tAll.contains("ae_grinder"), "the AE/TE/IC2 helper legs");
		// every enumerated absent face is ledged, not silent
		assertTrue(tAll.contains(":249"), "the baleWheat Drying leg");
	}

	/** The fluid-id universe: every gt6 fluid id the tables reference exists in the GTFluids
	 * spec tables (the b2b2 method — offline-safe registry-independent check). */
	@Test
	void fluidIdUniverse() {
		Set<String> tUniverse = new HashSet<>();
		for (GTFluids.EngineFluidSpec tSpec : GTFluids.ENGINE_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B1_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B2_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_TAIL_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CHEMICAL_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HOT_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CLOSURE_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.LUBRICANT_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HONEY_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.BEE_ROW_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.QU_FLUID_SPECS) tUniverse.add(tSpec.name());
		for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.NAMING_FLUID_SPECS) tUniverse.add(tSpec.name());
		Set<String> tMissing = new HashSet<>();
		for (GT6RecipesCrops.Row tRow : GT6RecipesCrops.table()) {
			if (tRow.fluidIn() != null && tRow.fluidIn().id().startsWith("gt6:") && !tUniverse.contains(tRow.fluidIn().id().substring(4))) tMissing.add(tRow.fluidIn().id());
			if (tRow.fluidOut() != null && tRow.fluidOut().id().startsWith("gt6:") && !tUniverse.contains(tRow.fluidOut().id().substring(4))) tMissing.add(tRow.fluidOut().id());
		}
		assertEquals(Set.of(), tMissing, "every gt6 fluid id resolves in the GTFluids spec universe");
	}

	// ------------------------------------------------------------------ the pour face

	/**
	 * The ledger closes on every leg, and the pour split is pinned bimodally: the forge
	 * offline JVM binds no gt6 registry (the fermenter stays empty), while the neoforge FML
	 * test JVM binds the REAL mod registries (research.fml-test-memory) — there the live
	 * pour fires: the flour dust feeders + the wheat/hay listeners + the vanilla food band
	 * (683 rows), the unmerged food-crop/fodder ids and the blockDust arm accounting for
	 * every skip (1085).
	 */
	@Test
	void registryStateAwareReconciliation() {
		GT6RecipesCrops.load();
		int tAttempted = GT6RecipesCrops.lastAttempted();
		assertEquals(GT6RecipesCrops.table().size(), tAttempted, "every table row attempted");
		assertEquals(tAttempted, GT6RecipesCrops.lastPoured() + GT6RecipesCrops.lastSkipped(), "the ledger closes");
		int tFlour = ANY.FlourGrains.mToThis.size();
		if (GT6RecipeMaps.FERMENTER.mRecipeList.isEmpty()) {
			// the unbound leg: no gt6 fluid resolves, so the fermenter walk cannot pour
			assertEquals(0, GT6RecipeMaps.FERMENTER.mRecipeList.size());
		} else {
			// the registry-bound leg: the live pour — the upstream-resolution pour face
			assertEquals((tFlour + 2) * 66, GT6RecipeMaps.FERMENTER.mRecipeList.size(),
					"the flour dust feeders + the cropWheat and baleWheat biomass legs");
			assertEquals(683, GT6RecipesCrops.lastPoured(), "the bound pour ratchet");
			assertEquals(tAttempted - 683, GT6RecipesCrops.lastSkipped(), "the bound skip ledger");
			assertEquals(4, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "wool + wheat mortarize + hay shred + potato remains");
			assertEquals(2, GT6RecipeMaps.MORTAR.mRecipeList.size(), "the wheat mortarize + the potato remains (the grain inputs are the food-crop pool)");
			assertEquals(1, GT6RecipeMaps.COMPRESSOR.mRecipeList.size(), "the wheat compact → hay block");
			assertEquals(1, GT6RecipeMaps.UNBOXINATOR.mRecipeList.size(), "the hay unpack → 9 wheat");
			assertEquals(4, GT6RecipeMaps.MIXER.mRecipeList.size(), "the cropWheat mash legs");
			assertEquals(1, GT6RecipeMaps.DRYING.mRecipeList.size(), "the cropWheat drying leg (the fodder id is the pool)");
			assertEquals(5, GT6RecipeMaps.SQUEEZER.mRecipeList.size());
			assertEquals(5, GT6RecipeMaps.JUICER.mRecipeList.size());
		}
	}

	/**
	 * With injected fixtures the SAME load() pours the WHOLE table — the pour face is live
	 * code (the Bath fixture form). Destinations, eut/duration faces and the buffered flags
	 * are verbatim-checked on the poured recipes.
	 */
	@Test
	void thePourFaceIsLiveUnderInjectedFixtures() {
		GT6RecipesCrops.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.BRICK; // every material face resolves
		GT6RecipesCrops.sIdItemResolver = aId -> aId.startsWith("minecraft:") ? vanillaItem(aId) : Items.BREAD; // every id face resolves
		GT6RecipesCrops.sFluidResolver = aId -> aId.equals("minecraft:water") ? Fluids.WATER : Fluids.LAVA; // every fluid resolves

		GT6RecipesCrops.load();

		assertEquals(GT6RecipesCrops.table().size(), GT6RecipesCrops.lastPoured(), "every row pours under full fixtures");
		assertEquals(0, GT6RecipesCrops.lastSkipped(), "no skips under full fixtures");
		// the map destinations: the fermenter carries exactly the biomass legs
		int tFlour = ANY.FlourGrains.mToThis.size();
		int tBiomassRows = (tFlour * 2 + 10) * 66;
		assertEquals(tBiomassRows, GT6RecipeMaps.FERMENTER.mRecipeList.size(), "the fermenter = the biomass walk");
		assertEquals(8, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "wool + wheat mortarize + hay shred + 4 grain mortarizes + potato remains");
		assertEquals(6, GT6RecipeMaps.MORTAR.mRecipeList.size(), "the wheat mortarize + 4 grain mortarizes + potato remains");
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mRecipeList.size(), "the wheat compact");
		assertEquals(1, GT6RecipeMaps.UNBOXINATOR.mRecipeList.size(), "the hay unpack");
		assertEquals(6, GT6RecipeMaps.DRYING.mRecipeList.size(), "fodder + 4 grains + wheat");
		assertEquals(20, GT6RecipeMaps.MIXER.mRecipeList.size(), "(4 grains + wheat) x 4 waters");
		assertEquals(5, GT6RecipeMaps.SQUEEZER.mRecipeList.size());
		assertEquals(5, GT6RecipeMaps.JUICER.mRecipeList.size());
		// a poured fermenter row's verbatim face (the wheat water leg, tSize 9)
		Recipe tWaterRow = GT6RecipeMaps.FERMENTER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mDuration == 28 && aRecipe.mFluidInputs.length == 1
						&& aRecipe.mFluidInputs[0].getAmount() == 120 && aRecipe.mFluidOutputs[0].getAmount() == 120)
				.findFirst().orElse(null);
		assertNotNull(tWaterRow, "the (64*4)/9 water leg poured verbatim");
		assertFalse(tWaterRow.mCanBeBuffered, "the RM.biomass rows carry the F first argument");
		// the wool row: white wool -> 4 string at chance 9000
		Recipe tWoolRow = GT6RecipeMaps.SHREDDER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == vanillaItem("minecraft:white_wool")).findFirst().orElse(null);
		assertNotNull(tWoolRow, "the wool shred poured");
		assertEquals(4, tWoolRow.mOutputs[0].getCount(), "4 string");
		assertNotNull(tWoolRow.mChances, "the 9000 chance array survives the pour");
		assertEquals(9000, tWoolRow.mChances[0]);
	}

	/** The vanilla faces of the enumerated table — the LoaderItemList aliases pinned. */
	@Test
	void vanillaFacesAreTheLoaderItemListAliases() {
		Set<String> tVanilla = new HashSet<>();
		for (GT6RecipesCrops.Row tRow : GT6RecipesCrops.table()) {
			for (GT6RecipesCrops.Slot tSlot : tRow.inputs()) if (tSlot.id() != null && tSlot.id().startsWith("minecraft:")) tVanilla.add(tSlot.id());
			for (GT6RecipesCrops.Slot tSlot : tRow.outputs()) if (tSlot.id() != null && tSlot.id().startsWith("minecraft:")) tVanilla.add(tSlot.id());
		}
		assertEquals(Set.of("minecraft:white_wool", "minecraft:string", "minecraft:wheat", "minecraft:hay_block",
				"minecraft:apple", "minecraft:melon_slice", "minecraft:beetroot", "minecraft:carrot", "minecraft:potato"),
				tVanilla, "the exact vanilla face set (Bale_Wheat = hay_block / Crop_Wheat = wheat per LoaderItemList:760/:761)");
	}

	// ------------------------------------------------------------------ fixture helpers

	private static Item vanillaItem(String aId) {
		return switch (aId) {
			case "minecraft:white_wool" -> Items.WHITE_WOOL;
			case "minecraft:string" -> Items.STRING;
			case "minecraft:wheat" -> Items.WHEAT;
			case "minecraft:hay_block" -> Items.HAY_BLOCK;
			case "minecraft:apple" -> Items.APPLE;
			case "minecraft:melon_slice" -> Items.MELON_SLICE;
			case "minecraft:beetroot" -> Items.BEETROOT;
			case "minecraft:carrot" -> Items.CARROT;
			case "minecraft:potato" -> Items.POTATO;
			default -> Items.BREAD;
		};
	}
}
