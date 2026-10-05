package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The Sifting Table BE offline tests (task sifting-table-family): the four-chain
 * acceptance (place / activate / watch-execute / collect, MultiTileEntitySiftingTable
 * .java:275-299 + :236-264), the 13-slot capacity boundary (:441-443), the
 * containsInput reject gate (:452), the sided capability posture and the watch-arm
 * seam. The haste/fatigue potion modifiers fold to the identity (the anvil fold
 * precedent, GT6AnvilBlockEntity.java:84-87) — the progress math below is the folded
 * one-per-arm form. The exhaustion VALUE rides the {@code exhaustionOf} pin; the live
 * charge is the vanilla invulnerable-gated face (a null-level offline player cannot
 * take it — the causeFoodExhaustion gate Player.java:1689-1693).
 */
public class GT6SiftingTableBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6SiftingTableBlockEntity> sTableType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The fixture BET (the GT6AnvilBlockEntityTest self-referencing form — the live BET is registration-bound). */
	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6SiftingTableBlockEntity>[] tTable = (BlockEntityType<GT6SiftingTableBlockEntity>[]) new BlockEntityType<?>[1];
		tTable[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6SiftingTableBlockEntity(tTable[0], aPos, aState),
				Blocks.STONE).build(null);
		sTableType = tTable[0];
	}

	/** A fresh generation per test — the fixture rows below are the only writers. */
	@BeforeEach
	void freshMaps() {
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		// the SIFTING fixture row: 1 grass block → 2 wheat seeds + 1 dirt, eUt 16 x duration 16
		GT6RecipeMaps.SIFTING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.GRASS_BLOCK, 1)},
				new ItemStack[] {new ItemStack(Items.WHEAT_SEEDS, 2), new ItemStack(Items.DIRT, 1)},
				new FluidStack[0], new FluidStack[0], 16, 16, 0));
	}

	@AfterEach
	void teardown() {
		GT6RecipeMaps.reset();
	}

	/** A fresh offline table (the plain-stone carrier — the BE reads no block-carrier values). */
	private static GT6SiftingTableBlockEntity table() {
		return new GT6SiftingTableBlockEntity(sTableType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// chain ①: the top-face place arm (:277-285)
	// ---------------------------------------------------------------------------

	@Test
	public void topClickPlacesOneUnitOfRowInput() {
		GT6SiftingTableBlockEntity tTable = table();
		ItemStack tHeld = new ItemStack(Items.GRASS_BLOCK, 5);
		String tReport = tTable.activateChain(null, (byte) 1, tHeld, 0.5F, 0.9F, 0.5F);
		assertTrue(tReport.startsWith("placed"), tReport);
		// the slot-0 stack limit 1 (:443) — ONE unit lands, the held stack keeps the rest
		assertEquals(1, tTable.inventory().getStackInSlot(0).getCount(), "the :443 getInventoryStackLimit");
		assertEquals(4, tHeld.getCount(), "the :284 ST.move one-unit form");
		// the fresh placement resets the progress (:283)
		assertEquals(0, tTable.mClickCount);
	}

	@Test
	public void topClickRefusesNonRowInput() {
		GT6SiftingTableBlockEntity tTable = table();
		ItemStack tHeld = new ItemStack(Items.DIAMOND, 2); // no SIFTING row carries a diamond input
		String tReport = tTable.activateChain(null, (byte) 1, tHeld, 0.5F, 0.9F, 0.5F);
		assertFalse(tReport.startsWith("placed"), tReport);
		assertTrue(tTable.inventory().getStackInSlot(0).isEmpty(), "the :452 containsInput gate");
		assertEquals(2, tHeld.getCount(), "the refused stack stays in hand");
	}

	@Test
	public void topCornerClickIsTheNeiSeat() {
		// :279 — both in-plane coords <= PX_P[2] = the 2px corner
		assertTrue(GT6SiftingTableBlockEntity.neiCorner(0.1F, 0.1F));
		assertTrue(GT6SiftingTableBlockEntity.neiCorner(2.0F / 16.0F, 2.0F / 16.0F));
		assertFalse(GT6SiftingTableBlockEntity.neiCorner(0.2F, 0.1F), "0.2 > the 2px bound");
		GT6SiftingTableBlockEntity tTable = table();
		String tReport = tTable.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.05F, 0.9F, 0.05F);
		assertTrue(tReport.contains("NEI corner"), tReport);
	}

	// ---------------------------------------------------------------------------
	// chain ②: the activate arm (:280-282 + the watch gate :236-239)
	// ---------------------------------------------------------------------------

	@Test
	public void topClickWithMaterialActivatesAndUnwatchedTickDropsIt() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.GRASS_BLOCK, 1));
		String tReport = tTable.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.9F, 0.5F);
		assertTrue(tReport.startsWith("activated"), tReport);
		assertTrue(tTable.isActive(), "the :281 B[2] seat");

		// the watch gate (:238) — nobody watching = the activation drops, zero progress
		tTable.tickWork(List.of());
		assertFalse(tTable.isActive(), "the :238 empty-watcher arm clears B[2]");
		assertEquals(0, tTable.mClickCount);
		assertEquals(1, tTable.inventory().getStackInSlot(0).getCount(), "the input stays put");
	}

	// ---------------------------------------------------------------------------
	// chain ③: the watch-execute arm (:241-263)
	// ---------------------------------------------------------------------------

	@Test
	public void fourWatchedArmsSiftTheRow() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.GRASS_BLOCK, 1));
		Player tPlayer = bagPlayer();

		// three watched arms = the progress ladder (:245, the haste-fold identity)
		tTable.tickWork(List.of(tPlayer));
		tTable.tickWork(List.of(tPlayer));
		tTable.tickWork(List.of(tPlayer));
		assertEquals(3, tTable.mClickCount);
		assertEquals(1, tTable.inventory().getStackInSlot(0).getCount(), "the input pays only on completion");

		// the fourth arm completes: the input pays, the outputs land positionally (:255)
		tTable.tickWork(List.of(tPlayer));
		assertEquals(0, tTable.mClickCount, "the :246 reset");
		assertTrue(tTable.inventory().getStackInSlot(0).isEmpty(), "the :253 slotKill");
		assertEquals(2, tTable.inventory().getStackInSlot(1).getCount(), "the first output → slot 1");
		assertEquals(Items.WHEAT_SEEDS, tTable.inventory().getStackInSlot(1).getItem());
		assertEquals(Items.DIRT, tTable.inventory().getStackInSlot(2).getItem(), "the second output → slot 2");
		// :256 — the exhaustion value = totalPower / 1000 (the live charge is the vanilla gate)
		assertEquals(0.256F, GT6SiftingTableBlockEntity.exhaustionOf(16 * 16), 1e-6F);
	}

	@Test
	public void creativeWatcherSiftsOnTheFirstArm() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.GRASS_BLOCK, 1));
		Player tPlayer = bagPlayer();
		tPlayer.getAbilities().instabuild = true; // hasInfiniteItems (:245)
		tTable.tickWork(List.of(tPlayer));
		assertEquals(0, tTable.mClickCount);
		assertTrue(tTable.inventory().getStackInSlot(0).isEmpty());
		assertEquals(Items.WHEAT_SEEDS, tTable.inventory().getStackInSlot(1).getItem());
	}

	@Test
	public void occupiedOutputsBlockTheArmAndKeepTheProgress() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.GRASS_BLOCK, 1));
		tTable.inventory().setStackInSlot(7, new ItemStack(Items.DIRT, 1)); // the :242 temp gate trips
		Player tPlayer = bagPlayer();
		tTable.tickWork(List.of(tPlayer));
		tTable.tickWork(List.of(tPlayer));
		assertEquals(0, tTable.mClickCount, "the progress counts only while the outputs are clear (:242)");
		assertEquals(1, tTable.inventory().getStackInSlot(0).getCount());
	}

	@Test
	public void rowMissMovesTheInputToTheFirstOutputSlot() {
		// :247-249 — a material with NO row: the input relocates to the first free output slot
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
		Player tPlayer = bagPlayer();
		for (int i = 0; i < 4; i++) tTable.tickWork(List.of(tPlayer));
		assertTrue(tTable.inventory().getStackInSlot(0).isEmpty(), "the :249 slotKill(0)");
		assertEquals(Items.DIAMOND, tTable.inventory().getStackInSlot(1).getItem(), "the first free output slot");
	}

	@Test
	public void twelveOutputBoundaryTruncatesTheSurplus() {
		// 14 outputs → :255 j = min(14, 12): exactly the first 12 land, slots stay 1..12
		ItemStack[] tOutputs = new ItemStack[14];
		for (int i = 0; i < 14; i++) tOutputs[i] = new ItemStack(i % 2 == 0 ? Items.IRON_NUGGET : Items.GOLD_NUGGET, 1);
		GT6RecipeMaps.SIFTING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.SAND, 1)},
				tOutputs, new FluidStack[0], new FluidStack[0], 16, 16, 0));
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.SAND, 1));
		Player tPlayer = bagPlayer();
		for (int i = 0; i < 4; i++) tTable.tickWork(List.of(tPlayer));
		int tFilled = 0;
		for (int i = 1; i < GT6SiftingTableBlockEntity.SLOTS; i++) if (!tTable.inventory().getStackInSlot(i).isEmpty()) tFilled++;
		assertEquals(12, tFilled, "the :255 min(outputs, 12) boundary");
	}

	// ---------------------------------------------------------------------------
	// chain ④: the non-top collect arm (:286-288)
	// ---------------------------------------------------------------------------

	@Test
	public void sideClickCollectsTheOutputsIntoTheBag() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(1, new ItemStack(Items.WHEAT_SEEDS, 1));
		tTable.inventory().setStackInSlot(2, new ItemStack(Items.DIRT, 1));
		Player tPlayer = bagPlayer();
		String tReport = tTable.activateChain(tPlayer, (byte) 2, ItemStack.EMPTY, 0.5F, 0.5F, 0.0F);
		assertTrue(tReport.startsWith("collected"), tReport);
		assertTrue(tTable.inventory().getStackInSlot(1).isEmpty(), "the :287 slotTake");
		assertTrue(tTable.inventory().getStackInSlot(2).isEmpty());
		assertEquals(1, countItem(tPlayer, Items.WHEAT_SEEDS));
		assertEquals(1, countItem(tPlayer, Items.DIRT));
	}

	@Test
	public void sideClickWithNothingToCollectReports() {
		GT6SiftingTableBlockEntity tTable = table();
		String tReport = tTable.activateChain(null, (byte) 3, ItemStack.EMPTY, 0.5F, 0.5F, 0.0F);
		assertEquals("nothing to collect", tReport);
	}

	// ---------------------------------------------------------------------------
	// the capability posture (:445-453)
	// ---------------------------------------------------------------------------

	@Test
	public void sideFaceInsertsRowInputsOnlyIntoSlotZero() {
		GT6SiftingTableBlockEntity tTable = table();
		var tHandler = tTable.newSideHandler();
		assertEquals(GT6SiftingTableBlockEntity.SLOTS, tHandler.getSlots(), "the :445 ACCESSIBLE_SLOTS all 13");

		// a row input rides slot 0 (the forge handler is NON-MUTATING — the remainder is
		// the return value, the ItemStackHandler convention)
		ItemStack tGrass = new ItemStack(Items.GRASS_BLOCK, 4);
		ItemStack tRemainder = tHandler.insertItem(0, tGrass, false);
		assertEquals(1, tTable.inventory().getStackInSlot(0).getCount(), "the slot-0 stack limit 1");
		assertEquals(3, tRemainder.getCount(), "the surplus returns to the caller");

		// a non-row material is refused everywhere (:452 containsInput)
		ItemStack tDiamond = new ItemStack(Items.DIAMOND, 1);
		assertEquals(tDiamond, tHandler.insertItem(0, tDiamond, false), "the containsInput reject");

		// extraction reads the outputs only (:453 canExtractItem2 aSlot != 0)
		tTable.inventory().setStackInSlot(3, new ItemStack(Items.DIRT, 1));
		assertEquals(Items.DIRT, tHandler.extractItem(3, 1, false).getItem());
		assertTrue(tHandler.extractItem(0, 1, false).isEmpty(), "the input slot never extracts");
		// the output slot never inserts (:452 aSlot == 0) — ItemStack has NO instance
		// equals in 1.20.1 (the static matches, vanilla ItemStack.java:416 — the refused
		// offer comes back as a distinct instance, identity asserts would pin nothing)
		assertTrue(ItemStack.matches(new ItemStack(Items.GRASS_BLOCK, 1),
						tHandler.insertItem(2, new ItemStack(Items.GRASS_BLOCK, 1), false)),
				"the output slot is insert-blind");
	}

	// ---------------------------------------------------------------------------
	// NBT round trip (:61-73)
	// ---------------------------------------------------------------------------

	@Test
	public void nbtRoundTripPreservesStateProgressAndInventory() {
		GT6SiftingTableBlockEntity tTable = table();
		tTable.inventory().setStackInSlot(0, new ItemStack(Items.GRASS_BLOCK, 1));
		tTable.mState = (byte) (GT6SiftingTableBlockEntity.HAS_INPUT | GT6SiftingTableBlockEntity.ACTIVE);
		tTable.mClickCount = 3;
		// saveWithoutMetadata — the fixture BET is not registry-mapped offline (the
		// saveWithFullMetadata id write needs the live registration)
		net.minecraft.nbt.CompoundTag tTag = tTable.saveWithoutMetadata();
		GT6SiftingTableBlockEntity tLoaded = new GT6SiftingTableBlockEntity(sTableType, POS, Blocks.STONE.defaultBlockState());
		tLoaded.load(tTag);
		assertEquals(Items.GRASS_BLOCK, tLoaded.inventory().getStackInSlot(0).getItem());
		assertEquals(GT6SiftingTableBlockEntity.HAS_INPUT | GT6SiftingTableBlockEntity.ACTIVE, tLoaded.mState, "the :63 NBT_STATE");
		assertEquals(3, tLoaded.mClickCount, "the :64 NBT_PROGRESS");
	}

	// ---------------------------------------------------------------------------
	// fixtures
	// ---------------------------------------------------------------------------

	/**
	 * The offline watcher (the GT6AnvilBlockEntityTest BagPlayer form — the Unsafe
	 * allocation + real Inventory/Abilities). invulnerable stays TRUE: the
	 * causeFoodExhaustion gate (Player.java:1689-1693) reads level() second, and the
	 * offline player has none — the charge face is the field_test arm.
	 */
	private static Player bagPlayer() {
		return SiftingPlayer.withInventory(new ItemStack[] {new ItemStack(Items.IRON_NUGGET, 1)});
	}

	private static int countItem(Player aPlayer, Item aItem) {
		int rCount = 0;
		for (int i = 0; i < aPlayer.getInventory().items.size(); i++) {
			ItemStack tStack = aPlayer.getInventory().items.get(i);
			if (tStack.getItem() == aItem) rCount += tStack.getCount();
		}
		return rCount;
	}

	/** The offline player — the anvil BagPlayer form, the abilities field real so the watcher seam can flip instabuild. */
	public static final class SiftingPlayer extends Player {
		private SiftingPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

		public static SiftingPlayer withInventory(ItemStack[] aSlots) {
			try {
				java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tTheUnsafe.setAccessible(true);
				sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
				SiftingPlayer tPlayer = (SiftingPlayer) tUnsafe.allocateInstance(SiftingPlayer.class);
				java.lang.reflect.Field tInventoryField = Player.class.getDeclaredField("inventory");
				tInventoryField.setAccessible(true);
				Inventory tInventory = new Inventory(null);
				for (int i = 0, n = Math.min(aSlots.length, tInventory.items.size()); i < n; i++) {
					if (aSlots[i] != null) tInventory.items.set(i, aSlots[i]);
				}
				tInventoryField.set(tPlayer, tInventory);
				java.lang.reflect.Field tFoodField = Player.class.getDeclaredField("foodData");
				tFoodField.setAccessible(true);
				tFoodField.set(tPlayer, new FoodData());
				java.lang.reflect.Field tAbilitiesField = Player.class.getDeclaredField("abilities");
				tAbilitiesField.setAccessible(true);
				net.minecraft.world.entity.player.Abilities tAbilities = new net.minecraft.world.entity.player.Abilities();
				tAbilities.invulnerable = true; // the null-level shield (the anvil fixture finding, S5 gate rerun)
				tAbilitiesField.set(tPlayer, tAbilities);
				return tPlayer;
			} catch (ReflectiveOperationException aE) {
				throw new IllegalStateException("the offline sifting player failed", aE);
			}
		}

		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
	}
}
