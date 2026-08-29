package gregtech6.item;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * One material-scoped item for an OreDictPrefix x OreDictMaterial pair (upstream GT6 bundles one
 * PrefixItem per prefix with metadata = material index, gregapi/item/prefixitem/PrefixItem.java:52;
 * 1.20.1 has no item metadata, so the GTCEu Modern shape of one Item per pair applies,
 * TagPrefixItem.java:35-47).
 *
 * <p>Naming (GTCEu TagPrefixItem.java:68-86 + TagPrefix.java:1306-1333, simplified to two levels):
 * <ul>
 * <li>template key {@code gt6.tagprefix.<prefix_snake>} — "%s"-templated, filled at
 * {@link #getName(ItemStack)} time with the material name (a plain descriptionId is translated
 * without arguments, so the fill has to happen on the returned Component, TagPrefix.java:1314-1316);</li>
 * <li>special-case override key {@code gt6.<prefix_snake>_<material_snake>} — used only when a
 * translation actually exists (existence check mirrored from TagPrefix.java:1321).</li>
 * </ul>
 *
 * <p>Tint: the material colour from {@link OreDictMaterial#mRGBa} (TagPrefixItem.java:55-57 isomorph;
 * upstream GT6 renders {@code mRGBa[mPrefix.mState]} in PrefixItem.java:147).
 */
public class MaterialPrefixItem extends Item {

    public final OreDictPrefix prefix;
    public final OreDictMaterial material;
    /** "%s"-templated display key, filled with the material name. */
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

    @Override
    public Component getName(ItemStack stack) {
        // The %s fill only works on the returned Component (Card R3); TagPrefix.java:1314-1316 isomorph.
        if (hasTranslation(specialKey)) return Component.translatable(specialKey);
        return Component.translatable(templateKey, material.mNameLocal);
    }

    @Override
    public String getDescriptionId() {
        // Two-level fallback of TagPrefix.java:1318-1333 (mod-specific key, then template key).
        return hasTranslation(specialKey) ? specialKey : templateKey;
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
