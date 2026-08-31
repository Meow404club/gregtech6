package gregtech6.block.material;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * One material-scoped storage block for an OreDictPrefix x OreDictMaterial pair (task
 * p8-prefixblock-registry). Upstream GT6 bundles one multi-material PrefixBlock per prefix
 * with metadata = material index (gregapi/block/prefixblock/PrefixBlock.java:171-172); 1.20.1
 * has no block metadata, so the per-pair shape applies (the GTBarrels per-row precedent, and
 * the ADR ruling against an IntegerProperty variant — one blockstate JSON per pair instead
 * of a thousand-variant file).
 *
 * <p>A PURE block: no EntityBlock, no ticker, no BlockEntity — upstream
 * {@code PrefixBlock.createNewTileEntity} returns {@code null} unconditionally
 * (PrefixBlock.java:584). No {@code onRemove} override either (project lesson id59).
 * Blockstate/model/loot JSONs live in the datagen providers (p8-prefixblock-render);
 * this class only carries the client tint colour seam ({@link #blockColor()} /
 * {@link #tintARGB()}, upstream PrefixBlock.java:279-282). Declared deviation: the
 * upstream per-prefix
 * {@code aGravity} flag (blockDust falls, Loader_PrefixBlocks.java:42 arg 1 = T) and the
 * harvest tool/level gating are not ported on this pure block — vanilla 1.20.1 models
 * gravity through FallingBlock and tool gating through loot/hooks, both deferred to the
 * render/loot card; the vanilla {@code Material} enum is gone in 1.20.1, its observable
 * carryover (strength/sound) is ported verbatim.
 */
public class GTMaterialPrefixBlock extends Block {

    /** The oredict prefix this block family serves (e.g. OP.blockIngot). */
    public final OreDictPrefix prefix;
    /** The material of this particular block (e.g. MT.Coal). */
    public final OreDictMaterial material;

    public GTMaterialPrefixBlock(OreDictPrefix prefix, OreDictMaterial material) {
        super(propertiesOf(prefix));
        this.prefix = prefix;
        this.material = material;
    }

    /**
     * The per-prefix BlockBehaviour.Properties, transcribed verbatim from the seven upstream
     * registration rows (Loader_PrefixBlocks.java:40-46 — aBaseHardness, aBaseResistance and
     * aSoundType per line; nothing invented):
     * <ul>
     * <li>:40 blockRaw     — rock/gravel family: strength 1.5F, 4.5F, GRAVEL</li>
     * <li>:41 blockGem     — rock/stone family:  strength 1.5F, 4.5F, STONE</li>
     * <li>:42 blockDust    — sand family:         strength 0.5F, 4.5F, SAND</li>
     * <li>:43 blockIngot   — iron family:         strength 1.0F, 3.0F, METAL</li>
     * <li>:44 blockPlate   — iron family:         strength 1.0F, 3.0F, METAL</li>
     * <li>:45 blockPlateGem— iron family:         strength 1.0F, 3.0F, METAL</li>
     * <li>:46 blockSolid   — iron family:         strength 1.7F, 5.0F, METAL</li>
     * </ul>
     * The MapColor stand-in per upstream Material (rock/sand/iron) keeps map behaviour sane;
     * anything outside the seven storage prefixes is a caller bug and fails loudly.
     */
    public static BlockBehaviour.Properties propertiesOf(OreDictPrefix prefix) {
        if (prefix == OP.blockRaw) return props(1.5F, 4.5F, SoundType.GRAVEL, MapColor.STONE);
        if (prefix == OP.blockGem) return props(1.5F, 4.5F, SoundType.STONE, MapColor.STONE);
        if (prefix == OP.blockDust) return props(0.5F, 4.5F, SoundType.SAND, MapColor.SAND);
        if (prefix == OP.blockIngot) return props(1.0F, 3.0F, SoundType.METAL, MapColor.METAL);
        if (prefix == OP.blockPlate) return props(1.0F, 3.0F, SoundType.METAL, MapColor.METAL);
        if (prefix == OP.blockPlateGem) return props(1.0F, 3.0F, SoundType.METAL, MapColor.METAL);
        if (prefix == OP.blockSolid) return props(1.7F, 5.0F, SoundType.METAL, MapColor.METAL);
        throw new IllegalArgumentException("not an upstream storage-block prefix: " + prefix.mNameInternal
                + " (Loader_PrefixBlocks.java:40-46 defines exactly seven)");
    }

    private static BlockBehaviour.Properties props(float hardness, float resistance, SoundType sound, MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance).sound(sound);
    }

    /**
     * The client {@code BlockColor} for every material prefix block (task p8-prefixblock-render
     * spec ③), registered once over the whole block array in GTClientHandlers — the world-side
     * half of the tint. Upstream colours the block render pass as
     * {@code UT.Code.getRGBInt(aMaterial.fRGBa[mPrefix.mState])} (PrefixBlock.java:279-282,
     * getRenderColor verbatim colour pick); every block* prefix has {@code mState == STATE_SOLID}
     * (= 0, OreDictPrefix.java:104 port), so the picked colour is {@code fRGBaSolid}.
     *
     * <p>Per the Forge docs this does NOT colour the BlockItem — the inventory half is the
     * separate {@code ItemColor} (GTMaterialPrefixBlockItem.tintColor) registered over the
     * block items in the same handler class.
     */
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static net.minecraft.client.color.block.BlockColor blockColor() {
        return (aState, aLevel, aPos, aTintIndex) -> {
            if (aTintIndex == 0 && aState.getBlock() instanceof GTMaterialPrefixBlock tBlock) return tBlock.tintARGB();
            return -1;
        };
    }

    /** The shared ARGB tint colour of this block: {@code fRGBa[prefix.mState]} (PrefixBlock.java:279-282), UT.Code.getRGBInt :1580-1582 encoding. */
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public int tintARGB() {
        short[] tRGBa = material.fRGBa[prefix.mState];
        return 0xFF000000 | (bind8(tRGBa[0]) << 16) | (bind8(tRGBa[1]) << 8) | bind8(tRGBa[2]); // UT.Code.getRGBInt, UT.java:1580-1582
    }

    /** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
    private static int bind8(long aValue) {
        return (int)Math.max(0, Math.min(255, aValue));
    }
}
