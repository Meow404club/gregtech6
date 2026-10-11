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
 * <p>The second pin (debt sweep 2026-10-11, the S16 finding): the COMMITTED generated
 * {@code assets/gt6/lang/zh_cn.json} — runData's product — must carry every non-skipped
 * direct row verbatim. The regen pin covers tsv↔py and the parity test covers
 * tsv↔provider face, but the generated file itself had no pin, so the anvil rename
 * (eab8b47412: 砧/锅炉) shipped while the committed face still printed 铁砧/锅炉箱
 * until the next datagen card regenerated it. Every direct hand row sits in the walk,
 * which subsumes the four keys the drift was caught on (anvil, anvil_bend,
 * boiler.display, boiler.strong).
 *
 * <p>Dump discovery: the upstream 1.7.10 zh lang dump lives in the MAIN worktree's
 * {@code tmp/gregtech.lang} (untracked, too big to commit). Resolved via the git common
 * dir, overridable with the {@code gt6.lang.dump} system property; absent dump = a
 * skipped (not failed) pin, the ratchet stays the always-on guard. The generated-file
 * pin needs no dump — the shared generated tree rides the test classpath.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

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

	/**
	 * The committed runData product must match the tsv's direct band (the S16 debt pin):
	 * every non-skipped direct row (the emitted hand layer) sits verbatim in the generated
	 * {@code zh_cn.json}; a diff means the committed generated face is stale — rerun
	 * runData. review-status rows are the provider's deliberate skips (the en fallback
	 * chain) and legitimately absent.
	 */
	@Test
	public void theGeneratedJsonCarriesEveryDirectHandRow() throws Exception {
		JsonObject tGenerated;
		try (InputStream tStream = getClass().getClassLoader()
				.getResourceAsStream("assets/gt6/lang/zh_cn.json")) {
			assertNotNull(tStream, "the committed generated zh_cn.json must be on the classpath");
			tGenerated = com.google.gson.JsonParser.parseReader(
					new InputStreamReader(tStream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
		List<String> tMissing = new ArrayList<>();
		List<String> tStale = new ArrayList<>();
		int tWalked = 0;
		for (var tRow : GT6ZhCn.loadReference().get("direct").entrySet()) {
			if (tRow.getValue().skipped()) continue;
			tWalked++;
			String tFace = tGenerated.has(tRow.getKey()) && tGenerated.get(tRow.getKey()).isJsonPrimitive()
					? tGenerated.get(tRow.getKey()).getAsString() : null;
			if (tFace == null) tMissing.add(tRow.getKey());
			else if (!tFace.equals(tRow.getValue().value()))
				tStale.add(tRow.getKey() + ": generated=" + tFace + " tsv=" + tRow.getValue().value());
		}
		assertTrue(tWalked > 0, "the direct band must not parse empty");
		assertTrue(tMissing.isEmpty(), "generated zh_cn.json missing direct rows (stale runData product?): " + tMissing);
		assertTrue(tStale.isEmpty(), "generated zh_cn.json drifted from the tsv — rerun runData: " + tStale);
	}
}
