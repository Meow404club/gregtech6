package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
//? if forge {
import net.minecraft.data.worldgen.BootstapContext;
//?} else {
/*import net.minecraft.data.worldgen.BootstrapContext; // 21.1: typo fixed
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import org.junit.jupiter.api.Test;

/**
 * The GTRenderUpdates pair: server side forwards through blockEvent, client side fires the
 * full sendBlockUpdated + requestModelDataUpdate pair (GTCEu IGregtechBlockEntity.java:49-61
 * semantics), null level is a no-op. The Level double follows the proven offline recipe of
 * GTRecipesOfflineTestBase.MinimalLevel (damage-type registry access, hand-made dimension
 * holder), extended with an isClientSide flag and event recording.
 */
public class GTRenderUpdatesTest extends GTOfflineRenderTestBase {

	/** Records the calls the pair is allowed to make, in order, across level and BE. */
	static class RecordingLevel extends Level {
		final List<String> mTimeline = new java.util.ArrayList<>();
		int mLastBlockEventId = -1;
		int mLastBlockEventParam = -1;
		Block mLastBlockEventBlock = null;
		int mLastSendBlockUpdatedFlags = -1;

		RecordingLevel(boolean aClientSide) {
			super(null, Level.OVERWORLD, damageTypeRegistryAccess(), dimensionHolder(),
					() -> net.minecraft.util.profiling.InactiveProfiler.INSTANCE, aClientSide, false, 0L, 0);
		}

		//? if neoforge {
		/*// 21.1: the day-time-scaling triple joined the Level abstracts.
		@Override public float getDayTimeFraction() { return 0.0F; }
		@Override public void setDayTimeFraction(float aFraction) {}
		@Override public float getDayTimePerTick() { return 0.0F; }
		@Override public void setDayTimePerTick(float aPerTick) {}
		@Override public net.minecraft.world.item.alchemy.PotionBrewing potionBrewing() { return null; } // 21.1: vanilla brewing holder abstract
		@Override public net.minecraft.world.TickRateManager tickRateManager() { return null; }
		*///?}

		static RegistryAccess damageTypeRegistryAccess() {
			//? if forge {
			MappedRegistry<DamageType> tDamageTypes = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
			DamageTypes.bootstrap(new BootstapContext<DamageType>() {
				@Override public Holder.Reference<DamageType> register(ResourceKey<DamageType> aKey, DamageType aValue, Lifecycle aLifecycle) {
					return tDamageTypes.register(aKey, aValue, aLifecycle);
				}
				@Override public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> aRegistry) { throw new UnsupportedOperationException(); }
			});
			//?} else {
			/*// 21.1: BootstapContext → BootstrapContext; MappedRegistry.register takes the RegistrationInfo.
			MappedRegistry<DamageType> tDamageTypes = new MappedRegistry<>(Registries.DAMAGE_TYPE, Lifecycle.stable());
			DamageTypes.bootstrap(new BootstrapContext<DamageType>() {
				@Override public net.minecraft.core.Holder.Reference<DamageType> register(ResourceKey<DamageType> aKey, DamageType aValue, Lifecycle aLifecycle) {
					return tDamageTypes.register(aKey, aValue, net.minecraft.core.RegistrationInfo.BUILT_IN);
				}
				@Override public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> aRegistry) { throw new UnsupportedOperationException(); }
			});
			*///?}
			return new RegistryAccess.ImmutableRegistryAccess(Map.of(Registries.DAMAGE_TYPE, tDamageTypes)).freeze();
		}

		static Holder<DimensionType> dimensionHolder() {
			ResourceKey<DimensionType> tKey = ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation("gt6:test"));
			DimensionType tType = new DimensionType(java.util.OptionalLong.empty(), true, false, false, true, 1.0, true, false, -64, 384, 384,
					net.minecraft.tags.TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft:infiniburn_overworld")),
					new ResourceLocation("minecraft:overworld"), 0.0F, null);
			return new Holder<DimensionType>() {
				@Override public DimensionType value() { return tType; }
				@Override public boolean isBound() { return true; }
				@Override public boolean is(ResourceLocation aId) { return false; }
				@Override public boolean is(ResourceKey<DimensionType> aKey) { return false; }
				@Override public boolean is(java.util.function.Predicate<ResourceKey<DimensionType>> aPredicate) { return false; }
				@Override public boolean is(net.minecraft.tags.TagKey<DimensionType> aTagKey) { return false; }
				//? if neoforge {
				/*@Override public boolean is(Holder<DimensionType> aHolder) { return false; } // 21.1: holder-identity probe
				*///?}
				@Override public java.util.stream.Stream<net.minecraft.tags.TagKey<DimensionType>> tags() { return java.util.stream.Stream.empty(); }
				@Override public Either<ResourceKey<DimensionType>, DimensionType> unwrap() { return Either.left(tKey); }
				@Override public java.util.Optional<ResourceKey<DimensionType>> unwrapKey() { return java.util.Optional.of(tKey); }
				@Override public Holder.Kind kind() { return Holder.Kind.REFERENCE; }
				@Override public boolean canSerializeIn(HolderOwner<DimensionType> aOwner) { return true; }
			};
		}

		@Override
		public void blockEvent(BlockPos aPos, Block aBlock, int aId, int aParam) {
			mTimeline.add("blockEvent");
			mLastBlockEventId = aId;
			mLastBlockEventParam = aParam;
			mLastBlockEventBlock = aBlock;
		}

		@Override
		public void sendBlockUpdated(BlockPos aPos, BlockState aOld, BlockState aNew, int aFlags) {
			mTimeline.add("sendBlockUpdated");
			mLastSendBlockUpdatedFlags = aFlags;
		}

		@Override public void playSeededSound(@org.jetbrains.annotations.Nullable Player aPlayer, Entity aEntity, Holder<net.minecraft.sounds.SoundEvent> aSound, net.minecraft.sounds.SoundSource aSource, float aVolume, float aPitch, long aSeed) {}
		@Override public void playSeededSound(@org.jetbrains.annotations.Nullable Player aPlayer, double aX, double aY, double aZ, Holder<net.minecraft.sounds.SoundEvent> aSound, net.minecraft.sounds.SoundSource aSource, float aVolume, float aPitch, long aSeed) {}
		@Override public String gatherChunkSourceStats() { return ""; }
		@Override public Entity getEntity(int aId) { return null; }
		//? if forge {
		@Override public MapItemSavedData getMapData(String aName) { return null; }
		@Override public void setMapData(String aName, MapItemSavedData aData) {}
		//?} else {
		/*// 21.1: the map-data table keys on the MapId record.
		@Override public MapItemSavedData getMapData(net.minecraft.world.level.saveddata.maps.MapId aId) { return null; }
		@Override public void setMapData(net.minecraft.world.level.saveddata.maps.MapId aId, MapItemSavedData aData) {}
		*///?}
		//? if forge {
		@Override public int getFreeMapId() { return 0; }
		//?} else {
		/*@Override public net.minecraft.world.level.saveddata.maps.MapId getFreeMapId() { return new net.minecraft.world.level.saveddata.maps.MapId(0); } // 21.1: int → MapId record
		*///?}
		@Override public void destroyBlockProgress(int aBreakerId, BlockPos aPos, int aProgress) {}
		@Override public Scoreboard getScoreboard() { return new Scoreboard(); }
		//? if forge {
		@Override public void gameEvent(net.minecraft.world.level.gameevent.GameEvent aEvent, Vec3 aPos, net.minecraft.world.level.gameevent.GameEvent.Context aContext) {}
		//?} else {
		/*@Override public void gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> aEvent, net.minecraft.world.phys.Vec3 aPos, net.minecraft.world.level.gameevent.GameEvent.Context aContext) {} // 21.1: the event rides a Holder
		*///?}
		@Override public void levelEvent(@org.jetbrains.annotations.Nullable Player aPlayer, int aLevelEvent, BlockPos aPos, int aData) {}
		@Override public net.minecraft.world.item.crafting.RecipeManager getRecipeManager() { return null; }
		@Override public ChunkSource getChunkSource() { return null; }
		@Override public LevelTickAccess<Block> getBlockTicks() { return null; }
		@Override public LevelTickAccess<Fluid> getFluidTicks() { return null; }
		@Override public float getShade(net.minecraft.core.Direction aDirection, boolean aShadeAmbientOcclusion) { return 1.0F; }
		@Override public Holder<Biome> getUncachedNoiseBiome(int aX, int aY, int aZ) { return null; }
		@Override public int getBlockTint(BlockPos aPos, net.minecraft.world.level.ColorResolver aResolver) { return 0; }
		@Override public java.util.List<net.minecraft.world.phys.shapes.VoxelShape> getEntityCollisions(@org.jetbrains.annotations.Nullable Entity aEntity, AABB aBox) { return java.util.List.of(); }
		@Override public java.util.List<? extends Player> players() { return java.util.List.of(); }
		@Override public FeatureFlagSet enabledFeatures() { return FeatureFlagSet.of(); }
		// Forge-patched abstract member (not present in the vanilla decompile).
		@Override protected LevelEntityGetter<Entity> getEntities() { return null; }
	}

	/** Test BE: vanilla stone state, records requestModelDataUpdate on the shared timeline. */
	static class RenderBE extends BlockEntity {
		final List<String> mTimeline;

		RenderBE(List<String> aTimeline) {
			// null BlockEntityType follows the established offline BE pattern
			// (TileEntityBase03DispatchTest.RecordingBE); the state must be real because
			// the server branch uses it as the blockEvent payload.
			super(null, BlockPos.ZERO, Blocks.STONE.defaultBlockState());
			mTimeline = aTimeline;
		}

		@Override
		public void requestModelDataUpdate() {
			mTimeline.add("requestModelDataUpdate");
		}
	}

	@Test
	public void serverSideForwardsThroughBlockEvent() {
		RecordingLevel tLevel = new RecordingLevel(false);
		RenderBE tTile = new RenderBE(tLevel.mTimeline);
		tTile.setLevel(tLevel);

		GTRenderUpdates.scheduleRenderUpdate(tTile);

		assertEquals(List.of("blockEvent"), tLevel.mTimeline, "server side: exactly one blockEvent, no client calls");
		assertEquals(GTRenderUpdates.RENDER_UPDATE_EVENT_ID, tLevel.mLastBlockEventId,
				"the reserved chunk-re-render id (GTCEu PipeBlockEntity.java:308)");
		assertEquals(0, tLevel.mLastBlockEventParam);
		assertSame(Blocks.STONE, tLevel.mLastBlockEventBlock,
				"the block of the BE's blockstate is the event payload");
		assertEquals(-1, tLevel.mLastSendBlockUpdatedFlags, "no client-side calls on the server branch");
	}

	@Test
	public void clientSideFiresTheFullPair() {
		RecordingLevel tLevel = new RecordingLevel(true);
		RenderBE tTile = new RenderBE(tLevel.mTimeline);
		tTile.setLevel(tLevel);

		GTRenderUpdates.scheduleRenderUpdate(tTile);

		// The pair in GTCEu order (IGregtechBlockEntity.java:55-56): chunk rebuild first
		// (sendBlockUpdated → ClientLevel:554 → setSectionDirty:2540), then the snapshot
		// cache refresh (requestModelDataUpdate → ModelDataManager.requestRefresh).
		assertEquals(List.of("sendBlockUpdated", "requestModelDataUpdate"), tLevel.mTimeline,
				"client side fires the FULL pair — requestModelDataUpdate alone never schedules a chunk rebuild");
		assertEquals(Block.UPDATE_IMMEDIATE, tLevel.mLastSendBlockUpdatedFlags,
				"UPDATE_IMMEDIATE drives the immediate rebuild path");
	}

	@Test
	public void nullLevelIsANoOp() {
		RenderBE tTile = new RenderBE(new java.util.ArrayList<>());

		GTRenderUpdates.scheduleRenderUpdate(tTile);

		assertTrue(tTile.mTimeline.isEmpty(), "no level: nothing scheduled, no crash");
	}

	@Test
	public void eventConstantMatchesTheGtceuWireSemantics() {
		assertEquals(1, GTRenderUpdates.RENDER_UPDATE_EVENT_ID,
				"GTCEu PipeBlockEntity.triggerEvent:307-315 reserves id 1 for chunk re-render");
	}
}
