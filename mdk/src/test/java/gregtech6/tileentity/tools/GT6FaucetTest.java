package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The card-B faucet truth tables (task p26-crucible-mold-faucet): the monkey-wrench
 * auto-pull state machine (:151-162), the mount-face mold-input gate (:93-95), the
 * pull-at-the-faced-crucible seam (:84-87 — the crucible receives
 * {@code fillMoldAtSide(this, opposite, facing)}), the pull NBT round-trip, and the
 * pure {@code walkContinues} half of the DOWN chain (:117-120). The live DOWN walk
 * through a Level is the RCON gate (the capability-dispatch precedent).
 */
public class GT6FaucetTest extends GTOfflineTestBase {

	static BlockEntityType<TestFaucet> sFaucetType;
	static BlockEntityType<TileEntityMold> sMoldType;
	static final BlockPos POS = new BlockPos(7, 4, 2);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		// the material boot BEFORE any BE fixture class-loads TileEntityMold — the BE
		// static blocks read OP.* at <clinit> (the P23/P6 lesson: a null-poisoned static
		// table never re-runs; the production order is data-init BEFORE BE class-load)
		gregapi.oredict.MaterialRegistry.INSTANCE.open();
		MT.init();
		gregapi.data.OP.init();
		gregapi.oredict.MaterialRegistry.INSTANCE.close();
		BlockEntityType<TestFaucet>[] tFaucets = (BlockEntityType<TestFaucet>[]) new BlockEntityType<?>[1];
		tFaucets[0] = BlockEntityType.Builder.of((aPos, aState) -> new TestFaucet(tFaucets[0], aPos, aState), Blocks.STONE).build(null);
		sFaucetType = tFaucets[0];
		BlockEntityType<TileEntityMold>[] tMolds = (BlockEntityType<TileEntityMold>[]) new BlockEntityType<?>[1];
		tMolds[0] = BlockEntityType.Builder.of((aPos, aState) -> new TileEntityMold(tMolds[0], aPos, aState), Blocks.STONE).build(null);
		sMoldType = tMolds[0];
	}

	/** Exposes the protected adjacency override seam (the crank setAdjacencyOverride precedent). */
	static final class TestFaucet extends TileEntityFaucet {
		TestFaucet(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}
		void setAdjacency(BlockEntity aBE) {
			mAdjacentOverride = aBE;
		}
		void saveTag(CompoundTag aNBT) {
			saveAdditional(aNBT);
		}
	}

	/** The recording crucible fake — captures the (mold, sideOfCrucible, sideOfMold) triple. */
	static final class RecordingCrucible implements ITileEntityCrucible {
		ITileEntityMold mMold;
		byte mSideOfCrucible = -1;
		byte mSideOfMold = -1;
		int mCalls = 0;
		boolean mAnswer = true;

		@Override
		public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
			mCalls++;
			mMold = aMold;
			mSideOfCrucible = aSide;
			mSideOfMold = aSideOfMold;
			return mAnswer;
		}
	}

	// ------------------------------------------------------------------------------------
	// the state machine (:151-162)
	// ------------------------------------------------------------------------------------

	@Test
	public void wrenchTogglesAutoPullAndSoftHammerResets() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		assertFalse(tFaucet.mAutoPull);
		assertEquals("Crucible Auto-Input: AUTOMATIC", tFaucet.toggleAutoPull());
		assertTrue(tFaucet.mAutoPull);
		assertEquals("Crucible Auto-Input: REDSTONE", tFaucet.toggleAutoPull());
		assertFalse(tFaucet.mAutoPull);
		tFaucet.toggleAutoPull();
		assertEquals("Crucible Auto-Input: REDSTONE", tFaucet.resetToRedstone());
		assertFalse(tFaucet.mAutoPull);
	}

	/** The mode rides the NBT (the :62/:69 pair, the trimmed port key). */
	@Test
	public void autoPullNbtRoundTrip() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		tFaucet.toggleAutoPull();
		CompoundTag tNBT = new CompoundTag();
		tFaucet.saveTag(tNBT);
		TestFaucet tLoaded = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		assertFalse(tLoaded.mAutoPull);
		tLoaded.load(tNBT);
		assertTrue(tLoaded.mAutoPull);
	}

	// ------------------------------------------------------------------------------------
	// the mold delegate face (:93-135)
	// ------------------------------------------------------------------------------------

	/** Only the mount face (toward the crucible) is the mold input side. */
	@Test
	public void isMoldInputSideIsTheMountFace() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		tFaucet.mFacing = (byte)Direction.WEST.get3DDataValue();
		assertTrue(tFaucet.isMoldInputSide((byte)Direction.WEST.get3DDataValue()));
		assertFalse(tFaucet.isMoldInputSide((byte)Direction.EAST.get3DDataValue()));
		assertFalse(tFaucet.isMoldInputSide((byte)Direction.UP.get3DDataValue()));
	}

	/** The pull drives the faced crucible with (this, opposite-of-facing, facing) — the :86 triple. */
	@Test
	public void pullHitsTheFacedCrucibleWithTheUpstreamTriple() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		tFaucet.mFacing = (byte)Direction.WEST.get3DDataValue();
		RecordingCrucible tCrucible = new RecordingCrucible();
		tFaucet.setAdjacency(new FakeCrucibleBE(tCrucible));
		assertEquals("poured into the mold", tFaucet.pullFromCrucible());
		assertEquals(1, tCrucible.mCalls);
		assertEquals(tFaucet, tCrucible.mMold);
		assertEquals(Direction.EAST.get3DDataValue(), tCrucible.mSideOfCrucible, "the crucible's side toward the faucet");
		assertEquals(Direction.WEST.get3DDataValue(), tCrucible.mSideOfMold, "the faucet's side toward the crucible");
		// the refusal path reads back as the report
		tCrucible.mAnswer = false;
		assertEquals("the crucible refused the pour", tFaucet.pullFromCrucible());
		// no crucible on the facing side
		tFaucet.setAdjacency(null);
		assertEquals("no crucible on the facing side", tFaucet.pullFromCrucible());
	}

	/** The right-click chain answers through the same arm (the :138-146 body). */
	@Test
	public void activateChainPullsFromTheCrucible() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		tFaucet.mFacing = (byte)Direction.NORTH.get3DDataValue();
		RecordingCrucible tCrucible = new RecordingCrucible();
		tFaucet.setAdjacency(new FakeCrucibleBE(tCrucible));
		String tReport = tFaucet.activate(null, tFaucet.mFacing, null);
		assertEquals("poured into the mold", tReport);
		assertEquals(1, tCrucible.mCalls);
	}

	/** The acid gate: a non-acid-proof faucet refuses acid material without calling down. */
	@Test
	public void fillMoldRefusesAcidWhenNotAcidProof() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		tFaucet.mFacing = (byte)Direction.UP.get3DDataValue(); // the acid-refusal happens before any walk
		assertEquals(0, tFaucet.fillMold(new OreDictMaterialStack(MT.HCl, CS.U), 500, tFaucet.mFacing));
		// the side gate
		assertEquals(0, tFaucet.fillMold(new OreDictMaterialStack(MT.Iron, CS.U), 500, (byte)Direction.DOWN.get3DDataValue()));
		// the level-less fixture cannot walk — the pour refuses after the guards
		assertEquals(0, tFaucet.fillMold(new OreDictMaterialStack(MT.Iron, CS.U), 500, tFaucet.mFacing));
	}

	// ------------------------------------------------------------------------------------
	// the DOWN-walk pure half (:117-120)
	// ------------------------------------------------------------------------------------

	@Test
	public void walkContinuesThroughFaucetsAndAirStopsAtMoldsAndSolids() {
		TestFaucet tFaucet = new TestFaucet(sFaucetType, POS, Blocks.STONE.defaultBlockState());
		TileEntityMold tMold = new TileEntityMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		assertTrue(TileEntityFaucet.walkContinues(tFaucet, Blocks.AIR.defaultBlockState()), "a stacked faucet continues the walk");
		assertFalse(TileEntityFaucet.walkContinues(tMold, Blocks.AIR.defaultBlockState()), "a mold stops the walk");
		assertFalse(TileEntityFaucet.walkContinues(null, Blocks.STONE.defaultBlockState()), "a collision cell stops the walk");
		assertTrue(TileEntityFaucet.walkContinues(null, Blocks.AIR.defaultBlockState()), "air falls through");
	}

	/** The BE carrier over the recording crucible (the fake-BE pattern). */
	static final class FakeCrucibleBE extends net.minecraft.world.level.block.entity.BlockEntity implements ITileEntityCrucible {
		private final RecordingCrucible mCrucible;
		FakeCrucibleBE(RecordingCrucible aCrucible) {
			// the FURNACE state (not stone): the 21.1 BlockEntity ctor validates
			// type.isValid(state) — FURNACE is only valid for the furnace block
			super(net.minecraft.world.level.block.entity.BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
			mCrucible = aCrucible;
		}
		@Override
		public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
			return mCrucible.fillMoldAtSide(aMold, aSide, aSideOfMold);
		}
	}
}
