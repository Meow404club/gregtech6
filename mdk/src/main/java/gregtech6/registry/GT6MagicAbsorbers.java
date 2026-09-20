package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.Map;

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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GT6MagicAbsorberBlock;
import gregtech6.tileentity.energy.generators.GT6MagicAbsorberBlockEntity;

/**
 * The Magic Field Absorber registration (task p32-magic-absorber) — the card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Lasers shape (ADR-P3-4). ONE machine over ONE block: the upstream
 * Loader_MultiTileEntities.java:1005 row "Magic Field Absorber", "Magical Energy
 * Production", ids 10180-10180, NBT_HARDNESS 4.0F == NBT_RESISTANCE 4.0F, stack 16, the
 * aMachine visual, NBT_MATERIAL MT.Pd (the Pd columns are registration config, never
 * persisted — the massfab reading; the port block carries no material tint face yet, the
 * W2 render pool).
 *
 * <p>The crafting row "GOG"/"LBL"/"CMC" of :1005 (casingMachine Pd + Circuit_Magic +
 * wireFine Au + plate Obsidian + gem Lapis + Blocks.beacon) is the CRAFTING POOL (the
 * usb-data posture — the machine stays RCON/test-obtainable until the pool card lands).
 * The creative tab "魔法能量" (itemGroup.gt.multitileentity.10180, the dump :17973) has
 * no port tab — the tab join is the pool. KJS surface: REGISTRATION face only, deferred
 * to the KJS binding card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MagicAbsorbers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The registered absorber block (path "magic_absorber", the BET twin). */
	public static final Map<String, RegistryObject<Block>> MAGIC_ABSORBER_BLOCKS_BY_PATH = new LinkedHashMap<>();
	/** The registered absorber item, same key. */
	public static final Map<String, RegistryObject<Item>> MAGIC_ABSORBER_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		// the :1005 columns: hardness/resistance 4.0 (the NBT_HARDNESS/RESISTANCE pair), stack 16
		MAGIC_ABSORBER_BLOCKS_BY_PATH.put("magic_absorber", BLOCKS.register("magic_absorber",
				() -> new GT6MagicAbsorberBlock(BlockBehaviour.Properties.of()
						.strength(4.0F, 4.0F).sound(SoundType.METAL), () -> GT6MagicAbsorbers.MAGIC_ABSORBER_BE.get())));
		MAGIC_ABSORBER_ITEMS_BY_PATH.put("magic_absorber", ITEMS.register("magic_absorber",
				() -> new BlockItem(MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get(),
						new Item.Properties().stacksTo(16))));
	}

	/** The BET — one BE class over the one block (the energy-source rig's single-type form). */
	public static final RegistryObject<BlockEntityType<GT6MagicAbsorberBlockEntity>> MAGIC_ABSORBER_BE =
			BLOCK_ENTITY_TYPES.register("magic_absorber", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6MagicAbsorberBlockEntity(GT6MagicAbsorbers.MAGIC_ABSORBER_BE.get(), aPos, aState),
					MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get()).build(null));

	private GT6MagicAbsorbers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Lasers fork form). */
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

	/** Registration smoke evidence (the GT6ElectricTransformers.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 magic absorber registered: Magic Field Absorber (10180, trophy-top -> QU 64 / TU 1 out the facing face), task p32-magic-absorber");
		});
	}
}
