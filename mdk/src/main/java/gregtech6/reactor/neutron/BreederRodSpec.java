package gregtech6.reactor.neutron;

import java.util.List;
import java.util.Optional;

/**
 * The 4 breeder rod specs (task debt-reactor-a-neutron-core), one row per upstream
 * registration in Loader_MultiTileEntities.java:782-785 — {@code needed} =
 * NBT_MAXDURABILITY, the neutron total to absorb before the rod becomes the enriched
 * product ({@code mDurability -= oNeutronCounts} per tick, swap on {@code <= 0},
 * RodBreeder.java:84-90); {@code loss} = NBT_NUCLEAR_LOSS, subtracted from the
 * neutrons entering <em>each side</em> of this rod, not from the total (Breeder
 * tooltip :67-68, reflection :97); {@code productId} = NBT_VALUE, the enriched rod
 * meta swapped to in place.
 */
public record BreederRodSpec(int id, String name, long needed, int loss, int productId) {

	/** Loader_MultiTileEntities.java:782-785 verbatim, registration order. */
	public static final List<BreederRodSpec> RODS = List.of(
		new BreederRodSpec(9410, "Thorium-232",    64_000_000L,  1000, 9411),
		new BreederRodSpec(9420, "Uranium-238",   256_000_000L,  2500, 9421),
		new BreederRodSpec(9430, "Lithium",          640_000L,   250, 9431),
		new BreederRodSpec(9440, "Naquadah",      4_096_000_000L, 10000, 9441));

	/** Row lookup by upstream meta id. */
	public static Optional<BreederRodSpec> byId(int aId) {
		return RODS.stream().filter(r -> r.id == aId).findFirst();
	}
}
