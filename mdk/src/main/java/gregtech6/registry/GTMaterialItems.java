package gregtech6.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.item.MaterialPrefixItem;

/**
 * Registration bridge (ADR-P2-2 / ADR-P2-3, full-prefix expansion per task p3-fullprefix-creativetab):
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
 * identical-name aliases exist purely for oredict parsing). This card owns the item path, so the
 * universe is that 105-prefix list — referenced via OP fields for compile-checked existence — while
 * the walk below iterates ALL of {@link OreDictPrefix#VALUES} and explicitly skips the non-item paths
 * (aggregate counter in the registration log) so the deferral stays visible and auditable.
 * Block/MTE families are later cards; PREFIX_UNUSED aliases never generate.
 *
 * <p><b>id collision policy = first-wins</b>, identical rule to the phase-2 lang collision
 * (compressed/Compressed both snake to "compressed", first registration wins, task p2-datagen-pipeline).
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
    private static final Map<PrefixMaterial, RegistryObject<Item>> INDEX = new LinkedHashMap<>();
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
        MaterialRegistry.INSTANCE.close(); // ADR-P2-2 open->closed; GTCEu CommonProxy.java:185-229 unfreeze->init->freeze isomorph
        GT6Mod.LOGGER.info("GT6 material system initialised: {} materials, {} prefixes", MaterialRegistry.INSTANCE.MATERIAL_MAP.size(), OreDictPrefix.VALUES.size());
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
     * convergence, task p3-fullprefix-creativetab). Order: {@link OreDictPrefix#VALUES} x
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
     */
    private static Enumeration enumerate() {
        Set<OreDictPrefix> tItemPath = new HashSet<>(itemPathPrefixes());
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
                if (!tPrefix.isGeneratingItem(tMaterial)) continue; // PrefixItem.java:104
                tRaw.add(tPair);
            }
        }
        FirstWins tFirstWins = firstWinsById(tRaw);
        return new Enumeration(tFirstWins.kept, tDuplicatePairs, tFirstWins.drops, tNonItemPath);
    }

    /** First-wins id dedup outcome: kept pairs in iteration order plus the drop count. */
    record FirstWins(List<PrefixMaterial> kept, int drops) {}

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
     * The upstream item-path prefixes: exactly the prefixes upstream constructs a PrefixItem for
     * (Loader_Items.java:57-171, verbatim membership in upstream file order; OP field references
     * keep it compile-checked). NOT a port-authority invention — it is upstream's own item-path
     * gate, see class javadoc.
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
            // :102-119 — gears, rods, springs, tool-adjacent parts
            OP.gearGtSmall, OP.gearGt, OP.rotor, OP.stick, OP.stickLong, OP.springSmall, OP.spring,
            OP.lens, OP.round, OP.bolt, OP.screw, OP.ring, OP.chain, OP.foil, OP.casingSmall, OP.wireFine, OP.minecartWheels, OP.railGt,
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
            RegistryObject<Item> tHandle = RegistryObject.create(tLoc, Registries.ITEM, "gt6"); // RegistryObject.java:62
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
            RegistryObject<Item> tIcon = INDEX.get(tTabItems.get(0)); // upstream icon = the prefix item itself with wildcard metadata (CreativeTab.java:28-35)
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
        return new ResourceLocation("gt6", path);
    }

    /** Query API for later cards: the handle of a prefix x material item, or null if not registered. */
    public static RegistryObject<Item> get(OreDictPrefix prefix, OreDictMaterial material) {
        return INDEX.get(new PrefixMaterial(prefix, material));
    }

    /** All registered handles (unmodifiable, registration order). */
    public static Map<PrefixMaterial, RegistryObject<Item>> items() {
        return Collections.unmodifiableMap(INDEX);
    }

    /** All registered items as an array, for ItemColors.register(ItemColor, ItemLike...) (client seam). */
    public static Item[] itemArray() {
        return INDEX.values().stream().map(RegistryObject::get).toArray(Item[]::new);
    }
}
