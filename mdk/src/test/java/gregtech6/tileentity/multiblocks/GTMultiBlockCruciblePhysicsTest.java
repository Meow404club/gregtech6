package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.ITileEntityMold;
import gregapi.util.CruciblePhysics;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The LARGE CRUCIBLE offline physics suite (task p26-crucible-multiblock acceptance ②/④):
 * the BE-side consumption of the A-card {@link CruciblePhysics} LARGE parameter face —
 * the HU charge step, the capacity gate, the boiling evaporation, the Invar alloy
 * formation, the through-wall mold proxy, the melt-down WARNING latch and the full
 * meltdown path (content trash + the 3x3x3 cavity lava, upstream :367-377).
 *
 * <p>All physics run over the real MT dataset (the CruciblePhysicsTest posture:
 * MT.init() + the crucible-alloy graph wiring), the structure rides the stub world.
 */
public class GTMultiBlockCruciblePhysicsTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestCrucible> sCrucibleType;
	static BlockEntityType<CrucibleWallBlockEntity> sWallType;

	/** The concrete test BE — the crucible over a vanilla-block BET, wall and env bound. */
	public static final class TestCrucible extends TileEntityCrucible {
		public TestCrucible(BlockPos aPos, BlockState aState) {
			super(sCrucibleType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
	}

	/**
	 * The recording Mold double (the pour target of the through-wall proxy) — takes at
	 * most {@code mDemand} units per pour, never more (the real Mold contract, so a
	 * drained-to-zero stack answers 0 and the pour ends).
	 */
	static final class RecordingMold implements ITileEntityMold {
		OreDictMaterialStack mPoured;
		long mPouredTemperature;
		byte mPouredSide;
		long mDemand = 0;

		@Override public boolean isMoldInputSide(byte aSide) { return true; }
		@Override public long getMoldMaxTemperature() { return 3000; }
		@Override public long getMoldRequiredMaterialUnits() { return 1; }
		@Override
		public long fillMold(OreDictMaterialStack aMaterial, long aTemperature, byte aSide) {
			mPoured = aMaterial;
			mPouredTemperature = aTemperature;
			mPouredSide = aSide;
			return aMaterial.mAmount >= mDemand ? mDemand : 0; // never overtake
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildPhysicsFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestCrucible>[] tHolder = (BlockEntityType<TestCrucible>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestCrucible::new, Blocks.BRICKS).build(null);
		sCrucibleType = tHolder[0];
		BlockEntityType<CrucibleWallBlockEntity>[] tWallHolder = (BlockEntityType<CrucibleWallBlockEntity>[]) new BlockEntityType<?>[1];
		tWallHolder[0] = BlockEntityType.Builder.of(CrucibleWallBlockEntity::new, Blocks.BRICKS).build(null);
		sWallType = tWallHolder[0];
		// the real MT dataset (the CruciblePhysicsTest posture) — the physics and the
		// alloy graph must be live before any stack is built
		MaterialRegistry.INSTANCE.open();
		MT.init();
		MaterialGraph.applyCrucibleAlloyReferences();
	}

	// ------------------------------------------------------------------
	// the fixture: a formed crucible at (100, 64, 100)
	// ------------------------------------------------------------------

	private record Formed(MultiBlockLevel level, TestCrucible crucible) {}

	private static Formed formedCrucible() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) placePart(tLevel, new BlockPos(100 + tDX, 64 + tY, 100 + tDZ));
		}
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false), "the fixture structure forms");
		return new Formed(tLevel, tCrucible);
	}

	private static long totalOf(List<OreDictMaterialStack> aList) {
		return CruciblePhysics.total(aList);
	}

	// ------------------------------------------------------------------
	// the wall-relay fixture: every ring cell carries the relaying wall BE —
	// the checker's binding then writes the per-layer usage mode into each
	// (y+0 ONLY_ENERGY_IN / y+1 ONLY_CRUCIBLE / y+2 ONLY_ITEM_FLUID)
	// ------------------------------------------------------------------

	private static CrucibleWallBlockEntity placeCrucibleWall(MultiBlockLevel aLevel, BlockPos aPos) {
		CrucibleWallBlockEntity tPart = new CrucibleWallBlockEntity(sWallType, aPos, Blocks.BRICKS.defaultBlockState());
		tPart.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPart);
		return tPart;
	}

	private record RelayFormed(MultiBlockLevel level, TestCrucible crucible) {}

	private static RelayFormed formedCrucibleWithRelayWalls() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		java.util.Map<BlockPos, CrucibleWallBlockEntity> tWalls = new java.util.HashMap<>();
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) {
				tWalls.put(new BlockPos(100 + tDX, 64 + tY, 100 + tDZ),
						placeCrucibleWall(tLevel, new BlockPos(100 + tDX, 64 + tY, 100 + tDZ)));
			}
		}
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false), "the relay-wall fixture structure forms");
		// the binding wrote the usage modes (the checkAndSetTarget setTarget write)
		assertEquals(MultiBlockPartBlockEntity.ONLY_CRUCIBLE, tWalls.get(new BlockPos(101, 65, 100)).mMode,
				"the y+1 ring carries ONLY_CRUCIBLE after the bind");
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tWalls.get(new BlockPos(101, 64, 100)).mMode,
				"the y+0 ring carries ONLY_ENERGY_IN after the bind");
		return new RelayFormed(tLevel, tCrucible);
	}

	// ------------------------------------------------------------------
	// the through-wall crucible relay (upstream MultiBlockPart :686-692)
	// ------------------------------------------------------------------

	@Test
	public void wallRelayPoursThroughTheY1Ring() {
		RelayFormed tF = formedCrucibleWithRelayWalls();
		tF.crucible().mTemperature = 2000;
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, 2 * gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tIron, 2000));

		RecordingMold tMold = new RecordingMold();
		tMold.mDemand = gregapi.data.CS.U;
		CrucibleWallBlockEntity tWall = (CrucibleWallBlockEntity) tF.level().getBlockEntity(new BlockPos(101, 65, 100));
		// the mold clicks the WALL; the wall forwards controller-ward
		assertTrue(tWall.fillMoldAtSide(tMold, (byte)2, (byte)3), "the y+1 wall relays the pour");
		assertSame(MT.Fe, tMold.mPoured.mMaterial, "the Fe stack reached the mold through the wall");
		assertEquals(gregapi.data.CS.U, totalOf(tF.crucible().mContent), "the pour subtracted one unit");
	}

	@Test
	public void wallRelayRefusesThePourOnTheWrongRings() {
		RelayFormed tF = formedCrucibleWithRelayWalls();
		tF.crucible().mTemperature = 2000;
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tIron, 2000));

		RecordingMold tMold = new RecordingMold();
		tMold.mDemand = gregapi.data.CS.U;
		// the y+2 ring is ONLY_ITEM_FLUID — the NO_CRUCIBLE bit kills the relay first (:688)
		CrucibleWallBlockEntity tFeedWall = (CrucibleWallBlockEntity) tF.level().getBlockEntity(new BlockPos(101, 66, 100));
		assertTrue((tFeedWall.mMode & MultiBlockPartBlockEntity.NO_CRUCIBLE) != 0,
				"the y+2 mode carries NO_CRUCIBLE");
		assertFalse(tFeedWall.fillMoldAtSide(tMold, (byte)2, (byte)3), "the feed-layer wall refuses the pour");
		assertNull(tMold.mPoured, "the mold was never contacted through the wrong ring");
	}

	// ------------------------------------------------------------------
	// the energy relay through the y+0 ring (the burning-box HU intake; the
	// inherited HeatTransmitterBlockEntity face, mode-gated NO_ENERGY_IN)
	// ------------------------------------------------------------------

	@Test
	public void wallRelayInjectsHUThroughTheY0Ring() {
		RelayFormed tF = formedCrucibleWithRelayWalls();
		assertEquals(0, tF.crucible().mEnergy, "the buffer starts empty");
		// the burning box's emit walk lands on the wall's ITileEntityEnergy face
		CrucibleWallBlockEntity tWall = (CrucibleWallBlockEntity) tF.level().getBlockEntity(new BlockPos(101, 64, 100));
		long tBooked = tWall.doEnergyInjection(gregapi.data.TD.Energy.HU, (byte)0, (byte)1, 100, true);
		assertEquals(100, tBooked, "the HU packet books through the wall");
		assertEquals(100, tF.crucible().mEnergy, "the controller buffer received the charge (the :704 doInject arm)");
	}

	@Test
	public void wallRelayRefusesEnergyOnTheY1Ring() {
		RelayFormed tF = formedCrucibleWithRelayWalls();
		// ONLY_CRUCIBLE carries NO_ENERGY_IN — the mold layer is not an intake layer
		CrucibleWallBlockEntity tWall = (CrucibleWallBlockEntity) tF.level().getBlockEntity(new BlockPos(101, 65, 100));
		long tBooked = tWall.doEnergyInjection(gregapi.data.TD.Energy.HU, (byte)0, (byte)1, 100, true);
		assertEquals(0, tBooked, "the mold-layer wall refuses the energy packet");
		assertEquals(0, tF.crucible().mEnergy, "nothing reached the buffer");
	}

	// ------------------------------------------------------------------
	// the HU charge step (upstream :353-365 via CruciblePhysics.tickHeat)
	// ------------------------------------------------------------------

	@Test
	public void heatChargesFromTheEnergyBuffer() {
		Formed tF = formedCrucible();
		tF.crucible().mTemperature = 300;
		// exactly five paid Kelvins (the :301 requiredEnergy over the Steel shell alone)
		long tRequired = CruciblePhysics.requiredEnergy(tF.crucible().shellWeight(), TileEntityCrucible.KG_PER_ENERGY);
		tF.crucible().mEnergy = tRequired * 5;
		tF.crucible().onTick(1, true);
		assertEquals(305, tF.crucible().mTemperature, "five paid Kelvins (300 + 5)");
		assertEquals(0, tF.crucible().mEnergy, "the buffer paid exactly five charges");
	}

	@Test
	public void outOfSupplyRelaxesTowardTheEnvironment() {
		Formed tF = formedCrucible();
		tF.crucible().mTemperature = 1500;
		tF.crucible().mEnergy = 0;
		tF.crucible().mCooldown = 1; // the grace already burnt
		// the first tick resets the countdown to 10 and cools 1 K; then 1 K per 10 ticks
		tF.crucible().onTick(1, true);
		long tAfterFirst = tF.crucible().mTemperature;
		assertEquals(1499, tAfterFirst, "the first out-of-supply window cools one Kelvin");
		for (long t = 2; t <= 21; t++) tF.crucible().onTick(t, true);
		assertEquals(1497, tF.crucible().mTemperature, "two more windows in twenty ticks");
	}

	// ------------------------------------------------------------------
	// the content admission (upstream addMaterialStacks :386-408)
	// ------------------------------------------------------------------

	@Test
	public void capacityGateRejectsOverflow() {
		Formed tF = formedCrucible();
		List<OreDictMaterialStack> tFill = new ArrayList<>();
		tFill.add(new OreDictMaterialStack(MT.Water, 431 * gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tFill, 300), "431U fits the 432U cavity");
		List<OreDictMaterialStack> tTooMuch = new ArrayList<>();
		tTooMuch.add(new OreDictMaterialStack(MT.Water, 2 * gregapi.data.CS.U));
		assertFalse(tF.crucible().addMaterialStacks(tTooMuch, 300), "433U total overflows");
		List<OreDictMaterialStack> tExact = new ArrayList<>();
		tExact.add(new OreDictMaterialStack(MT.Water, 1 * gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tExact, 300), "432U exactly fits");
		assertEquals(432 * gregapi.data.CS.U, totalOf(tF.crucible().mContent));
	}

	// ------------------------------------------------------------------
	// the phase gates BE-side (boiling evaporation, :296-334)
	// ------------------------------------------------------------------

	@Test
	public void boilingWaterEvaporates() {
		Formed tF = formedCrucible();
		tF.crucible().mTemperature = 500; // above the Water boiling point, below the Steel ceiling
		List<OreDictMaterialStack> tWater = new ArrayList<>();
		tWater.add(new OreDictMaterialStack(MT.Water, gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tWater, 500));
		tF.crucible().onTick(1, true);
		assertEquals(0, totalOf(tF.crucible().mContent), "the boil-off leaves nothing (:304-306)");
	}

	// ------------------------------------------------------------------
	// the alloy scan BE-side (Invar from the graph, :236-294)
	// ------------------------------------------------------------------

	@Test
	public void invarFormsInsideTheTickingCrucible() {
		Formed tF = formedCrucible();
		OreDictMaterial tInvar = MT.Invar;
		List<OreDictMaterialStack> tComponents = new ArrayList<>(MaterialGraph.alloyUndividedComponents(tInvar));
		assertFalse(tComponents.isEmpty(), "Invar declares its composition");
		long tHot = tInvar.mMeltingPoint;
		for (OreDictMaterialStack tComponent : tComponents) tHot = Math.max(tHot, tComponent.mMaterial.mMeltingPoint);
		assertTrue(tHot < tF.crucible().getTemperatureMax((byte)0), "the alloy fits under the Steel ceiling");

		tF.crucible().mTemperature = tHot;
		assertTrue(tF.crucible().addMaterialStacks(new ArrayList<>(tComponents), tHot));
		tF.crucible().onTick(1, true);

		OreDictMaterialStack tAlloyStack = null;
		for (OreDictMaterialStack tStack : tF.crucible().mContent) {
			if (tStack.mAmount > 0 && tStack.mMaterial == tInvar) tAlloyStack = tStack;
		}
		assertNotNull(tAlloyStack, "the tick formed Invar from its components");
	}

	// ------------------------------------------------------------------
	// the through-wall mold proxy (upstream :547-556)
	// ------------------------------------------------------------------

	@Test
	public void fillMoldAtSidePoursMoltenSelfSmeltedMetal() {
		Formed tF = formedCrucible();
		// fillMoldAtSide is a PULL API (the mold clicks the wall, no tick involved), so the
		// temperature here never reaches the tick's meltdown guard — 2000 K is molten-iron
		// territory and the ceiling math does not apply to a direct call.
		tF.crucible().mTemperature = 2000;
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, 2 * gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tIron, 2000));

		RecordingMold tMold = new RecordingMold();
		tMold.mDemand = gregapi.data.CS.U;
		assertTrue(tF.crucible().fillMoldAtSide(tMold, (byte)2, (byte)3), "the molten self-smelted stack pours");
		assertSame(MT.Fe, tMold.mPoured.mMaterial, "the Fe stack reached the mold");
		assertEquals(2000, tMold.mPouredTemperature, "the pour carries the crucible temperature");
		assertEquals((byte)3, tMold.mPouredSide, "the mold-facing side rode through");
		assertEquals(gregapi.data.CS.U, totalOf(tF.crucible().mContent), "the pour subtracted one unit");

		// the drained remainder still pours
		assertTrue(tF.crucible().fillMoldAtSide(tMold, (byte)2, (byte)3));
		assertEquals(0, totalOf(tF.crucible().mContent));
		assertFalse(tF.crucible().fillMoldAtSide(tMold, (byte)2, (byte)3), "nothing left to pour");
	}

	@Test
	public void fillMoldAtSideRefusesColdContentAndBrokenStructure() {
		Formed tF = formedCrucible();
		// solid iron in a cold crucible: the molten gate (:548) refuses
		tF.crucible().mTemperature = 300;
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tIron, 300));
		RecordingMold tMold = new RecordingMold();
		tMold.mDemand = gregapi.data.CS.U;
		assertFalse(tF.crucible().fillMoldAtSide(tMold, (byte)2, (byte)3), "cold solid iron does not pour");

		// break a wall: the structure gate (:548) refuses
		BlockPos tVictim = new BlockPos(99, 64, 100);
		tF.level().mBlockEntities.remove(tVictim);
		tF.level().mStates.put(tVictim, Blocks.AIR.defaultBlockState());
		tF.crucible().onStructureChange();
		tF.crucible().mTemperature = 2000;
		assertFalse(tF.crucible().fillMoldAtSide(tMold, (byte)2, (byte)3), "no structure, no pour");
		assertNull(tMold.mPoured, "the mold was never contacted");
	}

	// ------------------------------------------------------------------
	// the content NBT round-trip (upstream :98/:108 via MaterialStackNBT)
	// ------------------------------------------------------------------

	@Test
	public void contentListRoundTripsThroughNBT() {
		Formed tF = formedCrucible();
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, 10 * gregapi.data.CS.U));
		tF.crucible().addMaterialStacks(tIron, 300);

		net.minecraft.nbt.CompoundTag tTag = tF.crucible().saveWithoutMetadata();
		TestCrucible tRestored = sCrucibleType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);
		assertEquals(10 * gregapi.data.CS.U, totalOf(tRestored.mContent), "the content list survives save/load (:98/:108)");
		assertEquals(1, tRestored.mContent.size(), "exactly one stack");
		assertSame(MT.Fe, tRestored.mContent.get(0).mMaterial, "the material identity rides the 'i' key");
	}

	// ------------------------------------------------------------------
	// the meltdown path (upstream :367-378 — acceptance ④)
	// ------------------------------------------------------------------

	@Test
	public void meltdownTrashesContentAndLavasTheCavity() {
		Formed tF = formedCrucible();
		List<OreDictMaterialStack> tIron = new ArrayList<>();
		tIron.add(new OreDictMaterialStack(MT.Fe, 10 * gregapi.data.CS.U));
		assertTrue(tF.crucible().addMaterialStacks(tIron, 300));
		assertTrue(totalOf(tF.crucible().mContent) > 0);

		long tMax = tF.crucible().getTemperatureMax((byte)0);
		assertTrue(tMax > 0, "the Steel shell ceiling derives from the material graph");
		tF.crucible().mTemperature = tMax + 1; // :367 — one Kelvin over the ceiling

		tF.crucible().onTick(1, true);

		assertEquals(0, totalOf(tF.crucible().mContent), "the content is trashed (:369)");
		// the 3x3x3 cavity is lava (the controller cell included, :372-376)
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) for (int tY = 0; tY <= 2; tY++) {
			assertSame(Blocks.LAVA, tF.level().getBlockState(new BlockPos(100 + tDX, 64 + tY, 100 + tDZ)).getBlock(),
					"the cavity cell (" + tDX + "," + tY + "," + tDZ + ") is lava");
		}
	}

	@Test
	public void meltDownWarningLatchesNearTheCeiling() {
		Formed tF = formedCrucible();
		long tMax = tF.crucible().getTemperatureMax((byte)0);
		// :380-383 — the WARNING state is the +100 K window below the ceiling
		tF.crucible().mTemperature = tMax - 50;
		tF.crucible().mEnergy = 0;
		tF.crucible().onTick(1, true);
		assertTrue(tF.crucible().mMeltDown, "within 100 K of the ceiling the warning latches");
		// far below: the warning clears (the temperature stays put — the out-of-supply
		// decay takes time, the assertion reads the latch only)
		tF.crucible().mTemperature = tMax - 500;
		tF.crucible().onTick(2, true);
		assertFalse(tF.crucible().mMeltDown, "clear of the window the warning unlatches");
		// the ceiling itself was never crossed — no meltdown ran
		assertSame(Blocks.BRICKS, tF.level().getBlockState(new BlockPos(100, 64, 100)).getBlock(), "the controller cell survives");
	}
}
