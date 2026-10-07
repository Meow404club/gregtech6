package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.common.ToolActions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The wrench-interaction-key pin (task wrench-interaction-key — research.wrench-grid-status
 * verdict ③ "built but not wired"): ONE shared stack seam,
 * {@link GT6ToolActions#isWrenchInteractionKey}, sits behind every wrench-substitute
 * interaction point — the fluid-pipe connection toggle ({@code GTFluidPipeBlock.use}),
 * the item-pipe connection toggle ({@code GTItemPipeBlock.use}), the oven rotation
 * ({@code GTOvenBlock.use}, shift) and the nine-cell overlay display
 * ({@code GTWrenchHighlightListener.isWrenchHeld}). The four call sites delegate to
 * that single symbol (compile-enforced — there is no second spelling to drift), so the
 * truth table below IS the shown-means-clickable invariant: the same held-item set
 * displays the grid and clicks it.
 *
 * <p>The three acceptance states, per family:
 * <ul>
 * <li>the formal WRENCH ({@link GTWrenchItem}) — THE fix: before this card it
 *     classified WRENCH only while every point keyed bare HOE_DIG, so the real wrench
 *     triggered nothing (the overlay never showed, the pipes never toggled);</li>
 * <li>the vanilla hoe — the upstream 1.7.10 substitute key, retained (both pipe
 *     families and the oven keep working with a hoe in hand);</li>
 * <li>the empty hand and foreign items — never in, nowhere.</li>
 * </ul>
 * The wrench enters through the WRENCH leg: the item itself still never classifies
 * HOE_DIG (the decisions.p25-tool-hammer-wrench-rulings ② red line, pinned in
 * {@link HammerWrenchTest#classificationIsOneActionEach}). The item-pipe family has no
 * grid display yet (its shift layer is the monkeyWrench four-state disable cycle, a
 * different data face from the fluid pipe's ioMask — the renderer arm is declared
 * defer in the listener); its clickable half rides the same seam, pinned here.
 */
public class WrenchInteractionKeyTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** THE fix: a formal-wrench stack now passes the key the interaction points gate on. */
	@Test
	public void formalWrenchStacksAreTheKey() {
		GTWrenchItem tWrench = probeItem("wrench_key_probe_wrench", GTWrenchItem::new);
		ItemStack tStack = new ItemStack(tWrench);
		assertTrue(tWrench.classifies(GT6ToolActions.WRENCH), "the item's own classification is unchanged");
		assertFalse(tWrench.classifies(ToolActions.HOE_DIG),
				"the wrench enters through the WRENCH leg, never by classifying HOE_DIG (p25 ②)");
		assertTrue(GT6ToolActions.isWrenchInteractionKey(tStack),
				"THE FIX: the formal wrench shows the nine-cell grid and clicks the pipes/oven");
	}

	/** Upstream fidelity: every vanilla hoe tier keeps the substitute face (display AND click). */
	@Test
	public void vanillaHoeStaysTheSubstituteKey() {
		assertTrue(GT6ToolActions.isWrenchInteractionKey(new ItemStack(Items.WOODEN_HOE)),
				"the hoe was the 1.7.10 wrench key — it stays one (fluid pipe, item pipe, oven, overlay)");
		assertTrue(GT6ToolActions.isWrenchInteractionKey(new ItemStack(Items.NETHERITE_HOE)),
				"all tiers ride the same HOE_DIG leg");
	}

	/** The bare hand and foreign items never show the grid nor toggle anything. */
	@Test
	public void bareHandAndForeignItemsStayOut() {
		assertFalse(GT6ToolActions.isWrenchInteractionKey(ItemStack.EMPTY), "the bare hand is never the key");
		assertFalse(GT6ToolActions.isWrenchInteractionKey(new ItemStack(Items.STICK)));
		assertFalse(GT6ToolActions.isWrenchInteractionKey(new ItemStack(Items.IRON_PICKAXE)),
				"the vanilla dig actions are not the key either");
		assertFalse(GT6ToolActions.isWrenchInteractionKey(new ItemStack(Items.DIRT)));
	}

	/**
	 * The self-owned tool red lines compose with the key: the crowbar and the gt6 hoe
	 * classify neither of the two key actions, so the wider key did not widen the door
	 * for them (the crowbar's regression wall, family-wide).
	 */
	@Test
	public void selfOwnedToolsStayOutByConstruction() {
		assertFalse(GTCrowbarItem.classifies(GT6ToolActions.WRENCH));
		assertFalse(GTCrowbarItem.classifies(ToolActions.HOE_DIG));
		assertFalse(GTHoeItem.classifies(GT6ToolActions.WRENCH));
		assertFalse(GTHoeItem.classifies(ToolActions.HOE_DIG), "the gt6 hoe keeps its tilling-only face");
	}

	/**
	 * The HammerWrenchTest:274 reflection bracket, verbatim posture — the intrusive-holder
	 * wall is opened with the ForgeRegistry.unfreeze() window (the probe item is
	 * registered under a dedicated probe id and never reaches any committed data), so the
	 * WRENCH leg can be judged on a REAL stack instead of a static seam only.
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Function<Item.Properties, I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// the Forge runtime shape: three locks must open (see HammerWrenchTest.probeItem)
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
			// the 21.1 runtime shape: a single frozen flag guards both the intrusive-holder
			// construction and Registry.register
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.apply(new Item.Properties().durability(512));
		// the gt6 namespace is LOAD-BEARING: the String overload would land the probe in
		// the minecraft namespace, growing the frozen-vanilla pool GT6RecipesCokeOvenTest's
		// synthetic universe rides (its wrap-around aliasing re-deals on pool size).
		net.minecraft.core.Registry.register(tRegistry, new net.minecraft.resources.ResourceLocation("gt6", aProbeId), rItem);
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
}
