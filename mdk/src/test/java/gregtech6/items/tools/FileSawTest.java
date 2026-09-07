/**
 * Offline tests for task p24-tool-system: the file + saw pair (the crafting-tool
 * items), their ToolAction/dispatch-id pins, the crafting-loss seam and the tag
 * face the recipe provider consumes.
 *
 * <p>Assertion surface notes (the CutterTest/GT6ToolsCreativeTabTest constraints):
 * a mod Item cannot be constructed in this bootstrapped-and-frozen JVM (the
 * Item.java:61 intrusive-holder wall), so the classification and loss faces pin
 * through the STATIC seams ({@link GT6FileItem#classifies},
 * {@link GT6FileItem#craftRemaining}) and the live getCraftingRemainingItem override
 * rides the runServer/runData gates. The loss-seam behaviour is exercised over a
 * VANILLA damageable stack (Items.IRON_PICKAXE) — the seam is item-agnostic on
 * purpose so the boundary is offline-provable without the registry.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
}
