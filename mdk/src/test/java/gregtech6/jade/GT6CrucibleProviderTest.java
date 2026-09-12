package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * Offline gate for task p28-crucible-jade-face acceptance ①: the crucible Jade face tag
 * contract + the four display-line pure functions, over the SAME static seam the live
 * {@code appendServerData} reads through ({@code getTemperatureValue}/{@code getTemperatureMax}
 * /{@code mMeltDown}/{@code mContent} — TileEntitySmeltery.java:514/:519/:105/:98). The
 * BlockAccessor wrapper itself is live-only (GT6MachineProviderTest posture, its class doc).
 *
 * <p>Four SPEC groups: the format pins (displayUnits + the four lang lines), the truncation
 * (desc sort, top 5, "+N more"), the meltdown flag (tag latch + the RED line) and the empty
 * state. The tooltip language VALUES (en/zh) are pinned by the datagen faces + runData, not
 * here — this file pins the wire shape and the component structure.
 */
public class GT6CrucibleProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<TileEntitySmeltery> sSmelteryType;

	@BeforeAll
	static void fixture() {
		// the real MT dataset (the GTMultiBlockCruciblePhysicsTest posture) — the slug guard
		// ladder walks the registered materials, so they must be live before any stack is built
		MaterialRegistry.INSTANCE.open();
		MT.init();
		// the self-referencing lambda (the TileEntitySmelteryOfflineTest fixture shape — the
		// static field is read at create() time, after build() returned)
		sSmelteryType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntitySmeltery(sSmelteryType, aPos, aState), Blocks.BRICKS).build(null);
	}

	/** The BE readback exactly as the live appendServerData performs it (the getter trio + field). */
	private static CompoundTag syncOf(TileEntitySmeltery aSmeltery) {
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag,
				aSmeltery.getTemperatureValue((byte) 0), aSmeltery.getTemperatureMax((byte) 0),
				aSmeltery.mMeltDown, aSmeltery.mContent);
		return tTag;
	}

	// ------------------------------------------------------------------------------------
	// group ① the format pins
	// ------------------------------------------------------------------------------------

	@Test
	public void displayUnitsIsTheUpstreamForm() {
		// upstream UT.Code.displayUnits (UT.java:1670-1674) — integer part + "." + three digits
		assertEquals("4.000", GT6CrucibleProvider.displayUnits(4 * CS.U));
		assertEquals("0.500", GT6CrucibleProvider.displayUnits(CS.U / 2));
		assertEquals("0.000", GT6CrucibleProvider.displayUnits(0));
		assertEquals("?.???", GT6CrucibleProvider.displayUnits(-1));
		// the long face rides true longs (the GT6FluidProviderTest OVERFLOW posture) —
		// 3e9 = 4 U + 405408000 = "4.625", never the bindInt clamp
		assertEquals("4.625", GT6CrucibleProvider.displayUnits(3_000_000_000L));
	}

	@Test
	public void temperatureLineCarriesTheKeyAndBothArguments() {
		TranslatableContents tContents = (TranslatableContents) GT6CrucibleProvider
				.temperatureLine(2930, 3930, false).getContents();
		assertEquals(GT6CrucibleProvider.LANG_TEMPERATURE, tContents.getKey());
		assertEquals(2, tContents.getArgs().length);
		assertEquals(2930L, tContents.getArgs()[0]);
		assertEquals(3930L, tContents.getArgs()[1]);
	}

	@Test
	public void totalAndContentLinesComposeTheTfruLabelShape() {
		// the total row IS the TFRU LH.CONTENT label row (commit 33c22beb, first-row label form):
		// "Content: 4.000 U" with the displayUnits string as the slot
		TranslatableContents tTotal = (TranslatableContents) GT6CrucibleProvider.totalLine(4 * CS.U).getContents();
		assertEquals(GT6CrucibleProvider.LANG_TOTAL, tTotal.getKey());
		assertEquals("4.000", tTotal.getArgs()[0]);
		// the item row: two-space indent + the gt6.material small unit (server-vouched slug) + amount
		CompoundTag tEntry = GT6CrucibleProvider.entryTag(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		assertEquals("iron", tEntry.getString(GT6CrucibleProvider.ENTRY_MAT), "snakeCase(Iron) — the single derivation");
		Component tLine = GT6CrucibleProvider.contentLine(tEntry);
		assertTrue(tLine.getSiblings().size() >= 2, "indent + name + amount tail");
		TranslatableContents tName = (TranslatableContents) tLine.getSiblings().get(0).getContents();
		assertEquals("gt6.material.iron", tName.getKey(), "the small unit the en walk emitted");
		// the truncation tail
		TranslatableContents tMore = (TranslatableContents) GT6CrucibleProvider.moreLine(2).getContents();
		assertEquals(GT6CrucibleProvider.LANG_MORE, tMore.getKey());
		assertEquals(2, tMore.getArgs()[0]);
	}

	@Test
	public void unvouchedMaterialsFallBackToThePlainName() {
		// Superconductor is a tier material (mID -1, MT.java:1859) — the en walk never emits
		// its key, so the slug must be null and the plain face carries the internal name
		// (no raw gt6.material.* key can render — the honesty rule: no invented translations)
		OreDictMaterialStack tExotic = new OreDictMaterialStack(MT.Superconductor, CS.U);
		assertNull(GT6CrucibleProvider.materialSlug(tExotic), "unvouched material answers no slug");
		CompoundTag tEntry = GT6CrucibleProvider.entryTag(tExotic);
		assertFalse(tEntry.contains(GT6CrucibleProvider.ENTRY_MAT));
		assertEquals("Superconductor", tEntry.getString(GT6CrucibleProvider.ENTRY_NAME));
		Component tLine = GT6CrucibleProvider.contentLine(tEntry);
		assertEquals("Superconductor", tLine.getSiblings().get(0).getString());
	}

	// ------------------------------------------------------------------------------------
	// group ② the truncation
	// ------------------------------------------------------------------------------------

	@Test
	public void contentIsSortedDescendingAndCutAtFive() {
		List<OreDictMaterialStack> tContent = List.of(
				new OreDictMaterialStack(MT.Fe, CS.U),
				new OreDictMaterialStack(MT.Fe, 2 * CS.U),
				new OreDictMaterialStack(MT.Fe, 3 * CS.U),
				new OreDictMaterialStack(MT.Fe, 4 * CS.U),
				new OreDictMaterialStack(MT.Fe, 5 * CS.U),
				new OreDictMaterialStack(MT.Fe, 6 * CS.U),
				new OreDictMaterialStack(MT.Fe, 7 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 1811, 2011, false, tContent);
		// the wire: top 5 by amount desc + the "+2 more" tail + the true long total (28 U)
		ListTag tList = tTag.getList(GT6CrucibleProvider.KEY_CONTENT, Tag.TAG_COMPOUND);
		assertEquals(5, tList.size());
		assertEquals(7 * CS.U, tList.getCompound(0).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(6 * CS.U, tList.getCompound(1).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(3 * CS.U, tList.getCompound(4).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(2, tTag.getInt(GT6CrucibleProvider.KEY_TRUNCATED));
		assertEquals(28 * CS.U, tTag.getLong(GT6CrucibleProvider.KEY_TOTAL));
	}

	// ------------------------------------------------------------------------------------
	// group ③ the meltdown flag
	// ------------------------------------------------------------------------------------

	@Test
	public void meltdownLatchRidesTheTagAndPaintsTheLineRed() {
		TileEntitySmeltery tSmeltery = new TileEntitySmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tSmeltery.mTemperature = 2000;
		tSmeltery.mMeltDown = true;
		tSmeltery.mContent.add(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		CompoundTag tTag = syncOf(tSmeltery);
		// the getter trio the live wrapper reads (TileEntitySmeltery.java:514/:519/:105)
		assertEquals(2000L, tTag.getLong(GT6CrucibleProvider.KEY_TEMP));
		assertEquals(tSmeltery.getTemperatureMax((byte) 0), tTag.getLong(GT6CrucibleProvider.KEY_TEMP_MAX));
		assertTrue(tTag.getLong(GT6CrucibleProvider.KEY_TEMP_MAX) > 0, "the offline Stone shell answers a real ceiling");
		assertTrue(tTag.getBoolean(GT6CrucibleProvider.KEY_MELTDOWN));
		// the latch paints the WHOLE line RED (the upstream meltdown dark-red row face) —
		// withStyle(ChatFormatting.RED) stores the TextColor.fromLegacyFormat face, and the
		// TextColor equals covers the named "red" color (ChatFormatting.getColor() is an Integer,
		// NOT a TextColor — the compile-gate caught that swap)
		assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED),
				GT6CrucibleProvider.temperatureLine(2000, tSmeltery.getTemperatureMax((byte) 0), true)
						.getStyle().getColor());
		assertNotEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED),
				GT6CrucibleProvider.temperatureLine(293, 1375, false).getStyle().getColor(),
				"a calm crucible stays unstyled");
		// the six sync keys + the three entry keys, typed
		for (String tKey : Arrays.asList(GT6CrucibleProvider.KEY_TEMP, GT6CrucibleProvider.KEY_TEMP_MAX,
				GT6CrucibleProvider.KEY_TOTAL)) {
			assertEquals(Tag.TAG_LONG, tTag.getTagType(tKey), tKey + " rides the long face");
		}
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6CrucibleProvider.KEY_MELTDOWN));
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6CrucibleProvider.KEY_TRUNCATED));
		assertEquals(Tag.TAG_LIST, tTag.getTagType(GT6CrucibleProvider.KEY_CONTENT));
		assertEquals(1, tTag.getList(GT6CrucibleProvider.KEY_CONTENT, Tag.TAG_COMPOUND).size());
		assertTrue(tTag.getList(GT6CrucibleProvider.KEY_CONTENT, Tag.TAG_COMPOUND).getCompound(0)
				.contains(GT6CrucibleProvider.ENTRY_AMOUNT));
	}

	// ------------------------------------------------------------------------------------
	// group ④ the empty state
	// ------------------------------------------------------------------------------------

	@Test
	public void emptyCrucibleAnswersTheEmptyFace() {
		TileEntitySmeltery tSmeltery = new TileEntitySmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		CompoundTag tTag = syncOf(tSmeltery);
		assertEquals(0L, tTag.getLong(GT6CrucibleProvider.KEY_TOTAL));
		assertTrue(tTag.getList(GT6CrucibleProvider.KEY_CONTENT, Tag.TAG_COMPOUND).isEmpty());
		assertEquals(0, tTag.getInt(GT6CrucibleProvider.KEY_TRUNCATED));
		// the temperature row still rides (a cold crucible HAS a temperature)…
		TranslatableContents tTemp = (TranslatableContents) GT6CrucibleProvider
				.temperatureLine(tTag.getLong(GT6CrucibleProvider.KEY_TEMP), tSmeltery.getTemperatureMax((byte) 0), false)
				.getContents();
		assertEquals(GT6CrucibleProvider.LANG_TEMPERATURE, tTemp.getKey());
		// …and the content face collapses to the single Empty line (the appendTooltip gate is
		// total <= 0 — the face the lang key serves)
		TranslatableContents tEmpty = (TranslatableContents) GT6CrucibleProvider.emptyLine().getContents();
		assertEquals(GT6CrucibleProvider.LANG_EMPTY, tEmpty.getKey());
		assertEquals(0, tEmpty.getArgs().length);
	}
}
