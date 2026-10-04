package gregtech6.easter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.items.tools.GT6Prospector;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * The chat-domain April-Fools face (task easter-prospector-chat-line, the
 * easter-tooltip-rename leftover (1)): the two GT6Prospector answer rows read the material
 * word LIVE (the :385 getLocalName + the :430 getLocal) but never consulted the S2 flag
 * seam — the renames could stay unlanded when a player's first prospect is that read.
 * The rows are deliberately NOT localized (the ToolCompat.java:400-401 ruling, kept
 * verbatim), so the fix keeps the String face and consults the seam
 * ({@code MaterialPrefixItem.materialWord}, the ensureFoolsApplied + live-word consult):
 * flag on renders the fool word ("Irun", GT_API.java:394), flag off stays byte-identical
 * to the pre-card rows (the ProspectorTest composition pins are the zero-change wall;
 * this class re-pins them so the pair reads standalone).
 *
 * <p>Hygiene: the renames mutate {@code mNameLocal} in place — the GT6CalendarsTest
 * snapshot/restore bracket wraps every test.
 */
public class GT6ProspectorFoolRenameTest {

	private static final Map<OreDictMaterial, String> SNAPSHOT = new HashMap<>();

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GT6MaterialTestSupport.materials();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values())
			SNAPSHOT.put(tMaterial, tMaterial.mNameLocal);
	}

	@AfterEach
	public void restore() {
		GT6Calendars.resetForTest();
		for (Map.Entry<OreDictMaterial, String> tEntry : SNAPSHOT.entrySet())
			tEntry.getKey().setLocal(tEntry.getValue());
		boolean[] tFlags = GT6Calendars.computeFlags(System.currentTimeMillis());
		GT6Calendars.APRIL_FOOLS = tFlags[0];
		GT6Calendars.WOODMANS_BDAY = tFlags[1];
		GT6Calendars.XMAS_IN_JULY = tFlags[2];
		GT6Calendars.XMAS_IN_DECEMBER = tFlags[3];
	}

	/** Flag on WITHOUT a prior consult — the message rows themselves must land the table. */
	private static void foolsOnBare() {
		GT6Calendars.APRIL_FOOLS = true;
	}

	@Test
	public void foolOnChatLinesPinTheFoolWordOnFirstConsult() {
		foolsOnBare();
		// the :385 ore arm — "Stone Iron Ore!" reads "Stone Irun Ore!" (GT_API.java:394)
		assertEquals("Stone Irun Ore!", GT6Prospector.oreMessage(OP.oreVanillastone, MT.Fe),
				"the ore-arm chat line must ride the seam");
		// the :430 trace arm — "Found traces of Iron" reads "Found traces of Irun"
		assertEquals("Found traces of Irun", GT6Prospector.traceMessage(MT.Fe),
				"the trace chat line must ride the seam");
		// the bare-flag posture: the consult itself landed the rename table (the census pin)
		assertTrue(GT6Calendars.sFoolRenames >= 112, "the chat read must have landed the rename table");
	}

	@Test
	public void flagsOffLeavesTheChatLinesByteIdentical() {
		// the zero-change pin: the exact pre-card rows (the ProspectorTest composition wall)
		assertEquals("Found traces of Iron", GT6Prospector.traceMessage(MT.Fe));
		assertEquals("Small Copper Ore!", GT6Prospector.oreMessage(OP.oreSmall, MT.Cu));
		assertEquals("Stone Iron Ore!", GT6Prospector.oreMessage(OP.oreVanillastone, MT.Fe));
	}
}
