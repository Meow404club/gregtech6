package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
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
import gregtech6.block.surface.GT6FlowerBlock;
import gregtech6.block.surface.GT6GlowtusBlock;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.block.surface.GT6SurfaceStickBlock;
import gregtech6.block.surface.GT6WildBushBlock;
import gregtech6.block.tree.GT6FallenLogBlock;

/**
 * The surface deco block registrations (task w6-rocks-sticks) — the card-owned
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
	 * The large-vein indicator rock rows (task w6-t3-large-veins, the spec ⑤
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
			new IndicatorSpec("chromite",     () -> MT.OREMATS.Chromite),             // ore.large.asbestos top (a-ore-axis-extension: the stone-layer axis member)
			new IndicatorSpec("asbestos",     () -> MT.Asbestos),             // ore.large.asbestos spread
			new IndicatorSpec("blue_sapphire", () -> MT.BlueSapphire),        // ore.large.sapphire top (b-gem-pool-extension: the gem-pool axis members)
			new IndicatorSpec("orange_sapphire", () -> MT.OrangeSapphire),    // ore.large.sapphire bottom (r7-b)
			new IndicatorSpec("yellow_sapphire", () -> MT.YellowSapphire),    // ore.large.sapphire between (r7-b)
			new IndicatorSpec("ruby",         () -> MT.Ruby),                 // ore.large.sapphire spread / sapphire2 bottom / redstone between (r7-b)
			new IndicatorSpec("green_sapphire", () -> MT.GreenSapphire),      // ore.large.sapphire2 top (r7-b)
			new IndicatorSpec("purple_sapphire", () -> MT.PurpleSapphire),    // ore.large.sapphire2 spread (r7-b)
			new IndicatorSpec("almandine",    () -> MT.Almandine),            // ore.large.garnet top (r7-b)
			new IndicatorSpec("pyrope",       () -> MT.Pyrope),               // ore.large.garnet bottom (r7-b)
			new IndicatorSpec("andradite",    () -> MT.Andradite),            // ore.large.garnet between (r7-b)
			new IndicatorSpec("uvarovite",    () -> MT.Uvarovite),            // ore.large.garnet spread (a-ore-axis-extension)
			new IndicatorSpec("pitchblende",  () -> MT.OREMATS.Pitchblende),  // ore.large.pitchblende top/bottom (a-ore-axis-extension)
			new IndicatorSpec("uraninite",    () -> MT.OREMATS.Uraninite),            // ore.large.pitchblende between/spread (a-ore-axis-extension)
			new IndicatorSpec("graphite",     () -> MT.Graphite),             // ore.large.diamond top/bottom/spread
			new IndicatorSpec("diamond",      () -> MT.Diamond),              // ore.large.diamond between
			new IndicatorSpec("galena",       () -> MT.OREMATS.Galena),       // ore.large.galena top/bottom
			new IndicatorSpec("silver",      () -> MT.Ag),                   // ore.large.galena between
			new IndicatorSpec("lead",        () -> MT.Pb),                   // ore.large.galena spread
			new IndicatorSpec("magnesium_carbonate", () -> MT.MgCO3),        // ore.large.peridot bottom (a-ore-axis-extension)
			new IndicatorSpec("peridot",      () -> MT.Peridot),              // ore.large.peridot between (a-ore-axis-extension)
			new IndicatorSpec("pyrite",       () -> MT.Pyrite),               // ore.large.gold top / copper between
			new IndicatorSpec("chalcopyrite", () -> MT.OREMATS.Chalcopyrite), // ore.large.gold bottom / copper top
			new IndicatorSpec("gold",        () -> MT.Au),                   // ore.large.gold spread
			new IndicatorSpec("cooperite",    () -> MT.OREMATS.Cooperite),    // ore.large.platinum top
			new IndicatorSpec("sperrylite",   () -> MT.OREMATS.Sperrylite),   // ore.large.platinum between
			new IndicatorSpec("iridium",     () -> MT.Ir),                   // ore.large.platinum spread
			new IndicatorSpec("stannite",     () -> MT.OREMATS.Stannite),     // ore.large.cassiterite top (a-ore-axis-extension)
			new IndicatorSpec("kesterite",    () -> MT.OREMATS.Kesterite),    // ore.large.cassiterite bottom (a-ore-axis-extension)
			new IndicatorSpec("cassiterite",  () -> MT.OREMATS.Cassiterite),  // ore.large.cassiterite spread
			new IndicatorSpec("scheelite",    () -> MT.OREMATS.Scheelite),    // ore.large.tungstate top
			new IndicatorSpec("grossular",    () -> MT.Grossular),            // ore.large.manganese top (a-ore-axis-extension)
			new IndicatorSpec("spessartine",  () -> MT.Spessartine),          // ore.large.manganese bottom (b-gem-pool-extension)
			new IndicatorSpec("pyrolusite",   () -> MT.MnO2),                 // ore.large.manganese between
			new IndicatorSpec("aquamarine",   () -> MT.Aquamarine),           // ore.large.beryllium top (b-gem-pool-extension)
			new IndicatorSpec("maxixe",       () -> MT.Maxixe),               // ore.large.beryllium bottom (r7-b)
			new IndicatorSpec("emerald",      () -> MT.Emerald),              // ore.large.beryllium between (r7-b)
			new IndicatorSpec("bixbite",      () -> MT.Bixbite),              // ore.large.beryllium2 top (r7-b)
			new IndicatorSpec("goshenite",    () -> MT.Goshenite),            // ore.large.beryllium2 bottom (r7-b)
			new IndicatorSpec("heliodor",     () -> MT.Heliodor),             // ore.large.beryllium2 between (r7-b)
			new IndicatorSpec("morganite",    () -> MT.Morganite),            // ore.large.beryllium2 spread (r7-b)
			new IndicatorSpec("garnierite",   () -> MT.OREMATS.Garnierite),   // ore.large.nickel top
			new IndicatorSpec("pentlandite",  () -> MT.OREMATS.Pentlandite),  // ore.large.nickel spread
			new IndicatorSpec("redstone",     () -> MT.Redstone),             // ore.large.redstone top/bottom
			new IndicatorSpec("cinnabar",     () -> MT.OREMATS.Cinnabar),     // ore.large.redstone spread
			new IndicatorSpec("copper",      () -> MT.Cu),                   // ore.large.tetrahedrite between / copper spread
			new IndicatorSpec("stibnite",     () -> MT.OREMATS.Stibnite),     // ore.large.tetrahedrite spread
			new IndicatorSpec("hematite",     () -> MT.Fe2O3),                // ore.large.iron between / copper bottom
			new IndicatorSpec("malachite",    () -> MT.OREMATS.Malachite));   // ore.large.iron spread

	/** The 57 indicator rock handles, INDICATOR_SPECS order (31 + 9 since a-ore-axis-extension + 17 since b-gem-pool-extension lit the dormant slots). */
	public static final List<RegistryObject<Block>> INDICATOR_ROCKS = INDICATOR_SPECS.stream()
			.map(tRow -> BLOCKS.<Block>register("surface_rock_" + tRow.snake(),
					() -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), tRow.material().get())))
			.toList();

	/** The indicator rock materials, INDICATOR_ROCKS order (the loot walk + the Feature pick face). */
	public static final List<Supplier<OreDictMaterial>> INDICATOR_MATERIALS =
			INDICATOR_SPECS.stream().map(IndicatorSpec::material).toList();

	// ------------------------------------------------------------------
	// The nether rack band (task worldgen-racks) — tail-append after the
	// indicator rocks, before the flower band. The WorldgenRacks universe
	// (Loader_Worldgen.java:619 "nether.rocks"): the same MTE 32757 surface
	// rock as the overworld band, carrying the nether loot table
	// (WorldgenRacks.java:66-89 the 24-case NBT lottery → per-pair blocks
	// over the zero-material-NBT-port ruling). The FLINT arm reuses the
	// first-batch {@link #SURFACE_ROCK_FLINT} (same carried item both bands,
	// WorldgenRacks.java:72 {@code ST.make(Items.flint, 1, 0)}).
	// ------------------------------------------------------------------

	/** The nether-quartz gem rock (the :66 arm). */
	public static final RegistryObject<Block> SURFACE_ROCK_NETHER_QUARTZ =
			BLOCKS.register("surface_rock_nether_quartz", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.NetherQuartz));

	/** The glowstone gem rock (the :67 arm). */
	public static final RegistryObject<Block> SURFACE_ROCK_GLOWSTONE =
			BLOCKS.register("surface_rock_glowstone", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.Glowstone));

	/** The ancient-debris rock (the :2/:3/:4/:5 brick arms — the 3:1 rockGt/oreRaw lottery rides the loot table, the meteorite precedent). */
	public static final RegistryObject<Block> SURFACE_ROCK_ANCIENT_DEBRIS =
			BLOCKS.register("surface_rock_ancient_debris", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.AncientDebris));

	/** The obsidian rockGt rock (the :5 non-brick/:12 arms). */
	public static final RegistryObject<Block> SURFACE_ROCK_OBSIDIAN =
			BLOCKS.register("surface_rock_obsidian", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.Obsidian));

	/** The basalt rockGt rock (the :13-15 arms). */
	public static final RegistryObject<Block> SURFACE_ROCK_BASALT =
			BLOCKS.register("surface_rock_basalt", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.STONES.Basalt));

	/** The blackstone rockGt rock (the :16-23 non-gravel arms). */
	public static final RegistryObject<Block> SURFACE_ROCK_BLACKSTONE =
			BLOCKS.register("surface_rock_blackstone", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.STONES.Blackstone));

	/** The gloomstone gem rock (the :6/:7 soul-sand arms). */
	public static final RegistryObject<Block> SURFACE_ROCK_GLOOMSTONE =
			BLOCKS.register("surface_rock_gloomstone", () -> new GT6SurfaceRockBlock(surfaceProperties(MapColor.COLOR_GRAY, SoundType.STONE), MT.Gloomstone));

	/** The 7 nether rack rock handles, the {@code gregtech6.worldgen.GT6RacksFeature.Rack} draw-table order (flint stays the first-batch row). */
	public static final List<RegistryObject<Block>> NETHER_ROCKS = List.of(
			SURFACE_ROCK_NETHER_QUARTZ, SURFACE_ROCK_GLOWSTONE, SURFACE_ROCK_ANCIENT_DEBRIS,
			SURFACE_ROCK_OBSIDIAN, SURFACE_ROCK_BASALT, SURFACE_ROCK_BLACKSTONE, SURFACE_ROCK_GLOOMSTONE);

	/** The nether rack rock materials, NETHER_ROCKS order (the bare-descriptionId lang walk). */
	public static final List<Supplier<OreDictMaterial>> NETHER_MATERIALS = List.of(
			() -> MT.NetherQuartz, () -> MT.Glowstone, () -> MT.AncientDebris,
			() -> MT.Obsidian, () -> MT.STONES.Basalt, () -> MT.STONES.Blackstone, () -> MT.Gloomstone);

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

	// ------------------------------------------------------------------
	// The indicator-flower band (task flower-blocks-indicator-family)
	// — tail-append. The upstream two-meta deco blocks (gt.block.flower.a
	// = BlockFlowersA 10 metas / gt.block.flower.b = BlockFlowersB 8 metas,
	// Loader_Blocks.java:120-121) land as 18 INDEPENDENT blocks (the
	// INDICATOR_ROCKS spec-list precedent), one shared GT6FlowerBlock class
	// over the BlockBaseFlower semantics, each with its BlockItem (the
	// obtainable plant-band face). The 18 vanilla potted companions ride
	// the same register (the Blocks.flowerPot pairing — no items, the
	// vanilla potted parity).
	// ------------------------------------------------------------------

	/**
	 * One indicator-flower row: the literal id snake, the sand-soil flag (the
	 * BlockFlowersB.java:136-138 canBlockStay override vs the base :131 dirt face), and
	 * the tooltip faces (the upstream hardcoded addInformation rows, BlockFlowersA
	 * .java:62-81 + BlockFlowersB.java:65-84 — the indicator line + the "* exists in Real
	 * Life" flag).
	 */
	public record FlowerSpec(String snake, boolean sandSoil, String indicator, boolean realLife) {}

	/**
	 * The 18 rows, upstream order: BlockFlowersA metas 0-9 then BlockFlowersB metas 0-7
	 * (Loader_Blocks.java:120-121 registration order). Names live in the lang providers
	 * (the upstream LH.add rows verbatim), textures at {@code gt6:block/<snake>.png} (the
	 * upstream iconsets byte-borrows, assets/README.md rows this card).
	 */
	public static final List<FlowerSpec> FLOWER_SPECS = List.of(
			// BlockFlowersA metas 0-9 (BlockFlowersA.java:41-50)
			new FlowerSpec("flower_altered_andesite_buckwheat", false, "Indicates presence of a Gold Deposit nearby", true),
			new FlowerSpec("flower_crosby_buckwheat", false, "Indicates presence of a Silver Deposit nearby", true),
			new FlowerSpec("flower_alpine_catchfly", false, "Indicates presence of a Copper Deposit nearby", true),
			new FlowerSpec("flower_viola_calaminaria", false, "Indicates presence of a Zinc Deposit nearby", true),
			new FlowerSpec("flower_thlaspi_lereschianum", false, "Indicates presence of a Nickel Deposit nearby", true),
			new FlowerSpec("flower_tufted_evening_primrose", false, "Indicates presence of an Uranium Deposit nearby", true),
			new FlowerSpec("flower_narcissus_sheldonia", false, "Indicates presence of a Platinum Deposit nearby", false),
			new FlowerSpec("flower_orechid", false, "Indicates presence of an Ore Deposit nearby", false),
			new FlowerSpec("flower_hexalily", false, "Indicates presence of a Hexorium Deposit nearby", false),
			new FlowerSpec("flower_vindicator_flower", false, "Vindicates presence of a Rare Earth Deposit nearby", false),
			// BlockFlowersB metas 0-7 (BlockFlowersB.java:45-52) — the sand soil family
			new FlowerSpec("flower_sagebrush", true, "Indicates presence of an Arsenic Deposit nearby", true),
			new FlowerSpec("flower_four_wing_saltbush", true, "Indicates presence of an Antimony Deposit nearby", true),
			new FlowerSpec("flower_desert_trumpet", true, "Indicates presence of a Gold Deposit nearby", true),
			new FlowerSpec("flower_copper_plant", true, "Indicates presence of a Copper Deposit nearby", true),
			new FlowerSpec("flower_princes_plume", true, "Indicates presence of a Redstone Deposit nearby", true),
			new FlowerSpec("flower_thompsons_locoweed", true, "Indicates presence of an Uranium Deposit nearby", true),
			new FlowerSpec("flower_pandanus_candelabrum", true, "Indicates presence of a Diamond Deposit nearby", true),
			new FlowerSpec("flower_tungstus", true, "Indicates presence of a Tungsten Deposit nearby", false));

	/** The 18 indicator-flower blocks, FLOWER_SPECS order. */
	public static final List<RegistryObject<Block>> FLOWERS = FLOWER_SPECS.stream()
			.map(tRow -> BLOCKS.<Block>register(tRow.snake(),
					() -> new GT6FlowerBlock(flowerProperties(), tRow.sandSoil())))
			.toList();

	/**
	 * The 18 vanilla potted companions (the upstream TileEntityFlowerPot intercept
	 * BlockBaseFlower.java:152-160, translated to the platform pairing): the blessed
	 * {@code FlowerPotBlock(Supplier emptyPot, Supplier content, Properties)} ctor plus the
	 * explicit {@code addPlant} row on the vanilla empty pot (both legs — Forge
	 * FlowerPotBlock.java.patch / NeoForge FlowerPotBlock.java.patch, the deprecated
	 * {@code (Block, Properties)} ctor is exactly this dance). The vanilla pot properties
	 * (Blocks.java:7363 {@code instabreak().noOcclusion().pushReaction(DESTROY)}); NO
	 * BlockItem — the vanilla potted parity (potted blocks are pot-fill reachable only).
	 */
	public static final List<RegistryObject<Block>> POTTED_FLOWERS = registerPottedFlowers();

	private static List<RegistryObject<Block>> registerPottedFlowers() {
		List<RegistryObject<Block>> rList = new ArrayList<>(FLOWER_SPECS.size());
		for (int i = 0; i < FLOWER_SPECS.size(); i++) {
			final int tI = i;
			rList.add(BLOCKS.<Block>register("potted_" + FLOWER_SPECS.get(i).snake(), () -> {
				FlowerPotBlock tPotBlock = new FlowerPotBlock(
						() -> (FlowerPotBlock)net.minecraft.world.level.block.Blocks.FLOWER_POT,
						() -> FLOWERS.get(tI).get(),
						BlockBehaviour.Properties.of().instabreak().noOcclusion()
								.pushReaction(PushReaction.DESTROY));
				((FlowerPotBlock)net.minecraft.world.level.block.Blocks.FLOWER_POT).addPlant(
						FLOWERS.get(tI).getId(), () -> tPotBlock);
				return tPotBlock;
			}));
		}
		return List.copyOf(rList);
	}

	/** The vanilla flower row (Blocks.java:986-992 dandelion properties — mapColor PLANT /
	 * noCollission / instabreak / GRASS / offset XZ / DESTROY — the upstream hardness 0
	 * BlockBaseFlower.java:86-88 and the grass step sound map onto the vanilla pair). */
	private static BlockBehaviour.Properties flowerProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.PLANT)
				.noCollission()
				.instabreak()
				.sound(SoundType.GRASS)
				.offsetType(BlockBehaviour.OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY);
	}

	/** All ROCK-FAMILY surface deco blocks, registration order — the census/exemption
	 * walk unit (GT6LangParityTest, Jade name face) + the three rock consumers (the
	 * surface-rock tint listener, the addSurfaceBand FACING walk, the collected-rock
	 * loot). The 18 indicator flowers (task flower-blocks-indicator-family) stay OUT —
	 * they are not tinted rocks; their faces ride the FLOWERS/POTTED_FLOWERS lists and
	 * the DeferredRegister census walk covers them. */
	public static final List<RegistryObject<Block>> ALL;

	static {
		List<RegistryObject<Block>> tAll = new ArrayList<>(List.of(
				SURFACE_ROCK_STONE, SURFACE_ROCK_FLINT, SURFACE_ROCK_METEORITE, SURFACE_STICK));
		tAll.addAll(INDICATOR_ROCKS);
		ALL = List.copyOf(tAll);
	}

	// ------------------------------------------------------------------
	// The surface-plants + fallen-woods band (task w6-t2-surface-blocks)
	// — tail-append. Unlike the pickup-only rocks/sticks these are OBTAINABLE
	// blocks (worldgen loot = self-drop), so each carries a BlockItem and a
	// creative-tab row.
	// ------------------------------------------------------------------

	/** The glowtus (BlockGlowtus = BlockBaseLilyPad light 15; the 16-colour face collapsed, declared in GT6GlowtusBlock). */
	public static final RegistryObject<Block> GLOWTUS =
			BLOCKS.register("glowtus", () -> new GT6GlowtusBlock(decoProperties(MapColor.PLANT, SoundType.GRASS)
					.noCollission().lightLevel(aState -> 15)));
	/** The berry bush (MTE 32759 "Berry Bush") — the growth state lives in the blockstate
	 * (AGE_3+KIND, task bush-growth-blockstate; the five declared deviations in
	 * GT6WildBushBlock), {@code randomTicks()} drives the vanilla growth roll. */
	public static final RegistryObject<Block> BERRY_BUSH =
			BLOCKS.register("berry_bush", () -> new GT6WildBushBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.PLANT).strength(0.5F, 0.3F) // Loader_MultiTileEntities.java:2030
					.sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.DESTROY)
					.randomTicks()));
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

	/** The 18 block items, FLOWER_SPECS order (the natural-tab walk + the tooltip carrier). */
	public static final List<RegistryObject<Item>> FLOWER_ITEMS = registerFlowerItems();

	private static List<RegistryObject<Item>> registerFlowerItems() {
		List<RegistryObject<Item>> rList = new ArrayList<>(FLOWER_SPECS.size());
		for (int i = 0; i < FLOWER_SPECS.size(); i++) {
			final FlowerSpec tRow = FLOWER_SPECS.get(i);
			final RegistryObject<Block> tBlock = FLOWERS.get(i);
			rList.add(ITEMS.register(tRow.snake(), () -> new GT6FlowerBlock.Item(
					tBlock.get(), new Item.Properties(), tRow.indicator(), tRow.realLife())));
		}
		return List.copyOf(rList);
	}

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
			// the 18 indicator flowers (task flower-blocks-indicator-family) — the vanilla
			// flower row face (upstream CreativeTabs.tabDecorations, the port plant-band
			// pooling)
			for (RegistryObject<Item> tItem : FLOWER_ITEMS) {
				aEvent.accept(new net.minecraft.world.item.ItemStack(tItem.get()));
			}
		}
	}
}
