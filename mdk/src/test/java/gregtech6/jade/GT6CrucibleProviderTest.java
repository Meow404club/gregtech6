package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import gregapi.util.CruciblePhysics;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tools.TileEntitySmeltery;

/**
 * Offline gate for the crucible Jade face: the tag contract + the display-line pure
 * functions, over the SAME static seam the live {@code appendServerData} reads through
 * ({@code getTemperatureValue}/{@code getTemperatureMax}/{@code mMeltDown}/{@code mContent}).
 * The BlockAccessor wrapper itself is live-only (GT6MachineProviderTest posture).
 *
 * <p>Groups: the format pins (displayUnits + the lang lines), the truncation, the tank-bar
 * wire (task crucible-jade-tankbar: TOTAL_MAX/MOLTEN/overlay keys + the v3 dispatch — molten
 * bridged = the official fluid payload, molten unbridged = the ContentFace molten arm, solid
 * mapped = the ContentFace solid arm, solid unmapped = the guard fallback, meltdown = the
 * red text path) and the empty state. The tooltip language VALUES (en/zh) are pinned by the
 * datagen faces + runData, not here.
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
				aSmeltery.mMeltDown, CruciblePhysics.Params.SMALL.maxAmount(), aSmeltery.mContent);
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
	public void temperatureBarLineCarriesTheKeyAndBothArguments() {
		// the r8 B-case restyle: the temperature row is the shared bar (GT6JadeRows.bar — the
		// element assembly itself is live-only) over a two-slot text (the unit word at the tail)
		TranslatableContents tBar = (TranslatableContents) GT6CrucibleProvider.temperatureBarLine(2000, 5000).getContents();
		assertEquals(GT6CrucibleProvider.LANG_TEMPERATURE_BAR, tBar.getKey());
		assertEquals(2, tBar.getArgs().length);
		assertEquals(2000L, tBar.getArgs()[0]);
		assertEquals(5000L, tBar.getArgs()[1]);
		// the clamp face the bar feeds on — over-max pins at full, a zero ceiling answers 0
		assertEquals(1.0F, GT6JadeRows.ratio(9999, 5000), 1e-6F);
		assertEquals(0.0F, GT6JadeRows.ratio(293, 0), 1e-6F);
	}

	@Test
	public void totalAndEntryLinesComposeTheTfruLabelShape() {
		// the total row IS the TFRU LH.CONTENT label row (commit 33c22beb, first-row label form);
		// the unit word rides the lang VALUE (en U, zh 份 — the crucible-jade-tankbar ③ move)
		TranslatableContents tTotal = (TranslatableContents) GT6CrucibleProvider.totalLine(4 * CS.U).getContents();
		assertEquals(GT6CrucibleProvider.LANG_TOTAL, tTotal.getKey());
		assertEquals("4.000", tTotal.getArgs()[0]);
		// the entry row: ONE translatable now (the old literal composition retired) — the name
		// component rides the first slot, the displayUnits string the second
		CompoundTag tEntry = GT6CrucibleProvider.entryTag(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		assertEquals("iron", tEntry.getString(GT6CrucibleProvider.ENTRY_MAT), "snakeCase(Iron) — the single derivation");
		TranslatableContents tLine = (TranslatableContents) GT6CrucibleProvider.contentLine(tEntry).getContents();
		assertEquals(GT6CrucibleProvider.LANG_ENTRY, tLine.getKey());
		assertEquals(2, tLine.getArgs().length);
		assertEquals("gt6.material.iron", ((Component)tLine.getArgs()[0]).getString(), "the slot keeps the component face");
		assertEquals("4.000", tLine.getArgs()[1]);
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
		TranslatableContents tLine = (TranslatableContents) GT6CrucibleProvider.contentLine(tEntry).getContents();
		assertEquals("Superconductor", ((Component)tLine.getArgs()[0]).getString(), "the literal fallback name in the slot");
	}

	// ------------------------------------------------------------------------------------
	// group ② the truncation + the tank-bar wire
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
		GT6CrucibleProvider.writeCrucibleData(tTag, 1811, 2011, false, CruciblePhysics.Params.SMALL.maxAmount(), tContent);
		// the wire: top 5 by amount desc + the "+2 more" tail + the true long total (28 U)
		ListTag tList = tTag.getList(GT6CrucibleProvider.KEY_CONTENT, Tag.TAG_COMPOUND);
		assertEquals(5, tList.size());
		assertEquals(7 * CS.U, tList.getCompound(0).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(6 * CS.U, tList.getCompound(1).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(3 * CS.U, tList.getCompound(4).getLong(GT6CrucibleProvider.ENTRY_AMOUNT));
		assertEquals(2, tTag.getInt(GT6CrucibleProvider.KEY_TRUNCATED));
		assertEquals(28 * CS.U, tTag.getLong(GT6CrucibleProvider.KEY_TOTAL));
		// the tank-bar denominators ride (v3 ⑤): the capacity key is a long, the caller picks
		// the family Params (SMALL here, MAX_AMOUNT on the large crucible)
		assertEquals(CruciblePhysics.Params.SMALL.maxAmount(), tTag.getLong(GT6CrucibleProvider.KEY_TOTAL_MAX));
		assertEquals(Tag.TAG_LONG, tTag.getTagType(GT6CrucibleProvider.KEY_TOTAL_MAX));
	}

	/**
	 * The v3 ② dispatch matrix, offline: cold Steel content → the ContentFace SOLID arm
	 * verbatim (the bowl-shell same-origin pin); molten Copper (never bridged on either leg)
	 * → the MOLTEN arm verbatim (smeltery_content + the mRGBaLiquid tint); molten IRON →
	 * leg-split (see ironMoltenOverlayFollowsTheBridge*); cold IRON → the loud bodyTexture
	 * map throws and the guard answers NO overlay payload (plain bar, the honest fallback).
	 */
	@Test
	public void overlayDispatchFollowsTheContentFaceSeam() {
		// solid mapped family: the wire record EQUALS the seam's own dispatch (same-origin)
		List<OreDictMaterialStack> tSteel = List.of(new OreDictMaterialStack(MT.Steel, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 300, 2000, false, 16 * CS.U, tSteel);
		assertFalse(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), "300 K under the melting point stays solid");
		GT6CrucibleDatagen.ContentFace tExpected = GT6CrucibleDatagen.contentFace(MT.Steel, false);
		CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals(tExpected.texture(), tOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE));
		assertEquals(tExpected.tintARGB(), tOverlay.getInt(GT6CrucibleProvider.OVERLAY_TINT));
		assertFalse(tOverlay.contains(GT6CrucibleProvider.OVERLAY_FLUID), "the solid face is not a fluid payload");
		// molten unbridged: the molten arm is the smeltery_content sprite + the liquid tint —
		// the arm never walks the loud bodyTexture map, so ANY material resolves
		List<OreDictMaterialStack> tCopper = List.of(new OreDictMaterialStack(MT.Cu, 4 * CS.U));
		CompoundTag tHot = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tHot, 2000, 2000, false, 16 * CS.U, tCopper);
		assertTrue(tHot.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), "lightest melting point <= temperature is the :299 gate");
		GT6CrucibleDatagen.ContentFace tMoltenFace = GT6CrucibleDatagen.contentFace(MT.Cu, true);
		CompoundTag tMoltenOverlay = tHot.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals(tMoltenFace.texture(), tMoltenOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE));
		assertEquals(tMoltenFace.tintARGB(), tMoltenOverlay.getInt(GT6CrucibleProvider.OVERLAY_TINT));
		// the upstream :299 edge: exactly AT the melting point counts as molten (the > gate)
		CompoundTag tEdge = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tEdge, (long)MT.Cu.mMeltingPoint, 2000, false, 16 * CS.U, tCopper);
		assertTrue(tEdge.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), "melting point <= temperature — the <= arm");
	}

	@Test
	public void unmappedSolidFallsBackToNoOverlay() {
		// Iron is not in the bowl-model four-row bodyTexture map — the loud seam throws and
		// the guard answers null: no overlay key = the plain progress fill (never a live crash)
		List<OreDictMaterialStack> tIron = List.of(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 300, 2000, false, 16 * CS.U, tIron);
		assertFalse(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		assertFalse(tTag.contains(GT6CrucibleProvider.KEY_OVERLAY), "the guard fallback: no payload, no crash");
		// the lightest walk itself is the BE mirror (TileEntitySmeltery.java:469-475 shape)
		assertEquals(MT.Fe, GT6CrucibleProvider.lightest(tIron).mMaterial);
		assertNull(GT6CrucibleProvider.lightest(List.of()));
	}

	/**
	 * The ① official fluid-element payload — the molten + bridged arm. Leg reality: the forge
	 * offline JVM never binds the seed RegistryObject (the FluidBridgeTest posture), so the
	 * bridge misses and the face fallback answers; the 21.1 leg boots FML, the "iron" holder
	 * IS bound and the payload is the fluid id + the 144 L/unit bridge amount.
	 */
	//? if forge {
	@Test
	public void ironMoltenOverlayFallsBackToTheFaceOffline() {
		List<OreDictMaterialStack> tIron = List.of(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 2000, 2000, false, 16 * CS.U, tIron);
		assertTrue(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		GT6CrucibleDatagen.ContentFace tExpected = GT6CrucibleDatagen.contentFace(MT.Fe, true);
		CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals(tExpected.texture(), tOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE),
				"the unbound seed is a bridge miss offline — the molten face arm answers");
		assertFalse(tOverlay.contains(GT6CrucibleProvider.OVERLAY_FLUID));
	}
	//?} else {
	/*@Test
	public void ironMoltenOverlayIsTheOfficialFluidPayload() {
		List<OreDictMaterialStack> tIron = List.of(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 2000, 2000, false, 16 * CS.U, tIron);
		assertTrue(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals("gt6:iron_molten", tOverlay.getString(GT6CrucibleProvider.OVERLAY_FLUID),
				"the bound bridge seed answers the Jade official fluid element payload (v3 ①)");
		assertEquals(4 * CS.U * gregtech6.fluid.FluidBridge.L_PER_MOLTEN_UNIT,
				tOverlay.getLong(GT6CrucibleProvider.OVERLAY_AMOUNT), "the 144 L/unit bridge convention");
		assertFalse(tOverlay.contains(GT6CrucibleProvider.OVERLAY_TEXTURE), "no face payload rides along");
	}
	*///?}

	// ------------------------------------------------------------------------------------
	// group ③ the meltdown latch — the v3 red-text path
	// ------------------------------------------------------------------------------------

	@Test
	public void meltdownLatchRidesTheTagAndTheBarTextGoesRed() {
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
		// the v3 red path: the latch turns the TANK-BAR TEXT red (the retired standalone row's
		// face folded into the text color — the live appendTooltip passes this into
		// GT6JadeRows.bar's textColor; ProgressStyle.textColor disables the auto white)
		assertEquals(ChatFormatting.RED.getColor(), Integer.valueOf(GT6CrucibleProvider.barTextColor(true)));
		assertEquals(-1, GT6CrucibleProvider.barTextColor(false), "a calm crucible keeps the default white face");
		// the sync keys, typed (the retired alarm row's key is GONE from the wire)
		for (String tKey : Arrays.asList(GT6CrucibleProvider.KEY_TEMP, GT6CrucibleProvider.KEY_TEMP_MAX,
				GT6CrucibleProvider.KEY_TOTAL, GT6CrucibleProvider.KEY_TOTAL_MAX)) {
			assertEquals(Tag.TAG_LONG, tTag.getTagType(tKey), tKey + " rides the long face");
		}
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6CrucibleProvider.KEY_MELTDOWN));
		assertEquals(Tag.TAG_BYTE, tTag.getTagType(GT6CrucibleProvider.KEY_MOLTEN));
		assertEquals(Tag.TAG_INT, tTag.getTagType(GT6CrucibleProvider.KEY_TRUNCATED));
		assertEquals(Tag.TAG_LIST, tTag.getTagType(GT6CrucibleProvider.KEY_CONTENT));
		// the molten iron wire (the 2000 K furnace IS over iron's melting point — the BE-level
		// meltdown state and the :299 display gate are independent latches)
		assertTrue(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		assertEquals(Tag.TAG_COMPOUND, tTag.getTagType(GT6CrucibleProvider.KEY_OVERLAY));
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
		// no lightest census on an empty crucible: molten false and NO overlay payload —
		// the appendTooltip gate (total <= 0) short-circuits to the single Empty line anyway
		assertFalse(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		assertFalse(tTag.contains(GT6CrucibleProvider.KEY_OVERLAY));
		TranslatableContents tEmpty = (TranslatableContents) GT6CrucibleProvider.emptyLine().getContents();
		assertEquals(GT6CrucibleProvider.LANG_EMPTY, tEmpty.getKey());
		assertEquals(0, tEmpty.getArgs().length);
	}
}
