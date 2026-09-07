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
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

/**
 * The Chisel recipe book — task p19-chisel-recipes, the three upstream sources of the
 * RM.Chisel map ({@link GT6RecipeMaps#CHISEL}), in upstream order:
 *
 * <ul>
 * <li><b>the stonetypes line</b> — RM.java:470 {@code RM.Chisel.addRecipe1(T, 16, 16,
 * aStone, aChiseled)} inside the RM.add_stonetypes walk: one row per GT stone,
 * blockSolid-representative → stoneChiseled-representative. In the BlockStones universe
 * (BlockStones.java:135-194 {@code OM.reg_}) those representatives are the STONE and CHISL
 * variants, so the port pours 17 rows of {@code gt6:<snake>} STONE → CHISL — eUt 16,
 * duration 16, deterministic.</li>
 * <li><b>the bricks line</b> — RM.java:507-518: {@code if (ST.valid(aCracked))} the :508
 * bricks → cracked row, {@code else if (ST.valid(aCobble))} the :514 bricks → cobble row.
 * Every GT stone registers its full 16-variant family in this port (GTStoneBlocks), so the
 * cracked-validity gate resolves TRUE for all 17 stones and all 17 rows pour in the :508
 * shape BRICK → CRACK; the :514 shape is transcribed as the else-branch but is zero-hit
 * (the audit note on {@link #SKIPPED_UPSTREAM}).</li>
 * <li><b>the vanilla pair</b> — Loader_Recipes_Vanilla.java:772-773, eUt 16 duration 16:
 * stone → stonebrick-meta-3 (= chiseled stone bricks) and stonebrick-meta-0 (= stone
 * bricks) → stonebrick-meta-2 (= cracked stone bricks). The 1.20.1 flat identities are
 * {@code Blocks.STONE → Blocks.CHISELED_STONE_BRICKS} and
 * {@code Blocks.STONE_BRICKS → Blocks.CRACKED_STONE_BRICKS}.</li>
 * </ul>
 *
 * <p><b>The variant carrier</b> (the declared port-ism, evolved by task
 * p21-stoneblocks-16item-registry-split): upstream the rows live in the 1.7.10 item-damage
 * domain ({@code ST.make(block, 1, meta)}). At the p19 landing one GT stone family was ONE
 * registered Block, so the port encoded the variant in a stack tag ({@link #VARIANT_TAG}).
 * Task p21 split the registry per-pair — one Block+BlockItem per (stone, variant), the id
 * scheme riding {@link GTStoneBlocks#path} — so LIVE, each row leg now resolves its own
 * variant item and the item identity alone separates the domain. The tag STILL rides on
 * both legs of every GT stone row ({@link #withVariant}): it is the offline domain
 * separator (the synthetic-item test universe stands ONE item in for every GT stone leg,
 * only the tag tells the rows apart) and a defensive decode face. Vanilla rows carry no
 * tag: the recipe inputs' tags are empty so the equality falls back to plain item
 * identity, the upstream damage-0 behaviour.
 * The encoders live here (the pour is the writer side of the contract); the gate consumes
 * them so the tag key has exactly one definition site.
 *
 * <p><b>Load timing</b> (the GT6RecipesShCL precedent): a self-contained MOD-bus listener
 * pouring at FMLCommonSetup.enqueueWork; the stone item seam is injectable for the offline
 * tests (no registry access offline). {@code load()} is idempotent per JVM generation and
 * the pour-flag retires with the GT6RecipeMaps generation (ADR-P18).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesStoneChisel {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * The stack-tag key carrying the {@link StoneVariant} serialized name for the GT stone
	 * rows. The pour writes it on both legs; the GTChiselItem gate reads it (stack → findRecipe
	 * probe, and recipe output stack → setBlock variant). One definition site (this constant).
	 */
	public static final String VARIANT_TAG = "gt6:variant";

	/**
	 * Writes the variant tag onto a stack (the GT stone row legs). The stack is modified in
	 * place and returned. The 21.1 leg rides the CUSTOM_DATA component (the 1.20.1 tag died
	 * with the item-NBT removal).
	 */
	public static ItemStack withVariant(ItemStack aStack, StoneVariant aVariant) {
		//? if forge {
		aStack.getOrCreateTag().putString(VARIANT_TAG, aVariant.getSerializedName());
		//?} else {
		/*aStack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY,
				aData -> aData.update(aTag -> aTag.putString(VARIANT_TAG, aVariant.getSerializedName())));
		*///?}
		return aStack;
	}

	/**
	 * Reads the variant tag off a stack — null when the stack carries no (or an unknown)
	 * variant. The unknown-name arm keeps foreign or stale tags from aliasing a variant.
	 */
	@Nullable
	public static StoneVariant variantOf(ItemStack aStack) {
		String tName = variantName(aStack);
		if (tName == null || tName.isEmpty()) return null;
		for (StoneVariant tVariant : StoneVariant.VALUES) {
			if (tVariant.getSerializedName().equals(tName)) return tVariant;
		}
		return null;
	}

	@Nullable
	private static String variantName(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag() ? aStack.getTag().getString(VARIANT_TAG) : null;
		//?} else {
		/*net.minecraft.world.item.component.CustomData tData = aStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		return tData == null ? null : tData.copyTag().getString(VARIANT_TAG);
		*///?}
	}

	/**
	 * The {@link StoneVariant} for a serialized name — null when unknown (the gate's
	 * BlockState decode arm needs the same tolerance as {@link #variantOf}).
	 */
	@Nullable
	public static StoneVariant variantByName(@Nullable String aName) {
		if (aName == null || aName.isEmpty()) return null;
		for (StoneVariant tVariant : StoneVariant.VALUES) {
			if (tVariant.getSerializedName().equals(aName)) return tVariant;
		}
		return null;
	}

	/** One leg of a transcribed row: a vanilla block item, or a (GT stone snake, variant) pair. */
	public record StackSpec(@Nullable Supplier<Item> vanilla, @Nullable String stoneSnake, @Nullable StoneVariant variant) {
		/** A vanilla block-item leg (the Loader_Recipes_Vanilla pair). */
		public static StackSpec vanilla(Supplier<Item> aItem) { return new StackSpec(aItem, null, null); }
		/** A GT stone leg — the (snake, variant) pair the resolver turns into a tagged stack. */
		public static StackSpec stone(String aSnake, StoneVariant aVariant) { return new StackSpec(null, aSnake, aVariant); }
	}

	/** One transcribed row ({@code note} carries the upstream line number). */
	public record ChiselRow(String note, StackSpec input, StackSpec output) {}

	/**
	 * The resolution seam: the live registry lookups by default (the GTStoneBlocks.get seam
	 * shape), fixtures injected offline (no registry access offline — the GT6RecipesShCL
	 * synthetic-item convention). Public as the cross-package offline test seam (the
	 * GTChiselItem.sPayPerPointCalls one-notch visibility precedent). The argument is the
	 * <b>composite id path</b> ({@link GTStoneBlocks#path}: variant 0 = the bare snake,
	 * the other 15 = {@code snake_<variant>}) — task p21 splits one item per variant, so
	 * each row leg resolves its OWN variant item (live); offline both legs of a row collapse
	 * onto the injected item and only the variant tag separates them (the declared port-ism
	 * below).
	 */
	public static Function<String, Item> sStoneItemResolver = GT6RecipesStoneChisel::resolveStoneItem;

	/** The live item lookup of a composite stone path ({@link GTStoneBlocks#path} form) — null before registration. */
	@Nullable
	private static Item resolveStoneItem(String aPath) {
		RegistryObject<Item> tHandle = GTStoneBlocks.item(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The stonetypes line — RM.java:470 verbatim per stone (STONE → CHISL, eUt 16, duration
	 * 16), 17 rows in CS.java:1668 declaration order. The upstream {@code ST.valid(aStone)}
	 * gate (:451/:467) resolves TRUE for all 17: every stone registers its full family.
	 */
	public static List<ChiselRow> stoneTypesTable() {
		List<ChiselRow> rRows = new ArrayList<>();
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			rRows.add(new ChiselRow("RM.java:470 " + tStone.snake(),
					StackSpec.stone(tStone.snake(), StoneVariant.STONE),
					StackSpec.stone(tStone.snake(), StoneVariant.CHISL)));
		}
		return List.copyOf(rRows);
	}

	/**
	 * The bricks line — RM.java:507-518 per stone: the :508 BRICK → CRACK row when the stone
	 * carries a cracked variant, else the :514 BRICK → COBBL row. The 16-variant family is
	 * unconditional in this port, so the :508 shape wins for all 17 stones and the :514
	 * branch is the transcribed zero-hit else (see {@link #SKIPPED_UPSTREAM}). The
	 * {@code aCrackedAvailable} parameter keeps the upstream :507/:513 if-else shape in the
	 * DATA — the tests pin the zero-hit verdict per stone.
	 */
	public static List<ChiselRow> bricksTable() {
		List<ChiselRow> rRows = new ArrayList<>();
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			boolean tCrackedAvailable = true; // :507 ST.valid(aCracked) — the full family always registers
			rRows.add(tCrackedAvailable
					? new ChiselRow("RM.java:508 " + tStone.snake(),
							StackSpec.stone(tStone.snake(), StoneVariant.BRICK),
							StackSpec.stone(tStone.snake(), StoneVariant.CRACK))
					: new ChiselRow("RM.java:514 " + tStone.snake(),
							StackSpec.stone(tStone.snake(), StoneVariant.BRICK),
							StackSpec.stone(tStone.snake(), StoneVariant.COBBL)));
		}
		return List.copyOf(rRows);
	}

	/**
	 * The vanilla pair — Loader_Recipes_Vanilla.java:772-773 verbatim (eUt 16, duration 16),
	 * through the 1.20.1 flat block identities (the 1.7.10 stonebrick metas 3/0/2 are the
	 * chiseled/normal/cracked stone bricks blocks).
	 */
	public static List<ChiselRow> vanillaTable() {
		return List.of(
				new ChiselRow("Loader_Recipes_Vanilla.java:772",
						StackSpec.vanilla(() -> Blocks.STONE.asItem()),
						StackSpec.vanilla(() -> Blocks.CHISELED_STONE_BRICKS.asItem())),
				new ChiselRow("Loader_Recipes_Vanilla.java:773",
						StackSpec.vanilla(() -> Blocks.STONE_BRICKS.asItem()),
						StackSpec.vanilla(() -> Blocks.CRACKED_STONE_BRICKS.asItem())));
	}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (the GT6RecipesShCL
	 * form). Everything here is a POOL item, not a silent drop.
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"RM.java:469 the TE Mana Bath row (FL.Mana_TE gate) — the Thermal Expansion mana fluid does not exist in this port (TE pool)",
			"RM.java:471 the CR.shaped chiseled hand row (the 'y' tool key) — the crafting bridge pool (no vanilla crafting bridge in mdk, the P19 split verdict ⑤)",
			"RM.java:509-510 the Hammer/Crusher sibling rows of the bricks->cracked line — the tool-family pool",
			"RM.java:511-512 the CR.shaped cracked hand rows ('h'/'y' tool keys) — the crafting bridge pool",
			"RM.java:515-516 the Hammer/Crusher sibling rows of the bricks->cobble line — the tool-family pool (zero-hit upstream for the 17 GT stones: every stone carries a cracked variant)",
			"RM.java:517-518 the CR.shaped cobble hand rows — the crafting bridge pool (zero-hit upstream, same :513 else-if gate)",
			"RecipeMapChisel.java:47-64 the oredict ring-composition findRecipe synthesizer (blockSolid/storage-prefix inputs rotating through the oredict set) — the OM runtime face (OreDictManager.getOres + the GAPI_POST.mFinishedServerStarted gate); declared deviation: the port map is the base RecipeMap",
			"BlockStones.java:573-576 the BlockStones TE-face chisel arm (the CHISEL_MAPPINGS direct-meta write, pays 1250/octant) — the TE-face pool; the port gate routes every target through the RM.Chisel findRecipe face at the ToolCompat.java:224-229 shape (architect ruling)",
			"GT_Tool_Chisel.java:57-79 convertBlockDrops (the mining-drop chisel conversion) — 1.20.1 has no HarvestDropsEvent, so the BlockStones arm (:73-77, the CHISEL_MAPPINGS variant item) LANDED as a loot face instead: GT6StoneBlockLoot alternatives[match_tool(gt6:chisel) -> the mapped variant item, otherwise the BlockStones.java:731 baseline] (task p21-chisel-drop-conversion, 170 dispatch / 102 pass-through tables); the vanilla stone/stonebrick arms (:58-72) stay pooled (the vanilla-owned tables carry silk arms an override would inherit, GLM the architect ruling for them)",
			"BlockStones.java:495-498 the LaserEngraver white-lens rows — the research-card cut pool",
			"BlockStones.java:405-411 the CHISL equal-set machine family (Hammer/Crusher/Shredder/generify/smelting) — the tool/smelting pools (the CHISEL-map face of the family is RM.java:470, poured here)",
			"BlockStones.java:419-424 the SMOTH equal-set CR.shaped hand rows x6 (chiseled 'y'/bricks x4/tiles x2/small-tiles x2/small-bricks x2/windmill x2) — the crafting bridge pool");

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation — a bare GT6RecipeMaps.reset()
	// must retire the flag WITH the maps, or load() silently early-returns on the
	// "maps cleared × flag set" poison state (the GT6RecipesShCL lesson).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesStoneChisel::resetForTest);}

	/** FMLCommonSetup.enqueueWork — blocks and items are registered by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesStoneChisel::load);
	}

	/** Pours the three tables into the CHISEL map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod, tests may race it
		if (GT6RecipeMaps.CHISEL == null) return; // reset() between init and load — a broken lifecycle
		int tLoaded = pour(GT6RecipeMaps.CHISEL, "Chisel stonetypes", stoneTypesTable());
		tLoaded += pour(GT6RecipeMaps.CHISEL, "Chisel bricks", bricksTable());
		tLoaded += pour(GT6RecipeMaps.CHISEL, "Chisel vanilla", vanillaTable());
		sLoaded = true;
		LOGGER.info("GT6 Chisel recipes poured: {} loaded total across the three sources (per-source skip counts above)", tLoaded);
	}

	private static int pour(RecipeMap aMap, String aLabel, List<ChiselRow> aRows) {
		int tPoured = 0, tSkipped = 0;
		for (ChiselRow tRow : aRows) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // = upstream mat() → null silent drop
			aMap.addRecipe(tRecipe);
			tPoured++;
		}
		LOGGER.info("GT6 {} recipes poured: {} loaded, {} skipped", aLabel, tPoured, tSkipped);
		return tPoured;
	}

	/**
	 * Row → Recipe, or null when any leg fails to resolve (the upstream silent-drop
	 * semantics). The upstream {@code addRecipe1(T, 16, 16, in, out)} shape: buffered,
	 * no fluids, eUt 16, duration 16, deterministic (no chances — every source row of the
	 * three is the two-arg form).
	 */
	static Recipe buildRecipe(ChiselRow aRow) {
		ItemStack tInput = buildStack(aRow.input());
		if (tInput == null) return null;
		ItemStack tOutput = buildStack(aRow.output());
		if (tOutput == null) return null;
		return new Recipe(true, new ItemStack[] {tInput}, new ItemStack[] {tOutput}, new FluidStack[0], new FluidStack[0], 16, 16, 0);
	}

	/** StackSpec → ItemStack (variant-tagged for GT stone legs), or null when the item fails to resolve. */
	@Nullable
	static ItemStack buildStack(StackSpec aSpec) {
		Item tItem = aSpec.vanilla() != null ? aSpec.vanilla().get()
				: sStoneItemResolver.apply(GTStoneBlocks.path(aSpec.stoneSnake(), aSpec.variant()));
		if (tItem == null) return null;
		ItemStack tStack = new ItemStack(tItem, 1);
		if (aSpec.variant() != null) withVariant(tStack, aSpec.variant());
		return tStack;
	}

	/** Test seam (public: the cross-package items.tools test drive, the one-notch precedent): clears the poured flag so a fresh generation can re-pour. */
	public static void resetForTest() {
		sLoaded = false;
	}
}
