package gregtech6.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.ToolActions;

import gregtech6.block.GTBlockProperties;
import gregtech6.block.GTEntityBlock;
import gregtech6.client.render.GTRenderUpdates;
import gregtech6.registry.GTFluidPipes;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;
import gregtech6.util.UT6;

/**
 * The GT6 fluid pipe block (task p4-fluid-pipes spec ④) — the block side of the pipe
 * family over the shared BET (ADR-P3-1: one BlockEntityType mounting several blocks,
 * the GT6 "one TE class, many material blocks" counterpart). W1 shipped the two wood
 * tiers (the card fixes aStat=50 at 50 L / 300 L per tank; the upstream tiny/small/
 * medium multiplier row MultiTileEntityPipeFluid.java:92-94 is collapsed into the two
 * card-named tiers, other materials/tank-counts are a later card).
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask as a BlockState property — the
 * visual counterpart of {@code TileEntityBase09Connector.mConnections} (upstream
 * getDirectionData :98). The pipe BlockEntity writes it on every connection change
 * (onConnectionChange), and the datagen emits a variant per mask value (GT6 renders
 * its connections from the mask too, getTextureSide :522).
 *
 * <p>Flow-control interaction (task p4-pipe-flow-control spec ①) — the two-layer
 * {@code use} wiring over the upstream tool-click semantics:
 * <ul>
 * <li>hoe-class tool ({@code ToolActions.HOE_DIG} — the wrench substitute, the cover
 *     onCoverToolClick precedent) right click = the per-face connection toggle of
 *     upstream onToolClick2 (TileEntityBase09Connector.java:70-79): connected →
 *     disconnect, else connect;</li>
 * <li>hoe + shift = the per-face output-arrow toggle (the monkeywrench output layer,
 *     MultiTileEntityPipeItem.java:128-153 single-layered);</li>
 * <li>the target face is {@code UT6.getSideWrenching} over the 0..1 hit offsets
 *     (upstream UT.java:1776-1798 — clicked face + edge thresholds + OPOS corner
 *     fallback);</li>
 * <li>everything else passes through. The BE runs server-side only; the client returns
 *     CONSUME to claim the interaction.</li>
 * </ul>
 *
 * <p>Ownership face (task p24-pipe-owner): a locked pipe ({@code mOwnable} with a set
 * {@code mOwner}) drops its own tools — the hoe use returns PASS before any mutation
 * (the upstream TileEntityBase06Covers.java:141 host-tool-kill counterpart) and the
 * vanilla break progress is denied to 0.0F through the
 * {@link GTFluidPipeBlockEntity#ownerDestroyProgress} seam (the upstream
 * TileEntityBase01Root.java:941-943 getPlayerRelativeBlockHardness counterpart; creative
 * bypasses, upstream-consistent). Default {@code mOwnable=false} keeps the plain pipe
 * byte-for-byte.</p>
 *
 * <p>{@link #triggerEvent} is the consumer-side wiring of the C-grade render-update
 * pair (GTRenderUpdates.java:31-46 template): the server-side
 * {@code scheduleRenderUpdate} bounces here as a blockEvent, and the client block
 * forwards it back into the client scheduleRenderUpdate pair. NO onRemove override —
 * the BaseEntityBlock kill+recreate lesson (remember id59).
 */
public class GTFluidPipeBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final IntegerProperty CONNECTIONS = GTBlockProperties.CONNECTIONS;

	private final long mCapacityPerTank;

	public GTFluidPipeBlock(long aCapacityPerTank, Properties aProperties) {
		super(aProperties);
		mCapacityPerTank = aCapacityPerTank;
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTFluidPipeBlock> codec() {
		return simpleCodec(aProperties -> new GTFluidPipeBlock(50, aProperties));
	}
	*///?}

	/** Per-tank capacity in Liters (upstream NBT_TANK_CAPACITY :114). */
	public long capacityPerTank() {
		return mCapacityPerTank;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTFluidPipes.FLUID_PIPE_BE.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(CONNECTIONS);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	// ---------------------------------------------------------------------------
	// dried-foam physical face (task p25-c-foam-pipe-spray spec ⑥ — the block-level
	// counterparts of the upstream 10ConnectorRendered dried swaps: collision :218
	// addDefaultCollisionBoxToList / light :144 getLightOpacity → LIGHT_OPACITY_MAX;
	// both overrides are BE lookup + unwrap only, the decision lives in the static
	// seams the offline tests drive)
	// ---------------------------------------------------------------------------

	/**
	 * Dried foam collides as a FULL block (the hardened-foam walk-on surface; upstream
	 * :218 with the diameter arm cut — deviation A). 1.20.1 BlockBehaviour.java:290 public /
	 * 1.21.1:321 protected — the public widening is the legal both-legs shape (the
	 * getDestroyProgress override below, same treatment).
	 */
	@Override
	public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel,
			BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		BlockEntity tTile = aLevel.getBlockEntity(aPos);
		return GTFluidPipeBlockEntity.foamCollisionShape(
				tTile instanceof GTFluidPipeBlockEntity tPipe ? tPipe : null,
				super.getCollisionShape(aState, aLevel, aPos, aContext));
	}

	/**
	 * Dried foam blocks ALL light (upstream :144 LIGHT_OPACITY_MAX). 1.20.1
	 * BlockBehaviour.java:255 public (the TintedGlassBlock.java:19 override precedent) /
	 * 1.21.1:292 protected, same params — the public widening is the legal both-legs shape
	 * (deviation C, verified against tmp/vanilla-1.20.1 + tmp/refs/vanilla-mc/1.21.1).
	 */
	@Override
	public int getLightBlock(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
		BlockEntity tTile = aLevel.getBlockEntity(aPos);
		return GTFluidPipeBlockEntity.foamLightBlock(
				tTile instanceof GTFluidPipeBlockEntity tPipe ? tPipe : null,
				super.getLightBlock(aState, aLevel, aPos));
	}

	// ---------------------------------------------------------------------------
	// break gate (task p24-pipe-owner — the vanilla BlockBehaviour.getDestroyProgress
	// 1.20.1:319-327 public / 1.21.1:343 protected override, the BambooStalkBlock
	// 1.20.1:184 precedent; the upstream break-gate counterpart is
	// TileEntityBase01Root.java:941-943 getPlayerRelativeBlockHardness deny-to-0)
	// ---------------------------------------------------------------------------

	/**
	 * The break-ownership gate: the override body is only the BE lookup + the
	 * Player-to-UUID unwrap; the deny/super decision delegates to the static seam
	 * {@link GTFluidPipeBlockEntity#ownerDestroyProgress} — deny = 0.0F (progress never
	 * accrues, the upstream :943 {@code : 0} counterpart), allow = the super value
	 * unchanged. Creative bypasses this whole path (ServerPlayerGameMode :241 isCreative
	 * destroys immediately — the upstream creative behaviour, not a defect). The 1.20.1
	 * parent marks the method @Deprecated (the vanilla-idiomatic direct-call note); the
	 * override IS the intended consumption (the card risk note, BambooStalkBlock precedent),
	 * and the public widening over the 1.21.1 protected form is the legal both-legs shape.
	 */
	@Override
	public float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aLevel, BlockPos aPos) {
		BlockEntity tTile = aLevel.getBlockEntity(aPos);
		return GTFluidPipeBlockEntity.ownerDestroyProgress(
				tTile instanceof GTFluidPipeBlockEntity tPipe ? tPipe : null,
				super.getDestroyProgress(aState, aPlayer, aLevel, aPos),
				aPlayer.getUUID());
	}

	// ---------------------------------------------------------------------------
	// flow-control interaction (spec ①)
	// ---------------------------------------------------------------------------

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249); the game loop drives MAIN_HAND first.
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		if (tStack.isEmpty() || !tStack.canPerformAction(ToolActions.HOE_DIG)) return InteractionResult.PASS;
		if (aLevel.isClientSide) return InteractionResult.CONSUME; // claim the interaction, the BE executes server-side

		BlockEntity tTile = aLevel.getBlockEntity(aPos);
		if (!(tTile instanceof GTFluidPipeBlockEntity tPipe)) return InteractionResult.PASS;
		// the ownership gate (task p24-pipe-owner): upstream TileEntityBase06Covers.java:141
		// kills every tool on a locked host before dispatch — here the locked pipe simply
		// stops treating the wrench as a tool: PASS, no CONSUME, zero mutation (the upstream
		// tool-unhandled semantics). Ownable=false short-circuits allowInteraction, so the
		// plain-pipe path is untouched.
		if (!tPipe.allowInteraction(aPlayer.getUUID())) return InteractionResult.PASS;
		byte tClickedSide = (byte)aHit.getDirection().get3DDataValue();
		// the 0..1 in-face offsets — upstream passes the raw hit fractions of the clicked face
		float tHitX = (float)(aHit.getLocation().x - aPos.getX());
		float tHitY = (float)(aHit.getLocation().y - aPos.getY());
		float tHitZ = (float)(aHit.getLocation().z - aPos.getZ());
		byte tTargetSide = UT6.getSideWrenching(tClickedSide, tHitX, tHitY, tHitZ);

		if (aPlayer.isShiftKeyDown()) {
			tPipe.toggleOutput(tTargetSide); // shift+right-click = the output arrow (monkeywrench layer)
		} else {
			tPipe.toggleConnection(tTargetSide); // right-click = the wrench connection toggle (onToolClick2)
		}
		return InteractionResult.CONSUME;
	}

	// ---------------------------------------------------------------------------
	// render-update event forward (GTRenderUpdates.java:31-46 consumer template)
	// ---------------------------------------------------------------------------

	@Override
	public boolean triggerEvent(BlockState aState, Level aLevel, BlockPos aPos, int aId, int aParam) {
		if (aId == GTRenderUpdates.RENDER_UPDATE_EVENT_ID && aLevel.isClientSide) {
			BlockEntity tTile = aLevel.getBlockEntity(aPos);
			if (tTile != null) {
				GTRenderUpdates.scheduleRenderUpdate(tTile);
				return true;
			}
		}
		return super.triggerEvent(aState, aLevel, aPos, aId, aParam);
	}
}
