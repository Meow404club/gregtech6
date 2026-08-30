package gregtech6.tileentity.tank;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.client.render.GTModelProperties;
import gregtech6.client.render.GTRenderUpdates;
import gregtech6.covers.CoverData;
import gregtech6.covers.GTCoverRenderSnapshot;
import gregtech6.covers.ICoverableTE;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of gregapi/tileentity/tank/TileEntityBase08Barrel.java (317 lines)
 * — the sticky-tank barrel family base, trimmed direct translation (task p4-fluid-barrel
 * spec ①). The base chain TileEntityBase07Paintable maps onto the in-repo
 * {@link TileEntityBase03TicksAndSync} (the 01Root→03 port carries the tick dispatcher
 * and the two sync channels).
 *
 * <p>Port scope:
 * <ul>
 * <li>{@code mTank = new FluidTankGT(16000)} (:53) — the W1 long-amount tank;</li>
 * <li>{@code setPreventDraining(keepsFilter())} (:69) — the stickiness wiring, re-applied
 *     on every load so a keepsFilter barrel keeps its fluid identity even drained to
 *     0 L (the {@code MultiTileEntityBarrelLogistics} override is the T case,
 *     Logistics.java:40); the capacity rides NBT_TANK_CAPACITY (:69, the registration
 *     NBT row Loader_MultiTileEntities.java:2136) — the W1 block-derived-shape deviation
 *     applies: the override is a read seam only, the default is the class 16000;</li>
 * <li>the melt-down judgment {@code FL.temperature >= mMeltingPoint} (:66/:162) with the
 *     1.20.1 temperature home on the FluidType (FluidType.java:190
 *     {@code getTemperature()}); {@code mMeltingPoint} defaults to MAX_VALUE (:55) and
 *     the registration {@code NBT_CAPACITY_HU} row (upstream "gt.capacity.hu",
 *     CS.java:1355) overrides it at load — the wood barrel's 340 K comes from the
 *     block property (GTBarrelBlock, the W1 registration-NBT-carrier pattern);</li>
 * <li>{@link #meltdown()} (:220-231): the lava branch (remove 1000, replace the block
 *     with lava, :221-225) and the fire branch (:226-227), both level-guarded for the
 *     offline unit tests; the tank trash is the in-memory half and always runs;</li>
 * <li>the {@code IFluidHandler} capability (spec ①, W1 same shape): a fresh per-call
 *     {@link BarrelFluidHandler} side wrapper from
 *     {@code getCapability(FLUID_HANDLER, Direction)} (GTFluidPipeBlockEntity.java:297-303
 *     precedent; the 1.20.1 interface carries no side argument).</li>
 * </ul>
 *
 * <p>Cuts (pool, per the card spec ③): the sealed fermentation (RM.Fermenter :190 and the
 * mMode sealed bit with its fill/drain gates), the vertically-connected-tank mode B[0]
 * (:133/:209-213) and the gas/acid/plasma/magic proof quartet (:164-186/:251-254). With
 * both mode bits gone the {@code mMode} field itself has no consumer and is cut; the
 * item-form {@code IFluidContainerItem} face, the tool clicks (:106-154), the funnel/tap
 * interfaces, the rotting and the {@code onlySimple()} filter (FL.simple infra absent)
 * are feature-layer omissions. The world-fill path deliberately has NO fill-time fluid
 * gate: upstream fills first and melts on the next tick (:162 is the protection), the
 * {@code allowFluid} temperature gate (:233-235) belonged to the cut item face.
 *
 * <p>Cover wiring (task p5-barrel-side-rules spec ④ — the oven :167-177/:221/:230/:241/:652
 * template, composition over the {@link ICoverableTE} defaults): the store lives here, the
 * covers ride the NBT pair and the 03 sync channels, and the barrel hands
 * {@code mTank} out through {@link #getCoverPumpTank()} — the pump-cover direct-call seam
 * that lets the pump bypass the {@link BarrelFluidHandler} side rules (the upstream
 * FL.move(IFluidTank, ...) shape, the free reverse-output exemption).
 */
public abstract class TileEntityBase08Barrel extends TileEntityBase03TicksAndSync implements ICoverableTE {

	/** NBT keys — upstream CS.java:1355/:1258/:1262 ("gt.capacity.hu"/"gt.tank"/"gt.tankcap") in the in-repo plain key form. */
	public static final String NBT_CAPACITY_HU = "capacity.hu";
	public static final String NBT_TANK = "tank";
	public static final String NBT_TANK_CAPACITY = "tankcap";

	/** Upstream :53 — the sticky tank; 16000 L is the class default the card pins (acceptance ①). */
	public FluidTankGT mTank = new FluidTankGT(16000);

	/** Upstream :55 — the melt-down ceiling in Kelvin; MAX_VALUE means nothing melts (no material bridge). */
	public long mMeltingPoint = Long.MAX_VALUE;

	/** Upstream 06Covers :63 mCovers — {@code null} while no face carries a cover (the oven template). */
	public CoverData mCovers = null;

	@Override
	public CoverData getCovers() {
		return mCovers;
	}

	@Override
	public void setCovers(CoverData aCoverData) {
		mCovers = aCoverData;
	}

	/**
	 * The p5 pump seam (ICoverableTE.getCoverPumpTank): the pump cover moves fluid straight
	 * through {@code mTank}, bypassing the {@link BarrelFluidHandler} side rules — direct
	 * tank access is the upstream FL.move(IFluidTank, ...) semantics (FL.java:845-846).
	 */
	@Override
	public FluidTankGT getCoverPumpTank() {
		return mTank;
	}

	protected TileEntityBase08Barrel(boolean aIsTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aIsTicking, aType, aPos, aState);
	}

	/** Upstream :284 — the base barrels drain to null; the logistics barrel keeps the filter (Logistics.java:40). */
	public boolean keepsFilter() {
		return false;
	}

	// ---------------------------------------------------------------------------
	// NBT pair (upstream readFromNBT2 :60-70 / writeToNBT2 :73-78, trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_CAPACITY_HU, Tag.TAG_ANY_NUMERIC)) mMeltingPoint = aNBT.getLong(NBT_CAPACITY_HU); // :66
		mTank.setPreventDraining(keepsFilter()); // :69 — stickiness re-applied on every load
		if (aNBT.contains(NBT_TANK_CAPACITY, Tag.TAG_ANY_NUMERIC)) mTank.setCapacity(aNBT.getLong(NBT_TANK_CAPACITY));
		mTank.readFromNBT(aNBT, NBT_TANK); // :69
		readCoversFromNBT(aNBT); // upstream 06Covers :68 — the covers ride the tank NBT pair
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTank.writeToNBT(aNBT, NBT_TANK); // :77 (mode/progress ride the cut sealed-fermentation pool)
		writeCoversToNBT(aNBT); // upstream 06Covers :74
	}

	// ---------------------------------------------------------------------------
	// tick chain: covers validity + pre/post + the melt-down judgment (:157-218 trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide) {
			checkCoverValidity(); // upstream 06Covers :191 — the admission sweep rides onTickFirst (oven :221 template)
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// upstream 06Covers :200 — the cover tick precedes the barrel business (oven :230 template)
		if (hasCovers()) getCovers().tickPre(aTimer, aIsServerSide, mBlockUpdated, false);
		if (aIsServerSide) {
			FluidStack tFluid = mTank.getFluid();
			if (tFluid == null || tFluid.isEmpty() || tFluid.getAmount() <= 0) return; // :160-161
			if (meltsDown(tFluid) && meltdown()) return; // :162
		}
		// upstream 06Covers :202 — the cover tick follows the barrel business (oven :241 template)
		if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, false);
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream 06Covers :184-186 — the cover visual sync opens the 03 sync window
		return (hasCovers() && getCovers().requiresSync()) || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickChecked(long aTimer) {
		super.onTickChecked(aTimer);
		if (hasCovers()) getCovers().resetSync(); // upstream 06Covers :178-181
	}

	/**
	 * The :162 judgment — {@code FL.temperature(tFluid) >= mMeltingPoint}. The 1.20.1
	 * temperature home is the FluidType (FluidType.java:190): vanilla water sits at 300 K,
	 * lava at 1300 K, and the W1 {@code gt6:iron_molten} at 1811 K melts a 340 K wood barrel.
	 * The FluidType lookup needs the live registry (ForgeHooks.getVanillaFluidType resolves
	 * through RegistryObjects) — offline tests drive the raw comparison below.
	 */
	public boolean meltsDown(FluidStack aFluid) {
		return meltsDown(fluidTemperature(aFluid));
	}

	/** The raw :162 comparison — {@code FL.temperature >= mMeltingPoint} without the FluidStack plumbing. */
	public boolean meltsDown(long aTemperatureK) {
		return aTemperatureK >= mMeltingPoint;
	}

	/** The FL.temperature(FluidStack) counterpart over the FluidType temperature. */
	public static int fluidTemperature(@Nullable FluidStack aFluid) {
		return aFluid == null || aFluid.isEmpty() ? 0 : aFluid.getFluid().getFluidType().getTemperature();
	}

	/** Upstream FL.lava — same-fluid check that also covers the flowing state. */
	public static boolean isLava(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && aFluid.getFluid().isSame(Fluids.LAVA);
	}

	/**
	 * Upstream :220-231. The lava branch removes 1000 L and replaces the block with lava
	 * (:221-225), everything else goes up in fire (:226-227); the tank trash always runs
	 * (the GarbageGT.trash in-memory half). World effects are level-guarded so the
	 * offline unit tests exercise the judgment and the tank void alone.
	 */
	public boolean meltdown() {
		boolean tLava = isLava(mTank.getFluid()) && mTank.has(1000); // :221
		mTank.setEmpty(); // the GarbageGT.trash(mTank) half (:223/:226)
		if (hasLevel() && isServerSide()) {
			getLevel().setBlock(getBlockPos(), tLava ? Blocks.LAVA.defaultBlockState() : Blocks.FIRE.defaultBlockState(),
					Block.UPDATE_ALL); // :224 flowing lava / :227 setToFire
		}
		return true;
	}

	/** Content-change hook the capability wrapper calls after an executed fill/drain (the pipe onFilledFrom shape). */
	public void onTankChanged() {
		setChanged();
		updateClientData();
	}

	// ---------------------------------------------------------------------------
	// cover render sync (the oven :644-652 template — the covers ride the two sync
	// channels through saveAdditional/load; the client lands the refresh on arrival)
	// ---------------------------------------------------------------------------

	@Override
	public void onLoad() {
		super.onLoad();
		scheduleCoverRenderRefresh();
	}

	@Override
	public void handleUpdateTag(CompoundTag aTag) {
		super.handleUpdateTag(aTag);
		scheduleCoverRenderRefresh(); // chunk-data channel (login/chunk load)
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection aNet, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket aPacket) {
		super.onDataPacket(aNet, aPacket);
		scheduleCoverRenderRefresh(); // block-update channel (cover changes)
	}

	private void scheduleCoverRenderRefresh() {
		if (hasCovers() && hasLevel() && isClientSide()) GTRenderUpdates.scheduleRenderUpdate(this);
	}

	/**
	 * The C-grade render hook (IForgeBlockEntity.java:174): a covered barrel hands the
	 * render thread the immutable per-face sprite snapshot (the oven template); uncovered
	 * barrels keep {@code ModelData.EMPTY} and render through the plain blockstate model.
	 */
	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		CoverData tCovers = mCovers;
		if (tCovers == null) return super.getModelData();
		java.util.Map<Direction, net.minecraft.resources.ResourceLocation> tSprites = new java.util.EnumMap<>(Direction.class);
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tCovers.mBehaviours[tSide] == null) continue;
			net.minecraft.resources.ResourceLocation tSprite = tCovers.mBehaviours[tSide].getCoverTextureSurface(tSide, tCovers);
			if (tSprite != null) tSprites.put(Direction.from3DDataValue(tSide), tSprite);
		}
		if (tSprites.isEmpty()) return super.getModelData();
		return GTModelProperties.derive(super.getModelData())
				.with(GTModelProperties.RENDER_SNAPSHOT, new GTCoverRenderSnapshot(tSprites))
				.build();
	}

	// ---------------------------------------------------------------------------
	// capability (spec ① — fresh per-call side wrapper, GTFluidPipeBlockEntity.java:297-303 form)
	// ---------------------------------------------------------------------------

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// fresh per-call wrapper: the side is part of the handler identity
			return LazyOptional.of(() -> new BarrelFluidHandler(this, aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
}
