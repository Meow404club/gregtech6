package gregtech6.worldgen;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One vanilla-water replacement row (task worldgen-water-replace) — the Loader_Worldgen
 * .java:576-578 triple read face:
 * <ul>
 * <li>{@code ocean}  — WorldgenOcean("ocean.seawater")  :576, vanilla water → gt6:seawater</li>
 * <li>{@code river}  — WorldgenRiver("river.riverwater"):577, vanilla water → gt6:riverwater</li>
 * <li>{@code swamp}  — WorldgenSwamp("swamp.dirtywater"):578, vanilla water + the prior
 *     ocean/river bodies in swamp columns → gt6:waterdirty</li>
 * </ul>
 * The rows ride the ONE {@code gt6:water_replace} table IN THIS ORDER — the upstream
 * registration order IS the hard constraint ("OCEAN must generate before RIVER and SWAMP;
 * RIVER after OCEAN and before SWAMP; SWAMP after RIVER and OCEAN", Loader_Worldgen.java
 * :575-578): the swamp arm converts the ocean/river block faces the earlier rows placed,
 * which only exists if the table iterates ocean → river → swamp (the Feature's per-chunk
 * loop is the order carrier, immune to biome-modifier application order — the
 * fluid-spring replay-seam posture).
 *
 * <p><b>The gate column</b> — the chunk-granular biome gates, upstream verbatim
 * (WorldgenOcean.java:54 / WorldgenRiver.java:54 / WorldgenSwamp.java:55 over
 * {@code aBiomeNames}):
 * <ul>
 * <li>{@code ocean} — the chunk carries ANY ocean biome (upstream BIOMES_OCEAN,
 *     CS.java:251; the modern face is {@code #minecraft:is_ocean} — the vanilla member set
 *     plus the warm/cold/lukewarm splits).</li>
 * <li>{@code river} — the chunk carries a river biome AND carries NO ocean biome
 *     (WorldgenRiver.java:54 verbatim compound; upstream BIOMES_RIVER, CS.java:248 →
 *     {@code #minecraft:is_river}).</li>
 * <li>{@code swamp} — the chunk carries a swamp biome (upstream BIOMES_SWAMP,
 *     CS.java:263). No vanilla biome tag exists for swamps (the 1.20.1 tag dir has no
 *     {@code is_swamp}), so the port pins the two vanilla swamp-family biomes
 *     (minecraft:swamp + minecraft:mangrove_swamp — the modern swampland split; 1.7.10
 *     predates mangrove). The upstream mod-biome names (RWG/BiomesOPlenty rows in all
 *     three sets) have no modern identity — CUT, declared.</li>
 * </ul>
 *
 * <p><b>The scanTop column</b> — the upstream per-class config knob ("Height",
 * WorldgenOcean.java:48 default {@code WD.waterLevel()} = 62, the top water block y; the
 * modern sea level is 63 = the first air y, so the overworld rows ride the 62 default).
 * The scan walks each column DOWN from scanTop and STOPS at the first occluding block
 * (WorldgenOcean.java:64 {@code isOpaqueCube → break}, the port face
 * {@code BlockState.canOcclude()} — the GT6LargeVeinFeature:128 probe form): only the
 * surface water body converts, caves/aquifer water below the floor stay vanilla — the
 * upstream throttle, kept verbatim.
 *
 * <p><b>The block slot</b> — the plain ID STRING (the GTFluidSpringConfig face): the
 * target LiquidBlock id via {@code GTFluids.springBlockId} (gt6:seawater_block /
 * gt6:riverwater_block / gt6:waterdirty_block, task worldgen-water-replace's
 * WATER_REPLACE_BLOCK_IDS block face). The Feature resolves the live BlockState at place
 * time; an unresolvable id refuses the row (the spring loud-refusal face, no silent
 * water fallback).
 */
public record GT6WaterReplaceConfig(String name, String blockId, Gate gate, int scanTop)
        implements FeatureConfiguration {

    /** The three chunk-gate kinds (see the class javadoc for the upstream set identities). */
    public enum Gate {
        OCEAN("ocean"), RIVER("river"), SWAMP("swamp");

        /** The lowercase JSON spelling (the row-table face). */
        public final String lower;

        Gate(String aLower) {
            lower = aLower;
        }

        /** The parse face — an unknown string trips the codec (loud, the fluid-spring posture). */
        public static final Codec<Gate> CODEC = Codec.STRING
                .xmap(Gate::parse, Gate::json);

        private static Gate parse(String aName) {
            return Gate.valueOf(aName.toUpperCase(Locale.ROOT));
        }

        private String json() {
            return lower;
        }
    }

    public static final Codec<GT6WaterReplaceConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("name").forGetter(GT6WaterReplaceConfig::name),
            Codec.STRING.fieldOf("block").forGetter(GT6WaterReplaceConfig::blockId),
            Gate.CODEC.fieldOf("gate").forGetter(GT6WaterReplaceConfig::gate),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("scanTop", 62)
                    .forGetter(GT6WaterReplaceConfig::scanTop))
            .apply(aFields, GT6WaterReplaceConfig::new));

    /**
     * The configured-feature config: the ONE 3-row vanilla-water table
     * (Loader_Worldgen.java:576-578 verbatim order — the ordering constraint lives in the
     * table, see the class javadoc). Serialized inline in the {@code gt6:water_replace}
     * configured-feature JSON — the same tier-a datapack face as the vein/lens/bedrock/
     * spring tables (edit the rows without touching Java).
     */
    public record Table(List<GT6WaterReplaceConfig> rows) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GT6WaterReplaceConfig.CODEC
                .listOf().fieldOf("rows").xmap(Table::new, Table::rows).codec();
    }
}
