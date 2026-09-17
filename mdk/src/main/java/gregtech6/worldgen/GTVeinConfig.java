package gregtech6.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One large-vein row — the {@code WorldgenOresLarge} ctor read face verbatim
 * (WorldgenOresLarge.java:48-67: mMinY/mMaxY/mWeight/mDensity/mDistance/mSize/mIndicatorRocks
 * + the four OreDictMaterial slots :50), plus the two modern table columns the upstream
 * per-dimension registration lists carried ({@code overworld} = the row listed ORE_OVERWORLD —
 * Loader_Worldgen.java:886-916 rows are {@code true}, the :917-925 Mars/End/Moon/BL rows
 * {@code false}; the weighted draw must sum over the dimension's rows only, the
 * GT6WorldGenerator.java:93 tMaxWeight semantics).
 *
 * <p>The material slots hold {@link OreDictMaterial} constants (the datagen rows reference
 * {@code MT.*} directly, so the JSON carries the canonical mNameInternal strings —
 * "Hematite" for MT.Fe2O3 et al., no field-name transcription) and serialize through the
 * full registry name map (MaterialRegistry.java:110 MATERIAL_MAP keyed by mNameInternal);
 * an unknown name decodes to MT.NULL (mID &lt;= 0), which is upstream's own invalid-slot
 * semantics (WorldgenOresLarge.java:80-85 — the per-layer {@code mID > 0} guards :113-126).
 *
 * <p>Row validity for the draw: upstream excludes a vein only when ALL FOUR slots are
 * invalid (mInvalid, :85 + WorldgenObject.java:60); the modern same gate is
 * {@code overworld && >= 1 slot in the GT6OreBlocks.materialAxis() universe} — the axis is
 * what has registrable ore blocks (GT6OreBlocksRegistrationTest pins 53), so slots outside
 * it ride the same per-layer skip and light up as the axis extends. Declared mapping, not
 * a downgrade: the JSON stays canonical either way.
 */
public record GTVeinConfig(String name, int minY, int maxY, int weight, int density, int size,
        int spawnDistance, boolean indicator, boolean overworld,
        OreDictMaterial oreTop, OreDictMaterial oreBottom, OreDictMaterial oreBetween, OreDictMaterial oreSpread)
        implements FeatureConfiguration {

    /** The full-registry name codec — unknown names decode to MT.NULL (the upstream mID &lt;= 0 invalid slot). */
    public static final Codec<OreDictMaterial> MATERIAL_CODEC = Codec.STRING
            .xmap(aName -> MaterialRegistry.INSTANCE.get(aName), aMaterial -> aMaterial.mNameInternal);

    public static final Codec<GTVeinConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("name").forGetter(GTVeinConfig::name),
            Codec.INT.fieldOf("min_y").forGetter(GTVeinConfig::minY),
            Codec.INT.fieldOf("max_y").forGetter(GTVeinConfig::maxY),
            Codec.INT.fieldOf("weight").forGetter(GTVeinConfig::weight),
            Codec.INT.fieldOf("density").forGetter(GTVeinConfig::density),
            Codec.INT.fieldOf("size").forGetter(GTVeinConfig::size),
            Codec.INT.fieldOf("spawn_distance").forGetter(GTVeinConfig::spawnDistance),
            Codec.BOOL.fieldOf("indicator").forGetter(GTVeinConfig::indicator),
            Codec.BOOL.fieldOf("overworld").forGetter(GTVeinConfig::overworld),
            MATERIAL_CODEC.fieldOf("ore_top").forGetter(GTVeinConfig::oreTop),
            MATERIAL_CODEC.fieldOf("ore_bottom").forGetter(GTVeinConfig::oreBottom),
            MATERIAL_CODEC.fieldOf("ore_between").forGetter(GTVeinConfig::oreBetween),
            MATERIAL_CODEC.fieldOf("ore_spread").forGetter(GTVeinConfig::oreSpread))
            .apply(aFields, GTVeinConfig::new));

    /**
     * The configured-feature config: the ONE 40-row vein table (Loader_Worldgen.java:886-925
     * verbatim rows, GT6WorldgenDatagen.LARGE_VEIN_TABLE). Serialized inline in the
     * {@code gt6:large_veins} configured-feature JSON — the tier-a KJS/datapack face (edit
     * weights / add / remove rows without touching Java; the card spec ② "40 脉合一张 JSON 脉表").
     */
    public record Table(List<GTVeinConfig> veins) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GTVeinConfig.CODEC
                .listOf().fieldOf("veins").xmap(Table::new, Table::veins).codec();
    }
}
