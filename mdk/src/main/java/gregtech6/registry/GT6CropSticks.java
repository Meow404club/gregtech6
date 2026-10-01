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
import gregtech6.crop.GT6CropStickItem;
import gregtech6.crop.GT6CropSticksBlock;
import gregtech6.registry.GT6Foods;

/**
 * The crop-stick block/BE/item registration (card cbc-1-cropstick-base)  --  the ADR-P3-4
 * self-contained listener form (the GT6Cups shape). Ports the IC2 crop-stick carrier the
 * GT6 1.7.10 system parasited (research.crop-breeding q1: GT6 had NO own block): one block
 * (two blockstate faces single/crossing), one BET, one stick item. The block itself is NOT
 * a BlockItem  --  it is only reachable by planting sticks (the upstream acquisition face).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (one Block/BlockEntityType/Item set) with NO KubeJS-specific seam;
 * wiring an addon event for the crop ids is the declared defer to the KJS binding card
 * (the GT6Cups.java declaration form). Recipe face: datapack-domain zero-adaptation.
 * Viewer face: no machine diagram, zero JEI/EMI surfaces. Jade face: another card, not
 * this wave (the cbc-6 face census).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CropSticks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The crop-stick block  --  the two-face (single/crossing) BE carrier. */
	public static final RegistryObject<GT6CropSticksBlock> CROP_STICKS = BLOCKS.register("crop_sticks",
			() -> new GT6CropSticksBlock(GT6CropSticksBlock.cropProperties()));

	/** The crop-stick BET  --  one BlockEntityType over the one block (the MEASURING_POT_BE form). */
	public static final RegistryObject<BlockEntityType<gregtech6.crop.GT6CropBlockEntity>> CROP_STICKS_BE =
			BLOCK_ENTITY_TYPES.register("crop_sticks", () -> BlockEntityType.Builder.of(
					gregtech6.crop.GT6CropBlockEntity::new, CROP_STICKS.get()).build(null));

	/** The stick item  --  no BlockItem: the block is planted, never placed as a block item. */
	public static final RegistryObject<Item> CROP_STICK_ITEM = ITEMS.register("crop_stick",
			() -> new GT6CropStickItem(new Item.Properties()));

	/** The food-tab ride (the crop domain's upstream home  --  the "Nature & Foods" band). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GT6Foods.FOOD_TAB.getId())) {
			aEvent.accept(new ItemStack(CROP_STICK_ITEM.get()));
		}
	}

	private GT6CropSticks() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Cells fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework  --  the GT6Cups fork form.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Cups.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 crop sticks registered: the breeding base (4 grain crops walkable via /gt6crop, the ADR-CB5 offline-driven tick base)");
		});
	}
}
