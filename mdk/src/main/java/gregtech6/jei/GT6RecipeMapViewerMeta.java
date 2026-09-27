package gregtech6.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The viewer-neutral per-map category metadata + layout math + cost-text formatter of the
 * generic RM recipe-category factory (task debt-jei-emi-batch1, the batch-1 ruling of
 * decisions.2026-09-26-debt-jei-emi-coverage) — the shared seam consumed by BOTH the JEI
 * category ({@link GT6RecipeMapJeiCategory}) and the EMI category
 * (gregtech6.emi.GT6RecipeMapEmiCategory/GT6RecipeMapEmiRecipe), the same cross-package
 * sharing shape as the tier-b {@link GT6RecipeViewerText} seam. Strictly vanilla +
 * gregtech6.recipes imports: no JEI, no EMI, no client class — the whole face is
 * offline-testable.
 *
 * <p><b>The porting source (read in full, not guessed):</b> upstream
 * gregapi/NEI_RecipeMap.java, the 718-line single NEI handler that served EVERY
 * mNEIAllowed map — the ctor layout switches (:170-275 item inputs, :282-386 item
 * outputs, :389-390 the fluid rows), the drawExtras cost/tier/time/special text
 * (:680-717), the chance tooltip (:662-666) and the not-consumed tooltip (:671). The
 * per-map columns this table restores are the trailing NEI args of the upstream RM.java /
 * FM.java RecipeMap ctor rows (upstream Recipe.java:106 — aFuelMap,
 * aShowVoltageAmperageInNEI, aNEIAllowed, aConfigAllowed, aNeedsOutputs, aCombinePower,
 * aUseBucketSizeIn, aUseBucketSizeOut) that the port's 15-arg ctor folded away (the
 * declared fold documented on every GT6RecipeMaps field). Only the columns a viewer
 * consumes are restored: aNEIAllowed, aShowVoltageAmperageInNEI, aCombinePower and the
 * special-value triple. Dropped by ruling: aUseBucketSizeIn/Out (upstream
 * container-ized the fluids into buckets for NEI display, :389 FL.display — the modern
 * viewers render FluidStack natively, decisions.2026-09-26-debt-jei-emi-coverage ⑤),
 * aConfigAllowed/aNeedsOutputs (NEI-config faces with no port counterpart).
 *
 * <p><b>The exclusion table</b> (r-jei-emi-coverage id927, the research-card pin): the
 * true-zero / special-surface maps never enter a category even though their upstream
 * mNEIAllowed may be true — FURNACE (the port is the vanilla smelting mirror, the vanilla
 * viewer category already shows it), FURNACE_FUEL (the on-demand ForgeHooks burn-time
 * synthesizer, zero static rows ever), CRUCIBLE_SMELTING + CRUCIBLE_ALLOYING (the
 * dynamic material-graph derivation, zero static rows), BUMBLELYZER (the display stock
 * rides sFakeRecipes OUTSIDE mRecipeList) and PLANTALYZER (the Forestry/IC2 compat dead
 * surface). CHISEL and AUTOCRAFTER are excluded by the upstream mNEIAllowed=F itself
 * (RM.java:138/:63) — the faithful form, no ruling needed.
 *
 * <p><b>The visibility ruling (batch 1 canary → batch 2 full opening):</b> batch 1
 * shipped the six canaries of decisions.2026-09-26-debt-jei-emi-coverage ②
 * (COKE_OVEN/SHREDDER/CRUSHER/LATHE/DISTILLERY/DRYING) plus BEDROCK_ORE_LIST
 * (upstream RM.java:153 IS a NEI display map, in the acceptance face); batch 2
 * (task debt-jei-emi-batch2) opens visibility to the WHOLE eligible set — a map is
 * visible exactly when {@link #eligible} says so. The closure stays the exclusion
 * table (6) plus the upstream mNEIAllowed=F rows: 80 census maps → 72 visible.
 * The registration-cost ruling for the big maps this opens (MIXER's ~56000 rows,
 * MASSFAB's ~4220) lives in the registration paragraph at the bottom of this doc.
 */
public final class GT6RecipeMapViewerMeta {

	/** NEI handler geometry (NEI_RecipeMap.init NBT: handlerWidth 166): the shared category width. */
	public static final int CATEGORY_WIDTH = 166;
	/** NEI's fixed text band ran to y123 (+9 font) under a 135-tall handler; +5 margin → the shared height. */
	public static final int CATEGORY_HEIGHT = 140;
	/** NEI drawExtras x=10 (NEI_RecipeMap.java:689). */
	public static final int TEXT_X = 10;
	/** NEI's 10px line pitch (73/83/93/103/113/123). */
	public static final int TEXT_LINE_HEIGHT = 10;
	/** Upstream NEI_RecipeMap.java:665 "Chance: XX.XX%" + the " each" suffix for stacks > 1. */
	public static final String NOT_CONSUMED_TEXT = "Does not get consumed in the process";

	private GT6RecipeMapViewerMeta() {}

	/**
	 * The upstream trailing-NEI-arg columns a viewer consumes, keyed by the RecipeMap
	 * internal name (the GT6RecipeMaps census key). A pure transcription — {@code null}
	 * special strings are "" and the multiplier folds to 1 exactly like every census row
	 * that carries no special value.
	 */
	public record MapMeta(boolean neiAllowed, boolean showVoltageAmperage, boolean combinePower,
			String specialValuePre, long specialValueMultiplier, String specialValuePost) {
		public static final MapMeta STANDARD = new MapMeta(true, true, false, "", 1, "");
	}

	/** The RM.java:138/:63 + FM.java:38 rows whose upstream aNEIAllowed is F. */
	private static final Set<String> NEI_DISALLOWED = Set.of("gt.recipe.chisel", "gt.recipe.autocrafting", "mc.recipe.furnacefuel");

	/**
	 * The ruled exclusion table (the class doc) — these never enter a category, whatever
	 * their upstream mNEIAllowed column says.
	 */
	private static final Set<String> EXCLUDED = Set.of(
			"mc.recipe.furnace",            // vanilla mirror: the vanilla viewer category is the face
			"mc.recipe.furnacefuel",        // on-demand synthesizer, zero static rows
			"gt.recipe.cruciblesmelting",   // dynamic material-graph derivation, zero static rows
			"gt.recipe.cruciblealloying",   // dynamic alloying display, zero static rows
			"gt.recipe.bumblelyzer",        // display stock rides sFakeRecipes outside mRecipeList
			"gt.recipe.plantalyzer");       // compat dead surface

	/**
	 * The batch-2 visible set = the eligible set (the canary switch of batch 1 fully
	 * open — the class doc). Kept as the plugins' named seam: GT6JeiPlugin and
	 * gregtech6.emi.GT6EmiPlugin both iterate exactly this predicate.
	 */
	public static boolean visibleToViewers(RecipeMap aMap) {
		return eligible(aMap);
	}
	/**
	 * The per-map special deviations from {@link MapMeta#STANDARD} — the census rows whose
	 * upstream tail actually differs. Every census row NOT listed here is STANDARD
	 * ({@code T,T,T,T,F} over the 7-arg form, special {@code "",1,""}): the overwhelming
	 * RM.java shape. The deviations, with their upstream line pins:
	 * <ul>
	 * <li>the five fuel maps carry combinePower=T (FM.java:40/:41/:42/:43/:45 — the
	 *     {@code T,T,T,F,T,F,F} rows);</li>
	 * <li>FUSION carries the special-value triple {@code "Start: ", 1, " LU"}
	 *     (RM.java:146);</li>
	 * <li>the crucible pair carries {@code "Temperature: ", 1, " K"} (RM.java:128/:129 —
	 *     excluded from categories, tabled for census completeness);</li>
	 * <li>NEI_DISALLOWED above carries the mNEIAllowed=F rows.</li>
	 * </ul>
	 */
	private static final Map<String, MapMeta> DEVIATIONS = new HashMap<>();

	private static void deviation(String aName, boolean aNeiAllowed, boolean aShowVoltage, boolean aCombinePower,
			String aPre, long aMultiplier, String aPost) {
		DEVIATIONS.put(aName, new MapMeta(aNeiAllowed, aShowVoltage, aCombinePower, aPre, aMultiplier, aPost));
	}

	static {
		// the mNEIAllowed=F rows (furnacefuel's aShowVoltageAmperageInNEI is T per the
		// FM.java:38 T,F,... row tail — batch 1 had misread it F; dead value anyway, the
		// map is EXCLUDED and never renders)
		deviation("gt.recipe.chisel", false, true, false, "", 1, "");       // RM.java:138
		deviation("gt.recipe.autocrafting", false, true, false, "", 1, ""); // RM.java:63
		deviation("mc.recipe.furnacefuel", false, true, false, "", 1, "");  // FM.java:38
		// the fuel maps: combinePower=T (FM.java:40/:41/:42/:43/:45)
		deviation("gt.recipe.fuels.fluidbed", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.burn", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.gas", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.hot", true, true, true, "", 1, "");
		deviation("gt.recipe.fuels.engine", true, true, true, "", 1, "");
		// the special-value rows (RM.java:146/:128/:129)
		deviation("gt.recipe.fusionreactor", true, true, false, "Start: ", 1, " LU");
		deviation("gt.recipe.cruciblealloying", true, true, false, "Temperature: ", 1, " K");
		deviation("gt.recipe.cruciblesmelting", true, true, false, "Temperature: ", 1, " K");
		// every other census row (69 of 75) is STANDARD: neiAllowed=T, showVoltage=T,
		// combinePower=F, no special value — the RM.java:63-153 dominant column shape
	}

	/** The per-map columns; every non-census name reads as STANDARD (the defensive default). */
	public static MapMeta metaOf(RecipeMap aMap) {
		return DEVIATIONS.getOrDefault(aMap.mNameInternal, MapMeta.STANDARD);
	}

	/** Category-eligible: tabled census map, upstream mNEIAllowed, not ruled out. */
	public static boolean eligible(RecipeMap aMap) {
		return !NEI_DISALLOWED.contains(aMap.mNameInternal) && !EXCLUDED.contains(aMap.mNameInternal);
	}

	/** The visible maps in deterministic (name-sorted) registration order. */
	public static List<RecipeMap> visibleMaps() {
		List<RecipeMap> rMaps = new ArrayList<>();
		Set<String> tSorted = new TreeSet<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) if (visibleToViewers(tMap)) tSorted.add(tMap.mNameInternal);
		for (String tName : tSorted) rMaps.add(RecipeMap.RECIPE_MAPS.get(tName));
		return rMaps;
	}

	// -----------------------------------------------------------------------
	// The layout math — NEI_RecipeMap.CachedDefaultRecipe ctor switches, translated.
	// Item slots: a 3-column 18px grid anchored at x17 (inputs) / x107 (outputs); the row
	// count and the row Ys depend on the declared slot counts exactly like the upstream
	// switch (1-3 = one row at y7-or-25 by the fluid threshold >6; 4-6 = two rows whose
	// Ys shift by the fluid threshold >3; 7+ = the fixed 3x3 at y7/25/43, then at most a
	// three-slot fourth 61-row whose anchoring hugs the grid's LAST column on BOTH sides,
	// and no slot past the 12th is ever drawn — the switch has no case rendering one).
	// Fluids: the NEI :389-390 bottom rows.
	// -----------------------------------------------------------------------

	/**
	 * Item-input slot {x,y} (the NEI :170-275 switch). {@code null} = no slot at this
	 * index (count 0, past the declared count, or past the 12th drawn slot).
	 */
	public static int[] inputPos(int aIndex, RecipeMap aMap) {
		return itemPos(aIndex, aMap.mInputItemsCount, aMap.mInputFluidCount, 17, true);
	}

	/** Item-output slot {x,y} (the NEI :282-386 switch, mirrored anchor 107 — LEFT-anchored). */
	public static int[] outputPos(int aIndex, RecipeMap aMap) {
		return itemPos(aIndex, aMap.mOutputItemsCount, aMap.mOutputFluidCount, 107, false);
	}

	/**
	 * The switch translation. One asymmetry the upstream switch encodes and the tests
	 * pin: the INPUT rows are RIGHT-anchored (1 in at x53, 2 at 35/53, the 4-slot shape
	 * the {35,53} pair) while the OUTPUT rows are LEFT-anchored (1 out at x107, 2 at
	 * 107/125, the 4-slot shape the {107,125} pair) — the two sides grow away from the
	 * progress arrow in the middle.
	 */
	private static int[] itemPos(int aIndex, int aItemCount, int aFluidCount, int aAnchorX, boolean aRightAnchored) {
		if (aIndex < 0 || aIndex >= aItemCount) return null;
		if (aItemCount <= 3) {
			int tCol = aRightAnchored ? aIndex + 3 - aItemCount : aIndex;
			return new int[] {aAnchorX + 18 * tCol, aFluidCount > 6 ? 7 : 25};
		}
		if (aItemCount <= 6) {
			return new int[] {aAnchorX + 18 * twoRowCol(aIndex, aItemCount, aRightAnchored), twoRowY(aIndex < 3 ? 0 : 1, aFluidCount)};
		}
		// 7+ → the fixed 3x3 at y7/25/43, then the upstream fourth 61-row: at most three
		// slots, RIGHT-hugging on BOTH grids — upstream case 10 hangs its 10th slot on the
		// grid's LAST column (inputs x53, NEI_RecipeMap.java:246; outputs x143, :358), case
		// 11 fills {last-1, last} (:258-259 / :370-371), the default fills the full row.
		// And the switch draws at most 12 slots per side (no case renders a 13th) — the
		// batch-2 ruling pins this whole tail DEAD on the live census (no map declares
		// 10/11/13+ item slots either side), so it exists purely as the faithful
		// transcription the day a map outgrows the census.
		if (aIndex >= 12) return null;
		if (aIndex < 9) return new int[] {aAnchorX + 18 * (aIndex % 3), 7 + 18 * (aIndex / 3)};
		return new int[] {aAnchorX + 18 * (aIndex - 9 + Math.max(0, 12 - aItemCount)), 61};
	}

	/** The 4-6-slot two-row column shapes: 4 = {1,2}|{0,1}, 5 = {0,1,2}/{1,2}|{0,1,2}/{0,1}, 6 = full. */
	private static int twoRowCol(int aIndex, int aItemCount, boolean aRightAnchored) {
		int tLead = aRightAnchored ? 1 : 0;
		if (aItemCount == 4) return tLead + aIndex % 2;
		if (aItemCount == 5) return aIndex < 3 ? aIndex : tLead + (aIndex - 3);
		return aIndex % 3; // 6
	}

	/** The 4-6-slot row Ys: >3 fluids → {7,25}, else {16,34} (the NEI ternaries). */
	private static int twoRowY(int aRowIndex, int aFluidCount) {
		if (aFluidCount > 3) return aRowIndex == 0 ? 7 : 25;
		return aRowIndex == 0 ? 16 : 34;
	}

	/** Fluid-input slot {x,y} — NEI :389: {@code 53 - (i%3)*18, 63 - (i/3)*18}. */
	public static int[] fluidInputPos(int aIndex) {
		return new int[] {53 - aIndex % 3 * 18, 63 - aIndex / 3 * 18};
	}

	/** Fluid-output slot {x,y} — NEI :390: {@code 107 + (i%3)*18, 63 - (i/3)*18}. */
	public static int[] fluidOutputPos(int aIndex) {
		return new int[] {107 + aIndex % 3 * 18, 63 - aIndex / 3 * 18};
	}

	/**
	 * The text-band first Y. Upstream drew the cost lines at FIXED y73..123 over the GUI
	 * art (:680-717) — colliding with the y63 fluid row whenever the map carries fluids.
	 * The viewers carry no machine GUI texture, so the band shifts +10 (y83) on
	 * fluid-bearing maps — the one geometry deviation, declared here.
	 */
	public static int textBaseY(RecipeMap aMap) {
		return aMap.mInputFluidCount > 0 || aMap.mOutputFluidCount > 0 ? 83 : 73;
	}

	// -----------------------------------------------------------------------
	// The cost/tier/time/special text — NEI_RecipeMap.drawExtras :680-717, verbatim
	// arithmetic (UT.Code.makeString folds to plain long-to-string).
	// -----------------------------------------------------------------------

	/** The drawExtras line list, top-down (the viewers draw them at TEXT_LINE_HEIGHT pitch). */
	public static List<String> costLines(RecipeMap aMap, Recipe aRecipe) {
		List<String> rLines = new ArrayList<>();
		MapMeta tMeta = metaOf(aMap);
		long tGUt = aRecipe.mEUt;
		long tDuration = aRecipe.mDuration;
		if (tGUt == 0) {
			if (tMeta.showVoltageAmperage()) rLines.add("Tier: unspecified");
		} else if (tGUt > 0) {
			rLines.add("Costs: " + tGUt * tDuration + " GU");
			if (tMeta.showVoltageAmperage()) {
				if (!tMeta.combinePower()) rLines.add("Usage: " + tGUt + " GU/t");
				rLines.add("Tier: " + tGUt / aMap.mPower + " GU");
				rLines.add("Power: " + aMap.mPower);
			} else if (tGUt != 1 && !tMeta.combinePower()) {
				rLines.add("Usage: " + tGUt + " GU/t");
			}
		} else {
			tGUt *= -1;
			rLines.add("Gain: " + tGUt * tDuration + " GU");
			if (tMeta.showVoltageAmperage()) {
				if (!tMeta.combinePower()) rLines.add("Output: " + tGUt + " GU/t");
				rLines.add("Tier: " + tGUt / aMap.mPower + " GU");
				rLines.add("Power: " + aMap.mPower);
			} else if (tGUt != 1 && !tMeta.combinePower()) {
				rLines.add("Output: " + tGUt + " GU/t");
			}
		}
		if (tDuration > 0) rLines.add(timeLine(tDuration));
		if (!tMeta.specialValuePre().isEmpty() || !tMeta.specialValuePost().isEmpty())
			rLines.add(tMeta.specialValuePre() + aRecipe.mSpecialValue * tMeta.specialValueMultiplier() + tMeta.specialValuePost());
		return rLines;
	}

	/** NEI :714: {@code <1200 ticks, <36000 secs, else mins} (the 20 tps / 1200-per-min folds). */
	public static String timeLine(long aDuration) {
		if (aDuration < 1200) return "Time: " + aDuration + " ticks";
		if (aDuration < 36000) return "Time: " + aDuration / 20 + " secs";
		return "Time: " + aDuration / 1200 + " mins";
	}

	/**
	 * NEI :662-666: the chance tooltip of one output slot — shown when the chance is
	 * strictly between 0 and the folded max 10000; the percentage prints as two decimals
	 * (the {@code (tChance/100) "." padded (tChance%100) "%"} arithmetic verbatim), with
	 * " each" when the stack is larger than one. {@code null} = no tooltip.
	 */
	public static String chanceLine(long aChance10000, int aStackSize) {
		if (aChance10000 <= 0 || aChance10000 >= 10000) return null;
		long tFraction = aChance10000 % 100;
		return "Chance: " + aChance10000 / 100 + "." + (tFraction < 10 ? "0" : "") + tFraction + "%" + (aStackSize > 1 ? " each" : "");
	}

	/**
	 * The per-output chance read with the port's folded-max semantics (Recipe.mChances:
	 * null/short array reads 10000 past its end — Recipe.outputChance's public mirror;
	 * upstream getMaxChance folds to the 10000 constant).
	 */
	public static long outputChance(Recipe aRecipe, int aOutputIndex) {
		if (aRecipe.mChances == null || aOutputIndex < 0 || aOutputIndex >= aRecipe.mChances.length) return 10000;
		return aRecipe.mChances[aOutputIndex];
	}

	/**
	 * The not-consumed predicate for one INPUT slot: the port of the upstream
	 * stack-size-0 marker (NEI :671 {@code tStack.item.stackSize == 0}) — size-0 is not
	 * portable to 1.20.1, the port carries the never-consumed identity at count 1 behind
	 * {@link Recipe#sNotConsumable} (the circuit/mold/USB/blade disjunctions, Recipe.java
	 * field doc), so the tooltip rides the same predicate.
	 */
	public static boolean notConsumable(net.minecraft.world.item.ItemStack aInput) {
		return Recipe.sNotConsumable.test(aInput);
	}
}
