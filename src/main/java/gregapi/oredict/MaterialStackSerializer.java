/**
 * Part of the GregTech 6 (1.7.10) modernization port (task gt-material-foundation).
 * Replaces the Minecraft NBT coupling of upstream gregapi/oredict/OreDictMaterialStack.java
 * (NBT compound import at :29, save/load at :105-127).
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.oredict;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal pure-Java serialization seam for {@link OreDictMaterialStack}.
 *
 * Upstream GT6 persisted stacks directly via Minecraft NBT. To keep
 * this module free of Minecraft types, persistence is expressed through this interface; the
 * NeoForge binding (NBTTagCompound-backed Storage, and eventually MC Codec) lands in Phase 2
 * and must keep the "a"/"i"/"m" key contract so GT6 world data stays loadable.
 *
 * Key contract, mirroring upstream OreDictMaterialStack.save() (:105-114) and load() (:120-123):
 * - "a": the amount (long). Upstream wrote it via UT.NBT.setNumber.
 * - "i": the material ID (upstream a short via setShort, exposed as int here; a Phase-2 NBT
 *   Storage should write/read a short to stay byte-compatible with GT6 saves).
 * - "m": the material internal name, written instead of "i" when mID < 0.
 *
 * Material resolution (MATERIAL_ARRAY[id] / MATERIAL_MAP.get(name) upstream) is a registry
 * concern owned by task card gt-material-model, hence the {@link MaterialResolver} seam.
 */
public interface MaterialStackSerializer {
	void save(OreDictMaterialStack aStack, Storage aStorage);

	OreDictMaterialStack load(Storage aStorage, MaterialResolver aResolver);

	/** Minimal key/value storage abstraction. Phase 2 provides an NBT-backed implementation. */
	interface Storage {
		void put(String aKey, long aValue);
		void put(String aKey, int aValue);
		void put(String aKey, String aValue);
		boolean has(String aKey);
		long getLong(String aKey);
		int getInt(String aKey);
		String getString(String aKey);
	}

	/** Resolves stored material references back to material objects (registry lives in card gt-material-model). */
	interface MaterialResolver {
		OreDictMaterial byID(int aID);
		OreDictMaterial byName(String aName);
	}

	/** Reference implementation carrying the upstream save/load logic verbatim, NBT swapped for Storage. */
	final class Default implements MaterialStackSerializer {
		public static final Default INSTANCE = new Default();

		@Override
		public void save(OreDictMaterialStack aStack, Storage aStorage) {
			// upstream :106-113: amount "a"; name "m" for unregistered (mID < 0) materials, else id "i"
			aStorage.put("a", aStack.mAmount);
			if (aStack.mMaterial.mID < 0) {
				aStorage.put("m", aStack.mMaterial.mNameInternal);
				return;
			}
			aStorage.put("i", aStack.mMaterial.mID);
		}

		@Override
		public OreDictMaterialStack load(Storage aStorage, MaterialResolver aResolver) {
			// upstream :120-123: "i" present -> id lookup, else name lookup
			if (aStorage.has("i")) return new OreDictMaterialStack(aResolver.byID(aStorage.getInt("i")), aStorage.getLong("a"));
			return new OreDictMaterialStack(aResolver.byName(aStorage.getString("m")), aStorage.getLong("a"));
		}
	}

	/** Simple in-memory Storage (insertion ordered), usable for tests and tooling. */
	final class MemoryStorage implements Storage {
		private final Map<String, Object> mData = new LinkedHashMap<>();

		@Override
		public void put(String aKey, long aValue) { mData.put(aKey, aValue); }

		@Override
		public void put(String aKey, int aValue) { mData.put(aKey, aValue); }

		@Override
		public void put(String aKey, String aValue) { mData.put(aKey, aValue); }

		@Override
		public boolean has(String aKey) { return mData.containsKey(aKey); }

		@Override
		public long getLong(String aKey) { return (Long)mData.get(aKey); }

		@Override
		public int getInt(String aKey) { return (Integer)mData.get(aKey); }

		@Override
		public String getString(String aKey) { return (String)mData.get(aKey); }
	}
}
