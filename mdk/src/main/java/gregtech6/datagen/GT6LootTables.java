package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTWires;

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
 * blocks this card owns (the loader-patched {@code getKnownBlocks} narrowing hook exists
 * on BOTH legs — the Forge 1.20.1 patch and a NeoForge 21.1 override alike, javap-verified;
 * machines/barrels/pipes are
 * other cards' surfaces and must not be validated here).
 */
public final class GT6LootTables extends LootTableProvider {

    /**
     * {@code lookupProvider} is the shared cross-version seam: Forge 1.20.1's GatherDataEvent
     * already exposes {@code getLookupProvider()} (the forge-1.20.1 datagen docs pass it the
     * same way), so the constructor signature is identical on both legs — only the
     * {@code super} wiring differs. 1.21.1 javap ground truth: LootTableProvider grew a 4th
     * constructor parameter (the registry lookup future) and SubProviderEntry's first
     * component changed from Supplier to Function&lt;HolderLookup.Provider,
     * LootTableSubProvider&gt; — the same {@code GT6XxxLoot::new} references satisfy both
     * shapes via the forked sub-provider constructors below (provider-arg on 1.21.1,
     * no-arg on 1.20.1). The 1.20.1 leg takes the parameter and ignores it.
     */
    public GT6LootTables(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        //? if neoforge {
        /*
        super(output, Set.of(), List.of(
                new SubProviderEntry(GT6BlockLoot::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(GT6WireBlockLoot::new, LootContextParamSets.BLOCK), // task p9-wire-family-w1 ⑥
                new SubProviderEntry(GT6AxleBlockLoot::new, LootContextParamSets.BLOCK), // task p12-axle-family
                new SubProviderEntry(GT6EngineBlockLoot::new, LootContextParamSets.BLOCK), // task p12-engine-diesel
                new SubProviderEntry(GT6KineticsBlockLoot::new, LootContextParamSets.BLOCK), // task p12-gearbox-transformer
                new SubProviderEntry(GT6BurningBoxBlockLoot::new, LootContextParamSets.BLOCK), // task p13-burning-box-family
                new SubProviderEntry(GT6BoilerTankBlockLoot::new, LootContextParamSets.BLOCK), // task p13-boiler-tank
                new SubProviderEntry(GT6DryerBlockLoot::new, LootContextParamSets.BLOCK), // task p14-dryer-family
                new SubProviderEntry(GT6DistilleryBlockLoot::new, LootContextParamSets.BLOCK), // task p16-distillery-family
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK)), // task p19-stoneblocks-render
            lookupProvider);
         *///?} else {
        super(output, Set.of(), List.of(
                new SubProviderEntry(GT6BlockLoot::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(GT6WireBlockLoot::new, LootContextParamSets.BLOCK), // task p9-wire-family-w1 ⑥
                new SubProviderEntry(GT6AxleBlockLoot::new, LootContextParamSets.BLOCK), // task p12-axle-family
                new SubProviderEntry(GT6EngineBlockLoot::new, LootContextParamSets.BLOCK), // task p12-engine-diesel
                new SubProviderEntry(GT6KineticsBlockLoot::new, LootContextParamSets.BLOCK), // task p12-gearbox-transformer
                new SubProviderEntry(GT6BurningBoxBlockLoot::new, LootContextParamSets.BLOCK), // task p13-burning-box-family
                new SubProviderEntry(GT6BoilerTankBlockLoot::new, LootContextParamSets.BLOCK), // task p13-boiler-tank
                new SubProviderEntry(GT6DryerBlockLoot::new, LootContextParamSets.BLOCK), // task p14-dryer-family
                new SubProviderEntry(GT6DistilleryBlockLoot::new, LootContextParamSets.BLOCK), // task p16-distillery-family
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK))); // task p19-stoneblocks-render
        //?}
    }

    /** The block list this provider owns: exactly the material prefix block array (the census walk order). */
    public static List<Block> lootBlocks() {
        return List.of(GTMaterialBlocks.blockArray());
    }

    /**
     * The wire-family block list this second provider owns: the 620 GTWireSpecs variants
     * (datagen JVM) plus, since task p10-wire-contact-damage (the R1 review handoff), the
     * 6 redstone-family blocks, plus, since task p11-wire-laser-loot, the laser-family
     * blocks — they are the same wire carrier block (GTWireBlock) and breaking a placed
     * one must drop the item, the identical upstream Drops==null self-drop default
     * (PrefixBlock.java:227; the laser carrier is MultiTileEntityWireLaser, the same
     * gregapi Drops machinery, MultiTileEntityWireLaser.java:48 extends
     * TileEntityBase10ConnectorRendered). There is NO separate laser loot path: until this
     * card the single {@code wire_laser} block shipped table-less and broke into nothing.
     * The two material-less p7 legacy anchors keep shipping without a table (pre-existing
     * state, not this card's delta).
     */
    public static List<Block> wireLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tWire : GTWires.FAMILY_BLOCKS) rBlocks.add(tWire.get());
        for (var tWire : GTWires.REDSTONE_BLOCKS) rBlocks.add(tWire.get());
        for (var tWire : GTWires.LASER_BLOCKS) rBlocks.add(tWire.get());
        return rBlocks;
    }

    /**
     * The wire-family self-drop provider (task p9-wire-family-w1 ⑥): known-blocks narrowed
     * to {@link #wireLootBlocks()} so the missing-table validation covers exactly this card's
     * surface; the legacy p7 pair keeps shipping without a table (pre-existing state, not
     * this card's delta).
     */
    public static final class GT6WireBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6WireBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6WireBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return wireLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : wireLootBlocks()) dropSelf(tBlock); // the p8 self-drop direct translation
        }
    }

    /**
     * The axle-family block list (task p12-axle-family): the 44 material x diameter rows
     * (datagen JVM). The upstream axle registers {@code canDrop(0) == F} with the MTE
     * default drop = the block item itself (MultiTileEntityAxle.java:153, the popOff
     * {@code getDrops} path of TileEntityBase04:173-177 — the axle's overspeed break drops
     * its own item through {@code level.destroyBlock(pos, true)}); the 1.20.1 equivalent
     * is exactly {@code dropSelf} like the wire family.
     */
    public static List<Block> axleLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tAxle : GT6Kinetics.AXLE_BLOCKS.values()) rBlocks.add(tAxle.get());
        return rBlocks;
    }

    /** The axle-family self-drop provider (task p12-axle-family — the popOff drop path). */
    public static final class GT6AxleBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6AxleBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6AxleBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return axleLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : axleLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The kinetics-machine block list (task p12-gearbox-transformer): the gearbox + the
     * rotation transformer, the wood kinetic rows. The upstream MTEs register
     * {@code canDrop(0) == F} with the default drop = the block item itself
     * (MultiTileEntityGearBox.java:426 / TileEntityBase10EnergyConverter.java:161); the
     * 1.20.1 equivalent is exactly {@code dropSelf} like the axle/wire families.
     */
    public static List<Block> kineticsLootBlocks() {
        return List.of(GT6Kinetics.GEARBOX.get(), GT6Kinetics.TRANSFORMER_ROTATION.get());
    }

    /** The kinetics-machine self-drop provider (task p12-gearbox-transformer). */
    public static final class GT6KineticsBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6KineticsBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6KineticsBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return kineticsLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : kineticsLootBlocks()) dropSelf(tBlock);
        }
    }

    public static final class GT6BlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6BlockLoot() {
            // no explosion-resistant items: every drop survives-explosion-gated (vanilla default form)
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return lootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : lootBlocks()) dropSelf(tBlock); // Drops(this,this,this,this,F,F,0,0) direct translation
        }
    }

    /**
     * The diesel engine family block list (task p12-engine-diesel): the 8 material tiers
     * (datagen JVM). The upstream engine registers the MTE default drop (canDrop(0) == T,
     * MultiTileEntityMotorLiquid.java:224 — the block item itself, the same
     * Drops==null self-drop default as the axle/wire rows); the 1.20.1 equivalent is
     * exactly {@code dropSelf}.
     */
    public static List<Block> engineLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (Block tEngine : GT6Kinetics.dieselBlockArray()) rBlocks.add(tEngine);
        return rBlocks;
    }

    /** The diesel engine family self-drop provider (task p12-engine-diesel). */
    public static final class GT6EngineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6EngineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6EngineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return engineLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : engineLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The burning-box family block list (task p13-burning-box-family): all 97 rows
     * (Brick + Solid + Liquid + Gas + FluidBed). The upstream machines carry the MTE
     * default self-drop (canDrop(0) == T across the generator family); the 1.20.1
     * equivalent is exactly {@code dropSelf}.
     */
    public static List<Block> burningBoxLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.allRows()) {
            rBlocks.add(gregtech6.registry.GT6BurningBoxes.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The burning-box family self-drop provider (task p13-burning-box-family). */
    public static final class GT6BurningBoxBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6BurningBoxBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6BurningBoxBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return burningBoxLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : burningBoxLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The boiler-tank family block list (task p13-boiler-tank): all 26 rows. The upstream
     * machines carry the MTE default self-drop (canDrop :269 returns F for the TANK
     * inventory — the boiler has no item inventory at all; the block itself drops, the
     * vanilla {@code dropSelf} equivalent).
     */
    public static List<Block> boilerTankLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.registry.GT6Boilers.BoilerRow tRow : gregtech6.registry.GT6Boilers.allRows()) {
            rBlocks.add(gregtech6.registry.GT6Boilers.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The boiler-tank family self-drop provider (task p13-boiler-tank). */
    public static final class GT6BoilerTankBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6BoilerTankBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6BoilerTankBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return boilerTankLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : boilerTankLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The dryer-family block list (task p14-dryer-family): the four Dryer rows
     * (Loader_MultiTileEntities.java:1477-1480). The upstream machines carry the MTE
     * default self-drop (canDrop(0) == T — the block item itself, the same Drops==null
     * default as the boiler/burning-box families); the 1.20.1 equivalent is exactly
     * {@code dropSelf}.
     */
    public static List<Block> dryerLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.DRYER_ROWS) {
            rBlocks.add(gregtech6.registry.GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The dryer-family self-drop provider (task p14-dryer-family). */
    public static final class GT6DryerBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6DryerBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6DryerBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return dryerLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : dryerLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The distillery-family block list (task p16-distillery-family): the four Distillery
     * rows (Loader_MultiTileEntities.java:1398-1401) — the dryerLootBlocks shape verbatim,
     * the MTE default self-drop (the same Drops==null default).
     */
    public static List<Block> distilleryLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.DISTILLERY_ROWS) {
            rBlocks.add(gregtech6.registry.GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The distillery-family self-drop provider (task p16-distillery-family). */
    public static final class GT6DistilleryBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6DistilleryBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6DistilleryBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return distilleryLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : distilleryLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The stone-family block list (task p21-stoneblocks-16item-registry-split): the 272
     * per-pair GTStoneBlock registrations ({@link gregtech6.registry.GTStoneBlocks#blockArray()},
     * stone-major in CS.java:1668 order, variant-major in meta order). Upstream drops come
     * from the BlockStones.getDrops override (BlockStones.java:731): {@code ST.make(this, 1,
     * aMeta == STONE ? COBBL : aMeta)} — the classic stone-yields-cobble rule for variant 0,
     * self for the other 15 variants. The P19 pool item ("unrecoverable without splitting 16
     * items per stone", GT6LootTables.java:439-456 as it stood) closes HERE: with one
     * BlockItem per (stone, variant), the variant-0 block's table drops the SAME STONE's
     * COBBL variant item (a single pool, {@code dropOther}) and the other 271 blocks
     * {@code dropSelf} — the :731 line direct-translated, no collapse left. Each table lives
     * at the vanilla default per-BLOCK location {@code gt6:blocks/<path>} (zero block code).
     */
    public static List<Block> stoneLootBlocks() {
        return gregtech6.registry.GTStoneBlocks.blockArray();
    }

    /** The stone-family provider (task p19-stoneblocks-render, loot semantics completed by task p21). */
    public static final class GT6StoneBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6StoneBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6StoneBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return stoneLootBlocks(); // narrowed to exactly the 272 blocks this provider owns
        }

        @Override
        protected void generate() {
            for (Block tBlock : stoneLootBlocks()) {
                gregtech6.block.stone.GTStoneBlock tStone = (gregtech6.block.stone.GTStoneBlock)tBlock;
                if (tStone.variant == gregtech6.block.stone.StoneVariant.STONE) {
                    // BlockStones.java:731 verbatim — variant 0 yields the SAME STONE's COBBL
                    // variant item (its own registry id is gt6:<snake>, the cobble item's is
                    // gt6:<snake>_cobble); dropOther = one unconditional single-item pool.
                    dropOther(tBlock, gregtech6.registry.GTStoneBlocks
                            .item(tStone.stoneSnake, gregtech6.block.stone.StoneVariant.COBBL).get());
                } else {
                    dropSelf(tBlock); // the :731 self arm, one table per variant block
                }
            }
        }
    }
}
