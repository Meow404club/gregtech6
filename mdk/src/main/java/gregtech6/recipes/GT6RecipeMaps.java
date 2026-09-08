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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

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
 * counterpart until a pour card needs it. The BURN rows pour in with the W2 burning-box
 * card ({@link GT6RecipesBurnFuels}, FMLCommonSetup — the Loader_Fuels.java:77-120 BURN
 * column, the p12 ENGINE_FUELS transcription shape); FLUIDBED stays DECLARED-empty (the
 * spec ⑥ archaeology verdict on GT6RecipesBurnFuels — the upstream :37-43 material rows
 * need calcite/ash/burn-time primitives no card has landed). {@code FURNACE_FUEL} joins
 * them as the FM.java:38 on-demand synthesizer (no rows, ever — see its class doc).
 * Until the Liquid/Gas Burning Box consumers went live (this card) there was zero
 * findRecipe consumer for BURN.
 *
 * <p>{@code DISTILLERY} / {@code DRYING} mirror the RM.java:70 / RM.java:71 base-{@link
 * RecipeMap} rows (task p14-drying-distillery-maps, decision 2026-09-03-p14-distilled-loop
 * ⑥), declared in the upstream order (Distillery :70 before Drying :71):
 * "gt.recipe.distillery" / "Distillery" (item 1/2/1, fluid 1/2/1, minimal inputs 2) and
 * "gt.recipe.drying" / "Dryer" (item 1/1/0, fluid 1/3/0, minimal inputs 1). The trailing
 * NEI booleans fold away in the 15-arg port ctor like every other map. The GUI paths are
 * the upstream machines/Distillery|Dryer strings lowercased (the same 1.20.1
 * ResourceLocation-charset convention as the Shredder line — {@code
 * GTBasicMachineScreen.backgroundOf} parses this string). DRYING's Water→Distilled Water row
 * poured with the W3 loop-closure card (Loader_Recipes_Chem.java:525); DISTILLERY stayed
 * DECLARED-empty until task p16-distillery-family ③ landed the Integrated Circuit item
 * system (the ST.tag(0) selector every :534-541 row carries) — its seven water-family rows
 * now pour via {@code GT6RecipesDistillery} (the other ~299 census rows stay pooled on
 * unregistered fluids/items, the class doc carries the census). Both maps have live
 * findRecipe consumers since the dryer/distillery family BETs.
 *
 * <p>{@code CHISEL} mirrors the RM.java:138 base-map row (task p19-chisel-recipes):
 * "gt.recipe.chisel" / "Chisel" (item 1/1/1, fluid 0/0/0, minimal inputs 0, power 1), GUI
 * machines/Chisel lowercased per the Shredder-line convention. The rows pour in via
 * {@link GT6RecipesStoneChisel} (FMLCommonSetup — the RM.java:470/:508/:514 stonetypes
 * + bricks lines and the Loader_Recipes_Vanilla.java:772-773 vanilla pair). Upstream the
 * field is a {@code RecipeMapChisel} subclass whose {@code findRecipe} override
 * (RecipeMapChisel.java:47-64) synthesizes oredict ring-composition rows at lookup time —
 * an OM runtime feature ({@code OreDictManager.getOres} + {@code GAPI_POST
 * .mFinishedServerStarted}) with no port counterpart; the port carries the base
 * {@link RecipeMap} with the identical constants (the RecipeMapShredder deviation form,
 * documented deviation). The map's live findRecipe consumer is the
 * {@code GTChiselItem} right-click gate (the ToolCompat.java:224-229 transcription) —
 * there is no machine behind this map (upstream likewise).
 *
 * <p>P1 registry discipline: {@link #init()} is idempotent per JVM generation
 * (duplicate-name registration throws upstream Recipe.java:139), and
 * {@link #reset()} drops the generation so a subsequent init re-registers
 * cleanly. W2 (p4-machine-oven) wires {@code init()} into the mod lifecycle.
 */
public class GT6RecipeMaps {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * The generation-reset hooks: every loader that owns a private static "poured" flag
	 * registers its resetForTest here from its static initializer, so {@link #reset()}
	 * retires the WHOLE generation. One generation = the 12 map fields (11 + the MIXER
	 * append of task p26-c-foam-fluid-refill) + RecipeMap.RECIPE_MAPS
	 * + every registered loader pour-flag — the flags must retire WITH the maps, or the
	 * "maps cleared × pour-flag set" poison state becomes representable and the loaders'
	 * load() silently early-returns (ADR-P18 staticinit poison fix, case A: generation-wise
	 * reset via hook registration, no reverse maps→loader class dependency; the registration
	 * direction loader→maps is the same direction as the existing pour dependency, no clinit
	 * cycle — this class's static state is the empty hook list plus the null map fields).
	 */
	private static final CopyOnWriteArrayList<Runnable> sGenerationResetHooks = new CopyOnWriteArrayList<>();

	/**
	 * Registers a generation-reset hook (idempotent). Package-private by design — the
	 * recipe loaders' static initializers are the only intended callers, keeping this off
	 * the public API surface.
	 */
	static void registerGenerationResetHook(Runnable aHook) {
		sGenerationResetHooks.addIfAbsent(aHook);
	}

	/** Test seam (package-private, read-only copy): the registered hooks — the guard test's structural pin, i.e. the poison-capable loader ledger. */
	static List<Runnable> generationResetHooks() {
		return List.copyOf(sGenerationResetHooks);
	}

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

	/** RM.java:138 — the Chisel map (1 in / 1 out items, 0 in / 0 out fluids). Base-class form; see the class doc for the RecipeMapChisel subclass deviation. */
	public static volatile RecipeMap CHISEL;

	/** FM.java:45 — the Engine Fuels map (1 in / 2 out items, 1 in / 2 out fluids; the fuel rows are fluid-only). */
	public static volatile RecipeMap ENGINE_FUELS;

	/** FM.java:40 — the Fluidized Bed Fuels map (1/2/1 items, 1/2/1 fluids, minimal inputs 2; empty until the W2 burning-box card pours the rows). */
	public static volatile RecipeMap FLUIDBED;

	/** FM.java:41 — the Burnable Fuels map (1/2/0 items, 1/2/0 fluids, minimal inputs 1; empty until the W2 burning-box card pours the rows). */
	public static volatile RecipeMap BURN;

	/** RM.java:70 — the Distillery map (1/2/1 items, 1/2/1 fluids, minimal inputs 2; the seven water-family rows pour via GT6RecipesDistillery, the rest of the census stays pooled). */
	public static volatile RecipeMap DISTILLERY;

	/** RM.java:71 — the Drying map (1/1/0 items, 1/3/0 fluids, minimal inputs 1; DECLARED-empty — the Water→DistW row pours with the W3 loop-closure card). */
	public static volatile RecipeMap DRYING;

	/**
	 * RM.java:148 — the Canner map (task p24-canner-machine): the
	 * {@link gregtech6.recipes.maps.GT6RecipeMapCanner} subclass (the dynamic fill/empty
	 * semantics, ruling R1), transcribed parameter-for-parameter over the 15-arg port ctor:
	 * "gt.recipe.canner", "Canning Machine", NEI name null → the internal name, progress 0/1,
	 * GUI machines/Canner (lowercased, the Shredder-line convention), item slots 2/2/1,
	 * fluid slots 1/1/0, minimal inputs 1, power 1. The upstream ctor's
	 * {@code mMaxFluid*Size = 128000} cap folds into the T1 registration-row tank capacity
	 * (the R6 ruling, the GTGeneratorFluidBedBlockEntity:54-55 fold precedent). The 17
	 * refill rows pour via {@link GT6RecipesCanner} (FMLCommonSetup); the EMPTY and FILL
	 * dynamic arms ride the map's own findRecipe override
	 * (RecipeMapFluidCanner.java:48-71, minus the GC dead branch and the GAPI_POST guard,
	 * the two R1 declared deviations).
	 */
	public static volatile gregtech6.recipes.maps.GT6RecipeMapCanner CANNER;

	/**
	 * RM.java:74 — the Mixer map (task p26-c-foam-fluid-refill): "gt.recipe.mixer",
	 * "Mixer", NEI name null → the internal name, progress 0/1, GUI machines/mixer
	 * (lowercased, the Shredder-line convention), item slots 6/1/0, fluid slots 6/2/0,
	 * minimal inputs 2, power 1 — the upstream ctor row parameter-for-parameter over the
	 * 15-arg port ctor (the trailing NEI booleans fold away like every other map).
	 *
	 * <p><b>Boundary note (declared):</b> the task card froze this file ("GT6RecipeMaps.java
	 * 不碰…若发现必须碰，停下报告理由") — the STOP-and-report arm fired: the card's own
	 * spec ②/④ (the Mixer rock/Pd rows of Loader_Recipes_Other.java:251-304/:485-486) need
	 * a MIXER RecipeMap to pour into and this file is the single canonical registration
	 * point (the P1 registry discipline + the ADR-P18 generation-reset). The declaration is
	 * minimal and precedent-backed: the CHISEL map has been living machine-less since P19
	 * (its consumer is the chisel item gate), and DISTILLERY/DRYING shipped DECLARED-empty
	 * before their machines landed — rows pour via {@link gregtech6.recipes.GT6RecipesMixer}
	 * (FMLCommonSetup), no machine is touched, the existing maps are untouched. This commit
	 * is ATOMIC so a contrary ruling can drop it independently.
	 */
	public static volatile RecipeMap MIXER;

	/**
	 * FM.java:38 — the Furnace Fuels map (task p13-burning-box-family spec ①): the
	 * Solid Burning Box fuel face. Upstream this map is a static-row-EMPTY on-demand
	 * synthesizer (RecipeMapFurnaceFuel.findRecipe builds fuel rows from the vanilla
	 * furnace fuel value) — the port carries the same shape over the ForgeHooks
	 * .getBurnTime bridge, so {@link #init()} constructs the instance but NO rows are
	 * ever poured into its list (the class doc on RecipeMapFurnaceFuel).
	 */
	public static volatile RecipeMapFurnaceFuel FURNACE_FUEL;

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
		// the RM.java:138 row verbatim: items 1/1/1, fluids 0/0/0, MIN 0, AMP 1 (the
		// trailing NEI booleans fold away in the 15-arg port ctor, like every other map)
		CHISEL = new RecipeMap(new HashSet<>(),
				"gt.recipe.chisel", "Chisel", null,
				0, 1,
				"gt6:textures/gui/machines/chisel",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 1,
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
		// the RM.java:70/:71 pair, upstream declaration order (Distillery :70 before Drying :71);
		// Distillery carries MIN 2 (item AND fluid minimum 1 each), Drying MIN 1 — both DECLARED-empty
		DISTILLERY = new RecipeMap(new HashSet<>(),
				"gt.recipe.distillery", "Distillery", null,
				0, 1,
				"gt6:textures/gui/machines/distillery",
				/*IN-OUT-MIN-ITEM=*/ 1, 2, 1,
				/*IN-OUT-MIN-FLUID=*/ 1, 2, 1,
				/*MIN=*/ 2,
				/*AMP=*/ 1);
		DRYING = new RecipeMap(new HashSet<>(),
				"gt.recipe.drying", "Dryer", null,
				0, 1,
				"gt6:textures/gui/machines/dryer",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 0,
				/*IN-OUT-MIN-FLUID=*/ 1, 3, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		// FM.java:38 — the Solid Burning Box fuel face: an on-demand synthesizer over the
		// ForgeHooks.getBurnTime bridge, no static rows (RecipeMapFurnaceFuel class doc)
		FURNACE_FUEL = new RecipeMapFurnaceFuel();
		// RM.java:148 — the Canner map, the subclass ctor with the identical constants row
		CANNER = new gregtech6.recipes.maps.GT6RecipeMapCanner(new HashSet<>(),
				"gt.recipe.canner", "Canning Machine", null,
				0, 1,
				"gt6:textures/gui/machines/canner",
				/*IN-OUT-MIN-ITEM=*/ 2, 2, 1,
				/*IN-OUT-MIN-FLUID=*/ 1, 1, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		// RM.java:74 — the Mixer map (task p26-c-foam-fluid-refill, the declared boundary note
		// on the field above): items 6/1/0, fluids 6/2/0, MIN 2, AMP 1 — the RM.java:74 row
		// verbatim, the trailing NEI booleans folding away in the 15-arg port ctor
		MIXER = new RecipeMap(new HashSet<>(),
				"gt.recipe.mixer", "Mixer", null,
				0, 1,
				"gt6:textures/gui/machines/mixer",
				/*IN-OUT-MIN-ITEM=*/ 6, 1, 0,
				/*IN-OUT-MIN-FLUID=*/ 6, 2, 0,
				/*MIN=*/ 2,
				/*AMP=*/ 1);
	}

	/**
	 * Port-only: drops the whole generation (RecipeMap.RECIPE_MAPS included) for a clean
	 * re-init. The whole generation includes every registered loader pour-flag: each hook
	 * runs after the map teardown, isolated per hook (one failing hook must not orphan the
	 * cleanup of the rest — a half-cleared generation is the poison state in a variant form).
	 */
	public static synchronized void reset() {
		FURNACE = null;
		COKE_OVEN = null;
		SHREDDER = null;
		CRUSHER = null;
		LATHE = null;
		CHISEL = null;
		ENGINE_FUELS = null;
		FLUIDBED = null;
		BURN = null;
		DISTILLERY = null;
		DRYING = null;
		CANNER = null;
		MIXER = null;
		FURNACE_FUEL = null;
		RecipeMap.reset();
		for (Runnable tHook : sGenerationResetHooks) {
			try {tHook.run();}
			catch (Throwable tThrowable) {LOGGER.warn("GT6 RecipeMaps: a generation-reset hook failed — continuing with the remaining hooks", tThrowable);}
		}
	}
}
