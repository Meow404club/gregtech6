package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;

/**
 * The Logistics Tank (task p12-barrel-keepfilter-logistics) — the counterpart of the
 * upstream {@code MultiTileEntityBarrelLogistics} (gregtech/tileentity/tanks/
 * MultiTileEntityBarrelLogistics.java:37-58, row Loader_MultiTileEntities.java:2171,
 * 1000000 L @ 100000 K, ANY.W = tungsten, aUtilMetal, hardness 1.0 / resistance 10.0).
 * The BE rides the metal drum as its base class, exactly as the upstream class rides the
 * same {@code TileEntityBase08Barrel} family.
 *
 * <p>The upstream override block is the class's WHOLE behavioural content (:39-41):
 * {@code canBeSealed() = F}, {@code keepsFilter() = T}, {@code canLogistics(byte) = T}.
 * The port carries the two reachable halves:
 * <ul>
 * <li>{@link #keepsFilter()} = {@code true} — the ONLY {@code true} consumer in the port
 *     (upstream the same, Logistics.java:40, base default F at gregapi
 *     TileEntityBase08Barrel.java:284): the {@code TileEntityBase08Barrel.load} wiring
 *     {@code setPreventDraining(keepsFilter())} (:132) re-applies the stickiness on every
 *     load, so the tank keeps its fluid identity drained to 0 L and {@code FluidTankGT}
 *     persists that identity through the :85 {@code writeToNBT} judgement — the
 *     keepFilter 0-amount persistence gap closes with this consumer.</li>
 * <li>{@link #canBeSealed()} = {@code false} — the upstream :39 transcription. The port's
 *     seal FACE (the soft-mallet toggle at upstream :118-123 and the sealed fermentation)
 *     is a base-level pool cut with no {@code canBeSealed} on the frozen
 *     {@link TileEntityBase08Barrel}, so this is a transcription method, not an
 *     {@code @Override}: every port barrel is equally unsealable today, this row records
 *     the logistics barrel's upstream refusal and is the seam the future tool-face card
 *     flips the base to.</li>
 * </ul>
 * {@code canLogistics(byte)} is not transcribed: {@code ITileEntityLogisticsStorage} has
 * no consumer in the port (no logistics pipes — pool cut, card spec ②). Nothing else
 * differs from the metal drum: no cover override (the upstream :37 class takes the cover
 * defaults), the capacity and the 100000 K ceiling ride the {@link GTBarrelBlock} carrier
 * (the W1 registration-NBT-carrier pattern); the upstream row's four-proof flags
 * (PLASMA/GAS/ACID/MAGIC all T) are the P4 quartet pool cut with no port consumer.
 */
public class GTBarrelLogisticsBlockEntity extends GTBarrelMetalBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GTBarrelLogisticsBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — the offline (test) entry point passes an offline-built BET (W1
	 * precedent). The default-BET resolution happens HERE, not in the metal super ctor:
	 * a null aType forwarded through {@link GTBarrelMetalBlockEntity} would fall back to
	 * the METAL BET (its own null-guard), pointing the logistics BE at the wrong registry
	 * type — the guard resolves to {@link GTBarrels#BARREL_LOGISTICS_BE} instead.
	 */
	public GTBarrelLogisticsBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTBarrels.BARREL_LOGISTICS_BE.get(), aPos, aState);
		// The :132 stickiness wiring rides BE load() — but a FRESHLY PLACED barrel never
		// reaches load(CompoundTag) (setblock/place creates the BE without restore NBT), so a
		// placement-born logistics tank stayed unarmed until the first chunk reload and
		// drained to a true empty (live-proven by the card's RCON chain, the W1 wood barrel
		// could never expose the gap with its keepsFilter=F). The ctor arm closes it;
		// load() re-applies the same verdict on every restore (both call keepsFilter()).
		mTank.setPreventDraining(keepsFilter());
	}

	/** Upstream Logistics.java:40 — the ONLY keepsFilter()=T barrel in both codebases. */
	@Override
	public boolean keepsFilter() {
		return true;
	}

	/**
	 * The :2171 row carries NBT_GASPROOF=T (task p13) — transcribed explicitly so the row
	 * citation stands on its own (it would ride the metal override through the class
	 * hierarchy otherwise): the carrier value when the block is a GT barrel, the class
	 * truth as the offline-fixture fallback.
	 */
	@Override
	public boolean gasProof() {
		return getBlockState().getBlock() instanceof GTBarrelBlock tBarrel ? tBarrel.gasProof() : true;
	}

	/** Upstream Logistics.java:39 verbatim — see the class javadoc: a transcription, the port's seal face is a base-level cut. */
	public boolean canBeSealed() {
		return false;
	}

	@Override
	public String getTileEntityName() {
		return "barrel_logistics"; // BET registry path mirrors it (GTBarrels.BARREL_LOGISTICS_BE)
	}
}
