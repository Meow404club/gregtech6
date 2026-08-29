package gregapi.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import gregapi.oredict.MaterialGraph;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * Idempotency contract for {@code MaterialRegistry.reset()} -> {@code MT.init()} (task
 * p2-registry-reset-idempotency, the card-3 gap report). The {@code @BeforeAll} flood is the
 * observed baseline generation; every test re-floods and demands the same numbers back. The
 * assertions hold no matter which other test class touched the shared registry first, because
 * after the fix every generation re-creates the full table.
 *
 * Upstream evidence: MT.init() guard is upstream MT.java:1888; AM's identity-check guard is the
 * reset-aware pattern at AM.java:655; addAlloyingRecipe (:455-466) has NO dedup semantics and this
 * suite must never require one — generation reuse (not double registration) was the Invar
 * double-recipe root cause, and the applyCrucibleAlloyReferences quirk
 * (GT_API_Post.java:816-820, alloySimple :426-429 already recorded mComponents) is upstream
 * behavior that stays.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class MTInitResetIdempotencyTest {

	private static int sSize, sAny, sAnti;

	@BeforeAll
	static void bootstrap() {
		MaterialRegistry.INSTANCE.reset();
		MT.init();
		sSize = MaterialRegistry.INSTANCE.MATERIAL_MAP.size();
		sAny = countByPrefix("Any");
		sAnti = countByPrefix("Anti");
	}

	private static void flood() {
		MaterialRegistry.INSTANCE.reset();
		MT.init();
	}

	private static int countByPrefix(String aPrefix) {
		int rCount = 0;
		for (String tName : MaterialRegistry.INSTANCE.MATERIAL_MAP.keySet()) if (tName.startsWith(aPrefix)) rCount++;
		return rCount;
	}

	@Test
	void a000_fullFloodBaseline() {
		// MTTableFidelityTest.a000 freezes 2200 for a cold first flood; after the fix every
		// generation reproduces it, so the absolute number is order-independent.
		assertEquals(2200, sSize);
	}

	@Test
	void a100_mapSizeStableAcrossReset() {
		flood();
		assertEquals(sSize, MaterialRegistry.INSTANCE.MATERIAL_MAP.size(),
			"reset() -> MT.init() must re-register the whole table, not just the reg batches");
	}

	@Test
	void a110_anyGroupsStableAcrossReset() {
		flood();
		assertEquals(sAny, countByPrefix("Any"), "the ANY alias groups must survive a re-flood");
		assertTrue(countByPrefix("Any") > 0, "ANY groups must be registered at all");
	}

	@Test
	void a120_antiMatterStableAcrossReset() {
		flood();
		assertEquals(sAnti, countByPrefix("Anti"), "the AM anti-matter table must survive a re-flood");
		assertTrue(countByPrefix("Anti") > 0, "AM must be registered at all");
	}

	@Test
	void a130_techAliasesPointAtCurrentGeneration() {
		flood();
		// TECH aliases (upstream MT.java:1950) are field-to-field identities; after a re-flood
		// they must be re-bound, not frozen on the first generation's instances.
		assertSame(MT.Brick, MT.TECH.Brick);
		assertSame(ANY.Wood, MT.TECH.AnyWood);
		assertSame(ANY.Fe, MT.TECH.AnyIron); // upstream MT.java:1950: AnyIron aliases ANY.Fe ("Any Iron")
		// OREMATS deprecated aliases (Pyrolusite = MnO2 etc., upstream MT.java:4149 block)
		assertSame(MT.MnO2, MT.OREMATS.Pyrolusite);
		// the reg batches and the TECH chunks must agree on one generation
		assertSame(MT.OREMATS.Magnetite, MaterialRegistry.INSTANCE.MATERIAL_MAP.get("Magnetite"));
	}

	@Test
	void a140_invarAlloyRecipeCountStableAcrossReset() {
		flood();
		assertEquals(1, MT.Invar.mAlloyCreationRecipes.size(),
			"uumAloy (upstream MT.java:1754 via setMcfg+alloyCentrifuge :707-709) registers exactly one recipe per generation");
		flood();
		assertEquals(1, MT.Invar.mAlloyCreationRecipes.size(),
			"the re-flood must not reuse the previous generation's Invar instance (createMaterial same-name+same-ID NOTICE path, upstream :145-148) nor append to its recipe list");
	}

	@Test
	void a150_postInitWiringSymmetricAcrossReset() {
		// applyCrucibleAlloyReferences (GT_API_Post.java:816-820) is postInit semantics; the quirk
		// that it appends mComponents a second time (alloySimple :426-429 recorded it already) is
		// upstream behavior. The contract: the postInit result reproduces identically per generation.
		flood();
		assertEquals(1, MT.Invar.mAlloyCreationRecipes.size());
		assertTrue(MaterialGraph.applyCrucibleAlloyReferences() > 0);
		int tWiredFirst = MT.Invar.mAlloyCreationRecipes.size();
		flood();
		assertEquals(1, MT.Invar.mAlloyCreationRecipes.size());
		assertTrue(MaterialGraph.applyCrucibleAlloyReferences() > 0);
		assertEquals(tWiredFirst, MT.Invar.mAlloyCreationRecipes.size());
	}
}
