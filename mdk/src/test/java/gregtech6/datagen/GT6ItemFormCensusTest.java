/**
 * Offline pin for task tex-itemform-a — the item-model form census (the
 * {@link GT6RailItemModelDatagenTest} shape over the tex-r2 item_form column). The
 * single-texture cubeAll families whose BlockItems parented the block model rendered
 * the inventory form as one flat sprite tiled over six faces (the census "bad" form);
 * this card moves them to the 2D {@code item/generated} icon over the family's own
 * sprite (the r5 rail precedent). Reads the committed generated tree (no datagen run);
 * the block models are pinned UNCHANGED in the same breath, so the item fix can never
 * leak into the world face. The ok3D whitelist pins the families whose block models are
 * genuinely FACETED (the vanilla furnace form) so a future sweep cannot "fix" them, and
 * the GT6ItemModels.java fallback rows pin the census missing-hole verdict (the
 * placeables/bumbliary items were never missing — they ride their block models).
 * Task tex-itemform-b extends the census with the tier-B front-view families (the
 * 28 kinematics/controller/composite rows) on the same 2D form; task diesel-item-3d
 * retired the 8 diesel rows of that band into the block-model 3D form; task
 * r11-mains-tint-wrap retires the two remaining mains-controller rows of that band
 * (the lightning-rod controller + the large heat exchanger — the user-facing flat-white
 * inventory reports) AND the rod pillar part row into the same 3D form, the coke-oven
 * item precedent; task tap-funnel-model-audit retires the 12 tap/funnel attachment
 * rows the same way (the upstream item form is the 3D stack itself).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6LongDistWires;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6ItemFormCensusTest extends GTOfflineTestBase {

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6ItemFormCensusTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static void assert2DForm(String aId, String aLayer0) throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/item/" + aId + ".json");
        assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                aId + ": the 2D item/generated parent");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals(1, tTextures.size(), aId + ": exactly the layer0 texture");
        assertEquals(aLayer0, tTextures.get("layer0").getAsString(), aId + ": the family sprite");
    }

    /** The tier byte -> LONG_DIST_WIRES_01 sprite token (GT6BlockStates.wireArtOf mirror). */
    private static String wireArtOf(int aTier) {
        return switch (aTier) {
            case 4 -> "ev";
            case 5 -> "iv";
            case 6 -> "luv";
            case 7 -> "zpm";
            case 8 -> "uv";
            default -> throw new IllegalArgumentException("unknown LD wire tier byte: " + aTier);
        };
    }

    /**
     * The 12 battery boxes: FLIPPED BACK to the ok3D block-parent form by task
     * tex-composite-family (the block models are the true two-layer borrows now —
     * the 2D transitional icons this card shipped retired per their own declaration).
     */
    @Test
    void batteryBoxItemsParentTheTwoLayerBlockModels() throws Exception {
        assertFalse(GT6Batteries.BOX_ROWS.isEmpty(), "the row walk broke — never pass vacuously");
        for (GT6Batteries.BoxRow tRow : GT6Batteries.BOX_ROWS) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("gt6:block/" + (tRow.slots() == 16 ? "battery_box_large/battery_box_large" : "battery_box/battery_box"),
                    tModel.get("parent").getAsString(),
                    tRow.path() + ": the ok3D two-layer block-model parent");
        }
    }

    /** The 16 LD wires: 2D icons over the row's tier sprite. */
    @Test
    void ldWireItemsAre2DIcons() throws Exception {
        assertEquals(16, GT6LongDistWires.ROWS.size(), "the 16-meta walk");
        for (GT6LongDistWires.WireRow tRow : GT6LongDistWires.ROWS) {
            assert2DForm(GT6LongDistWires.pathOf(tRow.meta()),
                    "gt6:block/long_dist_wire_" + wireArtOf(tRow.tier()));
        }
    }

    /** The 16 LD pipe metas: 2D icons over the shared item_pipe placeholder sprite. */
    @Test
    void ldPipeItemsAre2DIcons() throws Exception {
        for (int tMeta = 0; tMeta < 16; tMeta++) {
            assert2DForm(gregtech6.registry.GT6LongDistPipes.pathOf(tMeta), "gt6:block/item_pipe");
        }
    }

    /** The placeholder energy family + example chest: 2D icons over their existing placeholder sprites. */
    @Test
    void placeholderFamilyItemsAre2DIcons() throws Exception {
        String[][] tRows = {
                {"example_chest", "gt6:block/example_chest"},
                {"energy_source", "gt6:block/energy_source"},
                {"fe_battery", "gt6:block/energy_source"},
                {"fe_converter", "gt6:block/energy_source"},
                {"fe_source", "gt6:block/energy_source"},
                {"test_machine", "gt6:block/example_chest"},
                {"test_machine_idle", "gt6:block/example_chest"},
                {"electric_dynamo_ulv", "gt6:block/energy_source"}};
        for (String[] tRow : tRows) {
            assert2DForm(tRow[0], tRow[1]);
        }
    }

    /**
     * The tier-B front-view families (tex-itemform-b, the 28 rows less the diesel
     * eight, the two mains controllers, the rod pillar and the 12 attachments): 2D
     * icons over the family's own front/side/composite sprite — the single
     * kinematics rows (crank, the water wheel, the gearbox) and the twin
     * distillation tower controllers over the borrowed parts side sprite. The 8 diesel
     * tiers LEFT this form in task diesel-item-3d — their block model is genuinely
     * faceted (front/back/side, the steam-family form), so the flat sprite was the
     * anti-pattern; they are pinned 3D below — the lightning-rod CONTROLLER and
     * the heat exchanger left in task r11-mains-tint-wrap (the same shape), and the
     * rod PILLAR left with them (the parts-walk item rides its block model now). The
     * 12 tap/funnel attachments left in task tap-funnel-model-audit (the same
     * retirement shape): upstream has no dedicated item PNG — the 1.7.10 item renders
     * the 3D stack — and the block models are the genuinely faceted overlay-pass
     * spouts now, pinned 3D in {@code GT6AttachmentStackDatagenTest
     * .attachmentItemModelsRideThe3DBlockIcons}.
     */
    @Test
    void frontViewFamiliesAre2DIcons() throws Exception {
        assert2DForm("crank", "gt6:block/crank");
        assert2DForm("water_wheel", "gt6:block/water_wheel");
        assert2DForm("gearbox", "gt6:block/gearbox");
        assertEquals(2, gregtech6.registry.GT6Distillation.ROWS.size(), "the twin-tower walk");
        for (gregtech6.registry.GT6Distillation.TowerRow tRow : gregtech6.registry.GT6Distillation.ROWS) {
            assert2DForm(tRow.path(), "gt6:block/parts/distillationtowerparts/0/colored/side");
        }
    }

    /**
     * The 8 diesel engine items ride the two-layer block model (task diesel-item-3d —
     * the steam-family 3D form, the {@code addSteamEngines} item row precedent): the
     * block model is the genuinely faceted front/back/side shell, so the BlockItems
     * parent it (the ok3D form) instead of the flat colored_front sprite. The
     * crank/gearbox/tap+funnel rows STAY 2D above — their block models are still
     * full-cube placeholders until the render-pool geometry card lands.
     */
    @Test
    void dieselItemsRideTheBlockModels() throws Exception {
        assertEquals(8, GT6Kinetics.DIESEL_SPECS.size(), "the 8-tier walk");
        for (GT6Kinetics.DieselSpec tSpec : GT6Kinetics.DIESEL_SPECS) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + GT6Kinetics.dieselName(tSpec.material()) + ".json");
            assertEquals("gt6:block/diesel_engine", tModel.get("parent").getAsString(),
                    GT6Kinetics.dieselName(tSpec.material()) + ": the two-layer block-model parent (the steam form)");
            assertFalse(tModel.has("textures"), GT6Kinetics.dieselName(tSpec.material())
                    + ": no local textures object (the flat-sprite pin is retired)");
        }
    }

    /**
     * The mains controller items ride the two-layer block models (task r11-mains-tint-wrap
     * — the diesel-item-3d retirement shape): the lightning-rod controller and the large
     * heat exchanger LEFT the tex-itemform-b flat composite icons — the inventory face is
     * the 3D block form upstream shows (the coke-oven item model :1459 precedent), and the
     * inventory tint rides the already-registered BlockItem ItemColors through the
     * tintindex-0 body seats ({@code GTMachineTintModel.onRegisterControllerPaintItemColors}).
     * The {@code lightning_rod} PART item stays 2D above — the declared tex-itemform-b
     * deviation, the part domain this card does not touch.
     */
    @Test
    void mainsControllerItemsRideTheBlockModels() throws Exception {
        String[][] tRows = {
                {"multiblock_lightning_rod", "gt6:block/multiblock_lightning_rod"},
                {"large_heat_exchanger", "gt6:block/large_heat_exchanger"}};
        for (String[] tRow : tRows) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow[0] + ".json");
            assertEquals(tRow[1], tModel.get("parent").getAsString(),
                    tRow[0] + ": the two-layer block-model parent (the coke-oven 3D form)");
            assertFalse(tModel.has("textures"), tRow[0]
                    + ": no local textures object (the flat-sprite pin is retired)");
        }
    }

    /**
     * The world-face guard: the touched block models stay the cube_all form over the
     * same sprites — one archetype per family (the item fix must not leak into the
     * blockstate/model chain), extended with the tier-B families (tex-itemform-b).
     * The battery boxes LEFT this guard in task tex-composite-family: their world
     * face is intentionally the two-layer borrow now, pinned by
     * GT6CompositeEnergyTexDatagenTest. The lightning-rod CONTROLLER and the heat
     * exchanger left it in task tex-multiblockmains (the same departure shape —
     * the flat bakes were placeholders, the groups' colored/colored_front splits are
     * real two-layer art), pinned by GT6MultiblockMainsTexDatagenTest; their ITEM
     * rows left the 2D composites in task r11-mains-tint-wrap (pinned 3D above), the
     * composite sprites stay on disk for the rod part and the historical ledger.
     */
    @Test
    void blockModelsKeepTheCubeAllForm() throws Exception {
        String[][] tArchetypes = {
                {"long_dist_wire_0", "gt6:block/long_dist_wire_ev"},
                {"long_dist_pipe_0", "gt6:block/item_pipe"},
                {"energy_source", "gt6:block/energy_source"},
                {"crank", "gt6:block/crank"},
                // ("diesel_engine" left in task tex-bridge-kinetic — the world face is the
                // two-layer borrow now, pinned by GT6BridgeKineticTexDatagenTest; the ITEM
                // row left the 2D sprite in task diesel-item-3d, pinned 3D below)
                // ("tap_ceramic"/"funnel_ceramic" left in task rod-render-pool — the world
                // face is the upstream three-pass element stack now; the ITEM rows left the
                // 2D family sprites in task tap-funnel-model-audit, pinned 3D in
                // GT6AttachmentStackDatagenTest.attachmentItemModelsRideThe3DBlockIcons)
                {"water_wheel", "gt6:block/water_wheel"},
                {"gearbox", "gt6:block/gearbox"},
                {"distillation_tower", "gt6:block/parts/distillationtowerparts/0/colored/side"}};
        for (String[] tCase : tArchetypes) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tCase[0] + ".json");
            assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString(),
                    tCase[0] + ": the cube_all parent (the world face is untouched)");
            assertEquals(tCase[1], tModel.getAsJsonObject("textures").get("all").getAsString(),
                    tCase[0] + ": the family sprite");
        }
    }

    /**
     * The ok3D whitelist — the census families whose BlockItems LEGITIMATELY parent a
     * faceted block model (the vanilla furnace form; the tex-census item_form ok3D
     * column): the basic-machine family, the bridge band, the converter/dynamo two-layer
     * band, the turbine mains, the parts design_0 walk, the large-machine controllers
     * and the Lightning Rod pillar (the parts-walk item since task r11-mains-tint-wrap).
     * These stay block-parented; only the single-texture cubeAll families went 2D.
     */
    @Test
    void ok3DWhitelistKeepsTheirBlockParents() throws Exception {
        String[][] tArchetypes = {
                {"shredder", "gt6:block/shredder"}, // the basic-machine familyMachineModel band
                {"electric_heater", "gt6:block/bridge_heater"}, // the bridge family
                {"electric_transformer", "gt6:block/electric_transformer"}, // the converter band
                {"electric_dynamo", "gt6:block/electric_dynamo"}, // the dynamo ladder
                {"steam_turbine_graphene", "gt6:block/turbine_main_steam"}, // the turbine mains
                {"machine_wall_tungsten", "gt6:block/machine_wall_tungsten_design_0"}, // the parts walk
                {"lightning_rod", "gt6:block/lightning_rod"}, // the rod pillar part (the parts walk, r11-mains-tint-wrap)
                {"large_massfab", "gt6:block/large_massfab"}, // the large-machine controllers
                {"battery_box_lv", "gt6:block/battery_box/battery_box"}}; // the composite-energy band (tex-composite-family flip-back)
        for (String[] tCase : tArchetypes) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tCase[0] + ".json");
            assertEquals(tCase[1], tModel.get("parent").getAsString(),
                    tCase[0] + ": the ok3D block-model parent (furnace form, not the anti-pattern)");
        }
    }

    /**
     * The census missing-hole verdict: the addBumbliary/addPlaceables blocks registered
     * NO itemModels row in GT6BlockStates — but the BlockItem models live in
     * GT6ItemModels.java (the placeables band, {@code withExistingParentUnchecked} over
     * their block models, the vanilla jack_o_lantern form). NOT known-missing; pinned so
     * the verdict cannot rot back into an open question.
     */
    @Test
    void placeablesAndBumbliaryRideTheirBlockModels() throws Exception {
        String[][] tRows = {
                {"greg_o_lantern", "gt6:block/greg_o_lantern"},
                {"sandwich", "gt6:block/sandwich"},
                {"bumbliary", "gt6:block/bumbliary"},
                {"bumbliary_advanced", "gt6:block/bumbliary_adv"}, // the advanced band drops the infix
                {"bumble_hive", "gt6:block/bumble_hive"}};
        for (String[] tRow : tRows) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow[0] + ".json");
            assertTrue(tModel.has("parent"), tRow[0] + ": the model exists (the fallback form)");
            assertEquals(tRow[1], tModel.get("parent").getAsString(),
                    tRow[0] + ": rides its block model (the GT6ItemModels placeables band)");
        }
    }

    /**
     * The gameplay cover items keep the flat plane (task cover-underlay-census, the ⑤-B
     * double-form check): upstream 1.7.10 cover ITEMS are flat icons too (the plate art
     * as one plane), so the item form is PRESERVED here — the fix this card ships lives
     * entirely on the INSTALLED face, where the underlay census + vent facet table make
     * the plate a base+art stack (with the vent's back/sides facets) instead of the item
     * icon's single sprite. Before the widening the installed plate rendered exactly the
     * item icon (the user-reported relief-valve defect); after it the two forms are
     * naturally distinct — this pin freezes the item half so the distinction cannot rot
     * back into a shared 3D form. Task cover-item-icons-dual-source then re-sourced the
     * item half: the icons are the upstream ITEM-domain art (MultiItemRandom
     * registerIcons), no longer the installed overlay sprite.
     */
    @Test
    void gameplayCoverItemsKeepTheFlatPlane() throws Exception {
        assert2DForm("cover_vent", "gt6:item/covers/vent");
        assert2DForm("cover_drain", "gt6:item/covers/drain");
        assert2DForm("cover_pressure_valve", "gt6:item/covers/pressure_valve");
        assert2DForm("cover_fluid_filter", "gt6:item/covers/fluid_filter");
        assert2DForm("cover_crafting", "gt6:item/covers/crafting");
        assert2DForm("cover_asphalt", "gt6:block/asphalt"); // the declared keep (the Asphalt Panel MTE icon IS the asphalt art)
        for (int i = 0; i < 16; i++) {
            assert2DForm("cover_selector_tag_" + i, "gt6:item/integrated_circuit/" + i);
        }
    }

    /**
     * The four machine-casing families (task casing-family-3d, 840 prefix items — the
     * user's known_bugs.r11-casing-item-model report「所有机械外壳都是 2D」): the upstream
     * art is the PrefixBlock_ full cube (Loader_PrefixBlocks.java:48-51 — the upstream
     * ITEM was therefore the 3D block form, the vanilla stone-item look), so the flat
     * {@code item/generated} sprite was the anti-pattern. They ride the two-layer tinted
     * cube directly (the {@code GT6BlockStates.partModel} body+decal shape over the
     * material_sets sprites, built in GT6ItemModels): the tintindex-0 body cube
     * ({@code MaterialPrefixItem.tintColor} serves the material colour on element faces
     * exactly as it did on the generated layer0) + the six un-tinted 0.01 overlay decals
     * (the shared ItemColor -1 arm), cutout for the transparent decal shells.
     */
    @Test
    void casingFamilyItemsRideThe3DCubeModels() throws Exception {
        // initMaterials FIRST — OP.init() re-creates the prefix objects on every call, so
        // the family array must read the statics AFTER init (identity == against the walk).
        gregtech6.registry.GTMaterialItems.initMaterials();
        gregapi.oredict.OreDictPrefix[] tFamilies = {
                gregapi.data.OP.casingMachine, gregapi.data.OP.casingMachineDouble,
                gregapi.data.OP.casingMachineQuadruple, gregapi.data.OP.casingMachineDense};
        int tWalked = 0;
        for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair
                : gregtech6.registry.GTMaterialItems.registrationOrder()) {
            boolean tCasing = false;
            for (gregapi.oredict.OreDictPrefix tPrefix : tFamilies) tCasing |= tPair.prefix() == tPrefix;
            if (!tCasing) continue;
            tWalked++;
            String tId = gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material());
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tId + ".json");
            assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                    tId + ": the 3D cube parent (the upstream PrefixBlock_ item form)");
            assertEquals(7, tModel.getAsJsonArray("elements").size(),
                    tId + ": the body cube + the six overlay decals (the partModel two-layer form)");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                    tId + ": cutout for the transparent decal shells");
            JsonObject tTextures = tModel.getAsJsonObject("textures");
            assertFalse(tTextures.has("layer0"), tId + ": the flat-sprite form is retired");
            assertTrue(tTextures.has("particle"), tId + ": the break-particle seat is set");
        }
        assertEquals(840, tWalked, "the quartet walk (210 per family, the casing-machine-register census)");
    }
}
