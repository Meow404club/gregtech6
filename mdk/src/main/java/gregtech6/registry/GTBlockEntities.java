package gregtech6.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.TestMachineBlock;
import gregtech6.tileentity.TestMachineBlockEntity;

/**
 * Block + BlockEntityType registration, card-owned (ADR-P3-4): the deferred registers
 * attach to the mod bus from this self-contained {@code @EventBusSubscriber(MOD)}
 * listener — GT6Mod.java / GTModBusListener.java stay untouched (the P2 instance
 * registration form is frozen; new registration never extends it).
 *
 * <p>BET form = one shared BlockEntityType mounting several blocks (ADR-P3-1, the GT6
 * MultiTile "one TE class, many material blocks" counterpart; GTCEu GTBlockEntities
 * CABLE precedent): {@code BlockEntityType.Builder.of(factory, Block instances...)}
 * + {@code build(null)} without a datafixer (vanilla BlockEntityType.java:316-322,
 * mod convention).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBlockEntities {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** Ticking test machine block. */
	public static final RegistryObject<Block> TEST_MACHINE = BLOCKS.register("test_machine",
			() -> new TestMachineBlock(true, BlockBehaviour.Properties.of()));

	/** Passive test machine block — same BE class, no ticker (notick chain equivalence). */
	public static final RegistryObject<Block> TEST_MACHINE_IDLE = BLOCKS.register("test_machine_idle",
			() -> new TestMachineBlock(false, BlockBehaviour.Properties.of()));

	/**
	 * Shared BET: one BlockEntityType, two valid blocks (ADR-P3-1). The registry path
	 * mirrors TestMachineBlockEntity#getTileEntityName — the 1.7.10 "id" write
	 * (TileEntityBase01Root.java:148) and the registration name were the same string,
	 * the BET registry key is its 1.20.1 carrier. BLOCK registers before
	 * BLOCK_ENTITY_TYPES (vanilla registry order), so the RegistryObject .get() calls
	 * in the supplier are safe at registration time.
	 */
	public static final RegistryObject<BlockEntityType<TestMachineBlockEntity>> TEST_MACHINE_BE =
			BLOCK_ENTITY_TYPES.register("test_machine", () -> BlockEntityType.Builder.of(
					TestMachineBlockEntity::new, TEST_MACHINE.get(), TEST_MACHINE_IDLE.get()).build(null));

	private GTBlockEntities() {}

	/**
	 * FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any
	 * RegisterEvent (ModLoadingStage order — GTModBusListener.java:16 wires the same
	 * point). Bus.MOD.bus().get() = FMLJavaModLoadingContext.get().getModEventBus()
	 * (Mod.java:81), valid on the mod-loading thread for the whole loading lifecycle.
	 */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}
}
