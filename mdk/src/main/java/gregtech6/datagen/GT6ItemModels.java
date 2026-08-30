package gregtech6.datagen;

import java.util.List;

import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * One generated model per registered material prefix item: parent = minecraft item/generated,
 * layer0 = {@code gt6:item/material_sets/<iconset>/<prefix>} (GTCEu GTModels.java:62-78 shape,
 * ADR-P2-4). Shared texture per (iconset, prefix) pair — 2469 items resolve onto ~100 textures,
 * the material colour comes from the runtime ItemColor tint (MaterialPrefixItem.tintColor,
 * tintIndex 0), mirroring upstream's grayscale icon + colour modulation.
 *
 * <p>Iconset resolution archaeology (task card): upstream PrefixItem.java:136-138 resolves the
 * item icon as {@code material.mTextureSetsItems.get(prefix.mIconIndexItem)} — the TEXTURE SET is
 * a property of the MATERIAL (assigned via {@code .setTextures(SET_X)}, upstream
 * OreDictMaterial.java:1024-1029), the prefix only indexes into the set's icon list.
 * OreDictPrefix.mNameTextureSet (OreDictPrefix.java:84/101/322) has zero readers in the whole
 * upstream tree and is NOT the icon source. Port equivalent: OreDictMaterial.mTextureSetsItems
 * holds the texture-set name assigned by MT.setTextures (MT.java:210-215); an empty list falls
 * back to "none" = upstream SET_NONE (TextureSet.java:188), which is what a never-explicitly-
 * textured material resolves to upstream (SET_NONE[1].mList receives every prefix icon via
 * TextureSet.addToAll, TextureSet.java:78). Set names are lower-snaked for asset paths
 * (METALLIC -&gt; metallic, GEM_VERTICAL -&gt; gem_vertical), matching GTCEu Modern's
 * material_sets directory convention.
 */
public final class GT6ItemModels extends ItemModelProvider {

    public GT6ItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GT6DataGenerators.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (GT6DatagenItems.Entry tEntry : GT6DatagenItems.collect()) {
            withExistingParent(tEntry.itemId(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/material_sets/"
                    + iconsetOf(tEntry.material())
                    + "/" + MaterialPrefixItem.snakeCase(tEntry.prefix().mNameInternal)));
        }
    }

    /** The material's item texture-set name, lower-snaked; empty falls back to upstream SET_NONE. */
    public static String iconsetOf(OreDictMaterial material) {
        List<String> tSets = material.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : MaterialPrefixItem.snakeCase(tSets.get(0));
    }
}
