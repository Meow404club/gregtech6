package gregtech6.tileentity.bees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.items.bees.GT6Bumbles;

/**
 * The Bumbliary acceptance pins (task p33-bees-lv3-b-bumbliary): the breeding state
 * machine (the no-drone 600 window / the 1200 pairing window / the offspring-count
 * formula / the same-species mutation copy / the cross-species four-way split), the
 * bumbleCombine pairing-table reconciliation (the :378-437 nested switch as data), the
 * TE NBT round trip (the :66-85 key faces) and the registration containment (the
 * FML-booted leg asserts for real, the offline bare-JVM leg skips via the containsKey
 * latch — the GT6BumblesTest posture).
 */
class GT6BumbliaryBlockEntityTest {

	static BlockEntityType<GT6BumbliaryBlockEntity> sType;

	static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void boot() {
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BumbliaryBlockEntity>[] tHolder = (BlockEntityType<GT6BumbliaryBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbliaryBlockEntity(false, tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sType = tHolder[0];
	}

	static GT6BumbliaryBlockEntity te() {
		return new GT6BumbliaryBlockEntity(false, sType, POS, Blocks.BRICKS.defaultBlockState());
	}

	/** An rng stub handing the scripted nextInt(5)/nextInt(4) values while any other draw is maxed. */
	static Random scripted(int... aValues) {
		return new Random(0) {
			int mIndex = 0;

			@Override
			public int nextInt(int aBound) {
				if (aBound == 10000) return Integer.MAX_VALUE; // never mutates
				if (aBound == 5 || aBound == 4) {
					return aValues[Math.min(mIndex++, aValues.length - 1)];
				}
				return aBound - 1;
			}
		};
	}

	// ------------------------------------------------ the state machine constants

	@Test
	void windowsCarryTheUpstreamValues() {
		// the :199 no-drone retry window and the :119/:215/:270/:276 pairing window
		assertEquals(600, GT6BumbliaryBlockEntity.NO_DRONE_WINDOW);
		assertEquals(1200, GT6BumbliaryBlockEntity.PAIRED_WINDOW);
		assertEquals(1200, GT6BumbliaryBlockEntity.ENVIRONMENT_PERIOD, "the :108 environment cadence");
		// the :119/:270/:276 resets: the primary stomps, the Advanced only raises
		assertEquals(1200, GT6BumbliaryBlockEntity.raisedWindow(false, 5), "primary stomps unconditionally");
		assertEquals(1200, GT6BumbliaryBlockEntity.raisedWindow(true, 5), "advanced raises a short window");
		assertEquals(1200, GT6BumbliaryBlockEntity.raisedWindow(true, 1200));
		assertEquals(3000, GT6BumbliaryBlockEntity.raisedWindow(true, 3000), "advanced never interrupts a running pairing");
	}

	@Test
	void princessShareMapsTheRngFive() {
		// the :218 formula: 1 + rng(5)/2 — 1,1,2,2,3 over rng 0..4
		assertEquals(1, GT6BumbliaryBlockEntity.princessCount(scripted(0)));
		assertEquals(1, GT6BumbliaryBlockEntity.princessCount(scripted(1)));
		assertEquals(2, GT6BumbliaryBlockEntity.princessCount(scripted(2)));
		assertEquals(2, GT6BumbliaryBlockEntity.princessCount(scripted(3)));
		assertEquals(3, GT6BumbliaryBlockEntity.princessCount(scripted(4)));
	}

	@Test
	void offspringLengthIsGenePlusPrincessShare() {
		// the :220 formula: getOffspring(genes) + princessCount
		assertEquals(2 + 3, GT6BumbliaryBlockEntity.offspringCodes(scripted(4), 0, 0, 2, 3).length);
		assertEquals(1 + 1, GT6BumbliaryBlockEntity.offspringCodes(scripted(0), 0, 0, 1, 1).length);
	}

	@Test
	void sameSpeciesCopiesWithTheMutationRoll() {
		// :222-231 — the royal copy; a passed roll rides the tier-0 up-only +10 step
		int[] tNoMutate = GT6BumbliaryBlockEntity.offspringCodes(scripted(0), 0, 0, 3, 1);
		for (int tCode : tNoMutate) assertEquals(0, tCode, "a maxed roll never mutates");
		Random tAlways = new Random(0) {
			@Override
			public int nextInt(int aBound) {
				return 0; // always below the mutate chance
			}
		};
		for (int tCode : GT6BumbliaryBlockEntity.offspringCodes(tAlways, 0, 0, 3, 1)) {
			assertEquals(10, tCode, "tier 0 mutates up-only");
		}
	}

	@Test
	void crossSpeciesSplitsTheFourWays() {
		// :233-249 — father, mother, combine(A,B), combine(B,A); an off-table pair
		// (0,100) collapses the combine arms onto the A/B copies (the :436 default)
		Set<Integer> tSeen = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			int[] tCodes = GT6BumbliaryBlockEntity.offspringCodes(new Random(i), 0, 100, 5, 1);
			assertEquals(6, tCodes.length);
			for (int tCode : tCodes) {
				assertTrue(tCode == 0 || tCode == 100, "the off-table four-way yields only the parents: " + tCode);
				tSeen.add(tCode);
			}
		}
		assertEquals(Set.of(0, 100), tSeen, "both parents occur");
		// the on-table tier-3 pair (330,430) adds the hybrid code 10300
		Set<Integer> tHybrids = new HashSet<>();
		for (int i = 0; i < 500; i++) {
			for (int tCode : GT6BumbliaryBlockEntity.offspringCodes(new Random(1000 + i), 330, 430, 5, 1)) {
				assertTrue(tCode == 330 || tCode == 430 || tCode == 10300, "the four-way yields parents or the hybrid: " + tCode);
				tHybrids.add(tCode);
			}
		}
		assertEquals(Set.of(330, 430, 10300), tHybrids, "all four arms occur");
	}

	// ------------------------------------------------ the pairing table

	@Test
	void combineTableReconcilesTheUpstreamSwitch() {
		// the :378-435 pairs verbatim (codeA/10 → codeB/10 → result), both directions
		int[][] tRows = {
				{30, 130, 10100}, {30, 530, 10000}, {30, 930, 10200}, {130, 530, 10400},
				{330, 430, 10300}, {330, 10530, 20000}, {430, 10530, 20200}, {530, 10530, 20300},
				{630, 930, 10500}, {730, 10530, 20100}};
		assertEquals(tRows.length, GT6BumbliaryBlockEntity.COMBINE_ROWS.length, "the 10 upstream pairs");
		for (int[] tRow : tRows) {
			assertEquals(tRow[2], GT6BumbliaryBlockEntity.combineCode(tRow[0], tRow[1]), "forward " + tRow[0] + "x" + tRow[1]);
			assertEquals(tRow[2], GT6BumbliaryBlockEntity.combineCode(tRow[1], tRow[0]), "reverse " + tRow[1] + "x" + tRow[0]);
		}
		// the :436 default — the A copy
		assertEquals(700, GT6BumbliaryBlockEntity.combineCode(700, 1010));
		assertEquals(310, GT6BumbliaryBlockEntity.combineCode(310, 320));
		// every result lands inside the species table and every participant is a tier-3
		for (int[] tRow : GT6BumbliaryBlockEntity.COMBINE_ROWS) {
			assertNotNull(GT6Bumbles.speciesOf(tRow[2]), "the result is a real species: " + tRow[2]);
			assertEquals(3, GT6Bumbles.tierOf(tRow[0]), "the pairing needs tier-3 parents: " + tRow[0]);
			assertEquals(3, GT6Bumbles.tierOf(tRow[1]), "the pairing needs tier-3 parents: " + tRow[1]);
		}
	}

	// ------------------------------------------------ the TE layout + NBT

	@Test
	void layoutsMatchTheUpstreamSlotMaps() {
		// the :333-337 primary layout and the Advanced :333-338 layout
		assertEquals(13, GT6BumbliaryBlockEntity.SLOT_ROYAL);
		assertEquals(22, GT6BumbliaryBlockEntity.SLOT_DRONE);
		assertEquals(36, te().slotCount());
		assertEquals("gt.multitileentity.bumbliary", te().getTileEntityName(), ":387 verbatim");
		GT6BumbliaryBlockEntity tAdvanced = new GT6BumbliaryBlockEntity(true, sType, POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(20, tAdvanced.slotCount(), "the advanced inventory is 20 slots");
		assertEquals(7, tAdvanced.slotRoyal(), "the advanced ROYAL slot");
		assertEquals(12, tAdvanced.slotDrone(), "the advanced main DRONE slot");
		assertEquals("gt.multitileentity.bumbliary.advanced", tAdvanced.getTileEntityName(), "the Advanced :388 name");
	}

	@Test
	void stateRidesTheUpstreamNbtKeys() {
		// the :66-85 read/write pair over gt.progress / gt.cooldown / gt.invout
		GT6BumbliaryBlockEntity tTe = te();
		tTe.mLife = 777;
		tTe.mBreedingCountDown = 555;
		tTe.mOffSpring = new ItemStack[] {new ItemStack(Items.HONEYCOMB, 2), ItemStack.EMPTY, new ItemStack(Items.HONEYCOMB, 1)};

		CompoundTag tSaved = tTe.saveWithoutMetadata();
		assertTrue(tSaved.contains(GT6BumbliaryBlockEntity.NBT_PROGRESS, Tag.TAG_ANY_NUMERIC));
		assertTrue(tSaved.contains(GT6BumbliaryBlockEntity.NBT_COOLDOWN, Tag.TAG_ANY_NUMERIC));
		assertTrue(tSaved.contains(GT6BumbliaryBlockEntity.NBT_INV_OUT, Tag.TAG_ANY_NUMERIC),
				"the upstream count key (the indexed children ride NBT_INV_OUT.\".<i>\")");
		assertEquals(777, tSaved.getLong(GT6BumbliaryBlockEntity.NBT_PROGRESS));
		assertEquals(555, tSaved.getLong(GT6BumbliaryBlockEntity.NBT_COOLDOWN));
		assertEquals(3, tSaved.getInt(GT6BumbliaryBlockEntity.NBT_INV_OUT), "the upstream count + indexed children shape");

		GT6BumbliaryBlockEntity tReloaded = te();
		tReloaded.load(tSaved);
		assertEquals(777, tReloaded.mLife);
		assertEquals(555, tReloaded.mBreedingCountDown);
		assertEquals(3, tReloaded.mOffSpring.length);
		assertEquals(2, tReloaded.mOffSpring[0].getCount(), "the indexed child rehydrates");
		assertTrue(tReloaded.mOffSpring[1].isEmpty(), "the empty child stays empty");
		assertEquals(1, tReloaded.mOffSpring[2].getCount());
	}

	@Test
	void containerFaceRejectsForeignItems() {
		// the :357-364 insert rule — the reject half is testable offline (the accept half
		// needs the live bee items, pinned on the FML leg)
		GT6BumbliaryBlockEntity tTe = te();
		tTe.setItem(0, new ItemStack(Items.DIRT, 8));
		assertTrue(tTe.getItem(0).isEmpty(), "a foreign item never lands in a comb slot");
		assertNull(GT6Bumbles.speciesOf(9990), "the containment guard stays honest");
	}

	// ------------------------------------- the penalty + the aggro walk (task p34-bumbliary-gui)

	@Test
	void thePenaltyStompsTheWindow() {
		// the :289/:307 stomp — 6000 over any prior window (the acceptance ② arm)
		GT6BumbliaryBlockEntity tTe = te();
		tTe.mBreedingCountDown = GT6BumbliaryBlockEntity.PAIRED_WINDOW;
		tTe.penalize(false);
		assertEquals(6000, tTe.mBreedingCountDown, "the survival stomp overrides the 1200 pairing window");
		assertEquals(6000, GT6BumbliaryBlockEntity.PENALTY_WINDOW, "the penalty window constant");
	}

	@Test
	void thePenaltyIsCreativeExempt() {
		// the :307 gate — the creative poke is free (the acceptance ② exemption arm)
		GT6BumbliaryBlockEntity tTe = te();
		tTe.mBreedingCountDown = 123;
		tTe.penalize(true);
		assertEquals(123, tTe.mBreedingCountDown, "creative leaves the window untouched");
		tTe.penalize(false);
		assertEquals(6000, tTe.mBreedingCountDown, "survival pays");
	}

	@Test
	void theStingDelegateNeverFiresOnANullTarget() {
		// the :371-373 gate — the ROYAL slot face decides; the live sting (a crowned queen
		// hurting a target through bumbleAttack) is the RCON chain's arm (a constructible
		// LivingEntity does not exist on the bare-JVM leg)
		GT6BumbliaryBlockEntity tTe = te();
		assertFalse(tTe.sting(null), "no target, no sting");
	}

	@Test
	void theAggroWindowAndProbabilityTruthTable() {
		// the :165 verdict — the window (mLife%300==150) and the probability (rng<aggro)
		assertTrue(GT6BumbliaryBlockEntity.aggroTicks(150, 0, 1), "the midpoint fires");
		assertFalse(GT6BumbliaryBlockEntity.aggroTicks(149, 0, 10000), "the tick before the midpoint");
		assertFalse(GT6BumbliaryBlockEntity.aggroTicks(151, 0, 10000), "the tick after the midpoint");
		assertTrue(GT6BumbliaryBlockEntity.aggroTicks(450, 0, 10000), "the next window (450%300==150)");
		assertFalse(GT6BumbliaryBlockEntity.aggroTicks(600, 0, 10000), "the :168 produce tick is not an aggro tick (600%300==0)");
		assertTrue(GT6BumbliaryBlockEntity.aggroTicks(150, 42, 43), "the strict edge (roll 42 < aggro 43)");
		assertFalse(GT6BumbliaryBlockEntity.aggroTicks(150, 43, 43), "a roll equal to the aggro never fires");
		assertFalse(GT6BumbliaryBlockEntity.aggroTicks(150, 100, 0), "a pacifist queen never aggros");
		assertTrue(GT6BumbliaryBlockEntity.aggroTicks(12450, 500, 10000), "12450%300==150 — the gene bound rides the window");
	}

	@Test
	void theAggroBoxIsTheMinusFourPlusFiveCube() {
		// the :166 box(-4, -4, -4, +5, +5, +5) around the TE
		net.minecraft.world.phys.AABB tBox = GT6BumbliaryBlockEntity.aggroBox(new BlockPos(100, 64, 100));
		assertEquals(96.0, tBox.minX, 0.0, "min x = -4");
		assertEquals(60.0, tBox.minY, 0.0, "min y = -4");
		assertEquals(96.0, tBox.minZ, 0.0, "min z = -4");
		assertEquals(105.0, tBox.maxX, 0.0, "max x = +5");
		assertEquals(69.0, tBox.maxY, 0.0, "max y = +5");
		assertEquals(105.0, tBox.maxZ, 0.0, "max z = +5");
		// the selection face — entities whose box intersects the cube
		assertTrue(tBox.intersects(99.0, 63.0, 99.0, 99.5, 63.5, 99.5), "a neighbour cell intersects");
		assertFalse(tBox.intersects(106.0, 64.0, 100.0, 107.0, 65.0, 101.0), "x+6 is outside");
	}

	// ------------------------------------- the GUI seat tables (the widget pins ride GT6BumbliaryMUIPanelTest)

	@Test
	void theGuiSeatGeometryFollowsTheUpstreamGrids() {
		// the primary 4x9 steps from x=8, the advanced 4x5 from x=44, both y=8+18*row
		GT6BumbliaryBlockEntity.GuiSeat tRoyal = GT6BumbliaryBlockEntity.guiSeat(false, false, 13);
		assertEquals(80, tRoyal.x(), "the primary ROYAL x (8 + 4*18)");
		assertEquals(26, tRoyal.y(), "the primary ROYAL y (row 1)");
		GT6BumbliaryBlockEntity.GuiSeat tDead = GT6BumbliaryBlockEntity.guiSeat(false, false, 35);
		assertEquals(152, tDead.x(), "the primary last seat x");
		assertEquals(62, tDead.y(), "the primary last seat y (row 3)");
		GT6BumbliaryBlockEntity.GuiSeat tAdvRoyal = GT6BumbliaryBlockEntity.guiSeat(true, false, 7);
		assertEquals(80, tAdvRoyal.x(), "the advanced ROYAL x (44 + 2*18)");
		assertEquals(26, tAdvRoyal.y(), "the advanced ROYAL y (row 1)");
		GT6BumbliaryBlockEntity.GuiSeat tAdvDead = GT6BumbliaryBlockEntity.guiSeat(true, false, 19);
		assertEquals(116, tAdvDead.x(), "the advanced last seat x");
		assertEquals(62, tAdvDead.y(), "the advanced last seat y (row 3)");
	}

	// ------------------------------------------------ the registration containment

	@Test
	void registrationCarriesBothVariants() {
		// acceptance ② — on an FML-booted JVM the real registries carry both variants
		ResourceLocation tAdvanced = new ResourceLocation("gt6", "bumbliary_advanced");
		if (!BuiltInRegistries.BLOCK.containsKey(tAdvanced)) {
			return; // the offline bare-JVM leg; the FML-booted leg asserts for real
		}
		ResourceLocation tPrimary = new ResourceLocation("gt6", "bumbliary");
		assertTrue(BuiltInRegistries.BLOCK.containsKey(tPrimary), "the block pair registers");
		assertTrue(BuiltInRegistries.ITEM.containsKey(tPrimary), "the BlockItem pair registers (the obtainable-machine face)");
		assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(tPrimary), "the BET pair registers");
		assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(tAdvanced));
		// the live BET factories produce the two layouts
		BlockEntityType<?> tPrimaryType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(tPrimary);
		GT6BumbliaryBlockEntity tPrimaryTe = (GT6BumbliaryBlockEntity)tPrimaryType.create(POS, BuiltInRegistries.BLOCK.get(tPrimary).defaultBlockState());
		assertEquals(36, tPrimaryTe.slotCount(), "the primary layout");
		assertEquals("gt.multitileentity.bumbliary", tPrimaryTe.getTileEntityName());
		BlockEntityType<?> tAdvType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(tAdvanced);
		GT6BumbliaryBlockEntity tAdvTe = (GT6BumbliaryBlockEntity)tAdvType.create(POS, BuiltInRegistries.BLOCK.get(tAdvanced).defaultBlockState());
		assertEquals(20, tAdvTe.slotCount(), "the advanced layout");
		assertEquals("gt.multitileentity.bumbliary.advanced", tAdvTe.getTileEntityName(), "the Advanced :388 name");
	}
}
