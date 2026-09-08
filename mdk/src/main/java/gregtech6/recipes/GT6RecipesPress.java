/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.recipes;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The RM.Press static pour — task p26-w1-press-extruder-molds. DECLARED-EMPTY: the row0
 * static-row census of upstream {@code RM.Press} is fully pooled, so this loader pours
 * ZERO rows and {@link GT6RecipeMaps#PRESS} stays an empty list; the map's row0 rows come
 * from the {@code GT6RecipeMapFormingPress} findRecipe dynamic arm (the mold + block
 * forming synthesis — the GT6RecipeMapCanner R1 form: the DYNAMIC semantics port in full,
 * not a narrow pinned row set). The FLUIDBED declared-empty precedent
 * (GT6RecipeMaps.java, "empty until a future card pours the rows").
 *
 * <p><b>The pooled census</b> (why zero rows, the P10 not-ported ruling for the compat
 * classes): every upstream static {@code RM.Press} row lives in one of
 * <ul>
 * <li>{@code Loader_Recipes_Food.java:146-152/:494+} — the dough + Shape_Foodmold_* rows
 *     (the food-mold and food items pool with the food chain);</li>
 * <li>{@code MultiItemTechnological.java:503-530} — the Electrode_FR_* assembly rows
 *     (the electrode items pool with the electrode card);</li>
 * <li>the 59 compat classes (IC2 coal-ball chain, GalactiCraft plating, ExtraUtilities
 *     compression, Bluepower, ...) — not ported (P10).</li>
 * </ul>
 * The W2 forming-chain card replaces this loader's emptiness with the canonical
 * {@code Shape_Mold_*} / {@code Shape_Foodmold_*} rows when those item families land.
 *
 * <p><b>Load timing</b>: the Canner shape (a self-contained MOD-bus listener), kept so
 * the pour entry point and the generation-reset hook exist for the W2 rows to land into;
 * {@code load()} is idempotent per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesPress {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Distillery form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesPress::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the Canner shape (see the class doc). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesPress::load);
	}

	/**
	 * The declared-empty pour: guards the map into existence and logs the census (zero
	 * rows — every upstream RM.Press static row pools, see the class doc).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java)
		if (GT6RecipeMaps.PRESS == null) return; // reset() between init and load — a broken lifecycle
		sLoaded = true;
		LOGGER.info("GT6 Press poured: 0 static rows (the declared-empty pour — the food-mold/electrode/compat census pools; the row0 rows ride the GT6RecipeMapFormingPress dynamic arm)");
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
	}
}
