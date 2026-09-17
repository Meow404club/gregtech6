package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregtech6.block.surface.GT6BlackSandBlock;
import gregtech6.block.surface.GT6GlowtusBlock;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.block.surface.GT6SurfaceStickBlock;
import gregtech6.block.surface.GT6WildBushBlock;
import gregtech6.block.tree.GT6FallenLogBlock;

/**
 * The surface deco block registrations (task p30-w6-rocks-sticks) — the card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister shape (the
 * {@link GT6FoamBlocks} precedent; GT6Mod/GTModBusListener untouched). Four blocks:
 * three per-material surface rocks (the first batch of the WorldgenRocks universe) and
 * the single zero-material surface stick (WorldgenSticks).
 *
 * <p>Behaviour numbers are the upstream MTE rows verbatim: hardness 0.25, blast
 * resistance 0 (MultiTileEntityRock.java:250/:249), no collision
 * (getCollisionBoundingBoxFromPool null :238), light-opaque 0 (:248) — the vanilla
 * stand-ins being {@code noCollission()} + {@code noOcclusion()} on a non-full micro
 * box. No creative tab, NO BlockItem: the upstream rock/stick is never obtainable as a
 * block (right-click collects the carried item, MultiTileEntityRock.java:143-148) — the
 * loot table is the only item path, so there is no item face to register.
 *
 * <p>Upstream first-batch transcription (WorldgenRocks.java:63, the NBT lottery into
 * per-pair blocks — the Feature weights in GT6WorldgenDatagen): half the placements carry
 * NO NBT = the default rock ({@code surface_rock_stone}, the overworld default MT.Stone,
 * MultiTileEntityRock.java:174); of the NBT half, 11/12 carry a flint item
 * ({@code surface_rock_flint}) and 1/12 carry MeteoricIron rockGt/oreRaw 3:1
 * ({@code surface_rock_meteorite}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SurfaceBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");

	/** The default-rock block (upstream NBT-less placement, the MT.Stone overworld default). */
	public static final RegistryObject<Block> SURFACE_ROCK_STONE =
			BLOCKS.register("surface_rock_stone", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.STONE, SoundType.STONE), MT.Stone));

	/** The flint-carrying rock (upstream NBT = Items.flint, 11/12 of the NBT half). */
	public static final RegistryObject<Block> SURFACE_ROCK_FLINT =
			BLOCKS.register("surface_rock_flint", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.Flint));

	/** The meteoric-iron rock (upstream NBT = MeteoricIron rockGt/oreRaw, 1/12 of the NBT half). */
	public static final RegistryObject<Block> SURFACE_ROCK_METEORITE =
			BLOCKS.register("surface_rock_meteorite", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.MeteoricIron));

	/** The surface stick (MTE 32756, the single zero-material block). */
	public static final RegistryObject<Block> SURFACE_STICK =
			BLOCKS.register("surface_stick", () -> new GT6SurfaceStickBlock(surfaceProperties(MapColor.WOOD, SoundType.WOOD)));

	/** One indicator row: the literal id snake (the class loads before MT.init — the GT6OreBlocks.java:114-117 supplier lesson) + the material. */
	private record IndicatorSpec(String snake, Supplier<OreDictMaterial> material) {}

	/**
	 * The large-vein indicator rock rows (task p30-w6-t3-large-veins, the spec ⑤
	 * compensation for the MTE 32757 arm): the DISTINCT VALID slots of the 40-row vein
	 * table (Loader_Worldgen.java:886-925 — a slot is valid when its material is in the
	 * GT6OreBlocks.materialAxis() universe, the same gate the vein Feature draws through).
	 * The upstream indicator carried one of the four vein materials (WorldgenOresLarge
	 * .java:104); rows whose materials have not landed in the registered universe yet
	 * light up here as the axis extends — GT6LargeVeinTest pins this list 1:1 against the
	 * datagen vein table, and each literal snake against GTMaterialItems.snakeCase.
	 */
	private static final List<IndicatorSpec> INDICATOR_SPECS = List.of(
			new IndicatorSpec("coal",         () -> MT.Coal),                 // ore.large.lignite/.coal (the coal-arm slots)
			new IndicatorSpec("lapis",        () -> MT.Lapis),                // ore.large.lapis between
			new IndicatorSpec("azurite",      () -> MT.Azurite),              // ore.large.lapis spread
			new IndicatorSpec("salt",         () -> MT.NaCl),                 // ore.large.iodinesalt bottom
			new IndicatorSpec("borax",        () -> MT.OREMATS.Borax),        // ore.large.iodinesalt between
			new IndicatorSpec("zeolite",      () -> MT.OREMATS.Zeolite),      // ore.large.iodinesalt spread
			new IndicatorSpec("sylvite",      () -> MT.KCl),                  // ore.large.rocksalt top
			new IndicatorSpec("coltan",       () -> MT.OREMATS.Coltan),       // ore.large.rocksalt bottom / manganese spread
			new IndicatorSpec("asbestos",     () -> MT.Asbestos),             // ore.large.asbestos spread
			new IndicatorSpec("graphite",     () -> MT.Graphite),             // ore.large.diamond top/bottom/spread
			new IndicatorSpec("diamond",      () -> MT.Diamond),              // ore.large.diamond between
			new IndicatorSpec("galena",       () -> MT.OREMATS.Galena),       // ore.large.galena top/bottom
			new IndicatorSpec("silver",      () -> MT.Ag),                   // ore.large.galena between
			new IndicatorSpec("lead",        () -> MT.Pb),                   // ore.large.galena spread
			new IndicatorSpec("pyrite",       () -> MT.Pyrite),               // ore.large.gold top / copper between
			new IndicatorSpec("chalcopyrite", () -> MT.OREMATS.Chalcopyrite), // ore.large.gold bottom / copper top
			new IndicatorSpec("gold",        () -> MT.Au),                   // ore.large.gold spread
			new IndicatorSpec("cooperite",    () -> MT.OREMATS.Cooperite),    // ore.large.platinum top
			new IndicatorSpec("sperrylite",   () -> MT.OREMATS.Sperrylite),   // ore.large.platinum between
			new IndicatorSpec("iridium",     () -> MT.Ir),                   // ore.large.platinum spread
			new IndicatorSpec("cassiterite",  () -> MT.OREMATS.Cassiterite),  // ore.large.cassiterite spread
			new IndicatorSpec("scheelite",    () -> MT.OREMATS.Scheelite),    // ore.large.tungstate top
			new IndicatorSpec("pyrolusite",   () -> MT.MnO2),                 // ore.large.manganese between
			new IndicatorSpec("garnierite",   () -> MT.OREMATS.Garnierite),   // ore.large.nickel top
			new IndicatorSpec("pentlandite",  () -> MT.OREMATS.Pentlandite),  // ore.large.nickel spread
			new IndicatorSpec("redstone",     () -> MT.Redstone),             // ore.large.redstone top/bottom
			new IndicatorSpec("cinnabar",     () -> MT.OREMATS.Cinnabar),     // ore.large.redstone spread
			new IndicatorSpec("copper",      () -> MT.Cu),                   // ore.large.tetrahedrite between / copper spread
			new IndicatorSpec("stibnite",     () -> MT.OREMATS.Stibnite),     // ore.large.tetrahedrite spread
			new IndicatorSpec("hematite",     () -> MT.Fe2O3),                // ore.large.iron between / copper bottom
			new IndicatorSpec("malachite",    () -> MT.OREMATS.Malachite));   // ore.large.iron spread

	/** The 31 indicator rock handles, INDICATOR_SPECS order. */
	public static final List<RegistryObject<Block>> INDICATOR_ROCKS = INDICATOR_SPECS.stream()
			.map(tRow -> BLOCKS.<Block>register("surface_rock_" + tRow.snake(),
					() -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), tRow.material().get())))
			.toList();

	/** The indicator rock materials, INDICATOR_ROCKS order (the loot walk + the Feature pick face). */
	public static final List<Supplier<OreDictMaterial>> INDICATOR_MATERIALS =
			INDICATOR_SPECS.stream().map(IndicatorSpec::material).toList();

	/**
	 * The vein-indicator rock of a picked material (GT6LargeVeinFeature's IO face): the
	 * per-material rock, or the default rock for a null/unregistered pick — the upstream
	 * NBT-less arm (WorldgenOresLarge.java:104 {@code : UT.NBT.make()}).
	 */
	public static Block indicatorRock(OreDictMaterial aMaterial) {
		if (aMaterial != null && aMaterial.mID > 0) {
			for (int i = 0; i < INDICATOR_MATERIALS.size(); i++) {
				if (INDICATOR_MATERIALS.get(i).get() == aMaterial) return INDICATOR_ROCKS.get(i).get();
			}
		}
		return SURFACE_ROCK_STONE.get();
	}

	/** All surface deco blocks, registration order — the census/exemption walk unit (GT6LangParityTest, Jade name face). */
	public static final List<RegistryObject<Block>> ALL;

	static {
		List<RegistryObject<Block>> tAll = new ArrayList<>(List.of(
				SURFACE_ROCK_STONE, SURFACE_ROCK_FLINT, SURFACE_ROCK_METEORITE, SURFACE_STICK));
		tAll.addAll(INDICATOR_ROCKS);
		ALL = List.copyOf(tAll);
	}

	// ------------------------------------------------------------------
	// The surface-plants + fallen-woods band (task p30-w6-t2-surface-blocks)
	// — tail-append. Unlike the pickup-only rocks/sticks these are OBTAINABLE
	// blocks (worldgen loot = self-drop), so each carries a BlockItem and a
	// creative-tab row.
	// ------------------------------------------------------------------

	/** The glowtus (BlockGlowtus = BlockBaseLilyPad light 15; the 16-colour face collapsed, declared in GT6GlowtusBlock). */
	public static final RegistryObject<Block> GLOWTUS =
			BLOCKS.register("glowtus", () -> new GT6GlowtusBlock(decoProperties(MapColor.PLANT, SoundType.GRASS)
					.noCollission().lightLevel(aState -> 15)));
	/** The berry bush, block-only (MTE 32759 "Berry Bush"; the berry NBT face cut, declared in GT6WildBushBlock). */
	public static final RegistryObject<Block> BERRY_BUSH =
			BLOCKS.register("berry_bush", () -> new GT6WildBushBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.PLANT).strength(0.5F, 0.3F) // Loader_MultiTileEntities.java:2030
					.sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.DESTROY)));
	/** The magnetite black sand (WorldgenBlackSand, the river-bed soil; the vanilla FallingBlock gravity idiom —
	 * SandBlock died with the 1.21.2 merge so the shared face is the {@link GT6BlackSandBlock} subclass). */
	public static final RegistryObject<Block> BLACK_SAND =
			BLOCKS.register("black_sand", () -> new GT6BlackSandBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.SAND)));
	/** The swamp turf (WorldgenTurf, the Diggables meta-2 soil — BlocksGT.Diggables has no modern port, card spec ⑤). */
	public static final RegistryObject<Block> TURF =
			BLOCKS.register("turf", () -> new Block(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GREEN).strength(0.6F).sound(SoundType.GRASS)));

	/** The four fallen-log woods (Dead/Rotten/Mossy/Frozen — the Log1 meta variants, see GT6FallenLogBlock), path order. */
	public static final List<RegistryObject<Block>> FALLEN_LOGS = List.of(
			BLOCKS.register("dead_log", GT6FallenLogBlock::new),
			BLOCKS.register("rotten_log", GT6FallenLogBlock::new),
			BLOCKS.register("mossy_log", GT6FallenLogBlock::new),
			BLOCKS.register("frozen_log", GT6FallenLogBlock::new));

	/** The obtainable-band blocks, registration order (the census/lang walk unit). */
	public static final List<RegistryObject<Block>> PLANT_BAND = List.of(
			GLOWTUS, BERRY_BUSH, BLACK_SAND, TURF);

	/** The obtainable band's item register (the GT6TreeBlocks shape). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");
	/** The 8 block items, registration order (the 4 plants, then the 4 fallen logs — the tab walks use the sub-lists). */
	public static final List<RegistryObject<Item>> PLANT_ITEMS = registerPlantItems();

	private static List<RegistryObject<Item>> registerPlantItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(PLANT_BAND.size() + FALLEN_LOGS.size());
		for (RegistryObject<Block> tBlock : PLANT_BAND) {
			rList.add(ITEMS.register(tBlock.getId().getPath(), () -> new BlockItem(tBlock.get(), new Item.Properties())));
		}
		for (RegistryObject<Block> tBlock : FALLEN_LOGS) {
			rList.add(ITEMS.register(tBlock.getId().getPath(), () -> new BlockItem(tBlock.get(), new Item.Properties())));
		}
		return List.copyOf(rList);
	}

	/** The four plant block items, registration order (the natural-tab walk). */
	public static final List<RegistryObject<Item>> PLANT_TAB_ITEMS = PLANT_ITEMS.subList(0, PLANT_BAND.size());
	/** The four fallen-log block items, registration order (the building-tab walk, the t1 log row). */
	public static final List<RegistryObject<Item>> LOG_TAB_ITEMS = PLANT_ITEMS.subList(PLANT_BAND.size(), PLANT_ITEMS.size());

	/** The shared behaviour properties (MultiTileEntityRock.java:238/:248/:249/:250 verbatim). */
	private static BlockBehaviour.Properties surfaceProperties(MapColor aColor, SoundType aSound) {
		return BlockBehaviour.Properties.of()
				.mapColor(aColor)
				.strength(0.25F, 0.0F) // hardness 0.25 (getBlockHardness :250), blast 0 (getExplosionResistance2 :249)
				.sound(aSound)
				.noCollission() // the upstream collision box is null (:238)
				.noOcclusion() // light opacity none (:248); the micro box never occludes
				.pushReaction(PushReaction.DESTROY); // the deco walks like the vanilla flower row
	}

	/** The obtainable-deco base (the vanilla lily-pad row: strength 0 + grass sound + DESTROY push). */
	private static BlockBehaviour.Properties decoProperties(MapColor aColor, SoundType aSound) {
		return BlockBehaviour.Properties.of()
				.mapColor(aColor)
				.strength(0.0F)
				.sound(aSound)
				.noOcclusion()
				.pushReaction(PushReaction.DESTROY);
	}

	private GT6SurfaceBlocks() {
	}

	/** The construct-event attach (the GT6FoamBlocks.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The obtainable-band tab joins (the GT6TreeBlocks.onBuildTabContents shape): the
	 * fallen logs join BUILDING_BLOCKS (the t1 log row), the plant band joins
	 * NATURAL_BLOCKS (the vanilla flower/sand row face).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			for (RegistryObject<Item> tItem : LOG_TAB_ITEMS) {
				aEvent.accept(new net.minecraft.world.item.ItemStack(tItem.get()));
			}
		} else if (aEvent.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			for (RegistryObject<Item> tItem : PLANT_TAB_ITEMS) {
				aEvent.accept(new net.minecraft.world.item.ItemStack(tItem.get()));
			}
		}
	}
}
