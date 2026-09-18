package gregtech6.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import gregtech6.covers.ICoverableTE;
import gregtech6.items.tools.GT6Prospector;
import gregtech6.items.tools.GT6ToolLadder;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.items.tools.GTCutterItem;
import gregtech6.items.tools.GTHammerItem;
import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * {@code /gt6tool} — the tool acceptance command (task p9-tool-crowbar spec ③; the
 * RCON entry that drives the formal crowbar without a player at the keyboard,
 * card-local in command/ like GTWireCommand/GT6EnergyCommand). The dismantle runs the
 * exact {@link GTCrowbarItem#crowbarToolClick(UseOnContext)} dispatch the item's
 * {@code useOn} runs — a fake player holds the crowbar so the cover item lands in the
 * inventory (upstream :149 ST.add) and the durability payment is observable:
 *
 * <ul>
 * <li>{@code dismantle [<pos>] [<side>]} — give the fake player a gt6:crowbar, aim it
 *     at the face (a synthetic BlockHitResult, the miss-free constructor form), run the
 *     shared dispatch and assert the upstream 10000 return, the one-point durability
 *     payment and the cover leaving the store; the report names the inventory landing.</li>
 * <li>{@code cut <pos> <side>} (task p10-tool-cutter spec ③) — give the fake player a
 *     gt6:cutter and run the same-shape dispatch into
 *     {@link GTCutterItem#cutterToolClick(UseOnContext)}; the report asserts the :76
 *     10000 return, the one-point durability payment and the wire's CONNECTIONS mask
 *     flipping (the connections before/after pair — the acceptance drives the flip
 *     round-trip with two cuts against /gt6wire stat).</li>
 * </ul>
 *
 * <p>This is the TOOL_CROWBAR-id half of the ICoverableTE :246-247 OR gate — the
 * legacy hoe half keeps its own {@code /gt6cover dismantle} (GTCoverCommand), so both
 * classification branches stay independently asserted in an RCON chain.
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTToolCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GTToolCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> tTool = Commands.literal("gt6tool")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("dismantle")
				.executes(context -> dismantle(context.getSource(), null, Direction.UP, null))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP, null))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")), null))
						.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
							.executes(context -> dismantle(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
									parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")),
									com.mojang.brigadier.arguments.StringArgumentType.getString(context, "material")))))))
			.then(Commands.literal("cut")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> cut(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> cut(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")))))))
			.then(Commands.literal("prospect")
				.executes(context -> prospect(context.getSource(), null, Direction.UP))
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(context -> prospect(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), Direction.UP))
					.then(Commands.argument("side", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> prospect(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"),
								parseSide(com.mojang.brigadier.arguments.StringArgumentType.getString(context, "side")))))))
			// task p31-machine-ladder — the machine-family material-ladder arm (the
			// GT6BladeToolCommand.stats shape): identity attached THE RECIPE WAY through
			// GT6ToolLadder.stampIdentity, the item surfaces read BACK.
			.then(Commands.literal("stats")
				.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
					.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(context -> stats(context.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(context, "tool"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(context, "material"))))));
		event.getDispatcher().register(tTool);
		LOGGER.info("Registered GT6 tool acceptance command /gt6tool (dismantle, cut, prospect, stats)");
		// task p27-vanilla-tag-dual-tree: the tag-membership debug command — the RCON
		// face of the dual-tree acceptance (`/gt6tags dump <tag>` lists the bound
		// runtime members of ANY item tag, so the forge:/c: twin faces are provable
		// live on both legs). Greedy-string argument: the tag id carries ':' and '/'
		// which the word() parser would reject.
		event.getDispatcher().register(Commands.literal("gt6tags")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("dump")
				.then(Commands.argument("tag", StringArgumentType.greedyString())
					.executes(context -> dumpTag(context.getSource(),
							StringArgumentType.getString(context, "tag"))))));
		LOGGER.info("Registered GT6 tags debug command /gt6tags (dump)");
	}

	/**
	 * {@code /gt6tags dump <tag>} — list the bound runtime members of one item tag
	 * (task p27-vanilla-tag-dual-tree). The spec string parses to a ResourceLocation
	 * (bare-identifier ctor argument — the 1.20.1 form, shifted to
	 * {@code ResourceLocation.parse} on the 21.1 leg by the stonecutter swap table),
	 * the members read through {@code BuiltInRegistries.ITEM.getTagOrEmpty} (the
	 * vanilla TagEntry.java:36 read shape, same-named on both legs) — the RUNTIME
	 * merged set, i.e. the Forge-shipped default members and the mod-datatpack
	 * members in one answer, which is exactly the unification-loop face the card
	 * proves ({@code forge:ingots/iron} must answer BOTH minecraft:iron_ingot and
	 * gt6:ingot_iron). Output is sorted-stable for the RCON assertions; an absent
	 * tag answers "0 members []" (getTagOrEmpty never throws).
	 */
	private static int dumpTag(CommandSourceStack aSource, String aTagSpec) {
		// the trimmed spec in a LOCAL — the stonecutter two-arg/one-arg ctor shift only
		// hits bare-identifier arguments (parenthesized expressions stay un-shifted and
		// would break the 21.1 leg compile, the materialTag javadoc lesson)
		String tSpec = aTagSpec.trim();
		ResourceLocation tId;
		try {
			tId = new ResourceLocation(tSpec);
		} catch (RuntimeException tError) {
			aSource.sendFailure(Component.literal("gt6tags: invalid tag id: " + tSpec));
			return 0;
		}
		TagKey<Item> tTag = TagKey.create(Registries.ITEM, tId);
		List<String> tMembers = new ArrayList<>();
		for (var tHolder : BuiltInRegistries.ITEM.getTagOrEmpty(tTag)) {
			tMembers.add(String.valueOf(BuiltInRegistries.ITEM.getKey(tHolder.value())));
		}
		Collections.sort(tMembers);
		String tReport = "gt6tags dump " + tId + ": " + tMembers.size() + " members " + tMembers;
		aSource.sendSuccess(() -> Component.literal(tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** {@code down|up|north|south|west|east} → Direction (the GTCoverCommand parser shape). */
	private static Direction parseSide(String aWord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		Direction tSide = Direction.byName(aWord.toLowerCase());
		if (tSide == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown side: " + aWord)).create();
		return tSide;
	}

	/**
	 * Crowbar-dismantle the cover through the item's own dispatch: the fake player
	 * holds a gt6:crowbar, the report asserts the 10000 upstream return, the 1-point
	 * durability payment and the cover landing in the fake player's inventory. The
	 * optional {@code material} argument (task p31-machine-ladder) stamps the
	 * {@code GT.ToolStats} identity THE RECIPE WAY first, so the per-material crowbar
	 * (its max-damage read and the payment ceiling) is the asserted surface.
	 */
	private static int dismantle(CommandSourceStack source, BlockPos pos, Direction side, String aMaterial) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		if (!(tLevel.getBlockEntity(tTarget) instanceof ICoverableTE tHost)) {
			source.sendFailure(Component.literal("No coverable GT6 BlockEntity at " + tTarget.toShortString()));
			return 0;
		}
		if (!tHost.isCovered((byte) side.get3DDataValue())) {
			source.sendFailure(Component.literal("gt6tool dismantle FAILED: no cover on face " + side + " at " + tTarget.toShortString()));
			return 0;
		}
		ItemStack tCoverBefore = tHost.getCoverItem((byte) side.get3DDataValue());
		ItemStack tCrowbar = new ItemStack(GT6Tools.CROWBAR.get());
		String tMaterialWord = "";
		if (aMaterial != null) {
			OreDictMaterial tMat = resolveMaterial(aMaterial);
			if (tMat == null) {
				source.sendFailure(Component.literal("gt6tool dismantle FAILED: unknown material: " + aMaterial));
				return 0;
			}
			GT6ToolLadder.stampIdentity(tCrowbar, tMat, ((GT6ToolLadder.LadderTool) GT6Tools.CROWBAR.get()).durabilityMultiplier());
			tMaterialWord = ", material=" + tMat.mNameInternal;
		}
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the landing
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tCrowbar);
		// the same UseOnContext shape useOn receives: the synthetic hit pins the clicked
		// face at the block pos (isInside=false, the plain four-arg constructor)
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tTarget), side, tTarget, false));
		long tDamage = GTCrowbarItem.crowbarToolClick(tContext);
		// the cover item landed in the fake player's inventory (upstream :149 ST.add —
		// the give half; the drop half is the /gt6cover dismantle null-player path)
		boolean tCoverInInventory = !tCoverBefore.isEmpty() && tFakePlayer.getInventory().items.stream()
				.anyMatch(t -> !t.isEmpty() && t.getItem() == tCoverBefore.getItem());
		int tHeldDamage = tCrowbar.getDamageValue();
		String tReport = String.format("gt6tool dismantle %s face %s at %s%s: toolDamage=%d, crowbarDamage=%d/%d, coverInInventory=%s",
				tHost, side, tTarget.toShortString(), tMaterialWord, tDamage, tHeldDamage, tCrowbar.getMaxDamage(), tCoverInInventory);
		if (tDamage != GTCrowbarItem.TOOL_DAMAGE_PER_DISMANTLE || tHeldDamage != 1 || !tCoverInInventory) {
			source.sendFailure(Component.literal("gt6tool dismantle FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("gt6tool dismantle check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Cutter-toggle the wire connection through the item's own dispatch (task
	 * p10-tool-cutter spec ③, the dismantle shape): the fake player holds a gt6:cutter,
	 * the synthetic hit lands at the block centre so the nine-grid resolves the clicked
	 * face itself, and the report asserts the :76 10000 return, the 1-point durability
	 * payment and the CONNECTIONS mask flip.
	 */
	private static int cut(CommandSourceStack source, BlockPos pos, Direction side) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		if (!(tLevel.getBlockEntity(tTarget) instanceof GTWireBlockEntity tWire)) {
			source.sendFailure(Component.literal("No GT6 wire BlockEntity at " + tTarget.toShortString()));
			return 0;
		}
		int tConnectionsBefore = tWire.getConnections();
		ItemStack tCutter = new ItemStack(GT6Tools.CUTTER.get());
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tCutter);
		// the same UseOnContext shape useOn receives: the synthetic centre hit pins the
		// clicked face at the block pos (isInside=false, the plain four-arg constructor)
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tTarget), side, tTarget, false));
		long tDamage = GTCutterItem.cutterToolClick(tContext);
		int tConnectionsAfter = tWire.getConnections();
		int tHeldDamage = tCutter.getDamageValue();
		String tReport = String.format("gt6tool cut face %s at %s: toolDamage=%d, cutterDamage=%d/%d, connections %d->%d",
				side, tTarget.toShortString(), tDamage, tHeldDamage, GTCutterItem.DURABILITY_POINTS,
				tConnectionsBefore, tConnectionsAfter);
		if (tDamage != GTCutterItem.TOOL_DAMAGE_PER_CUT || tHeldDamage != 1 || tConnectionsAfter == tConnectionsBefore) {
			source.sendFailure(Component.literal("gt6tool cut FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("gt6tool cut check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Prospect through the item's own dispatch (task p30-pool-prospector spec, the cut
	 * shape): the fake player holds a gt6:hammer, the synthetic hit pins the clicked face
	 * at the block pos, and the {@link GTHammerItem#useOn} arm runs the
	 * {@link GT6Prospector#prospect} single-source seam. The chat lines ride the report
	 * verbatim (RCON has no player to display to — the report IS the observable channel);
	 * the negative arm (nothing answers) is an EXPECTED report, not a sendFailure.
	 */
	private static int prospect(CommandSourceStack source, BlockPos pos, Direction side) {
		ServerLevel tLevel = source.getLevel();
		BlockPos tTarget = pos != null ? pos : BlockPos.containing(source.getPosition());
		ItemStack tHammer = new ItemStack(GT6Tools.HAMMER.get());
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover would fake the durability arm
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tHammer);
		// the same UseOnContext shape useOn receives: the synthetic centre hit pins the
		// clicked face at the block pos (isInside=false, the plain four-arg constructor)
		var tContext = new UseOnContext(tFakePlayer, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tTarget), side, tTarget, false));
		List<String> tLines = new ArrayList<>();
		long tDamage = GT6Prospector.prospect(tContext, tLines); // the item's useOn runs the identical call (null collector)
		int tHeldDamage = tHammer.getDamageValue();
		String tReport = String.format("gt6tool prospect face %s at %s: toolDamage=%d, hammerDamage=%d/%d, lines=%s",
				side, tTarget.toShortString(), tDamage, tHeldDamage, GTHammerItem.DURABILITY_POINTS, tLines);
		if (tDamage > 0 && tHeldDamage != 1) {
			source.sendFailure(Component.literal("gt6tool prospect FAILED: " + tReport));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("gt6tool prospect check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The machine-family material-ladder arm (task p31-machine-ladder, the
	 * GT6BladeToolCommand.stats shape): builds a stack, attaches the {@code GT.ToolStats}
	 * identity THE RECIPE WAY (the {@link GT6ToolLadder#stampIdentity} seam — the same
	 * face the gt6:material_tool rows assemble through), then reads the item surfaces
	 * BACK — the vanilla max-damage read, the class tint seam and the composed display
	 * name. The report IS the per-material verdict.
	 */
	private static int stats(CommandSourceStack aSource, String aTool, String aMaterial) {
		String tTool = aTool.toLowerCase(java.util.Locale.ROOT);
		Item tItem = switch (tTool) {
			case "wrench" -> GT6Tools.WRENCH.get();
			case "monkey_wrench" -> GT6Tools.MONKEY_WRENCH.get();
			case "screwdriver" -> GT6Tools.SCREWDRIVER.get();
			case "hammer" -> GT6Tools.HAMMER.get();
			case "soft_hammer" -> GT6Tools.SOFT_HAMMER.get();
			case "cutter" -> GT6Tools.CUTTER.get();
			case "saw" -> GT6Tools.SAW.get();
			case "chisel" -> GT6Tools.CHISEL.get();
			case "crowbar" -> GT6Tools.CROWBAR.get();
			case "pincers" -> GT6Tools.PINCERS.get();
			case "magnifying_glass" -> GT6Tools.MAGNIFYING_GLASS.get();
			default -> null;
		};
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6tool: not a ladder tool (wrench | monkey_wrench | screwdriver | hammer | soft_hammer | cutter | saw | chisel | crowbar | pincers | magnifying_glass): " + aTool));
			return 0;
		}
		OreDictMaterial tMaterial = resolveMaterial(aMaterial);
		if (tMaterial == null) {
			aSource.sendFailure(Component.literal("gt6tool: unknown material: " + aMaterial));
			return 0;
		}
		ItemStack tStack = new ItemStack(tItem);
		// the identity THE RECIPE WAY — the ONE stamp face the seam pinned (the serializer
		// and this arm route through it): primary + the form multiplier folded into j
		float tMultiplier = ((GT6ToolLadder.LadderTool) tItem).durabilityMultiplier();
		GT6ToolLadder.stampIdentity(tStack, tMaterial, tMultiplier);
		int tMaxDamage = tStack.getMaxDamage();
		int tTint = GT6ToolLadder.tintARGB(tStack, 0);
		String tName = tItem.getName(tStack).getString();
		String tReport = String.format("gt6tool stats %s@%s: maxDamage=%d, tint=0x%08X, name=%s",
				tTool, tMaterial.mNameInternal, tMaxDamage, tTint, tName);
		aSource.sendSuccess(() -> Component.literal("gt6tool stats check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The lenient material scan (the GT6BladeToolCommand shape — the RCON word arrives
	 * lower-case while the registry keys are camel), then the ALIAS MERGE: the alt-name
	 * slots (TungstenSteel mID -1 → 8635) resolve onto the registration target.
	 */
	private static OreDictMaterial resolveMaterial(String aWord) {
		OreDictMaterial tMaterial = MaterialRegistry.INSTANCE.byName(aWord);
		if (tMaterial == null) {
			for (OreDictMaterial tScan : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
				if (tScan.mNameInternal.equalsIgnoreCase(aWord)) {tMaterial = tScan; break;}
			}
		}
		if (tMaterial != null) tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
		if (tMaterial != null && tMaterial.mID < 0) return null;
		return tMaterial;
	}
}
