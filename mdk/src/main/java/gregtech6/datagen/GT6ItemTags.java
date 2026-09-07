package gregtech6.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregtech6.registry.GT6Tools;

/**
 * The GT6 item-tag datagen home — task p24-tool-system spec ④, the FIRST TagsProvider of
 * the port (the card ruling decisions.p24-tool-system-recipe-provider-first: the minimal
 * provider lands here, the tags-foundation card TAKES OVER this file and extends it band
 * by band). The base class is vanilla {@link TagsProvider} over {@link Registries#ITEM};
 * the five-argument super constructor is shape-identical on both legs (forge 1.20.1
 * TagsProvider.java:51 / NeoForge 21.1 TagsProvider.java:47 — PackOutput, ResourceKey,
 * lookup future, mod id, nullable ExistingFileHelper), so this file carries ZERO
 * {@code //?} except the {@link Tags} import (the class is not on the stonecutter swap
 * table and moved to {@code c:} constants on NeoForge).
 *
 * <p>Tag-naming ruling (decisions.p24-tool-system-tag-strategy): the crafting-tool
 * ingredients are SELF-OWNED tags translating the upstream oredict names by the
 * snake_case rule — {@code craftingToolFile}/{@code craftingToolSaw}
 * (CS.java:1867/:1864) → {@code #gt6:tools/file}/{@code #gt6:tools/saw}; the recipe
 * materials likewise — {@code dustRedstone} → {@code #gt6:redstone},
 * {@code plateCurvedSn} → {@code #gt6:plate_curved_tin} (vanilla offers no
 * redstone-dust item tag on either leg, only REDSTONE_ORES — the vanilla route is
 * ruled out). The five formal tools ride BOTH their gt6 tag and the ecosystem tag:
 * forge 1.20.1 {@code #forge:tools} (Tags.Items.TOOLS, Tags.java:419 — no saw/file
 * subdivision exists, which is what legitimizes the self-owned pair) and NeoForge 21.1
 * {@code #c:tools} (the same constant, namespace "c" — Tags.java:799/:923).
 *
 * <p>Element validation note: TagsProvider checks element references against the live
 * registry (the forge :114 verifyIfPresent chain over lookupProvider), and datagen runs
 * after mod construction — every gt6 element below is a registered item by then.
 */
public final class GT6ItemTags extends TagsProvider<Item> {

	/** The craftingToolFile oredient translation — #gt6:tools/file (CS.java:1867 snake). */
	public static final TagKey<Item> TOOLS_FILE = gt6("tools/file");

	/** The craftingToolSaw oredient translation — #gt6:tools/saw (CS.java:1864 snake). */
	public static final TagKey<Item> TOOLS_SAW = gt6("tools/saw");

	/** The dustRedstone recipe-material translation — #gt6:redstone (snake ruling). */
	public static final TagKey<Item> REDSTONE_DUSTS = gt6("redstone");

	/** The plateCurvedSn recipe-material translation — #gt6:plate_curved_tin (snake ruling). */
	public static final TagKey<Item> PLATE_CURVED_TIN = gt6("plate_curved_tin");

	public GT6ItemTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider,
			ExistingFileHelper aExistingFileHelper) {
		super(aOutput, Registries.ITEM, aLookupProvider, GT6DataGenerators.MOD_ID, aExistingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider aProvider) {
		// The takeover seam: the tags-foundation card appends its own add*Tags(aProvider)
		// bands AFTER the tool band, one band per logical family (the GT6EnUs table-tail
		// append convention), and hoists shared helpers if a second caller appears.
		addToolTags(aProvider);
	}

	/**
	 * The p24 tool band: the two crafting-tool tags (one member each — the tag exists so
	 * recipes and future relay code key on the TAG, not the item), the two recipe-material
	 * tags, and the ecosystem append (the five formal tools into the platform tools tag —
	 * the user ruling's bidirectional face; the platform constant resolves to
	 * {@code forge:tools} on 1.20.1 and {@code c:tools} on 1.21.1).
	 */
	private void addToolTags(HolderLookup.Provider aProvider) {
		tag(TOOLS_FILE).add(item(GT6Tools.FILE.getId()));
		tag(TOOLS_SAW).add(item(GT6Tools.SAW.getId()));
		tag(REDSTONE_DUSTS).add(item(gt6Rl("dust_redstone")));
		tag(PLATE_CURVED_TIN).add(item(gt6Rl("plate_curved_tin")));
		tag(Tags.Items.TOOLS).add(
				item(GT6Tools.FILE.getId()), item(GT6Tools.SAW.getId()),
				item(GT6Tools.CROWBAR.getId()), item(GT6Tools.CUTTER.getId()), item(GT6Tools.CHISEL.getId()));
	}

	// ------------------------------------------------------------------ shared helpers

	/** The gt6-namespaced tag key factory (single-source so no path can drift). */
	public static TagKey<Item> gt6(String aPath) {
		return TagKey.create(Registries.ITEM, gt6Rl(aPath));
	}

	/** The gt6-namespaced resource location factory (the 1.20.1 two-arg ctor form). */
	public static ResourceLocation gt6Rl(String aPath) {
		return new ResourceLocation(GT6DataGenerators.MOD_ID, aPath);
	}

	/** The element face TagsProvider appends with — a registry key over the gt6 id. */
	private static ResourceKey<Item> item(ResourceLocation aId) {
		return ResourceKey.create(Registries.ITEM, aId);
	}
}
