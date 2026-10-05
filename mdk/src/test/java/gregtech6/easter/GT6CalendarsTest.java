/**
 * The offline pin suite for the date-flag skeleton + April Fools' pack (task
 * easter-s2-date-flags-fools). Pins: the four CS.java:870-873 windows over an injected
 * clock (boundary days both sides), the config channel (GT_API.java:359-361 keys, one-way
 * force-on), the 112-row rename census (GT_API.java:363-475 + sFoolRenames), the wood tail
 * (:477, post-explicit order), the display seam (materialFill/foolFill), the bullet Bolt
 * face (LanguageHandler.java:169-179) and the login lines (GT_Client.java:117-122).
 *
 * <p>Hygiene: the fool renames mutate {@code mNameLocal} in the SHARED registry, and other
 * suite classes (lang walks, datagen parity) read it — every test flips flags through
 * {@link #foolsOn()} and {@link #restore()} snapshots/restores every local name back.
 */
package gregtech6.easter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.block.tree.GT6TreeXmasItem;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.items.tools.GTPistolItem;

public class GT6CalendarsTest {

	private static final Map<OreDictMaterial, String> SNAPSHOT = new HashMap<>();
	private static boolean sSnapshotTaken = false;

	@BeforeAll
	public static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		gregtech6.registry.GTMaterialItems.initMaterials();
		if (!sSnapshotTaken) {
			for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values())
				SNAPSHOT.put(tMaterial, tMaterial.mNameLocal);
			sSnapshotTaken = true;
		}
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

	/** Turns the fool flag on through the config channel (the production path, empty file). */
	private static void foolsOn() {
		Properties tProps = new Properties();
		tProps.setProperty(GT6Calendars.KEY_APRIL_FOOLS, "true");
		GT6Calendars.applyOverrides(tProps);
	}

	// ---------------------------------------------------------------------------
	// The four windows (CS.java:870-873, code truth — three open one day early)
	// ---------------------------------------------------------------------------

	/** Local-noon millis for a Y/M/D in the default zone (avoids zone-edge flake). */
	private static long noon(int aYear, int aMonth, int aDay) {
		return java.time.LocalDateTime.of(aYear, aMonth, aDay, 12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static void pin(int aYear, int aMonth, int aDay, int aIndex, boolean aExpected) {
		assertEquals(aExpected, GT6Calendars.computeFlags(noon(aYear, aMonth, aDay))[aIndex],
				aYear + "-" + aMonth + "-" + aDay + " flag[" + aIndex + "]");
	}

	private static void pin(int aMonth, int aDay, int aIndex, boolean aExpected) {
		pin(2026, aMonth, aDay, aIndex, aExpected);
	}

	@Test
	public void aprilFoolsWindowIsAprilOneTwo() {
		// CS.java:870 — "1st of April, first two days of the month" (<= 2, NO one-day-early tail)
		pin(3, 31, 0, false);
		pin(4, 1, 0, true);
		pin(4, 2, 0, true);
		pin(4, 3, 0, false);
	}

	@Test
	public void woodmansWindowOpensJuneTwenty() {
		// CS.java:871 — getDate() >= 20, the comment says the 21st, the CODE opens the 20th
		pin(6, 19, 1, false);
		pin(6, 20, 1, true);
		pin(6, 30, 1, true);
		pin(7, 1, 1, false);
	}

	@Test
	public void xmasInJulyWindowOpensJulyTwentyThree() {
		// CS.java:872 — getDate() >= 23, "one day early"
		pin(7, 22, 2, false);
		pin(7, 23, 2, true);
		pin(7, 31, 2, true);
		pin(8, 1, 2, false);
	}

	@Test
	public void xmasInDecemberWindowOpensDecemberFive() {
		// CS.java:873 — getDate() >= 5, "one day early"; the year boundary stays down
		pin(12, 4, 3, false);
		pin(12, 5, 3, true);
		pin(12, 31, 3, true);
		pin(2027, 1, 1, 3, false);
	}

	@Test
	public void flagsBindOnceAtClassLoad() {
		// the static init bound the wall clock — the live flags match the pure function now
		boolean[] tNow = GT6Calendars.computeFlags(System.currentTimeMillis());
		assertEquals(tNow[0], GT6Calendars.APRIL_FOOLS);
		assertEquals(tNow[1], GT6Calendars.WOODMANS_BDAY);
		assertEquals(tNow[2], GT6Calendars.XMAS_IN_JULY);
		assertEquals(tNow[3], GT6Calendars.XMAS_IN_DECEMBER);
	}

	// ---------------------------------------------------------------------------
	// The config channel (GT_API.java:359-361 — one-way force-on)
	// ---------------------------------------------------------------------------

	@Test
	public void configChannelForcesFlagsOnOneWay() {
		Properties tOn = new Properties();
		tOn.setProperty(GT6Calendars.KEY_APRIL_FOOLS, "true");
		tOn.setProperty(GT6Calendars.KEY_XMAS_JULY, "true");
		tOn.setProperty(GT6Calendars.KEY_XMAS_DECEMBER, "true");
		GT6Calendars.applyOverrides(tOn);
		assertTrue(GT6Calendars.APRIL_FOOLS);
		assertTrue(GT6Calendars.XMAS_IN_JULY);
		assertTrue(GT6Calendars.XMAS_IN_DECEMBER);
		// one-way: defaults can never turn a live flag off; WOODMANS_BDAY has NO key (upstream parity)
		GT6Calendars.applyOverrides(new Properties());
		assertTrue(GT6Calendars.APRIL_FOOLS);
		assertTrue(GT6Calendars.XMAS_IN_JULY);
		assertTrue(GT6Calendars.XMAS_IN_DECEMBER);
		assertFalse(GT6Calendars.WOODMANS_BDAY, "no woodmans key upstream");
	}

	// ---------------------------------------------------------------------------
	// The rename table (GT_API.java:363-477)
	// ---------------------------------------------------------------------------

	@Test
	public void renameTableCensusIsOneHundredTwelveRows() {
		assertEquals(0, GT6Calendars.sFoolRenames, "no renames while the flag is down");
		foolsOn();
		assertEquals(112, GT6Calendars.sFoolRenames, "GT_API.java:364-475, one row per setLocal");
		GT6Calendars.ensureFoolsApplied(); // idempotent
		assertEquals(112, GT6Calendars.sFoolRenames, "the table applies exactly once");
	}

	@Test
	public void verbatimRenameSamples() {
		foolsOn();
		// five verbatim rows + the Au<->Pyrite swap pair (GT_API.java:391-392/393/400/430/448/470)
		assertEquals("Pyrite", MT.Au.mNameLocal);
		assertEquals("Gold", MT.Pyrite.mNameLocal);
		assertEquals("Irun", MT.Fe.mNameLocal);
		assertEquals("Style", MT.Steel.mNameLocal);
		assertEquals("LEGO", MT.Plastic.mNameLocal);
		assertEquals("Crossbow Powder", MT.Gunpowder.mNameLocal);
		assertEquals("UwU-Matter", MT.UUMatter.mNameLocal);
	}

	@Test
	public void woodTailAppendsAfterExplicitRows() {
		foolsOn();
		// GT_API.java:394 renames IronWood first, so the :477 loop hits it AS "Irunwood"
		assertEquals("Irunwood >:] nice", MT.IronWood.mNameLocal);
		// :439 "Dork Wood" contains "wood" -> the tail rides it too
		assertEquals("Dork Wood >:] nice", MT.WOODS.Darkwood.mNameLocal);
		// an untouched wood material gets the bare tail
		assertTrue(MT.Wood.mNameLocal.endsWith(" >:] nice"), "the wood loop tail: " + MT.Wood.mNameLocal);
		assertTrue(MT.CertusQuartz.mNameLocal.equals("Citrus Quartz"), "non-wood materials untouched by the tail");
	}

	// ---------------------------------------------------------------------------
	// The display seam
	// ---------------------------------------------------------------------------

	@Test
	public void materialFillStaysTranslatableWhenFlagDown() {
		// MT.Au's internal name is "Gold" (the oredict word; "Au" is the field symbol) — the key follows it
		TranslatableContents tFill = (TranslatableContents) MaterialPrefixItem.materialFill(MT.Au).getContents();
		assertEquals("gt6.material.gold", tFill.getKey(), "the lang-key small unit");
	}

	@Test
	public void materialFillTurnsLiteralFoolWordWhenFlagUp() {
		foolsOn();
		// the fool word rides a plain literal (not the lang key) — leg-neutral pin: the
		// 1.21.1 contents class is PlainTextContents.Literal, so "literal" is asserted as
		// "the exact word AND not translatable" instead of by contents type
		Component tFill = MaterialPrefixItem.materialFill(MT.Au);
		assertEquals("Pyrite", tFill.getString(), "the live renamed word (Au -> Pyrite)");
		assertFalse(tFill.getContents() instanceof TranslatableContents, "no lang key behind the joke word");
	}

	@Test
	public void bulletFoolFillSpeaksBolt() {
		// LanguageHandler.java:169-179 — String fills, leg-neutral
		assertFalse(GT6Calendars.APRIL_FOOLS);
		foolsOn();
		assertEquals("Bolt Shaft", GT6Calendars.foolFill(OP.bulletGtSmall, MT.Empty), ":171 Empty -> Bolt Shaft");
		assertEquals("Bolt Shaft", GT6Calendars.foolFill(OP.bulletGtLarge, MT.Empty), ":173");
		assertEquals("Pyrite Bolt", GT6Calendars.foolFill(OP.bulletGtMedium, MT.Au), ":176 the renamed word rides it");
		assertEquals("Irun", GT6Calendars.foolFill(OP.ingot, MT.Fe), "non-bullet prefixes: bare word");
	}

	// the getName(ItemStack) override is a one-line delegation onto the pinned foolFill —
	// NOT instance-pinned: constructing an Item after Bootstrap freezes the registry
	// (Item.java:61 createIntrusiveHolder, the GTWireDisplayNameTest posture)

	@Test
	public void wireNamesRideTheSameSeam() {
		// flags down: the wire slot is the plain translatable unit (the GTWireDisplayNameTest face)
		Component tMaterial = (Component) ((TranslatableContents) GTWireBlock.displayNameOf(MT.Au,
				gregtech6.registry.GTWireSpecs.Row.Family.ELECTRIC, 1, false, false).getContents()).getArgs()[1];
		assertEquals("gt6.material.gold", ((TranslatableContents) tMaterial.getContents()).getKey());
		// flags up: the fool word rides the same slot as a plain literal
		foolsOn();
		Component tFooled = (Component) ((TranslatableContents) GTWireBlock.displayNameOf(MT.Au,
				gregtech6.registry.GTWireSpecs.Row.Family.ELECTRIC, 1, false, false).getContents()).getArgs()[1];
		assertEquals("Pyrite", tFooled.getString());
		assertFalse(tFooled.getContents() instanceof TranslatableContents);
	}

	// ---------------------------------------------------------------------------
	// The month binding + the maple season table (GT_API_Proxy_Client.java:144-167)
	// ---------------------------------------------------------------------------

	@Test
	public void monthBindsOnceAtClassLoad() {
		// the static init bound the wall clock beside the flags (the :144 new Date() face —
		// computed once per launch, never refreshed; same declared semantics as flagsBindOnceAtClassLoad)
		assertEquals(GT6Calendars.computeMonth(System.currentTimeMillis()), GT6Calendars.sMonth);
		assertTrue(GT6Calendars.sMonth >= 1 && GT6Calendars.sMonth <= 12);
	}

	@Test
	public void computeMonthReadsTheCalendarMonth() {
		assertEquals(1, GT6Calendars.computeMonth(noon(2026, 1, 15)));
		assertEquals(6, GT6Calendars.computeMonth(noon(2026, 6, 20)));
		assertEquals(9, GT6Calendars.computeMonth(noon(2026, 9, 30)));
		assertEquals(12, GT6Calendars.computeMonth(noon(2026, 12, 5)));
	}

	@Test
	public void mapleSeasonTableIsTheUpstreamSwitch() {
		// GT_API_Proxy_Client.java:146-167 verbatim: case 1/12 BROWN, 9 YELLOW, 10 ORANGE,
		// 11 RED; every other month falls through the switch (the pre-coloured base art stays)
		assertEquals(GT6Calendars.MapleSeason.BROWN, GT6Calendars.mapleSeasonOfMonth(1));
		assertEquals(GT6Calendars.MapleSeason.NONE, GT6Calendars.mapleSeasonOfMonth(2));
		assertEquals(GT6Calendars.MapleSeason.NONE, GT6Calendars.mapleSeasonOfMonth(8));
		assertEquals(GT6Calendars.MapleSeason.YELLOW, GT6Calendars.mapleSeasonOfMonth(9));
		assertEquals(GT6Calendars.MapleSeason.ORANGE, GT6Calendars.mapleSeasonOfMonth(10));
		assertEquals(GT6Calendars.MapleSeason.RED, GT6Calendars.mapleSeasonOfMonth(11));
		assertEquals(GT6Calendars.MapleSeason.BROWN, GT6Calendars.mapleSeasonOfMonth(12));
	}

	// ---------------------------------------------------------------------------
	// The RAINBOW_SLOW tooltip cycle (GT_API_Proxy_Client.java:571-582)
	// ---------------------------------------------------------------------------

	@Test
	public void rainbowSlowCyclesTheTenUpstreamColors() {
		// CLIENT_TIME steps 25 ticks per color = 1.25 s (the 1.7.10 client tick), full cycle 12.5 s;
		// the LH.Chat:687-701 constants map onto ChatFormatting GOLD/AQUA/DARK_AQUA/DARK_BLUE/
		// DARK_PURPLE/LIGHT_PURPLE verbatim
		long t0 = 1_700_000_000_000L;
		assertEquals(ChatFormatting.RED, GT6Calendars.rainbowSlow(t0), ":572 case 0");
		assertEquals(ChatFormatting.GOLD, GT6Calendars.rainbowSlow(t0 + 1250), ":573 ORANGE=GOLD");
		assertEquals(ChatFormatting.YELLOW, GT6Calendars.rainbowSlow(t0 + 2500), ":574");
		assertEquals(ChatFormatting.GREEN, GT6Calendars.rainbowSlow(t0 + 3750), ":575");
		assertEquals(ChatFormatting.AQUA, GT6Calendars.rainbowSlow(t0 + 5000), ":576 CYAN=AQUA");
		assertEquals(ChatFormatting.DARK_AQUA, GT6Calendars.rainbowSlow(t0 + 6250), ":577 DCYAN");
		assertEquals(ChatFormatting.DARK_BLUE, GT6Calendars.rainbowSlow(t0 + 7500), ":578 DBLUE");
		assertEquals(ChatFormatting.BLUE, GT6Calendars.rainbowSlow(t0 + 8750), ":579");
		assertEquals(ChatFormatting.DARK_PURPLE, GT6Calendars.rainbowSlow(t0 + 10000), ":580 PURPLE");
		assertEquals(ChatFormatting.LIGHT_PURPLE, GT6Calendars.rainbowSlow(t0 + 11250), ":581 PINK");
		assertEquals(ChatFormatting.RED, GT6Calendars.rainbowSlow(t0 + 12500), "the cycle wraps at 250 ticks");
		// mid-step holds the same color (a switch on the 25-tick slot, not a smooth lerp)
		assertEquals(GT6Calendars.rainbowSlow(t0), GT6Calendars.rainbowSlow(t0 + 1249));
	}

	// ---------------------------------------------------------------------------
	// The guns (Loader_Tools.java:198-200) and the login lines (GT_Client.java:117-122)
	// ---------------------------------------------------------------------------

	@Test
	public void gunsSpeakCrossbowDuringFools() {
		assertEquals("Small Crossbow", GTPistolItem.foolDisplayName(GTPistolItem.Kind.PISTOL).getString(), ":198");
		assertEquals("Medium Crossbow", GTPistolItem.foolDisplayName(GTPistolItem.Kind.CARBINE).getString(), ":199");
		assertEquals("Big Crossbow", GTPistolItem.foolDisplayName(GTPistolItem.Kind.RIFLE).getString(), ":200");
	}

	@Test
	public void loginLinesFirePerFlagInUpstreamOrder() {
		assertTrue(GT6Calendars.loginLines().isEmpty(), "no flags, no lines");
		GT6Calendars.WOODMANS_BDAY = true;
		List<Component> tOne = GT6Calendars.loginLines();
		assertEquals(1, tOne.size());
		assertEquals("<>:]> Have a nice day!", tOne.get(0).getString(), "GT_Client.java:118 verbatim");
		GT6Calendars.APRIL_FOOLS = true;
		List<Component> tBoth = GT6Calendars.loginLines();
		assertEquals(2, tBoth.size(), "WOODMANS_BDAY fires first (:117), APRIL_FOOLS second (:120)");
		assertEquals("<GregoriusT> Watch your Calendar!", tBoth.get(1).getString(), ":121 verbatim (CHAT_GREG face)");
		// the words ride literals — no lang keys behind the jokes
		assertFalse(tBoth.get(0).getContents() instanceof TranslatableContents);
		assertFalse(tBoth.get(1).getContents() instanceof TranslatableContents);
	}

	// ---------------------------------------------------------------------------
	// The Christmas-in-July tooltip census (the 8-file grep of "Christmas in July")
	// ---------------------------------------------------------------------------

	/**
	 * The 8-carrier census (research.easter-egg-census id1451 face 3): every upstream file
	 * gates the SAME RAINBOW_SLOW line on XMAS_IN_JULY, differing only in the meta guard —
	 * {upstream anchor, meta guard, port carrier (null = dormant, no port domain)}.
	 */
	private static final String[][] XMAS_TOOLTIP_CENSUS = {
		{"BlockTreePlanks2.java:96-98",            "aMeta == 0",       "planks"},
		{"BlockTreeLogC.java:105-106",             "(aMeta & 3) == 0", "log"},
		{"BlockTreeLeavesCD.java:130-131",         "(aMeta & 7) == 0", "leaves"},
		{"BlockTreeSaplingCD.java:110-111",        "(aMeta & 7) == 0", "sapling"},
		{"BlockTreePlanks2FireProof.java:96-97",   "aMeta == 0",       null},
		{"BlockTreeLogCFireProof.java:103-104",    "(aMeta & 3) == 0", null},
		{"BlockTreeBeamC.java:61-62",              "(aMeta & 3) == 0", null},
		{"BlockTreeBeamCFireProof.java:61-62",     "(aMeta & 3) == 0", null},
	};

	@Test
	public void xmasTooltipCensusSharesTheOneRainbowLineWithAtLeastThreeCarriers() {
		// the census shape: 8 rows, >= 3 sampled into port carriers (the fireproof twins +
		// the Blue Spruce beams stay dormant — no port domain)
		assertEquals(8, XMAS_TOOLTIP_CENSUS.length, "the full grep census");
		int tCarried = 0;
		for (String[] tRow : XMAS_TOOLTIP_CENSUS) if (tRow[2] != null) tCarried++;
		assertTrue(tCarried >= 3, "acceptance: at least 3 of the 8 rows sampled into port carriers, got " + tCarried);
		// the sampled rows' shared body, live: flag down no line; flag up exactly the one
		// verbatim literal line in the pinned RAINBOW_SLOW colour (injected clock — no race)
		assertFalse(GT6Calendars.XMAS_IN_JULY);
		List<Component> tTooltip = new ArrayList<>();
		GT6TreeXmasItem.addXmasLine(tTooltip, 0);
		assertTrue(tTooltip.isEmpty(), "flag down, no line");
		long t0 = 1_700_000_000_000L;
		GT6Calendars.XMAS_IN_JULY = true;
		GT6TreeXmasItem.addXmasLine(tTooltip, t0);
		assertEquals(1, tTooltip.size(), "exactly the one line");
		Component tLine = tTooltip.get(0);
		assertEquals("Save on everything at Christmas in July!", tLine.getString(), "the verbatim census text");
		assertFalse(tLine.getContents() instanceof TranslatableContents, "no lang key behind the joke line");
		assertEquals(ChatFormatting.RED.getColor().intValue(), tLine.getStyle().getColor().getValue(),
				"RAINBOW_SLOW slot 0 at t0 (:572) rides the style");
		GT6TreeXmasItem.addXmasLine(tTooltip, t0 + 5000);
		assertEquals(ChatFormatting.AQUA.getColor().intValue(), tTooltip.get(1).getStyle().getColor().getValue(),
				"slot 4 CYAN=AQUA (:576) — the tooltip colour animates with the cycle");
	}
}
