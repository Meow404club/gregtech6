/**
 * The translation-ledger terminal pin (task lang-hygiene-consolidated, the
 * research.zh-translation-full-audit follow-up): the committed zh_cn_ref.tsv must be
 * EXACTLY what {@code gen_zhcn_ref.py --dump <upstream>} reproduces — byte for byte.
 *
 * <p>The tsv is a pure function of the dump + the py hand tables (ADR §1.1), so a zero
 * regen diff is the whole-storey proof: every family band (material/itemgroup/mte), the
 * FAMILY_OVERRIDES layer, and the direct hand band re-derive identically. The --check
 * ratchet only sees the direct band; this pin closes the other three. One command,
 * one assertion — the audit's "100% 终证" as a test.
 *
 * <p>Dump discovery: the upstream 1.7.10 zh lang dump lives in the MAIN worktree's
 * {@code tmp/gregtech.lang} (untracked, too big to commit). Resolved via the git common
 * dir, overridable with the {@code gt6.lang.dump} system property; absent dump = a
 * skipped (not failed) pin, the ratchet stays the always-on guard.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class GT6ZhRefRegenPinTest {

	private static Path worktreeRoot() throws Exception {
		Process tGit = new ProcessBuilder("git", "rev-parse", "--show-toplevel")
			.directory(Path.of(System.getProperty("user.dir")).toFile())
			.start();
		tGit.waitFor(30, TimeUnit.SECONDS);
		assertEquals(0, tGit.exitValue(), "git rev-parse --show-toplevel must succeed");
		return Path.of(new String(tGit.getInputStream().readAllBytes()).strip());
	}

	private static Path dumpPath() throws Exception {
		String tOverride = System.getProperty("gt6.lang.dump");
		if (tOverride != null) return Path.of(tOverride);
		Process tGit = new ProcessBuilder("git", "rev-parse", "--path-format=absolute", "--git-common-dir")
			.directory(worktreeRoot().toFile())
			.start();
		tGit.waitFor(30, TimeUnit.SECONDS);
		assertEquals(0, tGit.exitValue(), "git rev-parse --git-common-dir must succeed");
		// the common dir is the MAIN checkout's .git — the dump lives one level above it
		Path tMain = Path.of(new String(tGit.getInputStream().readAllBytes()).strip()).getParent();
		return tMain.resolve("tmp").resolve("gregtech.lang");
	}

	@Test
	public void theRegenReproducesTheCommittedTsvByteForByte() throws Exception {
		Path tRoot = worktreeRoot();
		Path tScript = tRoot.resolve("mdk").resolve("tools").resolve("gen_zhcn_ref.py");
		Path tDump = dumpPath();
		Assumptions.assumeTrue(Files.isRegularFile(tDump),
			"upstream dump not found at " + tDump + " (set -Dgt6.lang.dump) — pin skipped, the --check ratchet stays on");
		Assumptions.assumeTrue(Files.isRegularFile(tScript), "gen_zhcn_ref.py not found at " + tScript);

		Path tRegen = Files.createTempFile("gt6-zhcn-regen-pin", ".tsv");
		try {
			Process tPy = new ProcessBuilder("python3", tScript.toString(),
				"--dump", tDump.toString(), "--out", tRegen.toString())
				.directory(tRoot.toFile())
				.redirectErrorStream(true)
				.start();
			tPy.waitFor(120, TimeUnit.SECONDS);
			assertEquals(0, tPy.exitValue(),
				"the regen must exit clean: " + new String(tPy.getInputStream().readAllBytes()));
			byte[] tCommitted = getClass().getResourceAsStream(GT6ZhCn.REFERENCE_RESOURCE).readAllBytes();
			byte[] tFresh = Files.readAllBytes(tRegen);
			assertArrayEquals(tCommitted, tFresh,
				"the regen must reproduce the committed tsv byte for byte — any diff is a hand-layer/dump drift");
		} finally {
			Files.deleteIfExists(tRegen);
		}
	}
}
