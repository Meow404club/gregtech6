/**
 * Ported from GregTech 6 (1.7.10), file gregapi/code/TagData.java:37-127.
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

package gregapi.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Gregorius Techneticies
 *
 * Useful for tagging things. It could Tag anything.
 * Better than Strings for tagging Stuff since you can do an == Check rather than needing to use equals.
 *
 * Port deviations from upstream (MC 1.7.10), all behavior-preserving on the pure path:
 * - gregapi.data.LH (localization store, MC chat facing) is NOT ported. The createTagData
 *   overloads keep their upstream signatures but no longer register localized names, and
 *   getLocalisedName*() always return the fallback mName, which is exactly what the upstream
 *   LH.get(key, mName) yields when nothing has been localized.
 * - NOT is typed ICondition&lt;ITagDataContainer&gt; instead of upstream's raw
 *   ICondition&lt;OreDictMaterial&gt; (upstream TagData.java:46 used a raw type + unchecked cast).
 *   Runtime behavior is identical; this removes the compile dependency on
 *   gregapi.oredict.OreDictMaterial, which is owned by task card gt-material-model.
 */
public final class TagData implements ICondition<ITagDataContainer> {
	private static final List<TagData> TAGS_INTERNAL = new ArrayList<>();
	public static final List<TagData> TAGS = new ArrayList<>();

	public final int mTagID;
	public final String mName;
	public String mChatFormat = "";

	public final ICondition<ITagDataContainer> NOT = new ICondition.Not<>(this);

	public final List<TagData> AS_LIST = Collections.unmodifiableList(Arrays.asList(this));

	private TagData(String aName) {
		mTagID = TAGS_INTERNAL.size();
		mName = aName;
		TAGS_INTERNAL.add(this);
		TAGS.add(this);
	}

	public static TagData createTagData(String aName, String aLocalShort, String aLocalLong, String aChatFormat) {
		TagData rTagData = createTagData(aName, aLocalShort, aLocalLong);
		rTagData.mChatFormat = aChatFormat;
		return rTagData;
	}

	/** Upstream registered aLocalShort/aLocalLong via LH.add; localization is deferred to Phase 2. */
	public static TagData createTagData(String aName, String aLocalShort, String aLocalLong) {
		return createTagData(aName);
	}

	public static TagData createTagData(String aName, String aLocal) {
		return createTagData(aName, aLocal, aLocal);
	}

	public static TagData createTagData(String aName) {
		aName = aName.toUpperCase();
		for (TagData tSubTag : TAGS_INTERNAL) if (tSubTag.mName.equals(aName)) return tSubTag;
		return new TagData(aName);
	}

	public String getTranslatableNameLong() {
		return "gt.td.long."+mName.toLowerCase();
	}

	/** Upstream: LH.get(getTranslatableNameLong(), mName). Un-localized fallback is mName. */
	public String getLocalisedNameLong() {
		return mName;
	}

	public String getLocalisedChatNameLong() {
		return getChatFormat() + getLocalisedNameLong();
	}

	public String getTranslatableNameShort() {
		return "gt.td.short."+mName.toLowerCase();
	}

	/** Upstream: LH.get(getTranslatableNameShort(), mName). Un-localized fallback is mName. */
	public String getLocalisedNameShort() {
		return mName;
	}

	public String getLocalisedChatNameShort() {
		return getChatFormat() + getLocalisedNameShort();
	}

	public String getChatFormat() {
		return mChatFormat;
	}

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public boolean isTrue(ITagDataContainer aObject) {
		return aObject.contains(this);
	}

	@Override
	public boolean equals(Object aObject) {
		return (aObject instanceof TagData && ((TagData)aObject).mTagID == mTagID);
	}

	@Override
	public int hashCode() {
		return mTagID;
	}
}
