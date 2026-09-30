/**
 * Pin (task gui-slot-theme) — the machine GUI slot-theme census. The vendored
 * ModularUI fork paints a vanilla-style gray slot bottom
 * ({@code modularui:textures/gui/slot/item.png} / {@code fluid.png}) under every
 * item slot via its {@code ITEM_SLOT}/{@code FLUID_SLOT} default theme
 * (fork GuiTextures.java SLOT_ITEM/SLOT_FLUID → IThemeApi ITEM_SLOT/FLUID_SLOT →
 * Widget.drawBackground), and the player-inventory grid inherits it through the
 * {@code itemSlot:player*} sub keys — on top of the amazawa-reskinned panels that
 * already bake their own slot frames this reads as a dirty double frame. The fix
 * ships a gt6-side theme that swaps both slot backgrounds to the amazawa crop
 * {@code gt6:textures/gui/parts/slot_frame_18x18.png} (GT6GuiParts.SLOT_FRAME,
 * previously a zero-consumer part) and routes every ModularUI screen to it: the
 * fork's ThemeManager reads {@code themes.json} from every namespace, resolves
 * screens by owner wildcard ({@code "screens": {"modularui": "gt6"}} — both GT6
 * screens are constructed under the fork's MOD_ID owner, GT6MuiMachine.createScreen
 * and GTActMenu.createScreen), and merges our {@code itemSlot}/{@code fluidSlot}
 * backgrounds over the fork's shipped {@code vanilla} parent theme; the player
 * sub keys inherit {@code itemSlot} automatically.
 *
 * <p>Like the other GUI censuses this is pure read-only JUnit, no vanilla bootstrap.
 * The codec pin feeds the shipped JSON through the fork's own IDrawable codec — the
 * same parse ThemeManager runs at resource reload — so a fork wire-format change
 * breaks here instead of silently falling back to the gray bottom in game.</p>
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.UITexture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import gregtech6.recipes.GTRecipesOfflineTestBase;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GT6GuiSlotThemeCensusTest extends GTRecipesOfflineTestBase {

    /** The gt6 asset root, under the mdk root. */
    private static final Path ASSETS_GT6 = Path.of("src", "main", "resources", "assets", "gt6");

    /** Theme id registered for every ModularUI screen (the screens wildcard value). */
    private static final String THEME_ID = "gt6";

    /** The theme JSON path declared for the id (ThemeManager: idToFile → themes/modern.json). */
    private static final String THEME_PATH = "gt6:modern";

    /** The screen owner both GT6 ModularScreen constructions pass (the fork MOD_ID). */
    private static final String SCREEN_OWNER = "brachy.modularui.ModularUI.MOD_ID";

    /** The slot background textures, both item and fluid seats. */
    private static final String SLOT_FRAME_LOCATION = "gt6:textures/gui/parts/slot_frame_18x18.png";

    /** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
            + Path.of("").toAbsolutePath());
    }

    private static JsonObject readJson(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * The theme chain pin: gt6's themes.json registers the {@code gt6} theme at
     * {@code gt6:modern}, routes the {@code modularui} screen owner (both GT6
     * ModularScreen constructions) to it, and the theme file exists with the
     * fork-shipped {@code vanilla} parent (ThemeManager validates the ancestor
     * against the same merged namespace map — a renamed fork theme id fails here,
     * not in game). The source walk guards the owner half: a future
     * {@code new ModularScreen(} with a different owner would silently miss the
     * wildcard and fall back to the gray vanilla slot bottom.
     */
    @Test
    void themeChainRoutesModularuiScreensToTheGt6Theme() throws IOException {
        Path root = mdkRoot();
        JsonObject themeList = readJson(root.resolve(ASSETS_GT6).resolve("themes.json"));
        assertEquals(THEME_PATH, themeList.get(THEME_ID).getAsString(),
            "themes.json must register the " + THEME_ID + " theme at " + THEME_PATH);
        JsonObject screens = themeList.getAsJsonObject("screens");
        assertEquals(THEME_ID, screens.get("modularui").getAsString(),
            "the screens wildcard must route the modularui owner (the owner every GT6 "
                + "ModularScreen passes) to the " + THEME_ID + " theme");

        Path themeFile = root.resolve(ASSETS_GT6).resolve("themes").resolve("modern.json");
        assertTrue(Files.isRegularFile(themeFile),
            "the theme JSON is missing at " + themeFile + " (themes.json points at " + THEME_PATH + ")");
        JsonObject theme = readJson(themeFile);
        assertEquals("vanilla", theme.get("parent").getAsString(),
            "the theme must parent onto the fork-shipped vanilla theme "
                + "(ThemeManager.validateAncestorTree resolves the parent from the merged namespace map)");

        List<String> strayScreens = new ArrayList<>();
        int screenConstructions = 0;
        try (Stream<Path> walk = Files.walk(root.resolve(Path.of("src", "main", "java")))) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    if (!line.contains("new ModularScreen(")) continue;
                    screenConstructions++;
                    if (!line.contains(SCREEN_OWNER)) {
                        strayScreens.add(file.getFileName() + ": " + line.trim());
                    }
                }
            }
        }
        assertTrue(screenConstructions >= 2,
            "expected the GT6MuiMachine + GTActMenu ModularScreen constructions, found " + screenConstructions);
        assertTrue(strayScreens.isEmpty(),
            "ModularScreen constructions outside the modularui owner would silently miss the "
                + "screens theme wildcard: " + strayScreens);
    }

    /**
     * The wire-format pin: both slot backgrounds parse through the fork's own
     * IDrawable codec (the exact parse ThemeManager runs at reload) into a full-image
     * UITexture of the GT6GuiParts.SLOT_FRAME crop — 18x18 sprite over the 18x18 slot
     * area, zero 9-slice borders. A fork codec change or a drifted location fails here
     * instead of drawing the gray vanilla bottom again.
     */
    @Test
    void slotBackgroundsParseThroughForkCodecToTheGt6SlotFrame() throws IOException {
        Path root = mdkRoot();
        JsonObject theme = readJson(root.resolve(ASSETS_GT6).resolve("themes").resolve("modern.json"));
        // The "texture" drawable codec registers in UITexture's static init; in game the
        // ThemeManager flow loads the class via GuiTextures long before a reload — here the
        // base @BeforeAll has bootstrapped the registries UITexture's clinit chain needs.
        java.util.Objects.requireNonNull(UITexture.CODEC, "the fork texture codec must exist");
        for (String key : List.of("itemSlot", "fluidSlot")) {
            JsonObject slot = theme.getAsJsonObject(key);
            assertTrue(slot != null && slot.has("background"),
                "the theme must define " + key + ".background");
            JsonElement background = slot.get("background");
            StringBuilder parseError = new StringBuilder();
            java.util.Optional<IDrawable> parsed = IDrawable.CODEC
                .parse(JsonOps.INSTANCE, background).resultOrPartial(parseError::append);
            assertTrue(parsed.isPresent(),
                key + ".background failed the fork codec parse: " + parseError);
            UITexture texture = assertInstanceOf(UITexture.class, parsed.get(),
                key + ".background must parse to a texture drawable");
            assertEquals(SLOT_FRAME_LOCATION, texture.location.toString(),
                key + ".background must be the amazawa slot frame crop");
            assertEquals(0f, texture.u0, key + " must draw the full image (u0)");
            assertEquals(0f, texture.v0, key + " must draw the full image (v0)");
            assertEquals(1f, texture.u1, key + " must draw the full image (u1) — 18x18 sprite "
                + "fills the 18x18 slot area exactly");
            assertEquals(1f, texture.v1, key + " must draw the full image (v1)");
        }
        assertEquals(SLOT_FRAME_LOCATION, GT6GuiParts.SLOT_FRAME.texture().toString(),
            "the theme targets GT6GuiParts.SLOT_FRAME — keep the constant and the JSON in lockstep");
        BufferedImage png = ImageIO.read(root.resolve(ASSETS_GT6)
            .resolve(Path.of("textures", "gui", "parts", "slot_frame_18x18.png")).toFile());
        assertEquals(18, png.getWidth());
        assertEquals(18, png.getHeight(), "the slot frame sprite must stay 18x18 (1 px = 1 GUI unit)");
    }

    /**
     * The residue census: the fork's vanilla-style slot bottoms
     * (modularui gui/slot/item + gui/slot/fluid, and the GuiTextures.SLOT_ITEM /
     * SLOT_FLUID constants referencing them) must stay unreferenced on the gt6 side —
     * a re-introduction would layer the gray bottom back over the modern slot frame.
     * (GT6GuiParts.SLOT_FLUID is our own part name and deliberately not banned.)
     */
    @Test
    void noForkVanillaSlotTextureReferencesRemainOnTheGt6Side() throws IOException {
        List<String> banned = List.of("gui/slot/item", "gui/slot/fluid",
            "GuiTextures.SLOT_ITEM", "GuiTextures.SLOT_FLUID");
        List<Path> roots = List.of(
            mdkRoot().resolve(Path.of("src", "main", "java")),
            mdkRoot().resolve(ASSETS_GT6));
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    // ISO-8859-1 decodes every byte sequence — the walk crosses PNG binaries too
                    String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                    for (String token : banned) {
                        if (text.contains(token)) {
                            violations.add(file + " references " + token);
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "fork vanilla slot-bottom references remain: " + violations);
    }
}
