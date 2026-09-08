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
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Coke Oven recipe pour (tasks p6-cokeoven-processing + p7-cokeoven-backfill +
 * p8-prefixblock-registry): 39 rows = 32 item-universe rows + the 7 backfilled block rows
 * (Loader_Recipes_Other.java:787-789/:803-805/:815 — GTMaterialBlocks pairs).
 * <ul>
 * <li>the transcription walk: every (prefix, material) pair of every row resolves inside
 *     the offline material universe (the union of {@link GTMaterialItems#registrationOrder}
 *     and {@link GTMaterialBlocks#registrationOrder}) — the "self-consistency, no
 *     hand-counted total" ruling;</li>
 * <li>the positive controls: gem Coal → gem CoalCoke + 500 mB creosote (U2) at 3600 t
 *     (p6), dust Oilshale → dustTiny Asphalt + 250 mB oil (U4) at 3600 t (p7,
 *     Loader_Recipes_Other.java:807), and the p8 block rows (32400 t, 9*U/9*U2/27*U2/27*U4
 *     creosote + 9*U4 oil per the file scale), plus the full end-to-end pour + lookup
 *     through injected resolvers;</li>
 * <li>the pooled entries are now only the dynamic log family (beam/bamboo/wood-pellet),
 *     declared in {@link GT6RecipesCokeOven#SKIPPED_UPSTREAM}; the block rows are IN the
 *     table since p8 (declared there as backfilled);</li>
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
		// VANILLA-NAMESPACE ITEMS ONLY — the wrap-around aliasing is sensitive to the pool
		// SIZE, so a probe-registering test class (FileSawTest p24 / HammerWrenchTest p25)
		// would silently re-alias the universe; the minecraft-namespace filter pins the
		// pool to the frozen vanilla item set (the ShCL stabilization comment).
		java.util.List<Item> tPool = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
				.filter(t -> "minecraft".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(t).getNamespace()))
				.filter(t -> new ItemStack(t, 1).getMaxStackSize() == 64) // 64-stack only: new ItemStack(item, N>max) silently clamps and breaks the parallel/count math (the ItemStack no-arg form is leg-agnostic; Item.getMaxStackSize takes a stack on 21.1)
				.toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			Item tItem;
			// AIR (or any item making an empty stack) must be skipped: new ItemStack(AIR, n) is an
			// empty stack, the Recipe ctor trims it, and the resulting empty-input row would match
			// EVERY lookup (the p7 ghost recipe). The RecipeMap.addRecipe double-empty guard
			// (p8-recipe-chances-orechain ②) is the structural backstop; keeping AIR out of the
			// pool keeps the poured-count assertions exact.
			do {tItem = tPool.get(tNext++ % tPool.size());} while (new ItemStack(tItem, 1).isEmpty());
			SYNTHETIC_ITEMS.put(tPair, tItem);
		}
		for (PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) { // the p8 block universe
			SYNTHETIC_ITEMS.putIfAbsent(tPair, tPool.get(tNext++ % tPool.size()));
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

	/** Every row's input and output pairs must live in the registered universe (item + block, self-consistency). */
	@Test
	void walkAssertsEveryRowResolvesInTheUniverse() {
		Set<PrefixMaterial> tUniverse = new HashSet<>(GTMaterialItems.registrationOrder());
		tUniverse.addAll(GTMaterialBlocks.registrationOrder()); // the p8 block universe
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

	/** All nine oil-shale rows carry the U4/U2 oil amounts of the upstream table (:807-815). */
	@Test
	void oilShaleFamilyShapes() {
		assertEquals(9, GT6RecipesCokeOven.table().stream().filter(tRow -> tRow.inMaterial() == MT.Oilshale).count(),
				"all nine upstream oil-shale rows (the :815 blockDust row backfilled by p8)");
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) {
			if (tRow.inMaterial() != MT.Oilshale) continue;
			assertEquals(GT6RecipesCokeOven.FLUID_OIL, tRow.fluid());
			if (OP.blockDust == tRow.inPrefix()) {
				assertEquals(":815", tRow.note());
				assertEquals(32400, tRow.duration());
				assertEquals(2250, tRow.fluidMB(), "9*U4 oil = 2250 mB");
				assertEquals(OP.dust, tRow.outputs()[0].prefix(), ":815 outputs full dust, not dustTiny");
			} else {
				assertEquals(OP.oreRaw == tRow.inPrefix() ? 500 : 250, tRow.fluidMB(), "U2 for :808, U4 elsewhere");
				assertEquals(OP.dustTiny, tRow.outputs()[0].prefix());
			}
			assertEquals(1, tRow.outputs().length);
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

	/**
	 * The p8 flip: the seven block rows ARE transcribed (39 = 32 + 7), and the skip list
	 * declares them backfilled instead of pooled; the remaining pools (beam, the log-family
	 * replacement) stay declared.
	 */
	@Test
	void blockRowsBackfilled39Total() {
		assertEquals(39, GT6RecipesCokeOven.table().size(), "39 = 32 item-universe rows + 7 backfilled block rows");
		Map<String, GT6RecipesCokeOven.StaticRow> tByNote = new java.util.HashMap<>();
		for (GT6RecipesCokeOven.StaticRow tRow : GT6RecipesCokeOven.table()) tByNote.put(tRow.note(), tRow);
		for (String tNote : List.of(":787", ":788", ":789", ":803", ":804", ":805", ":815")) {
			assertTrue(tByNote.containsKey(tNote), "block row " + tNote + " must be transcribed");
		}
		String tSkipped = String.join("\n", GT6RecipesCokeOven.SKIPPED_UPSTREAM);
		assertTrue(tSkipped.contains("BACKFILLED by p8-prefixblock-registry"), "the block rows are declared backfilled, not pooled");
		assertTrue(tSkipped.contains(":787-789/:803-805") && tSkipped.contains(":815"), "both former pool entries declare the backfill");
		assertTrue(tSkipped.contains("beam"));
		assertTrue(tSkipped.contains("#minecraft:logs"), "the tag listener replaces the log family");
	}

	/** The seven p8 block rows carry the upstream shapes: 32400 t, the file-scale mB amounts, the block outputs. */
	@Test
	void blockRowShapes() {
		GT6RecipesCokeOven.StaticRow t787 = findRow(OP.blockRaw, MT.Coal);
		assertNotNull(t787, ":787 blockRaw Coal");
		assertEquals(1, t787.inCount());
		assertEquals(32400, t787.duration());
		assertEquals(9000, t787.fluidMB(), "9*U creosote = 9000 mB");
		assertEquals(OP.blockIngot, t787.outputs()[0].prefix());
		assertSame(MT.CoalCoke, t787.outputs()[0].material());
		assertEquals(2, t787.outputs()[0].count());

		GT6RecipesCokeOven.StaticRow t788 = findRow(OP.blockIngot, MT.Coal);
		assertNotNull(t788, ":788 blockIngot Coal");
		assertEquals(32400, t788.duration());
		assertEquals(4500, t788.fluidMB(), "9*U2 creosote = 4500 mB");
		assertEquals(OP.blockIngot, t788.outputs()[0].prefix());
		assertEquals(1, t788.outputs()[0].count());

		GT6RecipesCokeOven.StaticRow t789 = findRow(OP.blockGem, MT.Coal);
		assertNotNull(t789, ":789 blockGem Coal");
		assertEquals(32400, t789.duration());
		assertEquals(4500, t789.fluidMB(), "9*U2 creosote = 4500 mB");
		assertEquals(OP.blockGem, t789.outputs()[0].prefix());
		assertSame(MT.CoalCoke, t789.outputs()[0].material());

		GT6RecipesCokeOven.StaticRow t803 = findRow(OP.blockRaw, MT.Lignite);
		assertNotNull(t803, ":803 blockRaw Lignite");
		assertEquals(32400, t803.duration());
		assertEquals(13500, t803.fluidMB(), "27*U2 creosote = 13500 mB");
		assertEquals(OP.blockIngot, t803.outputs()[0].prefix());
		assertSame(MT.LigniteCoke, t803.outputs()[0].material());
		assertEquals(2, t803.outputs()[0].count());

		GT6RecipesCokeOven.StaticRow t804 = findRow(OP.blockIngot, MT.Lignite);
		assertNotNull(t804, ":804 blockIngot Lignite");
		assertEquals(32400, t804.duration());
		assertEquals(6750, t804.fluidMB(), "27*U4 creosote = 6750 mB (the file scale: U4 = 250 mB per :791 = 3*U4 = 750 and :807 = U4 = 250)");
		assertEquals(OP.blockIngot, t804.outputs()[0].prefix());
		assertSame(MT.LigniteCoke, t804.outputs()[0].material());

		GT6RecipesCokeOven.StaticRow t805 = findRow(OP.blockGem, MT.Lignite);
		assertNotNull(t805, ":805 blockGem Lignite");
		assertEquals(32400, t805.duration());
		assertEquals(6750, t805.fluidMB(), "27*U4 creosote = 6750 mB");
		assertEquals(OP.blockGem, t805.outputs()[0].prefix());
		assertSame(MT.LigniteCoke, t805.outputs()[0].material());

		GT6RecipesCokeOven.StaticRow t815 = findRow(OP.blockDust, MT.Oilshale);
		assertNotNull(t815, ":815 blockDust Oilshale");
		assertEquals(32400, t815.duration());
		assertEquals(2250, t815.fluidMB(), "9*U4 oil = 2250 mB");
		assertEquals(OP.dust, t815.outputs()[0].prefix());
		assertSame(MT.Asphalt, t815.outputs()[0].material());
		assertEquals(1, t815.outputs()[0].count());
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
		assertEquals(30, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the 9 oil rows skip when gt6:oil does not resolve (39 - 9 = 30)");
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

	/** The p8 block-row end-to-end lookup: blockIngot Coal → blockIngot CoalCoke + 4500 mB creosote (:788). */
	@Test
	void blockRowEndToEnd() {
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tOutput.prefix(), tOutput.material()));
		GT6RecipesCokeOven.sInputItemResolver = tRow -> SYNTHETIC_ITEMS.get(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()));
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER;

		GT6RecipesCokeOven.load();
		Item tBlockIngotCoal = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.blockIngot, MT.Coal));
		Item tBlockIngotCoalCoke = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.blockIngot, MT.CoalCoke));
		ItemStack[] tInputs = {new ItemStack(tBlockIngotCoal, 4)};

		Recipe tFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 4, ItemStack.EMPTY, null, tInputs);
		assertNotNull(tFound, "the blockIngot Coal row must be findable after the pour");
		assertEquals(32400, tFound.mDuration);
		assertTrue(tFound.isRecipeInputEqual(true, false, null, tInputs));
		assertEquals(3, tInputs[0].getCount());
		ItemStack[] tOutputs = tFound.getOutputs(1);
		assertEquals(1, tOutputs.length);
		assertEquals(tBlockIngotCoalCoke, tOutputs[0].getItem());
		FluidStack[] tFluids = tFound.getFluidOutputs(1);
		assertEquals(1, tFluids.length);
		assertEquals(4500, tFluids[0].getAmount(), "9*U2 creosote = 4500 mB");
	}

	/** load() is idempotent within a generation. */
	@Test
	void loadIsIdempotent() {
		GT6RecipesCokeOven.sInputItemResolver = tRow -> Items.COAL;
		GT6RecipesCokeOven.sOutputItemResolver = tOutput -> Items.COAL; // identity-shared, fine for the count assertion
		GT6RecipesCokeOven.sFluidResolver = tFluidId -> Fluids.WATER;
		GT6RecipesCokeOven.load();
		int tFirst = GT6RecipeMaps.COKE_OVEN.mRecipeList.size();
		assertEquals(39, tFirst);
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
