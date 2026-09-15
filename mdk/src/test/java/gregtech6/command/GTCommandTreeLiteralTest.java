package gregtech6.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.RegisterCommandsEvent;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.example.GTExampleChestCommand;
import gregtech6.tileentity.machines.GTMachineCommand;

/**
 * Task p11-gt6machine-literal-fix: the double-registration literal hijack regression guard.
 *
 * <p>History: GTExampleChestCommand (p3) and GTMachineCommand (p7) both registered the root
 * literal {@code gt6machine}. Brigadier's CommandNode.addChild silently MERGES same-name
 * literals into one dispatch node (the later child's command overwrites on colliding names —
 * no exception), so {@code /gt6machine} became one shared dispatch position for two features
 * and the bare {@code /gt6machine check} slot resolved to the chest's open-chain check.
 * The fix renames the chest proof to {@code /gt6chest} (the port's one-feature-one-root
 * convention: gt6wire/gt6cover/gt6tool/gt6oven/...).
 *
 * <p>These tests register BOTH production listeners through the real
 * {@link RegisterCommandsEvent} into one dispatcher (the production shape) and assert the
 * two families own disjoint root literals and each intent parses to its own subtree. On
 * pre-fix main the first assertions fail: the merged tree has a single {@code gt6machine}
 * root child and no {@code gt6chest}.
 *
 * <p>Headless: registration only touches builders/deferred registry objects, and
 * {@code CommandDispatcher.parse} evaluates requirements via
 * {@code CommandSourceStack.hasPermission} (a plain permission-level field compare —
 * CommandSourceStack.java:425), so a level-2 stack over {@link CommandSource#NULL} with
 * null level/server parses without any bootstrap.
 */
public class GTCommandTreeLiteralTest {

	/**
	 * The machine-family root's exact subtree (p7 12 machine literals + p8 fakesource;
	 * the p14 dryer ladder grew it to 16 — the census follows the tree, task
	 * p14-dryer-family; p21 grew it again with the paint write-point arm).
	 */
	private static final Set<String> MACHINE_ROOT_CHILDREN = Set.of("fakesource",
		"shredder", "shredder_t2", "shredder_t3", "shredder_t4",
		"crusher", "crusher_t2", "crusher_t3", "crusher_t4",
		"lathe", "lathe_t2", "lathe_t3", "lathe_t4",
		"dryer", "dryer_t2", "dryer_t3", "dryer_t4",
		"distillery", "distillery_t2", "distillery_t3", "distillery_t4", // task p16-distillery-family
		"canner", "canner_t2", "canner_t3", "canner_t4", // task p24-canner-machine — the Canner ladder
		"sifter", "sifter_t2", "sifter_t3", "sifter_t4", // task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio
		"compressor", "compressor_t2", "compressor_t3", "compressor_t4",
		"wiremill", "wiremill_t2", "wiremill_t3", "wiremill_t4",
		"press", "press_t2", "press_t3", "press_t4", // task p26-w1-press-extruder-molds — the Press ladder
		"extruder", "extruder_t2", "extruder_t3", "extruder_t4", // task p26-w1-press-extruder-molds — the Extruder ladder
		"rollingmill_t1", "rollingmill_t2", "rollingmill_t3", "rollingmill_t4", // task p29-w1-kinetic-roll-ladder — the RU RollingMill ladder (the p28 ULV rung has no arm)
		"rollbender", "rollbender_t2", "rollbender_t3", "rollbender_t4", // task p29-w1-kinetic-roll-ladder — the Roll Bender ladder
		"rollformer", "rollformer_t2", "rollformer_t3", "rollformer_t4", // task p29-w1-kinetic-roll-ladder — the Roll Former ladder
		"clustermill", "clustermill_t2", "clustermill_t3", "clustermill_t4", // task p29-w1-kinetic-roll-ladder — the Cluster Mill ladder
		"mixer", "mixer_t2", "mixer_t3", "mixer_t4", // task p29-w1-eu-hu-families — the eu-hu families
		"electricmixer", "electricmixer_t2", "electricmixer_t3", "electricmixer_t4",
		"electricloom", "electricloom_t2", "electricloom_t3", "electricloom_t4",
		"electricsifter", "electricsifter_t2", "electricsifter_t3", "electricsifter_t4",
		"boxinator", "boxinator_t2", "boxinator_t3", "boxinator_t4",
		"unboxinator", "unboxinator_t2", "unboxinator_t3", "unboxinator_t4",
		// task p29-w2-hu-tu-piggyback — the seven hu-tu families
		"steamcracker", "steamcracker_t2", "steamcracker_t3", "steamcracker_t4",
		"catalyticcracker", "catalyticcracker_t2", "catalyticcracker_t3", "catalyticcracker_t4",
		"coagulator", "generifier", "bath", "autoclave",
		"loom", "loom_t2", "loom_t3", "loom_t4",
		"fermenter", // task p29-w1-eu-hu-families — the single-variant rung
		// task p29-w3-heat-smelter — the Smelter ladder + the Melter single
		"smelter", "smelter_t2", "smelter_t3", "smelter_t4", "melter",
		"roasting_oven", "roasting_oven_t2", "roasting_oven_t3", "roasting_oven_t4", // task p29-w4-eu-bridge — the Roasting ladder
		"paint", "unpaint"); // task p21-paintable-storage-sync — the spray write-point arm

	private static CommandSourceStack stack() {
		// permission level 2 satisfies both commands' requires(...) gate; level/server are
		// only stored (CommandSourceStack ctor :64-70) and parse never dereferences them.
		return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "test",
			Component.literal("test"), null, null);
	}

	private static CommandDispatcher<CommandSourceStack> registerBoth() {
		CommandDispatcher<CommandSourceStack> tDispatcher = new CommandDispatcher<>();
		RegisterCommandsEvent tEvent = new RegisterCommandsEvent(tDispatcher, Commands.CommandSelection.ALL, null);
		GTExampleChestCommand.onRegisterCommands(tEvent);
		GTMachineCommand.onRegisterCommands(tEvent);
		return tDispatcher;
	}

	/**
	 * The vanilla boot (the GTMachinesOfflineTestBase :47 idiom, added by task
	 * p14-dryer-family): registration does build only builders, but it class-initializes
	 * {@code GTMachines} (the RegistryObject references) whose {@code <clinit>} needs
	 * ForgeRegistries — a bootable JVM state. The old "without any bootstrap" claim above
	 * held only while test-class ORDER kept this class behind a bootstrapping class in the
	 * shared test JVM; the p14 dryer test class reshuffled that order and exposed the
	 * dependency.
	 */
	@BeforeAll
	static void bootVanilla() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The deepest resolved node of a parse — the dispatch slot the input lands on. */
	private static CommandNode<CommandSourceStack> deepest(ParseResults<CommandSourceStack> aResults) {
		CommandNode<CommandSourceStack> tNode = null;
		for (ParsedCommandNode<CommandSourceStack> tParsed : aResults.getContext().getNodes()) tNode = tParsed.getNode();
		return tNode;
	}

	private static Set<String> childNames(CommandNode<CommandSourceStack> aNode) {
		Set<String> tNames = new LinkedHashSet<>();
		for (CommandNode<CommandSourceStack> tChild : aNode.getChildren()) tNames.add(tChild.getName());
		return tNames;
	}

	@Test
	public void twoFamiliesOwnDisjointRootLiterals() {
		CommandDispatcher<CommandSourceStack> tDispatcher = registerBoth();
		assertEquals(3, tDispatcher.getRoot().getChildren().size(), "exactly the three family roots (a duplicate-root registration would silently merge into ONE node)");
		CommandNode<CommandSourceStack> tChest = tDispatcher.getRoot().getChild("gt6chest");
		CommandNode<CommandSourceStack> tMachine = tDispatcher.getRoot().getChild("gt6machine");
		CommandNode<CommandSourceStack> tBridge = tDispatcher.getRoot().getChild("gt6bridge");
		assertNotNull(tChest, "the chest open-chain proof must have its own root literal");
		assertNotNull(tMachine, "the machine family must keep /gt6machine");
		assertNotNull(tBridge, "the EU-bridge converter family keeps /gt6bridge (task p29-w4-eu-bridge)");
		assertNotEquals(tChest, tMachine);
		assertNotEquals(tMachine, tBridge);
	}

	@Test
	public void chestIntentsParseToTheChestSubtree() {
		CommandDispatcher<CommandSourceStack> tDispatcher = registerBoth();
		CommandNode<CommandSourceStack> tChest = tDispatcher.getRoot().getChild("gt6chest");
		assertEquals(Set.of("open", "check"), childNames(tChest));
		assertEquals(tChest.getChild("open"), deepest(tDispatcher.parse("gt6chest open", stack())),
			"the player open intent lands on the chest open literal");
		assertEquals(tChest.getChild("check"), deepest(tDispatcher.parse("gt6chest check", stack())),
			"the console check intent lands on the chest check literal");
	}

	@Test
	public void machineIntentsParseToTheMachineSubtree() {
		CommandDispatcher<CommandSourceStack> tDispatcher = registerBoth();
		CommandNode<CommandSourceStack> tMachine = tDispatcher.getRoot().getChild("gt6machine");
		assertEquals(MACHINE_ROOT_CHILDREN, childNames(tMachine));
		assertEquals(tMachine.getChild("crusher").getChild("check"),
			deepest(tDispatcher.parse("gt6machine crusher check", stack())),
			"the machine state-report check intent lands on the machine's own check literal");
		assertEquals(tMachine.getChild("fakesource").getChild("stat"),
			deepest(tDispatcher.parse("gt6machine fakesource stat", stack())),
			"the regime switch lands on the machine's own fakesource literal");
	}

	@Test
	public void noLiteralIsSharedBetweenTheTwoFamilies() {
		CommandDispatcher<CommandSourceStack> tDispatcher = registerBoth();
		Set<String> tChest = childNames(tDispatcher.getRoot().getChild("gt6chest"));
		Set<String> tMachine = childNames(tDispatcher.getRoot().getChild("gt6machine"));
		assertTrue(Collections.disjoint(tChest, tMachine),
			"no subcommand literal may be claimed by both families (the merged-tree overwrite hazard)");
	}
}
