/**
 * The kitchen shape + occlusion + material census (task p38-c3-kitchen-tint-shape): the
 * kitchen family rendered sub-cube hollow-tub element models (GT6BlockStates.addKitchen)
 * over the DEFAULT full-cube shape and bare properties — the #1 oversized selection box
 * and the #9 occlusion X-ray compound (canOcclude defaults true, BlockBehaviour.java:911;
 * getOcclusionShape = getShape, :240-242 — neighbour faces culled against the empty
 * cavity). The pin works over the per-file property SEAM
 * ({@code GT6Kitchen.kitchenProperties} — the {@code GTWires.wireProperties} seam form):
 * each census block replays the EXACT registration payload, so asserting it pins the
 * whole family, and the {@code GT6Kitchen.BLOCKS} register census (populated by the
 * register() calls before any registry event fires, the GTNoOcclusionCensusTest bracket)
 * forces a look on drift and keeps new rows on the seam.
 *
 * <p>The shapes pin the upstream collision-pool rows verbatim (the port's getShape override
 * serves selection AND collision — vanilla getCollisionShape delegates to getShape,
 * BlockBehaviour.java:290, exactly the upstream getCollisionBoundingBoxFromPool ==
 * getSelectedBoundingBoxFromPool form): pot/bowl (0,0,0)-(16,8,16)
 * (MultiTileEntityBathingPot.java:406 / MultiTileEntityMixingBowl.java:427), juicer
 * (2,0,2)-(14,4,14) (MultiTileEntityJuicer.java:286).
 *
 * <p>Offline block construction needs the vanilla block registry unfrozen and the material
 * registry booted (the GTNoOcclusionCensusTest bracket).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.block.tools.GTKitchenBlock;

public class GTKitchenBlockCensusTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the block registry is usable by now
			// (the GTNoOcclusionCensusTest / GTWireContactDamageTest bracket).
		}
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		gregtech6.registry.GTMaterialItems.initMaterials(); // the carrier rows dereference materials lazily
	}

	/** The EXACT registration payload replay (the #9 logistics makeWireBlock seam-replay form). */
	private static GTKitchenBlock kitchen(long aCapacityL, java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial,
			net.minecraft.world.phys.shapes.VoxelShape aShape, SoundType aSound, float aResistance) {
		return new GTKitchenBlock(aCapacityL, aMaterial, aShape, () -> null,
				GT6Kitchen.kitchenProperties(aSound, aResistance));
	}

	/** Per-axis bounds of the block's placement/selection shape. */
	private static void assertBounds(String aLabel, BlockState aState, double aMinX, double aMinY, double aMinZ,
			double aMaxX, double aMaxY, double aMaxZ) {
		var tShape = aState.getShape(null, null);
		assertEquals(aMinX, tShape.min(Direction.Axis.X), aLabel + " minX");
		assertEquals(aMinY, tShape.min(Direction.Axis.Y), aLabel + " minY");
		assertEquals(aMinZ, tShape.min(Direction.Axis.Z), aLabel + " minZ");
		assertEquals(aMaxX, tShape.max(Direction.Axis.X), aLabel + " maxX");
		assertEquals(aMaxY, tShape.max(Direction.Axis.Y), aLabel + " maxY (the wall height — the #1 selection-box fix)");
		assertEquals(aMaxZ, tShape.max(Direction.Axis.Z), aLabel + " maxZ");
	}

	/**
	 * The vessel shapes are the upstream collision-pool boxes (NOT the full cube) and the
	 * properties chain carries .noOcclusion().isViewBlocking(never) — the two-symptom fix
	 * (oversized selection box + neighbour-face culling X-ray) pinned per family row.
	 */
	@Test
	public void kitchenBlocksAreSubCubeAndNeverOcclude() {
		for (GTKitchenBlock tTub : new GTKitchenBlock[] {
				kitchen(4000, () -> MT.WoodTreated, GTKitchenBlock.SHAPE_TUB, SoundType.WOOD, 5.0F),
				kitchen(8000, () -> MT.StainlessSteel, GTKitchenBlock.SHAPE_TUB, SoundType.METAL, 6.0F),
				kitchen(8000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_TUB, SoundType.STONE, 5.0F)}) {
			BlockState tState = tTub.defaultBlockState();
			assertBounds(tTub + " (tub)", tState, 0, 0, 0, 1, 0.5, 1);
			assertFalse(tState.canOcclude(), tTub + " must register .noOcclusion() (the #9 X-ray over the hollow tub)");
			assertFalse(tState.isViewBlocking(null, null), tTub + " must never block the view (the fog-only rider)");
		}
		BlockState tJuicer = kitchen(1000000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_JUICER, SoundType.STONE, 5.0F)
				.defaultBlockState();
		assertBounds("juicer", tJuicer, 0.125, 0, 0.125, 0.875, 0.25, 0.875);
		assertFalse(tJuicer.canOcclude(), "the juicer must register .noOcclusion()");
		assertFalse(tJuicer.isViewBlocking(null, null), "the juicer must never block the view");
	}

	/** The collision shape rides the same sub-cube box (the vanilla getShape delegation — the upstream pool-box equality). */
	@Test
	public void collisionShapeMatchesTheVesselShape() {
		for (GTKitchenBlock tBlock : new GTKitchenBlock[] {
				kitchen(4000, () -> MT.WoodTreated, GTKitchenBlock.SHAPE_TUB, SoundType.WOOD, 5.0F),
				kitchen(1000000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_JUICER, SoundType.STONE, 5.0F)}) {
			BlockState tState = tBlock.defaultBlockState();
			assertEquals(tState.getShape(null, null).max(Direction.Axis.Y),
					tState.getCollisionShape(null, null).max(Direction.Axis.Y),
					tBlock + ": collision height = selection height (the upstream pool-box form)");
		}
	}

	/**
	 * The tint material dispatch: the carrier rows resolve their NBT_MATERIAL column (the
	 * upstream mRGBa source — the #7 javadoc row reading), non-kitchen blocks stay
	 * material-less (the white identity).
	 */
	@Test
	public void kitchenMaterialDispatchResolvesTheCarrierRows() {
		assertSame(MT.WoodTreated, kitchen(4000, () -> MT.WoodTreated, GTKitchenBlock.SHAPE_TUB, SoundType.WOOD, 5.0F).material(),
				"the wood pot tints the WoodTreated carrier colour (the melt-door representative row)");
		assertSame(MT.StainlessSteel, kitchen(8000, () -> MT.StainlessSteel, GTKitchenBlock.SHAPE_TUB, SoundType.METAL, 6.0F).material(),
				"the steel pot tints the StainlessSteel row colour");
		assertSame(MT.Ceramic, kitchen(8000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_TUB, SoundType.STONE, 5.0F).material(),
				"the bowl tints the Ceramic row colour");
		assertSame(MT.Ceramic, kitchen(1000000, () -> MT.Ceramic, GTKitchenBlock.SHAPE_JUICER, SoundType.STONE, 5.0F).material(),
				"the juicer tints the Ceramic row colour");
		assertNull(GTKitchenBlock.materialOf(Blocks.STONE), "a non-kitchen block is material-less (the domain gate)");
		assertNull(GTKitchenBlock.materialOf(null), "a null block is material-less");
	}

	/** The register census: 4 kitchen blocks — a drift forces a look and a seam reuse. */
	@Test
	public void kitchenRegisterCensusDoesNotDrift() {
		assertEquals(4, GT6Kitchen.BLOCKS.getEntries().size(), "the kitchen register census drifted");
	}
}
