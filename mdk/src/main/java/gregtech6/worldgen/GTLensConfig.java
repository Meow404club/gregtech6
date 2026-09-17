package gregtech6.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One strata-lens row (task p31-strata-lens) — the {@link GTVeinConfig} isomorphic row
 * face: the marker stone, its rarity (the weight of the exactly-one origin draw) and the
 * Y domain (the CENTER-y range, nextInt bound = maxY - minY + 1 &gt; 0), plus the freshly
 * calibrated shape pair (horizontal radius + vertical half-height of the flattened-blob
 * ellipsoid). The conflict-audit ORE_SIZE lesson: these are CLEAN numbers chosen for this
 * card (documented below), no P30 blob/small-ore calibration curve is reused.
 *
 * <p>Field bounds: {@code radius} rides {@code Codec.intRange(1, 48)} — the cap IS the
 * cross-chunk scan-safety rail (radius 48 + a center anywhere in the origin chunk = reach
 * &lt; 64 blocks = 4 chunks, so the Feature's ±3-chunk origin window sees every origin
 * whose lens can touch the work chunk). {@code halfHeight} and the Y domain are free ints
 * (out-of-world writes are clipped by the host-gate sink, the same silent-false semantics
 * the research pinned for the vein). {@code rarity} &gt; 0 (a 0-weight row would never
 * draw and is rejected by the codec).
 *
 * <p>The stone column is the GTStoneBlocks snake ({@code "marble"}..) resolved through
 * {@code GTStoneBlocks.block(snake, StoneVariant.STONE)} — the bare-variant-0 id scheme
 * (GTStoneBlocks.path, ADR ruling ②). A datapack row naming an unknown snake decodes fine
 * and its lens simply never places (the Feature resolves the block at use time, null =
 * skip, the same invalid-row posture as GTVeinConfig's MT.NULL slots).
 *
 * <p>Zero new blocks: all five marker stones are the 272-block GTStoneBlocks universe
 * (registration increment zero, the card spec).
 */
public record GTLensConfig(String stone, int rarity, int minY, int maxY, int radius, int halfHeight)
        implements FeatureConfiguration {

    /** The horizontal radius cap — 48 + the 16-block center spread stays under the 4-chunk reach the ±3 scan window covers. */
    public static final int RADIUS_CODEC_CAP = 48;

    public static final Codec<GTLensConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("stone").forGetter(GTLensConfig::stone),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("rarity").forGetter(GTLensConfig::rarity),
            Codec.INT.fieldOf("min_y").forGetter(GTLensConfig::minY),
            Codec.INT.fieldOf("max_y").forGetter(GTLensConfig::maxY),
            Codec.intRange(1, RADIUS_CODEC_CAP).fieldOf("radius").forGetter(GTLensConfig::radius),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("half_height").forGetter(GTLensConfig::halfHeight))
            .apply(aFields, GTLensConfig::new));

    /**
     * The configured-feature config: the 5-row marker-stone table (the card spec's settled
     * list, in spec order) serialized inline in the {@code gt6:strata_lenses}
     * configured-feature JSON — the same tier-a datapack face as the vein table (edit
     * rarity/shape/Y without touching Java; the KJS card declaration: datapack domain,
     * naturally scriptable, zero adaptation).
     */
    public record Table(List<GTLensConfig> lenses) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GTLensConfig.CODEC
                .listOf().fieldOf("lenses").xmap(Table::new, Table::lenses).codec();
    }
}
