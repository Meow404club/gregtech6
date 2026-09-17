package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The QU-matter fluid family offline tests (task p31-qu-a-foundation acceptance — the
 * registration-row assertions against the DECLARED values, the GTFluidsHotFamilyTest
 * posture): the three {@link GTFluids.ChemicalFluidSpec} rows of
 * {@link GTFluids#QU_FLUID_SPECS} (Loader_Fluids.java:70/:71 the matter pair, :193 the
 * GT6-owned FL.Ender) plus the REGISTRATION-FACE id assertions — lesson id686: a green
 * cleanTest does not prove a registration alive, so the DeferredRegister entries faces
 * are pinned here (the ids every downstream row and compat card references), along with
 * the Ender_TE conditional-mount ruling (no "ender" alias fluid — the mount condition
 * stays FALSE in a port without Thermal Expansion, and the two concentration domains
 * 144/250 mB-per-unit must never collapse into one fluid).
 */
public class GTQuFluidsFamilyTest {

	/** The three ids in declaration order — the upstream Loader_Fluids.java block order. */
	private static final List<String> QU_IDS = List.of("chargedmatter", "neutralmatter", "enderpearl_molten");

	@Test
	public void theTableCarriesThreeRowsInDeclarationOrder() {
		assertEquals(QU_IDS, GTFluids.QU_FLUID_SPECS.stream().map(GTFluids.ChemicalFluidSpec::name).toList());
		assertEquals(QU_IDS.size(), GTFluids.QU_FLUIDS.size(), "the live registrations walk the same table");
	}

	/** Acceptance: the per-fluid QU census — id / temperature / density / viscosity / luminosity / liquid carrier. */
	@Test
	public void declaredValuesMatchTheUpstreamAnchors() {
		// {id, tempK, density, viscosity, lum} — the FL.create literals
		Object[][] tCensus = {
			{"chargedmatter"     ,    1, -5000, 1000, 15}, // :70 the 6-arg create (temp 1 K, amount-per-unit 1 = 1 mB/proton) + setDensity(-5000) + setLuminosity(15)
			{"neutralmatter"     ,    1, -5000, 1000, 15}, // :71 the same carriers, 1 mB = 1 neutron
			{"enderpearl_molten" , 2723, 1000, 1000,  5}, // :193 MT.EnderPearl heat(2723, 3785), the :1128 formula over the 1.0 g/cm³ default, setLuminosity(5)
		};
		for (Object[] tRow : tCensus) {
			GTFluids.ChemicalFluidSpec tSpec = GTFluids.quSpec((String) tRow[0]);
			assertNotNull(tSpec, (String) tRow[0]);
			assertEquals(tRow[1], tSpec.temperature(), tSpec.name() + ": the FL.create temperature literal");
			assertEquals(tRow[2], tSpec.density(), tSpec.name() + ": the setDensity literal (matter) / the :1128 formula (enderpearl)");
			assertEquals(tRow[3], tSpec.viscosity(), tSpec.name() + ": the STATE_LIQUID viscosity (FL.java:1104)");
			assertEquals(tRow[4], tSpec.luminosity(), tSpec.name() + ": the setLuminosity literal");
			assertTrue(!tSpec.gas(), tSpec.name() + ": every row is a STATE_LIQUID carrier");
		}
		// the display names verbatim (the en_us lang walk reads these)
		assertEquals("Charged Matter", GTFluids.quSpec("chargedmatter").displayName(), ":70 verbatim");
		assertEquals("Neutral Matter", GTFluids.quSpec("neutralmatter").displayName(), ":71 verbatim");
		assertEquals("Molten Enderpearls", GTFluids.quSpec("enderpearl_molten").displayName(), ":193 verbatim");
	}

	/**
	 * The registration-face id assertions (lesson id686): every QU fluid family lives in
	 * the FLUID_TYPES/FLUIDS DeferredRegister entries with source + flowing, the ids the
	 * recipe rows and the compat seam reference. Offline the holder id face is the
	 * assertion surface (the GT6ToolsCreativeTabTest posture — the id is set at register()
	 * creation time, read via {@code getId()} without resolving); the live FML fire rides
	 * the same shared onModConstruct registers.
	 */
	@Test
	public void theRegistrationFacesCarryTheSixFluidIdsAndThreeTypes() {
		Set<String> tFluidIds = GTFluids.FLUIDS.getEntries().stream()
				.map(entry -> entry.getId().getPath())
				.collect(Collectors.toSet());
		for (String tId : QU_IDS) {
			assertTrue(tFluidIds.contains(tId), tId + ": the source id rides the FLUIDS register face");
			assertTrue(tFluidIds.contains(tId + "_flowing"), tId + "_flowing: the flowing id rides the FLUIDS register face");
		}
		Set<String> tTypeIds = GTFluids.FLUID_TYPES.getEntries().stream()
				.map(entry -> entry.getId().getPath())
				.collect(Collectors.toSet());
		for (String tId : QU_IDS) {
			assertTrue(tTypeIds.contains(tId), tId + ": the FluidType id rides the FLUID_TYPES register face");
		}
	}

	/**
	 * The Ender_TE conditional-mount ruling: the port registers NO "ender" alias — the
	 * external TE fluid name stays absent so {@code FL.Ender_TE.exists()} semantics remain
	 * FALSE (the tag(1) rows of Loader_Recipes_Other.java:897-914 ride unmounted, exactly
	 * upstream-without-TE) and the 144-per-unit (molten.enderpearl) vs 250-per-unit (ender)
	 * concentration domains never collapse.
	 */
	@Test
	public void theEnderTeAliasStaysUnregistered() {
		Set<String> tFluidIds = GTFluids.FLUIDS.getEntries().stream()
				.map(entry -> entry.getId().getPath())
				.collect(Collectors.toSet());
		assertTrue(!tFluidIds.contains("ender"), "no 'ender' alias: the Ender_TE mount condition stays FALSE in the port");
	}
}
