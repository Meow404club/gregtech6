/**
 * Task anvil-shape-cull-rename offline pins (the GT6HopperNoOcclusionTest / GT6AnvilShapePinTest
 * precedent forms). Two symptoms, one block family:
 * <ul>
 * <li>the collision box was the default FULL CUBE while the six-box model
 *     (anvil-shape-models) renders a mostly-empty volume — the pin transcribes the
 *     upstream collision/selection box (MultiTileEntityAnvil.java:397-398, ONE box for
 *     both) into the two cached VoxelShapes: the long axis runs PERPENDICULAR to the
 *     facing (the SIDES_AXIS_X[mFacing] leg picks the waist axis), the facing axis
 *     carries the PX_P[4]..PX_N[4] = 4..12px waist, the cross axis the PX_P[0]..PX_N[0]
 *     = 0..16px full band, y = PX_P[0]..PX_N[4] = 0..12px;</li>
 * <li>the family registered the DEFAULT canOcclude = true, so getOcclusionShape culled
 *     the neighbor faces against the model's empty space (the issue #9 wire/hopper X-ray
 *     family) — the pin walks all 35 rows through the EXACT registration payload (the
 *     {@link GT6Anvils#properties} seam — the static walk's single construction site) and
 *     asserts the block state can never occlude again.</li>
 * </ul>
 *
 * <p>Offline block construction needs the vanilla block registry unfrozen (the
 * GT6HopperNoOcclusionTest bracket verbatim); the BET supplier is a stand-in — the shape
 * and occlusion faces never resolve it.
 */
package gregtech6.block.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Anvils.AnvilRow;

public class GT6AnvilBlockShapePinTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the block registry is usable by now
			// (the GT6HopperNoOcclusionTest bracket).
		}
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** The EXACT registration payload (the GT6Anvils.properties seam — the BET stand-in never resolves). */
	private static GTAnvilBlock anvil(AnvilRow aRow) {
		return new GTAnvilBlock(aRow.material(), aRow.durability(), () -> null, GT6Anvils.properties(aRow));
	}

	@Test
	public void theShapeIsTheUpstreamBoxPerFacingAxis() {
		// the stone row carries the family geometry (all 35 rows share the block class)
		GTAnvilBlock tAnvil = anvil(GT6Anvils.ROWS.get(0));
		// the X-axis facings (EAST/WEST — the SIDES_AXIS_X[mFacing] leg): x waist 4..12px, z full band 0..16px
		assertBox(tAnvil.getShape(tAnvil.defaultBlockState().setValue(GTAnvilBlock.FACING, Direction.EAST), null, null, null),
				4, 12, 0, 16);
		// the Z-axis facings (NORTH/SOUTH): the X/Z legs swap
		assertBox(tAnvil.getShape(tAnvil.defaultBlockState().setValue(GTAnvilBlock.FACING, Direction.NORTH), null, null, null),
				0, 16, 4, 12);
	}

	@Test
	public void collisionAndSelectionShareTheOneUpstreamBox() {
		// :397 getCollisionBoundingBoxFromPool == :398 getSelectedBoundingBoxFromPool (the same cached form)
		GTAnvilBlock tAnvil = anvil(GT6Anvils.ROWS.get(0));
		BlockState tState = tAnvil.defaultBlockState().setValue(GTAnvilBlock.FACING, Direction.WEST);
		assertSame(tAnvil.getShape(tState, null, null, null), tAnvil.getCollisionShape(tState, null, null, null));
	}

	/** The six-value box pin (VoxelShape min/max are 0..1 block units; the px/16 values are exact dyadics). */
	private static void assertBox(VoxelShape aShape, int aXMinPx, int aXMaxPx, int aZMinPx, int aZMaxPx) {
		assertEquals(0.0, aShape.min(Direction.Axis.Y), 0.0, "y min = PX_P[0] = 0px");
		assertEquals(12.0 / 16, aShape.max(Direction.Axis.Y), 0.0, "y max = PX_N[4] = 12px");
		assertEquals(aXMinPx / 16.0, aShape.min(Direction.Axis.X), 0.0, "x min px");
		assertEquals(aXMaxPx / 16.0, aShape.max(Direction.Axis.X), 0.0, "x max px");
		assertEquals(aZMinPx / 16.0, aShape.min(Direction.Axis.Z), 0.0, "z min px");
		assertEquals(aZMaxPx / 16.0, aShape.max(Direction.Axis.Z), 0.0, "z max px");
	}

	@Test
	public void everyAnvilRowRegistersNoOcclusion() {
		assertEquals(35, GT6Anvils.ROWS.size(), "the anvil row census drifted (the :2185-2219 ladder)");
		for (AnvilRow tRow : GT6Anvils.ROWS) {
			assertFalse(anvil(tRow).defaultBlockState().canOcclude(),
					tRow.path() + " must register .noOcclusion() (anvil-shape-cull-rename: the six-box model over the "
							+ "full-cube occlusion culls neighbor faces = the issue #9 X-ray family)");
		}
	}
}
