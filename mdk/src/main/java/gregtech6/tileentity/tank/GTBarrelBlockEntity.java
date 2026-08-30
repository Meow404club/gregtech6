package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;

/**
 * The wood fluid barrel (task p4-fluid-barrel) — the concrete mount of the ported
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
 */
public class GTBarrelBlockEntity extends TileEntityBase08Barrel {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GTBarrelBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the registry type at runtime, tests pass an offline-built BET (W1 precedent). The
	 * melting ceiling comes from the block property; non-GT-barrel blocks (offline
	 * vanilla fixtures) leave the MAX_VALUE default.
	 */
	public GTBarrelBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBarrels.BARREL_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTBarrelBlock tBarrel) {
			mMeltingPoint = tBarrel.meltingPointK();
		}
	}

	@Override
	public String getTileEntityName() {
		return "barrel_wood"; // BET registry path mirrors it (GTBarrels.BARREL_BE)
	}
}
