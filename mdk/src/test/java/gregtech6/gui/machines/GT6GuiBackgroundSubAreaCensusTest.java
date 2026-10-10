/**
 * The machine-panel background census (task gui-bg-uv-fix, REWRITTEN by task
 * machine-gui-composed-purge): the machine sheets are retired — every MUI machine panel
 * now rides the gt6 theme's 9-slice base (the composed form, the GTBasicMachineMUI
 * clean-base shape), so NO machine panel carries a code background and the former
 * sub-area table (256x256 canvas, art window 176x166, task gui-bg-uv-fix) is history:
 * the vanilla-leg blit that sampled the same window is composed from the part crops.
 *
 * <p>Pinned faces:
 * <ul>
 * <li><b>the composed table</b> — the distillation tower and the bumbliary normal+advanced
 *     panels carry NO code background ({@code getBackground()} null headless — the theme
 *     base draws at runtime); the basic-machine family joined this form at
 *     r11-gui-basicmachine-clean-sample;</li>
 * <li><b>the consumption census</b> — zero {@code UITexture.fullImage} tokens left in the
 *     mdk main sources: a new panel re-committing the whole-canvas stretch turns this red;</li>
 * <li><b>the sheet-token census</b> — every machine-panel factory (the shared basic-machine
 *     factory + the tower + the bumbliary pair) sources zero {@code gui/machines} sheet
 *     tokens, and the shared basic-machine factory zero {@code GuiTextures.PROGRESS_ARROW}
 *     tokens (the parts arrow only).</li>
 * </ul>
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Distillation.TileEntityDistillationTower;
import gregtech6.tileentity.bees.GT6BumbliaryBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

class GT6GuiBackgroundSubAreaCensusTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(50, 64, 50);

	/**
	 * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form — the vendored MUI
	 * widget classes read FMLEnvironment.dist during their static init). Must run BEFORE
	 * the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6GuiBackgroundSubAreaCensusTest.class.getClassLoader());
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

	/** The panel-fixture BETs (the selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<TileEntityDistillationTower> sTowerType;
	static BlockEntityType<GT6BumbliaryBlockEntity> sBumbliaryType;

	@BeforeAll
	static void buildFixtureBets() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityDistillationTower>[] tTowers =
				(BlockEntityType<TileEntityDistillationTower>[]) new BlockEntityType<?>[1];
		tTowers[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityDistillationTower(tTowers[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sTowerType = tTowers[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BumbliaryBlockEntity>[] tHives =
				(BlockEntityType<GT6BumbliaryBlockEntity>[]) new BlockEntityType<?>[1];
		tHives[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbliaryBlockEntity(false, tHives[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sBumbliaryType = tHives[0];
	}

	/** The row-less tower fixture (the GT6DistillationTowerMUIPanelTest fake-map form). */
	private static TileEntityDistillationTower newTower() {
		TileEntityDistillationTower tTower =
				new TileEntityDistillationTower(sTowerType, P1, Blocks.BRICKS.defaultBlockState());
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(),
				"gt6.test.bgsubarea." + System.nanoTime(), "BG Sub-Area Test", null, 0, 1,
				"gt6:textures/gui/machines/distillationtower", 1, 3, 1, 1, 9, 0, 1, 1);
		tTower.mRecipes = tMap;
		return tTower;
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/**
	 * The composed table: every machine-panel factory builds a panel with NO code
	 * background — headless {@code getBackground()} is null, the gt6 theme's 9-slice base
	 * draws at runtime (the GTBasicMachineMUIPanelTest clean-base form; a sheet-background
	 * regression re-roots a UITexture here and fails).
	 */
	@Test
	public void machinePanelsCarryNoCodeBackground() {
		// the distillation tower
		assertNoBackground(GTDistillationTowerMUI.buildPanel(newTower(), headlessSyncManager()), "distillation tower");
		// the bumbliary pair — the advanced flag only picks the slot table, both ride the theme
		assertNoBackground(GT6BumbliaryMUI.buildPanel(
				new GT6BumbliaryBlockEntity(false, sBumbliaryType, P1, Blocks.BRICKS.defaultBlockState()),
				headlessSyncManager(), false), "bumbliary normal");
		assertNoBackground(GT6BumbliaryMUI.buildPanel(
				new GT6BumbliaryBlockEntity(true, sBumbliaryType, P1, Blocks.BRICKS.defaultBlockState()),
				headlessSyncManager(), false), "bumbliary advanced");
	}

	private static void assertNoBackground(ModularPanel<?> aPanel, String aWhat) {
		assertTrue(aPanel.getBackground() == null,
				aWhat + ": no code background — the theme panel base owns the panel (the composed form)");
	}

	/**
	 * The sheet-token census: every machine-panel factory (the shared basic-machine factory,
	 * the tower factory and the bumbliary factory) sources zero {@code gui/machines} sheet
	 * tokens (machine-gui-composed-purge — the theme base + the parts compose the visuals),
	 * and the shared basic-machine factory zero {@code GuiTextures.PROGRESS_ARROW} tokens
	 * (the parts arrow only).
	 */
	@Test
	public void theMachinePanelFactoriesSourceNoSheetTokens() throws IOException {
		for (String tFactory : new String[] {"GTBasicMachineMUI.java", "GTDistillationTowerMUI.java",
				"GT6BumbliaryMUI.java"}) {
			String tSource = Files.readString(mdkMainJava()
					.resolve("gregtech6").resolve("gui").resolve("machines").resolve(tFactory));
			assertFalse(tSource.contains("gui/machines"), tFactory + ": zero machines/ sheet tokens");
		}
		String tShared = Files.readString(mdkMainJava()
				.resolve("gregtech6").resolve("gui").resolve("machines").resolve("GTBasicMachineMUI.java"));
		assertFalse(tShared.contains("GuiTextures.PROGRESS_ARROW"),
				"zero vendored-arrow tokens in the shared panel factory (the parts arrow only)");
	}

	/**
	 * The consumption census: zero {@code UITexture.fullImage} tokens in the mdk main
	 * sources — the whole-canvas stretch has no legitimate consumer there (every machine
	 * sheet is a 256x256 canvas); a new panel committing the same bug turns this red.
	 */
	@Test
	public void noMainSourceConsumesFullImageAnymore() throws IOException {
		List<Path> tOffenders = new ArrayList<>();
		try (Stream<Path> tWalk = Files.walk(mdkMainJava())) {
			tWalk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
				try {
					if (Files.readString(p).contains("UITexture.fullImage")) tOffenders.add(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
		assertTrue(tOffenders.isEmpty(), () -> "UITexture.fullImage consumers — machine sheets are 256x256 "
				+ "canvases, declare .imageSize(256,256).subAreaXYWH(0,0,176,166) instead: " + tOffenders);
	}

	/** The mdk main source root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkMainJava() throws IOException {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			Path tMain = p.resolve("mdk").resolve("src").resolve("main").resolve("java");
			if (Files.isDirectory(tMain)) return tMain;
		}
		throw new IOException("mdk/src/main/java not found upward from " + Path.of("").toAbsolutePath());
	}
}
