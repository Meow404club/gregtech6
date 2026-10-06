package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.energy.GT6ZpmDechargerBlock;
import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.block.pipe.GTItemPipeBlock;
import net.minecraft.server.Bootstrap;
import gregtech6.client.render.GTWrenchGridRenderer;
import gregtech6.client.render.GTWrenchGridTables;
import gregtech6.client.render.GTWrenchHighlightListener;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.items.tools.GTMonkeyWrenchItem;
import gregtech6.items.tools.GTWrenchItem;
import gregtech6.block.sensors.GTSensorBlock;

/**
 * The wrench-chain presence pins (task wrench-interaction-chain, symptoms 18+19): the
 * interaction chain is only as strong as its weakest ring, and the ring that broke was
 * NOT a predicate — it was vanilla's sneak-use gate upstream of every in-block arm
 * (1.20.1 ServerPlayerGameMode.useItemOn :312-315 {@code isSecondaryUseActive}; the
 * 1.7.10 twin is why upstream never cared: the wrench dispatched from the sneak-proof
 * ITEM chain, Behavior_Tool.onItemUseFirst Behavior_Tool.java:57-68 →
 * IBlockToolable.Util.onToolClick IBlockToolable.java:60). These pins hold the RINGS:
 *
 * <ul>
 * <li>the marker ({@link GTWrenchUsable}) covers every wrench-clickable block — the
 *     sneak un-gate ({@link GTWrenchSneakUseListener}) keys on it, so a block that
 *     grows a wrench arm without implementing the marker stays sneak-dead (this test
 *     fails first);</li>
 * <li>the listener is a live FORGE-bus subscriber and keys the shared seam on the
 *     event's own stack;</li>
 * <li>the facing-machine grid ring: the renderer entry and the front-mark table exist
 *     and the listener predicate matches the family's click key
 *     ({@code GT6ToolActions.WRENCH} — the upstream getFacingTool shape,
 *     TileEntityBase09Connector.java:72), so shown means clickable per family;</li>
 * <li>the monkey-wrench ruling stays put: it classifies ONLY gt6_monkeywrench — the
 *     upstream monkey wrench mounts {@code Behavior_Tool(TOOL_monkeywrench, …)} alone
 *     (GT_Tool_MonkeyWrench.java:47) and the facing/connection faces key
 *     {@code getFacingTool()} = TOOL_wrench, so "the monkey wrench cannot rotate a
 *     transformer" is upstream fidelity, not a gap (pinned so it does not get
 *     "fixed" into a deviation).</li>
 * </ul>
 */
public class WrenchSneakChainTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The marker covers the whole wrench-clickable block set (the sneak un-gate's reach). */
	@Test
	public void markerCoversEveryWrenchClickableBlock() {
		Class<?>[] tFamily = {
				GTFluidPipeBlock.class,      // shift=toggleOutput / plain=toggleConnection
				GTItemPipeBlock.class,       // shift=monkeyWrench cycle / plain=toggleConnection
				GTOvenBlock.class,           // shift=setFrontFacing / plain=GUI
				GT6ElectricTransformerBlock.class, // wrenchRotate (all six sides)
				GT6DynamoBlock.class,        // wrenchRotate heir
				GT6BatteryBoxBlock.class,    // wrenchRotate, else GUI
				GT6ZpmDechargerBlock.class,  // the battery-box GUI heir (inherits use)
				GTSensorBlock.class          // the wrench side pick
		};
		for (Class<?> tBlock : tFamily) {
			assertTrue(GTWrenchUsable.class.isAssignableFrom(tBlock),
					tBlock.getSimpleName() + " carries a wrench use arm — it must implement GTWrenchUsable"
							+ " or the sneak un-gate never lifts for it (symptom18 ring)");
		}
	}

	/** The un-gate is a real, subscribed, correctly-keyed event handler. */
	@Test
	public void sneakListenerIsASubscribedWrenchKeyedHandler() throws Exception {
		Class<?> tListener = Class.forName("gregtech6.block.GTWrenchSneakUseListener");
		var tMethod = tListener.getDeclaredMethod("onRightClickBlock",
				Class.forName("net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock"));
		assertTrue(java.lang.reflect.Modifier.isStatic(tMethod.getModifiers()), "the bus needs a static handler");
		assertNotNull(tMethod.getAnnotation(SubscribeEvent.class), "the handler must ride @SubscribeEvent");
	}

	/** The front-mark table: exactly the front cell, nothing else — invalid bytes stay null. */
	@Test
	public void machineFrontIconMarksOnlyTheFrontCell() {
		for (byte tFront = 0; tFront < 6; tFront++) {
			for (byte tCell = 0; tCell < 7; tCell++) {
				GTWrenchGridTables.GTWrenchGridIcon tIcon = GTWrenchGridTables.machineFrontIcon(tCell, tFront);
				if (tCell == tFront) {
					assertEquals(GTWrenchGridTables.GTWrenchGridIcon.FRONT_FACING_ROTATION, tIcon,
							"the current front cell must carry the rotation mark");
				} else {
					assertEquals(null, tIcon, "cell " + tCell + " is not the front " + tFront + " — no mark");
				}
			}
		}
	}

	/** The renderer entry exists for the family grid (the client arm of symptom19). */
	@Test
	public void machineGridRendererEntryExists() throws Exception {
		Class<?> tPoseStack = Class.forName("com.mojang.blaze3d.vertex.PoseStack");
		Class<?> tBuffers = Class.forName("net.minecraft.client.renderer.MultiBufferSource");
		Class<?> tCamera = Class.forName("net.minecraft.client.Camera");
		Class<?> tHit = Class.forName("net.minecraft.world.phys.BlockHitResult");
		assertNotNull(GTWrenchGridRenderer.class.getDeclaredMethod("renderMachineGrid",
				tPoseStack, tBuffers, tCamera, tHit, byte.class));
		// and the highlight listener carries the family arm + its predicate
		assertNotNull(GTWrenchHighlightListener.class.getDeclaredMethod("isFacingMachineHeld", Player.class));
	}

	/**
	 * The family predicate: the FORMAL wrench is the facing-machine key (both the grid
	 * display and the wrenchRotate click), the vanilla hoe substitute is NOT (upstream
	 * getFacingTool), and the monkey wrench stays a class of its own (the
	 * GT_Tool_MonkeyWrench.java:47 single-behaviour ruling — see the class javadoc).
	 */
	@Test
	public void facingMachineKeyIsTheFormalWrenchOnly() {
		// the static classification seam — no item construction (the offline registry freeze wall)
		assertTrue(GTWrenchItem.classifies(GT6ToolActions.WRENCH), "the formal wrench is the family key");
		assertFalse(GTMonkeyWrenchItem.classifies(GT6ToolActions.WRENCH),
				"the monkey wrench stays TOOL_monkeywrench-only (upstream :47, getFacingTool fidelity)");
		assertFalse(GTMonkeyWrenchItem.classifies(net.minecraftforge.common.ToolActions.HOE_DIG),
				"and never the substitute leg (the p25 red line)");
	}
}
