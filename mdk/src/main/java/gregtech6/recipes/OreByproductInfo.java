/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
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
import java.util.List;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * The DECLARED ore-byproducts face (task debt-byproducts-data, batch-3 data-first card): the
 * material-level {@code mByProducts} data served as an immutable query model for the
 * ore-processing byproducts page (the display card material-tree-b / jei-emi-batch3-show;
 * upstream served this face as the {@code RM.ByProductList} fake-recipe family).
 *
 * <p><b>Upstream semantics</b> (archaeology, GT6 1.7.10): the byproduct RELATION lives on the
 * MATERIAL layer — {@code OreDictMaterial.mByProducts}, a plain ordered {@code List<OreDictMaterial>}
 * with no weights (OreDictMaterial.java:262, upstream ArrayListNoNulls = nulls-only, duplicates
 * allowed), filled by the {@code .ores(...)} setter (OreDictMaterial.java:1239-1242) from the
 * 308-line data table at MT.java:2991+ (this port carries it verbatim at MT.java:3780-4103; the
 * GT_API_Post.java:121-173 mod-compat additions have no third-party mods here and are correctly
 * absent). The fake-recipe face is built by the ore-registration listener
 * (Loader_OreProcessing.java:296-353, attached to the ORE prefix family :196-197): it redirects
 * {@code aMaterial = aMaterial.mTargetCrushing.mMaterial} (:311 — Fe crushes into Fe2O3, so the
 * Iron page is the FE2O3 page), walks {@code aMaterial.mByProducts} (:328), emits outputs as one
 * dust-or-ingot per byproduct (:329, item resolution = display-time concern, not modelled here)
 * and keys ONE row per crushing target (the {@code mAlreadyListedOres} first-wins dedup :306/:336)
 * into {@code RM.ByProductList} (RM.java:154). This model transcribes that face: the entry axis =
 * crushing targets of the ore universe, the entry payload = the verbatim ordered byproduct list.
 *
 * <p><b>Two faces, kept apart on purpose</b> (ruling 2026-09-26-debt-material-tree): this class
 * is the DECLARED face — what upstream says an ore's byproducts ARE. The DERIVED face (what the
 * live recipe rows actually produce) is MaterialTreeBuilder's byproduct edge walk; the display
 * card merges both (GTCEu's one-diagram-two-sources precedent). The declared face never drops a
 * byproduct for lacking a recipe row — upstream semantics preserved as-is.
 *
 * <p><b>Not this card's seam</b>: the PREFIX-level {@code OreDictPrefix.mByProducts} block
 * (upstream OP.java:639-738 — crafting leftovers like stone dust per ore prefix, consumed by the
 * real crushing handler RecipeMapHandlerCrushing.java:127-136) stays dormant; filling it would
 * change live recipe rows (the ore-pool cards' domain, GT6RecipesOreChain.java:66-69 declares the
 * dormancy), not just query data.
 *
 * <p><b>Immutability + no cache</b>: entries are records over {@link List#copyOf} snapshots.
 * {@link #entries()} re-walks the ~618-material ore universe per call (a material-data walk, no
 * recipe rows — the p32 perf red line does not apply; the 56k-row scan is the DERIVED face's
 * problem and it caches). Values follow the live material system, so a fresh
 * {@code GTMaterialItems.initMaterials()} generation is picked up with no reset hook.
 */
public final class OreByproductInfo {

	/**
	 * One declared-face entry: the crushing-target material and its ordered declared byproduct
	 * materials (upstream order preserved, duplicates preserved — ArrayListNoNulls semantics).
	 */
	public record Entry(OreDictMaterial material, List<OreDictMaterial> byproducts) {}

	private OreByproductInfo() {}

	/**
	 * The declared face, ore-walk first-wins order (upstream :336-338): one entry per crushing
	 * target of the ore universe whose byproduct list is non-empty.
	 */
	public static List<Entry> entries() {
		List<Entry> rEntries = new ArrayList<>();
		List<OreDictMaterial> tSeen = new ArrayList<>();
		for (OreDictMaterial tOreMaterial : GT6RecipesOreChain.expandOreMaterials()) {
			OreDictMaterial tTarget = GT6RecipesOreChain.crushingTarget(tOreMaterial); // upstream :311
			if (tTarget.mByProducts.isEmpty() || tSeen.contains(tTarget)) continue; // :336 gate + first-wins dedup
			tSeen.add(tTarget);
			rEntries.add(new Entry(tTarget, List.copyOf(tTarget.mByProducts)));
		}
		return rEntries;
	}

	/**
	 * The declared-face entry of a material (alias-resolved), or {@code null} when it is not a
	 * keyed crushing target or has no declared byproducts — upstream would emit no ByProductList
	 * row for it (e.g. Fe: no page of its own, its ore shows the Fe2O3 page).
	 */
	public static Entry of(OreDictMaterial aMaterial) {
		if (aMaterial == null || aMaterial.mID <= 0) return null;
		OreDictMaterial tResolved = MaterialRegistry.INSTANCE.get(aMaterial);
		if (tResolved == null || tResolved.mID <= 0) return null;
		for (Entry tEntry : entries()) if (tEntry.material() == tResolved) return tEntry;
		return null;
	}
}
