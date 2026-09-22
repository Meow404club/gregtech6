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
//? if kjs {
package gregtech6.integration.kjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.ItemStack;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GT6Kitchen;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The KubeJS-independent core of the KJS binding module (task p34-kjs-bindings).
 * EVERY method here is safe to call with KubeJS absent from the classpath — this
 * class imports no {@code dev.latvian.mods} type. The KubeJS-facing halves are:
 * <ul>
 * <li>{@link GT6KubeJSPlugin} — the only file importing KubeJS; its hook set IS the
 * dual-leg fork (6.x {@code extends KubeJSPlugin} + {@code BindingsEvent} +
 * {@code registerClasses(ScriptType, ClassFilter)}; 7.x {@code implements KubeJSPlugin}
 * + {@code BindingRegistry} + {@code registerClasses(ClassFilter)} — both verified
 * against the harvested KubeJS 2001/2101 branch sources, dev.latvian.mods.kubejs
 * (KubeJS|plugin).KubeJSPlugin + script/{BindingsEvent,BindingRegistry}).</li>
 * <li>{@link GT6Recipes} / {@link GT6RowBuilder} — the script-facing facade; typed
 * ItemStack/FluidStack parameters let Rhino's type wrappers convert script values
 * (KubeJS 6 BuiltinKubeJSPlugin.java:463 registers the ItemStack wrapper, the forge
 * plugin :55 the FluidStack wrapper; KubeJS 7 BuiltinKubeJSPlugin.java:552 the
 * FluidStack wrapper) — no KubeJS import needed on the Java side.</li>
 * </ul>
 *
 * <p><b>Declared deviation from the task card's original spec ③ (ruling
 * 2026-09-22, state key tasks.p34-kjs-bindings.rewrite):</b> the KubeJS recipe-SCHEMA
 * face is NOT used. Both legs hard-require a vanilla {@code RecipeSerializer} at
 * script-invocation time — 6.x RecipeTypeFunction.java:53 and 7.x RecipeTypeFunction
 * .createRecipe call {@code schemaType.getSerializer()} unconditionally, and both
 * legs' RecipeSchemaType.getSerializer() throw {@code "Serializer for type X is not
 * found!"} for ids without a registered vanilla serializer (harvested
 * kubejs-2001-src / kubejs-2101-src sources). GT6 RecipeMap rows are NOT vanilla
 * recipes and carry no serializer (nor may one be registered — registration-face
 * red line). GTCEu's schema route works only because every GTRecipeType carries a
 * GTRecipeSerializer. The row-write face below is the capability-equivalent
 * replacement: script rows add/remove on the SAME RecipeMap.addRecipe single funnel,
 * under the SAME FROZEN gate (rm-phase-gate compliance).
 *
	 * <p><b>Row lifecycle semantics (declared):</b> rows pour at script-run time through
	 * {@link RecipeMap#addRecipe} — legal while the generation phase is OPEN (boot-time
	 * script run, before ServerStarted freezes; offline tests). A runtime {@code /reload}
	 * re-run lands FROZEN: {@link #addRow} catches the gate's IllegalStateException, logs
	 * a WARN and keeps the existing rows — restart-to-apply semantics, no reload crash.
	 * Hot row changes remain the tier-b datapack seam's job (GT6RecipeMapJsonLoader +
	 * the reopenWindow mechanism, gregtech6.recipes package-private). Script RE-RUNS are
	 * idempotent per JVM: {@link #sAddedRows} tracks every row this module added per map
	 * and removes them before re-adding, so a boot-window script re-run cannot double-pour.
	 * Removal writes {@code RecipeMap.mRecipeList} directly (public final collection) —
	 * the declared direct-write seam pattern of GT6CokeOvenTagListener: the findRecipe
	 * membership probe (RecipeMap.firstMatch) never serves a removed row and the size
	 * drift triggers rebuildIndex, so NO explicit index invalidation is needed or
	 * available (invalidateIndex is gregtech6.recipes package-private — this module
	 * keeps ZERO diffs to the recipes package, the read-only-binding red line).
	 */
	public final class GT6KJS {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Lock serializing pours: KubeJS finalizes added recipes on a parallel stream (6.x RecipesEventJS.createRecipe map building), and addRecipe funnels through the non-thread-safe HashSet. KJS rows are few — one global lock is the whole story. */
	private static final Object S_POUR_LOCK = new Object();

	/** Rows this module added per map name — the re-run idempotence ledger (see class doc). ponytail: identity-set keyed by map name holds dead-generation refs after a test reset; harmless (bounded by script row count), purge only if that grows. */
	private static final Map<String, Set<Recipe>> sAddedRows = new ConcurrentHashMap<>();

	private GT6KJS() {}

	/**
	 * The bindings census — every top-level script binding this module registers,
	 * in registration order. Read-only faces: the RM registry trio, the material
	 * item/block families and the named registration families of the P33-deferred
	 * KJS notes (GT6Bumbles/Kitchen/Distillation/Anvils/Sensors). Values are Class
	 * objects (static access) except the {@link GT6Recipes} facade, which is bound
	 * as an instance (its row-builder methods are instance-fluent).
	 */
	public static Map<String, Object> bindingClasses() {
		Map<String, Object> rBindings = new LinkedHashMap<>();
		// The row-write face (instance facade — fluent builder methods; the plugin binds
		// the same singleton the tests drive).
		rBindings.put("GT6Recipes", GT6Recipes.instance());
		// The RM runtime recipe maps: 60+ map fields, scripts read map.mRecipeList/etc.
		rBindings.put("GT6RecipeMaps", GT6RecipeMaps.class);
		rBindings.put("RecipeMap", RecipeMap.class);
		// Material families (the card's "GT6Materials/GTMaterialItems registerBindings";
		// the port's material table is gregapi.data.MT — bound under its port name).
		rBindings.put("MT", gregapi.data.MT.class);
		rBindings.put("GTMaterialItems", GTMaterialItems.class);
		rBindings.put("GTMaterialBlocks", GTMaterialBlocks.class);
		// Registration families (RegistryInfo read-only face of the card spec: bound
		// classes expose their RegistryObject statics; NO builder registration — the
		// registries themselves stay mod-owned, zero semantic diff).
		rBindings.put("GT6Bumbles", gregtech6.items.bees.GT6Bumbles.class);
		rBindings.put("GT6Kitchen", GT6Kitchen.class);
		rBindings.put("GT6Distillation", GT6Distillation.class);
		rBindings.put("GT6Anvils", GT6Anvils.class);
		rBindings.put("GT6Sensors", GT6Sensors.class);
		rBindings.put("GTMachines", gregtech6.registry.GTMachines.class);
		return rBindings;
	}

	/**
	 * The class-filter allow prefixes for {@code registerClasses} — scripts may import
	 * the whole port + gregapi trees (GTCEu GregTechKubeJSPlugin.java:226-232 shape;
	 * the internal-package deny arms have no counterpart here — gregapi/gregtech6 carry
	 * no network/core split worth guarding).
	 */
	public static String[] classFilterPrefixes() {
		return new String[] {"gregtech6", "gregapi"};
	}

	/**
	 * The row-write seam's coverage census: the RecipeMap registry key snapshot the
	 * facade pours into. The generalized write path keys EVERY map generically through
	 * {@link RecipeMap#RECIPE_MAPS} — this method IS the "schema count == RECIPE_MAPS
	 * count" pin of the original spec, restated for the bindings face (ruling
	 * 2026-09-22): the seam covers exactly the registry, no per-map handwriting.
	 */
	public static List<String> recipeMapTargets() {
		synchronized (RecipeMap.RECIPE_MAPS) {
			return new ArrayList<>(RecipeMap.RECIPE_MAPS.keySet());
		}
	}

	/** Opens a row builder for the named map ({@link RecipeMap#RECIPE_MAPS} key, e.g. {@code "gt.recipe.shredder"}). Null = unknown map (script typo surfaces as a null-guard failure at add()). */
	public static GT6RowBuilder newRow(String aMapName) {
		RecipeMap tMap = map(aMapName);
		return tMap == null ? null : new GT6RowBuilder(tMap);
	}

	/** The live map for a registry key, or null. */
	public static RecipeMap map(String aMapName) {
		return RecipeMap.RECIPE_MAPS.get(aMapName);
	}

	/**
	 * Pours one row: build via the builder, remove this module's previous rows for the
	 * map (re-run idempotence), then {@link RecipeMap#addRecipe} (the single funnel —
	 * its ghost-recipe guard, dedup and FROZEN gate all apply). FROZEN → WARN + false,
	 * rows kept (the restart-to-apply semantics, class doc).
	 *
	 * @return true = row poured; false = rejected (frozen / unknown map / ghost row).
	 */
	public static boolean addRow(GT6RowBuilder aRow) {
		if (aRow == null || !aRow.validate()) return false;
		RecipeMap tMap = aRow.mMap;
		Recipe tRecipe = aRow.build();
		synchronized (S_POUR_LOCK) {
			removeTracked(tMap);
			try {
				if (tMap.addRecipe(tRecipe) == null) return false; // ghost row (no inputs) — the funnel's silent rejection
			} catch (IllegalStateException tFrozen) {
				LOGGER.warn("GT6 KJS: row rejected — {}", tFrozen.getMessage());
				LOGGER.warn("GT6 KJS: script rows apply at server boot; a runtime /reload keeps the existing rows (restart to apply; hot changes = the gt6:recipe_maps datapack seam)");
				return false;
			}
			sAddedRows.computeIfAbsent(tMap.mNameInternal, k -> ConcurrentHashMap.newKeySet()).add(tRecipe);
		}
		return true;
	}

	/**
	 * Removes every row of the named map matching the predicate — the script "删/改"
	 * half (modify = remove + re-add). Writes {@code mRecipeList} directly (public
	 * final collection): the GT6CokeOvenTagListener declared direct-write seam; the
	 * findRecipe membership probe blocks removed rows and the size drift triggers
	 * rebuildIndex (RecipeMap.firstMatch/findByIndex), no explicit invalidation.
	 * Removal is a read-path repair, not a pour — legal in the FROZEN phase (same as
	 * the tag listener's runtime /reload subset replace). Rows this module added
	 * itself are ALSO untracked, so a later addRow cannot resurrect them.
	 *
	 * @return how many rows went away.
	 */
	public static int removeRows(String aMapName, Predicate<Recipe> aFilter) {
		RecipeMap tMap = map(aMapName);
		if (tMap == null || aFilter == null) return 0;
		synchronized (S_POUR_LOCK) {
			Set<Recipe> tTracked = sAddedRows.get(aMapName);
			List<Recipe> tDoomed = new ArrayList<>();
			for (Recipe tRecipe : tMap.mRecipeList) if (aFilter.test(tRecipe)) tDoomed.add(tRecipe);
			tMap.mRecipeList.removeAll(tDoomed);
			if (tTracked != null) tTracked.removeAll(tDoomed);
			return tDoomed.size();
		}
	}

	/** Drops this module's tracked rows for the map from its list (the re-run half of addRow). */
	private static void removeTracked(RecipeMap aMap) {
		Set<Recipe> tTracked = sAddedRows.get(aMap.mNameInternal);
		if (tTracked == null || tTracked.isEmpty()) return;
		aMap.mRecipeList.removeAll(tTracked);
		tTracked.clear();
	}

	/**
	 * Test census: the per-map tracked-row ledger (package-visible read-only copy) —
	 * pins that the idempotence ledger stays in step with what actually poured.
	 */
	static Map<String, Set<Recipe>> addedRowsLedger() {
		Map<String, Set<Recipe>> rCopy = new LinkedHashMap<>();
		sAddedRows.forEach((k, v) -> rCopy.put(k, Set.copyOf(v)));
		return rCopy;
	}

	/** Test reset seam: drops the whole ledger (the GT6RecipeMaps.reset generation rewind counterpart — the tracker must never outlive its maps in a test JVM). */
	static void resetLedger() {
		sAddedRows.clear();
	}

	/** Visible-for-testing row assembly (the builder is the spec; this is its product readback). */
	static Recipe[] ledgerRows(String aMapName) {
		Set<Recipe> tRows = sAddedRows.get(aMapName);
		return tRows == null ? new Recipe[0] : tRows.toArray(new Recipe[0]);
	}

	/** Array helper the builder uses (varargs null-tolerant trim, the Recipe ctor convention). */
	static ItemStack[] trimNulls(ItemStack[] aArray) {
		if (aArray == null) return new ItemStack[0];
		int tEnd = aArray.length;
		while (tEnd > 0 && (aArray[tEnd - 1] == null || aArray[tEnd - 1].isEmpty())) tEnd--;
		return tEnd == aArray.length ? aArray : Arrays.copyOf(aArray, tEnd);
	}
}
//?}
