package gregtech6.tileentity.machines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * Offline boot + fixtures for the TileEntityBasicMachine suite (task p7-basicmachine-family
 * ⑦, the GTMachinesOfflineTestBase :36 precedent): the vanilla registries boot through the
 * oven base, and each test pours a REPRESENTATIVE recipe chain per map straight through the
 * public {@code Recipe}/{@code RecipeMap.addRecipe} face — the real GT6RecipesShCL pour is
 * W1's tested surface (its resolver seam is package-private to gregtech6.recipes) and gets
 * its end-to-end validation live via the RCON chains.
 *
 * <p>The synthetic (prefix, material) → vanilla-item universe follows the
 * GT6RecipesShCLTest pattern: new Items cannot be created offline (the intrusive vanilla
 * registry freezes at bootstrap), so pairs map onto distinct existing entries; the recipe
 * mechanics only compare identities. Per test the maps are FRESH (the inherited
 * init/reset pair), so the poured row sets are exactly the ones below — deterministic.
 */
public abstract class TileEntityBasicMachineOfflineTestBase extends GTMachinesOfflineTestBase {

	/** The synthetic offline universe: one distinct EXISTING item per (prefix, material) pair. */
	static final Map<PrefixMaterial, Item> SYNTHETIC_ITEMS = new HashMap<>();

	/**
	 * The vanilla items the all-vanilla table rows use as inputs — kept out of the synthetic
	 * pool so a lookup with one of them can only ever match its own row (the
	 * GT6RecipesShCLTest reservation list).
	 */
	private static final java.util.Set<Item> RESERVED_VANILLA_ITEMS = java.util.Set.of(
			Items.FLINT, Items.GRAVEL, Items.SAND, Items.COBWEB, Items.STRING,
			Items.COBBLESTONE, Items.STONE, Items.GLASS_PANE);

	/** The first gem-chain material of the registration order (the RCON crusher feed picks the same way). */
	static OreDictMaterial sGemMaterial;
	/** The synthetic item minted for (gem, sGemMaterial). */
	static Item sGemItem;
	/** The synthetic item minted for (gemFlawed, sGemMaterial). */
	static Item sGemFlawedItem;
	/** The synthetic item minted for (dust, MT.Stone) — the :692 cobblestone output. */
	static Item sDustStoneItem;
	/** The synthetic item minted for (stickLong, MT.Stone) — the :524 lathe output. */
	static Item sStickLongStoneItem;

	private static boolean sUniverseBuilt = false;

	@BeforeAll
	static void buildSyntheticUniverse() {
		if (sUniverseBuilt) return;
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		List<Item> tPool = BuiltInRegistries.ITEM.stream().toList();
		int tNext = 0;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			Item tItem;
			// AIR (or any item making an empty stack) must be skipped: new ItemStack(AIR, n) is an
			// empty stack, the Recipe ctor trims it, and the resulting empty-input row would match
			// EVERY lookup (the ghost-recipe lesson, GT6RecipesShCLTest).
			do {tItem = tPool.get(tNext++ % tPool.size());} while (RESERVED_VANILLA_ITEMS.contains(tItem) || new ItemStack(tItem, 1).isEmpty());
			SYNTHETIC_ITEMS.put(tPair, tItem);
		}
		// Material lookups go through the REGISTRATION ORDER (the kept pairs), not the MT
		// constants — the alias merge can make a constant's instance differ from the
		// registration target, so a direct map key lookup is not safe (the merge lesson,
		// MaterialRegistry.java:182-185). Pick the first material of each prefix that exists.
		OreDictMaterial tDustMaterial = null, tStickLongMaterial = null;
		sGemMaterial = null;
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == OP.dust && tDustMaterial == null) tDustMaterial = tPair.material();
			if (tPair.prefix() == OP.stickLong && tStickLongMaterial == null) tStickLongMaterial = tPair.material();
			if (tPair.prefix() == OP.gem && sGemMaterial == null
					&& SYNTHETIC_ITEMS.containsKey(new PrefixMaterial(OP.gemFlawed, tPair.material()))) {
				sGemMaterial = tPair.material(); // the gem chain needs BOTH ends resolvable (:72)
			}
		}
		sGemItem = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gem, sGemMaterial));
		sGemFlawedItem = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.gemFlawed, sGemMaterial));
		sDustStoneItem = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.dust, tDustMaterial));
		sStickLongStoneItem = SYNTHETIC_ITEMS.get(new PrefixMaterial(OP.stickLong, tStickLongMaterial));
		sUniverseBuilt = true;
	}

	/**
	 * Pours the representative chains into the FRESH maps (Loader_Recipes_Vanilla.java:689/
	 * :692/:524 + Loader_Recipes_Handlers.java:72 — eUt 16, duration 16, the T1 scale): one
	 * vanilla row per machine plus the material rows the chain tests consume.
	 */
	@BeforeEach
	void pourMachineChains() {
		RecipeMap tShredder = GT6RecipeMaps.SHREDDER;
		tShredder.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.GRAVEL, 1)},
				new ItemStack[] {new ItemStack(Items.SAND, 1)}, null, null, 16, 16, 0)); // :689
		tShredder.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.COBBLESTONE, 1)},
				new ItemStack[] {new ItemStack(sDustStoneItem, 9)}, null, null, 16, 16, 0)); // :692

		GT6RecipeMaps.CRUSHER.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(sGemItem, 1)},
				new ItemStack[] {new ItemStack(sGemFlawedItem, 2)}, null, null, 16, 16, 0)); // :72

		GT6RecipeMaps.LATHE.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.STONE, 1)},
				new ItemStack[] {new ItemStack(sStickLongStoneItem, 1)}, null, null, 16, 16, 0)); // :524
	}

	/**
	 * The machine-family regime switch resets to the SHIPPED default (TRUE) per test — the
	 * class-execution-order guard, same job as the oven switch restore above (task
	 * p8-machine-tiers-doinject ④: the net-mode tests flip it off at their own start).
	 */
	@BeforeEach
	void initMachineFakeSource() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = true;
	}

	// ------------------------------------------------------------------
	// fixtures
	// ------------------------------------------------------------------

	/** An offline BET carrying the machine config in its factory closure (Builder.of().build(null), oven precedent). */
	static BlockEntityType<TileEntityBasicMachine> machineType(RecipeMap aMap, int aParallel, boolean aParallelDuration) {
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, aMap, aParallel, aParallelDuration, null),
				Blocks.BRICKS).build(null);
		return tHolder[0];
	}

	/** Offline machine with a level attached (applyVisualState no-ops: the fixture block is not a GTBasicMachineBlock). */
	static TileEntityBasicMachine makeMachine(RecipeMap aMap, int aParallel, boolean aParallelDuration) {
		return makeMachine(aMap, aParallel, aParallelDuration, gregapi.data.TD.Energy.RU); // the Shredder/Lathe row carrier (:1294/:1306)
	}

	/** The same with an explicit energy-type carrier (the machine() factory assigns it per row). */
	static TileEntityBasicMachine makeMachine(RecipeMap aMap, int aParallel, boolean aParallelDuration, gregapi.code.TagData aEnergyType) {
		TileEntityBasicMachine tMachine = machineType(aMap, aParallel, aParallelDuration).create(POS, Blocks.BRICKS.defaultBlockState());
		tMachine.setLevel(emptyLevel());
		tMachine.mEnergyTypeAccepted = aEnergyType;
		return tMachine;
	}

	/** Drives the 03 dispatcher for {@code aTicks} ticks. */
	static void drive(TileEntityBasicMachine aMachine, int aTicks) {
		for (int i = 0; i < aTicks; i++) aMachine.updateEntity();
	}
}
