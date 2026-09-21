package gregtech6.covers.covers.logistics;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import gregtech6.covers.CoverData;

/**
 * The filtered-fluid logistics bus base — 1.20.1 port of the fluid-branch body of
 * gregapi/cover/covers/CoverLogisticsFluidExport.java (:45-112, shared verbatim by
 * Import/Storage), task p33-logistics-covers-12. The filter lives in the
 * {@link CoverData#mNBTs} lane under the verbatim upstream key {@code gt.filter.fluid}
 * — the port stores the fluid's registry path string (the identity form; the upstream
 * FL.save FluidStack wrapper degenerates since the port filter is NBT-insensitive).
 * The right-click set resolves the held bucket's fluid (upstream :91 FL.getFluid —
 * the OM container-material fallback (:93-97) rides the material-system card).
 */
public abstract class AbstractCoverLogisticsFluid extends AbstractCoverLogisticsFiltered {

	protected AbstractCoverLogisticsFluid() {
		super(FILTER_KEY_FLUID);
	}

	@Override
	@Nullable
	public CompoundTag filterLaneFor(ItemStack aHeld) {
		net.minecraft.world.level.material.Fluid tFluid = fluidOfHeld(aHeld);
		if (tFluid == null || BuiltInRegistries.FLUID.getKey(tFluid) == null) return null;
		CompoundTag tLane = new CompoundTag();
		tLane.putString(filterKey, BuiltInRegistries.FLUID.getKey(tFluid).toString()); // upstream :99 FL.save
		return tLane;
	}

	/** Upstream :91 — the held stack's fluid (the bucket branch; the OM material-container fallback :93-97 is the declared trim). */
	@Nullable
	public static net.minecraft.world.level.material.Fluid fluidOfHeld(ItemStack aHeld) {
		//? if forge {
		return net.minecraftforge.fluids.FluidUtil.getFluidContained(aHeld).map(f -> f.getFluid()).orElse(null);
		//?} else {
		/*var tContent = aHeld.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM, null);
		if (tContent == null) return null;
		var tStack = tContent.getFluidInTank(0);
		return tStack.isEmpty() ? null : tStack.getFluid();
		 *///?}
	}

	/** Upstream :71 FL.load — the stored fluid of the lane, {@code null} when unset. */
	@Nullable
	public net.minecraft.world.level.material.Fluid filterFluidOf(CoverData aData, byte aSide) {
		if (aData.mNBTs[aSide] == null || !aData.mNBTs[aSide].contains(filterKey, Tag.TAG_STRING)) return null;
		String tPath = aData.mNBTs[aSide].getString(filterKey);
		if (tPath.isEmpty()) return null;
		ResourceLocation tId = ResourceLocation.tryParse(tPath);
		return tId == null ? null : BuiltInRegistries.FLUID.get(tId); // .get returns EMPTY fluid on a miss — the ST.invalid arm
	}
}
