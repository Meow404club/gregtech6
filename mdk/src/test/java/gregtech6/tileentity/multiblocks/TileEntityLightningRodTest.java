package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;

/**
 * Offline acceptance rig for the Lightning Rod port (task p24-lightning-rod) — the seven
 * card arms over the GTMultiBlocksOfflineTestBase stub world:
 * <ol>
 * <li>the structure walk: the 45-cell base + the pillar probe per mSize, air self-stop,
 *     mSize == 0 still forms, the byte-wrap self-stop (the height-gate declaration);</li>
 * <li>the zero-pillar-SET arm: the wand triple (clickedAt = controller pos, null player,
 *     stocked inventory, upstream :313 the two-pass form) scaffolds the base ±1 layers
 *     and NEVER the pillar cells (the ±1 door, the form-set decision) — plus the base
 *     scaffold boolean pairs through the Util permission seam (creative free / OP2
 *     consume / no-permission deny, the FormSeamTest posture);</li>
 * <li>the unloaded arm: the four-corner pre-gate keeps the last verdict and performs ZERO
 *     block-entity lookups (no forced chunk load, the probe-unloaded decision);</li>
 * <li>the RCON arm: pre-placed 45 + pillar → checkStructure(true) forms; breaking any
 *     base block re-scan breaks it and the tick bleeds mEnergy dry (:150-152);</li>
 * <li>the energy arm: the formed machine pushes 16 A x 32768 EU out the bottom through
 *     the adjacency override into a CountingSink (:128-129), sub-packet energy is trashed
 *     and the strike gate takes over (:130-132);</li>
 * <li>the strike gate: the four gates + the interference dilution + the sky loop, rng
 *     through the seam, the strike via the recorded spawnStrike seam — mEnergy lands on
 *     the compile-time CAPACITY (no Entity is ever constructed offline);</li>
 * <li>the static table lifecycle (onTickFirst register / setRemoved / onChunkUnloaded)
 *     and the NBT face (only mEnergy persists).</li>
 * </ol>
 */
class TileEntityLightningRodTest extends GTMultiBlocksOfflineTestBase {

	/** The fixture BET (the offline selfHolder form — the frozen registry keeps .get() out of reach). */
	static BlockEntityType<TestLightningRod> sRodType;

	/**
	 * The rod-local part BET over ALL THREE fixture blocks — the base {@code sPartType}
	 * mounts BRICKS only, and 1.21.1 {@code BlockEntity.setBlockState} VALIDATES the state
	 * against the type ("Invalid block entity ..." IllegalStateException), so placing an
	 * IRON_BLOCK/STONE_BRICKS cell through it dies on the neo leg.
	 */
	static BlockEntityType<MultiBlockPartBlockEntity> sRodPartType;

	@BeforeAll
	static void buildRodFixture() {
		// the base's selfHolder is private — the same recipe, local (the GTMachinesOfflineTestBase form)
		@SuppressWarnings("unchecked")
		BlockEntityType<TestLightningRod>[] tHolder = (BlockEntityType<TestLightningRod>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestLightningRod(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sRodType = tHolder[0];
		// the explicit test-seam ctor: the bare (pos, state) lambda would bind the
		// registry-path constructor and resolve gt6:multiblock_part offline (NPE)
		@SuppressWarnings("unchecked")
		BlockEntityType<MultiBlockPartBlockEntity>[] tPartHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tPartHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new MultiBlockPartBlockEntity(tPartHolder[0], aPos, aState),
				Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.STONE_BRICKS).build(null);
		sRodPartType = tPartHolder[0];
	}

	@BeforeEach
	void clearRodTable() {
		TileEntityLightningRod.ALL_LIGHTNING_RODS.clear();
	}

	@AfterEach
	void drainRodTable() {
		TileEntityLightningRod.ALL_LIGHTNING_RODS.clear();
	}

	// -------------------------------------------------------------------------
	// fixtures
	// -------------------------------------------------------------------------

	/** The rod fixture: the three part blocks bound to distinct vanilla blocks, the strike recorded. */
	static class TestLightningRod extends TileEntityLightningRod {
		int strikes = 0;
		int strikeX, strikeY, strikeZ;
		final List<Integer> rngRanges = new ArrayList<>();

		TestLightningRod(BlockPos aPos, BlockState aState) {
			this(sRodType, aPos, aState);
		}

		TestLightningRod(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override protected Block getWallBlock() { return Blocks.BRICKS; }
		@Override protected Block getCoilBlock() { return Blocks.IRON_BLOCK; }
		@Override protected Block getRodBlock() { return Blocks.STONE_BRICKS; }

		@Override
		protected int rng(int aRange) {
			rngRanges.add(aRange); // the gate-order witness (the requested ranges, in call order)
			return super.rng(aRange);
		}

		@Override
		protected void spawnStrike(int aX, int aY, int aZ) {
			strikes++;
			strikeX = aX; strikeY = aY; strikeZ = aZ;
		}
	}

	/** The stub world: weather + loaded toggles, a BE-lookup counter, the vanilla overworld top. */
	static class RodLevel extends MultiBlockLevel {
		boolean raining = false;
		boolean thundering = false;
		boolean loaded = true;
		int beLookups = 0;

		@Override public boolean isRaining() { return raining; }
		@Override public boolean isThundering() { return thundering; }
		@Override public boolean isLoaded(BlockPos aPos) { return loaded; }
		@Override public int getHeight() { return 384; }

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			beLookups++;
			return super.getBlockEntity(aPos);
		}
	}

	/** A recording rng: answers from a queue (the offline rng seam payload). */
	static class SeqRng implements IntSupplier {
		final Deque<Integer> mValues = new ArrayDeque<>();

		SeqRng(int... aValues) {
			for (int v : aValues) mValues.add(v);
		}

		@Override
		public int getAsInt() {
			return mValues.isEmpty() ? 0 : mValues.removeFirst();
		}
	}

	/** The EU CountingSink (the GTSteamEngineBlockEntityTest posture, type-parameterised to EU). */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long packetsAccepted = 0, packetsRemaining = 0, lastSize = 0, lastAmount = 0;
		public int calls = 0;

		static final BlockEntityType<CountingSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new CountingSink(aPos), Blocks.STONE).build(null);

		public CountingSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) { return !aEmitting && aEnergyType == TD.Energy.EU; }
		@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) { return aEnergyType == TD.Energy.EU; }
		@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) { return false; }
		@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) { return TD.Energy.EU.AS_LIST; }

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (!aDoInject) return Math.min(aAmount, packetsRemaining);
			long tUsed = Math.min(aAmount, packetsRemaining);
			packetsRemaining -= tUsed;
			if (tUsed > 0) { calls++; lastSize = aSize; lastAmount = aAmount; packetsAccepted += tUsed; }
			return tUsed;
		}

		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) { return packetsRemaining; }
		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) { return 0; }
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) { return 0; }
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) { return 1; }
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) { return TileEntityLightningRod.VOLTAGE; }
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) { return TileEntityLightningRod.VOLTAGE; }
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) { return 1; }
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) { return 1; }
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) { return 1; }
	}

	/** Places the rod fixture straight into the stub world (the placeController recipe). */
	static TestLightningRod placeRod(RodLevel aLevel, BlockPos aPos) {
		TestLightningRod tRod = sRodType.create(aPos, Blocks.BRICKS.defaultBlockState());
		tRod.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tRod);
		return tRod;
	}

	/** Places a part BE of the given block straight into the stub world (the rod-local three-block part BET). */
	static MultiBlockPartBlockEntity placePartAt(RodLevel aLevel, BlockPos aPos, Block aBlock) {
		MultiBlockPartBlockEntity tPart = sRodPartType.create(aPos, aBlock.defaultBlockState());
		tPart.setLevel(aLevel);
		aLevel.mStates.put(aPos, aBlock.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPart);
		return tPart;
	}

	/** The 3x3x5 base (walls/coils alternating, the centre-bottom cell is the controller itself) plus an aPillar-long rod column. */
	static void buildStructure(RodLevel aLevel, BlockPos aCenter, int aPillar) {
		for (int tDy = 0; tDy < 5; tDy++) {
			Block tLayerBlock = (tDy % 2 == 0) ? Blocks.BRICKS : Blocks.IRON_BLOCK;
			for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) {
				BlockPos tPos = new BlockPos(aCenter.getX() + i, aCenter.getY() + tDy, aCenter.getZ() + j);
				if (tPos.equals(aCenter)) continue; // the controller cell
				placePartAt(aLevel, tPos, tLayerBlock);
			}
		}
		for (int k = 0; k < aPillar; k++) {
			placePartAt(aLevel, new BlockPos(aCenter.getX(), aCenter.getY() + 5 + k, aCenter.getZ()), Blocks.STONE_BRICKS);
		}
	}

	/** Removes a cell entirely (the player-break world half; the caller drives onStructureChange). */
	static void breakBlock(RodLevel aLevel, BlockPos aPos) {
		aLevel.mStates.remove(aPos);
		aLevel.mBlockEntities.remove(aPos);
	}

	/** Explicit static-table registration (the onTickFirst(true) face, without the forced structure walk). */
	static void register(TileEntityLightningRod aRod) {
		TileEntityLightningRod.ALL_LIGHTNING_RODS.add(aRod);
	}

	// -------------------------------------------------------------------------
	// ① the structure walk (the probe arm)
	// -------------------------------------------------------------------------

	@Test
	void structureWalkCountsThePillarAndStopsAtAir() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 3);

		assertTrue(tRod.checkStructure2(null, null, null)); // the polling triple
		assertEquals(3, tRod.mSize); // every rod cell counts
	}

	@Test
	void structureWalkZeroPillarStillForms() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 0);

		assertTrue(tRod.checkStructure2(null, null, null));
		assertEquals(0, tRod.mSize); // :84 rides the if(tSuccess) gate — mSize == 0 forms
	}

	@Test
	void structureWalkFailsOnABrokenBase() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 2);
		breakBlock(tLevel, new BlockPos(101, 66, 100)); // one y+2 wall cell

		assertFalse(tRod.checkStructure2(null, null, null));
		assertEquals(0, tRod.mSize); // :74 — the reset happens even on a failed base
	}

	@Test
	void structureWalkPillarSelfStopsAtTheByteWrap() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 128); // the full byte domain

		assertTrue(tRod.checkStructure2(null, null, null));
		assertEquals(-128, tRod.mSize); // the byte wrap self-stop (the height-gate declaration, upstream-homomorphic)
	}

	@Test
	void isInsideStructureIsTheVerbatimBox() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 2);
		tRod.checkStructure2(null, null, null);
		assertEquals(2, tRod.mSize);

		assertTrue(tRod.isInsideStructure(100, 64, 100)); // the controller cell
		assertTrue(tRod.isInsideStructure(99, 64, 101)); // the base box x/z ±1
		assertTrue(tRod.isInsideStructure(100, 68, 100)); // y+4 — the last full-box layer
		assertTrue(tRod.isInsideStructure(101, 67, 99)); // y+3 — inside the box, off-column
		assertTrue(tRod.isInsideStructure(100, 70, 100)); // y+5+mSize-1 = 70 — the pillar top
		assertFalse(tRod.isInsideStructure(100, 71, 100)); // one past the pillar top
		assertFalse(tRod.isInsideStructure(100, 70, 101)); // off-column above the box
		assertFalse(tRod.isInsideStructure(102, 64, 100)); // outside x
		assertFalse(tRod.isInsideStructure(100, 63, 100)); // below
	}

	// -------------------------------------------------------------------------
	// ② the SET arm (the zero-pillar-SET verdict + the base boolean pairs)
	// -------------------------------------------------------------------------

	@Test
	void wandTripleNeverSetsThePillarCells() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		tLevel.mBeFactory = (aPos, aState) -> { // wand placements land part BEs (the stub BE-creation step)
			MultiBlockPartBlockEntity tPart = sRodPartType.create(aPos, aState);
			tPart.setLevel(tLevel);
			return tPart;
		};
		SimpleContainer tStock = new SimpleContainer(
				new ItemStack(Blocks.BRICKS, 64),
				new ItemStack(Blocks.IRON_BLOCK, 64),
				new ItemStack(Blocks.STONE_BRICKS, 64));

		// the upstream :313-314 two-pass form, clicked at the controller
		tRod.checkStructure2(tCenter, null, tStock); // the placing pass
		tRod.checkStructure2(tCenter, null, tStock); // the linking pass
		tRod.checkStructure2(tCenter, null, tStock); // the y+2..y+4 layers stay outside the ±1 door

		// the base ±1 layers got scaffolded...
		assertTrue(tLevel.getBlockState(new BlockPos(99, 64, 99)).is(Blocks.BRICKS)); // a y+0 wall corner
		assertTrue(tLevel.getBlockState(new BlockPos(100, 65, 100)).is(Blocks.IRON_BLOCK)); // a y+1 coil centre
		// ...and the pillar NEVER (|dy| >= 2 from the click — the form-set decision's door)
		for (int k = 0; k < 3; k++) {
			BlockPos tPillarCell = new BlockPos(100, 69 + k, 100);
			assertTrue(tLevel.getBlockState(tPillarCell).isAir(), "pillar cell " + tPillarCell + " must see zero SET");
		}
		// the rod block item stock: untouched (nothing ever asked for it)
		assertEquals(64, tStock.getItem(2).getCount());
	}

	@Test
	void baseScaffoldBooleanPairs() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		tLevel.mBeFactory = (aPos, aState) -> {
			MultiBlockPartBlockEntity tPart = sRodPartType.create(aPos, aState);
			tPart.setLevel(tLevel);
			return tPart;
		};
		BlockPos tCell = new BlockPos(99, 64, 99); // a y+0 wall corner, air

		// creative free — (mayEdit, infiniteItems) = (true, true), an EMPTY inventory still places
		SimpleContainer tEmpty = new SimpleContainer(1);
		ITileEntityMultiBlockController.Util.checkAndSetTarget(tRod, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.NOTHING, tCenter, null, tEmpty, true, true);
		assertTrue(tLevel.getBlockState(tCell).is(Blocks.BRICKS), "creative free placement");
		assertEquals(0, tEmpty.getItem(0).getCount());

		// OP2 consume — (true, false) over a stocked inventory shrinks it by one
		BlockPos tCell2 = new BlockPos(101, 64, 99);
		SimpleContainer tStocked = new SimpleContainer(new ItemStack(Blocks.BRICKS, 64));
		ITileEntityMultiBlockController.Util.checkAndSetTarget(tRod, tCell2.getX(), tCell2.getY(), tCell2.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.NOTHING, tCenter, null, tStocked, true, false);
		assertTrue(tLevel.getBlockState(tCell2).is(Blocks.BRICKS), "OP2 consume placement");
		assertEquals(63, tStocked.getItem(0).getCount());

		// no permission — (false, *) places nothing and consumes nothing
		BlockPos tCell3 = new BlockPos(99, 64, 101);
		SimpleContainer tStocked3 = new SimpleContainer(new ItemStack(Blocks.BRICKS, 64));
		ITileEntityMultiBlockController.Util.checkAndSetTarget(tRod, tCell3.getX(), tCell3.getY(), tCell3.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.NOTHING, tCenter, null, tStocked3, false, false);
		assertTrue(tLevel.getBlockState(tCell3).isAir(), "the scaffold gate stays shut");
		assertEquals(64, tStocked3.getItem(0).getCount());
	}

	// -------------------------------------------------------------------------
	// ③ the unloaded arm (the probe-unloaded pre-gate)
	// -------------------------------------------------------------------------

	@Test
	void unloadedCornersKeepTheLastVerdictWithoutProbing() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 1);
		assertTrue(tRod.checkStructure2(null, null, null)); // formed while loaded

		tLevel.loaded = false; // the corner columns go away
		int tLookupsBefore = tLevel.beLookups;
		tRod.mStructureOkay = true; // the last verdict

		assertTrue(tRod.checkStructure2(null, null, null)); // the verdict is KEPT...
		assertEquals(tLookupsBefore, tLevel.beLookups); // ...with ZERO block-entity lookups — nothing was probed, no chunk was touched
	}

	@Test
	void unloadedCornersKeepTheBrokenVerdict() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		tLevel.loaded = false;
		tRod.mStructureOkay = false;
		assertFalse(tRod.checkStructure2(null, null, null));
	}

	// -------------------------------------------------------------------------
	// ④ the RCON arm (form → break → re-scan → dry)
	// -------------------------------------------------------------------------

	@Test
	void formedThenBrokenBleedsTheEnergyDry() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 5);

		assertTrue(tRod.checkStructure(true)); // the RCON linking pass — 45 base cells + any pillar
		assertTrue(tRod.mStructureOkay);
		tRod.mEnergy = 2 * TileEntityLightningRod.VOLTAGE;
		tRod.onTick(1, true); // formed: the empty-bottom push still drains the max(1, used)=1-packet floor (:129)
		assertEquals(TileEntityLightningRod.VOLTAGE, tRod.mEnergy);

		// break ONE base block — the part-block playerWillDestroy chain flags the controller
		breakBlock(tLevel, new BlockPos(101, 66, 100));
		tRod.onStructureChange();
		assertFalse(tRod.checkStructure(false)); // the re-scan flips the verdict
		assertFalse(tRod.mStructureOkay);
		tRod.onTick(2, true); // the :150-152 else form
		assertEquals(0, tRod.mEnergy); // the stored charge bleeds dry
	}

	// -------------------------------------------------------------------------
	// ⑤ the energy arm (the adjacency-override + CountingSink posture)
	// -------------------------------------------------------------------------

	@Test
	void formedMachinePushesSixteenAmpPacketsOutTheBottom() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 2);
		assertTrue(tRod.checkStructure(true));

		CountingSink tSink = new CountingSink(new BlockPos(100, 63, 100));
		tSink.packetsRemaining = 2; // the sink room: 2 packets
		tRod.setAdjacencyOverride(aSide -> {
			assertEquals(TileEntityLightningRod.SIDE_BOTTOM, aSide); // the :129 emission face
			return new EnergyTarget(tSink, (byte)Direction.UP.get3DDataValue());
		});
		tRod.mEnergy = 5 * TileEntityLightningRod.VOLTAGE;

		tRod.onTick(1, true); // no forced re-scan (1 % 1200 != 300) — the cached formed verdict drives the emit

		assertEquals(1, tSink.calls);
		assertEquals(TileEntityLightningRod.VOLTAGE, tSink.lastSize);
		assertEquals(16, tSink.lastAmount); // the 16 A offer
		assertEquals(2, tSink.packetsAccepted); // only 2 packets found room
		assertEquals(3 * TileEntityLightningRod.VOLTAGE, tRod.mEnergy); // mEnergy -= used * VOLTAGE (:129)
	}

	@Test
	void subPacketEnergyIsTrashedAndTheStrikeGateTakesOver() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 2);
		assertTrue(tRod.checkStructure(true));

		CountingSink tSink = new CountingSink(new BlockPos(100, 63, 100));
		tSink.packetsRemaining = 8;
		tRod.setAdjacencyOverride(aSide -> new EnergyTarget(tSink, (byte)1));
		tRod.mEnergy = 100; // below one packet

		tRod.onTick(1, true); // the :130-132 else form — clear weather, no strike

		assertEquals(0, tRod.mEnergy); // trashed
		assertEquals(0, tSink.calls);
		assertEquals(0, tRod.strikes);
	}

	// -------------------------------------------------------------------------
	// ⑥ the strike gate (the four gates + dilution + sky loop, all offline-pure)
	// -------------------------------------------------------------------------

	/** Forms a rod with an aSize pillar and parks it below one packet of charge. */
	static TestLightningRod strikeFixture(RodLevel aLevel, BlockPos aCenter, int aSize) {
		TestLightningRod tRod = placeRod(aLevel, aCenter);
		buildStructure(aLevel, aCenter, aSize);
		assertTrue(tRod.checkStructure(true));
		tRod.mEnergy = 0;
		return tRod;
	}

	@Test
	void theFullStrikeLandsOnTheCapacityConstant() {
		RodLevel tLevel = new RodLevel();
		tLevel.thundering = true; // gate ④: thundering needs no rng(10) roll
		BlockPos tCenter = new BlockPos(100, 100, 100); // tip at 100 + 3 = 103 >= 100
		TestLightningRod tRod = strikeFixture(tLevel, tCenter, 3);
		tRod.setRngOverride(new SeqRng(0, 0)); // rng(1000000)=0 < 3; rng(1)=0 → the solo strike
		register(tRod);

		tRod.onTick(300, true); // the forced re-scan arm AND the strike gates

		assertEquals(1, tRod.strikes);
		assertEquals(100, tRod.strikeX);
		assertEquals(100 + 3 + 4, tRod.strikeY); // yCoord + mSize + 4 (:144)
		assertEquals(100, tRod.strikeZ);
		assertEquals(TileEntityLightningRod.CAPACITY, tRod.mEnergy); // the mid-strike charge = 589,824,000 EU
	}

	@Test
	void strikeGateRejectsEveryShortPillar() {
		// gate ①: mSize == 0
		RodLevel tLevel1 = new RodLevel();
		tLevel1.thundering = true;
		TestLightningRod tRod1 = strikeFixture(tLevel1, new BlockPos(100, 100, 100), 0);
		tRod1.setRngOverride(new SeqRng(0));
		register(tRod1);
		tRod1.onTick(300, true);
		assertEquals(0, tRod1.strikes);

		// gate ②: the tip below Y = 100 (the absolute-altitude gate, no height rescale)
		RodLevel tLevel2 = new RodLevel();
		tLevel2.thundering = true;
		TestLightningRod tRod2 = strikeFixture(tLevel2, new BlockPos(100, 64, 100), 3); // tip 67
		tRod2.setRngOverride(new SeqRng(0, 0));
		register(tRod2);
		tRod2.onTick(300, true);
		assertEquals(0, tRod2.strikes);

		// gate ③: rng(1000000) >= min(100, mSize)
		RodLevel tLevel3 = new RodLevel();
		tLevel3.thundering = true;
		TestLightningRod tRod3 = strikeFixture(tLevel3, new BlockPos(100, 100, 100), 3);
		tRod3.setRngOverride(new SeqRng(3, 0)); // the roll misses by exactly the gate bound
		register(tRod3);
		tRod3.onTick(300, true);
		assertEquals(0, tRod3.strikes);
	}

	@Test
	void strikeGateNeedsThunderOrRainPlusTheOneInTenRoll() {
		// clear weather — gate ④ shut
		RodLevel tLevel = new RodLevel();
		TestLightningRod tRod = strikeFixture(tLevel, new BlockPos(100, 100, 100), 3);
		tRod.setRngOverride(new SeqRng(0, 0));
		register(tRod);
		tRod.onTick(300, true);
		assertEquals(0, tRod.strikes);

		// rain WITHOUT the one-in-ten roll — gate ④ shut (the rng(10) arm)
		RodLevel tLevel2 = new RodLevel();
		tLevel2.raining = true;
		TestLightningRod tRod2 = strikeFixture(tLevel2, new BlockPos(100, 100, 100), 3);
		tRod2.setRngOverride(new SeqRng(0, 1)); // rng(1000000) passes, rng(10) = 1 misses
		register(tRod2);
		tRod2.onTick(300, true);
		assertEquals(0, tRod2.strikes);

		// rain WITH the roll — the bolt lands
		RodLevel tLevel3 = new RodLevel();
		tLevel3.raining = true;
		TestLightningRod tRod3 = strikeFixture(tLevel3, new BlockPos(100, 100, 100), 3);
		tRod3.setRngOverride(new SeqRng(0, 0, 0));
		register(tRod3);
		tRod3.onTick(300, true);
		assertEquals(1, tRod3.strikes);
	}

	@Test
	void nearbyRodDilutesTheStrikeByItsCount() {
		// the second rod within 256 m (same world, mSize > 0) doubles the denominator
		RodLevel tLevel = new RodLevel();
		tLevel.thundering = true;
		TestLightningRod tRod = strikeFixture(tLevel, new BlockPos(100, 100, 100), 3);
		TestLightningRod tNeighbor = strikeFixture(tLevel, new BlockPos(200, 100, 200), 2); // |dx| = |dz| = 100 < 256
		register(tRod);
		register(tNeighbor);
		assertEquals(2, TileEntityLightningRod.ALL_LIGHTNING_RODS.size());

		SeqRng tRng = new SeqRng(0, 1); // rng(1000000) passes; rng(tCount = 2) = 1 → the neighbor wins the draw
		tRod.setRngOverride(tRng);
		tRod.onTick(300, true);
		assertEquals(0, tRod.strikes);
		assertEquals(2, tRod.rngRanges.get(1)); // the dilution denominator WAS the interference count

		// and beyond 256 m there is NO dilution: the solo draw strikes
		RodLevel tLevel2 = new RodLevel();
		tLevel2.thundering = true;
		TestLightningRod tFar = strikeFixture(tLevel2, new BlockPos(100, 100, 100), 3);
		TestLightningRod tDistant = strikeFixture(tLevel2, new BlockPos(100 + 256, 100, 100), 2); // |dx| = 256 — NOT < 256
		register(tFar);
		register(tDistant);
		tFar.setRngOverride(new SeqRng(0, 0));
		tFar.onTick(300, true);
		assertEquals(1, tFar.strikes);

		// a size-0 neighbor does not dilute either (the mSize > 0 arm)
		RodLevel tLevel3 = new RodLevel();
		tLevel3.thundering = true;
		TestLightningRod tSolo = strikeFixture(tLevel3, new BlockPos(100, 100, 100), 3);
		TestLightningRod tTiny = strikeFixture(tLevel3, new BlockPos(150, 100, 150), 0);
		register(tSolo);
		register(tTiny);
		tSolo.setRngOverride(new SeqRng(0, 0));
		tSolo.onTick(300, true);
		assertEquals(1, tSolo.strikes);
	}

	@Test
	void aBlockedSkyColumnDeniesTheStrike() {
		RodLevel tLevel = new RodLevel();
		tLevel.thundering = true;
		BlockPos tCenter = new BlockPos(100, 100, 100);
		TestLightningRod tRod = strikeFixture(tLevel, tCenter, 3);
		// a roof right above the tip — a COIL block, NOT a rod block (a rod block there would
		// just grow the pillar; the coil stops the probe at mSize = 3 AND blocks the sky loop
		// from yCoord + mSize + 5 = 108)
		placePartAt(tLevel, new BlockPos(100, 108, 100), Blocks.IRON_BLOCK);
		tRod.setRngOverride(new SeqRng(0, 0));
		register(tRod);

		tRod.onTick(300, true);
		assertEquals(3, tRod.mSize); // the roof stopped the probe where it belongs
		assertEquals(0, tRod.strikes); // the sky loop failed — no bolt, no charge
		assertEquals(0, tRod.mEnergy);
	}

	// -------------------------------------------------------------------------
	// ⑦ the static table + the re-scan cadence + NBT
	// -------------------------------------------------------------------------

	@Test
	void rescanCadenceRediscoversNewPillarBlocksOnlyWhenForced() {
		RodLevel tLevel = new RodLevel();
		BlockPos tCenter = new BlockPos(100, 64, 100);
		TestLightningRod tRod = placeRod(tLevel, tCenter);
		buildStructure(tLevel, tCenter, 0);
		assertTrue(tRod.checkStructure(true));
		assertEquals(0, tRod.mSize);

		// a new rod block lands (no neighbour update — the :126 lag comment)
		placePartAt(tLevel, new BlockPos(100, 69, 100), Blocks.STONE_BRICKS);

		tRod.onTick(301, true); // NOT the :300 cadence tick — no forced re-scan
		assertEquals(0, tRod.mSize);

		tLevel.raining = true; // the :127 arm needs rain or thunder
		tRod.onTick(300, true); // aTimer % 1200 == 300 — the forced re-scan
		assertEquals(1, tRod.mSize);
	}

	@Test
	void staticTableLifecycle() {
		RodLevel tLevel = new RodLevel();
		TestLightningRod tRod = placeRod(tLevel, new BlockPos(100, 64, 100));

		assertFalse(TileEntityLightningRod.ALL_LIGHTNING_RODS.contains(tRod));
		tRod.onTickFirst(true); // the server first-tick registers
		assertTrue(TileEntityLightningRod.ALL_LIGHTNING_RODS.contains(tRod));
		tRod.onTickFirst(true); // idempotent
		assertEquals(1, TileEntityLightningRod.ALL_LIGHTNING_RODS.size());

		tRod.setRemoved(); // the removal de-registers
		assertFalse(TileEntityLightningRod.ALL_LIGHTNING_RODS.contains(tRod));

		TileEntityLightningRod.ALL_LIGHTNING_RODS.add(tRod);
		tRod.onChunkUnloaded(); // the chunk unload de-registers
		assertFalse(TileEntityLightningRod.ALL_LIGHTNING_RODS.contains(tRod));
	}

	@Test
	void nbtCarriesOnlyTheEnergy() {
		RodLevel tLevel = new RodLevel();
		TestLightningRod tRod = placeRod(tLevel, new BlockPos(100, 64, 100));
		tRod.mEnergy = 123456789;

		CompoundTag tTag = tRod.saveWithoutMetadata(); // no id write — the fixture BET is unregistered (offline)
		assertEquals(123456789, tTag.getLong(TileEntityLightningRod.NBT_ENERGY));
		assertFalse(tTag.contains("capacity")); // the decision-capacity constant never rides NBT
		assertFalse(tTag.contains("size")); // mSize re-derives on the onTickFirst forced check

		TestLightningRod tLoaded = placeRod(tLevel, new BlockPos(100, 64, 100));
		tLoaded.load(tTag);
		assertEquals(123456789, tLoaded.mEnergy);
		assertEquals(TileEntityLightningRod.CAPACITY, tLoaded.mCapacity); // the compile-time 589,824,000 EU
		assertEquals(589824000L, TileEntityLightningRod.CAPACITY);
	}

	@Test
	void energyFaceReportsTheUpstreamConstants() {
		RodLevel tLevel = new RodLevel();
		TestLightningRod tRod = placeRod(tLevel, new BlockPos(100, 64, 100));

		assertTrue(tRod.isEnergyType(TD.Energy.EU, TileEntityLightningRod.SIDE_BOTTOM, true));
		assertFalse(tRod.isEnergyType(TD.Energy.EU, TileEntityLightningRod.SIDE_BOTTOM, false));
		assertFalse(tRod.isEnergyEmittingTo(TD.Energy.EU, TileEntityLightningRod.SIDE_BOTTOM, false)); // the :184 quirk — always false to networks
		assertEquals(2048, tRod.getEnergySizeOutputRecommended(TD.Energy.EU, (byte)0));
		assertEquals(1024, tRod.getEnergySizeOutputMin(TD.Energy.EU, (byte)0));
		assertEquals(4096, tRod.getEnergySizeOutputMax(TD.Energy.EU, (byte)0));
		assertEquals(TD.Energy.EU.AS_LIST, tRod.getEnergyTypes((byte)6));
	}
}
