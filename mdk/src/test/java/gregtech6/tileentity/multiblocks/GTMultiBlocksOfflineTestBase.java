package gregtech6.tileentity.multiblocks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;

import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * Offline boot + fixtures for the multiblock tests (task p4-multiblock-framework acceptance ①).
 * Every multiblock test class extends THIS one base — the class execution order is undefined,
 * and the vanilla registries must be bootstrapped before any Block/ItemStack class initializes.
 *
 * <p>Offline constraint (GTOfflineTestBase doc): the frozen registry forbids new Blocks, so
 * every fixture BET mounts {@link Blocks#BRICKS} as its valid block and the CokeOven fixture
 * overrides {@code getPartBlock()} to BRICKS (the registry-path .get() is unreachable offline).
 */
public abstract class GTMultiBlocksOfflineTestBase extends GTRecipesOfflineTestBase {

	/** Controller #1 — facing north (2), structure centre (100,64,101), box x[99,101] y[63,65] z[100,102]. */
	static final BlockPos C1 = new BlockPos(100, 64, 100);
	/** Controller #2 — facing north (2), structure centre (100,64,103), box z[102,104]; shares (101,65,102) with C1. */
	static final BlockPos C2 = new BlockPos(100, 64, 102);
	/** A C1 brick cell in BOTH structures' boxes (arbitration fixture): centre-relative (+1,+1,+1) for both. */
	static final BlockPos SHARED_CELL = new BlockPos(101, 65, 102);

	static BlockEntityType<TestCokeOven> sCokeOvenType;
	static BlockEntityType<MultiBlockPartBlockEntity> sPartType;
	static BlockEntityType<TestController> sTestControllerType;
	static BlockEntityType<InventoryController> sInventoryControllerType;

	@BeforeAll
	static void buildMultiBlockFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		sCokeOvenType = selfHolder(TestCokeOven::new);
		sPartType = selfHolder(MultiBlockPartBlockEntity::new);
		sTestControllerType = selfHolder(TestController::new);
		sInventoryControllerType = selfHolder(InventoryController::new);
	}

	/** The offline self-referencing BET holder (the GTMachinesOfflineTestBase recipe). */
	private static <T extends BlockEntity> BlockEntityType<T> selfHolder(BETFactory<T> aFactory) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> aFactory.create(tHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		return tHolder[0];
	}

	@FunctionalInterface
	interface BETFactory<T extends BlockEntity> {
		T create(BlockEntityType<T> aType, BlockPos aPos, BlockState aState);
	}

	/** Offline CokeOven bound to the fixture part block (Blocks.BRICKS). */
	static class TestCokeOven extends TileEntityCokeOven {
		TestCokeOven(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		protected Block getPartBlock() {
			return Blocks.BRICKS;
		}
	}

	/**
	 * A scripted controller for the template-logic tests: checkStructure2 pops the result
	 * queue (defaulting to mDefaultResult), mInside drives isInsideStructure.
	 */
	static class TestController extends TileEntityBase10MultiBlockBase {
		final Deque<Boolean> mResults = new ArrayDeque<>();
		boolean mDefaultResult = true;
		boolean mInside = true;
		int mCalls = 0;

		TestController(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_multiblock_controller";
		}

		@Override
		public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
			mCalls++;
			return mResults.isEmpty() ? mDefaultResult : mResults.pop();
		}

		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			return mInside;
		}
	}

	/** A controller with a one-slot item inventory — the capability-relay fixture. */
	static class InventoryController extends TileEntityBase10MultiBlockBase {
		final gregtech6.tileentity.GTItemStackHandler mInventory = new gregtech6.tileentity.GTItemStackHandler(1);

		InventoryController(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
			setInventory(mInventory);
		}

		@Override
		public String getTileEntityName() {
			return "test_inventory_controller";
		}

		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			return true;
		}
	}

	/**
	 * A Level double carrying a block-state map + block-entity map — the minimal "chunk"
	 * surface the multiblock path needs: getBlockState/getBlockEntity/setBlock (with the
	 * LevelChunk same-block CHECK-branch semantics: state flips keep the BE) + isLoaded.
	 * Extends the MinimalLevel recipe (the sendBlockUpdated/sound/etc. stubs are inherited).
	 */
	public static class MultiBlockLevel extends GTRecipesOfflineTestBase.MinimalLevel {

		public final Map<BlockPos, BlockState> mStates = new HashMap<>();
		public final Map<BlockPos, BlockEntity> mBlockEntities = new HashMap<>();
		/** The BE factory used when setBlock lands a fresh block-entity block (null = none created). */
		@Nullable
		public BiFunction<BlockPos, BlockState, BlockEntity> mBeFactory = null;

		public MultiBlockLevel() {
			super(new TestRecipeManager());
		}

		@Override
		public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos aPos) {
			return mStates.getOrDefault(aPos.immutable(), Blocks.AIR.defaultBlockState());
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return mBlockEntities.get(aPos.immutable());
		}

		@Override
		public boolean setBlock(BlockPos aPos, BlockState aState, int aFlags) {
			BlockPos tPos = aPos.immutable();
			BlockState tOld = getBlockState(tPos);
			mStates.put(tPos, aState);
			if (aState.isAir()) {
				mBlockEntities.remove(tPos);
			} else {
				// NOTE: no hasBlockEntity() gate — the fixture part block is a plain vanilla
				// block offline (the frozen registry forbids EntityBlocks), so the factory
				// IS the stub's block-entity creation step.
				BlockEntity tExisting = mBlockEntities.get(tPos);
				if (tExisting != null && tOld.is(aState.getBlock())) {
					tExisting.setBlockState(aState); // LevelChunk.setBlockState:292 CHECK branch — the BE survives flips
				} else if (mBeFactory != null) {
					BlockEntity tNew = mBeFactory.apply(tPos, aState);
					if (tNew != null) mBlockEntities.put(tPos, tNew);
				}
			}
			return true;
		}

		/** Level.getFluidState goes through getChunkAt — reroute it onto the state map. */
		@Override
		public net.minecraft.world.level.material.FluidState getFluidState(BlockPos aPos) {
			return getBlockState(aPos).getFluidState();
		}

		/** upstream worldObj.blockExists — the stub world is always loaded. */
		@Override
		public boolean isLoaded(BlockPos aPos) {
			return true;
		}

		/** The Forge-patched setChanged() guard (the MachineLevel recipe). */
		@Override
		public boolean hasChunkAt(BlockPos aPos) {
			return false;
		}

		/** The 01Root doBlockUpdate path must not hit the null ChunkSource. */
		@Override
		public void updateNeighborsAt(BlockPos aPos, Block aBlock) {}
	}

	/** Places a controller BE straight into the stub world (no EntityBlock involved). */
	static <T extends TileEntityBase10MultiBlockBase> T placeController(MultiBlockLevel aLevel, BlockEntityType<T> aType, BlockPos aPos, byte aFacing) {
		T tController = aType.create(aPos, Blocks.BRICKS.defaultBlockState());
		tController.setLevel(aLevel);
		tController.mFacing = aFacing;
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tController);
		return tController;
	}

	/** Places a part BE straight into the stub world. */
	static MultiBlockPartBlockEntity placePart(MultiBlockLevel aLevel, BlockPos aPos) {
		MultiBlockPartBlockEntity tPart = sPartType.create(aPos, Blocks.BRICKS.defaultBlockState());
		tPart.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tPart);
		return tPart;
	}
}
