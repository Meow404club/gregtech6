package gregtech6.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * The layer-sequence DSL self-tests (task p16-pattern-layers ①② — the kTFRU
 * {@code ExpandableLayer}/{@code LayerStructure} mechanism re-declared clean-room per
 * ADR 2026-09-05-p16-formation-scoping ②): the {@link GTMultiBlockPattern.Layer} slab
 * vocabulary (part/hollow/formingPart, the same triple as the flat builder), the
 * {@link GTMultiBlockPattern.Builder#layer}/{@link GTMultiBlockPattern.Builder#repeatable}
 * sequence with its Y-stacking-by-order rule, the build-time {@code min..max} expansion
 * (the {@code n} bounds arms), the cross-layer duplicate rejection (the {@code mSeen}
 * precedent spanning the flattened result), and the
 * {@link GTMultiBlockPatternFamily} per-size front ({@code forSize} cached-stable,
 * {@code build} fresh, failures never cached).
 */
public class GTMultiBlockPatternFamilyTest extends GTRecipesOfflineTestBase {

	private static final BlockState T_BRICKS = Blocks.BRICKS.defaultBlockState();
	private static final BlockState T_STONE = Blocks.STONE.defaultBlockState();
	private static final BlockState T_AIR = Blocks.AIR.defaultBlockState();

	/** A three-cell slab for the stacking tests. */
	private static GTMultiBlockPattern.Layer slab(int aMarkerX, int aMarkerZ) {
		return GTMultiBlockPattern.Layer.builder()
				.part(-1, 0, aState -> true)
				.part(aMarkerX, aMarkerZ, aState -> true)
				.part(1, 0, aState -> true)
				.build();
	}

	@Test
	public void layerSequenceStacksByOrderAlongY() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.layer(slab(0, 0))
				.layer(slab(0, 1))
				.layer(slab(0, 2))
				.build();
		List<GTMultiBlockPattern.Cell> tCells = tPattern.cells();
		assertEquals(9, tCells.size(), "three 3-cell slabs");
		// layer 0 -> y 0, layer 1 -> y 1, layer 2 -> y 2 (the Y-stacking-by-order rule);
		// within a layer the in-layer (x, z) footprint is preserved
		for (int tLayer = 0; tLayer < 3; tLayer++) {
			assertEquals(tLayer, tCells.get(tLayer * 3 + 1).y, "layer " + tLayer + " lands on y " + tLayer);
			assertEquals(0, tCells.get(tLayer * 3 + 1).x, "the in-layer x survives");
			assertEquals(tLayer, tCells.get(tLayer * 3 + 1).z, "the in-layer z survives");
			assertFalse(tCells.get(tLayer * 3 + 1).isHollow(), "part() slabs stay parts");
		}
		assertEquals(0, tPattern.minY()); assertEquals(2, tPattern.maxY(), "the bounds fold over the stacked slabs");
	}

	@Test
	public void layerCarriesTheFullCellVocabulary() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder().layer(GTMultiBlockPattern.Layer.builder()
				.part(-1, -1, aState -> true)
				.hollow(0, 0, GTMultiBlockPattern.AIR)
				.formingPart(1, 1, Blocks.BRICKS, -64, 1)
				.build()).build();
		List<GTMultiBlockPattern.Cell> tCells = tPattern.cells();
		assertEquals(3, tCells.size());
		assertFalse(tCells.get(0).isHollow()); assertFalse(tCells.get(0).forms(), "part() stays declaration-only");
		assertTrue(tCells.get(1).isHollow(), "the hollow marker rides the layer");
		assertFalse(tCells.get(1).forms(), "hollow never carries a forming expectation");
		assertTrue(tCells.get(1).matches(T_AIR), "the hollow judgement is AIR");
		assertTrue(tCells.get(2).forms(), "formingPart() is authoritative through the layer path");
		assertSame(Blocks.BRICKS, tCells.get(2).partBlock, "the triple's part block survives");
		assertEquals(-64, tCells.get(2).usage, "the usage mask rides the layer cell verbatim");
		assertEquals(1, tCells.get(2).design, "the design index rides the layer cell");
		assertTrue(tCells.get(2).matches(T_BRICKS)); assertFalse(tCells.get(2).matches(T_STONE));
		// every layer cell lands on y 0 when the layer is the first sequence entry
		assertEquals(0, tCells.get(2).y);
	}

	@Test
	public void repeatableExpandsToTheRequestedCount() {
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder().repeatable(1, 3, tRing -> slab(0, tRing));
		assertEquals(3, tDeclaration.build(1).cells().size(), "n = min");
		assertEquals(9, tDeclaration.build(3).cells().size(), "n = max");
		// the factory index distinguishes the copies (ring i carries its marker at z = i)
		GTMultiBlockPattern.Cell tRingTwoMarker = tDeclaration.build(3).cells().get(7);
		assertEquals(2, tRingTwoMarker.y, "the third copy lands on y 2");
		assertEquals(2, tRingTwoMarker.z, "the factory saw index 2");
	}

	@Test
	public void repeatableWindowIsEnforced() {
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder().repeatable(2, 4, tRing -> slab(0, tRing));
		assertThrows(IllegalArgumentException.class, () -> tDeclaration.build(1), "below min is a declaration bug");
		assertThrows(IllegalArgumentException.class, () -> tDeclaration.build(5), "above max is a declaration bug");
		assertThrows(IllegalArgumentException.class, () -> tDeclaration.build(-1), "a negative count is a bug");
		assertNotNull(tDeclaration.build(2).cells(), "the window edges themselves expand");
		assertNotNull(tDeclaration.build(4).cells());
	}

	@Test
	public void crossLayerDuplicateIsADeclarationBug() {
		// the mSeen precedent spans the WHOLE flattened result — the slab sequence and the
		// flat cells share one offset space, and a slab cell landing on an existing offset
		// (the layer's slots step y by 1, so the collision is at the SAME y) is the same
		// declaration bug the flat builder rejects eagerly
		GTMultiBlockPattern.Builder tFlatFirst = GTMultiBlockPattern.builder()
				.part(0, 0, 0, aState -> true)
				.layer(GTMultiBlockPattern.Layer.builder().part(0, 0, aState -> true).build());
		assertThrows(IllegalArgumentException.class, tFlatFirst::build,
				"a slab cell on an already-declared offset is a bug (flat first)");
		GTMultiBlockPattern.Builder tSlabFirst = GTMultiBlockPattern.builder()
				.layer(GTMultiBlockPattern.Layer.builder().part(0, 0, aState -> true).build())
				.part(0, 0, 0, aState -> true);
		assertThrows(IllegalArgumentException.class, tSlabFirst::build,
				"the same collision with the slab declared first");
		// a repeatable segment colliding with itself across copies keeps its own guard too —
		// identical copies at the SAME slot cannot happen (slots step y), but a factory
		// returning overlapping footprints within ONE layer still does
		GTMultiBlockPattern.Builder tSelf = GTMultiBlockPattern.builder().repeatable(2, 2, tRing -> slab(0, 0));
		assertNotNull(tSelf.build(2), "identical copies land on DIFFERENT y slots — legal, not a collision");
		assertEquals(6, tSelf.build(2).cells().size());
	}

	@Test
	public void layerVersusFlatCellConflictIsRejected() {
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder()
				.part(0, 0, 0, aState -> true)
				.layer(GTMultiBlockPattern.Layer.builder().part(0, 0, aState -> true).build());
		assertThrows(IllegalArgumentException.class, tDeclaration::build,
				"a flat cell and a layer cell on the same offset is the same declaration bug");
	}

	@Test
	public void plainBuildRejectsAnUnexpandedRepeatable() {
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder().repeatable(1, 3, tRing -> slab(0, tRing));
		assertThrows(IllegalStateException.class, tDeclaration::build,
				"the sizeless freeze has no n to expand to");
	}

	@Test
	public void repeatableShiftsItsFollowers() {
		// fixed + repeatable + fixed: the trailing layer's y moves with n
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder()
				.layer(slab(0, 0))
				.repeatable(1, 3, tRing -> slab(0, tRing))
				.layer(slab(9, 9));
		GTMultiBlockPattern tMin = tDeclaration.build(1);
		GTMultiBlockPattern.Cell tMinCap = tMin.cells().stream().filter(tCell -> tCell.x == 9).findFirst().get();
		assertEquals(2, tMinCap.y, "after one repeat the cap sits on y 2");
		GTMultiBlockPattern tMax = tDeclaration.build(3);
		GTMultiBlockPattern.Cell tMaxCap = tMax.cells().stream().filter(tCell -> tCell.x == 9).findFirst().get();
		assertEquals(4, tMaxCap.y, "after three repeats the cap sits on y 4");
	}

	@Test
	public void zeroLayerExpansionIsStillAnEmptyPattern() {
		GTMultiBlockPattern.Builder tDeclaration = GTMultiBlockPattern.builder().repeatable(0, 3, tRing -> slab(0, tRing));
		assertThrows(IllegalStateException.class, () -> tDeclaration.build(0),
				"a window may include zero, but zero cells is still not a pattern");
	}

	@Test
	public void originYReAnchorsTheSlabSequence() {
		// a shape centred on its controller layer: slot 0 sits at y -1
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.originY(-1)
				.layer(slab(0, 0))
				.layer(slab(0, 1))
				.build();
		assertEquals(-1, tPattern.minY()); assertEquals(0, tPattern.maxY(), "the whole sequence shifted by the origin");
		assertEquals(-1, tPattern.cells().get(0).y, "slot 0 on y -1");
		assertEquals(0, tPattern.cells().get(3).y, "slot 1 on y 0");
	}

	@Test
	public void layerAndSequenceValidation() {
		// the layer builder mirrors the flat validation
		assertThrows(IllegalStateException.class, () -> GTMultiBlockPattern.Layer.builder().build(), "an empty slab is a bug");
		GTMultiBlockPattern.Layer.Builder tLayer = GTMultiBlockPattern.Layer.builder().part(1, 1, aState -> true);
		assertThrows(IllegalArgumentException.class, () -> tLayer.part(1, 1, aState -> true), "in-layer duplicates rejected");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.Layer.builder().part(0, 0, null), "a cell needs a predicate");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.Layer.builder().formingPart(0, 0, null, 0, 0), "a forming expectation needs a block");
		// the sequence registration validation
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.builder().layer(null), "a null layer is a bug");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.builder().repeatable(-1, 3, tRing -> slab(0, tRing)), "a negative minimum is a bug");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.builder().repeatable(3, 1, tRing -> slab(0, tRing)), "max < min is a bug");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.builder().repeatable(1, 3, null), "a segment needs its factory");
	}

	@Test
	public void familyForSizeIsCachedStableAndBuildIsFresh() {
		GTMultiBlockPatternFamily tFamily = GTMultiBlockPatternFamily.of(tBuilder -> tBuilder
				.layer(slab(0, 0))
				.repeatable(1, 3, tRing -> slab(0, tRing)));
		GTMultiBlockPattern tFirst = tFamily.forSize(2);
		GTMultiBlockPattern tSecond = tFamily.forSize(2);
		assertSame(tFirst, tSecond, "same n, same immutable instance (the lazy-stable precedent)");
		GTMultiBlockPattern tFresh = tFamily.build(2);
		assertNotSame(tFirst, tFresh, "build() expands fresh");
		assertEquals(tFirst.cells().size(), tFresh.cells().size(), "the fresh expansion is equivalent");
		for (int tCell = 0; tCell < tFirst.cells().size(); tCell++) {
			GTMultiBlockPattern.Cell tA = tFirst.cells().get(tCell);
			GTMultiBlockPattern.Cell tB = tFresh.cells().get(tCell);
			assertEquals(tA.x, tB.x); assertEquals(tA.y, tB.y); assertEquals(tA.z, tB.z);
			assertEquals(tA.isHollow(), tB.isHollow()); assertEquals(tA.forms(), tB.forms());
		}
		// distinct sizes expand distinctly
		assertEquals(6, tFamily.forSize(1).cells().size(), "n = 1");
		assertEquals(12, tFamily.forSize(3).cells().size(), "n = 3");
	}

	@Test
	public void familyFailuresAreNeverCached() {
		GTMultiBlockPatternFamily tFamily = GTMultiBlockPatternFamily.of(tBuilder -> tBuilder
				.repeatable(1, 2, tRing -> slab(0, tRing)));
		assertThrows(IllegalArgumentException.class, () -> tFamily.forSize(9), "out of window");
		assertThrows(IllegalArgumentException.class, () -> tFamily.forSize(9), "the failure left no cache entry — it throws again, freshly");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPatternFamily.of(null), "a family needs its declaration");
	}
}
