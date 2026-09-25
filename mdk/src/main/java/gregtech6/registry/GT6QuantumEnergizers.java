package gregtech6.registry;

import java.util.ArrayList;
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
import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.tileentity.energy.converters.GT6LaserConverterBlockEntity;

/**
 * The Quantum Energizer registration (task p32-qu-energizer) — the card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the {@link GT6Lasers} shape (ADR-P3-4) over the REUSED {@link GT6DynamoBlock} carrier
 * and the REUSED {@link GT6LaserConverterBlockEntity} core: the energizer is the THIRD
 * type-pair instance of the laser converter family — LU in / QU out, input BACK only
 * (MultiTileEntityQuantumEnergizerLaser :36 {@code mFacing == OPOS[aSide]}), output FRONT
 * (:37). Loader_MultiTileEntities.java:961-966, ids 10121-10125, all five rows
 * MT.Osmiridium with NBT_HARDNESS/RESISTANCE <b>16.0</b> (the laser rows' 4.0 doubled
 * twice — the column is carried here, not inherited from the laser registration), stack
 * 16, {@code NBT_WASTE_ENERGY, T}, NBT_ENERGY_ACCEPTED TD.Energy.LU, NBT_ENERGY_EMITTED
 * TD.Energy.QU.
 *
 * <p>The ladder columns are the SHARED laser ladder verbatim (Loader :962-966: in
 * {32, 128, 512, 2048, 8192}, out {16, 64, 256, 1024, 4096} — numerically the laser
 * rows' columns; the BE constructor already resolves the tier through
 * {@link GT6Lasers#LASER_INPUTS}/{@link GT6Lasers#LASER_OUTPUTS}, so no second array
 * exists). The row record is {@link GT6Lasers.LaserRow} reused; its display-word slot
 * carries the QUANTUM word T1..T5 (the upstream name column is "Quantum Energizer (T)",
 * NOT a voltage word — the massfab/exotic family posture).
 *
 * <p>The QU consumer side: the small Massfab rows take QU on the back face with the SAME
 * band ladder (GTMachines.massfabSmall, TIER_INPUTS[0..3] + EV_TIER_INPUTS) — the
 * energizer's out column rides every rung's inMin door exactly (the RCON chain face).
 * Crafting rows are the crafting pool (the laser posture — the FIELD_GENERATORS/SENSORS/
 * EMITTERS component items have no port item rows; the row shape CFC/SME/CFC with the
 * per-tier component index is pinned by the offline test for the pool card to consume).
 * KJS surface: REGISTRATION face only, deferred to the KJS binding card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6QuantumEnergizers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** The Quantum Energizer rows, upstream line order :961-966 (ids 10121-10125); the word slot = the (T1..T5) display word. */
	public static final List<GT6Lasers.LaserRow> QUANTUM_ENERGIZER_ROWS;

	/** The registered Quantum Energizer blocks by path (the BET/datagen/loot walkers). */
	public static final Map<String, RegistryObject<Block>> QUANTUM_ENERGIZER_BLOCKS_BY_PATH = new LinkedHashMap<>();
	/** The registered Quantum Energizer items, same keys. */
	public static final Map<String, RegistryObject<Item>> QUANTUM_ENERGIZER_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		List<GT6Lasers.LaserRow> tRows = new ArrayList<>();
		tRows.add(new GT6Lasers.LaserRow("quantum_energizer", 10121, 0, "T1", null));
		tRows.add(new GT6Lasers.LaserRow("quantum_energizer_t2", 10122, 1, "T2", null));
		tRows.add(new GT6Lasers.LaserRow("quantum_energizer_t3", 10123, 2, "T3", null));
		tRows.add(new GT6Lasers.LaserRow("quantum_energizer_t4", 10124, 3, "T4", null));
		tRows.add(new GT6Lasers.LaserRow("quantum_energizer_t5", 10125, 4, "T5", null));
		QUANTUM_ENERGIZER_ROWS = List.copyOf(tRows);
	}

	static {
		registerEnergizerFamily(QUANTUM_ENERGIZER_ROWS, QUANTUM_ENERGIZER_BLOCKS_BY_PATH, QUANTUM_ENERGIZER_ITEMS_BY_PATH,
				() -> GT6QuantumEnergizers.QUANTUM_ENERGIZER_BE);
	}

	/**
	 * One family's block+item registration walk — the {@link GT6Lasers} form over the
	 * REUSED GT6DynamoBlock carrier; the Osmiridium 16.0/16.0 strength is the Loader
	 * :962-966 NBT_HARDNESS/RESISTANCE column (the laser's 4.0 is NOT shared).
	 */
	private static void registerEnergizerFamily(List<GT6Lasers.LaserRow> aRows,
			Map<String, RegistryObject<Block>> aBlocks, Map<String, RegistryObject<Item>> aItems,
			java.util.function.Supplier<RegistryObject<BlockEntityType<GT6LaserConverterBlockEntity>>> aBe) {
		for (GT6Lasers.LaserRow tRow : aRows) {
			aBlocks.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6DynamoBlock(BlockBehaviour.Properties.of()
							.strength(16.0F, 16.0F).sound(SoundType.METAL), tRow.tier(), () -> aBe.get().get(), tRow.material())));
			aItems.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(aBlocks.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/** The Quantum Energizer family BET — the LU→QU pair captured per factory, back-input (the absorber faces). */
	public static final RegistryObject<BlockEntityType<GT6LaserConverterBlockEntity>> QUANTUM_ENERGIZER_BE =
			BLOCK_ENTITY_TYPES.register("quantum_energizer", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6LaserConverterBlockEntity(GT6QuantumEnergizers.QUANTUM_ENERGIZER_BE.get(),
							TD.Energy.LU, TD.Energy.QU, true, aPos, aState),
					energizerBlockArray(QUANTUM_ENERGIZER_ROWS, QUANTUM_ENERGIZER_BLOCKS_BY_PATH)).build(null));

	/** The block list of the family in registration order (the BET varargs). */
	private static Block[] energizerBlockArray(List<GT6Lasers.LaserRow> aRows, Map<String, RegistryObject<Block>> aBlocks) {
		Block[] rBlocks = new Block[aRows.size()];
		for (int i = 0; i < rBlocks.length; i++) rBlocks[i] = aBlocks.get(aRows.get(i).path()).get();
		return rBlocks;
	}

	private GT6QuantumEnergizers() {}

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

	/** Registration smoke evidence (the GT6Lasers.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			gregtech6.GT6Mod.LOGGER.info("GT6 quantum energizers registered: 5 rows (10121-10125, LU->QU back-in/front-out, osmiridium 16.0), task p32-qu-energizer");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #QUANTUM_ENERGIZER_ITEMS_BY_PATH}
	 * family joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form,
	 * the class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display items,
	 * so registered-but-tab-less was invisible in both the creative menu and JEI. Pool-cut
	 * declaration: upstream gives the family its own "Quantum Energizers" category (tab
	 * 10121, Loader_MultiTileEntities.java:961-966); this port pools the join into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : QUANTUM_ENERGIZER_ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
