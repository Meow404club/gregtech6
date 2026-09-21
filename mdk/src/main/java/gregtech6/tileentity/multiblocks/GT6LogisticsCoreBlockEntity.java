package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.covers.CoverData;
import gregtech6.covers.ICover;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.covers.covers.logistics.AbstractCoverLogisticsDisplay;
import gregtech6.covers.covers.logistics.AbstractCoverLogisticsFiltered;
import gregtech6.covers.covers.logistics.AbstractCoverLogisticsFluid;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUControl;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUConversion;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPULogic;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPUStorage;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidExport;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidImport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericDump;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericExport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericImport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericStorage;
import gregtech6.covers.covers.logistics.CoverLogisticsItemExport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemImport;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.logistics.ITileEntityLogisticsSemiFilteredItem;
import gregtech6.tileentity.logistics.ITileEntityLogisticsStorage;
import gregtech6.util.GTItemMover;

/**
 * The Logistics Core multiblock controller (task p32-logistics-lv3) — the 1.20.1 port of
 * gregtech/tileentity/multiblocks/MultiTileEntityLogisticsCore.java over
 * {@link TileEntityBase10MultiBlockBase} (Loader_MultiTileEntities.java:1281: meta 17997,
 * item 17101, "Logistics Core", MT.SteelGalvanized, NBT_HARDNESS 6.0F == NBT_RESISTANCE
 * 6.0F, NBT_TEXTURE "logisticscore").
 *
 * <p><b>The structure</b> (upstream checkStructure2 :109-147): a 5x5x5 whose centre sits
 * 2 cells behind the front face ({@code getOffsetXN(mFacing, 2)}, the
 * {@link GTMultiBlockPattern#anchorOffset} doubled — "Main centered at any Side facing
 * outwards", :153). The inner 3x3x27 — every cell with squared offset distance &lt; 4,
 * i.e. the 27 cells of |offset| ≤ 1 — takes CPU units: the Versatile Quadcore (18200)
 * counts +1 to all four pools, each specialist (18201 Logic / 18202 Control / 18203
 * Storage / 18204 Conversion) counts +4 to its own (:117-131); any CPU cell may be a
 * plain Galvanized Steel Wall instead (:132, "someone's a cheapskate"). The six 3x3 faces
 * (squared distance 4..6) are the 53 Ventilation Units (18299 — 54 minus the controller's
 * own face-centre cell), EVERYTHING-else is the 44-cell wall frame (18008). Outer walls
 * run {@link MultiBlockPartBlockEntity#ONLY_LOGISTICS} &amp;
 * {@link MultiBlockPartBlockEntity#ONLY_ENERGY_IN} (:138), vents
 * {@link MultiBlockPartBlockEntity#ONLY_LOGISTICS} (:140), CPU cells
 * {@link MultiBlockPartBlockEntity#NOTHING}. Formed requires ≥ 1 of each processor pool
 * (:144), the Storage pool caps at {@link #MAX_STORAGE_CPU_COUNT} (:143).
 *
 * <p><b>The network</b> (upstream onServerTickPre :207-506): no persistent network object
 * — every {@code SYNC_SECOND} (the port's {@code mTimer % 20 == 0}) the core rebuilds the
 * world's logistics membership: the seed is the centre 5x5x5 cube (:270-273), the walk is
 * a BFS through {@link ITileEntityLogistics#canLogistics(byte)} sides only (:437-444),
 * bounded by {@code mCPU_Control + 2} cells from the centre (a CUBIC AoE — the per-cell
 * Chebyshev distance, :438-439), same-level only (:437). Scanned members that are
 * {@link ITileEntityLogisticsStorage}s register into the three-tier fluid/item lists
 * (:277-294); the semi-filtered answers and the storage item filters fold into the
 * network-wide protected set {@code tFilteredFor} (:278-282).
 *
 * <p><b>The routing</b> (:451-500): each Logic CPU performs at most ONE productive
 * operation, in priority order — (1) the Export×Import pairings over the six-row export
 * table × the three-row import table (:455-469; storage tiers double as export sources in
 * the paired table), (2) defragmentation generic→Filtered then generic→Semi (:473-476),
 * (3) Dump: generic item storage → the dump targets, excluded for every item in
 * {@code tFilteredFor} (:479-494). An operation that moved nothing is refunded (:498).
 * Every move charges EU: items cost 1 EU each, fluids 250 L per EU (:525/:565/:589).
 *
 * <p><b>The energy face</b> (:680-699): an EU capacitor sink, packet window 256/512/1024,
 * capacity {@code 128 + Logic * 256 * Conversion} (:699), the per-second startup gate
 * {@code 128 + Logic * 64 * Conversion} (:216). The idle draw is
 * {@code 20 + Σ CPUs} EU per second (:504). The oversize-packet EXPLODE arm (:684) is the
 * declared no-explosion narrowing (the massfab precedent): the packet is refused.
 *
 * <p><b>Port trims (declared):</b> the upstream {@code ITileEntityEnergyDataCapacitor}
 * face (:693/:698-701) has no port counterpart (the energy STORED is command-visible);
 * the 108 {@code FluidTankGT} buffer tanks (:70/:705-708 — fillable=null, drainable=null,
 * a passive ballast upstream) and the {@code IMultiBlockFluidHandler} re-exposure ride
 * with the fluid-IO cards that would consume them; the cover-bus registration arm
 * (:297-435 — the 12 logistics covers) is the declared cover-card slice, which leaves the
 * phase-1/2 import and export lists EMPTY until covers land: the coverless network routes
 * through the defragmentation arms and dump arm only, exactly as the upstream lists
 * dictate. The {@code CoverLogisticsDisplayCPU*} value arms (:300-319) go with the covers.
 */
public class GT6LogisticsCoreBlockEntity extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy, ITileEntityLogistics {

	/** Upstream :64. */
	public static final int MAX_STORAGE_CPU_COUNT = 108;

	// upstream :66-69, the NBT keys verbatim (:77-84/:96-103)
	public long mEnergy = 0;
	public int mCPU_Logic = 0, mCPU_Control = 0, mCPU_Storage = 0, mCPU_Conversion = 0;
	public int oCPU_Logic = 0, oCPU_Control = 0, oCPU_Storage = 0, oCPU_Conversion = 0;

	public static final TagData mEnergyTypeAccepted = TD.Energy.EU;

	public static final String NBT_ENERGY = "energy";
	public static final String NBT_CPU_LOGIC = "gt.cpu.logic", NBT_CPU_CONTROL = "gt.cpu.control",
			NBT_CPU_STORAGE = "gt.cpu.storage", NBT_CPU_CONVERSION = "gt.cpu.conversion";
	public static final String NBT_CPU_LOGIC_USED = "gt.cpu.logic.used", NBT_CPU_CONTROL_USED = "gt.cpu.control.used",
			NBT_CPU_STORAGE_USED = "gt.cpu.storage.used", NBT_CPU_CONVERSION_USED = "gt.cpu.conversion.used";

	// the last scan's report (the /gt6logistics core stat surface — a port addition, upstream
	// answers right-click chat :662-674; the port core carries no use-face, the W2 menu-null form)
	public final int[] mReportFluid = new int[3], mReportItem = new int[3];
	public int mReportFilters = 0;
	/** The scanned member count of the last scan (tanks + wires + cores — the BFS reach observable). */
	public int mReportMembers = 0;
	public long mMovedLast = 0, mMovedTotal = 0;
	/** The EU charged for the last scan's moves (the :525/:565/:485 deductions, idle-draw excluded — the deterministic live EU face). */
	public long mCostLast = 0, mCostTotal = 0;

	/** The network-wide protected items of the last scan (upstream {@code tFilteredFor}, :219). */
	protected final Set<ItemStack> mFilteredFor = Collections.newSetFromMap(new IdentityHashMap<>());

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the massfab precedent). */
	public GT6LogisticsCoreBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	public GT6LogisticsCoreBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GT6Logistics.LOGISTICS_CORE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_logistics_core";
	}

	// ---------------------------------------------------------------------------
	// NBT (:72-106)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		mEnergy = aNBT.getLong(NBT_ENERGY);
		mCPU_Logic = aNBT.getInt(NBT_CPU_LOGIC);
		mCPU_Control = aNBT.getInt(NBT_CPU_CONTROL);
		mCPU_Storage = aNBT.getInt(NBT_CPU_STORAGE);
		mCPU_Conversion = aNBT.getInt(NBT_CPU_CONVERSION);
		oCPU_Logic = aNBT.getInt(NBT_CPU_LOGIC_USED);
		oCPU_Control = aNBT.getInt(NBT_CPU_CONTROL_USED);
		oCPU_Storage = aNBT.getInt(NBT_CPU_STORAGE_USED);
		oCPU_Conversion = aNBT.getInt(NBT_CPU_CONVERSION_USED);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putInt(NBT_CPU_LOGIC, mCPU_Logic);
		aNBT.putInt(NBT_CPU_CONTROL, mCPU_Control);
		aNBT.putInt(NBT_CPU_STORAGE, mCPU_Storage);
		aNBT.putInt(NBT_CPU_CONVERSION, mCPU_Conversion);
		aNBT.putInt(NBT_CPU_LOGIC_USED, oCPU_Logic);
		aNBT.putInt(NBT_CPU_CONTROL_USED, oCPU_Control);
		aNBT.putInt(NBT_CPU_STORAGE_USED, oCPU_Storage);
		aNBT.putInt(NBT_CPU_CONVERSION_USED, oCPU_Conversion);
	}

	// ---------------------------------------------------------------------------
	// the structure (:109-183)
	// ---------------------------------------------------------------------------

	/** The wall block (upstream part id 18008, the METAL_WALL_ROWS row). */
	protected Block getWallBlock() {
		return GTMultiBlocks.anyPartBlock("machine_wall_galvanized_steel");
	}

	/** The vent block (upstream part id 18299). */
	protected Block getVentBlock() {
		return GTMultiBlocks.anyPartBlock("ventilation_unit");
	}

	/** The versatile PU (upstream part id 18200). */
	protected Block getVersatileBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_versatile");
	}

	/** The Logic PU (upstream part id 18201). */
	protected Block getLogicBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_logic");
	}

	/** The Control PU (upstream part id 18202). */
	protected Block getControlBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_control");
	}

	/** The Storage PU (upstream part id 18203). */
	protected Block getStorageBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_storage");
	}

	/** The Conversion PU (upstream part id 18204). */
	protected Block getConversionBlock() {
		return GTMultiBlocks.anyPartBlock("processor_unit_conversion");
	}

	/** The structure centre, 2 cells behind the front face (upstream :110 getOffsetXN/Y/ZN arithmetic). */
	public BlockPos structureCenter() {
		BlockPos tPos = getBlockPos();
		int[] tAnchor = GTMultiBlockPattern.anchorOffset(mFacing); // == -OFF[facing]
		return new BlockPos(tPos.getX() + 2 * tAnchor[0], tPos.getY() + 2 * tAnchor[1], tPos.getZ() + 2 * tAnchor[2]);
	}

	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay;
		Level tLevel = getLevel();
		BlockPos tCenter = structureCenter();

		// the unloaded guard — the per-cell superset of the upstream :111 four-corner blockExists
		// probe (the massfab checker form): one not-loaded cell keeps the last verdict.
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			if (!tLevel.isLoaded(tCenter.offset(i, j, k))) return mStructureOkay;
		}

		boolean tSuccess = true; // :112
		mCPU_Logic = 0; mCPU_Control = 0; mCPU_Storage = 0; mCPU_Conversion = 0; // :113-116
		Block tWall = getWallBlock(), tVent = getVentBlock();
		Block tVersatile = getVersatileBlock(), tLogic = getLogicBlock(), tControl = getControlBlock(),
				tStorage = getStorageBlock(), tConversion = getConversionBlock();

		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			BlockPos tPos = tCenter.offset(i, j, k);
			int tDistSq = i * i + j * j + k * k;
			if (tDistSq < 4) {
				// :117-136 — the 27 CPU cells: versatile → +1 each, specialists → +4 their own,
				// a wall is a legal substitute ("someone's a cheapskate" :133)
				if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tVersatile, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					mCPU_Logic++; mCPU_Control++; mCPU_Storage++; mCPU_Conversion++;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tLogic, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					mCPU_Logic += 4;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tControl, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					mCPU_Control += 4;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tStorage, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					mCPU_Storage += 4;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tConversion, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					mCPU_Conversion += 4;
				} else if (ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tWall, 0,
						MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) {
					// :132-133 — the cheapskate arm
				} else {
					tSuccess = false;
				}
			} else if (tDistSq > 6) {
				// :137-138 — the 44 wall cells, energy in + logistics only
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tWall, 0,
						MultiBlockPartBlockEntity.ONLY_LOGISTICS & MultiBlockPartBlockEntity.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			} else {
				// :139-140 — the 53 vent cells, logistics only
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tPos.getX(), tPos.getY(), tPos.getZ(), tVent, 0,
						MultiBlockPartBlockEntity.ONLY_LOGISTICS, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}
		}
		if (mCPU_Storage > MAX_STORAGE_CPU_COUNT) mCPU_Storage = MAX_STORAGE_CPU_COUNT; // :143
		return tSuccess && mCPU_Logic > 0 && mCPU_Control > 0 && mCPU_Storage > 0 && mCPU_Conversion > 0; // :144
	}

	/** Upstream :180-183 — the box ±2 around the centre. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		BlockPos tCenter = structureCenter();
		return aX >= tCenter.getX() - 2 && aY >= tCenter.getY() - 2 && aZ >= tCenter.getZ() - 2
				&& aX <= tCenter.getX() + 2 && aY <= tCenter.getY() + 2 && aZ <= tCenter.getZ() + 2;
	}

	// ---------------------------------------------------------------------------
	// the network (:205-506)
	// ---------------------------------------------------------------------------

	/** Upstream :209 SYNC_SECOND — the port's every-20-ticks form (the server tick path). */
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide || mTimer % 20 != 0) return;
		oCPU_Logic = 0; oCPU_Control = 0; oCPU_Storage = 0; oCPU_Conversion = 0; // :211-214
		mMovedLast = 0;
		if (checkStructure(false) && mEnergy >= 128L + mCPU_Logic * 64L * mCPU_Conversion) { // :216
			scanAndRoute();
		}
		mEnergy -= 20 + mCPU_Logic + mCPU_Control + mCPU_Storage + mCPU_Conversion; // :504
		if (mEnergy < 0) mEnergy = 0;
		setChanged();
	}

	/** The move-cost accounting (:525/:565/:485) — the idle draw never enters this ledger. */
	void chargeEnergy(long aCost) {
		mEnergy -= aCost;
		mCostLast += aCost;
		mCostTotal += aCost;
	}

	/** The per-scan pair of routing lists: [0] = fluids, [1] = stacks. */
	@SuppressWarnings("unchecked")
	private List<LogisticsData>[] pair(List<LogisticsData> aFluids, List<LogisticsData> aStacks) {
		return new List[] {aFluids, aStacks};
	}

	/**
	 * Upstream :217-500 — the scan, the tier registration and the routing loop. Package-private
	 * so the tests drive it without the timer.
	 */
	void scanAndRoute() {
		Level tLevel = getLevel();
		if (tLevel == null) return;
		// the :211-214 reset — the onTick arm already ran it before the gate; the head copy
		// keeps the direct-drive (test) callers on the same reset-then-route contract.
		oCPU_Logic = 0; oCPU_Control = 0; oCPU_Storage = 0; oCPU_Conversion = 0;
		mMovedLast = 0;
		BlockPos tCenter = structureCenter();

		final List<LogisticsData>
		  tStackImportsGeneric  = new ArrayList<>()
		, tStackImportsSemi     = new ArrayList<>()
		, tStackImportsFiltered = new ArrayList<>()
		, tStackExportsGeneric  = new ArrayList<>()
		, tStackExportsSemi     = new ArrayList<>()
		, tStackExportsFiltered = new ArrayList<>()
		, tStackStorageGeneric  = new ArrayList<>()
		, tStackStorageSemi     = new ArrayList<>()
		, tStackStorageFiltered = new ArrayList<>()
		, tStackDumps           = new ArrayList<>()
		, tFluidImportsGeneric  = new ArrayList<>()
		, tFluidImportsSemi     = new ArrayList<>()
		, tFluidImportsFiltered = new ArrayList<>()
		, tFluidExportsGeneric  = new ArrayList<>()
		, tFluidExportsSemi     = new ArrayList<>()
		, tFluidExportsFiltered = new ArrayList<>()
		, tFluidStorageGeneric  = new ArrayList<>()
		, tFluidStorageSemi     = new ArrayList<>()
		, tFluidStorageFiltered = new ArrayList<>()
		;

		// :242-264 — the six-row export table (export covers + storage tiers, the sources) against
		// the three-row import table (import covers, the targets), then the mirrored halves
		// (export covers against the storage tiers as targets).
		final List<LogisticsData>[]
		  tExports1[] = new List[][] {
		  pair(tFluidExportsFiltered, tStackExportsFiltered)
		, pair(tFluidExportsSemi    , tStackExportsSemi    )
		, pair(tFluidExportsGeneric , tStackExportsGeneric )
		, pair(tFluidStorageFiltered, tStackStorageFiltered)
		, pair(tFluidStorageSemi    , tStackStorageSemi    )
		, pair(tFluidStorageGeneric , tStackStorageGeneric )
		}
		, tExports2[] = new List[][] {
		  pair(tFluidExportsFiltered, tStackExportsFiltered)
		, pair(tFluidExportsSemi    , tStackExportsSemi    )
		, pair(tFluidExportsGeneric , tStackExportsGeneric )
		}
		, tImports1[] = new List[][] {
		  pair(tFluidImportsGeneric , tStackImportsGeneric )
		, pair(tFluidImportsSemi    , tStackImportsSemi    )
		, pair(tFluidImportsFiltered, tStackImportsFiltered)
		}
		, tImports2[] = new List[][] {
		  pair(tFluidStorageGeneric , tStackStorageGeneric )
		, pair(tFluidStorageSemi    , tStackStorageSemi    )
		, pair(tFluidStorageFiltered, tStackStorageFiltered)
		}
		;

		// :267-268 — identity-keyed (BlockEntity equals/hashCode is not identity)
		Set<BlockEntity> tScanned = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<ITileEntityLogistics> tScanning = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<ITileEntityLogistics> tScanningNext = Collections.newSetFromMap(new IdentityHashMap<>());

		int tMembers = 0;

		// :270-273 — the seed: the centre 5x5x5 cube
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) for (int k = -2; k <= 2; k++) {
			BlockPos tPos = tCenter.offset(i, j, k);
			if (!tLevel.isLoaded(tPos)) continue;
			BlockEntity tTileEntity = tLevel.getBlockEntity(tPos);
			if (tTileEntity != null && tScanned.add(tTileEntity) && tTileEntity instanceof ITileEntityLogistics) {
				tScanning.add((ITileEntityLogistics)tTileEntity);
				tMembers++;
			}
		}

		mFilteredFor.clear();

		while (!tScanning.isEmpty()) {
			for (ITileEntityLogistics tLogistics : tScanning) {
				BlockEntity tBE = (BlockEntity)tLogistics;

				if (tLogistics instanceof ITileEntityLogisticsStorage tStorage) {
					// :278 — the storage item filter joins the protected set
					ItemStack tItemFilter = tStorage.getLogisticsFilterItem();
					if (tItemFilter != null && !tItemFilter.isEmpty()) mFilteredFor.add(tItemFilter);
					// :280-282 — the semi-filtered answer joins the protected set
					if (tLogistics instanceof ITileEntityLogisticsSemiFilteredItem tSemi) {
						Collection<ItemStack> tFilter = tSemi.getLogisticsFilter((byte)6); // SIDE_ANY
						if (tFilter != null) mFilteredFor.addAll(tFilter);
					}
					switch (tStorage.getLogisticsPriorityFluid()) { // :284-288
					case  1: tFluidStorageGeneric .add(new LogisticsData(tBE, tStorage.getLogisticsFilterFluid(), null, 0)); break;
					case  2: tFluidStorageSemi    .add(new LogisticsData(tBE, tStorage.getLogisticsFilterFluid(), null, 0)); break;
					case  3: tFluidStorageFiltered.add(new LogisticsData(tBE, tStorage.getLogisticsFilterFluid(), null, 0)); break;
					}
					switch (tStorage.getLogisticsPriorityItem()) { // :290-294
					case  1: tStackStorageGeneric .add(new LogisticsData(tBE, null, tStorage.getLogisticsFilterItem(), 0)); break;
					case  2: tStackStorageSemi    .add(new LogisticsData(tBE, null, tStorage.getLogisticsFilterItem(), 0)); break;
					case  3: tStackStorageFiltered.add(new LogisticsData(tBE, null, tStorage.getLogisticsFilterItem(), 0)); break;
					}
				}

				// :297-435 — the cover bus (task p33-logistics-covers-12): the 12 logistics
				// covers on any BFS-reachable member (wire host / core / tank), each face
				// registering its covered-face adjacency into the routing lists above.
				CoverData tCovers = tLogistics instanceof gregtech6.covers.ICoverableTE tCoverable ? tCoverable.getCovers() : null;
				if (tCovers != null && !tCovers.mStopped) { // :298
					for (byte tSide = 0; tSide < 6; tSide++) { // :299 ALL_SIDES_VALID
						if (!(tCovers.mBehaviours[tSide] instanceof AbstractCoverAttachmentLogistics)) continue;
						gregtech6.covers.ICover tCover = tCovers.mBehaviours[tSide];

						// :300-319 — the CPU displays: the four pools drive value (redstone) + visual (bar)
						if (tCover == CoverLogisticsDisplayCPULogic.INSTANCE) {
							tCovers.value(tSide, (short) AbstractCoverLogisticsDisplay.displayValue(oCPU_Logic, mCPU_Logic), true);
							tCovers.visual(tSide, (short) AbstractCoverLogisticsDisplay.displayVisual(oCPU_Logic, mCPU_Logic));
							continue;
						}
						if (tCover == CoverLogisticsDisplayCPUControl.INSTANCE) {
							tCovers.value(tSide, (short) AbstractCoverLogisticsDisplay.displayValue(oCPU_Control, mCPU_Control), true);
							tCovers.visual(tSide, (short) AbstractCoverLogisticsDisplay.displayVisual(oCPU_Control, mCPU_Control));
							continue;
						}
						if (tCover == CoverLogisticsDisplayCPUStorage.INSTANCE) {
							tCovers.value(tSide, (short) AbstractCoverLogisticsDisplay.displayValue(oCPU_Storage, mCPU_Storage), true);
							tCovers.visual(tSide, (short) AbstractCoverLogisticsDisplay.displayVisual(oCPU_Storage, mCPU_Storage));
							continue;
						}
						if (tCover == CoverLogisticsDisplayCPUConversion.INSTANCE) {
							tCovers.value(tSide, (short) AbstractCoverLogisticsDisplay.displayValue(oCPU_Conversion, mCPU_Conversion), true);
							tCovers.visual(tSide, (short) AbstractCoverLogisticsDisplay.displayVisual(oCPU_Conversion, mCPU_Conversion));
							continue;
						}

						// :321-323 — the covered-face adjacency; logistics members are IGNORED
						// (the infinite-loop reduction), the cover targets the NON-member container.
						BlockEntity tAdjacent = tBE.getLevel() == null ? null
								: tBE.getLevel().getBlockEntity(tBE.getBlockPos().relative(Direction.from3DDataValue(tSide)));
						if (tAdjacent instanceof ITileEntityLogistics && ((ITileEntityLogistics)tAdjacent).canLogistics((byte)6)) continue;
						if (tAdjacent == null) continue;

						// :325-357 — the filtered-fluid trio
						if (tCover instanceof AbstractCoverLogisticsFluid tFluidCover) {
							net.minecraft.world.level.material.Fluid tFluid = tFluidCover.filterFluidOf(tCovers, tSide);
							if (tFluid != null) {
								int tPriority = tCovers.mValues[tSide] & 3;
								if (tCover == CoverLogisticsFluidExport.INSTANCE) {
									switch (tPriority) {
									case 1: tFluidExportsGeneric .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									case 2: tFluidExportsSemi    .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									default: tFluidExportsFiltered.add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									}
								} else if (tCover == CoverLogisticsFluidImport.INSTANCE) {
									switch (tPriority) {
									case 1: tFluidImportsGeneric .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									case 2: tFluidImportsSemi    .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									default: tFluidImportsFiltered.add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									}
								} else { // CoverLogisticsFluidStorage.INSTANCE
									switch (tPriority) {
									case 1: tFluidStorageGeneric .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									case 2: tFluidStorageSemi    .add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									default: tFluidStorageFiltered.add(new LogisticsData(tAdjacent, tFluid, null, 0)); break;
									}
								}
							}
							continue;
						}

						// :358-393 — the filtered-item trio
						if (tCover instanceof AbstractCoverLogisticsFiltered tItemCover) {
							ItemStack tStack = tItemCover.filterItemOf(tCovers, tSide);
							if (tStack != null) {
								mFilteredFor.add(tStack); // :361/:373/:385 — the filter joins the protected set
								int tStackSize = (tCovers.mValues[tSide] >> 2) & 127; // :363
								int tPriority = tCovers.mValues[tSide] & 3;
								if (tCover == CoverLogisticsItemExport.INSTANCE) {
									switch (tPriority) {
									case 1: tStackExportsGeneric .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									case 2: tStackExportsSemi    .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									default: tStackExportsFiltered.add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									}
								} else if (tCover == CoverLogisticsItemImport.INSTANCE) {
									switch (tPriority) {
									case 1: tStackImportsGeneric .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									case 2: tStackImportsSemi    .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									default: tStackImportsFiltered.add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									}
								} else { // CoverLogisticsItemStorage.INSTANCE
									switch (tPriority) {
									case 1: tStackStorageGeneric .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									case 2: tStackStorageSemi    .add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									default: tStackStorageFiltered.add(new LogisticsData(tAdjacent, null, tStack, tStackSize)); break;
									}
								}
							}
							continue;
						}

						LogisticsData tTarget = new LogisticsData(tAdjacent, null, null, (tCovers.mValues[tSide] >> 2) & 127); // :394

						// :395-398 — the Dump target
						if (tCover == CoverLogisticsGenericDump.INSTANCE) {
							tStackDumps.add(tTarget);
							continue;
						}

						// :399-432 — the generic trio (both items and fluids; the fluid arm skips
						// a semi-filtered item adjacency, :401-408 — its filter joins the protected set)
						int tDefault = tCovers.mValues[tSide] & 3;
						boolean aAllowFluids = true;
						if (tAdjacent instanceof ITileEntityLogisticsSemiFilteredItem) {
							aAllowFluids = false;
							Collection<ItemStack> tFilter = ((ITileEntityLogisticsSemiFilteredItem)tAdjacent).getLogisticsFilter(tSide);
							if (tFilter != null) {
								mFilteredFor.addAll(tFilter);
								if (tDefault == 0) tDefault = 2; // :406
							}
						}
						if (tCover == CoverLogisticsGenericExport.INSTANCE) {
							switch (tDefault) {
							default: if (aAllowFluids) tFluidExportsGeneric .add(tTarget); tStackExportsGeneric .add(tTarget); break;
							case 2:  if (aAllowFluids) tFluidExportsSemi    .add(tTarget); tStackExportsSemi    .add(tTarget); break;
							case 3:  if (aAllowFluids) tFluidExportsFiltered.add(tTarget); tStackExportsFiltered.add(tTarget); break;
							}
							continue;
						}
						if (tCover == CoverLogisticsGenericImport.INSTANCE) {
							switch (tDefault) {
							default: if (aAllowFluids) tFluidImportsGeneric .add(tTarget); tStackImportsGeneric .add(tTarget); break;
							case 2:  if (aAllowFluids) tFluidImportsSemi    .add(tTarget); tStackImportsSemi    .add(tTarget); break;
							case 3:  if (aAllowFluids) tFluidImportsFiltered.add(tTarget); tStackImportsFiltered.add(tTarget); break;
							}
							continue;
						}
						if (tCover == CoverLogisticsGenericStorage.INSTANCE) {
							switch (tDefault) {
							default: if (aAllowFluids) tFluidStorageGeneric .add(tTarget); tStackStorageGeneric .add(tTarget); break;
							case 2:  if (aAllowFluids) tFluidStorageSemi    .add(tTarget); tStackStorageSemi    .add(tTarget); break;
							case 3:  if (aAllowFluids) tFluidStorageFiltered.add(tTarget); tStackStorageFiltered.add(tTarget); break;
							}
							continue;
						}
					}
				}

				// :437-444 — the BFS extension
				if (tBE.getLevel() == tLevel) {
					for (byte tSide = 0; tSide < 6; tSide++) if (tLogistics.canLogistics(tSide)) {
						Direction tDir = Direction.from3DDataValue(tSide);
						BlockPos tAdj = tBE.getBlockPos().relative(tDir);
						int tMaxDistance = Math.max(Math.abs(tAdj.getX() - tCenter.getX()),
								Math.max(Math.abs(tAdj.getY() - tCenter.getY()), Math.abs(tAdj.getZ() - tCenter.getZ())));
						if (tMaxDistance <= mCPU_Control + 2) { // :439 — the cubic AoE
							oCPU_Control = Math.max(oCPU_Control, tMaxDistance - 2); // :440
							if (!tLevel.isLoaded(tAdj)) continue;
							BlockEntity tAdjacent = tLevel.getBlockEntity(tAdj);
							if (tAdjacent instanceof ITileEntityLogistics tNext
									&& tNext.canLogistics((byte)tDir.getOpposite().get3DDataValue()) // :442
									&& tScanned.add(tAdjacent)) {
								tScanningNext.add(tNext);
								tMembers++;
							}
						}
					}
				}
			}
			tScanning.clear(); // :446-448
			tScanning.addAll(tScanningNext);
			tScanningNext.clear();
		}

		// the stat report (port surface)
		mReportMembers = tMembers;
		mReportFluid[0] = tFluidStorageGeneric.size(); mReportFluid[1] = tFluidStorageSemi.size(); mReportFluid[2] = tFluidStorageFiltered.size();
		mReportItem[0] = tStackStorageGeneric.size(); mReportItem[1] = tStackStorageSemi.size(); mReportItem[2] = tStackStorageFiltered.size();
		mReportFilters = mFilteredFor.size();

		// :451-500 — the routing loop: one productive operation per Logic CPU
		while (++oCPU_Logic <= mCPU_Logic) {
			boolean tBreak = false;

			// :454-461 — Import Export Business
			for (List<LogisticsData>[] tExports : tExports1) {
				for (List<LogisticsData>[] tImports : tImports1) {
					if (moveFluids(tImports[0], tExports[0])) {tBreak = true; break;}
					if (moveStacks(tImports[1], tExports[1])) {tBreak = true; break;}
				}
				if (tBreak) break;
			}
			if (tBreak) continue;
			for (List<LogisticsData>[] tExports : tExports2) {
				for (List<LogisticsData>[] tImports : tImports2) {
					if (moveFluids(tImports[0], tExports[0])) {tBreak = true; break;}
					if (moveStacks(tImports[1], tExports[1])) {tBreak = true; break;}
				}
				if (tBreak) break;
			}
			if (tBreak) continue;

			// :472-476 — Defragmentation (generic content consolidates into the filtered then semi tiers)
			if (moveFluids(tFluidStorageGeneric, tFluidStorageFiltered)) continue;
			if (moveStacks(tStackStorageGeneric, tStackStorageFiltered)) continue;
			if (moveFluids(tFluidStorageGeneric, tFluidStorageSemi)) continue;
			if (moveStacks(tStackStorageGeneric, tStackStorageSemi)) continue;

			// :478-494 — Dump: generic item storage → dump targets, minus the protected set
			for (LogisticsData tImport : tStackStorageGeneric) {
				for (LogisticsData tExport : tStackDumps) {
					for (int j = 0; j < mCPU_Conversion; j++) {
						long tMoved = moveStacksForDump(tImport, tExport); // :482
						if (tMoved > 0) {
							oCPU_Conversion = Math.max(oCPU_Conversion, j + 1); // :484
							chargeEnergy(tMoved); // :485
							mMovedLast += tMoved;
							tBreak = true;
							continue;
						}
						break;
					}
					if (tBreak) break;
				}
				if (tBreak) break;
			}
			if (tBreak) continue;

			// :497-499 — Core didn't actually get used.
			oCPU_Logic--;
			break;
		}

		mMovedTotal += mMovedLast;
	}

	// ---------------------------------------------------------------------------
	// the moves (:508-609)
	// ---------------------------------------------------------------------------

	/** Upstream :508-511. */
	public boolean moveFluids(List<LogisticsData> aImports, List<LogisticsData> aExports) {
		for (LogisticsData aImport : aImports) for (LogisticsData aExport : aExports) if (moveFluids(aImport, aExport)) return true;
		return false;
	}

	/** Upstream :513-516. */
	public boolean moveStacks(List<LogisticsData> aImports, List<LogisticsData> aExports) {
		for (LogisticsData aImport : aImports) for (LogisticsData aExport : aExports) if (moveStacks(aImport, aExport)) return true;
		return false;
	}

	/**
	 * Upstream :518-555 — the four filter branches collapse to: the effective filter is the
	 * import's, else the export's; two set filters must be the SAME fluid (:521). Budget
	 * {@code 16000 L * Conversion}, cost {@code moved / 250} EU (:525).
	 */
	public boolean moveFluids(LogisticsData aImport, LogisticsData aExport) {
		if (aImport.mFluidFilter != null && aExport.mFluidFilter != null && aImport.mFluidFilter != aExport.mFluidFilter) return false; // :521
		Fluid tFilter = aImport.mFluidFilter != null ? aImport.mFluidFilter : aExport.mFluidFilter;
		IFluidHandler tFrom = fluidHandlerOf(aImport.mTarget), tTo = fluidHandlerOf(aExport.mTarget);
		if (tFrom == null || tTo == null) return false;
		long tBudget = 16000L * mCPU_Conversion; // :522/:530/:539/:546
		// FL.move_ :840/:842 — simulate the drain, execute the fill, then the real drain
		FluidStack tDrained = tFilter != null
				? tFrom.drain(new FluidStack(tFilter, (int)Math.min(tBudget, Integer.MAX_VALUE)), IFluidHandler.FluidAction.SIMULATE)
				: tFrom.drain((int)Math.min(tBudget, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
		if (tDrained == null || tDrained.getAmount() <= 0) return false;
		int tFilled = tTo.fill(tDrained.copy(), IFluidHandler.FluidAction.EXECUTE);
		if (tFilled <= 0) return false;
		if (tFilter != null) {
			FluidStack tQuery = new FluidStack(tFilter, tFilled);
			tFrom.drain(tQuery, IFluidHandler.FluidAction.EXECUTE);
		} else {
			tFrom.drain(tFilled, IFluidHandler.FluidAction.EXECUTE);
		}
		oCPU_Conversion = (int)Math.max(oCPU_Conversion, gregtech6.util.UT6.divup(tFilled, 16000L)); // :524
		chargeEnergy(gregtech6.util.UT6.divup(tFilled, 250L)); // :525
		mMovedLast += tFilled;
		return true;
	}

	/**
	 * Upstream :557-609 — the four branches collapse to: both filters set must be the same
	 * item (:561), the effective single filter is the import's else the export's else none.
	 * The per-operation throughput is {@code Conversion} stacks (:561/:572/:585/:596), the
	 * repeat-until-saturated arm (:567) only fires for unconfigured endpoints. The ST.move
	 * trailing quadruple maps (aMaxSize, aMinSize, aMaxMove, aMinMove) = (export||64,
	 * export||1, import||64, import||1); the GTItemMover seat takes (aMaxMove, aMinMove,
	 * aMaxSlotSize) = (import, minImport, export) — the aMinSize=export||1 clause is the
	 * mover's declared dead-at-1 trim. REVIEW SEAM: the first port draft passed
	 * (maxExport, maxExport, maxImport) — the 64 aMinMove skipped every sub-stack source
	 * slot (the mover's {@code count < aMinMove} gate) and the export/import max roles
	 * were swapped; the defragMovesPartialItemStacks arm pins the faithful form.
	 */
	public boolean moveStacks(LogisticsData aImport, LogisticsData aExport) {
		ItemStack tFilter;
		if (aImport.mItemFilter != null && aExport.mItemFilter != null) {
			if (aImport.mItemFilter.getItem() != aExport.mItemFilter.getItem()) return false; // :561 — the port item-identity form of ST.equal
			tFilter = aImport.mItemFilter;
		} else {
			tFilter = aImport.mItemFilter != null ? aImport.mItemFilter : aExport.mItemFilter;
		}
		IItemHandler tFrom = itemHandlerOf(aImport.mTarget), tTo = itemHandlerOf(aExport.mTarget);
		if (tFrom == null || tTo == null) return false;
		int tMaxExport = aExport.mStackSize == 0 ? 64 : aExport.mStackSize; // :562 the aMaxSize pair — the per-slot cap rides the EXPORT endpoint
		int tMaxImport = aImport.mStackSize == 0 ? 64 : aImport.mStackSize; // :562 the aMaxMove pair — the per-op total rides the IMPORT endpoint
		int tMinImport = aImport.mStackSize == 0 ? 1 : aImport.mStackSize;  // :562 the aMinMove pair
		boolean tReturn = false;
		for (int j = 0; j < mCPU_Conversion; j++) {
			int tMoved = GTItemMover.move(tFrom, tTo, tMaxImport, tMinImport, tMaxExport, tFilter, false);
			if (tMoved > 0) {
				oCPU_Conversion = Math.max(oCPU_Conversion, j + 1); // :564
				chargeEnergy(tMoved); // :565
				mMovedLast += tMoved;
				tReturn = true;
				if (aImport.mStackSize == 0 && aExport.mStackSize == 0) continue; // :567
			}
			break;
		}
		return tReturn;
	}

	/**
	 * Upstream :482 — {@code ST.move(import, export, tFilteredFor, F, F, T, F, 64, 1, 64, 1)}:
	 * the exclusion gate rides the protected set. The port's {@link GTItemMover} filter is a
	 * single stack, so the arm picks the FIRST source stack not in {@link #mFilteredFor} and
	 * moves exactly that item (the item-identity gate, the class-doc deviation note).
	 * The upstream eject arm (drop when the target is full) is cut with the ground-item
	 * surface — a saturated dump target just stops the move (declared trim).
	 */
	public long moveStacksForDump(LogisticsData aImport, LogisticsData aExport) {
		IItemHandler tFrom = itemHandlerOf(aImport.mTarget), tTo = itemHandlerOf(aExport.mTarget);
		if (tFrom == null || tTo == null) return 0;
		for (int i = 0; i < tFrom.getSlots(); i++) {
			ItemStack tStack = tFrom.getStackInSlot(i);
			if (tStack.isEmpty() || isFilteredFor(tStack)) continue;
			return GTItemMover.move(tFrom, tTo, 64, 1, 64, tStack.copy(), false);
		}
		return 0;
	}

	/** The item-identity membership of the protected set (the GTItemMover gate form). */
	public boolean isFilteredFor(ItemStack aStack) {
		for (ItemStack tFilter : mFilteredFor) {
			if (tFilter != null && !tFilter.isEmpty() && tFilter.getItem() == aStack.getItem()) return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the targets (the upstream DelegatorTileEntity resolution)
	// ---------------------------------------------------------------------------

	/**
	 * The per-target resolution seam — instance methods so the offline fixtures (which cannot
	 * init ForgeCapabilities, the CapabilityToken:28 transformer wall, the
	 * MultiBlockPartBlockEntity ruling) substitute per-target handlers in a test subclass.
	 * The production body IS the static capability query.
	 */
	@Nullable
	public IFluidHandler fluidHandlerOf(BlockEntity aTarget) {
		return fluidHandler(aTarget);
	}

	/** The item half of the {@link #fluidHandlerOf} seam. */
	@Nullable
	public IItemHandler itemHandlerOf(BlockEntity aTarget) {
		return itemHandler(aTarget);
	}

	@Nullable
	public static IFluidHandler fluidHandler(BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		//? if forge {
		return aTarget.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		//?} else {
		/*return aTarget.getLevel() == null ? null : aTarget.getLevel().getCapability(
				net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, aTarget.getBlockPos(), null);
		 *///?}
	}

	@Nullable
	public static IItemHandler itemHandler(BlockEntity aTarget) {
		if (aTarget == null || aTarget.isRemoved()) return null;
		//? if forge {
		return aTarget.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
		//?} else {
		/*return aTarget.getLevel() == null ? null : aTarget.getLevel().getCapability(
				net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, aTarget.getBlockPos(), null);
		 *///?}
	}

	/** The upstream {@code LogisticsData} (:611-659) — the target plus its optional filters and stack-size config. */
	public static class LogisticsData {
		public final BlockEntity mTarget;
		public final Fluid mFluidFilter;
		public final ItemStack mItemFilter;
		public final int mStackSize;

		public LogisticsData(BlockEntity aTarget, Fluid aFluidFilter, ItemStack aItemFilter, int aStackSize) {
			mTarget = aTarget;
			mFluidFilter = aFluidFilter;
			mItemFilter = aItemFilter;
			mStackSize = aStackSize;
		}
	}

	// ---------------------------------------------------------------------------
	// the node + energy faces (:680-701)
	// ---------------------------------------------------------------------------

	/** Upstream :689. */
	@Override
	public boolean canLogistics(byte aSide) {
		return true;
	}

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :691
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :700
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isEnergyType(aEnergyType, aSide, false); // :692
	}

	/**
	 * Upstream :680-687. The saturation gate first, then the size window; the
	 * oversize-packet explode arm (:684) is the declared no-explosion narrowing — refused,
	 * nothing consumed (the massfab precedent).
	 */
	@Override
	public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mEnergy > 128L + mCPU_Logic * 256L * mCPU_Conversion) return 0; // :681
		aSize = Math.abs(aSize); // :682
		if (!aDoInject) return aAmount; // :683
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) return 0; // :684
		mEnergy += aAmount * aSize; // :685
		return aAmount; // :686
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return 1024; // :694
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return 1024; // :695
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return 512; // :696
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 256; // :697
	}
}
