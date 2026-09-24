package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

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
 * the metal sound (aUtilMetal → METAL).
 *
	 * <p>CENSUS ERRATUM (task p34-sensors-trivial-14, coordinator ruling A): the upstream
	 * sensors() method registers 21 rows (Loader_MultiTileEntities.java:1979-1999, read line
	 * by line), not the 19 the P26/P34 census ledgers carried — the zh MTE dump tops out at
	 * 31022 with no 31023 row, which is where the undercount came from. The p34 batch
	 * appended 15 rows in the upstream anchor order (:1979 → :1994), POOLING the Tachometer
	 * 31019 / Geiger Counter 31020 / Laser-O-Meter 31021 rows on their then-missing seams.
	 * <p>POOL RESOLVED (task p37-sensors-3, 21/21): the p34 seam notes are the stale half —
	 * P28 built the kinetics (GTAxleBlockEntity mTransferredLast/mPower/mSpeed +
	 * GTGearBoxBlockEntity mMaxThroughPut/mTransferredLast) and P32 revived the LU carrier
	 * (GTWireBlockEntity mTransferredLast/isLaser), so the Tachometer and Laser-O-Meter
	 * ported whole; the Geiger Counter's reactor target (MultiTileEntityReactorCore,
	 * research.p37-gap-scan.s2 true-gap ①) is absent, and its declared non-reactor 0 arm
	 * IS the upstream :49/:65 behaviour verbatim (the reactor arm waits on the reactor
	 * card — the pre-left field face precedent). The CR.shapeless self-recast companions
	 * ride the ROWS walk automatically
	 * (GT6CraftingRecipes.sensorRecastBuilders — one 1:1 NBT-reset recast per row, the
	 * upstream per-row tails :1979-:1999); the SHAPED rows ride a recipe card (the
	 * electrometer precedent — its 'X' key is a dedicated GT6 item off the port path). The
	 * upstream "Sensors" MTE-registry category is pooled into the MACHINES_TAB join (task
	 * p38-tabfix-b-energy, {@link #onBuildTabContents}; the GTBarrels:257 pooling
	 * precedent — supersedes the old stay-out sentence).
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

	/**
	 * The 21 live rows: the three pioneers (upstream registration order :1995 → :1986 →
	 * :1997), the p34 batch of 15 in the upstream anchor order :1979 → :1994, then the
	 * p37 pool closure of 3 (the anchor's remaining rows in :1996 → :1998 → :1999 order —
	 * the appended subsequence is the anchor's row sequence verbatim, the census 钉测
	 * contract).
	 */
	public static final List<SensorRow> ROWS = List.of(
			new SensorRow("progressmeter"          , 31018, () -> GTBlockEntities.PROGRESSMETER_BE.get()),
			new SensorRow("fluidometer"            , 31006, () -> GTBlockEntities.FLUIDOMETER_BE.get()),
			new SensorRow("electrometer"           , 31015, () -> GTBlockEntities.ELECTROMETER_BE.get()),
			// p34-sensors-trivial-14 — Loader_MultiTileEntities.java:1979-1994 order
			new SensorRow("thermometer"            , 31000, () -> GTBlockEntities.THERMOMETER_BE.get()),            // :1979
			new SensorRow("luminometer"            , 31002, () -> GTBlockEntities.LUMINOMETER_BE.get()),            // :1980
			new SensorRow("chronometer"            , 31003, () -> GTBlockEntities.CHRONOMETER_BE.get()),            // :1981
			new SensorRow("gibblometer"            , 31001, () -> GTBlockEntities.GIBBLOMETER_BE.get()),            // :1982
			new SensorRow("kilogibblometer"        , 31023, () -> GTBlockEntities.KILOGIBBLOMETER_BE.get()),        // :1983
			new SensorRow("itemometer"             , 31004, () -> GTBlockEntities.ITEMOMETER_BE.get()),              // :1984
			new SensorRow("stackometer"            , 31005, () -> GTBlockEntities.STACKOMETER_BE.get()),             // :1985
			new SensorRow("bucketometer"           , 31007, () -> GTBlockEntities.BUCKETOMETER_BE.get()),            // :1987
			new SensorRow("kilobucketometer"       , 31022, () -> GTBlockEntities.KILOBUCKETOMETER_BE.get()),        // :1988
			new SensorRow("lightweightometer"      , 31010, () -> GTBlockEntities.LIGHTWEIGHTOMETER_BE.get()),       // :1989
			new SensorRow("mediumweightometer"     , 31011, () -> GTBlockEntities.MEDIUMWEIGHTOMETER_BE.get()),      // :1990
			new SensorRow("heavyweightometer"      , 31012, () -> GTBlockEntities.HEAVYWEIGHTOMETER_BE.get()),       // :1991
			new SensorRow("superheavyweightometer" , 31013, () -> GTBlockEntities.SUPERHEAVYWEIGHTOMETER_BE.get()),  // :1992
			new SensorRow("tpsmeter"               , 31016, () -> GTBlockEntities.TPSMETER_BE.get()),                // :1993
			new SensorRow("playercounter"          , 31017, () -> GTBlockEntities.PLAYERCOUNTER_BE.get()),           // :1994
			// p37-sensors-3 — the pool closure, the anchor's remaining rows (:1996/:1998/:1999)
			new SensorRow("geigercounter"          , 31020, () -> GTBlockEntities.GEIGERCOUNTER_BE.get()),           // :1996
			new SensorRow("tachometer"             , 31019, () -> GTBlockEntities.TACHOMETER_BE.get()),              // :1998
			new SensorRow("laserometer"            , 31021, () -> GTBlockEntities.LASEROMETER_BE.get()));            // :1999

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
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 sensors registered: {} sensor rows ({} / {} blocks valid)",
				ROWS.size(), BLOCKS_BY_PATH.size(), ITEMS_BY_PATH.size()));
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the family its own "Sensors" category
	 * (Loader_MultiTileEntities.java:1979-1999); this port pools the join into
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
