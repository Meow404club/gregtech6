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

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Shredder / Crusher / Lathe recipe book — task p7-recipe-maps-shcl (first deterministic
 * static batch for the basicmachine-family BE card), extended by task
 * p10-compat-vanilla-rows (the vanilla backfill of the machine-family compat research: the
 * SHREDDER bone row + the four CRUSHER {@code stone*} rows — after these, the in-port
 * 5-map vanilla coverage gap of the compat research is closed).
 *
 * <p><b>Upstream sources</b>: {@code RM.Shredder.addRecipe1} rows in
 * Loader_Recipes_Vanilla.java:688-693 + :697 + :707-708 (Shredder), :523-524 (Lathe), the
 * gem-chain {@code RecipeMapHandlerPrefix} rows in Loader_Recipes_Handlers.java:69-73/:75
 * (Crusher), and the OreDict {@code stone*} listener Crusher rows in
 * Loader_Recipes_OreDict.java:82-96 (Crusher, the p10 backfill). The p7 batch is fully
 * deterministic — null chances; the p10 backfill rows :82/:88 carry the upstream
 * {@code new long[] {...}} chance literals through {@link Recipe#mChances} (the P8
 * 9-arg ctor; {@code getOutputs(Random, int)} Bernoulli semantics).
 *
 * <p><b>Row shapes</b>: the Shredder/Lathe rows mirror the upstream
 * {@code addRecipe1(aOptimize, aEUt, aDuration, input, outputs...)} calls verbatim — fixed
 * eUt and duration. The Crusher rows mirror the
 * {@code RecipeMapHandlerPrefix(gemLegendary, 1, null, 0, NF, 16, 0, 256, NF, gemExquisite, 2, ...)}
 * 19-arg form (RecipeMapHandlerPrefix.java:82): eUt = 16, mDuration = 0 → the duration is
 * computed per material by getCosts (:225-227) —
 * {@code units(max(unitsIn, unitsOut), U, multiplier + multiplier*mToolQuality, T)} — which is
 * quantity-conserving across the gem chain (gemLegendary 8U = 2x gemExquisite 4U, etc.), so
 * every row resolves to {@code units * 256 * (1+mToolQuality)} ticks. The
 * {@code mOutputPulverizedRemains} secondary output (upstream :215) is null for these rows:
 * mUnitsInputted - mUnitsOutputted = 0, and OM.pulverize(mat, 0) produces nothing.
 *
 * <p><b>The upstream registration form silently drops a row when the (prefix, material) pair
 * has no item ({@code mat()} → null); the port keeps that shape: the tables below are DATA,
 * and {@link #load()} resolves every segment through {@link GTMaterialItems#get} at pour time,
 * skipping + counting unresolvable rows (the GT6RecipesCokeOven precedent). The gem-chain
 * material walk is the port counterpart of
 * {@code RecipeMapHandlerPrefix.addAllRecipesInternal :173} — it iterates the prefix's
 * registered materials via {@link GTMaterialItems#registrationOrder} (the port's single
 * enumeration source; the ported {@code OreDictPrefix.mRegisteredMaterials} field has no
 * fill-in yet), applying the upstream condition gate {@code ANTIMATTER.NOT} +
 * {@code INVALID_MATERIAL} (addRecipeForMaterial :205).
 *
 * <p><b>Skipped upstream rows (the pool, not silent — declared in {@link #SKIPPED_UPSTREAM})</b>:
 * the IL/probability Shredder rows, the Crusher prefix rows with null outputs (:64/:74 — the
 * mTargetCrushing pulverize-remains semantics) and the crushed-family handler chain, the
 * RECYCLABLE on-demand synthesis of RecipeMapShredder.getRecipeFor (RecipeMapShredder.java:47-64,
 * a recipe-POOL feature), the OreDict wood loop, the Furnace fallback bridge and all compat.
 *
 * <p><b>Load timing</b> (the GT6RecipesCokeOven precedent + the a9027ac lesson): a
 * self-contained MOD-bus listener pouring at FMLCommonSetup.enqueueWork. The
 * {@code @EventBusSubscriber} annotation scan class-loads this class at MOD CONSTRUCTION —
 * before {@code MT.init()} — so the OP/MT-referencing tables are built lazily by
 * {@code table()} on first load, never in static initializers. {@code load()} is idempotent
 * per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesShCL {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One input/output slot of a table row: a vanilla item reference or a (prefix, material) pair. */
	public record Slot(@Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int count) {
		public static Slot vanilla(Supplier<Item> aItem, int aCount) { return new Slot(aItem, null, null, aCount); }
		public static Slot material(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) { return new Slot(null, aPrefix, aMaterial, aCount); }
	}

	/**
	 * One transcribed Shredder/Lathe/Crusher-vanilla row (the upstream
	 * {@code addRecipe1(aOptimize, aEUt, aDuration, [chances,] input, outputs...)} call).
	 * {@code note} carries the upstream line number.
	 *
	 * <p>{@code chances} is the p10-compat-vanilla-rows extension: the chance-bearing rows
	 * (OreDict:82/:88) carry the upstream {@code new long[] {...}} literal through
	 * {@link Recipe#mChances} (the P8 9-arg ctor, chances tail-appended). {@code null} = the
	 * deterministic shape — what every pre-p10 row uses via the compact constructor.
	 */
	public record FixedRow(String note, Slot input, long eUt, long duration, @Nullable long[] chances, Slot... outputs) {
		/** The compact deterministic form (null chances) — the pre-p10 row shape. */
		public FixedRow(String note, Slot input, long eUt, long duration, Slot... outputs) {
			this(note, input, eUt, duration, null, outputs);
		}
	}

	/**
	 * One transcribed Crusher prefix-handler template (the upstream
	 * {@code RecipeMapHandlerPrefix(inPrefix, inCount, null, 0, NF, eUt, 0, multiplier, NF,
	 * outPrefix, outCount, null, 0, NI, NI, T, T, F, ANTIMATTER.NOT)} 19-arg form), expanded
	 * per material at pour time.
	 */
	public record CrusherTemplate(String note, OreDictPrefix inPrefix, int inCount, OreDictPrefix outPrefix, int outCount, long eUt, long multiplier) {}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline. */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesShCL::resolveItem;
	/** The vanilla item seam (only for offline determinism; live it just dereferences the supplier). */
	static Function<Supplier<Item>, Item> sVanillaItemResolver = Supplier::get;

	/**
	 * The transcribed rows (Loader_Recipes_Vanilla.java:688-693 + :697 + :707-708), order
	 * mirroring the upstream file order. All eUt 16; :692/:693 carry the
	 * {@code OM.dust(MT.Stone, U*9)} nine-dust output; :697 is the p10-compat-vanilla-rows
	 * backfill (bone → 4 bonemeal — the upstream {@code IL.Dye_Bonemeal} item is the
	 * 1.7.10 white-dye meta, whose 1.20.1 identity is {@link Items#BONE_MEAL}).
	 *
	 * <p><b>Lazily built</b>: the {@code @EventBusSubscriber} annotation scan class-loads this
	 * class at MOD CONSTRUCTION — before {@code MT.init()} — so OP/MT references must not be
	 * captured in static initializers (the a9027ac lesson: the live server poured 0/24 until
	 * the cokeoven table went lazy). Vanilla item references stay behind suppliers too, so
	 * nothing dereferences before the first load call.
	 */
	private static volatile List<FixedRow> sShredderRows = null;

	/** The transcribed Shredder rows, captured on first use (one material generation). */
	public static List<FixedRow> shredderTable() {
		List<FixedRow> tTable = sShredderRows;
		if (tTable == null) sShredderRows = tTable = List.of(
		// Loader_Recipes_Vanilla.java:688-693
		new FixedRow(":688", Slot.vanilla(() -> Items.FLINT, 1)          , 16,  16, Slot.material(OP.dust     , MT.Flint, 1)),
		new FixedRow(":689", Slot.vanilla(() -> Blocks.GRAVEL.asItem(), 1), 16,  16, Slot.vanilla(() -> Blocks.SAND.asItem(), 1)),
		new FixedRow(":690", Slot.vanilla(() -> Blocks.COBWEB.asItem(), 1), 16,  16, Slot.vanilla(() -> Items.STRING, 1)),
		new FixedRow(":692", Slot.vanilla(() -> Blocks.COBBLESTONE.asItem(), 1), 16, 16, Slot.material(OP.dust, MT.Stone, 9)),
		new FixedRow(":693", Slot.vanilla(() -> Blocks.STONE.asItem(), 1)  , 16,  16, Slot.material(OP.dust     , MT.Stone, 9)),
		// Loader_Recipes_Vanilla.java:697 — the p10 backfill; :698 WiMo_Thick_Bone and :699 melon stay CUT (see SKIPPED_UPSTREAM)
		new FixedRow(":697", Slot.vanilla(() -> Items.BONE, 1)             , 16,  32, Slot.vanilla(() -> Items.BONE_MEAL, 4)),
		// Loader_Recipes_Vanilla.java:707-708 — the MT.Blaze representative of the ANY.Blaze.mToThis loop (the group expansion is pooled)
		new FixedRow(":707", Slot.material(OP.stick    , MT.Blaze, 1)     , 16,  32, Slot.material(OP.dustSmall, MT.Blaze, 2)),
		new FixedRow(":708", Slot.material(OP.stickLong, MT.Blaze, 1)     , 16,  64, Slot.material(OP.dust     , MT.Blaze, 1)));
		return tTable;
	}

	/** The transcribed Lathe rows (Loader_Recipes_Vanilla.java:523-524); :525-527 are IL rows (pooled). */
	private static volatile List<FixedRow> sLatheRows = null;

	/** The transcribed Lathe rows, captured on first use (one material generation). */
	public static List<FixedRow> latheTable() {
		List<FixedRow> tTable = sLatheRows;
		if (tTable == null) sLatheRows = tTable = List.of(
		new FixedRow(":523", Slot.vanilla(() -> Blocks.GLASS_PANE.asItem(), 1), 16, 16, Slot.material(OP.lens     , MT.Glass, 1), Slot.material(OP.dustSmall, MT.Glass, 1)),
		new FixedRow(":524", Slot.vanilla(() -> Blocks.STONE.asItem(), 1)     , 16, 16, Slot.material(OP.stickLong, MT.Stone, 1)));
		return tTable;
	}

	/** The transcribed Crusher gem-chain templates (Loader_Recipes_Handlers.java:69-73 + :75). */
	private static volatile List<CrusherTemplate> sCrusherTemplates = null;

	/** The transcribed Crusher templates, captured on first use (one material generation). */
	public static List<CrusherTemplate> crusherTable() {
		List<CrusherTemplate> tTable = sCrusherTemplates;
		if (tTable == null) sCrusherTemplates = tTable = List.of(
		// Loader_Recipes_Handlers.java:69-73 — the quantity-conserving gem chain
		new CrusherTemplate(":69", OP.gemLegendary , 1, OP.gemExquisite, 2, 16, 256),
		new CrusherTemplate(":70", OP.gemExquisite , 1, OP.gemFlawless , 2, 16, 256),
		new CrusherTemplate(":71", OP.gemFlawless  , 1, OP.gem         , 2, 16, 256),
		new CrusherTemplate(":72", OP.gem          , 1, OP.gemFlawed   , 2, 16, 256),
		new CrusherTemplate(":73", OP.gemFlawed    , 1, OP.gemChipped  , 2, 16, 256),
		// :75 — the boule row (upstream OP.bouleGt is condition-FALSE, so the walk expands to zero materials; the row stays transcribed as DATA)
		new CrusherTemplate(":75", OP.bouleGt      , 1, OP.gem         , 4, 16, 256));
		return tTable;
	}

	/**
	 * The p10-compat-vanilla-rows Crusher backfill: the four vanilla rows of the
	 * {@code stone*} OreDict listeners (Loader_Recipes_OreDict.java:82/:88/:92/:96), in
	 * upstream file order. Upstream these fire as oredict-listener events (the Forge 1.7.10
	 * vanilla registrations "stoneNetherrack"/"stoneEndstone"/"stoneNetherBrick" plus the GT
	 * {@code blockSolidObsidian} registration); the port transcribes the static equivalents.
	 *
	 * <p><b>Input identity</b>: :88/:92/:96 bind to the vanilla blocks (Blocks.NETHER_BRICKS /
	 * NETHERRACK / END_STONE — the 1.20.1 identities of the 1.7.10 vanilla oredict names).
	 * :82 upstream input is the GT {@code blockSolidObsidian} block, but the port block
	 * universe generates NO blockSolid Obsidian — the {@code blockSolid → blockIngot → ingot
	 * → ITEMGENERATOR.INGOTS} condition chain fails for the STONE-family material (offline
	 * probe: {@code OP.blockSolid.isGeneratingItem(MT.Obsidian) == false}) — so the input is
	 * the vanilla obsidian block: declared deviation, same gameplay identity (GT6 1.7.10
	 * worldgen replaces vanilla obsidian with that block).
	 *
	 * <p><b>Output identity</b>: the :82 fallback chain
	 * {@code IL.RC_Crushed_Obsidian.get(1, IL.HBM_Crushed_Obsidian.get(1, dust Obsidian x8))}
	 * resolves to its dust tail — the RC/HBM crushed items are foreign-mod items that do not
	 * exist in this port (declared, no IL surface). :92/:96 produce {@code OP.rockGt} x4, and
	 * (rockGt, Netherrack/Endstone) both resolve inside the port item universe.
	 */
	private static volatile List<FixedRow> sCrusherVanillaRows = null;

	/** The transcribed Crusher vanilla rows, captured on first use (one material generation). */
	public static List<FixedRow> crusherVanillaTable() {
		List<FixedRow> tTable = sCrusherVanillaRows;
		if (tTable == null) sCrusherVanillaRows = tTable = List.of(
		// Loader_Recipes_OreDict.java:82 — chances {10000, 2500}, duration 600
		new FixedRow(":82", Slot.vanilla(() -> Blocks.OBSIDIAN.asItem(), 1), 16, 600, new long[] {10000, 2500},
				Slot.material(OP.dust, MT.Obsidian, 8), Slot.material(OP.dust, MT.Obsidian, 1)),
		// Loader_Recipes_OreDict.java:88 — four independent single-brick slots at descending certainty
		new FixedRow(":88", Slot.vanilla(() -> Blocks.NETHER_BRICKS.asItem(), 1), 16, 16, new long[] {10000, 9000, 8000, 7000},
				Slot.vanilla(() -> Items.NETHER_BRICK, 1), Slot.vanilla(() -> Items.NETHER_BRICK, 1),
				Slot.vanilla(() -> Items.NETHER_BRICK, 1), Slot.vanilla(() -> Items.NETHER_BRICK, 1)),
		// Loader_Recipes_OreDict.java:92 — deterministic
		new FixedRow(":92", Slot.vanilla(() -> Blocks.NETHERRACK.asItem(), 1), 16, 16, Slot.material(OP.rockGt, MT.Netherrack, 4)),
		// Loader_Recipes_OreDict.java:96 — deterministic
		new FixedRow(":96", Slot.vanilla(() -> Blocks.END_STONE.asItem(), 1), 16, 16, Slot.material(OP.rockGt, MT.Endstone, 4)));
		return tTable;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc). Everything
	 * here is a POOL item of the machine-family wave, not a silent drop.
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
		"Loader_Recipes_Vanilla.java:691 reeds → IL.Remains_Plant and :699 melon 6000-chance → IL.Remains_Fruit: CUT — the GT Remains_* items do not exist in this port (p10-compat-vanilla-rows; :697 bone→bonemeal BACKFILLED there)",
		"Loader_Recipes_Vanilla.java:694-696 generator modules (IL.Module_* items; p7 pool) + :698 WiMo_Thick_Bone (foreign-mod item, CUT)",
		"Loader_Recipes_OreDict.java:81/:87/:91/:95 Hammer + :83 pulverizing sibling rows of the stone* listeners — tool-family pool (handoff p10-arch-tools-covers), and :84 Boxinator TF_Pick_Giant IL.exists() branch — foreign-mod gate, declared, no gates ported",
		"Loader_Recipes_OreDict.java:82/:88/:92/:96 Crusher rows BACKFILLED by p10-compat-vanilla-rows (:82 input = vanilla obsidian — the port block universe generates no blockSolid Obsidian, the ITEMGENERATOR.INGOTS chain fails; declared deviation)",
		"Loader_Recipes_Vanilla.java:702-706 ANY.Blaze.mToThis group expansion + :709-712 compressor rows (only the MT.Blaze representative is transcribed; p7 pool)",
		"Loader_Recipes_Handlers.java:64-67/:74 rockGt/rawOreChunk/chunk/rubble/gemChipped prefix rows with null outputs (mTargetCrushing pulverize-remains semantics; needs Recipe chances; ore-chain backfill pool)",
		"Loader_Recipes_Handlers.java:77 RecipeMapHandlerCrushing — the crushed-family ore chain (Recipe chances + Cinnabar probability; ore-chain backfill pool)",
		"RecipeMapShredder.getRecipeFor RECYCLABLE on-demand synthesis + WOOD duration factor (RecipeMapShredder.java:47-64/:56; recipe-POOL feature)",
		"Loader_Recipes_Handlers.java:114-155/:152-155 dust-impure family + RECYCLABLE loop, OreDict:204 wood loop, Furnace:80 fallback bridge, Handlers:371-375 Lathe prefix rows, all compat (p7 pool)");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation — a bare GT6RecipeMaps.reset()
	// (a dozen unpaired test call sites) must retire the flag WITH the maps, or load() silently
	// early-returns on the "maps cleared × flag set" poison state.
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesShCL::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesShCL::load);
	}

	/** Pours the tables into the SHREDDER/CRUSHER/LATHE maps. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines), tests may race it
		if (GT6RecipeMaps.SHREDDER == null || GT6RecipeMaps.CRUSHER == null || GT6RecipeMaps.LATHE == null) return; // reset() between init and load — a broken lifecycle
		pourFixed(GT6RecipeMaps.SHREDDER, "Shredder", shredderTable());
		pourFixed(GT6RecipeMaps.LATHE, "Lathe", latheTable());
		pourCrusher(GT6RecipeMaps.CRUSHER, crusherTable());
		pourFixed(GT6RecipeMaps.CRUSHER, "Crusher vanilla", crusherVanillaTable()); // p10-compat-vanilla-rows
		sLoaded = true;
	}

	private static void pourFixed(RecipeMap aMap, String aLabel, List<FixedRow> aRows) {
		int tPoured = 0, tSkipped = 0;
		for (FixedRow tRow : aRows) {
			Recipe tRecipe = buildFixedRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // = upstream mat() → null silent drop
			aMap.addRecipe(tRecipe);
			tPoured++;
		}
		LOGGER.info("GT6 {} recipes poured: {} loaded, {} skipped (unresolvable prefix/material items, = upstream mat() null drops)", aLabel, tPoured, tSkipped);
	}

	private static void pourCrusher(RecipeMap aMap, List<CrusherTemplate> aTemplates) {
		int tPoured = 0, tSkipped = 0;
		for (CrusherTemplate tTemplate : aTemplates) {
			for (OreDictMaterial tMaterial : expandCrusherMaterials(tTemplate.inPrefix())) {
				Recipe tRecipe = buildCrusherRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;} // upstream :205 condition gate or :209/:214 mat() → null
				aMap.addRecipe(tRecipe);
				tPoured++;
			}
		}
		LOGGER.info("GT6 Crusher recipes poured: {} loaded, {} skipped (ANTIMATTER/INVALID_MATERIAL condition-filtered materials + unresolvable prefix items, = upstream RecipeMapHandlerPrefix.addRecipeForMaterial false returns)", tPoured, tSkipped);
	}

	/**
	 * The port counterpart of RecipeMapHandlerPrefix.addAllRecipesInternal (:173) iterating
	 * {@code mInputPrefixes[0].mRegisteredMaterials}: the registered materials of the prefix,
	 * in registration order (the port's single enumeration source).
	 */
	public static List<OreDictMaterial> expandCrusherMaterials(OreDictPrefix aInPrefix) {
		List<OreDictMaterial> rMaterials = new ArrayList<>();
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == aInPrefix) rMaterials.add(tPair.material());
		}
		return rMaterials;
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
		// the upstream addRecipe1(T, eUt, dur, ...) shape: buffered, no fluids, no special value;
		// the p10 chance rows carry their chances literal through the P8 9-arg ctor (null = deterministic)
		return new Recipe(true, new ItemStack[] {tInput}, tOutputs, new FluidStack[0], new FluidStack[0], aRow.duration(), aRow.eUt(), 0, aRow.chances());
	}

	/**
	 * Crusher template x material → Recipe, or null (the upstream addRecipeForMaterial false
	 * return): the ANTIMATTER.NOT + INVALID_MATERIAL condition gate (:205), both-side mat()
	 * resolution (:209/:214), duration = mDuration &lt;= 0 → max(1, getCosts) (:218).
	 * mCanBeBuffered is upstream {@code !UNUSED_MATERIAL}; the port has no material-level
	 * UNUSED tag, so rows are always buffered (declared deviation, same as the cokeoven rows).
	 */
	static Recipe buildCrusherRecipe(CrusherTemplate aTemplate, OreDictMaterial aMaterial) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return null; // upstream :205
		Item tInItem = sMaterialItemResolver.apply(aTemplate.inPrefix(), aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefix(), aMaterial);
		if (tOutItem == null) return null; // upstream :214 mat() → null
		long tDuration = Math.max(1, crusherCosts(aTemplate, aMaterial)); // upstream :218
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInItem, aTemplate.inCount())},
				new ItemStack[] {new ItemStack(tOutItem, aTemplate.outCount())},
				new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0);
	}

	/**
	 * Upstream RecipeMapHandlerPrefix.getCosts (:225-227):
	 * {@code UT.Code.units(max(mUnitsInputted, mUnitsOutputted), U, mMultiplier +
	 * mMultiplier*mToolQuality, T)} with unitsIn/Out = prefix.mAmount * stack count (:74/:77).
	 * UT.Code.units (UT.java:1677-1683) inlined — the U-anchored translation with round-up.
	 */
	static long crusherCosts(CrusherTemplate aTemplate, OreDictMaterial aMaterial) {
		long tUnitsIn = aTemplate.inPrefix().mAmount * aTemplate.inCount();
		long tUnitsOut = aTemplate.outPrefix().mAmount * aTemplate.outCount();
		long tAmount = Math.max(tUnitsIn, tUnitsOut);
		long tTarget = aTemplate.multiplier() + aTemplate.multiplier() * aMaterial.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / CS.U + ((tAmount * tTarget) % CS.U > 0 ? 1 : 0));
	}

	/** Slot → ItemStack, or null when the segment fails to resolve (the upstream silent-drop semantics). */
	@Nullable
	private static ItemStack resolveSlot(Slot aSlot) {
		Item tItem = aSlot.vanilla() != null
				? sVanillaItemResolver.apply(aSlot.vanilla())
				: sMaterialItemResolver.apply(aSlot.prefix(), aSlot.material());
		return tItem == null ? null : new ItemStack(tItem, aSlot.count());
	}

	/** The live item lookup (GTMaterialItems.get :287) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured tables so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sShredderRows = null;
		sLatheRows = null;
		sCrusherTemplates = null;
		sCrusherVanillaRows = null;
	}
}
