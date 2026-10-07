package gregtech6.block;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tools.TileEntityFaucet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The faucet three-way geometry pin (task faucet-material-rows spec 2, the
 * GT6ShelfCrateGeometryTest lesson form): the render envelope (the three-pass
 * {@link TileEntityFaucet#MODEL_BOXES} the datagen models build from), the outline
 * ({@link TileEntityFaucet.FaucetBlock#getShape}) and the collision
 * ({@link TileEntityFaucet.FaucetBlock#getCollisionShape}) must assert EQUAL per the
 * id1514 ruling — relative-relation pins have shipped visible mismatches before (the
 * shelf/crate 6/8/10 ladder). The outline values themselves are the upstream verbatim
 * (MultiTileEntityFaucet.java:208-215, north (5,1,0)-(11,6,4)); the collision equality is
 * the declared deviation (upstream 10Attachment:35 rides empty, javadoc on the override).
 *
 * <p>Blocks construct through the offline unfreeze window (the GTRodShapeTest memoized
 * form) — the facing switch is a real override, so the pin drives BlockStates, not just
 * constants.
 */
public class GT6FaucetShapeTest extends GTOfflineTestBase {

	private static void unfreeze() {
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	private static TileEntityFaucet.FaucetBlock faucetBlock() {
		unfreeze();
		return new TileEntityFaucet.FaucetBlock(GT6Molds.FAUCET_ROWS.get(0), () -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	private static VoxelShape shapeOf(TileEntityFaucet.FaucetBlock aBlock, Direction aFacing) {
		return aBlock.getShape(aBlock.defaultBlockState().setValue(gregtech6.block.attachment.GTAttachmentSmallBlock.FACING, aFacing),
				null, (BlockPos) null, CollisionContext.empty());
	}

	private static VoxelShape collisionOf(TileEntityFaucet.FaucetBlock aBlock, Direction aFacing) {
		return aBlock.getCollisionShape(aBlock.defaultBlockState().setValue(gregtech6.block.attachment.GTAttachmentSmallBlock.FACING, aFacing),
				null, (BlockPos) null, CollisionContext.empty());
	}

	private static void assertBox(VoxelShape aShape, double aMinX, double aMinY, double aMinZ,
			double aMaxX, double aMaxY, double aMaxZ) {
		var tBoxes = aShape.toAabbs();
		assertEquals(1, tBoxes.size(), "the shape is one box");
		AABB tBox = tBoxes.get(0);
		assertEquals(aMinX, tBox.minX, 1e-9, "minX");
		assertEquals(aMinY, tBox.minY, 1e-9, "minY");
		assertEquals(aMinZ, tBox.minZ, 1e-9, "minZ");
		assertEquals(aMaxX, tBox.maxX, 1e-9, "maxX");
		assertEquals(aMaxY, tBox.maxY, 1e-9, "maxY");
		assertEquals(aMaxZ, tBox.maxZ, 1e-9, "maxZ");
	}

	/** The upstream selection box verbatim (:208-215): each horizontal its arm, verticals fold to north. 1.20.1 shapes are the 0..1 block units (px/16). */
	@Test
	public void selectionBoxIsTheUpstreamVerbatimPerFacing() {
		TileEntityFaucet.FaucetBlock tBlock = faucetBlock();
		// :210 SIDE_Z_NEG — PX_P[5..N5]/P[1..N10]/P[0..N12] = (5,1,0)-(11,6,4) px
		assertBox(shapeOf(tBlock, Direction.NORTH), 5 / 16.0, 1 / 16.0, 0, 11 / 16.0, 6 / 16.0, 4 / 16.0);
		// :211 default (SIDE_Z_POS) — (5,1,12)-(11,6,16)
		assertBox(shapeOf(tBlock, Direction.SOUTH), 5 / 16.0, 1 / 16.0, 12 / 16.0, 11 / 16.0, 6 / 16.0, 1);
		// :212 SIDE_X_NEG — (0,1,5)-(4,6,11)
		assertBox(shapeOf(tBlock, Direction.WEST), 0, 1 / 16.0, 5 / 16.0, 4 / 16.0, 6 / 16.0, 11 / 16.0);
		// :213 SIDE_X_POS — (12,1,5)-(16,6,11)
		assertBox(shapeOf(tBlock, Direction.EAST), 12 / 16.0, 1 / 16.0, 5 / 16.0, 1, 6 / 16.0, 11 / 16.0);
		// the family-invalid verticals fold to the north arm (the model blockstate form)
		assertBox(shapeOf(tBlock, Direction.DOWN), 5 / 16.0, 1 / 16.0, 0, 11 / 16.0, 6 / 16.0, 4 / 16.0);
		assertBox(shapeOf(tBlock, Direction.UP), 5 / 16.0, 1 / 16.0, 0, 11 / 16.0, 6 / 16.0, 4 / 16.0);
	}

	/** The three-way pin, collision leg: the collision box IS the selection box per facing (id1514). */
	@Test
	public void collisionShapeEqualsTheSelectionShape() {
		TileEntityFaucet.FaucetBlock tBlock = faucetBlock();
		for (Direction tFacing : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
			assertEquals(shapeOf(tBlock, tFacing).toAabbs(), collisionOf(tBlock, tFacing).toAabbs(),
					tFacing + ": the collision box is the selection box (id1514 three-way)");
		}
	}

	/** The three-way pin, render leg: the model envelope IS the north shape box, 12 coordinates. */
	@Test
	public void renderEnvelopeEqualsTheShapeBox() {
		float tMinX = Float.MAX_VALUE, tMinY = Float.MAX_VALUE, tMinZ = Float.MAX_VALUE;
		float tMaxX = -Float.MAX_VALUE, tMaxY = -Float.MAX_VALUE, tMaxZ = -Float.MAX_VALUE;
		for (float[] tBox : TileEntityFaucet.MODEL_BOXES) {
			tMinX = Math.min(tMinX, tBox[0]); tMinY = Math.min(tMinY, tBox[1]); tMinZ = Math.min(tMinZ, tBox[2]);
			tMaxX = Math.max(tMaxX, tBox[3]); tMaxY = Math.max(tMaxY, tBox[4]); tMaxZ = Math.max(tMaxZ, tBox[5]);
		}
		// the envelope of the three-pass stack == the :210 selection box == the collision box
		assertEquals(5.0F, tMinX, 1e-9, "envelope minX");
		assertEquals(1.0F, tMinY, 1e-9, "envelope minY");
		assertEquals(0.0F, tMinZ, 1e-9, "envelope minZ");
		assertEquals(11.0F, tMaxX, 1e-9, "envelope maxX");
		assertEquals(6.0F, tMaxY, 1e-9, "envelope maxY");
		assertEquals(4.0F, tMaxZ, 1e-9, "envelope maxZ");
		TileEntityFaucet.FaucetBlock tBlock = faucetBlock();
		// the envelope in px, normalized to the 0..1 block units the 1.20.1 shapes answer
		assertBox(shapeOf(tBlock, Direction.NORTH), tMinX / 16.0, tMinY / 16.0, tMinZ / 16.0,
				tMaxX / 16.0, tMaxY / 16.0, tMaxZ / 16.0);
		assertBox(collisionOf(tBlock, Direction.NORTH), tMinX / 16.0, tMinY / 16.0, tMinZ / 16.0,
				tMaxX / 16.0, tMaxY / 16.0, tMaxZ / 16.0);
	}
}
