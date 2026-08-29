package gregtech6.tileentity;

import net.minecraft.nbt.CompoundTag;

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
