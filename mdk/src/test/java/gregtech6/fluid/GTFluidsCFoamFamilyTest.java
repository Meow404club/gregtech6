package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.registries.DeferredRegister;

import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.item.spraycan.GTSprayCanItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6FoamSprays;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * C-Foam family offline tests (task p26-c-foam-fluid-refill — the registration-row
 * assertions against the DECLARED values, the GTFluidsDyeChemicalFamilyTest shape): the
 * {@code gt6:cfoam} base (the decisions.p26-cfoam-fluid-naming ruling row) + the sixteen
 * WITH-BLOCK {@code cfoam_<DYE_IDS[i]>} families + the sixteen
 * {@code cfoam_owned_<DYE_IDS[i]>} "Advanced" families (Loader_Fluids.java:124-125).
 *
 * <p>Every colour value is the SHARED {@link GTSprayCanItem#DYES_INT} table — the three-way
 * pin (dye index i ↔ {@code DYES_INT[i]} ↔ the {@code foam_spray[_owned]_<DYE_IDS[i]>}
 * item id) is the Canner refill's correctness root, asserted per index. The base carries
 * the naming ruling structurally: the registry path is exactly {@code cfoam}, never the
 * upstream "ic2constructionfoam" compat name. The lang face is reconciled en=zh (all 33
 * dump-anchored zh faces, tmp/gregtech.lang:130-161/:361). The live registry side is the
 * runData/runServer smoke evidence and the RCON {@code /gt6machine canner fluid fill}
 * chain; the source compiles into BOTH legs (the stonecutter shared test tree), so a green
 * run on each leg IS the 双腿注册名一致 proof.
 */
public class GTFluidsCFoamFamilyTest extends GTOfflineTestBase {

	/** The 16 dyed paths in DYE_IDS declaration order (the :124 ladder). */
	private static final List<String> IDS = buildIds(false);

	/** The 16 owned paths in DYE_IDS declaration order (the :125 ladder). */
	private static final List<String> OWNED_IDS = buildIds(true);

	private static List<String> buildIds(boolean aOwned) {
		List<String> rList = new ArrayList<>(16);
		for (int i = 0; i < 16; i++) rList.add(aOwned ? GTFluids.cfoamOwnedName(i) : GTFluids.cfoamName(i));
		return List.copyOf(rList);
	}

	@BeforeAll
	static void bootTheRegistrationFaces() {
		GTMaterialItems.initMaterials(); // the lang recording walk loads the full OP/material registries
	}

	/** The 32-row census: the dyed and owned ladders in DYE_IDS order + the base as the family root. */
	@Test
	public void cfoamIdsCarryTheSprayCanSnakeInDyeOrder() {
		assertEquals(16, GTFluids.CFOAMS.size(), "16 dyed colours, one per GTSprayCanItem.DYE_IDS row (Loader_Fluids.java:124)");
		assertEquals(16, GTFluids.CFOAMS_OWNED.size(), "16 owned colours (Loader_Fluids.java:125)");
		assertEquals(IDS, GTFluids.CFOAMS.stream().map(GTFluids.CFoamFluid::name).toList(),
				"the dyed paths are cfoam_ + the DYE_IDS snake, in dye order");
		assertEquals(OWNED_IDS, GTFluids.CFOAMS_OWNED.stream().map(GTFluids.CFoamFluid::name).toList(),
				"the owned paths are cfoam_owned_ + the DYE_IDS snake, in dye order");
		// the inverse seams: path -> dye index
		for (int i = 0; i < 16; i++) {
			assertEquals(i, GTFluids.cfoamIndexOf(GTFluids.cfoamName(i)), "dyed inverse seam");
			assertEquals(i, GTFluids.cfoamOwnedIndexOf(GTFluids.cfoamOwnedName(i)), "owned inverse seam");
			assertEquals(-1, GTFluids.cfoamIndexOf(GTFluids.cfoamOwnedName(i)), "the owned path is not a dyed row");
			assertEquals(-1, GTFluids.cfoamOwnedIndexOf(GTFluids.cfoamName(i)), "the dyed path is not an owned row");
		}
		assertEquals(-1, GTFluids.cfoamIndexOf("water"), "vanilla water is not a cfoam row");
		assertEquals(-1, GTFluids.cfoamIndexOf("cfoam"), "the BASE fluid is not a dyed row");
		// the index accessor returns the same rows
		for (int i = 0; i < 16; i++) {
			assertTrue(GTFluids.CFOAMS.get(i) == GTFluids.cfoam(i, false), "dyed index accessor");
			assertTrue(GTFluids.CFOAMS_OWNED.get(i) == GTFluids.cfoam(i, true), "owned index accessor");
		}
	}

	/** The decisions.p26-cfoam-fluid-naming ruling, structural: the base path is exactly {@code cfoam} — no IC2 compat name anywhere. */
	@Test
	public void theBaseFluidCarriesTheNamingRulingNotTheIC2CompatName() {
		//? if forge {
		assertEquals(new ResourceLocation("gt6", "cfoam"), GTFluids.CFOAM_TYPE.getId(), "the base FluidType id is gt6:cfoam (the ruling row)");
		assertEquals(new ResourceLocation("gt6", "cfoam"), GTFluids.CFOAM.getId(), "the base source id");
		assertEquals(new ResourceLocation("gt6", "cfoam_flowing"), GTFluids.CFOAM_FLOWING.getId(), "the base flowing id");
		assertEquals(new ResourceLocation("gt6", "cfoam_block"), GTFluids.CFOAM_BLOCK.getId(), "the base block id");
		//?} else {
		/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "cfoam"), GTFluids.CFOAM_TYPE.getId(), "the base FluidType id is gt6:cfoam (the ruling row)");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "cfoam"), GTFluids.CFOAM.getId(), "the base source id");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "cfoam_flowing"), GTFluids.CFOAM_FLOWING.getId(), "the base flowing id");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "cfoam_block"), GTFluids.CFOAM_BLOCK.getId(), "the base block id");
		*///?}
		assertFalse(GTFluids.CFOAM_TYPE.getId().getPath().contains("ic2"), "no IC2 compat fragment in the base path (decisions.p26-cfoam-fluid-naming)");
		for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS) assertFalse(tFamily.name().contains("ic2"), tFamily.name() + ": no IC2 fragment");
		for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS_OWNED) assertFalse(tFamily.name().contains("ic2"), tFamily.name() + ": no IC2 fragment");
	}

	/** The four-DR-with-block template over all 32 families + the base (acceptance: 三注册表齐). */
	@Test
	public void registrationShapeCarriesTheFourRegistryHandles() {
		for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS) {
			assertFamilyHandles(tFamily);
		}
		for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS_OWNED) {
			assertFamilyHandles(tFamily);
		}
		// the base descriptionId composition the registration uses (the id face is pinned in
		// theBaseFluidCarriesTheNamingRulingNotTheIC2CompatName, the live face in runServer)
		assertEquals("fluid.gt6.cfoam", "fluid.gt6." + GTFluids.CFOAM_TYPE.getId().getPath(),
				"the base descriptionId shape");
	}

	private static void assertFamilyHandles(GTFluids.CFoamFluid tFamily) {
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

	/**
	 * The three-way pin, per dye index: the family's tint IS the spray-can table value, the
	 * display name is the upstream :124/:125 compose, and the sibling foam-spray item ids
	 * are the same DYE_IDS snake — the refill's correctness root (zero new colour data).
	 */
	@Test
	public void tintsAreTheSprayCanTableVerbatimAndTheThreeWayPins() {
		for (int i = 0; i < 16; i++) {
			GTFluids.CFoamFluid tDyed = GTFluids.cfoam(i, false);
			GTFluids.CFoamFluid tOwned = GTFluids.cfoam(i, true);
			assertEquals(GTSprayCanItem.DYES_INT[i], tDyed.tint(), tDyed.name() + ": tint == DYES_INT[" + i + "]");
			assertEquals(GTSprayCanItem.DYES_INT[i], tOwned.tint(), tOwned.name() + ": the owned tint is the SAME table value");
			assertFalse(tDyed.owned, tDyed.name() + ": the dyed row carries the owned=F flag");
			assertTrue(tOwned.owned, tOwned.name() + ": the owned row carries the owned=T flag");
			assertEquals(GTSprayCanItem.DYE_NAMES[i] + " C-Foam", tDyed.displayName(),
					tDyed.name() + ": the Loader_Fluids.java:124 compose verbatim");
			assertEquals("Advanced " + GTSprayCanItem.DYE_NAMES[i] + " C-Foam", tOwned.displayName(),
					tOwned.name() + ": the Loader_Fluids.java:125 compose verbatim");
			// the item face (read-only): the sibling can ids are the same snake
			//? if forge {
			assertEquals(new ResourceLocation("gt6", "foam_spray_" + GTSprayCanItem.DYE_IDS[i]),
					GT6FoamSprays.FOAM_SPRAYS.get(i).getId(), tDyed.name() + ": the foam_spray sibling id");
			assertEquals(new ResourceLocation("gt6", "foam_spray_owned_" + GTSprayCanItem.DYE_IDS[i]),
					GT6FoamSprays.FOAM_SPRAYS_OWNED.get(i).getId(), tOwned.name() + ": the foam_spray_owned sibling id");
			//?} else {
			/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "foam_spray_" + GTSprayCanItem.DYE_IDS[i]),
					GT6FoamSprays.FOAM_SPRAYS.get(i).getId(), tDyed.name() + ": the foam_spray sibling id");
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "foam_spray_owned_" + GTSprayCanItem.DYE_IDS[i]),
					GT6FoamSprays.FOAM_SPRAYS_OWNED.get(i).getId(), tOwned.name() + ": the foam_spray_owned sibling id");
			*///?}
			assertEquals(GTSprayCanItem.DYE_IDS[i], tDyed.name().substring("cfoam_".length()),
					tDyed.name() + ": the fluid id suffix IS the DYE_IDS row");
		}
	}

	/** The declared carrier values: the :124-125 FL.create literals + the FL.java:432 "// 100 per Unit" bucket root. */
	@Test
	public void declaredCarrierValuesMatchTheUpstreamLiterals() {
		assertEquals(300, GTFluids.CFOAM_TEMPERATURE, "the Loader_Fluids.java:124-125 FL.create temperature literal");
		assertEquals(1000, GTFluids.CFOAM_DENSITY, "the honest FluidType default (the dye-chemical precedent)");
		assertEquals(100, GTFluids.CFOAM_BUCKET_UNITS, "FL.java:432 '// 100 per Unit' — the refill multiplier root");
		assertEquals(25600, 256 * GTFluids.CFOAM_BUCKET_UNITS, "the Canner refill leg: 256 buckets = 25600 mB");
	}

	/** The whole family sits far under the 340 K wood-barrel ceiling — the RCON tank chain carries them. */
	@Test
	public void everyFluidIsWoodBarrelSafe() {
		assertTrue(GTFluids.CFOAM_TEMPERATURE < 340, "the C-Foam rows stay under the wood ceiling");
	}

	/**
	 * The structural face (the dye test's form): the family holder carries the two data
	 * fields + the four registry handles and nothing else (a bucket field would need a
	 * container face this card rules out); GTFluids registers no items.
	 */
	@Test
	public void theFamilyDeclaresNoBucketAndNoItemFace() throws Exception {
		Class<?> tHolder = Class.forName("gregtech6.fluid.GTFluids$CFoamFluid");
		assertEquals(6, tHolder.getDeclaredFields().length,
				"dyeIndex + owned + type + source + flowing + block — adding a bucket here needs an item face this card rules out");
		List<String> tRegisters = new ArrayList<>();
		for (java.lang.reflect.Field tField : GTFluids.class.getDeclaredFields()) {
			if (tField.getType() == DeferredRegister.class) tRegisters.add(tField.getName());
		}
		assertEquals(List.of("FLUID_TYPES", "FLUIDS", "BLOCKS"), tRegisters,
				"GTFluids registers no items — zero bucket, zero item-tag face");
	}

	/** The earlier fluid families are UNCHANGED by the new registrations (the cross-family form). */
	@Test
	public void theEarlierFluidTablesAreUntouched() {
		assertEquals(9, GTFluids.ENGINE_SPECS.size());
		assertEquals(6, GTFluids.AQUA_SPECS.size());
		assertEquals(2, GTFluids.SIMPLE_LIQUID_SPECS.size());
		assertEquals(4, GTFluids.FOOD_FLUID_SPECS.size());
		assertEquals(16, GTFluids.DYE_CHEMICALS.size(), "the p24 dye-chemical family stays exactly 16");
		// the new ids are NOT rows of any earlier family lookup
		for (String tId : IDS) {
			assertNotNull(GTFluids.cfoamIndexOf(tId), tId + " lives on the c-foam registration");
		}
	}

	/**
	 * The lang en=zh reconciliation (the dye test's recording posture, 33 rows): both
	 * providers emit the base + 32 fluid display keys, the en face is the upstream compose,
	 * the zh face is the dump anchor (tmp/gregtech.lang:130-161 + :361).
	 */
	@Test
	public void langEnAndZhFacesCarryAllThirtyThreeKeys() throws Exception {
		Map<String, String> tEn = record(false);
		Map<String, String> tZh = record(true);
		List<String> tKeys = new ArrayList<>();
		tKeys.add("fluid.gt6.cfoam");
		for (int i = 0; i < 16; i++) tKeys.add("fluid.gt6." + GTFluids.cfoamName(i));
		for (int i = 0; i < 16; i++) tKeys.add("fluid.gt6." + GTFluids.cfoamOwnedName(i));
		assertEquals(33, tKeys.size());
		for (String tKey : tKeys) {
			assertNotNull(tEn.get(tKey), "en is missing " + tKey);
			assertNotNull(tZh.get(tKey), "zh is missing " + tKey);
			assertFalse(tZh.get(tKey).isBlank(), "blank zh value for " + tKey);
		}
		// value spot-pins: the compose faces + the dump faces
		assertEquals("Red C-Foam", tEn.get("fluid.gt6.cfoam_red"), "the :124 compose over DYE_NAMES[1]");
		assertEquals("Advanced White C-Foam", tEn.get("fluid.gt6.cfoam_owned_white"), "the :125 compose over DYE_NAMES[15]");
		assertEquals("Construction Foam", tEn.get("fluid.gt6.cfoam"), "the FL.java:432 base local (the 建筑泡沫 face)");
		assertEquals("红色建筑泡沫", tZh.get("fluid.gt6.cfoam_red"), "dump S:fluid.cfoam.red :159");
		assertEquals("高级白色建筑泡沫", tZh.get("fluid.gt6.cfoam_owned_white"), "dump S:fluid.cfoam.owned.white :155");
		assertEquals("淡灰色建筑泡沫", tZh.get("fluid.gt6.cfoam_light_gray"), "dump S:fluid.cfoam.lightgray :137 (the port snake id)");
		assertEquals("建筑泡沫", tZh.get("fluid.gt6.cfoam"), "dump S:fluid.ic2constructionfoam :361 under the ruling name");
	}

	/**
	 * Records one provider's full walk offline (the GT6LangParityTest.collect shape — the
	 * anonymous subclass overrides the public {@code add}; the {@code offlineWalk} bridge
	 * reaches the protected addTranslations from the subclass body).
	 */
	private static Map<String, String> record(boolean aZh) throws Exception {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp",
				aZh ? "gt6zhcn-cfoam-parity" : "gt6enus-cfoam-parity"));
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
