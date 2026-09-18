package gregtech6.tileentity.energy;

import gregapi.util.UT;
import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesEngineFuels;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Diesel Engine — task p12-engine-diesel, ported from
 * gregtech/tileentity/energy/generators/MultiTileEntityMotorLiquid.java (:62-257,
 * Loader_MultiTileEntities.java:721-729 eight material tiers 9145-9199) as the RU
 * DIRECT-CURRENT source of the fluid-engine chain: FM.Engine fluid fuel burned into
 * {@code +mRate} RU every tick, constant magnitude, constant sign (clockwise DC —
 * the third source semantics beside the crank's negative DC and the steam engine's
 * KU square wave, research tmp.research.p12-engine-family q2).
 *
 * <p><b>The tick (upstream onTick2 :107-149, server branch verbatim)</b>:
 * <ol>
 * <li>emit — {@code mEnergy >= mRate} pushes {@code (mRate, 1)} through the Util
 *     handshake and subtracts {@code mRate} UNCONDITIONALLY (:109-112 — the packet is
 *     spent even into an empty face, the "打空也扣能" generator family; the return of
 *     {@code emitEnergyToNetwork} is ignored upstream);</li>
 * <li>burn — while {@code mEnergy < mRate * 2 && !mStopped} (:113): find the fuel row
 *     against the INPUT tank only (fluid-only items-less lookup), probe-then-consume
 *     1 L per burn ({@code isRecipeInputEqual(T, F, ...)} — the SEARCH phase ran
 *     size-insensitive upstream :487/:501, the APPLY checks the amount), credit
 *     {@code units(absoluteTotalPower, 10000, mEfficiency, F)} (:121), and keep burning
 *     in the :123 while-loop until the energy head-room fills or the tank empties;</li>
 * <li>the fuel-swap clear (:128-131) — recipe FOUND but the apply refuses (a drained
 *     0 L tank keeps its fluid identity): only after 64 consecutive INACTIVE ticks
 *     ({@code mActivity.mData == 0}, the TE_Behavior_Active_Trinary 64 bit shift
 *     register :48-53) the input tank is cleared so the new fuel can be admitted —
 *     the exact "only if it was inactive for 64 ticks" comment of :129;</li>
 * <li>the invalid-fuel clear (:133-136) — NO recipe at all: the tank clears
 *     immediately ("not valid Fuel anymore for whatever reason");</li>
 * <li>exhaust (:140-145) — see the EXHAUST block below.</li>
 * </ol>
 *
 * <p><b>The fluid-only lookup seam (declared, the fuel-fluids card handoff)</b>:
 * upstream answers fluid-only lookups through the mRecipeFluidMap hash index when
 * {@code mMinimalInputItems == 0} (Recipe.java:519-523), but the port
 * {@code RecipeMap.findRecipe} hard-returns on empty item inputs (RecipeMap.java:137-138,
 * a Furnace-level simplification) and the shared RecipeMap base is FROZEN for this card
 * (FILES_SCOPE red line). The closure therefore lives HERE and covers exactly the
 * FM.Engine map: {@link #findFuelRecipe} re-expresses the upstream :487/:501 scan
 * ({@code isRecipeInputEqual(false, true, fluids, EMPTY)} over {@code mRecipeList}) for
 * this BE only — the shared-map semantics untouched (task spec ⑥: the P6 RM-shell gap
 * (containsInput item form / minTankSize / three hash indexes) stays open, this is the
 * minimal one-map closure).
 *
 * <p><b>EXHAUST — the CO2 mark consumption (declared deviation)</b>: every upstream
 * :77-120 fuel row emits {@code FL.CarbonDioxide.make(1)} per burnt litre, carried in the
 * port as the {@link GT6RecipesEngineFuels.FuelRow#co2()} DATA mark (gt6:carbon_dioxide
 * is not a registered fluid — the fuel-fluids card deviation; this card owns the live
 * exhaust surface). The burn credits one CO2 unit per burnt litre on a co2-marked row
 * ({@link #mExhaustCO2}), and the :140-145 tick consumes it with both arms live:
 * <ul>
 * <li>the push arm (:141 {@code FL.move(mTanks[1], getAdjacentTank(OPOS[mFacing]))}) —
 *     when a CO2 fluid IS registered ({@code gt6:carbon_dioxide}, live registry lookup,
 *     the upstream FL.exists semantics) the back-face tank neighbour is filled and the
 *     counter drops by the accepted amount; today the fluid is absent so the arm is
 *     DORMANT and the units are retained — the exact upstream behaviour of pushing into
 *     a tank that cannot take the fluid (FL.move leaves it in place);</li>
 * <li>the vent arm (:142-145) — a gas with NO collision beyond the back face vents
 *     straight out ({@code mTanks[1].setEmpty()} upstream = the counter zeroed here).</li>
 * </ul>
 * The RCON exhaust assertion runs at this fidelity: barrel at the back face = units
 * retained (solid neighbour, no vent), open back = units vented to zero.
 *
 * <p><b>The funnel/tap face (:203-213)</b>: upstream has NO GUI — supply goes through
 * Funnels (the containsInput-gated input-tank fill :203-207) and Taps (drain
 * output-else-input :209-213). The p12-tap-funnel-attachment card was IN FLIGHT at
 * dispatch (not in main), so the attachment wiring is NOT mounted here; the two
 * upstream methods are ported verbatim as {@link #funnelFill}/{@link #tapDrain} for the
 * attachment card to wire and the tests to drive, and the ACCEPTANCE channel is the
 * {@code /gt6engine fuel <pos> <fluid> <amount>} direct tank write (the declared
 * RCON counterpart of the funnel face, gated on the same containsInput).
 *
 * <p><b>Facing</b>: the crank form — the BlockState FACING is the command/placement
 * authority, {@link #mFacing} the runtime mirror re-synced at each server tick head;
 * the emit side is the front (upstream getDefaultSide SIDE_FRONT :184, the machine sits
 * on the facing side), the exhaust pushes out of the BACK (OPOS[mFacing]).
 *
 * <p><b>Cropped with declaration</b>: the client :147 minecart ambience (audio pool),
 * the ITileEntityRunningActively/ITileEntityAdjacentOnOff surfaces (:234-239 — the
 * redstone/cover on-off family rides its pool card; {@link #mStopped} stays a
 * field+public-toggle for the tests and that card), the tool clicks (:152-171 —
 * plunger/magnifyingglass, no tool item seam in this port; their readout rides
 * {@code /gt6engine stat}), and the sColoreds/sOverlaysActive texture family
 * (:242-254 — one shared block texture, no active visual, the crank render-pool
 * precedent).
 */
public class GTDieselEngineBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.java:1340/:1241/:1327/:1258). */
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_TANK = "gt.tank";
	public static final String NBT_EFFICIENCY = "gt.efficiency";
	/** The activity shift-register key (CS NBT_ACTIVE_DATA, the TE_Behavior save form). */
	public static final String NBT_ACTIVE = "gt.active";
	public static final String NBT_ACTIVE_DATA = "gt.active.data";

	/** The upstream :64 efficiency constant — every :721-729 row registers NBT_EFFICIENCY 10000. */
	public static final short DEFAULT_EFFICIENCY = 10000;

	// ---------------------------------------------------------------------------
	// the upstream :63-69 fields
	// ---------------------------------------------------------------------------

	/** Upstream :63 — the adjacent-on-off stop gate (the :113 burn gate). */
	public boolean mStopped = false;

	/** Upstream :64 — NBT_EFFICIENCY of every registration row, 10000. */
	public short mEfficiency = DEFAULT_EFFICIENCY;

	/** Upstream :65 — the stored energy and the per-tier packet rate (NBT_OUTPUT of the row). */
	public long mEnergy = 0, mRate = 32;

	/** Upstream :66 — fixed RU across all eight rows (:721-729 NBT_ENERGY_EMITTED). */
	public TagData mEnergyTypeEmitted = TD.Energy.RU;

	/** Upstream :67 — the fuel map, NBT_FUELMAP-swappable upstream; the offline tests inject a fixture. */
	@Nullable
	public RecipeMap mRecipesMap = null;

	/** Upstream :68 — the mLastRecipe fast path. */
	@Nullable
	public Recipe mLastRecipe = null;

	/**
	 * Upstream :69 — the input/output pair, capacity {@code mRate * 10} (:82-83). Port
	 * note: upstream a burnt-dry tank keeps its fluid identity at 0 L (the :118 consume
	 * shrinks the live stack; upstream isEmpty() is identity-based, FluidTankGT.java:314),
	 * the state the :130 fuel-swap clear exists for. The 1.20.1 Forge FluidStack
	 * normalizes 0-amount stacks to EMPTY (getFluid() answers EmptyFluid), so the identity
	 * is unrepresentable and the port burns dry into a TRUE empty tank — the declared
	 * deviation documented on {@link #findFuelRecipe} (a new fuel is admitted instantly;
	 * the :130 arm stays as the defensive verbatim).
	 */
	public final FluidTankGT[] mTanks = {new FluidTankGT(1000), new FluidTankGT(1000)};

	// the TE_Behavior_Active_Trinary fields (:70, inline — the trinary behavior class is not ported)
	/** The 64 bit per-tick activity history (TE_Behavior_Active_Trinary.mData; 0 = 64 inactive ticks). */
	public long mActivityData = 0;
	/** The trinary visual state (0 idle / 1 steady / 2 mixed; the :179-183 sync family). */
	public byte mActivityState = 0;
	/** The burn flag of the running tick (mActivity.mActive). */
	public boolean mActive = false;

	/**
	 * The exhaust counter in CO2 litres (the declared carrier of the upstream mTanks[1] CO2
	 * content — see the EXHAUST class doc block).
	 */
	public long mExhaustCO2 = 0;

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue). */
	public byte mFacing = 2; // NORTH

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTDieselEngineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point (the crank/axle form). The
	 * per-tier rate comes off the placed row (the 1.20.1 carrier of the upstream
	 * registration NBT NBT_OUTPUT), and BOTH tank capacities re-bind to {@code mRate * 10}
	 * (:82-83) exactly like readFromNBT2 does after loading the rate.
	 */
	public GTDieselEngineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.DIESEL_ENGINE_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof gregtech6.block.energy.GTDieselEngineBlock tEngine) {
			mRate = Math.max(1, tEngine.spec.output());
		}
		mTanks[0].setCapacity(mRate * 10); // :82
		mTanks[1].setCapacity(mRate * 10); // :83
	}

	@Override
	public String getTileEntityName() {
		return "diesel_engine"; // BET registry path mirrors it (GTBlockEntities.DIESEL_ENGINE_BE)
	}

	/** The fuel map accessor — the injected fixture upstream, the live FM.Engine otherwise. */
	public RecipeMap recipeMap() {
		return mRecipesMap != null ? mRecipesMap : GT6RecipeMaps.ENGINE_FUELS;
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :107-149 server branch, the emit/burn/exhaust order)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :108 server branch (the :147 client ambience is the audio pool)
		syncFacingFromState();
		// :109-112 — the DC emit: +mRate, one packet, spent unconditionally
		if (mEnergy >= mRate) {
			ITileEntityEnergy.Util.emitEnergyToNetwork(mEnergyTypeEmitted, mRate, 1, this, adjacency());
			mEnergy -= mRate;
		}
		// :113-137 — the burn loop
		if (mEnergy < mRate * 2 && !mStopped) {
			mActive = false; // :114
			Recipe tRecipe = findFuelRecipe(mLastRecipe, mTanks[0].getFluid());
			if (tRecipe != null) {
				if (tRecipe.mFluidOutputs.length <= 0 || mTanks[1].amount() + tRecipe.mFluidOutputs[0].getAmount() <= mTanks[1].capacity()) {
					// :118 — the apply: consume-checks the amounts the probe skipped
					if (consumeFuel(tRecipe)) {
						mActive = true; // :119
						mLastRecipe = tRecipe; // :120
						mEnergy += UT.Code.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :121
						if (co2Mark(fluidPath(tRecipe))) mExhaustCO2++; // the CO2 mark consumption (class doc; upstream mTanks[1].fill)
						// :123-127 — the while-loop burn-out
						while (mEnergy < mRate * 2
								&& (tRecipe.mFluidOutputs.length <= 0 || mTanks[1].amount() + tRecipe.mFluidOutputs[0].getAmount() <= mTanks[1].capacity())
								&& consumeFuel(tRecipe)) {
							mEnergy += UT.Code.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :124
							if (co2Mark(fluidPath(tRecipe))) mExhaustCO2++; // :125 the CO2 mark
							if (mTanks[0].isEmpty()) break; // :126
						}
					} else {
						// :128-131 — set remaining Fluid to null, in case the Fuel Type needs to be
						// swapped out. But only if it was inactive for 64 ticks (mData == 0).
						if (mActivityData == 0) mTanks[0].setEmpty();
					}
				}
			} else {
				// :133-136 — set remaining Fluid to null, because it is not valid Fuel anymore.
				mTanks[0].setEmpty();
			}
		}
		if (mEnergy < 0) mEnergy = 0; // :138

		// :140-145 — the exhaust tick (the CO2 counter form, class doc)
		if (mExhaustCO2 > 0) {
			pushExhaustToBackTank();
			if (mExhaustCO2 > 0 && gasVentsOutBack()) mExhaustCO2 = 0; // :142-145 the direct vent
		}
		setChanged();
	}

	/**
	 * The activity shift (TE_Behavior_Active_Trinary.check :48-53 verbatim) + the visual
	 * dirty gate (upstream onTickCheck :174 {@code mActivity.check(mStopped)}). Fires from
	 * the dispatcher's {@code mTimer > 2} sync window — the upstream timing verbatim (the
	 * short-circuit skip on sync ticks included).
	 */
	@Override
	public boolean onTickCheck(long aTimer) {
		byte oState = mActivityState; // :51
		mActivityData <<= 1; // :49
		if (mActive) mActivityData |= 1; // :50
		if (mActivityData == 0 || mStopped) mActivityState = 0; // :52
		else if (mActivityData == ~0L) mActivityState = 1;
		else mActivityState = 2;
		return oState != mActivityState || super.onTickCheck(aTimer); // :53 + the super chain
	}

	// ---------------------------------------------------------------------------
	// the fuel lookup (the FM.Engine fluid-only closure — class doc seam)
	// ---------------------------------------------------------------------------

	/** The port Recipe class keeps its EMPTY item form; carried for the upstream ZL_IS shape. */
	public static final net.minecraft.world.item.ItemStack[] ZL_IS = new net.minecraft.world.item.ItemStack[0];

	/**
	 * The upstream :115 findRecipe call re-expressed for the fluid-only map (class doc
	 * seam): the mLastRecipe fast path (:487) then the linear {@code mRecipeList} scan
	 * (:498-501 minus the hash indexes), both probing
	 * {@code isRecipeInputEqual(false, true, fluids, EMPTY)} — search never checks amounts,
	 * the apply does. The voltage gate rides {@code absGreaterEqual(aSize * mPower, mEUt)}
	 * with the engine's Long.MAX_VALUE aSize (always true upstream).
	 *
	 * <p>DECLARED DEVIATION (the burn-dry identity): upstream a burnt-dry tank keeps its
	 * fluid identity at 0 L (FluidTankGT.java:314 identity-based isEmpty; the :805 probe
	 * has no empty guard), so the same-fuel row still answers the probe and the :130 64t
	 * swap gate governs. On the 1.20.1 Forge surface a 0-amount FluidStack is normalized
	 * to EMPTY (updateEmpty — getFluid() answers EmptyFluid), the identity state is
	 * UNREPRESENTABLE, and a burnt-dry tank is observably empty: the lookup finds nothing
	 * and the :135 clear runs (a no-op on an empty tank), so a NEW fuel is admitted
	 * instantly where upstream waits for the identity to clear. The :128-131 arm stays
	 * verbatim below as the defensive port for any future row shape whose probe/apply
	 * can diverge.
	 */
	@Nullable
	public Recipe findFuelRecipe(@Nullable Recipe aLastRecipe, @Nullable FluidStack aTankFluid) {
		RecipeMap tMap = recipeMap();
		if (tMap == null) return null;
		FluidStack[] tFluids = aTankFluid != null && !aTankFluid.isEmpty() ? new FluidStack[] {aTankFluid} : new FluidStack[0];
		// the :487 fast path
		if (aLastRecipe != null && !aLastRecipe.mFakeRecipe && aLastRecipe.mCanBeBuffered
				&& aLastRecipe.isRecipeInputEqual(false, true, tFluids, ZL_IS)) {
			return aLastRecipe.mEnabled && absGreaterEqual(Long.MAX_VALUE, aLastRecipe.mEUt) ? aLastRecipe : null;
		}
		// the :498-501 scan
		for (Recipe tRecipe : tMap.mRecipeList) {
			if (tRecipe.mFakeRecipe || !tRecipe.isRecipeInputEqual(false, true, tFluids, ZL_IS)) continue;
			return tRecipe.mEnabled && absGreaterEqual(Long.MAX_VALUE, tRecipe.mEUt) ? tRecipe : null;
		}
		return null;
	}

	/** Upstream UT.Code.abs_greater_equal (UT.java:1727), the RecipeMap.java:154 form. */
	private static boolean absGreaterEqual(long aAmount1, long aAmount2) {
		return Math.abs(aAmount1) >= Math.abs(aAmount2);
	}

	/**
	 * The :118/:123 apply — the port mirror of
	 * {@code tRecipe.isRecipeInputEqual(T, F, mTanks[0].AS_ARRAY, ZL_IS)}. Upstream the
	 * consume SHRINKS the tank's live stack in place (the 1.7.10 IFluidTank semantics); the
	 * port's tank keeps a long ledger, so the exact consumed amount (the row's fluid inputs
	 * total, 1 L for every FM.Engine row) is removed through {@link FluidTankGT#remove},
	 * which at 0 L sets the tank truly empty (the declared burn-dry deviation, class doc on
	 * {@link #mTanks} and {@link #findFuelRecipe}).
	 */
	private boolean consumeFuel(Recipe aRecipe) {
		// the amount-checking gate WITHOUT the in-place shrink: the port Recipe's consume
		// phase shrinks the live tank stack to 0 L, which the Forge FluidStack normalizes to
		// EMPTY — and FluidTankGT.remove then refuses to drain an "empty" tank (the shrink
		// would never land). The (F, F) form checks the same amounts; the removal happens
		// through the tank ledger below, the exact litre upstream's shrink took.
		FluidStack[] tFluids = tankFluids();
		if (!aRecipe.isRecipeInputEqual(false, false, tFluids, ZL_IS)) return false;
		long tConsume = 0;
		for (FluidStack tInput : aRecipe.mFluidInputs) if (tInput != null && !tInput.isEmpty()) tConsume += tInput.getAmount();
		if (tConsume > 0) mTanks[0].remove(tConsume);
		return true;
	}

	/** The input tank as the {@code FluidStack[]} probe argument ({@code mTanks[0].AS_ARRAY} upstream, the port extract of findRecipeInternal :470-474). */
	private FluidStack[] tankFluids() {
		FluidStack tFluid = mTanks[0].getFluid();
		return tFluid != null && !tFluid.isEmpty() ? new FluidStack[] {tFluid} : new FluidStack[0];
	}

	/**
	 * The upstream containsInput(Fluid) (:189/:204 gate) for the FM.Engine map — "is this
	 * fluid a valid input for any recipe" (Recipe.java:420-426) at the linear-scan level:
	 * an amount-insensitive {@code isFluidEqual} against every row's fluid inputs. The
	 * minimal one-map closure (class doc): the shared-map method stays un-ported.
	 */
	public boolean containsFuelInput(@Nullable FluidStack aFluid) {
		RecipeMap tMap = recipeMap();
		if (tMap == null || aFluid == null || aFluid.isEmpty()) return false;
		for (Recipe tRecipe : tMap.mRecipeList) {
			for (FluidStack tInput : tRecipe.mFluidInputs)
				if (tInput != null && !tInput.isEmpty() && tInput.isFluidEqual(aFluid)) return true;
		}
		return false;
	}

	/** The registry path of the recipe's first fluid input, or null (the exhaust mark lookup key). */
	@Nullable
	public static String fluidPath(@Nullable Recipe aRecipe) {
		if (aRecipe == null || aRecipe.mFluidInputs.length <= 0) return null;
		Fluid tFluid = aRecipe.mFluidInputs[0].getFluid();
		ResourceLocation tKey = ForgeRegistries.FLUIDS.getKey(tFluid);
		return tKey == null ? null : tKey.getPath();
	}

	/**
	 * The CO2 mark of a fuel fluid, walked over the public {@link GT6RecipesEngineFuels#table()}
	 * rows by the gt6 id path (upstream FL.CarbonDioxide.make(1) on every :77-120 row). A
	 * path outside the table (any non-gt6 fluid, the offline fixture water) is unmarked —
	 * no exhaust. String-keyed on purpose: pure data offline, exact live.
	 */
	public static boolean co2Mark(@Nullable String aFluidPath) {
		if (aFluidPath == null) return false;
		for (GT6RecipesEngineFuels.FuelRow tRow : GT6RecipesEngineFuels.table())
			if (tRow.fluid().equals(aFluidPath)) return tRow.co2();
		return false;
	}

	// ---------------------------------------------------------------------------
	// the exhaust arms (upstream :140-145, the CO2 counter form)
	// ---------------------------------------------------------------------------

	/**
	 * The push arm (:141 {@code FL.move} into the back-face adjacent tank): with a
	 * registered {@code gt6:carbon_dioxide} fluid the back-face FLUID_HANDLER neighbour is
	 * filled and the counter drops by the accepted amount; without the fluid (today) the
	 * units are retained — the upstream behaviour of a tank that cannot take the fluid.
	 */
	private void pushExhaustToBackTank() {
		if (!hasLevel() || getLevel().isClientSide()) return;
		Fluid tCO2 = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("gt6", "carbon_dioxide"));
		if (tCO2 == null) return; // the FL.exists semantics: no registered fluid, nothing can leave as a stack
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(back()));
		if (tNeighbor == null || tNeighbor.isRemoved()) return;
		//? if forge {
		IFluidHandler tHandler = tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, back().getOpposite()).orElse(null);
		//?} else {
		/*IFluidHandler tHandler = getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tNeighbor.getBlockPos(), back().getOpposite()); // 21.1: the level-form BlockCapability query (the coke-oven/barrel precedent)
		*///?}
		if (tHandler == null) return;
		long tMoved = tHandler.fill(new FluidStack(tCO2, (int)Math.min(Integer.MAX_VALUE, mExhaustCO2)), IFluidHandler.FluidAction.EXECUTE);
		mExhaustCO2 -= tMoved;
	}

	/**
	 * The vent arm (:142 {@code !WD.hasCollide(worldObj, getOffset(OPOS[mFacing], 1))}) —
	 * the block one PAST the back face has no collision = open air = the gas vents
	 * directly. {@code mBackCollideProbe} is the offline test seam (null = the live probe).
	 */
	private boolean gasVentsOutBack() {
		if (mBackCollideProbe != null) return !mBackCollideProbe.getAsBoolean(); // the probe answers the COLLIDE state (:142 !WD.hasCollide)
		if (!hasLevel()) return false;
		BlockPos tBeyond = getBlockPos().relative(back()).relative(back());
		return getLevel().getBlockState(tBeyond).getCollisionShape(getLevel(), tBeyond).isEmpty();
	}

	/** The offline seam for {@link #gasVentsOutBack()} (the adjacency-override pattern). */
	@Nullable
	private java.util.function.BooleanSupplier mBackCollideProbe = null;

	void setBackCollideProbe(@Nullable java.util.function.BooleanSupplier aProbe) {
		mBackCollideProbe = aProbe;
	}

	/** The back face: OPOS[mFacing] (CS.java:24 OPOS, byte side == Direction.get3DDataValue). */
	public Direction back() {
		return Direction.from3DDataValue(mFacing).getOpposite();
	}

	// ---------------------------------------------------------------------------
	// the funnel/tap face (upstream :203-213 verbatim — the attachment card wires these)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream funnelFill :203-207 — the containsInput-gated input-tank fill (the Funnel
	 * supply face; the {@code updateInventory()} of :205 is a no-op here, no inventory).
	 * The {@code /gt6engine fuel} command and the future tap-funnel attachment both land on
	 * this method.
	 */
	public int funnelFill(@Nullable FluidStack aFluid, boolean aDoFill) {
		if (aFluid == null || aFluid.isEmpty() || !containsFuelInput(aFluid)) return 0; // :204 the gate
		return mTanks[0].fill(aFluid, aDoFill ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
				: net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE); // :206
	}

	/** Upstream tapDrain :209-213 — output tank first, else the input tank (the Tap face). */
	@Nullable
	public FluidStack tapDrain(int aMaxDrain, boolean aDoDrain) {
		return mTanks[mTanks[1].has() ? 1 : 0].drain(aMaxDrain, aDoDrain
				? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
				: net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE); // :212
	}

	// ---------------------------------------------------------------------------
	// the on-off pair (upstream :238 verbatim; the adjacency/redstone surfaces are the pool)
	// ---------------------------------------------------------------------------

	/** Upstream setStateOnOff :238 — returns !mStopped (true = running). */
	public boolean setStateOnOff(boolean aOnOff) {
		mStopped = !aOnOff;
		return !mStopped;
	}

	// ---------------------------------------------------------------------------
	// the energy face family (upstream :226-232 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == mEnergyTypeEmitted; // upstream :226
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aSide == mFacing && isEnergyType(aEnergyType, aSide, true); // upstream :227 (the super call unfolded, the crank face form)
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		return Math.min(mRate, mEnergy); // upstream :228
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mRate;} // upstream :229

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return mRate;} // upstream :230

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return mRate;} // upstream :231

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeEmitted.AS_LIST; // upstream :232
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the crank/axle D1 form) + the facing mirror
	// ---------------------------------------------------------------------------

	/** The offline test seam (the crank mAdjacencyOverride form). */
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	private IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	/**
	 * The /setblock RCON path: the state carries the facing, the BE mirror re-syncs at each
	 * tick head (the crank syncFacingFromState form, keyed on the PROPERTY).
	 */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			mFacing = (byte)getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
		}
	}

	public byte getFacing() {
		return mFacing;
	}

	/**
	 * The chest/oven placement mirror (the crank setFacingFromPlacement form). Task
	 * p28-singleblock-facing-canon: the emit/front side TOWARDS the placer — the
	 * GT6PlacementFacing canon (view OPPOSITE).
	 */
	public void setFacingFromPlacement(net.minecraft.world.entity.player.Player aPlayer) {
		mFacing = gregtech6.block.GT6PlacementFacing.placementFacing(aPlayer.getDirection());
		setChanged();
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :73-84 / writeToNBT2 :87-94)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :89
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :90
		aNBT.putInt(NBT_ACTIVE, mActive ? 1 : 0);
		aNBT.putLong(NBT_ACTIVE_DATA, mActivityData);
		aNBT.putShort(NBT_EFFICIENCY, mEfficiency);
		aNBT.putLong(NBT_OUTPUT, mRate); // :78 the row rate rides NBT for a clean round trip
		mTanks[0].writeToNBT(aNBT, NBT_TANK + ".0"); // :92
		mTanks[1].writeToNBT(aNBT, NBT_TANK + ".1"); // :93
		aNBT.putLong("gt.exhaust", mExhaustCO2);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :75
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :77
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE);
		if (aNBT.contains(NBT_ACTIVE_DATA, Tag.TAG_ANY_NUMERIC)) mActivityData = aNBT.getLong(NBT_ACTIVE_DATA);
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) mRate = Math.max(1, aNBT.getLong(NBT_OUTPUT)); // :78
		if (aNBT.contains(NBT_EFFICIENCY, Tag.TAG_ANY_NUMERIC)) {
			mEfficiency = (short)Math.max(0, Math.min(10000, aNBT.getShort(NBT_EFFICIENCY))); // :80 UT.Code.bind_(0, 10000, ...)
		}
		mTanks[0].readFromNBT(aNBT, NBT_TANK + ".0").setCapacity(mRate * 10); // :82 the capacity re-bind after the rate
		mTanks[1].readFromNBT(aNBT, NBT_TANK + ".1").setCapacity(mRate * 10); // :83
		if (aNBT.contains("gt.exhaust", Tag.TAG_ANY_NUMERIC)) mExhaustCO2 = aNBT.getLong("gt.exhaust");
	}
}
