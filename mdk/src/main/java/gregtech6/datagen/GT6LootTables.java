package gregtech6.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import net.minecraftforge.registries.RegistryObject;

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
        super(output, Set.of(), List.of(
                new SubProviderEntry(GT6BlockLoot::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(GT6WireBlockLoot::new, LootContextParamSets.BLOCK), // task p9-wire-family-w1 ⑥
                new SubProviderEntry(GT6AxleBlockLoot::new, LootContextParamSets.BLOCK), // task p12-axle-family
                new SubProviderEntry(GT6EngineBlockLoot::new, LootContextParamSets.BLOCK), // task p12-engine-diesel
                new SubProviderEntry(GT6KineticsBlockLoot::new, LootContextParamSets.BLOCK), // task p12-gearbox-transformer
                new SubProviderEntry(GT6BurningBoxBlockLoot::new, LootContextParamSets.BLOCK))); // task p13-burning-box-family
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
        for (RegistryObject<GTWireBlock> tWire : GTWires.FAMILY_BLOCKS) rBlocks.add(tWire.get());
        for (RegistryObject<GTWireBlock> tWire : GTWires.REDSTONE_BLOCKS) rBlocks.add(tWire.get());
        for (RegistryObject<GTWireBlock> tWire : GTWires.LASER_BLOCKS) rBlocks.add(tWire.get());
        return rBlocks;
    }

    /**
     * The wire-family self-drop provider (task p9-wire-family-w1 ⑥): known-blocks narrowed
     * to {@link #wireLootBlocks()} so the missing-table validation covers exactly this card's
     * surface; the legacy p7 pair keeps shipping without a table (pre-existing state, not
     * this card's delta).
     */
    public static final class GT6WireBlockLoot extends BlockLootSubProvider {

        public GT6WireBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

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
        for (RegistryObject<GTAxleBlock> tAxle : GT6Kinetics.AXLE_BLOCKS.values()) rBlocks.add(tAxle.get());
        return rBlocks;
    }

    /** The axle-family self-drop provider (task p12-axle-family — the popOff drop path). */
    public static final class GT6AxleBlockLoot extends BlockLootSubProvider {

        public GT6AxleBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

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

        public GT6KineticsBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

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

        public GT6EngineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

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

        public GT6BurningBoxBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return burningBoxLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : burningBoxLootBlocks()) dropSelf(tBlock);
        }
    }
}
