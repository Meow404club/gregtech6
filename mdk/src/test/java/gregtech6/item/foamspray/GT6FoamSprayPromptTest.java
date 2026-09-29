package gregtech6.item.foamspray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.item.spraycan.GTSprayCanItem;

/**
 * The issue #42 prompt wires, pinned offline (the static seam face — the mod-Item wall
 * bars constructing {@link GT6FoamSprayItem} in the bootstrapped test JVM):
 * <ul>
 * <li>the mode-cycle prompt rides the ACTIONBAR (overlay=TRUE — the inventory-top strip,
 * the chat line freed) carrying the mode's {@code TranslatableContents} key (the upstream
 * :193-197 literals moved to lang);</li>
 * <li>the tooltip colour name rides the VANILLA {@code color.minecraft.<id>} key — the
 * GT6 DYE_IDS are the 16 vanilla DyeColor serial names verbatim, so zero new lang keys.</li>
 * </ul>
 * The recording Player double is the GT6AnvilBlockEntityTest.BagPlayer Unsafe-allocation
 * form (the base {@code Player.displayClientMessage} is a NO-OP body — Player.java:1385 —
 * so the override holds the exact wire).
 */
public class GT6FoamSprayPromptTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the component seams need no registries.
		}
	}

	/** The recording Player double (no fields injected — the prompt wire touches none). */
	public static final class PromptPlayer extends Player {
		Component tMessage;
		boolean tOverlay;

		private PromptPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

		static PromptPlayer alloc() {
			try {
				java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tTheUnsafe.setAccessible(true);
				return (PromptPlayer)((sun.misc.Unsafe)tTheUnsafe.get(null)).allocateInstance(PromptPlayer.class);
			} catch (ReflectiveOperationException aE) {
				throw new IllegalStateException("the offline prompt player failed", aE);
			}
		}

		@Override public void displayClientMessage(Component aMessage, boolean aOverlay) {
			tMessage = aMessage;
			tOverlay = aOverlay;
		}

		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
	}

	// ------------------------------------------------------------------ the mode prompt wire

	@Test
	void modePromptRidesTheActionbarWithTheTranslatableKey() {
		assertEquals(5, GT6FoamSprayItem.MODE_KEYS.length, "the plain-can cycle 0-4 (upstream :191 mOwned ? 3 : 5)");
		for (int i = 0; i < 5; i++) {
			PromptPlayer tPlayer = PromptPlayer.alloc();
			GT6FoamSprayItem.showModePrompt(tPlayer, i);
			assertTrue(tPlayer.tOverlay, "the actionbar carrier (issue #42 — no chat-line occupancy)");
			assertTrue(tPlayer.tMessage.getContents() instanceof TranslatableContents,
					"the prompt is a translatable (issue #42 — no bare literal)");
			assertEquals(GT6FoamSprayItem.MODE_KEYS[i], ((TranslatableContents)tPlayer.tMessage.getContents()).getKey(),
					"mode " + i + " rides its lang key");
		}
	}

	/** The floorMod guard: a hand-tampered {@code gt.mode} NBT cannot throw the prompt seam. */
	@Test
	void tamperedModeValuesStayInRange() {
		PromptPlayer tPlayer = PromptPlayer.alloc();
		GT6FoamSprayItem.showModePrompt(tPlayer, -1);
		assertEquals(GT6FoamSprayItem.MODE_KEYS[4], ((TranslatableContents)tPlayer.tMessage.getContents()).getKey());
		GT6FoamSprayItem.showModePrompt(tPlayer, 7);
		assertEquals(GT6FoamSprayItem.MODE_KEYS[2], ((TranslatableContents)tPlayer.tMessage.getContents()).getKey());
	}

	/** The owned ceiling (upstream :191 {@code mOwned ? 3 : 5}): cycle 0-2 stays on the block modes. */
	@Test
	void ownedCycleCeilingStaysWithinTheBlockModes() {
		for (int i = 0; i <= 2; i++) {
			assertFalse(GT6FoamSprayItem.MODE_KEYS[i].endsWith("slab") || GT6FoamSprayItem.MODE_KEYS[i].contains("slab"),
					"owned mode " + i + " is a block mode (the slab keys ride 3-4)");
		}
		assertTrue(GT6FoamSprayItem.MODE_KEYS[3].contains("slab") && GT6FoamSprayItem.MODE_KEYS[4].contains("slab"),
				"the slab keys are 3-4");
	}

	// ------------------------------------------------------------------ the colour name wire

	@Test
	void colourNamesRideTheVanillaColorKeys() {
		for (int i = 0; i < 16; i++) {
			Component tName = GT6FoamSprayItem.colorName(i);
			assertTrue(tName.getContents() instanceof TranslatableContents, "the colour name is a translatable");
			assertEquals("color.minecraft." + GTSprayCanItem.DYE_IDS[i], ((TranslatableContents)tName.getContents()).getKey(),
					"dye " + i + " rides the vanilla colour key (the MobBucketItem :60 convention)");
		}
	}

	/** The zero-new-keys guarantee: the GT6 DYE_IDS are the vanilla DyeColor serial names verbatim. */
	@Test
	void dyeIdsAreTheVanillaDyeColorSerialNames() {
		assertEquals(16, GTSprayCanItem.DYE_IDS.length);
		Set<String> tVanilla = new HashSet<>();
		for (DyeColor tColor : DyeColor.values()) tVanilla.add(tColor.getSerializedName());
		Set<String> tSeen = new HashSet<>();
		for (String tId : GTSprayCanItem.DYE_IDS) {
			assertTrue(tVanilla.contains(tId), tId + " must be a vanilla DyeColor serial name (the color.minecraft.<id> key exists)");
			assertTrue(tSeen.add(tId), tId + " is distinct (the 16-key mapping stays total)");
		}
	}
}
