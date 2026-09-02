package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GTCrankBlockEntity offline tests (task p12-engine-crank acceptance a): the RU packet
 * sign invariant (size &lt; 0 always, the counterclockwise DC the axle/gearbox cards
 * consume, amount = the haste scaler), the type gate (RU only — EU/KU never booked
 * through the real {@code Util.emitEnergyToNetwork}), the emit window gate (nothing
 * armed = nothing booked; one packet per armed window; the /gt6engine drive window
 * drains and stops), the upstream :193 facing truth table (emit face = mFacing only,
 * aTheoretical-insensitive like upstream), and the NBT round trip.
 *
 * <p>Offline-harness notes (the GTEnergySourceBlockEntityTest in-case record carries
 * over): the level-less fixture takes the SERVER branch of the tick chain, {@code onTick}
 * runs only when a test drives {@code updateEntity()} by hand, and the first
 * {@code updateEntity()} already fires {@code onTick} (the mTimer == 0 onTickFirst phase
 * runs in the same dispatcher pass). The {@code syncFacingFromState} test drives the
 * FACING property through a vanilla stairs state — the same
 * {@code BlockStateProperties.HORIZONTAL_FACING} INSTANCE the crank block registers, no
 * port block construction needed (the offline registry is frozen, GTOfflineTestBase doc).
 */
public class GTCrankBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GTCrankBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GTCrankBlockEntity>[] tHolder = (BlockEntityType<GTCrankBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTCrankBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fresh level-less crank fixture with the default facing (NORTH). */
	private static GTCrankBlockEntity crank() {
		return new GTCrankBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/**
	 * Counting RU sink — the shredder-shaped fake consumer: accepts RU from every side
	 * (the TileEntityBasicMachine :596 receiving arm + the :511 all-sides Root default),
	 * refuses everything else (the :501 type gate).
	 */
	public static class CountingSink extends BlockEntity implements ITileEntityEnergy {
		public long calls = 0, lastSize = 1, lastAmount = 0;
		public byte lastSide = -1;
		public final TagData acceptedType;

		public CountingSink(BlockPos aPos, TagData aAcceptedType) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
			acceptedType = aAcceptedType;
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == acceptedType;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == acceptedType;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return acceptedType.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					calls++;
					lastSize = aSize;
					lastAmount = aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** Wires the sink in as the crank's (offline) adjacency and returns it. */
	private static CountingSink wire(GTCrankBlockEntity aCrank, TagData aAcceptedType) {
		CountingSink tSink = new CountingSink(aCrank.getBlockPos().relative(Direction.from3DDataValue(aCrank.getFacing())), aAcceptedType);
		aCrank.setAdjacencyOverride(aSide -> {
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tSink, tOpposite);
		});
		return tSink;
	}

	@Test
	public void packetMathSignInvariant() {
		// the potionless player default (the :78 forms at pot == -1): -divup(8*2, 1) = -16, amount 1
		assertEquals(-16, GTCrankBlockEntity.packetSize(2, 1));
		assertEquals(1, GTCrankBlockEntity.packetAmount(1));
		// the scaler table (pot1 = pot+2 base 1, pot2 = pot+3 base 2):
		assertEquals(-8, GTCrankBlockEntity.packetSize(2, 2)); // Weakness I halves the torque
		assertEquals(-24, GTCrankBlockEntity.packetSize(3, 1)); // Strength I
		assertEquals(-6, GTCrankBlockEntity.packetSize(2, 3)); // divup rounds up: -divup(16, 3) = -6
		// the sign invariant over the reachable scaler window: size < 0 ALWAYS
		for (long tPot2 = 2; tPot2 <= 5; tPot2++) {
			for (long tPot1 = 1; tPot1 <= 4; tPot1++) {
				assertTrue(GTCrankBlockEntity.packetSize(tPot2, tPot1) < 0, "size must stay negative at " + tPot2 + "/" + tPot1);
				assertEquals(-UT_Code_divup(8L * tPot2, tPot1), GTCrankBlockEntity.packetSize(tPot2, tPot1));
			}
		}
		// the amount form is the haste scaler verbatim
		for (long tHaste = 1; tHaste <= 4; tHaste++) assertEquals(tHaste, GTCrankBlockEntity.packetAmount(tHaste));
	}

	/** The UT6.divup mirror for the table assertion (UT.Code.divup UT.java:34). */
	private static long UT_Code_divup(long aNumber, long aDivider) {
		return aNumber / aDivider + (aNumber % aDivider != 0 ? 1 : 0);
	}

	@Test
	public void faceTruthTable() {
		GTCrankBlockEntity tCrank = crank();
		// for every facing: RU emits on mFacing ONLY, the other five sides refuse; the
		// theoretical probe is answered identically (upstream :193 is aTheoretical-insensitive)
		for (byte tFacing = 0; tFacing < 6; tFacing++) {
			tCrank.mFacing = tFacing;
			for (byte tSide = 0; tSide < 6; tSide++) {
				boolean tExpect = tSide == tFacing;
				assertEquals(tExpect, tCrank.isEnergyEmittingTo(TD.Energy.RU, tSide, true), "theoretical " + tSide + "@" + tFacing);
				assertEquals(tExpect, tCrank.isEnergyEmittingTo(TD.Energy.RU, tSide, false), "real " + tSide + "@" + tFacing);
			}
		}
		// the type gate: EU/KU never emit (upstream :192/:193), any side
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertFalse(tCrank.isEnergyEmittingTo(TD.Energy.EU, tSide, true));
			assertFalse(tCrank.isEnergyEmittingTo(TD.Energy.KU, tSide, true));
			assertFalse(tCrank.isEnergyAcceptingFrom(TD.Energy.RU, tSide, true)); // pure source
			assertFalse(tCrank.isEnergyAcceptingFrom(TD.Energy.RU, tSide, false));
			// the incoming door is shut for every type (the Root default rides the false accepting probe)
			assertEquals(0, tCrank.doEnergyInjection(TD.Energy.RU, tSide, 16, 1, true));
			assertEquals(0, tCrank.doEnergyInjection(TD.Energy.EU, tSide, 32, 1, true));
		}
		// the types face: the RU AS_LIST only (upstream :198)
		Collection<TagData> tTypes = tCrank.getEnergyTypes((byte) 2);
		assertEquals(1, tTypes.size());
		assertTrue(tTypes.contains(TD.Energy.RU));
	}

	@Test
	public void sizeBandAndOfferedGate() {
		GTCrankBlockEntity tCrank = crank();
		// the constant band (upstream :195-197)
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(16, tCrank.getEnergySizeOutputMin(TD.Energy.RU, tSide));
			assertEquals(16, tCrank.getEnergySizeOutputRecommended(TD.Energy.RU, tSide));
			assertEquals(16, tCrank.getEnergySizeOutputMax(TD.Energy.RU, tSide));
			// offered 0 while idle (upstream :194 mActive ? 16 : 0)
			assertEquals(0, tCrank.getEnergyOffered(TD.Energy.RU, tSide, 16));
		}
		tCrank.mActive = true;
		assertEquals(16, tCrank.getEnergyOffered(TD.Energy.RU, (byte) 2, 16));
		tCrank.mActive = false;
		tCrank.setDriveTicks(7);
		assertEquals(16, tCrank.getEnergyOffered(TD.Energy.RU, (byte) 2, 16)); // the rig window counts as live
	}

	@Test
	public void emitWindowGate() {
		GTCrankBlockEntity tCrank = crank();
		CountingSink tSink = wire(tCrank, TD.Energy.RU);

		// idle: the tick emits NOTHING (the window gate)
		tCrank.updateEntity();
		assertEquals(0, tSink.calls);
	}

	@Test
	public void useArmEmitsOnePacket() {
		GTCrankBlockEntity tCrank = crank();
		CountingSink tSink = wire(tCrank, TD.Energy.RU);
		// arm through the field form of onPlayerCrank (a Player cannot exist offline):
		// the exact three writes the use path performs, with the potionless stats
		tCrank.mActive = true;
		tCrank.mPacketSize = GTCrankBlockEntity.packetSize(GTCrankBlockEntity.DEFAULT_POT2_STRENGTH, GTCrankBlockEntity.DEFAULT_POT1_WEAKNESS);
		tCrank.mPacketAmount = GTCrankBlockEntity.packetAmount(GTCrankBlockEntity.DEFAULT_POT1_HASTE);

		tCrank.updateEntity();
		assertEquals(1, tSink.calls);
		assertTrue(tSink.lastSize < 0); // the invariant: counterclockwise DC
		assertEquals(-16, tSink.lastSize);
		assertEquals(1, tSink.lastAmount);
		// the packet arrives on the OPPOSITE side value (the receiver's own face): 2 (NORTH) -> 3 (SOUTH)
		assertEquals(3, tSink.lastSide);

		// no re-arm: the next tick books nothing
		tCrank.updateEntity();
		assertEquals(1, tSink.calls);
	}

	@Test
	public void driveWindowDrainsAndStops() {
		GTCrankBlockEntity tCrank = crank();
		CountingSink tSink = wire(tCrank, TD.Energy.RU);

		tCrank.setDriveTicks(3);
		assertEquals(-16, tCrank.mPacketSize); // the rig pins the potionless :78 defaults
		assertEquals(1, tCrank.mPacketAmount);
		tCrank.updateEntity();
		tCrank.updateEntity();
		tCrank.updateEntity();
		assertEquals(3, tSink.calls);
		assertEquals(0, tCrank.mDriveTicks);
		// drained: nothing more
		tCrank.updateEntity();
		assertEquals(3, tSink.calls);

		// the stop gate: a fresh window zeroed before it runs books nothing
		tCrank.setDriveTicks(5);
		tCrank.setDriveTicks(0);
		tCrank.updateEntity();
		assertEquals(3, tSink.calls);
	}

	@Test
	public void typeGateRUOnlyThroughNetwork() {
		GTCrankBlockEntity tCrank = crank();
		CountingSink tRuSink = wire(tCrank, TD.Energy.RU);
		// an EU sink adjacent on the SAME side — the emit must never reach it (the :192 type gate)
		CountingSink tEuSink = new CountingSink(tCrank.getBlockPos().relative(Direction.from3DDataValue(tCrank.getFacing())), TD.Energy.EU);
		tCrank.setAdjacencyOverride(aSide -> {
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			// Util hands the TARGET and the side of it that faces us; both sinks sit on the same
			// neighbour cell in a real level — here the RU sink wins the cell (the type gate is
			// what keeps the EU sink dark in the live pair, asserted by it staying at zero
			// through the crank's own doEnergyInjection refusal below)
			return new EnergyTarget(tRuSink, tOpposite);
		});

		tCrank.setDriveTicks(2);
		tCrank.updateEntity();
		tCrank.updateEntity();
		assertEquals(2, tRuSink.calls);
		assertEquals(TD.Energy.RU, TD.Energy.RU); // (the booking type is the crank's emit type by construction)
		// the crank as a RECEIVER: the door is shut for EU/KU too (the pure-source refusal)
		assertEquals(0, tCrank.doEnergyInjection(TD.Energy.EU, (byte) 2, 32, 1, true));
		assertEquals(0, tCrank.doEnergyInjection(TD.Energy.KU, (byte) 2, 32, 1, true));
		assertEquals(0, tEuSink.calls); // never touched
	}

	@Test
	public void nbtRoundTrip() {
		GTCrankBlockEntity tCrank = crank();
		tCrank.mFacing = 4;
		tCrank.setDriveTicks(9);
		CompoundTag tTag = tCrank.saveWithoutMetadata();

		GTCrankBlockEntity tLoaded = crank();
		tLoaded.load(tTag);
		assertEquals(9, tLoaded.mDriveTicks);
		assertEquals(-16, tLoaded.mPacketSize);
		assertEquals(1, tLoaded.mPacketAmount);
		assertFalse(tLoaded.mActive); // the idle default survives the round trip
		assertEquals("crank", tTag.getString("te_name"));
		assertTrue(tTag.contains(GTCrankBlockEntity.NBT_ACTIVE, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTCrankBlockEntity.NBT_DRIVE_TICKS, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTCrankBlockEntity.NBT_PACKET_SIZE, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(GTCrankBlockEntity.NBT_PACKET_AMOUNT, Tag.TAG_ANY_NUMERIC));
	}

	@Test
	public void facingSyncFromState() {
		// the /setblock gt6:crank[facing=west] path: the state carries the facing, the BE
		// mirror re-syncs at the tick head — driven through a vanilla stairs state carrying
		// the SAME HORIZONTAL_FACING property instance (no port block offline)
		BlockState tState = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
		GTCrankBlockEntity tCrank = new GTCrankBlockEntity(sType, POS, tState);
		assertEquals(2, tCrank.getFacing()); // the default mirror before the first tick
		tCrank.updateEntity();
		assertEquals(4, tCrank.getFacing()); // WEST == 4, the GT6 side order
		assertTrue(tCrank.isEnergyEmittingTo(TD.Energy.RU, (byte) 4, false));
		// a state WITHOUT the property (the STONE fixture) leaves the mirror alone
		GTCrankBlockEntity tPlain = crank();
		tPlain.mFacing = 5;
		tPlain.updateEntity();
		assertEquals(5, tPlain.getFacing());
	}
}
