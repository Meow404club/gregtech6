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
	 * The offline fixture: every REGISTERED fluid id — water, distw and the six
	 * p16-aqua-fluids ids — resolves to the vanilla water fluid (the recipe mechanics
	 * only compare identities — the GTEngineFuelsTest WATER_FIXTURE convention); the
	 * deliberately unregistered water_hot alias (:530) alone stays null, mirroring the
	 * live resolver's absent-fluid verdict.
	 */
	private static final Function<String, Fluid> CENSUS_FIXTURE = aId ->
			GT6RecipesDrying.FLUID_HOT.equals(aId) ? null : Fluids.WATER;

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
	 */
	private static final Set<Item> RESERVED_VANILLA_ITEMS = Set.of(
			Items.SNOWBALL, Blocks.ICE.asItem(), Blocks.PACKED_ICE.asItem(), Blocks.SNOW_BLOCK.asItem());

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
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
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
		GT6RecipesDrying.sFluidResolver = CENSUS_FIXTURE;
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
				.filter(r -> r.mInputs.length == 1).toList();
		assertEquals(11, tIceRecipes.size(), "7 water + 11 ice: the :513/:514 gem rows skip (no such items, upstream too)");
		assertEquals(18, GT6RecipeMaps.DRYING.mRecipeList.size(), "7 water + 11 ice = the full poured census");
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
}
