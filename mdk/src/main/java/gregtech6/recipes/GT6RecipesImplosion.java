/**
 * Copyright (c) 2026 GregTech-6 Team
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
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.GT6Circuits;
import gregtech6.registry.GTMaterialItems;

/**
 * The Implosion Compressor recipe book (task p31-implosion) — the per-gem-material
 * 4-tier walk of Loader_Recipes_Other.java:709-764, poured into
 * {@link GT6RecipeMaps#IMPLOSION}.
 *
 * <p><b>The upstream census</b>: one TNT loop wrapping nine material groups — the TNT
 * array :709 carries FIVE branches ({@code ST.make(Blocks.tnt, 8, W)},
 * {@code IL.IC2_ITNT.get(4)}, {@code IL.Boomstick.get(12)}, {@code IL.Dynamite.get(2)},
 * {@code IL.Dynamite_Strong.get(1)}), every group walks the SAME four tier arms
 * (:711-714 template): tag(0) dust1 + TNT → plateGem, tag(1) dust1 + TNT → gem,
 * tag(2) dust2 + 4x TNT → gemFlawless, tag(3) dust4 + 8x TNT → gemExquisite, all
 * eUt 0 / duration 256.
 *
 * <p><b>The TNT branch cut</b> (the card spec ②): ONLY the vanilla TNT branch pours —
 * the other four branches are {@code IL.} items with no port identity (IC2_ITNT,
 * Boomstick, Dynamite, Dynamite_Strong — the foreign-IL pool, the P10 ruling shape),
 * declared in {@link #SKIPPED_UPSTREAM}. The vanilla branch is {@code count 8}: every
 * tier consumes 8/8/32/64 TNT per process.
 *
 * <p><b>The group walk</b> (:710-763 verbatim): Diamond → MT.DiamondIndustrial,
 * Sapphire → MT.Sapphire, Emerald → MT.Emerald, then the identity-output groups
 * Amethyst/Garnet/Jasper/TigerEye/Aventurine plus the :758 explicit singles
 * (Spinel..Craponite) → the material itself. The group membership rides
 * {@code ANY.<G>.mToThis} — the re-registration sets the port's root gregapi builds
 * (put(ANY.Diamond) → addReRegistrations, OreDictMaterial.java:947-953), so the
 * walk is the upstream iteration verbatim; unregistered prefix items resolve to null
 * and skip (the upstream {@code mat() → null} silent drop).
 *
 * <p><b>The selector arms</b>: {@code ST.tag(0..3)} = the Integrated Circuit at
 * configuration 0..3 (GT6Circuits.selector — the count-1 Damage-tagged stack, the
 * never-consumed identity rides Recipe.sNotConsumable, the Distillery :534-541 form).
 * Three real item inputs per row (dust + TNT + selector) satisfy the map's
 * mMinimalInputItems = 3.
 *
 * <p><b>Load timing</b>: the GT6RecipesCompressor form — self-contained MOD-bus
 * listener at FMLCommonSetup.enqueueWork, lazily built table, generation-tracked pour
 * flag (ADR-P18).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesImplosion {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One transcribed group arm — the material source walk plus its output (null group = the :758 explicit single). */
	public record ImplosionRow(String note, OreDictMaterial material, OreDictMaterial output) {}

	/** One transcribed tier arm (the :711-714 template, vanilla-TNT branch counts). */
	public record ImplosionTier(String note, int dustCount, int tntCount, int selectorConfig, OreDictPrefix outPrefix) {}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline (the ShCL precedent). */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesImplosion::resolveItem;
	/** The vanilla item seam (the compressor sVanillaItemResolver form). */
	public static Supplier<Item> sTntResolver = () -> Blocks.TNT.asItem();
	/** The circuit seam: the live selector stack ({@link GT6Circuits#selector(int)}), fixtures injected offline (the Distillery form). */
	public static Function<Integer, ItemStack> sCircuitResolver = GT6Circuits::selector;

	/** The four tier arms, vanilla-TNT branch (upstream order :711-714). */
	public static final List<ImplosionTier> TIERS = List.of(
			new ImplosionTier(":711", 1,  8, 0, OP.plateGem     ),
			new ImplosionTier(":712", 1,  8, 1, OP.gem          ),
			new ImplosionTier(":713", 2, 32, 2, OP.gemFlawless  ),
			new ImplosionTier(":714", 4, 64, 3, OP.gemExquisite));

	/** The lazily built group table (MT.init at class-load lesson — the a9027ac form). */
	private static volatile List<ImplosionRow> sRows = null;

	/**
	 * The transcribed group walk (:710-763), captured on first use (one material
	 * generation). The group material constant is the note anchor (the :710/:716/:722
	 * group openers, the :728/:734/:740/:746/:752 identity groups, the :758 singles).
	 */
	public static List<ImplosionRow> table() {
		List<ImplosionRow> tTable = sRows;
		if (tTable == null) {
			List<ImplosionRow> tRows = new ArrayList<>();
			// :710-715 — ANY.Diamond → MT.DiamondIndustrial (the Industrial Diamond conversion)
			for (OreDictMaterial tMat : ANY.Diamond.mToThis) tRows.add(new ImplosionRow(":710", tMat, MT.DiamondIndustrial));
			// :716-721 — ANY.Sapphire → MT.Sapphire
			for (OreDictMaterial tMat : ANY.Sapphire.mToThis) tRows.add(new ImplosionRow(":716", tMat, MT.Sapphire));
			// :722-727 — ANY.Emerald → MT.Emerald
			for (OreDictMaterial tMat : ANY.Emerald.mToThis) tRows.add(new ImplosionRow(":722", tMat, MT.Emerald));
			// :728-757 — the identity-output groups
			for (OreDictMaterial tMat : ANY.Amethyst  .mToThis) tRows.add(new ImplosionRow(":728", tMat, tMat));
			for (OreDictMaterial tMat : ANY.Garnet    .mToThis) tRows.add(new ImplosionRow(":734", tMat, tMat));
			for (OreDictMaterial tMat : ANY.Jasper    .mToThis) tRows.add(new ImplosionRow(":740", tMat, tMat));
			for (OreDictMaterial tMat : ANY.TigerEye  .mToThis) tRows.add(new ImplosionRow(":746", tMat, tMat));
			for (OreDictMaterial tMat : ANY.Aventurine.mToThis) tRows.add(new ImplosionRow(":752", tMat, tMat));
			// :758 — the explicit singles (identity output)
			for (OreDictMaterial tMat : new OreDictMaterial[] {MT.Spinel, MT.BalasRuby, MT.Topaz, MT.BlueTopaz, MT.Tanzanite, MT.Zanite, MT.Amazonite, MT.Alexandrite, MT.Opal, MT.OnyxRed, MT.OnyxBlack, MT.Peridot, MT.Dioptase, MT.Craponite}) {
				tRows.add(new ImplosionRow(":758", tMat, tMat));
			}
			sRows = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Other.java:709 the four non-vanilla TNT branches (IL.IC2_ITNT x4, IL.Boomstick x12, IL.Dynamite x2, IL.Dynamite_Strong x1) — foreign-IL items with no port identity, the card spec cut (P10 ruling shape); the vanilla branch (Blocks.tnt x8) is the only pour");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the GT6RecipesShCL form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesImplosion::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesImplosion::load);
	}

	/** Pours the tables into the IMPLOSION map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines)
		if (GT6RecipeMaps.IMPLOSION == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (ImplosionRow tRow : table()) {
			for (ImplosionTier tTier : TIERS) {
				Recipe tRecipe = buildRecipe(tRow, tTier);
				if (tRecipe == null || GT6RecipeMaps.IMPLOSION.addRecipe(tRecipe) == null) {tSkipped++; continue;} // mat() → null / the ghost guard
				tPoured++;
			}
		}
		LOGGER.info("GT6 Implosion recipes poured: {} rows loaded, {} skipped (unresolvable prefix items = upstream mat() null returns)", tPoured, tSkipped);
		sLoaded = true;
	}

	/**
	 * Row x tier → Recipe (the :711-714 arm), or null when a leg has no item (the
	 * upstream {@code mat() → null} drop). Inputs: dust + TNT + the tier selector
	 * (ST.tag(config)); output: the tier prefix on the row output.
	 */
	@Nullable
	public static Recipe buildRecipe(ImplosionRow aRow, ImplosionTier aTier) {
		Item tDust = sMaterialItemResolver.apply(OP.dust, aRow.material());
		if (tDust == null) return null; // upstream dust.mat(tMat, n) → null
		Item tTnt = sTntResolver.get();
		if (tTnt == null) return null;
		Item tOut = sMaterialItemResolver.apply(aTier.outPrefix(), aRow.output());
		if (tOut == null) return null; // upstream plateGem/gem.mat(tOutput, 1) → null
		ItemStack tSelector = sCircuitResolver.apply(aTier.selectorConfig());
		if (tSelector == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tDust, aTier.dustCount()), new ItemStack(tTnt, aTier.tntCount()), tSelector},
				new ItemStack[] {new ItemStack(tOut, 1)},
				new FluidStack[0], new FluidStack[0], 256, 0, 0);
	}

	/** The live item lookup (GTMaterialItems.get) — null when the pair has no item-path item (the compressor resolveItem form). */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		net.minecraftforge.registries.RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. Public — the BE-package e2e shares the reset. */
	public static void resetForTest() {
		sLoaded = false;
		sRows = null;
	}
}
