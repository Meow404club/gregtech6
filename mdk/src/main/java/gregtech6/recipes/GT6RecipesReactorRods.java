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
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.items.GT6ReactorRods;
import gregtech6.reactor.ReactorRodNbt;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.registry.GTMaterialItems;

/**
 * The reactor-rod recipe pour (task debt-reactor-c-rods) — the Canner filling/unpacking
 * and Centrifuge recycling rows of Loader_MultiTileEntities.java:741-790 verbatim over
 * the port Recipe ctor (the GT6RecipesCanner shape): 25 Canner rows (the 3 functional
 * fill + the 17 fuel fill + the 4 breeder fill + the Tritium unpack, :742-744/:746-762/
 * :782-785/:789) and 20 Centrifuge rows (the 17 depleted + the 3 solid enriched,
 * :764-780/:787-790). The two core CRAFTING rows (:734/:738) stay pooled — the 'P'
 * (IL.PISTONS[4]) leg has no port item (the GT6CraftingRecipes.java:2867 absent-component
 * ruling, declared on GT6ReactorRods; the former 'M' OP.casingMachineDense absence is
 * retired — the family is registered since casing-machine-register).
 *
 * <p><b>Row shape</b> — the fill rows are {@code addRecipe2(F, 16, 16, <stick|bolt×4>,
 * IL.Reactor_Rod_Empty.get(1), theRod)}: NOT buffered (the upstream F, unlike the laser
 * rows' T), no fluids. The fuel/breeder outputs carry {@code gt.maxdurability} (the
 * registration NBT the 1.7.10 item template bakes and the Canner output carries —
 * Nuclear.java:49's fresh-rod fallback) written through
 * {@link ReactorRodNbt#setMaxDurability}; a rod without it would read durability 0 and
 * deplete on its first reactor tick (the B-card fixture lesson). The Tritium row is the
 * {@code addRecipe1(F, 16, 16, rod, new FluidStack[]{}, FL.amount(MT.T.mGas, 500L),
 * IL.Reactor_Rod_Empty.get(1))} unpack: no fluid inputs, 500 mB of tritium gas OUT plus
 * the empty rod (the R4 mB 1:1 ruling — FL.amount IS mB). The Centrifuge rows are
 * {@code addRecipe1(F, 64|512, 256, rod, ZL_FS, ZL_FS, scrapGt(Zr, 9), dustTiny(aMat, 1|4),
 * dustDiv72(otherMat, 6|4))} — three item outputs, no fluids, the 512-EU outlier being
 * the Enriched Naquadah Enriched Rod (:790).
 *
 * <p><b>Seams</b> (the GT6RecipesCanner/Compressor form): the rod leg resolves through
 * {@link GT6ReactorRods#BY_ID}, the material legs through {@link GT6MaterialItems}, the
 * tritium leg through the GTFluids CHEMICALS walk — fixtures injected offline where the
 * RegistryObjects are unbound. The tables are LAZILY built — the {@code @EventBusSubscriber}
 * scan class-loads at MOD CONSTRUCTION, before MT.init() (the a9027ac lesson, the
 * Compressor table() form).
 *
 * <p><b>Load timing</b>: FMLCommonSetup.enqueueWork (the Canner/Bees precedent).
 * Idempotent per generation; an unresolvable row skips SILENTLY with a count (the
 * upstream {@code mat()} null-drop semantics).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesReactorRods {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The EUt column of every Canner row (:742-744/:746-762/:782-785/:789 — the first addRecipe2/1 argument). */
	public static final long CANNER_EUT = 16;

	/** The duration column of every Canner row (the second argument). */
	public static final long CANNER_DURATION = 16;

	/** The EUt column of the depleted + U-233/Pu-239 Centrifuge rows (:764-788 — 64). */
	public static final long CENTRIFUGE_EUT = 64;

	/** The EUt of the Enriched Naquadah Enriched Rod row (:790 — 512, the family outlier). */
	public static final long CENTRIFUGE_EUT_NQ = 512;

	/** The duration column of every Centrifuge row (:764-790 — 256). */
	public static final long CENTRIFUGE_DURATION = 256;

	/** The Tritium unpack fluid amount (:789 — {@code FL.amount(MT.T.mGas, 500L)}, mB 1:1). */
	public static final int TRITIUM_MB = 500;

	/** The Zr scrap column shared by every Centrifuge row ({@code OP.scrapGt.mat(MT.Zr, 9)}). */
	public static final int SCRAP_COUNT = 9;

	/** One Canner fill row: the rod id over its (stick | bolt×4) material column. */
	public record FillRow(int rodId, OreDictPrefix prefix, OreDictMaterial material, int materialCount) {}

	/**
	 * One Centrifuge row: the rod id over its two dust columns ({@code dustTiny(tinyMat,
	 * tinyCount)} + {@code dustDiv72(div72Mat, div72Count)}) and its EUt (64, or 512 for
	 * the :790 Naquadah outlier). The Zr scrap leg ({@code scrapGt(Zr, 9)}) is family-wide.
	 */
	public record CentrifugeRow(int rodId, long eUt, OreDictMaterial tinyMat, int tinyCount, OreDictMaterial div72Mat, int div72Count) {}

	/** The resolution seams (the Canner/Compressor fixture-injection form). */
	public static IntFunction<ItemStack> sRodResolver = GT6RecipesReactorRods::liveRod;
	public static BiFunction<OreDictPrefix, OreDictMaterial, ItemStack> sMaterialResolver = GT6RecipesReactorRods::liveMaterial;
	public static Supplier<Fluid> sTritiumResolver = GT6RecipesReactorRods::liveTritium;

	/** Lazily built fill table — class-load happens before MT.init() (the a9027ac lesson). */
	private static volatile List<FillRow> sFillRows = null;

	/** Lazily built centrifuge table (same discipline). */
	private static volatile List<CentrifugeRow> sCentrifugeRows = null;

	/**
	 * The 24 fill rows: :742-744 (the Cd-In-Ag/Be/Graphite functional sticks), :746-762
	 * (the 17 fuel sticks) and :782-785 (the 4 breeder bolt×4 columns), upstream order.
	 */
	public static List<FillRow> fillRows() {
		List<FillRow> tRows = sFillRows;
		if (tRows == null) sFillRows = tRows = List.of(
			// :742-744 — the functional three (the Empty Rod itself has no recipe)
			new FillRow(9202, OP.stick, MT.Cd_In_Ag_Alloy, 1),
			new FillRow(9203, OP.stick, MT.Be            , 1),
			new FillRow(9204, OP.stick, MT.Graphite      , 1),
			// :746-762 — the 17 fuel sticks
			new FillRow(9210, OP.stick, MT.Th        , 1),
			new FillRow(9219, OP.stick, MT.Cyanite   , 1),
			new FillRow(9220, OP.stick, MT.U_238     , 1),
			new FillRow(9221, OP.stick, MT.U_235     , 1),
			new FillRow(9222, OP.stick, MT.U_233     , 1),
			new FillRow(9229, OP.stick, MT.Yellorium , 1),
			new FillRow(9230, OP.stick, MT.Pu        , 1),
			new FillRow(9231, OP.stick, MT.Pu_241    , 1),
			new FillRow(9232, OP.stick, MT.Pu_243    , 1),
			new FillRow(9233, OP.stick, MT.Pu_239    , 1),
			new FillRow(9239, OP.stick, MT.Blutonium , 1),
			new FillRow(9240, OP.stick, MT.Am        , 1),
			new FillRow(9241, OP.stick, MT.Am_241    , 1),
			new FillRow(9249, OP.stick, MT.Ludicrite , 1),
			new FillRow(9250, OP.stick, MT.Co_60     , 1),
			new FillRow(9260, OP.stick, MT.Nq_528    , 1),
			new FillRow(9261, OP.stick, MT.Nq_522    , 1),
			// :782-785 — the 4 breeder bolt columns
			new FillRow(9410, OP.bolt, MT.Th    , 4),
			new FillRow(9420, OP.bolt, MT.U_238 , 4),
			new FillRow(9430, OP.bolt, MT.Li    , 4),
			new FillRow(9440, OP.bolt, MT.Nq    , 4));
		return tRows;
	}

	/**
	 * The 20 Centrifuge rows: :764-780 (the 17 depleted, each back into its own material
	 * plus the NEXT material down the breeding chain) and :787-790 (the 3 solid enriched,
	 * 4-of-each columns; :789 Tritium is the Canner unpack instead), upstream order.
	 */
	public static List<CentrifugeRow> centrifugeRows() {
		List<CentrifugeRow> tRows = sCentrifugeRows;
		if (tRows == null) sCentrifugeRows = tRows = List.of(
			// :764-780 — the 17 depleted rods
			new CentrifugeRow(9310, CENTRIFUGE_EUT, MT.Th        , 1, MT.U_238    , 6),
			new CentrifugeRow(9319, CENTRIFUGE_EUT, MT.Cyanite   , 1, MT.Blutonium, 6),
			new CentrifugeRow(9320, CENTRIFUGE_EUT, MT.U_238     , 1, MT.U_235    , 6),
			new CentrifugeRow(9321, CENTRIFUGE_EUT, MT.U_235     , 1, MT.Pu       , 6),
			new CentrifugeRow(9322, CENTRIFUGE_EUT, MT.U_233     , 1, MT.Pu_243   , 6),
			new CentrifugeRow(9329, CENTRIFUGE_EUT, MT.Yellorium , 1, MT.Cyanite  , 6),
			new CentrifugeRow(9330, CENTRIFUGE_EUT, MT.Pu        , 1, MT.Pu_241   , 6),
			new CentrifugeRow(9331, CENTRIFUGE_EUT, MT.Pu_241    , 1, MT.Pu_243   , 6),
			new CentrifugeRow(9332, CENTRIFUGE_EUT, MT.Pu_243    , 1, MT.Am       , 6),
			new CentrifugeRow(9333, CENTRIFUGE_EUT, MT.Pu_239    , 1, MT.Am_241   , 6),
			new CentrifugeRow(9339, CENTRIFUGE_EUT, MT.Blutonium , 1, MT.Ludicrite, 6),
			new CentrifugeRow(9340, CENTRIFUGE_EUT, MT.Am        , 1, MT.Am_241   , 6),
			new CentrifugeRow(9341, CENTRIFUGE_EUT, MT.Am_241    , 1, MT.Nq_528   , 6),
			new CentrifugeRow(9349, CENTRIFUGE_EUT, MT.Ludicrite , 1, MT.Yellorium, 6),
			new CentrifugeRow(9350, CENTRIFUGE_EUT, MT.Co_60     , 1, MT.Th       , 6),
			new CentrifugeRow(9360, CENTRIFUGE_EUT, MT.Nq_528    , 1, MT.Nq_522   , 6),
			new CentrifugeRow(9361, CENTRIFUGE_EUT, MT.Nq_522    , 1, MT.Co_60    , 6),
			// :787/:788/:790 — the 3 solid enriched rods (:789 Tritium is the Canner unpack)
			new CentrifugeRow(9411, CENTRIFUGE_EUT    , MT.U_233 , 4, MT.Th    , 4),
			new CentrifugeRow(9421, CENTRIFUGE_EUT    , MT.Pu_239, 4, MT.U_238 , 4),
			new CentrifugeRow(9441, CENTRIFUGE_EUT_NQ , MT.Nq_528, 4, MT.Nq    , 4));
		return tRows;
	}

	/** Poured flag — one generation, one pour (the Canner form). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesReactorRods::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the items/material system are live by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesReactorRods::load);
	}

	/**
	 * Pours the 45 rows: 24 Canner fills + the Tritium unpack into {@link GT6RecipeMaps#CANNER},
	 * the 20 Centrifuge rows into {@link GT6RecipeMaps#CENTRIFUGE}. Idempotent; an
	 * unresolvable leg skips with a count (= the upstream mat() null drops).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent (the Canner/Bees form)
		gregtech6.recipes.maps.GT6RecipeMapCanner tCanner = GT6RecipeMaps.CANNER;
		RecipeMap tCentrifuge = GT6RecipeMaps.CENTRIFUGE;
		if (tCanner == null || tCentrifuge == null) return; // reset() between init and load — a broken lifecycle

		int tPoured = 0, tSkipped = 0;
		for (FillRow tRow : fillRows()) {
			Recipe tRecipe = fillRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the unresolvable stick/bolt/rod silent skip
			tCanner.addRecipe(tRecipe);
			tPoured++;
		}
		Recipe tTritium = tritiumRecipe();
		if (tTritium == null) tSkipped++;
		else {tCanner.addRecipe(tTritium); tPoured++;}

		int tCentPoured = 0, tCentSkipped = 0;
		for (CentrifugeRow tRow : centrifugeRows()) {
			Recipe tRecipe = centrifugeRecipe(tRow);
			if (tRecipe == null) {tCentSkipped++; continue;}
			tCentrifuge.addRecipe(tRecipe);
			tCentPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Reactor Rods poured: {} canner ({} skipped), {} centrifuge ({} skipped) — the unresolvable legs drop like upstream mat() nulls",
				tPoured, tSkipped, tCentPoured, tCentSkipped);
	}

	/**
	 * The :742-744/:746-762/:782-785 row for ONE fill — NOT buffered, EUt 16, duration 16,
	 * [the stick | bolt×4, the empty rod] in, the filled rod out. Fuel/breeder outputs
	 * carry gt.maxdurability (the registration-NBT bake). Null when any leg fails.
	 */
	@Nullable
	static Recipe fillRecipe(FillRow aRow) {
		ItemStack tMaterial = sMaterialResolver.apply(aRow.prefix(), aRow.material());
		if (tMaterial == null || tMaterial.isEmpty()) return null;
		tMaterial = tMaterial.copy();
		tMaterial.setCount(aRow.materialCount());
		ItemStack tEmpty = sRodResolver.apply(9201);
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tFilled = sRodResolver.apply(aRow.rodId());
		if (tFilled == null || tFilled.isEmpty()) return null;
		tFilled = freshRod(aRow.rodId(), tFilled);
		// upstream :742 form — RM.Canner.addRecipe2(F, 16, 16, OP.stick.mat(aMat, 1), IL.Reactor_Rod_Empty.get(1), aRegistry.getItem())
		return new Recipe(false,
				new ItemStack[] {tMaterial, tEmpty.copy()}, new ItemStack[] {tFilled},
				null, null,
				CANNER_DURATION, CANNER_EUT, 0);
	}

	/**
	 * The fresh-rod burn budget: fuel → {@link FuelRodSpec#durability}, breeder →
	 * {@link BreederRodSpec#needed} (the registration NBT_MAXDURABILITY column), other
	 * kinds unchanged. The Nuclear.java:49 fallback face — a budget-less fuel/breeder
	 * would deplete on its first reactor tick.
	 */
	static ItemStack freshRod(int aRodId, ItemStack aStack) {
		java.util.Optional<FuelRodSpec> tFuel = FuelRodSpec.byId(aRodId);
		if (tFuel.isPresent()) {ReactorRodNbt.setMaxDurability(aStack, tFuel.get().durability()); return aStack;}
		java.util.Optional<BreederRodSpec> tBreeder = BreederRodSpec.byId(aRodId);
		if (tBreeder.isPresent()) ReactorRodNbt.setMaxDurability(aStack, tBreeder.get().needed());
		return aStack;
	}

	/**
	 * The :789 Tritium unpack — NOT buffered, EUt 16, duration 16, the Tritium Enriched
	 * Rod in, NO fluid inputs, 500 mB of tritium gas + the empty rod out. Null when any
	 * leg fails to resolve.
	 */
	@Nullable
	static Recipe tritiumRecipe() {
		ItemStack tRod = sRodResolver.apply(9431);
		if (tRod == null || tRod.isEmpty()) return null;
		ItemStack tEmpty = sRodResolver.apply(9201);
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		Fluid tTritium = sTritiumResolver.get();
		if (tTritium == null) return null;
		// upstream :789 — RM.Canner.addRecipe1(F, 16, 16, aRegistry.getItem(), new FluidStack[] {}, FL.amount(MT.T.mGas, 500L), IL.Reactor_Rod_Empty.get(1))
		return new Recipe(false,
				new ItemStack[] {tRod}, new ItemStack[] {tEmpty},
				new FluidStack[] {}, new FluidStack[] {new FluidStack(tTritium, TRITIUM_MB)},
				CANNER_DURATION, CANNER_EUT, 0);
	}

	/**
	 * The :764-780/:787-790 row for ONE rod — NOT buffered, EUt 64 (512 for the :790
	 * Naquadah outlier), duration 256, the rod in, [scrapGt(Zr)×9, dustTiny(mat, 1|4),
	 * dustDiv72(other, 6|4)] out. Null when any leg fails to resolve.
	 */
	@Nullable
	static Recipe centrifugeRecipe(CentrifugeRow aRow) {
		ItemStack tRod = sRodResolver.apply(aRow.rodId());
		if (tRod == null || tRod.isEmpty()) return null;
		ItemStack tScrap = sMaterialResolver.apply(OP.scrapGt, MT.Zr);
		if (tScrap == null || tScrap.isEmpty()) return null;
		tScrap = tScrap.copy();
		tScrap.setCount(SCRAP_COUNT);
		ItemStack tTiny = sMaterialResolver.apply(OP.dustTiny, aRow.tinyMat());
		if (tTiny == null || tTiny.isEmpty()) return null;
		tTiny = tTiny.copy();
		tTiny.setCount(aRow.tinyCount());
		ItemStack tDiv72 = sMaterialResolver.apply(OP.dustDiv72, aRow.div72Mat());
		if (tDiv72 == null || tDiv72.isEmpty()) return null;
		tDiv72 = tDiv72.copy();
		tDiv72.setCount(aRow.div72Count());
		// upstream :764 form — RM.Centrifuge.addRecipe1(F, 64, 256, aRegistry.getItem(), ZL_FS, ZL_FS, OP.scrapGt.mat(MT.Zr, 9), OP.dustTiny.mat(aMat, 1), OP.dustDiv72.mat(MT.X, 6))
		return new Recipe(false,
				new ItemStack[] {tRod}, new ItemStack[] {tScrap, tTiny, tDiv72},
				new FluidStack[] {}, new FluidStack[] {},
				CENTRIFUGE_DURATION, aRow.eUt(), 0);
	}

	/** The live rod leg ({@link GT6ReactorRods#rodById}) — null when the id has no item. */
	@Nullable
	static ItemStack liveRod(int aId) {
		return GT6ReactorRods.rodById(aId).map(ItemStack::new).orElse(null);
	}

	/** The live material leg ({@link GT6MaterialItems#get}) — null when the pair has no item. */
	@Nullable
	static ItemStack liveMaterial(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : new ItemStack(tHandle.get());
	}

	/** The live tritium leg: the {@link GTFluids#CHEMICALS} source fluid of "tritium" (the laser-gas walk form). */
	@Nullable
	static Fluid liveTritium() {
		for (GTFluids.ChemicalFluid tChemical : GTFluids.CHEMICALS) {
			if (tChemical.spec.name().equals("tritium")) return tChemical.source.get();
		}
		return null;
	}

	/** Test seam: clears the poured flag AND the captured tables so a fresh generation can re-pour (the Compressor form — the MT/OP statics are per-generation). */
	public static void resetForTest() {
		sLoaded = false;
		sFillRows = null;
		sCentrifugeRows = null;
	}

	private GT6RecipesReactorRods() {}
}
