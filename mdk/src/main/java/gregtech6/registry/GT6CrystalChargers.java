package gregtech6.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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

import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity;

/**
 * The Crystal Charger family registration (task p35-energy-tail-machines) — the twenty
 * rows of the upstream declared loop verbatim (Loader_MultiTileEntities.java:969-972,
 * {@code for (int i = 0; i < 10; i++)}): the small chargers 10130-10139 (4 slots) and
 * the Large chargers 10140-10149 (16 slots), every row NBT_INPUT = NBT_OUTPUT = V[0..9],
 * NBT_ENERGY_EMITTED LU, display "Crystal Charger (T0..T9)" / "Large Crystal Charger
 * (T0..T9)".
 *
 * <p>THE REUSE (the ponytail rung ②): upstream MultiTileEntityCrystalCharger(+Large)
 * are texture-only subclasses of TileEntityBase10EnergyBatBox — the behavior base the
 * BatteryBox family already transcribed into {@link GT6BatteryBoxBlockEntity}. The
 * charger = the SAME BE over an LU block column: {@link GT6BatteryBoxBlock} gains the
 * energy-domain supplier (EU = the battery boxes, LU = these chargers) and the BE
 * resolves it off the block state (the resolveTier form). The LU face charges and
 * drains the port's energium crystal items (GT6Batteries rows 14500-14515, TD.Energy.LU
 * — the same carriers upstream charges).
 *
 * <p>The crafting rows (:970-971: OD_CIRCUITS[i] + IL.Processor_Crystal_Emerald +
 * IL.FIELD_GENERATORS[i]) are DEFERRED to the crafting pool — the component items have
 * no port item rows (the QU-energizer card posture verbatim: "Crafting rows are the
 * crafting pool"). KJS surface: none (registration face deferred — the KJS binding pool).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CrystalChargers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One charger row — the upstream-parity columns of one :969-:972 loop iteration. */
	public record ChargerRow(String path, int metaId, int tier, String tierWord, int slots) {}

	/** The twenty rows, the loop order (:970 small T0..T9, :971 large T0..T9; tier = the V[i] index; the path suffix _t(i+1) for i≥1). */
	public static final java.util.List<ChargerRow> ROWS = java.util.List.of(
			new ChargerRow("crystal_charger"        , 10130, 0, "T0",  4),
			new ChargerRow("crystal_charger_t2"     , 10131, 1, "T1",  4),
			new ChargerRow("crystal_charger_t3"     , 10132, 2, "T2",  4),
			new ChargerRow("crystal_charger_t4"     , 10133, 3, "T3",  4),
			new ChargerRow("crystal_charger_t5"     , 10134, 4, "T4",  4),
			new ChargerRow("crystal_charger_t6"     , 10135, 5, "T5",  4),
			new ChargerRow("crystal_charger_t7"     , 10136, 6, "T6",  4),
			new ChargerRow("crystal_charger_t8"     , 10137, 7, "T7",  4),
			new ChargerRow("crystal_charger_t9"     , 10138, 8, "T8",  4),
			new ChargerRow("crystal_charger_t10"    , 10139, 9, "T9",  4),
			new ChargerRow("crystal_charger_large"     , 10140, 0, "T0", 16),
			new ChargerRow("crystal_charger_large_t2"  , 10141, 1, "T1", 16),
			new ChargerRow("crystal_charger_large_t3"  , 10142, 2, "T2", 16),
			new ChargerRow("crystal_charger_large_t4"  , 10143, 3, "T3", 16),
			new ChargerRow("crystal_charger_large_t5"  , 10144, 4, "T4", 16),
			new ChargerRow("crystal_charger_large_t6"  , 10145, 5, "T5", 16),
			new ChargerRow("crystal_charger_large_t7"  , 10146, 6, "T6", 16),
			new ChargerRow("crystal_charger_large_t8"  , 10147, 7, "T7", 16),
			new ChargerRow("crystal_charger_large_t9"  , 10148, 8, "T8", 16),
			new ChargerRow("crystal_charger_large_t10" , 10149, 9, "T9", 16));

	/** The charger blocks by registry path (the datagen/loot walk seat). */
	public static final java.util.Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	/** The charger items by registry path. */
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The 4-slot family BET (the ten small chargers, the battery-box split). */
	public static final RegistryObject<BlockEntityType<GT6BatteryBoxBlockEntity>> CHARGER_BE =
			BLOCK_ENTITY_TYPES.register("crystal_charger", () -> BlockEntityType.Builder.of(
					GT6BatteryBoxBlockEntity::new,
					ROWS.stream().filter(aRow -> aRow.slots() == 4)
							.map(aRow -> BLOCKS_BY_PATH.get(aRow.path()).get()).toArray(Block[]::new)).build(null));

	/** The 16-slot family BET (the ten Large chargers). */
	public static final RegistryObject<BlockEntityType<GT6BatteryBoxBlockEntity>> CHARGER_LARGE_BE =
			BLOCK_ENTITY_TYPES.register("crystal_charger_large", () -> BlockEntityType.Builder.of(
					GT6BatteryBoxBlockEntity::new,
					ROWS.stream().filter(aRow -> aRow.slots() == 16)
							.map(aRow -> BLOCKS_BY_PATH.get(aRow.path()).get()).toArray(Block[]::new)).build(null));

	static {
		for (ChargerRow tRow : ROWS) {
			int tTier = tRow.tier(), tSlots = tRow.slots();
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(), () -> new GT6BatteryBoxBlock(
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
							.strength(4.0F, 4.0F).sound(SoundType.METAL),
					tTier, tSlots, () -> tSlots == 16 ? CHARGER_LARGE_BE.get() : CHARGER_BE.get(),
					() -> gregapi.data.TD.Energy.LU))); // the NBT_ENERGY_EMITTED LU column (:970-:971)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new BlockItem(
					BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	private GT6CrystalChargers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ElectricDynamos fork form). */
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

	/** Registration smoke evidence (the GT6ElectricDynamos.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 crystal chargers registered: " + ROWS.size()
					+ " rows T0..T9 x small/large, LU domain over the BatBox transcription (ids 10130-10149, the p35 energy tail card)");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the family its own "Crystal Chargers" category
	 * (tab 10131, Loader_MultiTileEntities.java:969-972); this port pools the join into
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
