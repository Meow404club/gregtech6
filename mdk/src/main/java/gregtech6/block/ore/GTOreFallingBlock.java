package gregtech6.block.ore;

import net.minecraft.world.level.block.FallingBlock;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GT6OreBlocks;

/**
 * The gravity forms of the ore universe (task p30-ore-1-mech): every family row whose
 * upstream {@code aGravity} column is true (Loader_Ores.java:56-59 the broken stone-likes,
 * :65-67 gravel/sand/redSand, :121-122 mud, :73-75 their small ores; Loader_Rocks.java:58
 * et seq. every GT stone's broken ore) becomes a vanilla FallingBlock — 1.20.1 models
 * gravity through {@link FallingBlock} + the shared vanilla FallingBlockEntity (no per-block
 * entity registration; FallingBlock.java:19 takes plain Properties). Java's single
 * inheritance splits the hierarchy here: the row data (family/kind/form/prefix/material)
 * mirrors {@link GTOreBlock}, the properties come from the shared
 * {@link GTOreBlock#propertiesOreOf}.
 */
public class GTOreFallingBlock extends FallingBlock {

    /** The oredict prefix this form registers under (normal/broken = the family prefix, small = OP.oreSmall). */
    public final OreDictPrefix prefix;
    /** The material of this particular block. */
    public final OreDictMaterial material;
    /** The ore family (the GT6OreBlocks.FAMILIES entry). */
    public final GT6OreBlocks.OreFamily family;
    /** Which of the family's forms this block is. */
    public final GT6OreBlocks.FormKind kind;
    /** The form row (the verbatim hardness/resistance/harvest/gravity columns). */
    public final GT6OreBlocks.Form form;

    public GTOreFallingBlock(GT6OreBlocks.OreFamily family, GT6OreBlocks.FormKind kind, GT6OreBlocks.Form form,
            OreDictPrefix prefix, OreDictMaterial material) {
        super(GTOreBlock.propertiesOreOf(family, form));
        this.family = family;
        this.kind = kind;
        this.form = form;
        this.prefix = prefix;
        this.material = material;
    }

    //? if neoforge {
    /*
    // 21.1 made FallingBlock.codec() abstract (the vanilla 1.21 block-state codec
    // dispatch). The simpleCodec representative-value form is the GTOvenBlock codec
    // note verbatim — a parse-time default carrying no live config; world save/load
    // never runs through this codec (the registry-id + property mapper does).
    @Override
    protected com.mojang.serialization.MapCodec<? extends GTOreFallingBlock> codec() {
        return simpleCodec(aProperties -> new GTOreFallingBlock(GT6OreBlocks.FAMILIES.get(0),
                GT6OreBlocks.FormKind.NORMAL, GT6OreBlocks.FAMILIES.get(0).normal(),
                gregapi.data.OP.oreVanillastone, gregapi.data.MT.Stone));
    }
    *///?}
}
