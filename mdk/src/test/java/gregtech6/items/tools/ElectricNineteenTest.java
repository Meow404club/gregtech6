package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item; // the 21.1 leg: Item.TooltipContext.EMPTY in the callHoverText swap
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline tests for task w5-t6-electric-nineteen — the nineteen electric tools
 * (the DigSixTest offline boot form: pure static seams + the registerFixture item
 * seat for the IItemEnergy face, the GT6BatteryItemTest posture).
 *
 * <p>Surfaces pinned here (the card ACCEPTANCE rows):
 * <ul>
 * <li>the TAB_TABLE 35-row parity (16 prior + the nineteen electric rows, ids in the
 *     upstream :156-174 display order) + the 19-id registration census;</li>
 * <li>the capacity literals (64000/256000/1024000 — the lead-acid representative tiers,
 *     the declared 收敛 of the upstream capacity-sum face Loader_Tools.java:427-450) and
 *     the packet bands (V[tier] 32/128/512, the Base08 :62-66 derivation);</li>
 * <li>the twin-swap duality (the :162-166 对位表: wrench_lv↔monkey_wrench_lv through the
 *     hv tier, jackhammer_hv_normal↔jackhammer_hv_no_ores);</li>
 * <li>the wear semantics (the doDamage :433 denominator max(10, quality*20) → {10, 20,
 *     40}; the EU drain = the action's damage units; the EU-empty refusal);</li>
 * <li>the mining surfaces (the drill surface, the jackhammer ore exclusion on the
 *     no-ores form, the zero machine face of the machine-tool class);</li>
 * <li>the jackhammer rockGt conversion table (the pure-function seam + the offline
 *     identity guard — the LIVE drop face is the RCON chain's, the DigSixTest
 *     offline/live division);</li>
 * <li>the charge-to-full IItemEnergy loop (the offline half of the 电池盒充能 arm —
 *     the LIVE box push is the RCON chain). The charge-preserving twin swap rides the
 *     live registry face (switchForm resolves the twin through BuiltInRegistries) —
 *     its offline leg is the twin-table test above, the live leg the RCON switch arm.</li>
 * </ul>
 */
public class ElectricNineteenTest extends GTOfflineTestBase {

	static GT6ElectricToolItem sDrillLv; // tier 1 — 64000 EU, 25/break, q0
	static GT6ElectricToolItem sWrenchMv; // tier 2 — 256000 EU (the tooltip energy-row census's MV fixture)
	static GT6ElectricToolItem sWrenchHv; // tier 3 — 1024000 EU, 800/break, q2
	static GT6ElectricToolItem sJackHv; // tier 3 — the mode-switch tooltip ordering fixture

	@BeforeAll
	static void warmUpAndBuild() {
		// the vanilla boot rides the parent @BeforeAll (bootVanillaOffline runs superclass-first);
		gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType("TU"); // the TD CME warm-up
		sDrillLv = registerFixture("fixture_electric_drill_lv", GT6ElectricToolItem.MINING_DRILL_LV);
		sWrenchMv = registerFixture("fixture_electric_wrench_mv", GT6ElectricToolItem.WRENCH_MV);
		sWrenchHv = registerFixture("fixture_electric_wrench_hv", GT6ElectricToolItem.WRENCH_HV);
		sJackHv = registerFixture("fixture_electric_jackhammer_hv", GT6ElectricToolItem.JACKHAMMER_HV_NORMAL);
	}

	// ------------------------------------------------------------------ the registry latch

	/**
	 * The class-level Unsafe latch folded onto {@link GTOfflineTestBase} (the
	 * GT6BatteryItemTest machinery — task probeitem-latch-hygiene): the base's
	 * {@code registerItemFixture} carries the identical assume/unlock/register("gt6",
	 * key)/relock walk, so only the Spec-flavored seat stays here.
	 */
	static GT6ElectricToolItem registerFixture(String aKey, GT6ElectricToolItem.Spec aSpec) {
		return registerItemFixture(aKey, () ->
				new GT6ElectricToolItem(aSpec, new net.minecraft.world.item.Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
	}

	private static ResourceLocation rl(String aPath) {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}

	private static BlockState state(net.minecraft.world.level.block.Block aBlock) {
		return aBlock.defaultBlockState();
	}

	// ------------------------------------------------------------------ the 19-id census + TAB parity

	/** The 19 spec rows in the upstream registration order Loader_Tools.java:156-174. */
	@Test
	public void theSpecTableIsTheNineteenUpstreamRows() {
		assertEquals(19, GT6ElectricToolItem.SPECS.size());
		String[] tExpected = {"mining_drill_lv", "mining_drill_mv", "mining_drill_hv",
				"chainsaw_lv", "chainsaw_mv", "chainsaw_hv",
				"wrench_lv", "wrench_mv", "wrench_hv",
				"jackhammer_hv_normal", "jackhammer_hv_no_ores",
				"buzzsaw_lv", "screwdriver_lv", "hand_drill_lv", "hand_mixer_lv",
				"monkey_wrench_lv", "monkey_wrench_mv", "monkey_wrench_hv",
				"trimmer_lv"};
		for (int i = 0; i < tExpected.length; i++) {
			assertEquals(tExpected[i], GT6ElectricToolItem.SPECS.get(i).aPath(), "row " + i);
			assertSame(GT6Tools.electricTool(tExpected[i]), GT6Tools.TAB_TABLE.get(37 + i),
					"row " + (37 + i) + " of the tab is the registered " + tExpected[i]);
			assertEquals(rl(tExpected[i]), GT6Tools.electricTool(tExpected[i]).getId());
		}
		// 101 = the 91-row census + the ten Single Use tool rows (task
		// disposable-tools-tab-rehome, merge 86d4fad60, the user ruling 2026-10-06 — the
		// token tail append; the rows 37..55 absolute pins stay frozen)
		assertEquals(101, GT6Tools.TAB_TABLE.size(), "the 37 prior rows + the nineteen electric rows + the later bands + the ten rehomed Single Use tools (the 101-row census)");
	}

	/** The 19 snake tags (the p24 band shape — the constants pinned at their paths). */
	@Test
	public void theNineteenToolTagsAreTheSnakeCensus() {
		assertEquals(rl("tools/mining_drill_lv"), GT6ItemTags.TOOLS_MINING_DRILL_LV.location());
		assertEquals(rl("tools/monkey_wrench_hv"), GT6ItemTags.TOOLS_MONKEY_WRENCH_HV.location());
		assertEquals(rl("tools/jackhammer_hv_no_ores"), GT6ItemTags.TOOLS_JACKHAMMER_HV_NO_ORES.location());
		assertEquals(rl("tools/trimmer_lv"), GT6ItemTags.TOOLS_TRIMMER_LV.location());
	}

	// ------------------------------------------------------------------ the capacity literals + bands

	/** The lead-acid representative capacities per tier (the GT6BatteryLadderTest EXPECTED column). */
	@Test
	public void theCapacityLadderPinsTheLeadAcidLiterals() {
		assertEquals(64000L, GT6ElectricToolItem.CAPACITY_PER_TIER[1], "the LV lead-acid capacity");
		assertEquals(256000L, GT6ElectricToolItem.CAPACITY_PER_TIER[2], "the MV lead-acid capacity");
		assertEquals(1024000L, GT6ElectricToolItem.CAPACITY_PER_TIER[3], "the HV lead-acid capacity");
		for (GT6ElectricToolItem.Spec tSpec : GT6ElectricToolItem.SPECS) {
			assertEquals(GT6ElectricToolItem.CAPACITY_PER_TIER[tSpec.aTier()], tSpec.capacity(), tSpec.aPath() + " capacity");
			assertEquals(GT6ElectricToolItem.SIZE_PER_TIER[tSpec.aTier()], tSpec.sizeRec(), tSpec.aPath() + " packet size");
		}
		// the per-row band faces: LV [16..64], HV [512..1024] (the Base08 :62-66 derivation)
		assertEquals(16L, GT6ElectricToolItem.MINING_DRILL_LV.sizeMin());
		assertEquals(64L, GT6ElectricToolItem.MINING_DRILL_LV.sizeMax());
		assertEquals(512L, GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.sizeRec());
		assertEquals(256L, GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.sizeMin());
		assertEquals(1024L, GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.sizeMax());
	}

	// ------------------------------------------------------------------ the twin-swap duality (:162-166)

	/** The 对位表 — every twin edge is bidirectional, the upstream constructor cross-references verbatim. */
	@Test
	public void theTwinSwapEdgesAreMutual() {
		assertEquals("monkey_wrench_lv", GT6ElectricToolItem.WRENCH_LV.aTwinPath(), ":162 wrench_lv -> monkey_wrench_lv");
		assertEquals("monkey_wrench_mv", GT6ElectricToolItem.WRENCH_MV.aTwinPath(), ":163");
		assertEquals("monkey_wrench_hv", GT6ElectricToolItem.WRENCH_HV.aTwinPath(), ":164");
		assertEquals("wrench_lv", GT6ElectricToolItem.MONKEY_WRENCH_LV.aTwinPath(), ":171 monkey_wrench_lv -> wrench_lv");
		assertEquals("wrench_mv", GT6ElectricToolItem.MONKEY_WRENCH_MV.aTwinPath(), ":172");
		assertEquals("wrench_hv", GT6ElectricToolItem.MONKEY_WRENCH_HV.aTwinPath(), ":173");
		assertEquals("jackhammer_hv_no_ores", GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.aTwinPath(), ":165 normal -> no_ores");
		assertEquals("jackhammer_hv_normal", GT6ElectricToolItem.JACKHAMMER_HV_NO_ORES.aTwinPath(), ":166 no_ores -> normal");
		// the twins always sit on the SAME tier (the swap must not re-tier the tool)
		for (GT6ElectricToolItem.Spec tSpec : GT6ElectricToolItem.SPECS) {
			if (tSpec.aTwinPath() == null) continue;
			GT6ElectricToolItem.Spec tTwin = GT6ElectricToolItem.specOf(tSpec.aTwinPath());
			assertEquals(tSpec.aTier(), tTwin.aTier(), tSpec.aPath() + " twin tier parity");
			assertEquals(tSpec.capacity(), tTwin.capacity(), tSpec.aPath() + " twin capacity parity");
		}
		// the non-twin rows: the drill/chainsaw ladders, the buzzsaw/screwdriver/hand
		// drill/mixer/trimmer singles
		assertNull(GT6ElectricToolItem.MINING_DRILL_LV.aTwinPath());
		assertNull(GT6ElectricToolItem.CHAINSAW_HV.aTwinPath());
		assertNull(GT6ElectricToolItem.TRIMMER_LV.aTwinPath());
	}

	// ------------------------------------------------------------------ the wear semantics (:433-437)

	/** The wear denominators — max(10, quality*20): q0=10, q1=20, q2=40. */
	@Test
	public void theWearDenominatorsFoldTheQualityLadder() {
		assertEquals(10, GT6ElectricToolItem.MINING_DRILL_LV.wearDenominator(), "q0");
		assertEquals(20, GT6ElectricToolItem.CHAINSAW_LV.wearDenominator(), "q1");
		assertEquals(40, GT6ElectricToolItem.MINING_DRILL_HV.wearDenominator(), "q2");
		// a fixed-seed sample: 10000 LV (1-in-10) breaks → the hits stay in the ±30% band
		// (the RCON live sampling arm drives the SAME seam; the seeded form is deterministic)
		RandomSource tRng = RandomSource.create(1905L);
		int tHits = 0;
		for (int i = 0; i < 10000; i++) if (GT6ElectricToolItem.rollsWear(GT6ElectricToolItem.MINING_DRILL_LV, tRng)) tHits++;
		assertTrue(tHits > 700 && tHits < 1300, "10000 rolls at 1-in-10 hit " + tHits + " times");
	}

	/** The upstream getToolDamagePerBlockBreak columns — the EU cost of a break. */
	@Test
	public void theDrainLadderIsTheUpstreamDamageColumns() {
		assertEquals(25, GT6ElectricToolItem.MINING_DRILL_LV.aDamagePerBlock(), ":156 (GT_Tool_MiningDrill_LV :42)");
		assertEquals(100, GT6ElectricToolItem.MINING_DRILL_MV.aDamagePerBlock());
		assertEquals(400, GT6ElectricToolItem.MINING_DRILL_HV.aDamagePerBlock());
		assertEquals(200, GT6ElectricToolItem.JACKHAMMER_HV_NORMAL.aDamagePerBlock(), ":165 (GT_Tool_JackHammer_HV)");
		assertEquals(50, GT6ElectricToolItem.WRENCH_LV.aDamagePerBlock(), ":162 (GT_Tool_Wrench_LV)");
		assertEquals(800, GT6ElectricToolItem.WRENCH_HV.aDamagePerBlock());
	}

	// ------------------------------------------------------------------ the mining surfaces

	/**
	 * The surface families, the OFFLINE half (the DigSixTest division): the vanilla
	 * mineable tags do NOT bind in the offline JVM, so the tag arms ride the RCON speed
	 * leg; this test pins the explicit SET arms + the ORE_FAMILY exclusion table.
	 */
	@Test
	public void theMiningSurfacesMapTheUpstreamIsMinableBlock() {
		// the explicit set arms (offline-bindable)
		assertTrue(GT6ElectricToolItem.Surface.DRILL.mines(state(Blocks.GLASS)), "the Material.glass arm");
		assertTrue(GT6ElectricToolItem.Surface.DRILL.mines(state(Blocks.ICE)), "the ice arm");
		assertTrue(GT6ElectricToolItem.Surface.CHAINSAW.mines(state(Blocks.PACKED_ICE)));
		assertTrue(GT6ElectricToolItem.Surface.JACKHAMMER.mines(state(Blocks.TINTED_GLASS)));
		assertTrue(GT6ElectricToolItem.Surface.BUZZSAW.mines(state(Blocks.IRON_BARS)), "the bars face");
		assertFalse(GT6ElectricToolItem.Surface.BUZZSAW.mines(state(Blocks.GLASS)), "not suitable for harvesting blocks");
		assertTrue(GT6ElectricToolItem.Surface.TRIMMER.mines(state(Blocks.VINE)), "the vine arm");
		assertFalse(GT6ElectricToolItem.Surface.NONE.mines(state(Blocks.GLASS)), "the machine-tool classes: no port block universe");
		// the ORE_FAMILY exclusion table (the no-ores form's IPrefixBlock gate successor)
		assertTrue(GT6ElectricToolItem.ORE_FAMILY.contains(Blocks.IRON_ORE));
		assertTrue(GT6ElectricToolItem.ORE_FAMILY.contains(Blocks.DEEPSLATE_DIAMOND_ORE));
		assertTrue(GT6ElectricToolItem.ORE_FAMILY.contains(Blocks.ANCIENT_DEBRIS));
		assertFalse(GT6ElectricToolItem.ORE_FAMILY.contains(Blocks.COBBLESTONE), "the rocks stay breakable on the no-ores form");
		assertFalse(GT6ElectricToolItem.ORE_FAMILY.contains(Blocks.STONE));
	}

	// ------------------------------------------------------------------ the jackhammer rockGt conversion

	/** The pure-function table (the offline leg; the LIVE drop face is the RCON chain). */
	@Test
	public void theRockFamilyTableIsTheUpstreamColumns() {
		assertEquals(4, GT6ToolLootModifiers.ROCK_COUNT, "the rockGt x4 column");
		assertEquals("stone", GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.COBBLESTONE), "the RM.pack :152 column");
		assertEquals("stone", GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.STONE));
		assertEquals("netherrack", GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.NETHERRACK), ":153");
		assertEquals("endstone", GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.END_STONE), ":154");
		assertEquals("granite", GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.GRANITE), "the BlockStones family fold");
		assertNull(GT6ToolLootModifiers.ROCK_MATERIALS.get(Blocks.DIRT), "non-rock blocks ride through");
		// offline the gt6 items are unregistered → the mode degrades to the identity (the
		// null-safe guard); the live registry resolves gt6:rock_gt_stone (the RCON arm)
		assertFalse(GT6ToolLootModifiers.convert(GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.JACKHAMMER_ROCKS,
				state(Blocks.DIRT), new ArrayList<ItemStack>()), "dirt is no rock — no fire");
	}

	// ------------------------------------------------------------------ the IItemEnergy face (the offline charge arm)

	/** The EU pool face: fresh-empty, the clamp, the drain, the EU-empty refusal, the charge-to-full loop. */
	@Test
	public void theEnergyPoolCarriesTheFullLifecycle() {
		ItemStack tStack = new ItemStack(sDrillLv);
		assertEquals(0L, sDrillLv.getEnergyStored(TD.Energy.EU, tStack), "a fresh tool is empty");
		assertFalse(sDrillLv.usable(tStack), "empty = the isItemStackUsable refusal");
		// the clamp: the write face folds into [0, capacity]
		sDrillLv.setEnergyStored(TD.Energy.EU, tStack, 999999L);
		assertEquals(64000L, sDrillLv.getEnergyStored(TD.Energy.EU, tStack));
		assertTrue(sDrillLv.usable(tStack));
		// the drain arm: a stone break costs the upstream 25 units
		assertTrue(sDrillLv.useEnergy(TD.Energy.EU, tStack, 25L, true));
		assertEquals(63975L, sDrillLv.getEnergyStored(TD.Energy.EU, tStack));
		// the overdraw zeroes the pool and reports false (the :204 arm)
		assertFalse(sDrillLv.useEnergy(TD.Energy.EU, tStack, 999999L, true));
		assertEquals(0L, sDrillLv.getEnergyStored(TD.Energy.EU, tStack));
		// the LU domain is structurally foreign
		assertEquals(0L, sDrillLv.getEnergyStored(TD.Energy.LU, tStack), "EU-only domain");
		// the charge-to-full loop: packets of 32 land EXACTLY the capacity (the offline
		// half of the 电池盒充能 acceptance — the LIVE box push is the RCON chain)
		long tPackets = 0;
		while (sDrillLv.doEnergyInjection(TD.Energy.EU, tStack, 32L, 8L, true) > 0 && tPackets < 100000) tPackets += 8;
		assertEquals(64000L, sDrillLv.getEnergyStored(TD.Energy.EU, tStack), "the loop fills to EXACTLY full");
		assertTrue(tPackets >= 2000, "2000 packets x 32 EU = the capacity, got " + tPackets);
		// the band gate: an 8-EU packet is under the LV floor (16) — refused
		assertEquals(0L, sDrillLv.doEnergyInjection(TD.Energy.EU, tStack, 8L, 4L, true));
		// the HV wrench: the same carrier, the 1024000 clamp
		ItemStack tHv = new ItemStack(sWrenchHv);
		sWrenchHv.setEnergyStored(TD.Energy.EU, tHv, Long.MAX_VALUE);
		assertEquals(1024000L, sWrenchHv.getEnergyStored(TD.Energy.EU, tHv));
	}

	/** The durability shell stays at the family value 512 on every row (the card open-question ruling). */
	@Test
	public void theShellIsTheFamilyValue() {
		assertEquals(512, GT6ElectricToolItem.DURABILITY_POINTS);
	}

	/** The spec lookup seam (the command + the twin resolver). */
	@Test
	public void theSpecLookupResolvesByPath() {
		assertSame(GT6ElectricToolItem.JACKHAMMER_HV_NO_ORES, GT6ElectricToolItem.specOf("jackhammer_hv_no_ores"));
		assertNull(GT6ElectricToolItem.specOf("nonexistent_tool"));
		assertNotNull(GT6Tools.electricTool("mining_drill_lv"));
		assertNull(GT6Tools.electricTool("not_a_tool"));
	}

	// ------------------------------------------------------------------ the tooltip energy-stock row (task tooltip-electric-energy)

	private static void callHoverText(GT6ElectricToolItem aItem, ItemStack aStack, List<Component> aTooltip) {
		//? if forge {
		aItem.appendHoverText(aStack, null, aTooltip, TooltipFlag.NORMAL);
		//?} else {
		/*aItem.appendHoverText(aStack, Item.TooltipContext.EMPTY, aTooltip, TooltipFlag.NORMAL);
		*///?}
	}

	/** The energy-stock row of the hover (the one line containing " EU - Size: " — MultiItem.addInformation :259). */
	private static Component energyRow(GT6ElectricToolItem aItem, ItemStack aStack) {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(aItem, aStack, tTooltip);
		Component rRow = null;
		for (Component tLine : tTooltip) if (tLine.getString().contains(" EU - Size: ")) {
			assertNull(rRow, "exactly one energy row per hover");
			rRow = tLine;
		}
		return rRow;
	}

	private static Integer colorOf(Component aLine) {
		return aLine.getStyle().getColor() == null ? null : aLine.getStyle().getColor().getValue();
	}

	/**
	 * The MultiItem.addInformation :259 row VERBATIM: WHITE makeString(min(cap,stored)) " / "
	 * makeString(cap) " " <EU chat short> WHITE " - Size: " V[tier]. The makeString face rides
	 * this test through the real numbers: &lt;10000 plain, ≥10000 underscore thousands ("64_000",
	 * "1_024_000"). The EU sub-run carries the TagData EU chat color (TD.java:81 LH.Chat.BLUE —
	 * the energyUnit transcription); the rest of the row is WHITE (LH.Chat.WHITE :702).
	 */
	@Test
	public void theEnergyRowPinsTheUpstreamLineShape() {
		// fresh LV drill (tooltipKey null — the energy row IS the first row)
		Component tRow = energyRow(sDrillLv, new ItemStack(sDrillLv));
		assertNotNull(tRow, "the fresh LV drill carries the energy row");
		assertEquals("0 / 64_000 EU - Size: 32", tRow.getString(), "the :259 row verbatim");
		assertEquals(ChatFormatting.WHITE.getColor(), colorOf(tRow), "the row base is LH.Chat.WHITE");
		assertEquals(2, tRow.getSiblings().size(), "the EU short + the Size tail are the styled sub-runs");
		Component tUnit = tRow.getSiblings().get(0);
		assertEquals("EU", tUnit.getString(), "the TagData EU short name");
		assertEquals(ChatFormatting.BLUE.getColor(), colorOf(tUnit), "the EU chat color (TD.java:81)");
		Component tTail = tRow.getSiblings().get(1);
		assertEquals(" - Size: 32", tTail.getString(), "the upstream hard-coded-en tail");
		assertEquals(ChatFormatting.WHITE.getColor(), colorOf(tTail), "the tail resumes LH.Chat.WHITE");
		// the stored half: a part-charged pool renders the live value (12_345 ≥ 10000 → the underscore face)
		ItemStack tCharged = new ItemStack(sDrillLv);
		sDrillLv.setEnergyStored(TD.Energy.EU, tCharged, 12345L);
		assertEquals("12_345 / 64_000 EU - Size: 32", energyRow(sDrillLv, tCharged).getString());
		// the makeString boundary: 9999 stays plain, 10000 gains the underscore
		ItemStack tEdge = new ItemStack(sDrillLv);
		sDrillLv.setEnergyStored(TD.Energy.EU, tEdge, 9999L);
		assertEquals("9999 / 64_000 EU - Size: 32", energyRow(sDrillLv, tEdge).getString());
		sDrillLv.setEnergyStored(TD.Energy.EU, tEdge, 10000L);
		assertEquals("10_000 / 64_000 EU - Size: 32", energyRow(sDrillLv, tEdge).getString());
		// the HV wrench (tooltipKey non-null — the energy row rides AFTER the behavior row) at full clamp
		ItemStack tHv = new ItemStack(sWrenchHv);
		sWrenchHv.setEnergyStored(TD.Energy.EU, tHv, Long.MAX_VALUE);
		assertEquals("1_024_000 / 1_024_000 EU - Size: 512", energyRow(sWrenchHv, tHv).getString());
	}

	/** The row order: behavior tooltip row first (the upstream .tooltip row :249), THEN the energy row (:252), THEN the mode-switch row (the behavior block :273). */
	@Test
	public void theEnergyRowSitsBetweenBehaviorAndModeSwitchRows() {
		List<Component> tTooltip = new ArrayList<>();
		callHoverText(sWrenchHv, new ItemStack(sWrenchHv), tTooltip);
		assertTrue(tTooltip.size() >= 2, "the wrench hover carries behavior + energy rows");
		assertEquals("item.gt6.wrench_hv.tooltip", ((net.minecraft.network.chat.contents.TranslatableContents)tTooltip.get(0).getContents()).getKey(),
				"row 0 = the .tooltip behavior row (the :249 face)");
		assertTrue(tTooltip.get(1).getString().endsWith(" EU - Size: 512"), "row 1 = the energy row");
		// the jackhammer mode-switch row stays last (the behavior block rides AFTER the energy face)
		List<Component> tJack = new ArrayList<>();
		callHoverText(sJackHv, new ItemStack(sJackHv), tJack);
		assertTrue(tJack.size() >= 3, "the jackhammer hover carries behavior + energy + mode-switch rows");
		assertTrue(tJack.get(1).getString().endsWith(" EU - Size: 512"), "the energy row precedes the mode-switch row");
		assertEquals("item.gt6.mode_switch.tooltip", ((net.minecraft.network.chat.contents.TranslatableContents)tJack.get(2).getContents()).getKey(),
				"the mode-switch row stays last (the behavior block)");
	}

	/**
	 * The census: EVERY spec row's tier folds to the pinned tier literal, and each tier's
	 * literal is proven live through a real fixture hover — no electric id can miss the line
	 * (the negative-result evidence face: the family is one class over the spec table, so a
	 * per-tier live pin + the 19-row tier census covers every id).
	 */
	@Test
	public void everyElectricIdCarriesTheEnergyRow() {
		String[] tExpectedByTier = {"0 / 64_000 EU - Size: 32", "0 / 256_000 EU - Size: 128", "0 / 1_024_000 EU - Size: 512"};
		GT6ElectricToolItem[] tFixtureByTier = {sDrillLv, sWrenchMv, sWrenchHv};
		for (GT6ElectricToolItem.Spec tSpec : GT6ElectricToolItem.SPECS) {
			assertTrue(tSpec.aTier() >= 1 && tSpec.aTier() <= 3, tSpec.aPath() + " sits on a real tier");
			assertEquals(tExpectedByTier[tSpec.aTier() - 1], energyRow(tFixtureByTier[tSpec.aTier() - 1],
					new ItemStack(tFixtureByTier[tSpec.aTier() - 1])).getString(), tSpec.aPath() + " tier row");
		}
		assertEquals(19, GT6ElectricToolItem.SPECS.size(), "the 19-id census — none missed");
	}
}
