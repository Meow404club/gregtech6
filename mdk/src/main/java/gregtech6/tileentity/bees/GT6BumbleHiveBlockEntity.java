package gregtech6.tileentity.bees;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6BeeHives;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Bumble Hive BE (task p32-bees-lv2) — the {@code MultiTileEntityBumbleHive} port
 * (MultiTileEntityBumbleHive.java:50-102, MTE 32755 "Bumble Hive",
 * Loader_MultiTileEntities.java:2041). The 03 base carries the paintable stratum
 * ({@link gregtech6.tileentity.IPaintableTE} + the PAINT model-data supply, the
 * upstream TileEntityBase07Paintable inheritance) — worldgen paints each hive its
 * biome-family colour at placement (WorldgenHives.java:203 {@code NBT_COLOR+NBT_PAINTED}),
 * and the spray-can face reaches it for free (GTSprayCanItem routes every
 * IPaintableTE).
 *
 * <p>Loot shell: a 9-slot inventory ({@code getDefaultInventory = new ItemStack[9]},
 * MultiTileEntityBumbleHive.java:99) over the house {@link GTItemStackHandler}, saved
 * under the upstream {@code CS.NBT_INV_LIST} key name {@code gt.inv}. The upstream
 * {@code mDroppable}/{@code canDrop} gate (any player harvest drops the whole
 * inventory, :97-98) collapses into the block's {@code playerDestroy} walk — the
 * harvest gate (scoop) runs upstream of it (canHarvestBlock), so only a proper
 * harvest ever reaches the drop (the wrong-tool break drops nothing, the
 * TOOL_scoop aHive semantics, Loader_MultiTileEntities.java:111).
 *
 * <p>Loot content (the Lv2 deferral closed in p33-bees-lv3-c-hive-loot): worldgen fills
 * slots 0-2 ({@link gregtech6.worldgen.GT6HiveFeature#fillLoot}, the :203 placeHive
 * shape) — slot 0 the family comb (the {@code bumbleProductStack} species table), slot 1
 * the princess, slot 2 the drones (the offspring gene the count), the royals carrying
 * the {@code gt.bumble} wild gene roll; the scoop harvest walks them all out.
 *
 * <p>Upstream faces folded in: the thermometer tooltip/click face pools with the
 * sensor domain (no thermometer item in the port yet — the tooltip row is the only
 * loss); explosion resistance/hardness/flammability ride the block carrier; the
 * dragon {@code canEntityDestroy} exception pools with the entity-interaction domain;
 * the handler insert/extract stays default-open (the upstream
 * {@code getAccessibleSlotsFromSide2 = ZL_INTEGER} never exposed a side anyway —
 * the capability face is the port's declarative equivalent).
 */
public class GT6BumbleHiveBlockEntity extends TileEntityBase03TicksAndSync {

	/** Upstream CS.NBT_INV_LIST = "gt.inv" (CS.java:1160) — the key names verbatim (ADR ruling 4). */
	public static final String NBT_INV_LIST = "gt.inv";

	/** The upstream slot count ({@code getDefaultInventory = new ItemStack[9]}, :99). */
	public static final int SLOT_COUNT = 9;

	public GT6BumbleHiveBlockEntity(BlockPos aPos, BlockState aState) {
		this(GT6BeeHives.HIVE_BE.get(), aPos, aState);
	}

	public GT6BumbleHiveBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState);
		setInventory(new GTItemStackHandler(SLOT_COUNT, this::setChanged));
	}

	@Override
	public String getTileEntityName() {
		return "gt.multitileentity.bumble.hive"; // MultiTileEntityBumbleHive.java:101 verbatim
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		if (mInventory == null) return;
		//? if forge {
		aNBT.put(NBT_INV_LIST, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INV_LIST, mInventory.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		*///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (mInventory == null || !aNBT.contains(NBT_INV_LIST, Tag.TAG_COMPOUND)) return;
		//? if forge {
		mInventory.deserializeNBT(aNBT.getCompound(NBT_INV_LIST));
		//?} else {
		/*mInventory.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INV_LIST)); // 21.1: provider-first
		*///?}
	}

	/** The inventory face for the worldgen fill and the block drop walk (slot 0 = the comb). */
	public GTItemStackHandler inventory() {
		return mInventory;
	}
}
