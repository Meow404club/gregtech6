package gregtech6.tileentity.energy.converters;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Boilers;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.attachment.GTFunnelBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 single-block Steam Boiler Tank — task p13-boiler-tank,
 * ported from gregtech/tileentity/energy/converters/MultiTileEntityBoilerTank.java (:65-283)
 * as the HU consumer closing the P13 steam loop: water + HU in the bottom/sides, steam out
 * the top, a pressure gauge on the front.
 *
 * <p><b>The tanks (:66-70)</b>: water 4000 L fixed + the steam tank whose capacity is
 * re-derived as {@code mOutput * 10000} at every row load (:78) — the tank never persists a
 * stale capacity, the NBT_CAPACITY(_SU) override keys (:79-80) are pool (no writer in this
 * port). mOutput is the raw NBT_OUTPUT_SU row value (16..2048 SU/t — the Loader rows
 * already carry {@code N * STEAM_PER_EU}).
 *
 * <p><b>The conversion (:116-126, verbatim)</b>: every server tick
 * {@code tConversions = min(steamTank.capacity / 2560, min(mEnergy / EU_PER_WATER, water))};
 * each whole conversion drinks 1 L of water + {@link GTFluids#EU_PER_WATER 80} HU and makes
 * {@code units(tConversions, 10000, mEfficiency * STEAM_PER_WATER_GLOBAL, F)} litres of steam
 * ({@link GTFluids#STEAM_PER_WATER_GLOBAL 160} L per litre at full efficiency — the GLOBAL
 * constant, CS.java:242, never the engine-private 200). Scale ({@code rng(10) == 0},
 * efficiency above the 5000 floor, water present, NOT distilled water): the efficiency drops
 * by tConversions, floored at 5000 (:119-122) — distilled water is immune, the
 * REQUIREMENT_WATER_PURE tooltip (:102).
 *
 * <p><b>The cooldown (:129-137)</b>: while no conversion refreshed
 * {@code mCoolDownResetTimer} (set to 128 per conversion :125, to max(,32) per injection
 * :252) for 128 ticks, the boiler bleeds {@code (mOutput * 64) / STEAM_PER_EU} HU and trashes
 * {@code mOutput * 64} L of steam per tick — the GarbageGT.trash(:132) direct disposal (no
 * world emission), ported as the tank removal; the timer re-arms at 128 once the store
 * empties (:133-136).
 *
 * <p><b>The steam output (:139-142)</b>: only above HALF tank, only into the TOP-side
 * adjacent fluid handler (upstream getAdjacentTank(SIDE_UP); the port consults the
 * neighbour's capability through the face pointing back at the boiler — the pipe connection
 * face), at {@code min(tAmount > capacity/4 ? mOutput * 2 : mOutput, tAmount)} — the 3/4
 * double-rate gate.
 *
 * <p><b>The barometer (:145, :219-233)</b>: the 5-bit visual {@code scale(amount, capacity,
 * 31, F)}, synced through the vanilla two-channel sync (the onTickCheck oBarometer compare
 * flags the update; NBT_VISUAL rides getUpdateTag and the client copy re-loads it — the
 * port carrier of the upstream setVisualData byte).
 *
 * <p><b>The explosion family (HIGH risk, all four triggers verbatim)</b>:
 * <ol>
 * <li>overheated / steam tank FULL → explode(F) — deferred, the own-tick consumption
 *     (:148-150, the 01Root buffered form);</li>
 * <li>dismantled while pressurised (barometer &gt; 4, non-creative) → explode(T) — instant
 *     (:202-205; the port carries it on the block's playerWillDestroy — the removedByPlayer
 *     counterpart face, NOT onRemove — plus the /gt6boiler dismantle acceptance door);</li>
 * <li>caught in a neighbour's explosion with barometer &gt; 4 → explode(T) (:208-211, the
 *     block's onBlockExploded face — the BE is consulted BEFORE the air swap removes it);</li>
 * <li>decalcified with the chisel above 15/31 pressure → explode(F) (:168-169).</li>
 * </ol>
 * The strength is the upstream :215 formula
 * {@code max(1, sqrt(steam litres) / 100)} (the override of the Root strength-4 default);
 * the execution is the Root's declared vanilla bridge — destroyBlock +
 * {@code Level.explode(null, x, y, z, strength, ExplosionInteraction.BLOCK)}
 * (TileEntityBase01Root.java:284-292, the p8-d3 mini-census: upstream ExplosionGT destroy-
 * terrain + drop mapping onto the vanilla BLOCK interaction, the drop-detail pooling is the
 * card's declared O2 deviation). NO dry-burn explosion: an empty water tank only stops the
 * conversion — the GTCEu SteamBoilerMachine.java:188-99 semantics are NOT introduced (the
 * negative arm of the acceptance).
 *
 * <p><b>The energy face (:249-260)</b>: HU ONLY, accepting from EVERY side, never emitting —
 * {@code isEnergyType = !emitting && type == HU} (the :250 accepting delegation rides the mdk
 * Root default, TileEntityBase01Root.java:338). The injection door {@code doInject} (:252,
 * the Root :323 extension hook) accumulates {@code abs(amount * size)} and re-arms the
 * cooldown timer to max(, 32) — NO world writes here (the Root interface javadoc discipline;
 * the overheat reaction is the tick's buffered explode). demand/recommended = mOutput/2
 * (:253-254), min 1 / max Long.MAX_VALUE (:255-256 — the Root defaults Rec/2 and Rec*2 would
 * both differ). The stored/capacity reporting (:257-258) rides the /gt6boiler stat +
 * thermometer surface — the capacitor interface half is the cut ADR-D1 subsystem with no
 * port surface (declared).
 *
 * <p><b>The fluid face (:262-264)</b>: the capability door exists on every side BUT the top
 * (SIDES_BOTTOM_HORIZONTAL, the :262 half), fill = water only into the water tank, drain =
 * the :263 null (steam never leaves through the sides — only the top push), both tanks
 * exposed (:264). The funnel face (:236-238, the FunnelAccessible contract): water only,
 * any side. Non-water injection is a REFUSAL, not destruction (the bucket semantics are the
 * barrel's, not the boiler's).
 *
 * <p><b>Row configuration</b> rides the block carrier ({@link GT6Boilers.BoilerTankBlock#row()},
 * the SteamEngineBlock form; the BE ctor reads it so every placement path mounts the row) —
 * upstream wrote NBT_OUTPUT_SU at registration and read it back at :77. Foreign blocks (the
 * offline fixtures) keep the class defaults. NO GUI by census (:103 NO_GUI_FUNNEL_TO_TANK);
 * the contact heat damage (:242), the sub-cube collision box (:243) and the Gibbl display
 * (:266-267) ride the entity/tool pools (the W2 burning-box declarations).
 */
public class GTBoilerTankBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy, GTFunnelBlockEntity.FunnelAccessible {

	/** The NBT keys (the generators' "gt." spelling convention; visual = the :219-233 barometer byte). */
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_VISUAL = "gt.visual";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_EFFICIENCY = "gt.efficiency";
	public static final String NBT_TANK0 = "gt.tank0";
	public static final String NBT_TANK1 = "gt.tank1";

	/** The vanilla UP ordinal — the steam output face (the SIDES_UP slot). */
	public static final byte SIDE_UP = 1;

	/** The barometer (:66, the synced 5-bit pressure gauge). */
	public byte mBarometer = 0;
	/** The o-pair of the :219-228 change check. */
	private byte oBarometer = 0;
	/** The scale/calcification state (:66, ten-thousandths, the 5000 floor). */
	public short mEfficiency = 10000;
	/** The cooldown timer (:66; 128 set per conversion :125, max(,32) per injection :252). */
	public short mCoolDownResetTimer = 128;
	/** The stored heat (:68, NBT_ENERGY :75). */
	public long mEnergy = 0;
	/** The heat ceiling (:68 — re-derived with the row, :78). */
	public long mCapacity = 640000;
	/** The nominal steam output (:68; the raw NBT_OUTPUT_SU row value, :77). */
	public long mOutput = 64;
	/** The accepted energy type (:69 — every Loader boiler row is HU). */
	public TagData mEnergyTypeAccepted = TD.Energy.HU;
	/** The two tanks (:70): [0] water 4000 L, [1] steam capacity mOutput * 10000 (:78). */
	public final FluidTankGT[] mTanks = new FluidTankGT[] {new FluidTankGT(4000), new FluidTankGT(640000)};

	/** The BE runtime facing mirror (the BlockState FACING is the authority; the FRONT = the barometer face, :246-247). */
	public byte mFacing = 2; // NORTH

	// ---------------------------------------------------------------------------
	// the offline fluid identity seams (live paths never see these; the engine
	// mSteamMatch/setFluidSeams pattern — the gt6 fluids do not exist offline)
	// ---------------------------------------------------------------------------

	/** The steam identity (the conversion product identity, upstream FL.Steam.make, :123). */
	public Function<Long, FluidStack> mSteamMake = aAmount -> new FluidStack(GTFluids.STEAM.source.get(), (int)Math.min(Integer.MAX_VALUE, Math.max(1, aAmount.longValue())));

	/** The distilled-water identity (the :119 scale immunity, upstream FL.distw, FL.java:413). */
	public Function<Fluid, Boolean> mDistwMatch = f -> f == GTFluids.DISTILLED_WATER.source.get();

	/** The water identity (upstream FL.water, FL.java:403 — vanilla water, offline-safe). */
	public Function<Fluid, Boolean> mWaterMatch = f -> f == Fluids.WATER;

	/** The live rng (:119 — the world Random; the generator rng seam pattern). */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	@Nullable
	private IntSupplier mRngOverride = null;

	/** The offline rng seam. */
	public void setRngOverride(@Nullable IntSupplier aRng) {
		mRngOverride = aRng;
	}

	/** The offline fluid seam setter (the engine setFluidSeams form). */
	public void setFluidSeams(@Nullable Function<Long, FluidStack> aSteamMake, @Nullable Function<Fluid, Boolean> aDistwMatch, @Nullable Function<Fluid, Boolean> aWaterMatch) {
		if (aSteamMake != null) mSteamMake = aSteamMake;
		if (aDistwMatch != null) mDistwMatch = aDistwMatch;
		if (aWaterMatch != null) mWaterMatch = aWaterMatch;
	}

	// ---------------------------------------------------------------------------
	// construction (the engine/BET-factory shape)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTBoilerTankBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime. A placed family block injects its registration row
	 * (the :77-78 NBT_OUTPUT_SU load + capacity re-derivation); foreign blocks keep the
	 * defaults.
	 */
	public GTBoilerTankBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.BOILER_TANK_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6Boilers.BoilerTankBlock tBlock) {
			setOutput(tBlock.row().outputSteamPerTick()); // :77-78
		}
	}

	@Override
	public String getTileEntityName() {
		return "boiler_tank"; // BET registry path mirrors it (GTBlockEntities.BOILER_TANK_BE)
	}

	/** The :78 tank formula — 10000 L of steam tank per SU/t of nominal output. */
	public long steamTankCapacity() {
		return mOutput * 10000;
	}

	/** The output setter (row load + the offline tests) — re-derives the steam tank capacity, the :78 pairing. */
	public void setOutput(long aOutput) {
		mOutput = Math.max(1, aOutput);
		mTanks[1].setCapacity(steamTankCapacity());
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :112-151, the server branch verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // :114

		syncFacingFromState(); // the crank/engine form: the /setblock RCON path re-syncs the mirror

		// Convert Water to Steam — :116-126 verbatim
		long tConversions = Math.min(mTanks[1].capacity() / 2560, Math.min(mEnergy / GTFluids.EU_PER_WATER, mTanks[0].amount()));
		if (tConversions > 0) {
			mTanks[0].remove(tConversions); // :118
			if (rng(10) == 0 && mEfficiency > 5000 && mTanks[0].has() && !mDistwMatch.apply(mTanks[0].getFluid().getFluid())) { // :119
				mEfficiency -= tConversions; // :120
				if (mEfficiency < 5000) mEfficiency = 5000; // :121
			}
			// :123 — the steam goes IN; the add clamps at capacity (upstream setFluid could
			// overshoot), the isFull() explosion verdict below is the same either way
			mTanks[1].add(units(tConversions, 10000, (long)mEfficiency * GTFluids.STEAM_PER_WATER_GLOBAL, false), mSteamMake.apply(tConversions));
			mEnergy -= tConversions * GTFluids.EU_PER_WATER; // :124
			mCoolDownResetTimer = 128; // :125
		}

		// Remove Steam and Heat during the process of cooling down — :129-137
		if (mCoolDownResetTimer-- <= 0) {
			mCoolDownResetTimer = 0; // :130
			mEnergy -= (mOutput * 64) / GTFluids.STEAM_PER_EU; // :131
			mTanks[1].remove(mOutput * 64); // :132 — the GarbageGT.trash direct disposal (no world emission)
			if (mEnergy <= 0) { // :133
				mEnergy = 0; // :134
				mCoolDownResetTimer = 128; // :135
			}
		}

		long tAmount = mTanks[1].amount() - mTanks[1].capacity() / 2; // :139

		// Emit Steam — :141-142: only above HALF, only the TOP-side neighbour, the 3/4 double rate
		if (tAmount > 0) {
			long tRate = tAmount > mTanks[1].capacity() / 4 ? mOutput * 2 : mOutput;
			moveSteamToTop(Math.min(tRate, tAmount));
		}

		// Set Barometer — :145 (the synced 5-bit visual)
		mBarometer = (byte)scale(mTanks[1].amount(), mTanks[1].capacity(), 31, false);

		// Well the Boiler gets structural Damage when being too hot, or when being too full of Steam — :148-150
		if (mEnergy > mCapacity || mTanks[1].isFull()) {
			explode(false);
		}
	}

	/**
	 * The :142 {@code FL.move(mTanks[1], getAdjacentTank(SIDE_UP), aAmount)} port — up to
	 * {@code aAmount} litres into the top neighbour's fluid handler (consulted through the
	 * face pointing back at THIS boiler — the pipe connection face, the tank-fill-from-any-
	 * -face convention composes), as much as the neighbour accepts; what it refuses stays.
	 */
	private void moveSteamToTop(long aAmount) {
		IFluidHandler tTarget = mFluidAdjacency.apply(SIDE_UP);
		if (tTarget == null || aAmount <= 0) return;
		FluidStack tOffer = mTanks[1].get(FluidTankGT.bindInt(aAmount));
		if (tOffer == null || tOffer.isEmpty()) return;
		int tAccepted = tTarget.fill(tOffer, FluidAction.SIMULATE);
		if (tAccepted <= 0) return;
		int tMoved = tTarget.fill(new FluidStack(tOffer.getFluid(), tAccepted), FluidAction.EXECUTE);
		if (tMoved > 0) mTanks[1].remove(tMoved);
	}

	// ---------------------------------------------------------------------------
	// the pure conversion math (the engine units/scale statics re-called — zero-diff
	// reuse of the existing port copies; bind5 is the :220 local)
	// ---------------------------------------------------------------------------

	/** UT.Code.bind5 (UT.java:1544) — the 5-bit visual bind of :220. */
	public static byte bind5(byte aValue) {
		return (byte)Math.max(0, Math.min(31, aValue));
	}

	/** The statics re-call: UT.Code.units (the engine copy, UT.java:1677-1683 verbatim). */
	public static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		return GTSteamEngineBlockEntity.units(aAmount, aOriginalUnit, aTargetUnit, aRoundUp);
	}

	/** The statics re-call: UT.Code.scale (the engine copy, UT.java:1534-1537 verbatim). */
	public static long scale(long aValue, long aMax, long aScale, boolean aInvert) {
		return GTSteamEngineBlockEntity.scale(aValue, aMax, aScale, aInvert);
	}

	// ---------------------------------------------------------------------------
	// the explosion family (the four triggers; the execution is the Root bridge)
	// ---------------------------------------------------------------------------

	/** Upstream :213-216 — the strength is the steam volume mapped to vanilla power. */
	@Override
	public void explode(boolean aInstant) {
		explode(aInstant, Math.max(1, Math.sqrt(mTanks[1].amount()) / 100.0));
	}

	/**
	 * Upstream :208-211 — the caught-in-an-explosion second blast (barometer &gt; 4, server
	 * side, instant). Called from the block's onBlockExploded BEFORE the air swap removes
	 * this BE (the root explode(T) destroys the block itself).
	 */
	public void onExploded() {
		if (isServerSide() && mBarometer > 4) explode(true);
	}

	/**
	 * Upstream :202-205 — the pressurised-dismantle arm (the removedByPlayer counterpart).
	 * {@code aPlayer} null = the RCON acceptance channel (the non-creative counterfactual).
	 * Returns whether the boiler exploded; the caller removes the block afterwards either
	 * way (the :204 setBlockToAir).
	 */
	public boolean dismantle(@Nullable Player aPlayer) {
		boolean tCreative = aPlayer != null && aPlayer.isCreative(); // :203 UT.Entities.isCreative
		if (mBarometer > 4 && isServerSide() && !tCreative) {
			explode(true); // :203 — the T instant form
			return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the tool faces (upstream onToolClick2 :161-196 — the RCON command arms call these)
	// ---------------------------------------------------------------------------

	/**
	 * The TOOL_plunger arm (:161-164) — the water tank first, else the steam tank; empties
	 * it and returns the trashed litres (the GarbageGT.trash return).
	 */
	public long plunger() {
		if (mTanks[0].has()) { // :162 — the water tank first
			long tTrashed = mTanks[0].amount();
			mTanks[0].setEmpty();
			return tTrashed;
		}
		long tTrashed = mTanks[1].amount(); // :163 — else the steam tank
		mTanks[1].setEmpty();
		return tTrashed;
	}

	/**
	 * The TOOL_chisel arm (:165-178). Returns the repair value (0 = nothing to do / the
	 * explosion branch). Above 15/31 pressure the chisel DETONATES the boiler (:168-169,
	 * deferred explode(F)); otherwise the tank vents, the scale and the heat reset (:172-174)
	 * and the heat damage lands on {@code aPlayer} (the :171 formula verbatim; null player =
	 * the RCON channel, the damage arm skips — the UT.Entities pool).
	 */
	public int chisel(@Nullable Player aPlayer) {
		int rResult = 10000 - mEfficiency; // :166
		if (rResult > 0) { // :167
			if (mBarometer > 15) { // :168
				explode(false); // :169
			} else {
				if (mEnergy + mTanks[1].amount() / GTFluids.STEAM_PER_EU > 2000 && aPlayer != null) { // :171
					aPlayer.hurt(aPlayer.damageSources().onFire(), (mEnergy + mTanks[1].amount() / 2) / 2000.0F);
				}
				mTanks[1].setEmpty(); // :172
				mEfficiency = 10000; // :173
				mEnergy = 0; // :174
				return rResult; // :175
			}
		}
		return 0; // :178
	}

	/** The TOOL_thermometer arm (:181-184) — the stored-heat readout line. */
	public String thermometer() {
		return "Stored Heat Units: " + mEnergy + " / " + mCapacity + " HU"; // :182
	}

	/** The TOOL_magnifyingglass arm (:186-196) — the calcification + water-warning lines. */
	public List<String> magnifyingglass() {
		if (mEfficiency < 10000) { // :188
			return List.of("Calcification: " + (10000 - mEfficiency) / 100 + "%", // :189 (LH.percent)
					mTanks[0].has() ? "Water: " + mTanks[0].amount() + "/" + mTanks[0].capacity() + " L" : "WARNING: NO WATER!!!"); // :193
		}
		return List.of("No Calcification in this Boiler", // :191
				mTanks[0].has() ? "Water: " + mTanks[0].amount() + "/" + mTanks[0].capacity() + " L" : "WARNING: NO WATER!!!"); // :193
	}

	// ---------------------------------------------------------------------------
	// the funnel face (:236-238 — the FunnelAccessible contract, water only)
	// ---------------------------------------------------------------------------

	@Override
	public int funnelFill(byte aSide, FluidStack aFluid, boolean aDoFill) {
		if (aFluid == null || aFluid.isEmpty()) return 0;
		return mWaterMatch.apply(aFluid.getFluid()) ? mTanks[0].fill(aFluid, aDoFill ? FluidAction.EXECUTE : FluidAction.SIMULATE) : 0; // :237
	}

	// ---------------------------------------------------------------------------
	// the energy face family (:249-260 verbatim; accepting rides the Root default
	// :250 delegation — TileEntityBase01Root.java:338 = isEnergyType(F) && attachable)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :249 — HU only, never emitting
	}

	/**
	 * Upstream :252 — the injection door (the Root :323 extension hook the synchronized
	 * :355 gate calls). Accumulation ONLY: no world writes, no explosion — the overheat
	 * reaction is the tick's buffered explode (the Root interface javadoc discipline).
	 */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aDoInject) { // :252
			mEnergy += Math.abs(aAmount * aSize);
			mCoolDownResetTimer = (short)Math.max(mCoolDownResetTimer, 32);
		}
		return aAmount;
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return mOutput / 2; // :253
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mOutput / 2; // :254
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 1; // :255 — the Root default Rec/2 would differ
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :256 — the Root default Rec*2 would differ
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :259
	}

	// the :257-258 stored/capacity reporting rides the stat/thermometer surface — the
	// capacitor interface half is the cut ADR-D1 subsystem, no port surface (class doc).

	// ---------------------------------------------------------------------------
	// the fluid capability door (:262-264 — every side BUT the top, water-only fill,
	// no drain, both tanks exposed)
	// ---------------------------------------------------------------------------

	private final IFluidHandler mFluidHandler = new BoilerFluidHandler();

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// the :262 SIDES_BOTTOM_HORIZONTAL face half lives HERE — the top face exposes
			// no door at all (the steam leaves through the internal top push, never through
			// a capability); the side-less query stays open (the BarrelFluidHandler convention)
			if (aSide != null && aSide.get3DDataValue() == SIDE_UP) {
				return LazyOptional.empty();
			}
			return LazyOptional.of(() -> mFluidHandler).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	/**
	 * The door behind the capability: fill = the :262 fluid half (water only, into the water
	 * tank — the face half lives in {@link #getCapability}), drain = the :263 null (steam
	 * never leaves through the sides), the tank view exposes both (:264). A non-water fluid
	 * is REFUSED, not destroyed — the boiler has no fizz semantics.
	 */
	private class BoilerFluidHandler implements IFluidHandler {
		@Override public int getTanks() {return mTanks.length;} // :264
		@Override public FluidStack getFluidInTank(int aTank) {
			FluidStack tStack = (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].get() : null;
			return tStack == null ? FluidStack.EMPTY : tStack;
		}
		@Override public int getTankCapacity(int aTank) {return (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aTank == 0 && aStack != null && !aStack.isEmpty() && mWaterMatch.apply(aStack.getFluid());
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (!mWaterMatch.apply(aResource.getFluid())) return 0; // :262 FL.water half
			return mTanks[0].fill(aResource, aAction);
		}

		@Override public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;} // :263
		@Override public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;} // :263
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the engine form) + the facing mirror
	// ---------------------------------------------------------------------------

	/**
	 * The per-tick top-neighbour resolver (:142 getAdjacentTank(SIDE_UP)): the block above,
	 * consulted through its BOTTOM face — the face pointing back at THIS boiler (the pipe
	 * connection face; tanks accept fills from any face, the P5 ruling). Offline-replaceable
	 * via {@link #setFluidAdjacencyOverride} (the engine mFluidAdjacency seam shape).
	 */
	private java.util.function.Function<Byte, IFluidHandler> mFluidAdjacency = aSide -> {
		if (aSide == null || aSide != SIDE_UP || !hasLevel()) return null;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().above());
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).orElse(null);
	};

	/** The offline seam setter for the top-neighbour fluid handler (the test fixture form). */
	public void setFluidAdjacencyOverride(@Nullable java.util.function.Function<Byte, IFluidHandler> aAdjacency) {
		mFluidAdjacency = aAdjacency;
	}

	/** The placement mirror (the engine setFacingFromPlacement form — the FRONT = barometer face, :246). */
	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = (byte)aPlayer.getDirection().get3DDataValue();
		setChanged();
	}

	/** The /setblock RCON path re-sync (the engine form, keyed on the PROPERTY instance). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6Boilers.BoilerTankBlock.FACING)) {
			mFacing = (byte)getBlockState().getValue(GT6Boilers.BoilerTankBlock.FACING).get3DDataValue();
		}
	}

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// the sync checks (:219-233 — the barometer byte is the sync payload)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onTickCheck(long aTimer) {
		mBarometer = bind5(mBarometer); // :220
		return mBarometer != oBarometer || super.onTickCheck(aTimer); // :221
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oBarometer = mBarometer; // :227
	}

	// the :231-233 setVisualData byte: the vanilla two-channel sync carries NBT_VISUAL in
	// the update tag — the client copy re-derives mBarometer in load() (the port carrier).

	// ---------------------------------------------------------------------------
	// NBT (the upstream :73-92 set; the row config re-derives from the block carrier)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :89
		if (mEfficiency != 10000) aNBT.putShort(NBT_EFFICIENCY, mEfficiency); // :90
		aNBT.putLong(NBT_OUTPUT, mOutput); // the row value rides NBT for a clean round trip (:77)
		aNBT.putByte(NBT_VISUAL, mBarometer); // the :76 visual (the synced gauge byte)
		mTanks[0].writeToNBT(aNBT, NBT_TANK0); // :83
		mTanks[1].writeToNBT(aNBT, NBT_TANK1); // :83
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :75
		if (aNBT.contains(NBT_VISUAL, Tag.TAG_ANY_NUMERIC)) mBarometer = bind5(aNBT.getByte(NBT_VISUAL)); // :76 + :232 mask
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) setOutput(aNBT.getLong(NBT_OUTPUT)); // :77-78
		if (aNBT.contains(NBT_EFFICIENCY, Tag.TAG_ANY_NUMERIC)) {
			mEfficiency = (short)Math.max(0, Math.min(10000, aNBT.getShort(NBT_EFFICIENCY))); // :81 UT.Code.bind_
		}
		mTanks[0].readFromNBT(aNBT, NBT_TANK0); // :83
		mTanks[1].readFromNBT(aNBT, NBT_TANK1); // :83
		mTanks[1].setCapacity(steamTankCapacity()); // :78 half two
	}
}
