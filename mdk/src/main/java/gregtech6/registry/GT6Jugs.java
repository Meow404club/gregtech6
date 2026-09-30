package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.block.tank.GT6JugBlock;
import gregtech6.item.GT6JugBlockItem;
import gregtech6.tileentity.tank.GT6JugBlockEntity;

/**
 * The Ceramic Jug registration (task small-tank-jug) — the ADR-P3-4 self-contained
 * listener form (the GT6Cups shape). Ports the "Fluid Containers" row
 * Loader_MultiTileEntities.java:2095 verbatim: "Ceramic Jug", id 32740, hardness 0.5 /
 * resistance 6.0, aUtilStone, NBT_TANK_CAPACITY 2000, NBT_LIQUIDPROOF T, NBT_GASPROOF F,
 * NBT_MAGICPROOF F, NBT_TEMPERATURE = the Ceramic melting point, stack 16. The SECOND
 * concrete of the small-tank BE base {@code TileEntityBase10FluidContainerSmall} —
 * every behaviour face is the base's, this card adds the row and the two interaction
 * flags (canWaterCrops/canPickUpFluids, the top-only drink gate on the block).
 *
 * <p>The raw item {@link #CLAY_JUG_RAW} is upstream meta 996 "Clay Jug"
 * (MultiItemRandomTools.java:120, {@code OreDictItemData(MT.Clay, U*6)}); the shaped
 * :133 row ({@code "kCR"/"C C"/"CCC"} — knife mark + rolling-pin mark + six clay
 * balls), the reverse shapeless (:120 tail — raw → 6 clay balls) and the :2095 smelting
 * tail ({@code RM.add_smelting(Ceramic_Jug_Raw, Ceramic_Jug)}) ride the
 * crafting/smelting datagen (GT6JugDatagen). The raw joins the machines tab (the
 * clay-raw kitchen law — the CLAY_MEASURING_POT_RAW/MODELED_PORCELAIN_CUP form).
 *
 * <p>Creative tab ownership: the jug's upstream category IS the "Fluid Containers" tab
 * GTBarrels owns — this card rides it through the BuildCreativeModeTabContentsEvent
 * append (the GT6Cups/GT6Cells form; GTBarrels.java stays untouched, the card red
 * line).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (one Block/BlockEntityType/BlockItem pair + one raw Item) with NO
 * KubeJS-specific seam; wiring a RegistryObject-backed addon event for the jug ids is
 * the declared defer (the GT6Bumbles.java:71-72 precedent shape). The recipe face is
 * standard datagen JSON, naturally moddable through the recipe events.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Jugs {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The ceramic jug block — the Loader :2095 row (the properties/carrier live on the block). */
	public static final RegistryObject<GT6JugBlock> CERAMIC_JUG = BLOCKS.register("ceramic_jug",
			() -> new GT6JugBlock(() -> GT6Jugs.JUG_BE.get(), GT6JugBlock.jugProperties()));

	/** The jug BET — one BlockEntityType over the one block (the CUP_BE form). */
	public static final RegistryObject<BlockEntityType<GT6JugBlockEntity>> JUG_BE =
			BLOCK_ENTITY_TYPES.register("ceramic_jug", () -> BlockEntityType.Builder.of(
					GT6JugBlockEntity::new, CERAMIC_JUG.get()).build(null));

	/** The jug BlockItem — the 16-stack registration column; a filled stack rides 1 (the base :423 content gate). */
	public static final RegistryObject<Item> CERAMIC_JUG_ITEM = ITEMS.register("ceramic_jug",
			() -> new GT6JugBlockItem(CERAMIC_JUG.get(), new Item.Properties().stacksTo(GT6JugBlockEntity.STACK_SIZE)));

	/**
	 * The Clay Jug raw item — upstream MultiItemRandomTools.java:120 ("Clay Jug",
	 * "Put in Furnace to harden", {@code OreDictItemData(MT.Clay, U*6)}). Craft-only
	 * acquisition upstream (the shaped :133 row); the smelt hardens it into
	 * {@link #CERAMIC_JUG_ITEM}.
	 */
	public static final RegistryObject<Item> CLAY_JUG_RAW = ITEMS.register("clay_jug",
			() -> new Item(new Item.Properties()));

	/** The "Fluid Containers" tab ride (the row's upstream category — the GTBarrels-owned tab, appended not owned). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTBarrels.FLUID_CONTAINERS_TAB.getId())) {
			aEvent.accept(new ItemStack(CERAMIC_JUG_ITEM.get()));
		}
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(CLAY_JUG_RAW.get())); // the clay-raw kitchen law (the class doc)
		}
	}

	private GT6Jugs() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Cups fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GT6Kitchen fork form.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Cups.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 ceramic jug registered: 2000 L liquid-proof scoop-and-water jug (the 996 raw, rain-collecting, top-face drinking, the small-tank base's second concrete)");
		});
	}
}
