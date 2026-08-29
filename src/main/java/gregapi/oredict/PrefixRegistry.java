/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictPrefix.java:55-64 (static registry
 * state) and :92-93 (GAPI.mStartedInit gate), by task gt-ore-prefix.
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

/**
 * @author Gregorius Techneticies
 *
 * Resettable prefix registry carrying the lifecycle state of the static prefix collections
 * (OreDictPrefix.java:55-64), in the same open/closed/reset style as MaterialRegistry
 * (task gt-material-model). Architectural change replacing the GAPI.mStartedInit coupling at
 * OreDictPrefix.java:93 (and GAPI.mStartedPostInit at :375/:385, whose listener machinery is
 * MC-coupled and not ported): open() = registration phase (upstream PreInit or earlier),
 * close() = locked, reset() wipes all registered prefixes and re-opens the registry, so tests
 * and the Phase-2 datagen batches can run cleanly one after another.
 *
 * Unlike materials (OreDictMaterial.java:153-155, where name-only creation stays legal while
 * closed), upstream createPrefix throws unconditionally once init has started (:92-93), so
 * closed blocks every prefix creation here as well.
 *
 * The collections themselves stay public static finals on OreDictPrefix (upstream call-site
 * shape, e.g. OP.java:584/:622 iterate OreDictPrefix.VALUES); this class only owns their
 * lifecycle.
 */
public class PrefixRegistry {
	/** The default registry backing the OreDictPrefix static delegate. */
	public static final PrefixRegistry INSTANCE = new PrefixRegistry();

	private boolean mOpen = T;

	public PrefixRegistry() {
	}

	/** Registration phase: prefixes may be created (upstream: anything before/at PreInit). */
	public void open() {
		mOpen = T;
	}

	/** Locks the registry; createPrefix now throws (upstream: GAPI.mStartedInit == T at OreDictPrefix.java:93). */
	public void close() {
		mOpen = F;
	}

	/** @return if prefixes can still be registered. */
	public boolean isOpen() {
		return mOpen;
	}

	/** Wipes all registered prefixes and re-opens the registry. The parse cache (sParsed) is cleared as well, so a fresh batch re-parses from scratch. */
	public void reset() {
		OreDictPrefix.VALUES_SORTED.clear();
		OreDictPrefix.VALUES_SORTED_INTERNAL.clear();
		OreDictPrefix.VALUES.clear();
		OreDictPrefix.sPrefixes.clear();
		OreDictPrefix.sParsed.clear();
		mOpen = T;
	}
}
