package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

//? if forge {
import net.minecraftforge.fml.ModList;
//?} else {
/*import net.neoforged.fml.ModList;
 *///?}

import gregapi.oredict.OreDictMaterial;

/**
 * The unified mod-presence driver (issue #46, the mdh card series: mdh-1 driver core →
 * mdh-2 foreign-material atlas → mdh-3 clear-out wave → mdh-4 migration closeout).
 *
 * <p><b>User ruling (2026-10-01):</b> one unified mod-driven registration control point,
 * extensible to further condition-registered families — the port's single place where
 * "is mod X present" decides whether content registers (upstream spread this over
 * {@code ModData.mLoaded} + {@code CompatMods.java:35 if(mMod.mLoaded)}, never ported).
 *
 * <p><b>ADR-MDH1 — default all-PRESENT, zero behaviour change:</b> the override table starts
 * empty and every unknown modid answers "loaded", so with no seeded state every registration
 * mount sees exactly the pre-driver universe (pair-for-pair identical
 * {@link GTMaterialItems#registrationOrder()}, every fluid spec row registered). Content is
 * only ever hidden by an explicit ABSENT row — seeded from the environment here (real
 * installs only; the mdh-3 batches land the actual row deletions), never by an implicit
 * "mod not installed" walk.
 *
 * <p><b>ADR-MDH3 — this class is the ONLY ModList reference allowed in gregtech6 main
 * source:</b> the fmlcore ModList.INSTANCE is a private static and cannot be faked, and the
 * offline test JVM has no FML at all (GTOfflineTestBase.java:35-46) while the neo leg boots
 * one holding only gt6 + system mods — so tests inject through {@link #setDriver(String,
 * DriverLevel)} / {@link #reset()} instead (the upstream ModData.java:52-55 setLoaded seam
 * precedent). Runtime seeding happens once, from the GT6Mod constructor
 * ({@link #seedFromEnvironment()}): fmlcore binds ModList during
 * ModLoader.gatherAndInitializeMods (ModLoader.java:137-143), which strictly precedes any
 * @Mod construction, on both legs (the neo delta is only the package name).
 *
 * <p><b>Convergence boundary (coordinator ruling 2026-10-01, the semantic acceptance
 * form):</b> registration-face presence decisions (this class's isLoaded shape) converge
 * here and nowhere else. The pre-existing ModList references stay put, each a different
 * domain: {@code GTViewerJump.isLoaded("jei"/"emi")} is a runtime UI probe that needs the
 * TRUE query (absent mod → false) — the exact inverse of this driver's registration
 * default (unknown → visible), so migrating it would flip the jump gate open on a
 * JEI-less install (mdh-4 keeps); the five {@code ModList.get().getModContainerById("gt6")
 * .getEventBus()} call sites (GTMenuTypes/GTBasicMachinesMenus/GTOvenMenus/GT6Covers/
 * GT6LubricantBucket) are mod-bus plumbing, not presence decisions (kept, the vendored
 * modularui precedent); {@code GT6EmiPlugin} mentions ModList in javadoc only.
 *
 * <p>Mounts (mdh-1 wiring, both zero-effect by default): the material item universe filters
 * through {@link #visibilityGate()} in {@code GTMaterialItems.enumerate()} (after the
 * isGeneratingItem criterion, GTMaterialItems.java:176), and the fluid spec families carry a
 * per-row {@code driverDomain} consulted in their registration cores (GTFluids
 * engineFluid/registerFluidFamily/specFluid). A registration condition family that is not
 * mod-presence (item_exists, ...) composes into {@link #visibilityGate()} here too — one
 * ruling face, no second fork (mdh-4 keeps the orthogonal condition systems where they are).
 */
public final class GT6ModDrivers {

	/** The two answer states a modid can be pinned to. */
	public enum DriverLevel {PRESENT, ABSENT}

	/** The modid → level override table. Empty = all-PRESENT (ADR-MDH1); runtime writes only via {@link #seedFromEnvironment}. */
	private static final Map<String, DriverLevel> OVERRIDES = new LinkedHashMap<>();

	/**
	 * The atlas domain set the environment seed walks. Still INERT at the preconditions
	 * commit (mdh-clearout-batch2): the re-activation lands after the three review-seat-XVI
	 * preconditions — (1) the datagen-JVM short-circuit below (registration precedes
	 * GatherDataEvent, so a seeded runData JVM cannot be un-shrunk by a late reset), (2) the
	 * datagen-walk null-drop sweep over every GTMaterialItems.get call site the providers
	 * walk, (3) the committed-tree byte-identity verdict. mdh-2's activation attempt
	 * (SEEDED_DOMAINS = atlas.seedableDomains()) NPE'd GT6CraftingRecipes.hopperRecipeBuilder
	 * in the runData JVM exactly because those were missing (f8ffa0bd1).
	 */
	private static final List<String> SEEDED_DOMAINS = List.of();

	/** One-shot latch: the environment is read once, at mod construct (upstream ModData.mLoaded timing). */
	private static boolean seeded = false;

	/**
	 * The datagen-context probe (activation precondition 1). The platform default reads
	 * {@code DatagenModLoader.isRunningDataGen()} — set to true BEFORE the mod bootstrapping
	 * on both legs (forge: {@code DatagenModLoader.begin} sets the flag then calls
	 * {@code ModLoader.gatherAndInitializeMods}, DatagenModLoader.java:41-43; neo 21.1: the
	 * same public flag, {@code begin()} puts it before the
	 * {@code CommonModLoader.begin → gatherAndInitializeMods} chain, bytecode-verified) — so
	 * at the GT6Mod constructor the flag already answers "datagen" in a runData JVM and the
	 * seed short-circuits: datagen always walks the default full universe and the committed
	 * tree never drifts with the environment (acceptance: offline runData byte-identical).
	 * ponytail: seam field because no offline JVM can flip the platform flag — the walk-leg
	 * test injects {@code true} to pin the short-circuit itself.
	 */
	private static java.util.function.BooleanSupplier datagenProbe = GT6ModDrivers::inDatagenJvm;

	/**
	 * Material → owning modid, {@code null} = unattributed = always visible. The production
	 * source is the mdh-2 {@link GT6ForeignMaterialAtlas}: PRIMARY rows answer their domain,
	 * COMMON_SECONDARY/GT6_SELF rows answer null and never hide (ADR-MDH2). Tests swap in
	 * fixtures via {@link #setMaterialDomain(Function)}; {@code null} restores the atlas.
	 */
	private static Function<OreDictMaterial, String> materialDomain = GT6ForeignMaterialAtlas::domainOf;

	private GT6ModDrivers() {
	}

	/** Is the given modid loaded? Unknown (and unattributed/null) domains answer TRUE — ADR-MDH1. */
	public static boolean isLoaded(String aModid) {
		DriverLevel tLevel = aModid == null ? null : OVERRIDES.get(aModid);
		return tLevel == null || tLevel == DriverLevel.PRESENT;
	}

	/** The row decision for one material: unattributed materials are always visible. */
	public static boolean isVisible(OreDictMaterial aMaterial) {
		return isLoaded(aMaterial == null ? null : materialDomain.apply(aMaterial));
	}

	/**
	 * The extensible registration-condition face (the user ruling's control-plane entry):
	 * what {@code GTMaterialItems.enumerate()} consults per material pair. Default = plain
	 * mod-presence over {@link #materialDomain}; future condition families compose here,
	 * keeping the mounts single-consumer.
	 */
	public static Predicate<OreDictMaterial> visibilityGate() {
		return GT6ModDrivers::isVisible;
	}

	/**
	 * One-time environment seed, called from the GT6Mod constructor: pin ABSENT for every
	 * {@link #SEEDED_DOMAINS} entry the live mod list lacks. Two no-op guards: no FML
	 * (offline test JVM, ModList == null — never touch, ADR-MDH1), and a test JVM with a
	 * live FML (the neo junit-fml boot: it constructs this mod and registers everything
	 * with a mod list holding only gt6 + system mods — the foreign-mod absences there are
	 * harness artifacts, and seeding them would shrink the pinned registration universe
	 * per-leg; the junit classpath probe keeps the seed inert, a real install never ships
	 * junit on the game classpath).
	 */
	public static void seedFromEnvironment() {
		if (seeded) return;
		seeded = true;
		if (datagenProbe.getAsBoolean()) return; // datagen JVM: the walk face generates the default tree, never a seeded one (precondition 1)
		ModList tModList = ModList.get();
		if (tModList == null) return; // offline: no FML instance — never touch, never hide (ADR-MDH1)
		if (inUnitTestJvm()) return; // junit-fml boot: harness artifact mod list, not a user install
		for (String tModid : SEEDED_DOMAINS) {
			if (!tModList.isLoaded(tModid)) OVERRIDES.put(tModid, DriverLevel.ABSENT);
		}
	}

	/**
	 * True when this JVM runs the data generators. The platform flag is live before any mod
	 * constructs (see {@link #datagenProbe}); the class ships in the loader universal jar on
	 * both legs, so the reference is safe in every game JVM. Leg delta is only the package
	 * (net.minecraftforge.data.loading vs net.neoforged.neoforge.data.loading).
	 */
	private static boolean inDatagenJvm() {
		//? if forge {
		return net.minecraftforge.data.loading.DatagenModLoader.isRunningDataGen();
		//?} else {
		/*return net.neoforged.neoforge.data.loading.DatagenModLoader.isRunningDataGen();
		 *///?}
	}

	/**
	 * True when junit-jupiter rides the game classpath. ponytail: classpath probe, not an
	 * FML testing flag — a hypothetical install that jar-in-jars junit would just stay in
	 * the safe all-PRESENT default (nothing ever hides), which is the fail-visible side.
	 */
	private static boolean inUnitTestJvm() {
		try {
			Class.forName("org.junit.jupiter.api.Test", false, GT6ModDrivers.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException aE) {
			return false;
		}
	}

	/** Test seam (ADR-MDH3): pin one modid's level, overriding any seeded state. */
	public static void setDriver(String aModid, DriverLevel aLevel) {
		if (aModid == null) throw new IllegalArgumentException("driver modid must be named");
		OVERRIDES.put(aModid, aLevel);
	}

	/** Test seam (ADR-MDH3): swap the material attribution source; {@code null} restores the production atlas lookup. */
	public static void setMaterialDomain(Function<OreDictMaterial, String> aLookup) {
		materialDomain = aLookup == null ? GT6ForeignMaterialAtlas::domainOf : aLookup;
	}

	/** Test seam: swap the datagen probe; {@code null} restores the platform flag read. */
	public static void setDatagenProbe(java.util.function.BooleanSupplier aProbe) {
		datagenProbe = aProbe == null ? GT6ModDrivers::inDatagenJvm : aProbe;
	}

	/** Test seam (ADR-MDH3): full pristine restore — empty table, atlas attribution, seed latch cleared. */
	public static void reset() {
		OVERRIDES.clear();
		materialDomain = GT6ForeignMaterialAtlas::domainOf;
		datagenProbe = GT6ModDrivers::inDatagenJvm;
		seeded = false;
	}
}
