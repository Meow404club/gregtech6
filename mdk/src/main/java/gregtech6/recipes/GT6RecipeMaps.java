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
 * <p>{@code SHREDDER} / {@code CRUSHER} / {@code LATHE} mirror RM.Shredder (RM.java:134),
 * RM.Crusher (:135) and RM.Lathe (:97), transcribed parameter-for-parameter: internal name,
 * local name, NEI name null → the internal name, progress 0/1, GUI machines/Shredder|
 * Crusher|Lathe, item slots 1/12/1 | 1/12/1 | 1/2/1, fluid slots 0/0/0, minimal inputs 0,
 * power 1. Two documented deviations: (a) the trailing 9 NEI args ("", 1, "", T,T,T,T,F,T,T)
 * have no counterpart in the 15-arg port ctor — declared deviation, same as the other maps;
 * (b) RM.Shredder is a {@code RecipeMapShredder} subclass upstream, whose on-demand
 * getRecipeFor RECYCLABLE synthesis (RecipeMapShredder.java:47-64) is a recipe-POOL feature
 * and stays pooled — the port carries the base {@link RecipeMap} with the identical constants.
 * The GUI paths are lowercase (1.20.1 ResourceLocation paths are [a-z0-9_.-/] — the earlier
 * uppercase FURNACE/COKE_OVEN strings never shipped an asset, the gui-family card lands the
 * assets for these three). Recipes are poured in statically by {@link GT6RecipesShCL}
 * (FMLCommonSetup).
 *
 * <p>{@code ENGINE_FUELS} mirrors RM.sFuelsEngine = FM.Engine (RM.java:172 → FM.java:45, the
 * RecipeMapFuel row folding the trailing NEI defaults): "gt.recipe.fuels.engine", "Engine
 * Fuels", NEI name null → the internal name, progress 0/1, GUI machines/Default (lowercase
 * — no asset shipped, the FURNACE-line form), item slots 1/2/0, fluid slots 1/2/0, minimal
 * inputs 1, power 1. MIN-ITEMS 0 + IN-FLUID 1 is what makes upstream treat the map as
 * fluid-only-capable (Recipe.java:518-523 checks the fluid hash indexes when
 * {@code mMinimalInputItems == 0}). The rows are poured in statically by
 * {@link GT6RecipesEngineFuels} (FMLCommonSetup) — one row per FM.Engine fuel fluid of
 * Loader_Fuels.java:77-120, keyed by the fluid, valued |EUt × duration| power per fluid
 * unit (Recipe.java:723-725 getAbsoluteTotalPower semantics).
 *
 * <p>{@code BURN} / {@code FLUIDBED} mirror the FM.java:41 / FM.java:40 RecipeMapFuel rows
 * (task p13-hu-steam-foundation, decision 2026-09-03-p13-boiler-family-split ②): "Burnable
 * Fuels" ("gt.recipe.fuels.burn", item 1/2/0, fluid 1/2/0, minimal inputs 1) and "Fluidized
 * Bed Fuels" ("gt.recipe.fuels.fluidbed", item 1/2/1, fluid 1/2/1, minimal inputs 2) — the
 * two maps differ ONLY in those minimal-input columns. Same base-{@link RecipeMap} form as
 * {@code ENGINE_FUELS}: upstream {@code RecipeMapFuel} is a thin shell (the aFuelMap=T flag
 * feeding Recipe.java:142 FUEL_MAP_LIST plus the addFuel row helper), which has no port
 * counterpart until the W2 pour card needs it — RecipeMapFurnaceFuel (FM.Furnace) is likewise
 * deferred to its W2 consumer card. DECLARED skeleton state: both maps register EMPTY — the
 * fuel rows pour in with the W2 burning-box card, and until then there is zero findRecipe
 * consumer.
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

	/** RM.java:134 — the Shredder map (1 in / 12 out items, 0 in / 0 out fluids). Base-class form; see the class doc for the subclass deviation. */
	public static volatile RecipeMap SHREDDER;

	/** RM.java:135 — the Crusher map (1 in / 12 out items, 0 in / 0 out fluids). */
	public static volatile RecipeMap CRUSHER;

	/** RM.java:97 — the Lathe map (1 in / 2 out items, 0 in / 0 out fluids). */
	public static volatile RecipeMap LATHE;

	/** FM.java:45 — the Engine Fuels map (1 in / 2 out items, 1 in / 2 out fluids; the fuel rows are fluid-only). */
	public static volatile RecipeMap ENGINE_FUELS;

	/** FM.java:40 — the Fluidized Bed Fuels map (1/2/1 items, 1/2/1 fluids, minimal inputs 2; empty until the W2 burning-box card pours the rows). */
	public static volatile RecipeMap FLUIDBED;

	/** FM.java:41 — the Burnable Fuels map (1/2/0 items, 1/2/0 fluids, minimal inputs 1; empty until the W2 burning-box card pours the rows). */
	public static volatile RecipeMap BURN;

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
				"gt6:textures/gui/machines/cokeoven",
				/*IN-OUT-MIN-ITEM=*/ 1, 9, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 1, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		SHREDDER = new RecipeMap(new HashSet<>(),
				"gt.recipe.shredder", "Shredder", null,
				0, 1,
				"gt6:textures/gui/machines/shredder",
				/*IN-OUT-MIN-ITEM=*/ 1, 12, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
		CRUSHER = new RecipeMap(new HashSet<>(),
				"gt.recipe.crusher", "Crusher", null,
				0, 1,
				"gt6:textures/gui/machines/crusher",
				/*IN-OUT-MIN-ITEM=*/ 1, 12, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
		LATHE = new RecipeMap(new HashSet<>(),
				"gt.recipe.lathe", "Lathe", null,
				0, 1,
				"gt6:textures/gui/machines/lathe",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
		ENGINE_FUELS = new RecipeMap(new HashSet<>(),
				"gt.recipe.fuels.engine", "Engine Fuels", null,
				0, 1,
				"gt6:textures/gui/machines/default",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		// the FM.java:40/:41 pair, upstream declaration order (FluidBed :40 before Burn :41);
		// against ENGINE_FUELS above the rows differ ONLY in the minimal-input columns:
		// FLUIDBED min-item 1 / min-fluid 1 / MIN 2 vs BURN 0 / 0 / 1
		FLUIDBED = new RecipeMap(new HashSet<>(),
				"gt.recipe.fuels.fluidbed", "Fluidized Bed Fuels", null,
				0, 1,
				"gt6:textures/gui/machines/default",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 1,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 1,
				/*MIN=*/ 2,
				/*AMP=*/ 1);
		BURN = new RecipeMap(new HashSet<>(),
				"gt.recipe.fuels.burn", "Burnable Fuels", null,
				0, 1,
				"gt6:textures/gui/machines/default",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
	}

	/** Port-only: drops the whole generation (RecipeMap.RECIPE_MAPS included) for a clean re-init. */
	public static synchronized void reset() {
		FURNACE = null;
		COKE_OVEN = null;
		SHREDDER = null;
		CRUSHER = null;
		LATHE = null;
		ENGINE_FUELS = null;
		FLUIDBED = null;
		BURN = null;
		RecipeMap.reset();
	}
}
