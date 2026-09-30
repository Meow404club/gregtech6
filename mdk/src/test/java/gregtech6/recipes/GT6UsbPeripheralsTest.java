package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import gregapi.data.MT;
import gregtech6.items.GT6UsbSticks;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The usb-peripherals pins (the card acceptance): ① the registration/asset
 * witnesses (the GT6UsbDataTest census posture — the generated item models, the eight
 * tier tags, the en/zh lang faces, the committed generated files are the offline half
 * of the FML-registration proof), ② the eight crafting rows verbatim (:808-811 the
 * cable grids, :819-822 the HDD grids, the CABLES_01[6] graphene quirk included), and
 * ③ the {@code Behavior_DataStorage16} data carrier over the CS.java:1278
 * {@code gt.usb.drive} key (the GTOfflineTestBase registerItemFixture latch posture).
 *
 * <p>The HDD_1 upstream TODO (the :819 record→CD tail comment) is an upstream
 * placeholder — the port keeps OD.record → #minecraft:music_discs (the
 * coordinator-approved mapping), nothing of the TODO is implemented here (the card's
 * explicit 不做 clause).
 */
public class GT6UsbPeripheralsTest extends GTOfflineTestBase {

	@BeforeAll
	static void warmUp() {
		gregtech6.registry.GTMaterialItems.initMaterials(); // idempotent — the offline material universe
	}

	/** The classpath text of one generated file (the GT6UsbDataTest face). */
	private static String generated(String aPath) throws Exception {
		try (InputStream tStream = GT6UsbPeripheralsTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated file must be committed: " + aPath);
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	// ------------------------------------------------------------------ ① the registration witnesses

	/**
	 * ① The generated-tree census: the 8 peripheral item models + the 8 tier tags + the
	 * en/zh lang rows (16 usb keys + the exempted He emitter's 2) must be committed.
	 */
	@Test
	public void theGeneratedTreeCarriesThePeripheralRegistrationFace() throws Exception {
		for (int tTier = 1; tTier <= 4; tTier++) {
			var tCableModel = JsonParser.parseString(generated("assets/gt6/models/item/usb_cable_" + tTier + ".json")).getAsJsonObject();
			assertEquals("minecraft:item/generated", tCableModel.get("parent").getAsString(), "the cable model parent");
			assertEquals("gt6:item/usb_cable_" + tTier, tCableModel.getAsJsonObject("textures").get("layer0").getAsString(), "the cable texture leg");
			var tDriveModel = JsonParser.parseString(generated("assets/gt6/models/item/usb_drive_" + tTier + ".json")).getAsJsonObject();
			assertEquals("gt6:item/usb_drive_" + tTier, tDriveModel.getAsJsonObject("textures").get("layer0").getAsString(), "the drive texture leg");
			var tCableTag = JsonParser.parseString(generated("data/gt6/tags/items/usb_cable_" + tTier + ".json")).getAsJsonObject();
			assertEquals("gt6:usb_cable_" + tTier, tCableTag.getAsJsonArray("values").get(0).getAsString(), "the cable tier member id");
			var tDriveTag = JsonParser.parseString(generated("data/gt6/tags/items/usb_drive_" + tTier + ".json")).getAsJsonObject();
			assertEquals("gt6:usb_drive_" + tTier, tDriveTag.getAsJsonArray("values").get(0).getAsString(), "the drive tier member id");
		}
		String tEn = generated("assets/gt6/lang/en_us.json");
		assertTrue(tEn.contains("\"item.gt6.usb_cable_1\": \"USB 1.0 Cable\""), "the en cable name");
		assertTrue(tEn.contains("\"item.gt6.usb_cable_4.tooltip\": \"Replaces USB Sticks when connected to USB Ports\""), "the en cable tooltip");
		assertTrue(tEn.contains("\"item.gt6.usb_drive_1\": \"USB 1.0 HDD\""), "the en drive name");
		assertTrue(tEn.contains("\"item.gt6.usb_drive_4.tooltip\": \"Stores up to 16 Files at once\""), "the en drive tooltip");
		assertTrue(tEn.contains("\"item.gt6.comp_laser_gas_he\": \"Helium Laser Emitter\""), "the en He emitter name");
		String tZh = generated("assets/gt6/lang/zh_cn.json");
		assertTrue(tZh.contains("\"item.gt6.usb_cable_1\": \"USB 1.0 数据线\""), "the zh cable dump face");
		assertTrue(tZh.contains("\"item.gt6.usb_cable_4.tooltip\": \"与USB接口连接时自动替换USB\""), "the zh cable tooltip");
		assertTrue(tZh.contains("\"item.gt6.usb_drive_4\": \"USB 4.0 驱动器\""), "the zh drive dump face");
		assertTrue(tZh.contains("\"item.gt6.usb_drive_1.tooltip\": \"同时存储16项文件\""), "the zh drive tooltip");
		assertTrue(tZh.contains("\"item.gt6.comp_laser_gas_he\": \"氦激光镭射器\""), "the zh He emitter face");
	}

	// ------------------------------------------------------------------ ② the crafting rows

	/** The tier ladders (the :808-811/:819-822 columns). */
	private static final String[] WIRE_COLS = {"wire_gold_gt01", "wire_aluminium_gt01", "wire_platinum_gt01", "wire_graphene_gt01"};
	private static final String[] CABLE_COLS = {"cable_gold_gt01", "cable_aluminium_gt01", "cable_platinum_gt01", "wire_graphene_gt01"};
	private static final String[] MAT_SLUGS = {"aluminium", "stainless_steel", "chromium", "titanium"};

	/**
	 * ② The four cable rows carry the :808-811 grid verbatim: "xWd","PCP","TCT" with
	 * the wirecutter/screwdriver tool letters, the WIRES_01[3..6] 'W' column and the
	 * CABLES_01[3..6] 'C' column — INCLUDING the [6] quirk: upstream's graphene "cable"
	 * slot carries wireGt01 (MT.java:3646), so T4's 'C' IS the wire item.
	 */
	@Test
	public void theFourCableRowsCarryTheUpstreamGrids() throws Exception {
		for (int i = 0; i < 4; i++) {
			JsonObject tRow = generatedJson("recipes/usb_cable_" + (i + 1));
			JsonArray tPattern = tRow.getAsJsonArray("pattern");
			assertEquals("xWd", tPattern.get(0).getAsString(), "cable tier " + (i + 1) + ": the :808 row 1");
			assertEquals("PCP", tPattern.get(1).getAsString(), "cable tier " + (i + 1) + ": row 2");
			assertEquals("TCT", tPattern.get(2).getAsString(), "cable tier " + (i + 1) + ": row 3");
			JsonObject tKey = tRow.getAsJsonObject("key");
			assertEquals("gt6:tools/wire_cutter", tKey.getAsJsonObject("x").get("tag").getAsString(), "the 'x' wirecutter letter");
			assertEquals("gt6:tools/screwdriver", tKey.getAsJsonObject("d").get("tag").getAsString(), "the 'd' screwdriver letter");
			assertEquals("gt6:" + WIRE_COLS[i], tKey.getAsJsonObject("W").get("item").getAsString(), "the WIRES_01[3+i] column");
			assertEquals("gt6:" + CABLE_COLS[i], tKey.getAsJsonObject("C").get("item").getAsString(),
					"the CABLES_01[3+i] column" + (i == 3 ? " (the wireGt01 graphene quirk, MT.java:3646)" : ""));
			assertEquals("gt6:plate_" + MAT_SLUGS[i], tKey.getAsJsonObject("P").get("item").getAsString(), "the plate ladder");
			assertEquals("gt6:screw_" + MAT_SLUGS[i], tKey.getAsJsonObject("T").get("item").getAsString(), "the screw ladder");
			assertEquals("gt6:usb_cable_" + (i + 1), tRow.getAsJsonObject("result").get("item").getAsString(), "the result path");
		}
	}

	/**
	 * ② The four HDD rows carry the :819-822 grid verbatim: "PLT","dRW","TCP" with the
	 * exempted He emitter 'L', the OD.record→#minecraft:music_discs 'R', the
	 * OD_CIRCUITS[3..6] 'C' tags and the USB Cable 'W' items.
	 */
	@Test
	public void theFourDriveRowsCarryTheUpstreamGrids() throws Exception {
		for (int i = 0; i < 4; i++) {
			JsonObject tRow = generatedJson("recipes/usb_drive_" + (i + 1));
			JsonArray tPattern = tRow.getAsJsonArray("pattern");
			assertEquals("PLT", tPattern.get(0).getAsString(), "hdd tier " + (i + 1) + ": the :819 row 1");
			assertEquals("dRW", tPattern.get(1).getAsString(), "hdd tier " + (i + 1) + ": row 2");
			assertEquals("TCP", tPattern.get(2).getAsString(), "hdd tier " + (i + 1) + ": row 3");
			JsonObject tKey = tRow.getAsJsonObject("key");
			assertEquals("gt6:tools/screwdriver", tKey.getAsJsonObject("d").get("tag").getAsString(), "the 'd' screwdriver letter");
			assertEquals("minecraft:music_discs", tKey.getAsJsonObject("R").get("tag").getAsString(), "the OD.record column");
			assertEquals("gt6:comp_laser_gas_he", tKey.getAsJsonObject("L").get("item").getAsString(), "the exempted He emitter column");
			assertEquals("gt6:usb_cable_" + (i + 1), tKey.getAsJsonObject("W").get("item").getAsString(), "the IL.USB_Cable_N column");
			assertEquals("gt6:circuit" + (i + 3), tKey.getAsJsonObject("C").get("tag").getAsString(), "the OD_CIRCUITS[3+i] column");
			assertEquals("gt6:plate_" + MAT_SLUGS[i], tKey.getAsJsonObject("P").get("item").getAsString(), "the plate ladder");
			assertEquals("gt6:screw_" + MAT_SLUGS[i], tKey.getAsJsonObject("T").get("item").getAsString(), "the screw ladder");
			assertEquals("gt6:usb_drive_" + (i + 1), tRow.getAsJsonObject("result").get("item").getAsString(), "the result path");
		}
	}

	private JsonObject generatedJson(String aPath) throws Exception {
		return JsonParser.parseString(generated("data/gt6/" + aPath + ".json")).getAsJsonObject();
	}

	// ------------------------------------------------------------------ ③ the Behavior_DataStorage16 carrier

	/** The drive carrier is the upstream CS.java:1278 literal (the W2 consumers key on this exact string). */
	@Test
	public void theDriveCarrierKeyIsTheUpstreamLiteral() {
		assertEquals("gt.usb.drive", GT6UsbSticks.NBT_USB_DRIVE, "CS.java:1278");
	}

	/** The 16-slot drive round trip (the Behavior_DataStorage16.java:39-45 read legs), over the base-class fixture latch. */
	@Test
	public void theDriveCarrierRoundTrips() {
		GT6UsbSticks.GT6UsbDriveItem tDrive = registerItemFixture("fixture_usb_drive_3",
				() -> new GT6UsbSticks.GT6UsbDriveItem(new net.minecraft.world.item.Item.Properties(), (byte)3));
		ItemStack tStack = new ItemStack(tDrive);
		assertNull(GT6UsbSticks.readDrive(tStack), "no drive compound on a fresh stack (the Perfectly Formatted face)");
		// the :41-42 face — the key present, nothing in it (Uncleanly Formatted)
		GT6UsbSticks.writeDrive(tStack, new CompoundTag());
		CompoundTag tEmpty = GT6UsbSticks.readDrive(tStack);
		assertNotNull(tEmpty, "the drive compound exists after the write");
		assertTrue(tEmpty.isEmpty(), "an empty drive compound stays empty");
		// the :44-49 face — slot 0 carries a material, slots 1..15 read back empty
		CompoundTag tSlot = new CompoundTag();
		tSlot.putShort(GT6UsbSticks.NBT_REPLICATOR_DATA, MT.Iron.mID);
		CompoundTag tWritten = new CompoundTag();
		tWritten.put(GT6UsbSticks.NBT_USB_DATA + 0, tSlot);
		GT6UsbSticks.writeDrive(tStack, tWritten);
		CompoundTag tBack = GT6UsbSticks.readDrive(tStack);
		assertEquals(MT.Iron.mID, tBack.getCompound(GT6UsbSticks.NBT_USB_DATA + 0).getShort(GT6UsbSticks.NBT_REPLICATOR_DATA),
				"the slot-0 material short round-trips (the gt.usb.data0 key face)");
		assertTrue(tBack.getCompound(GT6UsbSticks.NBT_USB_DATA + 1).isEmpty(), "slot 1 reads empty (the Data Slot i is Empty face)");
		assertTrue(tBack.getCompound(GT6UsbSticks.NBT_USB_DATA + 15).isEmpty(), "slot 15 reads empty — the :44 loop bound");
	}
}
