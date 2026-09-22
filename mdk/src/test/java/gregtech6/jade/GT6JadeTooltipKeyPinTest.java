package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The keyed-tooltip ratchet (task p34-hygiene-lang acceptance ③): the two Jade providers
 * this card keyed must never grow a bare {@code Component.literal} again — every fixed
 * word rides a {@code gt6.jade.*} translatable face (en datagen row + tsv hand row, both
 * locales), and dynamic content moves through the %s slots. The scan strips comments
 * first (the javadocs cite the retired literal band) and pins EXACTLY the keyed sources:
 * {@link GT6CrucibleProvider} keeps its documented literal carve-outs (the dynamic
 * name/amount composition of contentLine) — widen the pin consciously when that band
 * keys too (the same shape as the ZH_KEY_FLOOR ratchet: it only ever tightens).
 *
 * <p>Source-level pin on purpose: the tooltip bodies are live-only offline (ITooltip needs
 * a client element helper — the GT6FluidProviderTest posture), so the behavioral seam pins
 * live in the provider tests (GT6MachineProviderTest.tooltipLinesAreKeyedTranslatables*,
 * GT6FluidProviderTest.groupTitlesAreKeyedTranslatablesNotLiterals) and THIS pin guards
 * the surface against regressions those targeted tests can never see.
 */
public class GT6JadeTooltipKeyPinTest {

	/** The keyed providers — source paths relative to the mdk root. */
	private static final List<String> PINNED_SOURCES = List.of(
			"src/main/java/gregtech6/jade/GT6MachineProvider.java",
			"src/main/java/gregtech6/jade/GT6FluidProvider.java");

	@Test
	public void keyedProvidersCarryZeroBareLiterals() throws IOException {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		for (String tRelative : PINNED_SOURCES) {
			String tSource = Files.readString(tMdk.resolve(tRelative));
			String tCode = stripComments(tSource);
			assertFalse(tCode.contains("Component.literal"),
					tRelative + " grew a bare Component.literal — route the face through a "
							+ "gt6.jade.* translatable key (en datagen row + tsv hand row) and move "
							+ "the dynamic part into a %s slot; a genuinely new literal carve-out "
							+ "must widen this pin consciously (the task p34-hygiene-lang ratchet)");
			assertTrue(tCode.contains("Component.translatable"),
					tRelative + " lost its translatable face — the keyed contract is broken");
		}
	}

	/** Drops block then line comments so the javadoc citations of the retired band don't trip the scan. */
	private static String stripComments(String aSource) {
		return aSource.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
	}

	/** Climb from the working directory to the mdk root (the GTEntityBlockRenderShapeCensusTest posture). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/jade/GT6MachineProvider.java"))) {
				return tDir;
			}
		}
		return null;
	}
}
