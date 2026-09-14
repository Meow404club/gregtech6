package gregtech6.tileentity.multiblocks;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}

import gregapi.oredict.OreDictMaterial;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluidLists;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GT6Tanks;
import gregtech6.tileentity.attachment.GTFunnelBlockEntity;
import gregtech6.tileentity.attachment.GTTapBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 Tank multiblock family — task p29-w3-tank-valves, ported
 * from gregtech/tileentity/multiblocks/MultiTileEntityTank.java (the fluid/proof/meltdown
 * core, :47-129) + MultiTileEntityTank3x3x3.java (:44-141) + MultiTileEntityTank5x5x5.java
 * (:44-140, the same shape at radius 2) as ONE class over the 25 block-carrier rows.
 *
 * <p><b>The structure (:61-75 both radii, hand-written verbatim)</b>: a hollow
 * {@code (2r+1)^3} of the row's Tank Walls around the centre — ONE cell in front of the
 * valve for the 3x3x3 (r=1, {@code getOffsetXN(mFacing)}), TWO cells for the 5x5x5 (r=2,
 * the upstream {@code getOffsetXN(mFacing, 2)} multiplier form, TileEntityBase01Root's
 * minimal face folds it as {@code relative(dir, -2)}). The centre must be air — the
 * upstream {@code getAir → setBlockToAir} pair is the idempotent clear-if-air, else FAIL
 * (:67, fail-not-clear like the boiler's :102 ruling); every other cell goes through
 * {@link ITileEntityMultiBlockController.Util#checkAndSetTarget} with the row's wall block,
 * design 0 (the upstream third argument is the LITERAL 0, :69 — the NBT_DESIGN SINGULAR
 * wall choice is the ROW's {@code wallPath}, resolved before the check) and the
 * {@link MultiBlockPartBlockEntity#ONLY_FLUID} mode. The controller's own cell sits in the
 * wall band and passes through the self-cell arm (:48-49). The loaded guard is the
 * upstream four-corner probe (:63/:139 — unloaded corners keep the last verdict).
 *
 * <p><b>The declared pattern</b> (the ghost-preview + generic-form face): the 3x3x3 binds
 * 26 formingPart cells + the hollow centre — declaring the controller cell TOO is correct
 * for every facing, the self-cell arm absorbs it exactly like the upstream loop. The
 * 5x5x5 declares NOTHING: its centre sits at anchor distance 2, outside the pattern
 * system's distance-1 {@code cellOffset} convention (GTMultiBlockPattern:344-357) — the
 * existence-probe escape hatch (getStructurePattern default null), the hand-written check
 * stays the server truth for it (declared on the task card, the form arm drives
 * {@link #checkStructure} directly through the family command).
 *
 * <p><b>The tank semantics (:47-129)</b>: ONE {@link FluidTankGT} at the row capacity
 * (:60), persisted under NBT_TANK (:61/:67). The tick (:84-128, both radii identical
 * outside the loop bound):
 * <ol>
 * <li>temperature ≥ the row material's melting point → {@link #meltdown()} (the :89
 *     guard; the melting point reads the deferred material at runtime, the null
 *     generation falls back to the OreDictMaterial class default 1000 K);</li>
 * <li>{@code !mMagicProof && FL.magic} → fizz + trash + the TC flux-block scatter
 *     (:91-100 — the flux BLOCKS are unported, the declared deviation: fizz+trash only);</li>
 * <li>{@code !mAcidProof && FL.acid} → fizz + trash + a 1-in-3 dissolve sweep over the
 *     wall cells + the valve itself to air (:101-110, setToAir verbatim);</li>
 * <li>{@code !mPlasmaProof && FL.plasma} → trash (:111-113), then {@code !mGasProof &&
 *     FL.gas} → trash (:115-117), then {@code !allowFluid} → trash (:119-121) — the
 *     upstream else-if chain, NO early return (a trashed tank just stays empty);</li>
 * <li>the auto-emit (:123-124): the whole tank offer moves into the facing neighbour when
 *     the facing is horizontal, or the content is gaseous, or (lighter-than-air and the
 *     valve faces UP) / (heavier and it faces DOWN) — the SIDES_TOP/SIDES_BOTTOM table
 *     arithmetic verbatim.</li>
 * </ol>
 * {@code allowFluid} (:101-103) = NOT power-conducting AND temperature below the melting
 * point AND ({@code !onlySimple() || FL.simple}) — the wood valve is the onlySimple row
 * (MultiTileEntityTank3x3x3Wood.java:29).
 *
 * <p><b>The interaction faces</b>: funnelFill/tapDrain (:114-121, the
 * {@link GTFunnelBlockEntity.FunnelAccessible}/{@link GTTapBlockEntity.TapAccessible}
 * pair — direct tank access, no gate, the upstream face) and the fluid capability door
 * (the fresh wrapper-per-call form). <b>Declared deviation</b>: the upstream valve-face
 * split — parts fill ({@code getFluidTankFillable} :123), direct drain
 * ({@code getFluidTankDrainable2} :127), direct fill NULL (:126) — folds into ONE
 * capability face accepting fill+drain on every side: the 1.20.1 part relay forwards to
 * the controller capability carrying no part identity (the MultiBlockPartBlockEntity
 * relay ruling, the port-consistent boiler shape).
 *
 * <p><b>NO GUI by census</b> (:71-82); the NO_GUI_FUNNEL_TAP_TO_TANK tooltip block is the
 * lang/datagen face. NBT items carrying tank content: none (the tank has no pick-block
 * content face upstream either, the task-card "不做" note).
 */
public class GTTankValveBlockEntity extends TileEntityBase10MultiBlockBase
		implements GTTapBlockEntity.TapAccessible, GTFunnelBlockEntity.FunnelAccessible {

	/** The NBT key of the tank content (upstream NBT_TANK, :61/:67 — the in-repo plain-key form). */
	public static final String NBT_TANK = "gt.tank0";

	/** The content tank (upstream :48 — the default 432000 is the wood row value; the ctor re-derives from the row). */
	public final FluidTankGT mTank = new FluidTankGT(432000);

	/** The four proof flags (upstream :50; the row re-derives them in the ctor). */
	public boolean mGasProof = false, mAcidProof = false, mPlasmaProof = false, mMagicProof = false;

	/**
	 * The upstream fluid-name category sets this tank judges on (the CS.java:1509-1531
	 * FluidsGT literals VERBATIM, the declared-minimal port rule: only names of fluids the
	 * port actually registers are listed beside the upstream compat names — the sets are
	 * live code, a compat/mod fluid whose registry path matches becomes judged).
	 */
	public static final java.util.Set<String> SIMPLE = new java.util.HashSet<>();
	public static final java.util.Set<String> PLASMA = new java.util.HashSet<>();
	public static final java.util.Set<String> MAGIC = new java.util.HashSet<>();

	static {
		// CS.java:1512 SIMPLE seeds ("poison", the FOOD fold "coffee") + the FL enum SIMPLE
		// members the port registers (FL.java:85 Steam, :103 Lava, :110 Water, :111 DistW).
		SIMPLE.add("poison"); SIMPLE.add("coffee");
		SIMPLE.add("water"); SIMPLE.add("lava"); SIMPLE.add("steam"); SIMPLE.add("distilled_water");
		// CS.java:1518 PLASMA seeds (the "rc fusion plasma" RotaryCraft compat name is not a
		// port registry path — cut with the compat names, the GTFluidLists seeding rule).
		PLASMA.add("heliumplasma"); PLASMA.add("nitrogenplasma");
		// CS.java:1515 MAGIC seeds verbatim.
		MAGIC.add("fluxgoo"); MAGIC.add("fluxgas"); MAGIC.add("fluiddeath"); MAGIC.add("fluidpure"); MAGIC.add("liquidessence");
	}

	/** Upstream FL.temperature default (CS.java:135 DEF_ENV_TEMP = C + 20 = 293). */
	public static final long DEF_ENV_TEMP = 293;
	/** The steam special case (FL.java:793 — {@code C+100} = 373 K, the only named override). */
	public static final long STEAM_TEMP = 373;
	/** The OreDictMaterial class default melting point (OreDictMaterial.java:114) — the null-generation fallback. */
	public static final long DEFAULT_MELTING_POINT = 1000;

	// ---------------------------------------------------------------------------
	// the offline seams (the W3 boiler form: no Level, no live fluid registry)
	// ---------------------------------------------------------------------------

	/** The live rng (:96/:106/:134 — the world Random; the boiler rng-seam shape). */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	@Nullable
	private java.util.function.IntSupplier mRngOverride = null;

	/** The offline rng seam. */
	public void setRngOverride(@Nullable java.util.function.IntSupplier aRng) {
		mRngOverride = aRng;
	}

	/** The offline melting-point seam (the null-generation guard + the physics fixture). */
	public void setMeltingPointOverride(long aMeltingPoint) {
		mMeltingPointOverride = aMeltingPoint;
	}

	@Nullable
	private Long mMeltingPointOverride = null;

	/** The offline fluid-temperature seam (the FluidType lookup is unavailable without a boot). */
	public void setFluidTemperatureSeam(@Nullable Function<FluidStack, Long> aSeam) {
		if (aSeam != null) mFluidTemperature = aSeam;
	}

	private Function<FluidStack, Long> mFluidTemperature = GTTankValveBlockEntity::fluidTemperature;

	/** The offline lighter/gas density seam (the gasDensitySign guard shape). */
	public void setDensitySignSeam(@Nullable Function<FluidStack, Integer> aSeam) {
		if (aSeam != null) mDensitySign = aSeam;
	}

	private Function<FluidStack, Integer> mDensitySign = GTTankValveBlockEntity::densitySign;

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + block-carrier row shape)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GTTankValveBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime. A placed variant block injects its registration row
	 * (the upstream NBT_TANK_CAPACITY + the four proof flags read); foreign blocks (the
	 * offline fixtures) keep the class defaults.
	 */
	public GTTankValveBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Tanks.TANK_VALVE_BE.get(), aPos, aState);
		GT6Tanks.TankValveRow tRow = row();
		if (tRow != null) {
			mTank.setCapacity(tRow.capacity()); // :60
			mGasProof = tRow.gasProof();
			mAcidProof = tRow.acidProof();
			mPlasmaProof = tRow.plasmaProof();
			mMagicProof = tRow.magicProof();
		}
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_tank_valve"; // BET registry path mirrors it
	}

	/** The registration row (the block carrier read) — null for a foreign block. */
	@Nullable
	public GT6Tanks.TankValveRow row() {
		return GT6Tanks.rowOf(getBlockState().getBlock());
	}

	/** The radius of the hollow: 1 (3x3x3) or 2 (5x5x5) — the {@code getOffsetXN(mFacing, r)} distance. */
	public int radius() {
		GT6Tanks.TankValveRow tRow = row();
		return tRow == null ? 1 : tRow.size() / 2;
	}

	/** The wall block of this variant (the row's NBT_DESIGN wall — the GTTankValveBlock carrier). */
	protected Block getWallBlock() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof gregtech6.block.multiblock.GTTankValveBlock tValve) {
			return tValve.wallBlock();
		}
		return gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH
				.get(gregtech6.registry.GTMultiBlocks.WALL_ROWS.get(0).path()).get(); // the SS Dense Wall defensive default
	}

	// ---------------------------------------------------------------------------
	// the fluid verdicts (upstream FL.java:741-776 + CS.java:1509-1531 name lists)
	// ---------------------------------------------------------------------------

	/** Upstream FL.powerconducting — the POWER_CONDUCTING name-set form (UT.java:187). */
	public static boolean isPowerConducting(@Nullable FluidStack aFluid) {
		return GTFluidLists.isPowerConducting(GTFluidLists.name(aFluid));
	}

	/** Upstream FL.acid — the ACID name-list form (FL.java:756; the port Categories set, test-injectable). */
	public static boolean isAcid(@Nullable FluidStack aFluid) {
		return gregtech6.tileentity.attachment.GTAttachmentSmallBlockEntity.isAcid(aFluid);
	}

	/** Upstream FL.gas — the density-sign + GAS-list verdict (FL.java:768, the port isGas shape). */
	public static boolean isGas(@Nullable FluidStack aFluid) {
		return gregtech6.tileentity.attachment.GTAttachmentSmallBlockEntity.isGas(aFluid);
	}

	/** Upstream FL.plasma — the PLASMA name-set form (FL.java:760). */
	public static boolean isPlasma(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && PLASMA.contains(GTFluidLists.name(aFluid));
	}

	/** Upstream FL.magic — the MAGIC name-set form (FL.java:764). */
	public static boolean isMagic(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && MAGIC.contains(GTFluidLists.name(aFluid));
	}

	/** Upstream FL.simple — the SIMPLE name-set form (FL.java:752). */
	public static boolean isSimple(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && SIMPLE.contains(GTFluidLists.name(aFluid));
	}

	/** Upstream FL.lava — the vanilla lava identity (FL.java:710). */
	public static boolean isLava(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && aFluid.getFluid() == Fluids.LAVA;
	}

	/** Upstream FL.lighter — the FluidType density sign (FL.java:775; the guarded lookup). */
	public static boolean isLighter(@Nullable FluidStack aFluid) {
		return densitySign(aFluid) < 0;
	}

	/** The guarded density sign (the gasDensitySign offline shape: no boot reads as 0 = neither). */
	static int densitySign(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return 0;
		try {
			return aFluid.getFluid().getFluidType().getDensity(aFluid) < 0 ? -1 : 1;
		} catch (Throwable t) {
			return 0;
		}
	}

	/**
	 * Upstream FL.temperature (:788-800): the steam special case 373 K, else the FluidType
	 * temperature; the guarded lookup falls back to DEF_ENV_TEMP 293 (no boot).
	 */
	public static long fluidTemperature(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return DEF_ENV_TEMP;
		if ("steam".equals(GTFluidLists.name(aFluid))) return STEAM_TEMP; // FL.java:793
		try {
			return aFluid.getFluid().getFluidType().getTemperature(aFluid);
		} catch (Throwable t) {
			return DEF_ENV_TEMP;
		}
	}

	/** The row material's melting point (the tick-time resolution; the null generation falls back to the class default). */
	public long meltingPoint() {
		if (mMeltingPointOverride != null) return mMeltingPointOverride;
		GT6Tanks.TankValveRow tRow = row();
		if (tRow == null) return DEFAULT_MELTING_POINT;
		OreDictMaterial tMaterial = tRow.material().get();
		return tMaterial == null ? DEFAULT_MELTING_POINT : tMaterial.mMeltingPoint;
	}

	/**
	 * Upstream allowFluid (:101-103): NOT power-conducting AND below the melting point AND
	 * ({@code !onlySimple() || FL.simple}) — the wood valve is the only onlySimple row.
	 */
	public boolean allowFluid(@Nullable FluidStack aFluid) {
		boolean tOnlySimple = row() != null && row().onlySimple();
		return !isPowerConducting(aFluid) && fluidTemperature(aFluid) < meltingPoint()
				&& (!tOnlySimple || isSimple(aFluid));
	}

	// ---------------------------------------------------------------------------
	// the structure (:61-81 verbatim, both radii)
	// ---------------------------------------------------------------------------

	/** The structure centre — {@code getOffsetXN/YN/ZN(mFacing)} at the radius distance (the multiplier form folded). */
	private BlockPos centre() {
		Direction tDir = Direction.from3DDataValue(mFacing);
		return getBlockPos().relative(tDir, -radius()); // the OFFX/Y/Z subtraction, r cells
	}

	/** The four-corner loaded probe (:63/:77 — the corners of the centre layer at ±radius). */
	private boolean cornersLoaded() {
		BlockPos tCentre = centre();
		int r = radius();
		return getLevel().isLoaded(new BlockPos(tCentre.getX() - r, tCentre.getY(), tCentre.getZ() - r))
				&& getLevel().isLoaded(new BlockPos(tCentre.getX() + r, tCentre.getY(), tCentre.getZ() - r))
				&& getLevel().isLoaded(new BlockPos(tCentre.getX() - r, tCentre.getY(), tCentre.getZ() + r))
				&& getLevel().isLoaded(new BlockPos(tCentre.getX() + r, tCentre.getY(), tCentre.getZ() + r));
	}

	/** Upstream :61-75 verbatim (the :67 clear-if-air centre, the :69 wall checks at design 0). */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay;
		int r = radius();
		if (!cornersLoaded()) return mStructureOkay; // :63/:74 — unloaded corners keep the last verdict
		BlockPos tCentre = centre();
		boolean tSuccess = true;
		for (int i = -r; i <= r; i++) for (int j = -r; j <= r; j++) for (int k = -r; k <= r; k++) {
			if (i == 0 && j == 0 && k == 0) {
				// :67 — clear-if-air (getAir → setBlockToAir), a non-air centre FAILS
				BlockPos tPos = new BlockPos(tCentre.getX() + i, tCentre.getY() + j, tCentre.getZ() + k);
				BlockState tState = getLevel().getBlockState(tPos);
				if (tState.isAir()) {
					getLevel().setBlock(tPos, Blocks.AIR.defaultBlockState(), 3); // the idempotent no-op
				} else if (!tState.canBeReplaced()) {
					tSuccess = false;
				} else {
					getLevel().setBlock(tPos, Blocks.AIR.defaultBlockState(), 3); // the WD.air replaceable family clears
				}
			} else {
				// :69 — the wall check, design 0 (the LITERAL; the wall choice is the row's wallPath)
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this,
						tCentre.getX() + i, tCentre.getY() + j, tCentre.getZ() + k,
						getWallBlock(), 0, MultiBlockPartBlockEntity.ONLY_FLUID, aCoordinates, aPlayer, aInventory)) {
					tSuccess = false;
				}
			}
		}
		return tSuccess;
	}

	/** Upstream :78-81 verbatim — the box around the centre at ±radius. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tCentre = centre();
		int r = radius();
		return aX >= tCentre.getX() - r && aY >= tCentre.getY() - r && aZ >= tCentre.getZ() - r
				&& aX <= tCentre.getX() + r && aY <= tCentre.getY() + r && aZ <= tCentre.getZ() + r;
	}

	/**
	 * The 3x3x3 declared pattern (26 formingPart cells + the hollow centre — the
	 * controller-in-band cell resolves to the self-cell arm for every facing, so declaring
	 * all 26 is correct). The 5x5x5 stays unbound: the distance-2 anchor is outside the
	 * pattern system's distance-1 cellOffset convention (the class-doc ruling).
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (radius() != 1) return null; // the 5x5x5 escape hatch
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			Block tWall = getWallBlock();
			for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
				if (i == 0 && j == 0 && k == 0) continue; // the centre — declared hollow below
				tBuilder.formingPart(i, j, k, tWall, MultiBlockPartBlockEntity.ONLY_FLUID, 0);
			}
			tBuilder.hollow(0, 0, 0, GTMultiBlockPattern.AIR);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	// ---------------------------------------------------------------------------
	// the tick (:84-128 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide || !checkStructure(false)) return; // :86
		FluidStack tFluid = mTank.getFluid();
		if (tFluid == null || mTank.amount() <= 0) return; // :87-88

		if (mFluidTemperature.apply(tFluid) >= meltingPoint() && meltdown()) return; // :89

		if (!mMagicProof && isMagic(tFluid)) { // :91
			trashContent(); // the :93 GarbageGT.trash — the flux-block scatter is unported (the declared deviation)
			return;
		}
		if (!mAcidProof && isAcid(tFluid)) { // :101
			trashContent();
			acidDissolve(); // the :105-108 sweep — walls at rng(3)==0, the valve itself to air
			return;
		}
		if (!mPlasmaProof && isPlasma(tFluid)) { // :111
			trashContent();
		} else if (!mGasProof && isGas(tFluid)) { // :115
			trashContent();
		} else if (!allowFluid(tFluid)) { // :119
			trashContent();
		} else if (shouldEmit(tFluid)) { // :123
			autoEmit(); // :124
		}
	}

	/** The :93/:103/:112/:116/:120 GarbageGT.trash — the content voided, the valve survives. */
	private void trashContent() {
		mTank.setEmpty();
		setChanged();
	}

	/** The :105-108 acid sweep — each wall cell dissolves at rng(3)==0, then the valve itself to air (:108 setToAir). */
	private void acidDissolve() {
		BlockPos tCentre = centre();
		int r = radius();
		for (int i = -r; i <= r; i++) for (int j = -r; j <= r; j++) for (int k = -r; k <= r; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tPos = new BlockPos(tCentre.getX() + i, tCentre.getY() + j, tCentre.getZ() + k);
			if (rng(3) == 0) getLevel().setBlock(tPos, Blocks.AIR.defaultBlockState(), 3); // :106 setBlockToAir
		}
		setToAir(); // :108
	}

	/**
	 * The :123 emit condition: a horizontal facing, or gaseous content, or
	 * {@code (lighter ? SIDES_TOP : SIDES_BOTTOM)[mFacing]} — lighter-than-air emits UP,
	 * heavier emits DOWN (the port side order == the Direction 3D data: DOWN=0, UP=1).
	 */
	public boolean shouldEmit(FluidStack aFluid) {
		byte tFacing = mFacing;
		boolean tHorizontal = tFacing >= Direction.NORTH.get3DDataValue() && tFacing <= Direction.EAST.get3DDataValue(); // SIDES_HORIZONTAL 2..5
		if (tHorizontal) return true;
		if (isGas(aFluid)) return true; // the :123 FL.gas(mTank) middle term
		return mDensitySign.apply(aFluid) < 0
				? tFacing == Direction.UP.get3DDataValue()   // SIDES_TOP
				: tFacing == Direction.DOWN.get3DDataValue(); // SIDES_BOTTOM
	}

	/** The :124 emit — the whole tank offer into the facing neighbour (the FL.move(mTank, adjacent) shape). */
	private void autoEmit() {
		FluidStack tContent = mTank.getFluid();
		if (tContent == null || mTank.amount() <= 0) return;
		Direction tDir = Direction.from3DDataValue(mFacing);
		IFluidHandler tTarget = fluidHandlerAt(getBlockPos().relative(tDir), tDir.getOpposite()); // the neighbour's face towards the valve
		if (tTarget == null) return;
		int tOffer = FluidTankGT.bindInt(mTank.amount());
		FluidStack tProbe = new FluidStack(tContent.getFluid(), tOffer);
		int tAccepted = tTarget.fill(tProbe, FluidAction.SIMULATE);
		if (tAccepted > 0) {
			tTarget.fill(new FluidStack(tContent.getFluid(), tAccepted), FluidAction.EXECUTE);
			mTank.remove(tAccepted);
			setChanged(); // the :124 updateInventory() beat
		}
	}

	/** The :130-140 meltdown (the 3x3x3 form — the 5x5x5 :130-138 lacks the lava arm, the asymmetry verbatim). */
	public boolean meltdown() {
		BlockPos tCentre = centre();
		int r = radius();
		for (int i = -r; i <= r; i++) for (int j = -r; j <= r; j++) for (int k = -r; k <= r; k++) {
			// :133 WD.burn — the fire-spread surface is unported (the declared deviation);
			// :134 — the 1-in-4 fire placement survives verbatim
			if (rng(4) == 0) {
				BlockPos tPos = new BlockPos(tCentre.getX() + i, tCentre.getY() + j, tCentre.getZ() + k);
				if (tPos.equals(getBlockPos())) continue; // the valve's own cell → setToFire below
				getLevel().setBlock(tPos, Blocks.FIRE.defaultBlockState(), 3);
			}
		}
		// :136 — the 3x3x3-only lava arm: ≥1000 L of lava leaves a lava block in the centre
		if (isLava(mTank.getFluid()) && mTank.amount() >= 1000) {
			mTank.remove(1000);
			getLevel().setBlock(tCentre, Blocks.LAVA.defaultBlockState(), 3);
		}
		trashContent(); // :137
		setToFire(); // :138
		return true;
	}

	/** Upstream setToAir (:108) — the valve block replaced with air (the BE goes with it). */
	private void setToAir() {
		getLevel().setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
	}

	/** Upstream setToFire (:138) — the valve block replaced with fire (the meltdown end state). */
	private void setToFire() {
		getLevel().setBlock(getBlockPos(), Blocks.FIRE.defaultBlockState(), 3);
	}

	// ---------------------------------------------------------------------------
	// the funnel/tap faces (:114-121 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public int funnelFill(byte aSide, @Nullable FluidStack aFluid, boolean aDoFill) {
		return mTank.fill(aFluid, aDoFill ? FluidAction.EXECUTE : FluidAction.SIMULATE); // :114-116
	}

	@Override
	@Nullable
	public FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {
		FluidStack tDrained = mTank.drain(aMaxDrain, aDoDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE); // :119-121
		return tDrained == null || tDrained.isEmpty() ? null : tDrained;
	}

	// ---------------------------------------------------------------------------
	// the fluid capability door (the fresh wrapper-per-call form; the fill/drain fold
	// is the declared deviation on the class doc)
	// ---------------------------------------------------------------------------

	private class TankValveFluidHandler implements IFluidHandler {

		@Override public int getTanks() {return 1;} // :125/:128 — the single tank view
		@Override public FluidStack getFluidInTank(int aTank) {
			FluidStack tFluid = aTank == 0 ? mTank.fluid() : null;
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}
		@Override public int getTankCapacity(int aTank) {return aTank == 0 ? mTank.getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {return aTank == 0 && mTank.isFluidValid(aStack);}

		@Override
		public int fill(@Nullable FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			return mTank.fill(aResource, aAction); // the :123 fillable face (the side-fold deviation)
		}

		@Override
		public FluidStack drain(@Nullable FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			return mTank.drain(aResource.getAmount(), aAction); // the :127/:128 drainable face
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			return mTank.drain(aMaxDrain, aAction);
		}
	}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			return LazyOptional.of(TankValveFluidHandler::new).cast(); // the fresh wrapper (the boiler form)
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// 21.1: BlockEntity carries no getCapability — the seam member the
	//GT6CapabilityWiring provider delegates into (the TileEntityLargeBoiler fork shape).
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.FluidHandler.BLOCK) {
			return (T) new TankValveFluidHandler();
		}
		return null;
	}
	 *///?}

	/** The leg-neutral resolve the auto-emit walks (the steamTargetAt idiom). */
	@Nullable
	private IFluidHandler fluidHandlerAt(BlockPos aPos, @Nullable Direction aSide) {
		BlockEntity tNeighbor = getLevel().getBlockEntity(aPos);
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		//? if forge {
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, aSide).orElse(null);
		//?} else {
		/*return getLevel().getCapability(Capabilities.FluidHandler.BLOCK, aPos, aSide);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// NBT (:53-68 — the tank content is the whole payload; the row config re-derives
	// from the block carrier)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		mTank.writeToNBT(aNBT, NBT_TANK); // :67
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		GT6Tanks.TankValveRow tRow = row();
		if (tRow != null) {
			mTank.setCapacity(tRow.capacity()); // :60 — the row re-derives before the content read
			mGasProof = tRow.gasProof();
			mAcidProof = tRow.acidProof();
			mPlasmaProof = tRow.plasmaProof();
			mMagicProof = tRow.magicProof();
		}
		mTank.readFromNBT(aNBT, NBT_TANK); // :61
	}
}
