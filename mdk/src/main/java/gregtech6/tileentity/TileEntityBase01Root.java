package gregtech6.tileentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * 1.20.1 counterpart of the upstream 1.7.10 root base class
 * gregapi/tileentity/base/TileEntityBase01Root.java (extends TileEntity, 1011 lines).
 *
 * <p>Port scope (task p3-be-framework, minimal face):
 * <ul>
 * <li>lifecycle flags (:97-115) — the IC2 E-net pair (:109) and the AE2/IC2
 *     {@code @Optional} interfaces (:92-94) are stripped together with their
 *     infrastructure, as is the FORCE_FULL_SELECTION_BOXES render hack (:112);</li>
 * <li>the NBT pair readFromNBT :134-141 / writeToNBT :144-152 mapped onto
 *     {@link BlockEntity#load(CompoundTag)} (BlockEntity.java:50) /
 *     {@link BlockEntity#saveAdditional(CompoundTag)} (:53): vanilla now carries
 *     the position ("x"/"y"/"z", saveMetadata :91-96) and the "id" key
 *     (BlockEntityType registry key, saveId :74-81), so the tile entity name
 *     travels under "te_name" instead of colliding with the vanilla slot —
 *     the negative-Y crash guard (:140) is obsolete since 1.18 made negative Y legal;</li>
 * <li>canUpdate() :440 ({@code mIsTicking && mShouldRefresh}) maps onto the
 *     EntityBlock ticker wiring in gregtech6.block.GTEntityBlock;</li>
 * <li>side-offset arithmetic (:162-176): the GT6 side order 0..5 equals
 *     Direction.getIndex() order (DOWN/UP/N/S/W/E), so the upstream OFFX/OFFY/OFFZ
 *     tables (gregapi.data.CS) carry over verbatim;</li>
 * <li>capability exposure per ADR-P3-2 (researcher R3 ruling):
 *     {@link ForgeCapabilities#ITEM_HANDLER} over a {@link LazyOptional}; only
 *     invalidateCaps() is overridden — the Forge patch already inserts
 *     invalidateCaps() into setRemoved() and onChunkUnloaded()
 *     (BlockEntity.java.patch:45/:51), so those two stay untouched.</li>
 * </ul>
 */
public abstract class TileEntityBase01Root extends BlockEntity {

	/** If this TileEntity checks for the Chunk to be loaded before returning World based values (upstream :97). */
	public boolean mIgnoreUnloadedChunks = true;

	/** This Variable checks if this TileEntity is dead (upstream :100). */
	public boolean mIsDead = false;

	/** This Variable checks if this TileEntity should refresh when the Block is being set (upstream :103). */
	public boolean mShouldRefresh = true;

	/** This Variable is for a buffered Block Update (upstream :106). */
	public boolean mDoesBlockUpdate = false;

	/** If this TileEntity is ticking at all (upstream :115). */
	public final boolean mIsTicking;

	/** Last error message set by {@link #setError(String)} (upstream ERROR_MESSAGE, :1010). */
	public String ERROR_MESSAGE = "";

	/** The inventory carrier, assigned by subclasses via {@link #setInventory(GTItemStackHandler)} (null = no inventory). */
	protected GTItemStackHandler mInventory;

	/** Lazy capability handle over {@link #mInventory} (ADR-P3-2). */
	private LazyOptional<IItemHandler> mItemHandlerCap = LazyOptional.empty();

	/** Upstream constructor (boolean aIsTicking, :119-121) extended with the mandatory 1.20.1 BlockEntity triple (:27-31). */
	protected TileEntityBase01Root(boolean aIsTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		mIsTicking = aIsTicking;
	}

	/** return the internal Name of this TileEntity to be registered. DO NOT START YOUR NAME WITH "gt."!!! (upstream :154-155) */
	public abstract String getTileEntityName();

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		// upstream readFromNBT :134-141 restored x/y/z and guarded the negative-Y crash;
		// both are vanilla's job now (position from chunk metadata, negative Y legal),
		// so the base load is the documented hook point for subclasses.
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		// upstream writeToNBT :144-152 wrote "id" = getTileEntityName() plus the position;
		// the vanilla "id" slot is the BET registry key now (saveId :74-81), so the name
		// persists under "te_name" to keep save files identifiable without corrupting
		// BlockEntity.loadStatic (:99-126).
		aNBT.putString("te_name", getTileEntityName());
	}

	@Override
	public void setChanged() {
		// Upstream markDirty() was deliberately no-op'd (:157, comparator support broken in 1.7.10);
		// 1.20.1 setChanged is the chunk dirty flag and must stay functional (GTItemStackHandler binds it).
		super.setChanged();
	}

	/** @return {@code true} while the TileEntity is dead (upstream :360-362). */
	public boolean isDead() {
		return mIsDead;
	}

	/** Upstream :1010 with the PacketBlockError broadcast stripped (IPacket stack gone). */
	public void setError(String aError) {
		ERROR_MESSAGE = aError;
	}

	/** Upstream :400-406 with the IC2 E-net unload branch (:404) stripped. */
	protected void setDead() {
		if (!mIsDead) {
			mIsDead = true;
		}
	}

	/** Upstream :408-410. */
	protected void setAlive() {
		mIsDead = false;
	}

	/**
	 * canUpdate semantics (upstream :440-442, {@code mIsTicking && mShouldRefresh}): the
	 * EntityBlock ticker consults this on the live instance every tick, so a machine can
	 * stop ticking by flipping mShouldRefresh, exactly like the 1.7.10 canUpdate gate.
	 */
	public boolean canUpdate() {
		return mIsTicking && mShouldRefresh;
	}

	/** Upstream :177 — with no world the 1.7.10 FML effective side was the server (dedicated offline construction, tests). */
	public boolean isServerSide() {
		return !isClientSide();
	}

	/** Upstream :178. */
	public boolean isClientSide() {
		Level tLevel = getLevel();
		return tLevel != null && tLevel.isClientSide();
	}

	// ---------------------------------------------------------------------------
	// side-offset arithmetic (upstream :162-176, OFFX/OFFY/OFFZ from gregapi.data.CS
	// — the 1.7.10 side order 0..5 == Direction.getIndex() order).
	// ---------------------------------------------------------------------------

	private static final int[] OFFX = {0, 0, 0, 0, -1, 1};
	private static final int[] OFFY = {0, 1, 0, 0, 0, 0};
	private static final int[] OFFZ = {0, 0, -1, 1, 0, 0};

	/** Upstream :162. */
	public int getOffsetX(byte aSide) {
		return getBlockPos().getX() + OFFX[aSide];
	}

	/** Upstream :163. */
	public int getOffsetY(byte aSide) {
		return getBlockPos().getY() + OFFY[aSide];
	}

	/** Upstream :164. */
	public int getOffsetZ(byte aSide) {
		return getBlockPos().getZ() + OFFZ[aSide];
	}

	/** Upstream :168-170 (the negative variants; the aMultiplier forms :165-167/:171-173 stay out of the minimal face). */
	public int getOffsetXN(byte aSide) {
		return getBlockPos().getX() - OFFX[aSide];
	}

	/** Upstream :169. */
	public int getOffsetYN(byte aSide) {
		return getBlockPos().getY() - OFFY[aSide];
	}

	/** Upstream :170. */
	public int getOffsetZN(byte aSide) {
		return getBlockPos().getZ() - OFFZ[aSide];
	}

	/** Upstream getOffset(aSide, 1) as a BlockPos. */
	public BlockPos getOffset(byte aSide) {
		return getBlockPos().offset(OFFX[aSide], OFFY[aSide], OFFZ[aSide]);
	}

	// ---------------------------------------------------------------------------
	// tick core (the 01Root half of the dispatcher's super.updateEntity() step)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream TileEntityBase01Root.updateEntity (:414-434) tick core. The IC2 E-net
	 * attach (:418-419) and the mExplosionStrength branch (:421-432) are stripped with
	 * their fields; the "ticked means alive" resurrection (:416) and the buffered block
	 * update (:434) carry over.
	 */
	protected void updateEntityCore() {
		if (isDead()) setAlive();
		if (mDoesBlockUpdate) doBlockUpdate();
	}

	/** Simple Function to prevent Block Updates from happening multiple times within the same Tick (upstream :444-447). */
	public final void causeBlockUpdate() {
		if (mIsTicking) mDoesBlockUpdate = true; else doBlockUpdate();
	}

	/** Minimal doBlockUpdate (upstream :449-459 — the IMTE_IsProvidingStrongPower branch is IMTE_-gated and out of scope). */
	public void doBlockUpdate() {
		if (hasLevel()) {
			getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
		}
		mDoesBlockUpdate = false;
	}

	// ---------------------------------------------------------------------------
	// capability exposure (ADR-P3-2, researcher R3 ruling)
	// ---------------------------------------------------------------------------

	/**
	 * Subclasses owning an inventory call this once from their constructor; the handler's
	 * content-change hook binds {@code this::setChanged} (GTItemStackHandler, GTCEu
	 * CustomItemStackHandler.java:18-52 同构) so every mutation marks the BE dirty.
	 */
	protected void setInventory(GTItemStackHandler aInventory) {
		mInventory = aInventory;
		mItemHandlerCap = LazyOptional.of(() -> aInventory);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		// IItemHandler carries @AutoRegisterCapability (IItemHandler.java:15) — no
		// RegisterCapabilitiesEvent wiring needed; the invalidation cycle is driven by the
		// Forge-patched setRemoved()/onChunkUnloaded() inserting invalidateCaps().
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return mItemHandlerCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mItemHandlerCap.invalidate();
	}
}
