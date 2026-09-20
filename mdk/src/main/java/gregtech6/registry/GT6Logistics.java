package gregtech6.registry;

import net.minecraft.core.registries.Registries;
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

import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.block.logistics.GTLogisticsWireBlockItem;
import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;

/**
 * The logistics-domain registration, card-owned (ADR-P3-4): self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event —
 * the {@link GTItemPipes} shape verbatim, so the domain's scope stays disjoint from the
 * in-flight wire-family cards (this class NEVER touches GTWires/GTWireSpecs/GTWireBlock
 * — the p32 conflict-surface ruling).
 *
 * <p>First row: the "Logistics Wire" connector (upstream meta id 24901, category
 * "Logistics", Loader_MultiTileEntities.java:1819 — NBT_HARDNESS 1.0F, NBT_RESISTANCE
 * 2.0F, NBT_DIAMETER PX_P[6], NBT_CONTACTDAMAGE F). The Core/CPU/Storage-endpoint rows
 * (17997/18200-04/6200+) are the Lv3 card's scope; the 12 logistics covers (1086-1099)
 * ride their own later card (research.p31-logistics slices.lv2 "死端点可后置").
 * NO creative tab yet — the upstream wire is tool-placed and the first logistics
 * machines arrive with Lv3 (declared cut).
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
