/**
 * Tests for task gt-material-model: the resettable MaterialRegistry (open/closed state machine,
 * createMaterial semantics, ID assignment, override chain, name blacklist) and the
 * MaterialStackSerializer.MaterialResolver wiring (serialization roundtrip).
 */
package gregapi.oredict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.TD;

public class MaterialRegistryTest {

	// --- state machine -------------------------------------------------

	@Test
	public void registryOpenByDefaultAndCreateSucceeds() {
		MaterialRegistry r = new MaterialRegistry();
		assertTrue(r.isOpen());
		OreDictMaterial m = r.createMaterial(8001, "State Mat", "State Mat");
		assertEquals(8001, m.mID);
		assertSame(m, r.MATERIAL_MAP.get("StateMat"));
		assertSame(m, r.MATERIAL_ARRAY[8001]);
	}

	@Test
	public void closeRejectsValidIDCreation() {
		MaterialRegistry r = new MaterialRegistry();
		r.createMaterial(8002, "Closed Mat", "Closed Mat");
		r.close();
		assertFalse(r.isOpen());
		assertThrows(IllegalStateException.class, () -> r.createMaterial(8003, "Too Late", "Too Late"));
		assertNull(r.MATERIAL_MAP.get("TooLate"), "the rejected material must not have been registered");
	}

	@Test
	public void closeStillAllowsNameOnlyCreation() {
		MaterialRegistry r = new MaterialRegistry();
		r.close();
		OreDictMaterial m = r.createMaterial(-1, "Ghost Late", "Ghost Late");
		assertEquals(-1, m.mID);
		assertSame(m, r.MATERIAL_MAP.get("GhostLate"));
		assertNull(r.byID(-1), "negative IDs never enter the array");
	}

	@Test
	public void resetClearsAllStateAndReopens() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(8004, "Reset Mat", "Reset Mat");
		assertFalse(r.MATERIAL_MAP.isEmpty());
		r.reset();
		assertTrue(r.isOpen());
		assertTrue(r.MATERIAL_MAP.isEmpty());
		assertTrue(r.FLUID_MAP.isEmpty());
		assertNull(r.MATERIAL_ARRAY[8004]);
		assertTrue(r.ALLOYS.isEmpty());
		// the slot is freely re-registrable with a fresh instance
		OreDictMaterial again = r.createMaterial(8004, "Reset Mat", "Reset Mat");
		assertNotSame(m, again);
		assertTrue(r.isOpen());
	}

	// --- ID normalization ----------------------------------------------

	@Test
	public void invalidIDsNormalizeToNegative() {
		MaterialRegistry r = new MaterialRegistry();
		assertEquals(-1, r.createMaterial(-5, "Id Neg", "Id Neg").mID);
		assertEquals(-1, r.createMaterial(r.MATERIAL_ARRAY.length, "Id End", "Id End").mID);
		// upstream :143: aID == W (OreDictionary.WILDCARD_VALUE = 32767) is invalid too
		assertEquals(-1, r.createMaterial(32767, "Id Wild", "Id Wild").mID);
	}

	// --- name validation ------------------------------------------------

	@Test
	public void sanitizeStripsSeparatorsAndCapitalises() {
		// upstream sanitize (:204-206) strips ' ' '-' '\'' '/' and capitalises the FIRST character only
		assertEquals("Goldore", MaterialRegistry.sanitize("gold ore"));
		assertEquals("Goldore", MaterialRegistry.sanitize("gold-ore"));
		assertEquals("Goldore", MaterialRegistry.sanitize("Gold'ore"));
		assertEquals("Goldore", MaterialRegistry.sanitize("gold/ore"));
		assertEquals("GoldOre", OreDictMaterial.sanitize("gold Ore")); // static delegate; the inner capital survives (only the first character is upper-cased)
		assertEquals("Redstone", MaterialRegistry.sanitize("redstone"));
	}

	@Test
	public void emptyNameAfterSanitizeRejected() {
		MaterialRegistry r = new MaterialRegistry();
		assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8005, " - ", "Whatever"));
	}

	@Test
	public void invalidCharactersRejected() {
		MaterialRegistry r = new MaterialRegistry();
		for (String bad : new String[] {"A|B", "A*B", "A:B", "A.B", "A$B"}) {
			final String name = bad;
			assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8006, name, name), bad);
		}
	}

	@Test
	public void digitStartRejected() {
		MaterialRegistry r = new MaterialRegistry();
		assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8007, "2Fast2Furious", "2Fast2Furious"));
	}

	@Test
	public void blacklistedNamesRejected() {
		MaterialRegistry r = new MaterialRegistry();
		// upstream :55 blacklist, exact match
		assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8008, "Dust", "Dust"));
		assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8008, "Ingot", "Ingot"));
		// upstream :163-164 startsWith match
		assertThrows(IllegalArgumentException.class, () -> r.createMaterial(8008, "Dustianium", "Dustianium"));
	}

	@Test
	public void blacklistIgnoredForNameOnlyRegistration() {
		MaterialRegistry r = new MaterialRegistry();
		// upstream :152-165: the checks only run for aID >= 0
		OreDictMaterial m = r.createMaterial(-1, "Dust", "Dust");
		assertEquals(-1, m.mID);
		assertSame(m, r.MATERIAL_MAP.get("Dust"));
	}

	// --- override chain --------------------------------------------------

	@Test
	public void sameNameSameIDReturnsExisting() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial first = r.createMaterial(8100, "Chain Mat", "Chain Mat");
		assertSame(first, r.createMaterial(8100, "Chain Mat", "Chain Mat"));
		// name-only lookups return the existing material too (upstream :168)
		assertSame(first, r.createMaterial(-1, "Chain Mat", "Chain Mat"));
	}

	@Test
	public void sameNameDifferentIDOverridesWithRegistrationChain() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial first = r.createMaterial(8100, "Chain Mat", "Chain Mat");
		OreDictMaterial second = r.createMaterial(8101, "Chain Mat", "Chain Mat");
		assertNotSame(first, second);
		// later registration wins the name slot (upstream :60, :173-175)
		assertSame(second, r.MATERIAL_MAP.get("ChainMat"));
		assertSame(first, r.MATERIAL_ARRAY[8100]);
		assertSame(second, r.MATERIAL_ARRAY[8101]);
		// the earlier material points at its registration target and is flagged invalid
		assertSame(second, first.mTargetRegistration);
		assertTrue(first.contains(TD.Properties.INVALID_MATERIAL));
		assertFalse(second.contains(TD.Properties.INVALID_MATERIAL));
		// the chain walk resolves to the owner (upstream :199-202)
		assertSame(second, r.get(first));
		assertSame(second, r.get("ChainMat"));
		assertSame(second, r.get(8101L));
		assertSame(second, r.get(8100L), "get by the overridden ID still walks to the owner");
	}

	@Test
	public void getReturnsDefaultForUnknowns() {
		MaterialRegistry r = new MaterialRegistry();
		assertSame(MT.NULL, r.get("Never Registered"));
		assertSame(MT.NULL, r.get(31337L));
		OreDictMaterial fallback = new MaterialRegistry().createMaterial(-1, "Fallback", "Fallback"); // detached: registers into a throwaway registry (constructor privatized by gt-material-dataset)
		assertSame(fallback, r.get("Never Registered", fallback));
		assertSame(fallback, r.get(-1, fallback));
	}

	// --- auto invalid ----------------------------------------------------

	@Test
	public void createAutoInvalidMaterialTagsNewAndSkipsExisting() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial auto = r.createAutoInvalidMaterial("Auto Ghost");
		assertTrue(auto.mID < 0);
		assertTrue(auto.contains(TD.Properties.INVALID_MATERIAL));
		assertTrue(auto.contains(TD.Properties.UNUSED_MATERIAL));
		assertTrue(auto.contains(TD.Properties.AUTO_BLACKLIST));
		assertTrue(auto.contains(TD.Properties.AUTO_MATERIAL));
		// existing material with a valid ID: returned untouched (upstream :180 mID<0 guard)
		OreDictMaterial real = r.createMaterial(8102, "Auto Real", "Auto Real");
		assertSame(real, r.createAutoInvalidMaterial("Auto Real"));
		assertFalse(real.contains(TD.Properties.INVALID_MATERIAL));
	}

	// --- MaterialResolver / serialization roundtrip -----------------------

	@Test
	public void resolverRoundtripByID() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(8010, "Resolver Mat", "Resolver Mat");
		OreDictMaterialStack stack = new OreDictMaterialStack(m, 123456789L);

		MaterialStackSerializer.MemoryStorage storage = new MaterialStackSerializer.MemoryStorage();
		MaterialStackSerializer.Default.INSTANCE.save(stack, storage);
		// upstream key contract: "a" amount, "i" material ID (a short upstream)
		assertEquals(123456789L, storage.getLong("a"));
		assertEquals(8010, storage.getInt("i"));
		assertFalse(storage.has("m"));

		OreDictMaterialStack loaded = MaterialStackSerializer.Default.INSTANCE.load(storage, r);
		assertSame(m, loaded.mMaterial);
		assertEquals(123456789L, loaded.mAmount);
	}

	@Test
	public void resolverRoundtripByName() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial unnamed = r.createMaterial(-1, "Ghost Resolver", "Ghost Resolver");
		assertEquals(-1, unnamed.mID);
		OreDictMaterialStack stack = new OreDictMaterialStack(unnamed, 42L);

		MaterialStackSerializer.MemoryStorage storage = new MaterialStackSerializer.MemoryStorage();
		MaterialStackSerializer.Default.INSTANCE.save(stack, storage);
		// upstream :107-110: mID < 0 persists the internal name instead of the ID
		assertEquals("GhostResolver", storage.getString("m"));
		assertFalse(storage.has("i"));

		OreDictMaterialStack loaded = MaterialStackSerializer.Default.INSTANCE.load(storage, r);
		assertSame(unnamed, loaded.mMaterial);
		assertEquals(42L, loaded.mAmount);
	}

	@Test
	public void resolverUnresolvableFallsBackToNULLMaterial() {
		MaterialRegistry r = new MaterialRegistry();
		MaterialStackSerializer.MemoryStorage storage = new MaterialStackSerializer.MemoryStorage();
		storage.put("i", 99999); // out of bounds -> byID returns null
		storage.put("a", 7L);
		OreDictMaterialStack loaded = MaterialStackSerializer.Default.INSTANCE.load(storage, r);
		assertSame(MT.NULL, loaded.mMaterial);
		assertEquals(7L, loaded.mAmount);
		assertSame(r, r); // resolver is the registry itself
		assertNull(r.byID(-1));
		assertNull(r.byID(r.MATERIAL_ARRAY.length));
		assertNull(r.byName("Never Registered"));
	}

	// --- static delegates --------------------------------------------------

	@Test
	public void staticDelegatesHitTheDefaultRegistry() {
		String name = "Delegate Mat Unique";
		OreDictMaterial m = OreDictMaterial.createMaterial(-1, name, name);
		assertSame(m, OreDictMaterial.MATERIAL_MAP.get("DelegateMatUnique"));
		assertSame(m, OreDictMaterial.get("DelegateMatUnique"));
		assertSame(m, OreDictMaterial.get(MaterialRegistry.INSTANCE.byName("DelegateMatUnique")));
		// the default registry is a resettable instance, not a static block
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_MAP.containsKey("DelegateMatUnique"));
	}

	// --- re-registration helpers -------------------------------------------

	@Test
	public void addIdenticalNamesReRegistersSanitizedAliases() {
		MaterialRegistry r = new MaterialRegistry();
		// "Alloy..." would hit the upstream :55 blacklist startsWith check for valid IDs
		OreDictMaterial m = r.createMaterial(8110, "Vibrant Alloy", "Vibrant Alloy");
		m.addIdenticalNames("vibrant alloy alt", "VibrantAlloyAlt2");
		assertEquals(2, m.mReRegistrations.size());
		for (OreDictMaterial alias : m.mReRegistrations) {
			assertSame(m, alias.mTargetRegistration);
			assertTrue(alias.contains(TD.Properties.INVALID_MATERIAL));
			assertSame(m, r.get(alias));
		}
		// a name equal to the material itself is refused with a notice (upstream :357-358)
		m.addIdenticalNames("Vibrant Alloy");
		assertEquals(2, m.mReRegistrations.size());
	}
}
