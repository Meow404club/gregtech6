package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.covers.ICover;
import gregtech6.registry.GTBarrels;

/**
 * The plastic canister (task p6-barrel-metal-plastic) — the counterpart of the upstream
 * {@code MultiTileEntityBarrelPlastic} row (gregtech/tileentity/tanks/
 * MultiTileEntityBarrelPlastic.java:37, Loader_MultiTileEntities.java:2150, 32000 L).
 *
 * <p>Cover admission is the upstream :38 verbatim predicate — the same decorative-only
 * rule as the wood barrel ({@link GTBarrelBlockEntity}), CoverTextureSimple plates pass,
 * functional covers like the pump are refused.
 *
 * <p>The 370 K melt ceiling and the 32000 L capacity ride the
 * {@link GTBarrelBlock} properties (the registration-NBT-carrier pattern). Gas-proof is
 * a cut feature (P4 quartet pool) and has no field here.
 */
public class GTBarrelPlasticBlockEntity extends TileEntityBase08Barrel {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GTBarrelPlasticBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the offline (test) entry point passes an offline-built BET (W1 precedent). */
	public GTBarrelPlasticBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBarrels.BARREL_PLASTIC_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTBarrelBlock tBarrel) {
			mTank.setCapacity(tBarrel.capacityL());
			mMeltingPoint = tBarrel.meltingPointK();
		}
	}

	/** Upstream MultiTileEntityBarrelPlastic.java:38 verbatim — decorative covers only. */
	@Override
	public boolean allowCover(byte aSide, ICover aCover) {
		return getCovers() != null && aCover.isDecorative(aSide, getCovers());
	}

	@Override
	public String getTileEntityName() {
		return "barrel_plastic"; // BET registry path mirrors it (GTBarrels.BARREL_PLASTIC_BE)
	}
}
