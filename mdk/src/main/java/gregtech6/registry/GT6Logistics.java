package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.logistics.GTLogisticsCoreBlock;
import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.block.logistics.GTLogisticsWireBlockItem;
import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;
import gregtech6.tileentity.multiblocks.GT6LogisticsCoreBlockEntity;

/**
 * The logistics-domain registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * the {@link GTItemPipes} shape verbatim, so the domain's scope stays disjoint from the
 * in-flight wire-family cards (this class NEVER touches GTWires/GTWireSpecs/GTWireBlock
 * — the p32 conflict-surface ruling).
 *
 * <p>Rows: the "Logistics Wire" connector (upstream meta id 24901, category
 * "Logistics", Loader_MultiTileEntities.java:1819 — NBT_HARDNESS 1.0F, NBT_RESISTANCE
 * 2.0F, NBT_DIAMETER PX_P[6], NBT_CONTACTDAMAGE F) and — since the Lv3 card — the
 * Logistics Core controller (upstream meta 17997, Loader:1281). The 7 structure parts
 * (18008 wall / 18299 vents / 18200-04 CPU units) are the GTMultiBlocks part rows the
 * massfab card landed; the 12 logistics covers (1086-1099) ride their own later card
 * (research.p31-logistics slices.lv2 "死端点可后置"). The wire and the core are
 * tool/RCON-placed — NO creative tab (the tab walk joins with the cover family).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Logistics {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The registry path (upstream :59 "gt.multitileentity.connector.wire.logistics", the BET-path-mirrors-TE-name convention). */
	public static final String WIRE_PATH = "logistics_wire";

	public static final RegistryObject<GTLogisticsWireBlock> LOGISTICS_WIRE = BLOCKS.register(WIRE_PATH,
			GT6Logistics::makeWireBlock);

	/** The registration payload of the wire block (the test-replay seam — the EXACT supplier FML pushes, GT6LogisticsRegistrationTest id686 guard). */
	public static GTLogisticsWireBlock makeWireBlock() {
		return new GTLogisticsWireBlock(BlockBehaviour.Properties.of()
				.strength(1.0F, 2.0F) // the Loader:1819 NBT_HARDNESS/NBT_RESISTANCE pair
				.sound(SoundType.METAL));
	}

	public static final RegistryObject<Item> LOGISTICS_WIRE_ITEM = ITEMS.register(WIRE_PATH,
			GT6Logistics::makeWireItem);

	/** The registration payload of the BlockItem — identity-critical: the item MUST hold the REGISTERED block instance (LOGISTICS_WIRE.get(), never a maker twin). Live-FML only; the offline test replay builds it over the replayed registry block directly. */
	public static Item makeWireItem() {
		return new GTLogisticsWireBlockItem(GT6Logistics.LOGISTICS_WIRE.get(), new Item.Properties());
	}

	/** The single-row BET (the "one TE class, many blocks" multi-mount with one block — Lv3 endpoints join as extra mounts, the ITEM_PIPE_BE form). */
	public static final RegistryObject<BlockEntityType<GTLogisticsWireBlockEntity>> LOGISTICS_WIRE_BE =
			BLOCK_ENTITY_TYPES.register(WIRE_PATH, GT6Logistics::makeWireBE);

	/** The registration payload of the BET (the test-replay seam — mounts the registered wire block). */
	public static BlockEntityType<GTLogisticsWireBlockEntity> makeWireBE() {
		return BlockEntityType.Builder.of(GTLogisticsWireBlockEntity::new, LOGISTICS_WIRE.get()).build(null);
	}

	// ---------------------------------------------------------------------------
	// the Logistics Core (task p32-logistics-lv3, upstream meta 17997,
	// Loader_MultiTileEntities.java:1281 — MT.SteelGalvanized, NBT_HARDNESS 6.0F ==
	// NBT_RESISTANCE 6.0F, NBT_TEXTURE "logisticscore", category "Multiblock Machines").
	// The seven structure parts (18008 wall / 18299 vents / 18200-04 CPU units) are the
	// GTMultiBlocks part rows already in the registry (the massfab card landed them); this
	// registration is the CONTROLLER only. The registration plane is KJS-deferred (the
	// kjs-binding card owns the expose face). NO creative tab (the wire form — RCON/tool
	// placed; the tab walk joins when the cover family lands).
	// ---------------------------------------------------------------------------

	/** The registry path (the BET-path-mirrors-TE-name convention, {@link GT6LogisticsCoreBlockEntity#getTileEntityName()}). */
	public static final String CORE_PATH = "multiblock_logistics_core";

	public static final RegistryObject<GTLogisticsCoreBlock> LOGISTICS_CORE = BLOCKS.register("logistics_core",
			GT6Logistics::makeCoreBlock);

	/** The registration payload of the core block (the test-replay seam, the makeWireBlock form). */
	public static GTLogisticsCoreBlock makeCoreBlock() {
		return new GTLogisticsCoreBlock(BlockBehaviour.Properties.of()
				.strength(6.0F, 6.0F) // the Loader:1281 NBT_HARDNESS/NBT_RESISTANCE pair
				.sound(SoundType.METAL));
	}

	public static final RegistryObject<Item> LOGISTICS_CORE_ITEM = ITEMS.register("logistics_core",
			() -> new BlockItem(LOGISTICS_CORE.get(), new Item.Properties()));

	/** The core BET (the massfab degenerate one-class-over-one-block form). */
	public static final RegistryObject<BlockEntityType<GT6LogisticsCoreBlockEntity>> LOGISTICS_CORE_BE =
			BLOCK_ENTITY_TYPES.register(CORE_PATH, GT6Logistics::makeCoreBE);

	/** The registration payload of the core BET (mounts the registered core block). */
	public static BlockEntityType<GT6LogisticsCoreBlockEntity> makeCoreBE() {
		return BlockEntityType.Builder.of(GT6LogisticsCoreBlockEntity::new, LOGISTICS_CORE.get()).build(null);
	}

	private GT6Logistics() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTItemPipes.onModConstruct verbatim). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (GTItemPipes fork verbatim)
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}
}
