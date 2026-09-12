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

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import gregapi.data.CS;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Anvil recipe book (task p28-c-anvil) — the registration-time expansion of the
 * upstream {@code RM.Anvil} + {@code RM.AnvilBendSmall|Big} handler templates of
 * Loader_Recipes_Handlers.java:157-205 (:157-172 the Shredding grinding rows, :175-205
 * the forging/welding rows, :208-214 the bending rows), the GT6RecipesShCL pour shape.
 *
 * <p><b>The upstream handler form</b> (RecipeMapHandlerPrefix, the 19-arg ctor over
 * :82/RecipeMapHandlerPrefixShredding :45): inputs 1-2 prefix slots, outputs 1-2 prefix
 * slots (+ the pulverized-remains tail, which every anvil row requests OFF — the 18th
 * ctor arg F), {@code mDuration = 0} on EVERY anvil row → the duration resolves per
 * material through {@code getCosts} (:225-227)
 * {@code units(max(unitsIn, unitsOut), U, multiplier + multiplier*mToolQuality, T)}
 * with the unit sums over BOTH input and BOTH output slots (:73-78). Output material:
 * the base handler emits SELF, the Shredding subclass hops to
 * {@code mTargetPulver.mMaterial} (RecipeMapHandlerPrefixShredding.java:46-48). Any
 * unresolvable input OR output drops the whole row (addRecipeForMaterial :209/:214).
 *
 * <p><b>The condition conjuncts</b> (transcribed as the template flag set, evaluated by
 * {@link #condition(OreDictMaterial, AnvilTemplate)}): {@code ANTIMATTER.NOT} +
 * {@code INVALID_MATERIAL} on every row (:205); {@code MORTAR} on the crushed/chunk
 * grinding rows (:158-172 — the manual-workability gate); {@code selfcrush()} on the
 * oreRaw row (:166, OreDictMaterialCondition.java:47-49 = {@code mTargetSmashing.mMaterial
 * == mat}); {@code FLAMMABLE.NOT + SMITHABLE + selfforge() + fullforge() + COATED.NOT}
 * on the forging/chance rows (:175-199 — selfforge :57-60 = the forging target is self,
 * fullforge :62-64 = the forging yield is a full unit); bare {@code SMITHABLE} on the
 * bend rows (:208-210/:214) plus the forge-purity conjuncts on the Small plate→foil row
 * (:213).
 *
 * <p><b>The two-maps-in-three fold</b> (GT6RecipeMaps.ANVIL_BEND field doc): the Small/
 * Big row sets pour into the ONE port bend map in upstream file order — :208-210 Big then
 * :213-214 Small — per-row chances and scrap byproducts verbatim. The exact-row dedup
 * (the W1-collision ruling via {@link GT6RecipesShCL#rowKey}) keeps the two
 * stick-start rows (:209 springSmall vs :214 ring) both alive: they differ in outputs.
 *
 * <p><b>Item-universe note</b>: the chunk/rubble/pebbles/clump/reduced/crystalline/
 * cleanGravel/cluster rows (:158-165) and the plateSteamcraft row (:192) carry prefixes
 * OUTSIDE the port item path (GTMaterialItems.itemPathPrefixes — the p26 Shredder
 * backfill census), so those templates expand to ZERO rows and name themselves in the
 * zero-expansion audit — neither poured nor skip-counted, lighting up automatically if
 * an item-universe card registers them.
 *
 * <p><b>Load timing</b>: the GT6RecipesShCL form — a self-contained MOD-bus listener
 * pouring at FMLCommonSetup.enqueueWork; the OP/MT-referencing tables build lazily on
 * first load (the a9027ac lesson); {@code load()} is idempotent per JVM generation and
 * the pour-flag joins the GT6RecipeMaps generation (ADR-P18).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesAnvil {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * One transcribed anvil handler template. {@code in2 != null} = the dual-input welding
	 * row (ingot+ingot→ingotDouble, Handlers:175-187); {@code outPrefixes.length == 2} =
	 * the secondary output row (the scrap byproducts, :191-199/:209/:213/:214); {@code
	 * chances != null} = the chance-bearing rows ({@code .chances(10000, 9000)}, carried
	 * through {@link Recipe#mChances}). {@code shredding} selects the
	 * {@code mTargetPulver} output hop; {@code isBend} routes the row to
	 * {@link GT6RecipeMaps#ANVIL_BEND} instead of {@link GT6RecipeMaps#ANVIL}.
	 */
	public record AnvilTemplate(String note, OreDictPrefix in1, int inCount1, @Nullable OreDictPrefix in2, int inCount2,
			OreDictPrefix[] outPrefixes, int[] outCounts, @Nullable long[] chances,
			long eUt, long multiplier, boolean shredding, boolean isBend,
			boolean useMortar, boolean useSelfcrush, boolean flammableNot, boolean smithable, boolean forgePurity, boolean coatedNot) {}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline (the GT6RecipesShCL form). */
	static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesAnvil::resolveItem;

	/**
	 * The RM.Anvil templates (Loader_Recipes_Handlers.java:157-205), upstream file order:
	 * the Shredding grinding rows :157-172, the welding ladders :175-187, the ring→chain
	 * row :188, the chance rows :191-199 and the gem chain :200-205.
	 *
	 * <p><b>Lazily built</b>: the {@code @EventBusSubscriber} annotation scan class-loads
	 * this class at MOD CONSTRUCTION — before {@code MT.init()} — so OP/MT references must
	 * not be captured in static initializers (the a9027ac lesson).
	 */
	private static volatile List<AnvilTemplate> sAnvilTemplates = null;

	/** The transcribed RM.Anvil templates, captured on first use (one material generation). */
	public static List<AnvilTemplate> anvilTable() {
		List<AnvilTemplate> tTable = sAnvilTemplates;
		if (tTable == null) sAnvilTemplates = tTable = List.of(
		// Loader_Recipes_Handlers.java:157 — rockGt, ANTIMATTER.NOT only (the sole non-MORTAR grinding row)
		shred(":157", OP.rockGt    , OP.dustSmall , 9, null , 0, false, false),
		// :158-165 — the MORTAR-gated grinding rows; chunk/rubble/pebbles/clump/reduced/
		// crystalline/cleanGravel/cluster have NO port items → zero expansion (the audit)
		shred(":158", OP.chunk     , OP.dust      , 2, OP.dustTiny, 1, true , false),
		shred(":159", OP.rubble    , OP.dust      , 2, OP.dustTiny, 1, true , false),
		shred(":160", OP.pebbles   , OP.dust      , 2, OP.dustTiny, 1, true , false),
		shred(":161", OP.clump     , OP.dust      , 1, null , 0, true , false),
		shred(":162", OP.reduced   , OP.dust      , 1, null , 0, true , false),
		shred(":163", OP.crystalline, OP.dust     , 1, null , 0, true , false),
		shred(":164", OP.cleanGravel, OP.dust     , 1, null , 0, true , false),
		shred(":165", OP.cluster   , OP.dust      , 3, null , 0, true , false),
		// :166 — oreRaw, MORTAR + selfcrush()
		shredSpecial(":166", OP.oreRaw, OP.crushed , 1, OP.crushedTiny, 6, true , true ),
		// :167-172 — the crushed family, MORTAR-gated
		shred(":167", OP.crushed                , OP.dust     , 1, OP.dustDiv72,  9, true , false),
		shred(":168", OP.crushedPurified        , OP.dust     , 1, OP.dustDiv72, 18, true , false),
		shred(":169", OP.crushedCentrifuged     , OP.dust     , 1, OP.dustDiv72, 27, true , false),
		shred(":170", OP.crushedTiny            , OP.dustDiv72, 9, null , 0, true , false),
		shred(":171", OP.crushedPurifiedTiny    , OP.dustDiv72, 10, null, 0, true , false),
		shred(":172", OP.crushedCentrifugedTiny , OP.dustDiv72, 11, null, 0, true , false),
		// :175-187 — the welding ladders (dual input), the full forge-purity conjunct set
		forge2(":175", OP.ingot        , 1, OP.ingot       , 1, OP.ingotDouble   , 1),
		forge2(":176", OP.ingot        , 1, OP.ingotDouble , 1, OP.ingotTriple   , 1),
		forge2(":177", OP.ingot        , 1, OP.ingotTriple , 1, OP.ingotQuadruple, 1),
		forge2(":178", OP.ingot        , 1, OP.ingotQuadruple, 1, OP.ingotQuintuple, 1),
		forge2(":179", OP.ingotDouble  , 1, OP.ingotDouble , 1, OP.ingotQuadruple, 1),
		forge2(":180", OP.ingotDouble  , 1, OP.ingotTriple , 1, OP.ingotQuintuple, 1),
		forge2(":181", OP.plate        , 1, OP.plate       , 1, OP.plateDouble   , 1),
		forge2(":182", OP.plate        , 1, OP.plateDouble , 1, OP.plateTriple   , 1),
		forge2(":183", OP.plate        , 1, OP.plateTriple , 1, OP.plateQuadruple, 1),
		forge2(":184", OP.plate        , 1, OP.plateQuadruple, 1, OP.plateQuintuple, 1),
		forge2(":185", OP.plateDouble  , 1, OP.plateDouble , 1, OP.plateQuadruple, 1),
		forge2(":186", OP.plateDouble  , 1, OP.plateTriple , 1, OP.plateQuintuple, 1),
		forge2(":187", OP.stick        , 1, OP.stick       , 1, OP.stickLong     , 1),
		// :188 — ring x2 → chain, ANTIMATTER.NOT only (no forge purity)
		forge1(":188", OP.ring, 2, null, 0, OP.chain, 1, null, false, false),
		// :191-199 — the chance rows (main 10000, scrap 9000); :192 plateSteamcraft has NO
		// port item → the walk skips every row (the mat() → null convention, the audit names it)
		forge1(":191", OP.chunkGt      , 1, OP.plateTiny   , 1, OP.scrapGt, 1, new long[] {10000, 9000}, true, true),
		forge1(":192", OP.ingot        , 1, OP.plateSteamcraft, 1, OP.scrapGt, 3, new long[] {10000, 9000}, true, true),
		forge1(":193", OP.ingotDouble  , 1, OP.plate       , 1, OP.scrapGt, 9, new long[] {10000, 9000}, true, true),
		forge1(":194", OP.ingotTriple  , 1, OP.plateDouble , 1, OP.scrapGt, 9, new long[] {10000, 9000}, true, true),
		forge1(":195", OP.ingotQuadruple, 1, OP.plateTriple, 1, OP.scrapGt, 9, new long[] {10000, 9000}, true, true),
		forge1(":196", OP.ingotQuintuple, 1, OP.plateQuadruple, 1, OP.scrapGt, 9, new long[] {10000, 9000}, true, true),
		forge1(":197", OP.plate        , 1, OP.casingSmall , 1, OP.scrapGt, 4, new long[] {10000, 9000}, true, true),
		forge1(":198", OP.plateCurved  , 1, OP.plate       , 1, null, 0, null, true, true),
		forge1(":199", OP.casingSmall  , 1, OP.railGt      , 2, null, 0, null, true, true),
		// :200-205 — the gem chain, ANTIMATTER.NOT only
		forge1(":200", OP.gemLegendary , 1, OP.gemExquisite, 2, null, 0, null, false, false),
		forge1(":201", OP.gemExquisite , 1, OP.gemFlawless , 2, null, 0, null, false, false),
		forge1(":202", OP.gemFlawless  , 1, OP.gem         , 2, null, 0, null, false, false),
		forge1(":203", OP.gem          , 1, OP.gemFlawed   , 2, null, 0, null, false, false),
		forge1(":204", OP.gemFlawed    , 1, OP.gemChipped  , 2, null, 0, null, false, false),
		forge1(":205", OP.gemChipped   , 1, OP.dustSmall   , 1, null, 0, null, false, false));
		return tTable;
	}

	/**
	 * The bend templates (Handlers:208-210 Big then :213-214 Small — the union pour into
	 * the ONE port bend map, the GT6RecipeMaps.ANVIL_BEND field doc). {@code chances}
	 * rows carry the scrap byproducts at the upstream 9000/10000.
	 */
	private static volatile List<AnvilTemplate> sBendTemplates = null;

	/** The transcribed bend templates, captured on first use (one material generation). */
	public static List<AnvilTemplate> bendTable() {
		List<AnvilTemplate> tTable = sBendTemplates;
		if (tTable == null) sBendTemplates = tTable = List.of(
		// Loader_Recipes_Handlers.java:208-210 — RM.AnvilBendBig, And(ANTIMATTER.NOT, SMITHABLE)
		forge1(":208", OP.plate   , 1, OP.plateCurved, 1, null      , 0, null                  , false, true ),
		forge1(":209", OP.stick   , 1, OP.springSmall, 1, OP.scrapGt, 2, new long[] {10000, 9000}, false, true ),
		forge1(":210", OP.stickLong, 1, OP.spring    , 1, null      , 0, null                  , false, true ),
		// :213 — RM.AnvilBendSmall plate→foil, SMITHABLE + the forge-purity conjuncts
		forge1Purity(":213", OP.plate, 1, OP.foil, 2, OP.scrapGt, 4, new long[] {10000, 9000}),
		// :214 — RM.AnvilBendSmall stick→ring, And(ANTIMATTER.NOT, SMITHABLE)
		forge1(":214", OP.stick, 1, OP.ring, 1, OP.scrapGt, 2, new long[] {10000, 9000}, false, true ));
		return tTable;
	}

	/** Shred-table builder: the RecipeMapHandlerPrefixShredding rows (:157-172). */
	private static AnvilTemplate shred(String aNote, OreDictPrefix aIn, OreDictPrefix aOut1, int aCount1, @Nullable OreDictPrefix aOut2, int aCount2, boolean aMortar, boolean aSelfcrush) {
		return new AnvilTemplate(aNote, aIn, 1, null, 0,
				aOut2 == null ? new OreDictPrefix[] {aOut1} : new OreDictPrefix[] {aOut1, aOut2},
				aOut2 == null ? new int[] {aCount1} : new int[] {aCount1, aCount2},
				null, 16, 16, true, false, aMortar, aSelfcrush, false, false, false, false);
	}

	/** The :166 special form — MORTAR + selfcrush(). */
	private static AnvilTemplate shredSpecial(String aNote, OreDictPrefix aIn, OreDictPrefix aOut1, int aCount1, OreDictPrefix aOut2, int aCount2, boolean aMortar, boolean aSelfcrush) {
		return new AnvilTemplate(aNote, aIn, 1, null, 0, new OreDictPrefix[] {aOut1, aOut2}, new int[] {aCount1, aCount2},
				null, 16, 16, true, false, aMortar, aSelfcrush, false, false, false, false);
	}

	/** Dual-input welding row (:175-187): full forge-purity conjunct set. */
	private static AnvilTemplate forge2(String aNote, OreDictPrefix aIn1, int aCount1, OreDictPrefix aIn2, int aCount2, OreDictPrefix aOut, int aOutCount) {
		return new AnvilTemplate(aNote, aIn1, aCount1, aIn2, aCount2, new OreDictPrefix[] {aOut}, new int[] {aOutCount},
				null, 64, 64, false, false, false, false, true, true, true, true);
	}

	/**
	 * Single-input row: {@code aFlammableNot + aForgePurity} select the condition arm —
	 * the :191-199 chance rows carry the full set, the :188/:200-205 rows are bare.
	 */
	private static AnvilTemplate forge1(String aNote, OreDictPrefix aIn, int aInCount, OreDictPrefix aOut1, int aCount1, @Nullable OreDictPrefix aOut2, int aCount2, @Nullable long[] aChances, boolean aForgePurityArm, boolean aBend) {
		return new AnvilTemplate(aNote, aIn, aInCount, null, 0,
				aOut2 == null ? new OreDictPrefix[] {aOut1} : new OreDictPrefix[] {aOut1, aOut2},
				aOut2 == null ? new int[] {aCount1} : new int[] {aCount1, aCount2},
				aChances, 64, 64, false, aBend, false, false, aForgePurityArm, aForgePurityArm || aBend, aForgePurityArm, aForgePurityArm);
	}

	/** The :213 form — bend + forge purity (SMITHABLE + selfforge + fullforge + COATED.NOT). */
	private static AnvilTemplate forge1Purity(String aNote, OreDictPrefix aIn, int aInCount, OreDictPrefix aOut1, int aCount1, OreDictPrefix aOut2, int aCount2, long[] aChances) {
		return new AnvilTemplate(aNote, aIn, aInCount, null, 0, new OreDictPrefix[] {aOut1, aOut2}, new int[] {aCount1, aCount2},
				aChances, 64, 64, false, true, false, false, true, true, true, true);
	}

	/**
	 * The upstream condition conjuncts as a boolean gate — ANTIMATTER.NOT + INVALID_MATERIAL
	 * on every row (:205), then the per-row conjunct set (the class doc table).
	 */
	static boolean condition(OreDictMaterial aMaterial, AnvilTemplate aTemplate) {
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Properties.INVALID_MATERIAL)) return false;
		if (aTemplate.useMortar() && !aMaterial.contains(TD.Processing.MORTAR)) return false;
		if (aTemplate.useSelfcrush() && aMaterial.mTargetSmashing.mMaterial != aMaterial) return false; // selfcrush() :47-49
		if (aTemplate.flammableNot() && aMaterial.contains(TD.Properties.FLAMMABLE)) return false;
		if (aTemplate.smithable() && !aMaterial.contains(TD.Processing.SMITHABLE)) return false;
		if (aTemplate.forgePurity() && (aMaterial.mTargetForging.mMaterial != aMaterial // selfforge() :57-60
				|| aMaterial.mTargetForging.mAmount < CS.U)) return false; // fullforge() :62-64
		if (aTemplate.coatedNot() && aMaterial.contains(TD.Compounds.COATED)) return false;
		return true;
	}

	/**
	 * Template x material → Recipe, or null (the upstream addRecipeForMaterial false
	 * return): the {@link #condition} gate, both-input resolution (:209), the
	 * {@code mTargetPulver} hop for the Shredding rows (RecipeMapHandlerPrefixShredding
	 * :46-48) / SELF for the handler rows, the all-outputs resolution (:214), and the
	 * mDuration=0 → max(1, getCosts) split (:225-227 over the summed unit columns :73-78).
	 */
	static Recipe buildRecipe(AnvilTemplate aTemplate, OreDictMaterial aMaterial) {
		if (!condition(aMaterial, aTemplate)) return null;
		Item tIn1 = sMaterialItemResolver.apply(aTemplate.in1(), aMaterial);
		if (tIn1 == null) return null; // upstream :209 mat() → null
		ItemStack[] tInputs = new ItemStack[aTemplate.in2() == null ? 1 : 2];
		tInputs[0] = new ItemStack(tIn1, aTemplate.inCount1());
		if (aTemplate.in2() != null) {
			Item tIn2 = sMaterialItemResolver.apply(aTemplate.in2(), aMaterial);
			if (tIn2 == null) return null; // upstream :209 — the second input slot drops the row too
			tInputs[1] = new ItemStack(tIn2, aTemplate.inCount2());
		}
		OreDictMaterial tOutMaterial = aTemplate.shredding() ? aMaterial.mTargetPulver.mMaterial : aMaterial;
		ItemStack[] tOutputs = new ItemStack[aTemplate.outPrefixes().length];
		long tUnitsIn = aTemplate.in1().mAmount * aTemplate.inCount1()
				+ (aTemplate.in2() == null ? 0 : aTemplate.in2().mAmount * aTemplate.inCount2());
		long tUnitsOut = 0;
		for (int i = 0; i < tOutputs.length; i++) {
			Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefixes()[i], tOutMaterial);
			if (tOutItem == null) return null; // upstream :214 — any invalid output drops the row
			tOutputs[i] = new ItemStack(tOutItem, aTemplate.outCounts()[i]);
			tUnitsOut += aTemplate.outPrefixes()[i].mAmount * aTemplate.outCounts()[i];
		}
		long tDuration = Math.max(1, GT6RecipesShCL.handlerCosts(tUnitsIn, tUnitsOut, aTemplate.multiplier(), aMaterial)); // :225-227
		return new Recipe(true, tInputs, tOutputs, new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0, aTemplate.chances());
	}

	/**
	 * The pour: every template x its registered materials (the first input prefix's
	 * registration order, the port's single enumeration source), with the exact-row dedup
	 * and the zero-expansion audit (the GT6RecipesShCL pourLathe/pourShredderTemplates form).
	 */
	private static void pour(RecipeMap aMap, String aLabel, List<AnvilTemplate> aTemplates) {
		Set<String> tSeen = new HashSet<>();
		for (Recipe tRecipe : aMap.mRecipeList) tSeen.add(GT6RecipesShCL.rowKey(tRecipe));
		int tPoured = 0, tSkipped = 0, tDeduped = 0;
		List<String> tZeroExpansion = new ArrayList<>();
		for (AnvilTemplate tTemplate : aTemplates) {
			List<OreDictMaterial> tMaterials = GT6RecipesShCL.expandCrusherMaterials(tTemplate.in1());
			if (tMaterials.isEmpty()) {tZeroExpansion.add(tTemplate.note() + " (" + tTemplate.in1().mNameInternal + ")"); continue;}
			for (OreDictMaterial tMaterial : tMaterials) {
				Recipe tRecipe = buildRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;} // the condition gates + the unresolvable items
				if (!tSeen.add(GT6RecipesShCL.rowKey(tRecipe))) {tDeduped++; continue;} // exact-row collision
				aMap.addRecipe(tRecipe);
				tPoured++;
			}
		}
		if (!tZeroExpansion.isEmpty()) LOGGER.info("GT6 {} templates with ZERO material expansion (the input prefix has no registered port items — they light up when an item-universe card registers them): {}", aLabel, tZeroExpansion);
		LOGGER.info("GT6 {} poured: {} loaded, {} skipped (condition gates + unresolvable items), {} deduped (exact rows already present)", aLabel, tPoured, tSkipped, tDeduped);
	}

	/** Poured flag — one generation, one pour (the GT6RecipesShCL form). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the poison-state fix).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesAnvil::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (the GT6RecipesShCL form). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesAnvil::load);
	}

	/** Pours the tables into the ANVIL/ANVIL_BEND maps. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent (the GT6RecipesShCL.load form)
		if (GT6RecipeMaps.ANVIL == null || GT6RecipeMaps.ANVIL_BEND == null) return; // reset() between init and load
		pour(GT6RecipeMaps.ANVIL, "Anvil", anvilTable());
		pour(GT6RecipeMaps.ANVIL_BEND, "Anvil bend", bendTable());
		sLoaded = true;
	}

	/** The live item lookup (the GT6RecipesShCL.resolveItem form) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured tables so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sAnvilTemplates = null;
		sBendTemplates = null;
	}

	private GT6RecipesAnvil() {}
}
