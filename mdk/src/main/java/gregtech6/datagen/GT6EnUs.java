package gregtech6.datagen;

import java.util.HashSet;
import java.util.Set;

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.fluid.GTFluids;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * en_us lang, two template tables with no per-item combination explosion (GTCEu data/lang/
 * ItemLang.java:37-54 shape, ADR-P2-4):
 * <ul>
 * <li>{@code gt6.tagprefix.<prefix_snake>} = "{pre}%s{post}" for every OP prefix — the template
 *     the runtime fills via {@code Component.translatable} at {@code getName} time, composed from
 *     the prefix's mMaterialPre/mMaterialPost (OreDictPrefix.java:108, set at :338-339), which is
 *     upstream GT6's item name composition (mMaterialPre + material name + mMaterialPost);</li>
 * <li>{@code gt6.material.<material_snake>} = the English local name — exactly the field
 *     {@code MaterialPrefixItem.getName} fills the template with
 *     (MaterialPrefixItem.java:65, mNameLocal);</li>
 * <li>{@code itemGroup.gt6.<prefix_snake>} = one per creative-visible prefix tab, valued with the
 *     prefix's mNameCategory — upstream registers the tab with
 *     {@code LH.add("itemGroup." + mNameInternal, mNameCategory)} (CreativeTab.java:32, created at
 *     PrefixItem.java:90); the tab set is taken from {@link GTMaterialItems#tabPrefixes()} so lang
 *     keys cannot drift from the registered tabs.</li>
 * </ul>
 *
 * <p>The per-item override keys {@code gt6.<prefix>_<material>} are intentionally NOT generated:
 * they are the hand-translation layer, and their absence makes the runtime
 * {@code Language.has(specialKey)} check (MaterialPrefixItem.java:64) fall through to the
 * template. LanguageProvider sorts keys (TreeMap) and writes via DataProvider.saveStable, so the
 * output is deterministic across runs.
 *
 * <p>Non-final on purpose: the offline lang-key reconciliation test (GT6EnUsJeiInfoTest,
 * task p12-jei-integration) records {@code add()} through a same-package subclass — the only
 * way to observe {@code addTranslations()} output without a datagen run.
 */
public class GT6EnUs extends LanguageProvider {

    public GT6EnUs(PackOutput output) {
        super(output, GT6DataGenerators.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addTabTitles();
        addBlockTabTitles();
        addPrefixTemplates();
        addMaterialNames();
        addExampleMachine();
        addFluidPipes();
        addEngineFluids();
        addElectricWires();
        addMachines();
        addMultiBlocks();
        addBarrels();
        addEnergySource();
        addKinetics(); // task p12-engine-crank
        addTools();
        addCovers();
        addJeiInfo();
    }

    /**
     * Engine fuel family keys (task p12-engine-fuel-fluids spec ③): one description entry
     * per {@link GTFluids.EngineFluidSpec} row — the exact descriptionId the FluidType is
     * registered with, walked from {@link GTFluids#ENGINE_SPECS} so the lang face cannot
     * drift from the registered fluids. Values are the upstream display names: the
     * material local names (MT.java:1884 "Steam" / :1891 "Distilled Water" /
     * :2040 "Ethanol" / :2044 "Fuel Oil" local / :2045 "Nitro-Fuel" / :2046-2048
     * Kerosine/Diesel/Petrol) and "Jet Fuel" for the material-less compat fluid
     * (FL.java:422 "rc jet fuel", the common name).
     */
    private void addEngineFluids() {
        for (GTFluids.EngineFluid tFamily : GTFluids.engineFluids()) {
            add(tFamily.spec.descriptionId(), DISPLAY_NAMES.get(tFamily.spec.name()));
        }
    }

    /** The engine-family display names, keyed by gt6 id path (see addEngineFluids for the anchors). */
    private static final java.util.Map<String, String> DISPLAY_NAMES = java.util.Map.of(
        "steam"           , "Steam",
        "distilled_water" , "Distilled Water",
        "diesel"          , "Diesel",
        "kerosine"        , "Kerosine",
        "petrol"          , "Petrol",
        "fuel"            , "Fuel",
        "nitrofuel"       , "Nitro-Fuel",
        "jetfuel"         , "Jet Fuel",
        "ethanol"         , "Ethanol");

    /**
     * The JEI ingredient info page (task p12-jei-integration, ADR 2026-09-02-p12-jei-dependency):
     * the coke oven structure description shown by JEI's built-in info page on the controller
     * item — the consumer is {@code GT6JeiPlugin.registerRecipes}
     * ({@code addIngredientInfo} → {@code Component.translatable}), so the key comes from the
     * plugin's constant and cannot drift from the consumer side.
     *
     * <p>Structure facts are pinned by the port's own live gate (task p6-cokeoven-processing,
     * RCON {@code gt6multiblock frame/check}: {@code linked_parts=25/25}): the 3x3x3 cube has
     * the controller in the middle of one face and an EMPTY center cell, so the bricks count is
     * 25 (the structure loop checks 26 cells, one of which is the controller itself).
     */
    private void addJeiInfo() {
        add(GT6JeiPlugin.INFO_KEY_COKE_OVEN,
            "The Coke Oven is a 3x3x3 cube: place the Coke Oven in the middle of one side, facing "
            + "outward, leave the center cell of the cube empty, and fill the remaining 25 cells "
            + "with Coke Oven Bricks. Ignite the controller to start it - it makes its own heat, "
            + "and a tank on the layer below the structure collects the Creosote.");
    }

    /**
     * Tool keys (task p9-tool-crowbar spec ④): the crowbar display name — the upstream
     * tool family name ("Crowbar", the GT_Tool_Crowbar registration row wording).
     * Task p10-tool-cutter spec ③: the wire cutter display name ("Wire Cutter", the
     * upstream WIRECUTTER registration row wording, Loader_Tools.java:131 verbatim).
     * Task p10-tool-creative-tab: the "Tools" creative tab title (the upstream ToolsGT
     * meta-tool category, Loader_Tools.java:114-145 registration rows; the key comes from
     * GT6Tools.TAB_TITLE_KEY so the lang face cannot drift from the registered tab —
     * the offline test pins the literal on both sides).
     */
    private void addTools() {
        add("item.gt6.crowbar", "Crowbar");
        add("item.gt6.cutter", "Wire Cutter");
        add(GT6Tools.TAB_TITLE_KEY, "Tools");
    }

    /**
     * The cover item display names. Task p9-redstone-cover-emitter: the redstone emitter
     * cover (upstream MultiItemTechnological.java:80 — meta 1021, display name
     * "Redstone Emitter" verbatim; the "Emits a constant Redstone Signal" tooltip rides
     * the upstream addToolTips channel which the single-item port does not carry).
     * Task p10-cover-conductor-redstone: the conductor pair (upstream
     * MultiItemTechnological.java:88-89 — metas 1029/1030, display names
     * "Redstone Conductor Cover (Accept)"/"(Emit)" verbatim; the transfer-direction
     * tooltips ride the same cut addToolTips channel).
     * Task p10-cover-controller-redstone: the machine switch (upstream
     * MultiItemTechnological.java:64 — meta 1005, display name "Redstone Machine
     * Switch" verbatim; the "Turns Machines ON/OFF using Redstone" tooltip and the
     * screwdriver tooltip ride the cut addToolTips channel).
     * Task p11-cover-shutter-filter: the shutter (upstream
     * MultiItemTechnological.java:85 — meta 1026, display name "Shutter Cover"
     * verbatim) and the item filter (upstream :82 — meta 1023, display name
     * "Item Filter" verbatim); the toggle/filter tooltips ride the same cut
     * addToolTips channel.
     * Task p11-cover-controllers: the auto redstone switch (upstream
     * MultiItemTechnological.java:65 — meta 1006, display name "Auto Redstone
     * Machine Switch" verbatim) and the cover controller (:84 — meta 1025,
     * "Cover Controller" verbatim); the tooltips ride the cut addToolTips channel.
     * Task p11-cover-conveyor-robotarm: the conveyor + robot arm tier ladders (upstream
     * MultiItemTechnological.java:51/:53 — metas 12040+i / 12080+i, display names
     * "Compact Electric Conveyor (...)"/"Compact Robot Arm (...)" verbatim, the suffixes
     * are VN[0..9] (CS.java:154 voltage numerals; tier i = the 512>>i period); the
     * "Transfers a Stack every N Ticks" tooltips ride the cut addToolTips channel).
     */
    private void addCovers() {
        add("item.gt6.cover_redstone_emitter", "Redstone Emitter");
        add("item.gt6.cover_redstone_conductor_in", "Redstone Conductor Cover (Accept)");
        add("item.gt6.cover_redstone_conductor_out", "Redstone Conductor Cover (Emit)");
        add("item.gt6.cover_redstone_machine_switch", "Redstone Machine Switch");
        add("item.gt6.cover_shutter", "Shutter Cover");
        add("item.gt6.cover_item_filter", "Item Filter");
        add("item.gt6.cover_auto_redstone_machine_switch", "Auto Redstone Machine Switch");
        add("item.gt6.cover_controller", "Cover Controller");
        String[] tTiers = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1"};
        for (int i = 0; i < tTiers.length; i++) {
            add("item.gt6.cover_conveyor_" + i, "Compact Electric Conveyor (" + tTiers[i] + ")");
            add("item.gt6.cover_robot_arm_" + i, "Compact Robot Arm (" + tTiers[i] + ")");
        }
    }

    /**
     * Test energy source key (task p8-d4-energy-source spec ②): the block display name,
     * the upstream family it borrows the shape from (MultiTileEntitySolarPanelElectric,
     * the simplest generator form).
     */
    private void addEnergySource() {
        add("block.gt6.energy_source", "Test Energy Source");
    }

    /**
     * Kinetics family keys (task p12-engine-crank): the Hand Crank display name — the
     * upstream registration row wording ("Hand Crank",
     * Loader_MultiTileEntities.java:2106). The engine + transmission family appends here.
     *
     * <p>Task p12-axle-family: the 44 axle rows — {@code block.gt6.axle_<material>_<size>}
     * = "{@code <Size> <Material> Axle}" (the upstream row wording: "Small Wooden Axle"
     * :1663 hard-codes the Wooden display; the metal rows carry mNameLocal, e.g. "Small
     * Bronze Axle" :1672). Table-driven over {@link GT6Kinetics#AXLE_SPECS} — the lang
     * cannot drift from the registry names.
     */
    private void addKinetics() {
        add("block.gt6.crank", "Hand Crank");
        for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
            for (int tSize = 0; tSize < GT6Kinetics.AXLE_DIAMETERS.length; tSize++) {
                add("block.gt6." + GT6Kinetics.axleName(tSpec.material(), tSize),
                        GT6Kinetics.axleDisplay(tSpec, tSize));
            }
        }
    }

    /**
     * The seven material prefix BLOCK tab titles (task p8-prefixblock-registry, spec ⑦):
     * one per creative-visible block prefix (upstream PrefixBlockItem.java:66-67 creates one
     * CreativeTab per block* prefix with the prefix's mNameCategory as its label), keyed
     * {@code itemGroup.gt6.block_raw} etc. — the same "itemGroup.&lt;internal&gt; = mNameCategory"
     * composition as {@link #addTabTitles()}, over {@link GTMaterialBlocks#tabPrefixes()} so
     * the lang keys cannot drift from the registered tabs. Values: "Blocks of Ore"/"Blocks of
     * Gems"/"Blocks of Dusts"/"Blocks of Ingots"/"Blocks of Plates"/"Blocks of Gem Plates"/
     * "Blocks of Cast Metal" (OP.java:345-351 mNameCategory). This is the ONLY lang delta of
     * the card — the block items themselves compose the existing tagprefix templates.
     */
    private void addBlockTabTitles() {
        for (OreDictPrefix tPrefix : GTMaterialBlocks.tabPrefixes()) {
            add("itemGroup.gt6." + MaterialPrefixItem.snakeCase(tPrefix.mNameInternal),
                tPrefix.mNameCategory == null ? tPrefix.mNameInternal : tPrefix.mNameCategory);
        }
    }

    /**
     * Fluid barrel keys (task p4-fluid-barrel, W3 provider additions; extended by task
     * p6-barrel-metal-plastic): the barrel display names (upstream rows — "Wooden Barrel"
     * Loader_MultiTileEntities.java:2136, "Plastic Canister" :2150, "Bronze Drum" :2151)
     * and the "Fluid Containers" creative tab (the upstream MTE-registry category of the
     * same rows). Task p7-barrel-high-tier-melt-bridge: the high-tier drum rows
     * :2159-2170, display names verbatim.
     */
    private void addBarrels() {
        add("block.gt6.barrel_wood", "Wooden Barrel");
        add("block.gt6.barrel_plastic", "Plastic Canister");
        add("block.gt6.barrel_metal", "Bronze Drum");
        add("block.gt6.barrel_tungsten_alloy", "Tungsten Alloy Drum");
        add("block.gt6.barrel_titanium", "Titanium Drum");
        add("block.gt6.barrel_netherite", "Netherite Drum");
        add("block.gt6.barrel_tungstensteel", "Tungstensteel Drum");
        add("block.gt6.barrel_tungsten", "Tungsten Drum");
        add("block.gt6.barrel_void_metal", "Voidmetal Drum");
        add("block.gt6.barrel_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide Drum");
        add("block.gt6.barrel_gaia_spirit", "Gaia Drum");
        add("block.gt6.barrel_adamantium", "Adamantium Drum");
        add("block.gt6.barrel_draconium", "Draconium Drum");
        add("block.gt6.barrel_awakened_draconium", "Awakened Draconium Drum");
        add("block.gt6.barrel_infinity", "Infinity Drum");
        add("itemGroup.gt6.fluid_containers", "Fluid Containers");
    }

    /**
     * Electric wire keys (task p7-d2-cable spec ⑥): the two W1 variants (the upstream row
     * names "1x &lt;material&gt; Wire" / "2x ...", MultiTileEntityWireElectric.java:72-73,
     * material-less here) and the "Electric Wires" category tab (the upstream MTE category
     * name, addElectricWires :72). Task p9-wire-family-w1: the 620 family rows looped over
     * {@link GTWireSpecs} — the upstream row string verbatim per variant
     * ({@code "1x Tin Wire"}, {@code "12x Tin Cable"} = size + local name + form).
     */
    private void addElectricWires() {
        add("block.gt6.wire_electric_1x", "1x Electric Wire");
        add("block.gt6.wire_electric_2x", "2x Electric Wire");
        add("itemGroup.gt6.electric_wires", "Electric Wires");
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
            add("block.gt6." + GTWireSpecs.registryName(tVariant), GTWireSpecs.displayName(tVariant));
        }
        // task p10-wire-redstone-family — the redstone family (Loader:1893-1902), table-tail
        // append: "Red Alloy Wire"/"Red Alloy Cable"/"Lumium Wirelamp" (the upstream
        // registration names, no size prefix, the GTWireSpecs.displayName redstone branch).
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
            add("block.gt6." + GTWireSpecs.registryName(tVariant), GTWireSpecs.displayName(tVariant));
        }
        // task p10-wire-laser-placeholder — the laser family (Loader:1814-1815), table-tail
        // append: "Laser Fiber Wire" (the upstream registration name verbatim, no size
        // prefix, the GTWireSpecs.displayName laser branch — the row is material-less).
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) {
            add("block.gt6." + GTWireSpecs.registryName(tVariant), GTWireSpecs.displayName(tVariant));
        }
        // task p11-flat-redstone-tab — the two new tab titles, the upstream MTE category
        // strings verbatim: "Redstone Wires" (every Loader:1895-1902 row, tab id 27050) and
        // "Laser Wires" (Loader:1815, tab id 24900) — upstream registers the display via
        // LH.add("itemGroup." + name, aCategoricalName) (CreativeTab.java:35, created at
        // MultiTileEntityRegistry.java:191), so the value is the registration literal.
        add(GTWires.REDSTONE_TAB_TITLE_KEY, "Redstone Wires");
        add(GTWires.LASER_TAB_TITLE_KEY, "Laser Wires");
    }

    /**
     * Machine family keys (task p4-machine-oven, W2-exclusive provider additions; extended
     * by task p7-basicmachine-family): the oven block display name (upstream "Oven",
     * Loader_MultiTileEntities.java:1288-1291), the machine family display names (upstream
     * rows "Shredder"/"Crusher"/"Lathe", :1294/:1300/:1306) and the "Machines" creative tab
     * (the upstream MTE-registry category of the same rows).
     */
    private void addMachines() {
        add("block.gt6.oven", "Oven");
        add("block.gt6.shredder", "Shredder");
        add("block.gt6.crusher", "Crusher");
        add("block.gt6.lathe", "Lathe");
        // task p8-machine-tiers-doinject ⑧: the T2-T4 ladder — the upstream row names carry
        // the material tier in parentheses (:1295-1297/:1301-1303/:1307-1309); the port
        // registers by tier index, so the display names say "Tier N".
        add("block.gt6.shredder_t2", "Shredder (Tier 2)");
        add("block.gt6.shredder_t3", "Shredder (Tier 3)");
        add("block.gt6.shredder_t4", "Shredder (Tier 4)");
        add("block.gt6.crusher_t2", "Crusher (Tier 2)");
        add("block.gt6.crusher_t3", "Crusher (Tier 3)");
        add("block.gt6.crusher_t4", "Crusher (Tier 4)");
        add("block.gt6.lathe_t2", "Lathe (Tier 2)");
        add("block.gt6.lathe_t3", "Lathe (Tier 3)");
        add("block.gt6.lathe_t4", "Lathe (Tier 4)");
        add("itemGroup.gt6.machines", "Machines");
    }

    /**
     * Multiblock family keys (task p4-multiblock-framework, W3 provider order 1:
     * multiblock→barrel→cover): the Coke Oven controller + bricks part display names (the
     * upstream coke oven bricks MTE 18000 naming) and the "Multiblocks" creative tab.
     */
    private void addMultiBlocks() {
        add("block.gt6.multiblock_coke_oven", "Coke Oven");
        add("block.gt6.multiblock_coke_oven_bricks", "Coke Oven Bricks");
        add("itemGroup.gt6.multiblocks", "Multiblocks");
    }

    /**
     * Example machine keys (task p3-example-machine): the chest block display name and the
     * "chests" creative tab title. The tab carries the upstream MTE-registry category name
     * ("Chests", Loader_MultiTileEntities.java:132 / MultiTileEntityRegistry.java:191); the
     * block name resolves through MenuProvider#getDisplayName (block.getName).
     */
    private void addExampleMachine() {
        add("block.gt6.example_chest", "GT Example Chest");
        add("itemGroup.gt6.chests", "Chests");
    }

    /**
     * Fluid pipe keys (task p4-fluid-pipes, W1-exclusive provider additions): the two wood
     * tiers, the "Fluid Pipes" category tab (the upstream MTE category name,
     * MultiTileEntityPipeFluid.java:83-98) and the molten iron FluidType description
     * (descriptionId set at GTFluids.IRON_MOLTEN_TYPE, the FL.create local-name counterpart
     * Loader_Fluids.java:40-106).
     */
    private void addFluidPipes() {
        add("block.gt6.wood_fluid_pipe_small", "Small Wood Fluid Pipe");
        add("block.gt6.wood_fluid_pipe_medium", "Wood Fluid Pipe");
        add("itemGroup.gt6.fluid_pipes", "Fluid Pipes");
        add("fluid.gt6.iron_molten", "Molten Iron");
        add("fluid.gt6.natural_gas", "Natural Gas"); // task p5-barrel-side-rules spec ⑤ — the lighter-fluid acceptance carrier
    }

    /** One title key per creative-visible prefix tab (CreativeTab.java:32 shape, upstream mNameCategory). */
    private void addTabTitles() {
        for (OreDictPrefix tPrefix : GTMaterialItems.tabPrefixes()) {
            add("itemGroup.gt6." + MaterialPrefixItem.snakeCase(tPrefix.mNameInternal),
                tPrefix.mNameCategory == null ? tPrefix.mNameInternal : tPrefix.mNameCategory);
        }
    }

    /** Table a: one "%s"-template per prefix, all of OP.VALUES (post-OP.init()). */
    private void addPrefixTemplates() {
        Set<String> tSeen = new HashSet<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            String tKey = "gt6.tagprefix." + MaterialPrefixItem.snakeCase(tPrefix.mNameInternal);
            if (!tSeen.add(tKey)) { // snake_case collisions are unlikely but must not pass silently
                GT6Mod.LOGGER.warn("GT6 datagen: duplicate prefix template key {} for {}", tKey, tPrefix.mNameInternal);
                continue;
            }
            add(tKey, templateOf(tPrefix));
        }
    }

    /** mMaterialPre + "%s" + mMaterialPost; nulls normalised (upstream composition is plain concat). */
    private static String templateOf(OreDictPrefix prefix) {
        String tPre = prefix.mMaterialPre == null ? "" : prefix.mMaterialPre;
        String tPost = prefix.mMaterialPost == null ? "" : prefix.mMaterialPost;
        return tPre + "%s" + tPost;
    }

    /** Table b: one entry per registration-target material (alias slots merged like the bridge). */
    private void addMaterialNames() {
        Set<String> tSeen = new HashSet<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias merge, MaterialRegistry.java:182-185
            if (tMaterial == null || tMaterial.mID < 0 || tMaterial.mNameLocal == null) continue;
            String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tMaterial.mNameInternal);
            if (!tSeen.add(tKey)) continue; // first (lowest mID) definition wins
            add(tKey, tMaterial.mNameLocal);
        }
    }
}
