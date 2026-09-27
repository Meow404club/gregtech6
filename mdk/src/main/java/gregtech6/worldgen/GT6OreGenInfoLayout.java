package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
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
 * data card (debt-ore-gen-data, merged 69c45ad66). Layout (GTCEu OreVeinRecipeWidget's
 * rows, flattened to per-face text lines because one material here covers MANY vein
 * definitions):
 * <pre>
 *   Cassiterite                        <- name line (gt6.material.&lt;snake&gt;, both locales live)
 *   [NORMAL-form ore block slot]  Overworld, Nether, End   <- representative + the dims row
 *   Small Overworld Y 60-120 16/chunk                     <- one line per small-ore face
 *   Vein Overworld Y 40-90 w170 s24                       <- one line per vein face (own kind)
 *   Bedrock cassiterite 1/2000/chunk                      <- one line per bedrock row
 * </pre>
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
    public static final int WIDTH = 200;
    /** The name line, above everything. */
    public static final int NAME_Y = 3;
    /** The representative ore-block slot (the material's NORMAL stone-family block). */
    public static final int SLOT_X = 4;
    public static final int SLOT_Y = 15;
    /** The dims row rides beside the slot (GTCEu's dimension-marker row, text form). */
    public static final int DIMS_X = 26;
    public static final int DIMS_Y = 21;
    /** The per-face text band: one line per face, top to bottom (base = slot bottom 33 + 5 gap). */
    public static final int TEXT_X = 4;
    public static final int FACE_BASE_Y = 38;
    public static final int LINE_HEIGHT = 10;
    /** face band + 4px bottom padding. */
    private static final int FACE_BAND_OFFSET = FACE_BASE_Y + 4;

    /** The page title — ponytail: literal title (batch1's literal-title precedent); a lang key needs the zh tsv hand-row pipeline, one line when that card runs. */
    public static final String TITLE_TEXT = "Ore Generation Distribution";

    /** The vanilla dim names in {@link GTOreWorldgen.Dim} declaration order — proper nouns, unlocalized (GTCEu used icons; text is the B-lite face). */
    public static String dimName(GTOreWorldgen.Dim aDim) {
        return switch (aDim) {
            case OVERWORLD -> "Overworld";
            case NETHER -> "Nether";
            case END -> "End";
        };
    }

    // ------------------------------------------------------------ text rows

    /** The page title component. */
    public static Component title() {
        return Component.literal(TITLE_TEXT);
    }

    /** The material name line — the {@code gt6.material.<snake>} small unit, composed in BOTH locales since p23-i18n-material-fill-fix. */
    public static Component name(OreDistributionInfo.Entry aEntry) {
        return Component.translatable("gt6.material." + MaterialPrefixItem.snakeCase(aEntry.material().mNameInternal));
    }

    /**
     * The dims row: the union of the three columns' dims, {@link GTOreWorldgen.Dim}
     * declaration order. Bedrock rows carry no dim in the data model but are
     * overworld-only by construction (the data layer keeps only the overworld rows), so
     * a bedrock face contributes Overworld.
     */
    public static String dimsLine(OreDistributionInfo.Entry aEntry) {
        boolean tHasBedrock = !aEntry.bedrockOres().isEmpty();
        List<String> tDims = new ArrayList<>(3);
        for (GTOreWorldgen.Dim tDim : GTOreWorldgen.Dim.values()) {
            if (hasDim(aEntry, tDim) || (tHasBedrock && tDim == GTOreWorldgen.Dim.OVERWORLD)) {
                tDims.add(dimName(tDim));
            }
        }
        return String.join(", ", tDims);
    }

    private static boolean hasDim(OreDistributionInfo.Entry aEntry, GTOreWorldgen.Dim aDim) {
        for (OreDistributionInfo.SmallOre tSmall : aEntry.smallOres()) if (tSmall.dim() == aDim) return true;
        for (OreDistributionInfo.Vein tVein : aEntry.veins()) if (tVein.dim() == aDim) return true;
        return false;
    }

    /**
     * One line per face, column order = the data layer's (small, vein, bedrock), row order
     * preserved. w/s = the GTCEu vein page's weight/size terms; every format stays within
     * ~33 chars so the longest line clears {@link #WIDTH} at the vanilla font.
     */
    public static List<String> faceLines(OreDistributionInfo.Entry aEntry) {
        List<String> rLines = new ArrayList<>();
        for (OreDistributionInfo.SmallOre tSmall : aEntry.smallOres()) {
            rLines.add("Small " + dimName(tSmall.dim()) + " Y " + tSmall.minY() + "-" + tSmall.maxY()
                    + " " + tSmall.amount() + "/chunk");
        }
        for (OreDistributionInfo.Vein tVein : aEntry.veins()) {
            rLines.add("Vein " + dimName(tVein.dim()) + " Y " + tVein.minY() + "-" + tVein.maxY()
                    + " w" + tVein.weight() + " s" + tVein.size());
        }
        for (OreDistributionInfo.BedrockOre tBedrock : aEntry.bedrockOres()) {
            rLines.add("Bedrock " + shortName(tBedrock.name()) + " 1/" + tBedrock.probability() + "/chunk");
        }
        return rLines;
    }

    /** {@code ore.bedrock.gold.a} -> {@code gold.a} — the row-suffix disambiguator (the gold.a/gold.b pair). */
    private static String shortName(String aRowName) {
        return aRowName.startsWith("ore.bedrock.") ? aRowName.substring("ore.bedrock.".length()) : aRowName;
    }

    /** The per-entry total height the wrappers report — face band + bottom padding. */
    public static int height(OreDistributionInfo.Entry aEntry) {
        return FACE_BAND_OFFSET + faceLines(aEntry).size() * LINE_HEIGHT;
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
