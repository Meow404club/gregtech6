package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6OreBlocks;

/**
 * The viewer-neutral layout seam of the ore-generation distribution page (task
 * debt-ore-gen-display, the B-lite ruling of decisions.2026-09-26-debt-ore-gen-info) —
 * EVERY text line, geometry constant and item-stack resolution the JEI and EMI wrappers
 * render from, so the two viewers show one page ({@code gregtech6.jei.GT6RecipeMapViewerMeta}
 * is the batch1 precedent of a shared seam consumed across the viewer packages). Zero
 * JEI/EMI imports: a JEI-only install loads it through the JEI wrapper, an EMI-only
 * install through the EMI wrapper, neither may class-link a missing viewer.
 *
 * <p>The page unit is the {@link OreDistributionInfo.Entry} — the material aggregate of the
 * data card (debt-ore-gen-data, merged 69c45ad66). Layout (task oregen-info-relayout — the
 * GTCEu OreVeinRecipeWidget.drawUI skeleton: labeled attribute rows + a gap between logical
 * blocks, adapted to per-face lines because one material here covers MANY vein
 * definitions):
 * <pre>
 *   Cassiterite                        <- name line (gt6.material.&lt;snake&gt;, both locales live)
 *   [NORMAL-form ore block slot]  Overworld, Nether, End   <- representative + the dims row
 *   Small Ores                                             <- section header (gap before)
 *     Overworld Y 60-120 · 16/chunk                        <- indented line per small-ore face
 *   Large Veins                                            <- section header (gap before)
 *     Overworld Y 40-90 · Weight 170 · Size 24             <- indented line per vein face
 *   Bedrock Ores                                           <- section header (gap before)
 *     cassiterite · 1/2000 per chunk                       <- indented line per bedrock row
 * </pre>
 *
 * <p>Every visible word is a lang key ({@code gt6.jei.info.ore_gen_info.*} domain, en+zh
 * faces — the zh 译名 follow the dump faces where one exists 主世界/下界/末地/基岩矿石/
 * 小矿石/区块, else the GTCEu zh faces of the same UI concepts 权重/生成, never machine
 * translation). The dims are proper nouns in en, translated in zh. The row-suffix of a
 * bedrock line (gold.a) is DATA (the row id), not display copy.
 *
 * <p><b>DISPLAY SEMANTICS (the data layer's 口径, see OreDistributionInfo's javadoc):
 * the numbers shown are the UPSTREAM table values</b> — a small-ore amount is the upstream
 * per-chunk attempt semantics (this port places at ORE_SIZE=4, so live density runs ~1.5x),
 * a bedrock probability is the independent 1/P per-chunk roll. The page never multiplies
 * or rounds them.
 *
 * <p>Stack resolution is registry-dependent and therefore OFFLINE-SAFE by construction:
 * the walks ({@link #variantPaths}) are pure key arithmetic (GT6OreBlocks/GT6BedrockOreBlocks
 * registrationOrder logic, no registry access), while the ItemStack faces
 * ({@link #variantStacks}, {@link #representative}, {@link #catalystStack}) resolve the
 * registered handles and degrade to EMPTY when the game registries have not fired — the
 * offline tests pin the paths and the walks, the live registry fills the stacks.
 */
public final class GT6OreGenInfoLayout {

    // ------------------------------------------------------------ geometry (both wrappers render from these)
    /** Wide enough for the longest pinned face line at the vanilla font's ~6px/char — the wrappers share it. */
    public static final int WIDTH = 250;
    /** The name line, above everything. */
    public static final int NAME_Y = 3;
    /** The representative ore-block slot (the material's NORMAL stone-family block). */
    public static final int SLOT_X = 4;
    public static final int SLOT_Y = 15;
    /** The dims row rides beside the slot (GTCEu's dimension-marker row, text form). */
    public static final int DIMS_X = 26;
    public static final int DIMS_Y = 21;
    /** The sectioned text band (base = slot bottom 33 + 5 gap); section headers sit at {@link #TEXT_X}. */
    public static final int TEXT_X = 4;
    public static final int FACE_BASE_Y = 38;
    public static final int LINE_HEIGHT = 10;
    /** The extra air before a section header — the GTCEu drawUI marginBottom-3 block separation (OreVeinRecipeWidget.java:105-114). */
    public static final int SECTION_GAP = 4;
    /** Member lines indent under their section header (the GTCEu column flow, text form). */
    public static final int INDENT = 10;
    /** face band + 4px bottom padding. */
    private static final int BOTTOM_PAD = 4;

    /** The page title lang key — the {@code gt6.jei.info.*} viewer-neutral domain (GT6RecipeViewerText precedent), key mirrors the category uid one-to-one; the values live on both lang faces (task debt-oregen-title-i18n). */
    public static final String TITLE_KEY = "gt6.jei.info.ore_gen_info";
    /** The section-header keys (en "Small Ores"/"Large Veins"/"Bedrock Ores"; zh 小矿石 — the dump face tmp/gregtech.lang:116979, 大型矿脉 — the GTCEu 矿脉 word, 基岩矿石 — the dump face tmp/gregtech.lang:17690). */
    public static final String SECTION_SMALL_KEY = TITLE_KEY + ".section.small_ores";
    public static final String SECTION_VEIN_KEY = TITLE_KEY + ".section.large_veins";
    public static final String SECTION_BEDROCK_KEY = TITLE_KEY + ".section.bedrock_ores";
    /** The per-face line format keys — "%s Y %s-%s ..." with the dim component + numbers as args. */
    public static final String LINE_SMALL_KEY = TITLE_KEY + ".line.small_ore";
    public static final String LINE_VEIN_KEY = TITLE_KEY + ".line.large_vein";
    public static final String LINE_BEDROCK_KEY = TITLE_KEY + ".line.bedrock_ore";
    /** The dim names (en proper nouns Overworld/Nether/End; zh the dump faces 主世界 tmp/gregtech.lang:17613 / 下界 :5100 (S:gt.material.Nether=下界) / 末地 :4651 (S:gt.material.Endstone=末地)). */
    public static final String DIM_OVERWORLD_KEY = TITLE_KEY + ".dim.overworld";
    public static final String DIM_NETHER_KEY = TITLE_KEY + ".dim.nether";
    public static final String DIM_END_KEY = TITLE_KEY + ".dim.end";
    public static final String DIM_ATUM_KEY = TITLE_KEY + ".dim.atum";

    /** The dim name as a translatable component — proper nouns in en, dump faces in zh; Atum
     * keeps the atum card's unlocalized-proper-noun face (the zh key value is "Atum", the same
     * rendered text this page answered before the translatable seam, task atum-dim-adaptation). */
    public static Component dim(GTOreWorldgen.Dim aDim) {
        return Component.translatable(switch (aDim) {
            case OVERWORLD -> DIM_OVERWORLD_KEY;
            case NETHER -> DIM_NETHER_KEY;
            case END -> DIM_END_KEY;
            case ATUM -> DIM_ATUM_KEY;
        });
    }

    // ------------------------------------------------------------ text rows

    /** The page title component — translatable so both locales render (the zh face rides the reference table's hand layer). */
    public static Component title() {
        return Component.translatable(TITLE_KEY);
    }

    /** The material name line — the {@code gt6.material.<snake>} small unit, composed in BOTH locales since i18n-material-fill-fix. */
    public static Component name(OreDistributionInfo.Entry aEntry) {
        return Component.translatable("gt6.material." + MaterialPrefixItem.snakeCase(aEntry.material().mNameInternal));
    }

    /**
     * The dims row: the union of the three columns' dims, {@link GTOreWorldgen.Dim}
     * declaration order. Bedrock rows carry no dim in the data model but are
     * overworld-only by construction (the data layer keeps only the overworld rows), so
     * a bedrock face contributes Overworld.
     */
    public static Component dimsRow(OreDistributionInfo.Entry aEntry) {
        boolean tHasBedrock = !aEntry.bedrockOres().isEmpty();
        MutableComponent rRow = Component.empty();
        boolean tFirst = true;
        for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
            if (hasDim(aEntry, tDim) || (tHasBedrock && tDim == GTOreWorldgen.Dim.OVERWORLD)) {
                if (!tFirst) rRow.append(", ");
                rRow.append(dim(tDim));
                tFirst = false;
            }
        }
        return rRow;
    }

    private static boolean hasDim(OreDistributionInfo.Entry aEntry, GTOreWorldgen.Dim aDim) {
        for (OreDistributionInfo.SmallOre tSmall : aEntry.smallOres()) if (tSmall.dim() == aDim) return true;
        for (OreDistributionInfo.Vein tVein : aEntry.veins()) if (tVein.dim() == aDim) return true;
        return false;
    }

    /**
     * One render row: a translatable format key + its args + the absolute x/y both
     * wrappers draw at — the wrappers carry ZERO layout logic, they walk this list.
     */
    public record Row(String key, List<Object> args, int x, int y) {
        /** The translatable component the wrappers render. */
        public Component component() {
            return Component.translatable(key, args.toArray());
        }
    }

    /**
     * The sectioned page rows, column order = the data layer's (small, vein, bedrock):
     * a header per non-empty section (GTCEu drawUI's childIf form, OreVeinRecipeWidget.java:107),
     * member lines indented under it, {@link #SECTION_GAP} air before each following
     * header. Args carry the UPSTREAM table numbers verbatim (the display-semantics rule).
     */
    public static List<Row> rows(OreDistributionInfo.Entry aEntry) {
        List<Row> rRows = new ArrayList<>();
        int tY = FACE_BASE_Y;
        if (!aEntry.smallOres().isEmpty()) {
            rRows.add(new Row(SECTION_SMALL_KEY, List.of(), TEXT_X, tY));
            tY += LINE_HEIGHT;
            for (OreDistributionInfo.SmallOre tSmall : aEntry.smallOres()) {
                rRows.add(new Row(LINE_SMALL_KEY,
                        List.of(dim(tSmall.dim()), tSmall.minY(), tSmall.maxY(), tSmall.amount()),
                        TEXT_X + INDENT, tY));
                tY += LINE_HEIGHT;
            }
        }
        if (!aEntry.veins().isEmpty()) {
            tY += SECTION_GAP;
            rRows.add(new Row(SECTION_VEIN_KEY, List.of(), TEXT_X, tY));
            tY += LINE_HEIGHT;
            for (OreDistributionInfo.Vein tVein : aEntry.veins()) {
                rRows.add(new Row(LINE_VEIN_KEY,
                        List.of(dim(tVein.dim()), tVein.minY(), tVein.maxY(), tVein.weight(), tVein.size()),
                        TEXT_X + INDENT, tY));
                tY += LINE_HEIGHT;
            }
        }
        if (!aEntry.bedrockOres().isEmpty()) {
            tY += SECTION_GAP;
            rRows.add(new Row(SECTION_BEDROCK_KEY, List.of(), TEXT_X, tY));
            tY += LINE_HEIGHT;
            for (OreDistributionInfo.BedrockOre tBedrock : aEntry.bedrockOres()) {
                rRows.add(new Row(LINE_BEDROCK_KEY, List.of(shortName(tBedrock.name()), tBedrock.probability()),
                        TEXT_X + INDENT, tY));
                tY += LINE_HEIGHT;
            }
        }
        return rRows;
    }

    /** {@code ore.bedrock.gold.a} -> {@code gold.a} — the row-suffix disambiguator (the gold.a/gold.b pair). */
    private static String shortName(String aRowName) {
        return aRowName.startsWith("ore.bedrock.") ? aRowName.substring("ore.bedrock.".length()) : aRowName;
    }

    /** The per-entry total height the wrappers report — sectioned band + bottom padding. */
    public static int height(OreDistributionInfo.Entry aEntry) {
        List<Row> tRows = rows(aEntry);
        int tLast = tRows.isEmpty() ? FACE_BASE_Y : tRows.get(tRows.size() - 1).y();
        return tLast + LINE_HEIGHT + BOTTOM_PAD;
    }

    /** The category height: the WORST entry (JEI fixes one height per category; EMI matches for shared geometry). */
    public static int categoryHeight() {
        int tMax = 0;
        for (OreDistributionInfo.Entry tEntry : OreDistributionInfo.entries()) {
            tMax = Math.max(tMax, height(tEntry));
        }
        return tMax;
    }

    // ------------------------------------------------------------ the mounting walk (U-reachability)

    /**
     * The registry-id paths of EVERY ore block item the material owns — the "ore BlockItem
     * 全变体" invisible-mounting walk: the {@link GT6OreBlocks} universe (family-major,
     * kind-major — the material slice of the registration walk) then the {@link
     * GT6BedrockOreBlocks} universe (large, small), each gated on the material's AXIS
     * MEMBERSHIP (a material outside an axis owns no blocks there — the bedrock-only 15
     * are not in the small-ore 53-axis; the walks must emit exactly the registered ids).
     * Pure key arithmetic, offline-safe.
     */
    public static List<String> variantPaths(OreDistributionInfo.Entry aEntry) {
        List<String> rPaths = new ArrayList<>();
        if (inAxis(oreBlockAxis, aEntry.material())) {
            for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
                for (GT6OreBlocks.FormKind tKind : tFamily.kinds()) {
                    rPaths.add(GT6OreBlocks.path(new GT6OreBlocks.OreKey(tFamily, tKind, aEntry.material())));
                }
            }
        }
        if (inAxis(bedrockBlockAxis, aEntry.material())) {
            for (boolean tSmall : new boolean[] {GT6BedrockOreBlocks.LARGE, GT6BedrockOreBlocks.SMALL}) {
                rPaths.add(GT6BedrockOreBlocks.path(tSmall, aEntry.material()));
            }
        }
        return rPaths;
    }

    /**
     * The mounting stacks: the same walk resolving REGISTERED items (in-game: all of the
     * paths; offline: none). The JEI wrapper feeds these to
     * {@code addInvisibleIngredients(OUTPUT)}, the EMI wrapper returns them from
     * {@code getOutputs()} — the same GTCEu addInvisibleIngredients trick
     * (GTOreVeinInfoCategory.java:51-53), so U on any ore block item finds the page.
     */
    public static List<ItemStack> variantStacks(OreDistributionInfo.Entry aEntry) {
        List<ItemStack> rStacks = new ArrayList<>();
        if (inAxis(oreBlockAxis, aEntry.material())) {
            for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
                for (GT6OreBlocks.FormKind tKind : tFamily.kinds()) {
                    addIfResolved(rStacks, resolve(GT6OreBlocks.items().get(new GT6OreBlocks.OreKey(tFamily, tKind, aEntry.material()))));
                }
            }
        }
        if (inAxis(bedrockBlockAxis, aEntry.material())) {
            for (boolean tSmall : new boolean[] {GT6BedrockOreBlocks.LARGE, GT6BedrockOreBlocks.SMALL}) {
                addIfResolved(rStacks, resolve(GT6BedrockOreBlocks.items().get(new GT6BedrockOreBlocks.BedrockKey(tSmall, aEntry.material()))));
            }
        }
        return rStacks;
    }

    /** The two block universes' axes, walked once at init (offline-safe — the registration walks are pure). */
    private static final List<OreDictMaterial> oreBlockAxis = GT6OreBlocks.materialAxis();
    private static final List<OreDictMaterial> bedrockBlockAxis = GT6BedrockOreBlocks.materialAxis();

    /** Identity membership (the MaterialRegistry canonical-identity semantics the data layer keys on). */
    private static boolean inAxis(List<OreDictMaterial> aAxis, OreDictMaterial aMaterial) {
        for (OreDictMaterial tAxisMaterial : aAxis) {
            if (tAxisMaterial == aMaterial) return true;
        }
        return false;
    }

    /**
     * The slot stack: the NORMAL-form stone-family ore block ({@link GT6OreBlocks#TAB_FAMILY}
     * — the one family the creative tab shows, the port's canonical ore face), falling back
     * to the large bedrock form for materials outside the small-ore block axis (the
     * bedrock-only 15). The stack-resolving twin of {@link #representativePath} (the
     * registry-dependent form — offline both branches degrade to EMPTY).
     */
    public static ItemStack representative(OreDistributionInfo.Entry aEntry) {
        ItemStack tStack = resolve(GT6OreBlocks.items().get(new GT6OreBlocks.OreKey(
                GT6OreBlocks.TAB_FAMILY, GT6OreBlocks.FormKind.NORMAL, aEntry.material())));
        if (!tStack.isEmpty()) return tStack;
        return resolve(GT6BedrockOreBlocks.items().get(new GT6BedrockOreBlocks.BedrockKey(
                GT6BedrockOreBlocks.LARGE, aEntry.material())));
    }

    /**
     * The offline-decidable representative contract behind {@link #representative}: the
     * stone NORMAL path when the material is in the {@link GT6OreBlocks} block axis, else
     * the large bedrock path — the tests pin this face (the stack form needs live registries).
     */
    public static String representativePath(OreDistributionInfo.Entry aEntry) {
        if (inAxis(oreBlockAxis, aEntry.material())) {
            return GT6OreBlocks.path(new GT6OreBlocks.OreKey(
                    GT6OreBlocks.TAB_FAMILY, GT6OreBlocks.FormKind.NORMAL, aEntry.material()));
        }
        return GT6BedrockOreBlocks.path(GT6BedrockOreBlocks.LARGE, aEntry.material());
    }

    /**
     * The catalyst: the surface-rock's collected pebble ({@code rockGt} Stone — the item
     * {@code surface_rock_stone} drops, GT6LootTables.java:2561), the port's stand-in for
     * GTCEu's prospector catalysts (GTOreVeinInfoCategory.java:55-59) and the upstream
     * find-the-ore semantic. The rock BLOCK itself has no BlockItem (GT6SurfaceBlocks class
     * doc), so the dropped pebble IS the "指示石" item face.
     */
    public static ItemStack catalystStack() {
        return resolve(GTMaterialItems.get(OP.rockGt, MT.Stone));
    }

    private static void addIfResolved(List<ItemStack> aStacks, ItemStack aStack) {
        if (!aStack.isEmpty()) aStacks.add(aStack);
    }

    /** The one registry-dependent face: present-and-bound handle -> stack, anything else -> EMPTY. */
    //? if forge {
    private static ItemStack resolve(net.minecraftforge.registries.RegistryObject<Item> aHandle) {
        return aHandle != null && aHandle.isPresent() ? new ItemStack(aHandle.get()) : ItemStack.EMPTY;
    }
    //?} else {
    /*private static ItemStack resolve(net.neoforged.neoforge.registries.DeferredHolder<Item, Item> aHandle) {
        return aHandle != null && aHandle.isBound() ? new ItemStack(aHandle.get()) : ItemStack.EMPTY;
    }
     *///?}

    private GT6OreGenInfoLayout() {
    }
}
