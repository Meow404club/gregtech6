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

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

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
 * (compressor/extruder inverse pairs like plate↔ingot) terminate.
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

	/** One cross-material output of a row that consumes material {@code from} (card B's display face). */
	public record ByproductEdge(OreDictMaterial from, OreDictMaterial to, OreDictPrefix outPrefix, String mapName) {}

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
						.add(new ByproductEdge(tInMaterial.get(i), tPair.material, tPair.prefix, aMapName));
				}
			}
		}
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
		Set<ChainEdge> tEdges = mChainIndex.get(aMaterial);
		if (tEdges == null || tEdges.isEmpty()) return Set.of();
		// adjacency: from-prefix -> to-prefixes (built per query from the indexed edges only)
		Map<OreDictPrefix, Set<OreDictPrefix>> tAdjacency = new LinkedHashMap<>();
		Set<OreDictPrefix> tStarts = new LinkedHashSet<>();
		for (ChainEdge tEdge : tEdges) {
			tAdjacency.computeIfAbsent(tEdge.from(), k -> new LinkedHashSet<>()).add(tEdge.to());
			if (isOreFamilyPrefix(tEdge.from())) tStarts.add(tEdge.from());
		}
		Set<OreDictPrefix> rVisited = new LinkedHashSet<>(tStarts);
		Deque<OreDictPrefix> tQueue = new ArrayDeque<>(tStarts);
		while (!tQueue.isEmpty()) {
			Set<OreDictPrefix> tNext = tAdjacency.get(tQueue.poll());
			if (tNext == null) continue;
			for (OreDictPrefix tPrefix : tNext) {
				if (rVisited.add(tPrefix)) tQueue.add(tPrefix); // visited-on-prefix — the cycle break
			}
		}
		return Collections.unmodifiableSet(rVisited);
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
