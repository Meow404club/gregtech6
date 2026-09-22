package gregtech6.items.bees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The bumblebee sting tables (task p34-bumbliary-gui, MultiItemBumbles.java:440-456
 * bumbleAttack): the {@code :448-455} damage ladder and the {@code :443-447}/per-case
 * target immunity gate as pure data — the hurt-call half runs only in the live walk.
 */
class GT6BumbleItemStingTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	// ------------------------------------------------ the :448-455 damage ladder

	@Test
	void stingDamageFollowsTheFamilyLadder() {
		// the (1+tier) base over the families, one tier probed per arm
		assertEquals(1, GT6BumbleItem.stingDamage(0), "family 0 tier 0 — the :449 default");
		assertEquals(2, GT6BumbleItem.stingDamage(10), "family 0 tier 1");
		assertEquals(2 * 3, GT6BumbleItem.stingDamage(920), "the :450 family-9 double of the base 3");
		assertEquals(4 * 4, GT6BumbleItem.stingDamage(630), "the :451 family-6 quadruple of the base 4");
		assertEquals(2 * 4, GT6BumbleItem.stingDamage(330), "the :453 nether double of the base 4");
		assertEquals(10 * 4, GT6BumbleItem.stingDamage(10530), "the :454 military ×10");
		assertEquals(10 * 1, GT6BumbleItem.stingDamage(20000), "the :454 pyro ×10 of the base 1");
		assertEquals(10 * 2, GT6BumbleItem.stingDamage(20110), "the :454 cryo ×10");
		assertEquals(10 * 3, GT6BumbleItem.stingDamage(20220), "the :454 aero ×10");
		assertEquals(10 * 4, GT6BumbleItem.stingDamage(20330), "the :454 tera ×10");
	}

	@Test
	void thePassiveFamilyNeverStings() {
		// the :452 family-8 arm
		assertEquals(0, GT6BumbleItem.stingDamage(800));
		assertEquals(0, GT6BumbleItem.stingDamage(830));
		for (boolean tSkeleton : new boolean[] {true, false})
			for (boolean tSnow : new boolean[] {true, false})
				for (boolean tIron : new boolean[] {true, false})
					for (boolean tPlayer : new boolean[] {true, false})
						assertFalse(GT6BumbleItem.stingGate(8, tSkeleton, tSnow, tIron, tPlayer), "family 8 has no sting at all");
	}

	// ------------------------------------------------ the immunity gate

	@Test
	void theStandardGateBlocksBoneAndSteel() {
		// :449-451 — skeleton (and its horse), snow golem, iron golem
		assertFalse(GT6BumbleItem.stingGate(0, true, false, false, false), "skeleton");
		assertFalse(GT6BumbleItem.stingGate(0, false, true, false, false), "snow golem");
		assertFalse(GT6BumbleItem.stingGate(0, false, false, true, false), "iron golem");
		assertTrue(GT6BumbleItem.stingGate(0, false, false, false, true), "players take the standard sting");
		assertTrue(GT6BumbleItem.stingGate(0, false, false, false, false), "everything else takes it");
		// the :450/:451 elite families share the standard gate
		assertFalse(GT6BumbleItem.stingGate(9, true, false, false, false));
		assertFalse(GT6BumbleItem.stingGate(6, false, true, false, false));
	}

	@Test
	void theNetherFamilyMeltFaceSparesOnlySkeletonAndGolem() {
		// :453 — NO snow-golem immunity there (fire melts it), no player carve-out
		assertFalse(GT6BumbleItem.stingGate(3, true, false, false, false), "skeleton");
		assertFalse(GT6BumbleItem.stingGate(3, false, false, true, false), "iron golem");
		assertTrue(GT6BumbleItem.stingGate(3, false, true, false, false), "the snow golem MELTS (upstream drops it from the gate)");
		assertTrue(GT6BumbleItem.stingGate(3, false, false, false, true), "players take the nether sting");
	}

	@Test
	void theElementalEliteQuartetSparesPlayers() {
		// :454 — 105/200/201/202/203 vs players
		for (int tFamily : new int[] {105, 200, 201, 202, 203}) {
			assertFalse(GT6BumbleItem.stingGate(tFamily, false, false, false, true), "players spared from family " + tFamily);
			assertTrue(GT6BumbleItem.stingGate(tFamily, true, true, true, false), "mobs take family " + tFamily);
		}
	}
}
