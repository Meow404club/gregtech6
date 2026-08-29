package gregtech6.gui;

import net.minecraft.world.Container;

/**
 * GT6 display-only slot — direct translation of gregapi/gui/Slot_Render.java:34, which is a
 * {@code Slot_Holo} with everything closed ({@code false, false, 0}): nothing goes in, nothing
 * comes out, the slot never holds live content. GT6 GUIs render whatever a screen wants to show
 * in these positions from their own draw code.
 *
 * <p>Cropped vs upstream (noted for the ADR-P3-6 pool): Slot_Render.putStack wrote the stack into
 * the client-side tile entity so 1.7.10's manual render pass could read it back. That client-side
 * write-back pipeline belongs to the deferred slotClick/render pool — the minimal port only
 * guarantees the slot itself is inert; ghost-content rendering is screen-side code.
 */
public class GTRenderSlot extends GTHoloSlot {

    public GTRenderSlot(Container container, int index, int x, int y) {
        super(container, index, x, y, false, false, 0); // Slot_Render.java:37-39
    }
}
