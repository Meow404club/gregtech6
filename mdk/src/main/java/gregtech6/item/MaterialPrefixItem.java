package gregtech6.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

//? if forge {
import net.minecraftforge.common.ForgeHooks;
//?}

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.UT;
import gregtech6.recipes.RecipeMapFurnaceFuel;
import gregtech6.tooltip.GT6TooltipStyle;

/**
 * One material-scoped item for an OreDictPrefix x OreDictMaterial pair (upstream GT6 bundles one
 * PrefixItem per prefix with metadata = material index, gregapi/item/prefixitem/PrefixItem.java:52;
 * 1.20.1 has no item metadata, so the GTCEu Modern shape of one Item per pair applies,
 * TagPrefixItem.java:35-47).
 *
 * <p>Naming (GTCEu TagPrefixItem.java:68-86 + TagPrefix.java:1306-1333, simplified to two levels):
 * <ul>
 * <li>template key {@code gt6.tagprefix.<prefix_snake>} — "%s"-templated, filled at
 * {@link #getName(ItemStack)} time with the material's {@code gt6.material.<material_snake>}
 * translatable small unit (a plain descriptionId is translated without arguments, so the fill
 * has to happen on the returned Component, TagPrefix.java:1314-1316); the slot is a NESTED
 * translatable (the {@link gregtech6.block.wire.GTWireBlock#displayNameOf} material-slot shape),
 * so each locale resolves the material word in its OWN language — en resolves to mNameLocal
 * (the {@code gt6.material.*} en values ARE the mNameLocal faces, GT6EnUs.addMaterialNames),
 * zh resolves to the localized word (task i18n-material-fill-fix: the raw mNameLocal
 * literal rendered "Bronze锭" mixed-script names on a zh client);</li>
 * <li>special-case override key {@code gt6.<prefix_snake>_<material_snake>} — used only when a
 * translation actually exists (existence check mirrored from TagPrefix.java:1321).</li>
 * </ul>
 *
 * <p>Tint: the material colour from {@link OreDictMaterial#mRGBa} (TagPrefixItem.java:55-57 isomorph;
 * upstream GT6 renders {@code mRGBa[mPrefix.mState]} in PrefixItem.java:147).
 *
 * <p>Tooltip: the item is SELF-DESCRIBING on hover — the port has no OM.anydata_ pool, so
 * {@link #appendHoverText} replays the material-domain rows of the upstream global hook
 * (GT_API_Proxy_Client.onItemTooltip, ItemTooltipEvent HIGHEST :214-518) for its own pair
 * (task material-tooltip-face; the 22 lang faces live under gt6.tooltip.material.*).
 */
public class MaterialPrefixItem extends Item {

    public final OreDictPrefix prefix;
    public final OreDictMaterial material;
    /** "%s"-templated display key, filled with the material small unit ({@link #materialFill}). */
    public final String templateKey;
    /** Hand-localisable override key, preferred over the template when a translation exists. */
    public final String specialKey;

    public MaterialPrefixItem(Properties properties, OreDictPrefix prefix, OreDictMaterial material) {
        super(properties);
        this.prefix = prefix;
        this.material = material;
        String prefixSnake = snakeCase(prefix.mNameInternal);
        this.templateKey = "gt6.tagprefix." + prefixSnake;
        this.specialKey = "gt6." + prefixSnake + "_" + snakeCase(material.mNameInternal);
    }

    /** CamelCase internal name to snake_case (GTCEu FormattingUtil.toLowerCaseUnderscore semantics, TagPrefix.java:1306-1308). */
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
     * The material word the {@code gt6.tagprefix.*} templates fill their {@code %s} slot with:
     * the {@code gt6.material.<snake>} translatable SMALL UNIT (GTWireBlock.displayNameOf
     * material-slot shape), derived with the same {@link #snakeCase} the en lang walk uses
     * (GT6EnUs.addMaterialNames / addWireRowMaterialNames — one derivation, the task's
     * consistency mandate). Nesting lets each locale resolve the slot itself: en renders the
     * mNameLocal word exactly as before (the en key face IS mNameLocal), zh renders the localized
     * word. Public static seam: the lang parity test pins the fill shape offline (no Item
     * instance — the GTWireDisplayNameTest posture).
     */
    public static MutableComponent materialFill(OreDictMaterial aMaterial) {
        return Component.translatable("gt6.material." + snakeCase(aMaterial.mNameInternal));
    }

    @Override
    public Component getName(ItemStack stack) {
        // The %s fill only works on the returned Component (Card R3); TagPrefix.java:1314-1316 isomorph.
        if (hasTranslation(specialKey)) return Component.translatable(specialKey);
        return Component.translatable(templateKey, materialFill(material));
    }

    @Override
    public String getDescriptionId() {
        // Two-level fallback of TagPrefix.java:1318-1333 (mod-specific key, then template key).
        return hasTranslation(specialKey) ? specialKey : templateKey;
    }

    // -------------------------------------------------------------------------
    // The material tooltip face (task material-tooltip-face). The upstream material-domain
    // hover body is the GLOBAL hook GT_API_Proxy_Client.onItemTooltip (ItemTooltipEvent
    // HIGHEST, :214-518); the port has no OM.anydata_ pool (GT6RecipeMapScannerMolecular:72
    // self-declares the cut), so the GT item self-describes: this class REPLAYS the
    // material rows for its own prefix x material pair — the instanceof shape the
    // r11-tooltip-census card pinned. Row order, colors and wording are the upstream
    // lines verbatim (the per-row anchors cite them); lang-keyed faces live under
    // gt6.tooltip.material.* (GT6EnUs.addMaterialTooltip + the zh_cn_ref.tsv hand band).
    // -------------------------------------------------------------------------

    /** The 1.7.10 maxLevel-1 enchantment IDs of MT's vocabulary (Enchantment.getFullname :106-108 shows NO numeral when level == maxLevel == 1). */
    private static final java.util.Set<String> MAXLEVEL_ONE_ENCHANTS = java.util.Set.of("aquaAffinity", "flame", "infinity", "silkTouch");

    //? if forge {
    @Override
    public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
        super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
    //?} else {
    /*// 21.1: the Level second parameter became Item.TooltipContext (vanilla 1.21.1
    //Item.java:292 — the GT6MachineBlockItem dual-leg swap shape).
    @Override
    public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
        super.appendHoverText(aStack, aContext, aTooltip, aFlag);
    *///?}
        appendMaterialTooltip(aStack, aTooltip, aFlag.isAdvanced());
    }

    /** The furnace burn value the vanilla face would report (the RecipeMapFurnaceFuel :88/:90 bridge — ONE swap, the shared truth). */
    private static int furnaceBurnTime(ItemStack aStack) {
        //? if forge {
        return ForgeHooks.getBurnTime(aStack, RecipeType.SMELTING);
        //?} else {
        /*return aStack.getBurnTime(RecipeType.SMELTING); // 21.1: IItemStackExtension.getBurnTime(RecipeType) is the same query
        *///?}
    }

    /**
     * The nine data-driven row families (:320-507), upstream order: fuel value, tool stats,
     * chemical formula, sharpening/handle, source-of, the enchant block, flammability,
     * unburnable, the F3+H contained-materials branch and the origin line. The upstream
     * gates that can never fire here are collapsed and documented inline: :437 (the row is
     * skipped for GT multi-tile BLOCKS — this is a plain item) and :449 (skipped for
     * vanilla-registered items — a gt6 id never is).
     */
    private void appendMaterialTooltip(ItemStack aStack, List<Component> aTooltip, boolean aAdvanced) {
        // :320-321 — the fuel row reads the LIVE furnace bridge, the same query the fuel
        // recipe map synthesizes from. Material items carry no furnace-fuel registration
        // yet, so the row is DORMANT on this class until that card lands (the tank.1
        // dormancy precedent) — a mFurnaceBurnTime-poured row would claim a furnace
        // behaviour the vanilla face does not have (the barrel lie-ruling).
        int tBurnTime = furnaceBurnTime(aStack);
        if (tBurnTime > 0) aTooltip.add(Component.empty()
                .append(Component.translatable("gt6.tooltip.material.furnace_fuel").withStyle(GT6TooltipStyle.RED))
                .append(Component.literal(tBurnTime + " (").withStyle(GT6TooltipStyle.WHITE))
                .append(Component.literal(tBurnTime * RecipeMapFurnaceFuel.EU_PER_FURNACE_TICK + "HU").withStyle(GT6TooltipStyle.RED))
                .append(Component.literal(")").withStyle(GT6TooltipStyle.WHITE)));

        // :345-347 — prefix != null is unconditional here, so the gate collapses to mToolTypes > 0
        if (material.mToolTypes > 0) aTooltip.add(Component.translatable("gt6.tooltip.material.tool_stats",
                material.mToolQuality, material.mToolSpeed, material.mToolDurability).withStyle(GT6TooltipStyle.BLUE));

        // :348-350 — SHOW_CHEM_FORMULAS defaults true (GT_API.java:623; the single-mod port carries no config face)
        if (UT.Code.stringValid(material.mTooltipChemical) && prefix.contains(TD.Prefix.TOOLTIP_MATERIAL)) {
            aTooltip.add(Component.literal(material.mTooltipChemical).withStyle(GT6TooltipStyle.YELLOW));
        }

        // :361-362
        if (prefix.contains(TD.Prefix.NEEDS_SHARPENING)) {
            aTooltip.add(Component.translatable("gt6.tooltip.material.needs_sharpening").withStyle(GT6TooltipStyle.CYAN));
        }
        if (prefix.contains(TD.Prefix.NEEDS_HANDLE)) {
            aTooltip.add(Component.empty()
                    .append(Component.translatable("gt6.tooltip.material.needs_handle").withStyle(GT6TooltipStyle.CYAN))
                    .append(materialFill(material.mHandleMaterial).withStyle(GT6TooltipStyle.WHITE)));
        }

        // :364-372 — "Source of: " is an upstream code literal, so the row key holds it and the
        // joined material words ride as the WHITE sibling (each word a gt6.material.* fill)
        if (!material.mSourceOf.isEmpty() && prefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_DIRTY)) {
            MutableComponent tNames = null;
            for (OreDictMaterial tSource : material.mSourceOf) {
                if (tNames == null) tNames = materialFill(tSource).copy();
                else { tNames.append(Component.literal(", ")).append(materialFill(tSource)); }
            }
            aTooltip.add(Component.empty()
                    .append(Component.translatable("gt6.tooltip.material.source_of").withStyle(GT6TooltipStyle.CYAN))
                    .append(tNames.withStyle(GT6TooltipStyle.WHITE)));
        }

        // :382-427 — the enchant block, gated on TD.Prefix.TOOLTIP_ENCHANTS (ingot/dust/tool
        // heads carry it); armor (:416-422) stays off tool/weapon/ammo/tool-alike prefixes.
        // The EnchantmentStack ids are the 1.7.10 Enchantment FIELD names (MT.java:61 policy);
        // the display face is the vanilla 1.20.1 getFullname reproduction (Enchantment.java
        // :98-109 — the description-id translatable + the enchantment.level.N numeral), which
        // needs NO Holder — the Holder shape only matters for stored enchantments.
        if (prefix.contains(TD.Prefix.TOOLTIP_ENCHANTS)) {
            addEnchantRow(aTooltip, "gt6.tooltip.material.tool_enchants", material.mEnchantmentTools, true);
            addEnchantRow(aTooltip, "gt6.tooltip.material.weapon_enchants", material.mEnchantmentWeapons, false);
            addEnchantRow(aTooltip, "gt6.tooltip.material.ammo_enchants", material.mEnchantmentAmmo, false);
            addEnchantRow(aTooltip, "gt6.tooltip.material.ranged_enchants", material.mEnchantmentRanged, false);
            addEnchantRow(aTooltip, "gt6.tooltip.material.fishing_enchants", material.mEnchantmentFishing, false);
            if (!prefix.containsAny(TD.Prefix.TOOL_HEAD, TD.Prefix.WEAPON_ALIKE, TD.Prefix.AMMO_ALIKE, TD.Prefix.TOOL_ALIKE)) {
                addEnchantRow(aTooltip, "gt6.tooltip.material.armor_enchants", material.mEnchantmentArmors, false);
            }
        }

        // :437-447 — RED, FLAMMABLE and EXPLOSIVE compose the single combined row first
        if (material.contains(TD.Properties.FLAMMABLE)) {
            aTooltip.add(Component.translatable(material.contains(TD.Properties.EXPLOSIVE)
                    ? "gt6.tooltip.material.flammable_explosive" : "gt6.tooltip.material.flammable").withStyle(GT6TooltipStyle.RED));
        } else if (material.contains(TD.Properties.EXPLOSIVE)) {
            aTooltip.add(Component.translatable("gt6.tooltip.material.explosive").withStyle(GT6TooltipStyle.RED));
        }

        // :337-339 + :449 — over getAllMaterialWeights, which for a prefix item is exactly
        // [material x prefix.mWeight] (OreDictItemData.java:153-164; no byproducts on this class)
        if (material.contains(TD.Properties.UNBURNABLE)) {
            aTooltip.add(Component.translatable("gt6.tooltip.material.unburnable").withStyle(GT6TooltipStyle.GREEN));
        }

        // :452-479 — the F3+H branch; TooltipFlag.isAdvanced IS the showAdvancedItemTooltips semantic.
        // Alloys stay ONE row over the main stack (the :153-164 semantics — components are NOT
        // decomposed, the upstream alloy-ingot single-row face).
        if (aAdvanced) {
            OreDictMaterialStack tStack = new OreDictMaterialStack(material, prefix.mWeight);
            if (tStack.mAmount != 0 && !material.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT)) {
                aTooltip.add(Component.translatable("gt6.tooltip.material.contained_materials").withStyle(GT6TooltipStyle.DCYAN));
                aTooltip.add(containedMaterialRow(tStack));
            }
        } else {
            aTooltip.add(Component.translatable("gt6.tooltip.material.f3h_hint").withStyle(GT6TooltipStyle.DGRAY));
        }

        // :481-495 — the origin line (ST.isGT is unconditional for this class)
        if (material.mOriginalMod == null) {
            aTooltip.add(Component.translatable("gt6.tooltip.material.origin_unknown").withStyle(GT6TooltipStyle.BLUE));
        } else if (material.mOriginalMod.equals(MT.MD.MC.mID)) {
            aTooltip.add(Component.translatable("gt6.tooltip.material.origin_vanilla").withStyle(GT6TooltipStyle.BLUE));
        } else if (material.mOriginalMod.equals(MT.MD.GAPI.mID)) {
            aTooltip.add(Component.translatable(material.mID > 0 && material.mID < 8000
                    ? "gt6.tooltip.material.origin_periodic" : "gt6.tooltip.material.origin_random").withStyle(GT6TooltipStyle.BLUE));
        } else {
            aTooltip.add(Component.translatable("gt6.tooltip.material.origin_mod", material.mOriginalMod).withStyle(GT6TooltipStyle.BLUE));
        }
    }

    /** The PURPLE label + PINK joined names row (:386-414 shape); the tools row carries the fireAspect >= 3 " (Autosmelt)" annex (:388). */
    private void addEnchantRow(List<Component> aTooltip, String aKey, List<OreDictMaterial.EnchantmentStack> aStacks, boolean aToolsRow) {
        if (aStacks.isEmpty()) return;
        MutableComponent tNames = null;
        for (OreDictMaterial.EnchantmentStack tStack : aStacks) {
            MutableComponent tName = Component.translatable("enchantment.minecraft." + snakeCase(tStack.mEnchantmentID));
            if (tStack.mLevel != 1 || !MAXLEVEL_ONE_ENCHANTS.contains(tStack.mEnchantmentID)) {
                tName.append(Component.literal(" ")).append(Component.translatable("enchantment.level." + tStack.mLevel));
            }
            if (aToolsRow && "fireAspect".equals(tStack.mEnchantmentID) && tStack.mLevel >= 3) tName.append(Component.literal(" (Autosmelt)"));
            tNames = tNames == null ? tName : tNames.append(Component.literal(", ")).append(tName);
        }
        aTooltip.add(Component.empty()
                .append(Component.translatable(aKey).withStyle(GT6TooltipStyle.PURPLE))
                .append(tNames.withStyle(GT6TooltipStyle.PINK)));
    }

    /**
     * The F3+H component row (:459-475 verbatim): WHITE amount + YELLOW material word +
     * the M/B/W stat block (CYAN labels, RED Kelvin units, YELLOW kg).
     */
    private static Component containedMaterialRow(OreDictMaterialStack aStack) {
        double tWeight = aStack.weight();
        long tFrac = ((long)(tWeight * 1000)) % 1000; // the :461 fraction digits
        return Component.empty()
                .append(Component.literal(displayUnits(aStack.mAmount) + " ").withStyle(GT6TooltipStyle.WHITE))
                .append(materialFill(aStack.mMaterial).withStyle(GT6TooltipStyle.YELLOW))
                .append(Component.literal(" (").withStyle(GT6TooltipStyle.WHITE))
                .append(Component.literal("M: ").withStyle(GT6TooltipStyle.CYAN))
                .append(Component.literal(aStack.mMaterial.mMeltingPoint + "").withStyle(GT6TooltipStyle.WHITE))
                .append(Component.literal("K ").withStyle(GT6TooltipStyle.RED))
                .append(Component.literal(" B: ").withStyle(GT6TooltipStyle.CYAN))
                .append(Component.literal(aStack.mMaterial.mBoilingPoint + "").withStyle(GT6TooltipStyle.WHITE))
                .append(Component.literal("K ").withStyle(GT6TooltipStyle.RED))
                .append(Component.literal(" W: ").withStyle(GT6TooltipStyle.CYAN))
                .append(Component.literal((long)tWeight + "." + (tFrac < 1 ? "000" : tFrac < 10 ? "00" + tFrac : tFrac < 100 ? "0" + tFrac : tFrac)).withStyle(GT6TooltipStyle.WHITE))
                .append(Component.literal("kg").withStyle(GT6TooltipStyle.YELLOW))
                .append(Component.literal(")").withStyle(GT6TooltipStyle.WHITE));
    }

    /** The upstream UT.Code.displayUnits face (UT.java:1670-1674 verbatim) — the port gregapi carries no UT.Code instance methods beyond the ones ported. */
    private static String displayUnits(long aAmount) {
        if (aAmount < 0) return "?.???";
        long tDigits = ((aAmount % CS.U) * 1000) / CS.U;
        return (aAmount / CS.U) + "." + (tDigits < 1 ? "000" : tDigits < 10 ? "00" + tDigits : tDigits < 100 ? "0" + tDigits : tDigits);
    }

    /** Opaque ARGB material colour for tint index 0, from mRGBa[mPrefix.mState] (PrefixItem.java:147). */
    @OnlyIn(Dist.CLIENT)
    public int tintARGB() {
        short[] tRGBa = material.mRGBa[prefix.mState]; // OreDictMaterial.java:108-109, short[state][r,g,b,a]
        return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt, UT.java:1580-1582
    }

    /** Single ItemColor shared by all material prefix items (registered once, GTClientHandlers). */
    @OnlyIn(Dist.CLIENT)
    public static ItemColor tintColor() {
        return (stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof MaterialPrefixItem tItem ? tItem.tintARGB() : -1;
    }

    /** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
    private static int bind8(long aValue) {
        return (int)Math.max(0, Math.min(255, aValue));
    }

    private static boolean hasTranslation(String aKey) {
        // net.minecraft.locale.Language is client-side content (getInstance Language.java:83, has :97);
        // the check is dist-guarded so the dedicated server never loads the seam.
        return FMLEnvironment.dist == Dist.CLIENT && ClientSeam.hasTranslation(aKey);
    }

    /** Client-only seam: GTCEu TagPrefix.java:1321 does Language.getInstance().has(key) unguarded; here it is side-isolated. */
    @OnlyIn(Dist.CLIENT)
    private static final class ClientSeam {
        static boolean hasTranslation(String aKey) {
            return net.minecraft.locale.Language.getInstance().has(aKey);
        }
    }
}
