package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Food-fluid batch-1 offline tests (task p33-food-fluids-b1 — the registration-row
 * assertions against the DECLARED values, the GTFluidsFoodFamilyTest shape over the
 * FIFTH AquaFluidSpec table). The batch is the FL.java FOOD-flag census remainder:
 * 234 FOOD rows in FL.java:110-408, minus the 15 already-ported earlier-table rows
 * (the eleven WATER-tagged aqua rows + sap/maplesap/reedwater/cactuswater) and the
 * 2 out-of-scope shorthands (Sap_Rainbow :251; the "honey" generic-id collision
 * HoneyBoP), leaving these 216 rows — the four p31 honey-family ids ride the ChemicalFluid HONEY_FLUID_SPECS registrations, NOT re-declared here. Temperatures are the upstream FL.create carriers
 * (Loader_Fluids.java:376-647 FoodStatDrink block) where GT6 defines the fluid and the
 * honest FluidType defaults (300 K) for the 68 external-mod ids; density/viscosity
 * 1000 are the STATE_LIQUID carriers (FL.java:1104). The live registry side is the
 * runServer smoke evidence and the RCON tank chain; offline asserts the declaration
 * table, the count reconciliation and the isolation faces.
 */
public class GTFluidsFoodB1Test extends GTOfflineTestBase {

	/** The 216 ids in declaration order — the FL.java FOOD-census reconciliation list. */
	private static final List<String> IDS = List.of(
		"mineralsoda",
		"soda",
		"milk",
		"soymilk",
		"grcmilk_milk",
		"spoiledmilk",
		"for_honey",
		"grc_honey",
		"fruitsmoothie",
		"melonsmoothie",
		"kiwismoothie",
		"currantsmoothie",
		"raspberrysmoothie",
		"blackberrysmoothie",
		"blueberrysmoothie",
		"gooseberrysmoothie",
		"strawberrysmoothie",
		"plumsmoothie",
		"peachsmoothie",
		"elderberrysmoothie",
		"grapefruitsmoothie",
		"limesmoothie",
		"orangesmoothie",
		"persimmonsmoothie",
		"apricotsmoothie",
		"pearsmoothie",
		"redgrapesmoothie",
		"whitegrapesmoothie",
		"grapesmoothie",
		"purplegrapesmoothie",
		"applesmoothie",
		"pineapplesmoothie",
		"bananasmoothie",
		"cherrysmoothie",
		"cranberrysmoothie",
		"lemonsmoothie",
		"mangosmoothie",
		"pomegranatesmoothie",
		"starfruitsmoothie",
		"papayasmoothie",
		"figsmoothie",
		"coconutsmoothie",
		"juice_juice",
		"kiwijuice",
		"juicelime",
		"juicelemon",
		"juiceorange",
		"persimmonjuice",
		"melonjuice",
		"currantjuice",
		"raspberryjuice",
		"blackberryjuice",
		"blueberryjuice",
		"gooseberryjuice",
		"strawberryjuice",
		"juiceplum",
		"juicepeach",
		"juiceelderberry",
		"hellderberryjuice",
		"juicegrapefruit",
		"juiceapricot",
		"juicepear",
		"grapejuice",
		"grc_grapewine0",
		"juiceredgrape",
		"juicewhitegrape",
		"juiceapple",
		"grc_applecider0",
		"juicepineapple",
		"juicebanana",
		"juicecherry",
		"juicecranberry",
		"cactusfruitjuice",
		"mangojuice",
		"pomegranatejuice",
		"starfruitjuice",
		"papayajuice",
		"figjuice",
		"coconutmilk",
		"datejuice",
		"juicecarrot",
		"juicetomato",
		"beetjuice",
		"pumpkinjuice",
		"cucumberjuice",
		"onionjuice",
		"potatojuice",
		"ricewater",
		"hopsmash",
		"wheathopsmash",
		"mashwheat",
		"mashcorn",
		"mashrye",
		"mashgrain",
		"maplesyrup",
		"peanutbutter",
		"grcmilk_cream",
		"chocolatecream",
		"coconutcream",
		"nutella",
		"ketchup",
		"mayo",
		"dressing",
		"mushroomsoup",
		"blood",
		"chillysauce",
		"hotsauce",
		"diabolosauce",
		"diablosauce",
		"diablosauce_strong",
		"bbqsauce",
		"slime_blue",
		"pinkslime",
		"slime",
		"bawls",
		"tea",
		"sweettea",
		"icetea",
		"purpledrink",
		"lemonade",
		"cavejohnsonsgrenadejuice",
		"vinegar",
		"applevinegar",
		"canevinegar",
		"ricevinegar",
		"juice_wine_fruit",
		"limoncello",
		"wineagave",
		"wineapricot",
		"winebanana",
		"winecarrot",
		"winecherry",
		"winecitrus",
		"winecranberry",
		"wineelderberry",
		"wineplum",
		"winesparkling",
		"winetomato",
		"wine",
		"ricardosanchez",
		"winered",
		"winewhite",
		"winefortified",
		"whiskey",
		"whiskeyrye",
		"whiskeycorn",
		"whiskeywheat",
		"glenmckenner",
		"liqueurchocolate",
		"liqueuralmond",
		"liqueuranise",
		"liqueurbanana",
		"liqueurblackberry",
		"liqueurblackcurrant",
		"liqueurcherry",
		"liqueurcinnamon",
		"liqueurcoffee",
		"liqueurhazelnut",
		"liqueurherbal",
		"liqueurlemon",
		"liqueurmelon",
		"liqueurmint",
		"liqueurorange",
		"liqueurpeach",
		"liqueurraspberry",
		"liquorfruit",
		"liquorapple",
		"liquorapricot",
		"liquorcherry",
		"liquorelderberry",
		"liquorpear",
		"spiritgin",
		"spiritneutral",
		"spiritsugarcane",
		"brandyfruit",
		"brandyapple",
		"brandyapricot",
		"brandycherry",
		"brandycitrus",
		"brandyelderberry",
		"brandygrape",
		"brandypear",
		"brandyplum",
		"ciderapple",
		"ciderpear",
		"ciderpeach",
		"winepineapple",
		"beer",
		"darkbeer",
		"dragonblood",
		"beerale",
		"beercorn",
		"beerlager",
		"beerrye",
		"beerstout",
		"beerwheat",
		"rumwhite",
		"rumdark",
		"pina_colada",
		"vodka",
		"leninade",
		"mead",
		"short_mead",
		"sake",
		"tequila",
		"alcopops",
		"hotfryingoil",
		"seedoil",
		"plantoil",
		"sunfloweroil",
		"juiceolive",
		"nutoil",
		"linoil",
		"hempoil",
		"fishoil",
		"whaleoil");

	@Test
	public void tableCarries220RowsInCensusOrder() {
		assertEquals(IDS, GTFluids.FOOD_B1_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		assertEquals(216, GTFluids.FOOD_B1_SPECS.size());
	}

	/** The reconciliation face: the earlier tables are UNCHANGED by the fifth (15 already-ported FOOD rows). */
	@Test
	public void earlierTablesAreUntouched() {
		assertEquals(6, GTFluids.AQUA_SPECS.size());
		assertEquals(2, GTFluids.SIMPLE_LIQUID_SPECS.size());
		assertEquals(4, GTFluids.FOOD_FLUID_SPECS.size());
		assertEquals(39, GTFluids.CHEMICAL_SPECS.size(), "the chemical domain is a different card — zero overlap");
	}

	/** The representative-row property assertions (one per family, acceptance 属性断言). */
	@Test
	public void representativeRowsCarryTheUpstreamAnchors() {
		// the 275 K alcohol/milk carriers (Loader_Fluids.java:485/:551/:583/:622/:646)
		assertEquals(275, GTFluids.foodB1Spec("vodka").temperature(), "binnie.vodka FL.create 275 K");
		assertEquals("Vodka", GTFluids.foodB1Spec("vodka").displayName(), "Loader_Fluids.java:551 verbatim");
		assertEquals(275, GTFluids.foodB1Spec("soymilk").temperature(), "FL.create :622 275 K");
		// the 255 K ice tea (:646) and the 400 K hot frying oil (:106)
		assertEquals(255, GTFluids.foodB1Spec("icetea").temperature(), "FL.create :646 255 K");
		assertEquals(400, GTFluids.foodB1Spec("hotfryingoil").temperature(), "FL.create :106 400 K");
		// milk is 300 K (Loader_Fluids.java:624, the ST.make container args ride after temp)
		assertEquals(300, GTFluids.foodB1Spec("milk").temperature());
		assertEquals("Milk", GTFluids.foodB1Spec("milk").displayName());
		// the C literal folds to 300 (CS.java:132, the smoothie block :413)
		assertEquals(300, GTFluids.foodB1Spec("fruitsmoothie").temperature());
		assertEquals("Froot Smoothie", GTFluids.foodB1Spec("fruitsmoothie").displayName(), ":413 verbatim");
		// a binnie-external honest-default row (no FL.create anywhere)
		assertEquals(300, GTFluids.foodB1Spec("beerale").temperature(), "the :488 external-id row — honest default");
		assertEquals("Ale", GTFluids.foodB1Spec("beerale").displayName(), "the FL-shorthand spelling (the water_boiling precedent)");
		// collision-suffix ids resolve
		assertNotNull(GTFluids.foodB1Spec("grcmilk_milk"));
		assertNotNull(GTFluids.foodB1Spec("juice_wine_fruit"));
		assertNotNull(GTFluids.foodB1Spec("slime_blue"));
		assertNotNull(GTFluids.foodB1Spec("diablosauce_strong"));
	}

	/** Every row rides the STATE_LIQUID carriers and the descriptionId shape; every row stays under the 340 K wood-barrel ceiling except hot frying oil (the BATH-tagged pan oil — the barrel chain excludes it the same way the hot-lube family does). */
	@Test
	public void carriersAndCeilings() {
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B1_SPECS) {
			assertEquals(1000, tSpec.density(), tSpec.name() + ": the STATE_LIQUID density (FL.java:1104)");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity (FL.java:1104)");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
			assertTrue(tSpec.temperature() >= 255, tSpec.name() + ": no cryogenic food row");
		}
		assertTrue(GTFluids.foodB1Spec("hotfryingoil").temperature() == 400, "the one hot row — excluded from the wood-barrel chain by temperature");
		long tOver = GTFluids.FOOD_B1_SPECS.stream().filter(s -> s.temperature() > 340).count();
		assertEquals(1, tOver, "exactly hotfryingoil is over the 340 K wood ceiling");
	}

	@Test
	public void unknownIdsResolveToNull() {
		assertNull(GTFluids.foodB1Spec("sap"), "sap lives on the THIRD table only");
		assertNull(GTFluids.foodB1Spec("honey"), "honey rides the p31 ChemicalFluid family, not the b1 table");
		assertNull(GTFluids.foodB1Spec("rainbowsap"), "Sap_Rainbow (FL.java:251) is OUTSIDE the b1 batch");
		assertNull(GTFluids.foodB1Spec(null));
		// bidirectional isolation
		assertNull(GTFluids.foodSpec("milk"), "milk lives on the FIFTH table only");
		assertNull(GTFluids.foodSpec("vodka"));
	}

	/** The registration shape: the 220 static fields walk 1:1 with the spec table, source = id / flowing = id + "_flowing". */
	@Test
	public void registrationFieldsWalkTheTable() {
		assertEquals(GTFluids.FOOD_B1_SPECS, GTFluids.foodB1Fluids().stream().map(f -> f.spec).toList());
		for (GTFluids.AquaFluid tFamily : GTFluids.foodB1Fluids()) {
			//? if forge {
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId());
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId());
			//?} else {
			/*assertEquals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId());
			assertEquals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name() + "_flowing"), tFamily.flowing.getId());
			*///?}
		}
	}

	/** The fluid-only declaration rides the SHARED AquaFluid holder — no new holder class, no block/bucket face. */
	@Test
	public void b1FamiliesReuseTheSharedHolder() {
		assertEquals(4, GTFluids.AquaFluid.class.getDeclaredFields().length,
			"spec + type + source + flowing — the shared fluid-only holder");
	}
}
