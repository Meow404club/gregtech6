package gregtech6.items;

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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

//? if forge {
//?} else {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
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

import gregtech6.item.GT6Circuits;

/**
 * The integrated-circuit crafting rows (task p33-circuits-crafting-c) — the port isomorph
 * of the upstream ItemIntegratedCircuit self-crafting block (gregapi/item/
 * ItemIntegratedCircuit.java:58-85): the base row (:58 "GhG"/"SSS"/"GwG"), the shapeless
 * reset (:59), and the 24 configuration-programming rows (:61-85, each producing the
 * circuit at a FIXED {@code Damage} configuration 1..24).
 *
 * <p><b>Why a custom serializer</b> (the GT6MaterialToolRecipe shape): vanilla 1.20.1
 * shaped JSON rejects a result data tag outright (ShapedRecipe.itemStackFromJson :274
 * "Disallowed data tag found" — the forge patch reroutes to CraftingHelper.getItemStack
 * which DOES read "nbt", so the 1.20.1 forge leg could ride plain nbt), but 1.21.1 result
 * components carry no {@code custom_data} field syntax the vanilla codecs accept — the
 * configuration number must be STAMPED at assemble time on BOTH legs through the ONE
 * {@link GT6Circuits} seam. The JSON stays the vanilla shaped shape plus ONE field:
 * {@code "configuration": <int>} (0 = the base/reset result, 1..24 = the programmed row).
 * The shapeless reset row reuses the SAME serializer (its JSON has no pattern and carries
 * the same field, the vanilla shapeless parser shape).
 *
 * <p><b>LEG-FORKED FILE</b> (ADR-P17-1 single implementation): the 1.20.1 forge leg parses
 * the vanilla shaped helpers and network-codes the vanilla fields + the configuration int;
 * the 21.1 neo leg wraps the vanilla codecs the same way. Both legs stamp through
 * {@link GT6Circuits#applyConfigurationFace} — the payload key keeps its exact
 * {@code Damage} shape byte-for-byte (forge: the stack tag; 21.1: inside the opaque
 * CUSTOM_DATA envelope, the GT6BatteryItem/GT6Circuits carrier ruling).
 */
//? if forge {
public class GT6CircuitProgramRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

	private final ShapedRecipe mDelegate;
	private final int mConfiguration;

	public GT6CircuitProgramRecipe(ShapedRecipe aDelegate, int aConfiguration) {
		mDelegate = aDelegate;
		mConfiguration = aConfiguration;
	}

	/** The assemble-time configuration stamp (the upstream ST.make(this, 1, N) face). */
	@Override
	public ItemStack assemble(CraftingContainer aContainer, RegistryAccess aRegistryAccess) {
		ItemStack tResult = mDelegate.assemble(aContainer, aRegistryAccess);
		GT6Circuits.applyConfigurationFace(tResult, mConfiguration);
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

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer aContainer) {
		return mDelegate.getRemainingItems(aContainer);
	}

	@Override
	public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
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

	public static class Serializer implements RecipeSerializer<GT6CircuitProgramRecipe> {

		/** The vanilla parsers — the parse face delegates by the row's grid kind (the shapeless reset row :59 rides the shapeless parser). */
		private static final ShapedRecipe.Serializer SHAPED = new ShapedRecipe.Serializer();
		private static final net.minecraft.world.item.crafting.ShapelessRecipe.Serializer SHAPELESS = new net.minecraft.world.item.crafting.ShapelessRecipe.Serializer();

		public GT6CircuitProgramRecipe fromJson(ResourceLocation aId, JsonObject aJson) {
			ShapedRecipe tDelegate = aJson.has("pattern")
					? SHAPED.fromJson(aId, aJson)
					: shapelessDelegate(aId, aJson);
			return new GT6CircuitProgramRecipe(tDelegate, configuration(aJson));
		}

		/** The shapeless reset row parse — the vanilla shapeless recipe re-wrapped as a 1x1 shaped delegate (the CraftingRecipe seam; matches() over one cell = the shapeless membership face). */
		private ShapedRecipe shapelessDelegate(ResourceLocation aId, JsonObject aJson) {
			net.minecraft.world.item.crafting.ShapelessRecipe tShapeless = SHAPELESS.fromJson(aId, aJson);
			NonNullList<net.minecraft.world.item.crafting.Ingredient> tIngredients = NonNullList.withSize(1,
					tShapeless.getIngredients().isEmpty() ? net.minecraft.world.item.crafting.Ingredient.EMPTY : tShapeless.getIngredients().get(0));
			ItemStack tResult = tShapeless.assemble(null, net.minecraft.core.RegistryAccess.EMPTY);
			return new ShapedRecipe(aId, tShapeless.getGroup(), CraftingBookCategory.MISC, 1, 1, tIngredients, tResult, true);
		}

		public GT6CircuitProgramRecipe fromNetwork(ResourceLocation aId, FriendlyByteBuf aBuffer) {
			ShapedRecipe tDelegate;
			if (aBuffer.readBoolean()) {
				tDelegate = SHAPED.fromNetwork(aId, aBuffer);
			} else {
				net.minecraft.world.item.crafting.ShapelessRecipe tShapeless = SHAPELESS.fromNetwork(aId, aBuffer);
				NonNullList<net.minecraft.world.item.crafting.Ingredient> tIngredients = NonNullList.withSize(1,
						tShapeless.getIngredients().isEmpty() ? net.minecraft.world.item.crafting.Ingredient.EMPTY : tShapeless.getIngredients().get(0));
				tDelegate = new ShapedRecipe(aId, tShapeless.getGroup(), CraftingBookCategory.MISC, 1, 1, tIngredients, tShapeless.getResultItem(null), true);
			}
			return new GT6CircuitProgramRecipe(tDelegate, aBuffer.readVarInt());
		}

		public void toNetwork(FriendlyByteBuf aBuffer, GT6CircuitProgramRecipe aRecipe) {
			// the shapeless reset row networked through the shapeless serializer (the 1x1
			// single-ingredient delegate reconstructs the membership face on the client)
			ShapedRecipe tDelegate = aRecipe.mDelegate;
			net.minecraft.world.item.crafting.Ingredient tOnly = tDelegate.getIngredients().isEmpty()
					? net.minecraft.world.item.crafting.Ingredient.EMPTY : tDelegate.getIngredients().get(0);
			boolean tShaped = tDelegate.getRecipeWidth() > 1 || tDelegate.getRecipeHeight() > 1;
			aBuffer.writeBoolean(tShaped);
			if (tShaped) {
				SHAPED.toNetwork(aBuffer, tDelegate);
			} else {
				net.minecraft.world.item.crafting.ShapelessRecipe tShapeless = new net.minecraft.world.item.crafting.ShapelessRecipe(
						tDelegate.getId(), tDelegate.getGroup(), CraftingBookCategory.MISC,
						tDelegate.getResultItem(null), NonNullList.withSize(1, tOnly));
				SHAPELESS.toNetwork(aBuffer, tShapeless);
			}
			aBuffer.writeVarInt(aRecipe.mConfiguration);
		}
	}

	/** The shared int parse — a missing/negative field is a parse error, never a silent 0. */
	static int configuration(JsonObject aJson) {
		int tConfig = GsonHelper.getAsInt(aJson, "configuration");
		if (tConfig < 0 || tConfig > 255) throw new JsonSyntaxException("configuration out of [0, 255]: " + tConfig);
		return tConfig;
	}

	/**
	 * The registration home — the GT6Circuits self-contained card form (the
	 * GT6MaterialToolRecipe.Registration shape): attached from the mod-construct event.
	 */
	@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
	public static class Registration {

		public static final net.minecraftforge.registries.DeferredRegister<RecipeSerializer<?>> RECIPES =
				net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, "gt6");

		public static final net.minecraftforge.registries.RegistryObject<RecipeSerializer<?>> SERIALIZER =
				RECIPES.register("circuit_program", Serializer::new);

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
			RECIPES.register(net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get());
		}
	}
}
//?} else {
/*public class GT6CircuitProgramRecipe extends ShapedRecipe {

	private final int mConfiguration;
	private final ItemStack mResult;

	public GT6CircuitProgramRecipe(String aGroup, CraftingBookCategory aCategory, ShapedRecipePattern aPattern,
			ItemStack aResult, boolean aShowNotification, int aConfiguration) {
		super(aGroup, aCategory, aPattern, aResult, aShowNotification);
		mResult = aResult;
		mConfiguration = aConfiguration;
	}

	// The assemble-time configuration stamp (the upstream ST.make(this, 1, N) face).
	@Override
	public ItemStack assemble(CraftingInput aInput, HolderLookup.Provider aProvider) {
		ItemStack tResult = super.assemble(aInput, aProvider);
		GT6Circuits.applyConfigurationFace(tResult, mConfiguration);
		return tResult;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registration.SERIALIZER.get();
	}

	public static class Serializer implements RecipeSerializer<GT6CircuitProgramRecipe> {

		public static final MapCodec<GT6CircuitProgramRecipe> CODEC = RecordCodecBuilder.mapCodec(aInstance -> aInstance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(aRecipe -> aRecipe.getGroup()),
				CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(aRecipe -> aRecipe.category()),
				ShapedRecipePattern.MAP_CODEC.forGetter(aRecipe -> aRecipe.pattern),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(aRecipe -> aRecipe.mResult),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(aRecipe -> aRecipe.showNotification()),
				Codec.intRange(0, 255).fieldOf("configuration").forGetter(aRecipe -> aRecipe.mConfiguration)
			).apply(aInstance, GT6CircuitProgramRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, GT6CircuitProgramRecipe> STREAM_CODEC = StreamCodec.of(
				Serializer::toNetwork, Serializer::fromNetwork);

		@Override
		public MapCodec<GT6CircuitProgramRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, GT6CircuitProgramRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static GT6CircuitProgramRecipe fromNetwork(RegistryFriendlyByteBuf aBuffer) {
			String tGroup = aBuffer.readUtf();
			CraftingBookCategory tCategory = aBuffer.readEnum(CraftingBookCategory.class);
			ShapedRecipePattern tPattern = ShapedRecipePattern.STREAM_CODEC.decode(aBuffer);
			ItemStack tResult = ItemStack.STREAM_CODEC.decode(aBuffer);
			boolean tShowNotification = aBuffer.readBoolean();
			int tConfig = aBuffer.readVarInt();
			return new GT6CircuitProgramRecipe(tGroup, tCategory, tPattern, tResult, tShowNotification, tConfig);
		}

		private static void toNetwork(RegistryFriendlyByteBuf aBuffer, GT6CircuitProgramRecipe aRecipe) {
			aBuffer.writeUtf(aRecipe.getGroup());
			aBuffer.writeEnum(aRecipe.category());
			ShapedRecipePattern.STREAM_CODEC.encode(aBuffer, aRecipe.pattern);
			ItemStack.STREAM_CODEC.encode(aBuffer, aRecipe.mResult);
			aBuffer.writeBoolean(aRecipe.showNotification());
			aBuffer.writeVarInt(aRecipe.mConfiguration);
		}
	}

	// The registration home — the GT6MaterialToolRecipe.Registration shape.
	@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class Registration {

		public static final DeferredRegister<RecipeSerializer<?>> RECIPES =
				DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER.key(), "gt6");

		public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> SERIALIZER =
				RECIPES.register("circuit_program", Serializer::new);

		@net.neoforged.bus.api.SubscribeEvent
		public static void onModConstruct(FMLConstructModEvent aEvent) {
			RECIPES.register(ModList.get().getModContainerById("gt6").orElseThrow().getEventBus());
		}
	}
}
*///?}
