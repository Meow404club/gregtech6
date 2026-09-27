package gregtech6.tileentity.energy.reactors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}

import gregtech6.fluid.FluidTankGT;
import gregtech6.reactor.IReactorRodItem;
import gregtech6.reactor.ReactorCoolantFluids;
import gregtech6.reactor.ReactorRadioactivity;
import gregtech6.reactor.ReactorRodNbt;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.reactor.neutron.ReactorCoolant;
import gregtech6.reactor.neutron.ReactorLattice2x2;
import gregtech6.reactor.neutron.ReactorNeutrons;
import gregtech6.reactor.neutron.ReactorRodKind;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The 2x2 Nuclear Reactor Core BE (task debt-reactor-b-2x2-be) — the port of
 * gregtech/tileentity/energy/reactors/MultiTileEntityReactorCore2x2.java (418 lines)
 * over the abstract MultiTileEntityReactorCore.java (323 lines), CONSUMING the A-card
 * MC-free neutron domain ({@link gregtech6.reactor.neutron}) for every physics number:
 * the exchange topology rides {@link ReactorLattice2x2}, the fuel emission / durability
 * / heat divider / coolant conversion / ambient radiation arithmetic rides
 * {@link ReactorNeutrons}, the per-kind rod rules ride {@link ReactorRodKind}, the
 * coolant table rides {@link ReactorCoolant} + {@link ReactorCoolantFluids}.
 *
 * <p><b>Structure (Core:57-62)</b>: the double 64000 L tanks ({@code mTanks[0]} the
 * cold input over the 11-entry whitelist of Core:255-267, {@code mTanks[1]} the hot
 * output whose capacity stretches to {@code 64000 * STEAM_PER_WATER} for steam,
 * Core:62), the 4 single-stack rod slots (top/bottom automation faces only, stopped or
 * per-slot-mode gated, Core:414-416), {@code mNeutronCounts/oNeutronCounts[4]}, the
 * stored/output HU pair and the {@code mMode} 4-bit per-slot disable bitmask.
 *
 * <p><b>The tick</b> — upstream ran the machine in two server post-tick proxies
 * (SERVER_TICK_POST = the exchange half, SERVER_TICK_PO2T = the bookkeeping half);
 * the port runs one dispatcher {@code onTick} with the same order (the exchange first,
 * Core2x2:51-99, then the bookkeeping, :100-212). The 20-tick gates key on the GLOBAL
 * game time ({@code SERVER_TIME} upstream; {@link Level#getGameTime()} here) — the
 * phase alignment between neighbouring cores is what makes their exchanges meet, a
 * per-BE timer would desync them (the sensors' per-BE {@code mTimer} shortcut is
 * declared unusable here for exactly that reason).
 *
 * <p><b>Upstream anchors</b>: exchange Core2x2:53-97; moderation update :101-106;
 * {@code tCalc} + the ±200 radiation scan :108-118 (the raycast TODO :110 stays
 * deferred); running flags :120-127; the Na/Sn heat divider :129-134; the 11-branch
 * coolant conversion :136-195 (collapsed onto the {@link ReactorCoolant} table); the
 * meltdown :197-210 — slotKill all four rods, the explode sound, {@code tCalc * 2}
 * (MELTDOWN_STRENGTH_MULTIPLIER) burst at ±500, and the block explosion itself STAYS
 * COMMENTED OUT exactly like upstream :198-199 ("Keep commented out until Reactor
 * System has been tested well enough"); the per-rod dispatchers :216-251.
 *
 * <p><b>Declared cuts/pools</b>: no GUI upstream (the funnel/tap/pincers tool faces,
 * Core:100-123 tooltips) — the port's world faces are the fluid capability door (the
 * whitelist fill + hot drain of Core:255-278), the gated item face (:309-316) and the
 * block's hand-insert (Core2x2:280-294, on the block class); the 11-pass rod/fluid
 * render stack (:320-390) is the render pool; the soft-hammer/thermometer/magnifier/
 * geiger tool chats (:253-277) are the RCON face — this BE exposes the readable
 * accessors instead. The upstream collision heat face (Core:303, 5 heat +
 * radioactivity(3,1) while running) lands on the block's entityInside.
 */
public class GT6ReactorCore2x2BlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream NBT keys (CS.java:1172/:1223/:1241/:1258/:1340) — verbatim names. */
	public static final String NBT_MODE = "gt.mode";
	public static final String NBT_ACTIVE = "gt.active";
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_TANK = "gt.tank";
	public static final String NBT_VALUE = "gt.value";
	/** Upstream NBT_FACING (CS.java:1191). */
	public static final String NBT_FACING = "gt.facing";
	/** Upstream NBT_FAC2NG (CS.java:1192) — the secondary facing. */
	public static final String NBT_FAC2NG = "gt.facing.2nd";
	/** The chest-form inventory key (GTExampleChestBlockEntity precedent). */
	public static final String NBT_INVENTORY = "gt.invlist";

	/** The per-slot disable bits ({@code B[aSlot]} = 1/2/4/8, the mMode low nibble). */
	public static final int[] SLOT_MODE_BITS = {1, 2, 4, 8};

	/** The cold-input tank capacity (Core:62, both tanks 64000 L). */
	public static final long TANK_CAPACITY = 64000;
	/** The steam stretch of the output tank (Core:62, {@code 64000 * STEAM_PER_WATER}, CS.java:242). */
	public static final long STEAM_TANK_CAPACITY = TANK_CAPACITY * 160;

	/** The 4 rod slots (Core:413, single-stack; the rod filter is the canInsert gate half). */
	public final GTItemStackHandler mInventory = new GTItemStackHandler(4, this::setChanged, this::isReactorRod) {
		@Override public int getSlotLimit(int aSlot) {return 1;} // Core:312 getInventoryStackLimit
	};

	/** The double tanks (Core:62) — [0] cold whitelist input, [1] hot output (steam-stretched). */
	public final FluidTankGT[] mTanks = {new FluidTankGT(TANK_CAPACITY), new SteamStretchedTank()};

	/** Core:57-58 — the running and last-tick per-rod neutron counts. */
	public int[] mNeutronCounts = new int[] {0, 0, 0, 0};
	public int[] oNeutronCounts = new int[] {0, 0, 0, 0};
	/** Core:59 — the stored HU and this tick's converted output. */
	public long mEnergy = 0, oEnergy = 0;
	/** Core:60 — the 4-bit per-slot disable bitmask. */
	public byte mMode = 0;
	/** Core:61 — running (any neutron activity or reacting rod) / soft-hammer stop. */
	public boolean mRunning = false, mStopped = false;
	/** The hot-output facing (Core:247 default SIDE_BOTTOM) and the cold-overflow facing (Core:248). */
	public byte mFacing = 0, mSecondFacing = 0;

	/** The offline tick-time seam: without a level the 20-tick gates key on this counter (the test driver advances it). */
	public long mTimeSeam = 0;

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + shared-type fallback shape, the HEX form)
	// ---------------------------------------------------------------------------

	/** The registry-path constructor. */
	public GT6ReactorCore2x2BlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ReactorCore2x2BlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : gregtech6.registry.GT6Reactors.REACTOR_CORE_2X2_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "reactor_core_2x2"; // the BET registry path mirrors it
	}

	/**
	 * The output tank (Core:62 verbatim shape): capacity 64000 L for every fluid except
	 * steam, which stretches to {@code 64000 * STEAM_PER_WATER} — the upstream
	 * {@code setCapacity(FL.Steam.mName, 64000L*STEAM_PER_WATER)} map entry as the one
	 * override of {@link FluidTankGT#capacity(FluidStack)}.
	 */
	private static final class SteamStretchedTank extends FluidTankGT {
		SteamStretchedTank() {
			super(TANK_CAPACITY);
		}

		@Override
		public long capacity(@Nullable FluidStack aFluid) {
			if (aFluid != null && !aFluid.isEmpty() && ReactorCoolantFluids.isSteam(aFluid)) return STEAM_TANK_CAPACITY;
			return super.capacity(aFluid);
		}
	}

	// ---------------------------------------------------------------------------
	// the tick (Core2x2:51-212 — the two server post-tick proxies in one dispatcher)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide) return;

		// the tank auto-moves FIRST (the upstream onTick2 :138-139 ran on the normal
		// dispatcher tick, before both post proxies)
		if (mTanks[0].amount() > mTanks[0].capacity() / 2 && mSecondFacing != mFacing) { // :138 overHalf + distinct faces
			moveExcessColdOut();
		}
		if (mTanks[1].has()) { // :139
			moveHotOut();
		}

		long tTime = hasLevel() ? getLevel().getGameTime() : mTimeSeam; // the GLOBAL phase gate (SERVER_TIME upstream)

		// the first proxy — the exchange (Core2x2:51-99)
		if (tTime % 20 == ReactorNeutrons.EXCHANGE_TICK && !mStopped) {
			exchangePass();
		}

		// the second proxy — the bookkeeping (Core2x2:100-212)
		if (tTime % 20 == ReactorNeutrons.EXCHANGE_TICK) { // :101-106
			updateReactorRodModeration(0);
			updateReactorRodModeration(1);
			updateReactorRodModeration(2);
			updateReactorRodModeration(3);
		}

		long tCalc = ReactorNeutrons.ambientNeutronLevel(
				oNeutronCounts[0] = mNeutronCounts[0],
				oNeutronCounts[1] = mNeutronCounts[1],
				oNeutronCounts[2] = mNeutronCounts[2],
				oNeutronCounts[3] = mNeutronCounts[3]); // :108

		// TODO Raycasting through Lead, Water and similar Blocks. (Core2x2:110 — upstream TODO kept verbatim, deferred)
		if (tCalc > 0 && tTime % 20 == ReactorNeutrons.RADIATION_BURST_TICK) { // :111
			radiationBurst(200, tCalc);
		}

		mRunning = (tCalc != 0); // :120

		long tEnergyBefore = mEnergy; // :122

		if (getReactorRodNeutronReaction(0)) mRunning = true; // :124-127
		if (getReactorRodNeutronReaction(1)) mRunning = true;
		if (getReactorRodNeutronReaction(2)) mRunning = true;
		if (getReactorRodNeutronReaction(3)) mRunning = true;

		ReactorCoolant tCoolant = coolantOf();
		long tGenerated = mEnergy - tEnergyBefore;
		if (tCoolant != null) { // :129-132 — only the heat GENERATED this tick is divided
			ReactorNeutrons.HeatTick tHeat = ReactorNeutrons.heatDividerTick(tEnergyBefore, tGenerated, tCoolant);
			mEnergy = tHeat.stored();
			oEnergy = tHeat.outputThisTick(); // :134
		} else {
			mEnergy = tEnergyBefore + tGenerated;
			oEnergy = tGenerated;
		}

		if (mEnergy > 0) { // :136
			boolean tIsExploding = false;
			if (tCoolant != null) { // the 11-branch conversion :138-195 collapsed onto the table
				long tLiters = ReactorNeutrons.convertibleLiters(mEnergy, tCoolant);
				if (tLiters > 0 && mTanks[0].has(tLiters) && fillAllHot(tCoolant, tLiters)) {
					mEnergy = ReactorNeutrons.storedAfterCoolant(mEnergy, tCoolant, mTanks[0].remove(tLiters));
				} else if (tLiters > 0) {
					tIsExploding = true;
				}
			} else if (mTanks[0].isEmpty()) { // :193-194
				if (oEnergy > 0) tIsExploding = true;
			}

			if (tIsExploding && !inventoryEmpty()) { // :197
				meltdown(tCalc);
			}
		}
	}

	/**
	 * The exchange pass (Core2x2:53-97) — each slot with a non-zero emission (or a
	 * moderated flag) reflects off its 2 orthogonal in-block neighbours
	 * ({@link ReactorLattice2x2#IN_BLOCK_TARGETS}) and the 2 horizontal neighbouring
	 * cores on its outward corner sides ({@link ReactorLattice2x2#OUTWARD_SIDES}, the
	 * receiving slot through {@link ReactorLattice2x2#neighbourReceivingSlot}).
	 */
	private void exchangePass() {
		for (int tSlot = 0; tSlot < 4; tSlot++) { // :63-97 — the four sequential emitters
			int tNeutronCount = getReactorRodNeutronEmission(tSlot);
			boolean tModerated = isReactorRodModerated(tSlot);
			if (tNeutronCount != 0 || tModerated) {
				int[] tInBlock = ReactorLattice2x2.IN_BLOCK_TARGETS[tSlot];
				mNeutronCounts[tSlot] += getReactorRodNeutronReflection(tInBlock[0], tNeutronCount, tModerated);
				mNeutronCounts[tSlot] += getReactorRodNeutronReflection(tInBlock[1], tNeutronCount, tModerated);
				for (int tOutwardSide : ReactorLattice2x2.OUTWARD_SIDES[tSlot]) {
					GT6ReactorCore2x2BlockEntity tAdjacent = adjacentCore(tOutwardSide);
					if (tAdjacent != null) { // :58-61 — horizontal reactor-core neighbours only
						int tRecvSlot = ReactorLattice2x2.neighbourReceivingSlot(tSlot, tOutwardSide, UT_OPOS[tOutwardSide]);
						mNeutronCounts[tSlot] += tAdjacent.getReactorRodNeutronReflection(tRecvSlot, tNeutronCount, tModerated);
					}
				}
			}
		}
	}

	/** {@code UT.Math.OPOS} folded — the GT6 side of the neighbour that faces this core. */
	private static final int[] UT_OPOS = {1, 0, 3, 2, 5, 4};

	/**
	 * The horizontal neighbour core on one outward side (Core2x2:56-61), the level look-up
	 * with the offline adjacency seam ({@link #setAdjacentCoresOverride}).
	 */
	@Nullable
	protected GT6ReactorCore2x2BlockEntity adjacentCore(int aOutwardSide) {
		if (mAdjacentCoresOverride != null) return mAdjacentCoresOverride[aOutwardSide];
		if (!hasLevel()) return null;
		if (aOutwardSide < 2 || aOutwardSide > 5) return null; // horizontal sides only (:58-61)
		Direction tDir = Direction.from3DDataValue(aOutwardSide);
		if (getLevel().getBlockEntity(getBlockPos().relative(tDir)) instanceof GT6ReactorCore2x2BlockEntity tCore) {
			return tCore;
		}
		return null;
	}

	@Nullable
	private GT6ReactorCore2x2BlockEntity[] mAdjacentCoresOverride = null;

	/** The offline seam: the 6-slot neighbour table indexed by the GT6 side (nulls = no core). */
	public void setAdjacentCoresOverride(@Nullable GT6ReactorCore2x2BlockEntity[] aCores) {
		mAdjacentCoresOverride = aCores;
	}

	// ---------------------------------------------------------------------------
	// the per-rod dispatchers (Core2x2:216-251 — the IItemReactorRod calls)
	// ---------------------------------------------------------------------------

	/** Core2x2:216-220 + RodNuclear:175-199 (via {@link ReactorNeutrons#fuelEmission}). */
	public int getReactorRodNeutronEmission(int aSlot) {
		ItemStack tStack = slot(aSlot);
		IReactorRodItem tRod = rodItem(tStack);
		if (!mStopped && (mMode & SLOT_MODE_BITS[aSlot]) == 0 && tRod != null) {
			FuelRodSpec tFuel = tRod.fuelSpec(tStack);
			if (tFuel != null && tRod.rodKind(tStack) == ReactorRodKind.FUEL) {
				ReactorCoolant tCoolant = coolantOf();
				if (tCoolant != null) {
					ReactorNeutrons.FuelEmission tEmission = ReactorNeutrons.fuelEmission(tFuel, tCoolant, oNeutronCounts[aSlot]);
					mNeutronCounts[aSlot] += tEmission.selfAdded(); // Nuclear:197
					return tEmission.emitted(); // Nuclear:198-199
				}
				// off the whitelist (empty tank): NO branch of Nuclear:179-196 matches, so
				// the plain unmodulated triple — the same arithmetic shape as
				// fuelEmission over an identity row (the A-domain divup is
				// package-private, this local copy keeps the domain untouched)
				mNeutronCounts[aSlot] += tFuel.self(); // Nuclear:197
				long tEmission = tFuel.other() + divup(Math.max((long) oNeutronCounts[aSlot] - tFuel.self(), 0), tFuel.div()); // Nuclear:198
				return bindInt(tEmission); // Nuclear:199
			}
			return 0; // the non-fuel rods emit nothing (Base:87 and every override :40/:68/:77)
		}
		mNeutronCounts[aSlot] = 0; // :218
		return 0;
	}

	/** UT.Code.divup (UT.java:1697-1699) shape — the no-coolant emission fallback's local copy (the A-domain original is package-private). */
	private static long divup(long aNumber, long aDivider) {return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);}

	/** UT.Code.bindInt (UT.java:1565) shape — the same local-copy rationale as {@link #divup}. */
	private static int bindInt(long aBoundValue) {return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));}

	/** Core2x2:222-227 + the rod reaction classes (the per-kind branches below). */
	public boolean getReactorRodNeutronReaction(int aSlot) {
		long tTime = hasLevel() ? getLevel().getGameTime() : mTimeSeam;
		if (tTime % 20 == ReactorNeutrons.NEUTRON_ROLLBACK_TICK) { // Core2x2:224
			mNeutronCounts[aSlot] -= oNeutronCounts[aSlot];
		}
		ItemStack tStack = slot(aSlot);
		IReactorRodItem tRod = rodItem(tStack);
		if (mStopped || (mMode & SLOT_MODE_BITS[aSlot]) != 0 || tRod == null) return false;
		ReactorRodKind tKind = tRod.rodKind(tStack);
		switch (tKind) {
			case MODERATOR -> { // Moderator:73-79 — the touch counter latches at the exchange tick
				if (tTime % 20 == ReactorNeutrons.EXCHANGE_TICK) {
					ReactorRodNbt.setModeration(tStack, 0, ReactorRodNbt.moderation(tStack));
				}
				return false;
			}
			case FUEL -> { // Nuclear:204-226
				FuelRodSpec tFuel = tRod.fuelSpec(tStack);
				if (tFuel == null) return false;
				ReactorCoolant tCoolant = coolantOf();
				mEnergy += tKind.reactionHeat(oNeutronCounts[aSlot]); // Nuclear:205 — 1 HU per neutron
				if (tCoolant != null && tCoolant.moderatesFuel) { // Nuclear:208-214 — the four waters
					ReactorRodNbt.setModerated(tStack, true, true);
				}
				int tMax = tCoolant != null ? ReactorNeutrons.neutronMaximum(tFuel, tCoolant) : tFuel.max(); // Nuclear:240-252
				long tLoss = ReactorNeutrons.durabilityLoss(oNeutronCounts[aSlot], tMax, ReactorRodNbt.moderatedO(tStack)); // Nuclear:215-216
				long tDurability = ReactorNeutrons.durabilityAfter(ReactorRodNbt.durability(tStack), tLoss); // Nuclear:217
				ReactorRodNbt.setDurability(tStack, tDurability);
				if (tDurability <= 0) { // Nuclear:220-224 — the in-place depletion swap
					swapRod(aSlot, tRod.rodSwapTarget(tFuel.depletedId()));
					return true;
				}
				return true; // Nuclear:225
			}
			case BREEDER -> { // Breeder:82-92
				BreederRodSpec tBreeder = tRod.breederSpec(tStack);
				if (tBreeder == null) return false;
				mEnergy += tKind.reactionHeat(oNeutronCounts[aSlot]); // :83 — half heat
				ReactorNeutrons.BreederTick tTick = ReactorNeutrons.breederTick(ReactorRodNbt.durability(tStack), oNeutronCounts[aSlot]); // :84-90
				ReactorRodNbt.setDurability(tStack, tTick.durability());
				if (tTick.becameProduct()) {
					swapRod(aSlot, tRod.rodSwapTarget(tBreeder.productId())); // :86 — the enriched product
				}
				return true; // :92
			}
			default -> {
				mEnergy += tKind.reactionHeat(oNeutronCounts[aSlot]); // Absorber:46 2 HU/n; Product:55 half; rest 0
				return tKind.reactionKeepsCoreRunning(); // Absorber:47 T, Product:56 T, Reflector:45 F, rest F
			}
		}
	}

	/**
	 * Core2x2:229-234 + the rod reflection classes — one reflection hit of
	 * {@code aNeutrons} from a (possibly moderated) emitter.
	 */
	public int getReactorRodNeutronReflection(int aSlot, int aNeutrons, boolean aEmitterModerated) {
		ItemStack tStack = slot(aSlot);
		IReactorRodItem tRod = rodItem(tStack);
		if (mStopped || (mMode & SLOT_MODE_BITS[aSlot]) != 0 || tRod == null) return 0;
		ReactorRodKind tKind = tRod.rodKind(tStack);
		int tBreederLoss = tKind == ReactorRodKind.BREEDER && tRod.breederSpec(tStack) != null ? tRod.breederSpec(tStack).loss() : 0;
		mNeutronCounts[aSlot] += tKind.absorbedNeutrons(aNeutrons, aEmitterModerated, tBreederLoss);
		if (tKind.countsModeratorTouch() && aNeutrons > 0) { // Moderator:84-87 — the touch counter
			ReactorRodNbt.setModeration(tStack, ReactorRodNbt.moderation(tStack) + 1, ReactorRodNbt.moderationO(tStack));
		}
		if (tKind.moderatedByTouch() && aEmitterModerated) { // Nuclear:231-234 — the moderation latch
			ReactorRodNbt.setModerated(tStack, true, ReactorRodNbt.moderatedO(tStack));
		}
		return tKind.reflectedNeutrons(aNeutrons, ReactorRodNbt.moderationO(tStack)); // Reflector:51 / Moderator:88
	}

	/** Core2x2:236-244 — the moderated flag the emitters read (the latched half, Moderator is always true). */
	public boolean isReactorRodModerated(int aSlot) {
		ItemStack tStack = slot(aSlot);
		IReactorRodItem tRod = rodItem(tStack);
		if (tRod != null) {
			ReactorRodKind tKind = tRod.rodKind(tStack);
			boolean tIsModerated = tKind == ReactorRodKind.MODERATOR || (tKind == ReactorRodKind.FUEL && ReactorRodNbt.moderatedO(tStack));
			if (mStopped || (mMode & SLOT_MODE_BITS[aSlot]) != 0) return false;
			return tIsModerated;
		}
		return false;
	}

	/** Core2x2:246-251 + Nuclear:260-264 — the fuel moderation latch refresh at the exchange tick. */
	public void updateReactorRodModeration(int aSlot) {
		ItemStack tStack = slot(aSlot);
		IReactorRodItem tRod = rodItem(tStack);
		if (tRod != null && tRod.rodKind(tStack) == ReactorRodKind.FUEL) {
			ReactorRodNbt.setModerated(tStack, false, ReactorRodNbt.moderated(tStack)); // oModerated = mModerated; mModerated = F
		}
	}

	// ---------------------------------------------------------------------------
	// the meltdown (Core2x2:197-210) — the commented-explosion semantics verbatim
	// ---------------------------------------------------------------------------

	/**
	 * The meltdown face (:197-210): every rod slot killed, the explode sound, the
	 * radiation burst doubled ({@link ReactorNeutrons#MELTDOWN_STRENGTH_MULTIPLIER})
	 * at ±500 — and the block explosion ITSELF STAYS COMMENTED, exactly like upstream:
	 *
	 * <pre>{@code
	 * // TODO proper explosion.
	 * // explode(10); // TODO Keep commented out until Reactor System has been tested well enough.
	 * }</pre>
	 *
	 * Enabling it is the card's declared deferral (an upstream超越 needing its own ruling).
	 */
	private void meltdown(long aCalc) {
		mInventory.setStackInSlot(0, ItemStack.EMPTY); // :200 slotKill(0..3)
		mInventory.setStackInSlot(1, ItemStack.EMPTY);
		mInventory.setStackInSlot(2, ItemStack.EMPTY);
		mInventory.setStackInSlot(3, ItemStack.EMPTY);
		if (hasLevel()) { // :201 — SFX.MC_EXPLODE
			getLevel().playSound(null, getBlockPos(),
					//? if forge {
					SoundEvents.GENERIC_EXPLODE,
					//?} else {
					/*SoundEvents.GENERIC_EXPLODE.value(), // 21.1: the Holder carrier
					*///?}
					SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		radiationBurst(500, aCalc * ReactorNeutrons.MELTDOWN_STRENGTH_MULTIPLIER); // :202-208
		updateClientData(); // :209
	}

	/**
	 * The ambient radiation scan (:111-118, the meltdown burst :203-208) — every living
	 * entity within {@code aRange} (the upstream loadedEntityList walk with the
	 * {@code |dx| > range} continue gates, the port's AABB query is the declared
	 * equivalent), strength {@code tCalc - distance}, applied through
	 * {@link ReactorRadioactivity} with the {@code divup(strength, 10)} amplifier.
	 */
	private void radiationBurst(int aRange, long aLevel) {
		if (!hasLevel()) return;
		for (LivingEntity tEntity : getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos()).inflate(aRange))) {
			double tDistance = Math.sqrt(tEntity.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5));
			int tStrength = ReactorNeutrons.ambientStrength(aLevel, tDistance); // :115/:206
			if (tStrength > 0) { // :116/:207
				ReactorRadioactivity.apply(tEntity, ReactorNeutrons.radiationAmplifier(tStrength), tStrength);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the coolant chain (Core2x2:129-195 over the A-card table; Core:255-296 the doors)
	// ---------------------------------------------------------------------------

	/**
	 * The coolant row of the current cold-tank content ({@code null} = empty or off the
	 * whitelist) — the 11-entry match of {@code getFluidTankFillable2} (Core:255-267).
	 */
	@Nullable
	public ReactorCoolant coolantOf() {
		FluidStack tFluid = mTanks[0].getFluid();
		if (tFluid == null || tFluid.isEmpty()) return null;
		return ReactorCoolantFluids.coolantOf(tFluid);
	}

	/**
	 * The hot-side fill (:140/:145/... {@code mTanks[1].fillAll(X.make(tEnergy))}) —
	 * everything or nothing, over the table's output identity and the steam ×160
	 * multiplier ({@link ReactorNeutrons#hotOutputAmount}).
	 */
	private boolean fillAllHot(ReactorCoolant aCoolant, long aLiters) {
		FluidStack tOut = ReactorCoolantFluids.hotStack(aCoolant, ReactorNeutrons.hotOutputAmount(aLiters, aCoolant));
		if (tOut == null) return false; // unresolvable hot fluid (offline without the seam) — the explode branch
		return mTanks[1].fill(tOut, FluidAction.SIMULATE) >= tOut.getAmount()
				&& mTanks[1].fill(tOut, FluidAction.EXECUTE) >= tOut.getAmount();
	}

	/** The :138 cold-overflow move — the excess over half, into the secondary-facing neighbour tank. */
	private void moveExcessColdOut() {
		IFluidHandler tTarget = tankAt(mSecondFacing);
		if (tTarget == null) return;
		long tExcess = mTanks[0].amount() - mTanks[0].capacity() / 2;
		if (tExcess <= 0) return;
		FluidStack tDrain = mTanks[0].get(tExcess);
		if (tDrain == null) return;
		int tMoved = tTarget.fill(tDrain, FluidAction.EXECUTE);
		if (tMoved > 0) mTanks[0].remove(tMoved);
	}

	/** The :139 hot-output move — the whole output tank into the primary-facing neighbour tank. */
	private void moveHotOut() {
		IFluidHandler tTarget = tankAt(mFacing);
		if (tTarget == null) return;
		FluidStack tDrain = mTanks[1].get(Long.MAX_VALUE);
		if (tDrain == null) return;
		int tMoved = tTarget.fill(tDrain, FluidAction.EXECUTE);
		if (tMoved > 0) mTanks[1].remove(tMoved);
	}

	/** The adjacent tank on one side, seen from its opposite face (the getAdjacentTank port). */
	@Nullable
	protected IFluidHandler tankAt(byte aSide) {
		if (mTankTargetsOverride != null) return mTankTargetsOverride[aSide];
		if (!hasLevel() || aSide < 0 || aSide > 5) return null;
		Direction tDir = Direction.from3DDataValue(aSide);
		BlockEntity tNeighbour = getLevel().getBlockEntity(getBlockPos().relative(tDir));
		if (tNeighbour == null || tNeighbour.isRemoved()) return null;
		//? if forge {
		return tNeighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, tDir.getOpposite()).orElse(null);
		//?} else {
		/*return getLevel().getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(tDir), tDir.getOpposite());
		 *///?}
	}

	@Nullable
	private IFluidHandler[] mTankTargetsOverride = null;

	/** The offline seam: the 6-slot neighbour tank table indexed by the GT6 side. */
	public void setTankTargetsOverride(@Nullable IFluidHandler[] aTanks) {
		mTankTargetsOverride = aTanks;
	}

	// ---------------------------------------------------------------------------
	// the slot helpers (the upstream slot()/slotHas()/slotKill() trio over the handler)
	// ---------------------------------------------------------------------------

	/** The rod stack of a slot (null-shape upstream {@code slot(aSlot)}). */
	@Nullable
	public ItemStack slot(int aSlot) {
		ItemStack tStack = mInventory.getStackInSlot(aSlot);
		return tStack.isEmpty() ? null : tStack;
	}

	/** The rod item seam ({@code ST.item(aStack) instanceof IItemReactorRod}). */
	@Nullable
	public static IReactorRodItem rodItem(@Nullable ItemStack aStack) {
		return aStack != null && !aStack.isEmpty() && aStack.getItem() instanceof IReactorRodItem tRod ? tRod : null;
	}

	/** The insert-filter half of the rod gate (upstream isReactorRod, Base:84). */
	public boolean isReactorRod(ItemStack aStack) {
		return rodItem(aStack) != null;
	}

	/** {@code invempty()} — all four slots empty. */
	public boolean inventoryEmpty() {
		for (int i = 0; i < 4; i++) if (slot(i) != null) return false;
		return true;
	}

	/** The in-place swap (upstream {@code ST.meta} + NBT clear, Nuclear:221-222 / Breeder:86-88). */
	private void swapRod(int aSlot, @Nullable ItemStack aSwapTarget) {
		if (aSwapTarget != null && !aSwapTarget.isEmpty()) {
			mInventory.setStackInSlot(aSlot, aSwapTarget);
		}
		updateClientData();
	}

	// ---------------------------------------------------------------------------
	// the faces (the geiger/thermometer read accessors + the capability doors)
	// ---------------------------------------------------------------------------

	/** The geiger arm (:46-48) — the last-tick per-rod neutron counts, sum. */
	public long neutronSum() {
		return (long) oNeutronCounts[0] + oNeutronCounts[1] + oNeutronCounts[2] + oNeutronCounts[3];
	}

	/**
	 * The geiger maximum arm (GeigerCounter:53-66) — the sum of the per-slot neutron
	 * maximums: the coolant-modulated {@link ReactorNeutrons#neutronMaximum} on fuel
	 * rods, 0 on every other kind (the Base:90 default).
	 */
	public long neutronMaximumSum() {
		long tMax = 0;
		for (int i = 0; i < 4; i++) {
			ItemStack tStack = slot(i);
			IReactorRodItem tRod = rodItem(tStack);
			if (tRod != null && tRod.rodKind(tStack) == ReactorRodKind.FUEL && tRod.fuelSpec(tStack) != null) {
				ReactorCoolant tCoolant = coolantOf();
				tMax += tCoolant != null ? ReactorNeutrons.neutronMaximum(tRod.fuelSpec(tStack), tCoolant) : tRod.fuelSpec(tStack).max();
			}
		}
		return tMax;
	}

	/** The thermometer arm (Core:192-197) — this tick's converted HU. */
	public long heatLevel() {
		return oEnergy;
	}

	// ---------------------------------------------------------------------------
	// the fluid capability door (Core:255-301 — fill = the whitelist gate into tank0,
	// drain = the hot tank; the funnel face is the same gate, the tap tool is the pool)
	// ---------------------------------------------------------------------------

	//? if forge {
	private final LazyOptional<IFluidHandler> mFluidCap = LazyOptional.of(ReactorFluidDoor::new);
	//?}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			return mFluidCap.cast();
		}
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			// the side-gated rod face (Core:309/:414-416 — top and bottom only)
			if (aSide == null || aSide == Direction.UP || aSide == Direction.DOWN) return LazyOptional.of(RodSlotFace::new).cast();
			return LazyOptional.empty();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mFluidCap.invalidate();
	}
	//?} else {
	/*// (21.1 seam: NeoForge removed BlockEntity#getCapability — the GT6CapabilityWiring
	//registerBlockEntity row delegates to this member; no @Override. The GT6BatteryBox /
	//GT6HopperBase member shape.)
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) new ReactorFluidDoor();
		}
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			// the side-gated rod face (Core:414 — top and bottom only)
			if (aSide == null || aSide == Direction.UP || aSide == Direction.DOWN) return (T) new RodSlotFace();
		}
		return null;
	}
	 *///?}

	/**
	 * The fluid door: fill = the 11-entry whitelist gate into {@code mTanks[0]}
	 * (Core:255-267, a non-whitelist fluid is REFUSED); drain = the hot output tank
	 * (Core:270-273); the tank view exposes both (Core:275-278).
	 */
	private class ReactorFluidDoor implements IFluidHandler {
		@Override public int getTanks() {return mTanks.length;} // :277
		@Override public FluidStack getFluidInTank(int aTank) {
			FluidStack tStack = aTank >= 0 && aTank < mTanks.length ? mTanks[aTank].getFluid() : null;
			return tStack == null ? FluidStack.EMPTY : tStack;
		}
		@Override public int getTankCapacity(int aTank) {
			return aTank == 1 ? (int) Math.min(Integer.MAX_VALUE, STEAM_TANK_CAPACITY)
					: aTank == 0 ? (int) TANK_CAPACITY : 0;
		}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aTank == 0 && ReactorCoolantFluids.coolantOf(aStack) != null; // the whitelist half
		}
		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (ReactorCoolantFluids.coolantOf(aResource) == null) return 0; // :256-267 the whitelist gate
			return mTanks[0].fill(aResource, aAction); // :267
		}
		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) { // :272
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			return mTanks[1].drain(aResource.getAmount(), aAction);
		}
		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) { // :272
			return mTanks[1].drain(aMaxDrain, aAction);
		}
	}

	/**
	 * The rod-slot item face (Core:309-316 + :414-416): reachable on the top and bottom
	 * sides only (the side gate lives on the capability exposure above); insert = a rod
	 * into an EMPTY slot while stopped or per-slot disabled; extract = while stopped or
	 * per-slot disabled — the pincers replacement (the hopper face of the same gates).
	 */
	private class RodSlotFace implements IItemHandler {
		private boolean gate(int aSlot) {return (mStopped || (mMode & SLOT_MODE_BITS[aSlot]) != 0);} // :415/:416
		@Override public int getSlots() {return 4;} // :414 the ascending 0..3
		@Override public ItemStack getStackInSlot(int aSlot) {return mInventory.getStackInSlot(aSlot);}
		@Override public boolean isItemValid(int aSlot, ItemStack aStack) {
			return gate(aSlot) && slot(aSlot) == null && isReactorRod(aStack); // :415
		}
		@Override public int getSlotLimit(int aSlot) {return 1;} // :312
		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			if (aStack.isEmpty() || !isItemValid(aSlot, aStack)) return aStack;
			if (!aSimulate) mInventory.setStackInSlot(aSlot, aStack.copyWithCount(1));
			return aStack.getCount() <= 1 ? ItemStack.EMPTY : aStack.copyWithCount(aStack.getCount() - 1);
		}
		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) { // :416
			ItemStack tStack = mInventory.getStackInSlot(aSlot);
			if (!gate(aSlot) || tStack.isEmpty() || aAmount <= 0) return ItemStack.EMPTY;
			if (aSimulate) return tStack.copyWithCount(1);
			mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
			return tStack.copyWithCount(1);
		}
	}

	// ---------------------------------------------------------------------------
	// the collision faces (Core:303-304 — the block carrier consumes these)
	// ---------------------------------------------------------------------------

	/** Core:303 — while running, an entity inside the collision box takes 5 heat + radioactivity(3,1). */
	public void onEntityCollided(LivingEntity aEntity) {
		if (mRunning && aEntity.isAlive()) {
			ReactorRadioactivity.applyHeatDamage(aEntity, 5.0F); // UT.Entities.applyHeatDamage(_, 5)
			ReactorRadioactivity.apply(aEntity, 3, 1);
		}
	}

	/** The soft-hammer arm (Core:185-191) — the RCON/tool stand-in toggles the stop latch. */
	public boolean toggleStopped() {
		mStopped = !mStopped;
		setChanged();
		updateClientData();
		return mStopped;
	}

	/**
	 * The hand-insert arm (Core2x2:280-294 {@code onBlockActivated3}) — a rod into an
	 * empty quadrant slot stops the reactor and clicks; the BLOCK face pays the held
	 * stack (the {@code ST.use} consume).
	 */
	public boolean handInsertRod(int aSlot, ItemStack aStack) {
		if (aSlot < 0 || aSlot > 3 || slot(aSlot) != null || !isReactorRod(aStack)) return false;
		mInventory.setStackInSlot(aSlot, aStack.copyWithCount(1)); // :286 slot(tSlot, ST.amount(1, aStack))
		mStopped = true; // :287 — inserting a rod stops the reactor
		if (hasLevel()) { // :288 SFX.MC_CLICK
			getLevel().playSound(null, getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 0.6F);
		}
		updateClientData(); // :289
		return true;
	}

	// ---------------------------------------------------------------------------
	// persistence (Core:64-98 + the facing pair + the inventory, the chest form)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	//?}
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_MODE, mMode); // :88
		aNBT.putLong(NBT_ENERGY, mEnergy); // :89
		aNBT.putBoolean(NBT_ACTIVE, mRunning); // :90
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :91
		aNBT.putByte(NBT_FACING, mFacing); // the 09FacingSingle :56 write
		aNBT.putByte(NBT_FAC2NG, mSecondFacing); // the 10FacingDouble :54 write
		mTanks[0].writeToNBT(aNBT, NBT_TANK + ".0"); // :92
		mTanks[1].writeToNBT(aNBT, NBT_TANK + ".1"); // :93
		for (int i = 0; i < 4; i++) { // :94-97
			aNBT.putInt(NBT_VALUE + ".m." + i, mNeutronCounts[i]);
			aNBT.putInt(NBT_VALUE + ".o." + i, oNeutronCounts[i]);
		}
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(NBT_ACCESS));
		 *///?}
	}

	//? if forge {
	@Override
	//?}
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE); // :67
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :68
		if (aNBT.contains(NBT_ACTIVE)) mRunning = aNBT.getBoolean(NBT_ACTIVE); // :69
		if (aNBT.contains(NBT_STOPPED)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :70
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
		if (aNBT.contains(NBT_FAC2NG, Tag.TAG_ANY_NUMERIC)) mSecondFacing = aNBT.getByte(NBT_FAC2NG);
		mTanks[0].readFromNBT(aNBT, NBT_TANK + ".0"); // :71
		mTanks[1].readFromNBT(aNBT, NBT_TANK + ".1"); // :72
		for (int i = 0; i < 4; i++) { // :73-76
			mNeutronCounts[i] = aNBT.getInt(NBT_VALUE + ".m." + i);
			oNeutronCounts[i] = aNBT.getInt(NBT_VALUE + ".o." + i);
		}
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			//? if forge {
			mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
			//?} else {
			/*mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
			 *///?}
		}
	}
}
