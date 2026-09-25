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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.tileentity.inventories.GT6BookShelfBlockEntity;
import gregtech6.tileentity.inventories.GT6BottleCrateBlockEntity;
import gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity;
import gregtech6.tileentity.inventories.GT6LockerBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity;
import gregtech6.tileentity.inventories.GT6StaticStorageBaseBlockEntity;

/**
 * The static storage batch registration home (task p26-storage-static-batch, the
 * GT6Hoppers/GT6Boilers self-contained-DR row form): 28 blocks/items over SIX shared
 * BETs — the five BE classes of the {@code gregtech6.tileentity.inventories} batch, the
 * row config riding the block carrier.
 *
 * <p><b>The ITEM BARREL TRAP (the card's hard javadoc clause).</b> Nothing here is the
 * "Item Barrel": the GT6 Item Barrel is the ITEM MASS STORAGE (the
 * MultiTileEntityMassStorageBarrel family, p26-storage-massstorage's card), and it shares
 * its NAME with this repo's already-ported FLUID barrels (GTBarrels, the
 * TileEntityBase08Barrel fluid carrier). The two are unrelated families — searches,
 * KG queries and reviews must not conflate them.
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java metalset()/storages()): the metal
 * kinds anchor the Bronze (aID 9, :191 hardness 7.0) and Steel (aID 10, :202 hardness 6.0)
 * ladder rows — the same two-material ruling as the hopper family card — and the wooden
 * kinds port the 300-ladder (:177-180) onto the vanilla-planks subset, the declared
 * wave-4 deviation (PlankData has no 1.20.1 counterpart; the ladder folds to the subset
 * order). Upstream columns carried verbatim per row:
 * <ul>
 * <li>Locker — id 7300+aID (:138), hardness = aHardness, the armor-swap front face;</li>
 * <li>Compartment Drawer — id 4000+aID (:140), hardness = aHardness, the 144-slot
 *     four-quadrant GUI;</li>
 * <li>Mechanical/Key Locked Safe — id 2000+aID (:134) / 3000+aID (:135), hardness =
 *     resistance = aHardness*2 (the blast-resistant column: Bronze 14, Steel 12);</li>
 * <li>Wooden Bookshelf — id 7000+i (:177-179), hardness 2.0;</li>
 * <li>Wooden Bottlecrate — id 8700+i (:180), hardness 0.5, resistance 2.0.</li>
 * </ul>
 * Tool-quality columns fold (no hardness-harvest layer on the port block properties); the
 * SFX folds (click/collect/anvil place) defer with the cosmetic layer; the upstream
 * creative-tab homes (Safes 2010, Storage 32751 — Loader_MultiTileEntities.java:134-135
 * and the :138-144/:181-184 storage rows) pool into the MACHINES_TAB join (task
 * p38-tabfix-b-energy, {@link #onBuildTabContents}; the GTBarrels:257 pooling precedent —
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
		/** The loader material face (the recipe 'P'-family columns resolve off it). */
		public OreDictMaterial mt() {
			return switch (slug) {
				case "bronze" -> MT.Bronze;
				case "steel" -> MT.Steel;
				default -> throw new IllegalStateException("no loader material for storage slug " + slug);
			};
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

	/** The meta id bases (Loader :138 locker / :140 drawer / :134-135 safes / :177-179 shelf ladder / :180 crate ladder). */
	public static final int META_ID_LOCKER = 7300;
	public static final int META_ID_DRAWER = 4000;
	public static final int META_ID_SAFE_MECHANICAL = 2000;
	public static final int META_ID_SAFE_KEYLOCKED = 3000;
	public static final int META_ID_BOOKSHELF = 7000;
	public static final int META_ID_BOTTLECRATE = 8700;

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

		/** The block properties (strength(hardness, resistance) — the two loader columns). */
		public BlockBehaviour.Properties properties() {
			BlockBehaviour.Properties tProps = BlockBehaviour.Properties.of()
					.strength(hardness, resistance);
			return tProps.sound(kind == Kind.BOOKSHELF || kind == Kind.BOTTLECRATE ? SoundType.WOOD : SoundType.METAL);
		}
	}

	/** The 28 rows in registration order (metal ladder, then the two wooden ladders). */
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
		return rRows;
	}

	/** The registered blocks by path (the BET multi-mount arrays + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6StorageBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

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

	/** The lookup for the data-driven place arms — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GT6StorageBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carrier — the horizontal-front container cube
	// -------------------------------------------------------------------------

	/**
	 * The static storage block — the front-face cube over the shared BET: the FACING is the
	 * front (toward the placer, the vanilla furnace convention = the upstream
	 * getSideForPlayerPlacing arm), the kind arms dispatch off the row. The half-height
	 * crate shape rides the BOTTLECRATE kind (upstream the 6/16 box).
	 */
	public static final class GT6StorageBlock extends gregtech6.block.GTEntityBlock {

		/** Facing property — the FRONT face, horizontals only (upstream SIDES_HORIZONTAL valid sides). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		/** The crate half-height box (upstream PX_P[0]..PX_P[6] = 0..6/16). */
		private static final VoxelShape CRATE_SHAPE = Block.box(0, 0, 0, 16, 6, 16);

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

		/** The crate is a 6/16 slab-like crate (upstream the collision/selection boxes). */
		@Override
		public VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return mRow.kind() == Kind.BOTTLECRATE ? CRATE_SHAPE : super.getShape(aState, aLevel, aPos, aContext);
		}

		@Override
		public VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return mRow.kind() == Kind.BOTTLECRATE ? CRATE_SHAPE : super.getCollisionShape(aState, aLevel, aPos, aContext);
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
		 * The break face — every kind pops its inventory (upstream canDrop = T everywhere
		 * but the crate); the crate drops ONE BlockItem carrying the BlockEntityTag (the
		 * keepSlot fold, the vanilla shulker convention).
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
				} else if (tBE instanceof GT6StaticStorageBaseBlockEntity tStorage) {
					for (int i = 0, l = tStorage.getInventory().getSlots(); i < l; i++) {
						ItemStack tStack = tStorage.getInventory().getStackInSlot(i);
						if (!tStack.isEmpty()) {
							Block.popResource(aLevel, aPos, tStack);
						}
					}
					aLevel.updateNeighbourForOutputSignal(aPos, this);
				}
			}
			super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
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
	 * The tab walk (task p38-tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family
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
