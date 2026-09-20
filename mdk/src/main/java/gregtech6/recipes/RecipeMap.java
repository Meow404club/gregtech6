/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.item.MaterialPrefixItem;

/**
 * GT6 RecipeMap shell, minimal port of the inner class upstream
 * gregapi/recipes/Recipe.java:49-140.
 *
 * <p>Trimmed against upstream (documented deviations for the Furnace-level port):
 * <ul>
 * <li>the upstream findRecipeInternal hash indexes (Recipe.java:469-551) are restored
 * (task p32-perf-recipe-hash-index): {@code mRecipeItemMap} → the item/tag indexes and
 * {@code mRecipeFluidMap} → the fluid index below, plus the map-level {@code oRecipe}
 * last-recipe buffer (:488). Modern key difference: the 1.7.10 ItemStackContainer key
 * (item+damage+NBT) folds into the {@link Item} identity (the exact-branch NBT routing
 * stays inside {@code Recipe.checkStacksEqual}), and the ore-dict unification leg
 * (upstream queries the unified stack tStack2) is keyed the p25 way — the RECIPE input's
 * material-family tag bucket, swept with {@link Recipe#sTagTest} at query time;</li>
 * <li>no mMinInputTankSizes (the minimal-input gates live at the machine callers,
 * TileEntityBasicMachine.java:708-710); no RecipeMapHandlers (:526-542 — the P8 ruling
 * keeps handler expansion at registration), no config file, no NEI fields, no GUIPath
 * registration into the localizer;</li>
 * <li>{@code RECIPE_MAPS} gained a {@link #reset()} (P1 registry discipline:
 * registries must be re-initializable per JVM generation — upstream has no reset
 * because 1.7.10 never reloads the mod class space).</li>
 * </ul>
 */
public class RecipeMap {
	/** RecipeMap-HashMap so that Machines can store their corresponding Recipe Lists as String NBT. (upstream :51) */
	public static final Map<String, RecipeMap> RECIPE_MAPS = new HashMap<>();

	/** Port-only reset hook (P1 registry discipline): drops every registered map so a fresh init re-registers cleanly. */
	public static void reset() {
		RECIPE_MAPS.clear();
	}

	/** The List of all Recipes. (upstream :68) */
	public final Collection<Recipe> mRecipeList;
	/** String used as an unlocalised Name. (upstream :72) */
	public final String mNameInternal;
	/** String used as a localised Name. (upstream :74) */
	public final String mNameLocal, mNameLocalUnderscored;
	/** String used for the Recipe Lists; if null it falls back to the unlocalised Name. (upstream :76/:123) */
	public final String mNameNEI;
	/** GUI used for the Recipe Display, usually the GUI of the Machine itself, auto-attaches ".png". (upstream :78/:124) */
	public final String mGUIPath;
	public final byte mProgressBarDirection, mProgressBarAmount;
	/** Slot constants (upstream :81). */
	public final int mInputItemsCount, mOutputItemsCount, mInputFluidCount, mOutputFluidCount, mMinimalInputItems, mMinimalInputFluids, mMinimalInputs;
	public final long mPower;

	// ---- the findRecipe hash indexes (task p32-perf-recipe-hash-index, upstream Recipe.java:469-551) ----
	// Phase accounting (rm-phase-gate contract): the indexes are maintained INCREMENTALLY by
	// addRecipe — the single pour funnel, legal only while the generation phase is OPEN — so a
	// FROZEN generation has a settled index. The two declared direct-write seams (the JSON
	// reload removeAll, GT6RecipeMapJsonLoader.java:349, and the CokeOven tag listener,
	// GT6CokeOvenTagListener) bypass the funnel; like upstream's MineTweaker detect/reInit
	// (:491-494) the read path self-heals on a size mismatch and the probe verifies list
	// membership, so a removed row can never be served from a stale bucket. The listener's
	// subset replace can additionally be SIZE-NEUTRAL (runtime /reload, unchanged
	// #minecraft:logs) — it calls {@link #invalidateIndex()} explicitly (the P1 fix).
	// Single-server-thread assumption, identical to upstream (:543 catches the concurrent case;
	// the port keeps the machine-tick/reload serialization the legs already run under).
	/** Exact-item buckets: a row lands under the Item of every non-empty mInputs entry (upstream addToItemMap :632-640, key folded item+meta+NBT → Item). */
	private final Map<Item, List<Recipe>> mItemIndex = new HashMap<>();
	/** Material-family-tag buckets (the p25 fallback leg, Recipe.matchesByMaterialTag): a row whose input is a family-bearing MaterialPrefixItem also lands under {@code <family>/<material>}. */
	private final Map<TagKey<Item>, List<Recipe>> mTagIndex = new HashMap<>();
	/** Fluid buckets (upstream mRecipeFluidMap keyed by fluid name — here by the Fluid registry object, isFluidEqual's necessary condition). */
	private final Map<Fluid, List<Recipe>> mFluidIndex = new HashMap<>();
	/** Rows no leg could bucket (e.g. an all-null mFluidInputs array slipping the ghost guard) — always probed, keeps the index an over-approximation of mRecipeList. */
	private final List<Recipe> mKeylessRecipes = new ArrayList<>();
	/** mRecipeList.size() at the last index maintenance; {@code -1} = never built → the next lookup rebuilds. */
	private int mIndexedSize = -1;
	/** The map-level last-recipe buffer (upstream Recipe.java oRecipe, set at :487/:501, probed at :488). */
	Recipe oRecipe;

	public RecipeMap(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI, long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath, long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems, long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		mRecipeList = (aRecipeList == null ? new HashSet<>() : aRecipeList);
		mNameInternal = aNameInternal;
		mNameLocal = aNameLocal;
		StringBuilder tBuilder = new StringBuilder(mNameLocal.length());
		for (char tChar : mNameLocal.toCharArray()) {
			if (tChar == '(' || tChar == ')' || tChar == '[' || tChar == ']' || tChar == '{' || tChar == '}' || tChar == '"' || tChar == '\'' || tChar == '<' || tChar == '>' || tChar == '°' || tChar == '~' || tChar == '$' || tChar == '%' || tChar == '#' || tChar == '+' || tChar == '*' || tChar == '§' || tChar == '!' || tChar == '?' || tChar == '.' || tChar == ',' || tChar == ':' || tChar == ';') continue;
			if (tChar == ' ' || tChar == '-' || tChar == '=' || tChar == '&' || tChar == '^' || tChar == '|' || tChar == '/' || tChar == '\\') tBuilder.append('_'); else tBuilder.append(tChar);
		}
		mNameLocalUnderscored = tBuilder.toString();
		mNameNEI = aNameNEI == null ? mNameInternal : aNameNEI;
		mGUIPath = aNEIGUIPath.endsWith(".png") ? aNEIGUIPath : aNEIGUIPath + ".png";
		mProgressBarDirection = (byte)aProgressBarDirection;
		mProgressBarAmount = (byte)aProgressBarAmount;
		mPower = aPower;
		mMinimalInputItems = (int)aMinimalInputItems;
		mInputItemsCount = (int)Math.max(aInputItemsCount, mMinimalInputItems);
		mOutputItemsCount = (int)aOutputItemsCount;
		mMinimalInputFluids = (int)aMinimalInputFluids;
		mInputFluidCount = (int)Math.max(aInputFluidCount, mMinimalInputFluids);
		mOutputFluidCount = (int)aOutputFluidCount;
		mMinimalInputs = (int)aMinimalInputs;
		if (RECIPE_MAPS.containsKey(mNameInternal)) throw new IllegalArgumentException("Recipe Map Name already exists: " + mNameInternal);
		RECIPE_MAPS.put(mNameInternal, this);
	}

	/**
	 * Registers a Recipe into this Map. Upstream Recipe.add() feeds three hash
	 * indexes; the port feeds the same indexes incrementally (task
	 * p32-perf-recipe-hash-index): every pour through this single funnel keeps
	 * {@link #mItemIndex}/{@link #mTagIndex}/{@link #mFluidIndex} in step with
	 * {@code mRecipeList}, so the FROZEN phase inherits a settled index.
	 *
	 * <p><b>Late-pour gate (p32-rm-phase-gate)</b>: once the map generation is FROZEN
	 * ({@link GT6RecipeMaps#freeze()}, live switch = ServerStarted), this is the one place
	 * a late pour fails loud — the {@link IllegalStateException} message carries the map
	 * name and the exception's own stack trace carries the offending caller. Registration
	 * pours (the static loaders at FMLCommonSetup, the JSON reload window) are untouched:
	 * the phase is OPEN for all of them. This is the single funnel — every pour in the repo
	 * routes through here, so one guard covers all maps.
	 *
	 * <p><b>Double-empty guard (p8-recipe-chances-orechain)</b>: a recipe with neither item
	 * nor fluid inputs is REJECTED — not added to {@code mRecipeList}, no exception thrown,
	 * {@code null} returned. Upstream is structurally immune to these ghost recipes at the
	 * index layer: {@code addToItemMap} (upstream Recipe.java:632-640) never buckets a
	 * recipe without inputs, so {@code findRecipeInternal} can never match one. This port
	 * scans {@code mRecipeList} linearly and {@code Recipe.checkStacksEqual} passes vacuously
	 * over an empty {@code mInputs} (Recipe.java:775 upstream — no required inputs = every
	 * caller-side input set matches), so an empty-input row would match EVERY lookup. The
	 * invariant "every stored recipe has at least one input leg" must therefore be enforced
	 * at the registration layer here — the upstream-layering equivalent. Rejections are not
	 * logged here (the map carries no logger; the loaders' skip-and-count pour statistics
	 * cover the audit trail, GT6RecipesCokeOven/GT6RecipesShCL precedent). Returning
	 * {@code null} is compatible with the existing call chain (loaders treat a null
	 * {@code addRecipe} result as a drop, the upstream {@code null != addRecipe(...)}
	 * idiom).
	 */
	@Nullable
	public Recipe addRecipe(@Nullable Recipe aRecipe) {
		if (aRecipe == null) return null;
		if (GT6RecipeMaps.phase() == GT6RecipeMaps.Phase.FROZEN) throw new IllegalStateException( // fail-loud AFTER the gate, fail-silent only for null (not a pour)
				"RecipeMap \"" + mNameInternal + "\" is FROZEN — addRecipe rejected outside the registration phase "
				+ "(pour during registration: the static loaders at FMLCommonSetup or the JSON reload window; "
				+ "the caller stack above names the offender — task p32-rm-phase-gate)");
		if (aRecipe.mInputs.length == 0 && aRecipe.mFluidInputs.length == 0) return null; // ghost-recipe guard, see javadoc
		if (aRecipe.mEnabled && !aRecipe.mFakeRecipe) {
			if (mRecipeList.add(aRecipe)) indexRecipe(aRecipe);
		}
		return aRecipe;
	}

	/** Buckets one stored row (the add-time half of upstream addToItemMap :632-640 plus the p25 tag leg). */
	private void indexRecipe(Recipe aRecipe) {
		boolean tKeyed = false;
		for (ItemStack tInput : aRecipe.mInputs) if (tInput != null && !tInput.isEmpty()) {
			mItemIndex.computeIfAbsent(tInput.getItem(), tKey -> new ArrayList<>(1)).add(aRecipe);
			tKeyed = true;
			if (tInput.getItem() instanceof MaterialPrefixItem tPrefixItem) { // the fallback reach: any stack the family tag admits must route here (Recipe.matchesByMaterialTag)
				String tFamily = GT6ItemTags.itemTagFamily(tPrefixItem.prefix);
				if (tFamily != null) mTagIndex.computeIfAbsent(GT6ItemTags.materialTag(tFamily, tPrefixItem.material), tKey -> new ArrayList<>(1)).add(aRecipe);
			}
		}
		for (FluidStack tFluid : aRecipe.mFluidInputs) if (tFluid != null && !tFluid.isEmpty()) {
			mFluidIndex.computeIfAbsent(tFluid.getFluid(), tKey -> new ArrayList<>(1)).add(aRecipe);
			tKeyed = true;
		}
		if (!tKeyed) mKeylessRecipes.add(aRecipe);
		mIndexedSize = mRecipeList.size();
	}

	/** The read-path self-heal (the upstream :491-494 reInit pattern): a size drift means a declared direct-write seam mutated the list behind the funnel. */
	private void rebuildIndex() {
		mItemIndex.clear();
		mTagIndex.clear();
		mFluidIndex.clear();
		mKeylessRecipes.clear();
		for (Recipe tRecipe : mRecipeList) indexRecipe(tRecipe);
		mIndexedSize = mRecipeList.size();
	}

	/**
	 * Forces the next lookup to rebuild the hash indexes (the P1 fix of task
	 * p32-perf-recipe-hash-index). For the one runtime remove+add seam that can be
	 * SIZE-NEUTRAL: {@code GT6CokeOvenTagListener.replaceLogRecipes} swaps its tag-derived
	 * subset on every /reload (TagsUpdatedEvent.shouldUpdateStaticData fires at runtime too)
	 * — with an unchanged #minecraft:logs that is remove M rows, add M fresh instances, Δ=0,
	 * so the size-drift rebuild never fires and the fresh instances would sit in no bucket
	 * (the probe's contains guard only blocks the inverse: bucketed-but-removed — a COKE_OVEN
	 * log lookup would silently null until restart). Callers invoke this right after their
	 * direct writes; the next findRecipe pays one rebuild.
	 */
	void invalidateIndex() {
		mIndexedSize = -1;
	}

	/**
	 * {@code aSize} = Voltage of the Machine or Long.MAX_VALUE if it has no Voltage;
	 * {@code aSpecialSlot} is the content of the Special Slot (regular Maps ignore it,
	 * upstream findRecipe :463 javadoc).
	 *
	 * <p>LOOKUP ONLY: never consumes the passed inputs. All three legs (the caller buffer,
	 * {@link #oRecipe}, the hash index) probe with {@code isRecipeInputEqual(F, T, ...)}
	 * (upstream :487/:488/:501). The upstream first-match-wins + power-gate-to-null shape
	 * is kept verbatim: the FIRST input-matching candidate decides, and a disabled or
	 * underpowered match returns {@code null} for the whole lookup (upstream :501).
	 *
	 * <p>Upstream findRecipeInternal structure restored (task p32-perf-recipe-hash-index):
	 * the caller-passed buffer (:487), the map-level {@code oRecipe} buffer (:488), the
	 * item/tag hash buckets (:498-516) and the fluid buckets (:519-523). The hash index
	 * over-approximates the linear scan exactly — every stored row is reachable through a
	 * bucket for every input that could match it — so the result equals the old linear
	 * scan for every unambiguous query (pinned by GT6RecipeMapHashIndexTest).
	 */
	@Nullable
	public Recipe findRecipe(@Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		// upstream findRecipeInternal (Recipe.java:findRecipeInternal) gates on the MAP's
		// minimal counts (mMinimalInputItems/mMinimalInputFluids/mMinimalInputs), NOT a hard
		// empty-array null — the zero-item-slot machines (the Coagulator, task
		// p29-w2-hu-tu-piggyback, the FIRST port carrier of an item-slot-free map) look up
		// FLUID-ONLY rows through an empty item array. The per-recipe isRecipeInputEqual
		// probe simply matches nothing for item-bearing rows (the mInputs.length > 0 guard),
		// and the addRecipe ghost guard keeps empty-input rows out of the list — so the
		// lookup stays safe to widen to the fluid-leg form.
		if ((aInputs == null || aInputs.length <= 0) && (aFluids == null || aFluids.length <= 0)) return null;

		// Check the Recipe which has been used last time in order to not have to search for it again, if possible. (upstream :487)
		if (aLastRecipe != null && !aLastRecipe.mFakeRecipe && aLastRecipe.mCanBeBuffered && aLastRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) {
			if (aLastRecipe.mEnabled && absGreaterEqual(aSize * mPower, aLastRecipe.mEUt)) {
				oRecipe = aLastRecipe;
				return aLastRecipe;
			}
			return null;
		}
		// The map-level buffer (upstream :488): a repeat lookup with the same inputs never re-enters the index.
		// Membership is re-verified (the one deviation from upstream :488, which has no check): a
		// row removed behind the map — the JSON reload removeAll, or a test/registry generation
		// clearing the row set — must never be served, exactly like the retired linear scan that
		// could only answer with rows the list still holds. The caller buffer above stays
		// membership-free (upstream :487 — the machine legitimately holds its own last recipe).
		if (oRecipe != null && mRecipeList.contains(oRecipe) && !oRecipe.mFakeRecipe && oRecipe.mCanBeBuffered && oRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) {
			return oRecipe.mEnabled && absGreaterEqual(aSize * mPower, oRecipe.mEUt) ? oRecipe : null;
		}

		return findByIndex(aSize, aFluids, aInputs);
	}

	/** The indexed half of upstream :496-542 (item buckets, the tag sweep for the p25 fallback leg, fluid buckets, keyless residue). */
	@Nullable
	private Recipe findByIndex(long aSize, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		if (mIndexedSize != mRecipeList.size()) rebuildIndex(); // upstream :491-494 reInit shape — see the field javadoc
		Recipe tMatched = null;
		if (aInputs != null) for (int i = 0; i < aInputs.length && tMatched == null; i++) {
			ItemStack tStack = aInputs[i];
			if (tStack != null && !tStack.isEmpty()) tMatched = firstMatch(mItemIndex.get(tStack.getItem()), aFluids, aInputs);
		}
		// The material-tag fallback route (Recipe.matchesByMaterialTag consults exactly these
		// TagKeys, derived from the recipe side): sweep the map's tag buckets with the same
		// sTagTest predicate the probe itself uses — complete by construction, since every
		// row's fallback tag IS a bucket key here.
		// ponytail: the sweep is O(tag buckets of this map) per lookup on the miss path;
		// per-stack holder.tags() intersection drops it to O(query tags) if a profile ever cares.
		if (tMatched == null && aInputs != null) for (ItemStack tStack : aInputs) if (tStack != null && !tStack.isEmpty()) {
			for (Map.Entry<TagKey<Item>, List<Recipe>> tBucket : mTagIndex.entrySet()) {
				if (!Recipe.sTagTest.test(tStack, tBucket.getKey())) continue;
				tMatched = firstMatch(tBucket.getValue(), aFluids, aInputs);
				if (tMatched != null) break;
			}
			if (tMatched != null) break;
		}
		if (tMatched == null && aFluids != null) for (FluidStack tFluid : aFluids) if (tFluid != null && !tFluid.isEmpty()) {
			tMatched = firstMatch(mFluidIndex.get(tFluid.getFluid()), aFluids, aInputs);
			if (tMatched != null) break;
		}
		if (tMatched == null) tMatched = firstMatch(mKeylessRecipes, aFluids, aInputs);
		// The upstream :501 arm verbatim: the FIRST input-matching candidate decides, and a
		// disabled or underpowered match nulls the WHOLE lookup (no rescanning).
		if (tMatched == null) return null;
		if (tMatched.mEnabled && absGreaterEqual(aSize * mPower, tMatched.mEUt)) {
			oRecipe = tMatched;
			return tMatched;
		}
		return null;
	}

	/**
	 * One bucket's input-match walk (the :501 probe half). Membership is re-verified against
	 * {@code mRecipeList} — the JSON reload seam removes rows behind the index,
	 * GT6RecipeMapJsonLoader.java:349, and a removed row must never be served.
	 */
	@Nullable
	private Recipe firstMatch(@Nullable List<Recipe> aBucket, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		if (aBucket == null) return null;
		for (Recipe tRecipe : aBucket) {
			if (tRecipe.mFakeRecipe || !mRecipeList.contains(tRecipe)) continue;
			if (tRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) return tRecipe;
		}
		return null;
	}

	/** Upstream UT.Code.abs_greater_equal (UT.java:1727). */
	protected static boolean absGreaterEqual(long aAmount1, long aAmount2) {
		return Math.abs(aAmount1) >= Math.abs(aAmount2);
	}
}
