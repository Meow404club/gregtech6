package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Food-fluid tail offline tests (task p33-food-tail — the b1/b2 尾货清账 card): the
 * FOOD_TAIL_SPECS table reconciliation (the card-block-outside drink fluids — the
 * golden-apple brews Loader_Fluids.java:607-610 + the coffee family :637-642), the
 * DrinkStat reachability the b2 report named (stat rows for :371 mnwtr + the two tail
 * families), the KEPT_FILLS reverse-census closure (the 10 potion.-prefixed rows over
 * the existing b1/p31 ids the b2 walk missed) and the b1 lemonade carrier correction
 * (:603 the 275 K literal). The live drink behaviour is the RCON chain evidence (the
 * p33_food_tail sweep group); offline asserts the declaration tables, the corrections
 * and the seam constants.
 */
public class GTFoodTailTest extends GTOfflineTestBase {

	/** The 10-row reconciliation: the census order :607-610 (the golden-apple brews) + :637-642 (the coffee family), all 300 K carriers. */
	@Test
	public void tableCarries10RowsInCensusOrder() {
		assertEquals(10, GTFluids.FOOD_TAIL_SPECS.size(), "the card-block-outside run :607-610 + :637-642");
		assertEquals(java.util.List.of("potion.goldenapplejuice", "potion.goldencider", "potion.idunsapplejuice", "potion.notchesbrew",
				"potion.darkcoffee", "potion.darkcafeaulait", "potion.coffee", "potion.cafeaulait", "potion.laitaucafe", "potion.darkchocolatemilk"),
			GTFluids.FOOD_TAIL_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList(), "the upstream line order");
		assertEquals("Golden Apple Juice", GTFluids.foodTailSpec("potion.goldenapplejuice").displayName(), ":607 verbatim");
		assertEquals("Idun's Apple Juice", GTFluids.foodTailSpec("potion.idunsapplejuice").displayName(), ":609 verbatim");
		assertEquals("Notches Brew", GTFluids.foodTailSpec("potion.notchesbrew").displayName(), ":610 verbatim");
		assertEquals("Dark Coffee", GTFluids.foodTailSpec("potion.darkcoffee").displayName(), ":637 verbatim");
		assertEquals("Bitter Chocolate Milk", GTFluids.foodTailSpec("potion.darkchocolatemilk").displayName(), ":642 verbatim");
	}

	/** The FL.create carrier column: every tail row rides the 300 K literal (:607-610/:637-642). */
	@Test
	public void carriersAndDescriptionIds() {
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_TAIL_SPECS) {
			assertEquals(300, tSpec.temperature(), tSpec.name() + ": the FL.create 300 K carrier literal");
			assertEquals(1000, tSpec.density(), tSpec.name() + ": the STATE_LIQUID density (FL.java:1104)");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity (FL.java:1104)");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
		}
	}

	/** The registration shape: the 10 static fields walk 1:1 with the spec table. */
	@Test
	public void registrationFieldsWalkTheTable() {
		assertEquals(GTFluids.FOOD_TAIL_SPECS, GTFluids.foodTailFluids().stream().map(f -> f.spec).toList());
		for (GTFluids.AquaFluid tFamily : GTFluids.foodTailFluids()) {
			//? if forge {
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId());
			//?} else {
			/*assertEquals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId());
			*///?}
		}
	}

	/** The lemonade carrier correction: Loader_Fluids.java:603 rides the 275 K FL.create literal (b1 shipped the honest-default 300 K). */
	@Test
	public void lemonadeCarrierIsThe603Literal() {
		assertEquals(275, GTFluids.foodB1Spec("lemonade").temperature(), "FL.create :603 275 K verbatim (the p33-food-tail correction)");
		assertEquals("Lemonade", GTFluids.foodB1Spec("lemonade").displayName(), ":603 verbatim");
	}

	/**
	 * The DrinkStat rows the b2 report named unreachable: the :371 potion.mineralwater row
	 * rides the p16 aqua id "mnwtr" (the KEPT census fill), the golden-apple and coffee
	 * families ride the tail table's registry paths.
	 */
	@Test
	public void tailDrinkStatsAreReachable() {
		GTDrinks.DrinkStat tMnwtr = GTDrinks.stat("mnwtr");
		assertNotNull(tMnwtr, "upstream :371 keyed through the p16 aqua id");
		assertEquals("Stay hydrated!", tMnwtr.tooltip(), ":371 verbatim");
		assertEquals(1, tMnwtr.food(), ":371 food=1");
		assertEquals(40, tMnwtr.hydration(), ":371 hydration=40");
		assertEquals(308, tMnwtr.temperature(), "the C+35 fold (the mineralsoda :372 sibling)");
		assertEquals(1, tMnwtr.effects().size(), ":371 the regeneration effect");
		assertEquals(net.minecraft.world.effect.MobEffects.REGENERATION, tMnwtr.effects().get(0).effect());

		// the golden-apple family: the .setLuminosity(15) rows, absorption = field_76444_x
		GTDrinks.DrinkStat tGolden = GTDrinks.stat("potion.goldenapplejuice");
		assertNotNull(tGolden, "the tail registration keyed");
		assertEquals(15, tGolden.luminosity(), ":607 the setLuminosity(15) face (no AquaFluidSpec column, the goldencarrotjuice :606 precedent)");
		assertEquals(2, tGolden.effects().size(), ":607 absorption + regeneration");
		assertEquals(net.minecraft.world.effect.MobEffects.ABSORPTION, tGolden.effects().get(0).effect(), "field_76444_x = absorption");
		assertEquals(2400, tGolden.effects().get(0).duration(), ":607 verbatim");
		assertEquals(net.minecraft.world.effect.MobEffects.REGENERATION, tGolden.effects().get(1).effect());
		assertEquals(100, tGolden.effects().get(1).duration());
		assertEquals(4, GTDrinks.stat("potion.idunsapplejuice").effects().size(), ":609 the four-effect Notch-apple brew");
		assertEquals(4, GTDrinks.stat("potion.notchesbrew").effects().size(), ":610 the four-effect brew");
		assertEquals(1, GTDrinks.stat("potion.goldencider").effects().size(), ":608 the absorption-only row");

		// the coffee family: the C+39 fold at 312 K, the chocolatemilk sibling C+37 at 310 K
		assertEquals("Just the regular morning Coffee", GTDrinks.stat("potion.coffee").tooltip(), ":639 verbatim");
		assertEquals(312, GTDrinks.stat("potion.coffee").temperature(), ":639 C+39 (the tea-row fold)");
		assertEquals(4, GTDrinks.stat("potion.coffee").food(), ":639 food=4");
		assertEquals(5, GTDrinks.stat("potion.coffee").hydration(), ":639 hydration=5");
		assertEquals(312, GTDrinks.stat("potion.laitaucafe").temperature(), ":641 C+39");
		assertEquals(310, GTDrinks.stat("potion.darkchocolatemilk").temperature(), ":642 C+37");
		assertTrue(GTDrinks.stat("potion.darkcafeaulait").effects().isEmpty(), ":638 the caffeine channel has no modern effect face");
	}

	/** The reverse-census closure: the 10 KEPT fills (upstream potion.-prefixed rows over the existing b1/p31 ids) are keyed, the 8 unported trims stay absent. */
	@Test
	public void keptFillsCloseTheReverseCensus() {
		assertEquals(10, GTDrinks.KEPT_FILLS.size(), "Loader_Fluids.java :486/:496/:552/:553/:566/:567/:568/:569/:576/:591");
		for (String tId : java.util.List.of("sake", "dragonblood", "leninade", "alcopops", "hotsauce", "diabolosauce", "diablosauce", "diablosauce_strong", "ambrosia", "dressing")) {
			assertNotNull(GTDrinks.stat(tId), "the KEPT fill keyed over the ported b1/p31 id");
		}
		// the representative anchors
		assertEquals("[Missing No]", GTDrinks.stat("diablosauce_strong").tooltip(), ":569 verbatim (the FoodStatDrink tooltip; 'There is no Cow Sauce' is the FL.create display face)");
		assertEquals("There is no Cow Sauce", GTFluids.foodB1Spec("diablosauce_strong").displayName(), ":569 the carrier display verbatim");
		assertEquals(15, GTDrinks.stat("diablosauce_strong").luminosity(), ":569 the setLuminosity(15) row");
		assertEquals(323, GTDrinks.stat("diablosauce_strong").temperature(), ":569 the C+50 fold");
		assertEquals(310, GTDrinks.stat("sake").temperature(), ":486 the C+37 fold");
		assertEquals(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, GTDrinks.stat("sake").effects().get(0).effect(), ":486 the damageBoost anchor");
		assertEquals(303, GTDrinks.stat("alcopops").temperature(), ":553 the C+30 fold");
		assertEquals(net.minecraft.world.effect.MobEffects.REGENERATION, GTDrinks.stat("ambrosia").effects().get(0).effect(), ":576 the regeneration anchor over the p31 honey id");
		// the trims: the upstream rows whose fluids have NO modern port (external-mod /
		// unregistered material faces) — the declared absent side of the census
		for (String tTrim : java.util.List.of("error", "glue", "mercury", "saltwater", "tropicswater", "rainbowsap", "sluicejuice")) {
			assertNull(GTDrinks.stat(tTrim), tTrim + ": the unported upstream row stays unkeyed");
		}
	}

	/** The isolation faces: the tail table does not shadow the earlier lookups. */
	@Test
	public void tableIsolation() {
		assertNull(GTFluids.foodB2Spec("potion.coffee"), "the coffee family lives on the SEVENTH table only");
		assertNull(GTFluids.foodB2Spec("potion.goldencider"));
		assertNull(GTFluids.foodTailSpec("potion.tainted"), "the b2 brew block stays on the SIXTH table");
		assertNull(GTFluids.foodB1Spec("potion.darkcoffee"));
		assertNull(GTFluids.foodTailSpec(null));
		assertNull(GTFluids.foodTailSpec("not_a_fluid"));
	}
}
