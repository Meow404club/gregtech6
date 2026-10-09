package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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
import gregtech6.block.tank.GT6MeasuringPotBlock;
import gregtech6.tileentity.tank.GT6MeasuringPotBlockEntity;

/**
 * The Measuring Pot family registration (task issue45-c3, issue #45) — the ADR-P3-4
 * self-contained listener form (the GT6Kitchen/GT6Tanks shape: a separate class keeps the
 * parallel issue-#45 card scopes disjoint — C1 owns GT6Kitchen, C2 owns GT6Crucibles).
 * Ports the "Fluid Containers" Measuring Pot band Loader_MultiTileEntities.java:2096-2099
 * verbatim — the ceramic row :2096 ("Ceramic Measuring Pot", id 32738, hardness 0.5 /
 * resistance 6.0, aUtilStone, NBT_TANK_CAPACITY 1000) plus the THREE metal rows
 * (task measuring-pot-variants, the :2097-2099 "Ch"/"Pf" plateCurved crafting band):
 * <ul>
 * <li>:2097 StainlessSteel, id 32743 — resistance 6.0, ACIDPROOF T / LIQUIDPROOF T /
 *     GASPROOF F / MAGICPROOF F, TEMPERATURE mMeltingPoint-50, aUtilMetal;</li>
 * <li>:2098 ANY.W, id 32744 — resistance 10.0, ACIDPROOF T / LIQUIDPROOF T /
 *     GASPROOF F / MAGICPROOF T, TEMPERATURE mMeltingPoint-50, aUtilMetal;</li>
 * <li>:2099 Ta4HfC5, id 32077 — resistance 10.0, ACIDPROOF F / LIQUIDPROOF T /
 *     GASPROOF F / MAGICPROOF T, TEMPERATURE mMeltingPoint-50, aUtilMetal.</li>
 * </ul>
 * Every row carries NBT_HARDNESS 0.5 + NBT_TANK_CAPACITY 1000 (the one BE class default).
 * The proof/temperature columns have no port consumer (the barrel-base pool cut, the
 * {@link GT6GasCylinders} ruling) and are recorded here only.
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (four Block/BlockItem pairs + one BET over them + one raw Item) and
 * the datapack crafting face (tier-a JSON, naturally moddable). NO KubeJS-specific seam
 * ships; wiring a RegistryObject-backed addon event for the pot ids is the declared
 * defer.
 *
 * <p>The raw item {@link #CLAY_MEASURING_POT_RAW} is upstream meta 997 "Clay Measuring
 * Pot" (MultiItemRandomTools.java:121, {@code OreDictItemData(MT.Clay, U*4)}); the shaped
 * :134 row ("CkC"/"CCR" over clay + the knife/rolling-pin tool marks, 4 clay = U*4), the
 * reverse shapeless (:121 — raw → 4 clay balls) and the :2096 smelting tail
 * ({@code RM.add_smelting(Measuring_Pot_Raw, Measuring_Pot)}) ride the crafting/smelting
 * datagen (GT6MeasuringPotDatagen). The raw item joins the machines tab (the kitchen
 * family law — the CLAY_BOWL_RAW row, GT6Kitchen.onBuildTabContents).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MeasuringPot {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The ceramic pot block — the Loader :2096 row (the properties/carrier live on the block). */
	public static final RegistryObject<GT6MeasuringPotBlock> MEASURING_POT = BLOCKS.register("measuring_pot",
			() -> new GT6MeasuringPotBlock(() -> GT6MeasuringPot.MEASURING_POT_BE.get(), GT6MeasuringPotBlock.potProperties()));

	/**
	 * One metal row — the upstream registration line flattened to its port-carried values
	 * (the {@link GT6GasCylinders.GasCylinderRow} form). The material column feeds the
	 * crafting datagen ({@code OP.plate/plateCurved.dat(aMat)}, the :2097-2099 "Ch"/"Pf"
	 * tails); the :2098 row's upstream {@code ANY.W} column resolves to the dataset's
	 * concrete {@code MT.Tungsten} (the gas-cylinder-row resolution, where the port items
	 * live). The proof/temperature columns ride the declared barrel-base pool cut —
	 * recorded on the class javadoc, no port consumer.
	 */
	public record PotRow(String path, String displayName, float resistanceF, Supplier<gregapi.oredict.OreDictMaterial> material) {}

	/** The three metal rows in registration order (upstream :2097-2099; the en names are the name columns verbatim). */
	public static final List<PotRow> ROWS = List.of(
			new PotRow("measuring_pot_stainless_steel", "Stainless Measuring Pot", 6.0F, () -> gregapi.data.MT.StainlessSteel), // :2097
			new PotRow("measuring_pot_tungsten", "Tungsten Measuring Pot", 10.0F, () -> gregapi.data.MT.Tungsten), // :2098 — ANY.W
			new PotRow("measuring_pot_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide Measuring Pot", 10.0F, () -> gregapi.data.MT.Ta4HfC5)); // :2099

	private static RegistryObject<GT6MeasuringPotBlock> registerRow(PotRow aRow) {
		RegistryObject<GT6MeasuringPotBlock> tBlock = BLOCKS.register(aRow.path(),
				() -> new GT6MeasuringPotBlock(() -> GT6MeasuringPot.MEASURING_POT_BE.get(),
						GT6MeasuringPotBlock.variantProperties(aRow.resistanceF())));
		ITEMS.register(aRow.path(),
				() -> new BlockItem(tBlock.get(), new Item.Properties()));
		return tBlock;
	}

	/** The :2097 row (aUtilMetal, resistance 6.0). */
	public static final RegistryObject<GT6MeasuringPotBlock> STAINLESS_POT = registerRow(ROWS.get(0));
	/** The :2098 row (aUtilMetal, resistance 10.0, magicproof). */
	public static final RegistryObject<GT6MeasuringPotBlock> TUNGSTEN_POT = registerRow(ROWS.get(1));
	/** The :2099 row (aUtilMetal, resistance 10.0, magicproof, acidproof F). */
	public static final RegistryObject<GT6MeasuringPotBlock> TANTALUM_HAFNIUM_CARBIDE_POT = registerRow(ROWS.get(2));

	/** The three metal blocks in row order (the datagen walks). */
	public static final List<RegistryObject<GT6MeasuringPotBlock>> VARIANT_BLOCKS =
			List.of(STAINLESS_POT, TUNGSTEN_POT, TANTALUM_HAFNIUM_CARBIDE_POT);

	/** All FOUR rows in registration order — ceramic first (Loader :2096-2099; the BET multi-mount + the tab join). */
	public static final List<RegistryObject<GT6MeasuringPotBlock>> BLOCKS_IN_ORDER =
			List.of(MEASURING_POT, STAINLESS_POT, TUNGSTEN_POT, TANTALUM_HAFNIUM_CARBIDE_POT);

	/** The pot BET — one BlockEntityType over ALL FOUR rows (the shared-BET multi-mount, the ADR-P3-1 gas-cylinder form). */
	public static final RegistryObject<BlockEntityType<GT6MeasuringPotBlockEntity>> MEASURING_POT_BE =
			BLOCK_ENTITY_TYPES.register("measuring_pot", () -> BlockEntityType.Builder.of(
					GT6MeasuringPotBlockEntity::new,
					BLOCKS_IN_ORDER.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	/** The pot BlockItem (plain — the family ships no item-capability face, the kitchen form). */
	public static final RegistryObject<Item> MEASURING_POT_ITEM = ITEMS.register("measuring_pot",
			() -> new BlockItem(MEASURING_POT.get(), new Item.Properties()));

	/**
	 * The Clay Measuring Pot raw item — upstream MultiItemRandomTools.java:121 ("Clay
	 * Measuring Pot", "Put in Furnace to harden", {@code OreDictItemData(MT.Clay, U*4)}).
	 * Craft-only acquisition upstream (the shaped :134 row); the smelt hardens it into
	 * {@link #MEASURING_POT_ITEM}.
	 */
	public static final RegistryObject<Item> CLAY_MEASURING_POT_RAW = ITEMS.register("clay_measuring_pot",
			() -> new Item(new Item.Properties()));

	/** The MACHINES-TAB join (the kitchen family law — GT6Kitchen.onBuildTabContents form; all four rows + the raw). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<GT6MeasuringPotBlock> tBlock : BLOCKS_IN_ORDER) {
				aEvent.accept(new ItemStack(tBlock.get().asItem())); // the gas-cylinder walk form
			}
			aEvent.accept(new ItemStack(CLAY_MEASURING_POT_RAW.get()));
		}
	}

	private GT6MeasuringPot() {}

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

	/** Registration smoke evidence (the GT6Kitchen.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 measuring pot registered: {} ({} L default limit) + {} (the 997 raw)",
					ForgeRegistries.BLOCKS.getKey(MEASURING_POT.get()), GT6MeasuringPotBlockEntity.DEFAULT_CAPACITY,
					ForgeRegistries.ITEMS.getKey(CLAY_MEASURING_POT_RAW.get()));
		});
	}
}
