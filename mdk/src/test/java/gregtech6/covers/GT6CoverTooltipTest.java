package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.covers.client.GTCoverTooltipListener;
import gregtech6.covers.covers.AbstractCoverAttachmentScale;
import gregtech6.covers.covers.CoverConveyor;
import gregtech6.covers.covers.CoverControllerAutoRedstone;
import gregtech6.covers.covers.CoverControllerAuto;
import gregtech6.covers.covers.CoverControllerAutoTimer;
import gregtech6.covers.covers.CoverControllerCovers;
import gregtech6.covers.covers.CoverControllerDisplay;
import gregtech6.covers.covers.CoverControllerRedstone;
import gregtech6.covers.covers.CoverDisplayEnergy;
import gregtech6.covers.covers.CoverDrain;
import gregtech6.covers.covers.CoverFilterFluid;
import gregtech6.covers.covers.CoverFilterItem;
import gregtech6.covers.covers.CoverPressureValve;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverRedstoneConductorIN;
import gregtech6.covers.covers.CoverRedstoneConductorOUT;
import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.covers.covers.CoverRedstoneRepeater;
import gregtech6.covers.covers.CoverRedstoneTorch;
import gregtech6.covers.covers.CoverRetrieverItem;
import gregtech6.covers.covers.CoverRobotArm;
import gregtech6.covers.covers.CoverScaleEnergy;
import gregtech6.covers.covers.CoverScaleProgress;
import gregtech6.covers.covers.CoverSelectorButtonPanel;
import gregtech6.covers.covers.CoverSelectorManual;
import gregtech6.covers.covers.CoverSelectorRedstone;
import gregtech6.covers.covers.CoverSelectorTag;
import gregtech6.covers.covers.CoverShutter;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.covers.covers.CoverVent;
import gregtech6.covers.covers.logistics.CoverLogisticsDisplayCPULogic;
import gregtech6.covers.covers.logistics.CoverLogisticsFluidImport;
import gregtech6.covers.covers.logistics.CoverLogisticsGenericExport;
import gregtech6.covers.covers.logistics.CoverLogisticsItemImport;

/**
 * The cover tooltip face (task tooltip-cover-face): every cover item answers the hover
 * with the rows its upstream class emits through {@code ICover.addToolTips}
 * (gregapi/cover/ICover.java:143, dispatched by the global hover hook
 * GT_API_Proxy_Client.onItemTooltip :285-286 over {@code CoverRegistry.get(stack)}).
 * Red-green card: before it, the port cover items carried ZERO hover rows.
 *
 * <p>Row order, colors and keys pin the upstream per-class addToolTips verbatim (the
 * upstream line anchors live on each override); rows gated on fluids/mods the port
 * does not register (the drain's sewage/XP zoo, the vent's Galacticraft note) must NOT
 * appear — the declared-collapse contract. The predicate-derived logistics rows follow
 * the PORT predicates (usePriorities/useTargetStackSize), so the hover can never
 * disagree with the tool behaviour of the class it describes.
 */
public class GT6CoverTooltipTest extends GTCoverTestBase {

	private static final String BASE = "gt6.tooltip.cover.base";
	private static final String SCREWDRIVER = "gt6.tooltip.cover.toggle_screwdriver";
	private static final String CUTTER = "gt6.tooltip.cover.toggle_cutter";
	private static final String CONTROLLER = "gt6.tooltip.cover.toggle_controller";
	private static final String MONKEY_WRENCH = "gt6.tooltip.cover.toggle_monkey_wrench";
	private static final String SOFT_HAMMER = "gt6.tooltip.cover.reset_soft_hammer";
	private static final String MAGNIFIER = "gt6.tooltip.cover.detail_magnifyingglass";
	private static final String CHISEL = "gt6.tooltip.cover.change_design_chisel";

	private static List<Component> collect(ICover aCover) {
		List<Component> rList = new ArrayList<>();
		aCover.addToolTips(rList, new ItemStack(Items.BRICKS), false);
		return rList;
	}

	private static List<Component> collect(ICover aCover, ItemStack aStack) {
		List<Component> rList = new ArrayList<>();
		aCover.addToolTips(rList, aStack, false);
		return rList;
	}

	/** The leg-swap lane write — the {@link CoverData#laneOf} read's twin (the getCoverItem fork). */
	private static ItemStack withLane(ItemStack aStack, CompoundTag aLane) {
		//? if forge {
		aStack.setTag(aLane);
		//?} else {
		/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.COVER_PAYLOAD, aStack, aLane);
		 *///?}
		return aStack;
	}

	private static Component row(List<Component> aTooltip, String aKey) {
		for (Component tRow : aTooltip) if (containsKey(tRow, aKey)) return tRow;
		return null;
	}

	private static boolean containsKey(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return true;
		for (Component tSibling : aRow.getSiblings()) if (containsKey(tSibling, aKey)) return true;
		return false;
	}

	private static TranslatableContents keyedContents(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return tContents;
		for (Component tSibling : aRow.getSiblings()) {
			TranslatableContents tFound = keyedContents(tSibling, aKey);
			if (tFound != null) return tFound;
		}
		return null;
	}

	private static void assertKeyAt(List<Component> aTooltip, int aIndex, String aKey, String aMessage) {
		TranslatableContents tContents = aTooltip.get(aIndex).getContents() instanceof TranslatableContents t
				? t : null;
		assertNotNull(tContents, aMessage);
		assertEquals(aKey, tContents.getKey(), aMessage);
	}

	private static void assertDGRAY(Component aRow) {
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), aRow.getStyle().getColor(), "LH.Chat.DGRAY (LH.java:695)");
	}

	private static void assertCYAN(Component aRow) {
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), aRow.getStyle().getColor(), "LH.Chat.CYAN (LH.java:698)");
	}

	private static void assertORANGE(Component aRow) {
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), aRow.getStyle().getColor(), "LH.Chat.ORANGE (LH.java:692)");
	}

	@Test
	public void baseRowIsTheOnlyRowOfThePlainCovers() {
		// upstream AbstractCoverDefault:77 — every cover carries the DGRAY usable-as-cover row
		List<ICover> tPlain = List.of(
				new CoverTextureSimple(TEST_SPRITE),
				new CoverRedstoneConductorIN(), new CoverRedstoneConductorOUT(),
				new CoverRedstoneTorch(), new CoverRedstoneRepeater(),
				new CoverSelectorTag((byte) 0), new CoverSelectorManual(), new CoverSelectorRedstone(),
				new CoverControllerAuto(), new CoverDisplayEnergy(),
				new CoverControllerAutoTimer(CoverControllerAutoTimer.TIMER_TIMES[0]));
		for (ICover tCover : tPlain) {
			List<Component> tRows = collect(tCover);
			assertEquals(1, tRows.size(), tCover.getClass().getSimpleName() + " carries exactly the base row");
			assertKeyAt(tRows, 0, BASE, tCover.getClass().getSimpleName() + " base row");
			assertDGRAY(tRows.get(0));
		}
	}

	@Test
	public void emitterShowsTheCutterAndMagnifierRows() {
		// upstream CoverRedstoneEmitter:65-68
		List<Component> tRows = collect(new CoverRedstoneEmitter());
		assertEquals(3, tRows.size());
		assertKeyAt(tRows, 0, BASE, "base first");
		assertKeyAt(tRows, 1, CUTTER, "the cutter toggle row");
		assertKeyAt(tRows, 2, MAGNIFIER, "the magnifyingglass detail row");
		tRows.forEach(r -> assertDGRAY(r));
	}

	@Test
	public void controllerPairsShowTheScrewdriverAndMagnifierRows() {
		// upstream CoverControllerRedstone:55-58 / CoverControllerAutoRedstone:57-60 / CoverControllerCovers:94-97
		for (ICover tCover : List.of(new CoverControllerRedstone(), new CoverControllerAutoRedstone(), new CoverControllerCovers())) {
			List<Component> tRows = collect(tCover);
			assertEquals(3, tRows.size(), tCover.getClass().getSimpleName());
			assertKeyAt(tRows, 1, SCREWDRIVER, tCover.getClass().getSimpleName());
			assertKeyAt(tRows, 2, MAGNIFIER, tCover.getClass().getSimpleName());
		}
	}

	@Test
	public void pumpShowsTheThroughputAndWarningRows() {
		// upstream CoverPump:79-84 — CYAN throughput (mThroughput arg) + ORANGE no-fluid-blocks + the two tool rows
		List<Component> tRows = collect(new CoverPump());
		assertEquals(5, tRows.size());
		assertKeyAt(tRows, 0, BASE, "base first");
		assertKeyAt(tRows, 1, "gt6.tooltip.cover.pump_throughput", "the CYAN throughput row");
		assertCYAN(tRows.get(1));
		assertEquals("1000", String.valueOf(keyedContents(tRows.get(1), "gt6.tooltip.cover.pump_throughput").getArgs()[0]), "the port single-tier THROUGHPUT");
		assertKeyAt(tRows, 2, "gt6.tooltip.cover.pump_no_fluid_blocks", "the ORANGE warning");
		assertORANGE(tRows.get(2));
		assertKeyAt(tRows, 3, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 4, SCREWDRIVER, "the screwdriver row");
	}

	@Test
	public void conveyorAndRobotArmComposeThePeriodRow() {
		// upstream CoverConveyor:76-81 / CoverRobotArm:102-107 — "Transfers a Stack every <arg>"
		List<Component> tSlow = collect(new CoverConveyor(512));
		assertEquals(4, tSlow.size());
		assertKeyAt(tSlow, 1, "gt6.tooltip.cover.transfer_period", "the CYAN period row");
		assertCYAN(tSlow.get(1));
		assertEquals("512 Ticks", keyedContents(tSlow.get(1), "gt6.tooltip.cover.transfer_period").getArgs()[0], "the tick-period word (mTiming > 1)");
		assertKeyAt(tSlow, 2, CONTROLLER, "the controller row");
		assertKeyAt(tSlow, 3, SCREWDRIVER, "the screwdriver row");

		List<Component> tFast = collect(new CoverConveyor(1));
		assertEquals("Tick", keyedContents(tFast.get(1), "gt6.tooltip.cover.transfer_period").getArgs()[0], "the singular arm (mTiming == 1)");

		List<Component> tArm = collect(new CoverRobotArm(8));
		assertEquals(5, tArm.size(), "the arm adds the monkey wrench row");
		assertEquals("8 Ticks from/to a specific Slot",
				keyedContents(tArm.get(1), "gt6.tooltip.cover.transfer_period").getArgs()[0], "the slot wording (mTiming > 1)");
		assertKeyAt(tArm, 2, MONKEY_WRENCH, "the monkey wrench row");
		assertKeyAt(tArm, 3, CONTROLLER, "the controller row");
		assertKeyAt(tArm, 4, SCREWDRIVER, "the screwdriver row");
	}

	@Test
	public void filterItemShowsTheCarriedFilterName() {
		// upstream CoverFilterItem:46-53 — filter name row BEFORE the super chain, then not-NBT + base + tools + reset
		ItemStack tFiltered = withLane(new ItemStack(Items.BRICKS), CoverFilterItem.filterTagFor(new ItemStack(Items.APPLE)));
		List<Component> tRows = collect(new CoverFilterItem(), tFiltered);
		// [name, not-NBT, base, controller, screwdriver, soft hammer] — NO cutter row:
		// CoverFilterItem is a plain attachment (upstream :46-53 has no stack-size row)
		assertEquals(6, tRows.size());
		assertTrue(tRows.get(0).getString().contains("Apple"), "the filter name resolves on the bootstrapped locale");
		assertCYAN(tRows.get(0));
		assertKeyAt(tRows, 1, "gt6.tooltip.cover.filter_not_nbt_sensitive", "the ORANGE NBT note");
		assertORANGE(tRows.get(1));
		assertKeyAt(tRows, 2, BASE, "the super chain lands AFTER the filter rows (upstream :50 order)");
		assertKeyAt(tRows, 3, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 4, SCREWDRIVER, "the screwdriver row");
		assertKeyAt(tRows, 5, SOFT_HAMMER, "the soft hammer reset row");

		List<Component> tBare = collect(new CoverFilterItem(), new ItemStack(Items.BRICKS));
		assertEquals(5, tBare.size(), "no filter set → no name row (the ORANGE note itself is unconditional, upstream :48)");
		assertNull(row(tBare, "item.minecraft.apple"), "no apple row");
	}

	@Test
	public void retrieverMirrorsTheFilterItemFace() {
		// upstream CoverRetrieverItem:82-89 — same six-row shape (controller + screwdriver
		// + reset over the super chain, no cutter — a plain attachment)
		ItemStack tFiltered = withLane(new ItemStack(Items.BRICKS), CoverFilterItem.filterTagFor(new ItemStack(Items.APPLE)));
		List<Component> tRows = collect(new CoverRetrieverItem(), tFiltered);
		assertEquals(6, tRows.size());
		assertTrue(tRows.get(0).getString().contains("Apple"));
		assertKeyAt(tRows, 5, SOFT_HAMMER, "the soft hammer reset row");
	}

	@Test
	public void filterFluidShowsTheCarriedFluidName() {
		// upstream CoverFilterFluid:50-57 — the gt.filter.fluid string lane resolves the fluid display face
		ItemStack tFiltered = new ItemStack(Items.BRICKS);
		CompoundTag tLane = new CompoundTag();
		tLane.putString(CoverFilterFluid.FILTER_KEY, "minecraft:water");
		tFiltered = withLane(tFiltered, tLane);
		List<Component> tRows = collect(new CoverFilterFluid(), tFiltered);
		assertEquals(6, tRows.size());
		assertEquals("Water", tRows.get(0).getString(), "the fluid display face (the fluid block's name — the upstream FL.name word)");
		assertCYAN(tRows.get(0));
		assertKeyAt(tRows, 5, SOFT_HAMMER, "the soft hammer reset row");
	}

	@Test
	public void pressureValveShowsTheReleaseRows() {
		// upstream CoverPressureValve:67-72
		List<Component> tRows = collect(new CoverPressureValve());
		assertEquals(5, tRows.size());
		assertKeyAt(tRows, 1, "gt6.tooltip.cover.valve_release", "the CYAN release row");
		assertCYAN(tRows.get(1));
		assertKeyAt(tRows, 2, "gt6.tooltip.cover.valve_liquids_tank", "the ORANGE liquid requirement");
		assertORANGE(tRows.get(2));
		assertKeyAt(tRows, 3, "gt6.tooltip.cover.valve_gases_air", "the ORANGE gas requirement");
		assertORANGE(tRows.get(3));
		assertKeyAt(tRows, 4, CONTROLLER, "the controller row");
	}

	@Test
	public void drainShowsTheThreeCollectionRowsAndNoCutArms() {
		// upstream CoverDrain:227-237 — the sewage/XP/mob/OpenBlocks rows collapse with the fluid zoo (the declared cut)
		List<Component> tRows = collect(new CoverDrain());
		assertEquals(5, tRows.size());
		assertKeyAt(tRows, 0, BASE, "base first");
		assertKeyAt(tRows, 1, "gt6.tooltip.cover.drain_fluid_blocks", "the CYAN fluid-block row");
		assertCYAN(tRows.get(1));
		assertKeyAt(tRows, 2, "gt6.tooltip.cover.drain_rainwater", "the CYAN rain row");
		assertKeyAt(tRows, 3, "gt6.tooltip.cover.drain_river_lake", "the CYAN river/lake row");
		assertKeyAt(tRows, 4, CONTROLLER, "the controller row");
	}

	@Test
	public void ventShowsOnlyTheControllerRow() {
		// upstream CoverVent:68-71 — the Galacticraft row collapses (single-mod port)
		List<Component> tRows = collect(new CoverVent());
		assertEquals(2, tRows.size());
		assertKeyAt(tRows, 1, CONTROLLER, "the controller row");
	}

	@Test
	public void shutterShowsTheColoredControllerRow() {
		// upstream CoverShutter:44-47 — the controller row is CYAN here, not DGRAY
		List<Component> tRows = collect(new CoverShutter());
		assertEquals(3, tRows.size());
		assertKeyAt(tRows, 1, CONTROLLER, "the controller row");
		assertCYAN(tRows.get(1));
		assertKeyAt(tRows, 2, SCREWDRIVER, "the screwdriver row");
	}

	@Test
	public void scaleFamilyShowsTheThreeSensorRows() {
		// upstream AbstractCoverAttachmentScale:72-76
		for (ICover tCover : List.of(new CoverScaleEnergy(), new CoverScaleProgress())) {
			List<Component> tRows = collect(tCover);
			assertEquals(4, tRows.size(), tCover.getClass().getSimpleName());
			assertKeyAt(tRows, 1, SCREWDRIVER, tCover.getClass().getSimpleName());
			assertKeyAt(tRows, 2, CUTTER, tCover.getClass().getSimpleName());
			assertKeyAt(tRows, 3, MAGNIFIER, tCover.getClass().getSimpleName());
		}
	}

	@Test
	public void selectorButtonPanelShowsTheChiselRow() {
		// upstream CoverSelectorButtonPanel:96-100 — DGRAY chisel + controller + screwdriver
		List<Component> tRows = collect(new CoverSelectorButtonPanel());
		assertEquals(4, tRows.size());
		assertKeyAt(tRows, 1, CHISEL, "the design chisel row");
		assertDGRAY(tRows.get(1));
		assertKeyAt(tRows, 2, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 3, SCREWDRIVER, "the screwdriver row");
	}

	@Test
	public void machineDisplayShowsTheUnstyledChiselRow() {
		// upstream CoverControllerDisplay:89 — the chisel row rides NO Chat prefix
		List<Component> tRows = collect(new CoverControllerDisplay());
		assertEquals(2, tRows.size());
		assertKeyAt(tRows, 1, CHISEL, "the design chisel row");
		assertNull(tRows.get(1).getStyle().getColor(), "upstream adds LH.get WITHOUT a Chat prefix — no color face");
	}

	@Test
	public void logisticsGenericShowsThePredicateRows() {
		// upstream AbstractCoverAttachmentLogistics:50-55 + CoverLogisticsGenericExport:38
		// (useTargetStackSize T on the generic bus) — base + controller + both predicate rows
		List<Component> tRows = collect(CoverLogisticsGenericExport.INSTANCE);
		assertEquals(4, tRows.size());
		assertKeyAt(tRows, 1, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 2, SCREWDRIVER, "the priorities screwdriver row (usePriorities)");
		assertKeyAt(tRows, 3, CUTTER, "the stack-size cutter row (useTargetStackSize T, upstream :38)");
	}

	@Test
	public void logisticsFilteredShowsTheFilterRowsAndStackRow() {
		// upstream CoverLogisticsItemImport:47-52 — filter name + not-NBT + super chain + reset;
		// the port filtered base: stack-size T → cutter row in the chain
		ItemStack tFiltered = withLane(new ItemStack(Items.BRICKS), CoverLogisticsItemImport.INSTANCE.filterLaneFor(new ItemStack(Items.APPLE)));
		List<Component> tRows = collect(CoverLogisticsItemImport.INSTANCE, tFiltered);
		assertEquals(7, tRows.size());
		assertTrue(tRows.get(0).getString().contains("Apple"), "the filter name row");
		assertKeyAt(tRows, 1, "gt6.tooltip.cover.filter_not_nbt_sensitive", "the ORANGE NBT note");
		assertKeyAt(tRows, 2, BASE, "the super chain");
		assertKeyAt(tRows, 3, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 4, SCREWDRIVER, "the priorities row");
		assertKeyAt(tRows, 5, CUTTER, "the stack-size row (useTargetStackSize T on the filtered base)");
		assertKeyAt(tRows, 6, SOFT_HAMMER, "the reset row");

		List<Component> tFluid = collect(CoverLogisticsFluidImport.INSTANCE,
				new ItemStack(Items.BRICKS)); // no lane → no name row, the base face stays
		assertEquals(6, tFluid.size(), "no lane → no name row (not-NBT + super chain + reset remain)");
		assertNull(row(tFluid, "minecraft.water"), "no water row without a lane");
	}

	@Test
	public void logisticsDisplayShowsTheStatusRow() {
		// upstream AbstractCoverAttachmentLogisticsDisplay:36-39 — the hardcoded status face; port display predicates both F → no tool rows
		List<Component> tRows = collect(CoverLogisticsDisplayCPULogic.INSTANCE);
		assertEquals(3, tRows.size());
		assertKeyAt(tRows, 1, CONTROLLER, "the controller row");
		assertKeyAt(tRows, 2, "gt6.tooltip.cover.logistics_display_status", "the DGRAY status row");
		assertDGRAY(tRows.get(2));
	}

	@Test
	public void scaleAbstractCarriesTheFamilyRowsContract() {
		// the AbstractCoverAttachmentScale override is the family carrier — a direct ICover-level smoke
		assertTrue(ICover.class.isAssignableFrom(AbstractCoverAttachmentScale.class), "the scale family rides ICover.addToolTips");
	}

	@Test
	public void listenerSeamAppendsRowsThroughTheRegistry() {
		// upstream GT_API_Proxy_Client.onItemTooltip :285-286 — CoverRegistry.get(stack) → addToolTips
		CoverRegistry.reset();
		CoverRegistry.put(Items.BRICKS, new CoverTextureSimple(TEST_SPRITE));
		List<Component> tTooltip = new ArrayList<>();
		GTCoverTooltipListener.appendCoverTooltips(new ItemStack(Items.BRICKS), tTooltip, false);
		assertEquals(1, tTooltip.size(), "the registered cover item gains the base row");
		assertKeyAt(tTooltip, 0, BASE, "the registry-dispatched row");

		List<Component> tStranger = new ArrayList<>();
		GTCoverTooltipListener.appendCoverTooltips(new ItemStack(Items.STICK), tStranger, false);
		assertTrue(tStranger.isEmpty(), "an unregistered item gains nothing");
		CoverRegistry.reset();
	}

	@Test
	public void advancedFlagRidesThroughTheContract() {
		// the aF3_H parameter is pass-through upstream (no cover branches on it) — the signature stays the contract
		List<Component> tRows = new ArrayList<>();
		new CoverTextureSimple(TEST_SPRITE).addToolTips(tRows, new ItemStack(Items.BRICKS), true);
		assertEquals(1, tRows.size(), "advanced flag does not change the row set");
	}
}
