package gregtech6.tileentity.energy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6ElectricTransformerBlockEntity offline tests (task p28-c-ulv-lv-transformer
 * acceptance ④): the ENERGY CONSERVATION table of both modes (4×8 EU → 1×32 EU and
 * 1×32 EU → 4×8 EU — the Transformer (ULV-LV) row Loader_MultiTileEntities.java:881
 * packet math, {@code NBT_INPUT, V[1]=32, NBT_OUTPUT, V[0]=8, NBT_MULTIPLIER, 4,
 * WASTE_ENERGY, F}), the BIDIRECTIONAL switch (the Base11 :80-88 storage-clearing
 * flip + the :63-64 face-set swap), the size-band pins per mode (Base10 :76-77 /
 * Base11 :56-57), the ULV-wall regression anchor (an 8 EU packet BELOW the step-down
 * input min 16 is swallowed by the Root gate — the same wall that keeps 8 EU out of
 * every LV machine, GTMachines.TIER_INPUTS[0][0]), the waste=F input throttle (Base10
 * :151 — the term the dynamo rows fold away because their waste=T arm is
 * constant-true), and the recipe MATERIAL-LOCK pins (decisions.p28-ulv-tier-rulings
 * transformer_ruling: the galvanized-steel casing + the LV-era copper wires + the
 * iron double plates).
 *
 * <p>Offline harness (the axle/water-wheel form): level-less fixtures on a vanilla
 * STONE state (the state cache answers getBlockState() offline — BlockEntity.java:145,
 * the GearBoxTest form), the {@code mAdjacencyOverride} seam wires the counting sink,
 * the injection side drives the Root gated {@code doEnergyInjection} (the live path).
 */
public class GT6ElectricTransformerBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6ElectricTransformerBlockEntity> sType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	/** The GT6 byte side of NORTH (the default HORIZONTAL_FACING; the BE mFacing default). */
	static final byte FRONT = 2;

	/** A non-front side (SOUTH = 3) — the reversed mode's input set member. */
	static final byte SIDE = 3;

	/** The reversed-mode injection side alias (readability: step-up tests push into SIDE). */
	static byte front() {return SIDE;}

	@BeforeAll
	static void initMaterials() {
		// the material system refills the MT fields (the GT6DynamoFamilyRowTest posture —
		// the recipe MATERIAL-LOCK pins read MT.SteelGalvanized / MT.Copper identities)
		GTMaterialItems.initMaterials();
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixture() {
		BlockEntityType<GT6ElectricTransformerBlockEntity>[] tHolder = (BlockEntityType<GT6ElectricTransformerBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ElectricTransformerBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** A fresh step-down (default) transformer fixture. */
	private static GT6ElectricTransformerBlockEntity transformer() {
		return new GT6ElectricTransformerBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A fresh step-up fixture (the Base11 flip already applied). */
	private static GT6ElectricTransformerBlockEntity transformerUp() {
		GT6ElectricTransformerBlockEntity rTrans = transformer();
		rTrans.toggleReversed();
		return rTrans;
	}

	/**
	 * Counting EU sink — the water-wheel CountingSink form: accepts EU from every side,
	 * records every packet (size, amount), refuses on demand (the plugged-net throttle
	 * fixture).
	 */
	public static class EuSink extends BlockEntity implements ITileEntityEnergy {
		public boolean refuse = false;
		public final List<long[]> packets = new ArrayList<>(); // {size, amount}

		static final BlockEntityType<EuSink> FAKE_TYPE =
				BlockEntityType.Builder.of((aPos, aState) -> new EuSink(aPos), Blocks.STONE).build(null);

		public EuSink(BlockPos aPos) {
			super(FAKE_TYPE, aPos, Blocks.STONE.defaultBlockState());
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.EU;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.EU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}

		@Override
		public Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (!refuse && aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, false)) {
				if (aDoInject) packets.add(new long[] {aSize, aAmount});
				return aAmount;
			}
			return 0;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return 0;}
	}

	/** A sink fixture wired to every side of the transformer under test. */
	private static EuSink wire(GT6ElectricTransformerBlockEntity aTrans) {
		EuSink tSink = new EuSink(new BlockPos(3, 4, 6));
		aTrans.setAdjacencyOverride(aSide -> new EnergyTarget(tSink, (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue()));
		return tSink;
	}

	// ---------------------------------------------------------------------------
	// 1. the step-down conservation table (the upstream default = the regression anchor)
	// ---------------------------------------------------------------------------

	@Test
	public void stepDownOne32euPacketYieldsFour8euPackets() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		EuSink tSink = wire(tTrans);

		// the high side accepts one 32 EU packet (min 16 <= 32 <= max 64)
		assertEquals(1, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true), "one 32 EU packet fully accepted");
		assertEquals(32, tTrans.mStorage, "the capacitor holds the packet's EU");

		tTrans.onTick(11, true);

		assertEquals(1, tSink.packets.size(), "one dispatch call landed on the sink");
		assertEquals(8, tSink.packets.get(0)[0], "the packet SIZE is the low side V[0] = 8");
		assertEquals(4, tSink.packets.get(0)[1], "the packet COUNT is NBT_MULTIPLIER = 4");
		assertEquals(0, tTrans.mStorage, "the capacitor paid exactly the emitted EU (4 x 8 = 32 in = out)");
		assertTrue(tTrans.mActive, "the converter reports activity");
	}

	@Test
	public void stepDownPartialAcceptanceChargesOnlyTheEmittedEU() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		EuSink tSink = wire(tTrans);
		tSink.refuse = true; // the net is plugged

		assertEquals(1, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true));
		tTrans.onTick(11, true);
		assertTrue(tSink.packets.isEmpty(), "nothing emitted into the plugged net");
		assertEquals(32, tTrans.mStorage, "waste = F: the capacitor KEEPS the charge (no vent, no loss)");

		tSink.refuse = false; // unplugged mid-charge
		tTrans.onTick(12, true);
		assertEquals(1, tSink.packets.size());
		assertEquals(0, tTrans.mStorage, "the delayed discharge is still conservation-exact");
	}

	// ---------------------------------------------------------------------------
	// 2. the step-up conservation table (the reversed converter, Base11 :56-57)
	// ---------------------------------------------------------------------------

	@Test
	public void stepUpFour8euPacketsYieldOne32euPacket() {
		GT6ElectricTransformerBlockEntity tTrans = transformerUp();
		EuSink tSink = wire(tTrans);

		// 4 x 8 EU trickle in on the low side (the FE-converter / ULV-grid packet)
		for (int i = 0; i < 4; i++) assertEquals(1, tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true), "8 EU packet accepted (min 1)");
		assertEquals(32, tTrans.mStorage);

		tTrans.onTick(11, true);

		assertEquals(1, tSink.packets.size(), "one dispatch call");
		assertEquals(32, tSink.packets.get(0)[0], "ONE packet of the accumulated 32 EU");
		assertEquals(1, tSink.packets.get(0)[1], "packet count 1 (multiplier folds to 1 reversed)");
		assertEquals(0, tTrans.mStorage, "4 x 8 EU in = 1 x 32 EU out — conservation");
	}

	@Test
	public void stepUpAccumulationIsGradualAndDoorGated() {
		GT6ElectricTransformerBlockEntity tTrans = transformerUp();
		EuSink tSink = wire(tTrans);

		tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true);
		tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true);
		assertEquals(16, tTrans.mStorage);
		tTrans.onTick(11, true);
		assertTrue(tSink.packets.isEmpty(), "16 EU < the :57 door 24: no emit (an LV net would bounce sub-16 anyway)");
		assertFalse(tTrans.mCanEmitEnergy, "the door readout is closed");

		tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true); // 24
		tTrans.onTick(12, true);
		assertEquals(1, tSink.packets.size());
		assertEquals(24, tSink.packets.get(0)[0], "the :57 door emits the ACCUMULATED size (>= 24), upstream :85 verbatim");
		assertEquals(0, tTrans.mStorage);
	}

	// ---------------------------------------------------------------------------
	// 3. the ULV-wall regression anchor (the step-down input min swallows 8 EU packets)
	// ---------------------------------------------------------------------------

	@Test
	public void stepDownHighSideSwallowsAn8euPacketBelowMin() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		EuSink tSink = wire(tTrans);

		// the Root gate (EnergyGate.gateInjection): 8 < input-min 16 → the offer is
		// swallowed (returns aAmount, doInject never runs) — the same wall class that
		// keeps the 8 EU ULV grid out of every LV machine
		assertEquals(8, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 8, 8, true), "the swallowed offer counts as USED");
		assertEquals(0, tTrans.mStorage, "nothing stored — the packet died at the gate");
		tTrans.onTick(11, true);
		assertTrue(tSink.packets.isEmpty(), "nothing to emit");
	}

	// ---------------------------------------------------------------------------
	// 4. the bidirectional switch (Base11 :80-88 + the :63-64 face sets)
	// ---------------------------------------------------------------------------

	@Test
	public void modeFlipClearsStorageAndSwapsTheFaceSets() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();

		// normal: FRONT in, ALL-BUT-FRONT out
		assertTrue(tTrans.isInput(FRONT));
		assertFalse(tTrans.isOutput(FRONT));
		assertTrue(tTrans.isOutput((byte) 0));
		assertTrue(tTrans.isOutput((byte) 1));
		assertFalse(tTrans.isInput((byte) 0));

		tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true);
		assertEquals(32, tTrans.mStorage);

		tTrans.toggleReversed();

		assertTrue(tTrans.mReversed, "the flag flipped");
		assertEquals(0, tTrans.mStorage, "Base11 :81 — the flip clears the capacitor (accidental-overcharge guard)");

		// reversed: FRONT out, ALL-BUT-FRONT in
		assertTrue(tTrans.isOutput(FRONT));
		assertFalse(tTrans.isInput(FRONT));
		assertTrue(tTrans.isInput((byte) 3));
		assertFalse(tTrans.isOutput((byte) 3));
	}

	@Test
	public void reversedNbtRoundTrip() {
		GT6ElectricTransformerBlockEntity tTrans = transformerUp();
		tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true);

		CompoundTag tNBT = new CompoundTag();
		tTrans.saveAdditional(tNBT);
		assertTrue(tNBT.getBoolean(GT6ElectricTransformerBlockEntity.NBT_REVERSED), "the mode flag persists");

		GT6ElectricTransformerBlockEntity tRestored = transformer();
		tRestored.load(tNBT);
		assertTrue(tRestored.mReversed, "the reversed mode survives save/load");
		assertEquals(8, tRestored.mStorage, "the capacitor survives save/load");
	}

	// ---------------------------------------------------------------------------
	// 5. the size-band pins (Base10 :76-77 normal / Base11 :56-57 reversed)
	// ---------------------------------------------------------------------------

	@Test
	public void sizeBandsPerMode() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();

		assertEquals(16, tTrans.getEnergySizeInputMin(TD.Energy.EU, FRONT), "step-down input min = inRec/2");
		assertEquals(32, tTrans.getEnergySizeInputRecommended(TD.Energy.EU, FRONT), "input rec = inRec = 32");
		assertEquals(64, tTrans.getEnergySizeInputMax(TD.Energy.EU, FRONT), "input max = inRec*2");
		assertEquals(4, tTrans.getEnergySizeOutputMin(TD.Energy.EU, FRONT), "step-down output min = V[0]/2");
		assertEquals(8, tTrans.getEnergySizeOutputRecommended(TD.Energy.EU, FRONT));
		assertEquals(16, tTrans.getEnergySizeOutputMax(TD.Energy.EU, FRONT));

		assertEquals(0, tTrans.getEnergySizeInputMin(TD.Energy.RU, FRONT), "EU-only row: wrong type answers 0");
		assertEquals(0, tTrans.getEnergySizeOutputMax(TD.Energy.RU, FRONT));

		GT6ElectricTransformerBlockEntity tUp = transformerUp();
		assertEquals(1, tUp.getEnergySizeInputMin(TD.Energy.EU, FRONT), "step-up input min = 1 (outMin 4 <= 8 fold)");
		assertEquals(32, tUp.getEnergySizeInputRecommended(TD.Energy.EU, FRONT), "input rec stays inRec 32");
		assertEquals(64, tUp.getEnergySizeInputMax(TD.Energy.EU, FRONT), "input max = max(inRec, outMax*mult) = 64");
		assertEquals(24, tUp.getEnergySizeOutputMin(TD.Energy.EU, FRONT), "step-up output min = inRec*3/4 = 24");
		assertEquals(32, tUp.getEnergySizeOutputRecommended(TD.Energy.EU, FRONT), "output rec = inRec 32");
		assertEquals(64, tUp.getEnergySizeOutputMax(TD.Energy.EU, FRONT), "output max = inRec*2 = 64");
	}

	// ---------------------------------------------------------------------------
	// 6. the waste=F input throttle (Base10 :151 — live here, folded on the dynamo rows)
	// ---------------------------------------------------------------------------

	@Test
	public void pluggedNetPausesTheInputSide() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		EuSink tSink = wire(tTrans);
		tSink.refuse = true;

		assertEquals(1, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true), "the capacitor accepts while the door state is neutral");
		tTrans.onTick(11, true); // canEmit = T, but nothing accepted → mActive = F

		// Base10 :151 with waste = F: (mEmitsEnergy == mCanEmitEnergy) is FALSE → the
		// input side pauses — the transformer stops eating while its output is plugged
		assertFalse(tTrans.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, false), "the :151 throttle pauses the input");
		assertTrue(tTrans.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, true), "the theoretical face stays connected (conductor stability)");
	}

	// ---------------------------------------------------------------------------
	// 7. the oversize/capacity pins (Stats :57-66 + the Base10 :140-148 ladder)
	// ---------------------------------------------------------------------------

	@Test
	public void oversizePacketConsumesAllAndStrikes() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		wire(tTrans);

		assertEquals(128, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 128, 128, true), "the oversize offer counts as used");
		assertEquals(0, tTrans.mStorage, "the Stats :57-61 leg consumed ALL");
		assertEquals(1, tTrans.mExplosionPrevention, "one soft strike recorded (100 then overcharge)");
	}

	@Test
	public void fullCapacitorRefusesMore() {
		GT6ElectricTransformerBlockEntity tTrans = transformerUp();
		wire(tTrans);

		for (int i = 0; i < 8; i++) tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true);
		assertEquals(64, tTrans.mStorage, "the capacitor caps at NBT_INPUT*2 = 64");
		assertEquals(0, tTrans.doEnergyInjection(TD.Energy.EU, front(), 8, 1, true), "the Stats :62 full gate refuses (0 = refundable)");
	}

	// ---------------------------------------------------------------------------
	// 8. the stop pin (the Base10 mStopped acceptance formula, the dynamo form)
	// ---------------------------------------------------------------------------

	@Test
	public void stoppedTransformerAcceptsNothingAndDoesNotConvert() {
		GT6ElectricTransformerBlockEntity tTrans = transformer();
		EuSink tSink = wire(tTrans);
		tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true);

		tTrans.mStopped = true;
		assertFalse(tTrans.isEnergyAcceptingFrom(TD.Energy.EU, FRONT, false), "the stopped acceptance formula");
		assertEquals(0, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 32, 1, true), "the doInject guard refuses");
		tTrans.onTick(11, true);
		assertTrue(tSink.packets.isEmpty(), "the stopped converter does not discharge");
	}

	// ---------------------------------------------------------------------------
	// 9-bis. the p35 ladder pins (the :882-:889 declared subset — 梯数按上游声明口径)
	// ---------------------------------------------------------------------------

	/**
	 * THE LADDER PIN (task p35-energy-tail-machines): the upstream declared subset is
	 * NINE rows (:881-:889, meta ids 10040-10048, pairs V[i+1]→V[i]); the p31
	 * single-tier ruling is the hand-tool domain and does NOT apply (对照申报) — the
	 * machine ladder pins at the full declared count.
	 */
	@Test
	public void ladderPinsTheUpstreamDeclaredSubset() {
		assertEquals(9, GT6ElectricTransformers.ROWS.size(), "the :881-:889 declared row count");
		for (int i = 0; i < 9; i++) {
			GT6ElectricTransformers.TransformerRow tRow = GT6ElectricTransformers.ROWS.get(i);
			assertEquals(10040 + i, tRow.metaId(), "row " + i + ": the upstream meta id");
			assertEquals(i, tRow.tier(), "row " + i + ": the ladder index");
			assertEquals(GTWireSpecs.VN[i] + "-" + GTWireSpecs.VN[i + 1], tRow.voltagePair(), "row " + i + ": the VN pair");
		}
		// the tier-pair arithmetic per row: NBT_INPUT V[i+1], NBT_OUTPUT V[i], multiplier 4
		for (int i = 0; i < 9; i++) {
			GT6ElectricTransformerBlockEntity tTrans = new GT6ElectricTransformerBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), i);
			assertEquals(GTWireSpecs.V[i + 1], tTrans.vHigh, "row " + i + ": NBT_INPUT = V[i+1]");
			assertEquals(GTWireSpecs.V[i], tTrans.vLow, "row " + i + ": NBT_OUTPUT = V[i]");
			assertEquals(4, tTrans.multiplier, "row " + i + ": NBT_MULTIPLIER = 4");
		}
		// the casing ladder (upstream MT.java:3691 members [0..8], the existence assertion)
		assertEquals(9, GT6ElectricTransformers.CASING_LADDER.size());
		assertEquals(gregapi.data.MT.Trinitanium, GT6ElectricTransformers.CASING_LADDER.get(8).get(), "Electric_T[8] = Trinitanium");
		assertEquals(gregapi.data.MT.Os, GT6ElectricTransformers.CASING_LADDER.get(7).get(), "Electric_T[7] = Os");
		assertEquals(gregapi.data.MT.Ir, GT6ElectricTransformers.CASING_LADDER.get(6).get(), "Electric_T[6] = Ir");
	}

	/** The higher-tier conservation anchor: the HV-EV row (tier 3, NBT_INPUT V[4]=2048 → NBT_OUTPUT V[3]=512) — one 2048 EU packet in, FOUR 512 EU packets out. */
	@Test
	public void hvEvRowStepDownConservesTierPackets() {
		GT6ElectricTransformerBlockEntity tTrans = new GT6ElectricTransformerBlockEntity(sType, POS, Blocks.STONE.defaultBlockState(), 3);
		EuSink tSink = wire(tTrans);

		assertEquals(1, tTrans.doEnergyInjection(TD.Energy.EU, FRONT, 2048, 1, true), "one 2048 EU packet accepted (band 1024..4096)");
		assertEquals(2048, tTrans.mStorage);
		tTrans.onTick(11, true);

		assertEquals(1, tSink.packets.size());
		assertEquals(512, tSink.packets.get(0)[0], "the packet SIZE is V[3] = 512");
		assertEquals(4, tSink.packets.get(0)[1], "the packet COUNT is the row multiplier 4");
		assertEquals(0, tTrans.mStorage, "2048 in = 4 x 512 out — conservation on the higher rung");
	}

	// ---------------------------------------------------------------------------
	// 9. the row + recipe MATERIAL-LOCK pins (decisions.p28-ulv-tier-rulings)
	// ---------------------------------------------------------------------------

	@Test
	public void rowColumnsPinThe881LoaderLine() {
		assertEquals(32, GT6ElectricTransformerBlockEntity.VOLTAGE_HIGH, "NBT_INPUT = V[1]");
		assertEquals(8, GT6ElectricTransformerBlockEntity.VOLTAGE_LOW, "NBT_OUTPUT = V[0]");
		assertEquals(4, GT6ElectricTransformerBlockEntity.MULTIPLIER, "NBT_MULTIPLIER = V[1]/V[0]");
		assertEquals(64, transformer().cap, "the capacitor = NBT_INPUT*2 (the row-0 instance pair)");
		assertFalse(GT6ElectricTransformerBlockEntity.WASTE_ENERGY, "NBT_WASTE_ENERGY = F (Loader :881)");
		assertEquals(10040, GT6ElectricTransformers.ROWS.get(0).metaId(), "the upstream meta id (the parity column)");
		assertEquals("electric_transformer", GT6ElectricTransformers.ROWS.get(0).path(), "the BET path");
	}

	/**
	 * THE CONDITIONAL-ENTRY LOCK (the user ruling, decisions.p28-ulv-tier-rulings
	 * transformer_ruling): the recipe carriers are the LV-era materials — the
	 * galvanized-steel machine casing (Electric_T[1], the declared deviation from the
	 * :881 TinAlloy = Electric_T[0] lowest-price housing), the LV-era copper wires
	 * (wireGt01/wireGt04 → the fine_wires/copper tag fold) and the iron double plates.
	 * The pairs must EXIST in the registration universe (the datagen .get() would
	 * otherwise throw at runData — this pin is the offline half).
	 */
	@Test
	public void recipeMaterialLockIsLvEra() {
		// the casing lock: casingSmall(SteelGalvanized) — NOT TinAlloy (the :881 deviation)
		assertTrue(GT6ElectricTransformers.CASING_LOCK_MATERIAL.get() == gregapi.data.MT.SteelGalvanized,
				"the casing lock = Electric_T[1] galvanized steel (the LV-era rung, MT.java:3691)");
		assertFalse(GT6ElectricTransformers.CASING_LOCK_MATERIAL.get() == gregapi.data.MT.TinAlloy,
				"the lock must NOT pin the upstream :881 TinAlloy housing");
		assertEquals(gregapi.data.OP.casingSmall, GT6ElectricTransformers.CASING_LOCK_PREFIX.get(),
				"the casingMachine column folds to casingSmall (no port item row — the static-storage fold precedent)");

		// the wire carrier EXISTS in the registration universe (the GTMaterialItems.enumerate walk)
		boolean tWire = false;
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == gregapi.data.OP.wireFine && tPair.material() == gregapi.data.MT.Copper) tWire = true;
		}
		assertTrue(tWire, "wireFine(Copper) must exist in the registration universe (the wireGt01/wireGt04 fold carrier)");
		assertEquals("fine_wires/copper", GT6ElectricTransformers.WIRE_TAG_PATH, "the copper fine-wire tag (the LV-era wire)");
		assertEquals("double_plates/iron", GT6ElectricTransformers.PLATE_TAG_PATH, "the iron double-plate tag (the :881 'I' column verbatim)");
		assertArrayEquals(new String[] {"WIW", "XM ", "WIW"}, GT6ElectricTransformers.RECIPE_PATTERN,
				"the :881 shape with the unbound 'm' dead cell folded to a space");
	}
}
