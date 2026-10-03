package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import brachy.modularui.api.IThemeApi;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.AdaptableUITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.theme.DefaultTheme;
import brachy.modularui.theme.ImmutableJson;
import brachy.modularui.theme.Theme;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.theme.WidgetThemeMap;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The static-storage offline panel gate (task r11-gui-storage-clean — the W2 clean-base
 * card for the DrawerQuad/Safe pair, the GT6BatteryBoxMUIPanelTest shape): the wave-4
 * deviation as a widget tree (144 quadrant seats / the 15-seat safe chest), the seat
 * topology per seat, and the r11 clean-base takeover — both panels are BARE
 * {@code defaultPanel}s (zero sheet tokens in the factory, the census below), so the gt6
 * theme {@code panel.background} (the derived 9-slice base, GT6PanelBaseThemeCensusTest)
 * is the ONLY visual — asserted here through the real factory outputs, plus the
 * 356x250 non-standard-size stretch semantics (the design-card census declaration:
 * borders stay 4 GUI units because the drawable is a true 9-slice, NOT a whole-sheet
 * stretch — the exact property a {@code .background(sheet)} regression loses).
 *
 * <p>This class also hosts the shared clean-base helpers for the storage-family gates
 * ({@link GT6HopperMUIPanelTest}/{@link GT6BatteryBoxMUIPanelTest} call
 * {@link #gt6Theme}/{@link #assertCleanBaseHandsTo}/{@link #sizeOf} — one copy, three
 * consumers, the same package).
 */
class GT6StorageMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos POS = new BlockPos(50, 64, 50);

	/** The panel-fixture BETs (the selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<GT6DrawerQuadBlockEntity> sDrawerType;
	static BlockEntityType<GT6SafeBlockEntity> sSafeType;

	@BeforeAll
	static void buildFixtureBets() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6DrawerQuadBlockEntity>[] tDrawers =
				(BlockEntityType<GT6DrawerQuadBlockEntity>[]) new BlockEntityType<?>[1];
		tDrawers[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6DrawerQuadBlockEntity(tDrawers[0], aPos, aState),
				Blocks.STONE).build(null);
		sDrawerType = tDrawers[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6SafeBlockEntity>[] tSafes =
				(BlockEntityType<GT6SafeBlockEntity>[]) new BlockEntityType<?>[1];
		tSafes[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6SafeBlockEntity(tSafes[0], aPos, aState),
				Blocks.STONE).build(null);
		sSafeType = tSafes[0];
	}

	/**
	 * The offline FML dist shaping (the GT6BatteryBoxMUIPanelTest form — the vendored MUI
	 * widget classes read FMLEnvironment.dist during their static init). Must run BEFORE
	 * the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6StorageMUIPanelTest.class.getClassLoader());
		java.lang.reflect.Field tDist = tFmlEnv.getDeclaredField("dist");
		sun.misc.Unsafe tUnsafe;
		java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		tTheUnsafe.setAccessible(true);
		tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
		if (tUnsafe.getObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist)) == null) {
			tUnsafe.putObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist),
					net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
		}
	}

	private static GT6DrawerQuadBlockEntity drawer() {
		return new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
	}

	private static GT6SafeBlockEntity safe() {
		return new GT6SafeBlockEntity(sSafeType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** Every widget in the panel tree, in child order. */
	private static List<IWidget> allWidgets(ModularPanel<?> aPanel) {
		List<IWidget> tAll = new ArrayList<>();
		collectAll(aPanel, tAll);
		return tAll;
	}

	private static void collectAll(IWidget aWidget, List<IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectAll(tChild, aSink);
			}
		}
	}

	/** The widget by its build name. */
	@Nullable
	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

	/** The slot-group widget by its registered group id (the quadrant groups carry no build name). */
	private static SlotGroupWidget group(ModularPanel<?> aPanel, String aGroupName) {
		return allWidgets(aPanel).stream()
				.filter(w -> w instanceof SlotGroupWidget tGroup && aGroupName.equals(tGroup.getSlotGroupName()))
				.map(w -> (SlotGroupWidget) w).findFirst().orElse(null);
	}

	/** The ItemSlot children of a group, in build order (the seat order of the builder matrix). */
	private static List<ItemSlot> seatsOf(SlotGroupWidget aGroup) {
		List<ItemSlot> tSeats = new ArrayList<>();
		for (IWidget tChild : aGroup.getChildren()) {
			if (tChild instanceof ItemSlot tSlot) tSeats.add(tSlot);
		}
		return tSeats;
	}

	/**
	 * The widget build position — pos() lands in the resizer's start Unit (the headless
	 * face the batterybox test pins). Works for panel-anchored widgets and for children
	 * INSIDE a slot group alike (the group builder lays its seats out relative).
	 */
	private static int posOf(IWidget aWidget, boolean aX) throws Exception {
		return unitOf(aWidget, aX ? "x" : "y", "start");
	}

	/** The widget build size (the drawer's 356x250 face). */
	static int sizeOf(IWidget aWidget, boolean aWidth) throws Exception {
		return unitOf(aWidget, aWidth ? "x" : "y", "size");
	}

	private static int unitOf(IWidget aWidget, String aAxis, String aUnit) throws Exception {
		brachy.modularui.api.widget.IPositioned tPositioned = (brachy.modularui.api.widget.IPositioned) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aAxis);
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tField = tSizer.getClass().getDeclaredField(aUnit);
		tField.setAccessible(true);
		Object tUnit = tField.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private static void assertPos(IWidget aWidget, int aX, int aY, String aWhat) throws Exception {
		assertEquals(aX, posOf(aWidget, true), aWhat + " x");
		assertEquals(aY, posOf(aWidget, false), aWhat + " y");
	}

	// ---------------------------------------------------------------------------
	// the drawer — ONE page, four 9x4 quadrants, 144 machine seats (the wave-4
	// deviation declaration as a widget tree)
	// ---------------------------------------------------------------------------

	@Test
	public void theDrawerPanelIsTheFourQuadrant144SeatTopology() throws Exception {
		ModularPanel<?> tPanel = GT6StorageMUI.drawerPanel(drawer(), headlessSyncManager());
		assertEquals(GT6StorageMUI.DRAWER_PANEL, tPanel.getName(), "the drawer panel name");
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GT6DrawerQuadBlockEntity.class),
				"the drawer BE implements GT6MuiMachine (the tryOpen bound, zero new MenuType)");

		// the non-standard geometry: two quadrant columns of 162px + a 16px gutter + 8px
		// margins; height = the 166 machine face + the 76-row player band + the 8px tail
		assertEquals(356, sizeOf(tPanel, true), "the drawer width = 8+162+16+162+8");
		assertEquals(250, sizeOf(tPanel, false), "the drawer height = 166+76+8");

		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(144 + 36, tSeats, "the 144 quadrant seats + the 36 player seats (issue #3)");

		// the quadrant origins (the four grid corners; q2/q3 ride the bottom row)
		assertPos(group(tPanel, GT6StorageMUI.GROUP_Q0), 8, 8, "quadrant q0 origin");
		assertPos(group(tPanel, GT6StorageMUI.GROUP_Q1), GT6StorageMUI.QUAD_RIGHT_X, 8, "quadrant q1 origin");
		assertPos(group(tPanel, GT6StorageMUI.GROUP_Q2), 8, GT6StorageMUI.QUAD_BOTTOM_Y, "quadrant q2 origin");
		assertPos(group(tPanel, GT6StorageMUI.GROUP_Q3), GT6StorageMUI.QUAD_RIGHT_X, GT6StorageMUI.QUAD_BOTTOM_Y,
				"quadrant q3 origin");

		// per seat, all 144: the 9x4 relative grid, the 36-seat quadrant block
		// (quadrantBase = q*36) and the shift-transfer row width 9
		String[] tGroups = {GT6StorageMUI.GROUP_Q0, GT6StorageMUI.GROUP_Q1, GT6StorageMUI.GROUP_Q2, GT6StorageMUI.GROUP_Q3};
		for (int q = 0; q < 4; q++) {
			SlotGroupWidget tQuadrant = group(tPanel, tGroups[q]);
			assertNotNull(tQuadrant, "quadrant group q" + q + " rides the tree");
			assertEquals(162, sizeOf(tQuadrant, true), "q" + q + " width = 9 columns of 18");
			assertEquals(72, sizeOf(tQuadrant, false), "q" + q + " height = 4 rows of 18");
			List<ItemSlot> tSeatsQ = seatsOf(tQuadrant);
			assertEquals(36, tSeatsQ.size(), "q" + q + " seat count");
			for (int i = 0; i < 36; i++) {
				ModularSlot tSlot = assertInstanceOf(ModularSlot.class, tSeatsQ.get(i).getSlot(), "q" + q + " seat " + i);
				assertEquals(q * 36 + i, tSlot.getSlotIndex(), "q" + q + " seat " + i + " binds its quadrant block");
				assertEquals(18 * (i % 9), posOf(tSeatsQ.get(i), true), "q" + q + " seat " + i + " relative x");
				assertEquals(18 * (i / 9), posOf(tSeatsQ.get(i), false), "q" + q + " seat " + i + " relative y");
			}
		}

		// the player inventory, centered under the quadrant field, UNCONDITIONAL (issue #3)
		assertPos(named(tPanel, "player_inventory"), 97, 166, "the drawer player inventory");
	}

	// ---------------------------------------------------------------------------
	// the safe — the upstream 15-slot chest on the standard 176x166 panel
	// ---------------------------------------------------------------------------

	@Test
	public void theSafePanelIsTheUpstream15SeatChestGrid() throws Exception {
		ModularPanel<?> tPanel = GT6StorageMUI.safePanel(safe(), headlessSyncManager());
		assertEquals(GT6StorageMUI.SAFE_PANEL, tPanel.getName(), "the safe panel name");
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GT6SafeBlockEntity.class),
				"the safe BE implements GT6MuiMachine (the tryOpen bound, zero new MenuType)");
		assertEquals(176, sizeOf(tPanel, true), "the standard panel width");
		assertEquals(166, sizeOf(tPanel, false), "the standard panel height");

		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(15 + 36, tSeats, "the 15-slot chest + the 36 player seats (issue #3)");

		// the 5x3 centered block at (43,8) — the ContainerCommonDefault full-inventory form
		SlotGroupWidget tChest = group(tPanel, GT6StorageMUI.GROUP_SAFE);
		assertNotNull(tChest, "the safe slot group rides the tree");
		assertPos(tChest, 43, 8, "the safe chest origin");
		assertEquals(90, sizeOf(tChest, true), "the chest width = 5 columns of 18");
		assertEquals(54, sizeOf(tChest, false), "the chest height = 3 rows of 18");
		List<ItemSlot> tSeats15 = seatsOf(tChest);
		assertEquals(15, tSeats15.size(), "the NBT_INV_SIZE 15 seat count");
		for (int i = 0; i < 15; i++) {
			ModularSlot tSlot = assertInstanceOf(ModularSlot.class, tSeats15.get(i).getSlot(), "safe seat " + i);
			assertEquals(i, tSlot.getSlotIndex(), "safe seat " + i + " binds its index");
			assertEquals(18 * (i % 5), posOf(tSeats15.get(i), true), "safe seat " + i + " relative x");
			assertEquals(18 * (i / 5), posOf(tSeats15.get(i), false), "safe seat " + i + " relative y");
		}

		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		assertPos(named(tPanel, "player_inventory"), 7, 84, "the safe player inventory");
	}

	// ---------------------------------------------------------------------------
	// the r11 clean-base takeover — the theme 9-slice base is the only visual
	// ---------------------------------------------------------------------------

	/**
	 * Both panels are bare {@code defaultPanel}s: zero code-side background (zero
	 * machines/ path — the census below makes the negative conclusion hold at the source
	 * level too) and the gt6 theme hands the derived 9-slice base through the real panel
	 * dispatch. The DRAWER is the one non-166 panel in the whole mod: the stretch stays
	 * faithful at 356x250 ONLY because the drawable is a true 9-slice — source 176x166
	 * with 4px borders drawn unscaled, center stretched (the design-card census
	 * declaration). A whole-sheet stretch regression loses exactly the bl/bt/br/bb=4
	 * pins here AND would drag the printed player-inventory band across the machine area.
	 */
	@Test
	public void theThemeHandsTheNineSliceBaseToBothStoragePanels() throws Exception {
		ModularPanel<?> tDrawer = GT6StorageMUI.drawerPanel(drawer(), headlessSyncManager());
		ModularPanel<?> tSafe = GT6StorageMUI.safePanel(safe(), headlessSyncManager());
		assertCleanBaseHandsTo(tDrawer, "the drawer panel (356x250)");
		assertCleanBaseHandsTo(tSafe, "the safe panel");

		// the non-deformation declaration: 4px borders on BOTH axes of the 356x250 target
		// stay 4 GUI units (2*4 slice bands never fold at this size), the source sheet
		// stays 176x166 — the 9-slice contract a fullImage stretch cannot satisfy
		assertEquals(356, sizeOf(tDrawer, true), "the stretched target width");
		assertEquals(250, sizeOf(tDrawer, false), "the stretched target height");
		AdaptableUITexture tBase = assertInstanceOf(AdaptableUITexture.class,
				tDrawer.getWidgetThemeInternal(gt6Theme()).theme().getBackground(), "the drawer chained background");
		assertEquals(8, tBase.bl() + tBase.br(), "the horizontal slice bands");
		assertEquals(8, tBase.bt() + tBase.bb(), "the vertical slice bands");
		assertTrue(2 * 4 < 356 && 2 * 4 < 250, "the slice bands never fold at the drawer size");
	}

	/**
	 * The negative conclusion behind "zero full-image status quo", held at the source
	 * level: the three storage-family panel factories (storage/hopper/batterybox) carry
	 * ZERO sheet tokens — no {@code gui/machines} texture path, no {@code GuiTextures}
	 * constant, no code-side {@code .background(} call. A re-introduced sheet turns
	 * this red (the W1 teardown form, pre-armed for this family).
	 */
	@Test
	public void theStoragePanelFactoriesSourceNoSheetTokens() throws IOException {
		Path tPackage = mdkRoot().resolve("src").resolve("main").resolve("java")
				.resolve("gregtech6").resolve("gui").resolve("machines");
		for (String tFactory : new String[] {"GT6StorageMUI.java", "GT6HopperMUI.java", "GT6BatteryBoxMUI.java"}) {
			String tSource = Files.readString(tPackage.resolve(tFactory));
			assertFalse(tSource.contains("gui/machines"), tFactory + ": zero machines/ sheet tokens");
			assertFalse(tSource.contains("GuiTextures."), tFactory + ": zero vendored-sheet constants");
			assertFalse(tSource.contains(".background("), tFactory + ": zero code-side backgrounds (the theme base only)");
		}
	}

	// ---------------------------------------------------------------------------
	// the shared clean-base helpers (the storage-family gates' one copy)
	// ---------------------------------------------------------------------------

	/** The gt6 asset root, under the mdk root (the GT6PanelBaseThemeCensusTest walk). */
	private static final Path ASSETS_GT6 = Path.of("src", "main", "resources", "assets", "gt6");

	/** The derived base location form the theme JSON uses (the UITexture texture path). */
	static final String CLEAN_BASE_LOCATION = "gt6:textures/gui/parts/panel_base_176x166.png";

	/**
	 * The shipped gt6 theme as a live fork {@link Theme} — the modern.json
	 * {@code panel.background} merged over the fork default panel theme and parsed by the
	 * fork's own key codec, mounted on the reflection-built Theme (the
	 * GT6PanelBaseThemeCensusTest construction, verbatim). Shared by the three
	 * storage-family panel gates.
	 */
	static Theme gt6Theme() throws IOException {
		Path tMdk = mdkRoot();
		JsonObject tPanelJson;
		try (var tReader = Files.newBufferedReader(tMdk.resolve(ASSETS_GT6).resolve("themes").resolve("modern.json"))) {
			tPanelJson = JsonParser.parseReader(tReader).getAsJsonObject().getAsJsonObject("panel");
		}
		JsonObject tDefault = encodePanelTheme(DefaultTheme.INSTANCE.getWidgetTheme(IThemeApi.PANEL));
		WidgetTheme tParsed = IThemeApi.PANEL.parseJson(IThemeApi.PANEL.getMerger()
				.merge(tPanelJson, ImmutableJson.of(tDefault), ImmutableJson.of(tDefault)));
		WidgetThemeMap tMap = new WidgetThemeMap();
		tMap.register(IThemeApi.FALLBACK, IThemeApi.FALLBACK.getDefaultValue(), IThemeApi.FALLBACK.getDefaultHoverValue());
		tMap.register(IThemeApi.PANEL, tParsed, tParsed);
		try {
			java.lang.reflect.Constructor<Theme> tCtor = Theme.class.getDeclaredConstructor(
					String.class, brachy.modularui.api.ITheme.class, WidgetThemeMap.class);
			tCtor.setAccessible(true);
			return tCtor.newInstance("gt6", DefaultTheme.INSTANCE, tMap);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("the fork Theme constructor changed shape — re-pin", e);
		}
	}

	/**
	 * The clean-base takeover on a real factory panel: NO code-side background (zero
	 * machines/ path at the code face), the dispatch resolves the PANEL key and hands
	 * exactly the derived 9-slice base.
	 */
	static void assertCleanBaseHandsTo(ModularPanel<?> aPanel, String aWhat) throws IOException {
		assertNull(aPanel.getBackground(), aWhat + ": zero code-side background (the theme base only)");
		WidgetThemeEntry<?> tEntry = aPanel.getWidgetThemeInternal(gt6Theme());
		assertEquals(IThemeApi.PANEL, tEntry.key(), aWhat + ": the dispatch resolves the panel theme key");
		AdaptableUITexture tBase = assertInstanceOf(AdaptableUITexture.class, tEntry.theme().getBackground(),
				aWhat + ": the chained background is the 9-slice base texture");
		assertEquals(CLEAN_BASE_LOCATION, tBase.location.toString(), aWhat + ": the base location (zero machines/ path)");
		assertEquals(176, tBase.imageWidth(), aWhat + ": the base source width");
		assertEquals(166, tBase.imageHeight(), aWhat + ": the base source height");
		assertEquals(4, tBase.bl(), aWhat + ": 9-slice border left");
		assertEquals(4, tBase.bt(), aWhat + ": 9-slice border top");
		assertEquals(4, tBase.br(), aWhat + ": 9-slice border right");
		assertEquals(4, tBase.bb(), aWhat + ": 9-slice border bottom");
	}

	/** The fork default panel theme, encoded back to the JSON the merger/codec consume (the census form). */
	private static JsonObject encodePanelTheme(WidgetThemeEntry<?> aEntry) {
		var tBuilder = JsonOps.INSTANCE.mapBuilder();
		aEntry.encode(JsonOps.INSTANCE, tBuilder, true);
		JsonElement tEncoded = tBuilder.build(JsonOps.INSTANCE.empty()).result()
				.orElseThrow(() -> new AssertionError("encoding the fork default panel theme failed"));
		return tEncoded.getAsJsonObject();
	}

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() throws IOException {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new IOException("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}
}
