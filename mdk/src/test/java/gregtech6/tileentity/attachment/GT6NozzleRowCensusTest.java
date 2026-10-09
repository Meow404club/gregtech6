package gregtech6.tileentity.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.attachment.GTAttachmentSmallBlock;
import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The nozzle-pair row census (task material-mc-f-attachment-rows) — the 12 rows
 * Loader_MultiTileEntities.java:2122-2134 verbatim (six nozzles :2122-2127 + six cap
 * nozzles :2129-2134): display word, acid-proof flag, resistance ladder
 * 3.0/5.0/6.0/10.0/10.0/100.0, the aUtil tool column → sound (WOOD on plastic,
 * METAL elsewhere — the nozzle set has NO ceramic row so no STONE sound), hardness
 * 0.5F on every row. Plus the family faces the rows mount through: the horizontal-only
 * validity (the 10Attachment:51 default — neither nozzle class carries a
 * getValidSides override), the shared BET pairing (NOZZLE_BE over the nozzle blocks,
 * CAP_NOZZLE_BE over the cap blocks — one {@link GTNozzleBlockEntity} class, the
 * ADR-P3-1 multi-mount) and the activation heads (the gas chains landed by task
 * nozzle-function — the offline behavior truth table is GT6NozzleFunctionTest).
 */
public class GT6NozzleRowCensusTest extends GTOfflineTestBase {

	static BlockEntityType<GTNozzleBlockEntity> sNozzleType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		// the row materials resolve through MT.init (the GT6AttachmentStackDatagenTest
		// bracket — the bare JVM leaves the MT statics null)
		gregtech6.registry.GT6MaterialTestSupport.materials();
		BlockEntityType<GTNozzleBlockEntity>[] tTypes = (BlockEntityType<GTNozzleBlockEntity>[]) new BlockEntityType<?>[1];
		tTypes[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTNozzleBlockEntity(tTypes[0], aPos, aState), Blocks.STONE).build(null);
		sNozzleType = tTypes[0];
	}

	private static GT6Attachments.AttachmentRow rowByPath(String aPath) {
		return GT6Attachments.ROWS.stream().filter(aRow -> aRow.path().equals(aPath)).findFirst().orElseThrow();
	}

	/** One row, verbatim (the path, the display word, the acid flag, the resistance, the sound). */
	private static void assertRow(String aPath, String aDisplay, boolean aAcid, float aResistance, SoundType aSound) {
		GT6Attachments.AttachmentRow tRow = rowByPath(aPath);
		assertEquals(aDisplay, tRow.matDisplay(), aPath + ": the display word");
		assertEquals(aAcid, tRow.acidProof(), aPath + ": the NBT_ACIDPROOF column");
		assertEquals(0.5F, tRow.hardnessF(), aPath + ": the NBT_HARDNESS column (0.5F on every attachment row)");
		assertEquals(aResistance, tRow.resistanceF(), aPath + ": the NBT_RESISTANCE column");
		assertEquals(aSound, tRow.sound(), aPath + ": the aUtil tool column → sound");
	}

	@Test
	public void theTwelveNozzleRowsAreTheLoaderLines() {
		assertEquals(24, GT6Attachments.ROWS.size(), "the attachment census: 12 tap/funnel + 12 nozzle pair");
		assertEquals(6, GT6Attachments.ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.NOZZLE).count());
		assertEquals(6, GT6Attachments.ROWS.stream().filter(r -> r.family() == GTAttachmentSmallBlock.Family.CAP_NOZZLE).count());
		// :2122-2127 nozzles — upstream order Plastic(32747)/Steel(32746)/Stainless(32748)/
		// W(32749)/Ta4HfC5(32079)/Ad(32750)
		assertRow("nozzle_plastic"                  , "Plastic"                  , false,  3.0F, SoundType.WOOD );
		assertRow("nozzle_steel"                    , "Steel"                    , false,  5.0F, SoundType.METAL);
		assertRow("nozzle_stainless_steel"          , "Stainless"                , true ,  6.0F, SoundType.METAL);
		assertRow("nozzle_tungsten"                 , "Tungsten"                 , true , 10.0F, SoundType.METAL);
		assertRow("nozzle_tantalum_hafnium_carbide" , "Tantalum Hafnium Carbide" , false, 10.0F, SoundType.METAL);
		assertRow("nozzle_adamantium"               , "Adamantium"               , true , 100.0F, SoundType.METAL);
		// :2129-2134 cap nozzles — the same column set (32059/32058/32060/32061/32082/32062)
		assertRow("cap_nozzle_plastic"                  , "Plastic"                  , false,  3.0F, SoundType.WOOD );
		assertRow("cap_nozzle_steel"                    , "Steel"                    , false,  5.0F, SoundType.METAL);
		assertRow("cap_nozzle_stainless_steel"          , "Stainless"                , true ,  6.0F, SoundType.METAL);
		assertRow("cap_nozzle_tungsten"                 , "Tungsten"                 , true , 10.0F, SoundType.METAL);
		assertRow("cap_nozzle_tantalum_hafnium_carbide" , "Tantalum Hafnium Carbide" , false, 10.0F, SoundType.METAL);
		assertRow("cap_nozzle_adamantium"               , "Adamantium"               , true , 100.0F, SoundType.METAL);
	}

	/** The nozzle material set differs from the tap/funnel set: no Ceramic, but a Steel row. */
	@Test
	public void theNozzleMaterialSetIsPlasticSteelStainlessWTaAd() {
		Set<String> tExpected = Set.of("plastic", "steel", "stainless_steel", "tungsten", "tantalum_hafnium_carbide", "adamantium");
		for (var tFamily : List.of(GTAttachmentSmallBlock.Family.NOZZLE, GTAttachmentSmallBlock.Family.CAP_NOZZLE)) {
			Set<String> tSlugs = new java.util.HashSet<>();
			for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
				if (tRow.family() == tFamily) tSlugs.add(
						GT6Attachments.matUnitKeyOf(tRow).substring("gt6.row.attachment.mat.".length()));
			}
			assertEquals(tExpected, tSlugs, tFamily + ": the six-row material set");
		}
		// the Steel rows ride ANY.Steel folded to MT.Steel (the boiler-fold convention);
		// every nozzle slug resolves a tint material (no untinted row in this pair)
		assertNotNull(GT6Attachments.materialOf(rowByPath("nozzle_steel")));
		assertEquals(gregapi.data.MT.Steel, GT6Attachments.materialOf(rowByPath("cap_nozzle_steel")));
		assertNotNull(GT6Attachments.materialOf(rowByPath("nozzle_adamantium")));
	}

	/**
	 * The mount validity: four horizontals (the 10Attachment:51 default — neither
	 * upstream nozzle class overrides getValidSides), never the verticals.
	 */
	@Test
	public void theNozzleFamiliesMountOnTheFourHorizontalsOnly() {
		for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
			if (tRow.family() != GTAttachmentSmallBlock.Family.NOZZLE
					&& tRow.family() != GTAttachmentSmallBlock.Family.CAP_NOZZLE) continue;
			for (Direction tDir : Direction.values()) {
				boolean tExpected = tDir.getAxis() != Direction.Axis.Y;
				assertEquals(tExpected, tRow.family().isValidMount(tDir),
						tRow.path() + ": " + tDir + " mount");
			}
		}
		// the contrast arms stay as landed: the funnel takes DOWN, the tap does not
		assertTrue(GTAttachmentSmallBlock.Family.FUNNEL.isValidMount(Direction.DOWN));
		assertFalse(GTAttachmentSmallBlock.Family.TAP.isValidMount(Direction.DOWN));
	}

	/**
	 * The BET pairing: the row's tickerType resolves the family BET
	 * (NOZZLE_BE/CAP_NOZZLE_BE), both BET classes are the one GTNozzleBlockEntity, and
	 * the offline-constructed BE answers the nozzle name + the live chain heads (the
	 * nozzle-function port: the drain chain is the default, the cap chain the family
	 * dispatch — behavior pins live in GT6NozzleFunctionTest).
	 */
	@Test
	public void theNozzleRowsRideTheSharedNozzleBetsAndTheDeclaredCut() {
		assertEquals("gt6:nozzle", GTBlockEntities.NOZZLE_BE.getId().toString());
		assertEquals("gt6:cap_nozzle", GTBlockEntities.CAP_NOZZLE_BE.getId().toString());
		// both BET types are declared over the ONE GTNozzleBlockEntity class (the
		// ADR-P3-1 shared-class multi-mount) — the offline construction proves the
		// supplier + the carrier face
		BlockState tState = Blocks.STONE.defaultBlockState();
		GTNozzleBlockEntity tNozzle = new GTNozzleBlockEntity(sNozzleType, POS, tState);
		assertEquals("nozzle", tNozzle.getTileEntityName());
		// the nozzle-function chains are live: the mount-less drain head answers its
		// adjacency verdict, the cap dispatch is the family seam (behavior truth table
		// in GT6NozzleFunctionTest)
		String tReport = tNozzle.activate(null, (byte)Direction.NORTH.get3DDataValue(), null);
		assertTrue(tReport.contains("no tap-accessible container"), "the drain chain head answers: " + tReport);
		assertFalse(tNozzle.isCapNozzle(), "the stone-carrier fixture defaults to the drain chain");
	}
}
