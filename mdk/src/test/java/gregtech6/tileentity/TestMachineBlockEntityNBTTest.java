package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.TestMachineBlock;

//? if forge {
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.items.IItemHandler;

/**
 * Acceptance 4 (task p3-be-framework): BE NBT round trip verified offline.
 * CompoundTag and BlockEntityType.Builder.of(...).build(null) construct without a
 * registry (see GTOfflineTestBase); load() has no Level dependency.
 *
 * <p>Offline fixtures use vanilla blocks (a fresh Block cannot be constructed after
 * the offline boot freezes the registries), which still exercises the shared-BET
 * multi-mount: one BlockEntityType over distinct blocks (ADR-P3-1). Ticking and
 * passive BE instances differ through the fixture {@code TestMachineBlock(false)}
 * state (mIsTicking mirrors what TestMachineBlock passes in-game). The GTEntityBlock
 * ticker lambda itself runs only against live blocks — covered by the WAVE-2 example
 * machine card.
 */
public class TestMachineBlockEntityNBTTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);

	static BlockEntityType<TestMachineBlockEntity> sType;

	/**
	 * The notick {@link TestMachineBlock} stand-in: the bare {@code TestMachineBlockEntity}
	 * constructor passed a null BET/state, which the 21.1 ctor validation rejects
	 * (validateBlockState → getType().isValid). The passive fixture block drives the same
	 * {@code mIsTicking = isTicking()} ctor branch through the full constructor instead
	 * (task p15-m4-test-infra-2).
	 */
	static TestMachineBlock sPassiveBlock;

	@BeforeAll
	static void buildOfflineFixtures() {
		// offline holders avoid the RegistryObject.get() path of the runtime factory
		@SuppressWarnings("unchecked")
		BlockEntityType<TestMachineBlockEntity>[] tHolder = (BlockEntityType<TestMachineBlockEntity>[]) new BlockEntityType<?>[1];
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		sPassiveBlock = new TestMachineBlock(false, BlockBehaviour.Properties.of());
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestMachineBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT, sPassiveBlock).build(null);
		sType = tHolder[0];
	}

	@Test
	public void sharedBETMountsBothBlocksAndCreatesOnDemand() {
		// ADR-P3-1: one BlockEntityType, two valid blocks
		assertTrue(sType.isValid(Blocks.STONE.defaultBlockState()));
		assertTrue(sType.isValid(Blocks.DIRT.defaultBlockState()));
		assertFalse(sType.isValid(Blocks.BRICKS.defaultBlockState()));

		TestMachineBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertTrue(tBe.mIsTicking, "BEs on non-GT blocks default to mIsTicking=true (ticking variant)");
		assertEquals("test_machine", tBe.getTileEntityName());
	}

	@Test
	public void nbtRoundTripPreservesInventoryAndCounters() {
		TestMachineBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());

		// inventory goes in through the capability handle (ADR-P3-2; the getCapability
		// branch against ForgeCapabilities.ITEM_HANDLER needs the live transformer stack
		// and is exercised by :mdk:runServer instead)
		//? if forge {
		LazyOptional<IItemHandler> tCap = tBe.itemHandlerCapability();
		assertTrue(tCap.isPresent());
		tCap.orElseThrow(IllegalStateException::new).insertItem(0, new ItemStack(Items.DIAMOND, 32), false);
		//?}
		//? if neoforge {
		/* // 21.1 seam: the store comes back directly, no LazyOptional wrapper.
		IItemHandler tCap = tBe.itemHandlerCapability();
		assertNotNull(tCap);
		tCap.insertItem(0, new ItemStack(Items.DIAMOND, 32), false);
		 *///?}

		// four dispatcher passes -> mTickCount == 4
		for (int i = 0; i < 4; i++) tBe.updateEntity();
		assertEquals(4, tBe.getTickCount());

		CompoundTag tSaved = tBe.saveWithoutMetadata();
		assertEquals("test_machine", tSaved.getString("te_name"));
		assertEquals(4L, tSaved.getLong("tick_count"));
		assertTrue(tSaved.contains("inventory", Tag.TAG_COMPOUND));

		TestMachineBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(4, tBack.getTickCount());

		//? if forge {
		LazyOptional<IItemHandler> tBackCap = tBack.itemHandlerCapability();
		assertSame(tBack.getInventory(), tBackCap.orElseThrow(IllegalStateException::new));
		assertEquals(32, tBackCap.orElseThrow(IllegalStateException::new).extractItem(0, 64, true).getCount());
		//?}
		//? if neoforge {
		/*
		IItemHandler tBackCap = tBack.itemHandlerCapability();
		assertSame(tBack.getInventory(), tBackCap);
		assertEquals(32, tBackCap.extractItem(0, 64, true).getCount());
		 *///?}
	}

	@Test
	public void syncChannelsCarryTheUpdateTagBothWays() {
		TestMachineBlockEntity tServer = sType.create(POS, Blocks.STONE.defaultBlockState());
		// the block-update channel (getUpdatePacket, vanilla ClientboundBlockEntityDataPacket
		// :31) reads level.registryAccess() — the map-backed MultiBlockLevel supplies it
		// (a bare MinimalLevel leaves setChanged NPE-ing on the null chunk source, the
		// dispatcher swallows it and the tick never counts)
		tServer.setLevel(new gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase.MultiBlockLevel());
		tServer.updateEntity();
		tServer.updateEntity();

		// chunk-data / block-update payload (getUpdateTag = saveWithoutMetadata, vanilla
		// BlockEntity.java:154 — the upstream getClientDataPacket(true) "send all" case)
		CompoundTag tUpdateTag = tServer.getUpdateTag();
		assertEquals("test_machine", tUpdateTag.getString("te_name"));
		assertEquals(2L, tUpdateTag.getLong("tick_count"));

		// block-update channel (getUpdatePacket, vanilla :150) = data packet over the update tag
		ClientboundBlockEntityDataPacket tPacket = (ClientboundBlockEntityDataPacket) tServer.getUpdatePacket();
		assertNotNull(tPacket);
		assertNotNull(tPacket.getTag());

		// client side: handleUpdateTag / onDataPacket default to load(tag) (IForgeBlockEntity :53/:68)
		TestMachineBlockEntity tClient = sType.create(POS, Blocks.STONE.defaultBlockState());
		tClient.handleUpdateTag(tUpdateTag);
		assertEquals(2, tClient.getTickCount());
	}

	//? if forge {
	@Test
	public void capabilityInvalidatesWithoutTouchingLifecycleCallbacks() {
		TestMachineBlockEntity tBe = sType.create(POS, Blocks.STONE.defaultBlockState());
		LazyOptional<IItemHandler> tCap = tBe.itemHandlerCapability();
		assertTrue(tCap.isPresent());

		tBe.invalidateCaps(); // Forge patch drives this from setRemoved()/onChunkUnloaded() (BlockEntity.java.patch:45/:51)
		assertFalse(tCap.isPresent());
		assertFalse(tBe.itemHandlerCapability().isPresent());
	}
	//?}
	//? if neoforge {
	/* // no 21.1 counterpart: with per-call providers there is no cached LazyOptional to
	   // invalidate — invalidateCapabilities() (IBlockEntityExtension) is a no-op for this
	   // face and needs no offline assertion.
	 *///?}

	@Test
	public void canUpdateFalseBEsNeverEnterTheDispatcher() {
		// the passive fixture block drives the notick branch of the full constructor
		// (mIsTicking = TestMachineBlock.isTicking() = false — the bare constructor form
		// the 21.1 ctor validation retired)
		TestMachineBlockEntity tPassive = new TestMachineBlockEntity(sType, POS, sPassiveBlock.defaultBlockState());
		assertFalse(tPassive.canUpdate(), "upstream TileEntityBase01Root.java:440 = mIsTicking && mShouldRefresh");

		// machines can also stop and resume by flipping mShouldRefresh (same upstream gate)
		TestMachineBlockEntity tStopped = sType.create(POS, Blocks.STONE.defaultBlockState());
		tStopped.mShouldRefresh = false;
		assertFalse(tStopped.canUpdate());
		tStopped.mShouldRefresh = true;
		assertTrue(tStopped.canUpdate());
	}

	@Test
	public void passiveHandlersSurviveARoundTripToo() {
		// full constructor over the passive fixture block + the injected 2-slot handler
		// (the setInventory seam the bare constructor used to serve)
		TestMachineBlockEntity tBe = new TestMachineBlockEntity(sType, POS, sPassiveBlock.defaultBlockState());
		tBe.setInventory(new GTItemStackHandler(2, () -> {}));
		tBe.getInventory().setStackInSlot(0, new ItemStack(Items.EMERALD, 9));

		CompoundTag tSaved = tBe.saveWithoutMetadata();
		TestMachineBlockEntity tBack = new TestMachineBlockEntity(sType, POS, sPassiveBlock.defaultBlockState());
		tBack.setInventory(new GTItemStackHandler(2, () -> {}));
		tBack.load(tSaved);

		assertEquals(9, tBack.getInventory().getStackInSlot(0).getCount());
	}
}
