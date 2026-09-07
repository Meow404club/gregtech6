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
        // the p5 pump cover item (task p5-barrel-side-rules ruling ⑥) — the item shows the
        // out-facing plate art; the direction sprites live in the block atlas via GT6Atlases
        withExistingParent("cover_pump", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/cover_pump_out"));
        // the formal crowbar item (task p9-tool-crowbar): handheld parent = the vanilla
        // tool shape (the GTCEu tools/crowbar.json precedent), texture = the upstream
        // CROWBAR.png borrow (assets/README.md attribution)
        withExistingParent("crowbar", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/crowbar"));
        // the formal wire cutter item (task p10-tool-cutter): handheld parent = the vanilla
        // tool shape (the crowbar row shape), texture = the upstream WIRE_CUTTER.png
        // borrow (assets/README.md attribution)
        withExistingParent("cutter", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/cutter"));
        // the formal chisel item (task p16-chisel-decalcify): handheld parent = the vanilla
        // tool shape (the crowbar row shape), texture = the upstream HANDLE_CHISEL.png
        // borrow (assets/README.md attribution)
        withExistingParent("chisel", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/chisel"));
        // the p9 redstone-emitter cover item (task p9-redstone-cover-emitter) — the item
        // shows the tier-0 plate art (the offline-composed keypad panel; the sprites live
        // in textures/block/, auto-stitched by the vanilla atlas directory source)
        withExistingParent("cover_redstone_emitter", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/redstone_emitter/0"));
        // the p10 redstone conductor pair (task p10-cover-conductor-redstone) — the items
        // show their own plate art, byte-identical upstream borrows living in
        // textures/block/redstone_conductor/ (assets/README.md attribution)
        withExistingParent("cover_redstone_conductor_in", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/redstone_conductor/in"));
        withExistingParent("cover_redstone_conductor_out", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/redstone_conductor/out"));
        // the p10 redstone machine switch (task p10-cover-controller-redstone) — the item
        // shows the switch plate art, a byte-identical upstream borrow living in
        // textures/block/redstone_switch/ (assets/README.md attribution)
        withExistingParent("cover_redstone_machine_switch", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/redstone_switch/circuit"));
        // the p11 shutter + item-filter covers (task p11-cover-shutter-filter) — the items
        // show their normal/whitelist plate art, byte-identical upstream borrows living in
        // textures/block/shutter/ and textures/block/filteritem/ (assets/README.md attribution)
        withExistingParent("cover_shutter", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/shutter/normal"));
        withExistingParent("cover_item_filter", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/filteritem/normal"));
        // the p11 controller pair (task p11-cover-controllers) — the items show their
        // own plate art, byte-identical upstream borrows living in
        // textures/block/auto_redstone_switch/ and textures/block/cover_switch/
        // (assets/README.md attribution)
        withExistingParent("cover_auto_redstone_machine_switch", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/auto_redstone_switch/circuit"));
        withExistingParent("cover_controller", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/cover_switch/circuit"));
        // the p11 conveyor + robot arm tier ladders (task p11-cover-conveyor-robotarm) —
        // 10 items each (one per 512>>i timing tier), all sharing the out-facing plate art
        // (upstream's items all show the same cover texture; the in/out sprites live in
        // textures/block/{conveyor,robotarm}/, byte-identical upstream borrows with their
        // animation mcmeta — assets/README.md attribution)
        for (int i = 0; i < gregtech6.covers.covers.CoverConveyor.TIMING_TIERS.length; i++) {
            withExistingParent("cover_conveyor_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("block/conveyor/out"));
            withExistingParent("cover_robot_arm_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("block/robotarm/out"));
        }
        // the Integrated Circuit item (task p16-distillery-family ①) — item/generated over
        // the byte-identical upstream icon borrow (gt.integrated_circuit/0.png, config 0 —
        // the 256-icon damage ladder is the declared single-model cut, assets/README.md
        // attribution)
        withExistingParent("integrated_circuit", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/integrated_circuit"));
        // the spray-can family (task p22-spraycan-items) — 18 item/generated models over the
        // byte-identical upstream icon borrows (gt.multiitem.randomtools metas
        // 1000+2i/1096/999, assets/README.md attribution): one model per colour + the remover
        // + the empty can, walked over the DYE_IDS snake table so the model ids cannot drift
        // from the registered item ids (registry "spray_paint_" + id / "spray_paint_remover" /
        // "spray_can_empty")
        for (String tDye : gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS) {
            withExistingParent("spray_paint_" + tDye, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/spray/paint_" + tDye));
        }
        withExistingParent("spray_paint_remover", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/spray/remover"));
        withExistingParent("spray_can_empty", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/spray/empty"));
    }

    /** The material's item texture-set name, lower-snaked; empty falls back to upstream SET_NONE. */
    public static String iconsetOf(OreDictMaterial material) {
        List<String> tSets = material.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : MaterialPrefixItem.snakeCase(tSets.get(0));
    }
}
