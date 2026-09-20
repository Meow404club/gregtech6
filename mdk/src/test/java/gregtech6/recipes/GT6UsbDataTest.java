package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.items.GT6UsbSticks;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The p32-usb-data pins (the card acceptance): ① the registration/asset witnesses (the
 * generated-tree census, the ArmorSetTest posture — the mod-Item intrusive-holder wall
 * keeps the live registry out of offline reach, the committed generated files + the RCON
 * give chain are the registration witnesses), ② the material-data NBT carrier round trip
 * (the RecipeMapScannerMolecular.java:58-60 write shape over the CS.java:1276/:1277/:1281
 * keys), and ③ the static-row constants re-pin (the replicator official row =
 * Loader_Recipes_Other.java:929 verbatim; the scanner stand-in stays the pinned 512/512).
 *
 * <p>Offline harness: the fixture item is registered under a fixture key through the
 * momentarily-opened registry latch (the GT6BatteryItemTest posture — the ItemStack ctor
 * needs a registry delegate); carrier tests assume-skip where the latch fields are
 * unreachable (the 1.20.1 leg gates, the 21.1 leg records).
 */
public class GT6UsbDataTest extends GTOfflineTestBase {

	// ------------------------------------------------------------------ the latch (the GT6BatteryItemTest posture)

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
			tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
			Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass();
			tLocked = tUnsafe.objectFieldOffset(findField(tClass, "locked"));
			tFrozen = tUnsafe.objectFieldOffset(findField(tClass, "frozen"));
		} catch (Throwable ignored) {
			tArmed = false; // the telemetry leg: carrier tests assume-skip
		}
		UNSAFE = tUnsafe;
		LOCKED_OFFSET = tLocked;
		FROZEN_OFFSET = tFrozen;
		ARMED = tArmed;
	}

	/** The latch fields live on wrapper superclasses — walk up (getDeclaredField sees one class only). */
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

	static GT6UsbSticks.GT6UsbStickItem registerFixture(String aKey, java.util.function.Supplier<GT6UsbSticks.GT6UsbStickItem> aItem) {
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

	@BeforeAll
	static void warmUp() {
		gregtech6.registry.GTMaterialItems.initMaterials(); // idempotent — the offline material universe (the QuSmokeRowsPourTest posture)
	}

	/**
	 * The fixture seat, LAZY: the latch's assumeTrue lives here, not in {@link #warmUp} —
	 * a @BeforeAll assumption aborts the WHOLE class as skipped, which would bench the
	 * latch-free row/witness tests on the 21.1 leg too. Only the carrier tests pay the
	 * assume (the GT6BatteryItemTest telemetry doctrine: the 1.20.1 leg gates the carrier
	 * semantics, the 21.1 leg records).
	 */
	static GT6UsbSticks.GT6UsbStickItem stick() {
		if (sStick == null) {
			sStick = registerFixture("fixture_usb_stick_3", () -> new GT6UsbSticks.GT6UsbStickItem(new Item.Properties(), (byte)3));
		}
		return sStick;
	}

	// ------------------------------------------------------------------ ③ the row pour (the QuSmokeRowsPourTest posture)

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId -> switch (aId.getPath()) {
			case "ender_pearl" -> Items.ENDER_PEARL;
			case "paper" -> Items.PAPER; // the scanner stand-in's data medium
			// task p32-qu-scanner-replicator — the six gem-tier stand-ins of the :941-946 rows
			case "gem_chipped_redstone", "gem_flawed_redstone", "gem_redstone",
					"gem_flawless_redstone", "gem_exquisite_redstone", "gem_legendary_redstone" -> Items.DIAMOND;
			default -> Items.AIR; // a miss is LOUD (the unregistered-id bad row)
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
		GT6RecipeMaps.reset();
	}

	/** Reads one shipped row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6UsbDataTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(java.util.Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	/**
	 * ③ The replicator map holds the official row PLUS the molten-redstone six (task
	 * p32-qu-scanner-replicator landed them) — the :929 constants re-pinned here, the
	 * :941-946 six pinned exhaustively in GT6QuMachinesTest; the :912/:934-939 _TE compat
	 * rows stay unmounted, the TF trophy rows are the card's compat cut.
	 */
	@Test
	public void theReplicatorMapHoldsExactlyTheOfficialEnderRow() throws Exception {
		pourShipped("replicator");
		assertEquals(7, GT6RecipeMaps.REPLICATOR.mRecipeList.size(), "the :929 row + the :941-946 six — zero compat rows");
		Recipe tRow = null;
		for (Recipe tScan : GT6RecipeMaps.REPLICATOR.mRecipeList) {
			if (tScan.mOutputs.length == 1 && tScan.mOutputs[0].getItem() == Items.ENDER_PEARL) tRow = tScan;
		}
		assertNotNull(tRow, "the :929 official row");
		assertEquals(16L, tRow.mEUt, "the :929 eut 16");
		assertEquals(144L, tRow.mDuration, "the :929 duration 144");
		assertEquals(1, tRow.mFluidInputs.length, "one fluid input");
		assertEquals(144, tRow.mFluidInputs[0].getAmount(), "one L-unit of molten enderpearls = 144 mB");
		assertEquals(1, tRow.mOutputs.length, "one item output");
		assertTrue(Items.ENDER_PEARL == tRow.mOutputs[0].getItem(), "the replicated ender pearl");
		assertEquals(0, tRow.mInputs.length, "the replicator row is addRecipe1 over the special slot — zero grid inputs");
	}

	/** ③ The scanner stand-in keeps the pinned constants (the QuSmokeRowsPourTest values, unchanged by this card). */
	@Test
	public void theScannerStandInKeepsThePinnedConstants() throws Exception {
		pourShipped("scannermolecular");
		assertEquals(1, GT6RecipeMaps.SCANNER_MOLECULAR.mRecipeList.size(), "the declared stand-in row");
		Recipe tRow = GT6RecipeMaps.SCANNER_MOLECULAR.mRecipeList.iterator().next();
		assertEquals(512L, tRow.mEUt, "the scan eut 512 (RecipeMapScannerMolecular.java:57 power face)");
		assertEquals(512L, tRow.mDuration, "the pinned duration 512");
		assertEquals(2, tRow.mInputs.length, "the 2-in map shape (scanned + data medium)");
		assertEquals(1, tRow.mOutputs.length, "the 1-out map shape (the medium back)");
	}

	// ------------------------------------------------------------------ ② the data carrier

	/** A fresh stick carries no data (the Behavior_DataStorage "This Stick is Empty" face). */
	@Test
	public void aFreshStickReadsEmpty() {
		Assumptions.assumeTrue(ARMED);
		ItemStack tStack = new ItemStack(stick());
		assertEquals((byte)0, GT6UsbSticks.readTier(tStack), "no tier byte on a fresh stack");
		assertNull(GT6UsbSticks.readData(tStack), "no data compound on a fresh stack");
		assertEquals(0, GT6UsbSticks.readMaterialId(tStack), "no material id");
		assertNull(GT6UsbSticks.materialOf(tStack), "no material");
	}

	/** The scanner write shape round-trips: material id short + the tier-3 byte, over the stack copy. */
	@Test
	public void theMaterialDataRoundTrips() {
		Assumptions.assumeTrue(ARMED);
		ItemStack tStack = new ItemStack(stick());
		OreDictMaterial tMaterial = MT.Iron;
		assertTrue(tMaterial.mID > 0, "the fixture material has a real registry id");
		GT6UsbSticks.writeMaterialData(tStack, tMaterial);
		assertEquals(GT6UsbSticks.TIER_SCANNER_WRITE, GT6UsbSticks.readTier(tStack), "the :60 tier-3 byte");
		assertEquals(tMaterial.mID, GT6UsbSticks.readMaterialId(tStack), "the :59 material short");
		assertTrue(tMaterial == GT6UsbSticks.materialOf(tStack), "the id resolves back to the material");
		// the copy face: the carrier survives the stack copy (the GT6BatteryItemTest round trip)
		assertEquals(tMaterial.mID, GT6UsbSticks.readMaterialId(tStack.copy()), "the carrier survives the copy");
	}

	/** The carrier keys are the upstream CS literals (the W2 consumers key on these exact strings). */
	@Test
	public void theCarrierKeysAreTheUpstreamLiterals() {
		assertEquals("gt.usb.tier", GT6UsbSticks.NBT_USB_TIER, "CS.java:1276");
		assertEquals("gt.usb.data", GT6UsbSticks.NBT_USB_DATA, "CS.java:1277");
		assertEquals("gt.replicator.data", GT6UsbSticks.NBT_REPLICATOR_DATA, "CS.java:1281");
	}

	/** The written compound is inspectable at the NBT face (the RCON data-merge probe parity). */
	@Test
	public void theWrittenCompoundCarriesTheShortFace() {
		Assumptions.assumeTrue(ARMED);
		ItemStack tStack = new ItemStack(stick());
		GT6UsbSticks.writeMaterialData(tStack, MT.Iron);
		CompoundTag tData = GT6UsbSticks.readData(tStack);
		assertNotNull(tData, "the data compound exists after the write");
		assertTrue(tData.getShort(GT6UsbSticks.NBT_REPLICATOR_DATA) == MT.Iron.mID, "the short face inside the compound");
	}

	// ------------------------------------------------------------------ ① the registration witnesses

	/** The classpath text of one generated file (the ArmorSetTest face). */
	private static String generated(String aPath) throws Exception {
		try (InputStream tStream = GT6UsbDataTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated file must be committed: " + aPath);
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * ① The generated-tree census: the four item models + the four tier tags + the en/zh
	 * lang keys must be committed — the offline half of the FML-registration proof (the
	 * live half is the RCON p32_qu_usb give chain).
	 */
	@Test
	public void theGeneratedTreeCarriesTheRegistrationFace() throws Exception {
		for (int tTier = 1; tTier <= 4; tTier++) {
			var tModel = JsonParser.parseString(generated("assets/gt6/models/item/usb_stick_" + tTier + ".json")).getAsJsonObject();
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), "the stick model parent");
			assertEquals("gt6:item/usb_stick_" + tTier, tModel.getAsJsonObject("textures").get("layer0").getAsString(), "the stick texture leg");
			var tTag = JsonParser.parseString(generated("data/gt6/tags/items/usb_stick_" + tTier + ".json")).getAsJsonObject();
			assertEquals(1, tTag.getAsJsonArray("values").size(), "one member per tier tag");
			assertEquals("gt6:usb_stick_" + tTier, tTag.getAsJsonArray("values").get(0).getAsString(), "the tier member id");
			String tEn = generated("assets/gt6/lang/en_us.json");
			assertTrue(tEn.contains("\"item.gt6.usb_stick_" + tTier + "\": \"USB " + tTier + ".0 Stick\""), "the en display name");
			assertTrue(tEn.contains("\"item.gt6.usb_stick_" + tTier + ".tooltip\": \"Stores Data\""), "the en tooltip");
			String tZh = generated("assets/gt6/lang/zh_cn.json");
			assertTrue(tZh.contains("\"item.gt6.usb_stick_" + tTier + "\": \"USB " + tTier + ".0\""), "the zh dump face");
			assertTrue(tZh.contains("\"item.gt6.usb_stick_" + tTier + ".tooltip\": \"储存数据\""), "the zh tooltip");
		}
	}
}
