package gregtech6.tileentity.connectors;

import java.util.Collection;

import javax.annotation.Nullable;

import gregapi.code.TagData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of gregapi/tileentity/connectors/TileEntityBase09Connector.java
 * (177 lines) — the connector base the pipe (and any later wire family) ports onto.
 * Mounts the 03 ticking chain directly; the upstream base classes between 03 and 09
 * (04TileEntityBase / 05Inventories / 06Covers / 07Energy / 08Directional) contribute
 * nothing the W1 pipe consumes.
 *
 * <p>Port scope (task p4-fluid-pipes spec ②):
 * <ul>
 * <li>{@code mConnections} — the 6-bit connection mask (upstream :50), NBT persisted
 *     with the {@code & 63} clamp on read (upstream :55/:61, key "gt.connection"
 *     CS.java:1194, in-repo plain key form "connections");</li>
 * <li>{@link #connected(byte)} — the FACE_CONNECTED[aSide][mConnections] lookup
 *     (upstream :106-108) as the equivalent {@code (mConnections & SBIT[aSide]) != 0}
 *     bit test (CS.java:598-612 defines the table as exactly that); the GT6 side order
 *     0..5 equals Direction.get3DDataValue() order (TileEntityBase01Root port doc);</li>
 * <li>{@link #connect(byte, boolean)} — the symmetric handshake (upstream :111-153):
 *     a neighbouring connector only connects when the two {@link #getConnectorTypes}
 *     sets share an element (upstream :118, UT.Code.haveOneCommonElement), and the
 *     aNotify recursion sets the partner's bit with aNotify=false (:126); non-connector
 *     neighbours connect when the target is air/liquid or {@link #canConnect} says so
 *     (upstream :141);</li>
 * <li>{@link #disconnect(byte, boolean)} — the mirrored handshake (upstream :156-172);</li>
 * <li>the override hooks {@link #onConnectionChange(byte)} / {@link #canConnect(byte, BlockEntity)}
 *     (upstream :175-176) — onConnectionChange is where the pipe pushes its mask into
 *     the CONNECTS BlockState property.</li>
 * </ul>
 *
 * <p>Omissions: the cover intercept gates (upstream :114/:117/:160-161, cover route is
 * W3), the redstone-wire connect-anywhere special case (upstream :130-140), the machine
 * block update fan-out and the E-net update (no consumers), the wrench tool click and
 * tooltips (tool system is a later pool item).
 */
public abstract class TileEntityBase09Connector extends TileEntityBase03TicksAndSync {

	/** The NBT key of {@link #mConnections} (upstream "gt.connection", CS.java:1194). */
	public static final String NBT_CONNECTION = "connections";

	/** Upstream :50 — one bit per side, GT6 side order == Direction.get3DDataValue() order. */
	protected byte mConnections = 0;

	/** Old value for the change-detection sync gate (the oX/oY/oZ idiom of the chest mUsingPlayers). */
	protected byte oConnections = 0;

	protected TileEntityBase09Connector(boolean aIsTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aIsTicking, aType, aPos, aState);
	}

	/**
	 * The connector type tags this connector offers on aSide (upstream ITileEntityConnector,
	 * consumed by the intersection check :118). Two connectors connect only on a common element.
	 */
	public abstract Collection<TagData> getConnectorTypes(byte aSide);

	/** Upstream :98/:55 — the mask is also the direction data; the 63 clamp is the whole 6-bit space. */
	public byte getConnections() {
		return (byte)(mConnections & 63);
	}

	// ---------------------------------------------------------------------------
	// connection state (upstream :106-108, FACE_CONNECTED as a bit test)
	// ---------------------------------------------------------------------------

	public boolean connected(byte aSide) {
		return aSide >= 0 && aSide < 6 && (mConnections & SBIT[aSide]) != 0;
	}

	/** The connection change fan-out of upstream :119-128/:142-149 — bit set plus the notify chain. */
	protected void setConnectionBit(byte aSide) {
		byte oConnections = mConnections;
		mConnections |= SBIT[aSide];
		updateClientData();
		causeBlockUpdate();
		onConnectionChange(oConnections);
	}

	// ---------------------------------------------------------------------------
	// handshake (upstream :111-153 connect / :156-172 disconnect)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :111-153. Connects aSide towards whatever sits there: a connector of a
	 * compatible type (symmetric with aNotify), or a non-connector when the spot is
	 * air/liquid or {@link #canConnect} accepts it.
	 */
	public boolean connect(byte aSide, boolean aNotify) {
		if (aSide < 0 || aSide >= 6) return false;
		if (connected(aSide)) return true;
		if (!hasLevel()) return false;

		BlockPos tTarget = getBlockPos().relative(Direction.from3DDataValue(aSide));
		BlockEntity tNeighbor = getLevel().getBlockEntity(tTarget);
		if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
			// upstream :118 — types must intersect, the partner side is this side's opposite
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			if (haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(aSide))) {
				setConnectionBit(aSide);
				if (aNotify) tConnector.connect(tOpposite, false); // upstream :126 — symmetric, one recursion deep
				return true;
			}
		} else if (tNeighbor != null ? canConnect(aSide, tNeighbor)
				: isAirOrLiquid(tTarget)) { // upstream :141 — open pipe ends connect into air/liquid
			setConnectionBit(aSide);
			return true;
		}
		return connected(aSide);
	}

	/** Upstream :156-172 — mirrored handshake, same intersection gate on the notify (upstream :170). */
	public boolean disconnect(byte aSide, boolean aNotify) {
		if (aSide < 0 || aSide >= 6) return false;
		if (!connected(aSide)) return true;
		byte oConnections = mConnections;
		mConnections &= ~SBIT[aSide];
		updateClientData();
		causeBlockUpdate();
		onConnectionChange(oConnections);
		if (aNotify && hasLevel()) {
			byte tOpposite = (byte)Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector && tConnector.connected(tOpposite)
					&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(aSide))) {
				tConnector.disconnect(tOpposite, false);
			}
		}
		return true;
	}

	// ---------------------------------------------------------------------------
	// hooks and helpers
	// ---------------------------------------------------------------------------

	/** Upstream :175 — override to push the mask into visuals (the pipe writes its CONNECTS property here). */
	public void onConnectionChange(byte aPreviousConnections) {/**/}

	/** Upstream :176 — the non-connector BE acceptance hook (the pipe accepts fluid handlers here). */
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {return false;}

	/** Upstream :141 WD.air/WD.liquid. */
	private boolean isAirOrLiquid(BlockPos aPos) {
		BlockState tState = getLevel().getBlockState(aPos);
		FluidState tFluid = tState.getFluidState();
		return tState.isAir() || (tFluid != null && !tFluid.isEmpty());
	}

	/** Upstream UT.Code.haveOneCommonElement. */
	public static boolean haveOneCommonElement(Collection<TagData> aA, Collection<TagData> aB) {
		if (aA == null || aB == null || aA.isEmpty() || aB.isEmpty()) return false;
		for (TagData tTag : aA) if (aB.contains(tTag)) return true;
		return false;
	}

	/** SBIT table (CS.java:612, first six entries — the GT6 side order). */
	public static final byte SBIT[] = {1, 2, 4, 8, 16, 32};

	// ---------------------------------------------------------------------------
	// NBT (upstream :53-62) and the change-detection sync gate
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_CONNECTION, mConnections);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_CONNECTION, Tag.TAG_ANY_NUMERIC)) {
			mConnections = (byte)(aNBT.getByte(NBT_CONNECTION) & 63); // upstream :55 verbatim clamp
		}
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		return mConnections != oConnections || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oConnections = mConnections;
	}
}
