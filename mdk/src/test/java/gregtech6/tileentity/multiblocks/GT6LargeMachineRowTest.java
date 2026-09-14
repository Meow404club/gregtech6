package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6LargeMachines.LargeMachineRow;
import gregtech6.registry.GT6LargeMachines.StructureKind;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;

/**
 * The twelve large-machine rows and declared structures pinned (task p29-w3-large-12 —
 * the Loader :1229-1240 columns and the upstream checkStructure2 geometries). The
 * structure census builds each {@link StructureKind} over vanilla stand-in blocks — the
 * pattern is pure data, no registry access — and pins the cell counts, the usage masks
 * and the facing-conditional cells against the upstream loops.
 */
class GT6LargeMachineRowTest extends GTMultiBlocksOfflineTestBase {

	private static LargeMachineRow row(String aPath) {
		for (LargeMachineRow tRow : GT6LargeMachines.ROWS) if (tRow.path().equals(aPath)) return tRow;
		throw new IllegalArgumentException(aPath);
	}

	// -------------------------------------------------------------------------
	// the row table (Loader :1229-1240)
	// -------------------------------------------------------------------------

	@Test
	void twelveRowsDistinctIds() {
		assertEquals(12, GT6LargeMachines.ROWS.size());
		assertEquals(12, GT6LargeMachines.ROWS.stream().map(LargeMachineRow::metaId).distinct().count());
		assertEquals(Set.of(17100, 17102, 17103, 17104, 17105, 17106, 17107, 17108, 17109, 17112, 17113, 17114),
				GT6LargeMachines.ROWS.stream().map(LargeMachineRow::metaId).collect(java.util.stream.Collectors.toSet()));
		assertEquals(12, GT6LargeMachines.ROWS.stream().map(LargeMachineRow::path).distinct().count());
		assertEquals(12, GT6LargeMachines.ROWS.stream().map(LargeMachineRow::texture).distinct().count());
	}

	@Test
	void energyDomainCoverage() {
		// acceptance ⑤ — EU x2 / RU x6 / HU x1 / TU x3, every row exactly one carrier
		assertEquals(2, GT6LargeMachines.ROWS.stream().filter(r -> r.energyType() == gregapi.data.TD.Energy.EU).count());
		assertEquals(6, GT6LargeMachines.ROWS.stream().filter(r -> r.energyType() == gregapi.data.TD.Energy.RU).count());
		assertEquals(1, GT6LargeMachines.ROWS.stream().filter(r -> r.energyType() == gregapi.data.TD.Energy.HU).count());
		assertEquals(3, GT6LargeMachines.ROWS.stream().filter(r -> r.energyType() == gregapi.data.TD.Energy.TU).count());
	}

	@Test
	void noConstantPowerSix() {
		// acceptance ③ — Coagulator/Autoclave/Bath (the TU three) + Crusher/Shredder/Squeezer
		for (String p : new String[] {"large_coagulator", "large_autoclave", "large_bath", "large_crusher", "large_shredder", "large_squeezer"}) {
			assertTrue(row(p).noConstantPower(), p + " carries NBT_NO_CONSTANT_POWER T");
		}
		for (String p : new String[] {"large_centrifuge", "large_electrolyzer", "large_batch_mixer", "large_fermenter", "large_electric_oven", "large_sluice"}) {
			assertFalse(row(p).noConstantPower(), p + " is constant-power");
		}
	}

	@Test
	void efficiencyLadder() {
		// acceptance ④ — 5000 = half speed (the W1 units() ruling), the Oven 2500, the rest identity
		assertEquals(5000, row("large_centrifuge").efficiency());
		assertEquals(5000, row("large_electrolyzer").efficiency());
		assertEquals(2500, row("large_electric_oven").efficiency());
		assertEquals(5000, row("large_sluice").efficiency());
		assertEquals(5000, row("large_crusher").efficiency());
		assertEquals(5000, row("large_shredder").efficiency());
		assertEquals(5000, row("large_squeezer").efficiency());
		for (String p : new String[] {"large_coagulator", "large_autoclave", "large_bath", "large_batch_mixer", "large_fermenter"}) {
			assertEquals(10000, row(p).efficiency(), p);
		}
	}

	@Test
	void parallelLadder() {
		// acceptance ② — 16 / 64 / 256 per the Loader rows
		assertEquals(16, row("large_centrifuge").parallel());
		assertEquals(16, row("large_electrolyzer").parallel());
		assertEquals(16, row("large_autoclave").parallel());
		assertEquals(64, row("large_coagulator").parallel());
		assertEquals(64, row("large_bath").parallel());
		assertEquals(64, row("large_electric_oven").parallel());
		assertEquals(64, row("large_sluice").parallel());
		assertEquals(64, row("large_crusher").parallel());
		assertEquals(64, row("large_shredder").parallel());
		assertEquals(64, row("large_squeezer").parallel());
		assertEquals(256, row("large_batch_mixer").parallel());
		assertEquals(256, row("large_fermenter").parallel());
	}

	@Test
	void cheapOverclockingAndParallelDurationRideTogether() {
		// the CHEAP_OC rows and the PARALLEL_DURATION rows are the same nine (every
		// non-TU machine except — verbatim the Loader lines; the TU three refuse both)
		for (LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			assertEquals(tRow.energyType() != gregapi.data.TD.Energy.TU, tRow.cheapOverclocking(), tRow.path());
			assertEquals(tRow.energyType() != gregapi.data.TD.Energy.TU, tRow.parallelDuration(), tRow.path());
		}
	}

	@Test
	void energySpecWindows() {
		// the two window forms: the 512..4096 band and the 1..16 TU band; the Fermenter
		// overrides the derived MIN down to 1 (Loader :1235 NBT_INPUT_MIN 1)
		LargeMachineRow tCent = row("large_centrifuge");
		assertEquals(512, tCent.nbtInput());
		assertEquals(512, tCent.nbtInputMin());
		assertEquals(4096, tCent.nbtInputMax());
		LargeMachineRow tCoag = row("large_coagulator");
		assertEquals(1, tCoag.nbtInput());
		assertEquals(1, tCoag.nbtInputMin());
		assertEquals(16, tCoag.nbtInputMax());
		LargeMachineRow tFerm = row("large_fermenter");
		assertEquals(512, tFerm.nbtInput());
		assertEquals(1, tFerm.nbtInputMin());
		assertEquals(4096, tFerm.nbtInputMax());
	}

	@Test
	void autoOutBackIsTheFermenterOnly() {
		for (LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			assertEquals(tRow.path().equals("large_fermenter"), tRow.autoOutBack(), tRow.path());
		}
	}

	// -------------------------------------------------------------------------
	// the declared structures (the upstream checkStructure2 geometries)
	// -------------------------------------------------------------------------

	private static int countCells(GTMultiBlockPattern aPattern, java.util.function.Predicate<GTMultiBlockPattern.Cell> aFilter) {
		return (int) aPattern.cells().stream().filter(aFilter).count();
	}

	@Test
	void centrifugeShape() {
		// MultiTileEntityCentrifuge :53-71 — 3x3x2, the ring designs 1..8 duplicated on
		// both layers ONLY_ITEM_FLUID, the centre column ONLY_ENERGY_IN design 0
		for (byte f = 2; f <= 5; f++) {
			GTMultiBlockPattern tPattern = StructureKind.CENTRIFUGE.build(null, f,
					Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS);
			assertEquals(18, tPattern.cells().size(), "facing " + f);
			assertEquals(16, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID));
			assertEquals(2, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN));
			assertEquals(java.util.Set.of(1, 2, 3, 4, 5, 6, 7, 8),
					tPattern.cells().stream().filter(c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID)
							.map(c -> c.design).collect(java.util.stream.Collectors.toSet()));
		}
	}

	@Test
	void autoclaveShape() {
		// MultiTileEntityAutoclave :49-62 — the 3x3x3 hollow one ABOVE the controller:
		// 26 dense walls item+fluid+energy + the air centre
		GTMultiBlockPattern tPattern = StructureKind.HOLLOW_3X3X3.build(null, (byte) 2,
				Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS);
		assertEquals(27, tPattern.cells().size());
		assertEquals(26, countCells(tPattern, GTMultiBlockPattern.Cell::forms));
		assertEquals(1, countCells(tPattern, GTMultiBlockPattern.Cell::isHollow));
		assertTrue(tPattern.cells().stream().allMatch(c -> c.isHollow()
				|| c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY));
	}

	@Test
	void boxShape() {
		// MultiTileEntityCoagulator/Bath — the solid 5x5x2, every cell ONLY_ITEM_FLUID
		GTMultiBlockPattern tPattern = StructureKind.BOX_5X5X2.build(null, (byte) 2,
				Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS);
		assertEquals(50, tPattern.cells().size());
		assertTrue(tPattern.cells().stream().allMatch(c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID));
	}

	@Test
	void mixerShape() {
		// MultiTileEntityMixer :49-68 — the bottom layer OUT, the top layer IN, the
		// centre column design 3 ONLY_ENERGY_IN
		GTMultiBlockPattern tPattern = StructureKind.MIXER.build(null, (byte) 2,
				Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS);
		assertEquals(18, tPattern.cells().size());
		assertEquals(8, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT));
		assertEquals(8, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN));
		assertEquals(2, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN));
	}

	@Test
	void fermenterVentFollowsTheFacing() {
		// MultiTileEntityFermenter :53-128 — the 25 transmitters BELOW, the 50 walls with
		// the BACK mid-edge cell design 7 per facing; controller-relative declared cell =
		// (0, j, -2) for north-facing... the back = OPPOSITE the facing
		for (byte f = 2; f <= 5; f++) {
			GTMultiBlockPattern tPattern = StructureKind.FERMENTER.build(null, f,
					Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.STONE);
			assertEquals(75, tPattern.cells().size());
			assertEquals(25, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN));
			List<GTMultiBlockPattern.Cell> tVents = tPattern.cells().stream()
					.filter(c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID && c.design == 7).toList();
			assertEquals(2, tVents.size(), "facing " + f); // one per wall layer
			// the vent sits on the wall layer, its side OPPOSITE the facing (the back),
			// judged in CENTRE-RELATIVE coordinates (the declared cell carries the
			// controller anchor — declared = centre-relative + anchorOffset(facing))
			int[] anch = GTMultiBlockPattern.anchorOffset(f);
			int tBackX = f == 4 ? 2 : f == 5 ? -2 : 0;
			int tBackZ = f == 2 ? 2 : f == 3 ? -2 : 0;
			for (GTMultiBlockPattern.Cell tVent : tVents) {
				assertEquals(tBackX, tVent.x - anch[0], "facing " + f);
				assertEquals(tBackZ, tVent.z - anch[2], "facing " + f);
			}
		}
	}

	@Test
	void ovenShape() {
		// MultiTileEntityOven :49-91 — the 3x3x3: walls / the 8-coil ring around an air
		// centre / walls; the wall layers item+fluid+energy, the coils NOTHING
		GTMultiBlockPattern tPattern = StructureKind.OVEN.build(null, (byte) 2,
				Blocks.BRICKS, Blocks.STONE, Blocks.STONE, Blocks.BRICKS);
		assertEquals(27, tPattern.cells().size());
		assertEquals(18, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY));
		assertEquals(8, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.NOTHING && c.forms()));
		assertEquals(1, countCells(tPattern, GTMultiBlockPattern.Cell::isHollow));
	}

	@Test
	void sluiceShape() {
		// MultiTileEntitySluice :50-83 — the 7x3x3 trough: 21 + 21 + 21 cells; the centre
		// line design 1 OUT, the far row (distance 5) design 3 ENERGY_IN, the far end
		// (distance 6) sluice parts IN with the idle tD = facing-2
		for (byte f = 2; f <= 5; f++) {
			GTMultiBlockPattern tPattern = StructureKind.SLUICE.build(null, f,
					Blocks.BRICKS, Blocks.STONE, Blocks.STONE, Blocks.BRICKS);
			assertEquals(63, tPattern.cells().size());
			assertEquals(7, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT));
			assertEquals(3, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN));
			assertEquals(3, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN));
			List<GTMultiBlockPattern.Cell> tIn = tPattern.cells().stream()
					.filter(c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN).toList();
			assertEquals(f - 2, tIn.get(0).design, "the idle tD design, facing " + f);
		}
	}

	@Test
	void basinShapes() {
		// MultiTileEntityCrusher/Shredder :53-140 — the 75-cell wheel basin: the energy
		// walls ride the mid-edge pair PERPENDICULAR to the facing axis
		for (byte f = 2; f <= 5; f++) {
			GTMultiBlockPattern tPattern = StructureKind.BASIN_WHEELS.build(null, f,
					Blocks.BRICKS, Blocks.STONE, Blocks.STONE, Blocks.BRICKS);
			assertEquals(75, tPattern.cells().size());
			assertEquals(25, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT));
			assertEquals(2, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ENERGY_IN));
			assertEquals(9, countCells(tPattern, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN));
			boolean tAxisX = f == 4 || f == 5;
			int[] anch = GTMultiBlockPattern.anchorOffset(f);
			for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
				if (tCell.usage != MultiBlockPartBlockEntity.ONLY_ENERGY_IN) continue;
				int tCi = tCell.x - anch[0], tK = tCell.z - anch[2]; // centre-relative
				if (tAxisX) {
					assertEquals(0, tCi, "the z-mid-edges take the energy when the facing is along X");
				} else {
					assertEquals(0, tK, "the x-mid-edges take the energy when the facing is along Z");
				}
			}
			// the squeezer open basin: 25 + 12 + 25
			GTMultiBlockPattern tOpen = StructureKind.BASIN_OPEN.build(null, f,
					Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS, Blocks.BRICKS);
			assertEquals(62, tOpen.cells().size());
			assertEquals(25, countCells(tOpen, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_OUT));
			assertEquals(12, countCells(tOpen, c -> c.usage == MultiBlockPartBlockEntity.NOTHING));
			assertEquals(25, countCells(tOpen, c -> c.usage == MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_IN));
		}
	}

}
