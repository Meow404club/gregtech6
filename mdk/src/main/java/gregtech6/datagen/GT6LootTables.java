package gregtech6.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.registry.GTMaterialBlocks;

/**
 * Loot tables of the material prefix blocks (task p8-prefixblock-render spec ④). One
 * self-drop table per block, at the vanilla default location
 * {@code gt6:blocks/<registry-path>} — {@code Block.getLootTable()} already resolves there
 * with ZERO block code (vanilla default {@code "blocks/" + registry path}), so
 * {@link GTMaterialPrefixBlock} stays untouched.
 *
 * <p>Upstream translation: every storage PrefixBlock registers {@code Drops == null}, which
 * defaults to {@code new Drops(this, this, this, this, F, F, 0, 0)} (PrefixBlock.java:227) —
 * normal/silk/fortune/silkFortune all drop the block itself, not for tunable, no XP. The
 * 1.20.1 equivalent is exactly {@link BlockLootSubProvider#dropSelf}: one unconditional
 * single-item pool over the block's own item, no silk-touch/fortune dispatch, no experience
 *; the pool keeps the vanilla {@code survives_explosion} condition (true whenever no
 * explosion context exists), which is the vanilla block-table convention this card ports.
 *
 * <p>Table count == block count == 3773 (the card-A census yardstick): {@link #generate()}
 * maps 1:1 over {@link GTMaterialBlocks#blockArray()}, and {@link #getKnownBlocks()} is
 * narrowed to the same array so the provider's missing-table validation only covers the
 * blocks this card owns (Forge patch on BlockLootSubProvider; machines/barrels/pipes are
 * other cards' surfaces and must not be validated here).
 */
public final class GT6LootTables extends LootTableProvider {

    public GT6LootTables(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(GT6BlockLoot::new, LootContextParamSets.BLOCK)));
    }

    /** The block list this provider owns: exactly the material prefix block array (the census walk order). */
    public static List<Block> lootBlocks() {
        return List.of(GTMaterialBlocks.blockArray());
    }

    public static final class GT6BlockLoot extends BlockLootSubProvider {

        public GT6BlockLoot() {
            // no explosion-resistant items: every drop survives-explosion-gated (vanilla default form)
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return lootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : lootBlocks()) dropSelf(tBlock); // Drops(this,this,this,this,F,F,0,0) direct translation
        }
    }
}
