package gregtech6.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * One bedrock-spring row (task fluid-spring spec ②, the springFluid column returned by
 * task issue5-fluid-spring-nozzle) — the {@code WorldgenFluidSpring} ctor read face
 * verbatim (WorldgenFluidSpring.java:50: aName, aDefault, aBlock, aMeta, aProbability,
 * [aIndicatorType,] aSpringFluid), minus the two columns the deferrals cut:
 * <ul>
 * <li><b>mMeta</b> — the 1.7.10 fluid-block meta (bind4(15)) is the legacy per-block
 *     fluid selector; the modern port carries one block per fluid (the fluid id + the
 *     {@code _block} suffix, GTFluids.springBlockId), the column is dead.</li>
 * <li><b>mIndicatorType</b> — the surface grass-indicator arm (WorldgenFluidSpring
 *     .java:82-103) rides the deferred indicator card (card spec ③ "可选 defer（声明
 *     即可）"), exactly the sibling bedrock card's indicator-arm posture.</li>
 * </ul>
 *
 * <p><b>The springFluid column</b> (task issue5-fluid-spring-nozzle): the upstream
 * mSpringFluid FluidStack, the nozzle arm's identity — the port carries the AMOUNT only
 * (one block per fluid makes the fluid identity the row's blockId; the nozzle BE sprays
 * the row's block), null = upstream NF (no nozzle arm, the tInfiniteOil=false face).
 * The port ships the rows at their loader amounts verbatim (Loader_Worldgen.java:782-797:
 * 6000 oils / 3000 gas / 500 geothermal / 1000 lava OW; 2000/1000/250/500 offworld) —
 * the upstream tInfiniteOil/tInfiniteGas=false config gates do not exist in this port,
 * the infinite springs ARE the issue #5 fix. The 1/16-per-position nozzle draws return
 * the ONLY shape randomness of the upstream spring (the dome passes draw nothing) — they
 * ride the caller's coordinate-seeded stream (GT6FluidSpringGenerator.generateDome), so
 * the decision-level determinism holds: same seed + same world = same nozzles.
 *
 * <p>The {@code nether} column (task worldgen-nether-bedrock-lava): the row listed
 * GEN_NETHER — the :797 nether lava dome, the only offworld dimension this port
 * hangs modifiers for; the :789-794 rows carry neither column (census-only until
 * their dim cards).
 *
 * <p>The {@code twilight} column (task twilight-hives-springs): the :795-796
 * GEN_TWILIGHT rows (natural gas 1/200 amount 1000, geothermal water 1/100 amount
 * 250 — the loader args verbatim) activate in the Twilight Forest dimension. The
 * port owns NO TF dimension routing upstream-style (the 1.7.10 GEN_* dim-type
 * registry list), so the mask column is what keeps the routing three-state — the
 * hasCeiling binary alone would roll the OW oil/gas band in TF (TF hangs no
 * ceiling; the trap the column exists for).
 *
 * <p>The {@code atum} column (task atum-dim-adaptation, the twilight-column mask
 * posture grown one more arm): the four :789-792 GEN_ATUM oil rows (1/200, amount
 * 2000 — the loader args verbatim, half the OW 1/400 · 6000 band) activate in the
 * Atum dimension ({@code atum:atum}, the one mod dim with a 1.20.1 carrier). The
 * codec face is {@code optionalFieldOf("atum", false)}: every pre-atum row decodes
 * and re-emits unchanged semantics (the default false), the old JSON stays valid.
 *
 * <p>The probability is the 1/P per-chunk roll (WorldgenFluidSpring.java:62
 * {@code aRandom.nextInt(mProbability) != 0 -> return F}): the OW band rolls
 * oil-extraheavy/heavy/medium/light 1/400 (:782-785), natural gas 1/200 (:786),
 * geothermal water 1/100 (:787), lava 1/200 (:788) — the upstream rows verbatim, in
 * table order, first hit wins (the CAN_GENERATE_BEDROCK_ORE=F claim of :64 blocks every
 * later row — the drawSpring first-hit-wins form; two springs per chunk is impossible,
 * unlike the independent bedrock-ore rows). The offworld rows (:789-796: atum oils,
 * erebus/betweenlands/twilight gas, twilight water) ride the table with
 * {@code overworld=false} — never rolled overworld, the sibling bedrock card's
 * dimension-mask convention (GTBedrockOreConfig); the :797 nether lava row rolls in the
 * nether ({@code nether=true}, the WD.waterLevel hasNoSky face — the dome floor rides
 * the nether's own y=0 bedrock).
 *
 * <p>The block slot is the plain ID STRING (registry-free, the offline test face):
 * {@code gt6:liquid_medium_oil_block} etc. via {@code GTFluids.springBlockId} plus the
 * bare {@code minecraft:lava}. The Feature resolves the live BlockState at place time;
 * an unresolvable id refuses the row (no silent water fallback — upstream's
 * ST.invalid-to-water arm (:54) becomes a loud refusal, the ids are single-sourced from
 * the datagen table over GTFluids.SPRING_BLOCK_IDS and pinned by the parity test).
 */
public record GTFluidSpringConfig(String name, String blockId, int probability, boolean overworld,
        boolean nether, boolean twilight, boolean atum, Integer springFluid) implements FeatureConfiguration {

    /** The legacy 5-arg face (the pre-nether call sites) — nether=false, twilight=false, atum=false, the :782-794 posture. */
    public GTFluidSpringConfig(String aName, String aBlockId, int aProbability, boolean aOverworld, Integer aSpringFluid) {
        this(aName, aBlockId, aProbability, aOverworld, false, false, aSpringFluid);
    }

    /** The nether-card face (the worldgen-nether-bedrock-lava call sites) — twilight=false, atum=false, the :788/:797 posture. */
    public GTFluidSpringConfig(String aName, String aBlockId, int aProbability, boolean aOverworld, boolean aNether, Integer aSpringFluid) {
        this(aName, aBlockId, aProbability, aOverworld, aNether, false, aSpringFluid);
    }

    public static final Codec<GTFluidSpringConfig> CODEC = RecordCodecBuilder.create(aFields -> aFields.group(
            Codec.STRING.fieldOf("name").forGetter(GTFluidSpringConfig::name),
            Codec.STRING.fieldOf("block").forGetter(GTFluidSpringConfig::blockId),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("probability").forGetter(GTFluidSpringConfig::probability),
            Codec.BOOL.fieldOf("overworld").forGetter(GTFluidSpringConfig::overworld),
            Codec.BOOL.optionalFieldOf("nether", false).forGetter(GTFluidSpringConfig::nether),
            Codec.BOOL.optionalFieldOf("twilight", false).forGetter(GTFluidSpringConfig::twilight),
            Codec.BOOL.optionalFieldOf("atum", false).forGetter(GTFluidSpringConfig::atum),
            Codec.INT.optionalFieldOf("springFluid")
                    .xmap(aOpt -> aOpt.orElse(null), java.util.Optional::ofNullable)
                    .forGetter(GTFluidSpringConfig::springFluid))
            .apply(aFields, GTFluidSpringConfig::new));

    /**
     * The configured-feature config: the ONE 16-row bedrock-spring table
     * (Loader_Worldgen.java:782-797 verbatim rows, GT6WorldgenDatagen.FLUID_SPRING_TABLE;
     * the atum oil band rides the atum mask since task atum-dim-adaptation — the
     * erebus/betweenlands/twilight dims stay unported, the nether row rides the mask).
     * Serialized inline in the {@code gt6:fluid_springs} configured-feature JSON — the
     * same tier-a datapack face as the vein/lens/bedrock tables (edit the 1/P columns
     * without touching Java).
     */
    public record Table(List<GTFluidSpringConfig> rows) implements FeatureConfiguration {
        public static final Codec<Table> CODEC = GTFluidSpringConfig.CODEC
                .listOf().fieldOf("rows").xmap(Table::new, Table::rows).codec();
    }
}
