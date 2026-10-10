package gregtech6.gui.machines;

import brachy.modularui.api.widget.IGuiAction;
import brachy.modularui.drawable.progress.ProgressDrawable;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

//? if forge {
import net.minecraftforge.fluids.FluidStack;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?}

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.GTViewerJump;
import gregtech6.recipes.Recipe;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The basic-machine ModularUI panel factory (task mui-a-panel-factory, batch A card 1 —
 * a NEW file, zero existing file touched). The {@link GTActMenu} shape: the panel builds on
 * SERVER and CLIENT inside {@link #buildPanel(GTBasicMachineMenu.Host, PanelSyncManager)}
 * (the sync handlers register there, IUIHolder.java:21-44 buildUI contract) — card 2's
 * open-chain wires the BE through this exact seam, so the signature is pinned.
 *
 * <p>Panel = the standard 176x166 machine GUI on the THEME BASE (r11-gui-basicmachine-clean-sample):
 * the code background is gone — the gt6 theme's 9-slice {@code panel.background}
 * ({@code parts/panel_base_176x166}, the machine-area-blank sheet with the printed player band,
 * task r11-gui-clean-base-theme) draws under the panel (fork Widget.drawBackground :255-264
 * paints the theme background first, then any code background on top — the old sheet-over-base
 * stack WAS the double-layer bug, so the sheet arm is simply removed). The per-machine mGUIPath
 * sheets and the vanilla screen that parsed them retired with the vanilla leg (task
 * basic-machines-mui-migration).
 *
 * <p>Title (F1-01): the machine display name as a {@link TextWidget} at the upstream print
 * position (8, 4) in the 0x404040 ink — the drawGuiContainerForegroundLayer arm
 * (ContainerClientBasicMachine.java:44). The port source is the served RecipeMap row's local
 * name (the upstream {@code LH.get(mRecipes.mNameInternal)} arm; the port has no
 * custom-inventory-name concept, so that upstream branch has no equivalent). A Host without a
 * map (the fakes) renders no title.
 *
 * <p>Slot topology is the upstream case table consumed through the {@link
 * GT6MachineGuiLayout} descriptor (task gui-layout-descriptor — the faithful per-case
 * transcription of ContainerCommonBasicMachine.java:51-156/:158-263, fluid arms included):
 * {@link GT6MachineGuiLayout#inputPositions} for the input seats (the case-3+ shapes the
 * old {@code GTBasicMachineMenu.inputSlotPos} extrapolation got wrong) and {@link
 * GT6MachineGuiLayout#outputPositions} for the outputs (:169-270 — Shredder/Crusher land
 * on the 12 case :247-269, the Lathe on the 2 case :178-181). The fluid arm reads the same
 * tank-bank lengths the display seats render. Inputs are open {@link ModularSlot}s; the
 * outputs carry {@code canPut(false)} — the upstream Slot_Normal.setCanPut(F),
 * ContainerCommonBasicMachine.java:162 (items come out, nothing goes in).
 *
 * <p>Progress rides the MUI sync-value face: a {@link DoubleSyncValue} fed by
 * {@link #progressRatio} (the normalization wrapper over the three-state
 * {@link GTBasicMachineMenu#progressValue} — the RCON-shared function stays untouched),
 * consumed by a {@link ProgressWidget}; the bar visual is the amazawa arrow PAIR — the
 * empty cell ({@link GT6GuiParts#ARROW_OUTLINE}, the machine skins' print at (78,24),
 * always drawn) under the clipped fill ({@link GT6GuiParts#ARROW_FORWARD}) — and the
 * fill direction
 * is the served map's upstream {@code mProgressBarDirection} case mapped through
 * {@link #progressDirection} (F1-02: cases 0-3 grow, 4-7 drain — the drain face feeds
 * {@code 1 - ratio}).
 *
 * <p>Arrow affordances (task machine-arrow-recipe-jump, the research card
 * {@code research.machine-arrow-recipe-jump}): the arrow wears a hover tooltip (the
 * current recipe's output name + progress percent) and a click that opens the served
 * RecipeMap page in whichever recipe viewer is installed ({@link GTViewerJump}, EMI
 * first). Both ride fork-native widget faces with zero fork changes: hover =
 * {@code Widget.drawForeground} (fork Widget.java:305-310) + {@link RichTooltip}
 * {@code tooltipBuilder}/{@code autoUpdate} (RichTooltip.java:71/:77, the FluidSlot
 * :59-63 in-service form); click = {@code listenGuiAction} MousePressed behind the
 * mandatory {@code isMouseAbove} gate (Widget.java:702-711, GuiContext.java:98-99 — the
 * official fork javadoc example is this exact ProgressWidget+click shape,
 * Widget.java:954-993). The "processing" identity rides the {@code bm_recipe}
 * {@link StringSyncValue}: the server getter derives the key from
 * {@link GTBasicMachineMenu.Host#getCurrentRecipe} ONLY while
 * {@code progressValue >= 0} — the upstream {@code mCurrentRecipe} is "last applied"
 * and never cleared (the TRAP), so the three-state progress is the gate.
 *
 * <p>Composed furniture (composed-ui-energy-slot-and-parts, the five-missing field
 * report): the special-slot gear cell ({@link GT6GuiParts#SLOT_SPECIAL} at (77,60) —
 * the print every machine skin carries and the theme base erased) rides as pure decor,
 * the title prints TRANSLATED (the shared viewer title key — zh rows ship for every
 * visible map) and CENTERED across the panel width, and the player-inventory label
 * (vanilla {@code container.inventory}) rides at the vanilla offset above the (7,84)
 * block.
 *
 * <p>Fluid seats (settled, task gui-basicmachine-fluids — the Option B ruling that
 * redeemed the batch-A boundary declared here before): the two {@link Host} fluid banks
 * (the {@link GTBasicMachineMenu.Host#getFluidInputTanks}/{@link GTBasicMachineMenu.Host#getFluidOutputTanks}
 * default-empty seams, GTBasicMachineMenu.java:117/:122) render one read-only display seat
 * per declared tank at the upstream {@code Slot_Render} geometry
 * ({@link GTBasicMachineMenu#fluidDisplayPos}: inputs descending from (53,63) right-to-left,
 * outputs ascending from (107,63) left-to-right, both wrapping upward every 3). The seat
 * form is the {@link GTDistillationTowerMUI} shape (GenericSyncValue.forFluid with the
 * null→EMPTY gate, the {@link FluidTankGT#bindInt} drawn-capacity clamp, the amount text
 * off — the upstream seat is icon-only), registered under the named sync keys
 * {@code bm_fluid_in_<i>} / {@code bm_fluid_out_<i>}. A zero-fluid RecipeMap Host (the
 * default banks) renders zero seats — the pre-p34 panel byte-identical. Each seat wears the
 * amazawa droplet frame (F2-01: the sheet-baked frame is gone with the sheet, so the
 * {@link GT6GuiParts#SLOT_FLUID} 18x19 crop rides the widget as its code background).
 *
 * <p>NOT in this card: the vanilla MenuType deregistration (card 3, done), BE/Block wiring
 * (card 2, done). The player-inventory widget is added UNCONDITIONALLY (issue #3: the old
 * {@code getContainer() == null} gate was always-true at runtime — the fork's GuiManager.open
 * runs createPanel before menu.construct, so getContainer() is still null while the panel
 * builds; the widget binds by sync key and needs no container, SlotGroupWidget.playerInventory
 * + the ModularSyncManager.construct auto-bind, GTCEu Modern MachineUIPanel.java:76-82 shape).
 */
public final class GTBasicMachineMUI {

	/** The panel name (the MUI2 main-panel key). */
	public static final String PANEL_NAME = "basic_machine";

	/** The sync key of the progress ratio value (server getter = {@link #progressRatio}). */
	public static final String SYNC_PROGRESS = "bm_progress";

	/**
	 * The sync key of the current-recipe identity (task machine-arrow-recipe-jump): a
	 * {@link StringSyncValue} the server getter feeds from
	 * {@link GTBasicMachineMenu.Host#getCurrentRecipe} through {@link #currentRecipeKey},
	 * gated on the three-state progress (idle syncs {@code null}). The gate rides the getter
	 * on purpose — the upstream {@code mCurrentRecipe} is "last applied" (assigned at
	 * MultiTileEntityBasicMachine.java:757, never cleared), so a non-null field is NOT a
	 * processing predicate; {@code progressValue >= 0} is.
	 */
	public static final String SYNC_RECIPE = "bm_recipe";

	/**
	 * The sync-key prefix of the input fluid seats: {@code bm_fluid_in_} + the tank index
	 * (task p34, one seat per {@link Host#getFluidInputTanks} element).
	 */
	public static final String SYNC_FLUID_IN = "bm_fluid_in_";

	/**
	 * The sync-key prefix of the output fluid seats: {@code bm_fluid_out_} + the tank index
	 * (task p34, one seat per {@link Host#getFluidOutputTanks} element).
	 */
	public static final String SYNC_FLUID_OUT = "bm_fluid_out_";

	/** The slot group of the input seat(s) — the shift-transfer source face. */
	public static final String GROUP_INPUTS = "bm_inputs";

	/** The slot group of the output grid — the shift-transfer destination face. */
	public static final String GROUP_OUTPUTS = "bm_outputs";

	/**
	 * The progress arrow seat — the upstream arrow CELL verbatim (78, 24, 20x18,
	 * ContainerClientBasicMachine.java:57 case-0 blit; the retired vanilla screen's ARROW_*
	 * constants carried the same numbers), so the family draws the bar where the upstream
	 * skins printed it.
	 */
	private static final int PROGRESS_X = 78, PROGRESS_Y = 24, PROGRESS_W = 20, PROGRESS_H = 18;

	/** The title print row — upstream drew at (8, 4), ContainerClientBasicMachine.java:44; the composed panel keeps the row, centered. */
	private static final int TITLE_Y = 4;

	/** The title ink — the upstream drawString color 4210752 (0x404040), the vanilla inventory-label ink too. */
	private static final int TITLE_COLOR = 0x404040;

	/**
	 * The special-slot (gear) cell — the 22x22 print EVERY machine skin carries at
	 * (77,60) (upstream 1.7.10 Default.png and the amazawa redraw agree; the crop is
	 * {@link GT6GuiParts#SLOT_SPECIAL}). Composed back onto the clean base
	 * (composed-ui-energy-slot-and-parts, the user ruling slot_special_22x22 未拼): the
	 * theme base flattened the panel interior, erasing the print the skins show. Pure
	 * decor — no slot is seated (GT6MachineGuiLayout.SPECIAL_SLOT stays a declared
	 * functional coordinate no map serves on this leg).
	 */
	private static final int GEAR_SLOT_X = 77, GEAR_SLOT_Y = 60, GEAR_SLOT_SIZE = 22;

	/** The player-inventory label row — vanilla's {@code imageHeight - 94} = 72 on the 166 panel, above the (7,84) block. */
	private static final int INV_LABEL_X = 8, INV_LABEL_Y = 72;

	private GTBasicMachineMUI() {
	}

	/**
	 * The panel body — runs on SERVER and CLIENT (the sync handlers must exist on both,
	 * the GTActMenu.buildPanel :73-105 shape). This is the pinned seam card 2 consumes.
	 */
	public static ModularPanel<?> buildPanel(GTBasicMachineMenu.Host aHost, PanelSyncManager aSyncManager) {
		return buildPanel(aHost, aSyncManager, null);
	}

	/**
	 * The title-override seam (task oven-gui-arrow-align, the sanctioned minimal
	 * parameterization): a non-null {@code aTitleOverride} prints instead of the map-title
	 * arm. The oven ladder needs it — the Furnace map is viewer-EXCLUDED
	 * (GT6RecipeMapViewerMeta's exclusion table: the vanilla smelting mirror), so no
	 * {@code gt6.jei.recipe_map.furnace} key exists and the family arm would print the raw
	 * key; the block name ("Oven (Steel)") is the per-tier title the vanilla GUI showed.
	 * The override Component arrives already localizable (a vanilla translatable).
	 */
	public static ModularPanel<?> buildPanel(GTBasicMachineMenu.Host aHost, PanelSyncManager aSyncManager,
			@Nullable Component aTitleOverride) {
		GTItemStackHandler tInventory = aHost.getInventory();
		int tInputs = aHost.getInputSlotCount();
		int tOutputs = aHost.getOutputSlotCount();

		aSyncManager.registerSlotGroup(GROUP_INPUTS, Math.max(1, tInputs));
		aSyncManager.registerSlotGroup(GROUP_OUTPUTS, 3);
		// the player-inventory SYNC face rides the fork auto-bind (ModularSyncManager.construct
		// :68-70 — it runs after the panel builds, when the menu exists; an explicit
		// bindPlayerInventory here would NPE on the null menu)

		// the title — the upstream foreground arm (ContainerClientBasicMachine.java:44), the
		// served map's local name; a map-less Host (the fakes) renders no title
		gregtech6.recipes.RecipeMap tMap = aHost.getRecipeMap();

		// the progress sync — the direction case table decides grow vs drain (drain feeds
		// 1 - ratio, idle gated to 0 = the upstream mProgressBar >= 0 draw arm)
		ProgressDirection tDirection = progressDirection(tMap == null ? 0 : tMap.mProgressBarDirection);
		DoubleSyncValue tProgress = tDirection.drain()
				? new DoubleSyncValue(() -> GTBasicMachineMenu.progressValue(aHost) < 0 ? 0.0D : 1.0D - progressRatio(aHost))
				: new DoubleSyncValue(() -> progressRatio(aHost));
		aSyncManager.syncValue(SYNC_PROGRESS, tProgress);

		// the recipe identity sync (machine-arrow-recipe-jump): server-authoritative — the
		// three-state progress gate decides "processing" (the mCurrentRecipe field alone is
		// "last applied" and never cleared, the research-card TRAP); idle syncs null so the
		// client tooltip never shows a stale recipe
		StringSyncValue tRecipeKey = new StringSyncValue(() -> GTBasicMachineMenu.progressValue(aHost) >= 0
				? currentRecipeKey(aHost.getCurrentRecipe())
				: null);
		aSyncManager.syncValue(SYNC_RECIPE, tRecipeKey);

		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, 176, 166);
		// NO code background: the gt6 theme's 9-slice panel base draws here (fork
		// Widget.drawBackground :255-264 theme arm) — the old .background(mGUIPath sheet) is
		// the removed half of the double-layer stack (r11-gui-basicmachine-clean-sample)

		if (aTitleOverride != null) {
			// the oven seam (see the overload doc): the caller's Component — already
			// localizable — in the same centered row/ink the map title wears
			tPanel.child(new TextWidget<>(aTitleOverride)
					.pos(0, TITLE_Y).size(176, 9)
					.textAlign(Alignment.TopCenter)
					.color(() -> TITLE_COLOR)
					.name("title"));
		} else if (tMap != null) {
			// vanilla Component.literal, NOT the fork Text.str → ModularComponent chain: the
			// fork builds MutableComponent through its AT-widened package-private ctor (fork
			// accesstransformer.cfg) — fine in game (Forge merges every mod's AT), but the
			// offline test JVM's minecraft artifact carries no AT, so a String-built title
			// would IllegalAccessError every map-bearing panel at build (all six pin tests
			// hit it). Plain text draws identically through the same TextWidget face.
			//
			// Title face (composed-ui-energy-slot-and-parts, the user rulings 标题没汉化 +
			// 标题没居中): TRANSLATABLE through the shared viewer title-key formula
			// (GT6RecipeMapViewerMeta.titleKey — the same key the JEI/EMI categories print;
			// en = the map's mNameLocal verbatim, zh rows ship in both datagen locales for
			// every visible map, so the in-game title localizes with zero new keys — a
			// non-visible map would fall back to printing the key, and no MUI-leg host
			// serves one today), and CENTERED across the 176 panel width (upstream printed
			// top-left at (8,4); the composed panel owns the full-width row).
			tPanel.child(new TextWidget<>(Component.translatable(gregtech6.jei.GT6RecipeMapViewerMeta.titleKey(tMap)))
					.pos(0, TITLE_Y).size(176, 9)
					.textAlign(Alignment.TopCenter)
					.color(() -> TITLE_COLOR)
					.name("title"));
		}

		// the input seats — the GT6MachineGuiLayout case table, the fluid arm riding the same
		// tank-bank length the display seats render (every 1/2-input family keeps its exact
		// geometry: cases 0-2 have no seat-count-visible arm at ≤6 fluids)
		FluidTankGT[] tInTanks = aHost.getFluidInputTanks();
		FluidTankGT[] tOutTanks = aHost.getFluidOutputTanks();
		int[][] tInPos = GT6MachineGuiLayout.inputPositions(tInputs, tInTanks.length);
		for (int i = 0; i < tInputs; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + i).slotGroup(GROUP_INPUTS))
					.pos(tInPos[i][0], tInPos[i][1])
					.name("input_" + i));
		}
		// the output grid — canPut(false) = the upstream setCanPut(F), :162
		int[][] tOutPos = GT6MachineGuiLayout.outputPositions(tOutputs, tOutTanks.length);
		for (int i = 0; i < tOutputs; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + tInputs + i).canPut(false).slotGroup(GROUP_OUTPUTS))
					.pos(tOutPos[i][0], tOutPos[i][1])
					.name("output_" + i));
		}
		// the fluid display seats — the Host bank seams consumed live (the
		// GTBasicMachineMenu.java:117/:122 default-empty faces): one read-only seat per
		// declared tank at the upstream :267/:268 Slot_Render geometry (fluidDisplayPos);
		// a default-bank Host renders zero seats — the pre-p34 panel byte-identical
		for (int i = 0; i < tInTanks.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(false, i);
			tPanel.child(fluidSeat(aSyncManager, tInTanks[i], SYNC_FLUID_IN + i)
					.pos(tPos[0], tPos[1])
					.name("fluid_in_" + i));
		}
		for (int i = 0; i < tOutTanks.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(true, i);
			tPanel.child(fluidSeat(aSyncManager, tOutTanks[i], SYNC_FLUID_OUT + i)
					.pos(tPos[0], tPos[1])
					.name("fluid_out_" + i));
		}
		// the special-slot gear cell — the skins' universal print, composed back onto the
		// clean base (the theme base flattened the interior and erased it)
		tPanel.child(GT6GuiParts.asUITexture(GT6GuiParts.SLOT_SPECIAL).asWidget()
				.pos(GEAR_SLOT_X, GEAR_SLOT_Y).size(GEAR_SLOT_SIZE, GEAR_SLOT_SIZE)
				.name("special_slot"));
		// the progress bar — the amazawa arrow pair, the EMPTY cell (the skins' print at
		// (78,24), part 13 arrow_outline) always drawn under the clipped fill: the
		// fill-only form read as "进度箭头缺失" on an idle/blank cell.
		// machine-arrow-recipe-jump: the arrow also wears the recipe/progress hover tooltip
		// (RichTooltip autoUpdate + tooltipBuilder — the fork FluidSlot.java:59-63 form;
		// hover draw rides Widget.drawForeground, fork Widget.java:305-310) and the
		// viewer-jump click (listenGuiAction MousePressed with the MANDATORY isMouseAbove
		// gate — listeners are dispatched screen-level BEFORE widget hit-testing, fork
		// Widget.java:690-694/:702-711; the official javadoc example is exactly this
		// ProgressWidget+click shape, Widget.java:954-993).
		ProgressWidget tArrow = new ProgressWidget()
				.value(tProgress)
				.progress(new ProgressDrawable()
						.emptyTexture(GT6GuiParts.asUITexture(GT6GuiParts.ARROW_OUTLINE))
						.filledTexture(GT6GuiParts.asUITexture(GT6GuiParts.ARROW_FORWARD))
						.direction(tDirection.direction()))
				.pos(PROGRESS_X, PROGRESS_Y)
				.size(PROGRESS_W, PROGRESS_H);
		tArrow.tooltip().autoUpdate(true).tooltipBuilder(
				aTooltip -> addArrowTooltip(aTooltip, tRecipeKey, tProgress, tDirection));
		tArrow.listenGuiAction((IGuiAction.MousePressed) (aContext, aButton) -> {
			if (!aContext.isMouseAbove(tArrow)) return false;
			// the family jump router (the retired vanilla screen's click face, GT_RectHandler
			// lineage): the map's recipe page, EMI first/JEI fallback/neither a silent no-op
			// (false = vanilla click handling continues)
			return tMap != null && GTViewerJump.openRecipeMapPage(tMap);
		});
		tPanel.child(tArrow);

		// the player-inventory label — vanilla's "container.inventory" (物品栏 in zh_cn;
		// the vanilla key, zero new lang rows) at the vanilla offset (imageHeight-94 = 72),
		// the same ink as the title. The printed band kept the slots but no label — the
		// missing "物品栏" report
		tPanel.child(new TextWidget<>(Component.translatable("container.inventory"))
				.pos(INV_LABEL_X, INV_LABEL_Y)
				.color(() -> TITLE_COLOR)
				.name("inventory_label"));

		// the player inventory at the standard 176x166 machine-panel offset 84
		// (bindPlayerInventory(84) semantics: the 3x9 block at (7,84), the hotbar at (7,142));
		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		return tPanel;
	}

	/**
	 * The progress ratio in [0, 1] — the MUI widget face over the three-state
	 * {@link GTBasicMachineMenu#progressValue} (ContainerCommonBasicMachine.java:280-286
	 * verbatim, the RCON-shared function stays untouched): the success flag
	 * ({@code PROGRESS_DONE}) maps to 1.0, the idle {@code -1} maps to 0.0, anything else
	 * divides down by {@code PROGRESS_DONE} (the exact fill arithmetic the retired vanilla
	 * screen's bar ran).
	 */
	public static double progressRatio(GTBasicMachineMenu.Host aHost) {
		int tValue = GTBasicMachineMenu.progressValue(aHost);
		if (tValue >= GTBasicMachineMenu.PROGRESS_DONE) return 1.0D;
		if (tValue < 0) return 0.0D;
		return (double) tValue / GTBasicMachineMenu.PROGRESS_DONE;
	}

	/**
	 * The cross-JVM identity key of the currently applied recipe — the {@code bm_recipe}
	 * wire form (task machine-arrow-recipe-jump). The first non-empty OUTPUT decides: items
	 * first ({@code i:} + the item registry id), then fluids ({@code f:} + the fluid
	 * registry id); no recipe / no output → {@code null} (the tooltip gate).
	 *
	 * <p>Wire-form rationale (the research card's two candidates): {@code mapName +
	 * ROW_ORDER index} would need the O(n log n) resort of the whole map on every sync
	 * poll (the getter runs server-side each tick) and pins this file to the EMI plugin's
	 * private comparator; the first-output registry id is O(outputs), renders directly as
	 * the tooltip's output name client-side ({@link #recipeKeyDisplayName}), and round-trips
	 * offline as a bare {@link String} (the StringSyncValue pin). The key is tooltip DISPLAY
	 * only — the click jump opens the RecipeMap page (v1 scope), so no recipe-exact identity
	 * is needed; v2's exact-page jump would revisit (the ROW_ORDER near-duplicate tie then
	 * becomes load-bearing, GT6RecipeMapEmiRecipe.java:32-34).
	 */
	@Nullable
	public static String currentRecipeKey(@Nullable Recipe aRecipe) {
		if (aRecipe == null) return null;
		for (ItemStack tStack : aRecipe.mOutputs) {
			if (tStack != null && !tStack.isEmpty()) return "i:" + BuiltInRegistries.ITEM.getKey(tStack.getItem());
		}
		for (FluidStack tStack : aRecipe.mFluidOutputs) {
			if (tStack != null && !tStack.isEmpty()) return "f:" + BuiltInRegistries.FLUID.getKey(tStack.getFluid());
		}
		return null;
	}

	/**
	 * The display name of a {@link #currentRecipeKey} — the output's localized name,
	 * resolved HERE on the client so the locale is the viewer's (a server-side name would
	 * bake en_us on dedicated servers). Unknown ids degrade to the registry defaults, never
	 * throw. Public for the pin test.
	 */
	public static Component recipeKeyDisplayName(String aKey) {
		String tId = aKey.substring(2);
		int tColon = tId.indexOf(':');
		ResourceLocation tRegistryId = tColon < 0
				? ResourceLocation.fromNamespaceAndPath("minecraft", tId)
				: ResourceLocation.fromNamespaceAndPath(tId.substring(0, tColon), tId.substring(tColon + 1));
		if (aKey.startsWith("f:")) {
			return new FluidStack(BuiltInRegistries.FLUID.get(tRegistryId), 1).getDisplayName();
		}
		return new ItemStack(BuiltInRegistries.ITEM.get(tRegistryId)).getHoverName();
	}

	/**
	 * The progress-arrow hover tooltip body (the {@code tooltipBuilder} consumer — runs on
	 * the client each draw frame via {@code autoUpdate(true)}, the FluidSlot.java:59-63
	 * form): the current recipe's output name, then the progress percent. THE GATE: no
	 * synced key (idle or recipe-less) → no lines at all — the "processing" truth is the
	 * three-state progress the server getter gated on, never the never-cleared
	 * {@code mCurrentRecipe} field (the research-card TRAP). Package-visible for the pin
	 * test.
	 */
	static void addArrowTooltip(RichTooltip aTooltip, StringSyncValue aRecipeKey, DoubleSyncValue aProgress,
			ProgressDirection aDirection) {
		String tKey = aRecipeKey.getValue();
		if (tKey == null || tKey.isEmpty()) return;
		aTooltip.addLine(recipeKeyDisplayName(tKey));
		double tShown = Math.min(1.0D, Math.max(0.0D, aProgress.getValue())); // the synced fill value
		double tTrueRatio = aDirection.drain() ? 1.0D - tShown : tShown; // undo the drain display inversion
		// Component.literal, NOT the addLine(String) overload — the String arm rides the
		// fork Text.str → ModularComponent chain, whose AT-widened MutableComponent ctor
		// IllegalAccessErrors on the offline test JVM (the same face the title avoids,
		// see the buildPanel title note); vanilla Components draw identically here.
		aTooltip.addLine(Component.literal((int) Math.round(100.0D * tTrueRatio) + "%"));
	}

	/**
	 * One read-only fluid seat — the {@link GTDistillationTowerMUI} seat form (:156-171)
	 * mirrored: that file is frozen for this card (zero diff), so the three reuse elements
	 * ride here instead of a shared helper — the {@link GenericSyncValue#forFluid} sync over
	 * the tank's live stack (null → EMPTY: the getter must not return null), the drawn
	 * capacity clamped through {@link FluidTankGT#bindInt}, the amount text off (the upstream
	 * Slot_Render seat is icon-only, ContainerClientBasicMachine draws no amount text).
	 *
	 * <p>Unlike the tower form the value is REGISTERED under its named sync key here (the
	 * {@code bm_fluid_in_<i>}/{@code bm_fluid_out_<i>} contract, task p34): an unregistered
	 * widget-carried handler would land under the
	 * {@code WidgetTree.collectSyncValues} auto key ({@code auto:<panel>} + a running id) —
	 * the same shape the {@link #SYNC_PROGRESS} registration already rides.
	 */
	private static FluidDisplayWidget fluidSeat(PanelSyncManager aSyncManager, FluidTankGT aTank, String aSyncKey) {
		// the leg-generic declaration (the tower :157-160 note): the forge factory returns
		// GenericSyncValue<FluidStack>, the neoforge one
		// GenericSyncValue<RegistryFriendlyByteBuf, FluidStack> — the same call shape, the
		// var keeps one body over both (FluidDisplayWidget.value takes the IValue face both
		// implement)
		var tValue = GenericSyncValue.forFluid(
				() -> {
					FluidStack tContent = aTank.fluid();
					return tContent == null ? FluidStack.EMPTY : tContent;
				},
				null);
		aSyncManager.syncValue(aSyncKey, tValue);
		return new FluidDisplayWidget()
				.value(tValue)
				.capacity(FluidTankGT.bindInt(aTank.getCapacity()))
				.displayAmount(false)
				// the droplet frame (F2-01): the sheet-baked frame is gone with the sheet —
				// the amazawa 18x19 fluid cell rides as the code background. The theme's own
				// fluidSlot frame (modern.json → slot_frame_18x18, the gui-slot-theme card)
				// would double-frame underneath (fork Widget.drawBackground :255-264 theme arm
				// first, code second), so the theme arm is disabled for the seat.
				// The widget box stays the default 18x18; the 1px lip rides the squeeze —
				// ponytail: revisit at the tower card if the frame shows it
				.disableThemeBackground(true)
				.background(GT6GuiParts.asUITexture(GT6GuiParts.SLOT_FLUID));
	}

	/**
	 * One upstream progress-direction case: the MUI {@link ProgressDrawable.Direction} the
	 * fill grows in, plus the drain flag (upstream cases 4-7 invert the step —
	 * {@code tProgress = tSize - tProgress}, ContainerClientBasicMachine.java:61-64 — so the
	 * arrow shows the REMAINING work; the panel feeds {@code 1 - ratio} then).
	 *
	 * @param direction the MUI fill direction
	 * @param drain     true = the fill drains as the machine runs (the value face inverts)
	 */
	public record ProgressDirection(ProgressDrawable.Direction direction, boolean drain) {
	}

	/**
	 * The upstream {@code mProgressBarDirection} case table (0-7) mapped onto the MUI
	 * direction quartet (F1-02) — the verbatim geometry read:
	 * <ul>
	 * <li>0 {@code (x+78, y+24, w=tP)} — grows right → {@code RIGHT}/grow;</li>
	 * <li>1 {@code (x+78+20-tP, w=tP)} — anchored right → {@code LEFT}/grow;</li>
	 * <li>2 {@code (h=tP)} — grows down → {@code DOWN}/grow (the Sifter/Anvil maps);</li>
	 * <li>3 {@code (y+24+18-tP)} — anchored bottom → {@code UP}/grow;</li>
	 * <li>4-7 — the 0-3 twins with the step inverted → same quartet, drain (the Hammer map
	 *     lives on case 6).</li>
	 * </ul>
	 * Out-of-range bytes ride case 0 (the upstream switch's silent no-draw fallback, rendered
	 * here as the plain grow-right bar).
	 */
	public static ProgressDirection progressDirection(int aUpstreamDirection) {
		return switch (aUpstreamDirection) {
			case 1 -> new ProgressDirection(ProgressDrawable.Direction.LEFT, false);
			case 2 -> new ProgressDirection(ProgressDrawable.Direction.DOWN, false);
			case 3 -> new ProgressDirection(ProgressDrawable.Direction.UP, false);
			case 4 -> new ProgressDirection(ProgressDrawable.Direction.RIGHT, true);
			case 5 -> new ProgressDirection(ProgressDrawable.Direction.LEFT, true);
			case 6 -> new ProgressDirection(ProgressDrawable.Direction.DOWN, true);
			case 7 -> new ProgressDirection(ProgressDrawable.Direction.UP, true);
			default -> new ProgressDirection(ProgressDrawable.Direction.RIGHT, false);
		};
	}
}
