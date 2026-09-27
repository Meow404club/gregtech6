package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterialStack;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.tileentity.GTItemStackHandler;

/**
 * The LARGE CRUCIBLE item-input face (issue #20 sub-task B): the slot-0 suck (:204),
 * the feed ladder melt (:206-234), the slot-0 NBT persistence, the useTop structure
 * gate (:461) and the wall-part item relay resolution (the hopper path) — the offline
 * half of the input chain (the capability forwarding itself is exercised on the live
 * server only, the MultiBlockPartBlockEntityTest posture).
 */
public class GTMultiBlockCrucibleInputTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestCrucible> sCrucibleType;
	static BlockEntityType<CrucibleWallBlockEntity> sWallType;
	static MaterialPrefixItem DUST_IRON;

	/** The concrete test BE — the crucible over a vanilla-block BET, wall and suck bound. */
	public static final class TestCrucible extends TileEntityCrucible {
		/** The suck stub: the stack the next {@link #suckCavityItem()} answers (the stub world has no entity list). */
		ItemStack mNextSuck = null;

		public TestCrucible(BlockPos aPos, BlockState aState) {
			super(sCrucibleType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		/** The protected-visibility bridge for the tests (the base carrier, never shadowed). */
		public GTItemStackHandler inv() {
			return mInventory;
		}
		@Override
		protected ItemStack suckCavityItem() {
			ItemStack rStack = mNextSuck;
			mNextSuck = null;
			return rStack == null ? ItemStack.EMPTY : rStack;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildInputFixtures() {
		ProbeBoot.boot();
		DUST_IRON = ProbeBoot.probePrefix("crucible_input_probe_dust_iron", () -> new MaterialPrefixItem(new Item.Properties(), OP.dust, MT.Iron));
		BlockEntityType<TestCrucible>[] tHolder = (BlockEntityType<TestCrucible>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestCrucible::new, Blocks.BRICKS).build(null);
		sCrucibleType = tHolder[0];
		BlockEntityType<CrucibleWallBlockEntity>[] tWallHolder = (BlockEntityType<CrucibleWallBlockEntity>[]) new BlockEntityType<?>[1];
		tWallHolder[0] = BlockEntityType.Builder.of(CrucibleWallBlockEntity::new, Blocks.BRICKS).build(null);
		sWallType = tWallHolder[0];
	}

	/** A formed crucible at (100, 64, 100) with its relaying wall ring (the physics-suite relay fixture). */
	private record Formed(MultiBlockLevel level, TestCrucible crucible, CrucibleWallBlockEntity feedWall) {}

	private static Formed formedCrucible() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		java.util.Map<BlockPos, CrucibleWallBlockEntity> tWalls = new java.util.HashMap<>();
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) {
				CrucibleWallBlockEntity tPart = new CrucibleWallBlockEntity(sWallType, new BlockPos(100 + tDX, 64 + tY, 100 + tDZ), Blocks.BRICKS.defaultBlockState());
				tPart.setLevel(tLevel);
				tLevel.mStates.put(tPart.getBlockPos(), Blocks.BRICKS.defaultBlockState());
				tLevel.mBlockEntities.put(tPart.getBlockPos(), tPart);
				tWalls.put(tPart.getBlockPos(), tPart);
			}
		}
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false), "the fixture structure forms");
		return new Formed(tLevel, tCrucible, tWalls.get(new BlockPos(101, 66, 100))); // the y+2 feed-layer wall
	}

	// ------------------------------------------------------------------
	// a) the slot-0 suck (:204) — the stub seam
	// ------------------------------------------------------------------

	@Test
	public void suckFillsTheEmptySlotThenTheLadderMeltsIt() {
		Formed tF = formedCrucible();
		// the melt sits exactly AT the iron melting point; the Steel ceiling stays clear
		tF.crucible().mTemperature = MT.Fe.mMeltingPoint;
		tF.crucible().mEnergy = 0; // no heat drift during the tick
		assertTrue(MT.Fe.mMeltingPoint < tF.crucible().getTemperatureMax((byte)0), "the iron melt fits under the Steel ceiling");

		tF.crucible().mNextSuck = new ItemStack(DUST_IRON, 1); // the cavity entity stub
		assertTrue(tF.crucible().inv().getStackInSlot(0).isEmpty(), "the feed slot starts empty");
		tF.crucible().onTick(1, true);

		assertTrue(tF.crucible().inv().getStackInSlot(0).isEmpty(), "the suck was consumed the same tick (:204→:216)");
		assertEquals(1, tF.crucible().mContent.size(), "the dust melted into the content (:206-216)");
		assertSame(MT.Fe, tF.crucible().mContent.get(0).mMaterial);
		assertEquals(CS.U, tF.crucible().totalContent(), "one dust = one unit");
	}

	@Test
	public void occupiedSlotIsNotSuckedOver() {
		Formed tF = formedCrucible();
		tF.crucible().inv().setStackInSlot(0, new ItemStack(DUST_IRON, 1));
		tF.crucible().mNextSuck = new ItemStack(DUST_IRON, 5); // would overwrite if the suck ran
		tF.crucible().mTemperature = MT.Fe.mMeltingPoint;
		tF.crucible().mEnergy = 0;
		tF.crucible().onTick(1, true);
		// the ladder melted only the slot's single item — the cavity stub still waits
		assertEquals(CS.U, tF.crucible().totalContent(), "the occupied slot never got sucked over (:204 slotHas gate)");
		assertNotNull(tF.crucible().mNextSuck, "the suck only runs on an empty slot");
	}

	// ------------------------------------------------------------------
	// b) the feed ladder (:206-234)
	// ------------------------------------------------------------------

	@Test
	public void feedLadderMeltsPrefixItemsByCount() {
		Formed tF = formedCrucible();
		tF.crucible().mTemperature = MT.Fe.mMeltingPoint;
		tF.crucible().mEnergy = 0;
		tF.crucible().inv().setStackInSlot(0, new ItemStack(DUST_IRON, 3));
		tF.crucible().onTick(1, true);
		assertEquals(0, tF.crucible().inv().getStackInSlot(0).getCount(), "the slot cleared after the melt (:216)");
		assertEquals(3 * CS.U, tF.crucible().totalContent(), "three dusts = three units (the port melts the whole slot)");
	}

	@Test
	public void feedLadderTrashesUnknownItems() {
		Formed tF = formedCrucible();
		tF.crucible().inv().setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.STICK, 1));
		tF.crucible().onTick(1, true);
		assertTrue(tF.crucible().inv().getStackInSlot(0).isEmpty(), "the unknown item was trashed (:210-212)");
		assertEquals(0, tF.crucible().totalContent(), "nothing entered the content");
	}

	// ------------------------------------------------------------------
	// c) the slot-0 NBT round-trip
	// ------------------------------------------------------------------

	@Test
	public void feedSlotRoundTripsThroughNBT() {
		Formed tF = formedCrucible();
		tF.crucible().inv().setStackInSlot(0, new ItemStack(DUST_IRON, 3));
		CompoundTag tTag = tF.crucible().saveWithoutMetadata();

		TestCrucible tRestored = sCrucibleType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);
		assertFalse(tRestored.inv().getStackInSlot(0).isEmpty(), "the feed slot survives save/load (gt.inv)");
		assertSame(DUST_IRON, tRestored.inv().getStackInSlot(0).getItem());
		assertEquals(3, tRestored.inv().getStackInSlot(0).getCount());
	}

	// ------------------------------------------------------------------
	// d) the useTop structure gate (:461)
	// ------------------------------------------------------------------

	@Test
	public void useTopGateRefusesUnformedAndAnswersFormed() {
		// an unformed controller: the click is refused (the block carrier PASSes on false)
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCrucible tBroken = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		assertFalse(tBroken.checkStructure(false), "no walls, no structure");
		assertFalse(tBroken.useTop(null, InteractionHand.MAIN_HAND), "an unformed crucible refuses the click (:461)");

		// the formed fixture answers (the null player consumes the gate test — the arms
		// need the live server, the TileEntitySmeltery useTop posture)
		Formed tF = formedCrucible();
		assertTrue(tF.crucible().useTop(null, InteractionHand.MAIN_HAND), "a formed crucible consumes the top click");
	}

	@Test
	public void useTopTakesTheFeedSlotBack() {
		Formed tF = formedCrucible();
		tF.crucible().inv().setStackInSlot(0, new ItemStack(DUST_IRON, 2));
		BagPlayer tPlayer = BagPlayer.withEmptyHand();
		assertTrue(tF.crucible().useTop(tPlayer, InteractionHand.MAIN_HAND), "the take-back consumes the click");
		assertTrue(tF.crucible().inv().getStackInSlot(0).isEmpty(), "the slot emptied (:470)");
		assertSame(DUST_IRON, tPlayer.mHeld.getItem(), "the player took the stack back (:470)");
		assertEquals(2, tPlayer.mHeld.getCount());
	}

	// ------------------------------------------------------------------
	// e) the wall-part item relay resolution (the hopper path)
	// ------------------------------------------------------------------

	@Test
	public void feedWallRelaysToTheControllerInventory() {
		Formed tF = formedCrucible();
		// the RESOLUTION half of the relay — the hopper's capability query on the live
		// server walks exactly this target (the MultiBlockPartBlockEntityTest posture:
		// ForgeCapabilities cannot class-init offline)
		assertSame(tF.crucible(), tF.feedWall().relayTarget(), "the feed-layer wall resolves its controller for the item relay");
		// the slot-0 carrier the relay terminus exposes accepts the push (the IItemHandler
		// face the VanillaInventoryCodeHooks insertHook drives)
		ItemStack tRejected = tF.crucible().inv().insertItem(0, new ItemStack(DUST_IRON, 3), false);
		assertTrue(tRejected.isEmpty(), "the feed slot accepted the hopper push");
		assertEquals(3, tF.crucible().inv().getStackInSlot(0).getCount());
		// and the pushed stack melts on the next tick (the full chain: hopper → slot → content)
		tF.crucible().mTemperature = MT.Fe.mMeltingPoint;
		tF.crucible().mEnergy = 0;
		tF.crucible().onTick(1, true);
		assertEquals(3 * CS.U, tF.crucible().totalContent(), "the pushed stack melted into the content");
	}

	@Test
	public void orphanWallResolvesNoRelay() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		CrucibleWallBlockEntity tOrphan = new CrucibleWallBlockEntity(sWallType, new BlockPos(101, 66, 100), Blocks.BRICKS.defaultBlockState());
		tOrphan.setLevel(tLevel);
		assertNull(tOrphan.relayTarget(), "an unlinked wall has no relay target");
	}

	// ------------------------------------------------------------------
	// the offline bag player (the GT6AnvilBlockEntityTest Unsafe-allocation form)
	// ------------------------------------------------------------------

	/** The minimal bag player: empty hand, real Inventory — enough for the take-back arm. */
	static final class BagPlayer extends net.minecraft.world.entity.player.Player {
		ItemStack mHeld = ItemStack.EMPTY;

		private BagPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

		static BagPlayer withEmptyHand() {
			try {
				java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tTheUnsafe.setAccessible(true);
				BagPlayer tPlayer = (BagPlayer) ((sun.misc.Unsafe) tTheUnsafe.get(null)).allocateInstance(BagPlayer.class);
				Field tInventoryField = net.minecraft.world.entity.player.Player.class.getDeclaredField("inventory");
				tInventoryField.setAccessible(true);
				tInventoryField.set(tPlayer, new net.minecraft.world.entity.player.Inventory(null)); // the anvil-test offline form
				tPlayer.mHeld = ItemStack.EMPTY; // Unsafe allocation skips field initializers
				return tPlayer;
			} catch (ReflectiveOperationException aE) {
				throw new IllegalStateException("the offline bag player failed", aE);
			}
		}

		@Override
		public ItemStack getItemInHand(InteractionHand aHand) {
			return mHeld; // the stub hand (the real Inventory.selected walk needs the entity level)
		}

		@Override
		public void setItemInHand(InteractionHand aHand, ItemStack aStack) {
			mHeld = aStack;
		}

		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
	}

	// ------------------------------------------------------------------
	// the shared offline boot (the TileEntitySmelteryOfflineTest ProbeBoot posture)
	// ------------------------------------------------------------------

	static final class ProbeBoot {
		static void boot() {
			SharedConstants.tryDetectVersion();
			try {
				Bootstrap.bootStrap();
			} catch (Throwable ignored) {
				// NetworkHooks.init() failure is expected offline; registries are ready by now.
			}
			MaterialRegistry.INSTANCE.open();
			MT.init();
			OP.init();
			MaterialGraph.applyCrucibleAlloyReferences();
			MaterialRegistry.INSTANCE.close();
		}

		static MaterialPrefixItem probePrefix(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
			net.minecraft.core.Registry<Item> tRegistry = BuiltInRegistries.ITEM;
			openOffline(tRegistry);
			MaterialPrefixItem rItem = aCreator.get();
			net.minecraft.core.Registry.register(tRegistry, new net.minecraft.resources.ResourceLocation("gt6", aProbeId), rItem);
			return rItem;
		}

		/** The registry open, best-effort PER FACE, shared by every offline registration site. */
		@SuppressWarnings("unchecked")
		static void openOffline(net.minecraft.core.Registry<?> aRegistry) {
			net.minecraft.core.Registry<Object> tRegistry = (net.minecraft.core.Registry<Object>)aRegistry;
			try {
				Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(tRegistry);
			} catch (Exception aE) {
				throw new IllegalStateException("could not unfreeze the offline registry", aE);
			}
			try {
				Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
				tDelegate.setAccessible(true);
				Object tForgeRegistry = tDelegate.get(tRegistry);
				Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
				tForgeUnfreeze.setAccessible(true);
				tForgeUnfreeze.invoke(tForgeRegistry);
			} catch (NoSuchFieldException | NoSuchMethodException ignored) {
				// the 21.1 face: no forge delegate behind the vanilla registry
			} catch (Exception aE) {
				throw new IllegalStateException("could not open the offline forge registry", aE);
			}
			try {
				Field tLocked = inheritedField(tRegistry.getClass(), "locked");
				tLocked.setBoolean(tRegistry, false);
			} catch (NoSuchFieldException ignored) {
				// the 21.1 face: nothing but the vanilla frozen flag to unlock
			} catch (Exception aE) {
				throw new IllegalStateException("could not clear the offline registry lock", aE);
			}
		}

		private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
			for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
				try {
					Field rField = tClass.getDeclaredField(aName);
					rField.setAccessible(true);
					return rField;
				} catch (NoSuchFieldException ignored) {}
			}
			throw new NoSuchFieldException(aName);
		}
	}
}
