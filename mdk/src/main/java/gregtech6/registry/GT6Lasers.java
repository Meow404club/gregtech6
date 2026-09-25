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
 * The Laser domain registration (task p32-qu-laser-domain) — the card-owned
 * {@code @EventBusSubscriber(MOD)} DeferredRegisters attached from the construct event,
 * the GT6ElectricTransformers shape (ADR-P3-4). TWO families over the REUSED
 * {@link GT6DynamoBlock} carrier (the p29-w4-eu-bridge posture — one block per rung, the
 * tier index rides the block, the FACING property carries the emission face):
 * <ul>
 * <li><b>Electric CO2 Laser</b> — Loader_MultiTileEntities.java:930-934, ids 10101-10105,
 *     display "Electric CO2 Laser (LV..IV)", EU in / LU out, input all-but-front.</li>
 * <li><b>Laser Absorber</b> — :976-980, ids 10151-10155, display "Laser Absorber
 *     (LV..IV)", LU in / EU out, input back only.</li>
 * </ul>
 *
 * <p>The row columns are the SHARED ladder (NBT_INPUT of one family IS the NBT_OUTPUT of
 * the other, all ten rows): {32, 128, 512, 2048, 8192} → {16, 64, 256, 1024, 4096}, and
 * every row carries {@code NBT_WASTE_ENERGY, T} — the lossy-by-design converter chain
 * (the BE class doc carries the math anchors). The upstream rows also pin
 * hardness/resistance 4.0/4.0 and stack 16 (the aRegistry.add columns).
 *
 * <p>NOT done here (the task card cuts, declared): the Buildcraft Assembly Laser and the
 * Flux Laser (the p28-cut-eu-fe-bridge RF/BC crop precedent), the Laserometer, the laser
 * beam rendering (visual = the static block face; the beam defer rides the ore_OVERLAY POC
 * verdict). Crafting rows are the crafting pool (the usb-data posture — the machines stay
 * RCON/test-obtainable until the pool card lands). KJS surface: REGISTRATION face only,
 * deferred to the KJS binding card.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Lasers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/** One ladder row — the upstream-parity columns of one Loader aRegistry.add line; {@code material} the row's NBT_MATERIAL aMat (task p38-c2-controller-tint, the Electric_T ladder via {@code GTMachines.electricTierMat}). */
	public record LaserRow(String path, int metaId, int tier, String voltageWord,
			java.util.function.Supplier<gregapi.oredict.OreDictMaterial> material) {}

	/** The shared NBT_INPUT / NBT_OUTPUT ladder of both families (Loader :930-934/:976-980 verbatim). */
	public static final long[] LASER_INPUTS = {32, 128, 512, 2048, 8192};
	/** The shared output ladder — in/2 on every rung (the units() half-rate, the bridge shape). */
	public static final long[] LASER_OUTPUTS = {16, 64, 256, 1024, 4096};

	/** The voltage words of the five rungs (upstream VN[1..5], the bridge word set). */
	public static final List<String> LASER_VOLTAGE_WORDS = List.of("LV", "MV", "HV", "EV", "IV");

	/** The CO2 Laser rows, upstream line order :930-934 (ids 10101-10105). */
	public static final List<LaserRow> CO2_LASER_ROWS;
	/** The Laser Absorber rows, upstream line order :976-980 (ids 10151-10155). */
	public static final List<LaserRow> LASER_ABSORBER_ROWS;

	/** The registered CO2 Laser blocks by path (the BET/datagen/loot walkers). */
	public static final Map<String, RegistryObject<Block>> CO2_LASER_BLOCKS_BY_PATH = new LinkedHashMap<>();
	/** The registered CO2 Laser items, same keys. */
	public static final Map<String, RegistryObject<Item>> CO2_LASER_ITEMS_BY_PATH = new LinkedHashMap<>();
	/** The registered Laser Absorber blocks by path. */
	public static final Map<String, RegistryObject<Block>> LASER_ABSORBER_BLOCKS_BY_PATH = new LinkedHashMap<>();
	/** The registered Laser Absorber items, same keys. */
	public static final Map<String, RegistryObject<Item>> LASER_ABSORBER_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		List<LaserRow> tRows = new ArrayList<>();
		tRows.add(new LaserRow("co2_laser", 10101, 0, "LV", () -> GTMachines.electricTierMat(0)));
		tRows.add(new LaserRow("co2_laser_t2", 10102, 1, "MV", () -> GTMachines.electricTierMat(1)));
		tRows.add(new LaserRow("co2_laser_t3", 10103, 2, "HV", () -> GTMachines.electricTierMat(2)));
		tRows.add(new LaserRow("co2_laser_t4", 10104, 3, "EV", () -> GTMachines.electricTierMat(3)));
		tRows.add(new LaserRow("co2_laser_t5", 10105, 4, "IV", () -> GTMachines.electricTierMat(4)));
		CO2_LASER_ROWS = List.copyOf(tRows);
		tRows = new ArrayList<>();
		tRows.add(new LaserRow("laser_absorber", 10151, 0, "LV", () -> GTMachines.electricTierMat(0)));
		tRows.add(new LaserRow("laser_absorber_t2", 10152, 1, "MV", () -> GTMachines.electricTierMat(1)));
		tRows.add(new LaserRow("laser_absorber_t3", 10153, 2, "HV", () -> GTMachines.electricTierMat(2)));
		tRows.add(new LaserRow("laser_absorber_t4", 10154, 3, "EV", () -> GTMachines.electricTierMat(3)));
		tRows.add(new LaserRow("laser_absorber_t5", 10155, 4, "IV", () -> GTMachines.electricTierMat(4)));
		LASER_ABSORBER_ROWS = List.copyOf(tRows);
	}

	static {
		registerLaserFamily(CO2_LASER_ROWS, CO2_LASER_BLOCKS_BY_PATH, CO2_LASER_ITEMS_BY_PATH, () -> GT6Lasers.CO2_LASER_BE);
		registerLaserFamily(LASER_ABSORBER_ROWS, LASER_ABSORBER_BLOCKS_BY_PATH, LASER_ABSORBER_ITEMS_BY_PATH, () -> GT6Lasers.LASER_ABSORBER_BE);
	}

	/**
	 * One family's block+item registration walk — the registerBridgeFamily form over the
	 * REUSED GT6DynamoBlock carrier (hardness/resistance 4.0 = the NBT_HARDNESS/RESISTANCE
	 * columns, stack 16 = the upstream stack column).
	 */
	private static void registerLaserFamily(List<LaserRow> aRows,
			Map<String, RegistryObject<Block>> aBlocks, Map<String, RegistryObject<Item>> aItems,
			java.util.function.Supplier<RegistryObject<BlockEntityType<GT6LaserConverterBlockEntity>>> aBe) {
		for (LaserRow tRow : aRows) {
			aBlocks.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6DynamoBlock(BlockBehaviour.Properties.of()
							.strength(4.0F, 4.0F).sound(SoundType.METAL), tRow.tier(), () -> aBe.get().get(), tRow.material())));
			aItems.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(aBlocks.get(tRow.path()).get(), new Item.Properties().stacksTo(16))));
		}
	}

	/** The CO2 Laser family BET — one BE class over the five laser rungs, the EU→LU type pair captured per factory. */
	public static final RegistryObject<BlockEntityType<GT6LaserConverterBlockEntity>> CO2_LASER_BE =
			BLOCK_ENTITY_TYPES.register("co2_laser", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6LaserConverterBlockEntity(GT6Lasers.CO2_LASER_BE.get(),
							TD.Energy.EU, TD.Energy.LU, false, aPos, aState),
					laserBlockArray(CO2_LASER_ROWS, CO2_LASER_BLOCKS_BY_PATH)).build(null));

	/** The Laser Absorber family BET (the LU→EU pair, back-input). */
	public static final RegistryObject<BlockEntityType<GT6LaserConverterBlockEntity>> LASER_ABSORBER_BE =
			BLOCK_ENTITY_TYPES.register("laser_absorber", () -> BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6LaserConverterBlockEntity(GT6Lasers.LASER_ABSORBER_BE.get(),
							TD.Energy.LU, TD.Energy.EU, true, aPos, aState),
					laserBlockArray(LASER_ABSORBER_ROWS, LASER_ABSORBER_BLOCKS_BY_PATH)).build(null));

	/** The block list of one family in registration order (the BET varargs). */
	private static Block[] laserBlockArray(List<LaserRow> aRows, Map<String, RegistryObject<Block>> aBlocks) {
		Block[] rBlocks = new Block[aRows.size()];
		for (int i = 0; i < rBlocks.length; i++) rBlocks[i] = aBlocks.get(aRows.get(i).path()).get();
		return rBlocks;
	}

	/**
	 * The laser-domain paint-tint walker (task p38-c2-controller-tint): the 10 ladder
	 * blocks (CO2 Laser :930-934 + Laser Absorber :976-980) whose datagen models carry
	 * tintindex 0 on the body cube (the {@code paintableBlockArray} census convention),
	 * feeding BOTH consumption halves: the baked world tint ({@code GTMachineTintModel},
	 * the p32 route) and the inventory {@code ItemColor}. Client-side call time only.
	 * NOT in the walk: the quantum energizer (the derived pre-tinted amber art — the
	 * card exemption).
	 */
	public static Block[] paintableBlockArray() {
		Block[] rBlocks = new Block[CO2_LASER_ROWS.size() + LASER_ABSORBER_ROWS.size()];
		int i = 0;
		for (LaserRow tRow : CO2_LASER_ROWS) rBlocks[i++] = CO2_LASER_BLOCKS_BY_PATH.get(tRow.path()).get();
		for (LaserRow tRow : LASER_ABSORBER_ROWS) rBlocks[i++] = LASER_ABSORBER_BLOCKS_BY_PATH.get(tRow.path()).get();
		return rBlocks;
	}

	private GT6Lasers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ElectricTransformers fork form). */
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
			gregtech6.GT6Mod.LOGGER.info("GT6 laser domain registered: 5 CO2 Laser rows (10101-10105, EU->LU) + 5 Laser Absorber rows (10151-10155, LU->EU), task p32-qu-laser-domain");
		});
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — both item maps join the machines tab; the
	 * GT6BurningBoxes.onBuildTabContents verbatim form, the class-level MOD-bus
	 * {@code @Mod.EventBusSubscriber} at the class head is what delivers this handler).
	 * JEI 1.20.1 derives its item list from the tab display items, so registered-but-
	 * tab-less was invisible in both the creative menu and JEI. Pool-cut declaration:
	 * upstream gives the CO2 Lasers their own "Lasers" category (tab 10071,
	 * Loader_MultiTileEntities.java:930-934) and the absorbers their own "Laser
	 * Absorbers" category (tab 10151, :976-980); this port pools both joins into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : CO2_LASER_ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
			for (RegistryObject<Item> tItem : LASER_ABSORBER_ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
