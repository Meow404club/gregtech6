package gregtech6.tileentity.multiblocks;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

import gregtech6.items.tools.GT6BuilderWandItem;
import gregtech6.tileentity.GTItemStackHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The part BE tests (task p4-multiblock-framework acceptance ①): the mTargetPos NBT
 * round-trip (upstream MultiTileEntityMultiBlockPart :76/:131/:161-166), the lazy
 * getTarget rebuild with the isInsideStructure validation (:199-214), the mMode bitmask
 * and the capability relay to the controller.
 */
public class MultiBlockPartBlockEntityTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos PART_CELL = new BlockPos(101, 65, 100); // a C1 part cell

	@Test
	void targetPositionRoundTripsThroughNBT() {
		InventoryController tController = sInventoryControllerType.create(C1, Blocks.BRICKS.defaultBlockState());
		MultiBlockPartBlockEntity tPart = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());

		tPart.setTarget(tController, 3, MultiBlockPartBlockEntity.ONLY_ITEM_ENERGY);

		// saveWithoutMetadata = saveAdditional only (saveWithFullMetadata would need the
		// BET's registry id, unreachable offline)
		CompoundTag tTag = tPart.saveWithoutMetadata();
		assertTrue(tTag.contains(MultiBlockPartBlockEntity.NBT_TARGET));
		assertEquals(C1.getX(), tTag.getInt(MultiBlockPartBlockEntity.NBT_TARGET_X));
		assertEquals(C1.getY(), tTag.getInt(MultiBlockPartBlockEntity.NBT_TARGET_Y));
		assertEquals(C1.getZ(), tTag.getInt(MultiBlockPartBlockEntity.NBT_TARGET_Z));

		MultiBlockPartBlockEntity tReloaded = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		tReloaded.load(tTag);
		assertEquals(C1, tReloaded.mTargetPos, "mTargetPos round-trip");
		assertEquals(3, tReloaded.mDesign);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_ENERGY, tReloaded.mMode);
	}

	@Test
	void absentTargetLeavesNoTargetKey() {
		MultiBlockPartBlockEntity tPart = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		tPart.setTarget(null, 0, 0);

		CompoundTag tTag = tPart.saveWithoutMetadata();
		assertFalse(tTag.contains(MultiBlockPartBlockEntity.NBT_TARGET), "an unclaimed part persists no target");

		MultiBlockPartBlockEntity tReloaded = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		tReloaded.load(tTag);
		assertNull(tReloaded.mTargetPos);
		assertNull(tReloaded.mTarget);
	}

	@Test
	void lazyRebuildResolvesTheControllerThroughTheOwnershipCheck() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestController tController = placeController(tLevel, sTestControllerType, C1, (byte) 2);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, PART_CELL);
		tPart.setTarget(tController, 2, 0);
		tPart.mTarget = null; // simulate a world reload (NBT present, cache gone)

		assertSame(tController, tPart.getTarget(false), "the rebuild validates through isInsideStructure");
		assertEquals(C1, tPart.mTargetPos);
	}

	@Test
	void staleOwnershipClearsTargetAndDesign() {
		// upstream :207-210 — a controller that no longer contains this cell drops the claim
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestController tController = placeController(tLevel, sTestControllerType, C1, (byte) 2);
		tController.mInside = false; // the controller no longer owns this cell
		MultiBlockPartBlockEntity tPart = placePart(tLevel, PART_CELL);
		tPart.setTarget(tController, 2, 0);
		tPart.mTarget = null;

		assertNull(tPart.getTarget(false));
		assertNull(tPart.mTargetPos, "the stale pointer self-clears (:208)");
		assertEquals(0, tPart.mDesign, "the design resets with the claim (:209)");
	}

	@Test
	void capabilityRelayResolvesTheController() {
		// the RESOLUTION half of the relay — ForgeCapabilities cannot class-init offline
		// (CapabilityToken "This will be implemented by a transformer"), so the
		// getCapability forwarding itself is exercised on the live server only
		InventoryController tController = sInventoryControllerType.create(C1, Blocks.BRICKS.defaultBlockState());
		MultiBlockPartBlockEntity tPart = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		tPart.setTarget(tController, 0, MultiBlockPartBlockEntity.EVERYTHING);

		assertSame(tController, tPart.relayTarget(), "the part resolves its controller for the relay");

		// an unclaimed part resolves nothing
		MultiBlockPartBlockEntity tOrphan = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		assertNull(tOrphan.relayTarget(), "orphan resolves empty");
	}

	@Test
	void modeBitmaskConstantsCarryTheUpstreamTable() {
		// the :85-126 composite identities
		assertEquals(MultiBlockPartBlockEntity.NO_ENERGY_IN | MultiBlockPartBlockEntity.NO_ENERGY_OUT, MultiBlockPartBlockEntity.NO_ENERGY);
		assertEquals(MultiBlockPartBlockEntity.NO_ITEM_IN | MultiBlockPartBlockEntity.NO_ITEM_OUT, MultiBlockPartBlockEntity.NO_ITEM);
		assertEquals(
				MultiBlockPartBlockEntity.NO_ITEM_IN | MultiBlockPartBlockEntity.NO_ITEM_OUT
						| MultiBlockPartBlockEntity.NO_FLUID_IN | MultiBlockPartBlockEntity.NO_FLUID_OUT
						| MultiBlockPartBlockEntity.NO_ENERGY_IN | MultiBlockPartBlockEntity.NO_ENERGY_OUT,
				~MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY);
		assertEquals(-1, MultiBlockPartBlockEntity.NOTHING);
		assertEquals(0, MultiBlockPartBlockEntity.EVERYTHING);
	}

	@Test
	void designBindsToUnsignedByte() {
		// upstream setDesign bind8 :224
		MultiBlockPartBlockEntity tPart = sPartType.create(PART_CELL, Blocks.BRICKS.defaultBlockState());
		assertTrue(tPart.setDesign(300));
		assertEquals(255, tPart.mDesign);
		assertFalse(tPart.setDesign(255), "no change, no dirty");
		tPart.setDesign(-5);
		assertEquals(0, tPart.mDesign);
	}

	// ---------------------------------------------------------------------------
	// the builder-wand relay (task p24-builder-wand — the upstream part :251-266
	// minimal faithful face, builder-wand exclusive)
	// ---------------------------------------------------------------------------

	@Test
	void wandRelayResolvesTheLinkedController() {
		// the happy relay: a linked part re-resolves its controller through the lazy
		// rebuild (cache gone = the world-reload shape) and stands inside the structure
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestController tController = placeController(tLevel, sTestControllerType, C1, (byte) 2);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, PART_CELL);
		tPart.setTarget(tController, 0, 0);
		tPart.mTarget = null; // simulate a world reload (NBT present, cache gone)

		assertSame(tController, tPart.wandTarget(), "the relay rides the lazy rebuild + ownership check");
		assertSame(tController, GT6BuilderWandItem.scaffoldTarget(tLevel, PART_CELL),
				"the wand target resolution rides the relay (the upstream :261 arm)");
		assertSame(tController, GT6BuilderWandItem.scaffoldTarget(tLevel, C1),
				"the controller itself is the direct target (the upstream :143 arm)");
	}

	@Test
	void wandRelayRefusesTheUnlinkedPart() {
		// the no-controller arm: the upstream :256-258 chat line is the declared silent
		// cut — the relay answers null and the wand PASSes
		MultiBlockLevel tLevel = new MultiBlockLevel();
		placeController(tLevel, sTestControllerType, C1, (byte) 2);
		MultiBlockPartBlockEntity tOrphan = placePart(tLevel, PART_CELL);
		assertNull(tOrphan.wandTarget(), "an unlinked part has no relay target");
		assertNull(GT6BuilderWandItem.scaffoldTarget(tLevel, PART_CELL), "the wand finds no scaffold target");
		assertNull(GT6BuilderWandItem.scaffoldTarget(tLevel, new BlockPos(0, 1, 0)),
				"a plain block cell (no BE) is no scaffold target either");
	}

	@Test
	void wandRelayRefusesTheStaleOwnership() {
		// the upstream :261 re-check face — the cached controller no longer contains this
		// cell, and the lazy rebuild cannot see it (the cache is non-null and the BE is
		// not removed): the relay refuses to fire
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestController tController = placeController(tLevel, sTestControllerType, C1, (byte) 2);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, PART_CELL);
		tPart.setTarget(tController, 0, 0);
		tController.mInside = false; // the ownership lapsed under a live cache

		assertNull(tPart.wandTarget(), "the relay refuses the stale ownership");
		assertNull(GT6BuilderWandItem.scaffoldTarget(tLevel, PART_CELL), "the wand has no target through the stale part");
	}
}
