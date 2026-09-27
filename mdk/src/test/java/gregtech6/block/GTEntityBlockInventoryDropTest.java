package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;

/**
 * GitHub #19 — the {@link GTEntityBlock} content-drop fallback, offline. Breaking a BE
 * block must scatter its exposed inventory (the vanilla ChestBlock.onRemove :230-241
 * duty the port never carried), a same-block swap must not, and the two gate layers must
 * hold slots back: the block-side canDrop ruling (the bottlecrate BlockEntityTag ride)
 * and the BE-declared upstream gate (the REAL ACT holo pair 31/32,
 * TileEntityAdvancedCraftingTable :770). A family that exposes no inventory (the barrel /
 * the hive posture) must stay untouched — no walk, no throw.
 *
 * <p>The walk is exercised through the real {@code onRemove} over a stub Level that
 * records what popResource hands to addFreshEntity — the MinimalLevel double
 * (GTRecipesOfflineTestBase) plus a BE map, a drop list and a null-safe removeBlockEntity
 * (the BlockBehaviour.onRemove tail hits it through super).
 */
public class GTEntityBlockInventoryDropTest extends GTRecipesOfflineTestBase {

	static BlockEntityType<TestTile> sTileType;
	static BlockEntityType<ContainerTile> sContainerType;
	static BlockEntityType<BareTile> sBareType;
	static BlockEntityType<TileEntityAdvancedCraftingTable> sActType;
	static GTEntityBlock sDropBlock;
	static GTEntityBlock sGateBlock;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	// -----------------------------------------------------------------------
	// fixtures
	// -----------------------------------------------------------------------

	/** The getInventory()-census shape (the machine/storage families' contract face). */
	public static class TestTile extends TileEntityBase03TicksAndSync {
		private final GTItemStackHandler mInventory = new GTItemStackHandler(4);

		public TestTile(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(true, aType, aPos, aState);
		}

		public GTItemStackHandler getInventory() {
			return mInventory;
		}

		@Override
		public String getTileEntityName() {
			return "test.drop_tile";
		}
	}

	/** The vanilla Container face — the GT6BumbliaryBlockEntity shape (implements Container). */
	public static class ContainerTile extends TileEntityBase03TicksAndSync implements Container {
		private final GTItemStackHandler mInventory = new GTItemStackHandler(2);

		public ContainerTile(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(true, aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test.container_tile";
		}

		@Override public int getContainerSize() { return mInventory.getSlots(); }
		@Override public boolean isEmpty() { return firstEmpty() < 0; }
		@Override public ItemStack getItem(int aSlot) { return mInventory.getStackInSlot(aSlot); }
		@Override public ItemStack removeItem(int aSlot, int aCount) {
			ItemStack tStack = mInventory.getStackInSlot(aSlot);
			if (tStack.isEmpty()) return ItemStack.EMPTY;
			ItemStack tSplit = tStack.split(aCount);
			if (tStack.isEmpty()) mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
			return tSplit;
		}
		@Override public ItemStack removeItemNoUpdate(int aSlot) {
			ItemStack tStack = mInventory.getStackInSlot(aSlot);
			mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
			return tStack;
		}
		@Override public void setItem(int aSlot, ItemStack aStack) { mInventory.setStackInSlot(aSlot, aStack); setChanged(); }
		@Override public boolean stillValid(Player aPlayer) { return true; }
		@Override public void clearContent() {
			for (int i = 0; i < mInventory.getSlots(); i++) mInventory.setStackInSlot(i, ItemStack.EMPTY);
		}

		private int firstEmpty() {
			for (int i = 0; i < mInventory.getSlots(); i++) if (mInventory.getStackInSlot(i).isEmpty()) return i;
			return -1;
		}
	}

	/** The no-exposed-inventory shape — the barrel/hive posture (nothing to drop, nothing thrown). */
	public static class BareTile extends TileEntityBase03TicksAndSync {
		public BareTile(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(true, aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test.bare_tile";
		}
	}

	public static class DropBlock extends GTEntityBlock {
		public DropBlock(Properties aProperties) {
			super(aProperties);
		}

		//? if neoforge {
		/*// 21.1 made BaseEntityBlock.codec() abstract (the GT6BumbliaryBlock fork shape).
		@Override
		protected com.mojang.serialization.MapCodec<? extends DropBlock> codec() {
			return simpleCodec(DropBlock::new);
		}
		*///?}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return sTileType;
		}
	}

	/** The block-side gate shape — the GT6StorageBlock bottlecrate ruling (canDrop = F). */
	public static class GateBlock extends GTEntityBlock {
		public GateBlock(Properties aProperties) {
			super(aProperties);
		}

		//? if neoforge {
		/*// 21.1 made BaseEntityBlock.codec() abstract (the GT6BumbliaryBlock fork shape).
		@Override
		protected com.mojang.serialization.MapCodec<? extends GateBlock> codec() {
			return simpleCodec(GateBlock::new);
		}
		*///?}

		@Override
		protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
			return sTileType;
		}

		@Override
		protected boolean canDrop(int aInventorySlot) {
			return false;
		}
	}

	@BeforeAll
	static void buildOfflineFixtures() {
		//? if forge {
		seedFluidTypeSize(); // the bare-JUnit leg needs the Entity ctor's FluidType read dead-headed
		//?}
		GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		sTileType = type(TestTile::new);
		sContainerType = type(ContainerTile::new);
		sBareType = type(BareTile::new);
		sActType = type(TileEntityAdvancedCraftingTable::new);
		sDropBlock = block(DropBlock::new);
		sGateBlock = block(GateBlock::new);
	}

	@FunctionalInterface
	private interface BEFactory<T extends BlockEntity> {
		T create(BlockEntityType<?> aType, BlockPos aPos, BlockState aState);
	}

	/**
	 * The bare-JUnit leg cannot construct an Entity: the Forge-patched ctor reads
	 * {@code FluidType.SIZE} (FluidType.java:71), a Lazy over ForgeRegistries.FLUID_TYPES
	 * that is dead offline. Seed the Lazy.Fast with its final value so vanilla popResource
	 * can build the drop ItemEntity (the GTOfflineTestBase ItemLatch form: a leg-pinned
	 * registry seam, repaired the day Forge reshapes the field).
	 */
	//? if forge {
	static void seedFluidTypeSize() {
		try {
			Object tLazy = net.minecraftforge.fluids.FluidType.class.getField("SIZE").get(null);
			java.lang.reflect.Field tSupplier = tLazy.getClass().getDeclaredField("supplier");
			tSupplier.setAccessible(true);
			tSupplier.set(tLazy, null);
			java.lang.reflect.Field tInstance = tLazy.getClass().getDeclaredField("instance");
			tInstance.setAccessible(true);
			tInstance.set(tLazy, 3); // any non-null int — the ctor only reads it
			// the ctor's second read: forgeFluidTypeOnEyes = ForgeMod.EMPTY_TYPE.get()
			// (the Entity.java.patch ctor tail) — resolve the FluidType RegistryObject the
			// same way with a bare instance (no registry dependency in the FluidType ctor).
			Object tEmptyType = net.minecraftforge.common.ForgeMod.class.getField("EMPTY_TYPE").get(null);
			java.lang.reflect.Field tValue = tEmptyType.getClass().getDeclaredField("value");
			tValue.setAccessible(true);
			if (tValue.get(tEmptyType) == null) {
				tValue.set(tEmptyType, new net.minecraftforge.fluids.FluidType(
						net.minecraftforge.fluids.FluidType.Properties.create()));
			}
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("could not seed the FluidType seams offline", aE);
		}
	}
	//?}

	private static <T extends BlockEntity> BlockEntityType<T> type(BEFactory<T> aFactory) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> aFactory.create(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		return tHolder[0];
	}

	/** Offline Block construction needs the block registry temporarily unfrozen (GTWireContactDamageTest form). */
	private static GTEntityBlock block(java.util.function.Function<BlockBehaviour.Properties, ? extends GTEntityBlock> aFactory) {
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return aFactory.apply(BlockBehaviour.Properties.of());
	}

	/**
	 * MinimalLevel plus the three faces the fallback touches: the BE lookup (the concrete
	 * Level.getBlockEntity walks the chunk source — null here), the BE removal (the
	 * BlockBehaviour.onRemove tail) and the popResource spawn (captured instead of spawned).
	 */
	static class DropCaptureLevel extends GTRecipesOfflineTestBase.MinimalLevel {
		final Map<BlockPos, BlockEntity> mBEs = new HashMap<>();
		final List<ItemEntity> mDrops = new ArrayList<>();

		DropCaptureLevel() {
			super(null);
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return mBEs.get(aPos);
		}

		@Override
		public void removeBlockEntity(BlockPos aPos) {
			mBEs.remove(aPos);
		}

		@Override
		public boolean addFreshEntity(Entity aEntity) {
			mDrops.add((ItemEntity) aEntity);
			return true;
		}

		@Override
		public GameRules getGameRules() {
			return new GameRules(); // the popResource RULE_DOBLOCKDROPS gate (MinimalLevel carries no LevelData)
		}
	}

	// -----------------------------------------------------------------------
	// the walks
	// -----------------------------------------------------------------------

	@Test
	public void machineBreakScattersItsInventory() {
		TestTile tTile = new TestTile(sTileType, POS, Blocks.STONE.defaultBlockState());
		tTile.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
		tTile.getInventory().setStackInSlot(3, new ItemStack(Items.DIAMOND, 2));
		DropCaptureLevel tLevel = level(tTile);

		sDropBlock.onRemove(sDropBlock.defaultBlockState(), tLevel, POS, Blocks.AIR.defaultBlockState(), false);

		assertEquals(2, tLevel.mDrops.size(), "both occupied slots pop, one ItemEntity each");
		assertEquals(7, tLevel.mDrops.stream().filter(e -> e.getItem().is(Items.IRON_INGOT)).findFirst().orElseThrow().getItem().getCount(),
				"the 7-stack lands whole");
		assertTrue(tLevel.mDrops.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND) && e.getItem().getCount() == 2),
				"the 2-stack lands whole");
		assertFalse(tLevel.mBEs.containsKey(POS), "super.onRemove still retires the BE");
	}

	@Test
	public void sameBlockSwapDropsNothing() {
		TestTile tTile = new TestTile(sTileType, POS, Blocks.STONE.defaultBlockState());
		tTile.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
		DropCaptureLevel tLevel = level(tTile);

		sDropBlock.onRemove(sDropBlock.defaultBlockState(), tLevel, POS, sDropBlock.defaultBlockState(), false);

		assertEquals(0, tLevel.mDrops.size(), "the !oldState.is(newState) vanilla convention — a like-for-like swap keeps the contents");
	}

	@Test
	public void containerBEDropsThroughTheVanillaFace() {
		ContainerTile tTile = new ContainerTile(sContainerType, POS, Blocks.STONE.defaultBlockState());
		tTile.setItem(0, new ItemStack(Items.BREAD, 3));
		tTile.setItem(1, new ItemStack(Items.APPLE, 5));
		DropCaptureLevel tLevel = level(tTile);

		sDropBlock.onRemove(sDropBlock.defaultBlockState(), tLevel, POS, Blocks.AIR.defaultBlockState(), false);

		assertEquals(2, tLevel.mDrops.size(), "Containers.dropContents walked the Container face (the Bumbliary shape)");
		assertEquals(3, tLevel.mDrops.stream().filter(e -> e.getItem().is(Items.BREAD)).findFirst().orElseThrow().getItem().getCount());
	}

	@Test
	public void blockCanDropGateHoldsSlotsBack() {
		TestTile tTile = new TestTile(sTileType, POS, Blocks.STONE.defaultBlockState());
		tTile.getInventory().setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 7));
		DropCaptureLevel tLevel = level(tTile);

		sGateBlock.onRemove(sGateBlock.defaultBlockState(), tLevel, POS, Blocks.AIR.defaultBlockState(), false);

		assertEquals(0, tLevel.mDrops.size(), "the bottlecrate ruling shape: canDrop = F keeps every slot with the BE (the BlockEntityTag ride)");
	}

	@Test
	public void bareFamilyStaysUntouched() {
		BareTile tTile = new BareTile(sBareType, POS, Blocks.STONE.defaultBlockState());
		DropCaptureLevel tLevel = level(tTile);

		sDropBlock.onRemove(sDropBlock.defaultBlockState(), tLevel, POS, Blocks.AIR.defaultBlockState(), false);

		assertEquals(0, tLevel.mDrops.size(), "no exposed inventory → no walk, no throw (the barrel/hive posture)");
	}

	// -----------------------------------------------------------------------
	// the BE-declared gate: the REAL ACT holo pair (upstream canDrop :511)
	// -----------------------------------------------------------------------

	@Test
	public void actHoloSlotsStayBehindOnBreak() {
		TileEntityAdvancedCraftingTable tAct = new TileEntityAdvancedCraftingTable(sActType, POS, Blocks.STONE.defaultBlockState());
		tAct.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
		tAct.getInventory().setStackInSlot(31, new ItemStack(Items.DIAMOND, 1));
		tAct.getInventory().setStackInSlot(32, new ItemStack(Items.GOLD_INGOT, 1));
		DropCaptureLevel tLevel = level(tAct);

		sDropBlock.onRemove(sDropBlock.defaultBlockState(), tLevel, POS, Blocks.AIR.defaultBlockState(), false);

		assertEquals(1, tLevel.mDrops.size(), "only the plain slot pops");
		assertTrue(tLevel.mDrops.get(0).getItem().is(Items.IRON_INGOT), "the pop is slot 0's stack");
		assertFalse(tAct.canDrop(31), "the upstream :511 pin — holo slot 31 answers F");
		assertFalse(tAct.canDrop(32), "holo slot 32 answers F");
		assertTrue(tAct.canDrop(30), "the selector band answers T");
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static DropCaptureLevel level(BlockEntity aTile) {
		DropCaptureLevel tLevel = new DropCaptureLevel();
		tLevel.mBEs.put(aTile.getBlockPos(), aTile);
		return tLevel;
	}
}
