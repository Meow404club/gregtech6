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
 * <p>Melting: the P6-era declared deviation ("MAX_VALUE, never melts — the material
 * melting-point bridge is a pool item") was revoked by task p7-barrel-high-tier-melt-bridge.
 * The bridge now ships as {@code GTBarrels.meltingPointK} — the verbatim gregapi
 * TileEntityBase08Barrel.java:66 else-branch {@code (long)(mMaterial.mMeltingPoint * 1.25)}
 * — and the ctor below reads the ceiling off the block carrier ({@code meltingPointK()}),
 * so the 64000 L bronze drum carries Copper's 1357 K dataset point (MT.java:1705
 * {@code heat(Cu.mMeltingPoint)}) and melts at 1696 K (live-server verified, task p7).
 * The twelve high-tier drum rows (Loader_MultiTileEntities.java:2159-2170, 128 K to 10 B)
 * share this BE as multi-mounts through {@code GTBarrels.HIGH_TIER_METAL_DRUMS}; a row
 * with an explicit {@code NBT_CAPACITY_HU} takes that value verbatim over the formula.
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

	/**
	 * The :2151-2170 rows ALL carry NBT_GASPROOF=T (task p13 — verified per line): the
	 * carrier value when the block is a GT barrel, the class truth as the offline-fixture
	 * fallback. The exemption covers the :180 gas gate ONLY — the :184 allowFluid gate
	 * still voids the power-conductor fluids (steam fizzes out of a gas-proof drum).
	 */
	@Override
	public boolean gasProof() {
		return getBlockState().getBlock() instanceof GTBarrelBlock tBarrel ? tBarrel.gasProof() : true;
	}

	@Override
	public String getTileEntityName() {
		return "barrel_metal"; // BET registry path mirrors it (GTBarrels.BARREL_METAL_BE)
	}
}
