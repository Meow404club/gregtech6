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

import gregtech6.items.tools.GTBranchCutterItem;
import gregtech6.items.tools.GTSenseItem;
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

	/**
	 * The tree-fell serializer row — id {@code gt6:gt6_tree_fell} (task p29-w5-t2-blade-six,
	 * the documented consumer contract's "new modifier class + a serializer row under
	 * SERIALIZERS" path: the whole-tree felling needs the loot ORIGIN/entity context the
	 * pure convert() switch does not carry).
	 */
	//? if forge {
	public static final net.minecraftforge.registries.RegistryObject<Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> GT6_TREE_FELL =
			SERIALIZERS.register("gt6_tree_fell", () -> GT6TreeFellModifier.CODEC);
	//?} else {
	/*public static final net.neoforged.neoforge.registries.DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>, com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> GT6_TREE_FELL =
			SERIALIZERS.register("gt6_tree_fell", () -> GT6TreeFellModifier.CODEC);
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

	/**
	 * The club rock-crush mapping table — the prefix x material pair each block crushes
	 * into (task p29-w5-t2-blade-six; the card ACCEPTANCE "club rockGt 映射表纯函数" —
	 * the KEY SET is the offline-pinnable face, the handle resolution is the live RCON
	 * face: the gt6 OP/MT handles are runtime {@code init()}-filled, so the record holds
	 * SUPPLIERS resolved at conversion time through {@code GTMaterialItems.get}).
	 * Upstream GT_Tool_Club.convertBlockDrops :61-110 minus the mod arms (NeLi/NePl/BOTA/
	 * GaSu → the vanilla basalt/blackstone pairs, {@code BlockStones.JUSTSTONE} → the W6
	 * domain, the {@code oreRedstone} oredict arm → the vanilla ore block):
	 * the stone family → rockGt Stone (:64-68), the nether-brick family (:69-73),
	 * netherrack (:74-78), end stone (:79-83), obsidian (:84-88), basalt (:89-93),
	 * blackstone (:94-98), and the redstone ore → the Cinnabar gemChipped (:104-108).
	 */
	public static final java.util.Map<Block, CrushTarget> ROCK_CRUSH = buildRockCrush();

	/** The crush-table entry (suppliers — the gt6 handles fill at mod-construct init). */
	public record CrushTarget(java.util.function.Supplier<gregapi.oredict.OreDictPrefix> prefix,
			java.util.function.Supplier<gregapi.oredict.OreDictMaterial> material) {
	}

	private static java.util.Map<Block, CrushTarget> buildRockCrush() {
		java.util.Map<Block, CrushTarget> tMap = new java.util.HashMap<>();
		CrushTarget tStone = new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.Stone);
		for (Block tBlock : new Block[] {Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
				Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.COBBLESTONE_WALL,
				Blocks.STONE_BUTTON, Blocks.STONE_PRESSURE_PLATE}) {
			tMap.put(tBlock, tStone); // upstream :64
		}
		CrushTarget tNetherBrick = new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.NetherBrick);
		for (Block tBlock : new Block[] {Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_FENCE}) {
			tMap.put(tBlock, tNetherBrick); // upstream :69
		}
		tMap.put(Blocks.NETHERRACK, new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.Netherrack)); // :74
		tMap.put(Blocks.END_STONE, new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.Endstone)); // :79
		tMap.put(Blocks.OBSIDIAN, new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.Obsidian)); // :84
		tMap.put(Blocks.BASALT, new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.STONES.Basalt)); // :89
		tMap.put(Blocks.POLISHED_BASALT, tMap.get(Blocks.BASALT)); // the polished unfold
		CrushTarget tBlackstone = new CrushTarget(() -> gregapi.data.OP.rockGt, () -> gregapi.data.MT.STONES.Blackstone);
		for (Block tBlock : new Block[] {Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
				Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE,
				Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS}) {
			tMap.put(tBlock, tBlackstone); // upstream :94
		}
		tMap.put(Blocks.REDSTONE_ORE, new CrushTarget(() -> gregapi.data.OP.gemChipped, () -> gregapi.data.MT.OREMATS.Cinnabar)); // :104
		return java.util.Collections.unmodifiableMap(tMap);
	}

	/**
	 * The shear-plant self-drop set (task p29-w5-t5-scene-six): the vine arm of
	 * GT_Tool_Scissors.convertBlockDrops (:87-101 — the cleared-drops + vine self
	 * replacement, verbatim) and the shears-class cobweb/vine full-drop face of the scoop
	 * (the research-ammunition declarative mapping; the vanilla loot match_tool predicate
	 * is the bare-shears ITEM identity, ShearsItem {@code Items.SHEARS} — it cannot see
	 * either GT tool, so this seam IS the drop path for both).
	 */
	public static final ImmutableSet<Block> PLANT_SELF = ImmutableSet.of(Blocks.VINE, Blocks.COBWEB);

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
			case SWORD_HARVEST -> {
				// upstream harvestGrass :111-146 — the 1.7.10 tallgrass meta 1/2 pair and the
				// double_plant meta 2/3 pair, unfolded onto the four modern plant blocks
				Block tBlock = aState.getBlock();
				//? if forge {
				Block tSelfGrass = Blocks.GRASS;
				//?} else {
				/*Block tSelfGrass = Blocks.SHORT_GRASS; // 21.1: GRASS renamed SHORT_GRASS (the 1.20.3 rename)
				*///?}
				if (tBlock == tSelfGrass || tBlock == Blocks.FERN) {
					aDrops.add(new ItemStack(tBlock)); // the plant's own item, :114 count 1+nextInt(1+fortune) = 1
					return true;
				}
				if (tBlock == Blocks.TALL_GRASS || tBlock == Blocks.LARGE_FERN) {
					aDrops.add(new ItemStack(tBlock, 2)); // :120 count 2+... = 2 at zero fortune
					return true;
				}
				if (tBlock == Blocks.DEAD_BUSH) { // upstream harvestStick :161-164
					aDrops.add(new ItemStack(net.minecraft.world.item.Items.STICK, 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(2)));
					return true;
				}
				if (tBlock == Blocks.VINE) { // upstream :98-101 — the vanilla empty table filled with the vine
					aDrops.clear();
					aDrops.add(new ItemStack(Blocks.VINE));
					return true;
				}
				return false;
			}
			case CLUB_ROCK_CRUSH -> {
				// upstream :62-63 — the single drop's block keys first (stone breaks into a
				// cobblestone drop; the conversion must still crush), else the state block
				Block tKey = null;
				if (aDrops.size() == 1 && aDrops.get(0).getItem() instanceof net.minecraft.world.item.BlockItem tBlockItem
						&& ROCK_CRUSH.containsKey(tBlockItem.getBlock())) {
					tKey = tBlockItem.getBlock();
				} else if (ROCK_CRUSH.containsKey(aState.getBlock())) {
					tKey = aState.getBlock();
				}
				if (tKey == null) return false;
				CrushTarget tTarget = ROCK_CRUSH.get(tKey);
				//? if forge {
				net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tRock = gregtech6.registry.GTMaterialItems.get(tTarget.prefix().get(), tTarget.material().get());
				//?} else {
				/*net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> tRock = gregtech6.registry.GTMaterialItems.get(tTarget.prefix().get(), tTarget.material().get());
				*///?}
				if (tTarget.prefix().get() == null || tTarget.material().get() == null) return false; // pre-init — the identity guard
				if (tRock == null) return false; // off-table pair — the identity guard
				//? if forge {
				if (!tRock.isPresent()) return false; // off-registry — the identity guard
				//?} else {
				/*if (!tRock.isBound()) return false; // 21.1: DeferredHolder binding read
				*///?}
				aDrops.clear(); // upstream :65 — the drops replaced wholesale
				aDrops.add(new ItemStack(tRock.get(), 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(4))); // :66
				return true;
			}
			case SENSE_VEGETAL -> {
				return GTSenseItem.convertVegetal(aState, aDrops); // task p29-w5-t4-field-five — the grass/fern self-drop + the dead-bush stick
			}
			case PLANT_SELF_DROP -> {
				Block tBlock = aState.getBlock();
				if (!PLANT_SELF.contains(tBlock) || tBlock.asItem() == net.minecraft.world.item.Items.AIR) return false;
				aDrops.clear(); // upstream GT_Tool_Scissors.java:89 — the cleared-drops contract
				aDrops.add(new ItemStack(tBlock)); // :90 — the vine self replacement, verbatim shape
			case JACKHAMMER_ROCKS -> {
				net.minecraft.world.item.Item tRock = rockItem(aState.getBlock());
				if (tRock == null) return false; // not a rock-family block — the drops ride through
				aDrops.clear(); // upstream convertBlockDrops :96 — the whole-block arm replaces the drops
				aDrops.add(new ItemStack(tRock, ROCK_COUNT)); // rockGt x4, the RM.pack :152 column
				return true;
			}
		}
		return false;
	}

	/** The rockGt drop count — the upstream hammer rows ({@code rockGt.mat(mMaterial, 4)}). */
	public static final int ROCK_COUNT = 4;

	/** The rock-family mapping table — the block → the {@code rock_gt_*} material segment. */
	public static final java.util.Map<Block, String> ROCK_MATERIALS = java.util.Map.of(
			Blocks.STONE, "stone", Blocks.COBBLESTONE, "stone", Blocks.MOSSY_COBBLESTONE, "stone",
			Blocks.GRANITE, "granite", Blocks.DIORITE, "diorite", Blocks.ANDESITE, "andesite",
			Blocks.NETHERRACK, "netherrack", Blocks.END_STONE, "endstone");

	/**
	 * The rock-family mapping — task p29-w5-t6-electric-nineteen (the jackhammer "Breaks
	 * Rocks into pieces" conversion). The upstream face routed drops through the
	 * {@code RM.Hammer} map; the card rules the port arm a PURE FUNCTION (no RM map): the
	 * vanilla rock-family blocks map onto the registered {@code gt6:rock_gt_*} items, the
	 * RM.pack :152-154 columns verbatim (cobblestone→Stone) plus the BlockStones
	 * :321 family fold (stone/granite/diorite/andesite→their material). Returns the
	 * resolved ITEM (registry lookup, null-safe for the offline JVM) or null when the
	 * block is not a rock — the resolver seam the offline tests pin through the ID table.
	 */
	public static net.minecraft.world.item.Item rockItem(Block aBlock) {
		String tMaterial = ROCK_MATERIALS.get(aBlock);
		if (tMaterial == null) return null;
		//? if forge {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				new net.minecraft.resources.ResourceLocation("gt6", "rock_gt_" + tMaterial));
		//?} else {
		/*return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "rock_gt_" + tMaterial));
		*///?}
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
		/**
		 * The universal-spade openableCrowbar Unboxinator walk (upstream :98-115).
		 */
		UNBOXINATOR_OPEN,
		/**
		 * The sword grass/stick/vine harvest (task p29-w5-t2-blade-six; upstream
		 * GT_Tool_Sword.convertBlockDrops :94-105 over the harvestGrass :111-146 /
		 * harvestStick :148-176 ToolStats helpers): the grass family (the 1.7.10
		 * tallgrass 1/2 + double_plant 2/3 pairs, the flattening unfolded) ADDS the
		 * plant's own item at the upstream counts (1 / 2 at zero fortune — the vanilla
		 * seeds ride untouched), the dead bush ADDS sticks 1-2 (:161-163), the vine is
		 * REPLACED by itself (:98-101, the vanilla no-shears empty table filled).
		 */
		SWORD_HARVEST,
		/**
		 * The club rockGt crush (task p29-w5-t2-blade-six; upstream GT_Tool_Club
		 * :61-110): the {@link GT6ToolLootModifiers#ROCK_CRUSH} block (or the single
		 * drop's block — the :62-63 face that catches the stone→cobblestone drop)
		 * REPLACES the drops with 1-4 of the crushed material item (the :66
		 * {@code 1+RNGSUS.nextInt(4)} row).
		 */
		CLUB_ROCK_CRUSH,
		/**
		 * The sense grass/fern self-drop + dead-bush stick conversion (task
		 * p29-w5-t4-field-five; upstream harvestGrass/harvestStick, ToolStats.java:111-176) —
		 * pure, rides {@link GT6ToolLootModifiers#convert} →
		 * {@code GTSenseItem.convertVegetal}.
		 */
		SENSE_VEGETAL,
		/**
		 * The branch-cutter leaves→sapling/apple conversion (task p29-w5-t4-field-five;
		 * upstream GT_Tool_BranchCutter.java:84-92). NOT a pure convert-mode member —
		 * the apple arm reads the tool's fortune enchantment + the loot random, so the
		 * dispatch lives on {@link #doApply} over {@code GTBranchCutterItem.convertLeaves}.
		 */
		BRANCHCUTTER_LEAVES,
		/** The scissors/scoop vine+cobweb self-drop (task p29-w5-t5-scene-six, GT_Tool_Scissors.java:87-101). */
		PLANT_SELF_DROP
		/** The universal-spade openableCrowbar Unboxinator walk (upstream :98-115). */
		UNBOXINATOR_OPEN,
		/** The jackhammer rockGt conversion (upstream GT_Tool_JackHammer_HV convertBlockDrops — the pure-function ruling). */
		JACKHAMMER_ROCKS
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
			BlockState tState = aContext.getParamOrNull(LootContextParams.BLOCK_STATE);
			if (mMode == GT6ToolConvertModifier.Mode.BRANCHCUTTER_LEAVES) {
				// the apple arm reads the tool's fortune + the loot random (upstream :83/:86 carried
				// both through the HarvestDropsEvent; the 1.20.1 context has no fortune param — the
				// TOOL stack is the carrier, the 1.7.10-identical read)
				ItemStack tTool = aContext.getParamOrNull(LootContextParams.TOOL);
				int tFortune = 0;
				//? if forge {
				tFortune = tTool == null ? 0 : tTool.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE);
				//?} else {
				/*tFortune = tTool == null ? 0 : tTool.getEnchantmentLevel(aContext.getLevel().holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE));
				//21.1: the enchantment argument went Holder (Enchantments.FORTUNE = ResourceKey →
				//holderOrThrow; javap 21.1.249 Enchantments/ItemStack).
				*///?}
				GTBranchCutterItem.convertLeaves(tState, aLoot, tFortune, aContext.getRandom());
				return aLoot;
			}
			convert(mMode, tState, aLoot);
			return aLoot;
		}
	}
}
