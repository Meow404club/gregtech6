package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

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
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.fluid.GTFluids;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tools.TileEntitySmeltery;
import gregtech6.registry.GT6MaterialTestSupport;

/**
 * Offline gate for the crucible Jade face: the tag contract + the display-line pure
 * functions, over the SAME static seam the live {@code appendServerData} reads through
 * ({@code getTemperatureValue}/{@code getTemperatureMax}/{@code mMeltDown}/{@code mContent}).
 * The BlockAccessor wrapper itself is live-only (GT6MachineProviderTest posture).
 *
 * <p>Groups: the format pins (displayUnits + the lang lines — the tank-bar text face keeps
 * the "n/16" share form, the entry face rides the bare numerator, task
 * crucible-jade-tankbar-fill's 2026-10-10 user ruling), the truncation, the tank-bar wire
 * (task crucible-jade-tankbar: TOTAL_MAX/MOLTEN/overlay keys + the ContentFace-only
 * dispatch — task crucible-jade-tankbar-fill retired the official-fluid payload arm, the
 * bridged-still water art read near-blank and broke the same-source contract with the
 * in-world face; meltdown = the red text path) and the empty state. The bridge coverage
 * census (task jade-molten-bridge-full) pins the full {@code gt6:<mat>_molten} domain
 * face (the bridge itself lives on for the pour/recycling/replicator consumers). The
 * render-primitive pin (the atlas pre-bind before the blit in GT6ContentFaceElement) is a
 * source-structure assertion. The tooltip language VALUES (en/zh) are pinned by the
 * datagen faces + runData, not here.
 */
public class GT6CrucibleProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<TileEntitySmeltery> sSmelteryType;

	@BeforeAll
	static void fixture() {
		// the real MT dataset (the GTMultiBlockCruciblePhysicsTest posture) — the slug guard
		// ladder walks the registered materials, so they must be live before any stack is built
		GT6MaterialTestSupport.materials(); // the hermetic bracket: reset FIRST, then the full refill (task hermetic-pour-tests)
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
	public void ownerLineNestsTheControllerNameKey() {
		// task mb-formed-crucible-wall ⑥ — the wall relay arm's ownership row: the outer
		// LANG_OWNER face over the NESTED controller-name translatable (the block.gt6.<path>
		// key the live arm derives from the target block; vanilla renders the nested slot
		// per locale, the contentLine material-name shape)
		TranslatableContents tOwner = (TranslatableContents) GT6CrucibleProvider.ownerLine("block.gt6.crucible_steel").getContents();
		assertEquals(GT6CrucibleProvider.LANG_OWNER, tOwner.getKey());
		assertEquals(1, tOwner.getArgs().length);
		TranslatableContents tName = (TranslatableContents) ((net.minecraft.network.chat.Component) tOwner.getArgs()[0]).getContents();
		assertEquals("block.gt6.crucible_steel", tName.getKey(), "the controller display key rides the nested slot");
		// the sync-face marker: the live arm writes KEY_OWNER only when the wall's target
		// resolves to a crucible controller (the getTarget(true) validity probe) — the key
		// round-trips through the Jade tag as a plain string
		CompoundTag tTag = new CompoundTag();
		tTag.putString(GT6CrucibleProvider.KEY_OWNER, "block.gt6.crucible_steel");
		assertEquals("block.gt6.crucible_steel", tTag.getString(GT6CrucibleProvider.KEY_OWNER));
	}

	@Test
	public void tankBarTextKeepsTheShareAmountForm() {
		// task crucible-jade-follower (the 2026-10-06 user ruling) + the tankbar-fill
		// clarification: the TANK-BAR text (totalLine) is the ONLY share-form consumer —
		// en "4/16", zh "4/16 份" (the unit word stays in the lang VALUE, ONE slot). The
		// entry face is pinned separately (contentLineRidesTheBareNumerator).
		TranslatableContents tTotal = (TranslatableContents) GT6CrucibleProvider.totalLine(4 * CS.U, 16 * CS.U).getContents();
		assertEquals(GT6CrucibleProvider.LANG_TOTAL, tTotal.getKey());
		assertEquals(1, tTotal.getArgs().length);
		assertEquals("4/16", tTotal.getArgs()[0], "the bare share form — 4 units of the 16-unit small capacity");
		// the LARGE crucible answers its own denominator (432, the TileEntityCrucible.MAX_AMOUNT face)
		assertEquals("4/432", GT6CrucibleProvider.shareAmount(4 * CS.U, CruciblePhysics.Params.LARGE.maxAmount()));
		// fractional amounts keep the displayUnits precision in the numerator slot
		assertEquals("0.500/16", GT6CrucibleProvider.shareAmount(CS.U / 2, 16 * CS.U));
		// zero and negative ride the displayUnits faces (the empty/odd guards)
		assertEquals("0.000/16", GT6CrucibleProvider.shareAmount(0, 16 * CS.U));
		assertEquals("?.???/16", GT6CrucibleProvider.shareAmount(-1, 16 * CS.U));
		// the truncation tail
		TranslatableContents tMore = (TranslatableContents) GT6CrucibleProvider.moreLine(2).getContents();
		assertEquals(GT6CrucibleProvider.LANG_MORE, tMore.getKey());
		assertEquals(2, tMore.getArgs()[0]);
	}

	@Test
	public void contentLineRidesTheBareNumerator() {
		// task crucible-jade-tankbar-fill (the 2026-10-10 user ruling): the ENTRY row loses
		// the denominator — "x" not "x/16" (the tank bar keeps its ratio text, see
		// tankBarTextKeepsTheShareAmountForm). Whole-U rides the bare integer, fractional
		// keeps the displayUnits precision, zero/negative the displayUnits guard faces.
		assertEquals("4", GT6CrucibleProvider.shareNumerator(4 * CS.U));
		assertEquals("0.500", GT6CrucibleProvider.shareNumerator(CS.U / 2));
		assertEquals("0.000", GT6CrucibleProvider.shareNumerator(0));
		assertEquals("?.???", GT6CrucibleProvider.shareNumerator(-1));
		// the entry row: ONE translatable — the name component + the NUMERATOR slot
		CompoundTag tEntry = GT6CrucibleProvider.entryTag(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		assertEquals("iron", tEntry.getString(GT6CrucibleProvider.ENTRY_MAT), "snakeCase(Iron) — the single derivation");
		TranslatableContents tLine = (TranslatableContents) GT6CrucibleProvider.contentLine(tEntry, 16 * CS.U).getContents();
		assertEquals(GT6CrucibleProvider.LANG_ENTRY, tLine.getKey());
		assertEquals(2, tLine.getArgs().length);
		assertEquals("gt6.material.iron", ((Component)tLine.getArgs()[0]).getString(), "the slot keeps the component face");
		assertEquals("4", tLine.getArgs()[1], "the entry amount rides the bare numerator — no denominator");
		// the fractional face and the aTotalMax irrelevance (the denominator is GONE from this row)
		CompoundTag tHalf = GT6CrucibleProvider.entryTag(new OreDictMaterialStack(MT.Fe, CS.U / 2));
		assertEquals("0.500", ((TranslatableContents) GT6CrucibleProvider.contentLine(tHalf, 16 * CS.U).getContents()).getArgs()[1]);
		assertEquals("4", ((TranslatableContents) GT6CrucibleProvider.contentLine(tEntry, 432 * CS.U).getContents()).getArgs()[1],
				"the capacity ceiling never leaks into the entry row");
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
		TranslatableContents tLine = (TranslatableContents) GT6CrucibleProvider.contentLine(tEntry, 16 * CS.U).getContents();
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
	 * The v3 ② dispatch matrix, offline: cold Steel content → the ContentFace SOLID arm —
	 * the FIXED upstream gray-black (task crucible-solid-gray-fidelity: Smeltery:590
	 * MT.NULL×blockRaw×CA_GRAY_64 verbatim, material-independent); molten Copper → the
	 * MOLTEN arm verbatim (molten art + the mRGBaLiquid tint). Molten iron (bridged) rides
	 * the SAME face since the official-fluid arm retired — see
	 * moltenOverlayIsAlwaysTheContentFacePayload.
	 */
	@Test
	public void overlayDispatchFollowsTheContentFaceSeam() {
		// solid: the wire record IS the fixed gray — texture and tint both the upstream
		// constant, whatever the lightest content's material is
		List<OreDictMaterialStack> tSteel = List.of(new OreDictMaterialStack(MT.Steel, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 300, 2000, false, 16 * CS.U, tSteel);
		assertFalse(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), "300 K under the melting point stays solid");
		CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals(GT6CrucibleDatagen.SOLID_CONTENT.texture(), tOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE),
				"the solid wire record rides the fixed gray borrow (material-independent)");
		assertEquals(0xFF404040, tOverlay.getInt(GT6CrucibleProvider.OVERLAY_TINT),
				"the solid tint is CA_GRAY_64 (CS.java:362) verbatim");
		assertEquals(Set.of(GT6CrucibleProvider.OVERLAY_TEXTURE, GT6CrucibleProvider.OVERLAY_TINT), tOverlay.getAllKeys(),
				"the solid face carries exactly the face pair (the fluid payload arm is retired)");
		// molten unbridged: the molten arm is the per-set molten sprite + the liquid tint —
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
		GT6CrucibleProvider.writeCrucibleData(tEdge, MT.Cu.mMeltingPoint, 2000, false, 16 * CS.U, tCopper);
		assertTrue(tEdge.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), "melting point <= temperature — the <= arm");
	}

	@Test
	public void everySolidMaterialAnswersTheTotalMatrix() {
		// the solid arm is the FIXED gray constant (task crucible-solid-gray-fidelity): iron —
		// never a bowl family row — answers the same upstream :590 face as every material, so
		// the wire record IS the SOLID_CONTENT constant. The ISE guard in overlayTag stays as
		// a belt-and-braces arm, just no longer reachable.
		List<OreDictMaterialStack> tIron = List.of(new OreDictMaterialStack(MT.Fe, 4 * CS.U));
		CompoundTag tTag = new CompoundTag();
		GT6CrucibleProvider.writeCrucibleData(tTag, 300, 2000, false, 16 * CS.U, tIron);
		assertFalse(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN));
		CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
		assertEquals(GT6CrucibleDatagen.SOLID_CONTENT.texture(), tOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE),
				"the total matrix: the solid wire record is the fixed gray, material-independent");
		assertEquals(GT6CrucibleDatagen.SOLID_CONTENT.tintARGB(), tOverlay.getInt(GT6CrucibleProvider.OVERLAY_TINT),
				"the total matrix: the CA_GRAY_64 gray rides the wire record");
		// the lightest walk itself is the BE mirror (TileEntitySmeltery.java:469-475 shape)
		assertEquals(MT.Fe, GT6CrucibleProvider.lightest(tIron).mMaterial);
		assertNull(GT6CrucibleProvider.lightest(List.of()));
	}

	/**
	 * task crucible-jade-tankbar-fill: the official-fluid payload arm is RETIRED — the
	 * molten face is the ContentFace payload (the per-set molten art + the mRGBaLiquid
	 * tint) on BOTH legs, bridged or not. Why: the bridged still art is the water texture
	 * with a light tint (GTFluids.java:54-56, no dedicated PNG) which read near-blank in
	 * the bar (the 2026-10-10 field feedback "molten is blank too"), and the upstream
	 * semantics anchor (MultiTileEntitySmeltery.java:585-591) draws the CONTENT face, not
	 * a fluid-cell element — the bar now shares its art source with the in-world pass.
	 * The bridge itself is untouched (pour/recycling/replicator consumers); the forge
	 * offline JVM never binds the seed RegistryObject (the FluidBridgeTest posture) while
	 * the 21.1 leg boots FML and binds it — irrelevant now, the payload has no fluid keys
	 * either way.
	 */
	@Test
	public void moltenOverlayIsAlwaysTheContentFacePayload() {
		for (OreDictMaterial tMaterial : Arrays.asList(MT.Fe, MT.Redstone, MT.W)) {
			CompoundTag tTag = new CompoundTag();
			GT6CrucibleProvider.writeCrucibleData(tTag, 4000, 9999, false, 16 * CS.U,
					List.of(new OreDictMaterialStack(tMaterial, 4 * CS.U)));
			assertTrue(tTag.getBoolean(GT6CrucibleProvider.KEY_MOLTEN), tMaterial.mNameInternal);
			GT6CrucibleDatagen.ContentFace tFace = GT6CrucibleDatagen.contentFace(tMaterial, true);
			CompoundTag tOverlay = tTag.getCompound(GT6CrucibleProvider.KEY_OVERLAY);
			assertEquals(tFace.texture(), tOverlay.getString(GT6CrucibleProvider.OVERLAY_TEXTURE),
					tMaterial.mNameInternal + " rides the ContentFace molten arm (bridged or not)");
			assertEquals(tFace.tintARGB(), tOverlay.getInt(GT6CrucibleProvider.OVERLAY_TINT), tMaterial.mNameInternal);
			// the payload key set pins the retirement: texture+tint ONLY — the retired fluid
			// keys must never reappear (no constant exists for them anymore, by design)
			assertEquals(Set.of(GT6CrucibleProvider.OVERLAY_TEXTURE, GT6CrucibleProvider.OVERLAY_TINT),
					tOverlay.getAllKeys(), tMaterial.mNameInternal + ": the payload carries exactly the face pair");
		}
	}

	/**
	 * The bridge coverage census (task jade-molten-bridge-full; alias seam task
	 * jade-molten-alias-seam): the bridge is a LIVE convention walk now (specOf id
	 * convention over GTFluids.chemicalSource + the iron W1 seed), so the coverage face =
	 * every registered material whose resolved fluid id (the {@code <name>_molten}
	 * convention OR the legacy FL alias table) is a SOURCE_SEAM key. The seam-key face is
	 * offline-stable on both legs (no registry binding involved). The five legacy-id rows
	 * (lithium_chloride_molten's underscore, glass/plastic/molten_latex/molten_hsla FL
	 * shorthands) ride the explicit alias seam now. The Jade bar no longer consumes this
	 * walk (the ContentFace arm is unconditional, task crucible-jade-tankbar-fill) — the
	 * bridge lives on for the pour/recycling/replicator consumers, which is what this
	 * census keeps pinned.
	 */
	@Test
	public void moltenBridgeCensusCoversTheFullDomain() {
		Set<String> tBridged = new TreeSet<>();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
			String tId = tMaterial.mNameInternal.toLowerCase(Locale.ROOT);
			if ("iron".equals(tId) || GTFluids.chemicalSource(gregtech6.fluid.FluidBridge.moltenIdForMaterial(tId)) != null) tBridged.add(tId);
		}
		assertEquals(41, tBridged.size(), "the full molten-domain census (39 -> 41: the chem-blocker batch adds the molten.aluminiumfluoride + molten.cryolite material moltens, task chem-fluids-unlock review fix): " + tBridged);
		// the original 7 seed materials + the expansion spot set stay bridged
		assertTrue(tBridged.containsAll(Arrays.asList("iron", "redstone", "silicon", "germanium",
				"redstonealloy", "nikolinealloy", "alumina", "tungsten", "tin", "sodium", "calcite",
				"wax", "chocolate", "enderpearl")), "seed + expansion spot set: " + tBridged);
		// the five legacy FL shorthand rows ride the alias seam now (task jade-molten-alias-seam)
		assertTrue(tBridged.containsAll(Arrays.asList("glass", "plastic", "latex", "hslasteel",
				"lithiumchloride")), "the legacy FL alias set: " + tBridged);
	}

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

	// ------------------------------------------------------------------------------------
	// group ⑤ the temperature ramp (task r11c-jade-bar-fluid-color ③ — the steel-heating
	// color ladder, the user ruling: the melt ceiling is the top)
	// ------------------------------------------------------------------------------------

	@Test
	public void theTemperatureRampAnswersTheSixStopsExactly() {
		// ratio 0 / 0.2 / 0.4 / 0.6 / 0.8 / 1.0 — black / dark red / red / orange / yellow /
		// white; ratio = temp/tempMax (the shell melt point x the heat-resistance bonus,
		// CruciblePhysics.temperatureMax :207-209), so ratio 1.0 IS the melt ceiling
		assertEquals(0xFF000000, GT6JadeRows.heatColor(0.0), "the ambient floor");
		assertEquals(0xFF8B0000, GT6JadeRows.heatColor(0.2), "the dark red band");
		assertEquals(0xFFFF0000, GT6JadeRows.heatColor(0.4), "the red band");
		assertEquals(0xFFFF8800, GT6JadeRows.heatColor(0.6), "the orange shared with the boiler heat bar");
		assertEquals(0xFFFFFF00, GT6JadeRows.heatColor(0.8), "the yellow band");
		assertEquals(0xFFFFFFFF, GT6JadeRows.heatColor(1.0), "the melt ceiling = white");
	}

	@Test
	public void theTemperatureRampInterpolatesMonotonicallyBetweenStops() {
		// two mid-band literal pins (the red->orange and yellow->white halves), then the
		// full 0..1 walk — every RGB channel non-decreasing (the continuous steel gradient,
		// never a backwards step)
		assertEquals(0xFFFF4400, GT6JadeRows.heatColor(0.5), "red->orange midpoint");
		assertEquals(0xFFFFFF80, GT6JadeRows.heatColor(0.9), "yellow->white midpoint");
		int tPrevR = 0, tPrevG = 0, tPrevB = 0;
		for (int tStep = 0; tStep <= 100; tStep++) {
			int tColor = GT6JadeRows.heatColor(tStep / 100.0);
			int tR = (tColor >>> 16) & 0xFF, tG = (tColor >>> 8) & 0xFF, tB = tColor & 0xFF;
			assertTrue(tR >= tPrevR && tG >= tPrevG && tB >= tPrevB,
					"a channel steps backwards at " + tStep + "%");
			tPrevR = tR;
			tPrevG = tG;
			tPrevB = tB;
		}
	}

	@Test
	public void theTemperatureRampClampsPastTheEdges() {
		assertEquals(0xFF000000, GT6JadeRows.heatColor(-1.0), "below zero clamps to black, never a wrapped channel");
		assertEquals(0xFFFFFFFF, GT6JadeRows.heatColor(1.5), "over the ceiling clamps to white");
		// the ramp feeds off the shared clamp: temp == tempMax pins ratio at 1.0 — exactly
		// where the melt-down warning gate lives (temp+100>max, CruciblePhysics :212-214)
		assertEquals(1.0F, GT6JadeRows.ratio(5000, 5000), 1e-6F);
	}

	// ------------------------------------------------------------------------------------
	// group ⑥ the wall anchor + the asset census (task crucible-jade-follower)
	// ------------------------------------------------------------------------------------

	@Test
	public void theWallBlockClassNeedsItsOwnClientAnchor() {
		// symptom A: Jade's client dispatch walks ONLY the superclass chain (jade-1201 impl
		// HierarchyLookup.java:70-75), so an anchor covers a block IFF anchor.isAssignableFrom
		// (block). The wall tree GTCrucibleWallBlock → GTMultiBlockPartBlock extends
		// BaseEntityBlock (GTMultiBlockPartBlock.java:62) — the GTEntityBlock anchor can never
		// answer it, which is why GT6JadePlugin carries the dedicated GTCrucibleWallBlock
		// registration line next to the original GTEntityBlock one. This pin goes RED if the
		// wall ever moves under GTEntityBlock — remove that line consciously then.
		assertFalse(gregtech6.block.GTEntityBlock.class.isAssignableFrom(gregtech6.block.multiblock.GTCrucibleWallBlock.class),
				"the wall joined the GTEntityBlock tree — the dedicated Jade anchor line in GT6JadePlugin is now redundant");
		assertTrue(gregtech6.block.GTEntityBlock.class.isAssignableFrom(gregtech6.block.multiblock.GTMultiBlockControllerBlock.class),
				"the controller stays on the GTEntityBlock tree (the original client leg)");
	}

	/**
	 * The B1 non-blank bar face, full census: the tank-bar overlay rides the contentFace
	 * texture ids through GT6ContentFaceElement → the BLOCK ATLAS sprite, and vanilla
	 * 1.20.1 stitches every {@code assets/<ns>/textures/block/**.png} (the atlases/blocks.json
	 * directory source — the only atlas face, no model reference needed). So the bar fill is
	 * non-blank IFF the asset file ships in the pack: this walk asserts the PNG for BOTH
	 * contentFace arms over the whole registered material universe, plus the animated
	 * molten strip's .mcmeta (the atlas loader refuses an unframed strip). The minecraft
	 * namespace row (the smooth-stone Stone borrow) rides the vanilla jar, not this pack.
	 */
	@Test
	public void everyContentFaceSpriteShipsInThePack() {
		int tChecked = 0;
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
			tChecked += assertFaceAsset(tMaterial, false);
			tChecked += assertFaceAsset(tMaterial, true);
		}
		assertTrue(tChecked >= 1000, "the census walked the real universe, got " + tChecked);
	}

	/** One arm of the census: the texture id → the pack asset path, PNG (+ mcmeta for molten). */
	private static int assertFaceAsset(OreDictMaterial aMaterial, boolean aMolten) {
		GT6CrucibleDatagen.ContentFace tFace = GT6CrucibleDatagen.contentFace(aMaterial, aMolten);
		int tColon = tFace.texture().indexOf(':');
		String tNamespace = tFace.texture().substring(0, tColon);
		String tPath = tFace.texture().substring(tColon + 1);
		if (tNamespace.equals("minecraft")) return 0; // the smooth-stone borrow rides the vanilla jar
		String tAsset = "assets/" + tNamespace + "/textures/" + tPath + ".png";
		assertNotNull(GT6CrucibleProviderTest.class.getClassLoader().getResource(tAsset),
				aMaterial.mNameInternal + " (" + (aMolten ? "molten" : "solid") + ") → " + tAsset + " missing — the Jade bar fill would draw the missing sprite");
		if (aMolten) {
			assertNotNull(GT6CrucibleProviderTest.class.getClassLoader().getResource(tAsset + ".mcmeta"),
					tAsset + ".mcmeta missing — the animated molten strip would fail the atlas stitch");
		}
		return 1;
	}

	// ------------------------------------------------------------------------------------
	// group ⑦ the render primitive (task crucible-jade-tankbar-fill)
	// ------------------------------------------------------------------------------------

	/**
	 * The atlas pre-bind pin, as a source-structure assertion (the element's render path is
	 * live-only client code — no offline GL to drive): GT6ContentFaceElement.render must
	 * carry {@code RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)} BEFORE the
	 * blit call. The Jade precedent never relies on a draw primitive's implicit binding
	 * (DisplayHelper.drawTiledSprite binds first, 1.20.1 :341 / 1.21.1 :278; the vanilla
	 * innerBlit does re-bind internally on both legs — 1.20.1 GuiGraphics.java:346-347,
	 * 1.21.1 :586 — so the pre-bind is redundancy by design: the primitive stays
	 * self-contained against foreign RenderSystem texture state around the overlay pass).
	 * Goes RED if either half of the pair is removed — remove consciously then, with the
	 * class doc's attribution chain re-checked.
	 */
	@Test
	public void contentFaceElementBindsTheAtlasBeforeBlitting() {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		String tSource;
		try {
			tSource = Files.readString(tMdk.resolve("src/main/java/gregtech6/jade/GT6ContentFaceElement.java"));
		} catch (IOException tMissing) {
			throw new AssertionError("the element source is unreadable from " + tMdk, tMissing);
		}
		int tBind = tSource.indexOf("RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)");
		int tBlit = tSource.indexOf("aGraphics.blit(");
		assertTrue(tBind >= 0, "the explicit atlas pre-bind is gone from GT6ContentFaceElement.render — restore it (the Jade DisplayHelper precedent)");
		assertTrue(tBlit >= 0, "the blit call itself is gone — the element draws nothing");
		assertTrue(tBlit > tBind, "the bind must precede the blit call in render()");
	}

	/** The mdk root from the test working directory (the GT6TintChainHopperGrindstoneSiftingTest form). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"))) {
				return tDir;
			}
		}
		return null;
	}
}
