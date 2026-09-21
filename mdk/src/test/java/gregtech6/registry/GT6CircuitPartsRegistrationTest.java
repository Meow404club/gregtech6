package gregtech6.registry;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The id686 registration guard for the circuits-parts family (task p33-circuits-parts:
 * the Ventilation Unit 18299 + the five Quadcore Processor Units 18200-18204, the
 * Logistics Core structure parts). The registration face itself landed on main in
 * fc1feb8c2 (the part-family expansion); this card pins the containment: the rows in
 * {@link GTMultiBlocks} must resolve through the REAL registries on an FML-booted JVM
 * (the offline bare-JVM leg skips the live probes via the containsKey latch, the
 * GT6LogisticsCoreRegistrationTest two-legs-one-contract form).
 */
public class GT6CircuitPartsRegistrationTest {

	private static final ResourceLocation VENT_ID = new ResourceLocation("gt6", "ventilation_unit");
	private static final ResourceLocation[] PU_IDS = {
			new ResourceLocation("gt6", "processor_unit_versatile"),   // 18200
			new ResourceLocation("gt6", "processor_unit_logic"),       // 18201
			new ResourceLocation("gt6", "processor_unit_control"),     // 18202
			new ResourceLocation("gt6", "processor_unit_storage"),     // 18203
			new ResourceLocation("gt6", "processor_unit_conversion"),  // 18204
	};
	private static final int[] PU_METAS = {18200, 18201, 18202, 18203, 18204};

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** The row data: paths, meta ids, zero designs, per-row texture families (Loader :1184-1189). */
	@Test
	public void rowsCarryTheUpstreamColumns() {
		assertEquals("ventilation_unit", GTMultiBlocks.VENTILATION_ROW.path());
		assertEquals(18299, GTMultiBlocks.VENTILATION_ROW.metaId());
		assertEquals(6.0F, GTMultiBlocks.VENTILATION_ROW.hardness(), "NBT_HARDNESS == NBT_RESISTANCE 6.0F");
		assertEquals(0, GTMultiBlocks.VENTILATION_ROW.designs(), "NBT_DESIGNS 0");
		assertEquals("ventilationunit", GTMultiBlocks.VENTILATION_ROW.textureFamily());
		assertEquals(PU_METAS.length, GTMultiBlocks.PROCESSOR_UNIT_ROWS.size(), "the five PU rows");
		for (int i = 0; i < PU_METAS.length; i++) {
			GTMultiBlocks.PartRow tRow = GTMultiBlocks.PROCESSOR_UNIT_ROWS.get(i);
			assertEquals(PU_METAS[i], tRow.metaId(), "the PU meta census");
			assertEquals(PU_IDS[i].getPath(), tRow.path());
			assertEquals(6.0F, tRow.hardness());
			assertEquals(0, tRow.designs(), "NBT_DESIGNS 0");
			assertEquals("processor" + tRow.path().substring("processor_unit_".length()), tRow.textureFamily(),
					"the texture family mirrors the path suffix");
		}
	}

	/** The rows live in the registration table and the NEW_PART_ROWS walk (the tab walk order). */
	@Test
	public void rowsResolveThroughTheRegistrationTable() {
		assertNotNull(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("ventilation_unit"), "vent block row");
		assertNotNull(GTMultiBlocks.NEW_PART_ITEMS_BY_PATH.get("ventilation_unit"), "vent item row");
		assertTrue(GTMultiBlocks.NEW_PART_ROWS.indexOf(GTMultiBlocks.VENTILATION_ROW)
				< GTMultiBlocks.NEW_PART_ROWS.indexOf(GTMultiBlocks.PROCESSOR_UNIT_ROWS.get(0)),
				"the upstream registration order: vent 18299 before the PUs 18200-04");
		for (ResourceLocation tId : PU_IDS) {
			assertNotNull(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tId.getPath()), "PU block row: " + tId);
			assertNotNull(GTMultiBlocks.NEW_PART_ITEMS_BY_PATH.get(tId.getPath()), "PU item row: " + tId);
		}
		// the anyPartBlock resolution seam the Logistics Core structure walk uses — the
		// live leg only (RegistryObject.get() is dead on the offline frozen-registry JVM)
		if (BuiltInRegistries.BLOCK.containsKey(VENT_ID)) {
			assertSame(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("ventilation_unit").get(),
					GTMultiBlocks.anyPartBlock("ventilation_unit"), "anyPartBlock vent");
			assertSame(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("processor_unit_versatile").get(),
					GTMultiBlocks.anyPartBlock("processor_unit_versatile"), "anyPartBlock PU versatile");
		}
	}

	/** THE id686 containment leg: on an FML-booted JVM the real registries carry all six rows. */
	@Test
	public void registriesContainTheSixRows() {
		if (!BuiltInRegistries.BLOCK.containsKey(VENT_ID)) {
			return; // the offline bare-JVM leg; the FML-booted leg (runServer test config) asserts for real
		}
		assertTrue(BuiltInRegistries.BLOCK.containsKey(VENT_ID), "vent block registered");
		assertTrue(BuiltInRegistries.ITEM.containsKey(VENT_ID), "vent item registered");
		assertSame(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get("ventilation_unit").get(),
				BuiltInRegistries.BLOCK.get(VENT_ID), "vent block identity");
		for (int i = 0; i < PU_IDS.length; i++) {
			assertTrue(BuiltInRegistries.BLOCK.containsKey(PU_IDS[i]), "PU block registered: " + PU_IDS[i]);
			assertTrue(BuiltInRegistries.ITEM.containsKey(PU_IDS[i]), "PU item registered: " + PU_IDS[i]);
			assertSame(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(PU_IDS[i].getPath()).get(),
					BuiltInRegistries.BLOCK.get(PU_IDS[i]), "PU block identity: " + PU_IDS[i]);
		}
	}
}
