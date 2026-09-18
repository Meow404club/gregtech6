package gregtech6.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One bedrock-ore row (task p31-bedrock-ore-worldgen spec ②) — the {@code WorldgenOresBedrock}
 * ctor read face verbatim (WorldgenOresBedrock.java:61-65: aName, aProbability, aPrimary;
 * the indicator columns ride the deferred-decor deviation below), plus the one modern table
 * column the upstream per-dimension registration lists carried ({@code overworld} = the row
 * listed GEN_FLOOR, Loader_Worldgen.java:725-757 — true; the nether/mars/BL rows :758-770
 * false; the per-chunk independent rolls only walk the dimension's own rows, the upstream
 * per-object iteration semantics).
 *
 * <p>The probability is the 1/P per-chunk roll (WorldgenOresBedrock.java:142
 * {@code aRandom.nextInt(mProbability) != 0 -> return F}): diamond 128000 .. cassiterite
 * 2000, the P columns verbatim. Rows roll INDEPENDENTLY in table order on the one
 * chunk-seeded stream — NOT a weighted exactly-one draw: upstream runs one WorldgenObject
 * per row, each with its own gate, so two rows CAN hit one chunk (the rarer the tail, the
 * closer to exactly-one; the OW sum ~0.5%/chunk).
 *
 * <p>The material slot holds an {@link OreDictMaterial} constant (the GTVeinConfig face):
 * the datagen rows reference {@code MT.*} directly, the JSON carries the canonical
 * mNameInternal strings, an unknown name decodes to MT.NULL (mID <= 0) = upstream's own
 * invalid-slot semantics (WorldgenOresBedrock.java:111-113).
 *
 * <p>Declared deviation (the spec ①-④ scope): the indicator arm (rocks MTE 32757 +
 * BlocksGT.FlowersA/B, WorldgenOresBedrock.java:147-177) is NOT ported — every row is
 * mIndicatorRocks=T upstream so the column would be constant, the flowers universe is not
 * ported, and the surface-rock channel carries only the 31 vein materials (the fallback
 * face would mislabel the other 14). The findability face returns with the flowers/surface
 * cards; ponytail: the arm is ~25 lines reusing the GT6VeinGenerator.SliceSink probe shape
 * when it does.
 */
public record GTBedrockOreConfig(String name, OreDictMaterial material, int probability, boolean overworld)
        implements FeatureConfiguration {

    /** The full-registry name codec — unknown names decode to MT.NULL (the upstream invalid slot). */
    public static final Codec<OreDictMaterial> MATERIAL_CODEC = Codec.STRING
            .xmap(aName -> MaterialRegistry.INSTANCE.get(aName), aMaterial -> aMaterial.mNameInternal);

    public static final Codec<GTBedrockOreConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("name").forGetter(GTBedrockOreConfig::name),
            MATERIAL_CODEC.fieldOf("ore").forGetter(GTBedrockOreConfig::material),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("probability").forGetter(GTBedrockOreConfig::probability),
            Codec.BOOL.fieldOf("overworld").forGetter(GTBedrockOreConfig::overworld))
            .apply(aFields, GTBedrockOreConfig::new));

    /**
     * The configured-feature config: the ONE 46-row bedrock-ore table
     * (Loader_Worldgen.java:725-770 verbatim rows, GT6WorldgenDatagen.BEDROCK_ORE_TABLE;
     * the :772 hexorium row rides the MD.HEX mod-gated compat pool). Serialized inline in
     * the {@code gt6:bedrock_ores} configured-feature JSON — the same tier-a datapack face
     * as the vein/lens tables (edit the 1/P columns without touching Java).
     */
    public record Table(List<GTBedrockOreConfig> rows) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GTBedrockOreConfig.CODEC
                .listOf().fieldOf("rows").xmap(Table::new, Table::rows).codec();
    }
}
