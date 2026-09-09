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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTComposedNameItem;
import gregtech6.block.GTEntityBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity;
import gregtech6.tileentity.inventories.GT6HopperBlockEntity;
import gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity;

/**
 * The storage-hopper family registration home (task p26-storage-hopper-family, the
 * GT6Boilers self-contained-DR row form): 4 blocks/items over TWO shared BETs — the two BE
 * classes {@link GT6HopperBlockEntity} / {@link GT6QueueHopperBlockEntity}, the row config
 * (slot count, material) riding the block carrier.
 *
 * <p><b>The rows</b> (Loader_MultiTileEntities.java — the metalset() hopper pair :145-146
 * over the material loop :186-245; first batch = the Bronze/Steel anchor rows of the
 * wave-4 arch ruling — research 'Iron(5)' was a census slip, the 60-material table has no
 * Iron row, the 5-slot anchor is Steel :202):
 * <ul>
 * <li>Bronze Hopper — id 8009 (:191, aHopperSize 3), NBT_INV_SIZE = max(1, 3) = 3,
 *     hardness = resistance = 7.0;</li>
 * <li>Steel Hopper — id 8010 (:202, aHopperSize 5), NBT_INV_SIZE = max(1, 5) = 5,
 *     hardness = resistance = 6.0;</li>
 * <li>Bronze Queue Hopper — id 8209 (:146 aID base 8200 + :191), NBT_INV_SIZE =
 *     max(2, 3) = 3;</li>
 * <li>Steel Queue Hopper — id 8210, NBT_INV_SIZE = max(2, 5) = 5.</li>
 * </ul>
 * The upstream registration columns: tool quality 0, weight 16, the aMachine block family,
 * the recipes "PwP"/"XCX"/" Xh" (hopper) and "PCP"/"XCX"/"wXh" (queue) over plate +
 * plateCurved + OD.craftingChest with the wrench/hammer tool letters (:145-146).
 *
 * <p>No creative tab (the boiler precedent — a tab row is a later append). The placement
 * face is the INVERSE clicked face (09FacingSingle.onPlaced :73 with
 * useInversePlacementRotation = T :257/:239 → OPOS[clickedFace], all six faces valid) —
 * the blockstate FACING property carries it.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Hoppers {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** One loader material anchor — slug + display word (the BoilerMaterial shape). */
	public record HopperMaterial(String slug, String display, float hardness, int metaId) {}

	/** The two first-batch materials (Loader :191 Bronze / :202 Steel). */
	public static final HopperMaterial MAT_BRONZE = new HopperMaterial("bronze", "Bronze", 7.0F, 9);
	public static final HopperMaterial MAT_STEEL = new HopperMaterial("steel", "Steel", 6.0F, 10);

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
	 * One registration row — the block-carrier projection of the metalset hopper pair line.
	 *
	 * @param path     the gt6 registry path (the blockstate/model/lang key tail)
	 * @param metaId   the upstream MultiTileEntity id (8009/8010/8209/8210)
	 * @param material the row material (slug/display/hardness/loader id)
	 * @param slots    the aHopperSize column (Bronze 3 / Steel 5)
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

	/** The four rows in registration order (hopper pair, then the queue pair). */
	public static final List<HopperRow> ROWS = List.of(
			new HopperRow("hopper_bronze", META_ID_BASE + MAT_BRONZE.metaId(), MAT_BRONZE, 3, false),
			new HopperRow("hopper_steel", META_ID_BASE + MAT_STEEL.metaId(), MAT_STEEL, 5, false),
			new HopperRow("queue_hopper_bronze", META_ID_BASE_QUEUE + MAT_BRONZE.metaId(), MAT_BRONZE, 3, true),
			new HopperRow("queue_hopper_steel", META_ID_BASE_QUEUE + MAT_STEEL.metaId(), MAT_STEEL, 5, true));

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

		/** The composed hopper name (task p20-i18n-compose-rows: the {@link GT6Hoppers#displayOf} carrier). */
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
		 * The vanilla HopperBlock.onRemove :157-167 shape ported onto the handler inventory:
		 * the canDrop-everything contract (upstream :250/:232) — every slot pops as a drop
		 * when the block is replaced, then the comparator neighbour refresh.
		 */
		@Override
		public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
			if (!aOldState.is(aNewState.getBlock())) {
				BlockEntity tBE = aLevel.getBlockEntity(aPos);
				if (tBE instanceof GT6HopperBaseBlockEntity tHopper) {
					for (int i = 0, l = tHopper.getInventory().getSlots(); i < l; i++) {
						ItemStack tStack = tHopper.getInventory().getStackInSlot(i);
						if (!tStack.isEmpty()) {
							net.minecraft.world.level.block.Block.popResource(aLevel, aPos, tStack);
						}
					}
					aLevel.updateNeighbourForOutputSignal(aPos, this);
				}
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
}
