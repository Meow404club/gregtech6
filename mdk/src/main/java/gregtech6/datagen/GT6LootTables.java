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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GTMaterialItems;

//? if forge {
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
//?}

import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.connectors.GTFluidPipeBlockEntity;

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
                new SubProviderEntry(GT6KineticMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-sifter-compressor-wiremill
                new SubProviderEntry(GT6ProcessMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w1-kinetic-process-ladder
                new SubProviderEntry(GT6PressBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-press-extruder-molds
                new SubProviderEntry(GT6ExtruderBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-press-extruder-molds
                new SubProviderEntry(GT6EuHuFamiliesBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w1-eu-hu-families — the seven eu-hu families
                new SubProviderEntry(GT6EuSpecialMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-eu-special — the Autocrafter/Lightning/Laminator families
                new SubProviderEntry(GT6ExoticFamiliesBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-exotic-energy — the six exotic-energy families
                new SubProviderEntry(GT6EuCoreMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-eu-core-5tier — the five eu-core families
                new SubProviderEntry(GT6HuTuPiggybackBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-hu-tu-piggyback — the seven hu-tu families
                new SubProviderEntry(GT6LightningRodBlockLoot::new, LootContextParamSets.BLOCK), // task p24-lightning-rod
                new SubProviderEntry(GT6HeatExchangerBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-heat-smelter — the Large Heat Exchanger controller
                new SubProviderEntry(GT6PartBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-nbtdesign-parts — the part-family expansion
                new SubProviderEntry(GT6TankBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-tank-valves — the 25 valve self-drops
                new SubProviderEntry(GT6TurbineDynamoBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-turbine-dynamo — the twelve turbine/dynamo controllers
                new SubProviderEntry(GT6DistillCrucibleLoot::new, LootContextParamSets.BLOCK), // task p29-w3-distill-crucible — the towers + the crucible ladder
                new SubProviderEntry(GT6LargeMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-large-12 — the twelve large machines
                new SubProviderEntry(GT6ImplosionBlockLoot::new, LootContextParamSets.BLOCK), // task p31-implosion — the Implosion Compressor controller
                new SubProviderEntry(GT6GraaggBlockLoot::new, LootContextParamSets.BLOCK), // task p31-graagg — the Von da Graagg controller
                new SubProviderEntry(GT6MassfabBlockLoot::new, LootContextParamSets.BLOCK), // task p31-massfab — the Large Matter Fabricator controller
                new SubProviderEntry(GT6FusionBlockLoot::new, LootContextParamSets.BLOCK), // task p31-fusion — the Fusion Reactor controller
                new SubProviderEntry(GT6LogisticsCoreBlockLoot::new, LootContextParamSets.BLOCK), // task p32-logistics-lv3 — the Logistics Core controller
                new SubProviderEntry(GT6AdvancedCraftingTableBlockLoot::new, LootContextParamSets.BLOCK), // task p24-act-machine
                new SubProviderEntry(GT6MachineBlockLoot::new, LootContextParamSets.BLOCK), // task p22-painted-item-domain
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK), // task p19-stoneblocks-render
                new SubProviderEntry(GT6GrassBlockLoot::new, LootContextParamSets.BLOCK), // task p24-grass-block
                new SubProviderEntry(GT6PipeBlockLoot::new, LootContextParamSets.BLOCK), // task p25-c-foam-pipe-spray
                new SubProviderEntry(GT6CFoamBlockLoot::new, LootContextParamSets.BLOCK), // task p26-c-foam-block-family
                new SubProviderEntry(GT6SensorBlockLoot::new, LootContextParamSets.BLOCK), // task p26-sensors-core
                new SubProviderEntry(GT6StaticStorageBlockLoot::new, LootContextParamSets.BLOCK), // task p26-storage-static-batch — the 28 self-drops
                new SubProviderEntry(GT6FeConverterBlockLoot::new, LootContextParamSets.BLOCK), // task p28-b-fe-converter-machine
                new SubProviderEntry(GT6AnvilBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-anvil — the stone anvil pair
                new SubProviderEntry(GT6EuBridgeBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w4-eu-bridge — the three EU-bridge families + the Roasting ladder
                new SubProviderEntry(GT6ElectricTransformerBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-ulv-lv-transformer
                new SubProviderEntry(GT6DynamoUlvBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-ulv-dynamo-row — the T0 self-drop
                new SubProviderEntry(GT6TreeBlockLoot::new, LootContextParamSets.BLOCK), // task p30-w6-t1-trees-nine — the 27 tree blocks
                new SubProviderEntry(GT6SurfaceBlockLoot::new, LootContextParamSets.BLOCK), // task p30-w6-rocks-sticks — the surface deco band
                new SubProviderEntry(GT6PlaceableBlockLoot::new, LootContextParamSets.BLOCK), // task p32-placeables — the lantern + sandwich self-drops
                new SubProviderEntry(GT6BumbliaryBlockLoot::new, LootContextParamSets.BLOCK), // task p33-bees-lv3-b-bumbliary — the Bumbliary pair self-drops
                new SubProviderEntry(GT6OreLootTables.GT6OreBlockLoot::new, LootContextParamSets.BLOCK), // task p30-ore-4-loot — the 3922 ore tables
                new SubProviderEntry(GT6WeightTableLoot::new, LootContextParamSets.CHEST)), // task p34-loot-injection — the gt.flawless/gems/misc bag tables
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
                new SubProviderEntry(GT6KineticMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-sifter-compressor-wiremill
                new SubProviderEntry(GT6ProcessMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w1-kinetic-process-ladder
                new SubProviderEntry(GT6PressBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-press-extruder-molds
                new SubProviderEntry(GT6ExtruderBlockLoot::new, LootContextParamSets.BLOCK), // task p26-w1-press-extruder-molds
                new SubProviderEntry(GT6EuHuFamiliesBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w1-eu-hu-families — the seven eu-hu families
                new SubProviderEntry(GT6EuSpecialMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-eu-special — the Autocrafter/Lightning/Laminator families
                new SubProviderEntry(GT6ExoticFamiliesBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-exotic-energy — the six exotic-energy families
                new SubProviderEntry(GT6EuCoreMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-eu-core-5tier — the five eu-core families
                new SubProviderEntry(GT6HuTuPiggybackBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w2-hu-tu-piggyback — the seven hu-tu families
                new SubProviderEntry(GT6LightningRodBlockLoot::new, LootContextParamSets.BLOCK), // task p24-lightning-rod
                new SubProviderEntry(GT6HeatExchangerBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-heat-smelter — the Large Heat Exchanger controller
                new SubProviderEntry(GT6PartBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-nbtdesign-parts — the part-family expansion
                new SubProviderEntry(GT6TankBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-tank-valves — the 25 valve self-drops
                new SubProviderEntry(GT6TurbineDynamoBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-turbine-dynamo — the twelve turbine/dynamo controllers
                new SubProviderEntry(GT6DistillCrucibleLoot::new, LootContextParamSets.BLOCK), // task p29-w3-distill-crucible — the towers + the crucible ladder
                new SubProviderEntry(GT6LargeMachineBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w3-large-12 — the twelve large machines
                new SubProviderEntry(GT6ImplosionBlockLoot::new, LootContextParamSets.BLOCK), // task p31-implosion — the Implosion Compressor controller
                new SubProviderEntry(GT6GraaggBlockLoot::new, LootContextParamSets.BLOCK), // task p31-graagg — the Von da Graagg controller
                new SubProviderEntry(GT6MassfabBlockLoot::new, LootContextParamSets.BLOCK), // task p31-massfab — the Large Matter Fabricator controller
                new SubProviderEntry(GT6FusionBlockLoot::new, LootContextParamSets.BLOCK), // task p31-fusion — the Fusion Reactor controller
                new SubProviderEntry(GT6LogisticsCoreBlockLoot::new, LootContextParamSets.BLOCK), // task p32-logistics-lv3 — the Logistics Core controller
                new SubProviderEntry(GT6AdvancedCraftingTableBlockLoot::new, LootContextParamSets.BLOCK), // task p24-act-machine
                new SubProviderEntry(GT6MachineBlockLoot::new, LootContextParamSets.BLOCK), // task p22-painted-item-domain
                new SubProviderEntry(GT6StoneBlockLoot::new, LootContextParamSets.BLOCK), // task p19-stoneblocks-render
                new SubProviderEntry(GT6GrassBlockLoot::new, LootContextParamSets.BLOCK), // task p24-grass-block
                new SubProviderEntry(GT6PipeBlockLoot::new, LootContextParamSets.BLOCK), // task p25-c-foam-pipe-spray
                new SubProviderEntry(GT6CFoamBlockLoot::new, LootContextParamSets.BLOCK), // task p26-c-foam-block-family
                new SubProviderEntry(GT6SensorBlockLoot::new, LootContextParamSets.BLOCK), // task p26-sensors-core
                new SubProviderEntry(GT6StaticStorageBlockLoot::new, LootContextParamSets.BLOCK), // task p26-storage-static-batch — the 28 self-drops
                new SubProviderEntry(GT6FeConverterBlockLoot::new, LootContextParamSets.BLOCK), // task p28-b-fe-converter-machine
                new SubProviderEntry(GT6AnvilBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-anvil — the stone anvil pair
                new SubProviderEntry(GT6EuBridgeBlockLoot::new, LootContextParamSets.BLOCK), // task p29-w4-eu-bridge — the three EU-bridge families + the Roasting ladder
                new SubProviderEntry(GT6ElectricTransformerBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-ulv-lv-transformer
                new SubProviderEntry(GT6DynamoUlvBlockLoot::new, LootContextParamSets.BLOCK), // task p28-c-ulv-dynamo-row — the T0 self-drop
                new SubProviderEntry(GT6TreeBlockLoot::new, LootContextParamSets.BLOCK), // task p30-w6-t1-trees-nine — the 27 tree blocks
                new SubProviderEntry(GT6SurfaceBlockLoot::new, LootContextParamSets.BLOCK), // task p30-w6-rocks-sticks — the surface deco band
                new SubProviderEntry(GT6PlaceableBlockLoot::new, LootContextParamSets.BLOCK), // task p32-placeables — the lantern + sandwich self-drops
                new SubProviderEntry(GT6BumbliaryBlockLoot::new, LootContextParamSets.BLOCK), // task p33-bees-lv3-b-bumbliary — the Bumbliary pair self-drops
                new SubProviderEntry(GT6OreLootTables.GT6OreBlockLoot::new, LootContextParamSets.BLOCK), // task p30-ore-4-loot — the 3922 ore tables
                new SubProviderEntry(GT6WeightTableLoot::new, LootContextParamSets.CHEST))); // task p34-loot-injection — the gt.flawless/gems/misc bag tables
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
     * The tree-family loot (task p30-w6-t1-trees-nine): logs and saplings self-drop
     * (dropSelf, the wire-family face); leaves carry the upstream chance table —
     * sapling 1-in-50 (coconut 2-in-50) with the fortune scaling arithmetic verbatim
     * (tChance = max(5, 50 - 5&lt;&lt;fortune), BlockTreeLeavesAB.java:110-114), the
     * shears/silk face is the vanilla dispatch (the IShearable equivalent), and the
     * stick alt-drop rides the SAME fortune table at numerator 2 for the three
     * upstream stick kinds (willow/blue_mahoe/hazel, :119-121) over their registered
     * gt6:stick_&lt;snake&gt; material rods. DECLARED CUT: the hazelnut/coconut fruit
     * drops (:124-125) — the port registers no fruit items yet, the food-item domain
     * card grows the pool.
     */
    public static final class GT6TreeBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6TreeBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6TreeBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return treeLootBlocks();
        }

        /** The 27 tree blocks, registration order (saplings, logs, leaves). */
        public static List<Block> treeLootBlocks() {
            List<Block> rBlocks = new ArrayList<>();
            for (RegistryObject<Block> tHandle : GT6TreeBlocks.SAPLINGS) rBlocks.add(tHandle.get());
            for (RegistryObject<Block> tHandle : GT6TreeBlocks.LOGS) rBlocks.add(tHandle.get());
            for (RegistryObject<Block> tHandle : GT6TreeBlocks.LEAVES) rBlocks.add(tHandle.get());
            return rBlocks;
        }

        /** The upstream fortune table: chance = numerator / max(5, 50 - (5 &lt;&lt; fortune)). */
        private static float[] chanceTable(int aNumerator) {
            float[] rChances = new float[5];
            for (int f = 0; f < 5; f++) {
                rChances[f] = (float) aNumerator / Math.max(5, 50 - (5 << f));
            }
            return rChances;
        }

        @Override
        protected void generate() {
            for (RegistryObject<Block> tLog : GT6TreeBlocks.LOGS) dropSelf(tLog.get());
            for (RegistryObject<Block> tSapling : GT6TreeBlocks.SAPLINGS) dropSelf(tSapling.get());
            for (int i = 0; i < GT6TreeBlocks.KINDS.size(); i++) {
                gregtech6.block.tree.GT6TreeKind tKind = GT6TreeBlocks.KINDS.get(i);
                Block tLeaves = GT6TreeBlocks.LEAVES.get(i).get();
                Block tSapling = GT6TreeBlocks.SAPLINGS.get(i).get();
                //? if forge {
                LootTable.Builder tBuilder = createSilkTouchOrShearsDispatchTable(tLeaves,
                        ((LootPoolSingletonContainer.Builder<?>) applyExplosionCondition(tLeaves,
                                LootItem.lootTableItem(tSapling)))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE,
                                        chanceTable(tKind == GT6TreeKind.COCONUT ? 2 : 1))));
                //?} else {
                /*LootTable.Builder tBuilder = createSilkTouchOrShearsDispatchTable(tLeaves,
                        ((LootPoolSingletonContainer.Builder<?>) applyExplosionCondition(tLeaves,
                                LootItem.lootTableItem(tSapling)))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                        this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                        chanceTable(tKind == GT6TreeKind.COCONUT ? 2 : 1))));
                 *///?}
                if (tKind == GT6TreeKind.WILLOW || tKind == GT6TreeKind.BLUE_MAHOE || tKind == GT6TreeKind.HAZEL) {
                    //? if forge {
                    Item tStick = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            new ResourceLocation(GT6DataGenerators.MOD_ID, "stick_" + tKind.snake()));
                    //?} else {
                    /*Item tStick = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            ResourceLocation.fromNamespaceAndPath(GT6DataGenerators.MOD_ID, "stick_" + tKind.snake()));
                     *///?}
                    if (tStick != null && tStick != Items.AIR) {
                        // the stick pool with the same fortune table — the fortune carrier
                        // forks (1.20.1 plain Enchantment vs 1.21.1 Holder via the lookup)
                        //? if forge {
                        tBuilder.withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(HAS_SHEARS.or(HAS_SILK_TOUCH).invert())
                                .add(((LootPoolSingletonContainer.Builder<?>) applyExplosionDecay(tLeaves,
                                        LootItem.lootTableItem(tStick)))
                                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                                Enchantments.BLOCK_FORTUNE, chanceTable(2)))));
                        //?} else {
                        /*tBuilder.withPool(LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(HAS_SHEARS.or(this.hasSilkTouch()).invert())
                                .add(((LootPoolSingletonContainer.Builder<?>) applyExplosionDecay(tLeaves,
                                        LootItem.lootTableItem(tStick)))
                                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                                this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), chanceTable(2)))));
                         *///?}
                    }
                }
                add(tLeaves, tBuilder);
            }
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
        // the water wheel rides the kinetics family (task p28-c-water-wheel): the upstream
        // wheel registers the MTE default self-drop (canDrop(0) == T, the axle/engine
        // Drops==null semantics); the 1.20.1 equivalent is the same dropSelf
        return List.of(GT6Kinetics.GEARBOX.get(), GT6Kinetics.TRANSFORMER_ROTATION.get(), GT6Kinetics.WATER_WHEEL.get());
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
        // task p28-c-ulv-machine-ladder — the ULV Canner rung joins the family table
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.CANNER_ULV_ROWS) {
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
     * The eu-hu families block list (task p29-w1-eu-hu-families): the 25 blocks of the
     * seven families (Mixer + ElectricMixer + ElectricLoom + ElectricSifter + Boxinator +
     * Unboxinator 4 tiers each, plus the single-variant Fermenter) — the cannerLootBlocks
     * shape verbatim, the MTE default self-drop.
     */
    public static List<Block> euHuFamiliesLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.mixerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.electricMixerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.electricLoomBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.electricSifterBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.boxinatorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.unboxinatorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.fermenterBlockArray());
        return rBlocks;
    }

    /** The eu-hu families self-drop provider (task p29-w1-eu-hu-families; the paint carry = task p22-painted-item-domain). */
    public static final class GT6EuHuFamiliesBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6EuHuFamiliesBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6EuHuFamiliesBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return euHuFamiliesLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : euHuFamiliesLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
    /**
     * The eu-special families block list (task p29-w2-eu-special): the 14 blocks of the
     * three families (Autocrafter + Lightning Processor 5 tiers each — the first _t5
     * rungs — plus the Laminator 4 tiers) — the euHuFamiliesLootBlocks shape verbatim,
     * the MTE default self-drop.
     */
    public static List<Block> euSpecialLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.autocrafterBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.lightningBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.laminatorBlockArray());
        return rBlocks;
    }

    /** The eu-special families self-drop provider (task p29-w2-eu-special; the paint carry = task p22-painted-item-domain). */
    public static final class GT6EuSpecialMachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6EuSpecialMachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6EuSpecialMachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return euSpecialLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : euSpecialLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The exotic-energy families block list (task p29-w2-exotic-energy): the 30 blocks of
     * the six families (Polarizer/MagneticSeparator/LaserEngraver/LaserWelder/Freezer/
     * CryoMixer, 5 tiers each, Loader_MultiTileEntities.java :1418-1422/:1470-1474/
     * :1483-1487/:1490-1494/:1621-1625/:1628-1632) — the euHuFamiliesLootBlocks shape
     * verbatim, the MTE default self-drop.
     */
    public static List<Block> exoticFamiliesLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.polarizerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.magneticSeparatorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.laserEngraverBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.laserWelderBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.freezerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.cryoMixerBlockArray());
        // task p31-massfab — the small Matter Fabricator 5-ladder (the exotic shape)
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.massfabSmallBlockArray());
        // task p32-qu-scanner-replicator — the QU machine pair (the exotic shape)
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.molecularScannerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.replicatorBlockArray());
        return rBlocks;
    }

    /**
     * The eu-core families block list (task p29-w2-eu-core-5tier): the 25 blocks of the
     * five EU 5-tier families (Electrolyzer/Injector/Printer/ScannerVisuals/Slicer,
     * Loader_MultiTileEntities.java :1336-1340/:1443-1447/:1450-1454/:1457-1461/
     * :1525-1529) — the euHuFamiliesLootBlocks shape verbatim, the MTE default self-drop.
     */
    public static List<Block> euCoreLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.electrolyzerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.injectorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.printerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.scannerVisualsBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.slicerBlockArray());
        return rBlocks;
    }

    /** The eu-core families self-drop provider (task p29-w2-eu-core-5tier; the paint carry = task p22-painted-item-domain). */
    public static final class GT6EuCoreMachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6EuCoreMachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6EuCoreMachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return euCoreLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : euCoreLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /** The exotic-energy families self-drop provider (task p29-w2-exotic-energy; the paint carry = task p22-painted-item-domain). */
    public static final class GT6ExoticFamiliesBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6ExoticFamiliesBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6ExoticFamiliesBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return exoticFamiliesLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : exoticFamiliesLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The hu-tu piggyback block list (task p29-w2-hu-tu-piggyback): the 16 blocks of the
     * seven families (SteamCracker/CatalyticCracker 4-ladders + the TU four singles +
     * the Loom 4-ladder) — the euHuFamiliesLootBlocks shape verbatim, the MTE default
     * self-drop.
     */
    public static List<Block> huTuPiggybackLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.steamcrackerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.catalyticcrackerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.coagulatorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.generifierBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.bathBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.autoclaveBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.loomBlockArray());
        // task p29-w3-heat-smelter — the Smelter 4-ladder + the Melter single join the
        // machine self-drop provider (the same MTE default self-drop, the tail-append
        // EDIT ruling: the provider is the generic machine self-drop walker)
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.smelterBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.melterBlockArray());
        return rBlocks;
    }

    /** The hu-tu piggyback self-drop provider (task p29-w2-hu-tu-piggyback; the paint carry = task p22-painted-item-domain). */
    public static final class GT6HuTuPiggybackBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6HuTuPiggybackBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6HuTuPiggybackBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return huTuPiggybackLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : huTuPiggybackLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The W1 Kinetic trio block list (task p26-w1-sifter-compressor-wiremill): the twelve
     * Sifter/Compressor/Wiremill rows (Loader_MultiTileEntities.java :1312-1315/
     * :1343-1346/:1373-1376) — the cannerLootBlocks shape verbatim, the MTE default
     * self-drop.
     */
    public static List<Block> kineticLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.sifterBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.compressorBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.wiremillBlockArray());
        // task p28-c-ulv-machine-ladder — the sifter/wiremill ULV rungs ride the family
        // map walks above (the BY_PATH tables carry them); the Rolling Mill family joins
        // here (its single-row BET shares the TileEntityBasicMachine loot shape)
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.rollingmillBlockArray());
        return rBlocks;
    }

    /** The W1 Kinetic trio self-drop provider (task p26-w1-sifter-compressor-wiremill; the paint carry = task p22-painted-item-domain). */
    public static final class GT6KineticMachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6KineticMachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6KineticMachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return kineticLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : kineticLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The P29 W1 process-family block list (task p29-w1-kinetic-process-ladder): the
     * twenty-four Buzzsaw/Squeezer/Centrifuge/Sluice/Sanding Machine/Pressure Washer rows
     * (Loader_MultiTileEntities.java :1318-1321/:1324-1327/:1330-1333/:1464-1467/
     * :1589-1592/:1615-1618) — the kineticLootBlocks shape verbatim, the MTE default
     * self-drop.
     */
    public static List<Block> processLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.buzzsawBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.squeezerBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.centrifugeBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.sluiceBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.sandingBlockArray());
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.pressurewasherBlockArray());
        return rBlocks;
    }

    /** The P29 W1 process-family self-drop provider (task p29-w1-kinetic-process-ladder; the paint carry = task p22-painted-item-domain). */
    public static final class GT6ProcessMachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6ProcessMachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6ProcessMachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return processLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : processLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The press-family block list (task p26-w1-press-extruder-molds): the four Press rows
     * (Loader_MultiTileEntities.java:1425-1428) — the cannerLootBlocks shape verbatim, the
     * MTE default self-drop (the same Drops==null default).
     */
    public static List<Block> pressLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.PRESS_ROWS) {
            rBlocks.add(gregtech6.registry.GTMachines.PRESS_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The press-family self-drop provider (task p26-w1-press-extruder-molds; the paint carry = task p22-painted-item-domain). */
    public static final class GT6PressBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6PressBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6PressBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return pressLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : pressLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The extruder-family block list (task p26-w1-press-extruder-molds): the four Extruder
     * rows (Loader_MultiTileEntities.java:1406-1409) — the pressLootBlocks shape verbatim,
     * the MTE default self-drop (the same Drops==null default).
     */
    public static List<Block> extruderLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.EXTRUDER_ROWS) {
            rBlocks.add(gregtech6.registry.GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The extruder-family self-drop provider (task p26-w1-press-extruder-molds; the paint carry = task p22-painted-item-domain). */
    public static final class GT6ExtruderBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6ExtruderBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6ExtruderBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return extruderLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : extruderLootBlocks()) add(tBlock, paintSelfTable(tBlock));
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

    /** The heat-exchanger block list (task p29-w3-heat-smelter): the one controller block. */
    public static List<Block> heatExchangerLootBlocks() {
        return List.of(gregtech6.registry.GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get());
    }

    /** The heat-exchanger self-drop provider (task p29-w3-heat-smelter; the lightning-rod shape). */
    public static final class GT6HeatExchangerBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6HeatExchangerBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6HeatExchangerBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return heatExchangerLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : heatExchangerLootBlocks()) dropSelf(tBlock);
        }
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
     * The Advanced Crafting Table block list (task p24-act-machine): the single-variant
     * row (Loader_MultiTileEntities.java:136) — the cannerLootBlocks shape over one
     * block, the MTE default self-drop.
     */
    public static List<Block> advancedCraftingTableLootBlocks() {
        return List.of(gregtech6.registry.GTMachines.ADVANCED_CRAFTING_TABLE.get());
    }

    /** The ACT self-drop provider (task p24-act-machine; the paint carry = task p22-painted-item-domain). */
    public static final class GT6AdvancedCraftingTableBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6AdvancedCraftingTableBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6AdvancedCraftingTableBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return advancedCraftingTableLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : advancedCraftingTableLootBlocks()) add(tBlock, paintSelfTable(tBlock));
        }
    }

    /**
     * The FE converter block list (task p28-b-fe-converter-machine): the single ULV
     * machine row — the ACT list shape over one block, self-drop (the MTE default). The
     * fe_source fixture deliberately has NO loot row (the fixture rule, the fe_battery
     * precedent — RCON-driven, not survival-obtainable).
     */
    public static List<Block> feConverterLootBlocks() {
        return List.of(gregtech6.registry.GT6FeConverters.FE_CONVERTER.get());
    }

    /** The FE converter self-drop provider (task p28-b-fe-converter-machine). */
    public static final class GT6FeConverterBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6FeConverterBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6FeConverterBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return feConverterLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : feConverterLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The electric-transformer block list (task p28-c-ulv-lv-transformer): the MTE default self-drop (the :881 row, canDrop(0) == T — the Base10 :161 form), the 1.20.1 equivalent = dropSelf. */
    public static List<Block> electricTransformerLootBlocks() {
        return List.of(gregtech6.registry.GT6ElectricTransformers.ELECTRIC_TRANSFORMER.get());
    }

    /** The electric-transformer self-drop provider (task p28-c-ulv-lv-transformer, the FE-converter shape verbatim). */
    /**
     * The eu-bridge block list (task p29-w4-eu-bridge): the Roasting 4-ladder (the MTE
     * default self-drop with the paint carry, the smelter shape) + the 15 EU-bridge
     * converter rungs (plain self-drop — the dynamo/transformer shape, the converters
     * carry no paint face) + the 10 laser-domain converter rungs of task
     * p32-qu-laser-domain (the same plain self-drop posture over the same reused carrier)
     * + the Magic Field Absorber of task p32-magic-absorber (the same plain self-drop).
     */
    public static List<Block> euBridgeLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        java.util.Collections.addAll(rBlocks, gregtech6.registry.GTMachines.roastingBlockArray());
        for (java.util.Map<String, RegistryObject<Block>> tFamily : java.util.List.of(
                gregtech6.registry.GTMachines.ELECTRIC_HEATER_BLOCKS_BY_PATH,
                gregtech6.registry.GTMachines.ELECTRIC_ENGINE_BLOCKS_BY_PATH,
                gregtech6.registry.GTMachines.ELECTRIC_MOTOR_BLOCKS_BY_PATH,
                gregtech6.registry.GT6Lasers.CO2_LASER_BLOCKS_BY_PATH,
                gregtech6.registry.GT6Lasers.LASER_ABSORBER_BLOCKS_BY_PATH,
                gregtech6.registry.GT6MagicAbsorbers.MAGIC_ABSORBER_BLOCKS_BY_PATH,
                gregtech6.registry.GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH)) {
            for (RegistryObject<Block> tHandle : tFamily.values()) rBlocks.add(tHandle.get());
        }
        return rBlocks;
    }

    /** The eu-bridge self-drop provider (task p29-w4-eu-bridge; the Roasting paint carry = task p22-painted-item-domain). */
    public static final class GT6EuBridgeBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6EuBridgeBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6EuBridgeBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return euBridgeLootBlocks();
        }

        @Override
        protected void generate() {
            java.util.List<Block> tRoasting = java.util.Arrays.asList(gregtech6.registry.GTMachines.roastingBlockArray());
            for (Block tBlock : euBridgeLootBlocks()) {
                if (tRoasting.contains(tBlock)) {
                    add(tBlock, paintSelfTable(tBlock)); // the machine paint carry
                } else {
                    dropSelf(tBlock); // the converter plain self-drop, the transformer form
                }
            }
        }
    }

    public static final class GT6ElectricTransformerBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6ElectricTransformerBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6ElectricTransformerBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return electricTransformerLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : electricTransformerLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The anvil block list (task p28-c-anvil): the two stone anvil rows — the FE
     * converter list shape over two blocks, self-drop (the MTE Drops default; the
     * working-surface content pop rides the block's onRemove face, not the loot table).
     */
    public static List<Block> anvilLootBlocks() {
        return List.of(
                gregtech6.registry.GT6Anvils.STONE_ANVIL.get(),
                gregtech6.registry.GT6Anvils.BLACKSTONE_ANVIL.get());
    }

    /** The anvil self-drop provider (task p28-c-anvil, the GT6FeConverterBlockLoot form). */
    public static final class GT6AnvilBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6AnvilBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6AnvilBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return anvilLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : anvilLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The Electric Dynamo T0 row block list (task p28-c-ulv-dynamo-row): the single ULV
     * self-drop — the feConverterLootBlocks list shape over one block. The upstream dynamo
     * rows carry the MTE default self-drop (canDrop = F with the Base10 default drop =
     * the block item itself, TileEntityBase10EnergyConverter.java:161 — the
     * kineticsLootBlocks ruling). CARD SCOPE: only the NEW tier — the W1 five LV..IV rows
     * and the whole Flux family stay table-less (their pre-existing gap, the W2
     * render-recipe card's declared surface); the plain dropSelf (NOT the paint carry —
     * the dynamo family is outside the machine paint census, the FE converter precedent).
     */
    public static List<Block> dynamoUlvLootBlocks() {
        return List.of(gregtech6.registry.GT6ElectricDynamos.ELECTRIC_DYNAMO_ULV.get());
    }

    /** The Electric Dynamo T0 self-drop provider (task p28-c-ulv-dynamo-row). */
    public static final class GT6DynamoUlvBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6DynamoUlvBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6DynamoUlvBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return dynamoUlvLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : dynamoUlvLootBlocks()) dropSelf(tBlock);
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
     * [{"source": "'gt.color'", "target": "BlockEntityTag.'gt.color'", "op": "replace"}, ...]}}.
     *
     * <p>QUOTED DOT KEYS (task p25-paint-loot-dotkey-fix): NbtPathArgument splits a path
     * on {@code '.'} (vanilla 1.20.1 NbtPathArgument.java:70-73 {@code expect('.')} between
     * nodes; :140 {@code isAllowedInUnquotedName} — a dot is NOT a name character), so the
     * raw dotted key {@code gt.color} parsed as the compound traversal root→gt→color and
     * NEVER matched the flat key the BE writes (TileEntityBase03TicksAndSync.java:323-324)
     * — both ops silently no-oped since P22 (CopyNbtFunction.CopyOperation.apply swallows
     * the miss, CopyNbtFunction.java:143-150), the paint round-trip never landed, and the
     * P23 item-side two-level read (6e617047) was starving on an empty payload. A
     * quote-opening node reads a FULL string including dots (:81-83), so
     * {@link #quoted} wraps each dotted key as the one-node SNBT quoted form.
     */
    private static LootItemFunction.Builder paintCopyNbt() {
        //? if neoforge {
        /*return net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction
                .copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_COLOR), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_COLOR),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_PAINTED), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_PAINTED),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE);
         *///?} else {
        return CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_COLOR), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_COLOR),
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_PAINTED), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_PAINTED),
                        CopyNbtFunction.MergeStrategy.REPLACE);
        //?}
    }

    /**
     * The single-quote SNBT-path segment wrap for a flat dotted key: NbtPathArgument's
     * parseNode routes a {@code '} / {@code "} opening node through {@code readString()}
     * (vanilla 1.20.1 NbtPathArgument.java:81-83), which consumes the whole quoted literal
     * — dots included — as ONE compound child name. The quoted segments live in the
     * generated JSON verbatim (CopyNbtFunction.CopyOperation.toJson writes the raw
     * path texts), and the loot runtime re-parses them through the same argument type
     * (CopyNbtFunction.compileNbtPath), so builder-time and runtime shapes stay identical.
     */
    private static String quoted(String aKey) {
        return "'" + aKey + "'";
    }

    /**
     * The machine-family block list (task p22-painted-item-domain; the Oven ladder joins in
     * task p27-oven-heat-t-ladder): the 16 machine-domain
     * blocks whose loot this NEW provider owns — the oven ladder (4) + the shredder/crusher/lathe
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
        rBlocks.add(gregtech6.registry.GTMachines.OVEN_T2.get()); // task p27-oven-heat-t-ladder
        rBlocks.add(gregtech6.registry.GTMachines.OVEN_T3.get());
        rBlocks.add(gregtech6.registry.GTMachines.OVEN_T4.get());
        for (RegistryObject<Block> tBlock : java.util.List.of(
                gregtech6.registry.GTMachines.SHREDDER, gregtech6.registry.GTMachines.SHREDDER_T2,
                gregtech6.registry.GTMachines.SHREDDER_T3, gregtech6.registry.GTMachines.SHREDDER_T4,
                gregtech6.registry.GTMachines.CRUSHER, gregtech6.registry.GTMachines.CRUSHER_T2,
                gregtech6.registry.GTMachines.CRUSHER_T3, gregtech6.registry.GTMachines.CRUSHER_T4,
                gregtech6.registry.GTMachines.LATHE, gregtech6.registry.GTMachines.LATHE_T2,
                gregtech6.registry.GTMachines.LATHE_T3, gregtech6.registry.GTMachines.LATHE_T4)) {
            rBlocks.add(tBlock.get());
        }
        // task p28-c-ulv-machine-ladder — the shredder/crusher ULV rungs (the row-carrier
        // blocks of the legacy tierOf families; the sifter/wiremill/canner/rollingmill ULV
        // rungs ride their own family providers above)
        rBlocks.add(gregtech6.registry.GTMachines.SHREDDER_ULV.get());
        rBlocks.add(gregtech6.registry.GTMachines.CRUSHER_ULV.get());
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

    /**
     * The fluid-pipe family block list (task p25-c-foam-pipe-spray spec ⑦): the two wood
     * tiers. Pre-existing state: the pipes shipped TABLE-LESS (breaking dropped nothing —
     * the same gap the p22 machine census closed for its 13); the foam NBT round-trip
     * needs a real drop, so this card gives them the upstream MTE default self-drop WITH
     * the foam carry (upstream writeItemNBT2, TileEntityBase10ConnectorRendered.java:82-87 —
     * the port's writeItemNBT2 counterpart is the loot copy_nbt, the p22 painted-item form).
     */
    public static List<Block> pipeLootBlocks() {
        return List.of(gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get(),
                gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.get());
    }

    /**
     * The pipe-family self-drop provider (task p25-c-foam-pipe-spray; the paint carry = the
     * p22 painted-item-domain form — the pipe BE rides the 03 base IPaintableTE stratum, so
     * a paint-less drop would reintroduce the exact regression p22 closed).
     */
    public static final class GT6PipeBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6PipeBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6PipeBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return pipeLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : pipeLootBlocks()) add(tBlock, foamPaintSelfTable(tBlock));
        }
    }

    /**
     * The foam+paint carrying self-drop table — the {@link #paintSelfTable} shape extended
     * by the three foam keys: the vanilla createSingleItemTable form (one pool, rolls 1,
     * {@code survives_explosion}) plus ONE entry-level {@code copy_nbt} from the block
     * entity into the dropped stack's {@code BlockEntityTag}, five REPLACE ops keyed off the
     * BE's own constants (single decision site — the BE save and the loot copy can never
     * drift): the upstream writeItemNBT2 trio {@code gt.foamed}/{@code gt.foamdried}/
     * {@code gt.ownable} (TileEntityBase10ConnectorRendered.java:82-87) plus the 03 base
     * paint pair {@code gt.color}/{@code gt.painted}. NOTE what is deliberately NOT copied:
     * {@code gt.owner} — the owner does NOT ride the item (upstream :82-87 has no owner op;
     * the re-placed pipe records the NEW placer through onPlaced, the :148-150 form).
     */
    static LootTable.Builder foamPaintSelfTable(ItemLike aItem) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(aItem).apply(foamPaintCopyNbt())));
    }

    /**
     * The foam+paint carry function builder — the {@code copy_nbt} five-op form over the
     * same ContextNbtProvider.BLOCK_ENTITY source (the generated JSON shape:
     * {@code {"function": "minecraft:copy_nbt", "source": "block_entity", "ops":
     * [{"source": "'gt.foamed'", "target": "BlockEntityTag.'gt.foamed'", "op": "replace"}, ...]}}).
     *
     * <p>QUOTED SEGMENTS (the live-chain finding of this card): NbtPathArgument splits
     * paths on {@code '.'} (vanilla 1.20.1 NbtPathArgument.java:70-71 {@code expect('.')}
     * / :140 — a dot is not a name character), so the raw dotted key {@code gt.foamed}
     * parses as the compound traversal root→gt→foamed and NEVER matches the flat key the
     * BE writes — the op silently no-ops. The literal key rides as a quoted segment
     * {@code 'gt.foamed'} (parseNode :81-82 — a quote-opening node reads a full string).
     * The p22 {@link #paintSelfTable} ops shared this defect (dotted, unquoted) — fixed
     * in the same rebase window by task p25-paint-loot-dotkey-fix (this file's
     * {@link #paintCopyNbt} + the shared {@link #quoted} helper).
     */
    private static LootItemFunction.Builder foamPaintCopyNbt() {
        //? if neoforge {
        /*return net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction
                .copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_FOAMED), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_FOAMED),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_FOAMDRIED), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_FOAMDRIED),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_OWNABLE), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_OWNABLE),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_COLOR), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_COLOR),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_PAINTED), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_PAINTED),
                        net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction.MergeStrategy.REPLACE);
         *///?} else {
        return CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_FOAMED), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_FOAMED),
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_FOAMDRIED), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_FOAMDRIED),
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(quoted(GTFluidPipeBlockEntity.NBT_OWNABLE), "BlockEntityTag." + quoted(GTFluidPipeBlockEntity.NBT_OWNABLE),
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_COLOR), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_COLOR),
                        CopyNbtFunction.MergeStrategy.REPLACE)
                .copy(quoted(TileEntityBase03TicksAndSync.NBT_PAINTED), "BlockEntityTag." + quoted(TileEntityBase03TicksAndSync.NBT_PAINTED),
                        CopyNbtFunction.MergeStrategy.REPLACE);
        //?}
    }

    // -------------------------------------------------------------------------
    // task p26-c-foam-block-family — the C-Foam block family band
    // -------------------------------------------------------------------------

    /**
     * The DRIED self-drop pair (task p26-c-foam-block-family): upstream BlockCFoam has no
     * getDrops override (BlockCFoam.java:37-76 — the vanilla default self-drop, the same
     * Drops==null form as the wire/axle families). NO foam NBT carry: the dried block's
     * item is the UNCOLOURED ladder collapse (the GT6FoamBlocks registry doc — the placed
     * colour persists while standing via the tint, the re-placed item lands colour 0), and
     * {@code gt.foamdried}/{@code gt.ownable} ride the BLOCKSTATE/BE, not the item.
     */
    public static List<Block> cfoamDriedLootBlocks() {
        return List.of(GT6FoamBlocks.CFOAM.get(), GT6FoamBlocks.CFOAM_SLAB.get());
    }

    /**
     * The drop-nothing trio: the two FRESH forms (upstream BlockCFoamFresh.getDrops
     * returns the empty list, :70-72 verbatim) plus the owned carrier (upstream
     * MultiTileEntityCFoam.canDrop false, :151 — and showInCreative false :152 means no
     * BlockItem exists to drop at all). An EMPTY loot table is the exact form.
     */
    public static List<Block> cfoamNoDropLootBlocks() {
        return List.of(GT6FoamBlocks.CFOAM_FRESH.get(), GT6FoamBlocks.CFOAM_FRESH_SLAB.get(),
                GT6FoamBlocks.CFOAM_OWNED.get());
    }

    /** The C-Foam family loot provider: dried = self-drop, fresh/owned = the empty table. */
    public static final class GT6CFoamBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6CFoamBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6CFoamBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return cfoamLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : cfoamDriedLootBlocks()) dropSelf(tBlock); // the void form (this mapping's dropSelf registers itself — the wire family :172 shape)
            for (Block tBlock : cfoamNoDropLootBlocks()) add(tBlock, LootTable.lootTable());
        }
    }

    /** The full family (the known-blocks narrowing — the missing-table validation covers exactly these five). */
    public static List<Block> cfoamLootBlocks() {
        List<Block> rBlocks = new ArrayList<>(cfoamDriedLootBlocks());
        rBlocks.addAll(cfoamNoDropLootBlocks());
        return rBlocks;
    }

    /**
     * The static storage batch block list (task p26-storage-static-batch): the 28
     * GT6StaticStorages rows — the loader MTE default self-drop (Drops==null,
     * PrefixBlock.java:227) over the locker/drawer/safe/bookshelf/bottlecrate ladders,
     * the cannerLootBlocks shape verbatim.
     */
    public static List<Block> staticStorageLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
            rBlocks.add(gregtech6.registry.GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /**
     * The sensor-family sub-provider (task p26-sensors-core): the three pioneer sensor
     * blocks self-drop (the axle/wire family form — the upstream MTE default, the block
     * itself drops; a sensor carries no item inventory, canDrop :291 answers F for the
     * slots and the tile drops as its block).
     */
    public static final class GT6SensorBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6SensorBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6SensorBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        /** The three pioneer blocks (the GT6Sensors.ROWS walk, the registration order). */
        public static List<Block> sensorLootBlocks() {
            List<Block> rBlocks = new java.util.ArrayList<>();
            for (gregtech6.registry.GT6Sensors.SensorRow tRow : gregtech6.registry.GT6Sensors.ROWS) {
                rBlocks.add(gregtech6.registry.GT6Sensors.BLOCKS_BY_PATH.get(tRow.path()).get());
            }
            return rBlocks;
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return sensorLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : sensorLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The static storage self-drop provider (task p26-storage-static-batch): one
     * unconditional self-drop per row (the vanilla survives_explosion condition, the
     * GT6BlockLoot convention). NOTE the safe's dungeon-loot seam does NOT live here —
     * the safe marker resolves ANY loot table id at runtime through the LootDataManager
     * (LootDataResolver.getLootTable, vanilla LootDataResolver.java:25), so the gt6
     * wrapper tables (data/gt6/loot_tables/chests/safe_*.json, static resources) are the
     * tier-a injection seam pack authors touch — a datagen-written reference to a vanilla
     * table would fail this provider's validation (LootTableProvider.run closes over only
     * its own tables, LootTableProvider.java:63-71).
     */
    public static final class GT6StaticStorageBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6StaticStorageBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6StaticStorageBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return staticStorageLootBlocks();
        }

        @Override
        protected void generate() {
            // dropSelf registers through the void add() face — the statement form, the
            // wireLootBlocks/axleLootBlocks precedent (:176/:215)
            for (Block tBlock : staticStorageLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The part-family expansion block list (task p29-w3-nbtdesign-parts ③): the 28
     * new-form part blocks (metal walls 10 — the tungsten wall already rides the
     * Lightning Rod provider — plus coils 5, parts 6, ventilation, processor units 5,
     * the wood wall). The upstream part MTEs self-drop (the MTE default). NOTE the six
     * Dense Wall additions also ship table-less here (the older walls' pre-existing gap
     * carries — not this card's delta).
     */
    public static List<Block> partLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GTMultiBlocks.NEW_PART_ROWS) {
            if (!gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.containsKey(tRow.path())) continue; // machine_wall_tungsten — the reused Lightning Rod registration
            rBlocks.add(gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /**
     * The turbine/dynamo controller block list (task p29-w3-turbine-dynamo): the twelve
     * Large Turbine + Large Dynamo mains (Loader_MultiTileEntities.java:1254-1257/
     * :1259-1262/:1264-1267). The upstream machines carry the MTE default self-drop (the
     * same Drops==null default as the boiler/burning-box families); the 1.20.1 equivalent
     * is exactly {@code dropSelf}.
     */
    public static List<Block> turbineDynamoLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GT6Turbines.STEAM_ROWS) {
            rBlocks.add(gregtech6.registry.GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        for (var tRow : gregtech6.registry.GT6Turbines.GAS_ROWS) {
            rBlocks.add(gregtech6.registry.GT6Turbines.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        for (var tRow : gregtech6.registry.GT6DynamoHousings.DYNAMO_ROWS) {
            rBlocks.add(gregtech6.registry.GT6DynamoHousings.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The turbine/dynamo controller self-drop provider (task p29-w3-turbine-dynamo; the part-family provider shape). */
    public static final class GT6TurbineDynamoBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6TurbineDynamoBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6TurbineDynamoBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return turbineDynamoLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : turbineDynamoLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The part-family self-drop provider (task p29-w3-nbtdesign-parts; the lightning-rod provider shape). */
    public static final class GT6PartBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6PartBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6PartBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return partLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : partLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The Tank Main Valve block list (task p29-w3-tank-valves): the 25 valve blocks (the upstream MTE default self-drop). */
    public static List<Block> tankLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GT6Tanks.ROWS) {
            rBlocks.add(gregtech6.registry.GT6Tanks.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The Tank Main Valve self-drop provider (task p29-w3-tank-valves; the part-family provider shape). */
    public static final class GT6TankBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6TankBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6TankBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return tankLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : tankLootBlocks()) dropSelf(tBlock);
        }
    }

/**
     * The distill-crucible family block list (task p29-w3-distill-crucible): the two tower
     * controllers + the SEVEN new crucible rungs and their walls (self-drops, the MTE
     * default). The pre-existing crucible_steel lootless gap CARRIES (the P26 state — not
     * this card's delta, the dense-wall gap precedent).
     */
    public static List<Block> distillCrucibleLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GT6Distillation.ROWS) {
            rBlocks.add(gregtech6.registry.GT6Distillation.TOWER_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        for (var tHandle : gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.values()) {
            rBlocks.add(tHandle.get());
        }
        for (var tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            if ("crucible_steel".equals(tRow.path())) continue; // the pre-existing gap carries
            rBlocks.add(gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The distill-crucible self-drop provider (task p29-w3-distill-crucible; the part provider shape). */
    public static final class GT6DistillCrucibleLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6DistillCrucibleLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6DistillCrucibleLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return distillCrucibleLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : distillCrucibleLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The twelve large machines (task p29-w3-large-12) — the upstream large-machine MTEs
     * self-drop (the MTE default; the controllers carry no item inventory face this port
     * drops — the gated item handler is the BE's, not the block drop's), the vanilla
     * {@code dropSelf} equivalent.
     */
    public static List<Block> largeMachineLootBlocks() {
        List<Block> rBlocks = new ArrayList<>();
        for (gregtech6.registry.GT6LargeMachines.LargeMachineRow tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            rBlocks.add(gregtech6.registry.GT6LargeMachines.BLOCKS_BY_PATH.get(tRow.path()).get());
        }
        return rBlocks;
    }

    /** The Fusion Reactor self-drop list (task p31-fusion — the massfabLootBlocks singleton form). */
    public static List<Block> fusionLootBlocks() {
        return List.of(gregtech6.registry.GTMultiBlocks.FUSION_REACTOR.get());
    }

    /** The Fusion Reactor self-drop provider (task p31-fusion; the massfab provider shape). */
    public static final class GT6FusionBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6FusionBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6FusionBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return fusionLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : fusionLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The Logistics Core self-drop list (task p32-logistics-lv3 — the massfabLootBlocks singleton form). */
    public static List<Block> logisticsCoreLootBlocks() {
        return List.of(gregtech6.registry.GT6Logistics.LOGISTICS_CORE.get());
    }

    /** The Logistics Core self-drop provider (task p32-logistics-lv3; the massfab provider shape). */
    public static final class GT6LogisticsCoreBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6LogisticsCoreBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6LogisticsCoreBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return logisticsCoreLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : logisticsCoreLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The Large Matter Fabricator self-drop list (task p31-massfab — the graaggLootBlocks singleton form). */
    public static List<Block> massfabLootBlocks() {
        return List.of(gregtech6.registry.GTMultiBlocks.MASSFAB.get());
    }

    /** The Large Matter Fabricator self-drop provider (task p31-massfab; the graagg provider shape). */
    public static final class GT6MassfabBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6MassfabBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6MassfabBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return massfabLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : massfabLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The Implosion Compressor self-drop list (task p31-implosion — the largeMachineLootBlocks singleton form). */
    public static List<Block> implosionLootBlocks() {
        return List.of(gregtech6.registry.GTMultiBlocks.IMPLOSION_COMPRESSOR.get());
    }

    /** The Implosion Compressor self-drop provider (task p31-implosion; the large-machine provider shape). */
    public static final class GT6ImplosionBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6ImplosionBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6ImplosionBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return implosionLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : implosionLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The Von da Graagg self-drop list (task p31-graagg — the implosionLootBlocks singleton form). */
    public static List<Block> graaggLootBlocks() {
        return List.of(gregtech6.registry.GTMultiBlocks.VON_DA_GRAAGG.get());
    }

    /** The Von da Graagg self-drop provider (task p31-graagg; the implosion provider shape). */
    public static final class GT6GraaggBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6GraaggBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6GraaggBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return graaggLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : graaggLootBlocks()) dropSelf(tBlock);
        }
    }

    /** The large-machine self-drop provider (task p29-w3-large-12; the part-provider shape). */
    public static final class GT6LargeMachineBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6LargeMachineBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6LargeMachineBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return largeMachineLootBlocks();
        }

        @Override
        protected void generate() {
            for (Block tBlock : largeMachineLootBlocks()) dropSelf(tBlock);
        }
    }

    /**
     * The surface deco block list (task p30-w6-rocks-sticks): the three per-material
     * surface rocks + the stick — four blocks, four pickup/break loot tables.
     */
    public static List<Block> surfaceLootBlocks() {
        return GT6SurfaceBlocks.ALL.stream().map(RegistryObject::get).toList();
    }

    /**
     * The obtainable surface-plants + fallen-woods block list (task p30-w6-t2-surface-blocks):
     * the plant quartet (glowtus/bush/black sand/turf) + the four fallen-log woods — all
     * drop themselves (the bush's berry item face is the declared cut, GT6WildBushBlock).
     */
    public static List<Block> plantLootBlocks() {
        List<Block> rList = new java.util.ArrayList<>();
        for (RegistryObject<Block> tRow : GT6SurfaceBlocks.PLANT_BAND) rList.add(tRow.get());
        for (RegistryObject<Block> tRow : GT6SurfaceBlocks.FALLEN_LOGS) rList.add(tRow.get());
        return rList;
    }

    /**
     * The surface deco provider (task p30-w6-rocks-sticks). The upstream MTE getDrops
     * {@code getRock(1+rng(1+fortune))} (MultiTileEntityRock.java:81) transcribes as ONE
     * pool: count uniform 1..2 (SetItemCountFunction + UniformGenerator), the winner per
     * the upstream NBT lottery —
     * <ul>
     * <li>{@code surface_rock_stone}: {@code gt6:rock_gt_stone} (the NBT-less default
     *     rock, the overworld arm of MultiTileEntityRock.java:174);</li>
     * <li>{@code surface_rock_flint}: {@code minecraft:flint} (11/12 of the NBT half,
     *     WorldgenRocks.java:63 {@code ST.make(Items.flint, 1, 0)});</li>
     * <li>{@code surface_rock_meteorite}: {@code gt6:rock_gt_meteoric_iron} weight 3 /
     *     {@code gt6:ore_raw_meteoric_iron} weight 1 (WorldgenRocks.java:63
     *     {@code nextInt(4)==0 ? oreRaw : rockGt} verbatim);</li>
     * <li>{@code surface_stick}: {@code minecraft:stick} (MultiTileEntityStick
     *     .java:103 {@code IL.Stick.get} — the biome wood band is a DECLARED DEVIATION:
     *     the ~25-material substring ladder is not materialised, the vanilla stick is the
     *     research-converged first batch, the wood rows land later as loot-table biome
     *     conditions, never as 25 blocks).</li>
     * </ul>
     * DECLARED DEVIATION: the fortune bonus ({@code rng(1+fortune)}) is not wired — the
     * 1.21.1 leg moved Enchantments to ResourceKey + ApplyBonusCount to
     * {@code Holder<Enchantment>}, a leg fork bought only by the rare pickaxe-break path
     * (the upstream right-click pickup gives exactly 1, MultiTileEntityRock.java:145);
     * the fortune leg lands with the t1 loot-modifier wave if a live use shows up.
     */
    public static final class GT6SurfaceBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6SurfaceBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6SurfaceBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            List<Block> rList = new java.util.ArrayList<>(surfaceLootBlocks());
            rList.addAll(plantLootBlocks());
            return rList;
        }

        /** The collected-deco table: one pool, uniform 1..2 of {@code aItem} (the getRock count row). */
        private LootTable.Builder collectedTable(ItemLike aItem) {
            return LootTable.lootTable().withPool(this.applyExplosionCondition(aItem,
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                            .add(LootItem.lootTableItem(aItem))));
        }

        @Override
        protected void generate() {
            add(GT6SurfaceBlocks.SURFACE_ROCK_STONE.get(),
                    collectedTable(GTMaterialItems.get(OP.rockGt, MT.Stone).get()));
            add(GT6SurfaceBlocks.SURFACE_ROCK_FLINT.get(), collectedTable(Items.FLINT));
            add(GT6SurfaceBlocks.SURFACE_ROCK_METEORITE.get(),
                    // WorldgenRocks.java:63 nextInt(4)==0 ? oreRaw : rockGt — the 3:1 weights
                    LootTable.lootTable().withPool(this.applyExplosionCondition(
                            GT6SurfaceBlocks.SURFACE_ROCK_METEORITE.get(),
                            LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                    .add(LootItem.lootTableItem(GTMaterialItems.get(OP.rockGt, MT.MeteoricIron).get()).setWeight(3))
                                    .add(LootItem.lootTableItem(GTMaterialItems.get(OP.oreRaw, MT.MeteoricIron).get()).setWeight(1)))));
            add(GT6SurfaceBlocks.SURFACE_STICK.get(), collectedTable(Items.STICK));
            // task p30-w6-t2-surface-blocks — the obtainable band: self-drop
            for (Block tBlock : plantLootBlocks()) {
                dropSelf(tBlock);
            }
            // task p30-w6-t3-large-veins — the 31 vein-indicator rocks: the WorldgenOresLarge
            // .java:104 arm (the rock carries rockGt of the picked vein material) as one
            // collectedTable per rock, INDICATOR_SPECS order.
            for (int i = 0; i < GT6SurfaceBlocks.INDICATOR_ROCKS.size(); i++) {
                add(GT6SurfaceBlocks.INDICATOR_ROCKS.get(i).get(),
                        collectedTable(GTMaterialItems.get(OP.rockGt, GT6SurfaceBlocks.INDICATOR_MATERIALS.get(i).get()).get()));
            }
        }
    }

    /**
     * The placeables-band loot (task p32-placeables): the Greg o'Lantern + the Sandwich
     * drop themselves (dropSelf — the upstream lantern {@code canDrop F :51} means the MTE
     * item never drops while the block drops itself via getDrops; the modern item IS that
     * loot face; the upstream Sandwich getDrops single-stack branch collapses to the item
     * form). The six placed piles are NOT here — they are noLootTable, the contents drop
     * through the block's playerDestroy walk (the GT6BumbleHiveBlock loot-shell form).
     */
    public static final class GT6PlaceableBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6PlaceableBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6PlaceableBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(gregtech6.registry.GT6Placeables.GREG_O_LANTERN.get(),
                    gregtech6.registry.GT6Placeables.SANDWICH.get());
        }

        @Override
        protected void generate() {
            dropSelf(gregtech6.registry.GT6Placeables.GREG_O_LANTERN.get());
            dropSelf(gregtech6.registry.GT6Placeables.SANDWICH.get());
        }
    }

    /**
     * The Bumbliary pair loot (task p33-bees-lv3-b-bumbliary): both machines drop
     * themselves (the upstream MTE item form — the assembled machine the :2222-2223
     * registry rows craft); the contents scatter through the block's vanilla
     * {@code onRemove} container walk, not the loot table.
     */
    public static final class GT6BumbliaryBlockLoot extends BlockLootSubProvider {

        //? if neoforge {
        /*
        public GT6BumbliaryBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
        }
         *///?} else {
        public GT6BumbliaryBlockLoot() {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
        }
        //?}

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(gregtech6.registry.GT6BeeHives.BUMBLIARY.get(),
                    gregtech6.registry.GT6BeeHives.BUMBLIARY_ADVANCED.get());
        }

        @Override
        protected void generate() {
            dropSelf(gregtech6.registry.GT6BeeHives.BUMBLIARY.get());
            dropSelf(gregtech6.registry.GT6BeeHives.BUMBLIARY_ADVANCED.get());
        }
    }

    /**
     * The gt.flawless/gt.gems/gt.misc bag weight tables (task p34-loot-injection spec ③, the
     * upstream ChestGenHooks categories {@code Loader_Loot.java:81-177}): one CHEST-param-set
     * table each at {@code gt6:chests/<name>}, one pool rolling the {@code setMin(8)/setMax(24)}
     * column ({@code :82-83/:107-108/:130-131} — the upstream roll count of a bag/category
     * open), entries = the upstream {@code addLoot} rows verbatim (weight + the
     * {@code set_count} [min,max] range). These are the rollable 对位 of the bag tables: the
     * bag ITEMS are unported (the declared pool on {@code GT6LootInjectionDatagen}), so the
     * tables serve the /loot face, the {@code GT6SafeBlockEntity} dungeonloot key (any table
     * id) and pack authors. Pure item entries — no vanilla references, so the
     * LootTableProvider self-validation the {@code gt6:chests/safe_*} statics dodged does not
     * apply here.
     */
    public static final class GT6WeightTableLoot implements net.minecraft.data.loot.LootTableSubProvider {

        //? if neoforge {
        /*
        public GT6WeightTableLoot(HolderLookup.Provider registries) {
        }
         *///?} else {
        public GT6WeightTableLoot() {
        }
        //?}

        @Override
        //? if forge {
        public void generate(java.util.function.BiConsumer<ResourceLocation, LootTable.Builder> aOutput) {
            for (GT6LootInjectionDatagen.WeightRow tRow : GT6LootInjectionDatagen.weightTables()) {
                aOutput.accept(new ResourceLocation("gt6", "chests/" + tRow.name()), table(tRow));
            }
        }
        //?} else {
        /*public void generate(java.util.function.BiConsumer<net.minecraft.resources.ResourceKey<LootTable>, LootTable.Builder> aOutput) {
            for (GT6LootInjectionDatagen.WeightRow tRow : GT6LootInjectionDatagen.weightTables()) {
                aOutput.accept(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                        tableId(tRow.name())), table(tRow));
            }
        }
        *///?}

        /** The {@code gt6:chests/<name>} id — the two-arg ctor is forge-only (the 21.1 removal). */
        private static ResourceLocation tableId(String aName) {
            //? if forge {
            return new ResourceLocation("gt6", "chests/" + aName);
            //?} else {
            /*return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "chests/" + aName);
            *///?}
        }

        /** The 8..24-roll pool over the weighted {@code set_count} entries (the bag-open face). */
        private static LootTable.Builder table(GT6LootInjectionDatagen.WeightRow aRow) {
            LootPool.Builder tPool = LootPool.lootPool()
                    .setRolls(UniformGenerator.between(GT6LootInjectionDatagen.BAG_ROLL_MIN, GT6LootInjectionDatagen.BAG_ROLL_MAX));
            for (GT6LootInjectionDatagen.EntryRow tEntry : aRow.entries()) {
                Item tItem = GT6LootInjectionDatagen.resolveItem(tEntry.item());
                if (tItem == Items.AIR) throw new IllegalStateException(
                        "weight table row references an unregistered item: " + tEntry.item());
                tPool.add(LootItem.lootTableItem(tItem).setWeight(tEntry.weight())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(tEntry.min(), tEntry.max()))));
            }
            // the vanilla chest-table conventions: the CHEST param set + the table-id sequence
            return LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
                    .setRandomSequence(tableId(aRow.name()))
                    .withPool(tPool);
        }
    }
}
