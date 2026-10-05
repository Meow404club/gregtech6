package gregtech6.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.fluid.GTFluids;

/**
 * The GT6 fluid-tag datagen home (review-seat seam fix, task worldgen-deepocean-corals):
 * the three natural water bodies (task worldgen-water-replace — {@code seawater}/
 * {@code riverwater}/{@code waterdirty}, the Loader_Worldgen.java:576-578 ocean/river/
 * swamp replacement rows) join the {@code minecraft:water} fluid tag.
 *
 * <p><b>Why</b>: the upstream {@code WD.anywater} gate (WorldgenDeepOcean.java:49 and
 * every other worldgen water probe) covers BOTH the vanilla water column AND the GT
 * waterlike bodies — the 1.7.10 {@code instanceof BlockWaterlike} arm. The modern carrier
 * of that equivalence is the fluid tag: the consumers ({@code FluidTags.WATER}) range from
 * the deep-ocean pylon water gate (GT6DeepOceanFeature — the prismarine pylons stop
 * generating wherever the water-replace feature has already swapped the column to GT
 * seawater) to seagrass/kelp placement and the water-mob spawn placements. Without the
 * tag the swapped oceans are invisible to all of them — the recorded seat-17 seam.
 *
 * <p><b>Shape</b>: the vanilla-tag extension face (the {@code mineable/shovel} precedent,
 * GT6BlockTags) — the mod datapack's {@code data/minecraft/tags/fluids/water.json} JOINS
 * vanilla's own tag at load (datagenerated, {@code //} no hand-written JSON). The members
 * ride {@code addOptional} (the required:false doctrine, the twilight deadrock-tag
 * precedent): the GT fluid ids resolve through the mod's own registry, and the optional
 * form keeps the tag loader-safe even if a future CUT retires a family. Source + flowing
 * pairs ({@code <name>} / {@code <name>_flowing}, the {@link GTFluids} family registration
 * naming) — the tag consumers probe FluidStates in both phases.
 *
 * <p>Directory: {@code data/minecraft/tags/fluids/**} on the canonical 1.20.1 producer,
 * the singular alias {@code data/minecraft/tags/fluid/**} riding
 * {@link GT6DualDirectoryFaces} (the RENAMES row this task adds — the javadoc's "extend
 * when a new family ships" face) and the datagen_tree_check SEGMENT_MAP entry.
 */
public final class GT6FluidTags extends FluidTagsProvider {

    /** The three natural water-body fluids + their flowing twins (the WATER_REPLACE_BLOCK_IDS family, GTFluids.java:3204-3207). */
    private static final String[] WATER_BODIES = {"seawater", "riverwater", "waterdirty"};

    public GT6FluidTags(PackOutput aOutput, CompletableFuture<HolderLookup.Provider> aLookupProvider,
            ExistingFileHelper aExistingFileHelper) {
        super(aOutput, aLookupProvider, GT6DataGenerators.MOD_ID, aExistingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider aProvider) {
        var tAppender = tag(FluidTags.WATER);
        for (String tBody : WATER_BODIES) {
            tAppender.addOptional(new ResourceLocation(GT6DataGenerators.MOD_ID, tBody))
                     .addOptional(new ResourceLocation(GT6DataGenerators.MOD_ID, tBody + "_flowing"));
        }
    }

    @Override
    public String getName() {
        return "GT6 Fluid Tags";
    }
}
