package gregtech6.tileentity.multiblocks;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregapi.util.CruciblePhysics;
import gregapi.util.UT;
import gregtech6.fluid.FluidBridge;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.recipes.maps.GT6RecipeMapCanner;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.MaterialStackNBT;

/**
 * 1.20.1 counterpart of gregtech/tileentity/multiblocks/MultiTileEntityCrucible.java
 * (714 lines, task crucible-multiblock) — the LARGE 3x3x3 crucible multiblock:
 * a hollow of wall parts with the opening on top, the controller at the
 * bottom-centre cell ("Main at Bottom-Center", upstream tooltip :139-140).
 *
 * <p><b>The three-layer wall semantics (upstream checkStructure2 :112-131, verbatim
 * per layer)</b>: the y+0 ring of 8 walls is the energy intake layer
 * ({@link MultiBlockPartBlockEntity#ONLY_ENERGY_IN} — the burning boxes touch HERE),
 * the y+1 ring is the mold-access layer ({@link MultiBlockPartBlockEntity#ONLY_CRUCIBLE}),
 * the y+2 ring is the item/fluid feed layer
 * ({@link MultiBlockPartBlockEntity#ONLY_ITEM_FLUID} — the top opening). The centre
 * column at y+1/y+2 must ALREADY be air (upstream :115-116: the getAir gate with the
 * idempotent setBlockToAir — a standing block FAILS the check, never cleared; the
 * checker's fail-not-clear hollow semantics are the same judgement). The layer masks
 * are consumed, never re-declared — the three ONLY_* constants are verbatim already
 * in {@link MultiBlockPartBlockEntity} (:103/:113/:118). The structure check walks
 * the declared pattern through the shared checker (the pattern-checker seam, the
 * CokeOven production pilot) — one judgement source for the server check and the
 * ghost preview.
 *
 * <p><b>The item input face (issue #20 sub-task B, upstream :204-234 + :460-544)</b>:
 * slot 0 — the base {@code mInventory} carrier wired in the constructor through
 * {@code setInventory} (the GT6HopperBaseBlockEntity.java:136 form, so the Root
 * ITEM_HANDLER capability answers and a hopper/pipe on any wall part resolves it through
 * the part relay). Per formed tick: the empty slot sucks ONE item entity out of the
 * cavity box (upstream :204 {@code WD.suck(x-0.5, y+0.125, z-0.5, 2, 3, 2)} — the whole
 * 3x3 footprint from just above the floor to above the top opening; the port takes one
 * item per tick, the TileEntitySmeltery.suckTopItem form, where the upstream took the
 * whole stack), then the feed ladder melts the slot content into {@code mContent}
 * through {@link #addMaterialStacks} (upstream :206-234 — the OM.anydata prefix
 * branches, ported over the TileEntitySmeltery.feedStacks MaterialPrefixItem form; the
 * upstream decremented ONE item per tick, the port melts the whole slot at once, the
 * same declared deviation as the small Smeltery). The right-click face is
 * {@link #useTop} (upstream onBlockActivated3 :460-544): structure-gated, top face only
 * (the block carrier {@code GTCrucibleControllerBlock.use} routes SIDES_UP), empty hand
 * takes the feed slot back (:469-473) or scrapes SCRAP off the cooled lightest content
 * (:475-497), a fluid container drains the molten lightest (:499-517) or pours a molten
 * fluid back through the bind(melting+25, boiling-1) gate (:518-538). A held SOLID is a
 * designed no-op (:498 falls through) — the upstream cannot right-click-feed either.
 * No GUI (the upstream census): suck/hopper + container right-click IS the interface.
 *
 * <p><b>The physics tick (upstream onServerTickPost :184-384) consumes the A-card
 * CruciblePhysics parameter face</b> — the LARGE parameter set
 * ({@link CruciblePhysics.Params#LARGE}: 432U / 1.10 / 8 / 5 / 100) drives the same
 * functions the small Smeltery runs at SMALL, exactly the shared-thermodynamics
 * arch ruling (tasks.p26-arch-crucible-chain ②). Per tick, upstream order:
 * <ol>
 * <li>the alloy scan + consumption (:236-294 → {@link CruciblePhysics#alloyScan} +
 *     {@link CruciblePhysics#applyAlloy});</li>
 * <li>the evaporation/acid/phase-gate loop (:296-334 →
 *     {@link CruciblePhysics#phaseGates}) with the world effects applied BE-side
 *     (the explosion via the Root {@code explode(strength)} :312-313, the acid
 *     destruction via {@code setToAir} :319, the gas/fire surfaces deferred — no
 *     entity-damage or WD.fire face is ported);</li>
 * <li>the weight census (:336-344) and the {@code oTemperature} latch (:346);</li>
 * <li>the HU heating step (:353-365 → {@link CruciblePhysics#tickHeat});</li>
 * <li>the meltdown gate (:367-378): over the ceiling the content is trashed and the
 *     3x3x3 cavity becomes lava (the core cell included — the controller dies in the
 *     flow);</li>
 * <li>the melt-down WARNING latch (:380-383 → {@link CruciblePhysics#isMeltDownWarning}).</li>
 * </ol>
 *
 * <p><b>The through-wall mold proxy</b>: the controller implements
 * {@link ITileEntityCrucible} with the upstream fillMoldAtSide (:547-556) verbatim —
 * the wall part answers the pour (the part-side relay is the B-card mold walking
 * onto the wall, the upstream MultiBlockPart :687-690 consumer). The wall parts
 * themselves need NO extra forwarding: the B-card mold already walks
 * controller-ward through the part's target resolution, so the controller-side
 * method IS the whole proxy (the card C ruling: controller-side implementation
 * preferred, the {@code MultiBlockPartBlockEntity implements} arm stays unused).
 *
 * <p><b>Structure loss = the slow cool-down (upstream :187-195)</b>: while the
 * structure check fails, the stored temperature decays toward the environment at
 * 1 K per 10 ticks and never drops below min(200, env). Declared deviation: the
 * upstream gates the decay on the GLOBAL server time (SERVER_TIME % 10); the port
 * counts on the controller's own tick cycle (mTimer % 10) — no global-clock face is
 * ported and the decay semantics (rate, floor) are identical.
 */
public class TileEntityCrucible extends TileEntityBase10MultiBlockBase implements ITileEntityCrucible, ITileEntityTemperature, ITileEntityEnergy {

	// ---------------------------------------------------------------------------
	// the constants (upstream :78-80, consuming the A-card LARGE parameter face)
	// ---------------------------------------------------------------------------

	/** Upstream :78 — the boiling vent radius (gas damage) and the fire spread radius. */
	public static long GAS_RANGE = CruciblePhysics.Params.LARGE.gasRange(), FLAME_RANGE = 5;

	/** Upstream :79 — 16 * 3 * 3 * 3 * U: the content capacity of the 3x3x3 cavity. */
	public static long MAX_AMOUNT = CruciblePhysics.Params.LARGE.maxAmount();

	/** Upstream :79 — the heat-mass divisor: 1 HU heats the wall material mass by 1 K per 100 kg. */
	public static long KG_PER_ENERGY = CruciblePhysics.Params.LARGE.kgPerEnergy();

	/** Upstream :80 — the LARGE-crucible wall heat bonus (the small Smeltery carries 1.25). */
	public static double HEAT_RESISTANCE_BONUS = CruciblePhysics.Params.LARGE.heatResistanceBonus();

	/** The default environment temperature (upstream CS.DEF_ENV_TEMP = C + 20 = 293). */
	public static final long DEF_ENV_TEMP = 293;

	/** The NBT keys (upstream NBT_TEMPERATURE / NBT_ENERGY / NBT_ACIDPROOF / NBT_MATERIALS). */
	public static final String NBT_TEMPERATURE = "temperature";
	public static final String NBT_ENERGY = "energy";
	public static final String NBT_ACIDPROOF = "acidproof";
	public static final String NBT_MATERIALS = "materials";

	/** The slot-0 feed inventory key (the smeltery "gt.inv" family spelling). */
	public static final String NBT_INVENTORY = "gt.inv";

	/**
	 * The client display census keys (task crucible-large-ber). Upstream never persists
	 * mDisplayed* — it hand-wraps them into getClientDataPacketByteArray (:596-604); the
	 * port rides the paint-key pattern instead (TileEntityBase03TicksAndSync :315-322):
	 * keys in {@code saveAdditional} ride BOTH sync channels for free because
	 * {@code getUpdateTag()} = {@code saveWithoutMetadata()}.
	 */
	public static final String NBT_DISPLAYED_HEIGHT = "gt.displayed_height";
	public static final String NBT_DISPLAYED_FLUID = "gt.displayed_fluid";

	// ---------------------------------------------------------------------------
	// the state (upstream :82-86)
	// ---------------------------------------------------------------------------

	/** The stored heat, in K (upstream :85 mTemperature — the physical state the tick drives). */
	public long mTemperature = DEF_ENV_TEMP;

	/** The previous tick's pre-heat temperature, the phase-crossing latch (upstream :85 oTemperature). */
	public long oTemperature = 0;

	/** The HU buffer (upstream :85 mEnergy — the burning-box feed pays into this). */
	public long mEnergy = 0;

	/** The heat countdown (upstream :83 mCooldown — 100 ticks of grace after the last charge). */
	public int mCooldown = 100;

	/** The melt-down WARNING latch (upstream :82 mMeltDown — the visual alarm state). */
	public boolean mMeltDown = false;

	/** The acidproofing (upstream :82 mAcidProof — a row property of the wall material). */
	public boolean mAcidProof = false;

	/** The molten content (upstream :86 mContent — the List of material stacks). */
	public final List<OreDictMaterialStack> mContent = new ArrayList<>();

	/**
	 * The client display census (upstream :83-84 mDisplayedHeight/mDisplayedFluid, the int
	 * form): the fill height 0..255 ({@code UT.Code.scale(tTotal, MAX_AMOUNT, 255, F)}
	 * :349 — the BER content top is {@code 1.125 + h/150} :635) and the lightest MOLTEN
	 * content's material id, {@code -1} = nothing molten (:350). Updated in the tick
	 * census; a change flags {@link #updateClientData()} (:351).
	 */
	public int mDisplayedHeight = 0, mDisplayedFluid = -1;

	/** The registry-path constructor (the BlockEntityType.Builder.of factory form, the oven precedent). */
	public TileEntityCrucible(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen registry keeps .get() out of reach). */
	protected TileEntityCrucible(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GT6Crucibles.MULTIBLOCK_CRUCIBLE_BE.get(), aPos, aState);
		// the slot-0 feed inventory, wired through the base setInventory (the
		// GT6HopperBaseBlockEntity.java:136 form) — the Root ITEM_HANDLER capability then
		// answers and the wall-part relay carries a hopper push the last step in
		// (issue #20: the crucible previously had NO item face at all). NEVER a fresh
		// mInventory field here: it would shadow the base carrier and the capability
		// would stay empty (the #20a TileEntitySmeltery lesson).
		setInventory(new GTItemStackHandler(1, this::setChanged));
		// the crucible has NO facing semantics upstream (getDefaultSide SIDE_UP :689, the
		// structure fully symmetric around the controller cell). The shared checker's cell
		// arithmetic ("the structure core sits BEHIND the facing", cellOffset = p - OFF)
		// degenerates to the identity at facing 0 (OFF[0] = 0,0,0) — the canonical facing
		// this controller is pinned to, so the pattern coordinates stay controller-relative
		// exactly like the upstream checkAndSetTargetOffset loop (:119-121, pure xCoord math).
		// NOTE this pin alone was NEVER a complete guard: the inherited
		// setFacingFromPlacement (GTMultiBlockControllerBlock.setPlacedBy, every player
		// placement) overwrites mFacing with the player's horizontal look direction 2..5
		// — which is why the walk itself must read patternWalkFacing() (the
		// builder-wand-form-fix override below); the RCON chains never saw the
		// displacement only because `setblock` has no placer and kept the pin.
		mFacing = 0;
	}

	@Override
	public String getTileEntityName() {
		return "crucible";
	}

	/** The break-drop face — {@link gregtech6.block.GTEntityBlock} reflects {@code getInventory} to scatter the slot-0 feed on break (Base10MultiBlockBase carries no accessor; upstream 05Inventories.breakBlock :153-171, canDrop = T). */
	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream :91-109)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_TEMPERATURE, Tag.TAG_ANY_NUMERIC)) mTemperature = aNBT.getLong(NBT_TEMPERATURE);
		if (aNBT.contains(NBT_TEMPERATURE + ".old", Tag.TAG_ANY_NUMERIC)) oTemperature = aNBT.getLong(NBT_TEMPERATURE + ".old");
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY);
		if (aNBT.contains(NBT_ACIDPROOF, Tag.TAG_ANY_NUMERIC)) mAcidProof = aNBT.getBoolean(NBT_ACIDPROOF);
		mContent.clear();
		mContent.addAll(MaterialStackNBT.loadList(NBT_MATERIALS, aNBT)); // :98 OreDictMaterialStack.loadList
		//? if forge {
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
		//?} else {
		/*if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
		 *///?}
		mMeltDown = CruciblePhysics.isMeltDownWarning(mTemperature, getTemperatureMax((byte)0)); // :99 re-derived, never stored
		// task crucible-large-ber — the client display census rehydration (the contains-guard
		// form; absent keys keep the zero/none display default)
		if (aNBT.contains(NBT_DISPLAYED_HEIGHT, Tag.TAG_ANY_NUMERIC)) mDisplayedHeight = aNBT.getInt(NBT_DISPLAYED_HEIGHT);
		if (aNBT.contains(NBT_DISPLAYED_FLUID, Tag.TAG_ANY_NUMERIC)) mDisplayedFluid = aNBT.getInt(NBT_DISPLAYED_FLUID);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_TEMPERATURE, mTemperature);              // UT.NBT.setNumber :106
		aNBT.putLong(NBT_TEMPERATURE + ".old", oTemperature);     // :107
		aNBT.putLong(NBT_ENERGY, mEnergy);                        // :105
		MaterialStackNBT.saveList(mContent, NBT_MATERIALS, aNBT); // :108 OreDictMaterialStack.saveList
		// task crucible-large-ber — the client display census (the paint-key pattern: both
		// sync channels ride getUpdateTag = saveWithoutMetadata)
		aNBT.putInt(NBT_DISPLAYED_HEIGHT, mDisplayedHeight);
		aNBT.putInt(NBT_DISPLAYED_FLUID, mDisplayedFluid);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the structure (:112-136)
	// ---------------------------------------------------------------------------

	/**
	 * The wall part block this structure is built from (upstream :88 mWalls = the part
	 * MTE id 18002 + the NBT_DESIGN wall swap — the registry pair becomes one Block per
	 * wall material, the LargeBoiler wall-variant shape). The production override reads
	 * the controller block's carried wall (the GT6Crucibles registration); the default
	 * here is the offline-test binding only.
	 */
	protected Block getWallBlock() {
		if (getLevel() != null && getBlockState().getBlock() instanceof gregtech6.block.multiblock.GTCrucibleControllerBlock tBlock) {
			return gregtech6.registry.GT6Crucibles.wallBlockOf(tBlock.row()); // the row's NBT_DESIGN wall identity
		}
		return Blocks.BRICKS;
	}

	/**
	 * The shell material — the wall material the physics ride (upstream mMaterial, the
	 * MTE registration row material; :336 the shell weight, :416-418 the temperature
	 * ceiling). Steel is the single-rung material ladder (the card C spec ⑤ ruling);
	 * the production override binds the registered wall row.
	 */
	@Nullable
	protected OreDictMaterial getShellMaterial() {
		return MT.Steel;
	}

	/** The physics parameter face this form consumes ({@link CruciblePhysics.Params#LARGE}). */
	protected CruciblePhysics.Params params() {
		return CruciblePhysics.Params.LARGE;
	}

	/**
	 * The crucible is CONTROLLER-ANCHORED and facing-independent — upstream
	 * MultiTileEntityCrucible.java:118-122 walks the wall rings straight off
	 * xCoord/yCoord/zCoord with no facing anywhere, and the :134-136 isInsideStructure is
	 * the controller box ("Main at Bottom-Center", the upstream :140 tooltip line). The
	 * declared pattern cells below are therefore CONTROLLER-relative, and the walk must
	 * NOT pass them through a horizontal facing's side-offset table: task
	 * builder-wand-form-fix — feeding {@code mFacing} displaced the whole check (and
	 * the builder-wand scaffold plus the {@code /gtmultiblock form} arm sharing the seam)
	 * one block off the machine for EVERY live facing, so the wand scaffolded a half-box
	 * and the part relay refused the far column ({@code wandTarget} →
	 * {@code isInsideStructure} = false → the silent no-op PASS). The zero-offset facing
	 * ({@code 0}: {@code OFF_X/Y/Z[0]} all zero, GTMultiBlockPattern.java:99-101)
	 * resolves every cell relative to the controller itself — upstream-exact for all
	 * facings.
	 */
	@Override
	public byte patternWalkFacing() {
		return 0;
	}

	/**
	 * Upstream :112-131, walked from the declared pattern (the pattern-checker seam):
	 * the three wall rings carry their per-layer usage masks, the centre column at
	 * y+1/y+2 is the fail-not-clear hollow pair (upstream :115-116), and the controller's
	 * own cell (y+0 centre) passes via the checker's inherited self-cell arm. The
	 * unloaded guard keeps the upstream semantics: an unloaded probe keeps the last
	 * verdict (the boiler :289-290 form). The walk rides {@link #patternWalkFacing()} —
	 * the controller-anchored feed, NOT {@code mFacing} (the displacement ruling above).
	 */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		if (!hasLevel()) return mStructureOkay; // :133
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				this, patternWalkFacing(), aCoordinates, aPlayer, aInventory);
		if (tVerdict.unloaded) return mStructureOkay; // unloaded cells keep the last verdict
		return tVerdict.formed;
	}

	/**
	 * The declared structure pattern: 24 forming wall cells in three rings (y+0
	 * ONLY_ENERGY_IN, y+1 ONLY_CRUCIBLE, y+2 ONLY_ITEM_FLUID — the upstream :119-121
	 * mask column) plus the two keep-hollow centre cells (y+1/y+2, the :115-116 top
	 * opening), appended last. The y+0 centre is the controller itself — never declared.
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			Block tWall = getWallBlock();
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			// :119 — the y+0 ring, ONLY_ENERGY_IN (the burning boxes touch here)
			ring(tBuilder, tWall, 0, MultiBlockPartBlockEntity.ONLY_ENERGY_IN);
			// :120 — the y+1 ring, ONLY_CRUCIBLE (the mold access layer)
			ring(tBuilder, tWall, 1, MultiBlockPartBlockEntity.ONLY_CRUCIBLE);
			// :121 — the y+2 ring, ONLY_ITEM_FLUID (the feed layer)
			ring(tBuilder, tWall, 2, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID);
			// :115-116 — the centre column, fail-not-clear air
			tBuilder.hollow(0, 1, 0, GTMultiBlockPattern.AIR);
			tBuilder.hollow(0, 2, 0, GTMultiBlockPattern.AIR);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	/**
	 * One wall ring: the 8 cells around the centre at the given layer height. The design
	 * write is 4 — the NET effect of the upstream two-pass check (task mb-formed-crucible-wall):
	 * pass 1 (:119-121) checks/writes design 0, pass 2 (:124-128, formed only) repaints every
	 * wall design 4 — the formed crucible's skin change. The declarative pattern declares the
	 * formed value directly (the checker judges block identity only, the upstream :70 column —
	 * design is never a FORM precondition); the unformed reset rides the target-invalidation
	 * arm (MultiBlockPartBlockEntity :203, the upstream MultiBlockPart :208-210 verbatim).
	 */
	private static final int FORMED_WALL_DESIGN = 4;

	/** The formed-design read seam (task crucible-render-followup — the datagen pin reads the constant without widening it). */
	public static int formedWallDesign() {
		return FORMED_WALL_DESIGN;
	}

	private static void ring(GTMultiBlockPattern.Builder aBuilder, Block aWall, int aY, int aUsage) {
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue; // the centre column is not a wall cell
			aBuilder.formingPart(tDX, aY, tDZ, aWall, aUsage, FORMED_WALL_DESIGN);
		}
	}

	/** Upstream :134-136 verbatim — the box around the controller, y from 0 to +2. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		return aX >= getBlockPos().getX() - 1 && aY >= getBlockPos().getY() && aZ >= getBlockPos().getZ() - 1
			&& aX <= getBlockPos().getX() + 1 && aY <= getBlockPos().getY() + 2 && aZ <= getBlockPos().getZ() + 1;
	}

	// ---------------------------------------------------------------------------
	// the temperature interface (upstream :410-418, ITileEntityTemperature)
	// ---------------------------------------------------------------------------

	@Override
	public long getTemperatureValue(byte aSide) {
		return mTemperature;
	}

	@Override
	public long getTemperatureMax(byte aSide) {
		OreDictMaterial tShell = getShellMaterial();
		if (tShell == null) return Long.MAX_VALUE; // the offline no-material form never melts
		return CruciblePhysics.temperatureMax(tShell, HEAT_RESISTANCE_BONUS); // :416-418
	}

	// ---------------------------------------------------------------------------
	// the energy face (upstream :699-709, ITileEntityEnergy — the y+0 HU intake)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :701 — accepting, never emitting. The HU arm is the burning-box feed the
	 * y+0 ONLY_ENERGY_IN ring exists for; the KU (oxygen steel-making), CU (cooling) and
	 * VIS_IGNIS arms of the upstream ENERGYTYPES list are the declared defer pool
	 * (SPEC ⑥), so this face answers HU only.
	 */
	@Override
	public boolean isEnergyType(gregapi.code.TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == gregapi.data.TD.Energy.HU;
	}

	/**
	 * Upstream :704 doInject, the HU arm verbatim — the buffer charge the :353-365 heat
	 * step consumes (the KU Air-injection branch and the CU drain branch defer with the
	 * energy-type list). The packet size bookkeeping is upstream-exact.
	 */
	@Override
	public long doInject(gregapi.code.TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aDoInject) mEnergy += Math.abs(aAmount * aSize);
		return aAmount;
	}

	/** Upstream :705 — the crucible always demands more (the melt is unbounded until the ceiling). */
	@Override
	public long getEnergyDemanded(gregapi.code.TagData aEnergyType, byte aSide, long aSize) {
		return Long.MAX_VALUE - mEnergy;
	}

	@Override
	public long getEnergySizeInputMin(gregapi.code.TagData aEnergyType, byte aSide) {
		return 1; // :706
	}

	@Override
	public long getEnergySizeInputRecommended(gregapi.code.TagData aEnergyType, byte aSide) {
		return 2048; // :707
	}

	@Override
	public long getEnergySizeInputMax(gregapi.code.TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :708
	}

	@Override
	public java.util.Collection<gregapi.code.TagData> getEnergyTypes(byte aSide) {
		return gregapi.data.TD.Energy.HU.AS_LIST; // :709
	}

	// ---------------------------------------------------------------------------
	// the content admission (upstream addMaterialStacks :386-408)
	// ---------------------------------------------------------------------------

	/** The shell weight the thermal blend rides (upstream :336/:388 mMaterial.getWeight(U*100)). */
	protected double shellWeight() {
		OreDictMaterial tShell = getShellMaterial();
		return tShell == null ? 0 : tShell.getWeight(CS.U * 100); // upstream :336 verbatim
	}

	/**
	 * Upstream addMaterialStacks (:386-408) via the shared physics: the capacity gate
	 * and the thermal blend live in {@link CruciblePhysics#addStacks}; the structure
	 * check stays here (the BE side owns the world question).
	 *
	 * @return if the material fit (and only then was blended in)
	 */
	public boolean addMaterialStacks(List<OreDictMaterialStack> aList, long aTemperature) {
		if (!checkStructure(false)) return false; // :387 the structure gate
		CruciblePhysics.AddResult tResult = CruciblePhysics.addStacks(mContent, aList, aTemperature, mTemperature, shellWeight(), params());
		if (tResult.added()) {
			mTemperature = tResult.temperature(); // :389 the blend
			setChanged();
		}
		return tResult.added();
	}

	/** The content census (upstream OM.total). */
	public long totalContent() {
		return CruciblePhysics.total(mContent);
	}

	// ---------------------------------------------------------------------------
	// the tick (:184-384)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide) return;

		// :187-195 — the structure loss arm: decay toward the environment, floor min(200, env)
		if (!checkStructure(false)) {
			coolStep(aTimer);
			return;
		}
		tickPhysics();
	}

	/**
	 * The formed-structure physics tick, upstream :236-383 order verbatim — preceded by
	 * the feed :204-234 (the cavity suck + the slot-0 melt ladder; the rain :197-202
	 * stays the declared pool arm).
	 */
	private void tickPhysics() {
		int tHashBefore = mContent.hashCode(); // :185 tHash

		// :204 — the empty slot sucks one item entity out of the cavity box
		if (mInventory.getStackInSlot(0).isEmpty()) {
			ItemStack tSucked = suckCavityItem();
			if (!tSucked.isEmpty()) mInventory.setStackInSlot(0, tSucked);
		}

		// :206-234 — the feed ladder melts the slot content (the structure gate rides
		// addMaterialStacks; the trash+fizz arm :210-212 fires on unknown items)
		ItemStack tStack = mInventory.getStackInSlot(0);
		if (!tStack.isEmpty()) {
			List<OreDictMaterialStack> tFeed = feedStacks(tStack);
			if (tFeed == null) {
				mInventory.setStackInSlot(0, ItemStack.EMPTY);
				fizz(); // SFX.MC_FIZZ :212 — the unknown-item trash hisses (the TileEntitySmeltery.fizz form)
			} else if (addMaterialStacks(tFeed, envTemperature())) {
				mInventory.setStackInSlot(0, ItemStack.EMPTY); // :216/:232 decrStackSize(0, 1) — the port melts the whole slot
			}
		}

		// :236-294 — the alloy scan + the consumption half
		CruciblePhysics.AlloyResult tAlloy = CruciblePhysics.alloyScan(mContent, mTemperature);
		CruciblePhysics.applyAlloy(mContent, tAlloy.alloy(), tAlloy.conversions());

		// :296-334 — the evaporation/acid/phase-gate loop, world effects BE-side
		boolean tNewContent = (tHashBefore != mContent.hashCode()); // :241
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(mContent, mTemperature, oTemperature, tNewContent, mAcidProof, params());
		if (tOutcome.fizz()) fizz(); // SFX.MC_FIZZ :303/:306/:318 — the boil-off/phase hiss

		// :336-344 — the weight census and the lightest-stack walk (the display feeds the BER)
		double tWeight = shellWeight() + CruciblePhysics.weight(mContent);
		if (tWeight < 0) tWeight = 0;
		OreDictMaterialStack tLightest = lightest();
		long tTotal = totalContent();

		// :346 — the crossing latch BEFORE the heat step (the next tick's gates read this)
		oTemperature = mTemperature;

		// :348-351 — the client display census: the fill height 0..255 + the lightest MOLTEN
		// material id; a change flags the vanilla block-update sync (the paint-key channel).
		int tDisplayedHeight = mDisplayedHeight, tDisplayedFluid = mDisplayedFluid;
		mDisplayedHeight = (int)CruciblePhysics.scale(tTotal, MAX_AMOUNT, 255, false);
		mDisplayedFluid = (tLightest == null || tLightest.mMaterial.mMeltingPoint > mTemperature ? -1 : tLightest.mMaterial.mID);
		if (mDisplayedHeight != tDisplayedHeight || mDisplayedFluid != tDisplayedFluid) updateClientData();

		// the destruction arms (:309-320) — content already cleared by the physics
		if (tOutcome.explosionStrength() > 0) {
			explode(tOutcome.explosionStrength()); // :312-313 the Root blast
			return;
		}
		if (tOutcome.acidDestroyed()) {
			setToAir(); // :319 — the acid melt-through kills the controller block
			return;
		}
		// the :307/:370 gas-damage and :308/:371 fire surfaces defer (no entity/fire face)

		// :353-365 — the HU heating step
		CruciblePhysics.TickResult tHeat = CruciblePhysics.tickHeat(mTemperature, mEnergy, envTemperature(), tWeight, mCooldown, KG_PER_ENERGY);
		mTemperature = tHeat.temperature();
		mEnergy = tHeat.energy();
		mCooldown = tHeat.cooldown();

		// :367-378 — the meltdown gate
		long tMax = getTemperatureMax((byte)0);
		if (mTemperature > tMax) {
			meltdown(tMax);
			return;
		}

		// :380-383 — the melt-down WARNING latch. The upstream :612-616 red-shifts the WALL
		// colour on this flag; the port keeps that face on the BER/controller cell only —
		// ponytail: the wall blocks' tint is BAKED (GTMachineTintModel, the p32 route; the
		// live-BE read is the red line and the runtime BlockColor rendered achromatic), a
		// per-wall meltdown arm would need ModelData + GTRenderUpdates plumbing on 24 walls,
		// defer declared (crucible-large-ber).
		boolean tWarning = CruciblePhysics.isMeltDownWarning(mTemperature, tMax);
		if (mMeltDown != tWarning) {
			mMeltDown = tWarning;
			updateClientData();
		}
		setChanged();
	}

	/**
	 * The :192-193 decay pair — 1 K per 10 ticks toward the environment, floored at
	 * min(200, env). Package-visible for the offline tests.
	 */
	void coolStep(long aTimer) {
		long tEnv = envTemperature();
		if (aTimer % 10 == 0) {
			if (mTemperature > tEnv) mTemperature--;
			if (mTemperature < tEnv) mTemperature++;
		}
		mTemperature = Math.max(mTemperature, Math.min(200, tEnv));
		setChanged();
	}

	/**
	 * The environment temperature seam. The production form binds the A-card
	 * temperature/environment face; the default here is the flat DEF_ENV_TEMP the
	 * upstream WD.envTemp degenerates to in a climate-less world.
	 */
	protected long envTemperature() {
		return DEF_ENV_TEMP;
	}

	/** Upstream onPlaced :589-592 — the stored heat starts at the environment. */
	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide);
		if (aIsServerSide && mTimer == 0) mTemperature = envTemperature();
	}

	/**
	 * Upstream :367-377 — the meltdown: the content is trashed and the 3x3x3 cavity
	 * becomes flowing lava (the controller's own cell included, so the multiblock dies
	 * in the flow). The :368 hiss rides {@link #fizz()}; the gas-damage and fire-spread
	 * arms (:370-371) defer with the other world-effect surfaces. Package-visible for
	 * the offline tests.
	 */
	void meltdown(long aTemperatureMax) {
		fizz(); // SFX.MC_FIZZ :368 — the melt-down hiss, before the trash
		mContent.clear(); // :369 GarbageGT.trash(mContent)
		if (hasLevel() && isServerSide()) {
			int tX = getBlockPos().getX(), tY = getBlockPos().getY(), tZ = getBlockPos().getZ();
			// :372-376 — flowing lava meta 1: no source, it decays away instead of a permanent pool
			net.minecraft.world.level.block.state.BlockState tLava = Blocks.LAVA.defaultBlockState()
					.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL, 1);
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) { // :372-376
				getLevel().setBlock(new BlockPos(tX + i, tY    , tZ + j), tLava, 3);
				getLevel().setBlock(new BlockPos(tX + i, tY + 1, tZ + j), tLava, 3);
				getLevel().setBlock(new BlockPos(tX + i, tY + 2, tZ + j), tLava, 3);
			}
		}
		setChanged();
	}

	/** The SFX.MC_FIZZ arm (:212/:303/:306/:318/:368) — the sound-only sink (the TileEntitySmeltery.fizz form verbatim). */
	protected void fizz() {
		if (hasLevel()) getLevel().levelEvent(1501, getBlockPos(), 0); // the vanilla LevelEvent fire-extinguish fizz
	}

	/** Upstream :319 setToAir — the acid melt-through. */
	private void setToAir() {
		if (hasLevel() && isServerSide()) {
			getLevel().setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
		}
	}

	// ---------------------------------------------------------------------------
	// the item input face (issue #20 sub-task B, upstream :204-234)
	// ---------------------------------------------------------------------------

	/**
	 * The :204 WD.suck cavity box — (x-0.5, y+0.125, z-0.5) to (x+1.5, y+3.125, z+1.5):
	 * the whole 3x3 footprint from just above the controller floor to above the top
	 * opening (PX_P[2] = 0.125, the 2-pixel lift). Takes ONE item per tick (the
	 * TileEntitySmeltery.suckTopItem port form; the upstream took the whole stack — the
	 * same declared deviation, the slot is a single-item feeder). Protected: the test
	 * seam (the stub world has no entity list to spawn into).
	 */
	protected ItemStack suckCavityItem() {
		if (!hasLevel()) return ItemStack.EMPTY;
		BlockPos tPos = getBlockPos();
		AABB tBox = new AABB(tPos.getX() - 0.5, tPos.getY() + 0.125, tPos.getZ() - 0.5,
				tPos.getX() + 1.5, tPos.getY() + 3.125, tPos.getZ() + 1.5);
		List<ItemEntity> tEntities = getLevel().getEntitiesOfClass(ItemEntity.class, tBox);
		for (ItemEntity tEntity : tEntities) {
			if (tEntity.isRemoved()) continue;
			ItemStack tStack = tEntity.getItem();
			if (tStack.isEmpty()) continue;
			ItemStack tOne = tStack.copy();
			tOne.setCount(1);
			tStack.shrink(1);
			if (tStack.isEmpty()) tEntity.discard();
			return tOne;
		}
		return ItemStack.EMPTY;
	}

	/**
	 * The :206-234 feed ladder over the TileEntitySmeltery.feedStacks form (the reviewed
	 * port of the OM.anydata prefix branches): a MaterialPrefixItem feeds its prefix
	 * amount per item (the :229-232 generic arm), the ore-family prefixes feed the
	 * ore-direct projection (:217-228 — mTargetCrushing × mOreMultiplier with the
	 * form-factor scaling), a vanilla ore rides the bridge, the vanilla ingot/nugget
	 * family rides {@link #vanillaIngots()}/{@link #vanillaNuggets()} at the prefix
	 * amount; anything else returns null (the :210-212 trash+fizz arm).
	 *
	 * <p>ponytail: the ladder + the vanilla bridges are duplicated from
	 * TileEntitySmeltery (the r4-20b declared debt — one shared crucible-io helper when
	 * a scope allows touching both files without a behavior card riding along).
	 */
	@Nullable
	public List<OreDictMaterialStack> feedStacks(ItemStack aStack) {
		if (aStack.getItem() instanceof MaterialPrefixItem tItem) {
			long tCount = aStack.getCount();
			List<OreDictMaterialStack> rList = new ArrayList<>();
			if (tItem.prefix == OP.oreRaw || tItem.prefix.contains(gregapi.data.TD.Prefix.STANDARD_ORE)) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 1)); // :218/:226
			} else if (tItem.prefix == OP.blockRaw) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 9)); // :220
			} else if (tItem.prefix.contains(gregapi.data.TD.Prefix.DENSE_ORE)) {
				rList.add(CruciblePhysics.oreDirect(tItem.material, 2)); // :228
			} else if (tItem.prefix.mAmount > 0) {
				rList.add(new OreDictMaterialStack(tItem.material, tItem.prefix.mAmount * tCount)); // :230-232
			}
			rList.removeIf(tStack -> tStack.mAmount <= 0);
			return rList.isEmpty() ? null : rList;
		}
		OreDictMaterial tVanilla = vanillaOres().get(aStack.getItem());
		if (tVanilla != null) {
			List<OreDictMaterialStack> rList = new ArrayList<>();
			rList.add(CruciblePhysics.oreDirect(tVanilla, 1)); // a vanilla ore block = one standard ore
			return rList;
		}
		// the vanilla ingot/nugget bridge — the prefix amount per item (the :229-232 generic
		// arm over the OM.anydata ingotIron/nugget* data; whole-stack, the declared deviation)
		OreDictMaterial tVanillaIngot = vanillaIngots().get(aStack.getItem());
		if (tVanillaIngot != null) {
			List<OreDictMaterialStack> rList = new ArrayList<>();
			rList.add(new OreDictMaterialStack(tVanillaIngot, OP.ingot.mAmount * aStack.getCount()));
			return rList;
		}
		OreDictMaterial tVanillaNugget = vanillaNuggets().get(aStack.getItem());
		if (tVanillaNugget != null) {
			List<OreDictMaterialStack> rList = new ArrayList<>();
			rList.add(new OreDictMaterialStack(tVanillaNugget, OP.nugget.mAmount * aStack.getCount()));
			return rList;
		}
		return null;
	}

	/**
	 * The vanilla-ore bridge for the feed ladder (the TileEntitySmeltery vanillaOres
	 * lazy form verbatim — the eager static-map form froze pre-init nulls on the 21.1
	 * class-init order lottery, GTWireSpecs:35).
	 */
	private static volatile java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> sVanillaOres = null;

	/** The vanilla-ore bridge, built on first use (one material generation — the lazy form). */
	private static java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> vanillaOres() {
		java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> tTable = sVanillaOres;
		if (tTable == null) sVanillaOres = tTable = java.util.Map.of(
				net.minecraft.world.item.Items.IRON_ORE, MT.Fe,
				net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE, MT.Fe,
				net.minecraft.world.item.Items.RAW_IRON, MT.Fe,
				net.minecraft.world.item.Items.GOLD_ORE, MT.Au,
				net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE, MT.Au,
				net.minecraft.world.item.Items.RAW_GOLD, MT.Au,
				net.minecraft.world.item.Items.COPPER_ORE, MT.Cu,
				net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE, MT.Cu,
				net.minecraft.world.item.Items.RAW_COPPER, MT.Cu);
		return tTable;
	}

	/** The vanilla ingot/nugget bridge (task crucible-behavior-fixes — the TileEntitySmeltery vanillaIngots/vanillaNuggets form verbatim, same lazy-form rule). */
	private static volatile java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> sVanillaIngots = null;

	/** The vanilla ingot bridge, built on first use (one material generation — the lazy form). */
	private static java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> vanillaIngots() {
		java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> tTable = sVanillaIngots;
		if (tTable == null) sVanillaIngots = tTable = java.util.Map.of(
				net.minecraft.world.item.Items.IRON_INGOT, MT.Fe,
				net.minecraft.world.item.Items.GOLD_INGOT, MT.Au,
				net.minecraft.world.item.Items.COPPER_INGOT, MT.Cu);
		return tTable;
	}

	private static volatile java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> sVanillaNuggets = null;

	/** The vanilla nugget bridge (vanilla has no copper nugget — two entries). */
	private static java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> vanillaNuggets() {
		java.util.Map<net.minecraft.world.level.ItemLike, OreDictMaterial> tTable = sVanillaNuggets;
		if (tTable == null) sVanillaNuggets = tTable = java.util.Map.of(
				net.minecraft.world.item.Items.IRON_NUGGET, MT.Fe,
				net.minecraft.world.item.Items.GOLD_NUGGET, MT.Au);
		return tTable;
	}

	// ---------------------------------------------------------------------------
	// the right-click face (issue #20 sub-task B, upstream onBlockActivated3 :460-544)
	// ---------------------------------------------------------------------------

	/**
	 * The top-face click (the GTCrucibleControllerBlock.use carrier routes SIDES_UP and
	 * this method answers the :461 structure gate — an unformed crucible refuses). Empty
	 * hand takes the feed slot back (:469-473, the temperature damage is the entity
	 * pool) or scrapes SCRAP from the cooled lightest content (:475-497); a fluid
	 * container drains the molten lightest (:499-517) or pours a molten fluid back in
	 * (:518-538). A held SOLID falls through (:498 — the upstream designed no-op: the
	 * right-click never feeds, players throw items in or use a hopper). Server side does
	 * the work; the client just consumes the click.
	 *
	 * @return false only when the structure gate refuses (the caller PASSes)
	 */
	public boolean useTop(Player aPlayer, InteractionHand aHand) {
		if (!checkStructure(false)) return false; // :461 — the structure gate FIRST
		if (!isServerSide() || aPlayer == null) return true;
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		OreDictMaterialStack tLightest = lightest();

		// :468-473 — take the feed slot back
		if (!mInventory.getStackInSlot(0).isEmpty()) {
			if (tHeld.isEmpty()) {
				aPlayer.setItemInHand(aHand, mInventory.getStackInSlot(0));
				mInventory.setStackInSlot(0, ItemStack.EMPTY);
			}
			return true;
		}

		// :474-497 — scrape SCRAP from the COOLED lightest content
		if (tHeld.isEmpty() && tLightest != null && mTemperature < tLightest.mMaterial.mMeltingPoint) {
			ItemStack tScrap = GT6RecipeMapCrucible.matStack(OP.scrapGt, tLightest.mMaterial, 1);
			long tScrapAmount = OP.scrapGt.mAmount;
			if (tScrap == null || tLightest.mAmount < tScrapAmount) {
				tLightest.mAmount = 0; // :478-482 — the remainder dusts away
				aPlayer.causeFoodExhaustion(0.4F); // the UT.Entities.exhaust arm
				return true;
			}
			tLightest.mAmount -= tScrapAmount;
			if (!aPlayer.getInventory().add(tScrap)) aPlayer.drop(tScrap, false); // the ST.add/give form
			aPlayer.causeFoodExhaustion(0.1F);
			return true;
		}

		// :499-538 — the fluid-container arm (drain molten out / pour back), playerless
		if (!tHeld.isEmpty()) {
			ContainerArm tArm = fluidContainerArm(tHeld);
			if (tArm != null) {
				aPlayer.setItemInHand(aHand, tArm.heldAfter());
				if (!aPlayer.getInventory().add(tArm.containerOut())) aPlayer.drop(tArm.containerOut(), false);
				return true;
			}
		}
		return true; // :541 — the top click is always consumed
	}

	/** The two-slot outcome of the container arm: the held stack after the shrink and the swapped-out container. */
	public record ContainerArm(ItemStack heldAfter, ItemStack containerOut) {}

	/**
	 * The :499-517 (an empty container DRAINS the lightest molten content) + :518-538 (a
	 * molten container POURS back through the bind(melting+25, boiling-1) gate) arm,
	 * playerless — the TileEntitySmeltery.fluidContainerArm form over THIS crucible's
	 * content and temperature. Shrinks {@code aHeld} on success and answers the
	 * swapped-out container; null = the arm falls through (no handler, nothing molten,
	 * one of the gates refused).
	 */
	@Nullable
	public ContainerArm fluidContainerArm(ItemStack aHeld) {
		IFluidHandlerItem tHandler = GT6RecipeMapCanner.sContainerResolver.apply(aHeld.copy());
		if (tHandler == null) return null;
		FluidStack tHeldFluid = tHandler.getFluidInTank(0);
		if (tHeldFluid == null || tHeldFluid.isEmpty()) {
			OreDictMaterialStack tLightest = lightest();
			if (tLightest == null || mTemperature < tLightest.mMaterial.mMeltingPoint) return null;
			net.minecraft.world.level.material.Fluid tMolten = FluidBridge.moltenFluidForMaterial(tLightest.mMaterial.mNameInternal);
			if (tMolten == null) return null;
			long tLiters = Math.min(1000, Math.max(1, UT.Code.units(tLightest.mAmount, CS.U, FluidBridge.L_PER_MOLTEN_UNIT, false)));
			FluidStack tFill = new FluidStack(tMolten, (int)tLiters);
			// the :504 gate — the fluid must not be hotter than the crucible unless cold
			int tFluidTemp = tFill.getFluid().getFluidType().getTemperature();
			if (tFluidTemp >= 320 && mTemperature < tFluidTemp) return null;
			int tFilled = tHandler.fill(tFill, IFluidHandler.FluidAction.EXECUTE);
			if (tFilled <= 0) return null;
			ItemStack tContainer = tHandler.getContainer();
			tLightest.mAmount -= UT.Code.units(tFilled, FluidBridge.L_PER_MOLTEN_UNIT, CS.U, true); // :510 back-conversion
			aHeld.shrink(1);
			return new ContainerArm(aHeld, tContainer);
		}
		// :518-538 — POUR the molten fluid back in (the bind(melting+25, boiling-1) temperature gate)
		OreDictMaterial tFluidMaterial = materialOfFluid(tHeldFluid.getFluid());
		if (tFluidMaterial == null) return null;
		long tUnits = UT.Code.units(tHeldFluid.getAmount(), FluidBridge.L_PER_MOLTEN_UNIT, CS.U, false);
		long tPourTemperature = UT.Code.bind(tFluidMaterial.mMeltingPoint + 25, tFluidMaterial.mBoilingPoint - 1, tHeldFluid.getFluid().getFluidType().getTemperature());
		if (!addMaterialStacks(new ArrayList<>(java.util.List.of(new OreDictMaterialStack(tFluidMaterial, tUnits))), tPourTemperature)) return null;
		// the container must leave EMPTY: drain before getContainer — the wrappers answer
		// their internal stack verbatim (FluidBucketWrapper.getContainer → the container
		// field), so an undrained handler would hand the FILLED container back and dupe
		// the molten charge (upstream :520 ST.container)
		tHandler.drain(tHeldFluid, IFluidHandler.FluidAction.EXECUTE);
		ItemStack tEmpty = tHandler.getContainer();
		aHeld.shrink(1);
		return new ContainerArm(aHeld, tEmpty);
	}

	/** The :465-466 lightest-content census (the same walk feeds scrap and the bucket arm). */
	@Nullable
	public OreDictMaterialStack lightest() {
		OreDictMaterialStack rLightest = null;
		for (OreDictMaterialStack tMaterial : mContent) {
			if (rLightest == null || tMaterial.mMaterial.mGramPerCubicCentimeter < rLightest.mMaterial.mGramPerCubicCentimeter) rLightest = tMaterial;
		}
		return rLightest;
	}

	/** The reverse FluidBridge walk (the upstream OreDictMaterial.FLUID_MAP face, :521). */
	@Nullable
	public static OreDictMaterial materialOfFluid(net.minecraft.world.level.material.Fluid aFluid) {
		if (aFluid == null) return null;
		for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			if (FluidBridge.moltenFluidForMaterial(tMaterial.mNameInternal) == aFluid) return tMaterial;
		}
		return null;
	}

	//? if neoforge {
	/*// (21.1 seam: NeoForge removed BlockEntity#getCapability — the W4 registerBlockEntity
	// delegates to this member; no @Override. The item face = the slot-0 feed handler over
	// the Root mInventory carrier (the GT6HopperBaseBlockEntity seam shape), so the
	// MULTIBLOCK_CRUCIBLE_BE wiring row and every wall-part relay resolve the feed slot.
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) mInventory;
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// the through-wall mold proxy (upstream :547-556, ITileEntityCrucible)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream fillMoldAtSide (:547-556) verbatim: a formed structure pours its
	 * molten, self-smelted stacks (the mTargetSmelting identity gate — the molten
	 * metal proper, not an intermediate) into the adjacent Mold, one stack per call,
	 * the pour amount subtracted from the content. The wall part relays here through
	 * its target resolution (the upstream MultiBlockPart :687-690 walk).
	 */
	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		if (checkStructure(false)) for (OreDictMaterialStack tContent : mContent) {
			if (tContent != null && mTemperature >= tContent.mMaterial.mMeltingPoint
					&& tContent.mMaterial.mTargetSmelting.mMaterial == tContent.mMaterial) {
				long tAmount = aMold.fillMold(tContent, mTemperature, aSideOfMold);
				if (tAmount > 0) {
					tContent.mAmount -= tAmount;
					setChanged();
					return true;
				}
			}
		}
		return false;
	}
}
