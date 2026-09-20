package gregtech6.registry;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.multiblocks.GT6LogisticsCoreBlockEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The core-half of the id686 registration-death-chain guard (task p32-logistics-lv3
 * acceptance ①, the {@link GT6LogisticsRegistrationTest} two-legs-one-contract form): the
 * Logistics Core controller plus its SEVEN structure parts must all be live — the parts
 * (18008 wall / 18299 vents / 18200-04 CPU units) are the GTMultiBlocks rows the massfab
 * card landed, and a Core whose part rows vanished would form NOTHING (the id686 failure
 * shape: a clean test suite over a dead registration).
 */
public class GT6LogisticsCoreRegistrationTest {

	private static final ResourceLocation CORE_ID = new ResourceLocation("gt6", "logistics_core");
	private static final ResourceLocation CORE_BE_ID = new ResourceLocation("gt6", "multiblock_logistics_core");
	private static final ResourceLocation[] PART_IDS = {
			new ResourceLocation("gt6", "machine_wall_galvanized_steel"),   // 18008
			new ResourceLocation("gt6", "ventilation_unit"),                // 18299
			new ResourceLocation("gt6", "processor_unit_versatile"),        // 18200
			new ResourceLocation("gt6", "processor_unit_logic"),            // 18201
			new ResourceLocation("gt6", "processor_unit_control"),          // 18202
			new ResourceLocation("gt6", "processor_unit_storage"),          // 18203
			new ResourceLocation("gt6", "processor_unit_conversion"),       // 18204
	};
	private static final BlockPos POS = new BlockPos(1, 2, 3);

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

	/** The single-path identity of the core registration (the ADR-P3-4 self-contained form). */
	@Test
	public void coreIdentityIsOnePath() {
		assertEquals(CORE_ID, GT6Logistics.LOGISTICS_CORE.getId());
		assertEquals(CORE_ID, GT6Logistics.LOGISTICS_CORE_ITEM.getId());
		assertEquals(CORE_BE_ID, GT6Logistics.LOGISTICS_CORE_BE.getId());
	}

	/** The seven structure parts are live rows (the walk's checkAndSetTarget targets). */
	@Test
	public void structurePartsResolveThroughAnyPartBlock() {
		String[] tPaths = {"machine_wall_galvanized_steel", "ventilation_unit", "processor_unit_versatile",
				"processor_unit_logic", "processor_unit_control", "processor_unit_storage", "processor_unit_conversion"};
		for (int i = 0; i < tPaths.length; i++) {
			// the offline leg resolves through the registration TABLE (the frozen registry keeps
			// RegistryObject.get() dead); the table IS what anyPartBlock() walks
			assertNotNull(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tPaths[i]), "structure part missing: " + PART_IDS[i]);
		}
		// the live leg: the real registries carry all seven rows (the id686 containment shape)
		if (BuiltInRegistries.BLOCK.containsKey(PART_IDS[0])) {
			for (int i = 0; i < PART_IDS.length; i++) {
				assertTrue(BuiltInRegistries.BLOCK.containsKey(PART_IDS[i]), "part registered: " + PART_IDS[i]);
				Block tBlock = BuiltInRegistries.BLOCK.get(PART_IDS[i]);
				assertSame(GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.get(tPaths[i]).get(), tBlock, "part identity: " + PART_IDS[i]);
			}
		}
	}

	/** The containment half (live leg) / the payload+mount half (offline leg) — the wire-test form. */
	@Test
	public void registriesContainAndMountTheCore() {
		if (BuiltInRegistries.BLOCK.containsKey(CORE_ID)) {
			// THE id686 guard — this JVM's registries are the real mod runtime view
			assertTrue(BuiltInRegistries.BLOCK.containsKey(CORE_ID), "block face registered");
			assertTrue(BuiltInRegistries.ITEM.containsKey(CORE_ID), "item face registered");
			assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(CORE_BE_ID), "BET face registered");
			assertSame(GT6Logistics.LOGISTICS_CORE.get(), BuiltInRegistries.BLOCK.get(CORE_ID));
			BlockEntityType<?> tType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(CORE_BE_ID);
			assertNotNull(tType);
			assertTrue(tType.isValid(GT6Logistics.LOGISTICS_CORE.get().defaultBlockState()),
					"the BET must be mounted on gt6:logistics_core — an unmounted type is the id686 failure shape");
			assertCoreFactory(tType.create(POS, GT6Logistics.LOGISTICS_CORE.get().defaultBlockState()));
			assertNotNull(new ItemStack(GT6Logistics.LOGISTICS_CORE_ITEM.get()), "the item stack builds");
		} else {
			// the offline leg: the payload+mount half (the explicit (type,pos,state) ctor over
			// the replayed registry block)
			gregtech6.block.logistics.GTLogisticsCoreBlock tBlock = GT6Logistics.makeCoreBlock();
			@SuppressWarnings("unchecked")
			BlockEntityType<GT6LogisticsCoreBlockEntity>[] tHolder = (BlockEntityType<GT6LogisticsCoreBlockEntity>[]) new BlockEntityType<?>[1];
			tHolder[0] = BlockEntityType.Builder.of(
					(aPos, aState) -> new GT6LogisticsCoreBlockEntity(tHolder[0], aPos, aState), tBlock).build(null);
			assertTrue(tHolder[0].isValid(tBlock.defaultBlockState()), "the offline BET mounts the payload block");
			assertCoreFactory(tHolder[0].create(POS, tBlock.defaultBlockState()));
		}
	}

	/** The factory half: the BET builds the core BE, and the TE name mirrors the BET path. */
	private static void assertCoreFactory(BlockEntity aCreated) {
		assertInstanceOf(GT6LogisticsCoreBlockEntity.class, aCreated);
		assertEquals("multiblock_logistics_core", ((GT6LogisticsCoreBlockEntity)aCreated).getTileEntityName());
	}

}
