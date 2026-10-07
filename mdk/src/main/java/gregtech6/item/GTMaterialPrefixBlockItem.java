package gregtech6.item;

import java.util.Objects;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.block.ore.GTOreFallingBlock;
import gregtech6.components.IOreDictItemDataOverrideItem;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The BlockItem of a {@link GTMaterialPrefixBlock}, carrying its (prefix, material) pair
 * (task prefixblock-registry). Naming mirrors {@link MaterialPrefixItem} with ZERO
 * per-pair lang: the composed template key {@code gt6.tagprefix.<prefix_snake>} — already
 * generated for every OP prefix (GT6EnUs.addPrefixTemplates) — is filled with the material's
 * {@code gt6.material.<snake>} translatable small unit at getName time
 * ({@link MaterialPrefixItem#materialFill}, the task i18n-material-fill-fix shared seam:
 * each locale resolves the slot in its own language, en = mNameLocal verbatim, zh = the
 * localized word), so "Block of %s Ingots" style names come for free (the block*
 * prefixes carry mMaterialPre/mMaterialPost exactly like the item prefixes, upstream
 * OP.java:345-351). The special-case key {@code gt6.<prefix_snake>_<material_snake>} stays
 * the hand-translation layer, preferred only when a translation exists.
 *
 * <p>Broken ores (task ore-broken-name, the declared reverse-upstream deviation): the
 * BROKEN form of an ore family (the {@code kind} field on GTOreBlock/GTOreFallingBlock)
 * composes its name from the extra
 * template {@code gt6.tagprefix.<prefix_snake>_broken} instead of the family's shared
 * template — upstream gives the broken block no distinct name (PrefixBlockItem.java:108-114),
 * the port does (user ruling 2026-09-28); the templates are the datagen card's face
 * (GT6EnUs/GT6ZhCn addBrokenOreTemplates, one per family that owns a separate broken block).
 *
 * <p>Tint: {@code material.mRGBa[prefix.mState]} (upstream PrefixBlockItem.java:103
 * {@code mRGBa[mBlock.mPrefix.mState]}); the ItemColor registration itself is the render
 * card's surface (prefixblock-render, GTClientHandlers) — this class only supplies the
 * shared {@link #tintColor()} implementation.
 */
public class GTMaterialPrefixBlockItem extends BlockItem implements IOreDictItemDataOverrideItem {

    public final OreDictPrefix prefix;
    public final OreDictMaterial material;
    /** "%s"-templated display key, filled with the material small unit ({@link MaterialPrefixItem#materialFill}). */
    public final String templateKey;
    /** Hand-localisable override key, preferred over the template when a translation exists. */
    public final String specialKey;

    public GTMaterialPrefixBlockItem(Item.Properties properties, OreDictPrefix prefix, OreDictMaterial material, Block block) {
        super(Objects.requireNonNull(block, "block"), properties);
        this.prefix = prefix;
        this.material = material;
        String prefixSnake = GTMaterialItems.snakeCase(prefix.mNameInternal);
        this.templateKey = "gt6.tagprefix." + prefixSnake;
        this.specialKey = "gt6." + prefixSnake + "_" + GTMaterialItems.snakeCase(material.mNameInternal);
    }

    /** The item is its own (prefix, material) data — arm 2 of the central component face's read
     * chain (upstream OreDictManager.getItemData_ :694, {@link IOreDictItemDataOverrideItem});
     * the MaterialPrefixItem javadoc carries the face note. */
    @Override public OreDictItemData getOreDictItemData(ItemStack stack) {
        return new OreDictItemData(prefix, material);
    }

    @Override
    public Component getName(ItemStack stack) {
        // The %s fill only works on the returned Component (the MaterialPrefixItem/Card R3 form);
        // the slot is the shared gt6.material.<snake> translatable unit (i18n-material-fill-fix).
        // ore-broken-name: the broken ore form gets its own template
        // gt6.tagprefix.<prefix_snake>_broken — an EXPLICIT reverse-upstream enhancement
        // (user ruling 2026-09-28): upstream oreBroken shares the family prefix and composes
        // the SAME display name (PrefixBlockItem.java:108-114 oredict compose, no broken
        // face), the port distinguishes it. Checked BEFORE the specialKey: the pair's
        // gt6.<prefix>_<material> hand key names the NORMAL form (broken≡normal upstream),
        // so letting it win here would erase the distinction the ruling asks for.
        if (isBrokenForm()) return Component.translatable(templateKey + "_broken", MaterialPrefixItem.materialFill(material));
        if (hasTranslation(specialKey)) return Component.translatable(specialKey);
        return Component.translatable(templateKey, MaterialPrefixItem.materialFill(material));
    }

    /**
     * The BROKEN form of an ore family — the ore block carries the kind. Single-inheritance
     * split (GTOreFallingBlock.java:14-18 javadoc): the gravity broken rows are GTOreFallingBlock,
     * the non-gravity ones GTOreBlock; both mirror {@code kind} (in practice every SEPARATE
     * broken row is gravity=true, the falling leg — GTOreBlock alone would silently miss them).
     */
    private boolean isBrokenForm() {
        return (getBlock() instanceof GTOreBlock tOre && tOre.kind == GT6OreBlocks.FormKind.BROKEN)
            || (getBlock() instanceof GTOreFallingBlock tFalling && tFalling.kind == GT6OreBlocks.FormKind.BROKEN);
    }

    @Override
    public String getDescriptionId() {
        // Two-level fallback mirrored from MaterialPrefixItem (block-level keys are card-B surface).
        return hasTranslation(specialKey) ? specialKey : templateKey;
    }

    /** Opaque ARGB material colour for tint index 0, from mRGBa[prefix.mState] (PrefixBlockItem.java:103). */
    @OnlyIn(Dist.CLIENT)
    public int tintARGB() {
        short[] tRGBa = material.mRGBa[prefix.mState]; // OreDictMaterial fRGBa-family field, port name mRGBa
        return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt, UT.java:1580-1582
    }

    /** Single ItemColor shared by all material prefix block items (registered once, card B). */
    @OnlyIn(Dist.CLIENT)
    public static ItemColor tintColor() {
        return (stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof GTMaterialPrefixBlockItem tItem ? tItem.tintARGB() : -1;
    }

    /** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
    private static int bind8(long aValue) {
        return (int)Math.max(0, Math.min(255, aValue));
    }

    private static boolean hasTranslation(String aKey) {
        // net.minecraft.locale.Language is client-side content; the check is dist-guarded so the
        // dedicated server never loads the seam (the MaterialPrefixItem.ClientSeam pattern).
        return FMLEnvironment.dist == Dist.CLIENT && ClientSeam.hasTranslation(aKey);
    }

    /** Client-only seam (MaterialPrefixItem.ClientSeam mirror; the original is private there). */
    @OnlyIn(Dist.CLIENT)
    private static final class ClientSeam {
        static boolean hasTranslation(String aKey) {
            return net.minecraft.locale.Language.getInstance().has(aKey);
        }
    }
}
