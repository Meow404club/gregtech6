package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.items.tools.GTMagnifyingGlassItem;
import gregtech6.items.tools.GTPincersItem;
import gregtech6.items.tools.GTSoftHammerItem;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.tileentity.inventories.GT6BookShelfBlockEntity;
import gregtech6.tileentity.inventories.GT6BottleCrateBlockEntity;
import gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity;
import gregtech6.tileentity.inventories.GT6LockerBlockEntity;
import gregtech6.tileentity.inventories.GT6MassStorageBlockEntity;
import gregtech6.tileentity.inventories.GT6MassStorageLogisticsBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity;
import gregtech6.tileentity.inventories.GT6StaticStorageBaseBlockEntity;

/**
 * The static storage batch registration home (task storage-static-batch, the
 * GT6Hoppers/GT6Boilers self-contained-DR row form): 148 blocks/items over SIX shared
 * BETs — the five BE classes of the {@code gregtech6.tileentity.inventories} batch, the
 * row config riding the block carrier.
 *
 * <p><b>The ITEM BARREL TRAP (the card's hard javadoc clause).</b> Nothing here is the
 * "Item Barrel": the GT6 Item Barrel is the ITEM MASS STORAGE (the
 * MultiTileEntityMassStorageBarrel family, storage-massstorage's card), and it shares
 * its NAME with this repo's already-ported FLUID barrels (GTBarrels, the
 * TileEntityBase08Barrel fluid carrier). The two are unrelated families — searches,
 * KG queries and reviews must not conflate them. The same deferral covers the MASS
 * STORAGE ladders themselves (Loader :141 {@code 6000+aID} MultiTileEntityMassStorageStandard /
 * :142 {@code 6200+aID} Logistics, 2x60 rows): task material-mc-b-storage-mass-shelf ruled
 * the material-row cards do not carry functionality, rows without a block entity are dead
 * blocks, and the upstream MassStorage base is a 759-line machine (pixel-grid take/insert,
 * six tool modes, overflow emission) — the whole family lands with the
 * storage-massstorage function card, NOT here.
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java metalset()/storages()): the metal
 * kinds anchor the Bronze (aID 9, :191 hardness 7.0) and Steel (aID 10, :202 hardness 6.0)
 * ladder rows — the same two-material ruling as the hopper family card — and the wooden
 * kinds port the 300-ladder (:177-180) onto the vanilla-planks subset, the declared
 * wave-4 deviation (PlankData has no 1.20.1 counterpart; the ladder folds to the subset
 * order). Task material-mc-b-storage-mass-shelf added the METAL shelf/crate ladders over
 * the FULL 60-line metalset (the {@link GT6Hoppers#MATERIALS} anchor table, :186-245) —
 * the same MTE classes as the wooden rows (MultiTileEntityBookShelf/BottleCrate, the
 * NBT_TEXTURE column absent → the material's casingMachine icon × mRGBa, the metalset
 * :143-144 rows), so the same Kind/BET/geometry serves both, only the texture face and
 * the name compose differ. Upstream columns carried verbatim per row:
 * <ul>
 * <li>Locker — id 7300+aID (:138), hardness = aHardness, the armor-swap front face;</li>
 * <li>Compartment Drawer — id 4000+aID (:140), hardness = aHardness, the 144-slot
 *     four-quadrant GUI;</li>
 * <li>Mechanical/Key Locked Safe — id 2000+aID (:134) / 3000+aID (:135), hardness =
 *     resistance = aHardness*2 (the blast-resistant column: Bronze 14, Steel 12);</li>
 * <li>Wooden Bookshelf — id 7000+i (:177-179), hardness 2.0;</li>
 * <li>Wooden Bottlecrate — id 8700+i (:180), hardness 0.5, resistance 2.0;</li>
 * <li>Metal Bookshelf — id 7100+aID (:143), hardness = resistance = aHardness;</li>
 * <li>Metal Bottlecrate — id 8600+aID (:144), hardness 0.5, resistance = aHardness.</li>
 * </ul>
 * Tool-quality columns fold (no hardness-harvest layer on the port block properties); the
 * SFX folds (click/collect/anvil place) defer with the cosmetic layer; the upstream
 * creative-tab homes (Safes 2010, Storage 32751 — Loader_MultiTileEntities.java:134-135
 * and the :138-144/:181-184 storage rows) pool into the MACHINES_TAB join (task
 * tabfix-b-energy, {@link #onBuildTabContents}; the GTBarrels:257 pooling precedent —
 * supersedes the old "no creative tab row" append note).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6StaticStorages {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The storage kinds — the five upstream container classes (Safe twice: its two personalities). */
	public enum Kind {
		LOCKER, DRAWER, SAFE_MECHANICAL, SAFE_KEYLOCKED, BOOKSHELF, BOTTLECRATE
	}

	/** One loader material anchor — slug + display word (the HopperMaterial shape). */
	public record StaticMaterial(String slug, String display, float hardness, int metaId) {
		/**
		 * The loader material face — the FULL 60-line metalset dispatch rides the shared
		 * {@link GT6Hoppers#bySlug} table (task material-mc-b-storage-mass-shelf; the
		 * former two-case bronze/steel switch folded into it when the metal bookshelf/
		 * bottlecrate ladders joined, one loader table per slug, loud drift preserved).
		 */
		public OreDictMaterial mt() {
			return GT6Hoppers.bySlug(slug).mt();
		}
	}

	/** The two metal ladder anchors (Loader :191 Bronze / :202 Steel — the hopper-family ruling). */
	public static final StaticMaterial MAT_BRONZE = new StaticMaterial("bronze", "Bronze", 7.0F, 9);
	public static final StaticMaterial MAT_STEEL = new StaticMaterial("steel", "Steel", 6.0F, 10);

	/** One plank-ladder row of the wooden subset (the vanilla planks, subset order i 0..9). */
	public record Plank(String slug, String display, Item item) {
	}

	/** The vanilla-planks subset — the 300-ladder fold (class doc); subset order IS the ladder index. */
	public static final List<Plank> PLANKS = List.of(
			new Plank("oak", "Oak", Items.OAK_PLANKS),
			new Plank("spruce", "Spruce", Items.SPRUCE_PLANKS),
			new Plank("birch", "Birch", Items.BIRCH_PLANKS),
			new Plank("jungle", "Jungle", Items.JUNGLE_PLANKS),
			new Plank("acacia", "Acacia", Items.ACACIA_PLANKS),
			new Plank("dark_oak", "Dark Oak", Items.DARK_OAK_PLANKS),
			new Plank("mangrove", "Mangrove", Items.MANGROVE_PLANKS),
			new Plank("cherry", "Cherry", Items.CHERRY_PLANKS),
			new Plank("crimson", "Crimson", Items.CRIMSON_PLANKS),
			new Plank("warped", "Warped", Items.WARPED_PLANKS));

	/** The meta id bases (Loader :138 locker / :140 drawer / :134-135 safes / :177-179 shelf ladder / :180 crate ladder / :143 metal shelf ladder / :144 metal crate ladder). */
	public static final int META_ID_LOCKER = 7300;
	public static final int META_ID_DRAWER = 4000;
	public static final int META_ID_SAFE_MECHANICAL = 2000;
	public static final int META_ID_SAFE_KEYLOCKED = 3000;
	public static final int META_ID_BOOKSHELF = 7000;
	public static final int META_ID_BOTTLECRATE = 8700;
	/** The METAL shelf/crate ladder bases (task material-mc-b-storage-mass-shelf — Loader :143 {@code 7100+aID} / :144 {@code 8600+aID}). */
	public static final int META_ID_METAL_BOOKSHELF = 7100;
	public static final int META_ID_METAL_BOTTLECRATE = 8600;

	/**
	 * The metal shelf/crate composed display templates (the GT6Hoppers.DISPLAY_KEY shape):
	 * the upstream registration names are the parenthesized forms —
	 * {@code "Bookshelf ("+aMat.getLocal()+")"} (Loader :143) and
	 * {@code "Bottlecrate ("+aMat.getLocal()+")"} (:144); zh faces
	 * {@code 书架 (%s)} / {@code 瓶筐 (%s)} (tmp/gregtech.lang :14096-14159/:14657-14716 verbatim).
	 */
	public static final String DISPLAY_METAL_BOOKSHELF_KEY = "gt6.row.metal_bookshelf.display";
	public static final String DISPLAY_METAL_BOTTLECRATE_KEY = "gt6.row.metal_bottlecrate.display";

	/** The row's material small-unit key (the shared hopper/boiler/pipes mat key). */
	public static String matUnitKeyOf(StaticRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	/** The composed name of a METAL row (the pure compose seam, the GT6Hoppers.displayOf shape; plank rows keep the description id). */
	public static net.minecraft.network.chat.MutableComponent displayOf(StaticRow aRow) {
		return net.minecraft.network.chat.Component.translatable(
				aRow.kind() == Kind.BOOKSHELF ? DISPLAY_METAL_BOOKSHELF_KEY : DISPLAY_METAL_BOTTLECRATE_KEY,
				net.minecraft.network.chat.Component.translatable(matUnitKeyOf(aRow)));
	}

	/**
	 * One registration row — the block-carrier projection of the metalset/plank-ladder line.
	 *
	 * @param path       the gt6 registry path (the blockstate/model/lang key tail, the BET mirror)
	 * @param metaId     the upstream MultiTileEntity id
	 * @param kind       the container kind (the BE class and the block use arms)
	 * @param material   the metal anchor (null on the wooden rows)
	 * @param plank      the plank anchor (null on the metal rows)
	 * @param hardness   the NBT_HARDNESS column
	 * @param resistance the NBT_RESISTANCE column (== hardness on every row but the crate)
	 */
	public record StaticRow(String path, int metaId, Kind kind, @Nullable StaticMaterial material,
			@Nullable Plank plank, float hardness, float resistance) {

		/**
		 * The block properties (strength(hardness, resistance) — the two loader columns).
		 * symptom27 rider (task hopper-culling-xray, the review-seat stitch): the BOOKSHELF
		 * rows ride {@code .noOcclusion()} — the family renders the open-front frame model
		 * (the 28-book BER niches, the six-box frame) over the FULL-CUBE {@code SHELF_SHAPE}
		 * (task shelf-crate-2px-realign), so the default {@code canOcclude=true} makes
		 * {@code getOcclusionShape} (=getShape, vanilla BlockBehaviour.java:240-242/911) cull
		 * the neighbor faces against the open front — the hopper family's exact X-ray
		 * mechanism (GT6HopperNoOcclusionTest; vanilla's own hopper carries noOcclusion,
		 * Blocks.java:3093). The BOTTLECRATE rows stay innocent: their 8px
		 * {@code CRATE_SELECTION_SHAPE} keeps the occlusion shape non-full and no neighbor
		 * face is culled. {@code isViewBlocking(never)} is the fog-only rider (the
		 * GT6Hoppers seam form).
		 */
		public BlockBehaviour.Properties properties() {
			BlockBehaviour.Properties tProps = BlockBehaviour.Properties.of()
					.strength(hardness, resistance);
			if (kind == Kind.BOOKSHELF) {
				tProps = tProps.noOcclusion().isViewBlocking(GT6StaticStorages::never);
			}
			// the wooden shelf/crate ladders carry the plank look (the WOOD carrier), the
			// metal ladders ride the material machine/util blocks (the METAL carrier)
			return tProps.sound(material() != null || (kind != Kind.BOOKSHELF && kind != Kind.BOTTLECRATE)
					? SoundType.METAL : SoundType.WOOD);
		}
	}

	/** The 148 rows in registration order (metal ladder, the two wooden ladders, the metal shelf/crate ladders). */
	public static final List<StaticRow> ROWS = buildRows();

	private static List<StaticRow> buildRows() {
		List<StaticRow> rRows = new ArrayList<>();
		for (StaticMaterial tMat : new StaticMaterial[] {MAT_BRONZE, MAT_STEEL}) {
			rRows.add(new StaticRow("locker_" + tMat.slug(), META_ID_LOCKER + tMat.metaId(), Kind.LOCKER, tMat, null, tMat.hardness(), tMat.hardness()));
			rRows.add(new StaticRow("drawer_" + tMat.slug(), META_ID_DRAWER + tMat.metaId(), Kind.DRAWER, tMat, null, tMat.hardness(), tMat.hardness()));
			rRows.add(new StaticRow("safe_mechanical_" + tMat.slug(), META_ID_SAFE_MECHANICAL + tMat.metaId(), Kind.SAFE_MECHANICAL, tMat, null, tMat.hardness() * 2, tMat.hardness() * 2));
			rRows.add(new StaticRow("safe_keylocked_" + tMat.slug(), META_ID_SAFE_KEYLOCKED + tMat.metaId(), Kind.SAFE_KEYLOCKED, tMat, null, tMat.hardness() * 2, tMat.hardness() * 2));
		}
		for (int i = 0; i < PLANKS.size(); i++) {
			Plank tPlank = PLANKS.get(i);
			rRows.add(new StaticRow("bookshelf_" + tPlank.slug(), META_ID_BOOKSHELF + i, Kind.BOOKSHELF, null, tPlank, 2.0F, 2.0F));
			rRows.add(new StaticRow("bottlecrate_" + tPlank.slug(), META_ID_BOTTLECRATE + i, Kind.BOTTLECRATE, null, tPlank, 0.5F, 2.0F));
		}
		// task material-mc-b-storage-mass-shelf — the METAL shelf/crate ladders over the full
		// 60-line metalset (Loader :143/:144, the GT6Hoppers.MATERIALS anchor table verbatim).
		// The bookshelf row: hardness = resistance = aHardness. The crate row: NBT_HARDNESS
		// 0.5F fixed, NBT_RESISTANCE = aResistance (:144).
		for (GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
			rRows.add(new StaticRow("bookshelf_metal_" + tMat.slug(), META_ID_METAL_BOOKSHELF + tMat.metaId(), Kind.BOOKSHELF,
					materialOf(tMat), null, tMat.hardness(), tMat.hardness()));
			rRows.add(new StaticRow("bottlecrate_metal_" + tMat.slug(), META_ID_METAL_BOTTLECRATE + tMat.metaId(), Kind.BOTTLECRATE,
					materialOf(tMat), null, 0.5F, tMat.hardness()));
		}
		return rRows;
	}

	/** The loader-line anchor conversion (the slug/display/hardness/aID columns carry over one-to-one). */
	private static StaticMaterial materialOf(GT6Hoppers.HopperMaterial aMat) {
		return new StaticMaterial(aMat.slug(), aMat.display(), aMat.hardness(), aMat.metaId());
	}

	/** The registered blocks by path (the BET multi-mount arrays + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6StorageBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** symptom27 rider: the shelf family never blocks the view (fog) — the GT6Hoppers::never rider form. */
	private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		return false;
	}

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (StaticRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6StorageBlock(tRow, tRow.properties())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new BlockItem(GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The per-kind block arrays (the BET multi-mount arguments). */
	public static Block[] blockArray(Kind aKind) {
		List<Block> rBlocks = new ArrayList<>();
		for (StaticRow tRow : ROWS) {
			if (tRow.kind() == aKind) rBlocks.add(BLOCKS_BY_PATH.get(tRow.path()).get());
		}
		return rBlocks.toArray(new Block[0]);
	}

	/**
	 * The METAL-row blocks (task tint-coverage-batch — the locker/drawer/safe pair kinds
	 * over the Bronze/Steel anchors; task material-mc-b-storage-mass-shelf — the metal
	 * shelf/crate ladders join over the full 60-material metalset, 8 → 128): the tint-walk
	 * registration payload, since every material row carries a grayscale tintindex-0 body
	 * (the wooden bookshelf/bottlecrate rows stay out — the vanilla-finished plank art, the
	 * stone precedent — never wrapped).
	 */
	public static Block[] metalBlockArray() {
		List<Block> rBlocks = new ArrayList<>();
		for (StaticRow tRow : ROWS) {
			if (tRow.material() != null) rBlocks.add(BLOCKS_BY_PATH.get(tRow.path()).get());
		}
		return rBlocks.toArray(new Block[0]);
	}

	/** The lookup for the data-driven place arms — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GT6StorageBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the item mass storage band (task storage-massstorage — the Loader :141-142
	// metalset rows over the 60-material loop; the row material anchor IS the
	// GT6Hoppers.MATERIALS table, the hopper-matrix ruling)
	// -------------------------------------------------------------------------

	/** The loader meta id bases (Loader :141 id 6000+aID standard, :142 id 6200+aID logistics). */
	public static final int META_ID_MASS_STORAGE = 6000;
	public static final int META_ID_MASS_STORAGE_LOGISTICS = 6200;

	/** The display templates (the i18n-compose-rows form; the loader name literals :141-142). */
	public static final String MASS_DISPLAY_KEY = "gt6.row.massstorage.display";
	public static final String MASS_LOGISTICS_DISPLAY_KEY = "gt6.row.logistics_massstorage.display";

	/**
	 * One mass-storage row — the block-carrier projection of one Loader metalset line half
	 * (:141 standard / :142 logistics). The hardness/resistance columns ride the
	 * metalset call (aHardness == aResistance on every material line, the hopper-table
	 * fold); the NBT_CAPACITY column never rides (both loader rows omit it — the
	 * upstream default 1000000, MultiTileEntityMassStorage.java:71).
	 *
	 * @param path      the gt6 registry path (the blockstate/model/lang key tail)
	 * @param metaId    the upstream MultiTileEntity id (6000+aID / 6200+aID)
	 * @param material  the row material (the GT6Hoppers.MATERIALS entry)
	 * @param logistics the :142 kind flag (the black body, cyan digits, the
	 *                  {@code ITileEntityLogisticsStorage} face; recipe pool-cut — the
	 *                  {@code IL.Cover_Logistics_Generic_Storage} column item is
	 *                  unported, the GTBarrels logistics-tank ruling)
	 */
	public record MassRow(String path, int metaId, GT6Hoppers.HopperMaterial material, boolean logistics) {
		/** The block properties (strength(hardness, resistance) — the metalset columns; the aMachine METAL sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness(), material.hardness())
					.sound(SoundType.METAL);
		}
	}

	/**
	 * The 120 mass-storage rows in registration order (the metalset loop walk — per
	 * material the standard :141 then the logistics :142, the upstream pair order).
	 */
	public static final List<MassRow> MASS_ROWS = buildMassRows();

	private static List<MassRow> buildMassRows() {
		List<MassRow> rRows = new ArrayList<>(GT6Hoppers.MATERIALS.size() * 2);
		for (GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
			rRows.add(new MassRow("mass_storage_" + tMat.slug(), META_ID_MASS_STORAGE + tMat.metaId(), tMat, false));
			rRows.add(new MassRow("logistics_mass_storage_" + tMat.slug(), META_ID_MASS_STORAGE_LOGISTICS + tMat.metaId(), tMat, true));
		}
		return List.copyOf(rRows);
	}

	/** The registered mass-storage blocks by path (the BET multi-mount arrays + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6MassStorageBlock>> MASS_BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered mass-storage items, same keys as {@link #MASS_BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> MASS_ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (MassRow tRow : MASS_ROWS) {
			MASS_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6MassStorageBlock(tRow, tRow.properties())));
			MASS_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GT6MassStorageItem(GT6StaticStorages.MASS_BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The standard-kind blocks in registration order (the MASS_STORAGE_BE multi-mount array). */
	public static Block[] massStorageBlockArray() {
		return massKindArray(false);
	}

	/** The logistics-kind blocks in registration order (the MASS_STORAGE_LOGISTICS_BE multi-mount array). */
	public static Block[] massLogisticsBlockArray() {
		return massKindArray(true);
	}

	private static Block[] massKindArray(boolean aLogistics) {
		List<Block> rBlocks = new ArrayList<>();
		for (MassRow tRow : MASS_ROWS) {
			if (tRow.logistics() == aLogistics) rBlocks.add(MASS_BLOCKS_BY_PATH.get(tRow.path()).get());
		}
		return rBlocks.toArray(new Block[0]);
	}

	/**
	 * The tint walk payload (the paintableBlockArray form) — every row carries its
	 * upstream NBT_MATERIAL (Loader :141 aMat / :142 MT.Black), the tintindex-0 body
	 * cube is the seat.
	 */
	public static Block[] massPaintableBlockArray() {
		return concat(massKindArray(false), massKindArray(true));
	}

	private static Block[] concat(Block[] aA, Block[] aB) {
		Block[] rOut = new Block[aA.length + aB.length];
		System.arraycopy(aA, 0, rOut, 0, aA.length);
		System.arraycopy(aB, 0, rOut, aA.length, aB.length);
		return rOut;
	}

	/** The composed name of a mass-storage row (the {@code GT6Hoppers.displayOf} form). */
	/** The row-material small-unit key (the shared {@code gt6.row.mat.<slug>} family, the hopper shape). */
	public static String massMatUnitKeyOf(MassRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	public static net.minecraft.network.chat.MutableComponent massDisplayOf(MassRow aRow) {
		return Component.translatable(aRow.logistics() ? MASS_LOGISTICS_DISPLAY_KEY : MASS_DISPLAY_KEY,
				Component.translatable(massMatUnitKeyOf(aRow)));
	}

	/**
	 * The mass-storage block — the front-face machine cube (the aMachine family, the
	 * upstream pass-0 full cube): the FACING is the front (toward the placer, the
	 * GT6StorageBlock furnace convention; the upstream getDefaultSide SIDE_FRONT :538
	 * with SIDES_HORIZONTAL valid sides :539). The use() face is the upstream
	 * onToolClick2 tool arms (:122-238) ahead of the onBlockActivated3 pixel grid
	 * (:241-331); no GUI exists upstream — there is no MUI face here.
	 *
	 * <p>Geometry: the render/collision/outline trio IS the untouched vanilla full cube
	 * (no getShape/collision override — the three-way ruling posture, the metal kinds of
	 * the static batch); the digit strip and the content display are the BER's quads,
	 * not model geometry.
	 */
	public static final class GT6MassStorageBlock extends gregtech6.block.GTEntityBlock {

		/** Facing property — the FRONT face, horizontals only (the upstream SIDES_HORIZONTAL :539). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		private final MassRow mRow;

		public GT6MassStorageBlock(MassRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}

		/** The registration row (the block-carrier config read). */
		public MassRow row() {
			return mRow;
		}

		/**
		 * The combined tint dispatch arm (the {@code GT6StorageBlock.materialOf} shape):
		 * the standard rows answer their loader material, the logistics rows the
		 * MT.Black body column (Loader :142 — the black body IS the logistics identity).
		 */
		@Nullable
		public static OreDictMaterial materialOf(@Nullable Block aBlock) {
			if (!(aBlock instanceof GT6MassStorageBlock tStorage)) return null;
			return tStorage.mRow.logistics() ? MT.Black : tStorage.mRow.material().mt();
		}

		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the GT6StorageBlock fork — a
		// parse-time representative value; world save/load never runs through this codec).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6MassStorageBlock> codec() {
			return simpleCodec(aProperties -> new GT6MassStorageBlock(GT6StaticStorages.MASS_ROWS.get(0), aProperties));
		}
		*///?}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return mRow.logistics() ? GTBlockEntities.MASS_STORAGE_LOGISTICS_BE.get() : GTBlockEntities.MASS_STORAGE_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		/** The composed display name (the i18n-compose-rows carrier, {@code GT6Hoppers.GT6HopperBlock.getName} form). */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return massDisplayOf(mRow);
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the front faces the placer (the vanilla furnace arm; the upstream
			// getSideForPlayerPlacing horizontal default)
			return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection().getOpposite());
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// keep the BE's NBT fallback byte in step (the live truth is the state property)
			if (aLevel.getBlockEntity(aPos) instanceof GT6MassStorageBlockEntity tStorage) {
				tStorage.setFacingNbtFallback((byte) aState.getValue(FACING).get3DDataValue());
			}
		}

		/**
		 * The upstream onBlockActivated3 + onToolClick2 routing: the tool arms claim the
		 * click first (the GT6HopperBlock use() shape), then the pixel grid; every arm
		 * CONSUMES the click (upstream :330 return T).
		 */
		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (the GT6HopperBlock fork verbatim)
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			BlockEntity tTile = aLevel.getBlockEntity(aPos);
			if (!(tTile instanceof GT6MassStorageBlockEntity tStorage)) {
				return InteractionResult.PASS;
			}
			ItemStack tHeld = aPlayer.getItemInHand(aHand);
			//? if forge {
			Direction tFront = aState.getValue(FACING);
			//?} else {
			/*Direction tFront = aState.getValue(FACING);
			 *///?}
			byte tFrontSide = (byte) tFront.get3DDataValue();
			// the tool arms (upstream onToolClick2 :122-238; the tape/scissors pair rides
			// the Duct_Tape item pool — the mode bit stays NBT-visible)
			if (tHeld.canPerformAction(GTPincersItem.ACTION)) {
				if (!aLevel.isClientSide) tStorage.pincersTakeAll(aPlayer);
				return InteractionResult.CONSUME;
			}
			if (tHeld.canPerformAction(GTSoftHammerItem.ACTION)) {
				if (!aLevel.isClientSide) tStorage.softHammerEject();
				return InteractionResult.CONSUME;
			}
			if (tHeld.canPerformAction(GT6ToolActions.WRENCH)) {
				if (!aLevel.isClientSide) aPlayer.displayClientMessage(Component.literal(tStorage.monkeyWrenchToggle()), true);
				return InteractionResult.CONSUME;
			}
			if (tHeld.canPerformAction(GT6ToolActions.SCREWDRIVER)) {
				if (!aLevel.isClientSide) aPlayer.displayClientMessage(Component.literal(tStorage.screwdriverToggle()), true);
				return InteractionResult.CONSUME;
			}
			if (tHeld.canPerformAction(GT6ToolActions.CUTTER)) {
				if (!aLevel.isClientSide) aPlayer.displayClientMessage(Component.literal(tStorage.cutterToggle()), true);
				return InteractionResult.CONSUME;
			}
			if (tHeld.canPerformAction(GTMagnifyingGlassItem.ACTION)) {
				if (!aLevel.isClientSide) {
					for (Component tLine : tStorage.lensStatus()) aPlayer.displayClientMessage(tLine, false);
				}
				return InteractionResult.CONSUME;
			}
			// the pixel grid (upstream onBlockActivated3 :241-331, front face only)
			if (tFaceClicked(aHit, tFrontSide)) {
				float tHitX = (float) (aHit.getLocation().x - aPos.getX());
				float tHitY = (float) (aHit.getLocation().y - aPos.getY());
				float tHitZ = (float) (aHit.getLocation().z - aPos.getZ());
				boolean tConsumed = tStorage.clickFrontFace(aPlayer, tHeld, tFrontSide, tHitX, tHitY, tHitZ);
				return tConsumed ? InteractionResult.CONSUME : InteractionResult.PASS;
			}
			return InteractionResult.PASS;
		}

		/** The front-face gate of the upstream :243 conjunct. */
		private static boolean tFaceClicked(BlockHitResult aHit, byte aFrontSide) {
			return aHit.getDirection().get3DDataValue() == aFrontSide;
		}

		/**
		 * The break face — the partial residue drops BEFORE the content pop (upstream
		 * breakBlock :559-566), the taped mass stack rides the harvested item (upstream
		 * keepSlot :556, the crate BlockEntityTag fold), the comparator tail rides the
		 * vanilla convention; the untaped content pops ride the {@code GTEntityBlock}
		 * fallback over the BE's {@code canDrop(int)}.
		 */
		@Override
		public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
			if (!aOldState.is(aNewState.getBlock())) {
				BlockEntity tBE = aLevel.getBlockEntity(aPos);
				if (tBE instanceof GT6MassStorageBlockEntity tStorage) {
					tStorage.dropPartialUnits();
					if ((tStorage.mMode & GT6MassStorageBlockEntity.MODE_TAPED) != 0 && tStorage.slotHas(GT6MassStorageBlockEntity.SLOT_MASS)) {
						// the keepSlot fold: the mass stack rides the ONE dropped item
						// (the vanilla shulker convention; the funnel slot popped separately)
						ItemStack tDrop = new ItemStack(this);
						CompoundTag tSaved = tStorage.saveWithoutMetadata();
						stripFunnelSlot(tSaved);
						//? if forge {
						tDrop.addTagElement("BlockEntityTag", tSaved);
						//?} else {
						/*// 21.1: the same convention over the BLOCK_ENTITY_DATA component
						tDrop.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
								net.minecraft.world.item.component.CustomData.of(tSaved));
						*///?}
						Block.popResource(aLevel, aPos, tDrop);
					}
					aLevel.updateNeighbourForOutputSignal(aPos, this);
				}
			}
			super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
		}

		/** The kept tag carries slot 1 ONLY (the funnel slot pops like upstream canDrop(0) = T). */
		private static void stripFunnelSlot(CompoundTag aSaved) {
			if (aSaved.contains("inventory", net.minecraft.nbt.Tag.TAG_LIST)) {
				net.minecraft.nbt.ListTag tList = aSaved.getList("inventory", net.minecraft.nbt.Tag.TAG_COMPOUND);
				net.minecraft.nbt.ListTag tFiltered = new net.minecraft.nbt.ListTag();
				for (int i = 0; i < tList.size(); i++) {
					CompoundTag tEntry = tList.getCompound(i);
					if (tEntry.contains("Slot", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) && tEntry.getInt("Slot") != GT6MassStorageBlockEntity.SLOT_FUNNEL) {
						tFiltered.add(tEntry);
					}
				}
				aSaved.put("inventory", tFiltered);
			}
		}

		/**
		 * The explosion face (upstream onExploded :544 — the mass store dies, the taped
		 * or not): the kill rides the BE seam, then the vanilla air swap fires the
		 * onRemove pop for the funnel slot.
		 */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			if (aLevel.getBlockEntity(aPos) instanceof GT6MassStorageBlockEntity tStorage) {
				tStorage.killForExplosion();
			}
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
		}
	}

	/**
	 * The mass-storage BlockItem — the composed name carrier + the tooltip face
	 * (upstream addToolTips :103-120): the content line (yellow), the capacity line and
	 * the ACT-adjacent line over the upstream lang keys
	 * {@code gt.multitileentity.massstorage.tooltip.1/2} (:98-101); the per-tool DGRAY
	 * hint block folds with the LH cosmetic pool (the tool arms themselves are live).
	 */
	public static final class GT6MassStorageItem extends GTComposedNameItem {

		public GT6MassStorageItem(Block aBlock, Item.Properties aProperties) {
			super(aBlock, aProperties);
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
			tooltipRows(aTooltip);
		}
		//?} else {
		/*// 21.1: Level folded into TooltipContext (the GTGrassBlock fork shape)
		@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, net.minecraft.world.item.TooltipFlag aFlag) {
			tooltipRows(aTooltip);
		}
		*///?}

		/** The two tooltip rows (the upstream LH add :98-101 over :104-107). */
		private static void tooltipRows(List<Component> aTooltip) {
			aTooltip.add(Component.translatable("gt.multitileentity.massstorage.tooltip.1").withStyle(net.minecraft.ChatFormatting.GRAY));
			aTooltip.add(Component.translatable("gt.multitileentity.massstorage.tooltip.2").withStyle(net.minecraft.ChatFormatting.GRAY));
		}
	}

	// -------------------------------------------------------------------------
	// the block carrier — the horizontal-front container cube
	// -------------------------------------------------------------------------

	/**
	 * The static storage block — the front-face cube over the shared BET: the FACING is the
	 * front (toward the placer, the vanilla furnace convention = the upstream
	 * getSideForPlayerPlacing arm), the kind arms dispatch off the row. The half-height
	 * crate shape rides the BOTTLECRATE kind (the 8/16 body box, the three-way ruling).
	 */
	public static final class GT6StorageBlock extends gregtech6.block.GTEntityBlock {

		/** Facing property — the FRONT face, horizontals only (upstream SIDES_HORIZONTAL valid sides). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		/**
		 * The crate body box — the rendered frame envelope (0,0,0)-(16,8,16): the outline
		 * (getShape) AND the collision ride the same box (task shelf-crate-2px-realign,
		 * the user ruling 2026-10-06: "我想要让渲染、碰撞箱、描边对齐，看起来不奇怪" —
		 * looks first, upstream values second). The upstream split stays a deliberate
		 * deviation: selection PX_P[6] = 6px (:213-214) vs collision PX_N[6] = 10px
		 * (:212) against the 8px walls (:165-166) read as a 6/8/10 staircase — the
		 * reported "outline vs model ~2px" mismatch. The bottles (the BER display) rise
		 * above the box as pure display — the vanilla flower-pot precedent (the pot
		 * shapes the boxes, the flower does not extend them).
		 */
		public static final VoxelShape CRATE_SELECTION_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
		public static final VoxelShape CRATE_COLLISION_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

		/**
		 * The bookshelf body box — the rendered frame envelope IS the full cube (task
		 * shelf-crate-2px-realign, the three-way ruling): the outline rides it and the
		 * collision stays the untouched vanilla full cube (no override — the 28 books
		 * live inside the niches). The former 2px-inset slab (upstream :349-350)
		 * outlined a hole 2px inside the visible mass — the deliberate deviation.
		 * Named constant so the offline pins can ride it (a Block instance cannot be
		 * constructed offline — the frozen block registry, NamespacedWrapper:271).
		 */
		public static final VoxelShape SHELF_SHAPE = Block.box(0, 0, 0, 16, 16, 16);

		private final StaticRow mRow;

		public GT6StorageBlock(StaticRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the GT6HopperBlock simpleCodec form —
		// a parse-time representative value; world save/load never runs through this codec).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6StorageBlock> codec() {
			return simpleCodec(aProperties -> new GT6StorageBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the block-carrier config read). */
		public StaticRow row() {
			return mRow;
		}

		/**
		 * The METAL rows compose their name at runtime (task material-mc-b-storage-mass-shelf,
		 * the GT6HopperBlock.getName shape): "Bookshelf (Bronze)" — the BlockItem inherits the
		 * block name (the vanilla BlockItem.getName face), the plank rows keep the
		 * description id.
		 */
		@Override
		public net.minecraft.network.chat.MutableComponent getName() {
			return mRow.material() != null ? displayOf(mRow) : super.getName();
		}

		/**
		 * The storage-domain material dispatch (task tint-coverage-batch, the
		 * {@code GTBarrelBlock.materialOf} mirror shape): the metal rows resolve their
		 * loader anchor (Bronze :191 / Steel :202 — the hopper-family ruling) through the
		 * row carrier, the wooden rows answer null (the vanilla-finished plank art keeps
		 * the white identity, the stone precedent). Every non-storage block is null here.
		 */
		@Nullable
		public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
			if (!(aBlock instanceof GT6StorageBlock tStorage)) return null;
			StaticMaterial tMaterial = tStorage.row().material();
			return tMaterial == null ? null : tMaterial.mt();
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the front faces the placer (the vanilla furnace arm = the upstream
			// getSideForPlayerPlacing :73 branch)
			return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection().getOpposite());
		}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return switch (mRow.kind()) {
				case LOCKER -> GTBlockEntities.LOCKER_BE.get();
				case DRAWER -> GTBlockEntities.DRAWER_QUAD_BE.get();
				case SAFE_MECHANICAL -> GTBlockEntities.SAFE_BE.get();
				case SAFE_KEYLOCKED -> GTBlockEntities.SAFE_KEYLOCKED_BE.get();
				case BOOKSHELF -> GTBlockEntities.BOOKSHELF_BE.get();
				case BOTTLECRATE -> GTBlockEntities.BOTTLECRATE_BE.get();
			};
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		/**
		 * The wooden-kind geometry (task shelf-crate-2px-realign, the three-way ruling):
		 * the crate outline IS the 8px body box, the bookshelf the full cube (SHELF_SHAPE
		 * — the collision keeps the untouched vanilla cube); the metal kinds stay the
		 * full cube.
		 */
		@Override
		public VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return switch (mRow.kind()) {
				case BOTTLECRATE -> CRATE_SELECTION_SHAPE;
				case BOOKSHELF -> SHELF_SHAPE;
				default -> super.getShape(aState, aLevel, aPos, aContext);
			};
		}

		@Override
		public VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return mRow.kind() == Kind.BOTTLECRATE ? CRATE_COLLISION_SHAPE : super.getCollisionShape(aState, aLevel, aPos, aContext);
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// keep the BE's NBT fallback byte in step (the live truth is the state property)
			if (aLevel.getBlockEntity(aPos) instanceof GT6StaticStorageBaseBlockEntity tStorage) {
				tStorage.setFacingNbtFallback((byte) aState.getValue(FACING).get3DDataValue());
			}
		}

		//? if forge {
		@Override
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (the GT6HopperBlock fork verbatim)
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			BlockEntity tTile = aLevel.getBlockEntity(aPos);
			if (!(tTile instanceof GT6StaticStorageBaseBlockEntity tStorage)) {
				return InteractionResult.PASS;
			}
			Direction tFront = aState.getValue(FACING);
			Direction tFace = aHit.getDirection();
			boolean tServer = !aLevel.isClientSide;
			ItemStack tHand = aPlayer.getItemInHand(aHand);
			switch (mRow.kind()) {
				case LOCKER -> {
					// the armor-swap arm (upstream onBlockActivated3 :58-79, front face only)
					if (tFace != tFront) return InteractionResult.PASS;
					if (tServer && tStorage instanceof GT6LockerBlockEntity tLocker) {
						swapArmor(tLocker, aPlayer);
					}
					return InteractionResult.CONSUME;
				}
				case DRAWER -> {
					// the monkey-wrench sided toggle (upstream onToolClick2 :92-100) + the MUI
					// open (the wave-4 one-page ruling; the click-a-quadrant window folds).
					// DECLARED DEVIATION: the toggle answers the front face only (the use-arm
					// routing below) — upstream onToolClick2 took a wrench on ANY face; the
					// toggled state is identical (the BE class doc).
					if (tFace != tFront) return InteractionResult.PASS;
					if (tHand.is(gregtech6.datagen.GT6ItemTags.TOOLS_WRENCH)) {
						if (tServer && tStorage instanceof GT6DrawerQuadBlockEntity tDrawer) {
							tDrawer.monkeyWrench();
							aPlayer.displayClientMessage(Component.literal(tDrawer.accessChatLine()), true);
						}
						return InteractionResult.CONSUME;
					}
					if (tServer && aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer
							&& tStorage instanceof GT6DrawerQuadBlockEntity tDrawer) {
						GT6MuiMachine.tryOpen(tServerPlayer, tDrawer);
					}
					return InteractionResult.CONSUME;
				}
				case SAFE_MECHANICAL, SAFE_KEYLOCKED -> {
					if (tFace != tFront) return InteractionResult.PASS;
					// the key arm (upstream Behavior_Key.onItemUseFirst :44-63 — the
					// 1.7.10 onItemUseFirst ran BEFORE the block arm, so a key in hand
					// intercepts the open arm verbatim): a keyed stack toggles, a blank
					// one claims/clones — the GT6Keys face over the KeyLocked latch.
					if (tStorage instanceof GT6SafeKeyLockedBlockEntity tKeySafe
							&& tHand.getItem() instanceof gregtech6.items.GT6Keys.GT6KeyItem) {
						if (tServer) gregtech6.items.GT6Keys.useOnKeyLocked(tKeySafe, tHand);
						return InteractionResult.CONSUME;
					}
					// the open arm (upstream onBlockActivated3 :80-87): loot generates on the
					// first open, the GUI gates on the latch (the KeyLocked mOpened)
					if (tServer && tStorage instanceof GT6SafeBlockEntity tSafe) {
						tSafe.tryGenerateDungeonLoot();
						if (tSafe.isOpen() && aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer) {
							GT6MuiMachine.tryOpen(tServerPlayer, tSafe);
						}
					}
					return InteractionResult.CONSUME;
				}
				case BOOKSHELF -> {
					// the book arm (upstream onBlockActivated3 :213-238), front OR back face,
					// the pixel picker folded to range-level picks (the BE class doc)
					if (tFace != tFront && tFace != tFront.getOpposite()) return InteractionResult.PASS;
					if (tServer && tStorage instanceof GT6BookShelfBlockEntity tShelf) {
						switchBooks(tShelf, aPlayer, tFace == tFront.getOpposite(), aPlayer.isShiftKeyDown(), tHand);
					}
					return InteractionResult.CONSUME;
				}
				default -> {
					// BOTTLECRATE: the bottle arm (upstream onBlockActivated3 :153-157, any face)
					if (tServer && tStorage instanceof GT6BottleCrateBlockEntity tCrate) {
						switchBottles(tCrate, aPlayer, aPlayer.isShiftKeyDown(), tHand);
					}
					return InteractionResult.CONSUME;
				}
			}
		}

		/**
		 * The locker armor swap (upstream :58-79): each of the four slots trades with the
		 * matching worn piece when the locker slot is empty OR holds the piece of ITS type;
		 * the BTRS backpack keep-out folds with the mod (class doc).
		 */
		private static void swapArmor(GT6LockerBlockEntity aLocker, Player aPlayer) {
			boolean tAny = false;
			for (int i = 0; i < GT6LockerBlockEntity.INVENTORY_SIZE; i++) {
				ItemStack tSlotStack = aLocker.getInventory().getStackInSlot(i);
				if (tSlotStack.isEmpty() || GT6LockerBlockEntity.isValidArmorForSlot(tSlotStack, i)) {
					aLocker.getInventory().setStackInSlot(i, aPlayer.getInventory().armor.set(i, tSlotStack));
					tAny = true;
				}
			}
			if (tAny) {
				aPlayer.getInventory().setChanged();
				aLocker.updateInventory();
			}
		}

		/**
		 * The bookshelf book arm (the shift-all/single-take ruling): shift = store ALL books
		 * from the player inventory into the face; a book in hand = insert one into the
		 * first empty of the face; otherwise = take the last occupied of the face. The
		 * upstream button/lever redstone arms defer with the no-tick fold (BE class doc).
		 */
		private static void switchBooks(GT6BookShelfBlockEntity aShelf, Player aPlayer, boolean aBackFace, boolean aShift, ItemStack aHand) {
			if (aShift) {
				for (int i = 0; i < aPlayer.getInventory().getContainerSize(); i++) {
					ItemStack tStack = aPlayer.getInventory().getItem(i);
					int tSlot;
					if (!tStack.isEmpty() && tStack.is(GT6BookShelfBlockEntity.BOOKS)
							&& (tSlot = aShelf.firstEmptyOfFace(aBackFace)) >= 0
							&& aShelf.canInsertItem(tSlot, tStack, (byte) 6)) {
						aShelf.getInventory().setStackInSlot(tSlot, tStack.copyWithCount(1));
						tStack.shrink(1);
					}
				}
				aPlayer.getInventory().setChanged();
				aShelf.updateInventory();
				return;
			}
			if (!aHand.isEmpty() && aHand.is(GT6BookShelfBlockEntity.BOOKS)) {
				int tSlot = aShelf.firstEmptyOfFace(aBackFace);
				if (tSlot >= 0 && aShelf.canInsertItem(tSlot, aHand, (byte) 6)) {
					aShelf.getInventory().setStackInSlot(tSlot, aHand.copyWithCount(1));
					aHand.shrink(1);
					aShelf.updateInventory();
				}
				return;
			}
			int tSlot = aShelf.lastOccupiedOfFace(aBackFace);
			if (tSlot >= 0 && aShelf.canExtractItem(tSlot, (byte) 6)) {
				ItemStack tStack = aShelf.getInventory().getStackInSlot(tSlot);
				aPlayer.getInventory().placeItemBackInInventory(tStack);
				aShelf.getInventory().setStackInSlot(tSlot, ItemStack.EMPTY);
				aShelf.updateInventory();
			}
		}

		/**
		 * The bottlecrate bottle arm (the shift-all/single-take ruling over the upstream
		 * swapBottles :158-183): shift = store ALL bottles; a bottle in hand = insert one;
		 * otherwise = take the last occupied.
		 */
		private static void switchBottles(GT6BottleCrateBlockEntity aCrate, Player aPlayer, boolean aShift, ItemStack aHand) {
			if (aShift) {
				for (int i = 0; i < aPlayer.getInventory().getContainerSize(); i++) {
					ItemStack tStack = aPlayer.getInventory().getItem(i);
					int tSlot;
					if (!tStack.isEmpty() && GT6BottleCrateBlockEntity.isBottleFamily(tStack)
							&& (tSlot = aCrate.firstEmpty()) >= 0) {
						aCrate.getInventory().setStackInSlot(tSlot, tStack.copyWithCount(1));
						tStack.shrink(1);
					}
				}
				aPlayer.getInventory().setChanged();
				aCrate.updateInventory();
				return;
			}
			if (!aHand.isEmpty() && GT6BottleCrateBlockEntity.isBottleFamily(aHand)) {
				int tSlot = aCrate.firstEmpty();
				if (tSlot >= 0) {
					aCrate.getInventory().setStackInSlot(tSlot, aHand.copyWithCount(1));
					aHand.shrink(1);
					aCrate.updateInventory();
				}
				return;
			}
			int tSlot = aCrate.lastOccupied();
			if (tSlot >= 0) {
				aPlayer.getInventory().placeItemBackInInventory(aCrate.getInventory().getStackInSlot(tSlot));
				aCrate.getInventory().setStackInSlot(tSlot, ItemStack.EMPTY);
				aCrate.updateInventory();
			}
		}

		/**
		 * The break face — the content pops ride the {@code GTEntityBlock} fallback
		 * (upstream canDrop = T everywhere but the crate); what stays here is what the base
		 * cannot know: the safe's dungeon-loot re-roll (upstream breakBlock :89-92), the
		 * crate's ONE BlockItem carrying the BlockEntityTag (the keepSlot fold, the vanilla
		 * shulker convention) and the comparator tail.
		 */
		@Override
		public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
			if (!aOldState.is(aNewState.getBlock())) {
				BlockEntity tBE = aLevel.getBlockEntity(aPos);
				if (tBE instanceof GT6SafeBlockEntity tSafe) {
					// the safe generates its dungeon loot on break too (upstream breakBlock :89-92)
					tSafe.tryGenerateDungeonLoot();
				}
				if (tBE instanceof GT6BottleCrateBlockEntity tCrate) {
					// the keepSlot fold: contents ride the dropped item (BE class doc)
					ItemStack tDrop = new ItemStack(this);
					//? if forge {
					tDrop.addTagElement("BlockEntityTag", tCrate.saveWithoutMetadata()); // the vanilla shulker convention
					//?} else {
					/*// 21.1: the same convention over the BLOCK_ENTITY_DATA component (BlockItem
					//updateCustomBlockEntityTag reads the component and loadInto()s the BE)
					tDrop.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
							net.minecraft.world.item.component.CustomData.of(
									tCrate.saveWithoutMetadata(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS)));
					*///?}
					Block.popResource(aLevel, aPos, tDrop);
				} else if (tBE instanceof GT6StaticStorageBaseBlockEntity) {
					aLevel.updateNeighbourForOutputSignal(aPos, this);
				}
			}
			super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
		}

		/**
		 * The crate kind is the canDrop = F block ruling (upstream :244-245): its slots ride
		 * the BlockEntityTag item above, so the base fallback must not ALSO pop them.
		 */
		@Override
		protected boolean canDrop(int aInventorySlot) {
			return mRow.kind() != Kind.BOTTLECRATE;
		}

		/**
		 * The explosion face — the safe carries the upstream {@code onExploded :111
		 * setToAir()} verbatim: the contents are DESTROYED, never scattered (the anti-theft
		 * semantic; the blast-resistance column stands between the explosion and this arm).
		 * The destroy rides the BE seam {@link GT6SafeBlockEntity#destroyForExplosion} —
		 * slots AND the dungeon-loot marker, because the air swap below fires the onRemove
		 * face whose break arm re-rolls the marker into the cleared slots (the leak the
		 * review caught: a surviving marker = the whole pack regenerated and scattered).
		 * Every other kind rides the vanilla pop path (the onRemove face above).
		 */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			if (mRow.kind() == Kind.SAFE_MECHANICAL || mRow.kind() == Kind.SAFE_KEYLOCKED) {
				if (aLevel.getBlockEntity(aPos) instanceof GT6SafeBlockEntity tSafe) {
					tSafe.destroyForExplosion();
				}
			}
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
		}

		/**
		 * The bookshelf enchant-power bonus (upstream getEnchantPowerBonus :160-169): 1 per
		 * normal book, 2 per enchanted book, /12.0 — the BooksGT book classes fold to the
		 * {@code isEnchanted} check (the vanilla-book-subset fold); the MAGICAL material
		 * column folds with the port's material-less rows (the unpaint deviation shape).
		 */
		@Override
		public float getEnchantPowerBonus(BlockState aState, LevelReader aLevel, BlockPos aPos) {
			if (mRow.kind() != Kind.BOOKSHELF || !(aLevel.getBlockEntity(aPos) instanceof GT6BookShelfBlockEntity tShelf)) {
				return 0.0F;
			}
			float tPoints = 0;
			for (int i = 0, l = tShelf.getInventory().getSlots(); i < l; i++) {
				ItemStack tStack = tShelf.getInventory().getStackInSlot(i);
				if (tStack.isEmpty()) continue;
				tPoints += tStack.isEnchanted() ? 2 : 1;
			}
			return tPoints / 12.0F;
		}
	}

	private GT6StaticStorages() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Hoppers.onModConstruct doc). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Hoppers fork verbatim)
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
	 * Pool-cut declaration: upstream hangs the metal kinds on the "Safes" category (tab
	 * 2010, Loader_MultiTileEntities.java:134-135) and the wooden kinds on "Storage" (tab
	 * 32751, :138-144/:177-184); this port pools both joins into MACHINES_TAB (the
	 * GTBarrels:257 pooling precedent).
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
