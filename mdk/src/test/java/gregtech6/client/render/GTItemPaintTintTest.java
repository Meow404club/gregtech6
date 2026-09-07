/*
 * Offline tests for task p22-painted-item-domain: the GTItemPaintTint inventory-half
 * mapping — the ItemColor over the stack NBT the loot copy_nbt function carries. The seam
 * reads the tag only (any item's stack works), so vanilla-registry stacks offline-booted
 * per GTOfflineRenderTestBase drive every arm; the value math rides the P21
 * GTMachinePaintTint.tintARGB seam (its own test pins the identity).
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

class GTItemPaintTintTest extends GTOfflineRenderTestBase {

    /** A painted colour as the 03 base stores it (0xRRGGBB, the direct-storage ruling). */
    private static final int PAINT_RED = 0xFF0000;

    /** A stack in the exact shape the leg's loot copy function leaves it (tag / CUSTOM_DATA envelope). */
    private static ItemStack lootCarriedStack(@Nullable Integer aColor, boolean aPainted) {
        ItemStack tStack = new ItemStack(Items.BRICKS, 1); // the seam reads the tag, item type irrelevant
        //? if forge {
        CompoundTag tTag = tStack.getOrCreateTag();
        //?} else {
        /*net.minecraft.nbt.CompoundTag tTag = tStack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        *///?}
        tTag.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, aPainted);
        if (aColor != null) tTag.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, aColor);
        //? if neoforge {
        /*tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tTag)); // the 21.1 copy_custom_data carrier
        *///?}
        return tStack;
    }

    /** Acceptance 3a: a painted stack tints with the stored colour at index 0. */
    @Test
    void paintedStackTintsWithTheStoredColour() {
        assertEquals(0xFFFF0000, GTItemPaintTint.itemColor().getColor(lootCarriedStack(PAINT_RED, true), 0),
                "red paint 0xFF0000 -> ARGB 0xFFFF0000 (the P21 tintARGB seam value)");
        assertEquals(0xFF202020, GTItemPaintTint.itemColor().getColor(lootCarriedStack(0x202020, true), 0),
                "the CS DYE_Black row value tints dark gray");
    }

    /** Acceptance 3b: unpainted stacks return the -1 no-tint sentinel (all guard arms). */
    @Test
    void unpaintedStackIsNoTint() {
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(new ItemStack(Items.BRICKS, 1), 0),
                "a bare stack (no tag) is no tint");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(PAINT_RED, false), 0),
                "a colour key without the painted flag is no tint (the BE writes the pair only while painted)");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(null, true), 0),
                "a painted flag without a colour key is no tint (defensive against a malformed copy)");
    }

    /** Acceptance 3c: a non-zero tint index is never tinted, even on a painted stack. */
    @Test
    void nonZeroTintIndexNeverTints() {
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(PAINT_RED, true), 1));
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(PAINT_RED, true), 3));
    }

    /** The stored colour is masked to 24-bit exactly like the world half (tintARGB & 0xFFFFFF). */
    @Test
    void storedColourIsMaskedTo24Bit() {
        assertEquals(0xFFFF0000, GTItemPaintTint.itemColor().getColor(lootCarriedStack(0x1FF0000, true), 0),
                "the alpha/overflow bits of the stored int are masked away");
    }
}
