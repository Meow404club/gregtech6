package gregtech6.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTOvenBlock;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.block.foam.GT6CFoamOwnedBlock;
import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.energy.GTAxleBlock;
import gregtech6.block.energy.GTDieselEngineBlock;
import gregtech6.block.energy.GTTransformerRotationBlock;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.sensors.GTSensorBlock;
import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.block.tank.GTBarrelBlock;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GTEnergySources;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTGrassBlocks;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GT6FeBatteries; // p26 tail-append
import gregtech6.registry.GT6FeConverters; // p28 tail-append
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockBase;

/**
 * Blockstate + block model provider (task p3-example-machine — the first blockstates this
 * pack generates; GT6DataGenerators doc said "blockstates do not exist yet" until this card).
 * Appended to the provider set without restructuring the existing ones.
 *
 * <p>The example chest renders as a placeholder cube_all block (the chest TESR/lid is a
 * feature-layer omission, MultiTileEntityChest.java:344-417); the BlockItem model parents the
 * block model, the documented Forge blockstates idiom. Block model saving
 * (BlockStateProvider.run, BlockStateProvider.java:103-104) flushes block models before item
 * models, so the same-run parent resolves in the ExistingFileHelper. The layer texture
 * {@code gt6:textures/block/example_chest.png} is a script-generated placeholder PNG
 * (mdk/tools/gen_gui_textures.py), not JSON — no red-line conflict.
 *
 * <p>Task p4-fluid-pipes (W1) appends the fluid pipes: a cube_all placeholder over
 * {@code gt6:textures/block/fluid_pipe_wood.png} with a variant per
 * {@link gregtech6.block.pipe.GTFluidPipeBlock} CONNECTIONS mask value (0..63, the 6-bit
 * connection state — W1 renders every mask with the same model, the per-connection model
 * picking is the render pool item). The texture is an inline-script generated placeholder
 * PNG (same hand-rolled style as mdk/tools/gen_gui_textures.py), not JSON.
 */
public final class GT6BlockStates extends BlockStateProvider {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");

    /** The runData-side tint census counter (machineModel invocations — the datagen-JVM half of the pinned 21x3 audit). */
    private int mMachineTintModels;

    /** The runData-side barrel tint census counter (task p23-barrel-paint-render — the datagen-JVM half of the pinned 16 audit). */
    private int mBarrelTintModels;

    public GT6BlockStates(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GT6DataGenerators.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Block tChest = GTBlockEntities.EXAMPLE_CHEST.get();
        simpleBlock(tChest, models().cubeAll("example_chest", modLoc("block/example_chest")));
        itemModels().withExistingParent("example_chest", modLoc("block/example_chest"));
        addFluidPipe(GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get());
        addFluidPipe(GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.get());
        // task p26-pipe-item — the item pipe family: one cube_all placeholder per row, the
        // restrictive variants over their own PNG (the p20 placeholder ruling, no per-material art)
        for (GTItemPipes.ItemPipeRow tItemRow : GTItemPipes.ROWS) {
            addItemPipe(GTItemPipes.BLOCKS_BY_PATH.get(tItemRow.path()).get(),
                    tItemRow.variant().suffix.startsWith("restrictive"));
        }
        addWire(GTWires.WIRE_ELECTRIC_1X.get());
        addWire(GTWires.WIRE_ELECTRIC_2X.get());
        // task p10: the two wire loops SHARE one (set -> model) map — models().getBuilder
        // APPENDS to a same-named builder, so a second local map would stack a duplicate
        // element onto the shared copper/wire model JSON (caught by the runData diff).
        Map<String, ModelFile> tWireShared = new HashMap<>();
        addWireFamily(tWireShared); // task p9-wire-family-w1 ⑥ — the 620-block loop, isolated section
        addRedstoneWireFamily(tWireShared); // task p10-wire-redstone-family — the 6-block redstone loop
        addLaserWireFamily(tWireShared); // task p10-wire-laser-placeholder — the 1-block laser loop (render target upgraded to the baked fiber model by p11-wire-fiber-texture)
        addOven();
        addMachine(GTMachines.SHREDDER.get(), "shredder"); // task p7-basicmachine-family ④
        addMachine(GTMachines.CRUSHER.get(), "crusher");
        addMachine(GTMachines.LATHE.get(), "lathe");
        // task p8-machine-tiers-doinject ⑧: the T2-T4 ladder, +9 rows (the tier blocks share
        // the T1 front textures — the tier is not a visual state upstream either)
        addMachine(GTMachines.SHREDDER_T2.get(), "shredder_t2", "shredder");
        addMachine(GTMachines.SHREDDER_T3.get(), "shredder_t3", "shredder");
        addMachine(GTMachines.SHREDDER_T4.get(), "shredder_t4", "shredder");
        addMachine(GTMachines.CRUSHER_T2.get(), "crusher_t2", "crusher");
        addMachine(GTMachines.CRUSHER_T3.get(), "crusher_t3", "crusher");
        addMachine(GTMachines.CRUSHER_T4.get(), "crusher_t4", "crusher");
        addMachine(GTMachines.LATHE_T2.get(), "lathe_t2", "lathe");
        addMachine(GTMachines.LATHE_T3.get(), "lathe_t3", "lathe");
        addMachine(GTMachines.LATHE_T4.get(), "lathe_t4", "lathe");
        addDryer(); // task p14-dryer-family
        addDistillery(); // task p16-distillery-family
        addCanner(); // task p24-canner-machine
        addKineticTrio(); // task p26-w1-sifter-compressor-wiremill
        addPress(); // task p26-w1-press-extruder-molds
        addExtruder(); // task p26-w1-press-extruder-molds
        addUlvLadder(); // task p28-c-ulv-machine-ladder — the six ULV rows (family textures, the addCanner shape)
        addStaticStorages(); // task p26-storage-static-batch
        addAdvancedCraftingTable(); // task p24-act-machine
        // task p21-paintable-tint-render: the datagen-JVM census half — 54 machine blocks x
        // 3 models (the six ULV rows joined at task p28-c-ulv-machine-ladder),
        // matching the paintableBlockArray() client registration census
        // (the offline JUnit half walks the generated tree and pins the same 162; the ACT
        // rides its own single-state model OUTSIDE the paint-array census — the
        // GTAdvancedCraftingTableBlock carries no ACTIVE/RUNNING payload, and the
        // family-wide paint extension stays pooled).
        LOGGER.info("GT6 machine paint tint: {} machine models tinted (54 blocks x 3 + the ACT single-state model, addOven/addMachine/addDryer/addDistillery/addCanner/addKineticTrio/addPress/addExtruder/addUlvLadder/addAdvancedCraftingTable)", mMachineTintModels);
        addMultiBlocks();
        addBarrel();
        addEnergySource();
        addFeBattery(); // task p26-eu-bridge-outbound (tail-append; shared serial file)
        addFeConverter(); // task p28-b-fe-converter-machine (tail-append; shared serial file)
        addFeSource(); // task p28-b-fe-converter-machine (tail-append; shared serial file)
        addTestMachines(); // task p20-testmachine-blockstates — the two dev BE-framework blocks
        addPrefixBlocks(); // task p8-prefixblock-render ①
        addCrank(); // task p12-engine-crank
        addAxles(); // task p12-axle-family
        addAttachments(); // task p12-tap-funnel-attachment
        addSteamEngines(); // task p12-engine-steam
        addDieselEngines(); // task p12-engine-diesel
        addBurningBoxes(); // task p13-burning-box-family
        addBoilers(); // task p13-boiler-tank
        addHoppers(); // task p26-storage-hopper-family
        addGearBoxTransformer(); // task p12-gearbox-transformer
        addElectricTransformer(); // task p28-c-ulv-lv-transformer
        addWaterWheel(); // task p28-c-water-wheel
        addLargeBoiler(); // task p13-large-boiler
        addLightningRod(); // task p24-lightning-rod
        addLargeCrucible(); // task p26-crucible-multiblock
        addStoneBlocks(); // task p21-stoneblocks-16item-registry-split — the 272 per-pair (stone, variant) blocks
        addGrassBlocks(); // task p24-grass-block — the 6 per-pair GT grass variants
        addFoamBlocks(); // task p26-c-foam-block-family — the C-Foam pair + slabs + the owned carrier
        addSensors(); // task p26-sensors-core — the three pioneer sensor blocks
        addAnvils(); // task p28-c-anvil — the stone anvil pair
    }

    /**
     * Task p28-c-anvil — the two stone anvil rows (Loader_MultiTileEntities.java
     * :2185-2186): ONE oriented cube model over the two grayscale placeholders (the
     * upstream stonetype texture has no borrowable source in this repo — the hopper
     * no-borrow precedent; the top face carries the working-surface texture, FACING is
     * HORIZONTAL only — the upstream SIDES_VALID :413 — so the y-mapping is the horizontal
     * band of the boiler form). Both rows share the model. The 2 BlockItem models parent it.
     */
    private void addAnvils() {
        ModelFile tModel = models().cube("gt6_anvil",
                modLoc("block/anvil_top"), modLoc("block/anvil_top"), // bottom/top
                modLoc("block/anvil_side"), modLoc("block/anvil_side"), // north/side rows (the FACING front is not a texture state)
                modLoc("block/anvil_side"), modLoc("block/anvil_side"));
        for (Block tBlock : new Block[] {
                gregtech6.registry.GT6Anvils.STONE_ANVIL.get(),
                gregtech6.registry.GT6Anvils.BLACKSTONE_ANVIL.get()}) {
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.block.tools.GTAnvilBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationX(0).rotationY(tY).build();
            });
        }
        itemModels().withExistingParent("stone_anvil", tModel.getLocation());
        itemModels().withExistingParent("blackstone_anvil", tModel.getLocation());
    }

    /**
     * Task p14-dryer-family — the four Dryer rows (Loader_MultiTileEntities.java
     * :1477-1480, all four NBT_TEXTURE "dryer"): the addMachine texture-base overload —
     * model names per path (dryer/dryer_t2/... own 16-variant blockstates + item
     * parents) while the FRONT TEXTURES stay on the family "dryer" set (the tier is not
     * a visual state upstream either, the p8 ruling — the ladder adds three PNGs total,
     * the placeholder fronts).
     */
    /**
     * Task p26-crucible-multiblock — the LARGE crucible family (the wall "Steel Wall"
     * + the "Large Steel Crucible" controller): placeholder cube models over the boiler
     * wall texture (the machine-wall placeholder convention — no crucible PNG exists;
     * the formed/unformed and the molten-content faces are the declared render defer,
     * the controller blockstate still carries the full 8 FACING×FORMED state coverage).
     */
    private void addLargeCrucible() {
        Block tWall = gregtech6.registry.GT6Crucibles.CRUCIBLE_STEEL_WALL.get();
        simpleBlock(tWall, models().cubeAll("crucible_steel_wall", modLoc("block/large_boiler/wall")));
        itemModels().withExistingParent("crucible_steel_wall", modLoc("block/crucible_steel_wall"));
        for (gregtech6.registry.GT6Crucibles.CrucibleRow tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            Block tBlock = gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cubeAll(tRow.path(), modLoc("block/large_boiler/wall"));
            getVariantBuilder(tBlock).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    private void addDryer() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
            addMachine(GTMachines.DRYER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p16-distillery-family — the four Distillery rows (Loader_MultiTileEntities.java
     * :1398-1401, all four NBT_TEXTURE "distillery"): the addDryer shape verbatim —
     * model names per path, the FRONT TEXTURES stay on the family "distillery" set
     * (three placeholder PNGs total, the p14 dryer ruling).
     */
    private void addDistillery() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
            addMachine(GTMachines.DISTILLERY_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p24-canner-machine — the four Canner rows (Loader_MultiTileEntities.java
     * :1379-1382, all four NBT_TEXTURE "canner"): the addDistillery shape verbatim —
     * model names per path, the FRONT TEXTURES stay on the family "canner" set (the
     * p22 split-front borrow: canner_colored_front + the three overlay decals).
     */
    private void addCanner() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) {
            addMachine(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio (Sifter/Compressor/
     * Wiremill rows, Loader_MultiTileEntities.java :1312-1315/:1343-1346/:1373-1376, all
     * rows NBT_TEXTURE "sifter"/"compressor"/"wiremill" per family): the addCanner shape
     * verbatim — model names per path, the FRONT TEXTURES stay on the family set (the
     * split-front borrow: {family}_colored_front + the three overlay decals each).
     */
    private void addKineticTrio() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ROWS) {
            addMachine(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.COMPRESSOR_ROWS) {
            addMachine(GTMachines.COMPRESSOR_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ROWS) {
            addMachine(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-press-extruder-molds — the four Press rows (Loader_MultiTileEntities.java
     * :1425-1428, all four NBT_TEXTURE "press"): the addCanner shape verbatim — model names
     * per path, the FRONT TEXTURES stay on the family "press" set (the borrowed upstream
     * basicmachines/press fronts, the p22 split-front borrow).
     */
    private void addPress() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESS_ROWS) {
            addMachine(GTMachines.PRESS_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-w1-press-extruder-molds — the four Extruder rows (Loader_MultiTileEntities
     * .java:1406-1409, all four NBT_TEXTURE "extruder"): the addPress shape verbatim (the
     * borrowed upstream basicmachines/extruder fronts).
     */
    private void addExtruder() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.EXTRUDER_ROWS) {
            addMachine(GTMachines.EXTRUDER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p28-c-ulv-machine-ladder — the six ULV rows (the five family extension rows +
     * the Rolling Mill rung, all NBT_TEXTURE riding their family/upstream tokens
     * "shredder"/"crusher"/"canner"/"sifter"/"wiremill"/"rollingmill"): the addCanner
     * shape verbatim — model names per path, the FRONT TEXTURES stay on the family sets
     * (the rollingmill fronts are the borrowed upstream basicmachines/rollingmill split,
     * the p22 borrow pipeline).
     */
    private void addUlvLadder() {
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SHREDDER_ULV_ROWS) {
            addMachine(GTMachines.SHREDDER_ULV.get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CRUSHER_ULV_ROWS) {
            addMachine(GTMachines.CRUSHER_ULV.get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ULV_ROWS) {
            addMachine(GTMachines.CANNER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.SIFTER_ULV_ROWS) {
            addMachine(GTMachines.SIFTER_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.WIREMILL_ULV_ROWS) {
            addMachine(GTMachines.WIREMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : GTMachines.ROLLINGMILL_ROWS) {
            addMachine(GTMachines.ROLLINGMILL_BLOCKS_BY_PATH.get(tRow.path()).get(), tRow.path(), tRow.texture());
        }
    }

    /**
     * Task p26-storage-static-batch — the 28 static storage rows (GT6StaticStorages.ROWS):
     * ONE oriented cube model per KIND over the placeholder front/side PNG pair (the
     * script-generated placeholder ruling — no upstream borrowable iconset in this repo),
     * the FRONT face carries the kind's front texture, the FACING (horizontal) drives the
     * 4-variant y-rotation (the ACT single-state arm; north default, south 180, west 270,
     * east 90). All six kinds share the model within the kind (the material/plank ladder
     * folds to the row, the same placeholder per material — the p20 placeholder ruling).
     * The 28 BlockItem models parent the kind model.
     */
    private void addStaticStorages() {
        for (gregtech6.registry.GT6StaticStorages.Kind tKind : gregtech6.registry.GT6StaticStorages.Kind.values()) {
            String tTex = "block/" + kindModelName(tKind) + "_";
            ModelFile tModel = models().cube("gt6_" + kindModelName(tKind),
                    modLoc(tTex + "side"), modLoc(tTex + "side"),        // bottom/top
                    modLoc(tTex + "front"), modLoc(tTex + "side"),       // north(front)/south
                    modLoc(tTex + "side"), modLoc(tTex + "side"));       // west/east
            for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
                if (tRow.kind() != tKind) continue;
                Block tBlock = gregtech6.registry.GT6StaticStorages.BLOCKS_BY_PATH.get(tRow.path()).get();
                getVariantBuilder(tBlock).forAllStates(aState -> {
                    int tY;
                    switch (aState.getValue(gregtech6.registry.GT6StaticStorages.GT6StorageBlock.FACING)) {
                        case SOUTH -> tY = 180;
                        case WEST -> tY = 270;
                        case EAST -> tY = 90;
                        default -> tY = 0; // NORTH
                    }
                    return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
                });
                itemModels().withExistingParent(tRow.path(), tModel.getLocation());
            }
        }
    }

    /** The texture/model stem of a storage kind (the PNG pair naming). */
    private static String kindModelName(gregtech6.registry.GT6StaticStorages.Kind aKind) {
        return switch (aKind) {
            case LOCKER -> "locker";
            case DRAWER -> "drawer";
            case SAFE_MECHANICAL, SAFE_KEYLOCKED -> "safe";
            case BOOKSHELF -> "bookshelf";
            case BOTTLECRATE -> "bottlecrate";
        };
    }

    /**
     * Task p24-act-machine — the Advanced Crafting Table (Loader_MultiTileEntities.java
     * :136, the single-variant row): FACING-ONLY blockstate (the upstream machine has no
     * ACTIVE/RUNNING visual payload — the craftingtables/advanced texture group ships no
     * overlay_active/overlay_running layers, the borrow-or-declare rule landed exactly
     * the two borrowable fronts), 4 facing variants over ONE tinted machine model
     * (machineModel = the shared oven body + the advanced_colored/overlay fronts), and
     * the BlockItem parent. The model joins the tint census count but NOT the
     * paintableBlockArray (the family-wide paint extension stays pooled, the class doc
     * of the block).
     */
    private void addAdvancedCraftingTable() {
        Block tBlock = GTMachines.ADVANCED_CRAFTING_TABLE.get();
        ModelFile tModel = machineModel("advanced_crafting_table", "advanced_colored_front", "advanced_overlay_front");
        getVariantBuilder(tBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(GTOvenBlock.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("advanced_crafting_table", modLoc("block/advanced_crafting_table"));
    }

    /**
     * Task p4-multiblock-framework (W3 provider order 1: multiblock→barrel→cover): the FORMED
     * blockstate fallback rendering (spec ⑧) — the Coke Oven controller carries the base-owned
     * FACING + FORMED properties (TileEntityBase10MultiBlockBase :188-189 bit-3 replacement),
     * 4 facings x 2 formed = 8 variants over two cube models.
     *
     * <p>Erratum (task p9-render-d-formed-look; the earlier "mirrors the upstream getTexture2
     * mStructureOkay pick" wording here was wrong): upstream getTexture2
     * (TileEntityBase10MultiBlockBase.java:192-194) picks the front-vs-side texture GROUPS by
     * {@code aSide == mFacing} over a colored+overlay two-layer stack, and NO upstream
     * consumer picks the front/side texture groups by mStructureOkay — every getTexture2,
     * including the Crucible's own (MultiTileEntityCrucible.java:643-651), keys the group on
     * {@code aSide == mFacing}. The only known VISUAL mStructureOkay consumers are the render
     * pass count (MultiTileEntityLargeTurbine.getRenderPasses2 :109-111, formed renders an
     * extra pass) and the render bounds (MultiTileEntityCrucible.setBlockBounds2 :628-638) —
     * neither touches the texture groups. The formed/unformed dual model below is therefore
     * a DECLARED this-port enhancement beyond upstream, kept as-is: the FORMED
     * blockstate is the RCON {@code execute if block ...[formed=true]} assertion surface, and
     * the GTCEu IS_FORMED ModelProperty technique is not adopted (zero-benefit refactor,
     * ADR 2026-09-01-p9-render-d-formed-look).
     *
     * <p>Texture census (same task, negative): upstream ships NO
     * {@code machines/multiblockmains/cokeoven/} PNG group at all — the colored/overlay/
     * colored_front/overlay_front icon paths the upstream controller registers
     * (TileEntityBase10MultiBlockBase.java:66-81, NBT_TEXTURE "cokeoven",
     * Loader_MultiTileEntities.java:1193) are missing resources in the upstream snapshot, so
     * there is nothing to borrow. Per the borrow-or-declare rule nothing was redrawn: the
     * script-generated placeholder PNGs stay (see assets README). The bricks part is a plain
     * cube_all (no properties).
     */
    private void addMultiBlocks() {
        Block tCokeOven = GTMultiBlocks.COKE_OVEN.get();
        ModelFile tUnformed = cokeOvenModel("multiblock_coke_oven", "multiblock_coke_oven_front");
        ModelFile tFormed = cokeOvenModel("multiblock_coke_oven_formed", "multiblock_coke_oven_front_formed");
        getVariantBuilder(tCokeOven).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            ModelFile tModel = aState.getValue(TileEntityBase10MultiBlockBase.FORMED) ? tFormed : tUnformed;
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent("multiblock_coke_oven", modLoc("block/multiblock_coke_oven"));

        Block tBricks = GTMultiBlocks.COKE_OVEN_BRICKS.get();
        simpleBlock(tBricks, models().cubeAll("multiblock_coke_oven_bricks", modLoc("block/multiblock_coke_oven_bricks")));
        itemModels().withExistingParent("multiblock_coke_oven_bricks", modLoc("block/multiblock_coke_oven_bricks"));
    }

    /** One cube model over the four-texture key set: down/up/north(front)/south+east+west(side). */
    private ModelFile cokeOvenModel(String aName, String aFrontTexture) {
        return models().cube(aName,
                modLoc("block/multiblock_coke_oven_bottom"), modLoc("block/multiblock_coke_oven_top"),
                modLoc("block/" + aFrontTexture), modLoc("block/multiblock_coke_oven_side"),
                modLoc("block/multiblock_coke_oven_side"), modLoc("block/multiblock_coke_oven_side"));
    }

    /**
     * Task p4-fluid-barrel (W3 provider addition, merge order multiblock→barrel→cover),
     * extended by task p6-barrel-metal-plastic: the three-barrel family (wood/plastic/
     * metal) over their {@code gt6:textures/block/barrel_<material>.png} (the barrel
     * TESR/lid is a feature-layer omission, MultiTileEntityBarrelWood.java:44-54) plus
     * the BlockItem model parenting the block model. The textures are script-generated
     * placeholder PNGs, not JSON.
     *
     * <p>Task p7-barrel-high-tier-melt-bridge spec ④: the twelve high-tier metal drums
     * (Loader_MultiTileEntities.java:2159-2170) share the ONE {@code barrel_metal.png} —
     * every model JSON references the same PNG, so the model count grows with the rows
     * and the PNG count does not.
     *
     * <p>Task p23-barrel-paint-render: the former plain {@code cube_all} placeholder
     * becomes the {@link #tintedCubeAll} single-element form (tintindex 0 on every face,
     * the p21 machine-model grammar over the barrel's ONE-texture shortcut) — the
     * upstream barrel renders its {@code colored/} texture multiplied by mRGBa
     * (MultiTileEntityBarrelWood.java:42-55 {@code new BlockTextureDefault(tTex, mRGBa)};
     * Plastic:42 / Metal:39 / Logistics:45 isomorphic), and the port's placeholder PNGs
     * are full-grayscale (verified), so whole-barrel single-element tinting IS the
     * colored/ layer equivalence. Unpainted barrels ride the {@code -1} white-multiply
     * identity sentinel exactly like the machine face (P21: 0xFFFFFFFF ≡ vanilla no-tint).
     * DECLARED DEVIATION (pool): upstream is TWO-layer — the {@code overlay/} decal
     * texture renders UNCOLOURED on top (BlockTextureMulti); the port borrows a single
     * texture per barrel and has no overlay decal pool yet, so v1 tints the whole
     * barrel with no decal layer (the p22 front-overlay-split precedent for the
     * two-layer follow-up).
     */
    private void addBarrel() {
        addBarrel(GTBarrels.BARREL.get());
        addBarrel(GTBarrels.BARREL_PLASTIC.get());
        addBarrel(GTBarrels.BARREL_METAL.get());
        addBarrel(GTBarrels.BARREL_LOGISTICS.get()); // task p12-barrel-keepfilter-logistics — the :2171 row, own PNG
        for (var tDrum : GTBarrels.METAL_DRUM_BLOCKS.values())
            addBarrel(tDrum.get(), "barrel_metal");
        // task p23-barrel-paint-render: the datagen-JVM census half — 16 barrel blocks,
        // matching the GTBarrels.paintableBlockArray() client registration census (the
        // offline JUnit half walks the generated tree and pins the same 16).
        LOGGER.info("GT6 barrel paint tint: {} barrel models tinted (4 rows + 12 high-tier drums, addBarrel)", mBarrelTintModels);
    }

    /** One tinted cube_all barrel + its BlockItem parent (the p4 wood barrel shape, reused per material row). */
    private void addBarrel(Block aBarrel) {
        addBarrel(aBarrel, aBarrel.getDescriptionId().replace("block.gt6.", ""));
    }

    /** The same shape over an explicit texture tail (the p7 shared-PNG drum family form). */
    private void addBarrel(Block aBarrel, String aTexture) {
        String tName = aBarrel.getDescriptionId().replace("block.gt6.", "");
        // the full "block/..." model path: getBuilder skips the folder prefix for
        // "/"-bearing names (ModelProvider.extendWithFolder — the addPrefixBlocks note).
        simpleBlock(aBarrel, tintedCubeAll("block/" + tName, modLoc("block/" + aTexture)));
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
        mBarrelTintModels++;
    }

    /**
     * Task p4-machine-oven (W2-exclusive provider addition), generalized by task
     * p7-basicmachine-family into {@link #addMachine(Block, String)}: the A-tier machine
     * rendering — a pure datagen blockstate over 4 horizontal facings x 2 active x 2
     * running = 16 variants (HORIZONTAL_FACING, A-tier furnace idiom). Three
     * models (inactive/active/running), each a {@code cube} with the four-texture key set
     * (top/bottom/side/front, spec 8): the front texture is the state carrier, mirroring the
     * upstream getTexture2 overlay pick (MultiTileEntityBasicMachine.java:1014, mActive →
     * mTexturesActive : mRunning → mTexturesRunning : mTexturesInactive); the y rotation maps
     * the FACING property (model-space north = front). Textures are script-generated
     * placeholder PNGs, not JSON.
     */
    private void addOven() {
        addMachine(GTMachines.OVEN.get(), "oven");
        // task p27-oven-heat-t-ladder — the Heat_T ladder rows through the p8 texture-base
        // overload: the tier models derive from aBase (oven_t2/_active/_running) while the
        // FRONT TEXTURES stay on the family T1 "oven" set (upstream NBT_TEXTURE "oven" on
        // all four rows :1288-1291) — the ladder adds zero PNGs.
        addMachine(GTMachines.OVEN_T2.get(), "oven_t2", "oven");
        addMachine(GTMachines.OVEN_T3.get(), "oven_t3", "oven");
        addMachine(GTMachines.OVEN_T4.get(), "oven_t4", "oven");
    }

    /**
     * The addOven generalization (task p7-basicmachine-family ④): one {@code aBase} machine
     * = three models ({@code aBase}, {@code aBase_active}, {@code aBase_running}), the
     * 16-variant blockstate, and the BlockItem model parenting the block model. Since task
     * p28-b-port-overlay-render the three models come from {@link #familyMachineModel} —
     * the family colored six-face body plus the six static state decals (the upstream
     * :1014 two-layer form over the full upstream texture arrays :176-203, not the
     * front-only split the p22 borrow started from).
     *
     * <p>Property identity: both GTOvenBlock.FACING and GTBasicMachineBlock.FACING are the
     * BlockStateProperties.HORIZONTAL_FACING instance, and GTOvenBlock.ACTIVE/RUNNING and
     * GTBasicMachineBlock.ACTIVE/RUNNING are aliases of the same GTBlockProperties single
     * instances (ADR-P16-2) — so the GTOvenBlock property reads below cover the machine
     * blocks too. (The old "BooleanProperty interning" wording here was a wrong theory:
     * vanilla never interned by name; 1.20.1 matched same-named foreign instances only by
     * value equality inside StateHolder, which 1.21.x replaced with identity lookup —
     * distinct instances crash there, hence the single-owner aliases.)
     */
    private void addMachine(Block aBlock, String aBase) {
        addMachine(aBlock, aBase, aBase);
    }

    /**
     * The texture-base overload (task p8-machine-tiers-doinject ⑧): the model names derive
     * from {@code aBase} (so shredder_t2 gets shredder_t2/_active/_running models + its own
     * 16-variant blockstate + the item parent) while the TEXTURES stay on the family's
     * T1 set ({@code aTextureBase_colored_*} + the {@code aTextureBase_overlay_*} state
     * trio, task p22-paint-front-overlay-split extended to all six faces by task
     * p28-b-port-overlay-render) — the tier is not a visual state upstream (the rows
     * :1294-1309 share the NBT_TEXTURE per family), so the ladder adds zero PNGs.
     */
    private void addMachine(Block aBlock, String aBase, String aTextureBase) {
        ModelFile tInactive = familyMachineModel(aBase, aTextureBase, "");
        ModelFile tActive = familyMachineModel(aBase + "_active", aTextureBase, "_active");
        ModelFile tRunning = familyMachineModel(aBase + "_running", aTextureBase, "_running");
        getVariantBuilder(aBlock).forAllStates(aState -> {
            int tY;
            switch (aState.getValue(GTOvenBlock.FACING)) {
                case SOUTH -> tY = 180;
                case WEST -> tY = 270;
                case EAST -> tY = 90;
                default -> tY = 0; // NORTH
            }
            ModelFile tModel = aState.getValue(GTOvenBlock.ACTIVE) ? tActive
                    : aState.getValue(GTOvenBlock.RUNNING) ? tRunning : tInactive;
            return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
        });
        itemModels().withExistingParent(aBase, modLoc("block/" + aBase));
    }

    /**
     * One cube model over the four-texture key set: down/up/north(front)/south+east+west(side).
     * The p22 two-element form. ACT-only since task p28-b-port-overlay-render (the
     * Advanced Crafting Table is the one machine family with no borrowed side art — the
     * craftingtables/advanced upstream group ships fronts only — so it keeps the shared
     * oven placeholder body and the single front decal; every addMachine family moved to
     * {@link #familyMachineModel}).
     *
     * <p>Task p21-paintable-tint-render: the vanilla {@code block/cube} element is re-declared
     * in the child with {@code tintindex 0} on EVERY face — the machine cube is six-texture,
     * so the {@link #tintedCubeAll} {@code #all} shortcut does not apply and the per-face
     * element form is required. Upstream canonical: every faced face multiplies the
     * grayscale texture by mRGBa (MultiTileEntityBasicMachine.java:1014 getTexture2,
     * white = unpainted = unchanged), so all three models (inactive/active/running — built
     * from {@link #addMachine}) tint identically; the runtime consumer is the
     * {@code GTMachinePaintTint} BlockColor. Output is otherwise equivalent to the former
     * parent-only {@code models().cube} form (no explicit UVs — they default to the element
     * bounds; the {@code block/cube} parent keeps the display transforms and its
     * {@code particle = #down} binding).
     *
     * <p>Task p22-paint-front-overlay-split: the former single BAKED front composite
     * (colored base + state overlay flattened, the P20 bake) is retired for the upstream
     * TWO-LAYER form (:1014 = BlockTextureMulti(BlockTextureDefault(colored, mRGBa),
     * BlockTextureDefault(state overlay)) with the second layer UNCOLOURED,
     * BlockTextureDefault.java:179-180 — the state decal is never tinted by the paint).
     * The north body face now carries the plain grayscale {@code aFrontTexture}
     * ({@code <family>_colored_front}), and a second thin element — 16x16x0.01 floating
     * 0.01 north of the body plane (inside the vanilla [-16,32] element tolerance) —
     * carries the state decal {@code aOverlayTexture} with NO tintindex (FaceBuilder's
     * default -1 omits the key), so {@code GTMachinePaintTint} (which only maps
     * tintindex 0) cannot re-tint it. The decal's single north face keeps
     * {@code cullface north}, mirroring the body cube's own north cullface: a solid
     * neighbor to the north hides body face and decal together (no decal floating
     * behind a wall), and the 0.01 offset keeps the decal off the body plane (no
     * coplanar z-fighting). The old baked fronts are retired in assets/README.md.
     */
    private ModelFile machineModel(String aName, String aFrontTexture, String aOverlayTexture) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/oven_bottom"))
                .texture("up", modLoc("block/oven_top"))
                .texture("north", modLoc("block/" + aFrontTexture))
                .texture("south", modLoc("block/oven_side"))
                .texture("west", modLoc("block/oven_side"))
                .texture("east", modLoc("block/oven_side"))
                .texture("overlay", modLoc("block/" + aOverlayTexture));
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        tModel.element()
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay").cullface(Direction.NORTH)
                .end();
        mMachineTintModels++;
        return tModel;
    }

    /**
     * Task p28-b-port-overlay-render — the full-family machine model over the borrowed
     * upstream basicmachines texture arrays (MultiTileEntityBasicMachine.java:176-203:
     * mTexturesMaterial + the mTexturesInactive/Active/Running trio, each the six-entry
     * array [bottom, top, left, front, right, back]). The upstream :1014 two-layer form
     * over ALL six faces, not the p22 front-only split:
     * <ul>
     * <li><b>body (element 0)</b> — the full 0..16 cube, every face tintindex 0 (the
     *     mRGBa paint seat, unchanged from p21), bound to the family's OWN colored art
     *     ({@code <family>_colored_bottom/top/front/back/left/right}) instead of the shared
     *     oven placeholders. The side art is no longer the oven's: every family ships its
     *     own colored six-set upstream (the A-card borrow, assets/README.md).</li>
     * <li><b>six state decals (elements 1-6)</b> — thin 0.01 plates floating 0.01 outside
     *     each face (the p22 front-decal geometry generalized), each a single face with NO
     *     tintindex (the UNCOLOURED second layer, BlockTextureDefault.java:179-180) and
     *     cullface synced with the body's own face (the p22 anti-z-fight + cull pairing).
     *     The decal state trio is selected per model by {@code aStateSuffix} — "" /
     *     {@code _active} / {@code _running} = the upstream :1014 pick
     *     {@code (mActive ? mTexturesActive : mRunning ? mTexturesRunning : mTexturesInactive)}
     *     — driven purely by the existing ACTIVE/RUNNING blockstate variants (no new
     *     state, no BE read: the port art is statically baked, the census ruling).</li>
     * </ul>
     *
     * <p>Face mapping: the texture keys are named by the UPSTREAM art token
     * ({@code overlay_front/back/left/right/top/bottom}), the model faces map per the
     * upstream FACING_ROTATIONS table (CS.java:528-537) with the model-space front at
     * north: for a north-facing machine the table binds west→right and east→left, so the
     * west face carries {@code #overlay_right}/{@code _colored_right} and the east face
     * {@code #overlay_left}/{@code _colored_left}; the blockstate y rotations reproduce
     * the remaining facings exactly (the same mapping the GTOvenOverlayModel
     * textureFaceOf table mirrors at runtime). UVs stay the vanilla defaults (the p22
     * precedent); the 1.7.10 "stupidly mirrored" north/east icon quirk (CS.java:516-518)
     * is NOT compensated — the side-decal orientations join the P28 runClient目验池.
     *
     * <p>Oven interaction note: the P9 GTOvenOverlayModel wraps the oven ladder's baked
     * models and stacks its own snapshot-driven cutout overlay quads on the solid layer —
     * with these static decals the active/running oven draws the same upstream art twice
     * (byte-same source PNGs, 0.002 vs 0.01 offsets). Declared overlap: retiring the P9
     * dynamic layer is a separate card's call (render code, out of this card's scope).
     */
    private ModelFile familyMachineModel(String aName, String aTextureBase, String aStateSuffix) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/cube")))
                .texture("down", modLoc("block/" + aTextureBase + "_colored_bottom"))
                .texture("up", modLoc("block/" + aTextureBase + "_colored_top"))
                .texture("north", modLoc("block/" + aTextureBase + "_colored_front"))
                .texture("south", modLoc("block/" + aTextureBase + "_colored_back"))
                .texture("west", modLoc("block/" + aTextureBase + "_colored_right")) // FACING_ROTATIONS[north][west]=4=right
                .texture("east", modLoc("block/" + aTextureBase + "_colored_left")) // FACING_ROTATIONS[north][east]=2=left
                .texture("overlay_front", modLoc("block/" + aTextureBase + "_overlay_front" + aStateSuffix))
                .texture("overlay_back", modLoc("block/" + aTextureBase + "_overlay_back" + aStateSuffix))
                .texture("overlay_left", modLoc("block/" + aTextureBase + "_overlay_left" + aStateSuffix))
                .texture("overlay_right", modLoc("block/" + aTextureBase + "_overlay_right" + aStateSuffix))
                .texture("overlay_top", modLoc("block/" + aTextureBase + "_overlay_top" + aStateSuffix))
                .texture("overlay_bottom", modLoc("block/" + aTextureBase + "_overlay_bottom" + aStateSuffix));
        // element 0 — the tinted body cube (p21/p22 shape, only the texture bindings changed).
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        // elements 1-6 — the six state decals: one thin plate per face, the p22 front-decal
        // form generalized (0.01 offset out, single face, no tintindex, cullface synced).
        tModel.element() // front (north)
                .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                .face(Direction.NORTH).texture("#overlay_front").cullface(Direction.NORTH)
                .end();
        tModel.element() // back (south)
                .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                .face(Direction.SOUTH).texture("#overlay_back").cullface(Direction.SOUTH)
                .end();
        tModel.element() // left art (east face — FACING_ROTATIONS[north][east]=2=left)
                .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                .face(Direction.EAST).texture("#overlay_left").cullface(Direction.EAST)
                .end();
        tModel.element() // right art (west face — FACING_ROTATIONS[north][west]=4=right)
                .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                .face(Direction.WEST).texture("#overlay_right").cullface(Direction.WEST)
                .end();
        tModel.element() // bottom (down)
                .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                .face(Direction.DOWN).texture("#overlay_bottom").cullface(Direction.DOWN)
                .end();
        tModel.element() // top (up)
                .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                .face(Direction.UP).texture("#overlay_top").cullface(Direction.UP)
                .end();
        mMachineTintModels++;
        return tModel;
    }

    /**
     * One cube_all model per item pipe row (the blockstate name mirrors the block registry
     * path), a variant per CONNECTIONS mask value (0..63), and the BlockItem model
     * parenting the block model — the addFluidPipe shape over the two shared placeholders.
     */
    private void addItemPipe(Block aPipe, boolean aRestrictive) {
        String tName = aPipe.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc(aRestrictive ? "block/item_pipe_restrictive" : "block/item_pipe"));
        getVariantBuilder(aPipe).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * One cube_all model per pipe tier (the blockstate name mirrors the block registry
     * path), a variant per CONNECTIONS mask value (0..63), and the BlockItem model
     * parenting the block model.
     */
    private void addFluidPipe(Block aPipe) {
        String tName = aPipe.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc("block/fluid_pipe_wood"));
        getVariantBuilder(aPipe).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * Task p7-d2-cable spec ⑥ — the electric wires, the pipe section shape: one shared
     * cube_all model over {@code gt6:textures/block/wire_electric.png} (both variants
     * reference the same PNG, the p7 shared-PNG drum family form) with a variant per
     * {@link gregtech6.block.wire.GTWireBlock} CONNECTIONS mask value (0..63, the 6-bit
     * connection state — the 64-variant exhaustive listing comes out of forAllStates, no
     * hand-written JSON) plus the BlockItem model parenting the block model. The texture
     * is an inline-script generated placeholder PNG, not JSON.
     */
    private void addWire(Block aWire) {
        String tName = aWire.getDescriptionId().replace("block.gt6.", "");
        var tModel = models().cubeAll(tName, modLoc("block/wire_electric"));
        getVariantBuilder(aWire).forAllStates(aState -> ConfiguredModel.builder().modelFile(tModel).build());
        itemModels().withExistingParent(tName, modLoc("block/" + tName));
    }

    /**
     * Task p9-wire-family-w1 spec ⑥, MODEL TARGET UPGRADED by task p9-wire-family-w2
     * (the card's "swap the model target to GTWireBakedModel + textured models"): the 620-block wire family,
     * looped over the {@link GTWireSpecs} table. The blockstate form is UNCHANGED from W1 —
     * per block ONE blockstate JSON whose SINGLE property-less variant (the {@code ""} key,
     * emitted by {@code partialState().setModels}) maps ALL 64
     * {@link gregtech6.block.wire.GTWireBlock} CONNECTIONS states onto a shared model
     * (vanilla resolves an empty variant key as no predicates = a wildcard over every
     * state, ModelBakery.java:173). What changed is the shared model TARGET: instead of the
     * W1 placeholder cube, the target is one SHARED tinted model per live texture set
     * (the addPrefixBlocks merge rule — the distinct {@code materialicons/<set>/wire.png}
     * first entries across the 30 wire rows; census 2026-09-01: exactly
     * {@code copper, shiny, metallic, dull, quartz, rad, none} — 7 sets, "none" =
     * upstream SET_NONE for the empty-list Superconductor row, every PNG borrowed
     * byte-identical, assets/README.md). The shared models carry {@code tintindex 0} (the
     * runtime {@link gregtech6.client.wire.GTWireTint} material dye); the true
     * connection-aware geometry (core + arms) is installed at bake time by
     * {@link gregtech6.client.wire.GTWireClientListener}, which replaces the per-state
     * AND item baked keys — this JSON model stays as the fallback carrier and the item
     * parent. A cable's shared fallback shows no insulation shell (the tier texture is
     * per-diameter) — a fallback-only simplification, the world form is the baked model.
     * Still NO 64-variant x 620 listing (the ADR red line).
     */
    private void addWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.FAMILY_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 wire family: {} blockstates over {} shared (set) models",
                GTWireSpecs.EXPECTED_VARIANTS, aShared.size());
    }

    /**
     * Task p10-wire-redstone-family — the redstone-wire family (6 blocks over
     * {@link GTWireSpecs#REDSTONE_ROWS}), the addWireFamily pipe over the redstone variant
     * list. The three materials (RedAlloy/Signalum/Lumium) all resolve to the
     * {@code copper} texture set (clloy/clloymachine construct with SET_COPPER —
     * MT.java:697/701), which the W2 borrow already shipped, so zero new PNGs. The same
     * single property-less variant wildcard maps the 64 CONNECTIONS states per block onto
     * the shared tinted model.
     *
     * <p>Task p11-wire-brightness UPGRADED the runtime target (the fiber-card shape): the
     * listener table ({@link gregtech6.client.wire.GTWireClientListener#buildParams}) now
     * covers the six redstone paths, so every per-state key
     * {@code gt6:wire_red_alloy#connections=0..63} et al AND the item keys bake into the
     * {@link gregtech6.client.wire.GTWireBakedModel} electric form — the block no longer
     * RENDERS through this JSON. The shared tinted cube stays generated UNCHANGED as the
     * per-state key carrier + item parent (spec-pinned: the blockstate's single wildcard
     * variant is what mints the 64 per-state keys the listener swaps), and as the baked
     * fallback safety net if the listener never fires. The family's render specifics live
     * elsewhere: the jacket colour is the per-family tint ({@code 96,64,64},
     * MultiTileEntityWireRedstoneInsulated :184-185, in gregtech6.client.wire.GTWireTint)
     * and the bare-wire {@code mState > 0} texture-fullbright (:81-82) is the declared
     * GTWireBakedModel deviation.
     */
    private void addRedstoneWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.REDSTONE_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 redstone wire family: {} blockstates over the shared (set) model pool",
                GTWireSpecs.EXPECTED_REDSTONE_VARIANTS);
    }

    /**
     * Task p10-wire-laser-placeholder — the laser-wire family (1 block over
     * {@link GTWireSpecs#LASER_ROWS}), the addWireFamily pipe over the laser variant list.
     * The row material is MT.NULL (upstream NBT_MATERIAL MT.NULL, Loader:1815), which
     * resolves to the {@code none} set (GTWireTextures.blockSetOf empty-list rule) — the
     * W2 borrow already shipped that set (the Superconductor row), so zero new PNGs.
     *
     * <p>Task p11-wire-fiber-texture UPGRADED the runtime target: the listener table
     * ({@link gregtech6.client.wire.GTWireClientListener#buildParams}) now covers
     * {@code wire_laser}, so every per-state key AND the item key bake into the
     * {@link gregtech6.client.wire.GTWireBakedModel} fiber form (the fixed FIBER_WIRE
     * + FIBER_WIRE_OVERLAY pair, MultiTileEntityWireLaser :121-122) — the block no longer
     * RENDERS through this JSON. The shared tinted cube stays generated UNCHANGED as the
     * per-state key carrier + item parent (the addWireFamily architecture, spec-pinned:
     * the blockstate's single wildcard variant is what mints the 64 per-state
     * {@code gt6:wire_laser#connections=N} keys the listener swaps), and as the baked
     * fallback safety net if the listener never fires. This is the exact shape the 620
     * electric rows and — since task p11-wire-brightness — the 6 redstone rows have;
     * every wire family now renders through the per-state baked model.
     */
    private void addLaserWireFamily(Map<String, ModelFile> aShared) {
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) {
            String tName = GTWireSpecs.registryName(tVariant);
            String tSet = blockSetOf(tVariant.row().material().get());
            String tModelName = "block/materialicons/" + tSet + "/wire";
            ModelFile tModel = aShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSet + "/wire")));
            getVariantBuilder(GTWires.LASER_BY_NAME.get(tName).get())
                    .partialState().setModels(new ConfiguredModel(tModel));
            itemModels().withExistingParent(tName, modLoc(tModelName));
        }
        LOGGER.info("GT6 laser wire family: {} blockstates over the shared (set) model pool",
                GTWireSpecs.EXPECTED_LASER_VARIANTS);
    }

    /**
     * Task p8-d4-energy-source spec ② — the test energy source: one cube_all over
     * {@code gt6:textures/block/energy_source.png} (the borrowed upstream
     * solarpanel_electric_8eu side texture, the p7-gui-family byte-identical form) plus
     * the BlockItem model parenting the block model. No properties, a single variant.
     */
    private void addEnergySource() {
        Block tSource = GTEnergySources.ENERGY_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("energy_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("energy_source", modLoc("block/energy_source"));
    }

    /**
     * Task p26-eu-bridge-outbound (TAIL-APPENDED row, the shared serial file) — the FE
     * battery fixture: the addEnergySource shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture the p20 testmachine rows pinned), plus the BlockItem model parenting the block
     * model. No properties, a single variant.
     */
    private void addFeBattery() {
        Block tBattery = GT6FeBatteries.FE_BATTERY.get();
        simpleBlock(tBattery, models().cubeAll("fe_battery", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_battery", modLoc("block/fe_battery"));
    }

    /**
     * Task p28-b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * ULV FE→EU converter: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder {@code gt6:textures/block/energy_source.png} (no new PNG — the borrow
     * posture), plus the BlockItem model parenting the block model. No properties, a
     * single variant.
     */
    private void addFeConverter() {
        Block tConverter = GT6FeConverters.FE_CONVERTER.get();
        simpleBlock(tConverter, models().cubeAll("fe_converter", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_converter", modLoc("block/fe_converter"));
    }

    /**
     * Task p28-b-fe-converter-machine (TAIL-APPENDED row, the shared serial file) — the
     * FE source fixture: the addFeBattery shape verbatim, one cube_all over the SHARED
     * placeholder, plus the BlockItem model parenting the block model.
     */
    private void addFeSource() {
        Block tSource = GT6FeBatteries.FE_SOURCE.get();
        simpleBlock(tSource, models().cubeAll("fe_source", modLoc("block/energy_source")));
        itemModels().withExistingParent("fe_source", modLoc("block/fe_source"));
    }

    /**
     * Task p20-testmachine-blockstates — the two dev BE-framework blocks (task p3-be-framework,
     * {@link GTBlockEntities#TEST_MACHINE} / {@link GTBlockEntities#TEST_MACHINE_IDLE}): the last
     * model-less blocks in the registry face (P20 texture census, research card
     * tasks.p20-research-texture-census — every other registered blockstate+item model was
     * already zero-gap). The addEnergySource shape verbatim: one cube_all per block over the
     * SHARED placeholder {@code gt6:textures/block/example_chest.png} (no new PNG per the card
     * scope — the placeholder-to-upstream art swap stays a P20 wave item; the census pin d
     * resolves the layer0 against the static tree), plus the item model parenting the block
     * model. Datagen-only JSON: the dev blocks register no BlockItem (the census "dev blocks
     * have no item" note), the item model row merely closes the model-resolution loop the way
     * TestMachineBlock.java:21-22 expected when it deferred this datagen to the example machine
     * card. No properties, a single variant each — the ticking/idle split is the BE ticker
     * (TestMachineBlock.java:44-60), not a blockstate.
     */
    private void addTestMachines() {
        simpleBlock(GTBlockEntities.TEST_MACHINE.get(),
                models().cubeAll("test_machine", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine", modLoc("block/test_machine"));
        simpleBlock(GTBlockEntities.TEST_MACHINE_IDLE.get(),
                models().cubeAll("test_machine_idle", modLoc("block/example_chest")));
        itemModels().withExistingParent("test_machine_idle", modLoc("block/test_machine_idle"));
    }

    /**
     * Task p12-engine-crank — the Hand Crank: one cube_all over
     * {@code gt6:textures/block/crank.png} (the borrowed upstream crank front icon, the
     * energy_source byte-identical-borrow form; assets/README.md attribution), single
     * model for every state — the FACING property drives the EMIT side, not the visuals
     * (the idle/spin dual texture of upstream getRenderPasses2 :125-130 is the render
     * pool item). The empty-partial variant key applies the model to all four facings.
     */
    private void addCrank() {
        Block tCrank = GT6Kinetics.CRANK.get();
        simpleBlock(tCrank, models().cubeAll("crank", modLoc("block/crank")));
        itemModels().withExistingParent("crank", modLoc("block/crank"));
    }

    /**
     * Task p12-axle-family — the 44 axle blocks (11 materials x 4 diameters): ONE shared
     * {@code cube_column} model over the borrowed static {@code gt6:block/axle} texture
     * (end == side, the GTWires shared-texture form; assets/README.md attribution) and a
     * 3-variant blockstate per block driving the AXIS property — the vanilla
     * {@code axisBlock} rotation map (y = none, x = 90/90, z = 90/180) without the
     * RotatedPillarBlock type coupling. The 44 BlockItem models parent the shared block
     * model (44 one-line JSONs, the crank {@code itemModels()} precedent carried per row).
     */
    private void addAxles() {
        ModelFile tModel = models().cubeColumn("axle", modLoc("block/axle"), modLoc("block/axle"));
        for (var tAxle : GT6Kinetics.AXLE_BLOCKS.values()) {
            getVariantBuilder(tAxle.get()).forAllStates(aState -> switch (aState.getValue(GTAxleBlock.AXIS)) {
                case X -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 90, false)};
                case Y -> new ConfiguredModel[] {new ConfiguredModel(tModel)};
                case Z -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 180, false)};
            });
            itemModels().withExistingParent(tAxle.getId().getPath(), modLoc("block/axle"));
        }
    }

    /**
     * Task p12-tap-funnel-attachment spec ⑤ — the 12 wall attachments: one cube_all
     * per row over the TWO family textures, {@code tap.png} (borrowed from upstream
     * {@code machines/tools/tap/colored/side.png}) and {@code funnel.png} (upstream
     * {@code machines/tools/funnel/colored/side.png}), the barrel_metal shared-PNG
     * precedent — every row's model JSON references the family PNG, the model count
     * grows with the rows and the PNG count does not. The upstream texture stack is the
     * mRGBa-tinted colored layer + an overlay pass (MultiTileEntityFluidTap.java:181-208
     * three-pass faucet/spout boxes); the port shows the grayscale side icon un-tinted
     * over the WHOLE cube (the thin-plate getShape is the collision-free outline), the
     * per-face rotated plate model and the tint riding the render pool card (the crank
     * single-model deviation repeated). The empty-partial variant key applies the model
     * to all six facings.
     */
    private void addAttachments() {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            Block tBlock = GT6Attachments.BLOCKS_BY_PATH.get(tRow.path()).get();
            String tTexture = tRow.family() == gregtech6.block.attachment.GTAttachmentSmallBlock.Family.TAP ? "tap" : "funnel";
            simpleBlock(tBlock, models().cubeAll(tRow.path(), modLoc("block/" + tTexture)));
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
    }

    /**
     * Task p12-engine-steam — the Steam Engine family (all 28 rows of
     * {@link GT6Kinetics#STEAM_ENGINES}, the no-upstream-subset ruling): ONE shared
     * oriented cube model for the whole family — front (the KU emit face) / back (the
     * steam face) / side over the three borrowed upstream
     * {@code machines/engines/kinetic_steam/colored/} icons (assets/README.md sha256
     * attribution), rotated per {@code FACING} exactly like the machine ladder
     * ({@code addMachine}) but with NO active/running split — upstream keys the visuals
     * on synced mState/mActive byte data (getTexture2 :263-273, the seven-pass bespoke
     * renderer), which is the render-pool item; the port machine blocks carry no
     * active-state blockstate property. The 26 variants are visually identical here
     * (upstream tints the grayscale icons per material mRGBa — the crank-card un-tinted
     * deviation, the runtime tint rides the render pool).
     */
    private void addSteamEngines() {
        ModelFile tModel = models().cube("steam_engine",
                modLoc("block/steam_engine_side"), modLoc("block/steam_engine_side"),   // bottom/top
                modLoc("block/steam_engine_front"), modLoc("block/steam_engine_back"),  // north(front)/south(back)
                modLoc("block/steam_engine_side"), modLoc("block/steam_engine_side"));  // west/east
        for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
            Block tBlock = GT6Kinetics.STEAM_ENGINE_BLOCKS.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(GT6Kinetics.SteamEngineBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/steam_engine"));
        }
    }

    /**
     * Task p12-engine-diesel — the 8 diesel engine tiers (Loader_MultiTileEntities.java
     * :721-729): ONE shared cube_all over the borrowed upstream motor_liquid front icon
     * {@code gt6:textures/block/diesel_engine.png} (assets/README.md attribution) and ONE
     * blockstate variant per block — the FACING property drives the EMIT side, not the
     * visuals, exactly the crank ruling (the upstream front/back/sides colored +
     * overlay_active texture family, MultiTileEntityMotorLiquid.java:242-254 +
     * getTexture2 :216-221, is the render pool item; the per-material mRGBa tint is the
     * same pool). The 8 BlockItem models parent the shared block model (the crank
     * {@code itemModels()} form per row).
     */
    private void addDieselEngines() {
        for (var tBlock : GT6Kinetics.DIESEL_BLOCKS.values()) {
            simpleBlock(tBlock.get(), models().cubeAll("diesel_engine", modLoc("block/diesel_engine")));
            itemModels().withExistingParent(tBlock.getId().getPath(), modLoc("block/diesel_engine"));
        }
    }

    /**
     * Task p28-c-water-wheel — the Water Wheel: one cube_all over the ORIGINAL
     * {@code gt6:block/water_wheel} texture (the kTFRUAddon PNG is NOT borrowed — the
     * research-card license ruling: AGPL artwork never enters this repo, the wheel
     * texture is drawn for the port). ONE model over every AXIS state (simpleBlock =
     * partialState().setModels() matches all states, the diesel FACING precedent) — the
     * blade spin visual is the declared defer (the GT6Kinetics.WATER_WHEEL doc; the
     * functional ACTIVE output rides the BE). The BlockItem parents the shared block
     * model (the crank form).
     */
    private void addWaterWheel() {
        Block tWheel = GT6Kinetics.WATER_WHEEL.get();
        simpleBlock(tWheel, models().cubeAll("water_wheel", modLoc("block/water_wheel")));
        itemModels().withExistingParent("water_wheel", modLoc("block/water_wheel"));
    }

    /**
     * Task p12-gearbox-transformer — the GearBox (one cube_all over the borrowed
     * {@code gt6:block/gearbox} texture, the upstream iconsets/GEARBOX.png; assets/README.md
     * attribution) and the Rotation Transformer (the crank facing-cube shape: an
     * {@code orientable} model over the borrowed transformer_rotation colored front/side
     * textures, the FRONT = input face, BACK = output face —
     * MultiTileEntityTransformerRotation :42-45). Both single wood-row variants (the
     * material fan-out is the pool), no connection-mask visual layer (the per-face gear
     * overlays are the render pool). The static placeholder texture carries no rotation
     * animation — declared with the axle.
     */
    private void addGearBoxTransformer() {
        Block tBox = GT6Kinetics.GEARBOX.get();
        simpleBlock(tBox, models().cubeAll("gearbox", modLoc("block/gearbox")));
        itemModels().withExistingParent("gearbox", modLoc("block/gearbox"));
        Block tTrans = GT6Kinetics.TRANSFORMER_ROTATION.get();
        ModelFile tTransModel = models().orientable("transformer_rotation",
                modLoc("block/transformer_rotation_side"), modLoc("block/transformer_rotation_front"), modLoc("block/transformer_rotation_side"));
        getVariantBuilder(tTrans).forAllStates(aState -> {
            // the vanilla horizontal-facing rotation map (Direction.getFrontRotationYaw form)
            Direction tFacing = aState.getValue(GTTransformerRotationBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(tTransModel)
                    .rotationY((int) (tFacing.toYRot() + 180) % 360)
                    .build();
        });
        itemModels().withExistingParent("transformer_rotation", modLoc("block/transformer_rotation"));
    }

    /**
     * Task p28-c-ulv-lv-transformer — the Electric Transformer ULV-LV: the p12 rotation
     * transformer's orientable facing-cube shape verbatim (the FRONT = INPUT face — the
     * Base11 :63 convention; ALL-BUT-FRONT = output) over the BAKED upstream textures:
     * colored/front + overlay/front and colored/side + overlay/side composited src-over
     * into single-layer opaque PNGs (the p19 distillery bake treatment — the upstream
     * two-layer colored+overlay stack with the mRGBa tint is the render pool card;
     * assets/README.md carries the source + product sha256 attribution). The active
     * overlay (MultiTileEntityTransformerElectric :50-57) is the render pool defer.
     */
    private void addElectricTransformer() {
        Block tTrans = GT6ElectricTransformers.ELECTRIC_TRANSFORMER.get();
        ModelFile tModel = models().orientable("electric_transformer",
                modLoc("block/electric_transformer_side"), modLoc("block/electric_transformer_front"), modLoc("block/electric_transformer_side"));
        getVariantBuilder(tTrans).forAllStates(aState -> {
            // the vanilla horizontal-facing rotation map (the addGearBoxTransformer form)
            Direction tFacing = aState.getValue(GT6ElectricTransformerBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(tModel)
                    .rotationY((int) (tFacing.toYRot() + 180) % 360)
                    .build();
        });
        itemModels().withExistingParent("electric_transformer", modLoc("block/electric_transformer"));
    }

    /**
     * Task p8-prefixblock-render spec ① — the 3773 material prefix blocks
     * ({@link GTMaterialBlocks#blockArray()}, the card-A census order).
     *
     * <p><b>Model merging rule (pinned, anti-bloat)</b>: one SHARED block model per
     * (prefix, live-texture-set) pair — for each of the seven storage prefixes the live
     * set is the distinct {@code material.mTextureSetsBlock} first entry across that
     * prefix's registered pairs (upstream OreDictMaterial.java:252 + TextureSet.java:188-228;
     * port: MT.setTextures, MT.java:210-215). Measured 2026-08-31 (pinned by
     * GT6PrefixBlockRenderDatagenTest): 29+22+36+22+23+21+22 = 175 shared models, so the
     * total JSON output is 175 models + 3773 blockstates + 3773 item models — NOT a model
     * per material pair (per-pair models are an explicit red line, and a per-material
     * texture would not tint anyway). <b>Fallback</b>: a material with an empty/blank set
     * list resolves to {@code "none"} = upstream SET_NONE (TextureSet.java:188; the
     * GT6ItemModels.iconsetOf mirror on the block list) — measured over the registered
     * 3773 pairs the fallback never fires today, but it keeps the generator total: no set
     * name is dereferenced blindly. The grayscale texture carries the SHAPE, the material
     * colour comes from the runtime tint ({@code fRGBa[prefix.mState]},
     * RegisterColorHandlersEvent.Block) — so a fallback loses only the texture detail,
     * never the material identity.
     *
     * <p>Each model is an element-built cube_all with {@code tintindex 0} on all six faces
     * (the vanilla grass/leaves idiom — the parent {@code block/block} carries the item
     * display transforms, the faces carry {@code #all} + cullface, UVs default to the
     * element bounds exactly like vanilla {@code cube_all}). Blockstate = one JSON per
     * block, a single variant onto the shared model. Item model = one JSON per block,
     * {@code withExistingParent} onto the shared block model — same provider, same pass,
     * so the parent resolves in the ExistingFileHelper (BlockStateProvider.run flushes
     * block models before item models, the GT6BlockStates.java:29-33 precedent — the very
     * reason the ADR moved the models off card A). Textures are the borrowed upstream
     * grayscale PNGs ({@code gt6:textures/block/materialicons/<set>/<prefix>.png},
     * lowercased per the 1.20.1 ResourceLocation constraint; assets/README.md attribution).
     */
    private void addPrefixBlocks() {
        Map<String, ModelFile> tShared = new HashMap<>(); // one shared model per (prefix, set), built on first use
        for (Block tBlock : GTMaterialBlocks.blockArray()) {
            GTMaterialPrefixBlock tPrefixBlock = (GTMaterialPrefixBlock)tBlock;
            String tPrefixSnake = GTMaterialItems.snakeCase(tPrefixBlock.prefix.mNameInternal);
            String tSetSnake = blockSetOf(tPrefixBlock.material);
            // Full "block/..." model path: getBuilder skips the folder prefix for "/"-bearing
            // names (ModelProvider.extendWithFolder), so the block/ segment must be explicit —
            // that keeps the tracked/built location == models/block/materialicons/... == the
            // item-parent lookup below.
            String tModelName = "block/materialicons/" + tSetSnake + "/" + tPrefixSnake;
            ModelFile tModel = tShared.computeIfAbsent(tModelName,
                    tKey -> tintedCubeAll(tKey, modLoc("block/materialicons/" + tSetSnake + "/" + tPrefixSnake)));
            simpleBlock(tBlock, tModel);
            itemModels().withExistingParent(GTMaterialItems.itemIdOf(tPrefixBlock.prefix, tPrefixBlock.material),
                    modLoc(tModelName));
        }
        LOGGER.info("GT6 prefix blocks: {} blocks over {} shared (prefix x set) models",
                GTMaterialBlocks.blockArray().length, tShared.size());
    }

    /**
     * One tinted cube_all block model: parent block/block (the 3D item display transforms),
     * a full 0..16 element with all six faces on {@code #all}, cullface per side and
     * {@code tintindex 0} (the vanilla grass_block/leaves element idiom; no explicit UVs —
     * they default to the element bounds, byte-equivalent to vanilla cube_all output).
     * {@code aName} must be the full {@code block/...} model path (see addPrefixBlocks).
     */
    private ModelFile tintedCubeAll(String aName, ResourceLocation aTexture) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aTexture)
                .texture("particle", "#all");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    /**
     * The material's BLOCK texture-set name, lower-snaked; an empty/blank list falls back
     * to {@code "none"} = upstream SET_NONE (TextureSet.java:188; MT.setTextures
     * MT.java:210-215 assigns the set name strings). Since task p9-wire-family-w2 the
     * implementation lives in the MC-free single source
     * {@link gregtech6.client.wire.GTWireTextures} (shared with the client listener — this
     * class's statics bootstrap-gate the offline test JVM, that one does not).
     */
    public static String blockSetOf(OreDictMaterial aMaterial) {
        return gregtech6.client.wire.GTWireTextures.blockSetOf(aMaterial);
    }

    /**
     * Task p13-boiler-tank — the 26 Steam Boiler Tank rows (Loader_MultiTileEntities.java
     * :553-579): ONE oriented cube model over the boiler_steam texture set (the grayscale
     * placeholders — the upstream machines/tanks/boiler_steam colored+overlay iconsets and
     * the BI.BAROMETER gauge have no borrowable source in this repo, the assets precedent;
     * the FRONT face carries the gauge texture, FACING drives the front semantics, the
     * barometer 5-bit visual is the synced BE payload — the per-state gauge rendering is
     * the render pool, the burning-box ruling repeated). Both ladders share the model (the
     * SAME block class upstream, :552 aClass). The 26 BlockItem models parent it.
     */
    private void addBoilers() {
        String tTex = "block/boiler_steam/";
        ModelFile tModel = models().cube("steam_boiler_tank",
                modLoc(tTex + "bottom"), modLoc(tTex + "top"),          // bottom/top
                modLoc(tTex + "front"), modLoc(tTex + "side"),          // north(front = the barometer face)/south
                modLoc(tTex + "side"), modLoc(tTex + "side"));          // west/east
        for (gregtech6.registry.GT6Boilers.BoilerRow tRow : gregtech6.registry.GT6Boilers.allRows()) {
            Block tBlock = gregtech6.registry.GT6Boilers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.registry.GT6Boilers.BoilerTankBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p26-storage-hopper-family — the 4 storage-hopper rows (Loader_MultiTileEntities
     * .java:145-146 over :191/:202, Bronze/Steel × hopper/queue): ONE oriented cube model
     * over the two grayscale placeholders (the upstream machines/automation/hopper and
     * queuehopper colored+overlay iconsets have no borrowable source in this repo — the
     * boiler/burning-box assets precedent; the FRONT face carries the output-face texture,
     * FACING drives the output semantics, the three-pass custom funnel shape of upstream
     * :263-277 is the render pool). Both kinds share the model (the SAME shapes upstream
     * :259-282/:241-264). The 4 BlockItem models parent it. The FACING is all-six (the
     * vanilla Piston blockstate convention): down x=90, up x=270, the horizontals the
     * boiler y-mapping.
     */
    private void addHoppers() {
        String tTex = "block/hopper_";
        ModelFile tModel = models().cube("gt6_hopper",
                modLoc(tTex + "side"), modLoc(tTex + "side"),        // bottom/top
                modLoc(tTex + "front"), modLoc(tTex + "side"),       // north(front = the output face)/south
                modLoc(tTex + "side"), modLoc(tTex + "side"));       // west/east
        for (gregtech6.registry.GT6Hoppers.HopperRow tRow : gregtech6.registry.GT6Hoppers.ROWS) {
            Block tBlock = gregtech6.registry.GT6Hoppers.BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tX = 0, tY = 0;
                switch (aState.getValue(gregtech6.registry.GT6Hoppers.GT6HopperBlock.FACING)) {
                    case DOWN -> tX = 90;   // the vanilla Piston blockstate convention
                    case UP -> tX = 270;
                    case SOUTH -> tY = 180;
                    case WEST -> tY = 270;
                    case EAST -> tY = 90;
                    default -> {} // NORTH
                }
                return ConfiguredModel.builder().modelFile(tModel).rotationX(tX).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p13-burning-box-family — the 97 burning-box rows (Loader_MultiTileEntities.java
     * :517-704): ONE shared oriented cube model per FAMILY over the four grayscale
     * family textures (gt6:block/burning_box_{solid,liquid,gas,fluidbed}, generated
     * placeholders — the upstream colored/overlay iconsets have no borrowable source in
     * this repo, the assets/README.md note), front = the FACING (the fuel/ignite face),
     * rotated per FACING exactly like the steam-engine ladder (addSteamEngines). The
     * Brick row shares the SOLID model (the same BE family, the stone-sound carrier).
     * The 97 BlockItem models parent their family model (the crank per-row form). The
     * per-material mRGBa tint and the burning overlay_active family are the render
     * pool card (the steam-engine ruling repeated).
     */
    private void addBurningBoxes() {
        java.util.Map<gregtech6.registry.GT6BurningBoxes.Family, ModelFile> tModels = new java.util.EnumMap<>(gregtech6.registry.GT6BurningBoxes.Family.class);
        for (gregtech6.registry.GT6BurningBoxes.Family tFamily : gregtech6.registry.GT6BurningBoxes.Family.values()) {
            String tTex = "block/burning_box_" + tFamily.name().toLowerCase(java.util.Locale.ROOT);
            tModels.put(tFamily, models().cube("burning_box_" + tFamily.name().toLowerCase(java.util.Locale.ROOT),
                    modLoc(tTex), modLoc(tTex),          // bottom/top
                    modLoc(tTex), modLoc(tTex),          // north(front)/south(back) — one face, FACING drives the front semantics
                    modLoc(tTex), modLoc(tTex)));        // west/east
        }
        for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.allRows()) {
            Block tBlock = gregtech6.registry.GT6BurningBoxes.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = tModels.get(tRow.family());
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(gregtech6.registry.GT6BurningBoxes.BurningBoxBlock.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tModel).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tModel.getLocation());
        }
    }

    /**
     * Task p13-large-boiler — the Large Boiler family (Loader_MultiTileEntities.java
     * :1159-1165/:1176/:1248-1252): the five Dense Wall part blocks + the Heat Transmitter
     * part block as plain cube_all over the generated grayscale placeholders (upstream has
     * no borrowable "largeboiler"/"metalwalldense" texture group in this snapshot — the
     * W2 burning-box ruling), and the five boiler variant controllers over ONE shared
     * oriented cube model (front = the barometer face) rotated per FACING exactly like
     * addBoilers; the FORMED variants map to the same model (the formed-look visual is the
     * p9 pool). The 11 BlockItem models parent their block models.
     */
    private void addLargeBoiler() {
        ModelFile tMain = models().cube("large_boiler_main",
                modLoc("block/large_boiler/wall"), modLoc("block/large_boiler/wall"),
                modLoc("block/large_boiler/main"), modLoc("block/large_boiler/main"),
                modLoc("block/large_boiler/main"), modLoc("block/large_boiler/main"));
        for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
            Block tBlock = gregtech6.registry.GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH.get(tRow.path()).get();
            getVariantBuilder(tBlock).forAllStates(aState -> {
                int tY = switch (aState.getValue(TileEntityBase10MultiBlockBase.FACING)) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0; // NORTH
                };
                return ConfiguredModel.builder().modelFile(tMain).rotationY(tY).build();
            });
            itemModels().withExistingParent(tRow.path(), tMain.getLocation());
        }
        for (var tRow : gregtech6.registry.GTMultiBlocks.WALL_ROWS) {
            addLargeBoilerPart(tRow.path(), "block/large_boiler/wall");
        }
        addLargeBoilerPart(gregtech6.registry.GTMultiBlocks.TRANSMITTER_ROW.path(), "block/large_boiler/transmitter");
    }

    /** One cube_all part block + its BlockItem parent (the coke-oven-bricks shape). */
    private void addLargeBoilerPart(String aPath, String aTexture) {
        Block tBlock = gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(aPath) != null
                ? gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(aPath).get()
                : gregtech6.registry.GTMultiBlocks.HEAT_TRANSMITTER.get();
        simpleBlock(tBlock, models().cubeAll(aPath, modLoc(aTexture)));
        itemModels().withExistingParent(aPath, modLoc("block/" + aPath));
    }

    /**
     * Task p24-lightning-rod — the Lightning Rod family (Loader_MultiTileEntities.java
     * :1151/:1168/:1179/:1282): the three part blocks as plain cube_all over the borrowed
     * upstream textures (the multiblockparts metalwall/coil/lightningrod colored faces, the
     * P20 ruling ①), and the single controller over ONE cube model (the borrowed
     * multiblockmains lightningrod group — the colored base alpha-over the overlay_front
     * decal, composited at borrow time; all three upstream faces composite to the SAME
     * visible pixels, so one texture serves all six faces, the large_boiler/main.png form).
     * The facing is structurally meaningless (the rod is vertical), so every state maps to
     * the same model with no rotation; the FORMED variants map to the same model (the
     * formed-look visual is the p9 pool). The four BlockItem models parent their block
     * models.
     */
    private void addLightningRod() {
        ModelFile tMain = models().cubeAll("multiblock_lightning_rod", modLoc("block/lightningrod/main"));
        Block tController = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD.get();
        getVariantBuilder(tController).forAllStates(aState -> ConfiguredModel.builder().modelFile(tMain).build());
        itemModels().withExistingParent("multiblock_lightning_rod", tMain.getLocation());
        addLightningRodPart("machine_wall_tungsten", "block/lightningrod/wall");
        addLightningRodPart("niobium_titanium_coil", "block/lightningrod/coil");
        addLightningRodPart("lightning_rod", "block/lightningrod/rod");
    }

    /** One cube_all Lightning Rod part block + its BlockItem parent (the addLargeBoilerPart shape). */
    private void addLightningRodPart(String aPath, String aTexture) {
        Block tBlock = gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH.get(aPath).get();
        simpleBlock(tBlock, models().cubeAll(aPath, modLoc(aTexture)));
        itemModels().withExistingParent(aPath, modLoc("block/" + aPath));
    }

    /**
     * Task p21-stoneblocks-16item-registry-split — the 272 GT6 stone VARIANT blocks
     * ({@link GTStoneBlocks#blockArray()}, stone-major in CS.java:1668 order and
     * variant-major in meta order, the per-pair registry split): each block is a degenerate
     * pure block with a FIXED {@link StoneVariant}, so each gets a plain single-state
     * blockstate over its OWN cube_all model (the P19 16-row {@code variant=<snake>} rows
     * retired with the EnumProperty — one property-free state per block now), and each gets
     * its OWN item model {@code withExistingParent} onto that block model (the id scheme is
     * {@link GTStoneBlocks#path}: variant 0 keeps the bare snake, the other 15 suffix the
     * variant segment). The model/texture keys are UNCHANGED from the P19 render card
     * ({@code gt6:block/stones/<stone>/<variant>}, one model per dedicated borrowed PNG,
     * assets/README.md attribution, census 17x16 = 272 files, zero gaps, NO tintindex — the
     * colored-PNG route), so only the blockstate/item/loot faces re-key per pair.
     *
     * <p>Same provider, same pass, so the parent resolves in the ExistingFileHelper (the
     * GT6BlockStates.java:29-33 BlockStateProvider.run ordering precedent). The P19 declared
     * deviation "the inventory shows the STONE look for every state" is RETIRED: with one
     * item per variant, every inventory stack now shows its own variant's look — the
     * upstream 1.7.10 ItemBlock per-meta icon face, restored.
     */
    private void addStoneBlocks() {
        for (Block tBlock : GTStoneBlocks.blockArray()) {
            GTStoneBlock tStone = (GTStoneBlock)tBlock;
            // Full "block/..." model path: getBuilder skips the folder prefix for
            // "/"-bearing names (ModelProvider.extendWithFolder), so the block/ segment
            // must be explicit — the addPrefixBlocks pin, same builder mechanics here.
            String tModelName = "block/stones/" + tStone.stoneSnake + "/" + tStone.variant.snake;
            ModelFile tModel = models().cubeAll(tModelName,
                    modLoc("block/stones/" + tStone.stoneSnake + "/" + tStone.variant.snake));
            simpleBlock(tBlock, tModel); // the single default state -> the variants:{"": ...} form
            itemModels().withExistingParent(GTStoneBlocks.path(tStone.stoneSnake, tStone.variant), modLoc(tModelName));
        }
        LOGGER.info("GT6 stone blocks: {} per-pair blockstates over {} models (one item model each)",
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length,
                GTStoneBlocks.STONES.size() * StoneVariant.VALUES.length);
    }

    /**
     * Task p24-grass-block — the 6 GT grass VARIANT blocks ({@link GTGrassBlocks#BLOCKS},
     * upstream meta order): one single-state blockstate per pair (the addStoneBlocks
     * degenerate-pure-block form) over a {@code cube_bottom_top} model — top/side ride the
     * BORROWED pre-coloured PNGs ({@code gt6:block/grass/top_<colour>}/
     * {@code side_<colour>}, byte-identical per the assets/README.md ledger) and the
     * BOTTOM face references the VANILLA {@code minecraft:block/dirt} model texture
     * directly (the upstream {@code IconContainerCopied(Blocks.dirt, 0, SIDE_BOTTOM)}
     * semantics, BlockGrass.java:102-104 — no PNG is copied). ZERO tintindex anywhere:
     * the colour is baked into the PNGs (Textures.java:530-565 pre-coloured sets) and a
     * GrassBlock-style biome tint is forbidden (decisions.p24-grass-behavior-trim).
     * Each pair gets its own item model parenting the block model. Same provider, same
     * pass, so the parent resolves in the ExistingFileHelper (the :29-33 precedent).
     *
     * <p>NAMING TRAP (the research card pin): the borrow is by CODE mapping, not by file
     * name — upstream meta 3 "LightGray" renders the {@code NORMAL} PNG and meta 0
     * "Green" the {@code MEDIUM} PNG (Textures.java:530-565). The borrowed files already
     * carry the VARIANT-semantics names ({@code top_green.png} et al), so this walk is
     * blind to the trap.
     */
    private void addGrassBlocks() {
        for (int i = 0; i < GTGrassBlocks.PATHS.size(); i++) {
            String tPath = GTGrassBlocks.PATHS.get(i);
            String tColour = GTGrassBlocks.textureOf(tPath);
            ModelFile tModel = models().cubeBottomTop(tPath,
                    modLoc("block/grass/side_" + tColour), // the forge (name, side, bottom, top) parameter order
                    mcLoc("block/dirt"), // the vanilla dirt bottom, the upstream copied-icon face
                    modLoc("block/grass/top_" + tColour));
            simpleBlock(GTGrassBlocks.BLOCKS.get(i).get(), tModel);
            itemModels().withExistingParent(tPath, modLoc("block/" + tPath));
        }
        LOGGER.info("GT6 grass blocks: {} per-pair blockstates over {} cube_bottom_top models (no tint)",
                GTGrassBlocks.PATHS.size(), GTGrassBlocks.PATHS.size());
    }

    /**
     * Task p26-c-foam-block-family — the C-Foam block family band (upstream
     * BlockCFoamFresh/BlockCFoam + the MTE 32765 carrier). The four-state texture set of
     * upstream MultiTileEntityCFoam.getTexture2 (:132 — CFOAM_FRESH/CFOAM_FRESH_OWNED/
     * CFOAM_HARDENED/CFOAM_HARDENED_OWNED) maps onto:
     * <ul>
     * <li>{@code cfoam_fresh}/{@code cfoam} — one tinted cube per state, the 16
     *     {@code color} states SHARE the model (the colour rides the BlockState tint,
     *     the {@link gregtech6.client.foam.GTCFoamTintListener} BlockColor, tintindex 0
     *     — the ruling_color_dim form, NOT a 16-instance ladder);</li>
     * <li>{@code cfoam_owned} — the DRIED property swaps the sprite pair (fresh_owned/
     *     hardened_owned), the paint colour rides the BE tint (the same BlockColor);
     *     no BlockItem exists (the upstream showInCreative false), so no item model;</li>
     * <li>the two slabs — hand-built half-cube elements with tintindex 0 (the vanilla
     *     slab parents carry NO tintindex, so the tinted family needs own models); the
     *     DOUBLE variant shares the full-cube model (these slabs are spray-placed only,
     *     the double form is unreachable but must not miss its variant). The fresh forms
     *     have no BlockItem either (spray-only intermediate states).</li>
     * </ul>
     */
    private void addFoamBlocks() {
        // the two full blocks — 16 colour states over one tinted model each
        ModelFile tFresh = tintedCubeAll("block/cfoam_fresh", modLoc("block/cfoam_fresh"));
        getVariantBuilder(GT6FoamBlocks.CFOAM_FRESH.get())
                .forAllStates(aState -> ConfiguredModel.builder().modelFile(tFresh).build());
        ModelFile tHardened = tintedCubeAll("block/cfoam_hardened", modLoc("block/cfoam_hardened"));
        getVariantBuilder(GT6FoamBlocks.CFOAM.get())
                .forAllStates(aState -> ConfiguredModel.builder().modelFile(tHardened).build());
        itemModels().withExistingParent("cfoam", modLoc("block/cfoam_hardened"));

        // the owned carrier — the DRIED property over the owned sprite pair (upstream :132)
        ModelFile tOwnedWet = tintedCubeAll("block/cfoam_fresh_owned", modLoc("block/cfoam_fresh_owned"));
        ModelFile tOwnedDry = tintedCubeAll("block/cfoam_hardened_owned", modLoc("block/cfoam_hardened_owned"));
        getVariantBuilder(GT6FoamBlocks.CFOAM_OWNED.get())
                .partialState().with(GT6CFoamOwnedBlock.DRIED, false)
                .setModels(ConfiguredModel.builder().modelFile(tOwnedWet).build())
                .partialState().with(GT6CFoamOwnedBlock.DRIED, true)
                .setModels(ConfiguredModel.builder().modelFile(tOwnedDry).build());

        // the two slabs — half-cube tinted elements (BOTTOM/TOP/DOUBLE triads over the family
        // textures); the DRIED item parents its bottom-slab model (the vanilla slab item form)
        tintedSlabFamily("cfoam_fresh_slab", modLoc("block/cfoam_fresh"), GT6FoamBlocks.CFOAM_FRESH_SLAB.get());
        tintedSlabFamily("cfoam_slab", modLoc("block/cfoam_hardened"), GT6FoamBlocks.CFOAM_SLAB.get());
        itemModels().withExistingParent("cfoam_slab", modLoc("block/cfoam_slab_bottom"));
        LOGGER.info("GT6 cfoam blocks: 5 blockstate bands (2 full + owned DRIED pair + 2 slab triads), 8 tinted models");
    }

    /** The three-variant slab band (BOTTOM/TOP/DOUBLE) over one texture — the half-cube elements carry tintindex 0. */
    private void tintedSlabFamily(String aName, ResourceLocation aTexture, Block aSlab) {
        ModelFile tBottom = tintedHalfSlab(aName + "_bottom", aTexture, false);
        ModelFile tTop = tintedHalfSlab(aName + "_top", aTexture, true);
        ModelFile tDouble = tintedCubeAll("block/" + aName + "_double", aTexture);
        getVariantBuilder(aSlab)
                .partialState().with(SlabBlock.TYPE, SlabType.BOTTOM)
                .setModels(ConfiguredModel.builder().modelFile(tBottom).build())
                .partialState().with(SlabBlock.TYPE, SlabType.TOP)
                .setModels(ConfiguredModel.builder().modelFile(tTop).build())
                .partialState().with(SlabBlock.TYPE, SlabType.DOUBLE)
                .setModels(ConfiguredModel.builder().modelFile(tDouble).build());
    }

    /** One tinted half-cube slab model — the {@link #tintedCubeAll} shape cut to the lower/upper half. */
    private ModelFile tintedHalfSlab(String aName, ResourceLocation aTexture, boolean aTop) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aTexture)
                .texture("particle", "#all");
        tModel.element()
                .from(0.0F, aTop ? 8.0F : 0.0F, 0.0F).to(16.0F, aTop ? 16.0F : 8.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }

    /**
     * Task p26-sensors-core — the three pioneer sensor blocks ({@link GT6Sensors#ROWS},
     * the Registration face): each one cube_all model over its OWN baked texture
     * ({@code gt6:block/<path>}, the assets/README.md bake-ledger entries) and ONE
     * 6-variant blockstate driving the full {@link GTSensorBlock#FACING} property — the
     * vanilla dispenser/observer rotation map (y = 0/90/180/270 on the horizontal ring,
     * x = 270/90 on up/down; the axle AXIS precedent for a property-driven variant
     * blockstate without the vanilla block-type coupling). The FACING property IS the
     * display/keypad face mirror (GTSensorBlockEntity#wrenchSetFacing writes it, the
     * TileEntityOven.setFrontFacing shape), so the baked digit-strip face re-orients with
     * the block exactly as the upstream thin-plate front icon did. Each BlockItem model
     * parents its block model (the crank one-line-per-row precedent).
     */
    private void addSensors() {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            Block tBlock = GT6Sensors.BLOCKS_BY_PATH.get(tRow.path()).get();
            ModelFile tModel = models().cubeAll(tRow.path(), modLoc("block/" + tRow.path()));
            getVariantBuilder(tBlock).forAllStates(aState -> switch (aState.getValue(GTSensorBlock.FACING)) {
                case NORTH -> new ConfiguredModel[] {new ConfiguredModel(tModel)};
                case SOUTH -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 180, false)};
                case WEST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 270, false)};
                case EAST  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 0, 90, false)};
                case UP    -> new ConfiguredModel[] {new ConfiguredModel(tModel, 270, 0, false)};
                case DOWN  -> new ConfiguredModel[] {new ConfiguredModel(tModel, 90, 0, false)};
            });
            itemModels().withExistingParent(tRow.path(), modLoc("block/" + tRow.path()));
        }
        LOGGER.info("GT6 sensors: {} pioneer blockstates x 6 FACING variants (the oriented cube)", GT6Sensors.ROWS.size());
    }
}
