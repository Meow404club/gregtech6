package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;

/**
 * The metal drum (task p6-barrel-metal-plastic) — the counterpart of the upstream
 * {@code MultiTileEntityBarrelMetal} row (gregtech/tileentity/tanks/
 * MultiTileEntityBarrelMetal.java:36, Loader_MultiTileEntities.java:2151, 64000 L bronze).
 *
 * <p>Upstream overrides NO cover admission here (:36-52 — the metal drum takes the
 * {@code TileEntityBase04Covers} base default and accepts every cover, functional pump
 * covers included), so this class has no {@code allowCover} override either: the
 * {@code ICoverableTE} default lets everything through, and the p5 pump machinery rides
 * the frozen {@link TileEntityBase08Barrel} base.
 *
 * <p>Declared deviation: the 64000 L bronze drum never melts. The upstream rows carry no
 * explicit {@code NBT_CAPACITY_HU} and melt through the
 * {@code mMaterial.mMeltingPoint * 1.25} formula (gregapi TileEntityBase08Barrel.java:66),
 * which needs the material melting-point bridge this repo does not ship (pool item) —
 * {@code Long.MAX_VALUE} keeps the drum at the base never-melt default rather than a
 * guessed number.
 */
public class GTBarrelMetalBlockEntity extends TileEntityBase08Barrel {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GTBarrelMetalBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the offline (test) entry point passes an offline-built BET (W1 precedent). */
	public GTBarrelMetalBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBarrels.BARREL_METAL_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTBarrelBlock tBarrel) {
			mTank.setCapacity(tBarrel.capacityL());
			mMeltingPoint = tBarrel.meltingPointK();
		}
	}

	// No allowCover override — the upstream MultiTileEntityBarrelMetal takes the base default.

	@Override
	public String getTileEntityName() {
		return "barrel_metal"; // BET registry path mirrors it (GTBarrels.BARREL_METAL_BE)
	}
}
