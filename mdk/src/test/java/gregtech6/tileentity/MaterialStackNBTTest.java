package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

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

	// ====================================================================================
	// the list face (task p26-crucible-physics-smeltery): the crucible mContent
	// List<OreDictMaterialStack> round trip — upstream OreDictMaterialStack.saveList/
	// loadList (:129-160): a "0".."n"-indexed compound + "size", NOT a vanilla ListTag.
	// ====================================================================================

	@Test
	public void crucibleContentListRoundTrips() {
		MT.init();
		assertTrue(MT.Iron.mID >= 0 && MT.Copper.mID >= 0, "precondition: registered materials");

		List<OreDictMaterialStack> tContent = new ArrayList<>();
		tContent.add(new OreDictMaterialStack(MT.Iron, 2 * 648648000L));
		tContent.add(new OreDictMaterialStack(MT.Copper, 648648000L));

		CompoundTag tNBT = new CompoundTag();
		MaterialStackNBT.saveList(tContent, "gt.materials", tNBT);

		// the upstream container shape: size marker + indexed sub-compounds
		CompoundTag tList = tNBT.getCompound("gt.materials");
		assertEquals(2, tList.getInt("size"));
		assertTrue(tList.contains("0", Tag.TAG_COMPOUND));
		assertTrue(tList.contains("1", Tag.TAG_COMPOUND));
		// the 'i' short save-compat contract holds per ENTRY
		assertEquals((short) MT.Iron.mID, tList.getCompound("0").getShort("i"));
		assertEquals(2 * 648648000L, tList.getCompound("0").getLong("a"));

		List<OreDictMaterialStack> tBack = MaterialStackNBT.loadList("gt.materials", tNBT);
		assertEquals(2, tBack.size());
		assertSame(MT.Iron, tBack.get(0).mMaterial);
		assertEquals(2 * 648648000L, tBack.get(0).mAmount);
		assertSame(MT.Copper, tBack.get(1).mMaterial);
		assertEquals(648648000L, tBack.get(1).mAmount);
	}

	@Test
	public void crucibleContentListDropsNullMaterialEntries() {
		MT.init();
		// upstream :136/:152 — MT.NULL entries are skipped on save and on load
		List<OreDictMaterialStack> tContent = new ArrayList<>();
		tContent.add(new OreDictMaterialStack(MT.NULL, 25L));
		tContent.add(new OreDictMaterialStack(MT.Iron, 648648000L));

		CompoundTag tNBT = new CompoundTag();
		MaterialStackNBT.saveList(tContent, "gt.materials", tNBT);
		assertEquals(1, tNBT.getCompound("gt.materials").getInt("size"), "the NULL entry never reaches the NBT");

		List<OreDictMaterialStack> tBack = MaterialStackNBT.loadList("gt.materials", tNBT);
		assertEquals(1, tBack.size());
		assertSame(MT.Iron, tBack.get(0).mMaterial);
	}

	@Test
	public void crucibleContentListEmptyAndMissingKeys() {
		CompoundTag tNBT = new CompoundTag();
		assertTrue(MaterialStackNBT.loadList("gt.materials", tNBT).isEmpty(), "a missing key yields the empty list");
		MaterialStackNBT.saveList(new ArrayList<>(), "gt.materials", tNBT);
		assertEquals(0, tNBT.getCompound("gt.materials").getInt("size"));
		assertTrue(MaterialStackNBT.loadList("gt.materials", tNBT).isEmpty());
	}
}
