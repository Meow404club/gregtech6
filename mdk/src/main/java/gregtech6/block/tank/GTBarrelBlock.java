package gregtech6.block.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.fluids.FluidUtil;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBarrels;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The wood fluid barrel block (task p4-fluid-barrel) — the block side of the barrel
 * family over the barrel BET (the GTFluidPipeBlock carrier pattern: the block carries
 * the registration value upstream wrote into the MTE definition NBT, here the
 * {@code NBT_CAPACITY_HU=340} melt-down ceiling, Loader_MultiTileEntities.java:2136).
 *
 * <p>{@code use} is the bucket interaction face (spec ②): the documented Forge idiom
 * over {@link FluidUtil#interactWithFluidHandler(Player, InteractionHand, Level, BlockPos,
 * net.minecraft.core.Direction)} (FluidUtil.java:64/:83) — the player's fluid container
 * is filled from the barrel first, then drained into it (:95/:98 order), the item swap
 * and stow handled by FluidUtil. The barrel capability resolves through the
 * side-wrapped {@code BarrelFluidHandler} (getFluidHandler(level, pos, side) :457).
 */
public class GTBarrelBlock extends GTEntityBlock {

	private final long mMeltingPointK;

	public GTBarrelBlock(long aMeltingPointK, Properties aProperties) {
		super(aProperties);
		mMeltingPointK = aMeltingPointK;
	}

	/** The melt-down ceiling in Kelvin (upstream NBT_CAPACITY_HU=340, Loader_MultiTileEntities.java:2136). */
	public long meltingPointK() {
		return mMeltingPointK;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBarrels.BARREL_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// p5 spec ④ — the cover machinery consumes the click first (the GTOvenBlock 2e89501
		// three-line pattern over ICoverableTE.onCoverUse: the covered-face intercepts, then
		// the attachCoversFirst install branch); false falls through to the bucket face below.
		// Runs on both sides like the FluidUtil idiom — the server pass is authoritative,
		// the client pass is the prediction.
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase08Barrel tBarrel
				&& tBarrel.onCoverUse(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
						(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()), (float) (aHit.getLocation().z - aPos.getZ()))) {
			return InteractionResult.CONSUME;
		}
		// spec ② — FluidUtil.java:64 signature; runs on both sides like the documented idiom,
		// the server pass is authoritative, the client pass is the prediction.
		return FluidUtil.interactWithFluidHandler(aPlayer, aHand, aLevel, aPos, aHit.getDirection())
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}
}
