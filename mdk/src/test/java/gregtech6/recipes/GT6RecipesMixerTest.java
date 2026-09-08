package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM.Mixer C-Foam pour offline tests (task p26-c-foam-fluid-refill — the
 * GT6RecipesDryingTest loop-walk shape): the thirteen rock groups (the base ten +
 * twelve colours, Loader_Recipes_Other.java:252-302), the faithful water × clay × sand
 * cross-product (the :230 FL.waters(1000) × :251 ANY.Clay.mToThis × ANY.SiO2.mToThis
 * loops), the FOUR missing colours (Cyan/Purple/Blue/Magenta — upstream has no group, the
 * faithful absence) and the 32 Pd owned rows (:485-486).
 *
 * <p>The pour runs over fixture seams (the Canner test convention): every material item
 * resolves to one vanilla item, the water variants to vanilla water, the C-Foam outputs to
 * DISTINCT fixture fluids (base = flowing water, dyed = lava, owned = milk) so the row
 * census counts per output leg; the resolver CALLS are recorded per dye index (the
 * four-way pin's row leg). The MIXER map is the p26 declared-append
 * {@link GT6RecipeMaps#MIXER} (the RM.java:74 constants row).
 */
class GT6RecipesMixerTest extends GTRecipesOfflineTestBase {

	/** The expected base-group rock count (:252 — the "10 岩组" pin). */
	private static final int BASE_ROCKS = 10;
	/** The expected per-group rock counts in file order (:252 + :256-302). */
	private static final List<Integer> ROCK_COUNTS = List.of(10, 8, 4, 5, 6, 1, 3, 3, 6, 2, 1, 2, 2);
	/** The colour groups' dye indices in file order (:256-302, the upstream DYE_INDEX order). */
	private static final List<Integer> DYE_ORDER = List.of(15, 0, 8, 7, 12, 10, 2, 1, 11, 14, 9, 3);
	/** The FOUR colours with no upstream group (Cyan 6 / Purple 5 / Blue 4 / Magenta 13) — the faithful absence. */
	private static final Set<Integer> MISSING_COLOURS = Set.of(4, 5, 6, 13);

	private static final int[] sDyedCalls = new int[16];
	private static final int[] sOwnedCalls = new int[16];

	@BeforeAll
	static void bootTheMaterialUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (the family-walk prerequisite)
	}

	@BeforeEach
	void armSeams() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		Arrays.fill(sDyedCalls, 0);
		Arrays.fill(sOwnedCalls, 0);
		GT6RecipesMixer.sMaterialItemResolver = (aPrefix, aMaterial) -> net.minecraft.world.item.Items.BRICK; // everything resolves
		GT6RecipesMixer.sWaterResolver = aIndex -> Fluids.WATER;
		GT6RecipesMixer.sBaseCfoamResolver = () -> Fluids.FLOWING_WATER;
		GT6RecipesMixer.sCfoamResolver = (aIndex, aOwned) -> {
			if (aOwned) sOwnedCalls[aIndex]++;
			else sDyedCalls[aIndex]++;
			return aOwned ? Fluids.FLOWING_LAVA : Fluids.LAVA; // DISTINCT fixture fluids per output leg
		};
		GT6RecipesMixer.resetForTest();
	}

	@AfterEach
	void restoreSeams() {
		// the live-seam lambdas restored verbatim (the Canner restoreSeams convention —
		// creating a lambda executes nothing, the unbound RegistryObject paths are never touched)
		GT6RecipesMixer.sMaterialItemResolver = GT6RecipesMixer::resolveItem;
		GT6RecipesMixer.sWaterResolver = GT6RecipesMixer::resolveWater;
		GT6RecipesMixer.sBaseCfoamResolver = () -> GTFluids.CFOAM.get();
		GT6RecipesMixer.sCfoamResolver = (aIndex, aOwned) -> GTFluids.cfoam(aIndex, aOwned).source.get();
		GT6RecipeMaps.reset();
	}

	@AfterAll
	static void finalReset() {
		GT6RecipeMaps.reset();
	}

	/** The thirteen groups: the base ten + twelve colour groups, each rock list upstream-identical (:252-302). */
	@Test
	void groupsMatchTheUpstreamRockLists() {
		List<GT6RecipesMixer.CFoamGroup> tGroups = GT6RecipesMixer.groups();
		assertEquals(13, tGroups.size(), "the base group + the TWELVE colour groups (:252-302)");
		assertEquals(GT6RecipesMixer.BASE_GROUP, tGroups.get(0).dyeIndex(), "the first group is the BASE (output gt6:cfoam)");
		assertEquals(MT.Stone, tGroups.get(0).rocks().get(0), ":252 — the base list opens with MT.Stone");
		assertEquals(MT.Asbestos, tGroups.get(0).rocks().get(9), ":252 — the base list closes with MT.Asbestos");
		assertEquals(DYE_ORDER, tGroups.subList(1, tGroups.size()).stream().map(GT6RecipesMixer.CFoamGroup::dyeIndex).toList(),
				":256-302 — White/Black/Gray/LightGray/LightBlue/Lime/Green/Red/Yellow/Orange/Pink/Brown");
		assertEquals(ROCK_COUNTS, tGroups.stream().map(g -> g.rocks().size()).toList(),
				"the upstream rock-list sizes per group (10 base + 43 colour rocks)");
		assertTrue(tGroups.stream().noneMatch(g -> MISSING_COLOURS.contains(g.dyeIndex())),
				"Cyan/Purple/Blue/Magenta have NO upstream group — the faithful absence (spec ②)");
	}

	/** The loop families the port walks live: ANY.Clay.mToThis and ANY.SiO2.mToThis (the :251 loop heads). */
	@Test
	void clayAndSandFamiliesAreTheLoopHeads() {
		assertNotNull(ANY.Clay.mToThis);
		assertNotNull(ANY.SiO2.mToThis);
		// the Drying test pins the clay membership upstream-identical; here the SAND side joins the pins
		assertTrue(ANY.SiO2.mToThis.contains(MT.SiO2) && ANY.SiO2.mToThis.contains(MT.Sand)
				&& ANY.SiO2.mToThis.contains(MT.Glass) && ANY.SiO2.mToThis.contains(MT.Flint)
				&& ANY.SiO2.mToThis.contains(MT.STONES.Quartzite),
				"the :251 ANY.SiO2.mToThis loop head carries the re-registration family (SiO2/Sand/Glass/Flint/Quartzite..)");
	}

	/**
	 * The pour census: rows = waters × clays × sands × Σ(rocks) × 2 sizes (the :251-304
	 * cross-product) + 32 Pd rows (:485-486, per colour, water-independent). Each colour
	 * group resolves its dyed output once per size-pass (rocks × W × C × S × 2); the owned
	 * output resolves exactly twice per colour (the two Pd rows).
	 */
	@Test
	void pourLandsTheFullUpstreamCrossProduct() {
		GT6RecipesMixer.load();
		int tWaters = GT6RecipesMixer.WATER_COUNT;
		int tClays = ANY.Clay.mToThis.size();
		int tSands = ANY.SiO2.mToThis.size();
		int tRocks = BASE_ROCKS + DYE_ORDER.stream().mapToInt(this::rocksOf).sum();
		assertEquals((long) tWaters * tClays * tSands * tRocks * 2 + 32, GT6RecipeMaps.MIXER.mRecipeList.size(),
				"the :230×:251×:251×:252-302 cross-product × 2 sizes + the 32 :485-486 Pd rows");
		for (int i = 0; i < 16; i++) {
			int tExpectedDyed = (i == GT6RecipesMixer.BASE_GROUP ? 0 : rocksOf(i) * tWaters * tClays * tSands * 2) + 2;
			assertEquals(tExpectedDyed, sDyedCalls[i], "dye index " + i + ": the dyed output resolves once per colour rock row (both sizes) + its :485/:486 input legs");
			assertEquals(2, sOwnedCalls[i], "dye index " + i + ": the owned output resolves exactly for the 2 Pd rows");
		}
	}

	/** The rocks of one dye-index group (0 for a missing colour). */
	private int rocksOf(int aDyeIndex) {
		for (GT6RecipesMixer.CFoamGroup tGroup : GT6RecipesMixer.groups()) {
			if (tGroup.dyeIndex() == aDyeIndex) return tGroup.rocks().size();
		}
		return 0; // a missing colour has no group
	}

	/**
	 * The four missing colours contribute ZERO rock rows: the dyed-output rows total exactly
	 * Σ(present rocks) × W × C × S × 2 — a missing-colour row would push the count past it.
	 */
	@Test
	void theFourMissingColoursHaveZeroRockRows() {
		GT6RecipesMixer.load();
		int tWaters = GT6RecipesMixer.WATER_COUNT;
		int tClays = ANY.Clay.mToThis.size();
		int tSands = ANY.SiO2.mToThis.size();
		long tDyedRows = GT6RecipeMaps.MIXER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 3) // rock rows only (the Pd rows carry one input)
				.filter(r -> r.mFluidOutputs.length == 1 && r.mFluidOutputs[0].getFluid() == Fluids.LAVA)
				.count();
		assertEquals((long) tWaters * tClays * tSands * 43 * 2, tDyedRows,
				"every dyed row belongs to the twelve present colours — the missing four contribute zero");
		for (int i : MISSING_COLOURS) {
			assertEquals(2, sDyedCalls[i], "missing colour " + i + ": ONLY its 2 Pd-row input legs ride the dyed fluid (zero rock rows)");
			assertEquals(2, sOwnedCalls[i], "missing colour " + i + ": only its 2 Pd rows (:485-486 ride the 16-colour loop)");
		}
	}

	/** The small/big rock row shape verbatim (:253/:254 — the dust ladder, water leg, output amount, duration, EUt). */
	@Test
	void rockRowShapeIsTheUpstreamLine() {
		GT6RecipesMixer.load();
		int tWaters = GT6RecipesMixer.WATER_COUNT;
		int tClays = ANY.Clay.mToThis.size();
		int tSands = ANY.SiO2.mToThis.size();
		long tPerSize = (long) tWaters * tClays * tSands * (BASE_ROCKS + 43);
		// the SMALL rows: 6 rock + 2 sand + 1 claySmall, water 1000 → foam 1000, 128t
		List<Recipe> tSmall = GT6RecipeMaps.MIXER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 3 && r.mInputs[0].getCount() == 6 && r.mDuration == 128)
				.toList();
		assertEquals(tPerSize, tSmall.size(), "every rock has one 128t small row per water×clay×sand");
		Recipe tRow = tSmall.get(0);
		assertTrue(tRow.mCanBeBuffered, "addRecipeX(T, ...) — buffered");
		assertEquals(16, tRow.mEUt, "EUt 16 (:253)");
		assertEquals(3, tRow.mInputs.length, "rock + sand + clay");
		assertEquals(6, tRow.mInputs[0].getCount(), "OM.dust(tRock, U*6) → dust x6");
		assertEquals(2, tRow.mInputs[1].getCount(), "OM.dust(tSand, U*2) → dust x2");
		assertEquals(1, tRow.mInputs[2].getCount(), "OM.dust(tClay, U4) → dustSmall x1");
		assertEquals(1000, tRow.mFluidInputs[0].getAmount(), "FL.mul(tWater, 1) over the 1000 mB FL.waters variant");
		assertEquals(1000, tRow.mFluidOutputs[0].getAmount(), "FL.CFoam.make(1000) / FL.mul(DYED_C_FOAMS[dye], 10)");
		assertEquals(0, tRow.mOutputs.length, "ZL_IS — no item outputs");
		// the BIG rows: 24 rock + 8 sand + 1 clay, water 4000 → foam 4000, 512t
		List<Recipe> tBig = GT6RecipeMaps.MIXER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 3 && r.mInputs[0].getCount() == 24 && r.mDuration == 512)
				.toList();
		assertEquals(tPerSize, tBig.size(), "every rock has one 512t big row per water×clay×sand");
		Recipe tBigRow = tBig.get(0);
		assertEquals(16, tBigRow.mEUt, "EUt 16 (:254)");
		assertEquals(8, tBigRow.mInputs[1].getCount(), "OM.dust(tSand, U*8) → dust x8");
		assertEquals(1, tBigRow.mInputs[2].getCount(), "OM.dust(tClay, U) → dust x1 (the big row's clay leg is a FULL dust)");
		assertEquals(4000, tBigRow.mFluidInputs[0].getAmount(), "FL.mul(tWater, 4)");
		assertEquals(4000, tBigRow.mFluidOutputs[0].getAmount(), "FL.CFoam.make(4000) / FL.mul(DYED_C_FOAMS[dye], 40)");
	}

	/** The Pd owned rows (:485-486): 16 colours × 2 sizes, one dust input, dyed → owned at equal amounts. */
	@Test
	void pdRowsPourAllThirtyTwoWithTheUpstreamShape() {
		GT6RecipesMixer.load();
		// the owned-output rows: input = the dyed fixture (lava), output = the owned fixture (milk)
		List<Recipe> tPdRows = GT6RecipeMaps.MIXER.mRecipeList.stream()
				.filter(r -> r.mInputs.length == 1)
				.filter(r -> r.mFluidOutputs.length == 1 && r.mFluidOutputs[0].getFluid() == Fluids.FLOWING_LAVA)
				.toList();
		assertEquals(32, tPdRows.size(), "the :485 small + the :486 big ladders over the 16 colours");
		assertEquals(16, tPdRows.stream().filter(r -> r.mDuration == 16).count(), ":485 — sixteen 16t rows");
		assertEquals(16, tPdRows.stream().filter(r -> r.mDuration == 64).count(), ":486 — sixteen 64t rows");
		Recipe tSmall = tPdRows.stream().filter(r -> r.mDuration == 16).findAny().orElseThrow();
		assertTrue(tSmall.mCanBeBuffered, "addRecipe1(T, ...) — buffered");
		assertEquals(16, tSmall.mEUt, "EUt 16 (:485)");
		assertEquals(1, tSmall.mInputs[0].getCount(), "OM.dust(MT.Pd, U4) → dustSmall x1");
		assertEquals(100, tSmall.mFluidInputs[0].getAmount(), "DYED_C_FOAMS[i] at the 100-unit make");
		assertEquals(100, tSmall.mFluidOutputs[0].getAmount(), "DYED_C_FOAMS_OWNED[i] at the 100-unit make");
		Recipe tBig = tPdRows.stream().filter(r -> r.mDuration == 64).findAny().orElseThrow();
		assertEquals(16, tBig.mEUt, "EUt 16 (:486)");
		assertEquals(1, tBig.mInputs[0].getCount(), "OM.dust(MT.Pd) → dust x1");
		assertEquals(400, tBig.mFluidInputs[0].getAmount(), "FL.mul(DYED_C_FOAMS[i], 4)");
		assertEquals(400, tBig.mFluidOutputs[0].getAmount(), "FL.mul(DYED_C_FOAMS_OWNED[i], 4)");
		// the base fluid is NOT an owned-row leg (owned production starts FROM the dyed fluids)
		assertTrue(tPdRows.stream().noneMatch(r -> r.mFluidInputs[0].getFluid() == Fluids.FLOWING_WATER),
				"the :485-486 legs ride the dyed fluids, never the base");
	}

	/** The base rock group (:252-254) outputs the BASE gt6:cfoam fixture, never a dyed one. */
	@Test
	void theBaseGroupOutputsTheBaseFluid() {
		GT6RecipesMixer.load();
		int tWaters = GT6RecipesMixer.WATER_COUNT;
		int tClays = ANY.Clay.mToThis.size();
		int tSands = ANY.SiO2.mToThis.size();
		long tBaseRows = GT6RecipeMaps.MIXER.mRecipeList.stream()
				.filter(r -> r.mFluidOutputs.length == 1 && r.mFluidOutputs[0].getFluid() == Fluids.FLOWING_WATER)
				.count();
		assertEquals((long) tWaters * tClays * tSands * BASE_ROCKS * 2, tBaseRows,
				"the base ten rocks produce gt6:cfoam per water×clay×sand×2 sizes — FL.CFoam.make(1000)/make(4000)");
	}

	/** load() is idempotent per generation (the pour flag). */
	@Test
	void loadIsIdempotentPerGeneration() {
		GT6RecipesMixer.load();
		long tFirst = GT6RecipeMaps.MIXER.mRecipeList.size();
		assertTrue(tFirst > 0, "the fixture pour");
		GT6RecipesMixer.load();
		assertEquals(tFirst, GT6RecipeMaps.MIXER.mRecipeList.size(), "the second load() is a no-op (the generation flag)");
	}

	/** An unresolvable dustSmall leg skips silently (the upstream mat() null drop): the small rock rows + :485 drop. */
	@Test
	void unresolvableDustSmallLegSkipsSilently() {
		GT6RecipesMixer.sMaterialItemResolver = (aPrefix, aMaterial) -> aPrefix == OP.dustSmall ? null : net.minecraft.world.item.Items.BRICK;
		GT6RecipesMixer.load();
		int tWaters = GT6RecipesMixer.WATER_COUNT;
		int tClays = ANY.Clay.mToThis.size();
		int tSands = ANY.SiO2.mToThis.size();
		long tCrossProduct = (long) tWaters * tClays * tSands * (BASE_ROCKS + 43);
		// the small rock rows (dustSmall clay) and the sixteen :485 rows (dustSmall Pd) drop;
		// the big rock rows and the sixteen :486 rows (full dust) pour
		assertEquals(tCrossProduct /* big rows */ + 16 /* the :486 rows */,
				GT6RecipeMaps.MIXER.mRecipeList.size(),
				"the small rows drop silently, the big rows + the :486 rows pour (the mat() null-drop semantics)");
	}
}
