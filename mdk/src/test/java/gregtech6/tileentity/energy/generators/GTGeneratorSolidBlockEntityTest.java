package gregtech6.tileentity.energy.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMapFurnaceFuel;
import gregtech6.tileentity.GTOfflineTestBase;
import net.minecraft.world.item.crafting.RecipeType;
//? if forge {
import net.minecraftforge.common.ForgeHooks;
//?}

/**
 * Task p13-burning-box-family — the offline acceptance fixture for the Solid Burning
 * Box semantics (the HuEnergyHandshakeTest fixture form): the HU emit truth table, the
 * SIDES_TOP six-direction gate, the refuel continue-burn gate, the fuel-charge
 * formula (UT.Code.units), the fire-spread pure-function rng table, and the
 * RecipeMapFurnaceFuel bridge against the vanilla fuel values.
 *
 * <p>Upstream anchors: MultiTileEntityGeneratorSolid.java:103-105 (the emit — the
 * packet is spent UNCONDITIONALLY, "打空也扣能", so with the receiver absent the
 * buffer RETAINS its bulk and loses exactly mRate per burning tick — this wave has no
 * HU consumer, W3's boilers are the sink), :106-108 (the fire judgment), :110 (the
 * 2×mRate refuel gate), :156 (the charge), :163 (burn-out), :270 (the top gate).
 */
public class GTGeneratorSolidBlockEntityTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);
	static BlockEntityType<FixtureBox> sType;

	/** The concrete test BE — the abstract family base over a vanilla-block BET. */
	public static final class FixtureBox extends GTGeneratorSolidBlockEntity {
		public FixtureBox(BlockPos aPos, BlockState aState) {
			super(sType, aPos, aState);
		}
	}

	/** HU-counting sink (the HuEnergyHandshakeTest form). */
	public static class HuSink extends net.minecraft.world.level.block.entity.BlockEntity implements ITileEntityEnergy {
		public long injectedAmount = 0, injectionCalls = 0;
		public byte lastSide = -1;

		public HuSink(BlockPos aPos) {
			super(null, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) {
					injectionCalls++;
					injectedAmount += aAmount;
					lastSide = aSide;
				}
				return aAmount;
			}
			return 0;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return aEnergyType == TD.Energy.HU;}

		@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return aEnergyType == TD.Energy.HU;}
		@Override public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}
		@Override public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}
		@Override public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}
		@Override public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
		@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.HU.AS_LIST;}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<FixtureBox>[] tHolder = (BlockEntityType<FixtureBox>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(FixtureBox::new, Blocks.STONE).build(null);
		sType = tHolder[0];
		GT6RecipeMaps.init(); // the live FURNACE_FUEL instance (the shared append face)
		try {
			//? if forge {
			ForgeHooks.updateBurns(); // the vanilla VANILLA_BURNS population (ForgeInternalHandler:106 live path) — offline-safe after Bootstrap
			//?} else {
			/*// 21.1: updateBurns is gone — the fuel values ride the FuelValues datapack face,
			//no offline population hook; the burn-time gates below assumeTrue-skip instead.
			*///?}
		} catch (Throwable tIgnored) {
			// the bridge test gates itself on getBurnTime availability
		}
	}

	// ---------------------------------------------------------------------------
	// 1. the HU emit truth table
	// ---------------------------------------------------------------------------

	@Test
	public void emitFiresOnlyWhenBufferReachesTheRate() {
		FixtureBox tBox = new FixtureBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurning = true;
		HuSink tSink = new HuSink(POS.offset(0, 1, 0)); // the sink ABOVE (the emit face is the top)
		tBox.setAdjacencyOverride(aSide -> aSide == 1 ? new EnergyTarget(tSink, (byte)0) : null);

		tBox.mEnergy = 10; // below mRate — the :103 gate stays shut
		tBox.onTick(1, true);
		assertEquals(10, tBox.mEnergy, "buffer under mRate: nothing emitted, nothing drained");
		assertEquals(0, tSink.injectionCalls, "no packets booked");
		assertFalse(tBox.mBurning, "10 < mRate — the :163 burn-out (the idle arm needs a front fire)");

		tBox.mEnergy = 100; // above mRate
		tBox.mBurning = true; // re-arm (the fire path is the idle arm's own test below)
		tBox.onTick(2, true);
		assertEquals(16, tSink.injectedAmount, "the :104 packet = min(mRate, mEnergy) = 16");
		assertEquals(1, tSink.injectionCalls, "one packet, one booking");
		assertEquals(84, tBox.mEnergy, "the :105 drain — mRate per burning tick");
		assertEquals(0, tSink.lastSide, "the injection lands on the sink's bottom face (the box's top output)");
	}

	@Test
	public void thePacketIsCappedAtMinRateBufferWithNoSinkRetainingTheBulk() {
		FixtureBox tBox = new FixtureBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 64;
		tBox.mBurning = true;
		tBox.setAdjacencyOverride(aSide -> null); // NO receiver anywhere — the :104 Util loop uses nothing
		tBox.mEnergy = 200;
		tBox.onTick(1, true);
		// the upstream :104-105 semantic (the diesel "打空也扣能" family): the emit runs, the
		// packet is spent into the void, and the buffer loses EXACTLY mRate — the bulk of
		// the charge is RETAINED (the W2 boundary: no HU consumer exists yet, the buffer
		// drains at mRate/tick until the next fuel charge refills it)
		assertEquals(136, tBox.mEnergy, "200 − mRate 64 — the retained bulk, not a void");
		assertTrue(tBox.mBurning, "136 ≥ mRate: still burning");
	}

	@Test
	public void burnOutAtBufferUnderRate() {
		FixtureBox tBox = new FixtureBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurning = true;
		tBox.setAdjacencyOverride(aSide -> null);
		tBox.mEnergy = 16; // exactly mRate: emits, drops to 0, then :163 flips the flag
		tBox.onTick(1, true);
		assertEquals(0, tBox.mEnergy, "16 − 16 — the :171 clamp not even needed");
		assertFalse(tBox.mBurning, ":163 — out of fuel");
		tBox.onTick(2, true);
		assertFalse(tBox.mBurning, "the idle arm only re-ignites on a front fire (rng-gated)");
	}

	// ---------------------------------------------------------------------------
	// 2. the SIDES_TOP six-direction gate (the :270 form — the pre-pinned W1 table)
	// ---------------------------------------------------------------------------

	@Test
	public void theTopFaceGateTruthTableOverAllSixSides() {
		FixtureBox tBox = new FixtureBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mBurning = false; // the gate is mode-independent (the theoretical probe answers while off)
		for (byte tSide = 0; tSide < 6; tSide++) {
			assertEquals(tSide == 1, tBox.isEnergyEmittingTo(TD.Energy.HU, tSide, true),
					"theoretical probe, side " + tSide + (tSide == 1 ? " — the top face alone" : " — dead"));
			assertEquals(tSide == 1, tBox.isEnergyEmittingTo(TD.Energy.HU, tSide, false),
					"real probe, side " + tSide);
		}
		// and the type gate: HU only
		assertFalse(tBox.isEnergyEmittingTo(TD.Energy.EU, (byte)1, true), "EU refused on the top face");
		assertFalse(tBox.isEnergyType(TD.Energy.HU, (byte)1, false), "the accepting probe stays pure-source");
		assertTrue(tBox.isEnergyType(TD.Energy.HU, (byte)1, true), ":269 — the emitting probe, HU, any side isEnergyType; the FACE gate is :270");
	}

	// ---------------------------------------------------------------------------
	// 3. the refuel gate + the charge formula
	// ---------------------------------------------------------------------------

	@Test
	public void theRefuelGateFiresOnlyUnderTwoPackets() {
		org.junit.jupiter.api.Assumptions.assumeTrue(
				//? if forge {
				ForgeHooks.getBurnTime(new ItemStack(Items.COAL), RecipeType.SMELTING) > 0,
				//?} else {
				/*new ItemStack(Items.COAL).getBurnTime(RecipeType.SMELTING) > 0, // 21.1: IItemStackExtension face
				*///?}
				"the VANILLA_BURNS population (updateBurns ran) — without it the charge math rides the units() test above");
		FixtureBox tBox = new FixtureBox(POS, Blocks.STONE.defaultBlockState());
		tBox.mRate = 16;
		tBox.mBurning = true;
		tBox.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 4));

		tBox.mEnergy = 100; // ≥ 2×mRate 32 — no charge after the emit (100−16=84 ≥ 32)
		tBox.onTick(1, true);
		assertEquals(4, tBox.mInventory.getStackInSlot(0).getCount(), "the :110 gate: buffer ≥ 2×mRate → no fuel consumed");
		assertEquals(84, tBox.mEnergy, "only the emit drained it");

		tBox.mEnergy = 24; // after the next emit 24−16=8 < 32 — the gate opens
		tBox.onTick(2, true);
		assertEquals(3, tBox.mInventory.getStackInSlot(0).getCount(), "one coal consumed");
		// the :156 charge — units(1600 × 25, 10000, 10000, F) = 40000 (coal, the vanilla fuel value)
		assertEquals(8 + 40000, tBox.mEnergy, "the charge = |−1 × (1600×25)| at the identity efficiency");
		assertTrue(tBox.mBurning, "the charge re-arms the flame");
	}

	@Test
	public void theChargeFormulaIsTheUnitsEfficiencyTranslation() {
		// the UT.Code.units form of :156 against the Brick box: 40000 raw at eff 2500 = 10000
		assertEquals(10000, GTGeneratorSolidBlockEntity.units(40000, 10000, 2500, false), "Brick: 1600t coal × 25 × 0.25");
		// the Steel solid box (eff 7000): 40000 × 0.7 = 28000
		assertEquals(28000, GTGeneratorSolidBlockEntity.units(40000, 10000, 7000, false), "Steel solid");
		// the identity at eff 10000
		assertEquals(40000, GTGeneratorSolidBlockEntity.units(40000, 10000, 10000, false), "the Invar identity");
		// and the diesel cross-check: the same units() answers the FM.Burn charge
		assertEquals(320, GTGeneratorSolidBlockEntity.units(320, 10000, 10000, false), "1L diesel-class fuel at eff 10000 (|−64×5| = 320)");
	}

	// ---------------------------------------------------------------------------
	// 4. the fire-spread pure function (the :106 judgment verbatim)
	// ---------------------------------------------------------------------------

	@Test
	public void theFireSpreadJudgmentIsThePureFunction() {
		assertTrue(GTGeneratorSolidBlockEntity.shouldSpreadFire(2500, 0), "rng(mEfficiency) == 0 → spread");
		assertFalse(GTGeneratorSolidBlockEntity.shouldSpreadFire(2500, 1), "rng != 0 → no spread");
		assertFalse(GTGeneratorSolidBlockEntity.shouldSpreadFire(2500, 2499), "the upper rng bound, no spread");
		assertTrue(GTGeneratorSolidBlockEntity.shouldSpreadFire(0, 12345), "eff < 1 → spread EVERY tick (the :106 disjunction)");
		assertTrue(GTGeneratorSolidBlockEntity.shouldSpreadFire(-1, 7), "a negative efficiency divides the rng to nothing — same arm");
		assertFalse(GTGeneratorSolidBlockEntity.shouldSpreadFire(1, 1), "eff 1: only rng(1) == 0 spreads");
		assertTrue(GTGeneratorSolidBlockEntity.shouldSpreadFire(1, 0), "eff 1, rng 0 → spread");
	}

	// ---------------------------------------------------------------------------
	// 5. the RecipeMapFurnaceFuel bridge (the vanilla fuel values, where updateBurns ran)
	// ---------------------------------------------------------------------------

	@Test
	public void theFurnaceBridgeAnswersTheVanillaFuelValues() {
		//? if forge {
		org.junit.jupiter.api.Assumptions.assumeTrue(ForgeHooks.getBurnTime(new ItemStack(Items.COAL), RecipeType.SMELTING) > 0,
		//?} else {
		/*org.junit.jupiter.api.Assumptions.assumeTrue(new ItemStack(Items.COAL).getBurnTime(RecipeType.SMELTING) > 0,
		*///?}
				"the VANILLA_BURNS map populated (updateBurns ran); without it the bridge test is vacuous");
		Recipe tRecipe = GT6RecipeMaps.FURNACE_FUEL.findFuelRecipe(new ItemStack(Items.COAL));
		assertTrue(tRecipe != null, "coal is a furnace fuel");
		assertEquals(1600L * 25, tRecipe.getAbsoluteTotalPower(), "1600 ticks × EU_PER_FURNACE_TICK 25 = 40000 HU raw (the :74 row math)");
		assertEquals(-1, tRecipe.mEUt, "the negative-EUt generator convention, verbatim");
		assertFalse(tRecipe.mCanBeBuffered, "the fresh-per-find port precedent");
		assertNull(GT6RecipeMaps.FURNACE_FUEL.findFuelRecipe(new ItemStack(Items.STONE)), "stone is not a furnace fuel");
		assertTrue(GT6RecipeMaps.FURNACE_FUEL.containsFuelInput(new ItemStack(Items.CHARCOAL)), "charcoal 1600 — the containsInput gate");
		assertFalse(GT6RecipeMaps.FURNACE_FUEL.containsFuelInput(ItemStack.EMPTY), "the empty guard");
	}
}
