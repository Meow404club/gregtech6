/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictMaterial.java (upstream 1512 lines),
 * by task gt-material-model (fields/registration :142-330, tag container :1431-1511).
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
import static gregapi.data.CS.U;
import static gregapi.data.CS.UD;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gregapi.code.HashSetNoNulls;
import gregapi.code.ICondition;
import gregapi.code.ITagDataContainer;
import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;
import gregapi.oredict.configurations.OreDictConfigurationComponent;
import gregapi.util.UT;

/**
 * @author Gregorius Techneticies
 *
 * MC-coupling policy applied to the upstream field region (decided, evidence :252-254/:315/:319):
 * - TextureSet/IIconContainer lists (:252) became texture-set name references (List&lt;String&gt;).
 * - Cached ITexture fields + getters (:254, :967-999) were deleted; rendering goes through the
 *   BakedModel route in Phase 2.
 * - ThaumCraft aspects (:256) are deferred to the ThaumCraft integration (no Phase-1 consumer).
 * - FluidStack references (:315) were deleted; the pure long units (:313) remain and the Phase-2
 *   fluid module recreates the stacks from the material name.
 * - Enchantment lists (:319) became {@link EnchantmentStack} (enchantment ID string + level);
 *   the MC-specific fishing-rod clamp of :1220 was stripped.
 * - Achievement list (:270) became achievement ID strings; ItemStack fields (:219, :1423) and
 *   the item listeners (:1414) were deleted.
 *
 * Registry: the upstream static maps (:49-53) and the createMaterial/get/sanitize methods
 * (:142-206) moved to the resettable {@link MaterialRegistry} (open/closed state machine
 * replacing GAPI.mStartedInit at :154); the statics below are delegates onto
 * {@link MaterialRegistry#INSTANCE} so existing call sites keep working.
 */
public final class OreDictMaterial implements ITagDataContainer<OreDictMaterial>, ICondition<OreDictMaterial> {

	public static final Map<String, OreDictMaterial> MATERIAL_MAP = MaterialRegistry.INSTANCE.MATERIAL_MAP;
	/** The Amount of the Stack represents how many Liters make up one Unit. */ // :50-51
	public static final Map<String, OreDictMaterialStack> FLUID_MAP = MaterialRegistry.INSTANCE.FLUID_MAP;
	public static final OreDictMaterial[] MATERIAL_ARRAY = MaterialRegistry.INSTANCE.MATERIAL_ARRAY;
	public static final Set<OreDictMaterial> ALLOYS = MaterialRegistry.INSTANCE.ALLOYS;
	/** The HashCode counter for Materials. Monotonic across registry resets on purpose. */ // :54
	public static int sHashID = 0;

	/** The Index of this Material inside the Array. Negative for "Not in the Array" and therefore also for "Not Unificatable", 0 is the NULL Material so a > 0 check could be useful for you. */ // :208-209
	public final short mID;
	/** The HashCode for this Material. Fully independent from any ID this Material would be assigned to, UNLIKE ITEMS. */ // :210-211
	private final int mHashID;
	/** The OreDictionary Name of this Material */ // :212-213
	public final String mNameInternal;
	/** The localised Name for this Material including Spaces and Stuff. It Defaults to the internal Name (if you have Spaces inside the Internal Name then those will be included in the Local Name but not in the final internal Name). */ // :214-215
	public String mNameLocal;
	/** The ID of the Mod, which initially created this Material. Upstream :216-217 is a ModData object; stripped to the mod ID string per the MC-coupling policy. */
	public String mOriginalMod = null;
	/** The Description inside the Material Dictionaries. */ // :220-221 (mDictionaryBook ItemStack :218-219 deleted per policy)
	public String mDescription[] = null, mTooltipChemical = null;
	/** The amount of crushed Ores you get from an Ore Block. */ // :222-223
	public byte mOreMultiplier = 1, mOreProcessingMultiplier = 1;
	/** The time 1 Unit of this Material takes to burn in a vanilla Furnace. */ // :224-225
	public long mFurnaceBurnTime = 0;
	/** The Types Tools allowed, 0 = No Tools, 1 = Flint/Stone/Wood Tools, 2 = Early Tools, 3 = Advanced Tools */ // :226-227
	public byte mToolTypes = 0;
	/** The Quality of the Material as Tool Material (ranges from 0 to 15) */ // :228-229
	public byte mToolQuality = 0;
	/** The Durability of the Material in Tool Form */ // :230-231
	public long mToolDurability = 0;
	/** The Speed of the Material as mining Material */ // :232-233
	public float mToolSpeed = 1.0F;
	public float mHeatDamage = 0.0F;
	/** If this Material is hidden */ // :235-236
	public boolean mHidden = F;
	/** If this Material contains the Metallum Aspect. */ // :237-238
	public boolean mHasMetallum = F;
	/** g/cm^3 of this Material at Room Temperature. 0 Means that it is not determined. */ // :239-240
	public double mGramPerCubicCentimeter = 1.0;
	/** The Colors of this Material in its 4 different states. Any change to these 4 final Arrays will be reflected in the Color of the Material at that state. */ // :241-243
	public final short[] mRGBaSolid = new short[] {255,255,255,255}, mRGBaLiquid = new short[] {255,255,255,255}, mRGBaGas = new short[] {255,255,255,255}, mRGBaPlasma = new short[] {255,255,255,255};
	public final short[][] mRGBa = new short[][] {mRGBaSolid, mRGBaLiquid, mRGBaGas, mRGBaPlasma};
	/** Do not modify these Colors for effects! They are supposed to be final! */ // :244-246
	public final short[] fRGBaSolid = new short[] {255,255,255,255}, fRGBaLiquid = new short[] {255,255,255,255}, fRGBaGas = new short[] {255,255,255,255}, fRGBaPlasma = new short[] {255,255,255,255};
	public final short[][] fRGBa = new short[][] {fRGBaSolid, fRGBaLiquid, fRGBaGas, fRGBaPlasma};
	/** The energetic boundaries of this Material, with somewhat reasonable Defaults for a Solid. Data in Kelvin. */ // :247-248
	public long mMeltingPoint = 1000, mBoilingPoint = 3000, mPlasmaPoint = 10000;
	/** The Atomic Values of one Molecule of this Material, Defaults to Technetium. If the Element is Antimatter, then mProtons means Antiprotons and mElectrons means Positrons */ // :249-250
	public long mNeutrons = 55, mProtons = 43, mElectrons = 43, mMass = mNeutrons + mProtons;
	/** The Texture Sets for the Materials, as texture-set name references. Upstream :251-252 is List&lt;IIconContainer&gt; seeded from TextureSet.SET_NONE, i.e. empty lists. */
	public List<String> mTextureSetsBlock = new ArrayList<>(0), mTextureSetsItems = new ArrayList<>(0);
	/** The List of Components this Material is made of */ // :257-258
	public IOreDictConfigurationComponent mComponents = null;
	/** The List of Materials this Material should register as too. This is for adding itself automatically to "OreDictionary List"-Materials such as "AnyCopper" or "AnyBronze" for example */ // :259-260
	public final Set<OreDictMaterial> mReRegistrations = new HashSetNoNulls<>(), mToThis = new HashSetNoNulls<>();
	/** The List of Materials this Material is outputting as Byproducts.*/ // :261-262
	public final List<OreDictMaterial> mByProducts = new ArrayList<>();
	/** The List of Materials which have this as Alloy Component.*/ // :263-264
	public final Set<OreDictMaterial> mAlloyComponentReferences = new HashSetNoNulls<>();
	/** The Materials which this is typically a source of. For Tooltips. */ // :265-266
	public final Set<OreDictMaterial> mSourceOf = new HashSetNoNulls<>();
	/** The List of Alloy Recipes which can create this Material. */ // :267-268
	public final List<IOreDictConfigurationComponent> mAlloyCreationRecipes = new ArrayList<>();
	/** List of Achievement IDs you get for creating an instance of this Material. Upstream :269-270 is List&lt;Achievement&gt;, stripped to ID strings per the MC-coupling policy. */
	public final List<String> mAchievementsForCreation = new ArrayList<>();
	/** Contains the most useful Prefix made of 1 Unit for this Material. */ // :271-272 (mPriorityPrefix :273-274 is OreDictPrefix-typed and belongs to task gt-ore-prefix)
	public int mPriorityPrefixIndex = 0;
	/** The Material which is the target for Recycling a Crafting Recipe. Mainly to prevent things like Iron from showing up twice in the Ingredients List */ // :275-276
	public OreDictMaterial mTargetReversing = this;
	/** The Material which is the target for Re-Registration. */ // :277-278
	public OreDictMaterial mTargetRegistration = this;
	/** The Material which is the target for selecting the preferred Tool Handle. */ // :279-280
	public OreDictMaterial mHandleMaterial = this;

	/** The Targets for certain kinds of Processing for this Material. */ // :282-295 (OM.stack(this, x) == new OreDictMaterialStack(this, x) because "this" is never null; the chain queries over these live in task gt-material-graph)
	public OreDictMaterialStack
	mTargetCrushing     = new OreDictMaterialStack(this, U),
	mTargetPulver       = new OreDictMaterialStack(this, U),
	mTargetSmelting     = new OreDictMaterialStack(this, U),
	mTargetSolidifying  = new OreDictMaterialStack(this, U),
	mTargetSmashing     = new OreDictMaterialStack(this, U),
	mTargetCutting      = new OreDictMaterialStack(this, U),
	mTargetWorking      = new OreDictMaterialStack(this, U),
	mTargetForging      = new OreDictMaterialStack(this, U),
	mTargetBurning      = new OreDictMaterialStack(this, 0), // The remaining Material when being burned. Used for getting the Ashes.
	mTargetBending      = new OreDictMaterialStack(this, U),
	mTargetCompressing  = new OreDictMaterialStack(this, U),
	mTargetGenerifying  = new OreDictMaterialStack(this, U);

	/** The Materials targetting this for certain kinds of Processing. */ // :297-310
	public final Set<OreDictMaterial>
	mTargetedCrushing     = new HashSetNoNulls<>(F, this),
	mTargetedPulver       = new HashSetNoNulls<>(F, this),
	mTargetedSmelting     = new HashSetNoNulls<>(F, this),
	mTargetedSolidifying  = new HashSetNoNulls<>(F, this),
	mTargetedSmashing     = new HashSetNoNulls<>(F, this),
	mTargetedCutting      = new HashSetNoNulls<>(F, this),
	mTargetedWorking      = new HashSetNoNulls<>(F, this),
	mTargetedForging      = new HashSetNoNulls<>(F, this),
	mTargetedBurning      = new HashSetNoNulls<>(F, this),
	mTargetedBending      = new HashSetNoNulls<>(F, this),
	mTargetedCompressing  = new HashSetNoNulls<>(F, this),
	mTargetedGenerifying  = new HashSetNoNulls<>(F, this);

	/**  */ // :312-313 (mLiquid/mGas/mPlasma FluidStacks :314-315 deleted per policy; the Phase-2 fluid module recreates them from mNameInternal)
	public long mLiquidUnit = U, mGasUnit = U, mPlasmaUnit = U;
	/** The Tags for this Material */ // :316-317
	private final Set<TagData> mTags = new HashSetNoNulls<>();
	/** Stores the Tool and Armor Enchants as (enchantment ID string, level) pairs. Upstream :318-319 is List&lt;ObjectStack&lt;Enchantment&gt;&gt;. */
	public final List<EnchantmentStack> mEnchantmentTools = new ArrayList<>(1), mEnchantmentWeapons = new ArrayList<>(1), mEnchantmentAmmo = new ArrayList<>(1), mEnchantmentRanged = new ArrayList<>(1), mEnchantmentFishing = new ArrayList<>(1), mEnchantmentArmors = new ArrayList<>(1);

	/**
	 * Registers this Material into the given Registry. Upstream :321-328 is private and
	 * self-registers into the static maps; the registry is an explicit parameter now.
	 */
	OreDictMaterial(MaterialRegistry aRegistry, short aID, String aNameInternal, String aNameLocal) {
		mID = aID;
		mNameInternal = aNameInternal;
		mNameLocal = aNameLocal;
		if (aRegistry != null) aRegistry.register(this);
		mHashID = sHashID++;
	}

	/**
	 * Registry-free constructor. Upstream this constructor is private (:321); it stays public
	 * WITHOUT any registry interaction only because the card-1 MT.NULL stub (gregapi.data.MT,
	 * outside this card's FILES_SCOPE) calls it directly. Task gt-material-dataset re-routes
	 * MT.NULL through MaterialRegistry.createMaterial(-1, "NULL", "NULL") (upstream MT.java:524),
	 * after which this constructor should be re-privatized.
	 */
	public OreDictMaterial(short aID, String aNameInternal, String aNameLocal) {
		this((MaterialRegistry)null, aID, aNameInternal, aNameLocal);
	}

	/** Delegates to the default Registry (upstream static createMaterial :142-176). */
	public static OreDictMaterial createMaterial(int aID, String aNameOreDict, String aLocalName) {
		return MaterialRegistry.INSTANCE.createMaterial(aID, aNameOreDict, aLocalName);
	}

	/** Delegates to the default Registry (upstream static createAutoInvalidMaterial :177-182). */
	public static OreDictMaterial createAutoInvalidMaterial(String aNameOreDict) {
		return MaterialRegistry.INSTANCE.createAutoInvalidMaterial(aNameOreDict);
	}

	/** Delegates to the default Registry (upstream static get :184-202). */
	public static OreDictMaterial get(String aMaterialName, OreDictMaterial aDefault) {
		return MaterialRegistry.INSTANCE.get(aMaterialName, aDefault);
	}
	public static OreDictMaterial get(String aMaterialName) {
		return MaterialRegistry.INSTANCE.get(aMaterialName);
	}
	public static OreDictMaterial get(long aMaterialID, OreDictMaterial aDefault) {
		return MaterialRegistry.INSTANCE.get(aMaterialID, aDefault);
	}
	public static OreDictMaterial get(long aMaterialID) {
		return MaterialRegistry.INSTANCE.get(aMaterialID);
	}
	public static OreDictMaterial get(OreDictMaterial aMaterial) {
		return MaterialRegistry.INSTANCE.get(aMaterial);
	}

	/** Delegates to {@link MaterialRegistry#sanitize} (upstream static sanitize :204-206). */
	public static String sanitize(String aString) {
		return MaterialRegistry.sanitize(aString);
	}

	/** Sets the localised Name of this Material */ // :330-334
	public OreDictMaterial setLocal(String aNameLocal) {
		mNameLocal = aNameLocal == null ? mNameInternal : aNameLocal;
		return this;
	}

	/** Gets the localised Name of this Material. Upstream :336-339 is LanguageHandler.translate("gt.material." + mNameInternal, mNameLocal); with localization stripped this is the un-localized fallback value. */
	public String getLocal() {
		return mNameLocal;
	}

	/** Sets the original Mod of this Material. Upstream :341-351 takes ModData / (modID, modName); stripped to the mod ID per the MC-coupling policy. */
	public OreDictMaterial setOriginalMod(String aModID) {
		mOriginalMod = aModID == null ? mOriginalMod : aModID;
		return this;
	}

	/** Adds Identical Names which are getting re-registered to this Material. returns this Material, not the newly created ones. */ // :353-364
	public OreDictMaterial addIdenticalNames(String... aNames) {
		for (String aName : aNames) {
			aName = sanitize(aName);
			if (mNameInternal.equals(aName)) {
				MaterialRegistry.ERR_LOG.println("The Material '" + mNameInternal + "' has almost registered an identical Name as an alternative Name by accident, almost leading to Issues with the Recipe System.");
			} else {
				addReRegistrations(createMaterial(-1, aName, aName).setRegistration(this));
			}
		}
		return this;
	}

	/** Adds additional Names which this Material is getting re-registered to. For example "AnyCopper" would be something "Copper" and "AnnealedCopper" are getting re-registered to, to make them interchangeable in some Recipes. */ // :366-370
	public OreDictMaterial addReRegistrations(OreDictMaterial... aMaterials) {
		for (OreDictMaterial aMaterial : aMaterials) if (mReRegistrations.add(aMaterial)) aMaterial.mToThis.add(this);
		return this;
	}

	/** Adds this Materials Name as an additional Name the passed Materials are getting re-registered to. For example "AnyCopper" would be something "Copper" and "AnnealedCopper" are getting re-registered to, to make them interchangeable in some Recipes. */ // :372-376
	public OreDictMaterial addReRegistrationToThis(OreDictMaterial... aMaterials) {
		for (OreDictMaterial aMaterial : aMaterials) aMaterial.addReRegistrations(this);
		return this;
	}

	/** The Re-Registration for the Ore Dictionary for invalid Materials */ // :378-383
	public OreDictMaterial setRegistration(OreDictMaterial aMaterial) {
		mTargetRegistration = aMaterial == null ? this : aMaterial.mTargetRegistration;
		put(TD.Properties.INVALID_MATERIAL);
		return this;
	}

	public OreDictMaterial addSourceOf(OreDictMaterial... aMaterials) { // :385-388
		mSourceOf.addAll(Arrays.asList(aMaterials));
		return this;
	}

	public OreDictMaterial hide() { // :390-393
		mHidden = T;
		return this;
	}

	public OreDictMaterial hide(boolean aHidden) { // :395-398
		mHidden = aHidden;
		return this;
	}

	/**
	 * Tags referenced here whose TD groups (TD.Atomic/Compounds/Processing/ItemGenerator) are not
	 * ported yet - gregapi/data/TD.java is outside this card's FILES_SCOPE.
	 * TagData.createTagData is idempotent by upper-cased name, so these constants UNIFY with the
	 * real TD constants as soon as task gt-material-dataset ports them with the upstream key
	 * strings referenced in the comments. No line of this class has to change for that.
	 */
	private static class TDG {
		static final TagData ELEMENT           = TagData.createTagData("ATOMIC.ELEMENT");                // TD.java:305
		static final TagData NO_ADVANCED_TOOLS = TagData.createTagData("PROPERTIES.NO_ADVANCED_TOOLS");  // TD.java:450
		static final TagData HAS_TOOL_STATS    = TagData.createTagData("PROPERTIES.HAS_TOOL_STATS");     // TD.java:452
		static final TagData ALLOY             = TagData.createTagData("COMPOUNDS.ALLOY");               // TD.java:481
		static final TagData APPROXIMATE       = TagData.createTagData("COMPOUNDS.APPROXIMATE");         // TD.java:490
		static final TagData DECOMPOSABLE      = TagData.createTagData("COMPOUNDS.DECOMPOSABLE");        // TD.java:493
		static final TagData CENTRIFUGE        = TagData.createTagData("PROCESSING.CENTRIFUGABLE");      // TD.java:501
		static final TagData ELECTROLYSER      = TagData.createTagData("PROCESSING.ELECTROLYSABLE");     // TD.java:503
		static final TagData CRUCIBLE_ALLOY    = TagData.createTagData("PROCESSING.CRUCIBLE_ALLOY");     // TD.java:505
		static final TagData UUM               = TagData.createTagData("PROCESSING.UUM_SYNTHESISABLE");  // TD.java:511
		static final TagData MELTING           = TagData.createTagData("PROCESSING.MELTING");            // TD.java:525
		static final TagData PARTS             = TagData.createTagData("ITEMGENERATOR.PARTS");           // TD.java:562
		static final TagData STICKS            = TagData.createTagData("ITEMGENERATOR.STICKS");          // TD.java:568
		static final TagData PLATES            = TagData.createTagData("ITEMGENERATOR.PLATES");          // TD.java:571
	}

	/** Upstream CS.java:132 "C = 273"; local constant since CS is outside this card's FILES_SCOPE. */
	private static final long C = 273;
	/** Upstream CS.java:169-201 is a literal String[301]: indices 0..299 are the Unicode subscript spellings of the decimal digits, index 300 (:200) is "₃₀₀₊" marking "300 or more"; generated here with identical values (CS is outside this card's FILES_SCOPE). */
	private static final String[] NUM_SUB = new String[301];
	static {
		StringBuilder tBuilder = new StringBuilder();
		for (int i = 0; i < NUM_SUB.length - 1; i++) {
			tBuilder.setLength(0);
			String tDigits = Integer.toString(i);
			for (int j = 0; j < tDigits.length(); j++) tBuilder.append((char)(tDigits.charAt(j) + 0x2050)); // '0'(0x30)+0x2050 = 0x2080 = subscript zero
			NUM_SUB[i] = tBuilder.toString();
		}
		NUM_SUB[NUM_SUB.length - 1] = "\u2083\u2080\u2080\u208A"; // upstream CS.java:200 "₃₀₀₊", the "300 or more" cap
	}

	/** OM.stack(OreDictMaterial, long) equivalent (upstream gregapi/util/OM.java:485): a null Material yields a null Stack, which the NoNulls list of the configuration component then drops, exactly like upstream. */
	private static OreDictMaterialStack stack(OreDictMaterial aMaterial, long aAmount) {
		return aMaterial == null ? null : new OreDictMaterialStack(aMaterial, aAmount);
	}

	private static final PrintStream ERR = MaterialRegistry.ERR_LOG; // upstream CS.java:853 ERR

	public OreDictMaterial alloyCentrifuge() {return put(TDG.CENTRIFUGE).alloySimple();} // :424
	public OreDictMaterial alloyElectrolyzer() {return put(TDG.ELECTROLYSER).alloySimple();} // :425
	public OreDictMaterial alloySimple() { // :426-429
		mAlloyCreationRecipes.add(mComponents);
		return put(TDG.ALLOY, TDG.DECOMPOSABLE, TDG.CRUCIBLE_ALLOY);
	}

	public OreDictMaterial alloyCentrifuge(long aMelt) {return put(TDG.CENTRIFUGE).alloySimple(aMelt);} // :431
	public OreDictMaterial alloyElectrolyzer(long aMelt) {return put(TDG.ELECTROLYSER).alloySimple(aMelt);} // :432
	public OreDictMaterial alloySimple(long aMelt) { // :433-436
		mAlloyCreationRecipes.add(mComponents);
		return put(TDG.ALLOY, TDG.DECOMPOSABLE, TDG.CRUCIBLE_ALLOY).heat(aMelt);
	}

	public OreDictMaterial alloyCentrifuge(long aMelt, long aBoil) {return put(TDG.CENTRIFUGE).alloySimple(aMelt, aBoil);} // :438
	public OreDictMaterial alloyElectrolyzer(long aMelt, long aBoil) {return put(TDG.ELECTROLYSER).alloySimple(aMelt, aBoil);} // :439
	public OreDictMaterial alloySimple(long aMelt, long aBoil) { // :440-443
		mAlloyCreationRecipes.add(mComponents);
		return put(TDG.ALLOY, TDG.DECOMPOSABLE, TDG.CRUCIBLE_ALLOY).heat(aMelt, aBoil);
	}

	public OreDictMaterial alloyCentrifuge(OreDictMaterial aHeat) { // :445-447
		return put(TDG.CENTRIFUGE).alloySimple(aHeat);
	}
	public OreDictMaterial alloyElectrolyzer(OreDictMaterial aHeat) { // :448-450
		return put(TDG.ELECTROLYSER).alloySimple(aHeat);
	}
	public OreDictMaterial alloySimple(OreDictMaterial aHeat) { // :451-453
		return put(TDG.ALLOY, TDG.DECOMPOSABLE, TDG.CRUCIBLE_ALLOY).heat(aHeat);
	}

	/** Sets the Molecule Configuration or Components of this Material. Calculates the Average of the MainStats and sets them. */ // :468-525 verbatim (TD references swapped for the unifying TDG constants)
	public OreDictMaterial setMoleculeConfiguration(IOreDictConfigurationComponent aComponents) {
		if (contains(TDG.ELEMENT)) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);

		mComponents = aComponents;
		double tDivider = 0, tProtons = 0, tElectrons = 0, tNeutrons = 0, tMass = 0, tGramPerCubicCentimeter = 0, tMeltingPoint = 0, tBoilingPoint = 0, tPlasmaPoint = 0;
		for (OreDictMaterialStack tMaterial : aComponents.getComponents()) tDivider += tMaterial.mAmount;
		for (OreDictMaterialStack tMaterial : aComponents.getComponents()) {
			tProtons                += (tMaterial.mMaterial.mProtons                * tMaterial.mAmount) / UD;
			tElectrons              += (tMaterial.mMaterial.mElectrons              * tMaterial.mAmount) / UD;
			tNeutrons               += (tMaterial.mMaterial.mNeutrons               * tMaterial.mAmount) / UD;
			tMass                   += (tMaterial.mMaterial.mMass                   * tMaterial.mAmount) / UD;
			tGramPerCubicCentimeter += (tMaterial.mMaterial.mGramPerCubicCentimeter * tMaterial.mAmount) / UD;
			tMeltingPoint           += (tMaterial.mMaterial.mMeltingPoint           * tMaterial.mAmount) / tDivider;
			tBoilingPoint           += (tMaterial.mMaterial.mBoilingPoint           * tMaterial.mAmount) / tDivider;
			tPlasmaPoint            += (tMaterial.mMaterial.mPlasmaPoint            * tMaterial.mAmount) / tDivider;
		}
		mProtons = (long)tProtons;
		mElectrons = (long)tElectrons;
		mNeutrons = (long)tNeutrons;
		mMass = (long)tMass;
		mMeltingPoint = Math.max(1, (long)tMeltingPoint);
		mBoilingPoint = Math.max(mMeltingPoint+1, (long)tBoilingPoint);
		mPlasmaPoint = Math.max(mBoilingPoint+1, (long)tPlasmaPoint);
		mGramPerCubicCentimeter = tGramPerCubicCentimeter;

		if (!contains(TDG.APPROXIMATE) && containsAny(TDG.UUM, TDG.DECOMPOSABLE)) {
			mTooltipChemical = "";
			List<OreDictMaterialStack> tComponents = aComponents.getUndividedComponents();
			if (tComponents.size() == 1 && tComponents.get(0).mAmount == U) {
				mTooltipChemical = tComponents.get(0).mMaterial.mTooltipChemical;
			} else for (OreDictMaterialStack tMaterial : tComponents) {
				if (UT.Code.stringValid(tMaterial.mMaterial.mTooltipChemical) && !tMaterial.mMaterial.contains(TDG.APPROXIMATE)) {
					if (tMaterial.mMaterial.contains(TDG.ELEMENT) || tMaterial.mMaterial.mComponents == null || (tMaterial.mMaterial.mComponents.getUndividedComponents().size() == 1 && tMaterial.mMaterial.mComponents.getComponents().get(0).mAmount == U)) {
						mTooltipChemical += tMaterial.mMaterial.mTooltipChemical;
					} else {
						mTooltipChemical += "("+tMaterial.mMaterial.mTooltipChemical+")";
					}
				} else {
					mTooltipChemical += "("+tMaterial.mMaterial.mNameLocal+")";
				}
				if (tMaterial.mAmount > U) {
					if (tMaterial.mMaterial.mComponents == null) {
						mTooltipChemical += NUM_SUB[(int)UT.Code.bind(0, NUM_SUB.length-1, tMaterial.mAmount / U)];
					} else if ((tMaterial.mAmount / U) % tMaterial.mMaterial.mComponents.getCommonDivider() == 0) {
						if ((tMaterial.mAmount / U) / tMaterial.mMaterial.mComponents.getCommonDivider() > 1) {
							mTooltipChemical += NUM_SUB[(int)UT.Code.bind(0, NUM_SUB.length-1, (tMaterial.mAmount / U) / tMaterial.mMaterial.mComponents.getCommonDivider())];
						} else {
							// nothing to add in this case.
						}
					} else {
						mTooltipChemical += NUM_SUB[(int)UT.Code.bind(0, NUM_SUB.length-1, tMaterial.mAmount / U)] + "," + NUM_SUB[(int)UT.Code.bind(0, NUM_SUB.length-1, tMaterial.mMaterial.mComponents.getCommonDivider())];
					}
				}
			}
		}
		return this;
	}

	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1) { // :527-534 (OM.stack unfolded)
		if (aCommonDivider == 0) {
			long tAmount = aAmount1;
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2) { // :535-542
		if (aCommonDivider == 0) {
			long tAmount = aAmount1+aAmount2;
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3) { // :543-550
		if (aCommonDivider == 0) {
			long tAmount = aAmount1+aAmount2+aAmount3;
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4) { // :551-558
		if (aCommonDivider == 0) {
			long tAmount = aAmount1+aAmount2+aAmount3+aAmount4;
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5) { // :559-566
		if (aCommonDivider == 0) {
			long tAmount = aAmount1+aAmount2+aAmount3+aAmount4+aAmount5;
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4), stack(aMaterial5, aAmount5)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6) { // :567-574
		long tAmount = aAmount1+aAmount2+aAmount3+aAmount4+aAmount5+aAmount6;
		if (aCommonDivider == 0) {
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4), stack(aMaterial5, aAmount5), stack(aMaterial6, aAmount6)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7) { // :575-582
		long tAmount = aAmount1+aAmount2+aAmount3+aAmount4+aAmount5+aAmount6+aAmount7;
		if (aCommonDivider == 0) {
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4), stack(aMaterial5, aAmount5), stack(aMaterial6, aAmount6), stack(aMaterial7, aAmount7)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8) { // :583-590
		long tAmount = aAmount1+aAmount2+aAmount3+aAmount4+aAmount5+aAmount6+aAmount7+aAmount8;
		if (aCommonDivider == 0) {
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4), stack(aMaterial5, aAmount5), stack(aMaterial6, aAmount6), stack(aMaterial7, aAmount7), stack(aMaterial8, aAmount8)));
	}
	public OreDictMaterial setMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8, OreDictMaterial aMaterial9, long aAmount9) { // :591-598
		long tAmount = aAmount1+aAmount2+aAmount3+aAmount4+aAmount5+aAmount6+aAmount7+aAmount8+aAmount9;
		if (aCommonDivider == 0) {
			aCommonDivider = tAmount / U;
			if (tAmount % U != 0) ERR.println("WARNING: Material '"+mNameInternal+"' has an Amount of " + tAmount + " Components and automatically generates a divider, that is leaving a tiny rest after the division, breaking some Material Amounts. Manual setting of Variables is required.");
		}
		return setMoleculeConfiguration(new OreDictConfigurationComponent(aCommonDivider, stack(aMaterial1, aAmount1), stack(aMaterial2, aAmount2), stack(aMaterial3, aAmount3), stack(aMaterial4, aAmount4), stack(aMaterial5, aAmount5), stack(aMaterial6, aAmount6), stack(aMaterial7, aAmount7), stack(aMaterial8, aAmount8), stack(aMaterial9, aAmount9)));
	}

	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1) { // :602-609
		if (aMaterial1.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2) { // :610-617
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3) { // :618-625
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4) { // :626-633
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5) { // :634-641
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM) && aMaterial5.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6) { // :642-649
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM) && aMaterial5.contains(TDG.UUM) && aMaterial6.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7) { // :650-657
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM) && aMaterial5.contains(TDG.UUM) && aMaterial6.contains(TDG.UUM) && aMaterial7.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8) { // :658-665
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM) && aMaterial5.contains(TDG.UUM) && aMaterial6.contains(TDG.UUM) && aMaterial7.contains(TDG.UUM) && aMaterial8.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8);
	}
	public OreDictMaterial uumMcfg(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8, OreDictMaterial aMaterial9, long aAmount9) { // :666-673
		if (aMaterial1.contains(TDG.UUM) && aMaterial2.contains(TDG.UUM) && aMaterial3.contains(TDG.UUM) && aMaterial4.contains(TDG.UUM) && aMaterial5.contains(TDG.UUM) && aMaterial6.contains(TDG.UUM) && aMaterial7.contains(TDG.UUM) && aMaterial8.contains(TDG.UUM) && aMaterial9.contains(TDG.UUM)) {
			put(TDG.UUM);
		} else {
			ERR.println("WARNING: " + mNameInternal + " has a UUM Config with impossible Materials.");
		}
		return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8, aMaterial9, aAmount9);
	}

	// Yes it is spelled Aloy because of being Four Letters long, for alignment reasons. // :675

	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1) {return setMcfg(aCommonDivider, aMaterial1, aAmount1).alloyCentrifuge();} // :677-679
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2).alloyCentrifuge();} // :680-682
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3).alloyCentrifuge();} // :683-685
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4).alloyCentrifuge();} // :686-688
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5).alloyCentrifuge();} // :689-691
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6).alloyCentrifuge();} // :692-694
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7).alloyCentrifuge();} // :695-697
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8).alloyCentrifuge();} // :698-700
	public OreDictMaterial setAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8, OreDictMaterial aMaterial9, long aAmount9) {return setMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8, aMaterial9, aAmount9).alloyCentrifuge();} // :701-703

	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1).alloyCentrifuge();} // :707-709
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2).alloyCentrifuge();} // :710-712
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3).alloyCentrifuge();} // :713-715
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4).alloyCentrifuge();} // :716-718
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5).alloyCentrifuge();} // :719-721
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6).alloyCentrifuge();} // :722-724
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7).alloyCentrifuge();} // :725-727
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8).alloyCentrifuge();} // :728-730
	public OreDictMaterial uumAloy(long aCommonDivider, OreDictMaterial aMaterial1, long aAmount1, OreDictMaterial aMaterial2, long aAmount2, OreDictMaterial aMaterial3, long aAmount3, OreDictMaterial aMaterial4, long aAmount4, OreDictMaterial aMaterial5, long aAmount5, OreDictMaterial aMaterial6, long aAmount6, OreDictMaterial aMaterial7, long aAmount7, OreDictMaterial aMaterial8, long aAmount8, OreDictMaterial aMaterial9, long aAmount9) {return uumMcfg(aCommonDivider, aMaterial1, aAmount1, aMaterial2, aAmount2, aMaterial3, aAmount3, aMaterial4, aAmount4, aMaterial5, aAmount5, aMaterial6, aAmount6, aMaterial7, aAmount7, aMaterial8, aAmount8, aMaterial9, aAmount9).alloyCentrifuge();} // :731-733

	@Deprecated public OreDictMaterial setTooltip(String aTooltip) {mTooltipChemical = aTooltip; return this;} // :737

	public OreDictMaterial tooltip(String aTooltip) { // :739-742
		mTooltipChemical = aTooltip;
		return this;
	}

	public OreDictMaterial handle(OreDictMaterial aHandle) { // :744-747
		mHandleMaterial = aHandle;
		return this;
	}

	public OreDictMaterial setAllToTheOutputOf(OreDictMaterial aMaterial) { // :749-762
		if (aMaterial == null) aMaterial = this;
		setPulver     (aMaterial.mTargetPulver     .mMaterial, aMaterial.mTargetPulver     .mAmount);
		setSmelting   (aMaterial.mTargetSmelting   .mMaterial, aMaterial.mTargetSmelting   .mAmount);
		setSolidifying(aMaterial.mTargetSolidifying.mMaterial, aMaterial.mTargetSolidifying.mAmount);
		setSmashing   (aMaterial.mTargetSmashing   .mMaterial, aMaterial.mTargetSmashing   .mAmount);
		setCutting    (aMaterial.mTargetCutting    .mMaterial, aMaterial.mTargetCutting    .mAmount);
		setWorking    (aMaterial.mTargetWorking    .mMaterial, aMaterial.mTargetWorking    .mAmount);
		setForging    (aMaterial.mTargetForging    .mMaterial, aMaterial.mTargetForging    .mAmount);
		setBurning    (aMaterial.mTargetBurning    .mMaterial, aMaterial.mTargetBurning    .mAmount);
		setBending    (aMaterial.mTargetBending    .mMaterial, aMaterial.mTargetBending    .mAmount);
		setCompressing(aMaterial.mTargetCompressing.mMaterial, aMaterial.mTargetCompressing.mAmount);
		return this;
	}

	public OreDictMaterial setAllToTheOutputOf(OreDictMaterial aMaterial, long aMultiplier, long aDivider) { // :764-778
		if (aMaterial == null) aMaterial = this;
		setPulver     (aMaterial.mTargetPulver     .mMaterial,(aMaterial.mTargetPulver     .mAmount * aMultiplier) / aDivider);
		setSmelting   (aMaterial.mTargetSmelting   .mMaterial,(aMaterial.mTargetSmelting   .mAmount * aMultiplier) / aDivider);
		setSolidifying(aMaterial.mTargetSolidifying.mMaterial,(aMaterial.mTargetSolidifying.mAmount * aMultiplier) / aDivider);
		setSmashing   (aMaterial.mTargetSmashing   .mMaterial,(aMaterial.mTargetSmashing   .mAmount * aMultiplier) / aDivider);
		setCutting    (aMaterial.mTargetCutting    .mMaterial,(aMaterial.mTargetCutting    .mAmount * aMultiplier) / aDivider);
		setWorking    (aMaterial.mTargetWorking    .mMaterial,(aMaterial.mTargetWorking    .mAmount * aMultiplier) / aDivider);
		setForging    (aMaterial.mTargetForging    .mMaterial,(aMaterial.mTargetForging    .mAmount * aMultiplier) / aDivider);
		setBurning    (aMaterial.mTargetBurning    .mMaterial,(aMaterial.mTargetBurning    .mAmount * aMultiplier) / aDivider);
		setBending    (aMaterial.mTargetBending    .mMaterial,(aMaterial.mTargetBending    .mAmount * aMultiplier) / aDivider);
		setCompressing(aMaterial.mTargetCompressing.mMaterial,(aMaterial.mTargetCompressing.mAmount * aMultiplier) / aDivider);
		setCrushing   (aMaterial.mTargetCrushing   .mMaterial,(aMaterial.mTargetCrushing   .mAmount * aMultiplier) / aDivider);
		return this;
	}

	/** The result of trying to ore process it, if you want to disable ore processing, then set the Amount to 0. If aMaterial == null it will choose the previous Material instead, which is usually "this". */ // :780-787
	public OreDictMaterial setCrushing(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetCrushing.mMaterial.mTargetedCrushing.remove(this);
		mTargetCrushing = stack(aMaterial, aAmount);
		aMaterial.mTargetedCrushing.add(this);
		return this;
	}

	/** The result of trying to pulverise it, if you want to disable pulverising, then set the Amount to 0. If aMaterial == null it will choose the previous Material instead, which is usually "this". */ // :789-796
	public OreDictMaterial setPulver(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetPulver.mMaterial.mTargetedPulver.remove(this);
		mTargetPulver = stack(aMaterial, aAmount);
		aMaterial.mTargetedPulver.add(this);
		return this;
	}

	/** The result of trying to smelt it, if you want to disable smelting, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :798-806
	public OreDictMaterial setSmelting(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetSmelting.mMaterial.mTargetedSmelting.remove(this);
		mTargetSmelting = stack(aMaterial, aAmount);
		aMaterial.mTargetedSmelting.add(this);
		if (aAmount > 0) put(TDG.MELTING);
		return this;
	}

	/** The result of cooling it down, if you want to disable cooling down, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :808-815
	public OreDictMaterial setSolidifying(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetSolidifying.mMaterial.mTargetedSolidifying.remove(this);
		mTargetSolidifying = stack(aMaterial, aAmount);
		aMaterial.mTargetedSolidifying.add(this);
		return this;
	}

	/** The result of trying to smash it, if you want to disable smashing, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :817-824
	public OreDictMaterial setSmashing(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetSmashing.mMaterial.mTargetedSmashing.remove(this);
		mTargetSmashing = stack(aMaterial, aAmount);
		aMaterial.mTargetedSmashing.add(this);
		return this;
	}

	/** The result of trying to cut it, if you want to disable cutting, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :826-833
	public OreDictMaterial setCutting(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetCutting.mMaterial.mTargetedCutting.remove(this);
		mTargetCutting = stack(aMaterial, aAmount);
		aMaterial.mTargetedCutting.add(this);
		return this;
	}

	/** The result of trying to craft with it, if you want to disable working, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :835-842
	public OreDictMaterial setWorking(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetWorking.mMaterial.mTargetedWorking.remove(this);
		mTargetWorking = stack(aMaterial, aAmount);
		aMaterial.mTargetedWorking.add(this);
		return this;
	}

	/** The result of trying to forge it, if you want to disable forging, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :844-851
	public OreDictMaterial setForging(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetForging.mMaterial.mTargetedForging.remove(this);
		mTargetForging = stack(aMaterial, aAmount);
		aMaterial.mTargetedForging.add(this);
		return this;
	}

	/** The result of trying to burn it (Ashes for example), if you want to disable burning, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :853-860
	public OreDictMaterial setBurning(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetBurning.mMaterial.mTargetedBurning.remove(this);
		mTargetBurning = stack(aMaterial, aAmount);
		aMaterial.mTargetedBurning.add(this);
		return this;
	}

	/** The result of trying to bend it, if you want to disable bending, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :862-869
	public OreDictMaterial setBending(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetBending.mMaterial.mTargetedBending.remove(this);
		mTargetBending = stack(aMaterial, aAmount);
		aMaterial.mTargetedBending.add(this);
		return this;
	}

	/** The result of trying to compress it, if you want to disable compressing, then set the Amount to 0. If aMaterial == null it will choose "this". */ // :871-878
	public OreDictMaterial setCompressing(OreDictMaterial aMaterial, long aAmount) {
		if (aMaterial == null) aMaterial = this;
		mTargetCompressing.mMaterial.mTargetedCompressing.remove(this);
		mTargetCompressing = stack(aMaterial, aAmount);
		aMaterial.mTargetedCompressing.add(this);
		return this;
	}

	/** The result of trying to generify it, If aMaterial == null it will choose "this". */ // :880-887
	public OreDictMaterial setGenerifying(OreDictMaterial aMaterial) {
		if (aMaterial == null) aMaterial = this;
		mTargetGenerifying.mMaterial.mTargetedGenerifying.remove(this);
		mTargetGenerifying = stack(aMaterial, U);
		aMaterial.mTargetedGenerifying.add(this);
		return this;
	}

	@Deprecated public OreDictMaterial setQuality(float aToolSpeed, long aToolDurability, long aToolQuality) {return qual(3, aToolSpeed, aToolDurability, aToolQuality);} // :889

	public OreDictMaterial qual(long aHarvestLevel) {return qual(mToolTypes, mToolSpeed, mToolDurability, aHarvestLevel);} // :891
	public OreDictMaterial qual(float aSpeed, long aDurability, long aQuality) {return qual(3, aSpeed, aDurability, aQuality);} // :892
	/** Sets the Tool Quality of this Material. */ // :893-902
	public OreDictMaterial qual(long aType, double aSpeed, long aDurability, long aQuality) {
		mToolTypes = UT.Code.bind2(aType);
		mToolDurability = Math.max(1, aDurability);
		mToolQuality = UT.Code.bind4(aQuality);
		mToolSpeed = (float)aSpeed;
		if (aType > 0) put(TDG.HAS_TOOL_STATS, TDG.PARTS, TDG.STICKS, TDG.PLATES);
		if (aType < 3) put(TDG.NO_ADVANCED_TOOLS);
		return this;
	}

	@Deprecated public OreDictMaterial stealQuality(OreDictMaterial aStatsToCopy) {return qual(aStatsToCopy);} // :1132
	public OreDictMaterial qual(OreDictMaterial aStatsToCopy) { // :1133-1135
		return qual(aStatsToCopy.mToolTypes, aStatsToCopy.mToolSpeed, aStatsToCopy.mToolDurability, aStatsToCopy.mToolQuality);
	}

	@Deprecated public OreDictMaterial setMeltingPoint(long aMeltingPoint) {return heat(aMeltingPoint);} // :904
	/** Sets the energetic Stats of this Material. Everything is measured in Kelvin. */ // :905-915
	public OreDictMaterial heat(long aMeltingPoint) {
		if (contains(TDG.ELEMENT)) {
			if (mMeltingPoint != 1000 && aMeltingPoint != mMeltingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
		//  if (mBoilingPoint != 3000 && aBoilingPoint != mBoilingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
		}
		mMeltingPoint = aMeltingPoint;
		mBoilingPoint = mMeltingPoint * 2;
		mPlasmaPoint = mBoilingPoint * 100;
		return this;
	}

	@Deprecated public OreDictMaterial setStatsEnergetic(long aMeltingPoint, long aBoilingPoint) {return heat(aMeltingPoint, aBoilingPoint);} // :917
	/** Sets the energetic Stats of this Material. Everything is measured in Kelvin. */ // :918-929
	public OreDictMaterial heat(long aMeltingPoint, long aBoilingPoint) {
		if (contains(TDG.ELEMENT)) {
			if (mMeltingPoint != 1000 && aMeltingPoint != mMeltingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
			if (mBoilingPoint != 3000 && aBoilingPoint != mBoilingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
		}
		if (aMeltingPoint > aBoilingPoint) throw new IllegalArgumentException("The Melting Point cannot be above the Boiling Point.");
		mMeltingPoint = aMeltingPoint;
		mBoilingPoint = aBoilingPoint;
		mPlasmaPoint = aBoilingPoint * 100;
		return this;
	}

	@Deprecated public OreDictMaterial setStatsEnergetic(long aMeltingPoint, long aBoilingPoint, long aPlasmaPoint) {return heat(aMeltingPoint, aBoilingPoint, aPlasmaPoint);} // :931
	/** Sets the energetic Stats of this Material. Everything is measured in Kelvin. */ // :932-944
	public OreDictMaterial heat(long aMeltingPoint, long aBoilingPoint, long aPlasmaPoint) {
		if (contains(TDG.ELEMENT)) {
			if (mMeltingPoint != 1000 && aMeltingPoint != mMeltingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
			if (mBoilingPoint != 3000 && aBoilingPoint != mBoilingPoint) new IllegalArgumentException("Detected problematic tampering with Elements of the Periodic Table").printStackTrace(ERR);
		}
		if (aMeltingPoint > aBoilingPoint) throw new IllegalArgumentException("The Melting Point cannot be above the Boiling Point.");
		if (aBoilingPoint > aPlasmaPoint) throw new IllegalArgumentException("The Boiling Point cannot be above the Plasmafication Point.");
		mMeltingPoint = aMeltingPoint;
		mBoilingPoint = aBoilingPoint;
		mPlasmaPoint = aPlasmaPoint;
		return this;
	}

	/** Copies the energetic Stats of another Material. */ // :1128-1130
	public OreDictMaterial heat(OreDictMaterial aStatsToCopy) {
		return heat(aStatsToCopy.mMeltingPoint, aStatsToCopy.mBoilingPoint, aStatsToCopy.mPlasmaPoint);
	}

	/** Sets the atomic and energetic Stats of this Element. */ // :946-955
	public OreDictMaterial setStats(long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter) {
		heat(aMeltingPoint, aBoilingPoint);
		mProtons = aProtonsAndElectrons;
		mElectrons = aProtonsAndElectrons;
		mNeutrons = aNeutrons;
		mMass = aProtonsAndElectrons + aNeutrons;
		mGramPerCubicCentimeter = aGramPerCubicCentimeter;
		return this;
	}

	/** Sets the atomic Stats of this Material. */ // :957-965
	public OreDictMaterial setStatsElement(long aProtons, long aElectrons, long aNeutrons, long aAdditionalMass, double aGramPerCubicCentimeter) {
		mProtons = aProtons;
		mElectrons = aElectrons;
		mNeutrons = aNeutrons;
		mMass = aProtons + aNeutrons + aAdditionalMass;
		mGramPerCubicCentimeter = aGramPerCubicCentimeter;
		return this;
	}

	/** Copies the atomic Stats of another Material. */ // :1137-1144
	public OreDictMaterial stealStatsElement(OreDictMaterial aStatsToCopy) {
		mProtons                = aStatsToCopy.mProtons;
		mElectrons              = aStatsToCopy.mElectrons;
		mNeutrons               = aStatsToCopy.mNeutrons;
		mMass                   = aStatsToCopy.mMass;
		mGramPerCubicCentimeter = aStatsToCopy.mGramPerCubicCentimeter;
		return this;
	}

	/** Adds an Enchantment for Tools. Upstream :1204-1207 takes the Enchantment object; ID string per the MC-coupling policy. */
	public OreDictMaterial addEnchantmentForTools(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentTools.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Adds an Enchantment for Damage dealing Weapons and Ammos. */ // :1209-1213
	public OreDictMaterial addEnchantmentForDamage(String aEnchantmentID, long aEnchantmentLevel) {
		addEnchantmentForWeapons(aEnchantmentID, aEnchantmentLevel);
		addEnchantmentForAmmo(aEnchantmentID, aEnchantmentLevel);
		return this;
	}

	/** Adds an Enchantment for Weapons. */ // :1215-1218
	public OreDictMaterial addEnchantmentForWeapons(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentWeapons.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Adds an Enchantment for Ammos. */ // :1220-1223 (upstream clamps the level of one specific vanilla Enchantment via a raw field reference; that MC special case is stripped)
	public OreDictMaterial addEnchantmentForAmmo(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentAmmo.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Adds an Enchantment for Ranged Weapons. */ // :1225-1228
	public OreDictMaterial addEnchantmentForRanged(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentRanged.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Adds an Enchantment for Fishing Rods. */ // :1230-1233
	public OreDictMaterial addEnchantmentForFishing(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentFishing.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Adds an Enchantment for Armors. */ // :1235-1238
	public OreDictMaterial addEnchantmentForArmors(String aEnchantmentID, long aEnchantmentLevel) {
		mEnchantmentArmors.add(new EnchantmentStack(aEnchantmentID, aEnchantmentLevel));
		return this;
	}

	/** Sets the furnace burn time. */ // :1197-1201 (referenced by put(Object...) :1472)
	public OreDictMaterial setFurnaceBurnTime(long aValue) {
		mFurnaceBurnTime = Math.max(0, aValue);
		return this;
	}

	@Override
	public boolean contains(TagData aTag) { // :1431-1434
		return mTags.contains(aTag);
	}

	public boolean containsAny(TagData... aTags) { // :1436-1439
		for (TagData aTag : aTags) if (mTags.contains(aTag)) return T;
		return F;
	}

	@Override
	public boolean containsAll(TagData... aTags) { // :1441-1444
		return mTags.containsAll(Arrays.asList(aTags));
	}

	@Override
	public boolean containsAll(Collection<TagData> aTags) { // :1446-1449
		return mTags.containsAll(aTags);
	}

	public OreDictMaterial put(TagData... aObjects) {return add(aObjects);} // :1451
	/** Upstream :1452-1486 dispatches TagData/ModData/String/Number/OreDictMaterial/Achievement/Iterable; the ModData and Achievement branches are deferred together with their stripped field types (setOriginalMod(String) and mAchievementsForCreation cover that data). */
	public OreDictMaterial put(Object... aObjects) {
		if (aObjects != null) for (Object aObject : aObjects) if (aObject != null) {
			if (aObject.getClass().isArray()) {
				put((Object[])aObject);
				continue;
			}
			if (aObject instanceof TagData) {
				mTags.add((TagData)aObject);
				continue;
			}
			if (aObject instanceof String) {
				addIdenticalNames((String)aObject);
				continue;
			}
			if (aObject instanceof Number) {
				setFurnaceBurnTime(((Number)aObject).longValue());
				continue;
			}
			if (aObject instanceof OreDictMaterial) {
				addReRegistrations((OreDictMaterial)aObject);
				continue;
			}
			if (aObject instanceof Iterable) for (Object aIterated : (Iterable)aObject) put(aIterated);
		}
		return this;
	}

	/** Additional Function for convenience. */ // :1488-1493
	public OreDictMaterial add(TagData[] aTags1, TagData... aTags2) {
		put(aTags1);
		put(aTags2);
		return this;
	}

	@Override
	public OreDictMaterial add(TagData... aTags) { // :1495-1499
		if (aTags != null) for (TagData aTag : aTags) mTags.add(aTag);
		return this;
	}

	@Override
	public boolean remove(TagData aTag) { // :1501-1504
		return mTags.remove(aTag);
	}

	@Override
	public boolean isTrue(OreDictMaterial aObject) { // :1506-1509
		return aObject == this;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public final ICondition<ITagDataContainer> NOT = new ICondition.Not(this); // :1510-1511, raw type like upstream (TagData.NOT carries the relaxed ITagDataContainer typing)

	/** Gets the amount of Neutrons per Unit of Molecule */ // :1370-1373
	public long getNeutrons() {
		return mNeutrons;
	}

	/** Gets the amount of Protons per Unit of Molecule */ // :1375-1378
	public long getProtons() {
		return mProtons;
	}

	/** Gets the amount of Electrons per Unit of Molecule */ // :1380-1383
	public long getElectrons() {
		return mElectrons;
	}

	/** Gets the Mass of one Unit of Molecule */ // :1385-1388
	public long getMass() {
		return mMass;
	}

	/** Gets the Weight of this Material in Kilogramme, depending on the Amount of Material passed. */ // :1390-1402 verbatim
	public double getWeight(long aAmount) {
		// Extended Math:
		// 9 Material-Units = 1 Cubic Meter.
		// 1000 g    = 1 kg
		// 1000 cm^3 = 1 dm^3
		// 1000 dm^3 = 1  m^3
		// ( g/cm^3 * aAmount * 1000 * 1000) / (Material-Unit * 9 * 1000)
		// ( g/ m^3 * aAmount              ) / (Material-Unit * 9 * 1000)
		// (kg/ m^3 * aAmount              ) / (Material-Unit * 9       )
		// (kg/ m^3 * aAmount * 0.111111111) /  Material-Unit
		return (mGramPerCubicCentimeter * 111.111111 * aAmount) / U;
	}

	@Override
	public String toString() { // :1404-1407
		return mNameInternal;
	}

	@Override
	public int hashCode() { // :1409-1412
		return mHashID;
	}

	/** (Enchantment ID, Level) pair replacing upstream ObjectStack&lt;Enchantment&gt; per the "Enchantment -> ID string" policy (upstream :318-319, :1204+). */
	public static final class EnchantmentStack {
		public final String mEnchantmentID;
		public final long mLevel;

		public EnchantmentStack(String aEnchantmentID, long aLevel) {
			mEnchantmentID = aEnchantmentID == null ? "" : aEnchantmentID;
			mLevel = aLevel;
		}

		@Override
		public boolean equals(Object aCompared) {
			return aCompared instanceof EnchantmentStack && ((EnchantmentStack)aCompared).mEnchantmentID.equals(mEnchantmentID);
		}

		@Override
		public int hashCode() {
			return mEnchantmentID.hashCode();
		}

		@Override
		public String toString() {
			return mEnchantmentID + " - " + mLevel;
		}
	}
}
