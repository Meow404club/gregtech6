package gregtech6.items.tools.loot;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.common.collect.ImmutableSet;

//? if forge {
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
//21.1: same simple names, neoforged package (the DatapackBuiltinEntriesProvider fork
//lesson — the loot subtree is NOT on the stonecutter swap table).
*///?}

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * The GT6 tool drop-conversion seam — task p29-w5-t1-dig-six shared infrastructure ①
 * (the wave ruling decisions.p30-w5-split-rulings: the upstream {@code convertBlockDrops}
 * arm lands as the Forge Global-Loot-Modifier chain — the datapack-driven, reload-safe
 * form; direct drop edits inside {@code item.mineBlock} are the card's declared RED
 * LINE). Upstream mechanics, arm by arm:
 * <ul>
 * <li><b>Spade harvestableSpade</b> (GT_Tool_Spade.java:81-89): breaking a
 *     {@link #HARVESTABLE_SPADE} block with the spade REPLACES the drops with the block
 *     item itself at forced chance ({@code dropChance = 1.0F}) — the silk-touch-like
 *     grass/mycelium/clay/snow harvest, verbatim.</li>
 * <li><b>Construction pick ender_chest</b> (GT_Tool_PickaxeConstruction.java:53-59):
 *     breaking the ender chest yields the chest itself (the vanilla table drops 8
 *     obsidian — replaced verbatim).</li>
 * <li><b>Universal spade openableCrowbar</b> (GT_Tool_UniversalSpade.java:98-115): every
 *     drop of an {@link #OPENABLE_CROWBAR} block routes through
 *     {@code RM.Unboxinator.findRecipe} — matched drops swap for the recipe outputs ×
 *     stack size. The port consumes {@link GT6RecipeMaps#UNBOXINATOR} (the W1 map, the
 *     {@code findRecipe(null, MAX_VALUE, null, null, oneDrop)} upstream call shape); the
 *     storage-block unpack rows are NOT this card's content — with the current row set
 *     the walk is an IDENTITY (no row matches, drops unchanged), and it ACTIVATES when
 *     rows land. The null/empty map guard is the identity, not an error.</li>
 * </ul>
 *
 * <p><b>Consumer contract</b> (t2/t4/t5/t6 — read before extending): a new conversion =
 * (a) a new {@link GT6ToolConvertModifier.Mode} ONLY when the drop transformation is a
 * pure function of the broken state/drops (the three modes above are the shape), else a
 * new modifier class + a serializer row under {@link #SERIALIZERS}; (b) a per-tool
 * datagen JSON gated by {@link GT6ToolHoldsCondition#holdsTool(net.minecraft.world.item.Item)}
 * (the {@link GT6ToolLootModifiersDatagen} form); (c) NEVER a new DeferredRegister —
 * both registries below are the seam. Never implement a consumer's business mode here
 * in advance (the wave ruling boundary).
 *
 * <p>Registration: self-contained {@code @EventBusSubscriber(MOD)} (the GT6Tools
 * precedent) — GT6Mod.java stays untouched. Both DeferredRegisters attach from the
 * construct event; the JSON instances themselves load per datapack reload through
 * {@code data/forge/loot_modifiers/global_loot_modifiers.json} (the forge namespace;
 * the 21.1 runtime reads {@code neoforge:} — the datagen provider writes the twin
 * index, the forge leg being the canonical producer per ADR-P17-1).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ToolLootModifiers {

	/** The serializer registry — the GTCEu-modern {@code GTGlobalLootModifiers} shape. */
	//? if forge {
	public static final DeferredRegister<Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> SERIALIZERS =
			DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "gt6");
	//?} else {
	/*public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> SERIALIZERS =
			DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "gt6");
	//21.1: the serializer registry element is a MapCodec (javap 21.1.249
	//NeoForgeRegistries$Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS), not the 1.20.1 Codec.
	*///?}

	/** The loot-condition-type registry — the vanilla built-in registry (both legs, same key). */
	public static final DeferredRegister<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> LOOT_CONDITION_TYPES =
			DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_CONDITION_TYPE, "gt6");

	/** The one conversion serializer — id {@code gt6:gt6_tool_convert} (the JSON "type" key). */
	//? if forge {
	public static final net.minecraftforge.registries.RegistryObject<Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> GT6_TOOL_CONVERT =
			SERIALIZERS.register("gt6_tool_convert", () -> GT6ToolConvertModifier.CODEC);
	//?} else {
	/*public static final net.neoforged.neoforge.registries.DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>, com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> GT6_TOOL_CONVERT =
			SERIALIZERS.register("gt6_tool_convert", () -> GT6ToolConvertModifier.CODEC);
	//21.1: the wildcard-holder form (the GT6ToolsCreativeTabTest itemIds lesson).
	*///?}

	/** The condition type — id {@code gt6:holds_tool} (the JSON "condition" key). */
	//? if forge {
	public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> HOLDS_TOOL =
			LOOT_CONDITION_TYPES.register("holds_tool", () -> GT6ToolHoldsCondition.TYPE);
	//?} else {
	/*public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType, net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> HOLDS_TOOL =
			LOOT_CONDITION_TYPES.register("holds_tool", () -> GT6ToolHoldsCondition.TYPE);
	//21.1: DeferredHolder takes BOTH the registry wide type and the element type (the
	//swap table's generic entries do not cover LootItemConditionType).
	*///?}

	/**
	 * Upstream {@code BlocksGT.harvestableSpade} (CS.java:1691 verbatim: grass, dirt,
	 * mycelium, clay, snow, gravel) — the blocks the spade harvests AS THEMSELVES. The
	 * 1.7.10 {@code Blocks.snow} is the full snow block (the layer is {@code snow_layer}),
	 * so the modern member is {@link Blocks#SNOW_BLOCK}.
	 */
	public static final ImmutableSet<Block> HARVESTABLE_SPADE = ImmutableSet.of(
			Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.MYCELIUM, Blocks.CLAY, Blocks.SNOW_BLOCK, Blocks.GRAVEL);

	/**
	 * Upstream {@code BlocksGT.openableCrowbar} (CS.java:1688 verbatim: the seven storage
	 * blocks) — the blocks whose drops the universal spade routes through the Unboxinator.
	 */
	public static final ImmutableSet<Block> OPENABLE_CROWBAR = ImmutableSet.of(
			Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.DIAMOND_BLOCK,
			Blocks.EMERALD_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.COAL_BLOCK);

	private GT6ToolLootModifiers() {
	}

	/** FMLConstructModEvent = the GT6Tools.onModConstruct shape (both DeferredRegisters, one bus). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.neoforged.bus.api.IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: the GT6Tools fork precedent verbatim.
		*///?}
		SERIALIZERS.register(tModBus);
		LOOT_CONDITION_TYPES.register(tModBus);
	}

	/**
	 * The pure conversion surface — {@link GT6ToolConvertModifier} delegates here so the
	 * offline tests pin every mode's transformation without a loot context (the
	 * GTCrowbarItem.mines static-seam ruling). MUTATES {@code aDrops} like the upstream
	 * {@code convertBlockDrops(aDrops, ...)} list contract; returns whether it fired.
	 */
	public static boolean convert(GT6ToolConvertModifier.Mode aMode, BlockState aState, List<ItemStack> aDrops) {
		if (aState == null) return false;
		switch (aMode) {
			case HARVESTABLE_SPADE -> {
				Block tBlock = aState.getBlock();
				if (!HARVESTABLE_SPADE.contains(tBlock) || tBlock.asItem() == net.minecraft.world.item.Items.AIR) return false;
				aDrops.clear(); // upstream :83
				aDrops.add(new ItemStack(tBlock)); // upstream ST.make(aBlock, 1, aMetaData) — the block item itself
				return true;
			}
			case ENDER_CHEST_SELF -> {
				if (aState.getBlock() != Blocks.ENDER_CHEST) return false;
				aDrops.clear(); // upstream :55 — the vanilla 8-obsidian drop replaced
				aDrops.add(new ItemStack(Blocks.ENDER_CHEST));
				return true;
			}
			case UNBOXINATOR_OPEN -> {
				if (!OPENABLE_CROWBAR.contains(aState.getBlock())) return false;
				if (GT6RecipeMaps.UNBOXINATOR == null) return false; // pre-init / offline — the identity guard
				List<ItemStack> tUnpacked = new java.util.ArrayList<>();
				for (ItemStack tDrop : aDrops) {
					Recipe tRecipe = GT6RecipeMaps.UNBOXINATOR.findRecipe(null, Long.MAX_VALUE, null, null,
							new ItemStack(tDrop.getItem(), 1)); // upstream ST.amount(1, drop)
					if (tRecipe == null) {
						tUnpacked.add(tDrop); // no row — the drop rides through untouched (upstream keeps it)
						continue;
					}
					int tStackSize = tDrop.getCount();
					for (int tCopies = 0; tCopies < tStackSize; tCopies++) { // upstream :104/:106 per-stack outputs
						for (ItemStack tOutput : tRecipe.getOutputs()) tUnpacked.add(tOutput.copy());
					}
				}
				aDrops.clear();
				aDrops.addAll(tUnpacked);
				return true;
			}
		}
		return false;
	}

	/**
	 * The one modifier type — serializer id {@code gt6:gt6_tool_convert}, JSON
	 * {@code {"type": "gt6:gt6_tool_convert", "conditions": [...], "mode": "..."}}; the
	 * per-tool identity rides the {@link GT6ToolHoldsCondition} entry inside
	 * {@code conditions} (the base class runs them before {@link #doApply}).
	 */
	public static class GT6ToolConvertModifier extends LootModifier {

		/** The drop-transformation modes — each delegates to {@link GT6ToolLootModifiers#convert}. */
		public enum Mode {
			/** The spade harvestableSpade self-drop conversion (upstream GT_Tool_Spade.java:81-89). */
			HARVESTABLE_SPADE,
			/** The construction-pick ender_chest self-drop conversion (upstream :53-59). */
			ENDER_CHEST_SELF,
			/** The universal-spade openableCrowbar Unboxinator walk (upstream :98-115). */
			UNBOXINATOR_OPEN
		}

		private static final Codec<Mode> MODE_CODEC = Codec.STRING.xmap(Mode::valueOf, Mode::name);

		/** The element type forks per leg — Codec (forge 1.20.1) vs MapCodec (neo 21.1). */
		//? if forge {
		public static final Codec<GT6ToolConvertModifier> CODEC =
				RecordCodecBuilder.create(aInst -> codecStart(aInst)
						.and(MODE_CODEC.fieldOf("mode").forGetter(aModifier -> aModifier.mMode))
						.apply(aInst, GT6ToolConvertModifier::new));
		//?} else {
		/*public static final com.mojang.serialization.MapCodec<GT6ToolConvertModifier> CODEC =
				RecordCodecBuilder.mapCodec(aInst -> codecStart(aInst)
						.and(MODE_CODEC.fieldOf("mode").forGetter(aModifier -> aModifier.mMode))
						.apply(aInst, GT6ToolConvertModifier::new));
		*///?}

		final Mode mMode;

		public GT6ToolConvertModifier(LootItemCondition[] aConditions, Mode aMode) {
			super(aConditions);
			mMode = aMode;
		}

		@Override
		//? if forge {
		public Codec<? extends IGlobalLootModifier> codec() {
			return CODEC;
		}
		//?} else {
		/*public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec() {
			return CODEC;
		}
		*///?}

		@Override
		protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> aLoot, LootContext aContext) {
			convert(mMode, aContext.getParamOrNull(LootContextParams.BLOCK_STATE), aLoot);
			return aLoot;
		}
	}
}
