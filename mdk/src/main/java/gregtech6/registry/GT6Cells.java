package gregtech6.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.block.tank.GT6CellBlock;
import gregtech6.item.GT6CellBlockItem;
import gregtech6.tileentity.tank.GT6CellBlockEntity;

/**
 * The Capsule-Cell-Container family registration (task small-tank-cell) — the
 * ADR-P3-4 self-contained listener form (the GT6GasCylinders shape: a separate class
 * keeps the small-tank card scopes disjoint). Ports the 40 "Fluid Containers" rows
 * Loader_MultiTileEntities.java:1770-1809 verbatim in registration order (ids
 * 32600-32639) — the tier-SM small tank (Base10 sync: the fill-level display face).
 * EVERY row: NBT_HARDNESS 0.5, NBT_RESISTANCE 6.0, NBT_TANK_CAPACITY 1000,
 * NBT_PLASMAPROOF F, NBT_GASPROOF T, stack 64 — only the host block (aUtilWood on
 * the nine wax/plastic rows, aUtilMetal on the 31 metal rows), the acid/magic/plasma
 * columns and NBT_TEMPERATURE vary, and the nine aUtilWood rows are the first nine.
 *
 * <p><b>The acquisition row is DECLARED DEFERRED</b>: upstream the family is made in
 * the extruder (Loader_Recipes_Handlers.java:766 {@code addExtruderRecipe(tInput,
 * capcellcon, T, IL.Shape_Extruder_CCC)} + :799 the {@code Shape_SimpleEx_CCC} simple
 * twin — the mAmount-ratio helper :809-823, so 1 ingot-tier input → the capcellcon U9
 * ratio, i.e. 8 empty cells per ingot). The port still has NO CCC extruder mold item
 * (re-verified 2026-10-03: GT6ExtruderMolds carries the row0 plate+rod pair, and the
 * in-flight toolhead-r11c-extruder-heads card adds the 16 tool-head molds — the CCC
 * pair stays zero-hit). Per the card ruling: record the row, defer it to the mold
 * card, fabricate NO substitute recipe. The registration face below is otherwise
 * complete, and the fill/drain loop is fully wired (gas-only fill, free drain, the
 * item NBT projection) — the missing face is only the SOURCE.
 *
 * <p><b>The recorded-only columns</b> (the GTBarrels P4 quartet pool cut — the port
 * has no consumer for them): NBT_ACIDPROOF / NBT_MAGICPROOF / NBT_PLASMAPROOF and
 * NBT_TEMPERATURE (mp-10 on the wax rows, mp-50 on the metal rows, 2700 fixed on
 * WaxMagic/WaxAmnesic, Integer.MAX_VALUE on Infinity) ride the {@link CellRow} table
 * verbatim and are pinned by the offline census. The admission gate carries the
 * GASPROOF T / LIQUIDPROOF F pair only. The upstream
 * {@code OreDictManager.setTarget_(OP.capcellcon, ...)} ore-prefix target has no port
 * consumer either — declared here per the card.
 *
 * <p>One BlockEntityType over the 40 blocks — the ADR-P3-1 shared-BET multi-mount
 * (all 40 rows are the one upstream TE class, the metal-drum Stream form).
 *
 * <p>Creative tab ownership (task cell-family-closeout): the family owns its DEDICATED
 * tab {@link #CELLS_TAB} (the per-prefix-tab treatment the user ruled alongside the test
 * tube). Upstream truth, re-dug 2026-10-03: the 40 rows carry aCreativeTabID 32719 and
 * so do the cup/jug/measuring-pot/thermos/gas-cylinder rows (Loader :2094-2104) — one
 * SHARED lazy CreativeTab (MultiTileEntityRegistry.java:191), so a dedicated page is
 * the DECLARED user deviation. The former GTBarrels "Fluid Containers" (zh 储罐) ride
 * via BuildCreativeModeTabContentsEvent is removed — the storage tab shows zero cells.
 *
 * <p><b>KJS surface declaration (the task card wording)</b>: this card produces the
 * REGISTRATION face (40 Block/BlockItem pairs + one BET over the fluid-interaction
 * seam) with NO KubeJS-specific seam; wiring a RegistryObject-backed addon event for
 * the family ids is the declared defer (the GT6Bumbles.java:71-72 precedent shape).
 * The recipe face is deferred WITH the acquisition row (above) — and when the mold
 * card lands it, the extruder recipes are standard datagen JSON, naturally moddable
 * through the recipe events.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Cells {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The recorded-only NBT_TEMPERATURE forms (the :1770-1809 column verbatim — no port consumer). */
	public enum TemperatureNote { MELTING_POINT_MINUS_10, MELTING_POINT_MINUS_50, FIXED_2700, INFINITE }

	/**
	 * One row of the 40 — the upstream registration line flattened to its port-carried
	 * and recorded-only values. {@code woodHost} = the aUtilWood/aUtilMetal carrier
	 * column (the only host face the port carries: the block sound). The proof/temperature
	 * columns are recorded verbatim (the class doc pool cut).
	 */
	public record CellRow(String path, String displayName, boolean woodHost,
			boolean plasmaProof, boolean acidProof, boolean magicProof, TemperatureNote temperature,
			Supplier<gregapi.oredict.OreDictMaterial> material) {

		/** The registration decision for one row (the mdh-6 family gate; single source for the walk and the tests). */
		public boolean registers() {
			return GT6ModDrivers.isLoaded(GT6Cells.driverDomainOf(this));
		}
	}

	/**
	 * The mdh-6 driver domain per row (task mdh-6-family-gate): the atlas PRIMARY modid when
	 * the row hides with its owning mod absent, else {@code null} = GT core /
	 * COMMON_SECONDARY / unattributed = always registers (ADR-MDH1/MDH2). A String keyed by
	 * path because the registration walk runs at class-init, before {@code MT.init()} fills
	 * the material fields — the GTFluids spec-row column shape, gathered in one switch
	 * instead of a per-row constructor column.
	 */
	public static String driverDomainOf(CellRow aRow) {
		return switch (aRow.path()) {
			case "cell_plant_wax" -> gregapi.data.MT.MD.HaC.mID; // MT.java:2120 — WaxPlant HaC PRIMARY (atlas, mdh-6)
			case "cell_aluminium" -> gregapi.data.MT.MD.TiC.mID; // MT.java:2350 — Al TiC PRIMARY (atlas, mdh-6)
			case "cell_tungsten_alloy" -> gregapi.data.MT.MD.RoC.mID; // MT.java:2429 — TungstenAlloy RoC PRIMARY (atlas, mdh-6)
			case "cell_tungsten_carbide" -> gregapi.data.MT.MD.ReC.mID; // MT.java:2412 — TungstenCarbide ReC PRIMARY (atlas, mdh-6)
			case "cell_workers_alloy" -> gregapi.data.MT.MD.HBM.mID; // MT.java:2404 — DeshAlloy HBM PRIMARY (atlas, mdh-6)
			case "cell_void" -> gregapi.data.MT.MD.TC.mID; // MT.java:2445 — VoidMetal TC PRIMARY (atlas, mdh-6)
			case "cell_manasteel", "cell_terrasteel", "cell_elementium", "cell_gaia_spirit" -> gregapi.data.MT.MD.BOTA.mID; // MT.java:2466-2471 — BOTA PRIMARY (atlas, mdh-6)
			case "cell_awakened_draconium" -> gregapi.data.MT.MD.DE.mID; // MT.java:2513 — DraconiumAwakened DE PRIMARY (atlas, mdh-6)
			case "cell_infinity" -> gregapi.data.MT.MD.AV.mID; // MT.java:2518 — Infinity AV PRIMARY (atlas, mdh-6)
			default -> null; // GT core / COMMON_SECONDARY (thaumium, desh, syrmorite, efrine, draconium) / unattributed — never hides
		};
	}

	/** The 40 rows in registration order (upstream :1770-1809; the en names are the "Capsule-Cell-Container (" + aMat.getLocal() + ")" forms verbatim). */
	public static final List<CellRow> ROWS = List.of(
			new CellRow("cell_wax", "Capsule-Cell-Container (Wax)", true, false, false, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.Wax), // :1770
			new CellRow("cell_bees_wax", "Capsule-Cell-Container (Bees Wax)", true, false, false, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.WaxBee), // :1771
			new CellRow("cell_plant_wax", "Capsule-Cell-Container (Plant Wax)", true, false, false, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.WaxPlant), // :1772
			new CellRow("cell_paraffin_wax", "Capsule-Cell-Container (Paraffin Wax)", true, false, false, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.WaxParaffin), // :1773
			new CellRow("cell_refractory_wax", "Capsule-Cell-Container (Refractory Wax)", true, false, true, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.WaxRefractory), // :1774
			new CellRow("cell_magic_wax", "Capsule-Cell-Container (Magic Wax)", true, false, true, true, TemperatureNote.FIXED_2700, () -> gregapi.data.MT.WaxMagic), // :1775
			new CellRow("cell_amnesic_wax", "Capsule-Cell-Container (Amnesic Wax)", true, false, true, true, TemperatureNote.FIXED_2700, () -> gregapi.data.MT.WaxAmnesic), // :1776
			new CellRow("cell_soulful_wax", "Capsule-Cell-Container (Soulful Wax)", true, false, true, true, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.WaxSoulful), // :1777
			new CellRow("cell_plastic", "Capsule-Cell-Container (Plastic)", true, false, false, false, TemperatureNote.MELTING_POINT_MINUS_10, () -> gregapi.data.MT.Plastic), // :1778
			new CellRow("cell_tin", "Capsule-Cell-Container (Tin)", false, false, false, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Sn), // :1779
			new CellRow("cell_tin_alloy", "Capsule-Cell-Container (Tin Alloy)", false, false, false, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.TinAlloy), // :1780
			new CellRow("cell_invar", "Capsule-Cell-Container (Invar)", false, false, false, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Invar), // :1781
			new CellRow("cell_gold", "Capsule-Cell-Container (Gold)", false, false, true, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Au), // :1782
			new CellRow("cell_aluminium", "Capsule-Cell-Container (Aluminium)", false, false, false, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Al), // :1783
			new CellRow("cell_stainless_steel", "Capsule-Cell-Container (Stainless Steel)", false, false, true, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.StainlessSteel), // :1784
			new CellRow("cell_tungsten_alloy", "Capsule-Cell-Container (Tungsten Alloy)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.TungstenAlloy), // :1785
			new CellRow("cell_titanium", "Capsule-Cell-Container (Titanium)", false, false, false, false, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Ti), // :1786
			new CellRow("cell_netherite", "Capsule-Cell-Container (Netherite)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Netherite), // :1787
			new CellRow("cell_tungstensteel", "Capsule-Cell-Container (Tungstensteel)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.TungstenSteel), // :1788
			new CellRow("cell_tungsten_carbide", "Capsule-Cell-Container (Tungsten Carbide)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.TungstenCarbide), // :1789
			new CellRow("cell_tungsten", "Capsule-Cell-Container (Tungsten)", false, false, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.W), // :1790
			new CellRow("cell_palladium", "Capsule-Cell-Container (Palladium)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Pd), // :1791
			new CellRow("cell_tantalum_hafnium_carbide", "Capsule-Cell-Container (Tantalum Hafnium Carbide)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Ta4HfC5), // :1792
			new CellRow("cell_desh", "Capsule-Cell-Container (Desh)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Desh), // :1793
			new CellRow("cell_workers_alloy", "Capsule-Cell-Container (Workers Alloy)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.DeshAlloy), // :1794
			new CellRow("cell_trinium", "Capsule-Cell-Container (Trinium)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Ke), // :1795
			new CellRow("cell_trinitanium", "Capsule-Cell-Container (Trinitanium)", false, true, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Trinitanium), // :1796
			new CellRow("cell_adamantium", "Capsule-Cell-Container (Adamantium)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Ad), // :1797
			new CellRow("cell_syrmorite", "Capsule-Cell-Container (Syrmorite)", false, false, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Syrmorite), // :1798
			new CellRow("cell_efrine", "Capsule-Cell-Container (Efrine)", false, true, false, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Efrine), // :1799
			new CellRow("cell_thaumium", "Capsule-Cell-Container (Thaumium)", false, false, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Thaumium), // :1800
			new CellRow("cell_void", "Capsule-Cell-Container (Void)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.VoidMetal), // :1801
			new CellRow("cell_manasteel", "Capsule-Cell-Container (Manasteel)", false, false, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Manasteel), // :1802
			new CellRow("cell_terrasteel", "Capsule-Cell-Container (Terrasteel)", false, false, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Terrasteel), // :1803
			new CellRow("cell_elementium", "Capsule-Cell-Container (Elementium)", false, false, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.ElvenElementium), // :1804
			new CellRow("cell_gaia_spirit", "Capsule-Cell-Container (Gaia Spirit)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.GaiaSpirit), // :1805
			new CellRow("cell_duranium_alloy", "Capsule-Cell-Container (Duranium Alloy)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.DuraniumAlloy), // :1806
			new CellRow("cell_draconium", "Capsule-Cell-Container (Draconium)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.Draconium), // :1807
			new CellRow("cell_awakened_draconium", "Capsule-Cell-Container (Awakened Draconium)", false, true, true, true, TemperatureNote.MELTING_POINT_MINUS_50, () -> gregapi.data.MT.DraconiumAwakened), // :1808
			new CellRow("cell_infinity", "Capsule-Cell-Container (Infinity)", false, true, true, true, TemperatureNote.INFINITE, () -> gregapi.data.MT.Infinity)); // :1809

	/**
	 * The blocks, one per registered row, in registration order (the 40-row table loop —
	 * the gas-cylinder four-field form degenerates here). The mdh-6 family gate filters the
	 * walk: an ABSENT domain's rows skip registration entirely (the class-load seed precedes
	 * this walk, FMLModContainer.constructMod order); default all-PRESENT = all 40 (ADR-MDH1).
	 */
	public static final List<RegistryObject<GT6CellBlock>> BLOCKS_IN_ORDER =
			ROWS.stream().filter(CellRow::registers).map(GT6Cells::registerRow).toList();

	/** The blocks by registration path (the GT6Crucibles lookup form — the datagen walk). */
	public static final java.util.Map<String, RegistryObject<GT6CellBlock>> BLOCKS_BY_PATH =
			BLOCKS_IN_ORDER.stream().collect(java.util.stream.Collectors.toMap(tRow -> tRow.getId().getPath(), tRow -> tRow));

	/** The family's creative tabs DR (the GT6Tools/GTWires self-held form). */
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/**
	 * The dedicated "Capsule Cell Containers" tab (task cell-family-closeout) — the user
	 * ruling 2026-10-03, the test-tube-prefix-tab treatment. Title key
	 * {@code itemGroup.gt6.cells}; the icon is the FIRST registered row (cell_wax) — the
	 * upstream lazy-tab face, where the tab's icon item is fixed by the first 32719
	 * registration (MultiTileEntityRegistry.java:191, the Wax row :1770). The zh title
	 * 单元 rides the user ruling (zh_cn_ref.tsv hand row).
	 */
	public static final RegistryObject<CreativeModeTab> CELLS_TAB = CREATIVE_MODE_TABS.register("cells",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.gt6.cells"))
					.icon(() -> new ItemStack(BLOCKS_IN_ORDER.get(0).get().asItem()))
					.displayItems((aParameters, aOutput) -> displayCells(aOutput))
					.build());

	/**
	 * The tab walk: every registered row, in registration order (the GT6Tools TAB_TABLE
	 * form). Package-visible so the offline census drives the SAME walk the tab does.
	 */
	static void displayCells(CreativeModeTab.Output aOutput) {
		for (RegistryObject<GT6CellBlock> tBlock : BLOCKS_IN_ORDER) {
			aOutput.accept(new ItemStack(tBlock.get().asItem()));
		}
	}

	private static RegistryObject<GT6CellBlock> registerRow(CellRow aRow) {
		RegistryObject<GT6CellBlock> tBlock = BLOCKS.register(aRow.path(),
				// task small-tank-colored-tint — the row's NBT_MATERIAL rides the block
				// carrier (the GTBarrelBlock supplier column form) into the tint dispatch
				() -> new GT6CellBlock(() -> GT6Cells.CELL_BE.get(), aRow.material(), GT6CellBlock.rowProperties(aRow.woodHost())));
		ITEMS.register(aRow.path(),
				// the 64-stack family override (MultiTileEntityCell.java:76) — the plain 64 properties, no content gate
				() -> new GT6CellBlockItem(tBlock.get(), new Item.Properties().stacksTo(GT6CellBlockEntity.STACK_SIZE)));
		return tBlock;
	}

	/**
	 * The family BET — the shared-BET multi-mount (ADR-P3-1): valid over all 40 rows,
	 * zero new BE classes (the metal-drum Stream form).
	 */
	public static final RegistryObject<BlockEntityType<GT6CellBlockEntity>> CELL_BE =
			BLOCK_ENTITY_TYPES.register("cell", () -> BlockEntityType.Builder.of(
					GT6CellBlockEntity::new,
					BLOCKS_IN_ORDER.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	private GT6Cells() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6GasCylinders fork form). */
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
		CREATIVE_MODE_TABS.register(tModBus);
	}

	/** Registration smoke evidence (the GT6GasCylinders.onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 cells registered: {} rows (1000 L gas-only, the stack-64 family, own creative tab, acquisition deferred to the CCC mold card)",
					BLOCKS_IN_ORDER.size());
		});
	}
}
