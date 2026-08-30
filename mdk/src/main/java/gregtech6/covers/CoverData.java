package gregtech6.covers;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 port of gregapi/cover/CoverData.java — the 6-face parallel-array cover store
 * (task p4-cover-core ①, ADR 2026-08-30-p4-cover-route keeps the GT6 singleton +
 * array shape against the GTCEu instantiated CoverBehavior model).
 *
 * <p>Field translation (:37-40 verbatim): {@code mIDs/mMetas/mVisuals/mValues} are the
 * four {@code short[6]} lanes, {@code mNBTs} the {@code CompoundTag[6]} lane and
 * {@code mBehaviours} the {@code ICover[6]} lane; {@code mVisualsToSync} :38 and
 * {@code mStopped} :38 ride along. Lane indices are the GT6 side order =
 * {@code Direction.get3DDataValue()}, one cover per face.
 *
 * <p>The {@code mIDs} lane holds the vanilla item-registry id of the cover's registered
 * item ({@code Item.getId}, Item.java:74) — the 1.20.1 counterpart of the 1.7.10 item id;
 * {@code mMetas} stays a zero-filled lane (1.20.1 items carry no metadata; the lane is
 * kept for the save-format shape).
 *
 * <p>NBT keys (:57-114 verbatim): ids {@code a}-{@code f}, metas {@code g}-{@code l},
 * visuals {@code m}-{@code r}, values {@code 0}-{@code 5}, NBT compounds
 * {@code s}-{@code x}, stop flag {@code y} — the GT6 save format shape. Zeros and empty
 * compounds are omitted (upstream only writes non-zero/non-empty lanes).
 *
 * <p>The upstream {@code box()} collision helpers (:193-216) are cut to the pool with
 * the collision surface; the delegator (:173) folds into the host reference.
 */
public class CoverData {

	public short mIDs[], mMetas[], mVisuals[], mValues[];
	public boolean mVisualsToSync[] = new boolean[] {false, false, false, false, false, false};
	public boolean mStopped = false;
	public CompoundTag mNBTs[];
	public ICover mBehaviours[] = new ICover[6];
	public final ICoverableTE mTileEntity;

	/** Upstream :43-46 — fresh empty store. */
	public CoverData(ICoverableTE aTileEntity) {
		mIDs = new short[6];
		mMetas = new short[6];
		mVisuals = new short[6];
		mValues = new short[6];
		mNBTs = new CompoundTag[6];
		mTileEntity = aTileEntity;
	}

	/** Upstream :48-55 — full rehydration; empty NBT lanes are nulled (:50) and loaded covers fire onCoverLoaded (:54). */
	public CoverData(short[] aIDs, short[] aMetas, short[] aVisuals, short[] aValues, CompoundTag[] aNBTs, boolean aStopped, ICoverableTE aTileEntity) {
		mVisuals = aVisuals;
		mValues = aValues;
		mNBTs = aNBTs;
		for (int i = 0; i < mNBTs.length; i++) if (mNBTs[i] != null && mNBTs[i].isEmpty()) mNBTs[i] = null;
		setIDs(aIDs, aMetas);
		mStopped = aStopped;
		mTileEntity = aTileEntity;
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) mBehaviours[tSide].onCoverLoaded(tSide, this);
	}

	/** Upstream :57-65 — the NBT keys {@code a}-{@code f}/{@code g}-{@code l}/{@code m}-{@code r}/{@code 0}-{@code 5}/{@code s}-{@code x}/{@code y}. */
	public CoverData(ICoverableTE aTileEntity, CompoundTag aNBT) {
		this(new short[] {aNBT.getShort("a"), aNBT.getShort("b"), aNBT.getShort("c"), aNBT.getShort("d"), aNBT.getShort("e"), aNBT.getShort("f")}
			, new short[] {aNBT.getShort("g"), aNBT.getShort("h"), aNBT.getShort("i"), aNBT.getShort("j"), aNBT.getShort("k"), aNBT.getShort("l")}
			, new short[] {aNBT.getShort("m"), aNBT.getShort("n"), aNBT.getShort("o"), aNBT.getShort("p"), aNBT.getShort("q"), aNBT.getShort("r")}
			, new short[] {aNBT.getShort("0"), aNBT.getShort("1"), aNBT.getShort("2"), aNBT.getShort("3"), aNBT.getShort("4"), aNBT.getShort("5")}
			, new CompoundTag[] {aNBT.getCompound("s"), aNBT.getCompound("t"), aNBT.getCompound("u"), aNBT.getCompound("v"), aNBT.getCompound("w"), aNBT.getCompound("x")}
			, aNBT.getBoolean("y")
			, aTileEntity);
	}

	/** Upstream :67. */
	public CompoundTag writeToNBT() {
		return writeToNBT(new CompoundTag(), true);
	}

	/** Upstream :68-114 verbatim — non-zero lanes only, visuals gated by {@link ICover#needsVisualsSaved} (:75). */
	public CompoundTag writeToNBT(CompoundTag aNBT, boolean aIncludeVisuals) {
		for (byte i = 0; i < 6; i++) {
			if (mIDs[i] == 0) continue;
			aNBT.putShort(SIDE_KEYS[i], mIDs[i]);
			if (mMetas[i] != 0) aNBT.putShort(META_KEYS[i], mMetas[i]);
			if (mValues[i] != 0) aNBT.putShort(VALUE_KEYS[i], mValues[i]);
			if (mNBTs[i] != null && !mNBTs[i].isEmpty()) aNBT.put(NBT_KEYS[i], mNBTs[i]);
			if (mVisuals[i] != 0 && (aIncludeVisuals || (mBehaviours[i] != null && mBehaviours[i].needsVisualsSaved(i, this)))) aNBT.putShort(VISUAL_KEYS[i], mVisuals[i]);
		}
		if (mStopped) aNBT.putBoolean("y", mStopped);
		return aNBT;
	}

	/** The per-lane NBT key families of :57-114, in GT6 side order. */
	public static final String[] SIDE_KEYS = {"a", "b", "c", "d", "e", "f"};
	public static final String[] META_KEYS = {"g", "h", "i", "j", "k", "l"};
	public static final String[] VISUAL_KEYS = {"m", "n", "o", "p", "q", "r"};
	public static final String[] VALUE_KEYS = {"0", "1", "2", "3", "4", "5"};
	public static final String[] NBT_KEYS = {"s", "t", "u", "v", "w", "x"};

	/** Upstream :116-120 — re-resolves the behaviours from the id/meta lanes. */
	public CoverData setIDs(short[] aIDs, short[] aMetas) {
		mIDs = aIDs;
		mMetas = aMetas;
		for (byte tSide = 0; tSide < 6; tSide++) mBehaviours[tSide] = mIDs[tSide] == 0 ? null : CoverRegistry.get(mIDs[tSide], mMetas[tSide]);
		return this;
	}

	/** Upstream :122-124 — the ItemStack form of {@link #set(byte, short, short, CompoundTag)}. */
	public CoverData set(byte aSide, @Nullable ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return set(aSide, (short) 0, (short) 0, null);
		CompoundTag tTag = aStack.getTag();
		return set(aSide, (short) CoverRegistry.getId(aStack.getItem()), (short) 0, tTag == null || tTag.isEmpty() ? null : tTag.copy());
	}

	/** Upstream :126-131 verbatim — empty NBT compounds are not stored (:129). */
	public CoverData set(byte aSide, short aID, short aMeta, @Nullable CompoundTag aNBT) {
		mIDs[aSide] = aID;
		mMetas[aSide] = aMeta;
		mBehaviours[aSide] = aID == 0 ? null : CoverRegistry.get(aID, aMeta);
		mNBTs[aSide] = aNBT == null || aNBT.isEmpty() ? null : aNBT.copy();
		return this;
	}

	/** Upstream :133-142 — the per-cover value lane. */
	public CoverData value(byte aSide, short aValue) {
		return value(aSide, aValue, false);
	}

	public CoverData value(byte aSide, short aValue, boolean aBlockUpdate) {
		if (mValues[aSide] != aValue) {
			mValues[aSide] = aValue;
			if (aBlockUpdate) mTileEntity.sendBlockUpdateFromCover();
		}
		return this;
	}

	/** Upstream :144-155 — the per-cover visual lane (the sync flag :150 feeds requiresSync). */
	public CoverData visual(byte aSide, short aVisual) {
		return visual(aSide, aVisual, false);
	}

	public CoverData visual(byte aSide, short aVisual, boolean aBlockUpdate) {
		if (mVisuals[aSide] != aVisual) {
			mVisuals[aSide] = aVisual;
			mVisualsToSync[aSide] = true;
			mTileEntity.updateCoverVisuals();
			if (aBlockUpdate) mTileEntity.sendBlockUpdateFromCover();
		}
		return this;
	}

	/** Upstream :157-162 — controller-driven stop; fires {@link ICover#onStoppedUpdate} on the change. */
	public boolean setStopped(boolean aStopped) {
		if (aStopped == mStopped) return false;
		mStopped = aStopped;
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) mBehaviours[tSide].onStoppedUpdate(tSide, this, mStopped);
		return true;
	}

	/** Upstream :164-167. */
	public boolean onBlockUpdate() {
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) mBehaviours[tSide].onBlockUpdate(tSide, this);
		return true;
	}

	/** Upstream :169-171 — the removal drop; behaviourless lanes rebuild the stack from the lanes. */
	public ItemStack getCoverItem(byte aSide) {
		if (mIDs[aSide] == 0) return ItemStack.EMPTY;
		if (mBehaviours[aSide] == null) {
			CompoundTag tTag = mNBTs[aSide] == null || mNBTs[aSide].isEmpty() ? null : mNBTs[aSide];
			ItemStack tStack = new ItemStack(CoverRegistry.getItem(mIDs[aSide]), 1);
			if (tTag != null) tStack.setTag(tTag.copy());
			return tStack;
		}
		return mBehaviours[aSide].getCoverItem(aSide, this);
	}

	/** Upstream :177-179. */
	public boolean requiresSync() {
		for (boolean tFlag : mVisualsToSync) if (tFlag) return true;
		return false;
	}

	/** Upstream :181-183. */
	public void resetSync() {
		for (int i = 0; i < mVisualsToSync.length; i++) mVisualsToSync[i] = false;
	}

	/** Upstream :185-187 — the Throwable guard folds into the dispatcher's own (03 :94-104). */
	public void tickPre(long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) mBehaviours[tSide].onTickPre(tSide, this, aTimer, aIsServerSide, aReceivedBlockUpdate, aReceivedInventoryUpdate);
	}

	/** Upstream :189-191. */
	public void tickPost(long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) mBehaviours[tSide].onTickPost(tSide, this, aTimer, aIsServerSide, aReceivedBlockUpdate, aReceivedInventoryUpdate);
	}

	/** True when no face carries a cover. */
	public boolean isEmpty() {
		for (byte tSide = 0; tSide < 6; tSide++) if (mBehaviours[tSide] != null) return false;
		return true;
	}

	/** @return the behaviour on that face or null. */
	public @Nullable ICover behaviour(byte aSide) {
		return mBehaviours[aSide];
	}
}
