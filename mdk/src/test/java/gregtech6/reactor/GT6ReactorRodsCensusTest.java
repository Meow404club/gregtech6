package gregtech6.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.items.GT6ReactorRods;
import gregtech6.items.GT6ReactorRods.GT6ReactorRodItem;
import gregtech6.items.GT6ReactorRods.RodRow;
import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.reactor.neutron.ReactorRodKind;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The 46-rod census (task debt-reactor-c-rods acceptance ①): the ROWS table pinned
 * against the upstream registration (Loader_MultiTileEntities.java:741-790), the spec
 * 对表 against the A-card {@link FuelRodSpec}/{@link BreederRodSpec} tables (the single
 * source of the numbers) and the {@link IReactorRodItem#rodSwapTarget} chain closed
 * end-to-end — fuel 9210→9310 depleted (Nuclear.java:221) and breeder 9410→9411 product
 * (Breeder.java:86), the swap the 2x2 core BE consumes through the B-card seam.
 *
 * <p>The live swap arm needs BOUND items: the real rod items register under their real
 * gt6 paths through the offline registry latch (the GT6ReactorCore2x2Test fixture
 * discipline), which also resolves the DeferredRegister's lazy
 * RegistryObject/DeferredHolder suppliers — so {@link GT6ReactorRods#BY_ID} answers
 * without a mod launch.
 */
class GT6ReactorRodsCensusTest extends GTOfflineTestBase {

	/** Whether the live-registry face is answerable in this JVM (the neo item-fixture skip posture). */
	static boolean sLiveReady = false;

	@BeforeAll
	static void registerTheRealRods() {
		// the real items under their real paths — the DR never fires on the offline forge
		// boot, so the fixture window stands in. On the 21.1 leg the FML test harness may
		// have already registered the real mod (the DR fired), or the offline latch may be
		// unreachable (the fields renamed) — in BOTH cases the probe decides, and the class
		// never aborts (the structural tests run everywhere, the live arm skips instead).
		try {
			for (RodRow tRow : GT6ReactorRods.ROWS) {
				registerRodFixture(tRow);
			}
		} catch (org.opentest4j.TestAbortedException tLatchUnreachable) {
			// the telemetry leg — sLiveReady stays whatever the probe says
		}
		sLiveReady = GT6ReactorRods.rodById(9201)
				.map(tItem -> tItem instanceof GT6ReactorRodItem).orElse(false);
	}

	/** Register-or-reuse under the real path (the two boot postures, see registerTheRealRods). */
	private static void registerRodFixture(RodRow aRow) {
		try {
			registerItemFixture(aRow.path(), () -> new GT6ReactorRodItem(aRow));
		} catch (IllegalStateException | IllegalArgumentException tFailure) {
			// NOT silently swallowed: the vanilla ITEM registry is AIR-defaulted, so a
			// get() fallback would mask the failure — surface the cause instead
			throw new IllegalStateException("the fixture registration of " + aRow.path() + " failed: " + tFailure, tFailure);
		}
	}

	// ---------------------------------------------------------------------------
	// the census
	// ---------------------------------------------------------------------------

	/** The 46-item total + the per-kind distribution (LME:741-790: 4+17+17+4+4). */
	@Test
	void theFortySixRodCensusIsPinned() {
		assertEquals(46, GT6ReactorRods.ROWS.size(), "the LME:741-790 registration total");
		Map<ReactorRodKind, Long> tByKind = GT6ReactorRods.ROWS.stream()
				.collect(Collectors.groupingBy(RodRow::kind, Collectors.counting()));
		assertEquals(1L, tByKind.get(ReactorRodKind.EMPTY), ":741 the Empty Rod");
		assertEquals(1L, tByKind.get(ReactorRodKind.ABSORBER), ":742 the Absorber");
		assertEquals(1L, tByKind.get(ReactorRodKind.REFLECTOR), ":743 the Reflector");
		assertEquals(1L, tByKind.get(ReactorRodKind.MODERATOR), ":744 the Moderator");
		assertEquals(17L, tByKind.get(ReactorRodKind.FUEL), ":746-762 the fuel rods");
		assertEquals(17L, tByKind.get(ReactorRodKind.DEPLETED), ":764-780 the depleted rods");
		assertEquals(4L, tByKind.get(ReactorRodKind.BREEDER), ":782-785 the breeder rods");
		assertEquals(4L, tByKind.get(ReactorRodKind.PRODUCT), ":787-790 the enriched rods");
	}

	/** Ids and paths are unique — the swap dispatch and the item registry both assume it. */
	@Test
	void idsAndPathsAreUnique() {
		Set<Integer> tIds = new HashSet<>();
		Set<String> tPaths = new HashSet<>();
		for (RodRow tRow : GT6ReactorRods.ROWS) {
			assertTrue(tIds.add(tRow.id()), "duplicate id " + tRow.id());
			assertTrue(tPaths.add(tRow.path()), "duplicate path " + tRow.path());
		}
		// the DR mirrors the rows (the registration-face contract)
		assertEquals(GT6ReactorRods.ROWS.size(), GT6ReactorRods.RODS.size(), "RODS mirrors ROWS");
		for (int i = 0; i < GT6ReactorRods.ROWS.size(); i++) {
			assertEquals(GT6ReactorRods.ROWS.get(i).path(), GT6ReactorRods.RODS.get(i).getId().getPath(),
					"the DR entry path for row " + GT6ReactorRods.ROWS.get(i).id());
		}
	}

	/** Verbatim name-column pins (the LME name strings, the en lang face). */
	@Test
	void nameColumnPins() {
		assertEquals("Empty Reactor Rod", GT6ReactorRods.rowOf(9201).orElseThrow().name());
		assertEquals("Neutron Absorber Rod", GT6ReactorRods.rowOf(9202).orElseThrow().name());
		assertEquals("Thorium-232 Fuel Rod", GT6ReactorRods.rowOf(9210).orElseThrow().name());
		assertEquals("Naquadria Fuel Rod", GT6ReactorRods.rowOf(9261).orElseThrow().name());
		assertEquals("Depleted Enriched Naquadah Fuel Rod", GT6ReactorRods.rowOf(9360).orElseThrow().name());
		assertEquals("Lithium Breeder Rod", GT6ReactorRods.rowOf(9430).orElseThrow().name());
		assertEquals("Tritium Enriched Rod", GT6ReactorRods.rowOf(9431).orElseThrow().name());
		assertEquals("Enriched Naquadah Enriched Rod", GT6ReactorRods.rowOf(9441).orElseThrow().name());
	}

	// ---------------------------------------------------------------------------
	// the spec 对表
	// ---------------------------------------------------------------------------

	/** Every FUEL row binds the A-card FuelRodSpec of its id, depleted target = the spec edge. */
	@Test
	void fuelRowsBindTheACardSpecTable() {
		List<RodRow> tFuelRows = rowsOfKind(ReactorRodKind.FUEL);
		assertEquals(FuelRodSpec.RODS.size(), tFuelRows.size(), "the row table and the spec table agree");
		for (RodRow tRow : tFuelRows) {
			FuelRodSpec tSpec = FuelRodSpec.byId(tRow.id()).orElse(null);
			assertNotNull(tSpec, "no A-card spec for fuel row " + tRow.id());
			assertTrue(tRow.name().startsWith(tSpec.name()), tRow.name() + " carries the spec identity " + tSpec.name());
			RodRow tDepleted = GT6ReactorRods.rowOf(tSpec.depletedId()).orElse(null);
			assertNotNull(tDepleted, "depleted target missing for " + tRow.id());
			assertEquals(ReactorRodKind.DEPLETED, tDepleted.kind(), "the depleted target kind");
		}
		// the depleted ids are exactly the DEPLETED rows (no orphan either way)
		Set<Integer> tDepletedTargets = FuelRodSpec.RODS.stream().map(FuelRodSpec::depletedId).collect(Collectors.toSet());
		assertEquals(tDepletedTargets, rowsOfKind(ReactorRodKind.DEPLETED).stream().map(RodRow::id).collect(Collectors.toSet()));
	}

	/** Every BREEDER row binds the A-card BreederRodSpec; every PRODUCT row is exactly one breeder's product. */
	@Test
	void breederAndProductRowsBindTheACardSpecTable() {
		List<RodRow> tBreederRows = rowsOfKind(ReactorRodKind.BREEDER);
		assertEquals(BreederRodSpec.RODS.size(), tBreederRows.size(), "the row table and the spec table agree");
		for (RodRow tRow : tBreederRows) {
			BreederRodSpec tSpec = BreederRodSpec.byId(tRow.id()).orElse(null);
			assertNotNull(tSpec, "no A-card spec for breeder row " + tRow.id());
			RodRow tProduct = GT6ReactorRods.rowOf(tSpec.productId()).orElse(null);
			assertNotNull(tProduct, "product target missing for " + tRow.id());
			assertEquals(ReactorRodKind.PRODUCT, tProduct.kind(), "the product target kind");
		}
		// the reverse edges are a bijection (9411/9421/9431/9441, each bred from exactly one)
		Set<Integer> tProductIds = BreederRodSpec.RODS.stream().map(BreederRodSpec::productId).collect(Collectors.toSet());
		assertEquals(tProductIds, rowsOfKind(ReactorRodKind.PRODUCT).stream().map(RodRow::id).collect(Collectors.toSet()));
		for (RodRow tRow : rowsOfKind(ReactorRodKind.PRODUCT)) {
			long tBack = BreederRodSpec.RODS.stream().filter(r -> r.productId() == tRow.id()).count();
			assertEquals(1L, tBack, "exactly one breeder breeds " + tRow.id());
		}
	}

	// ---------------------------------------------------------------------------
	// the swapTarget chain
	// ---------------------------------------------------------------------------

	/** The registered instance of a row (raw GT6ReactorRodItem construction has no registry holder — the ItemStack ctor would demand a registration write). */
	private static GT6ReactorRodItem itemOf(int aId) {
		String tPath = GT6ReactorRods.rowOf(aId).orElseThrow().path();
		Item tItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath));
		return tItem instanceof GT6ReactorRodItem tRod ? tRod : null;
	}

	/** The depletion chain: fuel.rodSwapTarget(depletedId) = the DEPLETED rod, count 1. */
	@Test
	void theDepletionSwapChainIsClosed() {
		org.junit.jupiter.api.Assumptions.assumeTrue(sLiveReady, "the live registry is unanswerable in this JVM");
		for (RodRow tRow : rowsOfKind(ReactorRodKind.FUEL)) {
			GT6ReactorRodItem tItem = itemOf(tRow.id());
			int tDepletedId = FuelRodSpec.byId(tRow.id()).orElseThrow().depletedId();
			ItemStack tSwap = tItem.rodSwapTarget(tDepletedId);
			assertNotNull(tSwap, "the depletion swap dropped for " + tRow.id());
			assertEquals(1, tSwap.getCount(), "the swap carries the upstream count-1 meta swap");
			Item tExpected = itemOf(tDepletedId);
			assertSame(tExpected, tSwap.getItem(), "the swap target identity for " + tRow.id());
		}
	}

	/** The breeding chain: breeder.rodSwapTarget(productId) = the PRODUCT rod (Breeder.java:86). */
	@Test
	void theBreedingSwapChainIsClosed() {
		org.junit.jupiter.api.Assumptions.assumeTrue(sLiveReady, "the live registry is unanswerable in this JVM");
		for (RodRow tRow : rowsOfKind(ReactorRodKind.BREEDER)) {
			GT6ReactorRodItem tItem = itemOf(tRow.id());
			int tProductId = BreederRodSpec.byId(tRow.id()).orElseThrow().productId();
			ItemStack tSwap = tItem.rodSwapTarget(tProductId);
			assertNotNull(tSwap, "the breeding swap dropped for " + tRow.id());
			Item tExpected = itemOf(tProductId);
			assertSame(tExpected, tSwap.getItem(), "the swap target identity for " + tRow.id());
			// the fresh product has no burn state (the swap clears the stack — Nuclear.java:222)
			assertEquals(0L, ReactorRodNbt.durability(tSwap), "the swapped product carries no burn budget");
		}
	}

	/** The unknown-target arm: a rod outside the map answers null (the fixture seam semantics). */
	@Test
	void unknownSwapTargetAnswersNull() {
		org.junit.jupiter.api.Assumptions.assumeTrue(sLiveReady, "the live registry is unanswerable in this JVM");
		GT6ReactorRodItem tItem = itemOf(9201);
		assertNull(tItem.rodSwapTarget(9999), "the fixture seam arm");
		assertNull(tItem.rodSwapTarget(0), "the zero sentinel arm");
	}

	/** The behavioural face: kind/spec answers are row-driven and stack-independent. */
	@Test
	void theKindAndSpecFaceMatchesTheRow() {
		org.junit.jupiter.api.Assumptions.assumeTrue(sLiveReady, "the live registry is unanswerable in this JVM");
		GT6ReactorRodItem tFuel = itemOf(9210);
		assertEquals(ReactorRodKind.FUEL, tFuel.rodKind(ItemStack.EMPTY));
		assertNotNull(tFuel.fuelSpec(ItemStack.EMPTY), "the fuel spec answers");
		assertEquals(12_000_000_000L, tFuel.fuelSpec(ItemStack.EMPTY).durability(), "the A-card durability column");
		assertNull(tFuel.breederSpec(ItemStack.EMPTY), "fuel answers no breeder spec");

		GT6ReactorRodItem tBreeder = itemOf(9430);
		assertEquals(ReactorRodKind.BREEDER, tBreeder.rodKind(ItemStack.EMPTY));
		assertEquals(640_000L, tBreeder.breederSpec(ItemStack.EMPTY).needed(), "the A-card needed column");
		assertNull(tBreeder.fuelSpec(ItemStack.EMPTY), "breeder answers no fuel spec");

		GT6ReactorRodItem tEmpty = itemOf(9201);
		assertEquals(ReactorRodKind.EMPTY, tEmpty.rodKind(ItemStack.EMPTY));
		assertNull(tEmpty.fuelSpec(ItemStack.EMPTY), "empty answers no fuel spec");
		assertNull(tEmpty.breederSpec(ItemStack.EMPTY), "empty answers no breeder spec");
	}

	// ---------------------------------------------------------------------------

	private static List<RodRow> rowsOfKind(ReactorRodKind aKind) {
		return GT6ReactorRods.ROWS.stream().filter(r -> r.kind() == aKind).toList();
	}
}
