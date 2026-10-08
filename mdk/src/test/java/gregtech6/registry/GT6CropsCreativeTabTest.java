package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import gregtech6.crop.GT6CropCard;
import gregtech6.crop.GT6CropCards;
import gregtech6.crop.GT6CropSeeds;
import gregtech6.crop.GT6Crops;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task crop-creative-tab — the {@code gt6:crops} creative-tab pins (the GT6FoodsTest /
 * GT6ToolsCreativeTabTest offline discipline): the tab and its items are NOT resolvable in
 * the bootstrapped-and-frozen bare forge JVM (the mod-Item intrusive-holder wall), so the
 * registration pins ride the pre-registration {@code DeferredRegister} view
 * ({@code getId()} reads the name field set at construction), and the display-walk pins
 * drive {@link GT6CropSticks#walkDisplayItems} directly through a collector lambda —
 * LIVE on the FML test JVM (the mod bootstraps and binds the holders), assume-skipped on
 * the bare forge leg (the GTOfflineTestBase ASSUME-SKIP posture; the FML leg is the leg
 * where the real registration lives). The walk tests therefore need no resolver stubs:
 * on the FML leg the real {@link GT6CropCards} resolvers answer from the live registry
 * (59 rows live — the production truth CropCardsTest pins under its stubs).
 *
 * <p>The walk pins ride an EXPLICIT hermetic card list, never {@code GT6Crops#crops()}
 * for the absolute count (the CropEndToEndTest law: the static registry is JVM-shared
 * across test classes and accumulates fixture cards); the live-registry delegate gets a
 * separate relative-count pin ({@code 2 + crops().size()}).
 *
 * <p>The full-book number 62 = 2 head rows (stick + blank seed) + 60 cards (WEED + the 59
 * GT6CropCards rows, all live on the FML leg), first-run-verified then pinned per the task
 * card.
 */
public class GT6CropsCreativeTabTest extends GTOfflineTestBase {

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/** The holder-bound face — the FML test JVM binds live, the bare forge JVM does not (the CropEndToEndTest:177 fork). */
	private static boolean seedItemsBound() {
		//? if forge {
		return GT6CropSticks.CROP_STICK_ITEM.isPresent() && GT6CropSeeds.CROP_SEED.isPresent();
		//?} else {
		/*return GT6CropSticks.CROP_STICK_ITEM.isBound() && GT6CropSeeds.CROP_SEED.isBound(); // 21.1: the DeferredHolder face
		 *///?}
	}

	/** The full book — WEED + the 59 rows built through the live card face, in walk order. */
	private static List<GT6CropCard> fullBook() {
		List<GT6CropCard> rCards = new ArrayList<>();
		rCards.add(GT6Crops.WEED);
		for (GT6CropCards.CropCardRow tRow : GT6CropCards.rows()) {
			GT6CropCard tCard = GT6CropCards.card(tRow);
			if (tCard != null) rCards.add(tCard);
		}
		return rCards;
	}

	// ----------------------------------------------------------------- registration pins

	/** The tab DR targets the vanilla creative-tab registry; id gt6:crops; the title key literal (the GT6FoodsTest form). */
	@Test
	public void tabRegistrationIsThePinnedSeat() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6CropSticks.CREATIVE_MODE_TABS.getRegistryKey());
		assertEquals(rl("crops"), GT6CropSticks.CROPS_TAB.getId());
		assertEquals("itemGroup.gt6.crops", GT6CropSticks.TAB_TITLE_KEY);
	}

	/** The DR holds exactly the one "crops" tab (the per-family tab discipline, the GT6FoodsTest form). */
	@Test
	public void tabsRegistryHoldsExactlyTheCropsTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (net.minecraftforge.registries.RegistryObject<CreativeModeTab> tTab : GT6CropSticks.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GT6CropSticks.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		 *///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("crops")), tIds);
	}

	/**
	 * The food-tab ride is RETIRED: neither registration home declares the old
	 * {@code onBuildTabContents} join handler anymore (the name-absence face — the
	 * CreativeTabJoinCensusTest reflection discipline, inverted for the negative).
	 */
	@Test
	public void theFoodTabRideIsRetired() {
		for (Class<?> tHome : new Class<?>[] {GT6CropSticks.class, GT6CropSeeds.class}) {
			for (Method tMethod : tHome.getDeclaredMethods()) {
				assertNotEquals("onBuildTabContents", tMethod.getName(),
						tHome.getSimpleName() + " still declares the retired gt6:food join");
			}
		}
	}

	// ----------------------------------------------------------------- the display walk

	/**
	 * The full-book count + the head/tail anchors + the payload face: 2 head rows then one
	 * fully-scanned representative seed per card, walk order = WEED first / cinderpearl last,
	 * every seed carrying owner gt6 + the neutral 1/1/1 bytes + scan 4 (the tooltip-disclosure
	 * face). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theWalkPinsTheFullBookSixtyTwo() {
		assumeTrue(seedItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<GT6CropCard> tCards = fullBook();
		assertEquals(60, tCards.size(), "the full book = WEED + the 59 GT6CropCards rows (all live on the FML leg)");
		List<ItemStack> tOut = new ArrayList<>();
		GT6CropSticks.walkDisplayItems((aStack, aVisibility) -> tOut.add(aStack), tCards);
		assertEquals(62, tOut.size(), "2 head rows + 60 representative seeds (2 + the full book, the task-card pin)");
		assertSame(GT6CropSticks.CROP_STICK_ITEM.get(), tOut.get(0).getItem(), "row 0 = the crop stick (the domain entry item, the tab icon)");
		assertSame(GT6CropSeeds.CROP_SEED.get(), tOut.get(1).getItem(), "row 1 = the blank seed");
		assertNull(GT6CropSeeds.readSeed(tOut.get(1)), "the blank seed carries NO payload (the bare item face)");
		SeedDataAssert.seed(tOut.get(2), "weed", "row 2 = the WEED seed, the walk-order head");
		SeedDataAssert.seed(tOut.get(61), "cinderpearl", "row 61 = the walk-order tail (the last rows() line)");
		for (int i = 2; i < tOut.size(); i++) {
			SeedDataAssert.seed(tOut.get(i), tCards.get(i - 2).name(), "walk order = the card list order, row " + i);
		}
	}

	/**
	 * The live-registry delegate — the tab builder lambda body walks {@code GT6Crops#crops()}
	 * itself: relative count 2 + the live size, WEED leading the seed band. Relative (never an
	 * absolute number) per the shared-registry law. FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theLiveDelegateWalksTheRegistry() {
		assumeTrue(seedItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<ItemStack> tOut = new ArrayList<>();
		GT6CropSticks.walkDisplayItems((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(2 + GT6Crops.crops().size(), tOut.size(), "2 head rows + one seed per live card");
		assertSame(GT6CropSticks.CROP_STICK_ITEM.get(), tOut.get(0).getItem());
		assertSame(GT6Crops.WEED, GT6Crops.crops().get(0), "WEED stays the index-0 contract");
		SeedDataAssert.seed(tOut.get(2), "weed", "the seed band leads with WEED (the honest full book)");
		assertTrue(tOut.size() >= 62, "at least the full book rides the live registry (fixture cards may accumulate)");
	}

	/** The payload-pin helper — the neutral 1/1/1 + scan 4 + owner gt6 (the GT6CropSticks javadoc law). */
	private static final class SeedDataAssert {
		static void seed(ItemStack aStack, String aCropId, String aMessage) {
			GT6CropSeeds.SeedData tData = GT6CropSeeds.readSeed(aStack);
			assertTrue(tData != null, aMessage + ": the payload parses");
			assertEquals("gt6", tData.cropOwner(), aMessage + ": owner");
			assertEquals(aCropId, tData.cropId(), aMessage + ": crop id");
			assertEquals(1, tData.growth(), aMessage + ": the neutral G byte (the GT_BaseCrop.java:77 literal)");
			assertEquals(1, tData.gain(), aMessage + ": the neutral Ga byte");
			assertEquals(1, tData.resistance(), aMessage + ": the neutral Re byte");
			assertEquals(4, tData.scanLevel(), aMessage + ": scan 4 = the full tooltip disclosure");
		}
	}
}
