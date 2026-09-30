package gregtech6.items.tools;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

//? if forge {
//?} else {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
*///?}

import gregtech6.itemdata.GT6ToolStats;

import gregapi.oredict.OreDictMaterial;

/**
 * The per-material tool crafting row (task dig-ladder) — the port isomorph of the
 * upstream OreProcessing_Tool rows on the toolHead prefixes (Loader_Tools.java:293-300,
 * the dig family: pickaxe :295 / construction :294 / shovel :296 / spade :297 / hoe
 * :299 / axe :300, the {@code And(ANTIMATTER.NOT, MT.Wood.NOT, COATED.NOT)} axis): a
 * vanilla SHAPED row whose RESULT carries the {@code GT.ToolStats} material identity.
 *
 * <p><b>Why a custom serializer</b> (the decisions.p24-screwdriver-result-recipe shape):
 * vanilla 1.20.1 shaped JSON rejects a result data tag outright (ShapedRecipe.java:274
 * "Disallowed data tag found"), and 1.21.1 result components cannot express the seam's
 * keyed carrier — the identity must be STAMPED at assemble time, the same face the
 * upstream {@code getToolWithStats} rows stamp through. The JSON stays the vanilla
 * shaped shape plus ONE field: {@code "material": "&lt;snake&gt;"} (the primary
 * material, resolved against the material registry at parse time — an unknown name is a
 * parse error, never a silent NULL). The secondary (handle) material stays null: the
 * upstream metal rows carry no handle letter in the grid (:295-300), only the spade's
 * wooden-stick auxiliary.
 *
 * <p>The stamped budget is {@code mToolDurability * 100 * multiplier} (MultiItemTool.java:182)
 * — every ladder axis form carries the upstream ×1.0 durability multiplier (rows omit the
 * field); the head+handle assembly rows (task 39-toolhead-assembly, the
 * AdvancedCraftingTool :332-350 port) needed the pre-declared growth: the OPTIONAL
 * {@code "multiplier"} JSON field (default 1.0) carries the item's own
 * {@code durabilityMultiplier()} face (the gem pick ×0.25) so the assembled budget stays
 * upstream-exact.
 *
 * <p>LEG-FORKED FILE (ADR-P17-1 single implementation): the 1.20.1 forge leg parses
 * the vanilla shaped JSON helpers and network-codes the vanilla fields + the material
 * name; the 1.21.1 neo leg wraps the vanilla {@code ShapedRecipePattern} codec + stream
 * codec the same way. Both legs stamp through the ONE {@link GT6ItemData} seam, so the
 * JSON (except the per-leg result dialect the datagen codecs emit natively) and the
 * semantics are byte-identical.
 */
//? if forge {
public class GT6MaterialToolRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

	private final ShapedRecipe mDelegate;
	private final OreDictMaterial mMaterial;
	/** The form durability multiplier (the :182 budget leg) — 1.0 for every ladder row, the item's own face for the assembly rows. */
	private final float mMultiplier;

	/** The ladder-row face: every grid row carries the ×1.0 multiplier (the dig card's form). */
	public GT6MaterialToolRecipe(ShapedRecipe aDelegate, OreDictMaterial aMaterial) {
		this(aDelegate, aMaterial, 1.0F);
	}

	public GT6MaterialToolRecipe(ShapedRecipe aDelegate, OreDictMaterial aMaterial, float aMultiplier) {
		mDelegate = aDelegate;
		mMaterial = aMaterial;
		mMultiplier = aMultiplier;
	}

	/** The assemble-time identity stamp (the upstream getToolWithStats face, MultiItemTool.java:180-192). */
	@Override
	public ItemStack assemble(CraftingContainer aContainer, RegistryAccess aRegistryAccess) {
		ItemStack tResult = mDelegate.assemble(aContainer, aRegistryAccess);
		GT6ToolLadder.stampIdentity(tResult, mMaterial, mMultiplier);
		return tResult;
	}

	@Override
	public boolean matches(CraftingContainer aContainer, net.minecraft.world.level.Level aLevel) {
		return mDelegate.matches(aContainer, aLevel);
	}

	@Override
	public boolean canCraftInDimensions(int aWidth, int aHeight) {
		return mDelegate.canCraftInDimensions(aWidth, aHeight);
	}

	@Override
	public ItemStack getResultItem(RegistryAccess aRegistryAccess) {
		return mDelegate.getResultItem(aRegistryAccess);
	}

	/**
	 * The recipe-viewer display face (task jei-tool-output-tint, GitHub #6 round 3): JEI
	 * renders the crafting category's output slot from {@link #getResultItem}, which stays
	 * bare BY CONTRACT (the vanilla shaped JSON carries no result tag, ShapedRecipe.java:274)
	 * — so every material row would show the identity-less Steel fallback. This accessor
	 * hands the display layer a COPY stamped with this row's identity, the same stamp
	 * {@link #assemble} applies to real crafts. {@code RegistryAccess.EMPTY} is safe here:
	 * vanilla {@code ShapedRecipe.getResultItem} ignores the parameter and returns its
	 * result field (vanilla 1.20.1 ShapedRecipe.java:72-74) — {@code assemble} leans on the
	 * same face via {@code getResultItem(...).copy()}.
	 */
	public ItemStack stampedDisplayResult() {
		ItemStack tResult = mDelegate.getResultItem(RegistryAccess.EMPTY).copy();
		GT6ToolLadder.stampIdentity(tResult, mMaterial, mMultiplier);
		return tResult;
	}

	/**
	 * The delegate pattern's width — the recipe-viewer layout seam: JEI's Forge
	 * RecipeHelper reads dimensions only off an {@code IShapedRecipe} (RecipeHelper.java:30-43),
	 * which this wrapper deliberately does not implement, so the JEI extension consumes
	 * them through this accessor instead.
	 */
	public int recipeWidth() {
		return mDelegate.getWidth();
	}

	/** The layout seam's height half — see {@link #recipeWidth()}. */
	public int recipeHeight() {
		return mDelegate.getHeight();
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer aContainer) {
		return mDelegate.getRemainingItems(aContainer);
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return mDelegate.getIngredients();
	}

	@Override
	public ResourceLocation getId() {
		return mDelegate.getId();
	}

	@Override
	public String getGroup() {
		return mDelegate.getGroup();
	}

	@Override
	public CraftingBookCategory category() {
		return mDelegate.category();
	}

	@Override
	public boolean isSpecial() {
		return mDelegate.isSpecial();
	}

	@Override
	public boolean isIncomplete() {
		return mDelegate.isIncomplete();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registration.SERIALIZER.get();
	}

	/**
	 * The registry miss is a parse error — a row for an unknown material must never load
	 * as NULL. The JSON carries the snake form (the id/tag convention); the material
	 * table keys on {@code mNameInternal} (Camel), so the snake is re-camelised first —
	 * the exact inverse of the GTMaterialItems.snakeCase the datagen writes with.
	 */
	private static OreDictMaterial resolve(String aSnake) {
		OreDictMaterial tMaterial = GT6ToolLadder.materialBySnake(aSnake);
		if (tMaterial == null || tMaterial == gregapi.data.MT.NULL) {
			throw new JsonSyntaxException("Unknown material \"" + aSnake + "\" in a gt6:material_tool row");
		}
		return tMaterial;
	}

		public static class Serializer implements RecipeSerializer<GT6MaterialToolRecipe> {

		/** The vanilla shaped parser — the parse face delegates, the JSON stays the vanilla shaped shape. */
		private static final ShapedRecipe.Serializer VANILLA = new ShapedRecipe.Serializer();

		public GT6MaterialToolRecipe fromJson(ResourceLocation aId, JsonObject aJson) {
			ShapedRecipe tDelegate = VANILLA.fromJson(aId, aJson);
			return new GT6MaterialToolRecipe(tDelegate, resolve(GsonHelper.getAsString(aJson, "material")),
					GsonHelper.getAsFloat(aJson, "multiplier", 1.0F));
		}

		public GT6MaterialToolRecipe fromNetwork(ResourceLocation aId, FriendlyByteBuf aBuffer) {
			ShapedRecipe tDelegate = VANILLA.fromNetwork(aId, aBuffer);
			return new GT6MaterialToolRecipe(tDelegate, resolve(aBuffer.readUtf()), aBuffer.readFloat());
		}

		public void toNetwork(FriendlyByteBuf aBuffer, GT6MaterialToolRecipe aRecipe) {
			VANILLA.toNetwork(aBuffer, aRecipe.mDelegate);
			aBuffer.writeUtf(aRecipe.mMaterial.mNameInternal);
			aBuffer.writeFloat(aRecipe.mMultiplier);
		}
	}

	/**
	 * The registration home — the GT6Tools self-contained card form: the serializer is
	 * the shared seam (the blade ladder reuses the SAME gt6:material_tool serializer),
	 * attached from the mod-construct event like every other gt6 DeferredRegister.
	 */
	@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
	public static class Registration {

		public static final net.minecraftforge.registries.DeferredRegister<RecipeSerializer<?>> RECIPES =
				net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, "gt6");

		public static final net.minecraftforge.registries.RegistryObject<RecipeSerializer<?>> SERIALIZER =
				RECIPES.register("material_tool", Serializer::new);

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
			RECIPES.register(net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get());
		}
	}
}
//?} else {
/*public class GT6MaterialToolRecipe extends ShapedRecipe {

	private final OreDictMaterial mMaterial;
	private final String mMaterialName;
	private final ItemStack mResult;
	// The form durability multiplier (the :182 budget leg) — 1.0 for every ladder row, the item's own face for the assembly rows.
	private final float mMultiplier;

	// The ladder-row face: every grid row carries the ×1.0 multiplier (the dig card's form).
	public GT6MaterialToolRecipe(String aGroup, CraftingBookCategory aCategory, ShapedRecipePattern aPattern,
			ItemStack aResult, boolean aShowNotification, String aMaterialName) {
		this(aGroup, aCategory, aPattern, aResult, aShowNotification, aMaterialName, 1.0F);
	}

	public GT6MaterialToolRecipe(String aGroup, CraftingBookCategory aCategory, ShapedRecipePattern aPattern,
			ItemStack aResult, boolean aShowNotification, String aMaterialName, float aMultiplier) {
		super(aGroup, aCategory, aPattern, aResult, aShowNotification);
		mMaterialName = aMaterialName;
		mResult = aResult;
		mMultiplier = aMultiplier;
		mMaterial = resolve(aMaterialName);
	}

	private GT6MaterialToolRecipe(String aGroup, CraftingBookCategory aCategory, ShapedRecipePattern aPattern,
			ItemStack aResult, boolean aShowNotification, OreDictMaterial aMaterial, String aMaterialName, float aMultiplier) {
		super(aGroup, aCategory, aPattern, aResult, aShowNotification);
		mMaterialName = aMaterialName;
		mResult = aResult;
		mMaterial = aMaterial;
		mMultiplier = aMultiplier;
	}

	// The assemble-time identity stamp (the upstream getToolWithStats face, MultiItemTool.java:180-192).
	@Override
	public ItemStack assemble(CraftingInput aInput, HolderLookup.Provider aProvider) {
		ItemStack tResult = super.assemble(aInput, aProvider);
		GT6ToolLadder.stampIdentity(tResult, mMaterial, mMultiplier);
		return tResult;
	}

	// The recipe-viewer display face (task jei-tool-output-tint): a COPY of the bare
	// result stamped with this row's identity — the display twin of assemble(). The JSON
	// result stays bare BY CONTRACT (:50-53); JEI's 19.x default dispatch reads the shaped
	// width/height straight off the ShapedRecipe superclass, so no layout accessor is
	// needed on this leg.
	public ItemStack stampedDisplayResult() {
		ItemStack tResult = mResult.copy();
		GT6ToolLadder.stampIdentity(tResult, mMaterial, mMultiplier);
		return tResult;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registration.SERIALIZER.get();
	}

	// The registry miss is a parse error — a row for an unknown material must never load as NULL.
	private static OreDictMaterial resolve(String aSnake) {
		OreDictMaterial tMaterial = GT6ToolLadder.materialBySnake(aSnake);
		if (tMaterial == null || tMaterial == gregapi.data.MT.NULL) {
			throw new JsonSyntaxException("Unknown material \"" + aSnake + "\" in a gt6:material_tool row");
		}
		return tMaterial;
	}

	public static class Serializer implements RecipeSerializer<GT6MaterialToolRecipe> {

		public static final MapCodec<GT6MaterialToolRecipe> CODEC = RecordCodecBuilder.mapCodec(aInstance -> aInstance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(aRecipe -> aRecipe.getGroup()),
				CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(aRecipe -> aRecipe.category()),
				ShapedRecipePattern.MAP_CODEC.forGetter(aRecipe -> aRecipe.pattern),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(aRecipe -> aRecipe.mResult),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(aRecipe -> aRecipe.showNotification()),
				Codec.STRING.fieldOf("material").forGetter(aRecipe -> aRecipe.mMaterialName),
				Codec.FLOAT.optionalFieldOf("multiplier", 1.0F).forGetter(aRecipe -> aRecipe.mMultiplier)
			).apply(aInstance, GT6MaterialToolRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, GT6MaterialToolRecipe> STREAM_CODEC = StreamCodec.of(
				Serializer::toNetwork, Serializer::fromNetwork);

		@Override
		public MapCodec<GT6MaterialToolRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, GT6MaterialToolRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static GT6MaterialToolRecipe fromNetwork(RegistryFriendlyByteBuf aBuffer) {
			String tGroup = aBuffer.readUtf();
			CraftingBookCategory tCategory = aBuffer.readEnum(CraftingBookCategory.class);
			ShapedRecipePattern tPattern = ShapedRecipePattern.STREAM_CODEC.decode(aBuffer);
			ItemStack tResult = ItemStack.STREAM_CODEC.decode(aBuffer);
			boolean tShowNotification = aBuffer.readBoolean();
			String tMaterialName = aBuffer.readUtf();
			float tMultiplier = aBuffer.readFloat();
			return new GT6MaterialToolRecipe(tGroup, tCategory, tPattern, tResult, tShowNotification,
					resolve(tMaterialName), tMaterialName, tMultiplier);
		}

		private static void toNetwork(RegistryFriendlyByteBuf aBuffer, GT6MaterialToolRecipe aRecipe) {
			aBuffer.writeUtf(aRecipe.getGroup());
			aBuffer.writeEnum(aRecipe.category());
			ShapedRecipePattern.STREAM_CODEC.encode(aBuffer, aRecipe.pattern);
			ItemStack.STREAM_CODEC.encode(aBuffer, aRecipe.mResult);
			aBuffer.writeBoolean(aRecipe.showNotification());
			aBuffer.writeUtf(aRecipe.mMaterial.mNameInternal);
			aBuffer.writeFloat(aRecipe.mMultiplier);
		}
	}

	// The registration home — the GT6Tools self-contained card form: the serializer is
	// the shared seam (the blade ladder reuses the SAME gt6:material_tool serializer),
	// attached from the mod-construct event like every other gt6 DeferredRegister.
	@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class Registration {

		public static final DeferredRegister<RecipeSerializer<?>> RECIPES =
				DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER.key(), "gt6");

		public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> SERIALIZER =
				RECIPES.register("material_tool", Serializer::new);

		@net.neoforged.bus.api.SubscribeEvent
		public static void onModConstruct(FMLConstructModEvent aEvent) {
			RECIPES.register(ModList.get().getModContainerById("gt6").orElseThrow().getEventBus());
		}
	}
}
*///?}
