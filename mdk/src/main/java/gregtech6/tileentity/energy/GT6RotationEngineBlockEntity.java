package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Kinetics;

/**
 * 1.20.1 counterpart of the GT6 Rotation Engine (task kinetics-be-function-family —
 * the mc-D 遗留 B 案面) — ported from
 * gregtech/tileentity/energy/converters/MultiTileEntityEngineRotation.java (:33-71)
 * over {@link GTBipolarConverterBlockEntity} (the TileEntityBase11Bipolar port): a
 * RU→KU converter that drives BOTH front and back with OPPOSITE rotation signs,
 * ALTERNATING which pole gets the positive half every 16 ticks (the
 * {@code aTimer % 32 < 16} cycle, upstream :36 — the oscillating engine drive).
 *
 * <p>The conversion (upstream doConversion :35-42 over the doBipolar :108-150 law, the
 * wood row numbers): {@code tOutput = units(storage, inRec = V[t], outRec = V[t]/2)} —
 * the storage half-converts; +V[t]/2-sized packets fire out one pole and −V[t]/2 out
 * the other (KU is negative-allowed, TD.java:205); the acceptance drains
 * {@code packets × tOutput}; the waste leg (the row's NBT_WASTE_ENERGY = T, Loader
 * :1667/:1676) vents the input max every tick — the funnel, not a buffer.
 *
 * <p>The 13 rows ride the mc-D data layer ({@link GT6Kinetics#ROTATION_ENGINES}): the
 * wooden row V[0]=8→4 (:1667), the metal ladder Bronze..Adamantium V[t]→V[t]/2
 * (:1676-:1764). The row values resolve from the mounted block
 * ({@link GT6Kinetics.PowertrainBlock}).
 *
 * <p>Cropped with declaration: the texture half (the sColoreds/sOverlays icon bank +
 * the mActivity state half — the port's machine-render pool), the mMode throttle
 * (constant 0, the transformer precedent), and the electricity-acceptor marker
 * interface (ITileEntityEnergyElectricityAcceptor — the electricity band is not in
 * this port's energy net).
 */
public class GT6RotationEngineBlockEntity extends GTBipolarConverterBlockEntity {

	/** The BET factory (the BlockEntityType.Builder.of shape). */
	public GT6RotationEngineBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The offline (test) entry point — a null type falls back to the shared registry type. */
	public GT6RotationEngineBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super((BlockEntityType<?>) (aType != null ? aType : GTBlockEntities.ROTATION_ENGINE_BE.get()), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "rotation_engine"; // BET registry path mirrors it (GTBlockEntities.ROTATION_ENGINE_BE)
	}

	// ---------------------------------------------------------------------------
	// the row adoption (the mc-D data layer — the mounted PowertrainBlock's row)
	// ---------------------------------------------------------------------------

	/** The row's NBT_INPUT = V[t] (the :1667/:1676 column). */
	public long inputSpeed() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.RotationEngineRow tRow) {
			return tRow.inputSpeed();
		}
		return 8; // the wooden V[0] stand-in (a BE detached from its row block)
	}

	/** The row's NBT_OUTPUT = V[t]/2 (the :1667/:1676 column). */
	public long outputSpeed() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.RotationEngineRow tRow) {
			return tRow.outputSpeed();
		}
		return 4;
	}

	// ---------------------------------------------------------------------------
	// the bands (readEnergyBehavior :73-78 over the row)
	// ---------------------------------------------------------------------------

	@Override
	public long inputSizeMin() {
		return inputSpeed() <= 16 ? 1 : inputSpeed() / 2; // readEnergyBehavior :76
	}

	@Override
	public long inputSizeRec() {
		return inputSpeed();
	}

	@Override
	public long inputSizeMax() {
		return inputSpeed() * 2;
	}

	@Override
	public long outputSizeMin() {
		return outputSpeed() / 2; // emitsAnyLowerSize = F for KU
	}

	@Override
	public long outputSizeRec() {
		return outputSpeed();
	}

	@Override
	public long outputSizeMax() {
		return outputSpeed() * 2;
	}

	@Override
	public long storageCapacity() {
		return inputSpeed() * 2; // readEnergyBehavior :75
	}

	@Override
	public boolean wasteEnergy() {
		return true; // the row column NBT_WASTE_ENERGY = T, Loader :1667/:1676
	}

	@Override
	public TagData energyTypeIn() {
		return TD.Energy.RU; // NBT_ENERGY_ACCEPTED, Loader :1667
	}

	@Override
	public TagData energyTypeOut() {
		return TD.Energy.KU; // NBT_ENERGY_EMITTED, Loader :1667
	}

	// ---------------------------------------------------------------------------
	// the oscillating conversion (MultiTileEntityEngineRotation.doConversion :35-42)
	// ---------------------------------------------------------------------------

	@Override
	protected void doConversion(long aTimer) {
		// upstream :36 — the pole assignment flips every 16 ticks (the 32-tick cycle)
		boolean tFirstHalf = aTimer % 32 < 16;
		mActive = doBipolar(aTimer, tFirstHalf ? opposite(mFacing) : mFacing,
				tFirstHalf ? mFacing : opposite(mFacing));
	}
}
