/**
 * Spot-check pins for task p23-i18n-zh-442-backfill: the acceptance card names four
 * values that must land in the zh face (the oven word per the dump/hand terminal value,
 * a Draconium drum, the diesel dump anchor, and the ×-shaped wire_gt14 template), plus
 * the full-closure count — every en key now has a zh entry (zero translation debt, the
 * parity subset reaching the en cardinality). Lives in its OWN file so the ratchet
 * constant and the material keyface assertions in GT6LangParityTest (task
 * p23-i18n-material-fill-fix's file) stay untouched apart from the floor bump.
 *
 * <p>Same bare-JVM recording posture as GT6LangParityTest.collect: a same-package
 * {@link GT6ZhCn} subclass captures every add() the provider makes.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

public class GT6ZhBackfillSpotCheckTest {

	private static Map<String, String> zhEntries;

	@BeforeAll
	public static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	private static Map<String, String> zh() {
		if (zhEntries == null) {
			zhEntries = new HashMap<>();
			PackOutput tOutput = new PackOutput(Path.of("build", "tmp", "gt6zhcn-spotcheck"));
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					if (zhEntries.put(aKey, aValue) != null) {
						throw new IllegalStateException("Duplicate translation key " + aKey);
					}
				}
			}.addTranslations();
		}
		return zhEntries;
	}

	@Test
	public void theFourCardNamedValuesLand() {
		assertEquals("烤箱", zh().get("block.gt6.oven"), "the oven block display name");
		assertEquals("龙合金鼓", zh().get("block.gt6.barrel_draconium"), "the Draconium drum (dump 龙 word root)");
		assertEquals("柴油", zh().get("fluid.gt6.diesel"), "the diesel dump anchor");
		String tWire14 = zh().get("gt6.tagprefix.wire_gt14");
		assertTrue(tWire14 != null && tWire14.contains("×%s"),
			"wire_gt14 must keep the × template shape, got " + tWire14);
	}

	@Test
	public void theBackfillClosesTheGapExactly() {
		Map<String, String> tEn = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", "gt6zhcn-spotcheck-en"));
		new GT6EnUs(tOutput) {
			@Override
			public void add(String aKey, String aValue) {
				tEn.put(aKey, aValue);
			}
		}.addTranslations();
		long tStillMissing = tEn.keySet().stream().filter(tKey -> !zh().containsKey(tKey)).count();
		assertEquals(0L, tStillMissing,
			"the 442-key backfill must close the zh gap exactly — zero keys may stay missing");
		assertEquals(tEn.size(), zh().size(), "zh key cardinality must equal the en cardinality");
	}
}
