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

import gregapi.data.ANY;
import gregapi.oredict.OreDictMaterial;
import gregtech6.GT6Mod;
import gregtech6.block.tools.GT6SiftingTableBlock;
import gregtech6.tileentity.tools.GT6SiftingTableBlockEntity;

/**
 * The Sifting Table registration (task sifting-table-family) — card-owned
 * {@code EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Anvils shape (a separate class keeps the manual-device card scopes disjoint).
 * Ports the manual-devices chain tail of Loader_MultiTileEntities.java:2227: the
 * "Sifting Table" row — ID 32702, single variant, category "Misc Tool Blocks",
 * NBT_MATERIAL ANY.Steel, NBT_HARDNESS 1.0 / NBT_RESISTANCE 6.0, aUtilMetal (the
 * {@link SoundType#METAL} mapping), NBT_RECIPEMAP RM.Sifting. No other rows exist in
 * the upstream registration (the mortar/grindstone neighbours are the parallel cards).
 *
 * <p>The block properties ride the shared {@link GT6Kitchen#kitchenProperties} seam
 * (the noOcclusion + isViewBlocking-never chain — the sub-cube element model over a
 * true {@code canOcclude} would X-ray the neighbours, the #9 ruling). The crafting
 * row ("TdT","WxW","SPS" over the iron plateDouble/stickLong/screw/wireFine carriers,
 * the :2227 tail) rides the crafting datagen (GT6CraftingRecipes).
 *
 * <p>KJS face (declared): the registration surface defers to the kjs-binding card
 * (the GT6Kitchen precedent); the recipe face is the datapack domain.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SiftingTables {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The lazy material supplier — the :2227 NBT_MATERIAL column (class-load precedes ANY.init). */
	public static final Supplier<OreDictMaterial> MATERIAL = () -> ANY.Steel;

	/** The sifting table block — the :2227 row (aUtilMetal, 1.0/6.0, the kitchen properties seam). */
	public static final RegistryObject<GT6SiftingTableBlock> SIFTING_TABLE = BLOCKS.register("sifting_table",
			() -> new GT6SiftingTableBlock(MATERIAL, () -> GT6SiftingTables.SIFTING_TABLE_BE.get(),
					GT6Kitchen.kitchenProperties(SoundType.METAL, 6.0F)));

	/** The block item — the "Misc Tool Blocks" creative face rides the machines tab (the p27 retirement form). */
	public static final RegistryObject<Item> SIFTING_TABLE_ITEM = ITEMS.register("sifting_table",
			() -> new BlockItem(GT6SiftingTables.SIFTING_TABLE.get(), new Item.Properties()));

	/** The family BET — one row, one type (the GT6Anvils.ANVIL_BE form). */
	public static final RegistryObject<BlockEntityType<GT6SiftingTableBlockEntity>> SIFTING_TABLE_BE =
			BLOCK_ENTITY_TYPES.register("sifting_table", () -> BlockEntityType.Builder.of(
					GT6SiftingTableBlockEntity::new, SIFTING_TABLE.get()).build(null));

	/** The MACHINES-TAB join (the p27 kitchen retirement form — the table is a processing block). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(SIFTING_TABLE_ITEM.get()));
		}
	}

	private GT6SiftingTables() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Anvils fork form). */
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
	}

	/** Registration smoke evidence (the GT6Anvils onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 sifting table registered: {} (RM.Sifting, the manual chain tail)",
					ForgeRegistries.BLOCKS.getKey(SIFTING_TABLE.get()));
		});
	}
}
