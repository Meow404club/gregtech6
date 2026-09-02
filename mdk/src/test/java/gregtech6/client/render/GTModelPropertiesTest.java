package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import org.junit.jupiter.api.Test;

/**
 * The GTModelProperties snapshot builder: immutability/thread-safety enforcement
 * (ModelData.java:28 iron law) and ModelProperty type safety. ModelData/ModelProperty are
 * pure data classes, so this runs fully offline.
 */
public class GTModelPropertiesTest extends GTOfflineRenderTestBase {

	/** Fixture snapshot — the record shape the GTRenderSnapshot contract prescribes. */
	private record TestSnapshot(String mName) implements GTRenderSnapshot {
	}

	private static final ModelProperty<Integer> NON_NEGATIVE = new ModelProperty<>(v -> v != null && v >= 0);

	@Test
	public void snapshotIsImmutableAndDefensive() {
		Map<Direction, String> tMutableFaces = new HashMap<>();
		tMutableFaces.put(Direction.NORTH, "cover:a");

		ModelProperty<Map<Direction, String>> tFacesProp = new ModelProperty<>();
		ModelData tData = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new TestSnapshot("machine"))
				.with(tFacesProp, tMutableFaces)
				.build();

		// The producer's own collection freezes into an unmodifiable copy...
		Map<Direction, String> tFrozenFaces = tData.get(tFacesProp);
		assertThrows(UnsupportedOperationException.class, () -> tFrozenFaces.put(Direction.SOUTH, "x"));

		// ...and mutating the source afterwards must not leak into the snapshot.
		tMutableFaces.put(Direction.SOUTH, "cover:b");
		assertEquals(Map.of(Direction.NORTH, "cover:a"), tFrozenFaces);

		// derive() carries the frozen parent entries into a new snapshot.
		ModelData tDerived = GTModelProperties.derive(tData).build();
		assertSame(tFrozenFaces, tDerived.get(tFacesProp));
		assertSame("machine", ((TestSnapshot) tDerived.get(GTModelProperties.RENDER_SNAPSHOT)).mName());
	}

	@Test
	public void collectionValuesAreFrozenIntoUnmodifiableCopies() {
		Map<String, String> tMutableMap = new LinkedHashMap<>();
		tMutableMap.put("k", "v");
		List<String> tMutableList = new ArrayList<>(List.of("a"));
		Set<String> tMutableSet = new java.util.HashSet<>(Set.of("s"));

		ModelProperty<Map<String, String>> tMapProp = new ModelProperty<>();
		ModelProperty<List<String>> tListProp = new ModelProperty<>();
		ModelProperty<Set<String>> tSetProp = new ModelProperty<>();

		ModelData tData = GTModelProperties.snapshot()
				.with(tMapProp, tMutableMap)
				.with(tListProp, tMutableList)
				.with(tSetProp, tMutableSet)
				.build();

		Map<String, String> tFrozenMap = tData.get(tMapProp);
		List<String> tFrozenList = tData.get(tListProp);
		Set<String> tFrozenSet = tData.get(tSetProp);

		assertThrows(UnsupportedOperationException.class, () -> tFrozenMap.put("k2", "v2"));
		assertThrows(UnsupportedOperationException.class, () -> tFrozenList.add("b"));
		assertThrows(UnsupportedOperationException.class, () -> tFrozenSet.add("s2"));

		// Defensive: source mutation after build leaves the snapshot untouched.
		tMutableMap.put("late", "x");
		tMutableList.add("late");
		assertEquals(Map.of("k", "v"), tFrozenMap);
		assertEquals(List.of("a"), tFrozenList);
		assertEquals(Set.of("s"), tFrozenSet);
	}

	@Test
	public void arrayValuesAreRejected() {
		ModelProperty<int[]> tProp = new ModelProperty<>();
		assertThrows(IllegalArgumentException.class,
				() -> GTModelProperties.snapshot().with(tProp, new int[] {1, 2, 3}));
		ModelProperty<String[]> tProp2 = new ModelProperty<>();
		assertThrows(IllegalArgumentException.class,
				() -> GTModelProperties.snapshot().with(tProp2, new String[] {"x"}));
	}

	@Test
	public void propertyPredicatesGateTypeSafety() {
		// RENDER_SNAPSHOT is non-null gated: a null value fails the type gate...
		assertThrows(IllegalStateException.class,
				() -> GTModelProperties.snapshot().with(GTModelProperties.RENDER_SNAPSHOT, null));
		// ...and a custom predicate rejects out-of-range values.
		assertThrows(IllegalStateException.class, () -> GTModelProperties.snapshot().with(NON_NEGATIVE, -1));

		// Valid values survive the gates and round-trip type-safely.
		TestSnapshot tSnapshot = new TestSnapshot("oven");
		ModelData tData = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, tSnapshot)
				.with(NON_NEGATIVE, 5)
				.build();

		assertTrue(tData.has(GTModelProperties.RENDER_SNAPSHOT));
		assertSame(tSnapshot, tData.get(GTModelProperties.RENDER_SNAPSHOT));
		assertEquals(5, tData.get(NON_NEGATIVE));
		assertFalse(tData.has(new ModelProperty<Integer>()), "properties are identity-keyed");
	}

	@Test
	public void builderIsSingleUse() {
		GTModelProperties.SnapshotBuilder tBuilder = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new TestSnapshot("one"));
		ModelData tFirst = tBuilder.build();

		// Forge's ModelData.Builder wraps its live map (ModelData.java:96), so a second
		// build would alias the first snapshot; the GT builder refuses instead.
		assertThrows(IllegalStateException.class, tBuilder::build);
		assertThrows(IllegalStateException.class,
				() -> tBuilder.with(GTModelProperties.RENDER_SNAPSHOT, new TestSnapshot("two")));
		assertTrue(tFirst.has(GTModelProperties.RENDER_SNAPSHOT));
	}

	@Test
	public void deriveCarriesParentEntries() {
		ModelData tParent = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new TestSnapshot("parent"))
				.build();

		ModelData tChild = GTModelProperties.derive(tParent).with(NON_NEGATIVE, 1).build();

		assertSame(tParent.get(GTModelProperties.RENDER_SNAPSHOT), tChild.get(GTModelProperties.RENDER_SNAPSHOT));
		assertEquals(1, tChild.get(NON_NEGATIVE));
		// and the parent snapshot stays untouched by the child build.
		assertFalse(tParent.has(NON_NEGATIVE));
	}

	@Test
	public void emptySnapshotMatchesForgeEmpty() {
		// ModelData carries no equals() (identity semantics), so emptiness is structural.
		assertTrue(GTModelProperties.snapshot().build().getProperties().isEmpty());
		assertTrue(ModelData.EMPTY.getProperties().isEmpty());
	}

	@Test
	public void blockPosPropertyCarriesPositionSemantics() {
		// Position-carrying snapshots (GTCEu GTModelProperties.java:15 POS counterpart, but frozen
		// into the snapshot instead of reaching for the live level): BlockPos is immutable.
		ModelProperty<BlockPos> tPosProp = new ModelProperty<>();
		ModelData tData = GTModelProperties.snapshot().with(tPosProp, BlockPos.ZERO).build();
		assertEquals(BlockPos.ZERO, tData.get(tPosProp));
	}
}
