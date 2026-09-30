package gregtech6.registry;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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

import gregtech6.block.GTEntityBlock;
import gregtech6.reactor.IReactorRodItem;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.reactors.GT6ReactorCore2x2BlockEntity;

/**
 * The 2x2 Nuclear Reactor Core registration home (task debt-reactor-b-2x2-be, the
 * GT6HeatExchangers self-contained-DR form, ADR-P3-4): ONE block + item + BET over the
 * upstream registration line Loader_MultiTileEntities.java:738 verbatim —
 * "Nuclear Reactor Core (2x2)", category "Reactors", MTE id 9200 over the shared 9200
 * family block, material Pb (tool quality 6/hardness 10/resistance 10). The 1x1 core
 * row (:734) rides the upstream default config OFF and stays deferred (the ruling).
 *
 * <p>Creative tab: upstream gives the family its own "Reactors" category — this port
 * pools the join into MACHINES_TAB (the GT6Sensors.onBuildTabContents pooling
 * precedent). The crafting row ("PCP"/"CMC"/"PCP", dense lead casing + circuit 5 +
 * piston) and the 46 rod ITEMS are the C card's face; this block is placed by the tab
 * and the RCON seat until then. The KJS face is declared: registration + datapack
 * smoke only, no KubeJS surface.
 *
 * <p>The block carries the world faces the upstream base/2x2 pair mounted on the MTE:
 * the 1-px-inset collision box (Core:304, {@code box(PX_P[1..], PX_N[1..])} — the rods
 * stand inside the shell), the entity-inside heat+radiation contact face (Core:303,
 * running only) and the top-face hand-insert of a rod (Core2x2:280-294, the quadrant
 * pick {@code hitX/hitZ < 0.5}). The 11-pass rod/fluid render stack (Core2x2:320-390)
 * is the render pool — the blockstate is the flat cube over the borrowed upstream
 * faces (the datagen entry).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Reactors {

	/** One registration row — the :738 line's block-carrier projection. */
	public record ReactorRow(String path, int legacyId, float hardness) {
		/** The block properties (hardness == resistance; the METAL machine sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of().strength(hardness, hardness).sound(net.minecraft.world.level.block.SoundType.METAL);
		}
	}

	/** The single row (:738 — Pb, hardness/resistance 10.0F). */
	public static final ReactorRow REACTOR_CORE_2X2_ROW = new ReactorRow("nuclear_reactor_core_2x2", 9200, 10.0F);

	// the DeferredRegister trio (the GT6HeatExchangers form)
	public static final DeferredRegister<Block> BLOCKS_REG = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The block carrier — the GTSensorBlock shape (no facing property: the upstream
	 * primary/secondary facings are BE-only NBT, the texture routing over them rides
	 * the render pool).
	 */
	public static final class ReactorCoreBlock extends GTEntityBlock {

		/** Core:304 — the 1-px-inset collision box (the shell the rods stand inside). */
		public static final VoxelShape REACTOR_SHELL = Block.box(1, 1, 1, 15, 15, 15);

		public ReactorCoreBlock(Properties aProperties) {
			super(aProperties);
		}

		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the GTSensorBlock fork shape).
		@Override
		protected com.mojang.serialization.MapCodec<? extends ReactorCoreBlock> codec() {
			return simpleCodec(ReactorCoreBlock::new);
		}
		*///?}

		/**
		 * The reactor-domain material dispatch (task tint-coverage-batch, the
		 * {@code GTBarrelBlock.materialOf} mirror shape): the single :738 row is its own
		 * carrier — the one-block domain over a fixed NBT_MATERIAL Pb column needs no
		 * Supplier ctor (the {@code GTMultiBlockControllerBlock} "the row IS the carrier"
		 * form). Resolved at tint time (never class-load, the bySlug ruling). The DYNAMIC
		 * face (the facing-dependent texture routing + the 11-pass rod/fluid render stack,
		 * Core2x2:320-390) stays with the rod-render-pool card — this arm tints the static
		 * cube body only, and the GTMachineTintModel wrap guard skips the dynamic model
		 * once that card lands its own seat.
		 */
		@javax.annotation.Nullable
		public static gregapi.oredict.OreDictMaterial materialOf(@javax.annotation.Nullable net.minecraft.world.level.block.Block aBlock) {
			return aBlock instanceof ReactorCoreBlock ? gregapi.data.MT.Pb : null;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			// no properties — the running/stop visual rides the render pool
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return REACTOR_CORE_2X2_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
			return REACTOR_SHELL; // Core:304
		}

		/** Core:303 — the entity-inside contact face (heat 5 + radioactivity(3,1) while running). */
		@Override
		public void entityInside(BlockState aState, Level aLevel, BlockPos aPos, net.minecraft.world.entity.Entity aEntity) {
			if (!aLevel.isClientSide() && aEntity instanceof LivingEntity tLiving
					&& aLevel.getBlockEntity(aPos) instanceof GT6ReactorCore2x2BlockEntity tCore) {
				tCore.onEntityCollided(tLiving);
			}
		}

		/** Core2x2:284 — the quadrant pick of the top-face hit. */
		public static int slotOfHit(double aHitX, double aHitZ) {
			return aHitX < 0.5 ? aHitZ < 0.5 ? 0 : 1 : aHitZ < 0.5 ? 2 : 3;
		}

		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem (the GTSensorBlock fork).
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
			// Core2x2:281-293 — the top-face hand-insert of a rod
			if (aHit.getDirection() == Direction.UP
					&& aLevel.getBlockEntity(aPos) instanceof GT6ReactorCore2x2BlockEntity tCore) {
				ItemStack tHeld = aPlayer.getItemInHand(aHand);
				if (tHeld.getItem() instanceof IReactorRodItem) {
					int tSlot = slotOfHit(aHit.getLocation().x - aPos.getX(), aHit.getLocation().z - aPos.getZ());
					if (tCore.handInsertRod(tSlot, tHeld)) {
						if (!aPlayer.getAbilities().instabuild) tHeld.shrink(1); // ST.use
						return InteractionResult.CONSUME;
					}
				}
			}
			return InteractionResult.PASS;
		}
	}

	public static final RegistryObject<ReactorCoreBlock> REACTOR_CORE_2X2_BLOCK =
			BLOCKS_REG.register(REACTOR_CORE_2X2_ROW.path(), () -> new ReactorCoreBlock(REACTOR_CORE_2X2_ROW.properties()));

	public static final RegistryObject<Item> REACTOR_CORE_2X2_ITEM =
			ITEMS.register(REACTOR_CORE_2X2_ROW.path(), () -> new BlockItem(REACTOR_CORE_2X2_BLOCK.get(), new Item.Properties()));

	/**
	 * The core BET: one BE class over its one block (the CokeOven degenerate shape).
	 * Registry path mirrors {@link GT6ReactorCore2x2BlockEntity#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<GT6ReactorCore2x2BlockEntity>> REACTOR_CORE_2X2_BE =
			BLOCK_ENTITY_TYPES.register("reactor_core_2x2", () -> BlockEntityType.Builder.of(
					GT6ReactorCore2x2BlockEntity::new, REACTOR_CORE_2X2_BLOCK.get()).build(null));

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Attachments shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Attachments fork).
		 *///?}
		BLOCKS_REG.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
		ITEMS.register(tModBus);
	}

	/** The tab walk (the GT6Sensors.onBuildTabContents pooling precedent — "Reactors" pools into MACHINES_TAB). */
	@SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			aEvent.accept(new ItemStack(REACTOR_CORE_2X2_ITEM.get()));
		}
	}
}
