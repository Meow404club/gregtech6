package gregtech6.tileentity.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.data.OP;
import gregtech6.registry.GT6Crucibles;

/**
 * The Basin — a Mold that always casts {@link OP#blockSolid} (task
 * material-mc-c-crucible-rows, the Loader_MultiTileEntities.java:424-466 family; upstream
 * {@code MultiTileEntityBasin extends MultiTileEntityMold} and its ONLY behavioral
 * overrides are the shape answer and the input face):
 * <ul>
 * <li>{@code getMoldRecipe} — upstream an instance override answering {@code OP.blockSolid}
 *     shape-agnostically (MultiTileEntityBasin.java:21-24); the port routes the mold's
 *     shape lookups through the {@link TileEntityMold#moldRecipeFor} instance seam and this
 *     class answers the block form (the cast item resolves through the
 *     {@code GT6RecipeMapCrucible.matStack} representable gate — a material without a
 *     blockSolid item path refuses the pour, the declared p10-compat deviation);</li>
 * <li>{@code isMoldInputSide} — SIDES_TOP ONLY (upstream :56-58, the vertical pour), where
 *     the base mold answers top + 4 horizontals;</li>
 * <li>everything else (temperature drift, over-heat lava melt-down, cool-then-solidify,
 *     the top-click pick-up/pour, the wrench auto-pull sides) is the base mold chain
 *     verbatim — the family is the shared {@link GT6Crucibles#BASIN_BE} over the 39
 *     {@code basin_*} shell rows.</li>
 * </ul>
 */
public class TileEntityBasin extends TileEntityMold {

	public TileEntityBasin(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	public TileEntityBasin(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gt6.basin"; // BET registry path mirrors it (GT6Crucibles.BASIN_BE)
	}

	/** The shell material rides the {@link GT6Crucibles.BasinBlock} carrier (the mold's MoldBlock form). */
	@Override
	@Nullable
	public OreDictMaterial material() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6Crucibles.BasinBlock tBlock) return tBlock.row().material().get();
		return null;
	}

	/** Upstream MultiTileEntityBasin.java:21-24 — the shape-agnostic block cast. */
	@Override
	@Nullable
	protected OreDictPrefix moldRecipeFor(int aShape) {
		return OP.blockSolid;
	}

	/** Upstream :56-58 — the vertical pour face only. */
	@Override
	public boolean isMoldInputSide(byte aSide) {
		return aSide == TileEntityMold.SIDE_TOP;
	}
}
