package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.energy.GTCrankBlock;
import gregtech6.block.energy.GTDieselEngineBlock;
import gregtech6.block.energy.GTTransformerRotationBlock;
import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.registry.GT6Boilers;
import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.GTCrankBlockEntity;
import gregtech6.tileentity.energy.GTDieselEngineBlockEntity;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.example.GTExampleChestBlockEntity;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregtech6.tileentity.sensors.GTSensorBlockEntity;

/**
 * Offline tests for task p28-singleblock-facing-canon: EVERY singleblock family places its
 * FRONT TOWARDS the placer — the {@link GT6PlacementFacing} canon (the view OPPOSITE; the
 * upstream UT.java:1751-1753 getHorizontalForPlayerPlacing over CS.java:638
 * COMPASS_DIRECTIONS, the vanilla furnace {@code getHorizontalDirection().getOpposite()}
 * idiom). The P27 disease (11 families, 19 sites) wrote the RAW VIEW direction
 * ({@code UseOnContext.getHorizontalDirection} = {@code player.getDirection()},
 * UseOnContext.java:70-72 — "the front looks WITH the player"); this suite pins the canon
 * at BOTH seams of every family the card touched:
 * <ol>
 * <li>the pure-function four-view table (view N → front S byte 3, view S → front N byte 2,
 *     view W → front E byte 5, view E → front W byte 4 — the GT6 side order ==
 *     {@code Direction.get3DDataValue()}) plus the Base10 delegation identity (card ②:
 *     the multiblock form rides the SAME seam, byte-identical);</li>
 * <li>the BE seam: {@code setFacingFromPlacement} driven by a REAL Player double (the
 *     offline-constructible {@link ViewPlayer}: Entity.getDirection() =
 *     {@code Direction.fromYRot(getYRot())}, Entity.java:2716; the Player ctor
 *     Player.java:180 needs only a Level — the two abstracts isSpectator/isCreative fall
 *     to the double) for the eight placement-facing families: BasicMachine / Oven / ACT /
 *     ExampleChest (machines) and Crank / SteamEngine / DieselEngine / BoilerTank
 *     (kinetics);</li>
 * <li>the BlockState seam: {@code getStateForPlacement} over a REAL
 *     {@link BlockPlaceContext} (the Player.java:17 direct ctor — reachable offline once
 *     the Player double exists; the card's "degrade to BE-only" fallback NOT needed) for
 *     ALL ten touched blocks — the pair-write law: state and BE MUST resolve the same
 *     side (the P27 client-prediction lesson);</li>
 * <li>the sensor mirror chain: Block.setPlacedBy re-mirrors the placed state into the BE
 *     {@code wrenchSetFacing} — driving the BLOCK seam exercises the whole pair.</li>
 * </ol>
 *
 * <p>The transformer and burning-box families carry NO {@code setFacingFromPlacement}
 * (their BE mirrors sync FROM the state — GTTransformerRotationBlockEntity
 * syncFacingFromState :164; BurningBoxBlock has no setPlacedBy at all): the state-seam pin
 * IS their whole placement semantics. RCON note (the p26 live lesson): {@code /setblock}
 * never triggers getStateForPlacement/setPlacedBy — the RCON chains place with explicit
 * facing and stay blind to this mapping by construction; the live user-facing fix rides
 * the offline legs here, the RCON legs remain the no-regression sweep.
 */
public class GT6SingleBlockFacingIntegrityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);
	static final Direction[] VIEWS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

	/** The GT6 side order == Direction.get3DDataValue() (Direction.java:119). */
	static byte sideOf(Direction aDirection) {
		return (byte) aDirection.get3DDataValue();
	}

	// ------------------------------------------------------------------
	// the fixtures
	// ------------------------------------------------------------------

	/**
	 * The offline Player double. The FORGE Entity ctor carries a patch (hit live as
	 * FluidType.lambda$static$0 over the Entity ctor chain) that reads the never-fired
	 * Forge registries — the constructor route is a hard wall offline (the
	 * GTBoilerChiselItemTest "offline Player wall" record). The double therefore skips ALL
	 * constructors via the in-repo Unsafe allocation form (the GT6BasicMachineMUIPanelTest
	 * precedent) and carries EXACTLY the two fields the placement seams read — nothing
	 * else is ever touched:
	 * <ul>
	 * <li>{@code Entity.level} (Entity.java:152) — {@code level()} for the
	 *     BlockPlaceContext world reads;</li>
	 * <li>{@code Entity.yRot} (Entity.java:160) — {@code getDirection()} =
	 *     {@code Direction.fromYRot(getYRot())} (Entity.java:2716), the yaw-driven view
	 *     exactly like live placement.</li>
	 * </ul>
	 */
	public static final class ViewPlayer extends Player {
		private ViewPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

		/** The ctor-free Player double over a stub level, viewing along {@code aView}. */
		public static ViewPlayer lookAt(Level aLevel, Direction aView) {
			try {
				java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tTheUnsafe.setAccessible(true);
				sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
				ViewPlayer tPlayer = (ViewPlayer) tUnsafe.allocateInstance(ViewPlayer.class);
				entityField(tPlayer, "level", aLevel);
				entityField(tPlayer, "yRot", aView.toYRot());
				return tPlayer;
			} catch (ReflectiveOperationException aE) {
				throw new IllegalStateException("the offline Player double failed", aE);
			}
		}

		private static void entityField(Player aPlayer, String aName, Object aValue) throws ReflectiveOperationException {
			java.lang.reflect.Field tField = net.minecraft.world.entity.Entity.class.getDeclaredField(aName);
			tField.setAccessible(true);
			tField.set(aPlayer, aValue); // boxed write is fine for the non-final level / primitive yRot
		}

		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
	}

	/** The MachineLevel with the fixture BE mounted at {@link #POS} (the ChiselClickLevel shape). */
	public static final class MountedLevel extends GTMachinesOfflineTestBase.MachineLevel {
		private final BlockEntity mBe;
		public MountedLevel(BlockEntity aBe) {
			super(new GTRecipesOfflineTestBase.TestRecipeManager());
			mBe = aBe;
		}
		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return aPos.equals(POS) ? mBe : null;
		}
	}

	static BlockEntityType<TileEntityBasicMachine> sBasicMachineType;
	static BlockEntityType<TileEntityOven> sOvenType;
	static BlockEntityType<TileEntityAdvancedCraftingTable> sActType;
	static BlockEntityType<GTExampleChestBlockEntity> sChestType;
	static BlockEntityType<GTCrankBlockEntity> sCrankType;
	static BlockEntityType<GTSteamEngineBlockEntity> sSteamType;
	static BlockEntityType<GTDieselEngineBlockEntity> sDieselType;
	static BlockEntityType<FixtureBoiler> sBoilerType;
	static BlockEntityType<FixtureSensor> sSensorType;

	/** The boiler over a vanilla-block BET (the GTBoilerChiselItemTest.FixtureBoiler recipe). */
	public static final class FixtureBoiler extends GTBoilerTankBlockEntity {
		public FixtureBoiler(BlockPos aPos, BlockState aState) { super(sBoilerType, aPos, aState); }
	}

	/** The sensor BE fixture (protected ctor — the subclass unlocks the offline BET; the sample pair is the no-op probe). */
	public static final class FixtureSensor extends GTSensorBlockEntity {
		public FixtureSensor(BlockPos aPos, BlockState aState) { super(sSensorType, aPos, aState); }
		@Override public long getCurrentValue(net.minecraft.world.level.block.entity.BlockEntity aTarget) { return 0; }
		@Override public long getCurrentMax(net.minecraft.world.level.block.entity.BlockEntity aTarget) { return 15; }
		@Override public String getTileEntityName() { return "test_sensor"; }
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		// the vanilla boot + BET unfreeze rode the superclass @BeforeAll; then the BLOCK
		// registry write window (the GT6CFoamFamilyTest recipe: the Block ctor registers
		// its intrusive holder, NamespacedWrapper.validateWrite rejects a frozen registry)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GT6RecipeMaps.init(); // the BasicMachine ctor requires a non-null RecipeMap (upstream :107 gate)
		// every BET closure is the explicit 3-arg form (the GTMachinesOfflineTestBase
		// recipe): the 2-arg BE ctors resolve the never-registered RegistryObjects offline,
		// and 21.1 validates the type/state pair in the BE ctor — a synthetic type it is
		BlockEntityType<TileEntityBasicMachine>[] tBasic = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tBasic[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tBasic[0], aPos, aState, GT6RecipeMaps.SHREDDER, 1, false, null),
				Blocks.BRICKS).build(null);
		sBasicMachineType = tBasic[0];
		BlockEntityType<TileEntityOven>[] tOven = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tOven[0] = BlockEntityType.Builder.of((aPos, aState) -> new TileEntityOven(tOven[0], aPos, aState), Blocks.BRICKS).build(null);
		sOvenType = tOven[0];
		BlockEntityType<TileEntityAdvancedCraftingTable>[] tAct = (BlockEntityType<TileEntityAdvancedCraftingTable>[]) new BlockEntityType<?>[1];
		tAct[0] = BlockEntityType.Builder.of((aPos, aState) -> new TileEntityAdvancedCraftingTable(tAct[0], aPos, aState), Blocks.BRICKS).build(null);
		sActType = tAct[0];
		BlockEntityType<GTExampleChestBlockEntity>[] tChest = (BlockEntityType<GTExampleChestBlockEntity>[]) new BlockEntityType<?>[1];
		tChest[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTExampleChestBlockEntity(tChest[0], aPos, aState), Blocks.STONE).build(null);
		sChestType = tChest[0];
		BlockEntityType<GTCrankBlockEntity>[] tCrank = (BlockEntityType<GTCrankBlockEntity>[]) new BlockEntityType<?>[1];
		tCrank[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTCrankBlockEntity(tCrank[0], aPos, aState), Blocks.STONE).build(null);
		sCrankType = tCrank[0];
		BlockEntityType<GTSteamEngineBlockEntity>[] tSteam = (BlockEntityType<GTSteamEngineBlockEntity>[]) new BlockEntityType<?>[1];
		tSteam[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTSteamEngineBlockEntity(tSteam[0], aPos, aState), Blocks.STONE).build(null);
		sSteamType = tSteam[0];
		BlockEntityType<GTDieselEngineBlockEntity>[] tDiesel = (BlockEntityType<GTDieselEngineBlockEntity>[]) new BlockEntityType<?>[1];
		tDiesel[0] = BlockEntityType.Builder.of((aPos, aState) -> new GTDieselEngineBlockEntity(tDiesel[0], aPos, aState), Blocks.STONE).build(null);
		sDieselType = tDiesel[0];
		BlockEntityType<FixtureBoiler>[] tBoiler = (BlockEntityType<FixtureBoiler>[]) new BlockEntityType<?>[1];
		tBoiler[0] = BlockEntityType.Builder.of(FixtureBoiler::new, Blocks.STONE).build(null);
		sBoilerType = tBoiler[0];
		BlockEntityType<FixtureSensor>[] tSensor = (BlockEntityType<FixtureSensor>[]) new BlockEntityType<?>[1];
		tSensor[0] = BlockEntityType.Builder.of(FixtureSensor::new, Blocks.STONE).build(null);
		sSensorType = tSensor[0];
	}

	// ------------------------------------------------------------------
	// ① the pure-function four-view table + the Base10 delegation identity
	// ------------------------------------------------------------------

	@Test
	public void canonTableEveryViewYieldsTheFrontTowardsThePlacer() {
		// the four live horizontal views, spelled out row by row (the placer stands OPPOSITE
		// the view; CS.java:638 COMPASS_DIRECTIONS: yaw 180 (view north) → SIDE_SOUTH)
		assertEquals(Direction.SOUTH, GT6PlacementFacing.facingTowardsPlacer(Direction.NORTH), "view north  -> front south");
		assertEquals(Direction.NORTH, GT6PlacementFacing.facingTowardsPlacer(Direction.SOUTH), "view south  -> front north");
		assertEquals(Direction.EAST,  GT6PlacementFacing.facingTowardsPlacer(Direction.WEST),  "view west   -> front east");
		assertEquals(Direction.WEST,  GT6PlacementFacing.facingTowardsPlacer(Direction.EAST),  "view east   -> front west");
		// the byte form over the GT6 side order: N=2 / S=3 / W=4 / E=5
		assertEquals(3, GT6PlacementFacing.placementFacing(Direction.NORTH), "view north  -> front 3 (south)");
		assertEquals(2, GT6PlacementFacing.placementFacing(Direction.SOUTH), "view south  -> front 2 (north)");
		assertEquals(5, GT6PlacementFacing.placementFacing(Direction.WEST),  "view west   -> front 5 (east)");
		assertEquals(4, GT6PlacementFacing.placementFacing(Direction.EAST),  "view east   -> front 4 (west)");
	}

	@Test
	public void base10PlacementFacingRidesTheSameCanonSeam() {
		// card ②: the multiblock form delegates to the seam — byte-identical over the whole
		// six-side domain (the live placement domain is the horizontal four)
		for (Direction tView : Direction.values()) {
			assertEquals(TileEntityBase10MultiBlockBase.placementFacing(tView),
					GT6PlacementFacing.placementFacing(tView),
					"the Base10 delegation is identity over " + tView);
		}
	}

	// ------------------------------------------------------------------
	// ② the BE seam — setFacingFromPlacement, one representative per family
	// ------------------------------------------------------------------

	@Test
	public void machineFamilyBeFacesThePlacer() {
		Level tLevel = new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager());
		for (Direction tView : VIEWS) {
			byte tExpected = sideOf(tView.getOpposite());
			Player tPlayer = ViewPlayer.lookAt(tLevel, tView);

			TileEntityBasicMachine tMachine = sBasicMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
			tMachine.setLevel(tLevel);
			tMachine.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tMachine.getFacing(), "BasicMachine front towards the placer, view " + tView);

			TileEntityOven tOven = sOvenType.create(POS, Blocks.BRICKS.defaultBlockState());
			tOven.setLevel(tLevel);
			tOven.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tOven.getFacing(), "Oven front towards the placer, view " + tView);

			TileEntityAdvancedCraftingTable tAct = sActType.create(POS, Blocks.BRICKS.defaultBlockState());
			tAct.setLevel(tLevel);
			tAct.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tAct.getFacing(), "AdvancedCraftingTable front towards the placer, view " + tView);

			GTExampleChestBlockEntity tChest = sChestType.create(POS, Blocks.STONE.defaultBlockState());
			tChest.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tChest.getFacing(), "ExampleChest front towards the placer, view " + tView);
		}
	}

	@Test
	public void energyFamilyBeFacesThePlacer() {
		Level tLevel = new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager());
		for (Direction tView : VIEWS) {
			byte tExpected = sideOf(tView.getOpposite());
			Player tPlayer = ViewPlayer.lookAt(tLevel, tView);

			GTCrankBlockEntity tCrank = sCrankType.create(POS, Blocks.STONE.defaultBlockState());
			tCrank.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tCrank.getFacing(), "Crank front towards the placer, view " + tView);

			GTSteamEngineBlockEntity tSteam = sSteamType.create(POS, Blocks.STONE.defaultBlockState());
			tSteam.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tSteam.getFacing(), "SteamEngine front towards the placer, view " + tView);

			GTDieselEngineBlockEntity tDiesel = sDieselType.create(POS, Blocks.STONE.defaultBlockState());
			tDiesel.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tDiesel.getFacing(), "DieselEngine front towards the placer, view " + tView);

			FixtureBoiler tBoiler = sBoilerType.create(POS, Blocks.STONE.defaultBlockState());
			tBoiler.setFacingFromPlacement(tPlayer);
			assertEquals(tExpected, tBoiler.getFacing(), "BoilerTank front towards the placer, view " + tView);
		}
	}

	// ------------------------------------------------------------------
	// ③ the BlockState seam — getStateForPlacement over a REAL BlockPlaceContext
	// ------------------------------------------------------------------

	/** The shared placement context: the yaw is re-read per look() (getHorizontalDirection = player.getDirection()). */
	private static BlockPlaceContext placeContext(ViewPlayer aPlayer) {
		return new BlockPlaceContext(aPlayer, InteractionHand.MAIN_HAND, new ItemStack(Items.BRICKS),
				new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false));
	}

	@Test
	public void machineFamilyBlockStateMirrorsTheCanon() {
		Level tLevel = new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager());
		for (Direction tView : VIEWS) {
			Direction tExpected = tView.getOpposite();
			// a fresh double per view: the context reads the yaw once at construction
			BlockPlaceContext tCtx = placeContext(ViewPlayer.lookAt(tLevel, tView));

			assertEquals(tExpected, new GTBasicMachineBlock(BlockBehaviour.Properties.of(), () -> null)
					.getStateForPlacement(tCtx).getValue(GTBasicMachineBlock.FACING), "BasicMachine state, view " + tView);
			assertEquals(tExpected, new GTOvenBlock(BlockBehaviour.Properties.of())
					.getStateForPlacement(tCtx).getValue(GTOvenBlock.FACING), "Oven state, view " + tView);
			assertEquals(tExpected, new GTAdvancedCraftingTableBlock(BlockBehaviour.Properties.of())
					.getStateForPlacement(tCtx).getValue(GTAdvancedCraftingTableBlock.FACING), "ACT state, view " + tView);
		}
	}

	@Test
	public void energyFamilyBlockStateMirrorsTheCanon() {
		Level tLevel = new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager());
		// the row fixtures (the public records over trivial fields; the GT6BurningBoxes
		// SOLID_ROWS.get(0) recipe for the burning box)
		GT6Kinetics.SteamEngineBlock tSteamBlock = new GT6Kinetics.SteamEngineBlock(new GT6Kinetics.SteamEngineRow(
				"test.steam", "wrought_iron", "Wrought Iron", false, (short)100, 10000, 32, 6.0F, 8.0F, false),
				BlockBehaviour.Properties.of());
		GT6Boilers.BoilerTankBlock tBoilerBlock = new GT6Boilers.BoilerTankBlock(new GT6Boilers.BoilerRow(
				"test.boiler", 0, 320, new GT6Boilers.BoilerMaterial("steel", "Steel", 6.0F), false),
				BlockBehaviour.Properties.of());
		GT6BurningBoxes.BurningBoxBlock tBurningBlock = new GT6BurningBoxes.BurningBoxBlock(
				GT6BurningBoxes.SOLID_ROWS.get(0), BlockBehaviour.Properties.of());
		for (Direction tView : VIEWS) {
			Direction tExpected = tView.getOpposite();
			BlockPlaceContext tCtx = placeContext(ViewPlayer.lookAt(tLevel, tView));

			assertEquals(tExpected, new GTCrankBlock(BlockBehaviour.Properties.of())
					.getStateForPlacement(tCtx).getValue(GTCrankBlock.FACING), "Crank state, view " + tView);
			assertEquals(tExpected, tSteamBlock.getStateForPlacement(tCtx).getValue(GT6Kinetics.SteamEngineBlock.FACING),
					"SteamEngine state, view " + tView);
			assertEquals(tExpected, new GTDieselEngineBlock(BlockBehaviour.Properties.of(), new GT6Kinetics.DieselSpec("Steel", "Steel", 256))
					.getStateForPlacement(tCtx).getValue(GTDieselEngineBlock.FACING), "DieselEngine state, view " + tView);
			assertEquals(tExpected, tBoilerBlock.getStateForPlacement(tCtx).getValue(GT6Boilers.BoilerTankBlock.FACING),
					"BoilerTank state, view " + tView);
			// the transformer: the old comment declared "FRONT faces the player" while the
			// code wrote the raw view — the same-disease self-witness, now canonical
			assertEquals(tExpected, new GTTransformerRotationBlock(BlockBehaviour.Properties.of())
					.getStateForPlacement(tCtx).getValue(GTTransformerRotationBlock.FACING), "Transformer state, view " + tView);
			assertEquals(tExpected, tBurningBlock.getStateForPlacement(tCtx).getValue(GT6BurningBoxes.BurningBoxBlock.FACING),
					"BurningBox state, view " + tView);
			// the sensor FACING rides BlockStateProperties.FACING (the 0..5 property) — the
			// placed value is still the horizontal opposite
			assertEquals(tExpected, new GTSensorBlock(() -> null, BlockBehaviour.Properties.of())
					.getStateForPlacement(tCtx).getValue(GTSensorBlock.FACING), "Sensor state, view " + tView);
		}
	}

	/**
	 * The sensor mirror chain: the block's setPlacedBy re-mirrors the PLACED state into the
	 * BE wrenchSetFacing (the probe face folds to OPOS) — driving the block seam exercises
	 * the whole pair, so the single-point state fix is proven to reach the BE.
	 */
	@Test
	public void sensorBlockPlacedByMirrorsTheCanonIntoTheBe() {
		for (Direction tView : VIEWS) {
			byte tExpected = sideOf(tView.getOpposite());
			FixtureSensor tSensor = sSensorType.create(POS, Blocks.STONE.defaultBlockState());
			MountedLevel tLevel = new MountedLevel(tSensor);
			GTSensorBlock tBlock = new GTSensorBlock(() -> null, BlockBehaviour.Properties.of());
			ViewPlayer tPlayer = ViewPlayer.lookAt(tLevel, tView);
			BlockState tPlaced = tBlock.getStateForPlacement(placeContext(tPlayer));
			tBlock.setPlacedBy(tLevel, POS, tPlaced, tPlayer, ItemStack.EMPTY);
			assertEquals(tExpected, tSensor.getFacing(), "Sensor BE mirrors the canonical state, view " + tView);
		}
	}
}
