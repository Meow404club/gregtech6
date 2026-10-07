package gregtech6.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
import net.minecraftforge.common.Tags;
//?} else {
/*import net.neoforged.neoforge.common.Tags;
*///?}

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTMaterialItems;

/**
 * The gun family crafting band (task pistol-family-items) — the upstream OreProcessing_Tool
 * arg-8 rows on the toolHeadWrench listener (Loader_Tools.java:317-319, the audit matrix
 * direct_only_arg8 trio; the port had ZERO rows — the items themselves were unregistered).
 *
 * <p><b>Why a separate provider, and why it implements DataProvider DIRECTLY</b>:
 * GT6CraftingRecipes.java is owned by the in-flight tool-arg8 card (the task FILES_SCOPE
 * zero-overlap ruling), and {@code RecipeProvider.getName()} is FINAL on both legs (1.20.1
 * sources jar :461) — a second RecipeProvider would trip the vanilla "Duplicate provider"
 * check. The GT6CrucibleDatagen.Recipes precedent: each leg's run() mirrors the vanilla
 * RecipeProvider.run shape and this class rides the SAME gt6:material_tool serializer +
 * stamped identity seam (the blade/machine band face).
 *
 * <p><b>The listener universe</b>: upstream the rows fire on the toolHeadWrench prefix
 * registration events (:317-319 all three listen there — the trigger only, the gun shapes
 * carry NO 'A' letter, the wrench head item is NOT an ingredient). The port walk rides
 * {@link GTMaterialItems#registrationOrder()} over {@code OP.toolHeadWrench} pairs — the
 * item-truth face of the same universe. The row axis is the upstream And() verbatim:
 * {@code And(ANTIMATTER.NOT, MT.Wood.NOT, BOUNCY.NOT, STRETCHY.NOT, typemin(2))} plus the
 * :426 mToolTypes&gt;0 listener gate (the BOUNCY/STRETCHY/typemin faces ride the
 * toolHeadWrench prefix condition too, OP.java:1285 {@code And(typemin(2), BOUNCY.NOT,
 * STRETCHY.NOT)} — carried explicitly here, machineLadderAxis form, idempotent with the
 * prefix fold). Steel is INCLUDED (no steel anchor exists — the blade-family ruling: the
 * stamped row IS the steel row).
 *
 * <p><b>Letters</b> (the :393-405 alphabet + the CR lowercase TOOL letters, decoded through
 * the OreProcessing_Tool constructor :406 — aSpecialObjectX=plateCurved, aSpecialObjectV=
 * Items.flint, aUseNormalHandle=T):
 * {@code X} = the material's plateCurved ITEM, {@code T} = the material's screw ITEM,
 * {@code V} = the vanilla flint item (the striker, :317-319 arg-13 {@code Items.flint}),
 * {@code H} = the wood-rod tag (the handle stick — {@code stick.dat(mHandleMaterial)},
 * the blade-family {@code #minecraft:rods_wooden} port face), {@code d}/{@code h} = the
 * screwdriver/hammer tool tags. A material-carrying letter's ITEM TRUTH miss skips the row
 * (never a ghost row — the machineLadderIngredient :4073 ruling).
 *
 * <p><b>Shapes</b> (verbatim :317-319): pistol {@code XXV/ TH/d h}, carbine
 * {@code XXV/THH/d h}, rifle {@code XXX/HHV/dTh}.
 */
public class GT6GunRecipes implements net.minecraft.data.DataProvider {

	/** One gun form: the id prefix + the upstream row shape (Loader_Tools.java:317-319 verbatim). */
	record GunForm(String aId, String[] aPattern) {
	}

	/** The :317-319 arg-8 table — upstream order, shapes verbatim. */
	static final List<GunForm> GUN_FORMS = List.of(
			new GunForm("pistol", new String[] {"XXV", " TH", "d h"}), // :317
			new GunForm("carbine", new String[] {"XXV", "THH", "d h"}), // :318
			new GunForm("rifle", new String[] {"XXX", "HHV", "dTh"})); // :319

	/** One walked gun row (the test-visible census unit). */
	record GunRow(GunForm aForm, OreDictMaterial aMaterial, String aSnake) {
	}

	/** The registry item of a form (the GT6Tools face — datagen JVM only). */
	private static Item gunItem(String aForm) {
		return switch (aForm) {
			case "pistol" -> GT6Tools.PISTOL.get();
			case "carbine" -> GT6Tools.CARBINE.get();
			case "rifle" -> GT6Tools.RIFLE.get();
			default -> throw new IllegalArgumentException("unknown gun form: " + aForm);
		};
	}

	/**
	 * The row axis — the upstream And() verbatim (OreProcessing_Tool :317-319 arg-15 + the
	 * :426 listener gate). The typemin(2)/BOUNCY/STRETCHY faces ride the toolHeadWrench
	 * prefix condition (OP.java:1285) through the pair universe; carried explicitly here.
	 */
	static boolean gunAxis(OreDictMaterial aMaterial) {
		if (aMaterial.mToolTypes < 2) return false; // the :426 listener gate + typemin(2)
		if (aMaterial.contains(TD.Atomic.ANTIMATTER)) return false; // ANTIMATTER.NOT
		if (aMaterial == MT.Wood) return false; // MT.Wood.NOT — the M5 identity form
		if (aMaterial.contains(TD.Properties.BOUNCY) || aMaterial.contains(TD.Properties.STRETCHY)) return false; // BOUNCY/STRETCHY.NOT
		return true;
	}

	/**
	 * The row letters — {@code null} = the item-truth miss (the row is skipped, never
	 * emitted with an unresolvable ingredient; the machineLadderIngredient form).
	 */
	static Ingredient gunIngredient(char aKey, OreDictMaterial aMaterial) {
		return switch (aKey) {
			case 'X' -> GTMaterialItems.get(OP.plateCurved, aMaterial) == null ? null
					: Ingredient.of(GTMaterialItems.get(OP.plateCurved, aMaterial).get());
			case 'T' -> GTMaterialItems.get(OP.screw, aMaterial) == null ? null
					: Ingredient.of(GTMaterialItems.get(OP.screw, aMaterial).get());
			case 'V' -> Ingredient.of(Items.FLINT); // the :317-319 arg-13 striker (no material axis)
			case 'H' -> Ingredient.of(Tags.Items.RODS_WOODEN); // the mHandleMaterial stick port face
			case 'd' -> Ingredient.of(GT6ItemTags.TOOLS_SCREWDRIVER);
			case 'h' -> Ingredient.of(GT6ItemTags.TOOLS_HARD_HAMMER);
			default -> throw new IllegalArgumentException("unknown gun row letter: " + aKey);
		};
	}

	/**
	 * Every material-carrying letter's ITEM TRUTH holds (the row-skip gate) — OFFLINE-PURE
	 * (the {@code isGeneratingItem} predicate, the toolHeadRowResolvable ruling: the census
	 * walk reproducible in the headless test JVM; the emission resolver's get() faces stay
	 * datagen-JVM-only).
	 */
	static boolean gunRowResolvable(GunForm aForm, OreDictMaterial aMaterial) {
		for (String tPatternRow : aForm.aPattern()) {
			for (char tChar : tPatternRow.toCharArray()) {
				if (tChar == ' ') continue; // the dead cell
				boolean tTruth = switch (tChar) {
					case 'X' -> OP.plateCurved.isGeneratingItem(aMaterial);
					case 'T' -> OP.screw.isGeneratingItem(aMaterial);
					case 'V', 'H', 'd', 'h' -> true; // the vanilla item / the rod + tool tags
					default -> throw new IllegalArgumentException("unknown gun row letter: " + tChar);
				};
				if (!tTruth) return false;
			}
		}
		return true;
	}

	/** The walk face (offline-pure, the census unit): the toolHeadWrench pair truth ∩ the axis ∩ the letter truth. */
	static List<GunRow> gunRows() {
		List<GunRow> rRows = new ArrayList<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() != OP.toolHeadWrench) continue; // the upstream listener prefix
			OreDictMaterial tMaterial = tPair.material();
			if (!gunAxis(tMaterial)) continue;
			for (GunForm tForm : GUN_FORMS) {
				if (!gunRowResolvable(tForm, tMaterial)) continue;
				rRows.add(new GunRow(tForm, tMaterial, GTMaterialItems.snakeCase(tMaterial.mNameInternal)));
			}
		}
		return rRows;
	}

	private final PackOutput mOutput;
	/** The 21.1 run feeds saveStable the registries lookup; the 1.20.1 leg ignores it. */
	private final CompletableFuture<net.minecraft.core.HolderLookup.Provider> mLookup;

	public GT6GunRecipes(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aLookup) {
		mOutput = aOutput;
		mLookup = aLookup;
	}

	/** Unique provider name (the "Duplicate provider" check — see the class javadoc). */
	@Override
	public String getName() {
		return "Recipes: gt6:guns";
	}

	//? if forge {
	@Override
	public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput aCache) {
		PackOutput.PathProvider tRecipePaths = mOutput.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
		// the unlock-advancement write stopped here (2026-10-03 user ruling, remember id1359):
		// JEI/EMI ubiquitous, the vanilla recipe book is dead weight.
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

	/** The has() helper — the one criterion shape is leg-split (the GT6CrucibleDatagen.Recipes form). */
	//? if forge {
	private static net.minecraft.advancements.CriterionTriggerInstance has(net.minecraft.world.level.ItemLike aItem) {
		return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(aItem);
	}
	//?} else {
	/*private static net.minecraft.advancements.Criterion<?> has(net.minecraft.world.level.ItemLike aItem) {
		return net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.createCriterion(
				new net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance(
						java.util.Optional.empty(),
						net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.Slots.ANY,
						java.util.List.of(net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(aItem).build())));
	}
	*///?}

	//? if forge {
	private void build(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> aConsumer) {
	//?} else {
	/*private void build(net.minecraft.data.recipes.RecipeOutput aOutput) {
	 *///?}
		for (GunRow tRow : gunRows()) {
			// the swap-table local — the bare-local form both legs read (the grassRecipeId precedent)
			String tPath = tRow.aForm().aId() + "/" + tRow.aSnake();
			ResourceLocation tId = new ResourceLocation(GT6DataGenerators.MOD_ID, tPath);
			Map<Character, Ingredient> tKey = new LinkedHashMap<>();
			List<String> tPattern = new ArrayList<>();
			for (String tPatternRow : tRow.aForm().aPattern()) {
				tPattern.add(tPatternRow);
				for (char tChar : tPatternRow.toCharArray()) {
					if (tChar != ' ' && !tKey.containsKey(tChar)) tKey.put(tChar, gunIngredient(tChar, tRow.aMaterial()));
				}
			}
			Item tPlateCurved = GTMaterialItems.get(OP.plateCurved, tRow.aMaterial()).get(); // the resolvable gate ran first
			Item tResult = gunItem(tRow.aForm().aId());
			//? if forge {
			Advancement.Builder tAdvancement = Advancement.Builder
					.recipeAdvancement()
					.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
					.addCriterion("has_plate_curved", has(tPlateCurved))
					.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
					.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
					.requirements(net.minecraft.advancements.RequirementsStrategy.OR);
			aConsumer.accept(new GunMaterialToolRow(tId, tId.withPrefix("recipes/tools/"),
					net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT, tPattern, tKey,
					tResult, tRow.aSnake(), tAdvancement));
			//?} else {
			/*gregtech6.items.tools.GT6MaterialToolRecipe tRecipe = new gregtech6.items.tools.GT6MaterialToolRecipe("",
					net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
					net.minecraft.world.item.crafting.ShapedRecipePattern.of(tKey, tPattern),
					new net.minecraft.world.item.ItemStack(tResult), true, tRow.aSnake());
			Advancement.Builder tAdvancement = Advancement.Builder
					.recipeAdvancement()
					.parent(net.minecraft.data.recipes.RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
					.addCriterion("has_plate_curved", has(tPlateCurved))
					.addCriterion("has_the_recipe", net.minecraft.advancements.critereon.RecipeUnlockedTrigger.unlocked(tId))
					.rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(tId))
					.requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR);
			aOutput.accept(tId, tRecipe, tAdvancement.build(tId.withPrefix("recipes/tools/")));
			*///?}
		}
	}

	//? if forge {
	/**
	 * The forge FinishedRecipe face — the vanilla shaped JSON (the "type" rides the
	 * registered gt6:material_tool serializer) + the ONE material field the serializer
	 * parses. The GT6CraftingRecipes.MaterialToolRow copy (that file is the tool-arg8
	 * card's; this band cannot touch it).
	 */
	private record GunMaterialToolRow(ResourceLocation aId, ResourceLocation aAdvancementId,
			net.minecraft.world.item.crafting.CraftingBookCategory aCategory, List<String> aPattern,
			Map<Character, Ingredient> aKey, Item aResult, String aMaterial, Advancement.Builder aAdvancement)
			implements net.minecraft.data.recipes.FinishedRecipe {

		@Override
		public void serializeRecipeData(com.google.gson.JsonObject aJson) {
			aJson.addProperty("category", aCategory.getSerializedName());
			com.google.gson.JsonArray tPattern = new com.google.gson.JsonArray();
			for (String tRow : aPattern) {
				tPattern.add(tRow);
			}
			aJson.add("pattern", tPattern);
			com.google.gson.JsonObject tKey = new com.google.gson.JsonObject();
			for (Map.Entry<Character, Ingredient> tEntry : aKey.entrySet()) {
				tKey.add(String.valueOf(tEntry.getKey()), tEntry.getValue().toJson());
			}
			aJson.add("key", tKey);
			com.google.gson.JsonObject tResult = new com.google.gson.JsonObject();
			tResult.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(aResult).toString());
			tResult.addProperty("count", 1);
			aJson.add("result", tResult);
			aJson.addProperty("show_notification", true);
			aJson.addProperty("material", aMaterial);
		}

		@Override
		public ResourceLocation getId() {
			return aId;
		}

		@Override
		public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
			return gregtech6.items.tools.GT6MaterialToolRecipe.Registration.SERIALIZER.get();
		}

		@Override
		@javax.annotation.Nullable
		public com.google.gson.JsonObject serializeAdvancement() {
			return aAdvancement.serializeToJson();
		}

		@Override
		@javax.annotation.Nullable
		public ResourceLocation getAdvancementId() {
			return aAdvancementId;
		}
	}
	//?}
}
