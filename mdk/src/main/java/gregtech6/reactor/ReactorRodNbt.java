package gregtech6.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * The rod burn state carrier on the item stack (task debt-reactor-b-2x2-be) — the port
 * counterpart of the item-NBT halves of the upstream rod classes: the fuel rod's
 * {@code mDurability/mModerated/oModerated} (MultiTileEntityReactorRodNuclear.java:41-44,
 * the {@code writeItemNBT2} :68-73 write and the {@code readFromNBT2} :49-56 read), the
 * moderator's {@code mModeration/oModeration} shorts (RodModerator.java:31-32, same
 * read/write shape) and the breeder's {@code mDurability} (RodBreeder, same key).
 *
 * <p>Keys are the upstream CS constants verbatim: {@code gt.durability}
 * (NBT_DURABILITY, CS.java:1187), {@code gt.maxdurability} (NBT_MAXDURABILITY :1188 —
 * the fresh-rod fallback a canner recipe output carries, Nuclear.java:49) and
 * {@code gt.nuclear.mod} / {@code gt.nuclear.mod.o} (NBT_NUCLEAR_MOD :1185 — a BOOLEAN
 * on fuel rods, a NUMBER on moderators; the typed accessors below keep the two uses
 * apart, exactly like the upstream getBoolean/getShort reads).
 *
 * <p>The state rides the stack so a half-burnt rod keeps its burn state when the
 * pincers/hopper face pulls it out — the upstream item-NBT semantics. Cross-leg form:
 * the 1.20.1 freeform stack tag, 21.1 the opaque CUSTOM_DATA envelope (the
 * GT6BatteryItem.readStoredRaw shape, same keys on both legs).
 */
public final class ReactorRodNbt {

	/** Upstream NBT_DURABILITY (CS.java:1187). */
	public static final String NBT_DURABILITY = "gt.durability";
	/** Upstream NBT_MAXDURABILITY (CS.java:1188). */
	public static final String NBT_MAXDURABILITY = "gt.maxdurability";
	/** Upstream NBT_NUCLEAR_MOD (CS.java:1185). */
	public static final String NBT_MOD = "gt.nuclear.mod";
	/** Upstream NBT_NUCLEAR_MOD + ".o". */
	public static final String NBT_MOD_O = "gt.nuclear.mod.o";

	private ReactorRodNbt() {/**/}

	// ---------------------------------------------------------------------------
	// the raw carrier (the per-leg fork, the GT6BatteryItem form)
	// ---------------------------------------------------------------------------

	//? if forge {
	private static CompoundTag tag(ItemStack aStack) {
		return aStack.hasTag() ? aStack.getTag() : null;
	}
	//?} else {
	/*private static CompoundTag tag(ItemStack aStack) {
		return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
	}
	 *///?}

	/** The 1.20.1 freeform write — getOrCreateTag. */
	//? if forge {
	private static CompoundTag tagForWrite(ItemStack aStack) {
		return aStack.getOrCreateTag();
	}
	//?} else {
	/*private static CompoundTag tagForWrite(ItemStack aStack) {
		// 21.1: every write re-packs the CUSTOM_DATA envelope from a fresh copy — the
		// GT6BatteryItem.writeItemNBT form.
		return tag(aStack);
	}
	private static void pack(ItemStack aStack, CompoundTag aTag) {
		aStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(aTag));
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// durability (fuel Nuclear:41 / breeder Breeder — long, the maxdurability fallback)
	// ---------------------------------------------------------------------------

	/** The burn budget on the stack, 0 when absent (Nuclear.java:49 verbatim shape). */
	public static long durability(ItemStack aStack) {
		if (aStack.isEmpty()) return 0;
		CompoundTag tTag = tag(aStack);
		//? if forge {
		if (tTag == null) return 0;
		return tTag.contains(NBT_DURABILITY, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_DURABILITY)
				: tTag.contains(NBT_MAXDURABILITY, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_MAXDURABILITY) : 0;
		//?} else {
		/*return tTag.contains(NBT_DURABILITY, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_DURABILITY)
				: tTag.contains(NBT_MAXDURABILITY, Tag.TAG_ANY_NUMERIC) ? tTag.getLong(NBT_MAXDURABILITY) : 0;
		 *///?}
	}

	/** The burn-budget write (Nuclear.java:69 writeItemNBT2). */
	public static void setDurability(ItemStack aStack, long aDurability) {
		if (aStack.isEmpty()) return;
		//? if forge {
		tagForWrite(aStack).putLong(NBT_DURABILITY, aDurability);
		//?} else {
		/*CompoundTag tTag = tag(aStack);
		tTag.putLong(NBT_DURABILITY, aDurability);
		pack(aStack, tTag);
		 *///?}
	}

	/**
	 * The fresh-rod burn-budget write (task debt-reactor-c-rods): the NBT_MAXDURABILITY
	 * the upstream registration bakes into the item template (LME:746-762/:782-785), which
	 * the 1.7.10 Canner output therefore carries and the durability() read falls back to
	 * (Nuclear.java:49). The port's recipe pours write it on the fuel/breeder outputs.
	 */
	public static void setMaxDurability(ItemStack aStack, long aMaxDurability) {
		if (aStack.isEmpty()) return;
		//? if forge {
		tagForWrite(aStack).putLong(NBT_MAXDURABILITY, aMaxDurability);
		//?} else {
		/*CompoundTag tTag = tag(aStack);
		tTag.putLong(NBT_MAXDURABILITY, aMaxDurability);
		pack(aStack, tTag);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the fuel moderation flags (Nuclear:44 — mModerated / oModerated booleans)
	// ---------------------------------------------------------------------------

	/** Whether a moderated emitter has touched this fuel rod since the latch (Nuclear.java:54). */
	public static boolean moderated(ItemStack aStack) {
		if (aStack.isEmpty()) return false;
		CompoundTag tTag = tag(aStack);
		//? if forge {
		return tTag != null && tTag.getBoolean(NBT_MOD);
		//?} else {
		/*return tTag.getBoolean(NBT_MOD);
		 *///?}
	}

	/** The latched half of the pair (Nuclear.java:55). */
	public static boolean moderatedO(ItemStack aStack) {
		if (aStack.isEmpty()) return false;
		CompoundTag tTag = tag(aStack);
		//? if forge {
		return tTag != null && tTag.getBoolean(NBT_MOD_O);
		//?} else {
		/*return tTag.getBoolean(NBT_MOD_O);
		 *///?}
	}

	/** The pair write (Nuclear.java:63-64). */
	public static void setModerated(ItemStack aStack, boolean aModerated, boolean aModeratedO) {
		if (aStack.isEmpty()) return;
		//? if forge {
		CompoundTag tTag = tagForWrite(aStack);
		tTag.putBoolean(NBT_MOD, aModerated);
		tTag.putBoolean(NBT_MOD_O, aModeratedO);
		//?} else {
		/*CompoundTag tTag = tag(aStack);
		tTag.putBoolean(NBT_MOD, aModerated);
		tTag.putBoolean(NBT_MOD_O, aModeratedO);
		pack(aStack, tTag);
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// the moderator touch counter (Moderator:31 — mModeration / oModeration shorts)
	// ---------------------------------------------------------------------------

	/** The touching-fuel count of this cycle (Moderator.java:37). */
	public static int moderation(ItemStack aStack) {
		if (aStack.isEmpty()) return 0;
		CompoundTag tTag = tag(aStack);
		//? if forge {
		return tTag != null && tTag.contains(NBT_MOD, Tag.TAG_ANY_NUMERIC) ? tTag.getShort(NBT_MOD) : 0;
		//?} else {
		/*return tTag.contains(NBT_MOD, Tag.TAG_ANY_NUMERIC) ? tTag.getShort(NBT_MOD) : 0;
		 *///?}
	}

	/** The latched reflection multiplier of the current cycle (Moderator.java:38). */
	public static int moderationO(ItemStack aStack) {
		if (aStack.isEmpty()) return 0;
		CompoundTag tTag = tag(aStack);
		//? if forge {
		return tTag != null && tTag.contains(NBT_MOD_O, Tag.TAG_ANY_NUMERIC) ? tTag.getShort(NBT_MOD_O) : 0;
		//?} else {
		/*return tTag.contains(NBT_MOD_O, Tag.TAG_ANY_NUMERIC) ? tTag.getShort(NBT_MOD_O) : 0;
		 *///?}
	}

	/** The counter pair write (Moderator writeItemNBT2 :60-64). */
	public static void setModeration(ItemStack aStack, int aModeration, int aModerationO) {
		if (aStack.isEmpty()) return;
		//? if forge {
		CompoundTag tTag = tagForWrite(aStack);
		tTag.putShort(NBT_MOD, (short) aModeration);
		tTag.putShort(NBT_MOD_O, (short) aModerationO);
		//?} else {
		/*CompoundTag tTag = tag(aStack);
		tTag.putShort(NBT_MOD, (short) aModeration);
		tTag.putShort(NBT_MOD_O, (short) aModerationO);
		pack(aStack, tTag);
		 *///?}
	}

	/** The NBT clear of the in-place depletion/breeding swap (Nuclear.java:222 / Breeder.java:88 — {@code ST.nbt(aStack, null)}). */
	public static void clear(ItemStack aStack) {
		//? if forge {
		aStack.setTag(null);
		//?} else {
		/*aStack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		 *///?}
	}
}
