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

import java.util.List;
import java.util.function.Function;
import java.util.function.BiFunction;

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

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.fluid.GTFluids;
import gregtech6.item.GT6Circuits;
import gregtech6.registry.GTMaterialItems;

/**
 * The Fusion Reactor recipe book (task p31-fusion) — the 18 static rows of
 * Loader_Recipes_Other.java:949-966, poured into {@link GT6RecipeMaps#FUSION}.
 *
 * <p><b>The row template</b> (every line :949-966 verbatim):
 * {@code RM.Fusion.addRecipe1(F, aEUt, aDur, ST.tag(n), FL.array(inputs), FL.array(outputs), outItems)
 * .setSpecialNumber(aStartLU)} —
 * <ul>
 * <li>the {@code ST.tag(1)}/{@code ST.tag(2)} selector is a REAL item input riding slot 0
 *     (the 4th addRecipe1 arg is {@code aInput}, Recipe.java:219 — NOT a display token);
 *     count 1, the never-consumed selector identity ({@code Recipe.sNotConsumable}); it
 *     satisfies the map's {@code mMinimalInputItems = 1} (the implosion selector form);</li>
 * <li>inputs and outputs are ORDINARY gas/molten states — never plasmas (the
 *     research.p31-qu-line finding); amounts are the upstream unit walk at 1000 mB per
 *     material unit ({@code mGasUnit = mLiquidUnit = U}, OreDictMaterial.java:313, the
 *     FL.create per-U 1000): {@code gas(U*2)=2000}, {@code gas(3*U4)=750}, ...;</li>
 * <li>the two {@code EUt = 0} rows (:952 He²→Be_8 and :953 Be_8²→O) are the zero-power
 *     direct translations (the :761 else-branch floor {@code mMinEnergy = max(1, 0) = 1}
 *     rides the TU self-generation); every other row is {@code EUt = -8192} = the
 *     generator form ({@code mOutputEnergy = 8192}, the BasicMachine :761-764 trio);</li>
 * <li>{@code mSpecialValue} carries the upstream {@code setSpecialNumber} number —
 *     {@code dur*8192*16} for most rows, the {@code 8469}/{@code 94956} outliers verbatim
 *     (:954/:966). Display data only (the port has no NEI face); the ignition gate it once
 *     fed is NOT ported — the dual-layer ruling: the {@code mSpecialIsStartEnergy} flag has
 *     no registration supplier anywhere upstream, so the :755 write is unreachable and the
 *     :809 progress gate runs permanently open (mechanism live, supply broken). The data is
 *     fully carried here, so a future gate card touches ONLY the machine face.</li>
 * </ul>
 *
 * <p><b>The resolution seams</b>: the circuit selector rides
 * {@link #sCircuitResolver} ({@link GT6Circuits#selector(int)}, the implosion form); the
 * state fluids resolve through {@link #sFluidResolver} over
 * {@link GTFluids#CHEMICALS} via {@link GTFluids#specOf} (the qu-b name binding — the
 * carbon/lithium/tungsten/adamantium_molten quartet joined CHEMICAL_SPECS in this card,
 * MT.Ad being ADAMANTIUM upstream, MT.java:794); the :966 Vibranium dust rides the
 * {@link #sMaterialItemResolver} lookup (the massfab form, null = skip).
 *
 * <p><b>Load timing</b>: the {@link gregtech6.recipes.GT6RecipesImplosion} form —
 * self-contained MOD-bus listener at FMLCommonSetup.enqueueWork, generation-tracked pour
 * flag (ADR-P18).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesFusion {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The :949 start-energy unit — {@code setSpecialNumber(dur * 8192 * 16)} per material unit of the constant. */
	public static final long START_LU_PER_TICK = 8192 * 16;

	/** One transcribed row (the :949-966 line shape): fluids as material+state+mB triples. */
	public record FusionRow(String note, int selector, long eUt, long duration, long startLU,
			FluidInput[] inputs, FluidInput[] outputs, @Nullable OreDictMaterial itemOut) {
		/** The material+state+amount triple ({@code MT.X.gas/mLiquid(amount, T)} of the source line). */
		public record FluidInput(OreDictMaterial material, boolean molten, int mB) {}
	}

	/** The circuit seam (the implosion form; offline fixtures inject). */
	public static Function<Integer, ItemStack> sCircuitResolver = GT6Circuits::selector;

	/** The state-fluid seam: material+state → the registered CHEMICALS source (offline fixtures inject). */
	public static BiFunction<OreDictMaterial, Boolean, Fluid> sFluidResolver = GT6RecipesFusion::resolveFluid;

	/** The item seam (the massfab resolver form — the :966 dust.mat(MT.Vb, 1) output). */
	public static BiFunction<gregapi.oredict.OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesFusion::resolveItem;

	/**
	 * The 18 rows, upstream order :949-966. LAZILY built — the {@code @EventBusSubscriber}
	 * scan class-loads this file at MOD CONSTRUCTION, before MT/OP init (the a9027ac
	 * lesson): an eager capture would freeze null materials into the constants.
	 */
	private static volatile List<FusionRow> sRows = null;

	public static List<FusionRow> rows() {
		List<FusionRow> tRows = sRows;
		if (tRows == null) {
			// shorthand: f(mat, mB) = the gas state, m(mat, mB) = the molten/liquid state
			sRows = tRows = List.of(
				new FusionRow(":949 D-D -> He3 + T"            , 1, -8192,  730,   730L*START_LU_PER_TICK, in(f(MT.D, 2000)), out(f(MT.He_3, 500), f(MT.T, 500)), null),
				new FusionRow(":950 T-T -> He"                 , 1, -8192, 1130,  1130L*START_LU_PER_TICK, in(f(MT.T, 2000)), out(f(MT.He, 1000)), null),
				new FusionRow(":951 He3-He3 -> He"             , 1, -8192, 1290,  1290L*START_LU_PER_TICK, in(f(MT.He_3, 2000)), out(f(MT.He, 1000)), null),
				new FusionRow(":952 He-He -> Be8 (eut 0)"      , 1,     0, 1890,  1890L*START_LU_PER_TICK, in(f(MT.He, 2000)), out(m(MT.Be_8, 1000)), null),
				new FusionRow(":953 Be8 -> O (eut 0)"          , 1,     0, 3214,  3214L*START_LU_PER_TICK, in(m(MT.Be_8, 2000)), out(f(MT.O, 1000)), null),
				new FusionRow(":954 H + B11 -> 3He"            , 2, -8192,  546,  8469L*START_LU_PER_TICK, in(f(MT.H, 1000), m(MT.B_11, 1000)), out(f(MT.He, 3000)), null),
				new FusionRow(":955 H + C -> C13"              , 2, -8192,  315,   315L*START_LU_PER_TICK, in(f(MT.H, 1000), m(MT.C, 1000)), out(m(MT.C_13, 1000)), null),
				new FusionRow(":956 H + C13 -> N"              , 2, -8192,  754,   754L*START_LU_PER_TICK, in(f(MT.H, 1000), m(MT.C_13, 1000)), out(f(MT.N, 1000)), null),
				new FusionRow(":957 2H + N -> He + C + O"      , 2, -8192, 1404,  1404L*START_LU_PER_TICK, in(f(MT.H, 2000), f(MT.N, 1000)), out(f(MT.He, 500), m(MT.C, 500), f(MT.O, 500)), null),
				new FusionRow(":958 2H + O -> He + F + N"      , 2, -8192,  455,   455L*START_LU_PER_TICK, in(f(MT.H, 2000), f(MT.O, 1000)), out(f(MT.He, 500), f(MT.F, 500), f(MT.N, 500)), null),
				new FusionRow(":959 D + T -> He"               , 2, -8192, 1760,  1760L*START_LU_PER_TICK, in(f(MT.D, 1000), f(MT.T, 1000)), out(f(MT.He, 1000)), null),
				new FusionRow(":960 D + He3 -> He"             , 2, -8192, 1830,  1830L*START_LU_PER_TICK, in(f(MT.D, 1000), f(MT.He_3, 1000)), out(f(MT.He, 1000)), null),
				new FusionRow(":961 T + He3 -> He + D"         , 2, -8192, 2640,  2640L*START_LU_PER_TICK, in(f(MT.T, 1000), f(MT.He_3, 1000)), out(f(MT.He, 750), f(MT.D, 250)), null),
				new FusionRow(":962 D + Li6 -> He + He3 + Li + Be7", 2, -8192, 3336,  3336L*START_LU_PER_TICK, in(f(MT.D, 1000), m(MT.Li_6, 1000)), out(f(MT.He, 375), f(MT.He_3, 125), m(MT.Li, 125), m(MT.Be_7, 125)), null),
				new FusionRow(":963 He3 + Li6 -> 2He"          , 2, -8192, 1690,  1690L*START_LU_PER_TICK, in(f(MT.He_3, 1000), m(MT.Li_6, 1000)), out(f(MT.He, 2000)), null),
				new FusionRow(":964 He + Be8 -> C"             , 2, -8192,  736,   736L*START_LU_PER_TICK, in(f(MT.He, 1000), m(MT.Be_8, 1000)), out(m(MT.C, 1000)), null),
				new FusionRow(":965 He + C -> O"               , 2, -8192,  716,   716L*START_LU_PER_TICK, in(f(MT.He, 1000), m(MT.C, 1000)), out(f(MT.O, 1000)), null),
				new FusionRow(":966 Ad + Be7 -> W + 16He + 24He3 + 24T + Vb", 2, -8192, 1956, 94956L*START_LU_PER_TICK, in(m(MT.Ad, 1000), m(MT.Be_7, 1000)), out(m(MT.W, 1000), f(MT.He, 16000), f(MT.He_3, 24000), f(MT.T, 24000)), MT.Vb));
		}
		return tRows;
	}

	// the record-builder shorthands (the FluidInput triples)
	private static FusionRow.FluidInput[] in(FusionRow.FluidInput... aInputs) {return aInputs;}
	private static FusionRow.FluidInput[] out(FusionRow.FluidInput... aOutputs) {return aOutputs;}
	/** The gaseous state ({@code MT.X.gas}). */
	private static FusionRow.FluidInput f(OreDictMaterial aMaterial, int aMB) {return new FusionRow.FluidInput(aMaterial, false, aMB);}
	/** The molten/liquid state ({@code MT.X.liquid}). */
	private static FusionRow.FluidInput m(OreDictMaterial aMaterial, int aMB) {return new FusionRow.FluidInput(aMaterial, true, aMB);}

	/** Poured flag — one generation, one pour (the implosion form). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the implosion form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesFusion::resetForTest);}

	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesFusion::load);
	}

	/** Pours the 18 rows into the FUSION map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod
		if (GT6RecipeMaps.FUSION == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (FusionRow tRow : rows()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null || GT6RecipeMaps.FUSION.addRecipe(tRecipe) == null) {tSkipped++; continue;}
			tPoured++;
		}
		LOGGER.info("GT6 Fusion recipes poured: {} rows loaded, {} skipped (unresolvable state fluids/items = the upstream mat()/fluid null drops)", tPoured, tSkipped);
		sLoaded = true;
	}

	/**
	 * One :949-966 line as a {@link Recipe}, or null when a state fluid or the item output
	 * does not resolve (the upstream {@code mat() → null} silent drop).
	 */
	@Nullable
	public static Recipe buildRecipe(FusionRow aRow) {
		ItemStack tSelector = sCircuitResolver.apply(aRow.selector());
		if (tSelector == null || tSelector.isEmpty()) return null;

		ItemStack[] tItemOutputs = new ItemStack[0];
		if (aRow.itemOut() != null) {
			Item tItem = sMaterialItemResolver.apply(OP.dust, aRow.itemOut()); // :966 dust.mat(MT.Vb, 1)
			if (tItem == null) return null;
			tItemOutputs = new ItemStack[] {new ItemStack(tItem, 1)};
		}

		FluidStack[] tFluidInputs = new FluidStack[aRow.inputs().length];
		for (int i = 0; i < tFluidInputs.length; i++) {
			Fluid tFluid = sFluidResolver.apply(aRow.inputs()[i].material(), aRow.inputs()[i].molten());
			if (tFluid == null) return null;
			tFluidInputs[i] = new FluidStack(tFluid, aRow.inputs()[i].mB());
		}
		FluidStack[] tFluidOutputs = new FluidStack[aRow.outputs().length];
		for (int i = 0; i < tFluidOutputs.length; i++) {
			Fluid tFluid = sFluidResolver.apply(aRow.outputs()[i].material(), aRow.outputs()[i].molten());
			if (tFluid == null) return null;
			tFluidOutputs[i] = new FluidStack(tFluid, aRow.outputs()[i].mB());
		}
		return new Recipe(true,
				new ItemStack[] {tSelector},
				tItemOutputs,
				tFluidInputs, tFluidOutputs,
				aRow.duration(), aRow.eUt(), aRow.startLU()); // the setSpecialNumber payload
	}

	/** The live state-fluid lookup: specOf(material, molten) matched over {@link GTFluids#CHEMICALS}. */
	@Nullable
	private static Fluid resolveFluid(OreDictMaterial aMaterial, boolean aMolten) {
		GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(aMaterial, aMolten);
		if (tSpec == null) return null;
		for (GTFluids.ChemicalFluid tChemical : GTFluids.CHEMICALS) if (tChemical.spec.equals(tSpec)) return tChemical.source.get();
		return null;
	}

	/** The live item lookup (the massfab resolveItem form). */
	@Nullable
	private static Item resolveItem(gregapi.oredict.OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		var tHandle = GTMaterialItems.get(aPrefix, aMaterial); // RegistryObject (forge) / DeferredHolder (neo) — the var cross-leg form
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	public static void resetForTest() {
		sLoaded = false;
		sRows = null;
	}
}
