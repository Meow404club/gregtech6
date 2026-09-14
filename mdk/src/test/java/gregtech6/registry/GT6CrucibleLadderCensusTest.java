package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.CruciblePhysics;

/**
 * The crucible 8-material ladder census (task p29-w3-distill-crucible ③ — ACCEPTANCE ③,
 * the OFFLINE half; the live half is the p29_w3_crucible_ladder RCON chain):
 * <ul>
 * <li>the eight rungs (the Loader :1270-1277 meta set 17302..17312 complete with the Steel
 *     17309 the P26 card landed) and the ACIDPROOF column — T on StainlessSteel/Tungsten/
 *     Adamantium ONLY;</li>
 * <li>the per-rung dedicated wall identity (the wallBlockOf map — the NBT_DESIGN column as
 *     a Block) and the wall BET valid list covering all eight;</li>
 * <li>the physics ceiling the ladder actually swaps — temperatureMax = the SHELL material
 *     melting point × 1.10 (the P26 CruciblePhysics LARGE parameter face, ZERO new
 *     physics — this pins the parameter consumption per material);</li>
 * <li>the twin tower rows (:1226-1227) ride the same registration home.</li>
 * </ul>
 */
class GT6CrucibleLadderCensusTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		MaterialRegistry.INSTANCE.open();
		MT.init();
		MaterialRegistry.INSTANCE.close();
	}

	@Test
	public void theLadderIsTheEightUpstreamRungs() {
		assertEquals(8, GT6Crucibles.CRUCIBLE_ROWS.size(), "the Loader :1270-1277 ladder, Steel first");
		java.util.Set<Integer> tIds = new java.util.HashSet<>();
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) tIds.add(tRow.metaId());
		// ACCEPTANCE ③ — 17302..17312 all present (Steel 17309 was the P26 single rung)
		assertTrue(tIds.containsAll(java.util.Set.of(17302, 17303, 17304, 17305, 17306, 17307, 17309, 17312)),
				"the 8-material meta set, got " + tIds);
		assertEquals(8, GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.size(), "one controller block per rung");
	}

	@Test
	public void theAcidProofColumnIsStainlessTungstenAdamantiumOnly() {
		// the Loader NBT_ACIDPROOF column, read row by row
		assertAcidProof("crucible_steel", false);                       // :1270
		assertAcidProof("crucible_stainless_steel", true);              // :1271
		assertAcidProof("crucible_invar", false);                       // :1272
		assertAcidProof("crucible_titanium", false);                    // :1273
		assertAcidProof("crucible_tungstensteel", false);               // :1274
		assertAcidProof("crucible_tungsten", true);                     // :1275
		assertAcidProof("crucible_tantalum_hafnium_carbide", false);    // :1276
		assertAcidProof("crucible_adamantium", true);                   // :1277
	}

	private static void assertAcidProof(String aPath, boolean aExpected) {
		GT6Crucibles.CrucibleRow tRow = rowByPath(aPath);
		assertNotNull(tRow, "the rung exists: " + aPath);
		assertEquals(aExpected, tRow.acidProof(), "the ACIDPROOF column of " + aPath);
	}

	private static GT6Crucibles.CrucibleRow rowByPath(String aPath) {
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			if (tRow.path().equals(aPath)) return tRow;
		}
		return null;
	}

	@Test
	public void everyRungCarriesItsOwnWallPath() {
		// the NBT_DESIGN column as the row's wallPath — eight DISTINCT identities (the Block
		// resolution itself is the live p29_w3_crucible_ladder arm: the frozen registry keeps
		// the DeferredRegister handles offline-unbound)
		java.util.Set<String> tPaths = new java.util.HashSet<>();
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			assertNotNull(tRow.wallPath(), "the wall of " + tRow.path());
			assertTrue(tRow.wallPath().startsWith("crucible_") && tRow.wallPath().endsWith("_wall"),
					"the crucible-wall path form of " + tRow.path());
			tPaths.add(tRow.wallPath());
		}
		assertEquals(8, tPaths.size(), "eight distinct wall identities");
		for (String tExpected : new String[] {"crucible_stainless_steel_wall", "crucible_invar_wall", "crucible_titanium_wall",
				"crucible_tungstensteel_wall", "crucible_tungsten_wall", "crucible_tantalum_hafnium_carbide_wall",
				"crucible_adamantium_wall"}) {
			assertTrue(GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.containsKey(tExpected), "the ladder wall registered: " + tExpected);
		}
	}

	@Test
	public void theShellMaterialIsWhatTheLadderSwaps() {
		// the row material column — the CruciblePhysics ceiling input
		assertSame(MT.Steel, rowByPath("crucible_steel").material(), ":1270");
		assertSame(MT.StainlessSteel, rowByPath("crucible_stainless_steel").material(), ":1271");
		assertSame(MT.Invar, rowByPath("crucible_invar").material(), ":1272");
		assertSame(MT.Ti, rowByPath("crucible_titanium").material(), ":1273");
		assertSame(MT.TungstenSteel, rowByPath("crucible_tungstensteel").material(), ":1274");
		assertSame(MT.W, rowByPath("crucible_tungsten").material(), ":1275");
		assertSame(MT.Ta4HfC5, rowByPath("crucible_tantalum_hafnium_carbide").material(), ":1276");
		assertSame(MT.Ad, rowByPath("crucible_adamantium").material(), ":1277");
	}

	@Test
	public void theHeatCeilingIsTheMeltPointTimesTheLargeBonus() {
		// the P26 parameter face consumed per material — ZERO new physics, the ladder only
		// swaps the input: temperatureMax = mMeltingPoint * 1.10 (Params.LARGE.heatResistanceBonus)
		double tBonus = CruciblePhysics.Params.LARGE.heatResistanceBonus();
		assertEquals(1.10, tBonus, 1e-9, "the LARGE wall heat bonus (the small Smeltery carries 1.25)");
		for (GT6Crucibles.CrucibleRow tRow : GT6Crucibles.CRUCIBLE_ROWS) {
			OreDictMaterial tShell = tRow.material();
			assertNotNull(tShell, "the shell of " + tRow.path());
			long tExpected = (long) (tShell.mMeltingPoint * tBonus);
			assertEquals(tExpected, CruciblePhysics.temperatureMax(tShell, tBonus),
					"the ceiling of " + tRow.path() + " rides the shell melt point x 1.10");
			// the ceiling rises with the shell: the Ta4HfC5 rung out-ceils the Steel rung
			assertTrue(tExpected > 0, "a real melting point behind " + tRow.path());
		}
		long tSteelCeiling = CruciblePhysics.temperatureMax(MT.Steel, tBonus);
		long tTa4HfC5Ceiling = CruciblePhysics.temperatureMax(MT.Ta4HfC5, tBonus);
		assertTrue(tTa4HfC5Ceiling > tSteelCeiling, "the exotic rung melts higher than Steel");
		assertNotEquals(tSteelCeiling, tTa4HfC5Ceiling, "the ladder actually re-ladders the ceiling");
		assertFalse(CruciblePhysics.Params.LARGE.heatResistanceBonus() == CruciblePhysics.Params.SMALL.heatResistanceBonus(),
				"the LARGE bonus (1.10) is the crucible-multiblock form, not the small Smeltery 1.25");
	}

	@Test
	public void theTwinTowerRowsRideTheSameRegistrationHome() {
		assertEquals(2, GT6Distillation.ROWS.size(), ":1226-1227");
		assertEquals(2, GT6Distillation.TOWER_BLOCKS_BY_PATH.size());
		assertNotNull(GT6Distillation.TOWER_BLOCKS_BY_PATH.get("distillation_tower"));
		assertNotNull(GT6Distillation.TOWER_BLOCKS_BY_PATH.get("cryo_distillation_tower"));
	}
}
