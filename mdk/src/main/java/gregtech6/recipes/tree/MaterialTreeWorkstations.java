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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GTMachines;

/**
 * The material-tree workstation seam (task debt-material-tree-c, card C of the A/B/C
 * split, ruling 2026-09-26-debt-material-tree): the machines that appear ON the material
 * tree page become the category's catalysts (JEI {@code registerRecipeCatalysts}) and
 * workstations (EMI {@code addWorkstation}) — the GTCEu registerWorkstation shape
 * (GTEMIPlugin registers machines per recipe category, GTRecipeEMICategory), so clicking
 * a Shredder/Sifter/Anvil/Crusher in either viewer reaches the {@code gt6:material_tree}
 * page.
 *
 * <p><b>Data-driven, not hand-listed</b> (the same anti-static-template ruling as the
 * builder): WHICH machines hang on the category is read off the DISPLAYED edges
 * ({@link MaterialTreeDisplay.Edge#mapNames()} of the assembled displays) — the maps the
 * live rows actually put on the tree — and this class only holds the
 * {@code map internal name -> representative machine item} table. A map joins or leaves
 * the catalyst row set by changing its recipes; a NEW displayed map without a table entry
 * contributes no catalyst (visible as a census-test failure — the deliberate discovery
 * face, not silent drift).
 *
 * <p>The representative is each family's TIER-0 machine (crusher/shredder tier-1 item,
 * the bronze {@code "sifter"}, the {@code "stone_anvil"}): one catalyst per map, the
 * cheapest obtainable machine, the upstream NEI machine-list head semantics. Strictly
 * vanilla + registry imports — viewer-neutral, offline-testable; unbound handles (offline
 * or pre-registry) resolve to EMPTY and drop out (the GT6OreGenInfoLayout.catalystStack
 * degrade shape).
 */
public final class MaterialTreeWorkstations {

	/**
	 * The {@code RecipeMap.mNameInternal -> representative machine item} table. Suppliers
	 * stay LAZY (nothing touches {@code GTMachines}/{@code GT6Anvils} until
	 * {@link #workstationStacks} runs) so merely loading this class stays side-effect free.
	 */
	private static final Map<String, Supplier<ItemStack>> TABLE = new LinkedHashMap<>();
	static {
		TABLE.put("gt.recipe.crusher", () -> resolve(GTMachines.CRUSHER_ITEM));
		TABLE.put("gt.recipe.shredder", () -> resolve(GTMachines.SHREDDER_ITEM));
		TABLE.put("gt.recipe.sifter", () -> resolve(GTMachines.SIFTER_ITEMS_BY_PATH.get("sifter")));
		TABLE.put("gt.recipe.anvil", () -> resolve(GT6Anvils.ITEMS_BY_PATH.get("stone_anvil")));
	}

	/** The table keys (unmodifiable) — the census test pins them against the live displayed set. */
	public static Set<String> tableNames() {
		return Collections.unmodifiableSet(TABLE.keySet());
	}

	/** The map internal names the displays actually show on their chain edges (dedup, unordered). */
	public static Set<String> displayedMapNames(Iterable<MaterialTreeDisplay> aDisplays) {
		Set<String> rNames = new HashSet<>();
		if (aDisplays == null) return rNames;
		for (MaterialTreeDisplay tDisplay : aDisplays)
			for (MaterialTreeDisplay.Edge tEdge : tDisplay.edges()) rNames.addAll(tEdge.mapNames());
		return rNames;
	}

	/**
	 * The catalyst stacks for the maps displayed on the tree, in TABLE order (deterministic
	 * regardless of display walk order), one per tabled map, EMPTY-resolved entries dropped.
	 * Offline (or any pre-registry call) this is empty — the registration sites must not
	 * treat empty as an error, the live client fills the stacks.
	 */
	public static List<ItemStack> workstationStacks(Iterable<MaterialTreeDisplay> aDisplays) {
		Set<String> tDisplayed = displayedMapNames(aDisplays);
		List<ItemStack> rStacks = new ArrayList<>();
		for (Map.Entry<String, Supplier<ItemStack>> tEntry : TABLE.entrySet()) {
			if (!tDisplayed.contains(tEntry.getKey())) continue;
			ItemStack tStack = tEntry.getValue().get();
			if (!tStack.isEmpty()) rStacks.add(tStack);
		}
		return rStacks;
	}

	/**
	 * The ONE map's representative machine stack (task mattree-v2-nodes, the v2 node-graph
	 * machine-icon face): EMPTY when the map has no table row or the handle is unbound — the
	 * degrade face (the edge renders as a plain arrow, the via-label carries the machine
	 * names). Offline this is always EMPTY; the live client resolves (the census test pins
	 * the table against 100% of the displayed mapNames).
	 */
	public static ItemStack workstationStack(String aMapName) {
		Supplier<ItemStack> tSupplier = TABLE.get(aMapName);
		return tSupplier == null ? ItemStack.EMPTY : tSupplier.get();
	}

	/** The one registry-dependent face: present-and-bound handle -> stack, anything else -> EMPTY. */
	//? if forge {
	private static ItemStack resolve(net.minecraftforge.registries.RegistryObject<Item> aHandle) {
		return aHandle != null && aHandle.isPresent() ? new ItemStack(aHandle.get()) : ItemStack.EMPTY;
	}
	//?} else {
	/*private static ItemStack resolve(net.neoforged.neoforge.registries.DeferredHolder<Item, Item> aHandle) {
		return aHandle != null && aHandle.isBound() ? new ItemStack(aHandle.get()) : ItemStack.EMPTY;
	}
	 *///?}

	private MaterialTreeWorkstations() {
	}
}
