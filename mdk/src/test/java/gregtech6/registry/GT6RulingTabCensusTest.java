package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import gregtech6.covers.GT6Covers;
import gregtech6.items.GT6UsbSticks;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task p38-tabfix-d-ruling — the user-ruling-pool tab-join coverage census: the rails,
 * portals, USB sticks and the earlier covers join {@link GTMachines#MACHINES_TAB}
 * (gt6:machines), the written books get the dedicated {@link GT6Books#BOOKS_TAB}
 * (gt6:books), and GTEnergySources stays tab-less (the test-rig ruling, zero code).
 * THIS test pins each walked surface's size against the DISK truth, so a future row
 * lands only with a conscious census bump (the BurningBoxRowTableTest row-count
 * posture; sizes read the DeferredRegister-held maps/fields without resolving
 * {@code .get()} — the frozen-registry wall, the GT6MultiblockTabCensusTest form).
 *
 * <p><b>COUNT ERRATA (declared)</b>: the census ledger says UsbSticks 3 and Covers
 * early 3, but (1) the UsbSticks evidence window (:83-89) truncated at USB_STICK_3 —
 * the file registers FOUR sticks (:92 the :794 USB 4.0 row); (2) the Covers evidence
 * window (:115-132) truncated at the conductor-in row — the tab-less face is THIRTY
 * items (the 10 single covers pump/emitter/conductor-in/conductor-out/machine-switch/
 * shutter/item-filter/retriever/auto-machine-switch/controller + the 10 conveyors + the
 * 10 robot arms; the p33 walk javadoc's "EARLIER cover family stays tab-less" covered
 * them all, upstream every one rides the MultiItemTechnological GT tab list). The
 * coordinator rulings (2026-09-24, the p38-tabfix-a Boilers 3→26 precedent): pin the
 * disk truth. Rails 31 and Books 15 match the ledger.
 */
public class GT6RulingTabCensusTest extends GTOfflineTestBase {

	/** Ruling ① — the rails family: the road stripe + the 30 material rows. */
	@Test
	public void theRailsJoinWalksAll31Items() {
		assertEquals(30, GT6Rails.ITEMS_BY_PATH.size(), "3 classes x 10 materials (Loader_Rails.java:41-72)");
		assertEquals(31, 1 + GT6Rails.ITEMS_BY_PATH.size(), "the road stripe (:39) + the 30 rows — the ruling-① join surface");
		assertNotNull(GT6Rails.ROAD_ITEM, "the Road Stripe's own item field — the walk accepts it before the map walk");
	}

	/** Ruling ② — the written books: 15 items on the dedicated tab. */
	@Test
	public void theBooksOwnTheirDedicatedTabWithAll15() {
		assertEquals(15, GT6Books.ITEMS_BY_PATH.size(), "the 15 static books (the gt.books pool minus the 5 CUT rows, the p35 basis)");
		assertNotNull(GT6Books.BOOKS_TAB, "the dedicated tab exists (the user ruling: the manuals should be easy to get)");
		assertEquals("gt6:books", GT6Books.BOOKS_TAB.getId().toString(), "the tab registry id");
	}

	/** Ruling ③ — the earlier covers: 10 singles + 10 conveyors + 10 robot arms = 30. */
	@Test
	public void theEarlierCoversJoinWithAll30Items() {
		assertNotNull(GT6Covers.COVER_PUMP, ":115 (census evidence :115)");
		assertNotNull(GT6Covers.COVER_REDSTONE_EMITTER, ":123 (census evidence :123)");
		assertNotNull(GT6Covers.COVER_REDSTONE_CONDUCTOR_IN, ":132 (census evidence :132)");
		assertNotNull(GT6Covers.COVER_REDSTONE_CONDUCTOR_OUT, ":135 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_REDSTONE_MACHINE_SWITCH, ":145 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_SHUTTER, ":154 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_ITEM_FILTER, ":163 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_ITEM_RETRIEVER, ":174 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_AUTO_REDSTONE_MACHINE_SWITCH, ":292 — the row the census window lost");
		assertNotNull(GT6Covers.COVER_CONTROLLER, ":302 — the row the census window lost");
		assertEquals(10, GT6Covers.COVER_CONVEYORS.size(), "the p11 conveyor timing tiers (CoverConveyor.TIMING_TIERS)");
		assertEquals(10, GT6Covers.COVER_ROBOT_ARMS.size(), "the p11 robot-arm timing tiers");
		assertEquals(30, 10 + GT6Covers.COVER_CONVEYORS.size() + GT6Covers.COVER_ROBOT_ARMS.size(),
				"the ruling-③ join surface — the census '3' truncated its evidence window at the conductor-in row (declared erratum)");
	}

	/** Ruling ④ — the portals (2) and the USB sticks (4, erratum) join the machines tab. */
	@Test
	public void thePortalsAndUsbSticksJoinTheMachinesTab() {
		assertNotNull(GT6Portals.PORTAL_NETHER_ITEM, "upstream :2003");
		assertNotNull(GT6Portals.PORTAL_END_ITEM, "upstream :2004");
		assertNotNull(GT6UsbSticks.USB_STICK_1, ":791");
		assertNotNull(GT6UsbSticks.USB_STICK_2, ":792");
		assertNotNull(GT6UsbSticks.USB_STICK_3, ":793 — the row the census window truncated at");
		assertNotNull(GT6UsbSticks.USB_STICK_4, ":794 — the row the census window lost (declared erratum 3->4)");
	}

	/** The join targets: the four families ride gt6:machines, the books their own tab. */
	@Test
	public void theJoinTargetsAreTheRuledTabs() {
		assertEquals("gt6:machines", GTMachines.MACHINES_TAB.getId().toString());
		assertEquals("gt6:books", GT6Books.BOOKS_TAB.getId().toString());
	}
}
