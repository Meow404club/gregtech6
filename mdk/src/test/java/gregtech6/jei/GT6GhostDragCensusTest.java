/**
 * Offline census for task debt-jei-emi-batch4-transfer, the ghost-drag face of the ACT
 * overlay (upstream NEI Config:64-67 registered the NEI "crafting" overlay + overlay
 * handler on the ACT client GUI). The port's ruling up front: the ACT GUI is ModularUI
 * (GTActMenu / the P24-P26 chain), and the vendored MUI layer ALREADY carries the ghost
 * face for every MUI screen — {@code PhantomItemSlot implements GhostIngredientSlot}
 * (the ACT 3x3 pattern grid is nine of those), delivered per viewer by MUI's own plugins:
 * <ul>
 * <li>JEI-only: {@code ModularUIJeiPlugin.registerGuiHandlers} registers the
 *     {@code JeiScreenHandler} ghost handler on the MUI screen wrapper classes;</li>
 * <li>EMI: {@code ModularUIEmiPlugin.register} registers the {@code EmiScreenHandler}
 *     drag-drop handler on the same wrappers;</li>
 * <li>THE MUTUAL-YIELD FORM (both installed): MUI's JEI plugin early-returns when EMI or
 *     REI is loaded — the EMI handler takes over. This census pins that guard text on
 *     both legs' plugin sources.</li>
 * </ul>
 * These registrations live in the vendored submodule (NOT owned by this repo), so the
 * census asserts their shape honestly: reflection for the implementable contracts, a
 * source-text read of the leg's plugin files for the registration/guard lines, and a
 * bytecode-constant scan proving the ACT panel builds PhantomItemSlot seats. If a MUI
 * update moves any line, this test flags it for re-audit rather than letting the face
 * rot silently.
 *
 * <p>The transfer half of the upstream overlay is DEFERRED by the card ruling: the
 * upstream DefaultOverlayHandler moved REAL items into the ACT's real 3x3 grid slots
 * (MultiTileEntityAdvancedCraftingTable.java:716-724 Slot_Normal), while this port's 3x3
 * is the phantom pattern grid (decisions.p24-act-ghost-form) — real-item transfer needs a
 * client→server bulk-move channel MUI2 does not ship (its JeiContainerHandler bridge is
 * commented out and no ModularScreen implements RecipeTransferHandler). The last test
 * pins that commented state so an upstream change re-opens the pool item instead of
 * rotting.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import brachy.modularui.integration.emi.handler.EmiScreenHandler;
import brachy.modularui.integration.jei.handler.JeiScreenHandler;
import brachy.modularui.integration.recipeviewer.handlers.GhostIngredientSlot;
import brachy.modularui.widgets.slot.PhantomItemSlot;

import dev.emi.emi.api.EmiDragDropHandler;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;

import gregtech6.menu.act.GTActMenu;

public class GT6GhostDragCensusTest {

	@BeforeAll
	static void bootVanillaOffline() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The MUI ghost-slot face the ACT 3x3 pattern grid rides (GTActMenu.patternGrid builds nine seats). */
	@Test
	public void actPatternSeatIsAMuiGhostIngredientSlot() {
		assertTrue(GhostIngredientSlot.class.isAssignableFrom(PhantomItemSlot.class),
				"PhantomItemSlot implements GhostIngredientSlot — the ghost face of the ACT pattern grid");
	}

	/** The viewer-side contracts MUI's two plugins deliver the face through. */
	@Test
	public void muiPluginsCarryTheViewerContracts() {
		assertTrue(IGhostIngredientHandler.class.isAssignableFrom(JeiScreenHandler.class),
				"MUI's JEI screen handler is the ghost-ingredient handler (JEI-only leg)");
		assertTrue(EmiDragDropHandler.class.isAssignableFrom(EmiScreenHandler.class),
				"MUI's EMI screen handler is the drag-drop handler (EMI leg, incl. both-installed)");
	}

	/** The ACT panel really builds PhantomItemSlot seats (bytecode constant-pool scan). */
	@Test
	public void actPanelBuildsPhantomSeats() throws IOException {
		String tClass = GTActMenu.class.getName().replace('.', '/').concat(".class");
		byte[] tBytes;
		try (var in = GTActMenu.class.getClassLoader().getResourceAsStream(tClass)) {
			assertNotNull(in, "GTActMenu bytecode on the test classpath");
			tBytes = in.readAllBytes();
		}
		assertTrue(new String(tBytes, StandardCharsets.ISO_8859_1)
						.contains("brachy/modularui/widgets/slot/PhantomItemSlot"),
				"the ACT panel's 3x3 seats are PhantomItemSlot (the ghost face consumer side)");
	}

	/**
	 * The mutual-yield + registration census over the vendored plugin sources, read from
	 * the submodule checkout of THIS repo — leg fork: forgeMain vs neoforgeMain.
	 */
	@Test
	public void vendoredPluginsRegisterTheGhostFaceAndYieldMutually() throws IOException {
		Path tMui = findMuiRoot();
		//? if forge {
		Path tJeiPlugin = tMui.resolve("src/forgeMain/java/brachy/modularui/integration/jei/ModularUIJeiPlugin.java");
		Path tEmiPlugin = tMui.resolve("src/forgeMain/java/brachy/modularui/integration/emi/ModularUIEmiPlugin.java");
		//?} else {
		/*Path tJeiPlugin = tMui.resolve("src/neoforgeMain/java/brachy/modularui/integration/jei/ModularUIJeiPlugin.java");
		Path tEmiPlugin = tMui.resolve("src/neoforgeMain/java/brachy/modularui/integration/emi/ModularUIEmiPlugin.java");
		*///?}
		List<String> tJei = Files.readAllLines(tJeiPlugin);
		List<String> tEmi = Files.readAllLines(tEmiPlugin);

		assertTrue(tJei.stream().anyMatch(l -> l.contains("JeiScreenHandler.register(ScreenWrapper.class")),
				"MUI JEI plugin registers the ghost handler for the MUI screen wrapper");
		assertTrue(tJei.stream().anyMatch(l -> l.contains("JeiScreenHandler.register(ContainerScreenWrapper.class")),
				"...and for the container wrapper (the ACT screen's wrapper)");
		assertTrue(tJei.stream().anyMatch(l -> l.contains("REI.isLoaded()") && l.contains("EMI.isLoaded()") && l.contains("return")),
				"MUTUAL YIELD: the MUI JEI plugin early-returns when EMI/REI is loaded (both-installed form)");
		assertTrue(tEmi.stream().anyMatch(l -> l.contains("EmiScreenHandler.register(ContainerScreenWrapper.class")),
				"MUI EMI plugin registers the drag-drop handler on the same wrappers");
	}

	/** The transfer-defer evidence pin: no ACTIVE JeiContainerHandler registration in the vendored plugin. */
	@Test
	public void muiTransferBridgeRemainsUnwired() throws IOException {
		Path tMui = findMuiRoot();
		//? if forge {
		Path tJeiPlugin = tMui.resolve("src/forgeMain/java/brachy/modularui/integration/jei/ModularUIJeiPlugin.java");
		//?} else {
		/*Path tJeiPlugin = tMui.resolve("src/neoforgeMain/java/brachy/modularui/integration/jei/ModularUIJeiPlugin.java");
		*///?}
		assertTrue(Files.readAllLines(tJeiPlugin).stream()
						.anyMatch(l -> l.strip().startsWith("//") && l.contains("JeiContainerHandler.register")),
				"the MUI transfer bridge sits commented out — the documented transfer-defer premise");
		assertFalse(Files.readAllLines(tJeiPlugin).stream()
						.anyMatch(l -> !l.strip().startsWith("//") && l.contains("JeiContainerHandler.register")),
				"an ACTIVE MUI transfer bridge re-opens the deferred act-transfer pool item");
	}

	/** Walks up from the test working dir to the repo root that owns the submodule checkout. */
	private static Path findMuiRoot() throws IOException {
		Path tDir = Paths.get("").toAbsolutePath();
		while (tDir != null && !Files.isDirectory(tDir.resolve("third-party/modularui"))) tDir = tDir.getParent();
		if (tDir == null) throw new IOException("third-party/modularui checkout not found above " + Paths.get("").toAbsolutePath());
		return tDir.resolve("third-party/modularui");
	}
}
