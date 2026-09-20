package gregtech6.registry;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The id686 registration guard for the QU machine pair (task p32-qu-scanner-replicator
 * acceptance ①: 2 families, 4 tiers — the Molecular Scanner T3 single :1551 and the Matter
 * Replicator T1-T3 rungs :1556-1558), the GT6LogisticsRegistrationTest two-leg form: the
 * FML-booted leg probes the real registries (block/item/BET faces, the BET mounted on each
 * block, the factory building a row-carrying machine BE — the id686 failure shape), the
 * offline leg pins the registration DATA (paths, metaIds, tiers, textures, the QU carrier,
 * the map suppliers, the window selectors).
 */
public class GT6QuMachinesRegistrationTest {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the block-construction write window (the logistics form) — irrelevant here (the
		// GTMachines registers ride its own static init), kept for the boot symmetry
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GTMaterialItems.initMaterials(); // idempotent — the row material supplier reads MT.Osmiridium
	}

	/** The 4 rungs: 1 scanner + 3 replicators, paths/metaIds/rows/tier words in upstream order. */
	@Test
	public void theTwoFamiliesCarryFourRungsInUpstreamOrder() {
		assertEquals(1, GTMachines.MOLECULAR_SCANNER_ROWS.size(), "the scanner is the T3-only single (:1551; T1/T2/T4/T5 commented upstream)");
		assertEquals("molecular_scanner_t3", GTMachines.MOLECULAR_SCANNER_ROWS.get(0).path(), "the scanner path");
		assertEquals(20423, GTMachines.MOLECULAR_SCANNER_ROWS.get(0).metaId(), "the :1551 numeric id");
		assertEquals(3, GTMachines.REPLICATOR_ROWS.size(), "the replicator T1-T3 rungs (:1556-1558; T4/T5 :1559-1560 outside the card scope)");
		assertEquals("replicator", GTMachines.REPLICATOR_ROWS.get(0).path());
		assertEquals("replicator_t2", GTMachines.REPLICATOR_ROWS.get(1).path());
		assertEquals("replicator_t3", GTMachines.REPLICATOR_ROWS.get(2).path());
		assertEquals(20431, GTMachines.REPLICATOR_ROWS.get(0).metaId(), "the :1556 numeric id");
		assertEquals(20432, GTMachines.REPLICATOR_ROWS.get(1).metaId(), "the :1557 numeric id");
		assertEquals(20433, GTMachines.REPLICATOR_ROWS.get(2).metaId(), "the :1558 numeric id");
		assertEquals(1, GTMachines.MOLECULAR_SCANNER_BLOCKS_BY_PATH.size(), "the scanner BY_PATH walk mirrors the rows");
		assertEquals(3, GTMachines.REPLICATOR_BLOCKS_BY_PATH.size(), "the replicator BY_PATH walk mirrors the rows");
	}

	/** The row columns: Osmiridium housing, hardness 16, the QU carrier, the family textures, the window selectors, the efficiencies. */
	@Test
	public void theRowColumnsCarryTheUpstreamValues() {
		GTBasicMachineBlock.MachineRow tScanner = GTMachines.MOLECULAR_SCANNER_ROWS.get(0);
		assertEquals("t3", tScanner.matSlug(), "the tier word in the literal slot");
		assertEquals(2, tScanner.tier(), "the :1551 NBT_INPUT 512 through the :126 conversion = TIER_INPUTS[2]");
		assertEquals("scannermolecular", tScanner.texture(), "the :1551 NBT_TEXTURE");
		assertSame(TD.Energy.QU, tScanner.energyType(), "the :1551 NBT_ENERGY_ACCEPTED");
		assertEquals(16.0F, tScanner.hardness(), "the :1551 hardness == resistance");
		assertEquals(10000, tScanner.efficiency(), "the :1551 NBT_EFFICIENCY 10000");
		assertEquals(false, tScanner.cheapOverclocking(), "the :1551 carries NO NBT_CHEAP_OVERCLOCKING key");
		assertTrue(GTMachines.TIER_INPUTS[tScanner.tier()][1] == 512, "the tier selector lands mid-window 512");
		for (int i = 0; i < 3; i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.REPLICATOR_ROWS.get(i);
			assertEquals(i, tRow.tier(), "the replicator rungs ride TIER_INPUTS[0..2]");
			assertEquals(GTMachines.TIER_INPUTS[i][1], 32 << (2 * i), "the :1556-1558 NBT_INPUT column 32/128/512");
			assertEquals("replicator", tRow.texture(), "the NBT_TEXTURE family column");
			assertSame(TD.Energy.QU, tRow.energyType(), "the NBT_ENERGY_ACCEPTED QU");
			assertEquals(GTMachines.REPLICATOR_EFFICIENCIES[i], tRow.efficiency(), "the :1556-1558 efficiency column 5000/6250/7500");
			assertEquals(true, tRow.cheapOverclocking(), "the NBT_CHEAP_OVERCLOCKING T column");
		}
	}

	/** The map suppliers: the LIVE subclasses (the p32 runtime chain), not the base map. */
	@Test
	public void theRowsSupplierTheLiveRuntimeMaps() {
		GT6RecipeMaps.init(); // a sibling class's @AfterEach reset nulls the volatiles — re-init before the reads
		assertSame(gregtech6.recipes.GT6RecipeMaps.SCANNER_MOLECULAR, GTMachines.MOLECULAR_SCANNER_ROWS.get(0).recipes().get(),
				"the scanner rides RM.ScannerMolecular");
		assertSame(gregtech6.recipes.GT6RecipeMaps.REPLICATOR, GTMachines.REPLICATOR_ROWS.get(0).recipes().get(),
				"the replicator rides RM.Replicator");
		assertTrue(gregtech6.recipes.GT6RecipeMaps.SCANNER_MOLECULAR instanceof gregtech6.recipes.maps.GT6RecipeMapScannerMolecular,
				"the scanner map is the synthesis subclass (the runtime arm is LIVE)");
		assertTrue(gregtech6.recipes.GT6RecipeMaps.REPLICATOR instanceof gregtech6.recipes.maps.GT6RecipeMapReplicator,
				"the replicator map is the synthesis subclass (the runtime arm is LIVE)");
	}

	/** The containment half (live leg) / the payload+mount half (offline leg) — the logistics two-leg form. */
	@Test
	public void registriesContainAndMountTheFourRungs() {
		boolean tLive = BuiltInRegistries.BLOCK.containsKey(new ResourceLocation("gt6", "replicator"));
		String[] tPaths = {"molecular_scanner_t3", "replicator", "replicator_t2", "replicator_t3"};
		if (tLive) { // the FML-booted leg — the id686 guard
			for (String tPath : tPaths) {
				ResourceLocation tId = new ResourceLocation("gt6", tPath);
				assertTrue(BuiltInRegistries.BLOCK.containsKey(tId), "block face registered: " + tPath);
				assertTrue(BuiltInRegistries.ITEM.containsKey(tId), "item face registered: " + tPath);
				String tBetPath = tPath.startsWith("molecular") ? "molecular_scanner" : "replicator";
				BlockEntityType<?> tType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(new ResourceLocation("gt6", tBetPath));
				assertNotNull(tType, "BET face registered: " + tPath);
				BlockEntity tCreated = tType.create(POS, BuiltInRegistries.BLOCK.get(tId).defaultBlockState());
				assertInstanceOf(TileEntityBasicMachine.class, tCreated, "the BET builds the machine BE: " + tPath);
				assertTrue(((TileEntityBasicMachine)tCreated).getTileEntityName().equals("replicator")
						|| ((TileEntityBasicMachine)tCreated).getTileEntityName().equals("scannermolecular"),
						"the TE name mirrors the recipe-map tail");
			}
		} else { // the offline leg — the row-carrier data face
			for (String tPath : tPaths) {
				assertTrue(GTMachines.BLOCKS.getEntries().stream().anyMatch(tEntry -> tEntry.getId().getPath().equals(tPath)),
						"the DeferredRegister carries the block entry: " + tPath);
			}
			assertEquals(GTMachines.MOLECULAR_SCANNER_BLOCKS_BY_PATH.get("molecular_scanner_t3").getId().getPath(), "molecular_scanner_t3");
			assertEquals(GTMachines.REPLICATOR_BLOCKS_BY_PATH.get("replicator_t2").getId().getPath(), "replicator_t2");
			assertEquals(GTMachines.MOLECULAR_SCANNER_ITEMS_BY_PATH.get("molecular_scanner_t3").getId().getPath(), "molecular_scanner_t3");
			assertEquals(GTMachines.REPLICATOR_ITEMS_BY_PATH.get("replicator_t3").getId().getPath(), "replicator_t3");
		}
	}
}
