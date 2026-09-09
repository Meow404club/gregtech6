package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6Kitchen;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The BathingPot — the port of the upstream steel body
 * {@code MultiTileEntityBathingPot} (tmp/gt6-1.7.10 .../tools/MultiTileEntityBathingPot
 * .java:62-459), shared over the wood + steel rows (the RM.Bath 4000 L wood pot :2173 /
 * 8000 L steel pot :2175 of Loader_MultiTileEntities.java, one BE class, the ADR-P3-1
 * shared-BET multi-mount; the wood class/steel class split carried no behavioural
 * difference beyond the registration NBT). Processes the {@code RM.Bath} map
 * ({@code GT6RecipeMaps.BATH}, the class doc of the map field carries the base-form
 * deviation of the upstream {@code RecipeMapBath} subclass).
 *
 * <p>The wood pot's {@code NBT_FLAMMABILITY 100} row is recorded on the block carrier
 * javadoc — the fire-spread bridge is the flammability pool. The Table variants (:2174/
 * :2176) are the task-card pool cut.
 */
public class GT6BathingPotBlockEntity extends GT6ManualKitchenBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the registry type at runtime. */
	public GT6BathingPotBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the GTBarrelBlockEntity null-type form). */
	public GT6BathingPotBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6Kitchen.BATHING_POT_BE.get(), aPos, aState);
	}

	/** Upstream :64 {@code protected RecipeMap mRecipes = RM.Bath}. */
	@Override
	protected RecipeMap recipeMap() {
		return GT6RecipeMaps.BATH;
	}

	/** Upstream :202 — {@code max(1, totalPower) / 1000}. */
	@Override
	protected long exhaustDivisor() {
		return 1000;
	}

	@Override
	public String getTileEntityName() {
		return "bathing_pot"; // BET registry path mirrors it (GT6Kitchen.BATHING_POT_BE)
	}
}
