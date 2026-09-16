package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.item.enchantment.Enchantments;

import gregapi.data.OP;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

//? if neoforge {
/*
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
 *///?}

/**
 * Loot tables of the ore universe (task p30-ore-4-loot): one table per block of the
 * 3922-block per-pair registration walk ({@link GT6OreBlocks#blocks()}, 26 stone families
 * x 53 materials x the family's forms), at the vanilla default {@code gt6:blocks/<path>}
 * location — zero block code, the {@link GT6StoneBlockLoot} per-pair precedent. The
 * sub-provider rides the existing {@link GT6LootTables} provider via the single
 * tail-appended {@code SubProviderEntry} line (the shared-seam ruling).
 *
 * <p><b>The normal-block dispatch</b> — the upstream four-way ternary
 * (gregapi/block/behaviors/Drops.java:72-76, the Drops(oreBroken, ore, OP.oreRaw...)
 * wiring Loader_Ores.java:77-83) arm-ified:
 * <ul>
 * <li>{@code fortune > 0} → the OP.oreRaw item of the SAME material, count
 *     {@code 1 + uniform(0..fortune)} — vanilla {@code apply_bonus}
 *     {@code uniform_bonus_count} with bonusMultiplier 1 is exactly this distribution
 *     (ApplyBonusCount.UniformBonusCount.calculateNewCount = count + nextInt(mult x
 *     fortune + 1), vendored 1.21.1 refs ApplyBonusCount.java:164-176; Drops.java:74
 *     {@code mFortunable?1+RNGSUS.nextInt(aFortune+1):1}); the raw arm sits FIRST because
 *     upstream is mPreferSilk=false — fortune wins over silk (Drops.java:74 both
 *     fortune>0 branches read the mDropFortune/mDropSilkFortune pair);</li>
 * <li>three-form families: {@code fortune == 0} + silk → the block itself
 *     (mDropSilkTouch); {@code fortune == 0} + bare hands → the broken-pair block item
 *     (mDropNormal, the {@code ore_broken_<family>_<material>} sibling);</li>
 * <li>loose dust families (gravel/sand/redsand/mud, broken≡normal, Loader_Ores.java
 *     :81-83 + :128 — mDropNormal == mDropSilkTouch == the block itself): {@code
 *     fortune == 0} → self under every condition.</li>
 * </ul>
 * No explosion_decay on the raw arm: upstream 1.7.10 has no decay on PrefixBlock drops —
 * the vanilla-ore explosion_decay face is a declared omit.
 *
 * <p><b>The broken form</b> is the upstream mDrops==null default
 * ({@code new Drops(this, this, this, this, F, F, 0, 0)}, PrefixBlock.java:227) — the
 * plain self-drop, dropSelf.
 *
 * <p><b>The small form</b> drops {@code 1 x rockGt(X)} — the "X bearing Rock" item of the
 * BLOCK's own material (ruling 2026-09-16: the ore-bearing-rock reading, which covers all
 * 26 families; the Drops_SmallOre seed-material column cannot be taken verbatim as the
 * rock's material because mud's seed is null, Loader_Ores.java:122, and deepslate has no
 * upstream row). This is the declared collapse of the Drops_SmallOre gem-selector +
 * secondary-dust + gemLegendary chance face (Drops_SmallOre.java:48-103) into one item —
 * no fortune/silk dispatch on small ores.
 *
 * <p><b>The XP column does NOT ride the loot tables</b>: neither leg's loot JSON has an
 * experience field (LootTable.java:40-44 fields = paramSet/randomSequence/pools/functions;
 * LootPool carries none either, both legs verified) — vanilla attaches ore XP on the
 * BLOCK (DropExperienceBlock's IntProvider, Blocks.java:390; Forge's
 * IForgeBlockState.getExpDrop default 0). The block files are ore-1's surface (this
 * card's FILES_SCOPE is datagen+test only), so the family XP column
 * ({@code OreFamily#xpMin/xpMax} — the nine vanilla anchors: 0-1 regular, endstone 2-3,
 * Loader_Ores.java:77-83; the 17 GT stones: 0..max(1, level), Loader_Rocks.java:144)
 * stays a pinned data face asserted by GT6OreLootParityTest, with the block-side
 * attachment (getExpDrop/DropExperienceBlock semantics) as the declared carry. Declared
 * deviation with it: upstream small ores carry their own 1..3 XP (Drops_SmallOre
 * super(1, 2)) — the port reads the family column family-uniform.
 *
 * <p><b>Dual leg</b>: 1.20.1 {@code ItemPredicate.Builder.hasEnchantment} +
 * {@code ApplyBonusCount.addUniformBonusCount(Enchantment)} + {@code
 * Enchantments.BLOCK_FORTUNE}; 21.1 sub-predicate enchantments + Holder-based
 * {@code addUniformBonusCount} + {@code Enchantments.FORTUNE} (BlockLootSubProvider
 * :77-79 vs :85-96, :243 vs :309/:381 of the vendored trees). The canonical tracked tree
 * is the forge-leg JSON (ADR-P17-1).
 *
 * <p>KJS face (card declaration): standard datapack loot JSON — zero adaptation, the
 * tables are datapack-overridable like any vanilla block table.
 */
public final class GT6OreLootTables {

    private GT6OreLootTables() {
    }

    /** Which arm of the dispatch a mining condition takes — the registry-free decision face. */
    public enum Arm { FORTUNE_RAW, SILK_SELF, PLAIN }

    /**
     * The arm a (fortune, silk) mining condition takes on one (family, form, material)
     * block — the Drops.java:74 ternary, arm-ified: fortune beats silk (the
     * mPreferSilk=false face); the separate silk arm exists only for the three-form
     * normal blocks (the broken-pair families); small ores and broken blocks have no
     * dispatch (the PLAIN arm under every condition).
     */
    public static Arm armOf(GT6OreBlocks.OreKey key, int fortune, boolean silkTouch) {
        if (key.kind() == GT6OreBlocks.FormKind.NORMAL && fortune > 0) return Arm.FORTUNE_RAW;
        if (key.kind() == GT6OreBlocks.FormKind.NORMAL && key.family().broken() != null && silkTouch) return Arm.SILK_SELF;
        return Arm.PLAIN;
    }

    /**
     * The registry-id path of the item an arm drops — the offline parity face: raw =
     * the OP.oreRaw item of the block's material (the upstream OP.oreRaw fortune pair,
     * Loader_Ores.java:77-83); silk/self = the block's own item; plain = the block's own
     * item EXCEPT the three-form normal (the broken-pair sibling) and the small form
     * (the rockGt(X) bearing rock, the ruling above).
     */
    public static String armPath(GT6OreBlocks.OreKey key, Arm arm) {
        return switch (arm) {
            case FORTUNE_RAW -> GTMaterialItems.itemIdOf(OP.oreRaw, key.material());
            case SILK_SELF -> GT6OreBlocks.path(key);
            case PLAIN -> switch (key.kind()) {
                case SMALL -> GTMaterialItems.itemIdOf(OP.rockGt, key.material());
                case BROKEN -> GT6OreBlocks.path(key);
                case NORMAL -> key.family().broken() == null ? GT6OreBlocks.path(key)
                        : GT6OreBlocks.path(new GT6OreBlocks.OreKey(key.family(), GT6OreBlocks.FormKind.BROKEN, key.material()));
            };
        };
    }

    /** The 3922 ore blocks in registration order — the getKnownBlocks narrowing face. */
    public static List<Block> oreLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.blocks().keySet()) rBlocks.add(GT6OreBlocks.blocks().get(tKey).get());
        return rBlocks;
    }

    /**
     * The ore sub-provider — the {@link GT6StoneBlockLoot} per-pair shape over the
     * {@link GT6OreBlocks} walk, getKnownBlocks narrowed to exactly the 3922 (the
     * GT6LootTables iron rule, GT6LootTables.java:43-64).
     */
    public static final class GT6OreBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6OreBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6OreBlockLoot() {
            // no explosion-resistant items — the raw arm needs no decay exemption (and carries none)
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return oreLootBlocks();
        }

        /** The block's own item (the SILK_SELF / loose-plain / broken arm). */
        private static Item selfItem(GT6OreBlocks.OreKey key) {
            return GT6OreBlocks.items().get(key).get();
        }

        /** The broken-pair sibling's item (the three-form normal plain arm). */
        private static Item brokenPairItem(GT6OreBlocks.OreKey key) {
            return GT6OreBlocks.items().get(new GT6OreBlocks.OreKey(key.family(), GT6OreBlocks.FormKind.BROKEN, key.material())).get();
        }

        /** The OP.oreRaw item of the block's material (the FORTUNE_RAW arm). */
        private static Item rawItem(GT6OreBlocks.OreKey key) {
            return GTMaterialItems.get(OP.oreRaw, key.material()).get();
        }

        /** The OP.rockGt item of the block's material (the small form's bearing rock). */
        private static Item rockItem(GT6OreBlocks.OreKey key) {
            return GTMaterialItems.get(OP.rockGt, key.material()).get();
        }

        //? if forge {
        /** The vanilla HAS_SILK_TOUCH constant (BlockLootSubProvider.java:77-79, vendored 1.20.1) — the parent face. */
        private static LootItemCondition.Builder silkTouch() {
            return HAS_SILK_TOUCH;
        }

        /** The fortune twin of the parent's HAS_SILK_TOUCH ({@code Enchantments.BLOCK_FORTUNE} in 1.20.1). */
        private static LootItemCondition.Builder hasFortune() {
            return MatchTool.toolMatches(ItemPredicate.Builder.item()
                    .hasEnchantment(new EnchantmentPredicate(Enchantments.BLOCK_FORTUNE, MinMaxBounds.Ints.atLeast(1))));
        }

        /** {@code apply_bonus / uniform_bonus_count}, bonusMultiplier 1 — the Drops.java:74 count distribution. */
        private static LootItemFunction.Builder fortuneUniformBonus() {
            return ApplyBonusCount.addUniformBonusCount(Enchantments.BLOCK_FORTUNE);
        }
        //?} else {
        /*private LootItemCondition.Builder silkTouch() {
            return hasSilkTouch(); // the vanilla 21.1 parent method, BlockLootSubProvider.java:85-96
        }

        private LootItemCondition.Builder hasFortune() {
            return MatchTool.toolMatches(ItemPredicate.Builder.item().withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
                    ItemEnchantmentsPredicate.enchantments(java.util.List.of(new EnchantmentPredicate(
                            registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                            MinMaxBounds.Ints.atLeast(1))))));
        }

        private LootItemFunction.Builder fortuneUniformBonus() {
            return ApplyBonusCount.addUniformBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE));
        }
         *///?}

        @Override
        protected void generate() {
            for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.blocks().keySet()) {
                Block tBlock = GT6OreBlocks.blocks().get(tKey).get();
                switch (tKey.kind()) {
                    case BROKEN -> dropSelf(tBlock); // the mDrops==null default, PrefixBlock.java:227
                    case SMALL -> add(tBlock, LootTable.lootTable()
                            .withPool(LootPool.lootPool()
                                    .setRolls(ConstantValue.exactly(1.0F))
                                    .when(ExplosionCondition.survivesExplosion())
                                    .add(LootItem.lootTableItem(rockItem(tKey)))));
                    case NORMAL -> add(tBlock, tKey.family().broken() == null
                            ? looseDispatchTable(tKey) : oreDispatchTable(tKey));
                }
            }
        }

        /** The three-form normal table: raw(fortune) → self(silk) → the broken-pair item, first-match-wins. */
        private LootTable.Builder oreDispatchTable(GT6OreBlocks.OreKey key) {
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(rawItem(key))
                                    .when(hasFortune())
                                    .apply(fortuneUniformBonus())
                                    .otherwise(LootItem.lootTableItem(selfItem(key))
                                            .when(silkTouch())
                                            .otherwise(LootItem.lootTableItem(brokenPairItem(key))))));
        }

        /** The loose dust-family table (broken≡normal): raw(fortune) → self, silk is not a separate arm. */
        private LootTable.Builder looseDispatchTable(GT6OreBlocks.OreKey key) {
            return LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(rawItem(key))
                                    .when(hasFortune())
                                    .apply(fortuneUniformBonus())
                                    .otherwise(LootItem.lootTableItem(selfItem(key)))));
        }
    }
}
