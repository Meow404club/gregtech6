/**
 * Tests for the NBT-stripping seam (MaterialStackSerializer) replacing upstream
 * gregapi/oredict/OreDictMaterialStack.java:105-127. The Default implementation must keep
 * the upstream "a"/"i"/"m" key contract so GT6 world data stays loadable after Phase 2.
 */

package gregapi.oredict;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialStackSerializer.MaterialResolver;
import gregapi.oredict.MaterialStackSerializer.MemoryStorage;
import gregapi.oredict.MaterialStackSerializer.Storage;

public class MaterialStackSerializerTest {
	private static final OreDictMaterial REGISTERED = new MaterialRegistry().createMaterial(5, "TIN", "TIN"); // detached registry (constructor privatized by gt-material-dataset)

	private static MaterialResolver resolver() {
		return new MaterialResolver() {
			private final Map<Integer, OreDictMaterial> mByID = new HashMap<>();
			private final Map<String, OreDictMaterial> mByName = new HashMap<>();
			{
				mByID.put(5, REGISTERED);
				mByName.put("TIN", REGISTERED);
				mByName.put("NULL", MT.NULL);
			}

			@Override
			public OreDictMaterial byID(int aID) { return mByID.get(aID); }

			@Override
			public OreDictMaterial byName(String aName) { return mByName.get(aName); }
		};
	}

	@Test
	public void registeredMaterialSavesAsIDKey() {
		MemoryStorage aStorage = new MemoryStorage();
		MaterialStackSerializer.Default.INSTANCE.save(new OreDictMaterialStack(REGISTERED, 123), aStorage);
		// upstream save(): id "i" + amount "a", no name key (:112-113)
		assertTrue(aStorage.has("i"));
		assertEquals(5, aStorage.getInt("i"));
		assertEquals(123L, aStorage.getLong("a"));
		assertFalse(aStorage.has("m"));
	}

	@Test
	public void unregisteredMaterialSavesAsNameKey() {
		MemoryStorage aStorage = new MemoryStorage();
		// MT.NULL has mID -1, so the name path applies (upstream :108-111)
		MaterialStackSerializer.Default.INSTANCE.save(new OreDictMaterialStack(MT.NULL, 456), aStorage);
		assertTrue(aStorage.has("m"));
		assertEquals("NULL", aStorage.getString("m"));
		assertEquals(456L, aStorage.getLong("a"));
		assertFalse(aStorage.has("i"));
	}

	@Test
	public void loadResolvesByIDAndByName() {
		MemoryStorage aStorage = new MemoryStorage();
		aStorage.put("i", 5);
		aStorage.put("a", 789L);
		OreDictMaterialStack aByID = MaterialStackSerializer.Default.INSTANCE.load(aStorage, resolver());
		assertSame(REGISTERED, aByID.mMaterial);
		assertEquals(789L, aByID.mAmount);

		MemoryStorage aNameStorage = new MemoryStorage();
		aNameStorage.put("m", "TIN");
		aNameStorage.put("a", 1L);
		OreDictMaterialStack aByName = MaterialStackSerializer.Default.INSTANCE.load(aNameStorage, resolver());
		assertSame(REGISTERED, aByName.mMaterial);
		assertEquals(1L, aByName.mAmount);
	}

	@Test
	public void roundTripPreservesStack() {
		OreDictMaterialStack aStack = new OreDictMaterialStack(REGISTERED, 987654321L);
		MemoryStorage aStorage = new MemoryStorage();
		aStack.save(MaterialStackSerializer.Default.INSTANCE, aStorage);
		OreDictMaterialStack aLoaded = OreDictMaterialStack.load(MaterialStackSerializer.Default.INSTANCE, aStorage, resolver());
		assertSame(aStack.mMaterial, aLoaded.mMaterial);
		assertEquals(aStack.mAmount, aLoaded.mAmount);
		assertEquals(aStack, aLoaded);
	}

	@Test
	public void customSerializersCanReplaceTheDefault() {
		// the seam is pluggable: the stack methods delegate to whatever serializer is passed
		Storage aStorage = new Storage() {
			long mAmount = -1;
			String mName = null;
			@Override public void put(String aKey, long aValue) { if ("AMOUNT".equals(aKey)) mAmount = aValue; }
			@Override public void put(String aKey, int aValue) {/**/}
			@Override public void put(String aKey, String aValue) { if ("NAME".equals(aKey)) mName = aValue; }
			@Override public boolean has(String aKey) { return true; }
			@Override public long getLong(String aKey) { return mAmount; }
			@Override public int getInt(String aKey) { return 0; }
			@Override public String getString(String aKey) { return mName; }
		};
		MaterialStackSerializer tCustom = new MaterialStackSerializer() {
			@Override public void save(OreDictMaterialStack aStack, Storage aOut) {
				aOut.put("AMOUNT", aStack.mAmount);
				aOut.put("NAME", aStack.mMaterial.mNameInternal);
			}
			@Override public OreDictMaterialStack load(Storage aIn, MaterialResolver aResolver) {
				return new OreDictMaterialStack(aResolver.byName(aIn.getString("NAME")), aIn.getLong("AMOUNT"));
			}
		};
		OreDictMaterialStack aStack = new OreDictMaterialStack(REGISTERED, 42);
		aStack.save(tCustom, aStorage);
		OreDictMaterialStack aLoaded = OreDictMaterialStack.load(tCustom, aStorage, resolver());
		assertSame(REGISTERED, aLoaded.mMaterial);
		assertEquals(42, aLoaded.mAmount);
	}

	@Test
	public void memoryStorageRoundTripsAllTypes() {
		MemoryStorage aStorage = new MemoryStorage();
		aStorage.put("l", Long.MAX_VALUE);
		aStorage.put("i", Integer.MIN_VALUE);
		aStorage.put("s", "text");
		aStorage.put("zero", 0L);
		assertTrue(aStorage.has("l"));
		assertFalse(aStorage.has("missing"));
		assertEquals(Long.MAX_VALUE, aStorage.getLong("l"));
		assertEquals(Integer.MIN_VALUE, aStorage.getInt("i"));
		assertEquals("text", aStorage.getString("s"));
		assertEquals(0L, aStorage.getLong("zero"));
	}
}
