package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.energy.IEnergyStorage;
//?} else {
/*import net.neoforged.neoforge.energy.IEnergyStorage;
 *///?}

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyBridge;
import gregtech6.registry.GT6FluxDynamos;

/**
 * The Flux Dynamo (task p28-c-dynamo-family-be) — the 1.20.1 counterpart of
 * {@code MultiTileEntityDynamoFlux} (DynamoFlux.java:35-64): back face in RU, front face
 * out RF, over the shared {@link GT6DynamoBlockEntity} core. 2.75 FE per RU, expressed the
 * W0-ruled way: the ratio lives in the REGISTRATION CONSTANTS (NBT_OUTPUT/NBT_INPUT =
 * 88/32 … 22528/8192, exactly 2.75), the BE computes the packet sizes from them and drives
 * the ratio-agnostic root seam {@link EnergyBridge#pushPacketTrain} — the same whole-packet
 * train the root tests pin (EnergyBridgeTest:98+), no ratio math of its own
 * (decisions.p28-arch-flux-dynamo-wave ruling b: the Dynamo must NOT fork the packet
 * math into the mdk).
 *
 * <h2>The emit arm (the upstream size-irrelevant branch, Converter:78-83, ported to the
 * whole-packet seam)</h2>
 *
 * Upstream RF ∈ ALL_SIZE_IRRELEVANT, so :79 emitted the converted AMOUNT {@code tOutput}
 * at size 1 into the CoFH net. The modern FE side has no amount-flood face — the W0 seam
 * is whole-packet trains. So: {@code tOutput = units(storage, in, out, F)} (the core),
 * {@code packetCount = tOutput / NBT_OUTPUT} (whole NBT_OUTPUT-FE packets only;
 * {@code floor(floor(s·out/in)/out) ≡ floor(s/in)}, the identity the ratio-table test
 * pins), then ONE {@code pushPacketTrain(receiver, NBT_OUTPUT, packetCount)} per tick.
 * The sub-packet remainder (tOutput % NBT_OUTPUT) is NOT accumulated (the wave-card wall:
 * never invent accumulation) — the :92 vent burns it with the rest of the bucket; on the
 * aligned steady-state diet (whole inRec RU packets) the remainder is zero and the
 * delivered FE is the EXACT 2.75× of the RU input.
 *
 * <p>PUSH-ONLY by faithfulness: upstream never overrode doExtract, so its CoFH pull face
 * answered 0 (research.p28-r-flux-dynamo runtime_semantics.rf_outbound) — this BE exposes
 * NO FE capability at all (no intake, no extract face); the FE leaves only through the
 * tick push. The receiver resolves FRONT-side only (isEnergyEmittingTo gates the GT util
 * for the EU family; the FE push has no GT util, so the arm resolves mFacing itself).
 *
 * <p>The negative flag folds to false: RF ∉ ALL_NEGATIVE_ALLOWED (TD.java:202), so the
 * Base10:121 {@code aNegative} conjunction dies on the output conjunct regardless of the
 * axle's spin direction — signed RU in, unsigned FE out (pushPacketTrain bridges a packet
 * size by its magnitude anyway, EnergyBridge :124).
 */
public class GT6FluxDynamoBlockEntity extends GT6DynamoBlockEntity {

	/** The family NBT_OUTPUT column (FE): 88/352/1408/5632/22528 — each exactly 2.75× its row's NBT_INPUT (Loader :953-957). */
	public static final long[] OUTPUTS = {88, 352, 1408, 5632, 22528};

	/** The family NBT_INPUT column (RU): 32/128/512/2048/8192 (Loader :953-957). */
	public static final long[] INPUTS = {32, 128, 512, 2048, 8192};

	/**
	 * The offline test seam: when set, replaces the live front-side FE resolution of
	 * {@link #emitConverted} (the GT6FeConverterBlockEntity.setPullSourceOverride form).
	 */
	private EnergyBridge.IFEReceiver mPushTargetOverride = null;

	void setPushTargetOverride(@Nullable EnergyBridge.IFEReceiver aTarget) {
		mPushTargetOverride = aTarget;
	}

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6FluxDynamoBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GT6FluxDynamoBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6FluxDynamos.FLUX_DYNAMO_BE.get(), aPos, aState);
		applyTier(aState, INPUTS, OUTPUTS);
	}

	@Override
	public String getTileEntityName() {
		return "flux_dynamo"; // the T1 BET registry path mirrors it (GT6FluxDynamos.FLUX_DYNAMO_BE)
	}

	@Override
	protected TagData outputType() {
		return TD.Energy.RF; // NBT_ENERGY_EMITTED, Loader :953-957
	}

	@Override
	protected boolean negativeOutputAllowed() {
		return TD.Energy.ALL_NEGATIVE_ALLOWED.contains(TD.Energy.RF); // false — the class-doc fold
	}

	@Override
	protected long emitConverted(long tOutput, boolean aNegative) {
		EnergyBridge.IFEReceiver tTarget = mPushTargetOverride != null ? mPushTargetOverride : resolveFeReceiver(mFacing);
		if (tTarget == null) return 0;
		long tPackets = tOutput / mOutput; // whole NBT_OUTPUT-FE packets — the sub-packet tail burns with the vent
		if (tPackets <= 0) return 0;
		return EnergyBridge.pushPacketTrain(tTarget, mOutput, tPackets); // the accepted packet count
	}

	/**
	 * The live front-side query — per-leg hunks (the GT6FeConverterBlockEntity.resolveFeSource
	 * shape mirrored for receive): null unless the neighbor exposes an FE storage that can
	 * receive. The side is mFacing itself (the FRONT = the output face).
	 */
	@Nullable
	private EnergyBridge.IFEReceiver resolveFeReceiver(byte aSide) {
		if (!hasLevel()) return null;
		Direction tDirection = Direction.from3DDataValue(aSide);
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		//? if forge {
		IEnergyStorage tStorage = tNeighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, tDirection.getOpposite()).orElse(null);
		//?} else {
		/*IEnergyStorage tStorage = getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
				getBlockPos().relative(tDirection), tDirection.getOpposite());
		 *///?}
		if (tStorage == null || !tStorage.canReceive()) return null;
		return tStorage::receiveEnergy; // the two-argument lambda shape (EnergyBridge.IFEReceiver)
	}
}
