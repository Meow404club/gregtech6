package gregtech6.tileentity.energy;

import gregapi.tileentity.energy.EnergyBridge;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;

/**
 * Shared offline fixtures for the dynamo-family tests (task p28-c-dynamo-family-be) — the
 * hand-verified row tables (the Loader :946-957 registration columns), the counting FE
 * sink (the {@code storage::receiveEnergy} lambda shape the Flux BE adapts on the live
 * legs) and the one-sided EU adjacency (the southOf fixture shape).
 */
public final class GT6DynamoBlockEntityTestHarness {

	/**
	 * The five Flux rows {NBT_INPUT RU, NBT_OUTPUT FE} (Loader :953-957) — each pair
	 * exactly 2.75. The Electric pair rides {@link #ELECTRIC_ROWS} (exactly 0.6875).
	 */
	public static final long[][] DYNAMO_ROWS = {{32, 88}, {128, 352}, {512, 1408}, {2048, 5632}, {8192, 22528}};

	/** The five Electric rows {NBT_INPUT RU, NBT_OUTPUT EU} (Loader :946-950). */
	public static final long[][] ELECTRIC_ROWS = {{32, 22}, {128, 88}, {512, 352}, {2048, 1408}, {8192, 5632}};

	/** The one-sided adjacency: only {@code aSide} resolves, to the sink's opposite face (the GTEnergySource fixture shape). */
	public static IEnergyAdjacency adjacencyAt(gregapi.tileentity.energy.ITileEntityEnergy aSink, byte aSide) {
		byte tOpposite = (byte) net.minecraft.core.Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
		return aVisited -> aVisited == aSide ? new EnergyTarget((net.minecraft.world.level.block.entity.BlockEntity)aSink, tOpposite) : null;
	}

	/**
	 * A counting {@link EnergyBridge.IFEReceiver} with a per-call acceptance cap — the fake
	 * FE cable (the LimitedSink shape of the FE-converter test, lambda-carried). totalFe
	 * accumulates what the train actually delivered; lastSize pins the packet size.
	 */
	public static final class CountingFeSink implements EnergyBridge.IFEReceiver {
		public long totalFe = 0, lastSize = -1;
		private final long mAcceptCap;

		public CountingFeSink(long aAcceptCap) {
			mAcceptCap = aAcceptCap;
		}

		@Override
		public int receiveEnergy(int aAmount, boolean aSimulate) {
			int tTake = (int)Math.min(aAmount, mAcceptCap);
			if (!aSimulate && tTake > 0) {
				totalFe += tTake;
				lastSize = aAmount;
			}
			return tTake;
		}
	}

	private GT6DynamoBlockEntityTestHarness() {}
}
