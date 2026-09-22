package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The bumble-hive FML REGISTRATION assertion (task p32-bees-lv2 acceptance ①, the
 * id686 lesson: the registration faces are asserted, not assumed) — the
 * GT6SurfaceBlocksTest DeferredRegister-entry posture.
 *
 * <p>Offline-safe by construction: DeferredRegister ENTRIES only (no supplier runs,
 * no RegisterEvent) + the vanilla bootstrap bracket. The class-exactness of the
 * block/BE pair is compile-time (the register lambdas name the concrete classes); the
 * BE behaviour rides the GT6BumbleHiveBlockEntityTest fixture.
 */
class GT6BeeHivesTest {

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@Test
	void hiveBlockAndBetAreRegisteredUnderPinnedIds() {
		// acceptance ① — the registry faces exist under the flattened MTE 32755 id and,
		// since task p33-bees-lv3-b-bumbliary, under the MTE 32741/32007 Bumbliary pair
		assertEquals(List.of("bumble_hive", "bumbliary", "bumbliary_advanced"),
				GT6BeeHives.BLOCKS.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the block register holds the hive + the Bumbliary pair");
		assertEquals(List.of("bumble_hive", "bumbliary", "bumbliary_advanced"),
				GT6BeeHives.BLOCK_ENTITY_TYPES.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the BET register holds the hive type + the two Bumbliary layouts");
	}

	//? if forge {
	@Test
	void suppliersAreDeferredNotRun() {
		// offline the RegistryObjects are NOT resolved (no game registry): .get() must
		// throw rather than half-construct — the assertion pins the no-supplier-run posture.
		// FORGE-LEG ONLY: the 21.1 test JVM boots through FML itself (the GTOfflineTestBase
		// javadoc), so the registry is populated and .get() RESOLVES there.
		assertThrows(Throwable.class, () -> GT6BeeHives.HIVE.get(),
				"unregistered .get() throws (the entries-only offline contract)");
	}
	//?}

	@Test
	void itemFaceIsTheHivePlusTheBumbliaryPair() {
		// the R2 containment-contract revision (task p34-bumbliary-recipes): the hive GAINS
		// its BlockItem (the carryable wild hive — the upstream 32755 item form the 32741
		// recipe keys on, :2222; the GT6BeeHives javadoc declares the broken-once ruling);
		// the Bumbliary pair ARE obtainable machines (task p33-bees-lv3-b-bumbliary).
		// Still no creative tab in the bee home (the hive item rides the gt6:bee tab, the
		// GT6BeeCombs registration).
		assertFalse(hasField("CREATIVE_MODE_TABS"), "no creative tab in the bee home");
		assertEquals(List.of("bumble_hive", "bumbliary", "bumbliary_advanced"),
				GT6BeeHives.ITEMS.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the item register holds the hive BlockItem + the Bumbliary pair");
	}

	private static boolean hasField(String aName) {
		for (java.lang.reflect.Field tField : GT6BeeHives.class.getDeclaredFields()) {
			if (tField.getName().equals(aName)) return true;
		}
		return false;
	}
}
