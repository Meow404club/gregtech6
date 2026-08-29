/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictMaterial.java:142-206 and :321-328
 * (registration region), by task gt-material-model.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.oredict;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gregapi.code.HashSetNoNulls;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.util.UT;

/**
 * @author Gregorius Techneticies
 *
 * Resettable material registry carrying the upstream static registration state of OreDictMaterial
 * (MATERIAL_MAP :49, FLUID_MAP :51, MATERIAL_ARRAY :52, ALLOYS :53) plus the createMaterial /
 * get / sanitize methods (:142-206).
 *
 * Architectural change (decided, replaces the GAPI.mStartedInit coupling at :154): the registry
 * has an open/closed state machine. open() = registration phase (like upstream PreInit or
 * earlier), close() = locked (upstream post-PreInit), and reset() wipes all registered state and
 * re-opens the registry, so tests and the Phase-2 datagen batches can run cleanly one after
 * another. Exactly like upstream, only materials with a valid ID (aID >= 0) are gated by the
 * state machine; name-only lookups/creations (aID < 0) stay legal while closed (:153-155).
 *
 * The INSTANCE singleton backs the static delegates on OreDictMaterial, so existing call sites
 * keep working; instance copies are used by tests.
 */
public class MaterialRegistry implements MaterialStackSerializer.MaterialResolver {
	/** The default registry backing the OreDictMaterial static delegates. */
	public static final MaterialRegistry INSTANCE = new MaterialRegistry();

	/** Upstream CS.java:138 "W = OreDictionary.WILDCARD_VALUE"; kept as a local constant because CS.W is outside this card's FILES_SCOPE. */
	public static final short W = 32767;
	/** Upstream CS.java:99 Ch_N (digits); local copy, same reason. */
	private static final Set<Character> CHARS_NUMBERS = new HashSet<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
	/** Upstream OreDictMaterial.java:55, verbatim. */
	private static final Set<String> INVALID_STRINGS_TO_START_A_MATERIAL_NAME = new HashSetNoNulls<>(Arrays.asList("Mul", "Div", "Rich", "Poor", "Raw", "Impure", "Pure", "Dirty", "Refined", "Tiny", "Small", "Normal", "Medium", "Large", "Huge", "Dense", "Alloy", "Head", "Tool", "Helmet", "Chestplate", "Leggings", "Boots", "Centrifuged", "Purified", "Quintuple", "Quadruple", "Triple", "Double", "Hot", "Uncut", "Polished", "Chipped", "Flawed", "Flawless", "Exquisite", "Gt", "Long", "Plasma", "Gas", "Liquid", "Solid", "Gem", "Dust", "Ingot", "Plate", "Block", "Leaves", "Sapling", "Mossy", "Brick", "Crack", "Chisel", "Broken", "Compact", "Curve", "Mixed", "Mixable"));

	/** Upstream CS.java:853 ERR is a LogBuffer PrintStream; plain System.err is the pure-Java equivalent. Package-visible so OreDictMaterial warnings share the same outlet. */
	static final PrintStream ERR_LOG = System.err;
	private static final PrintStream ERR = ERR_LOG;

	/** The OreDictionary Name to Material mapping. */ // OreDictMaterial.java:49
	public final Map<String, OreDictMaterial> MATERIAL_MAP = new HashMap<>();
	/** The Amount of the Stack represents how many Liters make up one Unit. */ // OreDictMaterial.java:51
	public final Map<String, OreDictMaterialStack> FLUID_MAP = new HashMap<>();
	/** The Array of Materials by ID; negative IDs never enter it. */ // OreDictMaterial.java:52
	public final OreDictMaterial[] MATERIAL_ARRAY = new OreDictMaterial[32767];
	/** All registered Alloys. Filled by addAlloyingRecipe, which is deferred to the dataset card. */ // OreDictMaterial.java:53
	public final Set<OreDictMaterial> ALLOYS = new HashSetNoNulls<>();

	private boolean mOpen = T;

	public MaterialRegistry() {
	}

	/** Registration phase: materials with a valid ID may be created (upstream: anything before/at PreInit). */
	public void open() {
		mOpen = T;
	}

	/** Locks the registry; createMaterial with a valid ID now throws (upstream: GAPI.mStartedInit == T at :154). */
	public void close() {
		mOpen = F;
	}

	/** @return if materials with a valid ID can still be registered. */
	public boolean isOpen() {
		return mOpen;
	}

	/** Wipes all registered state and re-opens the registry. The creation-order hash counter on OreDictMaterial stays monotonic on purpose, so hash codes never collide across resets. */
	public void reset() {
		MATERIAL_MAP.clear();
		FLUID_MAP.clear();
		Arrays.fill(MATERIAL_ARRAY, null);
		ALLOYS.clear();
		mOpen = T;
	}

	/** Registers a freshly constructed Material into the maps (upstream constructor :325-326). */
	void register(OreDictMaterial aMaterial) {
		MATERIAL_MAP.put(aMaterial.mNameInternal, aMaterial);
		if (aMaterial.mID >= 0) MATERIAL_ARRAY[aMaterial.mID] = aMaterial;
	}

	/**
	 * Upstream OreDictMaterial.createMaterial (:142-176), verbatim semantics. Only the
	 * GAPI.mStartedInit check (:154) is replaced by the registry state machine.
	 */
	public OreDictMaterial createMaterial(int aID, String aNameOreDict, String aLocalName) {
		aID = (aID < 0 || aID >= MATERIAL_ARRAY.length || aID == W ? -1 : aID);
		// Replace all Spaces and Minuses, and capitalise the String.
		aNameOreDict = sanitize(aNameOreDict);
		// That would cause really bad shit to happen.
		if (aNameOreDict.isEmpty())
			throw new IllegalArgumentException("This OreDict Name is not usable, due to being an empty String, after stripping all the minuses and spaces.");
		// Those Characters are used to declare private Prefixes, it is not a good Idea to use them as Material Name at all, since they would get ignored by my System automatically.
		if (aNameOreDict.contains("|") || aNameOreDict.contains("*") || aNameOreDict.contains(":") || aNameOreDict.contains(".") || aNameOreDict.contains("$"))
			throw new IllegalArgumentException("The Material Name contains at least one of the following five invalid Characters '|', '*', ':', '.' or '$'");
		// if the ID is 0 or greater, then check the Name of the Material a bit more precise, to prevent shit from happening.
		if (aID >= 0) {
			if (!mOpen)
				throw new IllegalStateException("Materials with a valid ID have to be initialised in PreInit or earlier!");

			if (CHARS_NUMBERS.contains(aNameOreDict.charAt(0)))
				throw new IllegalArgumentException("The OreDict Name '" + aNameOreDict + "' is not suitable for a valid Material. Choose a different one, which doesn't happen to start with a Numeral. You can always set the Local Name to your liking, but the internal Name must always be a proper one.");

			if (INVALID_STRINGS_TO_START_A_MATERIAL_NAME.contains(aNameOreDict))
				throw new IllegalArgumentException("The OreDict Name '" + aNameOreDict + "' is not suitable for a valid Material. Choose a different one, which doesn't happen to be a blacklisted Adjective. You can always set the Local Name to your liking, but the internal Name must always be a proper one.");

			for (String tInvalidString : INVALID_STRINGS_TO_START_A_MATERIAL_NAME) if (aNameOreDict.startsWith(tInvalidString))
				throw new IllegalArgumentException("The OreDict Name '" + aNameOreDict + "' is not suitable for a valid Material, as it conflicts with OreDict Prefixes. A better Name for your Material would be '" + UT.Code.capitalise(aNameOreDict.replaceFirst(tInvalidString, "")) + tInvalidString + "' with the '" + tInvalidString + "' at the end of the Material Name instead of the beginning.");
		}
		OreDictMaterial rMaterial1 = MATERIAL_MAP.get(aNameOreDict);
		if (rMaterial1 == null) return new OreDictMaterial(this, (short)aID, aNameOreDict, aLocalName);
		if (aID < 0) return rMaterial1;
		if (rMaterial1.mID == aID) {
			ERR.println("NOTICE: Two Materials used the same ID: " + aID + " - Names: " + aNameOreDict + " and " + rMaterial1.mNameInternal);
			return rMaterial1;
		}
		OreDictMaterial rMaterial2 = new OreDictMaterial(this, (short)aID, aNameOreDict, aLocalName);
		rMaterial1.setRegistration(rMaterial2);
		return rMaterial2;
	}

	/** Used by the OreDictManager to automatically generate an Invalid Material from a Name, with a safeguard in case the Material actually exists already. */ // OreDictMaterial.java:178-182
	public OreDictMaterial createAutoInvalidMaterial(String aNameOreDict) {
		OreDictMaterial rMaterial = createMaterial(-1, aNameOreDict, aNameOreDict);
		if (rMaterial.mID < 0) rMaterial.put(TD.Properties.INVALID_MATERIAL, TD.Properties.UNUSED_MATERIAL, TD.Properties.AUTO_BLACKLIST, TD.Properties.AUTO_MATERIAL);
		return rMaterial;
	}

	/** Upstream OreDictMaterial.get (:184-202), verbatim. */
	public OreDictMaterial get(String aMaterialName, OreDictMaterial aDefault) {
		OreDictMaterial rMaterial = MATERIAL_MAP.get(aMaterialName);
		return rMaterial == null ? aDefault : get(rMaterial);
	}

	public OreDictMaterial get(String aMaterialName) {
		return get(aMaterialName, MT.NULL);
	}

	public OreDictMaterial get(long aMaterialID, OreDictMaterial aDefault) {
		if (aMaterialID < 0 || aMaterialID >= MATERIAL_ARRAY.length || aMaterialID == W) return aDefault;
		OreDictMaterial rMaterial = MATERIAL_ARRAY[(int)aMaterialID];
		return rMaterial == null ? aDefault : get(rMaterial);
	}

	public OreDictMaterial get(long aMaterialID) {
		return get(aMaterialID, MT.NULL);
	}

	/** Follows the Registration Target chain to the Material that actually owns the slot. */ // OreDictMaterial.java:199-202
	public OreDictMaterial get(OreDictMaterial aMaterial) {
		while (aMaterial != aMaterial.mTargetRegistration) aMaterial = aMaterial.mTargetRegistration;
		return aMaterial;
	}

	/** Upstream OreDictMaterial.sanitize (:204-206), verbatim. */
	public static String sanitize(String aString) {
		return UT.Code.capitalise(aString.replaceAll(" ", "").replaceAll("-", "").replaceAll("'", "").replaceAll("/", ""));
	}

	/** MaterialResolver: stored ID -> Material (upstream is the raw MATERIAL_ARRAY access when deserializing). */
	@Override
	public OreDictMaterial byID(int aID) {
		if (aID < 0 || aID >= MATERIAL_ARRAY.length) return null;
		return MATERIAL_ARRAY[aID];
	}

	/** MaterialResolver: stored internal Name -> Material. */
	@Override
	public OreDictMaterial byName(String aName) {
		return MATERIAL_MAP.get(aName);
	}
}
