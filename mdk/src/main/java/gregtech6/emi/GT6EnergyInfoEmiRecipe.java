package gregtech6.emi;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregapi.code.TagData;
import gregtech6.jei.GT6EnergyCensus;
import gregtech6.jei.GT6RecipeMapIcons;
import gregtech6.jei.GT6RecipeMapViewerMeta;
import gregtech6.recipes.RecipeMap;

/**
 * One carrier's EMI face of the energy-source page (task energy-page-emi-twin) — the page
 * recipe the gear-port jump lands on, the approved 202x206 wireframe (header band /
 * produce / consume / converters / transfer). The sections come pre-packed as the
 * {@link Section} run built by {@link GT6EnergyInfoEmiCategory#pagesOf}; this class only
 * renders them, so every page provably fits the canvas whatever the census grows to.
 *
 * <p><b>THE DOUBLE HOOK</b> (the E1 red-proof fix — this class exists to mirror
 * {@code EmiInfoRecipe} exactly here): {@link #getInputs()} AND {@link #getOutputs()}
 * both return the pseudo carrier (the EmiInfoRecipe:41-48 double mount). EMI's
 * {@code EmiApi.displayRecipes} only consults the byOutput index, so the OUTPUT half is
 * what makes {@link GT6EmiPlugin#displayEnergyCarrierInfo} reach these pages; the INPUT
 * half keeps the mirror honest. {@link #supportsRecipeTree()} is false — a pseudo carrier
 * must never leak into the recipe tree. The pseudo stack's {@code copy()==this} identity
 * keeps the registration and click-time lookup keys unified
 * ({@link GT6EnergyCarrierEmiStack} contract).
 */
public class GT6EnergyInfoEmiRecipe implements EmiRecipe {

	/** The approved canvas (the material-tree twin form). */
	public static final int WIDTH = 202, HEIGHT = 206;

	/** The header band height — the body zone starts here ({@code HEIGHT - BODY_BUDGET} is the packer's budget). */
	static final int BODY_BUDGET = 28;

	// The E3-card lang keys — referenced here, written only by the lang wave (E5/E3);
	// until those rows land the labels display as raw keys, which is expected.
	public static final String GROUP_GENERATORS_KEY = "gt6.viewer.energy.group.generators";
	public static final String GROUP_PROCESSORS_KEY = "gt6.viewer.energy.group.processors";
	public static final String GROUP_CONVERTERS_KEY = "gt6.viewer.energy.group.converters";
	public static final String TRANSFER_KEY = "gt6.viewer.energy.transfer";
	public static final String EMPTY_KEY = "gt6.viewer.energy.empty";

	/** One consume-grid cell: the machine (the icon) and the recipe map it serves (the click target). */
	public record ConsumerCell(RecipeMap map, GT6RecipeMapIcons.Workstation workstation) {}

	private final TagData mCarrier;
	private final GT6EnergyCarrierEmiStack mStack;
	private final List<Section> mSections;
	private final ResourceLocation mId;

	/** Package-private: pages are built exclusively through the category's packer. */
	GT6EnergyInfoEmiRecipe(TagData aCarrier, List<Section> aSections, int aPage, int aPages) {
		mCarrier = aCarrier;
		mStack = GT6EnergyCarrierEmiStack.of(aCarrier);
		mSections = List.copyOf(aSections);
		mId = GT6EnergyInfoEmiCategory.pageId(aCarrier, aPage);
	}

	/** The page's carrier — the pseudo-stack identity face. */
	public TagData carrier() {
		return mCarrier;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return GT6EnergyInfoEmiCategory.CATEGORY;
	}

	@Override
	public ResourceLocation getId() {
		return mId;
	}

	/** The double hook, INPUT half (the EmiInfoRecipe:41 mirror). */
	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(mStack);
	}

	/** The double hook, OUTPUT half — the byOutput mount displayRecipes actually reads. */
	@Override
	public List<EmiStack> getOutputs() {
		return List.of(mStack);
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return HEIGHT;
	}

	/** A pseudo carrier is an information face, never a production step. */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	// -----------------------------------------------------------------------
	// the section model (built by the category packer, rendered below)
	// -----------------------------------------------------------------------

	/** One packed section: a family chunk, the empty state, or the transfer line. */
	record Section(Kind mKind, List<?> mCells, Component mLegend) {

		enum Kind { PRODUCE, CONSUME, CONVERT, PRODUCE_EMPTY, TRANSFER }

		/** The vertical pixels this section consumes (the packer's currency). */
		int height() {
			return switch (mKind) {
				case TRANSFER -> 12;
				case PRODUCE_EMPTY -> 25;
				default -> 11 + rows() * 18 + 4 + (mLegend == null ? 0 : 10);
			};
		}

		int rows() {
			return (mCells.size() + GT6EnergyInfoEmiCategory.COLUMNS - 1) / GT6EnergyInfoEmiCategory.COLUMNS;
		}
	}

	static Section produce(List<GT6EnergyCensus.Machine> aCells) {
		return new Section(Section.Kind.PRODUCE, aCells, null);
	}

	static Section consume(List<ConsumerCell> aCells) {
		return new Section(Section.Kind.CONSUME, aCells, null);
	}

	/** The converter section: a single distinct from→to pair rides as the on-page legend. */
	static Section convert(List<GT6EnergyCensus.Converter> aCells) {
		Component tLegend = null;
		if (!aCells.isEmpty()) {
			TagData tFrom = aCells.get(0).from(), tTo = aCells.get(0).to();
			boolean tSingle = true;
			for (GT6EnergyCensus.Converter tCell : aCells) {
				tSingle &= tCell.from() == tFrom && tCell.to() == tTo;
			}
			if (tSingle) {
				// the receiver must be a MutableComponent — energyUnit() hands back the
				// read-only Component interface, which carries no append (vanilla split)
				tLegend = Component.literal("")
						.append(GT6RecipeMapViewerMeta.energyUnit(tFrom))
						.append(Component.literal(" → "))
						.append(GT6RecipeMapViewerMeta.energyUnit(tTo))
						.append(Component.literal("  x" + aCells.size()));
			}
		}
		return new Section(Section.Kind.CONVERT, aCells, tLegend);
	}

	static Section transfer() {
		return new Section(Section.Kind.TRANSFER, List.of(), null);
	}

	static Section produceEmpty() {
		return new Section(Section.Kind.PRODUCE_EMPTY, List.of(), null);
	}

	/** The packed section run (the offline pin's read face). */
	List<Section> sections() {
		return mSections;
	}

	// -----------------------------------------------------------------------
	// the render
	// -----------------------------------------------------------------------

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		// the header band: the pseudo carrier (colored short code) + the long name + the
		// info body demoted to the subline (the approved wireframe; STEAM has neither a
		// long name nor an info key — the band degrades to the short code alone)
		aWidgets.add(new MachineIconWidget(mStack, 2, 2, List.of(mStack.getName()), null));
		String tLong = GT6RecipeMapViewerMeta.energyLongName(mCarrier);
		aWidgets.addText(Component.literal(tLong != null ? tLong : GT6RecipeMapViewerMeta.energyTypeShortCode(mCarrier)),
				24, 7, 0xFF000000, false);
		if (tLong != null) {
			aWidgets.addText(Component.translatable(GT6RecipeMapViewerMeta.energyInfoKey(mCarrier)),
					24, 17, 0xFF555555, false);
		}
		int tY = BODY_BUDGET;
		for (Section tSection : mSections) {
			tY = switch (tSection.mKind()) {
				case PRODUCE -> addGrid(aWidgets, tSection, tY, false);
				case CONSUME -> addGrid(aWidgets, tSection, tY, true);
				case CONVERT -> addGrid(aWidgets, tSection, tY, false);
				case PRODUCE_EMPTY -> {
					aWidgets.addText(Component.translatable(GROUP_GENERATORS_KEY), 4, tY, 0xFF000000, false);
					aWidgets.addText(Component.translatable(EMPTY_KEY), 4, tY + 11, 0xFF555555, false);
					yield tY + 25;
				}
				case TRANSFER -> {
					aWidgets.addText(Component.translatable(TRANSFER_KEY), 4, tY, 0xFF555555, false);
					yield tY + 12;
				}
			};
		}
	}

	/** One family grid: the icons, the per-cell hover lines, the optional click jump; returns the advanced y. */
	private int addGrid(WidgetHolder aWidgets, Section aSection, int aY, boolean aClickable) {
		aWidgets.addText(Component.translatable(labelKey(aSection.mKind())), 4, aY, 0xFF000000, false);
		int tY = aY + 11, tColumns = GT6EnergyInfoEmiCategory.COLUMNS;
		for (int i = 0; i < aSection.mCells().size(); i++) {
			int tX = 4 + (i % tColumns) * 18, tCellY = tY + (i / tColumns) * 18;
			switch (aSection.mKind()) {
				case PRODUCE -> {
					GT6EnergyCensus.Machine tMachine = (GT6EnergyCensus.Machine) aSection.mCells().get(i);
					EmiStack tStack = EmiStack.of(new ItemStack(tMachine.item().get()));
					aWidgets.add(new MachineIconWidget(tStack, tX, tCellY,
							List.of(tStack.getName(),
									Component.literal("→ ").append(GT6RecipeMapViewerMeta.energyUnit(mCarrier))),
							null));
				}
				case CONSUME -> {
					ConsumerCell tCell = (ConsumerCell) aSection.mCells().get(i);
					EmiStack tStack = EmiStack.of(GT6RecipeMapIcons.stackOf(tCell.workstation()));
					aWidgets.add(new MachineIconWidget(tStack, tX, tCellY,
							List.of(tStack.getName(), Component.literal(tCell.map().mNameLocal)),
							aClickable && tCell.map() != null ? () -> GT6EmiPlugin.openRecipeMapPage(tCell.map()) : null));
				}
				case CONVERT -> {
					GT6EnergyCensus.Converter tConverter = (GT6EnergyCensus.Converter) aSection.mCells().get(i);
					EmiStack tStack = EmiStack.of(new ItemStack(tConverter.machine().item().get()));
					aWidgets.add(new MachineIconWidget(tStack, tX, tCellY,
							List.of(tStack.getName(),
								Component.literal("")
										.append(GT6RecipeMapViewerMeta.energyUnit(tConverter.from()))
										.append(Component.literal(" → "))
										.append(GT6RecipeMapViewerMeta.energyUnit(tConverter.to()))),
							null));
				}
				default -> throw new IllegalStateException("non-grid section reached addGrid");
			}
		}
		tY += aSection.rows() * 18;
		if (aSection.mLegend() != null) {
			aWidgets.addText(aSection.mLegend(), 4, tY, 0xFF000000, false);
			tY += 10;
		}
		return tY + 4;
	}

	private static String labelKey(Section.Kind aKind) {
		return switch (aKind) {
			case PRODUCE, PRODUCE_EMPTY -> GROUP_GENERATORS_KEY;
			case CONSUME -> GROUP_PROCESSORS_KEY;
			case CONVERT -> GROUP_CONVERTERS_KEY;
			default -> throw new IllegalStateException("labelless section");
		};
	}

	/**
	 * One machine icon cell (the GearJumpWidget shape, plus a body): renders the stack
	 * bare (no slot frame — the material-tree machine-icon form), carries the hover
	 * lines, and when a click {@link Runnable} is injected takes the click (EMI's
	 * RecipeScreen routes clicks to every non-SlotWidget widget covering the cursor —
	 * the GearJumpWidget precedent; SlotWidget presses are deferred to release and would
	 * race the jump, so grid cells are plain Widgets). The click is bound at render time
	 * (production: the owning map's {@link GT6EmiPlugin#openRecipeMapPage}) so the
	 * offline pin drives clicks without EMI's static runtime.
	 */
	public static final class MachineIconWidget extends Widget {

		private final EmiStack mStack;
		private final int mX, mY;
		private final List<ClientTooltipComponent> mTooltip;
		private final Runnable mClick;

		public MachineIconWidget(EmiStack aStack, int aX, int aY, List<Component> aTooltip, Runnable aClick) {
			mStack = aStack;
			mX = aX;
			mY = aY;
			mTooltip = aTooltip.stream()
					.map(tLine -> ClientTooltipComponent.create(tLine.getVisualOrderText()))
					.toList();
			mClick = aClick;
		}

		@Override
		public Bounds getBounds() {
			return new Bounds(mX, mY, 18, 18);
		}

		@Override
		public void render(GuiGraphics aDraw, int aMouseX, int aMouseY, float aDelta) {
			mStack.render(aDraw, mX + 1, mY + 1, aDelta, -1);
		}

		@Override
		public List<ClientTooltipComponent> getTooltip(int aMouseX, int aMouseY) {
			return mTooltip;
		}

		@Override
		public boolean mouseClicked(int aMouseX, int aMouseY, int aButton) {
			if (mClick == null || !getBounds().contains(aMouseX, aMouseY)) return false;
			mClick.run();
			return true;
		}
	}
}
