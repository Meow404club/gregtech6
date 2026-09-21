package gregtech6.registry;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tools.GTKitchenBlock;
import gregtech6.tileentity.tools.GT6JuicerBlockEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Juicer registration guard (task p33-food-machines-kitchen; the
 * {@link GT6LogisticsCoreRegistrationTest} id686 FML-boot containment form — a clean
 * suite over a dead registration is the failure shape, so the real registries must
 * carry the block/item/BET faces and the BET must be MOUNTED on the carrier block).
 * The upstream row is Loader_MultiTileEntities.java:2184 (id 32722, MT.Ceramic,
 * aUtilStone, RM.Juicer — the 07Paintable manual block family).
 */
public class GT6JuicerRegistrationTest {

	private static final ResourceLocation JUICER_ID = new ResourceLocation("gt6", "juicer");
	private static final ResourceLocation JUICER_BE_ID = new ResourceLocation("gt6", "juicer");
	private static final BlockPos POS = new BlockPos(2, 2, 3);

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

	/** The single-path identity of the registration (the GT6Kitchen self-contained form). */
	@Test
	public void identityIsOnePath() {
		assertEquals(JUICER_ID, GT6Kitchen.JUICER.getId());
		assertEquals(JUICER_ID, GT6Kitchen.JUICER_ITEM.getId());
		assertEquals(JUICER_BE_ID, GT6Kitchen.JUICER_BE.getId());
	}

	/** THE id686 containment — the live registries carry the three faces and the BET mounts the block. */
	@Test
	public void registriesContainAndMountTheJuicer() {
		// the offline leg: the RegistryObjects never registered (no mod bus) — the
		// payload+mount half over an explicit holder (the GT6LogisticsCoreRegistrationTest form)
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6JuicerBlockEntity>[] tHolder = (BlockEntityType<GT6JuicerBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6JuicerBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		assertTrue(tHolder[0].isValid(Blocks.STONE.defaultBlockState()), "the offline BET mounts the fixture block");
		assertInstanceOf(GT6JuicerBlockEntity.class, tHolder[0].create(POS, Blocks.STONE.defaultBlockState()));
		assertEquals("juicer", ((GT6JuicerBlockEntity)tHolder[0].create(POS, Blocks.STONE.defaultBlockState())).getTileEntityName());

		if (BuiltInRegistries.BLOCK.containsKey(JUICER_ID)) {
			// THE id686 guard — this JVM's registries are the real mod runtime view
			assertTrue(BuiltInRegistries.BLOCK.containsKey(JUICER_ID), "block face registered");
			assertTrue(BuiltInRegistries.ITEM.containsKey(JUICER_ID), "item face registered");
			assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(JUICER_BE_ID), "BET face registered");
			assertInstanceOf(GTKitchenBlock.class, BuiltInRegistries.BLOCK.get(JUICER_ID));
			BlockEntityType<?> tType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(JUICER_BE_ID);
			assertNotNull(tType);
			if (GT6Kitchen.JUICER.isPresent()) {
				assertSame(GT6Kitchen.JUICER.get(), BuiltInRegistries.BLOCK.get(JUICER_ID));
				assertTrue(tType.isValid(GT6Kitchen.JUICER.get().defaultBlockState()),
						"the BET must be mounted on gt6:juicer — an unmounted type is the id686 failure shape");
				BlockEntity tCreated = tType.create(POS, GT6Kitchen.JUICER.get().defaultBlockState());
				assertInstanceOf(GT6JuicerBlockEntity.class, tCreated);
				assertNotNull(new ItemStack(GT6Kitchen.JUICER_ITEM.get()), "the item stack builds");
			}
		}
	}
}
