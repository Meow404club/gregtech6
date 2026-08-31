package gregtech6.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The electric-wire data table (task p9-wire-family-w1 spec ①) — the direct transcription of
 * upstream {@code MultiTileEntityWireElectric.addElectricWires}
 * (tmp/gt6-1.7.10 gregapi/tileentity/connectors/MultiTileEntityWireElectric.java:71-109) and
 * its 30-material registration loop (gregtech/loaders/b/Loader_MultiTileEntities.java:1913-1950).
 * Pure Java (no MC classes): the census test instantiates it in a plain JVM, the registration
 * (GTWires), the command selector (GTWireCommand) and datagen (GT6BlockStates/GT6EnUs/GT6LootTables)
 * all walk this ONE table.
 *
 * <p>Per material row the upstream utility registers 16 bare wires ({@code wireGt01..wireGt16},
 * :72-87 — diameter {@code PX_P[n]}, max stack {@code 64/n}, amperage {@code aAmperage*n}, loss
 * {@code aLossWire}) plus, when {@code aCable}, 5 insulated cables ({@code cableGt01/02/04/08/12},
 * :89-93 — diameter {@code PX_P[4/6/8/12/16]}, max stack {@code 64/32/16/8/4} as upstream's
 * literals, amperage {@code aAmperage*n}, loss {@code aLossCable}). Pinned size: 28 cable rows × 21
 * + the 2 pure-wire rows (Graphene/Superconductor, {@code aCable=F}, no shock flags) × 16 =
 * <b>620</b> registered variants — the census yardstick (GTWireSpecsCensusTest), no rows may be
 * dropped client-side (the ADR rules the full spectrum in one step; trimming is a pure-data
 * escape hatch that must go through an ADR first).
 *
 * <p>Voltages are the upstream {@code V[tier]*mult} expressions; {@link #V} is the verbatim port
 * of the CS.java:148-154 voltage-tier table (the census re-asserts every tier literal). The
 * contact-damage flags ride along as pure data (the shock behaviour itself is a pool item, ADR
 * deviation list) so a future card does not re-transcribe the rows.
 *
 * <p>Materials are {@link Supplier} references (the GTBarrels.MetalDrumRow lazy pattern): this
 * class loads before {@code MT.init()} (GT6Mod enqueueWork segment 1), so rows must not
 * dereference MT fields at class-init time — only inside suppliers, which run during Block
 * registration / datagen / tests, all after the material refill.
 */
public final class GTWireSpecs {

	private static final String MOD_ID = "gt6";

	/** The voltage-tier table, verbatim from CS.java:148-154 (V === VREC; VMIN/VMAX unused here). */
	public static final long[] V = {
			8, 32, 128, 512, 2048, 8192, 32768, 131072,
			524288, 2097152, 8388608, 33554432, 134217728, 536870912, 2147483648L, 8589934592L
	};

	/** The short tier names, verbatim from CS.java:154. */
	public static final String[] VN = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1", "PUV2", "PUV3", "PUV4", "PUV5", "XV", "XV"};

	/** The bare-wire shapes :72-87 — index i (0-based) = wireGt0(i+1): the PX_P diameter index. */
	public static final int[] WIRE_DIAMETERS = {2, 3, 4, 6, 7, 7, 8, 8, 9, 10, 11, 12, 13, 14, 15, 16};

	/** The insulated-cable shapes :89-93 — the multiplier, the upstream literal max stack, the PX_P diameter index. */
	public static final int[] CABLE_MULTIPLIERS = {1, 2, 4, 8, 12};
	public static final int[] CABLE_STACKS = {64, 32, 16, 8, 4};
	public static final int[] CABLE_DIAMETERS = {4, 6, 8, 12, 16};

	/**
	 * One material row = one upstream {@code addElectricWires(...)} call. Field order mirrors the
	 * upstream parameter list (aID/aCreativeTabID stay upstream-bookkeeping and are not ported).
	 *
	 * @param idComment the Loader_MultiTileEntities line this row transcribes
	 * @param token     the snake-cased form of the material internal name, fixed at table
	 *                  build time ({@code MT.Sn} -> "tin", {@code MT.SiC} -> "carborundum") — the
	 *                  registry-name composition must run before {@code MT.init()} fills the
	 *                  material fields, so the token is stored, not derived; the census test
	 *                  re-derives every token through {@code GTMaterialItems.snakeCase} and
	 *                  fails the table on drift
	 * @param material  the lazy {@code aMat} (MT fields are null until MT.init())
	 * @param tier      the {@code V[...]} index of the voltage expression
	 * @param mult      the {@code *n} multiplier of the voltage expression
	 * @param amperage  the {@code aAmperage} bandwidth base
	 * @param lossWire  the {@code aLossWire} per-segment loss of the bare wires
	 * @param lossCable the {@code aLossCable} per-segment loss of the insulated cables
	 * @param contactDamageWire  the {@code aContactDamageWire} flag (pure data, shock behaviour is a pool item)
	 * @param contactDamageCable the {@code aContactDamageCable} flag (pure data)
	 * @param cable     the {@code aCable} flag — false rows register the 16 bare wires ONLY
	 */
	public record Row(String idComment, String token, Supplier<OreDictMaterial> material, int tier, int mult,
			long amperage, long lossWire, long lossCable, boolean contactDamageWire, boolean contactDamageCable, boolean cable) {

		/** The row voltage: the upstream {@code V[tier]*mult} expression (Loader rows). */
		public long voltage() {
			return V[tier] * mult;
		}
	}

	/** The 30 registration rows, verbatim in Loader_MultiTileEntities.java:1914-1950 file order. */
	public static final List<Row> ROWS = List.of(
		/* :1914 */ new Row("Loader:1914", "tin", () -> MT.Sn, 1, 1, 1, 2, 1, true, false, true),
		/* :1915 */ new Row("Loader:1915", "lead", () -> MT.Pb, 1, 2, 1, 2, 1, true, false, true),
		/* :1917 */ new Row("Loader:1917", "constantan", () -> MT.Constantan, 2, 1, 4, 4, 3, true, false, true),
		/* :1918 */ new Row("Loader:1918", "copper", () -> MT.Cu, 2, 2, 1, 2, 1, true, false, true),
		/* :1919 */ new Row("Loader:1919", "annealed_copper", () -> MT.AnnealedCopper, 2, 3, 1, 2, 1, true, false, true),
		/* :1920 */ new Row("Loader:1920", "efrine", () -> MT.Efrine, 2, 2, 1, 2, 1, true, false, true),
		/* :1922 */ new Row("Loader:1922", "kanthal", () -> MT.Kanthal, 3, 1, 4, 4, 3, true, false, true),
		/* :1923 */ new Row("Loader:1923", "silver", () -> MT.Ag, 3, 3, 1, 2, 1, true, false, true),
		/* :1924 */ new Row("Loader:1924", "gold", () -> MT.Au, 3, 1, 3, 2, 1, true, false, true),
		/* :1925 */ new Row("Loader:1925", "electrum", () -> MT.Electrum, 3, 2, 2, 2, 1, true, false, true),
		/* :1926 */ new Row("Loader:1926", "blue_alloy", () -> MT.BlueAlloy, 3, 3, 2, 2, 1, true, false, true),
		/* :1927 */ new Row("Loader:1927", "electrotine_alloy", () -> MT.ElectrotineAlloy, 3, 2, 3, 2, 1, true, false, true),
		/* :1929 */ new Row("Loader:1929", "nichrome", () -> MT.Nichrome, 4, 1, 4, 4, 3, true, false, true),
		/* :1930 */ new Row("Loader:1930", "steel", () -> MT.Steel, 4, 1, 2, 3, 2, true, false, true),
		/* :1931 */ new Row("Loader:1931", "hslasteel", () -> MT.HSLA, 4, 1, 3, 3, 2, true, false, true),
		/* :1932 */ new Row("Loader:1932", "aluminium", () -> MT.Al, 4, 1, 1, 2, 1, true, false, true),
		/* :1933 */ new Row("Loader:1933", "tungstensteel", () -> MT.TungstenSteel, 4, 2, 4, 3, 2, true, false, true),
		/* :1934 */ new Row("Loader:1934", "tungsten", () -> MT.W, 4, 3, 8, 3, 2, true, false, true),
		/* :1935 */ new Row("Loader:1935", "netherite", () -> MT.Netherite, 4, 1, 1, 2, 1, true, false, true),
		/* :1937 */ new Row("Loader:1937", "osmium_elemental", () -> MT.Os, 5, 2, 4, 4, 3, true, false, true),
		/* :1938 */ new Row("Loader:1938", "platinum", () -> MT.Pt, 5, 3, 2, 2, 1, true, false, true),
		/* :1939 */ new Row("Loader:1939", "osmiridium", () -> MT.Osmiridium, 5, 3, 4, 2, 1, true, false, true),
		/* :1940 */ new Row("Loader:1940", "carborundum", () -> MT.SiC, 5, 1, 4, 4, 3, true, false, true),
		/* :1941 */ new Row("Loader:1941", "iridium", () -> MT.Ir, 5, 3, 4, 4, 2, true, false, true),
		/* :1943 */ new Row("Loader:1943", "naquadah", () -> MT.Nq, 6, 3, 4, 2, 1, true, false, true),
		/* :1944 */ new Row("Loader:1944", "niobium_titanium", () -> MT.NiobiumTitanium, 6, 1, 4, 4, 3, true, false, true),
		/* :1945 */ new Row("Loader:1945", "vanadium_gallium", () -> MT.VanadiumGallium, 6, 2, 4, 4, 3, true, false, true),
		/* :1946 */ new Row("Loader:1946", "yttrium_barium_cuprate", () -> MT.YttriumBariumCuprate, 6, 3, 4, 4, 3, true, false, true),
		/* :1948 */ new Row("Loader:1948", "graphene", () -> MT.Graphene, 6, 2, 1, 2, 2, false, false, false),
		/* :1950 */ new Row("Loader:1950", "superconductor", () -> MT.Superconductor, 15, 1, 4, 1, 1, false, false, false)
	);

	/**
	 * One derived registrable variant: a (row, form, size) triple with the ratings the block
	 * carrier picks up — the 1.20.1 counterpart of one upstream {@code aRegistry.add(...)} NBT row
	 * (NBT_PIPESIZE / NBT_PIPEBANDWIDTH / NBT_PIPELOSS / NBT_DIAMETER + max stack).
	 */
	public record Variant(Row row, boolean insulated, int size, long voltage, long amperage, long loss,
			int diameter, int maxStack) {}

	/**
	 * The full derived spectrum, upstream row order preserved (the registration and creative-tab
	 * order): per cable row 16 bare wires + 5 cables, per pure-wire row 16 bare wires — the 620.
	 */
	public static List<Variant> variants() {
		List<Variant> rVariants = new ArrayList<>(620);
		for (Row tRow : ROWS) {
			for (int tSize = 1; tSize <= 16; tSize++) { // :72-87 wireGt01..16
				rVariants.add(new Variant(tRow, false, tSize, tRow.voltage(), tRow.amperage() * tSize,
						tRow.lossWire(), WIRE_DIAMETERS[tSize - 1], 64 / tSize));
			}
			if (tRow.cable()) for (int tCable = 0; tCable < CABLE_MULTIPLIERS.length; tCable++) { // :89-93 cableGt01/02/04/08/12
				int tSize = CABLE_MULTIPLIERS[tCable];
				rVariants.add(new Variant(tRow, true, tSize, tRow.voltage(), tRow.amperage() * tSize,
						tRow.lossCable(), CABLE_DIAMETERS[tCable], CABLE_STACKS[tCable]));
			}
		}
		return rVariants;
	}

	/** The census yardstick: 28 cable rows × 21 + 2 pure-wire rows × 16. */
	public static final int EXPECTED_VARIANTS = 620;

	/**
	 * The registry path of a variant — {@code wire_<material>_gt<NN>} / {@code cable_<material>_gt<NN>}
	 * (the snake-cased material internal name, the GTMaterialItems.itemIdOf composition rule).
	 */
	public static String registryName(Variant aVariant) {
		return (aVariant.insulated() ? "cable_" : "wire_") + aVariant.row().token() + "_gt"
				+ (aVariant.size() < 10 ? "0" : "") + aVariant.size();
	}

	/** The display name — the upstream row string {@code "1x " + aMat.getLocal() + " Wire"/"Cable"} (:72/:89). */
	public static String displayName(Variant aVariant) {
		OreDictMaterial tMaterial = aVariant.row().material().get();
		String tLocal = tMaterial == null || tMaterial.mNameLocal == null ? aVariant.row().idComment() : tMaterial.mNameLocal;
		return aVariant.size() + "x " + tLocal + (aVariant.insulated() ? " Cable" : " Wire");
	}

	/** The snake-cased material token of a row (the command-selector material part, e.g. {@code "tin"}, {@code "carborundum"}). */
	public static String materialToken(Row aRow) {
		return aRow.token();
	}

	/**
	 * The command/census selector: the variant with the given material token, size and form, or
	 * null. Case-insensitive on the token (RCON scripts use plain lowercase).
	 */
	public static Variant find(String aMaterialToken, int aSize, boolean aInsulated) {
		for (Variant tVariant : variants()) {
			if (tVariant.size() == aSize && tVariant.insulated() == aInsulated
					&& materialToken(tVariant.row()).equalsIgnoreCase(aMaterialToken)) return tVariant;
		}
		return null;
	}

	private GTWireSpecs() {}

	static {
		// fail fast at the first real use if the table drifts from the pinned split (28 cable + 2 pure-wire)
		long tCableRows = ROWS.stream().filter(Row::cable).count();
		if (tCableRows != 28) throw new IllegalStateException(
				MOD_ID + " wire table: expected 28 cable rows (Loader:1914-1946), found " + tCableRows);
		if (ROWS.size() != 30) throw new IllegalStateException(
				MOD_ID + " wire table: expected 30 material rows (Loader:1914-1950), found " + ROWS.size());
	}
}
