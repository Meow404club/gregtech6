/**
 * Mute guard for task polish-compressed-warn: the prefix-template walk's real-world
 * snake_case collision is exactly the declared first-wins pair compressed/Compressed
 * (declaration pin GTMaterialItemsRegistrationTest.firstWinsIdCollisionRule; id collision
 * policy javadoc in GTMaterialItems) — that one is muted to debug. Any UNdeclared duplicate
 * must still WARN, so the guard records collisions through the package-visible
 * {@link GT6EnUs#logTemplateCollision} seam instead of parsing log output, and replays the
 * walk over a synthetic undeclared duplicate ({@code List.of(OP.ingot, OP.ingot)}).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

public class GT6EnUsTemplateCollisionMuteTest {

	@BeforeAll
	public static void boot() {
		// GT6EnUsJeiInfoTest.boot shape: the walk loads Item/Bootstrap faces and needs the
		// material system (addPrefixTemplates/addMaterialNames); NetworkHooks init failure is
		// expected offline, registries are ready by then.
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		GTMaterialItems.initMaterials();
	}

	/** Records seam collisions and add() entries instead of touching any logger. */
	private static final class RecordingProvider extends GT6EnUs {
		final List<String> collisions = new ArrayList<>();
		final Map<String, String> entries = new HashMap<>();

		RecordingProvider() {
			super(new PackOutput(Path.of("build", "tmp", "gt6enus-mute-test")));
		}

		@Override
		public void add(String aKey, String aValue) {
			if (entries.put(aKey, aValue) != null) {
				throw new IllegalStateException("Duplicate translation key " + aKey);
			}
		}

		@Override
		void logTemplateCollision(String aKey, String aLoserName) {
			collisions.add(aLoserName);
		}
	}

	@Test
	public void realWalkMutesOnlyTheDeclaredCompressedPair() {
		RecordingProvider tProvider = new RecordingProvider();
		tProvider.addPrefixTemplates(OreDictPrefix.VALUES);
		// any other loser here would be a NEW, undeclared collision — it must stay loud
		assertEquals(List.of("Compressed"), tProvider.collisions,
			"the real walk must hit exactly the declared pair, nothing else");
		assertTrue(GT6EnUs.isDeclaredTemplateCollision(tProvider.collisions.get(0)),
			"the real collision must be the declared (muted) one");
		// first-wins untouched: the winner's template still lands exactly once
		assertTrue(tProvider.entries.containsKey("gt6.tagprefix.compressed"));
	}

	@Test
	public void undeclaredDuplicateStillWarns() {
		// the real (non-overridden) seam: "ingot" is undeclared, so this call physically
		// WARNs into this test run's log — the empirical "still warns" evidence
		new GT6EnUs(new PackOutput(Path.of("build", "tmp", "gt6enus-mute-test")))
			.addPrefixTemplates(List.of(OP.ingot, OP.ingot));
		// the recorded replay pins the routing decision that produced that WARN
		RecordingProvider tRecorder = new RecordingProvider();
		tRecorder.addPrefixTemplates(List.of(OP.ingot, OP.ingot));
		assertEquals(List.of("ingot"), tRecorder.collisions,
			"an undeclared duplicate must reach the collision seam");
		assertFalse(GT6EnUs.isDeclaredTemplateCollision("ingot"),
			"undeclared losers must route to the WARN branch");
		assertTrue(tRecorder.entries.containsKey("gt6.tagprefix.ingot"),
			"first-wins keeps the winner's template");
	}

	@Test
	public void declaredPredicateIsExactlyTheCompressedPair() {
		assertTrue(GT6EnUs.isDeclaredTemplateCollision("compressed"), "pair member (lowercase winner)");
		assertTrue(GT6EnUs.isDeclaredTemplateCollision("Compressed"), "pair member (alias loser)");
		for (String tName : new String[] {"ingot", "dust", "plate", "gem", "crushed", "wireGt01"}) {
			assertFalse(GT6EnUs.isDeclaredTemplateCollision(tName), tName + " must never be muted");
		}
	}
}
