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

package gregtech6.recipes.maps;

import java.util.Collection;

import javax.annotation.Nullable;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GTMaterialItems;

/**
 * The Forming Press map — the port of upstream
 * {@code gregapi/recipes/maps/RecipeMapFormingPress.java} (task p26-w1-press-extruder-molds,
 * the RM.Press subclass, RM.java:99 "gt.recipe.press" 3/1/2 items, 0/0/0 fluids, MIN 0,
 * AMP 1, transcribed parameter-for-parameter over the 15-arg port ctor).
 *
 * <p><b>The upstream {@code findRecipe} override shape</b> (RecipeMapFormingPress.java
 * :44-75, the archaeology pinned in remember id478): <ol>
 * <li><b>the stored rows first</b> (:46) — {@code super.findRecipe} wins; the dynamic arms
 *     only fire when no stored row matched;</li>
 * <li><b>the two-slot gate</b> (:47) — the arms read exactly {@code aInputs[0]} and
 *     {@code aInputs[1]} ({@code aInputs.length < 2 || aInputs[0] == null ||
 *     aInputs[1] == null} return early) — of the map's 3 INPUT SLOTS the dynamic arm
 *     consumes the NON-MOLD one of the first two and leaves the mold in its slot (the
 *     upstream mold inputs are STACK-SIZE-0 markers — {@code IL.Shape_Mold_Name.get(0)}
 *     :52/:57 — whose consume decrements by zero, Recipe.java:780-781; the port port
 *     carries the never-consumed net effect through {@code Recipe.sNotConsumable}, the
 *     mold identity riding the {@code gt6:extruder_shapes} tag);</li>
 * <li><b>the Name-Mold arm</b> (:48-59) — synthesizes a one-time rename recipe
 *     {@code [Name Mold (0), item (1)] → item renamed to the mold's display name},
 *     {@code mCanBeBuffered = F};</li>
 * <li><b>the Credit-Mold arm</b> (:61-73) — a found row plus a credit mold in the inputs
 *     copies the row and stamps {@code credit_security_id} NBT onto the output.</li>
 * </ol>
 *
 * <p><b>The row0 face (declared deviation, the pooling ruling)</b>: the Shape_Mold_Name /
 * Shape_Mold_Credit items are NOT row0 (the press-mold family pools with the W2
 * forming-chain card, the GT6FoodCans row0-subset discipline), so arms 3 and 4 have no
 * item to key on and their faces are CUT here (the GT6RecipeMapCanner R1 dead-branch
 * precedent — the GC oxygen arm cut behind a mod-presence gate that is structurally dead).
 * What replaces them is the row0 FORMING arm, the same two-slot dynamic synthesis in the
 * SAME map-subclass form: when no stored row matched and the first two inputs are an
 * extruder mold + a storage block ({@code blockIngot} item), the arm synthesizes the
 * forming row LIVE — {@code [mold, block] → 9 plates / 18 sticks} by mold shape (the
 * RM.java:405/:407 row semantics, EUt 16, duration 32), the mold NOT consumed. The static
 * transcription of the same rows for the EXTRUDER map lives in {@code GT6RecipesExtruder}
 * (the card's static-loader face); the press carries the dynamic face so the KU ladder is
 * live in row0 without duplicating static rows across the two maps.
 *
 * <p><b>Seams</b> (the GT6RecipeMapCanner resolver form): {@link #sMoldShape} resolves a
 * stack to its mold-output prefix ({@code OP.plate} / {@code OP.stick}, null = not a mold)
 * — the live default is the two registered mold identities, fixtures injected offline
 * where the RegistryObjects are unbound.
 */
public class GT6RecipeMapFormingPress extends RecipeMap {

	/**
	 * The EUt column of the synthesized forming rows (RM.java:405/:407 first long argument
	 * after the booleans).
	 */
	public static final long FORMING_EUT = 16;

	/**
	 * The duration column of the synthesized forming rows (RM.java:405/:407 second long
	 * argument — BOTH the plate and the rod row carry 32).
	 */
	public static final long FORMING_DURATION = 32;

	/**
	 * The mold→output-prefix seam: the stack's mold identity as the prefix it FORMS
	 * ({@code OP.plate} for the plate mold, {@code OP.stick} for the rod mold), null = not
	 * a row0 mold. The live default resolves the two registered mold items (the unbound-
	 * RegistryObject wall makes this null offline — fixtures injected, the
	 * GT6RecipesCanner seam contract).
	 */
	public static java.util.function.Function<ItemStack, OreDictPrefix> sMoldShape = aStack -> {
		if (aStack == null || aStack.isEmpty()) return null;
		Item tItem = aStack.getItem();
		if (tItem == GT6ExtruderMolds.SHAPE_EXTRUDER_PLATE.get()) return OP.plate;
		if (tItem == GT6ExtruderMolds.SHAPE_EXTRUDER_ROD.get()) return OP.stick;
		return null;
	};

	/** The output multiplier of the plate rows (RM.java:405 {@code OP.plate.mat(aMat, 9)}). */
	public static final int PLATE_OUTPUT_COUNT = 9;

	/** The output multiplier of the rod rows (RM.java:407 {@code OP.stick.mat(aMat, 18)}). */
	public static final int ROD_OUTPUT_COUNT = 18;

	/**
	 * The storage-block seam: the stack's material WHEN it is a row0 blockIngot carrier,
	 * null otherwise. The live default reads the {@link GTMaterialPrefixBlockItem} public
	 * finals (prefix == {@code OP.blockIngot} gate — the declared 9-unit input face);
	 * fixtures injected offline where mod BlockItems are not constructible (the intrusive
	 * registry wall, the GT6RecipesShCLTest convention).
	 */
	public static java.util.function.Function<ItemStack, OreDictMaterial> sBlockMaterial = aStack -> {
		if (aStack == null || aStack.isEmpty()) return null;
		if (aStack.getItem() instanceof GTMaterialPrefixBlockItem tBlockItem && tBlockItem.prefix == OP.blockIngot) {
			return tBlockItem.material;
		}
		return null;
	};

	public GT6RecipeMapFormingPress(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
			long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath,
			long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems,
			long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		super(aRecipeList, aNameInternal, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath,
				aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount,
				aMinimalInputFluids, aMinimalInputs, aPower);
	}

	@Override
	@Nullable
	public Recipe findRecipe(@Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		// :46 — the stored rows win over the dynamic arm (the Credit arm's rewrite of a found
		// row rides the pooled credit-mold item, the W2 forming-chain card)
		Recipe rRecipe = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		// :47 — the two-slot gate, the empty-looking stacks included (the 1.20.1 slot census)
		if (aInputs == null || aInputs.length < 2 || rRecipe != null) return rRecipe;
		ItemStack tFirst = aInputs[0], tSecond = aInputs[1];
		if (tFirst == null || tFirst.isEmpty() || tSecond == null || tSecond.isEmpty()) return null;
		// the row0 FORMING arm (the declared Name/Credit replacement, see the class doc):
		// identify the mold slot and the block slot, either order (upstream :49/:54 order symmetry)
		ItemStack tMold = tFirst;
		OreDictPrefix tShape = sMoldShape.apply(tFirst);
		ItemStack tBlock = tSecond;
		if (tShape == null) {
			tMold = tSecond;
			tShape = sMoldShape.apply(tSecond);
			tBlock = tFirst;
		}
		if (tShape == null) return null;
		OreDictMaterial tMaterial = sBlockMaterial.apply(tBlock);
		if (tMaterial == null) return null; // not a row0 blockIngot carrier (the 9-unit input face)
		// the formed output: 9x the plate / 18x the stick of the block's material — null when
		// the material generates no such item (the upstream mat() silent-skip semantics)
		Item tOutput = sOutputResolver.apply(tShape, tMaterial);
		if (tOutput == null) return null;
		ItemStack tOutputStack = new ItemStack(tOutput, tShape == OP.plate ? PLATE_OUTPUT_COUNT : ROD_OUTPUT_COUNT);
		// the one-time synthesis: mCanBeBuffered F (the upstream :52/:57 one-time form —
		// aRecipe(F, F, F, ...) third boolean); the consume pass skips the mold through
		// Recipe.sNotConsumable (the findRecipe call is a pure lookup, nothing consumed here —
		// the machine's two-stage isRecipeInputEqual consume does the real consumption)
		return new Recipe(false,
				new ItemStack[] {new ItemStack(tBlock.getItem(), 1), new ItemStack(tMold.getItem(), 1)},
				new ItemStack[] {tOutputStack},
				null, null, FORMING_DURATION, FORMING_EUT, 0);
	}

	/**
	 * The formed-output seam: the registered (shape, material) item, or null when the
	 * material generates no such item (the upstream {@code mat()} silent-skip semantics —
	 * the live default = the GTMaterialItems registration walk, the resolveItem form),
	 * fixtures injected offline.
	 */
	public static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> sOutputResolver =
			GT6RecipeMapFormingPress::resolveOutput;

	/** The live output lookup (GTMaterialItems.get) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveOutput(OreDictPrefix aShape, OreDictMaterial aMaterial) {
		//? if forge {
		net.minecraftforge.registries.RegistryObject<Item> tHandle = GTMaterialItems.get(aShape, aMaterial);
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = GTMaterialItems.get(aShape, aMaterial);
		 *///?}
		return tHandle == null ? null : tHandle.get();
	}
}
