package gregtech6.crop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The 59-row data census for {@link GT6CropCards} (task cbc-3-crop-data-assets) — every pin is
 * a fixed literal walked against the upstream construction lines (tmp/gt6-1.7.10/src/main/java/
 * gregtech/compat/Compat_Recipes_IndustrialCraft.java:585-646; the GT_BaseCrop.java:58-80
 * constructor law). All walks ride the pure {@link GT6CropCards#card} face over STUB resolvers —
 * hermetic on both legs (no GTMaterialItems binding, no global-list size assumptions), per
 * ADR-CB5 (offline-first).
 */
public class CropCardsTest extends GTOfflineTestBase {

	/** The 59 card keys — the name law (GT_BaseCrop.java:59) IS the texture-dir name set (GT_BaseCrop.java:160-163). */
	private static final Set<String> EXPECTED_NAMES = Set.of(
			"indigo", "flax", "oilberries", "bobsyeruncleranks", "diareed", "withereed", "blazereed",
			"eggplant", "corium", "corpseplant", "creeperweed", "enderbloom", "meatrose", "milkwart",
			"glowshrooms", "slimeplant", "spidernip", "tearstalks", "tine", "coppon", "argentia",
			"plumbilia", "steeleafranks", "liveroots",
			"rye", "barley", "oats", "rice",
			"tea", "mint", "lemonplant", "greenappletree", "yellowappletree", "redappletree", "darkredappletree",
			"chili", "tomatoplant", "redgrapes", "whitegrapes", "greengrapes", "purplegrapes",
			"blueberrybush", "gooseberrybush", "candleberrybush", "cranberries",
			"blackcurrants", "whitecurrants", "redcurrants", "blackberries", "raspberries", "strawberries",
			"onion", "cucumber", "peanuts", "ananas",
			"desertnova", "cerublossom", "shimmerleaf", "cinderpearl");

	/** Permissive stubs + the material-dataset boot (the GTMaterialItemsRegistrationTest seat): the
	 * supplier-based Segs resolve REAL MT/OP objects here, so the verbatim pins are never vacuous. */
	@BeforeAll
	static void bootMaterialsAndStubResolvers() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		GT6CropCards.sMaterialItemResolver = (aPrefix, aMaterial) -> Items.WHEAT;
		GT6CropCards.sIdItemResolver = aId -> "minecraft:cobweb".equals(aId) ? null : Items.CARROT;
	}

	// ------------------------------------------------------------------ the census

	@Test
	public void fiftyNineRowsInFourUpstreamSegments() {
		assertEquals(59, GT6CropCards.rows().size());
		long tMaterial = GT6CropCards.rows().stream().filter(r -> r.line() >= 585 && r.line() <= 608).count();
		long tGrains   = GT6CropCards.rows().stream().filter(r -> r.line() >= 610 && r.line() <= 613).count();
		long tFood     = GT6CropCards.rows().stream().filter(r -> r.line() >= 615 && r.line() <= 641).count();
		long tMagic    = GT6CropCards.rows().stream().filter(r -> r.line() >= 643 && r.line() <= 646).count();
		assertEquals(24, tMaterial, ":585-608 the material/exotic band");
		assertEquals(4,  tGrains,   ":610-613 the grain band");
		assertEquals(27, tFood,     ":615-641 the food band");
		assertEquals(4,  tMagic,    ":643-646 the magic band");
		assertEquals(59, tMaterial + tGrains + tFood + tMagic, "the four bands tile the table");
	}

	@Test
	public void nameLawMatchesTheTextureDirectories() {
		Set<String> tSeen = new HashSet<>();
		for (GT6CropCards.CropCardRow tRow : GT6CropCards.rows()) {
			String tKey = tRow.name().toLowerCase().replaceAll(" ", ""); // GT_BaseCrop.java:59
			assertTrue(tKey.matches("[a-z0-9]+"), "the name law de-spaces and lowercases: " + tRow.name());
			assertTrue(tSeen.add(tKey), "duplicate card name: " + tKey);
		}
		assertEquals(EXPECTED_NAMES, tSeen, "the key set = the crop/<name>/ texture-dir census (248 sprites)");
	}

	@Test
	public void maxSizeBandsMatchTheTextureCensus() {
		Set<String> tS3 = bandNames(3), tS5 = bandNames(5), tS7 = bandNames(7);
		assertEquals(Set.of("eggplant", "milkwart", "glowshrooms", "tine", "coppon", "ananas"), tS3,
				"the six mx=3 families");
		assertEquals(Set.of("blueberrybush", "gooseberrybush", "candleberrybush", "blackcurrants", "whitecurrants", "redcurrants"),
				tS5, "the six berry bushes at mx=5");
		assertEquals(Set.of("rye", "barley", "oats", "rice"), tS7, "the four grains at mx=7");
		assertEquals(43, GT6CropCards.rows().stream().filter(r -> r.size() == 4).count());
		// the stage-set sum = the 248-path sprite ledger
		assertEquals(248, GT6CropCards.rows().stream().mapToInt(GT6CropCards.CropCardRow::size).sum());
	}

	// ------------------------------------------------------------------ verbatim spot walks (the acceptance x5+)

	@Test
	public void verbatimWalkIndigo() {
		GT6CropCards.CropCardRow r = row("indigo"); // :585
		assertEquals("Indigo", r.name());
		assertEquals("Eloraam", r.discoverer());
		assertEquals(OP.plantGtBlossom, r.drop().prefix(), "OP.plantGtBlossom.mat(MT.Indigo, 1)");
		assertEquals(MT.Indigo, r.drop().material());
		assertEquals(1, r.drop().count());
		assertNull(r.specialDrops());
		assertNotNull(r.baseSeed());
		assertEquals(OP.plantGtBlossom, r.baseSeed().prefix());
		assertEquals(MT.Indigo, r.baseSeed().material());
		assertEquals(4, r.baseSeed().count(), "the base seed rides mat(MT.Indigo, 4)");
		pinStats(r, 2, 4, 1, 4, 1, 1, 0, 4, 0);
		assertArrayEquals(new String[] {"Flower", "Color", "Ingredient"}, r.attributes());
	}

	@Test
	public void verbatimWalkDiareed() {
		GT6CropCards.CropCardRow r = row("diareed"); // :589
		assertEquals("Diareed", r.name());
		assertEquals("Direwolf20", r.discoverer());
		assertEquals(OP.dustTiny, r.drop().prefix());
		assertEquals(MT.Diamond, r.drop().material());
		assertNull(r.specialDrops(), "no special drops");
		assertNull(r.baseSeed(), "crossbreed-only");
		pinStats(r, 12, 4, 1, 4, 5, 0, 10, 2, 10);
		assertArrayEquals(new String[] {"Reed", "Fire", "Shiny", "Coal", "Diamond", "Crystal"}, r.attributes());
	}

	@Test
	public void verbatimWalkRice() {
		GT6CropCards.CropCardRow r = row("rice"); // :613
		assertEquals("Rice", r.name());
		assertEquals("Ellpeck", r.discoverer());
		assertEquals("gt6:food_crop_rice", r.drop().id(), "IL.Crop_Rice rides the T5a food_crop id");
		assertEquals("gt6:food_crop_rice", r.baseSeed().id());
		assertEquals(1, r.baseSeed().count());
		pinStats(r, 1, 7, 2, 7, 0, 4, 0, 0, 2);
		assertArrayEquals(new String[] {"Wheat", "Food", "Grain"}, r.attributes());
	}

	@Test
	public void verbatimWalkPurpleGrapes() {
		GT6CropCards.CropCardRow r = row("purplegrapes"); // :627
		assertEquals("Purple Grapes", r.name());
		assertEquals("Gregorius Techneticies", r.discoverer());
		assertEquals(1, r.specialDrops().length, "the Member Berries special-drop line");
		assertEquals("gt6:food_grapes_purple", r.specialDrops()[0].id());
		assertEquals("gt6:food_grapes_purple", r.drop().id());
		pinStats(r, 3, 4, 3, 4, 1, 4, 0, 2, 3);
		assertArrayEquals(new String[] {"Vine", "Food", "Fruit", "Member?"}, r.attributes());
	}

	@Test
	public void verbatimWalkCinderpearl() {
		GT6CropCards.CropCardRow r = row("cinderpearl"); // :646
		assertEquals("Cinderpearl", r.name());
		assertEquals("Azanor", r.discoverer());
		assertSame(Items.BLAZE_POWDER, r.drop().item(), "ST.make(Items.blaze_powder, 1, 0)");
		assertNull(r.baseSeed(), "IL.TC_Cinderpearl = foreign, declared absence");
		pinStats(r, 8, 4, 1, 4, 3, 1, 8, 2, 8);
		assertArrayEquals(new String[] {"Flower", "Magic", "Fire", "Blaze", "Sulfur", "Ingredient"}, r.attributes());
	}

	@Test
	public void verbatimStatBandWalks() {
		pinStats(row("eggplant"), 6, 3, 2, 3, 0, 4, 1, 0, 0);      // :592 — the AH=2/HA=3 mx=3 family
		assertArrayEquals(new GT6CropCards.Seg[] {null}, row("liveroots").specialDrops(), ":608 the TF_LiveRoot declared-absence slot");
		pinStats(row("strawberries"), 3, 4, 1, 4, 1, 4, 0, 2, 4);  // :637 — AH=1 (not the 3 the berry band rides)
		pinStats(row("blueberrybush"), 3, 5, 4, 5, 1, 4, 1, 4, 2); // :628 — the mx=5 band
		pinStats(row("desertnova"), 6, 4, 1, 4, 5, 1, 7, 4, 10);   // :643 — the WD=10 magic band
		assertEquals(OP.dust, row("withereed").drop().prefix(), ":590 OP.dust.mat(MT.Coal, 1) — the drop, not the special-drop coal item");
		assertEquals(MT.Coal, row("withereed").drop().material());
		assertEquals(Items.BONE_MEAL, row("corpseplant").specialDrops()[0].item(), ":594 IL.Dye_Bonemeal = the 1.7.10 white-dye meta (the ShCL identity law)");
		assertEquals(OP.nugget, row("steeleafranks").drop().prefix(), ":607 OP.nugget.mat(MT.Steeleaf, 1)");
		assertEquals(MT.Steeleaf, row("steeleafranks").drop().material());
		assertEquals(OP.plantGtBlossom, row("tea").baseSeed().prefix(), ":615 the base seed rides mat(MT.Tea, 4)");
		assertEquals(MT.Tea, row("tea").baseSeed().material());
		assertEquals(4, row("tea").baseSeed().count());
	}

	// ------------------------------------------------------------------ the constructor law

	@Test
	public void nullDropRowsSkipRegistration() {
		// the declared absences stay null slots (the :61 law): Shimmerleaf's TC base seed,
		// the Liveroots TF special drop, Cinderpearl's TC base seed
		assertNull(row("shimmerleaf").baseSeed(), ":645 IL.TC_Shimmerleaf = foreign, declared absence");
		assertNull(row("liveroots").specialDrops()[0], ":608 the TF_LiveRoot slot stays the null Seg");
		assertNull(row("cinderpearl").baseSeed(), ":646 IL.TC_Cinderpearl = foreign, declared absence");
		assertNull(GT6CropCards.resolve(null));
	}

	/**
	 * The cbc-6 re-seat: :643-644 ride the GT6 fallback flowers (GT6CropFoods meta
	 * 12010/12011, MultiItemFood.java:98-99) — drop = the ARS count 1 face, base seed =
	 * the ARS count 4 face, both of the fallback item (the upstream
	 * {@code get(1, fallback)/get(4, fallback)} shape, Compat_Recipes_IndustrialCraft
	 * :643-644), and the cards LIVE under resolving resolvers (59 live, below).
	 */
	@Test
	public void theMagicRowsRideTheGt6FallbackFlowers() {
		assertEquals("gt6:food_desertnova", row("desertnova").drop().id(), ":643 the IL.DesertNova fallback, count 1");
		assertEquals(1, row("desertnova").drop().count());
		assertEquals("gt6:food_desertnova", row("desertnova").baseSeed().id(), ":643 the base seed rides the fallback at 4");
		assertEquals(4, row("desertnova").baseSeed().count());
		assertEquals("gt6:food_cerublossom", row("cerublossom").drop().id(), ":644 the IL.Cerublossom fallback, count 1");
		assertEquals(1, row("cerublossom").drop().count());
		assertEquals("gt6:food_cerublossom", row("cerublossom").baseSeed().id(), ":644 the base seed rides the fallback at 4");
		assertEquals(4, row("cerublossom").baseSeed().count());
		assertNotNull(GT6CropCards.card(row("desertnova")), "GT_BaseCrop.java:61 — the drop resolves, the card lives");
		assertNotNull(GT6CropCards.card(row("cerublossom")), ":644 same law");
	}

	@Test
	public void stubbedWalkYieldsFiftyNineLiveCardsAndThirtySixBaseSeeds() {
		int tLive = 0, tSeeded = 0;
		Set<String> tNames = new HashSet<>();
		for (GT6CropCards.CropCardRow tRow : GT6CropCards.rows()) {
			GT6CropCard tCard = GT6CropCards.card(tRow);
			if (tCard == null) continue;
			tLive++;
			assertTrue(tNames.add(tCard.name()));
			assertEquals(tRow.name().toLowerCase().replaceAll(" ", ""), tCard.name(), "the card name = the name law");
			assertEquals(Math.max(1, tRow.tier()), tCard.tier(), "the :65 clamp");
			assertEquals(tRow.size(), tCard.maxSize(), "the :66 clamp (all rows >= 3 already)");
			if (tCard.baseSeed() != null) {
				assertFalse(tCard.baseSeed().isEmpty());
				tSeeded++;
			}
		}
		assertEquals(59, tLive, "59 rows, all live — the two magic rows ride the cbc-6 GT6 fallback flowers");
		assertEquals(36, tSeeded, "the base-seed rows (grains ride the produce item, GT_BaseCrop.java:77; flowers the fallback at 4)");
	}

	@Test
	public void registrationRidesTheSharedCbc2Face() {
		// the wiring face — a fixture card through GT6Crops (the cbc-2 registry), then cropOf finds it
		GT6CropCard tFixture = new GT6CropCard("testseed", "Tester", new ItemStack(Items.STICK), null, null,
				1, 4, 1, 2, 0, 0, 0, 0, 0, new String[0]);
		assertTrue(GT6Crops.registerCrop(tFixture), "the first registration of this JVM walk");
		assertFalse(GT6Crops.registerCrop(tFixture), "the duplicate keeps the first (the cbc-2 law)");
		assertSame(GT6Crops.WEED, GT6Crops.crops().get(0), "WEED stays the index-0 contract (the crossing enumeration)");
		ItemStack tMine = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tMine, GT6CropSeeds.OWNER, "testseed", 1, 1, 1, 1);
		assertSame(tFixture, GT6CropSeeds.cropOf(tMine), "cropOf resolves through the registration list");
	}

	// ------------------------------------------------------------------ the seed payload

	@Test
	public void seedPayloadRoundTrips() {
		ItemStack tStack = new ItemStack(Items.WHEAT); // the payload law guards keys, not the item type (ItemCropSeed :125-132)
		GT6CropSeeds.writeSeed(tStack, "gt6", "rye", 7, 9, 11, 2);
		GT6CropSeeds.SeedData tData = GT6CropSeeds.readSeed(tStack);
		assertNotNull(tData);
		assertEquals("gt6", tData.cropOwner());
		assertEquals("rye", tData.cropId());
		assertEquals(7, tData.growth());
		assertEquals(9, tData.gain());
		assertEquals(11, tData.resistance());
		assertEquals(2, tData.scanLevel());
		// the stat bytes truncate like the upstream setByte law (ItemCropSeed.java:115-118)
		ItemStack tWide = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tWide, "gt6", "indigo", 256, 0, 0, 0);
		assertEquals(0, GT6CropSeeds.readSeed(tWide).growth(), "(byte)256 = 0");
	}

	@Test
	public void seedPayloadNullLaw() {
		assertNull(GT6CropSeeds.readSeed(null));
		assertNull(GT6CropSeeds.readSeed(ItemStack.EMPTY));
		assertNull(GT6CropSeeds.readSeed(new ItemStack(Items.WHEAT)), "untagged = no payload");
		ItemStack tForeign = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tForeign, "othermod", "testseed", 1, 1, 1, 1);
		assertNull(GT6CropSeeds.cropOf(tForeign), "the owner stamp gates (the single-owner port)");
		ItemStack tUnknown = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tUnknown, GT6CropSeeds.OWNER, "nosuchcrop", 1, 1, 1, 1);
		assertNull(GT6CropSeeds.cropOf(tUnknown), "an unregistered crop id stays null");
	}

	/**
	 * The seed tooltip IS the scan disclosure (task cbc-6, the ItemCropSeed.addInformation
	 * :63-75 port): scan 4 discloses the three stat bytes after the crop name, scan 1 names
	 * the crop only, scan 0 discloses nothing. The payload law guards keys, not the item
	 * type (ItemCropSeed.getCropFromStack :125-132), so the face pins over a wheat stack.
	 */
	@Test
	public void seedTooltipDisclosesByScanLevel() {
		java.util.List<net.minecraft.network.chat.Component> tTooltip = new java.util.ArrayList<>();
		ItemStack tScanned = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tScanned, GT6CropSeeds.OWNER, "rye", 7, 9, 11, 4);
		GT6CropSeeds.appendSeedTooltip(tScanned, tTooltip);
		assertEquals(4, tTooltip.size(), "the crop name line + the three stat lines at scan 4");
		assertEquals("gt.crop.rye", ((net.minecraft.network.chat.contents.TranslatableContents) tTooltip.get(0).getContents()).getKey(),
				"scan >= 1 names the crop through the cbc-3 lang family");
		assertEquals("Gr 7", tTooltip.get(1).getString(), "the growth byte (the §2 face)");
		assertEquals("Ga 9", tTooltip.get(2).getString(), "the gain byte (the §6 face)");
		assertEquals("Re 11", tTooltip.get(3).getString(), "the resistance byte (the §3 face)");

		java.util.List<net.minecraft.network.chat.Component> tNamed = new java.util.ArrayList<>();
		ItemStack tScan1 = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tScan1, GT6CropSeeds.OWNER, "rye", 7, 9, 11, 1);
		GT6CropSeeds.appendSeedTooltip(tScan1, tNamed);
		assertEquals(1, tNamed.size(), "scan 1 discloses the name only (the stats stay hidden)");

		java.util.List<net.minecraft.network.chat.Component> tBlind = new java.util.ArrayList<>();
		ItemStack tScan0 = new ItemStack(Items.WHEAT);
		GT6CropSeeds.writeSeed(tScan0, GT6CropSeeds.OWNER, "rye", 7, 9, 11, 0);
		GT6CropSeeds.appendSeedTooltip(tScan0, tBlind);
		assertTrue(tBlind.isEmpty(), "scan 0 = the undisclosed face (the upstream unknown-seed line), nothing shown");

		java.util.List<net.minecraft.network.chat.Component> tUntagged = new java.util.ArrayList<>();
		GT6CropSeeds.appendSeedTooltip(new ItemStack(Items.WHEAT), tUntagged);
		assertTrue(tUntagged.isEmpty(), "a payload-less stack discloses nothing");
	}

	/** The cbc-6 fallback flowers exist in the T5a band — the crop-row seat is bound (the grain-bind census form). */
	@Test
	public void theMagicFallbacksSitInTheT5aBand() {
		assertTrue(gregtech6.registry.GT6CropFoods.PLAIN_ROWS.stream().anyMatch(r -> r.id().equals("food_desertnova")),
				"food_desertnova = the :643 fallback drop/base-seed carrier");
		assertTrue(gregtech6.registry.GT6CropFoods.PLAIN_ROWS.stream().anyMatch(r -> r.id().equals("food_cerublossom")),
				"food_cerublossom = the :644 fallback drop/base-seed carrier");
	}

	// ------------------------------------------------------------------ the ADR red lines

	@Test
	public void theDeadGrowthColumnNeverPortsAnywhereInTheCropPackage() throws IOException {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		// the pattern is split so this source never carries the dead column's literal itself
		List<String> tHits = scanSources(tMdk, Pattern.compile("growth" + "Speed"));
		assertTrue(tHits.isEmpty(), "ADR-CB4: the dead growth-speed column never ports — " + tHits);
	}

	@Test
	public void noIc2PackageReferencesInTheCropPackage() throws IOException {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		// the ADR-CB2 grep — dot-literal (the ic2/api prose faces stay allowed); the pattern is
		// split so this source never carries the literal itself
		List<String> tHits = scanSources(tMdk, Pattern.compile("ic2" + "\\.")); 
		assertTrue(tHits.isEmpty(), "ADR-CB2: zero IC2 package references in gregtech6/crop — " + tHits);
	}

	// ------------------------------------------------------------------ the sprite census

	/**
	 * The borrowed-sprite census (acceptance ②): 248 paths under block/crop/, every file PNG
	 * magic + 16x16, the per-crop stage count = the row's maxSize (the GT_BaseCrop.java:160-163
	 * render law), and every path seats exactly once in the assets/README.md "cbc-3" sha256
	 * ledger band (248 paths / 141 byte entities — the byte-identical borrow law: re-encoding
	 * or any silent drift turns this red).
	 */
	@Test
	public void spriteCensusMatchesTheRowsAndTheReadmeLedger() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		Path tCrop = tMdk.resolve("src/main/resources/assets/gt6/textures/block/crop");
		assertTrue(Files.isDirectory(tCrop), "the borrowed sprite tree exists");
		Map<String, Integer> tStagesByCrop = new HashMap<>();
		Map<String, List<String>> tPathsBySha = new HashMap<>();
		try (Stream<Path> tWalk = Files.walk(tCrop)) {
			for (Path tFile : tWalk.filter(Files::isRegularFile).toList()) {
				String tCropName = tFile.getParent().getFileName().toString();
				assertTrue(tFile.getFileName().toString().matches("[1-9][0-9]*\\.png"), "stage sprite: " + tFile);
				byte[] tPng = Files.readAllBytes(tFile);
				assertTrue(tPng.length > 24 && tPng[0] == (byte)0x89 && tPng[1] == 'P' && tPng[2] == 'N' && tPng[3] == 'G',
						"PNG magic: " + tFile);
				assertEquals(16, ihdr(tPng, 16), "16px wide: " + tFile);
				assertEquals(16, ihdr(tPng, 20), "16px tall: " + tFile);
				tStagesByCrop.merge(tCropName, 1, Integer::sum);
				tPathsBySha.computeIfAbsent(sha256(tPng), k -> new ArrayList<>())
						.add("block/crop/" + tCrop.relativize(tFile).toString().replace('\\', '/'));
			}
		}
		assertEquals(248, tStagesByCrop.values().stream().mapToInt(Integer::intValue).sum(), "the 248-path sprite ledger");
		for (GT6CropCards.CropCardRow tRow : GT6CropCards.rows()) {
			assertEquals(tRow.size(), tStagesByCrop.get(tRow.name().toLowerCase().replaceAll(" ", "")).intValue(),
					"stage set 1..maxSize: " + tRow.name());
		}
		// the README ledger band: 141 entity rows, dup counts, one sha per path, byte-consistent
		List<String> tLedgerPaths = new ArrayList<>();
		Pattern tLedgerRow = Pattern.compile("^- `(block/crop/[a-z0-9]+/\\d\\.png)` \\| `([0-9a-f]{64})` \\| (\\d+) \\| ?(.*)$", Pattern.MULTILINE);
		java.util.regex.Matcher tMatch = tLedgerRow.matcher(Files.readString(tMdk.resolve("src/main/resources/assets/README.md")));
		while (tMatch.find()) {
			List<String> tGroup = new ArrayList<>(List.of(tMatch.group(1)));
			for (String tDup : tMatch.group(4).split(" ")) {
				if (!tDup.isEmpty()) tGroup.add("block/crop/" + tDup.replace("`", ""));
			}
			assertEquals(Integer.parseInt(tMatch.group(3)), tGroup.size(), "dup count: " + tMatch.group(1));
			for (String tPath : tGroup) {
				assertFalse(tLedgerPaths.contains(tPath), "path ledgered twice: " + tPath);
				tLedgerPaths.add(tPath);
				String tSha = tPathsBySha.keySet().stream()
						.filter(sha -> tPathsBySha.get(sha).contains(tPath)).findFirst().orElse(null);
				assertNotNull(tSha, "ledgered path missing from the tree: " + tPath);
				assertEquals(tMatch.group(2), tSha, "sha256 drift: " + tPath);
			}
		}
		assertEquals(141, tPathsBySha.size(), "the 141 unique byte entities");
		assertEquals(248, tLedgerPaths.size(), "every tree path ledgered exactly once");
	}

	// ------------------------------------------------------------------ fixtures

	private static GT6CropCards.CropCardRow row(String aKey) {
		return GT6CropCards.rows().stream()
				.filter(r -> r.name().toLowerCase().replaceAll(" ", "").equals(aKey))
				.findFirst().orElseThrow();
	}

	private static Set<String> bandNames(int aSize) {
		Set<String> rSet = new HashSet<>();
		for (GT6CropCards.CropCardRow tRow : GT6CropCards.rows()) {
			if (tRow.size() == aSize) rSet.add(tRow.name().toLowerCase().replaceAll(" ", ""));
		}
		return rSet;
	}

	private static void pinStats(GT6CropCards.CropCardRow r, int tier, int size, int ah, int ha,
			int ch, int fd, int df, int co, int wd) {
		assertEquals(tier, r.tier(), "tier");
		assertEquals(size, r.size(), "maxSize");
		assertEquals(ah, r.afterHarvest(), "the AH column");
		assertEquals(ha, r.harvest(), "the HA column");
		assertEquals(ch, r.ch(), "statChemical");
		assertEquals(fd, r.fd(), "statFood");
		assertEquals(df, r.df(), "statDefensive");
		assertEquals(co, r.co(), "statColor");
		assertEquals(wd, r.wd(), "statWeed");
	}

	/** The big-endian IHDR dimension at the given offset (16 = width, 20 = height). */
	private static int ihdr(byte[] aPng, int aOffset) {
		return ((aPng[aOffset] & 0xFF) << 24) | ((aPng[aOffset + 1] & 0xFF) << 16)
				| ((aPng[aOffset + 2] & 0xFF) << 8) | (aPng[aOffset + 3] & 0xFF);
	}

	private static String sha256(byte[] aData) throws Exception {
		StringBuilder rHex = new StringBuilder();
		for (byte tB : MessageDigest.getInstance("SHA-256").digest(aData)) rHex.append(String.format("%02x", tB));
		return rHex.toString();
	}

	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/crop/GT6CropCards.java"))) {
				return tDir;
			}
		}
		return null;
	}

	private static List<String> scanSources(Path aMdk, Pattern aPattern) throws IOException {
		List<String> rHits = new ArrayList<>();
		List<Path> tRoots = List.of(
				aMdk.resolve("src/main/java/gregtech6/crop"),
				aMdk.resolve("src/test/java/gregtech6/crop"));
		for (Path tRoot : tRoots) {
			if (!Files.isDirectory(tRoot)) continue;
			try (Stream<Path> tWalk = Files.walk(tRoot)) {
				for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".java")).toList()) {
					List<String> tLines = Files.readAllLines(tFile);
					for (int i = 0; i < tLines.size(); i++) {
						if (aPattern.matcher(tLines.get(i)).find()) {
							rHits.add(tFile.getFileName() + ":" + (i + 1) + " " + tLines.get(i).trim());
						}
					}
				}
			}
		}
		return rHits;
	}
}
