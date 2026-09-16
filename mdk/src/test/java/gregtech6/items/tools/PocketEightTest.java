/**
 * Offline tests for task p29-w5-t7-pocket-eight: the eight pocket multitool forms
 * ({@link GTPocketMultitoolItem}, the base + form-parameter shape) — the registration
 * census, the TAB_TABLE tail parity, the :176-183 ring chain and its :187-196 8x8
 * duality table (the NEI-redirect loop, 64 ordered cells), the durability-preserving
 * switch carrier, the per-form face table, and the NO-BATTERY recipe pin (the reversal
 * ruling — the :354 row's null battery column read off the committed recipe JSON).
 *
 * <p>The faces ride the static seams (the mod-Item wall keeps instances
 * unconstructible offline, the CutterTest boot NOTE); the recipe JSON rides the
 * generated-resources classpath (the GT6DieselCraftingJsonTest {@code generated()}
 * convention).
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;

import gregtech6.items.tools.pocket.GTPocketMultitoolItem;
import gregtech6.registry.GT6Tools;

public class PocketEightTest {

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

	// ------------------------------------------------------- the registration + tab parity

	/** The eight registration ids in POCKET_FORMS order = the :176-183 row order. */
	@Test
	public void theEightFormsRegisterInTheRingOrder() {
		assertEquals(8, GT6Tools.POCKET_FORMS.size(), "the eight :176-183 forms");
		String[] tIds = {"pocket_multitool", "pocket_multitool_knife", "pocket_multitool_saw", "pocket_multitool_file",
				"pocket_multitool_screwdriver", "pocket_multitool_wire_cutter", "pocket_multitool_scissors",
				"pocket_multitool_chisel"};
		for (int i = 0; i < 8; i++) {
			assertSame(GT6Tools.POCKET_FORMS.get(i), GT6Tools.TAB_TABLE.get(56 + i), "row " + (56 + i) + " rides the pocket band (the t8 armor rows took the tail after — the absolute pin is the immune form)");
			assertEquals(rl(tIds[i]), GT6Tools.POCKET_FORMS.get(i).getId(), "form " + i + " id");
		}
		// the TAB_TABLE parity: the pocket band is rows 56..63 (the absolute pin — the
		// t8 armor rows took the table tail after this card, the dynamic tail pin went
		// stale, the t3-machine-face immune-form ruling)
		for (int i = 0; i < 8; i++) {
			assertSame(GT6Tools.POCKET_FORMS.get(i), GT6Tools.TAB_TABLE.get(56 + i),
					"the pocket band mirrors the ring at offset " + i + " (row " + (56 + i) + ")");
		}
		assertEquals(88, GT6Tools.TAB_TABLE.size(), "the 64 prior rows + the 24 armor rows");
		assertEquals(512, GTPocketMultitoolItem.DURABILITY_POINTS, "the single-steel-tier family value (ruling d)");
	}

	// ------------------------------------------------------- the ring chain + the 64-cell table

	/** The next() chain pinned to the :176-183 constructor sequence, line by line. */
	@Test
	public void theRingChainIsTheUpstreamConstructorSequence() {
		assertEquals(GTPocketMultitoolItem.KNIFE, GTPocketMultitoolItem.next(GTPocketMultitoolItem.MULTITOOL), ":176 multitool -> knife");
		assertEquals(GTPocketMultitoolItem.SAW, GTPocketMultitoolItem.next(GTPocketMultitoolItem.KNIFE), ":177 knife -> saw");
		assertEquals(GTPocketMultitoolItem.FILE, GTPocketMultitoolItem.next(GTPocketMultitoolItem.SAW), ":178 saw -> file");
		assertEquals(GTPocketMultitoolItem.SCREWDRIVER, GTPocketMultitoolItem.next(GTPocketMultitoolItem.FILE), ":179 file -> screwdriver");
		assertEquals(GTPocketMultitoolItem.WIRE_CUTTER, GTPocketMultitoolItem.next(GTPocketMultitoolItem.SCREWDRIVER), ":180 screwdriver -> wire cutter");
		assertEquals(GTPocketMultitoolItem.SCISSORS, GTPocketMultitoolItem.next(GTPocketMultitoolItem.WIRE_CUTTER), ":181 wire cutter -> scissors");
		assertEquals(GTPocketMultitoolItem.CHISEL, GTPocketMultitoolItem.next(GTPocketMultitoolItem.SCISSORS), ":182 scissors -> chisel");
		assertEquals(GTPocketMultitoolItem.MULTITOOL, GTPocketMultitoolItem.next(GTPocketMultitoolItem.CHISEL), ":183 chisel -> multitool (the wrap)");
	}

	/**
	 * The :187-196 NEI-redirect loop, ported to the duality table: from EVERY form
	 * (8 rows), walking next() eight times (8 cells) enumerates all eight forms exactly
	 * once — 64 ordered assertions, the ring's group property.
	 */
	@Test
	public void theRingWalkEnumeratesAllFormsFromEveryStart() {
		for (int i = 0; i < 8; i++) {
			Set<Integer> tSeen = new HashSet<>();
			int tForm = i;
			for (int k = 0; k < 8; k++) {
				tSeen.add(tForm);
				assertEquals((i + k) % 8, tForm, "the walk from " + i + " at hop " + k + " is the (i+k)%8 table");
				tForm = GTPocketMultitoolItem.next(tForm);
			}
			assertEquals(8, tSeen.size(), "the walk from " + i + " covers every form exactly once");
			assertEquals(i, tForm, "the walk from " + i + " returns home after 8 hops");
		}
	}

	// ------------------------------------------------------- the switch carrier

	/**
	 * The ItemStack.setItem replacement (the class-javadoc ruling: the card evidence line
	 * failed verification, no same-instance item swap exists on either leg) — the switched
	 * stack carries the NEW item and the HELD stack's damage, byte for byte the
	 * "same slot, same damage value, new item id" semantics.
	 */
	@Test
	public void theSwitchCarrierPreservesDurabilityAndSwapsTheItem() {
		Item tNext = probeItem("pocket_switch_probe_next");
		ItemStack tHeld = new ItemStack(tNext);
		tHeld.setDamageValue(17);
		Item tTarget = probeItem("pocket_switch_probe_target");
		ItemStack tSwitched = GTPocketMultitoolItem.switchForm(tHeld, tTarget);
		assertSame(tTarget, tSwitched.getItem(), "the switched stack carries the target form's item");
		assertEquals(17, tSwitched.getDamageValue(), "the damage rides across the switch");
		assertEquals(17, tHeld.getDamageValue(), "the held stack is never mutated");
	}

	// ------------------------------------------------------- the per-form face table

	/** The classification table — each switch form classifies ONLY its twin's action; knife/scissors/multitool none. */
	@Test
	public void theClassificationTableMirrorsTheTwinActions() {
		assertTrue(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.SAW, GT6ToolActions.SAW));
		assertTrue(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.FILE, GT6ToolActions.FILE));
		assertTrue(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.SCREWDRIVER, GT6ToolActions.SCREWDRIVER));
		assertTrue(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.WIRE_CUTTER, GT6ToolActions.CUTTER));
		assertTrue(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.CHISEL, GT6ToolActions.CHISEL));
		// the never-actions (the crowbar red-line form): no form classifies on HOE_DIG
		for (int i = 0; i < 8; i++) {
			assertFalse(GTPocketMultitoolItem.classifies(i, ToolActions.HOE_DIG), "form " + i + " never rides the wrench substitute");
			assertFalse(GTPocketMultitoolItem.classifies(i, ToolActions.AXE_DIG), "form " + i + " never rides the axe");
		}
		// the closed form + the two no-action-yet forms
		assertFalse(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.MULTITOOL, GT6ToolActions.SAW), "the multitool classifies nothing (:176)");
		assertFalse(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.KNIFE, GT6ToolActions.SAW), "the knife has no action until the blade card");
		assertFalse(GTPocketMultitoolItem.classifies(GTPocketMultitoolItem.SCISSORS, GT6ToolActions.SAW), "the scissors have no action until t5");
	}

	/** The attack face table — knife 2.0, scissors 1.0, everything else 0 (the :56/:70 rows). */
	@Test
	public void theAttackFaceTableIsTheUpstreamBaseDamages() {
		assertEquals(2.0F, GTPocketMultitoolItem.attackDamageOf(GTPocketMultitoolItem.KNIFE));
		assertEquals(1.0F, GTPocketMultitoolItem.attackDamageOf(GTPocketMultitoolItem.SCISSORS));
		for (int i : new int[] {GTPocketMultitoolItem.MULTITOOL, GTPocketMultitoolItem.SAW, GTPocketMultitoolItem.FILE,
				GTPocketMultitoolItem.SCREWDRIVER, GTPocketMultitoolItem.WIRE_CUTTER, GTPocketMultitoolItem.CHISEL}) {
			assertEquals(0.0F, GTPocketMultitoolItem.attackDamageOf(i), "form " + i + " is no weapon");
		}
	}

	/** The mining surface — the saw's wood+ice set, the file's iron bars, nothing else mines. */
	@Test
	public void theMiningSurfaceTableIsTheUpstreamIsMinableHalves() {
		BlockState tIce = Blocks.ICE.defaultBlockState();
		BlockState tBars = Blocks.IRON_BARS.defaultBlockState();
		BlockState tStone = Blocks.STONE.defaultBlockState();
		// the saw face (GT_Tool_Saw.java:166-169 — wood family + "Can harvest Ice"). The
		// TAG arms (LOGS/PLANKS/LEAVES) need a live datapack — the RCON faces chain asserts
		// them at a setblock oak_log; the offline pins are the tag-free ICE arm + negatives.
		assertTrue(GTPocketMultitoolItem.mines(GTPocketMultitoolItem.SAW, tIce), "the Can-harvest-Ice row");
		assertFalse(GTPocketMultitoolItem.mines(GTPocketMultitoolItem.SAW, tStone), "saw does not mine stone");
		assertEquals(GTPocketMultitoolItem.MINING_SPEED, GTPocketMultitoolItem.destroySpeedBonus(GTPocketMultitoolItem.SAW, tIce), "the mineable surface digs at MINING_SPEED");
		// the file face (GT_Tool_File.java:66-70 + the :82-85 ×3 row on the bars block)
		assertTrue(GTPocketMultitoolItem.mines(GTPocketMultitoolItem.FILE, tBars));
		assertEquals(3.0F, GTPocketMultitoolItem.destroySpeedBonus(GTPocketMultitoolItem.FILE, tBars), "the ×3 iron-bars row folded");
		assertEquals(1.0F, GTPocketMultitoolItem.destroySpeedBonus(GTPocketMultitoolItem.FILE, tStone), "off-surface = the hand speed");
		// the glass panes are NOT the file's surface — 1.20.1 GLASS_PANE IS an IronBarsBlock
		// instance (Blocks.java:2563), the exact-block ruling in the class javadoc
		assertFalse(GTPocketMultitoolItem.mines(GTPocketMultitoolItem.FILE, Blocks.GLASS_PANE.defaultBlockState()), "glass pane is not iron bars");
		assertFalse(GTPocketMultitoolItem.mines(GTPocketMultitoolItem.FILE, Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState()), "stained pane is not iron bars");
		// the other five forms mine nothing
		for (int i : new int[] {GTPocketMultitoolItem.MULTITOOL, GTPocketMultitoolItem.KNIFE,
				GTPocketMultitoolItem.SCREWDRIVER, GTPocketMultitoolItem.WIRE_CUTTER, GTPocketMultitoolItem.SCISSORS,
				GTPocketMultitoolItem.CHISEL}) {
			assertFalse(GTPocketMultitoolItem.mines(i, tBars), "form " + i + " does not mine bars");
			assertFalse(GTPocketMultitoolItem.mines(i, tStone), "form " + i + " does not mine stone");
		}
		// the multitool form: no attack, no mining, no classification — the closed form
		assertEquals(0.0F, GTPocketMultitoolItem.attackDamageOf(GTPocketMultitoolItem.MULTITOOL));
	}

	// ------------------------------------------------------- the no-battery recipe pin

	/**
	 * The reversal-ruling pin (acceptance: the :354 ingredients carry no re_battery tag):
	 * the committed pocket recipe JSON has NO battery key — no ingredient id or tag
	 * contains "battery", the ten keys are exactly the :354 letter fold, and the result
	 * is the closed form.
	 */
	@Test
	public void thePocketRecipeCarriesNoBatterySlot() throws Exception {
		JsonObject tRow = generated("pocket_multitool");
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString());
		JsonArray tPattern = tRow.getAsJsonArray("pattern");
		assertEquals("AXO", tPattern.get(0).getAsString(), "the :354 row 1");
		assertEquals("ZPV", tPattern.get(1).getAsString(), "the :354 row 2");
		assertEquals("OWY", tPattern.get(2).getAsString(), "the :354 row 3");
		JsonObject tKey = tRow.getAsJsonObject("key");
		assertEquals(8, tKey.size(), "the eight distinct :354 letters (the O ring fills two cells)");
		Set<String> tLetters = new HashSet<>();
		for (var tEntry : tKey.entrySet()) {
			tLetters.add(tEntry.getKey());
			String tMember = tEntry.getValue().getAsJsonObject().get("item") != null
					? tEntry.getValue().getAsJsonObject().get("item").getAsString()
					: tEntry.getValue().getAsJsonObject().get("tag").getAsString();
			assertFalse(tMember.contains("battery"), "NO battery slot — the reversal ruling (letter " + tEntry.getKey() + " = " + tMember + ")");
			assertFalse(tMember.contains("re_battery"), "NO re-battery tag (letter " + tEntry.getKey() + " = " + tMember + ")");
		}
		assertEquals(Set.of("A", "X", "O", "Z", "P", "V", "W", "Y"), tLetters,
				"the upstream letter alphabet verbatim (the O ring appears twice in the grid, one key)");
		assertEquals("gt6:pocket_multitool", tRow.getAsJsonObject("result").get("item").getAsString(), "the closed form is the crafted one");
		// the steel convergence: the P plate letter keys the steel plate tag (ruling d)
		assertEquals("forge:plates/steel", tKey.getAsJsonObject("P").get("tag").getAsString(), "the P plate letter = plates/steel (the wrench-row key)");
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = PocketEightTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/**
	 * The probe-item seam (the BendingCylinderSmallTest form, the GTWireBlockUseLockTest
	 * reflection bracket): the offline switch-carrier test needs two registered items, and
	 * the mod-Item intrusive-holder wall forces the registry unfreeze window. This file's
	 * copy is form-agnostic (plain Items, not pocket items — switchForm is item-agnostic).
	 */
	//? if forge {
	private static Item probeItem(String aProbeId) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		try {
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
			// the durability() properties face: the 21.1 damage is a data COMPONENT keyed on
			// max_damage — a bare Item cannot carry the axis (the BendingCylinderSmallTest form)
			Item rItem = new Item(new Item.Properties().durability(512));
			net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
			return rItem;
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry", aE);
		}
	}

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
	//?} else {
	/*private static Item probeItem(String aProbeId) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			// the durability() properties face: the 21.1 damage is a data COMPONENT keyed on
			// max_damage — a bare Item cannot carry the axis (the BendingCylinderSmallTest form)
			Item rItem = new Item(new Item.Properties().durability(512));
			net.minecraft.core.Registry.register(tRegistry, net.minecraft.resources.ResourceLocation.parse("gt6:" + aProbeId), rItem);
			return rItem;
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry", aE);
		}
	}
	*///?}
}
