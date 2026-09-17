package gregtech6.command;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import gregtech6.registry.GT6Tools;
import gregtech6.items.tools.GTButcheryKnifeItem;
import gregtech6.items.tools.GTKnifeItem;
import gregtech6.items.tools.GTSwordItem;
import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * {@code /gt6blade} — the blade-tool acceptance command (task p29-w5-t2-blade-six;
 * card-local in command/ like GT6DigToolCommand — the fake-player channel: the RCON
 * arms drive the EXACT item surfaces the keyboard player hits, the "the command IS the
 * acceptance channel" ruling):
 *
 * <ul>
 * <li>{@code mine <pos> <tool> [hand]} — the drop face (the GT6DigToolCommand.mine
 *     verbatim shape: Block.playerDestroy + removeBlock — the loot table consults the
 *     TOOL context, the GLOBAL loot modifier chain fires here); the spawned drops are
 *     counted, DISCARDED (rerun idempotency) and listed — the report IS the conversion
 *     verdict (the sword grass/vine + the club rockGt legs).</li>
 * <li>{@code chop <pos> <tool>} — the REAL break face:
 *     {@code ServerPlayerGameMode.destroyBlock} (the tryHarvestBlock channel the felling
 *     modifier walks — instant break, correct-tool drops, the vanilla mineBlock
 *     durability payment per log); the wide-radius report names the drops AND the tool
 *     damage — the whole-tree leg asserts the felled count AND the payment = the log
 *     count.</li>
 * <li>{@code stats <tool> <material>} (task p31-blade-ladder) — the material-ladder
 *     arm: builds a stack, attaches the {@link GT6ToolStats#KEY} identity THE RECIPE WAY
 *     (primary = the material, secondary = {@code mHandleMaterial}, the shape
 *     {@code DURABILITY_MULTIPLIER}) through {@link GT6ItemData#set}, then reads the
 *     item surfaces BACK (the vanilla max-damage read, the vanilla attribute map, the
 *     class tint seam) — the report IS the per-material 伤害/耐久/tint verdict. Restricted
 *     to the converted family (the p31 card ruling: axe/axe_double stay single-tier).</li>
 * </ul>
 *
 * <p>Tool ids: sword / knife / butchery_knife / club / axe / axe_double (the GT6Tools
 * registry paths, snake).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GT6BladeToolCommand {

	private static final Logger LOGGER = LogUtils.getLogger();

	private GT6BladeToolCommand() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
		LiteralArgumentBuilder<CommandSourceStack> tBlade = Commands.literal("gt6blade")
			.requires(aSource -> aSource.hasPermission(2))
			.then(Commands.literal("mine")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"), false))
						.then(Commands.literal("hand")
							.executes(aContext -> mine(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
									"bare", true))))))
			.then(Commands.literal("chop")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> chop(aContext.getSource(), BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"))))))
			.then(Commands.literal("stats")
				.then(Commands.argument("tool", com.mojang.brigadier.arguments.StringArgumentType.word())
					.then(Commands.argument("material", com.mojang.brigadier.arguments.StringArgumentType.word())
						.executes(aContext -> stats(aContext.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "tool"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(aContext, "material"))))));
		aEvent.getDispatcher().register(tBlade);
		LOGGER.info("Registered GT6 blade-tool acceptance command /gt6blade (mine [hand] | chop | stats)");
	}

	/** The snake id → the registered tool item (the GT6Tools registry face). */
	private static Item toolItem(String aTool) {
		return switch (aTool.toLowerCase(Locale.ROOT)) {
			case "sword" -> GT6Tools.SWORD.get();
			case "knife" -> GT6Tools.KNIFE.get();
			case "butchery_knife" -> GT6Tools.BUTCHERY_KNIFE.get();
			case "club" -> GT6Tools.CLUB.get();
			case "axe" -> GT6Tools.AXE.get();
			case "axe_double" -> GT6Tools.AXE_DOUBLE.get();
			default -> null;
		};
	}

	/**
	 * The material-ladder arm (task p31-blade-ladder): identity attached the recipe way,
	 * the item surfaces read BACK — the per-material 伤害/耐久/tint verdict in one line.
	 */
	private static int stats(CommandSourceStack aSource, String aTool, String aMaterial) {
		String tTool = aTool.toLowerCase(Locale.ROOT);
		Item tItem = switch (tTool) {
			case "sword" -> GT6Tools.SWORD.get();
			case "knife" -> GT6Tools.KNIFE.get();
			case "butchery_knife" -> GT6Tools.BUTCHERY_KNIFE.get();
			default -> null;
		};
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6blade: not a ladder tool (sword | knife | butchery_knife): " + aTool));
			return 0;
		}
		// the lenient material scan (the GT6CrucibleCommand.drop shape — the RCON word
		// arrives lower-case while MATERIAL_MAP keys are the camel-case internal names)
		OreDictMaterial tMaterial = MaterialRegistry.INSTANCE.byName(aMaterial);
		if (tMaterial == null) {
			for (OreDictMaterial tScan : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
				if (tScan.mNameInternal.equalsIgnoreCase(aMaterial)) {tMaterial = tScan; break;}
			}
		}
		if (tMaterial == null || tMaterial.mID < 0) {
			aSource.sendFailure(Component.literal("gt6blade: unknown material: " + aMaterial));
			return 0;
		}
		ItemStack tStack = new ItemStack(tItem);
		// the identity THE RECIPE WAY: primary, handle = mHandleMaterial (the default = self),
		// the shape multiplier folded into the j payload (MultiItemTool.java:182)
		float tMultiplier = switch (tTool) {
			case "sword" -> GTSwordItem.DURABILITY_MULTIPLIER;
			case "knife" -> GTKnifeItem.DURABILITY_MULTIPLIER;
			default -> GTButcheryKnifeItem.DURABILITY_MULTIPLIER;
		};
		GT6ItemData.set(tStack, GT6ToolStats.KEY, GT6ToolStats.of(tMaterial, tMaterial.mHandleMaterial, tMultiplier));
		// read the item surfaces BACK (the faces the gameplay code uses)
		int tMaxDamage = tStack.getMaxDamage();
		int tTint = switch (tTool) {
			case "sword" -> GTSwordItem.tintARGB(tStack, 0);
			case "knife" -> GTKnifeItem.tintARGB(tStack, 0);
			default -> GTButcheryKnifeItem.tintARGB(tStack, 0);
		};
		// the attack read rides the VANILLA per-stack attribute map — the end-to-end face
		// of the item's getAttributeModifiers(getDefaultAttributeModifiers) override (the
		// GT6ToolLadder.attackDamage math is its source; the offline test pins that half)
		//? if forge {
		double tAttack = tStack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
				.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
				.stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
		//?} else {
		/*double[] tSum = {0};
		tStack.getAttributeModifiers().forEach(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
				(aHolder, aMod) -> { if (aHolder.value() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) tSum[0] += aMod.amount(); });
		double tAttack = tSum[0];
		*///?}
		String tReport = String.format("gt6blade stats %s@%s: attack=%.1f, maxDamage=%d, tint=0x%08X",
				tTool, tMaterial.mNameInternal, tAttack, tMaxDamage, tTint);
		aSource.sendSuccess(() -> Component.literal("gt6blade stats check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/** The mining-drop face (the GT6DigToolCommand.mine shape): the drops list IS the verdict. */
	private static int mine(CommandSourceStack aSource, BlockPos aPos, String aTool, boolean aBareHand) {
		ServerLevel tLevel = aSource.getLevel();
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(tState.getBlock()));
		ItemStack tTool = ItemStack.EMPTY;
		if (!aBareHand) {
			Item tItem = toolItem(aTool);
			if (tItem == null) {
				aSource.sendFailure(Component.literal("gt6blade: unknown tool id: " + aTool));
				return 0;
			}
			tTool = new ItemStack(tItem);
		}
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent(); // a leftover from an earlier command would fake the arm
		if (!tTool.isEmpty()) tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		// snapshot the pre-existing drops so reruns/leftovers cannot double-count
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			tBefore.add(tEntity.getId());
		}
		// the drop face: dropResources consults the loot table with THIS_ENTITY + TOOL
		// context — the GLOBAL loot modifier chain fires here
		BlockEntity tBE = tLevel.getBlockEntity(aPos);
		tState.getBlock().playerDestroy(tLevel, tFakePlayer, aPos, tState, tBE, tTool);
		tLevel.removeBlock(aPos, false);
		// collect and DISCARD the new drops (the report is the assertion surface; counts
		// MERGE per item id — the felling spawns five separate x1 entities, the string-set
		// form collapsed them into one "x1" line, the live-calibration lesson)
		java.util.Map<String, Integer> tDrops = new java.util.TreeMap<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(2.0))) {
			if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDrops.merge(ForgeRegistries.ITEMS.getKey(tStack.getItem()) + " x", tStack.getCount(), Integer::sum);
			tEntity.discard();
		}
		tFakePlayer.getInventory().clearContent();
		String tDropsText = tDrops.entrySet().stream().map(tEntry -> tEntry.getKey() + tEntry.getValue())
				.collect(java.util.stream.Collectors.joining(", "));
		String tReport = String.format("gt6blade mine %s hand=%s on %s at %s: drops=[%s], toolDamage=%d",
				aBareHand ? "hand" : aTool, aBareHand, tBlockId, aPos.toShortString(), tDropsText, tTool.getDamageValue());
		aSource.sendSuccess(() -> Component.literal("gt6blade mine check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * The real break face — {@code ServerPlayerGameMode.destroyBlock} (the channel the
	 * felling modifier walks): instant break, the correct-tool drops, the vanilla
	 * mineBlock payment. The report is SELF-CONTAINED (the {code execute...run say}
	 * broadcast never rides the RCON response — the live-calibration lesson): the felled
	 * verdict = the STANDING-LOG COUNT above the base (the whole-tree face) + the tool
	 * damage (the payment = the felled count) + the spawned drops (the wide radius 6).
	 */
	private static int chop(CommandSourceStack aSource, BlockPos aPos, String aTool) {
		ServerLevel tLevel = aSource.getLevel();
		Item tItem = toolItem(aTool);
		if (tItem == null) {
			aSource.sendFailure(Component.literal("gt6blade: unknown tool id: " + aTool));
			return 0;
		}
		BlockState tState = tLevel.getBlockState(aPos);
		String tBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(tState.getBlock()));
		ItemStack tTool = new ItemStack(tItem);
		var tFakePlayer = FakePlayerFactory.getMinecraft(tLevel);
		tFakePlayer.getInventory().clearContent();
		tFakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tTool);
		Set<Integer> tBefore = new HashSet<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(6.0))) {
			tBefore.add(tEntity.getId());
		}
		boolean tBroke = tFakePlayer.gameMode.destroyBlock(aPos);
		java.util.Map<String, Integer> tDrops = new java.util.TreeMap<>();
		for (ItemEntity tEntity : tLevel.getEntitiesOfClass(ItemEntity.class, new AABB(aPos).inflate(6.0))) {
			if (tBefore.contains(tEntity.getId()) || tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			tDrops.merge(ForgeRegistries.ITEMS.getKey(tStack.getItem()) + " x", tStack.getCount(), Integer::sum);
			tEntity.discard();
		}
		// the standing-log count above the base (the felled face — the vanilla
		// natural-leaf/branch cases stay out: the trunk column IS the test rig)
		int tRemaining = 0;
		if (tBroke) {
			for (int tY = 1; tY <= 32; tY++) {
				if (tLevel.getBlockState(aPos.above(tY)).is(net.minecraft.tags.BlockTags.LOGS)) tRemaining++;
			}
		}
		int tDamage = tFakePlayer.getMainHandItem().getItem() == tItem ? tFakePlayer.getMainHandItem().getDamageValue() : tTool.getMaxDamage();
		tFakePlayer.getInventory().clearContent();
		String tDropsText = tDrops.entrySet().stream().map(tEntry -> tEntry.getKey() + tEntry.getValue())
				.collect(java.util.stream.Collectors.joining(", "));
		String tReport = String.format("gt6blade chop %s on %s at %s: broke=%s, remainingLogs=%d, drops=[%s], toolDamage=%d/%d",
				aTool, tBlockId, aPos.toShortString(), tBroke, tRemaining, tDropsText, tDamage, tTool.getMaxDamage());
		if (!tBroke) {
			aSource.sendFailure(Component.literal("gt6blade chop FAILED (destroyBlock returned false): " + tReport));
			return 0;
		}
		aSource.sendSuccess(() -> Component.literal("gt6blade chop check OK: " + tReport), false);
		LOGGER.info(tReport);
		return Command.SINGLE_SUCCESS;
	}
}
