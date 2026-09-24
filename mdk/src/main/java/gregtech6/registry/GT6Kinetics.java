package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.energy.GT6WaterWheelBlock;
import gregtech6.block.GTEntityBlock;
import gregtech6.block.energy.GTCrankBlock;
import gregtech6.block.energy.GTDieselEngineBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.block.energy.GTGearBoxBlock;
import gregtech6.block.energy.GTTransformerRotationBlock;

/**
 * The kinetics registration family home (task p12-engine-crank spec ③): the shared
 * DeferredRegister pair for the engine + rotation-transmission family — the Hand Crank
 * is its first member, the axle/gearbox/transformer/engine cards append here. Card-owned
 * (ADR-P3-4): self-contained {@code @EventBusSubscriber(MOD)} DeferredRegisters attached
 * from the construct event — GT6Mod.java / GTModBusListener.java stay untouched (the
 * frozen P2 form is never extended). Same shape as GTEnergySources / GTWires
 * (the GTEnergySources.java Supplier-block precedent); the shared BET row lives in
 * {@link GTBlockEntities#CRANK_BE} per the card (the BET type row in the shared registry
 * file, the ENERGY_SOURCE_BE/WIRE_ELECTRIC_BE cross-register resolution shape), and its
 * supplier resolves the block here — safe because the vanilla registry order fires the
 * Block registration event before the BlockEntityType one, across DeferredRegisters
 * (GTBlockEntities doc).
 *
 * <p>Block constants from the upstream registration row (Loader_MultiTileEntities.java
 * :2106): hardness 1.0F / resistance 6.0F, the metal tool material →
 * {@link SoundType#METAL}. Creative tab: the crank (upstream "Misc Tool Blocks", tab
 * 32720, :2106), its gearbox/rotation-transformer siblings and the water wheel join
 * MACHINES_TAB (task p38-tabfix-b-energy, {@link #onBuildTabContents}; the GTBarrels:257
 * pooling precedent — supersedes the old /give-reachable note). The axle/steam-engine/
 * diesel ladders stay out of this card's join — the p38 tail card owns them.
 *
 * <p><b>The axle family</b> (task p12-axle-family spec ③): 11 materials x 4 diameters =
 * 44 blocks/items over the ONE shared {@link GTBlockEntities#AXLE_BE} (the ADR-P3-1
 * multi-mount, the GTWires 620-variant form). {@link #AXLE_SPECS} is the Loader kinetic
 * section verbatim (Loader_MultiTileEntities.java:1662-1744 and the material rows through
 * :1797): every row pins the speed rating VMAX[tier] (CS.java:150) and the four
 * per-diameter bandwidths (NBT_PIPEBANDWIDTH); the diameters are PX_P (CS.java:492) =
 * 6/9/12/16 px. The table is NAME+NUMBER only — no OreDictMaterial references: the static
 * init of this class runs at MOD construct time, where MT is not yet initialized (the
 * GTFluids lesson, p6 a9027ac).
 * <p>Task p12-engine-steam appends the Steam Engine family: the full
 * Loader_MultiTileEntities.java:583-612 row projection (both the Steam Engine and Strong
 * Steam Engine ladders), the efficiency/capacity/output values riding the block carrier
 * ({@link SteamEngineBlock#row()}, the GTBarrelBlock capacityL shape), one shared BET
 * ({@link GTBlockEntities#STEAM_ENGINE_BE}, ADR-P3-1 multi-mount). COUNT ERRATUM
 * (census over card text, the p12-jei-integration brick-count precedent): the task card
 * says "26 variants, 13+13" but 1300..1313 and 1350..1363 are FOURTEEN ids each — the
 * upstream loop :584-597 + :599-612 adds 14+14 = 28 rows, and the wire-W1 "no upstream
 * subset" ruling forbids dropping the two the card's arithmetic lost. The table below is
 * the verbatim 28.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Kinetics {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The Hand Crank (upstream "Hand Crank" meta 32111, MultiTileEntityCrank port). */
	public static final RegistryObject<GTCrankBlock> CRANK = BLOCKS.register("crank",
			() -> new GTCrankBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 6.0F).sound(SoundType.METAL)));

	public static final RegistryObject<Item> CRANK_ITEM = ITEMS.register("crank",
			() -> new BlockItem(CRANK.get(), new Item.Properties()));

	// -------------------------------------------------------------------------
	// the gearbox + rotation transformer (task p12-gearbox-transformer) — the wood
	// kinetic rows, one variant each (the material family fan-out rides the pool)
	// -------------------------------------------------------------------------

	/** The Custom Wooden Gearbox row rating (Loader :1669: {@code NBT_INPUT, VMAX[0]} = 16; the literal carries the {@link #VMAX} forward reference). */
	public static final long GEARBOX_MAX_THROUGHPUT = 16;

	/**
	 * The Wooden Transformer Gearbox row (Loader :1668): {@code NBT_INPUT, V[0]},
	 * {@code NBT_OUTPUT, 2}, {@code NBT_MULTIPLIER, 4} — speed ÷ 4, power × 4 (the wood
	 * row 8→2, the bronze row :1677 32→8 by the same rule).
	 */
	public static final long TRANSFORMER_INPUT_SPEED = 8;
	public static final long TRANSFORMER_OUTPUT_SPEED = 2;
	public static final long TRANSFORMER_MULTIPLIER = 4;

	/** The GearBox (upstream "Custom Wooden Gearbox" meta 24809, MultiTileEntityGearBox port). */
	public static final RegistryObject<GTGearBoxBlock> GEARBOX = BLOCKS.register("gearbox",
			() -> new GTGearBoxBlock(BlockBehaviour.Properties.of()
					.strength(6.0F, 6.0F).sound(SoundType.WOOD)));

	public static final RegistryObject<Item> GEARBOX_ITEM = ITEMS.register("gearbox",
			() -> new BlockItem(GEARBOX.get(), new Item.Properties()));

	/** The Rotation Transformer (upstream "Wooden Transformer Gearbox" meta 24808, the MultiTileEntityTransformerRotation port). */
	public static final RegistryObject<GTTransformerRotationBlock> TRANSFORMER_ROTATION = BLOCKS.register("transformer_rotation",
			() -> new GTTransformerRotationBlock(BlockBehaviour.Properties.of()
					.strength(6.0F, 6.0F).sound(SoundType.WOOD)));

	public static final RegistryObject<Item> TRANSFORMER_ROTATION_ITEM = ITEMS.register("transformer_rotation",
			() -> new BlockItem(TRANSFORMER_ROTATION.get(), new Item.Properties()));

	// -------------------------------------------------------------------------
	// the axle family (task p12-axle-family) — 11 materials x 4 diameters = 44
	// -------------------------------------------------------------------------

	/**
	 * One Loader kinetic row (Loader_MultiTileEntities.java:1662-1797), name+number only (no MT at construct time).
	 * {@code matDisplay} is the row's material display word verbatim (the upstream row wording — Wood rows say
	 * "Wooden" hard-coded, :1663-1666; the metal rows carry mNameLocal), and the composed-name unit key derives
	 * from the material slug (see {@link #axleMatUnitKey}).
	 */
	public record AxleSpec(String material, String matDisplay, int tier, int[] bandwidth) {}

	/** The composed axle display template "{@code %s %s Axle}" (task p20-i18n-compose-rows): size unit + material unit slots. */
	public static final String AXLE_DISPLAY_KEY = "gt6.row.axle.display";

	/** The i-th diameter size unit key (the small unit nouns the template consumes — slot-less). */
	public static String axleSizeUnitKey(int aSizeIndex) {
		return "gt6.row.size." + AXLE_SIZE_NAMES[aSizeIndex];
	}

	/** The i-th diameter size display word (the en unit VALUE: the capitalized upstream wording). */
	public static String axleSizeDisplay(int aSizeIndex) {
		return Character.toUpperCase(AXLE_SIZE_NAMES[aSizeIndex].charAt(0)) + AXLE_SIZE_NAMES[aSizeIndex].substring(1);
	}

	/** The row's material small-unit key (the gt6.row.mat namespace, slug = the row material column). */
	public static String axleMatUnitKey(AxleSpec aSpec) {
		return "gt6.row.mat." + aSpec.material();
	}

	/** The row speed rating: NBT_PIPESIZE = VMAX[tier] (Loader :1663 e.g. {@code NBT_PIPESIZE, VMAX[0]}). */
	public static long axleSpeed(AxleSpec aSpec) {
		return VMAX[aSpec.tier()];
	}

	/** PX_P (CS.java:492) — the four diameters in 1/16 block units, shared by every row. */
	public static final int[] AXLE_DIAMETERS = {6, 9, 12, 16};

	/** The per-diameter registry/lang tail, small → huge (the upstream row wording order). */
	public static final String[] AXLE_SIZE_NAMES = {"small", "medium", "large", "huge"};

	/** CS.java:150 VMAX verbatim — the speed rating per tier (the axle rows use tiers 0-7). */
	public static final long[] VMAX = {16, 64, 256, 1024, 4096, 16384, 65536, 262144,
			1048576L, 4194304L, 16777216L, 67108864L, 268435456L, 1073741824L,
			4294967296L, 17179869184L};

	/**
	 * The kinetic section verbatim: the material slug (this-port material registry form,
	 * {@code gt6.material.<slug>} — Iritanium is the titanium_iridium alloy row), the
	 * display material name (the upstream row wording — Wood rows say "Wooden" hard-coded,
	 * :1663-1666; the metal rows carry mNameLocal), the VMAX tier (the row's NBT_PIPESIZE
	 * = VMAX[tier], resolved through {@link #axleSpeed}) and the four bandwidths
	 * NBT_PIPEBANDWIDTH (small/medium/large/huge). Rows: WoodTreated :1662-1666, Bronze
	 * :1671-1675, Brass :1679-1683, ArsenicCopper :1687-1691, ArsenicBronze :1695-1699
	 * (3/6/12/24), Steel :1703-1707, Ti :1712-1716, TungstenSteel :1721-1725, Ir
	 * :1730-1734, Iritanium :1739-1743, Trinitanium :1748-1752 (each following material
	 * doubles the previous bandwidth ladder).
	 */
	public static final List<AxleSpec> AXLE_SPECS = List.of(
			new AxleSpec("wood_treated", "Wooden", 0, new int[] {1, 2, 4, 8}),
			new AxleSpec("bronze", "Bronze", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("brass", "Brass", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("arsenic_copper", "Arsenic Copper", 1, new int[] {2, 4, 8, 16}),
			new AxleSpec("arsenic_bronze", "Arsenic Bronze", 1, new int[] {3, 6, 12, 24}),
			new AxleSpec("steel", "Steel", 2, new int[] {4, 8, 16, 32}),
			new AxleSpec("titanium", "Titanium", 3, new int[] {8, 16, 32, 64}),
			new AxleSpec("tungstensteel", "Tungstensteel", 4, new int[] {16, 32, 64, 128}),
			new AxleSpec("iridium", "Iridium", 5, new int[] {32, 64, 128, 256}),
			new AxleSpec("titanium_iridium", "Iritanium", 6, new int[] {64, 128, 256, 512}),
			new AxleSpec("trinitanium", "Trinitanium", 7, new int[] {128, 256, 512, 1024}));

	/** The 44 registered axle blocks (the datagen/loot/BET walkers iterate this). */
	public static final Map<String, RegistryObject<GTAxleBlock>> AXLE_BLOCKS = new LinkedHashMap<>();

	/** The 44 registered axle items, same keys as {@link #AXLE_BLOCKS}. */
	public static final Map<String, RegistryObject<Item>> AXLE_ITEMS = new LinkedHashMap<>();

	/** The registry-name form: {@code axle_<material>_<size>}. */
	public static String axleName(String aMaterial, int aSizeIndex) {
		return "axle_" + aMaterial + "_" + AXLE_SIZE_NAMES[aSizeIndex];
	}

	/** The composed name of an axle (the pure compose seam): the size unit + the material unit over the "%s %s Axle" template. */
	public static net.minecraft.network.chat.MutableComponent axleDisplayOf(AxleSpec aSpec, int aSizeIndex) {
		return net.minecraft.network.chat.Component.translatable(AXLE_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(axleSizeUnitKey(aSizeIndex)),
				net.minecraft.network.chat.Component.translatable(axleMatUnitKey(aSpec)));
	}

	/** The block list in declaration order (the BET multi-mount array + the loot/datagen walkers). */
	public static Block[] axleBlockArray() {
		Block[] rBlocks = new Block[AXLE_BLOCKS.size()];
		int i = 0;
		for (RegistryObject<GTAxleBlock> tBlock : AXLE_BLOCKS.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The axle registration loop — 11 materials x 4 diameters, the Loader row body
	 * (NBT_HARDNESS 2.0F / NBT_RESISTANCE 6.0F; the wooden NBT_FLAMMABILITY half is a
	 * later card's surface) with the METAL sound of the crank row precedent; noOcclusion
	 * for the sub-cube rod shape (the pipe-block convention). Registration happens in
	 * {@link #onModConstruct} BEFORE the DeferredRegisters attach — the plain loop calls
	 * BLOCKS.register(name, supplier) directly (DeferredRegister is just a holder until
	 * register(bus) fires).
	 */
	private static void registerAxles() {
		for (AxleSpec tSpec : AXLE_SPECS) {
			for (int tSize = 0; tSize < AXLE_DIAMETERS.length; tSize++) {
				String tName = axleName(tSpec.material(), tSize);
				final AxleSpec fSpec = tSpec;
				final int fSize = tSize;
				RegistryObject<GTAxleBlock> tBlock = BLOCKS.register(tName, () -> new GTAxleBlock(
						BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(),
						fSpec, fSize));
				AXLE_BLOCKS.put(tName, tBlock);
				AXLE_ITEMS.put(tName, ITEMS.register(tName, () -> new gregtech6.block.GTComposedNameItem(tBlock.get(), new Item.Properties())));
			}
		}
	}

	// steam engines (task p12-engine-steam — Loader_MultiTileEntities.java:583-612)
	// -------------------------------------------------------------------------

	/** The composed Steam Engine display template "{@code Steam Engine (%s)}" (task p20-i18n-compose-rows) — one material slot. */
	public static final String STEAM_DISPLAY_KEY = "gt6.row.steam_engine.display";

	/** The Strong-ladder template "{@code Strong Steam Engine (%s)}" — the Strong wording rides the template, not a slot. */
	public static final String STEAM_DISPLAY_STRONG_KEY = "gt6.row.steam_engine.display.strong";

	/** The row's material small-unit key (the gt6.row.mat namespace, slug = the row material column). */
	public static String steamMatUnitKey(SteamEngineRow aRow) {
		return "gt6.row.mat." + aRow.matSlug();
	}

	/** The composed name of a steam-engine row — the template fill (the pure compose seam, the GTWireBlock.displayNameOf posture). */
	public static net.minecraft.network.chat.MutableComponent steamDisplayOf(SteamEngineRow aRow) {
		return net.minecraft.network.chat.Component.translatable(
				aRow.strong() ? STEAM_DISPLAY_STRONG_KEY : STEAM_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(steamMatUnitKey(aRow)));
	}

	/**
	 * One Steam Engine registration row — the block-carrier projection of one upstream
	 * {@code aRegistry.add(..., MultiTileEntityEngineSteam.class, ..., UT.NBT.make(...))}
	 * line (:584-597 the Steam ladder, :599-612 the Strong ladder; values transcribed
	 * verbatim, {@code NBT_OUTPUT = N/STEAM_PER_EU} divided out — 2 L steam per EU, the
	 * loader's own CS.java:240 constant, so the KU packet is N/2).
	 *
	 * @param path           the gt6 registry path (the blockstate/model/lang key tail)
	 * @param matSlug        the row material slug (the gt6.row.mat small-unit key tail)
	 * @param matDisplay     the row material display word, verbatim from the old display column
	 * @param strong         the Strong-ladder flag (the display form selector, no behavioural
	 *                       difference upstream — the SAME class and the same NBT set)
	 * @param efficiency     the upstream NBT_EFFICIENCY (ten-thousandths)
	 * @param energyCapacity the upstream NBT_CAPACITY (the KU store, NOT tank litres — the
	 *                       tank re-derives as STEAM_PER_WATER * output * 2, EngineSteam :80)
	 * @param outputKU       the upstream NBT_OUTPUT (the nominal KU/t packet)
	 * @param hardness       the upstream NBT_HARDNESS
	 * @param resistance     the upstream NBT_RESISTANCE
	 * @param wooden         the row's {@code aWooden} flag (the IronWood rows) — the sound tier
	 */
	public record SteamEngineRow(String path, String matSlug, String matDisplay, boolean strong,
			short efficiency, long energyCapacity, long outputKU, float hardness, float resistance, boolean wooden) {
		/** The block properties of the row (hardness/resistance/sound verbatim). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(hardness, resistance)
					.sound(wooden ? SoundType.WOOD : SoundType.METAL);
		}
	}

	/**
	 * The 28 steam-engine rows, upstream line order (:584-597 then :599-612). Every
	 * efficiency 3000..6450 and output 8..256 KU/t; the IronWood pair is the aWooden tier.
	 */
	public static final List<SteamEngineRow> STEAM_ENGINES = List.of(
			// the Steam Engine ladder (meta 1300-1313)
			new SteamEngineRow("steam_engine_lead", "lead", "Lead", false, (short)3000, 16000, 8, 4.0F, 4.0F, false),
			new SteamEngineRow("steam_engine_tin_alloy", "tin_alloy", "Tin Alloy", false, (short)4000, 20000, 10, 4.0F, 4.0F, false),
			new SteamEngineRow("steam_engine_bronze", "bronze", "Bronze", false, (short)5000, 24000, 12, 7.0F, 7.0F, false),
			new SteamEngineRow("steam_engine_arsenic_copper", "arsenic_copper", "Arsenic Copper", false, (short)5000, 24000, 12, 7.0F, 7.0F, false),
			new SteamEngineRow("steam_engine_arsenic_bronze", "arsenic_bronze", "Arsenic Bronze", false, (short)5000, 28000, 14, 7.0F, 7.0F, false),
			new SteamEngineRow("steam_engine_brass", "brass", "Brass", false, (short)5000, 24000, 12, 7.0F, 7.0F, false),
			new SteamEngineRow("steam_engine_invar", "invar", "Invar", false, (short)6400, 16000, 8, 4.0F, 4.0F, false),
			new SteamEngineRow("steam_engine_iron_wood", "iron_wood", "Iron Wood", false, (short)6450, 16000, 8, 4.0F, 4.0F, true),
			new SteamEngineRow("steam_engine_steel", "steel", "Steel", false, (short)5000, 32000, 16, 6.0F, 6.0F, false),
			new SteamEngineRow("steam_engine_fiery_steel", "fiery_steel", "Fiery Steel", false, (short)6200, 64000, 32, 7.0F, 7.0F, false),
			new SteamEngineRow("steam_engine_chromium", "chromium", "Chromium", false, (short)6300, 96000, 48, 4.0F, 4.0F, false),
			new SteamEngineRow("steam_engine_titanium", "titanium", "Titanium", false, (short)5800, 112000, 56, 9.0F, 9.0F, false),
			new SteamEngineRow("steam_engine_tungsten", "tungsten", "Tungsten", false, (short)5800, 128000, 64, 10.0F, 10.0F, false),
			new SteamEngineRow("steam_engine_tungstensteel", "tungstensteel", "Tungstensteel", false, (short)6000, 128000, 64, 12.5F, 12.5F, false),
			// the Strong Steam Engine ladder (meta 1350-1363)
			new SteamEngineRow("strong_steam_engine_lead", "lead", "Lead", true, (short)3000, 64000, 32, 4.0F, 4.0F, false),
			new SteamEngineRow("strong_steam_engine_tin_alloy", "tin_alloy", "Tin Alloy", true, (short)4000, 80000, 40, 4.0F, 4.0F, false),
			new SteamEngineRow("strong_steam_engine_bronze", "bronze", "Bronze", true, (short)5000, 96000, 48, 7.0F, 7.0F, false),
			new SteamEngineRow("strong_steam_engine_arsenic_copper", "arsenic_copper", "Arsenic Copper", true, (short)5000, 96000, 48, 7.0F, 7.0F, false),
			new SteamEngineRow("strong_steam_engine_arsenic_bronze", "arsenic_bronze", "Arsenic Bronze", true, (short)5000, 112000, 56, 7.0F, 7.0F, false),
			new SteamEngineRow("strong_steam_engine_brass", "brass", "Brass", true, (short)5000, 96000, 48, 7.0F, 7.0F, false),
			new SteamEngineRow("strong_steam_engine_invar", "invar", "Invar", true, (short)6400, 64000, 32, 4.0F, 4.0F, false),
			new SteamEngineRow("strong_steam_engine_iron_wood", "iron_wood", "Iron Wood", true, (short)6450, 64000, 32, 4.0F, 4.0F, true),
			new SteamEngineRow("strong_steam_engine_steel", "steel", "Steel", true, (short)5000, 128000, 64, 6.0F, 6.0F, false),
			new SteamEngineRow("strong_steam_engine_fiery_steel", "fiery_steel", "Fiery Steel", true, (short)6200, 256000, 128, 7.0F, 7.0F, false),
			new SteamEngineRow("strong_steam_engine_chromium", "chromium", "Chromium", true, (short)6300, 384000, 192, 4.0F, 4.0F, false),
			new SteamEngineRow("strong_steam_engine_titanium", "titanium", "Titanium", true, (short)5800, 448000, 224, 9.0F, 9.0F, false),
			new SteamEngineRow("strong_steam_engine_tungsten", "tungsten", "Tungsten", true, (short)5800, 512000, 256, 10.0F, 10.0F, false),
			new SteamEngineRow("strong_steam_engine_tungstensteel", "tungstensteel", "Tungstensteel", true, (short)6000, 512000, 256, 12.5F, 12.5F, false));

	/**
	 * The 28 blocks/BlockItems, one pair per row — the GTBarrels METAL_DRUM_BLOCKS loop
	 * shape (the registration-lambda resolves the row's live values; the {@code GT6Kinetics.}
	 * qualified map reads are the legal forward-reference form, the P6 lambda lesson).
	 */
	public static final Map<String, RegistryObject<SteamEngineBlock>> STEAM_ENGINE_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> STEAM_ENGINE_ITEMS = new LinkedHashMap<>();
	static {
		for (SteamEngineRow tRow : STEAM_ENGINES) {
			STEAM_ENGINE_BLOCKS.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new SteamEngineBlock(tRow, tRow.properties())));
			STEAM_ENGINE_ITEMS.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new gregtech6.block.GTComposedNameItem(GT6Kinetics.STEAM_ENGINE_BLOCKS.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The one-line family reference for the shared BET (the GTWires.wireBlockArray shape). */
	public static Block[] steamEngineBlockArray() {
		return STEAM_ENGINE_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	/**
	 * The steam-engine block — the facing cube carrier over the shared BET (the
	 * GTCrankBlock shape minus the click drive: the engine has no GUI, no tool face and no
	 * use() interaction — the soft-hammer toggle is the p12-engine-steam pool cut and the
	 * on/off gate is the {@code /gt6engine mode} command). The row (efficiency/capacity/
	 * output/hardness) rides THIS block (the GTBarrelBlock capacityL registration-carrier
	 * pattern): the BE reads it off the placed BlockState.
	 *
	 * <p>Nested here per the task card ("GT6Kinetics.java — 26-row table append +
	 * efficiency/output carrier"): the whole family lands in this one card-owned file.
	 */
	public static final class SteamEngineBlock extends GTEntityBlock {

		/** Facing property (horizontal — the placement orientation; the KU emit side is this Direction). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		private final SteamEngineRow mRow;

		public SteamEngineBlock(SteamEngineRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends SteamEngineBlock> codec() {
			return simpleCodec(aProperties -> new SteamEngineBlock(STEAM_ENGINES.get(0), aProperties));
		}
		*///?}

		/** The registration row (the GTBarrelBlock.capacityL carrier read). */
		public SteamEngineRow row() {
			return mRow;
		}

		/** The composed row name (task p20-i18n-compose-rows): the family template over the material small unit. */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return steamDisplayOf(mRow);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the front TOWARDS the placer (the GT6PlacementFacing canon, task
			// p28-singleblock-facing-canon) — the KU emit side faces the machine
			return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.STEAM_ENGINE_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			if (aPlacer instanceof net.minecraft.world.entity.player.Player tPlayer && aLevel.getBlockEntity(aPos) instanceof GTSteamEngineBlockEntity tEngine) {
				tEngine.setFacingFromPlacement(tPlayer); // the oven/crank placement mirror
			}
		}
	}

	// -------------------------------------------------------------------------
	// the diesel engine family (task p12-engine-diesel) — 8 material tiers
	// -------------------------------------------------------------------------

	/**
	 * One Loader Diesel Engine row (Loader_MultiTileEntities.java:721-729), name+number only
	 * (no MT at construct time — the AXLE_SPECS ruling). Every upstream row carries the same
	 * shape: NBT_FUELMAP FM.Engine, NBT_EFFICIENCY 10000, NBT_ENERGY_EMITTED TD.Energy.RU,
	 * NBT_HARDNESS 6.0F / NBT_RESISTANCE 6.0F, the "Diesel Engine (Material)" wording, meta
	 * 9145-9149 + 9197-9199 — only the material and NBT_OUTPUT vary, so the table is
	 * material + output only, and the constants live on the BE/block (efficiency on
	 * GTDieselEngineBlockEntity.mEfficiency, hardness/resistance on the registration loop).
	 *
	 * <p>Rows in the upstream declaration order :721-729: Bronze 9147 → 16, ArsenicCopper
	 * 9146 → 16, ArsenicBronze 9145 → 24, Steel (ANY.Steel) 9148 → 32, Invar 9149 → 64,
	 * Ti 9197 → 128, TungstenSteel 9198 → 256, Ir 9199 → 512 RU/t. Material slugs follow the
	 * AXLE_SPECS port material registry form ({@code gt6.material.<slug>}; ANY.Steel →
	 * "steel", MT.Ti → "titanium", MT.Ir → "iridium").
	 */
	/** {@code material} is the row slug (the gt6.row.mat small-unit key tail), {@code matDisplay} the row's material word verbatim. */
	public record DieselSpec(String material, String matDisplay, long output) {}

	/** The composed Diesel Engine display template "{@code %s Diesel Engine}" (task p20-i18n-compose-rows) — one material slot. */
	public static final String DIESEL_DISPLAY_KEY = "gt6.row.diesel.display";

	/** The row's material small-unit key. */
	public static String dieselMatUnitKey(DieselSpec aSpec) {
		return "gt6.row.mat." + aSpec.material();
	}

	/** The composed name of a diesel row (the pure compose seam). */
	public static net.minecraft.network.chat.MutableComponent dieselDisplayOf(DieselSpec aSpec) {
		return net.minecraft.network.chat.Component.translatable(DIESEL_DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(dieselMatUnitKey(aSpec)));
	}

	/** The eight registration rows, the Loader :721-729 declaration order (class doc). */
	public static final List<DieselSpec> DIESEL_SPECS = List.of(
			new DieselSpec("bronze", "Bronze", 16),
			new DieselSpec("arsenic_copper", "Arsenic Copper", 16),
			new DieselSpec("arsenic_bronze", "Arsenic Bronze", 24),
			new DieselSpec("steel", "Steel", 32),
			new DieselSpec("invar", "Invar", 64),
			new DieselSpec("titanium", "Titanium", 128),
			new DieselSpec("tungstensteel", "Tungstensteel", 256),
			new DieselSpec("iridium", "Iridium", 512));

	/** The 8 registered diesel engine blocks (the BET/datagen/loot walkers iterate this). */
	public static final Map<String, RegistryObject<GTDieselEngineBlock>> DIESEL_BLOCKS = new LinkedHashMap<>();

	/** The 8 registered diesel engine items, same keys as {@link #DIESEL_BLOCKS}. */
	public static final Map<String, RegistryObject<Item>> DIESEL_ITEMS = new LinkedHashMap<>();

	/** The registry-name form: {@code diesel_engine_<material>}. */
	public static String dieselName(String aMaterial) {
		return "diesel_engine_" + aMaterial;
	}

	/** The block list in declaration order (the BET multi-mount array + the loot/datagen walkers). */
	public static Block[] dieselBlockArray() {
		Block[] rBlocks = new Block[DIESEL_BLOCKS.size()];
		int i = 0;
		for (RegistryObject<GTDieselEngineBlock> tBlock : DIESEL_BLOCKS.values()) rBlocks[i++] = tBlock.get();
		return rBlocks;
	}

	/**
	 * The diesel engine registration loop — 8 material rows, the Loader :721-729 row body:
	 * NBT_HARDNESS 6.0F / NBT_RESISTANCE 6.0F (every row), the METAL sound of the machine
	 * block convention (the GTMachines Shredder rows). The output rate rides the block
	 * instance (the GTAxleBlock spec carrier form — the 1.20.1 carrier of the upstream
	 * registration NBT NBT_OUTPUT). Same plain-loop-before-attach mechanics as
	 * {@link #registerAxles()}.
	 */
	private static void registerDieselEngines() {
		for (DieselSpec tSpec : DIESEL_SPECS) {
			String tName = dieselName(tSpec.material());
			final DieselSpec fSpec = tSpec;
			RegistryObject<GTDieselEngineBlock> tBlock = BLOCKS.register(tName, () -> new GTDieselEngineBlock(
					BlockBehaviour.Properties.of().strength(6.0F, 6.0F).sound(SoundType.METAL), fSpec));
			DIESEL_BLOCKS.put(tName, tBlock);
			DIESEL_ITEMS.put(tName, ITEMS.register(tName, () -> new gregtech6.block.GTComposedNameItem(tBlock.get(), new Item.Properties())));
		}
	}

	// -------------------------------------------------------------------------
	// the water wheel (task p28-c-water-wheel) — the ULV chain's RU source
	// -------------------------------------------------------------------------

	/**
	 * The Water Wheel (task p28-c-water-wheel) — the kinetics family's RU source: river
	 * flow turns it, it pushes {@code ±8} RU × 1A packets out along its axis into the
	 * axle faces (the kTFRUAddon WaterMill "Water Mill" registration-row semantics,
	 * tileEntityInit0.java:112 WoodTreated hardness 1.5 — clean-room re-expression, the
	 * AGPL behaviour contract only; the numbers are the p28 ULV-chain design ruling: 8 RU
	 * = the GTWireSpecs.V[0] wire domain, the Electric Dynamo T0 row's input-window
	 * centre [4,16], and below the LV machine input-min 16 — see
	 * GT6WaterWheelBlockEntity class doc). The block carries the AXIS property (the
	 * GTAxleBlock carrier form), the properties ride
	 * {@link gregtech6.block.energy.GT6WaterWheelBlock#blockProperties()}.
	 */
	public static final RegistryObject<GT6WaterWheelBlock> WATER_WHEEL = BLOCKS.register("water_wheel",
			() -> new GT6WaterWheelBlock(GT6WaterWheelBlock.blockProperties()));

	public static final RegistryObject<Item> WATER_WHEEL_ITEM = ITEMS.register("water_wheel",
			() -> new BlockItem(WATER_WHEEL.get(), new Item.Properties()));

	private GT6Kinetics() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		registerAxles();
		registerDieselEngines();
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GT6Mod/GTMenuTypes fork precedent.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The tab walk (task p38-tabfix-b-energy — the four single-block kinetic items join
	 * the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream hangs the crank on its "Misc Tool Blocks" category
	 * (tab 32720, Loader_MultiTileEntities.java:2106); this port pools the join into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent). The axle/steam-engine/diesel
	 * item ladders registered in this class stay OUT of this walk (the p38 tail card owns
	 * them) — the census test pins this four-item coverage.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(CRANK_ITEM.get()));
			aEvent.accept(new ItemStack(GEARBOX_ITEM.get()));
			aEvent.accept(new ItemStack(TRANSFORMER_ROTATION_ITEM.get()));
			aEvent.accept(new ItemStack(WATER_WHEEL_ITEM.get()));
		}
	}
}
