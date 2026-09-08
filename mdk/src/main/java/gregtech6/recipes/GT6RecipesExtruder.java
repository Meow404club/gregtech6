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

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM.Extruder plate/rod material pour — task p26-w1-press-extruder-molds, the port
 * counterpart of the RM.java:405 (plate row) / :407 (rod row) transcription domain, two
 * rows per material over the registered universe:
 * <pre>
 *   RM.Extruder.addRecipe2(F, F, F, F, F, 16, 32, ST.amount(1, tStack), IL.Shape_Extruder_Plate.get(0), OP.plate.mat(aMat, 9));  // :405
 *   RM.Extruder.addRecipe2(F, F, F, F, T, 16, 32, ST.amount(1, tStack), IL.Shape_Extruder_Rod .get(0), OP.stick.mat(aMat, 18));  // :407
 * </pre>
 *
 * <p><b>Row semantics</b> (the remember-id478 archaeology): EUt 16, duration 32, ONE
 * storage-block input, the mold as the SECOND input at the upstream STACK-SIZE-0 marker —
 * never consumed (the port carries the never-consumed net effect through
 * {@code Recipe.sNotConsumable}; the row's fifth-boolean F/T axis is aLogErrors, NOT the
 * not-consumable flag — both molds are equally unconsumed). Outputs are
 * material-conservative by the GT6 unit arithmetic: one 9-unit block → 9 plates
 * ({@code OP.plate.mat(aMat, 9)}) or 18 sticks ({@code 18 × U/2 = 9U}).
 *
 * <p><b>Declared deviations (both logged on the card)</b>:
 * <ul>
 * <li>the upstream input axis per stone material is the four stone-type block forms
 *     {@code (aStone, aCobble, aSmooth, aDustBlock)} (:403 loop) — the port collapses it
 *     to {@code blockIngot} (the one 9-unit storage-block form every registered material
 *     carries; the stone-type metatype block domain pools);</li>
 * <li>the :406 plateCurved row pools WITH ITS MOLD (the W2 forming-chain card — the
 *     {@code Shape_Extruder_Plate_Curved} item is not row0);</li>
 * <li>the Blackstone/Basalt/Stone module rows and the forging-handler prefix rows
 *     (Loader_Recipes_Extruder / Loader_Recipes_Handlers :816-838) pool — the module
 *     generator items and the Shape_SimpleEx_* family are not row0.</li>
 * </ul>
 *
 * <p><b>Load timing</b>: a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork (the GT6RecipesCanner shape — the item DeferredRegisters
 * have fired by then). {@code load()} is idempotent per JVM generation; an unresolvable
 * row skips SILENTLY with a count (the upstream {@code mat()} null drop semantics).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesExtruder {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The EUt column of both rows (:405/:407, the first long argument after the booleans). */
	public static final long EXTRUDER_EUT = 16;

	/** The duration column of both rows (:405/:407, the second long argument). */
	public static final long EXTRUDER_DURATION = 32;

	/**
	 * The (prefix, material) → item lookup seam (the live default = the GTMaterialItems
	 * registration walk, the GT6RecipesShCL resolveItem form), fixtures injected offline.
	 */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver =
			GT6RecipesExtruder::resolveItem;

	/**
	 * The row-input seam (the live default = the pair's registered blockIngot item), a
	 * fixture-item supplier injected offline (the GT6RecipesCanner seam contract).
	 */
	public static java.util.function.Function<GTMaterialItems.PrefixMaterial, ItemStack> sBlockResolver =
			GT6RecipesExtruder::resolveBlock;

	/**
	 * The mold seams (the live defaults = the two registered mold items, the port's
	 * count-1 carrier of the upstream size-0 marker), fixtures injected offline where the
	 * RegistryObjects are unbound (the GT6RecipesCanner seam contract).
	 */
	public static java.util.function.Supplier<ItemStack> sPlateMoldResolver =
			() -> new ItemStack(GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get(), 1);
	public static java.util.function.Supplier<ItemStack> sRodMoldResolver =
			() -> new ItemStack(GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get(), 1);

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Distillery form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesExtruder::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the item DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesExtruder::load);
	}

	/**
	 * Pours the plate/rod rows into {@link GT6RecipeMaps#EXTRUDER}: two rows per registered
	 * {@code blockIngot} material whose plate and stick items both exist. Idempotent; an
	 * unresolvable row skips with a count.
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java)
		RecipeMap tMap = GT6RecipeMaps.EXTRUDER;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle

		ItemStack tPlateMold = sPlateMoldResolver.get();
		ItemStack tRodMold = sRodMoldResolver.get();
		int tPoured = 0, tSkipped = 0;
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			if (tPair.prefix() != OP.blockIngot) continue; // the declared 9-unit input face
			OreDictMaterial tMaterial = tPair.material();
			Item tPlate = sMaterialItemResolver.apply(OP.plate, tMaterial);
			Item tStick = sMaterialItemResolver.apply(OP.stick, tMaterial);
			if (tPlate == null || tStick == null) {tSkipped += 2; continue;} // the upstream mat() null drop
			ItemStack tBlock = sBlockResolver.apply(tPair);
			if (tBlock == null || tBlock.isEmpty()) {tSkipped += 2; continue;}
			// :405 — block + plate mold (never consumed) → 9 plates
			tMap.addRecipe(new Recipe(true,
					new ItemStack[] {tBlock, tPlateMold},
					new ItemStack[] {new ItemStack(tPlate, 9)},
					null, null, EXTRUDER_DURATION, EXTRUDER_EUT, 0));
			tPoured++;
			// :407 — block + rod mold (never consumed) → 18 sticks
			tMap.addRecipe(new Recipe(true,
					new ItemStack[] {tBlock, tRodMold},
					new ItemStack[] {new ItemStack(tStick, 18)},
					null, null, EXTRUDER_DURATION, EXTRUDER_EUT, 0));
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Extruder poured: {} rows loaded, {} skipped (materials without a plate or stick item, = upstream mat() drops)", tPoured, tSkipped);
	}

	/** A fresh count-1 stack of the pair's blockIngot item (the live row input). */
	private static ItemStack resolveBlock(GTMaterialItems.PrefixMaterial aPair) {
		RegistryObject<Item> tHandle = GTMaterialBlocks.get(aPair.prefix(), aPair.material());
		return tHandle == null ? null : new ItemStack(tHandle.get(), 1);
	}

	/** The live item lookup (GTMaterialItems.get) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
	}
}
