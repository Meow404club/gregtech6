package gregtech6.tileentity.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The GT6 MOTOR converter base (task kinetics-be-function-family — the
 * TileEntityBase11Motor port, gregapi/tileentity/energy/TileEntityBase11Motor.java
 * :36-113): a converter whose rotation DIRECTION is machine state. Upstream the motor
 * carries a REVERSIBLE converter pair ({@code mConRevert}, the readEnergyConverter
 * :55-65 construction — clockwise/counterclockwise swapped by the monkey wrench
 * :80-90); the port has no monkey-wrench channel yet (the same pool crop the
 * transformer's {@code mReversed} took, declared), so this base carries the DIRECTION
 * STATE + its NBT persistence + the visual bits, and the conversion runs on the
 * forward converter only.
 *
 * <p>The steam turbine rides this base: the NBT_VISUAL fast bit and the
 * NBT_REVERSED direction bit are its upstream persistence (MultiTileEntityTurbineSteam
 * :54-58 reads both through the super chain).
 */
public abstract class GTMotorConverterBlockEntity extends GTEnergyConverterBlockEntity {

	/** The upstream NBT keys (CS.java:1241). */
	public static final String NBT_REVERSED = "gt.reversed";
	public static final String NBT_VISUAL = "gt.visual";

	/** The rotation direction (upstream :37; FALSE = clockwise, the readEnergyConverter default). */
	public boolean mCounterClockwise = false;

	/** The visual fast bit (upstream :44 — the converter mFast mirror; no consumer in this port yet). */
	public boolean mFast = false;

	protected GTMotorConverterBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	// ---------------------------------------------------------------------------
	// the direction NBT (upstream writeToNBT2 :48-51 / the readEnergyConverter reads)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_REVERSED, mCounterClockwise); // upstream :50
		aNBT.putBoolean(NBT_VISUAL, mFast); // upstream :51
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_REVERSED, Tag.TAG_ANY_NUMERIC)) mCounterClockwise = aNBT.getBoolean(NBT_REVERSED); // upstream :56
		if (aNBT.contains(NBT_VISUAL, Tag.TAG_ANY_NUMERIC)) mFast = aNBT.getBoolean(NBT_VISUAL); // upstream :44
	}
}
