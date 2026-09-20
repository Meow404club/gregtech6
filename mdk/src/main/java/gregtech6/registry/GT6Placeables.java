package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
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

import java.util.Locale;

import net.minecraft.world.level.block.state.BlockBehaviour;

import gregtech6.GT6Mod;
import gregtech6.tileentity.misc.GT6PlaceableBlock;
import gregtech6.tileentity.misc.GT6PlaceableBlockEntity;
import gregtech6.tileentity.misc.GT6GregOLanternBlock;
import gregtech6.tileentity.misc.GT6GregOLanternBlockEntity;
import gregtech6.tileentity.misc.GT6SandwichBlock;
import gregtech6.tileentity.misc.GT6SandwichBlockEntity;
import gregtech6.tileentity.misc.GT6SandwichItem;

/**
 * The placeables family registration (task p32-placeables) — the card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event, the
 * GT6Kitchen shape (ADR-P3-4; a separate class keeps the card scopes disjoint). Ports the
 * "Untyped" deco rows of Loader_MultiTileEntities.java:2023-2041 — the Greg o'Lantern
 * (:2031, MTE 32758), the Sandwich (:2032, MTE 32105) and the six material placement faces
 * (:2033-2040, the Ingot/Plate/GemPlate/Scrap/Rock/Stick placed piles) — while the Loot
 * Crate (:2030, the OD.crateGtEmpty prefix is unproven in the port) and the worldgen
 * Rock/Stick drops (:2034/:2036, the W6 worldgen domain — GT6SurfaceBlocks owns that face)
 * stay in the defer pool.
 *
 * <p>KJS面声明：本卡产出=注册面；注册面=defer 至 kjs 绑定卡。
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Placeables {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	// ------------------------------------------------------------------
	// The Greg o'Lantern (upstream :2031, MTE 32758; recipe "Pk"/"T "
	// = pumpkin over torch, the 'k' knife tool letter rides the crafting
	// datagen as the declared no-carrier fold — the port vanilla-crafting
	// bridge has no GT tool-letter face).
	// ------------------------------------------------------------------

	/** The lantern block — full cube, lightLevel 15, horizontal facing towards the placer. */
	public static final RegistryObject<GT6GregOLanternBlock> GREG_O_LANTERN = BLOCKS.register("greg_o_lantern",
			() -> new GT6GregOLanternBlock(GT6GregOLanternBlock.newProperties()));

	/** The lantern BET — the stateless TE carrier face (the "2 TE" registration assertion, half one). */
	public static final RegistryObject<BlockEntityType<GT6GregOLanternBlockEntity>> GREG_O_LANTERN_BE =
			BLOCK_ENTITY_TYPES.register("greg_o_lantern", () -> BlockEntityType.Builder.of(
					GT6GregOLanternBlockEntity::new, GREG_O_LANTERN.get()).build(null));

	/** The lantern item — plain BlockItem (the vanilla jack_o_lantern item form; drop-self loot). */
	public static final RegistryObject<Item> GREG_O_LANTERN_ITEM = ITEMS.register("greg_o_lantern",
			() -> new BlockItem(GREG_O_LANTERN.get(), new Item.Properties()));

	// ------------------------------------------------------------------
	// The Sandwich (upstream :2032, MTE 32105 — the bites comparator + the
	// placed bite face; the 16-slot ingredient assembly is the food-domain
	// pool, the port collapses to the default ten-layer sandwich).
	// ------------------------------------------------------------------

	/** The sandwich block — the fixed 12/16 layered box, bites comparator, wool-soft. */
	public static final RegistryObject<GT6SandwichBlock> SANDWICH = BLOCKS.register("sandwich",
			() -> new GT6SandwichBlock(GT6SandwichBlock.newProperties()));

	/** The sandwich BET — the bites-comparator TE carrier (the "2 TE" registration assertion, half two). */
	public static final RegistryObject<BlockEntityType<GT6SandwichBlockEntity>> SANDWICH_BE =
			BLOCK_ENTITY_TYPES.register("sandwich", () -> BlockEntityType.Builder.of(
					GT6SandwichBlockEntity::new, SANDWICH.get()).build(null));

	/** The sandwich item — edible BlockItem, sneak-place only (the upstream OnlyPlaceableWhenSneaking). */
	public static final RegistryObject<Item> SANDWICH_ITEM = ITEMS.register("sandwich",
			() -> new GT6SandwichItem(SANDWICH.get(), new Item.Properties()));

	// ------------------------------------------------------------------
	// The six placed piles (upstream :2033-2040 — RockPlaced 32074 /
	// StickPlaced 32073 / Ingots 32084 / Plates 32085 / Gem Plates 32086 /
	// Scrap 32103). No BlockItems: the placement face is the material item
	// dispatch (GT6PlaceablePlacement), the loot face is the BE contents
	// (the playerDestroy shell). The worldgen Rock/Stick drop faces stay in
	// the W6 domain (GT6SurfaceBlocks owns them).
	// ------------------------------------------------------------------

	public static final RegistryObject<GT6PlaceableBlock> PLACED_ROCK = BLOCKS.register("placed_rock",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.ROCK, BlockBehaviour.Properties.of()));
	public static final RegistryObject<GT6PlaceableBlock> PLACED_STICK = BLOCKS.register("placed_stick",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.STICK, BlockBehaviour.Properties.of()));
	public static final RegistryObject<GT6PlaceableBlock> PLACED_INGOT = BLOCKS.register("placed_ingot",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.INGOT, BlockBehaviour.Properties.of()));
	public static final RegistryObject<GT6PlaceableBlock> PLACED_PLATE = BLOCKS.register("placed_plate",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.PLATE, BlockBehaviour.Properties.of()));
	public static final RegistryObject<GT6PlaceableBlock> PLACED_GEM_PLATE = BLOCKS.register("placed_gem_plate",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.GEM_PLATE, BlockBehaviour.Properties.of()));
	public static final RegistryObject<GT6PlaceableBlock> PLACED_SCRAP = BLOCKS.register("placed_scrap",
			() -> new GT6PlaceableBlock(GT6PlaceableBlock.Kind.SCRAP, BlockBehaviour.Properties.of()));

	/**
	 * The shared placed-pile BET — six mounts, one BE class (ADR-P3-1; the upstream
	 * MultiTileEntityPlaceable base was itself the one-class-many-faces carrier).
	 */
	public static final RegistryObject<BlockEntityType<GT6PlaceableBlockEntity>> PLACEABLE_BE =
			BLOCK_ENTITY_TYPES.register("placed_pile", () -> BlockEntityType.Builder.of(
					GT6PlaceableBlockEntity::new,
					PLACED_ROCK.get(), PLACED_STICK.get(), PLACED_INGOT.get(),
					PLACED_PLATE.get(), PLACED_GEM_PLATE.get(), PLACED_SCRAP.get()).build(null));

	/** The Kind → block accessor (the placement dispatch face). */
	public static GT6PlaceableBlock placed(GT6PlaceableBlock.Kind aKind) {
		return switch (aKind) {
			case INGOT -> PLACED_INGOT.get();
			case PLATE -> PLACED_PLATE.get();
			case GEM_PLATE -> PLACED_GEM_PLATE.get();
			case SCRAP -> PLACED_SCRAP.get();
			case ROCK -> PLACED_ROCK.get();
			case STICK -> PLACED_STICK.get();
		};
	}

	/** The registry id of a placed pile (the lang/RCON face). */
	public static String placedId(GT6PlaceableBlock.Kind aKind) {
		return "placed_" + aKind.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * The FUNCTIONAL_BLOCKS-TAB join (the vanilla deco/functional tab — the 1.19.3 successor
	 * of the old decorations tab, where the torch lives; the GT6Kitchen MACHINES join is the
	 * processing-machine counter-form). Key-face comparison via location()/getId() —
	 * the GT6Kitchen.onBuildTabContents form.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS.location())) {
			aEvent.accept(new ItemStack(GREG_O_LANTERN_ITEM.get()));
			aEvent.accept(new ItemStack(SANDWICH_ITEM.get()));
		}
	}

	private GT6Placeables() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Kitchen.onModConstruct fork form). */
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

	/** Registration smoke evidence (the GT6Kitchen.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 placeables registered: {} (the Greg o'Lantern, MTE 32758) + {} (the Sandwich, MTE 32105) + {} (the six placed piles)",
					ForgeRegistries.BLOCKS.getKey(GREG_O_LANTERN.get()),
					ForgeRegistries.BLOCKS.getKey(SANDWICH.get()),
					ForgeRegistries.BLOCKS.getKey(PLACED_ROCK.get()));
		});
	}
}
