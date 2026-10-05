package gregtech6.registry;

import java.util.List;

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

import gregtech6.block.tools.GT6GrindstoneBlock;
import gregtech6.tileentity.tools.GT6GrindstoneBlockEntity;

/**
 * The Grindstone registration (task grindstone-family) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Anvils shape (a separate class keeps the card scopes disjoint). Ports the ONE
 * "Misc Tool Blocks" Grindstone row of Loader_MultiTileEntities.java:2226: id 32703,
 * NBT_MATERIAL ANY.Steel, NBT_HARDNESS 1.0 / NBT_RESISTANCE 6.0, aUtilMetal (the METAL
 * sound), NBT_RECIPEMAP RM.Sharpening — single variant, so there is no ROWS ladder (a
 * later row would append a register pair only). The crafting row "SAS","SwS","PPP"
 * (plateDouble×3 + stickLong×4 + stick, all ANY.Iron + the wrench tool letter) rides the
 * crafting datagen (GT6CraftingRecipes, the anvil band).
 *
 * <p>GUI: none — the upstream tooltip is {@code LH.NO_GUI_CLICK_TO_INTERACT} with the
 * {@code FACE_ANYBUT_SIDES} gate (MultiTileEntityGrindStone.java:81), the menu-less
 * kitchen ruling. Creative tab: the {@link GTMachines#MACHINES_TAB} join (the p27
 * kitchen-tab retirement form — {@link #onBuildTabContents}). KJS: the registration face
 * defers to the kjs binding card (this class carries no script adapter); the recipe face
 * is the datapack domain (sharpening.json), naturally editable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Grindstones {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The Grindstone block — the :2226 row carrier (NBT_MATERIAL ANY.Steel + NBT_HARDNESS
	 * 1.0 / NBT_RESISTANCE 6.0 through the properties, the aUtilMetal METAL sound). The
	 * sub-cube shape needs the kitchen {@code noOcclusion().isViewBlocking(never)} chain
	 * (the c3-kitchen-tint-shape seam form), re-declared here so the GT6Kitchen seam stays
	 * its card's.
	 */
	public static final RegistryObject<GT6GrindstoneBlock> GRINDSTONE =
			BLOCKS.register("grindstone", () -> new GT6GrindstoneBlock(
					() -> GT6Grindstones.GRINDSTONE_BE.get(),
					BlockBehaviour.Properties.of().strength(1.0F, 6.0F).sound(SoundType.METAL)
							.noOcclusion().isViewBlocking(GT6Grindstones::never)));

	/** The Grindstone item (plain BlockItem — the anvil form; the abrasive rides gt.toolstate). */
	public static final RegistryObject<Item> GRINDSTONE_ITEM =
			ITEMS.register("grindstone", () -> new BlockItem(GRINDSTONE.get(), new Item.Properties()));

	/**
	 * The Grindstone BET — one BlockEntityType over the one row (the anvil BET form; the
	 * multi-mount collapses to a single mount because :2226 is single-variant).
	 */
	public static final RegistryObject<BlockEntityType<GT6GrindstoneBlockEntity>> GRINDSTONE_BE =
			BLOCK_ENTITY_TYPES.register("grindstone", () -> BlockEntityType.Builder.of(
					GT6GrindstoneBlockEntity::new, GRINDSTONE.get()).build(null));

	/** The datagen walkers' block list (the paintableBlockArray convention shape; one block). */
	public static List<Block> blockArray() {
		return List.of(GRINDSTONE.get());
	}

	/** The MACHINES-TAB join (the p27 kitchen retirement form — the anvil is the precedent). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(GRINDSTONE_ITEM.get()));
		}
	}

	/** The sub-cube vessel never blocks the view (the GT6Kitchen seam body). */
	private static boolean never(net.minecraft.world.level.block.state.BlockState aState,
			net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos) {
		return false;
	}

	private GT6Grindstones() {}

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
			gregtech6.GT6Mod.LOGGER.info("GT6 grindstone registered: {} (RM.Sharpening manual face)",
					ForgeRegistries.BLOCKS.getKey(GRINDSTONE.get()));
		});
	}
}
