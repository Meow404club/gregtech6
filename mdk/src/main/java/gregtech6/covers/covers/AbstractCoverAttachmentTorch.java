package gregtech6.covers.covers;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The torch-cover base — 1.20.1 port of
 * gregapi/cover/covers/AbstractCoverAttachmentTorch.java:34-81 (task
 * p34-covers-gameplay-10). A torch-family cover mounts on a redstone wire
 * (upstream {@code MultiTileEntityWireRedstoneInsulated}; the port's shared carrier
 * {@link GTWireBlockEntity}), mirrors the wire's signal state into the visual lane
 * through the abstract {@link #condition} (:60-68 — the ON art is visual 0, the
 * upstream literal), and emits a FULL 15 redstone through the covered face exactly
 * while the art is ON (:50-57 — visual 0 = 15, visual 1 = 0). The wire redstone
 * surface is the port's {@code GTWireBlockEntity.mRedstone} full-range long, and the
 * family flag folds into the carrier-class check (the declared gate deviation below).
 *
 * <p>DECLARED DEVIATION — the host gate: upstream keys the insulated wire CLASS
 * (:35); the port keeps every wire family on the one carrier class, so the class gate
 * becomes the carrier class AND the host's own redstone-family flag
 * ({@link GTWireBlockEntity#isRedstone()} — the gate the wire class answers everything
 * redstone through). The narrowing is a COVER-SIDE declaration (the P34 policy ruling:
 * the host's {@code allowCovers} stays the zero-narrowing interface default) — an
 * electric- or laser-family wire row refuses the torch exactly as the upstream
 * electric wire classes did. {@code interceptConnect} (:36) rides the pooled
 * connector-hooks group. The bounds pair (:75-76 BOXES_TORCHES) rides the cut
 * collision surface. The placement disconnect arm (:39-42) ports through the wire BE's
 * own public {@code disconnect} (the torch face must not conduct).
 */
public abstract class AbstractCoverAttachmentTorch extends AbstractCoverDefault {

	/**
	 * Upstream :35 — the torch family only mounts on the REDSTONE family of the wire
	 * carrier (task p35-cover-narrowing-render-snapshot: the P34 carrier-class gate
	 * narrows to the redstone-class rows; electric/laser wire rows refuse).
	 */
	@Override
	public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, @Nullable Entity aPlayer) {
		return !(aData.mTileEntity instanceof GTWireBlockEntity tWire && tWire.isRedstone());
	}

	/**
	 * Upstream :39-42 — placement disconnects the wire's own covered face (the torch
	 * face is an emission face, not a through-connection) and plays the wood dig sound
	 * (:41 — the base vanilla placeholder rides instead, the AbstractCoverDefault form).
	 */
	@Override
	public void onCoverPlaced(byte aSide, CoverData aData, @Nullable Entity aPlayer, ItemStack aCover) {
		super.onCoverPlaced(aSide, aData, aPlayer, aCover);
		if (aData.mTileEntity instanceof GTWireBlockEntity tWire && tWire.connected(aSide)) tWire.disconnect(aSide, true);
	}

	/** Upstream :50-53 — the strong emission is FULL while the art is ON (visual 0). */
	@Override
	public byte getRedstoneOutStrong(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return (byte) (aData.mVisuals[aCoverSide] == 0 ? 15 : 0);
	}

	/** Upstream :55-57 — weak == strong verbatim on the torch family. */
	@Override
	public byte getRedstoneOutWeak(byte aCoverSide, CoverData aData, byte aDefaultRedstone) {
		return (byte) (aData.mVisuals[aCoverSide] == 0 ? 15 : 0);
	}

	/** Upstream :60-68 — the per-tick condition flip, block-update flagged. */
	@Override
	public void onTickPost(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && aData.mTileEntity instanceof GTWireBlockEntity) {
			if (condition(aSide, aData, aTimer, aIsServerSide, aReceivedBlockUpdate, aReceivedInventoryUpdate)) {
				if (aData.mVisuals[aSide] == 0) aData.visual(aSide, (short) 1, true);
			} else {
				if (aData.mVisuals[aSide] != 0) aData.visual(aSide, (short) 0, true);
			}
		}
	}

	/** Upstream :70 — the ON/OFF state survives the save (the CoverData writeToNBT :87 gate). */
	@Override
	public boolean needsVisualsSaved(byte aSide, CoverData aData) {
		return true;
	}

	/** Upstream :71-73 — the torch stem is non-solid, non-opaque and never a full texture. */
	@Override
	public boolean isSolid(byte aSide, CoverData aData) {return false;}

	@Override
	public boolean isOpaque(byte aSide, CoverData aData) {return false;}

	@Override
	public boolean isFullTexture(byte aCoverSide, CoverData aData) {return false;}

	/** Upstream :78 — the wire-state predicate the concrete torch answers. */
	public abstract boolean condition(byte aSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate);

	/** The ON/OFF front sprite pair helper (upstream the sTextureFront pair per family). */
	//? if forge {
	public static ResourceLocation spriteOf(String aFamily, boolean aOn) {
		return new ResourceLocation("gt6", "block/" + aFamily + "/" + (aOn ? "on" : "off") + "/front");
	}
	//?} else {
	/*public static ResourceLocation spriteOf(String aFamily, boolean aOn) {
		return ResourceLocation.fromNamespaceAndPath("gt6", "block/" + aFamily + "/" + (aOn ? "on" : "off") + "/front");
	}
	 *///?}
}
