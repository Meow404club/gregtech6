package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregtech6.util.UT6;

/**
 * The mdk Root default energy block acceptance (task p8-d3 §⑤): the upstream
 * TileEntityBase01Root :703-725 gate math against a minimal concrete fixture —
 * the swallow-aAmount semantics (:717), the ALL_SIZE_IRRELEVANT branch
 * (TD.java:218/221), the Min = Rec/2 · Max = Rec*2 defaults (:719-725), the
 * directionality gates (:716-717 size-0 / not-accepting / not-emitting halves) and
 * the overcharge/explode family (:473-509 minimal form): the tierMax curve, the
 * 0.1 fallback for non-exploding types and the suspend-vs-instant split
 * (!mIsTicking — instant outside the own tick, suspended into
 * {@code mExplosionStrength} and consumed by {@code updateEntityCore} :420-429
 * otherwise; the world ops are skipped on this level-less fixture, so the
 * consumption asserts the setDead half only).
 */
public class TileEntityBase01RootEnergyTest {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	static BlockEntityType<EnergyRoot> sType;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		@SuppressWarnings("unchecked")
		BlockEntityType<EnergyRoot>[] tHolder = (BlockEntityType<EnergyRoot>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new EnergyRoot(false, tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sType = tHolder[0];
	}

	/**
	 * The NO-override Root: only the abstract name — every assertion against this one
	 * exercises the pure Root defaults (upstream :703-725 verbatim).
	 */
	static class BareRoot extends TileEntityBase01Root {

		BareRoot(boolean aTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aTicking, aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test.bare.root";
		}
	}

	/**
	 * The minimal concrete Root: accepts the configured type on the input face, the
	 * input-recommended size is configurable, doInject/doExtract record their calls.
	 */
	static class EnergyRoot extends TileEntityBase01Root {

		TagData mAcceptedType = TD.Energy.EU;
		boolean mEmittingToo = false;
		long mInputRec = 32;
		int mDoInjectCalls = 0, mDoExtractCalls = 0;
		long mLastInjectSize = -1, mLastInjectAmount = -1;

		EnergyRoot(boolean aTicking, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aTicking, aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test.energyroot";
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return aEmitting ? mEmittingToo && aEnergyType == mAcceptedType : aEnergyType == mAcceptedType;
		}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
			return mInputRec;
		}

		@Override
		public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			mDoInjectCalls++;
			mLastInjectSize = aSize;
			mLastInjectAmount = aAmount;
			return aDoInject ? aAmount : 0;
		}

		@Override
		public long doExtract(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
			mDoExtractCalls++;
			return aDoExtract ? aAmount : 0;
		}
	}

	static EnergyRoot makeRoot(boolean aTicking) {
		return new EnergyRoot(aTicking, sType, POS, Blocks.BRICKS.defaultBlockState());
	}

	static TileEntityBase01Root makeBareRoot() {
		return new BareRoot(false, sType, POS, Blocks.BRICKS.defaultBlockState());
	}

	// -------------------------------------------------------------------------
	// the default block on a no-override Root
	// -------------------------------------------------------------------------

	@Test
	void defaultBlockRefusesEverything() {
		TileEntityBase01Root tRoot = makeBareRoot();
		assertFalse(tRoot.isEnergyType(TD.Energy.EU, (byte)2, false), "isEnergyType default false (:707)");
		assertEquals(0, tRoot.getEnergyTypes((byte)6).size(), "getEnergyTypes default empty (:711)");
		assertEquals(0, tRoot.doInject(TD.Energy.EU, (byte)2, 32, 5, true), "doInject hook default 0 (:705)");
		assertEquals(0, tRoot.doExtract(TD.Energy.EU, (byte)2, 32, 5, true), "doExtract hook default 0 (:706)");
		assertEquals(0, tRoot.doEnergyInjection(TD.Energy.EU, (byte)2, 32, 5, true), "the gate bounces on the type (not accepting)");
		assertEquals(0, tRoot.doEnergyExtraction(TD.Energy.EU, (byte)2, 32, 5, true), "the gate bounces on the type (not emitting)");
		assertEquals(0, tRoot.getEnergyOffered(TD.Energy.EU, (byte)2, 32), "getEnergyOffered default 0 (:718)");
		assertEquals(0, tRoot.getEnergyDemanded(TD.Energy.EU, (byte)2, 32), "getEnergyDemanded default 0 (:722)");
		assertFalse(tRoot.isEnergyEmittingTo(TD.Energy.EU, (byte)2, false), "(:714 through the type)");
	}

	@Test
	void defaultSizesAreRecHalfAndRecDouble() {
		// :719-725 — Rec 0 on the bare Root; the fixture sets the INPUT Rec to 32.
		TileEntityBase01Root tDefault = makeBareRoot();
		assertEquals(0, tDefault.getEnergySizeInputRecommended(TD.Energy.EU, (byte)2), ":723");
		assertEquals(0, tDefault.getEnergySizeInputMin(TD.Energy.EU, (byte)2), "InputMin = Rec/2 (:724)");
		assertEquals(0, tDefault.getEnergySizeInputMax(TD.Energy.EU, (byte)2), "InputMax = Rec*2 (:725)");
		assertEquals(0, tDefault.getEnergySizeOutputRecommended(TD.Energy.EU, (byte)2), ":719");
		assertEquals(0, tDefault.getEnergySizeOutputMin(TD.Energy.EU, (byte)2), "OutputMin = Rec/2 (:720)");
		assertEquals(0, tDefault.getEnergySizeOutputMax(TD.Energy.EU, (byte)2), "OutputMax = Rec*2 (:721)");
		assertTrue(tDefault.isSurfaceEnergyAttachable((byte)2), "the attachment seam defaults to true (ITileEntitySurface not ported)");

		EnergyRoot tRoot = makeRoot(false);
		tRoot.mInputRec = 32;
		assertEquals(16, tRoot.getEnergySizeInputMin(TD.Energy.EU, (byte)2), "Min = Rec/2 = 16 (:724)");
		assertEquals(64, tRoot.getEnergySizeInputMax(TD.Energy.EU, (byte)2), "Max = Rec*2 = 64 (:725)");
	}

	// -------------------------------------------------------------------------
	// the :717 injection gate
	// -------------------------------------------------------------------------

	@Test
	void gateSwallowsBelowMinimumPacketsWithoutCallingDoInject() {
		// :717 — EU is not size-irrelevant and |8| < InputMin 16: the WHOLE amount reports
		// as used, but doInject never runs (the energy vanishes).
		EnergyRoot tRoot = makeRoot(false);
		long tUsed = tRoot.doEnergyInjection(TD.Energy.EU, (byte)2, 8, 5, true);
		assertEquals(5, tUsed, "the offered amount reports as used (swallowed)");
		assertEquals(0, tRoot.mDoInjectCalls, "doInject was never called");
	}

	@Test
	void gateDelegatesAtAndAboveTheMinimum() {
		// the exact minimum (16 = |size|) passes the >= check.
		EnergyRoot tRoot = makeRoot(false);
		assertEquals(5, tRoot.doEnergyInjection(TD.Energy.EU, (byte)2, 16, 5, true), "the hook return flows through");
		assertEquals(1, tRoot.mDoInjectCalls, "doInject ran");
		assertEquals(16, tRoot.mLastInjectSize, "the size arrives untouched");

		assertEquals(5, tRoot.doEnergyInjection(TD.Energy.EU, (byte)2, 64, 5, true), "Max and above still injects (the overcharge is the hook's business)");
		assertEquals(2, tRoot.mDoInjectCalls);
	}

	@Test
	void sizeIrrelevantTypesSkipTheMinimumCheck() {
		// RF is an ALL_SIZE_IRRELEVANT member (TD.java:221) — any packet size injects.
		EnergyRoot tRoot = makeRoot(false);
		tRoot.mAcceptedType = TD.Energy.RF;
		long tUsed = tRoot.doEnergyInjection(TD.Energy.RF, (byte)2, 1, 3, true);
		assertEquals(3, tUsed, "the hook return flows through");
		assertEquals(1, tRoot.mDoInjectCalls, "doInject ran despite |1| < InputMin 16");
	}

	@Test
	void zeroSizeAndForeignTypesGateToZero() {
		EnergyRoot tRoot = makeRoot(false);
		assertEquals(0, tRoot.doEnergyInjection(TD.Energy.EU, (byte)2, 0, 5, true), "aSize 0 gates to 0 (:717 first half)");
		assertEquals(0, tRoot.doEnergyInjection(TD.Energy.RF, (byte)2, 32, 5, true), "a non-accepted type gates to 0");
		assertEquals(0, tRoot.mDoInjectCalls, "doInject never ran for either");
	}

	// -------------------------------------------------------------------------
	// the :716 extraction gate
	// -------------------------------------------------------------------------

	@Test
	void extractionGateDelegatesOnlyForEmittingTypes() {
		EnergyRoot tRoot = makeRoot(false);
		assertEquals(0, tRoot.doEnergyExtraction(TD.Energy.EU, (byte)2, 32, 5, true), "not emitting → 0 (:716)");
		assertEquals(0, tRoot.mDoExtractCalls, "doExtract never ran");

		tRoot.mEmittingToo = true;
		tRoot.mAcceptedType = TD.Energy.RF; // OutputMin = Rec/2 = 0 with the default Rec — any size passes
		assertEquals(5, tRoot.doEnergyExtraction(TD.Energy.RF, (byte)2, 1, 5, true), "the hook return flows through");
		assertEquals(1, tRoot.mDoExtractCalls, "doExtract ran (OutputMin 0 accepts any size)");
	}

	// -------------------------------------------------------------------------
	// the overcharge/explode family (:473-509 minimal form)
	// -------------------------------------------------------------------------

	@Test
	void overchargeUsesTheTierCurveForExplodingTypesAndPointOneOtherwise() {
		// EU ∈ ALL_EXPLODING → explode(tierMax(128) = 2); RF is not → explode(0.1).
		// The fixture does not tick (mIsTicking false) so the explosion is INSTANT — but
		// the fixture has no level, so the world ops are skipped and only the buffered
		// strength is observable.
		EnergyRoot tExploding = makeRoot(false);
		tExploding.overcharge(128, TD.Energy.EU);
		assertEquals(2.0F, tExploding.mExplosionStrength, 0.0F, "tierMax(128) = 2 (V = 8/32/128/...)");

		EnergyRoot tInert = makeRoot(false);
		tInert.overcharge(128, TD.Energy.RF);
		assertEquals(0.1F, tInert.mExplosionStrength, 0.0F, "the non-exploding fallback is 0.1 (:500)");
	}

	@Test
	void suspendedExplosionKeepsTheMaxDemandAndIsConsumedByTheOwnTick() {
		// A ticking machine injected by a NEIGHBOUR's tick suspends (explode(!mIsTicking)
		// = explode(false), :475-476) — the flag buffers the max (:482) and the own tick
		// consumes it (:420-429: world ops + setDead + return).
		EnergyRoot tRoot = makeRoot(true);
		tRoot.explode(2.0);
		assertFalse(tRoot.isDead(), "suspended, not consumed yet");
		tRoot.explode(1.0);
		assertEquals(2.0F, tRoot.mExplosionStrength, 0.0F, "the max of the demands (:482)");

		tRoot.updateEntityCore(); // package-visible seam; no level → the world ops skip
		assertTrue(tRoot.isDead(), "the own tick consumed the suspension and killed the BE (:427)");
	}

	@Test
	void instantExplodeOnANonTickingRoot() {
		// A non-ticking BE has no tick loop to consume the flag — explode() is instant
		// (explode(!mIsTicking), upstream :475) and buffers the default strength 4 (:479).
		EnergyRoot tRoot = makeRoot(false);
		tRoot.explode();
		assertEquals(4.0F, tRoot.mExplosionStrength, 0.0F, "the strength-4 default (:479)");
		assertTrue(tRoot.isDead() == false, "no level attached: only the buffer is observable");
	}

	// -------------------------------------------------------------------------
	// UT6.tierMax — the upstream UT.Code.tierMax ladder (UT.java:1388-1393, V at CS.java:151)
	// -------------------------------------------------------------------------

	@Test
	void tierMaxWalksTheVoltageLadder() {
		assertEquals(0, UT6.tierMax(1), "8 EU and below is tier 0");
		assertEquals(0, UT6.tierMax(8), "the boundary is inclusive (<=)");
		assertEquals(1, UT6.tierMax(9), "one over 8 is tier 1");
		assertEquals(1, UT6.tierMax(32), "32 is tier 1");
		assertEquals(2, UT6.tierMax(128), "128 is tier 2");
		assertEquals(3, UT6.tierMax(512), "512 is tier 3");
		assertEquals(9, UT6.tierMax(2097152), "2 EU is tier 9");
		assertEquals(16, UT6.tierMax(Long.MAX_VALUE), "off the 16-entry ladder top = the table length");
		assertEquals(0, UT6.tierMax(-8), "abs() first (:1390)");
	}
}
