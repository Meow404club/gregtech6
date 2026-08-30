package gregtech6.recipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * The pure tag→recipes expansion for the Coke Oven log family (the 2026-08-30 coordinator
 * amendment of ADR ruling ②): input = the content of a log tag, output = one recipe per log
 * item. No world, no registry lookups, no bus — the offline tests feed synthetic content.
 *
 * <p><b>Upstream shape</b> (Loader_Recipes_Woods.java:176-180, verbatim):
 * {@code if (mCreosoteAmount > 0 || mCharcoalCount > 0) { outputs = mCharcoalCount ×
 * gem.Charcoal; addRecipe1(T, 0, 3600, log, NF, creosote <= 0 ? NF : creosote.make(amount),
 * outputs); }}. For the vanilla log universe the rates are the 2-arg WoodEntry defaults
 * {@code (1, 250)} (WoodEntry.java:76-77 — every vanilla wood registers through
 * {@code new WoodEntry(log, beam)} at LoaderWoodDictionary.java:51-56), i.e. one charcoal
 * gem and 250 mB creosote per log at 3600 ticks.
 *
 * <p>The expansion registers CONCRETE ItemStack recipes (one per log — the upstream
 * oredict listener also registered per-item), consuming nothing from the P4 Recipe shell.
 */
public final class GT6CokeOvenLogExpansion {

	/** WoodEntry 2-arg default mCharcoalCount (WoodEntry.java:76-77, the vanilla-log universe). */
	public static final int CHARCOAL_PER_LOG = 1;
	/** WoodEntry 2-arg default mCreosoteAmount, in mB (250 raw via FL.make — already mB). */
	public static final int CREOSOTE_MB_PER_LOG = 250;
	/** Woods.java:179 — the fixed 3600 tick duration of the log family. */
	public static final long DURATION = 3600;

	private GT6CokeOvenLogExpansion() {}

	/**
	 * Expands the log items into one {@link Recipe} each (order preserved for the audit log).
	 * Null/empty items and a null charcoal gem skip (the upstream ST.invalid / mat()-null
	 * guard shape); a null creosote yields recipes without the fluid output (the upstream
	 * {@code creosote <= 0 ? NF} branch — keeps the expansion total when only the fluid is
	 * missing, matching the static pour's per-segment skip granularity).
	 */
	public static List<Recipe> expand(Collection<Item> aLogs, @Nullable Item aCharcoalGem, @Nullable Fluid aCreosote) {
		List<Recipe> rRecipes = new ArrayList<>();
		if (aCharcoalGem == null) return rRecipes;
		for (Item tLog : aLogs) {
			if (tLog == null) continue;
			ItemStack[] tOutputs = new ItemStack[CHARCOAL_PER_LOG];
			for (int i = 0; i < CHARCOAL_PER_LOG; i++) tOutputs[i] = new ItemStack(aCharcoalGem, 1); // Arrays.fill, Woods.java:178
			FluidStack[] tFluidOutputs = aCreosote == null
					? new FluidStack[0]
					: new FluidStack[] {new FluidStack(aCreosote, CREOSOTE_MB_PER_LOG)};
			rRecipes.add(new Recipe(true, new ItemStack[] {new ItemStack(tLog, 1)}, tOutputs,
					new FluidStack[0], tFluidOutputs, DURATION, 0, 0));
		}
		return rRecipes;
	}
}
