package gregtech6.datagen;

import java.util.Optional;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.SpriteSourceProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.covers.GT6Covers;

/**
 * The atlas sources provider (task p4-cover-core ⑥ — "atlases/blocks.json sources 落地,
 * 本卡消费贴图入图集"; the C-grade foundation shipped zero textures, GTRenderModelListener
 * class note). Every sprite a dynamic model stitches into runtime-built plate quads must
 * be present in the block atlas; the provider writes {@code assets/gt6/atlases/blocks.json}
 * with an explicit single-file source for the cover sprite — the cross-namespace merge of
 * atlas sources makes it join the vanilla directory sources (SpriteResourceLoader:30,
 * R4-5), which already carry the {@code gt6:item/...} tree, so the entry is the explicit
 * consumer-side wiring the render route prescribes rather than a load-bearing need.
 *
 * <p>The sprite id comes from {@link GT6Covers#ironPlateSprite()} — the same derivation
 * the item models used, so the cover texture cannot drift from the item texture.
 */
public final class GT6Atlases extends SpriteSourceProvider {

    public GT6Atlases(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper, GT6DataGenerators.MOD_ID);
    }

    @Override
    protected void addSources() {
        ResourceLocation tCoverSprite = GT6Covers.ironPlateSprite();
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(tCoverSprite, Optional.empty()));
    }
}
