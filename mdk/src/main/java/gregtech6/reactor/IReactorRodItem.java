package gregtech6.reactor;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import gregtech6.reactor.neutron.BreederRodSpec;
import gregtech6.reactor.neutron.FuelRodSpec;
import gregtech6.reactor.neutron.ReactorRodKind;

/**
 * The rod item seam (task debt-reactor-b-2x2-be) — the port counterpart of the upstream
 * {@code gregapi.item.IItemReactorRod} (IItemReactorRod.java:29-48), REDUCED to the
 * identity the 2x2 core BE actually reads: the behavioural {@link ReactorRodKind} (the
 * A-card enum already carries every per-kind neutron rule), the immutable spec row when
 * the kind is {@link ReactorRodKind#FUEL}/{@link ReactorRodKind#BREEDER}, and the
 * in-place swap target of the depletion/breeding end state. All mutable burn state
 * rides the stack through {@link ReactorRodNbt} (the upstream item-NBT halves).
 *
 * <p>The 46 upstream rod ITEMS (Loader_MultiTileEntities.java:734-790) are the C card's
 * face — this B card registers the BE/block and verifies the behaviour with
 * uncraftable test-fixture rods implementing this seam.
 */
public interface IReactorRodItem {

	/** The behavioural kind of this rod ({@code isReactorRod} is implicit — implementing this interface). */
	ReactorRodKind rodKind(ItemStack aStack);

	/** The fuel row ({@code NBT_NUCLEAR_*} columns), FUEL kind only — null otherwise. */
	@Nullable
	default FuelRodSpec fuelSpec(ItemStack aStack) {
		return null;
	}

	/** The breeder row ({@code NBT_MAXDURABILITY/NBT_NUCLEAR_LOSS/NBT_VALUE}), BREEDER kind only — null otherwise. */
	@Nullable
	default BreederRodSpec breederSpec(ItemStack aStack) {
		return null;
	}

	/**
	 * The in-place swap target of the end state: the depleted rod meta for fuel
	 * ({@code NBT_VALUE}, Nuclear.java:221), the enriched product meta for breeders
	 * (Breeder.java:86). A null answer keeps the current stack (the fixture seam of
	 * tests that pin the pre-swap state).
	 */
	@Nullable
	default ItemStack rodSwapTarget(int aTargetId) {
		return null;
	}
}
