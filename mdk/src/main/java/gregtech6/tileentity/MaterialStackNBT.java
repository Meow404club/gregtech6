package gregtech6.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.MaterialStackSerializer;
import gregapi.oredict.OreDictMaterialStack;

/**
 * CompoundTag binding of the MaterialStackSerializer.Storage seam
 * (src/main/java/gregapi/oredict/MaterialStackSerializer.java:44-58) — the Phase-2/3
 * NBT adapter promised there, sized for BE NBT consumption points like the upstream
 * crucible/smeltery NBT_MATERIALS tags (MultiTileEntityCrucible.java:108,
 * MultiTileEntitySmeltery.java:103). ~40 lines as scoped by ADR-P3-2.
 *
 * <p>Key contract (upstream OreDictMaterialStack.save :105-114 / load :120-123):
 * <ul>
 * <li>"a": the amount, long;</li>
 * <li>"i": the material ID, written AND read as a <b>short</b> to stay byte-compatible
 *     with GT6 saves (upstream setShort; MaterialStackSerializer.java:37-38 note) —
 *     the seam keeps exposing int, this adapter funnels it through short;</li>
 * <li>"m": the material internal name, written instead of "i" when mID &lt; 0.</li>
 * </ul>
 *
 * <p>Task p26-crucible-physics-smeltery closes the list gap announced above: the
 * saveList/loadList pair (upstream OreDictMaterialStack.java:129-160, NOT
 * ported into the root pure layer per its :37-39 deviation note) lands HERE over the
 * same Storage adapter, because the crucible mContent = List&lt;OreDictMaterialStack&gt;
 * is its consumer. Upstream shape kept verbatim: a COMPOUND of "0".."n"-indexed
 * sub-compounds plus a "size" int — NOT a vanilla ListTag (GT6 save compatibility).
 */
public final class MaterialStackNBT {

	private MaterialStackNBT() {}

	/** Wraps a CompoundTag into the Storage seam. */
	public static MaterialStackSerializer.Storage storage(CompoundTag aNBT) {
		return new CompoundTagStorage(aNBT);
	}

	/** {@link MaterialStackSerializer.Default#save} convenience. */
	public static void save(OreDictMaterialStack aStack, CompoundTag aNBT) {
		MaterialStackSerializer.Default.INSTANCE.save(aStack, storage(aNBT));
	}

	/** {@link MaterialStackSerializer.Default#load} convenience with an explicit resolver. */
	public static OreDictMaterialStack load(CompoundTag aNBT, MaterialStackSerializer.MaterialResolver aResolver) {
		return MaterialStackSerializer.Default.INSTANCE.load(storage(aNBT), aResolver);
	}

	/** {@link #load(CompoundTag, MaterialResolver)} with the live {@link MaterialRegistry#INSTANCE} as the resolver. */
	public static OreDictMaterialStack load(CompoundTag aNBT) {
		return load(aNBT, MaterialRegistry.INSTANCE);
	}

	/**
	 * Upstream OreDictMaterialStack.saveList(String, NBTTagCompound, List) (:145-148) —
	 * the list rides a sub-compound under aKey (see {@link #listToCompound}).
	 */
	public static void saveList(List<OreDictMaterialStack> aList, String aKey, CompoundTag aNBT) {
		aNBT.put(aKey, listToCompound(aList));
	}

	/**
	 * Upstream OreDictMaterialStack.saveList(List) (:129-143) over the Storage adapter:
	 * null-safe, MT.NULL materials dropped, the written count in "size".
	 *
	 * <p>Declared deviation: valid entries are written under CONSECUTIVE indices
	 * ("0".."count"-1) instead of their original list positions. Upstream keeps the
	 * original index, which desynchronizes the "size" counter the moment a MT.NULL
	 * entry precedes a valid one (the trailing entry becomes unreadable — upstream
	 * :150-157 walks 0..size-1 only). The crucible sweep (Smeltery :249) removes NULL
	 * entries every tick, so a live mContent never carries one and the two shapes are
	 * payload-identical there; the consecutive form additionally round-trips a list
	 * that still holds NULL entries (the offline fixture case).
	 */
	public static CompoundTag listToCompound(List<OreDictMaterialStack> aList) {
		CompoundTag rNBT = new CompoundTag();
		if (aList == null) return rNBT;
		int l = 0;
		for (OreDictMaterialStack tStack : aList) {
			if (tStack != null && tStack.mMaterial != MT.NULL) {
				CompoundTag tEntry = new CompoundTag();
				save(tStack, tEntry);
				rNBT.put("" + l++, tEntry);
			}
		}
		rNBT.putInt("size", l);
		return rNBT;
	}

	/** Upstream OreDictMaterialStack.loadList(String, NBTTagCompound) (:155-159) with an explicit resolver. */
	public static List<OreDictMaterialStack> loadList(String aKey, CompoundTag aNBT, MaterialStackSerializer.MaterialResolver aResolver) {
		return compoundToList(aNBT.getCompound(aKey), aResolver);
	}

	/** {@link #loadList(String, CompoundTag, MaterialResolver)} with the live {@link MaterialRegistry#INSTANCE} as the resolver. */
	public static List<OreDictMaterialStack> loadList(String aKey, CompoundTag aNBT) {
		return loadList(aKey, aNBT, MaterialRegistry.INSTANCE);
	}

	/**
	 * Upstream OreDictMaterialStack.loadList(NBTTagCompound) (:147-160) verbatim: walk
	 * "0".."size"-1, skip stacks resolving to MT.NULL. A missing key yields the empty
	 * list (CompoundTag.getCompound never nulls).
	 */
	public static List<OreDictMaterialStack> compoundToList(CompoundTag aNBT, MaterialStackSerializer.MaterialResolver aResolver) {
		List<OreDictMaterialStack> rList = new ArrayList<>();
		if (aNBT == null || !aNBT.contains("size")) return rList;
		for (int i = 0, j = aNBT.getInt("size"); i < j; i++) {
			OreDictMaterialStack tStack = load(aNBT.getCompound("" + i), aResolver);
			if (tStack != null && tStack.mMaterial != MT.NULL) rList.add(tStack);
		}
		return rList;
	}

	/**
	 * CompoundTag-backed Storage view. "i" funnels through putShort/getShort — the GT6
	 * save byte-compatibility contract — while the seam's int surface stays unchanged.
	 */
	static final class CompoundTagStorage implements MaterialStackSerializer.Storage {
		private final CompoundTag mNBT;

		CompoundTagStorage(CompoundTag aNBT) {
			mNBT = aNBT;
		}

		@Override
		public void put(String aKey, long aValue) {
			mNBT.putLong(aKey, aValue);
		}

		@Override
		public void put(String aKey, int aValue) {
			mNBT.putShort(aKey, (short) aValue);
		}

		@Override
		public void put(String aKey, String aValue) {
			mNBT.putString(aKey, aValue);
		}

		@Override
		public boolean has(String aKey) {
			return mNBT.contains(aKey);
		}

		@Override
		public long getLong(String aKey) {
			return mNBT.getLong(aKey);
		}

		@Override
		public int getInt(String aKey) {
			return mNBT.getShort(aKey);
		}

		@Override
		public String getString(String aKey) {
			return mNBT.getString(aKey);
		}
	}
}
