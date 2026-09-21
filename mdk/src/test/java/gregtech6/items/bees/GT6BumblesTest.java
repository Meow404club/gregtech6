package gregtech6.items.bees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The bumblebee domain pins (task p33-bees-lv3-a-items acceptance ①): the meta-decimal
 * code round-trip (MultiItemBumbles.java:512), the mutation ladder distribution
 * (:459-478), the gt.bumble NBT semantics (IItemBumbleBee.java:109-184) and the 8-face
 * registration containment (the FML-booted leg asserts for real, the offline bare-JVM
 * leg skips via the containsKey latch — the GT6CircuitPartsRegistrationTest form).
 */
class GT6BumblesTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	// ------------------------------------------------ the species table

	@Test
	void speciesTableCarriesTheEightyRows() {
		// the 20 families x 4 tiers of addItems() (MultiItemBumbles.java:70-178), in order
		assertEquals(80, GT6Bumbles.SPECIES.size(), "the 80 species rows");
		Set<Integer> tCodes = new HashSet<>();
		for (GT6Bumbles.SpeciesRow tRow : GT6Bumbles.SPECIES) assertTrue(tCodes.add(tRow.code()), "codes distinct: " + tRow.code());
		assertEquals(0, GT6Bumbles.SPECIES.get(0).code(), "the first row is the Wild");
		assertEquals(20330, GT6Bumbles.SPECIES.get(79).code(), "the last row is the Tera");
		// every family/tier pair of the upstream ranges exists
		for (int tFamily : new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 100, 101, 102, 103, 104, 105, 200, 201, 202, 203}) {
			for (int tTier = 0; tTier < 4; tTier++) assertNotNull(GT6Bumbles.speciesOf(tFamily * 100 + tTier * 10), "family " + tFamily + " tier " + tTier);
		}
		// the display faces verbatim (:70, :83, :154, :178)
		assertEquals("Wild Bumblebee", GT6Bumbles.speciesOf(0).display());
		assertEquals("Dumblebee", GT6Bumbles.speciesOf(230).display());
		assertEquals("General Bumblemond", GT6Bumbles.speciesOf(10530).display());
		assertEquals("Tera Bumble", GT6Bumbles.speciesOf(20330).display());
		assertNull(GT6Bumbles.speciesOf(9990), "an out-of-table code is null");
	}

	@Test
	void codeAlgebraRoundTrips() {
		// meta/100 family + (meta/10)%10 tier rebuild the code (MultiItemBumbles.java:512)
		for (GT6Bumbles.SpeciesRow tRow : GT6Bumbles.SPECIES) {
			assertEquals(tRow.code(), GT6Bumbles.familyOf(tRow.code()) * 100 + GT6Bumbles.tierOf(tRow.code()) * 10, "round trip: " + tRow.code());
		}
		assertTrue(GT6Bumbles.sameSpecies(310, 311), "the type digit is not identity (:567)");
		assertTrue(GT6Bumbles.sameSpecies(310, 316), "the scan digit is not identity");
		assertFalse(GT6Bumbles.sameSpecies(310, 320), "a tier step is a different species");
	}

	// ------------------------------------------------ the mutation ladder

	@Test
	void mutateChanceLadder() {
		// the :459-467 ladder verbatim — 500/500/250/25 of 10000, 0 beyond tier 3
		assertEquals(500, GT6Bumbles.mutateChance(0));
		assertEquals(500, GT6Bumbles.mutateChance(10));
		assertEquals(250, GT6Bumbles.mutateChance(20));
		assertEquals(25, GT6Bumbles.mutateChance(30));
		assertEquals(0, GT6Bumbles.mutateChance(40), "tier 4 has no mutation");
		assertEquals(250, GT6Bumbles.mutateChance(20120), "the hybrid families ride the same ladder");
	}

	@Test
	void mutateWalkDistribution() {
		// the :470-478 walk: tier 0 climbs, tier 3 falls, the middle tiers go either way
		Random tRandom = new Random(42);
		for (int i = 0; i < 1000; i++) assertEquals(10, GT6Bumbles.mutateCode(100, tRandom) - 100, "tier 0 is up-only");
		for (int i = 0; i < 1000; i++) assertEquals(-10, GT6Bumbles.mutateCode(10330, tRandom) - 10330, "tier 3 is down-only");
		boolean tUp = false, tDown = false;
		for (int i = 0; i < 200; i++) {
			int tStep = GT6Bumbles.mutateCode(10110, tRandom) - 10110;
			tUp |= tStep == 10;
			tDown |= tStep == -10;
			assertTrue(GT6Bumbles.familyOf(10110 + tStep) == GT6Bumbles.familyOf(10110), "the family never changes");
		}
		assertTrue(tUp && tDown, "tier 1 goes both ways");
		tUp = tDown = false;
		for (int i = 0; i < 200; i++) {
			int tStep = GT6Bumbles.mutateCode(310, tRandom) - 310;
			tUp |= tStep == 10;
			tDown |= tStep == -10;
		}
		assertTrue(tUp && tDown, "tier 2 goes both ways");
		assertEquals(40, GT6Bumbles.mutateCode(40, tRandom), "an out-of-table code copies");
	}

	// ------------------------------------------------ the gt.bumble NBT semantics

	@Test
	void wildRollCarriesTheThirteenGeneKeys() {
		CompoundTag tGenes = GT6BumbleGenes.rollGenes(new Random(7));
		// the always-written core keys (the :134-144 writes)
		for (String tKey : new String[] {GT6BumbleGenes.KEY_MIN_HUM, GT6BumbleGenes.KEY_MAX_HUM, GT6BumbleGenes.KEY_MIN_TEMP,
				GT6BumbleGenes.KEY_MAX_TEMP, GT6BumbleGenes.KEY_OFFSPRING, GT6BumbleGenes.KEY_AGGRO, GT6BumbleGenes.KEY_WORK,
				GT6BumbleGenes.KEY_LIFE, GT6BumbleGenes.KEY_DAY, GT6BumbleGenes.KEY_NIGHT, GT6BumbleGenes.KEY_OUTSIDE}) {
			assertTrue(tGenes.contains(tKey), "the gene key " + tKey);
		}
		// the conditional faces read through the getters (an absent key reads false, :178-183)
		assertTrue(GT6BumbleGenes.getOutsideActive(tGenes), "the sky face works outside");
		assertFalse(GT6BumbleGenes.getInsideActive(tGenes), "and not inside (the absent key)");
		// the :134-141 window shapes
		assertTrue(GT6BumbleGenes.getHumidityMin(tGenes) <= GT6BumbleGenes.getHumidityMax(tGenes), "the humidity window");
		assertTrue(GT6BumbleGenes.getTemperatureMin(tGenes) < GT6BumbleGenes.getTemperatureMax(tGenes), "the temperature window");
		assertTrue(GT6BumbleGenes.getOffspring(tGenes) >= 1 && GT6BumbleGenes.getOffspring(tGenes) <= 4, "offspring 1-4");
		assertTrue(GT6BumbleGenes.getWorkForce(tGenes) >= 1 && GT6BumbleGenes.getWorkForce(tGenes) <= 10000, "work 1-10000");
		assertTrue(GT6BumbleGenes.getAggressiveness(tGenes) >= 100 && GT6BumbleGenes.getAggressiveness(tGenes) <= 10000, "aggro 100-10000");
		assertTrue(GT6BumbleGenes.getLifeSpan(tGenes) >= 1200 && GT6BumbleGenes.getLifeSpan(tGenes) <= 144000, "life 1200-144000");
		// the plains outsider default (:130): sky, day-active
		assertTrue(GT6BumbleGenes.getDayActive(tGenes), "the plains default is day-active");
		assertFalse(GT6BumbleGenes.getNightActive(tGenes), "and not night-active");
	}

	@Test
	void rollWindowsStayInBounds() {
		// the desert/mesa flip (:131) and the rainfall-0/1 proofing bounds
		CompoundTag tDay = GT6BumbleGenes.rollGenes(300, 0.5F, true, false, new Random(1));
		assertTrue(GT6BumbleGenes.getDayActive(tDay) && !GT6BumbleGenes.getNightActive(tDay), "non-desert is day");
		CompoundTag tNight = GT6BumbleGenes.rollGenes(310, 0.0F, true, true, new Random(1));
		assertTrue(GT6BumbleGenes.getNightActive(tNight) && !GT6BumbleGenes.getDayActive(tNight), "desert is night");
		CompoundTag tRoofed = GT6BumbleGenes.rollGenes(286, 0.5F, false, false, new Random(1));
		assertTrue(GT6BumbleGenes.getInsideActive(tRoofed), "roofed biomes work inside (:150)");
		assertFalse(GT6BumbleGenes.getOutsideActive(tRoofed), "and the outside key stays unwritten (the :149-151 else branch)");
		for (int tSeed = 0; tSeed < 50; tSeed++) {
			CompoundTag tDry = GT6BumbleGenes.rollGenes(286, 0.0F, true, false, new Random(tSeed));
			assertFalse(GT6BumbleGenes.getRainproof(tDry) || GT6BumbleGenes.getStormproof(tDry), "rainfall 0 never proofs");
			CompoundTag tWet = GT6BumbleGenes.rollGenes(286, 1.0F, true, false, new Random(tSeed));
			assertTrue(GT6BumbleGenes.getRainproof(tWet), "rainfall 1 always rainproofs (the :147 P=rainfall roll)");
			// stormproof rides the :148 nextInt(20000) form — a HALF-chance at rainfall 1.0, never asserted always
			long tMin = GT6BumbleGenes.getTemperatureMin(tDry), tMax = GT6BumbleGenes.getTemperatureMax(tDry);
			assertTrue(tMin >= 286 - 45 && tMin <= 286 - 15, "the mintemp window");
			assertTrue(tMax >= 286 + 15 && tMax <= 286 + 45, "the maxtemp window");
		}
	}

	@Test
	void heredityPicksFromTheParents() {
		// the :109-128 walk — every gene a parent pick, the day/outside guarantees held
		CompoundTag tGenesA = new CompoundTag(), tGenesB = new CompoundTag();
		GT6BumbleGenes.setOffspring(tGenesA, 1);
		GT6BumbleGenes.setOffspring(tGenesB, 4);
		GT6BumbleGenes.setWorkForce(tGenesA, 2000);
		GT6BumbleGenes.setWorkForce(tGenesB, 9000);
		GT6BumbleGenes.setLifeSpan(tGenesA, 6000);
		GT6BumbleGenes.setLifeSpan(tGenesB, 120000);
		GT6BumbleGenes.setTemperatureMin(tGenesA, 100);
		GT6BumbleGenes.setTemperatureMin(tGenesB, 300);
		GT6BumbleGenes.setDayActive(tGenesA, true);
		GT6BumbleGenes.setNightActive(tGenesA, false);
		GT6BumbleGenes.setDayActive(tGenesB, false);
		GT6BumbleGenes.setNightActive(tGenesB, true);
		ItemStack tPrincess = beeStack(), tDrone = beeStack();
		GT6BumbleGenes.setGenes(tPrincess, tGenesA);
		GT6BumbleGenes.setGenes(tDrone, tGenesB);
		Random tRandom = new Random(11);
		boolean tPickedA = false, tPickedB = false;
		for (int i = 0; i < 100; i++) {
			CompoundTag tChild = GT6BumbleGenes.childGenes(tPrincess, tDrone, tRandom);
			long tOffspring = GT6BumbleGenes.getOffspring(tChild);
			assertTrue(tOffspring == 1 || tOffspring == 4, "offspring is a parent pick");
			tPickedA |= tOffspring == 1;
			tPickedB |= tOffspring == 4;
			long tWork = GT6BumbleGenes.getWorkForce(tChild);
			assertTrue(tWork == 2000 || tWork == 9000, "work is a parent pick");
			long tLife = GT6BumbleGenes.getLifeSpan(tChild);
			assertTrue(tLife == 6000 || tLife == 120000, "life is a parent pick");
			long tMin = GT6BumbleGenes.getTemperatureMin(tChild);
			assertTrue(tMin == 100 || tMin == 300, "mintemp is a parent pick");
			assertTrue(GT6BumbleGenes.getDayActive(tChild) || GT6BumbleGenes.getNightActive(tChild), "the day guarantee");
		}
		assertTrue(tPickedA && tPickedB, "both parents contribute");
	}

	// ------------------------------------------------ the stack carrier

	/**
	 * A plain carrier stack — a VANILLA item, because the offline forge leg cannot even
	 * CONSTRUCT a mod item (the forge intrusive-holder constructor writes the frozen
	 * registry, the GT6BeeHivesTest no-supplier-run posture); the gt.bumble seam
	 * functions are item-agnostic, and the real mod items assert through the containment
	 * test on the FML-booted leg.
	 */
	private static ItemStack beeStack() {
		return new ItemStack(net.minecraft.world.item.Items.STONE, 1);
	}

	@Test
	void codeAndGenesRoundTripOnTheStack() {
		ItemStack tStack = beeStack();
		assertEquals(0, GT6BumbleGenes.codeOf(tStack), "an untagged stack reads the Wild code 0");
		GT6BumbleGenes.setCode(tStack, 310);
		assertEquals(310, GT6BumbleGenes.codeOf(tStack), "the code round-trips");
		assertNull(GT6BumbleGenes.readGenes(tStack), "no genes yet");
		// the lazy roll face (:97-102): the first use rolls AND persists
		CompoundTag tFirst = GT6BumbleGenes.getOrCreateGenes(tStack, new Random(3));
		assertNotNull(GT6BumbleGenes.readGenes(tStack), "the roll persisted");
		assertSame(tFirst, GT6BumbleGenes.readGenes(tStack), "the second read returns the stored compound");
		// the explicit set face (:104-107) reads back through the same seam
		ItemStack tOther = beeStack();
		CompoundTag tHand = new CompoundTag();
		GT6BumbleGenes.setWorkForce(tHand, 4242);
		GT6BumbleGenes.setGenes(tOther, tHand);
		assertEquals(4242, GT6BumbleGenes.getWorkForce(GT6BumbleGenes.readGenes(tOther)), "the gene keys read back");
	}

	@Test
	void theMutateWalkRewritesTheCode() {
		// the Bumbliary :227-229 shape on a stack — the walk never leaves the family
		for (int tSeed = 0; tSeed < 20; tSeed++) {
			ItemStack tStack = beeStack();
			GT6BumbleGenes.setCode(tStack, 310);
			GT6Bumbles.mutate(tStack, new Random(tSeed));
			int tCode = GT6BumbleGenes.codeOf(tStack);
			assertTrue(tCode == 300 || tCode == 320, "tier 1 steps one rung: " + tCode);
			assertEquals(3, GT6Bumbles.familyOf(tCode), "the family is preserved");
		}
	}

	// ------------------------------------------------ the type fractal + registration

	@Test
	void theTypeFractalFoldsThroughFive() {
		// the %5 fold over the face table (IItemBumbleBee.java:51, MultiItemBumbles.java:564);
		// the item INSTANCES live only on the FML-booted leg (the frozen-registry posture)
		assertEquals(GT6Bumbles.TYPE_DRONE, GT6Bumbles.FACES.get(0).face());
		assertEquals(GT6Bumbles.TYPE_PRINCESS, GT6Bumbles.FACES.get(1).face());
		assertEquals(GT6Bumbles.TYPE_QUEEN, GT6Bumbles.FACES.get(2).face());
		assertEquals(GT6Bumbles.TYPE_DEAD, GT6Bumbles.FACES.get(3).face());
		for (int i = 0; i < 4; i++) {
			GT6Bumbles.FaceRow tBase = GT6Bumbles.FACES.get(i), tScanned = GT6Bumbles.FACES.get(i + 4);
			assertTrue(tScanned.face() == tBase.face() && tScanned.scanned(), "the scanned form shares the face digit: " + tBase.name());
			byte tFold = (byte)(tBase.face() % 5);
			assertTrue(tFold == tBase.face(), "the base faces never fold: " + tBase.name());
			byte tScannedType = (byte)(tScanned.face() + GT6Bumbles.SCAN_OFFSET);
			assertEquals(tBase.face(), tScannedType % 5, "the %5 fold lands the scanned form on its base: " + tScanned.name());
		}
	}

	@Test
	void theFaceTableCarriesTheEightIds() {
		assertEquals(8, GT6Bumbles.FACES.size(), "the 8 faces");
		assertEquals("bumble_drone", GT6Bumbles.FACES.get(0).itemId());
		assertEquals("bumble_dead_scanned", GT6Bumbles.FACES.get(7).itemId());
		Set<String> tIds = new HashSet<>();
		for (GT6Bumbles.FaceRow tFace : GT6Bumbles.FACES) assertTrue(tIds.add(tFace.itemId()), "ids distinct");
		assertEquals("gt6.row.bumble.name.princess_scanned", GT6Bumbles.FACES.get(5).formatKey());
	}

	@Test
	void theRequirementWordsAreTheVanillaTrim() {
		assertEquals("Flowers (even potted ones work)", GT6Bumbles.requirementOf(0));
		assertEquals("Netherwart", GT6Bumbles.requirementOf(3));
		assertEquals(GT6Bumbles.requirementOf(200), GT6Bumbles.requirementOf(3), "the element families share the base words");
		assertEquals("Soul Sand Blocks", GT6Bumbles.requirementOf(103));
		assertEquals("Cacti", GT6Bumbles.requirementOf(105));
	}

	@Test
	void theEnvTempCounterpartMatchesTheUpstreamFormula() {
		// WD.java:413 (C-3+temperature*20, CS.C = 273) over the modern base temperature
		assertEquals(286, GT6BumbleGenes.envTemp(0.8F), "plains 286 K (the :130 default)");
		assertEquals(310, GT6BumbleGenes.envTemp(2.0F), "desert 310 K");
		assertEquals(270, GT6BumbleGenes.envTemp(0.0F), "frozen 270 K");
		assertEquals(1, GT6BumbleGenes.envTemp(-50F), "the max(1, ...) floor");
	}

	@Test
	void theRainfallClassifierPartitionsTheClimateBands() {
		// the downfall-access compromise (the ponytail note): the four bands
		assertEquals(0.0F, GT6BumbleGenes.rainfallOf(false, 0.8F), "no precipitation = dry");
		assertEquals(0.0F, GT6BumbleGenes.rainfallOf(true, 2.0F), "the savanna band = dry");
		assertEquals(0.9F, GT6BumbleGenes.rainfallOf(true, 1.5F), "the jungle band");
		assertEquals(0.5F, GT6BumbleGenes.rainfallOf(true, 0.8F), "the temperate band");
		assertTrue(GT6BumbleGenes.isDesertOrMesa(false, 0.8F), "no precipitation = the desert flip");
		assertTrue(GT6BumbleGenes.isDesertOrMesa(true, 2.0F), "hot = the desert flip");
		assertFalse(GT6BumbleGenes.isDesertOrMesa(true, 0.8F), "temperate is not the flip");
	}

	// ------------------------------------------------ the FML-boot containment

	/** The bee item id (the constructor fork: 1.21.1 privatized it, GT6HiveFeature.fromNamespaceAndPath). */
	private static ResourceLocation beeId(String aPath) {
		//? if forge {
		return new ResourceLocation("gt6", aPath);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
		 *///?}
	}

	@Test
	void registriesContainTheEightFaces() {
		// acceptance ② — on an FML-booted JVM the real item registry carries all 8 faces
		if (!BuiltInRegistries.ITEM.containsKey(beeId("bumble_drone"))) {
			return; // the offline bare-JVM leg; the FML-booted leg asserts for real
		}
		for (int i = 0; i < GT6Bumbles.FACES.size(); i++) {
			GT6Bumbles.FaceRow tFace = GT6Bumbles.FACES.get(i);
			ResourceLocation tId = beeId(tFace.itemId());
			assertTrue(BuiltInRegistries.ITEM.containsKey(tId), "registered: " + tFace.itemId());
			assertSame(GT6Bumbles.BEE_ITEMS.get(i).get(), BuiltInRegistries.ITEM.get(tId), "identity: " + tFace.itemId());
		}
	}
}
