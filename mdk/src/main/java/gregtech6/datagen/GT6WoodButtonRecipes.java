package gregtech6.datagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The wood-button belt crafting band (task toolhead-r11d-wood-button-belt) — the upstream
 * {@code Loader_Recipes_Woods.java:137-152} "Generic Wooden Stuff made mostly from Buttons"
 * rows: the 6 misc rows (:137-142 gear/gearSmall/casingSmall/plateTiny/ring/round) + the 10
 * tool-head rows (:143-152 the finished Wood hammer + the nine raw heads, the R4 wood
 * acquisition route of the r11 tool-head census).
 *
 * <p><b>Why a separate provider, and why it implements DataProvider DIRECTLY</b>: the
 * GT6GunRecipes ruling — {@code RecipeProvider.getName()} is FINAL on both legs, and
 * GT6CraftingRecipes.java is the in-flight toolhead cards' file (this card's FILES_SCOPE
 * zero-overlap ruling). The rows are STATIC (fixed MT.Wood), so no walk face: plain vanilla
 * {@code minecraft:crafting_shaped} through the vanilla {@link ShapedRecipeBuilder} (the
 * headRowBuilder semantics — the head item IS the identity; the builder owns the book
 * category, the show_notification flag and the recipes/&lt;folder&gt;/ advancement id, the
 * vanilla tree canon). The unlock-advancement WRITE is stopped (the id1359 ruling, the
 * GT6GunRecipes form — the builder keeps its {@code unlockedBy} API face, the JSON never
 * lands).
 *
 * <p><b>The letters</b> (CR.java:339-360): {@code 'P'}/{@code 'B'} = {@code OD.buttonWood}
 * → {@code #minecraft:wooden_buttons} (the vanilla tag — the 1.7.10 {@code
 * minecraft:wooden_button} id does not exist on this generation; the tag carries the eleven
 * wooden buttons, the LoaderItemData.java:700 vanilla face) except the :137 gear row where
 * 'P' = {@code OD.plankAnyWood} → {@code #minecraft:planks} (the gt6 planks join the
 * vanilla tag, GT6ItemTags.addTreeTags); {@code 's'} = {@code #gt6:tools/saw} (the
 * CR.java:356 letter, the p24 tag); {@code 'r'} = {@code #gt6:tools/soft_hammer} (:355);
 * {@code 'g'} = {@code #gt6:tools/hand_drill} (:345 handdrill, the w5-t4 tag); {@code 'k'}
 * = {@code #gt6:tools/knife} (:349); {@code 'f'} = {@code #gt6:tools/file} (:344); {@code
 * 'v'} = the sawaxe slot — upstream the axe+saw re-registration alias
 * (LoaderOreDictReRegistrations.java:645-646), ported per the main-session ruling as the
 * COMBINED Ingredient over {@code #gt6:tools/axe} ∪ {@code #gt6:tools/saw} (both port tags
 * in register — the "new gt6 tag" branch of the ruling never fires; the serialized
 * ingredient array is the union any- face).
 *
 * <p>The row shapes/patterns are the upstream rows VERBATIM (2-wide rows stay 2-wide —
 * vanilla pads at runtime, the dissolvePattern face). Outputs resolve through
 * {@link GTMaterialItems#get} (datagen JVM, fail-loud — the offline walk truth that the
 * 16 wood pairs generate is pinned by GT6WoodButtonCraftingJsonTest).
 */
public class GT6WoodButtonRecipes implements net.minecraft.data.DataProvider {

	/** One belt form: the id leaf, the upstream line, the prefix, the output count, the category, the pattern (Woods:137-152 verbatim). */
	record BeltForm(String aId, int aLine, OreDictPrefix aPrefix, int aCount, RecipeCategory aCategory, String[] aPattern) {
	}

	/** The 16-row band — upstream order: the 6 misc rows then the 10 head rows. */
	static final List<BeltForm> BELT_FORMS = List.of(
			new BeltForm("gear_gt", 137, OP.gearGt, 1, RecipeCategory.MISC, new String[] {"BPB", "PsP", "BPB"}),
			new BeltForm("gear_gt_small", 138, OP.gearGtSmall, 1, RecipeCategory.MISC, new String[] {"P ", " s"}),
			new BeltForm("casing_small", 139, OP.casingSmall, 2, RecipeCategory.MISC, new String[] {" P", "s "}),
			new BeltForm("plate_tiny", 140, OP.plateTiny, 9, RecipeCategory.MISC, new String[] {"s ", " P"}),
			new BeltForm("ring", 141, OP.ring, 4, RecipeCategory.MISC, new String[] {"P ", " k"}),
			new BeltForm("round", 142, OP.round, 9, RecipeCategory.MISC, new String[] {"P ", "fk"}),
			new BeltForm("tool_head_hammer", 143, OP.toolHeadHammer, 1, RecipeCategory.TOOLS, new String[] {"PP ", "PPg", "PPv"}),
			new BeltForm("tool_head_raw_arrow", 144, OP.toolHeadRawArrow, 4, RecipeCategory.TOOLS, new String[] {"  P", "r v"}),
			new BeltForm("tool_head_raw_sword", 145, OP.toolHeadRawSword, 1, RecipeCategory.TOOLS, new String[] {" P ", "rPv"}),
			new BeltForm("tool_head_raw_pickaxe", 146, OP.toolHeadRawPickaxe, 1, RecipeCategory.TOOLS, new String[] {"PPP", "rgv"}),
			new BeltForm("tool_head_raw_shovel", 147, OP.toolHeadRawShovel, 1, RecipeCategory.TOOLS, new String[] {"rPv"}),
			new BeltForm("tool_head_raw_spade", 148, OP.toolHeadRawSpade, 1, RecipeCategory.TOOLS, new String[] {" P ", "r v"}),
			new BeltForm("tool_head_raw_axe", 149, OP.toolHeadRawAxe, 1, RecipeCategory.TOOLS, new String[] {" PP", "rPv"}),
			new BeltForm("tool_head_raw_hoe", 150, OP.toolHeadRawHoe, 1, RecipeCategory.TOOLS, new String[] {" PP", "r v"}),
			new BeltForm("tool_head_raw_sense", 151, OP.toolHeadRawSense, 1, RecipeCategory.TOOLS, new String[] {"PPP", "   ", "r v"}),
			new BeltForm("tool_head_raw_plow", 152, OP.toolHeadRawPlow, 1, RecipeCategory.TOOLS, new String[] {"PPP", "PPP", "r v"}));

	/** The recipe id — {@code <form>/wood} (the head-row <form>/<snake> convention; free leaves, no craftfrom collision). */
	static ResourceLocation rowId(BeltForm aForm) {
		String tPath = aForm.aId() + "/wood"; // the local — the stonecutter two-arg-ctor shift skips parenthesized args (GT6ItemTags.gt6Rl note)
		return new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
	}

	/**
	 * The row letters — the 'P' plank/button split is the :137 row alone; the button slot
	 * is the {@code #minecraft:wooden_buttons} tag (the OD.buttonWood modern face); the
	 * 'v' slot is the axe+saw combined Ingredient (the union any- face — vanilla has no
	 * public multi-tag factory on 1.20.1, the fromValues/TagValue pair is the public
	 * composition on both legs).
	 */
	static Ingredient beltIngredient(BeltForm aForm, char aKey) {
		return switch (aKey) {
			case 'P' -> "gear_gt".equals(aForm.aId()) // the :137 'P' = plankAnyWood face; :138-152 'P' = the button
					? Ingredient.of(ItemTags.PLANKS)
					: Ingredient.of(ItemTags.WOODEN_BUTTONS);
			case 'B' -> Ingredient.of(ItemTags.WOODEN_BUTTONS);
			case 's' -> Ingredient.of(GT6ItemTags.TOOLS_SAW);
			case 'r' -> Ingredient.of(GT6ItemTags.TOOLS_SOFT_HAMMER);
			case 'g' -> Ingredient.of(GT6ItemTags.TOOLS_HAND_DRILL);
			case 'k' -> Ingredient.of(GT6ItemTags.TOOLS_KNIFE);
			case 'f' -> Ingredient.of(GT6ItemTags.TOOLS_FILE);
			case 'v' -> Ingredient.fromValues(Stream.of( // the sawaxe union
					new Ingredient.TagValue(GT6ItemTags.TOOLS_AXE),
					new Ingredient.TagValue(GT6ItemTags.TOOLS_SAW)));
			default -> throw new IllegalArgumentException("unknown belt row letter: " + aKey);
		};
	}

	/** The output item — the datagen-JVM resolver, fail-loud on a registration miss (the r9-39 emission posture). */
	static net.minecraft.world.item.Item beltResult(BeltForm aForm) {
		var tItem = GTMaterialItems.get(aForm.aPrefix(), MT.Wood); // RegistryObject/DeferredHolder — the leg-split hides behind var
		if (tItem == null) throw new IllegalStateException("the wood-button belt needs " + aForm.aId() + "_wood (Woods:" + aForm.aLine() + ") but the pair is not registered");
		return tItem.get();
	}

	private final PackOutput mOutput;
	/** The 21.1 run feeds saveStable the registries lookup; the 1.20.1 leg ignores it. */
	private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> mLookup;

	public GT6WoodButtonRecipes(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aLookup) {
		mOutput = aOutput;
		mLookup = aLookup;
	}

	/** Unique provider name (the "Duplicate provider" check — see the class javadoc). */
	@Override
	public String getName() {
		return "Recipes: gt6:wood_button_belt";
	}

	//? if forge {
	@Override
	public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
		PackOutput.PathProvider tRecipePaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
		// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359):
		// JEI/EMI ubiquitous, the vanilla recipe book is dead weight (the GT6GunRecipes form).
		java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
		java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
		build(tFinished -> {
			if (!tSeen.add(tFinished.getId())) throw new IllegalStateException("Duplicate recipe " + tFinished.getId());
			tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, GT6ForeignRowConvergence.converged(tFinished).serializeRecipe(), tRecipePaths.json(tFinished.getId())));
		});
		return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture<?>[0]));
	}
	//?} else {
	/*@Override
	public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
		PackOutput.PathProvider tRecipePaths = mOutput.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.RECIPE);
		// the unlock-advancement write stopped here (id1359, the GT6GunRecipes form).
		return mLookup.thenCompose(tRegistries -> {
			java.util.List<java.util.concurrent.CompletableFuture<?>> tFutures = new java.util.ArrayList<>();
			java.util.Set<ResourceLocation> tSeen = new java.util.HashSet<>();
			build(new net.minecraft.data.recipes.RecipeOutput() {
				@Override
				public void accept(ResourceLocation aId, net.minecraft.world.item.crafting.Recipe<?> aRecipe,
						net.minecraft.advancements.AdvancementHolder aAdvancement,
						net.neoforged.neoforge.common.conditions.ICondition... aConditions) {
					if (!tSeen.add(aId)) throw new IllegalStateException("Duplicate recipe " + aId);
					tFutures.add(net.minecraft.data.DataProvider.saveStable(aCache, tRegistries,
							net.minecraft.world.item.crafting.Recipe.CONDITIONAL_CODEC,
							java.util.Optional.of(GT6ForeignRowConvergence.conditioned(aRecipe, tRegistries, aConditions)),
							tRecipePaths.json(aId)));
				}

				@Override
				public net.minecraft.advancements.Advancement.Builder advancement() {
					return net.minecraft.advancements.Advancement.Builder.recipeAdvancement()
							.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
				}
			});
			return java.util.concurrent.CompletableFuture.allOf(tFutures.toArray(new java.util.concurrent.CompletableFuture<?>[0]));
		});
	}
	*///?}

	/** The has() helper — the one criterion shape is leg-split (the GT6GunRecipes form). */
	//? if forge {
	private static net.minecraft.advancements.CriterionTriggerInstance has(TagKey<net.minecraft.world.item.Item> aTag) {
		return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
				net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(aTag).build());
	}
	//?} else {
	/*private static net.minecraft.advancements.Criterion<?> has(TagKey<net.minecraft.world.item.Item> aTag) {
		return net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.createCriterion(
				new net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance(
						java.util.Optional.empty(),
						net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.Slots.ANY,
						java.util.List.of(net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(aTag).build())));
	}
	*///?}

	//? if forge {
	private void build(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
	//?} else {
	/*private void build(net.minecraft.data.recipes.RecipeOutput aOutput) {
	 *///?}
		for (BeltForm tForm : BELT_FORMS) {
			ResourceLocation tId = rowId(tForm);
			Map<Character, Ingredient> tKey = new LinkedHashMap<>();
			for (String tPatternRow : tForm.aPattern()) {
				for (char tChar : tPatternRow.toCharArray()) {
					if (tChar != ' ' && !tKey.containsKey(tChar)) tKey.put(tChar, beltIngredient(tForm, tChar));
				}
			}
			ShapedRecipeBuilder tBuilder = ShapedRecipeBuilder.shaped(tForm.aCategory(), beltResult(tForm), tForm.aCount());
			for (String tPatternRow : tForm.aPattern()) tBuilder.pattern(tPatternRow);
			for (Map.Entry<Character, Ingredient> tEntry : tKey.entrySet()) tBuilder.define(tEntry.getKey(), tEntry.getValue());
			tBuilder.unlockedBy("has_wooden_button", has(ItemTags.WOODEN_BUTTONS));
			//? if forge {
			tBuilder.save(aConsumer, tId);
			//?} else {
			/*tBuilder.save(aOutput, tId);
			*///?}
		}
	}
}
