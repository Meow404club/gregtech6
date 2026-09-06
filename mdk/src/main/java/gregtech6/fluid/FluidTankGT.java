package gregtech6.fluid;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

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
			//? if forge {
			mFluid = FluidStack.loadFluidStackFromNBT(aNBT);
			//?} else {
			/*mFluid = FluidStack.parseOptional(nbtAccess(), aNBT); // 21.1: the codec parse face (javap 21.1.249); a failed parse lands EMPTY, folded to 0 below
			*///?}
			mAmount = (isEmpty() ? 0 : aNBT.contains(NBT_L_AMOUNT, Tag.TAG_ANY_NUMERIC) ? aNBT.getLong(NBT_L_AMOUNT) : mFluid.getAmount());
		}
	}

	/** Upstream :59-68. */
	public FluidTankGT readFromNBT(CompoundTag aNBT, String aKey) {
		if (aNBT.contains(aKey, Tag.TAG_COMPOUND)) {
			CompoundTag tNBT = aNBT.getCompound(aKey);
			if (!tNBT.isEmpty()) {
				//? if forge {
				mFluid = FluidStack.loadFluidStackFromNBT(tNBT);
				if (mFluid != null && mFluid.getRawFluid() == Fluids.EMPTY) {
					mFluid = null; // a legacy degraded "minecraft:empty" payload (pre-fix save) or a bare key: a truly empty tank
					mAmount = 0;
				} else if (mFluid != null && mFluid.isEmpty()) {
					// The keepFilter payload (Amount 0 with a REAL FluidName — what writeToNBT now
					// writes): loadFluidStackFromNBT empty-flagged the 0-amount stack, which would
					// collapse the kept identity to Fluids.EMPTY. Upstream :56 loads the REAL fluid
					// at amount 0 — the 1.20.1 carrier rebuilds it un-collapsed at a unit amount
					// (mAmount stays the authoritative 0; task p12-barrel-keepfilter-logistics).
					mFluid = new FluidStack(mFluid.getRawFluid(), 1);
					mAmount = 0;
				} else {
					mAmount = (isEmpty() ? 0 : tNBT.contains(NBT_L_AMOUNT, Tag.TAG_ANY_NUMERIC) ? tNBT.getLong(NBT_L_AMOUNT) : mFluid.getAmount());
				}
				//?} else {
				/*// 21.1 leg: parseOptional rides the codec face; a degraded payload lands EMPTY.
				//keepFilter READ-SIDE REBUILD (ADR-P18, task p18-keepfilter-2111-readback — replaces
				//the former KNOWN 21.1 DELTA of p15-prod-fix-fluidstack-save): the 0-amount keepFilter
				//payload {FluidName: REAL, Amount: 0} carries no codec keys, and the codec amount is
				//POSITIVE_INT (NeoForge FluidStack MAP_CODEC :65-73/:70 — 0 is unrepresentable), so
				//parseOptional fails it to EMPTY (parse(...).orElse(EMPTY), never throws). Before
				//folding to a null tank the identity is read back from the "FluidName" key and
				//re-queried against BuiltInRegistries.FLUID, rebuilding the forge leg's :87-94 state
				//verbatim: a unit-amount carrier with mAmount staying the authoritative 0 (upstream
				//FL.load_ :1035-1045 reads the same key unfiltered). The lookup is guarded twice —
				//tryParse nulls an illegal name, Registry.get nulls an unknown one, and Fluids.EMPTY
				//is rejected — so corrupt NBT degrades to an empty tank exactly like a codec
				//failure, never a throw. NON-ZERO roundtrips stay lossless via the save() return
				//tag (p15-prod-fix-fluidstack-save). Residual deltas: a 0-amount payload persists
				//neither components nor tag (both legs' pool debt), and a fluid removed from the
				//registry since the save still folds to an empty tank.
				mFluid = FluidStack.parseOptional(nbtAccess(), tNBT);
				if (mFluid.getFluid() == Fluids.EMPTY) {
					net.minecraft.world.level.material.Fluid tFluid = null;
					if (tNBT.contains("FluidName", Tag.TAG_STRING)) {
						net.minecraft.resources.ResourceLocation tName = net.minecraft.resources.ResourceLocation.tryParse(tNBT.getString("FluidName"));
						tFluid = tName == null ? null : net.minecraft.core.registries.BuiltInRegistries.FLUID.get(tName);
					}
					if (tFluid != null && tFluid != Fluids.EMPTY) {
						mFluid = new FluidStack(tFluid, 1); // the unit-amount carrier — mAmount stays the authoritative 0
						mAmount = 0;
					} else {
						mFluid = null; // a bare key, an illegal/unknown FluidName, or a payload without one: a truly empty tank
						mAmount = 0;
					}
				} else {
					mAmount = (isEmpty() ? 0 : tNBT.contains(NBT_L_AMOUNT, Tag.TAG_ANY_NUMERIC) ? tNBT.getLong(NBT_L_AMOUNT) : mFluid.getAmount());
				}
				*///?}
			}
		}
		return this;
	}

	/** Upstream :70-80 — binds the int amount into the FluidStack compound, overflows into "LAmount". */
	public CompoundTag writeToNBT(CompoundTag aNBT, String aKey) {
		if (mFluid != null && (mPreventDraining || mAmount > 0)) {
			CompoundTag tNBT = new CompoundTag();
			// upstream :73 rebinds the stack amount IN PLACE; the 1.20.1 carrier mutates a COPY —
			// setAmount(0) empty-flags the mutated stack, and the save/sync path (updateClientData
			// → saveAdditional) would otherwise collapse the LIVE kept-filter stack in memory
			// (live-proven: the show line degraded to "minecraft:empty" right after a draw to 0 L;
			// task p12-barrel-keepfilter-logistics). mFluid.amount is a derived cache either way —
			// getFluid() rebinds it on every read.
			FluidStack tCopy = mFluid.copy();
			tCopy.setAmount(bindInt(mAmount));
			if (tCopy.isEmpty()) {
				// The 1.20.1 empty-flag artifact (the keepFilter persistence gap this card fixes):
				// a 0-amount stack serializes through {@code FluidStack.writeToNBT} as
				// {@code FluidName: "minecraft:empty"} ({@code getFluid()} collapses to
				// Fluids.EMPTY once empty-flagged), while upstream :70-80/:84-88 keeps the REAL
				// identity at 0 L ({@code mFluid.getFluid()} stays the filter fluid). The raw
				// fluid still names the identity, so the judgement's promise (writeToNBT
				// 0 量含身份) is honoured by writing the registry name from it.
				// 21.1: the live mFluid carries its own non-zero amount cache in this state, so
				// getFluid() reads the REAL identity there too (the collapse only hits tCopy).
				//? if forge {
				tNBT.putString("FluidName", ForgeRegistries.FLUIDS.getKey(tCopy.getRawFluid()).toString());
				//?} else {
				/*tNBT.putString("FluidName", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(mFluid.getFluid()).toString());
				*///?}
				tNBT.putInt("Amount", 0);
			} else {
				//? if forge {
				tCopy.writeToNBT(tNBT);
				//?} else {
				/*// 21.1: the codec save face (writeToNBT deleted). save(Provider, Tag) RETURNS a fresh
				//tag and never writes the passed one (javap 21.1.249: wrapEncodingExceptions →
				//CODEC.encode(..., prefix).getOrThrow() — the probe target stayed {}), so the return
				//value must be kept: discarding it wrote a permanently empty {tank:{}} and every
				//roundtrip read folded to 0 (p15-prod-fix-fluidstack-save). The cast is safe — the
				//fluid record codec only ever emits a CompoundTag ({"id", "amount"}), and the
				//LAmount overflow below needs the CompoundTag surface anyway. The passed tag stays
				//as the DFU prefix argument; the codec emits its own {"id","amount"} shape.
				tNBT = (CompoundTag)tCopy.save(nbtAccess(), tNBT);
				// Upstream :73 byte-compat (ADR-P15-1): the GT6 contract keys must stay present next
				// to the codec's own face — "Amount" (the int-bound value) and "FluidName" (the
				// identity) are what the drop-item/item-handler payloads and legacy GT6 readers
				// name; the read side (parseOptional) ignores unknown keys, and LAmount below rides
				// the same tag either way.
				tNBT.putString("FluidName", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tCopy.getFluid()).toString());
				tNBT.putInt("Amount", bindInt(mAmount));
				*///?}
			}
			aNBT.put(aKey, tNBT);
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
		// upstream :359 rebinds the amount in place; the 1.20.1 carrier skips the rebind at 0 L
		// — setAmount(0) empty-flags the stack (FluidStack.updateEmpty) and collapses the kept
		// filter identity the keepFilter state exists to preserve (task
		// p12-barrel-keepfilter-logistics). The upstream rebind at 0 was loss-free (no empty
		// flag on a 1.7.10 FluidStack); mAmount stays the authoritative amount either way.
		if (mFluid != null && mAmount > 0) mFluid.setAmount(bindInt(mAmount));
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
		//? if forge {
		FluidStack rFluid = new FluidStack(mFluid, aDrained);
		//?} else {
		/*FluidStack rFluid = mFluid.copyWithAmount(aDrained); // 21.1: no copy ctor — copyWithAmount(int) is the same identity-at-count (javap 21.1.249)
		*///?}
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

	/**
	 * Upstream :314 verbatim — null content is the empty state (upstream keeps the stack, not
	 * just the amount). The W1 port added {@code || mFluid.isEmpty()} for the 1.20.1 empty
	 * flag; that collapsed the keepFilter state ({@code mFluid != null, mAmount == 0}) into
	 * "empty", so a refill ADOPTED any fluid instead of gating on the kept filter — task
	 * p12-barrel-keepfilter-logistics restores the upstream verdict. The state is only
	 * reachable under {@code mPreventDraining} (every other path ends in setEmpty's null).
	 */
	public boolean isEmpty() {
		return mFluid == null;
	}

	public boolean isFull() {return mAmount >= capacity();}
	public boolean has(long aAmount) {return mAmount >= aAmount;}
	public boolean has() {return mAmount > 0;}

	/**
	 * Upstream :321 — amount-independent fluid equality ({@code FL.equal} works on the REAL
	 * fluid; a 1.7.10 FluidStack has no empty flag). The 1.20.1 carrier routes the comparison
	 * through the raw fluid when the stored stack is empty-flagged (the keepFilter 0-amount
	 * state): {@code isFluidEqual} collapses it to Fluids.EMPTY on both sides and would
	 * answer false for the very identity this state exists to keep (task
	 * p12-barrel-keepfilter-logistics). A non-empty stored stack takes the plain
	 * {@code isFluidEqual} path, bit-identical to the W1 behaviour.
	 */
	public boolean contains(@Nullable FluidStack aFluid) {
		if (mFluid == null || aFluid == null || aFluid.isEmpty()) return false;
		if (!mFluid.isEmpty()) return mFluid.isFluidEqual(aFluid);
		//? if forge {
		if (mFluid.getRawFluid() != aFluid.getRawFluid()) return false;
		return mFluid.getTag() == null ? aFluid.getTag() == null : aFluid.getTag() != null && mFluid.getTag().equals(aFluid.getTag());
		//?} else {
		/*// 21.1: getRawFluid/getTag are gone with the component rework — the empty-flagged
		//keepFilter arm compares the collapsed getFluid() pair and delegates the tag half to
		//the forge-parity static (FluidStack.areFluidStackTagsEqual, javap 21.1.249).
		if (mFluid.getFluid() != aFluid.getFluid()) return false;
		return FluidStack.areFluidStackTagsEqual(mFluid, aFluid);
		*///?}
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
		//? if forge {
		return isEmpty() || aMax <= 0 ? null : new FluidStack(mFluid, bindInt(Math.min(mAmount, aMax)));
		//?} else {
		/*return isEmpty() || aMax <= 0 ? null : mFluid.copyWithAmount(bindInt(Math.min(mAmount, aMax))); // 21.1: no copy ctor — copyWithAmount(int)
		*///?}
	}

	//? if neoforge {
	/*// 21.1: the FluidStack codec face (parse/save) needs a HolderLookup.Provider — the frozen
	//builtin registry view serves offline tests and in-world saves alike (fluid id lookup only).
	private static net.minecraft.core.HolderLookup.Provider nbtAccess() {
		return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
	}
	*///?}

	/** Upstream UT.Code.bindInt (UT.java:1565) — the long→int boundary clamp. */
	public static int bindInt(long aBoundValue) {
		return (int)Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));
	}
}
