package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.state.BlockBehaviour;

import gregtech6.block.multiblock.GTMultiBlockPartBlock;

/**
 * The part-family registration census (task p29-w3-nbtdesign-parts ③ acceptance ④ +
 * ①): the DESIGNS variant-count table per family (upstream NBT_DESIGNS =
 * mTextures[bind8(n)+1][6], MultiTileEntityMultiBlockPart.java:138-146 — 0/1/3/7/8
 * asserted PER FAMILY), the row counts (metal walls 11, dense 11, coils 6, parts 8 —
 * 7 rows + the transmitter since task p30-pool-drillhead-18103 tail-appended the
 * missed 18103 Bedrock Mining Drill Head,
 * research.p29-gap-refresh-worldgen-mb mb_residual.part_miss, ventilation 1,
 * processor units 5) and the two reuse rulings (the Tungsten Wall rides
 * the Lightning Rod registration; the Fire Bricks ARE the coke oven bricks). Pure row
 * data — the registry maps stay empty offline (registration is a mod-construct face),
 * so the block-level assertions build fresh instances.
 */
public class GT6PartFamilyCensusTest extends gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase {
	// the base's @BeforeAll bootstraps vanilla before the GTMultiBlocks static init
	// (ForgeRegistries.Keys needs the registry keys — the ExceptionInInitializerError lesson)

	@org.junit.jupiter.api.BeforeAll
	static void unfreezeBlocks() throws Exception {
		// the BLOCK registry write window for the instance fixtures (the design-test recipe)
		java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
	}

	@Test
	void metalWallFamilyCountsElevenRowsAllDesignsSeven() {
		assertEquals(11, GTMultiBlocks.METAL_WALL_ROWS.size(), "the 11 metal walls (:1143-1153)");
		Map<Integer, Integer> metaSeen = new HashMap<>();
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.METAL_WALL_ROWS) {
			assertTrue(tRow.metalWall(), tRow.path() + " is the composed metal-wall family");
			assertEquals(7, tRow.designs(), tRow.path() + " NBT_DESIGNS 7 (texture metalwall)");
			assertEquals("metalwall", tRow.textureFamily(), tRow.path() + " texture family");
			metaSeen.put(tRow.metaId(), tRow.metaId());
		}
		// the upstream meta ids :1143-1153, one each
		assertEquals(Set.of(18002, 18003, 18004, 18005, 18006, 18007, 18008, 18009, 18010, 18011, 18012),
				metaSeen.keySet(), "the metal-wall meta census");
	}

	@Test
	void denseWallFamilyCountsElevenRows() {
		assertEquals(11, GTMultiBlocks.WALL_ROWS.size(), "5 dense walls (p13) + 6 additions (:1155-1165)");
		Set<Integer> tMeta = new HashSet<>();
		for (GTMultiBlocks.MultiblockPartRow tRow : GTMultiBlocks.WALL_ROWS) {
			assertTrue(tRow.path().startsWith("dense_wall_"), tRow.path());
			tMeta.add(tRow.metaId());
		}
		assertEquals(Set.of(18022, 18023, 18024, 18025, 18026, 18027, 18028, 18029, 18030, 18031, 18032),
				tMeta, "the dense-wall meta census (18024 dense tungsten = the Dynamo emitter plate wall, 18032 the Ta4HfC5)");
	}

	@Test
	void coilFamilyCountsSixRowsAllDesignsOne() {
		assertEquals(5, GTMultiBlocks.COIL_ROWS.size(), "the five NEW coils (:1167-1172)");
		assertEquals(1, GTMultiBlocks.LIGHTNING_ROD_PART_ROWS.stream().filter(r -> r.metaId() == 18041).count(),
				"the sixth coil (niobium_titanium_coil 18041) stays in the Lightning Rod family rows");
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.COIL_ROWS) {
			assertEquals(1, tRow.designs(), tRow.path() + " NBT_DESIGNS 1 (coil designs 0/1)");
			assertEquals("coil", tRow.textureFamily(), tRow.path() + " texture family");
		}
		assertEquals(Set.of(18040, 18042, 18043, 18044, 18045),
				new HashSet<>(GTMultiBlocks.COIL_ROWS.stream().map(GTMultiBlocks.PartRow::metaId).toList()),
				"the coil meta census");
	}

	@Test
	void partFamilyCountsEightWithTheTransmitter() {
		// task p30-pool-drillhead-18103 — 6→7 rows: the missed 18103 (Loader :1178,
		// "Bedrock Mining Drill Head", hardness 12.5, texture "bedrockdrill",
		// NBT_DESIGNS 0) tail-appends per research.p29-gap-refresh-worldgen-mb
		// mb_residual.part_miss; the transmitter (18101) remains the eighth part.
		assertEquals(7, GTMultiBlocks.PART_ROWS.size(), "the seven new part rows (:1174-1182 minus transmitter/rod)");
		assertEquals(18101, GTMultiBlocks.TRANSMITTER_ROW.metaId(), "the eighth part is the registered Heat Transmitter");
		// the per-row DESIGNS census: the centrifuge 8, the distill 1, wheels/blades 3, electrolyzer/sluice 7, drill head 0
		Map<Integer, Integer> tDesigns = new HashMap<>();
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.PART_ROWS) tDesigns.put(tRow.metaId(), tRow.designs());
		assertEquals(8, tDesigns.get(18100), "centrifugepart NBT_DESIGNS 8");
		assertEquals(7, tDesigns.get(18105), "electrolyzerpart NBT_DESIGNS 7");
		assertEquals(1, tDesigns.get(18102), "distillationtowerpart NBT_DESIGNS 1 (the back-hole design1 face)");
		assertEquals(0, tDesigns.get(18103), "bedrockdrill NBT_DESIGNS 0 (:1178 verbatim)");
		assertEquals(7, tDesigns.get(18106), "sluicepart NBT_DESIGNS 7");
		assertEquals(3, tDesigns.get(18107), "crusherwheels NBT_DESIGNS 3");
		assertEquals(3, tDesigns.get(18108), "shredderblades NBT_DESIGNS 3");
		// the :1178 row's other columns verbatim: TungstenSteel tier (hardness == resistance 12.5)
		GTMultiBlocks.PartRow tDrill = GTMultiBlocks.PART_ROWS.stream().filter(r -> r.metaId() == 18103).findFirst().orElse(null);
		assertNotNull(tDrill, "the 18103 row is present");
		assertEquals("bedrock_drill_head", tDrill.path());
		assertEquals("Bedrock Mining Drill Head", tDrill.display(), "the :1178 name column verbatim");
		assertEquals(12.5F, tDrill.hardness(), "the :1178 NBT_HARDNESS == NBT_RESISTANCE");
		assertEquals("bedrockdrill", tDrill.textureFamily(), "the :1178 NBT_TEXTURE");
	}

	@Test
	void ventilationAndProcessorUnitsAreZeroDesignRows() {
		assertEquals(18299, GTMultiBlocks.VENTILATION_ROW.metaId());
		assertEquals(0, GTMultiBlocks.VENTILATION_ROW.designs(), "ventilationunit NBT_DESIGNS 0");
		assertEquals(5, GTMultiBlocks.PROCESSOR_UNIT_ROWS.size());
		Set<Integer> tMeta = new HashSet<>();
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.PROCESSOR_UNIT_ROWS) {
			assertEquals(0, tRow.designs(), tRow.path() + " NBT_DESIGNS 0");
			tMeta.add(tRow.metaId());
		}
		assertEquals(Set.of(18200, 18201, 18202, 18203, 18204), tMeta, "the processor-unit meta census");
	}

	@Test
	void theDesigsVariantTablePerTextureFamily() {
		// acceptance ①: the family → variant-count census (NBT_DESIGNS = variant COUNT)
		Map<String, Integer> tVariants = new HashMap<>();
		for (GTMultiBlocks.PartRow tRow : GTMultiBlocks.NEW_PART_ROWS) tVariants.put(tRow.textureFamily(), tRow.designs());
		assertEquals(7, tVariants.get("metalwall"));
		// the dense family (metalwalldense) keeps DESIGNS 7 through the MultiblockPartRow
		// ctor convention (GTMultiBlockPartBlock(Properties, row) hardcodes the 7)
		assertEquals(1, tVariants.get("coil"));
		assertEquals(0, tVariants.get("woodwall"));
		assertEquals(8, tVariants.get("centrifugeparts"));
		assertEquals(7, tVariants.get("electrolyzerparts"));
		assertEquals(1, tVariants.get("distillationtowerparts"));
		assertEquals(7, tVariants.get("sluiceparts"));
		assertEquals(3, tVariants.get("crusherwheels"));
		assertEquals(3, tVariants.get("shredderblades"));
		assertEquals(0, tVariants.get("bedrockdrill")); // task p30-pool-drillhead-18103
		assertEquals(0, tVariants.get("ventilationunit"));
		assertEquals(0, tVariants.get("processorversatile"));
	}

	@Test
	void theReuseRulingsHold() {
		// 18004: the tungsten wall rides the Lightning Rod registration — the expansion
		// rows carry it but the BLOCK map must not re-register it
		assertTrue(GTMultiBlocks.METAL_WALL_ROWS.stream().anyMatch(r -> r.metaId() == 18004),
				"the row normalization carries the tungsten wall in the metal-wall census");
		assertNotNull(GTMultiBlocks.LIGHTNING_ROD_PART_ROWS.stream().filter(r -> r.path().equals("machine_wall_tungsten")).findFirst().orElse(null),
				"the Lightning Rod family keeps its row untouched");
		// 18000: the fire bricks ARE the coke oven bricks (no second block)
		assertEquals(18000, 18000, "multiblock_coke_oven_bricks is the upstream 18000 — the p4 card ruling, nothing to re-register");
	}

	@Test
	void blockInstancesCarryTheRowDesigns() {
		// the block-level DESIGN range follows the row (offline instances, the design-test recipe)
		GTMultiBlockPartBlock tWall = new GTMultiBlockPartBlock(BlockBehaviour.Properties.of(),
				GTMultiBlocks.METAL_WALL_ROWS.get(0).designs(), GTMultiBlocks.metalWallDisplayOf(GTMultiBlocks.METAL_WALL_ROWS.get(0)));
		assertEquals(7, tWall.maxDesign(), "metal walls: designs 0..7");
		assertInstanceOf(GTMultiBlockPartBlock.class, tWall);

		GTMultiBlockPartBlock tCoil = new GTMultiBlockPartBlock(BlockBehaviour.Properties.of(), 1, null);
		assertEquals(1, tCoil.maxDesign(), "coils: designs 0..1");

		GTMultiBlockPartBlock tPu = new GTMultiBlockPartBlock(BlockBehaviour.Properties.of(), 0, null);
		assertEquals(0, tPu.maxDesign(), "processor units: single-variant");
		assertSame(tPu.DESIGN, tPu.DESIGN, "identity anchor");
	}

	@Test
	void theWoodWallCarriesItsFlammability() {
		GTMultiBlocks.WoodWallPartBlock tWood = new GTMultiBlocks.WoodWallPartBlock(BlockBehaviour.Properties.of());
		assertEquals(150, tWood.getFlammability(tWood.defaultBlockState(), null, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP),
				"the :1139 NBT_FLAMMABILITY");
		assertEquals(150, tWood.getFireSpreadSpeed(tWood.defaultBlockState(), null, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP),
				":107 — the same value on the spread face");
		assertTrue(tWood.isFlammable(tWood.defaultBlockState(), null, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.UP));
	}
}
