package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.crop.GT6CropCard;
import gregtech6.crop.GT6CropSeeds;
import gregtech6.crop.GT6CropStickItem;
import gregtech6.crop.GT6CropSticksBlock;
import gregtech6.crop.GT6Crops;

/**
 * The crop-stick block/BE/item registration (card cbc-1-cropstick-base)  --  the ADR-P3-4
 * self-contained listener form (the GT6Cups shape). Ports the IC2 crop-stick carrier the
 * GT6 1.7.10 system parasited (research.crop-breeding q1: GT6 had NO own block): one block
 * (two blockstate faces single/crossing), one BET, one stick item. The block itself is NOT
 * a BlockItem  --  it is only reachable by planting sticks (the upstream acquisition face).
 *
 * <p>Also the crop domain's creative-tab home (task crop-creative-tab, the user ruling
 * 2026-10-08: no IC2 on the modern line — the crop domain is fully self-owned, creative
 * seat included): the {@code gt6:crops} tab (the GT6Foods.FOOD_TAB builder form), which
 * the crop_stick / crop_seed pairs retired from riding (the old "Nature &amp; Foods"
 * BuildCreativeModeTabContentsEvent seats are gone with it).
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
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use (the GT6Foods form). */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.crops";

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

	/**
	 * The "GregTech: Crops" tab — id {@code gt6:crops}, title key {@link #TAB_TITLE_KEY},
	 * icon the crop stick (the domain entry item). Builder form = the GT6Foods.FOOD_TAB
	 * precedent (Row.TOP, column 0). The walk ({@link #walkDisplayItems}): the stick, the
	 * blank seed, then one FULLY-SCANNED representative seed per registered card — the
	 * {@link GT6Crops#crops()} walk, the weed card included (the honest full book).
	 *
	 * <p>The representative seed's stat bytes are the NEUTRAL 1/1/1 — the GT_BaseCrop.java:77
	 * {@code registerBaseSeed(seed, this, 1, 1, 1, 1)} literal — because the port's
	 * {@link GT6CropCard} carries NO G/Ga/Re base column at all (its stat quintet is the IC2
	 * chem/food/def/color/weed set, a different face), so the task card's fallback arm applies
	 * and this javadoc is the declaration. scan 4 makes the tooltip disclose the name plus the
	 * Gr/Ga/Re lines (the port-native enhancement: the creative page hands out any crop's
	 * fully-scanned seed, serving the breeding play loop and the offline tests).
	 */
	public static final RegistryObject<CreativeModeTab> CROPS_TAB = CREATIVE_MODE_TABS.register("crops",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(CROP_STICK_ITEM.get()))
					.displayItems((aParameters, aOutput) -> walkDisplayItems(aOutput))
					.build());

	/**
	 * The tab walk over the live registry — the {@code displayItems} builder lambda body.
	 * The list overload is the real walk; this delegate hands it the live
	 * {@link GT6Crops#crops()} (registration order, WEED first).
	 */
	public static void walkDisplayItems(CreativeModeTab.Output aOutput) {
		walkDisplayItems(aOutput, GT6Crops.crops());
	}

	/**
	 * The walk body — the stick, the blank seed (no payload, the bare item face), then one
	 * fully-scanned representative seed per card. The explicit-list overload is the hermetic
	 * test seam (the CropEndToEndTest law: the shared GT6Crops registry accumulates fixture
	 * cards across the test JVM, so a count pin must ride an explicit list).
	 */
	public static void walkDisplayItems(CreativeModeTab.Output aOutput, List<GT6CropCard> aCards) {
		aOutput.accept(new ItemStack(CROP_STICK_ITEM.get()));
		aOutput.accept(new ItemStack(GT6CropSeeds.CROP_SEED.get()));
		for (GT6CropCard tCard : aCards) {
			aOutput.accept(GT6CropSeeds.generateSeeds(tCard, 1, 1, 1, 4));
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
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Cups.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 crop sticks registered: the breeding base (4 grain crops walkable via /gt6crop, the ADR-CB5 offline-driven tick base)");
		});
	}
}
