/**
 * The composed-display-name pin test for task p20-i18n-compose-wires (the B-wave lang
 * ruling, ADR 2026-09-06-p20-i18n-zhcn-pipeline §1.4): the wire family's display names are
 * RUNTIME compositions over the position-param templates — never pre-installed per-variant
 * strings again. Pins sample specs -> expected Component shape (template key + argument
 * slots) through the pure {@link GTWireBlock#displayNameOf} seam (the GTWireTint posture:
 * no block instance is ever constructed — a bootstrapped JVM freezes the registries, and
 * the getName/getName(ItemStack) overrides are one-line delegations onto this seam).
 */
package gregtech6.block.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Variant;

public class GTWireDisplayNameTest {

	@BeforeAll
	public static void boot() {
		// the compose walks the material registry (GTWireSpecs suppliers) — the parity-test
		// boot shape; offline-expected throwables swallowed
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		GTMaterialItems.initMaterials();
	}

	// ---------------------------------------------------------------------------
	// sample-spec pins: spec -> template key + argument slots
	// ---------------------------------------------------------------------------

	@Test
	public void electricSpecComposesSizeMaterialForm() {
		// "1x Tin Wire" -> gt6.wire.display(1, gt6.material.tin, gt6.wire.form.wire)
		TranslatableContents tName = contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tin", 1, false)));
		assertEquals(GTWireBlock.DISPLAY_KEY, tName.getKey());
		Object[] tArgs = tName.getArgs();
		assertEquals(3, tArgs.length, "size + material + form slots");
		assertEquals(1, ((Integer)tArgs[0]).intValue(), "slot 0 = the size numeral (the 'x' lives in the template)");
		assertSmallUnit(tArgs[1], "gt6.material.tin", "slot 1 = the material small unit (A-wave zh face rides it)");
		assertSmallUnit(tArgs[2], GTWireBlock.FORM_WIRE_KEY, "slot 2 = the form unit");
		// the insulated form flips ONLY the form slot: "12x Tin Cable"
		TranslatableContents tCable = contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tin", 12, true)));
		assertEquals(GTWireBlock.DISPLAY_KEY, tCable.getKey());
		assertEquals(12, ((Integer)tCable.getArgs()[0]).intValue());
		assertSmallUnit(tCable.getArgs()[1], "gt6.material.tin", "the material slot is form-invariant");
		assertSmallUnit(tCable.getArgs()[2], GTWireBlock.FORM_CABLE_KEY, "insulated -> the Cable form unit");
		// the top of the ladder composes the same way
		assertEquals(16, ((Integer)contents(GTWireBlock.displayNameOf(GTWireSpecs.find("tin", 16, false))).getArgs()[0]).intValue());
	}

	@Test
	public void redstoneSpecComposesWithoutTheSizeSlot() {
		// "Red Alloy Wire" -> gt6.wire.display.plain(gt6.material.red_alloy, gt6.wire.form.wire)
		// — upstream shows NO multiplier on the family (Loader:1893-1902)
		TranslatableContents tWire = contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("red_alloy", false)));
		assertEquals(GTWireBlock.DISPLAY_PLAIN_KEY, tWire.getKey());
		assertEquals(2, tWire.getArgs().length, "material + form slots, NO size slot");
		assertSmallUnit(tWire.getArgs()[0], "gt6.material.red_alloy", "slot 0 = the material small unit");
		assertSmallUnit(tWire.getArgs()[1], GTWireBlock.FORM_WIRE_KEY, "slot 1 = the form unit");
		// the cable form, same template
		assertSmallUnit(contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("signalum", true))).getArgs()[1],
				GTWireBlock.FORM_CABLE_KEY, "insulated -> the Cable form unit");
		// the luminous bare Lumium wire is the WIRELAMP (Loader:1900)
		TranslatableContents tLamp = contents(GTWireBlock.displayNameOf(GTWireSpecs.findRedstone("lumium", false)));
		assertEquals(GTWireBlock.DISPLAY_PLAIN_KEY, tLamp.getKey());
		assertSmallUnit(tLamp.getArgs()[1], GTWireBlock.FORM_WIRELAMP_KEY, "luminous row, bare form -> the Wirelamp unit");
	}

	@Test
	public void namelessFormsStayAtomic() {
		// the laser family: material-less (NBT_MATERIAL MT.NULL) — no composition applies,
		// the descriptionId key (block.gt6.wire_laser) is the name face
		for (Variant tVariant : GTWireSpecs.laserVariants()) {
			assertNull(GTWireBlock.displayNameOf(tVariant), "laser stays atomic (the arch card ruling)");
		}
		// the legacy-pair SHAPE (material-less ELECTRIC, the two p7 blocks) — the other
		// atomic gate; the direct static call needs no block instance
		assertNull(GTWireBlock.displayNameOf(null, GTWireSpecs.Row.Family.ELECTRIC, 1, false, false));
	}

	@Test
	public void everyComposedVariantHasItsSlots() {
		// the 620 electric + 6 redstone all compose; the material slot key exists on the en
		// face exactly when mNameLocal is non-null (the GT6EnUs.addMaterialNames contract),
		// so a null mNameLocal would render the RAW key in game — pinned absent
		List<Variant> tFamilies = GTWireSpecs.variants();
		tFamilies.addAll(GTWireSpecs.redstoneVariants());
		assertEquals(626, tFamilies.size(), "the composed domain = 620 electric + 6 redstone");
		for (Variant tVariant : tFamilies) {
			String tPath = GTWireSpecs.registryName(tVariant);
			TranslatableContents tName = contents(GTWireBlock.displayNameOf(tVariant));
			assertNotNull(tVariant.row().material().get().mNameLocal, "display-source material must have a local name: " + tPath);
			if (tVariant.row().family() == GTWireSpecs.Row.Family.REDSTONE) {
				assertEquals(GTWireBlock.DISPLAY_PLAIN_KEY, tName.getKey(), tPath);
				assertEquals(2, tName.getArgs().length, tPath);
			} else {
				assertEquals(GTWireBlock.DISPLAY_KEY, tName.getKey(), tPath);
				assertEquals(3, tName.getArgs().length, tPath);
				assertSmallUnit(tName.getArgs()[1],
						"gt6.material." + gregtech6.item.MaterialPrefixItem.snakeCase(tVariant.row().material().get().mNameInternal),
						"the material slot rides the single snakeCase derivation: " + tPath);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------

	/** The compose seam: asserts translatable shape and returns the contents (key + args). */
	private static TranslatableContents contents(Component aComponent) {
		assertNotNull(aComponent, "the composed variants never return null");
		assertTrue(aComponent.getContents() instanceof TranslatableContents, "the wire display must be a translatable composition");
		return (TranslatableContents)aComponent.getContents();
	}

	/** The material/form slot: the arg must itself be the translatable small unit with the given key. */
	private static void assertSmallUnit(Object aArg, String aKey, String aMessage) {
		assertTrue(aArg instanceof Component, aMessage + " — the slots are nested translatables");
		assertEquals(aKey, contents((Component)aArg).getKey(), aMessage);
	}
}
