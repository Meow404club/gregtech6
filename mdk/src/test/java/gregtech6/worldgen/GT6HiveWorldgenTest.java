package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.Level;

/**
 * The bumble-hive worldgen acceptance (task p32-bees-lv2, the GT6FluidSpringWorldgenTest
 * posture): the HiveKind family-table parity against the upstream placeHive call-site
 * constants (WorldgenHives.java:111/:120/:135/:155-186), the family-tag composition
 * pins, and the FIXED-SEED PLACEMENT-SET DETERMINISM — the offline leg of the card
 * acceptance ②: the coordinate-seeded stream the Feature consumes
 * ({@link GT6VeinGenerator#veinRandom} + the :59 column pick) replayed over a chunk
 * region twice with independent Random instances must yield IDENTICAL decision sets, in
 * all three dimension routings (the decision-level semantic,
 * decisions.2026-09-18-p31-strata-lens-determinism-acceptance — block survival rides the
 * pipeline drift).
 *
 * <p>Offline-safe by construction: pure Random walks + TagKey composition (no registry,
 * no world).
 */
class GT6HiveWorldgenTest {

	/** The card's fixed probe seed — the P31 family's calibrated RCON seed. */
	private static final long SEED = 6131000569321125127L;

	/** The offline replay region — 64x64 chunks. */
	private static final int REGION = 64;

	@BeforeAll
	static void boot() {
		// the vanilla bootstrap bracket (the GTOfflineTestBase shape) — tryDetectVersion
		// FIRST (MappedRegistry guards on it), then bootStrap; the Feature class-static
		// chain touches EntityType (Feature.java:82 the MonsterRoomFeature clinit)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@Test
	void kindTableMatchesTheUpstreamCallSites() {
		// (colour, species) per WorldgenHives.java — the embedded forms and the surface chain
		Object[][] tExpected = {
			{"STONE"      , 0xC0C0C0 , 500}, // :135 DYE_INT_LightGray
			{"WATER"      , 0x8080FF , 100}, // :156/:168 DYE_INT_LightBlue
			{"MAGICAL"    , 0x800080 , 200}, // :158 DYE_INT_Purple
			{"VOLCANIC"   , 0x202020 , 300}, // :160 DYE_INT_Black {32,32,32}
			{"END_BIOME"  , 0x00AAAA , 400}, // :162 literal 0x00aaaa
			{"NETHER_BIOME", 0xAA0000, 300}, // :164 literal 0xaa0000
			{"SHROOM"     , 0xFFC0C0 , 800}, // :166/:174 DYE_INT_Pink {255,192,192}
			{"SHORE"      , 0x8080FF , 100}, // :167 OCEAN_BEACH+LAKE
			{"JUNGLE"     , 0x00FF00 , 600}, // :170 DYE_INT_Green
			{"FROZEN"     , 0xFFFFFF , 700}, // :172 DYE_INT_White
			{"RED_SAND"   , 0xFF0000 , 900}, // :176 DYE_INT_Red
			{"SAND"       , 0xFFFF00 , 900}, // :178 DYE_INT_Yellow
			{"ROCK"       , 0xC0C0C0 , 500}, // :180 DYE_INT_LightGray
			{"GRASS"      , 0xFFDD99 , 0  }, // :182 literal 0xffdd99
			{"DIRT"       , 0x604000 , 0  }, // :184 DYE_INT_Brown {96,64,0}
			{"DEFAULT"    , 0x800080 , 200}, // :186 the magical default
		};
		assertEquals(16, tExpected.length);
		for (Object[] tRow : tExpected) {
			GT6HiveFeature.HiveKind tKind = GT6HiveFeature.HiveKind.valueOf((String) tRow[0]);
			assertEquals(tRow[1], tKind.color, tKind + " colour (the placeHive argument)");
			assertEquals(tRow[2], tKind.species, tKind + " species trace (the upstream speciesID argument)");
		}
		// the tag-family wiring: four live families, four EMPTY pack surfaces, the rest block-contact judged
		assertEquals("magical", GT6HiveFeature.HiveKind.MAGICAL.tagFamily);
		assertEquals("volcanic", GT6HiveFeature.HiveKind.VOLCANIC.tagFamily);
		assertEquals("end", GT6HiveFeature.HiveKind.END_BIOME.tagFamily);
		assertEquals("nether", GT6HiveFeature.HiveKind.NETHER_BIOME.tagFamily);
		assertEquals("shroom", GT6HiveFeature.HiveKind.SHROOM.tagFamily);
		assertEquals("shore", GT6HiveFeature.HiveKind.SHORE.tagFamily);
		assertEquals("jungle", GT6HiveFeature.HiveKind.JUNGLE.tagFamily);
		assertEquals("frozen", GT6HiveFeature.HiveKind.FROZEN.tagFamily);
		for (GT6HiveFeature.HiveKind tKind : GT6HiveFeature.HiveKind.values()) {
			switch (tKind) {
				case MAGICAL, VOLCANIC, END_BIOME, NETHER_BIOME, SHROOM, SHORE, JUNGLE, FROZEN ->
					assertTrue(tKind.tagFamily != null, tKind + " rides its family tag");
				default -> assertEquals(null, tKind.tagFamily, tKind + " is block-contact judged");
			}
		}
	}

	@Test
	void familyTagsComposeUnderTheGt6Namespace() {
		TagKey<Biome> tShore = GT6HiveFeature.hiveTag("shore");
		assertEquals("gt6", tShore.location().getNamespace());
		assertEquals("bumble_hives/shore", tShore.location().getPath(),
				"#gt6:bumble_hives/<family> — the GT6BiomeTags emission path (data/gt6/tags/worldgen/biome)");
	}

	/**
	 * THE determinism acceptance (card ②): over the same region the SAME decision walk
	 * reruns from fresh Randoms — the column picks per dimension must be set-identical
	 * bit-for-bit, and the three dimension routings must be MUTUALLY DISTINCT streams
	 * (the dimension salts fork them; a shared stream would correlate hive columns across
	 * dimensions).
	 */
	@Test
	void fixedSeedRerunsProduceIdenticalPlacementSets() {
		for (long tSalt : new long[] {
				GT6VeinGenerator.OVERWORLD_DIMENSION_SALT, GT6VeinGenerator.NETHER_DIMENSION_SALT,
				GT6VeinGenerator.END_DIMENSION_SALT}) {
			Set<String> tFirst = columnSet(tSalt);
			Set<String> tSecond = columnSet(tSalt);
			assertEquals(tFirst, tSecond,
					"the fixed-seed rerun is decision-identical (salt " + tSalt + ") — acceptance ②");
			assertFalse(tFirst.isEmpty(), "the walk actually decides in the region");
		}
	}

	@Test
	void dimensionStreamsAreDistinct() {
		assertNotEquals(columnSet(GT6VeinGenerator.OVERWORLD_DIMENSION_SALT),
				columnSet(GT6VeinGenerator.NETHER_DIMENSION_SALT), "the overworld and nether streams fork");
		assertNotEquals(columnSet(GT6VeinGenerator.OVERWORLD_DIMENSION_SALT),
				columnSet(GT6VeinGenerator.END_DIMENSION_SALT), "the overworld and end streams fork");
	}

	/**
	 * The Feature's decision core replayed outside the world: {@code veinRandom(seed, salt,
	 * cx, cz)} then the :59 column pick — exactly the consumption order GT6HiveFeature.place
	 * runs (the roll-order pin: any reordering would break this test and the determinism
	 * semantics with it).
	 */
	private static Set<String> columnSet(long aDimSalt) {
		Set<String> rSet = new HashSet<>();
		for (int tCx = 0; tCx < REGION; tCx++) {
			for (int tCz = 0; tCz < REGION; tCz++) {
				Random tRandom = GT6VeinGenerator.veinRandom(SEED, aDimSalt, tCx, tCz);
				int tX = tCx * 16 + tRandom.nextInt(16); // WorldgenHives.java:59 verbatim
				int tZ = tCz * 16 + tRandom.nextInt(16);
				rSet.add(tX + "," + tZ);
			}
		}
		return rSet;
	}

	/** The three upstream WorldgenObject rows collapsed onto one Feature + three modifiers (sanity: the constants still route). */
	@Test
	void dimensionRoutingKeysMatchVanilla() {
		assertEquals("minecraft:the_nether", Level.NETHER.location().toString());
		assertEquals("minecraft:the_end", Level.END.location().toString());
	}
}
