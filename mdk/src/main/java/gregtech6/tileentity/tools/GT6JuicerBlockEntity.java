package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GT6Kitchen;

/**
 * The Juicer — the port of upstream {@code MultiTileEntityJuicer}
 * (tmp/gt6-1.7.10 .../tools/MultiTileEntityJuicer.java:63, the
 * {@code TileEntityBase07Paintable} manual block; registered at Loader_MultiTileEntities
 * .java:2184 id 32722, MT.Ceramic, aUtilStone, hardness 1.0 / resistance 5.0, the RM.Juicer
 * map — task p33-food-machines-kitchen). Shares the p26 manual-kitchen base
 * ({@link GT6ManualKitchenBlockEntity}, the BathingPot/MixingBowl family body) — the
 * upstream Juicer class is itself the same manual top-face
 * {@code findRecipe → canOutput → isRecipeInputEqual → ST.give(outputs) + tank fill +
 * exhaust(totalPower/10000)} chain (:139-151), so the base covers it with three deltas:
 * <ul>
 * <li>the recipe map is {@code RM.Juicer} (:65, the 1/3/1 item 0/1/0 fluid shape,
 *     GT6RecipeMaps.JUICER — the b1 card poured the rows via juicer.json);</li>
 * <li>the exhaustion divisor is 10000 (:146 {@code / 10000.0F});</li>
 * <li>the tank array is the JUICER map's 0-input / 1-output fluid shape (:74, the
 *     base sizes the arrays from the map — the OUTPUT tank only, capacity = the
 *     carrier's block litres).</li>
 * </ul>
 * Declared deviations (the family pool cuts): the plunger/magnifying-glass tool arms
 * (:96-105) and the mDisplay fluid-tint renderer (:109-117) ride the p26 pool; the
 * outputs land in the output slots instead of {@code ST.give} straight to the player
 * (the base's click-economy collects them — the same one-click-extra the bowl family
 * accepted), the NEI corner branch is the declared no-op.
 *
 * <p>Upstream :2184 tail also ores the Juicer item ({@code IL.Juicer, MT.Ceramic U*4})
 * and ships the clay-Juicer smelting hardening line — the item face rides the
 * kjs-binding card (the KJS ruling: the registration face is this card, the item-ore
 * seam is not).
 */
public class GT6JuicerBlockEntity extends GT6ManualKitchenBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6JuicerBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6JuicerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Kitchen.JUICER_BE.get(), aPos, aState);
	}

	/** Upstream :65 {@code protected RecipeMap mRecipes = RM.Juicer}. */
	@Override
	protected RecipeMap recipeMap() {
		return GT6RecipeMaps.JUICER;
	}

	/** Upstream :146 — {@code totalPower / 10000.0F}. */
	@Override
	protected long exhaustDivisor() {
		return 10000;
	}

	@Override
	public String getTileEntityName() {
		return "juicer"; // BET registry path mirrors it (GT6Kitchen.JUICER_BE)
	}
}
