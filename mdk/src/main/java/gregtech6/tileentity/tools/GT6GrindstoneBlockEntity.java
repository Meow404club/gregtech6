/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.tileentity.tools;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.block.tools.GT6GrindstoneBlock;
import gregtech6.gui.GTViewerJump;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Grindstone — the zero-energy manual sharpening block (task grindstone-family), the
 * port of the upstream {@code MultiTileEntityGrindStone} (tmp/gt6-1.7.10 .../tools/
 * MultiTileEntityGrindStone.java:52-268), the RM.Sharpening manual consumer. NO GUI, no
 * inventory: the working material is the PLAYER'S HELD STACK (the upstream
 * {@code getCurrentEquippedItem} face), the abrasive lives in the blockstate
 * ({@link GT6GrindstoneBlock#STONE} — the upstream mStone visual-data channel, the carrier
 * class doc). The BE itself holds only the transient 10-combo counter + the last-recipe
 * cache (the upstream fields :53-55 — neither persisted upstream, readFromNBT2 :58-62
 * carries only NBT_STATE; there is no saveAdditional here on purpose).
 *
 * <h2>The sand ladder (:99-130)</h2>
 * An empty grindstone (mStone 0) CONSUMES one held abrasive: Soulsand / White-Black End
 * Sand = 16, Red Sand = 8, Sand = 4, sandstone = 8. The port carriers follow the vanilla
 * item identity (the mortar.json vanilla-input convention): {@code minecraft:soul_sand} /
 * {@code minecraft:red_sand} / {@code minecraft:sand} + the sandstone pair, and — the two
 * DECLARED DEVIATIONS — the End-sand rows ride {@code gt6:dust_white_end_sand} /
 * {@code gt6:dust_black_end_sand}, the materials' ONLY port items (upstream carrier = the
 * OP.stone oredict face of the GT6 end-sand stones, LoaderOreDictReRegistrations.java:1509;
 * no such block exists in the port).
 *
 * <h2>The 10-combo work arm (:145-165)</h2>
 * A loaded grindstone + held stack + 10 consecutive valid clicks ({@code mClickCount}):
 * an enchanted stack STRIPS (the enchantments become an XP orb at the stone — upstream
 * {@code getEnchantmentXP} = sum of {@code getMinEnchantability(level)} halved, any curse
 * kills the strip :2360-2371), anything else runs ONE RM.Sharpening row: the inputs pay
 * ({@code isRecipeInputEqual(T, F, ...)}), the outputs go to the player, the player
 * exhausts {@code totalPower / 10000} and mStone drops by exactly 1 (0 = spent).
 * CREATIVE folds to INSTANT (:131-144): the strip/orb fires per click with no combo, and
 * the sharpen probe {@code isRecipeInputEqual(F, T, ...)} pays NOTHING (upstream verbatim —
 * free outputs, input preserved, no mStone cost, no exhaust).
 *
 * <p>Port deviations (declared): the haste/fatigue entity-stat modifiers on the combo
 * fold to the identity (the anvil fold precedent, GT6AnvilBlockEntity class doc — the
 * fatigue face the port carries is the food exhaustion half); {@code SFX.MC_DIG_SAND}
 * folds onto {@code SoundEvents.SAND_BREAK} (the :177 TODO-SOUND face, server broadcast);
 * the recipe-map NBT override ({@code NBT_RECIPEMAP :61}) folds onto the fixed RM.Sharpening
 * (the :54 field initializer — no registration row carries the key).
 *
 * <p>The {@code aPlayer == null} arm is the RCON/offline channel (the anvil
 * {@code activateChain} precedent): every arm answers a human-readable report; the sand
 * ladder needs a hand (there is no stack to consume without one).
 *
 * <p>The NEI corner jump (task manual-nei-four-family family form): the upstream CLIENT
 * arm opened {@code RM.Sharpening}'s NEI page from the top-face corner region —
 * {@code tCoords[0] <= PX_P[8 or 4] && tCoords[1] <= PX_P[4 or 8]} (:93/:170, the
 * getFacingCoordsClicked [hitX, hitZ] top-face mapping UT.java:1737) — GATED on mStone
 * != 0 (the empty stone shows no glyph). The port routes it through {@link #openNei} →
 * {@code GTViewerJump.openRecipeMapPage}; the GT6GrindstoneNeiModel sibling paints the
 * matching corner glyph (the same STONE gate).
 */
public class GT6GrindstoneBlockEntity extends TileEntityBase03TicksAndSync {

	/** The combo length — upstream {@code ++mClickCount >= 10} (:145). */
	public static final int COMBO_LENGTH = 10;

	/** The strip exhaustion — upstream {@code UT.Entities.exhaust(aPlayer, 0.5F)} (:153). */
	public static final float STRIP_EXHAUSTION = 0.5F;

	/** The sharpen exhaustion divisor — upstream {@code totalPower / 10000.0F} (:160). */
	public static final float EXHAUST_DIVISOR = 10000.0F;

	/** The findRecipe stack-size argument — the anvil {@code RECIPE_SIZE} (V[1] = 32) form. */
	public static final long RECIPE_SIZE = 32;

	/** The shared no-fluids argument (the anvil {@code new FluidStack[0]} call-site form, hoisted). */
	static final FluidStack[] EMPTY_FLUIDS = new FluidStack[0];

	/** The abrasive ladder — one row per upstream arm, in the :100-125 check order. */
	public static final List<AbrasiveRow> ABRASIVES = List.of(
			new AbrasiveRow("soul_sand", 16, () -> new ItemStack(Items.SOUL_SAND)),
			new AbrasiveRow("white_end_sand", 16, () -> materialItem(OP.dust, MT.EndSandWhite)),
			new AbrasiveRow("black_end_sand", 16, () -> materialItem(OP.dust, MT.EndSandBlack)),
			new AbrasiveRow("red_sand", 8, () -> new ItemStack(Items.RED_SAND)),
			new AbrasiveRow("sand", 4, () -> new ItemStack(Items.SAND)),
			new AbrasiveRow("sandstone", 8, () -> new ItemStack(Items.SANDSTONE)),
			new AbrasiveRow("red_sandstone", 8, () -> new ItemStack(Items.RED_SANDSTONE)));

	/**
	 * One abrasive row: the load value + the carrier probe. The probe yields a SAMPLE stack
	 * (identity-matched against the held stack — the registry is consulted lazily per
	 * click, never at class-load time; unbound pairs resolve EMPTY and drop out).
	 */
	public record AbrasiveRow(String name, int stone, java.util.function.Supplier<ItemStack> sample) {
		/** True when the held stack IS this row's carrier. */
		public boolean matches(@Nullable ItemStack aHeld) {
			if (aHeld == null || aHeld.isEmpty()) return false;
			ItemStack tSample = sample.get();
			return !tSample.isEmpty() && ItemStack.isSameItemSameTags(tSample, aHeld);
		}
	}

	/** The GT material-item sample (safe resolve — null/unbound pairs answer EMPTY, the mat() convention). */
	private static ItemStack materialItem(gregapi.oredict.OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aMaterial) {
		//? if forge {
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle =
				gregtech6.registry.GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle != null && tHandle.isPresent() ? new ItemStack(tHandle.get()) : ItemStack.EMPTY;
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> tHandle =
				gregtech6.registry.GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle != null && tHandle.isBound() ? new ItemStack(tHandle.get()) : ItemStack.EMPTY;
		 *///?}
	}

	/** The 10-combo counter (the upstream mClickCount :53 — transient, not persisted). */
	protected byte mClickCount = 0;

	/** The last-recipe cache (the upstream mLastRecipe :55 — the findRecipe fast path). */
	@Nullable
	protected Recipe mLastRecipe = null;

	/**
	 * The live abrasive count (the upstream mStone :53). The BE field is the working truth
	 * (the offline-testable anvil-mDurability form); the blockstate STONE property is the
	 * VISUAL MIRROR the write-through pushes (the carrier class doc) — the ctor seeds it
	 * from the placed state, the chunk load re-seeds the same way.
	 */
	protected int mStone;

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6GrindstoneBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GT6AnvilBlockEntity null-type form). */
	public GT6GrindstoneBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : gregtech6.registry.GT6Grindstones.GRINDSTONE_BE.get(), aPos, aState);
		mStone = GT6GrindstoneBlock.stoneOf(aState); // the placed abrasive (the writeItemNBT2 round trip)
	}

	/** The live abrasive count (the upstream mStone read — the :99 empty gate + the :161 decrement). */
	public int stone() {
		return mStone;
	}

	/** The combo counter read (the test/report seam — the upstream field is the BE's own). */
	public int clickCount() {
		return mClickCount;
	}

	// ---------------------------------------------------------------------------
	// the abrasive ladder (upstream :99-130)
	// ---------------------------------------------------------------------------

	/** The held stack's load value, or 0 when it is no abrasive (the :100-125 chain). */
	public static int abrasiveLoad(@Nullable ItemStack aHeld) {
		for (AbrasiveRow tRow : ABRASIVES) if (tRow.matches(aHeld)) return tRow.stone();
		return 0;
	}

	// ---------------------------------------------------------------------------
	// the enchantment XP face (upstream UT.NBT.getEnchantmentXP :2356-2371)
	// ---------------------------------------------------------------------------

	/**
	 * The strip value of a stack: sum of {@code getMinCost(level)} over the enchantments,
	 * halved (divup); ANY curse zeroes the whole strip (the upstream class-name check folds
	 * onto {@code isCurse()}, the 1.20.1 curse identity). Enchanted BOOKS keep their
	 * {@code StoredEnchantments} untouched — the upstream {@code "ench"} key check missed
	 * them too (the faithful fold).
	 */
	public static int enchantmentXP(@Nullable ItemStack aStack) {
		if (aStack == null || aStack.isEmpty() || !aStack.isEnchanted()) return 0;
		int rXP = 0;
		//? if forge {
		Map<Enchantment, Integer> tEnchantments = EnchantmentHelper.getEnchantments(aStack);
		if (tEnchantments.isEmpty()) return 0;
		for (Map.Entry<Enchantment, Integer> tEntry : tEnchantments.entrySet()) {
			if (tEntry.getKey().isCurse()) return 0; // :2367 — the curse kill
			rXP += tEntry.getKey().getMinCost(tEntry.getValue()); // :2368
		}
		//?} else {
		/*net.minecraft.world.item.enchantment.ItemEnchantments tEnchantments =
				aStack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
		if (tEnchantments == null || tEnchantments.isEmpty()) return 0;
		//21.1: the enchantment map keys went Holder (the GT6ToolLootModifiers holderOrThrow note);
		//the isCurse() method died with the data-driven enchantments — the curse face is the #minecraft:curse tag
		for (Map.Entry<net.minecraft.core.Holder<Enchantment>, Integer> tEntry : tEnchantments.entrySet()) {
			if (tEntry.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE)) return 0;
			rXP += tEntry.getKey().value().getMinCost(tEntry.getValue());
		}
		*///?}
		return (rXP + 1) / 2; // :2370 divup 2
	}

	/** The strip write-back — the unenchanted twin of the held stack (upstream removeEnchantments :2372-2378). */
	public static ItemStack removeEnchantments(@Nullable ItemStack aStack) {
		ItemStack rStack = aStack == null ? ItemStack.EMPTY : aStack.copy();
		//? if forge {
		if (!rStack.isEmpty() && rStack.getTag() != null) rStack.getTag().remove("Enchantments");
		//?} else {
		/*if (!rStack.isEmpty()) rStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
				net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		 *///?}
		return rStack;
	}

	// ---------------------------------------------------------------------------
	// the activation chain (upstream onBlockActivated3 :88-181)
	// ---------------------------------------------------------------------------

	/**
	 * The whole activation chain. {@code aSide}: 1 = top, otherwise the front = the facing
	 * value (the :89 gate). {@code aHeld} = the clicked hand's stack; {@code aHitX/Z} the
	 * hit coordinates inside the block. Returns the report (the RCON channel).
	 */
	public String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld,
			float aHitX, float aHitY, float aHitZ) {
		Direction tFacing = getBlockState().hasProperty(GT6GrindstoneBlock.FACING)
				? getBlockState().getValue(GT6GrindstoneBlock.FACING) : Direction.NORTH;
		boolean tTop = aSide == 1; // SIDES_TOP
		boolean tFront = aSide == tFacing.get3DDataValue(); // ALONG_AXIS front
		if (!tTop && !tFront) return "strike the top or the front"; // :89 return F
		if (!isServerSide()) {
			// :168-173 — the CLIENT corner arm: the top-face corner + a loaded stone opens the viewer
			if (tTop && stone() != 0 && isCorner(aHitX, aHitZ, tFacing)) openNei();
			return "client side";
		}
		// :93 — the server EATS the corner click without acting (the jump is the client arm)
		if (tTop && isCorner(aHitX, aHitZ, tFacing)) return "the recipe-viewer corner (the jump is the client arm)";

		if (stone() != 0) playGrindSound(); // :177 — the loaded grind sound on every working click

		if (aHeld == null || aHeld.isEmpty() || aPlayer == null) {
			mClickCount = 0; // :97-98 — an empty hand resets the combo
			return stone() == 0 ? "the grindstone is empty (load an abrasive: sand ladder 16/16/16/8/4, sandstone 8)"
					: "hold the item to sharpen";
		}
		if (stone() <= 0) {
			int tLoad = abrasiveLoad(aHeld);
			if (tLoad <= 0) { // :100-130 — no arm matched: the combo resets, nothing happens
				mClickCount = 0;
				// one voice for the empty stone — the upstream non-abrasive arm is a silent
				// no-op, the report carries the STATE (the spent stone refuses the same way)
				return "the grindstone is empty (load an abrasive: sand ladder 16/16/16/8/4, sandstone 8)";
			}
			if (!isCreative(aPlayer)) aHeld.shrink(1); // ST.use — one abrasive consumed
			setStone(tLoad); // :103/:108/:113/:118/:123/:128
			return "loaded the grindstone (" + tLoad + ")";
		}
		if (isCreative(aPlayer)) return creativeArm(aPlayer, aHeld); // :131-144 — the instant fold
		return survivalArm(aPlayer, aHeld); // :145-165 — the 10-combo arm
	}

	/** The :131-144 creative fold — instant strip or a FREE sharpen probe (nothing consumed). */
	private String creativeArm(@Nullable Player aPlayer, ItemStack aHeld) {
		int tXP = enchantmentXP(aHeld);
		if (tXP > 0) {
			giveToPlayer(aPlayer, removeEnchantments(aHeld)); // :134 — the stripped twin into the bag
			spawnXPOrb(tXP); // :135
			return "stripped the enchantments (+" + tXP + " XP, creative instant)";
		}
		Recipe tRecipe = findRecipe(aHeld);
		if (tRecipe != null && tRecipe.isRecipeInputEqual(false, true, EMPTY_FLUIDS, aHeld)) { // :140 — the probe pays nothing
			for (ItemStack tOutput : tRecipe.getOutputs()) giveToPlayer(aPlayer, tOutput.copy());
			return "sharpened (creative instant, nothing consumed)";
		}
		return "no matching sharpening recipe";
	}

	/** The :145-165 survival arm — the 10-combo gate, the paid strip or the paid sharpen. */
	private String survivalArm(@Nullable Player aPlayer, ItemStack aHeld) {
		if (++mClickCount < COMBO_LENGTH) return "grinding (" + mClickCount + "/" + COMBO_LENGTH + ")";
		mClickCount = 0;
		int tXP = enchantmentXP(aHeld);
		if (tXP > 0) {
			ItemStack tOutput = aHeld.copy();
			tOutput.setCount(1);
			aHeld.shrink(1); // :150 ST.use — the enchanted piece pays
			giveToPlayer(aPlayer, removeEnchantments(tOutput)); // :151
			spawnXPOrb(tXP); // :152
			if (aPlayer != null) aPlayer.causeFoodExhaustion(STRIP_EXHAUSTION); // :153
			return "stripped the enchantments (+" + tXP + " XP)";
		}
		Recipe tRecipe = findRecipe(aHeld);
		if (tRecipe != null && tRecipe.isRecipeInputEqual(true, false, EMPTY_FLUIDS, aHeld)) { // :158 — the inputs pay
			for (ItemStack tOutput : tRecipe.getOutputs()) giveToPlayer(aPlayer, tOutput.copy());
			if (aPlayer != null) aPlayer.causeFoodExhaustion(tRecipe.getAbsoluteTotalPower() / EXHAUST_DIVISOR); // :160
			setStone(stone() - 1); // :161 — one abrasive unit spent
			return "sharpened (abrasive " + stone() + ")";
		}
		return "no matching sharpening recipe";
	}

	/** The findRecipe walk (upstream :137/:155 — the mLastRecipe fast path over RM.Sharpening). */
	@Nullable
	private Recipe findRecipe(ItemStack aHeld) {
		Recipe tRecipe = GT6RecipeMaps.SHARPENING.findRecipe(mLastRecipe, RECIPE_SIZE, ItemStack.EMPTY, EMPTY_FLUIDS, aHeld);
		if (tRecipe != null) mLastRecipe = tRecipe; // :139/:156
		return tRecipe;
	}

	/** The corner test — the :93/:170 thresholds over the top-face [hitX, hitZ] mapping. */
	public static boolean isCorner(float aHitX, float aHitZ, Direction aFacing) {
		boolean tAxisZ = aFacing.getAxis() == Direction.Axis.Z;
		return aHitX <= (tAxisZ ? 8.0F : 4.0F) / 16.0F && aHitZ <= (tAxisZ ? 4.0F : 8.0F) / 16.0F;
	}

	/** The mStone write + visual push (the carrier's visual sync — the setVisualData face). */
	private void setStone(int aStone) {
		mStone = Math.max(0, aStone);
		if (hasLevel() && getLevel() != null && getBlockState().getBlock() instanceof GT6GrindstoneBlock tBlock) {
			tBlock.setStone(getLevel(), getBlockPos(), getBlockState(), mStone);
		}
	}

	/** The :177 dig sound — every working click on a loaded stone (the TODO-SOUND face). */
	private void playGrindSound() {
		if (hasLevel() && getLevel() != null) {
			getLevel().playSound(null, getBlockPos(), SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	/** The :135/:152 XP orb — at the stone (x+0.5, y+1.25, z+0.5, the upstream coords). */
	private void spawnXPOrb(int aXP) {
		if (hasLevel() && getLevel() instanceof ServerLevel tLevel) {
			BlockPos tPos = getBlockPos();
			ExperienceOrb.award(tLevel, new Vec3(tPos.getX() + 0.5, tPos.getY() + 1.25, tPos.getZ() + 0.5), aXP);
		}
	}

	/** The ST.give face — best-effort into the bag, the overflow spawns above the stone (the anvil ST.place form). */
	private void giveToPlayer(@Nullable Player aPlayer, ItemStack aStack) {
		if (aStack.isEmpty()) return;
		if (aPlayer != null) {
			aPlayer.getInventory().add(aStack); // partial fill is the ST.give semantics
		}
		if (!aStack.isEmpty() && hasLevel() && getLevel() != null) {
			BlockPos tPos = getBlockPos();
			ItemEntity tEntity = new ItemEntity(getLevel(), tPos.getX() + 0.5, tPos.getY() + 1.2, tPos.getZ() + 0.5, aStack);
			tEntity.setDeltaMovement(Vec3.ZERO); // the p26 drop rule — no random scatter
			getLevel().addFreshEntity(tEntity);
		}
	}

	private static boolean isCreative(@Nullable Player aPlayer) {
		return aPlayer != null && aPlayer.getAbilities().instabuild; // hasInfiniteItems :3188
	}

	/**
	 * The client NEI arm — upstream {@code mRecipes.openNEI()} (:171): the viewer jump to
	 * the SHARPENING map (EMI first, JEI fallback, neither = silent — the GTViewerJump
	 * router bottom-arms both).
	 */
	protected void openNei() {
		GTViewerJump.openRecipeMapPage(GT6RecipeMaps.SHARPENING);
	}

	@Override
	public String getTileEntityName() {
		return "grindstone";
	}
}
