package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.Map;

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

import gregtech6.block.energy.GT6MagicAbsorberBlock;
import gregtech6.tileentity.energy.generators.GT6MagicAbsorberBlockEntity;

/**
 * The Magic Field Absorber registration (task p32-magic-absorber) — the card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6Lasers shape (ADR-P3-4). ONE machine over ONE block: the upstream
 * Loader_MultiTileEntities.java:1005 row "Magic Field Absorber", "Magical Energy
 * Production", ids 10180-10180, NBT_HARDNESS 4.0F == NBT_RESISTANCE 4.0F, stack 16, the
 * aMachine visual, NBT_MATERIAL MT.Pd (the Pd columns are registration config, never
 * persisted — the massfab reading; the tint face rides the block's material column since
 * task p38-c2-controller-tint, the paint spray seat stays the W2 render pool).
 *
 * <p>The crafting row "GOG"/"LBL"/"CMC" of :1005 (casingMachine Pd + Circuit_Magic +
 * wireFine Au + plate Obsidian + gem Lapis + Blocks.beacon) is the CRAFTING POOL (the
 * usb-data posture — the machine stays RCON/test-obtainable until the pool card lands).
 * The upstream "Magical Energy Production" tab (itemGroup.gt.multitileentity.10180, the
 * dump :17973) is pooled into the MACHINES_TAB join (task p38-tabfix-b-energy,
 * {@link #onBuildTabContents}; the GTBarrels:257 pooling precedent). KJS surface:
 * REGISTRATION face only, deferred to the KJS binding card.
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

	/** The :1005 row's NBT_MATERIAL column (task p38-c2-controller-tint — the census-facing single source). */
	public static final java.util.function.Supplier<gregapi.oredict.OreDictMaterial> MAGIC_ABSORBER_MATERIAL = () -> gregapi.data.MT.Pd;

	static {
		// the :1005 columns: hardness/resistance 4.0 (the NBT_HARDNESS/RESISTANCE pair),
		// stack 16, NBT_MATERIAL MT.Pd (the tint colour source — task p38-c2-controller-tint,
		// the class-doc massfab reading now wired)
		MAGIC_ABSORBER_BLOCKS_BY_PATH.put("magic_absorber", BLOCKS.register("magic_absorber",
				() -> new GT6MagicAbsorberBlock(BlockBehaviour.Properties.of()
						.strength(4.0F, 4.0F).sound(SoundType.METAL), () -> GT6MagicAbsorbers.MAGIC_ABSORBER_BE.get(),
						MAGIC_ABSORBER_MATERIAL)));
		MAGIC_ABSORBER_ITEMS_BY_PATH.put("magic_absorber", ITEMS.register("magic_absorber",
				() -> new BlockItem(MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get(),
						new Item.Properties().stacksTo(16))));
	}

	/**
	 * The BET — one BE class over the one block (the energy-source rig's single-type form).
	 */
	public static final RegistryObject<BlockEntityType<GT6MagicAbsorberBlockEntity>> MAGIC_ABSORBER_BE =
			BLOCK_ENTITY_TYPES.register("magic_absorber", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6MagicAbsorberBlockEntity(GT6MagicAbsorbers.MAGIC_ABSORBER_BE.get(), aPos, aState),
					MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get()).build(null));

	/**
	 * The absorber paint-tint walker (task p38-c2-controller-tint): the one block whose
	 * datagen model carries tintindex 0 on the body cube (the {@code paintableBlockArray}
	 * census convention), feeding BOTH consumption halves: the baked world tint
	 * ({@code GTMachineTintModel}, the p32 route) and the inventory {@code ItemColor}.
	 * Client-side call time only.
	 */
	public static Block[] paintableBlockArray() {
		return new Block[] {MAGIC_ABSORBER_BLOCKS_BY_PATH.get("magic_absorber").get()};
	}

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

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #MAGIC_ABSORBER_ITEMS_BY_PATH}
	 * family joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form,
	 * the class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display items,
	 * so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the machine its own "Magical Energy Production"
	 * category (tab 10180, Loader_MultiTileEntities.java:1005); this port pools the join
	 * into MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : MAGIC_ABSORBER_ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
