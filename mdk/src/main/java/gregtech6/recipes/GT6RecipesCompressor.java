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

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Compressor recipe book (task p26-w1-sifter-compressor-wiremill) — the row audit of
 * the RM.Compressor pour surface. Upstream feeders outside compat/ (the P10 59-class
 * ruling cuts the whole Compat_* tree):
 *
 * <ul>
 *   <li><b>The plate/dense walk</b> — Loader_Recipes_Handlers.java:217-233, two
 *       complementary condition arms (hard arm mDuration 0 / mMultiplier 256 through
 *       getCosts; easy arm fixed durations 16 / 144 / 144 / 144 / 144 / 144 / 16 / 10):
 *       {@code dust→plateGem} (the :217/:226 {@code Nor(gemLegendary, gemExquisite,
 *       gemFlawless, bouleGt, MT.Ice, ANTIMATTER, LAYERED, COATED, tEasyWorkable[.NOT])}
 *       gate), {@code compressed 9→plateDense}, {@code plate 9→plateDense},
 *       {@code plateTriple 3→plateDense}, {@code blockPlate→plateDense},
 *       {@code blockSolid→plateDense}, {@code ingot→compressed}, {@code billet→
 *       plateSteamcraft} (the :218-223/:227-233 base gate
 *       {@code And(ANTIMATTER.NOT, COATED.NOT, tEasyWorkable[.NOT])} — note NO SMITHABLE
 *       leg, unlike the Wiremill arm); tEasyWorkable = {@code Or(FURNACE, SOFT)}
 *       (Loader_Recipes_Handlers.java:42).</li>
 *   <li><b>The vanilla fixed rows</b> — Loader_Recipes_Vanilla.java:644-658 + :709-711
 *       (the ice/snow/quartz/sand/lapis-family/blaze set; :709-711 take the MT.Blaze
 *       representative of the ANY.Blaze.mToThis loop, the p7 group-expansion ruling),
 *       MultiItemFood.java:173 (clay dust → clay ball), Loader_Recipes_Other.java:129
 *       (dust Refined Obsidian → plate), Loader_Recipes_Food.java:57 (the 3-prefix x
 *       8-material meat loop, :56 fixed prefix/material lists).</li>
 * </ul>
 *
 * <p><b>Skipped upstream rows (the pool, not silent — declared in {@link #SKIPPED_UPSTREAM})</b>:
 * the {@code compressed}/{@code blockPlate}/{@code blockSolid}/{@code billet→
 * plateSteamcraft} templates resolve to ZERO pours (the port item universe carries none of
 * those prefixes on the MaterialPrefixItem path), the IC2/HBM plantball rows
 * (Handlers:236-244 + all the Loader_Recipes_Crops event rows — foreign-mod outputs), the
 * wax group walk (Loader_Recipes_Other:47-48, the ANY.Wax group + the non-GT registration
 * listener semantics, pooled), and all compat.
 *
 * <p><b>Load timing</b>: the GT6RecipesWiremill/ShCL form — self-contained MOD-bus
 * listener at FMLCommonSetup.enqueueWork, lazily built tables, generation-tracked pour flag.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesCompressor {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * One transcribed Compressor prefix template (the 15-arg RecipeMapHandlerPrefix form,
	 * the GT6RecipesWiremill.WireTemplate shape). {@code gemGate} marks the :217/:226 dust
	 * row whose condition is the Nor gem-chain gate (which embeds the workability split)
	 * instead of the plain base gate.
	 */
	public record CompressTemplate(String note, OreDictPrefix inPrefix, int inCount, OreDictPrefix outPrefix, int outCount,
			long eUt, long duration, long multiplier, boolean easyArm, boolean gemGate) {}

	/**
	 * One transcribed fixed row (the GT6RecipesShCL.FixedRow shape): a resolved input
	 * slot, fixed eUt/duration, optional chances, the outputs. {@code note} carries the
	 * upstream line number.
	 */
	public record FixedRow(String note, Slot input, long eUt, long duration, @Nullable long[] chances, Slot... outputs) {
		/** The compact deterministic form (null chances) — most fixed rows are chance-free. */
		public FixedRow(String note, Slot input, long eUt, long duration, Slot... outputs) {
			this(note, input, eUt, duration, null, outputs);
		}
	}

	/** One input/output slot of a fixed row: a vanilla item reference or a (prefix, material) pair (the ShCL record form). */
	public record Slot(@Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int count) {
		public static Slot vanilla(Supplier<Item> aItem, int aCount) { return new Slot(aItem, null, null, aCount); }
		public static Slot material(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) { return new Slot(null, aPrefix, aMaterial, aCount); }
	}

	/** The resolution seam: the live registry lookups by default, fixtures injected offline (the ShCL precedent). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesCompressor::resolveItem;
	/** The vanilla item seam (only for offline determinism; live it just dereferences the supplier). */
	static Function<Supplier<Item>, Item> sVanillaItemResolver = Supplier::get;

	/**
	 * The transcribed plate/dense templates (Loader_Recipes_Handlers.java:217-233), hard
	 * arm first in upstream order. <b>Lazily built</b> — the {@code @EventBusSubscriber}
	 * scan class-loads at MOD CONSTRUCTION, before MT.init() (the a9027ac lesson).
	 */
	private static volatile List<CompressTemplate> sTemplates = null;

	/** The transcribed templates, captured on first use (one material generation). */
	public static List<CompressTemplate> table() {
		List<CompressTemplate> tTable = sTemplates;
		if (tTable == null) sTemplates = tTable = List.of(
		// :217-224 — the hard arm, duration computed at multiplier 256 (:217 carries the gem Nor gate)
		new CompressTemplate(":217", OP.dust        , 1, OP.plateGem       , 1, 16, 0     , 256, false, true ),
		new CompressTemplate(":218", OP.compressed  , 9, OP.plateDense     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":219", OP.plate       , 9, OP.plateDense     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":220", OP.plateTriple, 3, OP.plateDense     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":221", OP.blockPlate  , 1, OP.plateDense     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":222", OP.blockSolid , 1, OP.plateDense     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":223", OP.ingot       , 1, OP.compressed     , 1, 16, 0     , 256, false, false),
		new CompressTemplate(":224", OP.billet      , 1, OP.plateSteamcraft, 1, 16, 0     , 256, false, false),
		// :226-233 — the easy arm, fixed durations (:226 carries the inverted gem Nor gate)
		new CompressTemplate(":226", OP.dust        , 1, OP.plateGem       , 1, 16, 16    , 0, true , true ),
		new CompressTemplate(":227", OP.compressed  , 9, OP.plateDense     , 1, 16, 16* 9, 0, true , false),
		new CompressTemplate(":228", OP.plate       , 9, OP.plateDense     , 1, 16, 16* 9, 0, true , false),
		new CompressTemplate(":229", OP.plateTriple, 3, OP.plateDense     , 1, 16, 16* 9, 0, true , false),
		new CompressTemplate(":230", OP.blockPlate  , 1, OP.plateDense     , 1, 16, 16* 9, 0, true , false),
		new CompressTemplate(":231", OP.blockSolid  , 1, OP.plateDense     , 1, 16, 16* 9, 0, true , false),
		new CompressTemplate(":232", OP.ingot       , 1, OP.compressed     , 1, 16, 16    , 0, true , false),
		new CompressTemplate(":233", OP.billet      , 1, OP.plateSteamcraft, 1, 16, 32/ 3, 0, true , false));
		return tTable;
	}

	/**
	 * The transcribed fixed rows (Loader_Recipes_Vanilla.java:644-658 + :709-711,
	 * MultiItemFood.java:173, Loader_Recipes_Other.java:129, Loader_Recipes_Food.java:56-57),
	 * upstream file order. The :709-711 blaze rows take the MT.Blaze representative of the
	 * ANY.Blaze group (the p7 group-expansion ruling). The Food:57 loop's eight materials
	 * ride the :56 fixed lists (MeatRotten/MeatRaw/MeatCooked/FishRotten/FishRaw/
	 * FishCooked/SoylentGreen/Tofu x dustTiny→nugget, dustSmall→chunkGt, dust→ingot).
	 */
	private static volatile List<FixedRow> sFixedRows = null;

	/** The transcribed fixed rows, captured on first use (one material generation). */
	public static List<FixedRow> fixedTable() {
		List<FixedRow> tTable = sFixedRows;
		if (tTable == null) {
			List<FixedRow> tRows = new ArrayList<>();
			// Loader_Recipes_Vanilla.java:644-658
			tRows.add(new FixedRow(":644", Slot.vanilla(() -> Blocks.ICE.asItem(), 2), 64, 32, Slot.vanilla(() -> Blocks.PACKED_ICE.asItem(), 1)));
			tRows.add(new FixedRow(":645", Slot.material(OP.dust, MT.Ice, 1), 16, 32, Slot.vanilla(() -> Blocks.ICE.asItem(), 1)));
			// :646 — OM.dust(MT.Ice, U4): the sub-unit OM dust lands on the U/4 dust prefix
			// (gemChipped == U/4 per the :647 4:1 ice-block ratio), so the item-count axis
			// carries it as one dustSmall Ice
			tRows.add(new FixedRow(":646", Slot.material(OP.dustSmall, MT.Ice, 1), 16, 16, Slot.material(OP.gemChipped, MT.Ice, 1)));
			tRows.add(new FixedRow(":647", Slot.material(OP.gemChipped, MT.Ice, 4), 16, 16, Slot.vanilla(() -> Blocks.ICE.asItem(), 1)));
			tRows.add(new FixedRow(":648", Slot.material(OP.gemFlawed, MT.Ice, 2), 16, 16, Slot.vanilla(() -> Blocks.ICE.asItem(), 1)));
			tRows.add(new FixedRow(":649", Slot.material(OP.gem, MT.Ice, 1), 16, 16, Slot.vanilla(() -> Blocks.ICE.asItem(), 1)));
			tRows.add(new FixedRow(":650", Slot.vanilla(() -> Blocks.SNOW.asItem(), 1), 16, 32, Slot.vanilla(() -> Blocks.ICE.asItem(), 1)));
			tRows.add(new FixedRow(":651", Slot.vanilla(() -> Items.SNOWBALL, 4), 16, 32, Slot.vanilla(() -> Blocks.SNOW.asItem(), 1)));
			tRows.add(new FixedRow(":652", Slot.vanilla(() -> Items.QUARTZ, 4), 16, 16, Slot.vanilla(() -> Blocks.QUARTZ_BLOCK.asItem(), 1)));
			tRows.add(new FixedRow(":654", Slot.vanilla(() -> Blocks.SAND.asItem(), 4), 16, 32, Slot.vanilla(() -> Blocks.SANDSTONE.asItem(), 1)));
			tRows.add(new FixedRow(":655", Slot.material(OP.dust, MT.Lapis, 1), 16, 32, Slot.material(OP.plateGem, MT.Lapis, 1)));
			tRows.add(new FixedRow(":656", Slot.material(OP.dust, MT.Asbestos, 1), 16, 32, Slot.material(OP.plate, MT.Asbestos, 1)));
			tRows.add(new FixedRow(":657", Slot.material(OP.dust, MT.Lazurite, 1), 16, 32, Slot.material(OP.plateGem, MT.Lazurite, 1)));
			tRows.add(new FixedRow(":658", Slot.material(OP.dust, MT.Sodalite, 1), 16, 32, Slot.material(OP.plateGem, MT.Sodalite, 1)));
			// Loader_Recipes_Vanilla.java:709-711 — the MT.Blaze representative of ANY.Blaze.mToThis
			tRows.add(new FixedRow(":709", Slot.material(OP.dust     , MT.Blaze, 1), 16, 32, Slot.material(OP.plate, MT.Blaze, 1)));
			tRows.add(new FixedRow(":710", Slot.material(OP.dustSmall, MT.Blaze, 4), 16, 32, Slot.material(OP.plate, MT.Blaze, 1)));
			tRows.add(new FixedRow(":711", Slot.material(OP.dustTiny , MT.Blaze, 9), 16, 32, Slot.material(OP.plate, MT.Blaze, 1)));
			// MultiItemFood.java:173 — :174-175 are the IL.Clay_Ball_Brown/Red foreign IL rows (SKIPPED_UPSTREAM)
			tRows.add(new FixedRow("Food:173", Slot.material(OP.dust, MT.Clay, 1), 16, 16, Slot.vanilla(() -> Items.CLAY_BALL, 1)));
			// Loader_Recipes_Other.java:129
			tRows.add(new FixedRow("Other:129", Slot.material(OP.dust, MT.RefinedObsidian, 1), 16, 256, Slot.material(OP.plate, MT.RefinedObsidian, 1)));
			// Loader_Recipes_Food.java:57 — the fixed :56 material loop, dustTiny/dustSmall/dust → nugget/chunkGt/ingot
			OreDictPrefix[] tPrefixListA = new OreDictPrefix[] {OP.dustTiny, OP.dustSmall, OP.dust};
			OreDictPrefix[] tPrefixListB = new OreDictPrefix[] {OP.nugget, OP.chunkGt, OP.ingot};
			OreDictMaterial[] tMaterialList = new OreDictMaterial[] {MT.MeatRotten, MT.MeatRaw, MT.MeatCooked, MT.FishRotten, MT.FishRaw, MT.FishCooked, MT.SoylentGreen, MT.Tofu};
			for (int i = 0; i < tPrefixListA.length; i++) for (int j = 0; j < tMaterialList.length; j++) {
				tRows.add(new FixedRow("Food:57", Slot.material(tPrefixListA[i], tMaterialList[j], 1), 16, 16,
						Slot.material(tPrefixListB[i], tMaterialList[j], 1)));
			}
			sFixedRows = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Handlers.java:218/:221/:222/:224/:227/:230/:231/:233 compressed|blockPlate|blockSolid→plateDense and billet→plateSteamcraft templates pour ZERO rows: the port item universe has none of those prefixes on the MaterialPrefixItem path (GTMaterialItems.itemPathPrefixes) — the plateDense walk pours through its plate/plateTriple arms, the compressed/plateSteamcraft arms unlock with those item families (pool)",
			"Loader_Recipes_Handlers.java:236-244 plantGt* → IL.IC2_Plantball / IL.HBM_Biomass rows + every Loader_Recipes_Crops.java RM.Compressor row (18 sites, ST.amount(1/8, aEvent.mStack) event feeds) — foreign-mod outputs (IC2/HBM), the P10 ruling",
			"Loader_Recipes_Other.java:47-48 the ANY.Wax.mToThis wax-dust → plate/foil walk — the wax group expansion + the non-GT registration listener semantics ride the cover/tools pool",
			"MultiItemFood.java:174-175 dust ClayBrown/ClayRed → IL.Clay_Ball_Brown/Red — foreign IL items, no port identity",
			"MultiItemRandomTools.java:417 IL.Pellet_Wood x2 → planks row — IL item, no port identity",
			"all Compat_Recipes_* RM.Compressor feeders — the P10 ruling (59 compat classes not ported)");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the GT6RecipesShCL form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesCompressor::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesCompressor::load);
	}

	/** Pours the tables into the COMPRESSOR map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines)
		if (GT6RecipeMaps.COMPRESSOR == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (CompressTemplate tTemplate : table()) {
			for (OreDictMaterial tMaterial : expandMaterials(tTemplate.inPrefix())) {
				Recipe tRecipe = buildTemplateRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;} // the condition arm or a mat() → null side
				GT6RecipeMaps.COMPRESSOR.addRecipe(tRecipe);
				tPoured++;
			}
		}
		int tFixedPoured = 0, tFixedSkipped = 0;
		for (FixedRow tRow : fixedTable()) {
			Recipe tRecipe = buildFixedRecipe(tRow);
			if (tRecipe == null) {tFixedSkipped++; continue;} // = upstream mat() → null silent drop
			GT6RecipeMaps.COMPRESSOR.addRecipe(tRecipe);
			tFixedPoured++;
		}
		LOGGER.info("GT6 Compressor recipes poured: {} template rows + {} fixed rows loaded, {} + {} skipped (condition-arm materials, unresolvable prefix items and the declared zero-pour arms = upstream addRecipeForMaterial false / mat() null returns)", tPoured, tFixedPoured, tSkipped, tFixedSkipped);
		sLoaded = true;
	}

	/**
	 * The port counterpart of RecipeMapHandlerPrefix.addAllRecipesInternal (:173) — the
	 * registered materials of the prefix in registration order (the ShCL form).
	 */
	public static List<OreDictMaterial> expandMaterials(OreDictPrefix aInPrefix) {
		List<OreDictMaterial> rMaterials = new ArrayList<>();
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == aInPrefix) rMaterials.add(tPair.material());
		}
		return rMaterials;
	}

	/**
	 * Template x material → Recipe, or null (the upstream addRecipeForMaterial false
	 * return). The two condition forms: the gem-gate dust rows (:217/:226) ride
	 * {@code Nor(gemLegendary, gemExquisite, gemFlawless, bouleGt, MT.Ice, ANTIMATTER,
	 * LAYERED, COATED, tEasyWorkable[.NOT])}; every other row the base
	 * {@code And(ANTIMATTER.NOT, COATED.NOT, tEasyWorkable[.NOT])} — note NO SMITHABLE leg
	 * (the Wiremill arm difference). tEasyWorkable = Or(FURNACE, SOFT)
	 * (Loader_Recipes_Handlers.java:42). Duration: fixed when mDuration &gt; 0 (:218),
	 * else getCosts(:225-227) at the row multiplier.
	 */
	static Recipe buildTemplateRecipe(CompressTemplate aTemplate, OreDictMaterial aMaterial) {
		boolean tEasyWorkable = aMaterial.contains(TD.Processing.FURNACE) || aMaterial.contains(TD.Properties.SOFT);
		if (tEasyWorkable != aTemplate.easyArm()) return null; // the complementary condition arms
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Compounds.COATED)) return null; // the base :205 gate
		if (aTemplate.gemGate()) {
			// :217/:226 — Nor(gemLegendary, gemExquisite, gemFlawless, bouleGt, MT.Ice, ANTIMATTER, LAYERED, COATED, tEasyWorkable[.NOT])
			// the prefix set membership rides the registration walk itself: only materials that
			// generated a gemLegendary/gemExquisite/gemFlawless/bouleGt item are gem-chain members
			if (aMaterial == MT.Ice || aMaterial.contains(TD.Compounds.LAYERED)) return null;
			if (hasGemChainItem(aMaterial)) return null;
		}
		Item tInItem = sMaterialItemResolver.apply(aTemplate.inPrefix(), aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefix(), aMaterial);
		if (tOutItem == null) return null; // upstream :214 mat() → null
		long tDuration = aTemplate.duration() > 0 ? aTemplate.duration()
				: Math.max(1, costs(aTemplate.inPrefix(), aTemplate.inCount(), aTemplate.outPrefix(), aTemplate.outCount(), aTemplate.multiplier(), aMaterial));
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInItem, aTemplate.inCount())},
				new ItemStack[] {new ItemStack(tOutItem, aTemplate.outCount())},
				new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0);
	}

	/**
	 * The gem-chain membership probe behind the :217 Nor gate: upstream lists four gem
	 * prefixes by NAME (any material registered on gemLegendary/gemExquisite/gemFlawless/
	 * bouleGt is excluded from the dust→plateGem row). The port asks the same thing of the
	 * registration walk — a material with a generated item on any of the four prefixes is
	 * a gem-chain member.
	 */
	private static boolean hasGemChainItem(OreDictMaterial aMaterial) {
		for (OreDictPrefix tPrefix : GEM_CHAIN_PREFIXES) {
			if (GTMaterialItems.get(tPrefix, aMaterial) != null) return true;
		}
		return false;
	}

	private static final OreDictPrefix[] GEM_CHAIN_PREFIXES = {OP.gemLegendary, OP.gemExquisite, OP.gemFlawless, OP.bouleGt};

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

	/**
	 * Upstream RecipeMapHandlerPrefix.getCosts (:225-227) — the GT6RecipesShCL.crusherCosts
	 * transcription.
	 */
	static long costs(OreDictPrefix aInPrefix, int aInCount, OreDictPrefix aOutPrefix, int aOutCount, long aMultiplier, OreDictMaterial aMaterial) {
		long tUnitsIn = aInPrefix.mAmount * aInCount;
		long tUnitsOut = aOutPrefix.mAmount * aOutCount;
		long tAmount = Math.max(tUnitsIn, tUnitsOut);
		long tTarget = aMultiplier + aMultiplier * aMaterial.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / gregapi.data.CS.U + ((tAmount * tTarget) % gregapi.data.CS.U > 0 ? 1 : 0));
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
		sTemplates = null;
		sFixedRows = null;
	}
}
