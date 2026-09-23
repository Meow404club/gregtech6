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

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.registry.GT6SlicerBlades;

/**
 * The RM.Slicer vanilla-face row pour — task p35-slicer-row-domain, the ruling-③
 * re-domained card (the boule-slicing hypothesis stays falsified; these rows are ZERO
 * boule-coupled). Five rows over the two registered blades, the upstream lines verbatim:
 * <pre>
 *   RM.Slicer.addRecipe2(T, 16, 16, ST.make(Items.leather_helmet,     1, W), IL.Shape_Slicer_Split.get(0), ST.make(Items.leather, 1, 0));  // Vanilla :638
 *   RM.Slicer.addRecipe2(T, 16, 16, ST.make(Items.leather_chestplate, 1, W), IL.Shape_Slicer_Split.get(0), ST.make(Items.leather, 2, 0));  // Vanilla :639
 *   RM.Slicer.addRecipe2(T, 16, 16, ST.make(Items.leather_leggings,   1, W), IL.Shape_Slicer_Split.get(0), ST.make(Items.leather, 2, 0));  // Vanilla :640
 *   RM.Slicer.addRecipe2(T, 16, 16, ST.make(Items.leather_boots,      1, W), IL.Shape_Slicer_Split.get(0), ST.make(Items.leather, 1, 0));  // Vanilla :641
 *   RM.Slicer.addRecipe2(T, 16, 16, ST.make(Items.paper,              1, W), IL.Shape_Slicer_Grid .get(0), plateTiny.mat(MT.Paper, 9));     // Other    :420
 * </pre>
 *
 * <p><b>Row semantics</b> (the GT6RecipesExtruder archaeology shape): EUt 16, duration 16,
 * the blade as the SECOND input at the upstream STACK-SIZE-0 marker — never consumed (the
 * port carries the never-consumed net effect through {@code Recipe.sNotConsumable}'s
 * {@code GT6SlicerBlades.isBlade} arm). The upstream {@code ST.make(..., W)} damage
 * wildcard rides the row's {@code mNoNBTChecks} flag (Recipe.java:140): a modern armor
 * ItemStack is born with a Damage:0 tag, so the default tag-gated match half would pin
 * the row to undamaged pieces — the flag widens the match to item identity alone.
 *
 * <p><b>POOLED with the domain rulings on the card</b> (the dependency investigation):
 * the GT6 MultiItemFood face (the cheese/egg/apple sliced rows, Loader_Recipes_Food.java
 * :130/:297 + Loader_Recipes_Crops.java:477-489 + MultiItemFood.java:602-618) — the
 * {@code IL.Food_*} item universe is not ported, CUT per the arch-open never_pool; the
 * fur row (Loader_Recipes_OreDict.java:98-100) — the {@code OD.craftingFur} input domain
 * is a mod-compat-only face (MoCreatures/WildMods upstream, LoaderItemData.java:560/:562),
 * zero carriers in the port, CUT; the melon row (Loader_Recipes_Vanilla.java:642) —
 * pools with its Eigths blade (not row0).
 *
 * <p><b>Load timing</b>: a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork (the GT6RecipesExtruder shape — the item DeferredRegisters
 * have fired by then). {@code load()} is idempotent per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesSlicer {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The EUt column of all five rows (:638-641/:420, the first long after the boolean). */
	public static final long SLICER_EUT = 16;

	/** The duration column of all five rows (:638-641/:420, the second long). */
	public static final long SLICER_DURATION = 16;

	/**
	 * The blade seams (the live defaults = the two registered blades at the port's count-1
	 * carrier of the upstream size-0 marker), fixtures injected offline where the
	 * RegistryObjects are unbound (the GT6RecipesExtruder mold-seam contract).
	 */
	public static Supplier<ItemStack> sSplitBladeResolver =
			() -> new ItemStack(GT6SlicerBlades.SHAPE_SLICER_SPLIT.get(), 1);
	public static Supplier<ItemStack> sGridBladeResolver =
			() -> new ItemStack(GT6SlicerBlades.SHAPE_SLICER_GRID.get(), 1);

	/**
	 * The tiny-paper output seam (the live default = the registered {@code plateTiny.Paper}
	 * item — the GTMaterialItems force table's one tiny plate), a fixture injected offline
	 * (the GT6RecipesWelder sOutputResolver contract).
	 */
	public static Supplier<ItemStack> sTinyPaperResolver = () -> {
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
				gregtech6.registry.GTMaterialItems.get(OP.plateTiny, MT.Paper);
		return tHandle == null ? null : new ItemStack(tHandle.get(), 9); // the GT6RecipesExtruder.resolveItem form (null = unregistered pair)
	};

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Extruder/Canner form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesSlicer::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the item DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesSlicer::load);
	}

	/**
	 * Pours the five vanilla rows into {@link GT6RecipeMaps#SLICER}. Idempotent.
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java)
		RecipeMap tMap = GT6RecipeMaps.SLICER;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle

		ItemStack tSplitBlade = sSplitBladeResolver.get();
		ItemStack tGridBlade = sGridBladeResolver.get();
		ItemStack tTinyPaper = sTinyPaperResolver.get();
		if (tSplitBlade == null || tGridBlade == null || tTinyPaper == null) {
			LOGGER.warn("GT6 Slicer pour skipped: a resolver returned null (broken fixture face)");
			return;
		}
		// :638-:641 — the four leather-armor rows over the split blade (the W damage
		// wildcard rides the no-tag match half, see the class doc)
		slice(tMap, new ItemStack(Items.LEATHER_HELMET, 1), tSplitBlade, new ItemStack(Items.LEATHER, 1));
		slice(tMap, new ItemStack(Items.LEATHER_CHESTPLATE, 1), tSplitBlade, new ItemStack(Items.LEATHER, 2));
		slice(tMap, new ItemStack(Items.LEATHER_LEGGINGS, 1), tSplitBlade, new ItemStack(Items.LEATHER, 2));
		slice(tMap, new ItemStack(Items.LEATHER_BOOTS, 1), tSplitBlade, new ItemStack(Items.LEATHER, 1));
		// :420 — the paper row over the grid blade → 9 tiny paper plates
		slice(tMap, new ItemStack(Items.PAPER, 1), tGridBlade, tTinyPaper);
		sLoaded = true;
		LOGGER.info("GT6 Slicer poured: 5 rows loaded (the vanilla leather-armor quartet + the paper face, Loader_Recipes_Vanilla.java:638-641 + Loader_Recipes_Other.java:420)");
	}

	/** One addRecipe2 row (the upstream two-item form: item + blade → output). */
	private static void slice(RecipeMap aMap, ItemStack aItem, ItemStack aBlade, ItemStack aOutput) {
		Recipe tRow = new Recipe(true,
				new ItemStack[] {aItem, aBlade},
				new ItemStack[] {aOutput},
				null, null, SLICER_DURATION, SLICER_EUT, 0);
		// The upstream ST.make(armor, 1, W) damage-wildcard carrier: a modern armor
		// ItemStack is BORN with a Damage:0 tag (the 1.20.1 Item,count ctor runs
		// setDamageValue(0) on damageable items), so the default tag-gated match half
		// would pin the row to undamaged pieces. mNoNBTChecks (Recipe.java:140, the
		// upstream per-row no-NBT axis) widens the exact match to item identity alone —
		// any-damage slices. Declared deviation: unrelated NBT (e.g. enchantments) is
		// ignored too, where upstream wildcards only the damage key.
		tRow.mNoNBTChecks = true;
		aMap.addRecipe(tRow);
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
	}
}
