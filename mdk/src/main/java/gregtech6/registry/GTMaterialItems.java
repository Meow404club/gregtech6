package gregtech6.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegisterEvent;
//? if forge {
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredHolder;
// 21.1: RegistryObject → DeferredHolder (one extra generic parameter, javap
// neoforge-21.1.249); not swap-able, forked per file (ADR-P15-3 r1 priority 3).
 *///?}

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.item.MaterialPrefixItem;

/**
 * Registration bridge (ADR-P2-2 / ADR-P2-3, full-prefix expansion per task fullprefix-creativetab):
 * floods the GT6 material system during FMLConstructModEvent.enqueueWork, then injects the material
 * prefix items into the RegisterEvent stream (LOW priority, GTCEu GTRegistrate.java:148-151 precedent)
 * plus one creative tab per creative-visible prefix in the same listener. FMLCommonSetupEvent then
 * completes the alloy reverse references (GT6 postInit equivalent, GT_API_Post.java:816-820) — that
 * part lives in GT6Mod.
 *
 * <p>Item ids follow the GT6 oredict reading order: {@code gt6:<prefix_snake>_<material_snake>}
 * (e.g. gt6:ingot_iron) — deliberately the reverse of GTCEu's material_prefix shape (ADR-P2-2).
 *
 * <p><b>Registration universe</b> (measured 2026-08-30, jshell over MT.init/OP.init): the per-material
 * criterion alone ({@link OreDictPrefix#isGeneratingItem}, upstream PrefixItem.java:104) does NOT
 * separate item families from parse-only or block families — 468 prefixes x isGeneratingItem yields
 * 476,183 pairs, because ~142 PREFIX_UNUSED prefixes (default condition TRUE) and every ore/block/
 * wire/crate family (real conditions) pass it. Upstream gates by registration PATH instead: material
 * ITEMS exist only for the prefixes handed a PrefixItem in Loader_Items.java:57-171 (105 prefixes;
 * ores/blocks/stones/crates go through PrefixBlock, pipes/wires through MultiTileEntities, and
 * identical-name aliases exist purely for oredict parsing) — plus the four casingMachine* block-family
 * prefixes the port deliberately registers as items (task casing-machine-register, the deviation
 * declared on {@link #itemPathPrefixes}). The plank prefix RODE this list as the second block-path
 * item adaptation (task wood-planks-register) and was RETIRED from it (task planks-blockification:
 * upstream ships planks ONLY as blocks — Loader_Woods.java:62-65 BlockTreePlanks/2, Loader_Items
 * carries zero plank rows — and the port now matches, the plank face being the {@link GT6WoodDict}
 * BlockItem rows). The sixteen wire multipliers RODE this list as
 * the third declared deviation (task wire-gt-registration) and were RETIRED from it (task
 * wiregt-prefix-item-retirement, the user-ruled upstream-fidelity regression 2026-10-06: the lift
 * flooded the creative screen with sixteen per-multiplier tabs of model-less wire items plus the
 * rubber wire rows — upstream ships wireGt01-16 ONLY on the MTE block path,
 * MultiTileEntityWireElectric.java:72-87, one OreDictManager.setTarget_ oredient row per multiplier;
 * upstream Loader_Items.java:57-171 carries ZERO wireGt rows). This card owns the item path, so the
 * universe is that 109-prefix list — referenced via OP fields for compile-checked existence — while
 * the walk below iterates ALL of {@link OreDictPrefix#VALUES} and explicitly skips the non-item paths
 * (aggregate counter in the registration log) so the deferral stays visible and auditable.
 * Block/MTE families are later cards; PREFIX_UNUSED aliases never generate.
 *
 * <p><b>id collision policy = first-wins</b>, identical rule to the phase-2 lang collision
 * (compressed/Compressed both snake to "compressed", first registration wins, task datagen-pipeline).
 * Within the item universe the census found zero id collisions (the only snake_case collision pair in
 * all of OP is compressed/Compressed, both outside the item path), so {@link #firstWinsById} is pinned
 * by unit tests on synthetic pairs instead of by runtime evidence.
 *
 * <p><b>Creative tabs</b> mirror upstream gregapi/item/CreativeTab.java semantics: one tab per
 * non-HIDDEN prefix (PrefixItem.java:89-91 {@code mPrefix.mCreativeTab = new CreativeTab(mNameInternal,
 * mNameCategory, ...)}) with the tab title = the prefix's mNameCategory and items filtered by !mHidden
 * (PrefixItem.java:114 with SHOW_HIDDEN_MATERIALS=false). HIDDEN prefixes (ingotHot, scrapGt, plantGt*)
 * register items but no tab — upstream SHOW_HIDDEN_PREFIXES=false leaves them invisible
 * (PrefixItem.java:89/114). Prefixes with zero registrable items get no tab (upstream's empty
 * dustImpure-style tab is a dead entry; its getSubItems adds nothing and ST.hide hides the item).
 */
public final class GTMaterialItems {

    /** Runtime index (prefix, material) -> handle, in registration order (creative tab display order). */
    //? if forge {
    private static final Map<PrefixMaterial, RegistryObject<Item>> INDEX = new LinkedHashMap<>();
    //?} else {
    /*private static final Map<PrefixMaterial, DeferredHolder<Item, Item>> INDEX = new LinkedHashMap<>();
     *///?}
    /** Defensive dedup across re-fired RegisterEvents (ADR-P2-2 fix 1). */
    private static final Set<ResourceLocation> REGISTERED_IDS = new HashSet<>();

    /** Index key: an OreDictPrefix x OreDictMaterial item pair. */
    public record PrefixMaterial(OreDictPrefix prefix, OreDictMaterial material) {}

    private GTMaterialItems() {
    }

    /** Segment 1 (FMLConstructModEvent.enqueueWork): full material system refill, registry open -> closed. */
    public static void initMaterials() {
        MaterialRegistry.INSTANCE.open(); // createMaterial with a valid ID requires open (MaterialRegistry.java:130-131)
        MT.init();  // MT.java:2695, per-generation full refill (reg0000..reg0038 + AM/ANY/TECH/OREMATS/WOODS/UNUSED)
        OP.init();  // OP.java:621, idempotent; requires PrefixRegistry open (OreDictPrefix.createPrefix :117)
        forceItemGeneration(); // the OP.java:603-625 force-table rows a port card consumes; see the method doc
        MaterialRegistry.INSTANCE.close(); // ADR-P2-2 open->closed; GTCEu CommonProxy.java:185-229 unfreeze->init->freeze isomorph
        GT6Mod.LOGGER.info("GT6 material system initialised: {} materials, {} prefixes", MaterialRegistry.INSTANCE.MATERIAL_MAP.size(), OreDictPrefix.VALUES.size());
    }

    /**
     * The force-table slice (task machines-bumblelyzer-crucible, extended by
     * toolhead-family-closeout) — the upstream OP.java:603-625 disable/forceItemGeneration
     * rows the port OP defers with the whole MT/ANY-dependent block, landed HERE (mdk-side,
     * caller-of-OP.init timing) because a port row consumes the items. The
     * material-condition-system "47 unlocked prefixes" is the same pattern: the port keeps
     * the upstream model and lands the table rows per consuming card. Exactly four rows,
     * each upstream verbatim:
     * <ul>
     * <li>{@code :616 bouleGt.forceItemGeneration(MT.Si, MT.Ge, MT.RedstoneAlloy, MT.NikolineAlloy)}
     *     — the boule quartet;</li>
     * <li>the {@code :624} ANY.Sapphire loop face — the ruling takes the 39 crystallisation-row
     *     output materials as the real count (Loader_Recipes_Other.java:683-706): the base
     *     Sapphire plus the six coloured sapphires. (The Hexorium loop :625 stays with the
     *     cutting-domain card — no port row consumes a Hexorium boule.)</li>
     * <li>{@code :619 plateTiny.forceItemGeneration(MT.Paper)} — the Bumblelyzer scan leg's
     *     paper tiny.</li>
     * <li>{@code :621 toolHeadPickaxeGem.forceItemGeneration(MT.Empty)} (task
     *     toolhead-family-closeout) — the blank gem pickaxe head: the base of the press
     *     retip row (Loader_Recipes_Handlers.java:251) and the return of the head-drop ring
     *     (GT_Tool_PickaxeGem.java:30 getBrokenItem). The :618-:620 drill/chainsaw/wrench
     *     blanks stay unlanded — no port row consumes them (GTMaterialItemsForceTest pins
     *     the scope).</li>
     * </ul>
     * Plus the gem-family Ice rows (task gem-ice-force-rows):
     * <ul>
     * <li>{@code :613/:614 gemChipped/gemFlawed.forceItemGeneration(MT.Ice)} — upstream forces
     *     the chipped/flawed Ice gems because Ice carries G_GEM_TRANSPARENT but NOT CRYSTAL,
     *     so the {@code And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT)} condition (OP.java:1223-1224)
     *     rejects it without the force (upstream OP.java:613-614; the same rows also force
     *     NaCl/KCl/KIO3/Firestone/Sugar — those stay with the per-consuming-card ruling, no
     *     port row consumes them, and none carries TRANSPARENT anyway).</li>
     * </ul>
     * The un-skip cascade (research.dust-tiny-ice-dead-rows id1688, the "candidate tail card"
     * section): the two items unlock six suppressed recipe rows — melter/smelter
     * gemChipped/gemFlawed Ice legs (Loader_Recipes_Chem.java:483-484/:498-499, the
     * melterchem/smelterchem keys), the drying gem ladder (:513-514, the
     * GT6RecipesDrying iceTable rows resolve via the mat() null-drop gate) and the cutter
     * gemChipped/gemFlawed statements (:638-639, the cutter.json walk rows). The
     * {@code gem_ice} row (:485/:500) needs nothing: gem's condition is the GEMS flag, which
     * G_GEM_TRANSPARENT already carries.
     * Plus the two port-authority dye-axis rows (task dye-item-axis) — upstream has NO force
     * row for the dyes because the dye() factory routes through dust() whose
     * {@code put(G_DUST, MORTAR)} (MT.java:528 = upstream MT.java:164) stamps
     * {@code ITEMGENERATOR.DUSTS|PLANTS} (TD.java:587 G_DUST), satisfying the dust gate
     * {@code Or(DUSTS, DIRTY_DUSTS)} (OP.java:1199) and the fiber gate {@code PLANTS}
     * (OP.java:1417) — upstream PrefixItem.run generates the 32 items on that condition face,
     * and the port enumerate() already keeps them (GTMaterialItemsForceTest probes proved
     * 32/32 in registrationOrder BEFORE these rows landed). The rows stay as the
     * condition-refactor pin at the established seam: the axis survives even if the G_DUST
     * stamp chain or the OP gates are ever reworked. Upstream MT.java:3687
     * {@code DATA.Dye_Materials} is the referenced table (bound per-generation in MT.init).
     *
     * <p>The known cascade is upstream-faithful, not an explosion: {@code plateGem}'s condition is
     * {@code Or(gem, bouleGt) && PLATES}, so these forcings also yield the Crystalline
     * Silicon/Germanium/Redstone-Alloy/Nikoline-Alloy gem plates (GT6MaterialsRegister.csv
     * carries them upstream); GTMaterialItemsForceTest pins the exact item delta.
     */
    static void forceItemGeneration() {
        gregapi.data.OP.bouleGt.forceItemGeneration(gregapi.data.MT.Si, gregapi.data.MT.Ge, gregapi.data.MT.RedstoneAlloy, gregapi.data.MT.NikolineAlloy);
        gregapi.data.OP.bouleGt.forceItemGeneration(gregapi.data.MT.Sapphire, gregapi.data.MT.BlueSapphire, gregapi.data.MT.GreenSapphire,
                gregapi.data.MT.YellowSapphire, gregapi.data.MT.OrangeSapphire, gregapi.data.MT.PurpleSapphire, gregapi.data.MT.Ruby);
        gregapi.data.OP.plateTiny.forceItemGeneration(gregapi.data.MT.Paper);
        gregapi.data.OP.dust.forceItemGeneration(gregapi.data.MT.DATA.Dye_Materials); // task dye-item-axis — the 16 vanilla-index dyes
        gregapi.data.OP.plantGtFiber.forceItemGeneration(gregapi.data.MT.DATA.Dye_Materials);
        gregapi.data.OP.toolHeadPickaxeGem.forceItemGeneration(gregapi.data.MT.Empty); // OP.java:621, task toolhead-family-closeout
        gregapi.data.OP.gemChipped.forceItemGeneration(gregapi.data.MT.Ice); // OP.java:613, task gem-ice-force-rows (Ice lacks CRYSTAL, the force IS the landing)
        gregapi.data.OP.gemFlawed.forceItemGeneration(gregapi.data.MT.Ice); // OP.java:614, task gem-ice-force-rows
    }

    /** Segment 2 (RegisterEvent, LOW priority): items and the creative tabs, one listener for both (task card). */
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.ITEM) {
            registerItems(event);
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            registerCreativeTabs(event);
        }
    }

    /**
     * The single enumeration source for "what material prefix items exist": the registration bridge,
     * the datagen fallback and the creative tab layout all take this one walk (dual-criterion
     * convergence, task fullprefix-creativetab). Order: {@link OreDictPrefix#VALUES} x
     * MATERIAL_ARRAY ascending-mID, mirroring the upstream default material list; same-name
     * re-registrations merge onto the registration target (MaterialRegistry.java:182-185 = upstream
     * OreDictMaterial.java:199-202).
     */
    public static List<PrefixMaterial> registrationOrder() {
        return enumerate().kept;
    }

    /** One enumeration pass: kept pairs (registration order) plus the first-wins/deferral bookkeeping. */
    record Enumeration(List<PrefixMaterial> kept, int duplicatePairDrops, int duplicateIdDrops, int nonItemPathPrefixes) {}

    /**
     * Full walk over ALL {@link OreDictPrefix#VALUES} (468). Non-item-path prefixes are skipped for
     * the whole family (aggregate counter); the per-material criterion is
     * {@link OreDictPrefix#isGeneratingItem} (upstream PrefixItem.run, PrefixItem.java:104 =
     * forced || !blacklist && mCondition — NOT canGenerateItem, which drops the blacklist leg,
     * OreDictPrefix.java:285 vs :290).
     *
     * <p>Two-layer gate (mdh-4 closeout, KEEP ruling): layer 1 = the unified mod-driver
     * pre-filter on the mod axis, GT6ModDrivers (mdh series; visibilityGate — the mdh-1
     * mount point rides this walk); layer 2 = the OP.setCondition material-axis condition
     * chain inside isGeneratingItem's mCondition leg — the gregapi chain stays as the
     * second layer; the driver face does not replace it.
     */
    private static Enumeration enumerate() {
        Set<OreDictPrefix> tItemPath = new HashSet<>(itemPathPrefixes());
        Predicate<OreDictMaterial> tGate = GT6ModDrivers.visibilityGate(); // the unified driver face (mdh-1)
        List<PrefixMaterial> tRaw = new ArrayList<>();
        int tNonItemPath = 0, tDuplicatePairs = 0;
        Set<PrefixMaterial> tSeenPairs = new HashSet<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (!tItemPath.contains(tPrefix)) {tNonItemPath++; continue;} // block/MTE/parse-only path, deferred (class javadoc)
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                // Same-name re-registrations (old ID + new ID, createMaterial NOTICE path) live in two
                // array slots; the item belongs to the current registration target, merging
                // deprecated aliases onto one item (MaterialRegistry.java:182-185).
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
                if (tMaterial == null || tMaterial.mID < 0) continue;
                PrefixMaterial tPair = new PrefixMaterial(tPrefix, tMaterial);
                if (!tSeenPairs.add(tPair)) {tDuplicatePairs++; continue;} // alias slot: the target already owns the item
                if (!generatesItemPathItem(tPrefix, tMaterial)) continue; // PrefixItem.java:104 + the plank WOOD gate (task wood-planks-register; rebase-union with the mdh-1 mount below)
                if (!tGate.test(tMaterial)) continue; // GT6ModDrivers mount ① (mdh-1) — default all-PRESENT = pair-for-pair unchanged (ADR-MDH1)
                tRaw.add(tPair);
            }
        }
        FirstWins tFirstWins = firstWinsById(tRaw);
        return new Enumeration(tFirstWins.kept, tDuplicatePairs, tFirstWins.drops, tNonItemPath);
    }

    /** First-wins id dedup outcome: kept pairs in iteration order plus the drop count. */
    record FirstWins(List<PrefixMaterial> kept, int drops) {}

    /**
     * The per-material registration gate: {@link OreDictPrefix#isGeneratingItem} (upstream
     * PrefixItem.java:104 = forced || !blacklist && mCondition). The former plank WOOD gate
     * (task wood-planks-register) retired with the plank item face — task
     * planks-blockification moved the plank face to the {@link GT6WoodDict} BlockItem rows,
     * so every prefix here evaluates the plain upstream criterion.
     */
    static boolean generatesItemPathItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
        return aPrefix.isGeneratingItem(aMaterial);
    }

    /** Pure first-wins rule: the earliest pair in iteration order owns the composed id (class javadoc). */
    static FirstWins firstWinsById(List<PrefixMaterial> aPairs) {
        Set<String> tSeen = new HashSet<>();
        List<PrefixMaterial> rKept = new ArrayList<>(aPairs.size());
        int tDrops = 0;
        for (PrefixMaterial tPair : aPairs) {
            if (!tSeen.add(itemIdOf(tPair.prefix(), tPair.material()))) {tDrops++; continue;}
            rKept.add(tPair);
        }
        return new FirstWins(rKept, tDrops);
    }

    /** The item id path for a prefix x material pair — THE id composition rule (single source). */
    public static String itemIdOf(OreDictPrefix prefix, OreDictMaterial material) {
        return snakeCase(prefix.mNameInternal) + "_" + snakeCase(material.mNameInternal);
    }

    /**
     * The lens-catalyst probe (task circuit-chain-recipes): the port face of the upstream
     * {@code ST.amount(0, lens)} non-consumable leg — every LaserEngraver lens band row
     * (Loader_Recipes_Other.java:147-167) keeps its lens across the craft. Consumed by
     * {@code Recipe.sNotConsumable} (the replicator-USB/slicer-blade disjunct posture):
     * the predicate is the ITEM face — a lens is a shaping tool, never consumed stock —
     * so only the rows carrying a lens input feel it.
     *
     * <p>The production default is the {@code MaterialPrefixItem.prefix == OP.lens} identity;
     * the seam exists because a mod-Item is NOT constructible in the offline test JVM (the
     * forge intrusive-holder wall — the GT6SlicerBlades.sBladeTest shape verbatim).
     */
    public static java.util.function.Predicate<ItemStack> sLensTest =
            aStack -> aStack != null && !aStack.isEmpty()
                    && aStack.getItem() instanceof MaterialPrefixItem tItem
                    && tItem.prefix == OP.lens;

    /** The swappable binding (production default: the lens prefix identity). */
    public static boolean isLens(ItemStack aStack) {
        return sLensTest.test(aStack);
    }

    /**
     * Registration-side camelCase to snake_case (GTCEu FormattingUtil.toLowerCaseUnderscore semantics,
     * TagPrefix.java:1306-1308). Lives here — not on {@link gregtech6.item.MaterialPrefixItem} — so the
     * id composition is testable in a plain JVM (MaterialPrefixItem extends Item, which drags vanilla
     * registry statics into class init). The item class keeps an identical copy for its name keys;
     * GTMaterialItemsRegistrationTest pins the algorithm on both sides' outputs.
     */
    public static String snakeCase(String camelCase) {
        StringBuilder rBuilder = new StringBuilder(camelCase.length() + 4);
        for (int i = 0; i < camelCase.length(); i++) {
            char tChar = camelCase.charAt(i);
            if (Character.isUpperCase(tChar) && i > 0 && (Character.isLowerCase(camelCase.charAt(i - 1)) || Character.isDigit(camelCase.charAt(i - 1)))) rBuilder.append('_');
            rBuilder.append(Character.toLowerCase(tChar));
        }
        return rBuilder.toString();
    }

    /**
     * The upstream item-path prefixes: the 105 prefixes upstream constructs a PrefixItem for
     * (Loader_Items.java:57-171, verbatim membership in upstream file order; OP field references
     * keep it compile-checked) plus the four machine-casing block-family prefixes (task
     * casing-machine-register) and the plank prefix (task wood-planks-register) — the declared
     * item-path deviations. NOT a port-authority invention for the 105 — that face is upstream's
     * own item-path gate, see class javadoc.
     *
     * <p>The four {@code casingMachine*} additions are the declared item-path deviation (task
     * casing-machine-register): upstream registers them as PrefixBlock_ BLOCKS
     * (Loader_PrefixBlocks.java:48-51 — the port block family stays pooled, GTMaterialBlocks
     * javadoc), so their port items are flat sprite adaptations of the block art, not upstream
     * PrefixItems. {@code plank} is the same deviation class (task wood-planks-register): upstream
     * ships planks ONLY as blocks (Loader_Woods.java:62-65 BlockTreePlanks/2; the upstream item
     * loader carries zero plank rows — loaders/a/Loader_Items.java census), so the port plank items
     * are flat sprite adaptations of the block art (the PLANKS_WOOD block-iconset borrow,
     * assets/README.md).
     *
     * <p>The sixteen {@code wireGt01-16} multipliers are OFF this list (task
     * wiregt-prefix-item-retirement, the user-ruled upstream-fidelity regression 2026-10-06).
     * They rode here as the third declared deviation (task wire-gt-registration) and the lift is
     * reverted: upstream ships them ONLY on the MTE block path —
     * MultiTileEntityWireElectric.addElectricWires (MultiTileEntityWireElectric.java:72-87)
     * lifts every multiplier onto the wire MTE via one OreDictManager.setTarget_ row per
     * material (:89-93 the same face for cableGt01/02/04/08/12), and the upstream item loader
     * carries ZERO wireGt rows (loaders/a/Loader_Items.java census, the grep face). The lift
     * materialized 16 x 14 WIRES-material flat prefix items whose only art was the grayscale
     * block wire.png borrow, plus sixteen per-multiplier creative tabs and the rubber wire
     * rows — all retired with the list rows. The wireGtXX oredient face now rides the GTWires
     * block items alone (the upstream {@code setTarget_} isomorph over the 620-spectrum
     * family, GTWireSpecs.registryName = wire_&lt;mat&gt;_gtNN). The PREFIX DEFINITIONS stay
     * (OP.java:1364-1382 verbatim): the OreDictPrefix rows are the tag face the wire family
     * and the GT6ElectricTransformers wireGt01/wireGt04 Cu fold (GT6ElectricTransformers
     * .WIRE_TAG_PATH, the fine_wires/%s carrier) read, and the vanilla tag family stays
     * EMPTY for wireGt (the single-identity pin). {@code cableGt01-16} were NEVER on the
     * item path — upstream :89-93 puts them on the same MTE block path, and no port card
     * lifted them (the cable_*_gt01 registration gap is the wire-family pool's declared
     * block-domain face).
     *
     * <p>Per-material gates need zero port code for all 109 prefixes: the OP conditions are
     * already verbatim in the port (e.g. OP.java:1274-1277 — casingMachine = And(PARTS, SMITHABLE),
     * the other casing three chain casingMachine via setCondition = OreDictPrefix.isTrue →
     * canGenerateItem, OreDictPrefix.java:349) and {@link OreDictPrefix#isGeneratingItem} evaluates
     * them unchanged. The one former exception (plank — upstream OP.plank carries NO setCondition,
     * OP.java:394, default TRUE for every material, so the item adaptation needed the WOOD gate)
     * left with the plank retirement: task planks-blockification moved the plank face to the
     * {@link GT6WoodDict} BlockItem rows (the 17 cubes of BlockTreePlanks/2 + the vanilla
     * identities), and {@code gt6:plank_wood}/{@code gt6:plank_wood_treated}/... survive as the
     * generic cubes' ids (GT6TreeBlocks.GENERIC_PLANK_ROWS). No port-authority gate remains on
     * the item path.
     *
     * <p><b>The sawing absorb seam (ABSORBED)</b>: the sawing increment card's DECLARED IDENTITY
     * MAPPING re-pour and the plank-mapping-sweep 76-row carry both landed on the then-registered
     * gt6:plank_wood prefix item; task planks-blockification landed the generic plank BLOCK under
     * the same id, so those rows ride the converged face unchanged — and the GT6RecipesBath
     * {@code sPlankItemResolver} universe moved with the registration by design (its class doc
     * carries the attribution).
     *
     * <p>KJS/ CraftTweaker exposure of the new families is the kjs binding card's face (deferred —
     * the registration bridge stays the single source; no KJS surface ships here).
     */
    /** Package-private for the spec-pinning test (GTMaterialItemsRegistrationTest). */
    static List<OreDictPrefix> itemPathPrefixes() {
        return List.of(
            // Loader_Items.java:57-67 — dusts and crushed ores
            OP.dust, OP.dustSmall, OP.dustTiny, OP.dustDiv72, OP.dustImpure,
            OP.crushed, OP.crushedTiny, OP.crushedPurified, OP.crushedPurifiedTiny, OP.crushedCentrifuged, OP.crushedCentrifugedTiny,
            // :69-75 — gems and bouleGt
            OP.gemChipped, OP.gemFlawed, OP.gem, OP.gemFlawless, OP.gemExquisite, OP.gemLegendary, OP.bouleGt,
            // :77-85 — nugget, chunk, billet, ingots
            OP.nugget, OP.chunkGt, OP.billet, OP.ingot, OP.ingotHot, OP.ingotDouble, OP.ingotTriple, OP.ingotQuadruple, OP.ingotQuintuple,
            // :87-96 — plates
            OP.plateGemTiny, OP.plateGem, OP.plateTiny, OP.plate, OP.plateDouble, OP.plateTriple, OP.plateQuadruple, OP.plateQuintuple, OP.plateDense, OP.plateCurved,
            // :98-100 — scrap, rock, raw ore
            OP.scrapGt, OP.rockGt, OP.oreRaw,
            // :102-119 — gears, rods, springs, tool-adjacent parts; the casingMachine quartet
            // rides the casing group (upstream block path, Loader_PrefixBlocks.java:48-51 — the
            // declared item-path deviation, see method javadoc); plank rides the wood block
            // path (Loader_Woods.java:62-65 — the same deviation class, task
            // wood-planks-register, WOOD-gated in generatesItemPathItem)
            OP.gearGtSmall, OP.gearGt, OP.rotor, OP.stick, OP.stickLong, OP.springSmall, OP.spring,
            OP.lens, OP.round, OP.bolt, OP.screw, OP.ring, OP.chain, OP.foil, OP.casingSmall,
            OP.casingMachine, OP.casingMachineDouble, OP.casingMachineQuadruple, OP.casingMachineDense,
            OP.wireFine, OP.minecartWheels, OP.railGt,
            // NOTE the wireGt01-16 absence (task wiregt-prefix-item-retirement): upstream ships
            // them ONLY on the MTE block path (MultiTileEntityWireElectric.java:72-87, one
            // OreDictManager.setTarget_ row per multiplier; :89-93 the cableGt face), so upstream
            // Loader_Items has no wireGt rows — the oredient face rides the GTWires block items
            // (wire_<mat>_gtNN), the prefix definitions stay (OP.java:1364-1382). See the
            // method javadoc for the retired wire-gt-registration lift.
            // :121-127 — plant drops and chemtube
            OP.plantGtBerry, OP.plantGtBlossom, OP.plantGtFiber, OP.plantGtTwig, OP.plantGtWart, OP.chemtube,
            // :131-166 — tool heads (raw + finished)
            OP.toolHeadRawSword, OP.toolHeadSword, OP.toolHeadRawPickaxe, OP.toolHeadPickaxe, OP.toolHeadPickaxeGem,
            OP.toolHeadConstructionPickaxe, OP.toolHeadBuilderwand, OP.toolHeadRawShovel, OP.toolHeadShovel,
            OP.toolHeadRawSpade, OP.toolHeadSpade, OP.toolHeadRawAxe, OP.toolHeadAxe, OP.toolHeadRawAxeDouble,
            OP.toolHeadAxeDouble, OP.toolHeadRawHoe, OP.toolHeadHoe, OP.toolHeadHammer, OP.toolHeadFile,
            OP.toolHeadRawChisel, OP.toolHeadChisel, OP.toolHeadRawSaw, OP.toolHeadSaw, OP.toolHeadDrill,
            OP.toolHeadChainsaw, OP.toolHeadWrench, OP.toolHeadScrewdriver, OP.toolHeadRawUniversalSpade,
            OP.toolHeadUniversalSpade, OP.toolHeadRawSense, OP.toolHeadSense, OP.toolHeadRawPlow, OP.toolHeadPlow,
            OP.toolHeadBuzzSaw, OP.toolHeadRawArrow, OP.toolHeadArrow,
            // :167-171 — arrows and bullets (PrefixItemProjectile)
            OP.arrowGtWood, OP.arrowGtPlastic, OP.bulletGtSmall, OP.bulletGtMedium, OP.bulletGtLarge);
    }

    private static void registerItems(RegisterEvent event) {
        Enumeration tSet = enumerate();
        long tTotal = 0;
        for (PrefixMaterial tPair : tSet.kept) {
            ResourceLocation tLoc = gtId(itemIdOf(tPair.prefix(), tPair.material()));
            if (!REGISTERED_IDS.add(tLoc)) { // defensive dedup, ADR-P2-2 fix 1
                GT6Mod.LOGGER.warn("GT6 skipped duplicate item id {}", tLoc);
                continue;
            }
            //? if forge {
            RegistryObject<Item> tHandle = RegistryObject.create(tLoc, Registries.ITEM, "gt6"); // RegistryObject.java:62
            //?} else {
            /*DeferredHolder<Item, Item> tHandle = DeferredHolder.create(Registries.ITEM, tLoc); // DeferredHolder.create(ResourceKey, id), javap neoforge-21.1.249
             *///?}
            event.register(Registries.ITEM, tLoc, () -> new MaterialPrefixItem(new Item.Properties(), tPair.prefix(), tPair.material())); // RegisterEvent.java:54-63
            INDEX.put(tPair, tHandle);
            tTotal++;
        }
        GT6Mod.LOGGER.info("GT6 registered {} material prefix items in total (first-wins id drops: {}, duplicate pair drops: {}, non-item-path prefixes skipped: {})",
            tTotal, tSet.duplicateIdDrops, tSet.duplicatePairDrops, tSet.nonItemPathPrefixes);
    }

    /**
     * The creative-visible prefixes: on the item path, not HIDDEN (PrefixItem.java:89,
     * SHOW_HIDDEN_PREFIXES=false default) and holding at least one registrable item; VALUES order.
     * Derived from {@link #registrationOrder()} so datagen (headless JVM) computes the same set.
     */
    public static List<OreDictPrefix> tabPrefixes() {
        List<OreDictPrefix> rTabs = new ArrayList<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (tPrefix.contains(TD.Creative.HIDDEN)) continue; // PrefixItem.java:89
            for (PrefixMaterial tPair : registrationOrder()) {
                if (tPair.prefix() == tPrefix) {rTabs.add(tPrefix); break;} // >= 1 item, PrefixItem.java:121 empties skipped
            }
        }
        return rTabs;
    }

    /** One tab per creative-visible prefix (upstream CreativeTab per-prefix semantics, class javadoc). */
    private static void registerCreativeTabs(RegisterEvent event) {
        Map<OreDictPrefix, List<PrefixMaterial>> tGroups = new LinkedHashMap<>();
        for (PrefixMaterial tPair : registrationOrder()) tGroups.computeIfAbsent(tPair.prefix(), tKey -> new ArrayList<>()).add(tPair);
        int tColumn = 0;
        int tTabCount = 0;
        for (OreDictPrefix tPrefix : tabPrefixes()) {
            List<PrefixMaterial> tTabItems = tGroups.get(tPrefix);
            //? if forge {
            RegistryObject<Item> tIcon = INDEX.get(tTabItems.get(0)); // upstream icon = the prefix item itself with wildcard metadata (CreativeTab.java:28-35)
            //?} else {
            /*DeferredHolder<Item, Item> tIcon = INDEX.get(tTabItems.get(0)); // upstream icon = the prefix item itself with wildcard metadata (CreativeTab.java:28-35)
             *///?}
            String tSnake = snakeCase(tPrefix.mNameInternal);
            final int tColumnF = tColumn;
            event.register(Registries.CREATIVE_MODE_TAB, gtId(tSnake), () ->
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, tColumnF) // vanilla CreativeModeTab.java:46-48; Forge 1.20.1 paginates modded tabs 10/page
                    .title(Component.translatable("itemGroup.gt6." + tSnake)) // upstream LH.add("itemGroup." + mNameInternal, mNameCategory), CreativeTab.java:32
                    .icon(() -> new ItemStack(tIcon.get()))
                    .displayItems((tParameters, tOutput) -> {
                        for (PrefixMaterial tPair : tTabItems) {
                            if (tPair.material().mHidden) continue; // PrefixItem.java:114, SHOW_HIDDEN_MATERIALS=false default
                            tOutput.accept(new ItemStack(INDEX.get(tPair).get())); // CreativeModeTab.Output.accept, CreativeModeTab.java:262-264
                        }
                    })
                    .build());
            tColumn++;
            tTabCount++;
        }
        GT6Mod.LOGGER.info("GT6 registered {} per-prefix creative tabs", tTabCount);
    }

    /** gt6 namespaced id. The two-arg constructor is the vanilla 1.20.1 form (ResourceLocation.java:37); Forge userdev backports a removal deprecation onto it. */
    private static ResourceLocation gtId(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6", path);
    }

    /** Query API for later cards: the handle of a prefix x material item, or null if not registered. */
    //? if forge {
    /**
     * The pair-lookup seam (the GT6RecipesImplosion.sMaterialItemResolver precedent, mdh-clearout-batch2):
     * production answers the frozen registration INDEX; the datagen walk-leg test swaps in the
     * registration-truth stub (registrationOrder ∩ driver pin) because a test JVM cannot re-run
     * registration. Tests capture the original reference and restore it in their afterEach, always.
     */
    public static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, RegistryObject<Item>> sLookup =
            (aPrefix, aMaterial) -> INDEX.get(new PrefixMaterial(aPrefix, aMaterial));

    public static RegistryObject<Item> get(OreDictPrefix prefix, OreDictMaterial material) {
        return sLookup.apply(prefix, material);
    }

    /** All registered handles (unmodifiable, registration order). */
    public static Map<PrefixMaterial, RegistryObject<Item>> items() {
        return Collections.unmodifiableMap(INDEX);
    }
    //?} else {
    /*public static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, DeferredHolder<Item, Item>> sLookup =
            (aPrefix, aMaterial) -> INDEX.get(new PrefixMaterial(aPrefix, aMaterial));

    public static DeferredHolder<Item, Item> get(OreDictPrefix prefix, OreDictMaterial material) {
        return sLookup.apply(prefix, material);
    }

    // All registered handles (unmodifiable, registration order).
    public static Map<PrefixMaterial, DeferredHolder<Item, Item>> items() {
        return Collections.unmodifiableMap(INDEX);
    }
     *///?}

    /** All registered items as an array, for ItemColors.register(ItemColor, ItemLike...) (client seam). */
    public static Item[] itemArray() {
        //? if forge {
        return INDEX.values().stream().map(RegistryObject::get).toArray(Item[]::new);
        //?} else {
        /*return INDEX.values().stream().map(DeferredHolder::get).toArray(Item[]::new);
         *///?}
    }

    /**
     * The leg-neutral (prefix, material) -> ItemStack face for RUNTIME consumers (the
     * pickaxe-gem head-drop ring, task toolhead-family-closeout — the port {@code mat()}
     * cut, OreDictPrefix.java:55, routes through the registration bridge instead).
     * EMPTY when the pair is unregistered or the handle is unresolved (offline/datagen
     * JVMs never bind INDEX; the caller's vanilla-vanish fallback answers there).
     */
    public static ItemStack stackOf(OreDictPrefix prefix, OreDictMaterial material) {
        //? if forge {
        RegistryObject<Item> tHandle = sLookup.apply(prefix, material);
        return tHandle == null || !tHandle.isPresent() ? ItemStack.EMPTY : new ItemStack(tHandle.get());
        //?} else {
        /*DeferredHolder<Item, Item> tHandle = sLookup.apply(prefix, material);
        return tHandle == null || !tHandle.isBound() ? ItemStack.EMPTY : new ItemStack(tHandle.get());
         *///?}
    }
}
