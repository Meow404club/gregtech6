package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.covers.CoverRegistry;
import gregtech6.covers.ICover;
import gregtech6.covers.TileEntityOvenCoverProbe;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;
import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * The crowbar classifier + dispatch tests (task p9-tool-crowbar acceptance, offline
 * half): the TOOL_CROWBAR-id path through the ICoverableTE :246-247 OR gate, the
 * classifier red line (CROWBAR action yes, HOE_DIG never), the durability mapping and
 * the legacy hoe-substitute branch regression. The live give/install/dismantle chain
 * rides the RCON acceptance (GTToolCommand); the hoe UI predicates are untouched by
 * construction (zero-diff on the three predicate files).
 */
public class CrowbarTest {

	static final BlockPos COVER_POS = new BlockPos(2, 2, 3);

	static BlockEntityType<TileEntityOvenCoverProbe> sProbeType;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		sProbeType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOvenCoverProbe(sProbeType, aPos, aState),
				Blocks.BRICKS).build(null);
		// NOTE: the GTCrowbarItem instance itself is NOT constructed here — a mod Item
		// cannot be built in this bootstrapped-and-frozen JVM (Item.java:61 intrusive
		// holder, the mod-Block offline wall); the classifier pins through the static
		// seam, the live item rides the RCON give/install/dismantle chain.
	}

	@BeforeEach
	void putCoverFixtures() {
		CoverRegistry.reset();
		CoverRegistry.put(Items.IRON_INGOT, new CoverTextureSimple(new ResourceLocation("gt6", "block/cover/test_plate")));
	}

	@AfterEach
	void clearCoverFixtures() {
		CoverRegistry.reset();
	}

	/** An offline oven probe on a stub server-side level (the GTCoverTestBase shape). */
	static TileEntityOvenCoverProbe leveledOven() {
		TileEntityOvenCoverProbe tOven = new TileEntityOvenCoverProbe(sProbeType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		tOven.setLevel(new GTMachinesOfflineTestBase.MachineLevel(new GTRecipesOfflineTestBase.TestRecipeManager()));
		return tOven;
	}

	@Test
	void classifierExposesCrowbarOnly() {
		assertTrue(GTCrowbarItem.classifies(GT6ToolActions.CROWBAR), "the crowbar action classifies");
		assertFalse(GTCrowbarItem.classifies(ToolActions.HOE_DIG), "RED LINE — never a hoe: the three wrench predicates must not fire");
		assertFalse(GTCrowbarItem.classifies(ToolActions.AXE_DIG), "no other action leaks");
		assertNotSame(GT6ToolActions.CROWBAR, ToolActions.HOE_DIG, "the action registry interns distinct actions");
		assertEquals("gt6_crowbar", GT6ToolActions.CROWBAR.name(), "the ADR-pinned action name");
		assertEquals(512, GTCrowbarItem.DURABILITY_POINTS, "the single steel tier");
		assertEquals(10000, GTCrowbarItem.TOOL_DAMAGE_PER_DISMANTLE, "the upstream :151 return");
	}

	@Test
	void crowbarIdPathDismantles() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.IRON_INGOT), null, false, true));

		// the reserved id with a non-hoe, non-empty classification carrier — since no
		// offline stack can carry the GT6 action (no mod Item offline), a vanilla stick
		// proves the id branch needs nothing from the stack classifier
		assertFalse(new ItemStack(Items.STICK).canPerformAction(GT6ToolActions.CROWBAR));
		long tDamage = tOven.onCoverToolClick(ICover.TOOL_CROWBAR, null, new ItemStack(Items.STICK), (byte) 4, false);
		assertEquals(10000, tDamage, ":151 — the upstream tool damage");
		assertEquals(1, tOven.mDropped.size(), ":149 — the cover drops (null player → the dropCoverStack seam)");
		assertEquals(Items.IRON_INGOT, tOven.mDropped.get(0).getItem());
		assertEquals((byte) 4, tOven.mDropSides.get(0));
		assertNull(tOven.getCovers(), ":313-317 — the store dissolves");
	}

	@Test
	void crowbarIdAloneSufficesWithoutStack() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 1, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertEquals(10000, tOven.onCoverToolClick(ICover.TOOL_CROWBAR, null, ItemStack.EMPTY, (byte) 1, false),
				"the id branch does not need the stack classifier (the :246 first half)");
		assertNull(tOven.getCovers());
	}

	@Test
	void crowbarStackWithoutIdClassifiesToNothing() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.IRON_INGOT), null, false, true));
		// a stack performing no HOE_DIG and carrying no id — the OR gate stays closed and
		// the click falls to the tool relay (no cover behaviour consumes it)
		assertEquals(0, tOven.onCoverToolClick("", null, new ItemStack(Items.STICK), (byte) 4, false),
				"classification comes from the id, never from a hoe alias");
		assertTrue(tOven.isCovered((byte) 4), "the cover survives");
	}

	@Test
	void legacyHoeBranchStillGreen() {
		TileEntityOvenCoverProbe tOven = leveledOven();
		assertTrue(tOven.setCoverItem((byte) 4, new ItemStack(Items.IRON_INGOT), null, false, true));
		assertEquals(10000, tOven.onCoverToolClick("", null, new ItemStack(Items.WOODEN_HOE), (byte) 4, false),
				"the :247 hoe-substitute half is intact (the TileEntityOvenCoverTest premise)");
		assertNull(tOven.getCovers());
	}

	@Test
	void durabilityMappingOnePointPerDismantle() {
		// the declared mapping: the :151 return 10000 (internal units, Behavior_Tool :63)
		// equals one vanilla durability point. The physical hurtAndBreak payment (needs a
		// server-side LivingEntity) is the RCON chain's crowbarDamage=1/512 assertion;
		// here the vanilla hurt() seam the payment rides is pinned on a vanilla item.
		assertEquals(10000, GTCrowbarItem.TOOL_DAMAGE_PER_DISMANTLE);
		assertEquals(512, GTCrowbarItem.DURABILITY_POINTS);
		ItemStack tStack = new ItemStack(Items.WOODEN_HOE);
		tStack.hurt(1, RandomSource.create(), null); // the return value is "broke", not "applied"
		assertEquals(1, tStack.getDamageValue(), "one payment = one damage value");
		assertFalse(tStack.isEmpty());
		ItemStack tDying = new ItemStack(Items.WOODEN_HOE);
		tDying.setDamageValue(tDying.getMaxDamage() - 1);
		assertTrue(tDying.hurt(1, RandomSource.create(), null), "the final unit reports the break");
	}
}
