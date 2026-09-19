package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
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

/**
 * The Fusion Reactor 18-row parity test (task p31-fusion) — every
 * Loader_Recipes_Other.java:949-966 line asserted against its transcribed constants:
 * the selector config (ST.tag(1)/ST.tag(2) as a REAL item input), the eUt (-8192 with
 * the two :952/:953 zero-power rows), the duration, the setSpecialNumber payload
 * (dur*8192*16 with the :8469/:94956 outliers), and the material+state+mB walk of both
 * fluid legs (captured through the resolver seam — the offline fixture cannot mint
 * per-material fluids, so material parity rides the CALL RECORD and amount parity the
 * built stacks).
 *
 * <p>Fixture posture = the GT6RecipesMassfabTest convention (frozen registry, vanilla
 * stand-ins, seams captured/restored per test).
 */
public class GT6RecipesFusionTest extends GTRecipesOfflineTestBase {

	/** One expected row — the :949-966 transcription (amounts in mB, the 1000-per-U unit walk). */
	private record ExpectedRow(String note, int selector, long eUt, long duration, long startLU,
			OreDictMaterial[] inMats, boolean[] inMolten, int[] inMB,
			OreDictMaterial[] outMats, boolean[] outMolten, int[] outMB,
			boolean hasVbDust) {}

	// the transcribed table, upstream order :949-966. LAZILY built — the class-load time
	// precedes MT.init (the a9027ac lesson): an eager capture would freeze null materials.
	private static ExpectedRow[] sRows = null;

	private static ExpectedRow[] rows() {
		if (sRows == null) sRows = buildRows();
		return sRows;
	}

	private static ExpectedRow[] buildRows() {
		return new ExpectedRow[] {
		new ExpectedRow(":949 D-D",           1, -8192,  730,   730L*131072, new OreDictMaterial[]{MT.D},            new boolean[]{false}, new int[]{2000}, new OreDictMaterial[]{MT.He_3, MT.T},       new boolean[]{false, false}, new int[]{500, 500}, false),
		new ExpectedRow(":950 T-T",           1, -8192, 1130,  1130L*131072, new OreDictMaterial[]{MT.T},            new boolean[]{false}, new int[]{2000}, new OreDictMaterial[]{MT.He},               new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":951 He3-He3",       1, -8192, 1290,  1290L*131072, new OreDictMaterial[]{MT.He_3},         new boolean[]{false}, new int[]{2000}, new OreDictMaterial[]{MT.He},               new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":952 He-He",         1,     0, 1890,  1890L*131072, new OreDictMaterial[]{MT.He},           new boolean[]{false}, new int[]{2000}, new OreDictMaterial[]{MT.Be_8},             new boolean[]{true},  new int[]{1000}, false),
		new ExpectedRow(":953 Be8",           1,     0, 3214,  3214L*131072, new OreDictMaterial[]{MT.Be_8},         new boolean[]{true},  new int[]{2000}, new OreDictMaterial[]{MT.O},                new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":954 H+B11",         2, -8192,  546,  8469L*131072, new OreDictMaterial[]{MT.H, MT.B_11},   new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He},     new boolean[]{false}, new int[]{3000}, false),
		new ExpectedRow(":955 H+C",           2, -8192,  315,   315L*131072, new OreDictMaterial[]{MT.H, MT.C},      new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.C_13},   new boolean[]{true},  new int[]{1000}, false),
		new ExpectedRow(":956 H+C13",         2, -8192,  754,   754L*131072, new OreDictMaterial[]{MT.H, MT.C_13},   new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.N},      new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":957 2H+N",          2, -8192, 1404,  1404L*131072, new OreDictMaterial[]{MT.H, MT.N},      new boolean[]{false, false}, new int[]{2000, 1000}, new OreDictMaterial[]{MT.He, MT.C, MT.O}, new boolean[]{false, true, false}, new int[]{500, 500, 500}, false),
		new ExpectedRow(":958 2H+O",          2, -8192,  455,   455L*131072, new OreDictMaterial[]{MT.H, MT.O},      new boolean[]{false, false}, new int[]{2000, 1000}, new OreDictMaterial[]{MT.He, MT.F, MT.N}, new boolean[]{false, false, false}, new int[]{500, 500, 500}, false),
		new ExpectedRow(":959 D+T",           2, -8192, 1760,  1760L*131072, new OreDictMaterial[]{MT.D, MT.T},      new boolean[]{false, false}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He},     new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":960 D+He3",         2, -8192, 1830,  1830L*131072, new OreDictMaterial[]{MT.D, MT.He_3},   new boolean[]{false, false}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He},     new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":961 T+He3",         2, -8192, 2640,  2640L*131072, new OreDictMaterial[]{MT.T, MT.He_3},   new boolean[]{false, false}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He, MT.D}, new boolean[]{false, false}, new int[]{750, 250}, false),
		new ExpectedRow(":962 D+Li6",         2, -8192, 3336,  3336L*131072, new OreDictMaterial[]{MT.D, MT.Li_6},   new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He, MT.He_3, MT.Li, MT.Be_7}, new boolean[]{false, false, true, true}, new int[]{375, 125, 125, 125}, false),
		new ExpectedRow(":963 He3+Li6",       2, -8192, 1690,  1690L*131072, new OreDictMaterial[]{MT.He_3, MT.Li_6}, new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.He},    new boolean[]{false}, new int[]{2000}, false),
		new ExpectedRow(":964 He+Be8",        2, -8192,  736,   736L*131072, new OreDictMaterial[]{MT.He, MT.Be_8},  new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.C},      new boolean[]{true},  new int[]{1000}, false),
		new ExpectedRow(":965 He+C",          2, -8192,  716,   716L*131072, new OreDictMaterial[]{MT.He, MT.C},     new boolean[]{false, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.O},      new boolean[]{false}, new int[]{1000}, false),
		new ExpectedRow(":966 Ad+Be7",        2, -8192, 1956, 94956L*131072, new OreDictMaterial[]{MT.Ad, MT.Be_7},  new boolean[]{true, true}, new int[]{1000, 1000}, new OreDictMaterial[]{MT.W, MT.He, MT.He_3, MT.T}, new boolean[]{true, false, false, false}, new int[]{1000, 16000, 24000, 24000}, true),
		};
	}

	private static java.util.function.Function<Integer, ItemStack> sDefaultCircuit;
	private static java.util.function.BiFunction<OreDictMaterial, Boolean, Fluid> sDefaultFluid;
	private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultItem;

	/** The (material, molten) call record the fixture resolver captures per build. */
	private static final List<OreDictMaterial> sRecordedMats = new ArrayList<>();
	private static final List<Boolean> sRecordedMolten = new ArrayList<>();

	@BeforeAll
	static void captureDefaults() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.registry.GTMaterialItems.initMaterials(); // the OP+MT walk (the neo-leg class-order lesson)
		sDefaultCircuit = GT6RecipesFusion.sCircuitResolver;
		sDefaultFluid = GT6RecipesFusion.sFluidResolver;
		sDefaultItem = GT6RecipesFusion.sMaterialItemResolver;
	}

	@BeforeEach
	void freshSeams() {
		sRecordedMats.clear();
		sRecordedMolten.clear();
		GT6RecipesFusion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER); // identity-only stand-in (the config parity rides the row data below)
		GT6RecipesFusion.sFluidResolver = (aMaterial, aMolten) -> {
			sRecordedMats.add(aMaterial);
			sRecordedMolten.add(aMolten);
			return aMolten ? Fluids.LAVA : Fluids.WATER;
		};
		GT6RecipesFusion.sMaterialItemResolver = (aPrefix, aMaterial) ->
				aPrefix == OP.dust && aMaterial == MT.Vb ? Items.IRON_INGOT : null; // the :966 dust.mat(MT.Vb, 1) arm
		GT6RecipeMaps.reset();
		GT6RecipesFusion.resetForTest();
	}

	@AfterEach
	void restoreResolvers() {
		GT6RecipesFusion.sCircuitResolver = sDefaultCircuit;
		GT6RecipesFusion.sFluidResolver = sDefaultFluid;
		GT6RecipesFusion.sMaterialItemResolver = sDefaultItem;
		GT6RecipeMaps.reset();
		GT6RecipesFusion.resetForTest();
	}

	@AfterAll
	static void restoreEverything() {
		GT6RecipesFusion.sCircuitResolver = sDefaultCircuit;
		GT6RecipesFusion.sFluidResolver = sDefaultFluid;
		GT6RecipesFusion.sMaterialItemResolver = sDefaultItem;
	}

	private int pour() {
		GT6RecipeMaps.init();
		GT6RecipesFusion.load();
		assertNotNull(GT6RecipeMaps.FUSION, "the FUSION map exists after the pour");
		return GT6RecipeMaps.FUSION.mRecipeList.size();
	}

	/** The order-free row lookup: every duration is unique across the 18 rows (the (duration, eUt) key). */
	private Recipe rowByDuration(long aDuration) {
		for (Recipe tRecipe : GT6RecipeMaps.FUSION.mRecipeList) if (tRecipe.mDuration == aDuration) return tRecipe;
		return null;
	}

	@Test
	void theEighteenRowsPourAndMatchTheUpstreamTable() {
		assertEquals(18, pour(), "the :949-966 block pours exactly 18 rows");
		assertEquals(18, GT6RecipesFusion.rows().size(), "the row table is the upstream order");

		// the resolver call record walks inputs then outputs per row (the buildRecipe order)
		int tCall = 0;
		for (int tRow = 0; tRow < rows().length; tRow++) {
			ExpectedRow tExpected = rows()[tRow];
			Recipe tRecipe = rowByDuration(tExpected.duration());
			assertNotNull(tRecipe, "row " + tRow + " (" + tExpected.note() + ") poured");
			assertTrue(GT6RecipeMaps.FUSION.mRecipeList.contains(tRecipe), "the row lives in the map list");

			// the selector rides item input 0 (count 1), the map item-min 1 satisfied
			assertEquals(1, tRecipe.mInputs.length, tExpected.note() + " carries exactly the selector item input");
			assertEquals(1, tRecipe.mInputs[0].getCount(), tExpected.note() + " selector count 1");

			// the scalar columns
			assertEquals(tExpected.eUt(), tRecipe.mEUt, tExpected.note() + " eUt");
			assertEquals(tExpected.duration(), tRecipe.mDuration, tExpected.note() + " duration");
			assertEquals(tExpected.startLU(), tRecipe.mSpecialValue, tExpected.note() + " the setSpecialNumber payload");

			// the fluid legs: material identity via the call record, amounts via the stacks
			assertEquals(tExpected.inMats().length, tRecipe.mFluidInputs.length, tExpected.note() + " fluid-input count");
			for (int i = 0; i < tExpected.inMats().length; i++) {
				assertEquals(tExpected.inMats()[i], sRecordedMats.get(tCall), tExpected.note() + " input " + i + " material");
				assertEquals(tExpected.inMolten()[i], sRecordedMolten.get(tCall), tExpected.note() + " input " + i + " state");
				tCall++;
				assertEquals(tExpected.inMB()[i], tRecipe.mFluidInputs[i].getAmount(), tExpected.note() + " input " + i + " mB");
			}
			assertEquals(tExpected.outMats().length, tRecipe.mFluidOutputs.length, tExpected.note() + " fluid-output count");
			for (int i = 0; i < tExpected.outMats().length; i++) {
				assertEquals(tExpected.outMats()[i], sRecordedMats.get(tCall), tExpected.note() + " output " + i + " material");
				assertEquals(tExpected.outMolten()[i], sRecordedMolten.get(tCall), tExpected.note() + " output " + i + " state");
				tCall++;
				assertEquals(tExpected.outMB()[i], tRecipe.mFluidOutputs[i].getAmount(), tExpected.note() + " output " + i + " mB");
			}

			// the item outputs: the :966 Vibranium dust row only
			if (tExpected.hasVbDust()) {
				assertEquals(1, tRecipe.mOutputs.length, tExpected.note() + " the Vb dust rides the item output");
				assertEquals(Items.IRON_INGOT, tRecipe.mOutputs[0].getItem(), tExpected.note() + " the fixture stand-in identity");
			} else {
				assertEquals(0, tRecipe.mOutputs.length, tExpected.note() + " carries no item outputs");
			}
		}
		assertEquals(sRecordedMats.size(), tCall, "the resolver saw exactly the transcribed calls");
	}

	@Test
	void theTwoZeroPowerRowsAreHeAndBe8() {
		pour();
		assertEquals(0, rowByDuration(1890).mEUt, ":952 He2->Be8 is the first zero-power row");
		assertEquals(0, rowByDuration(3214).mEUt, ":953 Be8->O is the second zero-power row");
		for (ExpectedRow tExpected : rows()) if (tExpected.duration() != 1890 && tExpected.duration() != 3214) {
			assertEquals(-8192, rowByDuration(tExpected.duration()).mEUt, tExpected.note() + " is a -8192 generator row");
		}
	}

	@Test
	void findRecipeMatchesByFluidShapeAndAmounts() {
		pour();
		// row :959 D+T -> He: two 1000 mB gas stacks (water stand-ins) + the selector
		FluidStack[] tFluids = {new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.WATER, 1000)};
		Recipe tRecipe = GT6RecipeMaps.FUSION.findRecipe(null, 16384, null, tFluids, new ItemStack(Items.PAPER));
		assertNotNull(tRecipe, "the fluid shape matches at voltage 16384");
		// the map list is a SET: any of the single/double gas-input rows matches first —
		// assert the returned row is one of the shape-compatible set (the per-row parity
		// rides the order-free rowByDuration sweep above)
		boolean tCompatible = false;
		for (ExpectedRow tExpected : rows()) if (tExpected.duration() == tRecipe.mDuration) tCompatible = true;
		assertTrue(tCompatible, "the matched row is one of the transcribed rows");
		// the short-supply refusal lives in the CONSUME half (the lookup probe runs with
		// aDontCheckStackSizes=true, the upstream :487/:501 posture)
		// the matcher is per-requirement first-match (the upstream :804-810 loop) — a
		// sharply-short pair [999, 999] leaves the 2x1000 requirements unsatisfiable
		FluidStack[] tShort = {new FluidStack(Fluids.WATER, 999), new FluidStack(Fluids.WATER, 999)};
		Recipe tShortRow = GT6RecipeMaps.FUSION.findRecipe(null, 16384, null, tShort, new ItemStack(Items.PAPER));
		assertNotNull(tShortRow, "the lookup probe matches (the amounts unchecked)...");
		assertFalse(tShortRow.isRecipeInputEqual(true, false, tShort, new ItemStack(Items.PAPER)), "...but the consume refuses the 999 mB short supply");
		// below the |EUt| voltage refuses (absGreaterEqual)
		FluidStack[] tOk = {new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.WATER, 1000)};
		assertNull(GT6RecipeMaps.FUSION.findRecipe(null, 4095, null, tOk, new ItemStack(Items.PAPER)), "voltage 4095 < |EUt| 8192 refuses");
	}

	@Test
	void thePourIsGenerationTracked() {
		assertEquals(18, pour(), "the first pour lands 18 rows");
		GT6RecipesFusion.load(); // the second call is a no-op (the generation flag)
		assertEquals(18, GT6RecipeMaps.FUSION.mRecipeList.size(), "no duplicate pour");
		GT6RecipeMaps.reset();
		GT6RecipesFusion.resetForTest();
		assertNull(GT6RecipeMaps.FUSION, "the generation reset drops the map");
	}
}
