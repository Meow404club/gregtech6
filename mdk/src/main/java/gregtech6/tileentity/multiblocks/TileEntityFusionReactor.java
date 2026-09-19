package gregtech6.tileentity.multiblocks;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMultiBlocks;

/**
 * The Fusion Reactor multiblock controller (task p31-fusion) — the 1.20.1/1.21.1 port of
 * gregtech/tileentity/multiblocks/MultiTileEntityFusionReactor.java over
 * {@link TileEntityBase10MultiBlockMachine} (Loader_MultiTileEntities.java:1242: meta 17198,
 * item 17101, "Fusion Reactor", MT.SteelGalvanized, NBT_HARDNESS 12.5F == NBT_RESISTANCE
 * 12.5F, NBT_TEXTURE "fusionreactor").
 *
 * <p><b>The :1242 registration columns</b> (registration config, never persisted — the
 * loadKeepsTheConstructorInjectedConfig contract): NBT_INPUT 8192 / NBT_INPUT_MIN 1 /
 * NBT_INPUT_MAX 16384 (the explicit-override form, {@link #applyEnergyRowSpec}),
 * NBT_ENERGY_ACCEPTED TD.Energy.TU, NBT_ENERGY_ACCEPTED_2 TD.Energy.LU (the glass-ring
 * type, {@link #mEnergyTypeCharged}), NBT_ENERGY_EMITTED TD.Energy.EU
 * ({@link #mEnergyTypeEmitted}), NBT_RECIPEMAP RM.Fusion → {@link GT6RecipeMaps#FUSION},
 * NBT_NO_CONSTANT_POWER T. No NBT_NEEDS_IGNITION key → {@code mRequiresIgnition = false}
 * (the :1241 Massfab reading). No NBT_PARALLEL/CHEAP_OVERCLOCKING keys → the defaults.
 *
 * <p><b>The structure</b> (upstream checkStructure2 :47-126 verbatim): the anchor is the
 * core centre 2 cells BEHIND the front face ({@code getOffsetXN(mFacing, 2)}, the port
 * anchor {@link GTMultiBlockPattern#anchorOffset} doubled). Five layers around that centre:
 * <ul>
 * <li>the 5x5x5 core walk (i²+j²+k² over -2..2): d²&lt;4 = the Quadcore PU cross —
 *     ≥3 Versatile (18200) / ≥12 Logic (18201) / ≥12 Control (18202) via the per-cell
 *     fallback chain (:54-64) and the :72 quota check; d²&gt;6 or the j==0 axis tips
 *     (|i|==2/|k|==2 cross arms) = Galvanized Steel Walls (18008, :65-66); everything
 *     between = Ventilation Units (18299, :68) — the 50;</li>
 * <li>the four orthogonal extension arms ±3/±4 along X/Z at the core level, skipping the
 *     facing direction itself (:74-89), 18008 — 8 cells, 4 of the 8+ the :65 axis tips
 *     merge into the 53 GalvSteel total;</li>
 * <li>OCTAGONS[0] (:94-102): y-1/y+1 = Tungstensteel Walls (18003, design 0,
 *     ONLY_ITEM_FLUID); y0 = the 'glass' ring — the four orthogonal edge midpoints
 *     (i/j == 0/18 cross) are design 2 ONLY_ENERGY_OUT, every other cell is design
 *     {@code mActive ? 6 : 5} ONLY_ENERGY_IN (the LU input faces, the active flip);</li>
 * <li>OCTAGONS[1] (:103-113): y-2/y+2 = 18003 ONLY_ITEM_FLUID, y-1/y+1 = 18003 NOTHING,
 *     y0 = Iridium Coils (18045) — the 144;</li>
 * <li>OCTAGONS[2] (:114-124): y-2/y+2 = 18003 ONLY_ITEM_FLUID, y-1/y+1 = 18045,
 *     y0 = Stainless Steel Walls (18002) — the 36.</li>
 * </ul>
 * The material ledger (:195-197): 144 Ir coils + 576 WSteel walls + 50 vents + 36 SS
 * walls + 53 GalvSteel walls + 27 PU.
 *
 * <p><b>Why hand-written (no bound pattern)</b>: the design-5/6 ring is
 * mActive-CONDITIONAL and the PU core is an N-of-M-per-type quota — the declarative
 * pattern API does not express either (GTMultiBlockPattern class doc, quota case ②), so
 * {@link #getStructurePattern()} stays the interface default null (the Massfab census
 * class). The walk is the upstream-verbatim loop via
 * {@link ITileEntityMultiBlockController.Util#checkAndSetTarget}; the unloaded guard is
 * the checker's per-cell superset of the upstream :47 four-corner {@code blockExists}
 * probe (keep the last verdict). {@link #refreshStructureOnActiveStateChange()} returns
 * true (:241) — the base onTickCheck hook (:105) re-runs the full walk on an
 * active/running flip, which is what rewrites the ring designs 5↔6.
 *
 * <p><b>The energy face</b> (MultiTileEntityBasicMachine.java:489-519): the controller
 * body is a TU capacitor sink (the {@link #doEnergyInjection} :501-506 arm over the
 * window 1..16384; the :493-496 overcharge arm REFUSES the packet — the declared
 * no-explosion narrowing, the Massfab form). The glass ring ADVERTISES LU acceptance
 * (isEnergyType :510 second arm) through the part relay — but the injection it would
 * charge is the ignition ledger, and that ledger is NOT ported (the declared waiver,
 * below), so LU packets fall through to the :501 type check and are refused
 * (0 accepted). UPSTREAM the same injection pays the start-LU ledger (:497-500); the
 * gateless port simply has no use for it. The generator emission rides
 * {@link #doOutputEnergy()} (:233-236): the raw 8192-EU packet pushed
 * ({@code insertEnergyInto}) at each of the four orthogonal ±10 offsets of the core
 * level, first accepting receiver wins — the remote launch seam.
 *
 * <p><b>Port deviation — the ignition gate is NOT implemented (declared waiver, the
 * S31-7 final qualification)</b>: <b>upstream = full-run ignition gating</b> — the
 * Loader_MultiTileEntities.java:1242 row carries {@code NBT_SPECIAL_IS_START_ENERGY, T},
 * the flag is SUPPLIED through readFromNBT2 (:112-124, the registration-config injection
 * route shared with NBT_INPUT/NBT_RECIPEMAP) → the :755 write IS reachable (on recipe
 * switch / non-active: {@code mChargeRequirement = mSpecialValue}) → the :809 progress
 * gate CLOSES until paid → the glass-ring :497-500 LU decrement feeds it. D-D:
 * 730×8192×16 ≈ 95.6M LU per arm. <b>port = declared waiver</b> — the gate is live
 * upstream and simply absent here: installing it with no LU economy makes a dead
 * machine. A future gate card (after the pool-E LU production chain lands) ports the
 * {@code mSpecialIsStartEnergy} field + the :755/:809/:497 three arms (a one-line
 * constructor boolean; the 18 rows' startLU data is already in place).
 *
 * <p><b>The IO face</b> (:223-230/:238-239): item and fluid auto-out/in targets are all
 * null upstream — the reactor has NO auto-IO; fluids park in the map-shaped tank bank
 * (RM.java:146 fluids 2/6/0 → 2 input + 6 output tanks, the readFromNBT2 :161 size) and
 * are hand/pipe serviced through the part faces. The port folds the same shape onto the
 * machine inventory + tank bank (the base NBT carries input_tank_N / output_tank_N).
 *
 * <p><b>The controller crafting row</b> "FFF"/"FMF"/"FFF" ('M' = the item(18003)
 * Tungstensteel Wall, 'F' = IL.FIELD_GENERATORS[5], Loader:1242 tail) is CUT — the 'F'
 * item family has no port identity (the W3 absent-input pool, the implosion/graagg/
 * massfab CUT precedent); the ledger lives on the GTMultiBlocks registration comment.
 *
 * <p>GUI face: none — the controller block carries no use-face (the W2 menu-null form,
 * the massfab shape); the recipe walk is RCON/data-visible through the block NBT.
 */
public class TileEntityFusionReactor extends TileEntityBase10MultiBlockMachine {

	/** The emitted type (upstream :100 mEnergyTypeEmitted — the :1242 NBT_ENERGY_EMITTED column). Registration config, not persisted. */
	public final TagData mEnergyTypeEmitted = TD.Energy.EU;

	/** The second accepted type (upstream :98 mEnergyTypeCharged — the :1242 NBT_ENERGY_ACCEPTED_2 column, the glass ring). Registration config, not persisted. */
	public final TagData mEnergyTypeCharged = TD.Energy.LU;

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the massfab precedent). */
	public TileEntityFusionReactor(BlockPos aPos, BlockState aState) {
		this(GTMultiBlocks.FUSION_REACTOR_BE.get(), aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public TileEntityFusionReactor(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		// the :1242 row — the explicit window 1..16384, no parallel/cheap-oc keys, no
		// NBT_NEEDS_IGNITION key. Not persisted (the contract).
		applyEnergyRowSpec(new EnergyRowSpec(8192L, 1L, 16384L, null, null, null));
		mRequiresIgnition = false;
		// the :1242 energy-type columns — the BASE fields assigned (fields do not
		// virtual-dispatch; the massfab constructor form)
		mEnergyTypeAccepted = TD.Energy.TU;
		// RM.java:146 fluids 2/6/0 → TWO input tanks and SIX output tanks (the :161
		// readFromNBT2 map-size re-point; the base NBT loops cover the extra tanks)
		mTanksInput = new FluidTankGT[] {new FluidTankGT(), new FluidTankGT()};
		mTanksOutput = new FluidTankGT[] {new FluidTankGT(), new FluidTankGT(), new FluidTankGT(), new FluidTankGT(), new FluidTankGT(), new FluidTankGT()};
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_fusion_reactor";
	}

	/** The Galvanized Steel wall (upstream part id 18008, Loader :1143 family). */
	protected Block getWallBlock() {
		return GTMultiBlocks.anyPartBlock("machine_wall_galvanized_steel");
	}

	/** The Tungstensteel 'glass' wall (upstream part id 18003, the design 0/2/5/6 carrier). */
	protected Block getGlassBlock() {
		return GTMultiBlocks.anyPartBlock("machine_wall_tungstensteel");
	}

	/** The Stainless Steel wall (upstream part id 18002). */
	protected Block getSsBlock() {
		return GTMultiBlocks.anyPartBlock("machine_wall_stainless_steel");
	}

	/** The Large Iridium Coil (upstream part id 18045). */
	protected Block getCoilBlock() {
		return GTMultiBlocks.anyPartBlock("large_iridium_coil");
	}

	/** The Ventilation Unit (upstream part id 18299). */
	protected Block getVentBlock() {
		return GTMultiBlocks.anyPartBlock("ventilation_unit");
	}

	/** The Versatile Quadcore PU (upstream part id 18200). */
	protected Block getVersatileBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_versatile");
	}

	/** The Logic Quadcore PU (upstream part id 18201). */
	protected Block getLogicBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_logic");
	}

	/** The Control Quadcore PU (upstream part id 18202). */
	protected Block getControlBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_control");
	}

	/** The lazy recipe map (the base's COKE_OVEN default overridden — the massfab form). */
	@Override
	public RecipeMap recipes() {
		RecipeMap tMap = mRecipes;
		if (tMap == null) {
			tMap = GT6RecipeMaps.FUSION;
			mRecipes = tMap;
		}
		return tMap;
	}

	// ---------------------------------------------------------------------------
	// the structure (:47-126 verbatim, hand-written — the active-conditional design and
	// the PU quota case, see the class doc)
	// ---------------------------------------------------------------------------

	/** The core-centre coordinate (upstream :46/:48 getOffsetXN/ZN(mFacing, 2)): the anchor IS -OFF, doubled. Pure so the tests drive it. */
	static int core(int aCoord, int aAnchor) {
		return aCoord + 2 * aAnchor;
	}

	/**
	 * The core-shell cell kind (upstream :55/:65): d² &lt; 4 = the PU cross (-1 = the PU
	 * fallback chain decides), d² &gt; 6 or the j==0 axis tips = the GalvSteel wall (0),
	 * otherwise the vent shell (1). Pure so the tests drive the geometry.
	 */
	static int shellKind(int i, int j, int k) {
		int d2 = i * i + j * j + k * k;
		if (d2 < 4) return -1;
		if (d2 > 6 || (j == 0 && (((i == -2 || i == 2) && k == 0) || ((k == -2 || k == 2) && i == 0)))) return 0;
		return 1;
	}

	/** The glass-ring edge-midpoint test (upstream :96): the four orthogonal ring tips are the design-2 OUT faces. */
	static boolean isRingOutTip(int i, int j) {
		return (i == 9 && (j == 0 || j == 18)) || (j == 9 && (i == 0 || i == 18));
	}

	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :128 (the upstream mStructureOkay fall-through)
		Level tLevel = getLevel();
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing); // == -OFF[facing]
		int tCX = core(tPos.getX(), tAnchor[0]), tCY = tPos.getY(), tCZ = core(tPos.getZ(), tAnchor[2]);

		// the unloaded guard — the per-cell superset of the upstream :47 four-corner
		// blockExists probe (the checker form): one not-loaded cell keeps the last verdict
		for (int dy = -2; dy <= 2; dy++) for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
			if (!tLevel.isLoaded(new BlockPos(tCX + dx, tCY + dy, tCZ + dz))) return mStructureOkay; // :47/:128
		}

		boolean tSuccess = true; // :50
		int tVersatile = 3, tLogic = 12, tControl = 12; // :52

		Block tWall = getWallBlock(), tGlass = getGlassBlock(), tSs = getSsBlock();
		Block tCoil = getCoilBlock(), tVent = getVentBlock();
		Block tPUV = getVersatileBlock(), tPUL = getLogicBlock(), tPUC = getControlBlock();

		// the 5x5x5 core (:54-70)
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			int tKind = shellKind(i, j, k);
			int tWX = tCX + i, tWY = tCY + j, tWZ = tCZ + k;
			if (tKind == -1) {
				// :56-64 — the PU fallback chain + the quota counting
				if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tPUV, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					tVersatile--;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tPUL, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					tLogic--;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tPUC, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					tControl--;
				} else {
					tSuccess = false; // :63
				}
			} else if (tKind == 0) {
				// :65-66 — the GalvSteel wall (the shell corners + the axis tips)
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tWall, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			} else {
				// :68 — the ventilation shell
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tWX, tWY, tWZ, tVent, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}
		}
		if (tVersatile > 0 || tLogic > 0 || tControl > 0) tSuccess = false; // :72

		// the four orthogonal extension arms (:74-89) — skip the facing direction itself
		if (mFacing != 4) { // SIDE_X_NEG
			if (!check(tCX - 3, tCY, tCZ, tWall, aCoordinates, aPlayer, aInventory) || !check(tCX - 4, tCY, tCZ, tWall, aCoordinates, aPlayer, aInventory)) tSuccess = false;
		}
		if (mFacing != 5) { // SIDE_X_POS
			if (!check(tCX + 3, tCY, tCZ, tWall, aCoordinates, aPlayer, aInventory) || !check(tCX + 4, tCY, tCZ, tWall, aCoordinates, aPlayer, aInventory)) tSuccess = false;
		}
		if (mFacing != 2) { // SIDE_Z_NEG
			if (!check(tCX, tCY, tCZ - 3, tWall, aCoordinates, aPlayer, aInventory) || !check(tCX, tCY, tCZ - 4, tWall, aCoordinates, aPlayer, aInventory)) tSuccess = false;
		}
		if (mFacing != 3) { // SIDE_Z_POS
			if (!check(tCX, tCY, tCZ + 3, tWall, aCoordinates, aPlayer, aInventory) || !check(tCX, tCY, tCZ + 4, tWall, aCoordinates, aPlayer, aInventory)) tSuccess = false;
		}

		// the OCTAGONS ring walk (:91-125) — the corner re-based to the ring origin
		int tRX = tCX - 9, tRZ = tCZ - 9; // :91
		for (int i = 0; i < 19; i++) for (int j = 0; j < 19; j++) {
			int tIX = tRX + i, tIZ = tRZ + j;
			if (OCTAGONS[0][i][j]) {
				// :95 — y-1 wall (item/fluid)
				if (!check(tIX, tCY - 1, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				// :96-100 — y0 the ring: the four orthogonal tips OUT (design 2), the rest IN (design 5, active 6)
				if (isRingOutTip(i, j)) {
					if (!check(tIX, tCY, tIZ, tGlass, 2, MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				} else {
					if (!check(tIX, tCY, tIZ, tGlass, mActive ? 6 : 5, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				}
				// :101 — y+1 wall (item/fluid)
				if (!check(tIX, tCY + 1, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}
			if (OCTAGONS[1][i][j]) {
				// :104-112 — the coil ring: y-2/y+2 walls (item/fluid), y-1/y+1 walls (nothing), y0 the Ir coil
				if (!check(tIX, tCY - 2, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY - 1, tIZ, tGlass, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY, tIZ, tCoil, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY + 1, tIZ, tGlass, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY + 2, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}
			if (OCTAGONS[2][i][j]) {
				// :115-123 — the SS wall ring: y-2/y+2 walls (item/fluid), y-1/y+1 coils, y0 the SS wall
				if (!check(tIX, tCY - 2, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY - 1, tIZ, tCoil, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY, tIZ, tSs, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY + 1, tIZ, tCoil, 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
				if (!check(tIX, tCY + 2, tIZ, tGlass, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}
		}
		return tSuccess; // :126
	}

	/** The design-0 NOTHING checkAndSetTarget one-liner (the arm cells, :75-88). */
	private boolean check(int aX, int aY, int aZ, Block aBlock, @Nullable BlockPos aClicked, @Nullable Player aPlayer, @Nullable Container aInventory) {
		return ITileEntityMultiBlockController.Util.checkAndSetTarget(this, aX, aY, aZ, aBlock, 0, MultiBlockPartBlockEntity.NOTHING, aClicked, aPlayer, aInventory);
	}

	/** The design/mode-full form (the ring cells). */
	private boolean check(int aX, int aY, int aZ, Block aBlock, int aDesign, int aMode, @Nullable BlockPos aClicked, @Nullable Player aPlayer, @Nullable Container aInventory) {
		return ITileEntityMultiBlockController.Util.checkAndSetTarget(this, aX, aY, aZ, aBlock, aDesign, aMode, aClicked, aPlayer, aInventory);
	}

	/** Upstream :241 — the base onTickCheck hook re-runs the walk on an active flip, rewriting the ring designs 5↔6. */
	@Override
	public boolean refreshStructureOnActiveStateChange() {
		return true;
	}

	/** Upstream :217-220 — the box over the whole machine, core centre ±9 horizontal, y-2..y+3 (the upstream +3 slop kept verbatim). */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing);
		int tCX = core(tPos.getX(), tAnchor[0]), tCY = tPos.getY() - 2, tCZ = core(tPos.getZ(), tAnchor[2]);
		return aX >= tCX - 9 && aY >= tCY && aZ >= tCZ - 9 && aX <= tCX + 9 && aY <= tCY + 5 && aZ <= tCZ + 9;
	}

	/**
	 * The three 19x19 octagon masks (upstream :131-191 verbatim): [0] the outer glass
	 * ring, [1] the Ir-coil ring, [2] the SS wall ring.
	 */
	public static final boolean[][][] OCTAGONS = {{
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,false,false},
		{false,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,false},
		{false,false,false,true,false,false,false,true,true,true,true,true,false,false,false,true,false,false,false},
		{false,false,true,false,false,false,true,false,false,false,false,false,true,false,false,false,true,false,false},
		{false,true,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,true,false},
		{true,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,true},
		{true,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,true},
		{true,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,true},
		{true,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,true},
		{true,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,true},
		{false,true,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,true,false},
		{false,false,true,false,false,false,true,false,false,false,false,false,true,false,false,false,true,false,false},
		{false,false,false,true,false,false,false,true,true,true,true,true,false,false,false,true,false,false,false},
		{false,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,false},
		{false,false,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
	}, {
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,true,false,true,true,true,true,true,false,true,false,false,false,false,false},
		{false,false,false,false,true,false,true,false,false,false,false,false,true,false,true,false,false,false,false},
		{false,false,false,true,false,true,false,false,false,false,false,false,false,true,false,true,false,false,false},
		{false,false,true,false,true,false,false,false,false,false,false,false,false,false,true,false,true,false,false},
		{false,true,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,true,false},
		{false,true,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,true,false},
		{false,true,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,true,false},
		{false,true,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,true,false},
		{false,true,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,true,false},
		{false,false,true,false,true,false,false,false,false,false,false,false,false,false,true,false,true,false,false},
		{false,false,false,true,false,true,false,false,false,false,false,false,false,true,false,true,false,false,false},
		{false,false,false,false,true,false,true,false,false,false,false,false,true,false,true,false,false,false,false},
		{false,false,false,false,false,true,false,true,true,true,true,true,false,true,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
	}, {
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,false,false},
		{false,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,false},
		{false,false,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false},
		{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false},
		{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false},
		{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false},
		{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false},
		{false,false,true,false,false,false,false,false,false,false,false,false,false,false,false,false,true,false,false},
		{false,false,false,true,false,false,false,false,false,false,false,false,false,false,false,true,false,false,false},
		{false,false,false,false,true,false,false,false,false,false,false,false,false,false,true,false,false,false,false},
		{false,false,false,false,false,true,false,false,false,false,false,false,false,true,false,false,false,false,false},
		{false,false,false,false,false,false,true,false,false,false,false,false,true,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,true,true,true,true,true,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
		{false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false},
	}};

	// ---------------------------------------------------------------------------
	// the IO face (:223-230/:238-239) — all four auto targets are null upstream
	// ---------------------------------------------------------------------------

	@Override
	@Nullable
	protected net.minecraftforge.fluids.capability.IFluidHandler getFluidOutputTarget(Fluid aOutput) {
		return null; // :223-225 — no auto-out; the output tanks are hand/pipe serviced
	}

	// ---------------------------------------------------------------------------
	// the energy face (MultiTileEntityBasicMachine.java:489-519)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting ? aEnergyType == mEnergyTypeEmitted
				: aEnergyType == mEnergyTypeAccepted || aEnergyType == mEnergyTypeCharged; // :510
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :519
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mStopped) return 0; // :490
		aSize = Math.abs(aSize); // :492
		// the :493-496 overcharge arm REFUSES the packet (the declared no-explosion
		// narrowing, the massfab form) — after the type gate so the glass-ring LU face
		// keeps its refusal semantics (upstream the :497-500 charged arm would bank the
		// packet against the start-LU ledger; the gateless port has no ledger, so LU falls
		// to the :501 type check — refused, 0 accepted)
		if (aEnergyType != mEnergyTypeAccepted) return 0; // :501
		if (aSize > mInputMax) return 0;
		long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput / aSize) + (tInput % aSize != 0 ? 1 : 0)); // :503
		if (aDoInject) mEnergy += tConsumed * aSize; // :504
		return tConsumed; // :504
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return mInputMin; // :513
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mInput; // :514
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return mInputMax; // :515
	}

	// ---------------------------------------------------------------------------
	// the generator emission (:233-236) — the ±10 remote launch seam
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :233-236 verbatim over the port coordinate seams: from the CORE centre
	 * (2 cells behind the front face), push one {@link #mOutputEnergy}-sized
	 * {@link #mEnergyTypeEmitted} packet into the tile entity at each orthogonal ±10
	 * offset (the {@code OFFX[tSide]*10} arithmetic), first accepting receiver wins.
	 */
	@Override
	public void doOutputEnergy() {
		if (!hasLevel() || isClientSide()) return;
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing);
		int tCX = core(tPos.getX(), tAnchor[0]), tCY = tPos.getY(), tCZ = core(tPos.getZ(), tAnchor[2]);
		for (byte tSide : ALL_SIDES_HORIZONTAL) {
			Direction tDir = Direction.from3DDataValue(tSide);
			BlockEntity tTarget = getLevel().getBlockEntity(new BlockPos(tCX + tDir.getStepX() * 10, tCY, tCZ + tDir.getStepZ() * 10));
			if (tTarget == null) continue; // the WD.te null guard (:235, OPOS unloaded arm)
			byte tSideInto = (byte) tDir.getOpposite().get3DDataValue(); // the receiving face (the upstream DelegatorTileEntity side)
			if (ITileEntityEnergy.Util.insertEnergyInto(mEnergyTypeEmitted, tSideInto, mOutputEnergy, 1, this, tTarget) > 0) return; // :235
		}
	}

	/** The four horizontal sides (upstream CS.java ALL_SIDES_HORIZONTAL, the Direction 3D order). */
	public static final byte[] ALL_SIDES_HORIZONTAL = {2, 3, 4, 5};
}
