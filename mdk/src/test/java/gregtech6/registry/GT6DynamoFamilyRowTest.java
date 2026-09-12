package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.tileentity.energy.GT6DynamoBlockEntityTestHarness;

/**
 * The dynamo-family row census (task p28-c-dynamo-family-be) — the
 * GTMachinesMaterialRowTest posture over the two dynamo ladders: the row tables mirror
 * the upstream registration columns (Loader_MultiTileEntities.java:946-957), the ratio
 * pairs are EXACT (2.75 = 11/4 and 0.6875 = 22/32, the load-bearing registration
 * constants the whole W0 pushPacketTrain design hangs on), the Electric_T[5] existence
 * assertion (the wave-card W1 gate — the modern GTMachines ladder stopped at [4]) and
 * the root {@code MT.FLUX_T} binding this card's root commit materialized.
 */
public class GT6DynamoFamilyRowTest {

	@BeforeAll
	static void bootOfflineThenMaterials() {
		// the vanilla bootstrap first, then the material system — the
		// GTMachinesMaterialRowTest verbatim posture
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; the registries are frozen-ready.
		}
		GTMaterialItems.initMaterials();
	}

	// ------------------------------------------------------------------
	// the Flux rows vs the Loader :953-957 columns
	// ------------------------------------------------------------------

	@Test
	public void fluxRowsCarryTheUpstreamIdsInOrder() {
		assertEquals(5, GT6FluxDynamos.ROWS.size());
		for (int i = 0; i < 5; i++) {
			GT6FluxDynamos.FluxRow tRow = GT6FluxDynamos.ROWS.get(i);
			assertEquals(11111 + i, tRow.metaId(), "the upstream meta id of row " + i);
			assertEquals(i, tRow.tier());
		}
		assertEquals("flux_dynamo", GT6FluxDynamos.ROWS.get(0).path(), "the T1 bare path");
		assertEquals("flux_dynamo_t5", GT6FluxDynamos.ROWS.get(4).path());
	}

	@Test
	public void fluxRatioPairsAreExactlyTwoPointSevenFive() {
		long[][] tRows = GT6DynamoBlockEntityTestHarness.DYNAMO_ROWS;
		assertEquals(5, tRows.length);
		for (long[] tRow : tRows) {
			assertEquals(tRow[1] * 4, tRow[0] * 11, tRow[0] + " RU: the pair must be the exact rational 11/4 = 2.75");
		}
		// and the BE columns are the same table (no drift between the registry doc and the core)
		for (int i = 0; i < 5; i++) {
			assertEquals(tRows[i][0], gregtech6.tileentity.energy.GT6FluxDynamoBlockEntity.INPUTS[i]);
			assertEquals(tRows[i][1], gregtech6.tileentity.energy.GT6FluxDynamoBlockEntity.OUTPUTS[i]);
		}
	}

	// ------------------------------------------------------------------
	// the Electric rows vs the Loader :946-950 columns + the Electric_T[5] gate
	// ------------------------------------------------------------------

	@Test
	public void electricRowsCarryTheUpstreamIdsAndVoltageWords() {
		assertEquals(5, GT6ElectricDynamos.ROWS.size());
		List<String> tWords = GT6ElectricDynamos.ROWS.stream().map(GT6ElectricDynamos.ElectricRow::voltageWord).toList();
		assertEquals(List.of("LV", "MV", "HV", "EV", "IV"), tWords, "the VN[1..5] display words (upstream CS.java:154)");
		for (int i = 0; i < 5; i++) {
			assertEquals(10111 + i, GT6ElectricDynamos.ROWS.get(i).metaId(), "the upstream meta id of row " + i);
		}
	}

	@Test
	public void electricRatioPairsAreExactlyZeroPointSixEightSevenFive() {
		long[][] tRows = GT6DynamoBlockEntityTestHarness.ELECTRIC_ROWS;
		assertEquals(5, tRows.length);
		for (long[] tRow : tRows) {
			assertEquals(tRow[1] * 32, tRow[0] * 22, tRow[0] + " RU: the pair must be the exact rational 22/32 = 0.6875");
		}
		for (int i = 0; i < 5; i++) {
			assertEquals(tRows[i][0], gregtech6.tileentity.energy.GT6ElectricDynamoBlockEntity.INPUTS[i]);
			assertEquals(tRows[i][1], gregtech6.tileentity.energy.GT6ElectricDynamoBlockEntity.OUTPUTS[i]);
		}
	}

	@Test
	public void electricT5MemberExistsAndIsTitanium() {
		// the wave-card W1 gate: upstream Electric_T[5] = Ti (upstream MT.java:3691 — the
		// modern GTMachines.ELECTRIC_T_LADDER stopped at the canner rows' [1..4]); the
		// dynamo family carries its own five-member ladder, [4] (0-based) = Electric_T[5]
		assertEquals(5, GT6ElectricDynamos.ELECTRIC_T_LADDER.size());
		OreDictMaterial tTier5 = GT6ElectricDynamos.ELECTRIC_T_LADDER.get(4).get();
		assertNotNull(tTier5, "Electric_T[5] must resolve");
		assertSame(MT.Ti, tTier5, "Electric_T[5] = Ti (upstream MT.java:3691)");
		assertSame(MT.SteelGalvanized, GT6ElectricDynamos.ELECTRIC_T_LADDER.get(0).get());
		assertSame(MT.Cr, GT6ElectricDynamos.ELECTRIC_T_LADDER.get(3).get());
	}

	// ------------------------------------------------------------------
	// the root FLUX_T ladder binding (the root commit of this card)
	// ------------------------------------------------------------------

	@Test
	public void rootFluxTLadderBindsTheUpstreamMembers() {
		// upstream MT.java:3692: Flux_T[0..5] = Sn / Pb / Invar / Electrum / EnderiumBase /
		// Enderium — the root array binds at the END of MT.init() (the late-binding ruling),
		// and initMaterials() has run it
		assertNotNull(MT.FLUX_T);
		assertEquals(6, MT.FLUX_T.length, "the [0..5] prefix — the upstream [6..15] padding cropped with declaration");
		assertSame(MT.Sn, MT.FLUX_T[0]);
		assertSame(MT.Pb, MT.FLUX_T[1]);
		assertSame(MT.Invar, MT.FLUX_T[2]);
		assertSame(MT.Electrum, MT.FLUX_T[3]);
		assertSame(MT.EnderiumBase, MT.FLUX_T[4]);
		assertSame(MT.Enderium, MT.FLUX_T[5]);
		// the family ladder reads THROUGH the root array, lazily
		for (int i = 0; i < 5; i++) {
			assertSame(MT.FLUX_T[i + 1], GT6FluxDynamos.FLUX_T_LADDER.get(i).get(), "row " + i + " rides the root ladder");
		}
	}

	@Test
	public void allInputColumnsSitAboveTheSixteenLine() {
		// the Base10:76 branch pin: tInput > 16 every row (and takesAnyLowerSize() = F), so
		// the input minimum is always tInput/2 — the white-burn door the tests drive
		for (long[] tRow : GT6DynamoBlockEntityTestHarness.DYNAMO_ROWS) assertTrue(tRow[0] > 16);
		for (long[] tRow : GT6DynamoBlockEntityTestHarness.ELECTRIC_ROWS) assertTrue(tRow[0] > 16);
	}
}
