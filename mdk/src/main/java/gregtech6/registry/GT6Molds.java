package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.TileEntityFaucet;
import gregtech6.tileentity.tools.TileEntityMold;

/**
 * The mold family registration home (task crucible-physics-smeltery spec ⑤⑥, the
 * ADR-P3-4 self-contained form — GT6Crucibles/GT6BurningBoxes shape). Card A ships the
 * STONE rung only (the Loader_MultiTileEntities.java:347 opening row: "Mold (Stone)",
 * the 7-cobblestone handcraft of the card face); the Bronze/Invar/Steel/Ceramic rungs
 * (:361-366/:352) and the 30 ceramic molds are the card-B surface.
 *
 * <p>The stone mold carries its {@code preCarvedShape} = the ingot bar (the declared
 * minimal-face deviation on {@link TileEntityMold}): the row0 chain needs no chisel
 * gymnastics to cast an ingot.
 *
 * <p><b>Card B append (crucible-mold-faucet)</b>: the CERAMIC rung — the blank mold
 * (Loader:352, one row, no pre-carve) plus the 30 pre-carved shape molds (:391-420, each
 * upstream a one-item-NBT variant; the 1.20.1 vanilla smelting JSON cannot emit item
 * NBT — SimpleCookingSerializer.fromJson reads a bare item id — so each shape is its
 * own block row, the declared port deviation). Each row pairs with a RAW clay item
 * ({@code *_raw}, the MultiItemRandomTools.java:127+ clay chain) that the VANILLA
 * furnace hardens into the formed block item (the :391-420 RM.add_smelting family,
 * datagen-native smelting JSON). The Crucible Faucet rows (:300 Stone / :305 Ceramic)
 * mount {@link gregtech6.tileentity.tools.TileEntityFaucet} over
 * {@link #FAUCET_BE} — the p12 tap/funnel attachment form. All card-B registration
 * rides a SECOND static block + the same DeferredRegisters (the append-only shared-file
 * discipline); {@link #blockArray()} walks the registration map so the shared BET
 * covers every family block in insertion order.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Molds {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	/**
	 * One registration row — the Loader aRegistry.add projection (path + shell material + the pre-carved shape).
	 * The material rides a {@link java.util.function.Supplier} (the GTWireSpecs.Row:81 form — the
	 * class-load-time static rows initialize before MT.init(); a direct MT.Stone captured null).
	 */
	public record MoldRow(String path, java.util.function.Supplier<OreDictMaterial> material, float hardness, int preCarvedShape) {}

	/** The stone rung (the :347 NBT_HARDNESS 1.0 / NBT_RESISTANCE 5.0 pair, pre-carved with the ingot bar). */
	public static final List<MoldRow> ROWS = List.of(
			new MoldRow("mold_stone", () -> MT.Stone, 1.0F, TileEntityMold.ingotShape(0)));

	/** The registered blocks by path. */
	public static final Map<String, RegistryObject<MoldBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (MoldRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new MoldBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), 5.0F) // the :347 NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The mold BET: one BlockEntityType over the family blocks (ADR-P3-1). Registry path
	 * "mold" mirrors {@link TileEntityMold#getTileEntityName}.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityMold>> MOLD_BE =
			BLOCK_ENTITY_TYPES.register("mold", () -> BlockEntityType.Builder.of(
					TileEntityMold::new, blockArray()).build(null));

	/** The block list of the family in registration order (stone first, then the card-B ceramic rung — map insertion order). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[BLOCKS_BY_PATH.size()];
		int i = 0;
		for (RegistryObject<MoldBlock> tHandle : BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static MoldBlock blockByPath(String aPath) {
		RegistryObject<MoldBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ------------------------------------------------------------------------------------
	// card B (crucible-mold-faucet): the ceramic rung + raw clay items + the faucet
	// ------------------------------------------------------------------------------------

	/** The ceramic blank (Loader:352 — the one row without a crafting recipe, shape 0 until carved). */
	public static final MoldRow CERAMIC_BLANK_ROW = new MoldRow("mold_ceramic", () -> MT.Ceramic, 1.0F, 0);

	/**
	 * The 30 pre-carved ceramic molds (Loader_MultiTileEntities.java:391-420, upstream
	 * order). Each mask is the {@code gt.mold} NBT the upstream smelting row wrote onto
	 * the one Ceramic Mold item; here it is the block row's {@code preCarvedShape}. The
	 * Nugget row's single-center-bit mask is deliberately NOT in {@link TileEntityMold
	 * #MOLD_RECIPES} — it answers through the {@code OP.nugget} fallback (:79-83).
	 */
	public static final List<MoldRow> CERAMIC_ROWS = List.of(
			new MoldRow("mold_ceramic_ingot", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_01110_01110), // -> ingot
			new MoldRow("mold_ceramic_billet", () -> MT.Ceramic, 1.0F, 0b0_01100_11110_11110_01100_00000), // -> billet
			new MoldRow("mold_ceramic_chunk", () -> MT.Ceramic, 1.0F, 0b0_11000_11000_00000_00000_00000), // -> chunkGt
			new MoldRow("mold_ceramic_plate", () -> MT.Ceramic, 1.0F, 0b0_11111_11111_11111_11111_11111), // -> plate
			new MoldRow("mold_ceramic_tiny_plate", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01110_01110_00000), // -> plateTiny
			new MoldRow("mold_ceramic_bolt", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_00100_00100_00000), // -> bolt
			new MoldRow("mold_ceramic_rod", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_11111_00000_00000), // -> stick
			new MoldRow("mold_ceramic_long_rod", () -> MT.Ceramic, 1.0F, 0b0_10000_01000_00100_00010_00001), // -> stickLong
			new MoldRow("mold_ceramic_item_casing", () -> MT.Ceramic, 1.0F, 0b0_11101_11101_11101_00001_11100), // -> casingSmall
			new MoldRow("mold_ceramic_ring", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01010_01110_00000), // -> ring
			new MoldRow("mold_ceramic_gear", () -> MT.Ceramic, 1.0F, 0b0_10101_01110_11011_01110_10101), // -> gearGt
			new MoldRow("mold_ceramic_small_gear", () -> MT.Ceramic, 1.0F, 0b0_01010_11111_01010_11111_01010), // -> gearGtSmall
			new MoldRow("mold_ceramic_sword", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01110_01110_01110), // -> toolHeadRawSword
			new MoldRow("mold_ceramic_pickaxe", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_10001_00000_00000), // -> toolHeadRawPickaxe
			new MoldRow("mold_ceramic_spade", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_01010_00000), // -> toolHeadRawSpade
			new MoldRow("mold_ceramic_shovel", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01110_01110_00000), // -> toolHeadRawShovel
			new MoldRow("mold_ceramic_universal_spade", () -> MT.Ceramic, 1.0F, 0b0_00100_01110_01100_01110_00000), // -> toolHeadRawUniversalSpade
			new MoldRow("mold_ceramic_axe", () -> MT.Ceramic, 1.0F, 0b0_00000_01110_01110_01000_00000), // -> toolHeadRawAxe
			new MoldRow("mold_ceramic_double_axe", () -> MT.Ceramic, 1.0F, 0b0_00000_11111_11111_10001_00000), // -> toolHeadRawAxeDouble
			new MoldRow("mold_ceramic_saw", () -> MT.Ceramic, 1.0F, 0b0_00000_11111_11111_00000_00000), // -> toolHeadRawSaw
			new MoldRow("mold_ceramic_hammer", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01010_01110_01110), // -> toolHeadHammer
			new MoldRow("mold_ceramic_file", () -> MT.Ceramic, 1.0F, 0b0_01110_01110_01110_00100_00100), // -> toolHeadFile
			new MoldRow("mold_ceramic_screwdriver", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_00100_00100_00100), // -> toolHeadScrewdriver
			new MoldRow("mold_ceramic_chisel", () -> MT.Ceramic, 1.0F, 0b0_01110_00100_00100_00100_00100), // -> toolHeadRawChisel
			new MoldRow("mold_ceramic_arrow", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_00100_01110_00000), // -> toolHeadRawArrow
			new MoldRow("mold_ceramic_hoe", () -> MT.Ceramic, 1.0F, 0b0_00000_00110_01110_00000_00000), // -> toolHeadRawHoe
			new MoldRow("mold_ceramic_sense", () -> MT.Ceramic, 1.0F, 0b0_00000_01111_11111_00000_00000), // -> toolHeadRawSense
			new MoldRow("mold_ceramic_plow", () -> MT.Ceramic, 1.0F, 0b0_11111_11111_11111_11111_00100), // -> toolHeadRawPlow
			new MoldRow("mold_ceramic_builderwand", () -> MT.Ceramic, 1.0F, 0b0_00000_00100_11111_01110_01010), // -> toolHeadBuilderwand
			new MoldRow("mold_ceramic_nugget", () -> MT.Ceramic, 1.0F, 0b0_00000_00000_00100_00000_00000)  // -> nugget (the :82 fallback)
			);

	/** The raw clay items, one per ceramic row (the {@code *_raw} tail; the MultiItemRandomTools :127+ chain hardens in a vanilla furnace). */
	public static final Map<String, RegistryObject<Item>> RAW_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The raw item of a formed row path ({@code mold_ceramic_plate} → the {@code *_raw} item) — null for an unknown path. */
	@Nullable
	public static Item rawItemByPath(String aFormedPath) {
		RegistryObject<Item> tHandle = RAW_ITEMS_BY_PATH.get(aFormedPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * One faucet registration row (the Loader aRegistry.add projection :300-341 — the
	 * material drives the {@code getMoldMaxTemperature} heat verdict, the acid-proof
	 * column rides the block carrier like every attachment). The material rides the same
	 * {@link java.util.function.Supplier} as {@link MoldRow}: the class-load-time static
	 * rows initialize before MT.init() (the P2 two-phase reset) — a direct MT.Ceramic
	 * captured null and the faucet stat NPE'd at {@code material().mMeltingPoint}
	 * (live-probed: "An unexpected error occurred trying to execute that command").
	 *
	 * @param meta       the upstream MTE id (the zh dump rows mte 1700-1749 key) — the
	 *                   reconciliation anchor, unique per row
	 * @param matDisplay the upstream en local word ({@code aMat.getLocal()}), the
	 *                   {@code gt6.row.faucet.mat.*} en face verbatim
	 * @param acidProof  the upstream NBT_ACIDPROOF (8 rows: SS/Netherite/Thaumium/Cr/Ir/W/VoidMetal/Ad)
	 * @param resistance the upstream NBT_RESISTANCE (5.0 stone/quartz family, 6.0 carbon+metals —
	 *                   NOT derivable from acidProof: carbon is F/6.0)
	 * @param hidden     the upstream NBT_HIDDEN (Loader:301-304 unconditional T; :306-309 the
	 *                   ERE/Botania/Aether/Betweenlands gates — the port's single-mod universe
	 *                   resolves every gate to hidden): registered but no creative-tab join
	 * @param craft      the upstream craft face, see {@link FaucetCraft}
	 */
	public record FaucetRow(String path, int meta, java.util.function.Supplier<OreDictMaterial> material,
			String matDisplay, boolean acidProof, float resistance, boolean hidden, FaucetCraft craft) {}

	/**
	 * The upstream craft face of a faucet row (Loader:300-341), and what the port's recipe
	 * provider does with it:
	 * <ul>
	 * <li>{@link #NONE} — no separate craft row: stone keeps its handcraft (the :300
	 *     "B B"/" B " row, already in {@code GT6MoldDatagen.Recipes}), ceramic keeps the
	 *     raw→furnace pair (:305);</li>
	 * <li>{@link #PLATE_SELF} / {@link #PLATE_GRAPHENE} — the metals' "P P"/" P " plate
	 *     crafts (:314-341) and the carbon row's graphene-plate craft (:312 "C C"/" C "):
	 *     the datagen resolves the {@code gt6:plate_<mat>} item via
	 *     {@code GTMaterialItems.get(OP.plate, ...)}, unresolvable pairs skip with a log;</li>
	 * <li>{@link #CUT_STONE_BLOCK} — the stone-family rows craft from
	 *     {@code OP.stone.dat(aMat)} upstream (:301-309): the port has NO stone-block item
	 *     path yet (GTMaterialItems javadoc: "Block/MTE families are later cards") — the
	 *     craft face is the declared cut, the rows stay obtainable via commands. All 8 rows
	 *     are NBT_HIDDEN upstream, so nothing craft-visible is lost;</li>
	 * <li>{@link #CUT_ANY_GEM} — the ANY.Quartz row's {@code OP.gem.dat(ANY.Quartz)} (:311):
	 *     ANY materials carry UNUSED/INVALID_MATERIAL (ANY.java:40) and generate no port
	 *     item, the ANY-ingredient face has no port item pool — the declared cut.</li>
	 * </ul>
	 */
	public enum FaucetCraft { NONE, PLATE_SELF, PLATE_GRAPHENE, CUT_STONE_BLOCK, CUT_ANY_GEM }

	/**
	 * The full 39-row family (task faucet-material-rows): the Loader_MultiTileEntities
	 * .java:300-341 projection row-for-row in upstream order — 10 stone/ceramic rungs,
	 * quartz + carbon, 27 metal rungs. Every row carries its upstream meta, en local word,
	 * acid/resistance pair, hidden flag and craft face (the FaucetCraft javadoc grounds
	 * each column). 31 rows are creative-visible, 8 NBT_HIDDEN.
	 */
	public static final List<FaucetRow> FAUCET_ROWS = List.of(
			new FaucetRow("faucet_stone"                  , 1700, () -> MT.Stone               , "Stone"                      , false, 5.0F, false, FaucetCraft.NONE),
			new FaucetRow("faucet_basalt"                 , 1701, () -> MT.STONES.Basalt       , "Basalt"                     , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_black_granite"          , 1702, () -> MT.STONES.GraniteBlack , "Black Granite"              , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_red_granite"            , 1703, () -> MT.STONES.GraniteRed   , "Red Granite"                , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_nether_brick"           , 1704, () -> MT.NetherBrick         , "Nether Brick"               , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_ceramic"                , 1705, () -> MT.Ceramic             , "Ceramic"                    , false, 5.0F, false, FaucetCraft.NONE),
			new FaucetRow("faucet_umber"                  , 1706, () -> MT.STONES.Umber        , "Umberstone"                 , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_livingrock"             , 1707, () -> MT.STONES.Livingrock   , "Livingrock"                 , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_holystone"              , 1708, () -> MT.STONES.Holystone    , "Holystone"                  , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_betweenstone"           , 1709, () -> MT.STONES.Betweenstone , "Betweenstone"               , false, 5.0F, true , FaucetCraft.CUT_STONE_BLOCK),
			new FaucetRow("faucet_quartz"                 , 1718, () -> ANY.Quartz             , "Quartz"                     , false, 5.0F, false, FaucetCraft.CUT_ANY_GEM),
			new FaucetRow("faucet_carbon"                 , 1719, () -> MT.C                   , "Carbon"                     , false, 6.0F, false, FaucetCraft.PLATE_GRAPHENE),
			new FaucetRow("faucet_bronze"                 , 1720, () -> MT.Bronze              , "Bronze"                     , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_invar"                  , 1721, () -> MT.Invar               , "Invar"                      , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_steel"                  , 1722, () -> MT.Steel               , "Steel"                      , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_hsla"                   , 1741, () -> MT.HSLA                , "HSLA-Steel"                 , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_stainless_steel"        , 1725, () -> MT.StainlessSteel      , "Stainless Steel"            , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_dark_iron"              , 1726, () -> MT.DarkIron            , "Dark Iron"                  , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_meteoric_iron"          , 1731, () -> MT.MeteoricIron        , "Meteoric Iron"              , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_meteoric_steel"         , 1732, () -> MT.MeteoricSteel       , "Meteoric Steel"             , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_netherite"              , 1744, () -> MT.Netherite           , "Netherite"                  , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_knightmetal"            , 1727, () -> MT.Knightmetal         , "Knightmetal"                , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_fiery_steel"            , 1728, () -> MT.FierySteel          , "Fiery Steel"                , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_octine"                 , 1742, () -> MT.Octine              , "Octine"                     , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_thaumium"               , 1729, () -> MT.Thaumium            , "Thaumium"                   , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_titanium"               , 1723, () -> MT.Ti                  , "Titanium"                   , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_chromium"               , 1733, () -> MT.Cr                  , "Chromium"                   , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_molybdenum"             , 1734, () -> MT.Mo                  , "Molybdenum"                 , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_niobium"                , 1735, () -> MT.Nb                  , "Niobium"                    , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_tantalum"               , 1736, () -> MT.Ta                  , "Tantalum"                   , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_osmium"                 , 1737, () -> MT.Os                  , "Osmium"                     , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_iridium"                , 1739, () -> MT.Ir                  , "Iridium"                    , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_niobium_titanium"       , 1740, () -> MT.NiobiumTitanium     , "Niobium Titanium"           , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_vanadium"               , 1738, () -> MT.V                   , "Vanadium"                   , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_tungsten"               , 1724, () -> ANY.W                  , "Tungsten"                   , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_tantalum_hafnium_carbide", 1743, () -> MT.Ta4HfC5            , "Tantalum Hafnium Carbide"   , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_void_metal"             , 1730, () -> MT.VoidMetal           , "Void Metal"                 , true , 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_bedrock_hsla_alloy"     , 1748, () -> MT.Bedrock_HSLA_Alloy  , "Bedrock-HSLA-Alloy"         , false, 6.0F, false, FaucetCraft.PLATE_SELF),
			new FaucetRow("faucet_adamantium"             , 1749, () -> MT.Ad                  , "Adamantium"                 , true , 6.0F, false, FaucetCraft.PLATE_SELF));

	/**
	 * The registered faucet blocks by path. The value type is the {@link java.util.function.Supplier}
	 * face (RegistryObject and DeferredHolder both implement it): the nested
	 * {@code TileEntityFaucet.FaucetBlock} type argument sits outside the stonecutter
	 * RegistryObject→DeferredHolder swap's simple-name census (the dotted name defeats the
	 * {@code [A-Za-z0-9]+} catch-all), so the supertype keeps the shared source leg-neutral.
	 */
	public static final Map<String, java.util.function.Supplier<TileEntityFaucet.FaucetBlock>> FAUCET_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered faucet items, same keys as {@link #FAUCET_BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> FAUCET_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The composed faucet display template "{@code %s Crucible Faucet}" (the Loader display column word order). */
	public static final String FAUCET_DISPLAY_KEY = "gt6.row.faucet.display";

	/** The faucet row's material small-unit key (the attachment namespace over the {@code faucet_} slug). */
	public static String faucetMatUnitKeyOf(FaucetRow aRow) {
		return "gt6.row.faucet.mat." + aRow.path().substring("faucet_".length());
	}

	/** The faucet BET: one BlockEntityType over the family blocks; registry path "faucet" mirrors the BE name. */
	public static final RegistryObject<BlockEntityType<TileEntityFaucet>> FAUCET_BE =
			BLOCK_ENTITY_TYPES.register("faucet", () -> BlockEntityType.Builder.of(
					TileEntityFaucet::new, faucetBlockArray()).build(null));

	/** The faucet block list of the family in registration order. */
	public static Block[] faucetBlockArray() {
		Block[] rBlocks = new Block[FAUCET_ROWS.size()];
		for (int i = 0; i < FAUCET_ROWS.size(); i++) rBlocks[i] = FAUCET_BLOCKS_BY_PATH.get(FAUCET_ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The faucet lookup for the command/RCON walkers — null for an unknown path. */
	@Nullable
	public static TileEntityFaucet.FaucetBlock faucetBlockByPath(String aPath) {
		java.util.function.Supplier<TileEntityFaucet.FaucetBlock> tHandle = FAUCET_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The raw clay faucet item (Loader:305 — only the Ceramic rung rides the raw→furnace pair). */
	public static final RegistryObject<Item> FAUCET_CERAMIC_RAW = ITEMS.register("faucet_ceramic_raw",
			() -> new Item(new Item.Properties()));

	/**
	 * The card-B registration pass — runs AFTER the card-A static block (multiple static
	 * initializers execute in declaration order), registering the ceramic rows into the
	 * SAME maps the stone rung uses (the BET/datagen/command walkers see one family) and
	 * the faucet family beside them. The GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read
	 * forward-reference form against the P6 lambda lesson.
	 */
	static {
		// the family walk: the carvable blank FIRST (Loader:352), then the 30 pre-carved
		// rows — its block/item/raw trio backs the datagen band (the blank clay handcraft,
		// the smelt_mold_ceramic hardening row) and the command walkers
		for (MoldRow tRow : withBlank(CERAMIC_ROWS)) {
			final MoldRow fRow = tRow;
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new MoldBlock(fRow, BlockBehaviour.Properties.of()
							.strength(fRow.hardness(), 5.0F) // the :352 NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
			RAW_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path() + "_raw",
					() -> new Item(new Item.Properties())));
		}
		for (FaucetRow tRow : FAUCET_ROWS) {
			final FaucetRow fRow = tRow;
			FAUCET_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new TileEntityFaucet.FaucetBlock(fRow, () -> GT6Molds.FAUCET_BE.get(),
							BlockBehaviour.Properties.of()
								.strength(1.0F, fRow.resistance()) // the :300-341 NBT_HARDNESS 1.0 / NBT_RESISTANCE pair (5.0 stone family, 6.0 carbon+metals)
								.sound(SoundType.STONE)))); // collision shape: the attachment base answers empty (TileEntityBase10Attachment:35)
			FAUCET_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Molds.FAUCET_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The family walk order: the carvable blank (Loader:352) leads, the 30 pre-carved rows follow. */
	private static java.util.List<MoldRow> withBlank(java.util.List<MoldRow> aRows) {
		java.util.List<MoldRow> rRows = new java.util.ArrayList<>(aRows.size() + 1);
		rRows.add(CERAMIC_BLANK_ROW);
		rRows.addAll(aRows);
		return rRows;
	}


	// ------------------------------------------------------------------------------------
	// the block carrier (the CrucibleBlock form) + the shape geometry (mold-geometry)
	// ------------------------------------------------------------------------------------

	/**
	 * The upstream cell-grid width in px (MultiTileEntityMold.java:459-509 MOLD_BOUNDS,
	 * render passes 18-42): the 5x5 grid spans the PX_P[2]..PX_N[2] window (2..14px,
	 * 12px wide), so each cell is 12/5 = 2.4px. The lit cells sink to the 1px floor
	 * (the concave cavity); the unlit cells stand 0..3px (PX_P[0]..PX_N[13]) as the
	 * surface. The floor is the full 16x16 footprint, 1px tall (MOLD_BOUNDS[1] =
	 * PX_N[15]).
	 */
	public static final float CELL_PX = 12F / 5F;

	/** The low edge (px) of grid line c (0..5) — PX_P[2] + c * PX_P[12]/5 (MOLD_BOUNDS[18+i] form). */
	public static float cellLo(int c) {
		return 2F + c * CELL_PX;
	}

	/** The high edge of cell c — PX_N[2] - (4-c) * PX_P[12]/5 (algebraically cellLo(c) + CELL_PX). */
	public static float cellHi(int c) {
		return cellLo(c + 1);
	}

	/**
	 * The selection box, upstream verbatim (getSelectedBoundingBoxFromPool,
	 * MultiTileEntityMold.java:560): the full 16x16 footprint, 3px tall. The r7
	 * per-bitmap selection died with issue #41 — after the render-polarity flip the
	 * cavities are the HOLES, and a mask-shaped selection made the concave mold
	 * un-clickable in the pit (the top-face click IS the NO_GUI interface, :268).
	 */
	public static final net.minecraft.world.phys.shapes.VoxelShape SELECTION_SHAPE = Block.box(0, 0, 0, 16, 3, 16);

	/**
	 * The collision box, upstream verbatim (getCollisionBoundingBoxFromPool,
	 * MultiTileEntityMold.java:559): the 12x12px inner cavity, 2px deep — entities sink
	 * into the pit between the 2px walls.
	 */
	public static final net.minecraft.world.phys.shapes.VoxelShape COLLISION_SHAPE = Block.box(2, 0, 2, 14, 2, 14);

	/**
	 * The mold block — a bitmap-stamped carrier over the shared BET; the top-face click IS
	 * the NO_GUI interface (the onBlockActivated3 SIDES_TOP gate, :268). Selection rides
	 * the upstream full-footprint box and collision the inner-cavity box (the
	 * MultiTileEntityMold.java:559-560 pair) — shape-independent, so the mask can never
	 * break clickability again (issue #41).
	 */
	public static final class MoldBlock extends GTEntityBlock {

		private final MoldRow mRow;

		public MoldBlock(MoldRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}

		/** The upstream full-footprint 3px box (MultiTileEntityMold.java:560). */
		@Override
		public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
			return SELECTION_SHAPE;
		}

		/** The upstream 12x12x2px inner cavity (MultiTileEntityMold.java:559). */
		@Override
		public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
			return COLLISION_SHAPE;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends MoldBlock> codec() {
			return simpleCodec(aProperties -> new MoldBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row. */
		public MoldRow row() {
			return mRow;
		}

		/** The composed display name (the lang provider key). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable("gt6.row.mold.display." + mRow.path());
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Molds.MOLD_BE.get();
		}

		@Override
		public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
			return net.minecraft.world.level.block.RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		//? if forge {
		public net.minecraft.world.InteractionResult use(BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		//?} else {
		/*public net.minecraft.world.InteractionResult useWithoutItem(BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (javap 21.1.249) — the
		//InteractionHand param dropped from the signature; the game loop drives the hands
		//in order and MAIN_HAND is the canonical first entry.
		net.minecraft.world.InteractionHand aHand = net.minecraft.world.InteractionHand.MAIN_HAND;
		*///?}
			// the :268 SIDES_TOP gate — only the top face reacts
			if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntityMold tMold) {
				if (!aLevel.isClientSide) tMold.useTop(aPlayer, aHand);
				return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
	}

	private GT6Molds() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBlockEntities.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework; a self-contained
		//listener reaches the mod bus through its mod container (javap loader-4.0.44:
		//ModContainer.getEventBus public abstract) — the GTMachines fork precedent.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}

	/**
	 * The MACHINES_TAB join (task tabfix-c-misc — the census zero-tab adjudication;
	 * the GT6BurningBoxes.onBuildTabContents verbatim form, delivered by the class-level
	 * MOD-bus {@code @Mod.EventBusSubscriber} at the class head). JEI 1.20.1 derives its
	 * item list from the tab display items, so registered-but-tab-less was invisible in
	 * BOTH the creative menu and JEI.
	 *
	 * <p><b>Pool cut (the census ruling, task tab-census)</b>: upstream the molds ride
	 * the per-family MTE tab "Molds" (1072, Loader_MultiTileEntities.java:347-352/:391-420)
	 * and the faucets the "Crucibles Faucets" tab (1722, :300/:305) — this port pools both
 * finished families into MACHINES_TAB. The RAW clay items stay OUT ({@link
 * #RAW_ITEMS_BY_PATH} 31 + {@link #FAUCET_CERAMIC_RAW} 1 = 32): upstream they are the
 * craft-only furnace-hardening intermediates (MultiItemRandomTools.java:127+), never a
 * tab member. The faucet rows ride the upstream NBT_HIDDEN split (task
 * faucet-material-rows): the 31 visible rows join, the 8 hidden stone-family rows
 * (Loader:301-304/:306-309) stay out — the raw split rides the finished/raw axis, not a
 * family axis.
 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
			// the NBT_HIDDEN rows (Loader:301-304/:306-309) stay out of the tab — the
			// upstream hidden semantics (no NEI/creative face, the block itself registered)
			for (FaucetRow tRow : FAUCET_ROWS) {
				if (tRow.hidden()) continue;
				aEvent.accept(new ItemStack(FAUCET_ITEMS_BY_PATH.get(tRow.path()).get()));
			}
		}
	}
}
