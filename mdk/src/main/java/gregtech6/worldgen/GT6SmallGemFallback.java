package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

/**
 * The RANDOM_SMALL_GEM_ORE 1% stone-layer fallback core (task small-gem-1pct) — the
 * vanilla-free half of the upstream {@code WorldgenStoneLayers.java:112-116/:165-169}
 * face: when a stone-layer block is about to be placed and the position holds no layer
 * ore, {@code nextInt(100) == 0} swaps in a small-ore block ({@code ore_small} form) of
 * a random {@code TD.Properties.RANDOM_SMALL_GEM_ORE} pool member — the layer-boundary
 * gem sprinkle.
 *
 * <p><b>The pool</b> (upstream {@code Loader_Worldgen.java:876-880} verbatim shape): a
 * MATERIAL_ARRAY walk over the {@code RANDOM_SMALL_GEM_ORE} flag — 61 members (the r7-b
 * census, research.r7-b-gem-pool pool_exact_61), ascending mID = the upstream declaration
 * order, alias-merged and id-gated exactly like the dungeon-loot sister walk
 * ({@code GT6LootInjectionDatagen.randomSmallGemLoop}, the upstream Loader_Loot:122-126
 * twin). Every member sits on {@code GT6OreBlocks.materialAxis()} (122) since
 * b-gem-pool-extension, so the small-ore block faces exist for all of them.
 *
 * <p><b>The sequence</b> (the card's verbatim pin): one {@code nextInt(100)} per candidate
 * stone block — consumed only when the switch is on and the pool is non-empty (the
 * upstream short-circuit order: a disabled/empty pool spends no draw) — then one
 * {@code nextInt(pool.size())} pick on a hit. Upstream picks the member via
 * {@code UT.Code.select(MT.Emerald, pool)} on the global RNGSUS (UT.java:1464-1465); this
 * port picks on the same injected stream — seeded and replayable, the declared
 * strengthening over the unseeded global.
 *
 * <p><b>The switch</b>: upstream is the per-material config
 * {@code ConfigsGT.WORLDGEN.get("ore.random_small_gem_ores", mNameInternal, T)} (:879,
 * all-default-T); the port has no config framework, so one boolean stands in for the whole
 * default-T category (the declared deviation — flip it and the fallback is a no-op that
 * consumes no draw, byte-identical stone placement).
 *
 * <p><b>The host face and the three-branch order</b>: upstream places the CURRENT layer's
 * small-ore metatile ({@code tScan[3].mOreSmall.placeBlock(..., select(...).mID, ...)}),
 * i.e. the gem ore block carries the stone base of the stone it replaces; the Feature side
 * resolves that via {@code GT6OreBlocks.stoneToOreFamilies()} + {@code FormKind.SMALL}
 * (the GT6BedrockOreFeature.ore host-skin form). The layer-ore-first arm of the upstream
 * order is the ore_lens companion chain — JSON features riding the biome modifier AFTER
 * the lens feature (the GT6LensOreTest FeatureSorter chain pin); ore blocks are not
 * stone-ore-replaceable, so a companion never overwrites a placed gem and the gem roll
 * never pre-empts a companion (mutual exclusion holds in both orders). Upstream gates the
 * roll on the layer-boundary rows ({@code tScan[4] != tScan[2]}); the port lens body is
 * wholly transition material — a distinct stone mass embedded in vanilla stone — so every
 * lens stone block rolls (the declared density deviation; over the lens lattice the global
 * gem rate stays in the upstream per-chunk order).
 *
 * <p><b>Scope</b>: the hook rides {@code GT6StrataLensFeature.placeSlice} — the r6 strata
 * lens chain, the port's WorldgenStoneLayers counterpart (the 5 marker-stone lens rows
 * whose companion ores r6-c3 transcribed). The 12 JSON blob stones have no code placement
 * point (vanilla OreFeature); no post-placement processor was invented for them.
 */
public final class GT6SmallGemFallback {

	/** The upstream "ore.random_small_gem_ores" category as one switch, all-default-T (the class javadoc deviation note). */
	public static boolean RANDOM_SMALL_GEM_ORES = true;

	/** The lazily-built pool (materials init long precedes the first worldgen use; tests boot first too). */
	private static List<OreDictMaterial> sPool;

	private GT6SmallGemFallback() {
	}

	/** The pool: the flagged MATERIAL_ARRAY walk, ascending mID (the upstream declaration order). */
	public static List<OreDictMaterial> pool() {
		List<OreDictMaterial> rPool = sPool;
		if (rPool == null) {
			rPool = new ArrayList<>(61);
			for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
				if (tMaterial == null || tMaterial.mID < 0 || !tMaterial.contains(TD.Properties.RANDOM_SMALL_GEM_ORE)) continue;
				tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge, MaterialRegistry.java:182-185
				if (tMaterial == null || tMaterial.mID < 0) continue;
				rPool.add(tMaterial);
			}
			sPool = List.copyOf(rPool);
		}
		return rPool;
	}

	/**
	 * The 1% roll: {@code nextInt(100) == 0} → one pool pick, else null (plain stone).
	 * Null on switch-off/empty-pool with ZERO draws consumed (the upstream short-circuit
	 * order); the pick draw is consumed only on a hit.
	 */
	public static OreDictMaterial roll(Random aRandom) {
		if (!RANDOM_SMALL_GEM_ORES) return null;
		List<OreDictMaterial> tPool = pool();
		if (tPool.isEmpty()) return null;
		if (aRandom.nextInt(100) != 0) return null;
		return tPool.get(aRandom.nextInt(tPool.size())); // the UT.Code.select pick, seeded port form
	}
}
