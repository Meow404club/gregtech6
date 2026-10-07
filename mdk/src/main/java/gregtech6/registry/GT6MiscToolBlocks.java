package gregtech6.registry;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

//? if forge {
import net.minecraftforge.common.IPlantable;
//?} else {
/*import net.neoforged.neoforge.common.util.TriState;
 *///?}
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6PlantPotBlockEntity;
import gregtech6.tileentity.tools.GT6SapBagBlockEntity;

/**
 * The "Misc Tool Blocks" singles of the 32xxx domain (task block-family-32xxx-port):
 * the Resin/Sap Bag (Loader_MultiTileEntities.java:2221, meta 32736, item 32720,
 * MT.Leather, aUtilWool) and the Universal Plant Pot (:2229, meta 32065, MT.Ceramic,
 * aUtilStone) — the two non-parameterized families the census pool note named.
 *
 * <p><b>Sap Bag semantics</b> (MultiTileEntitySapBag.java, 200 lines): a 1-slot,
 * 8000 L single-tank bag that mounts FLAT on a tree trunk (the FACING points INTO the
 * tree, useInversePlacementRotation :188-189, horizontal faces only :192), no collision
 * (:151 — walk-through), the selection box per facing (:154-161), and on click hands the
 * collected stack to the player or fills the held fluid container from the tank
 * (:99-113). The visual FULL byte (:195-196 — tank or slot occupied) swaps the overlay
 * art to {@code overlay_full}.
 * <b>DECLARED CUT</b>: the collection tick (:80-95) walks
 * {@code ITileEntityTreeHole} — the tree-hole MTE domain (the Resin/Sap Hole rows
 * :2027-2029) is a LATER card (the GT6TreeFeature spec-④ cut, the resin-hole arm
 * declared deferred); the port bag keeps every self-contained face 1:1 and the
 * collection arm stays a declared no-op until that domain lands. The tank carries NO
 * automation capability — the upstream bag tank is a bare FluidTankGT outside the
 * Base08 capability chain (the drain view below exists only for the click arm).
 *
 * <p><b>Plant Pot semantics</b> (MultiTileEntityPlantPot.java, 94 lines): a paintable
 * two-pass pot (the top plate 0,10,0..16 + the body 1,0,1..15,10 :66-70) that sustains
 * any plant on its top face (:60 canSustainPlant), full-cube collision/selection (the
 * MultiTileEntityBlock :192/:247 defaults — the pot does NOT override either), tooltip
 * "Can grow any Plants ontop of it!" (:45), light opacity = water (:93), no GUI, no
 * inventory (the canDrop :92 override is vacuous on the inventory-less 07Paintable base).
 *
 * <p>Texture chains: sap bag = the borrowed {@code machines/tools/sapbag}
 * colored/overlay/overlay_full TBS trios (9 PNGs, :137-149), plant pot = the borrowed
 * {@code machines/plantpot} colored/overlay TBS trios (6 PNGs, :80-88) — the colored
 * bodies are the tintindex-0 seats (the colored × mRGBa passes :133/:76), the overlays
 * untinted (the P22 contract). Default colours: Leather / Ceramic (the NBT_MATERIAL
 * columns through the GTMachinePaintTint dispatch arms).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6MiscToolBlocks {

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/** The NBT_TANK_CAPACITY column (:2221, 8000 L). */
	public static final long SAP_BAG_TANK_CAPACITY = 8000;

	/** The plant-pot tooltip key (upstream :45 verbatim text, the Chat.CYAN face). */
	public static final String TOOLTIP_PLANT_POT = "gt6.tooltip.plantpot";
	/** The no-GUI tooltip key (upstream LH.NO_GUI_CLICK_TO_INVENTORY, LH.java:506 verbatim text, the Chat.ORANGE face). */
	public static final String TOOLTIP_NO_GUI = "gt6.tooltip.no_gui_click_inventory";

	public static final RegistryObject<GT6SapBagBlock> SAP_BAG_BLOCK =
			BLOCKS.register("sap_bag", () -> new GT6SapBagBlock(BlockBehaviour.Properties.of()
					.strength(0.5F, 3.0F) // the :2221 NBT_HARDNESS/NBT_RESISTANCE columns
					.sound(SoundType.WOOL) // the aUtilWool harvest column's material family
					.noOcclusion()));
	public static final RegistryObject<Item> SAP_BAG_ITEM =
			ITEMS.register("sap_bag", () -> new ToolBlockItem(SAP_BAG_BLOCK.get(), TOOLTIP_NO_GUI, ChatFormatting.GOLD));

	public static final RegistryObject<GT6PlantPotBlock> PLANT_POT_BLOCK =
			BLOCKS.register("plant_pot", () -> new GT6PlantPotBlock(BlockBehaviour.Properties.of()
					.strength(1.0F, 5.0F) // the :2229 NBT_HARDNESS/NBT_RESISTANCE columns
					.sound(SoundType.STONE) // the aUtilStone harvest column's material family
					.noOcclusion()));
	public static final RegistryObject<Item> PLANT_POT_ITEM =
			ITEMS.register("plant_pot", () -> new ToolBlockItem(PLANT_POT_BLOCK.get(), TOOLTIP_PLANT_POT, ChatFormatting.AQUA));

	/** The BlockItem with the one-line tooltip (the upstream addToolTips one-liner form). */
	private static final class ToolBlockItem extends BlockItem {
		private final String mTooltipKey;
		private final ChatFormatting mStyle;

		ToolBlockItem(Block aBlock, String aTooltipKey, ChatFormatting aStyle) {
			super(aBlock, new Item.Properties());
			mTooltipKey = aTooltipKey;
			mStyle = aStyle;
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			aTooltip.add(Component.translatable(mTooltipKey).withStyle(mStyle));
		}
		//?} else {
		/*// 21.1: the Level second parameter became Item.TooltipContext (the
		//GTLightningRodBlock fork verbatim, vanilla 1.21.1 Item.java:292).
		@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			aTooltip.add(Component.translatable(mTooltipKey).withStyle(mStyle));
		}
		*///?}
	}

	// -------------------------------------------------------------------------
	// the sap bag block carrier
	// -------------------------------------------------------------------------

	/**
	 * The sap-bag block — the facing box carrier over the shared BET: the FACING points
	 * INTO the mounted face (the inverse placement, horizontals only), the FULL property
	 * rides the BE's tank/slot recount (the visual data byte :195-196).
	 */
	public static final class GT6SapBagBlock extends GTEntityBlock {

		/** Facing property — INTO the mounted face, horizontals only (upstream :192 SIDES_HORIZONTAL). */
		public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

		/** The full flag (the visual data byte :195-196 — the overlay_full model swap). */
		public static final BooleanProperty FULL = BooleanProperty.create("full");

		/** The selection boxes per facing (upstream :154-161, PX_P[n]=n px / PX_N[n]=(16-n) px). */
		private static final VoxelShape SHAPE_NORTH = Block.box(5, 0, 0, 11, 7, 6);   // :156 the Z_NEG box
		private static final VoxelShape SHAPE_SOUTH = Block.box(5, 0, 10, 11, 7, 16); // :157 the Z_POS box
		private static final VoxelShape SHAPE_WEST  = Block.box(0, 0, 5, 6, 7, 11);   // :158 the X_NEG box
		private static final VoxelShape SHAPE_EAST  = Block.box(10, 0, 5, 16, 7, 11); // :159 the default (X_POS) box

		public GT6SapBagBlock(Properties aProperties) {
			super(aProperties);
			registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FULL, false));
		}
		//? if neoforge {
		/*
		// 21.1: the GT6HopperBlock simpleCodec form.
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6SapBagBlock> codec() {
			return simpleCodec(GT6SapBagBlock::new);
		}
		*///?}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(FACING, FULL);
		}

		/**
		 * The inverse placement (:188-189 useInversePlacementRotation — the 09FacingSingle
		 * form the hopper family carries): the bag points INTO the clicked face; only
		 * horizontal mounts are valid (:192) — a floor/ceiling click refuses placement.
		 */
		@Override
		public BlockState getStateForPlacement(BlockPlaceContext aContext) {
			Direction tFace = aContext.getClickedFace();
			if (!tFace.getAxis().isHorizontal()) return null;
			return defaultBlockState().setValue(FACING, tFace.getOpposite());
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.SAP_BAG_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL;
		}

		/** The facing box (upstream :154-161). */
		@Override
		public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return switch (aState.getValue(FACING)) {
				case NORTH -> SHAPE_NORTH;
				case SOUTH -> SHAPE_SOUTH;
				case WEST -> SHAPE_WEST;
				default -> SHAPE_EAST;
			};
		}

		/** Upstream :151 getCollisionBoundingBoxFromPool = null — the bag is walk-through. */
		@Override
		public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return Shapes.empty();
		}

		/**
		 * The upstream onBlockActivated3 :99-113 (server arms, the click always consumed):
		 * the collected stack hands out first, else the held container fills from the tank.
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
			if (!(tTile instanceof GT6SapBagBlockEntity tBag)) return InteractionResult.PASS;
			if (!aLevel.isClientSide) {
				ItemStack tSlot = tBag.getInventory().getStackInSlot(0);
				if (!tSlot.isEmpty()) { // :101-104 ST.add + slotKill
					aPlayer.getInventory().placeItemBackInInventory(tSlot);
					tBag.getInventory().setStackInSlot(0, ItemStack.EMPTY);
					tBag.recountFull();
					return InteractionResult.CONSUME;
				}
				ItemStack tHeld = aPlayer.getItemInHand(aHand);
				if (!tHeld.isEmpty()) { // :105-110 FL.fill(mTank, container) — the bag fills the container
					FluidActionResult tResult =
							FluidUtil.tryFillContainer(tHeld, tBag.drainView(), Integer.MAX_VALUE, null, true);
					if (tResult != null && tResult.isSuccess()) {
						tHeld.shrink(1);
						aPlayer.getInventory().placeItemBackInInventory(tResult.getResult());
						tBag.recountFull();
						return InteractionResult.CONSUME;
					}
				}
			}
			return InteractionResult.CONSUME; // :112 — consumed either way
		}

		/** The vanilla explosion face (the GT6HopperBlock form; the tank voids upstream :117). */
		@Override
		public void onBlockExploded(BlockState aState, Level aLevel, BlockPos aPos, Explosion aExplosion) {
			aLevel.setBlock(aPos, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	// -------------------------------------------------------------------------
	// the plant pot block carrier
	// -------------------------------------------------------------------------

	/**
	 * The plant-pot block — the two-element pot over the paint-only BET: full-cube
	 * collision/selection (the upstream MultiTileEntityBlock defaults, the class doc),
	 * the top face sustains any plant (:60).
	 */
	public static final class GT6PlantPotBlock extends GTEntityBlock {

		public GT6PlantPotBlock(Properties aProperties) {
			super(aProperties);
		}
		//? if neoforge {
		/*
		@Override
		protected com.mojang.serialization.MapCodec<? extends GT6PlantPotBlock> codec() {
			return simpleCodec(GT6PlantPotBlock::new);
		}
		*///?}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GTBlockEntities.PLANT_POT_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL;
		}

		/**
		 * Upstream :60 canSustainPlant — the top face only (any plant, the IMTE face).
		 * Per-leg fork: the 1.20.1 forge face carries the IPlantable (the IMTE :60
		 * translation), the 21.1 NeoForge face reshaped to the plant STATE with the
		 * TriState answer (the GTGrassBlock javadoc records the API removal).
		 */
		//? if forge {
		@Override
		public boolean canSustainPlant(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection, IPlantable aPlantable) {
			return aDirection == Direction.UP;
		}
		//?} else {
		/*@Override
		public net.neoforged.neoforge.common.util.TriState canSustainPlant(BlockState aState, BlockGetter aLevel, BlockPos aPos, Direction aDirection, BlockState aPlant) {
			return aDirection == Direction.UP ? net.neoforged.neoforge.common.util.TriState.TRUE
					: net.neoforged.neoforge.common.util.TriState.DEFAULT;
		}
		*///?}

		/** Upstream :93 getLightOpacity = LIGHT_OPACITY_WATER (= 1, the water value). */
		@Override
		public int getLightBlock(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
			return 1;
		}
	}

	private GT6MiscToolBlocks() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Hoppers form). */
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
	 * The tab walk: the upstream category "Misc Tool Blocks" (tab 32720, Loader :2221/:2229)
	 * pools into MACHINES_TAB (the storage-static-batch pooling precedent).
	 */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(SAP_BAG_ITEM.get()));
			aEvent.accept(new ItemStack(PLANT_POT_ITEM.get()));
		}
	}
}
