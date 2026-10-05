package gregtech6.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One bedrock-ore row (task bedrock-ore-worldgen spec ②) — the {@code WorldgenOresBedrock}
 * ctor read face verbatim (WorldgenOresBedrock.java:61-65: aName, aProbability, aPrimary;
 * the indicator columns ride the deferred-decor deviation below), plus the modern table
 * columns the upstream per-dimension registration lists carried ({@code overworld} = the row
 * listed GEN_FLOOR, Loader_Worldgen.java:725-757; {@code nether} = the row listed GEN_NETHER,
 * the seven rows :758-764, task worldgen-nether-bedrock-lava; the mars/BL rows :765-770 carry
 * neither — census-only until their dim cards hang modifiers; the per-chunk independent rolls
 * only walk the dimension's own rows, the upstream per-object iteration semantics).
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
 * <p>The flower slot is the optional indicator-flower column (task worldgen-flower-arm;
 * WorldgenOresBedrock.java:69 the {@code aFlower}/{@code aFlowerMeta} ctor pair): the JSON
 * carries the GT6SurfaceBlocks.FLOWER_SPECS snake id (the port's one-id-per-meta flower
 * form — FlowersA meta m = spec m, FlowersB meta m = spec 10+m), "" = the upstream NB
 * slot (the offworld rows :758-770 and the hexorium row's absence). The arm itself:
 * {@link GT6BedrockOreGenerator#generateFlowers} (the ring math) + the
 * {@link GT6BedrockOreFeature} live scan (the block lookup + canSurvive). DECLARED
 * DEVIATIONS: the indicator ROCKS arm (MTE 32757) stays out — the surface-rock channel
 * carries only the 31 vein materials, the other 14 would mislabel (the prior javadoc's
 * established deferral); the flowers therefore always try first (the upstream
 * {@code !tRocks} branch, :162 — no nextInt(4) draw consumed), density marginally above
 * upstream; the wasteland (:150, CS.java:293) and streets (:147) exclusions are dead
 * clauses in the port (no GT biomes/streets) — CUT.
 */
public record GTBedrockOreConfig(String name, OreDictMaterial material, int probability, boolean overworld,
        boolean nether, String flower) implements FeatureConfiguration {

    /** The legacy 4-arg face (the pre-nether/pre-flower call sites) — nether=false, "" flower, the :765-770 posture. */
    public GTBedrockOreConfig(String aName, OreDictMaterial aMaterial, int aProbability, boolean aOverworld) {
        this(aName, aMaterial, aProbability, aOverworld, false, "");
    }

    /** The full-registry name codec — unknown names decode to MT.NULL (the upstream invalid slot). */
    public static final Codec<OreDictMaterial> MATERIAL_CODEC = Codec.STRING
            .xmap(aName -> MaterialRegistry.INSTANCE.get(aName), aMaterial -> aMaterial.mNameInternal);

    public static final Codec<GTBedrockOreConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("name").forGetter(GTBedrockOreConfig::name),
            MATERIAL_CODEC.fieldOf("ore").forGetter(GTBedrockOreConfig::material),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("probability").forGetter(GTBedrockOreConfig::probability),
            Codec.BOOL.fieldOf("overworld").forGetter(GTBedrockOreConfig::overworld),
            Codec.BOOL.optionalFieldOf("nether", false).forGetter(GTBedrockOreConfig::nether),
            Codec.STRING.optionalFieldOf("flower", "").forGetter(GTBedrockOreConfig::flower))
            .apply(aFields, GTBedrockOreConfig::new));

    /**
     * The configured-feature config: the ONE 46-row bedrock-ore table
     * (Loader_Worldgen.java:725-770 verbatim rows, GT6WorldgenDatagen.BEDROCK_ORE_TABLE;
     * the :772 hexorium row rides the MD.HEX mod-gated compat pool). Serialized inline in
     * the {@code gt6:bedrock_ores} configured-feature JSON — the same tier-a datapack face
     * as the vein/lens tables (edit the 1/P columns without touching Java).
     *
     * <p>Driver-face interlink (mdh-4 closeout): the MD.HEX row cut is port-time static
     * history — the unified mod-driver face is GT6ModDrivers (mdh series;
     * isLoaded/visibilityGate); HEX is an mdh-2 atlas (GT6ForeignMaterialAtlas) takeover
     * candidate.
     */
    public record Table(List<GTBedrockOreConfig> rows) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GTBedrockOreConfig.CODEC
                .listOf().fieldOf("rows").xmap(Table::new, Table::rows).codec();
    }
}
