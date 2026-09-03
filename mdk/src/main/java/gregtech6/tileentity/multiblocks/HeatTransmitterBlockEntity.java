package gregtech6.tileentity.multiblocks;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase01Root;

/**
 * The Heat Transmitter part BE — the energy-relaying half of the multiblock part (upstream
 * MultiTileEntityMultiBlockPart.java:496-595 "Relay Energy" section verbatim, task
 * p13-large-boiler SPEC ④): a {@link MultiBlockPartBlockEntity} whose ITileEntityEnergy
 * face forwards to the CONTROLLER through the mode gate.
 *
 * <p><b>The upstream relay shape, each method</b>: the mMode gate first (a NO_ENERGY_IN /
 * NO_ENERGY_OUT bit kills the direction — the ONLY_ENERGY_IN mode of the :104-112 base
 * layer is exactly {@code ~NO_ENERGY_IN}), then {@code getTarget(true)} (the ownership +
 * validity probe — a part without a live controller answers the empty default), then the
 * controller's same-named ITileEntityEnergy face, the empty default on any miss.
 *
 * <p><b>Why a separate class</b>: the shared part BE relays only the three Forge
 * CAPABILITIES (task p4), while the firebox emit path (W2) reaches receivers through the
 * ITileEntityEnergy interface directly ({@code ITileEntityEnergy.Util.insertEnergyInto →
 * receiver.doEnergyInjection}) — a plain part BE would let the Root injection gate SWALLOW
 * the packet instead of forwarding it (the HU would never reach the controller; the RCON
 * chain first-run would catch it as a dead loop). The upstream base carried this relay on
 * every part; the port cut it with the capability relay (task p4) and this subclass
 * restores it for the one part family that needs it, keeping MultiBlockPartBlockEntity
 * itself zero-diff (the FORBIDDEN ruling).
 *
 * <p><b>The aPart parameter is a discarded parameter upstream</b> — the
 * TileEntityBase10MultiBlockBase relay bodies (:206-225 family) forward
 * {@code (aPart, aEnergyType, aSide, ...)} to the machine's own {@code (aEnergyType,
 * aSide, ...)} methods verbatim, and the Large Boiler's implementations (:367-378) never
 * read the part. Forwarding straight to the controller's ITileEntityEnergy face is
 * therefore the same semantics without porting the IMultiBlockEnergy interface (the p4
 * cut stands; the deviation is declared on the task card).
 */
public class HeatTransmitterBlockEntity extends MultiBlockPartBlockEntity {

	/** The registry-path constructor (the shared-BET factory form). */
	public HeatTransmitterBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The test seam: offline fixtures build their own BET (the frozen-registry form). */
	public HeatTransmitterBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTMultiBlocks.HEAT_TRANSMITTER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_heat_transmitter"; // BET registry path mirrors it (GTMultiBlocks.HEAT_TRANSMITTER_BE)
	}

	/** The controller as an energy face, or null (the getTarget(T) probe of every relay body). */
	@Nullable
	private TileEntityBase01Root energyTarget() {
		ITileEntityMultiBlockController tTarget = getTarget(true); // upstream :498/:514/:530... — the VALIDITY probe
		return tTarget instanceof TileEntityBase01Root tRoot ? tRoot : null;
	}

	// ---------------------------------------------------------------------------
	// the Relay Energy section (upstream :496-595 verbatim, the aPart parameter dropped)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		if (aEmitting) { if ((mMode & NO_ENERGY_OUT) != 0) return false; } // :497
		else { if ((mMode & NO_ENERGY_IN) != 0) return false; }            // :497 else half
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null && tTarget.isEnergyType(aEnergyType, aSide, aEmitting); // :499 (→ F)
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		if ((mMode & NO_ENERGY) == NO_ENERGY) return Collections.emptyList(); // :505
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergyTypes(aSide) : Collections.emptyList(); // :507 (→ empty)
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		if ((mMode & NO_ENERGY_IN) != 0) return false; // :513
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null && tTarget.isEnergyAcceptingFrom(aEnergyType, aSide, aTheoretical); // :515 (→ F)
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		if ((mMode & NO_ENERGY_OUT) != 0) return false; // :520
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null && tTarget.isEnergyEmittingTo(aEnergyType, aSide, aTheoretical); // :522 (→ F)
	}

	@Override
	public synchronized long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if ((mMode & NO_ENERGY_IN) != 0) return 0; // :529
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.doEnergyInjection(aEnergyType, aSide, aSize, aAmount, aDoInject) : 0; // :531 (→ 0)
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		if ((mMode & NO_ENERGY_IN) != 0) return 0; // :537
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergyDemanded(aEnergyType, aSide, aSize) : 0; // :539 (→ 0)
	}

	@Override
	public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
		if ((mMode & NO_ENERGY_OUT) != 0) return 0; // :545
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.doEnergyExtraction(aEnergyType, aSide, aSize, aAmount, aDoExtract) : 0;
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		if ((mMode & NO_ENERGY_OUT) != 0) return 0;
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergyOffered(aEnergyType, aSide, aSize) : 0;
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_IN) != 0) return 0; // :561
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeInputMin(aEnergyType, aSide) : 0; // :563 (→ 0)
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_IN) != 0) return 0; // :577
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeInputRecommended(aEnergyType, aSide) : 0; // :579 (→ 0)
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_IN) != 0) return 0; // :593
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeInputMax(aEnergyType, aSide) : 0; // :595 (→ 0)
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_OUT) != 0) return 0;
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeOutputMin(aEnergyType, aSide) : 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_OUT) != 0) return 0;
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeOutputRecommended(aEnergyType, aSide) : 0;
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		if ((mMode & NO_ENERGY_OUT) != 0) return 0;
		TileEntityBase01Root tTarget = energyTarget();
		return tTarget != null ? tTarget.getEnergySizeOutputMax(aEnergyType, aSide) : 0;
	}
}
