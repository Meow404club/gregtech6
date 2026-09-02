package gregtech6.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.GTOvenBlock;
import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The FORGE-bus listener of the wrench 3x3 grid overlay (task p5-wrench-ui-gtceu) —
 * the first main-bus listener of this repo (the existing GTPipeFlowClientListener /
 * GTCoverClientListener are MOD-bus model-wiring and must not be copied; the GTCEu
 * precedent is ClientEventListener.java:55 {@code bus = FORGE, Dist.CLIENT}).
 *
 * <p>{@link RenderHighlightEvent.Block} fires on {@code MinecraftForge.EVENT_BUS},
 * client only (forge-api RenderHighlightEvent.java:103-104), right before the vanilla
 * selection box — the only sanctioned hook of the transient input-feedback exception
 * (ADR 2026-08-30-p5-wrench-ui). The three constraints of that exception are honored
 * structurally: no BE/Level static references (the BE is fetched from the level per
 * frame and passed down by value), zero writes, the event is never cancelled so the
 * vanilla selection box stays.
 *
 * <p>Filter = hoe held in either hand (the same {@code ToolActions.HOE_DIG} predicate
 * as {@link GTFluidPipeBlock#use} at :104 — the "shown means clickable" invariant)
 * hovering a {@link GTFluidPipeBlockEntity} (the connection/ioMask modes, task
 * p5-wrench-ui-gtceu), a {@link TileEntityOven} (the front-rotation mode, task
 * p6-oven-rotation — shift marks the rotatable cells, the same predicate
 * {@link GTOvenBlock#use} rotates through) or a {@link TileEntityCokeOven} (the
 * structure ghost preview, tasks p10-ghost-preview-poc / p12-ghost-pattern-api /
 * p12-ghost-render-match — a formed shell shows only its outer frame, an unformed one
 * per-cell translucent faces in green/red match colouring, judged fresh per frame
 * against the Level through the {@code Cell.matches} predicates; the pattern comes from
 * the {@code getStructurePattern()} controller binding, null = nothing drawn); shift
 * switches the display modes.
 * Bare hands and other items never show the grid.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
@OnlyIn(Dist.CLIENT)
public final class GTWrenchHighlightListener {

	private GTWrenchHighlightListener() {
	}

	@SubscribeEvent
	public static void onRenderHighlight(RenderHighlightEvent.Block aEvent) {
		Player tPlayer = Minecraft.getInstance().player;
		if (tPlayer == null) return;

		// the trigger predicate — identical to GTFluidPipeBlock.use:104, so the grid is
		// shown exactly when a click would act ("shown means clickable")
		if (!isWrenchHeld(tPlayer, InteractionHand.MAIN_HAND) && !isWrenchHeld(tPlayer, InteractionHand.OFF_HAND)) return;

		BlockHitResult tTarget = aEvent.getTarget();
		BlockEntity tTile = tPlayer.level().getBlockEntity(tTarget.getBlockPos());

		PoseStack tPoseStack = aEvent.getPoseStack();
		Camera tCamera = aEvent.getCamera();
		MultiBufferSource tBuffers = aEvent.getMultiBufferSource();
		if (tTile instanceof GTFluidPipeBlockEntity tPipe) {
			GTWrenchGridRenderer.renderGrid(tPoseStack, tBuffers, tCamera, tTarget, tPlayer.isShiftKeyDown(), tPipe);
		} else if (tTile instanceof TileEntityOven tOven) {
			// task p6-oven-rotation — the front facing reads the BlockState, the client
			// display authority: setBlock(state, 3) syncs the state without re-sending
			// the BE NBT, so the BE's own mFacing byte can be stale here
			byte tFrontFacing = (byte) tOven.getBlockState().getValue(GTOvenBlock.FACING).get3DDataValue();
			GTWrenchGridRenderer.renderOvenGrid(tPoseStack, tBuffers, tCamera, tTarget, tPlayer.isShiftKeyDown(), tFrontFacing);
		} else if (tTile instanceof TileEntityCokeOven tOven) {
			// tasks p10-ghost-preview-poc + p12-ghost-pattern-api + p12-ghost-render-match — the
			// structure ghost. Same BlockState rule: the FACING/FORMED pair of the state is the
			// client display authority (the BE's own mFacing/mStructureOkay can both be stale
			// here); the pattern comes from the controller binding (getStructurePattern, default
			// null = no declaration, nothing drawn) — checkStructure2 is never run on the client
			// (it carries the centre-cell removeBlock world write). The Level rides along by
			// value for the per-frame green/red match reads (GTMultiBlockGhostMatcher — reads
			// only, zero writes).
			GTMultiBlockPattern tPattern = tOven.getStructurePattern();
			if (tPattern != null) {
				BlockState tState = tOven.getBlockState();
				byte tFacing = (byte) tState.getValue(TileEntityBase10MultiBlockBase.FACING).get3DDataValue();
				boolean tFormed = tState.getValue(TileEntityBase10MultiBlockBase.FORMED);
				GTMultiBlockPreviewRenderer.renderPreview(tPoseStack, tBuffers, tCamera, tPlayer.level(),
						tTarget.getBlockPos(), tPattern, tFacing, tFormed);
			}
		}
		// no cancel — the vanilla selection box renders as usual
	}

	/** The use():104 predicate — an item that can perform the hoe-dig (wrench substitute) action. */
	private static boolean isWrenchHeld(Player aPlayer, InteractionHand aHand) {
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		return !tStack.isEmpty() && tStack.canPerformAction(ToolActions.HOE_DIG);
	}
}
