package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		// acceptance ① — the registry faces exist under the flattened MTE 32755 id
		assertEquals(List.of("bumble_hive"),
				GT6BeeHives.BLOCKS.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the block register holds exactly the hive");
		assertEquals(List.of("bumble_hive"),
				GT6BeeHives.BLOCK_ENTITY_TYPES.getEntries().stream().map(tRow -> tRow.getId().getPath()).toList(),
				"the BET register holds exactly the hive type");
	}

	@Test
	void suppliersAreDeferredNotRun() {
		// offline the RegistryObjects are NOT resolved (no game registry): .get() must
		// throw rather than half-construct — the assertion pins the no-supplier-run posture
		assertThrows(Throwable.class, () -> GT6BeeHives.HIVE.get(),
				"unregistered .get() throws (the entries-only offline contract)");
	}

	@Test
	void noItemFaceIsDeclared() {
		// the loot shell is worldgen-only: this home declares NO item register and NO
		// creative tab (the GT6SurfaceBlocks rock/stick form) — any item face needs a card
		assertFalse(hasField("ITEMS"), "no ITEMS DeferredRegister in the hive home");
		assertFalse(hasField("CREATIVE_MODE_TABS"), "no creative tab in the hive home");
	}

	private static boolean hasField(String aName) {
		for (java.lang.reflect.Field tField : GT6BeeHives.class.getDeclaredFields()) {
			if (tField.getName().equals(aName)) return true;
		}
		return false;
	}
}
