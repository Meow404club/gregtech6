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

import gregtech6.block.logistics.GTLogisticsWireBlock;
import gregtech6.block.logistics.GTLogisticsWireBlockItem;
import gregtech6.tileentity.connectors.GTLogisticsWireBlockEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The id686 registration-death-chain guard (task p32-logistics-lv2 acceptance ①): a
 * registration-face card must carry a JVM assertion on its FML face — the P31 lesson (a
 * neo-leg static-block snapshot registered ZERO entries while cleanTest stayed green;
 * the real break was a codec NPE at world load).
 *
 * <p>Two legs, one contract: on the 1.21.1 leg the test JVM boots through FML itself,
 * so the CONTAINMENT half runs against the real registries ({@code gt6:logistics_wire}
 * present in block/item/BET registries, the BET mounted on the block, the BET factory
 * building a named wire BE) — that is exactly the class of failure id686 guards. On the
 * 1.20.1 leg the offline bootstrapped JVM cannot touch the Forge-locked registries, so
 * it pins the payload+mount half instead: the registration suppliers build, the offline
 * BET over the built block is a working factory, the identity is one path. A silent
 * zero-entry registration (the wrong bus, a dead snapshot, a mistyped path) fails the
 * live leg HERE, in the JVM; the payload drift fails both.
 */
public class GT6LogisticsRegistrationTest {

	private static final ResourceLocation ID = new ResourceLocation("gt6", "logistics_wire");
	private static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// the block-construction write window (the GT6CFoamFamilyTest :69-77 shape) — the
		// Block ctor registers its intrusive holder, so the payload makers need it offline
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** The single-path identity of the three-part registration (the ADR-P3-4 self-contained form). */
	@Test
	public void registrationIdentityIsOnePath() {
		assertEquals("logistics_wire", GT6Logistics.WIRE_PATH);
		assertEquals(ID, GT6Logistics.LOGISTICS_WIRE.getId());
		assertEquals(ID, GT6Logistics.LOGISTICS_WIRE_ITEM.getId());
		assertEquals(ID, GT6Logistics.LOGISTICS_WIRE_BE.getId());
	}

	/** The containment half (live leg) / the payload+mount half (offline leg) — see the class doc. */
	@Test
	public void registriesContainAndMountGt6LogisticsWire() {
		if (BuiltInRegistries.BLOCK.containsKey(ID)) { // the FML-booted leg: the mod face is in the real registries (leg-agnostic probe — RegistryObject/DeferredHolder carry different presence APIs)
			// THE id686 guard — this JVM's registries are the real mod runtime view
			assertTrue(BuiltInRegistries.BLOCK.containsKey(ID), "block face registered");
			assertTrue(BuiltInRegistries.ITEM.containsKey(ID), "item face registered");
			assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ID), "BET face registered");
			assertSame(GT6Logistics.LOGISTICS_WIRE.get(), BuiltInRegistries.BLOCK.get(ID));
			assertSame(GT6Logistics.LOGISTICS_WIRE_ITEM.get(), BuiltInRegistries.ITEM.get(ID));
			BlockEntityType<?> tType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ID);
			assertNotNull(tType);
			assertTrue(tType.isValid(GT6Logistics.LOGISTICS_WIRE.get().defaultBlockState()),
					"the BET must be mounted on gt6:logistics_wire — an unmounted type is the id686 failure shape");
			assertWireFactory(tType.create(POS, GT6Logistics.LOGISTICS_WIRE.get().defaultBlockState()));
		} else {
			// the offline leg: the payload+mount half (the CFoam fixture shape — the explicit
			// (type,pos,state) ctor, the 2-arg form is the live FML factory whose fallback
			// resolves the real registry type). The item/BE payload CONSTRUCTIONS beyond the
			// block are frozen-registry-gated on this leg (the GTOfflineTestBase doc) and
			// ride the live branch's containment asserts.
			GTLogisticsWireBlock tBlock = GT6Logistics.makeWireBlock();
			@SuppressWarnings("unchecked")
			BlockEntityType<GTLogisticsWireBlockEntity>[] tHolder = (BlockEntityType<GTLogisticsWireBlockEntity>[]) new BlockEntityType<?>[1];
			tHolder[0] = BlockEntityType.Builder.of(
					(aPos, aState) -> new GTLogisticsWireBlockEntity(tHolder[0], aPos, aState), tBlock).build(null);
			assertTrue(tHolder[0].isValid(tBlock.defaultBlockState()), "the offline BET mounts the payload block");
			assertWireFactory(tHolder[0].create(POS, tBlock.defaultBlockState()));
		}
	}

	/** The factory half: the BET builds the wire BE, and the TE name mirrors the path. */
	private static void assertWireFactory(BlockEntity aCreated) {
		assertInstanceOf(GTLogisticsWireBlockEntity.class, aCreated);
		assertEquals("logistics_wire", ((GTLogisticsWireBlockEntity)aCreated).getTileEntityName());
	}
}
