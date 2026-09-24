package gregtech6.registry;

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
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6FeBatteryBlock;
import gregtech6.block.energy.GT6FeSourceBlock;
import gregtech6.tileentity.energy.GT6FeBatteryBlockEntity;
import gregtech6.tileentity.energy.GT6FeSourceBlockEntity;

/**
 * The FE battery/source test fixture registration, card-owned (ADR-P3-4): a self-contained
 * {@code @EventBusSubscriber(MOD)} triple DeferredRegister attached from the construct
 * event — the GTEnergySources shape verbatim (task p8-d4), including the BLOCK-before-BET
 * registration order (vanilla registry order holds across DeferredRegisters, the
 * GTBlockEntities doc). GT6Mod / GTModBusListener stay untouched (the frozen P2 form).
 *
 * <p>This battery is a PLAIN platform {@code EnergyStorage} reference implementation
 * (capacity = maxReceive, maxExtract = 0 — a pure sink) behind a simple-cube block, the
 * measurement endpoint of the W3 flux-dynamo RCON chain (task p28-c-flux-dynamo-rcon).
 * The RCON chain drives it headless (/gt6febattery place|stat|reset); the two items also
 * join MACHINES_TAB (task p38-tabfix-b-energy, {@link #onBuildTabContents} — supersedes
 * the old no-creative-tab fixture note; port-native family, no upstream category to
 * preserve).
 *
 * <p>The texture is borrowed from the energy_source rig placeholder (the same
 * placeholder-borrow posture as the p8 source rig, assets/README.md attribution row).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FeBatteries {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The FE sink fixture block (the W3 dynamo-chain measurement end). */
	public static final RegistryObject<GT6FeBatteryBlock> FE_BATTERY = BLOCKS.register("fe_battery",
			() -> new GT6FeBatteryBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	public static final RegistryObject<Item> FE_BATTERY_ITEM = ITEMS.register("fe_battery",
			() -> new BlockItem(FE_BATTERY.get(), new Item.Properties()));

	/** BET registers after BLOCK (vanilla registry order) so the validBlocks supplier resolves. */
	public static final RegistryObject<BlockEntityType<GT6FeBatteryBlockEntity>> FE_BATTERY_BE =
			BLOCK_ENTITY_TYPES.register("fe_battery", () -> BlockEntityType.Builder.of(
					GT6FeBatteryBlockEntity::new, FE_BATTERY.get()).build(null));

	// -------------------------------------------------------------------------
	// the FE source fixture (task p28-b-fe-converter-machine — TAIL-APPENDED, the
	// fixture domain): the EXTRACTABLE twin of the sink battery above. The converter's
	// pull face (EnergyBridge.extractFe over the adapted extractEnergy) needs a source
	// that canExtract — the sink's maxExtract=0 keeps it a pure sink. Creative tab:
	// rides the same MACHINES_TAB join as the sink (the p38-tabfix-b-energy pool-cut;
	// /gt6fesource still drives it headless).
	// -------------------------------------------------------------------------

	/** The FE source fixture block (the p28 inbound pull-face acceptance source). */
	public static final RegistryObject<GT6FeSourceBlock> FE_SOURCE = BLOCKS.register("fe_source",
			() -> new GT6FeSourceBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 2.0F).sound(SoundType.COPPER)));

	/** The fixture item — registered for /give parity with the sink battery (tab join: the shared {@link #onBuildTabContents} walk). */
	public static final RegistryObject<Item> FE_SOURCE_ITEM = ITEMS.register("fe_source",
			() -> new BlockItem(FE_SOURCE.get(), new Item.Properties()));

	/** BET registers after BLOCK (vanilla registry order, the FE_BATTERY_BE doc). */
	public static final RegistryObject<BlockEntityType<GT6FeSourceBlockEntity>> FE_SOURCE_BE =
			BLOCK_ENTITY_TYPES.register("fe_source", () -> BlockEntityType.Builder.of(
					GT6FeSourceBlockEntity::new, FE_SOURCE.get()).build(null));

	private GT6FeBatteries() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (the GTEnergySources fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — both fixture items join the machines tab;
	 * the GT6BurningBoxes.onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 * JEI 1.20.1 derives its item list from the tab display items, so registered-but-
	 * tab-less was invisible in both the creative menu and JEI. Pool-cut declaration:
	 * port-native family (the p28 FE bridge), no upstream category to preserve — the
	 * MACHINES_TAB is the nearest live category (the GT6Circuits.INTEGRATED_CIRCUIT
	 * precedent, GTMachines displayItems).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(FE_BATTERY_ITEM.get()));
			aEvent.accept(new ItemStack(FE_SOURCE_ITEM.get()));
		}
	}
}
