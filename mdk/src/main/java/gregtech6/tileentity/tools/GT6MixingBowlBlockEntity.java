package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Kitchen;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The Ceramic Mixing Bowl — the port of upstream {@code MultiTileEntityMixingBowl}
 * (tmp/gt6-1.7.10 .../tools/MultiTileEntityMixingBowl.java:62-480), the ceramic row
 * (MT.Ceramic, 8000 L, RM.Mixer — Loader_MultiTileEntities.java:2177; the Table variant
 * :2178 is the task-card pool cut). Processes the {@code RM.Mixer} map — the one the
 * p26-c-foam-fluid-refill card declared and poured (NO map creation here, the hard
 * ordering gate of the task card).
 *
 * <p><b>Body differences from the pot (the upstream copy deltas):</b>
 * <ul>
 * <li>the canOutput fluid door is {@code !FL.simple} (:196) instead of {@code FL.gas}
 *     (:178) — port ≈ the same carrier query, the base-class deviation note;</li>
 * <li>the fill door carries {@code !FL.simple} too (:325);</li>
 * <li>the exhaustion divisor is 250 (:223) instead of 1000 (:202);</li>
 * <li>the click-economy's input-return gate watches slot 6 only (:274
 *     {@code !slotHas(6)}) — generalised in the base as "all output slots empty", which
 *     is the same verdict while {@code mOutputItemsCount == 1} (the MIXER row's item
 *     output census, GT6RecipeMaps.java init);</li>
 * <li>the TOOL_mixer stick arm (:100-117) stays pooled — the stick tool does not exist
 *     in the port; the bare top-face right-click round (:214-227) is kept, so the bowl
 *     remains fully drivable (the tool arm is an ALTERNATIVE trigger upstream, not a
 *     gate).</li>
 * </ul>
 * The upstream {@code RM.add_smelting(IL.Ceramic_Bowl_Raw, IL.Ceramic_Bowl)} hardening
 * line (:2177 tail) rides the vanilla smelting datagen JSON of the registration card.
 */
public class GT6MixingBowlBlockEntity extends GT6ManualKitchenBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6MixingBowlBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6MixingBowlBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Kitchen.MIXING_BOWL_BE.get(), aPos, aState);
	}

	/** Upstream :64 {@code protected RecipeMap mRecipes = RM.Mixer}. */
	@Override
	protected RecipeMap recipeMap() {
		return GT6RecipeMaps.MIXER;
	}

	/** Upstream :223 — {@code totalPower / 250}. */
	@Override
	protected long exhaustDivisor() {
		return 250;
	}

	/** Upstream :325 — the {@code !FL.simple} fill door ≈ the gas carrier query (base-class deviation note). */
	@Override
	protected boolean refuseFillFluid(FluidStack aFluid) {
		return isGasFluid(aFluid);
	}

	/** Upstream :196 — the {@code !FL.simple} canOutput door ≈ the gas carrier query. */
	@Override
	protected boolean refuseOutputFluid(FluidStack aFluid) {
		return isGasFluid(aFluid);
	}

	@Override
	public String getTileEntityName() {
		return "mixing_bowl"; // BET registry path mirrors it (GT6Kitchen.MIXING_BOWL_BE)
	}
}
