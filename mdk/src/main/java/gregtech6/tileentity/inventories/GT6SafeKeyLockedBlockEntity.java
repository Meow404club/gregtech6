package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTBlockEntities;

/**
 * The Key Locked GT6 Safe — 1.20.1 counterpart of
 * {@code gregtech/tileentity/inventories/MultiTileEntitySafeKeyLocked.java:49-137} (the
 * metalset row :135, "Key Locked %s Safe" id 3000+aID, NBT_INV_SIZE 15).
 *
 * <p>Clauses, each anchored:
 * <ul>
 * <li>{@code mID}/{@code mOpened} (:52-53) with the NBT pair (readFromNBT2 :55-59 /
 *     writeToNBT2 :62-67) — the port keys are {@code gt.key}/{@code gt.open} (the upstream
 *     CS.NBT_KEY/NBT_OPEN constants);</li>
 * <li>the interaction gate {@code allowInteraction :77 allowInteraction = mOpened}: the
 *     GUI arm only opens (and the armor of the container is reachable) while the latch is
 *     OPEN — {@link #isOpen()} carries it;</li>
 * <li>the key toggle {@code useKey :83-95}: a key id equal to {@code mID} (or the first
 *     key when {@code mID == 0}, the claim arm) FLIPS the latch; wrong ids bounce. The
 *     upstream caller is the GT6 key item through {@code ITileEntityKeyInteractable} —
 *     the key item family is not in this port universe yet, so the method is the public
 *     SEAM (the future key item, or a data-driven arm, calls it verbatim; RCON drives it
 *     through the latch NBT). The {@code :86 UT.Sounds.send(SFX.MC_CLICK, 1.0F, 0.25F)}
 *     feedback rides the flip guarded server-side (the vanilla UI click placeholder, the
 *     CoverFilterItem SFX precedent);</li>
 * <li>{@code canCloneKey :97-99} (clone only while open) folds with the key item family
 *     (declared defer);</li>
 * <li>the open/closed texture pair folds into the placeholder block art (render pool,
 *     the hopper-family placeholder precedent).</li>
 * </ul>
 */
public class GT6SafeKeyLockedBlockEntity extends GT6SafeBlockEntity {

	/** Upstream CS.NBT_KEY — the claimed key id key. */
	public static final String NBT_KEY = "gt.key";

	/** Upstream CS.NBT_OPEN — the latch key. */
	public static final String NBT_OPEN = "gt.open";

	/** Upstream :52 {@code mID = 0} — zero = unclaimed, the first matching key claims it. */
	public long mID = 0;

	/** Upstream :53 {@code mOpened = F} — closed by default. */
	public boolean mOpened = false;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6SafeKeyLockedBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.SAFE_KEYLOCKED_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6SafeKeyLockedBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "safe_keylocked"; // upstream "gt.multitileentity.safe.keylocked" — the BET path mirrors it
	}

	/** Upstream :77 {@code allowInteraction = mOpened} — the GUI gate. */
	@Override
	public boolean isOpen() {
		return mOpened;
	}

	/**
	 * Upstream {@code useKey :83-95} port: flip the latch on a matching key id, claim on
	 * the first key while unclaimed; false when no key matches. The flip plays the
	 * upstream :86 click feedback ({@code SFX.MC_CLICK} 1.0F volume, 0.25F pitch — the
	 * vanilla UI click placeholder) guarded server-side like the flip itself, so the
	 * offline seam stays silent.
	 */
	public boolean useKey(long... aKeys) {
		for (long tID : aKeys) {
			if (mID == 0) mID = tID;
			if (mID != 0 && tID == mID) {
				mOpened = !mOpened;
				updateInventory(); // the latch rides the inventory-change sync window
				if (hasLevel() && !isClientSide()) { // upstream :86 UT.Sounds.send(SFX.MC_CLICK, 1.0F, 0.25F)
					getLevel().playSound(null, getBlockPos(),
							net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
							net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.25F);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_KEY, Tag.TAG_ANY_NUMERIC)) mID = aNBT.getLong(NBT_KEY);
		if (aNBT.contains(NBT_OPEN, Tag.TAG_ANY_NUMERIC)) mOpened = aNBT.getBoolean(NBT_OPEN);
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_KEY, mID); // upstream :64 verbatim (always written)
		aNBT.putBoolean(NBT_OPEN, mOpened); // upstream :65 verbatim (always written)
	}
}
