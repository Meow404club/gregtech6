package gregtech6.tileentity.attachment;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of gregapi/tileentity/base/TileEntityBase11AttachmentSmall.java
 * (:29-31, task p12-tap-funnel-attachment spec ①) — the small wall-attachment BE base
 * the Fluid Tap and the Fluid Funnel share. The upstream chain
 * {@code 11AttachmentSmall → 10Attachment → 09FacingSingle} collapses onto the in-repo
 * {@link TileEntityBase03TicksAndSync} with the three faces the two fluid families
 * actually carry:
 *
 * <ul>
 * <li>{@link #mFacing} — the side the attachment is mounted ON (the 09FacingSingle
 *     single-facing field); the interaction target is
 *     {@code getAdjacentTileEntity(mFacing)} (MultiTileEntityFluidTap.java:79 /
 *     MultiTileEntityFluidFunnel.java:74), so the attachment looks AT its host —
 *     the 1.20.1 carrier is the blockstate {@code FACING} written at placement
 *     (clicked face) and re-synced into this mirror each tick like the crank;</li>
 * <li>{@code ignorePlayerCollisionWhenPlacing} (:29-30) — FREE in 1.20.1: the vanilla
 *     {@code BlockItem.place → canPlace} path checks {@code state.canSurvive} only and
 *     never intersects the player AABB (no upstream-style placement obstruction
 *     machinery exists to port); declared in the block's javadoc;</li>
 * <li>{@link #mAcidProof} — the ONE registration flag the two families consume
 *     (Tap/Funnel {@code readFromNBT2}, MultiTileEntityFluidTap.java:64-67 /
 *     MultiTileEntityFluidFunnel.java:55-58): a non-acid-proof attachment refuses to
 *     transfer fluids on the acid list. The 1.20.1 home is the block carrier (the W1
 *     registration-NBT-carrier pattern — the row's {@code NBT_ACIDPROOF} rides
 *     {@code GTAttachmentSmallBlock#acidProof()}), re-read here through the block so
 *     {@code /setblock} rows behave like placed items. The row's {@code NBT_MAGICPROOF}
 *     has no BE consumer upstream either (dead registration data, the gasproof-quartet
 *     census q5④) and is NOT carried.</li>
 * </ul>
 *
 * <p>The upstream {@code TileEntityBase10Attachment} cover/obstruction surface
 * (TileEntityBase10Attachment.java:35-51) is the feature-layer omission it already is
 * in this port (covers are a barrel-family face); the render-pass box system
 * (11:178+ per-family {@code setBlockBounds2}) is the placeholder-shape deviation the
 * block class declares.
 *
 * <p>Fluid category lists (task card spec ⑥ — the acid refusal is a NAME-LIST lookup,
 * not a BE flag): the upstream {@code FL.acid} / {@code FL.gas} verdicts are
 * {@code FluidsGT.ACID / .GAS} set membership (FL.java:756 / :768); the port keeps the
 * SAME shape as the static {@link Categories} lists over 1.20.1 fluid registry keys,
 * declared minimal (no acid/gas fluid is registered yet — the verdicts are live code
 * with empty default lists, the mechanism the machine cards consume).
 */
public abstract class GTAttachmentSmallBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream CS NBT_ACIDPROOF ("gt.acidproof") — read on load like the Tap/Funnel readFromNBT2 pair. */
	public static final String NBT_ACIDPROOF = "acidproof";

	/** Upstream 09FacingSingle mFacing — the side the attachment is mounted on (GT6 side order == Direction.get3DDataValue). */
	public byte mFacing = (byte)Direction.NORTH.get3DDataValue();

	/**
	 * Offline test seam (the crank adjacency precedent): when non-null, {@link #adjacent()}
	 * returns it instead of the live neighbour — the tap/funnel chains stay drivable
	 * without a Level.
	 */
	@Nullable
	protected BlockEntity mAdjacentOverride = null;

	protected GTAttachmentSmallBlockEntity(boolean aIsTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aIsTicking, aType, aPos, aState);
	}

	/**
	 * {@code getAdjacentTileEntity(mFacing)} — the interaction target: the BlockEntity in
	 * the facing direction, with the side of it that faces the attachment (the upstream
	 * DelegatorTileEntity pair, mTileEntity + mSideOfTileEntity). Returns null when the
	 * neighbour has no BE (the delegator's null form).
	 */
	@Nullable
	public BlockEntity adjacent() {
		if (mAdjacentOverride != null) return mAdjacentOverride;
		if (!hasLevel()) return null;
		return getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(mFacing)));
	}

	/**
	 * The side of the interaction target that faces the attachment (the delegator
	 * {@code mSideOfTileEntity} — always the attachment facing's opposite).
	 */
	public static byte sideFacingBack(byte aFacing) {
		return (byte)Direction.from3DDataValue(aFacing).getOpposite().get3DDataValue();
	}

	/** The block-carried acid-proof flag (the W1 registration-NBT-carrier pattern). */
	public boolean acidProof() {
		BlockState tState = getBlockState();
		return tState.getBlock() instanceof gregtech6.block.attachment.GTAttachmentSmallBlock tBlock && tBlock.acidProof();
	}

	// ---------------------------------------------------------------------------
	// the shared acid/gas/xp/mob verdicts (the FL.java:756/:768 name-list semantics)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream {@code FL.acid(aFluid)} = {@code FluidsGT.ACID.contains(aFluid.getFluid().getName())}
	 * (FL.java:756). The 1.20.1 name home is the fluid registry key. The list is declared
	 * minimal — no ported fluid sits on it yet (the upstream list is populated by the
	 * material ACID property sweep, FL.java:1118, a dataset bridge the port has not built).
	 */
	public static boolean isAcid(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && Categories.ACIDS.contains(fluidKey(aFluid));
	}

	/**
	 * Upstream {@code FL.gas(aFluid, F)} = {@code !LIQUID.contains && (isGaseous || GAS.contains)}
	 * (FL.java:768). The 1.20.1 gaseous verdict is the FluidType density sign (the p5
	 * lighter ruling — density {@code < 0} is exactly the STATE_GASEOUS carrier the port
	 * registers, GTFluids density −100); the GAS list membership half keeps the upstream
	 * name-list escape hatch. The LIQUID whitelist half is cut with the list machinery
	 * (declared deviation: an explicitly-listed liquid would be misjudged gaseous, but the
	 * port registers no such conflict).
	 */
	public static boolean isGas(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return false;
		return isGasVerdict(gasDensitySign(aFluid), fluidKey(aFluid));
	}

	/**
	 * The offline seam over the FluidType density (the p5 {@code (byte, int)} overload
	 * precedent): the live lookup resolves ForgeMod.WATER_TYPE through a RegistryObject
	 * the offline boot never constructs, so an unavailable lookup reads as sign 0 (a
	 * neither-lighter fluid — the name-list half still answers); the live registry never
	 * throws.
	 */
	static int gasDensitySign(FluidStack aFluid) {
		try {
			return aFluid.getFluid().getFluidType().getDensity() < 0 ? -1 : 1;
		} catch (Throwable t) {
			return 0;
		}
	}

	/** The verdict form of {@link #isGas}: strictly lighter than air, or on the GAS list (FL.java:768). */
	public static boolean isGasVerdict(int aDensitySign, String aKey) {
		if (aDensitySign < 0) return true;
		return Categories.GASES.contains(aKey);
	}

	/** The upstream XP-fluid verdict ({@code FL.XP.is}, MultiTileEntityFluidTap.java:132) — name-list form, declared minimal. */
	public static boolean isXpFluid(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && Categories.XP_FLUIDS.contains(fluidKey(aFluid));
	}

	/** The upstream mob-essence verdict ({@code FL.Mob.is}, MultiTileEntityFluidTap.java:153) — name-list form, declared minimal. */
	public static boolean isMobFluid(@Nullable FluidStack aFluid) {
		return aFluid != null && !aFluid.isEmpty() && Categories.MOB_FLUIDS.contains(fluidKey(aFluid));
	}

	/**
	 * The 1.20.1 counterpart of {@code aFluid.getFluid().getName()} — the fluid's registry
	 * key string. The lookup goes through the VANILLA registry handle: the
	 * {@code ForgeRegistries} wrappers are lazy Forge-side bridges that do not exist in
	 * the offline boot (the "Registry Object not present" trap), while
	 * {@code BuiltInRegistries.FLUID} is populated by the vanilla bootstrap.
	 */
	public static String fluidKey(FluidStack aFluid) {
		Fluid tFluid = aFluid.getFluid();
		return String.valueOf(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tFluid));
	}

	/** The category lists behind the verdicts above — the FluidsGT set counterparts, test-injectable. */
	public static final class Categories {
		/** Upstream {@code FluidsGT.ACID} (FL.java:756) — declared minimal, no ported fluid on it. */
		public static final java.util.Set<String> ACIDS = new java.util.HashSet<>();
		/** Upstream {@code FluidsGT.GAS} (FL.java:768) — the escape hatch beside the density verdict. */
		public static final java.util.Set<String> GASES = new java.util.HashSet<>();
		/** Upstream {@code FL.XP} — declared minimal (OpenBlocks liquid XP is not ported). */
		public static final java.util.Set<String> XP_FLUIDS = new java.util.HashSet<>();
		/** Upstream {@code FL.Mob} — declared minimal. */
		public static final java.util.Set<String> MOB_FLUIDS = new java.util.HashSet<>();

		private Categories() {}
	}

	// ---------------------------------------------------------------------------
	// NBT (the Tap/Funnel readFromNBT2 :64-67 pair, hoisted to the shared base)
	// ---------------------------------------------------------------------------

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ACIDPROOF, Tag.TAG_ANY_NUMERIC)) {
			// the NBT row wins when present (the upstream readFromNBT2 :66 form); otherwise
			// the block carrier answers — the /setblock path has no row and stays block-driven
			mAcidProofNbt = aNBT.getBoolean(NBT_ACIDPROOF);
			mAcidProofFromNbt = true;
		}
	}

	/** The NBT override when the save carried the row; null = answer from the block carrier (package-visible: the offline seam). */
	@Nullable
	Boolean mAcidProofFromNbt = null;
	boolean mAcidProofNbt = false;

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mAcidProofFromNbt != null) aNBT.putBoolean(NBT_ACIDPROOF, mAcidProofNbt);
	}

	/** The effective acid-proof verdict: the NBT row when carried, else the block carrier (the registration row). */
	public boolean isAcidProof() {
		return mAcidProofFromNbt != null ? mAcidProofNbt : acidProof();
	}

	// ---------------------------------------------------------------------------
	// the activation entry (the upstream onBlockActivated3 :77/:68 server branch)
	// ---------------------------------------------------------------------------

	/**
	 * The attachment activation (the tap/funnel chain). {@code aHeld} null/empty = the
	 * empty hand (tap) / the no-op (funnel); {@code aPlayer} null = the RCON acceptance
	 * channel. Returns the action report (the player path ignores it).
	 */
	public abstract String activate(@Nullable net.minecraft.world.entity.player.Player aPlayer, byte aSide,
			@Nullable net.minecraft.world.item.ItemStack aHeld);

	/** The live block-use entry: the player's MAIN HAND (the upstream getCurrentEquippedItem). */
	public void onPlayerUse(net.minecraft.world.entity.player.Player aPlayer, byte aSide) {
		activate(aPlayer, aSide, aPlayer.getMainHandItem());
	}

	/**
	 * The held-container resolution seam ({@code FluidUtil.getFluidHandler}, FluidUtil
	 * .java:431) — overridable so the offline tests can inject a recording handler
	 * without the capability dispatch (the CoverItemInterceptTest ruling: the
	 * FLUID_HANDLER_ITEM dispatch itself is the RCON gate).
	 */
	@Nullable
	protected net.minecraftforge.fluids.capability.IFluidHandlerItem heldItemHandler(net.minecraft.world.item.ItemStack aHeld) {
		return net.minecraftforge.fluids.FluidUtil.getFluidHandler(aHeld).orElse(null);
	}
}
