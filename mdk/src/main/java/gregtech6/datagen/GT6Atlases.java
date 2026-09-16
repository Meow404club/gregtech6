package gregtech6.datagen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.SpriteSourceProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.covers.GT6Covers;

/**
 * The atlas sources provider (task p4-cover-core ⑥ — "land the atlases/blocks.json sources;
 * this card's consumed textures join the atlas"; the C-grade foundation shipped zero
 * textures, GTRenderModelListener
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

    /**
     * {@code lookupProvider} is the shared cross-version seam: Forge 1.20.1's GatherDataEvent
     * already exposes {@code getLookupProvider()} (the forge-1.20.1 datagen docs pass it the
     * same way), so the constructor signature is identical on both legs — only the
     * {@code super} wiring differs (1.21.1 javap: {@code (PackOutput, CompletableFuture<
     * HolderLookup.Provider>, String, ExistingFileHelper)} — the mod id moved before the
     * file helper and the registry lookup joined the parameter list). The 1.20.1 leg takes
     * the parameter and ignores it.
     */
    public GT6Atlases(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
        ExistingFileHelper existingFileHelper) {
        //? if neoforge {
        /*
        super(output, lookupProvider, GT6DataGenerators.MOD_ID, existingFileHelper);
         *///?} else {
        super(output, existingFileHelper, GT6DataGenerators.MOD_ID);
        //?}
    }

    /**
     * The provider hook fork: 1.21.1 has NO {@code addSources} any more — the abstract
     * subclass contract is JsonCodecProvider's {@code gather()} (javap 21.1.249:
     * SpriteSourceProvider declares only the atlas constants + {@code atlas(ResourceLocation)},
     * NeoForgeSpriteSourceProvider overrides {@code gather}); Forge 1.20.1 keeps its own
     * {@code addSources} hook. Both one-line hooks delegate to the shared body below.
     */
    @Override
    //? if neoforge {
    /*
    protected void gather() {
        addAtlasSources();
    }
     *///?} else {
    protected void addSources() {
        addAtlasSources();
    }
    //?}

    /** The shared atlas source list (byte-identical output on both legs — same JSON format). */
    private void addAtlasSources() {
        ResourceLocation tCoverSprite = GT6Covers.ironPlateSprite();
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(tCoverSprite, Optional.empty()));
        // the pipe flow-arrow sprite (task p4-pipe-flow-control spec ④) — stitched by the
        // runtime-built GTFluidPipeFlowModel quads, so it must live in the block atlas
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFlowModel.ARROW_SPRITE, Optional.empty()));
        // the pump-cover direction sprites (task p5-barrel-side-rules spec ③) — the plate
        // renderer stitches whichever the visual lane currently encodes (0 = out, 1 = in)
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.covers.covers.CoverPump.PUMP_OUT_SPRITE, Optional.empty()));
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.covers.covers.CoverPump.PUMP_IN_SPRITE, Optional.empty()));
        // the four grayscale C-Foam sprites (task p25-c-foam-pipe-spray spec ⑤) — stitched by
        // the runtime-built GTFluidPipeFoamModel quads (FRESH/HARDENED x normal/owned)
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFoamModel.FRESH_SPRITE, Optional.empty()));
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFoamModel.FRESH_OWNED_SPRITE, Optional.empty()));
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFoamModel.HARDENED_SPRITE, Optional.empty()));
        atlas(BLOCKS_ATLAS).addSource(new SingleFile(gregtech6.client.render.GTFluidPipeFoamModel.HARDENED_OWNED_SPRITE, Optional.empty()));
        // task p30-ore-3-datagen — the ore SET overlay sprites: the GTOreBakedModel looks
        // them up at bake, but they ride NO model JSON (the shared-placeholder composition),
        // so the atlas source IS the consumer-side stitching wiring (this class's javadoc).
        // One (ore, ore_small) pair per distinct SET across the material axis; the PNGs are
        // card ②'s borrow face — until that card lands the sources resolve to missingno,
        // the declared ADR ④ intermediate state.
        for (ResourceLocation tOverlay : gregtech6.client.ore.GTOreBakedModel.overlaySprites()) {
            atlas(BLOCKS_ATLAS).addSource(new SingleFile(tOverlay, Optional.empty()));
        }
    }
}
