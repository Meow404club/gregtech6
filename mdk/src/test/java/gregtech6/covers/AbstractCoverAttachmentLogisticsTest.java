package gregtech6.covers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.tileentity.logistics.ITileEntityLogistics;
import gregtech6.covers.covers.AbstractCoverAttachmentLogistics;
import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The logistics attachment gate truth tables (task p32-logistics-lv2 acceptance ③, the
 * offline half — the live half is /gt6logistics gate). Verbatim anchor: the placement
 * gate AbstractCoverAttachmentLogistics.java:40 — the host must be an ITileEntityLogistics
 * member whose SIDE_ANY family query answers.
 */
public class AbstractCoverAttachmentLogisticsTest extends GTCoverTestBase {

	/** A concrete logistics attachment — the gate is class-abstract upstream (:39) and here. */
	static final class ProbeAttachment extends AbstractCoverAttachmentLogistics {/**/}

	/** The member host probe — an oven probe that implements the node face (the Lv3 endpoint shape). */
	static class LogisticsOvenProbe extends TileEntityOvenCoverProbe implements ITileEntityLogistics {
		LogisticsOvenProbe(BlockEntityType<TileEntityOvenCoverProbe> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override
		public boolean canLogistics(byte aSide) {
			return true;
		}
	}

	/** The member host whose family query REFUSES — a member that is not currently attachable. */
	static final class ClosedLogisticsOvenProbe extends LogisticsOvenProbe {
		ClosedLogisticsOvenProbe(BlockEntityType<TileEntityOvenCoverProbe> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override
		public boolean canLogistics(byte aSide) {
			return false; // even SIDE_ANY refuses — the upstream :40 conjunction's second arm
		}
	}

	private static final AbstractCoverAttachmentLogistics COVER = new ProbeAttachment();

	@Test
	public void gateRefusesNonMembersAndAnswersMembers() {
		// the pure decision half — the shape /gt6logistics gate drives live
		assertTrue(AbstractCoverAttachmentLogistics.refusesAttachment(null), "no host — refused");
		assertTrue(AbstractCoverAttachmentLogistics.refusesAttachment(new Object()), "a non-member host — refused");
		ITileEntityLogistics tOpen = aSide -> true;
		assertFalse(AbstractCoverAttachmentLogistics.refusesAttachment(tOpen), "a member with the SIDE_ANY answer — allowed");
		ITileEntityLogistics tClosed = aSide -> false;
		assertTrue(AbstractCoverAttachmentLogistics.refusesAttachment(tClosed), "a member whose SIDE_ANY refuses — refused");
	}

	@Test
	public void interceptCoverPlacementRoutesThroughTheGate() {
		// a non-member host (the bare oven probe) — the negative arm of acceptance ③
		TileEntityOvenCoverProbe tOven = bareOven();
		CoverData tOvenData = new CoverData(tOven);
		assertTrue(COVER.interceptCoverPlacement((byte)0, tOvenData, null), "upstream :40 — a non-member host refuses the plate");

		// a member host whose family query answers — the plate mounts
		LogisticsOvenProbe tMember = new LogisticsOvenProbe(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		assertFalse(COVER.interceptCoverPlacement((byte)0, new CoverData(tMember), null), "upstream :40 — a member host mounts");

		// a member host whose family query refuses — the conjunction's second arm
		ClosedLogisticsOvenProbe tClosed = new ClosedLogisticsOvenProbe(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		assertTrue(COVER.interceptCoverPlacement((byte)0, new CoverData(tClosed), null), "upstream :40 — canLogistics(SIDE_ANY)=F refuses");

		// the logistics wire family qualifies as an attachment SUBJECT on every face (the
		// SIDES_INVALID arm of its :47 answer) — the CoverData route stays with the
		// 12-cover card, which mounts ICoverableTE on the wire host (the p31 pipe pattern)
		GTLogisticsWireBlockEntity tWire = offlineWire();
		assertFalse(AbstractCoverAttachmentLogistics.refusesAttachment(tWire),
				"the wire is an ITileEntityLogistics member — canLogistics(6)=T answers the gate");
	}

	/** An offline wire BE without a level (the GTLogisticsWireBlockEntityTest fixture shape, rebuilt here to avoid the package-local BET share). */
	static BlockEntityType<GTLogisticsWireBlockEntity> sWireType;

	@BeforeAll
	static void buildWireFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTLogisticsWireBlockEntity>[] tHolder = (BlockEntityType<GTLogisticsWireBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLogisticsWireBlockEntity(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sWireType = tHolder[0];
	}

	private static GTLogisticsWireBlockEntity offlineWire() {
		return sWireType.create(new BlockPos(2, 2, 4), Blocks.BRICKS.defaultBlockState());
	}
}
