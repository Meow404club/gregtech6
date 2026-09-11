/**
 * The machine-domain NBT_MATERIAL row census (task p27-machine-material-tint-fidelity,
 * the A data leg): every machine row mirrors the upstream {@code aMat = MT.DATA.*_T[n]}
 * registration column (Loader_MultiTileEntities.java:1288-1318/:1343/:1373/:1379/:1398/
 * :1406/:1425 — the burning boxes :519-548/:619-704), because upstream derives the
 * unpainted render colour from exactly that column (MultiTileEntityClassContainer.java:51,
 * {@code getRGBInt(material.fRGBaSolid)}). The colour maths itself pins here too:
 * {@link GTBasicMachineBlock#materialColor} is the UT.Code.getRGBInt :1580-1582 form over
 * OreDictMaterial.java:111 (Cu orange-red / Steel gray-white — the research.p27-machine-
 * tint-reresearch representative pair).
 *
 * <p>OFFLINE posture: the material system boots through
 * {@code GTMaterialItems.initMaterials()} (the GTWireTintTest shape). The BLOCK-instance
 * half ({@code GTBasicMachineBlock.material()} / {@code GTOvenBlock.material()} /
 * {@code BurningBoxBlock.material()}) needs live gt6 registrations (the intrusive-holder
 * wall, GTMachinesOvenLadderTest:50-56) — the row tables and the pure colour seam are the
 * offline assertion surface; the live rows ride the runServer registration gate.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;

public class GTMachinesMaterialRowTest {

	@BeforeAll
	public static void bootOfflineThenMaterials() {
		// the vanilla bootstrap first (the GTBasicMachineBlockDispatchTest shape — the
		// row-table/block classes carry BlockProperty/DeferredRegister statics), then the
		// material system (the GTWireTintTest shape)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; the registries are frozen-ready.
		}
		GTMaterialItems.initMaterials();
	}

	// ------------------------------------------------------------------
	// the three ladders vs the upstream MT.java:3689-3691 [1..4] members
	// ------------------------------------------------------------------

	@Test
	public void theLaddersCarryTheUpstreamTableMembers() {
		// Heat_T[1..4] = ANY.Steel / Invar / Ti / TungstenCarbide (upstream MT.java:3689)
		assertSame(gregapi.data.MT.Steel          , GTMachines.HEAT_T_LADDER.get(0).get());
		assertSame(gregapi.data.MT.Invar          , GTMachines.HEAT_T_LADDER.get(1).get());
		assertSame(gregapi.data.MT.Ti             , GTMachines.HEAT_T_LADDER.get(2).get());
		assertSame(gregapi.data.MT.TungstenCarbide, GTMachines.HEAT_T_LADDER.get(3).get());
		// Kinetic_T[1..4] = Bronze / ANY.Steel / Ti / TungstenSteel (upstream MT.java:3690)
		assertSame(gregapi.data.MT.Bronze        , GTMachines.KINETIC_T_LADDER.get(0).get());
		assertSame(gregapi.data.MT.Steel         , GTMachines.KINETIC_T_LADDER.get(1).get());
		assertSame(gregapi.data.MT.Ti            , GTMachines.KINETIC_T_LADDER.get(2).get());
		assertSame(gregapi.data.MT.TungstenSteel , GTMachines.KINETIC_T_LADDER.get(3).get());
		// Electric_T[1..4] = SteelGalvanized / Al / StainlessSteel / Cr (upstream MT.java:3691)
		// — the Canner rows (the LV/MV/HV/EV display words ride the name column, the
		// NBT_MATERIAL column the Electric_T material, upstream :1379-1382)
		assertSame(gregapi.data.MT.SteelGalvanized, GTMachines.ELECTRIC_T_LADDER.get(0).get());
		assertSame(gregapi.data.MT.Al             , GTMachines.ELECTRIC_T_LADDER.get(1).get());
		assertSame(gregapi.data.MT.StainlessSteel , GTMachines.ELECTRIC_T_LADDER.get(2).get());
		assertSame(gregapi.data.MT.Cr             , GTMachines.ELECTRIC_T_LADDER.get(3).get());
	}

	// ------------------------------------------------------------------
	// the row census — every MachineRow/OvenRow mirrors its ladder column
	// ------------------------------------------------------------------

	/** Every tier row of one family rides the SAME ladder (the row material == ladder[tier]). */
	private static void assertFamilyRides(List<GTBasicMachineBlock.MachineRow> aRows,
			List<java.util.function.Supplier<OreDictMaterial>> aLadder, String aFamily) {
		for (int i = 0; i < aRows.size(); i++) {
			assertSame(aLadder.get(i).get(), aRows.get(i).material().get(),
					aFamily + " row " + i + " carries its NBT_MATERIAL tier column");
		}
	}

	@Test
	public void everyMachineRowCarriesTheUpstreamMaterialColumn() {
		// the Heat_T families (Oven :1288-1291, Dryer :1477-1480, Distillery :1398-1401,
		// Extruder :1406-1409)
		assertFamilyRides(GTMachines.DRYER_ROWS    , GTMachines.HEAT_T_LADDER, "dryer");
		assertFamilyRides(GTMachines.DISTILLERY_ROWS, GTMachines.HEAT_T_LADDER, "distillery");
		assertFamilyRides(GTMachines.EXTRUDER_ROWS , GTMachines.HEAT_T_LADDER, "extruder");
		for (int i = 0; i < 4; i++) {
			assertSame(GTMachines.HEAT_T_LADDER.get(i).get(), GTMachines.OVEN_ROWS.get(i).material().get(),
					"oven row " + i + " carries the Heat_T tier column (:1288-1291)");
		}
		// the Kinetic_T families (Shredder/Crusher/Lathe :1294-1309, Sifter :1312-1315,
		// Compressor :1343-1346, Wiremill :1373-1376, Press :1425-1428)
		assertFamilyRides(GTMachines.SIFTER_ROWS    , GTMachines.KINETIC_T_LADDER, "sifter");
		assertFamilyRides(GTMachines.COMPRESSOR_ROWS, GTMachines.KINETIC_T_LADDER, "compressor");
		assertFamilyRides(GTMachines.WIREMILL_ROWS  , GTMachines.KINETIC_T_LADDER, "wiremill");
		assertFamilyRides(GTMachines.PRESS_ROWS     , GTMachines.KINETIC_T_LADDER, "press");
		// the Electric_T family (the Canner, :1379-1382)
		assertFamilyRides(GTMachines.CANNER_ROWS    , GTMachines.ELECTRIC_T_LADDER, "canner");
	}

	// ------------------------------------------------------------------
	// the burning-box rows (:519-548/:619-704 — the per-row aMat column)
	// ------------------------------------------------------------------

	@Test
	public void theBurningBoxMaterialsMapTheLoaderAMatColumn() {
		assertSame(gregapi.data.MT.Pb            , GT6BurningBoxes.MAT_LEAD.mat().get());
		assertSame(gregapi.data.MT.Bi            , GT6BurningBoxes.MAT_BISMUTH.mat().get());
		assertSame(gregapi.data.MT.Bronze        , GT6BurningBoxes.MAT_BRONZE.mat().get());
		assertSame(gregapi.data.MT.ArsenicCopper , GT6BurningBoxes.MAT_ARSENIC_COPPER.mat().get());
		assertSame(gregapi.data.MT.ArsenicBronze , GT6BurningBoxes.MAT_ARSENIC_BRONZE.mat().get());
		assertSame(gregapi.data.MT.Invar         , GT6BurningBoxes.MAT_INVAR.mat().get());
		assertSame(gregapi.data.MT.Steel         , GT6BurningBoxes.MAT_STEEL.mat().get());
		assertSame(gregapi.data.MT.Cr            , GT6BurningBoxes.MAT_CHROMIUM.mat().get());
		assertSame(gregapi.data.MT.Ti            , GT6BurningBoxes.MAT_TITANIUM.mat().get());
		assertSame(gregapi.data.MT.Netherite     , GT6BurningBoxes.MAT_NETHERITE.mat().get());
		assertSame(gregapi.data.MT.W             , GT6BurningBoxes.MAT_TUNGSTEN.mat().get());
		assertSame(gregapi.data.MT.TungstenSteel , GT6BurningBoxes.MAT_TUNGSTENSTEEL.mat().get());
		assertSame(gregapi.data.MT.Ta4HfC5       , GT6BurningBoxes.MAT_TANTALUM_HAFNIUM_CARBIDE.mat().get());
		// the Brick row carries NBT_MATERIAL = MT.Brick upstream (:519)
		assertSame(gregapi.data.MT.Brick         , GT6BurningBoxes.BRICK_ROW.material().mat().get());
		// every ladder row resolves a material through its BoxMaterial column
		for (GT6BurningBoxes.BurningBoxRow tRow : GT6BurningBoxes.allRows()) {
			assertNotNull(tRow.material().mat().get(), tRow.path() + " resolves a live material");
		}
	}

	// ------------------------------------------------------------------
	// the colour seam — UT.Code.getRGBInt over fRGBaSolid (the registration
	// derivation of MultiTileEntityClassContainer.java:51)
	// ------------------------------------------------------------------

	@Test
	public void materialColorIsTheUpstreamFRGBaSolidEncoding() {
		// null material = upstream UNCOLORED (CS.java:327, the Paintable:50 fallback)
		assertEquals(0xFFFFFF, GTBasicMachineBlock.materialColor(null), "the material-less arm is UNCOLORED white");
		// MT.NULL resolves white too — the no-tint identity (the laser-style negative face)
		assertEquals(0xFFFFFF, GTBasicMachineBlock.materialColor(gregapi.data.MT.NULL), "MT.NULL stays the white identity");
		// the representative pair (research.p27-machine-tint-reresearch): Cu orange-red
		// (copper() 255,130,90, root MT.java:1004 — the MT.java:1423 Cu assignment),
		// Steel gray-white (alloymachore 130,130,130) — pinned to the fRGBaSolid bodies
		// AND the derivation
		assertEquals(0xFF825A, GTBasicMachineBlock.materialColor(gregapi.data.MT.Cu), "Cu 255,130,90");
		assertEquals(0x828282, GTBasicMachineBlock.materialColor(gregapi.data.MT.Steel), "Steel 130,130,130");
		for (OreDictMaterial tMat : List.of(gregapi.data.MT.Cu, gregapi.data.MT.Steel, gregapi.data.MT.TungstenCarbide)) {
			int tColor = GTBasicMachineBlock.materialColor(tMat);
			assertEquals(tMat.fRGBaSolid[0], (tColor >> 16) & 0xFF, tMat.mNameLocal + " R rides fRGBaSolid");
			assertEquals(tMat.fRGBaSolid[1], (tColor >> 8) & 0xFF, tMat.mNameLocal + " G rides fRGBaSolid");
			assertEquals(tMat.fRGBaSolid[2], tColor & 0xFF, tMat.mNameLocal + " B rides fRGBaSolid");
		}
	}

	// ------------------------------------------------------------------
	// the dispatch — material-less vanilla states resolve null (the white
	// identity that keeps the P23 barrel registration byte-identical)
	// ------------------------------------------------------------------

	@Test
	public void materialLessBlocksResolveNull() {
		assertNull(GTBasicMachineBlock.materialOf(null), "a null block is material-less");
		assertNull(GTBasicMachineBlock.materialOf(net.minecraft.world.level.block.Blocks.BRICKS),
				"vanilla states are material-less (the offline fixture block — and the barrel domain by the same gate)");
	}
}
