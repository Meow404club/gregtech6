package gregtech6.client.render;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;

/**
 * The panel of {@link ModelProperty ModelProperties} of the GT6 render domain plus the
 * immutable {@link ModelData} snapshot builder — the C-grade render foundation consumed
 * by the W3 cover card (p4-cover-core, plate quads) and the pooled D-grade work
 * (multiblock formed-state visuals, dynamic machine rendering).
 *
 * <p>Iron law (ModelData.java:28): every value stored in {@link ModelData} must be
 * immutable or thread-safe, because the render thread reads it concurrently with the
 * client tick thread. {@link #snapshot()} / {@link #derive(ModelData)} return a builder
 * that enforces the law at the boundary — collection values are frozen into
 * {@code Map.copyOf}/{@code List.copyOf}/{@code Set.copyOf} copies and array values are
 * rejected outright. Non-collection values must be immutable by contract (e.g. a
 * {@link GTRenderSnapshot} record); the freeze is shallow, so nested collections inside
 * custom value objects are the producer's responsibility.
 *
 * <p>Dispatch key: {@link #RENDER_SNAPSHOT}. A BE participating in dynamic rendering
 * stores its (immutable) snapshot under it from {@code getModelData()}; the
 * {@link GTDynamicBakedModel} skeleton keys the dynamic/fallback dispatch on its
 * presence.
 */
public final class GTModelProperties {

	/**
	 * The GT6 dynamic-render dispatch key: the per-block immutable render snapshot
	 * ({@link GTRenderSnapshot}) the BE hands to the render thread. Non-null by predicate
	 * — an absent snapshot is expressed by not putting the property at all
	 * ({@link ModelData#EMPTY}).
	 */
	public static final ModelProperty<GTRenderSnapshot> RENDER_SNAPSHOT = new ModelProperty<>(Objects::nonNull);

	/**
	 * The second snapshot property (task p9-render-c-oven-overlay ⑤/coexistence ruling):
	 * the oven state overlay snapshot ({@link GTOvenRenderSnapshot}). RENDER_SNAPSHOT is
	 * a single-valued ModelProperty — the cover chain (p4-cover-core,
	 * TileEntityOvenCoverTest:122-137) already occupies it, so the oven snapshot must ride
	 * its own key instead of overwriting the cover value. A BE may carry both at once
	 * (a covered oven): {@code getModelData()} builds them together, each consumer model
	 * keys on its own property ({@code GTOvenOverlayModel} gates on this one, the cover
	 * plate model on RENDER_SNAPSHOT — the existing cover chain is untouched).
	 */
	public static final ModelProperty<GTOvenRenderSnapshot> OVEN_SNAPSHOT = new ModelProperty<>(Objects::nonNull);

	/**
	 * The machine paint property (task p21-paintable-storage-sync, ADR ruling 5): the
	 * painted 0xRRGGBB colour the paintable BE carries ({@link Integer}, immutable — the
	 * ModelData iron law above applies trivially). Present exactly while the machine is
	 * painted (an unpainted machine keeps the absent-property = no-tint contract, the
	 * {@code ModelData.EMPTY} semantics of the 03 base {@code getModelData()}); the
	 * consumer (the card_B BlockColor tint, {@code tintIndex == 0}) reads the value and
	 * falls back to white 0xFFFFFF when absent. Single-valued like its siblings — a
	 * covered+oven+painted BE carries all three keys on one derived snapshot
	 * (the GTModelProperties:46-55 coexistence ruling shape).
	 */
	public static final ModelProperty<Integer> PAINT = new ModelProperty<>(Objects::nonNull);

	private GTModelProperties() {
	}

	/** Fresh snapshot builder (empty). */
	public static SnapshotBuilder snapshot() {
		return new SnapshotBuilder(null);
	}

	/** Snapshot builder deriving from an existing (immutable) {@link ModelData}. */
	public static SnapshotBuilder derive(ModelData aParent) {
		return new SnapshotBuilder(Objects.requireNonNull(aParent, "parent ModelData"));
	}

	/**
	 * Immutable-snapshot builder over Forge {@link ModelData.Builder}. Single-use: after
	 * {@link #build()} the produced snapshot must never change, so the builder refuses
	 * further {@code with}/{@code build} calls (Forge's own builder would leak later
	 * {@code with} calls into an already-built snapshot through the shared unmodifiable
	 * map wrapper — ModelData.java:96).
	 */
	public static final class SnapshotBuilder {

		@Nullable
		private ModelData.Builder mBuilder;

		private SnapshotBuilder(@Nullable ModelData aParent) {
			mBuilder = aParent == null ? ModelData.builder() : aParent.derive();
		}

		/**
		 * Type gate first ({@link ModelProperty#test} — the property's own predicate, e.g.
		 * {@link #RENDER_SNAPSHOT}'s non-null), then the thread-safety gate
		 * ({@link #freeze(Object)}).
		 */
		public <T> SnapshotBuilder with(ModelProperty<T> aProperty, @Nullable T aValue) {
			Preconditions.checkState(mBuilder != null, "snapshot already built");
			// Forge validates the value against the property predicate inside with()
			// (ModelData.java:88) — the frozen value is what gets validated, so the type
			// gate and the freeze compose.
			mBuilder.with(aProperty, freeze(aValue));
			return this;
		}

		public ModelData build() {
			Preconditions.checkState(mBuilder != null, "snapshot already built");
			ModelData tData = mBuilder.build();
			mBuilder = null;
			return tData;
		}

		/**
		 * The ModelData.java:28 iron law at the boundary: collection values are defensively
		 * frozen (copyOf does not preserve Map iteration order — producers needing order
		 * must store a frozen {@code List.copyOf} instead), array values are rejected
		 * because they can never be made thread-safe, everything else is trusted to be
		 * immutable/thread-safe per the {@link GTRenderSnapshot} contract.
		 */
		private static <T> T freeze(@Nullable T aValue) {
			if (aValue == null) {
				return null;
			}
			if (aValue.getClass().isArray()) {
				throw new IllegalArgumentException(
						"Array values are thread-unsafe in ModelData (ModelData.java:28) — freeze them into a List.copyOf instead: "
								+ aValue.getClass().getSimpleName());
			}
			if (aValue instanceof Map<?, ?> tMap) {
				@SuppressWarnings("unchecked")
				T tFrozen = (T) Map.copyOf(tMap);
				return tFrozen;
			}
			if (aValue instanceof List<?> tList) {
				@SuppressWarnings("unchecked")
				T tFrozen = (T) List.copyOf(tList);
				return tFrozen;
			}
			if (aValue instanceof Set<?> tSet) {
				@SuppressWarnings("unchecked")
				T tFrozen = (T) Set.copyOf(tSet);
				return tFrozen;
			}
			return aValue;
		}
	}
}
