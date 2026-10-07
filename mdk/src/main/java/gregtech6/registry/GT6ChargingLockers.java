package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
import gregtech6.tileentity.inventories.GT6ChargingLockerBlockEntity;
import gregtech6.tileentity.inventories.GT6LockerBlockEntity;
import gregtech6.tileentity.inventories.GT6StaticStorageBaseBlockEntity;

/**
 * The Charging Locker family (task block-family-32xxx-port) — the full 60-material
 * metalset parameterized ladder of Loader_MultiTileEntities.java:139 ("Charging Locker
 * (Mat)", id 7500+aID, item id 32751, MultiTileEntityLockerCharging, aMachine): the
 * energy-injecting twin of the plain Locker, ported here because the census pool note
 * demanded the 32xxx domain ride the four-face chain (research.material-coverage-census
 * gap "Charging Locker n=60 m=0").
 *
 * <p><b>The material table is the GT6Hoppers one</b> — :139 rides the SAME
 * metalset() loop :186-245 as the hopper pair (:145-146), verbatim per-line columns
 * (aID, MT local, aHardness == aResistance), so {@link GT6Hoppers#MATERIALS} is the
 * single source: this family re-uses the 60 records verbatim (slug/display/hardness/
 * aID) plus their mdh-6 {@code registers()} family gate, and grows/shrinks with the
 * hopper table. Zero second copy of the 60-line ladder exists.
 *
 * <p><b>The rows</b>: one block per metalset line, path {@code charging_locker_<slug>},
 * meta id 7500+aID. Upstream columns carried: NBT_MATERIAL (the tint default — the
 * registration derives NBT_COLOR = fRGBaSolid, MultiTileEntityClassContainer.java:51,
 * the act-charging-table precedent), NBT_HARDNESS/NBT_RESISTANCE = the row hardness,
 * the 4-slot armor locker inventory (the class inherits MultiTileEntityLocker's
 * getDefaultInventory = 4 — the plain locker BE base re-used).
 *
 * <p><b>RECIPE RULING A (the coordinator 2026-10-07)</b>: the upstream recipe
 * "WCW"/"WMW"/"WCW" keys 'M' = {@code aRegistry.getItem(7300+aID)} — the SAME-material
 * plain Locker. The port plain Locker is the storage-static-batch two-anchor fold
 * (Bronze/Steel only), so exactly 2 of the 60 rows have a craftable 'M' column; the
 * other 58 recipes stay UNGENERATED (the dependency chain is recorded: the
 * plain-Locker 60-material ladder is the mc-A1 container-domain card, whose SPEC must
 * backfill these recipes). No material-compensating substitute is fabricated.
 *
 * <p>Texture chain (the census four-face clause): the borrowed upstream
 * {@code machines/lockers/charging} colored/overlay groups (MultiTileEntityLockerCharging
 * .java:70-82 — 10 PNGs, the front/back distinct set) render as the storageModel
 * faceted cube with the tintindex-0 body seat (the colored × mRGBa pass :62-66), the
 * overlay decals untinted (the P22 contract). The :65 GLOWING flag folds — no port
 * fullbright-texture seat exists (the declared fold, the port texture pipeline has no
 * emissive layer). The stateRunning visual payload (upstream :56-59) folds with it —
 * no active texture variant ships in the upstream charging group, the states feed only
 * the 1.7.10 comparator surface the port does not carry.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6ChargingLockers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The loader meta id base (:139 id 7500+aID). */
	public static final int META_ID_BASE = 7500;

	/** The composed display template "{@code Charging Locker (%s)}" (the upstream name column verbatim). */
	public static final String DISPLAY_KEY = "gt6.row.charging_locker.display";

	/** The row's material small-unit key — the SHARED hopper walk key (the dedup face). */
	public static String matUnitKeyOf(ChargingLockerRow aRow) {
		return "gt6.row.mat." + aRow.material().slug();
	}

	/** The composed name of a row (the GT6Hoppers.displayOf shape). */
	public static MutableComponent displayOf(ChargingLockerRow aRow) {
		return net.minecraft.network.chat.Component.translatable(DISPLAY_KEY,
				net.minecraft.network.chat.Component.translatable(matUnitKeyOf(aRow)));
	}

	/**
	 * One registration row — the block-carrier projection of one metalset line :139.
	 *
	 * @param path     the gt6 registry path ({@code charging_locker_<slug>})
	 * @param metaId   the upstream MultiTileEntity id (7500+aID)
	 * @param material the shared metalset record (slug/display/hardness/loader aID)
	 */
	public record ChargingLockerRow(String path, int metaId, GT6Hoppers.HopperMaterial material) {
		/** The block properties (hardness == resistance on every metalset line; the METAL sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of()
					.strength(material.hardness(), material.hardness())
					.sound(SoundType.METAL);
		}
	}

	/**
	 * The 60 rows in registration order (the metalset loop walk :186-245 — the hopper
	 * table's order verbatim). The mdh-6 family gate filters exactly like the hopper walk
	 * (one gate per loader line covers both families).
	 */
	public static final List<ChargingLockerRow> ROWS = buildRows();

	private static List<ChargingLockerRow> buildRows() {
		List<ChargingLockerRow> rRows = new ArrayList<>(GT6Hoppers.MATERIALS.size());
		for (GT6Hoppers.HopperMaterial tMat : GT6Hoppers.MATERIALS) {
			if (!tMat.registers()) continue; // the mdh-6 family gate (the hopper walk verbatim)
			rRows.add(new ChargingLockerRow("charging_locker_" + tMat.slug(), META_ID_BASE + tMat.metaId(), tMat));
		}
		return List.copyOf(rRows);
	}

	/** The registered blocks by path (the BET multi-mount array + the datagen walkers). */
	public static final Map<String, RegistryObject<GT6ChargingLockerBlock>> BLOCKS_BY_PATH = new LinkedHashMap<>();

	/** The registered items, same keys as {@link #BLOCKS_BY_PATH}. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (ChargingLockerRow tRow : ROWS) {
			BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GT6ChargingLockerBlock(tRow, tRow.properties())));
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6ChargingLockers.BLOCKS_BY_PATH.get(tRow.path()).get(), new Item.Properties())));
		}
	}

	/** The family blocks in registration order (the tint-walk + BET multi-mount payload). */
	public static Block[] blockArray() {
		List<Block> rBlocks = new ArrayList<>();
		for (ChargingLockerRow tRow : ROWS) {
			rBlocks.add(BLOCKS_BY_PATH.get(tRow.path()).get());
		}
		return rBlocks.toArray(new Block[0]);
	}

	// -------------------------------------------------------------------------
	// the block carrier — the front-face cube over the shared BET
	// -------------------------------------------------------------------------

	/**
	 * The charging locker block — the GT6StorageBlock front-face posture (the placer-facing
	 * FRONT) with the row riding the instance. The use arm is the INHERITED plain-locker
	 * armor swap (upstream MultiTileEntityLockerCharging overrides no interaction arm —
	 * onBlockActivated3 :58-79 comes with the superclass), re-using the static-storage
	 * swap walker.
	 */
	public static final class GT6ChargingLockerBlock extends GTEntityBlock {

		/** Facing property — the FRONT face, horizontals only (the GT6StorageBlock convention). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		private final ChargingLockerRow mRow;

		public GT6ChargingLockerBlock(ChargingLockerRow aRow, Properties aProperties) {
			super(aProperties);
			mRow = aRow;
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
		}
		//? if neoforge {
		/*
		// 21.1: the GT6HopperBlock simpleCodec form — a parse-time representative value.
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6ChargingLockerBlock> codec() {
			return simpleCodec(aProperties -> new GT6ChargingLockerBlock(ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the block-carrier config read; the materialOf payload). */
		public ChargingLockerRow row() {
			return mRow;
		}

		/** The composed locker name (the {@link GT6ChargingLockers#displayOf} carrier). */
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
			return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection().getOpposite());
		}

		@Override
		protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.CHARGING_LOCKER_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// keep the BE's NBT fallback byte in step (the live truth is the state property)
			if (aLevel.getBlockEntity(aPos) instanceof GT6StaticStorageBaseBlockEntity tStorage) {
				tStorage.setFacingNbtFallback((byte) aState.getValue(FACING).get3DDataValue());
			}
		}

		/**
		 * The inherited locker armor-swap arm (upstream MultiTileEntityLocker :58-79 through
		 * the charging subclass, front face only) — the GT6StorageBlock.LOCKER arm verbatim
		 * over the shared swap walker.
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
			if (!(tTile instanceof GT6LockerBlockEntity tLocker)) return InteractionResult.PASS;
			if (aHit.getDirection() != aState.getValue(FACING)) return InteractionResult.PASS;
			// the swap body is package-private on GT6StaticStorages — re-walked here (the same
			// upstream :58-79 loop; a public seam would touch the shared file for one call)
			if (!aLevel.isClientSide) {
				boolean tAny = false;
				for (int i = 0; i < GT6LockerBlockEntity.INVENTORY_SIZE; i++) {
					ItemStack tSlotStack = tLocker.getInventory().getStackInSlot(i);
					if (tSlotStack.isEmpty() || GT6LockerBlockEntity.isValidArmorForSlot(tSlotStack, i)) {
						tLocker.getInventory().setStackInSlot(i, aPlayer.getInventory().armor.set(i, tSlotStack));
						tAny = true;
					}
				}
				if (tAny) {
					aPlayer.getInventory().setChanged();
					tLocker.updateInventory();
				}
			}
			return InteractionResult.CONSUME;
		}

		/** The vanilla content-pop + explosion faces (the GT6HopperBlock form). */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3); // the IForgeBlock default body
		}
	}

	private GT6ChargingLockers() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Hoppers.onModConstruct form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: the GT6Hoppers fork verbatim.
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
	}

	/**
	 * The tab walk (the GT6Hoppers form): the upstream category "Storage" (tab 32751,
	 * Loader :139) pools into MACHINES_TAB (the storage-static-batch pooling precedent).
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
