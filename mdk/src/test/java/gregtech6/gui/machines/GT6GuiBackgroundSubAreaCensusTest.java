/**
 * The machine-panel background sub-area gate (task gui-bg-uv-fix): the machine sheets are
 * 256x256 canvases with the art in the top-left 176x166 (GT6GuiReskinCensusTest pin-h), so
 * every MUI panel background must declare the explicit sub-area — the former
 * {@code UITexture.fullImage} form stretched the whole canvas into the 176x166 panel and
 * shrank the art into the top-left corner (the vanilla-leg furnace blit samples the same
 * window implicitly, GTGuiScreen.renderBg:55-58).
 *
 * <p>Pinned faces:
 * <ul>
 * <li><b>the sub-area table</b> — all three panel families (basic machine / distillation
 *     tower / bumbliary normal+advanced) build backgrounds with UV (0,0)-(176/256,166/256),
 *     the top-left sub-area stretched over the full panel rect;</li>
 * <li><b>the consumption census</b> — zero {@code UITexture.fullImage} tokens left in the
 *     mdk main sources: a new panel re-committing the whole-canvas stretch turns this red.</li>
 * </ul>
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import brachy.modularui.drawable.UITexture;
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
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.bees.GT6BumbliaryBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

class GT6GuiBackgroundSubAreaCensusTest extends GTMultiBlocksOfflineTestBase {

	/** The top-right UV corner of the declared sub-area: 256-canvas, art window 176x166. */
	private static final float SUB_U1 = 176f / 256f, SUB_V1 = 166f / 256f;

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

	/** The parametrisable offline machine Host fake (the GT6BasicMachineMUIPanelTest shredder form). */
	private static class FakeHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory = new GTItemStackHandler(28);

		@Override public GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getOutputSlotCount() { return 12; }
		@Override public boolean isSuccessful() { return false; }
		@Override public long getProgress() { return 0; }
		@Override public long getMaxProgress() { return 0; }
		@Override public String getGuiTexture() { return "gt6:textures/gui/machines/shredder"; }
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
	 * The sub-area table: every machine-panel family samples ONLY the top-left 176x166 of
	 * the 256x256 canvas — the exact observable of
	 * {@code builder().imageSize(256,256).subAreaXYWH(0,0,176,166)} (a fullImage regression
	 * reads UV (0,0)-(1,1) and fails the u1/v1 pins).
	 */
	@Test
	public void machineBackgroundsSampleOnlyTheTopLeftSubArea() {
		// the basic-machine family (shredder stand-in — the location itself is pinned by
		// GT6BasicMachineMUIPanelTest; this test pins only the sampling window)
		assertSubArea(GTBasicMachineMUI.buildPanel(new FakeHost(), headlessSyncManager()), "basic machine");
		// the distillation tower
		assertSubArea(GTDistillationTowerMUI.buildPanel(newTower(), headlessSyncManager()), "distillation tower");
		// the bumbliary pair — the advanced flag picks the texture branch, both must sample
		// the same window
		assertSubArea(GT6BumbliaryMUI.buildPanel(
				new GT6BumbliaryBlockEntity(false, sBumbliaryType, P1, Blocks.BRICKS.defaultBlockState()),
				headlessSyncManager(), false), "bumbliary normal");
		assertSubArea(GT6BumbliaryMUI.buildPanel(
				new GT6BumbliaryBlockEntity(true, sBumbliaryType, P1, Blocks.BRICKS.defaultBlockState()),
				headlessSyncManager(), false), "bumbliary advanced");
	}

	private static void assertSubArea(ModularPanel<?> aPanel, String aWhat) {
		UITexture tBackground = assertInstanceOf(UITexture.class, aPanel.getBackground(),
				aWhat + ": the panel background is a texture");
		assertEquals(0f, tBackground.u0(), 0f, aWhat + ": sub-area starts at u=0");
		assertEquals(0f, tBackground.v0(), 0f, aWhat + ": sub-area starts at v=0");
		assertEquals(SUB_U1, tBackground.u1(), 0f, aWhat + ": sub-area ends at u=176/256 (NOT the fullImage 1.0)");
		assertEquals(SUB_V1, tBackground.v1(), 0f, aWhat + ": sub-area ends at v=166/256 (NOT the fullImage 1.0)");
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
