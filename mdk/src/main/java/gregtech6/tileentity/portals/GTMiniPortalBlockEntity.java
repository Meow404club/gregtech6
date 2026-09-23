package gregtech6.tileentity.portals;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.portals.GTMiniPortalBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of the upstream miniature portal base
 * gregtech/tileentity/portals/MultiTileEntityMiniPortal.java (520 lines) — the
 * cross-dimension relay block (task p35-portals-mini-nether-end, the POC-concluded
 * direct translation of state research.p35-r-portals-crossdim-sync).
 *
 * <p>Port scope (the POC "chosen" mechanism, verbatim semantics):
 * <ul>
 * <li><b>mTarget = session BE reference cache, NOT persisted</b> (upstream :62/:71-80 —
 *     readFromNBT2/writeToNBT2 carry only NBT_ACTIVE). The target rebuilds every server
 *     session from the static pair lists ({@link #onTickFirst} →
 *     {@link #addThisPortalToLists}), exactly upstream.</li>
 * <li><b>Runtime registry = the static pair lists</b> (upstream sListNetherSide/sListWorldSide
 *     Nether.java:42-44): server-only, built by first-tick registration, torn down by
 *     {@link #onChunkUnloaded} (upstream onChunkUnload → disableThisPortal :204-207) and
 *     cleared on {@code ServerStartedEvent}/{@code ServerStoppedEvent} (upstream
 *     onServerStart/Stop :209-210; the GT6Portals.ServerLifecycleLists listener). The
 *     upstream isDead poll maps onto {@code isRemoved()} (:173 ↔ IForgeBlockEntity
 *     removal flag — ServerLevel instances live the whole server session, so a cached
 *     reference stays valid until removal).</li>
 * <li><b>Pairing = coordinate-arithmetic nearest neighbour</b>, per family:
 *     Nether ×8 with a 128² tolerance (Nether.java:71-80), End ×128 with 512²
 *     (End.java:67-89), Y-proximity tie-break (upstream :78). One side's table gains a
 *     portal → the opposite table rescans wholesale (Nether.java:103/:107).</li>
 * <li><b>The cross-dimension access TRAP</b> (POC R1): a bare getBlockEntity on an
 *     unloaded chunk synchronously pulls the chunk (Level.getChunkAt → getChunk(FULL,
 *     true), Level.java:191-197 + LevelReader.java:129-130). EVERY cross-dimension
 *     access funnels through {@link #delegateAdjacent(byte)}, which guards with
 *     {@code Level.isLoaded(pos)} (Level.java:795, the chunkSource.hasChunk bit probe —
 *     never loads). chunkload stays user responsibility (upstream = tooltip only :97);
 *     no forced loading API is introduced.</li>
 * <li><b>Relay faces</b> (upstream :325-515): items (the ITEM_HANDLER capability forwards
 *     to the delegate's own — the upstream ISidedInventory relay + mLastSide memory is
 *     obsolete, every modern access is sided), fluids (FLUID_HANDLER forwards likewise),
 *     energy (the ITileEntityEnergy family forwards with the OPOS flip — the upstream
 *     ITileEntityDelegating face), redstone/comparator (the per-tick direct field write
 *     + 20 tick watchdog, upstream :112-181). With no target the portal passes through
 *     to its OWN neighbour (upstream {@code delegator(aSide)} :329 — a targetless portal
 *     is a plain one-block extender, no mActive gate on this path, upstream verbatim).</li>
 * <li><b>6×byte[6] server-only state</b> (upstream :63): mRedstone/mComparator (the own
 *     emission buffers), xRedstone/xComparator (the per-tick inbound relay targets),
 *     wRedstone/wComparator (the watchdog counters) — none of it syncs. The client sees
 *     only mActive through the vanilla two-channel sync (the 1-byte getClientDataPacket
 *     :244-257 counterpart; GTWireGlowLightTest two-channel precedent).</li>
 * </ul>
 *
 * <p>Declared deviations: the 13-pass frame render → an ACTIVE blockstate cube swap
 * (the visual face, cosmetic); the comparator OUTPUT bridge is direction-less in vanilla
 * ({@code getAnalogOutputSignal(BlockState, BlockGetter, BlockPos)} — no side parameter),
 * so the block folds the per-face mComparator to the maximum (upstream answered
 * per-face; single-comparator rigs read identically, the fold is the declared ceiling);
 * the activate portal sound is cut with the SFX system (declared deviation pool).
 */
public abstract class GTMiniPortalBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream CS.NBT_ACTIVE = "gt.active" (CS.java:1223) — the ONLY persisted portal key (upstream :71-80). */
	public static final String NBT_ACTIVE = "gt.active";

	/** Upstream :60 — the synced active flag (1 byte on the wire upstream, one boolean key on both modern channels). */
	public boolean mActive = false;

	/** Upstream :62 — the session target reference. NEVER persisted; rebuilt per session from the pair lists. */
	@Nullable
	public GTMiniPortalBlockEntity mTarget = null;

	/** Upstream :63 verbatim — the six per-face server-only relay buffers (GT6 side order 0..5). */
	public final byte[] mRedstone = {0, 0, 0, 0, 0, 0}, mComparator = {0, 0, 0, 0, 0, 0};
	/** Upstream :63 verbatim — the per-tick inbound relay writes (the -1 sentinel = fresh for this tick) and watchdog counters. */
	public final byte[] xRedstone = {0, 0, 0, 0, 0, 0}, xComparator = {0, 0, 0, 0, 0, 0}, wRedstone = {0, 0, 0, 0, 0, 0}, wComparator = {0, 0, 0, 0, 0, 0};

	protected GTMiniPortalBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	// ---------------------------------------------------------------------------
	// the per-family abstract half (upstream :65-68)
	// ---------------------------------------------------------------------------

	/**
	 * The pairing kernel (upstream Nether.java:71-80 / End.java:67-76 — the two mirrored
	 * dimension branches fold into one formula: the upstream overworld and nether branch
	 * compute the IDENTICAL expression {@code selfX - candidateX*factor}, :73 vs :85).
	 * Nearest candidate by distance² under the factor arithmetic, skipping dead entries
	 * ({@code !isRemoved()} ↔ upstream {@code !isDead()}), the Y-proximity tie-break on an
	 * equal distance (:78/:86); anything beyond the tolerance square never wins.
	 * Static and level-free — the offline pairing tests drive it directly.
	 */
	@Nullable
	static GTMiniPortalBlockEntity nearestPortal(List<GTMiniPortalBlockEntity> aCandidates, BlockPos aSelf, long aFactor, long aToleranceSq) {
		GTMiniPortalBlockEntity tBest = null;
		long tShortestDistance = aToleranceSq;
		for (GTMiniPortalBlockEntity tTarget : aCandidates) {
			if (tTarget == null || tTarget.isRemoved()) continue;
			long tXDifference = aSelf.getX() - tTarget.getBlockPos().getX() * aFactor;
			long tZDifference = aSelf.getZ() - tTarget.getBlockPos().getZ() * aFactor;
			long tTempDist = tXDifference * tXDifference + tZDifference * tZDifference;
			if (tTempDist < tShortestDistance) {
				tShortestDistance = tTempDist;
				tBest = tTarget;
			} else if (tTempDist == tShortestDistance && (tBest == null
					|| Math.abs(tTarget.getBlockPos().getY() - aSelf.getY()) < Math.abs(tBest.getBlockPos().getY() - aSelf.getY()))) {
				tBest = tTarget; // upstream :78 — the Y-proximity tie-break
			}
		}
		return tBest;
	}

	/** Upstream :65 — coordinate-arithmetic nearest-neighbour scan over the opposite table. */
	public abstract void findTargetPortal();

	/** Upstream :66 — join the family's table for this dimension, then rescan both tables. */
	public abstract void addThisPortalToLists();

	/** Upstream :67 — the own-dimension table. */
	protected abstract List<GTMiniPortalBlockEntity> getPortalListA();

	/** Upstream :68 — the opposite-dimension table. */
	protected abstract List<GTMiniPortalBlockEntity> getPortalListB();

	// ---------------------------------------------------------------------------
	// NBT (upstream :71-80 — mActive only, mTarget never persists)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE); // upstream :73
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_ACTIVE, mActive); // upstream :79
	}

	// ---------------------------------------------------------------------------
	// tick chain (upstream :102-181 verbatim phase-for-phase)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && mActive) { // upstream :105-108
			addThisPortalToLists();
			causeBlockUpdate();
		}
	}

	@Override
	public void onTickStart(long aTimer, boolean aIsServerSide) {
		super.onTickStart(aTimer, aIsServerSide);
		if (!aIsServerSide) return;
		for (byte tSide = 0; tSide < 6; tSide++) { // upstream ALL_SIDES_VALID :116
			if (mActive) {
				// redstone promote + watchdog (upstream :118-134)
				if (xRedstone[tSide] >= 0) {
					if (mRedstone[tSide] != xRedstone[tSide]) {
						mRedstone[tSide] = xRedstone[tSide];
						causeBlockUpdate(); // the emission face changed → neighbour re-eval
					}
					xRedstone[tSide] = -1;
					wRedstone[tSide] = 0;
				} else {
					if (wRedstone[tSide] >= 20) {
						if (mRedstone[tSide] != 0) {
							mRedstone[tSide] = 0;
							causeBlockUpdate();
						}
					} else {
						wRedstone[tSide]++;
					}
				}
				// comparator promote + watchdog (upstream :136-152)
				if (xComparator[tSide] >= 0) {
					if (mComparator[tSide] != xComparator[tSide]) {
						mComparator[tSide] = xComparator[tSide];
						causeBlockUpdate();
					}
					xComparator[tSide] = -1;
					wComparator[tSide] = 0;
				} else {
					if (wComparator[tSide] >= 20) {
						if (mComparator[tSide] != 0) {
							mComparator[tSide] = 0;
							causeBlockUpdate();
						}
					} else {
						wComparator[tSide]++;
					}
				}
			} else {
				// an inactive portal emits nothing (upstream :153-162)
				if (mRedstone[tSide] != 0) {
					mRedstone[tSide] = 0;
					causeBlockUpdate();
				}
				if (mComparator[tSide] != 0) {
					mComparator[tSide] = 0;
					causeBlockUpdate();
				}
			}
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide) return;
		// target liveness: null → the 100-tick rescan beat; removed → immediate (upstream :173,
		// mTarget.isDead() ↔ modern isRemoved() — the POC R3 mapping)
		if (mActive && (mTarget == null ? aTimer % 100 == 5 : mTarget.isRemoved())) findTargetPortal();
		// scan own redstone/comparator and relay into the target's inbound buffers (upstream :176-179;
		// the max-bind keeps the strongest writer — UT.Code.bind_(io, 15, in) = max(io, min(15, in)))
		if (mTarget != null) for (byte tSide = 0; tSide < 6; tSide++) {
			mTarget.xRedstone[UT6.OPOS[tSide]] = (byte)Math.max(mTarget.xRedstone[UT6.OPOS[tSide]], Math.min(15, getRedstoneIncoming(tSide)));
			mTarget.xComparator[UT6.OPOS[tSide]] = (byte)Math.max(mTarget.xComparator[UT6.OPOS[tSide]], Math.min(15, getComparatorIncoming(tSide)));
		}
	}

	/** Upstream :183 — sets active, joins the table and syncs (mActive rides the two vanilla channels + the blockstate). */
	public void setPortalActive() {
		if (!mActive) {
			mActive = true;
			addThisPortalToLists();
			causeBlockUpdate();
			updateClientData();
			syncActiveBlockState();
		}
	}

	/** Upstream :184 — the upstream updateClientData + disable ordering kept. */
	public void setPortalInactive() {
		if (mActive) {
			disableThisPortal();
			causeBlockUpdate();
			updateClientData();
			syncActiveBlockState();
		}
	}

	/** Upstream :186-189 — leaves both tables and rescans whoever pointed here. */
	public void removeThisPortalFromLists() {
		if (getPortalListA().remove(this)) for (GTMiniPortalBlockEntity tPortal : getPortalListB()) if (tPortal.mTarget == this) tPortal.findTargetPortal();
		if (getPortalListB().remove(this)) for (GTMiniPortalBlockEntity tPortal : getPortalListA()) if (tPortal.mTarget == this) tPortal.findTargetPortal();
	}

	/** Upstream :213-225 — clears the own emission, zeroes the target's inbound relay buffers and drops the target. */
	public void disableThisPortal() {
		mActive = false;
		for (byte tSide = 0; tSide < 6; tSide++) {
			mRedstone[tSide] = 0;
			mComparator[tSide] = 0;
			if (mTarget != null) {
				mTarget.xRedstone[UT6.OPOS[tSide]] = 0;
				mTarget.xComparator[UT6.OPOS[tSide]] = 0;
			}
		}
		removeThisPortalFromLists();
		mTarget = null;
	}

	@Override
	public void onChunkUnloaded() {
		// upstream onChunkUnload :204-207 — the registry keeps loaded portals only;
		// IForgeBlockEntity#onChunkUnloaded (forge) / IBlockEntityExtension:61 (neo 21.1).
		disableThisPortal();
		super.onChunkUnloaded();
	}

	/**
	 * The mirror of the disable path for the REMOVAL half of the upstream invalidate()
	 * (:198-201) — setRemoved fires on both break and unload, and onChunkUnloaded covers
	 * the unload arm; the removal arm clears the table membership so a broken portal never
	 * comes back through the session registry. The relay-buffer zeroing of disableThisPortal
	 * is skipped here on purpose: the target rescans on its next liveness poll (mTarget.isRemoved()
	 * → findTargetPortal), exactly the upstream isDead flow (:173).
	 */
	@Override
	public void setRemoved() {
		removeThisPortalFromLists();
		super.setRemoved();
	}

	/** The ACTIVE blockstate mirror (the visual face; the vanilla blockstate channel carries it to clients). */
	protected void syncActiveBlockState() {
		if (hasLevel() && isServerSide() && getBlockState().getBlock() instanceof GTMiniPortalBlock) {
			getLevel().setBlock(getBlockPos(), getBlockState().setValue(GTMiniPortalBlock.ACTIVE, mActive), Block.UPDATE_ALL);
		}
	}

	// ---------------------------------------------------------------------------
	// the delegation core (upstream :328-336) — EVERY cross-dimension access
	// funnels through here behind the isLoaded guard (the POC R1 trap)
	// ---------------------------------------------------------------------------

	/**
	 * The side the delegate BE is accessed through (the upstream DelegatorTileEntity
	 * mSideOfTileEntity): the OPOS flip through the target (:330), the raw side on the
	 * targetless pass-through (:329 — delegator(aSide) keeps aSide).
	 */
	public byte delegateAdjacentSide(byte aSide) {
		return mTarget != null ? UT6.OPOS[aSide] : aSide;
	}

	/**
	 * Upstream getDelegateTileEntity :328-331: the target's adjacent BE at the OPPOSITE
	 * face ({@link UT6#OPOS}); no target → the portal's OWN adjacent BE ({@code delegator(aSide)},
	 * :329 — the targetless portal is a plain pass-through extender). The unloaded-chunk
	 * guard (POC R1) returns null instead of pulling the chunk; upstream the same call on
	 * an unloaded chunk raised a null-form via the 1.7.10 chunk loader, so the relay degrades
	 * to "no delegate" — the R2 semantics (lost, not buffered).
	 */
	@Nullable
	public BlockEntity delegateAdjacent(byte aSide) {
		if (aSide < 0 || aSide >= 6) return null;
		return adjacent(mTarget != null ? mTarget : this, delegateAdjacentSide(aSide));
	}

	/**
	 * The guarded adjacency probe: {@code Level.isLoaded} (Level.java:795 — the
	 * chunkSource.hasChunk bit check, NEVER loads) before any getBlockEntity. The own-level
	 * case is guarded too (a border chunk can be unloaded on this level as well).
	 */
	@Nullable
	private static BlockEntity adjacent(GTMiniPortalBlockEntity aPortal, byte aSide) {
		Level tLevel = aPortal.getLevel();
		if (tLevel == null || tLevel.isClientSide()) return null;
		BlockPos tPos = aPortal.getBlockPos().relative(Direction.from3DDataValue(aSide));
		if (!tLevel.isLoaded(tPos)) return null; // the TRAP guard — never synchronously pull a chunk
		return tLevel.getBlockEntity(tPos);
	}

	// ---------------------------------------------------------------------------
	// capability relay (the upstream ISidedInventory/IFluidHandler relay, modern form)
	// ---------------------------------------------------------------------------

	//? if forge {
	/**
	 * The item/fluid relay (upstream :350-515): every capability ask forwards to the
	 * delegate's own capability on the delegate side (the OPOS face through the target —
	 * the upstream mSideOfTileEntity, {@link #delegateAdjacentSide}). Fresh per call, no
	 * caching — the delegate can vanish between ticks.
	 */
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aSide != null && (aCapability == ForgeCapabilities.ITEM_HANDLER || aCapability == ForgeCapabilities.FLUID_HANDLER)) {
			byte tQuerySide = (byte) aSide.get3DDataValue();
			BlockEntity tDelegate = delegateAdjacent((byte) tQuerySide);
			if (tDelegate != null) {
				return tDelegate.getCapability(aCapability, Direction.from3DDataValue(delegateAdjacentSide((byte) tQuerySide)));
			}
			return LazyOptional.empty();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// 21.1: BlockEntity carries no getCapability to override (javap 21.1.249) — the
	// item/fluid relay rides the GT6CapabilityWiring provider row: it calls the PUBLIC
	// delegateAdjacent(side) and resolves the delegate's capability through the LEVEL query
	// (tDelegate.getLevel().getCapability(cap, tDelegate.getBlockPos(), aSide)) — the
	// MultiBlockPartBlockEntity relay-row form. No seam member needed on this BE.
	*///?}

	// ---------------------------------------------------------------------------
	// energy relay (the upstream ITileEntityDelegating face — the network asks, the
	// portal forwards on the delegate side (delegateAdjacentSide); the delegate machine
	// answers through its own full gates, exactly the upstream network-side pull)
	// ---------------------------------------------------------------------------

	/** The delegate as an energy endpoint (own neighbour when targetless — the :329 pass-through). */
	@Nullable
	private ITileEntityEnergy delegateEnergy(byte aSide) {
		BlockEntity tDelegate = delegateAdjacent(aSide);
		return tDelegate instanceof ITileEntityEnergy tEnergy ? tEnergy : null;
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		if (aSide < 0 || aSide >= 6) return false;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate != null && tDelegate.isEnergyAcceptingFrom(aEnergyType, delegateAdjacentSide(aSide), aTheoretical);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		if (aSide < 0 || aSide >= 6) return false;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate != null && tDelegate.isEnergyEmittingTo(aEnergyType, delegateAdjacentSide(aSide), aTheoretical);
	}

	@Override
	public synchronized long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.doEnergyInjection(aEnergyType, delegateAdjacentSide(aSide), aSize, aAmount, aDoInject);
	}

	@Override
	public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.doEnergyExtraction(aEnergyType, delegateAdjacentSide(aSide), aSize, aAmount, aDoExtract);
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.getEnergyOffered(aEnergyType, delegateAdjacentSide(aSide), aSize);
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.getEnergyDemanded(aEnergyType, delegateAdjacentSide(aSide), aSize);
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.getEnergySizeInputRecommended(aEnergyType, delegateAdjacentSide(aSide));
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		if (aSide < 0 || aSide >= 6) return 0;
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? 0 : tDelegate.getEnergySizeOutputRecommended(aEnergyType, delegateAdjacentSide(aSide));
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		if (aSide < 0 || aSide >= 6) return Collections.emptyList();
		ITileEntityEnergy tDelegate = delegateEnergy(aSide);
		return tDelegate == null ? Collections.emptyList() : tDelegate.getEnergyTypes(delegateAdjacentSide(aSide));
	}

	// ---------------------------------------------------------------------------
	// redstone/comparator surface (the Block bridge consumes these)
	// ---------------------------------------------------------------------------

	/** Upstream isProvidingWeakPower :346-348 — the vanilla query side folds through OPOS. */
	public int redstoneOut(byte aQuerySide) {
		return mRedstone[UT6.OPOS[aQuerySide]];
	}

	/** Upstream getComparatorInputOverride :341-343 — the own per-face comparator buffer. */
	public int comparatorOut(byte aSide) {
		return mComparator[aSide];
	}

	/** Upstream Root.getRedstoneIncoming :577-588 — the neighbour signal, bind4-clamped (ICoverableTE.worldRedstoneIn convention). */
	public byte getRedstoneIncoming(byte aSide) {
		Level tLevel = getLevel();
		if (tLevel == null || aSide < 0 || aSide >= 6) return 0;
		Direction tFace = Direction.from3DDataValue(aSide);
		return bind4(tLevel.getSignal(getBlockPos().relative(tFace), tFace));
	}

	/**
	 * Upstream Root.getComparatorIncoming :591-595 — a neighbour with an analog output
	 * answers with it, anything else falls back to the redstone reading.
	 */
	public byte getComparatorIncoming(byte aSide) {
		Level tLevel = getLevel();
		if (tLevel == null || aSide < 0 || aSide >= 6) return 0;
		BlockPos tPos = getBlockPos().relative(Direction.from3DDataValue(aSide));
		BlockState tState = tLevel.getBlockState(tPos);
		if (tState.hasAnalogOutputSignal()) return bind4(tState.getAnalogOutputSignal(tLevel, tPos));
		return getRedstoneIncoming(aSide);
	}

	/** Upstream UT.Code.bind4 (UT.java:1556) — the 0..15 redstone scale clamp. */
	private static byte bind4(long aValue) {
		return (byte) Math.max(0, Math.min(15, aValue));
	}

	/** The family display-name hook (upstream getTileEntityName; the display key rides the block/lang). */
	@Override
	public abstract String getTileEntityName();
}
