package gregtech6.tileentity.multiblocks;

import com.mojang.brigadier.Command;
import javax.annotation.Nullable;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.registry.GTMultiBlocks;

/**
 * {@code /gt6multiblock} — the multiblock acceptance command (task p4-multiblock-framework ②,
 * RCON-drivable like /gt6oven). Console-safe throughout.
 *
 * <ul>
 * <li>{@code place <pos>} — setBlock a fresh Coke Oven controller (FACING north → the
 *     structure core sits one cell south, the getOffsetXN "behind the facing" arithmetic);</li>
 * <li>{@code frame <pos>} — place all 26 brick cells around the computed centre (occupied
 *     cells keep their content);</li>
 * <li>{@code hole <pos>} — break ONE part cell with the breakBlock propagation (upstream
 *     MultiTileEntityMultiBlockPart :176-184), the command-side stand-in for a player break;</li>
 * <li>{@code wand <pos>} — the builder-wand simulation (task card ③): a FakePlayer inventory
 *     stocked with 26 bricks, then the upstream onToolClick2 sequence :141-146 verbatim —
 *     {@code checkStructure2(controllerPos, player=null, fakeInventory)} (the consume path:
 *     null player auto-approves the canEdit chain and ST.use-equivalent shrinks the stock)
 *     followed by the linking {@code checkStructure(true)} pass;</li>
 * <li>{@code check <pos>} — the magnifying glass (upstream onMagnifyingGlass :160-170):
 *     cheap-path check, forced recheck on failure, verdict + linked-part census;</li>
 * <li>{@code tick <pos> <ticks>} — drives the dispatcher manually (the same updateEntity the
 *     real ticker runs), exercising the onTickFirst forced check and the 600-tick poll;</li>
 * <li>{@code input <count> [item|prefix material] [pos]} — the p6/p7 acceptance feed:
 *     inserts through the gated item capability (slot 0 only); the default feed is
 *     gt6:gem_coal, the explicit {@code item} form covers the tag-path oak_log assertion,
 *     and the p7 {@code prefix material} form (case-insensitive internal names, e.g.
 *     {@code dust Oilshale}) drives the oil-shale rows through GTMaterialItems;</li>
 * <li>{@code ignite [pos]} — the TOOL_igniter branch (MultiTileEntityBasicMachine
 *     :373-379 → TileEntityBase10MultiBlockMachine.ignite());</li>
 * <li>{@code check <pos>} additionally reports the processing state (progress/energy/
 *     ignited/tank/slots) since p6.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTMultiBlockCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The wand stock: the full 25-brick structure in one stack (27 cells = air centre + 25 bricks + the controller). */
	private static final int WAND_STOCK = 25;

	private GTMultiBlockCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tMulti = Commands.literal("gt6multiblock")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("place")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> place(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("frame")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> frame(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("hole")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> hole(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("wand")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> wand(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("check")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> check(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("input")
				.then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
					.executes(aContext -> input(aContext.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null, null, null, null))
					.then(Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(aEvent.getBuildContext()))
						.executes(aContext -> input(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
								net.minecraft.commands.arguments.item.ItemArgument.getItem(aContext, "item"), null, null, null))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.executes(aContext -> input(aContext.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
									net.minecraft.commands.arguments.item.ItemArgument.getItem(aContext, "item"), null, null,
									BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
					.then(Commands.argument("prefix", com.mojang.brigadier.arguments.StringArgumentType.word())
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(aContext -> input(aContext.getSource(),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null,
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "prefix"),
									com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"), null))
							.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(aContext -> input(aContext.getSource(),
										com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"), null,
										com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "prefix"),
										com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"),
										BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> input(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "count"),
								null, null, null, BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))))
			.then(Commands.literal("ignite")
				.executes(aContext -> ignite(aContext.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(aContext -> ignite(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos")))))
			.then(Commands.literal("tick")
				.then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20000))
					.executes(aContext -> tick(aContext.getSource(),
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"), null))
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(aContext -> tick(aContext.getSource(),
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "ticks"),
								BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
		aEvent.getDispatcher().register(tMulti);
		LOGGER.info("Registered GT6 multiblock acceptance command /gt6multiblock (place|frame|hole|wand|check|tick|input|ignite)");
	}

	private static TileEntityCokeOven ovenAt(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tTarget = aPos != null ? aPos : BlockPos.containing(aSource.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof TileEntityCokeOven tOven ? tOven : null;
	}

	/** The structure centre: one cell behind the facing (getOffsetXN/YN/ZN arithmetic). */
	private static BlockPos structureCenter(TileEntityCokeOven aOven) {
		return new BlockPos(aOven.getOffsetXN(aOven.mFacing), aOven.getOffsetYN(aOven.mFacing), aOven.getOffsetZN(aOven.mFacing));
	}

	private static int place(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		tLevel.setBlock(aPos, GTMultiBlocks.COKE_OVEN.get().defaultBlockState(), 3);
		aSource.sendSuccess(() -> Component.literal("GT6 coke oven controller placed at " + aPos.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Places the 25 brick cells of the 3x3x3 (the air centre and the controller cell excluded, occupied cells kept). */
	private static int frame(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		ServerLevel tLevel = aSource.getLevel();
		BlockPos tCenter = structureCenter(tOven);
		BlockPos tController = tOven.getBlockPos();
		int[] tPlaced = {0};
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = tCenter.offset(i, j, k);
			if (tCell.equals(tController)) continue; // the controller occupies one shell cell
			if (tLevel.getBlockState(tCell).isAir()) {
				tLevel.setBlock(tCell, GTMultiBlocks.COKE_OVEN_BRICKS.get().defaultBlockState(), 3);
				tPlaced[0]++;
			}
		}
		int tReported = tPlaced[0];
		aSource.sendSuccess(() -> Component.literal("GT6 coke oven frame: " + tReported + " bricks placed around " + tCenter.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	/** Breaks one part cell, running the breakBlock propagation the player-break hook runs. */
	private static int hole(CommandSourceStack aSource, BlockPos aPos) {
		ServerLevel tLevel = aSource.getLevel();
		if (!(tLevel.getBlockEntity(aPos) instanceof MultiBlockPartBlockEntity tPart)) {
			aSource.sendFailure(Component.literal("No MultiBlockPartBlockEntity at " + aPos.toShortString()));
			return 0;
		}
		ITileEntityMultiBlockController tTarget = tPart.getTarget(false); // upstream breakBlock :177
		tLevel.setBlock(aPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		if (tTarget != null) {
			tPart.clearTarget();                 // :179-180
			tTarget.onStructureChange();         // :181
		}
		aSource.sendSuccess(() -> Component.literal("GT6 part broken at " + aPos.toShortString() + ", controller flagged: " + (tTarget != null)), false);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The builder-wand simulation: stocked FakePlayer inventory + the upstream onToolClick2
	 * builder-wand branch :141-146 (two calls: the placing checkStructure2 pass, then the
	 * linking checkStructure pass). The player argument stays null — the canEdit chain
	 * auto-approves non-players (UT.Entities.canEdit :3159) and the consume path shrinks the
	 * FakePlayer stock (ST.use :304-319 semantics).
	 */
	private static int wand(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		net.minecraft.world.entity.player.Inventory tInventory = FakePlayerFactory.getMinecraft(aSource.getLevel()).getInventory();
		tInventory.items.set(0, new ItemStack(GTMultiBlocks.COKE_OVEN_BRICKS_ITEM.get(), WAND_STOCK));

		tOven.checkStructure2(tOven.getBlockPos(), null, tInventory); // the placing pass (:143)
		boolean tFormed = tOven.checkStructure(true);                 // the linking pass (:144)

		int tLeft = tInventory.items.get(0).getCount();
		String tReport = String.format("GT6 coke oven wand at %s: formed=%s, brick stock %d -> %d",
				tOven.getBlockPos().toShortString(), tFormed, WAND_STOCK, tLeft);
		if (!tFormed) {
			aSource.sendFailure(Component.literal("GT6 multiblock wand check FAILED: " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("GT6 multiblock wand check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The magnifying glass (:160-170) + a linked-part census over the 26 cells. */
	private static int check(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + aPos.toShortString()));
			return 0;
		}
		String tVerdict;
		if (tOven.checkStructure(false)) {
			tVerdict = "Structure is formed already!";
		} else {
			tVerdict = tOven.checkStructure(true) ? "Structure did form just now!" : "Structure did not form!";
		}
		int tLinked = 0;
		BlockPos tCenter = structureCenter(tOven);
		ServerLevel tLevel = aSource.getLevel();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = tCenter.offset(i, j, k);
			if (tLevel.getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.getTarget(false) == tOven) tLinked++;
		}
		boolean tBlockFormed = tLevel.getBlockState(tOven.getBlockPos()).getValue(TileEntityBase10MultiBlockBase.FORMED);
		String tReport = String.format("GT6 coke oven at %s: %s okay=%s block_formed=%s linked_parts=%d/25",
				tOven.getBlockPos().toShortString(), tVerdict, tOven.mStructureOkay, tBlockFormed, tLinked);
		String tMachine = machineReport(tOven);
		if (!tOven.mStructureOkay || !tBlockFormed) {
			aSource.sendFailure(Component.literal(tReport + " | " + tMachine));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		aSource.sendSuccess(() -> Component.literal(tMachine), false);
		LOGGER.info(tReport + " | " + tMachine);
		return Command.SINGLE_SUCCESS;
	}

	/** The processing-state report (task p6 acceptance: progress/energy/ignited/tank/slots). */
	private static String machineReport(TileEntityCokeOven aOven) {
		StringBuilder rSlots = new StringBuilder();
		for (int i = 0; i < aOven.INVENTORY_SIZE; i++) {
			ItemStack tStack = aOven.slot(i);
			if (tStack.isEmpty()) continue;
			if (rSlots.length() > 0) rSlots.append(", ");
			rSlots.append(i).append("=").append(tStack.getCount()).append("x ")
					.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tStack.getItem()));
		}
		String tTank = aOven.mTanksOutput[0].isEmpty()
				? "-"
				: aOven.mTanksOutput[0].amount() + "mB "
						+ net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aOven.mTanksOutput[0].fluid().getFluid());
		return String.format("machine: progress=%d/%d energy=%d min_energy=%d ignited=%d active=%s running=%s stopped=%s tank=[%s] slots=[%s]",
				aOven.mProgress, aOven.mMaxProgress, aOven.mEnergy, aOven.mMinEnergy, aOven.mIgnited,
				aOven.mActive, aOven.mRunning, aOven.mStopped, tTank, rSlots.length() == 0 ? "-" : rSlots);
	}

	/**
	 * {@code input <count> [item|prefix material] [pos]} — fills the input slot through the
	 * gated item capability (the slot-0 insert gate, canInsertItem2 :549-554). The default
	 * feed is gt6:gem_coal (GTMaterialItems.get(OP.gem, MT.Coal), the p6 card ruling); an
	 * explicit {@code item} argument covers the tag-path acceptance (minecraft:oak_log), and
	 * the p7 {@code prefix material} form resolves the GT material universe (case-insensitive
	 * internal names via {@link #findPrefix}/{@link #findMaterial}, e.g. {@code dust OilShale})
	 * so the oil-shale rows are drivable without hand-naming ids. Since p8 the resolution
	 * falls back to {@code GTMaterialBlocks.get} when the item path misses, so block items
	 * ({@code input blockIngot Coal}) feed the oven too.
	 */
	private static int input(CommandSourceStack aSource, int aCount,
			@Nullable net.minecraft.commands.arguments.item.ItemInput aItem,
			@Nullable String aPrefixName, @Nullable String aMaterialName, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		ItemStack tStack;
		if (aItem != null) {
			try {
				tStack = new ItemStack(aItem.getItem(), aCount);
			} catch (Exception e) {
				aSource.sendFailure(Component.literal("Cannot resolve item: " + e));
				return 0;
			}
		} else if (aPrefixName != null && aMaterialName != null) {
			gregapi.oredict.OreDictPrefix tPrefix = findPrefix(aPrefixName);
			gregapi.oredict.OreDictMaterial tMaterial = findMaterial(aMaterialName);
			if (tPrefix == null || tMaterial == null) {
				aSource.sendFailure(Component.literal("Cannot resolve GT material pair: " + aPrefixName + " " + aMaterialName));
				return 0;
			}
			net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
					gregtech6.registry.GTMaterialItems.get(tPrefix, tMaterial);
			if (tHandle == null || !tHandle.isPresent()) tHandle = gregtech6.registry.GTMaterialBlocks.get(tPrefix, tMaterial); // p8: block items (e.g. blockIngot Coal)
			if (tHandle == null || !tHandle.isPresent()) {
				aSource.sendFailure(Component.literal("No gt6 item for prefix '" + tPrefix.mNameInternal + "' + material '" + tMaterial.mNameInternal + "'"));
				return 0;
			}
			tStack = new ItemStack(tHandle.get(), aCount);
		} else {
			net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
					gregtech6.registry.GTMaterialItems.get(gregapi.data.OP.gem, gregapi.data.MT.Coal);
			if (tHandle == null || !tHandle.isPresent()) {
				aSource.sendFailure(Component.literal("gt6:gem_coal is not registered"));
				return 0;
			}
			tStack = new ItemStack(tHandle.get(), aCount);
		}
		ItemStack tLeftover = tOven.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null)
				.map(tHandler -> tHandler.insertItem(0, tStack, false))
				.orElse(tStack);
		int tInserted = aCount - tLeftover.getCount();
		String tReport = String.format("GT6 coke oven input %d %s at %s: inserted %d%s", aCount,
				net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tStack.getItem()),
				tOven.getBlockPos().toShortString(), tInserted, tLeftover.isEmpty() ? "" : ", leftover " + tLeftover.getCount());
		if (tInserted <= 0) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The case-insensitive internal-name lookup over the registered prefixes (RCON-friendly). */
	@Nullable
	private static gregapi.oredict.OreDictPrefix findPrefix(String aName) {
		for (gregapi.oredict.OreDictPrefix tPrefix : gregapi.oredict.OreDictPrefix.VALUES) {
			if (tPrefix.mNameInternal.equalsIgnoreCase(aName)) return tPrefix;
		}
		return null;
	}

	/**
	 * The case-insensitive internal-name lookup over the registered materials, merged onto the
	 * registration target (MaterialRegistry.get alias resolution). byName alone is exact-match
	 * AND can land on an id -1 auto-invalid placeholder shadowing the real name — e.g. the
	 * oredict name "Oil Shale" gives mNameInternal "OilShale" (item gt6:dust_oil_shale) while
	 * "Oilshale" sits in the map as an mID -1 husk — so the scan requires mID >= 0.
	 */
	@Nullable
	private static gregapi.oredict.OreDictMaterial findMaterial(String aName) {
		gregapi.oredict.OreDictMaterial tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.byName(aName);
		if (tMaterial == null) tMaterial = gregapi.oredict.MaterialRegistry.INSTANCE.byName(gregapi.oredict.MaterialRegistry.sanitize(aName));
		if (tMaterial != null && tMaterial.mID >= 0) return gregapi.oredict.MaterialRegistry.INSTANCE.get(tMaterial);
		for (gregapi.oredict.OreDictMaterial tCandidate : gregapi.oredict.OreDictMaterial.MATERIAL_MAP.values()) {
			if (tCandidate.mID >= 0 && tCandidate.mNameInternal.equalsIgnoreCase(aName)) {
				return gregapi.oredict.MaterialRegistry.INSTANCE.get(tCandidate);
			}
		}
		return null;
	}

	/** {@code ignite [pos]} — the TOOL_igniter branch (:373-379), the acceptance-chain ignition entry. */
	private static int ignite(CommandSourceStack aSource, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		tOven.ignite();
		String tReport = String.format("GT6 coke oven ignited at %s: requires_ignition=%s ignited=%d",
				tOven.getBlockPos().toShortString(), tOven.mRequiresIgnition, tOven.mIgnited);
		if (!tOven.mRequiresIgnition) {
			aSource.sendFailure(Component.literal(tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** Drives the 03 dispatcher manually — onTickFirst forced check + the 600-tick poll. */
	private static int tick(CommandSourceStack aSource, int aTicks, BlockPos aPos) {
		TileEntityCokeOven tOven = ovenAt(aSource, aPos);
		if (tOven == null) {
			aSource.sendFailure(Component.literal("No TileEntityCokeOven at " + (aPos != null ? aPos.toShortString() : "the source position")));
			return 0;
		}
		for (int i = 0; i < aTicks; i++) tOven.updateEntity();
		String tReport = String.format("GT6 coke oven ticked %d at %s: timer=%d okay=%s",
				aTicks, tOven.getBlockPos().toShortString(), tOven.getTimer(), tOven.mStructureOkay);
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		return Command.SINGLE_SUCCESS;
	}
}
