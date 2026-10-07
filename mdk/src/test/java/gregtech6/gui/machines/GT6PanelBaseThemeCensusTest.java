/**
 * Pin (task r11-gui-clean-base-theme) — the clean-base theme census: the gt6 theme's
 * {@code panel.background} hands the self-derived amazawa panel base
 * ({@code gt6:textures/gui/parts/panel_base_176x166.png}, user ruling 2026-10-03_b —
 * a generic machine-area-blank sheet that keeps the printed player-inventory band)
 * to every GT6 ModularUI panel through the fork's panel theme chain.
 *
 * <p>The chain, as shipped in the vendored fork (all line pins read from the submodule
 * at work time): {@code ModularPanel.getWidgetThemeInternal(theme)} returns
 * {@code theme.getPanelTheme()} (forge ModularPanel.java:207-208) → the
 * {@code panel} {@code WidgetThemeKey} (IThemeApi.java:68-71) → the theme map entry
 * ThemeManager parsed from the theme JSON ({@code key.getCodec().codec().parse},
 * ThemeManager.java:425) → {@code WidgetTheme.getBackground()} (the {@code background}
 * field, IThemeApi.java:62) painted by {@code Widget.drawBackground} before any
 * code-side background (forge Widget.java:256-266). A theme JSON whose background
 * fails the fork codec, a renamed panel key, or a fork dispatch change breaks here —
 * not by silently falling back to the gray vanilla panel in game.</p>
 *
 * <p>Like the other GUI censuses this is pure read-only JUnit, no vanilla bootstrap
 * beyond the shared offline base. The only reflective step is constructing the fork's
 * package-private {@code Theme} — everything else runs the fork's public parse and
 * dispatch surface; the derivation itself is replayed PIXEL-exactly against the
 * on-tree source sheet (the version-independent face of make_panel_base.py).</p>
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import brachy.modularui.api.IThemeApi;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.AdaptableUITexture;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.theme.DefaultTheme;
import brachy.modularui.theme.Theme;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.theme.WidgetThemeMap;
import brachy.modularui.theme.ImmutableJson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import gregtech6.recipes.GTRecipesOfflineTestBase;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GT6PanelBaseThemeCensusTest extends GTRecipesOfflineTestBase {

    /**
     * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form — the vendored MUI
     * widget classes read FMLEnvironment.dist during their static init, and
     * {@code new ModularPanel} arms {@code Widget}'s clinit). Must run BEFORE the first
     * widget class initializes.
     */
    @BeforeAll
    static void armFmlDistOffline() throws Exception {
        Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
            GT6PanelBaseThemeCensusTest.class.getClassLoader());
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

    /** The gt6 asset root, under the mdk root. */
    private static final Path ASSETS_GT6 = Path.of("src", "main", "resources", "assets", "gt6");

    /** The derived base: texture path form the theme JSON uses, and the file name. */
    private static final String BASE_LOCATION = "gt6:textures/gui/parts/panel_base_176x166.png";
    private static final String BASE_FILE = "panel_base_176x166.png";

    /** The derivation source sheet + the fill window (must mirror make_panel_base.py). */
    private static final String SOURCE_FILE = "wiremill.png";
    private static final int FILL_X0 = 4, FILL_Y0 = 4, FILL_X1 = 172, FILL_Y1 = 83;
    private static final int PANEL_W = 176, PANEL_H = 166;
    private static final int BASE_COLOR = (255 << 24) | (203 << 16) | (204 << 8) | 212;

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

    private static BufferedImage readPng(String underTextures) throws IOException {
        return ImageIO.read(mdkRoot().resolve(ASSETS_GT6).resolve("textures").resolve(underTextures).toFile());
    }

    /**
     * The derivation replay: the shipped base is the source sheet's panel region with
     * exactly the machine-area interior filled flat — every pixel inside the fill
     * window is the flat panel color, every pixel outside it (border, player-inventory
     * band, bottom edge) is byte-for-byte the source sheet's pixel. A re-crop of the
     * source or a drifted derivation breaks here, version-independently.
     */
    @Test
    void panelBaseReplaysPixelExactAgainstItsSourceSheet() throws IOException {
        BufferedImage src = readPng("gui/machines/" + SOURCE_FILE);
        assertEquals(256, src.getWidth());
        assertEquals(256, src.getHeight(), "the source sheet must stay the standard 256x256 amazawa canvas");
        BufferedImage base = readPng("gui/parts/" + BASE_FILE);
        assertEquals(PANEL_W, base.getWidth());
        assertEquals(PANEL_H, base.getHeight(), "the panel base must stay 176x166 (1 px = 1 GUI unit)");

        List<String> violations = new java.util.ArrayList<>();
        for (int y = 0; y < PANEL_H; y++) {
            for (int x = 0; x < PANEL_W; x++) {
                int got = base.getRGB(x, y);
                if (x >= FILL_X0 && x < FILL_X1 && y >= FILL_Y0 && y < FILL_Y1) {
                    if (got != BASE_COLOR) {
                        violations.add("(" + x + "," + y + ") inside the fill window is " + got
                            + ", not the flat panel color");
                    }
                } else if (got != src.getRGB(x, y)) {
                    violations.add("(" + x + "," + y + ") outside the fill window drifted from " + SOURCE_FILE);
                }
            }
        }
        assertTrue(violations.isEmpty(), "panel_base_176x166 derivation replay violations: "
            + violations.size() + " px, first: " + (violations.isEmpty() ? "none" : violations.get(0)));

        // and the kept band really is the printed player inventory (the ruling's point):
        // the source sheet prints a dense slot grid below y=83 — if the source ever loses
        // it, "preserved" would be vacuously true over a blank band
        int printed = 0;
        for (int y = FILL_Y1; y < 159; y++) {
            for (int x = FILL_X0; x < FILL_X1; x++) {
                if (base.getRGB(x, y) != BASE_COLOR) printed++;
            }
        }
        assertTrue(printed > (FILL_X1 - FILL_X0) * (159 - FILL_Y1) / 2,
            "the player-inventory band stopped looking printed (" + printed + " non-base px)");
    }

    /**
     * The wire-format pin: the shipped {@code panel.background} parses through the
     * fork's own IDrawable codec (the exact codec the panel WidgetTheme field uses,
     * IThemeApi.java:62) into a 9-slice AdaptableUITexture of the derived base —
     * full image, borders 4 — and stays in lockstep with the GT6GuiParts.PANEL_BASE
     * constant (which deliberately lives outside ALL: the 12-crop manifest pin in
     * GT6GuiPartsDatagenTest owns that list 1:1).
     */
    @Test
    void panelBackgroundParsesThroughForkCodecToTheDerivedNineSliceBase() throws IOException {
        JsonObject theme = readJson(mdkRoot().resolve(ASSETS_GT6).resolve("themes").resolve("modern.json"));
        JsonObject panel = theme.getAsJsonObject("panel");
        assertTrue(panel != null && panel.has("background"), "the theme must define panel.background");
        JsonElement background = panel.get("background");

        java.util.Objects.requireNonNull(UITexture.CODEC, "the fork texture codec must exist");
        StringBuilder parseError = new StringBuilder();
        java.util.Optional<IDrawable> parsed = IDrawable.CODEC
            .parse(JsonOps.INSTANCE, background).resultOrPartial(parseError::append);
        assertTrue(parsed.isPresent(), "panel.background failed the fork codec parse: " + parseError);
        AdaptableUITexture texture = assertInstanceOf(AdaptableUITexture.class, parsed.get(),
            "panel.background must parse to a 9-slice texture (borders>0 force imageWidth/Height "
                + "in the builder, UITexture.create)");
        assertIsPanelBase(texture, "panel.background");

        // the constant mirror: same sheet, same geometry — keep JSON and code in lockstep
        AdaptableUITexture fromConstant = assertInstanceOf(AdaptableUITexture.class,
            GT6GuiParts.asUITexture(GT6GuiParts.PANEL_BASE),
            "GT6GuiParts.asUITexture(PANEL_BASE) must be a 9-slice texture");
        assertIsPanelBase(fromConstant, "GT6GuiParts.PANEL_BASE");
        assertFalse(GT6GuiParts.ALL.contains(GT6GuiParts.PANEL_BASE),
            "PANEL_BASE is self-derived, not a pack crop — it must stay out of ALL/the 12-crop manifest");
    }

    private static void assertIsPanelBase(AdaptableUITexture texture, String what) {
        assertEquals(BASE_LOCATION, texture.location.toString(), what + " must be the derived panel base");
        assertEquals(176, texture.imageWidth(), what + " imageWidth");
        assertEquals(166, texture.imageHeight(), what + " imageHeight");
        assertEquals(4, texture.bl(), what + " 9-slice border left");
        assertEquals(4, texture.bt(), what + " 9-slice border top");
        assertEquals(4, texture.br(), what + " 9-slice border right");
        assertEquals(4, texture.bb(), what + " 9-slice border bottom");
    }

    /**
     * The headless panel chain: the shipped {@code panel} JSON merged over the fork's
     * default panel theme JSON (the merge ThemeManager.java:408 builds for a
     * DEFAULT-parented chain — the raw JSON alone lacks the required
     * width/color fields) and parsed by the fork's own widget-theme key codec (the
     * ThemeManager.java:425 call) hands exactly that background drawable to a
     * ModularPanel through {@code getWidgetThemeInternal → getPanelTheme}
     * (forge ModularPanel.java:207-208) — the seam every GT6 MUI panel hits at draw
     * time. The fork's own default panel background must differ, so the assertion
     * can't pass vacuously.
     */
    @Test
    void panelThemeChainHandsTheBaseToModularPanels() throws IOException {
        JsonObject theme = readJson(mdkRoot().resolve(ASSETS_GT6).resolve("themes").resolve("modern.json"));
        JsonObject panelJson = theme.getAsJsonObject("panel");

        JsonObject defaultJson = encodePanelTheme(DefaultTheme.INSTANCE.getWidgetTheme(IThemeApi.PANEL));
        JsonObject merged = IThemeApi.PANEL.getMerger()
            .merge(panelJson, ImmutableJson.of(defaultJson), ImmutableJson.of(defaultJson));
        WidgetTheme parsed = IThemeApi.PANEL.parseJson(merged);
        AdaptableUITexture background = assertInstanceOf(AdaptableUITexture.class, parsed.getBackground(),
            "the parsed panel theme background must be the 9-slice base texture");
        assertIsPanelBase(background, "the parsed panel theme background");

        WidgetThemeMap map = new WidgetThemeMap();
        map.register(IThemeApi.FALLBACK, IThemeApi.FALLBACK.getDefaultValue(), IThemeApi.FALLBACK.getDefaultHoverValue());
        map.register(IThemeApi.PANEL, parsed, parsed);
        Theme gt6 = newTheme("gt6", map);

        WidgetThemeEntry<?> entry = new ModularPanel<>("gt6_panel_base_chain_pin").getWidgetThemeInternal(gt6);
        assertEquals(IThemeApi.PANEL, entry.key(),
            "ModularPanel.getWidgetThemeInternal must resolve through the panel theme key "
                + "(forge ModularPanel.java:207-208)");
        assertIsPanelBase(assertInstanceOf(AdaptableUITexture.class, entry.theme().getBackground(),
            "the panel chain must hand widgets the theme background"), "the chained panel background");

        WidgetThemeEntry<?> forkDefault = DefaultTheme.INSTANCE.getWidgetTheme(IThemeApi.PANEL);
        assertFalse(BASE_LOCATION.equals(String.valueOf(
                forkDefault.theme().getBackground() instanceof UITexture t ? t.location : null)),
            "the fork default panel background now looks like our base — rework this vacuity guard");
    }

    /** The fork default panel theme, encoded back to the JSON the merger/codec consume. */
    private static JsonObject encodePanelTheme(WidgetThemeEntry<?> entry) {
        var builder = JsonOps.INSTANCE.mapBuilder();
        entry.encode(JsonOps.INSTANCE, builder, true);
        return builder.build(JsonOps.INSTANCE.empty()).result().orElseThrow(
            () -> new AssertionError("encoding the fork default panel theme failed")).getAsJsonObject();
    }

    /**
     * The fork builds themes only inside its reload listener (ThemeManager.apply), on a
     * package-private constructor. One reflective step here stands in for that call:
     * same constructor, same arguments shape (id, parent theme, parsed map), so the
     * dispatch under test is the shipped Theme, not a test double.
     */
    private static Theme newTheme(String id, WidgetThemeMap map) {
        try {
            Constructor<Theme> ctor = Theme.class.getDeclaredConstructor(
                String.class, brachy.modularui.api.ITheme.class, WidgetThemeMap.class);
            ctor.setAccessible(true);
            return ctor.newInstance(id, DefaultTheme.INSTANCE, map);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("the fork Theme constructor changed shape — re-pin", e);
        }
    }
}
