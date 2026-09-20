package gregtech6.tileentity.misc;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Sandwich BlockEntity (task p32-placeables) — the port of upstream MTE 32105
 * {@code MultiTileEntitySandwich} (tmp/gt6-1.7.10 .../food/MultiTileEntitySandwich.java),
 * collapsed to the faces the task card accepts: the placed bite face (the upstream
 * onBlockActivated2 add-ingredient arm is the food-domain pool — the port carries no
 * {@code Sandwiches.INGREDIENTS} registry, so the placed click BITES) and the bites
 * comparator ({@code getComparatorInputOverride :194}, {@code bind4(mSize-1)} verbatim).
 *
 * <p>Collapsed model: the upstream 16-slot {@code mStacks} + per-ingredient display model
 * render into ONE persisted scalar — {@code mSize}, the stacked height in 1/16s (the
 * upstream {@code updateSandwichSize} :212-216 sums the ingredient thicknesses into the
 * same scalar; the default NBT sandwich of :64-74 is ten ingredient layers → 10). The
 * comparator therefore reads the bites directly: full default sandwich 9, one bite per
 * click, empty at 0 (the upstream rottable/redstone-sandwich/food-value faces are the
 * food-domain pool, the declared cuts).
 */
public class GT6SandwichBlockEntity extends TileEntityBase03TicksAndSync {

	/** The persisted height scalar key (the NBT_VALUE fold; 1/16 units, 1..16). */
	public static final String NBT_SIZE = "gt6.size";

	/** The default sandwich (upstream :64-74, the ten-layer NBT default). */
	public static final int DEFAULT_SIZE = 10;

	/** The per-bite nutrition (the 10-layer default totals 8 upstream-ish → a bite is a fifth-ish; kept simple). */
	public static final int BITE_NUTRITION = 2;
	/** The per-bite saturation modifier. */
	public static final float BITE_SATURATION = 0.3F;

	private int mSize = DEFAULT_SIZE;

	public GT6SandwichBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6SandwichBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Placeables.SANDWICH_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "sandwich"; // BET registry path mirrors it (GT6Placeables.SANDWICH_BE)
	}

	/** The stacked height in 1/16s (the upstream {@code mSize}). */
	public int size() {
		return mSize;
	}

	/** The bites comparator (upstream :194 {@code bind4(mSize-1)} verbatim — 0..15). */
	public int comparatorValue() {
		return Math.max(0, Math.min(15, mSize - 1));
	}

	/**
	 * The placed bite face: feed the player one bite, shrink by one layer, sync. Empty →
	 * {@code false} (the block walk removes the block; the upstream setToAir semantics).
	 */
	public boolean bite(Player aPlayer) {
		if (mSize <= 0) return false;
		FoodData tFood = aPlayer.getFoodData();
		if (!aPlayer.getAbilities().instabuild && tFood.getFoodLevel() >= 20) return false; // the vanilla cake gate
		if (!isClientSide()) {
			tFood.eat(BITE_NUTRITION, BITE_SATURATION);
			mSize--;
			setChanged();
			sendClientData(); // non-ticking BE: flush the sync window directly (the 03 base sendBlockUpdated face)
			if (hasLevel()) getLevel().playSound(null, getBlockPos(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.5F, 1.0F);
		}
		return true;
	}

	/** Whether the bite face emptied the sandwich (the block walk removes it, no drops). */
	public boolean isEmpty() {
		return mSize <= 0;
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putInt(NBT_SIZE, mSize);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_SIZE, Tag.TAG_ANY_NUMERIC)) {
			mSize = Math.max(0, Math.min(16, aNBT.getInt(NBT_SIZE)));
		}
	}
}
