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

package gregtech6.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The offline ore-chain pour fixture for the panzoom pins (task mattree-jei-panzoom) — the
 * {@code MaterialTreeDisplayTest} fixture shape hoisted into a helper: the arming fields
 * ({@code sMaterialItemResolver}) are package-private to {@code gregtech6.recipes} while
 * the pins live in {@code gregtech6.jei} (whose widget/handler test faces are package-private
 * THERE) — one helper bridges the two packages without reflection.
 */
public final class MaterialTreePanzoomFixture {

	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	/** Arms the probe resolver into the four ore-chain loaders and pours the chain (the DisplayTest pour). */
	public static void armAndPour(BiFunction<OreDictPrefix, OreDictMaterial, Item> aResolver) {
		GT6RecipeMaps.reset();
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		GT6RecipesOreChain.sMaterialItemResolver = aResolver;
		GT6RecipesShCL.sMaterialItemResolver = aResolver;
		GT6RecipesAnvil.sMaterialItemResolver = aResolver;
		GT6RecipesSifter.sMaterialItemResolver = aResolver;
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesAnvil.load();
		GT6RecipesSifter.load();
	}

	/** Undoes {@link #armAndPour} (the deterministic-slate restore, sibling-class order safe). */
	public static void restore() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	/** The Iron display with injected item + machine resolvers (the seams the pins ride). */
	public static MaterialTreeDisplay ironDisplay(BiFunction<OreDictPrefix, OreDictMaterial, Item> aItems, Function<String, ItemStack> aMachines) {
		return MaterialTreeDisplay.of(MaterialTreeBuilder.build(), MT.Fe, aItems, aMachines);
	}

	private static <T> void capture(java.util.function.Supplier<T> aGetter, java.util.function.Consumer<T> aSetter) {
		T tDefault = aGetter.get();
		sSeamRestores.add(() -> aSetter.accept(tDefault));
	}

	private MaterialTreePanzoomFixture() {
	}
}
