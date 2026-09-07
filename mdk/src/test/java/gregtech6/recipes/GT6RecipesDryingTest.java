package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The DRYING water-family pour offline tests (task p14-loop-closure-chain acceptance,
 * extended by task p16-drying-rows-backfill): the row transcription of
 * Loader_Recipes_Chem.java:525-532, the seven-row pour (the Water 10 L → DistW 8 L
 * foundation plus the six p16-aqua-fluids rows, EUt 16, duration 16, the buffered
 * addRecipe0 shape), the :530 water_hot absent-fluid pool reconciliation, the
 * machine-shape findRecipe lookup, and the isRecipeInputEqual consume semantics. The
 * live loop itself is the RCON chain (offline cannot touch the Forge registries).
 */
class GT6RecipesDryingTest extends GTRecipesOfflineTestBase {

	private static Function<String, Fluid> sDefaultFluidResolver;

	/**
	 * The offline fixture: every REGISTERED fluid id of the water and salt families —
	 * water, distw, the six p16-aqua-fluids ids and the two p19 simple-liquid ids —
	 * resolves to the vanilla water fluid (the recipe mechanics only compare identities —
	 * the GTEngineFuelsTest WATER_FIXTURE convention); the deliberately unregistered
	 * water_hot alias (:530) AND the four p21 food ids stay null, mirroring the
	 * absent-fluid verdict and keeping the per-family censuses exact (the food family
	 * pours under its own FOOD_ONLY_FIXTURE below).
	 */
	private static final Function<String, Fluid> CENSUS_FIXTURE = aId ->
		(GT6RecipesDrying.FLUID_WATER.equals(aId) || GT6RecipesDrying.FLUID_DISTW.equals(aId)
				|| GT6RecipesDrying.FLUID_SPDEW.equals(aId) || GT6RecipesDrying.FLUID_MNWTR.equals(aId)
				|| GT6RecipesDrying.FLUID_GEOTHERMAL.equals(aId) || GT6RecipesDrying.FLUID_BOILING.equals(aId)
				|| GT6RecipesDrying.FLUID_HOT_WATER.equals(aId) || GT6RecipesDrying.FLUID_COLD.equals(aId)
				|| GT6RecipesDrying.FLUID_SEAWATER.equals(aId) || GT6RecipesDrying.FLUID_WATERDIRTY.equals(aId))
				? Fluids.WATER : null;

	/**
	 * The water-family-only fixture: ONLY the water row's input and the DistW output
	 * resolve (plus the six aqua ids, the p16 pour half) — the salt ids stay null so the
	 * water-family lookup tests keep their exact census (the findRecipe probe skips the
	 * stack-size check, aDontCheckStackSizes=true, so a fixture-collapsed 8000 L waterdirty
	 * row WOULD answer a 1000 L water probe and blur the water-family assertion otherwise).
	 */
	private static final Function<String, Fluid> WATER_ONLY_FIXTURE = aId ->
			(GT6RecipesDrying.FLUID_WATER.equals(aId) || GT6RecipesDrying.FLUID_DISTW.equals(aId)
					|| GT6RecipesDrying.FLUID_SPDEW.equals(aId) || GT6RecipesDrying.FLUID_MNWTR.equals(aId)
					|| GT6RecipesDrying.FLUID_GEOTHERMAL.equals(aId) || GT6RecipesDrying.FLUID_BOILING.equals(aId)
					|| GT6RecipesDrying.FLUID_HOT_WATER.equals(aId) || GT6RecipesDrying.FLUID_COLD.equals(aId))
					? Fluids.WATER : null;

	@BeforeAll
	static void captureDefaults() {
		sDefaultFluidResolver = GT6RecipesDrying.sFluidResolver;
		sDefaultMaterialResolver = GT6RecipesDrying.sMaterialItemResolver;
		sDefaultVanillaResolver = GT6RecipesDrying.sVanillaItemResolver;
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesDrying.sFluidResolver = sDefaultFluidResolver;
		GT6RecipesDrying.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesDrying.sVanillaItemResolver = sDefaultVanillaResolver;
		GT6RecipeMaps.reset();
		GT6RecipesDrying.resetForTest();
	}

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;
	private static Function<Supplier<Item>, Item> sDefaultVanillaResolver;

	/**
	 * The synthetic offline universe for the ice family (the GT6RecipesShCLTest convention):
	 * one distinct EXISTING item per (prefix, material) pair of the ice table — new Items
	 * cannot be created offline (the intrusive vanilla item registry freezes at bootstrap),
	 * so the driver maps pairs onto distinct vanilla registry entries; the recipe mechanics
	 * only compare identities.
	 */
	private static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new HashMap<>();

	/**
	 * The vanilla items the ice table's vanilla rows use as inputs — kept out of the
	 * synthetic pool so a lookup with one of them can only ever match its own row (a
	 * synthetic (prefix, material) pair must never alias onto a vanilla row input).
	 * The p19 additions: the :73 clay input (plus the TERRACOTTA output and the :553
	 * dirt output — outputs too, so a synthetic input can never collide with another
	 * row's output identity either).
	 */
	private static final Set<Item> RESERVED_VANILLA_ITEMS = Set.of(
			Items.SNOWBALL, Blocks.ICE.asItem(), Blocks.PACKED_ICE.asItem(), Blocks.SNOW_BLOCK.asItem(),
			Blocks.CLAY.asItem(), Blocks.TERRACOTTA.asItem(), Blocks.DIRT.asItem());

	/** The transcription walk: eight rows, values per Loader_Recipes_Chem.java:525-532. */
	@Test
	void tableTranscribesTheUpstreamWaterFamily() {
		assertEquals(8, GT6RecipesDrying.table().size());
		assertRow(":525", GT6RecipesDrying.FLUID_WATER     , 10,  8);
		assertRow(":526", GT6RecipesDrying.FLUID_SPDEW     , 10,  8);
		assertRow(":527", GT6RecipesDrying.FLUID_MNWTR     , 10,  8);
		assertRow(":528", GT6RecipesDrying.FLUID_GEOTHERMAL, 25, 20);
		assertRow(":529", GT6RecipesDrying.FLUID_BOILING   , 25, 20);
		assertRow(":530", GT6RecipesDrying.FLUID_HOT       , 25, 20);
		assertRow(":531", GT6RecipesDrying.FLUID_HOT_WATER , 25, 20);
		assertRow(":532", GT6RecipesDrying.FLUID_COLD      , 25, 20);
	}

	private void assertRow(String aNote, String aFluid, long aIn, long aOut) {
		GT6RecipesDrying.DryingRow tRow = GT6RecipesDrying.table().stream()
				.filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		assertEquals(aFluid, tRow.input(), "row " + aNote + ": the input fluid id");
		assertEquals(aIn, tRow.inAmount(), "row " + aNote + ": the verbatim input litres");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim distilled output litres");
	}

	/**
	 * The end-to-end pour: EXACTLY the seven registered water rows land (:525-529/:531-532),
	 * the :530 water_hot alias skips.
	 */
	@Test
	void loadPoursExactlyTheSevenWaterRows() {
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.sMaterialItemResolver = (aPrefix, aMaterial) -> null; // isolate the water family: every item row skips
		GT6RecipesDrying.sVanillaItemResolver = s -> null;
		GT6RecipesDrying.load();
		assertEquals(7, GT6RecipeMaps.DRYING.mRecipeList.size(), "the seven registered water rows resolve; :530 pools");

		// the :525 Water row, exactly as poured since P14: 10 L in, 8 L out, EUt 16, duration 16
		Recipe tRecipe = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 10
						&& r.mFluidInputs[0].getFluid() == Fluids.WATER).findFirst().orElse(null);
		assertNotNull(tRecipe, "the :525 Water row is among the poured");
		assertSame(Fluids.WATER, tRecipe.mFluidInputs[0].getFluid(), "the vanilla water input");

		assertEquals(0, tRecipe.mInputs.length, "fluid-only: no item inputs (upstream ZL_IS)");
		assertEquals(0, tRecipe.mOutputs.length, "fluid-only: no item outputs");
		assertEquals(1, tRecipe.mFluidInputs.length);
		assertEquals(8, tRecipe.mFluidOutputs[0].getAmount(), "the :525 FL.DistW.make(8)");
		assertEquals(16, tRecipe.mDuration, "the verbatim :525 duration");
		assertEquals(16, tRecipe.mEUt, "the verbatim :525 EUt");
		assertTrue(tRecipe.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");
		assertEquals(256, tRecipe.getAbsoluteTotalPower(), "|EUt x duration| (Recipe.java:723-725 semantics)");

		// the :528 thermal row keeps its verbatim 25/20 split
		Recipe tGeo = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 25).findFirst().orElse(null);
		assertNotNull(tGeo, "a 25 L thermal row (:528/:529/:531) is among the poured");
		assertEquals(20, tGeo.mFluidOutputs[0].getAmount(), "the 25 → 20 thermal split");

		GT6RecipesDrying.load(); // idempotent: the second load is a no-op
		assertEquals(7, GT6RecipeMaps.DRYING.mRecipeList.size(), "load() is one pour per generation");
	}

	/**
	 * The machine-shape lookup: the Dryer's checkRecipe probes with the REAL input-tank
	 * snapshot and the length-1 slot array (TileEntityBasicMachine.java:512/:531 — the
	 * RecipeMap's empty-array hard return needs the length, the row needs no item), and
	 * the row answers for water but not for another fluid.
	 */
	@Test
	void machineShapeFindRecipeHitsTheWaterRow() {
		GT6RecipesDrying.sFluidResolver = WATER_ONLY_FIXTURE;
		GT6RecipesDrying.load();
		ItemStack[] tSlots = new ItemStack[1]; // the empty input slot, the live :512 shape

		// mRecipeList is a HashSet (RecipeMap.java:74) — with seven water-family rows the
		// linear scan's first match is hash-ordered, so the assertion is "a water-family
		// row answers the water tank", the amounts being exactly the registered {10, 25}
		Recipe tFound = GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots);
		assertNotNull(tFound, "the Dryer T1 voltage (64) covers the rows' EUt 16");
		assertSame(Fluids.WATER, tFound.mFluidInputs[0].getFluid(), "the answering row consumes water");
		assertTrue(tFound.mFluidInputs[0].getAmount() == 10 || tFound.mFluidInputs[0].getAmount() == 25,
				"the answering row is one of the registered water-family splits (10 or 25 L)");

		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1000)}, tSlots), "lava in the tank finds nothing");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 0, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots), "size 0 fails the absGreaterEqual voltage gate");
		assertNull(GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[0], tSlots), "an empty tank census finds nothing (Recipe.java:800 early false)");
	}

	/**
	 * The consume semantics (the :738/:744 two-stage contract): the probe leaves the tank
	 * snapshot untouched, the applied consume drains exactly the row's litre amount, and a
	 * short tank fails the amount check UNCHANGED (aDontCheckStackSizes=false on the apply
	 * path). Pinned on both the 10 L (:525) and the 25 L thermal split.
	 */
	@Test
	void isRecipeInputEqualConsumesExactlyTheRowLitres() {
		GT6RecipesDrying.sFluidResolver = WATER_ONLY_FIXTURE;
		GT6RecipesDrying.load();
		ItemStack[] tSlots = new ItemStack[1];
		Recipe tTen = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 10).findFirst().orElse(null);
		assertNotNull(tTen, "a 10 L row (:525/:526/:527) is among the poured");

		FluidStack[] tProbe = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTen.isRecipeInputEqual(false, true, tProbe, tSlots), "the findRecipe probe shape matches");
		assertEquals(1000, tProbe[0].getAmount(), "the probe never consumes");

		FluidStack[] tConsume = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTen.isRecipeInputEqual(true, false, tConsume, tSlots), "the applied consume succeeds");
		assertEquals(990, tConsume[0].getAmount(), "exactly the row's 10 L are drained");

		FluidStack[] tShort = {new FluidStack(Fluids.WATER, 9)};
		assertFalse(tTen.isRecipeInputEqual(true, false, tShort, tSlots), "9 L cannot feed a 10 L row");
		assertEquals(9, tShort[0].getAmount(), "a failing consume leaves the tank untouched");

		Recipe tTwentyFive = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length > 0 && r.mFluidInputs[0].getAmount() == 25).findFirst().orElse(null);
		assertNotNull(tTwentyFive, "a 25 L thermal row (:528/:529/:531) is among the poured");
		FluidStack[] tThermal = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tTwentyFive.isRecipeInputEqual(true, false, tThermal, tSlots), "the thermal consume succeeds");
		assertEquals(975, tThermal[0].getAmount(), "exactly the row's 25 L are drained");
	}

	/**
	 * The pool reconciliation after the p16 backfill: the :530 water_hot row is the ONLY
	 * water-family row the live resolver still answers null for (the IC2 alias
	 * "ic2hotwater", FL.java:116, deliberately unregistered by p16-aqua-fluids) — the
	 * upstream {@code if (FL.Water_Hot.exists())} guard shape, so load() skips it. The
	 * other six non-water ids resolve through live RegistryObjects, which only exist under
	 * a real registry event — their live resolution is the RCON chain's proof, offline the
	 * pour tests carry them through fixtures.
	 */
	@Test
	void theWaterHotRowAlonePools() {
		assertEquals(7, GT6RecipesDrying.table().stream()
				.filter(r -> !GT6RecipesDrying.FLUID_WATER.equals(r.input())).count(), "five 25/20 rows + two 10/8 rows beyond the Water row");
		GT6RecipesDrying.DryingRow tHot = GT6RecipesDrying.table().stream()
				.filter(r -> r.note().equals(":530")).findFirst().orElse(null);
		assertNotNull(tHot, "the :530 row is transcribed");
		assertEquals(GT6RecipesDrying.FLUID_HOT, tHot.input(), "the :530 input is the water_hot alias");
		assertNull(GT6RecipesDrying.resolveFluid(GT6RecipesDrying.FLUID_HOT),
				"the live census: water_hot has no port fluid — the :530 row pools");
		assertNull(GT6RecipesDrying.buildRecipe(new GT6RecipesDrying.DryingRow(":530", GT6RecipesDrying.FLUID_HOT, 25, 20)),
				"a pooled row builds nothing (the silent-skip shape)");
	}

	// ==================================================================
	// the ice/snow family (Loader_Recipes_Chem.java:510-522, task p16-drying-rows-backfill)
	// ==================================================================

	/** The transcription walk: thirteen rows, values per Loader_Recipes_Chem.java:510-522. */
	@Test
	void iceTableTranscribesTheUpstreamIceSnowFamily() {
		List<GT6RecipesDrying.IceRow> tTable = GT6RecipesDrying.iceTable();
		assertEquals(13, tTable.size(), "the upstream census: :510-522 holds thirteen rows (the task card said 12)");

		assertIceRow(tTable, ":510", OP.dustTiny  , MT.Ice ,  111,  444);
		assertIceRow(tTable, ":511", OP.dustSmall , MT.Ice ,  250, 1000);
		assertIceRow(tTable, ":512", OP.dust      , MT.Ice , 1000, 4000);
		assertIceRow(tTable, ":513", OP.gemChipped, MT.Ice ,  250, 1000);
		assertIceRow(tTable, ":514", OP.gemFlawed , MT.Ice ,  500, 2000);
		assertIceRow(tTable, ":515", OP.gem       , MT.Ice , 1000, 4000);
		assertIceRow(tTable, ":518", OP.dustTiny  , MT.Snow,  111,  444);
		assertIceRow(tTable, ":519", OP.dustSmall , MT.Snow,  250, 1000);
		assertIceRow(tTable, ":520", OP.dust      , MT.Snow, 1000, 4000);

		// the vanilla identities — the flattening map, :522 the FULL snow block (1.7.10
		// Blocks.snow), NOT 1.20.1 Blocks.SNOW (the layer block, Blocks.java:2147-2177)
		assertIceVanillaRow(tTable, ":516", Blocks.ICE.asItem()       , 1000, 4000);
		assertIceVanillaRow(tTable, ":517", Blocks.PACKED_ICE.asItem(), 2000, 8000);
		assertIceVanillaRow(tTable, ":521", Items.SNOWBALL            ,  250, 1000);
		assertIceVanillaRow(tTable, ":522", Blocks.SNOW_BLOCK.asItem(), 1000, 4000);
	}

	private void assertIceRow(List<GT6RecipesDrying.IceRow> aTable, String aNote,
			OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aOut, long aDuration) {
		GT6RecipesDrying.IceRow tRow = findIceRow(aTable, aNote);
		assertSame(aPrefix, tRow.prefix(), "row " + aNote + ": the input prefix");
		assertSame(aMaterial, tRow.material(), "row " + aNote + ": the input material");
		assertEquals(1, tRow.count(), "row " + aNote + ": single-item input (upstream OM.dust/prefix.mat x1)");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim DistW litres");
		assertEquals(aDuration, tRow.duration(), "row " + aNote + ": the verbatim 4x litres duration");
	}

	private void assertIceVanillaRow(List<GT6RecipesDrying.IceRow> aTable, String aNote, Item aItem, long aOut, long aDuration) {
		GT6RecipesDrying.IceRow tRow = findIceRow(aTable, aNote);
		assertNull(tRow.prefix(), "row " + aNote + ": a vanilla row");
		assertSame(aItem, tRow.vanilla().get(), "row " + aNote + ": the 1.20.1 vanilla identity");
		assertEquals(1, tRow.count(), "row " + aNote + ": single-item input (upstream ST.make x1)");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim DistW litres");
		assertEquals(aDuration, tRow.duration(), "row " + aNote + ": the verbatim 4x litres duration");
	}

	private GT6RecipesDrying.IceRow findIceRow(List<GT6RecipesDrying.IceRow> aTable, String aNote) {
		GT6RecipesDrying.IceRow tRow = aTable.stream().filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		return tRow;
	}

	/**
	 * The live-universe census: every material (prefix, material) pair of the ice table
	 * resolves inside the port item universe ({@link GTMaterialItems#registrationOrder},
	 * the prefix's isGeneratingItem criterion) — EXCEPT the :513/:514 gemChipped/gemFlawed
	 * Ice rows, and that is UPSTREAM-Faithful: the gemChipped/gemFlawed condition is
	 * And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT) (OP.java:1219-1220), Ice carries
	 * GEMS+DUSTS+TRANSPARENT via its G_GEM_TRANSPARENT set (TD.java:599) but NOT CRYSTAL
	 * (MT.java:1013 — no CRYSTAL tag), so the item never existed upstream either and the
	 * upstream {@code gemChipped.mat(MT.Ice, 1)} / {@code gemFlawed.mat(MT.Ice, 1)} calls
	 * were null → those two rows are dead text in Loader_Recipes_Chem.java:513-514. The
	 * port transcribes them (the census duty) and pours them to nothing, same skip.
	 */
	@Test
	void iceFamilyPairsResolveInThePortItemUniverse() {
		GTMaterialItems.initMaterials();
		Set<PrefixMaterial> tUniverse = Set.copyOf(GTMaterialItems.registrationOrder());
		Set<String> tUnresolvable = new java.util.HashSet<>();
		for (GT6RecipesDrying.IceRow tRow : GT6RecipesDrying.iceTable()) {
			if (tRow.prefix() == null) continue;
			if (!tUniverse.contains(new PrefixMaterial(tRow.prefix(), tRow.material()))) tUnresolvable.add(tRow.note());
		}
		assertEquals(Set.of(":513", ":514"), tUnresolvable,
				"exactly the gemChipped/gemFlawed Ice rows lack items (upstream-faithful mat() null drops)");
	}

	/**
	 * A deterministic (prefix, material) → distinct vanilla item resolver: the first call
	 * for a pair draws the next non-reserved, non-empty pool item and REMEMBERS it, so a
	 * later call with the same pair re-derives exactly the item a poured row carries.
	 * Pairs outside the port item universe answer null — the live
	 * {@link GTMaterialItems#get} census semantics the fixture must mirror.
	 */
	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> pairAssigningResolver() {
		GTMaterialItems.initMaterials();
		Set<PrefixMaterial> tUniverse = Set.copyOf(GTMaterialItems.registrationOrder());
		List<Item> tPool = BuiltInRegistries.ITEM.stream().toList();
		int[] tNext = {0};
		Map<OreDictPrefix, Map<OreDictMaterial, Item>> tAssigned = new HashMap<>();
		return (aPrefix, aMaterial) -> {
			if (!tUniverse.contains(new PrefixMaterial(aPrefix, aMaterial))) return null;
			Item tItem = tAssigned.computeIfAbsent(aPrefix, p -> new HashMap<>()).get(aMaterial);
			if (tItem != null) return tItem;
			do {tItem = tPool.get(tNext[0]++ % tPool.size());}
			while (RESERVED_VANILLA_ITEMS.contains(tItem) || new ItemStack(tItem, 1).isEmpty());
			tAssigned.get(aPrefix).put(aMaterial, tItem);
			return tItem;
		};
	}

	/**
	 * The end-to-end pour census: with the fixture resolvers, load() lands the 7 fluid-only
	 * water rows plus the eleven resolvable ice rows — the :513/:514 gemChipped/gemFlawed
	 * Ice rows skip (see {@link #iceFamilyPairsResolveInThePortItemUniverse()}).
	 */
	@Test
	void loadPoursTheWholeIceFamily() {
		GTMaterialItems.initMaterials();
		GT6RecipesDrying.sMaterialItemResolver = pairAssigningResolver();
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();

		List<Recipe> tIceRecipes = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1 && r.mFluidOutputs.length == 1 && r.mOutputs.length == 0).toList();
		assertEquals(11, tIceRecipes.size(), "7 water + 11 ice: the :513/:514 gem rows skip (no such items, upstream too)");
		assertEquals(35, GT6RecipeMaps.DRYING.mRecipeList.size(),
				"7 water + 11 ice + 2 salt (:548/:553) + 8 mineral (:559-566) + 6 clay loop (:567-568) + 1 BlockDiggable (:73) = the full poured census");
		for (Recipe tRecipe : tIceRecipes) {
			assertEquals(0, tRecipe.mFluidInputs.length, "an ice row has no fluid inputs (upstream NF)");
			assertEquals(0, tRecipe.mOutputs.length, "an ice row has no item outputs (upstream NI)");
			assertEquals(1, tRecipe.mFluidOutputs.length, "an ice row yields exactly the DistW stack");
			assertEquals(16, tRecipe.mEUt, "the family EUt");
		}
	}

	/**
	 * The machine-shape ice row lookups: every RESOLVABLE row's single input item in the
	 * slot finds ITS row — the found recipe carries the row's verbatim litres/duration/EUt
	 * — and the applied consume drains exactly that one item (probe leaves the slot
	 * untouched). Empty tanks probe fine: the rows carry no fluid inputs, and
	 * Recipe.isRecipeInputEqual only hard-fails empty fluid arrays for rows that HAVE
	 * fluid inputs.
	 */
	@Test
	void iceRowsFindAndConsumeTheirOwnInput() {
		GTMaterialItems.initMaterials();
		GT6RecipesDrying.sMaterialItemResolver = pairAssigningResolver();
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();

		for (GT6RecipesDrying.IceRow tRow : resolvableIceRows()) {
			Item tInput = tRow.vanilla() != null ? GT6RecipesDrying.sVanillaItemResolver.apply(tRow.vanilla())
					: GT6RecipesDrying.sMaterialItemResolver.apply(tRow.prefix(), tRow.material());
			ItemStack[] tSlots = {new ItemStack(tInput, 4)};

			Recipe tFound = GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY, new FluidStack[0], tSlots);
			assertNotNull(tFound, "row " + tRow.note() + ": its own input finds the row (voltage 64 covers EUt 16)");
			assertEquals(tRow.outAmount(), tFound.mFluidOutputs[0].getAmount(), "row " + tRow.note() + ": the verbatim litres");
			assertEquals(tRow.duration(), tFound.mDuration, "row " + tRow.note() + ": the verbatim duration");
			assertEquals(16, tFound.mEUt, "row " + tRow.note() + ": the family EUt");
			assertTrue(tFound.mCanBeBuffered, "row " + tRow.note() + ": the addRecipe1(T, ...) buffered shape");
			assertTrue(tFound.isRecipeInputEqual(false, true, new FluidStack[0], tSlots), "row " + tRow.note() + ": the probe matches");
			assertEquals(4, tSlots[0].getCount(), "row " + tRow.note() + ": the probe never consumes");
			assertTrue(tFound.isRecipeInputEqual(true, false, new FluidStack[0], tSlots), "row " + tRow.note() + ": the consume succeeds");
			assertEquals(3, tSlots[0].getCount(), "row " + tRow.note() + ": exactly one item drained");
		}
	}

		/** The ice rows that resolve inside the port item universe: the vanilla rows plus the material rows in registrationOrder. */
	private List<GT6RecipesDrying.IceRow> resolvableIceRows() {
		GTMaterialItems.initMaterials();
		Set<PrefixMaterial> tUniverse = Set.copyOf(GTMaterialItems.registrationOrder());
		return GT6RecipesDrying.iceTable().stream()
				.filter(r -> r.prefix() == null || tUniverse.contains(new PrefixMaterial(r.prefix(), r.material())))
				.toList();
	}

	// ==================================================================
	// the salt + mineral-dehydration + clay-loop + BlockDiggable families
	// (Loader_Recipes_Chem.java:544-568 / BlockDiggable.java:73, task p19-drying-rows-backfill-2)
	// ==================================================================

	/** The transcription walk: two rows, values per Loader_Recipes_Chem.java:548/:553 verbatim. */
	@Test
	void saltTableTranscribesTheUpstreamSaltRows() {
		List<GT6RecipesDrying.SaltRow> tTable = GT6RecipesDrying.saltTable();
		assertEquals(2, tTable.size(), "the upstream census: :544-557 holds exactly two UNGUARDED rows");

		GT6RecipesDrying.SaltRow tOcean = tTable.get(0);
		assertEquals(":548", tOcean.note());
		assertEquals(GT6RecipesDrying.FLUID_SEAWATER, tOcean.input(), "FL.Ocean = \"seawater\" (FL.java:125)");
		assertEquals(7000, tOcean.inAmount(), "the verbatim FL.Ocean.make(7000)");
		assertEquals(6750, tOcean.outAmount(), "the verbatim FL.DistW.make(6750)");
		assertEquals(11200, tOcean.duration(), "the verbatim 11200 ticks");
		assertSame(OP.dustSmall, tOcean.outPrefix(), "OM.dust(MT.NaCl, U4) → the dustSmall output (OM.java:460-467)");
		assertSame(MT.NaCl, tOcean.outMaterial());
		assertNull(tOcean.outVanilla(), "a material-output row");

		GT6RecipesDrying.SaltRow tDirty = tTable.get(1);
		assertEquals(":553", tDirty.note());
		assertEquals(GT6RecipesDrying.FLUID_WATERDIRTY, tDirty.input(), "FL.Dirty_Water = \"waterdirty\" (FL.java:127)");
		assertEquals(8000, tDirty.inAmount(), "the verbatim FL.Dirty_Water.make(8000)");
		assertEquals(7000, tDirty.outAmount(), "the verbatim FL.DistW.make(7000)");
		assertEquals(16000, tDirty.duration(), "the verbatim 16000 ticks");
		assertSame(Blocks.DIRT.asItem(), tDirty.outVanilla().get(), "the vanilla dirt output, ST.make(Blocks.dirt, 1, 0)");
		assertNull(tDirty.outPrefix(), "a vanilla-output row");
	}

	/** The transcription walk: eight mineral rows, values per Loader_Recipes_Chem.java:559-566 verbatim. */
	@Test
	void dehydrationTableTranscribesTheMineralRows() {
		List<GT6RecipesDrying.DehydrationRow> tTable = GT6RecipesDrying.dehydrationTable();

		assertDustRow(tTable, ":559", MT.OREMATS.Mirabilite ,  7, 30000, MT.Na2SO4  , 7, 60000);
		assertDustRow(tTable, ":560", MT.FeO3H3             , 14,  9000, MT.Fe2O3   , 5, 18000);
		assertDustRow(tTable, ":561", MT.AlO3H3             , 14,  9000, MT.Al2O3   , 5, 18000);
		assertDustRow(tTable, ":562", MT.H2WO4              ,  7,  3000, MT.WO3     , 4,  6000);
		assertDustRow(tTable, ":563", MT.OREMATS.Bischofite ,  1,  2000, MT.MgCl2   , 1,  4000);
		assertDustRow(tTable, ":564", MT.OREMATS.Trona      ,  1,  1000, MT.Na2CO3  , 1,  2000);
		assertDustRow(tTable, ":565", MT.Gypsum             ,  1,  1000, MT.CaSO4   , 1,  2000);
		assertDustRow(tTable, ":566", MT.OREMATS.Perlite    ,  1,  1000, MT.Obsidian, 1,  2000);
	}

	private void assertDustRow(List<GT6RecipesDrying.DehydrationRow> aTable, String aNote,
			OreDictMaterial aIn, int aInCount, long aOut, OreDictMaterial aOutMat, int aOutCount, long aDuration) {
		GT6RecipesDrying.DehydrationRow tRow = aTable.stream().filter(r -> r.note().equals(aNote)).findFirst().orElse(null);
		assertNotNull(tRow, "row " + aNote + " transcribed");
		assertSame(OP.dust, tRow.inPrefix(), "row " + aNote + ": the dust input prefix");
		assertSame(aIn, tRow.inMaterial(), "row " + aNote + ": the input material");
		assertNull(tRow.inVanilla(), "row " + aNote + ": a material row");
		assertEquals(aInCount, tRow.inCount(), "row " + aNote + ": the verbatim input stack");
		assertEquals(aOut, tRow.outAmount(), "row " + aNote + ": the verbatim DistW litres");
		assertSame(OP.dust, tRow.outPrefix(), "row " + aNote + ": the dust output prefix");
		assertSame(aOutMat, tRow.outMaterial(), "row " + aNote + ": the output material");
		assertEquals(aOutCount, tRow.outCount(), "row " + aNote + ": the verbatim output stack");
		assertEquals(aDuration, tRow.duration(), "row " + aNote + ": the verbatim duration");
	}

	/**
	 * The clay loop (:567-568) walks the LIVE ANY.Clay.mToThis family — the transcription
	 * of the upstream loop itself. The membership is upstream-identical (Clay plus the
	 * five clay() materials ClayBrown/ClayRed/Bentonite/Palygorskite/Kaolinite, each
	 * .put(ANY.Clay) wiring mToThis, OreDictMaterial.java:262) — the task card's "Clay 2
	 * rows" counts the two upstream source LINES, the expansion is material-count-driven.
	 */
	@Test
	void clayLoopExpandsTheUpstreamFamily() {
		GTMaterialItems.initMaterials();
		Set<OreDictMaterial> tExpected = Set.of(MT.Clay, MT.ClayBrown, MT.ClayRed, MT.Bentonite, MT.Palygorskite, MT.Kaolinite);
		assertEquals(tExpected, ANY.Clay.mToThis, "the port family membership is upstream-identical");
		List<GT6RecipesDrying.DehydrationRow> tClayRows = GT6RecipesDrying.dehydrationTable().stream()
				.filter(r -> r.note().equals(":567-568")).toList();
		assertEquals(tExpected.size(), tClayRows.size(), "one row per family material");
		Map<OreDictMaterial, GT6RecipesDrying.DehydrationRow> tByMat = new HashMap<>();
		for (GT6RecipesDrying.DehydrationRow tRow : tClayRows) {
			assertSame(OP.dust, tRow.inPrefix());
			tByMat.put(tRow.inMaterial(), tRow);
			assertEquals(1, tRow.inCount(), "the verbatim OP.dust.mat(tMat, 1)");
			assertEquals(500, tRow.outAmount(), "the verbatim FL.DistW.make(500)");
			assertSame(MT.Ceramic, tRow.outMaterial(), "the verbatim OP.dust.mat(MT.Ceramic, 1)");
			assertEquals(1, tRow.outCount());
			assertEquals(1000, tRow.duration(), "the verbatim 1000 ticks");
		}
		assertEquals(tExpected, tByMat.keySet(), "the rows cover exactly the family, no duplicates");
	}

	/** The BlockDiggable.java:73 row — the 1.20.1 vanilla identity CLAY → TERRACOTTA (1.7.10 hardened_clay). */
	@Test
	void blockDiggableRowCarriesTheVanillaIdentity() {
		GTMaterialItems.initMaterials(); // the lazy table walks ANY.Clay.mToThis — materials must exist first
		GT6RecipesDrying.DehydrationRow tRow = GT6RecipesDrying.dehydrationTable().stream()
				.filter(r -> r.note().equals(":73")).findFirst().orElse(null);
		assertNotNull(tRow, "the BlockDiggable.java:73 row is transcribed");
		assertSame(Blocks.CLAY.asItem(), tRow.inVanilla().get(), "1.20.1 Blocks.CLAY (the ST.make(Blocks.clay, 1, 0) input)");
		assertSame(Blocks.TERRACOTTA.asItem(), tRow.outVanilla().get(),
				"1.20.1 Blocks.TERRACOTTA — 1.7.10 \"hardened_clay\" is the 1.13 flattening \"terracotta\" (Blocks.java:3565)");
		assertEquals(1, tRow.inCount());
		assertEquals(1, tRow.outCount());
		assertEquals(0, tRow.outAmount(), "no fluid leg at all (upstream NF/NF)");
		assertEquals(64, tRow.duration(), "the verbatim 64 ticks");
	}

	/** The live-universe census: every (dust, material) leg of the dehydration table resolves in the port item universe. */
	@Test
	void dehydrationLegsResolveInThePortItemUniverse() {
		GTMaterialItems.initMaterials();
		Set<PrefixMaterial> tUniverse = Set.copyOf(GTMaterialItems.registrationOrder());
		Set<String> tUnresolvable = new java.util.HashSet<>();
		for (GT6RecipesDrying.DehydrationRow tRow : GT6RecipesDrying.dehydrationTable()) {
			if (tRow.inPrefix() != null && !tUniverse.contains(new PrefixMaterial(tRow.inPrefix(), tRow.inMaterial()))) tUnresolvable.add(tRow.note() + " in");
			if (tRow.outPrefix() != null && !tUniverse.contains(new PrefixMaterial(tRow.outPrefix(), tRow.outMaterial()))) tUnresolvable.add(tRow.note() + " out");
		}
		assertEquals(Set.of(), tUnresolvable, "every mineral/clay/output dust pair resolves (= upstream live mat() semantics)");
	}

	/**
	 * The backfill pour, end-to-end: with the fixture resolvers the salt rows pour their
	 * fluid-in/fluid-out/item-out shape, the :73 row pours its fluid-less shape, and the
	 * full map census is 7 water + 11 ice + 2 salt + 8 mineral + 6 clay + 1 = 35.
	 */
	@Test
	void loadPoursTheSaltAndDehydrationFamilies() {
		GTMaterialItems.initMaterials();
		GT6RecipesDrying.sMaterialItemResolver = pairAssigningResolver();
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
		GT6RecipesDrying.load();

		// the :548 seawater row shape: fluid 7000 in, DistW 6750 out, one NaCl dustSmall out
		Recipe tOcean = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 7000).findFirst().orElse(null);
		assertNotNull(tOcean, "the :548 row is among the poured");
		assertEquals(6750, tOcean.mFluidOutputs[0].getAmount(), "the verbatim 7000 → 6750 split");
		assertEquals(11200, tOcean.mDuration, "the verbatim duration");
		assertEquals(16, tOcean.mEUt, "the family EUt");
		assertEquals(1, tOcean.mOutputs.length, "one item output: the NaCl dustSmall");
		assertEquals(1, tOcean.mOutputs[0].getCount());
		assertTrue(tOcean.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");

		// the :553 waterdirty row shape: fluid 8000 in, DistW 7000 out, one vanilla dirt out
		Recipe tDirty = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 8000).findFirst().orElse(null);
		assertNotNull(tDirty, "the :553 row is among the poured");
		assertEquals(7000, tDirty.mFluidOutputs[0].getAmount(), "the verbatim 8000 → 7000 split");
		assertEquals(16000, tDirty.mDuration, "the verbatim duration");
		assertSame(Blocks.DIRT.asItem(), tDirty.mOutputs[0].getItem(), "the vanilla dirt output");

		// the :73 row shape: one clay item in, one terracotta out, NO fluid legs
		Recipe tClay = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1 && r.mOutputs.length == 1
						&& r.mFluidInputs.length == 0 && r.mFluidOutputs.length == 0
						&& r.mInputs[0].getItem() == Blocks.CLAY.asItem()).findFirst().orElse(null);
		assertNotNull(tClay, "the :73 row is among the poured");
		assertSame(Blocks.TERRACOTTA.asItem(), tClay.mOutputs[0].getItem(), "the 1.20.1 terracotta output");
		assertEquals(64, tClay.mDuration, "the verbatim duration");
		assertEquals(16, tClay.mEUt, "the family EUt");
	}

	// ==================================================================
	// the food family (Loader_Recipes_Food.java:654-658, task p21-drying-food-fluids)
	// ==================================================================

	/** The transcription walk: four rows, values per Loader_Recipes_Food.java:655-658 verbatim. */
	@Test
	void foodTableTranscribesTheUpstreamFoodRows() {
		List<GT6RecipesDrying.FoodRow> tTable = GT6RecipesDrying.foodTable();
		assertEquals(4, tTable.size(), "the upstream census: :654-658 holds exactly four rows");

		assertFoodRow(tTable.get(0), ":655", GT6RecipesDrying.FLUID_SAP        , 250, 100, 1, 200,
				"FL.Sap.make(250) — the :654 FL.Sap.exists() guard row");
		assertFoodRow(tTable.get(1), ":656", GT6RecipesDrying.FLUID_MAPLESAP   , 250, 100, 1, 200,
				"FL.Sap_Maple.make(250), unguarded upstream");
		assertFoodRow(tTable.get(2), ":657", GT6RecipesDrying.FLUID_REEDWATER  , 200,  50, 1, 100,
				"FL.Juice_Reed.make(200), unguarded upstream");
		assertFoodRow(tTable.get(3), ":658", GT6RecipesDrying.FLUID_CACTUSWATER, 200,  50, 0, 100,
				"FL.Juice_Cactus.make(200), ZL_IS: no item output");
	}

	private void assertFoodRow(GT6RecipesDrying.FoodRow aRow, String aNote, String aFluid,
			long aIn, long aDist, int aSugar, long aDuration, String aAnchor) {
		assertEquals(aNote, aRow.note(), "row order: " + aAnchor);
		assertEquals(aFluid, aRow.input(), "row " + aNote + ": the input fluid id");
		assertEquals(aIn, aRow.inAmount(), "row " + aNote + ": the verbatim input litres");
		assertEquals(aDist, aRow.distAmount(), "row " + aNote + ": the verbatim DistW litres");
		assertEquals(aSugar, aRow.sugarAmount(), "row " + aNote + ": the OM.dust(MT.Sugar) / ZL_IS leg");
		assertEquals(aDuration, aRow.duration(), "row " + aNote + ": the verbatim duration");
	}

	/**
	 * The food fixture: ONLY the four food ids and the DistW output resolve — the water
	 * family ids stay null so the earlier families keep their exact census (the
	 * WATER_ONLY_FIXTURE isolation convention).
	 */
	private static final Function<String, Fluid> FOOD_ONLY_FIXTURE = aId ->
			(GT6RecipesDrying.FLUID_DISTW.equals(aId) || GT6RecipesDrying.FLUID_SAP.equals(aId)
					|| GT6RecipesDrying.FLUID_MAPLESAP.equals(aId) || GT6RecipesDrying.FLUID_REEDWATER.equals(aId)
					|| GT6RecipesDrying.FLUID_CACTUSWATER.equals(aId)) ? Fluids.WATER : null;

	/**
	 * The end-to-end pour: EXACTLY the four food rows land, each in its verbatim shape —
	 * the sugar rows carry one dust output (the (OP.dust, MT.Sugar) seam resolving onto
	 * Items.SUGAR), the :658 cactus row is the ZL_IS fluid-only shape.
	 */
	@Test
	void loadPoursExactlyTheFourFoodRows() {
		GTMaterialItems.initMaterials(); // load() walks dehydrationTable -> ANY.Clay.mToThis — materials must exist first
		GT6RecipesDrying.sFluidResolver = FOOD_ONLY_FIXTURE;
		GT6RecipesDrying.sMaterialItemResolver = (aPrefix, aMaterial) ->
				aPrefix == OP.dust && aMaterial == MT.Sugar ? Items.SUGAR : null; // the sugar leg alone
		GT6RecipesDrying.sVanillaItemResolver = s -> null;
		GT6RecipesDrying.load();
		assertEquals(4, GT6RecipeMaps.DRYING.mRecipeList.size(), "the four food rows resolve; every other family stays pooled under the fixture");

		// the :655 Sap row: 250 in, DistW 100 out, one Sugar dust, dur 200, EUt 16
		Recipe tSap = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 250
						&& r.mOutputs.length == 1).findFirst().orElse(null);
		assertNotNull(tSap, "a 250 L sugar row (:655/:656) is among the poured");
		assertEquals(100, tSap.mFluidOutputs[0].getAmount(), "the verbatim 250 → 100 split");
		assertSame(Items.SUGAR, tSap.mOutputs[0].getItem(), "the sugar dust output (the (OP.dust, MT.Sugar) seam fixture)");
		assertEquals(1, tSap.mOutputs[0].getCount(), "OM.dust(MT.Sugar) = one full dust");
		assertEquals(200, tSap.mDuration, "the verbatim duration");
		assertEquals(16, tSap.mEUt, "the family EUt");
		assertTrue(tSap.mCanBeBuffered, "the addRecipe0(T, ...) buffered shape");
		assertEquals(0, tSap.mInputs.length, "fluid-in only: no item inputs");
		assertEquals(3200, tSap.getAbsoluteTotalPower(), "|16 x 200| (Recipe.java:723-725 semantics)");

		// the :658 cactus row: 200 in, DistW 50 out, NO item output (ZL_IS), dur 100
		Recipe tCactus = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 200
						&& r.mOutputs.length == 0).findFirst().orElse(null);
		assertNotNull(tCactus, "the :658 ZL_IS row is among the poured");
		assertEquals(50, tCactus.mFluidOutputs[0].getAmount(), "the verbatim 200 → 50 split");
		assertEquals(0, tCactus.mOutputs.length, "ZL_IS: no item output");
		assertEquals(100, tCactus.mDuration, "the verbatim duration");
		assertEquals(16, tCactus.mEUt, "the family EUt");

		// the sugar rows are two (:655/:656), the ZL_IS row one (:658), the third sugar row :657
		assertEquals(3, GT6RecipeMaps.DRYING.mRecipeList.stream().filter(r -> r.mOutputs.length == 1).count(),
				":655/:656/:657 carry the Sugar leg; :658 alone does not");

		GT6RecipesDrying.load(); // idempotent: the second load is a no-op
		assertEquals(4, GT6RecipeMaps.DRYING.mRecipeList.size(), "load() is one pour per generation");
	}

	/**
	 * The machine-shape lookup over a poured food row: the Dryer's real input-tank probe
	 * (the length-1 slot array) finds the 250 L rows, the found recipe carries the verbatim
	 * litres/duration, and the applied consume drains exactly the row's litres.
	 */
	@Test
	void foodRowsFindAndConsumeThroughTheMachineShape() {
		GTMaterialItems.initMaterials(); // load() walks dehydrationTable -> ANY.Clay.mToThis — materials must exist first
		GT6RecipesDrying.sFluidResolver = FOOD_ONLY_FIXTURE;
		GT6RecipesDrying.sMaterialItemResolver = (aPrefix, aMaterial) ->
				aPrefix == OP.dust && aMaterial == MT.Sugar ? Items.SUGAR : null;
		GT6RecipesDrying.load();
		ItemStack[] tSlots = new ItemStack[1]; // the empty input slot, the live :512 shape

		// mRecipeList is a HashSet — the assertion is "a 250 L food row answers the tank"
		// (the two 250 rows :655/:656), the amounts being exactly the registered {250, 200}
		Recipe tFound = GT6RecipeMaps.DRYING.findRecipe(null, 64, ItemStack.EMPTY,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tSlots);
		assertNotNull(tFound, "the Dryer T1 voltage (64) covers the rows' EUt 16");
		assertTrue(tFound.mFluidInputs[0].getAmount() == 250 || tFound.mFluidInputs[0].getAmount() == 200,
				"the answering row is one of the registered food splits (250 or 200 L)");

		// an explicit 250 L row: the probe leaves the tank untouched, the consume drains 250
		Recipe tQuarter = GT6RecipeMaps.DRYING.mRecipeList.stream()
				.filter(r -> r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 250).findFirst().orElse(null);
		assertNotNull(tQuarter);
		FluidStack[] tProbe = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tQuarter.isRecipeInputEqual(false, true, tProbe, tSlots), "the probe matches");
		assertEquals(1000, tProbe[0].getAmount(), "the probe never consumes");
		FluidStack[] tConsume = {new FluidStack(Fluids.WATER, 1000)};
		assertTrue(tQuarter.isRecipeInputEqual(true, false, tConsume, tSlots), "the applied consume succeeds");
		assertEquals(750, tConsume[0].getAmount(), "exactly the row's 250 L are drained");
	}

	/**
	 * The audit walk update (task p21-drying-food-fluids): the food family entry stands as
	 * the POURED annotation — the pool pointer retired, the anchor kept. (The live sap
	 * resolution itself is never probed offline — RegistryObject.get() needs the registry —
	 * it is the RCON chain's proof; here the pour tests carry the rows through fixtures.)
	 */
	@Test
	void theFoodFamilyEntryIsThePouredAnnotation() {
		GT6RecipesDrying.FoodRow tSap = GT6RecipesDrying.foodTable().stream()
				.filter(r -> r.note().equals(":655")).findFirst().orElse(null);
		assertNotNull(tSap, "the :655 row is transcribed");
		assertEquals(GT6RecipesDrying.FLUID_SAP, tSap.input(), "the guard row's input is the sap id");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(3).contains("POURED by task p21-drying-food-fluids"),
				"the food entry is the poured annotation, not a pool pointer");
	}

	/**
	 * The SKIPPED_UPSTREAM audit (spec ④): the pool pins the FULL dead-row census of the
	 * Drying book outside the poured families — the guarded external-fluid rows, the
	 * material liquids, the water_hot alias, and the crops/resin/ores/other pool
	 * pointers (the census = tasks.p19-research-drying-rows, the full-file reads).
	 */
	@Test
	void skippedUpstreamAuditPinsTheDeadRowCensus() {
		assertEquals(8, GT6RecipesDrying.SKIPPED_UPSTREAM.size(), "the audit entry census (the architect card list)");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(0).contains(":544-545 Tropics_Water"), "the guarded external-fluid rows");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(1).contains("MT.SaltWater.liquid"), "the material-liquid rows");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(2).contains("ic2hotwater"), "the water_hot ruling entry");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(3).contains("Loader_Recipes_Food.java:654-658"), "the food entry (poured annotation since p21)");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(4).contains("crop/bale listener family"), "the crop-bale pool");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(5).contains("slimeball family"), "the resin pool");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(6).contains("Sluice"), "the ores pool");
		assertTrue(GT6RecipesDrying.SKIPPED_UPSTREAM.get(7).contains("dye-fluid rows"), "the BlocksGT/dye pool");
	}
}
