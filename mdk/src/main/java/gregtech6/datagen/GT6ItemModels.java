package gregtech6.datagen;

import java.util.List;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6BookText;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * One generated model per registered material prefix item: parent = minecraft item/generated,
 * layer0 = {@code gt6:item/material_sets/<iconset>/<prefix>} (GTCEu GTModels.java:62-78 shape,
 * ADR-P2-4). Shared texture per (iconset, prefix) pair — 2469 items resolve onto ~100 textures,
 * the material colour comes from the runtime ItemColor tint (MaterialPrefixItem.tintColor,
 * tintIndex 0), mirroring upstream's grayscale icon + colour modulation.
 *
 * <p>Layer 2 (task tool-model-layers): upstream pairs every materialicon with an
 * un-tinted OVERLAY pass (TextureSet.java:113-126) — added as layer1 wherever the sprite
 * file exists on the borrow face (never inferred; the sets disagree on what the overlay
 * carries, research.p27-render-three-fixes F2). The vanilla layer number IS the tint
 * index (ItemModelGenerator.java:15), and the shared ItemColor already returns -1 for
 * tintIndex != 0, so the overlay face needs zero runtime code. The tool rows below use
 * the same layer number = pass number mapping over the upstream four-pass icon
 * (ToolStats.java:267-287).
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
            String tSet = iconsetOf(tEntry.material());
            String tPrefix = spriteNameOf(tEntry.prefix());
            // the OVERLAY second pass (task tool-model-layers): upstream registers
            // every materialicon as a base + "<NAME>_OVERLAY" pair and draws pass0 tinted
            // with the material colour / pass1 un-tinted (TextureSet.java:113-116 and
            // :124-126). The overlay is not always mere shading — the rockGt body
            // (DULL/METALLIC sets) and the chemtube glass wall live entirely in the
            // OVERLAY sprite, while BRICK's rockGt base is already the full cobble — so
            // the layer is gated strictly on FILE EXISTENCE, never inferred from the
            // prefix or set (research.p27-render-three-fixes F2 ruling). The layer is
            // un-tinted for free: the shared ItemColor returns -1 for tintIndex != 0
            // (MaterialPrefixItem.java:104).
            String tOverlay = tPrefix + "_overlay";
            if (existingFileHelper.exists(modLoc("item/material_sets/" + tSet + "/" + tOverlay), TEXTURE)) {
                withExistingParent(tEntry.itemId(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/material_sets/" + tSet + "/" + tPrefix))
                    .texture("layer1", modLoc("item/material_sets/" + tSet + "/" + tOverlay));
            } else {
                withExistingParent(tEntry.itemId(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/material_sets/" + tSet + "/" + tPrefix));
            }
        }
        // the p5 pump cover item (task barrel-side-rules ruling ⑥; icon source swap task
        // cover-item-icons-dual-source) — the ITEM icon is the upstream item-domain art
        // (items/gt.multiitem.technological/12020.png, the LV pump the port's single item
        // mirrors, MultiItemRandom.java:366), NOT the machine-face overlay: upstream item
        // icons and installed overlays are two separate PNG domains, and the out/in
        // direction sprites stay in the block atlas via GT6Atlases for the INSTALLED face
        withExistingParent("cover_pump", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/pump"));
        // ─── the tool family multi-layer wave (task tool-model-layers) ───
        // Upstream renders every tool icon as FOUR passes (ToolStats.java:267-287):
        // pass0 = head base (tinted with the primary material), pass1 = head OVERLAY
        // (UNCOLOURED), pass2 = handle base (tinted with the secondary material),
        // pass3 = handle OVERLAY (UNCOLOURED). The vanilla item-model layer number IS
        // the tint index (ItemModelGenerator.java:15 LAYERS = layer0..layer4, the
        // processFrames layer argument becomes the quad tint index), so the four passes
        // map 1:1 onto layer0..layer3. The layer STRUCTURE is what this wave restores —
        // the tint ladder has since landed runtime-side (issue6-tool-4layer-tint +
        // tint-coverage-batch registered the fourPassTintARGB ItemColor faces over
        // these same layers), so the models here stay tint-blind by design.

        // the formal crowbar item (task tool-crowbar): handheld parent = the vanilla
        // tool shape; layer0 = the upstream CROWBAR.png iconset borrow + layer1 = the
        // CROWBAR_OVERLAY.png shadow borrow (assets/README.md attribution). Upstream's
        // handle is VOID (GT_Tool_Crowbar.getIcon :142-144), so passes 2/3 draw nothing
        // and the model is two-layer.
        withExistingParent("crowbar", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/crowbar"))
            .texture("layer1", modLoc("item/crowbar_overlay"));
        // the formal wire cutter item (task tool-cutter): the crowbar row shape;
        // layer0 = the WIRE_CUTTER.png borrow + layer1 = the WIRE_CUTTER_OVERLAY.png
        // shadow borrow (assets/README.md attribution); VOID handle = two-layer.
        withExistingParent("cutter", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/cutter"))
            .texture("layer1", modLoc("item/cutter_overlay"));
        // the formal chisel item (task chisel-decalcify): FOUR layers. Head = the
        // toolHeadChisel materialicon pair on the default primary Steel's texture set
        // (GT_Tool_Chisel.getIcon :87-89 — MT.Steel, the alloymachore family's
        // SET_METALLIC default, MT.java:247); handle = the HANDLE_CHISEL iconset pair
        // borrow (item/chisel.png + chisel_overlay.png, assets/README.md attribution) —
        // the old single-layer model borrowed only the handle silhouette, dropping the
        // head entirely (the census structural-loss case).
        withExistingParent("chisel", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_chisel"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_chisel_overlay"))
            .texture("layer2", modLoc("item/chisel"))
            .texture("layer3", modLoc("item/chisel_overlay"));
        // the formal file item (task tool-system): FOUR layers, the chisel row
        // shape — head = the toolHeadFile materialicon pair (Steel = metallic set,
        // GT_Tool_File.getIcon :91-93), handle = the HANDLE_FILE iconset pair borrow
        // (the old single layer carried only the handle; the head layer is the restore).
        withExistingParent("file", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_file"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_file_overlay"))
            .texture("layer2", modLoc("item/file"))
            .texture("layer3", modLoc("item/file_overlay"));
        // the formal saw item (task tool-system): FOUR layers, the chisel row shape
        // — head = the toolHeadSaw materialicon pair (Steel = metallic set,
        // GT_Tool_Saw.getIcon :186-188), handle = the HANDLE_SAW iconset pair borrow.
        withExistingParent("saw", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_saw"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_saw_overlay"))
            .texture("layer2", modLoc("item/saw"))
            .texture("layer3", modLoc("item/saw_overlay"));
        // the formal builder wand item (task builder-wand): FOUR layers — head = the
        // EMERALD-set toolHeadBuilderwand pair (default primary Heliodor = the emerald
        // factory, MT.java:1374; layer0 keeps the existing item/builder_wand.png byte
        // borrow, layer1 = the matching EMERALD OVERLAY pass), handle = the Scorched-wood
        // stick pair (default secondary MT.WOODS.Scorched, GT_Tool_Builderwand.getIcon
        // :51-53; woodnormal = SET_WOOD, MT.java:311-312). Upstream tints head/handle
        // with their material colours — the port shows them un-tinted at the single tier
        // (the family declared deviation).
        withExistingParent("builder_wand", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/builder_wand"))
            .texture("layer1", modLoc("item/material_sets/emerald/tool_head_builderwand_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the formal screwdriver item (task screwdriver-item, four-layer migration
        // task issue6-tool-4layer-tint SUPERSEDES the composition ruling): the chisel
        // row shape — head = the toolHeadScrewdriver materialicon pair (the default
        // primary Steel = metallic set, in-repo), handle = the HANDLE_SCREWDRIVER iconset
        // pair borrow (item/screwdriver.png + screwdriver_overlay.png, assets/README.md
        // attribution — the composed single is retired). Upstream render:
        // GT_Tool_Screwdriver.getIcon :115-117 (head = the material's texture-set icon,
        // handle = the iconset pair), tint = getRGBa :120-122 (head primary with the
        // Steel fallback, handle secondary with the Spruce fallback).
        withExistingParent("screwdriver", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_screwdriver"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_screwdriver_overlay"))
            .texture("layer2", modLoc("item/screwdriver"))
            .texture("layer3", modLoc("item/screwdriver_overlay"));
        // the formal hard hammer item (task tool-hammer-wrench, four-layer migration
        // task issue6-tool-4layer-tint SUPERSEDES the composition ruling): the
        // soft-hammer row shape — head = the toolHeadHammer materialicon pair (the
        // default primary Steel = metallic set, in-repo), handle = the wood stick pair
        // (the secondary MT.WOODS.Spruce rides SET_WOOD's stick icon,
        // GT_Tool_HardHammer.getIcon :123 handle half — zero new sprites, the composed
        // single is retired). Tint = getRGBa :127-129 (head primary Steel fallback,
        // handle secondary Spruce fallback).
        withExistingParent("hammer", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_hammer"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_hammer_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the formal wrench item (task tool-hammer-wrench): handheld parent = the
        // vanilla tool shape; layer0 = the byte-identical WRENCH.png iconset borrow +
        // layer1 = the WRENCH_OVERLAY.png pass borrow (transparent upstream, borrowed for
        // structure parity — assets/README.md attribution); VOID handle = two-layer.
        withExistingParent("wrench", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/wrench"))
            .texture("layer1", modLoc("item/wrench_overlay"));
        // the formal small bending cylinder item (task food-can-row0): the wrench
        // row shape; layer0 = the BENDING_CYLINDER_SMALL.png borrow + layer1 = its
        // OVERLAY pass borrow (transparent upstream — assets/README.md attribution);
        // VOID handle = two-layer.
        withExistingParent("bending_cylinder_small", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/bending_cylinder_small"))
            .texture("layer1", modLoc("item/bending_cylinder_small_overlay"));
        // the six blade tools (task w5-t2-blade-six) — the chisel/file four-layer row
        // shape. Sword: head = the toolHeadSword metallic-set pair (upstream getIcon :113-115
        // primary Steel), handle = the HANDLE_SWORD iconset pair borrow (item/sword.png).
        // Knife/Butchery Knife/Club: the single composed iconset pair borrows (upstream
        // KNIFE/BUTCHERYKNIFE/CLUB icons, VOID handles — GT_Tool_Knife.getIcon :85-87 /
        // GT_Tool_ButcheryKnife :97-99 / GT_Tool_Club :113-115), two-layer.
        withExistingParent("sword", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_sword"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_sword_overlay"))
            .texture("layer2", modLoc("item/sword"))
            .texture("layer3", modLoc("item/sword_overlay"));
        withExistingParent("knife", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/knife"))
            .texture("layer1", modLoc("item/knife_overlay"));
        withExistingParent("butchery_knife", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/butchery_knife"))
            .texture("layer1", modLoc("item/butchery_knife_overlay"));
        withExistingParent("club", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/club"))
            .texture("layer1", modLoc("item/club_overlay"));
        // Axe: head = the toolHeadAxe metallic-set pair (GT_Tool_Axe.getIcon :151-153), handle
        // = the stick pair (the secondary Spruce — the builder-wand handle borrow shape).
        withExistingParent("axe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_axe"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_axe_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // Double Axe: the toolHeadAxeDouble metallic-set pair + the stick pair.
        withExistingParent("axe_double", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_axe_double"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_axe_double_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the dig-family ladder rows (task dig-ladder): the axe row shape — head =
        // the toolHead metallic-set pair (layer0 = the TINTED head base, tint index 0,
        // the GT6ToolLadder material colour), layer1 = the OVERLAY shadow; handle = the
        // wood stick pair. The dig-six card (w5-t1-dig-six) shipped these six items
        // WITHOUT model rows (the registered items fell back to the missing model) — this
        // batch restores the dig band's render face alongside the material ladder.
        withExistingParent("pickaxe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_pickaxe"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_pickaxe_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the gem pick's head = the FINE set (the Diamond default primary's texture set,
        // GT_Tool_PickaxeGem.getIcon :34 — the gem identity rides the head texture set).
        withExistingParent("pickaxe_gem", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/fine/tool_head_pickaxe_gem"))
            .texture("layer1", modLoc("item/material_sets/fine/tool_head_pickaxe_gem_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("pickaxe_construction", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_construction_pickaxe"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_construction_pickaxe_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("shovel", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_shovel"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_shovel_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("spade", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_spade"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_spade_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("universal_spade", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_universal_spade"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_universal_spade_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("hoe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_hoe"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_hoe_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the formal soft hammer item (task w5-t3-machine-face-four): FOUR layers, the
        // builder-wand row shape — head = the RUBBER-set toolHeadHammer pair (the upstream
        // primary ANY.Rubber default, GT_Tool_SoftHammer.getIcon :120 — SET_RUBBER, not the
        // steel head), handle = the wood stick pair (the upstream secondary
        // MT.WOODS.Spruce, the same :120 row; the builder-wand borrow posture — zero new
        // sprite files, all four layers are existing material_sets borrows).
        withExistingParent("soft_hammer", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/rubber/tool_head_hammer"))
            .texture("layer1", modLoc("item/material_sets/rubber/tool_head_hammer_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        // the formal monkey wrench item (task w5-t3-machine-face-four): the wrench row
        // shape — layer0 = the byte-identical MONKEYWRENCH.png iconset borrow + layer1 =
        // the MONKEYWRENCH_OVERLAY.png pass borrow (assets/README.md attribution).
        withExistingParent("monkey_wrench", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/monkey_wrench"))
            .texture("layer1", modLoc("item/monkey_wrench_overlay"));
        // the formal magnifying glass item (task w5-t3-machine-face-four): the wrench
        // row shape — layer0 = the byte-identical MAGNIFYING_GLASS.png iconset borrow +
        // layer1 = the MAGNIFYING_GLASS_OVERLAY.png pass borrow (assets/README.md
        // attribution; the VOID handle = the two-layer form).
        withExistingParent("magnifying_glass", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/magnifying_glass"))
            .texture("layer1", modLoc("item/magnifying_glass_overlay"));
        // the formal pincers item (task w5-t3-machine-face-four): the wrench row
        // shape — layer0 = the byte-identical PINCERS.png iconset borrow + layer1 = the
        // PINCERS_OVERLAY.png pass borrow (assets/README.md attribution).
        withExistingParent("pincers", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/pincers"))
            .texture("layer1", modLoc("item/pincers_overlay"));
        // the crop seed (task cbc-3-crop-data-assets): one plain generated item over the family
        // crop-stick sprite — IC2 renders ItemCrop itself with crop_stick.png (ItemCrop.java:27),
        // the cbc-1 borrow re-seated (assets/README.md, the cbc-3 ledger band).
        withExistingParent("crop_seed", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/crop_stick"));
        // the electric nineteen (task w5-t6-electric-nineteen): the two-layer tool
        // face per id — layer0 = the head/tip sprite, layer1 = the handle/power-unit
        // pass (the upstream getIcon(false)/getIcon(true) pass order, the crowbar/cutter
        // row shape). The head faces ride the IN-REPO material_sets metallic sprites
        // (Steel default), the handles/power-units/jackhammer are the 12 byte-identical
        // upstream iconset borrows (assets/README.md attribution); the OVERLAY passes
        // are cut (the family un-tinted deviation). The monkey-wrench rows reuse the
        // wrench head art (the upstream getIcon inheritance verbatim); the two
        // jackhammer forms share the single JACKHAMMER sprite (the upstream icon face).
        withExistingParent("mining_drill_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_drill"))
            .texture("layer1", modLoc("item/electric/power_unit_lv"));
        withExistingParent("mining_drill_mv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_drill"))
            .texture("layer1", modLoc("item/electric/power_unit_mv"));
        withExistingParent("mining_drill_hv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_drill"))
            .texture("layer1", modLoc("item/electric/power_unit_hv"));
        withExistingParent("chainsaw_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_chainsaw"))
            .texture("layer1", modLoc("item/electric/power_unit_lv"));
        withExistingParent("chainsaw_mv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_chainsaw"))
            .texture("layer1", modLoc("item/electric/power_unit_mv"));
        withExistingParent("chainsaw_hv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_chainsaw"))
            .texture("layer1", modLoc("item/electric/power_unit_hv"));
        withExistingParent("wrench_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_lv"));
        withExistingParent("wrench_mv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_mv"));
        withExistingParent("wrench_hv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_hv"));
        withExistingParent("monkey_wrench_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_lv"));
        withExistingParent("monkey_wrench_mv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_mv"));
        withExistingParent("monkey_wrench_hv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_wrench"))
            .texture("layer1", modLoc("item/electric/power_unit_hv"));
        withExistingParent("jackhammer_hv_normal", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/electric/jackhammer"));
        withExistingParent("jackhammer_hv_no_ores", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/electric/jackhammer"));
        withExistingParent("buzzsaw_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_buzz_saw"))
            .texture("layer1", modLoc("item/electric/handle_buzzsaw"));
        withExistingParent("screwdriver_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_screwdriver"))
            .texture("layer1", modLoc("item/electric/handle_electric_screwdriver"));
        withExistingParent("hand_drill_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/electric/tip_electric_drill"))
            .texture("layer1", modLoc("item/electric/handle_electric_drill"));
        withExistingParent("hand_mixer_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/electric/tip_electric_mixer"))
            .texture("layer1", modLoc("item/electric/handle_electric_mixer"));
        withExistingParent("trimmer_lv", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/electric/tip_electric_trimmer"))
            .texture("layer1", modLoc("item/electric/handle_electric_trimmer"));
        // the pocket multitool family (task w5-t7-pocket-eight) — eight handheld
        // models over the byte-identical upstream iconset borrows (the POCKET_MULTITOOL_*
        // pair per form + its OVERLAY pass, the two-layer wrench row shape — the pocket
        // icons are complete single sprites, GT_Tool_Pocket_Multitool.getIcon :42-44,
        // assets/README.md attribution), walked over the POCKET_FORMS id table so the
        // model ids cannot drift (the spray-can band convention). The closed multitool
        // rides the POCKET_MULTITOOL_CLOSED pair (the sprite tail "multitool").
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tForm : gregtech6.registry.GT6Tools.POCKET_FORMS) {
            String tTail = tForm.getId().getPath().replace("pocket_multitool", "").replaceFirst("^_", "");
            if (tTail.isEmpty()) tTail = "multitool"; // the closed form → the _CLOSED pair
            withExistingParent(tForm.getId().getPath(), mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/pocket/" + tTail))
                .texture("layer1", modLoc("item/pocket/" + tTail + "_overlay"));
        }
        // the gun family (task pistol-family-items) — THREE four-layer handheld models,
        // the chisel row shape over iconset borrows on BOTH halves: layer0/1 = the gun
        // body pair (the upstream getIcon(false) pass, PISTOL/CARBINE/RIFLE.png +
        // _OVERLAY.png), layer2/3 = the handle pair (the getIcon(true) pass,
        // HANDLE_*.png — assets/README.md attribution). Upstream tints body/handle with
        // the primary/secondary material colours — the port shows them un-tinted at the
        // single steel tier (the family declared deviation; the stamped crafting rows
        // carry the identity for the ladder card). Walked over GUN_ROWS so the model ids
        // cannot drift.
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tGun : gregtech6.registry.GT6Tools.GUN_ROWS) {
            String tPath = tGun.getId().getPath();
            withExistingParent(tPath, mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + tPath))
                .texture("layer1", modLoc("item/" + tPath + "_overlay"))
                .texture("layer2", modLoc("item/" + tPath + "_handle"))
                .texture("layer3", modLoc("item/" + tPath + "_handle_overlay"));
        }
        // the food-can census (task food-can-row0, completed by food-meat-items) — 58
        // item/generated models over the byte-identical upstream icon borrows
        // (gt.multiitem.randomtools/998 for the empty can, gt.multiitem.cans/<meta> for the
        // nine families + the air cans — assets/README.md attribution), walked over the
        // registered tab table so the model ids cannot drift (the spray-can band
        // convention; the texture file drops the food_can_ family prefix,
        // food_can_rotten_tiny → food_can/rotten_tiny)
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tCan : gregtech6.registry.GT6FoodCans.TAB_TABLE) {
            String tPath = tCan.getId().getPath();
            withExistingParent(tPath, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food_can/" + tPath.substring("food_can_".length())));
        }
        // the food-item family (task food-items-core, T4a-extended by food-meat-items, the
        // pool-drain-food-t5-tail raisin row) — 39 item/generated models over the
        // byte-identical upstream icon borrows
        // (gt.multiitem.food/<meta> per row — assets/README.md attribution), walked over
        // FOOD_ROWS so the model ids cannot drift (the food-can band convention; the
        // texture file drops the food_ family prefix, food_can_rotten_tiny →
        // food_can/rotten_tiny 同型)
        for (gregtech6.registry.GT6Foods.FoodRow tFood : gregtech6.registry.GT6Foods.FOOD_ROWS) {
            withExistingParent(tFood.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food/" + tFood.id().substring("food_".length())));
        }
        // the food-item T3 bake chain (task food-bake-items) — 60 item/generated models over
        // the byte-identical upstream icon borrows (the T1 band walk form verbatim), plus
        // the 6 food-grade molds under item/shape_foodmold/ (the shape_extruder directory
        // convention; the sprites ARE standalone technological-atlas files, unlike the
        // composed shape_extruder placeholders — assets/README.md sha256 ledger)
        for (gregtech6.registry.GT6BakeFoods.BakeRow tBake : gregtech6.registry.GT6BakeFoods.BAKE_ROWS) {
            withExistingParent(tBake.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food/" + tBake.id().substring("food_".length())));
        }
        for (String tMold : new String[] {"empty", "bun", "bread", "baguette", "cylinder", "toast"}) {
            withExistingParent("shape_foodmold_" + tMold, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/shape_foodmold/" + tMold));
        }
        // the food T5a crop band (task food-crop-items) — 61 item/generated models over the
        // byte-identical upstream icon borrows (the T1 band walk form verbatim: 49 foods +
        // 12 inedibles, gt.multiitem.food/<meta> per row — assets/README.md attribution)
        for (gregtech6.registry.GT6CropFoods.CropFoodRow tCrop : gregtech6.registry.GT6CropFoods.FOOD_ROWS) {
            withExistingParent(tCrop.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food/" + tCrop.id().substring("food_".length())));
        }
        for (gregtech6.registry.GT6CropFoods.CropPlainRow tPlain : gregtech6.registry.GT6CropFoods.PLAIN_ROWS) {
            withExistingParent(tPlain.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food/" + tPlain.id().substring("food_".length())));
        }
        // the bottles domain (task food-bottles-min + btl-bottles-families-a) — one
        // item/generated model per ROWS walk over the byte-identical upstream icon borrows
        // (gt.multiitem.bottles/<meta> per row — assets/README.md sha256 ledger); the
        // texture basename strips the food_ prefix (the ketchup.png precedent). bottle_empty
        // parents the VANILLA glass_bottle model: upstream OP.bottle.dat(MT.Empty)
        // (OP.java:229) is a material-prefix technical container with no own sprite anywhere
        // in the upstream resources (the declared deviation, ledger-annotated)
        for (gregtech6.registry.GT6Bottles.BottleRow tRow : gregtech6.registry.GT6Bottles.ROWS) {
            if ("bottle_empty".equals(tRow.id())) {
                withExistingParent(tRow.id(), mcLoc("item/glass_bottle"));
            } else {
                withExistingParent(tRow.id(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/bottle/" + tRow.texture()));
            }
        }
        // the foodside small-item band (task vanilla-alias-foodside) — 6 item/generated
        // models over the item/foodside/ textures: the four Remains icons are the
        // byte-identical upstream atlas borrows (gt.multiitem.food/12100-12103 per row —
        // assets/README.md attribution), the two honey drops are the COMPOSED placeholders
        // (the P20 stdlib generator convention — no upstream sprite exists to borrow)
        for (gregtech6.registry.GT6FoodsideItems.SideRow tRow : gregtech6.registry.GT6FoodsideItems.ROWS) {
            withExistingParent(tRow.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/foodside/" + tRow.id()));
        }
        // the robot-component domain (task robotics-chain) — 60 item/generated models over
        // the byte-identical upstream multiitem borrows (gt.multiitem.technological
        // 12000-12009/12040-12049/12060-12069/12080-12089 + gt.multiitem.randomtools
        // 8000-8009/8500-8509, one standalone sprite per item — assets/README.md sha256
        // ledger), walked over the ROWS table so model ids cannot drift (the bottle band
        // convention)
        for (gregtech6.registry.GT6Robotics.RobotRow tRow : gregtech6.registry.GT6Robotics.ROWS) {
            withExistingParent(tRow.id(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/robotics/" + tRow.id()));
        }
        // the extruder-mold row0 subset (task w1-press-extruder-molds) — 2 item/generated
        // models over the composed placeholder icons (the mold-plate/mold-rod 16x16 stdlib
        // generator, the P20 placeholder-PNG convention; the upstream multiitem icons are
        // meta-atlas tiles with no standalone sprite file to borrow)
        // the battery family (task w4-battery-storage): 37 item/generated models over
        // the per-family upstream sprite borrows (the bake_battery_textures.py products,
        // assets/README.md attribution), walked over the ROWS table so model ids cannot
        // drift; the 5 cells share one cell sprite; the 7 circuit carriers each show their
        // upstream tier art (task circuit-config-icons: the gt.multiitem.technological
        // 30301-30306 borrows, assets/README.md attribution — tier-0 has no upstream item
        // of its own, LoaderOreDictReRegistrations.java:375 makes the T1 Basic item the
        // gt:circuit0 ground, so primitive rides the same 30301 art, the declared share);
        // the 12 box items parent their block models (the dynamo band convention)
        for (gregtech6.registry.GT6Batteries.BatteryRow tRow : gregtech6.registry.GT6Batteries.ROWS) {
            withExistingParent(tRow.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/battery/" + tRow.family()));
        }
        for (String tPath : gregtech6.registry.GT6Batteries.CELL_ITEMS.keySet()) {
            withExistingParent(tPath, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/battery/cell"));
        }
        for (String tPath : gregtech6.registry.GT6Batteries.CIRCUIT_ITEMS.keySet()) {
            withExistingParent(tPath, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/circuit/" + tPath.substring("circuit_".length())));
        }
        // (the 12 box ITEMS parent their block models from GT6BlockStates.addBatteryBoxes —
        //  the item face validates against the blockstates provider's own output, the dynamo band convention)
        // the extruder-mold FULL family (task mold-extruder-shapes; the row0 pair rode
        // task w1-press-extruder-molds, the head family task toolhead-r11c-extruder-heads)
        // — 64 item/generated models walked over the registration order (the addExtruderMolds
        // drift-throw form) over the byte-identical upstream icon borrows (the
        // gt.multiitem.technological 10000-10031 / 10200-10231 meta tiles now lifted as
        // standalone sprites, assets/README.md sha256 ledger; the texture basename drops
        // the family prefix, the shape_foodmold convention)
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tMold : gregtech6.registry.GT6ExtruderMolds.MOLDS) {
            String tPath = tMold.getId().getPath();
            String tTexture = tPath.startsWith("shape_simple_ex_")
                    ? "item/shape_simple_ex/" + tPath.substring("shape_simple_ex_".length())
                    : "item/shape_extruder/" + tPath.substring("shape_extruder_".length());
            withExistingParent(tPath, mcLoc("item/generated")).texture("layer0", modLoc(tTexture));
        }
        // the press-mold trio (task explosives-chain, MultiItemTechnological.java:352-354
        // metas 10896-10898) — 3 item/generated models over the byte-identical upstream
        // sprite borrows (assets/README.md sha256 ledger; the r11c head-family borrow form)
        withExistingParent("shape_press_bullet_casing_small", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_press/bullet_casing_small"));
        withExistingParent("shape_press_bullet_casing_medium", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_press/bullet_casing_medium"));
        withExistingParent("shape_press_bullet_casing_large", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_press/bullet_casing_large"));
        // the dynamite family (task explosives-chain, Loader_MultiTileEntities.java:2236-2238
        // metas 32104/32713/32712) — one shared 2-layer model form per item over the borrowed
        // greyscale block sprites: layer 0 = the "colored" body (the ItemColor material tint,
        // GT6ExplosivesTintListener), layer 1 = the overlay (as-is)
        withExistingParent("boomstick", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/explosives/dynamite"))
            .texture("layer1", modLoc("item/explosives/dynamite_overlay"));
        withExistingParent("dynamite", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/explosives/dynamite"))
            .texture("layer1", modLoc("item/explosives/dynamite_overlay"));
        withExistingParent("dynamite_strong", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/explosives/dynamite"))
            .texture("layer1", modLoc("item/explosives/dynamite_overlay"));
        // the slicer-blade row0 subset (task slicer-row-domain) — 2 item/generated models
        // over the composed placeholder icons (the blade-grid/blade-split 16x16 stdlib
        // generator, the shape_extruder band convention; the upstream multiitem icons are
        // meta-atlas tiles with no standalone sprite file to borrow)
        withExistingParent("shape_slicer_grid", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/grid"));
        withExistingParent("shape_slicer_split", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/split"));
        // the slicer census completion (task recipes-obtainability, ruling B) — the
        // frame + five remaining blade forms over the same composed placeholder convention
        withExistingParent("shape_slicer_empty", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/empty"));
        withExistingParent("shape_slicer_flat", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/flat"));
        withExistingParent("shape_slicer_eigths", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/eigths"));
        withExistingParent("shape_slicer_eigths_hollow", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/eigths_hollow"));
        withExistingParent("shape_slicer_quarters", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/quarters"));
        withExistingParent("shape_slicer_quarters_hollow", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_slicer/quarters_hollow"));
        // the p9 redstone-emitter cover item (task redstone-cover-emitter; icon source
        // swap task cover-item-icons-dual-source) — the item icon is the upstream
        // item-domain borrow (items/gt.multiitem.technological/1021.png, meta 1021
        // Redstone Emitter), the keypad overlay sprites stay in the block atlas for the
        // INSTALLED face
        withExistingParent("cover_redstone_emitter", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/redstone_emitter"));
        // the p10 redstone conductor pair (task cover-conductor-redstone; icon source
        // swap task cover-item-icons-dual-source) — the item icons are the upstream
        // item-domain borrows (metas 1029/1030, MultiItemTechnological.java:88-89)
        withExistingParent("cover_redstone_conductor_in", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/redstone_conductor_in"));
        withExistingParent("cover_redstone_conductor_out", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/redstone_conductor_out"));
        // the p10 redstone machine switch (task cover-controller-redstone; icon source
        // swap task cover-item-icons-dual-source) — the item icon is the upstream
        // item-domain borrow (meta 1005, MultiItemTechnological.java:64)
        withExistingParent("cover_redstone_machine_switch", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/redstone_machine_switch"));
        // the p11 shutter + item-filter covers (task cover-shutter-filter; icon source
        // swap task cover-item-icons-dual-source) — the item icons are the upstream
        // item-domain borrows (metas 1026/1023 — the shutter's framed plate art is the
        // card's proof case that the two domains differ), the in-world plate overlays stay in
        // textures/block/{shutter,filteritem}/ (assets/README.md attribution)
        withExistingParent("cover_shutter", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/shutter"));
        withExistingParent("cover_item_filter", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/item_filter"));
        // the p31 item-retriever cover (task retriever-cover; icon source swap task
        // cover-item-icons-dual-source) — the item icon is the upstream item-domain
        // borrow (meta 1031, MultiItemTechnological.java:90)
        withExistingParent("cover_item_retriever", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/item_retriever"));
        // the p33 logistics cover family (task logistics-covers-12; icon source swap
        // task cover-item-icons-dual-source) — 14 items, each icon the upstream
        // item-domain borrow (metas 1086-1099, MultiItemTechnological.java:101-114);
        // the in-world face sprites stay in the block domain directories
        for (String[] tRow : new String[][] {
                {"cover_logistics_display_cpu_logic", "display_cpu_logic"}, {"cover_logistics_display_cpu_control", "display_cpu_control"},
                {"cover_logistics_display_cpu_storage", "display_cpu_storage"}, {"cover_logistics_display_cpu_conversion", "display_cpu_conversion"},
                {"cover_logistics_fluid_export", "fluid_export"}, {"cover_logistics_fluid_import", "fluid_import"}, {"cover_logistics_fluid_storage", "fluid_storage"},
                {"cover_logistics_item_export", "item_export"}, {"cover_logistics_item_import", "item_import"}, {"cover_logistics_item_storage", "item_storage"},
                {"cover_logistics_generic_export", "generic_export"}, {"cover_logistics_generic_import", "generic_import"}, {"cover_logistics_generic_storage", "generic_storage"},
                {"cover_logistics_generic_dump", "generic_dump"}}) {
            withExistingParent(tRow[0], mcLoc("item/generated"))
                .texture("layer0", modLoc("item/covers/" + tRow[1]));
        }
        // the p34 gameplay cover family (task covers-gameplay-10; icon source swap task
        // cover-item-icons-dual-source) — the 7 singletons with a technological-domain
        // meta show their upstream item icons (metas 1022/1020/2000/1024/1007/1008/1027,
        // MultiItemTechnological.java:66-90/176); the in-world plate art stays in the
        // block domain (the vent facet table etc. — task cover-underlay-census)
        for (String[] tRow : new String[][] {
                {"cover_vent", "vent"}, {"cover_drain", "drain"}, {"cover_pressure_valve", "pressure_valve"},
                {"cover_fluid_filter", "fluid_filter"}, {"cover_selector_redstone", "selector_redstone"},
                {"cover_selector_manual", "selector_manual"}, {"cover_selector_button_panel", "selector_button_panel"}}) {
            withExistingParent(tRow[0], mcLoc("item/generated"))
                .texture("layer0", modLoc("item/covers/" + tRow[1]));
        }
        // DECLARED EXCEPTIONS (task cover-item-icons-dual-source): upstream the torch and
        // repeater covers ride the VANILLA items, not a technological-domain meta
        // (CoverRegistry.put(ST.make(Blocks.redstone_torch/Items.repeater) —
        // GT_API.java:799-802), so no GT PNG exists; per upstream semantics the item
        // icons point at the vanilla textures the vanilla items draw (the repeater row is
        // byte-for-byte the vanilla item model's layer0; the torch's vanilla GUI face is
        // a 3D cross-plane block model, the port keeps the census-stable 2D sprite form —
        // the recorded deviation)
        withExistingParent("cover_redstone_torch", mcLoc("item/generated"))
            .texture("layer0", mcLoc("block/redstone_torch"));
        withExistingParent("cover_redstone_repeater", mcLoc("item/generated"))
            .texture("layer0", mcLoc("block/repeater"));
        // the 16 tag-selector ladder items — upstream the selector-tag covers ride the
        // Integrated Circuit ITEM itself, damage 0-15 (ItemIntegratedCircuit.java:87),
        // whose icon per damage is the circuit config art (registerIcons :118) — so the
        // item icons REUSE the circuit-config-icons borrows (item/integrated_circuit/),
        // zero new PNGs; the in-world digit plates stay in block/selectortag/
        for (int i = 0; i < 16; i++) {
            withExistingParent("cover_selector_tag_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/integrated_circuit/" + i));
        }
        // the p11 controller pair (task cover-controllers; icon source swap task
        // cover-item-icons-dual-source) — the item icons are the upstream item-domain
        // borrows (metas 1006/1025, MultiItemTechnological.java:65/84); the in-world
        // circuit plate art stays in textures/block/{auto_redstone_switch,cover_switch}/
        // (assets/README.md attribution)
        withExistingParent("cover_auto_redstone_machine_switch", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/auto_redstone_machine_switch"));
        withExistingParent("cover_controller", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/controller"));
        // the p11 conveyor + robot arm tier ladders (task cover-conveyor-robotarm; icon
        // source swap task cover-item-icons-dual-source) — 10 items each, and upstream
        // gives EVERY tier its own item icon (the 12040+i / 12080+i PNGs are per-tier
        // distinct art, MultiItemTechnological.java:51/53), so the ladder walks the tier
        // borrows instead of all tiers sharing one overlay sprite (the in/out sprites
        // stay in textures/block/{conveyor,robotarm}/ with their animation mcmeta for
        // the INSTALLED face — assets/README.md attribution)
        for (int i = 0; i < gregtech6.covers.covers.CoverConveyor.TIMING_TIERS.length; i++) {
            withExistingParent("cover_conveyor_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/covers/conveyor_" + i));
            withExistingParent("cover_robot_arm_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/covers/robot_arm_" + i));
        }
        // the p35 display/scale cover family (task covers-display-scale-6; icon source
        // swap task cover-item-icons-dual-source) — the item icons are the upstream
        // item-domain borrows (metas 1002/1003/1004/1014/1018 and the auto-reboot
        // 1009-1013 ladder, MultiItemTechnological.java:61-78); the in-world display/
        // circuit plate art stays in textures/block/{status_display,energy_display,
        // auto_switch,auto_timer_switch}/ (assets/README.md attribution; the sensors
        // reuse the existing redstone-sensor sprites borrowed this card)
        withExistingParent("cover_machine_display", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/machine_display"));
        withExistingParent("cover_auto_switch", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/auto_switch"));
        withExistingParent("cover_energy_display", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/energy_display"));
        withExistingParent("cover_scale_energy", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/scale_energy"));
        withExistingParent("cover_scale_progress", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/scale_progress"));
        for (int i = 0; i < gregtech6.covers.GT6Covers.AUTO_TIMER_IDS.length; i++) {
            withExistingParent(gregtech6.covers.GT6Covers.AUTO_TIMER_IDS[i], mcLoc("item/generated"))
                .texture("layer0", modLoc("item/covers/" + gregtech6.covers.GT6Covers.AUTO_TIMER_IDS[i].substring("cover_".length())));
        }
        // the p37 crafting + asphalt cover pair (task covers-crafting-asphalt; icon
        // source swap task cover-item-icons-dual-source) — the crafting icon is the
        // upstream item-domain borrow (meta 1001, MultiItemTechnological.java:60); the
        // in-world crafting plate art stays in textures/block/crafting/. DECLARED KEEP
        // for the asphalt: upstream the cover rides the Asphalt Panel MTE item
        // (Loader_MultiTileEntities.java:2053-2055), whose icon IS the asphalt art
        // itself — no technological-domain PNG exists, so the existing block/asphalt
        // borrow is already the upstream icon art (the crafting variant row keeps
        // variant 0, the declared fold)
        withExistingParent("cover_crafting", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/covers/crafting"));
        withExistingParent("cover_asphalt", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/asphalt"));
        // the Integrated Circuit item (task distillery-family ①, per-config icon ladder
        // task circuit-config-icons) — the base model keeps item/generated over the config-0
        // borrow (gt.integrated_circuit/0.png) as the no-payload face, and the upstream
        // per-config art is restored by 25 override variants: the ladder walks config 24
        // down to 0 because ItemOverrides.resolve takes the FIRST override whose predicate
        // value is <= the property value (ItemOverrides.java:83-99 both legs), so descending
        // order selects exactly the stack's configuration (ItemIntegratedCircuit.java:118
        // registers configs 0-24, getIconFromDamage :93-95 rides meta&255). The property is
        // the client-side gt6:config registration (GTClientHandlers.onClientSetup, the
        // configurationOf face); payload-less stacks read 0 and land on the base model.
        // Declared deviation: damage 25-255 (the mode-prefixed payloads) shows the config-24
        // art where upstream shows its never-registered null icon (the :118 TODO cut).
        // (the variant names carry the explicit item/ folder prefix — a slashed name
        //  skips the provider's folder extension, the r8-tex-sensors pitfall verbatim;
        //  without it the override target gt6:item/... misses the file)
        for (int i = 0; i < 25; i++) {
            withExistingParent("item/integrated_circuit/config_" + i, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/integrated_circuit/" + i));
        }
        ItemModelBuilder tCircuit = withExistingParent("integrated_circuit", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/integrated_circuit/0"));
        for (int i = 24; i >= 0; i--) {
            tCircuit = tCircuit.override()
                .predicate(new ResourceLocation("gt6", "config"), i)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/integrated_circuit/config_" + i)))
                .end();
        }
        // the Lubricant Bucket item (task w4-hot-lube ④) — item/generated over the
        // byte-identical vanilla bucket icon borrow (assets/README.md attribution; the
        // crafting-ingredient face needs a neutral bucket glyph, the filled/tinted upgrade
        // rides the fluid-container capability card)
        withExistingParent("lubricant_bucket", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/lubricant_bucket"));
        // the USB Stick family (task usb-data) — 4 item/generated models over the
        // byte-identical upstream icon borrows (gt.multiitem.technological metas 32001-32004,
        // assets/README.md attribution): one model per tier, walked over the tier loop so
        // the model ids cannot drift from the GT6UsbSticks registry rows
        for (int tTier = 1; tTier <= 4; tTier++) {
            withExistingParent("usb_stick_" + tTier, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/usb_stick_" + tTier));
        }
        // the USB peripheral families (task usb-peripherals) — 8 item/generated models
        // over the byte-identical upstream icon borrows (gt.multiitem.technological metas
        // 32011-32014 the cables / 32021-32024 the HDDs, assets/README.md attribution),
        // the same tier-loop convention as the sticks
        for (int tTier = 1; tTier <= 4; tTier++) {
            withExistingParent("usb_cable_" + tTier, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/usb_cable_" + tTier));
            withExistingParent("usb_drive_" + tTier, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/usb_drive_" + tTier));
        }
        // the key family (task dungeon-keys) — 10 item/generated models over the
        // byte-identical upstream icon borrows (gt.multiitem.randomtools metas 30000-30009,
        // assets/README.md attribution), walked over the GT6Keys pool so the model ids
        // cannot drift from the registry rows (the usb-stick band convention)
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tKey : gregtech6.items.GT6Keys.KEYS) {
            withExistingParent(tKey.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + tKey.getId().getPath()));
        }
		// the gas laser emitter family (task qu-laser-domain + the debt-laser-gas-family
		// closure + the usb-peripherals He exemption merged in by the review-seat
		// rebase) — item/generated models over the byte-identical upstream icon borrows
		// (gt.multiitem.technological metas 11000-11008, assets/README.md attribution)
		for (String tEmitter : new String[] {"empty", "he", "ne", "ar", "kr", "xe", "hene", "co", "co2"}) {
			withExistingParent("comp_laser_gas_" + tEmitter, mcLoc("item/generated"))
				.texture("layer0", modLoc("item/comp_laser_gas_" + tEmitter));
		}

        // the three technological component families (task debt-emitter-sensor-generators)
        // — 30 item/generated models over the byte-identical upstream icon borrows
        // (gt.multiitem.technological metas 12100-12109/12120-12129/12140-12149,
        // assets/README.md attribution), walked over the GT6Emitters pool so the model
        // ids cannot drift from the registry rows (the usb-stick band convention)
        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tItem : gregtech6.items.GT6Emitters.ITEMS_BY_PATH.values()) {
            withExistingParent(tItem.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + tItem.getId().getPath()));
        }
		// the 46 reactor rod items (task debt-reactor-c-rods, Loader_MultiTileEntities
		// .java:741-790) — flat item/generated two-layer stacks over the two reactor_rods
		// byte borrows shared by the whole family (assets/README.md attribution): upstream
		// has NO 2D rod icons (block MTEs, inventory form = the 3D block) and differentiates
		// the rods only by the material tint (RodBase.java:69-70) — the un-tinted shared
		// icon stays the declared deviation (the render-pool card landed without the rod
		// item face; the tint seat is unbuilt today). The walk is
		// over GT6ReactorRods.ROWS so the model ids cannot drift from the registry rows.
		for (gregtech6.items.GT6ReactorRods.RodRow tRod : gregtech6.items.GT6ReactorRods.ROWS) {
			withExistingParent(tRod.path(), mcLoc("item/generated"))
				.texture("layer0", modLoc("item/reactor_rod_colored_sides"))
				.texture("layer1", modLoc("item/reactor_rod_overlay_sides"));
		}
        // the spray-can family (task spraycan-items) — 18 item/generated models over the
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
        // the C-Foam spray family (task c-foam-pipe-spray spec ①) — 32 item/generated
        // models over the byte-identical upstream icon borrows (gt.multiitem.randomtools
        // metas 1100+2i / 1132+2i, assets/README.md attribution), walked over the DYE_IDS
        // snake table so the model ids cannot drift from the registered item ids
        // (registry "foam_spray_<id>" / "foam_spray_owned_<id>")
        for (String tDye : gregtech6.item.spraycan.GTSprayCanItem.DYE_IDS) {
            withExistingParent("foam_spray_" + tDye, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/spray/foam_" + tDye));
            withExistingParent("foam_spray_owned_" + tDye, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/spray/foam_owned_" + tDye));
        }
        // the six scene tools (task w5-t5-scene-six) — handheld parents over the
        // byte-identical upstream iconset borrows (assets/README.md attribution): layer0 =
        // the tool head icon, layer1 = the OVERLAY shadow borrow (the crowbar/cutter
        // two-layer shape; the upstream handle half is VOID — passes 2/3 draw nothing).
        withExistingParent("scissors", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/scissors"))
            .texture("layer1", modLoc("item/scissors_overlay"));
        withExistingParent("scoop", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/scoop"))
            .texture("layer1", modLoc("item/scoop_overlay"));
        withExistingParent("plunger", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/plunger"))
            .texture("layer1", modLoc("item/plunger_overlay"));
        withExistingParent("flint_and_tinder", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/flint_tinder"))
            .texture("layer1", modLoc("item/flint_tinder_overlay"));
        withExistingParent("rolling_pin", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/rolling_pin"))
            .texture("layer1", modLoc("item/rolling_pin_overlay"));
        withExistingParent("bending_cylinder", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/bending_cylinder"))
            .texture("layer1", modLoc("item/bending_cylinder_overlay"));
        // the Hazmat armor family (task w5-t8-armor-24) — 24 item/generated models
        // over the bake_armor_textures.py placeholder icons (6 suits x 4 slots, the SUITS
        // walk so the model ids cannot drift from the registered ids; the WORN layer
        // textures are NOT item models — they resolve through the getArmorTexture
        // override straight from assets/gt6/textures/models/armor/)
        for (gregtech6.items.armor.GT6ArmorMaterials.SuitRow tSuit : gregtech6.items.armor.GT6ArmorMaterials.SUITS) {
            for (int i = 0; i < gregtech6.items.armor.GT6ArmorMaterials.PIECE_WORDS.length; i++) {
                withExistingParent(tSuit.pieceId(i), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/armor/" + tSuit.textureName() + "/" + gregtech6.items.armor.GT6ArmorMaterials.PIECE_WORDS[i]));
            }
        }
        // the bee-comb family (task bees-lv1) — 20 item/generated models over the
        // port-generated tinted honeycomb icons (item/comb/comb_<name>.png, one base
        // silhouette per-comb tinted, assets/README.md), walked over the COMB_SPECS table
        // so the model ids cannot drift (the spray-can band convention)
        for (gregtech6.registry.GT6BeeCombs.CombSpec tSpec : gregtech6.registry.GT6BeeCombs.COMB_SPECS) {
            withExistingParent(tSpec.itemId(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/comb/" + tSpec.itemId()));
        }
        // the bumblebee family (task bees-lv3-a-items) — 8 item/generated models over
        // the port-generated bee icons (item/bumble/bumble_<face>.png, one sprite per
        // fractal face; the scanned forms share the base-face sprite), walked over the
        // FACES table so the model ids cannot drift (the comb band convention)
        for (gregtech6.items.bees.GT6Bumbles.FaceRow tFace : gregtech6.items.bees.GT6Bumbles.FACES) {
            withExistingParent(tFace.itemId(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/bumble/bumble_"
                        + tFace.name().substring(0, tFace.name().length() - (tFace.scanned() ? "_scanned" : "").length())));
        }
        // the placeables band (task placeables) — the BlockItems ride the block models
        // (the vanilla jack_o_lantern item form); the block-model providers run LATER in the
        // generator order, so the parents are the UNCHECKED references (the turbine form)
        withExistingParentUnchecked("greg_o_lantern", "block/greg_o_lantern");
        withExistingParentUnchecked("sandwich", "block/sandwich");
        // the Bumbliary pair (task bees-lv3-b-bumbliary) — the BlockItems ride the
        // block models (the same placeables form)
        withExistingParentUnchecked("bumbliary", "block/bumbliary");
        withExistingParentUnchecked("bumbliary_advanced", "block/bumbliary_adv");
        // the R2 hive BlockItem (task bumbliary-recipes) — the same BlockItem form
        withExistingParentUnchecked("bumble_hive", "block/bumble_hive");
        // the written-book family (task books-written) — 15 models sharing the vanilla
        // written_book item model as the parent (zero shipped assets: the upstream
        // ItemsGT.BOOKS carriers render as plain books, MultiItemBooks.java carries no
        // per-book icon face), walked over the generated rows so the model ids cannot
        // drift from the GT6Books registry paths
        for (GT6BookText.BookText tRow : GT6BookText.BOOKS) {
            withExistingParent(tRow.path(), mcLoc("item/written_book"));
        }
        // the Dusty Guide Book loot carrier (task book-loot-first) — the same plain-book
        // parent (upstream meta 32765 renders as a book too, MultiItemBooks.java:67)
        withExistingParent("book_loot_guide", mcLoc("item/written_book"));
        // the Clay Bowl raw item (task c1-dynamo-bowl-models) — item/generated over the
        // byte-identical upstream icon borrow (gt.multiitem.randomtools/995.png, meta 995 =
        // the "Clay Bowl" row, MultiItemRandomTools.java:119 — assets/README.md attribution;
        // the registered item had zero model rows, the hand-held magenta case)
        withExistingParent("clay_bowl", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/clay_bowl"));
        // the Clay Faucet raw item (task p38-c1 append, the C5-guard catch) — the clay_bowl
        // row shape; layer0 = the byte-identical gt.multiitem.randomtools/992.png borrow
        // (meta 992 = "Clay Faucet", MultiItemRandomTools.java:117 — assets/README.md
        // attribution; the mold model segment covered only the finished pairs).
        withExistingParent("faucet_ceramic_raw", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/faucet_ceramic_raw"));
        // the Clay Juicer raw item (issue #45 C1) — the clay_bowl row shape; layer0 = the
        // byte-identical gt.multiitem.randomtools/994.png borrow (meta 994 = "Clay Juicer",
        // MultiItemRandomTools.java:118 — assets/README.md attribution).
        withExistingParent("clay_juicer", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/clay_juicer"));
        // the Clay Crucible raw item (issue #45 C2; GT6Crucibles.CLAY_CRUCIBLE_RAW) — the
        // clay_bowl row shape; layer0 = the byte-identical gt.multiitem.randomtools/989.png
        // borrow (meta 989 = "Clay Crucible", MultiItemRandomTools.java:113 — assets/README.md
        // attribution).
        withExistingParent("clay_crucible_raw", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/clay_crucible_raw"));
        // the Clay Measuring Pot raw item (task issue45-c3, issue #45) — the clay_bowl
        // row shape; layer0 = the byte-identical gt.multiitem.randomtools/997.png borrow
        // (meta 997 = "Clay Measuring Pot", MultiItemRandomTools.java:121 — assets/README.md
        // attribution).
        withExistingParent("clay_measuring_pot", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/clay_measuring_pot"));
        // the Modeled Porcelain Cup raw item (task small-tank-cup) — the clay_measuring_pot
        // row shape; layer0 = the byte-identical gt.multiitem.randomtools/899.png borrow
        // (meta 899 = "Modeled Porcelain Cup", MultiItemRandomTools.java:76 — assets/README.md
        // attribution).
        withExistingParent("modeled_porcelain_cup", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/modeled_porcelain_cup"));
        // the Clay Jug raw item (task small-tank-jug) — the modeled_porcelain_cup row
        // shape; layer0 = the byte-identical gt.multiitem.randomtools/996.png borrow
        // (meta 996 = "Clay Jug", MultiItemRandomTools.java:120 — assets/README.md
        // attribution).
        withExistingParent("clay_jug", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/clay_jug"));
        // the ZPM item (task p38-c1 append, the C5-guard catch; GT6Batteries.ZPM_ITEM) —
        // item/generated over the byte-identical ZPM_SIDES.png iconset borrow. Declared
        // deviation: upstream 14999 is a BLOCK item (the isometric block render is the
        // item face, no standalone sprite exists) — the flat casing art is the nearest
        // upstream sprite (assets/README.md attribution).
        withExistingParent("zpm", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/zpm"));
        // the p29-w5-t4 un-laddered tool quartet (task p38-c1 append, the C5-guard catch) —
        // behaviour landed without model rows. Plow + Sense: the axe/hoe four-layer row —
        // head = the toolHead{Plow,Sense} metallic-set pair (the default primary Steel,
        // GT_Tool_Plow.getIcon :78-80 / GT_Tool_Sense :94-96), handle = the wood stick pair
        // (the secondary Spruce, the hammer-row borrow shape; zero new PNGs). Branch
        // Cutter: the wrench two-layer row — head = the GRAFTER iconset pair borrow
        // (GT_Tool_BranchCutter.getIcon :126-127, VOID handle). Hand Drill: the same
        // two-layer row — the HAND_DRILL iconset pair borrow (GT_Tool_HandDrill.getIcon
        // :63-64, VOID handle; the electric drill band's tip/handle art is a DIFFERENT
        // sprite). All un-tinted at the single steel tier (the family declared deviation).
        withExistingParent("plow", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_plow"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_plow_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("sense", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_sense"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_sense_overlay"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/material_sets/wood/stick_overlay"));
        withExistingParent("branch_cutter", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/branch_cutter"))
            .texture("layer1", modLoc("item/branch_cutter_overlay"));
        withExistingParent("hand_drill", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/hand_drill"))
            .texture("layer1", modLoc("item/hand_drill_overlay"));
    }

    /** The unchecked parent reference (the GT6BlockStates turbine form) — cross-provider block models generated later. */
    private void withExistingParentUnchecked(String aItemId, String aBlockModelPath) {
        getBuilder(aItemId).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(
                new net.minecraft.resources.ResourceLocation(GT6DataGenerators.MOD_ID, aBlockModelPath)));
    }

    /** The material's item texture-set name, lower-snaked; empty falls back to upstream SET_NONE. */
    public static String iconsetOf(OreDictMaterial material) {
        List<String> tSets = material.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : MaterialPrefixItem.snakeCase(tSets.get(0));
    }

    /**
     * The item sprite basename for a prefix: the snake name, except the sixteen wire
     * multipliers (task wire-gt-registration) which all ride the ONE {@code wire} sprite —
     * upstream has no per-multiplier item art at all (the items domain carries zero wireGt
     * icons; the wires are MTE blocks over the single grayscale block-domain wire.png,
     * MultiTileEntityWireElectric.java:72-109), so all sixteen multipliers share the
     * borrowed {@code wire}/{@code wire_overlay} pair per texture set (assets/README.md,
     * the 3 reachable sets copper/dull/rubber) and the material colour still differentiates
     * them at runtime via the tintIndex-0 ItemColor (the grayscale + mRGBa modulation
     * semantics, MaterialPrefixItem.tintColor).
     */
    static String spriteNameOf(OreDictPrefix prefix) {
        return prefix.mNameInternal.startsWith("wireGt") ? "wire" : MaterialPrefixItem.snakeCase(prefix.mNameInternal);
    }
}
