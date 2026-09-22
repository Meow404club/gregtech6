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

import java.util.List;
import java.util.function.Predicate;

import gregtech6.recipes.Recipe;

/**
 * The script-facing row facade bound as {@code GT6Recipes} (see
 * {@link GT6KJS#bindingClasses}) — the generalized RM row-write seam. One method
 * pair covers EVERY map in {@code RecipeMap.RECIPE_MAPS} (no per-map handwriting;
 * the census pin lives at {@link GT6KJS#recipeMapTargets}).
 *
 * <p>Script shapes (server scripts, applied at boot; see GT6KJS row lifecycle):
 * <pre>
 * GT6Recipes.map('gt.recipe.shredder')
 *     .inputs('minecraft:stone')
 *     .outputs('minecraft:gravel')
 *     .duration(40).eut(16).add()
 *
 * GT6Recipes.remove('gt.recipe.shredder', r => r.mEUt == 16)
 * GT6Recipes.maps()   // discover the registry keys
 * </pre>
 */
public class GT6Recipes {

	/** The singleton the plugin binds and the offline tests drive (the binding census asserts it rides as the live instance). */
	private static final GT6Recipes INSTANCE = new GT6Recipes();

	/** The bound facade instance. */
	public static GT6Recipes instance() {
		return INSTANCE;
	}

	/** Opens a row builder for the named {@code RecipeMap.RECIPE_MAPS} key. */
	public GT6RowBuilder map(String aMapName) {
		return GT6KJS.newRow(aMapName);
	}

	/**
	 * Removes matching rows (the "删/改" half; modify = remove + re-add).
	 * @return how many rows went away.
	 */
	public int remove(String aMapName, Predicate<Recipe> aFilter) {
		return GT6KJS.removeRows(aMapName, aFilter);
	}

	/** The registry key snapshot ({@link GT6KJS#recipeMapTargets}) — script-side discoverability. */
	public List<String> maps() {
		return GT6KJS.recipeMapTargets();
	}
}
//?}
