package gregtech6.recipes;

import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Lifecycle;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import org.junit.jupiter.api.BeforeAll;

/**
 * Offline boot + minimal Level double for the recipes tests.
 *
 * <p>Vanilla registries need {@link SharedConstants#tryDetectVersion()} and
 * {@link Bootstrap#bootStrap()}; the Forge-patched bootStrap ends with
 * {@code NetworkHooks.init()}, which throws offline — by then vanilla items are
 * registered and that is exactly the state these tests need.
 *
 * <p>The Level constructor eagerly builds DamageSources, which resolves the
 * damage_type datapack registry; the stub therefore supplies one backed by
 * {@link DamageTypes} bootstrapped into a hand-made registry.
 */
public abstract class GTRecipesOfflineTestBase {

	private static boolean sIngredientSerializerRegistered = false;

	@org.junit.jupiter.api.BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		// ForgeMod.registerRecipeSerializers never runs offline, so the vanilla item
		// ingredient serializer is missing from CraftingHelper's dispatch map. Register
		// it once per JVM (@BeforeAll fires per test class).
		if (!sIngredientSerializerRegistered) {
			sIngredientSerializerRegistered = true;
			net.minecraftforge.common.crafting.CraftingHelper.register(
					new ResourceLocation("minecraft:item"), net.minecraftforge.common.crafting.VanillaIngredientSerializer.INSTANCE);
		}
	}

	/**
	 * Minimal Level double: only {@code getRecipeManager()} and
	 * {@code registryAccess()} are meaningful for the smelting bridge; the
	 * remaining abstracts are stubs (they live on ServerLevel/ClientLevel in
	 * vanilla, or come from the Forge patch).
	 */
	public static class MinimalLevel extends Level {
		private final RecipeManager mRecipeManager;

		public MinimalLevel(RecipeManager aRecipeManager) {
			super(null, Level.OVERWORLD, damageTypeRegistryAccess(), dimensionHolder(), () -> InactiveProfiler.INSTANCE, false, false, 0L, 0);
			mRecipeManager = aRecipeManager;
		}

		static RegistryAccess damageTypeRegistryAccess() {
			MappedRegistry<DamageType> tDamageTypes = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
			DamageTypes.bootstrap(new BootstapContext<DamageType>() {
				@Override public Holder.Reference<DamageType> register(ResourceKey<DamageType> aKey, DamageType aValue, Lifecycle aLifecycle) {
					return tDamageTypes.register(aKey, aValue, aLifecycle);
				}
				@Override public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> aRegistry) { throw new UnsupportedOperationException(); }
			});
			return new RegistryAccess.ImmutableRegistryAccess(Map.of(Registries.DAMAGE_TYPE, tDamageTypes)).freeze();
		}

		static Holder<DimensionType> dimensionHolder() {
			ResourceKey<DimensionType> tKey = ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation("gt6:test"));
			DimensionType tType = new DimensionType(java.util.OptionalLong.empty(), true, false, false, true, 1.0, true, false, -64, 384, 384,
					TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft:infiniburn_overworld")),
					new ResourceLocation("minecraft:overworld"), 0.0F, null);
			// Holder has no default methods; a tiny anonymous implementation avoids the
			// package-private Reference.bind* path entirely.
			return new Holder<DimensionType>() {
				@Override public DimensionType value() { return tType; }
				@Override public boolean isBound() { return true; }
				@Override public boolean is(ResourceLocation aId) { return false; }
				@Override public boolean is(ResourceKey<DimensionType> aKey) { return false; }
				@Override public boolean is(java.util.function.Predicate<ResourceKey<DimensionType>> aPredicate) { return false; }
				@Override public boolean is(TagKey<DimensionType> aTag) { return false; }
				@Override public java.util.stream.Stream<TagKey<DimensionType>> tags() { return java.util.stream.Stream.empty(); }
				@Override public Either<ResourceKey<DimensionType>, DimensionType> unwrap() { return Either.left(tKey); }
				@Override public java.util.Optional<ResourceKey<DimensionType>> unwrapKey() { return java.util.Optional.of(tKey); }
				@Override public Holder.Kind kind() { return Holder.Kind.REFERENCE; }
				@Override public boolean canSerializeIn(HolderOwner<DimensionType> aOwner) { return true; }
			};
		}

		@Override public RecipeManager getRecipeManager() { return mRecipeManager; }

		@Override public void sendBlockUpdated(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aOld, net.minecraft.world.level.block.state.BlockState aNew, int aFlags) {}
		@Override public void playSeededSound(@org.jetbrains.annotations.Nullable Player aPlayer, Entity aEntity, Holder<net.minecraft.sounds.SoundEvent> aSound, net.minecraft.sounds.SoundSource aSource, float aVolume, float aPitch, long aSeed) {}
		@Override public void playSeededSound(@org.jetbrains.annotations.Nullable Player aPlayer, double aX, double aY, double aZ, Holder<net.minecraft.sounds.SoundEvent> aSound, net.minecraft.sounds.SoundSource aSource, float aVolume, float aPitch, long aSeed) {}
		@Override public String gatherChunkSourceStats() { return ""; }
		@Override public Entity getEntity(int aId) { return null; }
		@Override public MapItemSavedData getMapData(String aName) { return null; }
		@Override public void setMapData(String aName, MapItemSavedData aData) {}
		@Override public int getFreeMapId() { return 0; }
		@Override public void destroyBlockProgress(int aBreakerId, net.minecraft.core.BlockPos aPos, int aProgress) {}
		@Override public Scoreboard getScoreboard() { return new Scoreboard(); }
		@Override public void gameEvent(net.minecraft.world.level.gameevent.GameEvent aEvent, net.minecraft.world.phys.Vec3 aPos, net.minecraft.world.level.gameevent.GameEvent.Context aContext) {}
		@Override public void levelEvent(@org.jetbrains.annotations.Nullable Player aPlayer, int aLevelEvent, net.minecraft.core.BlockPos aPos, int aData) {}
		@Override public ChunkSource getChunkSource() { return null; }
		@Override public LevelTickAccess<net.minecraft.world.level.block.Block> getBlockTicks() { return null; }
		@Override public LevelTickAccess<Fluid> getFluidTicks() { return null; }
		@Override public float getShade(net.minecraft.core.Direction aDirection, boolean aShadeAmbientOcclusion) { return 1.0F; }
		@Override public Holder<Biome> getUncachedNoiseBiome(int aX, int aY, int aZ) { return null; }
		@Override public int getBlockTint(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.ColorResolver aResolver) { return 0; }
		@Override public java.util.List<net.minecraft.world.phys.shapes.VoxelShape> getEntityCollisions(@org.jetbrains.annotations.Nullable Entity aEntity, net.minecraft.world.phys.AABB aBox) { return java.util.List.of(); }
		@Override public java.util.List<? extends Player> players() { return java.util.List.of(); }
		@Override public FeatureFlagSet enabledFeatures() { return FeatureFlagSet.of(); }
		// Forge-patched abstract member (not present in the vanilla decompile).
		@Override protected LevelEntityGetter<Entity> getEntities() { return null; }
	}

	/** Test RecipeManager exposing the protected apply() for direct offline JSON loading. */
	public static class TestRecipeManager extends RecipeManager {
		/** Bridge over the protected SimpleJsonResourceReloadListener entry point; it uses only the map argument. */
		public void load(Map<ResourceLocation, com.google.gson.JsonElement> aMap) {
			apply(aMap, null, null);
		}
	}
}
