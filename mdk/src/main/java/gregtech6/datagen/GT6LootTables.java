package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;

//? if forge {
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
//?}

import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

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
                new SubProviderEntry(GT6CannerBlockLoot::new, LootContextParamSets.BLOCK), // task p24-canner-machine
                new SubProviderEntry(GT6LightningRodBlockLoot::new, LootContextParamSets.BLOCK), // task p24-lightning-rod
                new SubProviderEntry(GT6MachineBlockLoot::new, LootContextParamSets.BLOCK), // task p22-painted-item-domain
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK), // task p19-stoneblocks-render
                new SubProviderEntry(GT6GrassBlockLoot::new, LootContextParamSets.BLOCK)), // task p24-grass-block
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
                new SubProviderEntry(GT6CannerBlockLoot::new, LootContextParamSets.BLOCK), // task p24-canner-machine
                new SubProviderEntry(GT6LightningRodBlockLoot::new, LootContextParamSets.BLOCK), // task p24-lightning-rod
                new SubProviderEntry(GT6MachineBlockLoot::new, LootContextParamSets.BLOCK), // task p22-painted-item-domain
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK), // task p19-stoneblocks-render
                new SubProviderEntry(GT6GrassBlockLoot::new, LootContextParamSets.BLOCK))); // task p24-grass-block
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

    /** The dryer-family self-drop provider (task p14-dryer-family; the paint carry = task p22-painted-item-domain). */
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
            for (Block tBlock : dryerLootBlocks()) add(tBlock, paintSelfTable(tBlock));
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

    /** The distillery-family self-drop provider (task p16-distillery-family; the paint carry = task p22-painted-item-domain). */
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
            for (Block tBlock : distilleryLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The canner-family block list (task p24-canner-machine): the four Canner rows
     * (Loader_MultiTileEntities.java:1379-1382) — the distilleryLootBlocks shape verbatim,
     * the MTE default self-drop (the same Drops==null default).
     */
    public static List<Block> cannerLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.CANNER_ROWS) {
            rBlocks.add(gregtech6.registry.GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The canner-family self-drop provider (task p24-canner-machine; the paint carry = task p22-painted-item-domain). */
    public static final class GT6CannerBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6CannerBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6CannerBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return cannerLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : cannerLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The Lightning Rod family block list (task p24-lightning-rod): the controller plus the
     * three part blocks (Loader :1282/:1151/:1168/:1179) — the canner shape verbatim. The
     * upstream part MTEs and the controller all self-drop (the MTE default); NOTE this is
     * the FIRST loot-tabled multiblock family — the older coke-oven/boiler/wall blocks
     * still ship table-less (their cards' pre-existing gap, not this card's delta).
     */
    public static List<Block> lightningRodLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        rBlocks.add(gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD.get());
        for (var tRow : gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_ROWS) {
            rBlocks.add(gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The Lightning Rod family self-drop provider (task p24-lightning-rod; plain dropSelf, no paint on the part BEs). */
    public static final class GT6LightningRodBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6LightningRodBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6LightningRodBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return lightningRodLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : lightningRodLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The paint-carrying self-drop table — the machine-domain drop form (task
     * p22-painted-item-domain): the vanilla {@code createSingleItemTable} shape verbatim
     * (BlockLootSubProvider.java:108-111 — one pool, rolls 1, the {@code survives_explosion}
     * condition, one item entry) plus ONE entry-level function carrying the placed-paint
     * round trip: {@code copy_nbt} from the block entity into the dropped stack's
     * {@code BlockEntityTag} compound, two REPLACE ops, exactly the paint keys
     * {@code gt.color}/{@code gt.painted} (the CS.java:1161-1162 pair the 03 base writes
     * while painted, TileEntityBase03TicksAndSync.saveAdditional). The op set is keyed
     * off the BE's own constants (single decision site — the BE save and the loot copy can
     * never drift). NOT the full BE NBT: upstream's getDrops writeItemNBT carries more
     * (TileEntityBase04MultiTileEntities), but the paint domain is this card's only scope.
     *
     * <p>Placement read-back needs ZERO BlockItem code: vanilla
     * {@code BlockItem.updateCustomBlockEntityTag(Level, Player, BlockPos, ItemStack)}
     * (BlockItem.java:158-184) merges {@code BlockEntityTag} into the fresh BE, whose
     * {@code load} rehydrates the two paint keys (TileEntityBase03TicksAndSync.load) — the
     * GTBarrelBlockItem.placeBlock override shape becomes unnecessary here (the machine
     * items are plain GTComposedNameItem extends BlockItem, no override).
     *
     * <p>Dual leg: 1.20.1 serializes the function as {@code minecraft:copy_nbt}; 1.20.5+
     * renamed it to {@code CopyCustomDataFunction} ({@code minecraft:copy_custom_data},
     * javap neoforge-21.1.249) — the node-forked {@link #paintCopyNbt()} keeps the same
     * builder shape over the same ContextNbtProvider.BLOCK_ENTITY source. The canonical
     * tracked tree is the 1.20.1-forge runData output (ADR-P17-1), the 1.21.1 leg's
     * node-local output is validation-only, and 21.1 loot-runtime fidelity rides the
     * declared loot deviation (build.neoforge.gradle.kts:218-221).
     */
    static LootTable.Builder paintSelfTable(ItemLike aItem) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(aItem).apply(paintCopyNbt())));
    }

    /**
     * The paint carry function builder — the {@code copy_nbt} two-op form: REPLACE
     * {@code gt.color} and {@code gt.painted} from the block-entity NBT into the item's
     * {@code BlockEntityTag} compound (the vanilla chest/shulker placement convention —
     * {@code BlockItem.getBlockEntityData} reads that key). The generated JSON shape:
     * {@code {"function": "minecraft:copy_nbt", "source": "block_entity", "ops":
     * [{"source": "gt.color", "target": "BlockEntityTag.gt.color", "op": "replace"}, ...]}}.
     */
    private static LootItemFunction.Builder paintCopyNbt() {
        //? if neoforge {
        /*return net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction
                .copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(TileEntityBase03TicksAndSync.NBT_COLOR, "BlockEntityTag." + TileEntityBase03TicksAndSync.NBT_COLOR,
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(TileEntityBase03TicksAndSync.NBT_PAINTED, "BlockEntityTag." + TileEntityBase03TicksAndSync.NBT_PAINTED,
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE);
         *///?} else {
        return CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(TileEntityBase03TicksAndSync.NBT_COLOR, "BlockEntityTag." + TileEntityBase03TicksAndSync.NBT_COLOR,
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(TileEntityBase03TicksAndSync.NBT_PAINTED, "BlockEntityTag." + TileEntityBase03TicksAndSync.NBT_PAINTED,
                        CopyNbtFunction.MergeStrategy.REPLACE);
        //?}
    }

    /**
     * The machine-family block list (task p22-painted-item-domain): the 13 machine-domain
     * blocks whose loot this NEW provider owns — the oven (1) + the shredder/crusher/lathe
     * ladders (4 each = 12), the {@code GTMachines.paintableBlockArray()} census rows the
     * dryer/distillery providers do NOT cover. Pre-existing state: these 13 shipped
     * table-less (breaking dropped nothing — the default loot path {@code gt6:blocks/<path>}
     * resolved to nothing); this card gives them the upstream MTE default self-drop
     * (canDrop(0) == T) WITH the paint carry, completing the 21-table paint census with the
     * dryer (4) + distillery (4) tables converted in place.
     */
    public static List<Block> machineLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        rBlocks.add(gregtech6.registry.GTMachines.OVEN.get());
        for (RegistryObject<Block> tBlock : java.util.List.of(
                gregtech6.registry.GTMachines.SHREDDER, gregtech6.registry.GTMachines.SHREDDER_T2,
                gregtech6.registry.GTMachines.SHREDDER_T3, gregtech6.registry.GTMachines.SHREDDER_T4,
                gregtech6.registry.GTMachines.CRUSHER, gregtech6.registry.GTMachines.CRUSHER_T2,
                gregtech6.registry.GTMachines.CRUSHER_T3, gregtech6.registry.GTMachines.CRUSHER_T4,
                gregtech6.registry.GTMachines.LATHE, gregtech6.registry.GTMachines.LATHE_T2,
                gregtech6.registry.GTMachines.LATHE_T3, gregtech6.registry.GTMachines.LATHE_T4)) {
            rBlocks.add(tBlock.get());
        }
        return rBlocks;
    }

    /**
     * The machine-family provider (task p22-painted-item-domain): every block in
     * {@link #machineLootBlocks()} drops its own item carrying the paint round-trip
     * function ({@link #paintSelfTable}).
     */
    public static final class GT6MachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6MachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6MachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return machineLootBlocks(); // narrowed to exactly the 13 blocks this provider owns
        }

        @Override
        protected void generate() {
            for (Block tBlock : machineLootBlocks()) add(tBlock, paintSelfTable(tBlock));
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
     * {@code dropSelf} — the :731 line direct-translated, no collapse left. Task
     * p21-chisel-drop-conversion adds the mining-tool face on top: the ten non-identity
     * chisel mappings rewrite their table into the {@link GT6StoneBlockLoot#chiselDispatchTable}
     * dispatch (the variant-0 baseline item stays the same stone's COBBL item), the six
     * identity mappings stay pass-through on this form. Each table lives
     * at the vanilla default per-BLOCK location {@code gt6:blocks/<path>} (zero block code).
     */
    public static List<Block> stoneLootBlocks() {
        return gregtech6.registry.GTStoneBlocks.blockArray();
    }

    /**
     * The stone-family provider (task p19-stoneblocks-render; the chisel mining-drop face =
     * task p21-chisel-drop-conversion). Per block, one of two forms:
     *
     * <ul>
     * <li>identity chisel mapping ({@link #chiselTarget} null, variants 1,2,6,8,9,11 — the
     *     six pass-through stones x17 = 102 tables): the landed :731 baseline form unchanged
     *     — variant 0 {@code dropOther} the SAME STONE's COBBL item (never identity, kept for
     *     form completeness in {@link #baselineItem}), the rest {@code dropSelf};</li>
     * <li>non-identity mapping (variants 0,3,4,5,7,10,12,13,14,15 — the ten dispatch stones
     *     x17 = 170 tables): the GT_Tool_Chisel.java:73-77 arm as a dispatch pool —
     *     {@code match_tool(gt6:chisel)} wins with the CHISEL_MAPPINGS variant item,
     *     everything else falls to the :731 baseline item.</li>
     * </ul>
     */
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

        /**
         * The chisel mapping arm of one variant (the GT_Tool_Chisel.java:73-77 BlockStones
         * face): the CHISEL_MAPPINGS target variant (BlockStones.java:81, byte-table indexed
         * by the meta declaration order = {@link StoneVariant} ordinal), or {@code null} when
         * the mapping is identity — the chisel drop IS the loot baseline, nothing to convert.
         * Static and registry-free so the offline census test and the live mine command share
         * this single decision site.
         */
        public static gregtech6.block.stone.StoneVariant chiselTarget(gregtech6.block.stone.StoneVariant aVariant) {
            byte tMapped = gregtech6.registry.GTStoneBlocks.CHISEL_MAPPINGS[aVariant.meta() & 15];
            return tMapped == aVariant.meta() ? null : gregtech6.block.stone.StoneVariant.VALUES[tMapped];
        }

        /**
         * The :731 loot baseline of one block ({@code ST.make(this, 1, aMeta == STONE ?
         * COBBL : aMeta)}, BlockStones.java:731): variant 0 yields the SAME STONE's COBBL
         * variant item (its registry id is gt6:&lt;snake&gt;, the cobble item's is
         * gt6:&lt;snake&gt;_cobble), every other variant its own item.
         */
        public static Item baselineItem(Block aBlock, gregtech6.block.stone.GTStoneBlock aStone) {
            return aStone.variant == gregtech6.block.stone.StoneVariant.STONE
                    ? gregtech6.registry.GTStoneBlocks.item(aStone.stoneSnake, gregtech6.block.stone.StoneVariant.COBBL).get()
                    : aBlock.asItem();
        }

        @Override
        protected void generate() {
            for (Block tBlock : stoneLootBlocks()) {
                gregtech6.block.stone.GTStoneBlock tStone = (gregtech6.block.stone.GTStoneBlock)tBlock;
                gregtech6.block.stone.StoneVariant tTarget = chiselTarget(tStone.variant);
                if (tTarget == null) {
                    // identity mapping — the chisel arm changes nothing, the landed baseline
                    // form stays (variant 0 never maps to itself, so this arm is always self)
                    dropSelf(tBlock);
                } else {
                    add(tBlock, chiselDispatchTable(baselineItem(tBlock, tStone),
                            gregtech6.registry.GTStoneBlocks.item(tStone.stoneSnake, tTarget).get()));
                }
            }
        }

        /**
         * The chisel dispatch pool — the vanilla BlockLootSubProvider tool-dispatch shape
         * (createSelfDropDispatchTable, BlockLootSubProvider.java:113-116 of the vendored
         * 1.20.1 tree: one entry armed with a tool condition, {@code otherwise} the else arm;
         * {@code LootPoolEntryContainer.Builder.otherwise} builds an AlternativesEntry on
         * BOTH legs, javap-verified 1.21.1): {@code match_tool} on the gt6:chisel item id
         * (GT6Tools.java:88 — the single-steel-tier chisel; the upstream multi-material
         * chisel family is a declared-deviation pool) wins with the mapped variant item;
         * bare hands, other tools and the no-TOOL contexts (explosions — the upstream
         * no-HarvestDropsEvent semantics) fall to the :731 baseline item. Pool-level
         * survives_explosion + rolls=1 kept identical to createSingleItemTable, so the else
         * arm is the pass-through form item-for-item. The upstream conversion economics ride
         * along structurally: a loot-table arm pays zero extra durability (the :73-77 arm
         * returns 0 at MultiItemTool.java:212) and ignores silk/fortune (:75 — the GT stone
         * tables carry no silk/fortune arm either). Reachability: GTStoneBlock never calls
         * requiresCorrectToolForDrops (GTStoneBlock.java:56-59), so any breaking tool sees
         * drops and the match_tool predicate alone decides the arm.
         */
        private LootTable.Builder chiselDispatchTable(Item aBaseline, Item aChiselMapped) {
            return LootTable.lootTable()
                    .withPool(this.applyExplosionCondition(aBaseline, LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(aChiselMapped)
                                    .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(GT6Tools.CHISEL.get())))
                                    .otherwise(LootItem.lootTableItem(aBaseline)))));
        }
    }

    /**
     * The grass-family block list (task p24-grass-block): the 6 GT grass variants, the
     * registration walk order.
     */
    public static List<Block> grassLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tHandle : gregtech6.registry.GTGrassBlocks.BLOCKS) rBlocks.add(tHandle.get());
        return rBlocks;
    }

    /**
     * The grass-family loot provider (task p24-grass-block) — the upstream
     * {@code BlockGrass.getDrops} shape (BlockGrass.java:105 returns vanilla dirt x1 with
     * the fortune parameter IGNORED) plus the silk-touch self-drop (upstream
     * canSilkHarvest = T, BlockBase.java:109, {@code createStackedBlock} = self with its
     * meta, :82-84). One table per variant at the vanilla default
     * {@code gt6:blocks/<registry-path>} location (zero block code), each the vanilla
     * grass_block.json alternatives face: {@link BlockLootSubProvider
     * #createSilkTouchDispatchTable(Block, LootPoolEntryContainer.Builder)} = self under
     * {@code match_tool silk_touch}, {@code otherwise} DIRT under {@code
     * survives_explosion} ({@code applyExplosionCondition}, the vanilla
     * {@code createSingleItemTableWithSilkTouch} :130-132 composition). No fortune arm —
     * the dirt is fortune-immune by construction (the upstream :105 ignore); the GT spade
     * convertBlockDrops self-drop arm stays in the tool-system pool (GT_Tool_Spade.java
     * :81-89); {@code requires_correct_tool_for_drops} is NOT set (the card red line).
     */
    public static final class GT6GrassBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6GrassBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6GrassBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return grassLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : grassLootBlocks()) {
                add(tBlock, createSilkTouchDispatchTable(tBlock,
                        this.applyExplosionCondition(tBlock, LootItem.lootTableItem(net.minecraft.world.item.Items.DIRT))));
            }
        }
    }
}
