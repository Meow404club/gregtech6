package gregtech6.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.InputConstants;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import gregapi.code.TagData;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.covers.GT6Covers;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GTMachines;

/**
 * The energy-source category of the JEI leg (task energy-page-jei-leg, the E3 card of the
 * energy-source-page wave) — the approved 形态A native static category page (the
 * material-tree 202x206 two-leg shape, the multiblock-preview twin), one recipe per page:
 * every carrier opens with ONE page ({@link GT6EnergyCensus#carriers} — the nine pinned
 * carriers plus STEAM as the tenth), and an overflowing family flows into further page
 * recipes of the SAME carrier (JEI's native page arrows — the E1 ruling that replaced the
 * 页底 tab wording; the EMI-side scroll-wheel problem is dodged by pagination outright).
 *
 * <p>Reachability (the E1 POC green light, research.e1-energy-poc q1): {@code show(focus)}
 * only limitFocus'es — every recipe mounts the carrier pseudo ingredient as an INPUT slot
 * ({@code uid = gt6:energy/<code>}, the {@link GT6EnergyCarrierJei} face), so the gear
 * port's {@link GT6JeiPlugin#openEnergyCarrierInfo} focus lands on this category
 * UNCONDITIONALLY — including the empty-carrier pages (CU/LU/MU produce nothing ported;
 * their pages still register, the E1 risk-2 clause). The nine addIngredientInfo text
 * pages retired WITH this category's registration (the same-commit clause — a transition
 * period with both categories would double-hit the same focus); the {@code
 * gt6.jei.info.energy.*} bodies the text pages carried are DEMOTED to the page header's
 * subline (the approved 降级 wording), STEAM has no info body and header-drives on the
 * slot's short code alone.
 *
 * <p>Page anatomy (the approved wireframe): header = the carrier slot + the info body;
 * the three family sections from {@link GT6EnergyCensus#familiesOf} — produce (hover-only
 * "emits" tooltip), consume (the flattened workstation grid, hover = machine name + the
 * owning map's energy row + the row's TIER_INPUTS window, click = the map's recipe page
 * through {@link GT6JeiPlugin#openRecipeMapPage} — the same by-construction JEI-present
 * face the GearJumpFace rides), convert (icon + the from→to colored short codes) — and
 * the one-line transfer footer. Empty sections draw the 空态 line ({@code
 * gt6.viewer.energy.empty}).
 *
 * <p>Both pinned JEI generations (15.62 forge / 19.52 neoforge) expose identical faces
 * here (read off the harvested API sources): {@code IRecipeCategory}, the slot builder,
 * {@code IRecipeWidget} (15.20/19.19 drawWidget floor), {@code IJeiInputHandler} — the
 * zero-fork surface; the single leg fork is the usual ResourceLocation constructor.
 */
public class GT6EnergyInfoJeiCategory implements IRecipeCategory<GT6EnergyInfoJeiCategory.Page> {

	/** {@code gt6:energy_info} — the EMI twin (E4) mirrors this uid one-to-one (the JEMI skip key). */
	public static final String UID_PATH = "energy_info";

	// The frozen E5 key set (the task card's 键集预定义 — E4 cites the same names).
	public static final String GROUP_GENERATORS_KEY = "gt6.viewer.energy.group.generators";
	public static final String GROUP_PROCESSORS_KEY = "gt6.viewer.energy.group.processors";
	public static final String GROUP_CONVERTERS_KEY = "gt6.viewer.energy.group.converters";
	public static final String TRANSFER_KEY = "gt6.viewer.energy.transfer";
	public static final String EMPTY_KEY = "gt6.viewer.energy.empty";
	/** Declared 6th key: the produce cells' hover line the wireframe demands (『发射 X』). */
	public static final String EMITS_KEY = "gt6.viewer.energy.emits";

	/** The category canvas (the material-tree precedent shape). */
	public static final int WIDTH = 202, HEIGHT = 206;

	// the fixed grid geometry: header band, then headered sections, transfer footer.
	private static final int HEADER_SLOT_X = 4, HEADER_SLOT_Y = 4;
	private static final int CONTENT_Y = 26, CONTENT_BOTTOM = 194, TRANSFER_Y = 197;
	private static final int X0 = 4, COLS = 8, CELL = 16, PITCH = 18, HEADER_H = 10;
	private static final int INK = 0xFF000000, INK_SOFT = 0xFF555555, INK_FAINT = 0xFFAAAAAA;

	private final RecipeType<Page> mRecipeType;

	public GT6EnergyInfoJeiCategory() {
		//? if forge {
		mRecipeType = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", UID_PATH), Page.class);
		//?} else {
		/*mRecipeType = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", UID_PATH), Page.class);
		 *///?}
	}

	@Override
	public RecipeType<Page> getRecipeType() {
		return mRecipeType;
	}

	/** The category chrome title — literal, the material-tree CATEGORY_TITLE precedent (no frozen key). */
	@Override
	public Component getTitle() {
		return Component.literal("Energy");
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	/** No icon (the material-tree precedent: JEI falls back to the title). */
	@Override
	public mezz.jei.api.gui.drawable.IDrawable getIcon() {
		return null;
	}

	/**
	 * The focus arm (the E1 green light): every page — overflow pages included, empty
	 * carriers included — mounts the carrier pseudo ingredient as the ONE INPUT slot, so
	 * {@code show(focus)} resolves this category for the whole census.
	 */
	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, Page aPage, IFocusGroup aFocuses) {
		aBuilder.addInputSlot(HEADER_SLOT_X, HEADER_SLOT_Y)
				.addIngredient(GT6EnergyCarrierJei.TYPE, aPage.carrier());
	}

	/**
	 * The per-cell widgets: the draw + tooltip hat on every cell, the input hat (the click
	 * jump) on consumer cells only — the one-instance-two-hats shape of the material-tree
	 * nav and the GearJumpFace.
	 */
	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder aBuilder, Page aPage, IFocusGroup aFocuses) {
		for (Section tSection : aPage.sections()) for (Cell tCell : tSection.cells()) {
			CellWidget tWidget = new CellWidget(tCell);
			aBuilder.addWidget(tWidget);
			if (tCell.map() != null) aBuilder.addInputHandler(tWidget);
		}
	}

	/**
	 * The static ink layer: the header lines (the demoted info body), the section headers,
	 * the empty-state line and the transfer footer. The cells draw themselves (widgets).
	 */
	@Override
	public void draw(Page aPage, mezz.jei.api.gui.ingredient.IRecipeSlotsView aRecipeSlotsView,
			GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		var tFont = Minecraft.getInstance().font;
		String[] tBody = infoBodyLines(aPage.carrier());
		if (tBody != null) {
			aGuiGraphics.drawString(tFont, tBody[0], HEADER_SLOT_X + CELL + 4, HEADER_SLOT_Y - 1, INK, false);
			if (tBody[1] != null) aGuiGraphics.drawString(tFont, tBody[1], HEADER_SLOT_X + CELL + 4, HEADER_SLOT_Y + 9, INK_SOFT, false);
		}
		for (Section tSection : aPage.sections()) {
			aGuiGraphics.drawString(tFont, Component.translatable(tSection.key()), X0, tSection.headerY(), INK_SOFT, false);
			if (tSection.cells().isEmpty())
				aGuiGraphics.drawString(tFont, Component.translatable(EMPTY_KEY), X0, tSection.headerY() + 2, INK_FAINT, false);
		}
		aGuiGraphics.drawString(tFont, Component.translatable(TRANSFER_KEY), X0, TRANSFER_Y, INK_SOFT, false);
	}

	/**
	 * The demoted info body of one carrier, split into the name line and the units line at
	 * the sentence seams (en ". " / zh "。") — the locale-correct 形式 of the wireframe's
	 * 『载体名 + 正文降级为副行』. {@code null} = the carrier has no info body (STEAM — the
	 * tenth page header-drives on the slot's colored short code alone).
	 */
	static String[] infoBodyLines(TagData aCarrier) {
		if (GT6RecipeMapViewerMeta.energyLongName(aCarrier) == null) return null;
		String tBody = Component.translatable(GT6RecipeMapViewerMeta.energyInfoKey(aCarrier)).getString();
		List<String> tSentences = List.of(tBody.split("(?<=\\.)\\s+|(?<=。)")).stream()
				.filter(tSentence -> !tSentence.isBlank()).toList();
		String tUnits = tSentences.size() > 1 ? String.join(" ", tSentences.subList(1, tSentences.size())) : null;
		return new String[] {tSentences.get(0), tUnits};
	}

	// -------------------------------------------------------------------
	// the page model + packer
	// -------------------------------------------------------------------

	/** One grid cell: produce ({@code to} set), consumer ({@code map} set), converter ({@code from} set). */
	public record Cell(int x, int y, java.util.function.Supplier<Item> item, String path,
			gregtech6.recipes.RecipeMap map, TagData from, TagData to) {}

	/** One section slice on one page: the group lang key, its header y and its positioned cells. */
	public record Section(String key, int headerY, List<Cell> cells) {}

	/** One page recipe of one carrier (the packer's output — the registration row). */
	public record Page(TagData carrier, int number, boolean produceEmpty, List<Section> sections) {}

	/**
	 * The pages of one carrier: the census families packed into the fixed canvas —
	 * generators, then processors, then converters, each cut at the content bottom and
	 * continued on the next page recipe of the same carrier (the approved 翻页 semantics).
	 * Consumers flatten to one cell per machine path (the first owning map wins — the
	 * click target); the walk needs {@code GT6RecipeMaps.init()} (the census contract).
	 */
	public static List<Page> pagesOf(TagData aCarrier) {
		GT6EnergyCensus.Families tFamilies = GT6EnergyCensus.familiesOf(aCarrier);
		boolean tEmpty = tFamilies.produce().isEmpty();

		List<Cell> tProduce = tFamilies.produce().stream()
				.map(tMachine -> new Cell(0, 0, tMachine.item(), tMachine.path(), null, null, aCarrier)).toList();

		Map<String, Cell> tSeen = new LinkedHashMap<>();
		for (GT6EnergyCensus.Consumer tConsumer : tFamilies.consume())
			for (GT6RecipeMapIcons.Workstation tWorkstation : tConsumer.workstations())
				tSeen.putIfAbsent(tWorkstation.path(), new Cell(0, 0, tWorkstation.item()::get, tWorkstation.path(),
						tConsumer.map(), null, null));
		List<Cell> tConsume = List.copyOf(tSeen.values());

		List<Cell> tConvert = tFamilies.convert().stream()
				.map(tConverter -> new Cell(0, 0, tConverter.machine().item(), tConverter.machine().path(),
						null, tConverter.from(), tConverter.to())).toList();

		List<Page> rPages = new ArrayList<>();
		List<Section> tSections = new ArrayList<>();
		int[] tY = {CONTENT_Y};
		int[] tNo = {0};
		pack(rPages, tSections, tY, tNo, aCarrier, tEmpty, GROUP_GENERATORS_KEY, gridRows(tProduce), true);
		pack(rPages, tSections, tY, tNo, aCarrier, tEmpty, GROUP_PROCESSORS_KEY, gridRows(tConsume), false);
		pack(rPages, tSections, tY, tNo, aCarrier, tEmpty, GROUP_CONVERTERS_KEY, converterRows(tConvert), false);
		if (!tSections.isEmpty()) rPages.add(new Page(aCarrier, tNo[0]++, tEmpty, List.copyOf(tSections)));
		return rPages;
	}

	/** The greedy packer for one section: headered 18px rows, cut and continued across pages. */
	private static void pack(List<Page> aPages, List<Section> aSections, int[] aY, int[] aNo,
			TagData aCarrier, boolean aEmpty, String aKey, List<List<Cell>> aRows, boolean aKeepEmpty) {
		if (aRows.isEmpty()) {
			// the generators section always opens page 0 — even empty (the 空态 clause)
			if (aKeepEmpty && aNo[0] == 0 && aY[0] + HEADER_H <= CONTENT_BOTTOM) {
				aSections.add(new Section(aKey, aY[0], List.of()));
				aY[0] += HEADER_H;
			}
			return;
		}
		int tIndex = 0;
		while (tIndex < aRows.size()) {
			if (aY[0] + HEADER_H + PITCH > CONTENT_BOTTOM) flushPage(aPages, aSections, aY, aNo, aCarrier, aEmpty);
			int tHeaderY = aY[0];
			aY[0] += HEADER_H;
			List<Cell> tCells = new ArrayList<>();
			while (tIndex < aRows.size() && aY[0] + PITCH <= CONTENT_BOTTOM) {
				List<Cell> tRow = aRows.get(tIndex++);
				int tCol = 0;
				for (Cell tCell : tRow) {
					boolean tConverter = tCell.map() == null && tCell.from() != null;
					int tX = tConverter ? tCell.x() : X0 + tCol++ * PITCH;
					tCells.add(new Cell(tX, aY[0], tCell.item(), tCell.path(), tCell.map(), tCell.from(), tCell.to()));
				}
				aY[0] += PITCH;
			}
			aSections.add(new Section(aKey, tHeaderY, List.copyOf(tCells)));
		}
	}

	private static void flushPage(List<Page> aPages, List<Section> aSections, int[] aY, int[] aNo,
			TagData aCarrier, boolean aEmpty) {
		aPages.add(new Page(aCarrier, aNo[0]++, aEmpty, List.copyOf(aSections)));
		aSections.clear();
		aY[0] = CONTENT_Y;
	}

	/** Fixed-grid rows: {@link #COLS} cells per row, unpositioned (x=y=0 — the packer places). */
	private static List<List<Cell>> gridRows(List<Cell> aCells) {
		List<List<Cell>> rRows = new ArrayList<>();
		for (int i = 0; i < aCells.size(); i += COLS)
			rRows.add(aCells.subList(i, Math.min(i + COLS, aCells.size())));
		return rRows;
	}

	/**
	 * Converter rows: sequential-width cells (icon + the from→to short codes, offline-safe
	 * 6px/char estimate — no Font touch in the packer), wrapped at the canvas edge.
	 */
	private static List<List<Cell>> converterRows(List<Cell> aCells) {
		List<List<Cell>> rRows = new ArrayList<>();
		List<Cell> tRow = new ArrayList<>();
		int tX = X0;
		for (Cell tCell : aCells) {
			int tWidth = PITCH + 6 * (codeLen(tCell.from()) + codeLen(tCell.to())) + 8;
			if (tX + tWidth > WIDTH - X0 && !tRow.isEmpty()) {
				rRows.add(List.copyOf(tRow));
				tRow = new ArrayList<>();
				tX = X0;
			}
			tRow.add(new Cell(tX, 0, tCell.item(), tCell.path(), null, tCell.from(), tCell.to()));
			tX += tWidth;
		}
		if (!tRow.isEmpty()) rRows.add(List.copyOf(tRow));
		return rRows;
	}

	private static int codeLen(TagData aCarrier) {
		return GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier).length();
	}

	// -------------------------------------------------------------------
	// the row-tier face (the hover's TIER_INPUTS window)
	// -------------------------------------------------------------------

	/** The basic-machine rows by registration path, built once (the census walk's sibling). */
	private static volatile Map<String, GTBasicMachineBlock.MachineRow> sBasicRows;

	private static Map<String, GTBasicMachineBlock.MachineRow> basicRows() {
		if (sBasicRows == null) {
			Map<String, GTBasicMachineBlock.MachineRow> rRows = new HashMap<>();
			for (List<GTBasicMachineBlock.MachineRow> tRows : List.of(
					GTMachines.DRYER_ROWS, GTMachines.CANNER_ROWS, GTMachines.CANNER_ULV_ROWS,
					GTMachines.PRESS_ROWS, GTMachines.EXTRUDER_ROWS, GTMachines.SIFTER_ROWS,
					GTMachines.SIFTER_ULV_ROWS, GTMachines.COMPRESSOR_ROWS, GTMachines.WIREMILL_ROWS,
					GTMachines.WIREMILL_ULV_ROWS, GTMachines.ROLLINGMILL_ROWS, GTMachines.ROLLINGMILL_RU_ROWS,
					GTMachines.ROLL_BENDER_ROWS, GTMachines.ROLL_FORMER_ROWS, GTMachines.CLUSTER_MILL_ROWS,
					GTMachines.MIXER_ROWS, GTMachines.ELECTRIC_MIXER_ROWS, GTMachines.LOOM_ROWS,
					GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_SIFTER_ROWS, GTMachines.BOXINATOR_ROWS,
					GTMachines.UNBOXINATOR_ROWS, GTMachines.FERMENTER_ROWS, GTMachines.POLARIZER_ROWS,
					GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.LASER_ENGRAVER_ROWS, GTMachines.LASER_WELDER_ROWS,
					GTMachines.FREEZER_ROWS, GTMachines.CRYO_MIXER_ROWS, GTMachines.MASSFAB_SMALL_ROWS,
					GTMachines.MOLECULAR_SCANNER_ROWS, GTMachines.REPLICATOR_ROWS, GTMachines.DISTILLERY_ROWS,
					GTMachines.BUZZSAW_ROWS, GTMachines.SQUEEZER_ROWS, GTMachines.CENTRIFUGE_ROWS,
					GTMachines.SLUICE_ROWS, GTMachines.SANDING_ROWS, GTMachines.PRESSURE_WASHER_ROWS,
					GTMachines.AUTOCRAFTER_ROWS, GTMachines.LIGHTNING_ROWS, GTMachines.LAMINATOR_ROWS,
					GTMachines.ELECTROLYZER_ROWS, GTMachines.INJECTOR_ROWS, GTMachines.PRINTER_ROWS,
					GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SLICER_ROWS, GTMachines.STEAM_CRACKER_ROWS,
					GTMachines.CATALYTIC_CRACKER_ROWS, GTMachines.COAGULATOR_ROWS, GTMachines.GENERIFIER_ROWS,
					GTMachines.BATH_ROWS, GTMachines.AUTOCLAVE_ROWS, GTMachines.SMELTER_ROWS,
					GTMachines.MELTER_ROWS, GTMachines.ROASTING_ROWS, GTMachines.BUMBLELYZER_ROWS,
					GTMachines.CRYSTALLISATION_ROWS, GTMachines.BURNER_MIXER_ROWS, GTMachines.PLANTALYZER_ROWS))
				for (GTBasicMachineBlock.MachineRow tRow : tRows) rRows.put(tRow.path(), tRow);
			// ponytail: a sibling walk of GT6RecipeMapIcons.build's table, not a shared seam — a
			// future family missing here only loses the hover tier line (graceful); unify via an
			// Icons-side row export if that ever stops being true.
			sBasicRows = Map.copyOf(rRows);
		}
		return sBasicRows;
	}

	/** The large-machine rows by path (the W3 twelve — window carried on the row itself). */
	private static volatile Map<String, GT6LargeMachines.LargeMachineRow> sLargeRows;

	private static Map<String, GT6LargeMachines.LargeMachineRow> largeRows() {
		if (sLargeRows == null) {
			Map<String, GT6LargeMachines.LargeMachineRow> rRows = new HashMap<>();
			for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) rRows.put(tRow.path(), tRow);
			sLargeRows = Map.copyOf(rRows);
		}
		return sLargeRows;
	}

	/**
	 * The hover tier line of one machine path — the TIER_INPUTS window the row's tier
	 * selects ({@code "LV 16–64 EU"}), the ULV window for the ulvVoltage rows, the
	 * EV window for the T5 arm; the large machines' own declared window. {@code null} =
	 * the path carries no row (ladders, towers, tools — no fabricated tier).
	 */
	static String tierLine(String aPath) {
		GTBasicMachineBlock.MachineRow tBasic = basicRows().get(aPath);
		if (tBasic != null) {
			String tCode = GT6RecipeMapViewerMeta.energyTypeShortCode(tBasic.energyType());
			if (tBasic.ulvVoltage()) return window("ULV", tCode, GTMachines.ULV_TIER_INPUTS);
			if (tBasic.tier() < GTMachines.TIER_INPUTS.length)
				return window(GT6Covers.TIER_NAMES[tBasic.tier() + 1], tCode, GTMachines.TIER_INPUTS[tBasic.tier()]);
			return window(GT6Covers.TIER_NAMES[5], tCode, GTMachines.EV_TIER_INPUTS);
		}
		GT6LargeMachines.LargeMachineRow tLarge = largeRows().get(aPath);
		if (tLarge != null) return window("",
				GT6RecipeMapViewerMeta.energyTypeShortCode(tLarge.energyType()),
				new long[] {tLarge.nbtInputMin(), tLarge.nbtInput(), tLarge.nbtInputMax()});
		return null;
	}

	private static String window(String aTier, String aCode, long[] aWindow) {
		String tHead = aTier.isEmpty() ? "" : aTier + " ";
		return tHead + aWindow[0] + "–" + aWindow[2] + " " + aCode;
	}

	// -------------------------------------------------------------------
	// the cell widget (draw + tooltip hat; the input hat joins for consumers)
	// -------------------------------------------------------------------

	static final class CellWidget implements IRecipeWidget, IJeiInputHandler {

		private final Cell mCell;
		private ItemStack mStack;

		CellWidget(Cell aCell) {
			mCell = aCell;
		}

		private ItemStack stack() {
			if (mStack == null) mStack = new ItemStack(mCell.item().get());
			return mStack;
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenPosition getPosition() {
			return new net.minecraft.client.gui.navigation.ScreenPosition(0, 0);
		}

		@Override
		public ScreenRectangle getScreenRectangle() {
			return new ScreenRectangle(mCell.x(), mCell.y(), CELL, CELL);
		}

		@Override
		public void drawWidget(GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
			aGuiGraphics.renderItem(stack(), mCell.x(), mCell.y());
			if (mCell.from() == null) return;
			var tFont = Minecraft.getInstance().font;
			int tX = mCell.x() + CELL + 2;
			// the converter's from→to colored short codes (the approved 色码短码 face)
			aGuiGraphics.drawString(tFont, GT6RecipeMapViewerMeta.energyUnit(mCell.from()), tX, mCell.y() + 4, INK, false);
			tX += tFont.width(GT6RecipeMapViewerMeta.energyUnit(mCell.from()));
			aGuiGraphics.drawString(tFont, "→", tX, mCell.y() + 4, INK_SOFT, false);
			tX += tFont.width("→");
			aGuiGraphics.drawString(tFont, GT6RecipeMapViewerMeta.energyUnit(mCell.to()), tX, mCell.y() + 4, INK, false);
		}

		@Override
		public void getTooltip(ITooltipBuilder aTooltip, double aMouseX, double aMouseY) {
			aTooltip.add(stack().getHoverName());
			if (mCell.map() != null) {
				// the owning map's energy row: the map title · the colored carrier code
				aTooltip.add(Component.translatable(GT6RecipeMapViewerMeta.titleKey(mCell.map()))
						.append(" · ").append(GT6RecipeMapViewerMeta.energyUnit(GT6RecipeMapViewerMeta.energyOf(mCell.map()))));
				String tTier = tierLine(mCell.path());
				if (tTier != null) aTooltip.add(Component.literal(tTier));
			} else if (mCell.from() != null) {
				aTooltip.add(GT6RecipeMapViewerMeta.energyUnit(mCell.from()).copy()
						.append(" → ").append(GT6RecipeMapViewerMeta.energyUnit(mCell.to())));
			} else {
				aTooltip.add(Component.translatable(EMITS_KEY, GT6RecipeMapViewerMeta.energyUnit(mCell.to())));
			}
		}

		@Override
		public ScreenRectangle getArea() {
			return getScreenRectangle();
		}

		@Override
		public boolean handleInput(double aMouseX, double aMouseY, IJeiUserInput aInput) {
			if (aInput.getKey().getType() != InputConstants.Type.MOUSE) return false;
			if (aInput.isSimulate()) return true; // mouse-down: this click can be handled
			GT6JeiPlugin.openRecipeMapPage(mCell.map()); // mouse-up: execute (runtime-guarded no-op offline)
			return true;
		}
	}
}
