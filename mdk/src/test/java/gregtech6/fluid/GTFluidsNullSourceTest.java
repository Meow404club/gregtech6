package gregtech6.fluid;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The driver-gated shell consumption pins (known_bugs client-crash-2026-10-02, the P0
 * bare-install load crash): an mdh-3 ABSENT-domain row is a declared-unregistered shell
 — the registerFluidFamily {@code GTFluids.java:692} / specFluid {@code :2419} shape,
 * {@code source}/{@code flowing} left null — and every consumption face must answer
 * null (the OrNull contract) instead of the live crash NPE "because tFamily.source is
 * null" (crash stack ① specSourceOrNull:2769 via GT6RecipesBees.resolveFluid, ② the
 * onCommonSetup CHEMICALS smoke line :3140).
 *
 * <p>The shell rows are built offline with the exact production shell form (the
 * package-private four-arg family constructors the registration cores call) — no FML,
 * no driver seam, no clinit ordering. The onCommonSetup continue-guards are test-exempt:
 * a real FML smoke enqueue is unreachable offline (declared in known_bugs).
 */
public class GTFluidsNullSourceTest extends GTOfflineTestBase {

	/** The hydrogenperoxide row values (CHEMICAL_SPECS, the 3d0edc19e mdh-3 gated row) under a synthetic absent domain. */
	@Test
	public void gatedChemicalShellWalksToNullNotNpe() {
		GTFluids.ChemicalFluidSpec tSpec = new GTFluids.ChemicalFluidSpec(
				"hydrogenperoxide", "Hydrogen Peroxide", 300, 1000, 1000, 0xFF1414FF, false, 0, "gt6test.absent");
		GTFluids.ChemicalFluid tShell = new GTFluids.ChemicalFluid(tSpec, null, null, null); // the :2419 shell shape verbatim
		assertNull(GTFluids.specSourceOrNull("hydrogenperoxide", List.of(tShell)),
				"a driver-gated chemical row answers null (fluid absent), never an NPE");
	}

	/** The aqua arm of the same contract (the :692 shell shape, the aquaSourceOrNull seam). */
	@Test
	public void gatedAquaShellWalksToNullNotNpe() {
		GTFluids.AquaFluidSpec tSpec = new GTFluids.AquaFluidSpec("gated_aqua", "Gated Aqua", 300, 1000, 1000, 0xFF112233, "gt6test.absent");
		GTFluids.AquaFluid tShell = new GTFluids.AquaFluid(tSpec, null, null, null); // the :692 shell shape verbatim
		assertNull(GTFluids.aquaSourceOrNull("gated_aqua", List.of(tShell)),
				"a driver-gated aqua row answers null (fluid absent), never an NPE");
	}

	/** The full {@link GTFluids#liveFluidSource} walk answers null — not an NPE — for a name no table carries (the crash-① consumer face, GT6RecipesBees.resolveFluid). */
	@Test
	public void liveFluidSourceAbsentNameStaysNull() {
		assertNull(GTFluids.liveFluidSource("no_such_fluid_anywhere"));
	}
}
