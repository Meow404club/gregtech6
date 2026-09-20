package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.items.GT6UsbSticks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The p32-qu-scanner-replicator offline pins (the card acceptance ③ + the runtime-chain
 * face): ① the molten-redstone six ride the replicator map at the Loader_Recipes_Other
 * .java:941-946 constants verbatim (L = 144, CS.java:129), ② the scanner synthesis writes
 * the gt.replicator.data + tier-3 carrier through a one-time unbuffered row at
 * (protons+neutrons)×512 eUt / 512 t (RecipeMapScannerMolecular.java:57-61), ③ the
 * replicator synthesis consumes the USB data + the nucleon matter legs at
 * (protons+neutrons)×256 eUt / 1 t with the stick never-consumed (RecipeMapReplicator
 * .java:88-114 + the ST.amount(0, aUSB) zero-consume face).
 *
 * <p>Offline harness (the GT6UsbDataTest posture): the fixture items register through the
 * momentarily-opened registry latch; the map-level resolver seams take synthetic
 * stand-ins (the massfab/fusion resolver form).
 */
public class GT6QuMachinesTest extends GTOfflineTestBase {

	// ------------------------------------------------------------------ the latch (the GT6UsbDataTest posture)

	static final sun.misc.Unsafe UNSAFE;
	static final long LOCKED_OFFSET;
	static final long FROZEN_OFFSET;
	static final boolean ARMED;
	static {
		sun.misc.Unsafe tUnsafe = null;
		long tLocked = 0, tFrozen = 0;
		boolean tArmed = true;
		try {
			java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tUnsafeField.setAccessible(true);
			tUnsafe = (sun.misc.Unsafe)tUnsafeField.get(null);
			Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass();
			tLocked = tUnsafe.objectFieldOffset(findField(tClass, "locked"));
			tFrozen = tUnsafe.objectFieldOffset(findField(tClass, "frozen"));
		} catch (Throwable ignored) {
			tArmed = false; // the telemetry leg: latch-bearing tests assume-skip
		}
		UNSAFE = tUnsafe;
		LOCKED_OFFSET = tLocked;
		FROZEN_OFFSET = tFrozen;
		ARMED = tArmed;
	}

	private static java.lang.reflect.Field findField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tWalk = aClass; tWalk != null; tWalk = tWalk.getSuperclass()) {
			try {
				return tWalk.getDeclaredField(aName);
			} catch (NoSuchFieldException ignored) {
				// keep walking
			}
		}
		throw new NoSuchFieldException(aName + " (walked " + aClass + " up)");
	}

	static void unlockItemRegistry() {
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, LOCKED_OFFSET, false);
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, FROZEN_OFFSET, false);
	}

	static void lockItemRegistry() {
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, FROZEN_OFFSET, true);
		UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, LOCKED_OFFSET, true);
	}

	static Item registerFixture(String aKey, java.util.function.Supplier<Item> aItem) {
		Assumptions.assumeTrue(ARMED, "the offline registry latch is unreachable on this JVM");
		unlockItemRegistry();
		try {
			return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
					new ResourceLocation("gt6", aKey), aItem.get());
		} finally {
			lockItemRegistry();
		}
	}

	static GT6UsbSticks.GT6UsbStickItem sStick;
	static MaterialPrefixItem sScannedGem;

	/** The fixture seat, LAZY (the @BeforeAll assumption would bench the whole class). */
	static GT6UsbSticks.GT6UsbStickItem stick() {
		if (sStick == null) {
			sStick = (GT6UsbSticks.GT6UsbStickItem)registerFixture("fixture_qu_usb_stick_3",
					() -> new GT6UsbSticks.GT6UsbStickItem(new Item.Properties(), (byte)3));
		}
		return sStick;
	}

	/** A SCANNABLE-prefixed gem item over MT.Hydrogen (the synthetic (prefix, material) pairing the seams read). */
	static MaterialPrefixItem scannedGem() {
		if (sScannedGem == null) {
			sScannedGem = (MaterialPrefixItem)registerFixture("fixture_qu_scanned_gem",
					() -> new MaterialPrefixItem(new Item.Properties(), OP.gem, MT.H));
		}
		return sScannedGem;
	}

	@BeforeAll
	static void warmUp() {
		GTMaterialItems.initMaterials(); // idempotent — the offline material universe
	}

	// ------------------------------------------------------------------ the pour harness (the UsbDataTest posture)

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The six gem-tier ids of :941-946, in upstream row order — composed by the single id rule. LAZY: the composition reads OP prefixes, which only exist after {@link #warmUp}. */
	private static volatile String[] sRedstoneGemIds = null;

	private static String[] redstoneGemIds() {
		String[] tTable = sRedstoneGemIds;
		if (tTable == null) {
			sRedstoneGemIds = tTable = new String[] {
					GTMaterialItems.itemIdOf(OP.gemChipped, MT.Redstone),
					GTMaterialItems.itemIdOf(OP.gemFlawed, MT.Redstone),
					GTMaterialItems.itemIdOf(OP.gem, MT.Redstone),
					GTMaterialItems.itemIdOf(OP.gemFlawless, MT.Redstone),
					GTMaterialItems.itemIdOf(OP.gemExquisite, MT.Redstone),
					GTMaterialItems.itemIdOf(OP.gemLegendary, MT.Redstone)};
		}
		return tTable;
	}

	/** The :941-946 fluid amounts — L/4..L*8 over L = 144 (CS.java:129). */
	private static final int[] REDSTONE_MB = {36, 72, 144, 288, 576, 1152};

	/** The :941-946 durations — 72/4..72*8. */
	private static final long[] REDSTONE_DURATIONS = {18, 36, 72, 144, 288, 576};

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			String tPath = aId.getPath();
			if (tPath.equals("ender_pearl")) return Items.ENDER_PEARL;
			for (int i = 0; i < REDSTONE_MB.length; i++) {
				if (tPath.equals(redstoneGemIds()[i])) return Items.DIAMOND; // the six gem stand-ins
			}
			return Items.AIR; // a miss is LOUD (the unregistered-id bad row)
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> switch (aId.getPath()) {
			case "enderpearl_molten", "redstone_molten" -> Fluids.LAVA; // identity stand-in
			default -> Fluids.EMPTY;
		};
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems;
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		gregtech6.recipes.maps.GT6RecipeMapReplicator.sMatterFluidResolver = sProdFluidResolver;
		gregtech6.recipes.maps.GT6RecipeMapReplicator.sMaterialItemResolver = sProdItemResolver;
		GT6RecipeMaps.reset();
	}

	/** Reads one shipped row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6QuMachinesTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(java.util.Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	// ------------------------------------------------------------------ ① the redstone six (:941-946 verbatim)

	/**
	 * ① The replicator map holds the :929 Ender row PLUS the molten-redstone six at the
	 * :941-946 constants — the shipped ids are the composition-rule ids (a typo is a LOUD
	 * bad row at load and a silently missing NEI row).
	 */
	@Test
	public void theReplicatorMapCarriesTheMoltenRedstoneSix() throws Exception {
		// the shipped output ids match the composition rule (the QuSmokeRowsPourTest massfab form)
		String tJson = shippedJson("replicator");
		for (int i = 0; i < REDSTONE_MB.length; i++) {
			assertTrue(tJson.contains("\"item\": \"gt6:" + redstoneGemIds()[i] + "\""),
					"the shipped :94" + (1 + i) + " output id matches the composition (" + redstoneGemIds()[i] + ")");
		}
		pourShipped("replicator");
		assertEquals(7, GT6RecipeMaps.REPLICATOR.mRecipeList.size(), "the :929 Ender row + the :941-946 six");
		for (int i = 0; i < REDSTONE_MB.length; i++) {
			Recipe tRow = findRedstoneRow(REDSTONE_MB[i]);
			assertNotNull(tRow, "the :94" + (1 + i) + " row poured");
			assertEquals(16L, tRow.mEUt, "the :94" + (1 + i) + " eut 16");
			assertEquals(REDSTONE_DURATIONS[i], tRow.mDuration, "the :94" + (1 + i) + " duration 72-based");
			assertEquals(1, tRow.mFluidInputs.length, "one fluid input");
			assertTrue(tRow.mFluidInputs[0].getFluid() == Fluids.LAVA, "the redstone carrier stand-in");
			assertEquals(REDSTONE_MB[i], tRow.mFluidInputs[0].getAmount(), "the L/4..L*8 matter amount");
			assertEquals(1, tRow.mOutputs.length, "one gem-tier output");
			assertEquals(0, tRow.mInputs.length, "the ST.tag selectors ride as nothing (the :929 ruling)");
		}
	}

	/** The shipped JSON text of one row file (the composition-check face). */
	private String shippedJson(String aMapKey) throws Exception {
		try (InputStream tStream = GT6QuMachinesTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aMapKey + ".json")) {
			assertNotNull(tStream);
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private Recipe findRedstoneRow(int aMB) {
		for (Recipe tRow : GT6RecipeMaps.REPLICATOR.mRecipeList) {
			// the gem rows carry the DIAMOND stand-in — distinguishes them from the :929 ender
			// row, whose stub output is the ender pearl (the 144 mB amount collides with :943)
			if (tRow.mFluidInputs.length == 1 && tRow.mFluidInputs[0].getAmount() == aMB
					&& tRow.mFluidInputs[0].getFluid() == Fluids.LAVA
					&& tRow.mOutputs.length == 1 && tRow.mOutputs[0].getItem() == Items.DIAMOND) return tRow;
		}
		return null;
	}

	// ------------------------------------------------------------------ ② the scanner synthesis

	/** The scanner arm: gem + stick in → the stick back with the material id short + the tier-3 byte, unbuffered, 512/512. */
	@Test
	public void theScannerSynthesisWritesTheReplicatorData() {
		GT6RecipeMaps.SCANNER_MOLECULAR.mRecipeList.clear();
		ItemStack tGem = new ItemStack(scannedGem(), 1);
		ItemStack tStick = new ItemStack(stick(), 1);
		Recipe tRow = GT6RecipeMaps.SCANNER_MOLECULAR.findRecipe(null, 1024, ItemStack.EMPTY, null, tGem, tStick);
		assertNotNull(tRow, "the synthesis answers a SCANNABLE gem + a T3 stick");
		assertEquals(512L, tRow.mDuration, "the :57 duration 512");
		assertEquals((MT.H.mProtons + MT.H.mNeutrons) * 512L, tRow.mEUt, "the :57 power face (protons+neutrons)×512");
		assertFalse(tRow.mCanBeBuffered, "the one-time row never caches (:57 aCanBeBuffered F)");
		assertEquals(2, tRow.mInputs.length, "scanned + stick consumed");
		assertEquals(1, tRow.mOutputs.length, "the stick back");
		ItemStack tWritten = tRow.mOutputs[0];
		assertEquals(GT6UsbSticks.TIER_SCANNER_WRITE, GT6UsbSticks.readTier(tWritten), "the :60 tier-3 byte");
		assertEquals(MT.H.mID, GT6UsbSticks.readMaterialId(tWritten), "the :59 material short");
		CompoundTag tData = GT6UsbSticks.readData(tWritten);
		assertNotNull(tData, "the data compound exists");
	}

	/** The negative faces: a non-SCANNABLE item or a data-less stick-less input set never synthesizes. */
	@Test
	public void theScannerSynthesisRejectsUnscannableInputs() {
		Recipe tRow = GT6RecipeMaps.SCANNER_MOLECULAR.findRecipe(null, 1024, ItemStack.EMPTY, null,
				new ItemStack(Items.DIAMOND, 1), new ItemStack(stick(), 1));
		assertNull(tRow, "a vanilla item carries no material data — no synthesis");
		Recipe tTwoGems = GT6RecipeMaps.SCANNER_MOLECULAR.findRecipe(null, 1024, ItemStack.EMPTY, null,
				new ItemStack(scannedGem(), 1), new ItemStack(scannedGem(), 1));
		assertNull(tTwoGems, "no stick in the input set — no synthesis");
	}

	// ------------------------------------------------------------------ ③ the replicator synthesis

	/** The replicator arm over hydrogen: charged-matter 1 mB (the NF neutral leg skipped), eUt 256, duration 1, the stick never-consumed. */
	@Test
	public void theReplicatorSynthesisReplicatesHydrogen() {
		injectStubs();
		ItemStack tStick = new ItemStack(stick(), 1);
		GT6UsbSticks.writeMaterialData(tStick, MT.H);
		Recipe tRow = GT6RecipeMaps.REPLICATOR.findRecipe(null, 256, ItemStack.EMPTY, new FluidStack[0], tStick);
		assertNotNull(tRow, "the synthesis answers the data-bearing stick");
		assertEquals(1L, tRow.mDuration, "the :94 duration 1");
		assertEquals((MT.H.mProtons + MT.H.mNeutrons) * 256L, tRow.mEUt, "the :91 power face (protons+neutrons)×256 = 256");
		assertFalse(tRow.mCanBeBuffered, "the setNoBuffering face");
		assertEquals(1, tRow.mFluidInputs.length, "the NF neutral leg is skipped (mNeutrons = 0)");
		assertEquals(1, tRow.mFluidInputs[0].getAmount(), "1 mB = 1 proton");
		assertTrue(tRow.mFluidInputs[0].getFluid() == Fluids.WATER, "the charged-matter stand-in");
		assertEquals(1, tRow.mOutputs.length, "the replicated dust");
		assertTrue(Items.STICK == tRow.mOutputs[0].getItem(), "the item-walk output stand-in");
		assertEquals(1, tRow.mInputs.length, "the stick rides the input match");
		assertTrue(Recipe.sNotConsumable.test(tRow.mInputs[0]), "the stick is the never-consumed medium (the ST.amount(0, aUSB) face)");
	}

	/** The replicator arm over iron: both matter legs, the ingot walk, eUt 56×256. */
	@Test
	public void theReplicatorSynthesisReplicatesIron() {
		injectStubs();
		ItemStack tStick = new ItemStack(stick(), 1);
		GT6UsbSticks.writeMaterialData(tStick, MT.Iron);
		Recipe tRow = gregtech6.recipes.maps.GT6RecipeMapReplicator.getReplicatorRecipe(MT.Iron, tStick);
		assertNotNull(tRow, "iron is UUM-synthesisable and not antimatter");
		assertEquals(14336L, tRow.mEUt, "(26 + 30) × 256");
		assertEquals(2, tRow.mFluidInputs.length, "the neutral + charged legs both exist");
		assertEquals(30, tRow.mFluidInputs[0].getAmount(), "30 neutrons");
		assertEquals(26, tRow.mFluidInputs[1].getAmount(), "26 protons");
		assertEquals(1, tRow.mOutputs.length, "the ingot walk (gem/plateGem miss for iron → ingot)");
		assertTrue(Items.IRON_INGOT == tRow.mOutputs[0].getItem(), "the ingot stand-in");
	}

	/** The gate: a data-less stick synthesizes nothing (the :80 fall-through face). */
	@Test
	public void theReplicatorSynthesisRejectsNonReplicableData() {
		injectStubs();
		assertNull(GT6RecipeMaps.REPLICATOR.findRecipe(null, 1024, ItemStack.EMPTY, new FluidStack[0], new ItemStack(stick(), 1)),
				"a fresh stick carries no data");
	}

	/** The production resolver bindings, captured BEFORE any injection (the static-seam swap-back rule). */
	private static final java.util.function.Function<String, Fluid> sProdFluidResolver = gregtech6.recipes.maps.GT6RecipeMapReplicator.sMatterFluidResolver;
	private static final java.util.function.BiFunction<gregapi.oredict.OreDictPrefix, OreDictMaterial, Item> sProdItemResolver = gregtech6.recipes.maps.GT6RecipeMapReplicator.sMaterialItemResolver;

	/** Injects the resolver stand-ins (water = charged, lava = neutral, stick/ingot = the walk outputs); restored by teardownGeneration. */
	private void injectStubs() {
		gregtech6.recipes.maps.GT6RecipeMapReplicator.sMatterFluidResolver = aHalf -> aHalf.equals("charged") ? Fluids.WATER : Fluids.LAVA;
		gregtech6.recipes.maps.GT6RecipeMapReplicator.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aMaterial == MT.H && aPrefix == OP.dust) return Items.STICK;
			if (aMaterial == MT.Iron && aPrefix == OP.ingot) return Items.IRON_INGOT;
			return null;
		};
	}
}
