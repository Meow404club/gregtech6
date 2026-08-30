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

import java.util.HashSet;

/**
 * Static GT6 RecipeMap registry, port counterpart of the Furnace line in
 * upstream gregapi/data/RM.java:103.
 *
 * <p>{@code FURNACE} mirrors RM.Furnace: RecipeMapFurnace("mc.recipe.furnace",
 * "Furnace", NEI name "smelting", progress 0/1, GUI machines/Oven, item slots
 * 1/1/1, fluid slots 1/1/0, minimal inputs 0, power 1). The GUI path uses the
 * gt6 namespace instead of the upstream assets/gregtech one.
 *
 * <p>{@code COKE_OVEN} mirrors RM.CokeOven (RM.java:78, the 25-arg overload folding the
 * trailing defaults): "gt.recipe.cokeoven", "Coke Oven", NEI name null → the internal name,
 * progress 0/1, GUI machines/CokeOven (a string only — no asset shipped, the same form as
 * the Oven line), item slots 1/9/1, fluid slots 0/1/0, minimal inputs 1, power 1.
 * The recipes are poured in statically by {@link GT6RecipesCokeOven} (FMLCommonSetup) and
 * the tag-driven log subset by {@link GT6CokeOvenTagListener} (TagsUpdatedEvent).
 *
 * <p>P1 registry discipline: {@link #init()} is idempotent per JVM generation
 * (duplicate-name registration throws upstream Recipe.java:139), and
 * {@link #reset()} drops the generation so a subsequent init re-registers
 * cleanly. W2 (p4-machine-oven) wires {@code init()} into the mod lifecycle.
 */
public class GT6RecipeMaps {
	/** RM.java:103 — the Oven/Furnace map backed by the vanilla smelting recipes. */
	public static volatile RecipeMapFurnace FURNACE;

	/** RM.java:78 — the Coke Oven map (1 in / 9 out items, 0 in / 1 out fluids). */
	public static volatile RecipeMap COKE_OVEN;

	/** Registers all Recipe Maps. Safe to call repeatedly within one generation. */
	public static synchronized void init() {
		if (FURNACE != null) return;
		FURNACE = new RecipeMapFurnace(new HashSet<>(),
				"mc.recipe.furnace", "Furnace", "smelting",
				0, 1,
				"gt6:textures/gui/machines/Oven",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 1,
				/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
		COKE_OVEN = new RecipeMap(new HashSet<>(),
				"gt.recipe.cokeoven", "Coke Oven", null,
				0, 1,
				"gt6:textures/gui/machines/CokeOven",
				/*IN-OUT-MIN-ITEM=*/ 1, 9, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 1, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
	}

	/** Port-only: drops the whole generation (RecipeMap.RECIPE_MAPS included) for a clean re-init. */
	public static synchronized void reset() {
		FURNACE = null;
		COKE_OVEN = null;
		RecipeMap.reset();
	}
}
