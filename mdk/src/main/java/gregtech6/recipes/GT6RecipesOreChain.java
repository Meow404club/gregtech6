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

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Crusher ore chain — task p8-recipe-chances-orechain ④: the upstream runtime handler
 * {@code RecipeMapHandlerCrushing.addRecipesUsing} (RecipeMapHandlerCrushing.java:50-137)
 * transcribed into a registration-time row expansion poured into
 * {@link GT6RecipeMaps#CRUSHER} (the GT6RecipesShCL static-pour precedent; the port keeps
 * no runtime RecipeMapHandlers — Recipe.java:526-542 stays unported, and the handler's
 * containsInput ore-block special case :141-143 dies with the runtime layer, ADR
 * 2026-08-31-p8-machine-closeout ③(b)).
 *
 * <p><b>First-wave input shapes</b>: {@link OP#oreRaw} and {@link OP#blockRaw} item rows —
 * the two {@code addRecipesProducing} driver items (RecipeMapHandlerCrushing.java:153-154).
 * {@code OP.blockRaw} is upstream {@code setCondition(oreRaw)} (OP.java:345 upstream), so
 * both prefixes walk the SAME material set: the {@link OP#oreRaw} registration universe
 * ({@link GTMaterialItems#registrationOrder}). {@code blockRaw} is a block-path prefix —
 * its items do not exist in the P3 item universe yet, so its rows transcribe as DATA and
 * skip at pour time (the upstream {@code mat()} → null drop shape) until the prefixblock
 * card lands them. The upstream ore-BLOCK rows ({@code BlocksGT.ore}/oreBroken,
 * :151-152) and the stone-family prefixes with prefix-level byproducts (OP.java:653-668
 * upstream — never ported into this OP, so every {@code mPrefix.mByProducts} is empty and
 * the byproduct loop :127-136 is structurally dormant) stay pooled.
 *
	 * <p><b>Chances semantics</b> (the reason this card needed Recipe.mChances): every row is
	 * chance-bearing — the pre-output slot 0 carries the upstream 0-sentinel (RecipeMapHandlerCrushing.java:77-85
	 * writes the main output twice: slot 0 chance 0, slot 1 chance 10000). Upstream Recipe.java:906
	 * rewrites {@code chances[i] <= 0} to 10000 in the ctor, making the sentinel a second full
	 * output — every handler row crushes into TWO main outputs. The p9 yield reform (task
	 * p9-recipe-yield-reform) writes that end state directly: the port emits BOTH slots at
	 * 10000 (the sentinel's upstream-effective value) instead of relying on the ctor rewrite
	 * it does NOT replicate, while the {@code chance == 0 → no output} code ruling (Recipe.java
	 * javadoc) stays untouched — no sentinel slots are written anymore. The Cinnabar gem row
	 * (:111-125) is genuinely probabilistic (2500 + byproduct bonuses over a 10000 base) —
	 * exercised through {@link Recipe#getOutputs(Random, int)}.
 *
 * <p><b>Skipped upstream branches (declared, not silent)</b>: the {@code oreSmall}/oreRich/oreNormal
 * TODO branch (:73-76, upstream returns F) transcribes as a skip; the netherrack-family
 * Hexorium multiplier tuning (:55-62) is transcribed but unreachable in the first wave
 * (none of those prefixes are walked yet); {@code orePoor} (:68-72) transcribes fully as
 * the tiny-crushed branch.
 *
 * <p><b>Load timing</b> (the a9027ac lesson): a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork; the OP/MT-referencing tables are built lazily by
 * {@code table()} on first load, never in static initializers. {@code load()} is idempotent
 * per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesOreChain {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One input shape of the ore chain: the upstream driver prefix (:153-154) transcribed as DATA. */
	public record OreChainTemplate(String note, OreDictPrefix inPrefix) {}

	/**
	 * The per-(prefix, material) plan — everything the row expansion computes from pure
	 * material data (no item resolution), kept offline-testable per the task card.
	 * {@code poorTinyBranch} = the upstream :68-72 shape; {@code null} plans are the
	 * upstream skip states (ANTIMATTER gate :51, the TODO branch :73-76).
	 */
	public record OreChainPlan(
			String note,
			OreDictPrefix inPrefix,
			OreDictMaterial inMaterial,
			/** mTargetCrushing.mMaterial (upstream :52) — the material the ore crushes into. */
			OreDictMaterial outMaterial,
			boolean poorTinyBranch,
			/** mOreProcessingMultiplier x branch factor (upstream :53-67; x3 for the tiny branch :69). */
			long multiplier,
			/** Extra main-output copies beyond the double-slot base: 7 for blockRaw (:86-103), 2 for DENSE_ORE (:104-110), 0 else. */
			int extraCopies,
			/** DENSE_ORE prefix flag: duration x2 (:109) and the Cinnabar gem count (:124). */
			boolean dense,
			/** Cinnabar gem chance sum (:111-122); 0 = no gem row (:123). */
			long cinnabarChance,
			/** Cinnabar gem count (:124): 9 blockRaw / 2 DENSE / 1. */
			long cinnabarCount,
			/** Prefix-level byproducts (:127); structurally empty in this port, see class doc. */
			List<OreDictMaterialStack> byproducts
	) {}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline. */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesOreChain::resolveItem;

	/**
	 * The transcribed input shapes, in upstream call-site order (:153 oreRaw before :154
	 * blockRaw).
	 */
	private static volatile List<OreChainTemplate> sTemplates = null;

	/** The transcribed input shapes, captured on first use (one material generation). */
	public static List<OreChainTemplate> table() {
		List<OreChainTemplate> tTable = sTemplates;
		if (tTable == null) sTemplates = tTable = List.of(
			new OreChainTemplate(":153", OP.oreRaw),
			new OreChainTemplate(":154", OP.blockRaw));
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation — a bare GT6RecipeMaps.reset()
	// (a dozen unpaired test call sites) must retire the flag WITH the maps, or load() silently
	// early-returns on the "maps cleared × flag set" poison state.
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesOreChain::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesOreChain::load);
	}

	/** Pours the ore-chain expansion into the CRUSHER map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines), tests may race it
		if (GT6RecipeMaps.CRUSHER == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (OreChainTemplate tTemplate : table()) {
			for (OreDictMaterial tMaterial : expandOreMaterials()) {
				OreChainPlan tPlan = planRow(tTemplate.inPrefix(), tMaterial, tTemplate.note());
				if (tPlan == null) continue; // upstream :51/:73-76 skip states — a declared non-row, not a pour skip
				Recipe tRecipe = buildRecipe(tPlan, GT6RecipeMaps.CRUSHER.mOutputItemsCount);
				if (tRecipe == null || GT6RecipeMaps.CRUSHER.addRecipe(tRecipe) == null) {tSkipped++; continue;} // upstream mat() → null drop / double-empty guard
				tPoured++;
			}
		}
		LOGGER.info("GT6 Crusher ore-chain recipes poured: {} loaded, {} skipped (unresolvable prefix/material items — blockRaw rows until the prefixblock card, = upstream mat() null drops)", tPoured, tSkipped);
		sLoaded = true;
	}

	/**
	 * The port counterpart of the handler's implicit universe: the {@link OP#oreRaw}
	 * registration-order materials ({@link GTMaterialItems#registrationOrder} — the port's
	 * single enumeration source). {@code OP.blockRaw} is upstream {@code setCondition(oreRaw)}
	 * (OP.java:345), so its material set is identical and both templates walk this list.
	 */
	public static List<OreDictMaterial> expandOreMaterials() {
		List<OreDictMaterial> rMaterials = new ArrayList<>();
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == OP.oreRaw) rMaterials.add(tPair.material());
		}
		return rMaterials;
	}

	/**
	 * The pure row planner — upstream :51-125 up to item resolution. Returns {@code null}
	 * for the declared skip states: the ANTIMATTER gate (:51) and the oreSmall/oreRich/
	 * oreNormal TODO branch (:73-76, upstream {@code return F}).
	 */
	@Nullable
	public static OreChainPlan planRow(OreDictPrefix aPrefix, OreDictMaterial aMaterial, String aNote) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER)) return null; // upstream :51 ANTIMATTER arm (the prefix gate is pinned by the template walk)
		long tMultiplier = crushingMultiplier(aPrefix, aMaterial);
		if (aPrefix == OP.orePoor) { // upstream :68-72
			return new OreChainPlan(aNote, aPrefix, aMaterial, crushingTarget(aMaterial), true, 3 * tMultiplier, 0, false, 0, 0, aPrefix.mByProducts);
		}
		if (aPrefix == OP.oreSmall || aPrefix == OP.oreRich || aPrefix == OP.oreNormal) return null; // upstream :73-76 TODO = F
		int tExtraCopies = 0;
		boolean tDense = aPrefix.contains(TD.Prefix.DENSE_ORE);
		if (aPrefix == OP.blockRaw) tExtraCopies = 7; // upstream :86-103 — 7 copies; the sentinel+duplicate pair (:78-85) is the buildRecipe base since the p9 reform
		if (tDense) tExtraCopies += 2; // upstream :104-110
		long tCinnabarChance = cinnabarBonus(aMaterial); // upstream :111-122
		return new OreChainPlan(aNote, aPrefix, aMaterial, crushingTarget(aMaterial), false, tMultiplier, tExtraCopies, tDense, tCinnabarChance, cinnabarGemCount(aPrefix, tDense), aPrefix.mByProducts);
	}

	/**
	 * Upstream :52-53 + :55-67: {@code mOreProcessingMultiplier} scaled by the prefix branch —
	 * the netherrack family with its Hexorium +/-1 tuning (:55-62), blockRaw x2 (:63-64),
	 * plain {@code x mOreMultiplier} otherwise (:65-67).
	 */
	public static long crushingMultiplier(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		long rMultiplier = aMaterial.mOreProcessingMultiplier; // upstream :53
		if (aPrefix == OP.oreNetherrack || aPrefix == OP.oreNether || aPrefix == OP.oreBasalt || aPrefix == OP.oreKomatiite || aPrefix == OP.oreDeepslate) {
			if (aMaterial == MT.HexoriumBlack || aMaterial == MT.HexoriumWhite) {
				rMultiplier *= (aMaterial.mOreMultiplier + 1); // upstream :56-57
			} else if (aMaterial == MT.HexoriumRed || aMaterial == MT.HexoriumGreen || aMaterial == MT.HexoriumBlue) {
				rMultiplier *= (aMaterial.mOreMultiplier - 1); // upstream :58-59
			} else {
				rMultiplier *= aMaterial.mOreMultiplier; // upstream :61
			}
		} else if (aPrefix == OP.blockRaw) {
			rMultiplier *= aMaterial.mOreMultiplier * 2L; // upstream :64 — "Multiply by 2, but fill out 4.5 times more Slots"
		} else {
			rMultiplier *= aMaterial.mOreMultiplier; // upstream :66
		}
		return rMultiplier;
	}

	/** Upstream :52 — the material this ore crushes into ({@code mTargetCrushing.mMaterial}). */
	public static OreDictMaterial crushingTarget(OreDictMaterial aMaterial) {
		return aMaterial.mTargetCrushing.mMaterial;
	}

	/** The main-output stack count: {@code bindStack(units(mTargetCrushing.mAmount, U, multiplier, F))} (upstream :78-80). */
	public static long mainOutputCount(OreDictMaterial aMaterial, long aMultiplier) {
		return bindStack(units(aMaterial.mTargetCrushing.mAmount, CS.U, aMultiplier, false));
	}

	/**
	 * The Cinnabar gem chance sum (upstream :111-122): +2500 for the
	 * PULVERIZING_CINNABAR tag, +1000/+500/+500 for the Hg/OREMATS.Cinnabar/Redstone
	 * material byproducts.
	 */
	public static long cinnabarBonus(OreDictMaterial aMaterial) {
		long rChance = 0;
		if (aMaterial.contains(TD.Processing.PULVERIZING_CINNABAR)) rChance += 2500;
		if (aMaterial.mByProducts.contains(MT.Hg)) rChance += 1000;
		if (aMaterial.mByProducts.contains(MT.OREMATS.Cinnabar)) rChance += 500;
		if (aMaterial.mByProducts.contains(MT.Redstone)) rChance += 500;
		return rChance;
	}

	/** Upstream :124 — the Cinnabar gem count: 9 blockRaw / 2 DENSE_ORE / 1. */
	public static long cinnabarGemCount(OreDictPrefix aPrefix, boolean aDense) {
		return aPrefix == OP.blockRaw ? 9 : aDense ? 2 : 1;
	}

	/**
	 * The base duration (upstream :83): {@code 128 x mainCount x max(1, mToolQuality+1)},
	 * then blockRaw x9/2 (:101-102) and DENSE_ORE x2 (:109); the prefix byproducts add
	 * {@code units(amount, U, 64 x max(1, mToolQuality+1), T)} each (upstream :128, round-up).
	 */
	public static long crushingDuration(OreChainPlan aPlan, long aMainCount) {
		long rDuration = 128 * aMainCount * Math.max(1, aPlan.inMaterial().mToolQuality + 1);
		if (aPlan.inPrefix() == OP.blockRaw) {rDuration *= 9; rDuration /= 2;} // upstream :101-102
		if (aPlan.dense()) rDuration *= 2; // upstream :109
		for (OreDictMaterialStack tByproduct : aPlan.byproducts()) {
			rDuration += units(tByproduct.mAmount, CS.U, 64L * Math.max(1, tByproduct.mMaterial.mToolQuality + 1), true); // upstream :128
		}
		return rDuration;
	}

	/**
	 * Plan → Recipe (the upstream :68-72 / :77-137 tails), or {@code null} when the input
	 * or every main-output candidate fails to resolve (the upstream {@code mat()} → null
	 * / {@code return F} drop semantics, :81).
	 *
	 * <p>Row shapes: the main path emits the main output TWICE (the upstream sentinel +
	 * duplicate pair :77-85, both slots at 10000 — the p9 yield reform, see the class doc),
	 * then {@code extraCopies} more, then the
	 * probabilistic Cinnabar gem (if its chance sums positive, :123), then the prefix
	 * byproduct dusts (10000 each, :132). The poor-tiny branch emits one tiny output with
	 * {@code null} chances (upstream :71 passes null; every row is deterministic there).
	 * eUt is 16 and the special value 0 on both paths (the trailing ctor args :71/:137).
	 */
	@Nullable
	static Recipe buildRecipe(OreChainPlan aPlan, int aOutputItemsCount) {
		Item tInput = sMaterialItemResolver.apply(aPlan.inPrefix(), aPlan.inMaterial());
		if (tInput == null) return null; // upstream mat() → null
		OreDictMaterial tTarget = aPlan.outMaterial();
		if (aPlan.poorTinyBranch()) { // upstream :68-72
			long tCount = bindStack(units(tTarget.mTargetCrushing.mAmount, CS.U, aPlan.multiplier(), false)); // :69 — 3x folded into the plan multiplier
			ItemStack tOutput = resolveStack(OP.crushedTiny, tTarget, tCount);
			if (tOutput == null) tOutput = resolveStack(OP.dustTiny, tTarget, tCount); // :70
			if (tOutput == null) return null; // :71 ST.valid gate
			long tDuration = Math.max(1, 16 * tOutput.getCount() * Math.max(1, aPlan.inMaterial().mToolQuality + 1)); // :71
			return new Recipe(true, new ItemStack[] {new ItemStack(tInput, 1)}, new ItemStack[] {tOutput}, new FluidStack[0], new FluidStack[0], tDuration, 16, 0);
		}
		long tMainCount = mainOutputCount(aPlan.inMaterial(), aPlan.multiplier()); // :78-80
		ItemStack tMain = resolveStack(OP.crushed, tTarget, tMainCount);
		if (tMain == null) tMain = resolveStack(OP.dust, tTarget, tMainCount);
		if (tMain == null) tMain = resolveStack(OP.gem, tTarget, tMainCount);
		if (tMain == null) return null; // :81
		ItemStack[] tOutputs = new ItemStack[aOutputItemsCount];
		long[] tChances = new long[aOutputItemsCount];
		int tIndex = 0;
		tOutputs[tIndex] = tMain; tChances[tIndex++] = 10000; // slot 0: the upstream chance-0 sentinel (:78) at its ctor-rewritten effective value (:906) — the p9 reform writes 10000 directly
		tOutputs[tIndex] = tMain; tChances[tIndex++] = 10000; // slot 1: the duplicate main output (:84-85) — every row crushes into TWO main outputs (upstream parity)
		for (int i = 0; i < aPlan.extraCopies(); i++) {tOutputs[tIndex] = tMain; tChances[tIndex++] = 10000;} // :86-110 — blockRaw 7 / DENSE 2 copies
		if (tIndex < tOutputs.length && aPlan.cinnabarChance() > 0) { // :123
			tChances[tIndex] = aPlan.cinnabarChance();
			tOutputs[tIndex++] = resolveStack(OP.gem, MT.OREMATS.Cinnabar, aPlan.cinnabarCount()); // :124 — may stay null like upstream mat()
		}
		for (OreDictMaterialStack tByproduct : aPlan.byproducts()) { // :127-136
			if (tIndex >= tOutputs.length) break;
			OreDictMaterial tByproductTarget = tByproduct.mMaterial.mTargetCrushing.mMaterial;
			long tCount = bindStack(units(tByproduct.mAmount, CS.U, tByproduct.mMaterial.mTargetCrushing.mAmount, false) / CS.U); // OM.dust :460-467 simplified to the plain dust tier per the task card
			ItemStack tStack = resolveStack(OP.dust, tByproductTarget, tCount);
			if (tStack != null) {tChances[tIndex] = 10000; tOutputs[tIndex++] = tStack;} // :132-133
		}
		long tDuration = crushingDuration(aPlan, tMain.getCount()); // :83/:101-102/:109/:128
		return new Recipe(true, new ItemStack[] {new ItemStack(tInput, 1)}, tOutputs, new FluidStack[0], new FluidStack[0], tDuration, 16, 0, tChances); // :137
	}

	/** Prefix x material x count → ItemStack, or null when the pair has no item (the upstream mat() semantics). */
	@Nullable
	private static ItemStack resolveStack(OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aCount) {
		Item tItem = sMaterialItemResolver.apply(aPrefix, aMaterial);
		return tItem == null ? null : new ItemStack(tItem, (int)aCount);
	}

	/** The live item lookup (GTMaterialItems.get) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Upstream UT.Code.units (UT.java:1673-1681 inlined — the ShCL crusherCosts precedent). */
	static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit % aTargetUnit == 0) {aOriginalUnit /= aTargetUnit; aTargetUnit = 1;}
		else if (aTargetUnit % aOriginalUnit == 0) {aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	/** Upstream UT.Code.bindStack (UT.java:1568). */
	static long bindStack(long aBoundValue) {
		return Math.max(1, Math.min(64, aBoundValue));
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTemplates = null;
	}
}
