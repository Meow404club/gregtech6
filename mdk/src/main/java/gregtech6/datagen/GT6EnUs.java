package gregtech6.datagen;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.block.stone.StoneVariant;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.fluid.GTFluids;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import gregtech6.covers.GT6Covers;
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
        addAquaFluids();
        addSimpleLiquidFluids(); // task p19-drying-rows-backfill-2
        addElectricWires();
        addMachines();
        addMultiBlocks();
        addBarrels();
        addEnergySource();
        addKinetics(); // task p12-engine-crank
        addAttachments(); // task p12-tap-funnel-attachment
        addTools();
        addCovers();
        addJeiInfo();
        addStoneBlocks(); // task p19-stoneblocks-registry — table-tail append (the drying card appends after this)
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
     * Aqua family keys (task p16-aqua-fluids): one description entry per
     * {@link GTFluids.AquaFluidSpec} row — the exact descriptionId the FluidType is
     * registered with, walked from {@link GTFluids#AQUA_SPECS} so the lang face cannot
     * drift from the registered fluids. Values ride the row's displayName: the upstream
     * display names verbatim where GT6 defines the fluid (Loader_Fluids.java:362
     * "Spectral Dew" / :371 "Mineral Water" / :373 "Hot Spring Water") and the common
     * names for the three external fluids the FL.java:114-117 shorthands spell
     * ("Boiling Water"/"Hot Water"/"Cold Water").
     */
    private void addAquaFluids() {
        for (GTFluids.AquaFluidSpec tSpec : GTFluids.AQUA_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
    }

    /**
     * Simple-liquid family keys (task p19-drying-rows-backfill-2 spec ③): the second loop
     * over {@link GTFluids#SIMPLE_LIQUID_SPECS} — the exact addAquaFluids shape, walked
     * from the SECOND table so the lang face cannot drift from the registered fluids
     * (gt6:seawater "Seawater" / gt6:waterdirty "Dirty Water", the common-name spellings
     * per the water_boiling "Boiling Water" precedent). Table-tail append, append-only.
     */
    private void addSimpleLiquidFluids() {
        for (GTFluids.AquaFluidSpec tSpec : GTFluids.SIMPLE_LIQUID_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
    }

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
     * Task p16-chisel-decalcify spec ①: the chisel display name ("Chisel", the upstream
     * CHISEL registration row wording, Loader_Tools.java:142 verbatim).
     * Task p10-tool-creative-tab: the "Tools" creative tab title (the upstream ToolsGT
     * meta-tool category, Loader_Tools.java:114-145 registration rows; the key comes from
     * GT6Tools.TAB_TITLE_KEY so the lang face cannot drift from the registered tab —
     * the offline test pins the literal on both sides).
     */
    private void addTools() {
        add("item.gt6.crowbar", "Crowbar");
        add("item.gt6.cutter", "Wire Cutter");
        add("item.gt6.chisel", "Chisel");
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
     * Task p11-cover-conveyor-robotarm: the eight atomic cover names stay verbatim. The
     * conveyor + robot arm tier ladders became COMPOSED (task p20-i18n-compose-wires, the
     * B-wave lang ruling): the twenty per-tier full strings retired into the two
     * position-param templates the item getName fills with the tier literal
     * (GT6Covers.CONVEYOR_DISPLAY_KEY/ROBOT_ARM_DISPLAY_KEY — the tier names are the
     * CS.java:154 voltage numerals, proper nouns that stay the en literals in both
     * locales, so they ride as plain literal args, no small-unit keys).
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
        add(GT6Covers.CONVEYOR_DISPLAY_KEY, "Compact Electric Conveyor (%s)");
        add(GT6Covers.ROBOT_ARM_DISPLAY_KEY, "Compact Robot Arm (%s)");
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
     *
     * <p>Task p12-engine-diesel: the 8 diesel engine rows —
     * {@code block.gt6.diesel_engine_<material>} = "{@code <Material> Diesel Engine}" (the
     * upstream row wording "Diesel Engine (Bronze)" :721-729, the port noun-order
     * convention). Table-driven over {@link GT6Kinetics#DIESEL_SPECS}.
     */
    private void addKinetics() {
        add("block.gt6.crank", "Hand Crank");
        // task p20-i18n-compose-rows — the axle/steam/diesel/burning-box/boiler families
        // compose at runtime (the family blocks' getName); the pre-installed full strings
        // retired into ONE family template + the row-material / size / family-word small
        // units, walked from the row tables so the faces cannot drift from the registries
        addAxleUnits();
        addSteamEngineUnits();
        addDieselUnits();
        addBurningBoxUnits();
        addBoilerUnits();
    }

    /** The axle template + the four size units + the eleven row-material units (the AXLE_SPECS walk). */
    private void addAxleUnits() {
        add(GT6Kinetics.AXLE_DISPLAY_KEY, "%s %s Axle");
        for (int i = 0; i < GT6Kinetics.AXLE_SIZE_NAMES.length; i++) {
            add(GT6Kinetics.axleSizeUnitKey(i), GT6Kinetics.axleSizeDisplay(i));
        }
        for (GT6Kinetics.AxleSpec tSpec : GT6Kinetics.AXLE_SPECS) {
            addRowMatUnit(GT6Kinetics.axleMatUnitKey(tSpec), tSpec.matDisplay());
        }
    }

    /** The two steam-engine templates (normal/strong) + the fourteen row-material units (the STEAM_ENGINES walk). */
    private void addSteamEngineUnits() {
        add(GT6Kinetics.STEAM_DISPLAY_KEY, "Steam Engine (%s)");
        add(GT6Kinetics.STEAM_DISPLAY_STRONG_KEY, "Strong Steam Engine (%s)");
        for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
            addRowMatUnit(GT6Kinetics.steamMatUnitKey(tRow), tRow.matDisplay());
        }
    }

    /** The diesel template + the eight row-material units (the DIESEL_SPECS walk — every word shared with the steam/axle faces). */
    private void addDieselUnits() {
        add(GT6Kinetics.DIESEL_DISPLAY_KEY, "%s Diesel Engine");
        for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
            addRowMatUnit(GT6Kinetics.dieselMatUnitKey(tSpec), tSpec.matDisplay());
        }
    }

    /** The four burning-box templates + three family words + the row-material units; the Brick row stays ATOMIC (the lone prefix form, Loader:519). */
    private void addBurningBoxUnits() {
        add(gregtech6.registry.GT6BurningBoxes.DISPLAY_KEY, "Burning Box (%s, %s)");
        add(gregtech6.registry.GT6BurningBoxes.DISPLAY_DENSE_KEY, "Dense Burning Box (%s, %s)");
        add(gregtech6.registry.GT6BurningBoxes.DISPLAY_FLUIDBED_KEY, "Fluidized Bed Burning Box (%s)");
        add(gregtech6.registry.GT6BurningBoxes.DISPLAY_FLUIDBED_DENSE_KEY, "Dense Fluidized Bed Burning Box (%s)");
        add(gregtech6.registry.GT6BurningBoxes.FAMILY_SOLID_UNIT_KEY, "Solid");
        add(gregtech6.registry.GT6BurningBoxes.FAMILY_LIQUID_UNIT_KEY, "Liquid");
        add(gregtech6.registry.GT6BurningBoxes.FAMILY_GAS_UNIT_KEY, "Gas");
        add("block.gt6.brick_burning_box", "Brick Burning Box (Solid)"); // the atomic Brick row (Loader:519 verbatim)
        for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.allRows()) {
            addRowMatUnit(gregtech6.registry.GT6BurningBoxes.matUnitKeyOf(tRow), tRow.material().display());
        }
    }

    /** The two boiler templates (normal/strong) + the row-material units (the BOILER_ROWS walk; Ultimet is boiler-only, Loader:565/:579). */
    private void addBoilerUnits() {
        add(gregtech6.registry.GT6Boilers.DISPLAY_KEY, "Steam Boiler Tank (%s)");
        add(gregtech6.registry.GT6Boilers.DISPLAY_STRONG_KEY, "Strong Steam Boiler Tank (%s)");
        for (gregtech6.registry.GT6Boilers.BoilerRow tRow : gregtech6.registry.GT6Boilers.allRows()) {
            addRowMatUnit(gregtech6.registry.GT6Boilers.matUnitKeyOf(tRow), tRow.material().display());
        }
    }

    /** Dedup across the row-family walks (a word shared by several families is ONE unit key). */
    private final Set<String> rowMatUnitsEmitted = new HashSet<>();

    private void addRowMatUnit(String aKey, String aWord) {
        if (rowMatUnitsEmitted.add(aKey)) add(aKey, aWord);
    }

    /**
     * Attachment family keys (task p12-tap-funnel-attachment spec ⑤): one display entry
     * per {@link GT6Attachments.AttachmentRow} — the upstream registration row display
     * names verbatim ("Ceramic Tap" .. "Adamantium Funnel",
     * Loader_MultiTileEntities.java:2108-2120), walked from {@link GT6Attachments#ROWS}
     * so the lang face cannot drift from the registered rows.
     */
    private void addAttachments() {
        // task p20-i18n-compose-rows: the twelve tap/funnel rows compose at runtime — two
        // family templates + the six material words (the attachment namespace: the row words
        // "Stainless"/"Tantalum Hafnium Carbide" differ from the gt6.material locals)
        add(GT6Attachments.TAP_DISPLAY_KEY, "%s Tap");
        add(GT6Attachments.FUNNEL_DISPLAY_KEY, "%s Funnel");
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            addRowMatUnit(GT6Attachments.matUnitKeyOf(tRow), tRow.matDisplay());
        }
        // task p12-gearbox-transformer: the wood kinetic rows (the upstream row wording,
        // "Custom Wooden Gearbox" :1669 / "Wooden Transformer Gearbox" :1668)
        add("block.gt6.gearbox", "Custom Wooden Gearbox");
        add("block.gt6.transformer_rotation", "Wooden Transformer Gearbox");
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
        add("block.gt6.barrel_logistics", "Logistics Tank"); // task p12-barrel-keepfilter-logistics — the :2171 row name verbatim
        add("itemGroup.gt6.fluid_containers", "Fluid Containers");
    }

    /**
     * Electric wire keys (task p7-d2-cable spec ⑥): the two W1 variants (the upstream row
     * names "1x &lt;material&gt; Wire" / "2x ...", MultiTileEntityWireElectric.java:72-73,
     * material-less here) and the "Electric Wires" category tab (the upstream MTE category
     * name, addElectricWires :72).
     *
     * <p>Task p20-i18n-compose-wires (the B-wave lang ruling, ADR
     * 2026-09-06-p20-i18n-zhcn-pipeline §1.4): the 626 per-variant full strings (620
     * electric + 6 redstone, the old GTWireSpecs.displayName rows) RETIRED — the names
     * compose at runtime from five template keys (GTWireBlock.displayNameOf fills
     * {@code gt6.wire.display[.plain]} with the size numeral, the gt6.material.&lt;snake&gt;
     * small unit and the gt6.wire.form.* unit). The ATOMIC forms stay: the two material-less
     * legacy blocks and the material-less laser family ({@code block.gt6.wire_laser},
     * "Laser Fiber Wire" verbatim, Loader:1815) — the arch card's non-composed-form ruling —
     * plus the two tab titles.
     */
    private void addElectricWires() {
        add("block.gt6.wire_electric_1x", "1x Electric Wire");
        add("block.gt6.wire_electric_2x", "2x Electric Wire");
        add("itemGroup.gt6.electric_wires", "Electric Wires");
        // the five position-param templates the runtime composes (electric takes the size
        // slot, redstone is the size-less variant, the form units close every template)
        add(GTWireBlock.DISPLAY_KEY, "%sx %s %s");
        add(GTWireBlock.DISPLAY_PLAIN_KEY, "%s %s");
        add(GTWireBlock.FORM_WIRE_KEY, "Wire");
        add(GTWireBlock.FORM_CABLE_KEY, "Cable");
        add(GTWireBlock.FORM_WIRELAMP_KEY, "Wirelamp");
        // task p10-wire-laser-placeholder — the laser stays ATOMIC (the row is material-less,
        // Loader:1815 verbatim — no composition applies)
        add("block.gt6.wire_laser", "Laser Fiber Wire");
        // review R2: the tier-material backfill so every compose material slot resolves
        addWireRowMaterialNames();
        // task p11-flat-redstone-tab — the two new tab titles, the upstream MTE category
        // strings verbatim: "Redstone Wires" (every Loader:1895-1902 row, tab id 27050) and
        // "Laser Wires" (Loader:1815, tab id 24900) — upstream registers the display via
        // LH.add("itemGroup." + name, aCategoricalName) (CreativeTab.java:35, created at
        // MultiTileEntityRegistry.java:191), so the value is the registration literal.
        add(GTWires.REDSTONE_TAB_TITLE_KEY, "Redstone Wires");
        add(GTWires.LASER_TAB_TITLE_KEY, "Laser Wires");
    }

    /**
     * The wire-row material small-unit backfill (review R2): the compose material slot
     * references {@code gt6.material.<snake>} UNCONDITIONALLY, but {@link #addMaterialNames}
     * walks the REGISTRATION face ({@code mID >= 0} only) — tier materials are created with
     * {@code mID -1} (Superconductor, MT.java:986 {@code tier()}), never reach that walk, and
     * the 16 superconductor variants composed the RAW key ("1x gt6.material.superconductor
     * Wire"). The guard ladder of the material walk is replayed over the same registration
     * source, then every compose-domain row material (the 30 electric + 3 redstone rows,
     * deliberately NOT the material-less laser row) that missed the walk gets its face —
     * currently exactly one data point ("Superconductor", the MT.java:986 local verbatim).
     * The parity test pins ALL 626 variant material keys on the en face, so a future row
     * missing BOTH walks is structurally red instead of a silent raw-key render.
     */
    private void addWireRowMaterialNames() {
        // (a) the shared seam's emitted face (task p20-i18n-compose-rows; Map form since task
        // p21-i18n-walk-seam-normalize — the seam carries the merged material per key, so this
        // backfill membership-checks the walk result instead of keeping its own mutable copy)
        Map<String, OreDictMaterial> tEmitted = materialWalkEmittedKeys();
        // (b) backfill exactly the compose-domain misses (containsKey == was absent from the walk)
        for (List<GTWireSpecs.Row> tRows : List.of(GTWireSpecs.ROWS, GTWireSpecs.REDSTONE_ROWS)) {
            for (GTWireSpecs.Row tRow : tRows) {
                OreDictMaterial tMaterial = tRow.material().get();
                if (tMaterial == null || tMaterial.mNameLocal == null) continue;
                String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tMaterial.mNameInternal);
                if (!tEmitted.containsKey(tKey)) add(tKey, tMaterial.mNameLocal);
            }
        }
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
        // task p20-i18n-compose-rows: the nine tier rows (p8-machine-tiers-doinject ⑧) and
        // the dryer/distillery ladders compose at runtime — one machine template + the three
        // machine words + the ordinal tier units, and one template per row family over the
        // gt6.row.mat small units
        add(gregtech6.registry.GTMachines.MACHINE_DISPLAY_KEY, "%s (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_SHREDDER_UNIT_KEY, "Shredder");
        add(gregtech6.registry.GTMachines.MACHINE_CRUSHER_UNIT_KEY, "Crusher");
        add(gregtech6.registry.GTMachines.MACHINE_LATHE_UNIT_KEY, "Lathe");
        add(gregtech6.registry.GTMachines.machineTierUnitKey(2), "Tier 2");
        add(gregtech6.registry.GTMachines.machineTierUnitKey(3), "Tier 3");
        add(gregtech6.registry.GTMachines.machineTierUnitKey(4), "Tier 4");
        add(gregtech6.registry.GTMachines.DRYER_DISPLAY_KEY, "Dryer (%s)");
        add(gregtech6.registry.GTMachines.DISTILLERY_DISPLAY_KEY, "Distillery (%s)");
        for (String[] tMat : new String[][] {{"steel", "Steel"}, {"invar", "Invar"}, {"titanium", "Titanium"},
                {"tungsten_carbide", "Tungsten Carbide"}}) {
            addRowMatUnit("gt6.row.mat." + tMat[0], tMat[1]); // the MT.DATA.Heat_T[1..4] locals (MT.java:3689)
        }
        // task p16-distillery-family ① — the Integrated Circuit ("Selector Tag", the upstream
        // registration name ItemIntegratedCircuit.java:50 verbatim) + the configuration
        // tooltip ("Configuration: ", the LH line :54/:100; the number rides %s).
        add("item.gt6.integrated_circuit", "Selector Tag");
        add(gregtech6.item.GT6Circuits.TOOLTIP_KEY, "Configuration: %s");
        add("itemGroup.gt6.machines", "Machines");
    }

    /**
     * Multiblock family keys (task p4-multiblock-framework, W3 provider order 1:
     * multiblock→barrel→cover): the Coke Oven controller + bricks part display names (the
     * upstream coke oven bricks MTE 18000 naming) and the "Multiblocks" creative tab.
     * Task p13-large-boiler appends the Large Boiler family: the five variant display
     * names + the five Dense Wall names + the Heat Transmitter (the upstream
     * aRegistry.add name column verbatim, Loader_MultiTileEntities.java
     * :1159-1165/:1176/:1248-1252).
     */
    private void addMultiBlocks() {
        add("block.gt6.multiblock_coke_oven", "Coke Oven");
        add("block.gt6.multiblock_coke_oven_bricks", "Coke Oven Bricks");
        add("itemGroup.gt6.multiblocks", "Multiblocks");
        // task p20-i18n-compose-rows: the Dense Wall + Large Boiler rows compose at runtime
        // (the wall/boiler blocks' getName); the Heat Transmitter stays ATOMIC (a bare noun,
        // the Loader:1176 row verbatim — nothing to compose)
        add(gregtech6.registry.GTMultiBlocks.DENSE_WALL_DISPLAY_KEY, "Dense %s Wall");
        add(gregtech6.registry.GTMultiBlocks.LARGE_BOILER_DISPLAY_KEY, "%s Boiler Main Barometer");
        for (gregtech6.registry.GTMultiBlocks.MultiblockPartRow tRow : gregtech6.registry.GTMultiBlocks.WALL_ROWS) {
            addRowMatUnit(gregtech6.registry.GTMultiBlocks.wallMatUnitKeyOf(tRow), tRow.matDisplay());
        }
        for (gregtech6.registry.GTMultiBlocks.LargeBoilerRow tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
            addRowMatUnit(gregtech6.registry.GTMultiBlocks.boilerMatUnitKeyOf(tRow), tRow.material());
        }
        add("block.gt6." + gregtech6.registry.GTMultiBlocks.TRANSMITTER_ROW.path(),
                gregtech6.registry.GTMultiBlocks.TRANSMITTER_ROW.matDisplay());
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

    /**
     * The replay of the {@link #addMaterialNames} guard ladder — one entry per
     * {@code gt6.material.<snake>} key the registration-face walk emits, valued with the merged
     * material (alias merge, {@code mID >= 0} only, MaterialRegistry.java:182-185;
     * putIfAbsent = the first (lowest mID) definition wins, the walk's dedup semantics
     * verbatim). Single implementation shared by the en table walk ({@link #addMaterialNames}),
     * the wire-row backfill ({@link #addWireRowMaterialNames}) and the zh family join
     * (GT6ZhCn.addMaterialNames — zh needs the material identity for its TSV family lookup,
     * which the old Set form could not carry; task p21-i18n-walk-seam-normalize). The parity
     * test does NOT consume this seam: its keyface guards replay the compose domains against
     * the recording face (the recorded entries), covering this walk indirectly.
     */
    static Map<String, OreDictMaterial> materialWalkEmittedKeys() {
        Map<String, OreDictMaterial> tEmitted = new LinkedHashMap<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias merge, MaterialRegistry.java:182-185
            if (tMaterial == null || tMaterial.mID < 0 || tMaterial.mNameLocal == null) continue;
            tEmitted.putIfAbsent("gt6.material." + MaterialPrefixItem.snakeCase(tMaterial.mNameInternal), tMaterial);
        }
        return Collections.unmodifiableMap(tEmitted);
    }

    /** Table b: one entry per registration-target material (alias slots merged like the bridge). */
    private void addMaterialNames() {
        materialWalkEmittedKeys().forEach((tKey, tMaterial) -> add(tKey, tMaterial.mNameLocal));
    }

    /**
     * The 16 stone-variant display templates (task p20-i18n-compose-rows, the B-wave lang
     * ruling): the 272 pre-installed per-(stone, variant) full strings (task
     * p19-stoneblocks-registry, 17 stones x 16 variants) RETIRED — the name composes at
     * runtime from {@code gt6.stone.variant.<snake>} (one position-param template per
     * {@link StoneVariant}, the stone name slot riding the {@code gt6.material.<snake>}
     * small unit) at {@link gregtech6.block.stone.GTStoneBlock#getName}. The template VALUES
     * are the old {@code StoneVariant.compose} branch ladder %s-ified (the upstream
     * LH.add table BlockStones.java:117-132 verbatim); the zh face follows the dump's
     * {@code gt.stone.andesite.N} family split (tmp/gregtech.lang:15246-15262).
     */
    private void addStoneBlocks() {
        for (StoneVariant tVariant : StoneVariant.VALUES) {
            add(tVariant.key(), STONE_VARIANT_TEMPLATES[tVariant.meta()]);
        }
    }

    /** The 16 variant templates, meta order — the old compose() ladder with the stone name as the {@code %s} slot. */
    private static final String[] STONE_VARIANT_TEMPLATES = {
            "%s",                              // 0  STONE   ".0" = the bare material name
            "%s Cobblestone",                  // 1  COBBL   ".1"
            "Mossy %s Cobblestone",            // 2  MCOBL   ".2"
            "%s Bricks",                       // 3  BRICK   ".3"
            "Cracked %s Bricks",               // 4  CRACK   ".4"
            "Mossy %s Bricks",                 // 5  MBRIK   ".5"
            "Chiseled %s",                     // 6  CHISL   ".6"
            "Smooth %s",                       // 7  SMOTH   ".7"
            "Reinforced %s Bricks",            // 8  RNFBR   ".8"
            "Redstoned %s Bricks",             // 9  RSTBR   ".9"
            "%s Tiles",                        // 10 TILES   ".10"
            "Small %s Tiles",                  // 11 STILE   ".11"
            "Small %s Bricks",                 // 12 SBRIK   ".12"
            "%s Windmill Tiles A",             // 13 WINDA   ".13"
            "%s Windmill Tiles B",             // 14 WINDB   ".14"
            "%s Square Bricks"                 // 15 QBRIK   ".15"
    };
}
