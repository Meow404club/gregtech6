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

	//? if forge {
	/** The genuine vanilla data/minecraft/recipes/glass.json (1.20.1) — the W1-proven smelting fixture. */
	private static final String VANILLA_GLASS_RECIPE_JSON =
			"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:sand\"},\"result\":\"minecraft:glass\",\"experience\":0.1,\"cookingtime\":200}";
	//?} else {
	/*// The genuine vanilla data/minecraft/recipes/glass.json (1.21.1) — the result carries the
	// 1.20.5+ ItemStack.CODEC object form (SimpleCookingSerializer.java:23; RecipeManager.apply
	// :60 Recipe.CONDITIONAL_CODEC): the 1.20.1 string form dies with "Not a JSON object" and
	// every smeltingLevel() query comes up empty.
	private static final String VANILLA_GLASS_RECIPE_JSON =
			"{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"minecraft:sand\"},\"result\":{\"id\":\"minecraft:glass\"},\"experience\":0.1,\"cookingtime\":200}";
	*///?}

	static BlockEntityType<TileEntityOven> sOvenType;

	@BeforeAll
	static void buildOvenFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		unfreezeForIntrusiveHolders(); // task p34-machines-bumblelyzer-crucible — see the method doc
		// offline holders avoid the RegistryObject.get() path of the runtime factory
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sOvenType = tHolder[0];
	}

	/**
	 * The offline registry-open face (task p34-machines-bumblelyzer-crucible): the
	 * {@code BlockEntityType.Builder.build} (:342) writes an INTRUSIVE HOLDER
	 * (MappedRegistry.createIntrusiveHolder :384 → validateWrite :111), which the frozen
	 * registry refuses — the {@code Registry is already frozen} initializationError. Whether
	 * the flag is open at fixture time depends on the SUITE's class-discovery order (the
	 * GT6MeltingGateTest unfreeze probe runs before or after this base by filesystem luck —
	 * the p34 card's new classes shifted the neo order and exposed the fragility), so the base
	 * now carries the GT6MeltingGateTest.java:78-101 walk itself: idempotent (an unfrozen
	 * registry re-unfreezes harmlessly), test-JVM only.
	 */
	private static void unfreezeForIntrusiveHolders() {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE;
		//? if forge {
		try {
			// the Forge runtime shape: THREE locks (the GT6MeltingGateTest walk — the vanilla
			// frozen flag, the delegate ForgeRegistry.isFrozen, the NamespacedWrapper.locked gate)
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block-entity-type registry", aE);
		}
		try {
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
		} catch (NoSuchFieldException | NoSuchMethodException aE) {
			// a vanilla-shaped registry without the forge delegate — the vanilla frozen flag
			// already opened above is the whole gate (the 21.1 face)
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline forge registry", aE);
		}
		try {
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (NoSuchFieldException aE) {
			// no NamespacedWrapper lock — nothing further to open (the 21.1 face)
		} catch (Exception aE) {
			throw new IllegalStateException("could not clear the offline registry lock", aE);
		}
		//?} else {
		/*try {
			// the 21.1 runtime shape: the plain vanilla DefaultedMappedRegistry — a single
			// frozen flag guards the intrusive-holder construction (the GT6MeltingGateTest :97 form)
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not clear the offline registry lock", aE);
		}
		 *///?}
	}

	/** The superclass-walking field lookup (the GT6MeltingGateTest helper verbatim). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = tClass.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {}
		}
		throw new NoSuchFieldException(aName);
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
