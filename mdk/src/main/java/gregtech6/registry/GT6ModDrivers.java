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
	 * The atlas domain set the environment seed walks (mdh-2 filled it from
	 * {@link GT6ForeignMaterialAtlas#seedableDomains()}): every domain not present in the
	 * live mod list flips to ABSENT at seed time — but never inside a test JVM (the guard
	 * below), where the foreign-mod absences are harness artifacts, not user installs.
	 */
	private static final List<String> SEEDED_DOMAINS = GT6ForeignMaterialAtlas.seedableDomains();

	/** One-shot latch: the environment is read once, at mod construct (upstream ModData.mLoaded timing). */
	private static boolean seeded = false;

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
		ModList tModList = ModList.get();
		if (tModList == null) return; // offline: no FML instance — never touch, never hide (ADR-MDH1)
		if (inUnitTestJvm()) return; // junit-fml boot: harness artifact mod list, not a user install
		for (String tModid : SEEDED_DOMAINS) {
			if (!tModList.isLoaded(tModid)) OVERRIDES.put(tModid, DriverLevel.ABSENT);
		}
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

	/** Test seam (ADR-MDH3): full pristine restore — empty table, atlas attribution, seed latch cleared. */
	public static void reset() {
		OVERRIDES.clear();
		materialDomain = GT6ForeignMaterialAtlas::domainOf;
		seeded = false;
	}
}
