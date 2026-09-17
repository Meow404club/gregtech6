package gregtech6.itemdata;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * The project-level keyed item-data access seam (task p31-identity-seam) —
 * {@code get}/{@code set(stack, KEY, ...)} over a typed key registry
 * ({@link GT6DataKey}), named after the {@code GT6DualDirectoryFaces} seam
 * family (the phase_anchors.p30 ruling). ONE storage contract, TWO carriers,
 * sealed inside this single fork face (ADR-P17-1: no second-path producers):
 * <ul>
 * <li><b>1.20.1 (forge leg)</b> — the payload rides the ItemStack root NBT
 * under the key's verbatim NBT name (for the first key the upstream
 * {@code "GT.ToolStats"} compound), so it syncs to the client inside the stack
 * — that IS the client-visibility design on this leg.</li>
 * <li><b>1.21.1 (neoforge leg)</b> — the same logical key is a registered
 * {@code DataComponentType} built from the SAME per-key codec
 * ({@code persistent} for disk, {@code ByteBufCodecs.fromCodec} for the
 * network-synchronized half — the client-visibility design on this leg: the
 * inventory client resolves the payload through the component stream codec,
 * no extra sync channel).</li>
 * </ul>
 *
 * <p>Guardrails (the user ruling recorded in research.p31-metatool-ladder):
 * <ol>
 * <li>the metatool lands as the first consumer — {@link GT6ToolStats#KEY},
 * consumed by the crowbar material identity (GT6Tools CROWBAR row);</li>
 * <li><b>raw-tag passthrough</b> — payloads that must stay verbatim (battery
 * Base08, bucket keepFilter, still payloads) do NOT migrate into keys; the
 * {@link #rawTag}/{@link #updateRaw} pair is the raw CompoundTag bypass (on
 * 1.21.1 the vanilla {@code CUSTOM_DATA} envelope, the same opaque-compound
 * carrier the GT6DataComponents shim uses for its per-domain payloads); keyed
 * writes never touch the raw payload (pinned by GT6ItemDataTest);</li>
 * <li><b>fail-visible</b> — a missing key is an explicit miss
 * ({@link #find} = empty / {@link #get(stack,key)} throws
 * {@link NoSuchElementException}); a present-but-undecodable payload throws on
 * both legs (1.20.1: the codec parse error; 1.21.1: the DC load fail-fast).
 * The seam itself NEVER fabricates a default; the three-arg
 * {@link #get(ItemStack, GT6DataKey, Object)} is the CALLER declaring its own
 * fallback at the call site (the upstream
 * {@code getPrimaryMaterial(stack, MT.Steel)} shape).</li>
 * </ol>
 */
//? if neoforge {
@Mod.EventBusSubscriber(modid = "gt6")
//?}
public final class GT6ItemData {

	private GT6ItemData() {}

	// ---------------------------------------------------------------- read

	/** The canonical read: present → the value; absent → {@link NoSuchElementException} (fail-visible). */
	public static <T> T get(ItemStack aStack, GT6DataKey<T> aKey) {
		T tValue = read(aStack, aKey);
		if (tValue == null) throw new NoSuchElementException(
				"GT6ItemData key \"" + aKey.nbtName() + "\" is not present on " + aStack.getItem());
		return tValue;
	}

	/** The explicit-missing read: present → filled {@link Optional}; absent → empty. */
	public static <T> Optional<T> find(ItemStack aStack, GT6DataKey<T> aKey) {
		return Optional.ofNullable(read(aStack, aKey));
	}

	/** The caller-declared fallback read (the upstream {@code get(stack, aDefault)} shape) — the seam never invents the default. */
	public static <T> T get(ItemStack aStack, GT6DataKey<T> aKey, T aFallback) {
		T tValue = read(aStack, aKey);
		return tValue == null ? aFallback : tValue;
	}

	// --------------------------------------------------------------- write

	/** Writes the payload through the key's codec. Keyed writes never touch the raw payload (guardrail ②). */
	public static <T> void set(ItemStack aStack, GT6DataKey<T> aKey, T aValue) {
		//? if forge {
		aStack.getOrCreateTag().put(aKey.nbtName(), aKey.write(aValue));
		//?} else {
		/*aStack.set(componentType(aKey), aValue);
		*///?}
	}

	@Nullable
	private static <T> T read(ItemStack aStack, GT6DataKey<T> aKey) {
		//? if forge {
		CompoundTag tRoot = aStack.getTag();
		if (tRoot == null || !tRoot.contains(aKey.nbtName())) return null;
		return aKey.read(tRoot.get(aKey.nbtName())); // decode errors throw — the DC fail-fast parity (guardrail ③)
		//?} else {
		/*return aStack.get(componentType(aKey)); // in-memory components cannot be corrupt — disk/sync decode is the DC fail-fast
		*///?}
	}

	// ------------------------------------------------- the raw-tag bypass (guardrail ②)

	/**
	 * A defensive COPY of the raw payload carrier, or {@code null} when the stack
	 * carries none: the 1.20.1 root tag / the 1.21.1 {@code CUSTOM_DATA} envelope.
	 * Verbatim by contract — battery Base08, bucket keepFilter, still payloads read
	 * here without any key machinery. Writes go through {@link #updateRaw}.
	 */
	@Nullable
	public static CompoundTag rawTag(ItemStack aStack) {
		//? if forge {
		CompoundTag tRoot = aStack.getTag();
		return tRoot == null ? null : tRoot.copy();
		//?} else {
		/*return aStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); // empty compound when absent (the GTItemPaintTint read fork shape)
		*///?}
	}

	/** The raw-bypass write: edits the live payload in place, keyed data untouched on both legs. */
	public static void updateRaw(ItemStack aStack, Consumer<CompoundTag> aEditor) {
		//? if forge {
		aEditor.accept(aStack.getOrCreateTag());
		//?} else {
		/*net.minecraft.world.item.component.CustomData.update(
				net.minecraft.core.component.DataComponents.CUSTOM_DATA, aStack, aEditor);
		*///?}
	}

	//? if neoforge {
	/*// ---- the 1.21.1 carrier: one DataComponentType per registered key, built from the shared codec ----

	private static final Map<GT6DataKey<?>, net.minecraft.core.component.DataComponentType<?>> sComponentTypes = new HashMap<>();

	static {
		// THE KEY-HOLDER MANIFEST (S31-1 review): direct field references force <clinit>
		// of every key-holding class HERE, at GT6ItemData's own construction — which the
		// @EventBusSubscriber scan runs at mod construct, BEFORE any consumer ever touched
		// a key holder. Each GT6DataKey ctor eagerly builds its DataComponentType through
		// {@link #onKeyCreated}, so this list is the load-bearing registration surface:
		// every future key-holding class (dig/blade ladder, the hives) MUST be added here,
		// a key created after the RegisterEvent would carry an unregistered component.
		// (Same-thread recursive init is JVM-legal: the ctor's onKeyCreated call re-enters
		// this in-progress <clinit> and finds sComponentTypes already assigned above.)
		Object tKeyManifest = GT6ToolStats.KEY;
	}

	private static <T> net.minecraft.core.component.DataComponentType<T> componentTypeOf(GT6DataKey<T> aKey) {
		return net.minecraft.core.component.DataComponentType.<T>builder()
				.persistent(aKey.codec())
				.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(aKey.codec()))
				.build();
	}

	@SuppressWarnings("unchecked")
	private static <T> net.minecraft.core.component.DataComponentType<T> componentType(GT6DataKey<T> aKey) {
		net.minecraft.core.component.DataComponentType<?> tType = sComponentTypes.get(aKey);
		if (tType == null) throw new IllegalStateException("GT6ItemData key \"" + aKey.nbtName()
				+ "\" has no DataComponentType — its holder class is missing from the GT6ItemData manifest");
		return (net.minecraft.core.component.DataComponentType<T>) tType;
	}

	@SubscribeEvent
	public static void onRegister(RegisterEvent aEvent) {
		// live iteration at event time — never a snapshot (S31-1)
		for (GT6DataKey<?> tKey : GT6DataKey.registry().values()) {
			aEvent.register(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tKey.path()),
					() -> componentType(tKey));
		}
	}
	*///?}

	/**
	 * The eager per-key carrier hook, fired from the {@link GT6DataKey} constructor.
	 * Forge: a no-op — the root tag needs no carrier object. Neoforge: eagerly builds
	 * and maps the key's {@code DataComponentType} at key-creation time, so the map can
	 * never be a stale snapshot (S31-1) and the manifest above only has to guarantee
	 * the class LOAD order, not the object graph.
	 */
	static <T> void onKeyCreated(GT6DataKey<T> aKey) {
		//? if neoforge {
		/*sComponentTypes.put(aKey, componentTypeOf(aKey));
		*///?}
	}
}
