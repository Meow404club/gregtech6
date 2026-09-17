package gregtech6.itemdata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * One registered key of the {@link GT6ItemData} keyed access seam (task
 * p31-identity-seam) — the per-key half of the project-level item data contract,
 * named after the {@code GT6DualDirectoryFaces} seam family.
 *
 * <p>A key is a named typed slot: the NBT compound name the payload rides on the
 * 1.20.1 leg ({@code nbtName()} — for the first key the upstream verbatim
 * {@code "GT.ToolStats"} compound, MultiItemTool.java:192), the registry path the
 * 1.21.1 leg registers its {@code DataComponentType} under ({@code path()}), and
 * the per-key {@code codec()} — the single source of truth both legs serialize
 * through (1.20.1: {@code NbtOps} encode/decode onto the root tag; 1.21.1:
 * {@code persistent(codec)} + {@code ByteBufCodecs.fromCodec(codec)} for the
 * network-synchronized component, the client-visibility half — the client
 * resolves the same codec, so tint-grade data is visible in the inventory
 * without an extra sync channel).
 *
 * <p>Keys self-register into {@link #registry()} at construction; a duplicate
 * name fails loudly (the TagData intern lesson: a typo must never silently
 * mint a second slot). Creation is expected at class-init time only.
 */
public final class GT6DataKey<T> {

	/** The name→key registry — the 键注册表 half of the seam. Insertion-ordered, read-only view via {@link #registry()}. */
	private static final Map<String, GT6DataKey<?>> REGISTRY = new LinkedHashMap<>();

	/** The NBT compound name on the 1.20.1 leg (the upstream verbatim key, e.g. {@code "GT.ToolStats"}). */
	private final String mNbtName;
	/** The registry path under {@code gt6:} for the 1.21.1 DataComponentType (snake case of the same key). */
	private final String mPath;
	/** The per-key codec — both legs serialize through this one instance. */
	private final Codec<T> mCodec;

	public GT6DataKey(String aNbtName, String aPath, Codec<T> aCodec) {
		mNbtName = aNbtName;
		mPath = aPath;
		mCodec = aCodec;
		GT6DataKey<?> tPrior = REGISTRY.get(aNbtName);
		if (tPrior != null) throw new IllegalStateException(
				"GT6ItemData key \"" + aNbtName + "\" registered twice: " + tPrior.path() + " vs " + aPath);
		REGISTRY.put(aNbtName, this);
		// eager carrier construction (S31-1): the key is self-carrying — on the 1.21.1 leg
		// its DataComponentType object exists the moment the key does, so the registration
		// only ever waits on the key-holder CLASS being loaded (the GT6ItemData manifest)
		GT6ItemData.onKeyCreated(this);
	}

	public String nbtName() {
		return mNbtName;
	}

	public String path() {
		return mPath;
	}

	public Codec<T> codec() {
		return mCodec;
	}

	/** Encodes the payload to its stored NBT form (fail-visible: a codec error throws, never a silent partial). */
	public Tag write(T aValue) {
		return failVisible(mCodec.encodeStart(NbtOps.INSTANCE, aValue), "encode " + mNbtName);
	}

	/** Decodes the payload from its stored NBT form (fail-visible: a decode error throws — the DC fail-fast parity). */
	public T read(Tag aTag) {
		return failVisible(mCodec.parse(NbtOps.INSTANCE, aTag), "decode " + mNbtName);
	}

	/**
	 * The one DFU call that exists on BOTH legs (1.20.1 ships DFU 6.0.8, 1.21.1 DFU
	 * 8.0.16 — the no-arg {@code getOrThrow} of DFU 8 does not exist there): the error
	 * message is captured and rethrown as a visible {@link NoSuchElementException}.
	 */
	private <R> R failVisible(DataResult<R> aResult, String aWhat) {
		String[] tMessage = {aWhat};
		return aResult.resultOrPartial(tMsg -> tMessage[0] = tMsg)
				.orElseThrow(() -> new NoSuchElementException(tMessage[0]));
	}

	/** The registry view, keyed by NBT name in registration order (the read-only lookup face). */
	public static Map<String, GT6DataKey<?>> registry() {
		return Collections.unmodifiableMap(REGISTRY);
	}
}
