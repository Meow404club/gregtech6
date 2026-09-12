package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6ElectricDynamos;

/**
 * The Electric Dynamo (task p28-c-dynamo-family-be) — the 1.20.1 counterpart of
 * {@code MultiTileEntityDynamoElectric} (DynamoElectric.java:34-63, the Flux twin minus
 * the CoFH flux face): back face in RU, front face out EU, over the shared
 * {@link GT6DynamoBlockEntity} core. 0.6875 EU per RU, the registration constants
 * NBT_OUTPUT/NBT_INPUT = 22/32 … 5632/8192 (Loader :946-950) — the machine whose item is
 * the Flux Dynamo recipe's middle key upstream (Loader :953-957 {@code getItem(10111..10115)}),
 * ported on the wave-card ruling c.
 *
 * <h2>The emit arm (the upstream size-carrying branch, Converter:85-89, verbatim)</h2>
 *
 * EU is NOT size-irrelevant, so :85 emitted ONE packet per tick whose SIZE is the whole
 * converted amount: {@code emitEnergyToNetwork(EU, ±tOutput × mFactor, mMultiplier = 1)}.
 * The modern port keeps exactly that shape — the standard GT emit push over
 * {@link ITileEntityEnergy.Util#emitEnergyToNetwork} (the transformer emit form): one
 * packet of size {@code tOutput} (22 … 5632), gated at the core's
 * {@code tOutput >= NBT_OUTPUT/2} door, front-side only via
 * {@code isEnergyEmittingTo}; the return (used packet count) drives mActive. Waste=T: the
 * capacitor is NOT deducted on emit (the :87 arm skipped) — the input side already paid.
 *
 * <p>THE SIGN IS LIVE here (unlike the Flux family): RU ∈ ALL_NEGATIVE_ALLOWED AND
 * EU ∈ ALL_NEGATIVE_ALLOWED (TD.java:202 — both conjuncts of the Base10:121 aNegative
 * form hold), so a counterclockwise axle emits a NEGATIVE-size EU packet (mFactor = 1,
 * the Base10 readEnergyConverter aNegativeOutput = F). The packet math is sign-mirrored
 * end to end, the transformer's preserved-sign semantics on the EU side.
 *
 * <p>Output side = the standard {@code ITileEntityEnergy} EU push (the wave-card spec ③),
 * pull-based extraction stays the Root default (0) — GT energy stays on the GT grid (the
 * post-W0 posture: EU never leaves through a bridge, only through this native face).
 */
public class GT6ElectricDynamoBlockEntity extends GT6DynamoBlockEntity {

	/** The family NBT_OUTPUT column (EU): 22/88/352/1408/5632 — each exactly 0.6875× its row's NBT_INPUT (Loader :946-950). */
	public static final long[] OUTPUTS = {22, 88, 352, 1408, 5632};

	/** The family NBT_INPUT column (RU): 32/128/512/2048/8192 (Loader :946-950). */
	public static final long[] INPUTS = {32, 128, 512, 2048, 8192};

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6ElectricDynamoBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GT6ElectricDynamoBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6ElectricDynamos.ELECTRIC_DYNAMO_BE.get(), aPos, aState);
		applyTier(aState, INPUTS, OUTPUTS);
	}

	@Override
	public String getTileEntityName() {
		return "electric_dynamo"; // the T1 BET registry path mirrors it (GT6ElectricDynamos.ELECTRIC_DYNAMO_BE)
	}

	@Override
	protected TagData outputType() {
		return TD.Energy.EU; // NBT_ENERGY_EMITTED, Loader :946-950
	}

	@Override
	protected boolean negativeOutputAllowed() {
		return TD.Energy.ALL_NEGATIVE_ALLOWED.contains(TD.Energy.EU); // true — the sign is live on this family
	}

	@Override
	protected long emitConverted(long tOutput, boolean aNegative) {
		// the Converter :85 size-carrying branch: ONE packet, size = ±tOutput × mFactor(1),
		// amount = mMultiplier(1); the Util loops the sides, isEnergyEmittingTo gates FRONT-only
		long tSign = aNegative ? -1 : 1;
		return ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, tSign * tOutput, 1, this, adjacency());
	}
}
