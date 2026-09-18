package gregapi.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * Fidelity tests for task p31-qu-b-materials: the fusion isotope material batch the QU
 * card chain stands on. The eight isotopes (D, T, He_3, Li_6, Be_7, Be_8, B_11, C_13)
 * and the two batch companions (Dilithium, AncientDebris) were wired into the reg0000
 * batch by the a69364513 full-table port — these tests freeze the registry face (静态可达)
 * and the proton/neutron values against the upstream 1.7.10 table (MT.java:381-393, every
 * spot check cites the upstream line), because the Massfab element-disintegration rows
 * (card C) pour exactly <code>(p+n)</code> per unit and the Fusion rows (card D) consume
 * these materials as gas/liquid carriers.
 *
 * <p>The fluid-binding half of the card lives on the mdk side
 * ({@code GTFluidsChemicalFamilyTest#materialSpecBindingRoundTrips}); these tags are its
 * upstream trigger conditions: the GASES tag (diatomicgas/noblegas put it, MT.java:66/:68)
 * drives FL.createGas (Loader_Fluids.java:660) and the MOLTEN tag (alkali/alkaline/
 * metalloid + the explicit :393/:1832 arguments) drives FL.createMolten (:659).
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class MTQuIsotopeFidelityTest {

	@BeforeAll
	static void bootstrap() {
		// The registry may hold state from other test classes; the table needs a clean, open registry.
		MaterialRegistry.INSTANCE.reset();
		MT.init();
	}

	/** The registry face: every batch field is assigned (reg0000, MT.java:1396-1419) and resolvable by internal name and id slot. */
	@Test
	void a000_registryCarriesTheIsotopeBatch() {
		OreDictMaterial[] tBatch = {MT.D, MT.T, MT.He_3, MT.Li_6, MT.Be_7, MT.Be_8, MT.B_11, MT.C_13, MT.Dilithium, MT.AncientDebris};
		for (OreDictMaterial tMaterial : tBatch) {
			assertNotNull(tMaterial, tMaterial == null ? "unassigned field" : tMaterial.mNameInternal);
			assertSame(tMaterial, MaterialRegistry.INSTANCE.MATERIAL_MAP.get(tMaterial.mNameInternal),
					tMaterial.mNameInternal + ": registry-name round trip");
		}
		// element ids are protons × 10 (upstream MT.java:337-338); the two compounds sit in their upstream id slots
		assertEquals(11  , MT.D             .mID);
		assertEquals(12  , MT.T             .mID);
		assertEquals(21  , MT.He_3          .mID);
		assertEquals(31  , MT.Li_6          .mID);
		assertEquals(41  , MT.Be_7          .mID);
		assertEquals(42  , MT.Be_8          .mID);
		assertEquals(51  , MT.B_11          .mID);
		assertEquals(61  , MT.C_13          .mID);
		assertEquals(8317, MT.Dilithium     .mID); // upstream MT.java:1508 crystal(8317, ...)
		assertEquals(8744, MT.AncientDebris .mID); // upstream MT.java:1832 metalore(8744, ...)
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_ARRAY[11] == MT.D);
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_ARRAY[8744] == MT.AncientDebris);
	}

	/**
	 * The proton/neutron pins, upstream MT.java:381-393 verbatim (the massfab element
	 * disintegration pours (p+n) mB of matter per unit — Loader_Recipes_Other.java:969-987):
	 * D=1p1n, T=1p2n, He-3=2p1n, Li-6=3p3n, Be-7=4p3n, Be-8=4p4n, B-11=5p6n, C-13=6p7n.
	 * mMass = p+n (the setStats recompute); the two compounds carry no element stats
	 * (the crystal/metalore helpers never call setStats), so the :969 loop skips them.
	 */
	@Test
	void a010_protonNeutronValuesVerbatim() {
		Object[][] tIsotopes = {
			{MT.D    , "Deuterium"  , "Deuterium"  , 1, 1,  14,    20},
			{MT.T    , "Tritium"    , "Tritium"    , 1, 2,  14,    20},
			{MT.He_3 , "Helium3"    , "Helium-3"   , 2, 1,   1,     4},
			{MT.Li_6 , "Lithium6"   , "Lithium-6"  , 3, 3, 453,  1560},
			{MT.Be_7 , "Beryllium7" , "Beryllium-7", 4, 3, 1560,  2742},
			{MT.Be_8 , "Beryllium8" , "Beryllium-8", 4, 4, 1560,  2742},
			{MT.B_11 , "Boron11"    , "Boron-11"   , 5, 6, 2349,  4200},
			{MT.C_13 , "Carbon13"   , "Carbon-13"  , 6, 7, 3800,  4300},
		};
		for (Object[] tRow : tIsotopes) {
			OreDictMaterial tMaterial = (OreDictMaterial)tRow[0];
			String tId = (String)tRow[1];
			assertEquals(tId, tMaterial.mNameInternal, tId + ": the sanitized internal name (createMaterial :147 strips the minus)");
			assertEquals(tRow[2], tMaterial.mNameLocal, tId + ": the local face keeps the minus");
			assertEquals((long)(Integer)tRow[3], tMaterial.mProtons, tId + ": protons");
			assertEquals((long)(Integer)tRow[4], tMaterial.mNeutrons, tId + ": neutrons");
			assertEquals((long)((Integer)tRow[3] + (Integer)tRow[4]), tMaterial.mMass, tId + ": mass = p+n");
			assertEquals((long)(Integer)tRow[5], tMaterial.mMeltingPoint, tId + ": melting point");
			assertEquals((long)(Integer)tRow[6], tMaterial.mBoilingPoint, tId + ": boiling point");
			assertTrue(tMaterial.contains(TD.Atomic.ELEMENT), tId + ": the element() wrapper tag");
			assertTrue(tMaterial.contains(TD.Processing.FUSION), tId + ": the fusion synthesisable tag (the card D feedstock)");
		}
		// the compounds: no element stats — the fields keep their Technetium ghost default
		// (OreDictMaterial.java:115-116, "Defaults to Technetium": 43p/55n), so the :969
		// disintegration loop never touches them on the ELEMENT predicate below
		assertEquals(43, MT.Dilithium.mProtons);
		assertEquals(55, MT.Dilithium.mNeutrons);
		assertEquals(43, MT.AncientDebris.mProtons);
		assertEquals(55, MT.AncientDebris.mNeutrons);
		assertFalse(MT.Dilithium.contains(TD.Atomic.ELEMENT));
		assertFalse(MT.AncientDebris.contains(TD.Atomic.ELEMENT));
	}

	/**
	 * The fluid-binding trigger tags — the upstream Loader_Fluids.java:658-662 loop
	 * predicates, i.e. exactly why the mdk spec table binds these nine and not Dilithium.
	 */
	@Test
	void a020_fluidBindingTriggerTags() {
		// the GASES legs (diatomicgas :66 / noblegas :68 put it)
		assertTrue(MT.D   .contains(TD.ItemGenerator.GASES));
		assertTrue(MT.T   .contains(TD.ItemGenerator.GASES));
		assertTrue(MT.He_3.contains(TD.ItemGenerator.GASES));
		// the MOLTEN legs (alkali :76 / alkaline :77 / metalloid :62 + the explicit :393/:1832 arguments)
		assertTrue(MT.Li_6         .contains(TD.ItemGenerator.MOLTEN));
		assertTrue(MT.Be_7         .contains(TD.ItemGenerator.MOLTEN));
		assertTrue(MT.Be_8         .contains(TD.ItemGenerator.MOLTEN));
		assertTrue(MT.B_11         .contains(TD.ItemGenerator.MOLTEN));
		assertTrue(MT.C_13         .contains(TD.ItemGenerator.MOLTEN));
		assertTrue(MT.AncientDebris.contains(TD.ItemGenerator.MOLTEN));
		// the faithful absence: the crystal helper (:198) carries no fluid tag
		assertFalse(MT.Dilithium.contains(TD.ItemGenerator.GASES));
		assertFalse(MT.Dilithium.contains(TD.ItemGenerator.MOLTEN));
		assertFalse(MT.Dilithium.contains(TD.ItemGenerator.LIQUID));
	}

	/** The batch anchors beyond the isotopes: the reg0000 aliases, Dilithium's crystal face, AncientDebris's heat chain. */
	@Test
	void a030_aliasesAndCompoundAnchors() {
		// upstream MT.java:569-570 H_2 = D; H_3 = T (the port reg0000 :1398/:1400)
		assertSame(MT.D, MT.H_2);
		assertSame(MT.T, MT.H_3);
		// upstream MT.java:1508 Dilithium = crystal(8317, ..., SET_DIAMOND, 153, 255, 255, 127, CRYSTALLISABLE, QUARTZ)
		assertEquals("Dilithium", MT.Dilithium.mNameInternal);
		assertTrue(MT.Dilithium.mTextureSetsBlock.contains("DIAMOND"));
		assertTrue(MT.Dilithium.contains(TD.Processing.CRYSTALLISABLE));
		assertTrue(MT.Dilithium.contains(TD.Properties.QUARTZ));
		// upstream MT.java:1832 AncientDebris = metalore(8744, "Ancient Debris", ..., "Ancient", ..., MOLTEN, ...).heat(MeteoricIron)
		assertEquals("AncientDebris", MT.AncientDebris.mNameInternal);
		assertEquals("Ancient Debris", MT.AncientDebris.mNameLocal);
		// MeteoricIron = heat(Fe.mp+200, Fe.boil+200) (upstream :1737, Fe :414 1811/3134) — the copy lands on Ad
		assertEquals(MT.MeteoricIron.mMeltingPoint, MT.AncientDebris.mMeltingPoint);
		assertEquals(2011, MT.AncientDebris.mMeltingPoint);
		// the identical-name face: the put("Ancient") argument registers the alias
		// (addIdenticalNames → the re-registration map entry)
		assertTrue(MaterialRegistry.INSTANCE.MATERIAL_MAP.containsKey("Ancient"));
	}
}
