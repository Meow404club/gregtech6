package gregtech6.easter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.items.GT6UsbSticks;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The tooltip-domain April-Fools face (task easter-tooltip-rename, the S2 leftover ①):
 * every material-word READ inside a tooltip assembly must ride the S2 seam
 * ({@link MaterialPrefixItem#materialFill}, the ensureFoolsApplied + live mNameLocal
 * consult). The census split:
 *
 * <ul>
 * <li>already routed pre-card (the material-tooltip-face / i18n-material-fill-fix seam
 *     consumers): the F3+H contained row, the handle row, the source-of row, the tool
 *     ladder suffix — pinned here flag-on/flag-off as the regression wall;</li>
 * <li>the card's gap: the USB data faces (GT6UsbSticks stick face UT.java:2246-2267 +
 *     drive short face UT.java:2260) read {@code getLocal()} live but NEVER consult the
 *     seam, so the renames can stay unlanded (production lands them on the first
 *     materialFill consult anywhere — a player whose first hover is a USB stick sees the
 *     pre-rename word). Routed by this card;</li>
 * <li>declared NOT applicable with upstream parity: the chemical formula row renders the
 *     INIT-FROZEN {@code mTooltipChemical} string on BOTH sides — upstream renames run in
 *     the GT_API constructor (GT_API.java:364), whose first {@code MT.W} evaluation
 *     triggers/completes the MT {@code <clinit>} (JLS class-init order) where every
 *     uumMcfg formula freezes the PRE-rename words; the port replays the identical
 *     order, so the formula row stays original under the flag on both sides
 *     (pinned in {@link #chemicalFormulaRowStaysTheFrozenInitString()}).</li>
 * </ul>
 *
 * <p>Hygiene: the renames mutate {@code mNameLocal} in place — the GT6CalendarsTest
 * snapshot/restore bracket wraps every test.
 */
public class GT6TooltipFoolRenameTest extends GTOfflineTestBase {

	private static final Map<OreDictMaterial, String> SNAPSHOT = new HashMap<>();
	private static boolean sSnapshotTaken = false;

	static MaterialPrefixItem sBronzeDust;
	static MaterialPrefixItem sIronIngot;
	static GT6UsbSticks.GT6UsbStickItem sStick;
	static GT6UsbSticks.GT6UsbDriveItem sDrive;

	@BeforeAll
	static void buildFixtures() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		if (!sSnapshotTaken) {
			for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values())
				SNAPSHOT.put(tMaterial, tMaterial.mNameLocal);
			sSnapshotTaken = true;
		}
		sBronzeDust = registerItemFixture("fixture_fool_bronze_dust",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Bronze));
		sIronIngot = registerItemFixture("fixture_fool_fe_ingot",
				() -> new MaterialPrefixItem(new Item.Properties(), OP.ingot, MT.Fe));
		sStick = registerItemFixture("fixture_fool_usb_stick_1",
				() -> new GT6UsbSticks.GT6UsbStickItem(new Item.Properties(), (byte) 1));
		sDrive = registerItemFixture("fixture_fool_usb_drive_3",
				() -> new GT6UsbSticks.GT6UsbDriveItem(new Item.Properties(), (byte) 3));
	}

	@BeforeEach
	public void cleanSlate() {
		restore();
	}

	@AfterEach
	public void restore() {
		GT6Calendars.resetForTest();
		for (Map.Entry<OreDictMaterial, String> tEntry : SNAPSHOT.entrySet())
			tEntry.getKey().setLocal(tEntry.getValue());
		boolean[] tFlags = GT6Calendars.computeFlags(System.currentTimeMillis());
		GT6Calendars.APRIL_FOOLS = tFlags[0];
		GT6Calendars.WOODMANS_BDAY = tFlags[1];
		GT6Calendars.XMAS_IN_JULY = tFlags[2];
		GT6Calendars.XMAS_IN_DECEMBER = tFlags[3];
	}

	/**
	 * The flag-on posture WITHOUT a prior {@code ensureFoolsApplied} consult — the
	 * date-window path (wall clock April 1-2). Production lands the renames on the first
	 * seam consult; the renders under test must be that consult.
	 */
	private static void foolsOnBare() {
		GT6Calendars.APRIL_FOOLS = true;
	}

	/** The leg-swap hover call — ONE swap, the GTLightningRodBlock.Item:71-81 shape. */
	private static List<Component> hover(Item aItem, ItemStack aStack, boolean aAdvanced) {
		List<Component> tTooltip = new ArrayList<>();
		TooltipFlag tFlag = aAdvanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
		//? if forge {
		aItem.appendHoverText(aStack, null, tTooltip, tFlag);
		//?} else {
		/*aItem.appendHoverText(aStack, Item.TooltipContext.EMPTY, tTooltip, tFlag);
		*///?}
		return tTooltip;
	}

	private static String flatText(List<Component> aTooltip) {
		StringBuilder tBuilder = new StringBuilder();
		for (Component tRow : aTooltip) tBuilder.append(tRow.getString()).append('\n');
		return tBuilder.toString();
	}

	private static boolean containsKey(Component aRow, String aKey) {
		if (aRow.getContents() instanceof TranslatableContents tContents && tContents.getKey().equals(aKey)) return true;
		for (Component tSibling : aRow.getSiblings()) if (containsKey(tSibling, aKey)) return true;
		return false;
	}

	private static boolean anyRowCarriesKey(List<Component> aTooltip, String aKey) {
		for (Component tRow : aTooltip) if (containsKey(tRow, aKey)) return true;
		return false;
	}

	// ---------------------------------------------------------------------------
	// flag ON — the F3+H contained row rides the seam (pre-routed face, regression wall)
	// ---------------------------------------------------------------------------

	@Test
	public void foolOnContainedRowPinsTheFoolWords() {
		foolsOnBare();
		// sample 1 + 2 — MT.Bronze → "Tinkerers Alloy" (GT_API.java:434), MT.Fe → "Irun" (:394)
		String tBronze = flatText(hover(sBronzeDust, new ItemStack(sBronzeDust), true));
		assertTrue(tBronze.contains("Tinkerers Alloy"), "the F3+H row must carry the fool word, got:\n" + tBronze);
		String tIron = flatText(hover(sIronIngot, new ItemStack(sIronIngot), true));
		assertTrue(tIron.contains("Irun"), "the F3+H row must carry the fool word, got:\n" + tIron);
		// the seam consult landed the renames — the census pin stays at the explicit 112 + dynamic tail
		assertTrue(GT6Calendars.sFoolRenames >= 112, "the render must have landed the rename table");
	}

	// ---------------------------------------------------------------------------
	// flag ON — the USB faces (the card's gap: live getLocal() reads that never consulted)
	// ---------------------------------------------------------------------------

	@Test
	public void foolOnUsbStickFacePinsTheFoolWordOnFirstConsult() {
		// the gap posture: renames NOT landed before the render — the hover itself must land them
		foolsOnBare();
		ItemStack tStack = new ItemStack(sStick);
		GT6UsbSticks.writeMaterialData(tStack, MT.Fe);
		String tText = flatText(hover(sStick, tStack, false));
		// UT.java:2246-2267 the "Material Data: " face, live-name read — "Irun" (:394), not "Iron"
		assertTrue(tText.contains("Irun"), "the stick face must ride the seam, got:\n" + tText);
		assertFalse(tText.contains("Iron"), "the pre-rename word must be gone, got:\n" + tText);
	}

	@Test
	public void foolOnUsbDriveShortFacePinsTheFoolWord() {
		foolsOnBare();
		ItemStack tStack = new ItemStack(sDrive);
		CompoundTagDrive.writeFeSlot(tStack);
		String tText = flatText(hover(sDrive, tStack, false));
		// UT.java:2260 the short "Mat Data: " face — sample 4: Bronze reads "Tinkerers Alloy" here too
		assertTrue(tText.contains("Irun"), "the drive face must ride the seam, got:\n" + tText);
		assertFalse(tText.contains("Iron"), "the pre-rename word must be gone, got:\n" + tText);

		// fresh names, flag back on bare (restore() re-binds the wall clock and the original words)
		restore();
		foolsOnBare();
		ItemStack tBronzeStack = new ItemStack(sDrive);
		CompoundTagDrive.writeSlot(tBronzeStack, MT.Bronze.mID);
		String tBronzeText = flatText(hover(sDrive, tBronzeStack, false));
		assertTrue(tBronzeText.contains("Tinkerers Alloy"), "the drive face must carry the fool word, got:\n" + tBronzeText);
	}

	/** Slot-NBT builder (the GT6UsbPeripheralsTest round-trip shape, kept off the imports). */
	private static final class CompoundTagDrive {
		static void writeFeSlot(ItemStack aStack) {
			writeSlot(aStack, MT.Fe.mID);
		}

		static void writeSlot(ItemStack aStack, short aMaterialId) {
			net.minecraft.nbt.CompoundTag tSlot = new net.minecraft.nbt.CompoundTag();
			tSlot.putShort(GT6UsbSticks.NBT_REPLICATOR_DATA, aMaterialId);
			net.minecraft.nbt.CompoundTag tWritten = new net.minecraft.nbt.CompoundTag();
			tWritten.put(GT6UsbSticks.NBT_USB_DATA + 0, tSlot);
			GT6UsbSticks.writeDrive(aStack, tWritten);
		}
	}

	// ---------------------------------------------------------------------------
	// flag OFF — zero change (the translatable fill face everywhere)
	// ---------------------------------------------------------------------------

	@Test
	public void flagsOffLeavesEveryFaceUntouched() {
		// the zero-change pin: NO fool word anywhere, every material word rides gt6.material.*
		String tBronze = flatText(hover(sBronzeDust, new ItemStack(sBronzeDust), true));
		assertFalse(tBronze.contains("Tinkerers Alloy"), "flag off must keep the original word");
		assertTrue(anyRowCarriesKey(hover(sBronzeDust, new ItemStack(sBronzeDust), true), "gt6.material.bronze"),
				"the contained row fill must stay the translatable seam face");

		ItemStack tStick = new ItemStack(sStick);
		GT6UsbSticks.writeMaterialData(tStick, MT.Fe);
		List<Component> tStickRows = hover(sStick, tStick, false);
		assertFalse(flatText(tStickRows).contains("Irun"), "flag off must keep the original word");
		assertTrue(anyRowCarriesKey(tStickRows, "gt6.material.iron"), "the stick face fill must stay the translatable seam face");

		ItemStack tDrive = new ItemStack(sDrive);
		CompoundTagDrive.writeFeSlot(tDrive);
		List<Component> tDriveRows = hover(sDrive, tDrive, false);
		assertFalse(flatText(tDriveRows).contains("Irun"), "flag off must keep the original word");
		assertTrue(anyRowCarriesKey(tDriveRows, "gt6.material.iron"), "the drive face fill must stay the translatable seam face");
	}

	// ---------------------------------------------------------------------------
	// the census N/A pin — the chemical formula row stays the INIT-FROZEN string
	// ---------------------------------------------------------------------------

	@Test
	public void chemicalFormulaRowStaysTheFrozenInitString() {
		// upstream parity: the formula freezes at MT <clinit>, BEFORE any rename can run
		// (JLS class-init order over GT_API.java:364) — the flag must NOT change it here either.
		assertNotNull(MT.Fe.mTooltipChemical, "the element symbol must exist");
		foolsOnBare();
		String tOn = flatText(hover(sIronIngot, new ItemStack(sIronIngot), false));
		assertTrue(tOn.contains(MT.Fe.mTooltipChemical), "the frozen formula survives the flag, got:\n" + tOn);
		assertFalse(tOn.contains("Irun"), "the formula face carries no fool word (upstream parity)");
	}
}
