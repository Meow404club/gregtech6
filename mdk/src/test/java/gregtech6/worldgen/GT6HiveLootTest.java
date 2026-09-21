package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.items.bees.GT6BumbleGenes;
import gregtech6.items.bees.GT6Bumbles;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.tileentity.bees.GT6BumbleHiveBlockEntity;

/**
 * The hive LOOT reconciliation (task p33-bees-lv3-c-hive-loot, acceptance ①): the
 * {@link GT6HiveFeature#fillLoot} layout against the upstream placeHive inventory table
 * (WorldgenHives.java:203) — slot 0 the family comb over the :189-213 species-comb table
 * with the {@code UT.Code.units(work, 10000, 10, T)} count fold, slot 1 the princess
 * (count 1, the type digit riding the item face), slot 2 the drones (count = the
 * offspring gene) — and the {@code gt.bumble} 14-key gene NBT riding both royals.
 *
 * <p>Offline-safe: the vanilla-BRICKS BET fixture (the GT6BumbleHiveBlockEntityTest
 * posture) for the {@link #combCountIsTheUnitsFold} pin; the item-walking reconciliation
 * runs on the FML-booted leg and skips the bare-JVM leg via the registry latch (the
 * GT6BumblesTest form — the RCON chain p33_bees_c carries the live face).
 */
class GT6HiveLootTest {

	static BlockEntityType<GT6BumbleHiveBlockEntity> sHiveType;

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
		BlockEntityType<GT6BumbleHiveBlockEntity>[] tHolder = (BlockEntityType<GT6BumbleHiveBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbleHiveBlockEntity(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sHiveType = tHolder[0];
	}

	static GT6BumbleHiveBlockEntity hive() {
		return new GT6BumbleHiveBlockEntity(sHiveType, POS, Blocks.BRICKS.defaultBlockState());
	}

	/** The FML-booted containment latch: the bare-JVM leg cannot even construct a mod item. */
	static boolean beeItemsLive() {
		//? if forge {
		return BuiltInRegistries.ITEM.containsKey(new ResourceLocation("gt6", "bumble_drone"));
		//?} else {
		/*return BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("gt6", "bumble_drone"));
		 *///?}
	}

	@Test
	void combCountIsTheUnitsFold() {
		// UT.Code.units(work, 10000, 10, T) = ceil(work/1000) — the :203 comb count
		assertEquals(1, GT6HiveFeature.combCount(1), "the work floor binds to one comb");
		assertEquals(1, GT6HiveFeature.combCount(999));
		assertEquals(1, GT6HiveFeature.combCount(1000));
		assertEquals(2, GT6HiveFeature.combCount(1001), "the round-up remainder");
		assertEquals(5, GT6HiveFeature.combCount(4500));
		assertEquals(10, GT6HiveFeature.combCount(10000), "the work ceiling binds to ten");
		Random tRandom = new Random(4);
		for (int i = 0; i < 200; i++) {
			long tWork = GT6BumbleGenes.getWorkForce(GT6BumbleGenes.rollGenes(tRandom));
			int tCount = GT6HiveFeature.combCount(tWork);
			assertTrue(tCount >= 1 && tCount <= 10, "the rolled comb count stays 1..10: " + tCount);
		}
	}

	@Test
	void theItemWalkLegIsDeclared() {
		// the latch state pin: the item-walking reconciliation runs where the registry
		// resolves (the 21.1 FML-booted JVM, the GTOfflineTestBase javadoc) and skips the
		// 1.20.1 offline JVM (the frozen registry — the GT6BumblesTest form); the live
		// 1.20.1 face is the RCON chain p33_bees_c. If a leg's path flips silently this
		// pin REDs instead of the reconciliation becoming a silent no-op.
		//? if forge {
		assertFalse(beeItemsLive(), "the 1.20.1 offline leg does not resolve the mod items");
		//?} else {
		/*assertTrue(beeItemsLive(), "the 21.1 FML-booted leg resolves the registry");
		 *///?}
	}

	@Test
	void lootTableReconciliationOnTheFilledHive() {
		// the FML-booted leg runs the real item walk; the bare-JVM leg carries the pins
		if (!beeItemsLive()) return;
		GT6BumbleHiveBlockEntity tHive = hive();
		CompoundTag tGenes = GT6BumbleGenes.rollGenes(286, 0.5F, true, false, new Random(7));
		GT6BumbleGenes.setWorkForce(tGenes, 4500); // the deterministic comb-count face
		GT6BumbleGenes.setOffspring(tGenes, 3);    // the deterministic drone count
		GT6HiveFeature.fillLoot(tHive, GT6HiveFeature.HiveKind.STONE, tGenes);

		// slot 0: the species comb — STONE species 500 → family 5 → the rock comb (:196)
		ItemStack tComb = tHive.inventory().getStackInSlot(0);
		assertSame(GT6BeeCombs.comb("rock").get(), tComb.getItem(), "the :190-213 species-comb walk");
		assertEquals(5, tComb.getCount(), "combCount(4500) = 5 (the units fold)");

		// slot 1: the princess — count 1, the type digit rides the face, the code is the species
		ItemStack tPrincess = tHive.inventory().getStackInSlot(1);
		GT6Bumbles.FaceRow tFace = GT6Bumbles.faceOf(tPrincess);
		assertNotNull(tFace, "the stack is a GT6 bumble");
		assertEquals(GT6Bumbles.TYPE_PRINCESS, tFace.face(), "the :203 speciesID+1 → the princess face");
		assertFalse(tFace.scanned(), "the wild princess is unscanned");
		assertEquals(1, tPrincess.getCount(), "the princess count is 1");
		assertEquals(500, GT6BumbleGenes.codeOf(tPrincess), "the code stays the species");

		// the gene NBT rides the princess — the wild roll verbatim
		CompoundTag tRidden = GT6BumbleGenes.readGenes(tPrincess);
		assertNotNull(tRidden, "the gt.bumble compound rides the princess");
		assertEquals(tGenes, tRidden, "the wild roll attached verbatim");
		assertEquals(4500, GT6BumbleGenes.getWorkForce(tRidden), "the work gene feeds the comb count");
		assertTrue(GT6BumbleGenes.getOffspring(tRidden) >= 1 && GT6BumbleGenes.getOffspring(tRidden) <= 4,
				"the offspring gene stays 1..4 (the :138 roll)");

		// slot 2: the drones — count = the offspring gene, same code, the same genes
		ItemStack tDrone = tHive.inventory().getStackInSlot(2);
		assertNotNull(GT6Bumbles.faceOf(tDrone), "the drone stack is a GT6 bumble");
		assertEquals(GT6Bumbles.TYPE_DRONE, GT6Bumbles.faceOf(tDrone).face(), "the :203 drone face");
		assertEquals(3, tDrone.getCount(), "the drone count is the offspring gene");
		assertEquals(500, GT6BumbleGenes.codeOf(tDrone), "the drone code is the species");
		assertEquals(tGenes, GT6BumbleGenes.readGenes(tDrone), "the gene compound rides the drone too");

		// slots 3..8 stay empty (the :203 makeInv three-stack shape)
		for (int i = 3; i < 9; i++) {
			assertTrue(tHive.inventory().getStackInSlot(i).isEmpty(), "slot " + i + " stays empty");
		}
	}

	@Test
	void theSpeciesCombWalkCoversTheHiveKinds() {
		// every HiveKind species → its family comb (the :190-212 table rows the kinds touch)
		if (!beeItemsLive()) return;
		Object[][] tRows = {
			{GT6HiveFeature.HiveKind.GRASS      ,     0, "honey" },
			{GT6HiveFeature.HiveKind.WATER      ,   100, "water" },
			{GT6HiveFeature.HiveKind.MAGICAL    ,   200, "magic" },
			{GT6HiveFeature.HiveKind.NETHER_BIOME,  300, "nether"},
			{GT6HiveFeature.HiveKind.END_BIOME  ,   400, "end"   },
			{GT6HiveFeature.HiveKind.STONE      ,   500, "rock"  },
			{GT6HiveFeature.HiveKind.JUNGLE     ,   600, "jungle"},
			{GT6HiveFeature.HiveKind.FROZEN     ,   700, "frozen"},
			{GT6HiveFeature.HiveKind.SHROOM     ,   800, "shroom"},
			{GT6HiveFeature.HiveKind.SAND       ,   900, "sandy" }};
		for (Object[] tRow : tRows) {
			GT6BumbleHiveBlockEntity tHive = hive();
			GT6HiveFeature.fillLoot(tHive, (GT6HiveFeature.HiveKind)tRow[0], GT6BumbleGenes.rollGenes(new Random(3)));
			assertSame(GT6BeeCombs.comb((String)tRow[2]).get(), tHive.inventory().getStackInSlot(0).getItem(),
					((GT6HiveFeature.HiveKind)tRow[0]) + " → the " + tRow[2] + " comb");
			assertEquals(((Integer)tRow[1]).intValue(), GT6BumbleGenes.codeOf(tHive.inventory().getStackInSlot(1)),
					((GT6HiveFeature.HiveKind)tRow[0]) + " → the species code on the princess");
		}
	}

	@Test
	void anEmptyGenesTagStillFillsTheThreeStacks() {
		// the lazy face: an all-default gene compound (work 1 → one comb, offspring 1 → one drone)
		if (!beeItemsLive()) return;
		GT6BumbleHiveBlockEntity tHive = hive();
		GT6HiveFeature.fillLoot(tHive, GT6HiveFeature.HiveKind.GRASS, new CompoundTag());
		ItemStack tComb = tHive.inventory().getStackInSlot(0);
		assertSame(GT6BeeCombs.comb("honey").get(), tComb.getItem(), "the GRASS family is the honey comb");
		assertEquals(1, tComb.getCount(), "work binds to 1 → one comb");
		assertEquals(1, tHive.inventory().getStackInSlot(1).getCount(), "the princess is always one");
		assertEquals(1, tHive.inventory().getStackInSlot(2).getCount(), "offspring binds to 1 → one drone");
		assertEquals(0, GT6BumbleGenes.codeOf(tHive.inventory().getStackInSlot(2)), "the Wild code");
		// the vanilla-carrier sanity for the offline pins: the gene seam functions are item-agnostic
		ItemStack tCarrier = new ItemStack(Items.STONE);
		GT6BumbleGenes.setCode(tCarrier, 500);
		assertEquals(500, GT6BumbleGenes.codeOf(tCarrier));
	}
}
