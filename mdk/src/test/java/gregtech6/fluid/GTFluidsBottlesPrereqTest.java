package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Bottle-prereq fluid rows offline tests (task btl-fluids-prereq — the registration-row
 * assertions against the DECLARED values, the GTFluidsDyeChemicalFamilyTest shape): the
 * MultiItemBottles census' fluid-gap closeout (state research.bottles-census) — the THREE
 * simple-liquid rows (swampwater FL.java:129 / stagnantwater :128 / tar :435) and the TWO
 * dye bottle-fluid families ×16 (Loader_Fluids.java:121-122, the :123 dye-chemical loop's
 * sibling rows). 35 rows total, all GT6-own domain (no driver gate, no shells).
 *
 * <p>The tar row is the FL.Tar MAIN id only — the "tarfluid" alias is UNVERIFIED and
 * stays unregistered (the ADR-MDH5 single-name ruling), asserted as a negative pin; and
 * MT.Tar is an unused material (MT.java:4033), so there is no material-fluid face either.
 * The bottles that carry these fluids are the follow-up MultiItemBottles cards' scope —
 * zero bottle registrations, zero recipe rows (the lang face is reconciled en=zh, the
 * live-registry side is the runData smoke + the RCON exemption declared on the card).
 */
public class GTFluidsBottlesPrereqTest extends GTOfflineTestBase {

	/** The bottle-prereq trio in declaration order (the SIMPLE_LIQUID_SPECS tail). */
	private static final List<String> TRIO = List.of("swampwater", "stagnantwater", "tar");

	/** The 16 ids per compose family, built from the spray-can snake (the same derivation the rows and the FluidType descriptionIds use). */
	private static List<String> buildIds(String aFamily) {
		List<String> rList = new ArrayList<>(16);
		for (int i = 0; i < 16; i++) rList.add("dye_" + aFamily + "_" + GTSprayCanItem.DYE_IDS[i]);
		return List.copyOf(rList);
	}

	@BeforeAll
	static void bootTheRegistrationFaces() {
		// the lang recording walk loads the full OP/material registries (the GT6LangParityTest
		// boot shape); vanilla itself is booted by GTOfflineTestBase first
		GTMaterialItems.initMaterials();
	}

	/** The 35-row census head: the trio on the SECOND table, the two families at 16 each, GT6-own domain (no shells). */
	@Test
	public void theThirtyFiveRowsExistOnTheirDeclaredFamilies() {
		// the trio: spec existence + family belonging (the simple-liquid table tail)
		assertEquals(List.of("seawater", "waterdirty", "brine", "spruceresin", "swampwater", "stagnantwater", "tar"),
				GTFluids.SIMPLE_LIQUID_SPECS.stream().map(GTFluids.AquaFluidSpec::name).toList());
		for (String tId : TRIO) {
			assertNotNull(GTFluids.simpleLiquidSpec(tId), tId + " must be a simple-liquid row");
			assertNull(GTFluids.aquaSpec(tId), tId + " lives on the SECOND table only");
			assertNull(GTFluids.foodSpec(tId), tId + " lives on the SECOND table only");
			assertNull(GTFluids.engineSpec(tId), tId + " lives on the SECOND table only");
		}
		// the two families: 16 per compose family, in dye order
		assertEquals(16, GTFluids.DYE_WATERMIXED.size(), "16 watermixed colours");
		assertEquals(16, GTFluids.DYE_FLOWER.size(), "16 flower colours");
		assertEquals(buildIds("watermixed"), GTFluids.DYE_WATERMIXED.stream().map(GTFluids.DyeFluid::name).toList(),
				"the watermixed paths are dye_watermixed_ + the DYE_IDS snake, in dye order (Loader_Fluids.java:121 verbatim compose)");
		assertEquals(buildIds("flower"), GTFluids.DYE_FLOWER.stream().map(GTFluids.DyeFluid::name).toList(),
				"the flower paths are dye_flower_ + the DYE_IDS snake, in dye order (Loader_Fluids.java:122 verbatim compose)");
		for (int i = 0; i < 16; i++) {
			assertEquals(i, GTFluids.DYE_WATERMIXED.get(i).dyeIndex, "watermixed walk order");
			assertEquals(i, GTFluids.DYE_FLOWER.get(i).dyeIndex, "flower walk order");
			assertEquals("watermixed", GTFluids.DYE_WATERMIXED.get(i).family, "the compose family field");
			assertEquals("flower", GTFluids.DYE_FLOWER.get(i).family, "the compose family field");
		}
	}

	/** The trio's declared values: the honest FluidType defaults (no FL.create, no dump face) + port-owned tints + the upstream displays. */
	@Test
	public void theTrioCarriesTheHonestDefaults() {
		GTFluids.AquaFluidSpec tSwamp = GTFluids.simpleLiquidSpec("swampwater");
		assertNotNull(tSwamp);
		assertEquals(300, tSwamp.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Swampwater", tSwamp.displayName(), "the id spelled out (the seawater common-name precedent)");
		assertEquals(0xFF556B2F, tSwamp.tint(), "the declared murky swamp green (no upstream texture exists to borrow)");

		GTFluids.AquaFluidSpec tStagnant = GTFluids.simpleLiquidSpec("stagnantwater");
		assertNotNull(tStagnant);
		assertEquals(300, tStagnant.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Stagnant Water", tStagnant.displayName(), "the FL field Stagnant_Water spelled out (the water_boiling precedent)");
		assertEquals(0xFF4E5B33, tStagnant.tint(), "the declared stagnant pond green");

		GTFluids.AquaFluidSpec tTar = GTFluids.simpleLiquidSpec("tar");
		assertNotNull(tTar);
		assertEquals(300, tTar.temperature(), "FluidType.java:925 default — the honest default, not fabricated");
		assertEquals("Tar", tTar.displayName(), "the FL shorthand spelled out");
		assertEquals(0xFF2E2620, tTar.tint(), "the declared tar black-brown");

		// the shared liquid carriers + the wood-barrel ceiling (the RCON tank chain)
		for (String tId : TRIO) {
			GTFluids.AquaFluidSpec tSpec = GTFluids.simpleLiquidSpec(tId);
			assertNotNull(tSpec);
			assertEquals(1000, tSpec.density(), tId + ": FluidType.java:924 default, the STATE_LIQUID carrier");
			assertEquals(1000, tSpec.viscosity(), tId + ": FL.java:1104 STATE_LIQUID viscosity");
			assertEquals("fluid.gt6." + tId, tSpec.descriptionId(), tId + ": the descriptionId shape");
			assertTrue(tSpec.temperature() < 340, tId + ": wood-barrel safe (under the 340 K ceiling)");
		}
	}

	/**
	 * The tar single-name ruling: the FL.java:435 alias "tarfluid" is UNVERIFIED and stays
	 * unregistered (ADR-MDH5) — the negative pin over every lookup seam.
	 */
	@Test
	public void theTarfluidAliasStaysUnregistered() {
		assertNull(GTFluids.simpleLiquidSpec("tarfluid"), "the alias is not a simple-liquid row (ADR-MDH5 UNVERIFIED)");
		for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) {
			assertFalse(tSpec.name().equals("tarfluid"), "no spec row carries the alias id");
		}
		assertNull(GTFluids.aquaSpec("tarfluid"), "the alias is not an aqua row");
		assertNull(GTFluids.foodSpec("tarfluid"), "the alias is not a food row");
		assertNull(GTFluids.engineSpec("tarfluid"), "the alias is not an engine row");
		assertNull(GTFluids.chemicalSpec("tarfluid"), "the alias is not a chemical row");
		assertNull(GTFluids.liveFluidSource("tarfluid"), "no table carries the alias — the seam answers null, never an NPE");
		// and tar itself has no material-fluid face: MT.Tar is unused (MT.java:4033)
		assertNull(GTFluids.chemicalSpec("tar"), "tar lives on the simple-liquid table only — no material bridge");
	}

	/** The single colour source: both families' tints ARE the spray-can table values, and the display composes are the upstream :121/:122 literals. */
	@Test
	public void tintsAreTheSprayCanTableAndTheComposesAreVerbatim() {
		for (int i = 0; i < 16; i++) {
			GTFluids.DyeFluid tWater = GTFluids.DYE_WATERMIXED.get(i);
			GTFluids.DyeFluid tFlower = GTFluids.DYE_FLOWER.get(i);
			assertEquals(GTSprayCanItem.DYES_INT[i], tWater.tint(), tWater.name() + ": tint == DYES_INT[" + i + "]");
			assertEquals(GTSprayCanItem.DYES_INT[i], tFlower.tint(), tFlower.name() + ": tint == DYES_INT[" + i + "]");
			assertEquals("Water Mixed " + GTSprayCanItem.DYE_NAMES[i] + " Dye", tWater.displayName(),
					tWater.name() + ": the Loader_Fluids.java:121 compose verbatim");
			assertEquals(GTSprayCanItem.DYE_NAMES[i] + " Flower Dye", tFlower.displayName(),
					tFlower.name() + ": the Loader_Fluids.java:122 compose verbatim");
			assertEquals(GTSprayCanItem.DYE_IDS[i], tWater.name().substring("dye_watermixed_".length()),
					tWater.name() + ": the fluid id suffix IS the DYE_IDS row");
		}
	}

	/** The four-DR-with-block template per family row: FluidType + Source/Flowing + LiquidBlock handles (the dye-chemical shape). */
	@Test
	public void registrationShapeCarriesTheFourRegistryHandles() {
		for (List<GTFluids.DyeFluid> tFamilies : List.of(GTFluids.DYE_WATERMIXED, GTFluids.DYE_FLOWER)) {
			for (GTFluids.DyeFluid tFamily : tFamilies) {
				//? if forge {
				ResourceLocation tBase = new ResourceLocation("gt6", tFamily.name());
				assertEquals(tBase, tFamily.type.getId(), tFamily.name() + ": FluidType id");
				assertEquals(tBase, tFamily.source.getId(), tFamily.name() + ": source fluid id");
				assertEquals(new ResourceLocation("gt6", tFamily.name() + "_flowing"), tFamily.flowing.getId(), tFamily.name() + ": flowing fluid id");
				assertEquals(new ResourceLocation("gt6", tFamily.name() + "_block"), tFamily.block.getId(), tFamily.name() + ": liquid block id");
				//?} else {
				/*ResourceLocation tBase = ResourceLocation.fromNamespaceAndPath("gt6", tFamily.name());
				assertEquals(tBase, tFamily.type.getId(), tFamily.name() + ": FluidType id");
				assertEquals(tBase, tFamily.source.getId(), tFamily.name() + ": source fluid id");
				assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.name() + "_flowing"), tFamily.flowing.getId(), tFamily.name() + ": flowing fluid id");
				assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", tFamily.name() + "_block"), tFamily.block.getId(), tFamily.name() + ": liquid block id");
				*///?}
				assertEquals("fluid.gt6." + tFamily.name(), tFamily.descriptionId(), tFamily.name() + ": descriptionId shape");
			}
		}
		// the trio rides the shared aqua body: source = the id, flowing = id + "_flowing"
		// (offline the handles are unbound — the spec-list/live-list alignment is the pin)
		assertEquals(GTFluids.SIMPLE_LIQUID_SPECS, GTFluids.simpleLiquids().stream().map(f -> f.spec).toList(),
				"the spec list and the live handles stay aligned, same order (the trio included)");
	}

	/** The two families are disjoint and the earlier fluid tables are UNCHANGED by this card (the cross-family ratchet form). */
	@Test
	public void theFamiliesAreDisjointAndTheEarlierTablesUntouched() {
		Set<String> tWater = new HashSet<>(buildIds("watermixed"));
		Set<String> tFlower = new HashSet<>(buildIds("flower"));
		assertTrue(tWater.stream().noneMatch(tFlower::contains), "the compose families never share an id");
		// and neither family collides with the dye-chemical family (dye_chemical_ prefix) —
		// the dyeIndexOf inverse seam stays blind to the bottle families
		assertEquals(-1, GTFluids.dyeIndexOf("dye_watermixed_black"), "the bottle family is not a dye-chemical row");
		assertEquals(-1, GTFluids.dyeIndexOf("dye_flower_white"), "the bottle family is not a dye-chemical row");
		// the earlier tables' exact counts (the census ratchet the merge order rides)
		assertEquals(9, GTFluids.ENGINE_SPECS.size());
		assertEquals(6, GTFluids.AQUA_SPECS.size());
		assertEquals(4, GTFluids.FOOD_FLUID_SPECS.size());
		assertEquals(16, GTFluids.DYE_CHEMICALS.size(), "the p24 dye-chemical family stays exactly 16");
		// no dye id is a simple-liquid/aqua/food row
		for (String tId : tWater) {
			assertNull(GTFluids.simpleLiquidSpec(tId), tId + " lives on the bottle family, not the simple-liquid table");
		}
	}

	/**
	 * The lang en=zh reconciliation (the GT6LangParityTest recording posture, 35 rows): both
	 * providers emit the trio + the 32 family keys; en is the compose/display face, zh the
	 * dump faces verbatim (tmp/gregtech.lang:262-277 watermixed / :246-261 flower) and the
	 * three hand rows (no dump face — the spruceresin precedent).
	 */
	@Test
	public void langEnAndZhFacesCarryAllThirtyFiveKeys() throws Exception {
		Map<String, String> tEn = record(false);
		Map<String, String> tZh = record(true);
		List<String> tKeys = new ArrayList<>();
		for (String tId : TRIO) tKeys.add("fluid.gt6." + tId);
		for (int i = 0; i < 16; i++) tKeys.add("fluid.gt6." + GTFluids.DYE_WATERMIXED.get(i).name());
		for (int i = 0; i < 16; i++) tKeys.add("fluid.gt6." + GTFluids.DYE_FLOWER.get(i).name());
		assertEquals(35, tKeys.size());
		for (String tKey : tKeys) {
			assertNotNull(tEn.get(tKey), "en is missing " + tKey);
			assertNotNull(tZh.get(tKey), "zh is missing " + tKey);
			assertFalse(tZh.get(tKey).isBlank(), "blank zh value for " + tKey);
		}
		// value spot-pins: the compose faces + the dump faces + the three hand rows
		assertEquals("Water Mixed Red Dye", tEn.get("fluid.gt6.dye_watermixed_red"), "the :121 compose over DYE_NAMES[1]");
		assertEquals("Red Flower Dye", tEn.get("fluid.gt6.dye_flower_red"), "the :122 compose over DYE_NAMES[1]");
		assertEquals("Stagnant Water", tEn.get("fluid.gt6.stagnantwater"), "the FL field spelled out");
		assertEquals("红色水性染料", tZh.get("fluid.gt6.dye_watermixed_red"), "dump S:fluid.dye.watermixed.red :275");
		assertEquals("淡灰色植物染料", tZh.get("fluid.gt6.dye_flower_light_gray"), "dump S:fluid.dye.flower.lightgray :253 (the port snake id, the dump colour face)");
		assertEquals("沼泽水", tZh.get("fluid.gt6.swampwater"), "the hand row (no dump face)");
		assertEquals("死水", tZh.get("fluid.gt6.stagnantwater"), "the hand row (no dump face)");
		assertEquals("焦油", tZh.get("fluid.gt6.tar"), "the hand row (no dump face)");
	}

	/**
	 * Records one provider's full walk offline (the GT6LangParityTest.collect shape — the
	 * anonymous subclass overrides the public {@code add}; the {@code offlineWalk} bridge
	 * reaches the protected addTranslations from the subclass body).
	 */
	private static Map<String, String> record(boolean aZh) throws Exception {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp",
				aZh ? "gt6zhcn-bottles-prereq-parity" : "gt6enus-bottles-prereq-parity"));
		if (aZh) {
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}

				public void offlineWalk() throws Exception {
					addTranslations();
				}
			}.offlineWalk();
		} else {
			new GT6EnUs(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}

				public void offlineWalk() throws Exception {
					addTranslations();
				}
			}.offlineWalk();
		}
		return tEntries;
	}

	/** Duplicate-key guard (the parity test posture — a double add is a hard failure). */
	private static void recordNew(Map<String, String> aEntries, String aKey, String aValue) {
		if (aEntries.put(aKey, aValue) != null) {
			throw new IllegalStateException("Duplicate translation key " + aKey);
		}
	}
}
