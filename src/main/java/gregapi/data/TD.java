/**
 * Minimal port of GregTech 6 (1.7.10), file gregapi/data/TD.java (upstream 615 lines).
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

package gregapi.data;

import gregapi.code.TagData;

/**
 * @author Gregorius Techneticies
 *
 * List of all default Tag Data. The Short Name is for ease of overview and stands for "Tag Data".
 *
 * Phase-1 policy (task gt-material-foundation): carry ONLY the tags later material cards
 * actually reference, with tag keys and display names matching upstream exactly
 * (TD.java:462-467 Properties, TD.java:567 ItemGenerator.LENSES). The full tag table is NOT
 * ported wholesale (red line: no giant static initialization blocks); remaining groups are
 * added incrementally as their consumers are ported. Tags depending on LH.Chat format codes
 * (e.g. the Energy group) are deferred to Phase 2 when chat formatting is wired.
 */
public class TD {
	/** Properties of a Material. */ // TD.java:461+
	public static class Properties {
		public static final TagData AUTO_BLACKLIST                          = TagData.createTagData("PROPERTIES.AUTO_BLACKLIST", "Auto Blacklist");
		public static final TagData AUTO_MATERIAL                           = TagData.createTagData("PROPERTIES.AUTO_MATERIAL", "Automatic Material");
		public static final TagData INVALID_MATERIAL                        = TagData.createTagData("PROPERTIES.INVALID_MATERIAL", "Invalid Material");
		public static final TagData UNUSED_MATERIAL                         = TagData.createTagData("PROPERTIES.UNUSED_MATERIAL", "Unused Material");
	}

	/** Which Items get generated for a Material. */ // TD.java:565+
	public static class ItemGenerator {
		public static final TagData LENSES                                  = TagData.createTagData("ITEMGENERATOR.LENSES");
	}
}
