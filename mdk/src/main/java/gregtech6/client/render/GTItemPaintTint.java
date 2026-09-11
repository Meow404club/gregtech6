package gregtech6.client.render;

import javax.annotation.Nullable;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The machine paint tint, the INVENTORY half (task p22-painted-item-domain) — the item
 * counterpart of {@link GTMachinePaintTint} over the same machine-domain census: a placed
 * painted machine now drops a stack whose NBT carries {@code gt.color}/{@code gt.painted}
 * under {@code BlockEntityTag} (the GT6LootTables copy_nbt function), and this
 * {@link ItemColor} renders that stack in the paint colour.
 *
 * <p>Explicit registration is mandatory: vanilla {@code ItemColors.createDefault} has NO
 * BlockItem delegation (ItemColors.java:25-93 — the grass/leaves rows :72-88 are
 * hand-written forwards), the Forge-docs "a BlockColor does NOT colour its BlockItem"
 * caveat of the wire/prefix cards applies verbatim here.
 *
 * <p>TWO-LEVEL READ (task p23-painted-item-tag-fix, kb-painted-item-tag-mismatch): the
 * loot copy_nbt lands the paint pair under {@code BlockEntityTag} (GT6LootTables.paintCopyNbt —
 * the vanilla placement read-back key, {@code BlockItem.BLOCK_ENTITY_TAG}), so the first-pass
 * root-tag-only read missed the dropped stacks (no inventory tint). The lambda now probes the
 * {@code BlockEntityTag} compound first and falls back to the root tag — the loot form and the
 * root form are both tinted, the write side stays untouched ({@code BlockEntityTag} is the
 * load-bearing placement key).
 *
 * <p>COLOUR SOURCE (task p27-machine-material-tint-fidelity): the unpainted arms no longer
 * short-circuit to the {@code -1} sentinel — an UNPAINTED machine stack renders its row
 * material (the upstream item colour is a function of the MTE id's registration row, not of
 * item NBT: writeItemNBT2 carries no NBT_COLOR, and the creative-tab/JEI enumeration rides
 * the canonical TE colour, MultiTileEntityItemInternal.java:145-147). The material resolves
 * through {@link GTBasicMachineBlock#materialOf} on the stack's BlockItem block — the same
 * domain gate as the world half, so material-less stacks (the P23 barrel co-registration,
 * the vanilla-fixture stacks of the offline tests) keep the {@code -1} white identity
 * byte-identical. A painted stack still wins (the spray override). The value math is NOT
 * duplicated: every arm funnels into the P21 pure seam
 * {@link GTMachinePaintTint#tintARGB(ModelData, OreDictMaterial, int)} — the single
 * decision site.
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from GTClientHandlers under
 * the dist guard, same as the P21 world half).
 */
@OnlyIn(Dist.CLIENT)
public final class GTItemPaintTint {

    private GTItemPaintTint() {
    }

    /**
     * The inventory half: registered over the {@code BlockItems of
     * GTMachines.paintableBlockArray()} (GTClientHandlers). Index 0 reads the stack NBT in the
     * two carried forms — the loot copy_nbt payload ({@code BlockEntityTag.gt.*}, preferred
     * when its compound carries the painted flag) and the root-tag fallback —; the unpainted
     * guards (no tag / no painted flag / no colour key, on the selected compound) fall back to
     * the row material, or the {@code -1} sentinel when the block carries no NBT_MATERIAL
     * column (no snapshot allocation on that arm). Index non-zero is the no-tint sentinel
     * like every other GT6 tint seam.
     */
    public static ItemColor itemColor() {
        return (@Nullable ItemStack aStack, int aTintIndex) -> {
            if (aTintIndex != 0 || aStack == null) return -1;
            OreDictMaterial tMaterial = aStack.getItem() instanceof BlockItem tItem
                    ? GTBasicMachineBlock.materialOf(tItem.getBlock()) : null;
            //? if forge {
            CompoundTag tTag = aStack.getTag();
            //?} else {
            /*CompoundTag tTag = aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); // the 21.1 tag envelope (GT6Circuits read fork shape)
            *///?}
            if (tTag == null) return GTMachinePaintTint.tintARGB(null, tMaterial, 0);
            // The loot form first: copy_nbt writes the pair into BlockEntityTag (vanilla
            // BlockItem.BLOCK_ENTITY_TAG, BlockItem.java:34 — same literal GT6LootTables
            // .paintCopyNbt targets on both legs). getCompound is an empty compound when the
            // key is absent, so the probe is null-safe; a payload WITHOUT the painted flag
            // (absent payload, an unrelated BE compound, or an unpainted machine) falls back
            // to the root form. The guards below apply to the selected compound only — no
            // cross-compound mixing (a painted-flag-without-colour payload falls back to the
            // row material like every other unpainted arm).
            CompoundTag tPaintTag = tTag.getCompound("BlockEntityTag");
            if (!tPaintTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED, Tag.TAG_ANY_NUMERIC)) tPaintTag = tTag;
            if (!tPaintTag.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED)) return GTMachinePaintTint.tintARGB(null, tMaterial, 0);
            if (!tPaintTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_ANY_NUMERIC)) return GTMachinePaintTint.tintARGB(null, tMaterial, 0);
            return GTMachinePaintTint.tintARGB(GTModelProperties.snapshot()
                    .with(GTModelProperties.PAINT, Integer.valueOf(tPaintTag.getInt(TileEntityBase03TicksAndSync.NBT_COLOR)))
                    .build(), tMaterial, 0);
        };
    }
}
