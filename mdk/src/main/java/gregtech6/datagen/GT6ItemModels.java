package gregtech6.datagen;

import java.util.List;
import java.util.Map;

import gregapi.oredict.OreDictMaterial;
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

    /**
     * task overlay-texture-dedupe — the overlay pass borrows are content-identical across
     * texture sets (upstream copies the same <CODE>&lt;Prefix&gt;_OVERLAY</CODE> art into every
     * SET folder of <CODE>materialicons/</CODE>, Textures.java:856-859 iconsets alike), and
     * 2146 of them are fully-transparent VOID passes shipped as byte-different but
     * pixel-empty files. Each distinct content group therefore ships ONCE under
     * {@code item/overlay/} under a semantic name and every (iconset, prefix) member
     * resolves onto the canonical. Row form: {@code <canonical> <set>/<prefix> ...}
     * (space-separated; parsed into {@link #OVERLAY_CANONICAL}). The 49 real-art
     * canonicals carry the exact bytes of one former member; the empty group carries the
     * former transparent bytes. Singleton overlays (unique content) keep their
     * material_sets paths — the {@code overlaySpriteOf} fallback below. The pin test
     * (GT6OverlayDedupeCensusTest) keeps the family at zero same-content duplicates.
     */
    private static final String[] OVERLAY_CONTENT_GROUPS = {
    "item/overlay/arrow_gt_plastic brick/arrow_gt_plastic copper/arrow_gt_plastic cube_shiny/arrow_gt_plastic diamond/arrow_gt_plastic dull/arrow_gt_plastic emerald/arrow_gt_plastic fine/arrow_gt_plastic flint/arrow_gt_plastic "
        + "gem_horizontal/arrow_gt_plastic gem_vertical/arrow_gt_plastic glass/arrow_gt_plastic hex/arrow_gt_plastic lapis/arrow_gt_plastic leaf/arrow_gt_plastic metallic/arrow_gt_plastic netherstar/arrow_gt_plastic "
        + "none/arrow_gt_plastic opal/arrow_gt_plastic powder/arrow_gt_plastic prismarine/arrow_gt_plastic quartz/arrow_gt_plastic rad/arrow_gt_plastic redstone/arrow_gt_plastic rubber/arrow_gt_plastic "
        + "ruby/arrow_gt_plastic shards/arrow_gt_plastic shiny/arrow_gt_plastic space/arrow_gt_plastic stone/arrow_gt_plastic wood/arrow_gt_plastic ",
    "item/overlay/arrow_gt_wood brick/arrow_gt_wood copper/arrow_gt_wood cube_shiny/arrow_gt_wood diamond/arrow_gt_wood dull/arrow_gt_wood emerald/arrow_gt_wood fine/arrow_gt_wood flint/arrow_gt_wood "
        + "gem_horizontal/arrow_gt_wood gem_vertical/arrow_gt_wood glass/arrow_gt_wood hex/arrow_gt_wood lapis/arrow_gt_wood leaf/arrow_gt_wood metallic/arrow_gt_wood netherstar/arrow_gt_wood "
        + "none/arrow_gt_wood opal/arrow_gt_wood powder/arrow_gt_wood prismarine/arrow_gt_wood quartz/arrow_gt_wood rad/arrow_gt_wood redstone/arrow_gt_wood rubber/arrow_gt_wood "
        + "ruby/arrow_gt_wood shards/arrow_gt_wood shiny/arrow_gt_wood space/arrow_gt_wood stone/arrow_gt_wood wood/arrow_gt_wood ",
    "item/overlay/billet magnetic/billet magnetic/ingot ",
    "item/overlay/bolt magnetic/bolt magnetic/casing_small magnetic/chain ",
    "item/overlay/boule_gt copper/boule_gt gem_vertical/boule_gt ",
    "item/overlay/bullet_gt_large brick/bullet_gt_large copper/bullet_gt_large cube_shiny/bullet_gt_large diamond/bullet_gt_large dull/bullet_gt_large emerald/bullet_gt_large fine/bullet_gt_large flint/bullet_gt_large "
        + "gem_horizontal/bullet_gt_large gem_vertical/bullet_gt_large glass/bullet_gt_large hex/bullet_gt_large lapis/bullet_gt_large leaf/bullet_gt_large lignite/bullet_gt_large metallic/bullet_gt_large "
        + "netherstar/bullet_gt_large none/bullet_gt_large opal/bullet_gt_large powder/bullet_gt_large prismarine/bullet_gt_large quartz/bullet_gt_large rad/bullet_gt_large redstone/bullet_gt_large "
        + "rough/bullet_gt_large rubber/bullet_gt_large ruby/bullet_gt_large shards/bullet_gt_large shiny/bullet_gt_large space/bullet_gt_large stone/bullet_gt_large wood/bullet_gt_large ",
    "item/overlay/bullet_gt_medium brick/bullet_gt_medium copper/bullet_gt_medium cube_shiny/bullet_gt_medium diamond/bullet_gt_medium dull/bullet_gt_medium emerald/bullet_gt_medium fine/bullet_gt_medium flint/bullet_gt_medium "
        + "gem_horizontal/bullet_gt_medium gem_vertical/bullet_gt_medium glass/bullet_gt_medium hex/bullet_gt_medium lapis/bullet_gt_medium leaf/bullet_gt_medium lignite/bullet_gt_medium metallic/bullet_gt_medium "
        + "netherstar/bullet_gt_medium none/bullet_gt_medium opal/bullet_gt_medium powder/bullet_gt_medium prismarine/bullet_gt_medium quartz/bullet_gt_medium rad/bullet_gt_medium redstone/bullet_gt_medium "
        + "rough/bullet_gt_medium rubber/bullet_gt_medium ruby/bullet_gt_medium shards/bullet_gt_medium shiny/bullet_gt_medium space/bullet_gt_medium stone/bullet_gt_medium wood/bullet_gt_medium ",
    "item/overlay/bullet_gt_small brick/bullet_gt_small copper/bullet_gt_small cube_shiny/bullet_gt_small diamond/bullet_gt_small dull/bullet_gt_small emerald/bullet_gt_small fine/bullet_gt_small flint/bullet_gt_small "
        + "gem_horizontal/bullet_gt_small gem_vertical/bullet_gt_small glass/bullet_gt_small hex/bullet_gt_small lapis/bullet_gt_small leaf/bullet_gt_small lignite/bullet_gt_small metallic/bullet_gt_small "
        + "netherstar/bullet_gt_small none/bullet_gt_small opal/bullet_gt_small powder/bullet_gt_small prismarine/bullet_gt_small quartz/bullet_gt_small rad/bullet_gt_small redstone/bullet_gt_small "
        + "rough/bullet_gt_small rubber/bullet_gt_small ruby/bullet_gt_small shards/bullet_gt_small shiny/bullet_gt_small space/bullet_gt_small stone/bullet_gt_small wood/bullet_gt_small ",
    "item/overlay/chemtube brick/chemtube copper/chemtube cube/chemtube cube_shiny/chemtube diamond/chemtube dull/chemtube emerald/chemtube fiery/chemtube "
        + "fine/chemtube flint/chemtube fluid/chemtube food/chemtube gem_horizontal/chemtube gem_vertical/chemtube glass/chemtube hex/chemtube "
        + "lapis/chemtube leaf/chemtube lignite/chemtube metallic/chemtube netherstar/chemtube none/chemtube opal/chemtube paper/chemtube "
        + "powder/chemtube prismarine/chemtube quartz/chemtube redstone/chemtube rough/chemtube rubber/chemtube ruby/chemtube sand/chemtube "
        + "shards/chemtube shiny/chemtube space/chemtube stone/chemtube wood/chemtube ",
    "item/overlay/chunk_gt magnetic/chunk_gt magnetic/tool_head_chisel magnetic/tool_head_file ",
    "item/overlay/crushed copper/crushed cube/crushed cube_shiny/crushed diamond/crushed dull/crushed emerald/crushed fine/crushed flint/crushed "
        + "gem_horizontal/crushed gem_vertical/crushed glass/crushed hex/crushed lapis/crushed lignite/crushed metallic/crushed netherstar/crushed "
        + "opal/crushed powder/crushed prismarine/crushed quartz/crushed rad/crushed redstone/crushed rough/crushed ruby/crushed "
        + "shards/crushed shiny/crushed space/crushed stone/crushed ",
    "item/overlay/crushed_tiny copper/crushed_tiny cube/crushed_tiny cube_shiny/crushed_tiny diamond/crushed_tiny dull/crushed_tiny emerald/crushed_tiny fine/crushed_tiny flint/crushed_tiny "
        + "gem_horizontal/crushed_tiny gem_vertical/crushed_tiny glass/crushed_tiny hex/crushed_tiny lapis/crushed_tiny lignite/crushed_tiny metallic/crushed_tiny netherstar/crushed_tiny "
        + "opal/crushed_tiny powder/crushed_tiny prismarine/crushed_tiny quartz/crushed_tiny rad/crushed_tiny redstone/crushed_tiny rough/crushed_tiny ruby/crushed_tiny "
        + "sand/crushed_tiny shards/crushed_tiny shiny/crushed_tiny space/crushed_tiny stone/crushed_tiny ",
    "item/overlay/dust cube_shiny/dust diamond/dust emerald/dust gem_horizontal/dust gem_vertical/dust glass/dust hex/dust netherstar/dust "
        + "opal/dust prismarine/dust rubber/dust ruby/dust shards/dust shiny/dust ",
    "item/overlay/dust_small cube_shiny/dust_small diamond/dust_small emerald/dust_small gem_horizontal/dust_small gem_vertical/dust_small glass/dust_small hex/dust_small netherstar/dust_small "
        + "opal/dust_small prismarine/dust_small rubber/dust_small ruby/dust_small shards/dust_small shiny/dust_small ",
    "item/overlay/dust_tiny cube_shiny/dust_tiny diamond/dust_tiny emerald/dust_tiny gem_horizontal/dust_tiny gem_vertical/dust_tiny glass/dust_tiny hex/dust_tiny netherstar/dust_tiny "
        + "opal/dust_tiny prismarine/dust_tiny rubber/dust_tiny ruby/dust_tiny shards/dust_tiny shiny/dust_tiny ",
    "item/overlay/empty brick/billet brick/bolt brick/casing_small brick/chain brick/chunk_gt brick/dust brick/dust_div72 brick/dust_small "
        + "brick/dust_tiny brick/foil brick/gear_gt brick/gear_gt_small brick/ingot brick/ingot_double brick/ingot_quadruple brick/ingot_quintuple "
        + "brick/ingot_triple brick/nugget brick/plant_gt_fiber brick/plant_gt_wart brick/plate brick/plate_curved brick/plate_dense brick/plate_double "
        + "brick/plate_quadruple brick/plate_quintuple brick/plate_tiny brick/plate_triple brick/rock_gt brick/rotor brick/round brick/scrap_gt "
        + "brick/screw brick/spring brick/spring_small brick/stick brick/stick_long brick/tool_head_arrow brick/tool_head_axe brick/tool_head_axe_double "
        + "brick/tool_head_builderwand brick/tool_head_buzz_saw brick/tool_head_chainsaw brick/tool_head_chisel brick/tool_head_construction_pickaxe brick/tool_head_drill brick/tool_head_file brick/tool_head_hammer "
        + "brick/tool_head_hoe brick/tool_head_pickaxe brick/tool_head_plow brick/tool_head_raw_arrow brick/tool_head_raw_axe brick/tool_head_raw_axe_double brick/tool_head_raw_chisel brick/tool_head_raw_hoe "
        + "brick/tool_head_raw_pickaxe brick/tool_head_raw_plow brick/tool_head_raw_saw brick/tool_head_raw_sense brick/tool_head_raw_shovel brick/tool_head_raw_spade brick/tool_head_raw_sword brick/tool_head_raw_universal_spade "
        + "brick/tool_head_saw brick/tool_head_screwdriver brick/tool_head_sense brick/tool_head_shovel brick/tool_head_spade brick/tool_head_sword brick/tool_head_universal_spade brick/tool_head_wrench "
        + "copper/billet copper/bolt copper/casing_small copper/chain copper/chunk_gt copper/crushed_centrifuged copper/crushed_centrifuged_tiny copper/crushed_purified "
        + "copper/crushed_purified_tiny copper/dust copper/dust_div72 copper/dust_small copper/dust_tiny copper/foil copper/gear_gt copper/gear_gt_small "
        + "copper/ingot copper/ingot_double copper/ingot_quadruple copper/ingot_quintuple copper/ingot_triple copper/nugget copper/ore_raw copper/plant_gt_fiber "
        + "copper/plant_gt_wart copper/plate copper/plate_curved copper/plate_dense copper/plate_double copper/plate_quadruple copper/plate_quintuple copper/plate_tiny "
        + "copper/plate_triple copper/rail_gt copper/rotor copper/round copper/scrap_gt copper/screw copper/spring copper/spring_small "
        + "copper/stick copper/stick_long copper/tool_head_arrow copper/tool_head_axe copper/tool_head_axe_double copper/tool_head_builderwand copper/tool_head_buzz_saw copper/tool_head_chainsaw "
        + "copper/tool_head_chisel copper/tool_head_construction_pickaxe copper/tool_head_drill copper/tool_head_file copper/tool_head_hammer copper/tool_head_hoe copper/tool_head_pickaxe copper/tool_head_plow "
        + "copper/tool_head_raw_arrow copper/tool_head_raw_axe copper/tool_head_raw_axe_double copper/tool_head_raw_chisel copper/tool_head_raw_hoe copper/tool_head_raw_pickaxe copper/tool_head_raw_plow copper/tool_head_raw_saw "
        + "copper/tool_head_raw_sense copper/tool_head_raw_shovel copper/tool_head_raw_spade copper/tool_head_raw_sword copper/tool_head_raw_universal_spade copper/tool_head_saw copper/tool_head_screwdriver copper/tool_head_sense "
        + "copper/tool_head_shovel copper/tool_head_spade copper/tool_head_sword copper/tool_head_universal_spade copper/tool_head_wrench cube/billet cube/chunk_gt cube/crushed_centrifuged "
        + "cube/crushed_centrifuged_tiny cube/crushed_purified cube/crushed_purified_tiny cube/dust cube/dust_div72 cube/dust_small cube/dust_tiny cube/ingot "
        + "cube/nugget cube/plant_gt_fiber cube/plant_gt_wart cube/scrap_gt cube_shiny/bolt cube_shiny/casing_small cube_shiny/chain cube_shiny/crushed_centrifuged "
        + "cube_shiny/crushed_centrifuged_tiny cube_shiny/crushed_purified cube_shiny/crushed_purified_tiny cube_shiny/dust_div72 cube_shiny/gem cube_shiny/gem_chipped cube_shiny/gem_flawed cube_shiny/plant_gt_fiber "
        + "cube_shiny/plant_gt_wart cube_shiny/rotor cube_shiny/round cube_shiny/scrap_gt cube_shiny/screw cube_shiny/stick_long cube_shiny/tool_head_axe cube_shiny/tool_head_hoe "
        + "cube_shiny/tool_head_pickaxe cube_shiny/tool_head_plow cube_shiny/tool_head_raw_axe cube_shiny/tool_head_raw_hoe cube_shiny/tool_head_raw_pickaxe cube_shiny/tool_head_raw_plow cube_shiny/tool_head_raw_sense cube_shiny/tool_head_raw_shovel "
        + "cube_shiny/tool_head_raw_spade cube_shiny/tool_head_raw_sword cube_shiny/tool_head_sense cube_shiny/tool_head_shovel cube_shiny/tool_head_spade cube_shiny/tool_head_sword diamond/billet diamond/bolt "
        + "diamond/casing_small diamond/chain diamond/chunk_gt diamond/crushed_centrifuged diamond/crushed_centrifuged_tiny diamond/crushed_purified diamond/crushed_purified_tiny diamond/dust_div72 "
        + "diamond/foil diamond/gem diamond/gem_chipped diamond/gem_exquisite diamond/gem_flawed diamond/gem_flawless diamond/gem_legendary diamond/ingot_double "
        + "diamond/ingot_quadruple diamond/ingot_quintuple diamond/ingot_triple diamond/plant_gt_fiber diamond/plant_gt_wart diamond/plate_curved diamond/plate_dense diamond/plate_double "
        + "diamond/plate_quadruple diamond/plate_quintuple diamond/plate_tiny diamond/plate_triple diamond/rotor diamond/round diamond/scrap_gt diamond/screw "
        + "diamond/spring diamond/spring_small diamond/stick_long diamond/tool_head_axe diamond/tool_head_axe_double diamond/tool_head_builderwand diamond/tool_head_buzz_saw diamond/tool_head_chainsaw "
        + "diamond/tool_head_chisel diamond/tool_head_construction_pickaxe diamond/tool_head_drill diamond/tool_head_file diamond/tool_head_hammer diamond/tool_head_hoe diamond/tool_head_pickaxe diamond/tool_head_plow "
        + "diamond/tool_head_raw_axe diamond/tool_head_raw_axe_double diamond/tool_head_raw_chisel diamond/tool_head_raw_hoe diamond/tool_head_raw_pickaxe diamond/tool_head_raw_plow diamond/tool_head_raw_saw diamond/tool_head_raw_sense "
        + "diamond/tool_head_raw_shovel diamond/tool_head_raw_spade diamond/tool_head_raw_sword diamond/tool_head_raw_universal_spade diamond/tool_head_saw diamond/tool_head_screwdriver diamond/tool_head_sense diamond/tool_head_shovel "
        + "diamond/tool_head_spade diamond/tool_head_sword diamond/tool_head_universal_spade diamond/tool_head_wrench dull/billet dull/bolt dull/casing_small dull/chain "
        + "dull/chunk_gt dull/crushed_centrifuged dull/crushed_centrifuged_tiny dull/crushed_purified dull/crushed_purified_tiny dull/dust dull/dust_div72 dull/dust_small "
        + "dull/dust_tiny dull/foil dull/gear_gt dull/gear_gt_small dull/gem dull/ingot dull/ingot_double dull/ingot_quadruple "
        + "dull/ingot_quintuple dull/ingot_triple dull/nugget dull/ore_raw dull/plant_gt_fiber dull/plant_gt_wart dull/plate dull/plate_curved "
        + "dull/plate_dense dull/plate_double dull/plate_gem dull/plate_gem_tiny dull/plate_quadruple dull/plate_quintuple dull/plate_tiny dull/plate_triple "
        + "dull/rail_gt dull/rotor dull/round dull/scrap_gt dull/screw dull/spring dull/spring_small dull/stick "
        + "dull/stick_long dull/tool_head_arrow dull/tool_head_axe dull/tool_head_axe_double dull/tool_head_builderwand dull/tool_head_buzz_saw dull/tool_head_chainsaw dull/tool_head_chisel "
        + "dull/tool_head_construction_pickaxe dull/tool_head_drill dull/tool_head_file dull/tool_head_hammer dull/tool_head_hoe dull/tool_head_pickaxe dull/tool_head_plow dull/tool_head_raw_arrow "
        + "dull/tool_head_raw_axe dull/tool_head_raw_axe_double dull/tool_head_raw_chisel dull/tool_head_raw_hoe dull/tool_head_raw_pickaxe dull/tool_head_raw_plow dull/tool_head_raw_saw dull/tool_head_raw_sense "
        + "dull/tool_head_raw_shovel dull/tool_head_raw_spade dull/tool_head_raw_sword dull/tool_head_raw_universal_spade dull/tool_head_saw dull/tool_head_screwdriver dull/tool_head_sense dull/tool_head_shovel "
        + "dull/tool_head_spade dull/tool_head_sword dull/tool_head_universal_spade dull/tool_head_wrench emerald/bolt emerald/casing_small emerald/chain emerald/crushed_centrifuged "
        + "emerald/crushed_centrifuged_tiny emerald/crushed_purified emerald/crushed_purified_tiny emerald/dust_div72 emerald/gem emerald/gem_chipped emerald/gem_exquisite emerald/gem_flawed "
        + "emerald/gem_flawless emerald/gem_legendary emerald/plant_gt_fiber emerald/plant_gt_wart emerald/rotor emerald/round emerald/scrap_gt emerald/screw "
        + "emerald/spring emerald/spring_small emerald/stick_long emerald/tool_head_axe emerald/tool_head_axe_double emerald/tool_head_builderwand emerald/tool_head_buzz_saw emerald/tool_head_chainsaw "
        + "emerald/tool_head_chisel emerald/tool_head_construction_pickaxe emerald/tool_head_drill emerald/tool_head_file emerald/tool_head_hammer emerald/tool_head_hoe emerald/tool_head_pickaxe emerald/tool_head_plow "
        + "emerald/tool_head_raw_axe emerald/tool_head_raw_axe_double emerald/tool_head_raw_chisel emerald/tool_head_raw_hoe emerald/tool_head_raw_pickaxe emerald/tool_head_raw_plow emerald/tool_head_raw_saw emerald/tool_head_raw_sense "
        + "emerald/tool_head_raw_shovel emerald/tool_head_raw_spade emerald/tool_head_raw_sword emerald/tool_head_raw_universal_spade emerald/tool_head_saw emerald/tool_head_screwdriver emerald/tool_head_sense emerald/tool_head_shovel "
        + "emerald/tool_head_spade emerald/tool_head_sword emerald/tool_head_universal_spade emerald/tool_head_wrench fiery/billet fiery/chain fiery/dust_div72 fiery/plant_gt_fiber "
        + "fiery/plant_gt_wart fiery/plate_tiny fiery/scrap_gt fiery/tool_head_builderwand fiery/tool_head_raw_spade fiery/tool_head_spade fine/billet fine/bolt "
        + "fine/casing_small fine/chain fine/chunk_gt fine/crushed_centrifuged fine/crushed_centrifuged_tiny fine/crushed_purified fine/crushed_purified_tiny fine/dust "
        + "fine/dust_div72 fine/dust_small fine/dust_tiny fine/foil fine/gear_gt fine/gear_gt_small fine/gem fine/gem_chipped "
        + "fine/gem_exquisite fine/gem_flawed fine/gem_flawless fine/gem_legendary fine/ingot fine/nugget fine/ore_raw fine/plant_gt_fiber "
        + "fine/plant_gt_wart fine/plate fine/plate_curved fine/plate_gem fine/plate_gem_tiny fine/plate_tiny fine/rotor fine/round "
        + "fine/scrap_gt fine/screw fine/spring fine/spring_small fine/stick fine/stick_long fine/tool_head_arrow fine/tool_head_axe "
        + "fine/tool_head_axe_double fine/tool_head_builderwand fine/tool_head_buzz_saw fine/tool_head_chainsaw fine/tool_head_chisel fine/tool_head_construction_pickaxe fine/tool_head_drill fine/tool_head_file "
        + "fine/tool_head_hammer fine/tool_head_hoe fine/tool_head_pickaxe fine/tool_head_plow fine/tool_head_raw_arrow fine/tool_head_raw_axe fine/tool_head_raw_axe_double fine/tool_head_raw_chisel "
        + "fine/tool_head_raw_hoe fine/tool_head_raw_pickaxe fine/tool_head_raw_plow fine/tool_head_raw_saw fine/tool_head_raw_sense fine/tool_head_raw_shovel fine/tool_head_raw_spade fine/tool_head_raw_sword "
        + "fine/tool_head_raw_universal_spade fine/tool_head_saw fine/tool_head_screwdriver fine/tool_head_sense fine/tool_head_shovel fine/tool_head_spade fine/tool_head_sword fine/tool_head_universal_spade "
        + "fine/tool_head_wrench flint/bolt flint/casing_small flint/chain flint/crushed_centrifuged flint/crushed_centrifuged_tiny flint/crushed_purified flint/crushed_purified_tiny "
        + "flint/dust flint/dust_div72 flint/dust_small flint/dust_tiny flint/gear_gt flint/gear_gt_small flint/gem flint/gem_chipped "
        + "flint/gem_exquisite flint/gem_flawed flint/gem_flawless flint/gem_legendary flint/ore_raw flint/plant_gt_fiber flint/plant_gt_wart flint/plate_gem "
        + "flint/plate_gem_tiny flint/rotor flint/round flint/scrap_gt flint/screw flint/spring flint/spring_small flint/stick "
        + "flint/stick_long flint/tool_head_arrow flint/tool_head_axe flint/tool_head_axe_double flint/tool_head_builderwand flint/tool_head_buzz_saw flint/tool_head_chainsaw flint/tool_head_chisel "
        + "flint/tool_head_construction_pickaxe flint/tool_head_drill flint/tool_head_file flint/tool_head_hammer flint/tool_head_hoe flint/tool_head_pickaxe flint/tool_head_plow flint/tool_head_raw_arrow "
        + "flint/tool_head_raw_axe flint/tool_head_raw_axe_double flint/tool_head_raw_chisel flint/tool_head_raw_hoe flint/tool_head_raw_pickaxe flint/tool_head_raw_plow flint/tool_head_raw_saw flint/tool_head_raw_sense "
        + "flint/tool_head_raw_shovel flint/tool_head_raw_spade flint/tool_head_raw_sword flint/tool_head_raw_universal_spade flint/tool_head_saw flint/tool_head_screwdriver flint/tool_head_sense flint/tool_head_shovel "
        + "flint/tool_head_spade flint/tool_head_sword flint/tool_head_universal_spade flint/tool_head_wrench fluid/plant_gt_fiber fluid/plant_gt_wart food/billet food/casing_small "
        + "food/chain food/chunk_gt food/dust food/dust_div72 food/dust_small food/dust_tiny food/foil food/gear_gt "
        + "food/gear_gt_small food/ingot food/nugget food/plant_gt_fiber food/plant_gt_wart food/plate food/plate_curved food/plate_tiny "
        + "food/rotor food/round food/scrap_gt gas/plant_gt_fiber gas/plant_gt_wart gem_horizontal/bolt gem_horizontal/casing_small gem_horizontal/chain "
        + "gem_horizontal/crushed_centrifuged gem_horizontal/crushed_centrifuged_tiny gem_horizontal/crushed_purified gem_horizontal/crushed_purified_tiny gem_horizontal/dust_div72 gem_horizontal/gem gem_horizontal/gem_chipped gem_horizontal/gem_exquisite "
        + "gem_horizontal/gem_flawed gem_horizontal/gem_flawless gem_horizontal/gem_legendary gem_horizontal/plant_gt_fiber gem_horizontal/plant_gt_wart gem_horizontal/rotor gem_horizontal/round gem_horizontal/scrap_gt "
        + "gem_horizontal/screw gem_horizontal/spring gem_horizontal/spring_small gem_horizontal/stick_long gem_horizontal/tool_head_axe gem_horizontal/tool_head_axe_double gem_horizontal/tool_head_builderwand gem_horizontal/tool_head_buzz_saw "
        + "gem_horizontal/tool_head_chainsaw gem_horizontal/tool_head_chisel gem_horizontal/tool_head_construction_pickaxe gem_horizontal/tool_head_drill gem_horizontal/tool_head_file gem_horizontal/tool_head_hammer gem_horizontal/tool_head_hoe gem_horizontal/tool_head_pickaxe "
        + "gem_horizontal/tool_head_plow gem_horizontal/tool_head_raw_axe gem_horizontal/tool_head_raw_axe_double gem_horizontal/tool_head_raw_chisel gem_horizontal/tool_head_raw_hoe gem_horizontal/tool_head_raw_pickaxe gem_horizontal/tool_head_raw_plow gem_horizontal/tool_head_raw_saw "
        + "gem_horizontal/tool_head_raw_sense gem_horizontal/tool_head_raw_shovel gem_horizontal/tool_head_raw_spade gem_horizontal/tool_head_raw_sword gem_horizontal/tool_head_raw_universal_spade gem_horizontal/tool_head_saw gem_horizontal/tool_head_screwdriver gem_horizontal/tool_head_sense "
        + "gem_horizontal/tool_head_shovel gem_horizontal/tool_head_spade gem_horizontal/tool_head_sword gem_horizontal/tool_head_universal_spade gem_horizontal/tool_head_wrench gem_vertical/bolt gem_vertical/casing_small gem_vertical/chain "
        + "gem_vertical/crushed_centrifuged gem_vertical/crushed_centrifuged_tiny gem_vertical/crushed_purified gem_vertical/crushed_purified_tiny gem_vertical/dust_div72 gem_vertical/gem gem_vertical/gem_chipped gem_vertical/gem_exquisite "
        + "gem_vertical/gem_flawed gem_vertical/gem_flawless gem_vertical/gem_legendary gem_vertical/plant_gt_fiber gem_vertical/plant_gt_wart gem_vertical/rotor gem_vertical/round gem_vertical/scrap_gt "
        + "gem_vertical/screw gem_vertical/spring gem_vertical/spring_small gem_vertical/stick_long gem_vertical/tool_head_axe gem_vertical/tool_head_axe_double gem_vertical/tool_head_builderwand gem_vertical/tool_head_buzz_saw "
        + "gem_vertical/tool_head_chainsaw gem_vertical/tool_head_chisel gem_vertical/tool_head_construction_pickaxe gem_vertical/tool_head_drill gem_vertical/tool_head_file gem_vertical/tool_head_hammer gem_vertical/tool_head_hoe gem_vertical/tool_head_pickaxe "
        + "gem_vertical/tool_head_plow gem_vertical/tool_head_raw_axe gem_vertical/tool_head_raw_axe_double gem_vertical/tool_head_raw_chisel gem_vertical/tool_head_raw_hoe gem_vertical/tool_head_raw_pickaxe gem_vertical/tool_head_raw_plow gem_vertical/tool_head_raw_saw "
        + "gem_vertical/tool_head_raw_sense gem_vertical/tool_head_raw_shovel gem_vertical/tool_head_raw_spade gem_vertical/tool_head_raw_sword gem_vertical/tool_head_raw_universal_spade gem_vertical/tool_head_saw gem_vertical/tool_head_screwdriver gem_vertical/tool_head_sense "
        + "gem_vertical/tool_head_shovel gem_vertical/tool_head_spade gem_vertical/tool_head_sword gem_vertical/tool_head_universal_spade gem_vertical/tool_head_wrench glass/bolt glass/casing_small glass/chain "
        + "glass/crushed_centrifuged glass/crushed_centrifuged_tiny glass/crushed_purified glass/crushed_purified_tiny glass/dust_div72 glass/gem_chipped glass/gem_exquisite glass/gem_flawed "
        + "glass/gem_flawless glass/gem_legendary glass/ore_raw glass/plant_gt_fiber glass/plant_gt_wart glass/rotor glass/round glass/scrap_gt "
        + "glass/screw glass/spring glass/spring_small glass/stick_long glass/tool_head_axe glass/tool_head_axe_double glass/tool_head_builderwand glass/tool_head_buzz_saw "
        + "glass/tool_head_chainsaw glass/tool_head_chisel glass/tool_head_construction_pickaxe glass/tool_head_drill glass/tool_head_file glass/tool_head_hammer glass/tool_head_hoe glass/tool_head_pickaxe "
        + "glass/tool_head_plow glass/tool_head_raw_axe glass/tool_head_raw_axe_double glass/tool_head_raw_chisel glass/tool_head_raw_hoe glass/tool_head_raw_pickaxe glass/tool_head_raw_plow glass/tool_head_raw_saw "
        + "glass/tool_head_raw_sense glass/tool_head_raw_shovel glass/tool_head_raw_spade glass/tool_head_raw_sword glass/tool_head_raw_universal_spade glass/tool_head_saw glass/tool_head_screwdriver glass/tool_head_sense "
        + "glass/tool_head_shovel glass/tool_head_spade glass/tool_head_sword glass/tool_head_universal_spade glass/tool_head_wrench hex/bolt hex/casing_small hex/chain "
        + "hex/crushed_centrifuged hex/crushed_centrifuged_tiny hex/crushed_purified hex/crushed_purified_tiny hex/dust_div72 hex/gem hex/gem_chipped hex/gem_exquisite "
        + "hex/gem_flawed hex/gem_flawless hex/gem_legendary hex/plant_gt_fiber hex/plant_gt_wart hex/rotor hex/round hex/scrap_gt "
        + "hex/screw hex/stick_long hex/tool_head_axe hex/tool_head_axe_double hex/tool_head_builderwand hex/tool_head_buzz_saw hex/tool_head_chainsaw hex/tool_head_chisel "
        + "hex/tool_head_construction_pickaxe hex/tool_head_drill hex/tool_head_file hex/tool_head_hammer hex/tool_head_hoe hex/tool_head_pickaxe hex/tool_head_plow hex/tool_head_raw_axe "
        + "hex/tool_head_raw_axe_double hex/tool_head_raw_chisel hex/tool_head_raw_hoe hex/tool_head_raw_pickaxe hex/tool_head_raw_plow hex/tool_head_raw_saw hex/tool_head_raw_sense hex/tool_head_raw_shovel "
        + "hex/tool_head_raw_spade hex/tool_head_raw_sword hex/tool_head_raw_universal_spade hex/tool_head_saw hex/tool_head_screwdriver hex/tool_head_sense hex/tool_head_shovel hex/tool_head_spade "
        + "hex/tool_head_sword hex/tool_head_universal_spade hex/tool_head_wrench lapis/billet lapis/bolt lapis/casing_small lapis/chain lapis/chunk_gt "
        + "lapis/crushed_centrifuged lapis/crushed_centrifuged_tiny lapis/crushed_purified lapis/crushed_purified_tiny lapis/dust lapis/dust_div72 lapis/dust_small lapis/dust_tiny "
        + "lapis/gear_gt lapis/gear_gt_small lapis/gem lapis/gem_chipped lapis/gem_exquisite lapis/gem_flawed lapis/gem_flawless lapis/gem_legendary "
        + "lapis/ingot lapis/nugget lapis/plant_gt_fiber lapis/plant_gt_wart lapis/plate lapis/plate_curved lapis/plate_dense lapis/plate_gem "
        + "lapis/plate_gem_tiny lapis/plate_tiny lapis/rotor lapis/round lapis/scrap_gt lapis/screw lapis/spring lapis/spring_small "
        + "lapis/stick lapis/stick_long lapis/tool_head_arrow lapis/tool_head_axe lapis/tool_head_axe_double lapis/tool_head_builderwand lapis/tool_head_buzz_saw lapis/tool_head_chainsaw "
        + "lapis/tool_head_chisel lapis/tool_head_construction_pickaxe lapis/tool_head_drill lapis/tool_head_file lapis/tool_head_hammer lapis/tool_head_hoe lapis/tool_head_pickaxe lapis/tool_head_plow "
        + "lapis/tool_head_raw_arrow lapis/tool_head_raw_axe lapis/tool_head_raw_axe_double lapis/tool_head_raw_chisel lapis/tool_head_raw_hoe lapis/tool_head_raw_pickaxe lapis/tool_head_raw_plow lapis/tool_head_raw_saw "
        + "lapis/tool_head_raw_sense lapis/tool_head_raw_shovel lapis/tool_head_raw_spade lapis/tool_head_raw_sword lapis/tool_head_raw_universal_spade lapis/tool_head_saw lapis/tool_head_screwdriver lapis/tool_head_sense "
        + "lapis/tool_head_shovel lapis/tool_head_spade lapis/tool_head_sword lapis/tool_head_universal_spade lapis/tool_head_wrench leaf/billet leaf/bolt leaf/casing_small "
        + "leaf/chain leaf/chunk_gt leaf/dust leaf/dust_div72 leaf/dust_small leaf/dust_tiny leaf/foil leaf/gear_gt "
        + "leaf/gear_gt_small leaf/ingot leaf/ingot_double leaf/ingot_quadruple leaf/ingot_quintuple leaf/ingot_triple leaf/nugget leaf/plant_gt_fiber "
        + "leaf/plant_gt_wart leaf/plate leaf/plate_curved leaf/plate_dense leaf/plate_double leaf/plate_quadruple leaf/plate_quintuple leaf/plate_tiny "
        + "leaf/plate_triple leaf/rotor leaf/round leaf/scrap_gt leaf/screw leaf/spring leaf/spring_small leaf/stick "
        + "leaf/stick_long leaf/tool_head_arrow leaf/tool_head_axe leaf/tool_head_axe_double leaf/tool_head_builderwand leaf/tool_head_buzz_saw leaf/tool_head_chainsaw leaf/tool_head_chisel "
        + "leaf/tool_head_construction_pickaxe leaf/tool_head_drill leaf/tool_head_file leaf/tool_head_hammer leaf/tool_head_hoe leaf/tool_head_pickaxe leaf/tool_head_plow leaf/tool_head_raw_arrow "
        + "leaf/tool_head_raw_axe leaf/tool_head_raw_axe_double leaf/tool_head_raw_chisel leaf/tool_head_raw_hoe leaf/tool_head_raw_pickaxe leaf/tool_head_raw_plow leaf/tool_head_raw_saw leaf/tool_head_raw_sense "
        + "leaf/tool_head_raw_shovel leaf/tool_head_raw_spade leaf/tool_head_raw_sword leaf/tool_head_raw_universal_spade leaf/tool_head_saw leaf/tool_head_screwdriver leaf/tool_head_sense leaf/tool_head_shovel "
        + "leaf/tool_head_spade leaf/tool_head_sword leaf/tool_head_universal_spade leaf/tool_head_wrench lignite/billet lignite/bolt lignite/chunk_gt lignite/crushed_centrifuged "
        + "lignite/crushed_centrifuged_tiny lignite/crushed_purified lignite/crushed_purified_tiny lignite/dust lignite/dust_div72 lignite/dust_small lignite/dust_tiny lignite/gem "
        + "lignite/ingot lignite/ingot_double lignite/ingot_quadruple lignite/ingot_quintuple lignite/ingot_triple lignite/nugget lignite/ore_raw lignite/plant_gt_fiber "
        + "lignite/plant_gt_wart lignite/plate lignite/plate_curved lignite/plate_gem lignite/plate_gem_tiny lignite/plate_tiny lignite/round lignite/scrap_gt "
        + "lignite/screw lignite/stick lignite/stick_long magnetic/tool_head_builderwand metallic/billet metallic/bolt metallic/casing_small metallic/chain "
        + "metallic/chunk_gt metallic/crushed_centrifuged metallic/crushed_centrifuged_tiny metallic/crushed_purified metallic/crushed_purified_tiny metallic/dust metallic/dust_div72 metallic/dust_small "
        + "metallic/dust_tiny metallic/foil metallic/gear_gt metallic/gear_gt_small metallic/gem metallic/ingot metallic/ingot_double metallic/ingot_quadruple "
        + "metallic/ingot_quintuple metallic/ingot_triple metallic/nugget metallic/ore_raw metallic/plant_gt_fiber metallic/plant_gt_wart metallic/plate metallic/plate_curved "
        + "metallic/plate_dense metallic/plate_double metallic/plate_quadruple metallic/plate_quintuple metallic/plate_tiny metallic/plate_triple metallic/rail_gt metallic/rotor "
        + "metallic/round metallic/scrap_gt metallic/screw metallic/spring metallic/spring_small metallic/stick metallic/stick_long metallic/tool_head_arrow "
        + "metallic/tool_head_axe metallic/tool_head_axe_double metallic/tool_head_builderwand metallic/tool_head_buzz_saw metallic/tool_head_chainsaw metallic/tool_head_chisel metallic/tool_head_construction_pickaxe metallic/tool_head_drill "
        + "metallic/tool_head_file metallic/tool_head_hammer metallic/tool_head_hoe metallic/tool_head_pickaxe metallic/tool_head_plow metallic/tool_head_raw_arrow metallic/tool_head_raw_axe metallic/tool_head_raw_axe_double "
        + "metallic/tool_head_raw_chisel metallic/tool_head_raw_hoe metallic/tool_head_raw_pickaxe metallic/tool_head_raw_plow metallic/tool_head_raw_saw metallic/tool_head_raw_sense metallic/tool_head_raw_shovel metallic/tool_head_raw_spade "
        + "metallic/tool_head_raw_sword metallic/tool_head_raw_universal_spade metallic/tool_head_saw metallic/tool_head_screwdriver metallic/tool_head_sense metallic/tool_head_shovel metallic/tool_head_spade metallic/tool_head_sword "
        + "metallic/tool_head_universal_spade metallic/tool_head_wrench netherstar/bolt netherstar/casing_small netherstar/chain netherstar/crushed_centrifuged netherstar/crushed_centrifuged_tiny netherstar/crushed_purified "
        + "netherstar/crushed_purified_tiny netherstar/dust_div72 netherstar/gem netherstar/gem_chipped netherstar/gem_exquisite netherstar/gem_flawed netherstar/gem_flawless netherstar/gem_legendary "
        + "netherstar/ore_raw netherstar/plant_gt_fiber netherstar/plant_gt_wart netherstar/rotor netherstar/round netherstar/scrap_gt netherstar/screw netherstar/stick_long "
        + "netherstar/tool_head_axe netherstar/tool_head_axe_double netherstar/tool_head_builderwand netherstar/tool_head_buzz_saw netherstar/tool_head_chainsaw netherstar/tool_head_chisel netherstar/tool_head_construction_pickaxe netherstar/tool_head_drill "
        + "netherstar/tool_head_file netherstar/tool_head_hammer netherstar/tool_head_hoe netherstar/tool_head_pickaxe netherstar/tool_head_plow netherstar/tool_head_raw_axe netherstar/tool_head_raw_axe_double netherstar/tool_head_raw_chisel "
        + "netherstar/tool_head_raw_hoe netherstar/tool_head_raw_pickaxe netherstar/tool_head_raw_plow netherstar/tool_head_raw_saw netherstar/tool_head_raw_sense netherstar/tool_head_raw_shovel netherstar/tool_head_raw_spade netherstar/tool_head_raw_sword "
        + "netherstar/tool_head_raw_universal_spade netherstar/tool_head_saw netherstar/tool_head_screwdriver netherstar/tool_head_sense netherstar/tool_head_shovel netherstar/tool_head_spade netherstar/tool_head_sword netherstar/tool_head_universal_spade "
        + "netherstar/tool_head_wrench opal/bolt opal/casing_small opal/chain opal/crushed_centrifuged opal/crushed_centrifuged_tiny opal/crushed_purified opal/crushed_purified_tiny "
        + "opal/dust_div72 opal/gem_chipped opal/gem_exquisite opal/gem_flawed opal/gem_flawless opal/gem_legendary opal/plant_gt_fiber opal/plant_gt_wart "
        + "opal/rotor opal/round opal/scrap_gt opal/screw opal/spring opal/spring_small opal/stick_long opal/tool_head_axe "
        + "opal/tool_head_axe_double opal/tool_head_builderwand opal/tool_head_buzz_saw opal/tool_head_chainsaw opal/tool_head_chisel opal/tool_head_construction_pickaxe opal/tool_head_drill opal/tool_head_file "
        + "opal/tool_head_hammer opal/tool_head_hoe opal/tool_head_pickaxe opal/tool_head_plow opal/tool_head_raw_axe opal/tool_head_raw_axe_double opal/tool_head_raw_chisel opal/tool_head_raw_hoe "
        + "opal/tool_head_raw_pickaxe opal/tool_head_raw_plow opal/tool_head_raw_saw opal/tool_head_raw_sense opal/tool_head_raw_shovel opal/tool_head_raw_spade opal/tool_head_raw_sword opal/tool_head_raw_universal_spade "
        + "opal/tool_head_saw opal/tool_head_screwdriver opal/tool_head_sense opal/tool_head_shovel opal/tool_head_spade opal/tool_head_sword opal/tool_head_universal_spade opal/tool_head_wrench "
        + "paper/dust paper/dust_div72 paper/dust_small paper/dust_tiny paper/plant_gt_fiber paper/plant_gt_wart paper/plate_double paper/plate_tiny "
        + "paper/scrap_gt powder/bolt powder/casing_small powder/chain powder/crushed_centrifuged powder/crushed_centrifuged_tiny powder/crushed_purified powder/crushed_purified_tiny "
        + "powder/dust powder/dust_div72 powder/dust_small powder/dust_tiny powder/gear_gt powder/gear_gt_small powder/ore_raw powder/plant_gt_fiber "
        + "powder/plant_gt_wart powder/plate powder/plate_curved powder/plate_tiny powder/rotor powder/round powder/scrap_gt powder/screw "
        + "powder/stick powder/stick_long powder/tool_head_arrow powder/tool_head_axe powder/tool_head_hammer powder/tool_head_hoe powder/tool_head_pickaxe powder/tool_head_plow "
        + "powder/tool_head_raw_arrow powder/tool_head_raw_axe powder/tool_head_raw_hoe powder/tool_head_raw_pickaxe powder/tool_head_raw_plow powder/tool_head_raw_sense powder/tool_head_raw_shovel powder/tool_head_raw_spade "
        + "powder/tool_head_raw_sword powder/tool_head_sense powder/tool_head_shovel powder/tool_head_spade powder/tool_head_sword prismarine/bolt prismarine/casing_small prismarine/chain "
        + "prismarine/crushed_centrifuged prismarine/crushed_centrifuged_tiny prismarine/crushed_purified prismarine/crushed_purified_tiny prismarine/dust_div72 prismarine/ore_raw prismarine/plant_gt_fiber prismarine/plant_gt_wart "
        + "prismarine/rotor prismarine/round prismarine/scrap_gt prismarine/screw prismarine/stick_long prismarine/tool_head_axe prismarine/tool_head_hammer prismarine/tool_head_hoe "
        + "prismarine/tool_head_pickaxe prismarine/tool_head_plow prismarine/tool_head_raw_axe prismarine/tool_head_raw_hoe prismarine/tool_head_raw_pickaxe prismarine/tool_head_raw_plow prismarine/tool_head_raw_sense prismarine/tool_head_raw_shovel "
        + "prismarine/tool_head_raw_spade prismarine/tool_head_raw_sword prismarine/tool_head_sense prismarine/tool_head_shovel prismarine/tool_head_spade prismarine/tool_head_sword quartz/billet quartz/bolt "
        + "quartz/casing_small quartz/chain quartz/chunk_gt quartz/crushed_centrifuged quartz/crushed_centrifuged_tiny quartz/crushed_purified quartz/crushed_purified_tiny quartz/dust "
        + "quartz/dust_div72 quartz/dust_small quartz/dust_tiny quartz/foil quartz/gear_gt quartz/gear_gt_small quartz/gem quartz/ingot "
        + "quartz/ingot_double quartz/ingot_quadruple quartz/ingot_quintuple quartz/ingot_triple quartz/nugget quartz/ore_raw quartz/plant_gt_fiber quartz/plant_gt_wart "
        + "quartz/plate quartz/plate_curved quartz/plate_dense quartz/plate_double quartz/plate_gem quartz/plate_gem_tiny quartz/plate_quadruple quartz/plate_quintuple "
        + "quartz/plate_tiny quartz/plate_triple quartz/rock_gt quartz/rotor quartz/round quartz/scrap_gt quartz/screw quartz/stick "
        + "quartz/stick_long quartz/tool_head_arrow quartz/tool_head_axe quartz/tool_head_axe_double quartz/tool_head_builderwand quartz/tool_head_buzz_saw quartz/tool_head_chainsaw quartz/tool_head_chisel "
        + "quartz/tool_head_construction_pickaxe quartz/tool_head_drill quartz/tool_head_file quartz/tool_head_hammer quartz/tool_head_hoe quartz/tool_head_pickaxe quartz/tool_head_plow quartz/tool_head_raw_arrow "
        + "quartz/tool_head_raw_axe quartz/tool_head_raw_axe_double quartz/tool_head_raw_chisel quartz/tool_head_raw_hoe quartz/tool_head_raw_pickaxe quartz/tool_head_raw_plow quartz/tool_head_raw_saw quartz/tool_head_raw_sense "
        + "quartz/tool_head_raw_shovel quartz/tool_head_raw_spade quartz/tool_head_raw_sword quartz/tool_head_raw_universal_spade quartz/tool_head_saw quartz/tool_head_screwdriver quartz/tool_head_sense quartz/tool_head_shovel "
        + "quartz/tool_head_spade quartz/tool_head_sword quartz/tool_head_universal_spade quartz/tool_head_wrench rad/billet rad/bolt rad/casing_small rad/chain "
        + "rad/chunk_gt rad/crushed_centrifuged rad/crushed_centrifuged_tiny rad/crushed_purified rad/crushed_purified_tiny rad/dust rad/dust_div72 rad/dust_small "
        + "rad/dust_tiny rad/foil rad/gear_gt rad/gear_gt_small rad/ingot rad/ingot_double rad/ingot_quadruple rad/ingot_quintuple "
        + "rad/ingot_triple rad/nugget rad/ore_raw rad/plant_gt_fiber rad/plant_gt_wart rad/plate rad/plate_curved rad/plate_dense "
        + "rad/plate_double rad/plate_quadruple rad/plate_quintuple rad/plate_tiny rad/plate_triple rad/rotor rad/round rad/scrap_gt "
        + "rad/screw rad/spring rad/spring_small rad/stick rad/stick_long rad/tool_head_arrow rad/tool_head_axe rad/tool_head_axe_double "
        + "rad/tool_head_builderwand rad/tool_head_buzz_saw rad/tool_head_chainsaw rad/tool_head_chisel rad/tool_head_construction_pickaxe rad/tool_head_drill rad/tool_head_file rad/tool_head_hammer "
        + "rad/tool_head_hoe rad/tool_head_pickaxe rad/tool_head_plow rad/tool_head_raw_arrow rad/tool_head_raw_axe rad/tool_head_raw_axe_double rad/tool_head_raw_chisel rad/tool_head_raw_hoe "
        + "rad/tool_head_raw_pickaxe rad/tool_head_raw_plow rad/tool_head_raw_saw rad/tool_head_raw_sense rad/tool_head_raw_shovel rad/tool_head_raw_spade rad/tool_head_raw_sword rad/tool_head_raw_universal_spade "
        + "rad/tool_head_saw rad/tool_head_screwdriver rad/tool_head_sense rad/tool_head_shovel rad/tool_head_spade rad/tool_head_sword rad/tool_head_universal_spade rad/tool_head_wrench "
        + "redstone/billet redstone/bolt redstone/casing_small redstone/chain redstone/chunk_gt redstone/crushed_centrifuged redstone/crushed_centrifuged_tiny redstone/crushed_purified "
        + "redstone/crushed_purified_tiny redstone/dust_div72 redstone/dust_small redstone/dust_tiny redstone/foil redstone/gear_gt redstone/gear_gt_small redstone/gem "
        + "redstone/gem_chipped redstone/gem_exquisite redstone/gem_flawed redstone/gem_flawless redstone/gem_legendary redstone/ingot redstone/ingot_double redstone/ingot_quadruple "
        + "redstone/ingot_quintuple redstone/ingot_triple redstone/nugget redstone/plant_gt_fiber redstone/plant_gt_wart redstone/plate redstone/plate_curved redstone/plate_dense "
        + "redstone/plate_double redstone/plate_gem redstone/plate_gem_tiny redstone/plate_quadruple redstone/plate_quintuple redstone/plate_tiny redstone/plate_triple redstone/rotor "
        + "redstone/round redstone/scrap_gt redstone/screw redstone/spring redstone/spring_small redstone/stick redstone/stick_long redstone/tool_head_arrow "
        + "redstone/tool_head_axe redstone/tool_head_axe_double redstone/tool_head_builderwand redstone/tool_head_buzz_saw redstone/tool_head_chainsaw redstone/tool_head_chisel redstone/tool_head_construction_pickaxe redstone/tool_head_drill "
        + "redstone/tool_head_file redstone/tool_head_hammer redstone/tool_head_hoe redstone/tool_head_pickaxe redstone/tool_head_plow redstone/tool_head_raw_arrow redstone/tool_head_raw_axe redstone/tool_head_raw_axe_double "
        + "redstone/tool_head_raw_chisel redstone/tool_head_raw_hoe redstone/tool_head_raw_pickaxe redstone/tool_head_raw_plow redstone/tool_head_raw_saw redstone/tool_head_raw_sense redstone/tool_head_raw_shovel redstone/tool_head_raw_spade "
        + "redstone/tool_head_raw_sword redstone/tool_head_raw_universal_spade redstone/tool_head_saw redstone/tool_head_screwdriver redstone/tool_head_sense redstone/tool_head_shovel redstone/tool_head_spade redstone/tool_head_sword "
        + "redstone/tool_head_universal_spade redstone/tool_head_wrench rough/billet rough/bolt rough/casing_small rough/chain rough/chunk_gt rough/crushed_centrifuged "
        + "rough/crushed_centrifuged_tiny rough/crushed_purified rough/crushed_purified_tiny rough/dust rough/dust_div72 rough/dust_small rough/dust_tiny rough/foil "
        + "rough/gear_gt rough/gear_gt_small rough/gem rough/ingot rough/ingot_double rough/ingot_quadruple rough/ingot_quintuple rough/ingot_triple "
        + "rough/nugget rough/ore_raw rough/plant_gt_fiber rough/plant_gt_wart rough/plate rough/plate_curved rough/plate_dense rough/plate_double "
        + "rough/plate_quadruple rough/plate_quintuple rough/plate_tiny rough/plate_triple rough/rotor rough/round rough/scrap_gt rough/screw "
        + "rough/spring rough/spring_small rough/stick rough/stick_long rubber/billet rubber/bolt rubber/casing_small rubber/chain "
        + "rubber/dust_div72 rubber/foil rubber/ingot_double rubber/ingot_quadruple rubber/ingot_quintuple rubber/ingot_triple rubber/plant_gt_fiber rubber/plant_gt_wart "
        + "rubber/plate_curved rubber/plate_dense rubber/plate_double rubber/plate_quadruple rubber/plate_quintuple rubber/plate_tiny rubber/plate_triple rubber/rotor "
        + "rubber/round rubber/scrap_gt rubber/screw rubber/spring rubber/spring_small rubber/tool_head_axe rubber/tool_head_hammer rubber/tool_head_hoe "
        + "rubber/tool_head_pickaxe rubber/tool_head_plow rubber/tool_head_raw_axe rubber/tool_head_raw_hoe rubber/tool_head_raw_pickaxe rubber/tool_head_raw_plow rubber/tool_head_raw_sense rubber/tool_head_raw_shovel "
        + "rubber/tool_head_raw_spade rubber/tool_head_raw_sword rubber/tool_head_sense rubber/tool_head_shovel rubber/tool_head_spade rubber/tool_head_sword ruby/billet ruby/bolt "
        + "ruby/casing_small ruby/chain ruby/chunk_gt ruby/crushed_centrifuged ruby/crushed_centrifuged_tiny ruby/crushed_purified ruby/crushed_purified_tiny ruby/dust_div72 "
        + "ruby/foil ruby/gem ruby/gem_chipped ruby/gem_exquisite ruby/gem_flawed ruby/gem_flawless ruby/gem_legendary ruby/ingot_double "
        + "ruby/ingot_quadruple ruby/ingot_quintuple ruby/ingot_triple ruby/plant_gt_fiber ruby/plant_gt_wart ruby/plate_curved ruby/plate_dense ruby/plate_double "
        + "ruby/plate_quadruple ruby/plate_quintuple ruby/plate_tiny ruby/plate_triple ruby/rotor ruby/round ruby/scrap_gt ruby/screw "
        + "ruby/spring ruby/spring_small ruby/stick_long ruby/tool_head_axe ruby/tool_head_axe_double ruby/tool_head_builderwand ruby/tool_head_buzz_saw ruby/tool_head_chainsaw "
        + "ruby/tool_head_chisel ruby/tool_head_construction_pickaxe ruby/tool_head_drill ruby/tool_head_file ruby/tool_head_hammer ruby/tool_head_hoe ruby/tool_head_pickaxe ruby/tool_head_plow "
        + "ruby/tool_head_raw_axe ruby/tool_head_raw_axe_double ruby/tool_head_raw_chisel ruby/tool_head_raw_hoe ruby/tool_head_raw_pickaxe ruby/tool_head_raw_plow ruby/tool_head_raw_saw ruby/tool_head_raw_sense "
        + "ruby/tool_head_raw_shovel ruby/tool_head_raw_spade ruby/tool_head_raw_sword ruby/tool_head_raw_universal_spade ruby/tool_head_saw ruby/tool_head_screwdriver ruby/tool_head_sense ruby/tool_head_shovel "
        + "ruby/tool_head_spade ruby/tool_head_sword ruby/tool_head_universal_spade ruby/tool_head_wrench sand/crushed_centrifuged sand/crushed_centrifuged_tiny sand/crushed_purified sand/crushed_purified_tiny "
        + "sand/dust sand/dust_div72 sand/dust_small sand/dust_tiny sand/ore_raw sand/plant_gt_fiber sand/plant_gt_wart sand/scrap_gt "
        + "shards/bolt shards/casing_small shards/chain shards/crushed_centrifuged shards/crushed_centrifuged_tiny shards/crushed_purified shards/crushed_purified_tiny shards/dust_div72 "
        + "shards/gem shards/gem_chipped shards/gem_exquisite shards/gem_flawed shards/gem_flawless shards/gem_legendary shards/plant_gt_fiber shards/plant_gt_wart "
        + "shards/rotor shards/round shards/scrap_gt shards/screw shards/spring shards/spring_small shards/stick_long shards/tool_head_axe "
        + "shards/tool_head_axe_double shards/tool_head_builderwand shards/tool_head_buzz_saw shards/tool_head_chainsaw shards/tool_head_chisel shards/tool_head_construction_pickaxe shards/tool_head_drill shards/tool_head_file "
        + "shards/tool_head_hammer shards/tool_head_hoe shards/tool_head_pickaxe shards/tool_head_plow shards/tool_head_raw_axe shards/tool_head_raw_axe_double shards/tool_head_raw_chisel shards/tool_head_raw_hoe "
        + "shards/tool_head_raw_pickaxe shards/tool_head_raw_plow shards/tool_head_raw_saw shards/tool_head_raw_sense shards/tool_head_raw_shovel shards/tool_head_raw_spade shards/tool_head_raw_sword shards/tool_head_raw_universal_spade "
        + "shards/tool_head_saw shards/tool_head_screwdriver shards/tool_head_sense shards/tool_head_shovel shards/tool_head_spade shards/tool_head_sword shards/tool_head_universal_spade shards/tool_head_wrench "
        + "shiny/billet shiny/bolt shiny/casing_small shiny/chain shiny/chunk_gt shiny/crushed_centrifuged shiny/crushed_centrifuged_tiny shiny/crushed_purified "
        + "shiny/crushed_purified_tiny shiny/dust_div72 shiny/foil shiny/gem shiny/ingot_double shiny/ingot_quadruple shiny/ingot_quintuple shiny/ingot_triple "
        + "shiny/ore_raw shiny/plant_gt_fiber shiny/plant_gt_wart shiny/plate shiny/plate_curved shiny/plate_dense shiny/plate_double shiny/plate_quadruple "
        + "shiny/plate_quintuple shiny/plate_tiny shiny/plate_triple shiny/rail_gt shiny/rotor shiny/round shiny/scrap_gt shiny/screw "
        + "shiny/spring shiny/spring_small shiny/stick_long shiny/tool_head_axe shiny/tool_head_axe_double shiny/tool_head_builderwand shiny/tool_head_buzz_saw shiny/tool_head_chainsaw "
        + "shiny/tool_head_chisel shiny/tool_head_construction_pickaxe shiny/tool_head_drill shiny/tool_head_file shiny/tool_head_hammer shiny/tool_head_hoe shiny/tool_head_pickaxe shiny/tool_head_plow "
        + "shiny/tool_head_raw_axe shiny/tool_head_raw_axe_double shiny/tool_head_raw_chisel shiny/tool_head_raw_hoe shiny/tool_head_raw_pickaxe shiny/tool_head_raw_plow shiny/tool_head_raw_saw shiny/tool_head_raw_sense "
        + "shiny/tool_head_raw_shovel shiny/tool_head_raw_spade shiny/tool_head_raw_sword shiny/tool_head_raw_universal_spade shiny/tool_head_saw shiny/tool_head_screwdriver shiny/tool_head_sense shiny/tool_head_shovel "
        + "shiny/tool_head_spade shiny/tool_head_sword shiny/tool_head_universal_spade shiny/tool_head_wrench space/billet space/bolt space/casing_small space/chain "
        + "space/chunk_gt space/crushed_centrifuged space/crushed_centrifuged_tiny space/crushed_purified space/crushed_purified_tiny space/dust space/dust_div72 space/dust_small "
        + "space/dust_tiny space/foil space/gear_gt space/gear_gt_small space/ingot space/ingot_double space/ingot_quadruple space/ingot_quintuple "
        + "space/ingot_triple space/nugget space/ore_raw space/plant_gt_fiber space/plant_gt_wart space/plate space/plate_curved space/plate_dense "
        + "space/plate_double space/plate_quadruple space/plate_quintuple space/plate_tiny space/plate_triple space/rail_gt space/rock_gt space/rotor "
        + "space/round space/scrap_gt space/screw space/spring space/spring_small space/stick space/stick_long space/tool_head_arrow "
        + "space/tool_head_axe space/tool_head_axe_double space/tool_head_builderwand space/tool_head_buzz_saw space/tool_head_chainsaw space/tool_head_chisel space/tool_head_construction_pickaxe space/tool_head_drill "
        + "space/tool_head_file space/tool_head_hammer space/tool_head_hoe space/tool_head_pickaxe space/tool_head_plow space/tool_head_raw_arrow space/tool_head_raw_axe space/tool_head_raw_axe_double "
        + "space/tool_head_raw_chisel space/tool_head_raw_hoe space/tool_head_raw_pickaxe space/tool_head_raw_plow space/tool_head_raw_saw space/tool_head_raw_sense space/tool_head_raw_shovel space/tool_head_raw_spade "
        + "space/tool_head_raw_sword space/tool_head_raw_universal_spade space/tool_head_saw space/tool_head_screwdriver space/tool_head_sense space/tool_head_shovel space/tool_head_spade space/tool_head_sword "
        + "space/tool_head_universal_spade space/tool_head_wrench stone/bolt stone/casing_small stone/chain stone/crushed_centrifuged stone/crushed_centrifuged_tiny stone/crushed_purified "
        + "stone/crushed_purified_tiny stone/dust stone/dust_div72 stone/dust_small stone/dust_tiny stone/gear_gt stone/gear_gt_small stone/ore_raw "
        + "stone/plant_gt_fiber stone/plant_gt_wart stone/plate stone/plate_curved stone/plate_tiny stone/rock_gt stone/rotor stone/round "
        + "stone/scrap_gt stone/screw stone/stick stone/stick_long stone/tool_head_arrow stone/tool_head_axe stone/tool_head_hammer stone/tool_head_hoe "
        + "stone/tool_head_pickaxe stone/tool_head_plow stone/tool_head_raw_arrow stone/tool_head_raw_axe stone/tool_head_raw_hoe stone/tool_head_raw_pickaxe stone/tool_head_raw_plow stone/tool_head_raw_sense "
        + "stone/tool_head_raw_shovel stone/tool_head_raw_spade stone/tool_head_raw_sword stone/tool_head_sense stone/tool_head_shovel stone/tool_head_spade stone/tool_head_sword wood/billet "
        + "wood/bolt wood/casing_small wood/chain wood/chunk_gt wood/dust wood/dust_div72 wood/dust_small wood/dust_tiny "
        + "wood/foil wood/gear_gt wood/gear_gt_small wood/ingot wood/ingot_double wood/ingot_quadruple wood/ingot_quintuple wood/ingot_triple "
        + "wood/nugget wood/plant_gt_fiber wood/plant_gt_wart wood/plate wood/plate_curved wood/plate_dense wood/plate_double wood/plate_quadruple "
        + "wood/plate_quintuple wood/plate_tiny wood/plate_triple wood/rotor wood/round wood/scrap_gt wood/screw wood/spring "
        + "wood/spring_small wood/stick wood/stick_long wood/tool_head_arrow wood/tool_head_axe wood/tool_head_axe_double wood/tool_head_builderwand wood/tool_head_buzz_saw "
        + "wood/tool_head_chainsaw wood/tool_head_chisel wood/tool_head_construction_pickaxe wood/tool_head_drill wood/tool_head_file wood/tool_head_hammer wood/tool_head_hoe wood/tool_head_pickaxe "
        + "wood/tool_head_plow wood/tool_head_raw_arrow wood/tool_head_raw_axe wood/tool_head_raw_axe_double wood/tool_head_raw_chisel wood/tool_head_raw_hoe wood/tool_head_raw_pickaxe wood/tool_head_raw_plow "
        + "wood/tool_head_raw_saw wood/tool_head_raw_sense wood/tool_head_raw_shovel wood/tool_head_raw_spade wood/tool_head_raw_sword wood/tool_head_raw_universal_spade wood/tool_head_saw wood/tool_head_screwdriver "
        + "wood/tool_head_sense wood/tool_head_shovel wood/tool_head_spade wood/tool_head_sword wood/tool_head_universal_spade wood/tool_head_wrench ",
    "item/overlay/gear_gt cube_shiny/gear_gt diamond/gear_gt emerald/gear_gt gem_horizontal/gear_gt gem_vertical/gear_gt glass/gear_gt netherstar/gear_gt opal/gear_gt "
        + "prismarine/gear_gt rubber/gear_gt ruby/gear_gt shards/gear_gt shiny/gear_gt ",
    "item/overlay/gear_gt_small cube_shiny/gear_gt_small diamond/gear_gt_small emerald/gear_gt_small gem_horizontal/gear_gt_small gem_vertical/gear_gt_small glass/gear_gt_small netherstar/gear_gt_small opal/gear_gt_small "
        + "prismarine/gear_gt_small rubber/gear_gt_small ruby/gear_gt_small shards/gear_gt_small shiny/gear_gt_small ",
    "item/overlay/ingot diamond/ingot ruby/ingot shiny/ingot ",
    "item/overlay/ingot_hot brick/ingot_hot copper/ingot_hot diamond/ingot_hot dull/ingot_hot fiery/ingot_hot leaf/ingot_hot metallic/ingot_hot quartz/ingot_hot "
        + "rad/ingot_hot redstone/ingot_hot rough/ingot_hot ruby/ingot_hot shiny/ingot_hot space/ingot_hot wood/ingot_hot ",
    "item/overlay/lens cube_shiny/lens diamond/lens emerald/lens fine/lens flint/lens gem_horizontal/lens gem_vertical/lens glass/lens "
        + "hex/lens lapis/lens netherstar/lens opal/lens redstone/lens ruby/lens shards/lens shiny/lens ",
    "item/overlay/minecart_wheels brick/minecart_wheels copper/minecart_wheels diamond/minecart_wheels dull/minecart_wheels leaf/minecart_wheels metallic/minecart_wheels quartz/minecart_wheels rad/minecart_wheels "
        + "redstone/minecart_wheels rough/minecart_wheels ruby/minecart_wheels shiny/minecart_wheels space/minecart_wheels wood/minecart_wheels ",
    "item/overlay/nugget diamond/nugget ruby/nugget shiny/nugget ",
    "item/overlay/ore_raw cube/ore_raw cube_shiny/ore_raw diamond/ore_raw emerald/ore_raw gem_horizontal/ore_raw gem_vertical/ore_raw hex/ore_raw lapis/ore_raw "
        + "opal/ore_raw redstone/ore_raw ruby/ore_raw shards/ore_raw ",
    "item/overlay/plant_gt_berry brick/plant_gt_berry copper/plant_gt_berry cube/plant_gt_berry cube_shiny/plant_gt_berry diamond/plant_gt_berry dull/plant_gt_berry emerald/plant_gt_berry fiery/plant_gt_berry "
        + "fine/plant_gt_berry flint/plant_gt_berry fluid/plant_gt_berry food/plant_gt_berry gas/plant_gt_berry gem_horizontal/plant_gt_berry gem_vertical/plant_gt_berry glass/plant_gt_berry "
        + "hex/plant_gt_berry lapis/plant_gt_berry leaf/plant_gt_berry lignite/plant_gt_berry metallic/plant_gt_berry netherstar/plant_gt_berry opal/plant_gt_berry paper/plant_gt_berry "
        + "powder/plant_gt_berry prismarine/plant_gt_berry quartz/plant_gt_berry rad/plant_gt_berry redstone/plant_gt_berry rough/plant_gt_berry rubber/plant_gt_berry ruby/plant_gt_berry "
        + "sand/plant_gt_berry shards/plant_gt_berry shiny/plant_gt_berry space/plant_gt_berry stone/plant_gt_berry wood/plant_gt_berry ",
    "item/overlay/plant_gt_blossom brick/plant_gt_blossom copper/plant_gt_blossom cube/plant_gt_blossom cube_shiny/plant_gt_blossom diamond/plant_gt_blossom dull/plant_gt_blossom emerald/plant_gt_blossom fiery/plant_gt_blossom "
        + "fine/plant_gt_blossom flint/plant_gt_blossom fluid/plant_gt_blossom food/plant_gt_blossom gas/plant_gt_blossom gem_horizontal/plant_gt_blossom gem_vertical/plant_gt_blossom glass/plant_gt_blossom "
        + "hex/plant_gt_blossom lapis/plant_gt_blossom leaf/plant_gt_blossom lignite/plant_gt_blossom metallic/plant_gt_blossom netherstar/plant_gt_blossom opal/plant_gt_blossom paper/plant_gt_blossom "
        + "powder/plant_gt_blossom prismarine/plant_gt_blossom quartz/plant_gt_blossom rad/plant_gt_blossom redstone/plant_gt_blossom rough/plant_gt_blossom rubber/plant_gt_blossom ruby/plant_gt_blossom "
        + "sand/plant_gt_blossom shards/plant_gt_blossom shiny/plant_gt_blossom space/plant_gt_blossom stone/plant_gt_blossom wood/plant_gt_blossom ",
    "item/overlay/plant_gt_twig brick/plant_gt_twig copper/plant_gt_twig cube/plant_gt_twig cube_shiny/plant_gt_twig diamond/plant_gt_twig dull/plant_gt_twig emerald/plant_gt_twig fiery/plant_gt_twig "
        + "fine/plant_gt_twig flint/plant_gt_twig fluid/plant_gt_twig food/plant_gt_twig gas/plant_gt_twig gem_horizontal/plant_gt_twig gem_vertical/plant_gt_twig glass/plant_gt_twig "
        + "hex/plant_gt_twig lapis/plant_gt_twig leaf/plant_gt_twig lignite/plant_gt_twig metallic/plant_gt_twig netherstar/plant_gt_twig opal/plant_gt_twig paper/plant_gt_twig "
        + "powder/plant_gt_twig prismarine/plant_gt_twig quartz/plant_gt_twig rad/plant_gt_twig redstone/plant_gt_twig rough/plant_gt_twig rubber/plant_gt_twig ruby/plant_gt_twig "
        + "sand/plant_gt_twig shards/plant_gt_twig shiny/plant_gt_twig space/plant_gt_twig stone/plant_gt_twig wood/plant_gt_twig ",
    "item/overlay/plate magnetic/plate magnetic/plate_curved ",
    "item/overlay/plate_gem copper/plate_gem cube_shiny/plate_gem diamond/plate diamond/plate_gem emerald/plate_gem gem_horizontal/plate_gem gem_vertical/plate_gem glass/plate_gem "
        + "metallic/plate_gem netherstar/plate_gem opal/plate_gem prismarine/plate_gem ruby/plate ruby/plate_gem shards/plate_gem shiny/plate_gem ",
    "item/overlay/plate_gem_tiny copper/plate_gem_tiny cube_shiny/plate_gem_tiny diamond/plate_gem_tiny emerald/plate_gem_tiny gem_horizontal/plate_gem_tiny gem_vertical/plate_gem_tiny glass/plate_gem_tiny metallic/plate_gem_tiny "
        + "netherstar/plate_gem_tiny opal/plate_gem_tiny prismarine/plate_gem_tiny ruby/plate_gem_tiny shards/plate_gem_tiny shiny/plate_gem_tiny ",
    "item/overlay/plate_quadruple magnetic/plate_quadruple magnetic/plate_tiny magnetic/scrap_gt ",
    "item/overlay/ring brick/ring copper/ring cube_shiny/ring diamond/ring dull/ring emerald/ring fine/ring flint/ring "
        + "food/ring gem_horizontal/ring gem_vertical/ring glass/ring hex/ring lapis/ring leaf/ring metallic/ring "
        + "netherstar/ring opal/ring powder/ring prismarine/ring quartz/ring rad/ring redstone/ring rough/ring "
        + "rubber/ring ruby/ring shards/ring shiny/ring space/ring stone/ring wood/ring ",
    "item/overlay/rock_gt copper/rock_gt cube/rock_gt cube_shiny/rock_gt diamond/rock_gt dull/rock_gt emerald/rock_gt fine/rock_gt flint/rock_gt "
        + "gem_horizontal/rock_gt gem_vertical/rock_gt glass/rock_gt hex/rock_gt lapis/rock_gt lignite/rock_gt metallic/rock_gt netherstar/rock_gt "
        + "opal/rock_gt powder/rock_gt rad/rock_gt redstone/rock_gt rough/rock_gt ruby/rock_gt sand/rock_gt shards/rock_gt "
        + "shiny/rock_gt ",
    "item/overlay/stick cube_shiny/stick diamond/stick emerald/stick gem_horizontal/stick gem_vertical/stick glass/stick hex/stick magnetic/stick "
        + "magnetic/tool_head_axe_double magnetic/tool_head_construction_pickaxe magnetic/tool_head_raw_axe_double netherstar/stick opal/stick prismarine/stick ruby/stick shards/stick "
        + "shiny/stick ",
    "item/overlay/tool_head_arrow cube_shiny/tool_head_arrow cube_shiny/tool_head_raw_arrow diamond/tool_head_arrow diamond/tool_head_raw_arrow emerald/tool_head_arrow emerald/tool_head_raw_arrow fiery/tool_head_arrow fiery/tool_head_raw_arrow "
        + "gem_horizontal/tool_head_arrow gem_horizontal/tool_head_raw_arrow gem_vertical/tool_head_arrow gem_vertical/tool_head_raw_arrow glass/tool_head_arrow glass/tool_head_raw_arrow hex/tool_head_arrow hex/tool_head_raw_arrow "
        + "magnetic/tool_head_arrow magnetic/tool_head_raw_arrow netherstar/tool_head_arrow netherstar/tool_head_raw_arrow opal/tool_head_arrow opal/tool_head_raw_arrow prismarine/tool_head_arrow prismarine/tool_head_raw_arrow "
        + "rubber/tool_head_arrow rubber/tool_head_raw_arrow ruby/tool_head_arrow ruby/tool_head_raw_arrow shards/tool_head_arrow shards/tool_head_raw_arrow shiny/tool_head_arrow shiny/tool_head_raw_arrow ",
    "item/overlay/tool_head_axe fiery/tool_head_axe fiery/tool_head_raw_axe magnetic/tool_head_axe magnetic/tool_head_raw_axe ",
    "item/overlay/tool_head_axe_double fiery/tool_head_axe_double fiery/tool_head_raw_axe_double ",
    "item/overlay/tool_head_buzz_saw magnetic/tool_head_buzz_saw magnetic/tool_head_plow magnetic/tool_head_raw_plow ",
    "item/overlay/tool_head_chisel fiery/tool_head_chisel fiery/tool_head_raw_chisel ",
    "item/overlay/tool_head_hoe fiery/tool_head_hoe fiery/tool_head_raw_hoe magnetic/tool_head_hoe magnetic/tool_head_raw_hoe ",
    "item/overlay/tool_head_pickaxe fiery/tool_head_pickaxe fiery/tool_head_raw_pickaxe magnetic/tool_head_pickaxe magnetic/tool_head_raw_pickaxe ",
    "item/overlay/tool_head_pickaxe_gem cube_shiny/tool_head_pickaxe_gem diamond/tool_head_pickaxe_gem emerald/tool_head_pickaxe_gem fine/tool_head_pickaxe_gem flint/tool_head_pickaxe_gem gem_horizontal/tool_head_pickaxe_gem gem_vertical/tool_head_pickaxe_gem glass/tool_head_pickaxe_gem "
        + "hex/tool_head_pickaxe_gem lapis/tool_head_pickaxe_gem netherstar/tool_head_pickaxe_gem opal/tool_head_pickaxe_gem redstone/tool_head_pickaxe_gem ruby/tool_head_pickaxe_gem shards/tool_head_pickaxe_gem ",
    "item/overlay/tool_head_plow fiery/tool_head_plow fiery/tool_head_raw_plow ",
    "item/overlay/tool_head_raw_chisel magnetic/tool_head_raw_chisel magnetic/tool_head_raw_saw magnetic/tool_head_saw ",
    "item/overlay/tool_head_raw_saw fiery/tool_head_raw_saw fiery/tool_head_saw ",
    "item/overlay/tool_head_raw_sense fiery/tool_head_raw_sense fiery/tool_head_sense magnetic/tool_head_raw_sense magnetic/tool_head_sense ",
    "item/overlay/tool_head_raw_shovel fiery/tool_head_raw_shovel fiery/tool_head_shovel magnetic/tool_head_raw_shovel magnetic/tool_head_shovel ",
    "item/overlay/tool_head_raw_sword fiery/tool_head_raw_sword fiery/tool_head_sword magnetic/tool_head_raw_sword magnetic/tool_head_sword ",
    "item/overlay/tool_head_raw_universal_spade fiery/tool_head_raw_universal_spade fiery/tool_head_universal_spade magnetic/tool_head_raw_universal_spade magnetic/tool_head_universal_spade ",
    "item/overlay/wire_fine brick/wire_fine copper/wire_fine diamond/wire_fine dull/wire_fine leaf/wire_fine metallic/wire_fine quartz/wire_fine rad/wire_fine "
        + "redstone/wire_fine rough/wire_fine rubber/wire_fine ruby/wire_fine shiny/wire_fine space/wire_fine wood/wire_fine ",
    };

    /** member "set/prefix" -> canonical resource path (e.g. "ruby/stick" -> "item/overlay/empty"); package-private for the GT6ItemModelLayersTest gate. */
    static final Map<String, String> OVERLAY_CANONICAL = new java.util.HashMap<>();
    static {
        for (String row : OVERLAY_CONTENT_GROUPS) {
            int space = row.indexOf(' ');
            String canonical = row.substring(0, space);
            for (String member : row.substring(space + 1).split(" ")) {
                String prev = OVERLAY_CANONICAL.put(member, canonical);
                if (prev != null) {
                    throw new IllegalStateException("overlay dedupe member twice: " + member);
                }
            }
        }
    }

    /**
     * The overlay second-pass sprite for one (iconset, prefix): the dedupe canonical when
     * the pair is a content-group member, else the per-set sprite gated on file existence
     * (the singletons; the research.p27-render-three-fixes F2 ruling — the gate is never
     * inferred from the prefix or set). Null = the pair has no overlay pass.
     */
    private ResourceLocation overlaySpriteOf(String aSet, String aPrefix) {
        String canonical = OVERLAY_CANONICAL.get(aSet + "/" + aPrefix);
        if (canonical != null) {
            return modLoc(canonical);
        }
        ResourceLocation perSet = modLoc("item/material_sets/" + aSet + "/" + aPrefix + "_overlay");
        return existingFileHelper.exists(perSet, TEXTURE) ? perSet : null;
    }

    @Override
    protected void registerModels() {
        for (GT6DatagenItems.Entry tEntry : GT6DatagenItems.collect()) {
            String tSet = iconsetOf(tEntry.material());
            String tPrefix = MaterialPrefixItem.snakeCase(tEntry.prefix().mNameInternal);
            // task casing-family-3d — the four machine-casing families ride the 3D
            // cube (the casingCubeModel javadoc); every other prefix keeps the flat
            // item/generated sprite below.
            if (tEntry.prefix() == gregapi.data.OP.casingMachine
                    || tEntry.prefix() == gregapi.data.OP.casingMachineDouble
                    || tEntry.prefix() == gregapi.data.OP.casingMachineQuadruple
                    || tEntry.prefix() == gregapi.data.OP.casingMachineDense) {
                casingCubeModel(tEntry.itemId(), tSet, tPrefix);
                continue;
            }
            // the OVERLAY second pass (task tool-model-layers): upstream registers
            // every materialicon as a base + "<NAME>_OVERLAY" pair and draws pass0 tinted
            // with the material colour / pass1 un-tinted (TextureSet.java:113-116 and
            // :124-126). The layer is un-tinted for free: the shared ItemColor returns -1
            // for tintIndex != 0 (MaterialPrefixItem.java:104). task overlay-texture-dedupe:
            // the sprite resolves through the content-group canonical (overlaySpriteOf —
            // alias map first, per-set file-existence fallback for the singletons).
            ResourceLocation tOverlay = overlaySpriteOf(tSet, tPrefix);
            if (tOverlay != null) {
                withExistingParent(tEntry.itemId(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/material_sets/" + tSet + "/" + tPrefix))
                    .texture("layer1", tOverlay);
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
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/chisel"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the formal file item (task tool-system): FOUR layers, the chisel row
        // shape — head = the toolHeadFile materialicon pair (Steel = metallic set,
        // GT_Tool_File.getIcon :91-93), handle = the HANDLE_FILE iconset pair borrow
        // (the old single layer carried only the handle; the head layer is the restore).
        withExistingParent("file", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_file"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/file"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the formal saw item (task tool-system): FOUR layers, the chisel row shape
        // — head = the toolHeadSaw materialicon pair (Steel = metallic set,
        // GT_Tool_Saw.getIcon :186-188), handle = the HANDLE_SAW iconset pair borrow.
        withExistingParent("saw", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_saw"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/saw"))
            .texture("layer3", modLoc("item/overlay/empty"));
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
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
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
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/screwdriver"))
            .texture("layer3", modLoc("item/overlay/empty"));
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
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the formal wrench item (task tool-hammer-wrench): handheld parent = the
        // vanilla tool shape; layer0 = the byte-identical WRENCH.png iconset borrow +
        // layer1 = the WRENCH_OVERLAY.png pass borrow (transparent upstream, borrowed for
        // structure parity — assets/README.md attribution); VOID handle = two-layer.
        withExistingParent("wrench", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/wrench"))
            .texture("layer1", modLoc("item/overlay/empty"));
        // the formal small bending cylinder item (task food-can-row0): the wrench
        // row shape; layer0 = the BENDING_CYLINDER_SMALL.png borrow + layer1 = its
        // OVERLAY pass borrow (transparent upstream — assets/README.md attribution);
        // VOID handle = two-layer.
        withExistingParent("bending_cylinder_small", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/bending_cylinder_small"))
            .texture("layer1", modLoc("item/overlay/empty"));
        // the six blade tools (task w5-t2-blade-six) — the chisel/file four-layer row
        // shape. Sword: head = the toolHeadSword metallic-set pair (upstream getIcon :113-115
        // primary Steel), handle = the HANDLE_SWORD iconset pair borrow (item/sword.png).
        // Knife/Butchery Knife/Club: the single composed iconset pair borrows (upstream
        // KNIFE/BUTCHERYKNIFE/CLUB icons, VOID handles — GT_Tool_Knife.getIcon :85-87 /
        // GT_Tool_ButcheryKnife :97-99 / GT_Tool_Club :113-115), two-layer.
        withExistingParent("sword", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_sword"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/sword"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("knife", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/knife"))
            .texture("layer1", modLoc("item/knife_overlay"));
        withExistingParent("butchery_knife", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/butchery_knife"))
            .texture("layer1", modLoc("item/butchery_knife_overlay"));
        withExistingParent("club", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/club"))
            .texture("layer1", modLoc("item/overlay/empty"));
        // Axe: head = the toolHeadAxe metallic-set pair (GT_Tool_Axe.getIcon :151-153), handle
        // = the stick pair (the secondary Spruce — the builder-wand handle borrow shape).
        withExistingParent("axe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_axe"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // Double Axe: the toolHeadAxeDouble metallic-set pair + the stick pair.
        withExistingParent("axe_double", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_axe_double"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the dig-family ladder rows (task dig-ladder): the axe row shape — head =
        // the toolHead metallic-set pair (layer0 = the TINTED head base, tint index 0,
        // the GT6ToolLadder material colour), layer1 = the OVERLAY shadow; handle = the
        // wood stick pair. The dig-six card (w5-t1-dig-six) shipped these six items
        // WITHOUT model rows (the registered items fell back to the missing model) — this
        // batch restores the dig band's render face alongside the material ladder.
        withExistingParent("pickaxe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_pickaxe"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the gem pick's head = the FINE set (the Diamond default primary's texture set,
        // GT_Tool_PickaxeGem.getIcon :34 — the gem identity rides the head texture set).
        withExistingParent("pickaxe_gem", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/fine/tool_head_pickaxe_gem"))
            .texture("layer1", modLoc("item/overlay/tool_head_pickaxe_gem"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("pickaxe_construction", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_construction_pickaxe"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("shovel", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_shovel"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("spade", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_spade"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("universal_spade", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_universal_spade"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("hoe", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_hoe"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        // the formal soft hammer item (task w5-t3-machine-face-four): FOUR layers, the
        // builder-wand row shape — head = the RUBBER-set toolHeadHammer pair (the upstream
        // primary ANY.Rubber default, GT_Tool_SoftHammer.getIcon :120 — SET_RUBBER, not the
        // steel head), handle = the wood stick pair (the upstream secondary
        // MT.WOODS.Spruce, the same :120 row; the builder-wand borrow posture — zero new
        // sprite files, all four layers are existing material_sets borrows).
        withExistingParent("soft_hammer", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/rubber/tool_head_hammer"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
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
                .texture("layer1", modLoc("item/overlay/pocket_tool"));
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
                .texture("layer1", modLoc("item/overlay/empty"))
                .texture("layer2", modLoc("item/" + tPath + "_handle"))
                .texture("layer3", modLoc("item/overlay/empty"));
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
            if (tRow.family().startsWith("energium_")) {
                energiumCrystalModel(tRow.path(), tRow.family());
            } else {
                withExistingParent(tRow.path(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/battery/" + tRow.family()));
            }
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
        // the dynamite family (task explosives-3d-item-models; registration
        // Loader_MultiTileEntities.java:2236-2238 metas 32104/32713/32712) — the upstream
        // ITEM is the 3D block form (MultiTileEntityDynamite MTE; the 1.7.10 inventory icon
        // renders the default-state block, the tap-funnel icon3d precedent), so the flat
        // item/generated sprite pair was the user symptom13 anti-form. The model mirrors the
        // upstream block element-by-element: default facing = getDefaultSide() = SIDE_UP
        // (TileEntityBase09FacingSingle.java:91), the stick bounds (5,0,5)-(11,16,11)
        // (MultiTileEntityDynamite.java:157-160) — a 6x16x6 rod along Y, front on UP /
        // back on DOWN (OPOS) / side on the four laterals (:177-181 getTexture2 face roles).
        // The grayscale "colored" pass rides tintindex 0 (the GT6ExplosivesTintListener
        // ItemColor serves the row material, element faces tint like the generated layer0 —
        // the casingCubeModel lesson), the "overlay" pass renders as-is un-tinted (the
        // partModel 0.01-offset decal form, cutout for the alpha texels); particle = the
        // body sprite (the r11-machine-particle-key lesson). One shared shape per item, the
        // colour identity riding the tint, not the file.
        for (gregtech6.registry.GT6Explosives.DynamiteRow tRow : gregtech6.registry.GT6Explosives.ROWS) {
            ItemModelBuilder tDynamite = getBuilder(tRow.path())
                    .parent(getExistingFile(mcLoc("block/block")))
                    .texture("particle", modLoc("block/dynamite_colored_side"))
                    .texture("front", modLoc("block/dynamite_colored_front"))
                    .texture("back", modLoc("block/dynamite_colored_back"))
                    .texture("side", modLoc("block/dynamite_colored_side"))
                    .texture("overlay_front", modLoc("block/dynamite_overlay_front"))
                    .texture("overlay_back", modLoc("block/dynamite_overlay_back"))
                    .texture("overlay_side", modLoc("block/dynamite_overlay_side"))
                    .renderType("cutout");
            // element 0 — the body stick, tintindex 0 = the material tint (the :157-160 bounds);
            // allFaces = the casingCubeModel:1083 form (FaceBuilder has no face() — one
            // allFaces lambda covers the six face roles in a single element)
            tDynamite.element()
                    .from(5.0F, 0.0F, 5.0F).to(11.0F, 16.0F, 11.0F)
                    .allFaces((aDir, aFace) -> aFace.texture(
                            aDir == net.minecraft.core.Direction.UP ? "#front"
                                : aDir == net.minecraft.core.Direction.DOWN ? "#back" : "#side").tintindex(0))
                    .end();
            // elements 1-6 — the overlay decals (the partModel 0.01-offset form, un-tinted = -1)
            tDynamite.element() // up
                    .from(5.0F, 16.0F, 5.0F).to(11.0F, 16.01F, 11.0F)
                    .face(net.minecraft.core.Direction.UP).texture("#overlay_front")
                    .end();
            tDynamite.element() // down
                    .from(5.0F, -0.01F, 5.0F).to(11.0F, 0.0F, 11.0F)
                    .face(net.minecraft.core.Direction.DOWN).texture("#overlay_back")
                    .end();
            tDynamite.element() // north
                    .from(5.0F, 0.0F, -0.01F).to(11.0F, 16.0F, 0.0F)
                    .face(net.minecraft.core.Direction.NORTH).texture("#overlay_side")
                    .end();
            tDynamite.element() // south
                    .from(5.0F, 0.0F, 16.0F).to(11.0F, 16.0F, 16.01F)
                    .face(net.minecraft.core.Direction.SOUTH).texture("#overlay_side")
                    .end();
            tDynamite.element() // west
                    .from(-0.01F, 0.0F, 5.0F).to(0.0F, 16.0F, 11.0F)
                    .face(net.minecraft.core.Direction.WEST).texture("#overlay_side")
                    .end();
            tDynamite.element() // east
                    .from(16.0F, 0.0F, 5.0F).to(16.01F, 16.0F, 11.0F)
                    .face(net.minecraft.core.Direction.EAST).texture("#overlay_side")
                    .end();
        }
        // the electrode thirteen (task press-electrodes, MultiItemTechnological.java:488-500
        // metas 29987-29999) — walked over the GT6Electrodes registry rows so the model face
        // cannot drift from the registered ids; single-layer models over the borrowed
        // pre-coloured multiitem sprites (the plain-item form — no tint seam)
        for (gregtech6.registry.GT6Electrodes.ElectrodeRow tRow : gregtech6.registry.GT6Electrodes.ROWS) {
            withExistingParent(tRow.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/electrode/" + tRow.path()));
        }
        // the circuit-chain 53 (task circuit-chain-items, MultiItemTechnological.java:546-770)
        // — walked over the GT6CircuitChain registry rows so the model face cannot drift from
        // the registered ids; single-layer models over the borrowed pre-coloured multiitem
        // sprites (the plain-item form — no tint seam)
        for (gregtech6.registry.GT6CircuitChain.ChainRow tRow : gregtech6.registry.GT6CircuitChain.ROWS) {
            withExistingParent(tRow.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/circuit_chain/" + tRow.path()));
        }
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
                .predicate(ResourceLocation.fromNamespaceAndPath("gt6", "config"), i)
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
            .texture("layer1", modLoc("item/overlay/empty"));
        withExistingParent("bending_cylinder", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/bending_cylinder"))
            .texture("layer1", modLoc("item/overlay/empty"));
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
        // the basin/crossing raw clay items (task material-mc-c-crucible-rows;
        // GT6Crucibles.BASIN_CERAMIC_RAW / CROSSING_CERAMIC_RAW) — the clay_crucible_raw
        // row shape; layer0 = the byte-identical gt.multiitem.randomtools/990.png and
        // /993.png borrows (meta 990 = "Clay Basin" :114, meta 993 = "Clay Crossing" :117 —
        // assets/README.md attribution).
        withExistingParent("basin_ceramic_raw", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/basin_ceramic_raw"));
        withExistingParent("crossing_ceramic_raw", mcLoc("item/generated"))
            .texture("layer0", modLoc("item/crossing_ceramic_raw"));
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
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
        withExistingParent("sense", mcLoc("item/handheld"))
            .texture("layer0", modLoc("item/material_sets/metallic/tool_head_sense"))
            .texture("layer1", modLoc("item/overlay/empty"))
            .texture("layer2", modLoc("item/material_sets/wood/stick"))
            .texture("layer3", modLoc("item/overlay/empty"));
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
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GT6DataGenerators.MOD_ID, aBlockModelPath)));
    }

    /**
     * Task casing-family-3d — the four machine-casing families ride the two-layer tinted
     * cube instead of the flat sprite: the upstream art is the PrefixBlock_ full cube
     * (Loader_PrefixBlocks.java:48-51 — the upstream ITEM was therefore the 3D block
     * form, the vanilla stone-item look), so the {@code item/generated} sprite was the
     * user-reported anti-pattern (known_bugs.r11-casing-item-model). The shape is the
     * {@code GT6BlockStates.partModel} body+decal grammar over the material_sets sprites:
     * the tintindex-0 body cube ({@code MaterialPrefixItem.tintColor} serves the material
     * colour on element faces exactly as it did on the generated layer0, the ItemColors
     * registration is per-ITEM so it covers element-face tint indexes unchanged) + the
     * six 0.01 un-tinted overlay decals (the shared ItemColor -1 arm), cutout for the
     * transparent decal shells. particle = the body sprite — this walk adds no new
     * missingno break-particle models (the r11-machine-particle-key lesson; partModel
     * itself predates it and stays append-only on the mains/rod domain).
     * <p>ponytail: duplicated from partModel rather than extracted — two providers, two
     * call sites, one shape; extract when a third cube-family walk appears.
     */
    private void casingCubeModel(String aItemId, String aSet, String aPrefix) {
        String tBase = "item/material_sets/" + aSet + "/" + aPrefix;
        // task overlay-texture-dedupe: the decal sprite resolves through the content-group
        // canonical (the casing overlay borrows are transparent VOID passes in the empty
        // group) — the decal shell structure stays byte-stable.
        ResourceLocation tOverlay = overlaySpriteOf(aSet, aPrefix);
        ItemModelBuilder tModel = getBuilder(aItemId)
                .parent(getExistingFile(mcLoc("block/cube")))
                .texture("particle", modLoc(tBase))
                .texture("down", modLoc(tBase))
                .texture("up", modLoc(tBase))
                .texture("north", modLoc(tBase))
                .texture("south", modLoc(tBase))
                .texture("west", modLoc(tBase))
                .texture("east", modLoc(tBase));
        if (tOverlay != null) {
            tModel.texture("overlay_bottom", tOverlay)
                  .texture("overlay_top", tOverlay)
                  .texture("overlay_side", tOverlay)
                  .renderType("cutout");
        }
        // element 0 — the body cube (tintindex 0 = the material tint)
        tModel.element()
                .from(0.0F, 0.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).tintindex(0).cullface(aDir))
                .end();
        if (tOverlay != null) {
            // elements 1-6 — the overlay decals (the partModel 0.01-offset form)
            tModel.element() // north
                    .from(0.0F, 0.0F, -0.01F).to(16.0F, 16.0F, 0.0F)
                    .face(net.minecraft.core.Direction.NORTH).texture("#overlay_side").cullface(net.minecraft.core.Direction.NORTH)
                    .end();
            tModel.element() // south
                    .from(0.0F, 0.0F, 16.0F).to(16.0F, 16.0F, 16.01F)
                    .face(net.minecraft.core.Direction.SOUTH).texture("#overlay_side").cullface(net.minecraft.core.Direction.SOUTH)
                    .end();
            tModel.element() // east
                    .from(16.0F, 0.0F, 0.0F).to(16.01F, 16.0F, 16.0F)
                    .face(net.minecraft.core.Direction.EAST).texture("#overlay_side").cullface(net.minecraft.core.Direction.EAST)
                    .end();
            tModel.element() // west
                    .from(-0.01F, 0.0F, 0.0F).to(0.0F, 16.0F, 16.0F)
                    .face(net.minecraft.core.Direction.WEST).texture("#overlay_side").cullface(net.minecraft.core.Direction.WEST)
                    .end();
            tModel.element() // top
                    .from(0.0F, 16.0F, 0.0F).to(16.0F, 16.01F, 16.0F)
                    .face(net.minecraft.core.Direction.UP).texture("#overlay_top").cullface(net.minecraft.core.Direction.UP)
                    .end();
            tModel.element() // bottom
                    .from(0.0F, -0.01F, 0.0F).to(16.0F, 0.0F, 16.0F)
                    .face(net.minecraft.core.Direction.DOWN).texture("#overlay_bottom").cullface(net.minecraft.core.Direction.DOWN)
                    .end();
        }
    }

    /**
     * The twelve LU energium crystals (red/cyan x T0-T5, upstream
     * Loader_MultiTileEntities.java:1078-1092) ride the 3D item model, NOT a flat
     * {@code item/generated} sprite. History: task energium-crystal-appearance-2
     * (2026-10-06) first gave them the faithful 3D form — the upstream ITEM render is
     * RendererBlockTextured.renderInventoryBlock (the FORGE IItemRenderer registered per
     * MTE block, MultiTileEntityRegistry.java:105, RendererBlockTextured.java:50-98)
     * drawing the MTE's own 4x4x4-pixel battery box (setBlockBounds2
     * box(PX_P[6],PX_P[0],PX_P[6],PX_N[6],PX_N[12],PX_N[6]),
     * MultiTileEntityBatteryLU8.java:50; CS.java:492-513) with per-face shading — so the
     * port generated that box as one full-uv element. Task energium-item-2d-gem
     * (2026-10-10): the user rejected that form twice as too small and too square and
     * ruled "还是3D,但是和上游一样的钻石形态" — keep the 3D render, reshape to the
     * DIAMOND-CRYSTAL outline. That is a DECLARED deviation from the upstream box: the
     * upstream material semantics is the diamond set (MT.java:1581-1582 EnergiumRed/Cyan
     * = SET_DIAMOND, the crystalcent item art), while the upstream gem ITEM form is
     * unreachable (GT6_Main.java:138-142 disables gem generation; the in-game MTE item
     * is the box). The silhouette is a stepped rhombic bipyramid: five concentric
     * cuboid elements (2px tips, 4px crown/pavilion, 8px girdle; y 2→14) each rotated
     * 45° about (8,8,8) so corners face the camera — ~11.3px wide x 12px tall, dwarfing
     * the retired 4px box. The family colour stays the BAKED mRGBa multiply
     * (the r11-energium-tint seat, the bake_battery_textures.py products): upstream
     * top/bottom/sides decode pixel-identical, so one sprite per family serves all six
     * faces, and its diagonal gradient reads as facet shading across the rotated faces.
     * cullface-less full-uv faces, particle = the same sprite.
     */
    private void energiumCrystalModel(String aItemId, String aFamily) {
        String tBase = "item/battery/" + aFamily;
        ItemModelBuilder tModel = getBuilder(aItemId)
                .parent(getExistingFile(mcLoc("block/cube")))
                .texture("particle", modLoc(tBase))
                .texture("down", modLoc(tBase))
                .texture("up", modLoc(tBase))
                .texture("north", modLoc(tBase))
                .texture("south", modLoc(tBase))
                .texture("west", modLoc(tBase))
                .texture("east", modLoc(tBase));
        energiumCrystalFacet(tModel, 7.0F, 2.0F, 7.0F, 9.0F, 4.0F, 9.0F);
        energiumCrystalFacet(tModel, 6.0F, 4.0F, 6.0F, 10.0F, 6.0F, 10.0F);
        energiumCrystalFacet(tModel, 4.0F, 6.0F, 4.0F, 12.0F, 10.0F, 12.0F);
        energiumCrystalFacet(tModel, 6.0F, 10.0F, 6.0F, 10.0F, 12.0F, 10.0F);
        energiumCrystalFacet(tModel, 7.0F, 12.0F, 7.0F, 9.0F, 14.0F, 9.0F);
    }

    /** One girdle/cone step of the energium bipyramid: a full-uv box rotated 45° about the cell centre. */
    private void energiumCrystalFacet(ItemModelBuilder aModel, float aX1, float aY1, float aZ1,
            float aX2, float aY2, float aZ2) {
        aModel.element()
                .from(aX1, aY1, aZ1).to(aX2, aY2, aZ2)
                .rotation().origin(8.0F, 8.0F, 8.0F)
                        .axis(net.minecraft.core.Direction.Axis.Y).angle(45.0F).end()
                .allFaces((aDir, aFace) -> aFace.texture("#" + aDir.getName()).uvs(0.0F, 0.0F, 16.0F, 16.0F))
                .end();
    }

    /** The material's item texture-set name, lower-snaked; empty falls back to upstream SET_NONE. */
    public static String iconsetOf(OreDictMaterial material) {
        List<String> tSets = material.mTextureSetsItems;
        return tSets == null || tSets.isEmpty() || tSets.get(0) == null || tSets.get(0).isBlank()
            ? "none"
            : MaterialPrefixItem.snakeCase(tSets.get(0));
    }
}
