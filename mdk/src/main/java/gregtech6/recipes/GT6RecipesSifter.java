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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The Sifter recipe book (task p26-w1-sifter-compressor-wiremill) — the row audit of the
 * RM.Sifting pour surface. Upstream feeders outside compat/:
 *
 * <ul>
 *   <li><b>Loader_Recipes_Furnace.java:71/:83/:89</b> — the audit found all three
 *       {@code RM.Sifting.addRecipe1(F, 16, 200, ...)} rows INSIDE the
 *       {@code MD.RoC.owns(tReg1, "extracts")} RotaryCraft branch of the furnace-recipe
 *       walk (:60 gate) — they are compat rows and stay CUT under the P10 ruling. The
 *       card's "Furnace:71/83/89 dust loop" row source therefore lands in
 *       {@link #SKIPPED_UPSTREAM} rather than the pour set.</li>
 *   <li><b>Loader_Recipes_Ores.java:224</b> — the vanilla grass row, the ONE resolvable
 *       pour: grass block → coarse dirt + wheat/melon/pumpkin seeds + the trailing seed
 *       tail, eUt 16, duration 144, chances {8000, 2000, 1000, 1000, 1000}. The 1.7.10
 *       {@code ST.make(Blocks.dirt, 1, 1)} meta-1 identity is 1.20.1
 *       {@link Blocks#COARSE_DIRT}; the EtFu beet-seed tail is the 1.20.1
 *       {@link Items#BEETROOT_SEEDS} identity (the p10-compat-vanilla-rows
 *       identity-mapping precedent). The trailing foreign outputs (IL.BoP_Turnip_Seeds,
 *       the MaCu bait items) are CUT — the chances array trims with them (a declared
 *       prefix-trim deviation).</li>
 *   <li><b>Loader_OreProcessing.java:351</b> — the DUST_ORE sifting template, LANDED here
 *       (task debt-ore-purified-edge): the {@code OreProcessing_Ore} listener
 *       (:196-197, the ORE-tagged prefixes minus bedrock/poor/small/rich/normal — the
 *       crushed family carries {@code ORE_PROCESSING_BASED}, not ORE, so it never fires
 *       there) pours {@code RM.Sifting.addRecipe1(T, 16, 256, {10000,10000,1500,1000,500},
 *       oreBlock, 2x crushedPurified, 3x tiny-byproduct-dust)} for every
 *       {@link TD.Prefix#DUST_ORE} prefix (OP.java:115-119: oreGravel/oreStrangesand/
 *       oreRedSand/oreSand/oreMud — the SAND-family ore BLOCKS, not crushed ores; this is
 *       the upstream's ONLY crushedPurified producer outside the PFAA compat rows). The
 *       walk rides {@link GT6OreBlocks#FAMILIES} (the {@code contains(DUST_ORE)} gate, no
 *       hardcoded names) x {@link GT6OreBlocks#materialAxis()} — 4 of the 5 upstream
 *       prefixes have port families (gravel/sand/redsand/mud; oreStrangesand has none =
 *       the declared zero-row remainder), the material axis is self-crushing for all 53
 *       members (the twelve {@code setCrushing} redirecters of MT.java:2884-2895 are all
 *       off-axis), so every row is a same-material ore-block → crushedPurified hop.</li>
 *   <li><b>Loader_Recipes_Handlers.java:62</b> — the pebbles→dust x3 template: zero
 *       pours, OP.pebbles is not on the port MaterialPrefixItem path.</li>
 * </ul>
 *
 * <p>The Sifting map itself is a plain base {@link RecipeMap} upstream (RM.java:83 — no
 * subclass deviation, unlike Shredder/Chisel) with the one non-0 progress-bar direction
 * (2, 1) in the RM.java:60-115 block.
 *
 * <p><b>Load timing</b>: the GT6RecipesWiremill/ShCL form — self-contained MOD-bus
 * listener at FMLCommonSetup.enqueueWork, lazily built tables, generation-tracked pour flag.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesSifter {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One transcribed fixed row (the GT6RecipesShCL.FixedRow shape). */
	public record FixedRow(String note, Slot input, long eUt, long duration, @Nullable long[] chances, Slot... outputs) {
		/** The compact deterministic form (null chances). */
		public FixedRow(String note, Slot input, long eUt, long duration, Slot... outputs) {
			this(note, input, eUt, duration, null, outputs);
		}
	}

	/** One input/output slot of a fixed row: a vanilla item reference or a (prefix, material) pair. */
	public record Slot(@Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int count) {
		public static Slot vanilla(Supplier<Item> aItem, int aCount) { return new Slot(aItem, null, null, aCount); }
		public static Slot material(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) { return new Slot(null, aPrefix, aMaterial, aCount); }
	}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline (the ShCL precedent). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesSifter::resolveItem;
	/** The vanilla item seam (only for offline determinism; live it just dereferences the supplier). */
	static Function<Supplier<Item>, Item> sVanillaItemResolver = Supplier::get;

	/**
	 * The transcribed rows. <b>Lazily built</b> — the {@code @EventBusSubscriber} scan
	 * class-loads at MOD CONSTRUCTION, before MT.init() (the a9027ac lesson).
	 */
	private static volatile List<FixedRow> sRows = null;

	/** The transcribed rows, captured on first use (one material generation). */
	public static List<FixedRow> table() {
		List<FixedRow> tTable = sRows;
		if (tTable == null) sRows = tTable = List.of(
		// Loader_Recipes_Ores.java:224 — the vanilla grass row0; the chances prefix-trim with the
		// cut foreign tail (BoP turnip seeds + the three MaCu baits rode chances 1000/500/500/500)
		new FixedRow(":224", Slot.vanilla(() -> Blocks.GRASS_BLOCK.asItem(), 1), 16, 144,
				new long[] {8000, 2000, 1000, 1000, 1000},
				Slot.vanilla(() -> Blocks.COARSE_DIRT.asItem(), 1),
				Slot.vanilla(() -> Items.WHEAT_SEEDS, 1),
				Slot.vanilla(() -> Items.MELON_SEEDS, 1),
				Slot.vanilla(() -> Items.PUMPKIN_SEEDS, 1),
				Slot.vanilla(() -> Items.BEETROOT_SEEDS, 1)));
		return tTable;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Furnace.java:71/:83/:89 — the AUDIT FINDING: all three Sifting rows sit inside the MD.RoC.owns(\"extracts\") RotaryCraft branch of the furnace walk (:60) — compat rows, CUT per the P10 ruling (the card row-source citation resolves to the pool, not the pour set)",
			"Loader_OreProcessing.java:351 oreStrangesand leg — the DUST_ORE template LANDED (task debt-ore-purified-edge, the pour walk in load()); of the five upstream DUST_ORE prefixes (OP.java:115-119) oreStrangesand alone has NO port family in GT6OreBlocks.FAMILIES — a declared zero-row remainder, not an invented family",
			"Loader_Recipes_Handlers.java:62 — the pebbles→dust x3 template pours ZERO rows: OP.pebbles is not on the port MaterialPrefixItem path",
			"Loader_Recipes_Ores.java:223/:225 — the BoP_Smoldering / BlocksGT.Grass inputs: foreign/GT-block identities with no port block",
			"Loader_Recipes_Ores.java:224 trailing outputs — IL.BoP_Turnip_Seeds and the MaCu bait items: foreign-mod identities, CUT; the chances array prefix-trims to the five kept outputs (declared deviation)",
			"Loader_Recipes_Temporary.java:588-607 — the PFAA/RH sand rows: IL inputs, no port identity",
			"MultiTileEntitySiftingTable.java — RM.Sifting is the manual table's live map, no rows added there (the kitchen card pool)",
			"all Compat_Recipes_* RM.Sifting feeders (Tropicraft etc.) — the P10 ruling (59 compat classes not ported)");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the GT6RecipesShCL form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesSifter::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesSifter::load);
	}

	/** Pours the tables into the SIFTING map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines)
		if (GT6RecipeMaps.SIFTING == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (FixedRow tRow : table()) {
			Recipe tRecipe = buildFixedRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // = upstream mat() → null silent drop
			GT6RecipeMaps.SIFTING.addRecipe(tRecipe);
			tPoured++;
		}
		for (GT6OreBlocks.OreFamily tFamily : dustOreFamilies()) { // Loader_OreProcessing.java:351 — the DUST_ORE template (:196-197 listener family)
			for (OreDictMaterial tMaterial : GT6OreBlocks.materialAxis()) {
				Recipe tRecipe = buildDustOreRecipe(planDustOre(tFamily, tMaterial));
				if (tRecipe == null) {tSkipped++; continue;} // the input ore-block / crushedPurified mat() null drops
				GT6RecipeMaps.SIFTING.addRecipe(tRecipe);
				tPoured++;
			}
		}
		LOGGER.info("GT6 Sifter recipes poured: {} loaded, {} skipped (unresolvable prefix/material items, = upstream mat() null drops; the RoC/pebbles/PFAA arms are the declared zero-pour pools)", tPoured, tSkipped);
		sLoaded = true;
	}

	// ------------------------------------------------------------------
	// the Loader_OreProcessing.java:351 DUST_ORE row family (task debt-ore-purified-edge)
	// ------------------------------------------------------------------

	/**
	 * The pour families of the :351 template: the port ore-block families whose prefix
	 * carries {@link TD.Prefix#DUST_ORE} (the upstream OP.java:115-119 gate, no hardcoded
	 * names) — gravel/sand/redsand/mud; oreStrangesand has no port family (the declared
	 * SKIPPED_UPSTREAM remainder).
	 */
	public static List<GT6OreBlocks.OreFamily> dustOreFamilies() {
		List<GT6OreBlocks.OreFamily> rFamilies = new ArrayList<>();
		for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
			if (tFamily.prefix().contains(TD.Prefix.DUST_ORE)) rFamilies.add(tFamily);
		}
		return rFamilies;
	}

	/**
	 * The pure row planner of the :351 arm (upstream {@code registerStandardOreRecipes}
	 * :308-351, no item resolution). {@code outMaterial} = the {@code mTargetCrushing}
	 * redirect of :311 — self for every axis material (the twelve MT.java:2884-2895
	 * redirecters are all off-axis), cross-material only when a redirecter ever joins the
	 * axis. {@code tinyMaterials} = the three :331-343 byproduct tiny tiers at the material
	 * level: the first three of {@code outMaterial.mByProducts}, padded by repeat-last,
	 * an empty list padding to the material itself (:341-343).
	 */
	public record DustOrePlan(GT6OreBlocks.OreFamily family, OreDictMaterial inMaterial, OreDictMaterial outMaterial, long multiplier, List<OreDictMaterial> tinyMaterials) {}

	/** Plans one (family, material) row — the :302/:311/:314/:328-343 walk, pure material data. */
	public static DustOrePlan planDustOre(GT6OreBlocks.OreFamily aFamily, OreDictMaterial aMaterial) {
		OreDictMaterial tOut = aMaterial.mTargetCrushing.mMaterial; // :311
		long tMultiplier = GT6RecipesOreChain.bindStack(tOut.mOreMultiplier * aMaterial.mOreProcessingMultiplier); // :302 x :314 (DUST_ORE prefixes are never DENSE_ORE)
		List<OreDictMaterial> tTinies = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			OreDictMaterial tTiny = i < tOut.mByProducts.size() ? tOut.mByProducts.get(i) // :331-333
					: tTinies.isEmpty() ? tOut : tTinies.get(tTinies.size() - 1); // :341-343 — empty → the material itself, else repeat-last
			tTinies.add(tTiny);
		}
		return new DustOrePlan(aFamily, aMaterial, tOut, tMultiplier, List.copyOf(tTinies));
	}

	/**
	 * Plan → Recipe (the :351 tail), or {@code null} when the input ore block or the
	 * crushedPurified main output fails to resolve (the {@code mat()} → null drop, the
	 * GT6RecipesOreChain :81 precedent). Outputs: the main output twice at 10000/10000,
	 * the three tiny byproduct dusts at 1500/1000/500 — {@code OM.dustOrIngot(mat, U9)}
	 * simplified to the plain {@link OP#dustTiny} tier (the OreChain :327 byproduct
	 * simplification precedent; count = the multiplier, upstream {@code ST.amount}); an
	 * unresolvable tiny stays a POSITIONAL null slot exactly as the upstream output array
	 * carries it (the port Recipe ctor trims trailing nulls and aligns chances).
	 */
	@Nullable
	static Recipe buildDustOreRecipe(DustOrePlan aPlan) {
		Item tInput = sMaterialItemResolver.apply(aPlan.family().prefix(), aPlan.inMaterial());
		if (tInput == null) return null; // upstream mat() → null
		ItemStack tMain = resolveStack(OP.crushedPurified, aPlan.outMaterial(), aPlan.multiplier());
		if (tMain == null) return null; // :351 mat() → null drop
		ItemStack[] tOutputs = new ItemStack[5];
		tOutputs[0] = tMain;
		tOutputs[1] = tMain.copy(); // the second 10000 main output
		for (int i = 0; i < 3; i++) tOutputs[2 + i] = resolveStack(OP.dustTiny, aPlan.tinyMaterials().get(i), aPlan.multiplier());
		return new Recipe(true, new ItemStack[] {new ItemStack(tInput, 1)}, tOutputs, new FluidStack[0], new FluidStack[0], 256, 16, 0, new long[] {10000, 10000, 1500, 1000, 500}); // :351 — eUt 16, duration 256
	}

	/** Prefix x material x count → ItemStack, or null when the pair has no item (the upstream mat() semantics). */
	@Nullable
	private static ItemStack resolveStack(OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aCount) {
		Item tItem = sMaterialItemResolver.apply(aPrefix, aMaterial);
		return tItem == null ? null : new ItemStack(tItem, (int) aCount);
	}

	/** Fixed-row → Recipe, or null when any segment fails to resolve (the upstream silent-drop semantics). */
	static Recipe buildFixedRecipe(FixedRow aRow) {
		ItemStack tInput = resolveSlot(aRow.input());
		if (tInput == null) return null;
		ItemStack[] tOutputs = new ItemStack[aRow.outputs().length];
		for (int i = 0; i < tOutputs.length; i++) {
			tOutputs[i] = resolveSlot(aRow.outputs()[i]);
			if (tOutputs[i] == null) return null;
		}
		return new Recipe(true, new ItemStack[] {tInput}, tOutputs, new FluidStack[0], new FluidStack[0], aRow.duration(), aRow.eUt(), 0, aRow.chances());
	}

	/** Slot → ItemStack, or null when the segment fails to resolve (the upstream silent-drop semantics). */
	@Nullable
	private static ItemStack resolveSlot(Slot aSlot) {
		Item tItem = aSlot.vanilla() != null
				? sVanillaItemResolver.apply(aSlot.vanilla())
				: sMaterialItemResolver.apply(aSlot.prefix(), aSlot.material());
		return tItem == null ? null : new ItemStack(tItem, aSlot.count());
	}

	/**
	 * The live item lookup — {@link GTMaterialItems#get :287} first, then the ore-BLOCK
	 * path: the DUST_ORE inputs (oreGravel/sand/redsand/mud normal-form blocks) are block
	 * prefixes with no item-path items, so the :351 walk resolves them through
	 * {@link GT6OreBlocks#items()} (the NORMAL form; broken≡normal upstream for the dust
	 * families). Null when neither carries the pair.
	 */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		if (tHandle != null) return tHandle.get();
		for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
			if (tFamily.prefix() != aPrefix) continue;
			var tBlockItem = GT6OreBlocks.items().get(new GT6OreBlocks.OreKey(tFamily, GT6OreBlocks.FormKind.NORMAL, aMaterial));
			return tBlockItem == null ? null : tBlockItem.get();
		}
		return null;
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sRows = null;
	}
}
