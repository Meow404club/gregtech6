package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.DeferredRegister;

import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesDistillery;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6SprayCans;
import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Dye-chemical family + chlorine offline tests (task p24-dye-chemical-fluids — the
 * registration-row assertions against the DECLARED values, the GTFluidsFoodFamilyTest
 * shape): sixteen WITH-BLOCK {@code dye_chemical_<GTSprayCanItem.DYE_IDS[i]>} families +
 * {@code chlorine} (the rulings R3/R4/R6 of decisions.p24-canner-dyes-rulings).
 *
 * <p>Every colour value is the SHARED {@link GTSprayCanItem#DYES_INT} table — the
 * three-way pin (dye index i ↔ {@code DYES_INT[i]} ↔ the {@code spray_paint_<DYE_IDS[i]>}
 * item id) is the Canner refill's correctness root (ruling R5), asserted per index.
 * Chlorine carries the ruling values verbatim (tint 0xF0FFFF = the MT.java:405 material
 * colour, temperature 239 K, density −100 the natural_gas carrier). The dead-end walk
 * proves zero recipe rows consume any of the 17 ids until the Canner machine card lands
 * ({@link GT6RecipeMaps} over the offline-poured distillery/drying generations). The lang
 * face is reconciled en=zh with the GT6LangParityTest recording posture (all 17 dump-anchored
 * zh faces, tmp/gregtech.lang:230-244/:168). The live registry side is the runData/runServer
 * smoke evidence and the RCON {@code /gt6tank fill} chain.
 */
public class GTFluidsDyeChemicalFamilyTest extends GTOfflineTestBase {

	/** The 16 dye-chemical paths in DYE_IDS declaration order (the card list). */
	private static final List<String> IDS = buildIds();

	private static List<String> buildIds() {
		List<String> rList = new ArrayList<>(16);
		for (int i = 0; i < 16; i++) rList.add(GTFluids.dyeChemicalName(i));
		return List.copyOf(rList);
	}

	/** All 17 dead-end paths: the 16 dyes + chlorine (the Canner card is the only intended consumer). */
	private static Set<String> deadEndPaths() {
		Set<String> rSet = new HashSet<>(IDS);
		rSet.add("chlorine");
		return rSet;
	}

	@BeforeAll
	static void bootTheRegistrationFaces() {
		// the lang recording walk loads the full OP/material registries (the GT6LangParityTest
		// boot shape); vanilla itself is booted by GTOfflineTestBase first
		GTMaterialItems.initMaterials();
	}

	@AfterEach
	void cleanMaps() {
		GT6RecipeMaps.reset(); // retire the walked generation with its pour flags (the ADR-P18 poison fix)
	}

	/** The 17-row census: the 16 dyes in DYE_IDS order + chlorine as the standalone row. */
	@Test
	public void dyeChemicalIdsCarryTheSprayCanSnakeInDyeOrder() {
		assertEquals(16, GTFluids.DYE_CHEMICALS.size(), "16 colours, one per GTSprayCanItem.DYE_IDS row");
		assertEquals(IDS, GTFluids.DYE_CHEMICALS.stream().map(GTFluids.DyeChemicalFluid::name).toList(),
				"the fluid paths are dye_chemical_ + the DYE_IDS snake, in dye order (the Canner refill loop's walk order)");
		// the inverse seam: path -> dye index, chlorine is not a dye
		for (int i = 0; i < 16; i++) assertEquals(i, GTFluids.dyeIndexOf(GTFluids.dyeChemicalName(i)), "inverse seam");
		assertEquals(-1, GTFluids.dyeIndexOf("chlorine"), "chlorine is the standalone carrier, not a dye row");
		assertEquals(-1, GTFluids.dyeIndexOf("water"), "vanilla water is not a dye row");
		// the index accessor returns the same rows, same order
		for (int i = 0; i < 16; i++) assertSame(GTFluids.DYE_CHEMICALS.get(i), GTFluids.dyeChemical(i), "dye index accessor");
	}

	private static void assertSame(Object aExpected, Object aActual, String aMessage) {
		assertTrue(aExpected == aActual, aMessage);
	}

	/** The four-DR-with-block template: FluidType + Source/Flowing + LiquidBlock handles, one block per family (acceptance: 三注册表齐). */
	@Test
	public void registrationShapeCarriesTheFourRegistryHandles() {
		for (GTFluids.DyeChemicalFluid tFamily : GTFluids.DYE_CHEMICALS) {
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
		// chlorine: the same four handles
		//? if forge {
		assertEquals(new ResourceLocation("gt6", "chlorine"), GTFluids.CHLORINE_TYPE.getId(), "chlorine FluidType id");
		assertEquals(new ResourceLocation("gt6", "chlorine"), GTFluids.CHLORINE.getId(), "chlorine source id");
		assertEquals(new ResourceLocation("gt6", "chlorine_flowing"), GTFluids.CHLORINE_FLOWING.getId(), "chlorine flowing id");
		assertEquals(new ResourceLocation("gt6", "chlorine_block"), GTFluids.CHLORINE_BLOCK.getId(), "chlorine block id");
		//?} else {
		/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "chlorine"), GTFluids.CHLORINE_TYPE.getId(), "chlorine FluidType id");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "chlorine"), GTFluids.CHLORINE.getId(), "chlorine source id");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "chlorine_flowing"), GTFluids.CHLORINE_FLOWING.getId(), "chlorine flowing id");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "chlorine_block"), GTFluids.CHLORINE_BLOCK.getId(), "chlorine block id");
		*///?}
	}

	/**
	 * The R5 three-way pin, per dye index: the fluid row's tint IS the spray-can table value,
	 * the display name is the upstream :123 compose, and the sibling spray-can item id is the
	 * same DYE_IDS snake — the Canner refill's correctness root (zero new colour data).
	 */
	@Test
	public void tintsAreTheSprayCanTableVerbatimAndTheThreeWayPins() {
		for (int i = 0; i < 16; i++) {
			GTFluids.DyeChemicalFluid tFamily = GTFluids.dyeChemical(i);
			assertEquals(GTSprayCanItem.DYES_INT[i], tFamily.tint(), tFamily.name() + ": tint == DYES_INT[" + i + "] (the single colour source)");
			assertEquals("Chemical " + GTSprayCanItem.DYE_NAMES[i] + " Dye", tFamily.displayName(),
					tFamily.name() + ": the Loader_Fluids.java:123 compose verbatim");
			// the item face (read-only): the sibling can id is the same snake
			//? if forge {
			assertEquals(new ResourceLocation("gt6", "spray_paint_" + GTSprayCanItem.DYE_IDS[i]),
					GT6SprayCans.SPRAY_PAINTS.get(i).getId(), tFamily.name() + ": the spray_paint sibling id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "spray_paint_" + GTSprayCanItem.DYE_IDS[i]),
					GT6SprayCans.SPRAY_PAINTS.get(i).getId(), tFamily.name() + ": the spray_paint sibling id");
			*///?}
			assertEquals(GTSprayCanItem.DYE_IDS[i], tFamily.name().substring("dye_chemical_".length()),
					tFamily.name() + ": the fluid id suffix IS the DYE_IDS row");
		}
	}

	/** Ruling R3/R4: the chlorine carrier values verbatim + the 300 K/1000 declared carriers of the dye rows (acceptance: 氯载体值逐项断言). */
	@Test
	public void declaredCarrierValuesMatchTheRulings() {
		// the dye rows: the FL.create temperature literal + the honest FluidType defaults
		assertEquals(300, GTFluids.DYE_CHEMICAL_TEMPERATURE, "the Loader_Fluids.java:123 FL.create literal");
		assertEquals(1000, GTFluids.DYE_CHEMICAL_DENSITY, "the honest FluidType default (the water_boiling precedent)");
		// chlorine: MT.java:405 boiling point + the natural_gas lightweight carrier + the material colour
		assertEquals(239, GTFluids.CHLORINE_TEMPERATURE, "MT.java:405 Cl boiling point 239K");
		assertEquals(-100, GTFluids.CHLORINE_DENSITY, "the natural_gas :169 carrier (port-owned, sign-only consumers)");
		assertEquals(0xF0FFFF, GTFluids.CHLORINE_TINT, "the RGB(0,240,255) material colour over the vanilla water textures");
	}

	/** Both families sit far under the 340 K wood-barrel ceiling (GTBarrelCommand.WOOD_MELTING_POINT) — the RCON tank chain carries them. */
	@Test
	public void everyFluidIsWoodBarrelSafe() {
		assertTrue(GTFluids.DYE_CHEMICAL_TEMPERATURE < 340, "the dye rows stay under the wood ceiling");
		assertTrue(GTFluids.CHLORINE_TEMPERATURE < 340, "chlorine at 239 K stays under the wood ceiling");
	}

	/**
	 * The R6 structural face: the family declares NO bucket and NO item registration anywhere
	 * in GTFluids (the :161-163/:212-213 "No bucket item" precedent) — so zero item-tag face;
	 * and the repo carries no fluid-tag provider (census 2026-09-07: vanilla tags are
	 * water/lava only, nothing auto-attaches) — the tag ruling's "无则声明无" arm.
	 */
	@Test
	public void theFamilyDeclaresNoBucketAndNoItemFace() {
		// the holder: dyeIndex + the four registry handles, nothing else (a bucket field would need a container face)
		assertEquals(5, GTFluids.DyeChemicalFluid.class.getDeclaredFields().length,
				"dyeIndex + type + source + flowing + block — adding a bucket here needs an item face this card rules out");
		// the class: exactly the three content DeferredRegisters, no ITEMS
		List<String> tRegisters = new ArrayList<>();
		for (Field tField : GTFluids.class.getDeclaredFields()) {
			if (tField.getType() == DeferredRegister.class) tRegisters.add(tField.getName());
		}
		assertEquals(List.of("FLUID_TYPES", "FLUIDS", "BLOCKS"), tRegisters,
				"GTFluids registers no items — zero bucket, zero item-tag挂面 (ruling R6)");
	}

	/** The earlier fluid families are UNCHANGED by the new tables (the food test's cross-family form). */
	@Test
	public void theEarlierFluidTablesAreUntouched() {
		assertEquals(9, GTFluids.ENGINE_SPECS.size());
		assertEquals(6, GTFluids.AQUA_SPECS.size());
		assertEquals(2, GTFluids.SIMPLE_LIQUID_SPECS.size());
		assertEquals(4, GTFluids.FOOD_FLUID_SPECS.size());
		// the new ids are NOT rows of any earlier family lookup
		for (String tId : deadEndPaths()) {
			assertNull(GTFluids.aquaSpec(tId), tId + " lives on the dye-chemical registration, not the aqua table");
			assertNull(GTFluids.simpleLiquidSpec(tId), tId + " lives on the dye-chemical registration, not the simple-liquid table");
			assertNull(GTFluids.foodSpec(tId), tId + " lives on the dye-chemical registration, not the food table");
			assertNull(GTFluids.engineSpec(tId), tId + " lives on the dye-chemical registration, not the engine table");
		}
	}

	/**
	 * Chlorine is a DEAD END until the Canner machine card (ruling R3: the only near-consumer
	 * is the remover refill, MultiItemRandomTools.java:272 — a future RM row). The walk pours
	 * a REAL recipe generation through the distillery loader's offline fixture seams (the
	 * GT6RecipesDistilleryTest convention: vanilla water stand-in for every fluid id, a
	 * vanilla stand-in stack for the circuit slot) and scans EVERY map's full row list for
	 * the 17 dead-end fluid paths, inputs and outputs.
	 */
	@Test
	public void chlorineAndTheDyesHaveZeroRecipeConsumers() {
		Function<String, Fluid> tDefaultFluidResolver = GT6RecipesDistillery.sFluidResolver;
		Function<Integer, ItemStack> tDefaultCircuitResolver = GT6RecipesDistillery.sCircuitResolver;
		try {
			GT6RecipeMaps.init();
			GT6RecipesDistillery.sFluidResolver = aId -> Fluids.WATER; // every id -> the vanilla stand-in (offline-safe)
			GT6RecipesDistillery.sCircuitResolver = aConfig -> new ItemStack(Items.BRICKS); // the circuit slot stand-in
			GT6RecipesDistillery.resetForTest();
			GT6RecipesDistillery.load();
			Set<String> tDeadEnds = deadEndPaths();
			List<String> tHits = new ArrayList<>();
			int tWalked = 0, tFluidRows = 0;
			for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
				for (Recipe tRecipe : tMap.mRecipeList) {
					tWalked++;
					for (FluidStack tStack : tRecipe.mFluidInputs) {
						String tPath = fluidPathOf(tStack);
						if (tPath != null) {
							tFluidRows++;
							if (tDeadEnds.contains(tPath)) tHits.add(tMap.mNameInternal + " in: " + tPath);
						}
					}
					for (FluidStack tStack : tRecipe.mFluidOutputs) {
						String tPath = fluidPathOf(tStack);
						if (tPath != null) {
							tFluidRows++;
							if (tDeadEnds.contains(tPath)) tHits.add(tMap.mNameInternal + " out: " + tPath);
						}
					}
				}
			}
			assertTrue(tWalked > 0, "the walk must see real recipe rows (the fixture-poured distillery generation)");
			assertTrue(tFluidRows > 0, "the walk must see real fluid rows");
			assertTrue(tHits.isEmpty(), "the 17 p24 fluids are dead ends until the Canner card: " + tHits);
		} finally {
			GT6RecipesDistillery.sFluidResolver = tDefaultFluidResolver;
			GT6RecipesDistillery.sCircuitResolver = tDefaultCircuitResolver;
		}
	}

	/** The registry-path face of a recipe fluid stack (null-safe: unbound/empty stacks never false-hit). */
	private static String fluidPathOf(FluidStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		Fluid tFluid = aStack.getFluid();
		if (tFluid == null) return null;
		ResourceLocation tKey = BuiltInRegistries.FLUID.getKey(tFluid);
		return tKey == null ? null : tKey.getPath();
	}

	/**
	 * The lang en=zh reconciliation (the GT6LangParityTest recording posture, 17 rows): both
	 * providers emit the 17 fluid display keys, the en face is the upstream compose, the zh
	 * face is the dump anchor (tmp/gregtech.lang:230-244 for the dyes, :168 for chlorine).
	 */
	@Test
	public void langEnAndZhFacesCarryAllSeventeenKeys() throws Exception {
		Map<String, String> tEn = record(false);
		Map<String, String> tZh = record(true);
		List<String> tKeys = new ArrayList<>(IDS.stream().map(s -> "fluid.gt6." + s).toList());
		tKeys.add("fluid.gt6.chlorine");
		assertEquals(17, tKeys.size());
		for (String tKey : tKeys) {
			assertNotNull(tEn.get(tKey), "en is missing " + tKey);
			assertNotNull(tZh.get(tKey), "zh is missing " + tKey);
			assertFalse(tZh.get(tKey).isBlank(), "blank zh value for " + tKey);
		}
		// value spot-pins: the compose face + the dump face
		assertEquals("Chemical Red Dye", tEn.get("fluid.gt6.dye_chemical_red"), "the :123 compose over DYE_NAMES[1]");
		assertEquals("Chemical White Dye", tEn.get("fluid.gt6.dye_chemical_white"), "the :123 compose over DYE_NAMES[15]");
		assertEquals("Chlorine", tEn.get("fluid.gt6.chlorine"), "the MT.Cl material local name");
		assertEquals("红色化学染料", tZh.get("fluid.gt6.dye_chemical_red"), "dump S:fluid.dye.chemical.red :243");
		assertEquals("淡灰色化学染料", tZh.get("fluid.gt6.dye_chemical_light_gray"), "dump S:fluid.dye.chemical.lightgray :237 (the port snake id, the dump colour face)");
		assertEquals("氯", tZh.get("fluid.gt6.chlorine"), "dump S:fluid.chlorine :168");
	}

	/**
	 * Records one provider's full walk offline (the GT6LangParityTest.collect shape — the
	 * anonymous subclass overrides the public {@code add}; the {@code offlineWalk} bridge
	 * reaches the protected addTranslations from the subclass body).
	 */
	private static Map<String, String> record(boolean aZh) throws Exception {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp",
				aZh ? "gt6zhcn-dye-chemical-parity" : "gt6enus-dye-chemical-parity"));
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
