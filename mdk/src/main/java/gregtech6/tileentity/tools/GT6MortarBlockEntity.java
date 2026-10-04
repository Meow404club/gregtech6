/**
 * Copyright (c) 2026 GregTech-6 Team
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

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import gregtech6.gui.GTViewerJump;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Mortars;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Mortar — the zero-energy manual grinding block (task mortar-family), the port of
 * the upstream {@code MultiTileEntityMortar} (tmp/gt6-1.7.10 .../tools/
 * MultiTileEntityMortar.java:76-106), shared over the five DESIGN rows (Loader_MultiTileEntities
 .java:2179-2183, one BE class — the ADR-P3-1 shared-BET multi-mount). NO GUI, no slots,
 * no tanks, no energy: the upstream tooltip is {@code LH.NO_GUI_CLICK_TO_INTERACT}
 * ({@code FACE_TOP}, :72) — the held item IS the input hopper, the player bag IS the
 * output slot.
 *
 * <h2>The working click (:77-95)</h2>
 * A click (ANY face — the processing arm has no side gate; only the corner swallow is
 * top-face) with a held stack runs {@code RM.Mortar.findRecipe(..., V[1], ..., aStack)}
 * (:86), consumes the inputs FROM the held stack ({@code isRecipeInputEqual(T, F, ...)},
 * :89 — the held stack shrinks), gives the outputs to the player bag (:90 {@code ST.give}),
 * exhausts the player by {@code totalPower / 250.0F} (:91 — the grinding fatigue face,
 * {@link #exhaustOf} pins the divisor), and plays {@code SFX.MC_DIG_ROCK} (:92 — the
 * port maps it to the stone-break dig, the ICoverableTE :205 mapping). No recipe → the
 * click is swallowed with no action (:105 {@code return T} — the port reports instead of
 * the silent swallow, the RCON report channel).
 *
 * <h2>The NEI corner (:79-82/:97-103)</h2>
 * The top-face {@code PX_P[4]} = 4px corner quadrant: the SERVER arm swallows the click
 * (:81), the CLIENT arm opens the recipe viewer (:100 {@code mRecipes.openNEI()} →
 * {@link #openNei} → {@code GTViewerJump.openRecipeMapPage} — EMI first, JEI fallback,
 * neither = silent). The corner rides the rim top face (the bowl envelope's x/z 2..14px
 * footprint makes the clickable corner the (2..4)x(2..4)px patch, the
 * GT6MortarNeiModel glyph seat).
 *
 * <h2>Port deviations (declared)</h2>
 * <ul>
 * <li>{@code UT.Entities.isPlayer(aPlayer)} (:84 — fake players can't work the mortar):
 *     the port reads {@code aPlayer == null} — the RCON/offline arm gets the report, a
 *     non-null Player is by construction a real player (the anvil BE's null-arm form).</li>
 * <li>The {@code ST.give} drop tail (:90): the anvil BE's all-or-nothing
 *     {@code giveToPlayer} + the spawn-above fallback (the :120 ST.place mapping) — a
 *     bag that cannot fit the whole output stack leaves the mortar untouched and the
 *     output spawns above the bowl.</li>
 * <li>The paintable face (upstream TileEntityBase07Paintable) is the declared render
 *     pool cut — the body tint is the constant Ceramic colour (the GT6MortarTint family
 *     dispatch, no spray-paint NBT).</li>
 * <li>The light-opacity column ({@code LIGHT_OPACITY_WATER}, :164) and the
 *     cover/surface/obstruction refusals (:171-180) fold onto the block-properties
 *     defaults ({@code .noOcclusion()}) — the sub-cube shape carries them.</li>
 * </ul>
 */
public class GT6MortarBlockEntity extends TileEntityBase03TicksAndSync {

	/** The top-face 4px corner bound — upstream {@code PX_P[4]} (:81/:99, PX_P = px/16). */
	public static final float CORNER_BOUND = 4.0F / 16.0F;

	/** The exhaustion divisor — upstream {@code totalPower / 250.0F} (:91). */
	public static final long EXHAUST_DIVISOR = 250;

	/** The findRecipe stack-size argument — the kitchen {@code RECIPE_SIZE} (V[1] = 32) form. */
	public static final long RECIPE_SIZE = 32;

	/** The findRecipe fast-path seed (upstream {@code mLastRecipe} :56, runtime cache — not persisted). */
	private Recipe mLastRecipe;

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6MortarBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GT6AnvilBlockEntity null-type form). */
	public GT6MortarBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6Mortars.MORTAR_BE.get(), aPos, aState);
	}

	/**
	 * The exhaustion formula seam (:91) — the offline-pinnable face of the grinding
	 * fatigue: {@code getAbsoluteTotalPower() / 250.0F} (the anvil's :122 arm carries the
	 * {@code max(1, ...)} clamp; the mortar's upstream row is verbatim, no clamp — every
	 * shipped row's total power already exceeds 250).
	 */
	static float exhaustOf(Recipe aRecipe) {
		return aRecipe.getAbsoluteTotalPower() / (float)EXHAUST_DIVISOR;
	}

	/** The top-face corner quadrant (upstream {@code tCoords[0] <= PX_P[4] && tCoords[1] <= PX_P[4]}, :81/:99). */
	public static boolean neiCorner(float aHitX, float aHitZ) {
		return aHitX <= CORNER_BOUND && aHitZ <= CORNER_BOUND;
	}

	/**
	 * The whole activation chain (upstream onBlockActivated3 :76-106). {@code aHeld} = the
	 * clicked hand's stack; {@code aHitX/Y/Z} the hit coordinates inside the block.
	 * Returns the report (the RCON channel — the player path ignores the string).
	 */
	public String activateChain(@Nullable Player aPlayer, byte aSide, @Nullable ItemStack aHeld,
			float aHitX, float aHitY, float aHitZ) {
		if (!isServerSide()) {
			// :97-103 — the CLIENT arm opens the recipe viewer from the top-face corner
			if (aSide == 1 && neiCorner(aHitX, aHitZ)) openNei();
			return "client side";
		}
		boolean tTop = aSide == 1; // SIDES_TOP — the CS side order == Direction.get3DDataValue (P4)
		// :79-82 — the top-face corner quadrant is swallowed server-side (the viewer jump is the client arm)
		if (tTop && neiCorner(aHitX, aHitZ)) return "NEI corner (the recipe-viewer jump is the client arm)";
		// :84 — UT.Entities.isPlayer gate: fake players can't work the mortar (the null arm = the RCON report)
		if (aPlayer == null) return "only a player can work the mortar";
		if (aHeld == null || aHeld.isEmpty()) return "nothing held";

		// :86-95 — the working click: the held stack is the whole input hopper
		Recipe tRecipe = GT6RecipeMaps.MORTAR.findRecipe(mLastRecipe, RECIPE_SIZE, ItemStack.EMPTY,
				new net.minecraftforge.fluids.FluidStack[0], aHeld);
		if (tRecipe == null) return "no matching recipe"; // :105 — the silent upstream swallow, reported
		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :88
		// :89 — the inputs pay FROM the held stack (the varargs entry IS the held stack)
		if (!tRecipe.isRecipeInputEqual(true, false, new net.minecraftforge.fluids.FluidStack[0], aHeld)) {
			return "the held stack does not cover the recipe input";
		}
		int tGiven = 0;
		ItemStack tFirst = null;
		ItemStack[] tOutputs = tRecipe.getOutputs(); // resolve ONCE (chance rows roll per call)
		for (ItemStack tOutput : tOutputs) { // :90 — ST.give into the bag
			if (tOutput == null || tOutput.isEmpty()) continue;
			if (!giveToPlayer(aPlayer, tOutput)) spawnAbove(tOutput.copy()); // the ST.give drop tail
			tGiven += tOutput.getCount();
			if (tFirst == null) tFirst = tOutput;
		}
		if (hasLevel() && getLevel() != null) { // :92 — SFX.MC_DIG_ROCK
			getLevel().playSound(null, getBlockPos(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		aPlayer.causeFoodExhaustion(exhaustOf(tRecipe)); // :91 — the grinding fatigue
		return "ground: " + tGiven + "x " + (tFirst != null ? tFirst.getItem() : "output");
	}

	/**
	 * The client NEI arm — upstream {@code mRecipes.openNEI()} (MultiTileEntityMortar.java
	 * :100): the viewer jump to the MORTAR map. Silent when no viewer is installed or its
	 * runtime is not ready — the GTViewerJump router bottom-arms both.
	 */
	protected void openNei() {
		GTViewerJump.openRecipeMapPage(GT6RecipeMaps.MORTAR);
	}

	/**
	 * The :90 {@code ST.give} give — ALL-OR-NOTHING (the GT6AnvilBlockEntity form: the
	 * vanilla {@code Inventory.add} is PARTIAL-fill, so the fit is decided FIRST by the
	 * free-capacity pass and the real add only runs when the whole stack fits — a nearly-
	 * full bag must not absorb part of an output while the tail spawns the full copy).
	 * Package-private: the boundary test walks it (same package).
	 */
	boolean giveToPlayer(Player aPlayer, ItemStack aStack) {
		if (aStack.isEmpty()) return true;
		Inventory tInventory = aPlayer.getInventory();
		if (freeCapacity(tInventory, aStack) < aStack.getCount()) return false; // all-or-nothing
		ItemStack tRemainder = aStack.copy();
		tInventory.add(tRemainder);
		return tRemainder.isEmpty(); // belt-and-braces: the simulation above matches add()
	}

	/** The total remaining capacity of the main inventory (the GT6AnvilBlockEntity form). */
	private static int freeCapacity(Inventory aInventory, ItemStack aStack) {
		int rCapacity = 0;
		for (int i = 0, n = aInventory.items.size(); i < n; i++) {
			ItemStack tSlot = aInventory.items.get(i);
			if (tSlot.isEmpty()) {
				rCapacity += aStack.getMaxStackSize();
			} else if (ItemStack.isSameItemSameTags(tSlot, aStack) && tSlot.getCount() < tSlot.getMaxStackSize()) {
				rCapacity += tSlot.getMaxStackSize() - tSlot.getCount();
			}
		}
		return rCapacity;
	}

	/** The ST.give drop tail — outputs spawn above the bowl (y + 1.2), zero delta (the anvil :120 form). */
	private void spawnAbove(ItemStack aStack) {
		if (!hasLevel() || getLevel() == null || aStack.isEmpty()) return;
		BlockPos tPos = getBlockPos();
		ItemEntity tEntity = new ItemEntity(getLevel(), tPos.getX() + 0.5, tPos.getY() + 1.2, tPos.getZ() + 0.5, aStack);
		tEntity.setDeltaMovement(Vec3.ZERO);
		getLevel().addFreshEntity(tEntity);
	}

	@Override
	public String getTileEntityName() {
		return "mortar";
	}
}
