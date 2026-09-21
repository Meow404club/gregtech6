package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Food-fluid batch-2 offline tests (task p33-food-fluids-b2 — the drink-seam card): the
 * FOOD_B2_SPECS table reconciliation (94 potion rows + 9 residual FOOD-flag drink fluids)
 * and the GTDrinks map reconciliation (the DrinksGT.REGISTER mirror — every REGISTER key
 * resolves to a gt6 fluid path registered on one of the six tables or the chemical family).
 * The live drink behaviour is the RCON chain evidence (the p33_food_drink sweep group);
 * offline asserts the declaration tables, the map closure and the seam constants.
 */
public class GTDrinksB2Test extends GTOfflineTestBase {

	@Test
	public void tableCarries103RowsInCensusOrder() {
		assertEquals(103, GTFluids.FOOD_B2_SPECS.size(), "94 potion + 9 residual FOOD-flag drink fluids");
		assertEquals(94, GTFluids.FOOD_B2_SPECS.stream().map(GTFluids.AquaFluidSpec::name).filter(n -> n.startsWith("potion.")).count(),
			"the card-block potion run Loader_Fluids.java:230-350");
		assertEquals(9, GTFluids.FOOD_B2_SPECS.stream().map(GTFluids.AquaFluidSpec::name).filter(n -> !n.startsWith("potion.")).count(),
			"riverwater/rottendrink/poison/chocolatemilk/medicine.heal/medicine.laxative/goldencarrotjuice/holywater + distilled alias");
	}

	/** The representative rows carry the upstream anchors (one per family, the b1 shape). */
	@Test
	public void representativeRowsCarryTheUpstreamAnchors() {
		assertEquals("Tainted Brew", GTFluids.foodB2Spec("potion.tainted").displayName(), "Loader_Fluids.java:230 verbatim");
		assertEquals(300, GTFluids.foodB2Spec("potion.tainted").temperature(), ":230 verbatim (the FL.create 300 K carrier — the C+37 drink-side fold rides GTDrinks)");
		assertEquals("Harming Brew", GTFluids.foodB2Spec("potion.damage").displayName(), ":235 verbatim");
		assertEquals("Stretched Lingering Invisible Brew", GTFluids.foodB2Spec("potion.invisibility.long.lingering").displayName(), ":348 verbatim");
		assertEquals("River Water", GTFluids.foodB2Spec("riverwater").displayName(), ":361 verbatim");
		assertEquals("Rotten Drink", GTFluids.foodB2Spec("rottendrink").displayName(), ":626 verbatim");
		assertEquals("Chocolate Milk", GTFluids.foodB2Spec("chocolatemilk").displayName(), ":643 verbatim");
		assertEquals("Medicine", GTFluids.foodB2Spec("medicine.heal").displayName(), ":649 verbatim");
		assertEquals("Laxative", GTFluids.foodB2Spec("medicine.laxative").displayName(), ":650 verbatim");
		assertNotNull(GTFluids.foodB2Spec("poison"), "the honest-default row (no FL.create, the water_boiling precedent)");
	}

	/** Every row rides the STATE_LIQUID carriers, the descriptionId shape and the FL.create carrier temps. */
	@Test
	public void carriersAndDescriptionIds() {
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_B2_SPECS) {
			assertEquals(1000, tSpec.density(), tSpec.name() + ": the STATE_LIQUID density (FL.java:1104)");
			assertEquals(1000, tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity (FL.java:1104)");
			assertEquals("fluid.gt6." + tSpec.name(), tSpec.descriptionId(), tSpec.name() + ": the descriptionId shape");
		}
		// the FL.create carrier column: the brew block 300 K (:230-294/:319-348), fireresistance
		// 375 K (:271-274/:337-338), riverwater C=273 K (:361), rottendrink 275 K (:626), the
		// rest of the residuals the 300 K literal/honest-default
		assertEquals(95, GTFluids.FOOD_B2_SPECS.stream().filter(s -> s.temperature() == 300).count(), "the 300 K carrier rows (95 = 103 - 6 fireresistance - riverwater - rottendrink)");
		assertEquals(6, GTFluids.FOOD_B2_SPECS.stream().filter(s -> s.temperature() == 375).count(), "the fireresistance 375 K carriers (:271-274, :337-338 — no strong variant exists)");
		assertEquals(1, GTFluids.FOOD_B2_SPECS.stream().filter(s -> s.temperature() == 273).count(), "riverwater C=273 (:361, CS.java:132)");
		assertEquals(1, GTFluids.FOOD_B2_SPECS.stream().filter(s -> s.temperature() == 275).count(), "rottendrink 275 K (:626)");
		assertEquals(300, GTFluids.foodB2Spec("potion.tainted").temperature(), ":230 verbatim (the FL.create 300 K carrier — the C+37 fold is the GTDrinks DRINK stat, not this column)");
		assertEquals(375, GTFluids.foodB2Spec("potion.fireresistance").temperature(), ":271 verbatim");
		assertEquals(375, GTFluids.foodB2Spec("potion.fireresistance.long.lingering").temperature(), ":338 verbatim");
		assertEquals(273, GTFluids.foodB2Spec("riverwater").temperature(), ":361 C (CS.java:132)");
		assertEquals(300, GTFluids.foodB2Spec("holywater").temperature(), ":615 verbatim");
		assertEquals(275, GTFluids.foodB2Spec("rottendrink").temperature(), ":626 verbatim");
		assertEquals(300, GTFluids.foodB2Spec("ic2distilledwater").temperature(), ":75 alias — the engine distilled_water twin");
	}

	/** The registration shape: the 103 static fields walk 1:1 with the spec table. */
	@Test
	public void registrationFieldsWalkTheTable() {
		assertEquals(GTFluids.FOOD_B2_SPECS, GTFluids.foodB2Fluids().stream().map(f -> f.spec).toList());
		for (GTFluids.AquaFluid tFamily : GTFluids.foodB2Fluids()) {
			//? if forge {
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", tFamily.spec.name()), tFamily.source.getId());
			//?} else {
			/*assertEquals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tFamily.spec.name()), tFamily.source.getId());
			*///?}
		}
	}

	/** The earlier tables are UNCHANGED by the sixth (the b1 reconciliation shape). */
	@Test
	public void earlierTablesAreUntouched() {
		assertEquals(6, GTFluids.AQUA_SPECS.size());
		assertEquals(2, GTFluids.SIMPLE_LIQUID_SPECS.size());
		assertEquals(4, GTFluids.FOOD_FLUID_SPECS.size());
		assertEquals(216, GTFluids.FOOD_B1_SPECS.size());
	}

	/**
	 * The DrinkStat map reconciliation (the DrinksGT.REGISTER mirror): every REGISTER key
	 * resolves to a registered gt6 fluid (one of the six AquaFluid tables, the chemical
	 * family or the engine family) or rides the one vanilla-carried exemption ("water" —
	 * minecraft:water, the upstream :360 row, no gt6 fluid id); the potion rows are all
	 * present; the seam threshold is the upstream 250 mB.
	 */
	@Test
	public void drinkStatMapClosesOntoRegisteredFluids() {
		assertEquals(214, GTDrinks.KEPT_SPECS.size(), "the kept census: 210 ported-fluid rows + the juice_juice/lemonade/cavejohnsonsgrenadejuice fillers + the vanilla water row");
		assertEquals(9, GTDrinks.RESIDUAL_SPECS.size(), "the b2 registrations");
		assertEquals(94, GTDrinks.POTION_SPECS.size(), "the Loader_Fluids.java:230-350 card block (64 main :230-294 + 30 lingering :319-348)");
		assertEquals(317, GTDrinks.REGISTER.size(), "214 kept + 9 residual + 94 potion — all keys unique (the LinkedHashMap.put mirror)");
		java.util.Set<String> tFluidIds = new java.util.HashSet<>();
		for (List<GTFluids.AquaFluid> tWalk : List.of(GTFluids.aquaFluids(), GTFluids.simpleLiquids(), GTFluids.foodFluids(), GTFluids.foodB1Fluids(), GTFluids.foodB2Fluids())) {
			for (GTFluids.AquaFluid tFamily : tWalk) tFluidIds.add(tFamily.spec.name());
		}
		// the chemical + engine families carry keyed rows too (honey/ice/lubricant/distilled_water/...)
		for (List<GTFluids.ChemicalFluid> tWalk : List.of(GTFluids.CHEMICALS, GTFluids.HONEY_FLUIDS, GTFluids.BEE_ROW_FLUIDS, GTFluids.HOT_FLUIDS, GTFluids.CLOSURE_FLUIDS, GTFluids.LUBRICANT_FLUIDS, GTFluids.QU_FLUIDS)) {
			for (GTFluids.ChemicalFluid tFamily : tWalk) tFluidIds.add(tFamily.spec.name());
		}
		for (GTFluids.EngineFluidSpec tSpec : GTFluids.ENGINE_SPECS) tFluidIds.add(tSpec.name());
		for (String tKey : GTDrinks.REGISTER.keySet()) {
			assertTrue(tFluidIds.contains(tKey) || tKey.equals("water"),
				"REGISTER key " + tKey + " resolves to a registered gt6 fluid (or the vanilla water exemption)");
		}
		assertEquals(250, GTDrinks.DRINK_MB, "the upstream mTank.has(250) threshold verbatim (TileEntityBase08FluidContainer.java:416)");
		assertNotNull(GTDrinks.stat("potion.tainted"), "the potion rows are keyed");
		assertNotNull(GTDrinks.stat("vodka"), "the b1 alcohol rows are keyed (the binnie alias ride)");
		assertNotNull(GTDrinks.stat("riverwater"));
		assertNotNull(GTDrinks.stat("water"), "the vanilla-carried row (minecraft:water barrels)");
		assertNotNull(GTDrinks.stat("juice_juice"));
		assertNotNull(GTDrinks.stat("lemonade"));
		assertNotNull(GTDrinks.stat("cavejohnsonsgrenadejuice"));
		assertNull(GTDrinks.stat(null));
		assertNull(GTDrinks.stat("not_a_fluid"));
	}

	/** The potion rows carry the mapped vanilla effects (the field_76444_x = ABSORPTION anchor). */
	@Test
	public void potionRowsCarryTheMappedEffects() {
		GTDrinks.DrinkStat tTainted = GTDrinks.stat("potion.tainted");
		assertEquals(2, tTainted.effects().size(), "Loader_Fluids.java:230 — poison + hunger");
		assertEquals(net.minecraft.world.effect.MobEffects.POISON, tTainted.effects().get(0).effect());
		assertEquals(100, tTainted.effects().get(0).duration());
		assertEquals(3, tTainted.effects().get(0).amplifier());
		assertEquals(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, GTDrinks.stat("potion.speed").effects().get(0).effect(),
			"the Potion.moveSpeed mapping (the :277 row)");
		assertTrue(GTDrinks.stat("potion.damage.strong").effects().get(0).effect() == net.minecraft.world.effect.MobEffects.HARM,
			"the instant-damage anchor :236");
		assertTrue(GTDrinks.stat("potion.awkward").effects().isEmpty(), ":232 — the no-effect brew");
	}

	/** The tooltip lines ride the rows verbatim (the drink feedback face). */
	@Test
	public void tooltipsRideTheUpstreamLines() {
		assertEquals("tainted between the lands", GTDrinks.stat("potion.tainted").tooltip(), ":230 verbatim");
		assertEquals("Industrial Use ONLY!", GTDrinks.stat("lubricant").tooltip(), ":617 verbatim");
		assertEquals("Lloyd? Water!", GTDrinks.stat("riverwater").tooltip(), ":361 verbatim");
	}

	/** The isolation faces: the b2 table does not shadow the b1/b3 lookups. */
	@Test
	public void tableIsolation() {
		assertNull(GTFluids.foodB2Spec("milk"), "milk lives on the FIFTH table only");
		assertNull(GTFluids.foodB2Spec("honey"), "honey rides the p31 chemical family");
		assertNull(GTFluids.foodB1Spec("potion.tainted"));
		assertNull(GTFluids.foodSpec("potion.tainted"));
		assertFalse(GTDrinks.stat("waterdirty") == null || GTDrinks.stat("seawater") == null,
			"the simple-liquid rows are keyed (they carry FoodStatDrink rows upstream)");
	}
}
