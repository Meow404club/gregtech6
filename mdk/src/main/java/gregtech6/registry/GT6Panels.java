package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.panels.GT6PanelBlock;

/**
 * Registration home of the GT6 dyed COVER PANEL family (task material-mc-g1-panels-dyed,
 * the residual_sweep G1 card — Loader_MultiTileEntities.java:2043-2056): <b>48 per-row
 * Block+BlockItem registrations</b> — 3 families (Concrete Panel 32452+i / C-Foam Panel
 * 32468+i / Asphalt Panel 32484+i) x 16 dye colours, in the card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the GT6ConcreteBlocks precedent).
 *
 * <p><b>granularity</b>: per-pair, NOT a colour-property single block (the P8 ADR ④ ruling,
 * the GT6ConcreteBlocks javadoc) — the panel crafting emits per-colour OUTPUT stacks
 * ({@code CR.shaped 6x panel <- 1x source block colour i}, Loader_MultiTileEntities.java:2044/:2048/:2052).
 *
 * <p><b>id scheme</b>: {@code gt6:concrete_panel_<dye>} / {@code gt6:cfoam_panel_<dye>} /
 * {@code gt6:asphalt_panel_<dye>}, the dye segment exactly the
 * {@link gregtech6.item.spraycan.GTSprayCanItem#DYE_IDS} snake (= the CS.DYE_INDEX meta
 * order 0=Black..15=White).
 *
 * <p><b>creative tab</b>: the vanilla COLORED_BLOCKS tab — the concrete family precedent
 * (the upstream {@code CreativeTabs.tabDecorations} join, 1.20.1 retired the decorations
 * tab; the dye-coloured decorative home is COLORED_BLOCKS).
 *
 * <p><b>declared deviations</b> (the full set on {@link GT6PanelBlock}): the placement face
 * (upstream {@code canPlace -> F}) and the dyed cover-mounting face (the port
 * {@code CoverTextureSimple} seat is a flat sprite, no tinted-sprite face — pooled).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face with NO KubeJS-specific seam; the blockstate/model/recipe faces are
 * datapack domains, naturally moddable through the standard events; the registration face
 * is the declared defer (the GT6ConcreteBlocks javadoc posture).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Panels {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One (family, colour) row — the 48-element registration/walk unit. */
	public record PanelRow(GT6PanelBlock.Family family, int dyeIndex) {}

	/** The registry id of a panel block ({@code concrete_panel_light_gray} form). */
	public static String path(GT6PanelBlock.Family aFamily, int aDyeIndex) {
		return aFamily.snake + "_" + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	/** The 48 (family, colour) rows, family-major in upstream registration order, colour-major in meta order. */
	public static List<PanelRow> registrationOrder() {
		List<PanelRow> rOrder = new ArrayList<>(3 * 16);
		for (GT6PanelBlock.Family tFamily : GT6PanelBlock.Family.values()) {
			for (int i = 0; i < 16; i++) {
				rOrder.add(new PanelRow(tFamily, i));
			}
		}
		return rOrder;
	}

	/** The 48 panel blocks, registration order. */
	public static final List<RegistryObject<Block>> BLOCKS = registerBlocks();

	/** The 48 block items, registration order. */
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	private static List<RegistryObject<Block>> registerBlocks() {
		List<RegistryObject<Block>> rList = new ArrayList<>(48);
		for (PanelRow tRow : registrationOrder()) {
			int tDye = tRow.dyeIndex();
			GT6PanelBlock.Family tFamily = tRow.family();
			rList.add(BLOCKS_REG.register(path(tFamily, tDye), () -> new GT6PanelBlock(path(tFamily, tDye), tFamily, tDye)));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(48);
		// the id derives from the WALK (no eager RegistryObject deref — the .get() only
		// runs inside the supplier at registration time, the GT6ConcreteBlocks form;
		// an eager get() is the clinit NPE trap runData caught)
		for (int i = 0; i < BLOCKS.size(); i++) {
			PanelRow tRow = registrationOrder().get(i);
			int tIndex = i;
			rList.add(ITEMS_REG.register(path(tRow.family(), tRow.dyeIndex()),
					() -> new GTComposedNameItem(BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	private GT6Panels() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ConcreteBlocks.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.minecraftforge.eventbus.api.IEventBus tModBus =
				net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS_REG.register(tModBus);
		ITEMS_REG.register(tModBus);
	}

	/**
	 * The COLORED_BLOCKS join (the concrete family precedent — the upstream
	 * tabDecorations face, 1.20.1 tab mapping): all 48 items, registration order.
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.COLORED_BLOCKS) {
			for (RegistryObject<Item> tItem : ITEMS) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
