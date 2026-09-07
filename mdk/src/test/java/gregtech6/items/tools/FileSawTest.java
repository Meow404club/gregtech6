/**
 * Offline tests for task p24-tool-system: the file + saw pair (the crafting-tool
 * items), their ToolAction/dispatch-id pins, the crafting-loss seam and the tag
 * face the recipe provider consumes.
 *
 * <p>Assertion surface notes (the CutterTest/GT6ToolsCreativeTabTest constraints):
 * a mod Item cannot normally be constructed in this bootstrapped-and-frozen JVM
 * (the Item.java:61 intrusive-holder wall) — the classification and static-seam
 * faces therefore pin through the STATIC seams ({@link GT6FileItem#classifies},
 * {@link GT6FileItem#craftRemaining}) over VANILLA damageable stacks, while the
 * REAL crafting channel gets its own probe: the intrusive-holder wall is opened
 * for the construction with the GTWireBlockUseLockTest reflection bracket (the
 * Forge-internal {@code ForgeRegistry.unfreeze()} — a test-JVM-local write
 * window, the wire lock test's exact precedent), after which the probe calls
 * {@code Recipe.getRemainingItems} DIRECTLY — the vanilla crafting loop's own
 * dispatch (Recipe.java:26 {@code item.hasCraftingRemainingItem()} gate THEN
 * {@code getCraftingRemainingItem()}). This is the S1-review anti-regression
 * probe (id410): the dead-gate shape — overriding get without has — leaves the
 * gate false and the probe sees EMPTY instead of the worn tool, so it goes red.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GT6Tools;

public class FileSawTest {

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

	// ------------------------------------------------------- the action + id pins

	/** The action-name pins — "gt6_file"/"gt6_saw" (the crowbar/chisel ADR-name shape). */
	@Test
	public void actionNamesAreThePinnedLiterals() {
		assertEquals("gt6_file", GT6ToolActions.FILE.name());
		assertEquals("gt6_saw", GT6ToolActions.SAW.name());
		assertNotSame(GT6ToolActions.FILE, GT6ToolActions.SAW, "the registry interns distinct actions");
	}

	/**
	 * The dispatch ids — the upstream CS.TOOL_file/TOOL_saw strings verbatim
	 * (CS.java:1050/:1049), the CHISEL_ID/CUTTER_ID shape.
	 */
	@Test
	public void dispatchIdsAreTheUpstreamStrings() {
		assertEquals("file", GT6ToolActions.FILE_ID);
		assertEquals("saw", GT6ToolActions.SAW_ID);
	}

	/** The classification faces — each item performs exactly its own action (the red line). */
	@Test
	public void classificationIsOneActionEach() {
		assertTrue(GT6FileItem.classifies(GT6ToolActions.FILE));
		assertTrue(GTSawItem.classifies(GT6ToolActions.SAW));
		assertFalse(GT6FileItem.classifies(GT6ToolActions.SAW));
		assertFalse(GT6FileItem.classifies(GT6ToolActions.CROWBAR));
		assertFalse(GT6FileItem.classifies(GT6ToolActions.CUTTER));
		assertFalse(GT6FileItem.classifies(GT6ToolActions.CHISEL));
		assertFalse(GTSawItem.classifies(GT6ToolActions.FILE));
		assertFalse(GTSawItem.classifies(GT6ToolActions.CROWBAR));
		// the wrench-substitute red line (the crowbar card's HOE_DIG wall, family-wide)
		assertFalse(GT6FileItem.classifies(ToolActions.HOE_DIG));
		assertFalse(GTSawItem.classifies(ToolActions.HOE_DIG));
	}

	// ------------------------------------------------------- the family + loss pins

	/** The single steel tier — 512 durability points on both items (the family value). */
	@Test
	public void durabilityIsTheFamilyValue() {
		assertEquals(512, GT6FileItem.DURABILITY_POINTS);
		assertEquals(512, GTSawItem.DURABILITY_POINTS);
	}

	/**
	 * The loss mapping (decisions.p24-tool-system-damage-mapping): ONE point per craft
	 * on BOTH items — the declared deviation from the upstream 400/100 unit ratio
	 * (GT_Tool_File.java:47-49 / GT_Tool_Saw.java:65-67) pinned as a single source
	 * ({@link GT6FileItem#DAMAGE_PER_CRAFT}, the saw delegates).
	 */
	@Test
	public void lossPerCraftIsOnePointShared() {
		assertEquals(1, GT6FileItem.DAMAGE_PER_CRAFT);
	}

	/** The seam on a wearable stack: one craft = one damage point, original untouched. */
	@Test
	public void craftRemainingPaysOnePointPerCraft() {
		ItemStack tStack = new ItemStack(Items.IRON_PICKAXE);
		ItemStack tResult = GT6FileItem.craftRemaining(tStack, GT6FileItem.DAMAGE_PER_CRAFT);
		assertSame(Items.IRON_PICKAXE, tResult.getItem(), "the tool follows the craft (the container-item channel)");
		assertEquals(1, tResult.getDamageValue(), "exactly one point per craft");
		assertEquals(0, tStack.getDamageValue(), "the grid input is never mutated");
	}

	/** The wear-out boundary: damage == maxDamage survives (strictly-greater consumes). */
	@Test
	public void craftRemainingAtTheMaxBoundaryStillReturnsTheTool() {
		ItemStack tStack = new ItemStack(Items.IRON_PICKAXE);
		tStack.setDamageValue(tStack.getMaxDamage() - 1);
		ItemStack tResult = GT6FileItem.craftRemaining(tStack, GT6FileItem.DAMAGE_PER_CRAFT);
		assertFalse(tResult.isEmpty(), "newDamage == maxDamage is a returned copy, not a consume");
		assertEquals(tStack.getMaxDamage(), tResult.getDamageValue());
	}

	/** The consume branch: a craft past maxDamage hands back EMPTY (the worn-out tool). */
	@Test
	public void craftRemainingPastTheMaxConsumesTheTool() {
		ItemStack tStack = new ItemStack(Items.IRON_PICKAXE);
		tStack.setDamageValue(tStack.getMaxDamage());
		assertTrue(GT6FileItem.craftRemaining(tStack, GT6FileItem.DAMAGE_PER_CRAFT).isEmpty(),
				"newDamage > maxDamage consumes the tool (the upstream worn-out null)");
	}

	/** The shared seam on a blank input: no crash, plain consume (the defensive boundary). */
	@Test
	public void craftRemainingOnAnEmptyStackConsumes() {
		assertTrue(GT6FileItem.craftRemaining(ItemStack.EMPTY, GT6FileItem.DAMAGE_PER_CRAFT).isEmpty());
	}

	// ------------------------------------------------------- the tag face

	/**
	 * The four self-owned tags — the oredict-name snake translation ruling
	 * (decisions.p24-tool-system-tag-strategy): craftingToolFile/craftingToolSaw
	 * (CS.java:1867/:1864) → tools/file, tools/saw; dustRedstone → redstone;
	 * plateCurvedSn → plate_curved_tin.
	 */
	@Test
	public void tagNamesAreTheSnakeTranslations() {
		assertEquals(rl("tools/file"), GT6ItemTags.TOOLS_FILE.location());
		assertEquals(rl("tools/saw"), GT6ItemTags.TOOLS_SAW.location());
		assertEquals(rl("redstone"), GT6ItemTags.REDSTONE_DUSTS.location());
		assertEquals(rl("plate_curved_tin"), GT6ItemTags.PLATE_CURVED_TIN.location());
	}

	/**
	 * The ecosystem tag face — the platform tools tag constant, path pinned namespace-
	 * agnostic; the namespace itself is the leg split (forge leg = "forge", 21.1 leg
	 * = "c", the Tags.java:419/:799 constants).
	 */
	@Test
	public void ecosystemToolsTagPathIsPinned() {
		//? if forge {
		assertEquals("tools", net.minecraftforge.common.Tags.Items.TOOLS.location().getPath());
		//?} else {
		/*assertEquals("tools", net.neoforged.neoforge.common.Tags.Items.TOOLS.location().getPath());
		*///?}
	}

	/**
	 * The registration wiring — the file + saw ids exist in the registry home and the
	 * GT6ItemTags helpers compose gt6-namespaced keys (the recipe-provider dependency).
	 */
	@Test
	public void registrationAndTagHelperShape() {
		assertEquals(rl("file"), GT6Tools.FILE.getId());
		assertEquals(rl("saw"), GT6Tools.SAW.getId());
		assertEquals(rl("tools/file"), GT6ItemTags.gt6("tools/file").location());
	}

	// --------------------------------------------- the REAL crafting-channel probe (id410)

	/**
	 * The S1-review anti-regression probe (id410): the vanilla crafting loop's own
	 * dispatch — {@code Recipe.getRemainingItems}, the Recipe.java:26 gate-then-get
	 * shape the server runs — called DIRECTLY over a 3x3 grid holding the tool. The
	 * dead-gate regression (get overridden without the hasCraftingRemainingItem gate)
	 * fails the FIRST assertion with EMPTY instead of the worn tool. Also pins the
	 * full loss semantics on the live items: same item comes back, exactly
	 * {@link GT6FileItem#DAMAGE_PER_CRAFT} wearier, the grid input untouched, every
	 * other cell stays empty.
	 */
	@Test
	public void realCraftingChannelKeepsTheFileAndPaysOnePoint() {
		GT6FileItem tFile = probeItem("crafting_probe_file", GT6FileItem::new);
		ItemStack tInput = new ItemStack(tFile);
		tInput.setDamageValue(3);
		ItemStack tRemaining = craftingChannel(tInput);
		assertFalse(tRemaining.isEmpty(), "the gate must be OPEN (has overridden) — EMPTY here is the id410 dead-gate shape");
		assertSame(tFile, tRemaining.getItem(), "the tool follows the craft on the live dispatch");
		assertEquals(3 + GT6FileItem.DAMAGE_PER_CRAFT, tRemaining.getDamageValue(), "exactly one point paid on the live dispatch");
		assertEquals(3, tInput.getDamageValue(), "the grid input is never mutated");
	}

	/** The saw rides the same live channel (its own has/get pair — no delegation shortcut). */
	@Test
	public void realCraftingChannelKeepsTheSawAndPaysOnePoint() {
		GTSawItem tSaw = probeItem("crafting_probe_saw", GTSawItem::new);
		ItemStack tInput = new ItemStack(tSaw);
		tInput.setDamageValue(tSaw.DURABILITY_POINTS - 2);
		ItemStack tRemaining = craftingChannel(tInput);
		assertFalse(tRemaining.isEmpty(), "the saw gate must be open at one-below-max (strictly-greater consumes)");
		assertEquals(tSaw.DURABILITY_POINTS - 2 + GT6FileItem.DAMAGE_PER_CRAFT, tRemaining.getDamageValue());
	}

	/**
	 * The GTWireBlockUseLockTest:43 reflection bracket, item flavor. THREE locks guard
	 * the offline item registry (namespaced-wrapper shape — runtime class of
	 * {@code BuiltInRegistries.ITEM} is a {@code NamespacedWrapper} over a ForgeRegistry
	 * delegate):
	 * <ol>
	 * <li>the vanilla {@code frozen} flag — the Item constructor's intrusive-holder gate
	 * (Item.java:61); {@code NamespacedWrapper.unfreeze()} clears it;</li>
	 * <li>the Forge wrapper register lock ({@code NamespacedWrapper.locked}, the
	 * "Modder should use Forge Register methods" gate);</li>
	 * <li>the {@code ForgeRegistry.isFrozen} delegate flag — its own {@code unfreeze()}
	 * clears this one AND mirrors to the wrapper lock (ForgeRegistry.java:693-698).</li>
	 * </ol>
	 * Test-JVM-local; the probe item is registered under a dedicated probe id (an
	 * ItemStack constructor resolves the registry delegate eagerly, so an unregistered
	 * item cannot ride the channel) and never reaches any committed data.
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
				return rl("crafting_channel_probe");
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

			// the probe id face (forge leg rides Recipe.getId) is not part of the 1.21
			// interface — the channel identity lives on the RecipeHolder wrapper there
		};
		NonNullList<ItemStack> tRemainders = tChannel.getRemainingItems(tGrid);
		for (int i = 1; i < tRemainders.size(); i++) {
			assertTrue(tRemainders.get(i).isEmpty(), "no side cells may produce remainders, saw one at " + i);
		}
		return tRemainders.get(0);
	}
	*///?}
}
