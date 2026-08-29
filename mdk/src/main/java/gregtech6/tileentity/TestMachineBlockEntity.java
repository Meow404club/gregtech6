package gregtech6.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.TestMachineBlock;
import gregtech6.registry.GTBlockEntities;

/**
 * Framework test vehicle for task p3-be-framework (the real example machine is the
 * WAVE-2 chest, ADR-P3-5). Exercises the 01Root/03TicksAndSync face end to end:
 * the eight-phase tick dispatch, the item-handler capability, and both sync channels.
 *
 * <p>NBT: "tick_count" + "inventory" + the base "te_name", written by saveAdditional.
 * Both sync channels deliver the same payload (the base 03 getUpdateTag =
 * saveWithoutMetadata, the upstream getClientDataPacket(true) "send all" case; the
 * partial-sync routing of getClientDataPacket(false) is a later feature need).
 */
public class TestMachineBlockEntity extends TileEntityBase03TicksAndSync {

	/** Persisted tick counter — proves the dispatcher runs and NBT round-trips. */
	private long mTickCount = 0;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime (no supplier self-reference). */
	public TestMachineBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.TEST_MACHINE_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point: BlockEntityType.Builder.of(...).build(null) works without a registry. */
	public TestMachineBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// mIsTicking follows the mounting block (upstream notick chain: TileEntityBase01Root(false))
		super(aState.getBlock() instanceof TestMachineBlock tBlock ? tBlock.isTicking() : true, aType, aPos, aState);
		setInventory(new GTItemStackHandler(4, this::setChanged));
	}

	/** Bare constructor for offline unit tests (no registry, no BlockEntity triple). */
	public TestMachineBlockEntity(GTItemStackHandler aInventory, boolean aTicking) {
		super(aTicking, null, BlockPos.ZERO, null);
		setInventory(aInventory);
	}

	@Override
	public String getTileEntityName() {
		return "test_machine"; // upstream :154 — DO NOT START YOUR NAME WITH "gt."; BET registry path mirrors it
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide) {
			mTickCount++;
			// periodic client sync (upstream updateClientData usage pattern, :152-153 doc)
			if (mTickCount % 40 == 0) {
				updateClientData();
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong("tick_count", mTickCount);
		aNBT.put("inventory", mInventory.serializeNBT());
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains("tick_count", Tag.TAG_ANY_NUMERIC)) {
			mTickCount = aNBT.getLong("tick_count");
		}
		if (aNBT.contains("inventory", Tag.TAG_COMPOUND)) {
			mInventory.deserializeNBT(aNBT.getCompound("inventory"));
		}
	}

	public long getTickCount() {
		return mTickCount;
	}

	public GTItemStackHandler getInventory() {
		return mInventory;
	}
}
