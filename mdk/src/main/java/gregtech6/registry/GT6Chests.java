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
import net.minecraft.world.level.block.Block;
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

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.inventories.GT6ChestBlockEntity;

/**
 * The metal-chest family registration home (task material-mc-a-storage-chests — the
 * research.material-coverage-census mc-A head, the GT6Hoppers self-contained-DR row
 * form): 120 blocks/items over ONE shared BET (the row config rides the block carrier),
 * the metalset() chest pair (Loader_MultiTileEntities.java:132-133) over the 60-material
 * loop :186-245 — the two absent families the census quantified first.
 *
 * <p><b>The material table is the metalset line table</b> — reused from
 * {@link GT6Hoppers#MATERIALS} verbatim (aID, MT local, aHardness; aHardness == aResistance
 * on all 60 lines), NOT re-declared: one loader line feeds both families, the mdh-6
 * family gate ({@code registers()}) and the setLocal display words live in one place.
 * Per loader line TWO rows expand — the plain metal chest (:132, id 0+aID) then the
 * reinforced wooden chest (:133, id 500+aID); 60 × 2 = 120.
 *
 * <p>The upstream registration columns per line:
 * <ul>
 * <li>Chest (:132) — tab 32745, NBT_INV_SIZE 54, NBT_TEXTURE "metalchest", hardness ==
 *     resistance == aHardness, block family {@code aMetal} = TOOL_pickaxe;</li>
 * <li>Reinforced wooden Chest (:133) — same tab/inv size, NBT_TEXTURE "woodchest",
 *     hardness = resistance = aHardness/2, block family {@code aWooden} = TOOL_axe, the
 *     NBT_FLAMMABILITY 100 column folds (the static-storage wooden-kinds fold — the port
 *     carries no flammability layer, GT6StaticStorages class doc).</li>
 * </ul>
 * Both rows share the {@code MultiTileEntityChest} class and tool quality 0.
 *
 * <p>Creative tab: the upstream "Chests" category (tab 32745, :132-133) pools into the
 * MACHINES_TAB join (task tabfix-b-energy, {@link #onBuildTabContents}; the
 * GTBarrels:257 pooling precedent). Placement faces the placer (the upstream
 * getSideForPlayerPlacing SIDES_HORIZONTAL arm — the static-storage convention). The
 * harvest bands ride the block families verbatim: the plain chest joins
 * mineable/pickaxe (aMetal), the reinforced chest mineable/axe (aWooden) — the task
 * brief's "金属=wrench" is corrected by the loader line itself (:98/:101 vs :132-133;
 * the wrench belongs to the aMachine massstorage pair, the mc-B card).
 *
 * <p>Declared folds (the {@code GTExampleChestBlockEntity} omissions carry over): the
 * lid-animation TESR (the blockstate JSON two-layer facade renders static), the
 * dungeon-loot creative variants (the metalset rows never carry gt.dungeonloot — the
 * loot-chest row :151 is the storages() domain, not this card), the trapped-chest
 * redstone arm (mIsTrapped never set by any metalset row) and the 1200-tick opener
 * resync (no TESR consumer).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Chests {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The loader meta id bases (:132 id 0+aID, :133 id 500+aID). */
	public static final int META_ID_BASE = 0;
	public static final int META_ID_BASE_REINFORCED = 500;

	/** The chest inventory size — the NBT_INV_SIZE column, 54 on both loader lines. */
	public static final int INVENTORY_SIZE = 54;

	/** The composed display templates "{@code %s Chest}" / "{@code %s reinforced wooden Chest}" (the p20 compose ruling; zh dump gt.multitileentity.0 铅箱子 / .500 铅强化木箱). */
	public static final String DISPLAY_KEY = "gt6.row.chest.display";
	public static final String DISPLAY_REINFORCED_KEY = "gt6.row.reinforced_chest.display";

	/** The row's material small-unit key — the SHARED metalset unit table (gt6.row.mat.*, the hopper walk emits the 60 words). */
	public static String matUnitKeyOf(gregtech6.registry.GT6Hoppers.HopperMaterial aMaterial) {
		return "gt6.row.mat." + aMaterial.slug();
	}

	/** The composed name of a row (the pure compose seam, the {@code GT6Hoppers.displayOf} shape). */
	public static MutableComponent displayOf(ChestRow aRow) {
		return Component.translatable(aRow.reinforced() ? DISPLAY_REINFORCED_KEY : DISPLAY_KEY,
				Component.translatable(matUnitKeyOf(aRow.material())));
	}

	/**
	 * One registration row — the block-carrier projection of one metalset chest-pair half
	 * (Loader :132 plain / :133 reinforced).
	 *
	 * @param path       the gt6 registry path (the blockstate/model/lang key tail)
	 * @param metaId     the upstream MultiTileEntity id (0+aID / 500+aID)
	 * @param material   the metalset line (the shared {@link GT6Hoppers#MATERIALS} record;
	 *                   hardness == resistance on every line, aHardness/2 on the reinforced half)
	 * @param reinforced the :133 reinforced kind flag (the wooden chest — the axe band, the
	 *                   half-hardness column)
	 */
	public record ChestRow(String path, int metaId, gregtech6.registry.GT6Hoppers.HopperMaterial material, boolean reinforced) {
		/** The block properties — aHardness on the plain half (:132), aHardness/2 on the reinforced half (:133); the METAL/WOOD sound split rides the block family (aMetal/aWooden). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness() / (reinforced ? 2 : 1), material.hardness() / (reinforced ? 2 : 1))
					.sound(reinforced ? SoundType.WOOD : SoundType.METAL);
		}
	}

	/**
	 * The 120 rows in registration order (the metalset loop walk — per loader line the
	 * plain chest :132 then the reinforced chest :133, the upstream pair order; the
	 * mdh-6 family gate skips both halves of an ABSENT domain's line, the hopper
	 * buildRows shape).
	 */
	public static final List<ChestRow> ROWS = buildRows();

	private static List<ChestRow> buildRows() {
		List<ChestRow> rRows = new ArrayList<>(GT6Hoppers.MATERIALS.size() * 2);
		for (gregtech6.registry.GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
			if (!tMat.registers()) continue; // the mdh-6 family gate
			rRows.add(new ChestRow("chest_" + tMat.slug(), META_ID_BASE + tMat.metaId(), tMat, false));
			rRows.add(new ChestRow("reinforced_chest_" + tMat.slug(), META_ID_BASE_REINFORCED + tMat.metaId(), tMat, true));
		}
		return List.copyOf(rRows);
	}

	/** The registered blocks by path (the BET multi-mount array + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6ChestBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (ChestRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6ChestBlock(tRow, tRow.properties())));
			// the GT6Kinetics.STEAM_ENGINE_ITEMS qualified-read forward-reference form (the P6 lambda lesson)
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Chests.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** All 120 chest blocks in registration order — both kinds are NBT_MATERIAL rows (the BET multi-mount array + the tint wrap walk). */
	public static Block[] blockArray() {
		Block[] rBlocks = new Block[ROWS.size()];
		for (int i = 0; i < ROWS.size(); i++) {
			rBlocks[i] = BLOCKS_BY_PATH.get(ROWS.get(i).path()).get();
		}
		return rBlocks;
	}

	/** The lookup for a data-driven place arm — null for an unknown path. */
	@Nullable
	public static Block blockByPath(String aPath) {
		RegistryObject<GT6ChestBlock> tHandle = BLOCKS_BY_PATH.get(aPath);
		return tHandle == null ? null : tHandle.get();
	}

	// -------------------------------------------------------------------------
	// the block carrier — the front-facing chest box
	// -------------------------------------------------------------------------

	/**
	 * The chest block — the front-face chest box over the shared BET: the FACING is the
	 * front (toward the placer, the vanilla furnace convention = the upstream
	 * getSideForPlayerPlacing SIDES_HORIZONTAL arm), the row (kind/material) rides the
	 * instance. The geometry is the vanilla chest box (1,0,1)-(15,14,15 — the upstream
	 * collision/selection columns, MultiTileEntityChest.java:308-311 the same 0.0625/
	 * 0.875 form). The use() arms are the upstream tool face ported onto the in-repo
	 * dispatch: the WRENCH rotates the front (onToolClick :173-181, horizontal faces),
	 * the PINCERS drains the chest into the player inventory (:182-229 the five-pass
	 * order), and with no tool claiming the click the open arm answers — the upstream
	 * onBlockActivated2 :234-240 (the blocked-above gate + openGUI) re-expressed as the
	 * MUI factory chain, zero MenuType (the wave-4 storage ruling).
	 */
	public static final class GT6ChestBlock extends GTEntityBlock {

		/** Facing property — the FRONT face, horizontals only (upstream SIDES_HORIZONTAL). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		/** The vanilla chest box — the upstream collision/selection columns verbatim (:308-311). */
		public static final VoxelShape CHEST_SHAPE = Block.box(1, 0, 1, 15, 14, 15);

		private final ChestRow mRow;

		public GT6ChestBlock(ChestRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the GT6HopperBlock simpleCodec form —
		// a parse-time representative value; world save/load never runs through this codec).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6ChestBlock> codec() {
			return simpleCodec(aProperties -> new GT6ChestBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the block-carrier config read). */
		public ChestRow row() {
			return mRow;
		}

		/** The composed chest name (the {@link GT6Chests#displayOf} carrier). */
		@Override
		public MutableComponent getName() {
			return displayOf(mRow);
		}

		/**
		 * The storage-domain material dispatch (the {@code GT6StorageBlock.materialOf}
		 * mirror shape): both kinds carry the loader NBT_MATERIAL column (the tint seat —
		 * upstream IItemColorableRGB on the shared MTE class), every non-chest block is
		 * null here.
		 */
		@Nullable
		public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
			if (!(aBlock instanceof GT6ChestBlock tChest)) return null;
			return tChest.row().material().mt();
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			// the front faces the placer (the vanilla furnace arm = the upstream
			// getSideForPlayerPlacing SIDES_HORIZONTAL branch)
			return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection().getOpposite());
		}

		@Override
		public VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return CHEST_SHAPE;
		}

		@Override
		public VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return CHEST_SHAPE;
		}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.CHEST_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// keep the BE's NBT fallback byte in step (the live truth is the state property)
			if (aLevel.getBlockEntity(aPos) instanceof GT6ChestBlockEntity tChest) {
				tChest.setFacingNbtFallback((byte) aState.getValue(FACING).get3DDataValue());
			}
		}

		/**
		 * The upstream tool face + open arm (onToolClick :171-231 / onBlockActivated2
		 * :234-240): the WRENCH rotates the front to the clicked horizontal face (the
		 * getSideWrenching arm — the vertical-face hits fold to a no-op, the declared
		 * face-fold), the PINCERS drains the chest into the player inventory in the
		 * five-pass order (the :182-229 loop, BE seam), and the unclaimed click opens the
		 * 54-grid MUI behind the blocked-above gate (the vanilla ChestBlock.isBlockedChestByBlock
		 * idiom, the {@code GTExampleChestBlock} shape).
		 */
		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (the GT6HopperBlock fork verbatim)
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			ItemStack tStack = aPlayer.getItemInHand(aHand);
			BlockEntity tTile = aLevel.getBlockEntity(aPos);
			if (!(tTile instanceof GT6ChestBlockEntity tChest)) {
				return InteractionResult.PASS;
			}
			if (tStack.is(gregtech6.datagen.GT6ItemTags.TOOLS_WRENCH)) {
				Direction tFace = aHit.getDirection();
				if (tFace.getAxis() != Direction.Axis.Y) {
					if (!aLevel.isClientSide) {
						aLevel.setBlock(aPos, aState.setValue(FACING, tFace), 3); // the :174-179 facing write
					}
					return InteractionResult.CONSUME;
				}
			}
			if (tStack.is(gregtech6.datagen.GT6ItemTags.TOOLS_PINCERS)) {
				if (!aLevel.isClientSide) {
					int tMoved = tChest.pincersTransfer(aPlayer.getInventory());
					if (tMoved > 0) {
						playCollectSound(aLevel, aPos); // the SFX.MC_COLLECT arm :226
					}
				}
				return InteractionResult.CONSUME;
			}
			// the GUI open arm (upstream :234-240 — the blocked-above gate rides first)
			if (!aLevel.isClientSide && aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer
					&& !isBlockedAbove(aLevel, aPos)) {
				gregtech6.gui.machines.GT6MuiMachine.tryOpen(tServerPlayer, tChest);
			}
			return InteractionResult.CONSUME; // upstream :240 return T — the click is consumed
		}

		/** Upstream :235 blocked-above check in the ChestBlock.java:323-326 idiom (the GTExampleChestBlock shape). */
		private static boolean isBlockedAbove(Level aLevel, BlockPos aPos) {
			BlockPos tAbove = aPos.above();
			return aLevel.getBlockState(tAbove).isRedstoneConductor(aLevel, tAbove);
		}

		/** The collect SFX (upstream :226 UT.Sounds.send(SFX.MC_COLLECT) — the level-less offline seam keeps it live-only). */
		private static void playCollectSound(Level aLevel, BlockPos aPos) {
			aLevel.playSound(null, aPos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
					net.minecraft.sounds.SoundSource.BLOCKS, 0.2F,
					(float) ((aLevel.random.nextDouble() * 0.7 + 0.3) * 0.7)); // the vanilla ItemEntity pop shape
		}

		@Override
		public boolean hasAnalogOutputSignal(BlockState aState) {
			return true; // the comparator tail drives (the GT6SandwichBlock face)
		}

		/** The comparator signal (the {@link #comparatorSignal} seam — public for the offline pin). */
		@Override
		public int getAnalogOutputSignal(BlockState aState, Level aLevel, BlockPos aPos) {
			if (!(aLevel.getBlockEntity(aPos) instanceof GT6ChestBlockEntity tChest)) return 0;
			return comparatorSignal(tChest.getInventory());
		}

		/**
		 * The comparator fullness formula (upstream getComparatorInputOverride :256 = the
		 * vanilla Container.calcRedstoneFromInventory identity): per-slot count/64, the
		 * mean fullness × 14, +1 when any slot is occupied — public for the offline pin
		 * (the empty/partial/full rungs).
		 */
		public static int comparatorSignal(net.minecraftforge.items.IItemHandler aInventory) {
			float tSum = 0;
			int tOccupied = 0;
			for (int i = 0, l = aInventory.getSlots(); i < l; i++) {
				ItemStack tStack = aInventory.getStackInSlot(i);
				if (!tStack.isEmpty()) {
					tSum += tStack.getCount() / (float) Math.min(64, tStack.getMaxStackSize());
					tOccupied++;
				}
			}
			if (tOccupied == 0) return 0;
			return (int) (tSum / aInventory.getSlots() * 14.0F) + 1;
		}

		/**
		 * The vanilla hopper onRemove comparator tail — the content pops ride the
		 * {@code GTEntityBlock} fallback (the same canDrop-everything contract, upstream
		 * :250 canDrop == T).
		 */
		@Override
		public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
			if (!aOldState.is(aNewState.getBlock())) {
				aLevel.updateNeighbourForOutputSignal(aPos, this);
			}
			super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
		}
	}

	private GT6Chests() {}

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
	 * The tab walk (task tabfix-b-energy — the whole {@link #ITEMS_BY_PATH} family joins
	 * the machines tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the
	 * class-level MOD-bus {@code @Mod.EventBusSubscriber} at the class head is what
	 * delivers this handler). Pool-cut declaration: upstream gives the family its own
	 * "Chests" category (tab 32745, Loader :132-133); this port pools the join into
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
