package gregtech6.datagen;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import gregtech6.client.ore.GTOreBakedModel;
import gregtech6.registry.GT6OreBlocks;

/**
 * The ore universe blockstate + item-model provider (task p30-ore-3-datagen spec ①/②).
 * The composition-model shape of {@code GT6BlockStates.addPrefixBlocks} (GT6BlockStates
 * .java:1550-1568, the 175-models/3773-blocks precedent) applied to the 3922-block ore
 * universe ({@code GT6OreBlocks.registrationOrder()}, 26 families x 74 form-rows x M=53):
 *
 * <ul>
 * <li>ONE SHARED placeholder block model per distinct BASE texture ({@code
 *     GTOreBakedModel.baseSpriteOf}): the 9 vanilla anchors + their 2 cobble broken forms
 *     + the 17 GT stones = 28 models — NOT a model per pair (the explicit red line) and
 *     not even a model per (base,SET): the dual-sprite ore look is the CUSTOM BAKED MODEL's
 *     face ({@code GTOreClientListener} bake dispatch), the JSON stays a graceful cube;</li>
 * <li>one blockstate JSON per block (3922) — a single variant onto the shared model;</li>
 * <li>one item model JSON per block (3922), {@code withExistingParent} onto the shared
 *     block model — same provider, same pass, so the parent resolves in the
 *     ExistingFileHelper (the BlockStateProvider.java:103-104 flush order precedent).</li>
 * </ul>
 *
 * <p>Total generated JSON = 3922 blockstates + 3922 item models + 28 shared models = 7872
 * (the composition-strategy expectation pinned by GT6OreRenderDatagenTest; the per-pair
 * alternative would be ~11.8k with zero shared visual identity). The placeholder cube
 * carries tintindex 0 on all faces (the tintedCubeAll idiom) so a pre-wrap render is
 * visibly material-coloured — loud over camouflaged; the real render carries the tint on
 * the OVERLAY layer only (the GTOreBakedModel javadoc carries the upstream two-pass
 * semantics). The overlay sprites are stitched via the GT6Atlases sources, not referenced
 * from these JSONs — the SET axis is per-material and the shared model is per-base.
 */
public final class GT6OreBlockStates extends BlockStateProvider {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("gt6");

    public GT6OreBlockStates(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GT6DataGenerators.MOD_ID, existingFileHelper);
    }

    /**
     * Forge 1.20.1's DataGenerator rejects two providers carrying the same name
     * (DataGenerator.java:83 "Duplicate provider: Block States: gt6" — the super's
     * {@code "Block States: " + modid} collides with GT6BlockStates), so this provider
     * carries its own. Same shape on the 21.1 leg (DataProvider.getName interface method).
     */
    @Override
    public String getName() {
        return "Ore Block States: " + GT6DataGenerators.MOD_ID;
    }

    @Override
    protected void registerStatesAndModels() {
        Map<ResourceLocation, ModelFile> tShared = new HashMap<>();
        int tBlocks = 0;
        for (GT6OreBlocks.OreKey tKey : GT6OreBlocks.registrationOrder()) {
            // live-only: the registration walk ran before GatherData (the GTMaterialBlocks
            // .blockArray() precedent — a null here means the registry face broke, fail loud)
            Block tBlock = GT6OreBlocks.blocks().get(tKey).get();
            ResourceLocation tBase = GTOreBakedModel.baseSpriteOf(tKey.family(), tKey.kind());
            String tModelName = modelNameOf(tBase);
            ModelFile tModel = tShared.computeIfAbsent(tBase, tTex -> tintedCubeAll(tModelName, tTex));
            simpleBlock(tBlock, tModel);
            itemModels().withExistingParent(GT6OreBlocks.path(tKey), modLoc(tModelName));
            tBlocks++;
        }
        LOGGER.info("GT6 ore blocks: {} per-pair blockstates + item models over {} shared base-texture models"
                + " (the GTOreBakedModel dual-sprite face carries the SET overlay)", tBlocks, tShared.size());
    }

    /**
     * The shared model path for one base texture: {@code block/ore/<texture path minus its
     * block/ segment>} — minecraft:block/stone → block/ore/stone,
     * gt6:block/stones/granite_black/stone → block/ore/stones/granite_black/stone. The
     * explicit {@code block/} prefix keeps the tracked location == models/block/ore/... ==
     * the item-parent lookup (the addPrefixBlocks extendWithFolder pin).
     */
    static String modelNameOf(ResourceLocation aBaseTexture) {
        String tTail = aBaseTexture.getPath();
        return ("block/ore/" + (tTail.startsWith("block/") ? tTail.substring("block/".length()) : tTail));
    }

    /**
     * One tinted cube_all block model — the GT6BlockStates.tintedCubeAll builder verbatim
     * (private there; this provider sits outside its file): parent block/block, one full
     * 0..16 element, six faces on {@code #all} with cullface and tintindex 0.
     */
    private ModelFile tintedCubeAll(String aName, ResourceLocation aTexture) {
        BlockModelBuilder tModel = models().getBuilder(aName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("all", aTexture)
                .texture("particle", "#all");
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#all").tintindex(0).cullface(aDir))
                .end();
        return tModel;
    }
}
