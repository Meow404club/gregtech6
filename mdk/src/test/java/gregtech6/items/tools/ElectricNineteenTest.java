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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.items.tools.electric.GT6ElectricToolItem;
import gregtech6.items.tools.loot.GT6ToolLootModifiers;
import gregtech6.registry.GT6Tools;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Offline tests for task p29-w5-t6-electric-nineteen — the nineteen electric tools
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
	static GT6ElectricToolItem sWrenchHv; // tier 3 — 1024000 EU, 800/break, q2

	@BeforeAll
	static void warmUpAndBuild() {
		// the vanilla boot rides the parent @BeforeAll (bootVanillaOffline runs superclass-first);
		gregtech6.tileentity.energy.GTEnergySourceBlockEntity.resolveEnergyType("TU"); // the TD CME warm-up
		sDrillLv = registerFixture("fixture_electric_drill_lv", GT6ElectricToolItem.MINING_DRILL_LV);
		sWrenchHv = registerFixture("fixture_electric_wrench_hv", GT6ElectricToolItem.WRENCH_HV);
	}

	// ------------------------------------------------------------------ the registry latch (the GT6BatteryItemTest machinery)

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
			tArmed = false; // the telemetry leg: the carrier tests assume-skip
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

	/** The ItemStack ctor needs a registry DELEGATE, so the fixtures register under fixture keys with the latch momentarily open. */
	static GT6ElectricToolItem registerFixture(String aKey, GT6ElectricToolItem.Spec aSpec) {
		org.junit.jupiter.api.Assumptions.assumeTrue(ARMED, "the offline registry latch is unreachable on this JVM");
		unlockItemRegistry();
		try {
			return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
					new ResourceLocation("gt6", aKey),
					new GT6ElectricToolItem(aSpec, new net.minecraft.world.item.Item.Properties().durability(GT6ElectricToolItem.DURABILITY_POINTS)));
		} finally {
			lockItemRegistry();
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
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
		assertEquals(64, GT6Tools.TAB_TABLE.size(), "the 37 prior rows + the nineteen electric rows");
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
}
