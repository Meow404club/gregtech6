package gregtech6.reactor.neutron;

import java.util.List;
import java.util.Optional;

/**
 * The 17 fuel rod specs (task debt-reactor-a-neutron-core), one row per upstream
 * registration in Loader_MultiTileEntities.java:746-762 — id = the upstream multi-tile
 * meta id, {@code durability} = NBT_MAXDURABILITY, {@code self/other/div/max} = the
 * NBT_NUCULAR_* parameters the RodNuclear reads back (RodNuclear.java:50-53),
 * {@code depletedId} = NBT_VALUE, the depleted rod meta the item swaps to in place on
 * depletion (RodNuclear.java:220-224 — meta swap + NBT clear, no drop).
 *
 * <p>Tooltip-derived sanity: remaining minutes = durability / 120000 (Nuclear.java:83);
 * div &le; 4 marks the fuel "Critical" (Nuclear.java:92).
 */
public record FuelRodSpec(int id, String name, long durability, int self, int other, int div, int max, int depletedId) {

	/** Loader_MultiTileEntities.java:746-762 verbatim, registration order. */
	public static final List<FuelRodSpec> RODS = List.of(
		new FuelRodSpec(9210, "Thorium-232",            12_000_000_000L,   2,   2, 32,   128, 9310),
		new FuelRodSpec(9219, "Cyanite",                12_000_000_000L,   2,   2, 32,    64, 9319),
		new FuelRodSpec(9220, "Uranium-238",             6_000_000_000L,   4,   4, 16,   512, 9320),
		new FuelRodSpec(9221, "Uranium-235",             1_200_000_000L,  32,  32,  4,  2048, 9321),
		new FuelRodSpec(9222, "Uranium-233",             6_000_000_000L,  32,  32,  4,  2048, 9322),
		new FuelRodSpec(9229, "Yellorium",               6_000_000_000L,   4,   4, 16,   256, 9329),
		new FuelRodSpec(9230, "Plutonium-244",           1_200_000_000L,  64,  64,  4,  2048, 9330),
		new FuelRodSpec(9231, "Plutonium-241",           1_200_000_000L, 128, 128,  3,  3072, 9331),
		new FuelRodSpec(9232, "Plutonium-243",           1_200_000_000L, 128, 128,  3,  4096, 9332),
		new FuelRodSpec(9233, "Plutonium-239",           2_400_000_000L, 128, 128,  3,  4096, 9333),
		new FuelRodSpec(9239, "Blutonium",               1_200_000_000L,  64,  64,  4,  1024, 9339),
		new FuelRodSpec(9240, "Americium-245",           1_200_000_000L,  64,  64,  4,  4096, 9340),
		new FuelRodSpec(9241, "Americium-241",           1_200_000_000L, 128, 128,  3,  4096, 9341),
		new FuelRodSpec(9249, "Ludicrite",               1_200_000_000L, 128, 128,  3,  3072, 9349),
		new FuelRodSpec(9250, "Cobalt-60",                 120_000_000L,   8,   0, 16,   256, 9350),
		new FuelRodSpec(9260, "Enriched Naquadah",       12_000_000_000L, 128, 128,  4,  8192, 9360),
		new FuelRodSpec(9261, "Naquadria",               12_000_000_000L, 512, 512,  3, 16384, 9361));

	/** Row lookup by upstream meta id. */
	public static Optional<FuelRodSpec> byId(int aId) {
		return RODS.stream().filter(r -> r.id == aId).findFirst();
	}

	/** Tooltip minutes at the advertised burn rate (Nuclear.java:83: durability / 120000). */
	public long advertisedMinutes() {
		return durability / 120_000;
	}

	/** Nuclear.java:92 — div &le; 4 marks the fuel Critical in the tooltip. */
	public boolean critical() {
		return div <= 4;
	}
}
