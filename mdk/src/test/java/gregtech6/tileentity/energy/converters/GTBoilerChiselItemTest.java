package gregtech6.tileentity.energy.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.items.tools.GT6ToolActions;
import gregtech6.items.tools.GTChiselItem;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase;

/**
 * Task p16-chisel-decalcify — the useOn-arm offline tests (the card's test domain): the
 * chisel item's static dispatch seam ({@link GTChiselItem#chiselToolClick(UseOnContext)},
 * the GTCutterItem p10/p11 test form) against a real {@link GTBoilerTankBlockEntity}.
 * The server chisel semantics themselves are the GTBoilerTankBlockEntityTest pins (the
 *ChiselDetonatesAboveFifteenAndRepairsBelow fixture, :386/:415) — THIS test pins the ITEM
 * layer on top of them: the hit reaches the boiler's own {@code chisel} face (the repair
 * observable lands), the Behavior_Tool.java:63 durability conversion pays (the
 * payPerPoint seam + the mDamage=25 points table), the detonation branch pays NOTHING
 * (upstream :178 return 0) and a non-boiler target is passed without touching the tool.
 *
 * <p>The GTCutterItem NOTE applies unchanged: a mod Item cannot be constructed in this
 * bootstrapped-and-frozen JVM (Item.java:61 intrusive holder), so {@code useOn} itself
 * rides the RCON chain while the static dispatch + the classifier pin here. The physical
 * {@code hurtAndBreak} half needs a server-side LivingEntity (the offline Player wall) —
 * the points the deduction WOULD pay are pinned exactly by {@link #sPayPerPointCalls} +
 * the {@link GTChiselItem#durabilityPoints} table.
 */
public class GTBoilerChiselItemTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);
	static BlockEntityType<FixtureBoiler> sType;
	static BlockEntityType<ForeignBlockEntity> sForeignType;

	/** The concrete test BE — the boiler class over a vanilla-block BET (the p13 fixture). */
	public static final class FixtureBoiler extends GTBoilerTankBlockEntity {
		public FixtureBoiler(BlockPos aPos, BlockState aState) {
			super(sType, aPos, aState);
		}
	}

	/** A concrete non-boiler BE (BlockEntity is abstract; the minimal concrete stand-in). */
	public static final class ForeignBlockEntity extends BlockEntity {
		public ForeignBlockEntity(BlockPos aPos, BlockState aState) {
			super(sForeignType, aPos, aState);
		}
	}

	/**
	 * The click level — a MachineLevel that yields the boiler at {@link #POS} and a
	 * foreign (non-boiler) BE at {@link #FOREIGN_POS} (the CoverClickLevel shape).
	 */
	public static class ChiselClickLevel extends GTMachinesOfflineTestBase.MachineLevel {
		public final BlockEntity mBoiler;
		public final BlockEntity mForeign;

		public ChiselClickLevel(BlockEntity aBoiler, BlockEntity aForeign) {
			super(new GTRecipesOfflineTestBase.TestRecipeManager());
			mBoiler = aBoiler;
			mForeign = aForeign;
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			if (aPos.equals(POS)) return mBoiler;
			if (aPos.equals(FOREIGN_POS)) return mForeign;
			return null;
		}
	}

	static final BlockPos FOREIGN_POS = new BlockPos(8, 4, 5);

	/** A calcified low-pressure fixture: efficiency 9000, barometer 10, 200000 L of steam stand-in, 500 HU. */
	private static FixtureBoiler calcifiedLowPressure() {
		FixtureBoiler tBoiler = new FixtureBoiler(POS, Blocks.STONE.defaultBlockState());
		tBoiler.setOutput(32);
		tBoiler.mEfficiency = 9000;
		tBoiler.mTanks[1].add(200000, new FluidStack(Fluids.LAVA, 200000));
		tBoiler.mEnergy = 500;
		tBoiler.mBarometer = 10;
		return tBoiler;
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		BlockEntityType<FixtureBoiler>[] tHolder = (BlockEntityType<FixtureBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FixtureBoiler::new, Blocks.STONE).build(null);
		sType = tHolder[0];
		BlockEntityType<ForeignBlockEntity>[] tForeign = (BlockEntityType<ForeignBlockEntity>[]) new BlockEntityType<?>[1];
		tForeign[0] = BlockEntityType.Builder.of(ForeignBlockEntity::new, Blocks.BRICKS).build(null);
		sForeignType = tForeign[0];
	}

	@BeforeEach
	void resetPayCounter() {
		GTChiselItem.sPayPerPointCalls = 0;
	}

	/** The context double (the CutterTest.TestContext verbatim shape). */
	private static UseOnContext clickContext(Level aLevel, BlockHitResult aHit) {
		return new UseOnContext(aLevel, null, InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_HOE), aHit);
	}

	private static BlockHitResult hitAt(BlockPos aPos) {
		return new BlockHitResult(new Vec3(aPos.getX() + 0.5, aPos.getY() + 0.5, aPos.getZ() + 0.5), Direction.UP, aPos, false);
	}

	@Test
	public void theUseOnArmRepairsTheLowPressureBoilerAndPaysTheUpstreamPoints() {
		FixtureBoiler tBoiler = calcifiedLowPressure();
		ChiselClickLevel tLevel = new ChiselClickLevel(tBoiler, null);

		assertEquals(1000, GTChiselItem.chiselToolClick(clickContext(tLevel, hitAt(POS))),
				"the :175 return = 10000 - mEfficiency (9000) rides straight out of the item dispatch");
		// the server-side repair observables (the p13 pins, re-proven through the item arm)
		assertEquals(10000, tBoiler.mEfficiency, "the :173 reset");
		assertEquals(0, tBoiler.mEnergy, "the :174 reset");
		assertEquals(0, tBoiler.mTanks[1].amount(), "the :172 vent");
		assertEquals(0.0F, tBoiler.mExplosionStrength, "no explosion on the repair branch");
		// the durability deduction: ONE payPerPoint call, and the mDamage=25 conversion
		// fully determines the vanilla points the live hurtAndBreak pays (3 for 1000)
		assertEquals(1, GTChiselItem.sPayPerPointCalls, "the item layer pays exactly once per click");
		assertEquals(3, GTChiselItem.durabilityPoints(1000), "units(1000, 10000, 25, T) = 3 points");
	}

	@Test
	public void theUseOnArmDetonatesThePressurisedBoilerWithoutPaying() {
		FixtureBoiler tBoiler = calcifiedLowPressure();
		tBoiler.mBarometer = 16; // above the :168 gate
		ChiselClickLevel tLevel = new ChiselClickLevel(tBoiler, null);

		assertEquals(0, GTChiselItem.chiselToolClick(clickContext(tLevel, hitAt(POS))),
				"the detonation branch returns 0 (upstream :178)");
		assertTrue(tBoiler.mExplosionStrength > 0, "the deferred explode(F) is armed");
		assertEquals(9000, tBoiler.mEfficiency, "the tank is NOT cleared on the detonation branch");
		assertEquals(200000, tBoiler.mTanks[1].amount(), "the steam stays on the detonation branch");
		assertEquals(0, GTChiselItem.sPayPerPointCalls,
				"Behavior_Tool.java:62 pays only when tDamage > 0 — a detonating chisel pays nothing");
	}

	@Test
	public void theUseOnArmPaysNothingWhenTheBoilerIsPristine() {
		FixtureBoiler tBoiler = calcifiedLowPressure();
		tBoiler.mEfficiency = 10000; // rResult = 0 — nothing to descale
		ChiselClickLevel tLevel = new ChiselClickLevel(tBoiler, null);

		assertEquals(0, GTChiselItem.chiselToolClick(clickContext(tLevel, hitAt(POS))), "rResult 0 = nothing to decalcify");
		assertEquals(0, GTChiselItem.sPayPerPointCalls, "no repair, no payment");
		assertEquals(10000, tBoiler.mEfficiency, "the pristine boiler is untouched");
	}

	@Test
	public void theUseOnArmPassesNonBoilerBlocksWithoutTouchingTheTool() {
		BlockEntity tForeign = sForeignType.create(FOREIGN_POS, Blocks.BRICKS.defaultBlockState());
		FixtureBoiler tBoiler = calcifiedLowPressure();
		ChiselClickLevel tLevel = new ChiselClickLevel(tBoiler, tForeign);

		// a foreign BE at the clicked pos — the dispatch declines it, nothing is paid
		assertEquals(0, GTChiselItem.chiselToolClick(clickContext(tLevel, hitAt(FOREIGN_POS))),
				"the chisel passes non-boiler block entities");
		assertEquals(0, GTChiselItem.sPayPerPointCalls, "no target, no payment — the carrier is never hurt");
		// and the boiler standing elsewhere on the same level is untouched by that click
		assertEquals(9000, tBoiler.mEfficiency, "the calcification is untouched by the foreign click");
		assertEquals(200000, tBoiler.mTanks[1].amount(), "the steam tank is untouched by the foreign click");

		// a bare level (no BE at all) declines the same way
		ChiselClickLevel tEmpty = new ChiselClickLevel(null, null);
		assertEquals(0, GTChiselItem.chiselToolClick(clickContext(tEmpty, hitAt(POS))), "no BE, no dispatch");
		assertEquals(0, GTChiselItem.sPayPerPointCalls, "no target, no payment");
	}

	/**
	 * The points table — the upstream Behavior_Tool.java:63 conversion
	 * {@code units(tDamage, 10000, 25, T)} with the ROUND-UP (any non-zero repair pays at
	 * least one point; a full 10000-unit repair costs 25). The physical hurtAndBreak is
	 * the live half (the offline Player wall, class doc).
	 */
	@Test
	public void thePointsMappingFollowsTheUpstreamBehaviourDamage() {
		assertEquals(25, GTChiselItem.UPSTREAM_DAMAGE_PER_REPAIR, "the GT_Tool_Chisel.java:98 behaviour damage");
		assertEquals(512, GTChiselItem.DURABILITY_POINTS, "the single steel tier (the crowbar/cutter family value)");
		assertEquals(0, GTChiselItem.durabilityPoints(0), "zero damage pays nothing");
		assertEquals(1, GTChiselItem.durabilityPoints(1), "the round-up: any non-zero repair pays one point");
		assertEquals(1, GTChiselItem.durabilityPoints(399), "the 400-unit point boundary (round up)");
		assertEquals(1, GTChiselItem.durabilityPoints(400), "400 units = exactly one point (10000/25)");
		assertEquals(2, GTChiselItem.durabilityPoints(401), "just past the boundary rounds up to two");
		assertEquals(3, GTChiselItem.durabilityPoints(1000), "the pinned repair value of the 9000 fixture");
		assertEquals(13, GTChiselItem.durabilityPoints(5000), "the 5000-floor worst case");
		assertEquals(25, GTChiselItem.durabilityPoints(10000), "a full 10000-unit repair");
	}

	/** The classifier pin — chisel only, never a wrench substitute (the cutter red line form). */
	@Test
	public void theClassifierExposesTheChiselActionOnly() {
		assertEquals("gt6_chisel", GT6ToolActions.CHISEL.name(), "the ADR-family action name");
		assertEquals("chisel", GT6ToolActions.CHISEL_ID, "the upstream CS.TOOL_chisel dispatch id (CS.java:1060)");
		assertTrue(GTChiselItem.classifies(GT6ToolActions.CHISEL), "the chisel action classifies");
		assertFalse(GTChiselItem.classifies(ToolActions.HOE_DIG), "RED LINE — never a hoe: the three wrench predicates must not fire");
		assertFalse(GTChiselItem.classifies(GT6ToolActions.CROWBAR), "the chisel is not a crowbar");
		assertFalse(GTChiselItem.classifies(GT6ToolActions.CUTTER), "the chisel is not the wire cutter");
	}
}
