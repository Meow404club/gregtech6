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

import java.util.List;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import gregtech6.item.GT6Circuits;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMultiBlocks;

/**
 * The Welder wall rows (task p29-w3-nbtdesign-parts ④) — the per-wall
 * {@code RM.Welder.addRecipe2(F, 16, 256, OP.plate.mat(aMat, 4), ST.tag(10), wall)} rows
 * of Loader_MultiTileEntities.java:1143-1153 and the dense
 * {@code addRecipe2(F, 64, 512, OP.plateDense.mat(aMat, 4), ST.tag(10), dense)} rows of
 * :1155-1165, poured into {@link GT6RecipeMaps#WELDER} at first boot.
 *
 * <p>Row semantics (upstream verbatim): FOUR plates/dense plates of the wall's material
 * plus the {@code ST.tag(10)} selector circuit at configuration 10 (ST.java:779-781 —
 * the port {@link GT6Circuits#selector} form; NOT consumed — the Recipe circuit
 * identity-skip) produce ONE wall block, UNBUFFERED ({@code F}, the first addRecipe2
 * argument). The rows coexist with the tier-b {@code data/gt6/recipe_maps/welder.json}
 * smoke subset (the JSON loader replaces only ITS OWN subset per reload — the
 * identity-tracked remove-then-add contract, GT6RecipeMapJsonLoader doc).
 *
 * <p>Deviations (declared): the Galvanized Steel rows (:1147/:1158) SKIP at resolve time
 * — SteelGalvanized carries no port plate/plateDense item rows (the GTMaterialItems
 * registration gate), the upstream {@code FL.exists} drop form, 20 of 22 rows pour.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesWelder {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The nominal welder EU per row family (the :1143/:1155 second argument). */
	public static final long WALL_EUT = 16, DENSE_EUT = 64;
	/** The welder duration per row family (the third argument). */
	public static final long WALL_DURATION = 256, DENSE_DURATION = 512;
	/** The {@code ST.tag(10)} selector configuration every wall row carries. */
	public static final int SELECTOR_CONFIG = 10;
	/** The plate count of the wall input column (four). */
	public static final int PLATE_COUNT = 4;

	/**
	 * The plate/dense-plate resolver seam (the distillery sFluidResolver precedent — the
	 * offline row-test injects fixtures). NULL/empty = the row skips (the FL.exists drop).
	 * Production: the registered material-prefix item (count 1; the row multiplies).
	 */
	public static BiFunction<gregapi.oredict.OreDictPrefix, gregapi.oredict.OreDictMaterial, ItemStack> sPlateResolver =
			GT6RecipesWelder::defaultPlateResolver;

	/**
	 * The wall-item output resolver seam (the sPlateResolver twin — the offline row-test
	 * injects a fixture item; the Forge item registry cannot be written offline).
	 * NULL = the row skips.
	 */
	public static java.util.function.Function<String, net.minecraft.world.item.Item> sOutputResolver =
			GT6RecipesWelder::defaultOutputResolver;

	/**
	 * The selector-circuit seam (the distillery sCircuitResolver twin): production
	 * resolves the registered INTEGRATED_CIRCUIT at config {@code aConfig}; the offline
	 * row-test injects a fixture item.
	 */
	public static java.util.function.IntFunction<ItemStack> sSelectorResolver =
			GT6Circuits::selector;

	/** The production resolver: the registered part block's item (the paths coincide — the registration loops). */
	private static net.minecraft.world.item.Item defaultOutputResolver(String aWallPath) {
		gregtech6.block.multiblock.GTMultiBlockPartBlock tBlock = GTMultiBlocks.anyPartBlock(aWallPath);
		return tBlock == null ? null : tBlock.asItem();
	}

	/** The production resolver: the registered material-prefix item, count applied by the caller. */
	private static ItemStack defaultPlateResolver(gregapi.oredict.OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aMaterial) {
		try {
			var tHandle = GTMaterialItems.get(aPrefix, aMaterial);
			net.minecraft.world.item.Item tItem = tHandle == null ? null : tHandle.get(); // the 21.1 get() returns a NULL holder for an unregistered pair
			return tItem == null ? null : new ItemStack(tItem, 1);
		} catch (IllegalStateException aE) {
			return null; // the unregistered pair (never registered at boot = the FL.exists form)
		}
	}

	/**
	 * One welder wall row — the Loader line columns (the wall path + its material NAME +
	 * the dense flag). The material rides its MT internal NAME (the offline-safe form:
	 * the gregapi MT singletons populate at loader init, they are NULL in a bare test
	 * JVM) and materializes through {@link #sMaterialResolver} at pour time.
	 */
	public record WelderWallRow(String note, String wallPath, gregapi.oredict.OreDictPrefix platePrefix,
			String materialName, boolean dense) {}

	/** The 11 metal-wall rows (:1143-1153) + the 11 dense-wall rows (:1155-1165), the registration order. */
	private static volatile List<WelderWallRow> sTable = null;

	/** The row table (lazy — the class-load at MOD CONSTRUCTION precedes the material tables). */
	public static synchronized List<WelderWallRow> table() {
		List<WelderWallRow> tTable = sTable;
		if (tTable == null) {
			tTable = new java.util.ArrayList<>();
			for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.METAL_WALL_ROWS) {
				tTable.add(new WelderWallRow(":1143-1153 " + tRow.path(), tRow.path(),
						gregapi.data.OP.plate, materialNameOf(tRow.path()), false));
			}
			for (GTMultiBlocks.MultiblockPartRow tRow : GTMultiBlocks.WALL_ROWS) {
				tTable.add(new WelderWallRow(":1155-1165 " + tRow.path(), tRow.path(),
						gregapi.data.OP.plateDense, materialNameOf(tRow.path()), true));
			}
			sTable = tTable;
		}
		return tTable;
	}

	/** The row material's MT internal name — a pure switch over the eleven wall materials (the Loader aMat columns verbatim). */
	public static String materialNameOf(String aPath) {
		return switch (aPath) {
			case "machine_wall_lead", "dense_wall_lead" -> "Pb";
			case "machine_wall_bronze", "dense_wall_bronze" -> "Bronze";
			case "machine_wall_steel", "dense_wall_steel" -> "Steel";
			case "machine_wall_galvanized_steel", "dense_wall_galvanized_steel" -> "SteelGalvanized";
			case "machine_wall_stainless_steel", "dense_wall_stainless_steel" -> "StainlessSteel";
			case "machine_wall_invar", "dense_wall_invar" -> "Invar";
			case "machine_wall_titanium", "dense_wall_titanium" -> "Ti";
			case "machine_wall_tungstensteel", "dense_wall_tungstensteel" -> "TungstenSteel";
			case "machine_wall_tungsten", "dense_wall_tungsten" -> "W";
			case "machine_wall_tantalum_hafnium_carbide", "dense_wall_tantalum_hafnium_carbide" -> "Ta4HfC5";
			case "machine_wall_adamantium", "dense_wall_adamantium" -> "Ad";
			default -> throw new IllegalArgumentException("unknown wall row " + aPath);
		};
	}

	/**
	 * The material-name resolver seam (production: {@code OreDictMaterial.get(name)} —
	 * the MT singletons are live at pour time; the offline tests never materialize).
	 */
	public static java.util.function.Function<String, gregapi.oredict.OreDictMaterial> sMaterialResolver =
			gregapi.oredict.OreDictMaterial::get;

	private static boolean sLoaded = false;

	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesWelder::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the item registers are live by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesWelder::load);
	}

	/**
	 * Pours the resolvable wall rows into {@link GT6RecipeMaps#WELDER}. Idempotent; the
	 * unregistered-plate rows skip with a count (the upstream FL.exists drop).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod, tests may race it
		RecipeMap tMap = GT6RecipeMaps.WELDER;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle

		int tPoured = 0, tSkipped = 0;
		for (WelderWallRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-plate silent skip (the FL.exists drop)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Welder wall rows poured: {} loaded, {} skipped (unregistered plate items, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/**
	 * Row → Recipe, or null when the plate item fails to resolve (the silent-skip
	 * semantics). The upstream {@code addRecipe2(F, eUt, dur, plate×4, ST.tag(10), wall)}
	 * shape: UNBUFFERED, plate stack + the config-10 selector (never consumed — the
	 * circuit identity-skip), ONE wall out.
	 */
	@Nullable
	public static Recipe buildRecipe(WelderWallRow aRow) {
		gregapi.oredict.OreDictMaterial tMaterial = sMaterialResolver.apply(aRow.materialName());
		ItemStack tPlate = tMaterial == null ? null : sPlateResolver.apply(aRow.platePrefix(), tMaterial);
		if (tPlate == null || tPlate.isEmpty()) return null;
		tPlate = tPlate.copy();
		tPlate.setCount(PLATE_COUNT);
		ItemStack tSelector = sSelectorResolver.apply(SELECTOR_CONFIG); // ST.tag(10) — the wall-welding mode
		net.minecraft.world.item.Item tWallItem = sOutputResolver.apply(aRow.wallPath());
		if (tWallItem == null) return null;
		// NOTE the port Recipe ctor order: (duration, EUt) — the P14 lesson.
		return new Recipe(false,
				new ItemStack[] {tPlate, tSelector},
				new ItemStack[] {new ItemStack(tWallItem, 1)},
				new net.minecraftforge.fluids.FluidStack[0], new net.minecraftforge.fluids.FluidStack[0],
				aRow.dense() ? DENSE_DURATION : WALL_DURATION,
				aRow.dense() ? DENSE_EUT : WALL_EUT, 0);
	}

	/** The test seam reset (the distillery form: the generation flag drops, the table stays). */
	public static void resetForTest() {sLoaded = false;}

	private GT6RecipesWelder() {}
}
