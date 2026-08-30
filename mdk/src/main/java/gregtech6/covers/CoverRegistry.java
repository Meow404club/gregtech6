package gregtech6.covers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The cover registry — 1.20.1 port of gregapi/cover/CoverRegistry.java (task p4-cover-core
 * ③). Upstream keys an {@code ItemStackMap<ItemStackContainer, ICover>} by item + metadata
 * (:34); 1.20.1 items carry no metadata, so the key collapses to the {@link Item} holder
 * while the {@code short} id/meta lookup surface stays intact for the CoverData lanes:
 * {@link #get(short, short)} rehydrates through the vanilla item-id map
 * ({@code Item.byId}, Item.java:78 — the save-format twin of {@code Item.getId}, :74).
 *
 * <p>The first cover mounts an EXISTING item (zero new registrations, spec ③): the iron
 * plate ({@code gt6:plate_iron}, the P2/P3 material-prefix item) — covers are literally
 * material plates upstream (Loader_OreProcessing.java:214 registers CoverTextureSimple
 * with the material's own texture; :88-97 the block-texture family), and a plate is the
 * thinnest material form this repo already ships. {@link GT6Covers} performs the put.
 */
public final class CoverRegistry {

	private static final Map<Item, ICover> COVERS = new HashMap<>();

	private CoverRegistry() {
	}

	/** Upstream :36-38 — the id/meta lookup; the meta axis is the 1.7.10 legacy shape (always 0 in 1.20.1). */
	public static @Nullable ICover get(short aID, short aMetaData) {
		return aID == 0 ? null : COVERS.get(getItem(aID));
	}

	/** Upstream :40-42. */
	public static @Nullable ICover get(@Nullable ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		return COVERS.get(aStack.getItem());
	}

	/** Upstream :44-46. */
	public static void put(@Nullable ItemStack aStack, ICover aCover) {
		if (aStack != null && !aStack.isEmpty()) put(aStack.getItem(), aCover);
	}

	/** Upstream :48-50 (the meta parameter is accepted for the upstream shape and ignored). */
	public static void put(@Nullable Item aItem, long aMetaData, ICover aCover) {
		put(aItem, aCover);
	}

	/** The 1.20.1 holder-key put. */
	public static void put(@Nullable Item aItem, ICover aCover) {
		if (aItem != null && aCover != null) COVERS.put(aItem, aCover);
	}

	/** Upstream :56-58 — the CoverData factory ({@code aNBT == null} = fresh empty store). */
	public static CoverData coverdata(ICoverableTE aTileEntity, @Nullable CompoundTag aNBT) {
		return aNBT == null ? new CoverData(aTileEntity) : new CoverData(aTileEntity, aNBT);
	}

	/** The registry id of the item (Item.getId, Item.java:74) — the CoverData mIDs lane value. */
	public static int getId(Item aItem) {
		return Item.getId(aItem);
	}

	/** The item for a registry id (Item.byId, Item.java:78) — null-safe over unmapped/removed ids. */
	public static Item getItem(short aID) {
		return Item.byId(aID & 0xFFFF);
	}

	/** Registration count (smoke tests / diagnostics). */
	public static int size() {
		return COVERS.size();
	}

	/** P1 registry discipline: tests reset the JVM-global map. */
	public static void reset() {
		COVERS.clear();
	}
}
