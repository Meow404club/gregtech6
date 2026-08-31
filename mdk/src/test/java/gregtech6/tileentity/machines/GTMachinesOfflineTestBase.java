package gregtech6.tileentity.machines;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * Offline boot + fixtures for the machines tests (task p4-machine-oven acceptance 1).
 * Every machine test class extends THIS one base — the class execution order is undefined,
 * and the vanilla registries must be bootstrapped before any ItemStack class initializes.
 *
 * <p>The boot itself is the W1 recipes base ({@link GTRecipesOfflineTestBase}, which already
 * carries the MinimalLevel damage-type registry and the offline ingredient serializer).
 * {@link GT6RecipeMaps} is re-initialized per test (P1 registry discipline, W1 reset()
 * convention) so the BE's lazy RM.Furnace resolution always hits a live generation.
 */
public abstract class GTMachinesOfflineTestBase extends GTRecipesOfflineTestBase {

	static final BlockPos POS = new BlockPos(1, 2, 3);
	static final BlockPos POS2 = new BlockPos(4, 2, 3);

	/** The genuine vanilla data/minecraft/recipes/glass.json (1.20.1) — the W1-proven smelting fixture. */
	private static final String VANILLA_GLASS_RECIPE_JSON =
			"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:sand\"},\"result\":\"minecraft:glass\",\"experience\":0.1,\"cookingtime\":200}";

	static BlockEntityType<TileEntityOven> sOvenType;

	@BeforeAll
	static void buildOvenFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// offline holders avoid the RegistryObject.get() path of the runtime factory
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sOvenType = tHolder[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
		// task p8-d3 §③: the option-A fake source is a static test switch now (default
		// false = grid-fed). The offline fixtures keep it ON so the p4 acceptance keeps
		// its power premise; the net-mode tests flip it off at their own start and this
		// per-test restore keeps the classes independent of execution order.
		TileEntityOven.ENERGY_FAKE_SOURCE = true;
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/** A MinimalLevel whose smelting bridge resolves sand → glass. */
	static MachineLevel smeltingLevel() {
		TestRecipeManager tManager = new TestRecipeManager();
		JsonObject tJson = new Gson().fromJson(VANILLA_GLASS_RECIPE_JSON, JsonObject.class);
		Map<ResourceLocation, JsonElement> tMap = new HashMap<>();
		tMap.put(new ResourceLocation("minecraft", "glass"), tJson);
		tManager.load(tMap);
		return new MachineLevel(tManager);
	}

	/** A smelting level with an EMPTY RecipeManager (no recipes at all). */
	static MachineLevel emptyLevel() {
		return new MachineLevel(new TestRecipeManager());
	}

	/** Offline oven with a level attached (applyVisualState no-ops: the fixture block is not a GTOvenBlock). */
	static TileEntityOven makeOven(MachineLevel aLevel) {
		TileEntityOven tOven = sOvenType.create(POS, Blocks.BRICKS.defaultBlockState());
		tOven.setLevel(aLevel);
		return tOven;
	}

	/** Drives the 03 dispatcher for {@code aTicks} ticks. */
	static void drive(TileEntityOven aOven, int aTicks) {
		for (int i = 0; i < aTicks; i++) aOven.updateEntity();
	}

	/**
	 * MinimalLevel with a stubbable neighbor-signal state — the option-C redstone gate
	 * queries {@code hasNeighborSignal} per tick, which on the stub Level would NPE on the
	 * null ChunkSource without this override.
	 */
	public static class MachineLevel extends GTRecipesOfflineTestBase.MinimalLevel {
		public boolean mNeighborSignal = false;

		public MachineLevel(RecipeManager aRecipeManager) {
			super(aRecipeManager);
		}

		@Override
		public boolean hasNeighborSignal(BlockPos aPos) {
			return mNeighborSignal;
		}

		/**
		 * The stub has no ChunkSource — reporting "not loaded" makes the Forge-patched
		 * setChanged() (Level.blockEntityChanged → hasChunkAt) a no-op instead of an NPE.
		 */
		@Override
		public boolean hasChunkAt(BlockPos aPos) {
			return false;
		}

		/** The 01Root doBlockUpdate path (mDoesBlockUpdate at mTimer == 1) must not hit the null ChunkSource. */
		@Override
		public void updateNeighborsAt(BlockPos aPos, net.minecraft.world.level.block.Block aBlock) {}

		/** applyVisualState samples the fixture block state; the real chunk read would NPE on the stub. */
		@Override
		public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos aPos) {
			return net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState();
		}
	}
}
