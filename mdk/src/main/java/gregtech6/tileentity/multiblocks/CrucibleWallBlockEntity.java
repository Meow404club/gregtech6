package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.tileentity.machines.ITileEntityCrucible;
import gregapi.tileentity.machines.ITileEntityMold;
import gregtech6.registry.GT6Crucibles;

/**
 * The LARGE-crucible wall part BE (task p26-crucible-multiblock SPEC ③) — the
 * {@link MultiBlockPartBlockEntity} that carries BOTH through-wall relays the crucible
 * chain needs, as a subclass pair rather than a touch on the shared part BE (the p13
 * FORBIDDEN zero-diff ruling on MultiBlockPartBlockEntity stands unbroken):
 * <ul>
 * <li>the ENERGY relay, inherited whole from {@link HeatTransmitterBlockEntity} — the
 *     y+0 ring's ONLY_ENERGY_IN mode opens exactly the HU intake (the burning boxes
 *     touch HERE, upstream MultiTileEntityCrucible.java:119), while the y+1/y+2 rings
 *     carry NO_ENERGY_IN and refuse (the mode gate does the layering, no extra code);</li>
 * <li>the CRUCIBLE relay, {@link #fillMoldAtSide} — upstream MultiTileEntityMultiBlockPart
 *     :686-692 verbatim: the NO_CRUCIBLE mode bit kills it first (so only the y+1
 *     ONLY_CRUCIBLE ring answers — :120), then {@code getTarget(true)} (the ownership +
 *     validity probe) and the controller's own fillMoldAtSide (the TileEntityCrucible
 *     :547-556 pour). This is the "wall part answers the mold" half of the through-wall
 *     proxy: the mold BE clicks the WALL it is mounted against, the wall forwards
 *     controller-ward.</li>
 * </ul>
 *
 * <p>Declared deviation from the upstream part: the upstream base implemented the whole
 * ITileEntityCrucible face on EVERY part; the port keeps the single-method seam and
 * mounts it on this crucible-family BE only — the other multiblock families (coke oven,
 * boilers, lightning rod) have no crucible semantics to relay.
 */
public class CrucibleWallBlockEntity extends HeatTransmitterBlockEntity implements ITileEntityCrucible {

	/** The registry-path constructor (the GT6Crucibles wall-BET factory form). */
	public CrucibleWallBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen-registry form). */
	public CrucibleWallBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Crucibles.CRUCIBLE_WALL_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_crucible_wall"; // BET registry path mirrors it (GT6Crucibles.CRUCIBLE_WALL_BE)
	}

	// ---------------------------------------------------------------------------
	// the through-wall crucible relay (upstream MultiTileEntityMultiBlockPart :686-692)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :687-691 verbatim: the NO_CRUCIBLE gate first (the y+0/y+2 rings refuse —
	 * only the ONLY_CRUCIBLE y+1 layer pours, :120), then the validity-probed controller
	 * forward, the silent false on any miss (no controller, controller not a crucible).
	 */
	@Override
	public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold) {
		if ((mMode & NO_CRUCIBLE) != 0) return false; // :688
		ITileEntityMultiBlockController tTarget = getTarget(true); // :689 — the validity probe
		if (tTarget instanceof ITileEntityCrucible tCrucible) return tCrucible.fillMoldAtSide(aMold, aSide, aSideOfMold); // :690
		return false;
	}
}
