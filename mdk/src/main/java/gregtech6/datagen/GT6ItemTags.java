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

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
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

	/**
	 * The builder-wand tool tag — #gt6:tools/builder_wand (task p24-builder-wand, the
	 * TOOLS_FILE/TOOLS_SAW snake shape). Upstream carries NO oredict crafting key for the
	 * wand (id402 proven, the research card) — the tag exists so future relay code and
	 * the tool-family recipe card key on the TAG, not the item.
	 */
	public static final TagKey<Item> TOOLS_BUILDER_WAND = gt6("tools/builder_wand");

	/** The dustRedstone recipe-material translation — #gt6:redstone (snake ruling). */
	public static final TagKey<Item> REDSTONE_DUSTS = gt6("redstone");

	/** The plateCurvedSn recipe-material translation — #gt6:plate_curved_tin (snake ruling). */
	public static final TagKey<Item> PLATE_CURVED_TIN = gt6("plate_curved_tin");

	/**
	 * The platform material-tag namespace (task p24-tags-provider-skeleton, the
	 * decisions.p24-tool-system-tag-strategy namespace face): {@code forge} on 1.20.1 (the
	 * Tags.Items constants — Tags.java:310-312/:220/:256) and {@code c} on NeoForge 21.1
	 * (Tags.java:799/:923). The emitted JSON location follows the tag id's namespace
	 * automatically — {@code data/forge/tags/items/**} vs {@code data/c/tags/items/**} —
	 * which is the whole fork surface: the family paths below are namespace-free.
	 */
	//? if forge {
	public static final String MATERIALS_NAMESPACE = "forge";
	//?} else {
	/*public static final String MATERIALS_NAMESPACE = "c";
	*///?}

	/** The ingot family — GTCEu TagPrefix.java:279 {@code defaultTagPath("ingots/%s")}. */
	public static final String INGOTS_FAMILY = "ingots/%s";

	/** The dust family — GTCEu TagPrefix.java:405 {@code defaultTagPath("dusts/%s")}. */
	public static final String DUSTS_FAMILY = "dusts/%s";

	/** The gem family — GTCEu TagPrefix.java:290 {@code defaultTagPath("gems/%s")}. */
	public static final String GEMS_FAMILY = "gems/%s";

	/** The nugget family — GTCEu TagPrefix.java:416 {@code defaultTagPath("nuggets/%s")}. */
	public static final String NUGGETS_FAMILY = "nuggets/%s";

	/**
	 * The storage-block family — GTCEu TagPrefix.java:727-729, the "Block of %s" prefix
	 * ("consisting out of 9 Ingots/Gems/Dusts") over the GT6 {@code blockIngot}/{@code
	 * blockGem}/{@code blockDust} block items.
	 */
	public static final String STORAGE_BLOCKS_FAMILY = "storage_blocks/%s";

	/**
	 * The raw-storage-block family — GTCEu TagPrefix.java:223, the rawOreBlock prefix
	 * {@code defaultTagPath("storage_blocks/raw_%s")} over the GT6 {@code blockRaw} items.
	 */
	public static final String STORAGE_BLOCKS_RAW_FAMILY = "storage_blocks/raw_%s";

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
		addMaterialTags(aProvider);
	}

	/**
	 * The p24 tool band: the two crafting-tool tags (one member each — the tag exists so
	 * recipes and future relay code key on the TAG, not the item), the two recipe-material
	 * tags, and the ecosystem append (the six formal tools into the platform tools tag —
	 * the user ruling's bidirectional face; the platform constant resolves to
	 * {@code forge:tools} on 1.20.1 and {@code c:tools} on 1.21.1).
	 */
	private void addToolTags(HolderLookup.Provider aProvider) {
		tag(TOOLS_FILE).add(item(GT6Tools.FILE.getId()));
		tag(TOOLS_SAW).add(item(GT6Tools.SAW.getId()));
		tag(TOOLS_BUILDER_WAND).add(item(GT6Tools.BUILDER_WAND.getId()));
		tag(REDSTONE_DUSTS).add(item(gt6Rl("dust_redstone")));
		tag(PLATE_CURVED_TIN).add(item(gt6Rl("plate_curved_tin")));
		tag(Tags.Items.TOOLS).add(
				item(GT6Tools.FILE.getId()), item(GT6Tools.SAW.getId()),
				item(GT6Tools.CROWBAR.getId()), item(GT6Tools.CUTTER.getId()), item(GT6Tools.CHISEL.getId()),
				item(GT6Tools.BUILDER_WAND.getId()));
	}

	/**
	 * The p24 tags-foundation material band: the first P0 platform item tags — one
	 * {@code <platform>:<family>/<material>} tag per (family, material) that actually has
	 * a registered item, strictly NO {@code addOptional} (TagsProvider.java:85-94 throws on
	 * a missing reference, and every id below is a live-registered item at datagen time —
	 * both walks are the registration walks, so a gap fails runData loudly). Family paths
	 * are the GTCEu {@code defaultTagPath} precedents (the constants above); the material
	 * segment is the GT {@code mNameInternal} snake — the same composition as the item ids
	 * ({@link GTMaterialItems#itemIdOf}), keeping tag and member derivable from one rule
	 * (cross-mod name normalization is a declared later-card open item). The namespace is
	 * {@link #MATERIALS_NAMESPACE}; a future band that needs to COPY block tags migrates to
	 * the {@code contentsGetter()} ItemTagsProvider shape (vanilla TagsProvider.java:119-121,
	 * RemoveTagDatagenTest.java:58) — this batch has only element members, so the plain
	 * TagsProvider surface stays.
	 */
	private void addMaterialTags(HolderLookup.Provider aProvider) {
		// item-path families: the ingot/dust/gem/nugget items over the material universe
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			String tFamily = itemTagFamily(tPair.prefix());
			if (tFamily != null) {
				tag(materialTag(tFamily, tPair.material()))
						.add(item(gt6Rl(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()))));
			}
		}
		// storage-block families: the blockPath prefixes' block ITEMS (the registered truth,
		// not the offline walk — a defensively-deduped id has no item and must not dangle)
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.items().keySet()) {
			String tFamily = storageTagFamily(tPair.prefix());
			if (tFamily != null) {
				tag(materialTag(tFamily, tPair.material()))
						.add(item(gt6Rl(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()))));
			}
		}
	}

	/** The platform tag key of one (family, material) face — {@code <namespace>:<family>/<materialSnake>}. */
	public static TagKey<Item> materialTag(String aFamilyPath, OreDictMaterial aMaterial) {
		// the composed path lives in a local so both ctor args are bare identifiers — the
		// stonecutter two-arg-ctor shift deliberately skips parenthesized argument
		// expressions (mdk/stonecutter.gradle.kts regex note), so an inline
		// formatted(...) argument would stay un-shifted and break the 21.1 leg compile
		String tPath = aFamilyPath.formatted(GTMaterialItems.snakeCase(aMaterial.mNameInternal));
		// the 1.20.1 two-arg ctor form; shifted to fromNamespaceAndPath on the 21.1 leg
		return TagKey.create(Registries.ITEM, new ResourceLocation(MATERIALS_NAMESPACE, tPath));
	}

	/** The item-path family path of a prefix, or null when the prefix carries no P0 platform tag. */
	private static String itemTagFamily(OreDictPrefix aPrefix) {
		if (aPrefix == OP.ingot) return INGOTS_FAMILY;
		if (aPrefix == OP.dust) return DUSTS_FAMILY;
		if (aPrefix == OP.gem) return GEMS_FAMILY;
		if (aPrefix == OP.nugget) return NUGGETS_FAMILY;
		return null;
	}

	/** The storage-block family path of a prefix, or null when the prefix is not a storage block (plate/solid). */
	private static String storageTagFamily(OreDictPrefix aPrefix) {
		if (aPrefix == OP.blockIngot || aPrefix == OP.blockGem || aPrefix == OP.blockDust) return STORAGE_BLOCKS_FAMILY;
		if (aPrefix == OP.blockRaw) return STORAGE_BLOCKS_RAW_FAMILY;
		return null;
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
