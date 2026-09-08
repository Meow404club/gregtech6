/**
 * Offline tests for task p24-screwdriver-item: the screwdriver (the crafting-tool item
 * in the GT6FileItem form), its ToolAction/dispatch-id pins — including the
 * {@code ICover.TOOL_SCREWDRIVER} reservation parity (the card's zero-ICover-diff
 * drift guard) — and the real crafting channel the loss seam rides.
 *
 * <p>Assertion surface (the FileSawTest constraints, verbatim posture): a mod Item
 * cannot normally be constructed in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the REAL crafting channel gets its own probe: the wall is
 * opened with the GTWireBlockUseLockTest reflection bracket (the Forge-internal
 * {@code ForgeRegistry.unfreeze()} — a test-JVM-local write window), after which the
 * probe calls {@code Recipe.getRemainingItems} DIRECTLY — the vanilla crafting loop's
 * own dispatch (Recipe.java:26 {@code item.hasCraftingRemainingItem()} gate THEN
 * {@code getCraftingRemainingItem()}). This is the id410/id413 anti-regression probe
 * (the review-mandated shape for every container-item-channel feature): the dead-gate
 * shape — overriding get without has — leaves the gate false and the probe sees EMPTY
 * instead of the worn tool, so it goes red.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.covers.ICover;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GT6Tools;

public class ScrewdriverTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		prebuildSyntheticUniverse();
	}

	/**
	 * THE PROBE-LEAK INVARIANT (the p24-screwdriver-item takeover lesson, found by the
	 * full-suite gate): the probe items this class registers are REAL registry entries
	 * (the ItemStack ctor resolves the registry delegate eagerly — an unregistered item
	 * cannot ride the channel), and the machine suite's
	 * {@code TileEntityBasicMachineOfflineTestBase.buildSyntheticUniverse} walks
	 * {@code BuiltInRegistries.ITEM.stream()} ONCE per JVM to alias the synthetic
	 * (prefix, material) universe onto existing items. Any probe present at THAT build
	 * time shifts the aliasing wholesale — two extra probe entries deterministically
	 * broke the gem-chain parallel/consume semantics seven tests downstream (the
	 * full-suite 7-red repro; the isolated classes stayed green). So the universe MUST
	 * be built before the FIRST screwdriver probe exists: this reflection pre-build
	 * (the package-private static {@code @BeforeAll}, shared source on both legs) locks
	 * the snapshot to whichever historically-verified state precedes this class
	 * (FileSawTest's two probes, the main-verified mapping, when the alphabetical order
	 * holds; the pristine pool otherwise). The probes themselves stay the verbatim
	 * FileSawTest shape — the fix is ordering of the BUILD, not the channel.
	 */
	private static void prebuildSyntheticUniverse() {
		try {
			Class<?> tBase = Class.forName("gregtech6.tileentity.machines.TileEntityBasicMachineOfflineTestBase");
			java.lang.reflect.Method tBuild = tBase.getDeclaredMethod("buildSyntheticUniverse");
			tBuild.setAccessible(true);
			tBuild.invoke(null);
		} catch (Throwable aE) {
			throw new IllegalStateException("could not pre-build the machine synthetic universe before the screwdriver probes exist", aE);
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	// ------------------------------------------------------- the action + id pins

	/** The action-name pin — "gt6_screwdriver" (the gt6-prefixed family shape). */
	@Test
	public void actionNameIsThePinnedLiteral() {
		assertEquals("gt6_screwdriver", GT6ToolActions.SCREWDRIVER.name());
	}

	/**
	 * The dispatch id — the upstream {@code CS.TOOL_screwdriver} string verbatim
	 * (CS.java:1057), AND the {@code ICover.TOOL_SCREWDRIVER} reservation parity: the
	 * pump cover's direction toggle keys on that exact string (ICover.java:70, the
	 * pre-existing constant — this card's zero-drift face; ICover.java itself stays at
	 * zero diff).
	 */
	@Test
	public void dispatchIdIsTheUpstreamStringAndMatchesTheICoverReservation() {
		assertEquals("screwdriver", GT6ToolActions.SCREWDRIVER_ID);
		assertEquals(GT6ToolActions.SCREWDRIVER_ID, ICover.TOOL_SCREWDRIVER,
				"the reserved cover-tool id and the action id must not drift");
	}

	/** The classification face — the item performs exactly its own action (the red line). */
	@Test
	public void classificationIsOneActionOnly() {
		assertTrue(GT6ScrewdriverItem.classifies(GT6ToolActions.SCREWDRIVER));
		assertFalse(GT6ScrewdriverItem.classifies(GT6ToolActions.FILE));
		assertFalse(GT6ScrewdriverItem.classifies(GT6ToolActions.SAW));
		assertFalse(GT6ScrewdriverItem.classifies(GT6ToolActions.CROWBAR));
		assertFalse(GT6ScrewdriverItem.classifies(GT6ToolActions.CUTTER));
		assertFalse(GT6ScrewdriverItem.classifies(GT6ToolActions.CHISEL));
		// the wrench-substitute red line (the crowbar card's HOE_DIG wall, family-wide)
		assertFalse(GT6ScrewdriverItem.classifies(ToolActions.HOE_DIG));
	}

	/**
	 * ZERO {@code useOn} by card cut — the machine-interaction face is the machine
	 * interaction card's pool (the TOOL_screwdriver-harvestable + Material.circuits
	 * surface, GT_Tool_Screwdriver.java:105-112). The class must not declare the method
	 * at all: {@code getDeclaredMethod} walks ONLY this class' declared methods, so any
	 * override (even a throwing stub) fails this pin; inheriting vanilla Item.useOn is
	 * the exact card-mandated shape.
	 */
	@Test
	public void zeroUseOnDeclaration() {
		assertThrows(NoSuchMethodException.class,
				() -> GT6ScrewdriverItem.class.getDeclaredMethod("useOn",
						net.minecraft.world.item.context.UseOnContext.class),
				"the screwdriver must not declare useOn (the world arms stay pooled)");
	}

	// ------------------------------------------------------- the family + loss pins

	/** The single steel tier — 512 durability points (the family value). */
	@Test
	public void durabilityIsTheFamilyValue() {
		assertEquals(512, GT6ScrewdriverItem.DURABILITY_POINTS);
	}

	/**
	 * The loss mapping rides the SHARED seam constant — one point per craft
	 * (decisions.p24-tool-system-damage-mapping; the declared deviation from the upstream
	 * 400 units, GT_Tool_Screwdriver.java:70-72). The screwdriver has no per-class
	 * constant to drift from: the delegation is pinned through the live channel probes
	 * below.
	 */
	@Test
	public void lossPerCraftIsTheSharedOnePointConstant() {
		assertEquals(1, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	// --------------------------------------------- the REAL crafting-channel probe (id410)

	/**
	 * The id410/id413 anti-regression probe on the live screwdriver: the vanilla crafting
	 * loop's own dispatch — {@code Recipe.getRemainingItems} — called DIRECTLY over a 3x3
	 * grid holding the tool. The dead-gate regression (get overridden without the
	 * hasCraftingRemainingItem gate) fails the FIRST assertion with EMPTY instead of the
	 * worn tool. Pins: same item back, exactly one point wearier, the grid input
	 * untouched, every other cell empty.
	 */
	@Test
	public void realCraftingChannelKeepsTheScrewdriverAndPaysOnePoint() {
		GT6ScrewdriverItem tScrewdriver = probeItem("crafting_probe_screwdriver", GT6ScrewdriverItem::new);
		ItemStack tInput = new ItemStack(tScrewdriver);
		tInput.setDamageValue(3);
		ItemStack tRemaining = craftingChannel(tInput);
		assertFalse(tRemaining.isEmpty(), "the gate must be OPEN (has overridden) — EMPTY here is the id410 dead-gate shape");
		assertSame(tScrewdriver, tRemaining.getItem(), "the tool follows the craft on the live dispatch");
		assertEquals(3 + GT6FileItem.DAMAGE_PER_CRAFT, tRemaining.getDamageValue(), "exactly one point paid on the live dispatch");
		assertEquals(3, tInput.getDamageValue(), "the grid input is never mutated");
	}

	/**
	 * The wear-out boundary on the live channel: damage at maxDamage + one more craft =
	 * strictly-greater = EMPTY (the tool is consumed, the upstream worn-out null).
	 */
	@Test
	public void realCraftingChannelConsumesTheWornOutScrewdriver() {
		GT6ScrewdriverItem tScrewdriver = probeItem("crafting_probe_screwdriver_worn", GT6ScrewdriverItem::new);
		ItemStack tInput = new ItemStack(tScrewdriver);
		tInput.setDamageValue(GT6ScrewdriverItem.DURABILITY_POINTS);
		ItemStack tRemaining = craftingChannel(tInput);
		assertTrue(tRemaining.isEmpty(),
				"newDamage > maxDamage consumes the tool on the live dispatch (the strictly-greater boundary)");
	}

	// ------------------------------------------------------- the registration + tag faces

	/** The registration id and the self-owned crafting-ingredient tag (the snake translation). */
	@Test
	public void registrationAndTagFace() {
		assertEquals(rl("screwdriver"), GT6Tools.SCREWDRIVER.getId());
		assertEquals(rl("tools/screwdriver"), GT6ItemTags.TOOLS_SCREWDRIVER.location());
	}

	/**
	 * The GTWireBlockUseLockTest:43 reflection bracket, item flavor — copied verbatim
	 * from FileSawTest.probeItem (the same three forge locks / single 21.1 frozen flag;
	 * test-JVM-local, the probe item registered under a dedicated probe id and never
	 * reaching any committed data).
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Function<Item.Properties, I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the Forge runtime shape: BuiltInRegistries.ITEM is a NamespacedWrapper over a
			// ForgeRegistry delegate — THREE locks must open (the defaulted wrapper hides
			// the lock field on the parent, hence the class-chain walk):
			// 1. the vanilla frozen flag — the Item constructor's intrusive-holder gate
			//    (Item.java:61), cleared by NamespacedWrapper.unfreeze();
			// 2. the delegate ForgeRegistry.isFrozen — its own unfreeze() clears this one
			//    AND mirrors the wrapper register lock off (ForgeRegistry.java:693-698);
			// 3. the NamespacedWrapper.locked register gate ("Modder should use Forge
			//    Register methods").
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
			// the 21.1 runtime shape: the plain vanilla DefaultedMappedRegistry (no Forge
			// wrapper) — a single frozen flag guards both the intrusive-holder construction
			// and Registry.register
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

	/**
	 * The vanilla crafting loop over a 3x3 grid: slot 0 = the tool, everything else
	 * empty. The channel is the Recipe.getRemainingItems DEFAULT the real ResultSlot.onTake
	 * path runs — the ItemStack-sensitive shape (gate has, then get). The grid face is
	 * the one leg split the probe cannot paper over: 1.20.1 walks a Container, 1.21
	 * re-typed Recipe to {@code <T extends RecipeInput>} and the crafting grid became a
	 * CraftingInput record (Recipe.java:30-40 — the gate-then-get loop is verbatim).
	 */
	//? if forge {
	private static ItemStack craftingChannel(ItemStack aTool) {
		Container tGrid = new Container() {
			private final NonNullList<ItemStack> mCells = NonNullList.withSize(9, ItemStack.EMPTY);

			@Override
			public int getContainerSize() {
				return this.mCells.size();
			}

			@Override
			public boolean isEmpty() {
				return this.mCells.stream().allMatch(ItemStack::isEmpty);
			}

			@Override
			public ItemStack getItem(int aIndex) {
				return this.mCells.get(aIndex);
			}

			@Override
			public ItemStack removeItem(int aIndex, int aCount) {
				ItemStack tCell = this.mCells.get(aIndex);
				ItemStack rTake = tCell.split(aCount);
				return rTake;
			}

			@Override
			public ItemStack removeItemNoUpdate(int aIndex) {
				return this.mCells.set(aIndex, ItemStack.EMPTY);
			}

			@Override
			public void setItem(int aIndex, ItemStack aStack) {
				this.mCells.set(aIndex, aStack);
			}

			@Override
			public void clearContent() {
				this.mCells.clear();
			}

			@Override
			public void setChanged() {
			}

			@Override
			public boolean stillValid(Player aPlayer) {
				return true;
			}
		};
		tGrid.setItem(0, aTool);
		Recipe<Container> tChannel = new Recipe<>() {
			@Override
			public boolean matches(Container aInv, net.minecraft.world.level.Level aLevel) {
				return false;
			}

			@Override
			public net.minecraft.world.item.ItemStack assemble(Container aInv, net.minecraft.core.RegistryAccess aRegistry) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean canCraftInDimensions(int aWidth, int aHeight) {
				return true;
			}

			@Override
			public net.minecraft.world.item.ItemStack getResultItem(net.minecraft.core.RegistryAccess aRegistry) {
				return ItemStack.EMPTY;
			}

			@Override
			public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
				return net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE;
			}

			@Override
			public net.minecraft.world.item.crafting.RecipeType<?> getType() {
				return net.minecraft.world.item.crafting.RecipeType.CRAFTING;
			}

			@Override
			public ResourceLocation getId() {
				return rl("screwdriver_channel_probe");
			}
		};
		NonNullList<ItemStack> tRemainders = tChannel.getRemainingItems(tGrid);
		for (int i = 1; i < tRemainders.size(); i++) {
			assertTrue(tRemainders.get(i).isEmpty(), "no side cells may produce remainders, saw one at " + i);
		}
		return tRemainders.get(0);
	}
	//?} else {
	/*private static ItemStack craftingChannel(ItemStack aTool) {
		java.util.ArrayList<ItemStack> tCells = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
		tCells.set(0, aTool);
		net.minecraft.world.item.crafting.CraftingInput tGrid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, tCells);
		net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput> tChannel = new net.minecraft.world.item.crafting.Recipe<>() {
			@Override
			public boolean matches(net.minecraft.world.item.crafting.CraftingInput aInv, net.minecraft.world.level.Level aLevel) {
				return false;
			}

			@Override
			public net.minecraft.world.item.ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput aInv, net.minecraft.core.HolderLookup.Provider aRegistries) {
				return net.minecraft.world.item.ItemStack.EMPTY;
			}

			@Override
			public boolean canCraftInDimensions(int aWidth, int aHeight) {
				return true;
			}

			@Override
			public net.minecraft.world.item.ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider aRegistries) {
				return net.minecraft.world.item.ItemStack.EMPTY;
			}

			@Override
			public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
				return net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE;
			}

			@Override
			public net.minecraft.world.item.crafting.RecipeType<?> getType() {
				return net.minecraft.world.item.crafting.RecipeType.CRAFTING;
			}

			// 21.1: Recipe.getId() moved to RecipeHolder — no id override on this leg
		};
		NonNullList<ItemStack> tRemainders = tChannel.getRemainingItems(tGrid);
		for (int i = 1; i < tRemainders.size(); i++) {
			assertTrue(tRemainders.get(i).isEmpty(), "no side cells may produce remainders, saw one at " + i);
		}
		return tRemainders.get(0);
	}
	*///?}
}
