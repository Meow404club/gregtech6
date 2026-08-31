package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Coke Oven recipe pour (tasks p6-cokeoven-processing + p7-cokeoven-backfill):
 * <ul>
 * <li>the transcription walk: every (prefix, material) pair of every row resolves inside
 *     the offline material universe ({@link GTMaterialItems#registrationOrder}) — the
 *     "self-consistency, no hand-counted total" ruling;</li>
 * <li>the positive controls: gem Coal → gem CoalCoke + 500 mB creosote (U2) at 3600 t
 *     (p6), and dust Oilshale → dustTiny Asphalt + 250 mB oil (U4) at 3600 t (p7,
 *     Loader_Recipes_Other.java:807), plus the full end-to-end pour + lookup through
 *     injected resolvers;</li>
 * <li>the pooled entries (block-family six, the :815 blockDust row, the dynamic log
 *     family) are NOT in the table and ARE declared in
 *     {@link GT6RecipesCokeOven#SKIPPED_UPSTREAM};</li>
 * <li>the gt6:creosote/gt6:oil fluid id assertions (the registry itself is live-verified
 *     by the RCON chain — offline cannot touch the Forge registries).</li>
 * </ul>
 */
class GT6RecipesCokeOvenTest extends GTRecipesOfflineTestBase {

	private static Function<GT6RecipesCokeOven.Output, Item> sDefaultOutputResolver;
	private static Function<GT6RecipesCokeOven.StaticRow, Item> sDefaultInputResolver;
	private static Function<String, Fluid> sDefaultFluidResolver;

	/**
	 * The synthetic offline universe: one distinct EXISTING item per (prefix, material) pair —
	 * new Items cannot be created offline (the intrusive vanilla item registry freezes at
	 * bootstrap), so the driver maps pairs onto distinct vanilla registry entries; the recipe
	 * mechanics only compare identities.
	 */
	private static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new java.util.HashMap<>();

	@BeforeAll
	static void buildSyntheticUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		java.util.List<Item> tPool = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			SYNTHETIC_ITEMS.put(tPair, tPool.get(tNext++ % tPool.size()));
		}
		sDefaultOutputResolver = GT6RecipesCokeOven.sOutputItemResolver;
		sDefaultInputResolver = GT6RecipesCokeOven.sInputItemResolver;
		sDefaultFluidResolver = GT6RecipesCokeOven.sFluidResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesCokeOven.sOutputItemResolver = sDefaultOutputResolver;
		GT6RecipesCokeOven.sInputItemResolver = sDefaultInputResolver;
		GT6RecipesCokeOven.sFluidResolver = sDefaultFluidResolver;
		GT6RecipeMaps.reset();
		GT6RecipesCokeOven.resetForTest();
	}

	/** Every row's input and output pairs must live in the registered universe (self-consistency). */
	@Test
	void walkAssertsEveryRowResolvesInTheUniverse() {
		Set<PrefixMaterial> tUniverse = new HashSet<>(GTMaterialItems.registrationOrder());
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			assertTrue(tUniverse.contains(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial())),
					"row " + tRow.note() + ": input " + tRow.inPrefix().mNameInternal + "/" + tRow.inMaterial().mNameInternal + " must be a registered pair");
			for (GT6RecipesCokeOven.Output tOutput : tRow.outputs()) {
				assertTrue(tUniverse.contains(new PrefixMaterial(tOutput.prefix(), tOutput.material())),
						"row " + tRow.note() + ": output " + tOutput.prefix().mNameInternal + "/" + tOutput.material().mNameInternal + " must be a registered pair");
			}
		}
	}

	/** The gem Coal positive control (Loader_Recipes_Other.java:775). */
	@Test
	void positiveControlGemCoalRow() {
		GT6RecipesCokeOven.StaticRow tRow = findRow(OP.gem, MT.Coal);
		assertNotNull(tRow, "the gem Coal row must be transcribed");
		assertEquals(1, tRow.inCount());
		assertEquals(3600, tRow.duration());
		assertEquals(GT6RecipesCokeOven.FLUID_CREOSOTE, tRow.fluid());
		assertEquals(500, tRow.fluidMB(), "U2 creosote = 500 mB (OreDictMaterial.liquid units(U2, U, 1000))");
		assertEquals(1, tRow.outputs().length);
		assertEquals(OP.gem, tRow.outputs()[0].prefix());
		assertSame(MT.CoalCoke, tRow.outputs()[0].material());
		assertEquals(1, tRow.outputs()[0].count());
	}

	/** The oil-shale positive controls (Loader_Recipes_Other.java:807-808, backfilled by p7). */
	@Test
	void oilPositiveControlRows() {
		GT6RecipesCokeOven.StaticRow tDust = findRow(OP.dust, MT.Oilshale);
		assertNotNull(tDust, "the dust Oilshale row must be transcribed (:807)");
		assertEquals(":807", tDust.note());
		assertEquals(1, tDust.inCount());
		assertEquals(3600, tDust.duration());
		assertEquals(GT6RecipesCokeOven.FLUID_OIL, tDust.fluid());
		assertEquals(250, tDust.fluidMB(), "U4 oil = 250 mB (OreDictMaterial.liquid units(U4, U, 1000))");
		assertEquals(1, tDust.outputs().length);
		assertEquals(OP.dustTiny, tDust.outputs()[0].prefix());
		assertSame(MT.Asphalt, tDust.outputs()[0].material());
		assertEquals(1, tDust.outputs()[0].count());

		GT6RecipesCokeOven.StaticRow tOreRaw = findRow(OP.oreRaw, MT.Oilshale);
		assertNotNull(tOreRaw, "the oreRaw Oilshale row must be transcribed (:808)");
		assertEquals(":808", tOreRaw.note());
		assertEquals(7200, tOreRaw.duration());
		assertEquals(500, tOreRaw.fluidMB(), "U2 oil = 500 mB");
		assertEquals(2, tOreRaw.outputs()[0].count(), "the oreRaw row yields two dustTiny Asphalt");
	}

	/** All eight oil-shale rows carry the U4/U2 oil amounts of the upstream table (:807-814). */
	@Test
	void oilShaleFamilyShapes() {
		assertEquals(8, GT6RecipesCokeOven.table().stream().filter(tRow -> tRow.inMaterial() == MT.Oilshale).count(),
				"eight of the nine upstream oil-shale rows (the :815 blockDust row stays pooled)");
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			if (tRow.inMaterial() != MT.Oilshale) continue;
			assertEquals(GT6RecipesCokeOven.FLUID_OIL, tRow.fluid());
			assertEquals(OP.oreRaw == tRow.inPrefix() ? 500 : 250, tRow.fluidMB(), "U2 for :808, U4 elsewhere");
			assertEquals(1, tRow.outputs().length);
			assertEquals(OP.dustTiny, tRow.outputs()[0].prefix());
			assertSame(MT.Asphalt, tRow.outputs()[0].material());
		}
	}

	/** The billet shape: 3 in → 2 out at 7200 t with U creosote (Loader_Recipes_Other.java:778). */
	@Test
	void billetRowShape() {
		GT6RecipesCokeOven.StaticRow tRow = findRow(OP.billet, MT.Coal);
		assertNotNull(tRow);
		assertEquals(3, tRow.inCount());
		assertEquals(2, tRow.outputs()[0].count());
		assertEquals(7200, tRow.duration());
		assertEquals(GT6RecipesCokeOven.FLUID_CREOSOTE, tRow.fluid());
		assertEquals(1000, tRow.fluidMB());
	}

	/** The chunk-family rows carry the 5/6 identical chunkGt outputs (:783-786). */
	@Test
	void chunkFamilyShapes() {
		assertEquals(5, findRow(OP.crushedPurified, MT.Coal).outputs().length);
		assertEquals(6, findRow(OP.crushedCentrifuged, MT.Coal).outputs().length);
		assertEquals(5, findRow(OP.crushedPurified, MT.Lignite).outputs().length);
		assertEquals(6, findRow(OP.crushedCentrifuged, MT.Lignite).outputs().length);
	}

	/** The pooled entries: no block prefix (incl. the :815 blockDust Oilshale row), and the skip list declares them. */
	@Test
	void pooledEntriesAreDeclaredNotTranscribed() {
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			assertNotSame(OP.blockRaw, tRow.inPrefix());
			assertNotSame(OP.blockIngot, tRow.inPrefix());
			assertNotSame(OP.blockGem, tRow.inPrefix());
			assertNotSame(OP.blockDust, tRow.inPrefix());
			for (GT6RecipesCokeOven.Output tOutput : tRow.outputs()) {
				assertNotSame(MT.Oilshale, tOutput.material());
			}
		}
		String tSkipped = String.join("\n", GT6RecipesCokeOven.SKIPPED_UPSTREAM);
		assertTrue(tSkipped.contains("blockRaw"));
		assertTrue(tSkipped.contains("blockDust"));
		assertTrue(tSkipped.contains("Oilshale"));
		assertTrue(tSkipped.contains("beam"));
		assertTrue(tSkipped.contains("#minecraft:logs"), "the tag listener replaces the log family");
		assertEquals(32, GT6RecipesCokeOven.table().size(), "12 Coal + 12 Lignite + 8 Oilshale transcribed rows");
	}

	/** The creosote and oil fluids are registered under their gt6 ids (the live registry is RCON-verified). */
	@Test
	void fluidIdsAreRegistered() {
		assertEquals("creosote", GTFluids.CREOSOTE.getId().getPath());
		assertEquals("gt6", GTFluids.CREOSOTE.getId().getNamespace());
		assertEquals("oil", GTFluids.OIL.getId().getPath());
		assertEquals("gt6", GTFluids.OIL.getId().getNamespace());
	}

	/** The end-to-end pour with injected resolvers: every row, positive control findable + consumable. */
	@Test
	void pourResolvesAndRegistersAllRows() {
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tOutput.prefix(), tOutput.material()));
		GT6RecipesCokeOven.sInputItemResolver = tRow -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()));
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER; // stand-in carrier; the real fluid ids are asserted above

		GT6RecipesCokeOven.load();
		assertEquals(GT6RecipesCokeOven.table().size(), GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "every row resolves in the synthetic universe — zero skips");

		// the positive control lookup: a gem Coal stack finds its row and consumes it
		GT6RecipesCokeOven.StaticRow tRow = findRow(OP.gem, MT.Coal);
		Item tGemCoal = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gem, MT.Coal));
		Item tGemCoalCoke = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gem, MT.CoalCoke));
		ItemStack[] tInputs = {new ItemStack(tGemCoal, 16)};

		// the two-stage contract: probe does not consume
		Recipe tFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound);
		assertTrue(tFound.isRecipeInputEqual(false, false, null, tInputs));
		assertEquals(16, tInputs[0].getCount(), "the probe must not consume");
		// the consume stage
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(15, tInputs[0].getCount(), "one pass consumes exactly the recipe input count");
		// the outputs shape
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(tGemCoalCoke, tOutputs[0].getItem());
		FluidStack[] tFluids = tFound.getFluidOutputs(1);
		assertEquals(1, tFluids.length);
		assertEquals(500, tFluids[0].getAmount());
	}

	/** A failed resolution drops the row (the upstream mat() → null silent-drop semantics). */
	@Test
	void unresolvableRowsSkip() {
		GT6RecipesCokeOven.sInputItemResolver = tRow -> null; // nothing resolves
		GT6RecipesCokeOven.load();
		assertEquals(0, GT6RecipeMaps.COKE_OVEN.mRecipeList.size());
	}

	/** An unregistered fluid drops exactly its rows (the upstream absent-fluid semantics, p6 pool evidence). */
	@Test
	void unknownFluidIdSkipsRows() {
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tOutput.prefix(), tOutput.material()));
		GT6RecipesCokeOven.sInputItemResolver = tRow -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()));
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> GT6RecipesCokeOven.FLUID_CREOSOTE.equals(tFluidId) ? Fluids.WATER : null;

		GT6RecipesCokeOven.load();
		assertEquals(24, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the 8 oil rows skip when gt6:oil does not resolve — the p6 pool shape");
	}

	/** The oil-shale end-to-end lookup: dust Oilshale → dustTiny Asphalt + 250 mB oil (:807). */
	@Test
	void oilRowEndToEnd() {
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tOutput.prefix(), tOutput.material()));
		GT6RecipesCokeOven.sInputItemResolver = tRow -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()));
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER;

		GT6RecipesCokeOven.load();
		Item tDustOilshale = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dust, MT.Oilshale));
		Item tDustTinyAsphalt = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dustTiny, MT.Asphalt));
		ItemStack[] tInputs = {new ItemStack(tDustOilshale, 16)};

		Recipe tFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the dust Oilshale row must be findable after the pour");
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(15, tInputs[0].getCount());
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(tDustTinyAsphalt, tOutputs[0].getItem());
		FluidStack[] tFluids = tFound.getFluidOutputs(1);
		assertEquals(1, tFluids.length);
		assertEquals(250, tFluids[0].getAmount(), "U4 oil = 250 mB");
	}

	/** load() is idempotent within a generation. */
	@Test
	void loadIsIdempotent() {
		GT6RecipesCokeOven.sInputItemResolver = tRow -> Items.COAL;
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> Items.COAL; // identity-shared, fine for the count assertion
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER;
		GT6RecipesCokeOven.load();
		int tFirst = GT6RecipeMaps.COKE_OVEN.mRecipeList.size();
		assertEquals(32, tFirst);
		GT6RecipesCokeOven.load();
		assertEquals(tFirst, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the second load must not stack");
	}

	private static GT6RecipesCokeOven.StaticRow findRow(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			if (tRow.inPrefix() == aPrefix && tRow.inMaterial() == aMaterial) return tRow;
		}
		return null;
	}
}
