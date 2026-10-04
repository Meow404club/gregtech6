package gregtech6.items.tools;

import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The GT6 tool death-message table (task easter-s1-death-messages, the census
 * research.easter-egg-census domain E). Upstream every tool carries a self-mocking
 * chat line for the entity it kills — the per-tool {@code getDeathMessage()} override
 * (45 tool classes) with the {@code [KILLER]}-green/{@code [VICTIM]}-red assembly at
 * gregapi/damage/DamageSources.java:110-128 and the ToolStats.java:104 default for
 * the tools without a custom line.
 *
 * <p>Modern 1.20.1/1.21.1 folds: the melee kill lands on the vanilla
 * {@code playerAttack} source (Player.java:1142 — no item-side hook), so the table is
 * keyed by the killing tool's registry path and consulted by
 * {@link GT6ToolDeathListener} at death time; the name coloring rides sibling
 * components (GREEN killer / RED victim) instead of the 1.7.10 {@code §a}/{@code §c}
 * code splice at DamageSources.java:122.
 *
 * <p>Table comments carry the upstream anchor (file:line); the values are verbatim.
 * BendingCylinder and its Small twin share one line (both upstream :80). The electric
 * rows inherit through the upstream chains — Wrench_LV/MV/HV and MonkeyWrench_LV/MV/HV
 * (GT_Tool_MonkeyWrench_LV ← Wrench_LV ← Wrench) all carry the Wrench line; Trimmer_LV
 * inherits BranchCutter's; the pocket variants inherit their base tools'.
 */
public final class GT6ToolDeathMessages {

	private GT6ToolDeathMessages() {
	}

	/** Upstream ToolStats.java:104 — the self-mocking default (the sword + the pocket-multitool base carry it). */
	public static final String DEFAULT_MESSAGE = "Why is there no custom Death Message for this Tool?";

	/** Upstream GT_Tool_Club.java:135 — carries the :139-140 Bear989Sr killer exclusive. */
	public static final String CLUB_MESSAGE = "[VICTIM] got welcomed into the club by [KILLER]";

	/** The census table — registry path → the verbatim upstream line. */
	static final Map<String, String> MESSAGES = Map.ofEntries(
			// — the flat tools —
			Map.entry("crowbar", "[VICTIM] lost Half a Life to [KILLER]"), // GT_Tool_Crowbar.java:158
			Map.entry("cutter", "[KILLER] has cut the Cable for the Life Support Machine of [VICTIM]"), // GT_Tool_WireCutter.java:107
			Map.entry("chisel", "[VICTIM] got a Statue made by [KILLER]"), // GT_Tool_Chisel.java:103
			Map.entry("file", "[VICTIM] has been filed D for 'Dead' by [KILLER]"), // GT_Tool_File.java:107
			Map.entry("saw", "[KILLER] failed to perform the 'sawing a woman in half' trick on [VICTIM]"), // GT_Tool_Saw.java:204
			Map.entry("builder_wand", "[VICTIM] has been poofed out of existence by [KILLER]"), // GT_Tool_Builderwand.java:68
			Map.entry("screwdriver", "[VICTIM] has screwed with [KILLER] for the last time!"), // GT_Tool_Screwdriver.java:131
			Map.entry("hammer", "[VICTIM] was squashed by [KILLER]"), // GT_Tool_HardHammer.java:139
			Map.entry("wrench", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_Wrench.java:100
			Map.entry("bending_cylinder_small", "[VICTIM] got bent into shape by [KILLER]"), // GT_Tool_BendingCylinderSmall.java:80
			Map.entry("bending_cylinder", "[VICTIM] got bent into shape by [KILLER]"), // GT_Tool_BendingCylinder.java:80
			Map.entry("pickaxe", "[VICTIM] got mined by [KILLER]"), // GT_Tool_Pickaxe.java:83
			Map.entry("pickaxe_gem", "[VICTIM] got mined by [KILLER]"), // GT_Tool_PickaxeGem ← Pickaxe (inherited)
			Map.entry("pickaxe_construction", "[VICTIM] got demolished by [KILLER]"), // GT_Tool_PickaxeConstruction.java:69
			Map.entry("shovel", "[VICTIM] got dug up by [KILLER]"), // GT_Tool_Shovel.java:101
			Map.entry("spade", "[VICTIM] got aced by [KILLER]"), // GT_Tool_Spade.java:118
			Map.entry("universal_spade", "[VICTIM] has been digged by [KILLER]"), // GT_Tool_UniversalSpade.java:145
			Map.entry("sword", DEFAULT_MESSAGE), // GT_Tool_Sword ← ToolStats (the :104 default)
			Map.entry("knife", "<[VICTIM]> [KILLER] what are you doing?, [KILLER]?!? STAHP!!!"), // GT_Tool_Knife.java:79
			Map.entry("butchery_knife", "[KILLER] butchered [VICTIM]!"), // GT_Tool_ButcheryKnife.java:108
			Map.entry("club", CLUB_MESSAGE), // GT_Tool_Club.java:135
			Map.entry("axe", "[VICTIM] has been chopped by [KILLER]"), // GT_Tool_Axe.java:169
			Map.entry("axe_double", "[VICTIM] got beheaded by [KILLER]"), // GT_Tool_AxeDouble.java:55
			Map.entry("soft_hammer", "[VICTIM] got hammered to death by [KILLER]"), // GT_Tool_SoftHammer.java:135
			Map.entry("monkey_wrench", "[KILLER] threw a Monkey Wrench into the Plans of [VICTIM]"), // GT_Tool_MonkeyWrench.java:52
			Map.entry("magnifying_glass", "[VICTIM] got investigated very closely by [KILLER]"), // GT_Tool_MagnifyingGlass.java:98
			Map.entry("pincers", "[KILLER] pulled a tooth out of [VICTIM]"), // GT_Tool_Pincers.java:129
			Map.entry("hoe", "[VICTIM] has been called a stupid Hoe by [KILLER]"), // GT_Tool_Hoe.java:112
			Map.entry("plow", "[KILLER] plew through the yard of [VICTIM]"), // GT_Tool_Plow.java:94
			Map.entry("branch_cutter", "[VICTIM] has been trimmed by [KILLER]"), // GT_Tool_BranchCutter.java:137
			Map.entry("sense", "[KILLER] has reaped the Soul of [VICTIM]"), // GT_Tool_Sense.java:110
			Map.entry("hand_drill", "[VICTIM] has been tortured by [KILLER]"), // GT_Tool_HandDrill.java:79
			Map.entry("scissors", "[KILLER] ran into [VICTIM] while holding Scissors"), // GT_Tool_Scissors.java:128
			Map.entry("scoop", "[VICTIM] got scooped up by [KILLER]"), // GT_Tool_Scoop.java:112
			Map.entry("plunger", "[VICTIM] got stuck trying to escape through a Pipe while fighting [KILLER]"), // GT_Tool_Plunger.java:96
			Map.entry("flint_and_tinder", "[VICTIM] has been ignited by [KILLER]"), // GT_Tool_FlintAndTinder.java:73
			Map.entry("rolling_pin", "[VICTIM] got flattened by [KILLER]"), // GT_Tool_RollingPin.java:80
			// — the pocket multitool family (each row its upstream base's line; the base itself is default) —
			Map.entry("pocket_multitool", DEFAULT_MESSAGE), // GT_Tool_Pocket_Multitool ← ToolStats (the :104 default)
			Map.entry("pocket_multitool_knife", "[KILLER] whacked [VICTIM] to death with a closed Pocket Knife"), // GT_Tool_Pocket_Knife.java:60
			Map.entry("pocket_multitool_saw", "[KILLER] failed to perform the 'sawing a woman in half' trick on [VICTIM]"), // GT_Tool_Pocket_Saw ← Saw
			Map.entry("pocket_multitool_file", "[VICTIM] has been filed D for 'Dead' by [KILLER]"), // GT_Tool_Pocket_File ← File
			Map.entry("pocket_multitool_screwdriver", "[VICTIM] has screwed with [KILLER] for the last time!"), // GT_Tool_Pocket_Screwdriver ← Screwdriver
			Map.entry("pocket_multitool_wire_cutter", "[KILLER] has cut the Cable for the Life Support Machine of [VICTIM]"), // GT_Tool_Pocket_Cutter ← WireCutter
			Map.entry("pocket_multitool_scissors", "[KILLER] ran into [VICTIM] while holding Scissors"), // GT_Tool_Pocket_Scissors ← Scissors
			Map.entry("pocket_multitool_chisel", "[VICTIM] got a Statue made by [KILLER]"), // GT_Tool_Pocket_Chisel ← Chisel
			// — the guns (the melee-whip face; the shooting face stays deferred on the GTPistolItem card) —
			Map.entry("pistol", "[VICTIM] got pistol-whipped over the head by [KILLER]"), // GT_Tool_Pistol.java:54
			Map.entry("carbine", "[VICTIM] got whacked over the head by [KILLER]"), // GT_Tool_Carbine.java:38
			Map.entry("rifle", "[VICTIM] got melee'd by [KILLER]"), // GT_Tool_Rifle.java:38
			// — the electric nineteen (Loader_Tools.java:156-174; inheritance as commented) —
			Map.entry("mining_drill_lv", "[VICTIM] has met Dentist Dr. [KILLER]"), // GT_Tool_MiningDrill_LV.java:133
			Map.entry("mining_drill_mv", "[VICTIM] has met Dentist Dr. [KILLER]"), // GT_Tool_MiningDrill_MV ← LV
			Map.entry("mining_drill_hv", "[VICTIM] has met Dentist Dr. [KILLER]"), // GT_Tool_MiningDrill_HV ← LV
			Map.entry("chainsaw_lv", "[VICTIM] was massacred by [KILLER]"), // GT_Tool_Chainsaw_LV.java:146
			Map.entry("chainsaw_mv", "[VICTIM] was massacred by [KILLER]"), // GT_Tool_Chainsaw_MV ← LV
			Map.entry("chainsaw_hv", "[VICTIM] was massacred by [KILLER]"), // GT_Tool_Chainsaw_HV ← MV
			Map.entry("wrench_lv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_Wrench_LV ← Wrench
			Map.entry("wrench_mv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_Wrench_MV ← LV
			Map.entry("wrench_hv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_Wrench_HV ← LV
			Map.entry("jackhammer_hv_normal", "[VICTIM] has been jackhammered into pieces by [KILLER]"), // GT_Tool_JackHammer_HV_Normal ← JackHammer_HV.java:150
			Map.entry("jackhammer_hv_no_ores", "[VICTIM] has been jackhammered into pieces by [KILLER]"), // GT_Tool_JackHammer_HV_No_Ores ← Normal
			Map.entry("buzzsaw_lv", "[VICTIM] got buzzed by [KILLER]"), // GT_Tool_BuzzSaw_LV.java:90
			Map.entry("screwdriver_lv", "[VICTIM] has screwed with [KILLER] for the last time!"), // GT_Tool_Screwdriver_LV ← Screwdriver
			Map.entry("hand_drill_lv", "[VICTIM] needed help with a few holes and [KILLER] gladly helped"), // GT_Tool_Drill_LV.java:130 (the "Hand Drill (LV)" row)
			Map.entry("hand_mixer_lv", "[KILLER] mixed up [VICTIM] with the Ingredients"), // GT_Tool_Mixer_LV.java:102
			Map.entry("monkey_wrench_lv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_MonkeyWrench_LV ← Wrench_LV ← Wrench
			Map.entry("monkey_wrench_mv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_MonkeyWrench_MV ← Wrench_MV
			Map.entry("monkey_wrench_hv", "[KILLER] gave [VICTIM] a whack with the Wrench!"), // GT_Tool_MonkeyWrench_HV ← Wrench_HV
			Map.entry("trimmer_lv", "[VICTIM] has been trimmed by [KILLER]")); // GT_Tool_Trimmer_LV ← BranchCutter

	/** The lookup (the listener's only read face; a miss = the vanilla message stands). */
	public static String templateOf(String aRegistryPath) {
		return MESSAGES.get(aRegistryPath);
	}

	/**
	 * The upstream DamageSources.java:110-128 fold: the {@code [KILLER]} marker renders
	 * GREEN, the {@code [VICTIM]} marker RED (:122); the Crazy↔Bear pairings ride ahead
	 * of any template (:113-115); the club answers the Bear989Sr killer with its own
	 * line (GT_Tool_Club.java:139-140).
	 */
	public static Component compose(String aKillerName, String aVictimName, String aTemplate) {
		if ("CrazyJ84".equalsIgnoreCase(aKillerName) || "CrazyJ1984".equalsIgnoreCase(aKillerName)) {
			if ("Bear989jr".equalsIgnoreCase(aVictimName)) return mrsCrazy("> Sorry ", "Junior", "");
			if ("Bear989Sr".equalsIgnoreCase(aVictimName)) return mrsCrazy("> Hush it!, ", "Bear", "!");
		}
		if (CLUB_MESSAGE.equals(aTemplate) && "Bear989Sr".equalsIgnoreCase(aKillerName)) {
			return Component.literal("").append(Component.literal(aVictimName).withStyle(ChatFormatting.RED))
					.append(Component.literal(" got clubbed by a Bear!").withStyle(ChatFormatting.WHITE));
		}
		MutableComponent tOut = Component.empty();
		String tRest = aTemplate;
		while (true) {
			int tKiller = tRest.indexOf("[KILLER]");
			int tVictim = tRest.indexOf("[VICTIM]");
			if (tKiller < 0 && tVictim < 0) break;
			if (tKiller >= 0 && (tVictim < 0 || tKiller < tVictim)) {
				tOut.append(Component.literal(tRest.substring(0, tKiller)));
				tOut.append(Component.literal(aKillerName).withStyle(ChatFormatting.GREEN));
				tRest = tRest.substring(tKiller + "[KILLER]".length());
			} else {
				tOut.append(Component.literal(tRest.substring(0, tVictim)));
				tOut.append(Component.literal(aVictimName).withStyle(ChatFormatting.RED));
				tRest = tRest.substring(tVictim + "[VICTIM]".length());
			}
		}
		return tOut.append(Component.literal(tRest));
	}

	/**
	 * The Mrs. Crazy lines (DamageSources.java:114-115) — light-purple speaker, red name,
	 * the WHITE tail verbatim (the :115 "!" rides after the red Bear).
	 */
	private static Component mrsCrazy(String aMiddle, String aRedName, String aTail) {
		return Component.literal("<").append(Component.literal("Mrs. Crazy").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal(aMiddle))
				.append(Component.literal(aRedName).withStyle(ChatFormatting.RED))
				.append(Component.literal(aTail));
	}

	/** The census read face (the test package; not an API). */
	static Map<String, String> table() {
		return MESSAGES;
	}
}
