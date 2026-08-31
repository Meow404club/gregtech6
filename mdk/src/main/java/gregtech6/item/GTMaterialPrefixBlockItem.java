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

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.registry.GTMaterialItems;

/**
 * The BlockItem of a {@link GTMaterialPrefixBlock}, carrying its (prefix, material) pair
 * (task p8-prefixblock-registry). Naming mirrors {@link MaterialPrefixItem} with ZERO
 * per-pair lang: the composed template key {@code gt6.tagprefix.<prefix_snake>} — already
 * generated for every OP prefix (GT6EnUs.addPrefixTemplates) — is filled with the material
 * name at getName time, so "Block of %s Ingots" style names come for free (the block*
 * prefixes carry mMaterialPre/mMaterialPost exactly like the item prefixes, upstream
 * OP.java:345-351). The special-case key {@code gt6.<prefix_snake>_<material_snake>} stays
 * the hand-translation layer, preferred only when a translation exists.
 *
 * <p>Tint: {@code material.mRGBa[prefix.mState]} (upstream PrefixBlockItem.java:103
 * {@code mRGBa[mBlock.mPrefix.mState]}); the ItemColor registration itself is the render
 * card's surface (p8-prefixblock-render, GTClientHandlers) — this class only supplies the
 * shared {@link #tintColor()} implementation.
 */
public class GTMaterialPrefixBlockItem extends BlockItem {

    public final OreDictPrefix prefix;
    public final OreDictMaterial material;
    /** "%s"-templated display key, filled with the material name. */
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

    @Override
    public Component getName(ItemStack stack) {
        // The %s fill only works on the returned Component (the MaterialPrefixItem/Card R3 form).
        if (hasTranslation(specialKey)) return Component.translatable(specialKey);
        return Component.translatable(templateKey, material.mNameLocal);
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
