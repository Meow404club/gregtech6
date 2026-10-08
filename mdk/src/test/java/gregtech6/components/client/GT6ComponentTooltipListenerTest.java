package gregtech6.components.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregtech6.components.OM;
import gregtech6.components.OMComponentFaceTest;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;

/**
 * The F3+H contained-materials face over the CENTRAL component face (task
 * component-tooltip-f3h-rows): {@link GT6ComponentTooltipListener} replays the
 * upstream arm (GT_API_Proxy_Client.onItemTooltip :452-479) for every item the
 * {@code gregtech6.components.OM} read chain can answer that does not already
 * self-describe ({@code MaterialPrefixItem.appendMaterialTooltip :272-283} owns the
 * prefix-item face — the double-print guard is pin #2).
 *
 * <p>Card pins (ACCEPTANCE ①②③ + the SPEC negative arms): ① the query→display mapping
 * (machine item with map-written data → DCYAN header + the :459-475 row, amount =
 * displayUnits of the WRITTEN data); the alloy stays ONE row over its alloy material —
 * the getAllMaterialWeights semantics (OreDictItemData.java:200-211), no Cu/Sn expansion
 * of Bronze; ② prefix items gain zero rows (their own hover owns the face); ③ the
 * non-advanced face carries ZERO component rows — the DGRAY hint (:477-479) is the
 * upstream non-advanced display of the SAME gate and rides along (the merged
 * material-tooltip-face convention,
 * MaterialPrefixTooltipTest.containedMaterialsOnlyRideTheAdvancedFlag); ④ an item the
 * central face cannot answer gains nothing in either mode — the :323/:336 guards;
 * ⑤ the :454 DONT_SHOW_THIS_COMPONENT filter arm.
 *
 * <p>Offline posture: the {@code OMComponentFaceTest} probe shape, NOT the
 * {@code GTOfflineTestBase} fixture latch — on the 1.21.1 leg the latch's
 * {@code locked}/{@code frozen} field walk never arms (baseline-proven 2026-10-08:
 * every {@code registerItemFixture} consumer assume-skips there, e.g. a clean-base
 * {@code MaterialPrefixTooltipTest} run is 10/10 skipped), which would have made this
 * file's neo leg vacuous. The probe opens the frozen registry per leg and registers the
 * fixture items for real, so every pin below executes on BOTH legs.
 */
public class GT6ComponentTooltipListenerTest {

	static Item sMachine;
	static Item sAlloyMachine;
	static Item sHintMachine;
	static Item sDontShowMachine;
	static Item sBare;
	static MaterialPrefixItem sPrefixIngot;

	@BeforeAll
	static void buildFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap(); // the Forge-patched boot throws offline at NetworkHooks — the registries are ready by then
		} catch (Throwable ignored) {
		}
		GTMaterialItems.initMaterials();
		sMachine = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_machine", Item::new);
		sAlloyMachine = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_alloy_machine", Item::new);
		sHintMachine = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_hint_machine", Item::new);
		sDontShowMachine = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_dontshow_machine", Item::new);
		sBare = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_bare", Item::new);
		sPrefixIngot = OMComponentFaceTest.probeItem("minecraft", "fixture_f3h_prefix_ingot",
				p -> new MaterialPrefixItem(p, OP.ingot, MT.Fe));
	}

	private static List<Component> hover(ItemStack aStack, boolean aAdvanced) {
		List<Component> tTooltip = new ArrayList<>();
		GT6ComponentTooltipListener.appendComponentTooltips(aStack, tTooltip, aAdvanced);
		return tTooltip;
	}

	/** The first row whose sibling walk carries the lang key (the MaterialPrefixTooltipTest walk). */
	private static Component findKeyedRow(List<Component> aTooltip, String aKey) {
		for (Component tRow : aTooltip) if (containsKey(tRow, aKey)) return tRow;
		return null;
	}

	private static boolean containsKey(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return true;
		for (Component tSibling : aRow.getSiblings()) if (containsKey(tSibling, aKey)) return true;
		return false;
	}

	/** The component-row face of the :459-475 builder — the "M: " literal slot. */
	private static Component componentRow(List<Component> aTooltip) {
		for (Component tRow : aTooltip) if (tRow.getString().contains("M: ")) return tRow;
		return null;
	}

	private static long countComponentRows(List<Component> aTooltip) {
		return aTooltip.stream().filter(tRow -> tRow.getString().contains("M: ")).count();
	}

	@Test
	public void machineItemShowsHeaderPlusOneRowFromTheWrittenData() {
		// acceptance ① — the central-face map arm (OM.setItemData_, OM.java:294) feeds the
		// display: 2U Fe written → "2.000 " leads the row, the word slot rides
		// gt6.material.iron, the melting/boiling points and the kg block ride the :459-475
		// verbatim shape.
		assertTrue(OM.setItemData_(new ItemStack(sMachine), new OreDictItemData(MT.Fe, 2 * CS.U)), "the map write must land");
		List<Component> tTooltip = hover(new ItemStack(sMachine), true);
		Component tHeader = findKeyedRow(tTooltip, "gt6.tooltip.material.contained_materials");
		assertNotNull(tHeader, "the DCYAN header row shows under F3+H");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA), tHeader.getStyle().getColor(), "LH.Chat.DCYAN (:456)");
		assertEquals(1, countComponentRows(tTooltip), "exactly one component row follows the header");
		Component tRow = componentRow(tTooltip);
		assertNotNull(tRow);
		assertFalse(tRow.getSiblings().isEmpty(), "the row is the multi-sibling composition");
		assertEquals("2.000 ", tRow.getSiblings().get(0).getString(), "displayUnits of the WRITTEN amount leads the row");
		assertEquals("gt6.material." + MaterialPrefixItem.snakeCase(MT.Fe.mNameInternal), tRow.getSiblings().get(1).getString(),
				"the material word rides the gt6.material fill");
		assertTrue(tRow.getString().contains("M: " + MT.Fe.mMeltingPoint + "K "), "the melting-point slot rides the row");
		assertTrue(tRow.getString().contains("B: " + MT.Fe.mBoilingPoint + "K "), "the boiling-point slot rides the row");
		assertTrue(tRow.getString().contains(" W: "), "the weight slot rides the row");
		assertTrue(tRow.getString().endsWith("kg)"), "the kg unit closes the stat block");
	}

	@Test
	public void alloyDataStaysOneRowWithoutComponentExpansion() {
		// acceptance ① (the alloy arm) — Bronze data renders the ALLOY as one row (the
		// getAllMaterialWeights main-stack semantics, OreDictItemData.java:200-211); no
		// Cu/Sn decomposition faces may appear (the r11-tooltip-census semantic pin).
		assertTrue(OM.setItemData_(new ItemStack(sAlloyMachine), new OreDictItemData(MT.Bronze, 3 * CS.U)), "the map write must land");
		List<Component> tTooltip = hover(new ItemStack(sAlloyMachine), true);
		assertNotNull(findKeyedRow(tTooltip, "gt6.tooltip.material.contained_materials"), "the header shows");
		assertEquals(1, countComponentRows(tTooltip), "the alloy stays ONE row");
		Component tRow = componentRow(tTooltip);
		assertNotNull(tRow);
		assertEquals("3.000 ", tRow.getSiblings().get(0).getString(), "3U leads the row");
		assertEquals("gt6.material." + MaterialPrefixItem.snakeCase(MT.Bronze.mNameInternal), tRow.getSiblings().get(1).getString(),
				"the row names the ALLOY material");
		assertFalse(tRow.getString().contains("gt6.material." + MaterialPrefixItem.snakeCase(MT.Cu.mNameInternal)),
				"no Copper expansion face");
		assertFalse(tRow.getString().contains("gt6.material." + MaterialPrefixItem.snakeCase(MT.Sn.mNameInternal)),
				"no Tin expansion face");
	}

	@Test
	public void prefixItemsGainNothingFromTheListener() {
		// acceptance ② — the double-print guard: MaterialPrefixItem owns its face
		// (appendMaterialTooltip :272-283); the listener appends ZERO rows to it.
		assertTrue(hover(new ItemStack(sPrefixIngot), true).isEmpty(),
				"the listener must not double-print on a self-describing prefix item");
	}

	@Test
	public void nonAdvancedFaceShowsNoComponentRows() {
		// acceptance ③ — F3+H OFF: zero header/component rows; the DGRAY hint (:477-479)
		// is the upstream non-advanced display of the same data gate. Own fixture + own
		// write: the map state is per-test, the run order between pins is not guaranteed.
		assertTrue(OM.setItemData_(new ItemStack(sHintMachine), new OreDictItemData(MT.Fe, CS.U)), "the map write must land");
		List<Component> tTooltip = hover(new ItemStack(sHintMachine), false);
		assertNull(findKeyedRow(tTooltip, "gt6.tooltip.material.contained_materials"), "no header row without F3+H");
		assertEquals(0, countComponentRows(tTooltip), "zero component rows without F3+H");
		Component tHint = findKeyedRow(tTooltip, "gt6.tooltip.material.f3h_hint");
		assertNotNull(tHint, "the upstream :477-479 hint row shows instead");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), tHint.getStyle().getColor(), "LH.Chat.DGRAY");
	}

	@Test
	public void itemsWithoutCentralFaceDataGainNothing() {
		// the empty negative — the :323 (tData != null) + :336 (validMaterial) guards:
		// no data → NOTHING in either mode (the hint rides the data gate too).
		assertTrue(hover(new ItemStack(sBare), true).isEmpty(), "advanced + no data = zero rows");
		assertTrue(hover(new ItemStack(sBare), false).isEmpty(), "normal + no data = zero rows (the hint needs data)");
	}

	@Test
	public void dontShowThisComponentMaterialsAreFiltered() {
		// the :454 filter arm — MT.NULL carries DONT_SHOW_THIS_COMPONENT
		// (the MTTableFidelityTest pin), so even valid stored data shows nothing,
		// header included.
		assertTrue(MT.NULL.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT), "the fixture material carries the filter property");
		assertTrue(OM.setItemData_(new ItemStack(sDontShowMachine), new OreDictItemData(MT.NULL, CS.U)), "the map write must land");
		assertTrue(hover(new ItemStack(sDontShowMachine), true).isEmpty(),
				"the DONT_SHOW_THIS_COMPONENT filter leaves zero rows, header included");
	}

	// the probe seat: the single OMComponentFaceTest.probeItem definition serves this file
	// (task om-hygiene-mini folded the private third mirror away — same body, one home).
}
