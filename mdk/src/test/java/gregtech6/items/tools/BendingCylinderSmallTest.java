/**
 * Offline tests for task p25-food-can-row0: the small bending cylinder item
 * ({@link GT6BendingCylinderSmallItem}, the GT6FileItem form with the census OFF) and
 * the REAL crafting-loss channel over the FOUR tool letters the two new crafting rows
 * consume — 's' (saw), 'f' (file), 'h' (hammer), 'o' (the bending cylinder).
 *
 * <p>The channel probe is the id413 methodology (the HammerWrenchTest contract piece):
 * {@code Recipe.getRemainingItems} called DIRECTLY over a 3x3 grid — the vanilla
 * crafting loop's own dispatch (the has-crafting-remaining-item GATE then the GET face,
 * forge IForgeItem.java:253-256). The dead-gate shape (get overridden without has)
 * leaves the gate closed and the probe sees EMPTY. The mod-Item intrusive-holder wall
 * opens through the GTWireBlockUseLockTest reflection bracket (the
 * ForgeRegistry.unfreeze() test-JVM-local write window; the probe items register under
 * dedicated probe ids and never reach any committed data).
 *
 * <p>The census-OFF pin (card spec ②): the cylinder overrides NEITHER useOn NOR
 * canPerformAction — the upstream tool isMinableBlock returns false verbatim
 * (GT_Tool_BendingCylinderSmall.java:59-60) and carries no Behavior_Tool machine face,
 * so the port carries zero ToolAction surface. Reflection over declared methods: an
 * override would throw here.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GT6Tools;

public class BendingCylinderSmallTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	// ------------------------------------------------------- the registration + tag pins

	/** The registration wiring — the id and the Tools-tab row (the table-tail append). */
	@Test
	public void registrationIsThePinnedId() {
		assertEquals(rl("bending_cylinder_small"), GT6Tools.BENDING_CYLINDER_SMALL.getId());
		// row 9 since task p29-w5-t1-dig-six appended the six dig rows 10-15 (the
		// table-tail append discipline — the "last row" pin was a point-in-time face)
		assertSame(GT6Tools.BENDING_CYLINDER_SMALL, GT6Tools.TAB_TABLE.get(9),
				"the cylinder rides the Tools tab at row 9");
	}

	/** The self-owned tag — the craftingToolBendingCylinderSmall snake (CS.java:1903), the 'o' letter face (CR.java:207). */
	@Test
	public void tagNameIsTheSnakeTranslation() {
		assertEquals(rl("tools/bending_cylinder_small"), GT6ItemTags.TOOLS_BENDING_CYLINDER_SMALL.location());
	}

	/** The single steel tier — 512 durability points (the family value) at the shared one-point loss mapping. */
	@Test
	public void durabilityAndLossMappingAreTheFamilyValues() {
		assertEquals(512, GT6BendingCylinderSmallItem.DURABILITY_POINTS);
		assertEquals(GT6FileItem.DAMAGE_PER_CRAFT, GT6BendingCylinderSmallItem.DAMAGE_PER_CRAFT,
				"the upstream 25-unit GT_Tool_BendingCylinderSmall.java:44-46 row folds onto the shared mapping");
	}

	// ------------------------------------------------------- the census-OFF pins

	/**
	 * The zero-world-interaction + zero-ToolAction surface (card spec ②): the cylinder
	 * declares NO useOn override and NO canPerformAction override — the vanilla defaults
	 * (no-op / false for every action) ARE the declared surface.
	 */
	@Test
	public void censusIsOffZeroWorldAndActionSurface() throws NoSuchMethodException {
		assertThrows(NoSuchMethodException.class,
				() -> GT6BendingCylinderSmallItem.class.getDeclaredMethod("useOn",
						net.minecraft.world.item.context.UseOnContext.class),
				"the cylinder must not override useOn (the world arm is pooled)");
		assertThrows(NoSuchMethodException.class,
				() -> GT6BendingCylinderSmallItem.class.getDeclaredMethod("canPerformAction",
						ItemStack.class, net.minecraftforge.common.ToolAction.class),
				"the cylinder must not override canPerformAction (the census is OFF)");
	}

	// ------------------------------------------------------- the REAL crafting channel (id413)

	/**
	 * THE letter probe: one 3x3 grid, cells 0-3 holding the saw ('s'), the file ('f'),
	 * the hammer ('h') and the bending cylinder ('o') — the union of the tool letters
	 * the bending-cylinder self-craft row ({"sfh"/"III"}, Loader_Tools.java:313) and the
	 * empty-can row ({"fh"/"oP"}, MultiItemRandomTools.java:239) consume. The live
	 * Recipe.getRemainingItems dispatch must keep ALL FOUR tools riding along, each
	 * exactly one point wearier, the grid inputs untouched.
	 */
	@Test
	public void realCraftingChannelChargesAllFourLettersOnePoint() {
		GTSawItem tSaw = probeItem("crafting_probe_foodcan_saw", GTSawItem::new);
		GT6FileItem tFile = probeItem("crafting_probe_foodcan_file", GT6FileItem::new);
		GTHammerItem tHammer = probeItem("crafting_probe_foodcan_hammer", GTHammerItem::new);
		GT6BendingCylinderSmallItem tCylinder = probeItem("crafting_probe_foodcan_cylinder", GT6BendingCylinderSmallItem::new);

		ItemStack tSawStack = worn(tSaw, 10);
		ItemStack tFileStack = worn(tFile, 20);
		ItemStack tHammerStack = worn(tHammer, 30);
		ItemStack tCylinderStack = worn(tCylinder, 40);

		NonNullList<ItemStack> tRemainders = craftingChannel(tSawStack, tFileStack, tHammerStack, tCylinderStack);

		assertSame(tSaw, tRemainders.get(0).getItem(), "'s' rides along");
		assertEquals(11, tRemainders.get(0).getDamageValue(), "'s' pays exactly one point");
		assertSame(tFile, tRemainders.get(1).getItem(), "'f' rides along");
		assertEquals(21, tRemainders.get(1).getDamageValue(), "'f' pays exactly one point");
		assertSame(tHammer, tRemainders.get(2).getItem(), "'h' rides along");
		assertEquals(31, tRemainders.get(2).getDamageValue(), "'h' pays exactly one point");
		assertSame(tCylinder, tRemainders.get(3).getItem(), "'o' rides along — the cylinder IS an in-grid crafting tool");
		assertEquals(41, tRemainders.get(3).getDamageValue(), "'o' pays exactly one point (the upstream 25-unit row folded)");
		for (int i = 4; i < tRemainders.size(); i++) {
			assertTrue(tRemainders.get(i).isEmpty(), "no empty cell may produce remainders, saw one at " + i);
		}
		assertEquals(10, tSawStack.getDamageValue(), "the grid input is never mutated");
		assertEquals(40, tCylinderStack.getDamageValue(), "the grid input is never mutated");
	}

	/** The cylinder at one-below-max survives the live channel (strictly-greater consumes). */
	@Test
	public void realCraftingChannelCylinderAtTheMaxBoundaryStillReturns() {
		GT6BendingCylinderSmallItem tCylinder = probeItem("crafting_probe_foodcan_cylinder_worn", GT6BendingCylinderSmallItem::new);
		ItemStack tInput = worn(tCylinder, GT6BendingCylinderSmallItem.DURABILITY_POINTS - 1);
		NonNullList<ItemStack> tRemainders = craftingChannel(tInput);
		assertFalse(tRemainders.get(0).isEmpty(), "one-below-max is a returned copy (strictly-greater consumes)");
		assertEquals(GT6BendingCylinderSmallItem.DURABILITY_POINTS, tRemainders.get(0).getDamageValue());
	}

	/** The cylinder past the boundary is CONSUMED by the live channel (the worn-out null). */
	@Test
	public void realCraftingChannelCylinderPastTheMaxIsConsumed() {
		GT6BendingCylinderSmallItem tCylinder = probeItem("crafting_probe_foodcan_cylinder_dead", GT6BendingCylinderSmallItem::new);
		ItemStack tInput = worn(tCylinder, GT6BendingCylinderSmallItem.DURABILITY_POINTS);
		assertTrue(craftingChannel(tInput).get(0).isEmpty(), "past-max hands back EMPTY = the tool is consumed");
	}

	private static ItemStack worn(Item aItem, int aDamage) {
		ItemStack rStack = new ItemStack(aItem);
		rStack.setDamageValue(aDamage);
		return rStack;
	}

	/**
	 * The GTWireBlockUseLockTest:43 reflection bracket, the HammerWrenchTest.probeItem
	 * form verbatim (the three forge locks / single 21.1 frozen flag; the probe item is
	 * registered under a dedicated probe id and never reaches any committed data).
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Function<Item.Properties, I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the Forge runtime shape: THREE locks must open (the GT6ToolsCreativeTabTest walk)
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
			// the 21.1 runtime shape: the plain vanilla DefaultedMappedRegistry — one frozen flag
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
	 * The vanilla crafting loop over a 3x3 grid — cells 0..(tools-1) hold the given
	 * stacks, the rest empty. The Recipe.getRemainingItems DEFAULT the real
	 * ResultSlot.onTake path runs. Leg split: 1.20.1 walks a Container, 21.1 re-typed
	 * Recipe to {@code <T extends RecipeInput>} with a CraftingInput record (the
	 * HammerWrenchTest.craftingChannel shape, generalized to N tools).
	 */
	//? if forge {
	private static NonNullList<ItemStack> craftingChannel(ItemStack... aTools) {
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
			public boolean stillValid(net.minecraft.world.entity.player.Player aPlayer) {
				return true;
			}
		};
		for (int i = 0; i < aTools.length; i++) {
			tGrid.setItem(i, aTools[i]);
		}
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
				return rl("food_can_crafting_channel_probe");
			}
		};
		return tChannel.getRemainingItems(tGrid);
	}
	//?} else {
	/*private static NonNullList<ItemStack> craftingChannel(ItemStack... aTools) {
		java.util.ArrayList<ItemStack> tCells = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
		for (int i = 0; i < aTools.length; i++) {
			tCells.set(i, aTools[i]);
		}
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
		return tChannel.getRemainingItems(tGrid);
	}
	*///?}
}
