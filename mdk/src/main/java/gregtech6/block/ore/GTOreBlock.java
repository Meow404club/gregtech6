package gregtech6.block.ore;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GT6OreBlocks;

/**
 * One material-scoped ore block for an (ore family, form, material) triple (task
 * p30-ore-1-mech). Upstream GT6 bundles one multi-material PrefixBlock per ore family
 * prefix with metadata = material index (Loader_Ores.java:56-128 vanilla anchors,
 * Loader_Rocks.java:57-139 the 17 GT stones); 1.20.1 has no block metadata, so the
 * per-pair shape applies (the GTMaterialPrefixBlock precedent, ADR ruling against an
 * IntegerProperty variant).
 *
 * <p>A PURE block: no EntityBlock, no ticker (upstream ore PrefixBlocks carry no tile
 * entity). Blockstate/model/loot JSONs are the datagen/textures/loot cards' surfaces
 * (p30 wave cards ②③④); between this card and those the blocks exist WITHOUT any
 * generated JSON — the declared intermediate state of GTMaterialBlocks (dedicated
 * servers never bake models, so runServer stays zero-ERROR).
 *
 * <p>Declared deviations (all carried as DATA on {@link gregtech6.registry.GT6OreBlocks.OreFamily}
 * / {@link Form} for the consuming cards, none silent):
 * <ul>
 * <li>the upstream harvest-level-scaled hardness ({@code getBlockHardness = max(1,
 *     base * (1 + level))}, PrefixBlock.java:595) lands as plain
 *     {@code strength(hardness, resistance)} — the GTMaterialPrefixBlock.java:62 form;
 *     the per-pair harvest level rides {@link Form} for the tool-gating card;</li>
 * <li>gravity forms are a separate subclass ({@link GTOreFallingBlock}, vanilla
 *     FallingBlock) — the upstream {@code aGravity} column verbatim;</li>
 * <li>the {@code aEnderDragonProof} column rides {@link GT6OreBlocks.OreFamily#enderProof()}
 *     (endstone = true, Loader_Ores.java:59/:64/:72) for the world-side consumer.</li>
 * </ul>
 */
public class GTOreBlock extends Block {

    /** The oredict prefix this form registers under (normal/broken = the family prefix, small = OP.oreSmall). */
    public final OreDictPrefix prefix;
    /** The material of this particular block (e.g. MT.Iron). */
    public final OreDictMaterial material;
    /** The ore family (stone/deepslate/... 26 rows, the GT6OreBlocks.FAMILIES entry). */
    public final GT6OreBlocks.OreFamily family;
    /** Which of the family's forms this block is (normal / broken / small). */
    public final GT6OreBlocks.FormKind kind;
    /** The form row (the verbatim hardness/resistance/harvest/gravity columns). */
    public final GT6OreBlocks.Form form;

    public GTOreBlock(GT6OreBlocks.OreFamily family, GT6OreBlocks.FormKind kind, GT6OreBlocks.Form form,
            OreDictPrefix prefix, OreDictMaterial material) {
        super(propertiesOreOf(family, form));
        this.family = family;
        this.kind = kind;
        this.form = form;
        this.prefix = prefix;
        this.material = material;
    }

    /**
     * The per-(family, form) BlockBehaviour.Properties, transcribed verbatim from the 26
     * family rows (Loader_Ores.java:56-128, Loader_Rocks.java:57-139 — aBaseHardness,
     * aBaseResistance, aSoundType and the vanilla-Material stand-in MapColor per family).
     * Distinct from GTMaterialPrefixBlock.propertiesOf (the seven storage prefixes, whose
     * throw gate stays untouched — task p30-ore-1-mech spec).
     */
    public static BlockBehaviour.Properties propertiesOreOf(GT6OreBlocks.OreFamily family, GT6OreBlocks.Form form) {
        return BlockBehaviour.Properties.of()
                .mapColor(family.color())
                .strength(form.hardness(), form.resistance())
                .sound(family.sound());
    }
}
