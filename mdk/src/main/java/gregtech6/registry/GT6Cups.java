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
import gregtech6.block.tank.GT6CupBlock;
import gregtech6.item.GT6CupBlockItem;
import gregtech6.tileentity.tank.GT6CupBlockEntity;

/**
 * The Porcelain Cup registration (task small-tank-cup) — the ADR-P3-4 self-contained
 * listener form (the GT6Cells/GT6MeasuringPot shape). Ports the "Fluid Containers" row
 * Loader_MultiTileEntities.java:2094 verbatim: "Porcelain Cup", id 32739, hardness 0.5 /
 * resistance 6.0, aUtilStone, NBT_TANK_CAPACITY 250, NBT_LIQUIDPROOF T, NBT_GASPROOF F,
 * NBT_MAGICPROOF T, NBT_TEMPERATURE = the Porcelain melting point, stack 16. The FIRST
 * concrete of the small-tank BE base {@code TileEntityBase10FluidContainerSmall} — the
 * base class is this card's deliverable and the jug card's reuse face (the MeasuringPot
 * coordination note: its card owns no small-tank base, this one lands it).
 *
 * <p>The raw item {@link #MODELED_PORCELAIN_CUP_RAW} is upstream meta 899 "Modeled
 * Porcelain Cup" (MultiItemRandomTools.java:76, {@code OreDictItemData(MT.Porcelain, U)});
 * the shaped :78 row ({@code "kPR"} — knife mark + porcelain dust + rolling-pin mark),
 * the reverse shapeless (:76 tail — raw → 1 porcelain dust) and the :2094 smelting tail
 * ({@code RM.add_smelting(Porcelain_Cup_Raw, Porcelain_Cup)}) ride the crafting/smelting
 * datagen (GT6CupDatagen). The raw joins the machines tab (the clay-raw kitchen law —
 * the CLAY_BOWL_RAW/CLAY_MEASURING_POT_RAW form); upstream it rode the "GregTech:
 * Equipment" tab the port does not carry.
 *
 * <p>Creative tab ownership: the cup's upstream category IS the "Fluid Containers" tab
 * GTBarrels owns — this card rides it through the BuildCreativeModeTabContentsEvent
 * append (the GT6MeasuringPot/GT6Cells form; GTBarrels.java stays untouched, the card
 * red line).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (one Block/BlockEntityType/BlockItem pair + one raw Item) with NO
 * KubeJS-specific seam; wiring a RegistryObject-backed addon event for the cup ids is
 * the declared defer (the GT6Bumbles.java:71-72 precedent shape). The recipe face is
 * standard datagen JSON, naturally moddable through the recipe events.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Cups {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The porcelain cup block — the Loader :2094 row (the properties/carrier live on the block). */
	public static final RegistryObject<GT6CupBlock> PORCELAIN_CUP = BLOCKS.register("porcelain_cup",
			() -> new GT6CupBlock(() -> GT6Cups.CUP_BE.get(), GT6CupBlock.cupProperties()));

	/** The cup BET — one BlockEntityType over the one block (the MEASURING_POT_BE form). */
	public static final RegistryObject<BlockEntityType<GT6CupBlockEntity>> CUP_BE =
			BLOCK_ENTITY_TYPES.register("porcelain_cup", () -> BlockEntityType.Builder.of(
					GT6CupBlockEntity::new, PORCELAIN_CUP.get()).build(null));

	/** The cup BlockItem — the 16-stack registration column; a filled stack rides 1 (the base :423 content gate). */
	public static final RegistryObject<Item> PORCELAIN_CUP_ITEM = ITEMS.register("porcelain_cup",
			() -> new GT6CupBlockItem(PORCELAIN_CUP.get(), new Item.Properties().stacksTo(GT6CupBlockEntity.STACK_SIZE)));

	/**
	 * The Modeled Porcelain Cup raw item — upstream MultiItemRandomTools.java:76 ("Modeled
	 * Porcelain Cup", "Put in Furnace to harden", {@code OreDictItemData(MT.Porcelain, U)}).
	 * Craft-only acquisition upstream (the shaped :78 row); the smelt hardens it into
	 * {@link #PORCELAIN_CUP_ITEM}.
	 */
	public static final RegistryObject<Item> MODELED_PORCELAIN_CUP_RAW = ITEMS.register("modeled_porcelain_cup",
			() -> new Item(new Item.Properties()));

	/** The "Fluid Containers" tab ride (the row's upstream category — the GTBarrels-owned tab, appended not owned). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTBarrels.FLUID_CONTAINERS_TAB.getId())) {
			aEvent.accept(new ItemStack(PORCELAIN_CUP_ITEM.get()));
		}
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(MODELED_PORCELAIN_CUP_RAW.get())); // the clay-raw kitchen law (the class doc)
		}
	}

	private GT6Cups() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Cells fork form). */
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

	/** Registration smoke evidence (the GT6Cells.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 porcelain cup registered: 250 L liquid+magic-proof drinkable bowl (the 899 raw, rain-collecting, the small-tank base's first concrete)");
		});
	}
}
