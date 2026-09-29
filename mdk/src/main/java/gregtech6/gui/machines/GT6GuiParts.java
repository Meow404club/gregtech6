package gregtech6.gui.machines;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * Single source of truth for the cropped composable GUI part sprites
 * (task r8-gui-part-crops; visual-source ruling decisions.r8-gui-visual-source).
 *
 * <p>Every sprite is a rect crop out of the amazawa resource pack's gregtech
 * domain (machine skins) or its minecraft domain (reskinned vanilla widgets —
 * the sanctioned generic-part fallback). TFC-domain art is NOT used here
 * (2026-09-29 ruling: GT6 does not ship TFC art). Full provenance ledger with
 * per-part source file + source sha256 + crop rect lives in
 * {@code mdk/src/main/resources/assets/README.md}, "GUI part crops" section, and
 * is machine-pinned by {@code GT6GuiPartsDatagenTest} +
 * {@code mdk/tools/crop_gui_parts.py} / {@code mdk/tools/parts_manifest.json}.
 * Parts the pack does not draw (large fluid tank frames, thermometer/ruler scale
 * bars, reverse progress arrows, GT semantic slot strokes, tier decorations) are
 * deliberately NOT here — deferred to a later card (author may draw them,
 * upstream crops as fallback).</p>
 *
 * <p>Data only: no rendering, no consumers yet (assembly is the D-card / future
 * layout-card face). Coordinates are GUI-texture pixels (1 px = 1 GUI unit).</p>
 */
public final class GT6GuiParts {

    /** One cropped part: texture path (always under textures/gui/parts/), size, 9-slice borders. */
    public record GuiPart(String fileName, int width, int height,
                          int borderLeft, int borderTop, int borderRight, int borderBottom) {
        public ResourceLocation texture() {
            return ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/parts/" + fileName);
        }
    }

    /** Full 176x166 machine background frame — the 9-slice panel source; borders cover the dark outline + white highlight + corner rounding. */
    public static final GuiPart PANEL_BACKGROUND = new GuiPart("panel_176x166.png", 176, 166, 4, 4, 4, 4);

    /** Standard 18x18 item slot, white stroke. */
    public static final GuiPart SLOT_FRAME = new GuiPart("slot_frame_18x18.png", 18, 18, 0, 0, 0, 0);

    /** 54x36 3x2 slot-group / display frame. */
    public static final GuiPart SLOT_FRAME_GROUP_3X2 = new GuiPart("slot_frame_group_54x36.png", 54, 36, 0, 0, 0, 0);

    /** 36x36 2x2 slot-group frame (crafting-grid look). */
    public static final GuiPart SLOT_FRAME_GROUP_2X2 = new GuiPart("slot_frame_group_2x2_36x36.png", 36, 36, 0, 0, 0, 0);

    /** 22x22 dark-stroke special slot (gear icon baked in the source crop). */
    public static final GuiPart SLOT_SPECIAL = new GuiPart("slot_special_22x22.png", 22, 22, 0, 0, 0, 0);

    /** 18x19 fluid display cell (droplet icon baked; amazawa's fluid-slot look from the distillation grids). */
    public static final GuiPart SLOT_FLUID = new GuiPart("slot_fluid_18x19.png", 18, 19, 0, 0, 0, 0);

    /** Flat button 200x20, normal state (amazawa reskin of the vanilla widget row). 9-slice-able. */
    public static final GuiPart BUTTON_FLAT = new GuiPart("button_flat_200x20.png", 200, 20, 2, 2, 2, 2);

    /** Flat button 200x20, hover state (blue face — the pack's hover look). 9-slice-able. */
    public static final GuiPart BUTTON_FLAT_HOVER = new GuiPart("button_flat_hover_200x20.png", 200, 20, 2, 2, 2, 2);

    /** Forward progress arrow strip, 20x18 — same geometry as the upstream UV(176,0) overlay the vanilla-Menu leg blits. */
    public static final GuiPart ARROW_FORWARD = new GuiPart("arrow_forward_20x18.png", 20, 18, 0, 0, 0, 0);

    /** Forward progress arrow, red machine accent (Melter skin strip). */
    public static final GuiPart ARROW_FORWARD_RED = new GuiPart("arrow_forward_red_20x18.png", 20, 18, 0, 0, 0, 0);

    /** Forward progress arrow, cyan machine accent (Freezer skin strip). */
    public static final GuiPart ARROW_FORWARD_CYAN = new GuiPart("arrow_forward_cyan_20x18.png", 20, 18, 0, 0, 0, 0);

    /** Player inventory block: 3x9 rows + hotbar (4 px row gap included, gap pixels are plain panel color). */
    public static final GuiPart PLAYER_INVENTORY = new GuiPart("player_inventory_162x76.png", 162, 76, 0, 0, 0, 0);

    /** Every part, in ledger order — the test walks this against the manifest. */
    public static final List<GuiPart> ALL = List.of(
        PANEL_BACKGROUND, SLOT_FRAME, SLOT_FRAME_GROUP_3X2, SLOT_FRAME_GROUP_2X2,
        SLOT_SPECIAL, SLOT_FLUID, BUTTON_FLAT, BUTTON_FLAT_HOVER,
        ARROW_FORWARD, ARROW_FORWARD_RED, ARROW_FORWARD_CYAN, PLAYER_INVENTORY);

    private GT6GuiParts() {
    }
}
