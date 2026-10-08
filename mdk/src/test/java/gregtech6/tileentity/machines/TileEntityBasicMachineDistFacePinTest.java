/**
 * The machine-face dist pin (task basicmachine-onremove-distleak). Forensics first, then
 * the guard the pin enforces:
 *
 * <p>The kinetics-be RCON teardown detonated on a dedicated server:
 * {@code RuntimeDistCleaner "Attempted to load class brachy/modularui/screen/ModularScreen
 * for invalid dist DEDICATED_SERVER"} followed by the zombie
 * {@code "Block entity gt6:crusher @ 33,64,4 state Block{minecraft:air} invalid for
 * ticking"} (gt6_rs_session_1201-forge_kinbe-e2e87ecb.log:41215-41216; the neo leg twin
 * carried two). The misattribution correction this pin records: the machine side has NO
 * removal-chain touch point of its own — {@code GTBasicMachineBlock} declares no onRemove
 * override (:42 "No onRemove override (id59)"), the BE never overrides setRemoved, and
 * {@code TileEntityBasicMachine.class} carries zero ModularScreen references. The real
 * site is the SHARED teardown seam: {@code GTEntityBlock.onRemove} → {@code beCanDrop}'s
 * {@code getMethod("canDrop", int.class)} misses on the declared walk (the census: only
 * the ACT declares it), the JVM falls through to the interface-default enumeration, and
 * materialising {@code GT6MuiMachine}'s reflection metadata resolves the un-annotated
 * {@code createScreen} default's {@code ModularScreen} return type. The root fix is the
 * mui-fill-teardown-distleak card's (probeDeclared walk + the {@code @OnlyIn} strip on
 * that default — files deliberately OUTSIDE this card's scope); no guard expressible on
 * the machine side can stop the interface walk, so this card ships the FACE PIN instead.
 *
 * <p>The pin reads RAW classfile bytes off the classpath resource (never the loaded
 * {@code Class} — the neo test JVM runs the FML transformer, which strips
 * {@code @OnlyIn} members for real, so reflection sees a "clean" chain the dedicated
 * world does not; raw bytes are leg-stable, the mui pin's lesson) and asserts the
 * needle {@code brachy/modularui/screen/ModularScreen} (the slash form — a dotted
 * spelling is a false negative, the constant pool stores slashes) appears in NONE of the
 * family's removal-chain classes. That bans even an {@code @OnlyIn}-annotated
 * ACT-style {@code createScreen} override here: the interface default is the sanctioned
 * carrier, a per-BE client-typed member is dead weight, and if one ever lands it fails
 * THIS pin instead of detonating on a live server. The positive control scans
 * {@code ModularScreen.class} itself (needle trivially present — the scan and the needle
 * cannot pass vacuously), and the family check asserts the pinned class really declares
 * its {@code GT6MuiMachine} interface, so the guard cannot go green on a stale artifact.
 */
package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class TileEntityBasicMachineDistFacePinTest {

	/** The client-only class the dist cleaner refuses — the constant-pool slash spelling. */
	static final String NEEDLE = "brachy/modularui/screen/ModularScreen";

	/** The family's removal-chain classes: the BE, its block, and the shared TE bases. */
	static final String[] FAMILY = {
		"gregtech6/tileentity/machines/TileEntityBasicMachine.class",
		"gregtech6/block/GTBasicMachineBlock.class",
		"gregtech6/tileentity/TileEntityBase03TicksAndSync.class",
		"gregtech6/tileentity/TileEntityBase01Root.class",
	};

	static byte[] rawBytes(String aResource) throws IOException {
		InputStream tIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(aResource);
		assertNotNull(tIn, aResource + " not on the test classpath");
		try (InputStream tStream = tIn) {
			return tStream.readAllBytes();
		}
	}

	static boolean containsNeedle(byte[] aBytes) {
		// plain byte-slice scan over the ASCII needle (the constant pool is ASCII here)
		String tText = new String(aBytes, StandardCharsets.ISO_8859_1);
		return tText.contains(NEEDLE);
	}

	@Test
	public void theMachineFamilyCarriesNoModularScreenReference() throws IOException {
		for (String tResource : FAMILY) {
			byte[] tBytes = rawBytes(tResource);
			assertTrue(!containsNeedle(tBytes),
				tResource + " references " + NEEDLE + " — a client-typed member (even an "
					+ "@OnlyIn-annotated one) on the basic-machine family is banned: the "
					+ "GT6MuiMachine.createScreen default is the sanctioned carrier, and the "
					+ "dedicated-server teardown must never resolve this type from the family");
		}
	}

	@Test
	public void thePinDetectorProvesItselfOnTheClientClass() throws IOException {
		// positive control: the scan finds the needle where it genuinely lives — the pins
		// above are a real property, not a vacuous scan of bytes that never mention it.
		byte[] tScreen = rawBytes("brachy/modularui/screen/ModularScreen.class");
		assertTrue(containsNeedle(tScreen),
			"the needle is absent even from ModularScreen.class itself — the pin's scan is broken");
	}

	@Test
	public void thePinnedClassReallyIsTheMuiImplementor() throws IOException {
		// non-vacuity tie: the pinned BE declares the MUI interface, so the family the
		// pin guards is the class the shared teardown reflection actually walks.
		byte[] tMachine = rawBytes("gregtech6/tileentity/machines/TileEntityBasicMachine.class");
		String tText = new String(tMachine, StandardCharsets.ISO_8859_1);
		assertTrue(tText.contains("gregtech6/gui/machines/GT6MuiMachine"),
			"TileEntityBasicMachine no longer declares GT6MuiMachine — the pin's subject moved, re-point the family list");
	}
}
