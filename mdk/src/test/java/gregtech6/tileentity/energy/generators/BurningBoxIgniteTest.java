package gregtech6.tileentity.energy.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6BurningBoxes;
import gregtech6.registry.GT6BurningBoxes.BurningBoxBlock.FrontArm;
import gregtech6.registry.GT6BurningBoxes.Family;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Issue #11 behavior half — the offline acceptance for the flint fast-ignite and the
 * LIT visual sync seams: the front-arm dispatch truth table (IGNITE on every family
 * front / TRANSFER on the Solid+FluidBed transfer fronts / NONE elsewhere), the 30%
 * strike roll boundary (the GT_Proxy mFlintChance=30 default), the {@code igniteNow}
 * body (the :226/:180 pair — mBurning plus the Liquid-family mCooldown=100, Gas
 * inherited) and the no-fuel burn-out (the :163 arm — a lit bufferless box self-
 * extinguishes on the next tick), with {@code applyVisualState} proven a safe no-op
 * offline (no level → no setBlock; the live LIT face is the RCON chain's).
 *
 * <p>The player-touching glue ({@code igniteWithFlint} — durability + the hit-side
 * ignite sound) has no offline Player fixture seat (the GT6MachineFluidDisplayTest
 * ruling): the RCON burning_box_family chain drives that face.
 */
public class BurningBoxIgniteTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);

	static BlockEntityType<FixtureSolidBox> sSolidType;
	static BlockEntityType<GTGeneratorLiquidBlockEntity> sLiquidType;
	static BlockEntityType<GTGeneratorGasBlockEntity> sGasType;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<FixtureSolidBox>[] tSolid = (BlockEntityType<FixtureSolidBox>[]) new BlockEntityType<?>[1];
		tSolid[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new FixtureSolidBox(aPos, aState), Blocks.STONE).build(null);
		sSolidType = tSolid[0];
		BlockEntityType<GTGeneratorLiquidBlockEntity>[] tLiquid = (BlockEntityType<GTGeneratorLiquidBlockEntity>[]) new BlockEntityType<?>[1];
		tLiquid[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTGeneratorLiquidBlockEntity(tLiquid[0], aPos, aState), Blocks.STONE).build(null);
		sLiquidType = tLiquid[0];
		BlockEntityType<GTGeneratorGasBlockEntity>[] tGas = (BlockEntityType<GTGeneratorGasBlockEntity>[]) new BlockEntityType<?>[1];
		tGas[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTGeneratorGasBlockEntity(tGas[0], aPos, aState), Blocks.STONE).build(null);
		sGasType = tGas[0];
		gregtech6.recipes.GT6RecipeMaps.init(); // the live FURNACE_FUEL/BURN instances (the shared append face)
	}

	/** The concrete solid-family test BE (the GTGeneratorSolidBlockEntityTest.FixtureBox form). */
	public static final class FixtureSolidBox extends GTGeneratorSolidBlockEntity {
		public FixtureSolidBox(BlockPos aPos, BlockState aState) {
			super(sSolidType, aPos, aState);
		}
	}

	/** The front-arm truth table — the pure dispatch the use() branch consumes. */
	@Test
	public void theFrontArmDispatchCoversTheThreeStates() {
		// a flint-like held stack takes the IGNITE arm on EVERY family front (the
		// upstream TOOL_igniter gate covers Solid←Brick/Metal :226, Liquid←Gas :180,
		// FluidBed :202)
		for (Family tFamily : Family.values()) {
			assertEquals(FrontArm.IGNITE, GT6BurningBoxes.BurningBoxBlock.frontArmOf(tFamily, true),
					tFamily + " front must strike the flint");
		}
		// the transfer click stays the Solid/FluidBed-only face (onBlockActivated3 :216)
		assertEquals(FrontArm.TRANSFER, GT6BurningBoxes.BurningBoxBlock.frontArmOf(Family.SOLID, false));
		assertEquals(FrontArm.TRANSFER, GT6BurningBoxes.BurningBoxBlock.frontArmOf(Family.FLUIDBED, false));
		// the Liquid/Gas fronts with anything else stay inert (the pre-issue11 shape)
		assertEquals(FrontArm.NONE, GT6BurningBoxes.BurningBoxBlock.frontArmOf(Family.LIQUID, false));
		assertEquals(FrontArm.NONE, GT6BurningBoxes.BurningBoxBlock.frontArmOf(Family.GAS, false));
	}

	/** The 30% strike boundary — the proxy FlintAndSteelChance default, parity kept. */
	@Test
	public void theStrikeRollHitsAtThirtyPercent() {
		assertTrue(GTGeneratorSolidBlockEntity.igniteRoll(29), "29 < 30 — a hit");
		assertFalse(GTGeneratorSolidBlockEntity.igniteRoll(30), "30 is NOT under 30 — the upstream fail arm");
		assertFalse(GTGeneratorSolidBlockEntity.igniteRoll(99), "the deep fail arm");
		assertTrue(GTGeneratorSolidBlockEntity.igniteRoll(0), "the certain hit");
	}

	/** The :226/:180 ignite body — mBurning plus the Liquid-family cooldown, Gas inherited. */
	@Test
	public void igniteNowLightsTheBoxAndChargesTheLiquidCooldown() {
		GTGeneratorSolidBlockEntity tSolid = new FixtureSolidBox(POS, Blocks.STONE.defaultBlockState());
		tSolid.igniteNow();
		assertTrue(tSolid.mBurning, "Solid :226 — mBurning = T");
		// the Solid family carries NO cooldown field (only the Liquid-class boxes do)
		GTGeneratorLiquidBlockEntity tLiquid = new GTGeneratorLiquidBlockEntity(sLiquidType, POS, Blocks.STONE.defaultBlockState());
		tLiquid.igniteNow();
		assertTrue(tLiquid.mBurning, "Liquid :180 — mBurning = T");
		assertEquals(100, tLiquid.mCooldown, "Liquid :180 — mCooldown = 100 (the auto-re-ignite window)");
		GTGeneratorGasBlockEntity tGas = new GTGeneratorGasBlockEntity(sGasType, POS, Blocks.STONE.defaultBlockState());
		tGas.igniteNow();
		assertTrue(tGas.mBurning, "Gas inherits the Liquid ignite");
		assertEquals(100, tGas.mCooldown, "Gas inherits the :180 cooldown through the Liquid class");
	}

	/** The :163 burn-out — a lit bufferless box self-extinguishes on the next tick. */
	@Test
	public void aLitBoxWithNoFuelSelfExtinguishesOnTheNextTick() {
		GTGeneratorSolidBlockEntity tSolid = new FixtureSolidBox(POS, Blocks.STONE.defaultBlockState());
		tSolid.igniteNow();
		assertTrue(tSolid.mBurning);
		tSolid.onTick(1, true); // the :163 arm: mEnergy(0) < mRate → mBurning = F
		assertFalse(tSolid.mBurning, "no fuel, no buffer — the box burns out within one tick");
		GTGeneratorLiquidBlockEntity tLiquid = new GTGeneratorLiquidBlockEntity(sLiquidType, POS, Blocks.STONE.defaultBlockState());
		tLiquid.igniteNow();
		tLiquid.onTick(1, true); // the Liquid :156 arm — no tank fuel
		assertFalse(tLiquid.mBurning, "the Liquid box burns out too");
		assertTrue(tLiquid.mCooldown > 0, "the cooldown keeps decaying (the :166 window)");
	}

	/** {@code applyVisualState} offline = the guarded no-op (no level → no setBlock). */
	@Test
	public void applyVisualStateIsASafeNoOpWithoutALevel() {
		GTGeneratorSolidBlockEntity tSolid = new FixtureSolidBox(POS, Blocks.STONE.defaultBlockState());
		tSolid.mBurning = true;
		tSolid.applyVisualState(); // must not throw — the hasLevel guard short-circuits
		assertTrue(tSolid.mBurning);
	}
}
