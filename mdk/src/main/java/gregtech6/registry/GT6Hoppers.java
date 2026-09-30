package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.TD;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.tileentity.connectors.TileEntityBase09Connector;
import gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity;
import gregtech6.tileentity.inventories.GT6HopperBlockEntity;
import gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity;

/**
 * The storage-hopper family registration home (task storage-hopper-family, the
 * GT6Boilers self-contained-DR row form): 120 blocks/items over TWO shared BETs — the two BE
 * classes {@link GT6HopperBlockEntity} / {@link GT6QueueHopperBlockEntity}, the row config
 * (slot count, material) riding the block carrier.
 *
 * <p><b>The rows</b> (task hopper-matrix — the full metalset() hopper pair :145-146 over
 * the 60-material loop :186-245, VERBATIM order and columns: aID, MT local, aHardness
 * (== aResistance on every line), aHopperSize; first batch = the Bronze/Steel anchor
 * rows of the wave-4 arch ruling — research 'Iron(5)' was a census slip, the 60-material
 * table has no Iron row, the 5-slot anchor is Steel :202). Every loader line expands to
 * TWO rows through the metalset pair — the plain hopper (:145, id 8000+aID,
 * NBT_INV_SIZE = max(1, aHopperSize)) then the queue hopper (:146, id 8200+aID,
 * NBT_INV_SIZE = max(2, aHopperSize)); 60 × 2 = 120. The display word is the MT local —
 * the setLocal overrides ride ({@code Tungsten Alloy} MT.java:1723, {@code Duranium
 * Alloy} :1840, {@code Osmium} :633, {@code Elementium} :1822, {@code Awakened
 * Draconium} :1862, {@code Galvanized Steel} :1731, {@code Tritanium Alloy} :1841) and
 * the :235 line is {@code ANY.W} → the Tungsten face (ANY.java:133 setLocal).
 *
 * <p>The upstream registration columns: tool quality 0, weight 16, the aMachine block family,
 * the recipes "PwP"/"XCX"/" Xh" (hopper) and "PCP"/"XCX"/"wXh" (queue) over plate +
 * plateCurved + OD.craftingChest with the wrench/hammer tool letters (:145-146).
 *
 * <p>Creative tab: the upstream "Hoppers" MTE-registry category (tab 8010, the
 * metalset() hopper pair Loader_MultiTileEntities.java:145-146) pools into the
 * MACHINES_TAB join (task tabfix-b-energy, {@link #onBuildTabContents}; the
 * GTBarrels:257 pooling precedent — supersedes the old later-append note). The placement
 * face is the INVERSE clicked face (09FacingSingle.onPlaced :73 with
 * useInversePlacementRotation = T :257/:239 → OPOS[clickedFace], all six faces valid) —
 * the blockstate FACING property carries it.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Hoppers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One loader line of the metalset material loop — slug + display word + the per-line
	 * columns (the ItemPipeMaterial per-line-columns shape; the aHardness/aResistance pair
	 * collapses to one column because they are EQUAL on all 60 lines, :186-245).
	 *
	 * @param slug     the registry-path tail ({@code hopper_<slug>} / {@code queue_hopper_<slug>})
	 *                 and the shared {@code gt6.row.mat.<slug>} unit key (the display-snake
	 *                 convention of the boiler/burning-box mat words)
	 * @param display  the MT local (the {@code getLocal()} column of the :186-245 names —
	 *                 the upstream hopper name is {@code <local> Hopper})
	 * @param hardness the aHardness column (== aResistance)
	 * @param metaId   the aID column (id 8000+aID / 8200+aID)
	 * @param slots    the aHopperSize column (NBT_INV_SIZE = max(1,n) / max(2,n))
	 */
	public record HopperMaterial(String slug, String display, float hardness, int metaId, int slots) {
		/**
		 * The loader material face (the recipe 'X' column = OP.plateCurved.dat(aMat),
		 * Loader:145-146) — the :186-245 MT argument verbatim per line. Loud drift on an
		 * unknown slug — the table only ever grows with a loader line in hand.
		 */
		public gregapi.oredict.OreDictMaterial mt() {
			return switch (slug) {
				case "lead" -> gregapi.data.MT.Pb;
				case "bismuth" -> gregapi.data.MT.Bi;
				case "antimony" -> gregapi.data.MT.Sb;
				case "nickel" -> gregapi.data.MT.Ni;
				case "constantan" -> gregapi.data.MT.Constantan;
				case "bronze" -> gregapi.data.MT.Bronze;
				case "arsenic_copper" -> gregapi.data.MT.ArsenicCopper;
				case "aluminium" -> gregapi.data.MT.Al;
				case "brass" -> gregapi.data.MT.Brass;
				case "tin_alloy" -> gregapi.data.MT.TinAlloy;
				case "cobalt" -> gregapi.data.MT.Co;
				case "ardite" -> gregapi.data.MT.Ardite;
				case "arsenic_bronze" -> gregapi.data.MT.ArsenicBronze;
				case "bismuth_bronze" -> gregapi.data.MT.BismuthBronze;
				case "germanium" -> gregapi.data.MT.Ge;
				case "invar" -> gregapi.data.MT.Invar;
				case "steel" -> gregapi.data.MT.Steel;
				case "hsla_steel" -> gregapi.data.MT.HSLA;
				case "gold" -> gregapi.data.MT.Au;
				case "silver" -> gregapi.data.MT.Ag;
				case "manganese" -> gregapi.data.MT.Mn;
				case "manyullyn" -> gregapi.data.MT.Manyullyn;
				case "lumium" -> gregapi.data.MT.Lumium;
				case "knightmetal" -> gregapi.data.MT.Knightmetal;
				case "galvanized_steel" -> gregapi.data.MT.SteelGalvanized;
				case "meteorite" -> gregapi.data.MT.Meteorite;
				case "meteoric_steel" -> gregapi.data.MT.MeteoricSteel;
				case "gilded_iron" -> gregapi.data.MT.GildedIron;
				case "molybdenum" -> gregapi.data.MT.Mo;
				case "syrmorite" -> gregapi.data.MT.Syrmorite;
				case "electrum" -> gregapi.data.MT.Electrum;
				case "stainless_steel" -> gregapi.data.MT.StainlessSteel;
				case "thaumium" -> gregapi.data.MT.Thaumium;
				case "manasteel" -> gregapi.data.MT.Manasteel;
				case "efrine" -> gregapi.data.MT.Efrine;
				case "tungsten_alloy" -> gregapi.data.MT.TungstenAlloy;
				case "titanium" -> gregapi.data.MT.Ti;
				case "netherite" -> gregapi.data.MT.Netherite;
				case "chromium" -> gregapi.data.MT.Cr;
				case "platinum" -> gregapi.data.MT.Pt;
				case "octine" -> gregapi.data.MT.Octine;
				case "desh" -> gregapi.data.MT.Desh;
				case "terrasteel" -> gregapi.data.MT.Terrasteel;
				case "tungstensteel" -> gregapi.data.MT.TungstenSteel;
				case "tungsten_carbide" -> gregapi.data.MT.TungstenCarbide;
				case "duranium_alloy" -> gregapi.data.MT.DuraniumAlloy;
				case "draconium" -> gregapi.data.MT.Draconium;
				case "ultimet" -> gregapi.data.MT.Ultimet;
				case "workers_alloy" -> gregapi.data.MT.DeshAlloy;
				case "tungsten" -> gregapi.data.MT.W; // the :235 ANY.W line — the Tungsten face (ANY.java:133)
				case "palladium" -> gregapi.data.MT.Pd;
				case "iridium" -> gregapi.data.MT.Ir;
				case "osmium" -> gregapi.data.MT.Os;
				case "void_metal" -> gregapi.data.MT.VoidMetal;
				case "elementium" -> gregapi.data.MT.ElvenElementium;
				case "tritanium_alloy" -> gregapi.data.MT.TritaniumAlloy;
				case "adamantium" -> gregapi.data.MT.Ad;
				case "bedrock_hsla_alloy" -> gregapi.data.MT.Bedrock_HSLA_Alloy;
				case "awakened_draconium" -> gregapi.data.MT.DraconiumAwakened;
				case "infinity" -> gregapi.data.MT.Infinity;
				default -> throw new IllegalStateException("no loader material for hopper slug " + slug);
			};
		}
	}

	/**
	 * The loader's 60 metalset lines, verbatim order and columns
	 * (Loader_MultiTileEntities.java:186-245 — aID, MT local, aHardness, aHopperSize).
	 * The zh words are the dump faces (tmp/gregtech.lang gt.multitileentity.8000-8059 /
	 * 8200-8259 verbatim heads); the en words are the MT locals — the setLocal overrides
	 * cited in the class javadoc, the Ultimet word rides the p27-lang-fix-batch2 ⑤ ruling
	 * (钴铬钨合金, not the dump's 哈氏合金 misattribution).
	 */
	public static final HopperMaterial MAT_LEAD = new HopperMaterial("lead", "Lead", 4.0F, 0, 1);
	public static final HopperMaterial MAT_BISMUTH = new HopperMaterial("bismuth", "Bismuth", 4.0F, 16, 2);
	public static final HopperMaterial MAT_ANTIMONY = new HopperMaterial("antimony", "Antimony", 4.0F, 47, 2);
	public static final HopperMaterial MAT_NICKEL = new HopperMaterial("nickel", "Nickel", 4.0F, 22, 3);
	public static final HopperMaterial MAT_CONSTANTAN = new HopperMaterial("constantan", "Constantan", 4.0F, 37, 3);
	public static final HopperMaterial MAT_BRONZE = new HopperMaterial("bronze", "Bronze", 7.0F, 9, 3);
	public static final HopperMaterial MAT_ARSENIC_COPPER = new HopperMaterial("arsenic_copper", "Arsenic Copper", 7.5F, 57, 4);
	public static final HopperMaterial MAT_ALUMINIUM = new HopperMaterial("aluminium", "Aluminium", 2.0F, 1, 4);
	public static final HopperMaterial MAT_BRASS = new HopperMaterial("brass", "Brass", 2.5F, 8, 4);
	public static final HopperMaterial MAT_TIN_ALLOY = new HopperMaterial("tin_alloy", "Tin Alloy", 3.0F, 5, 4);
	public static final HopperMaterial MAT_COBALT = new HopperMaterial("cobalt", "Cobalt", 4.0F, 21, 4);
	public static final HopperMaterial MAT_ARDITE = new HopperMaterial("ardite", "Ardite", 2.0F, 38, 4);
	public static final HopperMaterial MAT_ARSENIC_BRONZE = new HopperMaterial("arsenic_bronze", "Arsenic Bronze", 8.0F, 58, 5);
	public static final HopperMaterial MAT_BISMUTH_BRONZE = new HopperMaterial("bismuth_bronze", "Bismuth Bronze", 8.0F, 56, 5);
	public static final HopperMaterial MAT_GERMANIUM = new HopperMaterial("germanium", "Germanium", 4.0F, 23, 5);
	public static final HopperMaterial MAT_INVAR = new HopperMaterial("invar", "Invar", 4.0F, 6, 5);
	public static final HopperMaterial MAT_STEEL = new HopperMaterial("steel", "Steel", 6.0F, 10, 5);
	public static final HopperMaterial MAT_HSLA_STEEL = new HopperMaterial("hsla_steel", "HSLA-Steel", 6.0F, 18, 6);
	public static final HopperMaterial MAT_GOLD = new HopperMaterial("gold", "Gold", 3.0F, 2, 6);
	public static final HopperMaterial MAT_SILVER = new HopperMaterial("silver", "Silver", 3.0F, 3, 6);
	public static final HopperMaterial MAT_MANGANESE = new HopperMaterial("manganese", "Manganese", 6.0F, 46, 6);
	public static final HopperMaterial MAT_MANYULLYN = new HopperMaterial("manyullyn", "Manyullyn", 4.0F, 39, 6);
	public static final HopperMaterial MAT_LUMIUM = new HopperMaterial("lumium", "Lumium", 2.0F, 54, 6);
	public static final HopperMaterial MAT_KNIGHTMETAL = new HopperMaterial("knightmetal", "Knightmetal", 7.0F, 25, 7);
	public static final HopperMaterial MAT_GALVANIZED_STEEL = new HopperMaterial("galvanized_steel", "Galvanized Steel", 6.0F, 19, 7);
	public static final HopperMaterial MAT_METEORITE = new HopperMaterial("meteorite", "Meteorite", 7.0F, 43, 7);
	public static final HopperMaterial MAT_METEORIC_STEEL = new HopperMaterial("meteoric_steel", "Meteoric Steel", 8.0F, 24, 8);
	public static final HopperMaterial MAT_GILDED_IRON = new HopperMaterial("gilded_iron", "Gilded Iron", 6.0F, 20, 8);
	public static final HopperMaterial MAT_MOLYBDENUM = new HopperMaterial("molybdenum", "Molybdenum", 6.0F, 49, 8);
	public static final HopperMaterial MAT_SYRMORITE = new HopperMaterial("syrmorite", "Syrmorite", 4.0F, 44, 9);
	public static final HopperMaterial MAT_ELECTRUM = new HopperMaterial("electrum", "Electrum", 3.0F, 7, 9);
	public static final HopperMaterial MAT_STAINLESS_STEEL = new HopperMaterial("stainless_steel", "Stainless Steel", 5.0F, 11, 9);
	public static final HopperMaterial MAT_THAUMIUM = new HopperMaterial("thaumium", "Thaumium", 9.0F, 27, 9);
	public static final HopperMaterial MAT_MANASTEEL = new HopperMaterial("manasteel", "Manasteel", 9.0F, 40, 9);
	public static final HopperMaterial MAT_EFRINE = new HopperMaterial("efrine", "Efrine", 8.0F, 53, 9);
	public static final HopperMaterial MAT_TUNGSTEN_ALLOY = new HopperMaterial("tungsten_alloy", "Tungsten Alloy", 8.0F, 52, 12);
	public static final HopperMaterial MAT_TITANIUM = new HopperMaterial("titanium", "Titanium", 9.0F, 12, 12);
	public static final HopperMaterial MAT_NETHERITE = new HopperMaterial("netherite", "Netherite", 10.0F, 51, 12);
	public static final HopperMaterial MAT_CHROMIUM = new HopperMaterial("chromium", "Chromium", 4.0F, 13, 14);
	public static final HopperMaterial MAT_PLATINUM = new HopperMaterial("platinum", "Platinum", 2.0F, 4, 18);
	public static final HopperMaterial MAT_OCTINE = new HopperMaterial("octine", "Octine", 8.0F, 45, 18);
	public static final HopperMaterial MAT_DESH = new HopperMaterial("desh", "Desh", 15.0F, 30, 18);
	public static final HopperMaterial MAT_TERRASTEEL = new HopperMaterial("terrasteel", "Terrasteel", 15.0F, 42, 18);
	public static final HopperMaterial MAT_TUNGSTENSTEEL = new HopperMaterial("tungstensteel", "Tungstensteel", 12.5F, 14, 27);
	public static final HopperMaterial MAT_TUNGSTEN_CARBIDE = new HopperMaterial("tungsten_carbide", "Tungsten Carbide", 12.5F, 17, 27);
	public static final HopperMaterial MAT_DURANIUM_ALLOY = new HopperMaterial("duranium_alloy", "Duranium Alloy", 20.0F, 31, 27);
	public static final HopperMaterial MAT_DRACONIUM = new HopperMaterial("draconium", "Draconium", 50.0F, 35, 27);
	public static final HopperMaterial MAT_ULTIMET = new HopperMaterial("ultimet", "Ultimet", 12.5F, 48, 27);
	public static final HopperMaterial MAT_WORKERS_ALLOY = new HopperMaterial("workers_alloy", "Workers Alloy", 15.0F, 55, 27);
	public static final HopperMaterial MAT_TUNGSTEN = new HopperMaterial("tungsten", "Tungsten", 10.0F, 26, 36);
	public static final HopperMaterial MAT_PALLADIUM = new HopperMaterial("palladium", "Palladium", 15.0F, 59, 36);
	public static final HopperMaterial MAT_IRIDIUM = new HopperMaterial("iridium", "Iridium", 15.0F, 15, 36);
	public static final HopperMaterial MAT_OSMIUM = new HopperMaterial("osmium", "Osmium", 9.0F, 29, 36);
	public static final HopperMaterial MAT_VOID_METAL = new HopperMaterial("void_metal", "Void Metal", 30.0F, 28, 36);
	public static final HopperMaterial MAT_ELEMENTIUM = new HopperMaterial("elementium", "Elementium", 30.0F, 41, 36);
	public static final HopperMaterial MAT_TRITANIUM_ALLOY = new HopperMaterial("tritanium_alloy", "Tritanium Alloy", 30.0F, 32, 36);
	public static final HopperMaterial MAT_ADAMANTIUM = new HopperMaterial("adamantium", "Adamantium", 100.0F, 33, 36);
	public static final HopperMaterial MAT_BEDROCK_HSLA_ALLOY = new HopperMaterial("bedrock_hsla_alloy", "Bedrock-HSLA-Alloy", 100.0F, 34, 36);
	public static final HopperMaterial MAT_AWAKENED_DRACONIUM = new HopperMaterial("awakened_draconium", "Awakened Draconium", 100.0F, 36, 36);
	public static final HopperMaterial MAT_INFINITY = new HopperMaterial("infinity", "Infinity", 100.0F, 50, 36);

	/** The 60 loader lines in registration order (:186-245 verbatim). */
	public static final List<HopperMaterial> MATERIALS = List.of(
			MAT_LEAD, MAT_BISMUTH, MAT_ANTIMONY, MAT_NICKEL, MAT_CONSTANTAN,
			MAT_BRONZE, MAT_ARSENIC_COPPER, MAT_ALUMINIUM, MAT_BRASS, MAT_TIN_ALLOY,
			MAT_COBALT, MAT_ARDITE, MAT_ARSENIC_BRONZE, MAT_BISMUTH_BRONZE, MAT_GERMANIUM,
			MAT_INVAR, MAT_STEEL, MAT_HSLA_STEEL, MAT_GOLD, MAT_SILVER,
			MAT_MANGANESE, MAT_MANYULLYN, MAT_LUMIUM, MAT_KNIGHTMETAL, MAT_GALVANIZED_STEEL,
			MAT_METEORITE, MAT_METEORIC_STEEL, MAT_GILDED_IRON, MAT_MOLYBDENUM, MAT_SYRMORITE,
			MAT_ELECTRUM, MAT_STAINLESS_STEEL, MAT_THAUMIUM, MAT_MANASTEEL, MAT_EFRINE,
			MAT_TUNGSTEN_ALLOY, MAT_TITANIUM, MAT_NETHERITE, MAT_CHROMIUM, MAT_PLATINUM,
			MAT_OCTINE, MAT_DESH, MAT_TERRASTEEL, MAT_TUNGSTENSTEEL, MAT_TUNGSTEN_CARBIDE,
			MAT_DURANIUM_ALLOY, MAT_DRACONIUM, MAT_ULTIMET, MAT_WORKERS_ALLOY, MAT_TUNGSTEN,
			MAT_PALLADIUM, MAT_IRIDIUM, MAT_OSMIUM, MAT_VOID_METAL, MAT_ELEMENTIUM,
			MAT_TRITANIUM_ALLOY, MAT_ADAMANTIUM, MAT_BEDROCK_HSLA_ALLOY, MAT_AWAKENED_DRACONIUM, MAT_INFINITY);

	/** The loader meta id bases of the hopper pair (:145 id 8000+aID, :146 id 8200+aID). */
	public static final int META_ID_BASE = 8000;
	public static final int META_ID_BASE_QUEUE = 8200;

	/** The composed display templates "{@code %s Hopper}" / "{@code %s Queue Hopper}" (the p20 compose ruling). */
	public static final String DISPLAY_KEY = "gt6.row.hopper.display";
	public static final String DISPLAY_QUEUE_KEY = "gt6.row.queue_hopper.display";

	/** The row's material small-unit key (the shared boiler/pipes mat key). */
	public static String matUnitKeyOf(HopperRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	/** The composed name of a row (the pure compose seam, the GT6Boilers.displayOf shape). */
	public static MutableComponent displayOf(HopperRow aRow) {
		return Component.translatable(aRow.queue() ? DISPLAY_QUEUE_KEY : DISPLAY_KEY,
				Component.translatable(matUnitKeyOf(aRow)));
	}

	/**
	 * One registration row — the block-carrier projection of one metalset hopper-pair
	 * half (Loader :145 plain / :146 queue).
	 *
	 * @param path     the gt6 registry path (the blockstate/model/lang key tail)
	 * @param metaId   the upstream MultiTileEntity id (8000+aID / 8200+aID)
	 * @param material the row material (slug/display/hardness/loader id/aHopperSize)
	 * @param slots    the aHopperSize column (rides the material line; the BE applies the
	 *                 max(1,n) plain / max(2,n) queue floor)
	 * @param queue    the :146 queue kind flag (NBT_INV_SIZE floor 2, the FIFO compaction)
	 */
	public record HopperRow(String path, int metaId, HopperMaterial material, int slots, boolean queue) {
		/** The block properties (hardness == resistance on every row; the METAL sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness(), material.hardness())
					.sound(SoundType.METAL);
		}
	}

	/**
	 * The 120 rows in registration order (the metalset loop walk — per loader line the
	 * plain hopper :145 then the queue hopper :146, the upstream pair order verbatim).
	 */
	public static final List<HopperRow> ROWS = buildRows();

	private static List<HopperRow> buildRows() {
		List<HopperRow> rRows = new ArrayList<>(MATERIALS.size() * 2);
		for (HopperMaterial tMat : MATERIALS) {
			rRows.add(new HopperRow("hopper_" + tMat.slug(), META_ID_BASE + tMat.metaId(), tMat, tMat.slots(), false));
			rRows.add(new HopperRow("queue_hopper_" + tMat.slug(), META_ID_BASE_QUEUE + tMat.metaId(), tMat, tMat.slots(), true));
		}
		return List.copyOf(rRows);
	}

	/** The registered blocks by path (the BET multi-mount arrays + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6HopperBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (HopperRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6HopperBlock(tRow, tRow.properties())));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Hoppers.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The hopper-kind blocks in registration order (the HOPPER_BE multi-mount array). */
	public static Block[] hopperBlockArray() {
		return kindArray(false);
	}

	/** The queue-kind blocks in registration order (the QUEUE_HOPPER_BE multi-mount array). */
	public static Block[] queueBlockArray() {
		return kindArray(true);
	}

	private static Block[] kindArray(boolean aQueue) {
		List<Block> rBlocks = new ArrayList<>();
		for (HopperRow tRow : ROWS) {
			if (tRow.queue() == aQueue) rBlocks.add(BLOCKS_BY_PATH.get(tRow.path()).get());
		}
		return rBlocks.toArray(new Block[0]);
	}

	/** The lookup for a data-driven place arm — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GT6HopperBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carrier — the FACING-all-six inverse-placement cube
	// -------------------------------------------------------------------------

	/**
	 * The hopper block — the facing cube carrier over the shared BET: the FACING is the
	 * OUTPUT face (the inverse clicked face, 09FacingSingle.onPlaced :73 with
	 * useInversePlacementRotation = T), the row (slots/kind) rides the instance. The use()
	 * arms are the upstream onToolClick2 tool face ported onto the in-repo dispatch
	 * (SCREWDRIVER action = the mode cycle, the wrench layer = the exact toggle) plus the
	 * always-consumed click of onBlockActivated3 :108-111 (the GUI arm is the deferred
	 * panel face — the card's menu-less ruling).
	 */
	public static final class GT6HopperBlock extends GTEntityBlock {

		/** Facing property (all six — the output face; SIDES_VALID upstream :255). */
		public static final DirectionProperty FACING = BlockStateProperties.FACING;

		private final HopperRow mRow;

		public GT6HopperBlock(HopperRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
		// precedent — a parse-time default carrying no live config; world save/load never
		// runs through this codec (the registry-id + property mapper does).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6HopperBlock> codec() {
			return simpleCodec(aProperties -> new GT6HopperBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the block-carrier config read). */
		public HopperRow row() {
			return mRow;
		}

		/** The composed hopper name (task i18n-compose-rows: the {@link GT6Hoppers#displayOf} carrier). */
		@Override
		public MutableComponent getName() {
			return displayOf(mRow);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the upstream inverse placement (:257/:239 — OPOS[aSide], all faces valid :255):
			// the hopper points INTO the clicked block, the vanilla HopperBlock.getStateForPlacement shape
			return defaultBlockState().setValue(FACING, aContext.getClickedFace().getOpposite());
		}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return mRow.queue() ? GTBlockEntities.QUEUE_HOPPER_BE.get() : GTBlockEntities.HOPPER_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// keep the BE's NBT fallback byte in step (the live truth is the state property)
			if (aLevel.getBlockEntity(aPos) instanceof GT6HopperBaseBlockEntity tHopper) {
				tHopper.setFacingNbtFallback((byte) aState.getValue(FACING).get3DDataValue());
			}
			// the item-connector auto-connect arm (upstream onPlaced :114-122 hopper / :104-116
			// queue — identical bodies, server-side only)
			if (!aLevel.isClientSide) {
				Direction tFacing = aState.getValue(FACING);
				autoConnectItemConnector(aLevel.getBlockEntity(aPos.relative(tFacing)), tFacing);
			}
		}

		/**
		 * The placement auto-connect (upstream onPlaced, both hopper kinds): a neighbour in the
		 * output direction that is a connector whose types toward the hopper intersect
		 * TD.Connectors.ALL_ITEM_TRANSPORT gets {@code connect(side, true)} fired on the side
		 * facing back ({@code mSideOfTileEntity} — the upstream one-liner). The upstream
		 * {@code SIDES_VALID} column is vacuous (0-5 always valid) and {@code allowInteraction}
		 * is vacuously true in this port (the item pipe family carries no foam/ownable layer).
		 * Upstream never fires it on the UP face ({@code SIDES_BOTTOM_HORIZONTAL[mFacing]}).
		 *
		 * @return whether the connect fired (the offline-test seam).
		 */
		public static boolean autoConnectItemConnector(@Nullable BlockEntity aNeighbor, Direction aOwnFacing) {
			if (aOwnFacing == Direction.UP) return false; // SIDES_BOTTOM_HORIZONTAL — bottom or horizontal only
			if (!(aNeighbor instanceof TileEntityBase09Connector tConnector)) return false;
			byte tSideOfConnector = (byte) aOwnFacing.getOpposite().get3DDataValue(); // mSideOfTileEntity
			if (!TileEntityBase09Connector.haveOneCommonElement(tConnector.getConnectorTypes(tSideOfConnector), TD.Connectors.ALL_ITEM_TRANSPORT)) return false;
			return tConnector.connect(tSideOfConnector, true);
		}

		/**
		 * The upstream onBlockActivated3 :108-111 port (return T — the click is consumed,
		 * no block places against a hopper) with the tool arms between: the SCREWDRIVER
		 * action drives the mode cycle (:128-135, sneak reverses), the wrench layer drives
		 * the exact toggle (:137-141, hopper kind only). The GUI open arm is the deferred
		 * panel face (the card's menu-less ruling).
		 */
		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (the GTItemPipeBlock fork verbatim)
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			ItemStack tStack = aPlayer.getItemInHand(aHand);
			BlockEntity tTile = aLevel.getBlockEntity(aPos);
			if (tTile instanceof GT6HopperBaseBlockEntity tHopper) {
				if (tStack.canPerformAction(GT6ToolActions.SCREWDRIVER)) {
					if (aLevel.isClientSide) return InteractionResult.CONSUME; // claim, the BE executes server-side
					tHopper.screwdriver(aPlayer.isShiftKeyDown(), aPlayer); // :128-135 — sneak reverses
					return InteractionResult.CONSUME;
				}
				if (tStack.canPerformAction(ToolActions.HOE_DIG) && tHopper.hasExactMode()) {
					if (aLevel.isClientSide) return InteractionResult.CONSUME;
					tHopper.monkeyWrench(aPlayer); // :137-141 — the exact/divisible toggle
					return InteractionResult.CONSUME;
				}
			}
			return InteractionResult.CONSUME; // upstream :110 return T — consumed with or without a tool
		}

		/**
		 * The vanilla HopperBlock.onRemove comparator tail — the content pops moved to the
		 * {@code GTEntityBlock} fallback (the same canDrop-everything contract, upstream
		 * :250/:232; the hopper BE's {@code getInventory()} is exactly what the deleted
		 * loop walked). The explosion face below still rides the air swap firing this.
		 */
		@Override
		public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
			if (!aOldState.is(aNewState.getBlock())) {
				aLevel.updateNeighbourForOutputSignal(aPos, this);
			}
			super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
		}

		/**
		 * The explosion face (IForgeBlock.java:716-720) — the GT6Boilers.onBlockExploded
		 * shape: the Forge default body swaps the air FIRST (removing the BE with it); the
		 * inventory pop rides the onRemove face above (the air swap fires it).
		 */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
		}
	}

	private GT6Hoppers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Boilers.onModConstruct doc). */
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
	}

	/**
	 * The tab walk (task tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
	 * joins the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). JEI 1.20.1 derives its item list from the tab display
	 * items, so registered-but-tab-less was invisible in both the creative menu and JEI.
	 * Pool-cut declaration: upstream gives the family its own "Hoppers" category (tab
	 * 8010, Loader_MultiTileEntities.java:145-146); this port pools the join into
	 * MACHINES_TAB (the GTBarrels:257 pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
