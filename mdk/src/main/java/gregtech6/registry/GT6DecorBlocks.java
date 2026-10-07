package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.decor.GT6AsphaltBlock;
import gregtech6.block.decor.GT6BaleBlock;
import gregtech6.block.decor.GT6BarsBlock;
import gregtech6.block.decor.GT6GlassBlock;
import gregtech6.block.decor.GT6PathBlock;

/**
 * Registration home of the GT6 DECOR-MISC families (task material-mc-g2-decor-misc, the
 * Loader_Blocks.java rows Asphalt :59, Glass/GlowGlass :72-73, Paths :83, Bars :106-111,
 * BalesGrass/Crop :128-129; the spikes ride {@link GT6Spikes}): <b>63 Block + 63 Item
 * registrations</b> — Asphalt 16 dye blocks, Glass 16 + GlowGlass 16, the Path block,
 * 6 Bars (Wood/Brass/Steel/Titanium/TungstenSteel/Adamantium) and 2x4 Bales — in the
 * card-owned self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the
 * GT6ConcreteBlocks precedent; GT6Mod untouched).
 *
 * <p>id scheme: {@code gt6:asphalt_<dye>}, {@code gt6:glass_<dye>},
 * {@code gt6:glow_glass_<dye>} (the dye segment exactly the DYE_IDS snake), the single
 * {@code gt6:path}, {@code gt6:bars_<mat>}, {@code gt6:grass_bale}/{@code dry_grass_bale}
 * /{@code moldy_grass_bale}/{@code rotten_grass_bale} and {@code rye_bale}/{@code oats_bale}
 * /{@code barley_bale}/{@code rice_bale}. Creative tabs (the upstream 1.7.10 tab mapping,
 * the 1.20.1 vocabulary): Asphalt/Glass/GlowGlass = COLORED_BLOCKS (the tabTransport dye
 * ladder, the concrete ruling), Path = NATURAL_BLOCKS, Bars = REDSTONE_BLOCKS
 * (tabRedstone), Bales = BUILDING_BLOCKS (tabDecorations → the hay-block home).
 *
 * <p>KJS: registration face = the declared defer; the recipe face is datapack domain.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class GT6DecorBlocks {

	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(Registries.BLOCK, "gt6");
	public static final DeferredRegister<Item> ITEMS_REG = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The bars rows in the Loader_Blocks.java:106-111 order (the material rides a Supplier —
	 * the GT6GasCylinderRow shape, no MT deref at class-load). */
	public record BarsRow(String family, java.util.function.Supplier<OreDictMaterial> material, boolean flammable) {}

	public static final List<BarsRow> BARS_ROWS = List.of(
			new BarsRow("wood", () -> ANY.Wood, true),
			new BarsRow("brass", () -> MT.Brass, false),
			new BarsRow("steel", () -> MT.Steel, false),
			new BarsRow("titanium", () -> MT.Ti, false),
			new BarsRow("tungstensteel", () -> MT.TungstenSteel, false),
			new BarsRow("adamantium", () -> MT.Ad, false));

	/** The bale snake roots (the 4 grass variants then the 4 crop variants, the dump meta order). */
	public static final String[] BALE_PATHS = {"grass_bale", "dry_grass_bale", "moldy_grass_bale", "rotten_grass_bale",
			"rye_bale", "oats_bale", "barley_bale", "rice_bale"};

	// ---- asphalt (16 dye blocks) -----------------------------------------------------------
	public static final List<RegistryObject<Block>> ASPHALT_BLOCKS = registerDyeFamily("asphalt",
			(aSnake, aDye) -> new GT6AsphaltBlock(aSnake, aDye, GT6AsphaltBlock.decorProperties()));

	// ---- glass + glow glass (2 x 16 dye blocks) ---------------------------------------------
	public static final List<RegistryObject<Block>> GLASS_BLOCKS = registerDyeFamily("glass",
			(aSnake, aDye) -> new GT6GlassBlock(aSnake, aDye, false, GT6GlassBlock.plainProperties()));
	public static final List<RegistryObject<Block>> GLOW_GLASS_BLOCKS = registerDyeFamily("glow_glass",
			(aSnake, aDye) -> new GT6GlassBlock(aSnake, aDye, true, GT6GlassBlock.glowProperties()));

	// ---- the path block ---------------------------------------------------------------------
	public static final RegistryObject<Block> PATH = BLOCKS_REG.register("path",
			() -> new GT6PathBlock(GT6PathBlock.decorProperties()));

	// ---- bars (6 material blocks) -------------------------------------------------------------
	public static final List<RegistryObject<Block>> BARS_BLOCKS = registerBars();

	// ---- bales (2 families x 4 variants) --------------------------------------------------------
	public static final List<RegistryObject<Block>> GRASS_BALES = registerBales(true);
	public static final List<RegistryObject<Block>> CROP_BALES = registerBales(false);

	// ---- the 63 block items ----------------------------------------------------------------------
	public static final List<RegistryObject<Item>> ITEMS = registerItems();

	/** The bale blocks by snake — the transform-face source ({@code grass_bale} form). */
	public static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = blocksByPath();

	/** The registry id of a dye-family member ({@code asphalt_light_gray} form). */
	public static String dyePath(String aFamily, int aDyeIndex) {
		return aFamily + "_" + gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS[aDyeIndex & 15];
	}

	private interface DyeFactory {
		Block create(String aSnake, int aDyeIndex);
	}

	private static List<RegistryObject<Block>> registerDyeFamily(String aFamily, DyeFactory aFactory) {
		List<RegistryObject<Block>> rList = new ArrayList<>(16);
		for (int i = 0; i < 16; i++) {
			final int tDye = i;
			rList.add(BLOCKS_REG.register(dyePath(aFamily, tDye), () -> aFactory.create(dyePath(aFamily, tDye), tDye)));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Block>> registerBars() {
		List<RegistryObject<Block>> rList = new ArrayList<>(BARS_ROWS.size());
		for (BarsRow tRow : BARS_ROWS) {
			rList.add(BLOCKS_REG.register("bars_" + tRow.family(), () -> new GT6BarsBlock("bars_" + tRow.family(),
					tRow.material().get(), tRow.flammable(),
					tRow.flammable() ? GT6BarsBlock.woodProperties() : GT6BarsBlock.metalProperties())));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Block>> registerBales(boolean aGrass) {
		List<RegistryObject<Block>> rList = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			final int tVariant = i;
			final String tPath = BALE_PATHS[(aGrass ? 0 : 4) + i];
			rList.add(BLOCKS_REG.register(tPath, () -> new GT6BaleBlock(tPath, aGrass,
					GT6BaleBlock.BaleVariant.values()[tVariant], GT6BaleBlock.baleProperties())));
		}
		return List.copyOf(rList);
	}

	private static List<RegistryObject<Item>> registerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(63);
		// the id derives from the WALK (no eager RegistryObject deref — the supplier runs at
		// registration time, the GTGrassBlocks registerItems form)
		for (List<RegistryObject<Block>> tFamily : List.of(ASPHALT_BLOCKS, GLASS_BLOCKS, GLOW_GLASS_BLOCKS)) {
			for (int i = 0; i < tFamily.size(); i++) {
				final int tIndex = i;
				rList.add(ITEMS_REG.register(tFamily.get(i).getId().getPath(),
						() -> new GTComposedNameItem(tFamily.get(tIndex).get(), new Item.Properties())));
			}
		}
		for (int i = 0; i < BARS_BLOCKS.size(); i++) {
			final int tIndex = i;
			rList.add(ITEMS_REG.register("bars_" + BARS_ROWS.get(i).family(),
					() -> new GTComposedNameItem(BARS_BLOCKS.get(tIndex).get(), new Item.Properties())));
		}
		for (int i = 0; i < GRASS_BALES.size(); i++) {
			final int tIndex = i;
			rList.add(ITEMS_REG.register(BALE_PATHS[i],
					() -> new GTComposedNameItem(GRASS_BALES.get(tIndex).get(), new Item.Properties())));
		}
		for (int i = 0; i < CROP_BALES.size(); i++) {
			final int tIndex = i;
			rList.add(ITEMS_REG.register(BALE_PATHS[4 + i],
					() -> new GTComposedNameItem(CROP_BALES.get(tIndex).get(), new Item.Properties())));
		}
		rList.add(ITEMS_REG.register("path", () -> new GTComposedNameItem(PATH.get(), new Item.Properties())));
		return List.copyOf(rList);
	}

	private static Map<String, RegistryObject<Block>> blocksByPath() {
		Map<String, RegistryObject<Block>> rMap = new LinkedHashMap<>();
		for (int i = 0; i < GRASS_BALES.size(); i++) rMap.put(BALE_PATHS[i], GRASS_BALES.get(i));
		for (int i = 0; i < CROP_BALES.size(); i++) rMap.put(BALE_PATHS[4 + i], CROP_BALES.get(i));
		return java.util.Collections.unmodifiableMap(rMap);
	}

	private GT6DecorBlocks() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6ConcreteBlocks shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
		//? if forge {
		net.minecraftforge.eventbus.api.IEventBus tModBus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*net.minecraftforge.eventbus.api.IEventBus tModBus =
				net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS_REG.register(tModBus);
		ITEMS_REG.register(tModBus);
	}

	/** The tab joins (the class javadoc mapping): 63 items across four vanilla tabs. */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.COLORED_BLOCKS) {
			for (RegistryObject<Item> tItem : itemsOf(ASPHALT_BLOCKS)) aEvent.accept(tItem.get());
			for (RegistryObject<Item> tItem : itemsOf(GLASS_BLOCKS)) aEvent.accept(tItem.get());
			for (RegistryObject<Item> tItem : itemsOf(GLOW_GLASS_BLOCKS)) aEvent.accept(tItem.get());
		} else if (aEvent.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			aEvent.accept(itemOf(PATH.getId().getPath()).get());
		} else if (aEvent.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			for (RegistryObject<Item> tItem : itemsOf(BARS_BLOCKS)) aEvent.accept(tItem.get());
		} else if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			for (String tPath : BALE_PATHS) aEvent.accept(itemOf(tPath).get());
		}
	}

	private static List<RegistryObject<Item>> itemsOf(List<RegistryObject<Block>> aBlocks) {
		List<RegistryObject<Item>> rList = new ArrayList<>(aBlocks.size());
		for (RegistryObject<Block> tBlock : aBlocks) {
			rList.add(itemOf(tBlock.getId().getPath()));
		}
		return rList;
	}

	/** The public item walk by id path (the recipe/loot band source). */
	public static Item itemOfPath(String aPath) {
		return itemOf(aPath).get();
	}

	private static RegistryObject<Item> itemOf(String aPath) {
		for (RegistryObject<Item> tItem : ITEMS) {
			if (tItem.getId().getPath().equals(aPath)) return tItem;
		}
		throw new IllegalStateException("no decor item for " + aPath);
	}
}
