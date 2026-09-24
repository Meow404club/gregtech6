package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
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

import gregapi.data.TD;
import gregtech6.block.energy.GT6ZpmDechargerBlock;
import gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity;

/**
 * The ZPM Decharger family registration (task p36-energy-zpm-dechargers) — the two
 * Loader_MultiTileEntities.java:1000-:1001 rows verbatim:
 * <ul>
 * <li>{@code zpm_decharger_quantum} — meta 11170, "ZPM Decharger (Quantum)",
 *     {@code NBT_ENERGY_ACCEPTED QU, NBT_ENERGY_EMITTED QU}; recipe 'P' column
 *     {@code IL.Processor_Crystal_Ruby};</li>
 * <li>{@code zpm_decharger_electric} — meta 11171, "ZPM Decharger (Electric)",
 *     {@code NBT_ENERGY_ACCEPTED QU, NBT_ENERGY_EMITTED EU}; recipe 'P' column
 *     {@code IL.Processor_Crystal_Sapphire};</li>
 * </ul>
 * shared columns: NBT_INPUT = NBT_OUTPUT = V[7] (131072), NBT_INV_SIZE 1, hardness 4.0 /
 * resistance 50.0, Osmiridium casing. The BE is ONE class over both blocks (upstream the
 * two MTE classes are TEXTURE-ONLY siblings over the same BatBox — the port folds the
 * pair, the out-type rides the block column; the dedicated textures are the render pool,
 * the p36-render-texture-bake card).
 *
 * <p>The crafting rows (:1000-:1001 recipe strings: circuits[6] + the tier-6 crystal
 * processors + FIELD_GENERATORS[6] + casingMachineDense) are DEFERRED to the crafting
 * pool — the component items have no port item rows (the p35 charger posture verbatim);
 * obtainment rides the creative inventory through the MACHINES_TAB join (task
 * p38-tabfix-b-energy, {@link #onBuildTabContents} — the dechargers had registered with
 * zero tab membership, so the "creative inventory" sentence was unreachable until the
 * join landed; the ZPM itself rides the dungeon injection, GT6LootInjectionDatagen).
 * KJS surface: none (registration face deferred — the KJS binding pool).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ZpmDechargers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One decharger row — the :1000-:1001 registration columns (the BatteryRow type-supplier form). */
	public record DechargerRow(String path, int metaId, String enName, int tier, int slots,
			java.util.function.Supplier<gregapi.code.TagData> outType) {}

	/** The two rows, the :999-:1001 order (the out lane: :1000 QU, :1001 EU). */
	public static final List<DechargerRow> ROWS = List.of(
			new DechargerRow("zpm_decharger_quantum", 11170, "ZPM Decharger (Quantum)", 7, 1, () -> TD.Energy.QU),
			new DechargerRow("zpm_decharger_electric", 11171, "ZPM Decharger (Electric)", 7, 1, () -> TD.Energy.EU));

	/** The in lane — both rows accept QU (the ZPM's only language, the NBT_ENERGY_ACCEPTED column). */
	public static java.util.function.Supplier<gregapi.code.TagData> inType(DechargerRow aRow) {
		return () -> TD.Energy.QU;
	}

	/** The blocks by registry path (the datagen/test lookup seat). */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();
	/** The items by registry path. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The single BET over the two blocks (the texture-only-sibling fold). */
	public static final RegistryObject<BlockEntityType<GT6ZpmDechargerBlockEntity>> ZPM_DECHARGER_BE =
			BLOCK_ENTITY_TYPES.register("zpm_decharger", () -> BlockEntityType.Builder.of(
					GT6ZpmDechargerBlockEntity::new,
					ROWS.stream().map(aRow -> BLOCKS_BY_PATH.get(aRow.path()).get()).toArray(Block[]::new)).build(null));

	static {
		for (DechargerRow tRow : ROWS) {
			int tTier = tRow.tier(), tSlots = tRow.slots();
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(), () -> new GT6ZpmDechargerBlock(
					BlockBehaviour.Properties.of().strength(4.0F, 50.0F).sound(SoundType.METAL), // NBT_HARDNESS/RESISTANCE 4.0/50.0
					tTier, tSlots, ZPM_DECHARGER_BE::get, inType(tRow), tRow.outType())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new BlockItem(
					BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	private GT6ZpmDechargers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6CrystalChargers fork form). */
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

	/** Registration smoke evidence (the GT6CrystalChargers.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 ZPM dechargers registered: " + ROWS.size()
					+ " rows (quantum QU->QU / electric QU->EU), V[7] packets over the BatBox transcription (ids 11170-11171, the p36 energy tail card)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the family its own "ZPM" category (tab 14999,
	 * Loader_MultiTileEntities.java:1000-1001); this port pools the join into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
