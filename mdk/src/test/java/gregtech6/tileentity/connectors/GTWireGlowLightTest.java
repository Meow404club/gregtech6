/**
 * The offline truth tables of the wirelamp light chain (task p11-wire-brightness spec 1).
 * Upstream the 1.7.10 engine polled {@code bind4(TE.getLightValue())} (MultiTileEntityBlock
 * :212) with {@code getLightValue() = mIsGlowing ? mState : 0} (MultiTileEntityWireRedstone
 * :79) and moved the value through {@code onTickCheck} :51-58 / {@code setVisualData}
 * :62-67; the port lands the emission on {@code GTWireBlock.getLightEmission} (the
 * level-sensitive IForgeBlock.java:113 form, the official LevelSensitiveLightBlockTest
 * :82-89 shape) and the change chain on {@code onTickCheck} → {@code refreshGlowLight}
 * ({@code level.getLightEngine().checkBlock(pos)} = the :119 pattern). Pinned here: the
 * 0..15 ladder over mRedstone, the upstream CLASS split (glowing cable stays dark), the
 * BE-less safety, the change triggers (light GLOWING-gated, visual sync NOT) and the
 * two-channel sync surface ({@code gt.mredstone} rides getUpdateTag into client load()).
 */
package gregtech6.tileentity.connectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.block.wire.GTWireBlock;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.recipes.GTRecipesOfflineTestBase.MinimalLevel;
import gregtech6.tileentity.GTOfflineTestBase;

public class GTWireGlowLightTest extends GTOfflineTestBase {

	static BlockEntityType<GTWireBlockEntity> sType;
	static GTWireBlock sLumiumWire;   // the "Lumium Wirelamp" form (Loader:1901) — luminous, bare
	static GTWireBlock sLumiumCable;  // luminous MATERIAL on the INSULATED class upstream — stays dark
	static GTWireBlock sRedAlloyWire; // bare, but RedAlloy carries no GLOWING (Loader:1895)
	static GTWireBlock sElectricWire; // the tin row — the other family, the chain must not touch it

	static final BlockPos POS = new BlockPos(2, 64, 2);
	static final BlockPos POS2 = new BlockPos(2, 64, 3);

	/** Map-backed Level double (the GTWireStaleMaskTest.WireLevel form) with a client-side switch. */
	public static class WireLevel extends MinimalLevel {
		final Map<BlockPos, BlockEntity> mBlockEntities = new HashMap<>();
		final Map<BlockPos, BlockState> mStates = new HashMap<>();
		private boolean mClientSide = false;

		public WireLevel() {
			super(null);
		}

		public WireLevel client() {
			mClientSide = true;
			return this;
		}

		@Override
		public boolean isClientSide() {
			return mClientSide;
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos aPos) {
			return mBlockEntities.get(aPos);
		}

		/**
		 * The probe the light getter MUST use (IForgeBlock.java:106-110 worker-thread contract —
		 * {@code GTWireBlock.getLightEmission} calls {@code getExistingBlockEntity}). The default
		 * {@code instanceof Level} branch walks {@code hasChunk → level.getChunk} (IForgeBlockGetter
		 * :32-39), and this double's chunkSource is null — delegate straight to the map instead.
		 */
		//? if forge {
		@Override
		public BlockEntity getExistingBlockEntity(BlockPos aPos) {
			return mBlockEntities.get(aPos);
		}
		//?} else {
		/*// 21.1: the forge getExistingBlockEntity patch is gone — the class's own
		//getBlockEntity override is the vanilla read and needs no twin.
		*///?}

		@Override
		public BlockState getBlockState(BlockPos aPos) {
			return mStates.getOrDefault(aPos, Blocks.AIR.defaultBlockState());
		}
	}
	/** Records the {@code refreshGlowLight} triggers instead of calling the engine — a minimal Level has no chunk source to hand {@code getLightEngine()} (Level.java:333). */
	static final class CountingWire extends GTWireBlockEntity {
		int lightChecks;

		CountingWire(BlockPos aPos, BlockState aState) {
			super(sType, aPos, aState);
		}

		@Override
		protected void refreshGlowLight() {
			lightChecks++;
		}
	}

	@BeforeAll
	public static void buildOfflineFixture() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials();
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		sLumiumWire = new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, MT.Lumium, 1, false, 2, Family.REDSTONE,
				BlockBehaviour.Properties.of());
		sLumiumCable = new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, MT.Lumium, 1, true, 4, Family.REDSTONE,
				BlockBehaviour.Properties.of());
		sRedAlloyWire = new GTWireBlock(0, 1, GTWireSpecs.MAX_RANGE / 16, MT.RedAlloy, 1, false, 2, Family.REDSTONE,
				BlockBehaviour.Properties.of());
		sElectricWire = new GTWireBlock(32, 1, 2, MT.Sn, 1, false, 2, Family.ELECTRIC,
				BlockBehaviour.Properties.of());
		sType = BlockEntityType.Builder.of(GTWireBlockEntity::new, Blocks.STONE).build(null);
	}

	// ---------------------------------------------------------------------------
	// the emission truth table (GTWireBlock.getLightEmission = upstream :79)
	// ---------------------------------------------------------------------------

	@Test
	public void lumiumWirelampEmissionTracksTheSignalLadder() {
		WireLevel tLevel = new WireLevel();
		CountingWire tWire = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tWire.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tWire);
		tLevel.mStates.put(POS, sLumiumWire.defaultBlockState());
		assertTrue(sLumiumWire.luminous(), "the Lumium row carries GLOWING (MT.java:1792, the wirelamp pin)");
		assertTrue(tWire.glowingWire(), "luminous + bare = the glowing form");
		for (int tExpect = 0; tExpect <= 15; tExpect++) {
			tWire.mRedstone = GTWireSpecs.MAX_RANGE * (long) tExpect;
			assertEquals(tExpect, sLumiumWire.getLightEmission(sLumiumWire.defaultBlockState(), tLevel, POS),
					"emission = bind4(divup(mRedstone, MAX_RANGE)) — the upstream :53/:79 ladder");
		}
		// the divup ceil branch: just below an exact multiple still rounds UP
		tWire.mRedstone = GTWireSpecs.MAX_RANGE * 8 - 3;
		assertEquals(8, sLumiumWire.getLightEmission(sLumiumWire.defaultBlockState(), tLevel, POS));
	}

	@Test
	public void nonGlowingRowsStayDark() {
		WireLevel tLevel = new WireLevel();
		// RedAlloy: bare, but the material carries no GLOWING — the :79 mIsGlowing gate
		CountingWire tRedAlloy = new CountingWire(POS, sRedAlloyWire.defaultBlockState());
		tRedAlloy.mRedstone = GTWireSpecs.MAX_RANGE * 15;
		tLevel.mBlockEntities.put(POS, tRedAlloy);
		tLevel.mStates.put(POS, sRedAlloyWire.defaultBlockState());
		assertFalse(sRedAlloyWire.luminous(), "RedAlloy carries no GLOWING (Loader:1895)");
		assertFalse(tRedAlloy.glowingWire());
		assertEquals(0, sRedAlloyWire.getLightEmission(sRedAlloyWire.defaultBlockState(), tLevel, POS));
		// the Lumium CABLE: luminous MATERIAL, but upstream the INSULATED class
		// (MultiTileEntityWireRedstoneInsulated) implements no IMTE_GetLightValue — the
		// class split keeps the cable dark, verbatim
		CountingWire tCable = new CountingWire(POS2, sLumiumCable.defaultBlockState());
		tCable.mRedstone = GTWireSpecs.MAX_RANGE * 15;
		tLevel.mBlockEntities.put(POS2, tCable);
		tLevel.mStates.put(POS2, sLumiumCable.defaultBlockState());
		assertTrue(sLumiumCable.luminous(), "the flag is material-based — the cable row carries it too (p10 data pin)");
		assertFalse(tCable.glowingWire(), "the insulated class never answers the light query upstream");
		assertEquals(0, sLumiumCable.getLightEmission(sLumiumCable.defaultBlockState(), tLevel, POS2));
		// the electric family: the emission bridge is redstone-only, full signal or not
		CountingWire tElectric = new CountingWire(POS, sElectricWire.defaultBlockState());
		tElectric.mRedstone = GTWireSpecs.MAX_RANGE * 15; // electric rows never set this — proof by exhaustion
		tLevel.mBlockEntities.put(POS, tElectric);
		tLevel.mStates.put(POS, sElectricWire.defaultBlockState());
		assertEquals(0, sElectricWire.getLightEmission(sElectricWire.defaultBlockState(), tLevel, POS),
				"the electric family rides the vanilla default");
	}

	@Test
	public void emissionIsSafeWithoutABlockEntity() {
		WireLevel tLevel = new WireLevel(); // no BE anywhere
		assertEquals(0, sLumiumWire.getLightEmission(sLumiumWire.defaultBlockState(), tLevel, POS),
				"a BE-less glowing spot answers the vanilla default — no NPE out of the light engine");
	}

	// ---------------------------------------------------------------------------
	// the change chain (onTickCheck :51-58 / load = setVisualData :62-67)
	// ---------------------------------------------------------------------------

	@Test
	public void onTickCheckTriggersTheLightAndTheVisualSync() {
		CountingWire tWire = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tWire.mRedstone = GTWireSpecs.MAX_RANGE * 15; // as if the :104 scan had run (onTick precedes onTickCheck)
		assertTrue(tWire.onTickCheck(3), "the mState change returns true = the dispatcher's sendClientData trigger (:56)");
		assertEquals(15, tWire.mState, "the byte re-derived (:53)");
		assertEquals(1, tWire.lightChecks, "the :55 updateLightValue landing fired once");
		assertFalse(tWire.onTickCheck(3), "a converged byte reports no change (:58)");
		assertEquals(1, tWire.lightChecks, "no light churn on a steady signal");
		tWire.mRedstone = GTWireSpecs.MAX_RANGE * 5;
		assertTrue(tWire.onTickCheck(3));
		assertEquals(5, tWire.mState);
		assertEquals(2, tWire.lightChecks);
	}

	@Test
	public void theLightGateIsGlowingOnlyButTheSyncIsNot() {
		CountingWire tRedAlloy = new CountingWire(POS, sRedAlloyWire.defaultBlockState());
		tRedAlloy.mRedstone = GTWireSpecs.MAX_RANGE * 15;
		assertTrue(tRedAlloy.onTickCheck(3), "the visual sync fires for EVERY redstone row — :56 is not mIsGlowing-gated");
		assertEquals(0, tRedAlloy.lightChecks, "RedAlloy is not glowing — the :55 gate keeps the engine quiet");
		CountingWire tCable = new CountingWire(POS, sLumiumCable.defaultBlockState());
		tCable.mRedstone = GTWireSpecs.MAX_RANGE * 15;
		assertTrue(tCable.onTickCheck(3));
		assertEquals(0, tCable.lightChecks, "the insulated form never lights — the upstream class split");
	}

	@Test
	public void electricRowsKeepTheBaseCheck() {
		CountingWire tElectric = new CountingWire(POS, sElectricWire.defaultBlockState());
		assertFalse(tElectric.onTickCheck(3), "the base onTickCheck (false) — the electric tick chain untouched");
		assertEquals(0, tElectric.mState, "the byte only moves on redstone rows");
	}

	@Test
	public void clientLoadRelandsTheVisualDataAndTheLight() {
		WireLevel tLevel = new WireLevel().client();
		CountingWire tWire = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tWire.setLevel(tLevel);
		CompoundTag tTag = new CompoundTag();
		tTag.putLong(GTWireBlockEntity.NBT_MREDSTONE, GTWireSpecs.MAX_RANGE * 15);
		tWire.load(tTag);
		assertEquals(15, tWire.mState, "the client byte re-derived from the synced long (the :64 form)");
		assertEquals(1, tWire.lightChecks, "the setVisualData :62-67 landing re-checks the client light engine");
		assertFalse(tWire.onTickCheck(3), "the state is already current — no duplicate trigger");
	}

	@Test
	public void serverLoadLeavesTheLightToTheTickChain() {
		WireLevel tLevel = new WireLevel(); // server side: chunk load computes light from getLightEmission directly
		CountingWire tWire = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tWire.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tWire); // the engine's getBlockEntity probe
		CompoundTag tTag = new CompoundTag();
		tTag.putLong(GTWireBlockEntity.NBT_MREDSTONE, GTWireSpecs.MAX_RANGE * 15);
		tWire.load(tTag);
		assertEquals(0, tWire.lightChecks, "the server engine re-samples on its own — no client-path check");
		assertEquals(15, sLumiumWire.getLightEmission(sLumiumWire.defaultBlockState(), tLevel, POS),
				"the emission reads mRedstone regardless — the engine sees the right value at chunk load");
	}

	// ---------------------------------------------------------------------------
	// the sync surface (both vanilla channels already carry gt.mredstone)
	// ---------------------------------------------------------------------------

	@Test
	public void theSyncChannelsCarryTheSignal() {
		CountingWire tWire = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tWire.mRedstone = GTWireSpecs.MAX_RANGE * 9;
		CompoundTag tTag = tWire.getUpdateTag(); // = saveWithoutMetadata → saveAdditional
		assertTrue(tTag.contains(GTWireBlockEntity.NBT_MREDSTONE), "the chunk-data channel carries gt.mredstone");
		assertEquals(GTWireSpecs.MAX_RANGE * 9, tTag.getLong(GTWireBlockEntity.NBT_MREDSTONE));
		assertNotNull(tWire.getUpdatePacket(), "the block-update channel is armed (ClientboundBlockEntityDataPacket.create)");
		// the client lands in load() and re-derives the byte — the packet onTickCheck's
		// `true` return triggers is exactly the one this tag rides on
		WireLevel tClient = new WireLevel().client();
		CountingWire tMirror = new CountingWire(POS, sLumiumWire.defaultBlockState());
		tMirror.setLevel(tClient);
		tMirror.load(tTag);
		assertEquals(9, tMirror.mState);
		assertEquals(1, tMirror.lightChecks);
	}

	@Test
	public void theLuminousDataPinRidesTheLumiumRowOnly() {
		assertFalse(GTWireSpecs.REDSTONE_ROWS.get(0).luminous(), "RedAlloy (Loader:1893-1895)");
		assertFalse(GTWireSpecs.REDSTONE_ROWS.get(1).luminous(), "Signalum (Loader:1896-1898)");
		assertTrue(GTWireSpecs.REDSTONE_ROWS.get(2).luminous(), "Lumium — the GLOWING material (Loader:1899-1901)");
	}
}
