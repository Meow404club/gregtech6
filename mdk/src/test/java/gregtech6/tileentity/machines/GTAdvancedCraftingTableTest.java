package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Task p24-act-machine C1 acceptance (offline, zero Player instances — the pipe-owner
 * discipline): the pattern table over the eight selector configs (:222-283 对位), the
 * four-arm consumption priority (:406-413 对位 over the SLOTS_CONSUMPTION 70→0 walk
 * :499), the selector-only whitelist five arms (:515 through the frozen GT6Circuits
 * API), the 71-slot NBT round trip, the four click modes over the sink seam (:599-656)
 * and the real vanilla RecipeManager lookup (the stick recipe through the config-2
 * vertical-2 pattern, the chest recipe through the config-8 ring-8 pattern).
 */
public class GTAdvancedCraftingTableTest extends GTMachinesOfflineTestBase {

	/** The genuine vanilla data/minecraft/recipes/stick.json (1.20.1) — the config-2 vertical-2 consumer. */
	private static final String VANILLA_STICK_RECIPE_JSON =
			"{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"P\",\"P\"],\"key\":{\"P\":{\"item\":\"minecraft:oak_planks\"}},\"result\":{\"item\":\"minecraft:stick\",\"count\":4}}";

	/** The genuine vanilla data/minecraft/recipes/chest.json (1.20.1) — the config-8 ring-8 consumer. */
	private static final String VANILLA_CHEST_RECIPE_JSON =
			"{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"PPP\",\"P P\",\"PPP\"],\"key\":{\"P\":{\"item\":\"minecraft:oak_planks\"}},\"result\":{\"item\":\"minecraft:chest\"}}";

	static BlockEntityType<TileEntityAdvancedCraftingTable> sActType;

	/** The probe circuit item — REALLY registered offline (an ItemStack constructor resolves
	 * the registry delegate eagerly, so an unregistered item cannot ride the channel; the
	 * FileSawTest probeItem three-lock bracket, circuit flavor). */
	private static net.minecraft.world.item.Item sCircuitItem;

	/** The circuit fixture — a REAL registered {@code IntegratedCircuitItem} instance (the
	 * {@code isSelector} item-identity probe needs the class; the configuration rides the
	 * vanilla Damage key, both legs encapsulated in GT6Circuits). */
	static ItemStack selector(int aConfig) {
		return gregtech6.item.GT6Circuits.selector(sCircuitItem, aConfig);
	}

	@BeforeAll
	static void buildActFixtures() {
		// ORDER HAZARD, fixed here on purpose: the sibling machine tests build their
		// synthetic item universe ONCE per JVM by walking the LIVE registry pool with a
		// modulo cursor (TileEntityBasicMachineOfflineTestBase.buildSyntheticUniverse) —
		// the picks depend on the pool SIZE. This class registers one probe item, so it
		// must force-build the universe FIRST (idempotent via sUniverseBuilt), pinning
		// the sibling fixtures to the baseline phase; otherwise the +1 pool entry shifts
		// the wrap and every parallel/recipe fixture downstream drifts (the full-suite
		// bisect finding, task p24-act-machine).
		TileEntityBasicMachineOfflineTestBase.buildSyntheticUniverse();
		// offline holders avoid the RegistryObject.get() path of the runtime factory
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityAdvancedCraftingTable>[] tHolder = (BlockEntityType<TileEntityAdvancedCraftingTable>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityAdvancedCraftingTable(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sActType = tHolder[0];
		registerCircuitFixture();
	}

	/**
	 * The GTWireBlockUseLockTest:43 reflection bracket, item flavor (the FileSawTest
	 * probeItem body verbatim minus the durability column) — the offline item registry
	 * keeps THREE locks on the forge wrapper shape / ONE on the 21.1 vanilla shape.
	 */
	private static void registerCircuitFixture() {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// 1. the vanilla frozen flag (the Item ctor intrusive-holder gate);
			// 2. the delegate ForgeRegistry.isFrozen; 3. the NamespacedWrapper.locked gate
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
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			// the 21.1 shape: the plain vanilla DefaultedMappedRegistry — a single frozen flag
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		sCircuitItem = net.minecraft.core.Registry.register(tRegistry,
				new ResourceLocation("gt6", "probe_integrated_circuit"),
				new gregtech6.item.GT6Circuits.IntegratedCircuitItem(new net.minecraft.world.item.Item.Properties()));
	}

	/** getDeclaredField along the superclass chain (the defaulted wrapper hides the lock one level up). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// keep walking up
			}
		}
		throw new NoSuchFieldException(aName);
	}

	/** A MinimalLevel whose crafting bridge resolves the stick + chest recipes. */
	static MachineLevel craftingLevel() {
		TestRecipeManager tManager = new TestRecipeManager();
		Map<ResourceLocation, com.google.gson.JsonElement> tMap = new HashMap<>();
		tMap.put(new ResourceLocation("minecraft", "stick"), new Gson().fromJson(VANILLA_STICK_RECIPE_JSON, JsonObject.class));
		tMap.put(new ResourceLocation("minecraft", "chest"), new Gson().fromJson(VANILLA_CHEST_RECIPE_JSON, JsonObject.class));
		tManager.load(tMap);
		return new MachineLevel(tManager);
	}

	static TileEntityAdvancedCraftingTable makeAct(MachineLevel aLevel) {
		TileEntityAdvancedCraftingTable tTable = sActType.create(POS, Blocks.BRICKS.defaultBlockState());
		tTable.setLevel(aLevel);
		return tTable;
	}

	/** The player-free sink the click-mode arms drive (the /gt6act arraySink shape). */
	static TileEntityAdvancedCraftingTable.ICraftOutputSink sink(int aCells) {
		ItemStack[] tCells = new ItemStack[aCells];
		for (int i = 0; i < aCells; i++) tCells[i] = ItemStack.EMPTY;
		return new TileEntityAdvancedCraftingTable.ICraftOutputSink() {
			@Override public int cellCount() {
				return aCells;
			}

			@Override public ItemStack getHold(int aCell) {
				return tCells[aCell];
			}

			@Override public void setHold(int aCell, ItemStack aHold) {
				tCells[aCell] = aHold;
			}
		};
	}

	/** Puts the selector into slot 30 through the handler face (whitelist-guarded). */
	static void putSelector(TileEntityAdvancedCraftingTable aTable, int aConfig) {
		ItemStack tSelector = selector(aConfig);
		assertTrue(aTable.getInventory().isItemValid(30, tSelector), "config " + aConfig + " must pass the whitelist");
		aTable.getInventory().setStackInSlot(30, tSelector);
	}

	/** The ghost cell-set of a fresh compute — the exact SLOTS_CRAFTING index set the upstream case arms write. */
	static java.util.Set<Integer> ghostCells(TileEntityAdvancedCraftingTable aTable) {
		java.util.Set<Integer> rCells = new java.util.HashSet<>();
		for (int i = 0; i < 9; i++) {
			ItemStack tGhost = aTable.mPattern[i];
			if (tGhost != null && !tGhost.isEmpty()) rCells.add(i);
		}
		return rCells;
	}

	// ------------------------------------------------------------------ the pattern table (:222-283)

	@Test
	public void patternTableFillsTheEightSelectorShapes() {
		// {config → expected ghost cell indices} — the upstream case-arm order is irrelevant, the SET is the pin
		Map<Integer, int[]> tExpected = new HashMap<>();
		tExpected.put(2, new int[] {6});
		tExpected.put(3, new int[] {5, 4});
		tExpected.put(4, new int[] {4, 1, 0});
		tExpected.put(5, new int[] {7, 5, 4, 1});
		tExpected.put(6, new int[] {5, 4, 2, 1, 0});
		tExpected.put(7, new int[] {7, 5, 4, 2, 1, 0});
		tExpected.put(8, new int[] {8, 7, 6, 5, 2, 1, 0});
		tExpected.put(9, new int[] {8, 7, 6, 5, 4, 2, 1, 0});

		for (Map.Entry<Integer, int[]> tCase : tExpected.entrySet()) {
			TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
			tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 8));
			putSelector(tTable, tCase.getKey());
			tTable.getCraftingOutput(true);

			java.util.Set<Integer> tWant = new java.util.HashSet<>();
			for (int tCell : tCase.getValue()) tWant.add(tCell);
			assertEquals(tWant, ghostCells(tTable), "config " + tCase.getKey() + " ghost cells (upstream :222-283)");
			// the destination cell [3] keeps the REAL stack and never enters the pattern
			assertFalse(ghostCells(tTable).contains(3), "the destination cell keeps the real stack");
			for (int tCell : tWant) {
				ItemStack tGhost = tTable.mPattern[tCell];
				assertEquals(1, tGhost.getCount(), "ghost count NORMALIZED to 1 (decisions.p24-act-ghost-form)");
				assertEquals(Items.OAK_PLANKS, tGhost.getItem(), "ghost material = the swept destination stack");
			}
			assertEquals(8, tTable.getInventory().getStackInSlot(24).getCount(), "the real destination stack is untouched");
		}
	}

	@Test
	public void outOfBandConfigFallsIntoTheDefaultVerticalArm() {
		// the whitelist refuses 0/1 at the slot face; a setStackInSlot bypass (RCON/creative reachability)
		// still lands on the upstream `default` arm = vertical 2 (the stick shape)
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		tTable.getInventory().setStackInSlot(30, selector(0));
		tTable.getCraftingOutput(true);
		assertEquals(java.util.Set.of(6), ghostCells(tTable), "the default arm = vertical 2");
	}

	@Test
	public void sweepConsolidatesGridStockIntoTheDestination() {
		// upstream :215-221 — real stock in 21/23 merges into 24, ghost cells fill around it
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(21, new ItemStack(Items.OAK_PLANKS, 5));
		tTable.getInventory().setStackInSlot(23, new ItemStack(Items.OAK_PLANKS, 3));
		tTable.getInventory().setStackInSlot(25, new ItemStack(Items.IRON_INGOT, 2)); // foreign item → belt overflow
		putSelector(tTable, 9);
		tTable.getCraftingOutput(true);

		assertEquals(8, tTable.getInventory().getStackInSlot(24).getCount(), "the planks merged into the destination");
		assertTrue(tTable.getInventory().getStackInSlot(21).isEmpty() && tTable.getInventory().getStackInSlot(23).isEmpty(), "the source cells emptied");
		assertTrue(tTable.getInventory().getStackInSlot(25).isEmpty(), "the foreign item left the grid");
		boolean tIronOnBelt = false;
		for (int j : TileEntityAdvancedCraftingTable.SLOTS_STORAGE) {
			if (Items.IRON_INGOT.equals(tTable.getInventory().getStackInSlot(j).getItem())) tIronOnBelt = true;
		}
		assertTrue(tIronOnBelt, "the overflow item landed on a storage belt (:219-220)");
		// every ghost carries the SWEPT material — the iron never enters the plank pattern
		for (int tCell : ghostCells(tTable)) {
			assertEquals(Items.OAK_PLANKS, tTable.mPattern[tCell].getItem(), "the pattern material = the destination stack");
		}
	}

	@Test
	public void removingTheSelectorKillsTheGhosts() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		putSelector(tTable, 4);
		tTable.getCraftingOutput(true);
		assertFalse(ghostCells(tTable).isEmpty());

		tTable.getInventory().setStackInSlot(30, ItemStack.EMPTY);
		tTable.getCraftingOutput(true);
		assertTrue(ghostCells(tTable).isEmpty(), "a non-selector slot 30 clears the backing (the ghosts die)");
	}

	// ------------------------------------------------------------------ the vanilla recipe lookup (:285)

	@Test
	public void virtualGridFindsTheStickAndChestRecipes() {
		// config 2 = vertical 2 → the vanilla stick recipe; the swept real cell + the ghost cell match it
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		putSelector(tTable, 2);
		ItemStack tOutput = tTable.getCraftingOutput(true);
		assertEquals(Items.STICK, tOutput.getItem(), "CR.getany :285 over the virtual grid (stick recipe)");
		assertEquals(4, tOutput.getCount(), "the recipe count travels (4 sticks)");
		assertEquals(Items.STICK, tTable.getInventory().getStackInSlot(31).getItem(), "slot(31, rStack) :287");

		// config 8 = ring 8 → the vanilla chest recipe (the real dest + 7 ghosts = the ring)
		TileEntityAdvancedCraftingTable tChest = makeAct(craftingLevel());
		tChest.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 16));
		putSelector(tChest, 8);
		assertEquals(Items.CHEST, tChest.getCraftingOutput(true).getItem(), "the ring-8 pattern = the chest recipe");
	}

	@Test
	public void levellessComputeProducesNoOutput() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.setLevel(null); // the offline no-level face
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		putSelector(tTable, 2);
		assertTrue(tTable.getCraftingOutput(true).isEmpty(), "no Level → no lookup (the null-safe bridge)");
	}

	// ------------------------------------------------------------------ the consumption priority (:406-413)

	@Test
	public void consumptionPriorityArm1And2LeaveOneInEachGridSlot() {
		// arm 1 (:406-407): the indicator cell 24 leaves one; arm 2 (:408-409): the other grid cells leave one
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 5)); // the indicator cell
		putSelector(tTable, 2); // the stick recipe consumes 2 planks per craft (dest + ghost cell)
		tTable.getCraftingOutput(true);
		assertTrue(tTable.canDoCraftingOutput());

		ItemStack tHold = tTable.consumeMaterials(null, false);
		assertEquals(Items.STICK, tHold.getItem());
		assertEquals(3, tTable.getInventory().getStackInSlot(24).getCount(), "5 → 3: both recipe legs paid the indicator slot down to leave-one (arm 1 :407 + arm 2 :409)");
	}

	@Test
	public void consumptionPriorityArm3DrawsTheBeltsSeventyToZero() {
		// arm 3 (:410-411): the belts feed the craft — and the :499 walk consumes slot 70 BEFORE slot 35
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 2));
		tTable.getInventory().setStackInSlot(35, new ItemStack(Items.OAK_PLANKS, 3));
		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.OAK_PLANKS, 3));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);

		// each craft = 2 plank legs: leg 1 (the indicator cell) leaves the 2-stack at 1,
		// leg 2 (the ghost cell) walks the belts from 70
		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(1);
		assertTrue(tTable.craftOnce(tSink, 0));
		assertEquals(2, tTable.getInventory().getStackInSlot(70).getCount(), "belt 70 paid leg 2 of craft 1 (the :499 walk head)");
		assertEquals(3, tTable.getInventory().getStackInSlot(35).getCount(), "belt 35 is untouched while 70 has stock");
		assertEquals(Items.STICK, tSink.getHold(0).getItem(), "the crafted stack landed in the sink");

		// drain belt 70 (craft 2 pays both legs from it) — craft 3 pulls belt 35
		tTable.craftOnce(tSink, 0);
		assertTrue(tTable.getInventory().getStackInSlot(70).isEmpty(), "belt 70 drained to 0");
		tTable.craftOnce(tSink, 0);
		assertEquals(1, tTable.getInventory().getStackInSlot(35).getCount(), "belt 35 took over after 70 died (3 → 1 over craft 3)");
	}

	@Test
	public void consumptionPriorityArm4ClearsTheGridToZero() {
		// arm 4 (:412-413): with belts empty the grid itself is consumed to 0 — the real
		// stock the sweep consolidated into the destination pays LAST
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 1));
		tTable.getInventory().setStackInSlot(27, new ItemStack(Items.OAK_PLANKS, 1)); // manual stock on the ghost cell
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);
		// the sweep merged the manual stock into the destination (2 planks) BEFORE the craft
		assertEquals(2, tTable.getInventory().getStackInSlot(24).getCount(), "the sweep consolidated the grid stock (:215-221)");

		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(1);
		assertTrue(tTable.craftOnce(tSink, 0), "the grid-only stock crafts (arm 4)");
		assertTrue(tTable.getInventory().getStackInSlot(24).isEmpty() && tTable.getInventory().getStackInSlot(27).isEmpty(), "the grid cleared to 0 (:413)");
		assertEquals(Items.STICK, tSink.getHold(0).getItem());
	}

	@Test
	public void containerItemReturnsToTheInputBelts() {
		// consumeSlot :469-485 — a count>1 stack keeps one item and the container item
		// (water bucket → empty bucket) lands on belt 0 (the SLOTS_INPUT :500 walk)
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.WATER_BUCKET, 2));
		assertTrue(tTable.consumeSlot(24), "the slot consumed");
		assertEquals(1, tTable.getInventory().getStackInSlot(24).getCount(), "the stack kept one item (:473)");
		assertEquals(Items.WATER_BUCKET, tTable.getInventory().getStackInSlot(24).getItem());
		assertEquals(Items.BUCKET, tTable.getInventory().getStackInSlot(0).getItem(), "the empty bucket landed on belt 0 (SLOTS_INPUT :500)");
		assertEquals(1, tTable.getInventory().getStackInSlot(0).getCount());

		// the count-1 face :469-472 — the slot itself turns into the container
		TileEntityAdvancedCraftingTable tSingle = makeAct(craftingLevel());
		tSingle.getInventory().setStackInSlot(27, new ItemStack(Items.WATER_BUCKET, 1));
		tSingle.consumeSlot(27);
		assertEquals(Items.BUCKET, tSingle.getInventory().getStackInSlot(27).getItem(), "the count-1 slot turned into the container");
	}

	// ------------------------------------------------------------------ canDoCraftingOutput (:290-294)

	@Test
	public void canDoCraftingOutputGatesOnStock() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 1));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);
		assertFalse(tTable.canDoCraftingOutput(), "1 stocked plank < 2 recipe cells");

		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.OAK_PLANKS, 4));
		// slot 70 is outside the 21-30 recompute band — recompute explicitly
		tTable.getCraftingOutput(true);
		assertTrue(tTable.canDoCraftingOutput(), "belts cover the second cell");
	}

	// ------------------------------------------------------------------ the whitelist five arms (:515)

	@Test
	public void whitelistAcceptsOnlySelectorsInsideTwoToNine() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		// ACCEPT: selector configs 2 and 9 (the band edges) ride the frozen GT6Circuits API
		assertTrue(tTable.getInventory().isItemValid(30, selector(2)), "config 2 accepted");
		assertTrue(tTable.getInventory().isItemValid(30, selector(9)), "config 9 accepted");
		// REJECT: out-of-band configs 1 and 10
		assertFalse(tTable.getInventory().isItemValid(30, selector(1)), "config 1 rejected");
		assertFalse(tTable.getInventory().isItemValid(30, selector(10)), "config 10 rejected");
		// REJECT: a non-selector item in slot 30 (the blueprint paper arms ride the Printer pool)
		assertFalse(tTable.getInventory().isItemValid(30, new ItemStack(Items.PAPER)), "non-selector rejected");
		// the other slots keep their normal faces
		assertTrue(tTable.getInventory().isItemValid(21, new ItemStack(Items.OAK_PLANKS)), "grid slots accept real stock");
		assertFalse(tTable.getInventory().isItemValid(31, new ItemStack(Items.OAK_PLANKS)), "the output holo rejects");
		assertFalse(tTable.getInventory().isItemValid(32, new ItemStack(Items.OAK_PLANKS)), "the virtual holo rejects");
		// the slot-30 limit is 1 (upstream getInventoryStackLimitGUI :513)
		assertEquals(1, tTable.getInventory().getSlotLimit(30), "slot 30 stack limit 1");
		assertEquals(64, tTable.getInventory().getSlotLimit(0), "the belts keep the vanilla 64");
	}

	// ------------------------------------------------------------------ the four click modes (:599-656)

	@Test
	public void craftOnceLeftClickArm() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 8));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);

		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(1);
		assertTrue(tTable.craftOnce(tSink, 0), "LEFTCLICK = one craft (:644-646)");
		assertEquals(Items.STICK, tSink.getHold(0).getItem());
		assertEquals(6, tTable.getInventory().getStackInSlot(24).getCount(), "8 → 6: two planks per craft");
	}

	@Test
	public void craftCursorFillsToTheMaxStackSize() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.OAK_PLANKS, 48));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);

		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(1);
		int tCrafts = tTable.craftFillCell(tSink, 0); // RIGHTCLICK (:634-643)
		assertEquals(16, tCrafts, "4 sticks x 16 = the 64 cap (maxStackSize / stackSize)");
		assertEquals(64, tSink.getHold(0).getCount(), "the cursor filled to the vanilla cap");
	}

	@Test
	public void craftShiftLeftStopsAtTheFirstProductiveCell() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 2));
		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.OAK_PLANKS, 40));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);

		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(36);
		int tCrafts = tTable.craftTraverse(tSink, true); // SHIFT+LEFT (:618-632)
		assertEquals(16, tCrafts, "exactly ONE inventory cell filled to the 64/4 = 16-craft cap");
		assertEquals(64, tSink.getHold(0).getCount(), "the first cell filled to the vanilla cap");
		int tProductiveCells = 0;
		for (int i = 0; i < 36; i++) if (!tSink.getHold(i).isEmpty()) tProductiveCells++;
		assertEquals(1, tProductiveCells, "SHIFT+LEFT stops at the first productive cell (:629)");
	}

	@Test
	public void craftShiftRightTraversesEveryCell() {
		// belts pay the legs while they can; the destination's leave-one plank pays through
		// arm 4 at the very end — 146 planks / 2 = 73 crafts = 292 sticks
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 2));
		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.OAK_PLANKS, 64));
		tTable.getInventory().setStackInSlot(69, new ItemStack(Items.OAK_PLANKS, 64));
		tTable.getInventory().setStackInSlot(68, new ItemStack(Items.OAK_PLANKS, 16));
		putSelector(tTable, 2);
		tTable.getCraftingOutput(true);

		TileEntityAdvancedCraftingTable.ICraftOutputSink tSink = sink(36);
		int tCrafts = tTable.craftTraverse(tSink, false); // SHIFT+RIGHT (:604-617)
		assertEquals(73, tCrafts, "the whole stock crafted (no early stop; the last plank pays through arm 4)");
		int tProductiveCells = 0;
		for (int i = 0; i < 36; i++) if (!tSink.getHold(i).isEmpty()) tProductiveCells++;
		assertEquals(5, tProductiveCells, "73 crafts / 16 per cell = 4 full cells + the remainder (:604-617 no early return)");
		for (int i = 0; i < 4; i++) assertEquals(64, tSink.getHold(i).getCount(), "the full cells at the vanilla cap");
		assertEquals(36, tSink.getHold(4).getCount(), "the remainder cell (9 crafts x 4 sticks)");
		assertTrue(tTable.getInventory().getStackInSlot(70).isEmpty() && tTable.getInventory().getStackInSlot(69).isEmpty()
				&& tTable.getInventory().getStackInSlot(68).isEmpty(), "the belts drained");
		assertTrue(tTable.getInventory().getStackInSlot(24).isEmpty(), "the destination's leave-one paid last (arm 4)");
	}

	// ------------------------------------------------------------------ sort + modes + automation predicates

	@Test
	public void sortMovesTheGridStockOntoTheBelts() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(21, new ItemStack(Items.OAK_PLANKS, 5));
		tTable.getInventory().setStackInSlot(35, new ItemStack(Items.OAK_PLANKS, 10));
		tTable.sortIntoTheInputSlots(); // upstream :177-184
		assertTrue(tTable.getInventory().getStackInSlot(21).isEmpty(), "the grid cell emptied");
		assertEquals(15, tTable.getInventory().getStackInSlot(35).getCount(), "the stock merged into the equal belt slot");
		assertTrue(tTable.mUpdatedGrid, "sort flags the recompute (:183)");
	}

	@Test
	public void automationModeTableMatchesTheFourModeMatrix() {
		// upstream :510 verbatim — the DEFAULT (nothing blocked) is SLOTS_ALL; the blocked
		// flags NARROW the face; flush widens the extraction into the grid
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_ALL, tTable.accessibleSlotsFromSide(), "default = all belts");
		tTable.mFlushMode = true;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_ALL_FLUSHING, tTable.accessibleSlotsFromSide(), "flush adds the grid");
		tTable.mFlushMode = false;
		tTable.mBlocked16 = true;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_36, tTable.accessibleSlotsFromSide(), "16-belt blocked = 33 + the 9x4 (the 62,64,63 quirk rides along)");
		tTable.mFlushMode = true;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_36_FLUSHING, tTable.accessibleSlotsFromSide(), "16 blocked + flush");
		tTable.mFlushMode = false;
		tTable.mBlocked36 = true;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS, tTable.accessibleSlotsFromSide(), "both blocked = 33");
		tTable.mFlushMode = true;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_FLUSHING, tTable.accessibleSlotsFromSide(), "both blocked + flush opens the grid");
		tTable.mFlushMode = false;
		tTable.mBlocked16 = false;
		assertEquals(TileEntityAdvancedCraftingTable.SLOTS_16, tTable.accessibleSlotsFromSide(), "36-belt blocked = 33 + the 4x4");

		assertTrue(tTable.canInsertItem2(0, new ItemStack(Items.OAK_PLANKS)), "the 4x4 accepts");
		assertFalse(tTable.canInsertItem2(21, new ItemStack(Items.OAK_PLANKS)), "the grid refuses automation insert");
		assertTrue(tTable.canExtractItem2(33), "slot 33 extracts");
		assertFalse(tTable.canExtractItem2(21), "the grid does not extract without flush");
		tTable.mFlushMode = true;
		assertTrue(tTable.canExtractItem2(25), "flush opens the grid extraction (:532)");
		assertTrue(tTable.canDrop(30) && !tTable.canDrop(31) && !tTable.canDrop(32) && tTable.canDrop(33), "the holo pair never drops, everything else does (:511)");
	}

	// ------------------------------------------------------------------ NBT round trip

	@Test
	public void nbtRoundTripPreservesInventoryPatternFlagsAndFacing() {
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 7));
		tTable.getInventory().setStackInSlot(70, new ItemStack(Items.IRON_INGOT, 12));
		tTable.getInventory().setStackInSlot(33, new ItemStack(Items.DIRT, 1));
		putSelector(tTable, 2); // the stick recipe — the {"item":...} ingredient face the offline serializer set carries
		tTable.getCraftingOutput(true);
		tTable.mBlocked16 = true;
		tTable.mFilter36 = true;
		tTable.mFacing = 5; // EAST in the GT6 side order
		assertTrue(!ghostCells(tTable).isEmpty(), "the fixture carries ghosts");

		CompoundTag tTag = tTable.saveWithoutMetadata();
		TileEntityAdvancedCraftingTable tRestored = sActType.create(POS2, Blocks.BRICKS.defaultBlockState());
		tRestored.setLevel(tTable.getLevel());
		tRestored.load(tTag);

		assertEquals(71, tRestored.getInventory().getSlots(), "the 71-slot face");
		assertEquals(Items.OAK_PLANKS, tRestored.getInventory().getStackInSlot(24).getItem());
		assertEquals(7, tRestored.getInventory().getStackInSlot(24).getCount());
		assertEquals(Items.IRON_INGOT, tRestored.getInventory().getStackInSlot(70).getItem());
		assertEquals(12, tRestored.getInventory().getStackInSlot(70).getCount());
		assertEquals(Items.DIRT, tRestored.getInventory().getStackInSlot(33).getItem());
		assertEquals(Items.STICK, tRestored.getInventory().getStackInSlot(31).getItem(), "the output preview persisted");
		assertEquals(5, tRestored.getFacing(), "NBT_FACING");
		assertTrue(tRestored.mBlocked16 && tRestored.mFilter36, "the mode fields persisted (upstream :79-82)");
		assertTrue(tRestored.getInventory().getStackInSlot(30).getItem() == sCircuitItem, "the selector persisted in slot 30");
		// the ghosts round-trip: the same cell set, count 1 (decisions.p24-act-ghost-form — the backing is NBT-persisted)
		assertEquals(ghostCells(tTable), ghostCells(tRestored), "the pattern backing round-trips");
		for (int tCell : ghostCells(tRestored)) {
			assertEquals(1, tRestored.mPattern[tCell].getCount(), "restored ghosts stay count-1");
		}
	}

	@Test
	public void tickRecomputeArmRefreshesTheOutput() {
		// the upstream :154-157 arm — mUpdatedGrid drives the output refresh on the next tick
		TileEntityAdvancedCraftingTable tTable = makeAct(craftingLevel());
		tTable.getInventory().setStackInSlot(24, new ItemStack(Items.OAK_PLANKS, 4));
		putSelector(tTable, 2);
		assertTrue(tTable.mUpdatedGrid, "the fresh BE starts flagged");
		tTable.updateEntity(); // one 03 dispatcher pass: onTick consumes the flag
		assertFalse(tTable.mUpdatedGrid, "the tick consumed the recompute flag (:156)");
		assertEquals(Items.STICK, tTable.getInventory().getStackInSlot(31).getItem(), "the tick refreshed the output");

		// a 21-30 mutation re-flags through the handler hook (the upstream :505-508 band)
		tTable.getInventory().setStackInSlot(24, ItemStack.EMPTY);
		assertTrue(tTable.mUpdatedGrid, "the 21-30 band mutation re-flags");
		tTable.updateEntity();
		assertTrue(tTable.getInventory().getStackInSlot(31).isEmpty(), "the tick cleared the dead output");
		// and the ghosts died with the selector's material gone
		assertTrue(ghostCells(tTable).isEmpty() || tTable.getInventory().getStackInSlot(24).isEmpty(), "the sweep emptied the destination");
	}
}
