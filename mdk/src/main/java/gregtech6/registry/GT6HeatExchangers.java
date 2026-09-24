package gregtech6.registry;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import gregtech6.block.GTEntityBlock;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.GT6HeatExchangerBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * The Large Heat Exchanger registration home (task p29-w3-heat-smelter, the GT6Boilers
 * self-contained-DR form, ADR-P3-4): ONE controller block + item + BET over the row —
 * the upstream registration line Loader_MultiTileEntities.java:1245 verbatim
 * ("Large Heat Exchanger", "Multiblock Machines", 17197, ANY.W, NBT_HARDNESS ==
 * NBT_RESISTANCE 6.0F, NBT_TEXTURE "largeheatexchanger", NBT_OUTPUT 16384, NBT_FUELMAP
 * FM.Hot, NBT_ENERGY_EMITTED TD.Energy.HU).
 *
 * <p>GT6HeatExchangers is a NEW self-contained registry class (the wave-plan
 * structural-conflict ruling: GTMachines.java belongs to this card's machine rows, this
 * file to the HEX — zero shared-file edits beyond the parallel-card appends).
 *
 * <p>Creative tab (task p38-tabfix-a-multiblock): the single item joins MULTIBLOCKS_TAB
 * via the {@link ModBusListener#onBuildTabContents} handler — registered-but-tab-less is
 * invisible in BOTH the creative menu and JEI (the BurningBoxes issue-#10 form,
 * superseding the old GT6Boilers no-tab precedent). Pool cut declared: upstream rode the
 * per-family "Multiblock Machines" creative tab (tab id 17101, Loader :1245); this port
 * pools the family into the gt6:multiblocks tab. The KJS face of the card is declared on
 * the BE class doc: registration + datapack smoke rows, no KubeJS surface.
 *
 * <p>The block carrier is the BoilerTankBlock shape minus the facing (the HEX structure
 * is facing-independent, the controller is the centre cell of both layers —
 * {@link GT6HeatExchangerBlockEntity#patternWalkFacing()} zero-offset): the FORMED
 * property is the only state (the base {@link TileEntityBase10MultiBlockBase#FORMED}
 * write; the formed-look visual stays the p9 pool, every state maps to the same model).
 */
public final class GT6HeatExchangers {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One registration row — the block-carrier projection of the :1245 line. */
	public record HeatExchangerRow(String path, int metaId, long outputHUPerTick, float hardness) {
		/** The block properties (hardness == resistance; the METAL machine sound). */
		public BlockBehaviour.Properties properties() {
			return BlockBehaviour.Properties.of().strength(hardness, hardness).sound(SoundType.METAL);
		}
	}

	/** The single row (:1245 — NBT_OUTPUT 16384 HU/t emission rate). */
	public static final HeatExchangerRow HEAT_EXCHANGER_ROW = new HeatExchangerRow("large_heat_exchanger", 17197, 16384, 6.0F);

	// the DeferredRegister trio (the GT6Boilers form)
	public static final net.minecraftforge.registries.DeferredRegister<Block> BLOCKS_REG =
			net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS, "gt6");
	public static final net.minecraftforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
			net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");
	public static final net.minecraftforge.registries.DeferredRegister<Item> ITEMS =
			net.minecraftforge.registries.DeferredRegister.create(Registries.ITEM, "gt6");

	/** The controller block class — FORMED is the only state (the base write; no facing). */
	public static final class HeatExchangerBlock extends GTEntityBlock {

		public HeatExchangerBlock(Properties aProperties) {
			super(aProperties);
			registerDefaultState(this.stateDefinition.any().setValue(TileEntityBase10MultiBlockBase.FORMED, false));
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch) — the BoilerTankBlock simpleCodec representative-value form.
		@Override
		protected com.mojang.serialization.MapCodec<? extends HeatExchangerBlock> codec() {
			return simpleCodec(aProperties -> new HeatExchangerBlock(aProperties));
		}
		*///?}

		/** The registration row (the BoilerRow carrier read). */
		public HeatExchangerRow row() {
			return HEAT_EXCHANGER_ROW;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
			aBuilder.add(TileEntityBase10MultiBlockBase.FORMED);
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return HEAT_EXCHANGER_BE.get();
		}

		@Override
		public RenderShape getRenderShape(BlockState aState) {
			return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
		}

		@Override
		public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
			super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
			// no facing (the structure is facing-independent) — nothing to mirror
		}
	}

	public static final RegistryObject<HeatExchangerBlock> HEAT_EXCHANGER_BLOCK =
			BLOCKS_REG.register(HEAT_EXCHANGER_ROW.path(), () -> new HeatExchangerBlock(HEAT_EXCHANGER_ROW.properties()));

	public static final RegistryObject<Item> HEAT_EXCHANGER_ITEM =
			ITEMS.register(HEAT_EXCHANGER_ROW.path(), () -> new BlockItem(HEAT_EXCHANGER_BLOCK.get(), new Item.Properties()));

	/**
	 * The HEX BET: one controller class over its one block (the CokeOven BET degenerate
	 * shape). Registry path mirrors {@link GT6HeatExchangerBlockEntity#getTileEntityName()}.
	 */
	public static final RegistryObject<BlockEntityType<GT6HeatExchangerBlockEntity>> HEAT_EXCHANGER_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_heat_exchanger", () -> BlockEntityType.Builder.of(
					GT6HeatExchangerBlockEntity::new, HEAT_EXCHANGER_BLOCK.get()).build(null));

	// -------------------------------------------------------------------------
	// the acceptance command /gt6heatexchanger (the GTBoilerCommand shape, scoped
	// INSIDE the self-contained registry file per the FILES_SCOPE ruling):
	//   place <pos>        — setBlock the controller (FORMED false; the chains build
	//                        the 16-part shell with plain setblocks)
	//   fill <pos> <mB>    — hot_water through the CAPABILITY fuel door (the :223
	//                        isFuel gate proves the poured fuels_hot row live)
	//   fillraw <pos> <mB> — DIRECT mTanks[0] fill (the boiler distw acceptance
	//                        channel; bypasses the door)
	//   stat <pos>         — formed / rate / buffer / tanks / offered
	// -------------------------------------------------------------------------

	private static void register(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tHex = Commands.literal("gt6heatexchanger")
				.then(Commands.literal("place")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
				.then(Commands.literal("fill")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
										.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"), false)))))
				.then(Commands.literal("fillraw")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
										.executes(aContext -> fill(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "amount"), true)))))
				.then(Commands.literal("stat")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> stat(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
		aEvent.getDispatcher().register(tHex);
		LOGGER.info("Registered GT6 heat-exchanger acceptance command /gt6heatexchanger (place|fill|fillraw|stat <pos> x <amount>) — the Large Heat Exchanger (17197, FM.Hot, NBT_OUTPUT 16384 HU/t)");
	}

	/**
	 * FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Boilers form):
	 * the three DeferredRegisters join the mod bus, strictly before any RegisterEvent.
	 */
	@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
	private static final class ModBusListener {
		@SubscribeEvent
		public static void onModConstruct(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent aEvent) {
			//? if forge {
			net.minecraftforge.eventbus.api.IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
			//?} else {
			/*net.minecraftforge.eventbus.api.IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
			 *///?}
			BLOCKS_REG.register(tModBus);
			BLOCK_ENTITY_TYPES.register(tModBus);
			ITEMS.register(tModBus);
		}

		/**
		 * The tab walk (task p38-tabfix-a-multiblock — the single HEX item joins the
		 * multiblocks tab; the GT6BurningBoxes.onBuildTabContents verbatim form, the MOD-bus
		 * {@code @Mod.EventBusSubscriber} on this nested listener is what delivers it). JEI
		 * derives its item list from the tab display items.
		 */
		@SubscribeEvent
		public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
			if (aEvent.getTabKey().location().equals(GTMultiBlocks.MULTIBLOCKS_TAB.getId())) {
				aEvent.accept(new ItemStack(HEAT_EXCHANGER_ITEM.get()));
			}
		}
	}

	/** The command listener (the FORGE bus — the RegisterCommandsEvent face). */
	@Mod.EventBusSubscriber(modid = "gt6")
	private static final class CommandListener {
		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
			register(aEvent);
		}
	}

	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		boolean tPlaced = tLevel.setBlock(aPos, HEAT_EXCHANGER_BLOCK.get().defaultBlockState(), 3);
		if (!tPlaced) {
			aSource.sendFailure(Component.literal("PLACE FAILED at " + aPos.toShortString()));
			return 0;
		}
		String tLine = "GT6 large heat exchanger placed at " + aPos.getX() + ", " + aPos.getY() + ", " + aPos.getZ();
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int fill(CommandSourceStack aSource, BlockPos aPos, int aAmount, boolean aRaw) {
		ServerLevel tLevel = aSource.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		if (!(tBE instanceof GT6HeatExchangerBlockEntity tHex)) {
			aSource.sendFailure(Component.literal("FILL FAILED: no heat exchanger BE at " + aPos.toShortString()));
			return 0;
		}
		FluidStack tFuel = new FluidStack(GTFluids.HOT_WATER.source.get(), aAmount);
		int tFilled;
		if (aRaw) {
			tFilled = tHex.mTanks[0].fill(tFuel, IFluidHandler.FluidAction.EXECUTE); // the acceptance channel
		} else {
			//? if forge {
			IFluidHandler tDoor = tHex.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
					Direction.UP).orElse(null);
			//?} else {
			/*IFluidHandler tDoor = tHex.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
					Direction.UP);
			 *///?}
			if (tDoor == null) {
				aSource.sendFailure(Component.literal("FILL FAILED: no fuel door at " + aPos.toShortString()));
				return 0;
			}
			tFilled = tDoor.fill(tFuel, IFluidHandler.FluidAction.EXECUTE);
		}
		String tLine = String.format("GT6 heat exchanger fill at %s: filled %d/%d L of gt6:hot_water%s, fuel tank holds %d L",
				aPos.toShortString(), tFilled, aAmount, tFilled == 0 ? " (REJECTED)" : " (ACCEPTED)", tHex.mTanks[0].amount());
		aSource.sendSuccess(() -> Component.literal(tLine), false);
		LOGGER.info(tLine);
		return Command.SINGLE_SUCCESS;
	}

	private static int stat(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		if (!(tBE instanceof GT6HeatExchangerBlockEntity tHex)) {
			aSource.sendFailure(Component.literal("STAT FAILED: no heat exchanger BE at " + aPos.toShortString()));
			return 0;
		}
		for (String tLine : new String[] {
				// the FORCE recheck (the acceptance arm: the chains build the shell by plain
				// setblocks AFTER the controller's first-tick force check — the cached verdict
				// would read false until the 600-tick poll)
				"GT6 heat exchanger at " + aPos.toShortString() + ": formed=" + tHex.checkStructure(true)
						+ " rate=" + tHex.mRate + " efficiency=" + tHex.mEfficiency + " active=" + tHex.mActive
						+ " diag=" + gregtech6.multiblock.GTMultiBlockStructureChecker.check(tHex, tHex.patternWalkFacing(), null, null, null).describeFirstFailure(),
				"buffer=" + tHex.mEnergy + " HU offered=" + tHex.getEnergyOffered(tHex.mEnergyTypeEmitted, (byte)1, tHex.mRate)
						+ " HU/t fuel=" + tHex.mTanks[0].amount() + "/" + tHex.mTanks[0].capacity() + " L overflow=" + tHex.mTanks[1].amount() + " L"}) {
			aSource.sendSuccess(() -> Component.literal(tLine), false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private GT6HeatExchangers() {}
}
