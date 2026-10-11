/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregtech6.recipes.tree;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.tools.TileEntityMold;

/**
 * The material processing tree, data layer only (task debt-material-tree-a, card A of the
 * A/B/C split, ruling 2026-09-26-debt-material-tree): the port's own DYNAMIC derivation of
 * the GTCEu Ore Processing Diagram's data, derived from the live {@link RecipeMap} rows —
 * NOT a hand-drawn static template (GTCEu OreProcessingRecipeWidget.java:26-89 writes a
 * fixed XY coordinate table and GTOreByProduct.java:66-361 hardcodes 34 slots; their chain
 * topology also differs from GT6's — GTCEu crushedRefined/washer/thermal vs GT6
 * crushedCentrifuged/Bath/Drying, so it does not transfer).
 *
 * <p><b>Identity seam</b> (research r-material-tree): every material-scoped item carries its
 * {@code (prefix, material)} pair as public finals — {@link MaterialPrefixItem#prefix}/
 * {@link MaterialPrefixItem#material} (MaterialPrefixItem.java:40-41) and the block-item
 * isomorph {@link GTMaterialPrefixBlockItem#prefix}/{@link GTMaterialPrefixBlockItem#material}
 * — and every row is public on {@link RecipeMap#mRecipeList}. So the tree is a pure scan
 * product; no registration hook is needed.
 *
 * <p><b>Derivation</b> (one sweep over every {@link RecipeMap#RECIPE_MAPS} map × row):
 * <ul>
 * <li>a row whose input is a prefix item of material X and whose output is a prefix item of
 *     the SAME X yields the chain edge {@code (inputPrefix -> outputPrefix)}, labelled with
 *     the producing map's internal name — the ore→crushed→dust spine and every
 *     dust→plate→… tail;</li>
 * <li>a row whose input carries X and whose output carries a DIFFERENT material yields the
 *     byproduct edge {@code X -> Y} (with the output prefix + map label). The display face
 *     of byproducts is card B; this card only builds the edges.</li>
 * </ul>
 *
 * <p><b>Performance red line</b> (the perf-recipe-hash-index pattern): ONE full sweep at
 * {@link #build()} fills a {@code material -> edges} hash index; every per-material query is
 * a hash read that never re-enters the row lists — MIXER alone pours 56k rows and CRUSHER
 * 1643, so a per-material re-scan is forbidden. {@link #scannedRows()} counts the rows the
 * one sweep visited; it never moves afterwards — the structural no-rescan guard the census
 * test pins (a wall-clock assertion would flake, the known_bugs recipemap-hashindex-flaky
 * lesson).
 *
 * <p><b>Traversal</b>: {@link #reachableFromOre} BFS-walks the chain edges of one material
 * starting at the {@code OP.ore*} prefix family (every prefix whose internal name starts
 * with "ore" — ore/oreRaw/orePoor/dense/stone variants), visited-on-prefix, so cycles
 * (compressor/extruder inverse pairs like plate↔ingot) terminate. {@link #depthsFromOre}
 * is the same walk carrying the BFS layer index — the display's depth-band axis (the EMI
 * recipe-tree layering, BoMScreen.java:334/:337 {@code depth * NODE_VERTICAL_SPACING}).
 *
 * <p><b>Dynamic-map synthesis</b> (task mattree-emi-tree-rework, the 「坩埚没了」 root):
 * the stored-row sweep never sees the on-demand maps — {@code gt.recipe.cruciblesmelting}
 * is zero-row by construction (GT6RecipeMapCrucible.findRecipe :179-203 derives the row at
 * lookup time) and the Mold/Faucet casting pipeline is BE-driven (TileEntityMold
 * MOLD_RECIPES + TileEntityFaucet.fillMold), so after the sweep
 * {@link #synthesizeDynamicEdges} folds their derivable families in as ordinary edges:
 * <ul>
 * <li>the crucible SELF arm (target == self): {@code smeltInputPrefix -> ingot} chain
 *     edges labelled {@code gt.recipe.cruciblesmelting} (a live RecipeMap — the census
 *     live-map pin holds);</li>
 * <li>the crucible CROSS arm (ore-mineral materials whose smelting target is another
 *     material, e.g. Fe2O3 → Fe): a byproduct edge into the target's ingot — the terminal
 *     product shows in the byproduct band instead of hijacking the chain axis;</li>
 * <li>the casting family: {@code dust -> <mold prefix>} chain edges labelled
 *     {@code Mold} (the one synthetic label — {@link #SYNTHETIC_MAP_LABELS}, the census
 *     pins exempt it) for every self-solidifying MELTING material, prefixes read off the
 *     {@code TileEntityMold.MOLD_RECIPES} universe through the public lazy fill.</li>
 * </ul>
 * Alloying stays OUT (RM.Combination cross-material dust rows produce non-ore-universe
 * alloys — the X→alloy edge is a byproduct-band face the alloying viewer page owns; a
 * deliberate non-extension, not a silent drop).
 *
	 * <p><b>Purified producers LANDED</b> (task debt-ore-purified-edge): the upstream :351
	 * DUST_ORE sifting walk (GT6RecipesSifter.load(), the oreGravel/sand/redsand/mud
	 * ore-block rows) produces the on-axis materials' crushedPurified, so those edges appear
	 * here as ordinary chain edges with zero builder changes; Fe and the other MT setCrushing
	 * redirecters sit OFF the walk's material axis — their crushedPurified is honestly absent
	 * (only the terminal ShCL legs exist for them).
 *
 * <p><b>Card scope</b>: pure data, zero JEI/EMI/UI dependency (display is card B; the
 * workstation hook is card C). No KubeJS face — internal model only.
 */
public final class MaterialTreeBuilder {

	/** One same-material prefix hop inside one recipe map (the tree's chain edges). */
	public record ChainEdge(OreDictPrefix from, OreDictPrefix to, String mapName) {}

	/**
	 * One cross-material output of a row that consumes material {@code from} (card B's display
	 * face). {@code inPrefix} is the row's material-scoped INPUT prefix — the producing step's
	 * band (task mattree-r2-byproduct-per-step: the per-step hang needs the step, which is the
	 * input band, not the output's; the r11-gaps census named this field as the per-step unblock,
	 * {@code scanRow} had the pair at hand).
	 */
	public record ByproductEdge(OreDictMaterial from, OreDictMaterial to, OreDictPrefix inPrefix, OreDictPrefix outPrefix, String mapName) {}

	/** The material -> chain-edge set index (insertion-ordered per material; membership is what callers read). */
	private final Map<OreDictMaterial, Set<ChainEdge>> mChainIndex;
	/** The material -> byproduct-edge set index. */
	private final Map<OreDictMaterial, Set<ByproductEdge>> mByproductIndex;
	/** Rows visited by the ONE build sweep — the no-rescan guard's counter, immutable after build. */
	private final int mScannedRows;

	private MaterialTreeBuilder(int aScannedRows, Map<OreDictMaterial, Set<ChainEdge>> aChain, Map<OreDictMaterial, Set<ByproductEdge>> aByproduct) {
		mScannedRows = aScannedRows;
		mChainIndex = aChain;
		mByproductIndex = aByproduct;
	}

	/**
	 * The one full sweep over every {@code RecipeMap.RECIPE_MAPS} map and row, building the
	 * {@code material -> edges} indexes. Call once per generation (after the registration
	 * pour / on demand from card B); the result is an immutable read-only snapshot.
	 */
	public static MaterialTreeBuilder build() {
		int tScanned = 0;
		Map<OreDictMaterial, Set<ChainEdge>> tChain = new LinkedHashMap<>();
		Map<OreDictMaterial, Set<ByproductEdge>> tByproduct = new LinkedHashMap<>();
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) {
			for (Recipe tRow : tMap.mRecipeList) {
				tScanned++;
				scanRow(tMap.mNameInternal, tRow, tChain, tByproduct);
			}
		}
		synthesizeDynamicEdges(tChain, tByproduct);
		return new MaterialTreeBuilder(tScanned, tChain, tByproduct);
	}

	/** Folds one row's prefix-item legs into the two indexes. */
	private static void scanRow(String aMapName, Recipe aRow, Map<OreDictMaterial, Set<ChainEdge>> aChain, Map<OreDictMaterial, Set<ByproductEdge>> aByproduct) {
		// the row's prefix-item input legs: (prefix, material) identity pairs from the item seam
		java.util.List<OreDictPrefix> tInPrefix = null;
		java.util.List<OreDictMaterial> tInMaterial = null;
		for (ItemStack tStack : aRow.mInputs) {
			if (tStack == null || tStack.isEmpty()) continue;
			Pair tPair = pairOf(tStack.getItem());
			if (tPair == null) continue;
			if (tInPrefix == null) {tInPrefix = new java.util.ArrayList<>(2); tInMaterial = new java.util.ArrayList<>(2);}
			tInPrefix.add(tPair.prefix);
			tInMaterial.add(tPair.material);
		}
		if (tInPrefix == null) return; // no material-scoped input — nothing this card derives from it
		for (ItemStack tStack : aRow.mOutputs) {
			if (tStack == null || tStack.isEmpty()) continue;
			Pair tPair = pairOf(tStack.getItem());
			if (tPair == null) continue;
			for (int i = 0; i < tInPrefix.size(); i++) {
				if (tInMaterial.get(i) == tPair.material) {
					aChain.computeIfAbsent(tPair.material, k -> new LinkedHashSet<>())
						.add(new ChainEdge(tInPrefix.get(i), tPair.prefix, aMapName));
				} else {
						aByproduct.computeIfAbsent(tInMaterial.get(i), k -> new LinkedHashSet<>())
							.add(new ByproductEdge(tInMaterial.get(i), tPair.material, tInPrefix.get(i), tPair.prefix, aMapName));
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// the dynamic-map synthesis (the 「坩埚没了」 root fix)
	// ------------------------------------------------------------------

	/** The crucible smelting map's internal name (GT6RecipeMaps :1193, a live zero-row map). */
	public static final String CRUCIBLE_SMELTING_MAP = "gt.recipe.cruciblesmelting";

	/**
	 * The one synthetic edge label: the Mold/Faucet casting pipeline has no RecipeMap behind
	 * it (TileEntityMold BE-driven, TileEntityFaucet.fillMold the pour arm), so its edges
	 * carry this literal — {@link MaterialTreeDisplay#mapLabel} passes it through and the
	 * census pins exempt it through this set (the live-map rule keeps its teeth for rows).
	 */
	public static final Set<String> SYNTHETIC_MAP_LABELS = Set.of("Mold");

	/**
	 * Folds the on-demand maps' derivable families into the two indexes: the crucible
	 * smelting self/cross arms and the Mold casting family, over every registered
	 * non-hidden material that passes the crucible-food gate (MELTING tag + a positive
	 * smelting target — GT6RecipeMapCrucible :190-191). Zero row re-scans: this walks the
	 * MATERIAL registry only, the performance red line holds.
	 */
	private static void synthesizeDynamicEdges(Map<OreDictMaterial, Set<ChainEdge>> aChain, Map<OreDictMaterial, Set<ByproductEdge>> aByproduct) {
		OreDictPrefix[] tSmeltInputs = gregtech6.recipes.maps.GT6RecipeMapCrucible.selfPrefixes();
		for (OreDictMaterial tMat : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
			if (tMat == null || tMat.mID <= 0 || tMat.mHidden) continue;
			// the crucible-food gate (GT6RecipeMapCrucible :190-191) — also the melt-feed gate
			// of the casting family (a material that cannot melt cannot pour into a mold)
			if (!tMat.contains(TD.Processing.MELTING) || tMat.mTargetSmelting.mAmount <= 0) continue;
			OreDictMaterial tTarget = tMat.mTargetSmelting.mMaterial;
			if (tTarget == null || tTarget.mID <= 0) continue; // setSmelting(null, ..) — no row, no edge (the fluorite-helper face)
			for (OreDictPrefix tIn : tSmeltInputs) {
				if (tTarget == tMat) {
					// the SELF arm: dust iron -> ingot iron — an ordinary chain edge (on-axis)
					aChain.computeIfAbsent(tMat, k -> new LinkedHashSet<>())
						.add(new ChainEdge(tIn, OP.ingot, CRUCIBLE_SMELTING_MAP));
				} else {
					// the CROSS arm (the ore-mineral face, e.g. Fe2O3 -> Fe): the terminal
					// product rides the byproduct band instead of hijacking the chain axis
					aByproduct.computeIfAbsent(tMat, k -> new LinkedHashSet<>())
						.add(new ByproductEdge(tMat, tTarget, tIn, OP.ingot, CRUCIBLE_SMELTING_MAP));
				}
			}
			// the casting family: the Mold solidifies the melt into the MOLD_RECIPES shapes
			// (TileEntityMold :540-548, the matStack output) when the material solidifies to
			// itself (the COOL2CRYSTAL cross-material swap stays out, same self-arm rule)
			if (tMat.mTargetSolidifying.mMaterial == tMat) {
				for (OreDictPrefix tPrefix : moldPrefixes()) {
					aChain.computeIfAbsent(tMat, k -> new LinkedHashSet<>())
						.add(new ChainEdge(OP.dust, tPrefix, "Mold"));
				}
			}
		}
	}

	/** The distinct mold output prefixes (the lazy-fill trigger first — the P6 lesson). */
	private static Set<OreDictPrefix> moldPrefixes() {
		TileEntityMold.getMoldRecipe(1); // any non-zero shape warms the LAZY MOLD_RECIPES table
		Set<OreDictPrefix> rPrefixes = new LinkedHashSet<>();
		for (OreDictPrefix tPrefix : TileEntityMold.MOLD_RECIPES.values()) if (tPrefix != null) rPrefixes.add(tPrefix);
		return rPrefixes;
	}

	/** The item seam: (prefix, material) of a material-scoped item, or null for anything else. */
	private static Pair pairOf(Item aItem) {
		if (aItem instanceof MaterialPrefixItem tItem) return new Pair(tItem.prefix, tItem.material);
		if (aItem instanceof GTMaterialPrefixBlockItem tItem) return new Pair(tItem.prefix, tItem.material);
		return null;
	}

	/** Value carrier for {@link #pairOf} (a record over the two finals). */
	private record Pair(OreDictPrefix prefix, OreDictMaterial material) {}

	/** The chain edges of one material (empty for a material no row touches — never null). */
	public Set<ChainEdge> chainEdges(OreDictMaterial aMaterial) {
		Set<ChainEdge> rEdges = mChainIndex.get(aMaterial);
		return rEdges == null ? Set.of() : Collections.unmodifiableSet(rEdges);
	}

	/** The byproduct edges of one material (empty when nothing consumes it into other materials). */
	public Set<ByproductEdge> byproductEdges(OreDictMaterial aMaterial) {
		Set<ByproductEdge> rEdges = mByproductIndex.get(aMaterial);
		return rEdges == null ? Set.of() : Collections.unmodifiableSet(rEdges);
	}

	/**
	 * The BFS face of the tree: every prefix reachable from the {@code OP.ore*} family over
	 * the material's chain edges, visited-on-prefix (cycle-safe — the compressor/extruder
	 * inverse pairs close loops). The start set is the ore-family prefixes that actually
	 * appear as a chain-edge source of this material.
	 */
	public Set<OreDictPrefix> reachableFromOre(OreDictMaterial aMaterial) {
		return depthsFromOre(aMaterial).keySet();
	}

	/**
	 * The same BFS carrying each prefix's LAYER index — the display's depth-band axis. The
	 * layering is the EMI recipe-tree face: a node's children sit one
	 * {@code NODE_VERTICAL_SPACING} row below it (BoMScreen.java:334/:337
	 * {@code depth * NODE_VERTICAL_SPACING}, the addNewNodes recursion's {@code depth + 1}).
	 * Insertion order = BFS layer order (a LinkedHashMap, so the display's row assignment
	 * within a band is the deterministic encounter order). A prefix reached twice lands on
	 * its SHORTEST path depth (the first BFS visit) — a later same-band edge renders as the
	 * horizontal connector, a back-edge (to a shallower prefix) is the display's drop.
	 */
	public Map<OreDictPrefix, Integer> depthsFromOre(OreDictMaterial aMaterial) {
		Set<ChainEdge> tEdges = mChainIndex.get(aMaterial);
		if (tEdges == null || tEdges.isEmpty()) return Map.of();
		// adjacency: from-prefix -> to-prefixes (built per query from the indexed edges only)
		Map<OreDictPrefix, Set<OreDictPrefix>> tAdjacency = new LinkedHashMap<>();
		Set<OreDictPrefix> tStarts = new LinkedHashSet<>();
		for (ChainEdge tEdge : tEdges) {
			tAdjacency.computeIfAbsent(tEdge.from(), k -> new LinkedHashSet<>()).add(tEdge.to());
			if (isOreFamilyPrefix(tEdge.from())) tStarts.add(tEdge.from());
		}
		Map<OreDictPrefix, Integer> rDepths = new LinkedHashMap<>();
		Deque<OreDictPrefix> tQueue = new ArrayDeque<>();
		for (OreDictPrefix tStart : tStarts) {
			rDepths.put(tStart, 0); // visited-on-insert — the cycle break, now carrying the layer
			tQueue.add(tStart);
		}
		while (!tQueue.isEmpty()) {
			OreDictPrefix tCurrent = tQueue.poll();
			int tNextDepth = rDepths.get(tCurrent) + 1;
			Set<OreDictPrefix> tNext = tAdjacency.get(tCurrent);
			if (tNext == null) continue;
			for (OreDictPrefix tPrefix : tNext) {
				if (rDepths.putIfAbsent(tPrefix, tNextDepth) == null) tQueue.add(tPrefix);
			}
		}
		return Collections.unmodifiableMap(rDepths);
	}

	/** The {@code OP.ore*} family test: the prefix's internal name starts with "ore" (ore/oreRaw/orePoor/oreDense/stone variants). */
	public static boolean isOreFamilyPrefix(OreDictPrefix aPrefix) {
		return aPrefix.mNameInternal.startsWith("ore");
	}

	/** Rows the one build sweep visited — constant afterwards; the census test's no-rescan structural guard. */
	public int scannedRows() {
		return mScannedRows;
	}

	/** Materials with at least one chain edge (census sanity for card B's display walk). */
	public int chainMaterialCount() {
		return mChainIndex.size();
	}

	/** Materials with at least one byproduct edge. */
	public int byproductMaterialCount() {
		return mByproductIndex.size();
	}
}
