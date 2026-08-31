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
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The Coke Oven recipe book — task p6-cokeoven-processing (ADR ruling ② static pour +
 * the 2026-08-30 coordinator amendment: log recipes are TAG-driven, this file owns the
 * material-universe rows only; task p8-prefixblock-registry backfills the seven block
 * rows, 32 → 39).
 *
 * <p><b>Upstream source</b>: Loader_Recipes_Other.java:775-815 — 39
 * {@code RM.CokeOven.addRecipe1(T, 0, dur, input, NF, fluidOut, outputs...)} rows. The
 * upstream registration form {@code mat(prefix).mat(material, n)} silently drops the row
 * when the (prefix, material) pair has no item (mat() → null); the port keeps that shape:
 * the table below is DATA, and {@link #load()} resolves every row at pour time through
 * {@link GTMaterialItems#get} with a {@link GTMaterialBlocks#get} fallback (the p8 block
 * items — since p8 the block* pairs resolve), skipping + counting unresolvable rows.
 *
 * <p><b>Fluid amounts</b>: upstream {@code MT.Creosote.liquid(U2, F)} yields Forge mB
 * through OreDictMaterial.liquid (:1307-1311) — {@code units(aMaterialAmount, mLiquidUnit,
 * mLiquid.amount, F)} with the default {@code mLiquidUnit = U} (:313) and a 1000 mB
 * per-unit fluid stack — so U2 → 500, U → 1000, U4 → 250 mB (and 3*U4 → 750, 3*U2 → 1500).
 * The oil-shale rows carry {@code MT.Oil.liquid(U4/U2, F)} → 250/500 mB. The table carries
 * those resolved mB values (the port's FluidTankGT amounts are mB, the 16000 L barrel
 * precedent). The p8 block rows follow the same scale: 9*U = 9000, 9*U2 = 4500, 27*U2 =
 * 13500, 27*U4 = 6750, 9*U4 = 2250 mB (:787-789/:803-805/:815 — the :804/:805 amount is
 * 27*U4 = 6750, per the file scale; a 3375 figure in the task card contradicted its own
 * :803 = 27*U2 = 13500 line and the evidence rows).
 *
 * <p><b>Fluid identity</b> (task p7-cokeoven-backfill, spec ③): the row's fluid field is the
 * gt6 fluid id path (creosote/oil) instead of a hardcoded creosote amount; each row resolves
 * its fluid through {@link #sFluidResolver} at pour time, so an unregistered fluid skips its
 * rows exactly like the upstream absent-fluid behaviour.
 *
 * <p><b>Skipped upstream rows (the pool, not silent — asserted by the offline walk)</b>:
 * the block-family six (:787-789 Coal / :803-805 Lignite) and the oil-shale blockDust row
 * (:815) were pooled in p6/p7 (block* prefixes had no items) and are BACKFILLED by task
 * p8-prefixblock-registry — GTMaterialBlocks now registers the block universe, the two
 * resolvers below fall back to it, and the table carries all seven rows (39 total). The
 * Woods/OreDict/Crops/Tools dynamic surface (:176-180/:197-201, OreDict:205, Crops:76,
 * Tools:418) is replaced by the tag-driven {@link GT6CokeOvenTagListener} (one recipe per
 * #minecraft:logs item; beam/bamboo/wood-pellet have no tagged item and stay pooled).
 *
 * <p><b>Load timing</b> (ADR ruling ②): a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork — the ConstructMod-time init (GTMachines.onModConstruct
 * registering the maps) runs BEFORE any RegisterEvent, so items are unregistered there;
 * GT6Mod is frozen (ADR-P3-4). {@code load()} is idempotent per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesCokeOven {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The gt6 fluid id paths the table rows output (the {@code MT.X.liquid(..)} upstream slot). */
	public static final String FLUID_CREOSOTE = "creosote", FLUID_OIL = "oil";

	/** One output tuple of a table row (the upstream {@code prefix.mat(material, n)} call). */
	public record Output(OreDictPrefix prefix, OreDictMaterial material, int count) {}

	/**
	 * One transcribed upstream row: input (prefix, material, count), duration (ticks), the
	 * fluid output as a gt6 fluid id path + amount in mB ({@code <= 0} = none — the upstream
	 * NF slot), and the outputs. {@code note} carries the upstream line number for the audit
	 * walk.
	 */
	public record StaticRow(String note, OreDictPrefix inPrefix, OreDictMaterial inMaterial, int inCount,
			long duration, String fluid, long fluidMB, Output... outputs) {}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline. */
	static Function<Output, Item> sOutputItemResolver = GT6RecipesCokeOven::resolveItem;
	/** Same seam for the input side. */
	static Function<StaticRow, Item> sInputItemResolver = GT6RecipesCokeOven::resolveInput;
	/** The fluid seam (null = unregistered → the row skips, the upstream absent-fluid behaviour). */
	static Function<String, Fluid> sFluidResolver = GT6RecipesCokeOven::resolveFluid;

	/**
	 * The 39 transcribed material-universe rows (Loader_Recipes_Other.java:775-789 Coal,
	 * :791-805 Lignite, :807-815 Oilshale), order mirroring the upstream file order.
	 *
	 * <p><b>Lazily built</b>: {@code TABLE} used to be a static field, but the
	 * {@code @EventBusSubscriber} annotation scan class-loads this class at MOD CONSTRUCTION —
	 * before {@code MT.init()} — so the OP/MT references captured there were null (the live
	 * server poured 0/24; the offline tests only passed because their {@code @BeforeAll}
	 * initialized the materials first). {@link #table()} defers the capture to the first
	 * {@link #load()} call, after the material system exists.
	 */
	private static volatile List<StaticRow> sTable = null;

	/** The transcribed rows, captured on first use (one material generation). */
	public static List<StaticRow> table() {
		List<StaticRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Coal (Loader_Recipes_Other.java:775-786)
		new StaticRow(":775", OP.gem                  , MT.Coal, 1, 3600, FLUID_CREOSOTE,  500, new Output(OP.gem   , MT.CoalCoke, 1)),
		new StaticRow(":776", OP.nugget               , MT.Coal, 9, 3600, FLUID_CREOSOTE,  500, new Output(OP.ingot , MT.CoalCoke, 1)),
		new StaticRow(":777", OP.chunkGt              , MT.Coal, 4, 3600, FLUID_CREOSOTE,  500, new Output(OP.ingot , MT.CoalCoke, 1)),
		new StaticRow(":778", OP.billet               , MT.Coal, 3, 7200, FLUID_CREOSOTE, 1000, new Output(OP.ingot , MT.CoalCoke, 2)),
		new StaticRow(":779", OP.ingot                , MT.Coal, 1, 3600, FLUID_CREOSOTE,  500, new Output(OP.ingot , MT.CoalCoke, 1)),
		new StaticRow(":780", OP.oreRaw               , MT.Coal, 1, 7200, FLUID_CREOSOTE, 1000, new Output(OP.ingot , MT.CoalCoke, 2)),
		new StaticRow(":781", OP.crushed              , MT.Coal, 1, 3600, FLUID_CREOSOTE,  500, new Output(OP.ingot , MT.CoalCoke, 1)),
		new StaticRow(":782", OP.crushedTiny          , MT.Coal, 9, 3600, FLUID_CREOSOTE,  500, new Output(OP.ingot , MT.CoalCoke, 1)),
		new StaticRow(":783", OP.crushedPurified      , MT.Coal, 1, 3600, FLUID_CREOSOTE,  500, chunkOut(OP.chunkGt, MT.CoalCoke, 5)),
		new StaticRow(":784", OP.crushedPurifiedTiny  , MT.Coal, 9, 3600, FLUID_CREOSOTE,  500, chunkOut(OP.chunkGt, MT.CoalCoke, 5)),
		new StaticRow(":785", OP.crushedCentrifuged   , MT.Coal, 1, 3600, FLUID_CREOSOTE,  500, chunkOut(OP.chunkGt, MT.CoalCoke, 6)),
		new StaticRow(":786", OP.crushedCentrifugedTiny, MT.Coal, 9, 3600, FLUID_CREOSOTE, 500, chunkOut(OP.chunkGt, MT.CoalCoke, 6)),
		// Coal storage blocks (Loader_Recipes_Other.java:787-789 — backfilled by p8, GTMaterialBlocks items)
		new StaticRow(":787", OP.blockRaw                , MT.Coal, 1, 32400, FLUID_CREOSOTE, 9000, new Output(OP.blockIngot, MT.CoalCoke, 2)),
		new StaticRow(":788", OP.blockIngot              , MT.Coal, 1, 32400, FLUID_CREOSOTE, 4500, new Output(OP.blockIngot, MT.CoalCoke, 1)),
		new StaticRow(":789", OP.blockGem                , MT.Coal, 1, 32400, FLUID_CREOSOTE, 4500, new Output(OP.blockGem  , MT.CoalCoke, 1)),
		// Lignite (Loader_Recipes_Other.java:791-802)
		new StaticRow(":791", OP.gem                  , MT.Lignite, 1, 3600, FLUID_CREOSOTE,  750, new Output(OP.gem   , MT.LigniteCoke, 1)),
		new StaticRow(":792", OP.nugget               , MT.Lignite, 9, 3600, FLUID_CREOSOTE,  750, new Output(OP.ingot , MT.LigniteCoke, 1)),
		new StaticRow(":793", OP.chunkGt              , MT.Lignite, 4, 3600, FLUID_CREOSOTE,  750, new Output(OP.ingot , MT.LigniteCoke, 1)),
		new StaticRow(":794", OP.billet               , MT.Lignite, 3, 7200, FLUID_CREOSOTE, 1500, new Output(OP.ingot , MT.LigniteCoke, 2)),
		new StaticRow(":795", OP.ingot                , MT.Lignite, 1, 3600, FLUID_CREOSOTE,  750, new Output(OP.ingot , MT.LigniteCoke, 1)),
		new StaticRow(":796", OP.oreRaw               , MT.Lignite, 1, 7200, FLUID_CREOSOTE, 1500, new Output(OP.ingot , MT.LigniteCoke, 2)),
		new StaticRow(":797", OP.crushed              , MT.Lignite, 1, 3600, FLUID_CREOSOTE,  750, new Output(OP.ingot , MT.LigniteCoke, 1)),
		new StaticRow(":798", OP.crushedTiny          , MT.Lignite, 9, 3600, FLUID_CREOSOTE,  750, new Output(OP.ingot , MT.LigniteCoke, 1)),
		new StaticRow(":799", OP.crushedPurified      , MT.Lignite, 1, 3600, FLUID_CREOSOTE,  750, chunkOut(OP.chunkGt, MT.LigniteCoke, 5)),
		new StaticRow(":800", OP.crushedPurifiedTiny  , MT.Lignite, 9, 3600, FLUID_CREOSOTE,  750, chunkOut(OP.chunkGt, MT.LigniteCoke, 5)),
		new StaticRow(":801", OP.crushedCentrifuged   , MT.Lignite, 1, 3600, FLUID_CREOSOTE,  750, chunkOut(OP.chunkGt, MT.LigniteCoke, 6)),
		new StaticRow(":802", OP.crushedCentrifugedTiny, MT.Lignite, 9, 3600, FLUID_CREOSOTE, 750, chunkOut(OP.chunkGt, MT.LigniteCoke, 6)),
		// Lignite storage blocks (Loader_Recipes_Other.java:803-805 — backfilled by p8, GTMaterialBlocks items)
		new StaticRow(":803", OP.blockRaw              , MT.Lignite, 1, 32400, FLUID_CREOSOTE, 13500, new Output(OP.blockIngot, MT.LigniteCoke, 2)),
		new StaticRow(":804", OP.blockIngot            , MT.Lignite, 1, 32400, FLUID_CREOSOTE,  6750, new Output(OP.blockIngot, MT.LigniteCoke, 1)),
		new StaticRow(":805", OP.blockGem              , MT.Lignite, 1, 32400, FLUID_CREOSOTE,  6750, new Output(OP.blockGem  , MT.LigniteCoke, 1)),
		// Oilshale (Loader_Recipes_Other.java:807-814 backfilled by p7; :815 blockDust backfilled by p8)
		new StaticRow(":807", OP.dust                  , MT.Oilshale, 1, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":808", OP.oreRaw                , MT.Oilshale, 1, 7200, FLUID_OIL, 500, new Output(OP.dustTiny, MT.Asphalt, 2)),
		new StaticRow(":809", OP.crushed               , MT.Oilshale, 1, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":810", OP.crushedTiny           , MT.Oilshale, 9, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":811", OP.crushedPurified       , MT.Oilshale, 1, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":812", OP.crushedPurifiedTiny   , MT.Oilshale, 9, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":813", OP.crushedCentrifuged    , MT.Oilshale, 1, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":814", OP.crushedCentrifugedTiny, MT.Oilshale, 9, 3600, FLUID_OIL, 250, new Output(OP.dustTiny, MT.Asphalt, 1)),
		new StaticRow(":815", OP.blockDust             , MT.Oilshale, 1, 32400, FLUID_OIL, 2250, new Output(OP.dust, MT.Asphalt, 1)));
		return tTable;
	}

	/** The coal/lignite chunk-family outputs: n identical chunkGt stacks (:783-786/:799-802). */
	private static Output[] chunkOut(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {
		Output[] rOutputs = new Output[aCount];
		for (int i = 0; i < aCount; i++) rOutputs[i] = new Output(aPrefix, aMaterial, 1);
		return rOutputs;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc):
	 * the block rows (:787-789/:803-805/:815) were p6/p7 pool and are BACKFILLED by task
	 * p8-prefixblock-registry (GTMaterialBlocks + the resolver fallback); the dynamic
	 * log/beam family is now owned by the tag listener (beam/bamboo/wood-pellet have no
	 * tagged counterpart → pooled).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
		"Loader_Recipes_Other.java:787-789/:803-805 — blockRaw/blockIngot/blockGem x Coal/Lignite: BACKFILLED by p8-prefixblock-registry (GTMaterialBlocks block items; was the p6 pool)",
		"Loader_Recipes_Other.java:815 — Oilshale blockDust row: BACKFILLED by p8-prefixblock-registry (blockDust joined the block universe; was the p7 ruling)",
		"Loader_Recipes_Woods.java:197-201 — beam family (no beam item; p6 pool)",
		"Loader_Recipes_Woods.java:165-180 log family — replaced by the #minecraft:logs tag listener (coordinator amendment 2026-08-30)",
		"Loader_Recipes_Other.java:205 OreDict listener — no dynamic oredict surface; static pour only",
		"Loader_Crops.java:76 bamboo / Loader_Tools.java:418 wood bullet (no counterpart item; p6 pool)");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	/** FMLCommonSetup.enqueueWork — items and fluids are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesCokeOven::load);
	}

	/** Pours the table into {@link GT6RecipeMaps#COKE_OVEN}. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) return;
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java:91), tests may race it
		RecipeMap tMap = GT6RecipeMaps.COKE_OVEN;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (StaticRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // = upstream mat() → null silent drop
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Coke Oven recipes poured: {} loaded, {} skipped (unresolvable prefix/material/fluid pairs, = upstream mat() null drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when any segment fails to resolve (the upstream silent-drop semantics). */
	static Recipe buildRecipe(StaticRow aRow) {
		Item tInput = sInputItemResolver.apply(aRow);
		if (tInput == null) return null;

		ItemStack[] tOutputs = new ItemStack[aRow.outputs().length];
		for (int i = 0; i < tOutputs.length; i++) {
			Item tItem = sOutputItemResolver.apply(aRow.outputs()[i]);
			if (tItem == null) return null;
			tOutputs[i] = new ItemStack(tItem, aRow.outputs()[i].count());
		}

		FluidStack[] tFluidOutputs;
		if (aRow.fluidMB() > 0) {
			Fluid tFluid = sFluidResolver.apply(aRow.fluid());
			if (tFluid == null) return null;
			tFluidOutputs = new FluidStack[] {new FluidStack(tFluid, (int)aRow.fluidMB())};
		} else {
			tFluidOutputs = new FluidStack[0];
		}

		// the upstream addRecipe1(T, 0, dur, ...) shape: buffered, EUt 0, no special value
		return new Recipe(true, new ItemStack[] {new ItemStack(tInput, aRow.inCount())}, tOutputs,
				new FluidStack[0], tFluidOutputs, aRow.duration(), 0, 0);
	}

	/** The live item lookup (GTMaterialItems.get :287) with the p8 block-item fallback (GTMaterialBlocks.get) — null when the pair has neither. */
	@Nullable
	private static Item resolveItem(Output aOutput) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aOutput.prefix(), aOutput.material());
		if (tHandle == null) tHandle = GTMaterialBlocks.get(aOutput.prefix(), aOutput.material()); // p8: the block universe
		return tHandle == null ? null : tHandle.get();
	}

	@Nullable
	private static Item resolveInput(StaticRow aRow) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aRow.inPrefix(), aRow.inMaterial());
		if (tHandle == null) tHandle = GTMaterialBlocks.get(aRow.inPrefix(), aRow.inMaterial()); // p8: the block universe
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The live fluid lookup by gt6 id path — null for an unknown path (the row skips, the
	 * upstream absent-fluid semantics) and while the DeferredRegister has not fired yet.
	 */
	@Nullable
	private static Fluid resolveFluid(String aFluidId) {
		if (FLUID_CREOSOTE.equals(aFluidId)) return GTFluids.CREOSOTE.get();
		if (FLUID_OIL.equals(aFluidId)) return GTFluids.OIL.get();
		return null;
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTable = null;
	}
}
