package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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

import gregtech6.GT6Mod;
import gregtech6.block.tank.GT6GasCylinderBlock;
import gregtech6.item.GT6GasCylinderBlockItem;
import gregtech6.tileentity.tank.GT6GasCylinderBlockEntity;

/**
 * The Barometer Gas Cylinder family registration (task small-tank-gas-cylinder) — the
 * ADR-P3-4 self-contained listener form (the GT6MeasuringPot/GTBarrels shape: a separate
 * class keeps the four small-tank card scopes disjoint). Ports the four "Fluid
 * Containers" rows Loader_MultiTileEntities.java:2101-2104 verbatim — the tier-S small
 * tank (Base09 no-sync: no fluid render pass, no BER):
 * <ul>
 * <li>Steel, id 32055, resistance 6.0, acid F / magic F;</li>
 * <li>Stainless, id 32056, resistance 6.0, acid T / magic F;</li>
 * <li>Tungsten, id 32057, resistance 10.0, acid T / magic T;</li>
 * <li>Tantalum Hafnium Carbide, id 32078, resistance 10.0, acid F / magic T.</li>
 * </ul>
 * Every row: NBT_HARDNESS 0.5, NBT_TANK_CAPACITY 8000, NBT_LIQUIDPROOF F +
 * NBT_GASPROOF T (the gas-only proof pair — the BE admission gate), aUtilMetal, stack
 * 16. The acid/magic columns have no port consumer (the GTBarrels P4 quartet pool cut)
 * and are recorded here only.
 *
 * <p>One BlockEntityType over the four blocks — the ADR-P3-1 shared-BET multi-mount
 * (all four rows are the one upstream TE class, the metal-drum shape).
 *
 * <p>Creative tab ownership: the rows' upstream category IS the "Fluid Containers" tab
 * GTBarrels owns — this card rides it through the BuildCreativeModeTabContentsEvent
 * append (the GT6MeasuringPot.onBuildTabContents form; GTBarrels.java stays untouched,
 * the card red line).
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (four Block/BlockItem pairs + one BET over the fluid-interaction
 * seam) and the datapack face (the four shaped-recipe JSONs + lang, naturally moddable
 * through the standard recipe events). NO KubeJS-specific seam ships; wiring a
 * RegistryObject-backed addon event for the four ids is the declared defer (the
 * GT6Bumbles.java:71-72 precedent shape).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6GasCylinders {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One row of the four — the upstream registration line flattened to its port-carried
	 * values. The material column feeds the crafting datagen ({@code OP.ring/plateCurved/
	 * round/screw/plate.dat(aMat)}, the :2101-2104 recipe tails); the :2103 row's upstream
	 * {@code ANY.W} column resolves to the dataset's concrete {@code MT.Tungsten} (the
	 * ANY pseudo-material carries no items — the tungsten-drum row's resolution, where the
	 * port items live). The acid/magic proof columns (F/T per row) ride the declared
	 * GTBarrels P4 quartet pool cut — recorded on the class javadoc, no port consumer.
	 */
	public record GasCylinderRow(String path, String displayName, float resistanceF, Supplier<gregapi.oredict.OreDictMaterial> material) {}

	/** The four rows in registration order (upstream :2101-2104; the en names are the name columns verbatim). */
	public static final List<GasCylinderRow> ROWS = List.of(
			new GasCylinderRow("gas_cylinder_steel", "Steel Barometer Gas Cylinder", 6.0F, () -> gregapi.data.MT.Steel),
			new GasCylinderRow("gas_cylinder_stainless_steel", "Stainless Barometer Gas Cylinder", 6.0F, () -> gregapi.data.MT.StainlessSteel),
			new GasCylinderRow("gas_cylinder_tungsten", "Tungsten Barometer Gas Cylinder", 10.0F, () -> gregapi.data.MT.Tungsten),
			new GasCylinderRow("gas_cylinder_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide Barometer Gas Cylinder", 10.0F, () -> gregapi.data.MT.Ta4HfC5));

	/** The blocks/BlockItems, one pair per row (the GTBarrels standalone-row form — four rows is no table loop). */
	public static final RegistryObject<GT6GasCylinderBlock> STEEL_CYLINDER = registerRow(ROWS.get(0));
	public static final RegistryObject<GT6GasCylinderBlock> STAINLESS_CYLINDER = registerRow(ROWS.get(1));
	public static final RegistryObject<GT6GasCylinderBlock> TUNGSTEN_CYLINDER = registerRow(ROWS.get(2));
	public static final RegistryObject<GT6GasCylinderBlock> TANTALUM_HAFNIUM_CARBIDE_CYLINDER = registerRow(ROWS.get(3));

	public static final List<RegistryObject<GT6GasCylinderBlock>> BLOCKS_IN_ORDER =
			List.of(STEEL_CYLINDER, STAINLESS_CYLINDER, TUNGSTEN_CYLINDER, TANTALUM_HAFNIUM_CARBIDE_CYLINDER);

	private static RegistryObject<GT6GasCylinderBlock> registerRow(GasCylinderRow aRow) {
		RegistryObject<GT6GasCylinderBlock> tBlock = BLOCKS.register(aRow.path(),
				() -> new GT6GasCylinderBlock(() -> GT6GasCylinders.GAS_CYLINDER_BE.get(), GT6GasCylinderBlock.rowProperties(aRow.resistanceF())));
		ITEMS.register(aRow.path(),
				() -> new GT6GasCylinderBlockItem(tBlock.get(), new Item.Properties().stacksTo(16)));
		return tBlock;
	}

	/**
	 * The family BET — the shared-BET multi-mount (ADR-P3-1): valid over all four rows,
	 * zero new BE classes (the metal-drum Stream form).
	 */
	public static final RegistryObject<BlockEntityType<GT6GasCylinderBlockEntity>> GAS_CYLINDER_BE =
			BLOCK_ENTITY_TYPES.register("gas_cylinder", () -> BlockEntityType.Builder.of(
					GT6GasCylinderBlockEntity::new,
					BLOCKS_IN_ORDER.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	/** The "Fluid Containers" tab ride (the rows' upstream category — the GTBarrels-owned tab, appended not owned). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTBarrels.FLUID_CONTAINERS_TAB.getId())) {
			for (RegistryObject<GT6GasCylinderBlock> tBlock : BLOCKS_IN_ORDER) {
				aEvent.accept(new ItemStack(tBlock.get().asItem()));
			}
		}
	}

	private GT6GasCylinders() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Kitchen fork form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework — the GT6Kitchen fork form.
		*///?}
		BLOCKS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6MeasuringPot.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 gas cylinders registered: {} (8000 L gas-only, the NBT_MODE limit face)",
					ForgeRegistries.BLOCKS.getKey(STEEL_CYLINDER.get()));
		});
	}
}
