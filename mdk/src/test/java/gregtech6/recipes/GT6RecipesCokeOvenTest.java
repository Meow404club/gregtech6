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
 * The Coke Oven recipe pour (task p6-cokeoven-processing, acceptance ①):
 * <ul>
 * <li>the transcription walk: every (prefix, material) pair of every row resolves inside
 *     the offline material universe ({@link GTMaterialItems#registrationOrder}) — the
 *     "self-consistency, no hand-counted total" ruling;</li>
 * <li>the positive control: gem Coal → gem CoalCoke + 500 mB creosote (U2) at 3600 t, and
 *     the full end-to-end pour + lookup through injected resolvers;</li>
 * <li>the pooled entries (block-family six, the Oilshale nine, the dynamic log family) are
 *     NOT in the table and ARE declared in {@link GT6RecipesCokeOven#SKIPPED_UPSTREAM};</li>
 * <li>the gt6:creosote fluid id assertion (the registry itself is live-verified by the
 *     RCON chain — offline cannot touch the Forge registries).</li>
 * </ul>
 */
class GT6RecipesCokeOvenTest extends GTRecipesOfflineTestBase {

	private static Function<GT6RecipesCokeOven.Output, Item> sDefaultOutputResolver;
	private static Function<GT6RecipesCokeOven.StaticRow, Item> sDefaultInputResolver;
	private static java.util.function.Supplier<Fluid> sDefaultCreosoteResolver;

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
		sDefaultCreosoteResolver = GT6RecipesCokeOven.sCreosoteResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesCokeOven.sOutputItemResolver = sDefaultOutputResolver;
		GT6RecipesCokeOven.sInputItemResolver = sDefaultInputResolver;
		GT6RecipesCokeOven.sCreosoteResolver = sDefaultCreosoteResolver;
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
		assertEquals(500, tRow.creosote(), "U2 creosote = 500 mB (OreDictMaterial.liquid units(U2, U, 1000))");
		assertEquals(1, tRow.outputs().length);
		assertEquals(OP.gem, tRow.outputs()[0].prefix());
		assertSame(MT.CoalCoke, tRow.outputs()[0].material());
		assertEquals(1, tRow.outputs()[0].count());
	}

	/** The billet shape: 3 in → 2 out at 7200 t with U creosote (Loader_Recipes_Other.java:778). */
	@Test
	void billetRowShape() {
		GT6RecipesCokeOven.StaticRow tRow = findRow(OP.billet, MT.Coal);
		assertNotNull(tRow);
		assertEquals(3, tRow.inCount());
		assertEquals(2, tRow.outputs()[0].count());
		assertEquals(7200, tRow.duration());
		assertEquals(1000, tRow.creosote());
	}

	/** The chunk-family rows carry the 5/6 identical chunkGt outputs (:783-786). */
	@Test
	void chunkFamilyShapes() {
		assertEquals(5, findRow(OP.crushedPurified, MT.Coal).outputs().length);
		assertEquals(6, findRow(OP.crushedCentrifuged, MT.Coal).outputs().length);
		assertEquals(5, findRow(OP.crushedPurified, MT.Lignite).outputs().length);
		assertEquals(6, findRow(OP.crushedCentrifuged, MT.Lignite).outputs().length);
	}

	/** The pooled entries: no block prefix, no Oilshale, and the skip list declares them. */
	@Test
	void pooledEntriesAreDeclaredNotTranscribed() {
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			assertNotSame(OP.blockRaw, tRow.inPrefix());
			assertNotSame(OP.blockIngot, tRow.inPrefix());
			assertNotSame(OP.blockGem, tRow.inPrefix());
			assertNotSame(MT.Oilshale, tRow.inMaterial());
			for (GT6RecipesCokeOven.Output tOutput : tRow.outputs()) {
				assertNotSame(MT.Oilshale, tOutput.material());
			}
		}
		String tSkipped = String.join("\n", GT6RecipesCokeOven.SKIPPED_UPSTREAM);
		assertTrue(tSkipped.contains("blockRaw"));
		assertTrue(tSkipped.contains("Oilshale"));
		assertTrue(tSkipped.contains("beam"));
		assertTrue(tSkipped.contains("#minecraft:logs"), "the tag listener replaces the log family");
		assertEquals(24, GT6RecipesCokeOven.table().size(), "12 Coal + 12 Lignite transcribed rows");
	}

	/** The creosote fluid is registered under the gt6:creosote id (the live registry is RCON-verified). */
	@Test
	void creosoteFluidIdIsRegistered() {
		assertEquals("creosote", GTFluids.CREOSOTE.getId().getPath());
		assertEquals("gt6", GTFluids.CREOSOTE.getId().getNamespace());
	}

	/** The end-to-end pour with injected resolvers: 24 recipes, positive control findable + consumable. */
	@Test
	void pourResolvesAndRegistersAllRows() {
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tOutput.prefix(), tOutput.material()));
		GT6RecipesCokeOven.sInputItemResolver = tRow -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()));
		GT6RecipesCokeOven.sCreosoteResolver = () -> Fluids.WATER; // stand-in carrier; the real fluid id is asserted above

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

	/** load() is idempotent within a generation. */
	@Test
	void loadIsIdempotent() {
		GT6RecipesCokeOven.sInputItemResolver = tRow -> Items.COAL;
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> Items.COAL; // identity-shared, fine for the count assertion
		GT6RecipesCokeOven.sCreosoteResolver = () -> Fluids.WATER;
		GT6RecipesCokeOven.load();
		int tFirst = GT6RecipeMaps.COKE_OVEN.mRecipeList.size();
		assertEquals(24, tFirst);
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
