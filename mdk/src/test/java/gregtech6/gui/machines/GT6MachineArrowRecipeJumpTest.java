package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.api.widget.IGuiAction;
import brachy.modularui.drawable.text.RichText;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widgets.ProgressWidget;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The machine-arrow recipe-jump pins (task machine-arrow-recipe-jump, the research card
 * research.machine-arrow-recipe-jump): the progress arrow of the shared
 * {@link GTBasicMachineMUI} panel wears (a) the current-recipe identity sync ({@code
 * bm_recipe}), (b) the recipe/progress hover tooltip, (c) the viewer-jump click.
 *
 * <p>Pinned faces:
 * <ul>
 * <li><b>the sync roundtrip</b> — on a REAL Drying machine (the buildUI → hostOf seam, the
 *     GT6BasicMachineMUIPanelTest fixture form) the getter derives the identity key of
 *     {@code mCurrentRecipe} ONLY while the three-state progress says "processing";
 *     idle syncs {@code null} even though {@code mCurrentRecipe} is still set — the
 *     never-cleared-field TRAP (upstream :757 assigns, never clears);</li>
 * <li><b>the identity-key table</b> — first non-empty output decides, items before fluids,
 *     none → null (the {@code i:}/{@code f:} registry-id wire form, javadoc'd on
 *     {@link GTBasicMachineMUI#currentRecipeKey}), and the client-side resolution back to
 *     the output's localized name;</li>
 * <li><b>the tooltip two-state gate</b> — key present → the output's display name + the
 *     progress percent (drain-aware); key absent → NO lines at all (RichTooltip
 *     {@code isEmpty});</li>
 * <li><b>the click structure</b> — the arrow carries exactly one gui-action listener, a
 *     {@code MousePressed} behind the mandatory {@code isMouseAbove} gate (the fork
 *     dispatches listeners screen-level BEFORE widget hit-testing); driving it offline:
 *     outside the arrow cell → false, inside a map-less arrow → false (the null-map
 *     no-op). The live viewer route itself stays with GT6ViewerJumpCensusTest's pure-arm
 *     table — the ModList probe cannot run offline.</li>
 * </ul>
 */
class GT6MachineArrowRecipeJumpTest extends GTRecipesOfflineTestBase {

	/**
	 * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form, reflective so the
	 * shared tree compiles on both legs): the vendored MUI widget classes read
	 * {@code FMLEnvironment.dist} during their static init — SERVER selects the headless
	 * text branch. Must run BEFORE the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6MachineArrowRecipeJumpTest.class.getClassLoader());
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

	/** The offline machine BET (the panel test's Drying fixture verbatim). */
	private static BlockEntityType<TileEntityBasicMachine> sMachineType;

	private static final BlockPos POS = new BlockPos(100, 64, 100);

	@BeforeAll
	static void buildMachineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, GT6RecipeMaps.DRYING, 8, true, null),
				Blocks.BRICKS).build(null);
		sMachineType = tHolder[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/** A fresh headless sync manager (the panel test form). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/**
	 * The map-less Host fake (the panel test's FakeHost shape minus the fluid banks — the
	 * interface-default single input, no {@code getRecipeMap} override = the null map).
	 */
	private static class BareHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory = new GTItemStackHandler(16);

		@Override public GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getOutputSlotCount() { return 12; }
		@Override public boolean isSuccessful() { return false; }
		@Override public long getProgress() { return 0; }
		@Override public long getMaxProgress() { return 0; }
	}

	/** A bricks-output recipe — the identity-key source (vanilla-registry items resolve offline). */
	private static Recipe bricksRecipe() {
		return new Recipe(true,
				new ItemStack[] {new ItemStack(Blocks.BRICKS)},
				new ItemStack[] {new ItemStack(Items.BRICK, 2)},
				null, null, 100, 1, 0);
	}

	/** A water-only recipe — the fluid fallback arm. */
	private static Recipe waterRecipe() {
		return new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 250)},
				20, 1, 0);
	}

	/** The panel's progress arrow (exactly one ProgressWidget rides the shared panel). */
	private static ProgressWidget arrowOf(ModularPanel<?> aPanel) {
		List<IWidget> tAll = new ArrayList<>();
		collect(aPanel, tAll);
		return tAll.stream().filter(w -> w instanceof ProgressWidget).map(w -> (ProgressWidget) w).findFirst().orElseThrow();
	}

	private static void collect(IWidget aWidget, List<IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collect(tChild, aSink);
			}
		}
	}

	/** The single MousePressed gui action registered on a widget (reflect — no fork getter). */
	@SuppressWarnings("unchecked")
	private static IGuiAction.MousePressed pressActionOf(ProgressWidget aArrow) throws Exception {
		java.lang.reflect.Field tListeners = brachy.modularui.widget.Widget.class.getDeclaredField("guiActionListeners");
		tListeners.setAccessible(true);
		List<IGuiAction> tActions = (List<IGuiAction>) tListeners.get(aArrow);
		assertEquals(1, tActions.size(), "the arrow carries exactly one gui action");
		return assertInstanceOf(IGuiAction.MousePressed.class, tActions.get(0));
	}

	// ---------------------------------------------------------------------------
	// the sync roundtrip — the bm_recipe key over the REAL hostOf adapter seam
	// ---------------------------------------------------------------------------

	/**
	 * The open arm's server half feeds the getter: running → the identity key of the
	 * machine's {@code mCurrentRecipe}; idle → {@code null} while the field is STILL set —
	 * the gate is the three-state progress, never the never-cleared field (the TRAP).
	 */
	@Test
	void theRecipeKeySyncsOnlyWhileTheMachineProcesses() {
		TileEntityBasicMachine tMachine = sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
		tMachine.mCurrentRecipe = bricksRecipe();
		tMachine.mMaxProgress = 100;
		tMachine.mProgress = 50;

		PanelSyncManager tSync = headlessSyncManager();
		tMachine.buildUI(null, tSync, new UISettings());

		StringSyncValue tValue = tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_RECIPE, StringSyncValue.class);
		assertNotNull(tValue, "bm_recipe registered by the shared panel");

		tValue.updateCacheFromSource(true);
		assertEquals("i:minecraft:brick", tValue.getValue(),
				"running → the first-output identity key rides hostOf(...).getCurrentRecipe()");

		// idle: max 0 → the -1 sentinel → the getter gates to null — the field stays set,
		// and the synced identity MUST still go away (the "last applied" trap)
		tMachine.mMaxProgress = 0;
		tMachine.mProgress = 0;
		tValue.updateCacheFromSource(true);
		assertNull(tValue.getValue(), "idle → null even with mCurrentRecipe set (the three-state gate)");

		// and a machine that is processing with no applied recipe at all
		tMachine.mCurrentRecipe = null;
		tMachine.mMaxProgress = 100;
		tValue.updateCacheFromSource(true);
		assertNull(tValue.getValue(), "processing with no applied recipe → null key");
	}

	// ---------------------------------------------------------------------------
	// the identity-key table (the pure wire-form derivation + client resolution)
	// ---------------------------------------------------------------------------

	@Test
	void theIdentityKeyPicksTheFirstOutputItemThenFluid() {
		assertNull(GTBasicMachineMUI.currentRecipeKey(null), "no recipe → no key");
		assertEquals("i:minecraft:brick", GTBasicMachineMUI.currentRecipeKey(bricksRecipe()),
				"the first non-empty item output");
		assertEquals("f:minecraft:water", GTBasicMachineMUI.currentRecipeKey(waterRecipe()),
				"fluid-only outputs ride the f: fallback");
		assertNull(GTBasicMachineMUI.currentRecipeKey(new Recipe(true, new ItemStack[0], new ItemStack[0], null, null, 20, 1, 0)),
				"no outputs at all → no key");
	}

	/**
	 * The client-side resolution: the registry id back to the output's localized name.
	 * Item arm only — the fluid arm rides {@code FluidStack.getDisplayName}, which needs
	 * the ForgeHooks vanilla-FluidType registry that never registers in the offline JVM
	 * (in game it is the correct face; the fluid arm's WIRE side is pinned by the
	 * identity-key table above).
	 */
	@Test
	void theKeyResolvesBackToTheOutputDisplayName() {
		assertEquals(new ItemStack(Items.BRICK).getHoverName().getString(),
				GTBasicMachineMUI.recipeKeyDisplayName("i:minecraft:brick").getString(),
				"i: resolves through the item registry to the hover name");
	}

	// ---------------------------------------------------------------------------
	// the tooltip two-state gate (the builder body, offline-driven)
	// ---------------------------------------------------------------------------

	/** The tooltip lines the builder produced (the Either.left components' strings). */
	private static List<String> tooltipLines(RichTooltip aTooltip) {
		aTooltip.buildTooltip();
		if (!(aTooltip.getRichText() instanceof RichText tText)) return List.of();
		// TooltipLines IS a List<Either<FormattedText, TooltipComponent>> (RichTooltip.draw's
		// own cast, RichTooltip.java:167-171)
		@SuppressWarnings("unchecked")
		List<Either<FormattedText, TooltipComponent>> tElements = (List<Either<FormattedText, TooltipComponent>>) (List<?>) tText.getAsText();
		return tElements.stream().map(e -> e.left().orElseThrow().getString()).toList();
	}

	private static RichTooltip builtTooltip(StringSyncValue aKey, DoubleSyncValue aProgress,
			GTBasicMachineMUI.ProgressDirection aDirection) {
		RichTooltip tTooltip = new RichTooltip();
		tTooltip.tooltipBuilder(t -> GTBasicMachineMUI.addArrowTooltip(t, aKey, aProgress, aDirection));
		return tTooltip;
	}

	@Test
	void theArrowTooltipShowsRecipeAndProgressOnlyWhileProcessing() {
		StringSyncValue tKey = new StringSyncValue(() -> "i:minecraft:brick");
		DoubleSyncValue tProgress = new DoubleSyncValue(() -> 0.5D);

		List<String> tLines = tooltipLines(builtTooltip(tKey, tProgress, GTBasicMachineMUI.progressDirection(0)));
		assertEquals(2, tLines.size(), "the output name + the percent");
		assertEquals(new ItemStack(Items.BRICK).getHoverName().getString(), tLines.get(0), "the recipe output name");
		assertEquals("50%", tLines.get(1), "the progress percent");

		// THE GATE: no synced key (idle or recipe-less — the server getter already gated on
		// the three-state progress) → no tooltip lines at all
		StringSyncValue tIdle = new StringSyncValue(() -> null);
		RichTooltip tGated = builtTooltip(tIdle, tProgress, GTBasicMachineMUI.progressDirection(0));
		tGated.buildTooltip();
		assertTrue(tGated.isEmpty(), "idle → the tooltip stays empty (no stale last-applied recipe)");

		// the drain face: the synced value is the DISPLAY value (already inverted), the
		// percent undoes it — a drained-done drain bar (0.0 shown) is 100% progress
		DoubleSyncValue tDrained = new DoubleSyncValue(() -> 0.0D);
		List<String> tDrainLines = tooltipLines(builtTooltip(tKey, tDrained, GTBasicMachineMUI.progressDirection(6)));
		assertEquals(2, tDrainLines.size());
		assertEquals("100%", tDrainLines.get(1), "the drain inversion is undone in the percent");
	}

	// ---------------------------------------------------------------------------
	// the click structure — the gated MousePressed on the arrow
	// ---------------------------------------------------------------------------

	/**
	 * The click pin (the offline-drivable shape): the arrow carries one MousePressed
	 * listener; the mandatory isMouseAbove gate closes it outside the arrow cell, and a
	 * map-less arrow is the silent no-op (the census test owns the viewer route table —
	 * the ModList probe cannot run offline, so the live route is never driven here).
	 */
	@Test
	void theArrowClickIsGatedByTheMouseAboveAndNoOpsOnAMapLessHost() throws Exception {
		// a real machine's arrow (the DRYING map present — only the gate arm is driven)
		TileEntityBasicMachine tMachine = sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
		ProgressWidget tArrow = arrowOf(tMachine.buildUI(null, headlessSyncManager(), new UISettings()));

		// the tooltip face rides the same widget: autoUpdate so the builder reruns as
		// progress advances (the FluidSlot.java:59-63 form)
		assertTrue(tArrow.tooltip().autoUpdate(), "the arrow tooltip auto-updates");

		IGuiAction.MousePressed tPress = pressActionOf(tArrow);

		// the arrow cell (78,24,20x18) — the headless Area only resolves at layout, so the
		// pin writes the geometry the build stores (the panel test's posOf note)
		tArrow.getArea().set(78, 24, 20, 18);
		GuiContext tContext = new GuiContext(null);
		tContext.updateState(10, 10, 0.0F);
		assertFalse(tPress.press(tContext, 0),
				"outside the arrow cell → the gate closes (listeners dispatch screen-level BEFORE hit-testing)");

		// a map-less arrow (the BareHost fake): inside the cell → the null-map no-op, false
		// = the vanilla click handling continues — GTViewerJump is never reached
		ProgressWidget tBareArrow = arrowOf(GTBasicMachineMUI.buildPanel(new BareHost(), headlessSyncManager()));
		pressActionOf(tBareArrow); // the same single-listener structure
		tBareArrow.getArea().set(78, 24, 20, 18);
		tContext.updateState(88, 33, 0.0F);
		assertFalse(pressActionOf(tBareArrow).press(tContext, 0), "map-less arrow click → silent no-op");
	}
}
