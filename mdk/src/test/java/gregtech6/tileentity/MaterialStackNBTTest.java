package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

import gregapi.oredict.MaterialStackSerializer;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.data.MT;

/**
 * MaterialStackNBT — the CompoundTag Storage adapter over
 * gregapi/oredict/MaterialStackSerializer.java:44-78. Key contract from upstream
 * OreDictMaterialStack.save :105-114 / load :120-123: "a" long, "m" name when
 * mID &lt; 0, else "i" — written and read as a <b>short</b> for GT6 save byte
 * compatibility (MaterialStackSerializer.java:37-38).
 */
public class MaterialStackNBTTest extends GTOfflineTestBase {

	@Test
	public void idKeyIsStoredAsShortForGt6SaveCompatibility() {
		CompoundTag tTag = new CompoundTag();
		MaterialStackSerializer.Storage tStorage = MaterialStackNBT.storage(tTag);

		tStorage.put("i", 777);
		// byte-compat: the tag holds a SHORT, not an int
		assertTrue(tTag.contains("i", Tag.TAG_SHORT), "the 'i' key must be stored as a short");
		assertFalse(tTag.contains("i", Tag.TAG_INT), "the 'i' key must not widen to an int");
		assertEquals((short) 777, tTag.getShort("i"));
		assertEquals(777, tStorage.getInt("i"));

		tStorage.put("a", 5L);
		assertTrue(tTag.contains("a", Tag.TAG_LONG));
		assertEquals(5L, tStorage.getLong("a"));

		tStorage.put("m", "NULL");
		assertTrue(tTag.contains("m", Tag.TAG_STRING));
		assertEquals("NULL", tStorage.getString("m"));

		assertTrue(tStorage.has("a"));
		assertFalse(tStorage.has("nonexistent"));
	}

	@Test
	public void registeredMaterialRoundTripsThroughIdKey() {
		MT.init(); // real material identity from the live registry (pure Java flood)

		OreDictMaterialStack tStack = new OreDictMaterialStack(MT.Iron, 12345L);
		assertTrue(MT.Iron.mID >= 0, "precondition: Iron is a registered material");

		CompoundTag tTag = new CompoundTag();
		MaterialStackNBT.save(tStack, tTag);

		assertEquals(12345L, tTag.getLong("a"));
		assertEquals((short) MT.Iron.mID, tTag.getShort("i"));
		assertFalse(tTag.contains("m"), "registered materials must not carry the name fallback key");

		OreDictMaterialStack tBack = MaterialStackNBT.load(tTag); // default resolver = MaterialRegistry.INSTANCE
		assertSame(MT.Iron, tBack.mMaterial);
		assertEquals(12345L, tBack.mAmount);
	}

	@Test
	public void unregisteredMaterialRoundTripsThroughNameKey() {
		// MT.NULL (mID = -1) is registered at MT class load (MT.java:1366-1371), no init() needed
		OreDictMaterialStack tStack = new OreDictMaterialStack(MT.NULL, 25L);

		CompoundTag tTag = new CompoundTag();
		MaterialStackNBT.save(tStack, tTag);

		assertTrue(tTag.contains("m", Tag.TAG_STRING));
		assertEquals("NULL", tTag.getString("m"));
		assertFalse(tTag.contains("i"), "mID < 0 materials must use the name key");
		assertEquals(25L, tTag.getLong("a"));

		OreDictMaterialStack tBack = MaterialStackNBT.load(tTag);
		assertSame(MT.NULL, tBack.mMaterial);
		assertEquals(25L, tBack.mAmount);
	}
}
