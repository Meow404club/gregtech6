package gregtech6.block.tools;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6MortarBlockEntity;

/**
 * The mortar block carrier — the block side of the five-variant mortar family (task
 * mortar-family), the GTAnvilBlock carrier pattern: the block carries the registration
 * values upstream wrote into the MTE definition NBT — here the {@code NBT_DESIGN} row's
 * pestle material (the tint seat 1 and the crafting ingredient; the body is the constant
 * {@code MT.Ceramic} column every row shares, Loader_MultiTileEntities.java:2179-2183,
 * riding {@link GT6Mortars#BODY_MATERIAL}) — plus the family BET (one shared BET over
 * all five rows, ADR-P3-1).
 *
 * <p>NO state properties: the upstream mortar is facing-symmetric (no FACING read
 * anywhere in MultiTileEntityMortar.java:76-180) — the single-state blockstate, the
 * {@code simpleBlock} form. The shape is the upstream collision/selection pool verbatim:
 * the bowl envelope box {@code (PX_P[2], 0, PX_P[2]) → (PX_N[2], PX_N[10], PX_N[2])}
 * (MultiTileEntityMortar.java:167-169 — the five-pool union IS this envelope), the 12px
 * footprint × 6px rim.
 *
 * <p>{@code use} is the one-click manual-interaction face (the GTAnvilBlock form): the
 * whole upstream onBlockActivated3 (:76-106) rides the BE's
 * {@link GT6MortarBlockEntity#activateChain} — which doubles as the RCON acceptance
 * channel (a null player = the report path).
 */
public class GT6MortarBlock extends GTEntityBlock {

	/**
	 * The bowl envelope — the upstream collision/selection pool verbatim
	 * (MultiTileEntityMortar.java:167-169): x/z 2..14px, y 0..6px.
	 */
	public static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.box(
			2.0 / 16.0, 0.0, 2.0 / 16.0, 14.0 / 16.0, 6.0 / 16.0, 14.0 / 16.0);

	private final Supplier<OreDictMaterial> mPestle;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aPestle   the {@code NBT_DESIGN} row of MORTAR_MATERIALS — the pestle tint
	 *                  seat and the crafting ingredient (MultiTileEntityMortar.java:59/:149)
	 * @param aTickerType the family BET (all five rows share one)
	 */
	public GT6MortarBlock(Supplier<OreDictMaterial> aPestle,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mPestle = aPestle;
		mTickerType = aTickerType;
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected com.mojang.serialization.MapCodec<GT6MortarBlock> codec() {
		return simpleCodec(aProperties -> new GT6MortarBlock(mPestle, mTickerType, aProperties));
	}
	 *///?}

	/** The {@code NBT_DESIGN} pestle material (the tint seat 1 dispatch + the crafting row). */
	public OreDictMaterial pestle() {
		return mPestle.get();
	}

	@Override
	public VoxelShape getShape(BlockState aState, net.minecraft.world.level.BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6MortarBlockEntity tMortar) {
			tMortar.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); the hand-degradation deviation is the
	// GTAnvilBlock 1.21.1 form verbatim — MAIN_HAND stands in.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6MortarBlockEntity tMortar) {
			tMortar.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	 *///?}
}
