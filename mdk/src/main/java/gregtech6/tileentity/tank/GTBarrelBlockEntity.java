package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.covers.ICover;
import gregtech6.registry.GTBarrels;

/**
 * The wood fluid barrel (task p4-fluid-barrel, cover admission restored by task
 * p6-barrel-metal-plastic ①) — the concrete mount of the ported
 * {@link TileEntityBase08Barrel} family base, the counterpart of the upstream
 * {@code MultiTileEntityBarrelWood} row (gregtech/tileentity/tanks/MultiTileEntityBarrelWood.java:38).
 *
 * <p>The registration-NBT row (Loader_MultiTileEntities.java:2136) carried
 * {@code NBT_TANK_CAPACITY=8000, NBT_CAPACITY_HU=340} upstream; the W1
 * block-derived-shape deviation applies: the capacity stays the class default 16000 L
 * (card acceptance ① pins it) and the 340 K melt-down ceiling rides the
 * {@link GTBarrelBlock#meltingPointK()} block property. The {@code NBT_CAPACITY_HU}
 * read seam (:66) still overrides the field at load. Wood does NOT override
 * {@code keepsFilter()} upstream — the barrel drains to a true empty.
 *
 * <p>Cover admission (task p6-barrel-metal-plastic ①): the upstream :39 verbatim
 * predicate — {@code allowCover → aCover.isDecorative(aSide, getCoverData())} — restores
 * the decorative-only rule: CoverTextureSimple plates pass (the p4 iron-plate cover),
 * functional covers like the pump are refused; the P5泵盖全开裁偏离就此回补. A cover
 * installed before this gate stays valid but any newly refused one drops itself on the
 * first tick through {@code ICoverableTE.checkCoverValidity} (06Covers :207-215) — zero
 * migration code. The null guard is defensive: upstream reaches this with a non-null
 * store (setCoverItem :135 builds it before the :146 gate).
 */
public class GTBarrelBlockEntity extends TileEntityBase08Barrel {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GTBarrelBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the registry type at runtime, tests pass an offline-built BET (W1 precedent). The
	 * melting ceiling and the tank size come from the block properties; non-GT-barrel
	 * blocks (offline vanilla fixtures) leave the 16000 L / MAX_VALUE defaults.
	 */
	public GTBarrelBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBarrels.BARREL_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTBarrelBlock tBarrel) {
			mTank.setCapacity(tBarrel.capacityL());
			mMeltingPoint = tBarrel.meltingPointK();
		}
	}

	/** Upstream MultiTileEntityBarrelWood.java:39 verbatim — decorative covers only. */
	@Override
	public boolean allowCover(byte aSide, ICover aCover) {
		return getCovers() != null && aCover.isDecorative(aSide, getCovers());
	}

	@Override
	public String getTileEntityName() {
		return "barrel_wood"; // BET registry path mirrors it (GTBarrels.BARREL_BE)
	}
}
