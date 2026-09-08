package gregtech6.tileentity.connectors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.client.render.GTRenderUpdates;
import gregtech6.fluid.FluidTankGT;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of gregapi/tileentity/connectors/MultiTileEntityPipeFluid.java
 * (533 lines) — the trimmed direct translation (task p4-fluid-pipes spec ③).
 *
 * <p>The GT6 fluid transport semantics are kept verbatim: there is NO network object —
 * every pipe segment is its own tank+ pump that pushes fluid to its neighbours every few
 * ticks (upstream class doc and distribute :341-429). Port scope:
 * <ul>
 * <li>{@code mTanks} — a {@link FluidTankGT} per tank slot (upstream :77, W1 ships the
 *     single-tank wood tiers, tank count is block-derived where upstream read
 *     NBT_TANK_COUNT/:116-126);</li>
 * <li>{@link #distribute} — the pressure-equalising push (:341-429): targets are adjacent
 *     pipes whose receiving tank holds LESS than ours (:387, pressure driven) and adjacent
 *     IFluidHandlers probed with two simulated fills (:403); the total is split with
 *     divup over targets+self (:415), pipes filled before tanks (:417/:421) and the
 *     leftover above the bandwidth {@code mCapacity/2} pushed a second time (:427-428);</li>
 * <li>anti-backflow — {@code mLastReceivedFrom[tankIndex]} SBIT mask (:74): external
 *     fills record their source side (SideFluidHandler :487), pushes record the receiving
 *     neighbour's mask (:389), and distribute skips the masked sides (:378) until the
 *     mask clears at the end of the round (:331);</li>
 * <li>phase staggering — the upstream SERVER_TICK_PRE/PR2 coordinate-parity split
 *     (:128-135/:234-245) becomes a BE-ticker gate {@code (timer + offset) % 5 == 0} with
 *     a random per-segment offset, the GTCEu FREQUENCY+offset precedent
 *     (FluidPipeBlockEntity.java:67/:75/:154);</li>
 * <li>connection — the {@link TileEntityBase09Connector} handshake with
 *     {@link TD.Connectors#PIPE_FLUID} types (:528) and the fluid-handler canConnect hook
 *     (:493-508, minus the extenders/cauldron branches);</li>
 * <li>capability — the side-wrapped {@link SideFluidHandler} via
 *     {@code getCapability(FLUID_HANDLER, Direction)} (spec ⑤);</li>
 * <li>ownership — the {@code mOwnable}/{@code mOwner} pair + {@link #allowInteraction(UUID)}
 *     and the live gates (break/use/connect-neighbour; task p24-pipe-owner);</li>
 * <li>C-Foam — the {@code mFoam}/{@code mFoamDried} pair with the applyFoam/dryFoam/
 *     removeFoam write points and the drying ticker (task p25-c-foam-pipe-spray, the
 *     upstream TileEntityBase10ConnectorRendered foam stratum :57/:99-102/:153-183).</li>
 * </ul>
 *
 * <p>Cuts (pool, per the card): corrosion leaks, over-temperature ignition and the entity
 * damage loop (:285-327), the vanilla cauldron filling speciality (:343-364), the break
 * dump (:432-445), the gibbl/temperature/progress telemetry interfaces, the magnifier
 * network dump and the plunger. The bandwidth rule survives inside the leftover push.
 */
public class GTFluidPipeBlockEntity extends TileEntityBase09Connector {

	/** Distribution rounds per tick-window (GTCEu FluidPipeBlockEntity FREQUENCY=5, :67). */
	public static final int DISTRIBUTION_PERIOD = 5;

	/** NBT keys — the upstream "gt.tank.i"/"gt.mlast.i"/"gt.mtransfer" family (:120-121/:142-145) in the in-repo plain key form. */
	public static final String NBT_TANK_PREFIX = "tank.";
	public static final String NBT_LAST_PREFIX = "last.";
	public static final String NBT_TRANSFERRED = "transferred";

	/** The ioMask NBT key (task p4-pipe-flow-control spec ⑤ — the only new key of the card). */
	public static final String NBT_IO_MASK = "ioMask";

	/** Upstream :74 — one 6-bit source mask per tank. */
	public byte[] mLastReceivedFrom = new byte[0];

	/** Upstream :75 — the per-round transferred amount (persisted, upstream :107/:145). */
	public long mTransferredAmount = 0;

	/** Upstream :75/:114 — per-tank capacity; the bandwidth is mCapacity/2 (:427). */
	public long mCapacity = 1000;

	/** Upstream :77. */
	public FluidTankGT[] mTanks = new FluidTankGT[0];

	// ---------------------------------------------------------------------------
	// ownership (task p24-pipe-owner — upstream TileEntityBase10ConnectorRendered:57 +
	// TileEntityBase03TicksAndSync:40/:106-108, foam-free simplification)
	// ---------------------------------------------------------------------------

	/** Upstream NBT_OWNABLE = "gt.ownable" (CS.java:1167, Boolean) — the key name is verbatim upstream (the gt.color/gt.painted literal-key precedent, 03 port :78-82). */
	public static final String NBT_OWNABLE = "gt.ownable";

	/** Upstream NBT_OWNER = "gt.owner" (CS.java:1168) — the VALUE form is the vanilla CompoundTag.putUUID/getUUID/hasUUID pair (the int-array UUID form, both legs NbtUtils.createUUID = IntArrayTag; declared deviation from the upstream String form, no 1.7.10-world migration exists). */
	public static final String NBT_OWNER = "gt.owner";

	/** Upstream NBT_FOAMED = "gt.foamed" (CS.java:1214, Boolean). */
	public static final String NBT_FOAMED = "gt.foamed";

	/** Upstream NBT_FOAMDRIED = "gt.foamdried" (CS.java:1215, Boolean). */
	public static final String NBT_FOAMDRIED = "gt.foamdried";

	/**
	 * Upstream TileEntityBase10ConnectorRendered.java:57 ({@code mOwnable = F}). False by
	 * default: a freshly placed pipe NEVER locks — the ONLY runtime activation is a dried
	 * owned foam (the applyFoam write point below, task p25-c-foam-pipe-spray).
	 */
	public boolean mOwnable = false;

	/** Upstream TileEntityBase03TicksAndSync.java:40 — null = unowned. */
	@Nullable
	public UUID mOwner = null;

	/**
	 * The upstream three-clause predicate verbatim, TileEntityBase10ConnectorRendered
	 * .java:153-156 ({@code !mOwnable || !mFoamDried || super}) over the 03 base
	 * :106-108 core ({@code mOwner == null || (aEntity != null && mOwner.equals(...))}
	 * with the Entity unwrapped to its UUID — the offline-test discipline forbids
	 * constructing Players/Entities). The {@code !mFoamDried} third clause (task
	 * p25-c-foam-pipe-spray, the javadoc obligation of the p24 fold): UNLESS the foam has
	 * dried, the ownership half is bypassed entirely — an undried (or unfoamed) pipe
	 * passes everyone, and only a dried foam arms the lock.
	 *
	 * <p>Null owner = everyone passes (upstream :107 arm 1); a null aUUID against a set
	 * owner denies (the :107 {@code aEntity != null} arm — the console is nobody).
	 */
	public boolean allowInteraction(@Nullable UUID aUUID) {
		return !mOwnable || !mFoamDried || mOwner == null || (aUUID != null && mOwner.equals(aUUID));
	}

	/**
	 * The break-gate decision seam (task p24-pipe-owner, the creative-form-seam precedent):
	 * a denied breaker gets 0.0F — progress never accrues, the upstream
	 * TileEntityBase01Root.java:943 {@code getPlayerRelativeBlockHardness} deny-to-0
	 * counterpart — an allowed one gets the caller's super progress unchanged. Static and
	 * Player-free so the offline tests drive it directly. Public (the ruling's
	 * "package-private" narrows here): its two consumers live in different packages — the
	 * gregtech6.block.pipe Block override AND this offline test package — and the single
	 * decision body must serve both (the creative-form-seam could stay package-private
	 * only because its seam and test shared one package).
	 */
	public static float ownerDestroyProgress(@Nullable GTFluidPipeBlockEntity aPipe, float aSuperProgress, @Nullable UUID aUUID) {
		if (aPipe == null || aPipe.allowInteraction(aUUID)) return aSuperProgress;
		return 0.0F;
	}

	// ---------------------------------------------------------------------------
	// C-Foam (task p25-c-foam-pipe-spray — the upstream TileEntityBase10ConnectorRendered
	// foam stratum: fields :57, write points :159-183, drying :99-102, queries :215-217)
	// ---------------------------------------------------------------------------

	/** Upstream :57 {@code mFoam = F} — the pipe currently carries wet or dried C-Foam. */
	public boolean mFoam = false;

	/** Upstream :57 {@code mFoamDried = F} — the foam has hardened (arms the lock, the full-block collision/light swap). */
	public boolean mFoamDried = false;

	/** The drying gate's minimum age (upstream :99 {@code aTimer >= 100}). */
	public static final int DRY_MIN_AGE = 100;

	/** The drying roll bound (upstream :99 {@code rng(5900) == 0} — mean ~5900 ticks ≈ 295 s). */
	public static final int DRY_RNG_BOUND = 5900;

	/** Upstream :144 {@code LIGHT_OPACITY_MAX} — the dried-foam light block (the vanilla max, 15). */
	public static final int FOAM_LIGHT_OPACITY = 15;

	/**
	 * The upstream applyFoam verbatim (:159-166), with two recorded folds:
	 * <ul>
	 * <li>the {@code mDiameter >= 1.0F} arm is CUT (declared deviation A — the port pipe
	 *     has no diameter field, the clause is the constant false);</li>
	 * <li>the {@code short[] aCFoamRGB + UT.Code.getRGBInt} colour walk folds to the final
	 *     0xRRGGBB int — the spray can carries the {@code DYES_INT} table value directly
	 *     (the p22 GTSprayCanItem colour form; {@code getRGBInt} is identity over it).</li>
	 * </ul>
	 * The OWNERSHIP_RESET clause of upstream :162 folds away (the p24 recorded deviation,
	 * CS.java:866 defaults false). Spraying PAINTS the pipe the foam colour (upstream :161
	 * {@code mIsPainted = T} + :163 mRGBa — the IPaintableTE stratum of the 03 base).
	 *
	 * <p>Gates: already foamed (wet OR dried) refuses, client side refuses, and the
	 * allowInteraction gate refuses a locked pipe's non-owner RE-spray (upstream :160
	 * last arm). The side parameter is interface-uniform (the upstream body ignores it).
	 *
	 * @return true when the foam landed (the caller pays its 10 internal units, upstream
	 *         Behavior_Spray_Foam.java:114)
	 */
	public boolean applyFoam(byte aSide, @Nullable UUID aSprayer, int aRGB, boolean aOwned) {
		if (mFoam || mFoamDried || isClientSide() || !allowInteraction(aSprayer)) return false; // upstream :160, mDiameter arm cut (deviation A)
		mFoam = true; mFoamDried = false; mIsPainted = true; mOwnable = aOwned; // upstream :161
		if (mOwnable && aSprayer != null) mOwner = aSprayer; // upstream :162, OWNERSHIP_RESET folded (p24 deviation)
		mRGBa = aRGB; // upstream :163 UT.Code.getRGBInt over the spray colour
		foamChanged(); // upstream :164 updateClientData + the port's chunk-dirty/render-update pair
		return true;
	}

	/**
	 * The upstream dryFoam verbatim (:169-174) — NO allowInteraction gate (the upstream
	 * asymmetry: anyone may harden a wet foam, only the owner may remove a dried one).
	 * The dead {@code mFoam = T} store is kept verbatim (the card's "无门逐字保留").
	 *
	 * @return true when a wet foam got hardened
	 */
	public boolean dryFoam(byte aSide, @Nullable UUID aPlayer) {
		if (!mFoam || mFoamDried || isClientSide()) return false; // upstream :170
		mFoam = true; mFoamDried = true; // upstream :171 verbatim
		foamChanged(); // upstream :172
		return true;
	}

	/**
	 * The upstream removeFoam verbatim (:177-183): only a DRIED foam removes, and only
	 * through the allowInteraction gate (a locked pipe keeps every non-owner out). The
	 * reset form clears all four foam-ownership fields and UNPAINTS (upstream :180 — the
	 * applyFoam paint write-point's symmetric revert; live because the pipe BE rides the
	 * IPaintableTE stratum of the 03 base, the deviation-D verification).
	 *
	 * @return true when the foam got removed
	 */
	public boolean removeFoam(byte aSide, @Nullable UUID aPlayer) {
		if (!mFoam || !mFoamDried || isClientSide() || !allowInteraction(aPlayer)) return false; // upstream :178
		mFoam = false; mFoamDried = false; mOwnable = false; mOwner = null; // upstream :179
		unpaint(); // upstream :180
		foamChanged(); // upstream :181
		return true;
	}

	/** Upstream :215 — foamed on any face (the pipe carries one foam state across all faces). */
	public boolean hasFoam(byte aSide) {
		return mFoam;
	}

	/** Upstream :216. */
	public boolean driedFoam(byte aSide) {
		return mFoam && mFoamDried;
	}

	/** Upstream :217. */
	public boolean ownedFoam(byte aSide) {
		return mFoam && mOwnable;
	}

	/**
	 * The upstream drying ticker verbatim (:99-102): a matured wet foam hardens on a
	 * 1/{@link #DRY_RNG_BOUND} per-tick roll. Ungated by the distribution phase — upstream
	 * runs it in onTick2, every server tick. The roll rides {@link #rng(int)} — the
	 * injection seam the deterministic tests override.
	 */
	private void tickFoamDrying(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide && aTimer >= DRY_MIN_AGE && mFoam && !mFoamDried && rng(DRY_RNG_BOUND) == 0) {
			mFoamDried = true;
			foamChanged(); // upstream :101 updateClientData + the port sync pair
		}
	}

	/**
	 * The uniform random roll (the upstream 01Root rng face over the vanilla level random)
	 * — {@code protected} so the offline tests inject a deterministic roll (the
	 * creative-form-seam precedent). Level-less BEs roll 0 (a fresh offline BE dries on its
	 * first matured tick unless the test overrides — deterministic either way).
	 */
	protected int rng(int aBound) {
		return hasLevel() ? getLevel().random.nextInt(aBound) : 0;
	}

	/**
	 * Every foam state change: {@code setChanged()} (the chunk-dirty flag, the
	 * onFilledFrom port precedent) + {@code updateClientData()} (the upstream :101/:164/:181
	 * call — both sync channels carry the foam/paint keys on the next window) + the
	 * C-grade render-update pair (the snapshot the dynamic foam model reads must refresh).
	 */
	private void foamChanged() {
		setChanged();
		updateClientData();
		GTRenderUpdates.scheduleRenderUpdate(this);
	}

	// ---------------------------------------------------------------------------
	// dried-foam static seams (task p25-c-foam-pipe-spray spec ⑥ — the
	// ownerDestroyProgress shape: the block overrides stay BE-lookup + unwrap only)
	// ---------------------------------------------------------------------------

	/**
	 * The dried light-block seam (the vanilla TintedGlassBlock.java:19 precedent): dried
	 * foam blocks ALL light (upstream :144 {@code getLightOpacity() → LIGHT_OPACITY_MAX}
	 * when dried), everything else passes the super value. Static and Player-free for the
	 * offline tests; the Block override is the only live caller.
	 */
	public static int foamLightBlock(@Nullable GTFluidPipeBlockEntity aPipe, int aSuperLight) {
		return aPipe != null && aPipe.mFoamDried ? FOAM_LIGHT_OPACITY : aSuperLight;
	}

	/**
	 * The dried collision seam (upstream :218 {@code addDefaultCollisionBoxToList() =
	 * mDiameter >= 1.0F || mFoamDried} with the diameter arm cut — deviation A): dried
	 * foam collides as a FULL block (walk-on surface, the hardened-foam floor), everything
	 * else keeps the super shape. Static for the offline tests; the Block override is the
	 * only live caller.
	 */
	public static VoxelShape foamCollisionShape(@Nullable GTFluidPipeBlockEntity aPipe, VoxelShape aSuperShape) {
		return aPipe != null && aPipe.mFoamDried ? Shapes.block() : aSuperShape;
	}

	/** The random phase offset within {@link #DISTRIBUTION_PERIOD} (assigned on the first server tick, GTCEu offset :75). */
	private int mPhaseOffset = 0;
	private boolean mPhaseAssigned = false;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTFluidPipeBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime, tests pass an offline-built BET. The tank shape comes
	 * from the block tier (the 1.20.1 counterpart of the registration NBT_TANK_CAPACITY,
	 * upstream :114) — vanilla blocks fall back to the upstream default 1000 (:75).
	 */
	public GTFluidPipeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : gregtech6.registry.GTFluidPipes.FLUID_PIPE_BE.get(), aPos, aState);
		mCapacity = aState.getBlock() instanceof gregtech6.block.pipe.GTFluidPipeBlock tPipe ? tPipe.capacityPerTank() : 1000;
		mTanks = new FluidTankGT[] {new FluidTankGT(mCapacity).setIndex(0)};
		mLastReceivedFrom = new byte[1];
	}

	@Override
	public String getTileEntityName() {
		return "fluid_pipe"; // BET registry path mirrors it (GTFluidPipes.FLUID_PIPE_BE)
	}

	// ---------------------------------------------------------------------------
	// tick: phase-gated distribution (upstream onServerTickPre :257-338, trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide && hasLevel()) {
			// the random stagger of the two-phase upstream tick lists (:128-135) — GTCEu offset style
			mPhaseOffset = getLevel().random.nextInt(DISTRIBUTION_PERIOD);
			mPhaseAssigned = true;
			// NO auto-handshake here (task p4-pipe-flow-control spec ② — the new baseline is
			// "GT6 pipes never auto-connect"): upstream does its placement connect in onPlaced
			// (TileEntityBase09Connector.java:82-96, driven here by the BlockItem place chain),
			// everything after that is manual per-face work. The W1 first-tick all-sides
			// handshake would resurrect connections the user manually disconnected (mConnections
			// persists across chunk loads, upstream :53-62).
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		tickFoamDrying(aTimer, aIsServerSide); // upstream 10ConnectorRendered:99-102 — ungated by the distribution phase
		if (aIsServerSide && mPhaseAssigned && (aTimer + mPhaseOffset) % DISTRIBUTION_PERIOD == 0) {
			mTransferredAmount = 0; // upstream :258
			for (FluidTankGT tTank : mTanks) {
				if (tTank.has()) distribute(tTank);
				mLastReceivedFrom[tTank.mIndex] = 0; // upstream :331 — the mask guards exactly one round
			}
		}
	}

	// ---------------------------------------------------------------------------
	// distribute (upstream :341-429, cauldron/cover branches cut)
	// ---------------------------------------------------------------------------

	/** Upstream :415 UT.Code.divup — ceiling division. */
	public static long divup(long aNumber, long aDivider) {
		return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);
	}

	/**
	 * Upstream :341-429. Pushes from aTank into every adjacent target: connected pipes
	 * whose fillable tank holds less than ours, and fluid handlers that accept the fluid.
	 */
	public void distribute(FluidTankGT aTank) {
		if (aTank.isEmpty() || !hasLevel()) return;

		List<FluidTankGT> tPipes = new ArrayList<>();
		List<IFluidHandler> tTanks = new ArrayList<>();
		long tAmount = aTank.amount(); // :372
		int tTargetCount = 1;         // :374 — includes THIS for even distribution

		for (byte tSide = 0; tSide < 6; tSide++) {
			// :378 — don't you dare flow backwards!
			if ((mLastReceivedFrom[aTank.mIndex] & SBIT[tSide]) != 0) continue;
			if (!canEmitFluidsTo(tSide)) continue; // :380

			Direction tDirection = Direction.from3DDataValue(tSide);
			byte tOpposite = (byte)tDirection.getOpposite().get3DDataValue();
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
			if (tNeighbor == null) continue;

			// :384-398 — a neighbouring pipe with a fillable, less-full tank
			if (tNeighbor instanceof GTFluidPipeBlockEntity tPipe) {
				FluidTankGT tTank = tPipe.getFluidTankFillable(tOpposite, aTank.get());
				if (tTank != null && tTank.amount() < aTank.amount()) {
					tPipe.mLastReceivedFrom[tTank.mIndex] |= SBIT[tOpposite]; // :389 — mark the receiver
					tPipes.add(getLevel().random.nextInt(tPipes.size() + 1), tTank); // :391 random position
					tAmount += tTank.amount(); // :393
					tTargetCount++;
				}
				continue;
			}

			// :401-410 — any other fluid handler, probed with 1 L and then the full stack (both simulate).
			// spec ① (task p5-pipe-flow-semantics): the arrow mask is an OPT-IN pump valve on top
			// of the GT6 default — mask == 0 keeps the upstream all-faces push (upstream :400-410
			// gates only on backflow :378 + canEmitFluidsTo :380 + cover :382, no ioMask concept);
			// a non-zero mask restricts the push to the arrow faces. Pipe-to-pipe equalisation
			// above is NOT gated.
			if (!externalPushAllowed(tSide)) continue;
			//? if forge {
			IFluidHandler tHandler = tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, tDirection.getOpposite()).orElse(null);
			//?} else {
			/*IFluidHandler tHandler = getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tNeighbor.getBlockPos(), tDirection.getOpposite());
			 *///?}
			if (tHandler == null) continue;
			FluidStack tProbe1 = aTank.get(1), tProbeAll = aTank.get(Long.MAX_VALUE);
			if ((tProbe1 != null && tHandler.fill(tProbe1, IFluidHandler.FluidAction.SIMULATE) > 0)
					|| (tProbeAll != null && tHandler.fill(tProbeAll, IFluidHandler.FluidAction.SIMULATE) > 0)) {
				tTanks.add(getLevel().random.nextInt(tTanks.size() + 1), tHandler); // :405 random position
				tTargetCount++;
			}
		}

		if (tTargetCount <= 1) return; // :413
		tAmount = divup(tAmount, tTargetCount); // :415

		// :417 — pipes first
		for (FluidTankGT tPipe : tPipes) {
			mTransferredAmount += aTank.remove(tPipe.add(aTank.amount(tAmount - tPipe.amount()), aTank.get()));
		}
		if (aTank.isEmpty()) return; // :419

		// :421 — tanks afterwards
		for (IFluidHandler tHandler : tTanks) {
			FluidStack tOffer = aTank.get(tAmount);
			if (tOffer == null) break;
			mTransferredAmount += aTank.remove(tHandler.fill(tOffer, IFluidHandler.FluidAction.EXECUTE));
			if (aTank.isEmpty()) break; // upstream :423 between-targets guard
		}
		if (aTank.isEmpty()) return;

		// :425-428 — leftover pressure above the bandwidth (mCapacity/2 L) pushes to pipes again
		if (tPipes.isEmpty()) return;
		long tLeftover = (aTank.amount() - mCapacity / 2) / tPipes.size();
		if (tLeftover > 0) for (FluidTankGT tPipe : tPipes) {
			mTransferredAmount += aTank.remove(tPipe.add(aTank.amount(tLeftover), aTank.get()));
		}
	}

	// ---------------------------------------------------------------------------
	// fill/drain tank lookups (upstream getFluidTankFillable2 :463-468 / getFluidTankDrainable2 :471-475)
	// ---------------------------------------------------------------------------

	/** Upstream :463-468 — a containing tank first, then an empty one; side-gated by canAcceptFluidsFrom. */
	@Nullable
	public FluidTankGT getFluidTankFillable(byte aSide, @Nullable FluidStack aFluid) {
		if (aSide >= 0 && aSide < 6 && !canAcceptFluidsFrom(aSide)) return null;
		if (aFluid == null || aFluid.isEmpty()) return null;
		for (FluidTankGT tTank : mTanks) if (tTank.contains(aFluid)) return tTank;
		for (FluidTankGT tTank : mTanks) if (tTank.isEmpty()) return tTank;
		return null;
	}

	/** Upstream :471-475 — a containing tank only; side-gated by canEmitFluidsTo. */
	@Nullable
	public FluidTankGT getFluidTankDrainable(byte aSide, @Nullable FluidStack aFluid) {
		if (aSide >= 0 && aSide < 6 && !canEmitFluidsTo(aSide)) return null;
		if (aFluid == null || aFluid.isEmpty()) return null;
		for (FluidTankGT tTank : mTanks) if (tTank.contains(aFluid)) return tTank;
		return null;
	}

	/** Upstream :485-488 — the external-fill record: source side into the mask, then dirty + client sync. */
	public void onFilledFrom(byte aSide, FluidTankGT aTank) {
		if (aSide >= 0 && aSide < 6) {
			mLastReceivedFrom[aTank.mIndex] |= SBIT[aSide];
		}
		setChanged();
		updateClientData();
	}

	/** Upstream :510. */
	public boolean canEmitFluidsTo(byte aSide) {
		return connected(aSide);
	}

	/** Upstream :511. */
	public boolean canAcceptFluidsFrom(byte aSide) {
		return connected(aSide);
	}

	// ---------------------------------------------------------------------------
	// connection (upstream :493-511/:528)
	// ---------------------------------------------------------------------------

	/** Upstream :493-508 — non-connector neighbours: fluid handlers with at least one tank. */
	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (aNeighbor == null) return false;
		//? if forge {
		IFluidHandler tHandler = aNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.from3DDataValue(aSide).getOpposite()).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = getLevel().getCapability(Capabilities.FluidHandler.BLOCK, aNeighbor.getBlockPos(), Direction.from3DDataValue(aSide).getOpposite());
		 *///?}
		return tHandler != null && tHandler.getTanks() > 0;
	}

	@Override
	public List<TagData> getConnectorTypes(byte aSide) {
		return TD.Connectors.PIPE_FLUID.AS_LIST; // upstream :528
	}

	/** The mask becomes the CONNECTS BlockState — the 1.20.1 visual counterpart of the connection data. */
	@Override
	public void onConnectionChange(byte aPreviousConnections) {
		super.onConnectionChange(aPreviousConnections);
		if (hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS)
					&& tState.getValue(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS) != (int)getConnections()) {
				getLevel().setBlock(getBlockPos(), tState.setValue(gregtech6.block.pipe.GTFluidPipeBlock.CONNECTIONS, (int)getConnections()),
						Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// placement (upstream onPlaced :82-96 — task p4-pipe-flow-control spec ②)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream TileEntityBase09Connector.onPlaced (:82-96), server side, driven by the
	 * BlockItem place chain (GTFluidPipeBlockItem.placeBlock — the only vanilla hook that
	 * holds both the live BE and the BlockPlaceContext; the architect ruling rejects a
	 * BE.onLoad first-check because every chunk load would replay the support-side connect
	 * and resurrect manually disconnected pipes). aSide is the CLICKED face
	 * (context.getClickedFace(), 0..5): the pipe side that touches the neighbour is its
	 * opposite — the upstream OPOS flip (:84). A support block that is not a pipe/fluid
	 * container fails the connect → all six sides stay unconnected (upstream :87).
	 *
	 * <p>Console/legacy entry: a null owner (the console is nobody — the /gt6pipe place
	 * driver passes null through {@link #onPlaced(byte, UUID)}).
	 */
	public void onPlaced(byte aSide) {
		onPlaced(aSide, null);
	}

	/**
	 * The owner-carrying placement (task p24-pipe-owner). Order follows upstream: the
	 * owner is recorded FIRST (TileEntityBase10ConnectorRendered.java:148-150 verbatim
	 * {@code if (mOwnable && aPlayer != null) mOwner = aPlayer.getUniqueID()} — the
	 * OWNERSHIP_RESET clause of CS.java:866 defaults false and is not ported, the
	 * constant-fold is the recorded deviation), THEN the support-side neighbour's
	 * ownership gates the connect (upstream 09Connector.java:85-86 — a locked support
	 * pipe {@code return T}s the whole placement, so the :88-93 back-connect loop is
	 * skipped with it).
	 */
	public void onPlaced(byte aSide, @Nullable UUID aOwner) {
		if (aSide < 0 || aSide >= 6 || !hasLevel() || !isServerSide()) return;
		// upstream 10ConnectorRendered:148-150 — a not-ownable pipe never records (default
		// mOwnable=false keeps the live behaviour the upstream plain pipe)
		if (mOwnable && aOwner != null) mOwner = aOwner;
		// upstream 09Connector:86 — the support-side neighbour's allowInteraction before the
		// first connect; the rejection skips the whole placement (return T there)
		BlockEntity tSupport = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(UT6.OPOS[aSide])));
		if (tSupport instanceof GTFluidPipeBlockEntity tPipe && !tPipe.allowInteraction(aOwner)) return;
		connect(UT6.OPOS[aSide], true); // upstream :84/:87
		// upstream :88-93 — symmetric back-connect to neighbouring connectors that already
		// connect towards this pipe (the Delegator mSideOfTileEntity validity check is the
		// opposite-side tautology and folds away with the direct BE access). Upstream has NO
		// allowInteraction gate on this loop — none is added (card ruling).
		for (byte tSide = 0; tSide < 6; tSide++) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tConnector.connected(tOpposite)
						&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(tSide))) {
					connect(tSide, true); // upstream :91
				}
			}
		}
	}

	// ---------------------------------------------------------------------------
	// per-face output arrows (spec ①/③ — the monkeywrench output layer of
	// MultiTileEntityPipeItem.java:128-153, single-layered onto one mask)
	// ---------------------------------------------------------------------------

	/**
	 * The 6-bit output-arrow mask (task p4-pipe-flow-control spec ①): a set bit = the face
	 * pushes to external fluid handlers. Decoupled from the connection state — any face can
	 * carry the arrow, the gate is applied at runtime ({@link #isOutputFace}).
	 */
	public byte mIoMask = 0;

	/** The clamp mirrors getConnections() — the whole 6-bit space. */
	public byte getIoMask() {
		return (byte)(mIoMask & 63);
	}

	/**
	 * spec ③ — the distribute gate for the external IFluidHandler branch: the face carries
	 * the arrow AND is connected. Pipe-to-pipe equalisation is never gated.
	 */
	public boolean isOutputFace(byte aSide) {
		return aSide >= 0 && aSide < 6 && (mIoMask & SBIT[aSide]) != 0 && connected(aSide);
	}

	/**
	 * spec ① (task p5-pipe-flow-semantics) — the full external-push truth table, the
	 * corrected form of the p4 hard gate (which left a mask==0 pipe pushing nowhere):
	 * <ul>
	 * <li>{@code mIoMask == 0} → the GT6 default, every face pushes (the connection gate
	 *     itself stays with canEmitFluidsTo, distribute :380);</li>
	 * <li>{@code mIoMask != 0} → {@link #isOutputFace}, i.e. arrow bit AND connected.</li>
	 * </ul>
	 * Only the distribute external branch consults this — the fill-reject twin lives in
	 * {@link SideFluidHandler#fill}, never in canAcceptFluidsFrom/getFluidTankFillable
	 * (those are shared with the pipe-to-pipe equalisation receiver, distribute :180).
	 */
	public boolean externalPushAllowed(byte aSide) {
		return mIoMask == 0 || isOutputFace(aSide);
	}

	/**
	 * The shift-right-click toggle (the monkeywrench :128-153 single-layered): the side
	 * flips, marked → unmarked or the reverse; the other bits stay untouched; several faces
	 * can be marked at once; no three-state cycle. /gt6pipe output shares this entry.
	 */
	public boolean toggleOutput(byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		mIoMask ^= SBIT[aSide];
		outputChanged();
		return true;
	}

	/** /gt6pipe clear — drop every arrow on the pipe. */
	public void clearOutputs() {
		if (mIoMask == 0) return;
		mIoMask = 0;
		outputChanged();
	}

	/**
	 * The right-click (hoe = the wrench substitute, ToolActions.HOE_DIG — the cover
	 * onCoverToolClick precedent) connection toggle of upstream onToolClick2
	 * (TileEntityBase09Connector.java:70-79): connected → disconnect, else connect. The
	 * air/liquid branch of connect (upstream :141, mdk isAirOrLiquid) stays the open-end
	 * "manual pipe mouth" semantics. /gt6pipe toggle shares this entry.
	 *
	 * <p>Console/legacy entry: a null owner (the console is nobody).
	 */
	public boolean toggleConnection(byte aSide) {
		return toggleConnection(aSide, null);
	}

	/**
	 * The owner-carrying toggle (task p24-pipe-owner). Two gates, both upstream:
	 * <ul>
	 * <li>SELF — upstream TileEntityBase06Covers.java:141 kills every tool on a locked
	 *     host before the tool click even dispatches; the port has no cover layer, so the
	 *     toggle entry carries the counterpart (this is the arm that makes a locked pipe
	 *     reject the /gt6pipe console toggle — the RCON verify arm);</li>
	 * <li>TARGET-SIDE NEIGHBOUR — upstream TileEntityBase09Connector.java:75 checks the
	 *     neighbour on the toggled side before the connect/disconnect (both arms, :76).</li>
	 * </ul>
	 * A rejection flips nothing and returns false (the upstream :75 {@code return 0}).
	 */
	public boolean toggleConnection(byte aSide, @Nullable UUID aOwner) {
		if (aSide < 0 || aSide >= 6) return false;
		if (!allowInteraction(aOwner)) return false; // upstream 06Covers:141 host-tool kill
		if (hasLevel()) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			// upstream 09Connector:75 — the neighbour's ownership gates the whole toggle
			if (tNeighbor instanceof GTFluidPipeBlockEntity tPipe && !tPipe.allowInteraction(aOwner)) return false;
		}
		if (connected(aSide)) return disconnect(aSide, true);
		return connect(aSide, true);
	}

	/** spec ①/⑤ — every arrow change: dirty + the client sync window + the render-update pair. */
	private void outputChanged() {
		setChanged();
		updateClientData();
		GTRenderUpdates.scheduleRenderUpdate(this);
	}

	// ---------------------------------------------------------------------------
	// arrow render + client sync (spec ⑤ — the two 03 sync channels carry the ioMask
	// through saveAdditional/load; the client refreshes the render snapshot on each landing)
	// ---------------------------------------------------------------------------

	@Override
	public void onLoad() {
		super.onLoad();
		scheduleFlowRenderRefresh();
	}

	//? if forge {
	@Override
	public void handleUpdateTag(CompoundTag aTag) {
		super.handleUpdateTag(aTag);
		scheduleFlowRenderRefresh(); // chunk-data channel (login/chunk load)
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection aNet, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket aPacket) {
		super.onDataPacket(aNet, aPacket);
		scheduleFlowRenderRefresh(); // block-update channel (arrow changes)
	}
	//?} else {
	/*@Override
	public void handleUpdateTag(CompoundTag aTag, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.handleUpdateTag(aTag, aProvider);
		scheduleFlowRenderRefresh(); // chunk-data channel (login/chunk load)
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection aNet, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket aPacket, net.minecraft.core.HolderLookup.Provider aProvider) {
		super.onDataPacket(aNet, aPacket, aProvider);
		scheduleFlowRenderRefresh(); // block-update channel (arrow changes)
	}
	// 21.1: both IBlockEntityExtension hooks gain the serialization provider (javap)
	*///?}

	private void scheduleFlowRenderRefresh() {
		if ((mIoMask != 0 || mFoam) && hasLevel() && isClientSide()) GTRenderUpdates.scheduleRenderUpdate(this);
	}

	/**
	 * The C-grade render hook (IForgeBlockEntity.java:174): a pipe with output arrows AND/OR
	 * C-Foam hands the render thread its immutable snapshots — the {@link
	 * gregtech6.client.render.PipeFlowSnapshot} arrows on the flow key and the {@link
	 * gregtech6.client.render.PipeFoamSnapshot} foam state on its own key (the
	 * GTModelProperties single-valued-property coexistence ruling). A plain pipe keeps
	 * {@code ModelData.EMPTY} semantics and renders through the plain blockstate model;
	 * the paint colour rides the 03 base's PAINT supply through the same derived snapshot.
	 */
	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		byte tMask = getIoMask();
		if (!mFoam && tMask == 0) return super.getModelData();
		gregtech6.client.render.GTModelProperties.SnapshotBuilder tBuilder =
				gregtech6.client.render.GTModelProperties.derive(super.getModelData());
		if (tMask != 0) {
			tBuilder.with(gregtech6.client.render.GTModelProperties.RENDER_SNAPSHOT, new gregtech6.client.render.PipeFlowSnapshot(tMask));
		}
		if (mFoam) {
			tBuilder.with(gregtech6.client.render.GTModelProperties.FOAM_SNAPSHOT, new gregtech6.client.render.PipeFoamSnapshot(mFoamDried, mOwnable));
		}
		return tBuilder.build();
	}

	// ---------------------------------------------------------------------------
	// capability (spec ⑤ — the side wrapper per getCapability call, GTCEu IOFluidHandlerList form)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// fresh per-call wrapper: the side is part of the handler identity
			return LazyOptional.of(() -> new SideFluidHandler(this, aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — W4's
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// Fresh per-call wrapper kept: the side is part of the handler identity.
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.FluidHandler.BLOCK) {
			return (T) new SideFluidHandler(this, aSide);
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (upstream :105-146, tank contents only — the tank shape is block-derived)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		for (int i = 0; i < mTanks.length; i++) {
			mTanks[i].writeToNBT(aNBT, NBT_TANK_PREFIX + i);
			aNBT.putByte(NBT_LAST_PREFIX + i, mLastReceivedFrom[i]);
		}
		aNBT.putLong(NBT_TRANSFERRED, mTransferredAmount);
		aNBT.putByte(NBT_IO_MASK, mIoMask); // spec ⑤ — the plain-key byte (bit0-5)
		// upstream TileEntityBase10ConnectorRendered:75-76 (UT.NBT.setBoolean, written
		// unconditionally so the loot copy_nbt always sees the keys) + :77-78 (ownable +
		// the non-null owner guard) — the value forms are the vanilla putBoolean/putUUID pair
		aNBT.putBoolean(NBT_FOAMED, mFoam);
		aNBT.putBoolean(NBT_FOAMDRIED, mFoamDried);
		aNBT.putBoolean(NBT_OWNABLE, mOwnable);
		if (mOwner != null) aNBT.putUUID(NBT_OWNER, mOwner);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		for (int i = 0; i < mTanks.length; i++) {
			if (aNBT.contains(NBT_TANK_PREFIX + i, Tag.TAG_COMPOUND)) {
				mTanks[i].readFromNBT(aNBT, NBT_TANK_PREFIX + i);
			}
			mLastReceivedFrom[i] = aNBT.getByte(NBT_LAST_PREFIX + i);
		}
		if (aNBT.contains(NBT_TRANSFERRED, Tag.TAG_ANY_NUMERIC)) {
			mTransferredAmount = aNBT.getLong(NBT_TRANSFERRED);
		}
		if (aNBT.contains(NBT_IO_MASK, Tag.TAG_ANY_NUMERIC)) {
			mIoMask = (byte)(aNBT.getByte(NBT_IO_MASK) & 63); // bit0-5 clamp, the mConnections form
		}
		// upstream TileEntityBase10ConnectorRendered readFromNBT2 :65-66 (FOAMDRIED first,
		// then FOAMED — the order kept) and :67-68 hasKey-guarded ownable/owner pair; the
		// OWNERSHIP_RESET clause (CS.java:866, default F) is not ported — the constant-fold
		// drops it (recorded deviation). The UUID reads back hasUUID-guarded.
		if (aNBT.contains(NBT_FOAMDRIED, Tag.TAG_ANY_NUMERIC)) {
			mFoamDried = aNBT.getBoolean(NBT_FOAMDRIED);
		}
		if (aNBT.contains(NBT_FOAMED, Tag.TAG_ANY_NUMERIC)) {
			mFoam = aNBT.getBoolean(NBT_FOAMED);
		}
		if (aNBT.contains(NBT_OWNABLE, Tag.TAG_ANY_NUMERIC)) {
			mOwnable = aNBT.getBoolean(NBT_OWNABLE);
		}
		if (aNBT.hasUUID(NBT_OWNER)) {
			mOwner = aNBT.getUUID(NBT_OWNER);
		}
	}
}
