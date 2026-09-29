package gregtech6.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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
import gregtech6.block.tank.GT6MeasuringPotBlock;
import gregtech6.tileentity.tank.GT6MeasuringPotBlockEntity;

/**
 * The Measuring Pot family registration (task r8-issue45-c3, issue #45) — the ADR-P3-4
 * self-contained listener form (the GT6Kitchen/GT6Tanks shape: a separate class keeps the
 * parallel issue-#45 card scopes disjoint — C1 owns GT6Kitchen, C2 owns GT6Crucibles).
 * Ports the "Fluid Containers" ceramic row Loader_MultiTileEntities.java:2096 verbatim:
 * "Ceramic Measuring Pot", id 32738, hardness 0.5 / resistance 6.0, aUtilStone,
 * NBT_TANK_CAPACITY 1000 (the metal rows :2097-2099 — SS/W/Ta4HfC5, the plateCurved
 * crafting band — are the declared defer, the research split).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (one Block/BlockEntityType/BlockItem + one raw Item) and the datapack
 * crafting face (tier-a JSON, naturally moddable). NO KubeJS-specific seam ships; wiring
 * a RegistryObject-backed addon event for {@code clay_measuring_pot} is the declared
 * defer.
 *
 * <p>The raw item {@link #CLAY_MEASURING_POT_RAW} is upstream meta 997 "Clay Measuring
 * Pot" (MultiItemRandomTools.java:121, {@code OreDictItemData(MT.Clay, U*4)}); the shaped
 * :134 row ("CkC"/"CCR" over clay + the knife/rolling-pin tool marks, 4 clay = U*4), the
 * reverse shapeless (:121 — raw → 4 clay balls) and the :2096 smelting tail
 * ({@code RM.add_smelting(Measuring_Pot_Raw, Measuring_Pot)}) ride the crafting/smelting
 * datagen (GT6MeasuringPotDatagen). The raw item joins the machines tab (the kitchen
 * family law — the CLAY_BOWL_RAW row, GT6Kitchen.onBuildTabContents).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MeasuringPot {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The ceramic pot block — the Loader :2096 row (the properties/carrier live on the block). */
	public static final RegistryObject<GT6MeasuringPotBlock> MEASURING_POT = BLOCKS.register("measuring_pot",
			() -> new GT6MeasuringPotBlock(() -> GT6MeasuringPot.MEASURING_POT_BE.get(), GT6MeasuringPotBlock.potProperties()));

	/** The pot BET — one BlockEntityType over the one block (the MIXING_BOWL_BE form). */
	public static final RegistryObject<BlockEntityType<GT6MeasuringPotBlockEntity>> MEASURING_POT_BE =
			BLOCK_ENTITY_TYPES.register("measuring_pot", () -> BlockEntityType.Builder.of(
					GT6MeasuringPotBlockEntity::new, MEASURING_POT.get()).build(null));

	/** The pot BlockItem (plain — the family ships no item-capability face, the kitchen form). */
	public static final RegistryObject<Item> MEASURING_POT_ITEM = ITEMS.register("measuring_pot",
			() -> new BlockItem(MEASURING_POT.get(), new Item.Properties()));

	/**
	 * The Clay Measuring Pot raw item — upstream MultiItemRandomTools.java:121 ("Clay
	 * Measuring Pot", "Put in Furnace to harden", {@code OreDictItemData(MT.Clay, U*4)}).
	 * Craft-only acquisition upstream (the shaped :134 row); the smelt hardens it into
	 * {@link #MEASURING_POT_ITEM}.
	 */
	public static final RegistryObject<Item> CLAY_MEASURING_POT_RAW = ITEMS.register("clay_measuring_pot",
			() -> new Item(new Item.Properties()));

	/** The MACHINES-TAB join (the kitchen family law — GT6Kitchen.onBuildTabContents form). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(MEASURING_POT_ITEM.get()));
			aEvent.accept(new ItemStack(CLAY_MEASURING_POT_RAW.get()));
		}
	}

	private GT6MeasuringPot() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Kitchen fork form). */
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

	/** Registration smoke evidence (the GT6Kitchen.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 measuring pot registered: {} ({} L default limit) + {} (the 997 raw)",
					ForgeRegistries.BLOCKS.getKey(MEASURING_POT.get()), GT6MeasuringPotBlockEntity.DEFAULT_CAPACITY,
					ForgeRegistries.ITEMS.getKey(CLAY_MEASURING_POT_RAW.get()));
		});
	}
}
