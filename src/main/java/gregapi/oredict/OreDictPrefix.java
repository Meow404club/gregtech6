/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictPrefix.java (upstream 577 lines),
 * by task gt-ore-prefix.
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
import static gregapi.data.CS.U;
import static gregapi.data.CS.T;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gregapi.code.HashSetNoNulls;
import gregapi.code.ICondition;
import gregapi.code.ITagDataContainer;
import gregapi.code.TagData;

/**
 * @author Gregorius Techneticies
 *
 * Pure-data port of the OreDictPrefix model. Everything kept here is upstream verbatim
 * (line references in the member comments); the MC-coupled members were cut, see the
 * "MC coupling" note below.
 *
 * MC coupling cut versus upstream (OreDictPrefix.java:37-42 imports; policy: item/recipe/
 * rendering state moves to Phase-2 item registration):
 * - mCreativeTab (:72, CreativeTabs), mContainerItem (:74, ItemStack) — removed.
 * - mShapelessManagersSingle/mShapelessManagers (:81-82, crafting managers) — removed.
 * - mAspects + aspects()/addAspects() (:90, :412-446, Thaumcraft) — removed (Phase 2).
 * - applyAllStackSizes()/applyStackSizes() (:205-314, vanilla Item.setMaxStackSize calls) — removed.
 * - addTextureSet(...) (:398-410, TextureSet icon registration) — removed; mNameTextureSet and
 *   the mIconIndexBlock/mIconIndexItem ints are kept as pure data for the Phase-2 renderer.
 * - mat(...) (:448-462, ItemStack creation) — removed.
 * - onOreRegistration + listener buffering (:375-392, :468-511) — removed (the oredict event
 *   system is MC-facing); the ItemStack-typed mRegisteredItems/mRegisteredPrefixItems
 *   (:513-515) are removed with it; mRegisteredMaterials (:516) is kept as pure data.
 * - dat()/get(OreDictMaterial) (:555-563, OreDictItemData) — removed with OreDictItemData.
 */
public final class OreDictPrefix implements ITagDataContainer<OreDictPrefix>, ICondition<ITagDataContainer<?>> {
	/**
	 * Phase-1 tag seam (same pattern as OreDictMaterial.TDG from task gt-material-model): the
	 * TD.Prefix group and TD.ItemGenerator.ORES are not ported yet (TD.java is outside this
	 * card's FILES_SCOPE), so the six tags the model itself references are created here with the
	 * exact upstream keys and display names (TD.java:226-236 Prefix, TD.java:551 ItemGenerator).
	 * TagData.createTagData is idempotent by uppercase key (TagData.java:77-82), so once the real
	 * TD groups are ported these become the very same instances automatically.
	 */
	private static final TagData
	PREFIX_UNUSED     = TagData.createTagData("PREFIX.PREFIX_UNUSED"    , "Unused Prefix"),        // TD.java:226
	MATERIAL_BASED    = TagData.createTagData("PREFIX.MATERIAL_BASED"   , "Material Based"),       // TD.java:229
	UNIFICATABLE      = TagData.createTagData("PREFIX.UNIFICATABLE"     , "Unificatable"),         // TD.java:231
	TOOLTIP_ENCHANTS  = TagData.createTagData("PREFIX.TOOLTIP_ENCHANTS" , "Enchantment Tooltip"),  // TD.java:236
	ORE               = TagData.createTagData("PREFIX.ORE"              , "Ore");                  // TD.java:241
	/** Upstream static import gregapi.data.TD.ItemGenerator.ORES (OreDictPrefix.java:47, TD.java:551). */
	public static final TagData ORES = TagData.createTagData("ITEMGENERATOR.ORES");

	/** This List is sorted by the length of the Prefixes. */ // :55-56
	public static final List<OreDictPrefix> VALUES_SORTED = new ArrayList<>(); // upstream ArrayListNoNulls; only the constructor adds here (never null)
	/** This is to prevent smaller Prefixes from breaking larger ones by just starting with the same few Characters the larger one starts with. */ // :57-58
	static final List<OreDictPrefix> VALUES_SORTED_INTERNAL = new ArrayList<>(); // upstream :58 private; widened to package-private so PrefixRegistry.reset() can clear it (upstream never resets)
	/** The List of all Values in the order they have been added. not really sorted at all. */ // :59-60
	public static final List<OreDictPrefix> VALUES = new ArrayList<>(); // upstream ArrayListNoNulls; only the constructor adds here (never null)
	/** The Map containing all Prefixes by their Name. */ // :61-62
	public static final Map<String, OreDictPrefix> sPrefixes = new HashMap<>();
	/** The Map containing all Prefixes by their Name. */ // :63-64 — upstream comment says "Prefixes", it actually caches get() parse results, including misses (negative cache).
	public static final Map<String, OreDictPrefix> sParsed = new HashMap<>();

	/** The Tags for this Prefix. */ // :66
	private final Set<TagData> mTags = new HashSetNoNulls<>();
	/** Materials that are ignored on Registration, generate no Items, or are forced to generate Items. */ // :67
	private final Set<OreDictMaterial> mItemGeneratorBlackList = new HashSetNoNulls<>(), mIgnoredRegistrations = new HashSetNoNulls<>(), mItemGeneratorForced = new HashSetNoNulls<>();
	public final String mNameInternal; // :70
	/** The Re-Registration for the Ore Dictionary for invalid Prefixes. */ // :71
	public OreDictPrefix mTargetRegistration = this;
	/** Upstream :75-76 raw-typed field; kept raw because setCondition stores TagData conditions as well as OreDictPrefix conditions. */
	@SuppressWarnings("rawtypes")
	private ICondition mCondition = ICondition.TRUE;
	/** The Indices of the Icons inside the Texture Sets. -1 if it doesn't have a Set. Pure data for the Phase-2 renderer (upstream :77-78). */
	public int mIconIndexBlock = -1, mIconIndexItem = -1; // :78
	/** Upstream CS.java:856 "STATE_SOLID = 0"; local constant because CS is outside this card's FILES_SCOPE. */
	public static final byte STATE_SOLID = 0;
	public byte mConfigStackSize = 64, mDefaultStackSize = 64, mMinimumStackSize = 1, mState = STATE_SOLID; // :79
	public long mAmount = -1, mWeight = U; // :80
	public float mHeatDamage = 0.0F; // :83
	/** The naming template pieces. mMaterialPre/mMaterialPost bracket the Material name; upstream consumption is LanguageHandler.getLocalName (:158, universal fallback at :579). */ // :84
	public String mNameLocal, mMaterialPre, mMaterialPost, mNameCategory, mNameTextureSet;
	/** List of Prefixes which are familiar to this Prefix. Like "dust" having "dustSmall" and "dustTiny" and vice versa. Note that this per Default also contains the Prefix itself inside this Set. */ // :85-86
	public final Set<OreDictPrefix> mFamiliarPrefixes = new HashSetNoNulls<>();
	/** Secondary Materials of this Prefix. OreDictMaterialStacks are In Material Units */ // :87-88
	public final List<OreDictMaterialStack> mByProducts = new ArrayList<>(); // upstream ArrayListNoNulls (gregapi/code is outside this card's FILES_SCOPE, see OreDictConfigurationComponent.java:33): it dropped null elements silently; upstream adders (OM.stack) never pass null, so the observable behavior is identical
	/** Materials that have been registered with this Prefix. Upstream :516 (the ItemStack-typed sibling sets are MC-coupled and cut). */
	public final Set<OreDictMaterial> mRegisteredMaterials = new HashSetNoNulls<>();

	public static OreDictPrefix createPrefix(String aName) { // :92-97, GAPI.mStartedInit -> PrefixRegistry state machine
		if (!PrefixRegistry.INSTANCE.isOpen()) throw new IllegalStateException("Prefixes have to be initialised in PreInit or earlier!");
		String tName = aName.replaceAll(" ", "").replaceAll("-", ""); // Auto-Replace all Spaces and Minuses.
		OreDictPrefix rPrefix = sPrefixes.get(tName);
		return rPrefix == null ? new OreDictPrefix(tName, aName) : rPrefix;
	}

	/** Upstream :99-118, verbatim. Visibility widened private -> package-private: the registry entry point and the data batches (same package) instantiate this class. */
	OreDictPrefix(String aNameInternal, String aNameLocal) {
		mNameInternal = aNameInternal;
		mNameTextureSet = mNameInternal;
		mNameCategory = mNameLocal = aNameLocal;
		if (mNameInternal.contains("|") || mNameInternal.contains("*") || mNameInternal.contains(":") || mNameInternal.contains(".") || mNameInternal.contains("$")) throw new IllegalArgumentException("The Prefix Name contains invalid Characters!");
		if (mNameInternal.length() < 3) throw new IllegalArgumentException("A Prefix must have at least 3 Characters, otherwise it would break other Prefixes way to easily.");
		VALUES.add(this);
		if (VALUES_SORTED_INTERNAL.isEmpty()) {
			VALUES_SORTED_INTERNAL.add(this);
			VALUES_SORTED.add(this);
		} else for (int i = 0, j = VALUES_SORTED_INTERNAL.size(); i < j; i++) {
			if (mNameInternal.length() >= VALUES_SORTED_INTERNAL.get(i).mNameInternal.length()) {
				VALUES_SORTED_INTERNAL.add(i, this);
				VALUES_SORTED.add(i, this);
				break;
			}
		}
		sPrefixes.put(mNameInternal, this);
		mFamiliarPrefixes.add(this);
	}

	/** Upstream :120-133, verbatim. Long prefixes match first (VALUES_SORTED_INTERNAL is length-sorted by the constructor); unknown names are negative-cached in sParsed. Case-sensitive on purpose. */
	public static OreDictPrefix get(String aOre) {
		OreDictPrefix rPrefix = sParsed.get(aOre);
		if (rPrefix != null) return rPrefix;
		if (sParsed.containsKey(aOre)) return null;
		for (int i = 0, j = VALUES_SORTED_INTERNAL.size(); i < j; i++) {
			rPrefix = VALUES_SORTED_INTERNAL.get(i);
			if (aOre.startsWith(rPrefix.mNameInternal)) {
				sParsed.put(aOre, rPrefix);
				return rPrefix;
			}
		}
		sParsed.put(aOre, null);
		return null;
	}

	/** Adds Identical Names which are getting re-registered to this Prefix */ // :135-139
	public OreDictPrefix addIdenticalNames(String... aNames) {
		for (String aName : aNames) createPrefix(aName).setRegistration(this);
		return this;
	}

	public OreDictPrefix addFamiliarPrefix(OreDictPrefix aPrefix) { // :141-144
		mFamiliarPrefixes.add(aPrefix);
		return this;
	}

	public OreDictPrefix addFamiliarPrefixWithReversal(OreDictPrefix aPrefix) { // :145-147
		return addFamiliarPrefix(aPrefix.addFamiliarPrefix(this));
	}

	/** The Re-Registration for the Ore Dictionary for invalid Prefixes */ // :149-156
	public OreDictPrefix setRegistration(OreDictPrefix aPrefix) {
		if (aPrefix != null) {
			mTargetRegistration = aPrefix;
			add(PREFIX_UNUSED);
		}
		return this;
	}

	public OreDictPrefix setMaterialStats(long aMaterialAmount) { // :158-164
		add(MATERIAL_BASED);
		mAmount = aMaterialAmount;
		mWeight = aMaterialAmount;
		setStacksize(mAmount < U * 2 ? 64 : 64 / (mAmount / U));
		return this;
	}

	public OreDictPrefix setMaterialStats(long aMaterialAmount, long aMaterialWeight) { // :166-172
		add(MATERIAL_BASED);
		mAmount = aMaterialAmount;
		mWeight = aMaterialWeight;
		setStacksize(mAmount < U * 2 ? 64 : 64 / (mAmount / U));
		return this;
	}

	public OreDictPrefix setOreStats(long aMaterialWeight) { // :174-180
		add(MATERIAL_BASED, UNIFICATABLE, ORE, TOOLTIP_ENCHANTS);
		mAmount = -1;
		mWeight = aMaterialWeight;
		setCondition(ORES);
		return this;
	}

	public OreDictPrefix setConfigStacksize(long aStacksize) { // :182-186
		mConfigStackSize = (byte)bind_(mMinimumStackSize, 64, aStacksize);
		mDefaultStackSize = (byte)bind_(mMinimumStackSize, 64, aStacksize);
		return this;
	}

	public OreDictPrefix setStacksize(long aStacksize) { // :188-191
		mDefaultStackSize = (byte)bind_(mMinimumStackSize, 64, aStacksize);
		return this;
	}

	public OreDictPrefix setStacksize(long aStacksize, long aMinimumStacksize) { // :193-197
		mMinimumStackSize = (byte)bind_(1, 64, aMinimumStacksize);
		mDefaultStackSize = (byte)bind_(mMinimumStackSize, 64, aStacksize);
		return this;
	}

	public OreDictPrefix setMinStacksize(long aMinimumStacksize) { // :199-203
		mMinimumStackSize = (byte)bind_(1, 64, aMinimumStacksize);
		mDefaultStackSize = (byte)bind_(mMinimumStackSize, 64, mDefaultStackSize);
		return this;
	}

	/** Upstream UT.Code.bind_ (gregapi/util/UT.java:1547-1549), verbatim, inlined because UT.java is outside this card's FILES_SCOPE. */
	private static long bind_(long aMin, long aMax, long aBoundValue) {
		return Math.max(aMin, Math.min(aMax, aBoundValue));
	}

	public OreDictPrefix setCategoryName(String aCategoryName) { // :316-319
		mNameCategory = aCategoryName;
		return this;
	}

	public OreDictPrefix setTextureSetName(String aTextureSetName) { // :321-324 — texture-set NAME reference per the MC-coupling policy (rendering is Phase 2).
		mNameTextureSet = aTextureSetName;
		return this;
	}

	/** sets the State of things which are of this Prefix. 0 - 3 are from Solid to Plasma. Solid is Default. */ // :326-330
	public OreDictPrefix setState(long aState) {
		mState = gregapi.util.UT.Code.bind2(aState);
		return this;
	}

	public OreDictPrefix setCondition(ICondition<?> aCondition) { // :332-335
		mCondition = aCondition==null?ICondition.FALSE:aCondition;
		return this;
	}

	public OreDictPrefix setLocalItemName(String aPreMaterial, String aPostMaterial) { // :337-341
		mMaterialPre = aPreMaterial;
		mMaterialPost = aPostMaterial;
		return this;
	}

	public OreDictPrefix setLocalPrefixName(String aLocalName) { // :343-346
		mNameLocal = aLocalName;
		return this;
	}

	public OreDictPrefix ignoreMaterials(OreDictMaterial... aMaterials) { // :348-351
		if (aMaterials != null) for (OreDictMaterial aMaterial : aMaterials) if (aMaterial != null && !mIgnoredRegistrations.contains(aMaterial)) mIgnoredRegistrations.add(aMaterial);
		return this;
	}

	public OreDictPrefix disableItemGeneration(OreDictMaterial... aMaterials) { // :353-356
		if (aMaterials != null) for (OreDictMaterial aMaterial : aMaterials) if (aMaterial != null && !mItemGeneratorBlackList.contains(aMaterial)) mItemGeneratorBlackList.add(aMaterial);
		return this;
	}

	public OreDictPrefix forceItemGeneration(OreDictMaterial... aMaterials) { // :358-361
		if (aMaterials != null) for (OreDictMaterial aMaterial : aMaterials) if (aMaterial != null && !mItemGeneratorForced.contains(aMaterial)) mItemGeneratorForced.add(aMaterial);
		return this;
	}

	@SuppressWarnings("unchecked")
	public boolean isGeneratingItem(OreDictMaterial aMaterial) { // :363-366
		return aMaterial != null && (mItemGeneratorForced.contains(aMaterial) || (!mItemGeneratorBlackList.contains(aMaterial) && mCondition.isTrue(aMaterial)));
	}

	@SuppressWarnings("unchecked")
	public boolean canGenerateItem(OreDictMaterial aMaterial) { // :368-371
		return aMaterial != null && (mItemGeneratorForced.contains(aMaterial) || mCondition.isTrue(aMaterial));
	}

	public OreDictMaterialStack byproduct(int aIndex) { // :464-466
		return aIndex < mByProducts.size() ? mByProducts.get(aIndex) : null;
	}

	/**
	 * Universal naming-template application, moved here from upstream LanguageHandler.getLocalName
	 * (gregapi/lang/LanguageHandler.java:158, universal fallback at :579):
	 * "aPrefix.mMaterialPre + aMaterial.mNameLocal + aPrefix.mMaterialPost", verbatim including the
	 * absence of null-guards. The material-specific special cases (LH:160-578) live in the
	 * localisation layer and are not ported. The un-localized fallback chain stays upstream-exact:
	 * OreDictMaterial.mNameLocal already falls back to mNameInternal (OreDictMaterial.setLocal,
	 * OreDictMaterial.java:234-236 = upstream :330-333), and mMaterialPre/mMaterialPost are null
	 * until setLocalItemName runs (OP.java:49).
	 */
	public String getLocalizedName(OreDictMaterial aMaterial) {
		return mMaterialPre + aMaterial.mNameLocal + mMaterialPost;
	}

	@Override
	public boolean contains(TagData aTag) { // :524-527
		return mTags.contains(aTag);
	}

	public boolean containsAny(TagData... aTags) { // :529-532
		for (TagData aTag : aTags) if (mTags.contains(aTag)) return T;
		return F;
	}

	@Override
	public boolean containsAll(TagData... aTags) { // :534-537
		return mTags.containsAll(Arrays.asList(aTags));
	}

	@Override
	public boolean containsAll(Collection<TagData> aTags) { // :539-542
		return mTags.containsAll(aTags);
	}

	@Override
	public OreDictPrefix add(TagData... aTags) { // :544-548
		if (aTags != null) for (TagData aTag : aTags) mTags.add(aTag);
		return this;
	}

	@Override
	public boolean remove(TagData aTag) { // :550-553
		return mTags.remove(aTag);
	}

	@Override
	public String toString() { // :565-568
		return mNameInternal;
	}

	@Override
	public boolean isTrue(ITagDataContainer<?> aMaterial) { // :570-573
		return aMaterial instanceof OreDictMaterial && canGenerateItem((OreDictMaterial)aMaterial);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public final ICondition<ITagDataContainer> NOT = new ICondition.Not(this); // :575-576
}
