package gregtech6.item.energy;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.code.TagData;
import gregapi.data.TD;

/**
 * The battery ITEM (task p29-w4-battery-storage ②) — the port counterpart of the upstream
 * battery MTE family's ITEM-STATE face, {@code TileEntityBase08Battery} transcribed onto a
 * plain {@link Item}. Upstream files every battery as an item-form MultiTileEntity whose
 * charge lives in the MTE fields mirrored to the item NBT; the port has no MTE layer, so
 * the SAME semantics live on the item with the charge riding the stack carrier:
 *
 * <p><b>The NBT face</b> (the card SPEC: NBT_ENERGY + NBT_ENERGY_ACCEPTED read/write
 * semantics, TileEntityBase08Battery.java:68/:73/:82):
 * <ul>
 * <li>{@code gt.energy} (upstream CS.java:1340 NBT_ENERGY) — the stored charge, LONG.
 *     Write face {@link #writeItemNBT} = the :89-90 writeItemNBT2 verbatim
 *     ({@code NBT_ENERGY = mEnergy; NBT_ACTIVE_ENERGY = false});</li>
 * <li>{@code gt.active.energy} (upstream CS.java:1225 NBT_ACTIVE_ENERGY) — the
 *     "spawn/store as FULL" lane: read TRUE collapses the charge to capacity (the :70-74
 *     branch), always written back FALSE (the :83/:90 verbatim);</li>
 * <li>{@code gt.energy.accepted} (upstream CS.java:1304 NBT_ENERGY_ACCEPTED) — the
 *     energy-type lane. Upstream reads it off the NBT into {@code mType} (:68); the port
 *     battery's type is a CONSTRUCTOR constant (one item per family — the type cannot
 *     drift per stack), so the key is honored on the READ path only via
 *     {@link #GT6BatteryItem} construction parity and carried in the class doc instead of
 *     the stack: a per-stack type string would let a save edit turn an EU battery into an
 *     LU one, which the upstream MTE class layout structurally prevents (one MTE class
 *     per type ladder). Declared carrier narrowing.</li>
 * </ul>
 *
 * <p><b>The read clamp</b> (the :76 verbatim): a charge above capacity collapses to
 * capacity on every read path. The port ALSO clamps the write face
 * ({@link #setEnergyStored}) to {@code [0, mCapacity]} — a defensive superset: every
 * upstream caller already produces in-range values (the :160/:171 packet loops guarantee
 * it), so live behavior is unchanged, while a hostile NBT edit or a future caller bug
 * cannot persist an out-of-range charge (the card acceptance ② pins the clamp).
 *
 * <p><b>The packet band</b> (the :62-66 readFromNBT2 transcription, mechanism): the
 * packet SIZE window derives from the row's NBT_INPUT column ({@code mSizeRec} = V[tier]):
 * {@code mSizeMin = mSizeRec / 2}, {@code mSizeMax = mSizeRec * 2}, EXCEPT the floor arm
 * {@code if (mSizeMin <= 8 && mSizeRec > 0) mSizeMin = 1} — an ULV battery (8 EU packets,
 * min 4) would otherwise refuse the sub-16 packets the whole ULV tier runs on, so its
 * window opens to [1..16] while LV+ keep the [rec/2..rec*2] band (LV 32 → [16..64]).
 *
 * <p><b>Stacking</b> (the :143 getMaxStackSize verbatim): a charged battery is a
 * stack of ONE; an empty one stacks to the row's 16 (the upstream aRegistry stack
 * column). The durability bar renders ONLY between the extremes — empty (0) and full
 * (capacity) show no bar, the vanilla undamaged-tool analogy (acceptance ⑥).
 *
 * <p><b>Cut with declaration</b>: the tooltips (:106-108) and the
 * rechargeFromPlayer armor arm (:176-189, COMPAT_EU_ITEM — no port counterpart) ride the
 * pool; the magnifying-glass/tool faces are block-MTE-only and have no item surface here.
 *
 * <p>KJS surface: none (registration face is deferred — the KJS binding pool).
 */
public class GT6BatteryItem extends Item implements IItemEnergy {

	/** The stored-charge key (upstream CS.java:1340 NBT_ENERGY = "gt.energy"). */
	public static final String NBT_ENERGY = "gt.energy";
	/** The store-as-full lane (upstream CS.java:1225 NBT_ACTIVE_ENERGY = "gt.active.energy"). */
	public static final String NBT_ACTIVE_ENERGY = "gt.active.energy";
	/** The accepted-type lane (upstream CS.java:1304 NBT_ENERGY_ACCEPTED = "gt.energy.accepted"). */
	public static final String NBT_ENERGY_ACCEPTED = "gt.energy.accepted";

	/** The row's packet size (the NBT_INPUT column, V[tier]). */
	public final long mSizeRec;
	/** The derived packet band — mSizeRec/2 with the :64 floor arm ({@code <=8 → 1}). */
	public final long mSizeMin;
	/** The derived packet band — mSizeRec*2 (the :63 verbatim). */
	public final long mSizeMax;
	/** The row's capacity (the NBT_CAPACITY column, V[tier] × the family multiplier). */
	public final long mCapacity;
	/** The energy domain — TD.Energy.EU or TD.Energy.LU (the reference-equality gate mType). */
	public final TagData mType;

	/**
	 * @param aSizeRec   the row's packet size (V[tier]; the NBT_INPUT column)
	 * @param aCapacity  the row's capacity (V[tier] × the family multiplier; the NBT_CAPACITY column)
	 * @param aType      the energy domain (the NBT_ENERGY_ACCEPTED column, TD.Energy.EU/LU)
	 */
	public GT6BatteryItem(Properties aProperties, long aSizeRec, long aCapacity, TagData aType) {
		this(aProperties, aSizeRec, aCapacity, (aSizeRec / 2 <= 8 && aSizeRec > 0) ? 1 : aSizeRec / 2, aType);
	}

	/**
	 * The explicit-band constructor (task p36 — the ZPM row): upstream files the band as the
	 * :62-:66 ladder — the :63-:64 DERIVED pair first, then the NBT_INPUT_MIN/NBT_INPUT_MAX
	 * overrides win. The ZPM row carries {@code NBT_INPUT_MIN 1, NBT_INPUT_MAX VMAX[7]}
	 * (Loader_MultiTileEntities.java:1103), so the explicit [1..262144] band is the
	 * transcription; every family row without the overrides keeps the 4-arg derived form
	 * above (zero drift — the derived expression moved into the delegating ctor verbatim).
	 *
	 * @param aSizeMin   the explicit band floor (the NBT_INPUT_MIN column)
	 */
	public GT6BatteryItem(Properties aProperties, long aSizeRec, long aCapacity, long aSizeMin, TagData aType) {
		super(aProperties);
		mSizeRec = aSizeRec;
		mSizeMin = aSizeMin; // the :65 explicit column (the 4-arg ctor passes the :63-64 derived pair)
		mSizeMax = aSizeRec * 2; // :63 — and VMAX[7] = V[7]*2 is the same product, the :66 override folds in
		mCapacity = aCapacity;
		mType = aType;
	}

	// ---------------------------------------------------------------------------
	// the charge carrier (the per-leg fork, the GT6Circuits configurationOf shape:
	// 1.20.1 freeform stack tag, 21.1 the opaque CUSTOM_DATA envelope — same keys)
	// ---------------------------------------------------------------------------

	/** The raw carrier read: the {@code gt.energy} long, 0 when absent. */
	public static long readStoredRaw(ItemStack aStack) {
		if (aStack.isEmpty()) return 0;
		//? if forge {
		return aStack.hasTag() && aStack.getTag().contains(NBT_ENERGY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
				? aStack.getTag().getLong(NBT_ENERGY) : 0;
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getLong(NBT_ENERGY);
		 *///?}
	}

	/** The store-as-full lane read: the {@code gt.active.energy} boolean. */
	public static boolean readActiveEnergy(ItemStack aStack) {
		if (aStack.isEmpty()) return false;
		//? if forge {
		return aStack.hasTag() && aStack.getTag().getBoolean(NBT_ACTIVE_ENERGY);
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean(NBT_ACTIVE_ENERGY);
		 *///?}
	}

	/** The raw carrier write (the :89-90 writeItemNBT2 verbatim: energy + active=false). */
	public static void writeItemNBT(ItemStack aStack, long aEnergy) {
		//? if forge {
		aStack.getOrCreateTag().putLong(NBT_ENERGY, aEnergy);
		aStack.getOrCreateTag().putBoolean(NBT_ACTIVE_ENERGY, false);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		tTag.putLong(NBT_ENERGY, aEnergy);
		tTag.putBoolean(NBT_ACTIVE_ENERGY, false);
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
	}

	/**
	 * The effective charge of a stack (the :70-77 readFromNBT2 verbatim on the item
	 * carrier): the active-energy lane collapses to FULL, the raw long is taken as-is,
	 * the :76 upper clamp closes the read.
	 */
	public static long readStored(GT6BatteryItem aItem, ItemStack aStack) {
		if (readActiveEnergy(aStack)) return aItem.mCapacity; // :70-71
		long rEnergy = readStoredRaw(aStack); // :73
		if (rEnergy > aItem.mCapacity) rEnergy = aItem.mCapacity; // :76
		return rEnergy;
	}

	// ---------------------------------------------------------------------------
	// the IItemEnergy face (the TileEntityBase08Battery transcription)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting) {
		return aEnergyType == mType || aEnergyType == null; // :227
	}

	@Override
	public Collection<TagData> getEnergyTypes(ItemStack aStack) {
		return mType.AS_LIST; // :226, the single-element list (TagData.AS_LIST)
	}

	@Override
	public boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize) {
		// :228 verbatim — type match, single stack, size inside the packet band
		return (aEnergyType == mType || aEnergyType == null) && aStack.getCount() == 1
				&& aSize <= mSizeMax && aSize >= mSizeMin;
	}

	@Override
	public boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize) {
		// :229 verbatim
		return (aEnergyType == mType || aEnergyType == null) && aStack.getCount() == 1
				&& aSize <= mSizeMax && aSize >= mSizeMin;
	}

	@Override
	public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoInject) {
		if (aAmount < 1 || mSizeRec < 1) return 0; // :156
		if (!canEnergyInjection(aEnergyType, aStack, aSize = Math.abs(aSize))) return 0; // :157
		long tEnergy = readStored(this, aStack);
		if (tEnergy >= mCapacity) return 0; // :158
		long rAmount = Math.min(mSizeRec, aAmount); // :159
		while (rAmount > 1 && tEnergy + rAmount * aSize > mCapacity) rAmount--; // :160
		if (aDoInject) setEnergyStored(mType, aStack, tEnergy + rAmount * aSize); // :161
		return rAmount;
	}

	@Override
	public long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, boolean aDoExtract) {
		if (aAmount < 1 || mSizeRec < 1) return 0; // :167
		if (!canEnergyExtraction(aEnergyType, aStack, aSize = Math.abs(aSize))) return 0; // :168
		long tEnergy = readStored(this, aStack);
		if (tEnergy < aSize) return 0; // :169
		long rAmount = Math.min(mSizeRec, aAmount); // :170
		while (rAmount > 1 && tEnergy - rAmount * aSize < 0) rAmount--; // :171
		if (aDoExtract) setEnergyStored(mType, aStack, tEnergy - rAmount * aSize); // :172
		return rAmount;
	}

	@Override
	public boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, boolean aDoUse) {
		// :192-208 with the creative bypass and the rechargeFromPlayer arm cut (the class
		// doc) — the narrowed player-less face
		if (aEnergyType != mType && aEnergyType != null) return F(); // :194
		if (readStored(this, aStack) >= aEnergyAmount) {
			if (aDoUse) setEnergyStored(mType, aStack, readStored(this, aStack) - aEnergyAmount); // :198
			return T();
		}
		if (aDoUse) setEnergyStored(mType, aStack, 0); // :204
		return F();
	}

	@Override
	public ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount) {
		if ((aEnergyType != mType && aEnergyType != null) || aStack.isEmpty()) return aStack; // :212
		long tEnergy = Math.max(0, Math.min(mCapacity, aAmount)); // the :76 clamp hoisted to the write face (class doc)
		writeItemNBT(aStack, tEnergy); // :214 (the writeItemNBT2 carrier)
		return aStack;
	}

	@Override
	public long getEnergyStored(TagData aEnergyType, ItemStack aStack) {
		return aEnergyType == mType || aEnergyType == null ? readStored(this, aStack) : 0; // :218
	}

	@Override
	public long getEnergyCapacity(TagData aEnergyType, ItemStack aStack) {
		return aEnergyType == mType || aEnergyType == null ? mCapacity : 0; // :219
	}

	// ---------------------------------------------------------------------------
	// the stack faces (the :143 max-stack + the durability bar)
	// ---------------------------------------------------------------------------

	@Override
	public int getMaxStackSize(ItemStack aStack) {
		return readStored(this, aStack) > 0 ? 1 : 16; // :143 — charged = single
	}

	/** The bar shows ONLY strictly between empty and full (the vanilla undamaged-tool analogy, acceptance ⑥). */
	@Override
	public boolean isBarVisible(ItemStack aStack) {
		long tStored = readStored(this, aStack);
		return tStored > 0 && tStored < mCapacity;
	}

	@Override
	public int getBarWidth(ItemStack aStack) {
		long tStored = readStored(this, aStack);
		return (int) (13 * tStored / mCapacity); // the vanilla 13-dot durability scale
	}

	/** A fixed green — the charge sign (the vanilla full-durability green). */
	@Override
	public int getBarColor(ItemStack aStack) {
		return 0xFF2FCE2F;
	}

	/** Upstream-literal booleans (the CS T/F names, kept for the transcription parity). */
	private static boolean T() {return true;}
	private static boolean F() {return false;}

	/** The type list is always the singleton (the :226 shape, listed here for the census). */
	public static List<TagData> typesOf(GT6BatteryItem aItem) {
		return List.of(aItem.mType);
	}
}
