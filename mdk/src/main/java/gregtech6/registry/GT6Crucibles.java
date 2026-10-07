package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.block.multiblock.GTCrucibleControllerBlock;
import gregtech6.block.multiblock.GTCrucibleWallBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.CrucibleWallBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;
import gregtech6.tileentity.tools.TileEntityBasin;
import gregtech6.tileentity.tools.TileEntityCrossing;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * The crucible family registration home — the A/B/C SHARED registration point
 * (tasks.p26-arch-crucible-chain ⑥, the ADR-P3-4 self-contained form —
 * GT6BurningBoxes/GT6Boilers shape): block + item + BET DeferredRegisters attached from
 * the construct event, {@code GTMachines.java} and {@code GTMultiBlocks.java} untouched.
 *
 * <p><b>The union</b> (the S8 merge of tasks crucible-multiblock and
 * crucible-mold-faucet — the A/B/C bodies landed as ONE DeferredRegister trio, the
 * same field names, one onModConstruct attach, exactly the union this file's discipline
 * paragraph declared): <ul>
 * <li><b>the SMALL Smeltery rows</b> (task crucible-physics-smeltery spec ⑥ — the
 *     Loader_MultiTileEntities.java:250-289 projection, the Stone/Bronze/Steel minimal
 *     ladder; the {@link SmelteryRow} carrier with the lazy {@link java.util.function.Supplier}
 *     material — the static rows initialize at class-load time BEFORE MT.init() assigns the
 *     OreDictMaterial statics (the P2 two-phase reset), a direct MT.Steel reference captured
 *     null and every shell read as the Stone fallback ceiling — live-probed on the B card)，
 *     mounted by {@link TileEntitySmeltery} through {@link #CRUCIBLE_BE} over the
 *     {@link CrucibleBlock} carrier with the {@link CrucibleBlock#LIQUID_LEVEL} fill-height
 *     property (spec ⑦, the mDisplayedHeight census bucketed to 9 datagen-native variants);</li>
 * <li><b>the LARGE-crucible rows</b> (task crucible-multiblock SPEC ⑤, the ladder
 *     completed by task w3-distill-crucible ③): the wall is the upstream metal-wall
 *     family (the "Steel Wall" part id 18009, Loader:1145 — hardness == resistance 6.0, the
 *     NBT_DESIGN :1270 wall reference as a Block identity); the controller is the "Large
 *     Steel Crucible" (:1270 — MTE id 17309, NBT_ACIDPROOF F). The FULL 8-material ladder
 *     (:1270-1277 — Steel/SS/Invar/Ti/WSteel/W/Ta4HfC5/Ad, ACIDPROOF T on SS/W/Ad ONLY) is
 *     NOW IN REGISTER: the shell material is what the ladder swaps (the P26 CruciblePhysics
 *     ceiling input, melt point × 1.10, via {@link TileEntityCrucibleRow}) and each rung
 *     carries its dedicated {@link GTCrucibleWallBlock} (the mold-pour relay the shared
 *     machine walls do not carry). Two family BETs: the wall BET mounts the relaying
 *     {@link CrucibleWallBlockEntity} over all eight walls; the controller BET mounts
 *     {@link TileEntityCrucibleRow}.</li>
 * </ul>
 *
	 * <p>Creative tab (task tabfix-a-multiblock, split by material-mc-c-crucible-rows along
	 * the small-crucible-boiler-tab-rehome ruling): the single-block families — the 39
	 * Smeltery rungs, the 39 Basins, the 39 Crossings (hidden rows excluded) — join the
	 * gt6:machines tab; the LARGE family (8 controllers + Steel wall + 7 ladder walls)
	 * stays on gt6:multiblocks via {@link #onBuildTabContents}. Pool cut declared: upstream
	 * rode the per-family MTE tabs ("Smelting Crucibles" :251-292 / "Molds" :425-513 /
	 * the multiblock tab :1270-1277).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Crucibles {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	// ------------------------------------------------------------------------------------
	// the SMALL Smeltery family (task crucible-physics-smeltery spec ⑥/⑦)
	// ------------------------------------------------------------------------------------

	/**
	 * One Smeltery registration row — the Loader aRegistry.add projection (path + shell
	 * material + hardness pair). The material rides a {@link java.util.function.Supplier}
	 * (the GTWireSpecs.Row:81 form): the static ROWS initialize at class-load time, which
	 * on the mod bus runs before MT.init() assigns the OreDictMaterial statics (the P2
	 * two-phase material reset) — a direct MT.Steel reference captured null and every
	 * shell read as the Stone fallback ceiling (live-probed: all three rungs answered
	 * max=1375K with row.material()==NULL).
	 */
	public record SmelteryRow(String path, java.util.function.Supplier<OreDictMaterial> material, String display, float hardness) {}

	/**
	 * One SHARED crucible-domain material row — the 39-entry table the three families
	 * (Smeltery :251-292 / Basin :425-466 / Crossing :471-513) all walk in the SAME order
	 * (task material-mc-c-crucible-rows — the census mc-C reconciliation ledger, one anchor
	 * per material). The hardness column is the upstream NBT_HARDNESS==NBT_RESISTANCE pair
	 * verbatim; {@code hidden} is the NBT_HIDDEN column (the four mod-stones hide forever —
	 * their gating mods ERE/BOTA/AETHER/BTL have no 1.20.1 port); {@code craft} drives the
	 * recipe ingredient resolution (the faucet-card resolvable gate: a pair without a port
	 * item path emits NO recipe JSON).
	 */
	public record CrucibleMaterial(String slug, java.util.function.Supplier<OreDictMaterial> material, float hardness,
			boolean hidden, CraftKind craft) {

		/** The upstream craft 'X' column (Loader_MultiTileEntities :251-292 tail args). */
		public enum CraftKind { NONE, STONE, GEM, PLATE_SELF, PLATE_GRAPHENE }

		/** The material (the supplier dereference — the P2 two-phase reset lesson). */
		public OreDictMaterial mt() {
			return material.get();
		}

		/** The row path of a family ({@code "smeltery"}/{@code "basin"}/{@code "crossing"} prefix). */
		public String pathOf(String aFamilyPrefix) {
			return aFamilyPrefix + "_" + slug;
		}
	}

	/** :251 — the opening row (the port 6.0 hardness is the recorded issue-#45-C2 deviation, upstream says 5.0). */
	public static final CrucibleMaterial MAT_STONE = new CrucibleMaterial("stone", () -> MT.Stone, 6.0F, false, CrucibleMaterial.CraftKind.STONE);
	/** :252 (hidden T). */
	public static final CrucibleMaterial MAT_BASALT = new CrucibleMaterial("basalt", () -> MT.STONES.Basalt, 15.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :253 (hidden T). */
	public static final CrucibleMaterial MAT_GRANITE_BLACK = new CrucibleMaterial("granite_black", () -> MT.STONES.GraniteBlack, 15.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :254 (hidden T). */
	public static final CrucibleMaterial MAT_GRANITE_RED = new CrucibleMaterial("granite_red", () -> MT.STONES.GraniteRed, 15.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :255 (hidden T). */
	public static final CrucibleMaterial MAT_NETHER_BRICK = new CrucibleMaterial("nether_brick", () -> MT.NetherBrick, 5.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :256 — the raw-clay chain row, the only craft-less kind. */
	public static final CrucibleMaterial MAT_CERAMIC = new CrucibleMaterial("ceramic", () -> MT.Ceramic, 5.0F, false, CrucibleMaterial.CraftKind.NONE);
	/** :257 — hidden forever (the ERE gate, no 1.20.1 port). */
	public static final CrucibleMaterial MAT_UMBER = new CrucibleMaterial("umber", () -> MT.STONES.Umber, 5.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :258 — hidden forever (the BOTA gate). */
	public static final CrucibleMaterial MAT_LIVINGROCK = new CrucibleMaterial("livingrock", () -> MT.STONES.Livingrock, 5.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :259 — hidden forever (the AETHER/AETHEL gate). */
	public static final CrucibleMaterial MAT_HOLYSTONE = new CrucibleMaterial("holystone", () -> MT.STONES.Holystone, 5.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :260 — hidden forever (the BTL gate). */
	public static final CrucibleMaterial MAT_BETWEENSTONE = new CrucibleMaterial("betweenstone", () -> MT.STONES.Betweenstone, 5.0F, true, CrucibleMaterial.CraftKind.STONE);
	/** :262 ({@code ANY.Quartz}, the gem craft). */
	public static final CrucibleMaterial MAT_QUARTZ = new CrucibleMaterial("quartz", () -> gregapi.data.ANY.Quartz, 5.0F, false, CrucibleMaterial.CraftKind.GEM);
	/** :263 ({@code MT.C}, the graphene-plate craft). */
	public static final CrucibleMaterial MAT_GRAPHITE = new CrucibleMaterial("graphite", () -> MT.C, 10.0F, false, CrucibleMaterial.CraftKind.PLATE_GRAPHENE);
	/** :265. */
	public static final CrucibleMaterial MAT_BRONZE = new CrucibleMaterial("bronze", () -> MT.Bronze, 7.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :266. */
	public static final CrucibleMaterial MAT_INVAR = new CrucibleMaterial("invar", () -> MT.Invar, 4.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :267. */
	public static final CrucibleMaterial MAT_STEEL = new CrucibleMaterial("steel", () -> MT.Steel, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :268. */
	public static final CrucibleMaterial MAT_HSLA = new CrucibleMaterial("hsla", () -> MT.HSLA, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :269 (upstream NBT_ACIDPROOF T — the shell row, not the render). */
	public static final CrucibleMaterial MAT_STAINLESS_STEEL = new CrucibleMaterial("stainless_steel", () -> MT.StainlessSteel, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :270. */
	public static final CrucibleMaterial MAT_DARK_IRON = new CrucibleMaterial("dark_iron", () -> MT.DarkIron, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :271. */
	public static final CrucibleMaterial MAT_METEORIC_IRON = new CrucibleMaterial("meteoric_iron", () -> MT.MeteoricIron, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :272. */
	public static final CrucibleMaterial MAT_METEORIC_STEEL = new CrucibleMaterial("meteoric_steel", () -> MT.MeteoricSteel, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :273 (acidproof T). */
	public static final CrucibleMaterial MAT_NETHERITE = new CrucibleMaterial("netherite", () -> MT.Netherite, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :274. */
	public static final CrucibleMaterial MAT_KNIGHTMETAL = new CrucibleMaterial("knightmetal", () -> MT.Knightmetal, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :275. */
	public static final CrucibleMaterial MAT_FIERY_STEEL = new CrucibleMaterial("fiery_steel", () -> MT.FierySteel, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :276. */
	public static final CrucibleMaterial MAT_OCTINE = new CrucibleMaterial("octine", () -> MT.Octine, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :277 (acidproof T). */
	public static final CrucibleMaterial MAT_THAUMIUM = new CrucibleMaterial("thaumium", () -> MT.Thaumium, 6.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :278. */
	public static final CrucibleMaterial MAT_TITANIUM = new CrucibleMaterial("titanium", () -> MT.Ti, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :279 (acidproof T). */
	public static final CrucibleMaterial MAT_CHROMIUM = new CrucibleMaterial("chromium", () -> MT.Cr, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :280. */
	public static final CrucibleMaterial MAT_MOLYBDENUM = new CrucibleMaterial("molybdenum", () -> MT.Mo, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :281. */
	public static final CrucibleMaterial MAT_NIOBIUM = new CrucibleMaterial("niobium", () -> MT.Nb, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :282. */
	public static final CrucibleMaterial MAT_TANTALUM = new CrucibleMaterial("tantalum", () -> MT.Ta, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :283. */
	public static final CrucibleMaterial MAT_OSMIUM = new CrucibleMaterial("osmium", () -> MT.Os, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :284 (acidproof T). */
	public static final CrucibleMaterial MAT_IRIDIUM = new CrucibleMaterial("iridium", () -> MT.Ir, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :285. */
	public static final CrucibleMaterial MAT_NIOBIUM_TITANIUM = new CrucibleMaterial("niobium_titanium", () -> MT.NiobiumTitanium, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :286. */
	public static final CrucibleMaterial MAT_VANADIUM = new CrucibleMaterial("vanadium", () -> MT.V, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :287 ({@code ANY.W}, acidproof T). */
	public static final CrucibleMaterial MAT_TUNGSTEN = new CrucibleMaterial("tungsten", () -> MT.W, 10.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :288. */
	public static final CrucibleMaterial MAT_TANTALUM_HAFNIUM_CARBIDE = new CrucibleMaterial("tantalum_hafnium_carbide", () -> MT.Ta4HfC5, 9.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :289 (acidproof T). */
	public static final CrucibleMaterial MAT_VOID_METAL = new CrucibleMaterial("void_metal", () -> MT.VoidMetal, 10.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :291. */
	public static final CrucibleMaterial MAT_BEDROCK_HSLA_ALLOY = new CrucibleMaterial("bedrock_hsla_alloy", () -> MT.Bedrock_HSLA_Alloy, 100.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);
	/** :292 (acidproof T). */
	public static final CrucibleMaterial MAT_ADAMANTIUM = new CrucibleMaterial("adamantium", () -> MT.Ad, 100.0F, false, CrucibleMaterial.CraftKind.PLATE_SELF);

	/** The 39 loader lines in registration order (:251-292 verbatim — the three families share it). */
	public static final List<CrucibleMaterial> MATERIALS = List.of(
			MAT_STONE, MAT_BASALT, MAT_GRANITE_BLACK, MAT_GRANITE_RED, MAT_NETHER_BRICK,
			MAT_CERAMIC, MAT_UMBER, MAT_LIVINGROCK, MAT_HOLYSTONE, MAT_BETWEENSTONE,
			MAT_QUARTZ, MAT_GRAPHITE,
			MAT_BRONZE, MAT_INVAR, MAT_STEEL, MAT_HSLA, MAT_STAINLESS_STEEL, MAT_DARK_IRON,
			MAT_METEORIC_IRON, MAT_METEORIC_STEEL, MAT_NETHERITE, MAT_KNIGHTMETAL, MAT_FIERY_STEEL,
			MAT_OCTINE, MAT_THAUMIUM, MAT_TITANIUM, MAT_CHROMIUM, MAT_MOLYBDENUM, MAT_NIOBIUM,
			MAT_TANTALUM, MAT_OSMIUM, MAT_IRIDIUM, MAT_NIOBIUM_TITANIUM, MAT_VANADIUM, MAT_TUNGSTEN,
			MAT_TANTALUM_HAFNIUM_CARBIDE, MAT_VOID_METAL,
			MAT_BEDROCK_HSLA_ALLOY, MAT_ADAMANTIUM);

	/** The material lookup behind the shared table — loud on an unknown slug (the GT6Hoppers.mt() drift discipline). */
	public static CrucibleMaterial materialBySlug(String aSlug) {
		for (CrucibleMaterial tMat : MATERIALS) if (tMat.slug().equals(aSlug)) return tMat;
		throw new IllegalArgumentException("unknown crucible-domain material slug: " + aSlug);
	}

	/**
	 * The 39 rungs (task material-mc-c-crucible-rows — the Loader_MultiTileEntities
	 * .java:250-292 FULL ladder; the census mc-C smeltery gap of 35 rows closed). The four
	 * former rungs keep their rows at their upstream positions (the stone 6.0 hardness is
	 * the recorded issue-#45-C2 deviation — upstream says 5.0); the new rungs carry the
	 * upstream hardness column verbatim. The Ceramic rung (:256) keeps its raw-clay chain
	 * and stays the only craft-less row. The {@code display} column is documentation — the
	 * real names ride the per-path lang keys.
	 */
	public static final List<SmelteryRow> ROWS = List.of(
		new SmelteryRow("smeltery_stone"      , () -> MT.Stone               , "Stone Smeltery"      , 6.0F ), // :251
		new SmelteryRow("smeltery_basalt"     , () -> MT.STONES.Basalt       , "Basalt Smeltery"     , 15.0F), // :252
		new SmelteryRow("smeltery_granite_black", () -> MT.STONES.GraniteBlack, "Black Granite Smeltery", 15.0F), // :253
		new SmelteryRow("smeltery_granite_red", () -> MT.STONES.GraniteRed   , "Red Granite Smeltery", 15.0F), // :254
		new SmelteryRow("smeltery_nether_brick", () -> MT.NetherBrick        , "Nether Brick Smeltery", 5.0F), // :255
		new SmelteryRow("smeltery_ceramic"    , () -> MT.Ceramic             , "Ceramic Smeltery"    , 5.0F ), // :256
		new SmelteryRow("smeltery_umber"      , () -> MT.STONES.Umber        , "Umber Smeltery"      , 5.0F ), // :257
		new SmelteryRow("smeltery_livingrock" , () -> MT.STONES.Livingrock   , "Livingrock Smeltery" , 5.0F ), // :258
		new SmelteryRow("smeltery_holystone"  , () -> MT.STONES.Holystone    , "Holystone Smeltery"  , 5.0F ), // :259
		new SmelteryRow("smeltery_betweenstone", () -> MT.STONES.Betweenstone, "Betweenstone Smeltery", 5.0F), // :260
		new SmelteryRow("smeltery_quartz"     , () -> gregapi.data.ANY.Quartz, "Quartz Smeltery"     , 5.0F ), // :262
		new SmelteryRow("smeltery_graphite"   , () -> MT.C                   , "Graphite Smeltery"   , 10.0F), // :263
		new SmelteryRow("smeltery_bronze"     , () -> MT.Bronze              , "Bronze Smeltery"     , 7.0F ), // :265
		new SmelteryRow("smeltery_invar"      , () -> MT.Invar               , "Invar Smeltery"      , 4.0F ), // :266
		new SmelteryRow("smeltery_steel"      , () -> MT.Steel               , "Steel Smeltery"      , 6.0F ), // :267
		new SmelteryRow("smeltery_hsla"       , () -> MT.HSLA                , "HSLA Smeltery"       , 6.0F ), // :268
		new SmelteryRow("smeltery_stainless_steel", () -> MT.StainlessSteel  , "Stainless Steel Smeltery", 6.0F), // :269
		new SmelteryRow("smeltery_dark_iron"  , () -> MT.DarkIron            , "Dark Iron Smeltery"  , 6.0F ), // :270
		new SmelteryRow("smeltery_meteoric_iron", () -> MT.MeteoricIron      , "Meteoric Iron Smeltery", 6.0F), // :271
		new SmelteryRow("smeltery_meteoric_steel", () -> MT.MeteoricSteel    , "Meteoric Steel Smeltery", 6.0F), // :272
		new SmelteryRow("smeltery_netherite"  , () -> MT.Netherite           , "Netherite Smeltery"  , 6.0F ), // :273
		new SmelteryRow("smeltery_knightmetal", () -> MT.Knightmetal         , "Knightmetal Smeltery", 6.0F ), // :274
		new SmelteryRow("smeltery_fiery_steel", () -> MT.FierySteel          , "Fiery Steel Smeltery", 6.0F ), // :275
		new SmelteryRow("smeltery_octine"     , () -> MT.Octine              , "Octine Smeltery"     , 6.0F ), // :276
		new SmelteryRow("smeltery_thaumium"   , () -> MT.Thaumium            , "Thaumium Smeltery"   , 6.0F ), // :277
		new SmelteryRow("smeltery_titanium"   , () -> MT.Ti                  , "Titanium Smeltery"   , 9.0F ), // :278
		new SmelteryRow("smeltery_chromium"   , () -> MT.Cr                  , "Chromium Smeltery"   , 9.0F ), // :279
		new SmelteryRow("smeltery_molybdenum" , () -> MT.Mo                  , "Molybdenum Smeltery" , 9.0F ), // :280
		new SmelteryRow("smeltery_niobium"    , () -> MT.Nb                  , "Niobium Smeltery"    , 9.0F ), // :281
		new SmelteryRow("smeltery_tantalum"   , () -> MT.Ta                  , "Tantalum Smeltery"   , 9.0F ), // :282
		new SmelteryRow("smeltery_osmium"     , () -> MT.Os                  , "Osmium Smeltery"     , 9.0F ), // :283
		new SmelteryRow("smeltery_iridium"    , () -> MT.Ir                  , "Iridium Smeltery"    , 9.0F ), // :284
		new SmelteryRow("smeltery_niobium_titanium", () -> MT.NiobiumTitanium, "Niobium Titanium Smeltery", 9.0F), // :285
		new SmelteryRow("smeltery_vanadium"   , () -> MT.V                   , "Vanadium Smeltery"   , 9.0F ), // :286
		new SmelteryRow("smeltery_tungsten"   , () -> MT.W                   , "Tungsten Smeltery"   , 10.0F), // :287
		new SmelteryRow("smeltery_tantalum_hafnium_carbide", () -> MT.Ta4HfC5, "Tantalum Hafnium Carbide Smeltery", 9.0F), // :288
		new SmelteryRow("smeltery_void_metal" , () -> MT.VoidMetal           , "Void Metal Smeltery" , 10.0F), // :289
		new SmelteryRow("smeltery_bedrock_hsla_alloy", () -> MT.Bedrock_HSLA_Alloy, "Bedrock-HSLA-Alloy Smeltery", 100.0F), // :291
		new SmelteryRow("smeltery_adamantium" , () -> MT.Ad                  , "Adamantium Smeltery" , 100.0F)); // :292

	/**
	 * The raw clay crucible (issue #45 C2): upstream IL.Ceramic_Crucible_Raw, meta 989
	 * "Clay Crucible" / "Put in Furnace to harden" (MultiItemRandomTools.java:113, the
	 * U*7 clay row). The vanilla furnace hardens it into the {@code smeltery_ceramic}
	 * block item (the Loader :256 RM.add_smelting tail — the GT6CrucibleDatagen smelt
	 * row); the raw item rides the same craft-only posture as the
	 * {@link GT6Molds#RAW_ITEMS_BY_PATH} family — registered, craft-acquired, never a
	 * creative-tab member.
	 */
	public static final RegistryObject<Item> CLAY_CRUCIBLE_RAW = ITEMS.register("clay_crucible_raw",
			() -> new Item(new Item.Properties()));

	/**
	 * The raw clay basin/crossing (task material-mc-c-crucible-rows — the :430/:477
	 * {@code IL.Ceramic_Basin_Raw}/{@code IL.Ceramic_Crossing_Raw} pair, both U*5): the
	 * vanilla furnace hardens them into the {@code basin_ceramic}/{@code crossing_ceramic}
	 * block items (the smeltery_ceramic chain form). Registered, craft-acquired, never a
	 * creative-tab member.
	 */
	public static final RegistryObject<Item> BASIN_CERAMIC_RAW = ITEMS.register("basin_ceramic_raw",
			() -> new Item(new Item.Properties()));

	public static final RegistryObject<Item> CROSSING_CERAMIC_RAW = ITEMS.register("crossing_ceramic_raw",
			() -> new Item(new Item.Properties()));

	/** The registered Smeltery blocks by path (the BET/datagen/command walkers iterate this). */
	public static final Map<String, RegistryObject<CrucibleBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered Smeltery items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (SmelteryRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new CrucibleBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), tRow.hardness() * 2) // the upstream NBT_HARDNESS/NBT_RESISTANCE pair
							.sound(SoundType.STONE))));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Crucibles.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/**
	 * The shared Smeltery BET: one BlockEntityType over the four shell blocks (ADR-P3-1,
	 * the one-TE-class-many-material-blocks counterpart). Registry path "smeltery" mirrors
	 * {@link TileEntitySmeltery#getTileEntityName}. The blocks register before the BET
	 * (vanilla registry order across DeferredRegisters), so the .get() calls are safe.
	 */
	public static final RegistryObject<BlockEntityType<TileEntitySmeltery>> CRUCIBLE_BE =
			BLOCK_ENTITY_TYPES.register("smeltery", () -> BlockEntityType.Builder.of(
					TileEntitySmeltery::new, blockArray()).build(null));

	/** The Smeltery block list of the family in registration order (the BET multi-mount form). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[ROWS.size()];
		for (int i = 0; i < ROWS.size(); i++) rBlocks[i] = BLOCKS_BY_PATH.get(ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static CrucibleBlock blockByPath(String aPath) {
		RegistryObject<CrucibleBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// ------------------------------------------------------------------------------------
	// the block carrier (the BurningBoxBlock form: the row rides the instance)
	// ------------------------------------------------------------------------------------

	/**
	 * The crucible block — a plain cube carrier over the shared BET; the shell material
	 * rides the row, the fill height rides {@link #LIQUID_LEVEL}, and the top-face click
	 * opens the NO_GUI world interaction (the onBlockActivated3 SIDES_TOP gate, :413).
	 */
	public static final class CrucibleBlock extends GTEntityBlock {

		/**
		 * The fill-height property (spec ⑦): 0 = empty, 8 = full — the mDisplayedHeight
		 * :298 {@code UT.Code.scale(tTotal, MAX_AMOUNT, 255, F)} census bucketed to 9
		 * datagen-native variants. Single-owner property (the P16 identity lesson).
		 */
		public static final IntegerProperty LIQUID_LEVEL = IntegerProperty.create("gt_liquid_level", 0, 8);

		/**
		 * The content-phase property (task crucible-render-followup — the symptom-B face):
		 * true while the lightest content is MOLTEN (the census gate
		 * {@code mMeltingPoint <= mTemperature}, MultiTileEntitySmeltery.java:299), false
		 * once the charge cools. The blockstate doubles over it (the molten-art vs solid-art
		 * content seat), the tint listener reads the SAME state so texture and colour can
		 * never disagree — the upstream pass-5 renders whenever the fill census is non-zero
		 * (:616) and picks the art by the mDisplayedFluid validity (:587-591); the port's
		 * model texture is static per state, so the phase needs the blockstate voice.
		 */
		public static final BooleanProperty MOLTEN = BooleanProperty.create("molten");

		private final SmelteryRow mRow;

		public CrucibleBlock(SmelteryRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(LIQUID_LEVEL, 0).setValue(MOLTEN, false));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends CrucibleBlock> codec() {
			return simpleCodec(aProperties -> new CrucibleBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the GTBarrelBlock.capacityL carrier read). */
		public SmelteryRow row() {
			return mRow;
		}

		/** The composed display name (the row display column; composed keys ride the lang provider). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable("gt6.row.crucible.display." + mRow.path());
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(LIQUID_LEVEL, MOLTEN);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Crucibles.CRUCIBLE_BE.get();
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
			// the :413 SIDES_TOP gate — only the top face reacts (the NO_GUI contract)
			if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntitySmeltery tCrucible) {
				if (!aLevel.isClientSide) tCrucible.useTop(aPlayer, aHand);
				return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
	}

	/** (unused today) the ItemStack display helper for the /give-facing items. */
	public static Component displayOf(SmelteryRow aRow) {
		return Component.translatable("gt6.row.crucible.display." + aRow.path());
	}

	// ------------------------------------------------------------------------------------
	// the BASIN family (task material-mc-c-crucible-rows — Loader:424-466, design 1072,
	// the upstream "Molds" group; the mold-that-casts-blocks rides TileEntityBasin)
	// ------------------------------------------------------------------------------------

	/** The composed basin display template "{@code Basin (%s)}" (the GT6Hoppers.DISPLAY_KEY form). */
	public static final String BASIN_DISPLAY_KEY = "gt6.row.basin.display";

	/**
	 * The 39 basin rows — the {@link #MATERIALS} table projected 1:1 (same order, same
	 * hardness; the {@code display} column carries the slug for the census walk — the real
	 * name composes {@link #BASIN_DISPLAY_KEY} over {@code gt6.row.mat.<slug>}).
	 */
	public static final List<SmelteryRow> BASIN_ROWS = MATERIALS.stream()
			.map(tMat -> new SmelteryRow(tMat.pathOf("basin"), tMat.material(), tMat.slug(), tMat.hardness()))
			.toList();

	/** The registered Basin blocks by path. */
	public static final Map<String, RegistryObject<BasinBlock>> BASIN_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered Basin items, same keys. */
	public static final Map<String, RegistryObject<Item>> BASIN_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (SmelteryRow tRow : BASIN_ROWS) {
			BASIN_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new BasinBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), tRow.hardness() * 2) // the upstream NBT_HARDNESS/NBT_RESISTANCE pair (the smeltery family form)
							.sound(SoundType.STONE))));
			BASIN_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Crucibles.BASIN_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The shared Basin BET over the 39 shell blocks; registry path mirrors {@link TileEntityBasin#getTileEntityName}. */
	public static final RegistryObject<BlockEntityType<TileEntityBasin>> BASIN_BE =
			BLOCK_ENTITY_TYPES.register("basin", () -> BlockEntityType.Builder.of(
					TileEntityBasin::new, basinBlockArray()).build(null));

	/** The Basin block list of the family in registration order. */
	public static Block[] basinBlockArray() {
		Block[] rBlocks = new Block[BASIN_ROWS.size()];
		for (int i = 0; i < BASIN_ROWS.size(); i++) rBlocks[i] = BASIN_BLOCKS_BY_PATH.get(BASIN_ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static BasinBlock basinBlockByPath(String aPath) {
		RegistryObject<BasinBlock> tHandle = BASIN_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The basin block — the mold-family carrier over the shared Basin BET (the CrucibleBlock
	 * shape: the row rides the instance, the top-face click is the NO_GUI interface).
	 * Selection is the full footprint; collision is the four 1px walls (you can stand IN the
	 * basin — the upstream full-height wall passes, MultiTileEntityBasin.java:100-104).
	 */
	public static final class BasinBlock extends GTEntityBlock {

		private final SmelteryRow mRow;

		public BasinBlock(SmelteryRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}

		/** The registration row. */
		public SmelteryRow row() {
			return mRow;
		}

		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch) — the CrucibleBlock simpleCodec representative-value form verbatim.
		@Override
		protected com.mojang.serialization.MapCodec<? extends BasinBlock> codec() {
			return simpleCodec(aProperties -> new BasinBlock(BASIN_ROWS.get(0), aProperties));
		}
		*///?}

		/** The composed display name (the BASIN_DISPLAY_KEY template over the material word). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable(BASIN_DISPLAY_KEY,
					Component.translatable("gt6.row.mat." + mRow.display()));
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Crucibles.BASIN_BE.get();
		}

		@Override
		public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
			return net.minecraft.world.level.block.RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		/** The four 1px full-height walls (MultiTileEntityBasin.java:100-104 render passes 0-3). */
		@Override
		public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
			return net.minecraft.world.phys.shapes.Shapes.or(
					net.minecraft.world.level.block.Block.box(0, 0, 0, 1, 16, 16),
					net.minecraft.world.level.block.Block.box(15, 0, 0, 16, 16, 16),
					net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 16, 1),
					net.minecraft.world.level.block.Block.box(0, 0, 15, 16, 16, 16));
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
			// the SIDES_TOP gate (MultiTileEntityBasin.java:63-66 — the pick-up/pour top click)
			if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntityBasin tBasin) {
				if (!aLevel.isClientSide) tBasin.useTop(aPlayer, aHand);
				return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
			}
			return net.minecraft.world.InteractionResult.PASS;
		}
	}

	// ------------------------------------------------------------------------------------
	// the CROSSING family (task material-mc-c-crucible-rows — Loader:471-513, design 1072,
	// the upstream "Molds" group; the pour bridge rides TileEntityCrossing)
	// ------------------------------------------------------------------------------------

	/** The composed crossing display template "{@code Crucible Crossing (%s)}" (the Loader name column). */
	public static final String CROSSING_DISPLAY_KEY = "gt6.row.crossing.display";

	/** The 39 crossing rows — the {@link #MATERIALS} projection (the basin form). */
	public static final List<SmelteryRow> CROSSING_ROWS = MATERIALS.stream()
			.map(tMat -> new SmelteryRow(tMat.pathOf("crossing"), tMat.material(), tMat.slug(), tMat.hardness()))
			.toList();

	/** The registered Crossing blocks by path. */
	public static final Map<String, RegistryObject<CrossingBlock>> CROSSING_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered Crossing items, same keys. */
	public static final Map<String, RegistryObject<Item>> CROSSING_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (SmelteryRow tRow : CROSSING_ROWS) {
			CROSSING_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new CrossingBlock(tRow, BlockBehaviour.Properties.of()
							.strength(tRow.hardness(), tRow.hardness() * 2) // the upstream NBT pair (the smeltery family form)
							.sound(SoundType.STONE)
							.noOcclusion()))); // the bridge is a non-full cross (upstream isSideSolid2 F / LIGHT_OPACITY_NONE)
			CROSSING_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Crucibles.CROSSING_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The shared Crossing BET over the 39 shell blocks; registry path mirrors {@link TileEntityCrossing#getTileEntityName}. */
	public static final RegistryObject<BlockEntityType<TileEntityCrossing>> CROSSING_BE =
			BLOCK_ENTITY_TYPES.register("crossing", () -> BlockEntityType.Builder.of(
					TileEntityCrossing::new, crossingBlockArray()).build(null));

	/** The Crossing block list of the family in registration order. */
	public static Block[] crossingBlockArray() {
		Block[] rBlocks = new Block[CROSSING_ROWS.size()];
		for (int i = 0; i < CROSSING_ROWS.size(); i++) rBlocks[i] = CROSSING_BLOCKS_BY_PATH.get(CROSSING_ROWS.get(i).path()).get();
		return rBlocks;
	}

	/** The lookup for the datagen/command walkers — null for an unknown path. */
	@Nullable
	public static CrossingBlock crossingBlockByPath(String aPath) {
		RegistryObject<CrossingBlock> tHandle = CROSSING_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The crossing block — the pour-bridge carrier (the upstream selection/collision box
	 * verbatim, MultiTileEntityCrossing.java:127-129: the full footprint, y 1..6px). The
	 * redstone half: signal on the TOP/BOTTOM faces lights the BE's {@code mRedstone} and
	 * the block answers it as a horizontal weak-power source (upstream
	 * isProvidingWeakPower2, SIDES_HORIZONTAL, value 1).
	 */
	public static final class CrossingBlock extends GTEntityBlock {

		private final SmelteryRow mRow;

		public CrossingBlock(SmelteryRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}

		/** The registration row. */
		public SmelteryRow row() {
			return mRow;
		}

		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract — the CrucibleBlock simpleCodec
		// representative-value form verbatim (parse-time default, no live config).
		@Override
		protected com.mojang.serialization.MapCodec<? extends CrossingBlock> codec() {
			return simpleCodec(aProperties -> new CrossingBlock(CROSSING_ROWS.get(0), aProperties));
		}
		*///?}

		/** The composed display name (the CROSSING_DISPLAY_KEY template over the material word). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return Component.translatable(CROSSING_DISPLAY_KEY,
					Component.translatable("gt6.row.mat." + mRow.display()));
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Crucibles.CROSSING_BE.get();
		}

		@Override
		public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
			return net.minecraft.world.level.block.RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		/** The upstream getSelectedBoundingBoxFromPool box verbatim (MultiTileEntityCrossing.java:128). */
		@Override
		public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
			return net.minecraft.world.level.block.Block.box(0, 1, 0, 16, 6, 16);
		}

		@Override
		public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
			return net.minecraft.world.level.block.Block.box(0, 1, 0, 16, 6, 16);
		}

		@Override
		public boolean isSignalSource(BlockState aState) {
			return true; // upstream isProvidingWeakPower2 gate exists
		}

		@Override
		public int getSignal(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.core.Direction aDirection) {
			// upstream :131 — SIDES_HORIZONTAL only, value 1 (the vanilla query direction is
			// the side the RECEIVER sees the machine from, the GTOvenBlock.getSignal note)
			if (!aDirection.getAxis().isHorizontal()) return 0;
			if (aLevel.getBlockEntity(aPos) instanceof TileEntityCrossing tCrossing && tCrossing.mRedstone) return 1;
			return 0;
		}
	}

	// ------------------------------------------------------------------------------------
	// the LARGE-crucible family (task crucible-multiblock SPEC ⑤)
	// ------------------------------------------------------------------------------------

	/**
	 * One LARGE registration row — the Loader aRegistry.add projection (path + shell material
	 * + hardness + the NBT_DESIGN wall). The material rides a {@link java.util.function.Supplier}
	 * (the {@link SmelteryRow} form, task w3-distill-crucible ③): the static rows initialize
	 * at class-load time which on the 21.1 leg runs before MT.init() — a direct MT.Steel
	 * reference captured null and the shell (the physics ceiling input) read as null.
	 */
	public record CrucibleRow(String path, java.util.function.Supplier<OreDictMaterial> materialSupplier, String display, float hardness,
			String wallPath, boolean acidProof, int metaId) {

		/** The shell material (the supplier dereference — the physics ceiling input). */
		public OreDictMaterial material() {
			return materialSupplier.get();
		}
	}

	/**
	 * The single rung (SPEC ⑤): upstream :1270 verbatim — Steel shell, the 18009 Steel
	 * Wall design, NOT acidproof (the StainlessSteel row carries NBT_ACIDPROOF T, the
	 * rung would flip the wall row with the defer-pool ladder).
	 */
	public static final CrucibleRow STEEL_ROW =
			new CrucibleRow("crucible_steel", () -> MT.Steel, "Large Steel Crucible", 6.0F, "crucible_steel_wall", false, 17309);

	/** The wall row (upstream :1145 "Steel Wall" — part id 18009, the single-rung wall ladder). */
	public static final CrucibleRow STEEL_WALL_ROW =
			new CrucibleRow("crucible_steel_wall", () -> MT.Steel, "Steel Wall", 6.0F, "crucible_steel_wall", false, 18009);

	// ------------------------------------------------------------------------------------
	// task w3-distill-crucible ③ — the 8-material ladder completion (the P26 defer pool
	// row this card harvests). The seven rows re-read VERBATIM from Loader_MultiTileEntities
	// .java:1271-1277 (the upstream line order below): the NBT_DESIGN column names the metal
	// wall (18002/18007/18006/18003/18004/18012/18005) and NBT_ACIDPROOF is T for the
	// StainlessSteel/Tungsten/Adamantium rungs ONLY (the task-card ACIDPROOF column). The
	// heat ceiling is the P26 CruciblePhysics parameter face (temperatureMax = the SHELL
	// material melting point × 1.10) — zero new physics, the shell material is what the
	// ladder actually swaps (TileEntityCrucibleRow.getShellMaterial below).
	// The dedicated crucible-wall blocks (not card ①'s shared machine walls) keep the
	// ITileEntityCrucible mold-pour relay the port cut from the shared part BE — the
	// GTCrucibleWallBlock class doc carries the ruling.
	// ------------------------------------------------------------------------------------

	/** :1271 — the Stainless Steel rung (ACIDPROOF T). */
	public static final CrucibleRow STAINLESS_STEEL_ROW =
			new CrucibleRow("crucible_stainless_steel", () -> MT.StainlessSteel, "Large Stainless Steel Crucible", 6.0F, "crucible_stainless_steel_wall", true, 17302);
	/** :1272 — the Invar rung. */
	public static final CrucibleRow INVAR_ROW =
			new CrucibleRow("crucible_invar", () -> MT.Invar, "Large Invar Crucible", 6.0F, "crucible_invar_wall", false, 17307);
	/** :1273 — the Titanium rung (hardness 9.0). */
	public static final CrucibleRow TITANIUM_ROW =
			new CrucibleRow("crucible_titanium", () -> MT.Ti, "Large Titanium Crucible", 9.0F, "crucible_titanium_wall", false, 17306);
	/** :1274 — the Tungstensteel rung (hardness 12.5). */
	public static final CrucibleRow TUNGSTENSTEEL_ROW =
			new CrucibleRow("crucible_tungstensteel", () -> MT.TungstenSteel, "Large Tungstensteel Crucible", 12.5F, "crucible_tungstensteel_wall", false, 17303);
	/** :1275 — the Tungsten rung (hardness 10.0, ACIDPROOF T). */
	public static final CrucibleRow TUNGSTEN_ROW =
			new CrucibleRow("crucible_tungsten", () -> MT.W, "Large Tungsten Crucible", 10.0F, "crucible_tungsten_wall", true, 17304);
	/** :1276 — the Tantalum Hafnium Carbide rung (hardness 12.5). */
	public static final CrucibleRow TANTALUM_HAFNIUM_CARBIDE_ROW =
			new CrucibleRow("crucible_tantalum_hafnium_carbide", () -> MT.Ta4HfC5, "Large Tantalum Hafnium Carbide Crucible", 12.5F, "crucible_tantalum_hafnium_carbide_wall", false, 17312);
	/** :1277 — the Adamantium rung (hardness 100.0, ACIDPROOF T). */
	public static final CrucibleRow ADAMANTIUM_ROW =
			new CrucibleRow("crucible_adamantium", () -> MT.Ad, "Large Adamantium Crucible", 100.0F, "crucible_adamantium_wall", true, 17305);

	/**
	 * The controller rows — the FULL 8-material ladder (upstream :1270-1277, the line order
	 * kept; the datagen/lang/walkers iterate).
	 */
	public static final List<CrucibleRow> CRUCIBLE_ROWS = List.of(
			STEEL_ROW,
			STAINLESS_STEEL_ROW,
			INVAR_ROW,
			TITANIUM_ROW,
			TUNGSTENSTEEL_ROW,
			TUNGSTEN_ROW,
			TANTALUM_HAFNIUM_CARBIDE_ROW,
			ADAMANTIUM_ROW);

	/** The registered controller blocks by path. */
	public static final Map<String, RegistryObject<GTCrucibleControllerBlock>> CRUCIBLE_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered controller items, same keys. */
	public static final Map<String, RegistryObject<Item>> CRUCIBLE_ITEMS_BY_PATH = new LinkedHashMap<>();

	/** The wall block (the single-rung wall ladder rides one handle; the ladder grows into a map). */
	public static final RegistryObject<GTCrucibleWallBlock> CRUCIBLE_STEEL_WALL =
			BLOCKS.register(STEEL_WALL_ROW.path(), () -> new GTCrucibleWallBlock(partProperties(STEEL_WALL_ROW.hardness()),
					STEEL_WALL_ROW.materialSupplier));

	/** The wall item. */
	public static final RegistryObject<Item> CRUCIBLE_STEEL_WALL_ITEM =
			ITEMS.register(STEEL_WALL_ROW.path(), () -> new GTComposedNameItem(CRUCIBLE_STEEL_WALL.get(), new Item.Properties()));

	/**
	 * The seven ladder wall blocks by path (task w3-distill-crucible ③ — the dedicated
	 * {@link GTCrucibleWallBlock} per material, the mold-pour relay). The display composes
	 * "{@code <mat> Wall}" over the EXISTING gt6.row.mat words (the card ① metal-wall
	 * template — zero new unit keys, all seven slugs ride the dense-wall walk).
	 */
	public static final Map<String, RegistryObject<GTCrucibleWallBlock>> CRUCIBLE_WALL_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The ladder wall items, same keys. */
	public static final Map<String, RegistryObject<Item>> CRUCIBLE_WALL_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		// the seven wall registrations (the forward-reference lambda form — the BET builders
		// below resolve these handles at REGISTER time)
		for (CrucibleRow tRow : new CrucibleRow[] {STAINLESS_STEEL_ROW, INVAR_ROW, TITANIUM_ROW,
				TUNGSTENSTEEL_ROW, TUNGSTEN_ROW, TANTALUM_HAFNIUM_CARBIDE_ROW, ADAMANTIUM_ROW}) {
			String tWallPath = tRow.wallPath();
			String tSlug = tWallPath.substring("crucible_".length(), tWallPath.length() - "_wall".length());
			CRUCIBLE_WALL_BLOCKS_BY_PATH.put(tWallPath, BLOCKS.register(tWallPath, () -> new GTCrucibleWallBlock(partProperties(tRow.hardness()),
					gregtech6.registry.GTMultiBlocks.PartRow.METAL_WALL_DISPLAY_KEY, "gt6.row.mat." + tSlug,
					tRow.materialSupplier)));
			CRUCIBLE_WALL_ITEMS_BY_PATH.put(tWallPath, ITEMS.register(tWallPath, () -> new GTComposedNameItem(
					GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.get(tWallPath).get(), new Item.Properties())));
		}
	}

	static {
		for (CrucibleRow tRow : CRUCIBLE_ROWS) {
			CRUCIBLE_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTCrucibleControllerBlock(tRow, partProperties(tRow.hardness()))));
			CRUCIBLE_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The part properties (hardness == resistance on every row; the METAL sound, the machine-block convention). */
	public static BlockBehaviour.Properties partProperties(float aHardness) {
		return BlockBehaviour.Properties.of().strength(aHardness, aHardness).sound(SoundType.METAL);
	}

	/**
	 * The wall part BET: the relaying {@link CrucibleWallBlockEntity} over ALL EIGHT wall
	 * blocks (the ladder form of the HEAT_TRANSMITTER_BE degenerate shape — the SHARED part
	 * BET cannot mount it, the valid list lives in GTMultiBlocks.java which this card does
	 * not touch).
	 */
	public static final RegistryObject<BlockEntityType<CrucibleWallBlockEntity>> CRUCIBLE_WALL_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_crucible_wall", () -> BlockEntityType.Builder.of(
					CrucibleWallBlockEntity::new, crucibleWallBlockArray()).build(null));

	/** The wall-block array for the wall BET: the Steel wall + the seven ladder walls. */
	private static Block[] crucibleWallBlockArray() {
		Block[] rBlocks = new Block[1 + CRUCIBLE_WALL_BLOCKS_BY_PATH.size()];
		rBlocks[0] = CRUCIBLE_STEEL_WALL.get();
		int i = 1;
		for (RegistryObject<GTCrucibleWallBlock> tHandle : CRUCIBLE_WALL_BLOCKS_BY_PATH.values()) rBlocks[i++] = tHandle.get();
		return rBlocks;
	}

	/**
	 * The crucible-wall paint-tint walker (task issue8-residual, the
	 * {@code GT6Kitchen.paintableBlockArray} census convention): the eight wall blocks whose
	 * datagen models carry tintindex 0 on every face. Feeding BOTH consumption halves — the
	 * baked world tint ({@code GTMachineTintModel}, the p32 route) and the inventory
	 * {@code ItemColor} — the row materials render through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} part gate (the dedicated wall blocks are
	 * {@link GTCrucibleWallBlock} part carriers, not the shared GTMultiBlocks walls). The
	 * CONTROLLERS stay out — their placeholder cubes are the declared render defer. Client
	 * call time only.
	 */
	public static Block[] paintableWallBlockArray() {
		return crucibleWallBlockArray();
	}

	/**
	 * The controller BET: one crucible class over the registered controller blocks (the
	 * LARGE_BOILER_BE one-BET-many-blocks form; the row rides the block carrier). The
	 * factory is the ROW-AWARE {@link TileEntityCrucibleRow} subclass — the shell material
	 * (the physics ceiling input) is what the 8-material ladder actually swaps.
	 */
	public static final RegistryObject<BlockEntityType<TileEntityCrucibleRow>> MULTIBLOCK_CRUCIBLE_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_crucible", () -> BlockEntityType.Builder.of(
					TileEntityCrucibleRow::new, crucibleBlockArray()).build(null));

	/**
	 * The row-aware crucible controller (task w3-distill-crucible ③): the P26 base
	 * hardcodes the single-rung Steel shell ({@code getShellMaterial} → MT.Steel); the
	 * ladder swaps the SHELL MATERIAL — the CruciblePhysics ceiling input
	 * ({@code temperatureMax = material melting point × 1.10}, zero new physics) — so this
	 * subclass reads the shell off the carried row and falls back to the base form on the
	 * offline fixtures.
	 */
	public static final class TileEntityCrucibleRow extends TileEntityCrucible {

		public TileEntityCrucibleRow(BlockPos aPos, BlockState aState) {
			super(aPos, aState);
		}

		public TileEntityCrucibleRow(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		@Nullable
		protected OreDictMaterial getShellMaterial() {
			if (getLevel() != null && getBlockState().getBlock() instanceof GTCrucibleControllerBlock tBlock && tBlock.row().material() != null) {
				return tBlock.row().material(); // the carried row's shell (the Loader NBT_MATERIAL column)
			}
			return super.getShellMaterial(); // the offline fixture fallback (Steel)
		}
	}

	/** The controller-block array for the BET (the same varargs shape). */
	private static Block[] crucibleBlockArray() {
		return CRUCIBLE_BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	/** The lookup for the command/datagen walkers — null for an unknown variant path. */
	@Nullable
	public static Block crucibleBlockByPath(String aPath) {
		RegistryObject<GTCrucibleControllerBlock> tHandle = CRUCIBLE_BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	/** The wall block of a controller row (the GTLargeBoilerBlock.wallBlock resolution, the production {@code getWallBlock} binding). */
	public static Block wallBlockOf(CrucibleRow aRow) {
		RegistryObject<GTCrucibleWallBlock> tHandle = CRUCIBLE_WALL_BLOCKS_BY_PATH.get(aRow.wallPath());
		if (tHandle != null) return tHandle.get();
		if ("crucible_steel_wall".equals(aRow.wallPath())) return CRUCIBLE_STEEL_WALL.get(); // the single-rung legacy path
		return CRUCIBLE_STEEL_WALL.get(); // the declared fallback (the boiler wallBlock erratum form)
	}

	// ------------------------------------------------------------------------------------
	// the block-state sanity walk (the datagen/census helper, the GTMultiBlocks block-array shape)
	// ------------------------------------------------------------------------------------

	/** Every block of this family in registration order (the BET census + the loot walkers). */
	public static List<Block> familyBlocks() {
		List<Block> rBlocks = new ArrayList<>();
		rBlocks.add(CRUCIBLE_STEEL_WALL.get());
		for (RegistryObject<GTCrucibleWallBlock> tHandle : CRUCIBLE_WALL_BLOCKS_BY_PATH.values()) rBlocks.add(tHandle.get());
		for (RegistryObject<GTCrucibleControllerBlock> tHandle : CRUCIBLE_BLOCKS_BY_PATH.values()) rBlocks.add(tHandle.get());
		return rBlocks;
	}

	/**
	 * The displayed-material resolution (task crucible-large-ber): the upstream
	 * {@code OreDictMaterial.MATERIAL_ARRAY[mDisplayedFluid]} face (MultiTileEntityCrucible
	 * .java:620, the TileEntitySmeltery.displayedMaterial and the content-tint consumers
	 * share it), null for an out-of-range id. Both crucible BEs sync the lightest molten
	 * material's {@code mID} (upstream :350/:299).
	 */
	@Nullable
	public static OreDictMaterial materialById(int aId) {
		OreDictMaterial[] tArray = gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY;
		return aId >= 0 && aId < tArray.length ? tArray[aId] : null;
	}

	private GT6Crucibles() {}

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
	 * The tab walk (task tabfix-a-multiblock, SPLIT by task material-mc-c-crucible-rows
	 * along the small-crucible-boiler-tab-rehome ruling): the single-block families (the 39
	 * Smeltery rungs + the 39 Basins + the 39 Crossings — the upstream "Smelting Crucibles"
	 * and "Molds" groups, this port pooling all three into the gt6:machines tab) ride the
	 * MACHINES arm; the LARGE multiblock family (the Steel wall, the 7 ladder walls, the 8
	 * controllers) stays on the multiblocks arm. The hidden rows (the NBT_HIDDEN column:
	 * the granite/nether-brick stones and the four mod-stones) stay OUT of the creative
	 * walk — registered, craftable where the resolvable gate answers, never tab members
	 * (the upstream hidden semantics). JEI derives its item list from the tab display items.
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (CrucibleMaterial tMat : MATERIALS) {
				if (tMat.hidden()) continue;
				aEvent.accept(new ItemStack(ITEMS_BY_PATH.get(tMat.pathOf("smeltery")).get()));
				aEvent.accept(new ItemStack(BASIN_ITEMS_BY_PATH.get(tMat.pathOf("basin")).get()));
				aEvent.accept(new ItemStack(CROSSING_ITEMS_BY_PATH.get(tMat.pathOf("crossing")).get()));
			}
		} else if (aEvent.getTabKey().location().equals(GTMultiBlocks.MULTIBLOCKS_TAB.getId())) {
			aEvent.accept(new ItemStack(CRUCIBLE_STEEL_WALL_ITEM.get()));
			for (RegistryObject<Item> tItem : CRUCIBLE_WALL_ITEMS_BY_PATH.values()) aEvent.accept(new ItemStack(tItem.get()));
			for (RegistryObject<Item> tItem : CRUCIBLE_ITEMS_BY_PATH.values()) aEvent.accept(new ItemStack(tItem.get()));
		}
	}
}
