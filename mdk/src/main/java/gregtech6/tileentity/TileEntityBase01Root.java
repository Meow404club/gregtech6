package gregtech6.tileentity;

import java.util.Collection;
import java.util.Collections;

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

import gregapi.code.TagData;
import gregapi.data.CS;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyGate;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.GT6Mod;
import gregtech6.util.UT6;

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
 * <li>the energy behaviour default block (task p8-d3 §①, upstream :703-725) —
 *     {@link ITileEntityEnergy} implemented with the full upstream defaults;
 *     the multiblock-part delegation (:729-747), the RF/IC2 bridges (:751-768) and
 *     the structural checks (:776+) are pool;</li>
 * <li>the explosion family (task p8-d3 §①, upstream :473-509 minimal form) —
 *     {@code mExplosionStrength} (:473), {@link #explode()} (:475-491, the SFX
 *     branches are cut with the SFX system, declared deviation pool) and
 *     {@link #overcharge(long, TagData)} (:494-509, the :506 sound cut, the :508
 *     DEB line kept as an unconditional gt6 logger line); the suspended explosion
 *     is consumed by the own tick in {@link #updateEntityCore()} (upstream
 *     :420-429).</li>
 * </ul>
 */
public abstract class TileEntityBase01Root extends BlockEntity implements ITileEntityEnergy {

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
	 * attach (:418-419) is stripped with its infrastructure; the "ticked means alive"
	 * resurrection (:416), the suspended-explosion consumption (:420-429, task p8-d3 §①)
	 * and the buffered block update (:434) carry over. The {@code return} of upstream
	 * :428 is structurally provided by setDead(): every subsequent dispatcher phase is
	 * !isDead()-guarded (TileEntityBase03TicksAndSync.updateEntity).
	 */
	protected void updateEntityCore() {
		if (isDead()) setAlive();
		if (mExplosionStrength > 0) {
			// upstream :420-429 — setToAir + explosion (the <1 strength sound-only branch
			// keeps its no-explosion semantics, SFX cut) + setDead.
			if (hasLevel() && !isClientSide()) {
				getLevel().destroyBlock(getBlockPos(), false); // upstream :421 setToAir
				if (mExplosionStrength >= 1) {
					getLevel().explode(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), mExplosionStrength, Level.ExplosionInteraction.BLOCK); // upstream :425
				}
			}
			setDead(); // upstream :427
			return; // upstream :428
		}
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
	// explosion family (task p8-d3 §① — upstream :473-509 minimal form)
	// ---------------------------------------------------------------------------

	/** Upstream :473 — the suspended explosion strength; > 0 means "explode on the own tick" (updateEntityCore). */
	public float mExplosionStrength = 0;

	/** Upstream :475 — instant outside the own tick ({@code !mIsTicking}, i.e. injected by a neighbour's tick). */
	public final void explode() {explode(!mIsTicking);}

	/** Upstream :476. */
	public final void explode(double aStrength) {explode(!mIsTicking, aStrength);}

	/** Upstream :478-480 — the strength-4 default, overridable. */
	public void explode(boolean aInstant) {
		explode(aInstant, 4); // Seems to be a reasonable Default Explosion.
	}

	/**
	 * Upstream :481-492 minimal form. The strength is always buffered into
	 * {@link #mExplosionStrength} (the max keeps the largest demand); the instant path
	 * (only when the explosion was NOT triggered from the own tick — {@code !mIsTicking})
	 * destroys the block and, at strength &gt;= 1, explodes (upstream :484 setToAir +
	 * :489 ExplosionGT; the :486 sound-only branch keeps its no-explosion semantics with
	 * the SFX cut — declared deviation pool). The vanilla bridge:
	 * LevelWriter.destroyBlock(pos, false) (LevelWriter.java:17, limit 512) and
	 * Level.explode(null, x, y, z, strength, ExplosionInteraction.BLOCK) (Level.java:468,
	 * the 1.19.4+ ExplosionInteraction overload).
	 */
	public void explode(boolean aInstant, double aStrength) {
		mExplosionStrength = (float)Math.max(aStrength, mExplosionStrength);
		if (aInstant && hasLevel() && !isClientSide()) {
			getLevel().destroyBlock(getBlockPos(), false); // upstream :484 setToAir
			if (mExplosionStrength >= 1) {
				getLevel().explode(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), mExplosionStrength, Level.ExplosionInteraction.BLOCK); // upstream :489
			}
		}
	}

	/**
	 * Upstream :494-509. The CS.OVERCHARGE_EXPLOSIONS gate picks the strength — the tier
	 * curve UT.Code.tierMax (UT.java:1388) for exploding types, 0.1 otherwise — and
	 * CS.OVERCHARGE_BREAKING is the 0.1 fallback; the :506 sound is cut with the SFX
	 * system (declared deviation pool) and the :508 DEB line stays unconditional as the
	 * gt6 logger position marker ("The Noise should make the position obvious").
	 */
	public void overcharge(long aVoltage, TagData aEnergyType) {
		// Only explode if allowed
		if (CS.OVERCHARGE_EXPLOSIONS) {
			if (TD.Energy.ALL_EXPLODING.contains(aEnergyType)) {
				explode(UT6.tierMax(aVoltage));
			} else {
				explode(0.1);
			}
		} else if (CS.OVERCHARGE_BREAKING) {
			explode(0.1);
		}
		// Yes, I will annoy people with that a lot, even when they disable Explosions.
		GT6Mod.LOGGER.info("Machine overcharged with: " + aVoltage + " " + aEnergyType.getLocalisedNameLong());
	}

	// ---------------------------------------------------------------------------
	// A Default implementation of the Energy behaviour (task p8-d3 §① — upstream
	// :703-725; :729-747 multiblock part delegation, :751-768 RF/IC2 bridges and
	// :776+ structural checks are pool).
	// ---------------------------------------------------------------------------

	/** Upstream :705 — the extension hook the :717 gate calls (not an interface method; subclasses override THIS). */
	public long doInject (TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject ) {return 0;}

	/** Upstream :706 — the extension hook the :716 gate calls (not an interface method; subclasses override THIS). */
	public long doExtract(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

	/** Upstream :707. */
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return false;}

	/** Upstream :711 (the capacitor pair :708-712 half is cut with the capacitor subsystem, ADR D1 ruling 1). */
	@Override public Collection<TagData> getEnergyTypes(byte aSide) {return Collections.emptyList();}

	/** Upstream :714 — the {@code getSurfaceSizeAttachable(aSide) > 0} half rides the {@link #isSurfaceEnergyAttachable} seam (ITileEntitySurface not ported). */
	@Override public boolean isEnergyEmittingTo   (TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyType(aEnergyType, aSide, true ) && isSurfaceEnergyAttachable(aSide);}

	/** Upstream :715 — the {@code getSurfaceSizeAttachable(aSide) > 0} half rides the {@link #isSurfaceEnergyAttachable} seam (ITileEntitySurface not ported). */
	@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyType(aEnergyType, aSide, false) && isSurfaceEnergyAttachable(aSide);}

	/**
	 * Upstream :716 verbatim via the D1 pure gate (EnergyGate.gateExtraction) — aSize 0 or
	 * not emitting → 0; size-irrelevant types skip the minimum check; a packet below the
	 * output minimum returns 0 (refused).
	 */
	@Override public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		return EnergyGate.gateExtraction(aEnergyType, isEnergyEmittingTo(aEnergyType, aSide, false), aSize, getEnergySizeOutputMin(aEnergyType, aSide), aAmount,
				()->doExtract(aEnergyType, aSide, aSize, aAmount, aDoExtract));
	}

	/**
	 * Upstream :717 verbatim via the D1 pure gate (EnergyGate.gateInjection) — aSize 0 or
	 * not accepting → 0; size-irrelevant types skip the minimum check; a packet below the
	 * input minimum returns aAmount (swallowed — doInject is never called).
	 */
	@Override public synchronized long doEnergyInjection (TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject ) {
		return EnergyGate.gateInjection(aEnergyType, isEnergyAcceptingFrom(aEnergyType, aSide, false), aSize, getEnergySizeInputMin(aEnergyType, aSide), aAmount,
				()->doInject (aEnergyType, aSide, aSize, aAmount, aDoInject ));
	}

	/** Upstream :718. */
	@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

	/** Upstream :719. */
	@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

	/** Upstream :720 — Min = Rec / 2. */
	@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return getEnergySizeOutputRecommended(aEnergyType, aSide) / 2;}

	/** Upstream :721 — Max = Rec * 2. */
	@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return getEnergySizeOutputRecommended(aEnergyType, aSide) * 2;}

	/** Upstream :722. */
	@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

	/** Upstream :723. */
	@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

	/** Upstream :724 — Min = Rec / 2. */
	@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return getEnergySizeInputRecommended(aEnergyType, aSide) / 2;}

	/** Upstream :725 — Max = Rec * 2. */
	@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return getEnergySizeInputRecommended(aEnergyType, aSide) * 2;}

	/**
	 * The surface-attachment seam of the :714-715 gates. Upstream asked
	 * {@code getSurfaceSizeAttachable(aSide) > 0} (the ITileEntitySurface cover/ocean-going
	 * surface system, not ported); the default "every side attachable" keeps the gates
	 * purely type-driven. Overridable seam.
	 */
	public boolean isSurfaceEnergyAttachable(byte aSide) {return true;}

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

	/** Raw capability handle (package-private test seam — ForgeCapabilities cannot class-init offline, so runtime code goes through {@link #getCapability}). */
	LazyOptional<IItemHandler> itemHandlerCapability() {
		return mItemHandlerCap;
	}
}
