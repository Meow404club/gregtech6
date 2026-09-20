package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import gregtech6.tileentity.logistics.ITileEntityLogisticsStorage;
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
 * {@code canLogistics(byte)} and the storage face joined the port with task
 * p32-logistics-lv3 (the Core's BFS endpoint face, below) — until the cover family lands
 * this tank IS the logistics network's storage endpoint (research.p31-logistics
 * missing_by_cost_asc row 4, the Storage-endpoint slice).
 */
public class GTBarrelLogisticsBlockEntity extends GTBarrelMetalBlockEntity implements ITileEntityLogisticsStorage {

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

	// ---------------------------------------------------------------------------
	// the logistics endpoint face (task p32-logistics-lv3)
	// ---------------------------------------------------------------------------

	/**
	 * The tier-override NBT key and the override value. Upstream configures endpoint
	 * tiering through the screwdriver on the COVER family (the AbstractCoverAttachmentLogistics
	 * mValues bit 0-1) — the port's cover card is a later slice, so the headless
	 * configuration seam (the /gt6logistics tank priority command, the /gt6itempipe
	 * retriever place→装盖设滤 precedent) drives this persisted byte. {@code -1} (the
	 * default) = the upstream content-derived answer below.
	 */
	public static final String NBT_LOGISTICS_PRIORITY = "gt.logistics.priority";

	/** {@link #NBT_LOGISTICS_PRIORITY} — a negative value is the auto (content-derived) arm. */
	public byte mLogisticsPriority = -1;

	/** The command seam for the tier override (clamped to the 0..3 tier window; -1 = auto). */
	public void setLogisticsPriority(int aPriority) {
		byte tPriority = aPriority < 0 ? -1 : (byte)Math.min(3, aPriority);
		if (tPriority != mLogisticsPriority) {
			mLogisticsPriority = tPriority;
			setChanged();
		}
	}

	/** Upstream MultiTileEntityBarrelLogistics.java:41 verbatim — every side participates. */
	@Override
	public boolean canLogistics(byte aSide) {
		return true;
	}

	/** Upstream TileEntityBase08Barrel.java:285, with the override arm ahead of it. */
	@Override
	public int getLogisticsPriorityFluid() {
		if (mLogisticsPriority >= 0) return mLogisticsPriority;
		return mTank.isEmpty() ? 1 : 2;
	}

	/** Upstream TileEntityBase08Barrel.java:286 — the tank carries no item routing. */
	@Override
	public int getLogisticsPriorityItem() {
		return 0;
	}

	/** Upstream TileEntityBase08Barrel.java:287 — the tank's fluid identity IS the filter (keepsFilter keeps it at 0 L). */
	@Override
	public Fluid getLogisticsFilterFluid() {
		return mTank.fluid() == null ? null : mTank.fluid().getFluid();
	}

	/** Upstream TileEntityBase08Barrel.java:288. */
	@Nullable
	@Override
	public ItemStack getLogisticsFilterItem() {
		return null;
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_LOGISTICS_PRIORITY, Tag.TAG_ANY_NUMERIC)) mLogisticsPriority = aNBT.getByte(NBT_LOGISTICS_PRIORITY);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mLogisticsPriority >= 0) aNBT.putByte(NBT_LOGISTICS_PRIORITY, mLogisticsPriority);
	}
}
