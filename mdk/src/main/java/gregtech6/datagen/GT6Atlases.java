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
 * be present in the block atlas. The Forge-convention target is the atlas id's own
 * namespace ({@code BLOCKS_ATLAS} = minecraft:blocks → the generated file is
 * {@code assets/minecraft/atlases/blocks.json}); at runtime
 * {@code SpriteResourceLoader.load} parses EVERY resource on the path's stack
 * (SpriteResourceLoader.java:72-84 addAll) and concatenates the sources, so the mod file
 * joins vanilla's own atlas definition instead of replacing it.
 *
 * <p>The sprite id comes from {@link GT6Covers#ironPlateSprite()} — the same derivation
 * the item models used, so the cover texture cannot drift from the item texture. (The
 * sprite is in any case already picked up by the vanilla cross-namespace
 * {@code directory("item")} source; the single-file entry is the explicit consumer-side
 * wiring the render route prescribes.)
 */
public final class GT6Atlases extends SpriteSourceProvider {

    public GT6Atlases(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper, GT6DataGenerators.MOD_ID);
    }

    @Override
    protected void addSources() {
        ResourceLocation tCoverSprite = GT6Covers.ironPlateSprite();
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(tCoverSprite, Optional.empty()));
        // the pipe flow-arrow sprite (task p4-pipe-flow-control spec ④) — stitched by the
        // runtime-built GTFluidPipeFlowModel quads, so it must live in the block atlas
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFlowModel.ARROW_SPRITE, Optional.empty()));
    }
}
