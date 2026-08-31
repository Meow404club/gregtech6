package gregtech6.covers;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import gregtech6.client.render.GTModelProperties;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.util.UT6;

/**
 * {@code /gt6cover} — the cover acceptance command (task p4-cover-core acceptance ②,
 * card-local in covers/ like the oven's /gt6oven; RCON-drivable, console-safe via
 * FakePlayerFactory). Task p5-barrel-side-rules spec F generalizes the host from
 * {@code TileEntityOven} to {@link ICoverableTE} (the barrel joins the covered family),
 * adds the optional {@code itemId} install argument (any registered cover item) and the
 * {@code mode} subcommand (the screwdriver relay that flips the pump direction).
 * Task p9-redstone-cover-emitter extends the {@code mode} target vocabulary with
 * {@code cutter} (the emitter's strong-gate toggle, relayed through the same
 * onCoverToolClick arm) and adds the {@code signal} subcommand — the direct
 * {@code covers.visual(side, bind4(v), true)} write that tunes the redstone emitter's
 * tier. The right-click install and the crowbar dismantle run through the ICoverableTE
 * dispatches; the hoe-class crowbar substitute is a real hoe in the fake player's hand,
 * so the ToolActions classification path is what gets exercised:
 *
 * <ul>
 * <li>{@code install [<pos>] [<side>] [<itemId>]} — setCoverItem(side, stack, player,
 *     force=F, blockUpdate=T): the Registry check (:295-296), the set (:302),
 *     onCoverPlaced (:304) and causeBlockUpdate (:306) run; the report asserts the
 *     covers NBT keys and the render snapshot mask (acceptance ③ server evidence);
 *     the item defaults to gt6:plate_iron, the pump chain passes gt6:cover_pump,
 *     the emitter chain gt6:cover_redstone_emitter;</li>
 * <li>{@code dismantle [<pos>] [<side>]} — onCoverToolClick with a hoe: the :145-152
 *     path drops the cover (popResource, acceptance's 掉落断言 via an ItemEntity scan)
 *     and the store dissolves to {@code null} (:313-317, 全空回 null);</li>
 * <li>{@code mode [<pos>] [<side>] [<target>]} — onCoverToolClick with the screwdriver
 *     tool id: the covered behaviour's onToolClick relay (the pump cover flips its
 *     visual lane 0 out ↔ 1 in). {@code target=cutter} relays the emitter's
 *     TOOL_CUTTER id instead (the mValues bit-0 strong-gate toggle, upstream
 *     CoverRedstoneEmitter :42-46) — a cover that answers neither id returns 0 and
 *     the command fails;</li>
 * <li>{@code signal [<pos>] [<side>] <0-15>} — the declared-deviation acceptance
 *     channel: the direct {@code covers.visual(side, bind4(v), true)} tier write on
 *     the emitter plate (upstream the bare-hand keypad :72-109 is the player channel).
 *     The value change fires sendBlockUpdateFromCover through CoverData.visual
 *     (:142-150), refreshing the redstone neighbours; the report carries the host
 *     exit readings (weak/strong toward a receiver on that face).</li>
 * <li>{@code check [<pos>]} — the covers NBT + snapshot report.</li>
 * </ul>
 *
 * <p>The GTOvenBlock/GTBarrelBlock.use wiring (right-click install in-game) is the
 * three-line onCoverUse append at the block side; the dispatch itself lives in
 * {@link ICoverableTE#onCoverUse} and is unit-tested.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTCoverCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The default install item — the p4 iron-plate cover (gt6:plate_iron). */
	private static final net.minecraft.resources.ResourceLocation DEFAULT_COVER_ITEM = new net.minecraft.resources.ResourceLocation("gt6", "plate_iron");

	private GTCoverCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tCover = Commands.literal("gt6cover")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("install")
				.executes(context -> install(context.getSource(), null, Direction.UP, DEFAULT_COVER_ITEM))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> install(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP, DEFAULT_COVER_ITEM))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> install(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")), DEFAULT_COVER_ITEM))
						.then(Commands.argument("itemId", net.minecraft.commands.arguments.ResourceLocationArgument.id())
							.executes(context -> install(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
									parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")),
									net.minecraft.commands.arguments.ResourceLocationArgument.getId(context, "itemId")))))))
			.then(Commands.literal("dismantle")
				.executes(context -> dismantle(context.getSource(), null, Direction.UP))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")))))))
			.then(Commands.literal("mode")
				// the p5 screwdriver relay — flips the pump cover's direction lane (0 out ↔ 1 in);
				// the optional trailing word pins the target direction ("out"/"in") so an RCON
				// chain can SET a direction instead of guessing the toggle parity
				.executes(context -> mode(context.getSource(), null, Direction.UP, null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> mode(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP, null))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> mode(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")), null))
						.then(Commands.argument("target", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(context -> mode(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
									parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")),
									com.mojang.brigadier.arguments.StringArgumentType.getString(context, "target")))))))
			.then(Commands.literal("signal")
				// the p9 emitter acceptance channel — the direct visual-lane write (the player
				// channel is the bare-hand keypad on the plate); pos/side default like install
				.then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 15))
					.executes(context -> signal(context.getSource(), null, Direction.UP,
							com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value"))))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 15))
						.executes(context -> signal(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP,
								com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value"))))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 15))
							.executes(context -> signal(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
									parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")),
									com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value")))))))
			.then(Commands.literal("check")
				.executes(context -> check(context.getSource(), null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> check(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))));
		event.getDispatcher().register(tCover);
		LOGGER.info("Registered GT6 cover acceptance command /gt6cover (install|dismantle|mode|signal|check)");
	}

	/** {@code down|up|north|south|west|east} → Direction; 1.20.1 ships no direction argument type. */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/** The covered host at pos (p5 spec F — any ICoverableTE, oven and barrel alike). */
	private static CoverableHost coverableHostAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		return tLevel.getBlockEntity(tTarget) instanceof ICoverableTE tHost ? new CoverableHost(tHost, tTarget) : null;
	}

	private record CoverableHost(ICoverableTE host, BlockPos pos) {
	}

	/** Install a cover item on the face (default gt6:plate_iron); asserts the NBT keys + snapshot mask. */
	private static int install(CommandSourceStack source, BlockPos pos, Direction side, net.minecraft.resources.ResourceLocation aItemId) {
		CoverableHost tHost = coverableHostAt(source, pos);
		if (tHost == null) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		byte tSide = (byte) side.get3DDataValue();
		GT6Covers.init(); // idempotent safety: the registration rides CommonSetup; commands may run first on a bare world
		ItemStack tCoverStack = new ItemStack(ForgeRegistries.ITEMS.getValue(aItemId));
		if (tCoverStack.isEmpty()) {
			source.sendFailure(Component.literal(aItemId + " not registered"));
			return 0;
		}
		if (CoverRegistry.get(tCoverStack) == null) {
			source.sendFailure(Component.literal(aItemId + " carries no registered cover"));
			return 0;
		}
		boolean tOk = tHost.host().setCoverItem(tSide, tCoverStack, FakePlayerFactory.getMinecraft(source.getLevel()), false, true);
		CompoundTag tCoversTag = new CompoundTag();
		tHost.host().writeCoversToNBT(tCoversTag);
		GTCoverRenderSnapshot tSnapshot = snapshotOf(tHost.host());
		String tReport = String.format("GT6 cover install %s face %s at %s: ok=%s, nbt=%s, snapshotMask=%s, sprites=%s",
				tCoverStack.getItem(), side, tHost.pos().toShortString(), tOk, tCoversTag.getCompound(ICoverableTE.NBT_COVERS),
				tSnapshot == null ? "none" : tSnapshot.mask(), tSnapshot == null ? "none" : tSnapshot.coverSprites());
		if (!tOk || tCoversTag.getCompound(ICoverableTE.NBT_COVERS).isEmpty() || tSnapshot == null) {
			source.sendFailure(Component.literal("GT6 cover install FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 cover install check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** Hoe-dismantle the cover; asserts the drop + the store dissolving to null. */
	private static int dismantle(CommandSourceStack source, BlockPos pos, Direction side) {
		CoverableHost tHost = coverableHostAt(source, pos);
		if (tHost == null) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		byte tSide = (byte) side.get3DDataValue();
		if (!tHost.host().isCovered(tSide)) {
			source.sendFailure(Component.literal("GT6 cover dismantle FAILED: no cover on face " + side + " at " + tHost.pos().toShortString()));
			return 0;
		}
		// upstream :149 — ST.add(aPlayer,...) falls through to ST.place; the RCON observable is
		// the world drop (the give-path is covered by the unit-test double), so aPlayer = null
		// forces the popResource branch deterministically.
		long tDamage = tHost.host().onCoverToolClick("", null, new ItemStack(Items.WOODEN_HOE), tSide, false);
		// the 掉落断言: the popResource path drops at the covered face — scan a small box
		BlockPos tDropPos = tHost.pos().relative(side);
		List<? extends ItemEntity> tDrops = source.getLevel().getEntitiesOfClass(ItemEntity.class,
				new AABB(tDropPos.getX() - 2, tDropPos.getY() - 2, tDropPos.getZ() - 2, tDropPos.getX() + 3, tDropPos.getY() + 3, tDropPos.getZ() + 3));
		CompoundTag tCoversTag = new CompoundTag();
		tHost.host().writeCoversToNBT(tCoversTag);
		String tReport = String.format("GT6 cover dismantle %s face %s at %s: toolDamage=%d, drops=%s, coversNbt=%s, store=%s",
				tHost.host(), side, tHost.pos().toShortString(), tDamage,
				tDrops.stream().map(t -> t.getItem().getCount() + "x " + t.getItem().getItem()).toList(),
				tCoversTag.getCompound(ICoverableTE.NBT_COVERS), tHost.host().getCovers() == null ? "null" : "alive");
		if (tDamage != 10000 || tDrops.isEmpty() || tHost.host().getCovers() != null) {
			source.sendFailure(Component.literal("GT6 cover dismantle FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 cover dismantle check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The p5 mode relay: onCoverToolClick with {@link ICover#TOOL_SCREWDRIVER} — the cover
	 * dispatch skips the crowbar branch (a screwdriver is not a hoe) and hands the tool id
	 * to the covered behaviour, whose onToolClick flips the visual lane. With
	 * {@code aTarget} ("out"/"in") the command SETS the direction (a single flip when the
	 * lane differs); without it the command is the plain toggle relay.
	 *
	 * <p>Task p9: {@code aTarget == "cutter"} relays {@link CoverRedstoneEmitter#TOOL_CUTTER}
	 * instead — the emitter's strong-gate toggle (upstream CoverRedstoneEmitter :42-46,
	 * {@code mValues ^ B[0]} with the block update). The cutter relay is always a toggle
	 * (upstream semantics — no pinned target); a cover answering neither id returns 0 and
	 * the command fails, so the pump is untouched by the new word (it does not know
	 * "cutter") and the emitter does not answer the screwdriver.
	 */
	private static int mode(CommandSourceStack source, BlockPos pos, Direction side, @Nullable String aTarget) {
		CoverableHost tHost = coverableHostAt(source, pos);
		if (tHost == null) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		byte tSide = (byte) side.get3DDataValue();
		if (!tHost.host().isCovered(tSide)) {
			source.sendFailure(Component.literal("GT6 cover mode FAILED: no cover on face " + side + " at " + tHost.pos().toShortString()));
			return 0;
		}
		boolean tCutter = aTarget != null && "cutter".equalsIgnoreCase(aTarget);
		Short tDesired = null;
		if (aTarget != null && !tCutter) {
			tDesired = aTarget.equalsIgnoreCase("out") ? (short) 0 : aTarget.equalsIgnoreCase("in") ? (short) 1 : null;
			if (tDesired == null) {
				source.sendFailure(Component.literal("GT6 cover mode FAILED: unknown direction '" + aTarget + "' (use out|in|cutter)"));
				return 0;
			}
		}
		short tCurrent = tHost.host().getCovers() == null ? 0 : tHost.host().getCovers().mVisuals[tSide];
		boolean tFlipNeeded = tCutter || tDesired == null || tDesired != tCurrent;
		long tDamage = tFlipNeeded ? tHost.host().onCoverToolClick(tCutter ? CoverRedstoneEmitter.TOOL_CUTTER : ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, tSide, false) : 0;
		short tVisual = tHost.host().getCovers() == null ? 0 : tHost.host().getCovers().mVisuals[tSide];
		boolean tStrong = tHost.host().getCovers() != null && tHost.host().getCovers().mValues[tSide] != 0;
		String tDirection = tVisual == 0 ? "out" : "in";
		String tReport = String.format("GT6 cover mode %s face %s at %s: tool=%s, toolDamage=%d, visual=%d (%s), strong=%s, flipped=%s",
				tHost.host(), side, tHost.pos().toShortString(), tCutter ? "cutter" : "screwdriver", tDamage, tVisual, tDirection, tStrong, tFlipNeeded);
		if (tFlipNeeded && tDamage == 0) {
			source.sendFailure(Component.literal("GT6 cover mode FAILED: the tool toggle did not fire — " + tReport));
			return 0;
		}
		if (tCutter && tDamage != 1000) {
			source.sendFailure(Component.literal("GT6 cover mode FAILED: the cutter relay did not answer — " + tReport));
			return 0;
		}
		if (!tCutter && tDesired != null && tVisual != tDesired) {
			source.sendFailure(Component.literal("GT6 cover mode FAILED: direction did not land — " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 cover mode OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The p9 emitter acceptance channel (the declared deviation — the player channel is
	 * the bare-hand keypad): the direct {@code covers.visual(side, bind4(aValue), true)}
	 * tier write. CoverData.visual fires {@code sendBlockUpdateFromCover} on the change
	 * (:142-150), so the redstone neighbours re-query the host exits without any extra
	 * API; the report carries those exit readings toward a receiver on the written face.
	 */
	private static int signal(CommandSourceStack source, BlockPos pos, Direction side, int aValue) {
		CoverableHost tHost = coverableHostAt(source, pos);
		if (tHost == null) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		byte tSide = (byte) side.get3DDataValue();
		if (!tHost.host().isCovered(tSide)) {
			source.sendFailure(Component.literal("GT6 cover signal FAILED: no cover on face " + side + " at " + tHost.pos().toShortString()));
			return 0;
		}
		byte tTier = CoverRedstoneEmitter.bind4(aValue);
		tHost.host().getCovers().visual(tSide, tTier, true);
		// the host exit readings toward a receiver sitting on the written face (the vanilla
		// query hands in OPOS[face]; the bridge folds it back — the same numbers the lamp sees)
		byte tQuery = UT6.OPOS[tSide];
		int tWeak = tHost.host().getRedstoneOutWeak(tQuery, 0);
		int tStrong = tHost.host().getRedstoneOutStrong(tQuery, 0);
		String tReport = String.format("GT6 cover signal %s face %s at %s: tier=%d, outWeak=%d, outStrong=%d",
				tHost.host(), side, tHost.pos().toShortString(), tTier, tWeak, tStrong);
		if (tWeak != tTier) {
			source.sendFailure(Component.literal("GT6 cover signal FAILED: the weak exit does not read the written tier — " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("GT6 cover signal OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	private static int check(CommandSourceStack source, BlockPos pos) {
		CoverableHost tHost = coverableHostAt(source, pos);
		if (tHost == null) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + (pos != null ? pos.toShortString() : "the source position")));
			return 0;
		}
		CompoundTag tCoversTag = new CompoundTag();
		tHost.host().writeCoversToNBT(tCoversTag);
		GTCoverRenderSnapshot tSnapshot = snapshotOf(tHost.host());
		source.sendSuccess(() -> Component.literal(String.format(
				"GT6 covers at %s: store=%s, nbt=%s, snapshotMask=%s, sprites=%s",
				tHost.pos().toShortString(), tHost.host().getCovers() == null ? "null" : "alive",
				tCoversTag.getCompound(ICoverableTE.NBT_COVERS),
				tSnapshot == null ? "none" : tSnapshot.mask(), tSnapshot == null ? "none" : tSnapshot.coverSprites())), false);
		return Command.SINGLE_SUCCESS;
	}

	/** The render snapshot the BE model would serve (acceptance ③ server-side evidence). */
	private static GTCoverRenderSnapshot snapshotOf(ICoverableTE aHost) {
		return aHost.self().getModelData().get(GTModelProperties.RENDER_SNAPSHOT) instanceof GTCoverRenderSnapshot tSnapshot ? tSnapshot : null;
	}
}
