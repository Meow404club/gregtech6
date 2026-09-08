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

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The Wiremill recipe book (task p26-w1-sifter-compressor-wiremill) — the row audit of the
 * RM.Wiremill pour surface. Upstream the map is fed ONLY by the {@code RecipeMapHandlerPrefix}
 * templates of Loader_Recipes_Handlers.java:287-295 (the class-load grep found no fixed
 * {@code addRecipe1} rows outside the compat/ tree, which the P10 ruling does not port):
 *
 * <ul>
 *   <li>:287-290 — the HARD arm: {@code (stick|stickLong|ingot|compressed) → wireFine 4/8 |
 *       wireGt01 2/2}, eUt 16, mDuration 0 (computed by getCosts at multiplier 128), condition
 *       {@code And(ANTIMATTER.NOT, COATED.NOT, SMITHABLE, tEasyWorkable.NOT)} — tEasyWorkable =
 *       {@code Or(FURNACE, SOFT)} (Loader_Recipes_Handlers.java:42);</li>
 *   <li>:292-295 — the EASY arm: the same four prefix pairs with FIXED durations 8/16/16/16
 *       (mMultiplier 0, mDuration &gt; 0 short-circuits getCosts — RecipeMapHandlerPrefix
 *       :218), condition the same And over plain {@code tEasyWorkable}.</li>
 * </ul>
 *
 * <p>The two arms are complementary halves of one workability split, so the port walks the
 * registration order once per template and routes each material to its arm — the same
 * expansion shape as the p7 {@link GT6RecipesShCL} crusher transcription (the port
 * counterpart of {@code RecipeMapHandlerPrefix.addAllRecipesInternal :173}). The duration
 * formula is the :225-227 getCosts transcription already proven in
 * {@link GT6RecipesShCL#crusherCosts}: {@code units(max(unitsIn, unitsOut), U, mMultiplier +
 * mMultiplier*mToolQuality, T)} with round-up.
 *
 * <p><b>Skipped upstream rows (the pool, not silent — declared in {@link #SKIPPED_UPSTREAM})</b>:
 * the {@code ingot→wireGt01} and {@code compressed→wireGt01} templates resolve to ZERO
 * pours because the port item universe carries no wireGt01 MaterialPrefixItems (the wire
 * family lives in the GTWires block/item domain, GTMaterialItems.itemPathPrefixes has no
 * OP.wireGt01) — the templates stay transcribed as DATA and the pour log reports the skips;
 * all Compat_* feeders (the P10 59-class ruling).
 *
 * <p><b>Load timing</b> (the GT6RecipesShCL precedent + the a9027ac lesson): a
 * self-contained MOD-bus listener pouring at FMLCommonSetup.enqueueWork; the
 * OP/MT-referencing tables are built lazily by {@code table()} on first load, never in
 * static initializers. {@code load()} is idempotent per JVM generation and the pour flag
 * joins the {@link GT6RecipeMaps} generation reset.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesWiremill {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * One transcribed Wiremill prefix template (the 15-arg
	 * {@code RecipeMapHandlerPrefix(inPrefix, inCount, NF, eUt, mDuration, mMultiplier, NF,
	 * outPrefix, outCount, ...)} form). {@code easyArm} selects the condition half:
	 * {@code mDuration > 0} rows are the fixed-duration easy arm (:292-295), the
	 * {@code mDuration == 0} rows the getCosts(128) hard arm (:287-290). {@code note}
	 * carries the upstream line number.
	 */
	public record WireTemplate(String note, OreDictPrefix inPrefix, OreDictPrefix outPrefix, int outCount,
			long eUt, long duration, long multiplier, boolean easyArm) {}

	/**
	 * The resolution seam: the live registry lookups by default, fixtures injected offline
	 * (the GT6RecipesShCL precedent).
	 */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesWiremill::resolveItem;

	/**
	 * The eight transcribed templates (Loader_Recipes_Handlers.java:287-295), hard arm
	 * first in upstream order. <b>Lazily built</b> — the {@code @EventBusSubscriber} scan
	 * class-loads at MOD CONSTRUCTION, before MT.init() (the a9027ac lesson).
	 */
	private static volatile List<WireTemplate> sTemplates = null;

	/** The transcribed templates, captured on first use (one material generation). */
	public static List<WireTemplate> table() {
		List<WireTemplate> tTable = sTemplates;
		if (tTable == null) sTemplates = tTable = List.of(
		// :287-290 — the hard arm, duration computed at multiplier 128
		new WireTemplate(":287", OP.stick     , OP.wireFine, 4, 16, 0, 128, false),
		new WireTemplate(":288", OP.stickLong , OP.wireFine, 8, 16, 0, 128, false),
		new WireTemplate(":289", OP.ingot     , OP.wireGt01, 2, 16, 0, 128, false),
		new WireTemplate(":290", OP.compressed, OP.wireGt01, 2, 16, 0, 128, false),
		// :292-295 — the easy arm, fixed durations 8/16/16/16
		new WireTemplate(":292", OP.stick     , OP.wireFine, 4, 16, 16/2, 0, true),
		new WireTemplate(":293", OP.stickLong , OP.wireFine, 8, 16, 16  , 0, true),
		new WireTemplate(":294", OP.ingot     , OP.wireGt01, 2, 16, 16  , 0, true),
		new WireTemplate(":295", OP.compressed, OP.wireGt01, 2, 16, 16  , 0, true));
		return tTable;
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (see class doc).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Handlers.java:289/:290/:294/:295 ingot|compressed → wireGt01 templates pour ZERO rows: the port item universe has no wireGt01 MaterialPrefixItems (GTMaterialItems.itemPathPrefixes carries OP.wireFine only — the 1x-wire family lives in the GTWires block domain); the wiremill's wireGt01 rows unlock with that item family (pool)",
			"all Compat_Recipes_* RM.Wiremill feeders — the P10 ruling (59 compat classes not ported)");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the GT6RecipesShCL form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesWiremill::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesWiremill::load);
	}

	/** Pours the template walk into the WIREMILL map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines)
		if (GT6RecipeMaps.WIREMILL == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (WireTemplate tTemplate : table()) {
			for (OreDictMaterial tMaterial : expandMaterials(tTemplate.inPrefix())) {
				Recipe tRecipe = buildRecipe(tTemplate, tMaterial);
				if (tRecipe == null) {tSkipped++; continue;} // the condition arm or a mat() → null side
				GT6RecipeMaps.WIREMILL.addRecipe(tRecipe);
				tPoured++;
			}
		}
		LOGGER.info("GT6 Wiremill recipes poured: {} loaded, {} skipped (condition-arm materials + unresolvable prefix items, = upstream addRecipeForMaterial false returns; the wireGt01 arm is the declared zero-pour pool)", tPoured, tSkipped);
		sLoaded = true;
	}

	/**
	 * The port counterpart of RecipeMapHandlerPrefix.addAllRecipesInternal (:173): the
	 * registered materials of the prefix in registration order (the port's single
	 * enumeration source, the GT6RecipesShCL.expandCrusherMaterials form).
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
	 * return). Condition gate :205 over the two arms — {@code And(ANTIMATTER.NOT,
	 * COATED.NOT, SMITHABLE, tEasyWorkable[.NOT])} with tEasyWorkable = Or(FURNACE, SOFT)
	 * (Loader_Recipes_Handlers.java:42) — then both-side mat() resolution, then the
	 * duration: mDuration &gt; 0 short-circuits (:218), else getCosts(:225-227) at the row
	 * multiplier. mCanBeBuffered T (the port has no UNUSED_MATERIAL tag, the ShCL declared
	 * deviation).
	 */
	static Recipe buildRecipe(WireTemplate aTemplate, OreDictMaterial aMaterial) {
		boolean tEasyWorkable = aMaterial.contains(TD.Processing.FURNACE) || aMaterial.contains(TD.Properties.SOFT);
		if (tEasyWorkable != aTemplate.easyArm()) return null; // the complementary condition arms
		if (aMaterial.contains(TD.Atomic.ANTIMATTER) || aMaterial.contains(TD.Compounds.COATED)
		|| !aMaterial.contains(TD.Processing.SMITHABLE)) return null; // :205
		Item tInItem = sMaterialItemResolver.apply(aTemplate.inPrefix(), aMaterial);
		if (tInItem == null) return null; // upstream :209 mat() → null
		Item tOutItem = sMaterialItemResolver.apply(aTemplate.outPrefix(), aMaterial);
		if (tOutItem == null) return null; // upstream :214 mat() → null
		long tDuration = aTemplate.duration() > 0 ? aTemplate.duration()
				: Math.max(1, costs(aTemplate.inPrefix(), 1, aTemplate.outPrefix(), aTemplate.outCount(), aTemplate.multiplier(), aMaterial));
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInItem, 1)},
				new ItemStack[] {new ItemStack(tOutItem, aTemplate.outCount())},
				new FluidStack[0], new FluidStack[0], tDuration, aTemplate.eUt(), 0);
	}

	/**
	 * Upstream RecipeMapHandlerPrefix.getCosts (:225-227) — the GT6RecipesShCL.crusherCosts
	 * transcription: {@code units(max(unitsIn, unitsOut), U, multiplier + multiplier*
	 * mToolQuality, T)}, the U-anchored translation with round-up.
	 */
	static long costs(OreDictPrefix aInPrefix, int aInCount, OreDictPrefix aOutPrefix, int aOutCount, long aMultiplier, OreDictMaterial aMaterial) {
		long tUnitsIn = aInPrefix.mAmount * aInCount;
		long tUnitsOut = aOutPrefix.mAmount * aOutCount;
		long tAmount = Math.max(tUnitsIn, tUnitsOut);
		long tTarget = aMultiplier + aMultiplier * aMaterial.mToolQuality;
		if (tTarget == 0) return 0;
		return Math.max(0, tAmount * tTarget / CSU() + ((tAmount * tTarget) % CSU() > 0 ? 1 : 0));
	}

	/** CS.U (the static-import shortcut is not available in a loader class without the CS import). */
	private static long CSU() {return gregapi.data.CS.U;}

	/** The live item lookup (GTMaterialItems.get :287) — null when the pair has no item-path item. */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTemplates = null;
	}
}
