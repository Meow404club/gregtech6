package gregtech6.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.tools.GTKitchenBlock;
import gregtech6.tileentity.tools.GT6BathingPotBlockEntity;
import gregtech6.tileentity.tools.GT6JuicerBlockEntity;
import gregtech6.tileentity.tools.GT6MixingBowlBlockEntity;

/**
 * The kitchen family registration (task p26-kitchen-pot-bowl) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GTBarrels shape (ADR-P3-4; a separate class keeps the wave-4 card scopes
 * disjoint). Ports the "Misc Tool Blocks" kitchen rows of
 * Loader_MultiTileEntities.java:2173-2178 (the pot pair + the bowl; the Table variants
 * :2174/:2176/:2178 and the Mortar/Juicer/Grindstone/SiftingTable neighbours :2179+ are
 * the pool cuts of the research split).
 *
 * <p>Rows (upstream NBT → the block carrier):
 * <ul>
 * <li>Wooden Bathing Pot (:2173; carrier MT.WoodTreated, the deviation below) — RM.Bath, 4000 L, hardness 1.0 / resistance
 *     5.0, NBT_FLAMMABILITY 100 (recorded, no fire bridge — the pool);</li>
 * <li>Bathing Pot (:2175) — MT.StainlessSteel, RM.Bath, 8000 L, 1.0/6.0;</li>
 * <li>Ceramic Bowl (:2177) — MT.Ceramic, RM.Mixer, 8000 L, 1.0/5.0 — plus the
 *     {@code clay_bowl} raw item (upstream MultiItemRandomTools.java:119 "Clay Bowl",
 *     the {@code RM.add_smelting(Raw → Bowl)} hardening line rides the smelting
 *     datagen).</li>
 * </ul>
 * The wood row rides {@code MT.WoodTreated} as the melt-door representative — the
 * verbatim -100 K door reads ONLY {@code mMeltingPoint}, and the port's wood-family
 * heat传导 leaves {@code ANY.Wood} at mp 400 (water, 300 K, would trip 300 &gt;= 400-100;
 * the upstream ANY.Wood default 1000 passes) — the class-doc deviation on the row below.
 *
 * <p>GUI: none — the upstream tooltip is {@code LH.NO_GUI_CLICK_TO_INTERACT} and the
 * wave4 GUI ruling binds menu-less carriers (zero new MenuType). Rendering: the #7
 * element-model wave drew the sub-cube hollow tubs (the mDisplay fluid surface stays the
 * pool cut); task p38-c3-kitchen-tint-shape closed the render double-gap — the
 * {@link #kitchenProperties} seam rides {@code .noOcclusion().isViewBlocking(never)} (the
 * #9 wire seam form: the sub-cube quads over a canOcclude block culled neighbour faces),
 * the carriers mount the upstream collision-pool shapes, and the tintindex-0 faces resolve
 * the row material colour through {@link #paintableBlockArray()} (the #8 census convention,
 * feeding the baked world tint and the inventory ItemColor).
 *
 * <p>Task p27-lang-fix — the CREATIVE-TAB RETIREMENT: the standalone "kitchen" tab
 * ({@code gt6:kitchen}, the former "Misc Tool Blocks" category face) is retired per the
 * user ruling — the pot/bowl rows are processing machines, not cookware, and NO renamed
 * successor tab was kept; the four family items join {@code GTMachines.MACHINES_TAB}
 * through {@link #onBuildTabContents} (the GTGrassBlocks event seam). The
 * {@code itemGroup.gt6.kitchen} lang key retired with it on both locales; the block
 * display names keep their dump-verbatim zh faces.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Kitchen {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The shared kitchen-family properties seam (task p38-c3-kitchen-tint-shape, the
	 * {@code GTWires.wireProperties} per-file seam form): the ONE chain every registration
	 * point builds from. The sub-cube element models over a true {@code canOcclude} make
	 * the occlusion culling treat the vessel cell as a full cube — neighbour faces get
	 * culled against the empty cavity (the #9 X-ray) and the selection box rides the
	 * full-cube default (the #1 oversize) — so the chain carries
	 * {@code .noOcclusion().isViewBlocking(never)}; {@code isViewBlocking} is the
	 * GT6TreeLeavesBlock fog-only rider.
	 *
	 * @param aSound the upstream aUtil column (WOOD pot / METAL steel pot / STONE bowl+juicer)
	 * @param aResistance the upstream NBT_RESISTANCE column (5.0 / 6.0 / 5.0; hardness is 1.0 on every row)
	 */
	static BlockBehaviour.Properties kitchenProperties(SoundType aSound, float aResistance) {
		return BlockBehaviour.Properties.of()
				.strength(1.0F, aResistance).sound(aSound) // upstream NBT_HARDNESS 1.0 / NBT_RESISTANCE
				.noOcclusion().isViewBlocking(GT6Kitchen::never);
	}

	/** The fog-only rider (the GTWires seam body): the sub-cube vessel never blocks the view. */
	private static boolean never(net.minecraft.world.level.block.state.BlockState aState,
			net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos) {
		return false;
	}

	/**
	 * The wood pot — RM.Bath, 4000 L (upstream :2173; flammability 100 recorded on the
	 * carrier javadoc). DECLARED CARRIER DEVIATION: the upstream row's NBT_MATERIAL is
	 * {@code ANY.Wood}, whose UPSTREAM default mMeltingPoint is the plain-material 1000 —
	 * the -100 K melt-door read passes water (300 &lt; 900). In the PORT universe the
	 * {@code ANY.Wood} representative carries the wood() factory heat传导 (mp 400, the
	 * MT.java:809 {@code .heat(400, 500)} family row — live-proven: the RCON fill REJECTS
	 * water through 300 &gt;= 400-100), and EVERY {@code wood()} family member shares that
	 * 400. {@code MT.WoodTreated} (:2084, mp 500) is the only wood-family carrier where
	 * the verbatim door admits water (300 &lt; 500-100) — so it rides as the melt-door
	 * representative (the door reads ONLY mMeltingPoint; the pot is built from untreated
	 * planks upstream, this is the melt-door equivalence, not a recipe/material claim).
	 */
	public static final RegistryObject<GTKitchenBlock> BATHING_POT_WOOD = BLOCKS.register("bathing_pot_wood",
			() -> new GTKitchenBlock(4000, () -> MT.WoodTreated, GTKitchenBlock.SHAPE_TUB,
					() -> GT6Kitchen.BATHING_POT_BE.get(), kitchenProperties(SoundType.WOOD, 5.0F)));

	/** The steel pot — MT.StainlessSteel, RM.Bath, 8000 L (upstream :2175). */
	public static final RegistryObject<GTKitchenBlock> BATHING_POT_STEEL = BLOCKS.register("bathing_pot_steel",
			() -> new GTKitchenBlock(8000, () -> MT.StainlessSteel, GTKitchenBlock.SHAPE_TUB,
					() -> GT6Kitchen.BATHING_POT_BE.get(), kitchenProperties(SoundType.METAL, 6.0F)));

	/** The ceramic bowl — MT.Ceramic, RM.Mixer, 8000 L (upstream :2177). */
	public static final RegistryObject<GTKitchenBlock> MIXING_BOWL = BLOCKS.register("mixing_bowl",
			() -> new GTKitchenBlock(8000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_TUB,
					() -> GT6Kitchen.MIXING_BOWL_BE.get(), kitchenProperties(SoundType.STONE, 5.0F)));

	/**
	 * The Juicer — MT.Ceramic, RM.Juicer (upstream Loader_MultiTileEntities.java:2184,
	 * id 32722, aUtilStone, hardness 1.0 / resistance 5.0; task p33-food-machines-kitchen).
	 * The tank capacity rides the upstream :75 per-tank litres ({@code new
	 * FluidTankGT(1000000)}) — 1000000 L through the carrier (the p26 family reads the
	 * carrier capacity for every tank, so the carrier carries the Juicer's own shape, not
	 * the pot/bowl 4000/8000).
	 */
	public static final RegistryObject<GTKitchenBlock> JUICER = BLOCKS.register("juicer",
			() -> new GTKitchenBlock(1000000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_JUICER,
					() -> GT6Kitchen.JUICER_BE.get(), kitchenProperties(SoundType.STONE, 5.0F)));

	/**
	 * The pot BET — the shared-BET multi-mount (ADR-P3-1): one BE class over the wood +
	 * steel rows (the upstream wood/steel class split carried no behavioural difference
	 * beyond the registration NBT the carrier now holds).
	 */
	public static final RegistryObject<BlockEntityType<GT6BathingPotBlockEntity>> BATHING_POT_BE =
			BLOCK_ENTITY_TYPES.register("bathing_pot", () -> BlockEntityType.Builder.of(
					GT6BathingPotBlockEntity::new, BATHING_POT_WOOD.get(), BATHING_POT_STEEL.get()).build(null));

	/** The bowl BET — one BlockEntityType over the ceramic bowl. */
	public static final RegistryObject<BlockEntityType<GT6MixingBowlBlockEntity>> MIXING_BOWL_BE =
			BLOCK_ENTITY_TYPES.register("mixing_bowl", () -> BlockEntityType.Builder.of(
					GT6MixingBowlBlockEntity::new, MIXING_BOWL.get()).build(null));

	/** The Juicer BET — one BlockEntityType over the ceramic Juicer (task p33-food-machines-kitchen). */
	public static final RegistryObject<BlockEntityType<GT6JuicerBlockEntity>> JUICER_BE =
			BLOCK_ENTITY_TYPES.register("juicer", () -> BlockEntityType.Builder.of(
					GT6JuicerBlockEntity::new, JUICER.get()).build(null));

	/** The pot items (plain BlockItems — the family ships no item-capability face, the manual block has no bucket-item form upstream). */
	public static final RegistryObject<Item> BATHING_POT_WOOD_ITEM = ITEMS.register("bathing_pot_wood",
			() -> new BlockItem(BATHING_POT_WOOD.get(), new Item.Properties()));
	public static final RegistryObject<Item> BATHING_POT_STEEL_ITEM = ITEMS.register("bathing_pot_steel",
			() -> new BlockItem(BATHING_POT_STEEL.get(), new Item.Properties()));
	public static final RegistryObject<Item> MIXING_BOWL_ITEM = ITEMS.register("mixing_bowl",
			() -> new BlockItem(MIXING_BOWL.get(), new Item.Properties()));
	public static final RegistryObject<Item> JUICER_ITEM = ITEMS.register("juicer",
			() -> new BlockItem(JUICER.get(), new Item.Properties()));

	/**
	 * The Clay Bowl raw item — upstream MultiItemRandomTools.java:119 ("Clay Bowl", "Put
	 * in Furnace to harden", {@code OreDictItemData(MT.Clay, U*5)}); the vanilla smelting
	 * JSON hardens it into {@link #MIXING_BOWL_ITEM} (the :2177
	 * {@code RM.add_smelting(IL.Ceramic_Bowl_Raw, IL.Ceramic_Bowl)} line, tier-a
	 * datagen). The reverse :119 shapeless line (bowl → 5 clay balls) rides the crafting
	 * datagen too.
	 */
	public static final RegistryObject<Item> CLAY_BOWL_RAW = ITEMS.register("clay_bowl",
			() -> new Item(new Item.Properties()));

	/**
	 * The MACHINES-TAB join (task p27-lang-fix — the kitchen-tab retirement): the user
	 * ruling retired the standalone kitchen tab WITHOUT a renamed successor (the pot/bowl
	 * rows are processing machines, not cookware — "no kitchen-machines tab"), so the four
	 * family items ride the machines tab through the same event seam the grass family uses
	 * for BUILDING_BLOCKS (GTGrassBlocks.onBuildTabContents, the BuildCreativeModeTabContents
	 * fork form). Key-face comparison via location()/getId() — the two faces agree across
	 * both registration universes without relying on ResourceKey interning. Zero edits
	 * inside GTMachines.java (the tint-queue card owns that file; this join keeps the
	 * card scopes disjoint).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(BATHING_POT_WOOD_ITEM.get()));
			aEvent.accept(new ItemStack(BATHING_POT_STEEL_ITEM.get()));
			aEvent.accept(new ItemStack(MIXING_BOWL_ITEM.get()));
			aEvent.accept(new ItemStack(JUICER_ITEM.get()));
			aEvent.accept(new ItemStack(CLAY_BOWL_RAW.get()));
		}
	}

	private GT6Kitchen() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GTBarrels.onModConstruct fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GTBarrels fork form.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
		// task p27-lang-fix: the kitchen CREATIVE_MODE_TABS register retired with the tab —
		// the family items join GTMachines.MACHINES_TAB via onBuildTabContents instead.
	}

	/** Registration smoke evidence (the GTBarrels.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 kitchen registered: {} {} L / {} {} L / {} {} L / {} {} L + {} (RM.Bath pot pair + RM.Mixer bowl + RM.Juicer juicer)",
					ForgeRegistries.BLOCKS.getKey(BATHING_POT_WOOD.get()), BATHING_POT_WOOD.get().capacityL(),
					ForgeRegistries.BLOCKS.getKey(BATHING_POT_STEEL.get()), BATHING_POT_STEEL.get().capacityL(),
					ForgeRegistries.BLOCKS.getKey(MIXING_BOWL.get()), MIXING_BOWL.get().capacityL(),
					ForgeRegistries.BLOCKS.getKey(JUICER.get()), JUICER.get().capacityL(),
					ForgeRegistries.ITEMS.getKey(CLAY_BOWL_RAW.get()));
		});
	}

	/** The material accessor seam (the offline tests pin the melt-door read through it). */
	public static long meltingPointOf(Supplier<OreDictMaterial> aMaterial) {
		return aMaterial.get().mMeltingPoint;
	}

	/**
	 * The kitchen-family paint-tint walker (task p38-c3-kitchen-tint-shape, the
	 * {@code GTMultiBlocks.partPaintableBlockArray} census convention): the four blocks
	 * whose datagen models carry tintindex 0 on every face (the #7 reservation). Feeding
	 * BOTH consumption halves — the baked world tint ({@code GTMachineTintModel}, the p32
	 * route) and the inventory {@code ItemColor} — the row materials render through the
	 * {@code GTKitchenBlock.materialOf} dispatch (WoodTreated / StainlessSteel /
	 * Ceramic / Ceramic, the carrier rows). Client-side call time only.
	 */
	public static Block[] paintableBlockArray() {
		return new Block[] { BATHING_POT_WOOD.get(), BATHING_POT_STEEL.get(), MIXING_BOWL.get(), JUICER.get() };
	}
}
