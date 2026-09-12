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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6FeConverterBlock;
import gregtech6.tileentity.energy.GT6FeConverterBlockEntity;

/**
 * The FE→EU converter registration (task p28-b-fe-converter-machine) — card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6FeBatteries/GT6Kitchen shape (ADR-P3-4; a separate class keeps the card scopes
 * disjoint). GTMod / GTModBusListener stay untouched (the frozen P2 form); GTMachines.java
 * is ZERO-edited — the machine tab join rides the BuildCreativeModeTabContents event seam
 * (the GT6Kitchen.onBuildTabContents precedent).
 *
 * <p>THE DECLARED DEVIATION FAMILY (deviation ledger, decisions.p28-eu-inbound-converter):
 * upstream 1.7.10 deliberately ships no RF→EU converter (Greg wall RF), so this machine is
 * a GTCEu-form addition (ConverterMachine + ConverterTrait reference), not an upstream
 * port. The card ADR carries the full declaration.
 *
 * <p>THE ULV BALANCE RULING (the user's mid-card correction, 2026-09-12): the family is
 * ONE machine, not a tier ladder — a single ULV converter at {@code V[0]} = 8 EU and one
 * amp. RF is only ever allowed to enter the energy chain at its very bottom: the
 * steam-boiler → kinetic → LV+-grid progression wall stays intact (LV machines demand
 * packets of at least 16 EU, so ULV packets power nothing beyond the bridge — the mild
 * version of Greg's "RF never touches EU"). A ladder would sum back to LV-class
 * throughput and break that wall, which is why the amps are pinned to 1. The lossless
 * 4:1 ratio ({@code CS.RF_PER_EU}) is unchanged. Block id {@code fe_converter} (lowercase,
 * the 1.20.1 ResourceLocation constraint).
 *
 * <p>Behavior lives in {@link GT6FeConverterBlockEntity} (the conversion core; the
 * upstream TE_Behavior_Energy_Converter anchor). KJS surface: the crafting recipe is
 * datapack-domain (naturally scriptable); the machine behavior has NO KubeJS face; the
 * registration face is deferred (the KJS binding pool) — the card's KJS declaration.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FeConverters {

	/**
	 * The tier voltage: ULV = 8 EU (upstream VN[0]). THE ONLY tier — the balance ruling in
	 * the class doc (RF enters the energy chain at its very bottom, one amp, or the
	 * progression wall breaks).
	 */
	public static final long VOLTAGE_ULV = 8;

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/**
	 * The ULV converter block (8 EU packets x 1 A; buffer 512 FE = 128 EU = 16 packets).
	 */
	public static final RegistryObject<GT6FeConverterBlock> FE_CONVERTER = BLOCKS.register("fe_converter",
			() -> new GT6FeConverterBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL)));

	/** The tier item — a plain BlockItem (no item-capability face; the FE face is block-only). */
	public static final RegistryObject<Item> FE_CONVERTER_ITEM = ITEMS.register("fe_converter",
			() -> new BlockItem(FE_CONVERTER.get(), new Item.Properties()));

	/**
	 * The BET — one BE class over the one block. Registers AFTER the BLOCK (vanilla
	 * registry order, the GT6FeBatteries doc).
	 */
	public static final RegistryObject<BlockEntityType<GT6FeConverterBlockEntity>> FE_CONVERTER_BE =
			BLOCK_ENTITY_TYPES.register("fe_converter", () -> BlockEntityType.Builder.of(
					GT6FeConverterBlockEntity::new, FE_CONVERTER.get()).build(null));

	/**
	 * The MACHINES-TAB join (the GT6Kitchen.onBuildTabContents verbatim form): the item
	 * rides {@code GTMachines.MACHINES_TAB} through the event seam — zero edits inside
	 * GTMachines.java. Key-face comparison via getId() (the location()/getId() two-face
	 * note).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(FE_CONVERTER_ITEM.get()));
		}
	}

	private GT6FeConverters() {}

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
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Kitchen.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 FE converter registered: {} (ULV, 8 EU x 1 A, the p28 inbound machine)",
					ForgeRegistries.BLOCKS.getKey(FE_CONVERTER.get()));
		});
	}
}
