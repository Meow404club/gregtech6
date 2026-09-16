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
import gregtech6.registry.GT6ExtruderMolds;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6FoamSprays;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Sensors;
import gregtech6.registry.GT6SprayCans;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWires;
import gregtech6.covers.GT6Covers;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.RegistryObject;

/**
 * en_us lang, two template tables with no per-item combination explosion (GTCEu data/lang/
 * ItemLang.java:37-54 shape, ADR-P2-4):
 * <ul>
 * <li>{@code gt6.tagprefix.<prefix_snake>} = "{pre}%s{post}" for every OP prefix — the template
 *     the runtime fills via {@code Component.translatable} at {@code getName} time, composed from
 *     the prefix's mMaterialPre/mMaterialPost (OreDictPrefix.java:108, set by setLocalItemName
 *     :258-262), which is upstream GT6's item name composition (mMaterialPre + material name +
 *     mMaterialPost);</li>
 * <li>{@code gt6.material.<material_snake>} = the English local name (mNameLocal) — since task
 *     p23-i18n-material-fill-fix the runtime does NOT pass a mNameLocal literal: the outer
 *     tagprefix template's %s slot is filled with this key as a NESTED translatable unit
 *     ({@link MaterialPrefixItem#materialFill}, MaterialPrefixItem.java:77-79, applied at
 *     {@code getName} :85; {@code GTMaterialPrefixBlockItem.getName} :57-62 shares the same
 *     seam), so each locale resolves the material word in its OWN language — en renders
 *     mNameLocal verbatim (these en values ARE the mNameLocal faces, {@link #addMaterialNames}),
 *     zh renders the localized word;</li>
 * <li>{@code itemGroup.gt6.<prefix_snake>} = one per creative-visible prefix tab, valued with the
 *     prefix's mNameCategory — upstream registers the tab with
 *     {@code LH.add("itemGroup." + mNameInternal, mNameCategory)} (CreativeTab.java:32, created at
 *     PrefixItem.java:90); the tab set is taken from {@link GTMaterialItems#tabPrefixes()} so lang
 *     keys cannot drift from the registered tabs.</li>
 * </ul>
 *
 * <p>The per-item override keys {@code gt6.<prefix>_<material>} are intentionally NOT generated:
 * they are the hand-translation layer, and their absence makes the runtime
 * {@code Language.has(specialKey)} check (MaterialPrefixItem.java:84, the dist-guarded
 * hasTranslation seam :112-116) fall through to the template. LanguageProvider sorts keys
 * (TreeMap) and writes via DataProvider.saveStable, so the output is deterministic across runs.
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
        addItemPipes(); // task p26-pipe-item — the item pipe family
        addEngineFluids();
        addAquaFluids();
        addSimpleLiquidFluids(); // task p19-drying-rows-backfill-2
        addFoodFluids(); // task p21-drying-food-fluids
        addDyeChemicalFluids(); // task p24-dye-chemical-fluids — table-tail append
        addCFoamFluids(); // task p26-c-foam-fluid-refill — table-tail append
        addChemicalFluids(); // task p29-w4-f1-chemicals — table-tail append
        addHotFamilyFluids(); // task p29-w4-hot-lube — table-tail append (hot + closure + lubricant)
        addCFoamBlocks(); // task p26-c-foam-block-family — table-tail append
        addElectricWires();
        addMachines();
        addMultiBlocks();
        addBarrels();
        addKitchen(); // task p26-kitchen-pot-bowl
        addAnvils(); // task p28-c-anvil
        addEnergySource();
        addFeBattery(); // task p26-eu-bridge-outbound — tail-append
        addFeConverter(); // task p28-b-fe-converter-machine — tail-append
        addElectricTransformer(); // task p28-c-ulv-lv-transformer
        addEuBridgeFamilies(); // task p29-w4-eu-bridge — the three EU-bridge families + the Roasting template
        addDynamoFamily(); // task p28-c-ulv-dynamo-row — the W1 family's deferred name face + the T0 row
        addBatteryFamily(); // task p29-w4-battery-storage — the 37+5+7+12 storage face
        addKinetics(); // task p12-engine-crank
        addAttachments(); // task p12-tap-funnel-attachment
        addTools();
        addCovers();
        addJeiInfo();
        addStoneBlocks(); // task p19-stoneblocks-registry — table-tail append (the drying card appends after this)
        addSprayCans(); // task p22-spraycan-items — table-tail append
        addGrassBlocks(); // task p24-grass-block — table-tail append
        addFoamSprays(); // task p25-c-foam-pipe-spray — table-tail append
        addFoodCans(); // task p25-food-can-row0 — table-tail append
        addExtruderMolds(); // task p26-w1-press-extruder-molds — table-tail append
        addSensors(); // task p26-sensors-core — table-tail append
        addCrucibleJade(); // task p28-crucible-jade-face — table-tail append
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
     * Food family keys (task p21-drying-food-fluids): the third loop over
     * {@link GTFluids#FOOD_FLUID_SPECS} — the addSimpleLiquidFluids shape, walked from the
     * THIRD table so the lang face cannot drift from the registered fluids. Values ride
     * the row's displayName: "Maple Sap" / "Reedwater" / "Cactuswater" verbatim from the
     * Loader_Fluids.java:461-463 {@code FL.create} rows, and "Sap" the FL.java:250
     * shorthand spelled out for the external-name fluid (no {@code FL.create}, the
     * water_boiling "Boiling Water" precedent). The upstream 1.7.10 lang key
     * {@code potion.reedwater} (FL.java:233) is NOT transcribed — the port
     * descriptionId convention (the declared deviation). No zh_cn rows: the reference
     * table's hand layer carries no food-fluid entries yet (the aqua/simple-liquid
     * precedent — the runtime falls back to English per key), so the zh provider walk
     * would emit nothing.
     */
    private void addFoodFluids() {
        for (GTFluids.AquaFluidSpec tSpec : GTFluids.FOOD_FLUID_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
    }

    /**
     * Dye-chemical family + chlorine keys (task p24-dye-chemical-fluids): one description
     * entry per {@link GTFluids.DyeChemicalFluid} row — the exact descriptionId the FluidType
     * is registered with, walked from {@link GTFluids#DYE_CHEMICALS} so the lang face cannot
     * drift from the registered fluids. Values ride the row's displayName: the upstream
     * Loader_Fluids.java:123 {@code FL.create} local-name compose verbatim
     * ({@code "Chemical " + DYE_NAMES[i] + " Dye"}); chlorine is the MT.Cl material local
     * name (MT.java:405, the createGas naming face). Unlike the three earlier fluid
     * families the dump DOES carry all 17 zh faces (tmp/gregtech.lang:230-244/:168), so
     * the zh side rides the reference table's hand layer (the GT6ZhCn mirror walk).
     */
    private void addDyeChemicalFluids() {
        for (GTFluids.DyeChemicalFluid tFamily : GTFluids.DYE_CHEMICALS) {
            add(tFamily.descriptionId(), tFamily.displayName());
        }
        add("fluid.gt6.chlorine", "Chlorine");
    }

    /**
     * C-Foam family keys (task p26-c-foam-fluid-refill): the base + one description entry
     * per {@link GTFluids.CFoamFluid} row — the exact descriptionIds the FluidTypes are
     * registered with, walked from {@link GTFluids#CFOAMS}/{@link GTFluids#CFOAMS_OWNED}
     * so the lang face cannot drift from the registered fluids. Values ride the row's
     * displayName: the upstream Loader_Fluids.java:124/:125 compose verbatim
     * ({@code ["Advanced "] + DYE_NAMES[i] + " C-Foam"}); the base is the FL.java:432
     * material local name "Construction Foam" (the dump's zh face 建筑泡沫, the
     * decisions.p26-cfoam-fluid-naming naming ruling). Unlike the dye-chemical family the
     * dump carries all 33 zh faces (tmp/gregtech.lang:130-161/:361), so the zh side rides
     * the reference table's hand layer (the GT6ZhCn mirror walk).
     */
    private void addCFoamFluids() {
        add("fluid.gt6.cfoam", "Construction Foam");
        for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS) {
            add(tFamily.descriptionId(), tFamily.displayName());
        }
        for (GTFluids.CFoamFluid tFamily : GTFluids.CFOAMS_OWNED) {
            add(tFamily.descriptionId(), tFamily.displayName());
        }
    }

    /**
     * Chemical family keys (task p29-w4-f1-chemicals): one description entry per
     * {@link GTFluids.ChemicalFluidSpec} row — the exact descriptionId the FluidType is
     * registered with, walked from {@link GTFluids#CHEMICAL_SPECS} so the lang face cannot
     * drift from the registered fluids. Values ride the row's displayName: the upstream
     * display names verbatim — the oils' {@code FL.create} local strings
     * (Loader_Fluids.java:59-63 "Very Heavy Oil"/"Heavy Oil"/"Raw Oil"/"Light Oil"/
     * "Soulsand Oil"), the plasma literals (:40-41 "Helium Plasma"/"Nitrogen Plasma"),
     * "Liquid Oxygen" (:68), and the createGas material local names for the rest
     * (FL.java:1080 — the MT.java mNameLocal face, "Propane"/"Methane"/"Carbon Dioxide"/
     * "Hydrogen"/"Nitrogen"/the noble gases). zh values ride the reference table's hand
     * layer (the GT6ZhCn mirror walk; the dump carries all 25 faces — tmp/gregtech.lang
     * S:fluid.* rows cited on the zh side).
     */
    private void addChemicalFluids() {
        for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CHEMICAL_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
    }

    /**
     * Hot family keys (task p29-w4-hot-lube): one description entry per
     * {@link GTFluids.ChemicalFluidSpec} row across the THREE tables this card appends —
     * the twelve hot fluids ({@link GTFluids#HOT_FLUID_SPECS}, the upstream
     * Loader_Fluids.java:85-99 local strings verbatim: "Industrial Coolant"/"Industrial
     * Heatant"/"Hot Molten Sodium"/... /"Pahoehoe Lava"), the seven FM.Hot closure carriers
     * ({@link GTFluids#CLOSURE_FLUID_SPECS}, the createMolten "Molten Sodium"/"Molten Tin"/
     * "Molten Lithium Chloride" + createLiquid "Heavy Water"/"Semiheavy Water"/
     * "Tritiated Water" local names + :195 "Blazing Goo"), and the F-2 lubricant row
     * ({@link GTFluids#LUBRICANT_FLUID_SPECS}, :617 "Lubricant"). zh values ride the
     * reference table's hand layer (the GT6ZhCn mirror walk; the dump carries the faces —
     * tmp/gregtech.lang S:fluid.* rows cited on the zh side, the molten-licl display the
     * one hand row following the molten.X=熔融X dump rule).
     */
    private void addHotFamilyFluids() {
        for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.HOT_FLUID_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
        for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.CLOSURE_FLUID_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
        for (GTFluids.ChemicalFluidSpec tSpec : GTFluids.LUBRICANT_FLUID_SPECS) {
            add(tSpec.descriptionId(), tSpec.displayName());
        }
    }

    /**
     * The C-Foam BLOCK family display names (task p26-c-foam-block-family; the owned row is
     * task p28-cfoam-lang-key): the two dried forms carry BlockItems, the two fresh forms +
     * the owned carrier are item-less and resolve their block keys (Jade/breaking overlays
     * resolve block.gt6.* through getDescriptionId — the owned row was MISSING from this walk
     * until p28, so Jade showed the raw key on the foam a player sprayed). Values are the
     * upstream display names verbatim where they exist: "Fresh C-Foam"
     * (BlockCFoamFresh.java:47) and "C-Foam" (BlockCFoam.java:38); the owned carrier takes
     * "Advanced C-Foam" (user ruling 2026-09-12 — the upstream owned foam is exactly what
     * the "Advanced C-Foam Spray" sprays, MultiItemRandomTools.java:259, matching the owned
     * FLUID family's Advanced word); the slab forms take the vanilla "&lt;name&gt; Slab"
     * composition. zh values ride the reference table's hand layer (the GT6ZhCn mirror
     * walk; zh owned = 强化建筑泡沫 per the same ruling).
     */
    private void addCFoamBlocks() {
        add("block.gt6.cfoam_fresh", "Fresh C-Foam");
        add("block.gt6.cfoam", "C-Foam");
        add("block.gt6.cfoam_owned", "Advanced C-Foam");
        add("block.gt6.cfoam_fresh_slab", "Fresh C-Foam Slab");
        add("block.gt6.cfoam_slab", "C-Foam Slab");
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
     * Task p24-tool-system: the file + saw display names ("File"/"Saw", the upstream
     * TOOL_LOCALISER rows CS.java:1095/:1094 verbatim).
     * Task p24-screwdriver-item: the screwdriver display name ("Screwdriver", the
     * upstream TOOL_LOCALISER row CS.java:1102 verbatim — also the Loader_Tools.java:129
     * registration-row wording).
     * Task p10-tool-creative-tab: the "Tools" creative tab title (the upstream ToolsGT
     * meta-tool category, Loader_Tools.java:114-145 registration rows; the key comes from
     * GT6Tools.TAB_TITLE_KEY so the lang face cannot drift from the registered tab —
     * the offline test pins the literal on both sides).
     */
    private void addTools() {
        add("item.gt6.crowbar", "Crowbar");
        add("item.gt6.cutter", "Wire Cutter");
        add("item.gt6.chisel", "Chisel");
        add("item.gt6.file", "File");
        add("item.gt6.saw", "Saw");
        // task p24-builder-wand: the builder wand display name (the upstream
        // registration row wording "Builder Wand", Loader_Tools.java:153 verbatim,
        // matching the TOOL_LOCALISER face CS.java:1112)
        add("item.gt6.builder_wand", "Builder Wand");
        add("item.gt6.screwdriver", "Screwdriver");
        // task p25-tool-hammer-wrench: the hammer + wrench display names (the upstream
        // registration-row wordings "Hammer"/"Wrench", Loader_Tools.java:124/:126
        // verbatim, matching the TOOL_LOCALISER faces CS.java:1096/:1083)
        add("item.gt6.hammer", "Hammer");
        add("item.gt6.wrench", "Wrench");
        // task p29-w5-t1-dig-six — the six dig-tool display names (the upstream
        // registration-row wordings verbatim: "Construction Pick" Loader_Tools.java:147,
        // "Gem tipped Pickaxe" :151, "Pickaxe"/"Shovel"/"Spade" the TOOL_LOCALISER rows
        // CS.java:1093-1097 and the :339-341 recipe-row family)
        add("item.gt6.pickaxe", "Pickaxe");
        add("item.gt6.pickaxe_gem", "Gem tipped Pickaxe");
        add("item.gt6.pickaxe_construction", "Construction Pick");
        add("item.gt6.shovel", "Shovel");
        add("item.gt6.spade", "Spade");
        add("item.gt6.universal_spade", "Universal Spade");
        // task p29-w5-t2-blade-six — the six blade-tool display names (the upstream
        // registration-row wordings verbatim: "Sword" :118, "Knife" :135, "Butchery Knife"
        // :136, "Club" :130, "Axe" :122, "Double Axe" :123) + the four non-empty desc rows
        // (the Loader_Tools third column, the TOOL_LOCALISER tooltip face)
        add("item.gt6.sword", "Sword");
        add("item.gt6.knife", "Knife");
        add("item.gt6.butchery_knife", "Butchery Knife");
        add("item.gt6.club", "Club");
        add("item.gt6.axe", "Axe");
        add("item.gt6.axe_double", "Double Axe");
        add("item.gt6.axe.tooltip", "Faster on Logs. Chops down whole Trees.");
        add("item.gt6.axe_double.tooltip", "Chops down whole Trees and has a slow Attack Rate");
        add("item.gt6.club.tooltip", "A blunt primitive Weapon and Rock Crusher");
        add("item.gt6.butchery_knife.tooltip", "Has a slow Attack Rate");
        // task p29-w5-t4-field-five — the five field-tool display names (the upstream
        // registration-row wordings verbatim: "Hoe" Loader_Tools.java:122, "Branch Cutter"
        // :133, "Sense" :138, "Plow" :139, "Hand Drill" :152)
        add("item.gt6.hoe", "Hoe");
        add("item.gt6.branch_cutter", "Branch Cutter");
        add("item.gt6.sense", "Sense");
        add("item.gt6.plow", "Plow");
        add("item.gt6.hand_drill", "Hand Drill");
        // task p29-w5-t5-scene-six — the six scene-tool display names (the upstream
        // registration-row wordings verbatim: "Scoop" Loader_Tools.java:132, "Plunger"
        // :140, "Rolling Pin" :141, "Flint and Tinder" :143, "Bending Cylinder" :145,
        // "Scissors" :149)
        add("item.gt6.scissors", "Scissors");
        add("item.gt6.scoop", "Scoop");
        add("item.gt6.plunger", "Plunger");
        add("item.gt6.flint_and_tinder", "Flint and Tinder");
        add("item.gt6.rolling_pin", "Rolling Pin");
        add("item.gt6.bending_cylinder", "Bending Cylinder");
        // task p29-w5-t6-electric-nineteen — the nineteen electric-tool display names
        // (the upstream registration-row wordings verbatim, Loader_Tools.java:156-174) +
        // the fourteen tooltips (the row tooltip strings verbatim, :159-171 + the
        // Behavior_Switch_Metadata mode line) = the 33-key band.
        add("item.gt6.mining_drill_lv", "Mining Drill (LV)");
        add("item.gt6.mining_drill_mv", "Mining Drill (MV)");
        add("item.gt6.mining_drill_hv", "Mining Drill (HV)");
        add("item.gt6.chainsaw_lv", "Chainsaw (LV)");
        add("item.gt6.chainsaw_lv.tooltip", "Can also harvest Ice");
        add("item.gt6.chainsaw_mv", "Chainsaw (MV)");
        add("item.gt6.chainsaw_mv.tooltip", "Can also harvest Ice");
        add("item.gt6.chainsaw_hv", "Chainsaw (HV)");
        add("item.gt6.chainsaw_hv.tooltip", "Can also harvest Ice");
        add("item.gt6.wrench_lv", "Wrench (LV)");
        add("item.gt6.wrench_lv.tooltip", "Sneak Rightclick to switch to Monkey Wrench");
        add("item.gt6.wrench_mv", "Wrench (MV)");
        add("item.gt6.wrench_mv.tooltip", "Sneak Rightclick to switch to Monkey Wrench");
        add("item.gt6.wrench_hv", "Wrench (HV)");
        add("item.gt6.wrench_hv.tooltip", "Sneak Rightclick to switch to Monkey Wrench");
        add("item.gt6.jackhammer_hv_normal", "JackHammer (HV, Normal Mode)");
        add("item.gt6.jackhammer_hv_normal.tooltip", "Breaks Rocks into pieces");
        add("item.gt6.jackhammer_hv_no_ores", "JackHammer (HV, No Ores Mode)");
        add("item.gt6.jackhammer_hv_no_ores.tooltip", "Doesn't break Ore Blocks, except Layer Ores");
        add("item.gt6.buzzsaw_lv", "Buzzsaw (LV)");
        add("item.gt6.buzzsaw_lv.tooltip", "Not suitable for harvesting Blocks");
        add("item.gt6.screwdriver_lv", "Screwdriver (LV)");
        add("item.gt6.hand_drill_lv", "Hand Drill (LV)");
        add("item.gt6.hand_mixer_lv", "Hand Mixer (LV)");
        add("item.gt6.hand_mixer_lv.tooltip", "Doesn't consume exhaustion in the Mixing Bowls");
        add("item.gt6.monkey_wrench_lv", "Monkey Wrench (LV)");
        add("item.gt6.monkey_wrench_lv.tooltip", "Sneak Rightclick to switch to Wrench");
        add("item.gt6.monkey_wrench_mv", "Monkey Wrench (MV)");
        add("item.gt6.monkey_wrench_mv.tooltip", "Sneak Rightclick to switch to Wrench");
        add("item.gt6.monkey_wrench_hv", "Monkey Wrench (HV)");
        add("item.gt6.monkey_wrench_hv.tooltip", "Sneak Rightclick to switch to Wrench");
        add("item.gt6.trimmer_lv", "Trimmer (LV)");
        add("item.gt6.mode_switch.tooltip", "Sneak Rightclick to switch Mode");
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
     * FE battery fixture key (task p26-eu-bridge-outbound, tail-append): the block display
     * name — the receiving end of the EU->FE outbound bridge acceptance chain.
     */
    private void addFeBattery() {
        add("block.gt6.fe_battery", "FE Test Battery");
    }

    /**
     * FE converter family keys (task p28-b-fe-converter-machine, tail-append): the ULV
     * machine display name — the inbound FE→EU machine — and the fe_source fixture name.
     */
    private void addFeConverter() {
        add("block.gt6.fe_converter", "FE Converter (ULV)");
        add("block.gt6.fe_source", "FE Test Source");
    }

    /**
     * The Electric Transformer display key (task p28-c-ulv-lv-transformer): the :881
     * registration wording "Transformer (ULV-LV)" verbatim — the atomic key (the
     * fe_converter shape; a single block, no row template to compose).
     */
    private void addElectricTransformer() {
        add("block.gt6.electric_transformer", "Transformer (ULV-LV)");
    }

    /**
     * The EU-bridge display rows (task p29-w4-eu-bridge): the three converter families'
     * atomic keys over the upstream name columns "Electric Heater (LV)" / "Electric
     * Engine (LV)" / "Electric Motor (LV)" (Loader :817-821/:833-837/:849-853, VN[1..5]),
     * plus the Roasting Oven one-slot template (the upstream name column
     * "Roasting Oven ("+aMat.getLocal()+")" :1386-1389 — the Heat_T material-word slot,
     * the OVEN/MACHINE_SMELTER_UNIT_KEY contract). The converters are row-less blocks
     * (the GT6DynamoBlock carrier), so their names are the plain atomic-key form.
     */
    private void addEuBridgeFamilies() {
        add(gregtech6.registry.GTMachines.MACHINE_ROASTING_OVEN_UNIT_KEY, "Roasting Oven (%s)");
        for (String[] tFace : new String[][] {
                {"electric_heater", "Electric Heater"}, {"electric_engine", "Electric Engine"}, {"electric_motor", "Electric Motor"}}) {
            String[] tWords = {"LV", "MV", "HV", "EV", "IV"};
            add("block.gt6." + tFace[0], tFace[1] + " (" + tWords[0] + ")");
            add("block.gt6." + tFace[0] + "_t2", tFace[1] + " (" + tWords[1] + ")");
            add("block.gt6." + tFace[0] + "_t3", tFace[1] + " (" + tWords[2] + ")");
            add("block.gt6." + tFace[0] + "_t4", tFace[1] + " (" + tWords[3] + ")");
            add("block.gt6." + tFace[0] + "_t5", tFace[1] + " (" + tWords[4] + ")");
        }
    }

    /**
     * The Dynamo family display rows (task p28-c-ulv-dynamo-row — the W1 name face the
     * family card declared deferred, landed now that the registry-coverage gate walks both
     * registration classes): the upstream registration names verbatim — "Electric Dynamo
     * ("+VN[n]+")" over Loader_MultiTileEntities.java:946-950 and "Flux Dynamo
     * ("+aMat.getLocal()+")" over :953-957, the local words Enderium Base / Enderium the
     * MT.java:2541/:2544 mNameLocal faces. The T0 row is the declared "Electric Dynamo
     * (ULV)" extension (VN[0], the invented id).
     */
    private void addDynamoFamily() {
        add("block.gt6.electric_dynamo_ulv", "Electric Dynamo (ULV)");
        add("block.gt6.electric_dynamo",    "Electric Dynamo (LV)");
        add("block.gt6.electric_dynamo_t2", "Electric Dynamo (MV)");
        add("block.gt6.electric_dynamo_t3", "Electric Dynamo (HV)");
        add("block.gt6.electric_dynamo_t4", "Electric Dynamo (EV)");
        add("block.gt6.electric_dynamo_t5", "Electric Dynamo (IV)");
        add("block.gt6.flux_dynamo",        "Flux Dynamo (Lead)");
        add("block.gt6.flux_dynamo_t2",     "Flux Dynamo (Invar)");
        add("block.gt6.flux_dynamo_t3",     "Flux Dynamo (Electrum)");
        add("block.gt6.flux_dynamo_t4",     "Flux Dynamo (Enderium Base)");
        add("block.gt6.flux_dynamo_t5",     "Flux Dynamo (Enderium)");
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
    private void addBatteryFamily() {
		add("item.gt6.battery_lead_acid_ulv", "Lead-Acid Battery (ULV)");
		add("item.gt6.battery_lead_acid_lv", "Lead-Acid Battery (LV)");
		add("item.gt6.battery_lead_acid_mv", "Lead-Acid Battery (MV)");
		add("item.gt6.battery_lead_acid_hv", "Lead-Acid Battery (HV)");
		add("item.gt6.battery_lead_acid_ev", "Lead-Acid Battery (EV)");
		add("item.gt6.battery_alkaline_ulv", "Alkaline Battery (ULV)");
		add("item.gt6.battery_alkaline_lv", "Alkaline Battery (LV)");
		add("item.gt6.battery_alkaline_mv", "Alkaline Battery (MV)");
		add("item.gt6.battery_alkaline_hv", "Alkaline Battery (HV)");
		add("item.gt6.battery_alkaline_ev", "Alkaline Battery (EV)");
		add("item.gt6.battery_nicd_ulv", "Nickel-Cadmium Battery (ULV)");
		add("item.gt6.battery_nicd_lv", "Nickel-Cadmium Battery (LV)");
		add("item.gt6.battery_nicd_mv", "Nickel-Cadmium Battery (MV)");
		add("item.gt6.battery_nicd_hv", "Nickel-Cadmium Battery (HV)");
		add("item.gt6.battery_nicd_ev", "Nickel-Cadmium Battery (EV)");
		add("item.gt6.battery_licoo2_ulv", "Lithium-Cobalt Battery (ULV)");
		add("item.gt6.battery_licoo2_lv", "Lithium-Cobalt Battery (LV)");
		add("item.gt6.battery_licoo2_mv", "Lithium-Cobalt Battery (MV)");
		add("item.gt6.battery_licoo2_hv", "Lithium-Cobalt Battery (HV)");
		add("item.gt6.battery_licoo2_ev", "Lithium-Cobalt Battery (EV)");
		add("item.gt6.battery_limn_ulv", "Lithium-Manganese Battery (ULV)");
		add("item.gt6.battery_limn_lv", "Lithium-Manganese Battery (LV)");
		add("item.gt6.battery_limn_mv", "Lithium-Manganese Battery (MV)");
		add("item.gt6.battery_limn_hv", "Lithium-Manganese Battery (HV)");
		add("item.gt6.battery_limn_ev", "Lithium-Manganese Battery (EV)");
		add("item.gt6.energium_red_ulv", "Red Energium Crystal (T0)");
		add("item.gt6.energium_red_lv", "Red Energium Crystal (T1)");
		add("item.gt6.energium_red_mv", "Red Energium Crystal (T2)");
		add("item.gt6.energium_red_hv", "Red Energium Crystal (T3)");
		add("item.gt6.energium_red_ev", "Red Energium Crystal (T4)");
		add("item.gt6.energium_red_iv", "Red Energium Crystal (T5)");
		add("item.gt6.energium_cyan_ulv", "Cyan Energium Crystal (T0)");
		add("item.gt6.energium_cyan_lv", "Cyan Energium Crystal (T1)");
		add("item.gt6.energium_cyan_mv", "Cyan Energium Crystal (T2)");
		add("item.gt6.energium_cyan_hv", "Cyan Energium Crystal (T3)");
		add("item.gt6.energium_cyan_ev", "Cyan Energium Crystal (T4)");
		add("item.gt6.energium_cyan_iv", "Cyan Energium Crystal (T5)");
		add("item.gt6.battery_cell_lead_acid", "Lead-Acid Cell (Filled)");
		add("item.gt6.battery_cell_alkaline", "Alkaline Button Cell (Filled)");
		add("item.gt6.battery_cell_nicd", "Nickel-Cadmium Cell (Filled)");
		add("item.gt6.battery_cell_licoo2", "Lithium-Cobalt Cell (Filled)");
		add("item.gt6.battery_cell_limn", "Lithium-Manganese Cell (Filled)");
		add("item.gt6.circuit_primitive", "Primitive Circuit");
		add("item.gt6.circuit_basic", "Basic Electronic Circuit");
		add("item.gt6.circuit_good", "Good Electronic Circuit");
		add("item.gt6.circuit_advanced", "Advanced Electronic Circuit");
		add("item.gt6.circuit_elite", "Elite Electronic Circuit");
		add("item.gt6.circuit_master", "Master Electronic Circuit");
		add("item.gt6.circuit_ultimate", "Ultimate Electronic Circuit");
		add("block.gt6.battery_box_ulv", "Battery Box (ULV)");
		add("block.gt6.battery_box_lv", "Battery Box (LV)");
		add("block.gt6.battery_box_mv", "Battery Box (MV)");
		add("block.gt6.battery_box_hv", "Battery Box (HV)");
		add("block.gt6.battery_box_ev", "Battery Box (EV)");
		add("block.gt6.battery_box_iv", "Battery Box (IV)");
		add("block.gt6.battery_box_large_ulv", "Large Battery Box (ULV)");
		add("block.gt6.battery_box_large_lv", "Large Battery Box (LV)");
		add("block.gt6.battery_box_large_mv", "Large Battery Box (MV)");
		add("block.gt6.battery_box_large_hv", "Large Battery Box (HV)");
		add("block.gt6.battery_box_large_ev", "Large Battery Box (EV)");
		add("block.gt6.battery_box_large_iv", "Large Battery Box (IV)");
    }

    private void addKinetics() {
        add("block.gt6.crank", "Hand Crank");
        // task p28-c-water-wheel — the kTFRUAddon registration wording ("Water Mill",
        // tileEntityInit0.java:112) in its natural English noun form; the atomic key (the
        // crank shape — a single block, no row template to compose)
        add("block.gt6.water_wheel", "Water Wheel");
        // task p20-i18n-compose-rows — the axle/steam/diesel/burning-box/boiler families
        // compose at runtime (the family blocks' getName); the pre-installed full strings
        // retired into ONE family template + the row-material / size / family-word small
        // units, walked from the row tables so the faces cannot drift from the registries
        addAxleUnits();
        addSteamEngineUnits();
        addDieselUnits();
        addBurningBoxUnits();
        addBoilerUnits();
        addHopperUnits();
        addStaticStorageUnits();
    }

    /**
     * The static storage batch display rows (task p26-storage-static-batch): one full
     * display per row over the vanilla description id — the metal names carry the loader
     * display words verbatim ("Mechanical Bronze Safe", Loader :134-135), the wooden
     * ladders the plank word (the 300-ladder fold, the wave-4 deviation).
     */
    private void addStaticStorageUnits() {
        for (gregtech6.registry.GT6StaticStorages.StaticRow tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
            String tName = switch (tRow.kind()) {
                case LOCKER -> tRow.material().display() + " Locker";
                case DRAWER -> tRow.material().display() + " Compartment Drawer";
                case SAFE_MECHANICAL -> "Mechanical " + tRow.material().display() + " Safe";
                case SAFE_KEYLOCKED -> "Key Locked " + tRow.material().display() + " Safe";
                case BOOKSHELF -> tRow.plank().display() + " Wooden Bookshelf";
                case BOTTLECRATE -> tRow.plank().display() + " Wooden Bottlecrate";
            };
            add("block.gt6." + tRow.path(), tName);
        }
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

    /** The two hopper templates (regular/queue) + the row-material units (the GT6Hoppers.ROWS walk; Bronze/Steel join the boiler words, Loader:191/:202). */
    private void addHopperUnits() {
        add(gregtech6.registry.GT6Hoppers.DISPLAY_KEY, "%s Hopper");
        add(gregtech6.registry.GT6Hoppers.DISPLAY_QUEUE_KEY, "%s Queue Hopper");
        for (gregtech6.registry.GT6Hoppers.HopperRow tRow : gregtech6.registry.GT6Hoppers.ROWS) {
            addRowMatUnit(gregtech6.registry.GT6Hoppers.matUnitKeyOf(tRow), tRow.material().display());
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
    /**
     * The kitchen family keys (task p26-kitchen-pot-bowl): the three manual blocks and
     * the raw bowl item, display names VERBATIM from the upstream registration rows
     * ("Wooden Bathing Pot" :2173, "Bathing Pot" :2175, "Ceramic Bowl" :2177 — all
     * "Misc Tool Blocks") plus the "Clay Bowl" raw item (MultiItemRandomTools.java:119).
     * Task p27-lang-fix — the TAB RETIREMENT: the "GT6 Kitchen" tab face
     * (itemGroup.gt6.kitchen) is gone per the user ruling (the pot/bowl rows are
     * processing machines, not cookware; no renamed successor) — the four items ride
     * the machines tab through the GT6Kitchen BuildCreativeModeTabContentsEvent join,
     * so this provider carries no tab key for the family anymore.
     *
     * <p>Task p29-w3-heat-smelter lang rider (decisions.p29-mixingbowl-ruling, the user
     * ruling 2026-09-14): the en display of the mixing_bowl row had DRIFTED to the
     * upstream registration literal "Ceramic Bowl" while the zh face always carried the
     * correct 搅拌盆 ("Mixing Bowl") — the en face is corrected to the Mixing Bowl
     * semantics here, keeping the three surfaces (GT6EnUs / GT6ZhCn / the TSV) on one
     * 口径. Deliberate deviation from the :2177 name column, ruling-backed.
     */
    private void addKitchen() {
        add("block.gt6.bathing_pot_wood", "Wooden Bathing Pot");
        add("block.gt6.bathing_pot_steel", "Bathing Pot");
        add("block.gt6.mixing_bowl", "Mixing Bowl"); // decisions.p29-mixingbowl-ruling — the en drift corrected
        add("item.gt6.clay_bowl", "Clay Bowl");
    }

    /**
     * The anvil family keys (task p28-c-anvil): the two stone anvil blocks, display
     * names VERBATIM from the upstream registration rows (aMat.mNameLocal + " Anvil",
     * Loader_MultiTileEntities.java:2185-2186 — "Stone Anvil" / "Blackstone Anvil",
     * both "Misc Tool Blocks"). The items ride the machines tab (the kitchen join form),
     * no tab key here.
     */
    private void addAnvils() {
        add("block.gt6.stone_anvil", "Stone Anvil");
        add("block.gt6.blackstone_anvil", "Blackstone Anvil");
    }

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
     * by task p7-basicmachine-family; the Oven ladder joins in task p27-oven-heat-t-ladder):
     * the machine family display names (upstream
     * rows "Shredder"/"Crusher"/"Lathe", :1294/:1300/:1306) and the "Machines" creative tab
     * (the upstream MTE-registry category of the same rows). The retired atomic
     * "block.gt6.oven" joined the composed face (upstream name column
     * "Oven ("+Heat_T local+")", :1288-1291 — the OVEN_DISPLAY_KEY template).
     */
    private void addMachines() {
        add("block.gt6.shredder", "Shredder");
        add("block.gt6.crusher", "Crusher");
        add("block.gt6.lathe", "Lathe");
        // task p20-i18n-compose-rows: the nine tier rows (p8-machine-tiers-doinject ⑧) and
        // the dryer/distillery ladders compose at runtime — one machine template + the three
        // machine words, and one template per row family over the gt6.row.mat small units.
        // task p27-machine-energy-display-fix: the tier slot rides the Kinetic_T MATERIAL
        // word (upstream "Shredder ("+aMat.getLocal()+")" :1294-1309, Kinetic_T[1..4]
        // MT.java:3690) — the ordinal gt6.row.tier.* units are RETIRED (the T2-T4 blocks
        // fill the slot with the shared gt6.row.mat.* words below)
        add(gregtech6.registry.GTMachines.OVEN_DISPLAY_KEY, "Oven (%s)"); // task p27-oven-heat-t-ladder
        add(gregtech6.registry.GTMachines.MACHINE_DISPLAY_KEY, "%s (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_SHREDDER_UNIT_KEY, "Shredder");
        add(gregtech6.registry.GTMachines.MACHINE_CRUSHER_UNIT_KEY, "Crusher");
        add(gregtech6.registry.GTMachines.MACHINE_LATHE_UNIT_KEY, "Lathe");
        add(gregtech6.registry.GTMachines.DRYER_DISPLAY_KEY, "Dryer (%s)");
        add(gregtech6.registry.GTMachines.DISTILLERY_DISPLAY_KEY, "Distillery (%s)");
        // task p24-canner-machine — the Canner family template: the upstream name column
        // "Canning Machine ("+VN[tier]+")" (Loader_MultiTileEntities.java:1379-1382;
        // CS.java:154 the LV/MV/HV/EV voltage ladder — the row mat units are the VOLTAGE
        // ids, not a material word like the Heat_T families below)
        add(gregtech6.registry.GTMachines.CANNER_DISPLAY_KEY, "Canning Machine (%s)");
        // task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio: the MACHINE_DISPLAY_KEY
        // template face of the card spec lands as the three unit keys in the :101-104 shape;
        // the MachineRow carrier fills exactly ONE slot (the tier rides the Kinetic_T material
        // word), so each unit key's value carries the family template (the one-slot
        // CANNER_DISPLAY_KEY contract, not the two-slot MACHINE_DISPLAY_KEY form)
        add(gregtech6.registry.GTMachines.MACHINE_SIFTER_UNIT_KEY, "Sifter (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_COMPRESSOR_UNIT_KEY, "Compressor (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_WIREMILL_UNIT_KEY, "Wiremill (%s)");
        // task p26-w1-press-extruder-molds — the Press + Extruder family templates and the
        // :101-104 unit words (the T1 Extruder word differs upstream: "Low Heat Extruder",
        // :1406, vs "Extruder" :1407-1409)
        add(gregtech6.registry.GTMachines.PRESS_DISPLAY_KEY, "Press (%s)");
        add(gregtech6.registry.GTMachines.EXTRUDER_DISPLAY_KEY, "Extruder (%s)");
        add(gregtech6.registry.GTMachines.EXTRUDER_LOW_HEAT_DISPLAY_KEY, "Low Heat Extruder (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_PRESS_UNIT_KEY, "Press");
        add(gregtech6.registry.GTMachines.MACHINE_EXTRUDER_UNIT_KEY, "Extruder");
        add(gregtech6.registry.GTMachines.MACHINE_EXTRUDER_LOW_HEAT_UNIT_KEY, "Low Heat Extruder");
        // task p28-c-ulv-machine-ladder — the ULV face: the one-slot templates the new row
        // carriers compose with (Shredder/Crusher get their first row-carrier template —
        // their T1-T4 siblings stay tierOf tierName; Rolling Mill is the new family, the
        // upstream name column "Rolling Mill ("+aMat.getLocal()+")" Loader:1349-1352 form)
        add(gregtech6.registry.GTMachines.MACHINE_SHREDDER_DISPLAY_KEY, "Shredder (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_CRUSHER_DISPLAY_KEY, "Crusher (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_ROLLING_MILL_UNIT_KEY, "Rolling Mill (%s)");
        // task p29-w1-kinetic-roll-ladder — the roll-ladder families: the one-slot
        // templates the row carriers compose with (the upstream name columns
        // "Roll Bender (" / "Roll Former (" / "Cluster Mill (" + aMat.getLocal(),
        // Loader:1355-1358 / :1361-1364 / :1367-1370 form)
        add(gregtech6.registry.GTMachines.MACHINE_ROLL_BENDER_UNIT_KEY, "Roll Bender (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_ROLL_FORMER_UNIT_KEY, "Roll Former (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_CLUSTER_MILL_UNIT_KEY, "Cluster Mill (%s)");
        // task p29-w1-kinetic-process-ladder — the six process-family templates (the
        // :101-104 one-slot unit-key form; the upstream name columns verbatim: "Buzzsaw ("
        // :1318-1321 / "Squeezer (" :1324-1327 / "Centrifuge (" :1330-1333 / "Sluice ("
        // :1464-1467 / "Sanding Machine (" :1589-1592 / "Pressure Washer (" :1615-1618)
        add(gregtech6.registry.GTMachines.MACHINE_BUZZSAW_UNIT_KEY, "Buzzsaw (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_SQUEEZER_UNIT_KEY, "Squeezer (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_CENTRIFUGE_UNIT_KEY, "Centrifuge (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_SLUICE_UNIT_KEY, "Sluice (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_SANDING_UNIT_KEY, "Sanding Machine (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_PRESSURE_WASHER_UNIT_KEY, "Pressure Washer (%s)");
        // task p29-w1-eu-hu-families — the eu-hu family templates: the Mixer rides the
        // one-slot unit-key form (the Kinetic_T material word, the upstream name column
        // "Mixer ("+aMat+")" :1392-1395); the five Electric* ladders ride the voltage-word
        // slot (the CANNER_DISPLAY_KEY contract, the VN ladder — the upstream "Electric
        // Mixer ("+VN[tier]+")" :1504-1508 name-column form); the Fermenter is the
        // single-variant ATOMIC row (upstream name column "Fermenter", :1654 — the
        // template carries no %s slot)
        add(gregtech6.registry.GTMachines.MACHINE_MIXER_UNIT_KEY, "Mixer (%s)");
        add(gregtech6.registry.GTMachines.ELECTRIC_MIXER_DISPLAY_KEY, "Electric Mixer (%s)");
        add(gregtech6.registry.GTMachines.ELECTRIC_LOOM_DISPLAY_KEY, "Electric Loom (%s)");
        add(gregtech6.registry.GTMachines.ELECTRIC_SIFTER_DISPLAY_KEY, "Electric Sifter (%s)");
        add(gregtech6.registry.GTMachines.BOXINATOR_DISPLAY_KEY, "Boxinator (%s)");
        add(gregtech6.registry.GTMachines.UNBOXINATOR_DISPLAY_KEY, "Unboxinator (%s)");
        add(gregtech6.registry.GTMachines.FERMENTER_DISPLAY_KEY, "Fermenter");
        // task p29-w2-eu-special — the eu-special family templates: the two EU ladders ride
        // the voltage-word slot (the CANNER_DISPLAY_KEY contract — the upstream name columns
        // "Autocrafter ("+VN[tier]+")" :1497-1501 / "Lightning Processor ("+VN[tier]+")"
        // :1582-1586; the T5 word is IV, VN[5], the card-① erratum face) and the Laminator
        // rides the Heat_T material-word slot (upstream "Laminator ("+aMat.getLocal()+")",
        // :1532-1535)
        add(gregtech6.registry.GTMachines.AUTOCRAFTER_DISPLAY_KEY, "Autocrafter (%s)");
        add(gregtech6.registry.GTMachines.LIGHTNING_PROCESSOR_DISPLAY_KEY, "Lightning Processor (%s)");
        add(gregtech6.registry.GTMachines.LAMINATOR_DISPLAY_KEY, "Laminator (%s)");
        // task p29-w2-exotic-energy — the six exotic-energy family templates: the
        // Polarizer/Magnetic Separator ride the material-word slot (the upstream name
        // columns "Polarizer ("+aMat+")" :1418-1422 / "Magnetic Separator ("+aMat+")"
        // :1470-1474 over Electric_T[1..5]); the Laser Engraver / Laser Welder / Freezer /
        // Cryo Mixer ride the LITERAL tier-word slot "(T1)".."(T5)" (:1483/:1490/:1621/
        // :1628 — the declaration-fidelity column, no voltage word)
        add(gregtech6.registry.GTMachines.MACHINE_POLARIZER_UNIT_KEY, "Polarizer (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_MAGNETIC_SEPARATOR_UNIT_KEY, "Magnetic Separator (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_LASER_ENGRAVER_UNIT_KEY, "Laser Engraver (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_LASER_WELDER_UNIT_KEY, "Laser Welder (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_FREEZER_UNIT_KEY, "Freezer (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_CRYO_MIXER_UNIT_KEY, "Cryo Mixer (%s)");
        // task p29-w2-hu-tu-piggyback — the seven hu-tu family templates: the two Crackers
        // and the Loom ride the one-slot unit-key form (the Kinetic/Heat_T material word,
        // the upstream name columns "Steam Cracker (" :1576-1579 / "Catalytic Cracker ("
        // :1570-1573 / "Loom (" :1412-1415); the TU four are the single-variant ATOMIC
        // rows (upstream name columns "Coagulator" :1651 / "Generifier" :1652 / "Bath"
        // :1653 / "Autoclave" :1655 — the FERMENTER_DISPLAY_KEY no-slot contract)
        add(gregtech6.registry.GTMachines.MACHINE_STEAM_CRACKER_UNIT_KEY, "Steam Cracker (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_CATALYTIC_CRACKER_UNIT_KEY, "Catalytic Cracker (%s)");
        add(gregtech6.registry.GTMachines.MACHINE_LOOM_UNIT_KEY, "Loom (%s)");
        add(gregtech6.registry.GTMachines.COAGULATOR_DISPLAY_KEY, "Coagulator");
        add(gregtech6.registry.GTMachines.GENERIFIER_DISPLAY_KEY, "Generifier");
        add(gregtech6.registry.GTMachines.BATH_DISPLAY_KEY, "Bath");
        add(gregtech6.registry.GTMachines.AUTOCLAVE_DISPLAY_KEY, "Autoclave");
        // task p29-w3-heat-smelter — the two heat families: the Smelter rides the
        // one-slot unit-key form (the Heat_T material word, the upstream name column
        // "Smelter ("+aMat+")" :1431-1434); the Melter is the single-variant ATOMIC row
        // (the upstream name column "Melter" :1657 — the FERMENTER_DISPLAY_KEY no-slot
        // contract)
        add(gregtech6.registry.GTMachines.MACHINE_SMELTER_UNIT_KEY, "Smelter (%s)");
        add(gregtech6.registry.GTMachines.MELTER_DISPLAY_KEY, "Melter");
        // task p24-act-machine — the single-variant row (upstream "Advanced Crafting Table",
        // Loader_MultiTileEntities.java:136 name column)
        add("block.gt6.advanced_crafting_table", "Advanced Crafting Table");
        // task p29-w2-eu-core-5tier — the eu-core family templates: the voltage-word slot
        // (the CANNER_DISPLAY_KEY contract) over the FIVE-word VN ladder LV/MV/HV/EV/IV
        // (the upstream name columns "Electrolyzer ("+VN[tier]+")" :1336-1340 / "Injector ("
        // :1443-1447 / "Printer (" :1450-1454 / "Scanner (Visuals, " :1457-1461 — the comma
        // form verbatim / "Slicer (" :1525-1529; VN[5] = "IV", CS.java:154 — the S9 ruling,
        // task p29-w2-eu-core-5tier — the eu-core family templates: the voltage-word slot
        // NOT "EV": ev is T4's word since p24)
        add(gregtech6.registry.GTMachines.ELECTROLYZER_DISPLAY_KEY, "Electrolyzer (%s)");
        add(gregtech6.registry.GTMachines.INJECTOR_DISPLAY_KEY, "Injector (%s)");
        add(gregtech6.registry.GTMachines.PRINTER_DISPLAY_KEY, "Printer (%s)");
        add(gregtech6.registry.GTMachines.SCANNER_VISUALS_DISPLAY_KEY, "Scanner (Visuals, %s)");
        add(gregtech6.registry.GTMachines.SLICER_DISPLAY_KEY, "Slicer (%s)");
        for (String[] tMat : new String[][] {{"steel", "Steel"}, {"invar", "Invar"}, {"titanium", "Titanium"},
                {"tungsten_carbide", "Tungsten Carbide"}, {"bronze", "Bronze"}, {"tungstensteel", "Tungstensteel"},
                {"lv", "LV"}, {"mv", "MV"}, {"hv", "HV"}, {"ev", "EV"}, {"iv", "IV"},
                {"any_wood", "Any Wood"}, {"ulv", "ULV"},
                // task p29-w2-exotic-energy — the Electric_T[1]/[2]/[4] locals (Galvanized
                // Steel/Aluminium/Chromium, MT.java:1731/:401/:412 setLocal faces; the
                // stainless_steel/titanium rungs ride the units above) + the LITERAL
                // "(T1)".."(T5)" tier words of the four T-named exotic families
                {"galvanized_steel", "Galvanized Steel"}, {"aluminium", "Aluminium"}, {"chromium", "Chromium"},
                {"t1", "T1"}, {"t2", "T2"}, {"t3", "T3"}, {"t4", "T4"}, {"t5", "T5"}}) {
            addRowMatUnit("gt6.row.mat." + tMat[0], tMat[1]); // the Heat_T[1..4] locals (MT.java:3689) + the VN[1..4] ids (CS.java:154) + the Kinetic_T[1..4] locals (the W1 trio) + the p28 ULV rungs (the T0 material word + VN[0]) + the p29 W2 5-tier rung (VN[5] = "IV" — the card gloss said EV, refuted by the CS.java:154 array and by ev already being T4's word; the GTMachines.EV_TIER_INPUTS doc carries the erratum)
        }
        // task p16-distillery-family ① — the Integrated Circuit ("Selector Tag", the upstream
        // registration name ItemIntegratedCircuit.java:50 verbatim) + the configuration
        // tooltip ("Configuration: ", the LH line :54/:100; the number rides %s).
        add("item.gt6.integrated_circuit", "Selector Tag");
        add(gregtech6.item.GT6Circuits.TOOLTIP_KEY, "Configuration: %s");
        add("itemGroup.gt6.machines", "Machines");
        // task p29-w4-hot-lube ④ — the Lubricant Bucket (the architect ruling's naming
        // candidate) + its "Industrial Use ONLY!" tooltip (the upstream :617 drink
        // description verbatim).
        add("item.gt6.lubricant_bucket", "Lubricant Bucket");
        add(gregtech6.item.GT6LubricantBucket.TOOLTIP_KEY, "Industrial Use ONLY!");
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
        // task p24-lightning-rod — the controller + the three atomic part names (the Loader
        // :1282/:1151/:1168/:1179 name column verbatim) and the addToolTips replay keys (the
        // upstream :88-115 LH rows verbatim; the zh wording rides the dump
        // tmp/gregtech.lang :17758-17766 via the reference table)
        // task p26-crucible-multiblock — the LARGE crucible family (the Loader :1145/:1270
        // name column verbatim; the single-rung Steel ladder, the composed-row template
        // defers with the 8-material pool)
        add("block.gt6.crucible_steel_wall", "Steel Wall");
        add("block.gt6.crucible_steel", "Large Steel Crucible");
        // task p29-w3-distill-crucible — the crucible ladder (the Loader :1271-1277 name
        // column verbatim; the ladder walls compose "<mat> Wall" over the EXISTING
        // gt6.row.mat words — zero new unit keys) and the twin towers (:1226-1227 verbatim)
        add("block.gt6.crucible_stainless_steel", "Large Stainless Steel Crucible");
        add("block.gt6.crucible_invar", "Large Invar Crucible");
        add("block.gt6.crucible_titanium", "Large Titanium Crucible");
        add("block.gt6.crucible_tungstensteel", "Large Tungstensteel Crucible");
        add("block.gt6.crucible_tungsten", "Large Tungsten Crucible");
        add("block.gt6.crucible_tantalum_hafnium_carbide", "Large Tantalum Hafnium Carbide Crucible");
        add("block.gt6.crucible_adamantium", "Large Adamantium Crucible");
        add("block.gt6.distillation_tower", "Distillation Tower");
        add("block.gt6.cryo_distillation_tower", "Cryo Distillation Tower");
        add("block.gt6.multiblock_lightning_rod", "Lightning Rod Electric Output");
        // task p29-w3-heat-smelter — the Large Heat Exchanger controller (the :1245 name column)
        add("block.gt6.large_heat_exchanger", "Large Heat Exchanger");
        add("block.gt6.machine_wall_tungsten", "Tungsten Wall");
        add("block.gt6.niobium_titanium_coil", "Large Niobium-Titanium Coil");
        add("block.gt6.lightning_rod", "Lightning Rod");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_STRUCTURE, "Structure:");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(1), "Bottom: 3x3 of Tungsten Walls with Main at Center");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(2), "Then: Full 3x3 of Large Niobium-Titanium Coils");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(3), "Then: Full 3x3 of Tungsten Walls");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(4), "Then: Full 3x3 of Large Niobium-Titanium Coils");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(5), "Top: 3x3 of Tungsten Walls");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(6), "Centered Above: 1x1 Pillar of simple Lightning Rod Blocks");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(7), "The Tip of the Rod has to be at Y = 100 or above");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(8), "Optimum Efficiency at a Rod Length of 100m");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_LINE.formatted(9), "Reduced Efficiency if too close to another Lightning Rod (256m)");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_ENERGY, "%s EU/p (up to 16 Amps)");
        add(gregtech6.block.multiblock.GTLightningRodBlock.Item.KEY_CAPACITY, "%s EU per Lightning Strike");
        // task p29-w3-nbtdesign-parts — the part-family expansion: the METAL WALL rows
        // compose "<mat> Wall" over the EXISTING gt6.row.mat words (zero new unit keys);
        // every other new row is ATOMIC (the Loader :1138-1189 name column verbatim)
        add(gregtech6.registry.GTMultiBlocks.PartRow.METAL_WALL_DISPLAY_KEY, "%s Wall");
        for (gregtech6.registry.GTMultiBlocks.PartRow tRow : gregtech6.registry.GTMultiBlocks.NEW_PART_ROWS) {
            if (!tRow.metalWall()) {
                add("block.gt6." + tRow.path(), tRow.display());
            }
        }
        // task p29-w3-tank-valves — the 25 Tank Main Valve rows compose over the size words
        // and the EXISTING gt6.row.mat wall-row words (all six metal materials already in the
        // table); the wood valve takes the ONE-slot template over the new wood unit word
        add(gregtech6.registry.GT6Tanks.TANK_VALVE_DISPLAY_KEY, "%s %s Tank Main Valve");
        add(gregtech6.registry.GT6Tanks.TANK_VALVE_WOOD_DISPLAY_KEY, "%s Tank Main Valve");
        add(gregtech6.registry.GT6Tanks.WOOD_UNIT_KEY, "Wood");
        add(gregtech6.registry.GT6Tanks.SIZE_SMALL, "Small");
        add(gregtech6.registry.GT6Tanks.SIZE_SMALL_DENSE, "Small Dense");
        add(gregtech6.registry.GT6Tanks.SIZE_LARGE, "Large");
        add(gregtech6.registry.GT6Tanks.SIZE_LARGE_DENSE, "Large Dense");
        // task p29-w3-turbine-dynamo — the twelve controller names (the Loader
        // :1254-1257/:1259-1262/:1264-1267 name column verbatim, ATOMIC rows)
        for (gregtech6.registry.GT6Turbines.SteamTurbineRow tRow : gregtech6.registry.GT6Turbines.STEAM_ROWS) {
            add("block.gt6." + tRow.path(), tRow.display());
        }
        for (gregtech6.registry.GT6Turbines.GasTurbineRow tRow : gregtech6.registry.GT6Turbines.GAS_ROWS) {
            add("block.gt6." + tRow.path(), tRow.display());
        }
        for (gregtech6.registry.GT6DynamoHousings.DynamoRow tRow : gregtech6.registry.GT6DynamoHousings.DYNAMO_ROWS) {
            add("block.gt6." + tRow.path(), tRow.display());
        }
        // task p29-w3-large-12 — the twelve large machines (the Loader :1229-1240 name
        // column verbatim, the row walk over GT6LargeMachines.ROWS)
        for (gregtech6.registry.GT6LargeMachines.LargeMachineRow tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            add("block.gt6." + tRow.path(), tRow.display());
        }
    }

    /**
     * Example machine keys (task p3-example-machine): the chest block display name and the
     * "chests" creative tab title. The tab carries the upstream MTE-registry category name
     * ("Chests", Loader_MultiTileEntities.java:132 / MultiTileEntityRegistry.java:191); the
     * block name resolves through MenuProvider#getDisplayName (block.getName).
     *
     * <p>The two TestMachine framework blocks join here (task p28-cfoam-lang-key, the
     * registry-coverage gate's discovery run): they have NO getName override, so their Jade
     * name line resolves the vanilla {@code block.gt6.test_machine[_idle]} keys — which no
     * provider face ever wrote (the same omission class as cfoam_owned). Faces are the
     * TestMachineBlock javadoc's own vocabulary ("a ticking and a passive variant"); the
     * idle id carries "(Passive)" (the no-tick chain equivalent, TestMachineBlock.java:17-19).
     */
    private void addExampleMachine() {
        add("block.gt6.example_chest", "GT Example Chest");
        add("block.gt6.test_machine", "Test Machine");
        add("block.gt6.test_machine_idle", "Test Machine (Passive)");
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

    /**
     * Item pipe keys (task p26-pipe-item): the six variant templates over the three
     * material words (the composed display — the upstream name columns
     * Loader_MultiTileEntities.java:1823-1843 + MultiTileEntityPipeItem.java:77-82; the
     * dump word set tmp/gregtech.lang:11849-11866 keeps 物流管道 for the zh face) and the
     * "Item Pipes" category tab (itemGroup.gt.multitileentity.25202).
     */
    private void addItemPipes() {
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "medium", "%s Item Pipe");
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "large", "Large %s Item Pipe");
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "huge", "Huge %s Item Pipe");
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "restrictive_medium", "Restrictive %s Item Pipe");
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "restrictive_large", "Restrictive Large %s Item Pipe");
        add(gregtech6.registry.GTItemPipes.DISPLAY_KEY_PREFIX + "restrictive_huge", "Restrictive Huge %s Item Pipe");
        addRowMatUnit(gregtech6.registry.GTItemPipes.MAT_BRASS.unitKey(), "Brass");
        addRowMatUnit(gregtech6.registry.GTItemPipes.MAT_CONSTANTAN.unitKey(), "Constantan");
        addRowMatUnit(gregtech6.registry.GTItemPipes.MAT_COBALT_BRASS.unitKey(), "Cobalt Brass");
        add("itemGroup.gt6.item_pipes", "Item Pipes");
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

    /**
     * Spray-can family keys (task p22-spraycan-items): the 18 item display names + the family
     * tab title + the three tooltip templates. The item names are the upstream registration
     * rows verbatim — "Spray Paint (Black)".."Spray Paint (White)"
     * (MultiItemRandomTools.java:243), "Paint Removal Spray" (:269), "Empty Spray Can" (:235)
     * — walked from the {@link GT6SprayCans} registry face (the RegistryObject id is the lang
     * key path) so the two faces cannot drift. The tooltip templates ride the item constants
     * (GTSprayCanItem.PAINT_TOOLTIP_KEY/DECOLOR_TOOLTIP_KEY/REMAINING_TOOLTIP_KEY) with the
     * upstream LH wordings (:56 "Can Color things in ", Remover :109 "Can Decolor things",
     * :170 + the :179 "X.Y" format "Remaining Uses: ").
     */
    private void addSprayCans() {
        for (int i = 0; i < 16; i++) {
            add("item.gt6." + GT6SprayCans.SPRAY_PAINTS.get(i).getId().getPath(),
                "Spray Paint (" + gregtech6.item.spraycan.GTSprayCanItem.DYE_NAMES[i] + ")");
        }
        add("item.gt6." + GT6SprayCans.SPRAY_PAINT_REMOVER.getId().getPath(), "Paint Removal Spray");
        add("item.gt6." + GT6SprayCans.SPRAY_CAN_EMPTY.getId().getPath(), "Empty Spray Can");
        add(GT6SprayCans.TAB_TITLE_KEY, "Spray Cans");
        add(gregtech6.item.spraycan.GTSprayCanItem.PAINT_TOOLTIP_KEY, "Can Color things in %s");
        add(gregtech6.item.spraycan.GTSprayCanItem.DECOLOR_TOOLTIP_KEY, "Can Decolor things");
        add(gregtech6.item.spraycan.GTSprayCanItem.REMAINING_TOOLTIP_KEY, "Remaining Uses: %s.%s");
    }

    /**
     * Food-can family keys (task p25-food-can-row0): the row0 MINIMAL subset's display
     * names, walked over the {@link GT6FoodCans} registry constants so the lang face
     * cannot drift from the registered ids (the addSprayCans form). Values are the
     * upstream registration-row wordings verbatim: "Empty Food Can"
     * (MultiItemRandomTools.java:234), the six rotten tiers "Tiny/Small/Tall/Wide/Large/
     * Huge Food Can (Rotten)" (MultiItemCans.java:53-58), the cookies tin "Huge Food Can
     * (Cookies)" (:107) and the bending cylinder "Small Bending Cylinder"
     * (Loader_Tools.java:146), plus the tab title ("GregTech: Cans", the upstream
     * MultiItemCans.java:41 category label; the key comes from
     * {@link GT6FoodCans#TAB_TITLE_KEY} so the lang face cannot drift from the
     * registered tab).
     */
    private void addFoodCans() {
        add("item.gt6." + GT6FoodCans.FOOD_CAN_EMPTY.getId().getPath(), "Empty Food Can");
        String[] tSizes = {"tiny", "small", "tall", "wide", "large", "huge"};
        String[] tNames = {"Tiny", "Small", "Tall", "Wide", "Large", "Huge"};
        for (int i = 0; i < 6; i++) {
            add("item.gt6." + GT6FoodCans.FOOD_CAN_ROTTEN.get(i).getId().getPath(),
                tNames[i] + " Food Can (Rotten)");
            if (!GT6FoodCans.FOOD_CAN_ROTTEN.get(i).getId().getPath().equals("food_can_rotten_" + tSizes[i])) {
                throw new IllegalStateException("rotten can id drifted: " + GT6FoodCans.FOOD_CAN_ROTTEN.get(i).getId().getPath());
            }
        }
        add("item.gt6." + GT6FoodCans.FOOD_CAN_COOKIES_HUGE.getId().getPath(), "Huge Food Can (Cookies)");
        add("item.gt6.bending_cylinder_small", "Small Bending Cylinder");
        // task p29-w5-t3-machine-face-four: the machine-face four display names (the
        // upstream registration-row wordings "Soft Hammer"/"Monkey Wrench"/"Magnifying
        // Glass"/"Pincers", Loader_Tools.java:125/:144/:148/:150 verbatim)
        add("item.gt6.soft_hammer", "Soft Hammer");
        add("item.gt6.monkey_wrench", "Monkey Wrench");
        add("item.gt6.magnifying_glass", "Magnifying Glass");
        add("item.gt6.pincers", "Pincers");
        add(GT6FoodCans.TAB_TITLE_KEY, "GregTech: Cans");
    }

    /**
     * Extruder-mold family keys (task p26-w1-press-extruder-molds): the row0 MINIMAL
     * subset's display names, walked over the {@link GT6ExtruderMolds} registry constants
     * so the lang face cannot drift from the registered ids (the addFoodCans form).
     * Values are the upstream registration-row wordings verbatim: "Extruder Shape (Plate)"
     * (MultiItemTechnological.java:186) and "Extruder Shape (Rod)" (:212).
     * Table-tail append, append-only.
     */
    private void addExtruderMolds() {
        for (RegistryObject<Item> tMold : GT6ExtruderMolds.MOLDS) {
            String tPath = tMold.getId().getPath();
            if (tPath.equals("shape_extruder_plate")) {
                add("item.gt6." + tPath, "Extruder Shape (Plate)");
            } else if (tPath.equals("shape_extruder_rod")) {
                add("item.gt6." + tPath, "Extruder Shape (Rod)");
            } else {
                throw new IllegalStateException("extruder mold id drifted: " + tPath);
            }
        }
    }

    /**
     * Grass family keys (task p24-grass-block): 8 rows — the 6 variant display names and
     * the 2 tooltip lines. The 6 block items resolve the VANILLA BlockItem descriptionId
     * face ({@code block.gt6.<registry-path>}); all six names are the SAME word — the
     * upstream 16 meta keys are all named "Grass" (BlockGrass.java:49-64, no colour
     * prefix; the SPEC parity ruling keeps it: "Grass"/zh "草方块", the usable-name
     * question stays a later i18n card). The tooltips ride the upstream LH keys verbatim
     * (the GTGrassBlock.TOOLTIP_KEY/TOOLTIP_SPRAY_KEY constants, BlockGrass.java
     * :86-87 — the :92-93 CYAN/GRAY colours live in the BLOCK class, not the lang).
     * Table-tail append, append-only.
     */
    private void addGrassBlocks() {
        for (String tPath : gregtech6.registry.GTGrassBlocks.PATHS) {
            add("block.gt6." + tPath, "Grass");
        }
        add(gregtech6.block.GTGrassBlock.TOOLTIP_KEY,
            "Does not spread, get eaten, change color nor need light");
        add(gregtech6.block.GTGrassBlock.TOOLTIP_SPRAY_KEY,
            "Spray Paint can also be used to dye Grass!");
    }

    /**
     * C-Foam spray family keys (task p25-c-foam-pipe-spray spec ①): 35 rows — the 32 item
     * display names (16 C-Foam Sprays + 16 Advanced owned variants), the family tab and the
     * two tooltip templates. The item names are the upstream registration rows verbatim —
     * "C-Foam Spray (Black)".."C-Foam Spray (White)" (MultiItemRandomTools.java:251) and
     * "Advanced C-Foam Spray (...)" (:259) — walked from the {@link GT6FoamSprays} registry
     * face (the RegistryObject id is the lang key path, the addSprayCans shape). The
     * tooltips ride the item constants with the upstream wordings (Behavior_Spray_Foam ctor
     * :59 "Can place " + DYE_NAMES + " C-Foam"; the :259 "C-Foam only breakable by Owner
     * once dry"); the remaining-uses line reuses the p22 REMAINING_TOOLTIP_KEY. Table-tail
     * append, append-only.
     */
    private void addFoamSprays() {
        for (int i = 0; i < 16; i++) {
            add("item.gt6." + GT6FoamSprays.FOAM_SPRAYS.get(i).getId().getPath(),
                "C-Foam Spray (" + gregtech6.item.spraycan.GTSprayCanItem.DYE_NAMES[i] + ")");
            add("item.gt6." + GT6FoamSprays.FOAM_SPRAYS_OWNED.get(i).getId().getPath(),
                "Advanced C-Foam Spray (" + gregtech6.item.spraycan.GTSprayCanItem.DYE_NAMES[i] + ")");
        }
        add(GT6FoamSprays.TAB_TITLE_KEY, "C-Foam Sprays");
        add(gregtech6.item.foamspray.GT6FoamSprayItem.FOAM_TOOLTIP_KEY, "Can place %s C-Foam");
        add(gregtech6.item.foamspray.GT6FoamSprayItem.OWNED_TOOLTIP_KEY, "C-Foam only breakable by Owner once dry");
    }

    /**
     * Sensor family keys (task p26-sensors-core): the three pioneer display names — the
     * upstream registration rows verbatim (Loader_MultiTileEntities.java :1995 "Progress
     * Sensor", :1986 "Fluid-O-Meter Sensor", :1997 "Electrometer Sensor"), walked from the
     * {@link GT6Sensors#ROWS} face (the row path IS the lang key tail — the
     * {@code SensorRow#displayKey} composition, the addSprayCans walk shape). The upstream
     * tooltip stack (Sensor:89-97 — the NO_GUI / screwdriver / monkey-wrench lines) is the
     * lang card's face and stays out of this card (the BE javadoc cut list).
     * Table-tail append, append-only.
     */
    private void addSensors() {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            add(tRow.displayKey(), switch (tRow.path()) {
                case "progressmeter" -> "Progress Sensor";      // Loader :1995
                case "fluidometer"   -> "Fluid-O-Meter Sensor"; // Loader :1986
                case "electrometer"  -> "Electrometer Sensor";  // Loader :1997
                default -> throw new IllegalArgumentException("untranslated sensor row: " + tRow.path());
            });
        }
    }

    /**
     * The crucible Jade face keys (task p28-crucible-jade-face, 4 keys — the final key set
     * after the TFRU 33c22beb ruling): the temperature line (current/max K, the thermometer
     * anchor MultiTileEntitySmeltery.java:512), the content total line (its "Content" label
     * IS the TFRU LH.CONTENT prefix form — the total row is the label row, the item rows
     * below are indented details), the empty state and the "+N more" truncation tail. The
     * Formed line is NOT here — the large crucible's formed state already rides the
     * GT6MachineProvider "Multiblock: formed/incomplete" row. Values are consumed by
     * GT6CrucibleProvider (the lang constants live there).
     */
    private void addCrucibleJade() {
        add("gt6.jade.crucible.temperature", "Temperature: %s K / %s K");
        add("gt6.jade.crucible.total", "Content: %s U");
        add("gt6.jade.crucible.empty", "Empty");
        add("gt6.jade.crucible.more", "+%s more");
    }
}
