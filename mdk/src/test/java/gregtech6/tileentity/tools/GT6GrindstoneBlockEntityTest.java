package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;

import gregtech6.block.tools.GT6GrindstoneBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GT6Grindstones;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The grindstone BE offline tests (task grindstone-family acceptance): the sand ladder
 * (MultiTileEntityGrindStone.java:99-130), the 10-combo sharpen + the mStone spend
 * (:145-165), the enchant strip XP face (:147-153 + UT.NBT.getEnchantmentXP :2356-2371),
 * the creative instant fold (:131-144), the corner NEI gate (:93/:168-173) and the
 * registration row (:2226).
 */
public class GT6GrindstoneBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6GrindstoneBlockEntity> sGrindstoneType;
	/** The offline block instance (the GT6GrindstoneNeiModelTest recipe — registration pins). */
	static GT6GrindstoneBlock sGrindstoneBlock;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** The fixture BET (the GT6AnvilBlockEntityTest self-referencing form). */
	@BeforeAll
	static void buildOfflineFixture() {
		// the BLOCK registry write window (the GT6GrindstoneNeiModelTest recipe — the
		// Block ctor's createIntrusiveHolder needs the registry unfrozen; a sibling
		// fixture may have unfrozen it already, the catch is the idempotence)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		sGrindstoneBlock = new GT6GrindstoneBlock(() -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6GrindstoneBlockEntity>[] tHolder = (BlockEntityType<GT6GrindstoneBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6GrindstoneBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sGrindstoneType = tHolder[0];
	}

	/** A fresh generation per test — the fixture rows below are the only writers. */
	@BeforeEach
	void freshMaps() {
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		// the SHARPENING fixture row: 1 flint → 1 iron nugget, eUt 16 x duration 16 (the
		// upstream :520 flint row shape — the row the combo clicks execute)
		GT6RecipeMaps.SHARPENING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.FLINT, 1)},
				new ItemStack[] {new ItemStack(Items.IRON_NUGGET, 1)},
				new FluidStack[0], new FluidStack[0], 16, 16, 0));
	}

	@AfterEach
	void teardown() {
		GT6RecipeMaps.reset();
	}

	/** A fresh offline grindstone (empty — the vanilla carrier has no STONE property, stone() = 0). */
	private static GT6GrindstoneBlockEntity grindstone() {
		return new GT6GrindstoneBlockEntity(sGrindstoneType, POS, Blocks.STONE.defaultBlockState());
	}

	/** A loaded grindstone (the field write — the offline stand-in for the sand ladder). */
	private static GT6GrindstoneBlockEntity loadedGrindstone(int aStone) {
		GT6GrindstoneBlockEntity tGrindstone = grindstone();
		tGrindstone.mStone = aStone;
		return tGrindstone;
	}

	/** The top-face click (side 1, mid-face — outside the corner region). */
	private static String click(GT6GrindstoneBlockEntity tGrindstone, Player aPlayer, ItemStack aHeld) {
		return tGrindstone.activateChain(aPlayer, (byte) 1, aHeld, 0.5F, 0.5F, 0.5F);
	}

	// ---------------------------------------------------------------------------
	// the sand ladder (:99-130)
	// ---------------------------------------------------------------------------

	@Test
	public void sandLadderLoadsTheUpstreamLevels() {
		assertEquals(16, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.SOUL_SAND)), ":100 Soulsand 16");
		assertEquals(8, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.RED_SAND)), ":115 RedSand 8");
		assertEquals(4, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.SAND)), ":120 Sand 4");
		assertEquals(8, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.SANDSTONE)), ":125 OD.sandstone 8");
		assertEquals(8, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.RED_SANDSTONE)), ":125 the sandstone pair");
		assertEquals(0, GT6GrindstoneBlockEntity.abrasiveLoad(new ItemStack(Items.DIRT)), "a non-abrasive loads nothing");
		assertEquals(0, GT6GrindstoneBlockEntity.abrasiveLoad(ItemStack.EMPTY));
	}

	@Test
	public void loadingConsumesOneAbrasiveAndResetsTheCombo() {
		GT6GrindstoneBlockEntity tGrindstone = grindstone();
		Player tPlayer = grindPlayer(false);
		ItemStack tSand = new ItemStack(Items.SAND, 5);
		assertTrue(click(tGrindstone, tPlayer, tSand).startsWith("loaded"), "the sand arm loads");
		assertEquals(4, tGrindstone.stone(), ":123 — Sand loads 4");
		assertEquals(4, tSand.getCount(), "ST.use — exactly one abrasive consumed");
		assertEquals(0, tGrindstone.clickCount(), "the load resets the combo");
	}

	@Test
	public void creativeLoadingSpendsNothing() {
		GT6GrindstoneBlockEntity tGrindstone = grindstone();
		Player tPlayer = grindPlayer(true); // instabuild — ST.use keeps the stack
		ItemStack tSoul = new ItemStack(Items.SOUL_SAND, 3);
		assertTrue(click(tGrindstone, tPlayer, tSoul).startsWith("loaded"));
		assertEquals(16, tGrindstone.stone(), ":103 — Soulsand loads 16");
		assertEquals(3, tSoul.getCount(), "creative pays nothing");
	}

	@Test
	public void anEmptyHandResetsTheCombo() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		assertTrue(click(tGrindstone, null, ItemStack.EMPTY).startsWith("hold the item"));
		assertEquals(0, tGrindstone.clickCount(), ":97 — the empty hand resets");
	}

	// ---------------------------------------------------------------------------
	// the 10-combo sharpen + the mStone spend (:145-165)
	// ---------------------------------------------------------------------------

	@Test
	public void tenClickComboSharpensAndSpendsOneAbrasive() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		Player tPlayer = grindPlayer(false);
		ItemStack tFlint = new ItemStack(Items.FLINT, 3);
		for (int i = 1; i < 10; i++) {
			assertTrue(click(tGrindstone, tPlayer, tFlint).startsWith("grinding (" + i + "/"), "click " + i + " walks the combo");
		}
		assertEquals(16, tGrindstone.stone(), "no spend inside the combo");
		assertEquals(3, tFlint.getCount(), "no input pay inside the combo");
		assertTrue(click(tGrindstone, tPlayer, tFlint).startsWith("sharpened"), "the 10th click sharpens");
		assertEquals(15, tGrindstone.stone(), ":161 — mStone-- per sharpen");
		assertEquals(2, tFlint.getCount(), ":158 — the input paid");
		assertEquals(1, countIn(tPlayer, Items.IRON_NUGGET), "the output went to the bag");
	}

	@Test
	public void theStoneSpendsToZeroAndRefuses() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(4);
		Player tPlayer = grindPlayer(false);
		ItemStack tFlint = new ItemStack(Items.FLINT, 16);
		for (int i = 0; i < 4 * 10; i++) click(tGrindstone, tPlayer, tFlint); // four full combos
		assertEquals(0, tGrindstone.stone(), "4 sharpen = 4 units — spent to zero");
		assertTrue(click(tGrindstone, tPlayer, tFlint).startsWith("the grindstone is empty"), "the spent stone refuses");
	}

	@Test
	public void aNonRecipeHeldStackResetsNothingButPaysNothing() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(8);
		Player tPlayer = grindPlayer(false);
		ItemStack tDirt = new ItemStack(Items.DIRT, 1);
		assertTrue(click(tGrindstone, tPlayer, tDirt).startsWith("grinding (1/"), "a non-recipe click still walks the combo");
		assertEquals(1, tDirt.getCount(), "nothing pays on a miss");
		assertEquals(8, tGrindstone.stone(), "no spend on a miss");
	}

	// ---------------------------------------------------------------------------
	// the enchant strip face (:147-153 + UT.NBT.getEnchantmentXP :2356-2371)
	// ---------------------------------------------------------------------------

	/**
	 * The enchant fixtures ride the FORGE leg: 1.21 made enchantments data-driven (no
	 * offline BuiltInRegistries handle for a Holder, the ItemLatch assume-skip convention).
	 * The strip SEMANTICS are proven here; the 21.1 face is the main source's chisel branch
	 * (compile-covered), its live math is version-identical logic.
	 */
	private static boolean enchantFixturesArmed() {
		//? if forge {
		return true;
		//?} else {
		/*return false;
		 *///?}
	}

	/** aKind: 0 = SHARPNESS 3 (the XP payer), 1 = BINDING_CURSE 1 (the strip killer). */
	private static ItemStack enchanted(ItemStack aStack, int aKind, int aLevel) {
		//? if forge {
		net.minecraft.world.item.enchantment.Enchantment tEnch = aKind == 0
				? net.minecraft.world.item.enchantment.Enchantments.SHARPNESS
				: net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE;
		EnchantmentHelper.setEnchantments(Map.of(tEnch, aLevel), aStack);
		return aStack;
		//?} else {
		/*throw new IllegalStateException("the enchant fixtures ride the forge leg");
		 *///?}
	}

	/** The sharpness-3 min cost (the XP math anchor). */
	private static int sharpnessMinCost3() {
		//? if forge {
		return net.minecraft.world.item.enchantment.Enchantments.SHARPNESS.getMinCost(3);
		//?} else {
		/*throw new IllegalStateException("the enchant fixtures ride the forge leg");
		 *///?}
	}

	private static ItemStack enchantedSword() {
		return enchanted(new ItemStack(Items.DIAMOND_SWORD), 0, 3);
	}

	@Test
	public void enchantmentXPPinsTheUpstreamMath() {
		org.junit.jupiter.api.Assumptions.assumeTrue(enchantFixturesArmed());
		ItemStack tSword = enchantedSword();
		int tMinCost = sharpnessMinCost3();
		assertEquals((tMinCost + 1) / 2, GT6GrindstoneBlockEntity.enchantmentXP(tSword), ":2368-2370 — the min-cost half");
		assertTrue(GT6GrindstoneBlockEntity.enchantmentXP(tSword) > 0);
		assertEquals(0, GT6GrindstoneBlockEntity.enchantmentXP(new ItemStack(Items.DIAMOND_SWORD)), "unenchanted = 0");

		ItemStack tCursed = enchanted(new ItemStack(Items.IRON_HELMET), 1, 1);
		assertEquals(0, GT6GrindstoneBlockEntity.enchantmentXP(tCursed), ":2367 — the curse kills the strip");
	}

	@Test
	public void removeEnchantmentsStripsTheTwelveKey() {
		org.junit.jupiter.api.Assumptions.assumeTrue(enchantFixturesArmed());
		ItemStack tStripped = GT6GrindstoneBlockEntity.removeEnchantments(enchantedSword());
		assertFalse(tStripped.isEnchanted(), "the strip clears the enchantments");
		assertEquals(Items.DIAMOND_SWORD, tStripped.getItem(), "the twin keeps the item");
		assertEquals(0, GT6GrindstoneBlockEntity.enchantmentXP(tStripped));
	}

	@Test
	public void theComboStripsTheEnchantmentsIntoAnXPOrb() {
		org.junit.jupiter.api.Assumptions.assumeTrue(enchantFixturesArmed());
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		Player tPlayer = grindPlayer(false);
		ItemStack tSword = enchantedSword(); // count 1
		for (int i = 0; i < 9; i++) click(tGrindstone, tPlayer, tSword);
		assertTrue(click(tGrindstone, tPlayer, tSword).startsWith("stripped"), "the 10th click strips");
		assertEquals(0, tSword.getCount(), ":150 — the enchanted piece paid");
		assertEquals(1, countIn(tPlayer, Items.DIAMOND_SWORD), ":151 — the unenchanted twin into the bag");
		assertEquals(0.5F, tPlayer.getFoodData().getExhaustionLevel(), 1e-6F, ":153 — the strip exhaustion");
		assertEquals(16, tGrindstone.stone(), "the strip spends NO abrasive (upstream verbatim)");
	}

	// ---------------------------------------------------------------------------
	// the creative instant fold (:131-144)
	// ---------------------------------------------------------------------------

	@Test
	public void creativeSharpensInstantlyAndPaysNothing() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		Player tPlayer = grindPlayer(true);
		ItemStack tFlint = new ItemStack(Items.FLINT, 2);
		assertTrue(click(tGrindstone, tPlayer, tFlint).startsWith("sharpened (creative instant"), ":131 — no combo");
		assertEquals(16, tGrindstone.stone(), "creative spends no abrasive");
		assertEquals(2, tFlint.getCount(), ":140 — the probe pays nothing");
		assertEquals(1, countIn(tPlayer, Items.IRON_NUGGET), "the free output");
		assertEquals(0.0F, tPlayer.getFoodData().getExhaustionLevel(), 1e-6F, "no exhaust on the creative fold");
	}

	@Test
	public void creativeStripsInstantly() {
		org.junit.jupiter.api.Assumptions.assumeTrue(enchantFixturesArmed());
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		Player tPlayer = grindPlayer(true);
		ItemStack tSword = enchantedSword();
		assertTrue(click(tGrindstone, tPlayer, tSword).startsWith("stripped"), ":132 — the instant strip");
		assertEquals(1, countIn(tPlayer, Items.DIAMOND_SWORD), ":134 — the stripped twin");
	}

	// ---------------------------------------------------------------------------
	// the corner NEI gate (:93 server eat / :168-173 client jump)
	// ---------------------------------------------------------------------------

	/** Captures the viewer jump and flags the BE client-side (the GT6AnvilNeiCornerTest form). */
	static class ClientGrindstone extends GT6GrindstoneBlockEntity {
		int tOpened;
		ClientGrindstone() { super(sGrindstoneType, POS, Blocks.STONE.defaultBlockState()); }
		@Override protected void openNei() { tOpened++; }
		@Override public boolean isServerSide() { return false; }
	}

	@Test
	public void cornerThresholdsFollowTheFacingAxis() {
		// the top-face [hitX, hitZ] mapping (UT.Code.getFacingCoordsClicked :1737)
		assertTrue(GT6GrindstoneBlockEntity.isCorner(8.0F / 16.0F, 4.0F / 16.0F, Direction.NORTH), "Z-facing: the 8x4 corner is inclusive");
		assertFalse(GT6GrindstoneBlockEntity.isCorner(8.0F / 16.0F + 1e-6F, 0.0F, Direction.NORTH), "past 8px on X leaves");
		assertFalse(GT6GrindstoneBlockEntity.isCorner(0.0F, 4.0F / 16.0F + 1e-6F, Direction.NORTH), "past 4px on Z leaves");
		assertTrue(GT6GrindstoneBlockEntity.isCorner(4.0F / 16.0F, 8.0F / 16.0F, Direction.EAST), "X-facing: the 4x8 corner is inclusive");
		assertFalse(GT6GrindstoneBlockEntity.isCorner(0.5F, 0.5F, Direction.NORTH), "the mid-face click is no corner");
	}

	@Test
	public void theClientCornerArmJumpsOnlyWhenLoaded() {
		ClientGrindstone tGrindstone = new ClientGrindstone();
		tGrindstone.mStone = 16;
		tGrindstone.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.3F, 0.9F, 0.1F);
		assertEquals(1, tGrindstone.tOpened, ":168-173 — loaded + top corner → the viewer jump");

		ClientGrindstone tEmpty = new ClientGrindstone(); // mStone = 0 — the gate
		tEmpty.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.3F, 0.9F, 0.1F);
		assertEquals(0, tEmpty.tOpened, ":168 — the EMPTY stone shows no glyph and jumps nothing");

		ClientGrindstone tMid = new ClientGrindstone();
		tMid.mStone = 16;
		tMid.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.5F, 0.9F, 0.5F);
		assertEquals(0, tMid.tOpened, "the mid-face click is no corner (the work arm's face)");

		ClientGrindstone tFront = new ClientGrindstone();
		tFront.mStone = 16;
		tFront.activateChain(null, (byte) 2, ItemStack.EMPTY, 0.3F, 0.9F, 0.1F); // NORTH front = side 2
		assertEquals(0, tFront.tOpened, "the corner arm is top-face only");
	}

	@Test
	public void theServerCornerClickIsEatenWithoutAction() {
		GT6GrindstoneBlockEntity tGrindstone = loadedGrindstone(16);
		assertTrue(tGrindstone.activateChain(null, (byte) 1, ItemStack.EMPTY, 0.3F, 0.9F, 0.1F)
				.startsWith("the recipe-viewer corner"), ":93 — the server eats the corner");
		// the non-corner side passes the guard refusals through
		assertTrue(tGrindstone.activateChain(null, (byte) 3, ItemStack.EMPTY, 0.5F, 0.5F, 0.5F)
				.startsWith("strike the top or the front"), ":89 — a wrong-side click refuses");
	}

	// ---------------------------------------------------------------------------
	// the registration row (:2226) + the item NBT convention
	// ---------------------------------------------------------------------------

	@Test
	public void registrationRowPinsTheLoaderAnchors() {
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE.getId().getPath(), "the :2226 row id");
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE_BE.getId().getPath(), "the BET id");
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE_ITEM.getId().getPath(), "the BlockItem id");
		// the state-definition faces ride the offline block instance (the DeferredRegister
		// stays unbound in the test JVM — the .get() faces are the live-registry domain)
		BlockState tDefault = sGrindstoneBlock.defaultBlockState();
		assertEquals(Direction.NORTH, tDefault.getValue(GT6GrindstoneBlock.FACING), "the 09FacingSingle SIDE_FRONT default");
		assertEquals(0, tDefault.getValue(GT6GrindstoneBlock.STONE), "the empty placement default");
		assertEquals(68, sGrindstoneBlock.getStateDefinition().getPossibleStates().size(),
				"4 FACING x 17 STONE states");
		assertEquals(7, GT6GrindstoneBlockEntity.ABRASIVES.size(), "the :100-125 arm order — 7 rows");
		assertEquals("gt.toolstate", GT6GrindstoneBlock.NBT_TOOLSTATE, "the writeItemNBT2 carrier key");
	}

	@Test
	public void theItemNbtRoundTripConventionPins() {
		// the placement read (getStateForPlacement rides gt.toolstate) + the drop write
		// (getDrops rides the BE field) are live faces — offline pins the TAG SHAPE:
		CompoundTag tTag = new CompoundTag();
		tTag.putByte(GT6GrindstoneBlock.NBT_TOOLSTATE, (byte) 12);
		assertEquals(12, tTag.getByte(GT6GrindstoneBlock.NBT_TOOLSTATE));
	}

	// ---------------------------------------------------------------------------
	// the offline player double (the GT6AnvilBlockEntityTest.BagPlayer form, slimmed)
	// ---------------------------------------------------------------------------

	/** A bag player with a real Inventory + FoodData + an abilities flag (the Unsafe-allocation form). */
	private static Player grindPlayer(boolean aCreative) {
		try {
			java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tTheUnsafe.setAccessible(true);
			sun.misc.Unsafe tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
			Player tPlayer = (Player) tUnsafe.allocateInstance(OfflinePlayer.class);
			java.lang.reflect.Field tInventoryField = Player.class.getDeclaredField("inventory");
			tInventoryField.setAccessible(true);
			tInventoryField.set(tPlayer, new Inventory(null));
			java.lang.reflect.Field tFoodField = Player.class.getDeclaredField("foodData");
			tFoodField.setAccessible(true);
			tFoodField.set(tPlayer, new FoodData());
			java.lang.reflect.Field tAbilitiesField = Player.class.getDeclaredField("abilities");
			tAbilitiesField.setAccessible(true);
			net.minecraft.world.entity.player.Abilities tAbilities = new net.minecraft.world.entity.player.Abilities();
			tAbilities.invulnerable = true;
			tAbilities.instabuild = aCreative;
			tAbilitiesField.set(tPlayer, tAbilities);
			return tPlayer;
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("the offline grind player failed", aE);
		}
	}

	/** The abstract-free double (isSpectator/isCreative answer off the abilities flag). */
	private static final class OfflinePlayer extends Player {
		private OfflinePlayer() { super(null, null, 0.0F, null); } // never runs — the Unsafe allocation form
		@Override public boolean isSpectator() { return false; }
		@Override public boolean isCreative() { return false; }
		/**
		 * The exhaustion seam offline (the anvil fixture's invulnerable-TRUE form blocks the
		 * vanilla path — Player.java:1689-1694 reads invulnerable FIRST, then the null
		 * level()): the BE contract under test is the CALL (0.5 on strip, power/10000 on
		 * sharpen), routed straight into the injected FoodData.
		 */
		@Override public void causeFoodExhaustion(float aExhaustion) {
			getFoodData().addExhaustion(aExhaustion);
		}
	}

	private static int countIn(Player aPlayer, net.minecraft.world.item.Item aItem) {
		int rTotal = 0;
		for (int i = 0, n = aPlayer.getInventory().items.size(); i < n; i++) {
			ItemStack tSlot = aPlayer.getInventory().items.get(i);
			if (!tSlot.isEmpty() && tSlot.getItem() == aItem) rTotal += tSlot.getCount();
		}
		return rTotal;
	}
}
