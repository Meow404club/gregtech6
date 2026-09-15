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
 * <p>Layer 2 (task p27-tool-model-layers): upstream pairs every materialicon with an
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
            String tPrefix = MaterialPrefixItem.snakeCase(tEntry.prefix().mNameInternal);
            // the OVERLAY second pass (task p27-tool-model-layers): upstream registers
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
        // the p5 pump cover item (task p5-barrel-side-rules ruling ⑥) — the item shows the
        // out-facing plate art; the direction sprites live in the block atlas via GT6Atlases
        withExistingParent("cover_pump", mcLoc("item/generated"))
            .texture("layer0", modLoc("block/cover_pump_out"));
        // ─── the tool family multi-layer wave (task p27-tool-model-layers) ───
        // Upstream renders every tool icon as FOUR passes (ToolStats.java:267-287):
        // pass0 = head base (tinted with the primary material), pass1 = head OVERLAY
        // (UNCOLOURED), pass2 = handle base (tinted with the secondary material),
        // pass3 = handle OVERLAY (UNCOLOURED). The vanilla item-model layer number IS
        // the tint index (ItemModelGenerator.java:15 LAYERS = layer0..layer4, the
        // processFrames layer argument becomes the quad tint index), so the four passes
        // map 1:1 onto layer0..layer3. The port stays un-tinted at the single steel tier
        // (the family declared deviation, GT6Tools) — the layer STRUCTURE is what this
        // wave restores; the tint ladder is a later card.

        // the formal crowbar item (task p9-tool-crowbar): handheld parent = the vanilla
        // tool shape; layer0 = the upstream CROWBAR.png iconset borrow + layer1 = the
        // CROWBAR_OVERLAY.png shadow borrow (assets/README.md attribution). Upstream's
        // handle is VOID (GT_Tool_Crowbar.getIcon :142-144), so passes 2/3 draw nothing
        // and the model is two-layer.
        withExistingParent("crowbar", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/crowbar"))
            .texture("layer1", modLoc("item/crowbar_overlay"));
        // the formal wire cutter item (task p10-tool-cutter): the crowbar row shape;
        // layer0 = the WIRE_CUTTER.png borrow + layer1 = the WIRE_CUTTER_OVERLAY.png
        // shadow borrow (assets/README.md attribution); VOID handle = two-layer.
        withExistingParent("cutter", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/cutter"))
            .texture("layer1", modLoc("item/cutter_overlay"));
        // the formal chisel item (task p16-chisel-decalcify): FOUR layers. Head = the
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
        // the formal file item (task p24-tool-system): FOUR layers, the chisel row
        // shape — head = the toolHeadFile materialicon pair (Steel = metallic set,
        // GT_Tool_File.getIcon :91-93), handle = the HANDLE_FILE iconset pair borrow
        // (the old single layer carried only the handle; the head layer is the restore).
        withExistingParent("file", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_file"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_file_overlay"))
            .texture("layer2", modLoc("item/file"))
            .texture("layer3", modLoc("item/file_overlay"));
        // the formal saw item (task p24-tool-system): FOUR layers, the chisel row shape
        // — head = the toolHeadSaw materialicon pair (Steel = metallic set,
        // GT_Tool_Saw.getIcon :186-188), handle = the HANDLE_SAW iconset pair borrow.
        withExistingParent("saw", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_saw"))
            .texture("layer1", modLoc("item/material_sets/metallic/tool_head_saw_overlay"))
            .texture("layer2", modLoc("item/saw"))
            .texture("layer3", modLoc("item/saw_overlay"));
        // the formal builder wand item (task p24-builder-wand): FOUR layers — head = the
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
        // the formal screwdriver item (task p24-screwdriver-item): handheld parent = the
        // vanilla tool shape, texture = the single composed flat icon — the upstream
        // four-layer render (head base/overlay + handle base/overlay,
        // ToolStats.getIcon pass order) flattened offline untinted (assets/README.md
        // attribution, the composition ruling). Deliberately NOT migrated to layers
        // (the census erratum keeps the composed singles — the offline composite is
        // structurally complete; only the un-tinted deviation remains).
        withExistingParent("screwdriver", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/screwdriver"));
        // the formal hard hammer item (task p25-tool-hammer-wrench): the screwdriver
        // composition ruling applies — the offline-composed flat icon stays single-layer
        // (upstream head OVER handle alpha-over, GT_Tool_HardHammer.getIcon :123 —
        // assets/README.md attribution).
        withExistingParent("hammer", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/hammer"));
        // the formal wrench item (task p25-tool-hammer-wrench): handheld parent = the
        // vanilla tool shape; layer0 = the byte-identical WRENCH.png iconset borrow +
        // layer1 = the WRENCH_OVERLAY.png pass borrow (transparent upstream, borrowed for
        // structure parity — assets/README.md attribution); VOID handle = two-layer.
        withExistingParent("wrench", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/wrench"))
            .texture("layer1", modLoc("item/wrench_overlay"));
        // the formal small bending cylinder item (task p25-food-can-row0): the wrench
        // row shape; layer0 = the BENDING_CYLINDER_SMALL.png borrow + layer1 = its
        // OVERLAY pass borrow (transparent upstream — assets/README.md attribution);
        // VOID handle = two-layer.
        withExistingParent("bending_cylinder_small", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/bending_cylinder_small"))
            .texture("layer1", modLoc("item/bending_cylinder_small_overlay"));
        // the six blade tools (task p29-w5-t2-blade-six) — the chisel/file four-layer row
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
        // the formal soft hammer item (task p29-w5-t3-machine-face-four): FOUR layers, the
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
        // the formal monkey wrench item (task p29-w5-t3-machine-face-four): the wrench row
        // shape — layer0 = the byte-identical MONKEYWRENCH.png iconset borrow + layer1 =
        // the MONKEYWRENCH_OVERLAY.png pass borrow (assets/README.md attribution).
        withExistingParent("monkey_wrench", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/monkey_wrench"))
            .texture("layer1", modLoc("item/monkey_wrench_overlay"));
        // the formal magnifying glass item (task p29-w5-t3-machine-face-four): the wrench
        // row shape — layer0 = the byte-identical MAGNIFYING_GLASS.png iconset borrow +
        // layer1 = the MAGNIFYING_GLASS_OVERLAY.png pass borrow (assets/README.md
        // attribution; the VOID handle = the two-layer form).
        withExistingParent("magnifying_glass", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/magnifying_glass"))
            .texture("layer1", modLoc("item/magnifying_glass_overlay"));
        // the formal pincers item (task p29-w5-t3-machine-face-four): the wrench row
        // shape — layer0 = the byte-identical PINCERS.png iconset borrow + layer1 = the
        // PINCERS_OVERLAY.png pass borrow (assets/README.md attribution).
        withExistingParent("pincers", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/pincers"))
            .texture("layer1", modLoc("item/pincers_overlay"));
        // the electric nineteen (task p29-w5-t6-electric-nineteen): the two-layer tool
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
        // the pocket multitool family (task p29-w5-t7-pocket-eight) — eight handheld
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
        // the food-can row0 subset (task p25-food-can-row0) — 8 item/generated models over
        // the byte-identical upstream icon borrows (gt.multiitem.randomtools/998 for the
        // empty can, gt.multiitem.cans/11-16 for the rotten family, :86 for the cookies
        // tin — assets/README.md attribution), walked over the registered ids so the model
        // ids cannot drift (the spray-can band convention)
        withExistingParent("food_can_empty", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/food_can/empty"));
        for (String tSize : new String[] {"tiny", "small", "tall", "wide", "large", "huge"}) {
            withExistingParent("food_can_rotten_" + tSize, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/food_can/rotten_" + tSize));
        }
        withExistingParent("food_can_cookies_huge", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/food_can/cookies_huge"));
        // the extruder-mold row0 subset (task p26-w1-press-extruder-molds) — 2 item/generated
        // models over the composed placeholder icons (the mold-plate/mold-rod 16x16 stdlib
        // generator, the P20 placeholder-PNG convention; the upstream multiitem icons are
        // meta-atlas tiles with no standalone sprite file to borrow)
        // the battery family (task p29-w4-battery-storage): 37 item/generated models over
        // the per-family upstream sprite borrows (the bake_battery_textures.py products,
        // assets/README.md attribution), walked over the ROWS table so model ids cannot
        // drift; the 5 cells share one cell sprite; the 7 circuit carriers reuse the
        // in-repo item/integrated_circuit.png (no new file, the declared placeholder);
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
                .texture("layer0", modLoc("item/integrated_circuit"));
        }
        // (the 12 box ITEMS parent their block models from GT6BlockStates.addBatteryBoxes —
        //  the item face validates against the blockstates provider's own output, the dynamo band convention)
        withExistingParent("shape_extruder_plate", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_extruder/plate"));
        withExistingParent("shape_extruder_rod", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/shape_extruder/rod"));
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
        // the Lubricant Bucket item (task p29-w4-hot-lube ④) — item/generated over the
        // byte-identical vanilla bucket icon borrow (assets/README.md attribution; the
        // crafting-ingredient face needs a neutral bucket glyph, the filled/tinted upgrade
        // rides the fluid-container capability card)
        withExistingParent("lubricant_bucket", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/lubricant_bucket"));
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
        // the C-Foam spray family (task p25-c-foam-pipe-spray spec ①) — 32 item/generated
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
        // the six scene tools (task p29-w5-t5-scene-six) — handheld parents over the
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
        // the Hazmat armor family (task p29-w5-t8-armor-24) — 24 item/generated models
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
    }

    /** The material's item texture-set name, lower-snaked; empty falls back to upstream SET_NONE. */
    public static String iconsetOf(OreDictMaterial material) {
        List<String> tSets = material.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : MaterialPrefixItem.snakeCase(tSets.get(0));
    }
}
