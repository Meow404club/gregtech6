/*
 * Offline tests for task p22-painted-item-domain + the p23-painted-item-tag-fix two-level
 * read: the GTItemPaintTint inventory-half mapping — the ItemColor over the stack NBT the
 * loot copy_nbt function carries. The seam reads the tag only (any item's stack works), so
 * vanilla-registry stacks offline-booted per GTOfflineRenderTestBase drive every arm; the
 * value math rides the P21 GTMachinePaintTint.tintARGB seam (its own test pins the identity).
 *
 * <p>Both carried shapes are pinned: the REAL loot form (copy_nbt under BlockEntityTag,
 * GT6LootTables.paintCopyNbt — the shape the p22 first-pass tests missed, which is why the
 * kb-painted-item-tag-mismatch dead seam never went red) and the root-tag fallback form.
 *
 * <p>task p27-machine-material-tint-fidelity re-based the unpainted arms on the row
 * material (the upstream item colour is a registration-row function, not item NBT). The
 * stacks below ride Items.BRICKS — a material-LESS block by the
 * {@code GTBasicMachineBlock.materialOf} gate — so every {@code -1} assertion here pins
 * the material-less arm, which stays byte-identical (full-alpha white == -1); the
 * machine-item material arm shares the P21 seam pinned in GTMachinePaintTintTest
 * (0xFFFF825A Cu / 0xFF828282 Steel) and the live items ride the runServer registration gate.
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

    /** A paint the seam must NOT pick when the BlockEntityTag payload wins (the priority pin). */
    private static final int PAINT_BLUE = 0x0000FF;

    /** The vanilla placement payload key — BlockItem.BLOCK_ENTITY_TAG, BlockItem.java:34; the literal GT6LootTables.paintCopyNbt targets on both legs. */
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    /** A stack in the ROOT-tag form (the p22 first-pass shape, kept as the fallback-arm regression). */
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

    /** A stack in the REAL loot form (kb-painted-item-tag-mismatch): copy_nbt lands the paint pair under BlockEntityTag, the root tag itself stays bare. */
    private static ItemStack beTaggedPaintStack(@Nullable Integer aColor, boolean aPainted) {
        CompoundTag tBeTag = new CompoundTag();
        tBeTag.putBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED, aPainted);
        if (aColor != null) tBeTag.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, aColor);
        ItemStack tStack = new ItemStack(Items.BRICKS, 1);
        //? if forge {
        tStack.getOrCreateTag().put(BLOCK_ENTITY_TAG, tBeTag);
        //?} else {
        /*net.minecraft.nbt.CompoundTag tTag = tStack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tTag.put(BLOCK_ENTITY_TAG, tBeTag);
        tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
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

    /** Acceptance 3b: unpainted MATERIAL-LESS stacks return the -1 no-tint sentinel (all guard arms). */
    @Test
    void unpaintedStackIsNoTint() {
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(new ItemStack(Items.BRICKS, 1), 0),
                "a bare stack (no tag, no material) is no tint — the material-less arm of the fidelity fallback");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(PAINT_RED, false), 0),
                "a colour key without the painted flag is no tint on a material-less block (the BE writes the pair only while painted)");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(lootCarriedStack(null, true), 0),
                "a painted flag without a colour key is no tint on a material-less block (defensive against a malformed copy)");
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

    /** p23 acceptance: the REAL loot form (paint pair under BlockEntityTag) tints — the dead-seam shape, now the primary arm. */
    @Test
    void lootBlockEntityTagFormTints() {
        assertEquals(0xFFFF0000, GTItemPaintTint.itemColor().getColor(beTaggedPaintStack(PAINT_RED, true), 0),
                "the BlockEntityTag form GT6LootTables.paintCopyNbt actually leaves tints at index 0");
        assertEquals(0xFF202020, GTItemPaintTint.itemColor().getColor(beTaggedPaintStack(0x202020, true), 0),
                "the BE form rides the same tintARGB value math");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(beTaggedPaintStack(PAINT_RED, false), 0),
                "an unpainted BE payload is no tint (copy_nbt only writes the pair while painted)");
        assertEquals(-1, GTItemPaintTint.itemColor().getColor(beTaggedPaintStack(null, true), 0),
                "a malformed BE payload (painted flag without a colour key) is no tint — no cross-compound mixing");
    }

    /** The BlockEntityTag payload is authoritative: when both forms are present the root colour must NOT leak through. */
    @Test
    void blockEntityTagFormWinsOverRootForm() {
        ItemStack tStack = beTaggedPaintStack(PAINT_RED, true);
        //? if forge {
        tStack.getOrCreateTag().putInt(TileEntityBase03TicksAndSync.NBT_COLOR, PAINT_BLUE);
        //?} else {
        /*net.minecraft.nbt.CompoundTag tTag = tStack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tTag.putInt(TileEntityBase03TicksAndSync.NBT_COLOR, PAINT_BLUE);
        tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tTag));
        *///?}
        assertEquals(0xFFFF0000, GTItemPaintTint.itemColor().getColor(tStack, 0),
                "the BlockEntityTag payload wins over a root-tag colour (the placement key is authoritative)");
    }

    /** A BlockEntityTag compound WITHOUT the paint keys (e.g. chest-style payload) must not mask the root form. */
    @Test
    void paintlessBlockEntityTagFallsBackToRoot() {
        ItemStack tStack = lootCarriedStack(PAINT_RED, true);
        //? if forge {
        tStack.getOrCreateTag().put(BLOCK_ENTITY_TAG, new CompoundTag());
        //?} else {
        /*net.minecraft.nbt.CompoundTag tTag = tStack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tTag.put(BLOCK_ENTITY_TAG, new net.minecraft.nbt.CompoundTag());
        tStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tTag));
        *///?}
        assertEquals(0xFFFF0000, GTItemPaintTint.itemColor().getColor(tStack, 0),
                "an empty BlockEntityTag compound falls back to the root-tag form (the payload probe is key-gated)");
    }
}
