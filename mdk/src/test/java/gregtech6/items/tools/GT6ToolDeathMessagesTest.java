package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

/**
 * The tool death-message census (task easter-s1-death-messages): the table is pinned
 * verbatim against the upstream grep — 45 String {@code getDeathMessage()} overrides
 * (44 plain lines + the Club pair GT_Tool_Club.java:134/:139) plus the ToolStats.java:104
 * default; the port keys fan out to 67 rows (45 flat/pocket classes + the 19 electric
 * tier rows + the 3 guns) — the census research.easter-egg-census domain E. The
 * compose fold pins the DamageSources.java:110-128 semantics (GREEN killer / RED
 * victim, the Crazy↔Bear pairings :113-115 incl. the :115 white "!" tail, the club's
 * Bear989Sr line GT_Tool_Club.java:139-140) and the tool-domain filter (table miss →
 * null → the vanilla message stands). Pure statics — no registry, leg-identical.
 */
public class GT6ToolDeathMessagesTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// registries are ready by now even where the NetworkHooks boot fails offline
		}
	}

	@Test
	void censusMatchesUpstreamVerbatim() {
		assertEquals(Map.ofEntries(
				Map.entry("crowbar", "[VICTIM] lost Half a Life to [KILLER]"),
				Map.entry("cutter", "[KILLER] has cut the Cable for the Life Support Machine of [VICTIM]"),
				Map.entry("chisel", "[VICTIM] got a Statue made by [KILLER]"),
				Map.entry("file", "[VICTIM] has been filed D for 'Dead' by [KILLER]"),
				Map.entry("saw", "[KILLER] failed to perform the 'sawing a woman in half' trick on [VICTIM]"),
				Map.entry("builder_wand", "[VICTIM] has been poofed out of existence by [KILLER]"),
				Map.entry("screwdriver", "[VICTIM] has screwed with [KILLER] for the last time!"),
				Map.entry("hammer", "[VICTIM] was squashed by [KILLER]"),
				Map.entry("wrench", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("bending_cylinder_small", "[VICTIM] got bent into shape by [KILLER]"),
				Map.entry("bending_cylinder", "[VICTIM] got bent into shape by [KILLER]"),
				Map.entry("pickaxe", "[VICTIM] got mined by [KILLER]"),
				Map.entry("pickaxe_gem", "[VICTIM] got mined by [KILLER]"),
				Map.entry("pickaxe_construction", "[VICTIM] got demolished by [KILLER]"),
				Map.entry("shovel", "[VICTIM] got dug up by [KILLER]"),
				Map.entry("spade", "[VICTIM] got aced by [KILLER]"),
				Map.entry("universal_spade", "[VICTIM] has been digged by [KILLER]"),
				Map.entry("sword", GT6ToolDeathMessages.DEFAULT_MESSAGE),
				Map.entry("knife", "<[VICTIM]> [KILLER] what are you doing?, [KILLER]?!? STAHP!!!"),
				Map.entry("butchery_knife", "[KILLER] butchered [VICTIM]!"),
				Map.entry("club", GT6ToolDeathMessages.CLUB_MESSAGE),
				Map.entry("axe", "[VICTIM] has been chopped by [KILLER]"),
				Map.entry("axe_double", "[VICTIM] got beheaded by [KILLER]"),
				Map.entry("soft_hammer", "[VICTIM] got hammered to death by [KILLER]"),
				Map.entry("monkey_wrench", "[KILLER] threw a Monkey Wrench into the Plans of [VICTIM]"),
				Map.entry("magnifying_glass", "[VICTIM] got investigated very closely by [KILLER]"),
				Map.entry("pincers", "[KILLER] pulled a tooth out of [VICTIM]"),
				Map.entry("hoe", "[VICTIM] has been called a stupid Hoe by [KILLER]"),
				Map.entry("plow", "[KILLER] plew through the yard of [VICTIM]"),
				Map.entry("branch_cutter", "[VICTIM] has been trimmed by [KILLER]"),
				Map.entry("sense", "[KILLER] has reaped the Soul of [VICTIM]"),
				Map.entry("hand_drill", "[VICTIM] has been tortured by [KILLER]"),
				Map.entry("scissors", "[KILLER] ran into [VICTIM] while holding Scissors"),
				Map.entry("scoop", "[VICTIM] got scooped up by [KILLER]"),
				Map.entry("plunger", "[VICTIM] got stuck trying to escape through a Pipe while fighting [KILLER]"),
				Map.entry("flint_and_tinder", "[VICTIM] has been ignited by [KILLER]"),
				Map.entry("rolling_pin", "[VICTIM] got flattened by [KILLER]"),
				Map.entry("pocket_multitool", GT6ToolDeathMessages.DEFAULT_MESSAGE),
				Map.entry("pocket_multitool_knife", "[KILLER] whacked [VICTIM] to death with a closed Pocket Knife"),
				Map.entry("pocket_multitool_saw", "[KILLER] failed to perform the 'sawing a woman in half' trick on [VICTIM]"),
				Map.entry("pocket_multitool_file", "[VICTIM] has been filed D for 'Dead' by [KILLER]"),
				Map.entry("pocket_multitool_screwdriver", "[VICTIM] has screwed with [KILLER] for the last time!"),
				Map.entry("pocket_multitool_wire_cutter", "[KILLER] has cut the Cable for the Life Support Machine of [VICTIM]"),
				Map.entry("pocket_multitool_scissors", "[KILLER] ran into [VICTIM] while holding Scissors"),
				Map.entry("pocket_multitool_chisel", "[VICTIM] got a Statue made by [KILLER]"),
				Map.entry("pistol", "[VICTIM] got pistol-whipped over the head by [KILLER]"),
				Map.entry("carbine", "[VICTIM] got whacked over the head by [KILLER]"),
				Map.entry("rifle", "[VICTIM] got melee'd by [KILLER]"),
				Map.entry("mining_drill_lv", "[VICTIM] has met Dentist Dr. [KILLER]"),
				Map.entry("mining_drill_mv", "[VICTIM] has met Dentist Dr. [KILLER]"),
				Map.entry("mining_drill_hv", "[VICTIM] has met Dentist Dr. [KILLER]"),
				Map.entry("chainsaw_lv", "[VICTIM] was massacred by [KILLER]"),
				Map.entry("chainsaw_mv", "[VICTIM] was massacred by [KILLER]"),
				Map.entry("chainsaw_hv", "[VICTIM] was massacred by [KILLER]"),
				Map.entry("wrench_lv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("wrench_mv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("wrench_hv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("jackhammer_hv_normal", "[VICTIM] has been jackhammered into pieces by [KILLER]"),
				Map.entry("jackhammer_hv_no_ores", "[VICTIM] has been jackhammered into pieces by [KILLER]"),
				Map.entry("buzzsaw_lv", "[VICTIM] got buzzed by [KILLER]"),
				Map.entry("screwdriver_lv", "[VICTIM] has screwed with [KILLER] for the last time!"),
				Map.entry("hand_drill_lv", "[VICTIM] needed help with a few holes and [KILLER] gladly helped"),
				Map.entry("hand_mixer_lv", "[KILLER] mixed up [VICTIM] with the Ingredients"),
				Map.entry("monkey_wrench_lv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("monkey_wrench_mv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("monkey_wrench_hv", "[KILLER] gave [VICTIM] a whack with the Wrench!"),
				Map.entry("trimmer_lv", "[VICTIM] has been trimmed by [KILLER]")),
				GT6ToolDeathMessages.table());
	}

	/** ToolStats.java:104 verbatim — the default self-mocking line (the sword / pocket base carry it). */
	@Test
	void defaultLineIsTheToolStatsSelfMock() {
		assertEquals("Why is there no custom Death Message for this Tool?", GT6ToolDeathMessages.DEFAULT_MESSAGE);
		assertEquals(GT6ToolDeathMessages.DEFAULT_MESSAGE, GT6ToolDeathMessages.templateOf("sword"));
		assertEquals(GT6ToolDeathMessages.DEFAULT_MESSAGE, GT6ToolDeathMessages.templateOf("pocket_multitool"));
		// the default carries markers nowhere — it renders as the plain literal
		assertEquals(GT6ToolDeathMessages.DEFAULT_MESSAGE,
				GT6ToolDeathMessages.compose("Hermit", "Griefer", GT6ToolDeathMessages.DEFAULT_MESSAGE).getString());
	}

	/** DamageSources.java:122 — GREEN killer, RED victim, verbatim MonkeyWrench:52 line. */
	@Test
	void customLineColorsKillerGreenAndVictimRed() {
		Component tMsg = GT6ToolDeathMessages.compose("Hermit", "Griefer",
				GT6ToolDeathMessages.templateOf("monkey_wrench"));
		assertEquals("Hermit threw a Monkey Wrench into the Plans of Griefer", tMsg.getString());
		assertTrue(tMsg.getSiblings().stream().anyMatch(s -> "Hermit".equals(s.getString())
				&& colored(s, ChatFormatting.GREEN)), "the killer rides GREEN");
		assertTrue(tMsg.getSiblings().stream().anyMatch(s -> "Griefer".equals(s.getString())
				&& colored(s, ChatFormatting.RED)), "the victim rides RED");
	}

	/** GT_Tool_Knife.java:79 — the double-killer template keeps order and colors both mentions. */
	@Test
	void knifeTemplateSplicesBothKillerMentions() {
		Component tMsg = GT6ToolDeathMessages.compose("Hermit", "Griefer", GT6ToolDeathMessages.templateOf("knife"));
		assertEquals("<Griefer> Hermit what are you doing?, Hermit?!? STAHP!!!", tMsg.getString());
		assertEquals(2, tMsg.getSiblings().stream()
				.filter(s -> "Hermit".equals(s.getString()) && colored(s, ChatFormatting.GREEN)).count(),
				"both [KILLER] mentions green");
	}

	/** DamageSources.java:114 — the Crazy↔Bear989jr pairing outranks any template. */
	@Test
	void mrsCrazyPardonsJunior() {
		Component tMsg = GT6ToolDeathMessages.compose("CrazyJ1984", "Bear989jr",
				GT6ToolDeathMessages.templateOf("hammer"));
		assertEquals("<Mrs. Crazy> Sorry Junior", tMsg.getString());
		assertTrue(tMsg.getSiblings().stream().anyMatch(s -> "Mrs. Crazy".equals(s.getString())
				&& colored(s, ChatFormatting.LIGHT_PURPLE)), "the speaker rides LIGHT_PURPLE");
		assertTrue(tMsg.getSiblings().stream().anyMatch(s -> "Junior".equals(s.getString())
				&& colored(s, ChatFormatting.RED)), "Junior rides RED");
		// the :113 spelling gate — the historic CrazyJ84 alias answers the same
		assertEquals("<Mrs. Crazy> Sorry Junior",
				GT6ToolDeathMessages.compose("CrazyJ84", "Bear989jr", GT6ToolDeathMessages.templateOf("hammer")).getString());
	}

	/** DamageSources.java:115 — the Bear989Sr pairing. */
	@Test
	void mrsCrazyHushesBear() {
		assertEquals("<Mrs. Crazy> Hush it!, Bear!",
				GT6ToolDeathMessages.compose("CrazyJ1984", "Bear989Sr", GT6ToolDeathMessages.templateOf("hammer")).getString());
	}

	/** GT_Tool_Club.java:139-140 — the Bear989Sr killer exclusive, red victim, verbatim. */
	@Test
	void clubAnswersTheBearKiller() {
		Component tMsg = GT6ToolDeathMessages.compose("Bear989Sr", "Griefer", GT6ToolDeathMessages.CLUB_MESSAGE);
		assertEquals("Griefer got clubbed by a Bear!", tMsg.getString());
		assertTrue(tMsg.getSiblings().stream().anyMatch(s -> "Griefer".equals(s.getString())
				&& colored(s, ChatFormatting.RED)), "the victim rides RED");
		// a different killer keeps the plain club line ([VICTIM]-first order sanity)
		assertEquals("Griefer got welcomed into the club by Hermit",
				GT6ToolDeathMessages.compose("Hermit", "Griefer", GT6ToolDeathMessages.CLUB_MESSAGE).getString());
	}

	/** The club special-case is club-scoped: other templates answer Bear989Sr normally. */
	@Test
	void bearKillerOnlyTrumpsTheClub() {
		assertEquals("Griefer was squashed by Bear989Sr",
				GT6ToolDeathMessages.compose("Bear989Sr", "Griefer", GT6ToolDeathMessages.templateOf("hammer")).getString());
	}

	/** The tool-domain filter — a miss returns null so the vanilla message stands. */
	@Test
	void tableMissFallsBackToVanilla() {
		assertNull(GT6ToolDeathMessages.templateOf("diamond_sword"));
		assertNull(GT6ToolDeathMessages.templateOf("air"));
	}

	/** The legacy-format equivalence: the sibling's resolved color equals the code's (DamageSources.java:122 fold). */
	static boolean colored(Component aPart, ChatFormatting aColor) {
		return aPart.getStyle().getColor() != null && aColor.getColor() != null
				&& aPart.getStyle().getColor().getValue() == aColor.getColor();
	}
}
