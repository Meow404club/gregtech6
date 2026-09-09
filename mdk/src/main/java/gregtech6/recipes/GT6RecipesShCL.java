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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * 5-map vanilla coverage gap of the compat research is closed), and by task
 * p26-rm-row-backfill (the registration-time expansion of the upstream prefix HANDLER
 * templates: the Lathe 22-template tEasyWorkable twin arms of Loader_Recipes_Handlers.java
 * :371-393, the Shredder 34-template MORTAR twin arms + crushed-array rows of :114-150, and
 * the :152-155 RECYCLABLE ring — two rows per RECYCLABLE prefix material, output = the
 * {@code OM.pulverize} remains transcription).
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

	/**
	 * One transcribed Lathe prefix-handler template (task p26-rm-row-backfill) — the upstream
	 * {@code RecipeMapHandlerPrefix(inPrefix, 1, NF, eUt, duration, multiplier, NF, outPrefix,
	 * outCount, NI, NI, T, T, F, cond)} 15-arg form (Loader_Recipes_Handlers.java:371-393,
	 * the tEasyWorkable twin arms), expanded per material at pour time.
	 *
	 * <p>{@code easyArm} selects the condition arm: arm A rows (:371-381) carry
	 * {@code tEasyWorkable.NOT} (hard materials) with {@code multiplier 64, duration 0} → the
	 * getCosts arithmetic; arm B rows (:383-393) carry {@code tEasyWorkable} (FURNACE/SOFT
	 * materials) with FIXED durations and {@code multiplier 0} (the upstream integer literals
	 * {@code 16/8=2, 16/9=1, 16, 16*4=64, 16/4=4, 16/2=8}). {@code layeredNot}/{@code lensNot}
	 * transcribe the trailing {@code LAYERED.NOT} / {@code lens.NOT} condition conjuncts
	 * ({@code lens.NOT} is the port {@code OreDictPrefix.NOT} condition = the material has no
	 * lens item generation, OreDictPrefix.java:570-573).
	 */
	public record LatheTemplate(String note, OreDictPrefix inPrefix, OreDictPrefix outPrefix, int outCount,
			long eUt, long duration, long multiplier, boolean easyArm, boolean layeredNot, boolean lensNot) {}

	/**
	 * One transcribed Shredder prefix-shredding template (task p26-rm-row-backfill) — the
	 * upstream {@code RecipeMapHandlerPrefixShredding(...)} rows of
	 * Loader_Recipes_Handlers.java:114-124/:126-136 (single-input, 1-2 outputs) and
	 * :138-143/:145-150 (the crushed-array forms, 2-4 outputs), expanded per material at pour
	 * time. {@code mortar} selects the condition twin ({@code MORTAR.NOT} rows run the
	 * multiplier-256 getCosts arithmetic, {@code MORTAR} rows the multiplier-16 one — the two
	 * arms are mutually exclusive per material). {@code bedrockNot} transcribes the extra
	 * {@code MT.Bedrock.NOT} conjunct of the dust-impure family rows.
	 *
	 * <p><b>Output material</b>: unlike the Lathe/Crusher handler rows, the Shredding handler
	 * overrides {@code getOutputMaterial} to {@code aMaterial.mTargetPulver.mMaterial}
	 * (RecipeMapHandlerPrefixShredding.java:45-48) — every output resolves against the
	 * PULVERIZE target of the input material.
	 */
	public record ShredTemplate(String note, OreDictPrefix inPrefix, OreDictPrefix[] outPrefixes, int[] outCounts,
			long eUt, long multiplier, boolean mortar, boolean bedrockNot) {}

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
	 * The p26-rm-row-backfill Lathe prefix templates (Loader_Recipes_Handlers.java:371-393,
	 * the tEasyWorkable twin arms — arm A :371-381, arm B :383-393), in upstream file order.
	 * eUt 16 throughout; inCount 1 throughout. Arm A: duration 0 → the getCosts arithmetic
	 * with multiplier 64; arm B: the upstream fixed integer literals (16/8, 16/9, 16, 16*4,
	 * 16/4, 16/2) with multiplier 0. The :374/:386 plateGem→ring rows carry the trailing
	 * {@code lens.NOT} conjunct and :372/:384 the {@code LAYERED.NOT} conjunct — see
	 * {@link LatheTemplate}. All in/out prefixes are port item-path prefixes
	 * (GTMaterialItems.itemPathPrefixes), so these rows resolve against the live universe.
	 *
	 * <p><b>Lazily built</b> (the a9027ac lesson, same as every table above).
	 */
	private static volatile List<LatheTemplate> sLatheTemplates = null;

	/** The transcribed Lathe templates, captured on first use (one material generation). */
	public static List<LatheTemplate> latheTemplateTable() {
		List<LatheTemplate> tTable = sLatheTemplates;
		if (tTable == null) sLatheTemplates = tTable = List.of(
		// Loader_Recipes_Handlers.java:371-381 — arm A, tEasyWorkable.NOT (hard materials), mult 64, duration 0 → getCosts
		new LatheTemplate(":371", OP.bolt      , OP.screw    , 1, 16,  0, 64, false, false, false),
		new LatheTemplate(":372", OP.nugget    , OP.round    , 1, 16,  0, 64, false, true , false),
		new LatheTemplate(":373", OP.plateGem  , OP.lens     , 1, 16,  0, 64, false, false, false),
		new LatheTemplate(":374", OP.plateGem  , OP.ring     , 1, 16,  0, 64, false, false, true ),
		new LatheTemplate(":375", OP.lens      , OP.ring     , 1, 16,  0, 64, false, false, false),
		new LatheTemplate(":376", OP.gem       , OP.stick    , 1, 16,  0, 64, false, true , false),
		new LatheTemplate(":377", OP.ingot     , OP.stick    , 1, 16,  0, 64, false, true , false),
		new LatheTemplate(":378", OP.billet    , OP.stick    , 1, 16,  0, 64, false, true , false),
		new LatheTemplate(":379", OP.bouleGt   , OP.stickLong, 3, 16,  0, 64, false, true , false),
		new LatheTemplate(":380", OP.gemChipped, OP.bolt     , 1, 16,  0, 64, false, true , false),
		new LatheTemplate(":381", OP.gemFlawed , OP.bolt     , 3, 16,  0, 64, false, true , false),
		// Loader_Recipes_Handlers.java:383-393 — arm B, tEasyWorkable (FURNACE/SOFT materials), fixed durations, mult 0
		new LatheTemplate(":383", OP.bolt      , OP.screw    , 1, 16, 16/8,  0, true , false, false),
		new LatheTemplate(":384", OP.nugget    , OP.round    , 1, 16, 16/9,  0, true , true , false),
		new LatheTemplate(":385", OP.plateGem  , OP.lens     , 1, 16, 16  ,  0, true , false, false),
		new LatheTemplate(":386", OP.plateGem  , OP.ring     , 1, 16, 16  ,  0, true , false, true ),
		new LatheTemplate(":387", OP.lens      , OP.ring     , 1, 16, 16  ,  0, true , false, false),
		new LatheTemplate(":388", OP.gem       , OP.stick    , 1, 16, 16  ,  0, true , true , false),
		new LatheTemplate(":389", OP.ingot     , OP.stick    , 1, 16, 16  ,  0, true , true , false),
		new LatheTemplate(":390", OP.billet    , OP.stick    , 1, 16, 16  ,  0, true , true , false),
		new LatheTemplate(":391", OP.bouleGt   , OP.stickLong, 3, 16, 16*4,  0, true , true , false),
		new LatheTemplate(":392", OP.gemChipped, OP.bolt     , 1, 16, 16/4,  0, true , true , false),
		new LatheTemplate(":393", OP.gemFlawed , OP.bolt     , 3, 16, 16/2,  0, true , true , false));
		return tTable;
	}

	/**
	 * The upstream :371-393 condition conjuncts as a boolean gate — the port counterpart of
	 * {@code new And(ANTIMATTER.NOT, COATED.NOT, [easyArm? tEasyWorkable : tEasyWorkable.NOT]
	 * [, LAYERED.NOT] [, lens.NOT])} plus the INVALID_MATERIAL check of
	 * addRecipeForMaterial (:205). tEasyWorkable = Or(FURNACE, SOFT) (Handlers:58) — the
	 * port transcribes it as the two material-tag contains probes.
	 */
	static boolean latheCondition(OreDictMaterial aMaterial, LatheTemplate aTemplate) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Compounds.COATED)
				|| aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return false; // upstream :205 + the And() head
		boolean tEasyWorkable = aMaterial.contains(TD.Processing.FURNACE) || aMaterial.contains(TD.Properties.SOFT);
		if (aTemplate.easyArm() != tEasyWorkable) return false;
		if (aTemplate.layeredNot() && aMaterial.contains(TD.Compounds.LAYERED)) return false;
		if (aTemplate.lensNot() && OP.lens.canGenerateItem(aMaterial)) return false; // the lens.NOT conjunct (OreDictPrefix.NOT)
		return true;
	}

	/**
	 * Lathe template x material → Recipe, or null (the upstream addRecipeForMaterial false
	 * return): the {@link #latheCondition} gate, both-side item resolution (:209/:214), and
	 * the duration split — arm A mDuration=0 → max(1, getCosts) (:218, the multiplier-64
	 * arithmetic); arm B the fixed duration literal. Output material = SELF (the base
	 * RecipeMapHandlerPrefix.getOutputMaterial :221-223 — the Lathe rows are NOT Shredding
	 * rows, no mTargetPulver hop).
	 */
	static Recipe buildLatheRecipe(LatheTemplate aTemplate, OreDictMaterial aMaterial) {
		if (!latheCondition(aMaterial, aTemplate)) return null;
		Item tInItem = sMaterialItemResolver.apply(aTemplate.inPrefix(), aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefix(), aMaterial);
		if (tOutItem == null) return null; // upstream :214 mat() → null
		long tDuration = aTemplate.duration() > 0 ? aTemplate.duration()
				: Math.max(1, handlerCosts(aTemplate.inPrefix(), 1, aTemplate.outPrefix(), aTemplate.outCount(), aTemplate.multiplier(), aMaterial));
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInItem, 1)},
				new ItemStack[] {new ItemStack(tOutItem, aTemplate.outCount())},
				new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0);
	}

	/**
	 * The p26 Lathe backfill pour: every template x its registered materials, with the
	 * exact-row dedup (same input item+count, same outputs, same eUt and duration as a row
	 * already in the map = skip, the W1-collision ruling).
	 */
	private static void pourLathe(RecipeMap aMap, List<LatheTemplate> aTemplates) {
		Set<String> tSeen = seenRowKeys(aMap);
		int tPoured = 0, tSkipped = 0, tDeduped = 0;
		for (LatheTemplate tTemplate : aTemplates) {
			for (OreDictMaterial tMaterial : expandCrusherMaterials(tTemplate.inPrefix())) {
				Recipe tRecipe = buildLatheRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;} // upstream :205/:209/:214 false returns
				if (!tSeen.add(rowKey(tRecipe))) {tDeduped++; continue;} // exact-row collision
				aMap.addRecipe(tRecipe);
				tPoured++;
			}
		}
		LOGGER.info("GT6 Lathe backfill poured: {} loaded, {} skipped (condition gates + unresolvable items), {} deduped (exact rows already present)", tPoured, tSkipped, tDeduped);
	}

	/**
	 * The exact-row dedup key: input item+count, output items+counts, eUt, duration. Two
	 * rows differing in ANY of those are upstream-distinct (the RECYCLABLE ring's MORTAR
	 * twin rows share input/output but differ in the getCosts multiplier — both survive).
	 */
	static String rowKey(Recipe aRecipe) {
		StringBuilder rKey = new StringBuilder("e").append(aRecipe.mEUt).append("d").append(aRecipe.mDuration);
		for (ItemStack tStack : aRecipe.mInputs) rKey.append('|').append(tStack.getItem()).append('x').append(tStack.getCount());
		for (ItemStack tStack : aRecipe.mOutputs) rKey.append('|').append(tStack.getItem()).append('x').append(tStack.getCount());
		return rKey.toString();
	}

	/** The dedup seed: the rows already poured into the map by the earlier pours (the fixed tables). */
	private static Set<String> seenRowKeys(RecipeMap aMap) {
		Set<String> rKeys = new HashSet<>();
		for (Recipe tRecipe : aMap.mRecipeList) rKeys.add(rowKey(tRecipe));
		return rKeys;
	}

	/**
	 * Upstream RecipeMapHandlerPrefix.getCosts (:225-227) in its prefix-pair form —
	 * {@code UT.Code.units(max(mUnitsInputted, mUnitsOutputted), U, mMultiplier +
	 * mMultiplier*mToolQuality, T)} with unitsIn/Out = prefix.mAmount * stack count (:74/:77).
	 * The crusherCosts twin (same arithmetic, template-shaped); mDuration=0 rows resolve
	 * their duration through this (:218).
	 */
	static long handlerCosts(OreDictPrefix aInPrefix, int aInCount, OreDictPrefix aOutPrefix, int aOutCount, long aMultiplier, OreDictMaterial aMaterial) {
		return handlerCosts(aInPrefix.mAmount * aInCount, aOutPrefix.mAmount * aOutCount, aMultiplier, aMaterial);
	}

	/** The unit-sums form of {@link #handlerCosts} (the multi-output rows sum their output units, :77). */
	static long handlerCosts(long aUnitsIn, long aUnitsOut, long aMultiplier, OreDictMaterial aMaterial) {
		long tAmount = Math.max(aUnitsIn, aUnitsOut);
		long tTarget = aMultiplier + aMultiplier * aMaterial.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / CS.U + ((tAmount * tTarget) % CS.U > 0 ? 1 : 0));
	}

	/**
	 * The p26-rm-row-backfill Shredder prefix templates (Loader_Recipes_Handlers.java:114-124
	 * + :126-136 single-prefix rows and :138-143 + :145-150 the crushed-array rows), in
	 * upstream file order. eUt 16 throughout; duration is 0 (the getCosts arithmetic) on every
	 * row — the twin MORTAR/MORTAR.NOT arms carry multiplier 16 / 256 respectively.
	 *
	 * <p><b>Item-universe note</b>: of these 34 templates only the dustImpure pair (:114/:126)
	 * and the twelve crushed-family array rows (:138-150) have their input prefix in the port
	 * item universe (GTMaterialItems.itemPathPrefixes — dustPure/dustRefined/chunk/rubble/
	 * pebbles/clump/reduced/crystalline/cleanGravel/cluster have no port items), so the other
	 * 20 templates expand to zero rows and are skip-counted, lighting up automatically if a
	 * future item-universe card registers those prefixes.
	 */
	private static volatile List<ShredTemplate> sShredTemplates = null;

	/** The transcribed Shredder templates, captured on first use (one material generation). */
	public static List<ShredTemplate> shredTemplateTable() {
		List<ShredTemplate> tTable = sShredTemplates;
		if (tTable == null) sShredTemplates = tTable = List.of(
		// Loader_Recipes_Handlers.java:114-124 — the MORTAR.NOT arm, multiplier 256
		shred(":114", OP.dustImpure   , OP.dust, 1, OP.dustTiny, 1, 256, false, true ),
		shred(":115", OP.dustPure     , OP.dust, 1, OP.dustTiny, 2, 256, false, true ),
		shred(":116", OP.dustRefined  , OP.dust, 1, OP.dustTiny, 3, 256, false, true ),
		shred(":117", OP.chunk        , OP.dust, 2, OP.dustTiny, 1, 256, false, false),
		shred(":118", OP.rubble       , OP.dust, 2, OP.dustTiny, 1, 256, false, false),
		shred(":119", OP.pebbles      , OP.dust, 3, OP.dustTiny, 1, 256, false, false),
		shred(":120", OP.clump        , OP.dust, 1, 256, false, false),
		shred(":121", OP.reduced      , OP.dust, 1, 256, false, false),
		shred(":122", OP.crystalline  , OP.dust, 1, 256, false, false),
		shred(":123", OP.cleanGravel  , OP.dust, 1, 256, false, false),
		shred(":124", OP.cluster      , OP.dust, 3, 256, false, false),
		// Loader_Recipes_Handlers.java:126-136 — the MORTAR arm, multiplier 16
		shred(":126", OP.dustImpure   , OP.dust, 1, OP.dustTiny, 1, 16, true, true ),
		shred(":127", OP.dustPure     , OP.dust, 1, OP.dustTiny, 2, 16, true, true ),
		shred(":128", OP.dustRefined  , OP.dust, 1, OP.dustTiny, 3, 16, true, true ),
		shred(":129", OP.chunk        , OP.dust, 2, OP.dustTiny, 1, 16, true, false),
		shred(":130", OP.rubble       , OP.dust, 2, OP.dustTiny, 1, 16, true, false),
		shred(":131", OP.pebbles      , OP.dust, 3, OP.dustTiny, 1, 16, true, false),
		shred(":132", OP.clump        , OP.dust, 1, 16, true, false),
		shred(":133", OP.reduced      , OP.dust, 1, 16, true, false),
		shred(":134", OP.crystalline  , OP.dust, 1, 16, true, false),
		shred(":135", OP.cleanGravel  , OP.dust, 1, 16, true, false),
		shred(":136", OP.cluster      , OP.dust, 3, 16, true, false),
		// Loader_Recipes_Handlers.java:138-143 — the crushed-array rows, MORTAR.NOT arm, multiplier 256
		array(":138", OP.crushed                , OP.dust, OP.dustTiny, OP.dustDiv72, null        , 256, false),
		array(":139", OP.crushedPurified        , OP.dust, OP.dustSmall, null        , null        , 256, false),
		array(":140", OP.crushedCentrifuged     , OP.dust, OP.dustSmall, OP.dustTiny, OP.dustDiv72, 256, false),
		array(":141", OP.crushedTiny            , OP.dustTiny, OP.dustDiv72, null   , null        , 256, false),
		array(":142", OP.crushedPurifiedTiny    , OP.dustTiny, OP.dustDiv72, OP.dustDiv72, null   , 256, false),
		array(":143", OP.crushedCentrifugedTiny , OP.dustTiny, OP.dustDiv72, OP.dustDiv72, OP.dustDiv72, 256, false),
		// Loader_Recipes_Handlers.java:145-150 — the crushed-array rows, MORTAR arm, multiplier 16
		array(":145", OP.crushed                , OP.dust, OP.dustTiny, null        , null        , 16, true),
		array(":146", OP.crushedPurified        , OP.dust, OP.dustSmall, null        , null        , 16, true),
		array(":147", OP.crushedCentrifuged     , OP.dust, OP.dustSmall, OP.dustTiny, OP.dustDiv72, 16, true),
		array(":148", OP.crushedTiny            , OP.dustTiny, OP.dustDiv72, null   , null        , 16, true),
		array(":149", OP.crushedPurifiedTiny    , OP.dustTiny, OP.dustDiv72, OP.dustDiv72, null   , 16, true),
		array(":150", OP.crushedCentrifugedTiny , OP.dustTiny, OP.dustDiv72, OP.dustDiv72, OP.dustDiv72, 16, true));
		return tTable;
	}

	/** Table-builder helper (the 1-2-output single-prefix rows) — amounts are the upstream L12_LONG_1 1s. */
	private static ShredTemplate shred(String aNote, OreDictPrefix aIn, OreDictPrefix aOut1, int aCount1, OreDictPrefix aOut2, int aCount2, long aMultiplier, boolean aMortar, boolean aBedrockNot) {
		return new ShredTemplate(aNote, aIn,
				aOut2 == null ? new OreDictPrefix[] {aOut1} : new OreDictPrefix[] {aOut1, aOut2},
				aOut2 == null ? new int[] {aCount1} : new int[] {aCount1, aCount2},
				16, aMultiplier, aMortar, aBedrockNot);
	}

	/** Table-builder helper (the single-output rows, :120-124/:132-136). */
	private static ShredTemplate shred(String aNote, OreDictPrefix aIn, OreDictPrefix aOut, int aCount, long aMultiplier, boolean aMortar, boolean aBedrockNot) {
		return shred(aNote, aIn, aOut, aCount, null, 0, aMultiplier, aMortar, aBedrockNot);
	}

	/** Table-builder helper (the 2-4-output crushed-array rows). */
	private static ShredTemplate array(String aNote, OreDictPrefix aIn, OreDictPrefix aOut1, OreDictPrefix aOut2, OreDictPrefix aOut3, OreDictPrefix aOut4, long aMultiplier, boolean aMortar) {
		if (aOut4 != null) return new ShredTemplate(aNote, aIn, new OreDictPrefix[] {aOut1, aOut2, aOut3, aOut4}, new int[] {1, 1, 1, 1}, 16, aMultiplier, aMortar, false);
		if (aOut3 != null) return new ShredTemplate(aNote, aIn, new OreDictPrefix[] {aOut1, aOut2, aOut3}, new int[] {1, 1, 1}, 16, aMultiplier, aMortar, false);
		return new ShredTemplate(aNote, aIn, new OreDictPrefix[] {aOut1, aOut2}, new int[] {1, 1}, 16, aMultiplier, aMortar, false);
	}

	/**
	 * The upstream :114-150 condition conjuncts as a boolean gate — the port counterpart of
	 * {@code new And(ANTIMATTER.NOT, mortar? MORTAR : MORTAR.NOT [, MT.Bedrock.NOT])} plus the
	 * INVALID_MATERIAL check of addRecipeForMaterial (:205). The Shredding output-material hop
	 * ({@code mTargetPulver}) is applied by the caller.
	 */
	static boolean shredCondition(OreDictMaterial aMaterial, ShredTemplate aTemplate) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return false;
		if (aTemplate.mortar() != aMaterial.contains(TD.Processing.MORTAR)) return false;
		if (aTemplate.bedrockNot() && aMaterial == MT.Bedrock) return false; // the MT.Bedrock.NOT conjunct
		return true;
	}

	/**
	 * Shred template x material → Recipe, or null (the upstream addRecipeForMaterial false
	 * return): the {@link #shredCondition} gate, the input resolution (:209), the
	 * mTargetPulver output-material hop (RecipeMapHandlerPrefixShredding.java:47), the
	 * all-outputs resolution (:214 — ANY unresolvable output drops the row), and the
	 * mDuration=0 → max(1, getCosts) split (:218, with the summed output units :77).
	 */
	static Recipe buildShredRecipe(ShredTemplate aTemplate, OreDictMaterial aMaterial) {
		if (!shredCondition(aMaterial, aTemplate)) return null;
		Item tInItem = sMaterialItemResolver.apply(aTemplate.inPrefix(), aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		OreDictMaterial tOutMaterial = aMaterial.mTargetPulver.mMaterial; // the Shredding override :47
		ItemStack[] tOutputs = new ItemStack[aTemplate.outPrefixes().length];
		long tUnitsOut = 0;
		for (int i = 0; i < tOutputs.length; i++) {
			Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefixes()[i], tOutMaterial);
			if (tOutItem == null) return null; // upstream :214 — any invalid output drops the row
			tOutputs[i] = new ItemStack(tOutItem, aTemplate.outCounts()[i]);
			tUnitsOut += aTemplate.outPrefixes()[i].mAmount * aTemplate.outCounts()[i];
		}
		long tDuration = Math.max(1, handlerCosts(aTemplate.inPrefix().mAmount, tUnitsOut, aTemplate.multiplier(), aMaterial));
		return new Recipe(true, new ItemStack[] {new ItemStack(tInItem, 1)}, tOutputs, new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0);
	}

	/** The p26 Shredder template backfill pour (the pourLathe shape over the ShredTemplate walk). */
	private static void pourShredderTemplates(RecipeMap aMap, List<ShredTemplate> aTemplates) {
		Set<String> tSeen = seenRowKeys(aMap);
		int tPoured = 0, tSkipped = 0, tDeduped = 0;
		for (ShredTemplate tTemplate : aTemplates) {
			for (OreDictMaterial tMaterial : expandCrusherMaterials(tTemplate.inPrefix())) {
				Recipe tRecipe = buildShredRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;}
				if (!tSeen.add(rowKey(tRecipe))) {tDeduped++; continue;}
				aMap.addRecipe(tRecipe);
				tPoured++;
			}
		}
		LOGGER.info("GT6 Shredder backfill poured: {} loaded, {} skipped (condition gates + unresolvable items), {} deduped (exact rows already present)", tPoured, tSkipped, tDeduped);
	}

	/**
	 * The prefixes upstream OP.java:639-738 gives a non-empty {@code mByProducts} list — the
	 * {@code :152} filter leg that is VACUOUS in this port (the byproduct dataset block is not
	 * ported, every {@code mPrefix.mByProducts} is empty — the GT6RecipesOreChain.java:66-69
	 * self-evidence), transcribed as an explicit skip-set so the port does not over-generate
	 * the RECYCLABLE ring rows upstream excludes through that leg (the card's verification
	 * point). The ore* members would fail the ORE-tag conjunct anyway; the toolHead, bullet,
	 * arrow and chemtube members are the real divergences this set closes.
	 */
	static final Set<String> UPSTREAM_BYPRODUCT_PREFIXES = Set.of(
			"pipeRestrictiveTiny", "pipeRestrictiveSmall", "pipeRestrictiveMedium", "pipeRestrictiveLarge", "pipeRestrictiveHuge",
			"cableGt12", "cableGt08", "cableGt04", "cableGt02", "cableGt01",
			"cell", "bottle", "chemtube",
			"oreAndesite", "oreDiorite", "oreBlackstone", "oreRedgranite", "oreBlackgranite", "oreVanillagranite", "oreVanillastone",
			"oreDeepslate", "oreMoon", "oreMars", "oreSpace", "orePhobos", "oreDeimos", "oreMercury", "oreVenus", "oreCeres",
			"oreJupiter", "oreIo", "oreGanymede", "oreCallisto", "oreSaturn", "oreRhea", "oreTitan", "oreOberon", "oreIapetus",
			"oreUranus", "oreNeptune", "oreTriton", "orePluto", "oreEris", "oreKepler22b", "oreHolystone", "oreLivingrock",
			"oreDeadrock", "oreBetweenstone", "orePitstone", "oreUmberstone", "oreKomatiite", "oreBasalt", "oreMarble",
			"oreLimestone", "oreSiltstone", "oreShale", "oreSlate", "oreGreenschist", "oreBlueschist", "orePinkschist",
			"oreGneiss", "oreLightprismarine", "oreDarkprismarine", "oreKimberlite", "oreQuartzite", "oreNetherrack",
			"oreNether", "oreEndstone", "oreEnd", "orePoor", "oreSmall", "oreNormal", "oreRich",
			"crushed",
			"toolHeadPickaxeGem", "toolHeadDrill", "toolHeadChainsaw", "toolHeadWrench",
			"crateGtGem", "crateGtDust", "crateGtIngot", "crateGtPlate", "crateGtPlateGem",
			"crateGt64Gem", "crateGt64Dust", "crateGt64Ingot", "crateGt64Plate", "crateGt64PlateGem",
			"plantGtTwig", "arrowGtWood", "arrowGtPlastic",
			"bulletGtSmall", "bulletGtMedium", "bulletGtLarge");

	/**
	 * The p26 RECYCLABLE ring pour — the upstream :152-155 loop over OreDictPrefix.VALUES,
	 * transcribed filter-for-filter: {@code mByProducts.isEmpty()} (vacuously true in this
	 * port — replaced by the {@link #UPSTREAM_BYPRODUCT_PREFIXES} skip-set),
	 * {@code contains(RECYCLABLE)}, {@code !containsAny(ORE, ORE_PROCESSING_BASED, DUST_BASED,
	 * IS_CONTAINER)}, the cableGt/wireGt/pipe name-prefix exclusions. Two rows per material
	 * (:153 the MORTAR.NOT arm multiplier 256, :154 the MORTAR arm multiplier 16), both with
	 * NULL fixed outputs — the output is the {@code mOutputPulverizedRemains} stack alone
	 * (RecipeMapHandlerPrefix.java:215 = {@code OM.pulverize(aMaterial, mUnitsInputted)}).
	 */
	private static void pourRecyclableRing(RecipeMap aMap) {
		Set<String> tSeen = seenRowKeys(aMap);
		int tPoured = 0, tSkipped = 0, tDeduped = 0, tPrefixes = 0, tByproductSkipped = 0;
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
			if (!tPrefix.mByProducts.isEmpty()) continue; // upstream :152 leg 1 (live; vacuous in this port today)
			if (UPSTREAM_BYPRODUCT_PREFIXES.contains(tPrefix.mNameInternal)) {tByproductSkipped++; continue;} // the port form of the same leg
			if (!tPrefix.contains(TD.Prefix.RECYCLABLE)) continue; // :152 leg 2
			if (tPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED, TD.Prefix.DUST_BASED, TD.Prefix.IS_CONTAINER)) continue; // :152 leg 3
			if (tPrefix.mNameInternal.startsWith("cableGt") || tPrefix.mNameInternal.startsWith("wireGt") || tPrefix.mNameInternal.startsWith("pipe")) continue; // :152 leg 4
			tPrefixes++;
			for (OreDictMaterial tMaterial : expandCrusherMaterials(tPrefix)) {
				for (int tArm = 0; tArm < 2; tArm++) { // :153 (256, MORTAR.NOT) then :154 (16, MORTAR)
					Recipe tRecipe = buildRingRecipe(tPrefix, tMaterial, tArm == 0 ? 256 : 16, tArm == 0);
					if (tRecipe == null) {tSkipped++; continue;}
					if (!tSeen.add(rowKey(tRecipe))) {tDeduped++; continue;}
					aMap.addRecipe(tRecipe);
					tPoured++;
				}
			}
		}
		LOGGER.info("GT6 Shredder RECYCLABLE ring poured: {} loaded, {} skipped, {} deduped, across {} qualifying prefixes ({} byproduct-carrier prefixes excluded per upstream OP.java:639-738)", tPoured, tSkipped, tDeduped, tPrefixes, tByproductSkipped);
	}

	/**
	 * Ring prefix x material x arm → Recipe, or null (the upstream addRecipeForMaterial false
	 * return): the And(ANTIMATTER.NOT, [MORTAR split]) condition + :205, the input resolution
	 * (:209), and the :215 remains output — {@code OM.pulverize(aMaterial, unitsIn)} over the
	 * :79 gate {@code unitsIn - unitsOut >= dustDiv72.mAmount} (unitsOut = 0 here — the null
	 * output-prefix rows carry no fixed outputs). Output material SELF (the base handler).
	 */
	static Recipe buildRingRecipe(OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aMultiplier, boolean aMortarNot) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return null;
		if (aMortarNot == aMaterial.contains(TD.Processing.MORTAR)) return null;
		Item tInItem = sMaterialItemResolver.apply(aPrefix, aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		long tUnitsIn = aPrefix.mAmount; // x1 — mUnitsInputted (:74) with mUnitsOutputted = 0
		if (tUnitsIn < OP.dustDiv72.mAmount) return null; // the :79 mOutputPulverizedRemains gate — no output, row drops
		ItemStack tOut = pulverizeOutput(aMaterial, tUnitsIn); // upstream :215 = OM.pulverize(aMaterial, unitsIn)
		if (tOut == null) return null; // OM.dust amount/indent resolution failed (= upstream mat() → null)
		long tDuration = Math.max(1, handlerCosts(tUnitsIn, 0, aMultiplier, aMaterial));
		return new Recipe(true, new ItemStack[] {new ItemStack(tInItem, 1)}, new ItemStack[] {tOut}, new FluidStack[0], new FluidStack[0], tDuration, 16, 0);
	}

	/**
	 * Upstream OM.pulverize(OreDictMaterial, long) (OM.java:370-372): the material's
	 * mTargetPulver hop + the round-DOWN unit translation
	 * {@code units(aMaterialAmount, U, mTargetPulver.mAmount, F)}, feeding the OM.dust
	 * prefix-size cascade (OM.java:460-470) transcribed below.
	 */
	static ItemStack pulverizeOutput(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount <= 0 || aMaterial == null) return null;
		return dustCascade(aMaterial.mTargetPulver.mMaterial, units(aMaterialAmount, CS.U, aMaterial.mTargetPulver.mAmount, false));
	}

	/** Upstream UT.Code.units (UT.java:1677-1683), the generic form (the crusherCosts inline is the specialization). */
	static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit % aTargetUnit == 0) {aOriginalUnit /= aTargetUnit; aTargetUnit = 1;}
		else if (aTargetUnit % aOriginalUnit == 0) {aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, (aAmount * aTargetUnit) / aOriginalUnit + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	/** Upstream UT.Code.bindStack (UT.java:1568). */
	private static int bindStack(long aBoundValue) {
		return (int)Math.max(1, Math.min(64, aBoundValue));
	}

	/**
	 * Upstream OM.dust(OreDictMaterial, long) (OM.java:460-470): the dust prefix-size cascade
	 * blockDust → dust → dustSmall → dustTiny → dustDiv72, each stage falling through to the
	 * next when the prefix has no item for the material (= upstream mat() → null fallthrough).
	 * The modulo comparisons pick the smallest faithful representation.
	 */
	static ItemStack dustCascade(OreDictMaterial aMaterial, long aAmount) {
		if (aAmount < CS.U72 || aMaterial == null) return null;
		if (aAmount >= CS.U * 72) {Item tItem = sMaterialItemResolver.apply(OP.blockDust, aMaterial); if (tItem != null) return new ItemStack(tItem, bindStack(aAmount / (CS.U * 9)));}
		if (aAmount >= CS.U     ) if (aAmount >= CS.U * 16 || aAmount % CS.U == 0) {Item tItem = sMaterialItemResolver.apply(OP.dust, aMaterial); if (tItem != null) return new ItemStack(tItem, bindStack(aAmount / CS.U));}
		if (aAmount >= CS.U4    ) if (aAmount >= CS.U *  8 || aAmount % CS.U4 <= aAmount % CS.U9) {Item tItem = sMaterialItemResolver.apply(OP.dustSmall, aMaterial); if (tItem != null) return new ItemStack(tItem, bindStack(aAmount * 4 / CS.U));}
		if (aAmount >= CS.U9    ) if (aAmount >= CS.U      || aAmount % CS.U9 <= aAmount % CS.U72) {Item tItem = sMaterialItemResolver.apply(OP.dustTiny, aMaterial); if (tItem != null) return new ItemStack(tItem, bindStack(aAmount * 9 / CS.U));}
		Item tItem = sMaterialItemResolver.apply(OP.dustDiv72, aMaterial);
		return tItem == null ? null : new ItemStack(tItem, bindStack(aAmount * 72 / CS.U));
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
		"RecipeMapShredder.getRecipeFor RECYCLABLE on-demand synthesis + WOOD duration factor (RecipeMapShredder.java:47-64/:56; the ON-DEMAND synthesis stays a recipe-POOL feature — the STATIC :152-155 RECYCLABLE ring rows are BACKFILLED by p26-rm-row-backfill, two rows per RECYCLABLE prefix material)",
		"Loader_Recipes_Handlers.java:114-155 dust-impure family + crushed-array rows + the :152-155 RECYCLABLE ring, and Handlers:371-393 the Lathe prefix rows — BACKFILLED by p26-rm-row-backfill (the 20 templates whose input prefix has no port item skip-count and stay declared here until an item-universe card registers them); OreDict:204 wood loop, Furnace:80 fallback bridge, all compat remain pooled");

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
		pourLathe(GT6RecipeMaps.LATHE, latheTemplateTable()); // p26-rm-row-backfill (Handlers:371-393)
		pourShredderTemplates(GT6RecipeMaps.SHREDDER, shredTemplateTable()); // p26-rm-row-backfill (Handlers:114-150)
		pourRecyclableRing(GT6RecipeMaps.SHREDDER); // p26-rm-row-backfill (Handlers:152-155)
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
		sLatheTemplates = null;
		sShredTemplates = null;
	}
}
