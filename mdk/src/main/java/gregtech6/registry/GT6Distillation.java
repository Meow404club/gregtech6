package gregtech6.registry;

import gregapi.util.UT;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.block.GTComposedNameItem;
import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.gui.machines.GTDistillationTowerMUI;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.MultiBlockFluidHandler;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * The distillation-tower family registration home (task p29-w3-distill-crucible ①②, the
 * ADR-P3-4 self-contained form — the {@link GT6Crucibles}/{@code GT6BurningBoxes} shape:
 * block + item + BET DeferredRegisters attached from the construct event;
 * {@code GTMachines.java}/{@code GTMultiBlocks.java} untouched). The KJS face of the card:
 * registration surface (2 controllers + the part blocks they consume are card ①'s) plus the
 * datapack domain (the DISTILLATION_TOWER / CRYO_DISTILLATION_TOWER smoke rows are the
 * consumer-card JSON direct-pour anchors); NO KubeJS surface.
 *
 * <p><b>The two rows</b> (Loader_MultiTileEntities.java:1226-1227 re-read verbatim —
 * StainlessSteel shell, hardness 6.0, NBT_INPUT 512 window 1..1024, NBT_INV/TANK_SIDE_AUTO_OUT
 * SIDE_BACK, NBT_CHEAP_OVERCLOCKING T — the card ① semantic bit's FIRST live consumer): the
 * Distillation Tower 17101 takes HU over {@link GT6RecipeMaps#DISTILLATION_TOWER}, the Cryo
 * Distillation Tower 17111 takes CU over {@link GT6RecipeMaps#CRYO_DISTILLATION_TOWER} (the
 * crafting pipe column is {@code pipeNonuple} StainlessSteel vs ANY.Cu — the tier-a JSON
 * faces, GT6CraftingRecipes). The structures are IDENTICAL (the Cryo class upstream is a
 * verbatim clone, MultiTileEntityCryoDistillationTower.java:52-101), so ONE block class, ONE
 * BE class and ONE BET mount both rows — the row (energy domain + map) rides the block
 * carrier, the GT6Boilers/LargeBoilerRow form.
 *
 * <p><b>The structure</b> (MultiTileEntityDistillationTower.java:52-101 transcription over the
 * declared-pattern seam): 3x3 Heat Transmitter 18101 base layer at y-1 (ONLY_ENERGY_IN), then
 * a 3x3x8 column of Distill Part 18102 (bottom layer ONLY_ITEM_FLUID — the only input layer;
 * the seven layers above ONLY_FLUID_OUT). The BACK-CENTER cell of each part layer carries
 * design 1 (the hole texture — the card ① DESIGN property's first structural consumer): the
 * hole is the cell at {@code centre - OFF[facing]}, i.e. the pattern REBUILDS per facing (a
 * facing-keyed lazy cache — the Coke Oven's static ring is facing-symmetric and could stay
 * lazy-immutable, the tower's hole column cannot). The controller itself sits in the front
 * middle of the y+0 layer (centre + OFF[facing]) and passes via the checker's self-cell arm.
 *
 * <p><b>The output chain</b> (upstream :141-171): items auto-move from the output slots to the
 * arm cell one block behind the hole column ({@code getOffset*N(mFacing, 3)} at controller Y —
 * the port's auto-IO cut is restored HERE because the registration row declares
 * NBT_INV_SIDE_AUTO_OUT SIDE_BACK, an auto-out machine by census); fluids push per-class down
 * the back hole column ({@code mTanksOutput} iterate + the upstream fluid-class routing table
 * :148-170: propane/methane y+7 ... default y+1). <b>The output bank is the upstream NINE-tank
 * library</b> (task p30-distill-output-routing, the 2026-09-16 distill-tower ruling option a
 * — the W3④ single-tank freeze is UNDONE): upstream readFromNBT2 sizes {@code mTanksOutput}
 * from the map's fluid-OUT count (MultiTileEntityBasicMachine.java:161), and RM.java:65/:66
 * fix BOTH tower maps at fluids 1/9/0 — nine default-capacity tanks, so the seven-fraction
 * true rows (Loader_Recipes_Chem.java:352-:360) pass canOutput and every fraction lands in
 * its own tank before the class routing pushes it out. NBT: tank 0 keeps the base
 * {@code output_tank} key (old single-tank saves load verbatim), tanks 1..8 ride
 * {@code output_tank_i} (the upstream {@code NBT_TANK.out.i} index form, in-repo key naming).
 *
 * <p><b>The tick face</b> (the one deliberate base override): the shared machine base's onTick
 * carries the Coke Oven's UNCONDITIONAL TU self-generation ({@code mEnergy++} per tick), whose
 * upstream form is GATED ({@code mEnergyTypeAccepted == TD.Energy.TU},
 * MultiTileEntityBasicMachine.java:455). The tower rows are HU/CU and must not self-generate —
 * an ungated +1/t would run the machine forever on nothing and make every energy-guard arm
 * untestable — so {@link TileEntityDistillationTower#onTick} replicates the 600-tick poll
 * (TileEntityBase10MultiBlockBase) plus the gated body (no self-generation, :459/:461) instead
 * of calling the machine base's body.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Distillation {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "gt6");
	public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, "gt6");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "gt6");

	// ------------------------------------------------------------------------------------
	// the rows (Loader_MultiTileEntities.java:1226-1227, the registration order)
	// ------------------------------------------------------------------------------------

	/**
	 * One tower registration row — the Loader aRegistry.add projection. {@code energyType}
	 * is the NBT_ENERGY_ACCEPTED column (HU :1226 / CU :1227); the shell material
	 * (StainlessSteel), hardness (6.0) and the NBT_INPUT 512 window (1..1024) are shared by
	 * both rows and live as constants on the BE.
	 */
	public record TowerRow(String path, String display, int metaId, TagData energyType) {

		/** The Cryo flag — the CU row (:1227). Drives the map and the energy domain together. */
		public boolean cryo() {
			return energyType == TD.Energy.CU;
		}
	}

	/** :1226 — "Distillation Tower", HU. */
	public static final TowerRow TOWER_ROW = new TowerRow("distillation_tower", "Distillation Tower", 17101, TD.Energy.HU);
	/** :1227 — "Cryo Distillation Tower", CU (the verbatim-clone structure, the CU energy domain). */
	public static final TowerRow CRYO_ROW = new TowerRow("cryo_distillation_tower", "Cryo Distillation Tower", 17111, TD.Energy.CU);

	/** The two rows in registration order (the datagen/walkers iterate). */
	public static final java.util.List<TowerRow> ROWS = java.util.List.of(TOWER_ROW, CRYO_ROW);

	/** The registered controller blocks by path (the BET valid list + the command walkers). */
	public static final Map<String, RegistryObject<GTDistillationTowerBlock>> TOWER_BLOCKS_BY_PATH = new java.util.LinkedHashMap<>();

	/** The registered controller items, same keys. */
	public static final Map<String, RegistryObject<net.minecraft.world.item.Item>> TOWER_ITEMS_BY_PATH = new java.util.LinkedHashMap<>();

	static {
		for (TowerRow tRow : ROWS) {
			TOWER_BLOCKS_BY_PATH.put(tRow.path(), BLOCKS.register(tRow.path(),
					() -> new GTDistillationTowerBlock(tRow, GTMultiBlocks.partProperties(6.0F))));
			TOWER_ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(),
					() -> new GTComposedNameItem(GT6Distillation.TOWER_BLOCKS_BY_PATH.get(tRow.path()).get(), new net.minecraft.world.item.Item.Properties())));
		}
	}

	/**
	 * The tower BET: one BE class over both controller blocks (the LARGE_BOILER_BE
	 * one-BET-many-blocks form; the row rides the block carrier).
	 */
	public static final RegistryObject<BlockEntityType<TileEntityDistillationTower>> TOWER_BE =
			BLOCK_ENTITY_TYPES.register("multiblock_distillation_tower", () -> BlockEntityType.Builder.of(
					TileEntityDistillationTower::new, towerBlockArray()).build(null));

	/** The controller-block array for the BET (the same varargs shape). */
	private static Block[] towerBlockArray() {
		return TOWER_BLOCKS_BY_PATH.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	// ------------------------------------------------------------------------------------
	// the controller block
	// ------------------------------------------------------------------------------------

	/** The tower controller block — the concrete {@link GTMultiBlockControllerBlock} over the shared BET, the row rides it. The use arm (task p33-gui-distill-tower) opens the ModularUI panel through the {@link GT6MuiMachine#tryOpen} factory chain (the ACT :80-96 shape) — no MenuType (the P26 no-new-MenuType ruling holds, the factory carries its own network). */
	public static final class GTDistillationTowerBlock extends GTMultiBlockControllerBlock {

		private final TowerRow mRow;

		public GTDistillationTowerBlock(TowerRow aRow, net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
			super(aProperties);
			mRow = aRow;
		}
		//? if neoforge {
		/*
		// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
		// dispatch) — the simpleCodec representative-value form, the GTLargeBoilerBlock precedent.
		@Override
		protected com.mojang.serialization.MapCodec<? extends GTDistillationTowerBlock> codec() {
			return simpleCodec(aProperties -> new GTDistillationTowerBlock(GT6Distillation.ROWS.get(0), aProperties));
		}
		*///?}

		/** The registration row (the BoilerTankBlock.row carrier read). */
		public TowerRow row() {
			return mRow;
		}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return GT6Distillation.TOWER_BE.get();
		}

		@Override
		//? if forge {
		public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		//?} else {
		/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
		//(the GTAdvancedCraftingTableBlock fork shape).
		InteractionHand aHand = InteractionHand.MAIN_HAND;
		*///?}
			// the MUI open chain — the BE implements GT6MuiMachine, the factory's own network
			// carries the open (no MenuType, the GTAdvancedCraftingTableBlock :89-96 shape)
			BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
			if (tBlockEntity instanceof TileEntityDistillationTower tTower && aPlayer instanceof net.minecraft.server.level.ServerPlayer tServerPlayer) {
				GT6MuiMachine.tryOpen(tServerPlayer, tTower);
				return InteractionResult.CONSUME; // upstream openGUI
			}
			return InteractionResult.CONSUME;
		}
	}

	// ------------------------------------------------------------------------------------
	// the controller BE
	// ------------------------------------------------------------------------------------

	/**
	 * The distillation tower controller — MultiTileEntityDistillationTower.java /
	 * MultiTileEntityCryoDistillationTower.java over the ported machine base. The class doc
	 * of {@link GT6Distillation} carries the structure/output-chain/tick derivations; this
	 * BE carries: the row config (energy domain + map), the HU/CU energy face, the with-tanks
	 * recipe check, the back-hole fluid routing and the item auto-out arm.
	 */
	public static class TileEntityDistillationTower extends TileEntityBase10MultiBlockMachine implements GT6MuiMachine {

		/** The shared registration constants: NBT_HARDNESS/NBT_RESISTANCE 6.0 (:1226-1227). */
		public static final float SHELL_HARDNESS = 6.0F;

		/**
		 * The output-bank size — RM.java:65/:66 {@code IN-OUT-MIN-FLUID} 1/9/0: BOTH tower
		 * maps declare NINE fluid-OUT slots, which is the upstream readFromNBT2 :161
		 * {@code mTanksOutput} length (MultiTileEntityBasicMachine sizes the bank from
		 * {@code mRecipes.mOutputFluidCount}). The seven-fraction true rows fill seven of
		 * the nine; the last two stay spare, exactly like upstream.
		 */
		public static final int OUTPUT_TANK_COUNT = 9;

		/** The NBT key of the input tank (the upstream NBT_TANK+".in."+i form, i = 0). */
		public static final String NBT_INPUT_TANK = "tank_in_0";

		/** The item auto-out arm distance — upstream :143 {@code getOffset*N(mFacing, 3)}. */
		public static final int OUTPUT_ARM_DISTANCE = 3;

	/** The input tank — ONE (the map's fluid-IN count is 1; the upstream :160 per-map sizing). */
	public final FluidTankGT mTankInput = new FluidTankGT();

		/** The facing the cached pattern was built for (the hole column is facing-dependent). */
		private byte mPatternFacing = -1;

		@Nullable
		private GTMultiBlockPattern mStructurePattern = null;

		/**
		 * The registry-path constructor — the one the BET factory's method reference resolves
		 * to, so it MUST carry the type (the CokeOven form: a null type here silently killed
		 * the ticker pairing and the save mapping — the live r1-r6 debugging cycle).
		 */
		public TileEntityDistillationTower(BlockPos aPos, BlockState aState) {
			this(TOWER_BE.get(), aPos, aState);
		}

		/** The test seam: offline fixtures build their own BET (the frozen-registry form). */
		public TileEntityDistillationTower(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
			// the output bank unfrozen (p30-distill-output-routing, ruling 2026-09-16 option
			// a): the upstream readFromNBT2 :161 row — the bank re-points to the map's
			// fluid-OUT count, nine default-capacity tanks (RM.java:65/:66 fluids 1/9/0; the
			// tower rows carry no NBT_TANK_CAPACITY so the upstream default FluidTankGT
			// capacity stands). The base field lost its final for THIS consumer; the Coke
			// Oven family keeps the one-tank default.
			mTanksOutput = new FluidTankGT[OUTPUT_TANK_COUNT];
			for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i] = new FluidTankGT();
			// Loader:1226-1227 — NBT_INPUT 512 + NBT_INPUT_MIN 1 + NBT_INPUT_MAX 1024 (the
			// explicit-override form) + NBT_CHEAP_OVERCLOCKING T (the card ① semantic bit's
			// first live consumer). Registration config, not persisted.
			applyEnergyRowSpec(new EnergyRowSpec(512L, 1L, 1024L, null, true, null));
			// the base defaults carry the Coke Oven row's constants-power/ignition shape; the
			// towers register NEITHER NBT_NO_CONSTANT_POWER NOR NBT_NEEDS_IGNITION (:1226-1227).
			mRequiresIgnition = false;
			mNoConstantEnergy = false;
			mEnergyTypeAccepted = row() != null ? row().energyType() : TD.Energy.HU;
		}

		/** The carried row — null on the offline fixtures (a non-tower block state). */
		@Nullable
		public TowerRow row() {
			return getBlockState().getBlock() instanceof GTDistillationTowerBlock tBlock ? tBlock.row() : null;
		}

		/**
		 * The MUI panel build (task p33-gui-distill-tower, the IUIHolder.buildUI :21-44
		 * contract) — the tower's own panel factory, the GTBasicMachineMUI shared panel is
		 * NOT touched (the shared-face boundary). MUI-only: the inherited base MenuProvider
		 * (getMenuType default) is dead code on this family — nobody calls it, no MenuType
		 * is registered for the towers (the P26 no-new-MenuType ruling).
		 */
		@Override
		public brachy.modularui.screen.ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData aData,
				brachy.modularui.value.sync.PanelSyncManager aSyncManager, brachy.modularui.screen.UISettings aSettings) {
			return GTDistillationTowerMUI.buildPanel(this, aSyncManager);
		}

		@Override
		public String getTileEntityName() {
			TowerRow tRow = row();
			return tRow != null ? tRow.path() : "distillation_tower";
		}

		@Override
		public RecipeMap recipes() {
			RecipeMap tMap = mRecipes;
			if (tMap == null) {
				TowerRow tRow = row();
				tMap = tRow != null && tRow.cryo() ? GT6RecipeMaps.CRYO_DISTILLATION_TOWER : GT6RecipeMaps.DISTILLATION_TOWER;
				mRecipes = tMap;
			}
			return tMap;
		}

		// ---------------------------------------------------------------------------
		// the structure (MultiTileEntityDistillationTower.java:52-101)
		// ---------------------------------------------------------------------------

		/** The structure centre — one cell behind the facing (the getOffset*N anchor, :53). */
		private BlockPos centre() {
			Direction tBack = Direction.from3DDataValue(mFacing).getOpposite();
			return getBlockPos().relative(tBack);
		}

		/** The base-layer part block (upstream part id 18101). A hook so the offline tests bind fixtures (the CokeOven getPartBlock seam). */
		protected Block getTransmitterBlock() {
			return GTMultiBlocks.HEAT_TRANSMITTER.get();
		}

		/** The column part block (upstream part id 18102). The same offline fixture hook. */
		protected Block getPartBlock() {
			return GTMultiBlocks.anyPartBlock("distill_part");
		}

		/**
		 * The declared pattern (the class doc derivation): 9 transmitter cells at y-1, 72 part
		 * cells at y0..y7, the back-centre design 1. Built PER FACING (the hole column moves
		 * with it) — the cache is keyed on the facing the walk actually rides.
		 */
		@Override
		@Nullable
		public GTMultiBlockPattern getStructurePattern() {
			if (mStructurePattern == null || mPatternFacing != mFacing) {
				GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
				Block tTransmitter = getTransmitterBlock();
				Block tPart = getPartBlock();
				int tBackX = -Direction.from3DDataValue(mFacing).getStepX(); // the back-centre column, centre-relative
				int tBackZ = -Direction.from3DDataValue(mFacing).getStepZ();
				for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
					tBuilder.formingPart(i, -1, j, tTransmitter, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, 0);
				}
				for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
					for (int tY = 0; tY < 8; tY++) {
						tBuilder.formingPart(i, tY, j, tPart,
								tY == 0 ? MultiBlockPartBlockEntity.ONLY_ITEM_FLUID : MultiBlockPartBlockEntity.ONLY_FLUID_OUT,
								i == tBackX && j == tBackZ ? 1 : 0);
					}
				}
				mStructurePattern = tBuilder.build();
				mPatternFacing = mFacing;
			}
			return mStructurePattern;
		}

		@Override
		public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable net.minecraft.world.entity.player.Player aPlayer, @Nullable net.minecraft.world.Container aInventory) {
			if (!hasLevel()) return mStructureOkay; // :91-133 — unloaded/no-level keeps the last verdict
			GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
					this, mFacing, aCoordinates, aPlayer, aInventory);
			if (tVerdict.unloaded) return mStructureOkay; // :59
			return tVerdict.formed;
		}

		/** Upstream :125-128 verbatim — the tower box, centre x/z ±1, y-1..y+7. */
		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			BlockPos tCentre = centre();
			return aX >= tCentre.getX() - 1 && aY >= tCentre.getY() - 1 && aZ >= tCentre.getZ() - 1
				&& aX <= tCentre.getX() + 1 && aY <= tCentre.getY() + 7 && aZ <= tCentre.getZ() + 1;
		}

		// ---------------------------------------------------------------------------
		// the tick face — the gated body (see the class doc, upstream :451-467)
		// ---------------------------------------------------------------------------

		@Override
		public void onTick(long aTimer, boolean aIsServerSide) {
			// the 10Base 600-tick poll (:121-124) — replicated because the machine base's
			// onTick body carries the UNGATED TU self-generation the HU/CU rows refuse
			// (MultiTileEntityBasicMachine.java:455 gates it on mEnergyTypeAccepted == TU).
			if (aIsServerSide && aTimer % 600 == 5) {
				if (!checkStructure(false)) checkStructure(true);
				doDefaultStructuralChecks();
			}
			if (!aIsServerSide) return;
			doOutputFluids(); // :459
			doWork(aTimer);   // :461
			doOutputItems();  // the back-arm auto-out (upstream :143, the SIDE_BACK row column)
		}

		// ---------------------------------------------------------------------------
		// the energy face (upstream :489-515, the HU/CU accepting form)
		// ---------------------------------------------------------------------------

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :510 (the emitting half is the pool)
		}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
			return mEnergyTypeAccepted.AS_LIST;
		}

		/** Upstream :489-508 doInject verbatim (the mChargeRequirement charging half is cut — the unported subsystem). */
		@Override
		public synchronized long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (mStopped) return 0;
			aSize = Math.abs(aSize);
			if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) {
				if (aDoInject) overcharge(aSize, aEnergyType);
				return aAmount;
			}
			if (aEnergyType == mEnergyTypeAccepted) {
				// :502 mStateNew folds away with the alternating face (the base's :815 cut)
				long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput / aSize) + (tInput % aSize != 0 ? 1 : 0));
				if (aDoInject) mEnergy += tConsumed * aSize;
				return tConsumed;
			}
			return 0;
		}

		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return mInputMin;}             // :513
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mInput;}        // :514
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return mInputMax;}             // :515
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return Math.max(0, mInputMax - mEnergy);} // the :503 min-form

		// ---------------------------------------------------------------------------
		// the recipe check — the upstream :683-778 WITH the fluid counting (the port base's
		// trimmed form passes null tanks; the tower rows are fluid-IN machines)
		// ---------------------------------------------------------------------------

		@Override
		public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
			mCouldUseRecipe = false; // :684
			RecipeMap tRecipes = recipes();
			if (tRecipes == null) return DID_NOT_FIND_RECIPE; // :685

			int tInputItemsCount = 0, tInputFluidsCount = 0; // :689
			ItemStack[] tInputs = new ItemStack[tRecipes.mInputItemsCount];
			for (int i = 0; i < tRecipes.mInputItemsCount; i++) {
				tInputs[i] = slot(i);
				if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
			}
			if (mTankInput.has()) tInputFluidsCount++; // :706 (the mTanksInput walk, one tank)

			if (tInputItemsCount                     < tRecipes.mMinimalInputItems ) return DID_NOT_FIND_RECIPE; // :708
			if (tInputFluidsCount                    < tRecipes.mMinimalInputFluids) return DID_NOT_FIND_RECIPE; // :709 — the gate the trimmed base cut
			if (tInputItemsCount + tInputFluidsCount < tRecipes.mMinimalInputs     ) return DID_NOT_FIND_RECIPE; // :710

			Recipe tRecipe = tRecipes.findRecipe(mLastRecipe, mInputMax, slot(SLOT_SPECIAL), tankSnapshot(), tInputs); // :712
			if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719 (the mCanUseOutputTanks fallback is the pool)

			if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
			int tMaxProcessCount = canOutput(tRecipe); // :735
			if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

			if (aApplyRecipe) aApplyRecipe = !mRequiresIgnition || mIgnited > 0 || mActive; // :737
			if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, tankSnapshot(), tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738
			if (aApplyRecipe) drainTankFor(tRecipe); // the tank mirror — see tankSnapshot
			mCouldUseRecipe = true; // :739
			if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

			if (tMaxProcessCount > 1) {
				// :743-745 — the energy bind (HU/CU rows DO bind) + the boolean two-stage consume
				if (!mParallelDuration && mEnergyTypeAccepted != TD.Energy.TU) {
					tMaxProcessCount = (int) gregapi.util.UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt));
				}
				int tExtra = 0;
				while (tExtra < tMaxProcessCount - 1 && tRecipe.isRecipeInputEqual(true, false, tankSnapshot(), tInputs)) {
					drainTankFor(tRecipe); // the per-extra-stage tank mirror
					tExtra++;
				}
				tMaxProcessCount = 1 + tExtra;
			}

			mCurrentRecipe = tRecipe; // :757
			mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
			mOutputFluids = tRecipe.getFluidOutputs(tMaxProcessCount); // :759

			if (tRecipe.mEUt < 0) { // :761-777 verbatim (the base's math — duplicated here because the base body is fluid-blind)
				mMaxProgress = tRecipe.mDuration;
				mMinEnergy = 0;
			} else {
				if (mParallelDuration) {
					mMinEnergy = Math.max(1, tRecipe.mEUt);
					mMaxProgress = Math.max(1, UT.Code.units(mMinEnergy * Math.max(1, tRecipe.mDuration) * tMaxProcessCount, 10000, 10000, true));
				} else {
					mMinEnergy = Math.max(1, mEnergyTypeAccepted == TD.Energy.TU ? tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount);
					mMaxProgress = Math.max(1, UT.Code.units(mMinEnergy * Math.max(1, tRecipe.mDuration), 10000, 10000, true));
				}
				// :773 — the CHEAP_OVERCLOCKING gate: the tower row REFUSES the fold (T)
				if (!mCheapOverclocking) {
					while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {
						mMinEnergy *= 4;
						mMaxProgress *= 2;
					}
				}
			}

			removeEmptyInputStacks(); // :776
			return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
		}

		/**
		 * The input-tank snapshot for the recipe seams (null = empty). COPIES, not the live
		 * stack: the consume phase SHRINKS the passed stacks, and {@code FluidTankGT.getFluid()}
		 * returns the internal carrier whose {@code setAmount(0)} would collapse the fluid
		 * identity to EMPTY while the authoritative long amount stayed — the live r6 corruption
		 * (in_tank=[1000mB minecraft:empty]). The tank itself is drained by
		 * {@link #drainTankFor} after each successful apply-stage.
		 */
		@Nullable
		private FluidStack[] tankSnapshot() {
			FluidStack tContent = mTankInput.fluid();
			if (tContent == null || tContent.getAmount() <= 0) return null;
			return new FluidStack[] {tContent.copy()};
		}

		/** One apply-stage's tank drain: the recipe's fluid-input amounts, mirrored off the COPIES. */
		private void drainTankFor(Recipe aRecipe) {
			for (FluidStack tFluid : aRecipe.mFluidInputs) {
				if (tFluid != null && !tFluid.isEmpty()) mTankInput.remove(tFluid.getAmount());
			}
		}

		// ---------------------------------------------------------------------------
		// the output chain (upstream :141-171)
		// ---------------------------------------------------------------------------

		/** Upstream :142-144 doOutputItems — move the output slots to the arm cell (offset 3, controller Y). */
		public void doOutputItems() {
			IItemHandler tTarget = itemOutputTarget();
			if (tTarget == null) return;
			RecipeMap tRecipes = recipes();
			for (int i = tRecipes.mInputItemsCount, n = tRecipes.mInputItemsCount + tRecipes.mOutputItemsCount; i < n; i++) {
				ItemStack tStack = slot(i);
				if (tStack == null || tStack.isEmpty()) continue;
				// the IItemHandler has NO insert-any-slot form — walk the slots (the -1 shortcut
				// threw ArrayIndexOutOfBounds inside InvWrapper, the live r7 tick killer)
				ItemStack tRest = tStack;
				for (int tTargetSlot = 0; tTargetSlot < tTarget.getSlots() && !tRest.isEmpty(); tTargetSlot++) {
					tRest = tTarget.insertItem(tTargetSlot, tRest, false);
				}
				if (tRest.isEmpty() || tRest.getCount() < tStack.getCount()) {
					mInventory.setStackInSlot(i, tRest.isEmpty() ? ItemStack.EMPTY : tRest);
					mInventoryChanged = true;
					setChanged();
				}
			}
		}

		/**
		 * Upstream :143 — the arm cell target ({@code getOffset*N(mFacing, 3)} at controller Y),
		 * resolved fresh per push (the aUseAutoIO pull face stays null — the tower never pulls).
		 */
		@Nullable
		protected IItemHandler itemOutputTarget() {
			if (!hasLevel() || isClientSide()) return null;
			BlockPos tArm = offsetBy(mFacing, OUTPUT_ARM_DISTANCE);
			BlockEntity tNeighbor = getLevel().getBlockEntity(tArm);
			if (tNeighbor == null) return null;
			//? if forge {
			return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.from3DDataValue(mFacing)).resolve().orElse(null);
			//?} else {
			/*return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, tArm, Direction.from3DDataValue(mFacing));
			 *///?}
		}

		/**
		 * Upstream :146-171 — every output tank pushes down the back hole column by FLUID
		 * CLASS (:152-166): propane/methane to y+7, butane y+6, petrol/gasoline/bioethanol
		 * y+5, kerosene/kerosine/glycerol y+4, diesel/biodiesel y+3, fuel/fueloil/biofuel
		 * y+2, everything else y+1. The bank loop is the upstream :148
		 * {@code for (FluidTankGT tTank : mTanksOutput)} (the nine-tank library); the
		 * three-beat fill-then-deduct is the base's {@code doOutputFluids} form.
		 */
		@Override
		public void doOutputFluids() {
			for (FluidTankGT tTank : mTanksOutput) {
				FluidStack tContent = tTank.fluid();
				if (tContent == null || tContent.getAmount() <= 0) continue;
				IFluidHandler tTarget = fluidHandlerAt(routingCell(tContent.getFluid()));
				if (tTarget == null) continue;
				FluidStack tAvailable = tTank.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
				if (tAvailable == null || tAvailable.isEmpty()) continue;
				int tFilled = tTarget.fill(tAvailable, FluidAction.EXECUTE);
				if (tFilled <= 0) continue; // the target refuses → the source keeps everything
				FluidStack tDrained = tTank.drain(tFilled, FluidAction.EXECUTE);
				if (tDrained != null && !tDrained.isEmpty()) mInventoryChanged = true; // :168 updateInventory
			}
		}

		/** The upstream :152-166 class table → the hole column layer (1..7) of the fluid. */
		public static int routingLayer(Fluid aFluid) {
			return routingLayerByName(String.valueOf(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aFluid)));
		}

		/** The pure name-seam of the routing table (the offline tests drive THIS — the registry lookup stays the production wrapper). "fuel" is the cover word: fueloil/biofuel carry it as a substring, exactly like upstream {@code FL.is} name matching. */
		public static int routingLayerByName(String aKey) {
			if (aKey.contains("propane") || aKey.contains("methane")) return 7;
			if (aKey.contains("butane")) return 6;
			if (aKey.contains("petrol") || aKey.contains("gasoline") || aKey.contains("bioethanol")) return 5;
			if (aKey.contains("kerosene") || aKey.contains("kerosine") || aKey.contains("glycerol")) return 4;
			if (aKey.contains("diesel") || aKey.contains("biodiesel")) return 3;
			if (aKey.contains("fuel")) return 2;
			return 1;
		}

		/** The routing cell for one fluid: the arm column (offset 3) at {@code controllerY + routingLayer}. */
		public final BlockPos routingCell(Fluid aFluid) {
			return offsetBy(mFacing, OUTPUT_ARM_DISTANCE).above(routingLayer(aFluid));
		}

		/** Upstream :173-176 — the tower never pulls and never hands out per-side targets (the routing IS the push face). */
		@Override
		protected IFluidHandler getFluidOutputTarget(Fluid aOutput) {
			return null;
		}

		// ---------------------------------------------------------------------------
		// the fluid capability face (fill → the input tank, drain → the output tank)
		// ---------------------------------------------------------------------------

		//? if forge {
		@Override
		public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
			if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
				return net.minecraftforge.common.util.LazyOptional.of(() -> new TowerFluidHandler(this, aSide)).cast();
			}
			return super.getCapability(aCapability, aSide);
		}
		//?} else {
		/*// 21.1 face: the provider wiring (GT6CapabilityWiring.registerDistillationFaces)
		// delegates into this seam — the TileEntityBase10MultiBlockMachine :832-840 fork form,
		// overridden so the FLUID_HANDLER branch answers the tower's own handler.
		public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
			if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
				return (T) new TowerFluidHandler(this, aSide);
			}
			return super.getCapability(aCapability, aSide);
		}
		*///?}

		/**
		 * The tower fluid face: FILL reaches the input tank (the map carries fluid-IN 1; the
		 * side-less query and every world face admit — the input geometry "bottom layer only"
		 * is the PARTS' ONLY_ITEM_FLUID mode, not a face mask), DRAIN reads the output bank
		 * behind the shared coke-oven mask 61 (the MultiBlockFluidHandler side rules). The
		 * exposed tank list is the upstream getFluidTanks2 union: index 0 = the input, 1..9 =
		 * the output library in bank order (the nine-tank library).
		 */
		public static final class TowerFluidHandler implements IFluidHandler {

			private final TileEntityDistillationTower mTower;
			@Nullable
			private final Direction mSide;

			public TowerFluidHandler(TileEntityDistillationTower aTower, @Nullable Direction aSide) {
				mTower = aTower;
				mSide = aSide;
			}

			@Override public int getTanks() {return 1 + mTower.mTanksOutput.length;} // input + the output library

			/** Index 0 = the input tank, i >= 1 = output bank slot i-1 (the getFluidTanks2 union). */
			private FluidTankGT tank(int aTank) {
				return aTank == 0 ? mTower.mTankInput : mTower.mTanksOutput[aTank - 1];
			}

			@Override
			public net.minecraftforge.fluids.FluidStack getFluidInTank(int aTank) {
				FluidStack tContent = tank(aTank).fluid();
				return tContent == null ? net.minecraftforge.fluids.FluidStack.EMPTY : tContent;
			}

			@Override
			public int getTankCapacity(int aTank) {
				return FluidTankGT.bindInt(tank(aTank).getCapacity());
			}

			@Override
			public boolean isFluidValid(int aTank, net.minecraftforge.fluids.FluidStack aStack) {
				return aTank == 0; // fill reaches the input tank only
			}

			@Override
			public int fill(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {
				if (aResource == null || aResource.isEmpty()) return 0;
				FluidStack tContent = mTower.mTankInput.fluid();
				if (tContent != null && !tContent.isEmpty() && !mTower.mTankInput.contains(aResource)) return 0; // the contains gate
				int rFilled = (int) Math.min(Integer.MAX_VALUE, mTower.mTankInput.add(aResource.getAmount(), aResource)); // the long add → int contract
				if (rFilled > 0 && aAction.execute()) {
					mTower.setChanged();
					mTower.mInventoryChanged = true; // the recipe re-check window (the MultiBlockFluidHandler drain-side beat)
				}
				return rFilled;
			}

			@Override
			public net.minecraftforge.fluids.FluidStack drain(int aMaxDrain, FluidAction aAction) {
				if (aMaxDrain <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
				if (!MultiBlockFluidHandler.drainAllowedBySide(mTower.mFacing, mSide)) return net.minecraftforge.fluids.FluidStack.EMPTY;
				for (FluidTankGT tTank : mTower.mTanksOutput) { // the first bank tank with content serves the draw
					if (tTank.isEmpty()) continue;
					return tTank.drain(aMaxDrain, aAction);
				}
				return net.minecraftforge.fluids.FluidStack.EMPTY;
			}

			@Override
			public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {
				if (aResource == null || aResource.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
				for (FluidTankGT tTank : mTower.mTanksOutput) if (tTank.contains(aResource)) {
					return tTank.drain(FluidTankGT.bindInt(Math.min(tTank.amount(), aResource.getAmount())), aAction);
				}
				return net.minecraftforge.fluids.FluidStack.EMPTY;
			}
		}

		// ---------------------------------------------------------------------------
		// NBT (the base shape + the input tank + the output library tail)
		//
		// The BASE owns tank 0 under "output_tank" (super save/load) — old single-tank
		// saves load verbatim, no migration branch. Tanks 1..8 ride the per-index
		// "output_tank_i" keys here (the upstream NBT_TANK+".out."+i index form, in-repo
		// key naming); absent keys leave an empty tank.
		// ---------------------------------------------------------------------------

		/** The per-index key of output bank tank i, i >= 1 (tank 0 stays the base's). */
		public static String outputTankKey(int aIndex) {return TileEntityBase10MultiBlockMachine.NBT_OUTPUT_TANK + "_" + aIndex;}

		// The (CompoundTag) overrides compile on BOTH legs, unguarded — the shared-tree
		// chain is VIRTUAL: the 21.1 canonical provider hooks delegate in at the 01Root
		// (loadAdditional/saveAdditional(CompoundTag, Provider) → this chain,
		// TileEntityBase01Root:138-147), and the machine base's own 21.1 arm stops at
		// tank 0 — a subclass without its own (CompoundTag) override silently drops its
		// keys off the 21.1 serialize (the massfab live proof, work/p31-massfab d9dfcfb77).
		// THIS pair previously rode a //? if forge {…} else {…} duplicate of the same
		// bodies: behaviorally complete (the else arm rode the 21.1 leg — the 1.21.1
		// generated tree had it active), but the forge-looking header read as a forge-only
		// guard and misfired the massfab cross-card audit onto this file. Fork gone:
		// the bodies are leg-invariant (FluidTankGT read/write carry their own leg forks,
		// the ADR-P18 frozen NBT_ACCESS view), so ONE copy serves both legs.
		@Override
		protected void saveAdditional(CompoundTag aNBT) {
			super.saveAdditional(aNBT);
			mTankInput.writeToNBT(aNBT, NBT_INPUT_TANK);
			for (int i = 1; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, outputTankKey(i));
		}

		@Override
		public void load(CompoundTag aNBT) {
			super.load(aNBT);
			mTankInput.readFromNBT(aNBT, NBT_INPUT_TANK);
			for (int i = 1; i < mTanksOutput.length; i++) mTanksOutput[i].readFromNBT(aNBT, outputTankKey(i));
		}

		// ---------------------------------------------------------------------------
		// the offset helpers (the port's getOffset*N(mFacing) has no distance form — the
		// GT6 side tables are the Direction steps for the horizontal domain)
		// ---------------------------------------------------------------------------

		/** The cell {@code aDistance} steps BEHIND the controller (the {@code getOffset*N(mFacing, aDistance)} arithmetic). */
		public BlockPos offsetBy(byte aFacing, int aDistance) {
			Direction tBack = Direction.from3DDataValue(aFacing).getOpposite();
			return getBlockPos().relative(tBack, aDistance);
		}

		@Nullable
		private IFluidHandler fluidHandlerAt(BlockPos aPos) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(aPos);
			if (tNeighbor == null) return null;
			//? if forge {
			return tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.from3DDataValue(mFacing)).resolve().orElse(null);
			//?} else {
			/*return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, aPos, Direction.from3DDataValue(mFacing));
			 *///?}
		}
	}

	// ------------------------------------------------------------------------------------
	// the acceptance command (self-contained — the RCON arm of the two towers; the shared
	// GTMultiBlockCommand stays untouched, the per-family command file precedent)
	// ------------------------------------------------------------------------------------

	/** {@code /gt6distillation} — the tower acceptance arms: check | fluid fill/stat. */
	@Mod.EventBusSubscriber(modid = "gt6")
	public static final class Command {

		private Command() {}

		@SubscribeEvent
		public static void onRegisterCommands(RegisterCommandsEvent aEvent) {
			LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> tDist = net.minecraft.commands.Commands.literal("gt6distillation")
					.requires(aSource -> aSource.hasPermission(2));
			tDist.then(net.minecraft.commands.Commands.literal("check")
					.then(net.minecraft.commands.Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
							.executes(aContext -> check(aContext.getSource(),
									net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos")))));
			tDist.then(net.minecraft.commands.Commands.literal("fluid")
					.then(net.minecraft.commands.Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
							.then(net.minecraft.commands.Commands.literal("fill")
									// the ResourceLocation argument — the word()/string() readers stop at the
									// ':' of a namespaced id (the live r1 parse error); the gt6machine form
									.then(net.minecraft.commands.Commands.argument("fluid", net.minecraft.commands.arguments.ResourceLocationArgument.id())
											.executes(aContext -> fill(aContext.getSource(),
													net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
													net.minecraft.commands.arguments.ResourceLocationArgument.getId(aContext, "fluid"),
													1000))
											.then(net.minecraft.commands.Commands.argument("mB", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
													.executes(aContext -> fill(aContext.getSource(),
															net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"),
															net.minecraft.commands.arguments.ResourceLocationArgument.getId(aContext, "fluid"),
															com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(aContext, "mB"))))))
							.then(net.minecraft.commands.Commands.literal("stat")
									.executes(aContext -> fluidStat(aContext.getSource(),
											net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(aContext, "pos"))))));
			aEvent.getDispatcher().register(tDist);
			LOGGER.info("Registered GT6 distillation command /gt6distillation (check|fluid fill|fluid stat)");
		}

		@Nullable
		private static TileEntityDistillationTower towerAt(net.minecraft.commands.CommandSourceStack aSource, BlockPos aPos) {
			return aSource.getLevel().getBlockEntity(aPos) instanceof TileEntityDistillationTower tTower ? tTower : null;
		}

		/** The structure verdict + the linked-part census + the machine report (the cokeoven check shape). */
		private static int check(net.minecraft.commands.CommandSourceStack aSource, BlockPos aPos) {
			TileEntityDistillationTower tTower = towerAt(aSource, aPos);
			if (tTower == null) {
				aSource.sendFailure(Component.literal("No distillation tower at " + aPos.toShortString()));
				return 0;
			}
			String tVerdict;
			if (tTower.checkStructure(false)) {
				tVerdict = "Structure is formed already!";
			} else {
				tVerdict = tTower.checkStructure(true) ? "Structure did form just now!" : "Structure did not form!";
			}
			int tLinked = 0;
			BlockPos tCentre = tTower.centre();
			for (int tY = -1; tY <= 7; tY++) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
				BlockPos tCell = tCentre.offset(i, tY, j);
				if (aSource.getLevel().getBlockEntity(tCell) instanceof MultiBlockPartBlockEntity tPart && tPart.getTarget(false) == tTower) tLinked++;
			}
			boolean tBlockFormed = aSource.getLevel().getBlockState(tTower.getBlockPos()).getValue(TileEntityBase10MultiBlockBase.FORMED);
			String tMachine = String.format("machine: progress=%d/%d energy=%d min_energy=%d window=%d/%d/%d timer=%d active=%s stopped=%s in_tank=[%s] out_tank=[%s]",
					tTower.mProgress, tTower.mMaxProgress, tTower.mEnergy, tTower.mMinEnergy,
					tTower.mInputMin, tTower.mInput, tTower.mInputMax, tTower.getTimer(),
					tTower.mActive, tTower.mStopped, tankText(tTower.mTankInput), outBankText(tTower));
			String tReport = String.format("GT6 %s at %s: %s okay=%s block_formed=%s linked_parts=%d/81 | %s",
					tTower.getTileEntityName(), tTower.getBlockPos().toShortString(), tVerdict, tTower.mStructureOkay,
					tBlockFormed, tLinked, tMachine);
			if (!tTower.mStructureOkay || !tBlockFormed) {
				aSource.sendFailure(Component.literal(tReport));
				return 0;
			}
			aSource.sendSuccess(() -> Component.literal(tReport), false);
			LOGGER.info(tReport);
			return com.mojang.brigadier.Command.SINGLE_SUCCESS;
		}

		/** Fills the input tank THROUGH the tower's own fluid capability (the fill face under test). */
		private static int fill(net.minecraft.commands.CommandSourceStack aSource, BlockPos aPos, net.minecraft.resources.ResourceLocation aFluidId, int aAmount) {
			TileEntityDistillationTower tTower = towerAt(aSource, aPos);
			if (tTower == null) {
				aSource.sendFailure(Component.literal("No distillation tower at " + aPos.toShortString()));
				return 0;
			}
			Fluid tFluid = ForgeRegistries.FLUIDS.getValue(aFluidId);
			if (tFluid == null || tFluid.defaultFluidState().isEmpty()) {
				aSource.sendFailure(Component.literal("Unknown fluid: " + aFluidId));
				return 0;
			}
			//? if forge {
			IFluidHandler tHandler = tTower.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
			//?} else {
			/*IFluidHandler tHandler = tTower.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tTower.getBlockPos(), null);
			 *///?}
			if (tHandler == null) {
				aSource.sendFailure(Component.literal("CAPABILITY MISSING: the tower exposes no FLUID_HANDLER"));
				return 0;
			}
			int tAccepted = tHandler.fill(new FluidStack(tFluid, aAmount), FluidAction.EXECUTE);
			String tLine = String.format("GT6 %s fluid fill at %s: accepted %d/%d mB of %s%s, in_tank=[%s]",
					tTower.getTileEntityName(), tTower.getBlockPos().toShortString(), tAccepted, aAmount, aFluidId,
					tAccepted == 0 ? " (REJECTED)" : " (ACCEPTED)", tankText(tTower.mTankInput));
			if (tAccepted <= 0) {
				aSource.sendFailure(Component.literal(tLine));
				return 0;
			}
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return com.mojang.brigadier.Command.SINGLE_SUCCESS;
		}

		/** The tank census (the machine-report tank shape, both tanks). */
		private static int fluidStat(net.minecraft.commands.CommandSourceStack aSource, BlockPos aPos) {
			TileEntityDistillationTower tTower = towerAt(aSource, aPos);
			if (tTower == null) {
				aSource.sendFailure(Component.literal("No distillation tower at " + aPos.toShortString()));
				return 0;
			}
			String tLine = String.format("GT6 %s fluid stat at %s: in_tank=[%s] out_tank=[%s]",
					tTower.getTileEntityName(), tTower.getBlockPos().toShortString(), tankText(tTower.mTankInput), outBankText(tTower));
			aSource.sendSuccess(() -> Component.literal(tLine), false);
			LOGGER.info(tLine);
			return com.mojang.brigadier.Command.SINGLE_SUCCESS;
		}

		private static String tankText(FluidTankGT aTank) {
			if (aTank.isEmpty() || aTank.fluid() == null) return "-";
			return aTank.amount() + "mB " + net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aTank.fluid().getFluid());
		}

		/**
		 * The output-library census — every NON-EMPTY bank tank joined with ", " (the bank
		 * index in brackets); an empty bank renders as "-" exactly like the old single-tank
		 * empty form (the chain asserts keep their shape).
		 */
		private static String outBankText(TileEntityDistillationTower aTower) {
			StringBuilder rText = new StringBuilder();
			for (int i = 0; i < aTower.mTanksOutput.length; i++) {
				String tTank = tankText(aTower.mTanksOutput[i]);
				if ("-".equals(tTank)) continue;
				if (rText.length() > 0) rText.append(", ");
				rText.append("[").append(i).append("]").append(tTank);
			}
			return rText.length() == 0 ? "-" : rText.toString();
		}
	}

	private GT6Distillation() {}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Crucibles form). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		//21.1: Mod.EventBusSubscriber.Bus died with the annotation rework (the GT6Crucibles fork precedent).
		*///?}
		BLOCKS.register(tModBus);
		ITEMS.register(tModBus);
		BLOCK_ENTITY_TYPES.register(tModBus);
	}
}
