package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import gregtech6.items.GT6LaserGas;

/**
 * Task p38-tabfix-b-energy — the creative-tab join census for the energy/equipment
 * family batch: every family of the card joins {@link GTMachines#MACHINES_TAB} through
 * its own {@code onBuildTabContents} handler (the GT6BurningBoxes verbatim form), and
 * this test pins the per-family join-coverage counts so a future registration cannot
 * silently land tab-less again (the issue #10 lesson — JEI 1.20.1 derives its item list
 * from the tab display items, so registered-but-tab-less is invisible in BOTH the
 * creative menu and JEI).
 *
 * <p><b>COUNT ERRATUM (declared, the BurningBoxRowTableTest precedent)</b>: the task
 * card's SPEC (from the p38-tab-census ledger) says 164 items, with
 * GT6ElectricDynamos=3 and GT6FluxDynamos=3. The code truth is 6 + 5 = 11 dynamo items —
 * the census evidence windows (:117-:121 / :88-:92) stopped at the T2 row while the
 * ladders continue to T5. GT6Kinetics likewise holds a fourth single-block item the
 * census lost: the water wheel (:545). The tests pin the code truth: 170 items across
 * the 18 files (17 card slots; the dynamo slot is two files). Coverage = what the
 * family's {@code onBuildTabContents} walk accepts: map-walk families are counted by
 * their item map size, single-{@link RegistryObject} families by their
 * {@code RegistryObject<Item>} field count (reflective — a newly added field breaks the
 * count until the join and this census are extended).
 *
 * <p>Pool-cut declaration (the card's declared deviation): upstream gives each family
 * its own MTE-registry category (Transformers 10041, LD Transport 10060, Lasers 10071,
 * Dynamos 10111, Quantum Energizers 10121, Crystal Chargers 10131, ZPM 14999, Magical
 * 10180, Hoppers 8010, Misc Tool Blocks 32720, Safes 2010 / Storage 32751,
 * Loader_MultiTileEntities.java per the census anchors); this port pools all the joins
 * into MACHINES_TAB (the GTBarrels:257 pooling precedent).
 *
 * <p>The offline assertion surface follows the GTWiresCreativeTabTest discipline: the
 * tab and the items are not resolvable in this bootstrapped-and-frozen JVM, but map
 * sizes and field counts are — the counts ARE the join coverage because every walk
 * enumerates its whole container.
 */
public class CreativeTabJoinCensusTest {

	/** The offline boot before the first registry-class touch (the GTWiresCreativeTabTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	// ---------------------------------------------------------------------------
	// the map-walk families — map size = join coverage
	// ---------------------------------------------------------------------------

	/** The nine transformer rows (:881-889), the map the walk enumerates. */
	@Test
	public void electricTransformersJoinNine() {
		assertEquals(9, GT6ElectricTransformers.ITEMS_BY_PATH.size());
	}

	/** The five LD transformer endpoints (:909-913). */
	@Test
	public void longDistanceTransformersJoinFive() {
		assertEquals(5, GT6LongDistanceTransformers.ITEMS_BY_PATH.size());
	}

	/** The 16 LD wire metas (Loader_Blocks.java:160). */
	@Test
	public void longDistWiresJoinSixteen() {
		assertEquals(16, GT6LongDistWires.ITEMS_BY_META.size());
	}

	/** The LD pipes: 16 metas + the two endpoints = 18 (:906-916). */
	@Test
	public void longDistPipesJoinEighteen() {
		assertEquals(16, GT6LongDistPipes.WIRE_ITEMS_BY_META.size());
		assertEquals(18, GT6LongDistPipes.WIRE_ITEMS_BY_META.size()
				+ itemFields(GT6LongDistPipes.class).size(), "16 metas + item/fluid endpoint pair");
	}

	/** The laser domain: 5 CO2 + 5 absorbers = 10 (:930-934 + :976-980). */
	@Test
	public void lasersJoinTen() {
		assertEquals(5, GT6Lasers.CO2_LASER_ITEMS_BY_PATH.size());
		assertEquals(5, GT6Lasers.LASER_ABSORBER_ITEMS_BY_PATH.size());
	}

	/** The five quantum energizers (:961-966). */
	@Test
	public void quantumEnergizersJoinFive() {
		assertEquals(5, GT6QuantumEnergizers.QUANTUM_ENERGIZER_ITEMS_BY_PATH.size());
	}

	/** The twenty crystal chargers (small + large x T0..T9, :969-972). */
	@Test
	public void crystalChargersJoinTwenty() {
		assertEquals(20, GT6CrystalChargers.ITEMS_BY_PATH.size());
	}

	/** The two ZPM dechargers (:1000-1001). */
	@Test
	public void zpmDechargersJoinTwo() {
		assertEquals(2, GT6ZpmDechargers.ITEMS_BY_PATH.size());
	}

	/** The single magic field absorber (:1005). */
	@Test
	public void magicAbsorbersJoinOne() {
		assertEquals(1, GT6MagicAbsorbers.MAGIC_ABSORBER_ITEMS_BY_PATH.size());
	}

	/** The 21 sensor rows (:1979-1999, the p34 erratum count). */
	@Test
	public void sensorsJoinTwentyOne() {
		assertEquals(21, GT6Sensors.ITEMS_BY_PATH.size());
	}

	/** The 12 attachments (6 taps + 6 funnels, :2108-2120). */
	@Test
	public void attachmentsJoinTwelve() {
		assertEquals(12, GT6Attachments.ITEMS_BY_PATH.size());
	}

	/** The 28 static storages (2 metals x 4 kinds + 10 planks x 2 wooden kinds). */
	@Test
	public void staticStoragesJoinTwentyEight() {
		assertEquals(28, GT6StaticStorages.ROWS.size());
		assertEquals(28, GT6StaticStorages.ITEMS_BY_PATH.size());
	}

	/** The four hoppers (Bronze/Steel x plain/queue, :145-146). */
	@Test
	public void hoppersJoinFour() {
		assertEquals(4, GT6Hoppers.ITEMS_BY_PATH.size());
	}

	// ---------------------------------------------------------------------------
	// the single-RO families — field count = join coverage
	// ---------------------------------------------------------------------------

	/** The electric dynamo ladder: 6 items (ULV + T1..T5) — the census said 3 (erratum). */
	@Test
	public void electricDynamosJoinSix() {
		assertEquals(6, itemFields(GT6ElectricDynamos.class).size());
	}

	/** The flux dynamo ladder: 5 items (T1..T5) — the census said 3 (erratum). */
	@Test
	public void fluxDynamosJoinFive() {
		assertEquals(5, itemFields(GT6FluxDynamos.class).size());
	}

	/** The kinetic single blocks: crank + gearbox + rotation transformer + water wheel = 4 (crank :2106). The axle/steam/diesel ladders are the p38 tail card's crop and stay out. */
	@Test
	public void kineticsJoinFour() {
		assertEquals(4, itemFields(GT6Kinetics.class).size());
	}

	/** The FE fixtures: sink battery + source = 2 (port-native, the p28 bridge). */
	@Test
	public void feBatteriesJoinTwo() {
		assertEquals(2, itemFields(GT6FeBatteries.class).size());
	}

	/** The laser gas emitters: empty + CO2 = 2 (MultiItemTechnological :384/:394). */
	@Test
	public void laserGasJoinsTwo() {
		assertEquals(2, itemFields(GT6LaserGas.class).size());
	}

	// ---------------------------------------------------------------------------
	// the census total + the join-handler presence
	// ---------------------------------------------------------------------------

	/** The 170-item total (the card's 164 + the 6-item erratum: 3+2 dynamo rows, 1 water wheel — see the class doc). */
	@Test
	public void theBatchTotalIsOneSeventy() {
		int tTotal = GT6ElectricTransformers.ITEMS_BY_PATH.size()
				+ GT6LongDistanceTransformers.ITEMS_BY_PATH.size()
				+ GT6LongDistWires.ITEMS_BY_META.size()
				+ GT6LongDistPipes.WIRE_ITEMS_BY_META.size() + itemFields(GT6LongDistPipes.class).size()
				+ GT6Lasers.CO2_LASER_ITEMS_BY_PATH.size() + GT6Lasers.LASER_ABSORBER_ITEMS_BY_PATH.size()
				+ GT6QuantumEnergizers.QUANTUM_ENERGIZER_ITEMS_BY_PATH.size()
				+ GT6CrystalChargers.ITEMS_BY_PATH.size()
				+ GT6ZpmDechargers.ITEMS_BY_PATH.size()
				+ GT6MagicAbsorbers.MAGIC_ABSORBER_ITEMS_BY_PATH.size()
				+ GT6Sensors.ITEMS_BY_PATH.size()
				+ GT6Attachments.ITEMS_BY_PATH.size()
				+ GT6StaticStorages.ITEMS_BY_PATH.size()
				+ GT6Hoppers.ITEMS_BY_PATH.size()
				+ itemFields(GT6ElectricDynamos.class).size()
				+ itemFields(GT6FluxDynamos.class).size()
				+ itemFields(GT6Kinetics.class).size()
				+ itemFields(GT6FeBatteries.class).size()
				+ itemFields(GT6LaserGas.class).size();
		assertEquals(170, tTotal, "the p38-tabfix-b-energy batch — 164 card + 6 erratum");
	}

	/**
	 * Every batch family declares its own {@code onBuildTabContents} handler — the join
	 * method the class-level MOD-bus {@code @Mod.EventBusSubscriber} delivers. A family
	 * losing its handler (or a new family skipping it) breaks this walk.
	 */
	@Test
	public void everyBatchFamilyDeclaresTheTabWalk() throws Exception {
		List<Class<?>> tFamilies = List.of(
				GT6ElectricTransformers.class, GT6LongDistanceTransformers.class, GT6LongDistWires.class,
				GT6LongDistPipes.class, GT6Lasers.class, GT6ElectricDynamos.class, GT6FluxDynamos.class,
				GT6QuantumEnergizers.class, GT6CrystalChargers.class, GT6ZpmDechargers.class,
				GT6MagicAbsorbers.class, GT6Sensors.class, GT6Attachments.class, GT6StaticStorages.class,
				GT6Hoppers.class, GT6Kinetics.class, GT6FeBatteries.class, GT6LaserGas.class);
		for (Class<?> tFamily : tFamilies) {
			Method tWalk = tFamily.getDeclaredMethod("onBuildTabContents", BuildCreativeModeTabContentsEvent.class);
			assertTrue(java.lang.reflect.Modifier.isStatic(tWalk.getModifiers()), tFamily.getSimpleName() + " walk");
		}
	}

	// ---------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------

	/**
	 * The {@code RegistryObject<Item>}-shaped fields of a family class (the single-RO
	 * coverage count). The holder type is matched BY NAME — forge = RegistryObject,
	 * neo = DeferredHolder (the W4 mechanical swap widens the generic, the LAST type
	 * argument stays the concrete Item type on both legs) — so this test needs no
	 * per-leg import seam.
	 */
	private static List<Field> itemFields(Class<?> aFamily) {
		return java.util.Arrays.stream(aFamily.getDeclaredFields())
				.filter(f -> {
					String tName = f.getType().getName();
					return tName.endsWith(".RegistryObject") || tName.endsWith(".DeferredHolder");
				})
				.filter(f -> f.getGenericType() instanceof ParameterizedType t
						&& t.getActualTypeArguments().length >= 1
						&& t.getActualTypeArguments()[t.getActualTypeArguments().length - 1] == net.minecraft.world.item.Item.class)
				.toList();
	}
}
