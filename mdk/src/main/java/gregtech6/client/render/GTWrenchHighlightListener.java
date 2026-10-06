package gregtech6.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.GTOvenBlock;
import gregtech6.block.pipe.GTFluidPipeBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The FORGE-bus listener of the wrench 3x3 grid overlay (task wrench-ui-gtceu) —
 * the first main-bus listener of this repo (the existing GTRodClientListener /
 * GTCoverClientListener are MOD-bus model-wiring and must not be copied; the GTCEu
 * precedent is ClientEventListener.java:55 {@code bus = FORGE, Dist.CLIENT}).
 *
 * <p>{@link RenderHighlightEvent.Block} fires on {@code MinecraftForge.EVENT_BUS},
 * client only (forge-api RenderHighlightEvent.java:103-104), right before the vanilla
 * selection box — the only sanctioned hook of the transient input-feedback exception
 * (ADR 2026-08-30-wrench-ui). The three constraints of that exception are honored
 * structurally: no BE/Level static references (the BE is fetched from the level per
 * frame and passed down by value), zero writes, the event is never cancelled so the
 * vanilla selection box stays.
 *
 * <p>Filter = the wrench-interaction key held in either hand — the shared
 * {@code GT6ToolActions.isWrenchInteractionKey} seam (the formal wrench or the
 * vanilla-hoe substitute; task wrench-interaction-key wired the wrench in), the same
 * predicate {@link GTFluidPipeBlock#use}, {@code GTItemPipeBlock#use} and
 * {@link GTOvenBlock#use} gate on — the "shown means clickable" invariant.
 * Hovering a {@link GTFluidPipeBlockEntity} (the connection/ioMask modes, task
 * wrench-ui-gtceu), a {@link TileEntityOven} (the front-rotation mode, task
 * oven-rotation — shift marks the rotatable cells, the same predicate
 * {@link GTOvenBlock#use} rotates through), a facing-machine block (the
 * transformer/dynamo/battery-box family, task wrench-interaction-chain symptom19 —
 * the upstream {@code TileEntityBase08Directional.isUsingWrenchingOverlay} :58 bare
 * wrench overlay plus the current-front mark; the family's own
 * {@code GT6ToolActions.WRENCH} key, not the substitute pool) or a
 * {@link TileEntityCokeOven} (the
 * structure ghost preview, tasks ghost-preview-poc / ghost-pattern-api /
 * ghost-render-match — a formed shell shows only its outer frame, an unformed one
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

		// the trigger predicate — the same shared seam GTFluidPipeBlock.use,
		// GTItemPipeBlock.use and GTOvenBlock.use gate on, so the grid is shown exactly
		// when a click would act ("shown means clickable")
		if (!isWrenchHeld(tPlayer, InteractionHand.MAIN_HAND) && !isWrenchHeld(tPlayer, InteractionHand.OFF_HAND)) return;

		BlockHitResult tTarget = aEvent.getTarget();
		BlockEntity tTile = tPlayer.level().getBlockEntity(tTarget.getBlockPos());

		PoseStack tPoseStack = aEvent.getPoseStack();
		Camera tCamera = aEvent.getCamera();
		MultiBufferSource tBuffers = aEvent.getMultiBufferSource();
		if (tTile instanceof GTFluidPipeBlockEntity tPipe) {
			// task wrench-interaction-key gap (declared defer): the item-pipe family is
			// clickable through the same key (GTItemPipeBlock.use) but shows no grid here —
			// its shift layer is the monkeyWrench four-state disable cycle
			// (mDisabledInputs/mDisabledOutputs), a different data face from the fluid
			// pipe's ioMask arrows, so the renderer arm is a renderer-domain card.
			GTWrenchGridRenderer.renderGrid(tPoseStack, tBuffers, tCamera, tTarget, tPlayer.isShiftKeyDown(), tPipe);
		} else if (tTile instanceof TileEntityOven tOven) {
			// task oven-rotation — the front facing reads the BlockState, the client
			// display authority: setBlock(state, 3) syncs the state without re-sending
			// the BE NBT, so the BE's own mFacing byte can be stale here
			byte tFrontFacing = (byte) tOven.getBlockState().getValue(GTOvenBlock.FACING).get3DDataValue();
			GTWrenchGridRenderer.renderOvenGrid(tPoseStack, tBuffers, tCamera, tTarget, tPlayer.isShiftKeyDown(), tFrontFacing);
		} else if (isFacingMachineHeld(tPlayer)) {
			// task wrench-interaction-chain, symptom19 "扳手指向变压器无九宫格" — the
			// facing-machine family (transformer / dynamo / battery box incl. the ZPM
			// decharger heir). Upstream trigger: a facing machine shows the wrench
			// overlay while its facing tool is held
			// (TileEntityBase08Directional.isUsingWrenchingOverlay :58
			// getFacingTool()==TOOL_wrench → TileEntityBase01Root.onDrawBlockHighlight
			// :995-1005 → RenderHelper.drawWrenchOverlay — the bare 3x3 grid). The
			// predicate here mirrors the family's click arm
			// (GT6ElectricTransformerBlock.wrenchRotate :171 keys
			// GT6ToolActions.WRENCH — the upstream getFacingTool shape, NOT the
			// substitute pool: the hoe never rotated a facing machine, so it must not
			// show this grid either — shown means clickable per family, both
			// hands compose like wrenchRotate's own hand read). The front mark rides
			// GTWrenchGridTables.machineFrontIcon; the BlockState is the display
			// authority (wrenchRotate writes flag 3, same rule as the oven arm above).
			BlockState tState = tPlayer.level().getBlockState(tTarget.getBlockPos());
			if (tState.getBlock() instanceof gregtech6.block.energy.GT6ElectricTransformerBlock
					|| tState.getBlock() instanceof gregtech6.block.energy.GT6DynamoBlock
					|| tState.getBlock() instanceof gregtech6.block.energy.GT6BatteryBoxBlock) {
				byte tFrontFacing = (byte) tState.getValue(gregtech6.block.energy.GT6ElectricTransformerBlock.FACING).get3DDataValue();
				GTWrenchGridRenderer.renderMachineGrid(tPoseStack, tBuffers, tCamera, tTarget, tFrontFacing);
			}
		} else if (tTile instanceof TileEntityCokeOven tOven) {
			// tasks ghost-preview-poc + ghost-pattern-api + ghost-render-match — the
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

	/** The use() predicate — the shared wrench-interaction key ({@link GT6ToolActions#isWrenchInteractionKey}). */
	private static boolean isWrenchHeld(Player aPlayer, InteractionHand aHand) {
		return GT6ToolActions.isWrenchInteractionKey(aPlayer.getItemInHand(aHand));
	}

	/**
	 * The facing-machine grid predicate — the {@code wrenchRotate} family key
	 * ({@code GT6ToolActions.WRENCH}, either hand), NOT the substitute-pool seam: the
	 * upstream facing machine answers {@code getFacingTool()} only
	 * (TileEntityBase08Directional :58 → Base09 onToolClick2 :72), so the vanilla-hoe
	 * substitute stays out of this family's display exactly as it stays out of its
	 * click arm (GT6ElectricTransformerBlock.wrenchRotate :171).
	 */
	private static boolean isFacingMachineHeld(Player aPlayer) {
		return aPlayer.getItemInHand(InteractionHand.MAIN_HAND).canPerformAction(GT6ToolActions.WRENCH)
				|| aPlayer.getItemInHand(InteractionHand.OFF_HAND).canPerformAction(GT6ToolActions.WRENCH);
	}
}
