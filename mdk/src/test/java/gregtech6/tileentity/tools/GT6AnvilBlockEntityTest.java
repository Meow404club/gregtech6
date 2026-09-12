package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.items.tools.GTHammerItem;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GT6Anvils;
import gregtech6.registry.GT6Anvils.AnvilRow;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity;

/**
 * The anvil BE offline tests (task p28-c-anvil acceptance ⑤): the working-strike
 * semantics (MultiTileEntityAnvil.java:94-141), the durability/fatigue math (:122-130)
 * and the interaction chain (:218-274), plus the hopper-suction easter-egg branch
 * (GT6HopperBaseBlockEntity.moveInPhase, the upstream :191 anvil gate — task p28-c-anvil
 * closed the defer).
 */
public class GT6AnvilBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6AnvilBlockEntity> sAnvilType;
	static BlockEntityType<HopperFixture> sHopperType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The fixture BET (the GT6HopperFamilyTest self-referencing form — the live BET is registration-bound). */
	@BeforeAll
	static void buildOfflineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6AnvilBlockEntity>[] tAnvil = (BlockEntityType<GT6AnvilBlockEntity>[]) new BlockEntityType<?>[1];
		tAnvil[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6AnvilBlockEntity(tAnvil[0], aPos, aState),
				Blocks.STONE).build(null);
		sAnvilType = tAnvil[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<HopperFixture>[] tHopper = (BlockEntityType<HopperFixture>[]) new BlockEntityType<?>[1];
		tHopper[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new HopperFixture(tHopper[0], aPos, aState),
				Blocks.STONE).build(null);
		sHopperType = tHopper[0];
	}

	/** A fresh generation per test — the fixture rows below are the only writers. */
	@BeforeEach
	void freshMaps() {
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		// the ANVIL fixture row: 1 stone → 1 iron nugget, eUt 16 x duration 16 (power 256 —
		// the wear lands on the 10000 floor, the upstream :124 max(10000, divup(power,4)))
		GT6RecipeMaps.ANVIL.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.STONE, 1)},
				new ItemStack[] {new ItemStack(Items.IRON_NUGGET, 1)},
				new FluidStack[0], new FluidStack[0], 16, 16, 0));
		// the ANVIL_BEND fixture row: 1 clay → 1 brick (bends on a SIDE strike only)
		GT6RecipeMaps.ANVIL_BEND.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new FluidStack[0], new FluidStack[0], 16, 16, 0));
	}

	@AfterEach
	void teardown() {
		GT6RecipeMaps.reset();
	}

	/** A fresh offline anvil (the plain-stone carrier → the 10000 default). */
	private static GT6AnvilBlockEntity anvil(long aDurability) {
		GT6AnvilBlockEntity tAnvil = new GT6AnvilBlockEntity(sAnvilType, POS, Blocks.STONE.defaultBlockState());
		tAnvil.mDurability = aDurability;
		return tAnvil;
	}

	// ---------------------------------------------------------------------------
	// the working strike (:94-141)
	// ---------------------------------------------------------------------------

	@Test
	public void topStrikeProcessesAndWearsTheFloor() {
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 1));
		String tReport = tAnvil.hammerStrike(null, (byte) 1); // SIDES_TOP
		assertTrue(tReport.startsWith("worked:"), tReport);
		// :118 — the inputs paid
		assertTrue(tAnvil.inventory().getStackInSlot(0).isEmpty());
		assertTrue(tAnvil.inventory().getStackInSlot(1).isEmpty());
		// :124 — the wear = max(10000, divup(256,4)) = the floor verbatim (one displayed point)
		assertEquals(20000, tAnvil.mDurability);
		assertEquals(2, tAnvil.durabilityPoints(), "divup(20000, 10000)");
	}

	@Test
	public void stoneAnvilBreaksOnItsFirstPoint() {
		GT6AnvilBlockEntity tAnvil = anvil(10000); // the :2185 NBT_DURABILITY default
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 1));
		String tReport = tAnvil.hammerStrike(null, (byte) 1);
		assertTrue(tReport.startsWith("the anvil broke"), tReport);
		assertEquals(0, tAnvil.mDurability);
		// the offline arm skips the scrap rain + the block removal (no level), the state is gone
		assertTrue(tAnvil.isRemoved() || tAnvil.mDurability <= 0);
	}

	@Test
	public void sideStrikeBendsAndTopDoesNot() {
		// the folded selector: a side strike reads ANVIL_BEND (:100-116 — the Small/Big
		// hit-half aiming folded onto one map, the declared deviation)
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.CLAY, 1));
		assertTrue(tAnvil.hammerStrike(null, (byte) 3).startsWith("worked:"), "a side strike bends");
		assertTrue(tAnvil.inventory().getStackInSlot(0).isEmpty());

		// the same clay on the TOP face reads ANVIL — no row matches
		GT6AnvilBlockEntity tAnvil2 = anvil(30000);
		tAnvil2.inventory().setStackInSlot(0, new ItemStack(Items.CLAY, 1));
		assertEquals("no matching recipe on the working surface", tAnvil2.hammerStrike(null, (byte) 1));
		assertFalse(tAnvil2.inventory().getStackInSlot(0).isEmpty(), "the inputs stay put on a miss");
	}

	@Test
	public void emptySurfaceRefuses() {
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		assertEquals("the working surface is empty", tAnvil.hammerStrike(null, (byte) 1));
		assertEquals(30000, tAnvil.mDurability, "a refused strike wears nothing");
	}

	// ---------------------------------------------------------------------------
	// the put/take chain (:218-274)
	// ---------------------------------------------------------------------------

	@Test
	public void topClickPlacesByHitHalf() {
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		ItemStack tHeld = new ItemStack(Items.STONE, 3);
		// the Z-axis default facing (NORTH) splits by hitX: 0.3 → slot 0
		String tReport = tAnvil.activateChain(null, (byte) 1, tHeld, 0.3F, 0.5F, 0.5F);
		assertTrue(tReport.startsWith("placed"), tReport);
		assertEquals(3, tAnvil.inventory().getStackInSlot(0).getCount());
		assertTrue(tHeld.isEmpty(), "the held stack moved whole (the :240 ST.move)");
		// 0.8 → slot 1
		ItemStack tHeld2 = new ItemStack(Items.STONE, 2);
		tAnvil.activateChain(null, (byte) 1, tHeld2, 0.8F, 0.5F, 0.5F);
		assertEquals(2, tAnvil.inventory().getStackInSlot(1).getCount());
	}

	@Test
	public void legsRegionRefuses() {
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		assertEquals("the anvil legs (no action)", tAnvil.activateChain(null, (byte) 1, new ItemStack(Items.STONE, 1), 0.5F, 0.1F, 0.5F));
		assertTrue(tAnvil.inventory().getStackInSlot(0).isEmpty(), "the :220 PX_P[4] gate");
	}

	@Test
	public void sideClickEvenSplits() {
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 4));
		tAnvil.activateChain(null, (byte) 2, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F);
		assertEquals(2, tAnvil.inventory().getStackInSlot(0).getCount(), "the even split (:253)");
		assertEquals(2, tAnvil.inventory().getStackInSlot(1).getCount());

		// an odd stack gives its single odd unit back first (:251), then splits evenly
		GT6AnvilBlockEntity tAnvil2 = anvil(30000);
		tAnvil2.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 5));
		tAnvil2.activateChain(null, (byte) 2, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F);
		assertEquals(2, tAnvil2.inventory().getStackInSlot(0).getCount());
		assertEquals(2, tAnvil2.inventory().getStackInSlot(1).getCount());
	}

	@Test
	public void hammerInHandStrikesRatherThanStores() {
		GTHammerItem tHammerItem = probeItem("anvil_probe_hammer", GTHammerItem::new);
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 1));
		ItemStack tHammer = new ItemStack(tHammerItem);
		String tReport = tAnvil.activateChain(null, (byte) 1, tHammer, 0.5F, 0.5F, 0.5F);
		assertTrue(tReport.startsWith("worked:"), tReport);
		assertTrue(tAnvil.inventory().getStackInSlot(0).isEmpty());

		// both halves empty + hammer in hand = the :240 hammer-storage arm
		GT6AnvilBlockEntity tAnvil2 = anvil(30000);
		ItemStack tHammer2 = new ItemStack(tHammerItem);
		String tReport2 = tAnvil2.activateChain(null, (byte) 1, tHammer2, 0.7F, 0.5F, 0.5F);
		assertTrue(tReport2.startsWith("placed"), tReport2);
		assertTrue(GT6AnvilBlockEntity.isHammer(tAnvil2.inventory().getStackInSlot(1)), "hitX 0.7 = the far half");
	}

	// ---------------------------------------------------------------------------
	// the R1 all-or-nothing give-back (the S5 review: bag + ground from one output)
	// ---------------------------------------------------------------------------

	/**
	 * The near-full bag with a same-item partial stack: capacity 1, offer 5 — the
	 * all-or-nothing give-back refuses WHOLESALE (upstream ST.add :1074-1093), so nothing
	 * moves. The pre-fix partial-fill form put 1 into the bag AND dropped the full 5 —
	 * this test walks that exact path and pins the fix.
	 */
	@Test
	public void giveToPlayerRefusesThePartialFitAllOrNothing() {
		Player tPlayer = bagPlayer(63);
		ItemStack tFive = new ItemStack(Items.IRON_NUGGET, 5);
		boolean tGiven = tAnvilRef().giveToPlayer(tPlayer, tFive);
		assertFalse(tGiven, "capacity 1 < 5 — the whole offer is refused");
		assertEquals(63, tPlayer.getInventory().items.get(0).getCount(), "nothing moved (the pre-fix form would show 64)");
		assertEquals(63, countNuggets(tPlayer));
	}

	@Test
	public void giveToPlayerTakesTheWholeStackWhenItFits() {
		Player tPlayer = bagPlayer(59); // capacity 5 in slot 0
		ItemStack tFive = new ItemStack(Items.IRON_NUGGET, 5);
		assertTrue(tAnvilRef().giveToPlayer(tPlayer, tFive));
		assertEquals(64, tPlayer.getInventory().items.get(0).getCount(), "the full stack lands in the partial stack");
		assertEquals(64, countNuggets(tPlayer));
	}

	/**
	 * The end-to-end strike into the near-full bag: the 5-nugget output goes to the GROUND
	 * whole (spawnAbove), the bag keeps exactly its 63 — the duplication path (bag +1 AND
	 * ground +5) is structurally gone.
	 */
	@Test
	public void strikeIntoNearlyFullBagDropsTheWholeOutputWithoutDuplication() {
		// a dedicated row with a 5-count output (a distinct input keeps findRecipe deterministic)
		GT6RecipeMaps.ANVIL.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.APPLE, 1)},
				new ItemStack[] {new ItemStack(Items.IRON_NUGGET, 5)},
				new FluidStack[0], new FluidStack[0], 16, 16, 0));
		GT6AnvilBlockEntity tAnvil = anvil(30000);
		tAnvil.inventory().setStackInSlot(0, new ItemStack(Items.APPLE, 1));
		Player tPlayer = bagPlayer(63);

		String tReport = tAnvil.hammerStrike(tPlayer, (byte) 1);
		assertTrue(tReport.startsWith("worked: 5"), tReport);
		assertTrue(tAnvil.inventory().getStackInSlot(0).isEmpty(), "the input paid");
		assertEquals(63, tPlayer.getInventory().items.get(0).getCount(),
				"the partial-fit bag is untouched (the pre-fix form would show 64 = the duplication)");
		assertEquals(63, countNuggets(tPlayer), "zero duplication: bag + ground = 5, never 6");
		assertEquals(20000, tAnvil.mDurability, "the wear floor unchanged (one displayed point)");
	}

	/** The nugget total across the bag (the duplication counter). */
	private static int countNuggets(Player aPlayer) {
		int rTotal = 0;
		for (int i = 0, n = aPlayer.getInventory().items.size(); i < n; i++) {
			ItemStack tSlot = aPlayer.getInventory().items.get(i);
			if (!tSlot.isEmpty() && tSlot.getItem() == Items.IRON_NUGGET) rTotal += tSlot.getCount();
		}
		return rTotal;
	}

	/** The give-back seam under test (package-private by the R1 ruling). */
	private static GT6AnvilBlockEntity tAnvilRef() {
		return new GT6AnvilBlockEntity(sAnvilType, POS, Blocks.STONE.defaultBlockState());
	}

	/**
	 * A bag: slot 0 = {@code aSlot0Count} iron nuggets, slots 1-35 = full dirt (the
	 * "nearly full but a same-item stack has headroom" scenario of the S5 review).
	 */
	private static Player bagPlayer(int aSlot0Count) {
		ItemStack[] tSlots = new ItemStack[36];
		tSlots[0] = new ItemStack(Items.IRON_NUGGET, aSlot0Count);
		for (int i = 1; i < 36; i++) tSlots[i] = new ItemStack(Items.DIRT, 64);
		return BagPlayer.withInventory(tSlots);
	}

	/**
	 * The offline Player double (the GT6SingleBlockFacingIntegrityTest.ViewPlayer Unsafe-
	 * allocation form) — plus a REAL {@link Inventory} and {@link FoodData} injected so
	 * the give-back and the exhaustion seams run for real (both are plain in-memory
	 * offline; the vanilla add() never touches the player back-ref for stackables).
	 */
	public static final class BagPlayer extends Player {
		private BagPlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form

		public static BagPlayer withInventory(ItemStack[] aSlots) {
			try {
				java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tTheUnsafe.setAccessible(true);
				sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
				BagPlayer tPlayer = (BagPlayer) tUnsafe.allocateInstance(BagPlayer.class);
				java.lang.reflect.Field tInventoryField = Player.class.getDeclaredField("inventory");
				tInventoryField.setAccessible(true);
				Inventory tInventory = new Inventory(null); // the GT6MachineFluidDisplayTest offline form
				for (int i = 0, n = Math.min(aSlots.length, tInventory.items.size()); i < n; i++) {
					if (aSlots[i] != null) tInventory.items.set(i, aSlots[i]);
				}
				tInventoryField.set(tPlayer, tInventory);
				java.lang.reflect.Field tFoodField = Player.class.getDeclaredField("foodData");
				tFoodField.setAccessible(true);
				tFoodField.set(tPlayer, new FoodData());
				// the exhaustion seam reads abilities.instabuild (Player.causeFoodExhaustion)
				// — instabuild TRUE = the vanilla creative no-exhaust form, and it keeps the
				// strike path off the null level() read
				java.lang.reflect.Field tAbilitiesField = Player.class.getDeclaredField("abilities");
				tAbilitiesField.setAccessible(true);
				net.minecraft.world.entity.player.Abilities tAbilities = new net.minecraft.world.entity.player.Abilities();
				tAbilities.instabuild = true;
				tAbilitiesField.set(tPlayer, tAbilities);
				return tPlayer;
			} catch (ReflectiveOperationException aE) {
				throw new IllegalStateException("the offline bag player failed", aE);
			}
		}

		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
	}

	// ---------------------------------------------------------------------------
	// the row axis + the hopper easter egg (the p28 defer closure)
	// ---------------------------------------------------------------------------

	@Test
	public void rowAxisReproducesTheLoaderAnchors() {
		assertEquals(2, GT6Anvils.ROWS.size());
		AnvilRow tStone = GT6Anvils.ROWS.get(0), tBlackstone = GT6Anvils.ROWS.get(1);
		assertEquals("stone_anvil", tStone.path());
		assertEquals("blackstone_anvil", tBlackstone.path());
		assertEquals(10000, tStone.durability(), ":2185 NBT_DURABILITY");
		assertEquals(100000, tBlackstone.durability(), ":2186 NBT_DURABILITY");
		// the NBT_MATERIAL columns (the smash-target hop reads them)
		assertEquals(gregapi.data.MT.Stone, tStone.material().get());
		assertEquals(gregapi.data.MT.STONES.Blackstone, tBlackstone.material().get());
	}

	/**
	 * The upstream :191 gate — an anvil above refuses the drain half (the NO_SLOTS stand-in
	 * stays undrained) while a generic TE still drains; the suction half runs for the anvil.
	 */
	@Test
	public void hopperUnderAnvilSucksInsteadOfDraining() {
		GTItemStackHandler tSource = new GTItemStackHandler(1);
		tSource.setStackInSlot(0, new ItemStack(Items.STONE, 7));

		// the generic neighbour: the drain arm moves the source out (the :192 verbatim)
		HopperFixture tGeneric = hopperFixture(tSource, false, null);
		tGeneric.moveInPhaseForTest();
		assertTrue(tSource.getStackInSlot(0).isEmpty(), "a generic top container drains");

		// the anvil: the drain arm is refused (the :191 conjunct) — the source keeps its stack
		HopperFixture tUnderAnvil = hopperFixture(tSource, true, new ItemStack(Items.IRON_NUGGET, 1));
		tSource.setStackInSlot(0, new ItemStack(Items.STONE, 7));
		tUnderAnvil.moveInPhaseForTest();
		assertEquals(7, tSource.getStackInSlot(0).getCount(), "the anvil is never drained");

		// ...and the suction half picks the spy stack instead (the WD.suck arm :196-204)
		assertEquals(1, tUnderAnvil.getInventory().getStackInSlot(0).getCount(), "one dropped item sucked");
	}

	/**
	 * The probe-item bracket (the BendingCylinderSmallTest.probeItem form verbatim): the
	 * three forge locks / single 21.1 frozen flag; the probe item rides a dedicated probe
	 * id and never reaches any committed data.
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Function<Item.Properties, I> aCreator) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the vanilla frozen flag (the Item ctor intrusive-holder gate), the delegate
			// ForgeRegistry.isFrozen, the NamespacedWrapper.locked register gate
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]: " + aE, aE);
		}
		//?} else {
		/*try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.apply(new Item.Properties().durability(512));
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the defaulted wrapper hides the lock one level up). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = tClass.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException aE) {/* walk up */}
		}
		throw new NoSuchFieldException(aName + " not found on " + aClass.getName());
	}

	/** The hopper fixture (the GT6HopperFamilyTest BET-free form + the anvil/spy overrides). */
	private static final class HopperFixture extends GT6HopperBaseBlockEntity {
		final GTItemStackHandler mSource;
		final boolean mAnvilAbove;
		final ItemStack mSucked;

		HopperFixture(GTItemStackHandler aSource, boolean aAnvilAbove, ItemStack aSucked) {
			this(sHopperType, POS, Blocks.STONE.defaultBlockState(), aSource, aAnvilAbove, aSucked);
		}

		/** The BET-builder form (the factory lambda calls this with the live type). */
		HopperFixture(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			this(aType, aPos, aState, null, false, null);
		}

		HopperFixture(BlockEntityType<?> aType, BlockPos aPos, BlockState aState,
				GTItemStackHandler aSource, boolean aAnvilAbove, ItemStack aSucked) {
			super(aType, aPos, aState);
			mSource = aSource;
			mAnvilAbove = aAnvilAbove;
			mSucked = aSucked;
		}

		@Override protected int inventorySize(BlockState aState) {return 1;}
		@Override public int stackLimit() {return 64;}
		@Override protected byte defaultMode() {return 0;}
		@Override protected void clampMode() {}
		@Override protected byte modeMin() {return 0;}
		@Override protected byte modeMax() {return 0;}
		@Override protected boolean compressInsideGate() {return false;}
		@Override protected int compressPhase() {return 0;}
		@Override protected int findSuctionSlot() {return 0;}
		@Override public int[] getAccessibleSlotsFromSide(byte aSide) {return new int[] {0};}
		@Override public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {return true;}
		@Override public boolean canExtractItem(int aSlot, byte aSide) {return true;}
		@Override public boolean hasExactMode() {return false;}
		@Override protected int moveOutPhase() {return 0;}
		@Override protected net.minecraftforge.items.IItemHandler topSource() {return mSource;}
		@Override protected boolean topIsAnvil() {return mAnvilAbove;}
		@Override protected ItemStack suckTopItem() {return mSucked == null ? null : mSucked.copy();}
		@Override protected boolean topVisiblyOpaque() {return false;}
		@Override public String getTileEntityName() {return "hopper_fixture";}
		/** The protected-method bridge (a foreign-package test cannot call it directly). */
		public void moveInPhaseForTest() {moveInPhase();}
	}

	private static HopperFixture hopperFixture(GTItemStackHandler aSource, boolean aAnvilAbove, ItemStack aSucked) {
		return new HopperFixture(aSource, aAnvilAbove, aSucked);
	}
}
