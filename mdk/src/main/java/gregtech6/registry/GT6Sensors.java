package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

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

import gregtech6.GT6Mod;
import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The sensor registration home (task p26-sensors-core, ADR-P3-4 card-owned) — the
 * GT6Attachments shape: self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters
 * attached from the construct event, the BET type rows appended in {@link GTBlockEntities}
 * (the cross-register resolution form; one BET per pioneer class, the CRANK_BE
 * single-mount shape — the three concrete BE classes are unrelated subtypes of the
 * abstract base, so the ADR-P3-1 one-type-many-blocks degenerates per class).
 *
 * <p>The three pioneer rows (Loader_MultiTileEntities.java:1995/:1986/:1997, category
 * "Sensors", host block aUtilMetal): Progressmeter 31018, Fluidometer 31006,
 * Electrometer 31015 — every live upstream column transcribed: hardness 1, resistance 16,
 * the metal sound (aUtilMetal → METAL). The remaining 16 of the 19 sensor rows are the
 * p26-sensors-batch2 pool card (the base is landed, each body ≈40 lines trivial) and
 * Thermometer/Tachometer/GeigerCounter/Laserometer stay pooled on their missing seams
 * (temperature/rotation/radiation/laser). The row ids and the CR.shapeless self-recast
 * crafting ride the recipe card; the "Sensors" creative tab is upstream's MTE-registry
 * category and stays out (the attachments precedent — /give-reachable, the tab system is
 * the pool card).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Sensors {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One sensor row: the upstream aRegistry.add columns, the block-carrier projection. */
	public record SensorRow(
			String path,          // the registry path (also the blockstate/model/lang key tail)
			long legacyId,        // the upstream 1.7.10 MTE id (31018/31006/31015)
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> tickerType) {

		/** The composed display key ({@code block.gt6.<path>}, the vanilla BlockItem naming). */
		public String displayKey() {
			return "block.gt6." + path();
		}
	}

	/** The three pioneer rows, upstream registration order :1995 → :1986 → :1997. */
	public static final List<SensorRow> ROWS = List.of(
			new SensorRow("progressmeter", 31018, () -> GTBlockEntities.PROGRESSMETER_BE.get()),
			new SensorRow("fluidometer"  , 31006, () -> GTBlockEntities.FLUIDOMETER_BE.get()),
			new SensorRow("electrometer" , 31015, () -> GTBlockEntities.ELECTROMETER_BE.get()));

	/** The blocks/BlockItems, one pair per row (the GT6Attachments static-block form). */
	public static final java.util.Map<String, RegistryObject<GTSensorBlock>> BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();
	public static final java.util.Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new java.util.LinkedHashMap<>();
	static {
		for (SensorRow tRow : ROWS) {
			final SensorRow fRow = tRow;
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTSensorBlock(fRow.tickerType(), BlockBehaviour.Properties.of()
							.strength(1.0F, 16.0F).sound(SoundType.METAL))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6Sensors.BLOCKS_BY_PATH.get(fRow.path()).get(), new Item.Properties())));
		}
	}

	private GT6Sensors() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Attachments shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Attachments fork).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6Attachments.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 sensors registered: {} pioneer rows ({} / {} blocks valid)",
				ROWS.size(), BLOCKS_BY_PATH.size(), ITEMS_BY_PATH.size()));
	}
}
