package gregtech6.fluid;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * 1.20.1 counterpart of gregapi/fluid/FluidTankGT.java (implements 1.7.10 IFluidTank).
 * Port scope (task p4-fluid-pipes spec ①, the W3 barrel card consumes the same class):
 * <ul>
 * <li>the long internal amount ({@code mAmount}, upstream :41) behind the int IFluidTank
 *     surface — every int crossing clamps through {@link #bindInt} (upstream UT.Code.bindInt,
 *     UT.java:1565);</li>
 * <li>NBT persistence (upstream :59-93): the FluidStack compound carries the amount
 *     bound to int, and when the true amount exceeds the int range the exact 63-bit value
 *     rides along in the {@code "LAmount"} long key — read back verbatim (:64, "hasKey
 *     LAmount ? getLong : mFluid.amount"), which keeps GT6 save files legible both ways;</li>
 * <li>{@code mPreventDraining} (never drop the last 0 L, keeps the fluid identity) and
 *     {@code mVoidExcess} (overflow voids, upstream :42/:121-126/:162/:190/:198);</li>
 * <li>fill/drain with {@link FluidAction#SIMULATE} — the boolean aDoFill/aDoDrain of the
 *     1.7.10 signatures maps onto the 1.20.1 FluidAction enum (IFluidHandler.java:21-31);
 *     the simulate return path is upstream :200 verbatim (bindInt of the same min arithmetic);</li>
 * <li>the single-fluid rule: filling a tank that holds a different fluid is a no-op
 *     (upstream :192 contains-gate), and the long-add/remove pair ({@link #add(long, FluidStack)}
 *     / {@link #remove(long)}, upstream :144-179) is the primitive the pipe distribute loop
 *     (MultiTileEntityPipeFluid.java:417/:421/:428) is written against.</li>
 * </ul>
 *
 * <p>Omissions (not needed by the W1 consumers, back on demand): the adjustable-capacity
 * map (:44-45/:303-343), fillAll/canFillAll (:203-258), the changed-fluids dirty flag
 * (:42/:326-328) and the FL-backed display helpers (:345-350). {@link #getFluid()} keeps
 * the upstream contract of returning the live stack with the amount rebound (upstream :359,
 * also how vanilla FluidTank behaves despite the 1.20.1 @NotNull annotation).
 */
public class FluidTankGT implements IFluidTank {

	/** Upstream :38 — the single-tank array form the pipe code iterates. */
	public final FluidTankGT[] AS_ARRAY = new FluidTankGT[] {this};

	/** The NBT overflow key for amounts beyond the int range (upstream :75/:88). */
	public static final String NBT_L_AMOUNT = "LAmount";

	@Nullable
	private FluidStack mFluid;
	private long mCapacity = 0, mAmount = 0;
	private boolean mPreventDraining = false, mVoidExcess = false;
	/** Gives you a Tank Index in case there is multiple Tanks on a TileEntity that cares (upstream :47). */
	public int mIndex = 0;

	public FluidTankGT() {mCapacity = Long.MAX_VALUE;}
	public FluidTankGT(long aCapacity) {mCapacity = aCapacity;}

	/**
	 * Upstream :56 constructor — reads the compound the {@link #writeToNBT} form produced.
	 * An absent/empty compound leaves an empty tank; a compound without the overflow key
	 * falls back to the int amount (:56).
	 */
	public FluidTankGT(@Nullable CompoundTag aNBT, long aCapacity) {
		mCapacity = aCapacity;
		if (aNBT != null && !aNBT.isEmpty()) {
			mFluid = FluidStack.loadFluidStackFromNBT(aNBT);
			mAmount = (isEmpty() ? 0 : aNBT.contains(NBT_L_AMOUNT, Tag.TAG_ANY_NUMERIC) ? aNBT.getLong(NBT_L_AMOUNT) : mFluid.getAmount());
		}
	}

	/** Upstream :59-68. */
	public FluidTankGT readFromNBT(CompoundTag aNBT, String aKey) {
		if (aNBT.contains(aKey, Tag.TAG_COMPOUND)) {
			CompoundTag tNBT = aNBT.getCompound(aKey);
			if (!tNBT.isEmpty()) {
				mFluid = FluidStack.loadFluidStackFromNBT(tNBT);
				mAmount = (isEmpty() ? 0 : tNBT.contains(NBT_L_AMOUNT, Tag.TAG_ANY_NUMERIC) ? tNBT.getLong(NBT_L_AMOUNT) : mFluid.getAmount());
			}
		}
		return this;
	}

	/** Upstream :70-80 — binds the int amount into the FluidStack compound, overflows into "LAmount". */
	public CompoundTag writeToNBT(CompoundTag aNBT, String aKey) {
		if (mFluid != null && (mPreventDraining || mAmount > 0)) {
			CompoundTag tNBT = new CompoundTag();
			mFluid.setAmount(bindInt(mAmount)); // upstream :73 mutates the stack amount in place
			aNBT.put(aKey, mFluid.writeToNBT(tNBT));
			if (mAmount > Integer.MAX_VALUE) tNBT.putLong(NBT_L_AMOUNT, mAmount);
		} else {
			aNBT.remove(aKey);
		}
		return aNBT;
	}

	// ---------------------------------------------------------------------------
	// the long-add/remove primitives the distribute loop consumes (upstream :144-179)
	// ---------------------------------------------------------------------------

	/** Upstream :144-156 — removes up to aDrained, keeps the fluid identity under mPreventDraining. */
	public long remove(long aDrained) {
		if (isEmpty() || mAmount <= 0 || aDrained <= 0) return 0;
		if (mAmount < aDrained) aDrained = mAmount;
		mAmount -= aDrained;
		if (mAmount <= 0) {
			if (mPreventDraining) {
				mAmount = 0;
			} else {
				setEmpty();
			}
		}
		return aDrained;
	}

	/** Upstream :158-168 — long add on an existing content; overflow voids only under mVoidExcess. */
	public long add(long aFilled) {
		if (isEmpty() || aFilled <= 0) return 0;
		long tCapacity = capacity();
		if (mAmount + aFilled > tCapacity) {
			if (!mVoidExcess) aFilled = tCapacity - mAmount;
			mAmount = tCapacity;
			return aFilled;
		}
		mAmount += aFilled;
		return aFilled;
	}

	/** Upstream :170-179 — long add accepting the fluid too; an empty tank adopts a copy of it. */
	public long add(long aFilled, @Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty() || aFilled <= 0) return 0;
		if (isEmpty()) {
			mFluid = aFluid.copy();
			mAmount = Math.min(capacity(aFluid), aFilled);
			return mVoidExcess ? aFilled : mAmount;
		}
		return contains(aFluid) ? add(aFilled) : 0;
	}

	// ---------------------------------------------------------------------------
	// IFluidTank (1.20.1 interface, IFluidTank.java:16-63) — FluidAction replaces the booleans
	// ---------------------------------------------------------------------------

	@Override
	@Nullable
	public FluidStack getFluid() {
		if (mFluid != null) mFluid.setAmount(bindInt(mAmount)); // upstream :359 rebinds the amount
		return mFluid;
	}

	@Override
	public int getFluidAmount() {
		return bindInt(mAmount);
	}

	@Override
	public int getCapacity() {
		return bindInt(capacity());
	}

	@Override
	public boolean isFluidValid(@Nullable FluidStack aStack) {
		return aStack != null && !aStack.isEmpty() && (isEmpty() || contains(aStack));
	}

	/** Upstream :183-201 with {@code aDoFill} → {@link FluidAction}. */
	@Override
	public int fill(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (aFluid == null || aFluid.isEmpty()) return 0;
		if (aAction.execute()) {
			if (isEmpty()) {
				mFluid = aFluid.copy();
				mAmount = Math.min(capacity(aFluid), aFluid.getAmount());
				return mVoidExcess ? aFluid.getAmount() : (int)mAmount;
			}
			if (!contains(aFluid)) return 0;
			long tCapacity = capacity(aFluid), tFilled = tCapacity - mAmount;
			if (aFluid.getAmount() < tFilled) {
				mAmount += aFluid.getAmount();
				tFilled = aFluid.getAmount();
			} else mAmount = tCapacity;
			return mVoidExcess ? aFluid.getAmount() : (int)tFilled;
		}
		return bindInt(isEmpty()
			? mVoidExcess ? aFluid.getAmount() : Math.min(capacity(aFluid), aFluid.getAmount())
			: contains(aFluid) ? mVoidExcess ? aFluid.getAmount() : Math.min(capacity(aFluid) - mAmount, aFluid.getAmount())
			: 0);
	}

	/** Upstream :114-129 with {@code aDoDrain} → {@link FluidAction}; empty drains give FluidStack.EMPTY (1.20.1 @NotNull surface). */
	@Override
	public FluidStack drain(int aDrained, FluidAction aAction) {
		if (isEmpty() || aDrained <= 0) return FluidStack.EMPTY;
		if (mAmount < aDrained) aDrained = (int)mAmount;
		FluidStack rFluid = new FluidStack(mFluid, aDrained);
		if (aAction.execute()) {
			mAmount -= aDrained;
			if (mAmount <= 0) {
				if (mPreventDraining) {
					mAmount = 0;
				} else {
					setEmpty();
				}
			}
		}
		return rFluid;
	}

	@Override
	public FluidStack drain(@Nullable FluidStack aFluid, FluidAction aAction) {
		if (aFluid == null || aFluid.isEmpty() || !contains(aFluid)) return FluidStack.EMPTY;
		return drain(bindInt(Math.min(mAmount, aFluid.getAmount())), aAction);
	}

	// ---------------------------------------------------------------------------
	// state helpers (upstream :261-362, the members the W1/W3 consumers touch)
	// ---------------------------------------------------------------------------

	/** Upstream :261-266. */
	public FluidTankGT setEmpty() {
		mFluid = null;
		mAmount = 0;
		return this;
	}

	/** Upstream :292. */
	public FluidTankGT setIndex(int aIndex) {
		mIndex = aIndex;
		return this;
	}

	/** Upstream :294 — accepts 63 bit capacities. */
	public FluidTankGT setCapacity(long aCapacity) {
		if (aCapacity >= 0) mCapacity = aCapacity;
		return this;
	}

	/** Upstream :296-298 — always keeps at least 0 Liters of Fluid instead of setting it to null. */
	public FluidTankGT setPreventDraining(boolean aPrevent) {
		mPreventDraining = aPrevent;
		return this;
	}

	/** Upstream :300-302 — voids any overflow. */
	public FluidTankGT setVoidExcess(boolean aVoidExcess) {
		mVoidExcess = aVoidExcess;
		return this;
	}

	/** Upstream :314 — null content is the empty state (upstream keeps the stack, not just the amount). */
	public boolean isEmpty() {
		return mFluid == null || mFluid.isEmpty();
	}

	public boolean isFull() {return mAmount >= capacity();}
	public boolean has(long aAmount) {return mAmount >= aAmount;}
	public boolean has() {return mAmount > 0;}

	/** Upstream :321 — amount-independent fluid equality (FluidStack.isFluidEqual :260). */
	public boolean contains(@Nullable FluidStack aFluid) {
		return mFluid != null && !mFluid.isEmpty() && aFluid != null && !aFluid.isEmpty() && mFluid.isFluidEqual(aFluid);
	}

	/** Upstream :330-331. */
	public long amount() {
		return isEmpty() ? 0 : mAmount;
	}

	/** Upstream :331. */
	public long amount(long aMax) {
		return isEmpty() || aMax <= 0 ? 0 : Math.min(mAmount, aMax);
	}

	/** Upstream :333-334 — the W1 tank has no adjustable-capacity map. */
	public long capacity() {
		return mCapacity;
	}

	public long capacity(@Nullable FluidStack aFluid) {
		return mCapacity;
	}

	/** Upstream :352. */
	@Nullable
	public FluidStack fluid() {
		return isEmpty() ? null : mFluid;
	}

	/** Upstream :356-357 — the live stack, or a bounded copy. */
	@Nullable
	public FluidStack get() {
		return mFluid;
	}

	/** Upstream :357. */
	@Nullable
	public FluidStack get(long aMax) {
		return isEmpty() || aMax <= 0 ? null : new FluidStack(mFluid, bindInt(Math.min(mAmount, aMax)));
	}

	/** Upstream UT.Code.bindInt (UT.java:1565) — the long→int boundary clamp. */
	public static int bindInt(long aBoundValue) {
		return (int)Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));
	}
}
