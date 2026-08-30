package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.Test;

import gregtech6.util.UT6;

/**
 * Acceptance 1 (task p6-oven-rotation): the setFrontFacing rotation entry — the GTCEu
 * MetaMachine :794-811 counterpart. The same-facing call is the :796 no-op, vertical
 * (0/1) and invalid sides are rejected, a horizontal re-facing writes mFacing and
 * triggers the BlockState re-application ({@link #applyVisualState()} — the spec-7
 * double write), and the rotated facing survives the NBT round trip.
 *
 * <p>The literal BlockState FACING assertion is NOT possible on this offline base: a
 * real GTOvenBlock instance cannot be constructed post-bootstrap (the Forge-patched
 * Block ctor needs a writable registry — "Registry is already frozen") and the stub
 * level has no chunk source for setBlock. The re-application trigger is asserted here
 * through the recording override; the real setBlock(state, 3) FACING write is covered
 * live by the RCON chain ({@code execute if block ... gt6:oven[facing=...]}).
 */
public class TileEntityOvenFacingTest extends GTMachinesOfflineTestBase {

	/** Records the applyVisualState invocations — the BlockState re-application trigger. */
	public static class RecordingOven extends TileEntityOven {
		public int mVisualApplications = 0;

		public RecordingOven() {
			super(sOvenType, POS, Blocks.BRICKS.defaultBlockState());
			setLevel(emptyLevel());
		}

		@Override
		public void applyVisualState() {
			mVisualApplications++;
		}
	}

	@Test
	public void sameFacingIsTheNoOp() {
		RecordingOven tOven = new RecordingOven();
		tOven.mFacing = 3;
		assertFalse(tOven.setFrontFacing((byte)3), "the :796 same-facing no-op");
		assertEquals(3, tOven.getFacing(), "the facing is untouched");
		assertEquals(0, tOven.mVisualApplications, "no BlockState re-application");
	}

	@Test
	public void verticalAndInvalidSidesAreRejected() {
		RecordingOven tOven = new RecordingOven();
		tOven.mFacing = 2;
		assertFalse(tOven.setFrontFacing((byte)0), "DOWN is vertical");
		assertFalse(tOven.setFrontFacing((byte)1), "UP is vertical");
		assertFalse(tOven.setFrontFacing(UT6.SIDE_INVALID), "SIDE_INVALID is not a side");
		assertEquals(2, tOven.getFacing(), "rejected calls leave the facing");
		assertEquals(0, tOven.mVisualApplications, "no BlockState re-application");
	}

	@Test
	public void horizontalRefacingRotatesAndTriggersTheVisualApplication() {
		RecordingOven tOven = new RecordingOven(); // default facing NORTH (2)
		assertTrue(tOven.setFrontFacing((byte)5), "EAST re-facing accepted");
		assertEquals(5, tOven.getFacing());
		assertEquals(1, tOven.mVisualApplications, "applyVisualState fired for the rotation");
		assertTrue(tOven.setFrontFacing((byte)3), "SOUTH re-facing accepted");
		assertEquals(3, tOven.getFacing());
		assertEquals(2, tOven.mVisualApplications);
		assertFalse(tOven.setFrontFacing((byte)3), "the new front itself is the no-op");
		assertEquals(2, tOven.mVisualApplications, "the same-facing no-op does not re-apply");
	}

	@Test
	public void rotatedFacingSurvivesNbtRoundTrip() {
		TileEntityOven tOven = makeOven(emptyLevel());
		assertTrue(tOven.setFrontFacing((byte)4)); // WEST
		CompoundTag tTag = tOven.saveWithoutMetadata();
		assertEquals(4, tTag.getByte(TileEntityOven.NBT_FACING), "the NBT_FACING key written");

		TileEntityOven tRestored = sOvenType.create(POS2, Blocks.BRICKS.defaultBlockState());
		tRestored.setLevel(tOven.getLevel());
		tRestored.load(tTag);
		assertEquals(4, tRestored.getFacing(), "NBT_FACING persists the rotation");
	}
}
