/**
 * Tests for task mdh-1-driver-core: the unified mod-driver core — the injection seam
 * contract (ADR-MDH3), the all-PRESENT default (ADR-MDH1), and the gate mechanism proven on a
 * fixture domain (no real domain data written; production lookup stays the mdh-2 atlas slot).
 * Since ruling id1745-4 (task material-foreign-domain-registration) the ITEM face is
 * pin-invariant — the gate's live consumers are the fluid/block/walk faces, so the fixture
 * arm observes the gate itself, not the item universe.
 *
 * <p>The offline JVM has no FML, so {@link GT6ModDrivers#seedFromEnvironment()} must be a
 * no-op here (forge leg: ModList == null; neo leg: FML boots but the SEEDED_DOMAINS set is
 * empty — both legs write nothing, ADR-MDH1).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

public class GT6ModDriversTest {

    /** A modid no real content is attributed to — the fixture domain of acceptance 3. */
    private static final String FIXTURE_DOMAIN = "gt6test.driver_fixture";
    /** The fixture material family: exact internal name, one merge target in the registry. */
    private static final String FIXTURE_MATERIAL = "Iron";

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
        GT6DriverTestSupport.loadFamiliesPristine(); // mdh-6: freeze the gated family lists before any pin
    }

    @AfterEach
    public void restorePristineDriver() {
        GT6ModDrivers.reset();
    }

    private static OreDictMaterial findMaterial(String aInternalName) {
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial != null && tMaterial.mID >= 0 && tMaterial.mNameInternal.equals(aInternalName)) return tMaterial;
        }
        throw new AssertionError("fixture material not registered: " + aInternalName);
    }

    private static long countMaterial(List<GTMaterialItems.PrefixMaterial> aOrder, String aInternalName) {
        return aOrder.stream().filter(tPair -> tPair.material().mNameInternal.equals(aInternalName)).count();
    }

    @Test
    public void defaultsAreAllPresent() {
        assertTrue(GT6ModDrivers.isLoaded("jei"), "unknown domain answers true (ADR-MDH1)");
        assertTrue(GT6ModDrivers.isLoaded("some.uninstalled.mod"), "unknown domain answers true (ADR-MDH1)");
        assertTrue(GT6ModDrivers.isLoaded(null), "unattributed answers true");
        assertTrue(GT6ModDrivers.visibilityGate().test(findMaterial(FIXTURE_MATERIAL)), "default gate is a pass-through");
        assertFalse(GTMaterialItems.registrationOrder().isEmpty(), "the registration universe survives");
    }

    @Test
    public void absentFixtureDomainAnswersTheGateAndTheItemFaceStaysInert() {
        int tBaseline = GTMaterialItems.registrationOrder().size();
        long tIronRows = countMaterial(GTMaterialItems.registrationOrder(), FIXTURE_MATERIAL);
        assertTrue(tBaseline > 0 && tIronRows > 0, "fixture precondition: universe and iron family both populated");

        // Binding alone is inert — attribution without a pin changes nothing (ADR-MDH1).
        GT6ModDrivers.setMaterialDomain(tMaterial -> FIXTURE_MATERIAL.equals(tMaterial.mNameInternal) ? FIXTURE_DOMAIN : null);
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "attribution alone must not change the universe");

        // The PRESENT state of the seam: explicit pin, no change.
        GT6ModDrivers.setDriver(FIXTURE_DOMAIN, GT6ModDrivers.DriverLevel.PRESENT);
        assertTrue(GT6ModDrivers.isVisible(findMaterial(FIXTURE_MATERIAL)), "explicit PRESENT keeps the material visible");

        // The ABSENT state: the GATE answers the pin (the fluid/block faces' truth), while the
        // ITEM face stays inert — ruling id1745-4 (task material-foreign-domain-registration)
        // retired the driver from the item path (upstream PrefixItem generation has no mod axis).
        GT6ModDrivers.setDriver(FIXTURE_DOMAIN, GT6ModDrivers.DriverLevel.ABSENT);
        assertFalse(GT6ModDrivers.visibilityGate().test(findMaterial(FIXTURE_MATERIAL)), "the gate answers the pin");
        assertFalse(GT6ModDrivers.isVisible(findMaterial(FIXTURE_MATERIAL)), "isVisible answers the pin");
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "the item universe is pin-invariant (ruling id1745-4)");
        assertEquals(tIronRows, countMaterial(GTMaterialItems.registrationOrder(), FIXTURE_MATERIAL), "the attributed family keeps every item pair");

        // reset restores the pristine gate (the acceptance's ratchet-proof arm).
        GT6ModDrivers.reset();
        assertTrue(GT6ModDrivers.isVisible(findMaterial(FIXTURE_MATERIAL)), "reset clears the pin");
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "reset restores the full universe");
    }

    @Test
    public void seedFromEnvironmentIsAnOfflineNoOp() {
        int tBaseline = GTMaterialItems.registrationOrder().size();
        GT6ModDrivers.seedFromEnvironment(); // must not throw: forge leg ModList == null, neo leg empty domain set
        assertTrue(GT6ModDrivers.isLoaded("gt6test.never_seeded"), "no seeded state after the offline seed");
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "the offline seed hides nothing");
    }

    /**
     * Activation precondition 1 (mdh-clearout-batch2, review seat XVI): a datagen JVM
     * short-circuits the seed BEFORE any mod-list walk — the runData NPE face (f8ffa0bd1)
     * was exactly a seeded runData JVM, and registration precedes GatherDataEvent so a late
     * reset cannot un-shrink the universe. The probe seam stands in for the platform flag
     * ({@code DatagenModLoader.isRunningDataGen()}, live before any mod constructs on both
     * legs); on the neo junit-fml leg this is a REAL regression pin — a live mod list lacking
     * the foreign domains plus a seeding seed would write ABSENT and shrink the census.
     */
    @Test
    public void datagenJvmShortCircuitsTheSeed() {
        int tBaseline = GTMaterialItems.registrationOrder().size();
        GT6ModDrivers.setDatagenProbe(() -> true);
        GT6ModDrivers.seedFromEnvironment();
        assertTrue(GT6ModDrivers.isLoaded(MT.MD.HaC.mID), "a probe-true (datagen) JVM never seeds: the batch-1 domain stays PRESENT");
        assertTrue(GT6ModDrivers.isLoaded(MT.MD.TiC.mID), "a probe-true (datagen) JVM never seeds: the batch-2 domain stays PRESENT");
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "the datagen seed wrote nothing — the walk universe is untouched");
    }
}
