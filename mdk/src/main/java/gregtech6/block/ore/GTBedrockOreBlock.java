package gregtech6.block.ore;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * One material-scoped bedrock-ore block (task p31-bedrock-ore-worldgen): the per-pair
 * split of the upstream single-id pair {@code BlocksGT.oreBedrock} / {@code oreSmallBedrock}
 * (Loader_Ores.java:44-45 — one meta block each, meta = material). Deliberately NOT a
 * {@link GTOreBlock}: the custom ore bake dispatch (GTOreClientListener PARAMS table over
 * {@code GT6OreBlocks.registrationOrder()}) and the atlas overlay stitching never see this
 * class, and the blockstate JSON is a plain bedrock cube — the upstream look IS the plain
 * bedrock texture copy ({@code BlockTextureCopied.get(Blocks.bedrock, 0)}), no ore overlay.
 *
 * <p>The upstream column row verbatim: hardness 3600000F, blast resistance 9999,
 * TOOL_pickaxe quality -1, {@code Drops_None} (player-unmineable; the future bedrock drill
 * reads the blocks in place — 17999 is another card). {@code noLootTable} IS the Drops_None
 * face (a block without a loot table drops nothing). {@code ILLEGAL_DROPS} /
 * {@code GarbageGT.BLACKLIST} ride the same unmineable face for free.
 *
 * <p>The prefix/material fields are the drill-card's data face (the upstream drill reads
 * meta = material; the per-pair split carries them as fields). prefix is OP.oreBedrock on
 * the large form and OP.oreSmall on the small form (upstream verbatim, Loader_Ores.java:44
 * vs :45 — the small block shares the OP.oreSmall prefix, its name segment says bedrock).
 */
public class GTBedrockOreBlock extends Block {

    /** The oredict prefix (OP.oreBedrock large / OP.oreSmall small, upstream verbatim). */
    public final OreDictPrefix prefix;
    /** The material of this particular block (the upstream meta axis, split per-pair). */
    public final OreDictMaterial material;
    /** False = the large bedrock ore, true = the small bedrock ore (the 1:2 muffin ratio face). */
    public final boolean small;

    public GTBedrockOreBlock(OreDictPrefix aPrefix, OreDictMaterial aMaterial, boolean aSmall) {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3600000.0F, 9999.0F) // Loader_Ores.java:44-45 — 3600000F hardness / 9999 resistance
                .sound(SoundType.STONE)        // upstream Block.soundTypePiston — the modern STONE stand-in
                .noLootTable());               // Drops_None verbatim
        this.prefix = aPrefix;
        this.material = aMaterial;
        this.small = aSmall;
    }
}
