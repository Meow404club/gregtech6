/**
 * Ported from GregTech 6 (1.7.10), file gregapi/data/MT.java (upstream 4118 lines),
 * by task gt-material-dataset (card 3).
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

import static gregapi.data.CS.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gregapi.code.HashSetNoNulls;
import gregapi.code.TagData;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.OreDictConfigurationComponent;

import static gregapi.data.TD.Atomic.*;
import static gregapi.data.TD.Compounds.*;
import static gregapi.data.TD.ItemGenerator.*;
import static gregapi.data.TD.Processing.*;
import static gregapi.data.TD.Properties.*;

/**
 * @author Gregorius Techneticies
 *
 * List of all Materials. The Short Name is for ease of overview and stands for "MaTerial".
 *
 * Note: I wrote those shortcuts not only because of overview Reasons. I have hit the 65536 Limit of the static initialiser multiple times by now. // upstream MT.java:45
 *
 * PORT ARCHITECTURE (card gt-material-dataset):
 * - Upstream initializes every field inside ONE giant clinit; this port keeps the exact upstream
 *   declaration ORDER but moves the initializations into reg0000()..reg00NN() methods called from
 *   {@link #init()} (upstream MT.java:1887), honoring the red line against giant static
 *   initializers (upstream :45 author note). TECH.init is split into 32 chunks the same way.
 * - Main-class material fields are therefore public static (non-final) OreDictMaterial
 *   references; the small nested classes TECH-fields/OREMATS/STONES/WOODS/UNUSED keep their
 *   upstream static-final + initializer form (their class-init is tiny) and are force-loaded by
 *   init() exactly like upstream :1891-1900.
 * - MC-strip (decided, evidence in comments): TextureSet/IIconContainer -> String[] name
 *   references (SET_X = {"X","X"}); ModData -> MDRef (mod ID string, funnelled through
 *   create()/put like upstream put :1463-1466); Enchantment -> vanilla ID string
 *   (Enchantment.field_151370_z = "luckOfTheSea"); FluidStack (.liquid(FL...)) -> deleted;
 *   OreDictManager/OreDictItemData side effects (wood/stone helpers :302-306, :314-318, :326-327)
 *   -> deferred to the manager/prefix tasks; zero MC imports.
 * - Deferred with the upstream call sites stripped and commented: .aspects(...) (ThaumCraft,
 *   1078+68 sites), .lens(...) :418-422 (44 sites, Phase 2), .visDefault() :410-416 (192 sites,
 *   Phase 2 visibility manager).
 * - Missing OreDictMaterial setters (setRGBa :1032, setRGBaLiquid :1060, setDensity :1146,
 *   setOreMultiplier :1193, setPriorityPrefix :1334, ores :1239, steal :1101, stealLooks :1108,
 *   setTextures :1024) are replicated as MT-local static helpers because FILES_SCOPE forbids
 *   touching the OreDictMaterial method region (except the constructor privatization and the
 *   spec-mandated addAlloyingRecipe :455-466).
 */
public class MT {

	/** This Set is for GregTech usage only, do not add your Materials to this! */ // upstream MT.java:48-49
	public static final HashSetNoNulls<OreDictMaterial> ALL_MATERIALS_REGISTERED_HERE = new HashSetNoNulls<>();

	/** TextureSet name references (upstream gregapi/render/TextureSet.java:188-228). Each upstream SET_X is a TextureSet[] {addTextureSet(MD.GT.mID, F, "X"), addTextureSet(MD.GT.mID, T, "X")} = {Block-Variant, Item-Variant}; the MC-strip keeps the two variant NAMES as String[2] (Phase-2 rendering resolves them). */
	public static final String[] SET_NONE = {"NONE", "NONE"}, SET_DULL = {"DULL", "DULL"};
	public static final String[] SET_RAD = {"RAD", "RAD"}, SET_HEX = {"HEX", "HEX"};
	public static final String[] SET_RUBY = {"RUBY", "RUBY"}, SET_OPAL = {"OPAL", "OPAL"};
	public static final String[] SET_LEAF = {"LEAF", "LEAF"}, SET_SAND = {"SAND", "SAND"};
	public static final String[] SET_FINE = {"FINE", "FINE"}, SET_FOOD = {"FOOD", "FOOD"};
	public static final String[] SET_WOOD = {"WOOD", "WOOD"}, SET_CUBE = {"CUBE", "CUBE"};
	public static final String[] SET_FIERY = {"FIERY", "FIERY"}, SET_GAS = {"GAS", "GAS"};
	public static final String[] SET_FLUID = {"FLUID", "FLUID"}, SET_PLASMA = {"PLASMA", "PLASMA"};
	public static final String[] SET_ROUGH = {"ROUGH", "ROUGH"}, SET_STONE = {"STONE", "STONE"};
	public static final String[] SET_BRICK = {"BRICK", "BRICK"}, SET_SPACE = {"SPACE", "SPACE"};
	public static final String[] SET_PAPER = {"PAPER", "PAPER"}, SET_GLASS = {"GLASS", "GLASS"};
	public static final String[] SET_FLINT = {"FLINT", "FLINT"}, SET_LAPIS = {"LAPIS", "LAPIS"};
	public static final String[] SET_SHINY = {"SHINY", "SHINY"}, SET_RUBBER = {"RUBBER", "RUBBER"};
	public static final String[] SET_SHARDS = {"SHARDS", "SHARDS"}, SET_POWDER = {"POWDER", "POWDER"};
	public static final String[] SET_COPPER = {"COPPER", "COPPER"}, SET_QUARTZ = {"QUARTZ", "QUARTZ"};
	public static final String[] SET_EMERALD = {"EMERALD", "EMERALD"}, SET_DIAMOND = {"DIAMOND", "DIAMOND"};
	public static final String[] SET_LIGNITE = {"LIGNITE", "LIGNITE"}, SET_REDSTONE = {"REDSTONE", "REDSTONE"};
	public static final String[] SET_MAGNETIC = {"MAGNETIC", "MAGNETIC"}, SET_METALLIC = {"METALLIC", "METALLIC"};
	public static final String[] SET_PRISMARINE = {"PRISMARINE", "PRISMARINE"}, SET_CUBE_SHINY = {"CUBE_SHINY", "CUBE_SHINY"};
	public static final String[] SET_NETHERSTAR = {"NETHERSTAR", "NETHERSTAR"}, SET_GEM_VERTICAL = {"GEM_VERTICAL", "GEM_VERTICAL"};
	public static final String[] SET_GEM_HORIZONTAL = {"GEM_HORIZONTAL", "GEM_HORIZONTAL"};

	/** Upstream CS.java:442-457. */
	public static final byte DYE_INDEX_Black = 0, DYE_INDEX_Red = 1, DYE_INDEX_Green = 2, DYE_INDEX_Brown = 3, DYE_INDEX_Blue = 4, DYE_INDEX_Purple = 5, DYE_INDEX_Cyan = 6, DYE_INDEX_LightGray = 7, DYE_INDEX_Gray = 8, DYE_INDEX_Pink = 9, DYE_INDEX_Lime = 10, DYE_INDEX_Yellow = 11, DYE_INDEX_LightBlue = 12, DYE_INDEX_Magenta = 13, DYE_INDEX_Orange = 14, DYE_INDEX_White = 15;
	/** Upstream CS.java:459. */
	public static final String[] DYE_NAMES = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "Light Gray", "Gray", "Pink", "Lime", "Yellow", "Light Blue", "Magenta", "Orange", "White"};

	/** Upstream CS.java:210 TICKS_PER_SMELT = 200 (CS is outside this card's FILES_SCOPE). */
	public static final int TICKS_PER_SMELT = 200;
	/** Upstream CS.java:829. */
	public static final OreDictMaterial[] ZL_MT = new OreDictMaterial[0];
	/** Upstream CS.java:859. */
	public static final double WEIGHT_AIR_G_PER_CUBIC_CENTIMETER = 0.0012;
	/** Upstream CS.java:132 "C = 273"; MT has a material field named C (Carbon), so the constant hides behind a same-named nested type exactly like the upstream CS.C qualification. */
	private static final class CS {
		static final long C = 273;
	}
	public static final class MDRef {
		public final String mID;
		public MDRef(String aModID) { mID = aModID; }
	}

	/** ModData replacement (upstream gregapi/data/MD.java over CS.ModIDs); MC-strip: ModData -> mod ID string. */
	public static final class MD {
		public static final MDRef AA = new MDRef("ActuallyAdditions");
		public static final MDRef ABYSSAL = new MDRef("abyssalcraft");
		public static final MDRef AE = new MDRef("appliedenergistics2");
		public static final MDRef AETHER = new MDRef("aether");
		public static final MDRef ALF = new MDRef("alfheim");
		public static final MDRef ARS = new MDRef("arsmagica2");
		public static final MDRef AV = new MDRef("Avaritia");
		public static final MDRef BC = new MDRef("BuildCraft|Core");
		public static final MDRef BINNIE = new MDRef("BinnieCore");
		public static final MDRef BINNIE_TREE = new MDRef("ExtraTrees");
		public static final MDRef BOTA = new MDRef("Botania");
		public static final MDRef BP = new MDRef("bluepower");
		public static final MDRef BR = new MDRef("BigReactors");
		public static final MDRef BTL = new MDRef("thebetweenlands");
		public static final MDRef BoP = new MDRef("BiomesOPlenty");
		public static final MDRef CANDY = new MDRef("candycraftmod");
		public static final MDRef DE = new MDRef("DraconicEvolution");
		public static final MDRef DRPG = new MDRef("divinerpg");
		public static final MDRef EB = new MDRef("enhancedbiomes");
		public static final MDRef EBXL = new MDRef("ExtrabiomesXL");
		public static final MDRef EIO = new MDRef("EnderIO");
		public static final MDRef ERE = new MDRef("erebus");
		public static final MDRef EnLi = new MDRef("enderlicious");
		public static final MDRef EtFu = new MDRef("etfuturum");
		public static final MDRef ExU = new MDRef("ExtraUtilities");
		public static final MDRef FM = new MDRef("meteors");
		public static final MDRef FR = new MDRef("Forestry");
		public static final MDRef FRMB = new MDRef("MagicBees");
		public static final MDRef FZ = new MDRef("factorization");
		public static final MDRef Fossil = new MDRef("fossil");
		public static final MDRef GAPI = new MDRef("gregapi");
		public static final MDRef GC = new MDRef("GalacticraftCore");
		public static final MDRef GC_ADV_ROCKETRY = new MDRef("advancedRocketry");
		public static final MDRef GC_EXTRAPLANETS = new MDRef("ExtraPlanets");
		public static final MDRef GC_GALAXYSPACE = new MDRef("GalaxySpace");
		public static final MDRef GT = new MDRef("gregtech");
		public static final MDRef GT5U = new MDRef("gregtech");
		public static final MDRef GrC = new MDRef("Growthcraft");
		public static final MDRef HBM = new MDRef("hbm");
		public static final MDRef HEE = new MDRef("HardcoreEnderExpansion");
		public static final MDRef HEX = new MDRef("hexcraft");
		public static final MDRef HaC = new MDRef("harvestcraft");
		public static final MDRef IC2 = new MDRef("IC2");
		public static final MDRef IE = new MDRef("ImmersiveEngineering");
		public static final MDRef IHL = new MDRef("ihl");
		public static final MDRef MC = new MDRef("minecraft");
		public static final MDRef MET = new MDRef("Metallurgy");
		public static final MDRef MFR = new MDRef("MineFactoryReloaded");
		public static final MDRef MO = new MDRef("mo");
		public static final MDRef MaCu = new MDRef("Mariculture");
		public static final MDRef Mek = new MDRef("Mekanism");
		public static final MDRef MoCr = new MDRef("MoCreatures");
		public static final MDRef NeLi = new MDRef("netherlicious");
		public static final MDRef NePl = new MDRef("netheriteplus");
		public static final MDRef PE = new MDRef("ProjectE");
		public static final MDRef PFAA = new MDRef("PFAAGeologica");
		public static final MDRef PR = new MDRef("ProjRed|Core");
		public static final MDRef PnC = new MDRef("PneumaticCraft");
		public static final MDRef RC = new MDRef("Railcraft");
		public static final MDRef RH = new MDRef("globbypotato_rockhounding");
		public static final MDRef RP = new MDRef("Redpower");
		public static final MDRef RT = new MDRef("RandomThings");
		public static final MDRef ReC = new MDRef("ReactorCraft");
		public static final MDRef RoC = new MDRef("RotaryCraft");
		public static final MDRef SC2 = new MDRef("steamcraft2");
		public static final MDRef Salt = new MDRef("SaltMod");
		public static final MDRef TC = new MDRef("Thaumcraft");
		public static final MDRef TCFM = new MDRef("ForbiddenMagic");
		public static final MDRef TCTE = new MDRef("ThaumcraftExtras");
		public static final MDRef TE = new MDRef("ThermalExpansion");
		public static final MDRef TF = new MDRef("TwilightForest");
		public static final MDRef TFC = new MDRef("terrafirmacraft");
		public static final MDRef TG = new MDRef("Techguns");
		public static final MDRef TROPIC = new MDRef("tropicraft");
		public static final MDRef TiC = new MDRef("TConstruct");
		public static final MDRef UB = new MDRef("UndergroundBiomes");
		public static final MDRef VOLTZ = new MDRef("voltzengine");
		public static final MDRef WTCH = new MDRef("witchery");
	}

	// ============================ MC-strip field helpers (upstream gregapi/oredict/OreDictMaterial.java bodies, MT-local because
	// ============================ FILES_SCOPE of task gt-material-dataset only allows the constructor privatization + addAlloyingRecipe in that file) ============================

	/** Upstream UT.java:1560 bind8, UT is outside this card's FILES_SCOPE. */
	private static short bind8(long aBoundValue) {return (short)Math.max(0, Math.min(255, aBoundValue));}

	/** Upstream OreDictMaterial.setTextures(TextureSet...) :1024-1029; MC-strip: TextureSet -> {"blockVariantName","itemVariantName"}. Upstream assigns a SHARED TextureSet list; the port gives each material its own copy (safe under the resettable registry). Upstream :1025 recurses infinitely for <2 sets (latent bug, unreachable from MT call sites); the port uses empty defaults as base case. */
	public static OreDictMaterial setTextures(OreDictMaterial aMaterial, String... aSets) {
		if (aSets == null || aSets.length < 2) { aMaterial.mTextureSetsBlock = new ArrayList<>(0); aMaterial.mTextureSetsItems = new ArrayList<>(0); return aMaterial; }
		aMaterial.mTextureSetsBlock = new ArrayList<>(Arrays.asList(aSets[0]));
		aMaterial.mTextureSetsItems = new ArrayList<>(Arrays.asList(aSets[1]));
		return aMaterial;
	}

	/** Upstream OreDictMaterial.setRGBa :1032-1043 verbatim. */
	public static OreDictMaterial setRGBa(OreDictMaterial aMaterial, long aR, long aG, long aB, long aA) {
		aMaterial.mRGBaSolid[0] = aMaterial.mRGBaLiquid[0] = aMaterial.mRGBaGas[0] = aMaterial.mRGBaPlasma[0] = bind8(aR);
		aMaterial.mRGBaSolid[1] = aMaterial.mRGBaLiquid[1] = aMaterial.mRGBaGas[1] = aMaterial.mRGBaPlasma[1] = bind8(aG);
		aMaterial.mRGBaSolid[2] = aMaterial.mRGBaLiquid[2] = aMaterial.mRGBaGas[2] = aMaterial.mRGBaPlasma[2] = bind8(aB);
		aMaterial.mRGBaSolid[3] = aMaterial.mRGBaLiquid[3] = aMaterial.mRGBaGas[3] = aMaterial.mRGBaPlasma[3] = bind8(aA);
		aMaterial.fRGBaSolid[0] = aMaterial.fRGBaLiquid[0] = aMaterial.fRGBaGas[0] = aMaterial.fRGBaPlasma[0] = bind8(aR);
		aMaterial.fRGBaSolid[1] = aMaterial.fRGBaLiquid[1] = aMaterial.fRGBaGas[1] = aMaterial.fRGBaPlasma[1] = bind8(aG);
		aMaterial.fRGBaSolid[2] = aMaterial.fRGBaLiquid[2] = aMaterial.fRGBaGas[2] = aMaterial.fRGBaPlasma[2] = bind8(aB);
		aMaterial.fRGBaSolid[3] = aMaterial.fRGBaLiquid[3] = aMaterial.fRGBaGas[3] = aMaterial.fRGBaPlasma[3] = bind8(aA);
		return aMaterial;
	}

	/** Upstream OreDictMaterial.setRGBaLiquid :1060-1073 verbatim. */
	public static OreDictMaterial setRGBaLiquid(OreDictMaterial aMaterial, long aR, long aG, long aB, long aA) {
		aMaterial.mRGBaLiquid[0] = bind8(aR); aMaterial.mRGBaLiquid[1] = bind8(aG); aMaterial.mRGBaLiquid[2] = bind8(aB); aMaterial.mRGBaLiquid[3] = bind8(aA);
		aMaterial.fRGBaLiquid[0] = bind8(aR); aMaterial.fRGBaLiquid[1] = bind8(aG); aMaterial.fRGBaLiquid[2] = bind8(aB); aMaterial.fRGBaLiquid[3] = bind8(aA);
		return aMaterial;
	}

	/** Upstream OreDictMaterial.setDensity :1146-1149 verbatim. */
	public static OreDictMaterial setDensity(OreDictMaterial aMaterial, double aGramPerCubicCentimeter) {aMaterial.mGramPerCubicCentimeter = aGramPerCubicCentimeter; return aMaterial;}

	/** Upstream OreDictMaterial.setOreMultiplier :1193-1196 verbatim. */
	public static OreDictMaterial setOreMultiplier(OreDictMaterial aMaterial, int aMultiplier) {aMaterial.mOreMultiplier = (byte)Math.max(1, aMultiplier); return aMaterial;}

	/** Upstream OreDictMaterial.setPriorityPrefix(int) :1334-1345; only the index assignment is ported, the OreDictManager.INSTANCE.addReRegistrationWithReversal side effects (:1338-1341) belong to the manager/prefix tasks. */
	public static OreDictMaterial setPriorityPrefix(OreDictMaterial aMaterial, int aIndex) {aMaterial.mPriorityPrefixIndex = aIndex; return aMaterial;}

	/** Upstream OreDictMaterial.ores :1239-1242 verbatim. */
	public static OreDictMaterial ores(OreDictMaterial aMaterial, OreDictMaterial... aOreMaterials) {aMaterial.mByProducts.addAll(Arrays.asList(aOreMaterials)); return aMaterial;}

	/** Upstream OreDictMaterial.steal :1101-1106 verbatim. */
	public static OreDictMaterial steal(OreDictMaterial aMaterial, OreDictMaterial aStatsToCopy) {
		aMaterial.heat(aStatsToCopy);
		aMaterial.stealStatsElement(aStatsToCopy);
		aMaterial.qual(aStatsToCopy);
		return aMaterial;
	}

	/** Upstream OreDictMaterial.stealLooks :1108-1125 verbatim (fields are public). */
	public static OreDictMaterial stealLooks(OreDictMaterial aMaterial, OreDictMaterial aStatsToCopy) {
		aMaterial.mTextureSetsItems = aStatsToCopy.mTextureSetsItems;
		aMaterial.mTextureSetsBlock = aStatsToCopy.mTextureSetsBlock;
		for (byte i = 0; i < 4; i++) {
			aMaterial.mRGBa       [i] = aStatsToCopy.mRGBa       [i];
			aMaterial.mRGBaSolid  [i] = aStatsToCopy.mRGBaSolid  [i];
			aMaterial.mRGBaLiquid [i] = aStatsToCopy.mRGBaLiquid [i];
			aMaterial.mRGBaGas    [i] = aStatsToCopy.mRGBaGas    [i];
			aMaterial.mRGBaPlasma [i] = aStatsToCopy.mRGBaPlasma [i];
			aMaterial.fRGBa       [i] = aStatsToCopy.fRGBa       [i];
			aMaterial.fRGBaSolid  [i] = aStatsToCopy.fRGBaSolid  [i];
			aMaterial.fRGBaLiquid [i] = aStatsToCopy.fRGBaLiquid [i];
			aMaterial.fRGBaGas    [i] = aStatsToCopy.fRGBaGas    [i];
			aMaterial.fRGBaPlasma [i] = aStatsToCopy.fRGBaPlasma [i];
		}
		return aMaterial;
	}

	/** Upstream gregapi/util/OM.java:485 OM.stack semantics (OM is not ported; same null->null contract the model card already replicated privately). */
	static OreDictMaterialStack stack(OreDictMaterial aMaterial, long aAmount) {return aMaterial == null ? null : new OreDictMaterialStack(aMaterial, aAmount);}

	/** Upstream put(Object...) :1463-1466 dispatches ModData args to setOriginalMod at ANY depth (the vararg chains of MT.java nest aRandomData arrays when helpers forward them); the ported OreDictMaterial.put has no ModData branch, so the create() funnel pre-processes aData recursively here. */
	static OreDictMaterial putWithMods(OreDictMaterial aMaterial, Object[] aData, Object aExtra) {
		List<Object> tRest = new ArrayList<>();
		collectWithoutMods(aMaterial, aData, tRest);
		if (aExtra != null) tRest.add(aExtra);
		return aMaterial.put(tRest.toArray());
	}

	private static void collectWithoutMods(OreDictMaterial aMaterial, Object[] aData, List<Object> aRest) {
		if (aData == null) return;
		for (Object aObject : aData) {
			if (aObject instanceof MDRef) aMaterial.setOriginalMod(((MDRef)aObject).mID);
			else if (aObject instanceof Object[]) collectWithoutMods(aMaterial, (Object[])aObject, aRest);
			else if (aObject != null) aRest.add(aObject);
		}
	}

	/** Upstream :1879-1883 static{} block. */
	private static void fixups() {
		H2O.setSolidifying(Ice, U);
		setDensity(Lava.setSolidifying(Obsidian, U), Obsidian.mGramPerCubicCentimeter);
		Netherrack.setSmelting(NetherBrick, U);
	}

	// ============================ builder helpers (upstream MT.java:51-520, TextureSet[] -> String[]; MC-strip applied per rule) ============================
		// upstream MT.java:51
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:52
	static OreDictMaterial tier         (String aNameOreDict) {return create(-1, aNameOreDict).setOriginalMod(MD.GAPI.mID).put(UNUSED_MATERIAL,  DONT_SHOW_THIS_COMPONENT,  IGNORE_IN_COLOR_LOG).setAllToTheOutputOf(null, 0, 1);}
		// upstream MT.java:53
	static OreDictMaterial unused       (String aNameOreDict) {return create(-1, aNameOreDict).put(UNUSED_MATERIAL, DONT_SHOW_THIS_COMPONENT);}
		// upstream MT.java:54
	static OreDictMaterial deprecated   (String aNameOreDict) {return create(-1, aNameOreDict).put(UNUSED_MATERIAL, DONT_SHOW_THIS_COMPONENT);}
		// upstream MT.java:55
	static OreDictMaterial invalid      (String aNameOreDict) {return create(-1, aNameOreDict).put(UNUSED_MATERIAL, DONT_SHOW_THIS_COMPONENT, INVALID_MATERIAL);}
		// upstream MT.java:56
	static OreDictMaterial create       (int aID, String aNameOreDict) {if (aID >= 10000) return null; OreDictMaterial rMaterial = OreDictMaterial.createMaterial(aID, aNameOreDict, aNameOreDict); ALL_MATERIALS_REGISTERED_HERE.add(rMaterial); if (aID > 0) rMaterial.setOriginalMod(MD.GAPI.mID); return rMaterial.handle(ANY.WoodPlastic);}
		// upstream MT.java:57
	static OreDictMaterial create       (int aID, String aNameOreDict, String[] aSets) {return setTextures(create(aID, aNameOreDict), aSets);}
		// upstream MT.java:58
	static OreDictMaterial create       (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return putWithMods(setRGBa(create(aID, aNameOreDict, aSets), aR, aG, aB, aA), aRandomData, aR==256?UNUSED_MATERIAL:null).hide(aR==256);}
		// upstream MT.java:60
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:61
	static OreDictMaterial element      (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return create      (aID, aNameOreDict, aSets, aR, aG, aB, aA, aRandomData).setStats(aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter).put(ELEMENT).tooltip(aSymbol);}
		// upstream MT.java:62
	static OreDictMaterial metal        (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return element     (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(METAL    , G_INGOT_ORES, SMITHABLE, MELTING        , EXTRUDER, aMeltingPoint <= 1200 ? new Object[] {EXTRUDER_SIMPLE, FURNACE, MORTAR} : null);}
		// upstream MT.java:63
	static OreDictMaterial metalloid    (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return element     (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(METALLOID, G_INGOT_ORES, SMITHABLE, MELTING, MOLTEN, EXTRUDER, aMeltingPoint <= 1200 ? new Object[] {EXTRUDER_SIMPLE, FURNACE, MORTAR} : null);}
		// upstream MT.java:64
	static OreDictMaterial nonmetal     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return element     (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(NONMETAL);}
		// upstream MT.java:65
	static OreDictMaterial diatomic     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return nonmetal    (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(DIATOMIC_NONMETAL);}
		// upstream MT.java:66
	static OreDictMaterial diatomicgas  (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return diatomic    (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(GASES);}
		// upstream MT.java:67
	static OreDictMaterial polyatomic   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return nonmetal    (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(POLYATOMIC_NONMETAL);}
		// upstream MT.java:68
	static OreDictMaterial noblegas     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return nonmetal    (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, 15, aRandomData).put(NOBLE_GAS, GASES);}
		// upstream MT.java:69
	static OreDictMaterial alkali       (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(ALKALI_METAL, MOLTEN);}
		// upstream MT.java:70
	static OreDictMaterial alkaline     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(ALKALINE_EARTH_METAL, MOLTEN);}
		// upstream MT.java:71
	static OreDictMaterial lanthanide   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(LANTHANIDE);}
		// upstream MT.java:72
	static OreDictMaterial actinide     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(ACTINIDE);}
		// upstream MT.java:73
	static OreDictMaterial transmetal   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(TRANSITION_METAL);}
		// upstream MT.java:74
	static OreDictMaterial precmetal    (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return transmetal  (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(PRECIOUS_METAL);}
		// upstream MT.java:75
	static OreDictMaterial noblemetal   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return precmetal   (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB    , aRandomData).put(NOBLE_METAL);}
		// upstream MT.java:76
	static OreDictMaterial refractmetal (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData) {return transmetal  (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA, aRandomData).put(REFRACTORY_METAL, WASHING_FIRESTONE);}
		// upstream MT.java:77
	static OreDictMaterial platingroup  (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return precmetal   (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB    , aRandomData).put(PLATINUM_GROUP);}
		// upstream MT.java:78
	static OreDictMaterial posttrans    (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB         , Object... aRandomData) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB,255, aRandomData).put(POST_TRANSITION_METAL);}
		// upstream MT.java:80
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:81
	static OreDictMaterial element      (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return create      (aID, aNameOreDict, aSets, 256, 256, 256, 255).setStats(aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter).put(aTags, ELEMENT).tooltip(aSymbol);}
		// upstream MT.java:82
	static OreDictMaterial element      (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData[] aTags2, TagData... aTags                       ) {return create      (aID, aNameOreDict, aSets, 256, 256, 256, 255).setStats(aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter).put(aTags, aTags2, ELEMENT).tooltip(aSymbol);}
		// upstream MT.java:83
	static OreDictMaterial element      (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData aTag3, TagData[] aTags2, TagData... aTags        ) {return create      (aID, aNameOreDict, aSets, 256, 256, 256, 255).setStats(aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter).put(aTags, aTags2, aTag3, ELEMENT).tooltip(aSymbol);}
		// upstream MT.java:84
	static OreDictMaterial unknown      (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons                                                                                            , TagData... aTags                                         ) {return element     (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, 1000, 3000, 0, SET_SHINY                                                        ).put(aTags).hide();}
		// upstream MT.java:85
	static OreDictMaterial metalloid    (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return element     (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, METALLOID, G_INGOT_ORES, SMITHABLE, MELTING, MOLTEN, EXTRUDER, aMeltingPoint <= 1200 ? new Object[] {EXTRUDER_SIMPLE, FURNACE, MORTAR} : null);}
		// upstream MT.java:86
	static OreDictMaterial alkali       (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, ALKALI_METAL, MOLTEN);}
		// upstream MT.java:87
	static OreDictMaterial alkaline     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, ALKALINE_EARTH_METAL, MOLTEN);}
		// upstream MT.java:88
	static OreDictMaterial lanthanide   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, LANTHANIDE);}
		// upstream MT.java:89
	static OreDictMaterial actinide     (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, ACTINIDE);}
		// upstream MT.java:90
	static OreDictMaterial transmetal   (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, TRANSITION_METAL);}
		// upstream MT.java:91
	static OreDictMaterial posttrans    (int aID, String aNameOreDict, String aSymbol, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, TagData... aTags                                         ) {return metal       (aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, 256, 256, 256, 255).put(aTags, POST_TRANSITION_METAL);}
		// upstream MT.java:93
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:94
	static OreDictMaterial dcmp         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:95
	static OreDictMaterial cent         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dcmp            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:96
	static OreDictMaterial elec         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dcmp            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:97
	static OreDictMaterial gas          (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS);}
		// upstream MT.java:98
	static OreDictMaterial gasdcmp      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gas             (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:99
	static OreDictMaterial gasflam      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gas             (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:100
	static OreDictMaterial gasexpl      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gas             (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:101
	static OreDictMaterial gascent      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasdcmp         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:102
	static OreDictMaterial gaselec      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasdcmp         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:103
	static OreDictMaterial lqud         (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID);}
		// upstream MT.java:104
	static OreDictMaterial lqudflam     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqud            (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE);}
		// upstream MT.java:105
	static OreDictMaterial lqudexpl     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqud            (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE, EXPLOSIVE);}
		// upstream MT.java:106
	static OreDictMaterial lquddcmp     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqud            (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:107
	static OreDictMaterial lqudcent     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lquddcmp        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:108
	static OreDictMaterial lqudelec     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lquddcmp        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:109
	static OreDictMaterial gaschem      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS);}
		// upstream MT.java:110
	static OreDictMaterial gaschemdcmp  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschem         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:111
	static OreDictMaterial gaschemflam  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschem         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(FLAMMABLE);}
		// upstream MT.java:112
	static OreDictMaterial gaschemexpl  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschem         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(FLAMMABLE, EXPLOSIVE);}
		// upstream MT.java:113
	static OreDictMaterial gaschemcent  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschemdcmp     (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:114
	static OreDictMaterial gaschemelec  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschemdcmp     (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:115
	static OreDictMaterial lqudchem     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID);}
		// upstream MT.java:116
	static OreDictMaterial lqudchemdcmp (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchem        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:117
	static OreDictMaterial lqudchemflam (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchem        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE);}
		// upstream MT.java:118
	static OreDictMaterial lqudchemexpl (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchem        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE, EXPLOSIVE);}
		// upstream MT.java:119
	static OreDictMaterial lqudchemcent (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchemdcmp    (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:120
	static OreDictMaterial lqudchemelec (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchemdcmp    (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:121
	static OreDictMaterial gasacid      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS, ACID);}
		// upstream MT.java:122
	static OreDictMaterial gasaciddcmp  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasacid         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:123
	static OreDictMaterial gasacidflam  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasacid         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(FLAMMABLE);}
		// upstream MT.java:124
	static OreDictMaterial gasacidexpl  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasacid         (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(FLAMMABLE, EXPLOSIVE);}
		// upstream MT.java:125
	static OreDictMaterial gasacidcent  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasaciddcmp     (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:126
	static OreDictMaterial gasacidelec  (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasaciddcmp     (aID, aNameOreDict, SET_GAS         , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:127
	static OreDictMaterial lqudacid     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID, ACID);}
		// upstream MT.java:128
	static OreDictMaterial lqudaciddcmp (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudacid        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:129
	static OreDictMaterial lqudacidflam (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudacid        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE);}
		// upstream MT.java:130
	static OreDictMaterial lqudacidexpl (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudacid        (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(FLAMMABLE, EXPLOSIVE);}
		// upstream MT.java:131
	static OreDictMaterial lqudacidcent (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudaciddcmp    (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:132
	static OreDictMaterial lqudacidelec (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudaciddcmp    (aID, aNameOreDict, SET_FLUID       , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:133
	static OreDictMaterial gas          (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS);}
		// upstream MT.java:134
	static OreDictMaterial gasdcmp      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gas             (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:135
	static OreDictMaterial gascent      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasdcmp         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:136
	static OreDictMaterial gaselec      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasdcmp         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:137
	static OreDictMaterial lqud         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID);}
		// upstream MT.java:138
	static OreDictMaterial lquddcmp     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqud            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:139
	static OreDictMaterial lqudcent     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lquddcmp        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:140
	static OreDictMaterial lqudelec     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lquddcmp        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:141
	static OreDictMaterial gaschem      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS);}
		// upstream MT.java:142
	static OreDictMaterial gaschemdcmp  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschem         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:143
	static OreDictMaterial gaschemcent  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschemdcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:144
	static OreDictMaterial gaschemelec  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gaschemdcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:145
	static OreDictMaterial lqudchem     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID);}
		// upstream MT.java:146
	static OreDictMaterial lqudchemdcmp (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchem        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:147
	static OreDictMaterial lqudchemcent (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchemdcmp    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:148
	static OreDictMaterial lqudchemelec (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudchemdcmp    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:149
	static OreDictMaterial gasacid      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_GAS, ACID);}
		// upstream MT.java:150
	static OreDictMaterial gasaciddcmp  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasacid         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:151
	static OreDictMaterial gasacidcent  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasaciddcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:152
	static OreDictMaterial gasacidelec  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gasaciddcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:153
	static OreDictMaterial lqudacid     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_CONTAINERS, CONTAINERS_FLUID, ACID);}
		// upstream MT.java:154
	static OreDictMaterial lqudaciddcmp (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudacid        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:155
	static OreDictMaterial lqudacidcent (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudaciddcmp    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:156
	static OreDictMaterial lqudacidelec (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return lqudaciddcmp    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:159
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:160
	static OreDictMaterial dust         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setPriorityPrefix(create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_DUST, MORTAR), 2);}
		// upstream MT.java:161
	static OreDictMaterial dustdcmp     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dust            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:162
	static OreDictMaterial dustcent     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dustdcmp        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:163
	static OreDictMaterial dustelec     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dustdcmp        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:164
	static OreDictMaterial glowstone    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setOreMultiplier(setPriorityPrefix(oredustcent     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(PLATES, STICKS, MORTAR, BRITTLE, MELTING, CRYSTAL, CRYSTALLISABLE, G_GEM_ORES_TRANSPARENT, ANY.Glowstone, MOLTEN, GLOWING, LIGHTING), 2), 4);}
		// upstream MT.java:165
	static OreDictMaterial redstone     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setOreMultiplier(setPriorityPrefix(oredustcent     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(PLATES, STICKS, MORTAR, BRITTLE, MELTING, CRYSTAL, CRYSTALLISABLE, G_GEM_ORES_TRANSPARENT), 2), 4);}
		// upstream MT.java:166
	static OreDictMaterial coal         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setPriorityPrefix(elec            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_GEM_ORES, BRITTLE, FLAMMABLE, MORTAR, INGOTS, COAL), 1);}
		// upstream MT.java:167
	static OreDictMaterial wax          (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dust            (aID, aNameOreDict, SET_FOOD        , aR, aG, aB, aA, aRandomData).put(ANY.Wax, FOILS, PLATES, INGOTS, PARTS, FURNACE, MELTING, BRITTLE, MORTAR, EXTRUDER, EXTRUDER_SIMPLE);}
		// upstream MT.java:168
	static OreDictMaterial meat         (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dustfood        (aID, aNameOreDict, SET_FINE        , aR, aG, aB, aA, aRandomData).put(MEAT, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE);}
		// upstream MT.java:169
	static OreDictMaterial grain        (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dustfood        (aID, aNameOreDict, SET_POWDER      , aR, aG, aB, aA, aRandomData).put(ANY.Grains, ANY.FlourGrains, FLAMMABLE).setBurning(Ash, U9);}
		// upstream MT.java:170
	static OreDictMaterial food         (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:171
	static OreDictMaterial orefood      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return oredust         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:172
	static OreDictMaterial dustfood     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dust            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:173
	static OreDictMaterial mixfood      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return mixdust         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:174
	static OreDictMaterial food         (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_FOOD        , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:175
	static OreDictMaterial orefood      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return oredust         (aID, aNameOreDict, SET_FINE        , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:176
	static OreDictMaterial dustfood     (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dust            (aID, aNameOreDict, SET_FINE        , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:177
	static OreDictMaterial mixfood      (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return mixdust         (aID, aNameOreDict, SET_FINE        , aR, aG, aB, aA, aRandomData).put(FOOD, MORTAR);}
		// upstream MT.java:178
	static OreDictMaterial dye          (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return dust            (aID, aNameOreDict, SET_FOOD        , aR, aG, aB,255, aRandomData).put(DONT_SHOW_THIS_COMPONENT);}
		// upstream MT.java:179
	static OreDictMaterial quartz       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setOreMultiplier(setPriorityPrefix(create          (aID, aNameOreDict, SET_QUARTZ      , aR, aG, aB, aA, aRandomData).put(G_QUARTZ_ORES, ANY.Quartz, ANY.SiO2, MORTAR, BRITTLE, QUARTZ, BLACKLISTED_SMELTER).setSmelting(SiO2, U), 1), 2);}
		// upstream MT.java:180
	static OreDictMaterial gem          (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setPriorityPrefix(create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_GEM_ORES), 1);}
		// upstream MT.java:181
	static OreDictMaterial gemdcmp      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gem             (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:182
	static OreDictMaterial gemcent      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gemdcmp         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:183
	static OreDictMaterial gemelec      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gemdcmp         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:184
	static OreDictMaterial stonedcmp    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stone           (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:185
	static OreDictMaterial stonecent    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stonedcmp       (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:186
	static OreDictMaterial stoneelec    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stonedcmp       (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:187
	static OreDictMaterial stone        (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stone           (aID, aNameOreDict, SET_STONE       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:188
	static OreDictMaterial stonedcmp    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stonedcmp       (aID, aNameOreDict, SET_STONE       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:189
	static OreDictMaterial stonecent    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stonecent       (aID, aNameOreDict, SET_STONE       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:190
	static OreDictMaterial stoneelec    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stoneelec       (aID, aNameOreDict, SET_STONE       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:191
	static OreDictMaterial brickdcmp    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stone           (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:192
	static OreDictMaterial brickcent    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return brickdcmp       (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:193
	static OreDictMaterial brickelec    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return brickdcmp       (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:194
	static OreDictMaterial brick        (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return stone           (aID, aNameOreDict, SET_BRICK       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:195
	static OreDictMaterial brickdcmp    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return brickdcmp       (aID, aNameOreDict, SET_BRICK       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:196
	static OreDictMaterial brickcent    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return brickcent       (aID, aNameOreDict, SET_BRICK       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:197
	static OreDictMaterial brickelec    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return brickelec       (aID, aNameOreDict, SET_BRICK       , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:198
	static OreDictMaterial crystal      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return setPriorityPrefix(create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_GEM_ORES_TRANSPARENT, CRYSTAL), 1);}
		// upstream MT.java:199
	static OreDictMaterial crystaldcmp  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return crystal         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:200
	static OreDictMaterial crystalcent  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return crystaldcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:201
	static OreDictMaterial crystalelec  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return crystaldcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:202
	static OreDictMaterial crystal_tc   (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return setOreMultiplier(crystal         (aID, aNameOreDict, SET_SHARDS      , aR, aG, aB,255, aRandomData).setOriginalMod(MD.TC.mID).put( ANY.ThaumCrystal,  COMMON_ORE,  MAGICAL,  UNBURNABLE).handle(ANY.WoodMagical), 2);}
		// upstream MT.java:203
	static OreDictMaterial hexorium     (int aID                                         , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return setOreMultiplier(crystal(aID, "Hexorium"+DYE_NAMES[aColor], SET_HEX  , aR, aG, aB,127, aRandomData).setOriginalMod(MD.HEX.mID).put( ANY.Hexorium    ,  COMMON_ORE,  GLOWING,  MORTAR,  BRITTLE,  CRYSTALLISABLE,  BLACKLISTED_SMELTER).setSmelting(null, 0), aColor == DYE_INDEX_Black || aColor == DYE_INDEX_White ? 3 : 4).setLocal(DYE_NAMES[aColor] + " Hexorium").qual(2, 5.0, aColor == DYE_INDEX_Black ? 512 : aColor == DYE_INDEX_White ? 384 : 256, 2);}
		// upstream MT.java:204
	static OreDictMaterial valgem       (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return gem             (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_GEM_ORES_TRANSPARENT, CRYSTAL, VALUABLE);}
		// upstream MT.java:205
	static OreDictMaterial valgemdcmp   (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return valgem          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE).setSmelting(null, 0);}
		// upstream MT.java:206
	static OreDictMaterial valgemcent   (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return valgemdcmp      (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:207
	static OreDictMaterial valgemelec   (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return valgemdcmp      (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:208
	static OreDictMaterial diamond      (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return steal(valgemdcmp      (aID, aNameOreDict, SET_DIAMOND     , aR, aG, aB,127, aRandomData).put(ANY.Diamond   , COMMON_ORE                               ), C).qual(3, 8.0,1280, 3).setSmelting(C    , 2*U  ).setBurning(Ash, U);}
		// upstream MT.java:209
	static OreDictMaterial sapphire     (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemcent      (aID, aNameOreDict, SET_GEM_VERTICAL, aR, aG, aB,127, aRandomData).put(ANY.Sapphire  , COMMON_ORE, MELTING, RANDOM_SMALL_GEM_ORE)         .qual(3, 7.0, 512, 3).setSmelting(Al2O3, 3*U4 ).addSourceOf(Al);}
		// upstream MT.java:210
	static OreDictMaterial emerald      (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemelec      (aID, aNameOreDict, SET_EMERALD     , aR, aG, aB,127, aRandomData).put(ANY.Emerald   , COMMON_ORE, MELTING, RANDOM_SMALL_GEM_ORE)         .qual(3, 9.0, 128, 2).setSmelting(Be   ,   U36).addSourceOf(Be);}
		// upstream MT.java:211
	static OreDictMaterial garnet       (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemelec      (aID, aNameOreDict, SET_RUBY        , aR, aG, aB,127, aRandomData).setOriginalMod(MD.GT.mID).put(ANY.Garnet    ,  RANDOM_SMALL_GEM_ORE)         .qual(3, 7.0, 128, 2);}
		// upstream MT.java:212
	static OreDictMaterial jasper       (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemelec      (aID, aNameOreDict, SET_GLASS       , aR, aG, aB,150, aRandomData).setOriginalMod(MD.RH.mID).put(ANY.Jasper    ,  RANDOM_SMALL_GEM_ORE)         .qual(3, 7.0, 256, 2).uumMcfg( 0, SiO2, 2*U, Fe, 1*U);}
		// upstream MT.java:213
	static OreDictMaterial tigereye     (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemelec      (aID, aNameOreDict, SET_GLASS       , aR, aG, aB,200, aRandomData).setOriginalMod(MD.RH.mID).put(ANY.TigerEye  ,  RANDOM_SMALL_GEM_ORE)         .qual(3, 7.0, 256, 2).uumMcfg( 0, SiO2, 1*U);}
		// upstream MT.java:214
	static OreDictMaterial aventurine   (int aID, String aNameOreDict                    , long aR, long aG, long aB, byte aColor, Object... aRandomData)  {return valgemelec      (aID, aNameOreDict, SET_GLASS       , aR, aG, aB,200, aRandomData).setOriginalMod(MD.RH.mID).put(ANY.Aventurine,  RANDOM_SMALL_GEM_ORE)         .qual(3, 7.0, 256, 2).uumMcfg( 0, SiO2, 1*U);}
		// upstream MT.java:215
	static OreDictMaterial fluorite     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return gem             (aID, aNameOreDict, SET_RUBY        , aR, aG, aB,255, aRandomData)             .setOriginalMod(MD.ReC.mID).put(ANY.CaF2      ,  COMMON_ORE,  RANDOM_SMALL_GEM_ORE)                              .uumMcfg( 0, Ca, 1*U, F, 2*U).addSourceOf(F).heat(1633).put(DECOMPOSABLE, ACID, MELTING, MORTAR, BRITTLE, CRYSTALLISABLE).setSmelting("Fluorite".equals(aNameOreDict) ? null : CaF2, U);}
		// upstream MT.java:216
	static OreDictMaterial blaze        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_POWDER      , aR, aG, aB,255, aRandomData)             .put(ANY.Blaze     , COMMON_ORE                               )         .qual(1, 2.0,  16, 1).handle(ANY.Blaze).put(G_BLAZE, GLOWING, MAGICAL, BRITTLE, MORTAR);}
		// upstream MT.java:217
	static OreDictMaterial clay         (int aID, String aNameOreDict         , long aR, long aG, long aB, OreDictMaterial aTrace, Object... aRandomData)  {return oredustelec     (aID, aNameOreDict, SET_ROUGH       , aR, aG, aB,255, aRandomData)             .put(ANY.Clay      , MORTAR, PLATES                           )         .uumMcfg(18, aTrace, 1*U, Clay, 18*U).heat(2000).setSmelting(Ceramic, U);}
		// upstream MT.java:218
	static OreDictMaterial mix          (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dcmp            (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:219
	static OreDictMaterial mixdust      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return dustcent        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:220
	static OreDictMaterial oredust      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_DUST_ORES);}
		// upstream MT.java:221
	static OreDictMaterial oredustdcmp  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return oredust         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(DECOMPOSABLE);}
		// upstream MT.java:222
	static OreDictMaterial oredustcent  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return oredustdcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(CENTRIFUGE);}
		// upstream MT.java:223
	static OreDictMaterial oredustelec  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return oredustdcmp     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ELECTROLYSER);}
		// upstream MT.java:224
	static OreDictMaterial metal_       (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:225
	static OreDictMaterial metalore_    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:226
	static OreDictMaterial metalmachine_(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:227
	static OreDictMaterial metalmachore_(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:228
	static OreDictMaterial metal_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:229
	static OreDictMaterial metalore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:230
	static OreDictMaterial metalmachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:231
	static OreDictMaterial metalmachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:232
	static OreDictMaterial setal_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:233
	static OreDictMaterial setalore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:234
	static OreDictMaterial setalmachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:235
	static OreDictMaterial setalmachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:236
	static OreDictMaterial cetal_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:237
	static OreDictMaterial cetalore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:238
	static OreDictMaterial cetalmachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:239
	static OreDictMaterial cetalmachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:240
	static OreDictMaterial alloy_       (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:241
	static OreDictMaterial alloyore_    (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalore        (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:242
	static OreDictMaterial alloymachine_(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:243
	static OreDictMaterial alloymachore_(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:244
	static OreDictMaterial alloy_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:245
	static OreDictMaterial alloyore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:246
	static OreDictMaterial alloymachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:247
	static OreDictMaterial alloymachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:248
	static OreDictMaterial slloy_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:249
	static OreDictMaterial slloyore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:250
	static OreDictMaterial slloymachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:251
	static OreDictMaterial slloymachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:252
	static OreDictMaterial clloy_       (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:253
	static OreDictMaterial clloyore_    (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:254
	static OreDictMaterial clloymachine_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:255
	static OreDictMaterial clloymachore_(int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:256
	static OreDictMaterial metalnd      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT_ND, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:257
	static OreDictMaterial metalmachnd  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalnd         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(G_INGOT_ND_MACHINE);}
		// upstream MT.java:258
	static OreDictMaterial alloynd      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalnd         (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:259
	static OreDictMaterial alloymachnd  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA    , Object... aRandomData)  {return metalmachnd     (aID, aNameOreDict, aSets           , aR, aG, aB, aA, aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:260
	static OreDictMaterial metal        (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB,255, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:261
	static OreDictMaterial metalore     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:262
	static OreDictMaterial metalmachine (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:263
	static OreDictMaterial metalmachore (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:264
	static OreDictMaterial metal        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB,255, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:265
	static OreDictMaterial metalore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:266
	static OreDictMaterial metalmachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:267
	static OreDictMaterial metalmachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:268
	static OreDictMaterial setal        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_SHINY       , aR, aG, aB,255, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:269
	static OreDictMaterial setalore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:270
	static OreDictMaterial setalmachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:271
	static OreDictMaterial setalmachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:272
	static OreDictMaterial cetal        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, SET_COPPER      , aR, aG, aB,255, aRandomData).put(G_INGOT, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:273
	static OreDictMaterial cetalore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(G_INGOT_ORES);}
		// upstream MT.java:274
	static OreDictMaterial cetalmachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE);}
		// upstream MT.java:275
	static OreDictMaterial cetalmachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(G_INGOT_MACHINE_ORES);}
		// upstream MT.java:276
	static OreDictMaterial alloy        (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:277
	static OreDictMaterial alloyore     (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalore        (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:278
	static OreDictMaterial alloymachine (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:279
	static OreDictMaterial alloymachore (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:280
	static OreDictMaterial alloy        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:281
	static OreDictMaterial alloyore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:282
	static OreDictMaterial alloymachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:283
	static OreDictMaterial alloymachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_METALLIC    , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:284
	static OreDictMaterial slloy        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:285
	static OreDictMaterial slloyore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:286
	static OreDictMaterial slloymachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:287
	static OreDictMaterial slloymachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_SHINY       , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:288
	static OreDictMaterial clloy        (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metal           (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:289
	static OreDictMaterial clloyore     (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalore        (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:290
	static OreDictMaterial clloymachine (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachine    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:291
	static OreDictMaterial clloymachore (int aID, String aNameOreDict                    , long aR, long aG, long aB             , Object... aRandomData)  {return metalmachore    (aID, aNameOreDict, SET_COPPER      , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:292
	static OreDictMaterial metalnd      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return create          (aID, aNameOreDict, aSets           , aR, aG, aB,255, aRandomData).put(G_INGOT_ND, SMITHABLE, MELTING, EXTRUDER);}
		// upstream MT.java:293
	static OreDictMaterial metalmachnd  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalnd         (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(G_INGOT_ND_MACHINE);}
		// upstream MT.java:294
	static OreDictMaterial alloynd      (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalnd         (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:295
	static OreDictMaterial alloymachnd  (int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB             , Object... aRandomData)  {return metalmachnd     (aID, aNameOreDict, aSets           , aR, aG, aB    , aRandomData).put(ALLOY, DECOMPOSABLE);}
		// upstream MT.java:296
	static OreDictMaterial wood         (int aID, String aNameOreDict                    , long aR, long aG, long aB, long aA    , Object... aRandomData)  {return wood            (aID, aNameOreDict, SET_WOOD        , aR, aG, aB, aA, aRandomData);}
		// upstream MT.java:298
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:299
	static OreDictMaterial wood(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData)  {
		// upstream MT.java:300
		OreDictMaterial rMaterial = create(aID, aNameOreDict, aSets, aR, aG, aB, aA, aRandomData, G_WOOD, ANY.Wood, ANY.WoodPlastic, WOOD, MORTAR);
		// upstream MT.java:301
		String tPlank = "plank"+rMaterial.mNameInternal;
		// upstream MT.java:302
		// upstream MT.java:303
		// upstream MT.java:304
		if ("Wood".equalsIgnoreCase(rMaterial.mNameInternal)) return rMaterial;
		// upstream MT.java:305
		// upstream MT.java:306
		// upstream MT.java:307
		return rMaterial;
		// upstream MT.java:308
	}
		// upstream MT.java:310
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:311
	static OreDictMaterial woodnormal(int aID, String aNameOreDict, String aLocal, long aR, long aG, long aB, double aSpeed, long aDurability, Object... aRandomData) {
		// upstream MT.java:312
		OreDictMaterial rMaterial = create(aID, aNameOreDict, SET_WOOD, aR, aG, aB, 255, aRandomData, G_WOOD, ANY.Wood, ANY.WoodPlastic, ANY.WoodNormal, ANY.WoodDefault, ANY.WoodUntreated, WOOD, MORTAR, TICKS_PER_SMELT/2, FLAMMABLE, APPROXIMATE).setLocal(aLocal).uumMcfg( 0, C, 6*U, H2O,15*U).setBurning(Ash, U9).setSmelting(Ash, U4).qual(1, aSpeed, aDurability, 0).heat(400, 500);
		// upstream MT.java:313
		String tPlank = "plank"+rMaterial.mNameInternal, tStick = "stick"+rMaterial.mNameInternal;
		// upstream MT.java:314
		// upstream MT.java:315
		// upstream MT.java:316
		// upstream MT.java:317
		// upstream MT.java:318
		// upstream MT.java:319
		return rMaterial;
		// upstream MT.java:320
	}
		// upstream MT.java:322
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:323
	static OreDictMaterial stone(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA, Object... aRandomData)  {
		// upstream MT.java:324
		OreDictMaterial rMaterial = create(aID, aNameOreDict, aSets, aR, aG, aB, aA, aRandomData, G_STONE, ANY.Stone, STONE, BRITTLE, MORTAR, FURNACE, EXTRUDER, EXTRUDER_SIMPLE);
		// upstream MT.java:325
		String tStone = "stone"+rMaterial.mNameInternal;
		// upstream MT.java:326
		// upstream MT.java:327
		// upstream MT.java:328
		return rMaterial;
		// upstream MT.java:329
	}
		// upstream MT.java:331
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:332
	static OreDictMaterial gem_aa(int aID, String aNameOreDict, String[] aSets, long aR, long aG, long aB, long aA, OreDictMaterial aCopy, Object... aRandomData) {
		// upstream MT.java:333
		return steal(dcmp(aID, aNameOreDict, aSets, aR, aG, aB, aA, G_GEM_TRANSPARENT, CRYSTAL, BRITTLE, MD.AA).uumMcfg(0, aCopy, U), aCopy).setAllToTheOutputOf(aCopy);
		// upstream MT.java:334
	}
		// upstream MT.java:336
	/** Making the Table a little bit more overviewable. DO NOT USE THESE FUNCTIONS YOURSELF!!! Use "OreDictMaterial.createMaterial(YOUR-ID-AS-SPECIFIED-IN-THE-ID-RANGES, OREDICT-NAME, LOCALISED-NAME)" */
		// upstream MT.java:337
	static OreDictMaterial unknown(int aID, long aNeutrons, Object... aRandomData) {
		// upstream MT.java:338
		int aProtonsAndElectrons = aID / 10;
		// upstream MT.java:339
		String aNameOreDict, aSymbol;
		// upstream MT.java:341
		switch (aProtonsAndElectrons / 100) {
		// upstream MT.java:342
		case 1: aNameOreDict  = "Un"     ; aSymbol  = "U"; break;
		// upstream MT.java:343
		case 2: aNameOreDict  = "Bi"     ; aSymbol  = "B"; break;
		// upstream MT.java:344
		case 3: aNameOreDict  = "Tri"    ; aSymbol  = "T"; break;
		// upstream MT.java:345
		case 4: aNameOreDict  = "Quad"   ; aSymbol  = "Q"; break;
		// upstream MT.java:346
		case 5: aNameOreDict  = "Pent"   ; aSymbol  = "P"; break;
		// upstream MT.java:347
		case 6: aNameOreDict  = "Hex"    ; aSymbol  = "H"; break;
		// upstream MT.java:348
		case 7: aNameOreDict  = "Sept"   ; aSymbol  = "S"; break;
		// upstream MT.java:349
		case 8: aNameOreDict  = "Oct"    ; aSymbol  = "O"; break;
		// upstream MT.java:350
		case 9: aNameOreDict  = "Enn"    ; aSymbol  = "E"; break;
		// upstream MT.java:351
		default: throw new IllegalArgumentException("Disallowed Parameter");
		// upstream MT.java:352
		}
		// upstream MT.java:353
		switch ((aProtonsAndElectrons / 10) % 10) {
		// upstream MT.java:354
		case 0: aNameOreDict += "nil"    ; aSymbol += "n"; break;
		// upstream MT.java:355
		case 1: aNameOreDict += "un"     ; aSymbol += "u"; break;
		// upstream MT.java:356
		case 2: aNameOreDict += "bi"     ; aSymbol += "b"; break;
		// upstream MT.java:357
		case 3: aNameOreDict += "tri"    ; aSymbol += "t"; break;
		// upstream MT.java:358
		case 4: aNameOreDict += "quad"   ; aSymbol += "q"; break;
		// upstream MT.java:359
		case 5: aNameOreDict += "pent"   ; aSymbol += "p"; break;
		// upstream MT.java:360
		case 6: aNameOreDict += "hex"    ; aSymbol += "h"; break;
		// upstream MT.java:361
		case 7: aNameOreDict += "sept"   ; aSymbol += "s"; break;
		// upstream MT.java:362
		case 8: aNameOreDict += "oct"    ; aSymbol += "o"; break;
		// upstream MT.java:363
		case 9: aNameOreDict += "enn"    ; aSymbol += "e"; break;
		// upstream MT.java:364
		}
		// upstream MT.java:365
		switch (aProtonsAndElectrons % 10) {
		// upstream MT.java:366
		case 0: aNameOreDict += "nilium" ; aSymbol += "n"; break;
		// upstream MT.java:367
		case 1: aNameOreDict += "unium"  ; aSymbol += "u"; break;
		// upstream MT.java:368
		case 2: aNameOreDict += "bium"   ; aSymbol += "b"; break;
		// upstream MT.java:369
		case 3: aNameOreDict += "trium"  ; aSymbol += "t"; break;
		// upstream MT.java:370
		case 4: aNameOreDict += "quadium"; aSymbol += "q"; break;
		// upstream MT.java:371
		case 5: aNameOreDict += "pentium"; aSymbol += "p"; break;
		// upstream MT.java:372
		case 6: aNameOreDict += "hexium" ; aSymbol += "h"; break;
		// upstream MT.java:373
		case 7: aNameOreDict += "septium"; aSymbol += "s"; break;
		// upstream MT.java:374
		case 8: aNameOreDict += "octium" ; aSymbol += "o"; break;
		// upstream MT.java:375
		case 9: aNameOreDict += "ennium" ; aSymbol += "e"; break;
		// upstream MT.java:376
		}
		// upstream MT.java:377
		return element(aID, aNameOreDict, aSymbol, aProtonsAndElectrons, aNeutrons, 1000, 3000, 0, SET_RAD).put(aRandomData).hide();
		// upstream MT.java:378
	}
		// upstream MT.java:380
	static OreDictMaterial hydrogen       () {return diatomicgas (  10, "Hydrogen"       , "H"     ,   1,   0,    14,    20,  0.00008988, SET_DULL    ,   0,   0, 255,  15, UUM                         , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:381
	static OreDictMaterial deuterium      () {return diatomicgas (  11, "Deuterium"      , "D"     ,   1,   1,    14,    20,  0.00008988, SET_RAD     , 255, 255,   0,  15,      FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:382
	static OreDictMaterial tritium        () {return diatomicgas (  12, "Tritium"        , "T"     ,   1,   2,    14,    20,  0.00008988, SET_RAD     , 255,   0,   0,  15,      FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:383
	static OreDictMaterial helium         () {return noblegas    (  20, "Helium"         , "He"    ,   2,   2,     1,     4,  0.0001785 , SET_SHINY   , 255, 255, 120     , UUM, FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:384
	static OreDictMaterial helium3        () {return noblegas    (  21, "Helium-3"       , "He-3"  ,   2,   1,     1,     4,  0.0001785 , SET_RAD     , 255, 255, 140     ,      FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:385
	static OreDictMaterial lithium        () {return alkali      (  30, "Lithium"        , "Li"    ,   3,   4,   453,  1560,  0.534     , SET_ROUGH   , 225, 220, 255     , UUM, FUSION                                       , TICKS_PER_SMELT*10, WASHING_MERCURY                                                    );}
		// upstream MT.java:386
	static OreDictMaterial lithium6       () {return alkali      (  31, "Lithium-6"      , "Li-6"  ,   3,   3,   453,  1560,  0.534     , SET_RAD     , 230, 225, 255     ,      FUSION                                       , TICKS_PER_SMELT*10, WASHING_MERCURY                                                    );}
		// upstream MT.java:387
	static OreDictMaterial beryllium      () {return alkaline    (  40, "Beryllium"      , "Be"    ,   4,   5,  1560,  2742,  1.85      , SET_METALLIC, 100, 180, 100     , UUM, FUSION                 , G_INGOT_ORES        , MORTAR                                                                                 );}
		// upstream MT.java:388
	static OreDictMaterial beryllium7     () {return alkaline    (  41, "Beryllium-7"    , "Be-7"  ,   4,   3,  1560,  2742,  1.85      , SET_RAD     , 110, 190, 110     ,      FUSION                 , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:389
	static OreDictMaterial beryllium8     () {return alkaline    (  42, "Beryllium-8"    , "Be-8"  ,   4,   4,  1560,  2742,  1.85      , SET_RAD     , 110, 200, 110     ,      FUSION                 , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:390
	static OreDictMaterial boron          () {return metalloid   (  50, "Boron"          , "B"     ,   5,   5,  2349,  4200,  2.34      , SET_DULL    , 250, 250, 250, 255, UUM, FUSION, ICOSAGEN       , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:391
	static OreDictMaterial boron11        () {return metalloid   (  51, "Boron-11"       , "B-11"  ,   5,   6,  2349,  4200,  2.34      , SET_RAD     , 240, 240, 240, 255,      FUSION, ICOSAGEN       , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:392
	static OreDictMaterial carbon         () {return polyatomic  (  60, "Carbon"         , "C"     ,   6,   6,  3800,  4300,  2.267     , SET_FINE    ,  20,  20,  20, 255, UUM, FUSION, CRYSTALLOGEN   , G_DUST_ORES         , TICKS_PER_SMELT* 4, FLAMMABLE, MELTING, MOLTEN, MORTAR, STICKS                         );}
		// upstream MT.java:393
	static OreDictMaterial carbon13       () {return polyatomic  (  61, "Carbon-13"      , "C-13"  ,   6,   7,  3800,  4300,  2.267     , SET_RAD     ,  25,  25,  25, 255,      FUSION, CRYSTALLOGEN   , G_DUST_ORES         , TICKS_PER_SMELT* 4, FLAMMABLE, MELTING, MOLTEN                                         );}
		// upstream MT.java:394
	static OreDictMaterial carbon14       () {return polyatomic  (  62, "Carbon-14"      , "C-14"  ,   6,   8,  3800,  4300,  2.267     , SET_RAD     ,  30,  30,  30, 255,      FUSION, CRYSTALLOGEN   , G_DUST_ORES         , TICKS_PER_SMELT* 4, FLAMMABLE, MELTING, MOLTEN                                         );}
		// upstream MT.java:395
	static OreDictMaterial nitrogen       () {return diatomicgas (  70, "Nitrogen"       , "N"     ,   7,   7,    63,    77,  0.0012506 , SET_DULL    ,   0, 150, 200,  15, UUM, FUSION, PNICTOGEN      , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:396
	static OreDictMaterial oxygen         () {return diatomicgas (  80, "Oxygen"         , "O"     ,   8,   8,    54,    90,  0.001429  , SET_DULL    ,   0, 100, 200,  15, UUM, FUSION, CHALCOGEN      , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:397
	static OreDictMaterial fluorine       () {return diatomicgas (  90, "Fluorine"       , "F"     ,   9,   9,    53,    85,  0.001696  , SET_DULL    ,  64, 192,   0, 255, UUM, FUSION, HALOGEN        , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:398
	static OreDictMaterial neon           () {return noblegas    ( 100, "Neon"           , "Ne"    ,  10,  10,    24,    27,  0.0008999 , SET_SHINY   , 250, 180, 180     , UUM, FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:399
	static OreDictMaterial sodium         () {return alkali      ( 110, "Sodium"         , "Na"    ,  11,  11,   370,  1156,  0.971     , SET_ROUGH   ,   0,   0, 150     , UUM, FUSION                                       , TICKS_PER_SMELT*20, "Natrium"                                                          );}
		// upstream MT.java:400
	static OreDictMaterial magnesium      () {return alkaline    ( 120, "Magnesium"      , "Mg"    ,  12,  12,   923,  1363,  1.738     , SET_COPPER  , 255, 200, 200     , UUM, FUSION                                       , TICKS_PER_SMELT* 4, FLAMMABLE                                                          );}
		// upstream MT.java:401
	static OreDictMaterial aluminium      () {return posttrans   ( 130, "Aluminium"      , "Al"    ,  13,  13,   933,  2792,  2.698     , SET_COPPER  , 128, 200, 240     , UUM, FUSION, ICOSAGEN       , G_INGOT_MACHINE_ORES, RAILS, MOLTEN, SOFT, "Aluminum"                                                        );}
		// upstream MT.java:402
	static OreDictMaterial silicon        () {return metalloid   ( 140, "Silicon"        , "Si"    ,  14,  14,  1687,  3538,  2.3296    , SET_COPPER  ,  60,  60,  80, 255, UUM, FUSION, CRYSTALLOGEN                                                                                                                  );}
		// upstream MT.java:403
	static OreDictMaterial phosphor       () {return polyatomic  ( 150, "Phosphor"       , "P"     ,  15,  15,   317,   550,  1.82      , SET_FINE    , 255, 255,   0, 255, UUM, FUSION, PNICTOGEN      , G_CRYSTAL_ORES      , TICKS_PER_SMELT* 5, FLAMMABLE, EXPLOSIVE, BRITTLE, MORTAR                              );}
		// upstream MT.java:404
	static OreDictMaterial sulfur         () {return polyatomic  ( 160, "Sulfur"         , "S"     ,  16,  16,   388,   717,  2.067     , SET_FINE    , 234, 234,   0, 255, UUM, FUSION, CHALCOGEN      , G_CRYSTAL_ORES      , TICKS_PER_SMELT* 5, FLAMMABLE, MELTING, MOLTEN, BRITTLE, MORTAR, "Sulphur"             );}
		// upstream MT.java:405
	static OreDictMaterial chlorine       () {return diatomicgas ( 170, "Chlorine"       , "Cl"    ,  17,  18,   171,   239,  0.003214  , SET_DULL    ,   0, 240, 255, 255, UUM, FUSION, HALOGEN        , CONTAINERS_FLUID                                                                                             );}
		// upstream MT.java:406
	static OreDictMaterial argon          () {return noblegas    ( 180, "Argon"          , "Ar"    ,  18,  22,    83,    87,  0.0017837 , SET_SHINY   ,   0, 255,   0     , UUM, FUSION                 , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:407
	static OreDictMaterial potassium      () {return alkali      ( 190, "Potassium"      , "K"     ,  19,  20,   336,  1032,  0.862     , SET_ROUGH   , 250, 250, 250     , UUM, FUSION                                       , "Kalium"                                                                               );}
		// upstream MT.java:408
	static OreDictMaterial calcium        () {return alkaline    ( 200, "Calcium"        , "Ca"    ,  20,  20,  1115,  1757,  1.54      , SET_METALLIC, 255, 245, 245     , UUM, FUSION                                                                                                                                );}
		// upstream MT.java:409
	static OreDictMaterial scandium       () {return transmetal  ( 210, "Scandium"       , "Sc"    ,  21,  24,  1814,  3109,  2.989     , SET_METALLIC                    , UUM, FUSION, SCANDIUM_GROUP                                                                                                                );}
		// upstream MT.java:410
	static OreDictMaterial titanium       () {return refractmetal( 220, "Titanium"       , "Ti"    ,  22,  26,  1941,  3560,  4.54      , SET_METALLIC, 220, 160, 240, 255, UUM, FUSION, TITANIUM_GROUP , G_INGOT_MACHINE_ORES, RAILS, MOLTEN, NEVER_FURNACE, "Titan"                                                  );}
		// upstream MT.java:411
	static OreDictMaterial vanadium       () {return refractmetal( 230, "Vanadium"       , "V"     ,  23,  28,  2183,  3680,  6.11      , SET_METALLIC,  50,  50,  50, 255, UUM, FUSION, VANADIUM_GROUP                       , MOLTEN                                                                                 );}
		// upstream MT.java:412
	static OreDictMaterial chromium       () {return refractmetal( 240, "Chromium"       , "Cr"    ,  24,  28,  2180,  2944,  7.15      , SET_SHINY   , 255, 230, 230, 255, UUM, FUSION, CHROMIUM_GROUP , G_INGOT_MACHINE_ORES, MOLTEN, "Chrome"                                                                       );}
		// upstream MT.java:413
	static OreDictMaterial manganese      () {return transmetal  ( 250, "Manganese"      , "Mn"    ,  25,  30,  1519,  2334,  7.44      , SET_DULL    , 250, 250, 250, 255, UUM, FUSION, MANGANESE_GROUP                      , MAGNETIC_PASSIVE, MOLTEN                                                               );}
		// upstream MT.java:414
	static OreDictMaterial iron           () {return transmetal  ( 260, "Iron"           , "Fe"    ,  26,  30,  1811,  3134,  7.874     , SET_METALLIC, 200, 200, 200, 255, UUM, FUSION, IRON_GROUP     , G_INGOT_MACHINE_ORES, MOLTEN, RAILS, MORTAR, MAGNETIC_PASSIVE, NEVER_FURNACE                                 );}
		// upstream MT.java:415
	static OreDictMaterial cobalt         () {return transmetal  ( 270, "Cobalt"         , "Co"    ,  27,  32,  1768,  3200,  8.86      , SET_METALLIC,  80,  80, 250, 255, UUM        , COBALT_GROUP                         , WASHING_PERSULFATE, MORTAR, MAGNETIC_PASSIVE, MOLTEN                                   );}
		// upstream MT.java:416
	static OreDictMaterial cobalt60       () {return transmetal  ( 278, "Cobalt-60"      , "Co-60" ,  27,  33,  1768,  3200,  8.86      , SET_RAD     ,  90,  90, 250, 255             , COBALT_GROUP                         , WASHING_PERSULFATE, MORTAR, MAGNETIC_PASSIVE                                   , "Co60");}
		// upstream MT.java:417
	static OreDictMaterial nickel         () {return transmetal  ( 280, "Nickel"         , "Ni"    ,  28,  30,  1728,  3186,  8.912     , SET_METALLIC, 250, 250, 200, 255, UUM        , NICKEL_GROUP                         , WASHING_PERSULFATE, MORTAR, MAGNETIC_PASSIVE, MOLTEN                                   );}
		// upstream MT.java:418
	static OreDictMaterial copper         () {return noblemetal  ( 290, "Copper"         , "Cu"    ,  29,  34,  1357,  2835,  8.96      , SET_COPPER  , 255, 130,  90     , UUM        , COPPER_GROUP   , G_INGOT_MACHINE_ORES, WASHING_PERSULFATE, SOFT, FURNACE, EXTRUDER_SIMPLE, MORTAR, MOLTEN, RAILS              );}
		// upstream MT.java:419
	static OreDictMaterial zinc           () {return transmetal  ( 300, "Zinc"           , "Zn"    ,  30,  35,   692,  1180,  7.134     , SET_COPPER  , 250, 240, 240, 255, UUM        , ZINC_GROUP                           , WASHING_PERSULFATE, SOFT, WASHING_MERCURY, MOLTEN                                      );}
		// upstream MT.java:420
	static OreDictMaterial gallium        () {return posttrans   ( 310, "Gallium"        , "Ga"    ,  31,  39,   302,  2477,  5.907     , SET_COPPER  , 220, 220, 255     , UUM        , ICOSAGEN                             , BRITTLE                                                                                );}
		// upstream MT.java:421
	static OreDictMaterial germanium      () {return metalloid   ( 320, "Germanium"      , "Ge"    ,  32,  40,  1211,  3106,  5.323     , SET_COPPER  , 212, 212, 212, 255, UUM        , CRYSTALLOGEN   , G_INGOT_MACHINE_ORES, FURNACE, EXTRUDER_SIMPLE, MORTAR, "Osmium"                                             );}
		// upstream MT.java:422
	static OreDictMaterial arsenic        () {return metalloid   ( 330, "Arsenic"        , "As"    ,  33,  42,   887,  1090,  5.776     , SET_SHINY   , 103, 103,  86, 255, UUM        , PNICTOGEN                                                                                                                     );}
		// upstream MT.java:423
	static OreDictMaterial selenium       () {return polyatomic  ( 340, "Selenium"       , "Se"    ,  34,  45,   453,   958,  4.809     , SET_DULL    , 111,  20,  20, 255, UUM        , CHALCOGEN      , G_CRYSTAL_ORES      , BRITTLE                                                                                );}
		// upstream MT.java:424
	static OreDictMaterial bromine        () {return diatomic    ( 350, "Bromine"        , "Br"    ,  35,  45,   265,   332,  3.122     , SET_FLUID   ,  80,  10,  10, 255, UUM        , HALOGEN        , CONTAINERS_FLUID    , LIQUID, MELTING                                                                        );}
		// upstream MT.java:425
	static OreDictMaterial krypton        () {return noblegas    ( 360, "Krypton"        , "Kr"    ,  36,  48,   115,   119,  0.003733  , SET_DIAMOND , 128, 255, 128     , UUM                         , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:426
	static OreDictMaterial rubidium       () {return alkali      ( 370, "Rubidium"       , "Rb"    ,  37,  48,   312,   961,  1.532     , SET_SHINY   , 240,  30,  30     , UUM                                                                                                                                        );}
		// upstream MT.java:427
	static OreDictMaterial strontium      () {return alkaline    ( 380, "Strontium"      , "Sr"    ,  38,  49,  1050,  1655,  2.64      , SET_METALLIC, 200, 200, 200     , UUM                                                                                                                                        );}
		// upstream MT.java:428
	static OreDictMaterial yttrium        () {return transmetal  ( 390, "Yttrium"        , "Y"     ,  39,  50,  1799,  3609,  4.469     , SET_METALLIC, 220, 250, 220, 255, UUM        , SCANDIUM_GROUP                                                                                                                );}
		// upstream MT.java:429
	static OreDictMaterial zirconium      () {return refractmetal( 400, "Zirconium"      , "Zr"    ,  40,  51,  2128,  4682,  6.506     , SET_DIAMOND , 200, 255, 255, 127, UUM        , TITANIUM_GROUP , G_GEM_ORES_TRANSPARENT, CRYSTAL, VALUABLE                                                                    );}
		// upstream MT.java:430
	static OreDictMaterial niobium        () {return refractmetal( 410, "Niobium"        , "Nb"    ,  41,  53,  2750,  5017,  8.57      , SET_METALLIC, 190, 180, 200, 255, UUM        , VANADIUM_GROUP                       , "Columbium"                                                                            );}
		// upstream MT.java:431
	static OreDictMaterial molybdenum     () {return refractmetal( 420, "Molybdenum"     , "Mo"    ,  42,  53,  2896,  4912, 10.22      , SET_COPPER  , 180, 180, 220, 255, UUM        , CHROMIUM_GROUP                       , MOLTEN                                                                                 );}
		// upstream MT.java:432
	static OreDictMaterial technetium     () {return transmetal  ( 430, "Technetium"     , "Tc"    ,  43,  55,  2430,  4538, 11.5       , SET_RAD     ,  66,  66,  99, 255             , MANGANESE_GROUP                      , "Gregorium"                                                                            );}
		// upstream MT.java:433
	static OreDictMaterial ruthenium      () {return platingroup ( 440, "Ruthenium"      , "Ru"    ,  44,  57,  2607,  4423, 12.37      , SET_SHINY   , 155, 155, 155     , UUM        , IRON_GROUP                                                                                                                    );}
		// upstream MT.java:434
	static OreDictMaterial rhodium        () {return platingroup ( 450, "Rhodium"        , "Rh"    ,  45,  58,  2237,  3968, 12.41      , SET_SHINY   , 144, 144, 144     , UUM        , COBALT_GROUP                                                                                                                  );}
		// upstream MT.java:435
	static OreDictMaterial palladium      () {return platingroup ( 460, "Palladium"      , "Pd"    ,  46,  60,  1828,  3236, 12.02      , SET_SHINY   , 128, 128, 128     , UUM        , NICKEL_GROUP   , G_INGOT_MACHINE_ORES                                                                                         );}
		// upstream MT.java:436
	static OreDictMaterial silver         () {return noblemetal  ( 470, "Silver"         , "Ag"    ,  47,  60,  1234,  2435, 10.501     , SET_SHINY   , 220, 220, 255     , UUM        , COPPER_GROUP   , G_INGOT_MACHINE_ORES, RAILS, WASHING_MERCURY, MORTAR, MOLTEN, VALUABLE, SOFT, ENDER_DRAGON_PROOF             );}
		// upstream MT.java:437
	static OreDictMaterial cadmium        () {return transmetal  ( 480, "Cadmium"        , "Cd"    ,  48,  64,   594,  1040,  8.69      , SET_SHINY   ,  50,  50,  60, 255, UUM        , ZINC_GROUP                                                                                                                    );}
		// upstream MT.java:438
	static OreDictMaterial indium         () {return posttrans   ( 490, "Indium"         , "In"    ,  49,  65,   429,  2345,  7.31      , SET_SHINY   ,  64,   0, 128     , UUM        , ICOSAGEN                                                                                                                      );}
		// upstream MT.java:439
	static OreDictMaterial tin            () {return posttrans   ( 500, "Tin"            , "Sn"    ,  50,  68,   505,  2875,  7.287     , SET_COPPER  , 220, 220, 220     , UUM        , CRYSTALLOGEN   , G_INGOT_MACHINE_ORES, SOFT, SOLDERING_MATERIAL, MOLTEN                                                       );}
		// upstream MT.java:440
	static OreDictMaterial antimony       () {return metalloid   ( 510, "Antimony"       , "Sb"    ,  51,  70,   903,  1860,  6.685     , SET_COPPER  , 220, 220, 240, 255, UUM        , PNICTOGEN      , G_INGOT_MACHINE_ORES, SOFT                                                                                   );}
		// upstream MT.java:441
	static OreDictMaterial tellurium      () {return metalloid   ( 520, "Tellurium"      , "Te"    ,  52,  75,   722,  1261,  6.232     , SET_SHINY                       , UUM        , CHALCOGEN                                                                                                                     );}
		// upstream MT.java:442
	static OreDictMaterial iodine         () {return diatomic    ( 530, "Iodine"         , "I"     ,  53,  74,   386,   457,  4.93      , SET_DULL    , 255, 240, 240, 255, UUM        , HALOGEN        , G_CRYSTAL_ORES                                                                                               );}
		// upstream MT.java:443
	static OreDictMaterial xenon          () {return noblegas    ( 540, "Xenon"          , "Xe"    ,  54,  77,   161,   165,  0.005887  , SET_DULL    ,   0, 255, 255     , UUM                         , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:444
	static OreDictMaterial caesium        () {return alkali      ( 550, "Caesium"        , "Cs"    ,  55,  77,   301,   944,  1.873     , SET_SHINY   , 128,  98,  11     , UUM                                                                                                                                        );}
		// upstream MT.java:445
	static OreDictMaterial barium         () {return alkaline    ( 560, "Barium"         , "Ba"    ,  56,  81,  1000,  2170,  3.594     , SET_METALLIC, 131, 130,  76     , UUM                                                                                                                                        );}
		// upstream MT.java:446
	static OreDictMaterial lanthanium     () {return lanthanide  ( 570, "Lanthanium"     , "La"    ,  57,  81,  1193,  3737,  6.145     , SET_METALLIC,  93, 117, 117     , UUM                                               , "Lantanium", "Lantanum", "Lanthanum"                                                   );}
		// upstream MT.java:447
	static OreDictMaterial cerium         () {return lanthanide  ( 580, "Cerium"         , "Ce"    ,  58,  82,  1068,  3716,  6.77      , SET_SHINY   , 255, 255, 190     , UUM                                                                                                                                        );}
		// upstream MT.java:448
	static OreDictMaterial praseodymium   () {return lanthanide  ( 590, "Praseodymium"   , "Pr"    ,  59,  81,  1208,  3793,  6.773     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:449
	static OreDictMaterial neodymium      () {return lanthanide  ( 600, "Neodymium"      , "Nd"    ,  60,  84,  1297,  3347,  7.007     , SET_SHINY   , 100, 100, 100     , UUM                                               , MAGNETIC_PASSIVE, MOLTEN                                                               );}
		// upstream MT.java:450
	static OreDictMaterial promethium     () {return lanthanide  ( 610, "Promethium"     , "Pm"    ,  61,  83,  1315,  3273,  7.26      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:451
	static OreDictMaterial samarium       () {return lanthanide  ( 620, "Samarium"       , "Sm"    ,  62,  88,  1345,  2067,  7.52      , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:452
	static OreDictMaterial europium       () {return lanthanide  ( 630, "Europium"       , "Eu"    ,  63,  88,  1099,  1802,  5.243     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:453
	static OreDictMaterial gadolinium     () {return lanthanide  ( 640, "Gadolinium"     , "Gd"    ,  64,  93,  1585,  3546,  7.895     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:454
	static OreDictMaterial terbium        () {return lanthanide  ( 650, "Terbium"        , "Tb"    ,  65,  93,  1629,  3503,  8.229     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:455
	static OreDictMaterial dysprosium     () {return lanthanide  ( 660, "Dysprosium"     , "Dy"    ,  66,  96,  1680,  2840,  8.55      , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:456
	static OreDictMaterial holmium        () {return lanthanide  ( 670, "Holmium"        , "Ho"    ,  67,  97,  1734,  2993,  8.795     , SET_METALLIC, 196, 150, 159     , UUM                                               , MAGNETIC_ACTIVE                                                                        );}
		// upstream MT.java:457
	static OreDictMaterial erbium         () {return lanthanide  ( 680, "Erbium"         , "Er"    ,  68,  99,  1802,  3141,  9.066     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:458
	static OreDictMaterial thulium        () {return lanthanide  ( 690, "Thulium"        , "Tm"    ,  69,  99,  1818,  2223,  9.321     , SET_METALLIC                    , UUM                                                                                                                                        );}
		// upstream MT.java:459
	static OreDictMaterial ytterbium      () {return lanthanide  ( 700, "Ytterbium"      , "Yb"    ,  70, 103,  1097,  1469,  6.965     , SET_METALLIC, 167, 167, 167     , UUM                                                                                                                                        );}
		// upstream MT.java:460
	static OreDictMaterial lutetium       () {return lanthanide  ( 710, "Lutetium"       , "Lu"    ,  71, 103,  1925,  3675,  9.84      , SET_METALLIC                    , UUM        , SCANDIUM_GROUP                                                                                                                );}
		// upstream MT.java:461
	static OreDictMaterial hafnium        () {return refractmetal( 720, "Hafnium"        , "Hf"    ,  72, 106,  2506,  4876, 13.31      , SET_COPPER  , 140, 140, 150, 255, UUM        , TITANIUM_GROUP                       , FLAMMABLE, EXPLOSIVE                                                                   );}
		// upstream MT.java:462
	static OreDictMaterial tantalum       () {return refractmetal( 730, "Tantalum"       , "Ta"    ,  73, 107,  3290,  5731, 16.654     , SET_COPPER  , 120, 120, 140, 255, UUM        , VANADIUM_GROUP                       , "Tantalium"                                                                            );}
		// upstream MT.java:463
	static OreDictMaterial tungsten       () {return refractmetal( 740, "Tungsten"       , "W"     ,  74, 109,  3695,  5828, 19.25      , SET_METALLIC,  50,  50,  50, 255, UUM        , CHROMIUM_GROUP , G_INGOT_MACHINE_ORES, MOLTEN, RAILS, UNBURNABLE, NEVER_FURNACE, "Wolframium", "Wolfram", "ElnTungsten"       );}
		// upstream MT.java:464
	static OreDictMaterial rhenium        () {return precmetal   ( 750, "Rhenium"        , "Re"    ,  75, 111,  3459,  5869, 21.02      , SET_SHINY   , 255, 255, 200     , UUM        , MANGANESE_GROUP                                                                                                               );}
		// upstream MT.java:465
	static OreDictMaterial osmium         () {return platingroup ( 760, "OsmiumElemental", "Os"    ,  76, 114,  3306,  5285, 22.61      , SET_METALLIC,  50,  50, 255     , UUM        , IRON_GROUP     , G_INGOT_MACHINE_ORES, MOLTEN, VALUABLE, RAILS                                                                );}
		// upstream MT.java:466
	static OreDictMaterial iridium        () {return platingroup ( 770, "Iridium"        , "Ir"    ,  77, 115,  2719,  4701, 22.56      , SET_DULL    , 240, 240, 245     , UUM        , COBALT_GROUP   , G_INGOT_MACHINE_ORES, MOLTEN, VALUABLE                                                                       );}
		// upstream MT.java:467
	static OreDictMaterial platinum       () {return platingroup ( 780, "Platinum"       , "Pt"    ,  78, 117,  2041,  4098, 21.46      , SET_SHINY   , 100, 180, 250     , UUM        , NICKEL_GROUP   , G_INGOT_MACHINE_ORES, WASHING_MERCURY, MOLTEN, VALUABLE, RAILS, MORTAR                                       );}
		// upstream MT.java:468
	static OreDictMaterial gold           () {return noblemetal  ( 790, "Gold"           , "Au"    ,  79, 117,  1337,  3129, 19.282     , SET_SHINY   , 255, 230,  80     , UUM        , COPPER_GROUP   , G_INGOT_MACHINE_ORES, WASHING_MERCURY, MOLTEN, VALUABLE, SOFT, RAILS, MORTAR, WITHER_PROOF                   );}
		// upstream MT.java:469
	static OreDictMaterial gold198        () {return noblemetal  ( 791, "Gold-198"       , "Au-198",  79, 119,  1337,  3129, 19.282     , SET_SHINY   , 255, 230,  80                  , COPPER_GROUP   , G_INGOT_ORES        , WASHING_MERCURY, MOLTEN, VALUABLE, SOFT, WITHER_PROOF, "Gol198"               , "Au198");}
		// upstream MT.java:470
	static OreDictMaterial mercury        () {return precmetal   ( 800, "Mercury"        , "Hg"    ,  80, 120,   234,   629, 13.5336    , SET_COPPER  , 230, 220, 220     , UUM        , ZINC_GROUP                           , "Quicksilver", "QuickSilver", PULVERIZING_CINNABAR                                     );}
		// upstream MT.java:471
	static OreDictMaterial thallium       () {return posttrans   ( 810, "Thallium"       , "Tl"    ,  81, 123,   577,  1746, 11.85      , SET_METALLIC                    , UUM        , ICOSAGEN                                                                                                                      );}
		// upstream MT.java:472
	static OreDictMaterial lead           () {return posttrans   ( 820, "Lead"           , "Pb"    ,  82, 125,   600,  2022, 11.342     , SET_DULL    ,  60,  40, 110     , UUM        , CRYSTALLOGEN   , G_INGOT_MACHINE_ORES, SOLDERING_MATERIAL, SOLDERING_MATERIAL_BAD, MOLTEN, SOFT                               );}
		// upstream MT.java:473
	static OreDictMaterial bismuth        () {return posttrans   ( 830, "Bismuth"        , "Bi"    ,  83, 125,   544,  1837,  9.807     , SET_COPPER  , 100, 160, 160                  , PNICTOGEN      , G_INGOT_MACHINE_ORES, MAGNETIC_PASSIVE, MOLTEN                                                               );}
		// upstream MT.java:474
	static OreDictMaterial polonium       () {return posttrans   ( 840, "Polonium"       , "Po"    ,  84, 124,   527,  1235,  9.32      , SET_RAD                                      , CHALCOGEN                                                                                                                     );}
		// upstream MT.java:475
	static OreDictMaterial astatine       () {return metalloid   ( 850, "Astatine"       , "At"    ,  85, 124,   575,   610,  7.0       , SET_RAD     ,  33,  33,  33, 255             , HALOGEN        , CONTAINERS_GAS      , GASES, "Astatine209"                                                          , "At209");}
		// upstream MT.java:476
	static OreDictMaterial radon          () {return noblegas    ( 860, "Radon"          , "Rn"    ,  86, 134,   202,   211,  0.00973   , SET_DULL    , 255,   0, 255                                   , CONTAINERS_GAS                                                                                               );}
		// upstream MT.java:477
	static OreDictMaterial francium       () {return alkali      ( 870, "Francium"       , "Fr"    ,  87, 134,   300,   950,  1.87      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:478
	static OreDictMaterial radium         () {return alkaline    ( 880, "Radium"         , "Ra"    ,  88, 138,   973,  2010,  5.5       , SET_RAD     , 255, 255, 205                                                         , "Radium226"                                                                            );}
		// upstream MT.java:479
	static OreDictMaterial actinium       () {return actinide    ( 890, "Actinium"       , "Ac"    ,  89, 136,  1323,  3471, 10.07      , SET_RAD     , 125, 113, 113                                                                                                                                                  );}
		// upstream MT.java:480
	static OreDictMaterial thorium        () {return actinide    ( 900, "Thorium"        , "Th"    ,  90, 142,  2115,  5061, 11.72      , SET_RAD     ,   0,  30,   0                                                         , "Thorium232"                                                                  , "Th232");}
		// upstream MT.java:481
	static OreDictMaterial protactinium   () {return actinide    ( 910, "Protactinium"   , "Pa"    ,  91, 138,  1841,  4300, 15.37      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:482
	static OreDictMaterial uranium        () {return actinide    ( 920, "Uranium"        , "U"     ,  92, 146,  1405,  4404, 18.95      , SET_RAD     ,  50, 240,  50                                                         , MELTING, MOLTEN, "Uranium238", "Uran"                                         , "U238" );}
		// upstream MT.java:483
	static OreDictMaterial uranium235     () {return actinide    ( 921, "Uranium-235"    , "U-235" ,  92, 143,  1405,  4404, 18.95      , SET_RAD     ,  70, 250,  70                                                         , MELTING, MOLTEN, "UraniumEnriched"                                            , "U235" );}
		// upstream MT.java:484
	static OreDictMaterial uranium233     () {return actinide    ( 922, "Uranium-233"    , "U-233" ,  92, 141,  1405,  4404, 18.95      , SET_RAD     ,  70, 250,  50                                                                                                                                         , "U233" );}
		// upstream MT.java:485
	static OreDictMaterial neptunium      () {return actinide    ( 930, "Neptunium"      , "Np"    ,  93, 144,   917,  4273, 20.45      , SET_RAD     ,  78,  90,  78                                                         , "Neptunium237"                                                                , "Np237");}
		// upstream MT.java:486
	static OreDictMaterial plutonium      () {return actinide    ( 940, "Plutonium"      , "Pu"    ,  94, 150,   912,  3501, 19.84      , SET_RAD     , 240,  50,  50                                                         , "Plutonium244"                                                                         );}
		// upstream MT.java:487
	static OreDictMaterial plutonium240   () {return actinide    ( 942, "Plutonium-240"  , "Pu-240",  94, 146,   912,  3501, 19.84      , SET_RAD     , 235,  30,  30                                                                                                                                         , "Pu240");}
		// upstream MT.java:488
	static OreDictMaterial plutonium241   () {return actinide    ( 943, "Plutonium-241"  , "Pu-241",  94, 147,   912,  3501, 19.84      , SET_RAD     , 245,  70,  70                                                                                                                                         , "Pu241");}
		// upstream MT.java:489
	static OreDictMaterial plutonium243   () {return actinide    ( 945, "Plutonium-243"  , "Pu-243",  94, 149,   912,  3501, 19.84      , SET_RAD     , 250,  70,  70                                                                                                                                                  );}
		// upstream MT.java:490
	static OreDictMaterial plutonium238   () {return actinide    ( 946, "Plutonium-238"  , "Pu-238",  94, 144,   912,  3501, 19.84      , SET_RAD     , 250,  30,  30                                                                                                                                         , "Pu238");}
		// upstream MT.java:491
	static OreDictMaterial plutonium239   () {return actinide    ( 947, "Plutonium-239"  , "Pu-239",  94, 145,   912,  3501, 19.84      , SET_RAD     , 235,  50,  50                                                                                                                                         , "Pu239");}
		// upstream MT.java:492
	static OreDictMaterial americium      () {return actinide    ( 950, "Americium"      , "Am"    ,  95, 150,  1449,  2880, 13.69      , SET_RAD     , 200, 200, 200                                                                                                                                                  );}
		// upstream MT.java:493
	static OreDictMaterial americium241   () {return actinide    ( 951, "Americium-241"  , "Am-241",  95, 146,  1449,  2880, 13.69      , SET_RAD     , 210, 210, 210                                                                                                                                         , "Am241");}
		// upstream MT.java:494
	static OreDictMaterial americium242   () {return actinide    ( 952, "Americium-242"  , "Am-242",  95, 147,  1449,  2880, 13.69      , SET_RAD     , 210, 210, 210                                                                                                                                         , "Am242");}
		// upstream MT.java:495
	static OreDictMaterial curium         () {return actinide    ( 960, "Curium"         , "Cm"    ,  96, 153,  1613,  3383, 13.51      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:496
	static OreDictMaterial berkelium      () {return actinide    ( 970, "Berkelium"      , "Bk"    ,  97, 152,  1259,  2900, 14.79      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:497
	static OreDictMaterial californium    () {return actinide    ( 980, "Californium"    , "Cf"    ,  98, 153,  1173,  1743, 15.1       , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:498
	static OreDictMaterial einsteinium    () {return actinide    ( 990, "Einsteinium"    , "Es"    ,  99, 153,  1133,  1269,  8.84      , SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:499
	static OreDictMaterial fermium        () {return actinide    (1000, "Fermium"        , "Fm"    , 100, 157,  1125, 3000 ,           0, SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:500
	static OreDictMaterial mendelevium    () {return actinide    (1010, "Mendelevium"    , "Md"    , 101, 157,  1100, 3000 ,           0, SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:501
	static OreDictMaterial nobelium       () {return actinide    (1020, "Nobelium"       , "No"    , 102, 157,  1100, 3000 ,           0, SET_RAD                                                                                                                                                                      );}
		// upstream MT.java:502
	static OreDictMaterial lawrencium     () {return actinide    (1030, "Lawrencium"     , "Lr"    , 103, 159,  1900, 3000 ,           0, SET_RAD                                      , SCANDIUM_GROUP                                                                                                                );}
		// upstream MT.java:503
	static OreDictMaterial rutherfordium  () {return transmetal  (1040, "Rutherfordium"  , "Rf"    , 104, 161,  2400,  5800, 23.2       , SET_RAD                                      , TITANIUM_GROUP                                                                                                                );}
		// upstream MT.java:504
	static OreDictMaterial dubnium        () {return transmetal  (1050, "Dubnium"        , "Db"    , 105, 163, 1000 , 3000 , 29.3       , SET_RAD                                      , VANADIUM_GROUP                                                                                                                );}
		// upstream MT.java:505
	static OreDictMaterial seaborgium     () {return transmetal  (1060, "Seaborgium"     , "Sg"    , 106, 165, 1000 , 3000 , 35.0       , SET_RAD                                      , CHROMIUM_GROUP                                                                                                                );}
		// upstream MT.java:506
	static OreDictMaterial bohrium        () {return transmetal  (1070, "Bohrium"        , "Bh"    , 107, 163, 1000 , 3000 , 37.1       , SET_RAD                                      , MANGANESE_GROUP                                                                                                               );}
		// upstream MT.java:507
	static OreDictMaterial hassium        () {return transmetal  (1080, "Hassium"        , "Hs"    , 108, 169, 1000 , 3000 , 40.7       , SET_RAD                                      , IRON_GROUP                                                                                                                    );}
		// upstream MT.java:508
	static OreDictMaterial meitnerium     () {return element     (1090, "Meitnerium"     , "Mt"    , 109, 167, 1000 , 3000 , 37.4       , SET_RAD                                      , COBALT_GROUP   , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:509
	static OreDictMaterial darmstadtium   () {return element     (1100, "Darmstadtium"   , "Ds"    , 110, 171, 1000 , 3000 , 34.8       , SET_RAD                                      , NICKEL_GROUP   , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:510
	static OreDictMaterial roentgenium    () {return element     (1110, "Roentgenium"    , "Rg"    , 111, 169, 1000 , 3000 , 28.7       , SET_RAD                                      , COPPER_GROUP   , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:511
	static OreDictMaterial copernicium    () {return transmetal  (1120, "Copernicium"    , "Cn"    , 112, 173,  150 ,   357, 23.7       , SET_RAD                                      , ZINC_GROUP                                                                                                                    );}
		// upstream MT.java:512
	static OreDictMaterial nihonium       () {return element     (1130, "Nihonium"       , "Nh"    , 113, 171,   700,  1400, 16.0       , SET_RAD                                      , ICOSAGEN       , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:513
	static OreDictMaterial flerovium      () {return posttrans   (1140, "Flerovium"      , "Fl"    , 114, 175,   340,   420, 14.0       , SET_RAD                                      , CRYSTALLOGEN                                                                                                                  );}
		// upstream MT.java:514
	static OreDictMaterial flerovium298   () {return posttrans   (1148, "Flerovium-298"  , "Fl-298", 114, 184,   340,   420, 14.0       , SET_RAD                                      , CRYSTALLOGEN                                                                                                                  );}
		// upstream MT.java:515
	static OreDictMaterial moscovium      () {return element     (1150, "Moscovium"      , "Mc"    , 115, 174,   700,  1400, 13.5       , SET_RAD                                      , PNICTOGEN      , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:516
	static OreDictMaterial livermorium    () {return element     (1160, "Livermorium"    , "Lv"    , 116, 177,   708,  1085, 12.9       , SET_RAD                                      , CHALCOGEN      , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:517
	static OreDictMaterial farnsium       () {return element     (1170, "Farnsium"       , "Fa"    , 117, 177,   673,   823,  7.2       , SET_RAD     ,  60,  70,  80, 255             , HALOGEN        , G_INGOT_ORES        , "Tennessine"                                                                           );}
		// upstream MT.java:518
	static OreDictMaterial oganesson      () {return element     (1180, "Oganesson"      , "Og"    , 118, 176,   258,   263,  5.0       , SET_RAD                                                       , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:519
	static OreDictMaterial ununennium     () {return element     (1190, "Ununennium"     , "Uue"   , 119, 178,   290,   903,           0, SET_RAD                                                       , G_INGOT_ORES                                                                                                 );}
		// upstream MT.java:520
	static OreDictMaterial unbinilium     () {return element     (1200, "Unbinilium"     , "Ubn"   , 120, 180,   953,  1973,           0, SET_RAD                                                       , G_INGOT_ORES                                                                                                 );}
	// ============================ material fields (upstream declaration order :523-1945, non-final: assigned by the reg batches below) ============================
	public static OreDictMaterial NULL, Empty, y, Photon, v, Neutrino, n, Neutron;
	public static OreDictMaterial p, Proton, e, Electron, Ma, Magic, H, D;
	public static OreDictMaterial H_2, T, H_3, He, He_3, Li, Li_6, Be;
	public static OreDictMaterial Be_7, Be_8, B, B_11, C, C_13, C_14, N;
	public static OreDictMaterial O, F, Ne, Na, Mg, Al, Si, P;
	public static OreDictMaterial S, Cl, Ar, K, Ca, Sc, Ti, V;
	public static OreDictMaterial Cr, Mn, Fe, Co, Co_60, Ni, Cu, Zn;
	public static OreDictMaterial Ga, Ge, As, Se, Br, Kr, Rb, Sr;
	public static OreDictMaterial Y, Zr, Nb, Mo, Tc, Gregorium, Ru, Rh;
	public static OreDictMaterial Pd, Ag, Cd, In, Sn, Sb, Te, I;
	public static OreDictMaterial Xe, Cs, Ba, La, Ce, Pr, Nd, Pm;
	public static OreDictMaterial Sm, Eu, Gd, Tb, Dy, Ho, Er, Tm;
	public static OreDictMaterial Yb, Lu, Hf, Ta, W, Re, Os, Ir;
	public static OreDictMaterial Pt, Au, Au_198, Hg, Tl, Pb, Bi, Po;
	public static OreDictMaterial At, Rn, Fr, Ra, Ac, Th, Pa, U_238;
	public static OreDictMaterial U_235, U_233, Np, Pu, Pu_240, Pu_241, Pu_243, Pu_238;
	public static OreDictMaterial Pu_239, Am, Am_241, Am_242, Cm, Bk, Cf, Es;
	public static OreDictMaterial Fm, Md, No, Lr, Rf, Db, Sg, Bh;
	public static OreDictMaterial Hs, Mt, Ds, Rg, Cn, Nh, Fl, Fl_298;
	public static OreDictMaterial Mc, Lv, Fa, Ts, Og, Uue, Ubn, Ubu;
	public static OreDictMaterial Ubb, Ubt, Ubq, Tn, Ubp, Ke, Ubh, Ubs;
	public static OreDictMaterial Ubo, Ube, Utn, Utu, Utb, Utt, Utq, Utp;
	public static OreDictMaterial Uth, Uts, Uto, Ute, Uqn, Uqu, Uqb, Uqt;
	public static OreDictMaterial Uqq, Dn, Uqp, Uqh, Uqs, Uqo, Uqe, Upn;
	public static OreDictMaterial Upu, Vb, Upb, Upt, Upq, Upp, Uph, Ups;
	public static OreDictMaterial Upo, Upe, Uhn, Uhu, Uhb, Uht, Uhq, Uhp;
	public static OreDictMaterial Uhh, Uhs, Uho, Uhe, Usn, Usu, Usb, Ust;
	public static OreDictMaterial Nq, Usq, Nq_528, Nq_522, Usp, Ush, Uss, Uso;
	public static OreDictMaterial Use, Uon, Uou, Uob, Uot, Uoq, An, Uop;
	public static OreDictMaterial Cor, Uoh, Dr, Uos, Etx, Uoo, Uoe, Uen;
	public static OreDictMaterial Ueu, Ueb, Uet, Ueq, Uep, Ueh, Ues, Ueo;
	public static OreDictMaterial Uee, Bnn, Bnu, Bnb, Bnt, Bnq, Bnp, Bnh;
	public static OreDictMaterial Bns, Bno, Bne, Bun, Buu, Bub, But, Buq;
	public static OreDictMaterial Bup, Buh, Bus, Buo, Bue, Bbn, Atl, Bbu;
	public static OreDictMaterial Atlarus, Ad, Bbb, Bbt, Bbq, Bbp, Bbh, Bbs;
	public static OreDictMaterial Bbo, Bbe, Btn, Btu, Btb, Btt, Btq, Btp;
	public static OreDictMaterial Bth, Bts, Bto, Mcg, Bte, Bqn, Bqu, Bqb;
	public static OreDictMaterial Bqt, Bqq, Bqp, Bqh, Bqs, Bqo, Bqe, Bpn;
	public static OreDictMaterial Bpu, Bpb, Bpt, Bpq, Bpp, Bph, Bps, Bpo;
	public static OreDictMaterial Bpe, Bhn, Bhu, Bhb, Bht, Bhq, Bhp, Bhh;
	public static OreDictMaterial Bhs, Bho, Bhe, Bsn, Bsu, Bsb, Bst, Bsq;
	public static OreDictMaterial Bsp, Bsh, Bss, Bso, Bse, Bon, Bou, Bob;
	public static OreDictMaterial Bot, Boq, Bop, Boh, Bos, Boo, Boe, Ben;
	public static OreDictMaterial Beu, Beb, Bet, Beq, Bep, Beh, Bes, Beo;
	public static OreDictMaterial Bee, Tnn, Tnu, Tnb, Tnt, Tnq, Tnp, Tnh;
	public static OreDictMaterial Tns, Tno, Tne, Tun, Tuu, Tub, Tut, Tuq;
	public static OreDictMaterial Tup, Tuh, Tus, Tuo, Tue, Tbn, Tbu, Tbb;
	public static OreDictMaterial Tbt, Tbq, Tbp, Tbh, Tbs, Tbo, Tbe, Ttn;
	public static OreDictMaterial Ttu, Ttb, Ttt, Ttq, Ttp, Tth, Tts, Tto;
	public static OreDictMaterial Tte, Tqn, Tqu, Tqb, Tqt, Tqq, Tqp, Tqh;
	public static OreDictMaterial Tqs, Tqo, Tqe, Tpn, Tpu, Tpb, Tpt, Tpq;
	public static OreDictMaterial Tpp, Tph, Tps, Tpo, Tpe, Thn, Thu, Thb;
	public static OreDictMaterial Tht, Thq, Thp, Thh, Ths, Tho, The, Tsn;
	public static OreDictMaterial Tsu, Gt, Tsb, Tst, Tsq, Tsp, Tsh, Tss;
	public static OreDictMaterial Tso, Tse, Ton, Tou, Tob, Tot, Toq, Top;
	public static OreDictMaterial Toh, Tos, Too, Toe, Ten, Teu, Teb, Tet;
	public static OreDictMaterial Teq, Tep, Teh, Tes, Teo, Tee, Neutronium, Primitive;
	public static OreDictMaterial Basic, Good, Advanced, Data, Elite, Master, Ultimate, Quantum;
	public static OreDictMaterial Superconductor, Infinite, Black, Red, Green, Brown, Blue, Purple;
	public static OreDictMaterial Cyan, LightGray, Gray, Pink, Lime, Yellow, LightBlue, Magenta;
	public static OreDictMaterial Orange, White, H2O, Water, HDO, D2O, T2O, Steam;
	public static OreDictMaterial Snow, Ice, FreshWater, HolyWater, SeaWater, DirtyWater, DistWater, H2O2;
	public static OreDictMaterial HCl, HF, HeNe, HeliumNeon, Air, NO, NO2, NH3;
	public static OreDictMaterial HNO3, NitricAcid, CO, CO2, CO3, CH4, Sugar, Vanilla;
	public static OreDictMaterial Glycerol, Glyceryl, SO2, SO3, H2S, H2SO4, SulfuricAcid, H2S2O7;
	public static OreDictMaterial AgI, SilverIodide, H2SiF6, HexafluorosilicicAcid, SiC, SiO2, SiliconDioxide, Glass;
	public static OreDictMaterial Flint, H3BO3, HydrogenBorate, BoricAcid, Datolite, H2Ca2B2Si2O10, V2O5, VanadiumPentoxide;
	public static OreDictMaterial Nb2O5, Ta2O5, PO4, WO3, H2WO4, Al2O3, AlF3, AlO3H3;
	public static OreDictMaterial TiO2, TiCl4, TitaniumTetrachloride, MnO2, MnCl2, Fe2O3, FeCl2, FeCl3;
	public static OreDictMaterial FeO3H3, MgCl2, MgCO3, CaCl2, CaSO4, Gypsum, Quicklime, CaCO3;
	public static OreDictMaterial CaF2, FluoriteRed, FluoritePink, FluoriteBlue, FluoriteGreen, FluoriteBlack, FluoriteWhite, FluoriteYellow;
	public static OreDictMaterial FluoriteOrange, FluoriteMagenta, LiCl, LiClO3, LiClO4, Li2O, Li2Fe2O4, LiOH;
	public static OreDictMaterial NaCl, NaNO3, NaOH, NaHCO3, Soda, NaHSO4, NaSO4, Na2S;
	public static OreDictMaterial Na2SO3, Na2SO4, Na2S2O7, Na2CO3, NaAlO2, NaF, Na3AlF6, Cryolite;
	public static OreDictMaterial SaltWater, KIO3, IodineSalt, KCl, KNO3, KOH, KHSO4, KSO4;
	public static OreDictMaterial K2S, K2SO3, K2SO4, K2S2O7, K2CO3, KAlO2, KF, K2TaF7;
	public static OreDictMaterial SaltedWater, ChloroauricAcid, ChloroplatinicAcid, StannicChloride, BlackVitriol, BlueVitriol, GreenVitriol, RedVitriol;
	public static OreDictMaterial PinkVitriol, CyanVitriol, WhiteVitriol, GrayVitriol, MartianVitriol, VitriolOfClay, UF4, UF6;
	public static OreDictMaterial U238F4, U238F6, U235F4, U235F6, AquaRegia, CobaltHexahydrate, MethaneIce, NitroCarbon;
	public static OreDictMaterial Lava, Biomass, BioFuel, Ethanol, Oil, Oilsands, CrudeOil, Fuel;
	public static OreDictMaterial NitroFuel, Kerosine, Diesel, Petrol, Propane, Butane, Propylene, Ethylene;
	public static OreDictMaterial Creosote, FishOil, WhaleOil, SeedOil, HempOil, LinOil, SunflowerOil, NutOil;
	public static OreDictMaterial OliveOil, FryingOilHot, Glue, Lubricant, ConstructionFoam, UUAmplifier, UUMatter, Latex;
	public static OreDictMaterial Ash, DarkAsh, VolcanicAsh, Chalk, Dolomite, Asbestos, Talc, Pyrite;
	public static OreDictMaterial PotassiumFeldspar, Biotite, Emery, Bark, Wood, WoodTreated, WoodPolished, WoodRubber;
	public static OreDictMaterial Bamboo, Skyroot, Weedwood, Livingwood, Dreamwood, Shimmerwood, Greatwood, Silverwood;
	public static OreDictMaterial Peanutwood, Marshmallow, LiveRoot, PetrifiedWood, Wax, WaxBee, WaxRefractory, WaxParaffin;
	public static OreDictMaterial WaxPlant, WaxMagic, WaxAmnesic, WaxSoulful, Basalz, Blitz, Blizz, Blaze;
	public static OreDictMaterial Breeze, Ceramic, Brick, Clay, ClayBrown, ClayRed, Bentonite, Palygorskite;
	public static OreDictMaterial Kaolinite, Porcelain, Graphite, Niter, Phosphorus, PhosphorusBlue, PhosphorusRed, PhosphorusWhite;
	public static OreDictMaterial Apatite, Phosphorite, Paper, Rubber, Plastic, Teflon, PTFE, PVC;
	public static OreDictMaterial Bakelite, Polycarbonate, Bone, BoneWither, SlimyBone, Gunpowder, Dynamite, Asphalt;
	public static OreDictMaterial Tallow, Leather, Indigo, MeatCooked, MeatRaw, MeatRotten, FishCooked, FishRaw;
	public static OreDictMaterial FishRotten, Wheat, Barley, Rye, Rice, Oat, OatAbyssal, Corn;
	public static OreDictMaterial Potato, Tofu, SoylentGreen, Cheese, Chili, Cocoa, Chocolate, Coffee;
	public static OreDictMaterial Cinnamon, Nutmeg, Peanut, Hazelnut, Pistachio, Almond, PEZ, Licorice;
	public static OreDictMaterial Nougat, PepperBlack, Curry, Milk, Butter, ButterSalted, Honey, Honeydew;
	public static OreDictMaterial Tea, Mint, Diamond, DiamondBlue, DiamondGreen, DiamondPurple, DiamondRed, DiamondYellow;
	public static OreDictMaterial DiamondPink, DiamondIndustrial, ManaDiamond, ElvenDragonstone, Gravitite, Emerald, Aquamarine, Morganite;
	public static OreDictMaterial Heliodor, Goshenite, Bixbite, Maxixe, Sapphire, Ruby, BlueSapphire, GreenSapphire;
	public static OreDictMaterial PurpleSapphire, YellowSapphire, OrangeSapphire, Spinel, BalasRuby, Almandine, Grossular, Pyrope;
	public static OreDictMaterial Spessartine, Andradite, Uvarovite, Jasper, JasperOcean, JasperRainforest, JasperBlue, JasperGreen;
	public static OreDictMaterial JasperYellow, TigerEyeYellow, TigerEyeGreen, TigerEyeRed, TigerEyeBlue, TigerEyeBlack, TigerIron, AventurineGreen;
	public static OreDictMaterial AventurineBrown, AventurineYellow, AventurineBlack, AventurineBlue, AventurineRed, Topaz, BlueTopaz, Tanzanite;
	public static OreDictMaterial Zanite, Amazonite, Alexandrite, Opal, OnyxRed, OnyxBlack, Sugilite, Peridot;
	public static OreDictMaterial Amethyst, Dioptase, Carminite, Amber, AmberGolden, AmberDominican, Craponite, Jade;
	public static OreDictMaterial Vinteum, VinteumPurified, ArcaneAsh, ArcaneCompound, Moonstone, Sunstone, Chimerite, CrimsonMiddle;
	public static OreDictMaterial GreenMiddle, AquaMiddle, Valonite, Scabyst, Ambrosium, Continuum, EnderAmethyst, EnderPearl;
	public static OreDictMaterial EnderEye, NetherStar, Frezarite, RedMeteor, Dilithium, Zircon, Azurite, Eudialyte;
	public static OreDictMaterial Lazurite, Sodalite, Lapis, Charcoal, Coal, CoalCoke, Anthracite, Prismane;
	public static OreDictMaterial Lonsdaleite, Lignite, LigniteCoke, PetCoke, Peat, PeatBituminous, HydratedCoal, Graphene;
	public static OreDictMaterial Ectoplasm, Firestone, Redstone, Nikolite, Glowstone, GlowstoneCeres, GlowstoneIo, GlowstoneEnceladus;
	public static OreDictMaterial GlowstoneProteus, GlowstonePluto, Gloomstone, MilkyQuartz, NetherQuartz, VoidQuartz, SunnyQuartz, LavenderQuartz;
	public static OreDictMaterial RedQuartz, BlazeQuartz, SmokeyQuartz, ManaQuartz, ElvenQuartz, BlackQuartz, CertusQuartz, ChargedCertusQuartz;
	public static OreDictMaterial Fluix, Redstonia, Palis, Diamantine, VoidCrystal, Emeradic, Enori, DarkMatter;
	public static OreDictMaterial RedMatter, EnergiumRed, EnergiumCyan, InfusedDull, InfusedVis, InfusedAir, InfusedFire, InfusedEarth;
	public static OreDictMaterial InfusedWater, InfusedEntropy, InfusedOrder, InfusedBalance, HexoriumBlack, HexoriumRed, HexoriumGreen, HexoriumBlue;
	public static OreDictMaterial HexoriumWhite, Sand, RedSand, EndSandWhite, EndSandBlack, SoulSand, SluiceSand, PlatinumGroupSludge;
	public static OreDictMaterial RareEarth, Monazite, Force, Forcicium, Forcillium, Stone, Gravel, Concrete;
	public static OreDictMaterial Netherrack, NetherBrick, Endstone, Obsidian, Bedrock, PrismarineLight, PrismarineDark, Greenstone;
	public static OreDictMaterial Bluestone, Epidote, Oilshale, Petrotheum, Aerotheum, Pyrotheum, Cryotheum, WroughtIron;
	public static OreDictMaterial AnnealedCopper, Alduorite, Infuscolium, Rubracium, Meutoite, Lemurite, Aredrite, Ceruclase;
	public static OreDictMaterial Oureclase, Kalendrite, Carmot, Sanguinite, Vyroxeres, Eximite, Ignatius, DeepIron;
	public static OreDictMaterial ShadowIron, Adamantine, Prometheum, Vulcanite, Orichalcum, AstralSilver, Midasium, Mithril;
	public static OreDictMaterial Celenegil, ShadowSteel, Inolashite, Haderoth, Desichalkos, Tartarite, Amordrine, Electrum;
	public static OreDictMaterial SterlingSilver, RoseGold, Angmallen, InductiveAlloy, Cd_In_Ag_Alloy, GildedIron, Brass, CobaltBrass;
	public static OreDictMaterial AluminiumAlloy, Bronze, BlackBronze, BismuthBronze, Hepatizon, ArsenicCopper, ArsenicBronze, Steel;
	public static OreDictMaterial BlackSteel, BlueSteel, RedSteel, DamascusSteel, VanadiumSteel, TungstenSteel, TungstenCarbide, HSLA;
	public static OreDictMaterial SpringSteel, TungstenAlloy, PigIron, IronCompressed, IronCast, IronMagnetic, SteelMagnetic, NeodymiumMagnetic;
	public static OreDictMaterial DarkIron, SteelGalvanized, TungstenSintered, TitaniumGold, Ta4HfC5, MeteoricIron, MeteoricSteel, MeteoricBlackSteel;
	public static OreDictMaterial MeteoricBlueSteel, MeteoricRedSteel, RedAlloy, BlueAlloy, PurpleAlloy, Mingrade, RedstoneAlloy, NikolineAlloy;
	public static OreDictMaterial ElectrotineAlloy, ElectrumFlux, ConductiveIron, EnergeticSilver, Invar, Constantan, Cupronickel, Nichrome;
	public static OreDictMaterial Kanthal, Magnalium, StainlessSteel, Ultimet, TinAlloy, BatteryAlloy, SolderingAlloy, IronWood;
	public static OreDictMaterial Steeleaf, Knightmetal, FierySteel, Fireleaf, MeteoflameSteel, MeteoflameBlackSteel, MeteoflameBlueSteel, MeteoflameRedSteel;
	public static OreDictMaterial FlamascusSteel, Thaumium, DarkThaumium, VoidMetal, Osmiridium, Sunnarium, ChromiumDioxide, CrO2;
	public static OreDictMaterial VanadiumGallium, YttriumBariumCuprate, NiobiumNitride, NiobiumTitanium, AluminiumBrass, Ardite, Alumite, Manyullyn;
	public static OreDictMaterial VibraniumSteel, VibraniumSilver, Vibramantium, Signalum, Lumium, EnderiumBase, Enderium, RefinedGlowstone;
	public static OreDictMaterial RefinedObsidian, Yellorium, Blutonium, Cyanite, Ludicrite, Yellorite, Bedrock_HSLA_Alloy, ObsidianSteel;
	public static OreDictMaterial PulsatingIron, EnergeticAlloy, VibrantAlloy, ElectricalSteel, Soularium, CrudeSteel, EndSteel, MelodicAlloy;
	public static OreDictMaterial StellarAlloy, VividAlloy, CrystallineAlloy, CrystallinePinkSlime, SpectreIron, Manasteel, Terrasteel, ElvenElementium;
	public static OreDictMaterial GaiaSpirit, Endium, Mauftrium, Elvorium, NiflheimPower, MuspelheimPower, Iffesal, AncientDebris;
	public static OreDictMaterial Netherite, NetherizedDiamond, Efrine, Desh, DeshAlloy, DuraniumAlloy, TritaniumAlloy, Dolamide;
	public static OreDictMaterial Oriharukon, Adamantite, Duralumin, Meteorite, FrozenIron, Kreknorite, Syrmorite, Octine;
	public static OreDictMaterial HSSG, HSSE, HSSS, Bedrockium, Draconium, DraconiumAwakened, CrystalMatrix, CosmicNeutronium;
	public static OreDictMaterial Infinity, Unstable, Trinaquadalloy, Trinitanium, Iritanium, TitaniumAluminide, Trinium, Vibranium;
	public static OreDictMaterial Naquadah, NaquadahEnriched, Naquadria, FakeOsmium, Adamantium, Silver, Aluminium, Bismuth;
	public static OreDictMaterial Lead, Argon, Copper, Gold, Iron, Titanium, Calcite, Tungsten;
	public static OreDictMaterial Beryllium, Chromium, Manganese, Cobalt, Cobalt60, Nickel, Arsenic, Zirconium;
	public static OreDictMaterial Molybdenum, Technetium, Palladium, Neodymium, Osmium, Iridium, Platinum, Thorium;
	public static OreDictMaterial Uranium, Uranium235, Plutonium, Plutonium241, Plutonium243, Americium, Americium241, Alumina;
	public static OreDictMaterial AluminiumFluoride, AluminiumHydroxide, Gibbsite, Fluorite, Soapstone, WoodSealed, TeslatineAlloy, Teslatite;
	public static OreDictMaterial Electrotine, Olivine, SpaceRock, MoonRock, MoonTurf, MarsRock, MarsSand, Holystone;
	public static OreDictMaterial Livingrock, Deadrock, Betweenstone, Pitstone, Umber, Redrock, Komatiite, Pumice;
	public static OreDictMaterial Gabbro, Basalt, Marble, Limestone, Greenschist, Blueschist, Kimberlite, Quartzite;
	public static OreDictMaterial GraniteRed, GraniteBlack, Granite, Andesite, Diorite, Blackstone, Gneiss, Greywacke;
	public static OreDictMaterial Siltstone, Rhyolite, Migmatite, Chert, Dacite, Shale, Slate, Eclogite;
	/** Upstream MT.java:524 NULL is created at class-load time (like the upstream <clinit>), so MaterialRegistry.get(name, MT.NULL) keeps working before init(). */
	static {
		NULL = create(-1, "NULL").setStatsElement(0,0,0,0,0).put(INVALID_MATERIAL, DONT_SHOW_THIS_COMPONENT);
	}

	private static void reg0000() { // upstream MT.java:524-569
		Empty = create(0, "Empty").setStatsElement(0,0,0,0,0).put(EMPTY, AUTO_BLACKLIST, DONT_SHOW_THIS_COMPONENT);
		Photon                    =   y       = setRGBa(create(   1, "Photon"     ).setStatsElement( 0, 0, 0, 0, 0).heat(0,0,0), 255, 255, 255, 255).put(PARTICLE).tooltip("y").hide();
		Neutrino                  =   v       = setRGBa(create(   2, "Neutrino"   ).setStatsElement( 0, 0, 0, 0, 0).heat(0,0,0), 180, 180, 180,   0).put(PARTICLE).tooltip("v").hide();
		Neutron                   =   n       = setRGBa(create(   3, "Neutron"    ).setStatsElement( 0, 0, 1, 0, 0).heat(0,0,0), 128, 128, 128,   0).put(PARTICLE).tooltip("n").hide();
		Proton                    =   p       = setRGBa(create(   4, "Proton"     ).setStatsElement( 1, 0, 0, 0, 0).heat(0,0,0), 255,   0,   0,   0).put(PARTICLE).tooltip("p").hide();
		Electron                  =   e       = setRGBa(create(   5, "Electron"   ).setStatsElement( 0, 1, 0, 0, 0).heat(0,0,0),   0,   0, 255,   0).put(PARTICLE).tooltip("e").hide();
		Magic                     =   Ma      = setTextures(setRGBa(create( 4000, "Magic"     ).setStatsElement( 0, 0, 0,-1, 0).heat(0,0,0), 255,   0, 255,   0), SET_SHINY).put(ELEMENT, MAGICAL, UNBURNABLE).tooltip("Ma").hide().qual(3, 10.0, 5120, 5);
		H       = hydrogen       ();
		D       = deuterium      ();
		H_2=D;
		T       = tritium        ();
		H_3=T;
		He      = helium         ();
		He_3    = helium3        ();
		Li      = lithium        ();
		Li_6    = lithium6       ();
		Be      = beryllium      ().qual(2, 14.0, 64, 2);
		Be_7    = beryllium7     ();
		Be_8    = beryllium8     ();
		B       = boron          ();
		B_11    = boron11        ();
		C       = carbon         ();
		C_13    = carbon13       ();
		C_14    = carbon14       ();
		N       = nitrogen       ();
		O       = oxygen         ();
		F       = fluorine       ();
		Ne      = neon           ();
		Na      = sodium         ();
		Mg      = magnesium      ();
		Al      = aluminium      ().qual(2, 10.0, 128, 2);
	}
	private static void reg0001() { // upstream MT.java:570-600
		Si      = setPriorityPrefix(silicon        (), 5);
		P       = phosphor       ();
		S       = setPriorityPrefix(sulfur         (), 2);
		Cl      = chlorine       ();
		Ar      = argon          ();
		K       = potassium      ();
		Ca      = calcium        ();
		Sc      = scandium       ();
		Ti      = titanium       ().qual(3,  8.0, 2560, 3);
		V       = vanadium       ();
		Cr      = chromium       ().qual(3, 11.0,  256, 3);
		Mn      = manganese      ().qual(3,  7.0,  256, 2);
		Fe      = setRGBaLiquid(iron           ().qual(3,  6.0,  256, 2), 255, 64, 32, 255);
		Co      = cobalt         ().qual(3,  5.0,  256, 3);
		Co_60   = cobalt60       ().qual(3,  5.0,  256, 3);
		Ni      = nickel         ().qual(2,  6.0,   64, 2);
		Cu      = copper         ().qual(2,  4.0,   64, 0);
		Zn      = zinc           ();
		Ga      = gallium        ();
		Ge      = germanium      ().qual(2,  6.0,  256, 2);
		As      = arsenic        ();
		Se      = selenium       ();
		Br      = bromine        ();
		Kr      = krypton        ();
		Rb      = rubidium       ();
		Sr      = strontium      ();
		Y       = yttrium        ();
		Zr      = zirconium      ().qual(3,  8.0, 1280, 3);
		Nb      = niobium        ();
		Mo      = molybdenum     ().qual(3,  7.0,  512, 2);
		Tc      = technetium     ().qual(3, 10.0, 1280, 1);
		Gregorium=Tc;
	}
	private static void reg0002() { // upstream MT.java:601-632
		Ru      = ruthenium      ();
		Rh      = rhodium        ();
		Pd      = palladium      ().qual(3,  8.0,  512, 2);
		Ag      = silver         ().qual(3, 10.0,   64, 2);
		Cd      = cadmium        ();
		In      = indium         ();
		Sn      = tin            ();
		Sb      = antimony       ();
		Te      = tellurium      ();
		I       = iodine         ();
		Xe      = xenon          ();
		Cs      = caesium        ();
		Ba      = barium         ();
		La      = lanthanium     ();
		Ce      = cerium         ();
		Pr      = praseodymium   ();
		Nd      = neodymium      ().qual(3, 6.0, 512, 3);
		Pm      = promethium     ();
		Sm      = samarium       ();
		Eu      = europium       ();
		Gd      = gadolinium     ();
		Tb      = terbium        ();
		Dy      = dysprosium     ();
		Ho      = holmium        ();
		Er      = erbium         ();
		Tm      = thulium        ();
		Yb      = ytterbium      ();
		Lu      = lutetium       ();
		Hf      = hafnium        ();
		Ta      = tantalum       ();
		W       = tungsten       ().qual(3,  8.0, 5120, 3);
		Re      = rhenium        ();
	}
	private static void reg0003() { // upstream MT.java:633-664
		Os      = osmium         ().qual(3, 16.0, 1280, 4).setLocal("Osmium");
		Ir      = setRGBaLiquid(iridium        ().qual(3,  6.0, 5120, 4), 255, 128, 200, 255);
		Pt      = platinum       ().qual(3, 15.0, 64, 2);
		Au      = gold           ().qual(3, 12.5, 64, 2);
		Au_198  = gold198        ();
		Hg      = mercury        ();
		Tl      = thallium       ();
		Pb      = lead           ().qual(2,  8.0, 64, 1);
		Bi      = bismuth        ().qual(2,  6.0, 64, 1);
		Po      = polonium       ();
		At      = astatine       ();
		Rn      = radon          ();
		Fr      = francium       ();
		Ra      = radium         ();
		Ac      = actinium       ();
		Th      = thorium        ().qual(3, 6.0, 512, 2);
		Pa      = protactinium   ();
		U_238   = uranium        ().qual(3, 6.0, 512, 3);
		U_235   = uranium235     ().qual(3, 6.0, 512, 3);
		U_233   = uranium233     ().qual(3, 6.0, 512, 3);
		Np      = neptunium      ();
		Pu      = plutonium      ().qual(3, 6.0, 512, 3);
		Pu_240  = plutonium240   ().qual(3, 6.0, 512, 3);
		Pu_241  = plutonium241   ().qual(3, 6.0, 512, 3);
		Pu_243  = plutonium243   ().qual(3, 6.0, 512, 3);
		Pu_238  = plutonium238   ().qual(3, 6.0, 512, 3);
		Pu_239  = plutonium239   ().qual(3, 6.0, 512, 3);
		Am      = americium      ().qual(3, 4.0, 256, 2);
		Am_241  = americium241   ().qual(3, 4.0, 256, 2);
		Am_242  = americium242   ().qual(3, 4.0, 256, 2);
		Cm      = curium         ();
		Bk      = berkelium      ();
	}
	private static void reg0004() { // upstream MT.java:665-696
		Cf      = californium    ();
		Es      = einsteinium    ();
		Fm      = fermium        ();
		Md      = mendelevium    ();
		No      = nobelium       ();
		Lr      = lawrencium     ();
		Rf      = rutherfordium  ();
		Db      = dubnium        ();
		Sg      = seaborgium     ();
		Bh      = bohrium        ();
		Hs      = hassium        ();
		Mt      = meitnerium     ();
		Ds      = darmstadtium   ();
		Rg      = roentgenium    ();
		Cn      = copernicium    ();
		Nh      = nihonium       ();
		Fl      = flerovium      ();
		Fl_298  = flerovium298   ();
		Mc      = moscovium      ();
		Lv      = livermorium    ();
		Fa      = farnsium       ();
		Ts = Fa;
		Og      = oganesson      ();
		Uue     = ununennium     ();
		Ubn     = unbinilium     ();
		Ubu     = unknown( 1210,  182);
		Ubb     = unknown( 1220,  184);
		Ubt     = unknown( 1230,  186);
		Ubq     = unknown( 1240,  188);
		Tn      = element( 1250, "TritaniumElemental"    , "Tn"      , 125, 198, 2000, 3138, 25.0    , SET_DULL    ,  55, 155, 155, 255, G_DUST_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unbipentium").setLocal("Elemental Tritanium");
		Ubp = Tn;
		Ke      = element( 1260, "Trinium"               , "Ke"      , 126, 192, 2645, 4523,  1.06874, SET_COPPER  , 234, 234, 234, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unbihexium").qual(3, 12.0, 2560, 4);
	}
	private static void reg0005() { // upstream MT.java:696-725
		Ubh = Ke;
		Ubs     = unknown( 1270,  194);
		Ubo     = unknown( 1280,  196);
		Ube     = unknown( 1290,  198);
		Utn     = unknown( 1300,  200);
		Utu     = unknown( 1310,  203);
		Utb     = unknown( 1320,  206);
		Utt     = unknown( 1330,  209);
		Utq     = unknown( 1340,  212);
		Utp     = unknown( 1350,  215);
		Uth     = unknown( 1360,  218);
		Uts     = unknown( 1370,  221);
		Uto     = unknown( 1380,  224);
		Ute     = unknown( 1390,  227);
		Uqn     = unknown( 1400,  230);
		Uqu     = unknown( 1410,  233);
		Uqb     = unknown( 1420,  236);
		Uqt     = unknown( 1430,  239);
		Uqq     = unknown( 1440,  242);
		Dn      = element( 1450, "DuraniumElemental"     , "Dn"      , 145, 190,  1200, 2491, 20.0, SET_DULL,  75, 175, 175, 255, G_DUST_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unquadpentium").setLocal("Elemental Duranium");
		Uqp = Dn;
		Uqh     = unknown( 1460,  248);
		Uqs     = unknown( 1470,  251);
		Uqo     = unknown( 1480,  254);
		Uqe     = unknown( 1490,  257);
		Upn     = unknown( 1500,  260);
		Upu     = unknown( 1510,  263);
		Vb      = setPriorityPrefix(element( 1520, "Vibranium"             , "Vb"      , 152, 266,  4852, 9415, 3.23978365, SET_EMERALD, 200, 128, 255, 100, G_GEM_ORES_TRANSPARENT, UUM, VALUABLE, GLOWING, UNBURNABLE, "Unpentbium"), 1).qual(3, 1000.0, 512, 15);
		Upb = Vb;
		Upt     = unknown( 1530,  269);
		Upq     = unknown( 1540,  272);
		Upp     = unknown( 1550,  276);
	}
	private static void reg0006() { // upstream MT.java:726-756
		Uph     = unknown( 1560,  280);
		Ups     = unknown( 1570,  284);
		Upo     = unknown( 1580,  288);
		Upe     = unknown( 1590,  292);
		Uhn     = unknown( 1600,  296);
		Uhu     = unknown( 1610,  300);
		Uhb     = unknown( 1620,  304);
		Uht     = unknown( 1630,  308);
		Uhq     = unknown( 1640,  312);
		Uhp     = unknown( 1650,  316);
		Uhh     = unknown( 1660,  320);
		Uhs     = unknown( 1670,  324);
		Uho     = unknown( 1680,  328);
		Uhe     = unknown( 1690,  332);
		Usn     = unknown( 1700,  336);
		Usu     = unknown( 1710,  340);
		Usb     = unknown( 1720,  344);
		Ust     = unknown( 1730,  348);
		Nq      = setRGBaLiquid(element( 1740, "Naquadah"              , "Nq"      , 174, 352, 1500, 3000, 21.0, SET_RAD       ,  50,  50,  50, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unseptquadium"),   0, 255,   0, 255).qual(3, 6.0, 1280, 4);
		Usq = Nq;
		Nq_528  = setRGBaLiquid(element( 1741, "Naquadah-Enriched"     , "Nq-528"  , 174, 354, 1500, 3000, 22.0, SET_RAD       ,  60,  60,  60, 255, G_INGOT_ORES, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, EXPLOSIVE),  64, 255,  64, 255).setLocal("Enriched Naquadah").qual(3, 6.0, 1280, 4);
		Nq_522  = setRGBaLiquid(element( 1742, "Naquadria"             , "Nq-522"  , 174, 348, 1500, 3000, 20.0, SET_RAD       ,  30,  30,  30, 255, G_INGOT_ORES, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, EXPLOSIVE), 128, 255, 128, 255).qual(3, 1.0, 512, 4);
		Usp     = unknown( 1750,  356);
		Ush     = unknown( 1760,  360);
		Uss     = unknown( 1770,  364);
		Uso     = unknown( 1780,  368);
		Use     = unknown( 1790,  372);
		Uon     = unknown( 1800,  376);
		Uou     = unknown( 1810,  380);
		Uob     = unknown( 1820,  384);
		Uot     = unknown( 1830,  388);
		Uoq     = unknown( 1840,  392);
	}
	private static void reg0007() { // upstream MT.java:757-784
		An      = element( 1850, "Abyssalnite"           , "An"      , 185, 396, 1500, 3000, 15.0, SET_METALLIC  ,  90,  40, 170, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unoctpentium"                                ).qual(3, 8.0, 1600, 3);
		Uop = An;
		Cor     = element( 1860, "Coralium"              , "Cor"     , 186, 400, 2000, 4000, 20.0, SET_RUBY      ,  20, 160, 110, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, G_GEM_ORES, "Unocthexium", "LiquifiedCoralium").qual(3,12.0, 3200, 3);
		Uoh = Cor;
		Dr      = element( 1870, "Dreadium"              , "Dr"      , 187, 405, 2500, 5000, 25.0, SET_METALLIC  , 170,   0,   0, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unoctseptium"                                ).qual(3,16.0, 4800, 4);
		Uos = Dr;
		Etx     = element( 1880, "Ethaxium"              , "Etx"     , 188, 410, 3000, 6000, 30.0, SET_METALLIC  , 160, 170, 150, 255, G_INGOT_MACHINE_ORES, UUM, METAL, SMITHABLE, MELTING, MOLTEN, EXTRUDER, "Unoctoctium"                                 ).qual(3,20.0, 6400, 4);
		Uoo = Etx;
		Uoe     = unknown( 1890,  415);
		Uen     = unknown( 1900,  420);
		Ueu     = unknown( 1910,  425);
		Ueb     = unknown( 1920,  430);
		Uet     = unknown( 1930,  435);
		Ueq     = unknown( 1940,  440);
		Uep     = unknown( 1950,  445);
		Ueh     = unknown( 1960,  450);
		Ues     = unknown( 1970,  455);
		Ueo     = unknown( 1980,  460);
		Uee     = unknown( 1990,  465);
		Bnn     = unknown( 2000,  470);
		Bnu     = unknown( 2010,  475);
		Bnb     = unknown( 2020,  480);
		Bnt     = unknown( 2030,  485);
		Bnq     = unknown( 2040,  490);
		Bnp     = unknown( 2050,  495);
		Bnh     = unknown( 2060,  500);
		Bns     = unknown( 2070,  505);
		Bno     = unknown( 2080,  510);
		Bne     = unknown( 2090,  515);
		Bun     = unknown( 2100,  520);
		Buu     = unknown( 2110,  525);
		Bub     = unknown( 2120,  530);
	}
	private static void reg0008() { // upstream MT.java:785-812
		But     = unknown( 2130,  535);
		Buq     = unknown( 2140,  540);
		Bup     = unknown( 2150,  545);
		Buh     = unknown( 2160,  550);
		Bus     = unknown( 2170,  555);
		Buo     = unknown( 2180,  560);
		Bue     = unknown( 2190,  565);
		Bbn     = unknown( 2200,  570);
		Atl     = element( 2210, "Atlarus"               , "Atl"     , 221, 575, 3276, 11524, 21.24625421, SET_METALLIC, 204, 179,   0, 255, G_INGOT_MACHINE_ORES, UUM, SMITHABLE, MELTING, MOLTEN, EXTRUDER, MAGICAL, "Bibiunium").qual(3,  4.0, 4480, 4);
		Bbu = Atl;
		Atlarus = Atl;
		Ad      = element( 2220, "Adamantium"            , "Ad"      , 222, 580, 5225, 14528, 13.35624762, SET_SHINY   , 255, 255, 255, 255, G_INGOT_MACHINE_ORES, UUM, SMITHABLE, MELTING, MOLTEN, EXTRUDER, MAGICAL, MAGNETIC_PASSIVE, UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, RAILS, "Adamant", "Bibibium").qual(3, 10.0, 5120, 5);
		Bbb = Ad;
		Bbt     = unknown( 2230,  585);
		Bbq     = unknown( 2240,  590);
		Bbp     = unknown( 2250,  595);
		Bbh     = unknown( 2260,  600);
		Bbs     = unknown( 2270,  605);
		Bbo     = unknown( 2280,  610);
		Bbe     = unknown( 2290,  615);
		Btn     = unknown( 2300,  620);
		Btu     = unknown( 2310,  625);
		Btb     = unknown( 2320,  630);
		Btt     = unknown( 2330,  635);
		Btq     = unknown( 2340,  640);
		Btp     = unknown( 2350,  645);
		Bth     = unknown( 2360,  650);
		Bts     = unknown( 2370,  655);
		Bto     = unknown( 2380,  660);
		Mcg     = element( 2390, "Mac-Guffium"           , "Mcg"     , 239, 665, 200,  1000,  3.122, SET_SHINY, 200,  50, 150, 255, G_CONTAINERS, UUM, VALUABLE, GLOWING, LIGHTING, "Bitriennium");
		Bte = Mcg;
		Bqn     = unknown( 2400,  670);
	}
	private static void reg0009() { // upstream MT.java:813-844
		Bqu     = unknown( 2410,  675);
		Bqb     = unknown( 2420,  680);
		Bqt     = unknown( 2430,  685);
		Bqq     = unknown( 2440,  690);
		Bqp     = unknown( 2450,  695);
		Bqh     = unknown( 2460,  700);
		Bqs     = unknown( 2470,  705);
		Bqo     = unknown( 2480,  710);
		Bqe     = unknown( 2490,  715);
		Bpn     = unknown( 2500,  720);
		Bpu     = unknown( 2510,  725);
		Bpb     = unknown( 2520,  730);
		Bpt     = unknown( 2530,  735);
		Bpq     = unknown( 2540,  740);
		Bpp     = unknown( 2550,  745);
		Bph     = unknown( 2560,  750);
		Bps     = unknown( 2570,  755);
		Bpo     = unknown( 2580,  760);
		Bpe     = unknown( 2590,  765);
		Bhn     = unknown( 2600,  770);
		Bhu     = unknown( 2610,  775);
		Bhb     = unknown( 2620,  780);
		Bht     = unknown( 2630,  785);
		Bhq     = unknown( 2640,  790);
		Bhp     = unknown( 2650,  795);
		Bhh     = unknown( 2660,  800);
		Bhs     = unknown( 2670,  805);
		Bho     = unknown( 2680,  810);
		Bhe     = unknown( 2690,  815);
		Bsn     = unknown( 2700,  820);
		Bsu     = unknown( 2710,  825);
		Bsb     = unknown( 2720,  830);
	}
	private static void reg0010() { // upstream MT.java:845-876
		Bst     = unknown( 2730,  835);
		Bsq     = unknown( 2740,  840);
		Bsp     = unknown( 2750,  845);
		Bsh     = unknown( 2760,  850);
		Bss     = unknown( 2770,  855);
		Bso     = unknown( 2780,  860);
		Bse     = unknown( 2790,  865);
		Bon     = unknown( 2800,  870);
		Bou     = unknown( 2810,  875);
		Bob     = unknown( 2820,  880);
		Bot     = unknown( 2830,  885);
		Boq     = unknown( 2840,  890);
		Bop     = unknown( 2850,  895);
		Boh     = unknown( 2860,  900);
		Bos     = unknown( 2870,  905);
		Boo     = unknown( 2880,  910);
		Boe     = unknown( 2890,  915);
		Ben     = unknown( 2900,  920);
		Beu     = unknown( 2910,  925);
		Beb     = unknown( 2920,  930);
		Bet     = unknown( 2930,  935);
		Beq     = unknown( 2940,  940);
		Bep     = unknown( 2950,  945);
		Beh     = unknown( 2960,  950);
		Bes     = unknown( 2970,  955);
		Beo     = unknown( 2980,  960);
		Bee     = unknown( 2990,  965);
		Tnn     = unknown( 3000,  970);
		Tnu     = unknown( 3010,  975);
		Tnb     = unknown( 3020,  980);
		Tnt     = unknown( 3030,  985);
		Tnq     = unknown( 3040,  990);
	}
	private static void reg0011() { // upstream MT.java:877-908
		Tnp     = unknown( 3050,  995);
		Tnh     = unknown( 3060, 1000);
		Tns     = unknown( 3070, 1005);
		Tno     = unknown( 3080, 1010);
		Tne     = unknown( 3090, 1015);
		Tun     = unknown( 3100, 1020);
		Tuu     = unknown( 3110, 1025);
		Tub     = unknown( 3120, 1030);
		Tut     = unknown( 3130, 1035);
		Tuq     = unknown( 3140, 1040);
		Tup     = unknown( 3150, 1045);
		Tuh     = unknown( 3160, 1050);
		Tus     = unknown( 3170, 1055);
		Tuo     = unknown( 3180, 1060);
		Tue     = unknown( 3190, 1065);
		Tbn     = unknown( 3200, 1070);
		Tbu     = unknown( 3210, 1075);
		Tbb     = unknown( 3220, 1080);
		Tbt     = unknown( 3230, 1085);
		Tbq     = unknown( 3240, 1090);
		Tbp     = unknown( 3250, 1095);
		Tbh     = unknown( 3260, 1100);
		Tbs     = unknown( 3270, 1105);
		Tbo     = unknown( 3280, 1110);
		Tbe     = unknown( 3290, 1115);
		Ttn     = unknown( 3300, 1120);
		Ttu     = unknown( 3310, 1125);
		Ttb     = unknown( 3320, 1130);
		Ttt     = unknown( 3330, 1135);
		Ttq     = unknown( 3340, 1140);
		Ttp     = unknown( 3350, 1145);
		Tth     = unknown( 3360, 1150);
	}
	private static void reg0012() { // upstream MT.java:909-940
		Tts     = unknown( 3370, 1155);
		Tto     = unknown( 3380, 1160);
		Tte     = unknown( 3390, 1165);
		Tqn     = unknown( 3400, 1170);
		Tqu     = unknown( 3410, 1175);
		Tqb     = unknown( 3420, 1180);
		Tqt     = unknown( 3430, 1185);
		Tqq     = unknown( 3440, 1190);
		Tqp     = unknown( 3450, 1195);
		Tqh     = unknown( 3460, 1200);
		Tqs     = unknown( 3470, 1205);
		Tqo     = unknown( 3480, 1210);
		Tqe     = unknown( 3490, 1215);
		Tpn     = unknown( 3500, 1220);
		Tpu     = unknown( 3510, 1225);
		Tpb     = unknown( 3520, 1230);
		Tpt     = unknown( 3530, 1235);
		Tpq     = unknown( 3540, 1240);
		Tpp     = unknown( 3550, 1245);
		Tph     = unknown( 3560, 1250);
		Tps     = unknown( 3570, 1255);
		Tpo     = unknown( 3580, 1260);
		Tpe     = unknown( 3590, 1265);
		Thn     = unknown( 3600, 1270);
		Thu     = unknown( 3610, 1275);
		Thb     = unknown( 3620, 1280);
		Tht     = unknown( 3630, 1285);
		Thq     = unknown( 3640, 1290);
		Thp     = unknown( 3650, 1295);
		Thh     = unknown( 3660, 1300);
		Ths     = unknown( 3670, 1305);
		Tho     = unknown( 3680, 1310);
	}
	private static void reg0013() { // upstream MT.java:941-971
		The     = unknown( 3690, 1315);
		Tsn     = unknown( 3700, 1320);
		Tsu     = unknown( 3710, 1325);
		Gt      = element( 3720, "Gravitonium"           , "Gt"      , 372,1330, 112, 1275, 1768.866761, SET_SHINY,   0,  50,   0, 255, G_CONTAINERS, UUM, "Triseptbium");
		Tsb = Gt;
		Tst     = unknown( 3730, 1335);
		Tsq     = unknown( 3740, 1340);
		Tsp     = unknown( 3750, 1345);
		Tsh     = unknown( 3760, 1350);
		Tss     = unknown( 3770, 1355);
		Tso     = unknown( 3780, 1360);
		Tse     = unknown( 3790, 1365);
		Ton     = unknown( 3800, 1370);
		Tou     = unknown( 3810, 1375);
		Tob     = unknown( 3820, 1380);
		Tot     = unknown( 3830, 1385);
		Toq     = unknown( 3840, 1390);
		Top     = unknown( 3850, 1395);
		Toh     = unknown( 3860, 1400);
		Tos     = unknown( 3870, 1405);
		Too     = unknown( 3880, 1410);
		Toe     = unknown( 3890, 1415);
		Ten     = unknown( 3900, 1420);
		Teu     = unknown( 3910, 1425);
		Teb     = unknown( 3920, 1430);
		Tet     = unknown( 3930, 1435);
		Teq     = unknown( 3940, 1440);
		Tep     = unknown( 3950, 1445);
		Teh     = unknown( 3960, 1450);
		Tes     = unknown( 3970, 1455);
		Teo     = unknown( 3980, 1460);
		Tee     = unknown( 3990, 1465);
	}
	private static void reg0014() { // upstream MT.java:973-1009
		Neutronium = unused("Neutronium").qual(3, 6.0, 81920, 6).put(IGNORE_IN_COLOR_LOG).tooltip("Nt");
		Primitive      = tier("Primitive"     );
		Basic          = tier("Basic"         );
		Good           = tier("Good"          );
		Advanced       = tier("Advanced"      );
		Data           = tier("Data"          );
		Elite          = tier("Elite"         );
		Master         = tier("Master"        );
		Ultimate       = tier("Ultimate"      );
		Quantum        = tier("Quantum"       );
		Superconductor = tier("Superconductor");
		Infinite       = tier("Infinite"      );
		Black          = dye( 8250, "Black"     ,  32,  32,  32);
		Red            = dye( 8251, "Red"       , 255,   0,   0);
		Green          = dye( 8252, "Green"     ,   0, 255,   0);
		Brown          = dye( 8253, "Brown"     ,  96,  64,   0);
		Blue           = dye( 8254, "Blue"      ,   0,   0, 255);
		Purple         = dye( 8255, "Purple"    , 128,   0, 128);
		Cyan           = dye( 8256, "Cyan"      ,   0, 255, 255);
		LightGray      = dye( 8257, "Light Gray", 192, 192, 192);
		Gray           = dye( 8258, "Gray"      , 128, 128, 128);
		Pink           = dye( 8259, "Pink"      , 255, 192, 192);
		Lime           = dye( 8260, "Lime"      , 128, 255, 128);
		Yellow         = dye( 8261, "Yellow"    , 255, 255,   0);
		LightBlue      = dye( 8262, "Light Blue", 128, 128, 255);
		Magenta        = dye( 8263, "Magenta"   , 255,   0, 255);
		Orange         = dye( 8264, "Orange"    , 255, 128,   0);
		White          = dye( 8265, "White"     , 255, 255, 255);
		H2O                     = setDensity(lquddcmp      ( 9800, "Water"                                         , 100, 100, 255, 255, UNRECYCLABLE, FOOD, MELTING)                                                                                                              .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C  , CS.C+100), 1.0000);
		Water = H2O;
		HDO                     = setDensity(lquddcmp      ( 9811, "Semiheavy Water"                               , 200, 200, 155, 255, UNRECYCLABLE, FOOD, MELTING, LIQUID)                                                                                                      .setMcfg( 0, H              , 1*U, D                , 1*U, O                , 1*U)                                                                          .heat(CS.C+2, CS.C+101), 1.0540);
		D2O                     = setDensity(lquddcmp      ( 9812, "Heavy Water"                                   , 255, 255, 100, 255, UNRECYCLABLE, FOOD, MELTING, LIQUID)                                                                                                      .setMcfg( 0, D              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C+4, CS.C+102), 1.1056);
	}
	private static void reg0015() { // upstream MT.java:1010-1047
		T2O                     = setDensity(lquddcmp      ( 9813, "Tritiated Water"                               , 255, 100, 100, 255, UNRECYCLABLE, FOOD, MELTING, LIQUID)                                                                                                      .setMcfg( 0, T              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C+7, CS.C+104), 1.2112);
		Steam                   = setDensity(gasdcmp       ( 9814, "Steam"                                         , 200, 200, 200, 255, UNRECYCLABLE)                                                                                                                             .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C  , CS.C+100), 0.0010);
		Snow                    = setDensity(dust          ( 9801, "Snow"                  , SET_FINE              , 250, 250, 250, 255, UNRECYCLABLE, FOOD, MORTAR)                                                                                                               .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .setSmelting(H2O, U).heat(CS.C, CS.C+100), 1.0);
		Ice                     = setDensity(create        ( 9802, "Ice"                   , SET_CUBE_SHINY        , 200, 200, 255, 255, G_GEM_TRANSPARENT, CONTAINERS, UNRECYCLABLE, FOOD, BRITTLE, MORTAR, COOL2CRYSTAL)                                                         .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .setSmelting(H2O, U).heat(CS.C, CS.C+100), 1.0).qual(1, 2.0, 4, 0);
		FreshWater              = setDensity(lqud          ( 9803, "Fresh Water"                                   , 110, 110, 255, 255, UNRECYCLABLE, FOOD)                                                                                                                       .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C, CS.C+100), 1.0);
		HolyWater               = setDensity(lqud          ( 9805, "Holy Water"                                    , 120, 120, 255, 255, GLOWING)                                                                                                                                  .setMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C, CS.C+100), 1.0);
		SeaWater                = setDensity(lqud          ( 9806, "Sea Water"                                     ,  90,  90, 255, 255, UNRECYCLABLE, LIQUID)                                                                                                                     .setMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C, CS.C+100), 1.0);
		DirtyWater              = setDensity(lqud          ( 9807, "WaterDirty"                                    ,  70, 150, 200, 255, UNRECYCLABLE, LIQUID)                                                                                                                     .setMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C, CS.C+100), 1.0).setLocal("Dirty Water");
		DistWater               = setDensity(lquddcmp      ( 9808, "WaterDistilled"                                , 110, 110, 255, 255, UNRECYCLABLE, FOOD, MELTING)                                                                                                              .uumMcfg( 0, H              , 2*U, O                , 1*U)                                                                                                  .heat(CS.C, CS.C+100), 1.0).setLocal("Distilled Water");
		H2O2                    = setDensity(lquddcmp      ( 9809, "Hydrogen Peroxide"                             ,  20,  20, 255, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 2*U, O                , 2*U)                                                                                                  , 1.0).heat(CS.C, CS.C+150);
		HCl                     = gasaciddcmp   ( 9826, "Hydrochloric Acid"                             ,   0, 255, 128, 255, GASES)                                                                                                                                    .uumMcfg( 0, H              , 1*U, Cl               , 1*U)                                                                                                  .heat( 100,  200);
		HF                      = gasaciddcmp   ( 9829, "Hydrogen Fluoride"                             ,   0, 240, 240, 255, GASES)                                                                                                                                    .uumMcfg( 0, H              , 1*U, F                , 1*U)                                                                                                  .heat( 189,  292);
		HeNe                    = gaschemcent   ( 9839, "Helium-Neon"                                   , 255,   0, 128, 255, GASES)                                                                                                                                    .uumMcfg( 0, He             , 1*U, Ne               , 1*U);
		HeliumNeon = HeNe;
		Air                     = setDensity(gas           ( 9830, "Air"                                           , 169, 208, 245,  15, TRANSPARENT, GASES)                                                                                                                       .uumMcfg( 0, N              ,40*U, O                ,11*U, Ar               , 1*U)                                                                          .heat( 100,  200), WEIGHT_AIR_G_PER_CUBIC_CENTIMETER);
		NO                      = gaschemelec   ( 9837, "Nitrogen Monoxide"                             , 100, 175, 255,  15, GASES)                                                                                                                                    .uumMcfg( 0, N              , 1*U, O                , 1*U)                                                                                                  .heat( 100,  200);
		NO2                     = gaschemelec   ( 9831, "Nitrogen Dioxide"                              , 120, 190, 255,  15, GASES)                                                                                                                                    .uumMcfg( 0, N              , 1*U, O                , 2*U)                                                                                                  .heat( 100,  200);
		NH3                     = gasaciddcmp   ( 8025, "Ammonia"                                       , 114, 223, 232, 255, GASES)                                                                                                                                    .uumMcfg( 0, N              , 1*U, H                , 3*U)                                                                                                  .heat( 195,  239);
		HNO3                    = setDensity(lqudaciddcmp  ( 9825, "Nitric Acid"                                   , 128, 255,   0, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 1*U, N                , 1*U, O                , 3*U)                                                                          , 1.5).heat( 231,  356);
		NitricAcid = HNO3;
		CO                      = gaschemelec   ( 9838, "Carbon Monoxide"                               ,  10,  10,  10,  15, GASES)                                                                                                                                    .uumMcfg( 0, C              , 1*U, O                , 1*U)                                                                                                  .heat( 100,  200);
		CO2                     = gaschemelec   ( 9836, "Carbon Dioxide"                                ,  40,  40,  40,  15, GASES)                                                                                                                                    .uumMcfg( 0, C              , 1*U, O                , 2*U)                                                                                                  .heat( 100,  200);
		CO3                     = gaschemelec   ( 9843, "Carbon Trioxide"                               ,  45,  45,  45,  15, GASES, ACID)                                                                                                                              .uumMcfg( 0, C              , 1*U, O                , 3*U)                                                                                                  .heat( 100,  200);
		CH4                     = gaschemelec   ( 9832, "Methane"                                       , 250, 200, 250,  15, GASES, FLAMMABLE)                                                                                                                         .uumMcfg( 0, C              , 1*U, H                , 4*U)                                                                                                  .heat( 100,  200);
		Sugar                   = dustdcmp      ( 9703, "Sugar"                 , SET_CUBE              , 250, 250, 250, 255, FURNACE, MELTING, FLAMMABLE, BRITTLE, MORTAR, FOOD)                                                                                       .uumMcfg( 0, C              ,12*U, H                ,22*U, O                ,11*U)                                                                          .heat(459);
		Vanilla                 = dustfood      ( 9787, "Vanilla"                                       , 110,  80,  40, 255, DECOMPOSABLE)                                                                                                                             .uumMcfg( 0, C              , 8*U, H                , 8*U, O                , 3*U);
		Glycerol                = setDensity(lqudchemelec  ( 9828, "Glycerol"                                      ,   0, 180, 180, 255, LIQUID, FLAMMABLE)                                                                                                                        .uumMcfg( 0, C              , 3*U, H                , 8*U, O                , 3*U)                                                                          , 1.5).heat( 291,  563);
		Glyceryl                = setDensity(lqudchemelec  ( 9821, "Glyceryl"                                      ,   0, 150, 150, 255, LIQUID, FLAMMABLE, EXPLOSIVE)                                                                                                             .uumMcfg( 0, C              , 3*U, H                , 5*U, N                , 3*U, O                , 9*U)                                                  , 1.5).heat( 287,  323);
		SO2                     = gaschemdcmp   ( 9834, "Sulfur Dioxide"                                , 255, 200,   0, 120, GASES, "SulphurDioxide")                                                                                                                  .uumMcfg( 0, S              , 1*U, O                , 2*U)                                                                                                  .heat( 100,  200);
		SO3                     = gaschemdcmp   ( 9835, "Sulfur Trioxide"                               , 255, 220,   0, 120, GASES, "SulphurTrioxide")                                                                                                                 .uumMcfg( 0, S              , 1*U, O                , 3*U)                                                                                                  .heat( 100,  200);
		H2S                     = gasaciddcmp   ( 8024, "Hydrosulfuric Acid"                            , 241, 188, 133, 255, GASES, FLAMMABLE)                                                                                                                         .uumMcfg( 0, H              , 2*U, S                , 1*U)                                                                                                  .heat( 191,  213);
		H2SO4                   = setDensity(lqudaciddcmp  ( 9824, "Sulfuric Acid"                                 , 255, 128,   0, 255, LIQUID, "SulphuricAcid")                                                                                                                  .uumMcfg( 0, H              , 2*U, S                , 1*U, O                , 4*U)                                                                          , 1.5).heat( 200,  400);
	}
	private static void reg0016() { // upstream MT.java:1047-1094
		SulfuricAcid = H2SO4;
		H2S2O7                  = setDensity(lqudaciddcmp  ( 9844, "Disulfuric Acid"                               , 255, 150,   0, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 2*U, S                , 2*U, O                , 7*U)                                                                          , 1.5).heat( 200,  400);
		AgI                     = oredustelec   ( 8243, "Silver Iodide"         , SET_CUBE              , 240, 200, 100, 255, BRITTLE, MORTAR)                                                                                                                          .uumMcfg( 0, Ag             , 1*U, I                , 1*U)                                                                                                  .heat(831, 1779);
		SilverIodide = AgI;
		H2SiF6                  = setDensity(lqudacidelec  ( 8011, "Hexafluorosilicic Acid"                        , 190, 200, 190, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, H              , 2*U, Si               , 1*U, F                , 6*U)                                                                          , 1.5).heat( 250,  381);
		HexafluorosilicicAcid = H2SiF6;
		SiC                     = metalore      ( 8003, "Carborundum"           , SET_QUARTZ            ,  77,  77,  77     , BRITTLE, QUARTZ, DECOMPOSABLE)                                                                                                            .uumMcfg( 0, Si             , 1*U, C                , 1*U)                                                                                                  .alloyElectrolyzer(3000, 3100).qual(3, 8.0, 256, 3);
		SiO2                    = oredustdcmp   ( 8000, "Silicon Dioxide"       , SET_QUARTZ            , 200, 200, 200, 255, BRITTLE, QUARTZ, CRYSTALLISABLE, FURNACE, UNRECYCLABLE)                                                                                   .uumMcfg( 0, Si             , 1*U, O                , 2*U)                                                                                                  .heat(1986, 3220);
		SiliconDioxide = SiO2;
		Glass                   = gemcent       ( 8001, "Glass"                 , SET_GLASS             , 250, 250, 250,  35, UNRECYCLABLE, BRITTLE, MORTAR, G_GLASS, FURNACE, CRYSTAL, COOL2CRYSTAL, MELTING, EXTRUDER, EXTRUDER_SIMPLE)         .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 1.0, 1,  0).heat(1200);
		Flint                   = cent          ( 8002, "Flint"                 , SET_FLINT             ,   0,  32,  64, 255, UNRECYCLABLE, BRITTLE, MORTAR, G_GEM, STONE)                                                                                              .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 48,  1).setSmelting(SiO2, U);
		H3BO3                   = dustelec      ( 8007, "Hydrogen Borate"       , SET_FINE              , 234, 234, 255, 255, "BoricAcid")                                                                                                                              .uumMcfg( 0, H              , 3*U, B                , 1*U, O                , 3*U);
		HydrogenBorate = H3BO3;
		BoricAcid = H3BO3;
		Datolite                = oredustelec   ( 8006, "Datolite"              , SET_ROUGH             , 222, 255, 222, 255)                                                                                                                                           .uumMcfg( 0, H              , 2*U, Ca               , 2*U, B                , 2*U, Si               , 2*U, O                ,10*U);
		H2Ca2B2Si2O10 = Datolite;
		V2O5                    = oredustelec   ( 8234, "Vanadium Pentoxide"    , SET_FINE              ,  50,  50,  50, 255, WASHING_FIRESTONE, MAGNETIC_PASSIVE)                                                                                                      .uumMcfg( 0, V              , 2*U, O                , 5*U)                                                                                                  .addSourceOf(V).setSmelting(V, U7);
		VanadiumPentoxide = V2O5;
		Nb2O5                   = oredustdcmp   ( 8461, "Niobium Pentoxide"     , SET_FINE              ,  50,  64,  10, 255, WASHING_FIRESTONE)                                                                                                                        .uumMcfg( 0, Nb             , 2*U, O                , 5*U)                                                                                                  .addSourceOf(Nb);
		Ta2O5                   = oredustdcmp   ( 8462, "Tantalum Pentoxide"    , SET_FINE              ,  64,  50,  10, 255, WASHING_FIRESTONE)                                                                                                                        .uumMcfg( 0, Ta             , 2*U, O                , 5*U)                                                                                                  .addSourceOf(Ta);
		PO4                     = oredustelec   ( 8207, "Phosphate"             , SET_DULL              , 255, 255,   0, 255, FLAMMABLE, EXPLOSIVE, BRITTLE, MORTAR)                                                                                                    .uumMcfg( 0, P              , 1*U, O                , 4*U)                                                                                                  .heat( 400,  800).addSourceOf(P);
		WO3                     = oredustdcmp   ( 8026, "Tungsten Trioxide"     , SET_DULL              , 199, 211,   0, 255, MELTING, MOLTEN, INGOTS, MORTAR, WASHING_FIRESTONE, "TungstenOxide")                                                                      .uumMcfg( 0, W              , 1*U, O                , 3*U)                                                                                                  .heat(1746, 1970).addSourceOf(W);
		H2WO4                   = dustdcmp      ( 8027, "Tungstic Acid"         , SET_SHINY             , 188, 200,   0, 255, MELTING, ACID)                                                                                                                            .uumMcfg( 0, H              , 2*U, W                , 1*U, O                , 4*U)                                                                          .heat( 373, 1746).addSourceOf(W).setSmelting(WO3, 4*U7);
		Al2O3                   = oredustdcmp   ( 8008, "Alumina"               , SET_METALLIC          , 120, 195, 235, 255, MELTING, INGOTS, "NaturalAluminum")                                                                                                       .uumMcfg( 0, Al             , 2*U, O                , 3*U)                                                                                                  .heat(2345, 3250).addSourceOf(Al);
		AlF3                    = dustdcmp      ( 8010, "Aluminium Fluoride"    , SET_DULL              , 200, 190, 190, 255, MELTING, MOLTEN, INGOTS, ACID)                                                                                                            .uumMcfg( 0, Al             , 1*U, F                , 3*U)                                                                                                  .heat(1560);
		AlO3H3                  = oredustdcmp   ( 8014, "Aluminium Hydroxide"   , SET_DULL              , 190, 190, 200, 255, MELTING, "Gibbsite")                                                                                                                      .uumMcfg( 0, Al             , 1*U, O                , 3*U, H                , 3*U)                                                                          .heat( 573).addSourceOf(Al).setSmelting(Al2O3, 5*U14);
		TiO2                    = oredustdcmp   ( 9192, "Rutile"                , SET_METALLIC          , 110,  80, 120, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 1, Ti             , 1*U, O                , 2*U)                                                                                                  .heat(Ti.mMeltingPoint + 100, Ti.mBoilingPoint).qual(2).addSourceOf(Ti);
		TiCl4                   = lqudaciddcmp  ( 8413, "Titanium Tetrachloride"                        , 233, 244, 222, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Ti             , 1*U, Cl               , 4*U)                                                                                                  .heat( 249,  409);
		TitaniumTetrachloride = TiCl4;
		MnO2                    = oredustdcmp   ( 9126, "Pyrolusite"            , SET_DULL              ,  50,  50,  70, 255, MELTING, INGOTS, MORTAR, MAGNETIC_PASSIVE).setSmelting(Mn, 3*U4)                                                                          .uumMcfg( 1, Mn             , 1*U, O                , 2*U)                                                                                                  .heat( 808, 2334).addSourceOf(Mn);
		MnCl2                   = dustdcmp      ( 8031, "Manganese Chloride"    , SET_CUBE              , 255, 213, 213, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Mn             , 1*U, Cl               , 2*U)                                                                                                  .heat( 927, 1498);
		Fe2O3                   = oredustdcmp   ( 9104, "Hematite"              , SET_DULL              , 145,  90,  90, 255, MELTING, MOLTEN, MORTAR, MAGNETIC_PASSIVE, "BandedIron", "IronOxide")                                                                     .uumMcfg( 0, Fe             , 2*U, O                , 3*U)                                                                                                  .heat(2 * Fe.mMeltingPoint / 3).addSourceOf(Fe).qual(0);
	}
	private static void reg0017() { // upstream MT.java:1095-1133
		FeCl2                   = dustelec      ( 8030, "Ferrous Chloride"      , SET_CUBE              , 199, 233, 199, 255, MELTING, MOLTEN, INGOTS, MAGNETIC_PASSIVE)                                                                                                .uumMcfg( 0, Fe             , 1*U, Cl               , 2*U)                                                                                                  .heat( 950, 1296);
		FeCl3                   = dustdcmp      ( 8017, "Ferric Chloride"       , SET_METALLIC          , 180, 180, 120, 255, MELTING)                                                                                                                                  .uumMcfg( 0, Fe             , 1*U, Cl               , 3*U)                                                                                                  .heat( 580,  589);
		FeO3H3                  = dustdcmp      ( 8035, "Ferric Oxyhydroxide"   , SET_FINE              , 137,  62,  40, 255, MELTING)                                                                                                                                  .uumMcfg( 0, Fe             , 1*U, O                , 3*U, H                , 3*U)                                                                          .heat(Fe2O3.mMeltingPoint).addSourceOf(Fe).setSmelting(Fe2O3, 5*U14);
		MgCl2                   = oredustdcmp   ( 8018, "Magnesium Chloride"    , SET_CUBE              , 235, 235, 250, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Mg             , 1*U, Cl               , 2*U)                                                                                                  .heat( 987, 1685);
		MgCO3                   = oredustdcmp   ( 8016, "Magnesium Carbonate"   , SET_DULL              , 240, 230, 230, 255, MELTING, MOLTEN, INGOTS, "Magnesite")                                                                                                     .uumMcfg( 0, Mg             , 1*U, CO3              , 4*U)                                                                                                  .heat( 623, 3000);
		CaCl2                   = oredustdcmp   ( 8028, "Calcium Chloride"      , SET_CUBE              , 235, 235, 250, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Ca             , 1*U, Cl               , 2*U)                                                                                                  .heat(1048, 2208);
		CaSO4                   = dustdcmp      ( 8274, "Calcium Sulfate"       , SET_CUBE              , 240, 220, 210, 255, MORTAR, "CalciumSulphate")                                                                                                                .uumMcfg( 0, Ca             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat(1730, 3000);
		Gypsum                  = oredustdcmp   ( 9161, "Gypsum"                , SET_POWDER            , 240, 240, 240, 255, MORTAR)                                                                                                                                   .uumMcfg( 6, CaSO4          , 6*U, H2O              , 6*U)                                                                                                  .heat(1730, 3000);
		Quicklime               = oredustdcmp   ( 9271, "Quicklime"             , SET_POWDER            , 240, 230, 210, 255, MORTAR)                                                                                                                                   .uumMcfg( 0, Ca             , 1*U, O                , 1*U)                                                                                                  .heat(2886, 3120);
		CaCO3                   = oredustdcmp   ( 9107, "Calcite"               , SET_DULL              , 250, 230, 220, 255, MELTING, MOLTEN, INGOTS, MORTAR, "Valerite", "Aragonite", "Flux")                                                                         .uumMcfg( 0, Ca             , 1*U, CO3              , 4*U)                                                                                                  .heat(1612, 3000);
		CaF2                    = fluorite      ( 9215, "Fluorite"                                      , 225, 185, 140, MOLTEN, INGOTS);
		FluoriteRed             = fluorite      ( 8436, "Red Fluorite"                                  , 226,  56,  65);
		FluoritePink            = fluorite      ( 8437, "Pink Fluorite"                                 , 226, 117, 148);
		FluoriteBlue            = fluorite      ( 8438, "Blue Fluorite"                                 ,  88, 172, 180);
		FluoriteGreen           = fluorite      ( 8439, "Green Fluorite"                                ,  37, 168,  35);
		FluoriteBlack           = fluorite      ( 8440, "Black Fluorite"                                ,  48,  48,  48, MD.RH);
		FluoriteWhite           = fluorite      ( 8441, "White Fluorite"                                , 180, 180, 180);
		FluoriteYellow          = fluorite      ( 8442, "Yellow Fluorite"                               , 206, 182,  80);
		FluoriteOrange          = fluorite      ( 8443, "Orange Fluorite"                               , 255, 189,  88);
		FluoriteMagenta         = fluorite      ( 8444, "Magenta Fluorite"                              , 204,  88, 255);
		LiCl                    = setPriorityPrefix(oredustdcmp   ( 8029, "Lithium Chloride"      , SET_CUBE              , 222, 222, 250, 255, MELTING, MOLTEN, INGOTS, WASHING_MERCURY)                                                                                                 .uumMcfg( 0, Li             , 1*U, Cl               , 1*U)                                                                                                  .addSourceOf(Li), 2).heat( 880, 1655);
		LiClO3                  = dustdcmp      ( 8033, "Lithium Chlorate"      , SET_ROUGH             , 222, 233, 250, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Li             , 1*U, Cl               , 1*U, O                , 3*U)                                                                          .addSourceOf(Li).heat( 400);
		LiClO4                  = dustdcmp      ( 8034, "Lithium Perchlorate"   , SET_ROUGH             , 222, 244, 250, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Li             , 1*U, Cl               , 1*U, O                , 4*U)                                                                          .addSourceOf(Li).heat( 509,  703);
		Li2O                    = dustelec      ( 8004, "Lithium Oxide"         , SET_ROUGH             , 222, 222, 234, 255)                                                                                                                                           .uumMcfg( 0, Li             , 2*U, O                , 1*U)                                                                                                  .addSourceOf(Li);
		Li2Fe2O4                = metalore      ( 8005, "Ferrite"               , SET_METALLIC          , 120, 120, 130     , DECOMPOSABLE, ELECTROLYSER, MAGNETIC_PASSIVE)                                                                                             .uumMcfg( 0, Li             , 2*U, Fe               , 2*U, O                , 4*U)                                                                          .addSourceOf(Li,Fe);
		LiOH                    = dustelec      ( 8032, "Lithium Hydroxide"     , SET_CUBE              , 222, 202, 250, 255)                                                                                                                                           .uumMcfg( 0, Li             , 1*U, O                , 1*U, H                , 1*U)                                                                          .addSourceOf(Li).heat( 735, 1197);
		NaCl                    = setPriorityPrefix(oredustdcmp   ( 8204, "Salt"                  , SET_CUBE              , 250, 250, 250, 255, BRITTLE, MORTAR, FOOD)                                                                                                                    .uumMcfg( 0, Na             , 1*U, Cl               , 1*U)                                                                                                  .addSourceOf(Na), 2).heat(1074, 1686);
		NaNO3                   = setPriorityPrefix(oredustelec   ( 8019, "Sodium Nitrate"        , SET_FINE              , 230, 230, 230, 255, FLAMMABLE, BRITTLE, MORTAR)                                                                                                               .uumMcfg( 0, Na             , 1*U, N                , 1*U, O                , 3*U)                                                                          , 2).heat(607);
		NaOH                    = dustelec      ( 8268, "Sodium Hydroxide"      , SET_CUBE              , 220, 250, 220, 255)                                                                                                                                           .uumMcfg( 0, Na             , 1*U, O                , 1*U, H                , 1*U)                                                                          .heat( 596, 1661);
		NaHCO3                  = oredustdcmp   ( 8039, "Sodium Hydrogencarbonate", SET_FINE            , 230, 235, 240, 255, "Soda").setLocal("Soda")                                                                                                                  .uumMcfg( 0, Na             , 1*U, H                , 1*U, C                , 1*U, O                , 3*U);
		Soda = NaHCO3;
		NaHSO4                  = dustdcmp      ( 8230, "Sodium Bisulfate"      , SET_FINE              , 240, 240, 255, 255, "SodiumBisulphate", "SodiumHydrogenSulfate", "SodiumHydrogenSulphate")                                                                    .uumMcfg( 0, Na             , 1*U, H                , 1*U, S                , 1*U, O                , 4*U);
	}
	private static void reg0018() { // upstream MT.java:1134-1169
		NaSO4                   = dustdcmp      ( 9822, "Sodium Persulfate"     , SET_CUBE              , 130, 180, 250, 255, "SodiumPersulphate")                                                                                                                      .uumMcfg( 0, Na             , 1*U, S                , 1*U, O                , 4*U);
		Na2S                    = dustdcmp      ( 9823, "Sodium Sulfide"        , SET_CUBE              , 220, 220, 100, 255, "SodiumSulphide")                                                                                                                         .uumMcfg( 0, Na             , 2*U, S                , 1*U);
		Na2SO3                  = dustdcmp      ( 8269, "Sodium Sulfite"        , SET_CUBE              , 190, 190, 140, 255, "SodiumSulphite")                                                                                                                         .uumMcfg( 0, Na             , 2*U, S                , 1*U, O                , 3*U)                                                                          .heat( 306);
		Na2SO4                  = dustdcmp      ( 8270, "Sodium Sulfate"        , SET_CUBE              , 190, 190, 140, 255, "SodiumSulphate")                                                                                                                         .uumMcfg( 0, Na             , 2*U, S                , 1*U, O                , 4*U)                                                                          .heat(1157, 1702);
		Na2S2O7                 = dustdcmp      ( 8231, "Sodium Pyrosulfate"    , SET_FINE              , 240, 240, 255, 255, "SodiumPyrosulphate")                                                                                                                     .uumMcfg( 0, Na             , 2*U, S                , 2*U, O                , 7*U)                                                                          .heat( 674);
		Na2CO3                  = dustdcmp      ( 8013, "Sodium Carbonate"      , SET_FINE              , 230, 230, 230, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, Na             , 2*U, CO3              , 4*U)                                                                                                  .heat(1124);
		NaAlO2                  = dustdcmp      ( 8012, "Sodium Aluminate"      , SET_CUBE              , 230, 230, 250, 255)                                                                                                                                           .uumMcfg( 0, Na             , 1*U, Al               , 1*U, O                , 2*U)                                                                          .heat(1920);
		NaF                     = dustelec      ( 8037, "Sodium Fluoride"       , SET_CUBE              ,  64, 200, 225, 255)                                                                                                                                           .uumMcfg( 0, Na             , 1*U, F                , 1*U)                                                                                                  .heat(1266);
		Na3AlF6                 = oredustdcmp   ( 8009, "Cryolite"              , SET_DULL              , 200, 190, 190, 255, MELTING, MOLTEN, INGOTS, ACID)                                                                                                            .uumMcfg( 0, Na             , 3*U, Al               , 1*U, F                , 6*U)                                                                          .heat(1285).addSourceOf(Na,F);
		Cryolite = Na3AlF6;
		SaltWater               = setDensity(lqudelec      ( 9804, "Saltwater"                                     , 255,   0, 255, 255, LIQUID, UNRECYCLABLE, "SaltWater", "Brine")                                                                                               .uumMcfg( 0, H2O            , 3*U, NaCl             , 1*U)                                                                                                  .heat(CS.C, CS.C+90), 1.0);
		KIO3                    = oredustelec   ( 8242, "Iodine Salt"           , SET_CUBE              , 240, 200, 240, 255, BRITTLE, MORTAR)                                                                                                                          .uumMcfg( 0, K              , 1*U, I                , 1*U, O                , 3*U)                                                                          .addSourceOf(K).heat(300, 370);
		IodineSalt = KIO3;
		KCl                     = setPriorityPrefix(oredustdcmp   ( 8203, "Sylvite"               , SET_CUBE              , 240, 200, 200, 255, BRITTLE, MORTAR, "RockSalt", "Sylvine")                                                                                                   .uumMcfg( 0, K              , 1*U, Cl               , 1*U)                                                                                                  .addSourceOf(K), 2).heat(1040, 1690);
		KNO3                    = setPriorityPrefix(oredustelec   ( 8205, "Potassium Nitrate"     , SET_FINE              , 230, 230, 230, 255, FLAMMABLE, BRITTLE, MORTAR, "Saltpeter", "Nitrate", "Salpeter")                                                                           .uumMcfg( 0, K              , 1*U, N                , 1*U, O                , 3*U)                                                                          , 2).heat(607);
		KOH                     = dustelec      ( 8015, "Potassium Hydroxide"   , SET_CUBE              , 250, 220, 220, 255)                                                                                                                                           .uumMcfg( 0, K              , 1*U, O                , 1*U, H                , 1*U)                                                                          .heat( 633, 1600);
		KHSO4                   = dustdcmp      ( 8232, "Potassium Bisulfate"   , SET_FINE              , 255, 240, 240, 255, "PotassiumBisulphate")                                                                                                                    .uumMcfg( 0, K              , 1*U, H                , 1*U, S                , 1*U, O                , 4*U);
		KSO4                    = dustdcmp      ( 8022, "Potassium Persulfate"  , SET_CUBE              , 250, 180, 130, 255, "PotassiumPersulphate")                                                                                                                   .uumMcfg( 0, K              , 1*U, S                , 1*U, O                , 4*U);
		K2S                     = dustdcmp      ( 8272, "Potassium Sulfide"     , SET_CUBE              , 100, 220, 220, 255, "PotassiumSulphide")                                                                                                                      .uumMcfg( 0, K              , 2*U, S                , 1*U);
		K2SO3                   = dustdcmp      ( 8021, "Potassium Sulfite"     , SET_CUBE              , 140, 190, 190, 255, "PotassiumSulphite")                                                                                                                      .uumMcfg( 0, K              , 2*U, S                , 1*U, O                , 3*U)                                                                          .heat( 306);
		K2SO4                   = dustdcmp      ( 8271, "Potassium Sulfate"     , SET_CUBE              , 140, 190, 190, 255, "PotassiumSulphate")                                                                                                                      .uumMcfg( 0, K              , 2*U, S                , 1*U, O                , 4*U)                                                                          .heat(1342, 1962);
		K2S2O7                  = dustdcmp      ( 8233, "Potassium Pyrosulfate" , SET_FINE              , 255, 240, 240, 255, "PotassiumPyrosulphate")                                                                                                                  .uumMcfg( 0, K              , 2*U, S                , 2*U, O                , 7*U)                                                                          .heat( 598);
		K2CO3                   = dustdcmp      ( 8020, "Potassium Carbonate"   , SET_FINE              , 230, 225, 225, 255, MELTING, MOLTEN, INGOTS)                                                                                                                  .uumMcfg( 0, K              , 2*U, CO3              , 4*U)                                                                                                  .heat(1164);
		KAlO2                   = dustdcmp      ( 8023, "Potassium Aluminate"   , SET_CUBE              , 250, 230, 230, 255)                                                                                                                                           .uumMcfg( 0, K              , 1*U, Al               , 1*U, O                , 2*U)                                                                          .heat(1920);
		KF                      = dustelec      ( 8036, "Potassium Fluoride"    , SET_CUBE              , 200,  64, 225, 255)                                                                                                                                           .uumMcfg( 0, K              , 1*U, F                , 1*U)                                                                                                  .heat(1131);
		K2TaF7                  = dustdcmp      ( 8038, "Potassium Heptafluorotantalate", SET_FINE      , 164, 200, 225, 255)                                                                                                                                           .uumMcfg( 0, K              , 2*U, Ta               , 1*U, F                , 7*U);
		SaltedWater             = setDensity(lqudelec      ( 9815, "Salted Water"                                  , 255,   0, 200, 255, LIQUID, UNRECYCLABLE, "Saltedwater")                                                                                                      .uumMcfg( 0, H2O            , 3*U, KCl              , 1*U)                                                                                                  .heat(CS.C, CS.C+90), 1.0);
		ChloroauricAcid         = lqudaciddcmp  ( 8400, "Chloroauric Acid"                              , 255, 200,  70, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Au             , 1*U, Cl               , 4*U, H                , 1*U)                                                                          .heat( 200,  400);
		ChloroplatinicAcid      = lqudaciddcmp  ( 8401, "Chloroplatinic Acid"                           , 255,  70,  70, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Pt             , 1*U, Cl               , 6*U, H                , 2*U)                                                                          .heat( 200,  400);
		StannicChloride         = lqudaciddcmp  ( 8402, "Stannic Chloride"                              , 210, 250, 250, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Sn             , 1*U, Cl               , 4*U)                                                                                                  .heat( 200,  400);
		BlackVitriol            = lqudacidelec  ( 8403, "Black Vitriol"                                 ,  66,  66,  66, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Fe             , 1*U, S                , 1*U)                                                                                                  .heat( 200,  400);
		BlueVitriol             = lqudaciddcmp  ( 8404, "Blue Vitriol"                                  ,  66,  66, 222, 255, LIQUID, "RomanVitriol", "CyprusVitriol", "SolutionBlueVitriol")                                                                           .uumMcfg( 0, Cu             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
	}
	private static void reg0019() { // upstream MT.java:1170-1207
		GreenVitriol            = lqudaciddcmp  ( 8405, "Green Vitriol"                                 ,  66, 222,  66, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Fe             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		RedVitriol              = lqudaciddcmp  ( 8406, "Red Vitriol"                                   , 222,  66,  66, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Co             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		PinkVitriol             = lqudaciddcmp  ( 8407, "Pink Vitriol"                                  , 222, 111, 111, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Mg             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		CyanVitriol             = lqudaciddcmp  ( 8408, "Cyan Vitriol"                                  , 111, 222, 222, 255, LIQUID, "SolutionNickelSulfate", "SolutionNickelSulphate")                                                                                .uumMcfg( 0, Ni             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		WhiteVitriol            = lqudaciddcmp  ( 8409, "White Vitriol"                                 , 222, 222, 222, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Zn             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		GrayVitriol             = lqudaciddcmp  ( 8410, "Gray Vitriol"                                  , 111, 111, 111, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Mn             , 1*U, S                , 1*U, O                , 4*U)                                                                          .heat( 200,  400);
		MartianVitriol          = lqudaciddcmp  ( 8411, "Martian Vitriol"                               , 222,  66, 222, 255, LIQUID)                                                                                                                                   .uumMcfg(18, Fe             , 2*U, S                , 3*U, O                ,12*U)                                                                          .heat( 200,  400);
		VitriolOfClay           = lqudaciddcmp  ( 8412, "Vitriol Of Clay"                               ,  66, 222, 222, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, Al2O3          , 5*U, S                , 3*U, O                , 9*U)                                                                          .heat( 200,  400);
		UF4                     = dustdcmp      ( 9007, "Uranium Tetrafluoride"    , SET_SHARDS         ,  86, 118, 105, 255, MELTING, MOLTEN)                                                                                                                          .setMcfg( 0, U_238          , 1*U, F                , 4*U).tooltip("UF\u2084")                                                                              .heat(1309, 1690);
		UF6                     = gaschemdcmp   ( 9008, "Uranium Hexafluoride"                          ,  66,  98,  85, 255, GASES)                                                                                                                                    .setMcfg( 0, U_238          , 1*U, F                , 6*U).tooltip("UF\u2086")                                                                              .heat( 100,  329);
		U238F4                  = dustdcmp      ( 9009, "Uranium-238 Tetrafluoride", SET_SHARDS         ,  86, 118, 105, 255, MELTING, MOLTEN)                                                                                                                          .setMcfg( 0, U_238          , 1*U, F                , 4*U).tooltip("U-238F\u2084")                                                                          .heat(1309, 1690);
		U238F6                  = gaschemdcmp   ( 9010, "Uranium-238 Hexafluoride"                      ,  66,  98,  85, 255, GASES)                                                                                                                                    .setMcfg( 0, U_238          , 1*U, F                , 6*U).tooltip("U-238F\u2086")                                                                          .heat( 100,  329);
		U235F4                  = dustdcmp      ( 9011, "Uranium-235 Tetrafluoride", SET_SHARDS         ,  86, 118, 105, 255, MELTING, MOLTEN)                                                                                                                          .setMcfg( 0, U_235          , 1*U, F                , 4*U).tooltip("U-235F\u2084")                                                                          .heat(1309, 1690);
		U235F6                  = gaschemdcmp   ( 9012, "Uranium-235 Hexafluoride"                      ,  66,  98,  85, 255, GASES)                                                                                                                                    .setMcfg( 0, U_235          , 1*U, F                , 6*U).tooltip("U-235F\u2086")                                                                          .heat( 100,  329);
		AquaRegia               = lqudacidcent  ( 9827, "Aqua Regia"                                    ,  64, 255,  64, 255, LIQUID)                                                                                                                                   .uumMcfg( 0, HNO3           , 5*U, HCl              , 8*U)                                                                                                  .heat( 200,  400);
		CobaltHexahydrate       = dustcent      ( 8229, "Cobalt Hexahydrate"    , SET_ROUGH             ,  80,  80, 250, 255)                                                                                                                                           .uumMcfg( 0, Co             , 1*U, H2O              , 6*U);
		MethaneIce              = dustcent      ( 9833, "Methane Ice"           , SET_SHINY             , 225, 200, 250, 255, G_CONTAINERS, FLAMMABLE)                                                                                                                  .setMcfg( 2, CH4            , 1*U, Ice              , 2*U);
		NitroCarbon             = elec          ( 9820, "Nitro Carbon"          , SET_FLUID             ,   0,  75, 100, 255, G_CONTAINERS, EXPLOSIVE, FLAMMABLE)                                                                                                       .uumMcfg( 0, N              , 1*U, C                , 1*U);
		Lava                    = lqud          ( 9810, "Lava"                  , SET_STONE             , 255,  64,   0, 255, MELTING, GLOWING, LIGHTING)                                                                                                                                                                                                                                                                           .heat(1300, 4000);
		Biomass                 = lqudflam      ( 9840, "Biomass"                                       ,   0, 255,   0, 255, TICKS_PER_SMELT* 30, "BioMass")                                                                                                                                                                                                                                                                       .heat( 200,  400);
		BioFuel                 = lqudflam      ( 9841, "Bio Fuel"                                      , 200, 128,   0, 255, TICKS_PER_SMELT*360)                                                                                                                                                                                                                                                                                  .heat( 100,  400).setLocal("Bio Diesel");
		Ethanol                 = lqudflam      ( 9842, "Ethanol"                                       , 255, 128,   0, 255, TICKS_PER_SMELT*360)                                                                                                                                                                                                                                                                                  .heat( 100,  400);
		Oil                     = lqudflam      ( 9850, "Oil"                                           ,  10,  10,  10, 255, TICKS_PER_SMELT* 60)                                                                                                                                                                                                                                                                                  .heat( 100,  400);
		Oilsands                = oredust       ( 9851, "Oil Sand"              , SET_SAND              ,  10,  10,  10, 255, TICKS_PER_SMELT* 30, "Oilsands")                                                                                                                                                                                                                                                                      .heat( 100,  400);
		CrudeOil                = oredust       ( 9852, "Crude Oil"             , SET_DULL              ,  10,  10,  10, 255, TICKS_PER_SMELT* 60)                                                                                                                                                                                                                                                                                  .heat( 100,  400);
		Fuel                    = lqudexpl      ( 9860, "Fuel"                                          , 255, 255,   0, 255, TICKS_PER_SMELT*360, "FuelOil")                                                                                                                                                                                                                                                                       .heat( 100,  400).setLocal("Fuel Oil");
		NitroFuel               = lqudexpl      ( 9861, "Nitro-Fuel"                                    , 200, 255,   0, 255, TICKS_PER_SMELT*360)                                                                                                                      .setMcfg( 0, Glyceryl       , 1*U, Fuel             , 4*U)                                                                                                  .heat( 100,  400);
		Kerosine                = lqudexpl      ( 9862, "Kerosine"                                      ,   0,   0, 255, 255, TICKS_PER_SMELT*360)                                                                                                                                                                                                                                                                                  .heat( 100,  400);
		Diesel                  = lqudexpl      ( 9863, "Diesel"                                        , 255, 255,   0, 255, TICKS_PER_SMELT*360)                                                                                                                                                                                                                                                                                  .heat( 100,  400);
		Petrol                  = lqudexpl      ( 9864, "Petrol"                                        , 255,   0,   0, 255, TICKS_PER_SMELT*360, "Gasoline")                                                                                                                                                                                                                                                                      .heat( 100,  400);
		Propane                 = gasexpl       ( 9865, "Propane"                                       , 255,  20,  20, 255, TICKS_PER_SMELT*180)                                                                                                                                                                                                                                                                                  .heat( 100,  200);
		Butane                  = gasexpl       ( 9866, "Butane"                                        , 255,  40,  40, 255, TICKS_PER_SMELT*180)                                                                                                                                                                                                                                                                                  .heat( 100,  200);
	}
	private static void reg0020() { // upstream MT.java:1208-1245
		Propylene               = gasexpl       ( 9867, "Propylene"                                     ,  90,  60, 140, 255)                                                                                                                                                                                                                                                                                                       .heat( 100,  200);
		Ethylene                = gasexpl       ( 9868, "Ethylene"                                      ,  64,  40, 100, 255)                                                                                                                                                                                                                                                                                                       .heat( 100,  200);
		Creosote                = lqudflam      ( 9870, "Creosote"                                      , 128,  64,   0, 255, LIQUID, "Creosote Oil")                                                                                                                                                                                                                                                                               .heat( 100,  400);
		FishOil                 = lqudflam      ( 9871, "Fish Oil"                                      , 255, 196,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		WhaleOil                = lqudflam      ( 9887, "Whale Oil"                                     ,  51,  40,  23, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		SeedOil                 = lqudflam      ( 9872, "Seed Oil"                                      , 196, 255,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		HempOil                 = lqudflam      ( 9873, "Hemp Oil"                                      , 196, 255,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		LinOil                  = lqudflam      ( 9874, "Lin Oil"                                       , 196, 255,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		SunflowerOil            = lqudflam      ( 9875, "Sunflower Oil"                                 , 216, 189,  17, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		NutOil                  = lqudflam      ( 9876, "Nut Oil"                                       , 235, 173,  70, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		OliveOil                = lqudflam      ( 9877, "Olive Oil"                                     ,  63, 146,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400);
		FryingOilHot            = lqudflam      ( 9880, "FryingOilHot"                                  , 200, 196,   0, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 100,  400).setLocal("Hot Frying Oil");
		Glue                    = lqudflam      ( 9881, "Glue"                                          , 200, 196,   0, 255)                                                                                                                                                                                                                                                                                                       .heat( 150,  500);
		Lubricant               = lqud          ( 9882, "Lubricant"                                     , 255, 196,   0, 255)                                                                                                                                                                                                                                                                                                       .heat( 150,  500);
		ConstructionFoam        = lqud          ( 9883, "Construction Foam"     , SET_DULL              , 128, 128, 128, 255, DUSTS, STONE, BRITTLE, MELTING)                                                                                                                                                                                                                                                                       .heat( 500, 2000).setLocal("C-Foam");
		UUAmplifier             = lqud          ( 9884, "UU-Amplifier"                                  , 196,   0, 255, 255)                                                                                                                                                                                                                                                                                                       .heat(  50,  500);
		UUMatter                = lqud          ( 9885, "UU-Matter"                                     ,  96,   0, 128, 255, GLOWING)                                                                                                                                                                                                                                                                                              .heat(  50,  500);
		Latex                   = lqud          ( 9886, "Latex"                                         , 250, 250, 250, 255)                                                                                                                                                                                                                                                                                                       .heat( 150,  500);
		Ash                     = dust          ( 8200, "Ashes"                 , SET_DULL              , 120, 120, 120, 255, MORTAR, BRITTLE, "Ash");
		DarkAsh                 = dust          ( 8201, "Dark Ashes"            , SET_DULL              ,  50,  50,  50, 255, MORTAR, BRITTLE, "DarkAsh", "AshDark", "AshesDark");
		VolcanicAsh             = dustcent      ( 8202, "Volcanic Ashes"        , SET_FLINT             ,  60,  50,  50, 255, MORTAR, BRITTLE, "VolcanicAsh", "AshVolcanic", "AshesVolcanic")                                                                           .setMcfg( 0, Flint          , 6*U, Fe2O3            , 1*U, Mg               , 1*U);
		Chalk                   = oredustelec   ( 9112, "Chalk"                 , SET_FINE              , 250, 250, 250, 255, FURNACE, MORTAR).setSmelting(CaCO3, 2*U3)                                                                                                 .setMcfg( 0, CaCO3          , 1*U);
		Dolomite                = oredustcent   ( 9163, "Dolomite"              , SET_FLINT             , 225, 205, 205, 255, FURNACE, MORTAR).setSmelting(CaCO3, U2)                                                                                                   .setMcfg( 0, CaCO3          , 1*U, MgCO3            , 1*U);
		Asbestos                = oredustelec   ( 9103, "Asbestos"              , SET_LAPIS             , 230, 230, 230, 255, FURNACE, MORTAR, PLATES, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE, "Chrysotile")                                                        .uumMcfg( 0, Mg             , 3*U, SiO2             , 6*U, H2O              , 6*U, O                , 3*U);
		Talc                    = oredustelec   ( 9169, "Talc"                  , SET_DULL              ,  95, 145,  95, 255, "Soapstone")                                                                                                                              .uumMcfg( 0, Mg             , 3*U, SiO2             ,12*U, H2O              , 3*U, O                , 3*U);
		Pyrite                  = oredustdcmp   ( 9125, "Pyrite"                , SET_SHINY             , 255, 230,  80, 255, G_GEM_ORES, BLACKLISTED_SMELTER, MORTAR, MAGNETIC_PASSIVE)                                                                                .uumMcfg( 0, Fe             , 1*U, S                , 2*U)                                                                                                  .qual(0).addSourceOf(Fe);
		PotassiumFeldspar       = oredustelec   ( 9140, "Potassium Feldspar"    , SET_FINE              , 120,  40,  40, 255)                                                                                                                                           .uumMcfg( 0, K              , 2*U, Al2O3            , 5*U, SiO2             ,18*U, O                , 1*U);
		Biotite                 = oredustelec   ( 9141, "Biotite"               , SET_METALLIC          ,  20,  30,  20, 255)                                                                                                                                           .setMcfg( 0, K              , 2*U, Mg               , 6*U, Al2O3            ,15*U, F                , 4*U, SiO2             ,18*U)                          .addSourceOf(Ar,K,F,Al);
		Emery                   = oredust       ( 9183, "Emery"                 , SET_STONE             , 128, 128, 128, 255);
		Bark                    = dust          ( 8275, "Bark"                  , SET_ROUGH             ,  80,  40,   0, 255, TICKS_PER_SMELT/ 2, WOOD, MORTAR, FLAMMABLE, APPROXIMATE)                                                                                 .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U9).setSmelting(Ash, U4).heat(400, 500);
		Wood                    = wood          ( 8221, "Wood"                                          , 100,  50,   0, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, APPROXIMATE)                                                                                               .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U9).setSmelting(Ash, U4).qual(1, 2.0, 16, 0).heat(400, 500);
		WoodTreated             = wood          ( 8222, "WoodTreated"                                   ,  80,  40,   0, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, COATED, "WoodSealed")                                                                                      .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setAllToTheOutputOf(Wood).qual(1, 3.0, 24, 0).heat(500, 600).setLocal("Treated Wood");
	}
	private static void reg0021() { // upstream MT.java:1246-1283
		WoodPolished            = wood          ( 8267, "WoodPolished"                                  ,  60,  30,   0, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, COATED)                                                                                                    .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setAllToTheOutputOf(Wood).qual(1, 3.0, 24, 0).heat(500, 600).setLocal("Polished Wood");
		WoodRubber              = wood          ( 8224, "WoodRubber"                                    , 180, 150,   0, 255, TICKS_PER_SMELT/ 4, FLAMMABLE, APPROXIMATE)                                                                                               .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 1.5, 12, 0).heat(350, 450).setLocal("Rubber Wood");
		Bamboo                  = wood          ( 8418, "Bamboo"                                        , 100, 200, 100, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, APPROXIMATE)                                                                                               .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 2.0, 32, 0).heat(350, 450);
		Skyroot                 = wood          ( 8291, "Skyroot"                                       ,  50,  80,  80, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, APPROXIMATE)                                                                                               .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 4.0, 64, 0).heat(350, 450);
		Weedwood                = wood          ( 8286, "Weedwood"                                      ,  80,  50,   0, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, APPROXIMATE)                                                                                               .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 2.0, 32, 0).heat(350, 450);
		Livingwood              = wood          ( 8289, "Livingwood"                                    ,  60,  30,   0, 255, TICKS_PER_SMELT   , FLAMMABLE, APPROXIMATE, MAGICAL)                                                                                      .setMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 4.0, 64, 0).heat(350, 500);
		Dreamwood               = wood          ( 8290, "Dreamwood"                                     , 200, 240, 240, 255, TICKS_PER_SMELT* 2, FLAMMABLE, APPROXIMATE, MAGICAL)                                                                                      .setMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 4.0,128, 1).heat(350, 550);
		Shimmerwood             = wood          ( 8414, "Shimmerwood"                                   , 234, 234, 234, 255, TICKS_PER_SMELT* 2, FLAMMABLE, APPROXIMATE, MAGICAL)                                                                                      .setMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 4.0,128, 1).heat(350, 550);
		Greatwood               = wood          ( 8296, "Greatwood"                                     ,  60,  30,  25, 255, TICKS_PER_SMELT* 2, FLAMMABLE, APPROXIMATE, MAGICAL)                                                                                      .setMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash, U4).setSmelting(Ash, U2).qual(1, 6.0, 64, 1).heat(400, 600);
		Silverwood              = wood          ( 8297, "Silverwood"                                    , 234, 222, 210, 255, TICKS_PER_SMELT* 4, FLAMMABLE, APPROXIMATE, MAGICAL)                                                                                      .setMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  .setBurning(Ash,  0).setSmelting(Ash,  0).qual(1, 8.0,128, 1).heat(450, 650);
		Peanutwood              = steal(wood          ( 8227, "Peanut Wood"                                   , 120,  60,   0, 255, TICKS_PER_SMELT/ 2, FLAMMABLE, "Peanutwood")                                                                                              .uumMcfg( 0, C              , 6*U, H2O              ,15*U)                                                                                                  , Wood).heat(350, 450);
		Marshmallow             = wood          ( 9715, "Marshmallow"           , SET_FINE              , 255, 220, 220, 255, FOOD)                                                                                                                                                                                                                                                                                                 .qual(1, 3.0, 24, 0);
		LiveRoot                = dustcent      ( 8223, "LiveRoot"              , SET_WOOD              , 220, 200,   0, 255, TICKS_PER_SMELT   , WOOD, MORTAR, MAGICAL, MORTAR)                                                                                        .setMcfg( 3, Wood           , 3*U, Ma               , 1*U)                                                                                                  .setLocal("Liveroot").setBurning(Ash, U9).heat(1178, 2465);
		PetrifiedWood           = create        ( 8277, "Petrified Wood"        , SET_WOOD              , 110,  50,  35, 255, TICKS_PER_SMELT/ 4, G_STONE, STONE, WOOD, MORTAR, FLAMMABLE)                                                                              .setMcfg( 0, Wood           , 1*U)                                                                                                                          .qual(1, 2.0, 24, 1).heat(350, 450);
		Wax                     = wax           ( 8235, "Wax"                                           , 250, 250, 250, 255)                                                                                                                                                                                                                                                                                                       .heat( 350);
		WaxBee                  = wax           ( 8236, "WaxBee"                                        , 250, 220, 110, 255, FOOD, "BeesWax", "Beeswax", "BeeWax", "Beewax")                                                                                                                                                                                                                                                       .heat( 350).setLocal("Bees Wax");
		WaxRefractory           = wax           ( 8237, "WaxRefractory"                                 , 250,  50,  50, 255, UNBURNABLE, "RefractoryWax", "Refractorywax")                                                                                                                                                                                                                                                         .heat(2600).setLocal("Refractory Wax");
		WaxParaffin             = wax           ( 8238, "WaxParaffin"                                   , 210, 210, 250, 255, "ParaffinWax", "Paraffinwax")                                                                                                                                                                                                                                                                         .heat( 400).setLocal("Paraffin Wax");
		WaxPlant                = wax           ( 8239, "WaxPlant"                                      , 210, 250, 210, 255, FOOD)                                                                                                                                                                                                                                                                                                 .heat( 350).setLocal("Plant Wax");
		WaxMagic                = wax           ( 8240, "WaxMagic"                                      , 200,  80, 200, 255, MAGICAL)                                                                                                                                                                                                                                                                                              .heat( 350).setLocal("Magic Wax");
		WaxAmnesic              = wax           ( 8280, "WaxAmnesic"                                    , 180,  70, 250, 255, MAGICAL)                                                                                                                                                                                                                                                                                              .heat( 350).setLocal("Amnesic Wax");
		WaxSoulful              = wax           ( 8281, "WaxSoulful"                                    ,  90,  40,  10, 255, MAGICAL)                                                                                                                                                                                                                                                                                              .heat( 350).setLocal("Soulful Wax");
		Basalz                  = blaze         ( 8247, "Basalz"                                        , 100,  81,  81     , MAGNETIC_ACTIVE, AUTO_COLLECTING);
		Blitz                   = blaze         ( 8248, "Blitz"                                         , 250, 219,   0     );
		Blizz                   = blaze         ( 8210, "Blizz"                                         ,  33, 200, 234     );
		Blaze                   = blaze         ( 8211, "Blaze"                                         , 255, 200,   0     , UNBURNABLE, BURNING, MELTING, TICKS_PER_SMELT*24)                                                                                                                                                                                                                                                     .heat(4000);
		Breeze                  = blaze         ( 8471, "Breeze"                                        , 170, 155, 203     );
		Ceramic                 = dustelec      ( 8225, "Ceramic"               , SET_ROUGH             , 220, 130,  70, 255, MORTAR, PLATES, BRITTLE)                                                                                                                  .uumMcfg(18, Al2O3          , 5*U, SiO2             ,12*U)                                                                                                  .heat(2000).setCompressing(null, 0).setBending(null, 0).setForging(null, 0).setSmashing(null, 0);
		Brick                   = steal(create        ( 9243, "Clay Brick"            , SET_ROUGH             , 183,  90,  64, 255, MORTAR, BRITTLE, "Brick")                                                                                                                 .uumMcfg( 1, Ceramic        , 1*U)                                                                                                                          .heat(2000), Ceramic).setAllToTheOutputOf(Ceramic);
		Clay                    = oredustdcmp   ( 8215, "Clay"                  , SET_ROUGH             , 200, 200, 220, 255, MORTAR, PLATES)                                                                                                                           .uumMcfg( 2, Ceramic        , 2*U, H2O              , 1*U)                                                                                                  .heat(2000).setSmelting(Ceramic, U);
		ClayBrown               = clay          ( 8276, "ClayBrown"                                     , 230, 140,  75, LiOH )                                                                                                                                                                                                                                                                                                     .setLocal("Brown Clay");
		ClayRed                 = clay          ( 8455, "ClayRed"                                       , 230,  40,  25, KOH  )                                                                                                                                                                                                                                                                                                     .setLocal("Red Clay");
	}
	private static void reg0022() { // upstream MT.java:1284-1319
		Bentonite               = clay          ( 9153, "Bentonite"                                     , 255, 192,   4, NaOH )                                                                                                                                                                                                                                                                                                     .setLocal("Bentonite Clay");
		Palygorskite            = clay          ( 9154, "Palygorskite"                                  , 114, 157, 179, Mg   , "FullersEarth")                                                                                                                                                                                                                                                                                     .setLocal("Palygorskite Clay");
		Kaolinite               = clay          ( 9167, "Kaolinite"                                     , 245, 235, 235, Ca   )                                                                                                                                                                                                                                                                                                     .setLocal("Kaolinite Clay");
		Porcelain               = mixdust       ( 8273, "Porcelain"             , SET_FINE              , 235, 235, 245, 255, PLATES, BRITTLE, MORTAR)                                                                                                                  .uumMcfg( 0, Ceramic        , 2*U, SiO2             , 1*U, PotassiumFeldspar, 1*U)                                                                          .heat(1800).setCompressing(null, 0).setBending(null, 0).setForging(null, 0).setSmashing(null, 0);
		Graphite                = oredustdcmp   ( 9174, "Graphite"              , SET_DULL              , 128, 128, 128, 255, BLACKLISTED_SMELTER, BRITTLE, MORTAR, STICKS)                                                                                             .uumMcfg( 0, C              , 1*U)                                                                                                                          .qual(1, 5.0, 32, 2).setSmelting(C, U2).setBurning(Ash, U4).heat(1700, C.mBoilingPoint);
		Niter                   = oredustcent   ( 8206, "Niter"                 , SET_FLINT             , 255, 200, 200, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, CRYSTAL, "Nitre")                                                                                 .uumMcfg( 0, KNO3           , 1*U, NaNO3            , 1*U)                                                                                                  .addSourceOf(Na,K).heat(607);
		Phosphorus              = cent          ( 8208, "Phosphorus"            , SET_FLINT             , 255, 255,   0, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, EXPLOSIVE, "Phosphorous")                                                                         .uumMcfg( 0, Ca             , 3*U, PO4              , 2*U)                                                                                                  .addSourceOf(P);
		PhosphorusBlue          = cent          ( 8458, "Blue Phosphorus"       , SET_FLINT             , 155, 227, 228, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, EXPLOSIVE)                                                                                        .uumMcfg( 0, Ca             , 3*U, PO4              , 2*U)                                                                                                  .addSourceOf(P);
		PhosphorusRed           = cent          ( 8459, "Red Phosphorus"        , SET_FLINT             , 119,   4,  14, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, EXPLOSIVE)                                                                                        .uumMcfg( 0, Ca             , 3*U, PO4              , 2*U)                                                                                                  .addSourceOf(P);
		PhosphorusWhite         = cent          ( 8460, "White Phosphorus"      , SET_FLINT             , 236, 234, 221, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, EXPLOSIVE)                                                                                        .uumMcfg( 0, Ca             , 3*U, PO4              , 2*U)                                                                                                  .addSourceOf(P);
		Apatite                 = elec          ( 8209, "Apatite"               , SET_DIAMOND           , 120, 180, 250, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, CRYSTAL, CRYSTALLISABLE)                                                                          .uumMcfg( 0, Ca             , 5*U, PO4              , 3*U, Cl               , 1*U)                                                                          .addSourceOf(P);
		Phosphorite             = elec          ( 8226, "Phosphorite"           , SET_DIAMOND           ,  50,  50,  65, 255, G_GEM_ORES, FLAMMABLE, BRITTLE, MORTAR, CRYSTAL, CRYSTALLISABLE)                                                                          .uumMcfg( 0, Ca             , 5*U, PO4              , 3*U, F                , 1*U)                                                                          .addSourceOf(P,F);
		Paper                   = dust          ( 8216, "Paper"                 , SET_PAPER             , 250, 250, 250, 255, TICKS_PER_SMELT/ 8, MULTIPLATES, MORTAR)                                                                                                                                                                                                                                                              .setBurning(Ash, U9);
		Rubber                  = create        ( 8217, "Rubber"                , SET_RUBBER            ,  20,  20,  20, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, STRETCHY, FURNACE)                             .uumMcfg( 0, C              , 5*U, H                , 8*U)                                                                                                  .heat(410).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 0);
		Plastic                 = create        ( 8218, "Plastic"               , SET_DULL              , 200, 200, 200, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                              .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 1);
		Teflon                  = create        ( 8196, "Teflon"                , SET_DULL              ,  80,  80,  80, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE, "Polymer", "PTFE")           .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 1);
		PTFE = Teflon;
		PVC                     = create        ( 8197, "PVC"                   , SET_DULL              , 250, 250,  50, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                              .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 1);
		Bakelite                = create        ( 8198, "Bakelite"              , SET_DULL              , 201,  57,  64, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE)                              .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 1);
		Polycarbonate           = create        ( 8199, "Hard Plastic"          , SET_DULL              , 180, 180, 180, 255, G_INGOT_MACHINE, APPROXIMATE, FLAMMABLE, EXTRUDER, EXTRUDER_SIMPLE, WIRES, MORTAR, BOUNCY, BRITTLE, FURNACE, "Polycarbonate")             .uumMcfg( 0, C              , 1*U, H                , 2*U)                                                                                                  .heat(423).setBurning(Ash, U9).setSmelting(null, 2*U3).qual(1, 3.0, 256, 1);
		Bone                    = oredustelec   ( 8219, "Bone"                  , SET_DULL              , 250, 250, 250, 255, MORTAR, "Fossil")                                                                                                                         .uumMcfg( 8, Ca             , 1*U)                                                                                                                          .qual(1, 4.0,  64, 1);
		BoneWither = Bone;
		SlimyBone               = gem           ( 8287, "Slimy Bone"            , SET_DULL              , 230, 250, 230, 255, MORTAR)                                                                                                                                   .uumMcfg( 8, Ca             , 1*U)                                                                                                                          .qual(1, 5.0, 128, 1);
		Gunpowder               = dust          ( 8220, "Gunpowder"             , SET_DULL              , 128, 128, 128, 255, EXPLOSIVE, FLAMMABLE)                                                                                                                     .uumMcfg( 4, C              , 2*U, S                , 1*U, NaNO3            , 1*U)                                                                          .setBurning(Ash, U9);
		Dynamite                = dust          ( 8249, "Dynamite"              , SET_ROUGH             , 111, 131, 111, 255, EXPLOSIVE, FLAMMABLE)                                                                                                                     .uumMcfg( 0, Glyceryl       , 1*U, Wood             , 1*U)                                                                                                  .setBurning(Ash, U9);
		Asphalt                 = dust          ( 8266, "Asphalt"               , SET_ROUGH             ,  88,  88,  99, 255, FURNACE, MELTING, EXTRUDER, EXTRUDER_SIMPLE, MOLTEN);
		Tallow                  = dust          ( 8244, "Tallow"                , SET_FOOD              , 220, 200, 100, 255, MELTING, MOLTEN)                                                                                                                                                                                                                                                                                      .heat(350).setLocal("Magic Tallow");
		Leather                 = create        ( 8241, "Leather"               , SET_ROUGH             , 141,  65,  37, 255, FLAMMABLE)                                                                                                                                                                                                                                                                                            .setBurning(Ash, U9).setSmelting(Ash, U9);
		Indigo                  = dust          ( 8228, "Indigo"                , SET_LEAF              , 255, 128, 255, 255, FLAMMABLE);
		MeatCooked              = meat          ( 9701, "MeatCooked"                                    , 150,  60,  20, 255, "Meat")                                                                                                                                                                                                                                                                                               .heat(477, 550).setLocal("Cooked Meat");
		MeatRaw                 = meat          ( 9700, "MeatRaw"                                       , 255, 100, 100, 255, FURNACE)                                                                                                                                                                                                                                                                                              .heat(477, 550).setLocal("Raw Meat").setSmelting(MeatCooked, U).setForging(MeatCooked, U);
		MeatRotten              = meat          ( 9710, "MeatRotten"                                    , 255, 150, 100, 255, ROTTEN)                                                                                                                                                                                                                                                                                               .heat(477, 550).setLocal("Rotten Meat");
	}
	private static void reg0023() { // upstream MT.java:1320-1351
		FishCooked              = meat          ( 9711, "FishCooked"                                    , 150, 120,  20, 255)                                                                                                                                                                                                                                                                                                       .heat(477, 550).setLocal("Cooked Fishmeal");
		FishRaw                 = meat          ( 9712, "FishRaw"                                       , 255, 150, 100, 255, FURNACE)                                                                                                                                                                                                                                                                                              .heat(477, 550).setLocal("Raw Fishmeal").setSmelting(FishCooked, U).setForging(FishCooked, U);
		FishRotten              = meat          ( 9713, "FishRotten"                                    , 220, 200, 100, 255, ROTTEN)                                                                                                                                                                                                                                                                                               .heat(477, 550).setLocal("Rotten Fishmeal");
		Wheat                   = grain         ( 9702, "Wheat"                                         , 255, 255, 196, 255, "Flour");
		Barley                  = grain         ( 9704, "Barley"                                        , 196, 255, 196, 255);
		Rye                     = grain         ( 9705, "Rye"                                           , 255, 230, 180, 255);
		Rice                    = grain         ( 9706, "Rice"                                          , 252, 252, 240, 255);
		Oat                     = grain         ( 9707, "Oat"                                           , 240, 240, 222, 255, "Oats");
		OatAbyssal              = grain         ( 9719, "Abyssal Oat"                                   , 133,  62,  25, 255, "AbyssalOats");
		Corn                    = grain         ( 9708, "Corn"                                          , 250, 240, 111, 255);
		Potato                  = dustfood      ( 9709, "Potato"                , SET_POWDER            , 240, 240, 164, 255)                                                                                                                                                                                                                                                                                                       .setBurning(Ash, U9);
		Tofu                    = dustfood      ( 9778, "Tofu"                  , SET_FOOD              , 222, 222, 222, 255, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE)                                                                                                                                                                                                                                                           .heat(422, 500);
		SoylentGreen            = dustfood      ( 9779, "Soylent Green"         , SET_FOOD              ,   0, 222,   0, 255, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE)                                                                                                                                                                                                                                                           .heat(422, 500).setLocal("Emerald Green");
		Cheese                  = orefood       ( 9780, "Cheese"                                        , 255, 234,   0, 255, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE, FURNACE)                                                                                                                                                                                                                                                  .heat(320, 500);
		Chili                   = dustfood      ( 9781, "Chili"                                         , 200,   0,   0, 255);
		Cocoa                   = dustfood      ( 9782, "Cocoa"                                         , 190,  95,   0, 255);
		Chocolate               = mixfood       ( 9783, "Chocolate"                                     , 100,  50,   0, 255, FURNACE, INGOTS, MELTING, EXTRUDER, EXTRUDER_SIMPLE)                                                                                      .setMcfg( 0, Cocoa          , 1*U, Sugar            , 1*U)                                                                                                  .heat(CS.C+40, 400);
		Coffee                  = dustfood      ( 9784, "Coffee"                                        , 150,  75,   0, 255, "CoffeeDust");
		Cinnamon                = dustfood      ( 9785, "Cinnamon"                                      , 122,  83,  53, 255, TICKS_PER_SMELT/ 2);
		Nutmeg                  = dustfood      ( 9786, "Nutmeg"                                        , 240, 220, 180, 255, TICKS_PER_SMELT/ 2);
		Peanut                  = dustfood      ( 9795, "Peanut"                                        , 240, 210, 160, 255, TICKS_PER_SMELT/ 2);
		Hazelnut                = dustfood      ( 9796, "Hazelnut"                                      , 240, 200, 140, 255, TICKS_PER_SMELT/ 2);
		Pistachio               = dustfood      ( 9797, "Pistachio"                                     , 200, 250, 140, 255, TICKS_PER_SMELT/ 2);
		Almond                  = dustfood      ( 9714, "Almond"                                        , 200, 160, 140, 255, TICKS_PER_SMELT/ 2);
		PEZ                     = orefood       ( 9716, "PEZ"                                           , 210, 210, 210, 255)                                                                                                                                                                                                                                                                                                       .qual(2,  8.0,  512, 3);
		Licorice                = orefood       ( 9717, "Licorice"                                      ,  30,  30,  30, 255)                                                                                                                                                                                                                                                                                                       .qual(2,  6.0,  128, 2);
		Nougat                  = orefood       ( 9718, "Nougat"                                        , 240, 200, 150, 255);
		PepperBlack             = dustfood      ( 9788, "PepperBlack"                                   ,  40,  40,  40, 255, "Pepper")                                                                                                                                                                                                                                                                                             .setLocal("Black Pepper");
		Curry                   = dustfood      ( 9789, "Curry"                                         , 240, 190, 100, 255);
		Milk                    = food          ( 9790, "Milk"                  , SET_FINE              , 254, 254, 254, 255, G_CONTAINERS, DUSTS)                                                                                                                                                                                                                                                                                  .heat(CS.C, CS.C+100);
		Butter                  = food          ( 9798, "Butter"                                        , 230, 230, 100, 255, INGOTS, MELTING)                                                                                                                          .setMcfg( 1, Milk           , 1*U)                                                                                                                          .heat(CS.C+40, 500);
		ButterSalted            = food          ( 9799, "Salted Butter"                                 , 230, 230, 110, 255, INGOTS, MELTING)                                                                                                                          .setMcfg( 1, Milk           , 1*U, NaCl             , 1*U)                                                                                                  .heat(CS.C+40, 500);
	}
	private static void reg0024() { // upstream MT.java:1352-1393
		Honey                   = orefood       ( 9791, "Honey"                                         , 250, 200,   0, 255, (Object[])G_CONTAINERS)                                                                                                                                                                                                                                                                               .heat(CS.C, CS.C+100);
		Honeydew                = orefood       ( 9793, "Honeydew"                                      , 210, 100,   0, 255, (Object[])G_CONTAINERS)                                                                                                                                                                                                                                                                               .heat(CS.C, CS.C+100);
		Tea                     = dustfood      ( 9792, "Tea"                                           , 100, 250, 100, 255);
		Mint                    = dustfood      ( 9794, "Mint"                                          , 150, 250, 150, 255);
		Diamond                 = setDensity(diamond       ( 8300, "Diamond"                                       , 200, 255, 255, DYE_INDEX_White      )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondBlue             = setDensity(diamond       ( 8464, "Blue Diamond"                                  ,  30,  60, 240, DYE_INDEX_Blue       )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondGreen            = setDensity(diamond       ( 8465, "Green Diamond"                                 ,   0, 240,   0, DYE_INDEX_Green      )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondPurple           = setDensity(diamond       ( 8466, "Purple Diamond"                                , 120,   0, 240, DYE_INDEX_Purple     )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondRed              = setDensity(diamond       ( 8467, "Red Diamond"                                   , 240,   0,   0, DYE_INDEX_Red        )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondYellow           = setDensity(diamond       ( 8468, "Yellow Diamond"                                , 240, 240,   0, DYE_INDEX_Yellow     )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                                                     , 3.53).heat(4200, C.mBoilingPoint);
		DiamondPink             = setDensity(diamond       ( 8424, "Pink Diamond"                                  , 240, 160, 160, DYE_INDEX_Pink       )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                          , 3.53).heat(4200, C.mBoilingPoint).qual(3, 12.0,  1600,  3);
		DiamondIndustrial       = setDensity(diamond       ( 8423, "DiamondIndustrial"                             , 255, 255, 210, DYE_INDEX_Yellow     )                                                                                                                         .uumMcfg( 1, C              , 4*U)                                                                                                                          , 3.53).heat(4200, C.mBoilingPoint).qual(3,  9.0,  1440,  3).setLocal("Industrial Diamond");
		ManaDiamond             = setDensity(diamond       ( 8278, "Mana Diamond"                                  , 128, 255, 255, DYE_INDEX_Cyan       , MAGICAL, UNBURNABLE)                                                                                                    .setMcfg( 1, C              , 4*U, Ma               , 1*U)                                                                                                  , 3.53).heat(4200, C.mBoilingPoint).qual(3, 10.0,  1280,  3);
		ElvenDragonstone        = setDensity(diamond       ( 8279, "Elven Dragonstone"                             , 240, 140, 240, DYE_INDEX_Magenta    , MAGICAL, UNBURNABLE)                                                                                                    .setMcfg( 1, C              , 4*U, Ma               , 2*U)                                                                                                  , 3.53).heat(4200, C.mBoilingPoint).qual(3, 12.0,  1280,  3).setLocal("Dragonstone");
		Gravitite               = setDensity(diamond       ( 8294, "Gravitite"                                     , 182,  91, 159, DYE_INDEX_Magenta    , MAGICAL, CRYSTALLISABLE)                                                                                                .setMcfg( 1, C              , 4*U, Gt               , 1*U, Ma               , 1*U)                                                                          , 3.53).heat(4200, C.mBoilingPoint).qual(3,  9.0,  1280,  3);
		Emerald                 = emerald       ( 8301, "Emerald"                                       ,  80, 255,  80, DYE_INDEX_Green      )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Aquamarine              = emerald       ( 8323, "Aquamarine"                                    , 200, 220, 255, DYE_INDEX_Cyan       )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Morganite               = emerald       ( 8324, "Morganite"                                     , 255, 200, 200, DYE_INDEX_Pink       )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Heliodor                = emerald       ( 8384, "Heliodor"                                      , 255, 255, 150, DYE_INDEX_Yellow     )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Goshenite               = emerald       ( 8385, "Goshenite"                                     , 240, 240, 240, DYE_INDEX_White      )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Bixbite                 = emerald       ( 8386, "Bixbite"                                       , 255,  80,  80, DYE_INDEX_Red        , "ScarletEmerald")                                                                                                       .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Maxixe                  = emerald       ( 8387, "Maxixe"                                        ,  80,  80, 255, DYE_INDEX_Blue       )                                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Be               , 3*U, SiO2             ,18*U, O                , 3*U);
		Sapphire                = sapphire      ( 8304, "Sapphire"                                      , 120, 120, 160, DYE_INDEX_Blue       , "Saphire")                                                                                                              .uumMcfg( 6, Al2O3          , 5*U);
		Ruby                    = sapphire      ( 8302, "Ruby"                                          , 255, 100, 100, DYE_INDEX_Red        )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, Cr               , 1*U);
		BlueSapphire            = sapphire      ( 8328, "Blue Sapphire"                                 , 100, 100, 200, DYE_INDEX_Blue       )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, Fe               , 1*U);
		GreenSapphire           = sapphire      ( 8305, "Green Sapphire"                                , 100, 200, 130, DYE_INDEX_Green      )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, Mg               , 1*U);
		PurpleSapphire          = sapphire      ( 8383, "Purple Sapphire"                               , 220,  50, 255, DYE_INDEX_Purple     )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, V                , 1*U);
		YellowSapphire          = sapphire      ( 8315, "Yellow Sapphire"                               , 220, 220,  50, DYE_INDEX_Yellow     )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, TiO2             , 1*U);
		OrangeSapphire          = sapphire      ( 8314, "Orange Sapphire"                               , 220, 150,  50, DYE_INDEX_Orange     )                                                                                                                         .uumMcfg( 6, Al2O3          , 5*U, Cu               , 1*U);
		Spinel                  = valgemelec    ( 8326, "Spinel"                , SET_GEM_VERTICAL      ,   0, 100,   0, 127, RANDOM_SMALL_GEM_ORE)                                                                                               .uumMcfg( 0, Al2O3          , 5*U, Mg               , 1*U, O                , 1*U)                                                                          .qual(3,  7.0,   384,  2);
		BalasRuby               = steal(valgemelec    ( 8303, "Balas Ruby"            , SET_GEM_VERTICAL      , 255, 100, 100, 127, RANDOM_SMALL_GEM_ORE, "FoolsRuby").setLocal("Ruby"), Ruby)                                                     .uumMcfg( 0, Cr             , 2*U, Mg               , 1*U, O                , 4*U)                                                                          .qual(3,  7.0,   384,  2);
		Almandine               = garnet        ( 9101, "Almandine"                                     , 255,   0,   0, DYE_INDEX_Red        , "GarnetRed")                                                                                                            .uumMcfg( 0, Al2O3          , 5*U, Fe               , 3*U, SiO2             , 9*U, O                , 3*U);
	}
	private static void reg0025() { // upstream MT.java:1394-1433
		Grossular               = garnet        ( 9119, "Grossular"                                     , 200, 100,   0, DYE_INDEX_Orange     , "GarnetOrange")                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Ca               , 3*U, SiO2             , 9*U, O                , 3*U);
		Pyrope                  = garnet        ( 9127, "Pyrope"                                        , 120,  50, 100, DYE_INDEX_Purple     , "GarnetPurple")                                                                                                         .uumMcfg( 0, Al2O3          , 5*U, Mg               , 3*U, SiO2             , 9*U, O                , 3*U);
		Spessartine             = garnet        ( 9129, "Spessartine"                                   , 255, 100, 100, DYE_INDEX_Red        , "Garnet")                                                                                                               .uumMcfg( 0, Al2O3          , 5*U, Mn               , 3*U, SiO2             , 9*U, O                , 3*U);
		Andradite               = garnet        ( 9102, "Andradite"                                     , 150, 120,   0, DYE_INDEX_Yellow     , "GarnetYellow")                                                                                                         .uumMcfg( 0, Ca             , 3*U, Fe               , 2*U, SiO2             , 9*U, O                , 6*U);
		Uvarovite               = garnet        ( 9135, "Uvarovite"                                     , 180, 255, 180, DYE_INDEX_Lime       , "GarnetGreen")                                                                                                          .uumMcfg( 0, Ca             , 3*U, Cr               , 2*U, SiO2             , 9*U, O                , 6*U);
		Jasper                  = jasper        ( 8309, "Red Jasper"                                    , 200,  80,  80, DYE_INDEX_Red        , "Jasper");
		JasperOcean             = jasper        ( 8425, "Ocean Jasper"                                  , 139, 115,  86, DYE_INDEX_Orange     );
		JasperRainforest        = jasper        ( 8426, "Rainforest Jasper"                             , 129, 123,  55, DYE_INDEX_Yellow     );
		JasperBlue              = jasper        ( 8427, "Blue Jasper"                                   ,  60, 124, 151, DYE_INDEX_Blue       );
		JasperGreen             = jasper        ( 8428, "Green Jasper"                                  ,  91, 131, 108, DYE_INDEX_Green      );
		JasperYellow            = jasper        ( 8429, "Yellow Jasper"                                 , 156, 128,  39, DYE_INDEX_Yellow     );
		TigerEyeYellow          = tigereye      ( 8430, "Tiger Eye"                                     , 142, 116,  61, DYE_INDEX_Yellow     , "YellowTigerEye");
		TigerEyeGreen           = tigereye      ( 8431, "Cat's Eye"                                     ,  77, 116,  81, DYE_INDEX_Green      , "GreenTigerEye" );
		TigerEyeRed             = tigereye      ( 8432, "Dragon Eye"                                    , 164,  95,  83, DYE_INDEX_Red        , "RedTigerEye"   );
		TigerEyeBlue            = tigereye      ( 8433, "Hawk's Eye"                                    ,  76,  94, 109, DYE_INDEX_Blue       , "BlueTigerEye"  );
		TigerEyeBlack           = tigereye      ( 8434, "Black Eye"                                     ,  66,  68,  66, DYE_INDEX_Black      , "BlackTigerEye" );
		TigerIron               = tigereye      ( 8435, "Tiger Iron"                                    , 106,  86,  66, DYE_INDEX_Yellow     );
		AventurineGreen         = aventurine    ( 8448, "Green Aventurine"                              ,  66, 189, 133, DYE_INDEX_Green      , "Aventurine");
		AventurineBrown         = aventurine    ( 8446, "Brown Aventurine"                              , 185, 110,  35, DYE_INDEX_Brown      );
		AventurineYellow        = aventurine    ( 8447, "Yellow Aventurine"                             , 200, 179, 121, DYE_INDEX_Yellow     );
		AventurineBlack         = aventurine    ( 8449, "Black Aventurine"                              ,  50,  50,  50, DYE_INDEX_Black      );
		AventurineBlue          = aventurine    ( 8450, "Blue Aventurine"                               ,  67, 103, 138, DYE_INDEX_Blue       );
		AventurineRed           = aventurine    ( 8451, "Red Aventurine"                                , 116,  46,  33, DYE_INDEX_Red        );
		Topaz                   = valgemelec    ( 8306, "Topaz"                 , SET_GEM_HORIZONTAL    , 255, 128,   0, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, Al2O3          , 5*U, SiO2             , 3*U, F                , 2*U, H2O              , 3*U)                                                  .qual(3,  7.0,   256,  3);
		BlueTopaz               = valgemelec    ( 8307, "Blue Topaz"            , SET_GEM_HORIZONTAL    , 123, 150, 220, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, Al2O3          , 5*U, SiO2             , 3*U, F                , 2*U, H2O              , 3*U)                                                  .qual(3,  7.0,   256,  3).setGenerifying(Topaz);
		Tanzanite               = valgemelec    ( 8308, "Tanzanite"             , SET_GEM_HORIZONTAL    ,  64,   0, 200, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, Al2O3          ,15*U, SiO2             ,18*U, Ca               , 4*U, H2O              , 3*U, O                , 4*U)                          .qual(3,  7.0,   256,  2);
		Zanite                  = valgemelec    ( 8292, "Zanite"                , SET_REDSTONE          , 146,  73, 255, 127, CRYSTALLISABLE                                      )                                                         .uumMcfg( 0, Al2O3          ,15*U, SiO2             ,18*U, Ca               , 4*U, H2O              , 3*U, O                , 4*U)                          .qual(3, 16.0,   512,  2).setGenerifying(Tanzanite);
		Amazonite               = valgemelec    ( 8417, "Amazonite"             , SET_LAPIS             ,  71, 241, 170, 255, RANDOM_SMALL_GEM_ORE, MD.VOLTZ                      )                                                         .uumMcfg( 0, Al2O3          , 5*U, SiO2             ,18*U, K                , 2*U, O                , 1*U)                                                  .qual(3,  7.0,   256,  2);
		Alexandrite             = valgemelec    ( 8388, "Alexandrite"           , SET_OPAL              , 200, 255, 170, 127                          )                                                         .uumMcfg( 0, Al2O3          , 1*U, Be               , 1*U, O                , 1*U)                                                                          .qual(3,  7.0,   512,  3);
		Opal                    = valgemelec    ( 8312, "Opal"                  , SET_OPAL              ,   0,   0, 255, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(3,  7.0,   256,  2);
		OnyxRed                 = valgemelec    ( 8415, "OnyxRed"               , SET_LAPIS             , 255,  88,  77, 255, RANDOM_SMALL_GEM_ORE, MD.VOLTZ                      )                                                         .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(3,  7.0,   256,  2).setLocal("Red Onyx");
		OnyxBlack               = valgemelec    ( 8416, "OnyxBlack"             , SET_LAPIS             ,  50,  50,  50, 255, RANDOM_SMALL_GEM_ORE, MD.VOLTZ, "Onyx"              )                                                         .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(3,  7.0,   256,  2).setLocal("Black Onyx");
	}
	private static void reg0026() { // upstream MT.java:1434-1511
		Sugilite                = valgemelec    ( 8463, "Sugilite"              , SET_LAPIS             , 153, 100, 196, 255                                                      )                                                         .uumMcfg( 0, K              , 1*U, Na               , 2*U, MnO2             , 2*U, Li               , 3*U, SiO2             ,36*U, O                , 2*U)  .qual(3,  9.0,   512,  3);
		Peridot                 = valgemelec    ( 8311, "Peridot"               , SET_RUBY              , 150, 255, 150, 127, RANDOM_SMALL_GEM_ORE, "Olivine"                     )                                                         .uumMcfg( 0, SiO2           , 2*U, Fe               , 1*U, Mg               , 2*U)                                                                          .qual(3,  7.0,   256,  2);
		Amethyst                = valgemelec    ( 8313, "Amethyst"              , SET_RUBY              , 166, 120, 241, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, SiO2           , 4*U, Fe               , 1*U)                                                                                                  .qual(3,  6.0,   128,  3);
		Dioptase                = valgemelec    ( 8325, "Dioptase"              , SET_EMERALD           ,   0, 180, 180, 127, RANDOM_SMALL_GEM_ORE                                )                                                         .uumMcfg( 0, SiO2           , 3*U, Cu               , 1*U, O                , 1*U, H2O              , 3*U)                                                  .qual(3,  7.0,   256,  2);
		Carminite               = valgem        ( 8470, "Carminite"             , SET_FINE              , 150,   0,   0, 255                      , MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                                                 .qual(3,  7.0,   384,  3);
		Amber                   = valgem        ( 8310, "Amber"                 , SET_RUBY              , 255, 180,   0, 127                      , MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                     .qual(3,  4.0,   256,  3).heat(473);
		AmberGolden             = valgem        ( 8469, "Golden Amber"          , SET_RUBY              , 255, 230,  80, 127                      , MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                     .qual(3,  5.0,   384,  3).heat(473).setGenerifying(Amber);
		AmberDominican          = valgem        ( 8422, "Dominican Amber"       , SET_RUBY              ,  80,  80, 240, 127                      , MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                     .qual(3,  5.0,   384,  3).heat(473).setGenerifying(Amber).setLocal("Blue Amber");
		Craponite               = valgem        ( 8322, "Craponite"             , SET_FLINT             , 255, 170, 185, 127                      , MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                     .qual(3,  7.0,   256,  2);
		Jade                    = valgem        ( 8321, "Jade"                  , SET_LAPIS             , 100, 255, 125, 255, RANDOM_SMALL_GEM_ORE, MORTAR, CRYSTALLISABLE        )                                                                                                                                                                                                                     .qual(3,  8.0,   512,  2);
		Vinteum                 = dcmp          ( 8316, "Vinteum"               , SET_EMERALD           ,  80,  80, 255, 255, G_GEM_ORES, MAGICAL, CRYSTAL, MORTAR, BRITTLE, UNBURNABLE)                                                                                .setMcfg( 0, Ma             , 1*U)                                                                                                                          .qual(3, 10.0, 128,  3);
		VinteumPurified         = dcmp          ( 8327, "VinteumPurified"       , SET_EMERALD           , 230, 100, 255, 255, G_GEM_ORES, MAGICAL, CRYSTAL, MORTAR, BRITTLE, UNBURNABLE)                                                                                .setMcfg( 0, Ma             , 1*U)                                                                                                                          .qual(3, 12.0, 256,  3).setLocal("Purified Vinteum");
		ArcaneAsh               = dust          ( 8367, "Arcane Ashes"          , SET_FINE              , 150,  50, 180, 255, MAGICAL, "ArcaneAsh");
		ArcaneCompound          = dust          ( 8366, "Arcane Compound"       , SET_ROUGH             , 180, 140,  50, 255, MAGICAL, FURNACE)                                                                                                                                                                                                                                                                                     .setSmelting(ArcaneAsh, U*2);
		Moonstone               = gem           ( 8452, "Moonstone"             , SET_RUBY              , 210, 210, 255, 255)                                                                                                                                                                                                                                                                                                       .qual(3, 14.0, 512,  4);
		Sunstone                = gem           ( 8453, "Sunstone"              , SET_RUBY              , 255, 150,   0, 255)                                                                                                                                                                                                                                                                                                       .qual(3, 14.0, 512,  4);
		Chimerite               = gem           ( 8454, "Chimerite"             , SET_RUBY              , 255, 255, 255, 255)                                                                                                                                                                                                                                                                                                       .qual(3, 10.0, 256,  3).setGenerifying(Vinteum);
		CrimsonMiddle           = gem           ( 8282, "Crimson Middle"        , SET_DIAMOND           , 240,  50,  50, 127, GLOWING, LIGHTING, MORTAR, BRITTLE);
		GreenMiddle             = gem           ( 8283, "Green Middle"          , SET_DIAMOND           ,  50, 240,  50, 127, GLOWING, LIGHTING, MORTAR, BRITTLE);
		AquaMiddle              = gem           ( 8284, "Aqua Middle"           , SET_DIAMOND           ,  50,  50, 240, 127, GLOWING, LIGHTING, MORTAR, BRITTLE);
		Valonite                = gem           ( 8285, "Valonite"              , SET_SHARDS            , 255, 205, 240, 127)                                                                                                                                                                                                                                                                           .qual(3,  9.0,  2560,  3);
		Scabyst                 = gem           ( 8288, "Scabyst"               , SET_SHARDS            , 110, 165, 165, 127);
		Ambrosium               = gem           ( 8293, "Ambrosium"             , SET_DIAMOND           , 244, 242,  96, 127, CRYSTALLISABLE, GLOWING, LIGHTING)                                                                                                                                                                                                                                        .qual(3,  4.0,    16,  2);
		Continuum               = gem           ( 8295, "Continuum"             , SET_RUBY              , 222, 129,  40, 127, CRYSTALLISABLE)                                                                                                                                                                                                                                                           .qual(3,  4.0,    64,  3);
		EnderAmethyst           = valgemelec    ( 8329, "AmethystEnder"         , SET_FLINT             , 210,  50, 210, 127)                                                                                                               .setMcfg( 5, SiO2           , 4*U, Fe               , 1*U, Ma               , 1*U)                                                                          .qual(3, 10.0, 2560, 3).setGenerifying(Amethyst).setLocal("Ender Amethyst");
		EnderPearl              = elec          ( 8318, "EnderPearl"            , SET_SHINY             , 108, 220, 200, 255, G_PEARL_TRANSPARENT, CRYSTAL, BRITTLE, MAGICAL, PEARL, MELTING, "Ender")                                                                  .setMcfg(10, Be             , 1*U, K                , 4*U, N                , 5*U, Ma               , 6*U)                                                  .qual(3,  1.0,  16,  1).setLocal("Enderpearl").heat(2723, 3785);
		EnderEye                = cent          ( 8319, "EnderEye"              , SET_SHINY             , 160, 250, 230, 255, G_PEARL_TRANSPARENT, CRYSTAL, BRITTLE, MAGICAL, PEARL, MELTING)                                                                           .setMcfg( 9, EnderPearl     , 9*U, Blaze            , 1*U)                                                                                                  .qual(3,  1.0,   16,  1).setLocal("Endereye").heat(3447, 4978);
		NetherStar              = crystal       ( 8320, "Nether Star"           , SET_NETHERSTAR        , 255, 255, 255, 255, BRITTLE, UNBURNABLE, MAGICAL, GLOWING, MELTING)                                                                                                                                                                                                                                                       .qual(3,  8.0,   5120,  4).heat(3896, 5127);
		Frezarite               = create        ( 8391, "Frezarite"             , SET_NETHERSTAR        , 255, 255, 255, 255, G_GEM, CRYSTAL, BRITTLE, MAGICAL)                                                                                                                                                                                                                                                                     .qual(3, 4.0, 128, 2);
		RedMeteor               = create        ( 8392, "Red Meteor"            , SET_RUBY              , 255,  60,  60, 255, G_GEM, MAGICAL, GLOWING, UNBURNABLE)                                                                                                                                                                                                                                                                  .qual(3, 8.0, 512, 3);
		Dilithium               = crystal       ( 8317, "Dilithium"             , SET_DIAMOND           , 153, 255, 255, 127, CRYSTALLISABLE, QUARTZ)                                                                                                                                                                                                                                                         .setSmelting(null, 0);
		Zircon                  = valgemelec    ( 8419, "Zircon"                , SET_EMERALD           ,  99,  24,  29, 255, WASHING_FIRESTONE)                                                                                                    .uumMcfg( 0, Zr             , 1*U, SiO2             , 3*U, O                , 2*U)                                                                          .setSmelting(Zr, U9).qual(3, 8.0, 384, 2);
	}
	private static void reg0027() { // upstream MT.java:1512-1555
		Azurite                 = elec          ( 8420, "Azurite"               , SET_QUARTZ            , 109, 164, 247, 255, G_GEM_ORES, CRYSTAL, MORTAR, BRITTLE, FURNACE)                                                                                            .uumMcfg( 0, Cu             , 3*U, CO3              , 8*U, O                , 1*U, H2O              , 3*U)                                                  .setSmelting(Cu, U9);
		Eudialyte               = dcmp          ( 8421, "Eudialyte"             , SET_LAPIS             , 155,  96, 114, 255, G_GEM_ORES, CRYSTAL, MORTAR, BRITTLE)                                                                                                     .setMcfg( 0, Zircon         ,18*U, MnO2             , 3*U, Na               ,15*U, Ca               , 6*U, Cl, U*2, SiO2, 75*U, O, 12*U);
		Lazurite                = elec          ( 8330, "Lazurite"              , SET_LAPIS             , 100, 120, 255, 255, G_GEM_ORES, DENSEPLATES, CRYSTAL, CRYSTALLISABLE, MORTAR, BRITTLE)                                                                        .uumMcfg( 0, Al2O3          , 6*U, SiO2             , 6*U, Ca               , 8*U, Na               , 8*U);
		Sodalite                = elec          ( 8331, "Sodalite"              , SET_LAPIS             ,  20,  20, 255, 255, G_GEM_ORES, DENSEPLATES, CRYSTAL, CRYSTALLISABLE, MORTAR, BRITTLE)                                                                        .uumMcfg( 0, Al2O3          , 3*U, SiO2             , 3*U, Na               , 4*U, Cl               , 1*U);
		Lapis                   = cent          ( 8332, "Lapis"                 , SET_LAPIS             ,  70,  70, 220, 255, G_GEM_ORES, DENSEPLATES, CRYSTAL, CRYSTALLISABLE, MORTAR, BRITTLE)                                                                        .uumMcfg( 0, Lazurite       ,12*U, Sodalite         , 2*U, Pyrite           , 1*U, CaCO3            , 1*U);
		Charcoal                = setDensity(coal          ( 8336, "Charcoal"              , SET_LIGNITE           , 100,  70,  70, 255, TICKS_PER_SMELT* 8).setBurning(Ash, U4)                                                                                                   .uumMcfg( 0, C              , 1*U)                                                                                                                          .setSmelting(C, U2).heat(1700, C.mBoilingPoint), 0.929);
		Coal                    = setDensity(coal          ( 8334, "Coal"                  , SET_LIGNITE           ,  70,  70,  70, 255, TICKS_PER_SMELT* 8).setBurning(DarkAsh, U4)                                                                                               .uumMcfg( 0, C              , 1*U)                                                                                                                          .setSmelting(C, U2).heat(1700, C.mBoilingPoint), 0.929);
		CoalCoke                = steal(setDensity(coal          ( 8349, "Coal Coke"             , SET_LIGNITE           , 140, 140, 170, 255, TICKS_PER_SMELT*16, "Coke").setBurning(DarkAsh, U9)                                                                                       .uumMcfg( 0, C              , 1*U)                                                                                                                          .setSmelting(C, U ).heat(1700, C.mBoilingPoint), 0.929), Coal);
		Anthracite              = coal          ( 8362, "Anthracite"            , SET_LIGNITE           ,  90,  90,  90, 255, TICKS_PER_SMELT*24).setBurning(DarkAsh, U2)                                                                                               .uumMcfg( 1, C              , 2*U)                                                                                                                          .setSmelting(C, U2).heat(1700, C.mBoilingPoint);
		Prismane                = coal          ( 8363, "Prismane"              , SET_LIGNITE           , 115, 110, 110, 255, TICKS_PER_SMELT*48).setBurning(DarkAsh, U)                                                                                                .uumMcfg( 1, C              , 4*U);
		Lonsdaleite             = coal          ( 8364, "Lonsdaleite"           , SET_DIAMOND           , 140, 130, 130, 255, TICKS_PER_SMELT*96).setBurning(DarkAsh, U*2)                                                                                              .uumMcfg( 1, C              , 8*U);
		Lignite                 = setDensity(coal          ( 8337, "Lignite"               , SET_LIGNITE           , 100,  70,  70, 255, TICKS_PER_SMELT* 4).setBurning(DarkAsh, U4)                                                                                               .setMcfg( 7, C              , 2*U, H2O              , 4*U, DarkAsh          , 1*U)                                                                          , 0.865).setLocal("Lignite Coal");
		LigniteCoke             = setDensity(coal          ( 8365, "Lignite Coke"          , SET_LIGNITE           , 140, 100, 100, 255, TICKS_PER_SMELT* 8).setBurning(DarkAsh, U9)                                                                                               .setMcfg( 7, C              , 2*U, DarkAsh          , 1*U)                                                                                                  , 0.865);
		PetCoke                 = steal(setDensity(coal          ( 8390, "Petroleum Coke"        , SET_LIGNITE           , 150, 150, 180, 255, TICKS_PER_SMELT*32, "PetCoke").setBurning(DarkAsh, U9)                                                                                    .uumMcfg( 1, C              , 2*U, S                , 1*U)                                                                                                  , 0.929), Coal);
		Peat                    = dust          ( 8360, "Peat"                  , SET_LIGNITE           ,  64,  40,  14, 255, TICKS_PER_SMELT*10, INGOTS, MULTIINGOTS, BRITTLE, FLAMMABLE, MORTAR).setBurning(Ash, U2);
		PeatBituminous          = dust          ( 8361, "PeatBituminous"        , SET_LIGNITE           ,  80,  40,  10, 255, TICKS_PER_SMELT*12, INGOTS, MULTIINGOTS, BRITTLE, FLAMMABLE, MORTAR)                                                                                                                                                                                                                                  .setLocal("Bituminous Peat");
		HydratedCoal            = mixdust       ( 8335, "Hydrated Coal"         , SET_LIGNITE           ,  70,  70, 100, 255, BRITTLE, FURNACE, MORTAR, COAL)                                                                                                           .uumMcfg( 8, Coal           , 8*U, H2O              , 1*U);
		Graphene                = dcmp          ( 9175, "Graphene"              , SET_DULL              , 128, 128, 128, 255, G_MACHINE, BLACKLISTED_SMELTER, MORTAR, STICKS).setBurning(Ash, U4)                                                                       .uumMcfg( 0, C              , 1*U)                                                                                                                          .setSmelting(C, U2).heat(4300, 4400).setPulver(C, U);
		Ectoplasm               = dust          ( 8373, "Ectoplasm"             , SET_FOOD              , 220, 255, 255, 200, MAGICAL, LIQUID, GLOWING)                                                                                                                                                                                                                                                                             .heat(400, 3000);
		Firestone               = create        ( 8342, "Firestone"             , SET_QUARTZ            , 200,  20,   0, 255, G_QUARTZ_ORES, CRYSTAL, BRITTLE, CRYSTALLISABLE, MAGICAL, QUARTZ, UNBURNABLE, BURNING).qual(3,  6.0, 1280, 3)                                                                                                                                                                                         .heat(3000, 3700);
		Redstone                = redstone      ( 8333, "Redstone"              , SET_REDSTONE          , 200,   0,   0, 255, PULVERIZING_CINNABAR)                                                                                                 .uumMcfg( 0, Pyrite         , 5*U, Hg               , 3*U, SiO2             , 1*U, Ruby             , 1*U)                                                  .heat(500, 1500).qual(0);
		Nikolite                = redstone      ( 8340, "Nikolite"              , SET_REDSTONE          ,  60, 180, 200, 255, MOLTEN, "Electrotine", "Teslatite")                                                                                  .uumMcfg( 0, Sodalite       , 5*U, Cu               , 3*U, SiO2             , 1*U, Ar               , 1*U)                                                  .heat(1500, 3000).qual(0);
		Glowstone               = glowstone     ( 8341, "Glowstone"             , SET_REDSTONE          , 255, 255,   0, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600);
		GlowstoneCeres          = glowstone     ( 8368, "GlowstoneCeres"        , SET_REDSTONE          ,  70,  90,  70, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone).setLocal("Ceres Glowstone");
		GlowstoneIo             = glowstone     ( 8369, "GlowstoneIo"           , SET_REDSTONE          , 180,  20,   0, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone).setLocal("Io Glowstone");
		GlowstoneEnceladus      = glowstone     ( 8370, "GlowstoneEnceladus"    , SET_REDSTONE          ,   0, 250, 250, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone).setLocal("Enceladus Glowstone");
		GlowstoneProteus        = glowstone     ( 8371, "GlowstoneProteus"      , SET_REDSTONE          ,  62,  62,  62, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone).setLocal("Proteus Glowstone");
		GlowstonePluto          = glowstone     ( 8372, "GlowstonePluto"        , SET_REDSTONE          , 123, 150, 220, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone).setLocal("Pluto Glowstone");
		Gloomstone              = glowstone     ( 8456, "Gloomstone"            , SET_REDSTONE          ,  19, 250, 255, 255)                                                                                                                                           .uumMcfg( 0, Phosphorite    , 5*U, Au               , 3*U, SiO2             , 1*U, He               , 1*U)                                                  .heat(500, 600).setGenerifying(Glowstone);
		MilkyQuartz             = quartz        ( 8445, "Milky Quartz"                                  , 210, 210, 210, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3);
		NetherQuartz            = quartz        ( 8346, "Nether Quartz"                                 , 230, 210, 210, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		VoidQuartz              = quartz        ( 8457, "Void Quartz"                                   , 183, 120, 212, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 48, 3).setGenerifying(NetherQuartz);
	}
	private static void reg0028() { // upstream MT.java:1556-1597
		SunnyQuartz             = quartz        ( 8393, "Sunny Quartz"                                  , 255, 255, 200, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		LavenderQuartz          = quartz        ( 8394, "Lavender Quartz"                               , 255, 200, 255, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		RedQuartz               = quartz        ( 8395, "Red Quartz"                                    , 255, 210, 210, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		BlazeQuartz             = quartz        ( 8396, "Blaze Quartz"                                  , 255, 230, 200, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		SmokeyQuartz            = quartz        ( 8397, "Smokey Quartz"                                 ,  20,  20,  20, 255, CRYSTALLISABLE, "QuartzSmoky", "SmokyQuartz")                                                                                             .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz);
		ManaQuartz              = quartz        ( 8398, "Mana Quartz"                                   , 210, 210, 255, 255, CRYSTALLISABLE)                                                                                                                           .setMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 64, 3).setGenerifying(MilkyQuartz);
		ElvenQuartz             = quartz        ( 8399, "Elven Quartz"                                  , 210, 255, 210, 255, CRYSTALLISABLE)                                                                                                                           .setMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 2.5, 64, 3).setGenerifying(MilkyQuartz);
		BlackQuartz             = quartz        ( 8374, "QuartzBlack"                                   ,  20,  20,  20, 255, CRYSTALLISABLE, DECOMPOSABLE, CENTRIFUGE)                                                                                                 .uumMcfg( 1, SiO2           , 1*U, C                , 1*U)                                                                                                  .qual(1, 2.5, 32, 3).setGenerifying(MilkyQuartz).setLocal("Black Quartz");
		CertusQuartz            = quartz        ( 8347, "Certus Quartz"                                 , 210, 210, 230, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          .qual(1, 5.0, 32, 3).setGenerifying(MilkyQuartz);
		ChargedCertusQuartz     = steal(quartz        ( 8348, "Charged Certus Quartz"                         , 210, 210, 230, 255, GLOWING)                                                                                                                                  .uumMcfg( 0, SiO2           , 1*U)                                                                                                                          , CertusQuartz).setPulver(CertusQuartz, U).setGenerifying(CertusQuartz);
		Fluix                   = quartz        ( 8389, "Fluix"                                         , 120,  70, 140, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 2, SiO2           , 2*U, Redstone         , 1*U);
		Redstonia               = gem_aa        ( 8375, "Redstonia"             , SET_EMERALD           , 255,   0,   0, 127, Redstone)                                                                                                                                                                                                                                                                                             .qual(3,  6.0,  300,  2).setGenerifying(Redstone);
		Palis                   = gem_aa        ( 8376, "Palis"                 , SET_EMERALD           ,   0,   0, 255, 127, Lapis   )                                                                                                                                                                                                                                                                                             .qual(3,  6.0,  300,  2).setGenerifying(Lapis);
		Diamantine              = gem_aa        ( 8377, "Diamantine"            , SET_DIAMOND           , 128, 128, 255, 127, Diamond , VALUABLE)                                                                                                                                                                                                                                                                                   .qual(3, 10.0, 1600,  4).setGenerifying(Diamond);
		VoidCrystal             = setDensity(gem_aa        ( 8378, "VoidCrystal"           , SET_RUBY              ,  10,  10,  10, 127, Coal    , TICKS_PER_SMELT*16)                                                                                                                                                                                                                                                                         .qual(3,  6.0,  280,  2).setGenerifying(Coal), 0.929).setLocal("Void");
		Emeradic                = gem_aa        ( 8379, "Emeradic"              , SET_EMERALD           ,   0, 255,   0, 127, Emerald , VALUABLE)                                                                                                                                                                                                                                                                                   .qual(3,  8.0, 2200,  3).setGenerifying(Emerald);
		Enori                   = gem_aa        ( 8380, "Enori"                 , SET_NETHERSTAR        , 255, 255, 255, 127, Fe      )                                                                                                                                                                                                                                                                                             .qual(3,  6.0,  280,  3).setGenerifying(Fe);
		DarkMatter              = create        ( 8381, "Dark Matter"           , SET_RUBY              ,  40,  20,  40, 255, G_GEM, MAGICAL, UNBURNABLE, VALUABLE)                                                                                                                                                                                                                                                                 .qual(3, 20.0, 12800, 5);
		RedMatter               = create        ( 8382, "Red Matter"            , SET_RUBY              , 255,   0,   0, 255, G_GEM, MAGICAL, UNBURNABLE, VALUABLE)                                                                                                                                                                                                                                                                 .qual(3, 30.0, 25600, 6);
		EnergiumRed             = crystalcent   ( 8298, "EnergiumRed"           , SET_DIAMOND           , 255,   0,   0, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, Sapphire       , 4*U, Redstone         , 5*U)                                                                                                  .setLocal("Red Energium");
		EnergiumCyan            = crystalcent   ( 8299, "EnergiumCyan"          , SET_DIAMOND           ,   0, 255, 255, 255, CRYSTALLISABLE)                                                                                                                           .uumMcfg( 0, Sapphire       , 4*U, Nikolite         , 5*U)                                                                                                  .setLocal("Cyan Energium");
		InfusedDull             = crystal_tc    ( 8350, "Infused Dull"                                  , 100, 100, 100, DYE_INDEX_Gray        )                                                                                                                                                                                                                                                                                    .qual(3, 32.0,   64,  3);
		InfusedVis              = crystal_tc    ( 8351, "Infused Vis"                                   , 255,   0, 255, DYE_INDEX_Magenta     , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,   64,  3);
		InfusedAir              = crystal_tc    ( 8352, "Infused Air"                                   , 255, 255,   0, DYE_INDEX_Yellow      , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,   64,  3);
		InfusedFire             = crystal_tc    ( 8353, "Infused Fire"                                  , 255,   0,   0, DYE_INDEX_Red         , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,   64,  3);
		InfusedEarth            = crystal_tc    ( 8354, "Infused Earth"                                 ,   0, 255,   0, DYE_INDEX_Green       , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,  256,  3);
		InfusedWater            = crystal_tc    ( 8355, "Infused Water"                                 ,   0,   0, 255, DYE_INDEX_Blue        , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,   64,  3);
		InfusedEntropy          = crystal_tc    ( 8356, "Infused Entropy"                               ,  62,  62,  62, DYE_INDEX_Black       , GLOWING)                                                                                                                                                                                                                                                                           .qual(3, 32.0,   64,  4);
		InfusedOrder            = crystal_tc    ( 8357, "Infused Order"                                 , 252, 252, 252, DYE_INDEX_White       , GLOWING)                                                                                                                                                                                                                                                                           .qual(3,  8.0,   64,  3);
		InfusedBalance          = crystal_tc    ( 8358, "Infused Balance"                               , 252, 252, 252, DYE_INDEX_LightGray   , GLOWING)                                                                                                                                                                                                                                                                           .qual(3, 32.0,  256,  4);
		HexoriumBlack           = hexorium      ( 9224,  32,  32,  32, DYE_INDEX_Black    );
		HexoriumRed             = hexorium      ( 9225, 128,   0,   0, DYE_INDEX_Red      );
	}
	private static void reg0029() { // upstream MT.java:1598-1651
		HexoriumGreen           = hexorium      ( 9226,   0, 128,   0, DYE_INDEX_Green    );
		HexoriumBlue            = hexorium      ( 9228,   0,   0, 128, DYE_INDEX_Blue     );
		HexoriumWhite           = hexorium      ( 9239, 224, 224, 224, DYE_INDEX_White    );
		Sand                    = dust          ( 8100, "Sand"                  , SET_SAND              , 250, 250, 200, 255, FURNACE, UNRECYCLABLE)                                                                                                                                                                                                                                                                                .stealStatsElement(SiO2).setSmelting(Glass, U).setForging(Glass, U);
		RedSand                 = dust          ( 8104, "Red Sand"              , SET_SAND              , 170,  86,  35, 255, FURNACE)                                                                                                                                                                                                                                                                                              .stealStatsElement(SiO2).setSmelting(Glass, U).setForging(Glass, U);
		EndSandWhite            = dust          ( 8105, "White End Sand"        , SET_SAND              , 230, 250, 250, 255, FURNACE)                                                                                                                                                                                                                                                                                              .stealStatsElement(SiO2).setSmelting(Glass, U).setForging(Glass, U);
		EndSandBlack            = dust          ( 8106, "Black End Sand"        , SET_SAND              ,  40,  60,  60, 255, FURNACE)                                                                                                                                                                                                                                                                                              .stealStatsElement(SiO2).setSmelting(Glass, U).setForging(Glass, U);
		SoulSand                = oredust       ( 8101, "Soulsand"              , SET_SAND              , 100, 100,  80, 255)                                                                                                                                                                                                                                                                                                       .stealStatsElement(SiO2);
		SluiceSand              = dust          ( 8102, "Sluice Sand"           , SET_SAND              , 165, 165, 120, 255)                                                                                                                                                                                                                                                                                                       .stealStatsElement(SiO2);
		PlatinumGroupSludge     = oredust       ( 8103, "Platinum Group Sludge" , SET_SAND              ,  50,  50,  80, 255)                                                                                                                                                                                                                                                                                                       .stealStatsElement(Pt  ).addSourceOf(Ru,Rh,Pd,Os,Ir,Pt);
		RareEarth               = oredust       ( 9100, "Rare Earth"            , SET_FINE              , 128, 128, 100, 255)                                                                                                                                                                                                                                                                                                       .stealStatsElement(Nd  ).addSourceOf(Nd,Y ,La,Ce,Cd,Cs);
		Monazite                = elec          ( 8338, "Monazite"              , SET_REDSTONE          ,  50,  70,  50, 255, G_GEM_ORES, CRYSTAL, BRITTLE, CRYSTALLISABLE, BLACKLISTED_SMELTER)                                                                        .setMcfg( 0, RareEarth      , 1*U, PO4              , 1*U)                                                                                                  .addSourceOf(He);
		Force                   = create        ( 8343, "Force"                 , SET_REDSTONE          , 255, 255,   0, 255, G_GEM_ORES, G_INGOT_MACHINE_ORES, CRYSTAL, MAGICAL, UNBURNABLE, GLOWING).qual(3, 10.0, 128, 3);
		Forcicium               = create        ( 8344, "Forcicium"             , SET_REDSTONE          ,  50,  50,  70, 255, G_QUARTZ_ORES, CRYSTAL, BRITTLE, CRYSTALLISABLE, MAGICAL);
		Forcillium              = create        ( 8345, "Forcillium"            , SET_REDSTONE          ,  50,  50,  70, 255, G_QUARTZ_ORES, CRYSTAL, BRITTLE, CRYSTALLISABLE, MAGICAL);
		Stone                   = setRGBaLiquid(stone         ( 8500, "Stone"                                         , 205, 205, 205, 255, MELTING, MOLTEN, UNRECYCLABLE)                                                                                                                                                                                                                                                                        .stealStatsElement(SiO2).qual(1, 2.0, 16, 1).heat(1100), 192,  96,  64, 255);
		Gravel = Stone;
		Concrete                = stone         ( 8501, "Concrete"              , SET_BRICK             , 100, 100, 100, 255, MELTING)                                                                                                                                  .setMcfg( 0, Stone          , 1*U)                                                                                                                          .stealStatsElement(SiO2).qual(1, 2.5, 32, 0).heat( 500).setSmelting(Stone, U).setGenerifying(Stone);
		Netherrack              = stone         ( 8502, "Netherrack"                                    , 200,   0,   0, 255, UNBURNABLE, FLAMMABLE, BLACKLISTED_SMELTER)                                                                                                                                                                                                                                                           .stealStatsElement(SiO2).qual(1, 2.0,  8, 0).heat(1500, 3000);
		NetherBrick             = stone         ( 8503, "Nether Brick"          , SET_BRICK             , 100,   0,   0, 255, UNBURNABLE, "BrickNether")                                                                                                                                                                                                                                                                            .stealStatsElement(SiO2).qual(1, 2.0, 24, 1).heat(1800, 3000).setPulver(Netherrack, U).setGenerifying(Netherrack);
		Endstone                = stone         ( 8504, "Endstone"                                      , 217, 222, 158, 255, ENDER_DRAGON_PROOF)                                                                                                                                                                                                                                                                                   .stealStatsElement(SiO2).qual(1, 3.0, 16, 1).heat(1200).setGenerifying(EndSandWhite).addSourceOf(He,He_3);
		Obsidian                = elec          ( 8214, "Obsidian"              , SET_STONE             ,  80,  50, 100, 255, G_STONE, STONE, BRITTLE, MORTAR, UNBURNABLE)                                                                                              .setMcfg(64, Mg             , 1*U, Fe               , 1*U, SiO2             , 6*U, O                , 4*U)                                                  .setSmelting(Lava, U).heat(1300, 4000).qual(1, 3.0, 32, 3);
		Bedrock                 = setRGBaLiquid(create        ( 8599, "Bedrock"               , SET_STONE             ,  64,  64,  64, 255, G_STONE, STONE, BRITTLE, MELTING, UNBURNABLE)                                                                                                                                                                                                                                                         .stealStatsElement(Ad  ).qual(1, 8.0, 2048, 5).heat(4000), 128,  96,  64, 255).addSourceOf(Ad,Atl,RareEarth).setGenerifying(Stone);
		PrismarineLight         = stone         ( 9219, "Prismarine"            , SET_PRISMARINE        , 110, 178, 165, 255, G_GEM_ORES, CRYSTAL, CRYSTALLISABLE)                                                                                                                                                                                                                                                                  .qual(1, 4.0, 48, 1).setLocal("Light Prismarine");
		PrismarineDark          = stone         ( 9220, "PrismarineDark"        , SET_PRISMARINE        ,  88, 125, 108, 255, G_GEM_ORES, CRYSTAL, CRYSTALLISABLE)                                                                                                                                                                                                                                                                  .qual(1, 4.0, 48, 1).setLocal("Dark Prismarine");
		Greenstone              = stone         ( 9172, "Greenstone"                                    ,  52, 252,  52, 255)                                                                                                                                                                                                                                                                                                       .setGenerifying(Stone);
		Bluestone               = stone         ( 9185, "Bluestone"                                     ,  52,  52, 252, 255)                                                                                                                                                                                                                                                                                                       .setGenerifying(Stone);
		Epidote                 = stone         ( 9182, "Epidote"                                       , 128, 128, 128, 255)                                                                                                                                                                                                                                                                                                       .setGenerifying(Stone);
		Oilshale                = oredustcent   ( 9853, "Oil Shale"             , SET_STONE             ,  50,  50,  60, 255, FLAMMABLE, TICKS_PER_SMELT* 2, "Oilshale").setBurning(Stone, U2)                                                                          .setMcfg( 0, CaCO3          , 2*U, MilkyQuartz      , 1*U, Clay             , 1*U)                                                                          .heat( 500, 1000).setGenerifying(Stone);
		Petrotheum              = mixdust       ( 8245, "Petrotheum"            , SET_DULL              ,  86,  76,  82, 255, CONTAINERS, MELTING, MORTAR)                                                                                                              .setMcfg(18, Clay           , 9*U, Obsidian         , 9*U, Redstone         , 9*U, Basalz           , 1*U)                                                  .heat(400, 2000);
		Aerotheum               = dust          ( 8246, "Aerotheum"             , SET_SHINY             , 250, 226,  83, 255, CONTAINERS, MELTING, MORTAR)                                                                                                              .setMcfg(18, Sand           , 9*U, KNO3             , 9*U, Redstone         , 9*U, Blitz            , 1*U)                                                  .heat(299, 300);
		Pyrotheum               = mixdust       ( 8212, "Pyrotheum"             , SET_FIERY             , 255, 200,  60, 255, CONTAINERS, MELTING, MORTAR, TICKS_PER_SMELT*120)                                                                                         .setMcfg(18, Coal           , 9*U, S                , 9*U, Redstone         , 9*U, Blaze            , 1*U)                                                  .heat(3800, 6400);
	}
	private static void reg0030() { // upstream MT.java:1652-1687
		Cryotheum               = dust          ( 8213, "Cryotheum"             , SET_SHINY             , 100, 220, 255, 255, CONTAINERS, MELTING, MORTAR)                                                                                                              .setMcfg(18, Snow           , 2*U, KNO3             , 9*U, Redstone         , 9*U, Blizz            , 1*U)                                                  .heat(40, 1000);
		WroughtIron             = setRGBaLiquid(steal(metalmachnd   ( 8643, "Wrought Iron"          , SET_METALLIC          , 200, 180, 180     , RAILS, MORTAR, MAGNETIC_PASSIVE, MOLTEN, NEVER_FURNACE, "WrougtIron")                                                                     .uumAloy( 0, Fe             , 1*U)                                                                                                                          .setPulver(Fe, U), Fe).heat(Fe.mMeltingPoint + 200, Fe.mBoilingPoint).setGenerifying(Fe).qual(3,  6.0,  384,  2), 255, 80, 40, 255);
		AnnealedCopper          = steal(metalmachnd   ( 8640, "Annealed Copper"       , SET_COPPER            , 255, 100,   0     , MOLTEN, FURNACE, EXTRUDER_SIMPLE, MORTAR, WIRES, RAILS, SOFT)                                                                             .uumAloy( 0, Cu             , 1*U)                                                                                                                          .setPulver(Cu, U), Cu).heat(                  2800, Cu.mBoilingPoint).setGenerifying(Cu);
		Alduorite               = steal(setalore      ( 8760, "Alduorite"                                     , 159, 180, 180     , "Adluorite")                                                                                                                              , Al)                                                                                                                                                  .qual(3,  5.0,  256, 2).heat(1567);
		Infuscolium             = steal(metalore      ( 8761, "Infuscolium"                                   , 146,  33,  86     )                                                                                                                                           , Cu)                                                                                                                                                  .qual(3,  5.0,  256, 2).heat(1828);
		Rubracium               = steal(metalore      ( 8762, "Rubracium"                                     , 151,  45,  45     )                                                                                                                                           , W )                                                                                                                                                  .qual(3,  5.0,  256, 2).heat(1847);
		Meutoite                = steal(metalore      ( 8763, "Meutoite"                                      ,  95,  82, 105     )                                                                                                                                           , Ni)                                                                                                                                                  .qual(3,  5.0,  256, 2).heat(1837);
		Lemurite                = steal(metalore      ( 8764, "Lemurite"                                      , 219, 219, 219     )                                                                                                                                           , Mg)                                                                                                                                                  .qual(3,  5.0,  256, 2).heat(1179);
		Aredrite                = steal(metalore      ( 8701, "Aredrite"                                      , 255, 255,   0     , "Ardaite")                                                                                                                                , Pb)                                                                                                                                                  .qual(3,  6.0, 1440, 3).heat(2240);
		Ceruclase               = steal(metalore      ( 8765, "Ceruclase"                                     , 140, 189, 208     )                                                                                                                                           , Sb)                                                                                                                                                  .qual(3,  6.0, 1280, 2).heat(1867);
		Oureclase               = steal(metalore      ( 8767, "Oureclase"                                     , 183,  98,  21     )                                                                                                                                           , Ni)                                                                                                                                                  .qual(3,  6.0, 1920, 3).heat(2789);
		Kalendrite              = steal(metalore      ( 8768, "Kalendrite"                                    , 170,  91, 189     )                                                                                                                                           , Pt)                                                                                                                                                  .qual(3,  5.0, 2560, 3).heat(2679);
		Carmot                  = steal(metalore      ( 8770, "Carmot"                                        , 217, 205, 140     , SOFT)                                                                                                                                     , Zn)                                                                                                                                                  .qual(3, 16.0,  128, 1).heat(1178);
		Sanguinite              = steal(metalore      ( 8771, "Sanguinite"                                    , 185,   0,   0     , "Fettelite")                                                                                                                              , Hg)                                                                                                                                                  .qual(3,  3.0, 4480, 4).heat(3104);
		Vyroxeres               = steal(metalore      ( 8772, "Vyroxeres"                                     ,  85, 224,   1     )                                                                                                                                           , Cu)                                                                                                                                                  .qual(3,  9.0,  768, 3).heat(2348);
		Eximite                 = steal(metalore      ( 8773, "Eximite"                                       , 124,  90, 150     )                                                                                                                                           , Mn)                                                                                                                                                  .qual(3,  5.0, 2560, 3).heat(2758);
		Ignatius                = steal(metalore      ( 8775, "Ignatius"                                      , 255, 169,  83     )                                                                                                                                           , Sn)                                                                                                                                                  .qual(3, 12.0,  512, 2).heat(1978);
		DeepIron                = steal(metalore      ( 8641, "Deep Iron"                                     ,  73,  91, 105     , MAGNETIC_PASSIVE)                                                                                                                         , Fe)                                                                                                                                                  .qual(3,  6.0,  384, 2).heat(Fe);
		ShadowIron              = steal(metalore      ( 8670, "Shadow Iron"                                   ,  95,  76,  63     , MAGNETIC_PASSIVE)                                                                                                                         , Fe)                                                                                                                                                  .qual(3,  6.0,  384, 2).heat(WroughtIron);
		Adamantine              = metalore      ( 8784, "Adamantine"                                    , 255,   0,  64     , MAGNETIC_PASSIVE, DECOMPOSABLE, WITHER_PROOF, ENDER_DRAGON_PROOF)                                                                         .uumMcfg( 0, Ad             , 3*U, O                , 4*U)                                                                                                  .qual(3, 10.0, 4500, 5).heat(Ad).addSourceOf(Ad);
		Prometheum              = metalore      ( 8774, "Prometheum"                                    ,  90, 129,  86     , CENTRIFUGE, DECOMPOSABLE, SOFT)                                                                                                           .setMcfg( 0, Pm             , 3*U, O                , 4*U)                                                                                                  .qual(3,  8.0,  512, 1).heat(Pm);
		Vulcanite               = metalore      ( 8776, "Vulcanite"                                     , 255, 132,  72     , CENTRIFUGE, DECOMPOSABLE)                                                                                                                 .uumMcfg( 0, Cu             , 1*U, Te               , 1*U)                                                                                                  .qual(3,  5.0, 3840, 3);
		Orichalcum              = alloymachore  ( 8769, "Orichalcum"                                    ,  84, 122,  56     , MAGICAL, MOLTEN, WASHING_MERCURY, VALUABLE, CENTRIFUGE)                                                                                   .setMcfg( 4, Cu             , 3*U, Zn               , 1*U, Ma               , 2*U)                                                                          .qual(3,  4.5, 3456, 3).heat(Cu);
		AstralSilver            = slloymachore  ( 8676, "Astral Silver"                                 , 230, 230, 255     , MAGICAL, MOLTEN, WASHING_MERCURY, VALUABLE, CENTRIFUGE, ENDER_DRAGON_PROOF)                                                               .setMcfg( 2, Ag             , 2*U, Ma               , 1*U)                                                                                                  .qual(3, 10.0,   64, 2).heat(Ag).setGenerifying(Ag);
		Midasium                = slloymachore  ( 8677, "Midasium"                                      , 255, 200,  40     , MAGICAL, MOLTEN, WASHING_MERCURY, VALUABLE, WITHER_PROOF)                                                                                 .setMcfg( 2, Au             , 2*U, Ma               , 1*U)                                                                                                  .qual(3, 12.0,   64, 2).heat(Au).setGenerifying(Au);
		Mithril                 = slloymachore  ( 8678, "Mithril"                                       , 100, 140, 250     , MAGICAL, MOLTEN, WASHING_MERCURY, VALUABLE, CENTRIFUGE, "Mythril")                                                                        .setMcfg( 2, Pt             , 2*U, Ma               , 1*U)                                                                                                  .qual(3, 14.0,   64, 3).heat(Pt).setGenerifying(Pt);
		Celenegil               = alloymachore  ( 8779, "Celenegil"                                     , 148, 204,  72     , MAGICAL, MOLTEN, WASHING_MERCURY, VALUABLE, CENTRIFUGE)                                                                                   .setAloy( 0, Pt             , 1*U, Orichalcum       , 1*U)                                                                                                  .qual(3, 10.0, 4096, 3);
		ShadowSteel             = alloy         ( 8671, "Shadow Steel"                                  , 136, 115,  98     , MAGNETIC_PASSIVE)                                                                                                                         .setAloy( 0, ShadowIron     , 1*U, Lemurite         , 1*U)                                                                                                  .qual(3,  6.0,  768, 2);
		Inolashite              = alloy         ( 8777, "Inolashite"                                    , 148, 216, 187     )                                                                                                                                           .setAloy( 0, Alduorite      , 1*U, Ceruclase        , 1*U)                                                                                                  .qual(3,  8.0, 2304, 3);
		Haderoth                = alloy         ( 8778, "Haderoth"                                      , 119,  52,  30     , MAGICAL)                                                                                                                                  .setAloy( 0, Mithril        , 1*U, Rubracium        , 1*U)                                                                                                  .qual(3, 10.0, 3200, 3);
		Desichalkos             = alloy         ( 8781, "Desichalkos"                                   , 114,  47, 168     )                                                                                                                                           .setAloy( 0, Eximite        , 1*U, Meutoite         , 1*U)                                                                                                  .qual(3, 11.0, 4608, 4);
		Tartarite               = alloy         ( 8782, "Tartarite"                                     , 255, 118,  60     , MAGNETIC_PASSIVE, WITHER_PROOF, ENDER_DRAGON_PROOF)                                                                                       .uumAloy( 0, Adamantine     , 1*U, Atl              , 1*U)                                                                                                  .qual(3, 20.0, 7680, 5);
	}
	private static void reg0031() { // upstream MT.java:1688-1727
		Amordrine               = alloy         ( 8783, "Amordrine"                                     , 169, 141, 177     )                                                                                                                                           .setAloy( 0, Prometheum     , 1*U, Kalendrite       , 1*U)                                                                                                  .qual(3,  7.0, 3328, 3);
		Electrum                = slloymachore  ( 8600, "Electrum"                                      , 255, 255, 100     , MORTAR, MOLTEN, VALUABLE, SOFT, ENDER_DRAGON_PROOF, RAILS, WASHING_MERCURY, WITHER_PROOF).qual(3, 12.0,   64, 2)                          .uumAloy( 0, Ag             , 1*U, Au               , 1*U);
		SterlingSilver          = slloymachine  ( 8601, "Sterling Silver"                               , 250, 220, 225     , MORTAR, MOLTEN, VALUABLE, SOFT, ENDER_DRAGON_PROOF).qual(3, 13.0,  128, 2)                                                                .uumAloy( 0, Cu             , 1*U, Ag               , 4*U);
		RoseGold                = slloymachine  ( 8602, "Rose Gold"                                     , 255, 230,  30     , MORTAR, MOLTEN, VALUABLE, SOFT, WITHER_PROOF, "Tumbaga").qual(3, 14.0,  128, 2)                                                           .uumAloy( 0, Cu             , 1*U, Au               , 4*U);
		Angmallen               = slloymachine  ( 8603, "Angmallen"                                     , 215, 225, 138     , MORTAR, MOLTEN, MAGNETIC_PASSIVE).qual(3, 10.0,  128, 2)                                                                                  .uumAloy( 0, Au             , 1*U, WroughtIron      , 1*U);
		InductiveAlloy          = slloymachine  ( 8604, "GoldInductive"                                 , 255, 150,  75     , MORTAR, MAGNETIC_PASSIVE, MOLTEN, SOFT)                                                                                                   .uumAloy( 0, Au             , 1*U, Redstone         , 1*U)                                                                                                  .setLocal("Inductive Alloy");
		Cd_In_Ag_Alloy          = alloymachine  ( 8605, "Cd-In-Ag-Alloy"                                , 100, 100, 128     )                                                                                                                                           .uumAloy( 0, Cd             , 1*U, In               , 1*U, Ag               , 1*U);
		GildedIron              = slloymachine  ( 8606, "Gilded Iron"                                   , 255, 230,  80     , COATED, CENTRIFUGE, MAGNETIC_PASSIVE, WITHER_PROOF).qual(3, 12.0,  256, 2)                                                                .setMcfg( 9, Fe             , 9*U, Au               , 1*U)                                                                                                  .setSmelting(Fe, U).setForging(Fe, U);
		Brass                   = clloymachine  ( 8620, "Brass"                                         , 255, 180,   0     , FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(2, 7.0,  96, 1)                                                                      .uumAloy( 0, Cu             , 3*U, Zn               , 1*U)                                                                                                  .heat(1160, Cu.mBoilingPoint);
		CobaltBrass             = clloymachine  ( 8621, "Cobalt Brass"                                  , 180, 180, 160     , FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(3, 8.0, 256, 2)                                                                      .uumAloy( 0, Brass          , 7*U, Al               , 1*U, Co               , 1*U);
		AluminiumAlloy          = clloymachine  ( 8622, "Aluminium Alloy"                               , 200, 200, 180     , CENTRIFUGE)                                                                                                                               .uumMcfg(45, Al             ,45*U, Si               , 1*U)                                                                                                  .qual(Al);
		Bronze                  = clloymachine  ( 8610, "Bronze"                                        , 210, 130,  60     , RAILS, FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(3, 5.5,  448, 2)                                                              .uumAloy( 0, Cu             , 3*U, Sn               , 1*U)                                                                                                  .heat(Cu.mMeltingPoint, Cu.mBoilingPoint);
		BlackBronze             = clloymachine  ( 8611, "Black Bronze"                                  , 100,  50, 125     , MORTAR, MOLTEN, SOFT).qual(3, 12.0,  512, 2)                                                                                              .uumAloy( 0, Cu             , 3*U, Electrum         , 2*U);
		BismuthBronze           = clloymachine  ( 8612, "Bismuth Bronze"                                , 100, 125, 125     , FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(3,  8.0,  512, 2)                                                                    .setAloy( 0, Bi             , 1*U, Brass            , 4*U);
		Hepatizon               = alloymachine  ( 8613, "Hepatizon"                                     , 117,  94, 117     , MORTAR, MOLTEN).qual(3, 12.0,  256, 2)                                                                                                    .uumAloy( 0, Au             , 1*U, Bronze           , 1*U);
		ArsenicCopper           = clloymachine  ( 8614, "Arsenic Copper"                                , 210, 160,  60     , FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(3, 5.5,  448, 2)                                                                     .uumAloy( 0, Cu             , 3*U, As               , 1*U)                                                                                                  .heat(Cu.mMeltingPoint, Cu.mBoilingPoint);
		ArsenicBronze           = clloymachine  ( 8615, "Arsenic Bronze"                                , 200, 200, 222     , FURNACE, SOFT, EXTRUDER_SIMPLE, MORTAR, MOLTEN).qual(3, 6.0,  480, 2)                                                                     .uumAloy( 0, As             , 1*U, Bronze           , 4*U)                                                                                                  .heat(Cu.mMeltingPoint, Cu.mBoilingPoint);
		Steel                   = setRGBaLiquid(alloymachore  ( 8630, "Steel"                                         , 130, 130, 130     , MOLTEN, RAILS, MORTAR, MAGNETIC_PASSIVE, NEVER_FURNACE).qual(3,  6.0,  512, 2)                                                            .uumMcfg( 0, WroughtIron    , 1*U)                                                                                                                          .heat(2046, Fe.mBoilingPoint, Fe.mPlasmaPoint), 255, 20, 10, 255);
		BlackSteel              = alloymachine  ( 8631, "Black Steel"                                   ,  90,  90,  90     , MOLTEN).qual(3,  6.5,  768, 2)                                                                                                            .uumAloy( 0, Ni             , 1*U, BlackBronze      , 1*U, Steel            , 3*U);
		BlueSteel               = alloymachine  ( 8632, "Blue Steel"                                    , 100, 100, 140     , MOLTEN).qual(3,  7.0,  896, 2)                                                                                                            .setAloy( 0, SterlingSilver , 1*U, BismuthBronze    , 1*U, Steel            , 2*U, BlackSteel       , 4*U);
		RedSteel                = alloymachine  ( 8633, "Red Steel"                                     , 140, 100, 100     , MOLTEN).qual(3,  7.5, 1024, 2)                                                                                                            .uumAloy( 0, RoseGold       , 1*U, Brass            , 1*U, Steel            , 2*U, BlackSteel       , 4*U);
		DamascusSteel           = alloymachine  ( 8634, "Damascus Steel"                                , 110, 110, 110     , MOLTEN, CENTRIFUGE, MAGNETIC_PASSIVE).qual(3,  8.0, 1280, 2)                                                                              .uumMcfg(50, Steel          ,50*U, V                , 1*U, W                , 1*U);
		VanadiumSteel           = alloymachine  ( 8653, "VanadiumSteel"                                 , 100, 100, 100     , MOLTEN, MAGNETIC_PASSIVE).qual(3, 7.0, 512, 3)                                                                                            .uumAloy( 0, Steel          , 4*U, V                , 1*U)                                                                                                  .setLocal("Vanadiumsteel");
		TungstenSteel           = alloymachine  ( 8635, "Tungstensteel"                                 , 100, 100, 160     , MOLTEN, RAILS, MAGNETIC_PASSIVE, UNBURNABLE, "TungstenSteel", "Wolframsteel", "WolframSteel").qual(3, 10.0, 5120, 4)                      .uumAloy( 0, Steel          , 1*U, W                , 1*U);
		TungstenCarbide         = setDensity(alloymachine  ( 8638, "Tungsten Carbide"                              , 123, 123, 123     , RAILS, UNBURNABLE, "Carbide", "WolframCarbide").qual(3, 10.0, 5120, 4)                                                                    .uumAloy( 0, W              , 1*U, C                , 1*U)                                                                                                  , 15.6).heat(3070, 6270);
		HSLA                    = setRGBaLiquid(steal(alloymachine  ( 8637, "HSLA-Steel"                                    , 210, 210, 255     , RAILS, CENTRIFUGE, MORTAR, MAGNETIC_PASSIVE, "HSLA")                                                                                      .uumMcfg( 2, WroughtIron    , 1*U)                                                                                                                          , Steel).heat(1873, Fe.mBoilingPoint, Fe.mPlasmaPoint), 180, 80, 30, 255);
		SpringSteel             = steal(alloymachine  ( 8639, "HSLA-Spring-Steel"                             , 220, 100, 100     , CENTRIFUGE, MAGNETIC_PASSIVE)                                                                                                             .uumMcfg(45, HSLA           ,45*U, Redstone         , 2*U)                                                                                                  , HSLA).setLocal("Spring Steel");
		TungstenAlloy           = steal(alloymachine  ( 8766, "HSLA-Tungsten-Alloy"                           , 179, 119, 190     , CENTRIFUGE, MAGNETIC_PASSIVE)                                                                                                             .uumMcfg(180,SpringSteel   ,180*U, W                , 1*U)                                                                                                  , SpringSteel).qual(3,  7.0, 1024, 2).setLocal("Tungsten Alloy");
		PigIron                 = steal(metalmachore  ( 8642, "Pig Iron"                                      , 200, 180, 180     , MOLTEN, MORTAR, MAGNETIC_PASSIVE)                                                                                                         .uumMcfg( 0, WroughtIron    , 1*U)                                                                                                                          .setPulver(Fe    , U).setSmelting(WroughtIron, U)                                                                                                          , WroughtIron).setGenerifying(Fe   ).qual(3,  6.0,  384,  2);
		IronCompressed          = steal(alloymachnd   ( 8644, "IronCompressed"        , SET_METALLIC          , 128, 128, 128     , CENTRIFUGE, MORTAR, MAGNETIC_PASSIVE)                                                                                                     .uumMcfg( 0, Fe             , 1*U)                                                                                                                          .setPulver(Fe    , U).setSmelting(Fe         , U)                                                                                                          , Fe         ).setGenerifying(Fe   ).setLocal("Compressed Iron");
		IronCast                = steal(alloymachnd   ( 8803, "Cast Iron"             , SET_METALLIC          ,  64,  64,  64     , CENTRIFUGE, MORTAR, MAGNETIC_PASSIVE)                                                                                                     .uumMcfg( 0, Fe             , 1*U)                                                                                                                          .setPulver(Fe    , U)                                                                                                                                      , Fe         ).setGenerifying(Fe   );
		IronMagnetic            = steal(metalmachnd   ( 8645, "IronMagnetic"          , SET_MAGNETIC          , 200, 200, 200     , LAYERED, MORTAR, MAGNETIC_ACTIVE, AUTO_COLLECTING)                                                                                        .uumMcfg( 0, Fe             , 1*U)                                                                                                                          .setBending(Fe   , U).setCompressing(Fe      , U).setPulver(Fe   , U).setSmashing(Fe   , U).setSmelting(Fe   , U).setWorking(Fe   , U).setForging(Fe   , U), Fe         ).setGenerifying(Fe   ).setLocal("Magnetic Iron");
	}
	private static void reg0032() { // upstream MT.java:1728-1762
		SteelMagnetic           = steal(metalmachnd   ( 8646, "SteelMagnetic"         , SET_MAGNETIC          , 128, 128, 128     , LAYERED, MORTAR, MAGNETIC_ACTIVE, AUTO_COLLECTING)                                                                                        .uumMcfg( 0, Steel          , 1*U)                                                                                                                          .setBending(Steel, U).setCompressing(Steel   , U).setPulver(Steel, U).setSmashing(Steel, U).setSmelting(Steel, U).setWorking(Steel, U).setForging(Steel, U), Steel      ).setGenerifying(Steel).setLocal("Magnetic Steel");
		NeodymiumMagnetic       = steal(metalmachnd   ( 8647, "NeodymiumMagnetic"     , SET_MAGNETIC          , 100, 100, 100     , LAYERED, MORTAR, MAGNETIC_ACTIVE, AUTO_COLLECTING)                                                                                        .uumMcfg( 0, Nd             , 1*U)                                                                                                                          .setBending(Fe   , U).setCompressing(Nd      , U).setPulver(Nd   , U).setSmashing(Nd   , U).setSmelting(Nd   , U).setWorking(Nd   , U).setForging(Nd   , U), Nd         ).setGenerifying(Nd   ).setLocal("Magnetic Neodymium");
		DarkIron                = steal(metalmachore  ( 8648, "Dark Iron"             , SET_DULL              ,  55,  40,  60     , MAGNETIC_PASSIVE, "FzDarkIron", "FZDarkIron")                                                                                             .setMcfg( 0, Fe             , 1*U)                                                                                                                          , Fe).qual(3, 7.0, 384, 3).heat(Steel.mMeltingPoint + 200, Fe.mBoilingPoint);
		SteelGalvanized         = clloymachine  ( 8651, "SteelGalvanized"                               , 250, 240, 240     , COATED, CENTRIFUGE).qual(3,  7.0, 768, 2)                                                                                                 .setMcfg( 9, Steel          , 9*U, Zn               , 1*U)                                                                                                  .setSmelting(Steel, U).setForging(Steel, U).setLocal("Galvanized Steel");
		TungstenSintered        = steal(alloymachnd   ( 8652, "TungstenSintered"      , SET_METALLIC          ,  70,  70,  70     , RAILS, UNBURNABLE).qual(3, 8.0, 5120, 3)                                                                                                  .uumMcfg( 0, W              , 1*U)                                                                                                                          , W).setAllToTheOutputOf(W).setForging(null, U).setCutting(null, U).setWorking(null, U).setSmashing(null, U).setGenerifying(W).setLocal("Sintered Tungsten");
		TitaniumGold            = alloymachine  ( 8654, "Titanium-Gold"                                 , 222, 222, 255     , MOLTEN).qual(3, 12.0, 5120, 4)                                                                                                            .uumAloy( 0, Ti             , 3*U, Au               , 1*U);
		Ta4HfC5                 = alloymachine  ( 8802, "Tantalum Hafnium Carbide"                      ,  32, 128,  32     , UNBURNABLE)                                                                                                                               .uumAloy( 0, Ta             , 4*U, Hf               , 1*U, C                , 5*U)                                                                          .qual(2).heat(4263);
		MeteoricIron            = steal(metalmachore  ( 8649, "Meteoric Iron"         , SET_SPACE             , 150, 140, 120     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING, RAILS, DECOMPOSABLE)                                                                            .uumMcfg( 0, Fe             , 1*U)                                                                                                                          , WroughtIron).qual(3, 7.0, 896, 2).heat(Fe.mMeltingPoint + 200, Fe.mBoilingPoint + 200).setGenerifying(Fe);
		MeteoricSteel           = steal(alloymachine  ( 8650, "Meteoric Steel"        , SET_SPACE             , 130, 120, 100     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING, RAILS)                                                                                          .uumMcfg( 0, MeteoricIron   , 1*U)                                                                                                                          , Steel).qual(3, 8.0, 1280, 2).heat(Steel.mMeltingPoint + 200, Steel.mBoilingPoint + 200).setGenerifying(Steel);
		MeteoricBlackSteel      = alloymachine  ( 8690, "Meteoric Black Steel"                          ,  85,  85,  85     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING).qual(3,  8.0, 1280, 2)                                                                          .uumAloy( 0, Ni             , 1*U, BlackBronze      , 1*U, MeteoricSteel    , 3*U)                                                                          .setGenerifying(BlackSteel);
		MeteoricBlueSteel       = alloymachine  ( 8691, "Meteoric Blue Steel"                           ,  95,  95, 135     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING).qual(3,  8.5, 1408, 2)                                                                          .setAloy( 0, SterlingSilver , 1*U, BismuthBronze    , 1*U, MeteoricSteel    , 2*U, MeteoricBlackSteel,4*U)                                                  .setGenerifying(BlueSteel);
		MeteoricRedSteel        = alloymachine  ( 8692, "Meteoric Red Steel"                            , 135,  95,  95     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING).qual(3,  9.0, 1536, 2)                                                                          .uumAloy( 0, RoseGold       , 1*U, Brass            , 1*U, MeteoricSteel    , 2*U, MeteoricBlackSteel,4*U)                                                  .setGenerifying(RedSteel);
		RedAlloy                = clloy         ( 8660, "Red Alloy"                                     , 200,   0,   0     , MORTAR, WIRES, FURNACE, SOFT, EXTRUDER_SIMPLE, MOLTEN)                                                                                    .uumAloy( 1, Cu             , 1*U, Redstone         , 4*U)                                                                                                  .stealStatsElement(Cu).heat(1400, Cu.mBoilingPoint);
		BlueAlloy               = clloy         ( 8659, "Blue Alloy"                                    , 100, 180, 255     , MORTAR, WIRES, FURNACE, SOFT, EXTRUDER_SIMPLE, MOLTEN)                                                                                    .uumAloy( 1, Ag             , 1*U, Nikolite         , 4*U)                                                                                                  .stealStatsElement(Ag).heat(1400, Ag.mBoilingPoint);
		PurpleAlloy             = clloy         ( 8657, "Purple Alloy"                                  , 255, 120, 255     , MORTAR, WIRES, FURNACE, SOFT, EXTRUDER_SIMPLE, MOLTEN)                                                                                    .uumAloy( 1, RedAlloy       , 1*U, BlueAlloy        , 1*U)                                                                                                                        .heat(1400, Ag.mBoilingPoint);
		Mingrade                = clloy         ( 8804, "Mingrade"                                      , 255,  80,  20     , MORTAR, WIRES, FURNACE, SOFT, EXTRUDER_SIMPLE)                                                                                            .uumMcfg( 0, Cu             , 1*U, Redstone         , 1*U)                                                                                                                        .heat(1400, Cu.mBoilingPoint).setLocal("Red Copper");
		RedstoneAlloy           = setPriorityPrefix(clloy         ( 8733, "Redstone Alloy"                                , 140,  50,  50     , MOLTEN)                                                                                                                                   .uumAloy( 1, Si             , 1*U, Redstone         , 1*U)                                                                                                  .stealStatsElement(Si), 5);
		NikolineAlloy           = setPriorityPrefix(clloy         ( 8737, "Nikoline Alloy"                                ,  50,  90, 140     , MOLTEN, "TeslatineAlloy")                                                                                                                 .uumAloy( 1, Si             , 1*U, Nikolite         , 1*U)                                                                                                  .stealStatsElement(Si), 5);
		ElectrotineAlloy        = clloy         ( 8658, "Electrotine Alloy"                             , 100, 180, 255     , MORTAR, WIRES, MOLTEN, FURNACE, EXTRUDER_SIMPLE)                                                                                          .uumAloy( 1, WroughtIron    , 1*U, Nikolite         , 8*U)                                                                                                  .stealStatsElement(Fe).heat(1400, Fe.mBoilingPoint);
		ElectrumFlux            = slloy         ( 8711, "Electrum Flux"                                 , 255, 255, 120     , SOFT).qual(3, 14.0,  64, 2)                                                                                                               .uumAloy( 1, Electrum       , 1*U, Redstone         , 2*U)                                                                                                  .stealStatsElement(Electrum);
		ConductiveIron          = alloy         ( 8727, "Conductive Iron"                               , 170, 140, 140     , MAGNETIC_PASSIVE).qual(WroughtIron)                                                                                                       .uumAloy( 1, WroughtIron    , 1*U, Redstone         , 1*U)                                                                                                  .stealStatsElement(Fe);
		EnergeticSilver         = alloy         ( 8808, "Energetic Silver"                              ,  93, 126, 151     , SOFT)                                                                                                                                     .uumAloy( 1, Ag             , 1*U, Redstone         , 1*U, Glowstone        , 1*U)                                                                          .stealStatsElement(Ag);
		Invar                   = alloymachine  ( 8661, "Invar"                                         , 220, 220, 150     , MORTAR, MAGNETIC_PASSIVE, MOLTEN).qual(3,  6.0,    256,  2)                                                                               .uumAloy( 0, WroughtIron    , 2*U, Ni               , 1*U);
		Constantan              = clloymachine  ( 8662, "Constantan"                                    , 150, 100,  85     , MORTAR, MAGNETIC_PASSIVE, MOLTEN, "Cupronickel").qual(3,  6.0,  64,  1)                                                                   .uumAloy( 0, Cu             , 1*U, Ni               , 1*U);
		Cupronickel = Constantan;
		Nichrome                = alloymachine  ( 8663, "Nichrome"                                      , 205, 206, 246     , MOLTEN).qual(3,  6.0,   64,  2)                                                                                                           .uumAloy( 0, Ni             , 4*U, Cr               , 1*U);
		Kanthal                 = alloymachine  ( 8664, "Kanthal"                                       , 194, 210, 223     , MOLTEN).qual(3,  6.0,   64,  2)                                                                                                           .uumAloy( 0, WroughtIron    , 1*U, Al               , 1*U, Cr               , 1*U);
		Magnalium               = alloymachine  ( 8665, "Magnalium"             , SET_DULL              , 200, 190, 255     , MOLTEN, FURNACE, EXTRUDER_SIMPLE, RAILS).qual(3,  6.0,  256,  2)                                                                          .uumAloy( 0, Mg             , 1*U, Al               , 2*U);
		StainlessSteel          = slloymachine  ( 8636, "Stainless Steel"                               , 200, 200, 220     , MOLTEN, RAILS).qual(3,  7.0, 480,  2)                                                                                                     .uumAloy( 0, WroughtIron    , 4*U, Invar            , 3*U, Cr               , 1*U, Mn               , 1*U);
		Ultimet                 = slloymachine  ( 8666, "Ultimet"                                       , 180, 180, 230     , MOLTEN).qual(3,  8.0,1024,  3)                                                                                                            .uumAloy( 0, Co             , 5*U, Ni               , 1*U, Cr               , 2*U, Mo               , 1*U);
		TinAlloy                = clloymachine  ( 8667, "Tin Alloy"                                     , 200, 200, 200     , MORTAR, MOLTEN, FURNACE, EXTRUDER_SIMPLE, SOFT).qual(3,  6.5,    96,  2)                                                                  .uumAloy( 0, Sn             , 1*U, WroughtIron      , 1*U);
		BatteryAlloy            = alloy         ( 8668, "Battery Alloy"         , SET_DULL              , 156, 124, 160     , MORTAR, MOLTEN, FURNACE, EXTRUDER_SIMPLE, SOFT)                                                                                           .uumAloy( 0, Pb             , 4*U, Sb               , 1*U);
	}
	private static void reg0033() { // upstream MT.java:1763-1793
		SolderingAlloy          = clloy         ( 8669, "Soldering Alloy"                               , 220, 220, 230     , MORTAR, MOLTEN, BRITTLE, EXTRUDER_SIMPLE, SOFT, SOLDERING_MATERIAL, SOLDERING_MATERIAL_GOOD, FURNACE, WIRES)                              .uumAloy( 0, Sn             , 9*U, Sb               , 1*U);
		IronWood                = alloymachine  ( 8672, "Ironwood"              , SET_WOOD              , 150, 140, 110     , MAGICAL, WOOD, FURNACE, EXTRUDER_SIMPLE, MORTAR, MAGNETIC_PASSIVE, MOLTEN, "IronWood").qual(2, 6.5, 512, 2)                               .setAloy(18, WroughtIron    , 8*U, LiveRoot         , 9*U, Angmallen        , 2*U);
		Steeleaf                = alloymachine  ( 8673, "Steeleaf"              , SET_LEAF              ,  50, 127,  50     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, WOOD  , MORTAR, AUTO_COLLECTING, SOFT).qual(2,  8.0, 144, 3)                                       .setMcfg( 1, Steel             , 1*U, Ma            , 1*U)                                                                                                  .setSmelting(Steel, U4);
		Knightmetal             = alloymachine  ( 8674, "Knightmetal"                                   , 210, 240, 200     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, MOLTEN, MORTAR, "KnightMetal").qual(3, 8.0, 512, 3)                                                .setMcfg( 2, Steel             , 2*U, Ma            , 1*U)                                                                                                  .heat(Steel.mMeltingPoint+100, Steel.mBoilingPoint+100).setGenerifying(Steel);
		FierySteel              = alloymachine  ( 8675, "Fiery Steel"           , SET_FIERY             ,  64,   0,   0     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, "Fiery").qual(3, 9.0, 1024, 4)                 .setMcfg( 1, Steel             , 1*U, Ma            , 1*U)                                                                                                  .heat(Steel.mBoilingPoint-200, Steel.mBoilingPoint+500).setGenerifying(Steel);
		Fireleaf                = alloymachine  ( 8698, "Fireleaf"              , SET_LEAF              , 127,  50,  50     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, WOOD  , WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, AUTO_COLLECTING, SOFT).qual(2, 12.0,  288, 4)  .setMcfg( 1, Steel             , 1*U, Ma            , 2*U)                                                                                                  .setSmelting(FierySteel, U4).heat(Steel.mBoilingPoint-200, Steel.mBoilingPoint+500).setGenerifying(Steel).setGenerifying(Steeleaf);
		MeteoflameSteel         = alloymachine  ( 8693, "Meteoflame Steel"      , SET_FIERY             , 130, 120, 100     , MAGICAL, CENTRIFUGE, MAGNETIC_ACTIVE , MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, AUTO_COLLECTING).qual(3, 12.0, 1280, 4)        .setMcfg( 1, MeteoricSteel     , 1*U, Ma            , 1*U)                                                                                                  .heat(MeteoricSteel.mBoilingPoint-200, MeteoricSteel.mBoilingPoint+500).setGenerifying(MeteoricSteel);
		MeteoflameBlackSteel    = alloymachine  ( 8694, "Meteoflame Black Steel", SET_FIERY             ,  85,  85,  85     , MAGICAL, CENTRIFUGE, MAGNETIC_ACTIVE , MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, AUTO_COLLECTING).qual(3, 12.0, 1280, 4)        .setMcfg( 1, MeteoricBlackSteel, 1*U, Ma            , 1*U)                                                                                                  .heat(MeteoricBlackSteel.mBoilingPoint-200, MeteoricBlackSteel.mBoilingPoint+500).setGenerifying(MeteoricBlackSteel);
		MeteoflameBlueSteel     = alloymachine  ( 8695, "Meteoflame Blue Steel" , SET_FIERY             ,  95,  95, 135     , MAGICAL, CENTRIFUGE, MAGNETIC_ACTIVE , MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, AUTO_COLLECTING).qual(3, 13.0, 1408, 4)        .setMcfg( 1, MeteoricBlueSteel , 1*U, Ma            , 1*U)                                                                                                  .heat(MeteoricBlueSteel .mBoilingPoint-200, MeteoricBlueSteel .mBoilingPoint+500).setGenerifying(MeteoricBlueSteel );
		MeteoflameRedSteel      = alloymachine  ( 8696, "Meteoflame Red Steel"  , SET_FIERY             , 135,  95,  95     , MAGICAL, CENTRIFUGE, MAGNETIC_ACTIVE , MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING, AUTO_COLLECTING).qual(3, 14.0, 1536, 4)        .setMcfg( 1, MeteoricRedSteel  , 1*U, Ma            , 1*U)                                                                                                  .heat(MeteoricRedSteel  .mBoilingPoint-200, MeteoricRedSteel  .mBoilingPoint+500).setGenerifying(MeteoricRedSteel  );
		FlamascusSteel          = alloymachine  ( 8697, "Flamascus Steel"       , SET_FIERY             , 110, 110, 110     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, MOLTEN, WITHER_PROOF, UNBURNABLE, BURNING, GLOWING).qual(3, 12.0, 1280, 4)                         .setMcfg( 1, DamascusSteel     , 1*U, Ma            , 1*U)                                                                                                  .heat(DamascusSteel.mBoilingPoint-200, DamascusSteel.mBoilingPoint+500).setGenerifying(DamascusSteel);
		Thaumium                = alloymachore  ( 8679, "Thaumium"                                      , 150, 100, 200     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, MOLTEN).qual(3, 12.0,   256,  3)                                                                   .setMcfg( 1, Fe                , 1*U, Ma            , 1*U)                                                                                                  .heat(Fe.mMeltingPoint+500, Fe.mBoilingPoint+1000).setGenerifying(Fe);
		DarkThaumium            = alloymachine  ( 8680, "Dark Thaumium"                                 , 100,  75,  75     , MAGICAL, CENTRIFUGE, MAGNETIC_PASSIVE, MOLTEN, WARPING).qual(3, 12.0,   512,  3)                                                                                                                                                                                                                      .heat(Thaumium).setGenerifying(Thaumium);
		VoidMetal               = alloymachine  ( 8681, "Void Metal"                                    ,  30,  10,  30     , MAGICAL, CENTRIFUGE, "Void"          , MOLTEN, WARPING).qual(3, 12.0,  2048,  4)                                                                                                                                                                                                                      .heat(3000, 5000);
		Osmiridium              = alloymachine  ( 8682, "Osmiridium"                                    , 100, 100, 255     , VALUABLE, MOLTEN).qual(3, 11.0, 3840, 4)                                                                                                  .uumAloy( 0, Os             , 1*U, Ir               , 1*U);
		Sunnarium               = slloymachine  ( 8683, "Sunnarium"                                     , 255, 255,   0     , GLOWING, LIGHTING);
		ChromiumDioxide         = alloy         ( 8685, "Chromium Dioxide"      , SET_DULL              ,  10,  20,  10     , ELECTROLYSER, MAGNETIC_PASSIVE)                                                                                                           .uumMcfg( 1, Cr             , 1*U, O                , 2*U)                                                                                                  .qual(3, 11.0,  256,  3).heat(650);
		CrO2 = ChromiumDioxide;
		VanadiumGallium         = slloy         ( 8686, "Vanadium-Gallium"                              , 128, 128, 140     , BRITTLE)                                                                                                                                  .uumAloy( 0, V              , 3*U, Ga               , 1*U);
		YttriumBariumCuprate    = alloy         ( 8687, "Yttrium-Barium-Cuprate"                        ,  80,  64,  70     , BRITTLE, ELECTROLYSER, LAYERED)                                                                                                           .uumMcfg( 6, Y              , 1*U, Ba               , 2*U, Cu               , 3*U, O                , 7*U)                                                  .heat(1200);
		NiobiumNitride          = alloy         ( 8688, "Niobium Nitride"       , SET_DULL              ,  29,  41,  29     , BRITTLE, ELECTROLYSER)                                                                                                                    .uumMcfg( 0, Nb             , 1*U, N                , 1*U)                                                                                                  .heat(2573);
		NiobiumTitanium         = alloy         ( 8689, "Niobium Titanium"      , SET_DULL              ,  29,  29,  41     , BRITTLE)                                                                                                                                  .uumAloy( 0, Nb             , 1*U, Ti               , 1*U);
		AluminiumBrass          = clloymachine  ( 8700, "Aluminium Brass"                               , 220, 220, 130     , FURNACE, EXTRUDER_SIMPLE, MORTAR, MOLTEN, "AluminumBrass").qual(2, 6.0,  64,  2)                                                          .uumAloy( 0, Al             , 3*U, Cu               , 1*U);
		Ardite                  = cetalore      ( 8707, "Ardite"                                        , 200, 120,  20     , MOLTEN).qual(2, 6.0,   64,  3);
		Alumite                 = clloymachine  ( 8702, "Alumite"                                       , 230, 100, 230     , MOLTEN).qual(2, 1.5,   64,  3)                                                                                                            .setAloy( 9, Al2O3          , 5*U, WroughtIron      , 2*U, Obsidian         ,18*U);
		Manyullyn               = clloymachine  ( 8703, "Manyullyn"                                     , 175, 100, 175     , MOLTEN).qual(2, 2.0,   96,  3)                                                                                                            .setAloy( 0, Co             , 1*U, Ardite           , 1*U);
		VibraniumSteel          = slloymachine  ( 8704, "Vibranium Steel"                               ,  40,  24,  50     , CENTRIFUGE, UNBURNABLE, MAGNETIC_PASSIVE).qual(3,   50.0, 2048, 10)                                                                       .uumAloy( 0, Vb             , 1*U, Steel            , 3*U);
		VibraniumSilver         = slloy         ( 8705, "Vibranium Silver"                              , 240, 240, 255     , CENTRIFUGE, UNBURNABLE, ENDER_DRAGON_PROOF).qual(3,  100.0,  512,  9)                                                                     .uumAloy( 0, Vb             , 1*U, Ag               , 3*U);
		Vibramantium            = slloymachine  ( 8706, "Vibramantium"                                  , 250, 250, 250     , CENTRIFUGE, UNBURNABLE, MAGNETIC_PASSIVE).qual(3, 1000.0, 5120, 15)                                                                       .uumAloy( 0, Vb             , 1*U, Ad               , 3*U);
		Signalum                = clloymachine  ( 8708, "Signalum"                                      , 255,  64,   0     , MOLTEN, FURNACE, EXTRUDER_SIMPLE, SOFT)                                                                                                   .uumAloy( 8, Cu             , 1*U, Ag               , 2*U, RedAlloy         , 5*U);
		Lumium                  = clloymachine  ( 8709, "Lumium"                                        , 255, 255,  80     , MOLTEN, FURNACE, EXTRUDER_SIMPLE, SOFT, LIGHTING, GLOWING)                                                                                .uumAloy( 4, Sn             , 3*U, Ag               , 1*U, Glowstone        , 4*U);
		EnderiumBase            = clloymachine  ( 8729, "Enderium Base"                                 ,  53,  85, 108     , MOLTEN)                                                                                                                                   .uumAloy( 4, Sn             , 2*U, Ag               , 1*U, Pt               , 1*U);
	}
	private static void reg0034() { // upstream MT.java:1794-1828
		Enderium                = clloymachine  ( 8710, "Enderium"                                      ,  60, 125, 115     , MAGICAL, MOLTEN).qual(3,  8.0, 256, 3)                                                                                                    .setAloy( 1, EnderiumBase   , 1*U, EnderPearl       , 1*U);
		RefinedGlowstone        = alloynd       ( 8713, "GlowstoneRefined"      , SET_REDSTONE          , 255, 240, 100     , GLOWING).setLocal("Refined Glowstone").qual(2, 8.0, 256, 2)                                                                               .setMcfg( 1, Glowstone      , 1*U, Ge               , 1*U)                                                                                                  .setAllToTheOutputOf(Glowstone);
		RefinedObsidian         = alloy         ( 8714, "ObsidianRefined"       , SET_REDSTONE          , 120,  90, 140     ).setLocal("Refined Obsidian").qual(2, 8.0, 512, 3)                                                                                         .setMcfg( 1, Obsidian       , 1*U, Diamond          , 1*U)                                                                                                  .setAllToTheOutputOf(Obsidian);
		Yellorium               = setalore      ( 8715, "Yellorium"                                     , 140, 130,  20     );
		Blutonium               = setalore      ( 8716, "Blutonium"                                     ,  60,  60, 180     );
		Cyanite                 = setalore      ( 8717, "Cyanite"                                       ,  50, 110, 150     );
		Ludicrite               = setalore      ( 8723, "Ludicrite"                                     , 180, 120, 150     );
		Yellorite               = oredustelec   ( 8724, "Yellorite"             , SET_METALLIC          , 150, 140,  40, 255, BLACKLISTED_SMELTER).setSmelting(Yellorium,U3)                                                                                            .setMcfg( 1, Yellorium      , 1*U, O                , 2*U);
		Bedrock_HSLA_Alloy      = alloy         ( 8718, "Bedrock-HSLA-Alloy"    , SET_BRICK             ,  64,  64,  64     , CENTRIFUGE, MAGNETIC_PASSIVE)                                                                                                             .setMcfg( 1, Bedrock        , 4*U, HSLA             , 1*U)                                                                                                  .qual(3, 10.0, 2560, 5).heat(4000);
		ObsidianSteel           = alloy         ( 8731, "Obsidian Steel"                                ,  60,  60,  60     , MAGNETIC_PASSIVE, RAILS, UNBURNABLE, "DarkSteel", MD.TG).qual(Steel)                                                                      .setAloy( 1, Steel          , 1*U, Obsidian         , 9*U);
		PulsatingIron           = alloy         ( 8725, "Pulsating Iron"                                , 100, 160, 110     , MAGNETIC_PASSIVE, "PhasedIron").qual(WroughtIron)                                                                                         .setAloy( 1, WroughtIron    , 1*U, EnderPearl       , 1*U);
		EnergeticAlloy          = alloy         ( 8728, "Energetic Alloy"       , SET_DULL              , 200, 120,  50     ).qual(Au)                                                                                                                                  .uumAloy( 1, InductiveAlloy , 2*U, Glowstone        , 1*U);
		VibrantAlloy            = slloy         ( 8726, "Vibrant Alloy"                                 , 160, 170,  70     , "PhasedGold", "Vibrant").qual(EnergeticAlloy)                                                                                             .setAloy( 1, EnergeticAlloy , 1*U, EnderPearl       , 1*U);
		ElectricalSteel         = alloy         ( 8730, "Electrical Steel"                              , 140, 140, 140     , MAGNETIC_PASSIVE).qual(Steel)                                                                                                             .uumAloy( 1, Steel          , 1*U, Si               , 1*U);
		Soularium               = alloy         ( 8732, "Soularium"             , SET_DULL              ,  90,  70,  50     ).qual(3, 15.0, 128, 2)                                                                                                                     .setAloy( 1, SoulSand       , 9*U, Au               , 1*U);
		CrudeSteel              = alloy         ( 8806, "Clay Compound"         , SET_BRICK             , 132, 127, 123     , "CrudeSteel")                                                                                                                             .setAloy( 1, Stone          , 2*U, Ceramic          , 1*U);
		EndSteel                = alloy         ( 8807, "End Steel"                                     , 164, 157, 116     )                                                                                                                                           .setAloy( 1, Endstone       , 1*U, ObsidianSteel    , 1*U, Obsidian         , 9*U);
		MelodicAlloy            = alloy         ( 8809, "Melodic Alloy"                                 , 138,  92, 138     )                                                                                                                                           .setAloy( 1, EndSteel       , 1*U, EnderEye         , 1*U);
		StellarAlloy            = alloy         ( 8810, "Stellar Alloy"                                 , 170, 181, 164     )                                                                                                                                           .setAloy( 2, MelodicAlloy   , 1*U, NetherStar       , 1*U, Clay             , 4*U);
		VividAlloy              = alloy         ( 8811, "Vivid Alloy"                                   ,  60, 139, 160     )                                                                                                                                           .setAloy( 1, EnergeticSilver, 1*U, EnderPearl       , 1*U);
		CrystallineAlloy        = slloy         ( 8812, "Crystalline Alloy"                             , 109, 169, 169     );
		CrystallinePinkSlime    = slloy         ( 8813, "Crystalline Pink Slime"                        , 176, 112, 166     );
		SpectreIron             = clloymachine_ ( 8734, "Spectre Iron"                                  , 150, 200, 200, 200, MAGNETIC_PASSIVE, MAGICAL, MOLTEN, GLOWING).qual(3,  8.5, 768, 2)                                                                         .setAloy( 1, WroughtIron    , 1*U, Ectoplasm        , 1*U)                                                                                                  .heat(Fe).setGenerifying(Fe);
		Manasteel               = steal(slloymachine  ( 8720, "Manasteel"                                     , 110, 200, 250     , MAGICAL)                                                                                                                                  .setMcfg( 1, Fe             , 1*U, Ma               , 1*U)                                                                                                  , Steel).qual(3, 12.0, 256, 3).heat(Fe.mMeltingPoint+ 500, Fe.mBoilingPoint+1000).setGenerifying(Fe);
		Terrasteel              = steal(slloymachine  ( 8721, "Terrasteel"                                    , 110, 200,  50     , MAGICAL, UNBURNABLE)                                                                                                                                                                                                                                                                                  , Steel).qual(3, 16.0,2048, 4).heat(Fe.mMeltingPoint+ 750, Fe.mBoilingPoint+1500);
		ElvenElementium         = steal(slloymachore  ( 8722, "Elven Elementium"                              , 250, 120, 250     , MAGICAL, UNBURNABLE, "Elementium")                                                                                                                                                                                                                                                                    , Steel).qual(3, 14.0, 512, 3).heat(Fe.mMeltingPoint+1000, Fe.mBoilingPoint+2000).setLocal("Elementium");
		GaiaSpirit              = slloymachine  ( 8735, "Gaia Spirit"                                   , 250, 250, 250     , MAGICAL, UNBURNABLE, GLOWING).qual(3, 20.0,2048, 4)                                                                                                                                                                                                                                                   .heat(W.mMeltingPoint+250, W.mBoilingPoint+500);
		Endium                  = metalore      ( 8736, "Endium"                , SET_SHINY             , 169, 215, 254     , MAGICAL, "HeeEndium").qual(3, 12.0, 256, 3);
		Mauftrium               = slloymachore  ( 8739, "Mauftrium"                                     , 250, 225, 121     , MAGICAL).qual(3, 12.0,1024, 3)                                                                                                                                                                                                                                                                        .heat(Fe.mMeltingPoint, Fe.mBoilingPoint);
		Elvorium                = slloymachore  ( 8740, "Elvorium"                                      , 235, 164,  77     , MAGICAL).qual(3, 14.0,2048, 3)                                                                                                            .setMcfg( 1, ElvenElementium, 1*U, ElvenDragonstone , 1*U)                                                                                                  .heat(Fe.mMeltingPoint+1000, Fe.mBoilingPoint+2000);
		NiflheimPower           = slloy         ( 8741, "Niflheim Power"                                ,  94,  94, 174     , MAGICAL, GLOWING, UNBURNABLE).qual(3, 18.0,2048, 3)                                                                                       .setMcfg( 1, Elvorium       , 1*U);
		MuspelheimPower         = slloy         ( 8742, "Muspelheim Power"                              , 174,  94,  94     , MAGICAL, GLOWING, UNBURNABLE, BURNING).qual(3, 18.0,2048, 3)                                                                              .setMcfg( 1, Elvorium       , 1*U);
	}
	private static void reg0035() { // upstream MT.java:1829-1876
		Iffesal                 = oredust       ( 8743, "Iffesal"               , SET_SHINY             ,  14,  25, 171, 255, MAGICAL);
		AncientDebris           = metalore      ( 8744, "Ancient Debris"        , SET_SPACE             , 110,  80,  90     , "Ancient", UNBURNABLE, MAGNETIC_PASSIVE, WITHER_PROOF, MOLTEN, VALUABLE, WASHING_MERCURY).qual(0, 1.0,   16, 3)                                                                                                                                                                                       .heat(MeteoricIron);
		Netherite               = alloymachine  ( 8745, "Netherite"                                     ,  80,  70,  80     ,            UNBURNABLE, MAGNETIC_ACTIVE , WITHER_PROOF, MOLTEN, VALUABLE, AUTO_COLLECTING).qual(2, 10.0,  500, 4)                          .setAloy( 1, Au             , 4*U, AncientDebris    , 4*U)                                                                                                  .heat(MeteoricSteel);
		NetherizedDiamond       = alloymachine  ( 8746, "Netherized Diamond"    , SET_DIAMOND           ,  90,  80,  90     , G_GEM    , UNBURNABLE, MAGNETIC_ACTIVE , WITHER_PROOF, COATED, VALUABLE, AUTO_COLLECTING).qual(3, 12.0, 2560, 4)                          .setMcfg( 4, Netherite      , 1*U, Diamond          , 4*U)                                                                                                  .heat(MeteoricSteel);
		Efrine                  = metalore      ( 8747, "Efrine"                                        ,  80, 107,  72     ,            UNBURNABLE, MAGNETIC_PASSIVE, WITHER_PROOF, MOLTEN, WASHING_MERCURY).qual(3, 9.0,  500, 3)                                                                                                                                                                                                 .heat(MeteoricSteel);
		Desh                    = alloymachore  ( 8750, "Desh"                  , SET_DULL              ,  40,  40,  40     , MOLTEN).qual(3,  4.0,   1280,  3)                                                                                                         .uumAloy( 0, B              , 2*U, La               , 2*U, Nd, 1*U, Nb, 1*U, Co, 1*U, Ce, 1*U, Li, 1*U);
		DeshAlloy               = alloymachine  ( 8780, "Workers Alloy"                                 , 216,  42,  42     , MOLTEN).qual(3,  7.0,   2560,  2)                                                                                                         .uumMcfg( 4, Desh           , 4*U, Hg               , 1*U);
		DuraniumAlloy           = alloymachine  ( 8751, "Duranium"                                      ,  75, 175, 175             ).qual(3,  8.0,   1280,  4)                                                                                                         .uumAloy( 0, Dn             , 7*U, Mg               , 1*U)                                                                                                  .setLocal("Duranium Alloy");
		TritaniumAlloy          = alloymachine  ( 8752, "Tritanium"                                     ,  55, 155, 155             ).qual(3, 12.0,   2560,  5)                                                                                                         .uumAloy( 0, Tn             , 3*U, Dn               , 1*U)                                                                                                  .setLocal("Tritanium Alloy");
		Dolamide                = oredust       ( 8753, "Dolamide"              , SET_METALLIC          , 188, 100, 122, 255        );
		Oriharukon              = metalmachore  ( 8754, "Oriharukon"                                    , 220, 220, 240             ).qual(3,  8.0,   2560,  2);
		Adamantite              = metalmachore  ( 8755, "Adamantite"                                    , 255, 255, 190             ).qual(3,  6.0,   2560,  3);
		Duralumin               = alloymachine  ( 8756, "Duralumin"             , SET_DULL              , 255, 255, 220             ).qual(3, 10.0,    512,  2)                                                                                                         .uumMcfg( 0, Al             , 1*U, Cu               , 1*U);
		Meteorite               = steal(metalmachore  ( 8757, "Meteorite"             , SET_SPACE             , 222, 100, 222     , MOLTEN, MAGNETIC_ACTIVE, AUTO_COLLECTING, RAILS, DECOMPOSABLE)                                                                            .uumMcfg( 0, Fe             , 1*U)                                                                                                                          , MeteoricIron).qual(3, 8.0, 1200, 3).setGenerifying(Fe);
		FrozenIron              = steal(metalmachore  ( 8758, "Frozen Iron"           , SET_DULL              , 235, 235, 255             , DECOMPOSABLE, MAGNETIC_PASSIVE)                                                                                                   .uumMcfg( 0, Fe             , 1*U)                                                                                                                          , Fe).setSmelting(Fe, U).setForging(Fe, U);
		Kreknorite              = metalmachore  ( 8759, "Kreknorite"            , SET_SPACE             , 128,   0,   0     , MOLTEN, TICKS_PER_SMELT*18)                                                                                                                                                                                                                                                                           .qual(3, 8.0, 1200, 3).heat(MeteoricSteel.mMeltingPoint + 200, MeteoricSteel.mBoilingPoint + 200);
		Syrmorite               = metalmachore  ( 8785, "Syrmorite"             , SET_DULL              ,  80,  80, 199     , MOLTEN, SOFT)                                                                                                                                                                                                                                                                                         .qual(2, 6.0,  500, 1).heat(Au);
		Octine                  = metalmachore  ( 8786, "Octine"                                        , 255, 128,  32     , MOLTEN, MAGICAL, GLOWING, UNBURNABLE, BURNING)                                                                                                                                                                                                                                                        .qual(3, 8.0,  900, 2).heat(Steel.mBoilingPoint-200, Steel.mBoilingPoint+500);
		HSSG                    = alloymachine  ( 8796, "HSS-G"                                         , 153, 153,   0     , RAILS, MOLTEN, UNBURNABLE).qual(3, 10.0, 4000, 3)                                                                                         .uumAloy( 0, TungstenSteel  , 5*U, Cr               , 1*U, Mo               , 2*U, V                , 1*U);
		HSSE                    = alloymachine  ( 8797, "HSS-E"                                         ,  51, 102,   0     , RAILS, MOLTEN, UNBURNABLE).qual(3, 10.0, 5120, 4)                                                                                         .uumAloy( 0, HSSG           , 6*U, Co               , 1*U, Mn               , 1*U, Si               , 1*U);
		HSSS                    = alloymachine  ( 8798, "HSS-S"                                         , 102,   0,  51     , RAILS, MOLTEN, UNBURNABLE).qual(3, 14.0, 3000, 4)                                                                                         .uumAloy( 0, HSSG           , 6*U, Osmiridium       , 2*U, Ir               , 1*U);
		Bedrockium              = metalmachore  ( 8795, "Bedrockium"            , SET_ROUGH             ,  88,  88,  88     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF)                                                                                                                                                                                                                                                         .heat(2000);
		Draconium               = metalmachore  ( 8791, "Draconium"                                     , 150,  50, 250     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, VALUABLE, MOLTEN).qual(3, 16.0,  5000, 4)                                                                                                                                                                                                               .heat(4500);
		DraconiumAwakened       = alloymachine  ( 8792, "DraconiumAwakened"                             , 250, 150,  50     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, VALUABLE, MOLTEN, GLOWING).qual(3, 24.0, 10000, 5)                                          .setMcfg( 1, Draconium      , 1*U)                                                                                                                          .heat(5500).setLocal("Awakened Draconium");
		CrystalMatrix           = slloymachine  ( 8799, "Crystal Matrix"                                ,  83, 231, 234     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, VALUABLE, MAGICAL).qual(3,        20.0,     25600,  4)                                      .setMcfg( 1, Diamond        ,20*U, NetherStar       , 2*U)                                                                                                  .heat(3896, 5127);
		CosmicNeutronium        = setalmachine  ( 8800, "Cosmic Neutronium"                             ,  30,  10,  40     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, VALUABLE, MAGICAL).qual(3,        50.0,    100000, 10)                                                                                                                                                                                                  .heat(100000);
		Infinity                = setalmachine  ( 8801, "Infinity"                                      , 250, 250, 250     , UNBURNABLE, WITHER_PROOF, ENDER_DRAGON_PROOF, VALUABLE, MAGICAL, GLOWING, LIGHTING, MAGNETIC_ACTIVE, AUTO_COLLECTING).qual(3,1000000000.0,1000000000, 15)                                                                                                                                             .heat(100000);
		Unstable                = setal         ( 8805, "Unstable"                                      , 255, 255, 255, 128, AUTO_BLACKLIST, EXPLODES_IN_NONVANILLA_CRAFTING_GRID, "Unstableingot");
		Trinaquadalloy          = alloymachine  ( 8684, "Trinaquadalloy"                                , 146, 186, 146     , CENTRIFUGE, "NaquadahAlloy").qual(3, 14.0,  20480,  5)                                                                                    .uumAloy( 0, Ke             , 6*U, Nq               , 2*U, C                , 1*U);
		Trinitanium             = clloymachine  ( 8790, "Trinitanium"                                   , 235, 175, 255     ).qual(3, 16.0,   5120,  4)                                                                                                                 .uumAloy( 0, Ke             , 2*U, Ti               , 1*U);
		Iritanium               = alloymachine  ( 8793, "Titanium Iridium"                              , 220, 220, 255     , "Iritanium").qual(3,  8.0,   7680,  4)                                                                                                    .uumAloy( 0, Ir             , 1*U, Ti               , 1*U)                                                                                                  .setLocal("Iritanium");
		TitaniumAluminide       = alloymachine  ( 8794, "Titanium Aluminide"                            , 200, 200, 255     ).qual(3, 12.0,   3840,  3)                                                                                                                 .uumAloy( 3, Ti             , 3*U, Al               , 7*U);
	}
	private static void reg0036() { // upstream MT.java:None-1904
		Trinium = Ke;
		Vibranium = Vb;
		Naquadah = Nq;
		NaquadahEnriched = Nq_528;
		Naquadria = Nq_522;
		FakeOsmium = Ge;
		Adamantium = Ad;
		Silver = Ag;
		Aluminium = Al;
		Bismuth = Bi;
		Lead = Pb;
		Argon = Ar;
		Copper = Cu;
		Gold = Au;
		Iron = Fe;
		Titanium = Ti;
		Calcite = CaCO3;
		Tungsten = W;
		Beryllium = Be;
		Chromium = Cr;
		Manganese = Mn;
		Cobalt = Co;
		Cobalt60 = Co_60;
		Nickel = Ni;
		Arsenic = As;
		Zirconium = Zr;
		Molybdenum = Mo;
		Technetium = Tc;
		Palladium = Pd;
		Neodymium = Nd;
		Osmium = Os;
	}
	private static void reg0037() { // upstream MT.java:1904-1918
		Iridium = Ir;
		Platinum = Pt;
		Thorium = Th;
		Uranium = U_238;
		Uranium235 = U_235;
		Plutonium = Pu;
		Plutonium241 = Pu_241;
		Plutonium243 = Pu_243;
		Americium = Am;
		Americium241 = Am_241;
		Alumina = Al2O3;
		AluminiumFluoride = AlF3;
		AluminiumHydroxide = AlO3H3;
		Gibbsite = AlO3H3;
		Fluorite = CaF2;
		Soapstone = Talc;
		WoodSealed = WoodTreated;
		TeslatineAlloy = NikolineAlloy;
		Teslatite = Nikolite;
		Electrotine = Nikolite;
		Olivine = Peridot;
		SpaceRock    = STONES.SpaceRock;
		MoonRock     = STONES.MoonRock;
		MoonTurf     = STONES.MoonTurf;
		MarsRock     = STONES.MarsRock;
		MarsSand     = STONES.MarsSand;
		Holystone    = STONES.Holystone;
		Livingrock   = STONES.Livingrock;
		Deadrock     = STONES.Deadrock;
		Betweenstone = STONES.Betweenstone;
		Pitstone     = STONES.Pitstone;
		Umber        = STONES.Umber;
	}
	private static void reg0038() { // upstream MT.java:1919-1945
		Redrock      = STONES.Redrock;
		Komatiite    = STONES.Komatiite;
		Pumice       = STONES.Pumice;
		Gabbro       = STONES.Gabbro;
		Basalt       = STONES.Basalt;
		Marble       = STONES.Marble;
		Limestone    = STONES.Limestone;
		Greenschist  = STONES.Greenschist;
		Blueschist   = STONES.Blueschist;
		Kimberlite   = STONES.Kimberlite;
		Quartzite    = STONES.Quartzite;
		GraniteRed   = STONES.GraniteRed;
		GraniteBlack = STONES.GraniteBlack;
		Granite      = STONES.Granite;
		Andesite     = STONES.Andesite;
		Diorite      = STONES.Diorite;
		Blackstone   = STONES.Blackstone;
		Gneiss       = STONES.Gneiss;
		Greywacke    = STONES.Greywacke;
		Siltstone    = STONES.Siltstone;
		Rhyolite     = STONES.Rhyolite;
		Migmatite    = STONES.Migmatite;
		Chert        = STONES.Chert;
		Dacite       = STONES.Dacite;
		Shale        = STONES.Shale;
		Slate        = STONES.Slate;
		Eclogite     = STONES.Eclogite;
	}
	/** Upstream MT.init() :1885-1901. The INITIALIZED guard is upstream :1888; the extra registry-reset awareness is port-only (MaterialRegistry.reset() is a port concept). DATA.Dye_Materials.getClass() (:1892) is omitted: MT.DATA is pure OreDictItemData/OP tables, deferred to the prefix/data tasks. */
	private static boolean INITIALIZED = false;

	public static void init() {
		if (INITIALIZED && MaterialRegistry.INSTANCE.MATERIAL_MAP.get("NULL") == NULL) return;
		INITIALIZED = true;
		reg0000();
		reg0001();
		reg0002();
		reg0003();
		reg0004();
		reg0005();
		reg0006();
		reg0007();
		reg0008();
		reg0009();
		reg0010();
		reg0011();
		reg0012();
		reg0013();
		reg0014();
		reg0015();
		reg0016();
		reg0017();
		reg0018();
		reg0019();
		reg0020();
		reg0021();
		reg0022();
		reg0023();
		reg0024();
		reg0025();
		reg0026();
		reg0027();
		reg0028();
		reg0029();
		reg0030();
		reg0031();
		reg0032();
		reg0033();
		reg0034();
		reg0035();
		fixups(); // upstream :1879-1883 static{} runs after the main fields and before the @Deprecated alias fields
		reg0036();
		reg0037();
		reg0038();
		// Making sure shit is statically loaded, damn it. // upstream :1890-1900
		H.getClass();
		OREMATS.Magnetite.getClass();
		WOODS.Oak.getClass();
		STONES.Basalt.getClass();
		AM.init(); // upstream :1896 AM.Hydrogen.getClass(); AM is chunk-registered in this port, so init() replaces the class-load trigger
		ANY.init();
		TECH.Unknown.getClass();
		TECH.init();
		UNUSED.Vis.getClass();
	}

	/** Technical Materials, which are only there for Recipes and such. */
	public static class TECH {
		@SuppressWarnings("hiding") @Deprecated public static final OreDictMaterial Brick = MT.Brick, AnyGlowstone = ANY.Glowstone, AnyWax = ANY.Wax, AnyWood = ANY.Wood, AnyStone = ANY.Stone, AnyClay = ANY.Clay, AnyIron = ANY.Fe, AnyIronSteel = ANY.Steel, AnyCopper = ANY.Cu, AnySilicon = ANY.Si, AnyTungsten = ANY.W, AnyThaumicCrystal = ANY.ThaumCrystal, AnySalt = ANY.Salt, AnySteel = ANY._Steel, AnyBronze = ANY._Bronze, AnyMetal = ANY._Metal; // upstream MT.java:1950

		// upstream MT.java:1952-1958 (small set, keeps its natural class-init)
		public static final OreDictMaterial
		Organic     = invalid("Organic"    ).put(IGNORE_IN_COLOR_LOG, DONT_SHOW_THIS_COMPONENT),
		Crystal     = invalid("Crystal"    ).put(IGNORE_IN_COLOR_LOG, DONT_SHOW_THIS_COMPONENT, BRITTLE, CRYSTAL),
		Unknown     = invalid("Unknown"    ).put(IGNORE_IN_COLOR_LOG, DONT_SHOW_THIS_COMPONENT),
		Cobblestone = invalid("Cobblestone").put(IGNORE_IN_COLOR_LOG, DONT_SHOW_THIS_COMPONENT, UNRECYCLABLE),
		RefinedIron = steal(stealLooks(invalid("RefinedIron"), HSLA), WroughtIron).setLocal("Refined Iron").setAllToTheOutputOf(Fe).put(IGNORE_IN_COLOR_LOG, SMITHABLE, MELTING).addReRegistrationToThis(WroughtIron);

		// upstream MT.java:3412-3426 String template locals, promoted to fields so the init chunks can share them
		private static final String tMakeSteel = "In order to make Steel you just need to melt Iron or Wrought Iron in a Smelting Crucible and apply Air to it using an Engine.";
		private static final String tMakeWroughtIron = "Wrought Iron is created by heating up Iron until " + WroughtIron.mMeltingPoint + " Kelvin to dissolve most unwanted impurities.";
		private static final String tMakeAnnealedCopper = "Annealed Copper is created by heating up Copper until " + AnnealedCopper.mMeltingPoint + " Kelvin to dissolve most unwanted impurities.";
		private static final String tMakeAluminium = "Making Aluminium is a very complicated chemical Process. You will need an LV Electrolyzer, a Mixer, a Corrosion Resistant Crucible or a Smelter, Fluorite, Saltwater, Alumina and a bit more to do it.";
		private static final String tKillWerewolf = "It is also very useful in order to kill Werewolves and alike, since everyone knows how Werewolves are allergic to Silver! It also works on Armor like a kind of Thorns (without the stupid extra armor damage)";
		private static final String tKillSlime = "Somehow this Material dissolves Slimey substances and therefore causes severe damage to Slimes and similar Creatures!";

		/** Upstream TECH.init :1959-3591, split into 32 chunks (red line: no giant methods). */
		static void init() {
			init0();
			init1();
			init2();
			init3();
			init4();
			init5();
			init6();
			init7();
			init8();
			init9();
			init10();
			init11();
			init12();
			init13();
			init14();
			init15();
			init16();
			init17();
			init18();
			init19();
			init20();
			init21();
			init22();
			init23();
			init24();
			init25();
			init26();
			init27();
			init28();
			init29();
			init30();
			init31();
		}

		private static void init0() { // upstream MT.java:None-None
			OreDictMaterial.MATERIAL_ARRAY[9151] = OREMATS.Glauconite;
			OreDictMaterial.MATERIAL_ARRAY[9142] = Asbestos;
			OreDictMaterial.MATERIAL_ARRAY[9121] = MgCO3;
			OreDictMaterial.MATERIAL_ARRAY[9168] = Talc;
			OreDictMaterial.MATERIAL_ARRAY[8719] = Ge;
			OreDictMaterial.MATERIAL_ARRAY[8738] = NikolineAlloy;
			OreDictMaterial.MATERIAL_ARRAY[8339] = Nikolite;
			OreDictMaterial.MATERIAL_ARRAY[8359] = Nikolite;
			OreDictMaterial.MATERIAL_ARRAY[8510] = Stone;
			setOreMultiplier(Ad                      ,  2).setCrushing(Adamantine, U);
			setOreMultiplier(Fe                      ,  3).setCrushing(Fe2O3, U);
			setOreMultiplier(Al                      ,  2).setCrushing(Al2O3, U);
			setOreMultiplier(Ti                      ,  2).setCrushing(TiO2, U);
			setOreMultiplier(W                       ,  2).setCrushing(OREMATS.Scheelite, U);
			setOreMultiplier(U_238                   ,  2).setCrushing(OREMATS.Uraninite, U);
			setOreMultiplier(F                       ,  2).setCrushing(CaF2, U);
			setOreMultiplier(Ta                      ,  2).setCrushing(OREMATS.Tantalite, U);
			setOreMultiplier(Nb                      ,  2).setCrushing(OREMATS.Columbite, U);
			setOreMultiplier(Nq_528                  ,  2).setCrushing(Nq, U);
			setOreMultiplier(Nq_522                  ,  4).setCrushing(Nq, U);
			setOreMultiplier(Dilithium               ,  2).setCrushing(Dolamide, U);
			setOreMultiplier(Meteorite               ,  2);
			setOreMultiplier(MeteoricIron            ,  2);
			setOreMultiplier(MeteoricSteel           ,  2);
			setOreMultiplier(Amber                   ,  2);
			setOreMultiplier(AmberGolden             ,  2);
			setOreMultiplier(AmberDominican          ,  2);
			setOreMultiplier(Zircon                  ,  2);
			setOreMultiplier(Draconium               ,  2);
			setOreMultiplier(OREMATS.Borax           ,  2);
			setOreMultiplier(OREMATS.Cassiterite     ,  2);
			setOreMultiplier(OREMATS.Bastnasite      ,  3);
			setOreMultiplier(Monazite                ,  2);
			setOreMultiplier(Scabyst                 ,  2);
			setOreMultiplier(Phosphorus              ,  3);
			setOreMultiplier(PhosphorusBlue          ,  3);
			setOreMultiplier(PhosphorusRed           ,  3);
			setOreMultiplier(PhosphorusWhite         ,  3);
			setOreMultiplier(NaNO3                   ,  3);
			setOreMultiplier(KNO3                    ,  3);
		}
		private static void init1() { // upstream MT.java:None-None
			setOreMultiplier(Apatite                 ,  4);
			setOreMultiplier(Bone                    ,  4);
			setOreMultiplier(Lapis                   ,  5);
			setOreMultiplier(Sodalite                ,  5);
			setOreMultiplier(Lazurite                ,  5);
			setOreMultiplier(OREMATS.Malachite       ,  5);
			setOreMultiplier(Azurite                 ,  5);
			setOreMultiplier(Eudialyte               ,  5);
			setOreMultiplier(Moonstone               ,  2);
			setOreMultiplier(Sunstone                ,  4);
			setOreMultiplier(Chimerite               ,  3);
			setOreMultiplier(OREMATS.Perlite         ,  8);
			Empty                   .setOriginalMod(MD.MC.mID);
			Wood                    .setOriginalMod(MD.MC.mID);
			Stone                   .setOriginalMod(MD.MC.mID);
			Fe                      .setOriginalMod(MD.MC.mID);
			Au                      .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Diamond                 .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Emerald                 .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			NetherQuartz            .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Lapis                   .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Redstone                .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Glowstone               .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Coal                    .setOriginalMod(MD.MC.mID).put( COMMON_ORE);
			Charcoal                .setOriginalMod(MD.MC.mID);
			EnderPearl              .setOriginalMod(MD.MC.mID);
			EnderEye                .setOriginalMod(MD.MC.mID);
			Blaze                   .setOriginalMod(MD.MC.mID);
			Breeze                  .setOriginalMod(MD.MC.mID);
			Gunpowder               .setOriginalMod(MD.MC.mID);
			Sugar                   .setOriginalMod(MD.MC.mID);
			Cocoa                   .setOriginalMod(MD.MC.mID);
			Milk                    .setOriginalMod(MD.MC.mID);
			Paper                   .setOriginalMod(MD.MC.mID);
			Clay                    .setOriginalMod(MD.MC.mID);
			Netherrack              .setOriginalMod(MD.MC.mID);
			NetherBrick             .setOriginalMod(MD.MC.mID);
			SoulSand                .setOriginalMod(MD.MC.mID);
			NetherStar              .setOriginalMod(MD.MC.mID);
			Endstone                .setOriginalMod(MD.MC.mID);
		}
		private static void init2() { // upstream MT.java:None-None
			Bedrock                 .setOriginalMod(MD.MC.mID);
			Sand                    .setOriginalMod(MD.MC.mID);
			RedSand                 .setOriginalMod(MD.MC.mID);
			Glass                   .setOriginalMod(MD.MC.mID);
			H2O                     .setOriginalMod(MD.MC.mID);
			Snow                    .setOriginalMod(MD.MC.mID);
			Ice                     .setOriginalMod(MD.MC.mID);
			Bone                    .setOriginalMod(MD.MC.mID);
			Lava                    .setOriginalMod(MD.MC.mID);
			Flint                   .setOriginalMod(MD.MC.mID);
			Obsidian                .setOriginalMod(MD.MC.mID);
			Leather                 .setOriginalMod(MD.MC.mID);
			MeatRotten              .setOriginalMod(MD.MC.mID);
			Wheat                   .setOriginalMod(MD.MC.mID);
			Potato                  .setOriginalMod(MD.MC.mID);
			Black                   .setOriginalMod(MD.MC.mID);
			Red                     .setOriginalMod(MD.MC.mID);
			Green                   .setOriginalMod(MD.MC.mID);
			Brown                   .setOriginalMod(MD.MC.mID);
			Blue                    .setOriginalMod(MD.MC.mID);
			Purple                  .setOriginalMod(MD.MC.mID);
			Cyan                    .setOriginalMod(MD.MC.mID);
			LightGray               .setOriginalMod(MD.MC.mID);
			Gray                    .setOriginalMod(MD.MC.mID);
			Pink                    .setOriginalMod(MD.MC.mID);
			Lime                    .setOriginalMod(MD.MC.mID);
			Yellow                  .setOriginalMod(MD.MC.mID);
			LightBlue               .setOriginalMod(MD.MC.mID);
			Magenta                 .setOriginalMod(MD.MC.mID);
			Orange                  .setOriginalMod(MD.MC.mID);
			White                   .setOriginalMod(MD.MC.mID);
			Cu                      .setOriginalMod(MD.EtFu.mID).put( COMMON_ORE);
			STONES.Deepslate        .setOriginalMod(MD.EtFu.mID);
			STONES.Granite          .setOriginalMod(MD.EtFu.mID);
			STONES.Diorite          .setOriginalMod(MD.EtFu.mID);
			STONES.Andesite         .setOriginalMod(MD.EtFu.mID);
			PrismarineLight         .setOriginalMod(MD.EtFu.mID);
			PrismarineDark          .setOriginalMod(MD.EtFu.mID);
			NaCl                    .setOriginalMod(MD.HaC.mID).put( COMMON_ORE);
			WaxPlant                .setOriginalMod(MD.HaC.mID);
		}
		private static void init3() { // upstream MT.java:None-None
			Barley                  .setOriginalMod(MD.HaC.mID);
			Rye                     .setOriginalMod(MD.HaC.mID);
			Rice                    .setOriginalMod(MD.HaC.mID);
			Oat                     .setOriginalMod(MD.HaC.mID);
			Corn                    .setOriginalMod(MD.HaC.mID);
			Tofu                    .setOriginalMod(MD.HaC.mID);
			Chocolate               .setOriginalMod(MD.HaC.mID);
			Cinnamon                .setOriginalMod(MD.HaC.mID);
			Nutmeg                  .setOriginalMod(MD.HaC.mID);
			Peanut                  .setOriginalMod(MD.HaC.mID);
			Pistachio               .setOriginalMod(MD.HaC.mID);
			Almond                  .setOriginalMod(MD.HaC.mID);
			Vanilla                 .setOriginalMod(MD.HaC.mID);
			PepperBlack             .setOriginalMod(MD.HaC.mID);
			Curry                   .setOriginalMod(MD.HaC.mID);
			ButterSalted            .setOriginalMod(MD.HaC.mID);
			OliveOil                .setOriginalMod(MD.HaC.mID);
			NaHCO3                  .setOriginalMod(MD.Salt.mID);
			Butter                  .setOriginalMod(MD.GrC.mID);
			Netherite               .setOriginalMod(MD.NePl.mID).put( COMMON_ORE);
			NetherizedDiamond       .setOriginalMod(MD.NePl.mID);
			AncientDebris           .setOriginalMod(MD.NePl.mID).put( COMMON_ORE);
			Efrine                  .setOriginalMod(MD.NeLi.mID).put( COMMON_ORE);
			VoidCrystal             .setOriginalMod(MD.NeLi.mID).put( COMMON_ORE);
			Gloomstone              .setOriginalMod(MD.NeLi.mID).put( COMMON_ORE);
			OatAbyssal              .setOriginalMod(MD.NeLi.mID);
			STONES.Basalt           .setOriginalMod(MD.NeLi.mID);
			STONES.Blackstone       .setOriginalMod(MD.NeLi.mID);
			Sugilite                .setOriginalMod(MD.EnLi.mID).put( COMMON_ORE);
			EndSandWhite            .setOriginalMod(MD.EnLi.mID);
			EndSandBlack            .setOriginalMod(MD.EnLi.mID);
			Zn                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Be                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Th                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Li                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Phosphorus              .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Craponite               .setOriginalMod(MD.GT.mID);
			NitroCarbon             .setOriginalMod(MD.GT.mID);
			NitroFuel               .setOriginalMod(MD.GT.mID);
			SoylentGreen            .setOriginalMod(MD.GT.mID);
		}
		private static void init4() { // upstream MT.java:None-None
			ClayBrown               .setOriginalMod(MD.GT.mID);
			ClayRed                 .setOriginalMod(MD.GT.mID);
			Ceramic                 .setOriginalMod(MD.GT.mID);
			SluiceSand              .setOriginalMod(MD.GT.mID);
			SunflowerOil            .setOriginalMod(MD.GT.mID);
			NutOil                  .setOriginalMod(MD.GT.mID);
			LinOil                  .setOriginalMod(MD.GT.mID);
			HempOil                 .setOriginalMod(MD.GT.mID);
			Glue                    .setOriginalMod(MD.GT.mID);
			HolyWater               .setOriginalMod(MD.GT.mID);
			Nichrome                .setOriginalMod(MD.GT.mID);
			Kanthal                 .setOriginalMod(MD.GT.mID);
			VanadiumGallium         .setOriginalMod(MD.GT.mID);
			YttriumBariumCuprate    .setOriginalMod(MD.GT.mID);
			Graphene                .setOriginalMod(MD.GT.mID);
			Magnalium               .setOriginalMod(MD.GT.mID);
			BatteryAlloy            .setOriginalMod(MD.GT.mID);
			SolderingAlloy          .setOriginalMod(MD.GT.mID);
			AnnealedCopper          .setOriginalMod(MD.GT.mID);
			IronMagnetic            .setOriginalMod(MD.GT.mID);
			SteelMagnetic           .setOriginalMod(MD.GT.mID);
			NeodymiumMagnetic       .setOriginalMod(MD.GT.mID);
			CobaltBrass             .setOriginalMod(MD.GT.mID);
			Ultimet                 .setOriginalMod(MD.GT.mID);
			SteelGalvanized         .setOriginalMod(MD.GT.mID);
			StainlessSteel          .setOriginalMod(MD.GT.mID);
			TungstenSteel           .setOriginalMod(MD.GT.mID);
			NiobiumTitanium         .setOriginalMod(MD.GT.mID);
			Ta4HfC5                 .setOriginalMod(MD.GT.mID);
			Al2O3                   .setOriginalMod(MD.GT.mID);
			Osmiridium              .setOriginalMod(MD.GT.mID);
			UUAmplifier             .setOriginalMod(MD.GT.mID);
			Primitive               .setOriginalMod(MD.GT.mID);
			Good                    .setOriginalMod(MD.GT.mID);
			Data                    .setOriginalMod(MD.GT.mID);
			Master                  .setOriginalMod(MD.GT.mID);
			Vb                      .setOriginalMod(MD.GT.mID).put( BETWEENLANDS,  MAZEBREAKER);
			VibraniumSilver         .setOriginalMod(MD.GT.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Ad                      .setOriginalMod(MD.GT.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Vibramantium            .setOriginalMod(MD.GT.mID).put( BETWEENLANDS,  MAZEBREAKER);
		}
		private static void init5() { // upstream MT.java:None-None
			VibraniumSteel          .setOriginalMod(MD.GT.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Dn                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			DuraniumAlloy           .setOriginalMod(MD.GT.mID);
			Ke                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Trinitanium             .setOriginalMod(MD.GT.mID);
			Nq                      .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Nq_522                  .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			Nq_528                  .setOriginalMod(MD.GT.mID).put( COMMON_ORE);
			HSSG                    .setOriginalMod(MD.GT5U.mID);
			HSSE                    .setOriginalMod(MD.GT5U.mID);
			HSSS                    .setOriginalMod(MD.GT5U.mID);
			PlatinumGroupSludge     .setOriginalMod(MD.GT5U.mID);
			Superconductor          .put(COMMON_ORE).setOriginalMod("rocketscience");
			Os                      .put(COMMON_ORE).setOriginalMod("gravisuite");
			Sn                      .setOriginalMod(MD.IC2.mID).put( COMMON_ORE);
			Bronze                  .setOriginalMod(MD.IC2.mID);
			RefinedIron             .setOriginalMod(MD.IC2.mID);
			Ir                      .setOriginalMod(MD.IC2.mID).put( COMMON_ORE);
			U_238                   .setOriginalMod(MD.IC2.mID);
			U_235                   .setOriginalMod(MD.IC2.mID);
			Pu                      .setOriginalMod(MD.IC2.mID);
			DistWater               .setOriginalMod(MD.IC2.mID);
			SiO2                    .setOriginalMod(MD.IC2.mID);
			EnergiumRed             .setOriginalMod(MD.IC2.mID);
			ConstructionFoam        .setOriginalMod(MD.IC2.mID);
			UUMatter                .setOriginalMod(MD.IC2.mID);
			HydratedCoal            .setOriginalMod(MD.IC2.mID);
			Coffee                  .setOriginalMod(MD.IC2.mID);
			Rubber                  .setOriginalMod(MD.IC2.mID);
			WoodRubber              .setOriginalMod(MD.IC2.mID);
			Advanced                .setOriginalMod(MD.IC2.mID);
			SiC                     .setOriginalMod(MD.IHL.mID);
			H2Ca2B2Si2O10           .setOriginalMod(MD.IHL.mID);
			H3BO3                   .setOriginalMod(MD.IHL.mID);
			Li2O                    .setOriginalMod(MD.IHL.mID);
			NaOH                    .setOriginalMod(MD.IHL.mID);
			NaHSO4                  .setOriginalMod(MD.IHL.mID);
			H2O2                    .setOriginalMod(MD.IHL.mID);
			Li2Fe2O4                .setOriginalMod(MD.IHL.mID);
			Porcelain               .setOriginalMod(MD.IHL.mID);
		}
		private static void init6() { // upstream MT.java:None-None
			Oil                     .setOriginalMod(MD.BC.mID);
			Fuel                    .setOriginalMod(MD.BC.mID);
			I                       .setOriginalMod(MD.FR.mID);
			Ash                     .setOriginalMod(MD.FR.mID);
			Peat                    .setOriginalMod(MD.FR.mID);
			PeatBituminous          .setOriginalMod(MD.FR.mID);
			Apatite                 .setOriginalMod(MD.FR.mID);
			PhosphorusBlue          .setOriginalMod(MD.FR.mID);
			Biomass                 .setOriginalMod(MD.FR.mID);
			BioFuel                 .setOriginalMod(MD.FR.mID);
			Ethanol                 .setOriginalMod(MD.FR.mID);
			SeedOil                 .setOriginalMod(MD.FR.mID);
			Honey                   .setOriginalMod(MD.FR.mID);
			Honeydew                .setOriginalMod(MD.FR.mID);
			Wax                     .setOriginalMod(MD.FR.mID);
			WaxBee                  .setOriginalMod(MD.FR.mID);
			WaxRefractory           .setOriginalMod(MD.FR.mID);
			WaxMagic                .setOriginalMod(MD.FRMB.mID);
			WaxAmnesic              .setOriginalMod(MD.FRMB.mID);
			WaxSoulful              .setOriginalMod(MD.FRMB.mID);
			Bark                    .setOriginalMod(MD.BINNIE.mID);
			Hazelnut                .setOriginalMod(MD.BINNIE.mID);
			Bi                      .setOriginalMod(MD.TFC.mID).put( COMMON_ORE);
			Jasper                  .setOriginalMod(MD.TFC.mID);
			WroughtIron             .setOriginalMod(MD.TFC.mID);
			RoseGold                .setOriginalMod(MD.TFC.mID);
			SterlingSilver          .setOriginalMod(MD.TFC.mID);
			BlackBronze             .setOriginalMod(MD.TFC.mID);
			BismuthBronze           .setOriginalMod(MD.TFC.mID);
			BlackSteel              .setOriginalMod(MD.TFC.mID);
			RedSteel                .setOriginalMod(MD.TFC.mID);
			BlueSteel               .setOriginalMod(MD.TFC.mID);
			MeteoricBlackSteel      .setOriginalMod(MD.TFC.mID);
			MeteoricBlueSteel       .setOriginalMod(MD.TFC.mID);
			MeteoricRedSteel        .setOriginalMod(MD.TFC.mID);
			STONES.Mazestone        .setOriginalMod(MD.TF.mID);
			STONES.Castlerock       .setOriginalMod(MD.TF.mID);
			STONES.Deadrock         .setOriginalMod(MD.TF.mID);
			LiveRoot                .setOriginalMod(MD.TF.mID);
			IronWood                .setOriginalMod(MD.TF.mID);
		}
		private static void init7() { // upstream MT.java:None-None
			Steeleaf                .setOriginalMod(MD.TF.mID);
			Knightmetal             .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			FierySteel              .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			Fireleaf                .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			MeteoflameSteel         .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			MeteoflameBlackSteel    .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			MeteoflameBlueSteel     .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			MeteoflameRedSteel      .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			FlamascusSteel          .setOriginalMod(MD.TF.mID).put( MAZEBREAKER);
			STONES.Umber            .setOriginalMod(MD.ERE.mID);
			STONES.Gneiss           .setOriginalMod(MD.ERE.mID);
			PetrifiedWood           .setOriginalMod(MD.ERE.mID);
			Jade                    .setOriginalMod(MD.ERE.mID);
			K                       .setOriginalMod(MD.RC.mID);
			S                       .setOriginalMod(MD.RC.mID).put( COMMON_ORE);
			KNO3                    .setOriginalMod(MD.RC.mID).put( COMMON_ORE);
			Firestone               .setOriginalMod(MD.RC.mID).put( COMMON_ORE);
			Creosote                .setOriginalMod(MD.RC.mID);
			TinAlloy                .setOriginalMod(MD.RC.mID);
			Steel                   .setOriginalMod(MD.RC.mID);
			CoalCoke                .setOriginalMod(MD.RC.mID);
			Constantan              .setOriginalMod(MD.IE.mID);
			WoodTreated             .setOriginalMod(MD.IE.mID);
			Ni                      .setOriginalMod(MD.TE.mID).put( COMMON_ORE);
			Pt                      .setOriginalMod(MD.TE.mID).put( COMMON_ORE);
			Invar                   .setOriginalMod(MD.TE.mID);
			Electrum                .setOriginalMod(MD.TE.mID);
			Enderium                .setOriginalMod(MD.TE.mID);
			Signalum                .setOriginalMod(MD.TE.mID);
			Lumium                  .setOriginalMod(MD.TE.mID);
			RareEarth               .setOriginalMod(MD.TE.mID).put( COMMON_ORE);
			Niter                   .setOriginalMod(MD.TE.mID).put( COMMON_ORE);
			Basalz                  .setOriginalMod(MD.TE.mID);
			Blitz                   .setOriginalMod(MD.TE.mID);
			Blizz                   .setOriginalMod(MD.TE.mID);
			Petrotheum              .setOriginalMod(MD.TE.mID);
			Aerotheum               .setOriginalMod(MD.TE.mID);
			Pyrotheum               .setOriginalMod(MD.TE.mID);
			Cryotheum               .setOriginalMod(MD.TE.mID);
			STONES.SkyStone         .setOriginalMod(MD.AE.mID);
		}
		private static void init8() { // upstream MT.java:None-None
			Si                      .setOriginalMod(MD.AE.mID);
			CertusQuartz            .setOriginalMod(MD.AE.mID).put( COMMON_ORE);
			ChargedCertusQuartz     .setOriginalMod(MD.AE.mID).put( COMMON_ORE);
			Fluix                   .setOriginalMod(MD.AE.mID).put( COMMON_ORE);
			IronCompressed          .setOriginalMod(MD.PnC.mID);
			IronCast                .setOriginalMod(MD.SC2.mID);
			WhaleOil                .setOriginalMod(MD.SC2.mID);
			Al                      .setOriginalMod(MD.TiC.mID);
			Co                      .setOriginalMod(MD.TiC.mID).put( COMMON_ORE);
			Ardite                  .setOriginalMod(MD.TiC.mID).put( COMMON_ORE);
			Alumite                 .setOriginalMod(MD.TiC.mID);
			Manyullyn               .setOriginalMod(MD.TiC.mID).put( BETWEENLANDS);
			AluminiumBrass          .setOriginalMod(MD.TiC.mID);
			BlackQuartz             .setOriginalMod(MD.AA.mID);
			RedstoneAlloy           .setOriginalMod(MD.EIO.mID);
			EnderiumBase            .setOriginalMod(MD.EIO.mID);
			PulsatingIron           .setOriginalMod(MD.EIO.mID);
			ConductiveIron          .setOriginalMod(MD.EIO.mID);
			EnergeticAlloy          .setOriginalMod(MD.EIO.mID);
			VibrantAlloy            .setOriginalMod(MD.EIO.mID);
			ElectricalSteel         .setOriginalMod(MD.EIO.mID);
			Soularium               .setOriginalMod(MD.EIO.mID);
			CrudeSteel              .setOriginalMod(MD.EIO.mID);
			CrystallineAlloy        .setOriginalMod(MD.EIO.mID);
			CrystallinePinkSlime    .setOriginalMod(MD.EIO.mID);
			EndSteel                .setOriginalMod(MD.EIO.mID);
			EnergeticSilver         .setOriginalMod(MD.EIO.mID);
			MelodicAlloy            .setOriginalMod(MD.EIO.mID);
			StellarAlloy            .setOriginalMod(MD.EIO.mID);
			VividAlloy              .setOriginalMod(MD.EIO.mID);
			MeatRaw                 .setOriginalMod(MD.MFR.mID);
			MeatCooked              .setOriginalMod(MD.MFR.mID);
			Plastic                 .setOriginalMod(MD.MFR.mID);
			Yellorium               .setOriginalMod(MD.BR.mID).put( COMMON_ORE);
			Blutonium               .setOriginalMod(MD.BR.mID).put( COMMON_ORE);
			Cyanite                 .setOriginalMod(MD.BR.mID).put( COMMON_ORE);
			Ludicrite               .setOriginalMod(MD.BR.mID).put( COMMON_ORE);
			Yellorite               .setOriginalMod(MD.BR.mID).put( COMMON_ORE);
			Pu_238                  .setOriginalMod(MD.HBM.mID);
			Pu_240                  .setOriginalMod(MD.HBM.mID);
		}
		private static void init9() { // upstream MT.java:None-None
			Mingrade                .setOriginalMod(MD.HBM.mID);
			PhosphorusRed           .setOriginalMod(MD.HBM.mID);
			PhosphorusWhite         .setOriginalMod(MD.HBM.mID);
			Alexandrite             .setOriginalMod(MD.HBM.mID);
			Asbestos                .setOriginalMod(MD.HBM.mID).put( COMMON_ORE);
			OREMATS.Columbite       .setOriginalMod(MD.HBM.mID).put( COMMON_ORE);
			OREMATS.Tantalite       .setOriginalMod(MD.HBM.mID).put( COMMON_ORE);
			OREMATS.Coltan          .setOriginalMod(MD.HBM.mID).put( COMMON_ORE);
			Ta                      .setOriginalMod(MD.HBM.mID);
			Nb                      .setOriginalMod(MD.HBM.mID);
			Nd                      .setOriginalMod(MD.HBM.mID);
			DeshAlloy               .setOriginalMod(MD.HBM.mID);
			PVC                     .setOriginalMod(MD.HBM.mID);
			Teflon                  .setOriginalMod(MD.HBM.mID);
			Bakelite                .setOriginalMod(MD.HBM.mID);
			Polycarbonate           .setOriginalMod(MD.HBM.mID);
			In                      .setOriginalMod(MD.ReC.mID);
			TungstenCarbide         .setOriginalMod(MD.ReC.mID);
			Anthracite              .setOriginalMod(MD.RoC.mID).put( COMMON_ORE);
			Prismane                .setOriginalMod(MD.RoC.mID);
			Lonsdaleite             .setOriginalMod(MD.RoC.mID);
			Lubricant               .setOriginalMod(MD.RoC.mID);
			F                       .setOriginalMod(MD.RoC.mID);
			CaF2                    .setOriginalMod(MD.RoC.mID);
			AgI                     .setOriginalMod(MD.RoC.mID);
			InductiveAlloy          .setOriginalMod(MD.RoC.mID);
			Prismane                .setOriginalMod(MD.RoC.mID);
			Lonsdaleite             .setOriginalMod(MD.RoC.mID);
			Cd_In_Ag_Alloy          .setOriginalMod(MD.RoC.mID);
			HSLA                    .setOriginalMod(MD.RoC.mID);
			SpringSteel             .setOriginalMod(MD.RoC.mID);
			AluminiumAlloy          .setOriginalMod(MD.RoC.mID);
			TungstenAlloy           .setOriginalMod(MD.RoC.mID);
			TungstenSintered        .setOriginalMod(MD.RoC.mID);
			Bedrock_HSLA_Alloy      .setOriginalMod(MD.RoC.mID).put( BETWEENLANDS,  MAZEBREAKER);
			RefinedGlowstone        .setOriginalMod(MD.Mek.mID);
			RefinedObsidian         .setOriginalMod(MD.Mek.mID);
			Ge                      .setOriginalMod(MD.Mek.mID).put( COMMON_ORE);
			Basic                   .setOriginalMod(MD.Mek.mID);
			Elite                   .setOriginalMod(MD.Mek.mID);
		}
		private static void init10() { // upstream MT.java:None-None
			InfusedVis              .setOriginalMod(MD.TC.mID);
			Silverwood              .setOriginalMod(MD.TC.mID);
			Greatwood               .setOriginalMod(MD.TC.mID);
			Tallow                  .setOriginalMod(MD.TC.mID);
			VoidMetal               .setOriginalMod(MD.TC.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Thaumium                .setOriginalMod(MD.TC.mID).put( COMMON_ORE);
			Amber                   .setOriginalMod(MD.TC.mID).put( COMMON_ORE);
			Hg                      .setOriginalMod(MD.TC.mID).put( COMMON_ORE);
			OREMATS.Cinnabar        .setOriginalMod(MD.TC.mID).put( COMMON_ORE);
			DarkThaumium            .setOriginalMod(MD.TCTE.mID);
			Livingwood              .setOriginalMod(MD.BOTA.mID);
			STONES.Livingrock       .setOriginalMod(MD.BOTA.mID);
			Dreamwood               .setOriginalMod(MD.BOTA.mID);
			Shimmerwood             .setOriginalMod(MD.BOTA.mID);
			SunnyQuartz             .setOriginalMod(MD.BOTA.mID);
			LavenderQuartz          .setOriginalMod(MD.BOTA.mID);
			RedQuartz               .setOriginalMod(MD.BOTA.mID);
			BlazeQuartz             .setOriginalMod(MD.BOTA.mID);
			SmokeyQuartz            .setOriginalMod(MD.BOTA.mID);
			ManaQuartz              .setOriginalMod(MD.BOTA.mID);
			ElvenQuartz             .setOriginalMod(MD.BOTA.mID);
			Manasteel               .setOriginalMod(MD.BOTA.mID);
			ManaDiamond             .setOriginalMod(MD.BOTA.mID);
			ElvenElementium         .setOriginalMod(MD.BOTA.mID);
			ElvenDragonstone        .setOriginalMod(MD.BOTA.mID);
			Terrasteel              .setOriginalMod(MD.BOTA.mID);
			GaiaSpirit              .setOriginalMod(MD.BOTA.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Mauftrium               .setOriginalMod(MD.ALF.mID);
			Elvorium                .setOriginalMod(MD.ALF.mID);
			MuspelheimPower         .setOriginalMod(MD.ALF.mID);
			NiflheimPower           .setOriginalMod(MD.ALF.mID);
			Iffesal                 .setOriginalMod(MD.ALF.mID);
			PEZ                     .setOriginalMod(MD.CANDY.mID);
			Licorice                .setOriginalMod(MD.CANDY.mID);
			Nougat                  .setOriginalMod(MD.CANDY.mID);
			Marshmallow             .setOriginalMod(MD.CANDY.mID);
			Iritanium               .setOriginalMod(MD.GC_ADV_ROCKETRY.mID);
			TitaniumAluminide       .setOriginalMod(MD.GC_ADV_ROCKETRY.mID);
			Endium                  .setOriginalMod(MD.HEE.mID).put( COMMON_ORE);
			OREMATS.Sphalerite      .setOriginalMod(MD.HEE.mID).put( COMMON_ORE);
		}
		private static void init11() { // upstream MT.java:None-None
			Ti                      .setOriginalMod(MD.MaCu.mID);
			TiO2                    .setOriginalMod(MD.MaCu.mID).put( COMMON_ORE);
			FishCooked              .setOriginalMod(MD.MaCu.mID);
			FishRaw                 .setOriginalMod(MD.MaCu.mID);
			FishRotten              .setOriginalMod(MD.MaCu.mID);
			FishOil                 .setOriginalMod(MD.MaCu.mID);
			An                      .setOriginalMod(MD.ABYSSAL.mID).put( COMMON_ORE);
			Cor                     .setOriginalMod(MD.ABYSSAL.mID).put( COMMON_ORE);
			Dr                      .setOriginalMod(MD.ABYSSAL.mID).put( COMMON_ORE);
			Etx                     .setOriginalMod(MD.ABYSSAL.mID).put( COMMON_ORE);
			AmberDominican          .setOriginalMod(MD.Fossil.mID).put( COMMON_ORE);
			Draconium               .setOriginalMod(MD.DE.mID).put( COMMON_ORE);
			DraconiumAwakened       .setOriginalMod(MD.DE.mID).put( BETWEENLANDS,  MAZEBREAKER);
			CrystalMatrix           .setOriginalMod(MD.AV.mID);
			CosmicNeutronium        .setOriginalMod(MD.AV.mID);
			Infinity                .setOriginalMod(MD.AV.mID).put( BETWEENLANDS,  MAZEBREAKER);
			DarkMatter              .setOriginalMod(MD.PE.mID).put( BETWEENLANDS,  MAZEBREAKER);
			RedMatter               .setOriginalMod(MD.PE.mID).put( BETWEENLANDS,  MAZEBREAKER);
			Zr                      .setOriginalMod(MD.TROPIC.mID);
			Zircon                  .setOriginalMod(MD.TROPIC.mID);
			Azurite                 .setOriginalMod(MD.TROPIC.mID);
			Eudialyte               .setOriginalMod(MD.TROPIC.mID);
			Topaz                   .setOriginalMod(MD.BoP.mID).put( COMMON_ORE);
			Peridot                 .setOriginalMod(MD.BoP.mID).put( COMMON_ORE);
			Amethyst                .setOriginalMod(MD.BoP.mID).put( COMMON_ORE);
			EnderAmethyst           .setOriginalMod(MD.BoP.mID).put( COMMON_ORE);
			Meteorite               .setOriginalMod(MD.FM.mID).put( COMMON_ORE);
			FrozenIron              .setOriginalMod(MD.FM.mID);
			Kreknorite              .setOriginalMod(MD.FM.mID);
			RedMeteor               .setOriginalMod(MD.FM.mID);
			Frezarite               .setOriginalMod(MD.FM.mID);
			Vinteum                 .setOriginalMod(MD.ARS.mID).put( COMMON_ORE);
			VinteumPurified         .setOriginalMod(MD.ARS.mID);
			ArcaneAsh               .setOriginalMod(MD.ARS.mID);
			ArcaneCompound          .setOriginalMod(MD.ARS.mID);
			Moonstone               .setOriginalMod(MD.ARS.mID).put( COMMON_ORE);
			Sunstone                .setOriginalMod(MD.ARS.mID).put( COMMON_ORE);
			Chimerite               .setOriginalMod(MD.ARS.mID).put( COMMON_ORE);
			BlueTopaz               .setOriginalMod(MD.ARS.mID).put( COMMON_ORE);
			MeteoricIron            .setOriginalMod(MD.GC.mID);
		}
		private static void init12() { // upstream MT.java:None-None
			MeteoricSteel           .setOriginalMod(MD.GC.mID);
			Desh                    .setOriginalMod(MD.GC.mID).put( COMMON_ORE);
			Cheese                  .setOriginalMod(MD.GC.mID).put( COMMON_ORE);
			STONES.MoonTurf         .setOriginalMod(MD.GC.mID);
			STONES.MoonRock         .setOriginalMod(MD.GC.mID);
			STONES.MarsSand         .setOriginalMod(MD.GC.mID);
			STONES.MarsRock         .setOriginalMod(MD.GC.mID);
			STONES.SpaceRock        .setOriginalMod(MD.GC.mID);
			Ultimate                .setOriginalMod(MD.GC.mID);
			DiamondBlue             .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			DiamondGreen            .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			DiamondPurple           .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			DiamondRed              .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			DiamondYellow           .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.PhobosRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.DeimosRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.MercuryRock      .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.VenusRock        .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.CeresRock        .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.JupiterRock      .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.IoRock           .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.EuropaRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.GanymedeRock     .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.CallistoRock     .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.SaturnRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.RheaRock         .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.TitanRock        .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.OberonRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.IapetusRock      .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.UranusRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.TitaniaRock      .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.NeptuneRock      .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.TritonRock       .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.PlutoRock        .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.ErisRock         .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			STONES.Kepler22bRock    .setOriginalMod(MD.GC_EXTRAPLANETS.mID);
			Duralumin               .setOriginalMod(MD.GC_GALAXYSPACE.mID).put( COMMON_ORE);
			Oriharukon              .setOriginalMod(MD.GC_GALAXYSPACE.mID).put( COMMON_ORE);
			Adamantite              .setOriginalMod(MD.GC_GALAXYSPACE.mID).put( COMMON_ORE);
			GlowstoneCeres          .setOriginalMod(MD.GC_GALAXYSPACE.mID);
		}
		private static void init13() { // upstream MT.java:None-None
			GlowstoneIo             .setOriginalMod(MD.GC_GALAXYSPACE.mID);
			GlowstoneEnceladus      .setOriginalMod(MD.GC_GALAXYSPACE.mID);
			GlowstoneProteus        .setOriginalMod(MD.GC_GALAXYSPACE.mID);
			GlowstonePluto          .setOriginalMod(MD.GC_GALAXYSPACE.mID);
			Tn                      .setOriginalMod(MD.MO.mID);
			TritaniumAlloy          .setOriginalMod(MD.MO.mID).put( COMMON_ORE);
			Dilithium               .setOriginalMod(MD.MO.mID);
			Dolamide                .setOriginalMod(MD.MO.mID);
			SpectreIron             .setOriginalMod(MD.RT.mID);
			Ectoplasm               .setOriginalMod(MD.RT.mID);
			Unstable                .setOriginalMod(MD.ExU.mID).put( BETWEENLANDS);
			Bedrockium              .setOriginalMod(MD.ExU.mID).put( BETWEENLANDS);
			CrimsonMiddle           .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			GreenMiddle             .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			AquaMiddle              .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			Valonite                .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			Scabyst                 .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			SlimyBone               .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			STONES.Betweenstone     .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			STONES.Pitstone         .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			STONES.Cragrock         .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			STONES.Templerock       .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			Weedwood                .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS);
			Syrmorite               .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS,  COMMON_ORE);
			Octine                  .setOriginalMod(MD.BTL.mID).put( BETWEENLANDS,  COMMON_ORE);
			Skyroot                 .setOriginalMod(MD.AETHER.mID);
			STONES.Holystone        .setOriginalMod(MD.AETHER.mID);
			Zanite                  .setOriginalMod(MD.AETHER.mID).put( COMMON_ORE);
			AmberGolden             .setOriginalMod(MD.AETHER.mID).put( COMMON_ORE);
			Ambrosium               .setOriginalMod(MD.AETHER.mID).put( COMMON_ORE);
			Gravitite               .setOriginalMod(MD.AETHER.mID).put( COMMON_ORE);
			Continuum               .setOriginalMod(MD.AETHER.mID).put( COMMON_ORE);
			W                       .setOriginalMod(MD.RP.mID);
			Ag                      .setOriginalMod(MD.RP.mID).put( COMMON_ORE);
			Indigo                  .setOriginalMod(MD.RP.mID);
			Sapphire                .setOriginalMod(MD.RP.mID);
			GreenSapphire           .setOriginalMod(MD.RP.mID);
			BlueSapphire            .setOriginalMod(MD.RP.mID);
			Ruby                    .setOriginalMod(MD.RP.mID);
			BalasRuby               .setOriginalMod(MD.RP.mID);
		}
		private static void init14() { // upstream MT.java:None-None
			STONES.Marble           .setOriginalMod(MD.RP.mID);
			Brass                   .setOriginalMod(MD.RP.mID);
			RedAlloy                .setOriginalMod(MD.RP.mID);
			Nikolite                .setOriginalMod(MD.RP.mID).put( COMMON_ORE);
			NikolineAlloy           .setOriginalMod(MD.RP.mID);
			BlueAlloy               .setOriginalMod(MD.RP.mID);
			ElectrotineAlloy        .setOriginalMod(MD.PR.mID);
			PurpleAlloy             .setOriginalMod(MD.BP.mID);
			Pb                      .setOriginalMod(MD.FZ.mID).put( COMMON_ORE);
			OREMATS.Galena          .setOriginalMod(MD.FZ.mID).put( COMMON_ORE);
			H2SO4                   .setOriginalMod(MD.FZ.mID);
			AquaRegia               .setOriginalMod(MD.FZ.mID);
			DarkIron                .setOriginalMod(MD.FZ.mID);
			Bentonite                  .setOriginalMod(MD.PFAA.mID);
			Palygorskite               .setOriginalMod(MD.PFAA.mID);
			Kaolinite                  .setOriginalMod(MD.PFAA.mID);
			OREMATS.BasalticMineralSand.setOriginalMod(MD.PFAA.mID);
			OREMATS.GraniticMineralSand.setOriginalMod(MD.PFAA.mID);
			Lignite                 .setOriginalMod(MD.UB.mID).put( COMMON_ORE);
			Angmallen               .setOriginalMod(MD.MET.mID);
			Hepatizon               .setOriginalMod(MD.MET.mID);
			DamascusSteel           .setOriginalMod(MD.MET.mID);
			Aredrite                .setOriginalMod(MD.MET.mID);
			Atl                     .setOriginalMod(MD.MET.mID);
			Tartarite               .setOriginalMod(MD.MET.mID);
			Adamantine              .setOriginalMod(MD.MET.mID);
			AstralSilver            .setOriginalMod(MD.MET.mID);
			Mithril                 .setOriginalMod(MD.MET.mID);
			Infuscolium             .setOriginalMod(MD.MET.mID);
			Rubracium               .setOriginalMod(MD.MET.mID);
			Oureclase               .setOriginalMod(MD.MET.mID);
			Orichalcum              .setOriginalMod(MD.MET.mID);
			Carmot                  .setOriginalMod(MD.MET.mID);
			Prometheum              .setOriginalMod(MD.MET.mID);
			DeepIron                .setOriginalMod(MD.MET.mID);
			Haderoth                .setOriginalMod(MD.MET.mID);
			Celenegil               .setOriginalMod(MD.MET.mID);
			Meutoite                .setOriginalMod(MD.MET.mID);
			Eximite                 .setOriginalMod(MD.MET.mID);
		}
		private static void init15() { // upstream MT.java:None-None
			Desichalkos             .setOriginalMod(MD.MET.mID);
			Midasium                .setOriginalMod(MD.MET.mID);
			Alduorite               .setOriginalMod(MD.MET.mID);
			Lemurite                .setOriginalMod(MD.MET.mID);
			Ceruclase               .setOriginalMod(MD.MET.mID);
			Kalendrite              .setOriginalMod(MD.MET.mID);
			Sanguinite              .setOriginalMod(MD.MET.mID);
			Vyroxeres               .setOriginalMod(MD.MET.mID);
			Ignatius                .setOriginalMod(MD.MET.mID);
			Vulcanite               .setOriginalMod(MD.MET.mID);
			ShadowIron              .setOriginalMod(MD.MET.mID);
			ShadowSteel             .setOriginalMod(MD.MET.mID);
			Inolashite              .setOriginalMod(MD.MET.mID);
			Amordrine               .setOriginalMod(MD.MET.mID);
			Force                   .put(COMMON_ORE);
			Forcicium               .put(COMMON_ORE);
			Forcillium              .put(COMMON_ORE);
			Plastic                 .addEnchantmentForDamage("knockback", 1).addEnchantmentForRanged("punch", 1);
			Bakelite                .addEnchantmentForDamage("knockback", 1).addEnchantmentForRanged("punch", 1);
			Teflon                  .addEnchantmentForDamage("knockback", 1).addEnchantmentForRanged("punch", 1);
			PVC                     .addEnchantmentForDamage("knockback", 2).addEnchantmentForRanged("punch", 2);
			Polycarbonate           .addEnchantmentForDamage("knockback", 2).addEnchantmentForRanged("punch", 2);
			Rubber                  .addEnchantmentForDamage("knockback", 2).addEnchantmentForRanged("punch", 2);
			Kalendrite              .addEnchantmentForDamage("knockback", 2).addEnchantmentForRanged("punch", 2);
			InfusedAir              .addEnchantmentForDamage("knockback", 2).addEnchantmentForRanged("punch", 2);
			Blitz                   .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			Gravitite               .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			DarkIron                .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			Tartarite               .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			DarkMatter              .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			RedMeteor               .addEnchantmentForDamage("knockback", 3).addEnchantmentForRanged("punch", 3);
			Infinity                .addEnchantmentForDamage("knockback",10).addEnchantmentForRanged("punch",10);
			Skyroot                 .addEnchantmentForTools("fortune", 1).addEnchantmentForWeapons("looting", 1).addEnchantmentForAmmo("looting", 2).addEnchantmentForRanged("infinity", 1).addEnchantmentForFishing("luckOfTheSea", 1);
			IronWood                .addEnchantmentForTools("fortune", 1).addEnchantmentForWeapons("looting", 1).addEnchantmentForAmmo("looting", 2).addEnchantmentForRanged("infinity", 1).addEnchantmentForFishing("luckOfTheSea", 1);
			Steeleaf                .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			Fireleaf                .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Efrine                  .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			Soularium               .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			Midasium                .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			Mithril                 .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
		}
		private static void init16() { // upstream MT.java:None-None
			ElvenQuartz             .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Vinteum                 .addEnchantmentForTools("fortune", 1).addEnchantmentForWeapons("looting", 1).addEnchantmentForAmmo("looting", 2).addEnchantmentForRanged("infinity", 1).addEnchantmentForFishing("luckOfTheSea", 1);
			Manasteel               .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			Thaumium                .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			DarkThaumium            .addEnchantmentForTools("fortune", 2).addEnchantmentForWeapons("looting", 2).addEnchantmentForAmmo("looting", 4).addEnchantmentForRanged("infinity", 2).addEnchantmentForFishing("luckOfTheSea", 2);
			VoidMetal               .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			InfusedWater            .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Eximite                 .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			DarkMatter              .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			RedMatter               .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Chimerite               .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Jade                    .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Sugilite                .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			EnderAmethyst           .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Continuum               .addEnchantmentForTools("fortune", 3).addEnchantmentForWeapons("looting", 3).addEnchantmentForAmmo("looting", 6).addEnchantmentForRanged("infinity", 3).addEnchantmentForFishing("luckOfTheSea", 3);
			Basalz                  .addEnchantmentForTools("fortune", 4).addEnchantmentForWeapons("looting", 4).addEnchantmentForAmmo("looting", 8).addEnchantmentForRanged("infinity", 4).addEnchantmentForFishing("luckOfTheSea", 4);
			Carminite               .addEnchantmentForTools("fortune", 4).addEnchantmentForWeapons("looting", 4).addEnchantmentForAmmo("looting", 8).addEnchantmentForRanged("infinity", 4).addEnchantmentForFishing("luckOfTheSea", 4);
			Ma                      .addEnchantmentForTools("fortune", 4).addEnchantmentForWeapons("looting", 4).addEnchantmentForAmmo("looting", 8).addEnchantmentForRanged("infinity", 4).addEnchantmentForFishing("luckOfTheSea", 4);
			Haderoth                .addEnchantmentForTools("fortune", 4).addEnchantmentForWeapons("looting", 4).addEnchantmentForAmmo("looting", 8).addEnchantmentForRanged("infinity", 4).addEnchantmentForFishing("luckOfTheSea", 4);
			VibraniumSteel          .addEnchantmentForTools("fortune", 5).addEnchantmentForWeapons("looting", 5).addEnchantmentForAmmo("looting",10).addEnchantmentForRanged("infinity", 5).addEnchantmentForFishing("luckOfTheSea", 5);
			Vibramantium            .addEnchantmentForTools("fortune", 5).addEnchantmentForWeapons("looting", 5).addEnchantmentForAmmo("looting",10).addEnchantmentForRanged("infinity", 5).addEnchantmentForFishing("luckOfTheSea", 5);
			Vb                      .addEnchantmentForTools("fortune", 5).addEnchantmentForWeapons("looting", 5).addEnchantmentForAmmo("looting",10).addEnchantmentForRanged("infinity", 5).addEnchantmentForFishing("luckOfTheSea", 5);
			Infinity                .addEnchantmentForTools("fortune",10).addEnchantmentForWeapons("looting",10).addEnchantmentForAmmo("looting",20).addEnchantmentForRanged("infinity",10).addEnchantmentForFishing("luckOfTheSea",10);
			Ad                      .addEnchantmentForTools("silkTouch", 1);
			Adamantine              .addEnchantmentForTools("silkTouch", 1);
			Force                   .addEnchantmentForTools("silkTouch", 1);
			Amber                   .addEnchantmentForTools("silkTouch", 1);
			AmberGolden             .addEnchantmentForTools("silkTouch", 1);
			AmberDominican          .addEnchantmentForTools("silkTouch", 1);
			Ambrosium               .addEnchantmentForTools("silkTouch", 1);
			ManaQuartz              .addEnchantmentForTools("silkTouch", 1);
			Blizz                   .addEnchantmentForTools("silkTouch", 1);
			Frezarite               .addEnchantmentForTools("silkTouch", 1);
			Inolashite              .addEnchantmentForTools("silkTouch", 1);
			Sanguinite              .addEnchantmentForTools("silkTouch", 1);
			NetherStar              .addEnchantmentForTools("silkTouch", 1);
			InfusedOrder            .addEnchantmentForTools("silkTouch", 1);
			InfusedBalance          .addEnchantmentForTools("silkTouch", 1);
			NiflheimPower           .addEnchantmentForTools("silkTouch", 1);
			Vibramantium            .addEnchantmentForTools("silkTouch", 1);
		}
		private static void init17() { // upstream MT.java:None-None
			Infinity                .addEnchantmentForTools("silkTouch",10);
			EnderPearl              .addEnchantmentForTools("silkTouch", 1).addEnchantmentForRanged("infinity", 1);
			Enderium                .addEnchantmentForTools("silkTouch", 1).addEnchantmentForRanged("infinity", 2);
			Endium                  .addEnchantmentForTools("silkTouch", 1).addEnchantmentForRanged("infinity", 3);
			SpectreIron             .addEnchantmentForTools("silkTouch", 1).addEnchantmentForRanged("infinity", 3);
			Flint                   .addEnchantmentForDamage("fireAspect", 1).addEnchantmentForRanged("flame", 1);
			Netherrack              .addEnchantmentForDamage("fireAspect", 1).addEnchantmentForRanged("flame", 1);
			Obsidian                .addEnchantmentForDamage("fireAspect", 1).addEnchantmentForRanged("flame", 1);
			STONES.Gneiss           .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			NetherBrick             .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			PO4                     .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			Phosphorite             .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			Phosphorus              .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			PhosphorusBlue          .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			PhosphorusRed           .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			PhosphorusWhite         .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			ObsidianSteel           .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			Ignatius                .addEnchantmentForDamage("fireAspect", 2).addEnchantmentForRanged("flame", 2);
			Sunstone                .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Prometheum              .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Octine                  .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Kreknorite              .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Firestone               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Fireleaf                .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			FierySteel              .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			MeteoflameSteel         .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			MeteoflameBlackSteel    .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			MeteoflameBlueSteel     .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			MeteoflameRedSteel      .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			FlamascusSteel          .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Pyrotheum               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Blaze                   .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			InfusedFire             .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Vulcanite               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Amordrine               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			MuspelheimPower         .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			RedMatter               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Netherite               .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			NetherizedDiamond       .addEnchantmentForDamage("fireAspect", 3).addEnchantmentForRanged("flame", 3).addEnchantmentForTools("fireAspect", 3);
			Infinity                .addEnchantmentForDamage("fireAspect",10).addEnchantmentForRanged("flame",10).addEnchantmentForTools("fireAspect",10);
		}
		private static void init18() { // upstream MT.java:None-None
			Skyroot                 .addEnchantmentForDamage("smite", 2);
			Hepatizon               .addEnchantmentForDamage("smite", 2);
			BlackBronze             .addEnchantmentForDamage("smite", 2);
			RedSteel                .addEnchantmentForDamage("smite", 3);
			MeteoricRedSteel        .addEnchantmentForDamage("smite", 3);
			MeteoflameRedSteel      .addEnchantmentForDamage("smite", 3);
			Au                      .addEnchantmentForDamage("smite", 3);
			TitaniumGold            .addEnchantmentForDamage("smite", 3);
			Electrum                .addEnchantmentForDamage("smite", 3);
			GildedIron              .addEnchantmentForDamage("smite", 3);
			STONES.Holystone        .addEnchantmentForDamage("smite", 3);
			RoseGold                .addEnchantmentForDamage("smite", 4);
			EnergeticAlloy          .addEnchantmentForDamage("smite", 4);
			SpectreIron             .addEnchantmentForDamage("smite", 5);
			VibrantAlloy            .addEnchantmentForDamage("smite", 5);
			AmberGolden             .addEnchantmentForDamage("smite", 5);
			Zanite                  .addEnchantmentForDamage("smite", 5);
			Mauftrium               .addEnchantmentForDamage("smite", 5);
			Carmot                  .addEnchantmentForDamage("smite", 5);
			Pt                      .addEnchantmentForDamage("smite", 5);
			Mithril                 .addEnchantmentForDamage("smite", 5);
			InfusedVis              .addEnchantmentForDamage("smite", 5);
			Infinity                .addEnchantmentForDamage("smite",10);
			Pb                      .addEnchantmentForDamage("baneOfArthropods", 2);
			Ni                      .addEnchantmentForDamage("baneOfArthropods", 2);
			Constantan              .addEnchantmentForDamage("baneOfArthropods", 2);
			Nichrome                .addEnchantmentForDamage("baneOfArthropods", 2);
			Invar                   .addEnchantmentForDamage("baneOfArthropods", 3);
			Sb                      .addEnchantmentForDamage("baneOfArthropods", 3);
			Aredrite                .addEnchantmentForDamage("baneOfArthropods", 3);
			BatteryAlloy            .addEnchantmentForDamage("baneOfArthropods", 4);
			Bi                      .addEnchantmentForDamage("baneOfArthropods", 4);
			Orichalcum              .addEnchantmentForDamage("baneOfArthropods", 4);
			BismuthBronze           .addEnchantmentForDamage("baneOfArthropods", 4);
			InfusedEarth            .addEnchantmentForDamage("baneOfArthropods", 5);
			Celenegil               .addEnchantmentForDamage("baneOfArthropods", 5);
			Infinity                .addEnchantmentForDamage("baneOfArthropods",10);
			Fe                      .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			IronMagnetic            .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			IronWood                .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
		}
		private static void init19() { // upstream MT.java:None-None
			Ice                     .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			Glass                   .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			Bronze                  .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			ArsenicCopper           .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			ArsenicBronze           .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			GildedIron              .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			PulsatingIron           .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			ConductiveIron          .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			RedstoneAlloy           .addEnchantmentForDamage("sharpness", 1).addEnchantmentForRanged("power", 1);
			ElectricalSteel         .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			Brass                   .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			CobaltBrass             .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			HSLA                    .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			Steel                   .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			SteelMagnetic           .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			SteelGalvanized         .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			Syrmorite               .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			WroughtIron             .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			PigIron                 .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			Meteorite               .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			FierySteel              .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			FrozenIron              .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			MeteoricIron            .addEnchantmentForDamage("sharpness", 2).addEnchantmentForRanged("power", 2);
			MeteoricSteel           .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoflameSteel         .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			Steeleaf                .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			Fireleaf                .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			VanadiumSteel           .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			StainlessSteel          .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			Knightmetal             .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			DeepIron                .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			ShadowIron              .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			BlackSteel              .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoricBlackSteel      .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoflameBlackSteel    .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			RedSteel                .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoricRedSteel        .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoflameRedSteel      .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			BlueSteel               .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			MeteoricBlueSteel       .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
		}
		private static void init20() { // upstream MT.java:None-None
			MeteoflameBlueSteel     .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			Ti                      .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			TitaniumGold            .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			TungstenAlloy           .addEnchantmentForDamage("sharpness", 3).addEnchantmentForRanged("power", 3);
			TungstenSteel           .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			NetherizedDiamond       .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			HSSG                    .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			HSSE                    .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			HSSS                    .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			ShadowSteel             .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			Zanite                  .addEnchantmentForDamage("sharpness", 4).addEnchantmentForRanged("power", 4);
			DamascusSteel           .addEnchantmentForDamage("sharpness", 5).addEnchantmentForRanged("power", 5);
			FlamascusSteel          .addEnchantmentForDamage("sharpness", 5).addEnchantmentForRanged("power", 5);
			Elvorium                .addEnchantmentForDamage("sharpness", 5).addEnchantmentForRanged("power", 5);
			InfusedEntropy          .addEnchantmentForDamage("sharpness", 5).addEnchantmentForRanged("power", 5);
			Ke                      .addEnchantmentForDamage("sharpness", 6).addEnchantmentForRanged("power", 6);
			Ad                      .addEnchantmentForDamage("sharpness", 7).addEnchantmentForRanged("power", 7);
			Trinitanium             .addEnchantmentForDamage("sharpness", 7).addEnchantmentForRanged("power", 7);
			Trinaquadalloy          .addEnchantmentForDamage("sharpness", 8).addEnchantmentForRanged("power", 8);
			Infinity                .addEnchantmentForDamage("sharpness",10).addEnchantmentForRanged("power",10);
			Oureclase               .addEnchantmentForArmors("respiration", 3);
			InfusedAir              .addEnchantmentForArmors("respiration", 3);
			Infinity                .addEnchantmentForArmors("respiration",10);
			Atl                     .addEnchantmentForArmors("featherFalling", 4);
			InfusedFire             .addEnchantmentForArmors("featherFalling", 4);
			Infinity                .addEnchantmentForArmors("featherFalling",10);
			Steeleaf                .addEnchantmentForArmors("protection", 2);
			Fireleaf                .addEnchantmentForArmors("protection", 2);
			Knightmetal             .addEnchantmentForArmors("protection", 1);
			Celenegil               .addEnchantmentForArmors("protection", 4);
			InfusedEarth            .addEnchantmentForArmors("protection", 4);
			InfusedVis              .addEnchantmentForArmors("protection", 4);
			InfusedBalance          .addEnchantmentForArmors("protection", 4);
			Tartarite               .addEnchantmentForArmors("protection", 5);
			Adamantine              .addEnchantmentForArmors("protection", 5);
			DarkMatter              .addEnchantmentForArmors("protection", 5);
			VibraniumSilver         .addEnchantmentForArmors("protection", 6);
			VibraniumSteel          .addEnchantmentForArmors("protection", 8);
			Vibramantium            .addEnchantmentForArmors("protection",10);
			Vb                      .addEnchantmentForArmors("protection",10);
		}
		private static void init21() { // upstream MT.java:None-None
			Ad                      .addEnchantmentForArmors("protection",10);
			Infinity                .addEnchantmentForArmors("protection",10);
			Aredrite                .addEnchantmentForArmors("fireProtection", 3);
			Efrine                  .addEnchantmentForArmors("fireProtection", 3);
			Netherite               .addEnchantmentForArmors("fireProtection", 3);
			NetherizedDiamond       .addEnchantmentForArmors("fireProtection", 5);
			Vyroxeres               .addEnchantmentForArmors("thorns", 3);
			InfusedEntropy          .addEnchantmentForArmors("thorns", 3);
			Infinity                .addEnchantmentForArmors("thorns",10);
			InfusedWater            .addEnchantmentForArmors("aquaAffinity", 1);
			IronWood                .addEnchantmentForArmors("aquaAffinity", 1);
			Ceruclase               .addEnchantmentForArmors("aquaAffinity", 1);
			Inolashite              .addEnchantmentForArmors("aquaAffinity", 1);
			Infinity                .addEnchantmentForArmors("aquaAffinity",10);
			InfusedOrder            .addEnchantmentForArmors("projectileProtection", 4);
			Infinity                .addEnchantmentForArmors("projectileProtection",10);
			Obsidian                .addEnchantmentForArmors("blastProtection", 3);
			InfusedDull             .addEnchantmentForArmors("blastProtection", 4);
			Amordrine               .addEnchantmentForArmors("blastProtection", 5);
			RedMatter               .addEnchantmentForArmors("blastProtection", 5);
			Infinity                .addEnchantmentForArmors("blastProtection",10);
			NaNO3.addSourceOf(Niter, KNO3, K);
			KNO3.addSourceOf(Niter, NaNO3, Na);
			Niter.addSourceOf(KNO3, NaNO3, K, Na);
			ores(OREMATS.Pitchblende             , Pb                        , Ra                        , RareEarth             , Th                    );
			ores(OREMATS.Uraninite               , Pb                        , Ra                        , RareEarth             , Th                    );
			ores(Yellorite                       , Pb                        , Ra                        , RareEarth             , Th                    );
			ores(Th                              , Pb                        , Ra                        , RareEarth             );
			ores(Cyanite                         , Pb                        , Ra                        , RareEarth             );
			ores(U_238                           , Pb                        , Ra                        , RareEarth             );
			ores(Yellorium                       , Pb                        , Ra                        , RareEarth             );
			ores(Pu                              , Pb                        , Ra                        , RareEarth             );
			ores(Blutonium                       , Pb                        , Ra                        , RareEarth             );
			ores(Am                              , Pb                        , Ra                        , RareEarth             );
			ores(Ludicrite                       , Pb                        , Ra                        , RareEarth             );
			for (OreDictMaterial tMat : ANY.CaF2.mToThis) ores(tMat, OREMATS.Huebnerite, Y, Ce, Fe2O3, Na, Ba);
			ores(CaF2                            , FluoriteGreen             , FluoriteOrange            );
			ores(FluoriteRed                     , FluoritePink              , FluoriteMagenta           );
			ores(FluoritePink                    , FluoriteWhite             , FluoriteRed               );
			ores(FluoriteBlue                    , FluoriteMagenta           , FluoriteBlack             );
		}
		private static void init22() { // upstream MT.java:None-None
			ores(FluoriteGreen                   , FluoriteYellow            , FluoriteBlue              );
			ores(FluoriteBlack                   , FluoriteWhite             , CaF2                      );
			ores(FluoriteWhite                   , FluoriteBlack             , CaF2                      );
			ores(FluoriteYellow                  , FluoriteGreen             , FluoriteOrange            );
			ores(FluoriteOrange                  , FluoriteYellow            , FluoriteRed               );
			ores(FluoriteMagenta                 , FluoritePink              , FluoriteBlue              );
			ores(S                               , Pyrite                    , OREMATS.Sphalerite        , OREMATS.Cinnabar      , OREMATS.Chalcopyrite  , OREMATS.Arsenopyrite  , OREMATS.Galena        , OREMATS.Stibnite      , Gypsum);
			ores(Se                              , Pyrite                    , OREMATS.Galena            , OREMATS.Sphalerite    , In                    , Ga                    , Cd                    );
			ores(OREMATS.Chalcopyrite            , Pyrite                    , OREMATS.Cobaltite         , Cd                    , Au                    , OREMATS.Sperrylite    , OREMATS.Stannite      , In                    );
			ores(OREMATS.Sperrylite              , Sb                        , Cu                        , Fe2O3                 , Rh                    , OREMATS.Cooperite     );
			ores(OREMATS.Pentlandite             , Fe2O3                     , S                         , OREMATS.Cobaltite     , OREMATS.Sperrylite    , Gypsum                );
			ores(OREMATS.Sphalerite              , Cd                        , Ga                        , Zn                    , OREMATS.Kesterite     , Se                    , In                    );
			ores(OREMATS.Tetrahedrite            , Cu                        , Sb                        , Zn                    , OREMATS.Kesterite     , As                    );
			ores(Pyrite                          , S                         , Phosphorus                , Fe2O3                 , OREMATS.Stannite      , Se                    );
			ores(Sn                              , OREMATS.Molybdenite       , OREMATS.Wolframite        , FluoriteBlack         , OREMATS.Arsenopyrite  , OREMATS.Stannite      , OREMATS.Sperrylite    , OREMATS.Huebnerite    , Apatite);
			ores(OREMATS.Cassiterite             , OREMATS.Molybdenite       , OREMATS.Wolframite        , FluoriteWhite         , OREMATS.Arsenopyrite  , OREMATS.Stannite      , OREMATS.Sperrylite    , OREMATS.Huebnerite    , Apatite);
			ores(Sb                              , Zn                        , OREMATS.Realgar           , OREMATS.Cinnabar      , OREMATS.Galena        , OREMATS.Arsenopyrite  , Pyrite                , OREMATS.Barite        , CaCO3);
			ores(OREMATS.Stibnite                , Sb                        , OREMATS.Realgar           , OREMATS.Cinnabar      , OREMATS.Galena        , OREMATS.Arsenopyrite  , Pyrite                , OREMATS.Barite        , CaCO3);
			ores(OREMATS.Bauxite                 , Kaolinite                 , OREMATS.Ilmenite          , Fe2O3                 , Al2O3                 , AlO3H3                );
			ores(AlO3H3                          , OREMATS.Bauxite           , OREMATS.Ilmenite          , Fe2O3                 , Al2O3                 );
			ores(OREMATS.Ilmenite                , TiO2                      , Fe2O3                     , MgCO3                 , MnO2                  );
			ores(TiO2                            , Fe2O3                     , Zircon                    );
			ores(Fe2O3                           , OREMATS.Ilmenite          , OREMATS.GraniticMineralSand, MnO2                 , ClayRed               );
			ores(OREMATS.Galena                  , OREMATS.Sphalerite        , Ag                        , Pb                    , Se                    , FluoriteRed           , CaCO3);
			ores(OREMATS.Arsenopyrite            , Au                        , OREMATS.Realgar           , FluoriteOrange        , OREMATS.Cassiterite   , OREMATS.Huebnerite    );
			ores(OREMATS.Cobaltite               , Co                        , OREMATS.Realgar           , FluoriteOrange        , OREMATS.Pentlandite   , OREMATS.YellowLimonite);
			ores(Co_60                           , OREMATS.Cobaltite         , OREMATS.Realgar           , FluoriteOrange        , OREMATS.Pentlandite   , OREMATS.YellowLimonite);
			ores(Co                              , OREMATS.Cobaltite         , OREMATS.Realgar           , FluoriteOrange        , OREMATS.Pentlandite   , OREMATS.YellowLimonite);
			ores(OREMATS.Realgar                 , OREMATS.Cobaltite         , OREMATS.Arsenopyrite      );
			ores(Cu                              , OREMATS.Cobaltite         , Au                        , Ni                    , OREMATS.Malachite     , As                    );
			ores(Ni                              , OREMATS.Cobaltite         , OREMATS.Cooperite         , Fe2O3                 , OREMATS.Pentlandite   );
			ores(OREMATS.Stannite                , Ge                        , Pyrite                    , OREMATS.Kesterite     );
			ores(OREMATS.Kesterite               , Ge                        , Pyrite                    , OREMATS.Stannite      );
			ores(OREMATS.Glauconite              , Na                        , Al2O3                     , Fe2O3                 );
			ores(OREMATS.Diatomite               , OREMATS.Mica              , Opal                      , Biotite               , OREMATS.Perlite       , Sapphire);
			ores(OREMATS.Mica                    , OREMATS.Vermiculite       , Asbestos                  , Biotite               , OREMATS.Perlite       );
			ores(OREMATS.Vermiculite             , OREMATS.Mica              , Asbestos                  , Biotite               , OREMATS.Diatomite     );
			ores(Biotite                         , OREMATS.Mica              , OREMATS.Vermiculite       , Asbestos              , OREMATS.Perlite       );
			ores(Asbestos                        , OREMATS.Mica              , Biotite                   , Talc                  , Jade                  );
			ores(Jade                            , OREMATS.Mica              , Biotite                   , Talc                  , Asbestos              );
		}
		private static void init23() { // upstream MT.java:None-None
			ores(Gypsum                          , OREMATS.Trona             , OREMATS.Mirabilite        , Asbestos              , Talc                  , S);
			ores(OREMATS.Mirabilite              , OREMATS.Trona             , Gypsum                    );
			ores(OREMATS.Trona                   , OREMATS.Mirabilite        , Gypsum                    , NaHCO3                );
			ores(NaHCO3                          , OREMATS.Mirabilite        , Gypsum                    , OREMATS.Trona         );
			ores(Lapis                           , Lazurite                  , Sodalite                  , Pyrite                );
			ores(OREMATS.Cooperite               , Pd                        , Ni                        , Ir                    );
			ores(OREMATS.Cinnabar                , Redstone                  , S                         , Glowstone             , Se                    );
			ores(OREMATS.Chromite                , MnO2                      , Fe2O3                     , MgCO3                 , OREMATS.Bromargyrite  );
			ores(OREMATS.Bromargyrite            , MnO2                      , Ag                        , OREMATS.Chromite      , OREMATS.Smithsonite   );
			ores(Mn                              , MnO2                      , Fe2O3                     , OREMATS.Chromite      );
			ores(MnO2                            , OREMATS.Bromargyrite      , Fe2O3                     , OREMATS.Chromite      );
			ores(OREMATS.Columbite               , OREMATS.Tantalite         , OREMATS.Coltan            , MnO2                  , OREMATS.Ilmenite      );
			ores(OREMATS.Tantalite               , OREMATS.Columbite         , OREMATS.Coltan            , MnO2                  , OREMATS.Ilmenite      );
			ores(OREMATS.Coltan                  , OREMATS.Columbite         , OREMATS.Tantalite         , MnO2                  , OREMATS.Ilmenite      );
			ores(Apatite                         , Phosphorite               , PhosphorusBlue            , FluoriteBlue          , PO4                   );
			ores(Phosphorus                      , Phosphorite               , Apatite                   , FluoriteYellow        , PO4                   , PhosphorusRed         , PhosphorusWhite       );
			ores(PhosphorusBlue                  , Phosphorite               , Apatite                   , FluoriteBlue          , PO4                   , PhosphorusRed         , PhosphorusWhite       );
			ores(PhosphorusRed                   , Phosphorite               , Apatite                   , FluoriteRed           , PO4                   , PhosphorusBlue        , PhosphorusWhite       );
			ores(PhosphorusWhite                 , Phosphorite               , Apatite                   , FluoriteWhite         , PO4                   , PhosphorusRed         , PhosphorusBlue        );
			ores(Phosphorite                     , Phosphorus                , Apatite                   , FluoriteYellow        , PO4                   , PhosphorusRed         , PhosphorusWhite       );
			ores(P                               , Phosphorus                , Apatite                   , FluoriteYellow        , PO4                   , PhosphorusRed         , PhosphorusWhite       );
			ores(PO4                             , Phosphorus                , Apatite                   , FluoriteYellow        , Phosphorite           , PhosphorusRed         , PhosphorusWhite       );
			ores(OREMATS.Zeolite                 , OREMATS.Pollucite         , NaCl                      );
			ores(OREMATS.Pollucite               , OREMATS.Zeolite           , Cs                        , Rb                    );
			ores(Diamond                         , Graphite                  , DiamondPink               ).addSourceOf(Diamond, Graphite);
			ores(DiamondBlue                     , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(DiamondGreen                    , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(DiamondPurple                   , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(DiamondRed                      , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(DiamondYellow                   , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(DiamondPink                     , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(ManaDiamond                     , Graphite                  , Diamond                   ).addSourceOf(Diamond, Graphite);
			ores(ElvenDragonstone                , Graphite                  );
			ores(Be                              , Al2O3                     , Emerald                   , Aquamarine            , Morganite             , Goshenite             , Bixbite               , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Emerald                         , Al2O3                     , Be                        , Aquamarine            , Morganite             , Goshenite             , Bixbite               , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Aquamarine                      , Al2O3                     , Be                        , Emerald               , Morganite             , Goshenite             , Bixbite               , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Morganite                       , Al2O3                     , Be                        , Emerald               , Aquamarine            , Goshenite             , Bixbite               , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Goshenite                       , Al2O3                     , Be                        , Emerald               , Aquamarine            , Morganite             , Bixbite               , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Bixbite                         , Al2O3                     , Be                        , Emerald               , Aquamarine            , Morganite             , Goshenite             , Heliodor              , Maxixe    ).addSourceOf(Emerald, Be, Al);
			ores(Heliodor                        , Al2O3                     , Be                        , Emerald               , Aquamarine            , Morganite             , Goshenite             , Bixbite               , Maxixe    ).addSourceOf(Emerald, Be, Al);
		}
		private static void init24() { // upstream MT.java:None-None
			ores(Maxixe                          , Al2O3                     , Be                        , Emerald               , Aquamarine            , Morganite             , Goshenite             , Bixbite               , Heliodor  ).addSourceOf(Emerald, Be, Al);
			ores(Sapphire                        , Al2O3                     , Ruby                      , GreenSapphire         , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(Ruby                            , Al2O3                     , OrangeSapphire            , GreenSapphire         , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(GreenSapphire                   , Al2O3                     , Ruby                      , YellowSapphire        , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(BlueSapphire                    , Al2O3                     , Ruby                      , GreenSapphire         , PurpleSapphire        ).addSourceOf(Sapphire, Al);
			ores(YellowSapphire                  , Al2O3                     , Ruby                      , GreenSapphire         , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(OrangeSapphire                  , Al2O3                     , Ruby                      , GreenSapphire         , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(PurpleSapphire                  , Al2O3                     , Ruby                      , GreenSapphire         , BlueSapphire          ).addSourceOf(Sapphire, Al);
			ores(Almandine                       , Grossular                 , Pyrope                    , Spessartine           , Andradite             , Uvarovite             );
			ores(Grossular                       , Almandine                 , Pyrope                    , Spessartine           , Andradite             , Uvarovite             );
			ores(Pyrope                          , Almandine                 , Grossular                 , Spessartine           , Andradite             , Uvarovite             );
			ores(Spessartine                     , Almandine                 , Grossular                 , Pyrope                , Andradite             , Uvarovite             );
			ores(Andradite                       , Almandine                 , Grossular                 , Pyrope                , Spessartine           , Uvarovite             );
			ores(Uvarovite                       , Almandine                 , Grossular                 , Pyrope                , Spessartine           , Andradite             );
			ores(Jasper                          , JasperOcean               , JasperRainforest          , JasperBlue            , JasperGreen           , JasperYellow          );
			ores(JasperOcean                     , Jasper                    , JasperRainforest          , JasperBlue            , JasperGreen           , JasperYellow          );
			ores(JasperRainforest                , Jasper                    , JasperOcean               , JasperBlue            , JasperGreen           , JasperYellow          );
			ores(JasperBlue                      , Jasper                    , JasperOcean               , JasperRainforest      , JasperGreen           , JasperYellow          );
			ores(JasperGreen                     , Jasper                    , JasperOcean               , JasperRainforest      , JasperBlue            , JasperYellow          );
			ores(JasperYellow                    , Jasper                    , JasperOcean               , JasperRainforest      , JasperBlue            , JasperGreen           );
			ores(TigerEyeYellow                  , Fe2O3                     , TigerEyeGreen             , TigerEyeRed           , TigerEyeBlue          , TigerEyeBlack         , TigerIron             );
			ores(TigerEyeGreen                   , Fe2O3                     , TigerEyeYellow            , TigerEyeRed           , TigerEyeBlue          , TigerEyeBlack         , TigerIron             );
			ores(TigerEyeRed                     , Fe2O3                     , TigerEyeYellow            , TigerEyeGreen         , TigerEyeBlue          , TigerEyeBlack         , TigerIron             );
			ores(TigerEyeBlue                    , Fe2O3                     , TigerEyeYellow            , TigerEyeGreen         , TigerEyeRed           , TigerEyeBlack         , TigerIron             );
			ores(TigerEyeBlack                   , Fe2O3                     , TigerEyeYellow            , TigerEyeGreen         , TigerEyeRed           , TigerEyeBlue          , TigerIron             );
			ores(TigerIron                       , Fe2O3                     , TigerEyeYellow            , TigerEyeGreen         , TigerEyeRed           , TigerEyeBlue          , TigerEyeBlack         );
			ores(AventurineGreen                 , AventurineBrown           , AventurineYellow          , AventurineBlack       , AventurineBlue        , AventurineRed         );
			ores(AventurineBrown                 , AventurineGreen           , AventurineYellow          , AventurineBlack       , AventurineBlue        , AventurineRed         );
			ores(AventurineYellow                , AventurineGreen           , AventurineBrown           , AventurineBlack       , AventurineBlue        , AventurineRed         );
			ores(AventurineBlack                 , AventurineGreen           , AventurineBrown           , AventurineYellow      , AventurineBlue        , AventurineRed         );
			ores(AventurineBlue                  , AventurineGreen           , AventurineBrown           , AventurineYellow      , AventurineBlack       , AventurineRed         );
			ores(AventurineRed                   , AventurineGreen           , AventurineBrown           , AventurineYellow      , AventurineBlack       , AventurineBlue        );
			ores(Spinel                          , Al2O3                     , BalasRuby                 );
			ores(BalasRuby                       , OREMATS.Chromite          , Spinel                    );
			ores(HexoriumRed                     , HexoriumWhite             , HexoriumBlack             );
			ores(HexoriumGreen                   , HexoriumWhite             , HexoriumBlack             );
			ores(HexoriumBlue                    , HexoriumWhite             , HexoriumBlack             );
			ores(HexoriumBlack                   , HexoriumRed               , HexoriumGreen             , HexoriumBlue          );
			ores(HexoriumWhite                   , HexoriumRed               , HexoriumGreen             , HexoriumBlue          );
			ores(Clay                            , Kaolinite                 , Palygorskite              );
		}
		private static void init25() { // upstream MT.java:None-None
			ores(ClayBrown                       , Clay                      , ClayRed                   );
			ores(ClayRed                         , Bentonite                 , ClayBrown                 );
			ores(Bentonite                       , ClayRed                   , Palygorskite              );
			ores(Palygorskite                    , Kaolinite                 , Bentonite                 );
			ores(Kaolinite                       , ClayBrown                 , Clay                      );
			ores(OREMATS.Barite                  , CertusQuartz              , STONES.Quartzite          );
			ores(OREMATS.QuartzSand              , CertusQuartz              , STONES.Quartzite          , OREMATS.Barite        );
			ores(OREMATS.Wollastonite            , Fe2O3                     , MgCO3                     , MnO2                  );
			ores(Redstone                        , OREMATS.Cinnabar          , RareEarth                 , Glowstone             );
			ores(Nikolite                        , Cu                        , RareEarth                 , Azurite               , As);
			ores(Re                              , OREMATS.Chalcopyrite      , OREMATS.Molybdenite       );
			ores(Os                              , Ir                        , Pt                        , Ru                    );
			ores(Ir                              , Pt                        , Os                        , Rh                    );
			ores(Pt                              , Ni                        , Ir                        , Pd                    );
			ores(MeteoricIron                    , Ni                        , Ir                        , Pt                    );
			ores(Au                              , Cu                        , Ni                        , OREMATS.Cinnabar      );
			ores(Ag                              , Pb                        , S                         , OREMATS.Bromargyrite  );
			ores(Nd                              , Monazite                  , RareEarth                 );
			ores(OREMATS.Bastnasite              , Monazite                  , RareEarth                 , Nd                    );
			ores(Monazite                        , Th                        , Nd                        , RareEarth             );
			ores(Forcicium                       , Th                        , Nd                        , RareEarth             );
			ores(Forcillium                      , Th                        , Nd                        , RareEarth             );
			ores(Ge                              , Fe2O3                     , Sn                        , OREMATS.Chromite      );
			ores(Cd                              , OREMATS.Chalcopyrite      , OREMATS.Sphalerite        , Se                    );
			ores(OREMATS.Powellite               , OREMATS.Molybdenite       , OREMATS.Scheelite         );
			ores(OREMATS.Wulfenite               , OREMATS.Powellite         , OREMATS.Scheelite         , OREMATS.Molybdenite   , OREMATS.Galena        );
			ores(Mo                              , OREMATS.Powellite         , OREMATS.Scheelite         , Re                    , OREMATS.Wulfenite     , Os);
			ores(OREMATS.Molybdenite             , OREMATS.Powellite         , OREMATS.Scheelite         , Re                    , OREMATS.Wulfenite     , Os);
			ores(OREMATS.Malachite               , Cu                        , OREMATS.BrownLimonite     , CaCO3                 , Azurite               , As);
			ores(OREMATS.BrownLimonite           , OREMATS.Malachite         , OREMATS.YellowLimonite    );
			ores(OREMATS.YellowLimonite          , Ni                        , OREMATS.BrownLimonite     , OREMATS.Cobaltite     );
			ores(OREMATS.Garnierite              , Ni                        , OREMATS.Sperrylite        );
			ores(OREMATS.Tungstate               , MnO2                      , Ag                        , LiCl                  );
			ores(OREMATS.Scheelite               , MnO2                      , OREMATS.Molybdenite       , CaCO3                 );
			ores(OREMATS.Huebnerite              , OREMATS.Wolframite        , OREMATS.Molybdenite       , FluoriteGreen         , OREMATS.Arsenopyrite  , OREMATS.Cassiterite   , Topaz                 );
			ores(OREMATS.Wolframite              , OREMATS.Tungstate         , Fe2O3                     , OREMATS.Stannite      , MgCO3);
			ores(OREMATS.Ferberite               , OREMATS.Tungstate         , Fe2O3                     );
			ores(OREMATS.Russellite              , OREMATS.Tungstate         , Bi                        );
			ores(OREMATS.Stolzite                , OREMATS.Tungstate         , Pb                        );
			ores(OREMATS.Pinalite                , OREMATS.Tungstate         , Pb                        );
		}
		private static void init26() { // upstream MT.java:None-None
			ores(NaCl                            , KCl                       , KIO3                      , OREMATS.Borax         );
			ores(KCl                             , KIO3                      , NaCl                      );
			ores(KIO3                            , NaCl                      , KCl                       );
			ores(Endstone                        , He_3                      , Be                        );
			ores(Endium                          , OREMATS.Wolframite        , OREMATS.Sperrylite        , OREMATS.Coltan        , Ke);
			ores(Glowstone                       , Redstone                  , Au                        , Gloomstone            , FluoriteYellow        );
			ores(GlowstoneCeres                  , Redstone                  , Au                        , Glowstone             );
			ores(GlowstoneIo                     , Redstone                  , Au                        , Glowstone             );
			ores(GlowstoneEnceladus              , Redstone                  , Au                        , Glowstone             );
			ores(GlowstoneProteus                , Redstone                  , Au                        , Glowstone             );
			ores(GlowstonePluto                  , Redstone                  , Au                        , Glowstone             );
			ores(Gloomstone                      , Redstone                  , Au                        , Glowstone             , FluoriteBlue          );
			ores(Efrine                          , SoulSand                  , Be                        , OREMATS.Pentlandite   , Zircon                , FluoriteGreen);
			ores(AncientDebris                   , SoulSand                  , Efrine                    , OREMATS.Huebnerite    , Firestone             );
			ores(Firestone                       , NetherQuartz              , VoidQuartz                , PhosphorusRed         , FluoriteRed           );
			ores(SoulSand                        , Coal                      , NetherQuartz              , Niter                 , Gloomstone            );
			ores(NetherQuartz                    , OREMATS.Barite            , Efrine                    , VoidQuartz            , FluoriteWhite         );
			ores(VoidQuartz                      , OREMATS.Barite            , Efrine                    , NetherQuartz          , FluoriteMagenta       );
			ores(STONES.Quartzite                , CertusQuartz              , OREMATS.Barite            , Fe2O3                 );
			ores(MilkyQuartz                     , CertusQuartz              , OREMATS.Barite            );
			ores(CertusQuartz                    , MilkyQuartz               , OREMATS.Barite            );
			ores(ChargedCertusQuartz             , MilkyQuartz               , OREMATS.Barite            );
			ores(BlackQuartz                     , MilkyQuartz               , OREMATS.Barite            );
			ores(Syrmorite                       , OREMATS.Stannite          , OREMATS.Tetrahedrite      , Be                    );
			ores(Octine                          , OREMATS.Pentlandite       , OREMATS.Huebnerite        , Zircon                );
			ores(Ga                              , Zn                        , Se                        );
			ores(Zn                              , Sn                        , Ga                        );
			ores(OREMATS.Lepidolite              , LiCl                      , Cs                        , Rb                    );
			ores(OREMATS.Spodumene               , Al2O3                     , LiCl                      );
			ores(OREMATS.Kyanite                 , STONES.Quartzite          , OREMATS.Lepidolite        , OREMATS.Spodumene     );
			ores(OREMATS.Alunite                 , STONES.Quartzite          );
			ores(OREMATS.Smithsonite             , Zn                        , OREMATS.Bromargyrite      );
			ores(Pb                              , Ag                        , S                         );
			ores(Electrum                        , Au                        , Ag                        );
			ores(Bronze                          , Cu                        , Sn                        , As                    );
			ores(Brass                           , Cu                        , Zn                        , As                    );
			ores(Coal                            , Lignite                   , S                         );
			ores(Lignite                         , Coal                      , S                         , Ge                    );
			ores(Al2O3                           , OREMATS.Bauxite           , Al2O3                     , AlO3H3                );
			ores(Bi                              , OREMATS.Russellite        , OREMATS.Galena            , OREMATS.Kesterite     );
		}
		private static void init27() { // upstream MT.java:None-None
			ores(Cr                              , OREMATS.Chromite          , Fe2O3                     , MgCO3                 );
			ores(OREMATS.Ferrovanadium           , OREMATS.Magnetite         , VanadiumPentoxide         );
			ores(OREMATS.Magnetite               , Fe2O3                     , Au                        , Stone);
			ores(OREMATS.GraniticMineralSand     , Fe2O3                     , Au                        , STONES.GraniteBlack);
			ores(OREMATS.BasalticMineralSand     , Fe2O3                     , Au                        , STONES.Basalt);
			ores(OREMATS.Celestine               , Sr                        , S                         );
			ores(Lazurite                        , Sodalite                  , Lapis                     );
			ores(Sodalite                        , Lazurite                  , Lapis                     );
			ores(Zr                              , TiO2                      , Hf                        );
			ores(Zircon                          , TiO2                      , Hf                        , OREMATS.Uraninite     );
			ores(Eudialyte                       , Zircon                    , RareEarth                 , Hf                    , Pb                    );
			ores(Azurite                         , Zircon                    , OREMATS.Malachite         , Hf                    );
			ores(Adamantine                      , OREMATS.GraniticMineralSand);
			ores(Peridot                         , Obsidian                  , MgCO3                     );
			ores(PigIron                         , Fe2O3                     );
			ores(DarkIron                        , Fe2O3                     );
			ores(Steel                           , Fe2O3                     );
			ores(MeteoricSteel                   , Fe2O3                     );
			ores(Graphite                        , C                         );
			ores(MgCO3                           , OREMATS.Cobaltite         , MnO2                      );
			ores(CaCO3                           , OREMATS.Malachite         );
			ores(OREMATS.Borax                   , B                         , NaCl                      );
			ores(Netherrack                      , S                         );
			ores(Flint                           , Obsidian                  );
			ores(NaNO3                           , KNO3                      , Niter                     );
			ores(KNO3                            , NaNO3                     , Niter                     );
			ores(Niter                           , KNO3                      , NaNO3                     );
			ores(Hf                              , Zircon                    );
			ores(Mg                              , Peridot                   , MgCO3                     );
			ores(Obsidian                        , Peridot                   , MgCO3                     );
			ores(OREMATS.Perlite                 , Peridot                   , MgCO3                     );
			ores(STONES.Redrock                  , ClayRed                   );
			ores(STONES.Limestone                , CaCO3                     );
			ores(STONES.Marble                   , CaCO3                     );
			ores(STONES.Eclogite                 , TiO2                      );
			ores(STONES.Limestone                , Phosphorite               );
			ores(STONES.Holystone                , Ambrosium                 );
			ores(Ambrosium                       , STONES.Holystone          );
			ores(Zanite                          , Opal                      , Ambrosium                 );
			ores(Tanzanite                       , Opal                      );
		}
		private static void init28() { // upstream MT.java:None-None
			ores(Opal                            , Tanzanite                 );
			ores(Topaz                           , BlueTopaz                 );
			ores(BlueTopaz                       , Topaz                     );
			ores(In                              , Se                        );
			ores(Li                              , LiCl                      );
			ores(LiCl                            , Li                        );
			ores(Nq_528                          , Nq                        , OREMATS.DuraniumHexafluoride);
			ores(Nq_522                          , Nq                        , OREMATS.TritaniumHexafluoride);
			ores(Nq                              , OREMATS.DuraniumHexachloride, OREMATS.TritaniumHexafluoride);
			ores(Ke                              , Sn                        , TiO2                        , Fe2O3, OREMATS.DuraniumHexaiodide);
			ores(Dn                              , OREMATS.TritaniumDioxide  , Ke                          );
			ores(DuraniumAlloy                   , OREMATS.TritaniumDioxide  , Ke                          );
			ores(OREMATS.DiduraniumTrioxide      , OREMATS.TritaniumDioxide  , Ke                          );
			ores(Tn                              , OREMATS.DiduraniumTrioxide, Ke                          );
			ores(TritaniumAlloy                  , OREMATS.DiduraniumTrioxide, Ke                          );
			ores(OREMATS.TritaniumDioxide        , OREMATS.DiduraniumTrioxide, Ke                          );
			ores(Dolamide                        , Dilithium                 , OREMATS.DiduraniumTrioxide  , OREMATS.DuraniumHexafluoride, OREMATS.DuraniumHexabromide);
			ores(Desh                            , Dolamide                  , OREMATS.DuraniumHexaiodide  , OREMATS.DuraniumHexachloride, OREMATS.DuraniumHexaastatide, OREMATS.Columbite, OREMATS.Cobaltite, Monazite, LiCl);
			ores(OREMATS.DuraniumHexafluoride    , OREMATS.DiduraniumTrioxide, FluoritePink                );
			ores(OREMATS.DuraniumHexachloride    , OREMATS.DiduraniumTrioxide, NaCl                        , KCl);
			ores(OREMATS.DuraniumHexabromide     , OREMATS.DiduraniumTrioxide, OREMATS.Bromargyrite        );
			ores(OREMATS.DuraniumHexaiodide      , OREMATS.DiduraniumTrioxide, KIO3                        );
			ores(OREMATS.DuraniumHexaastatide    , OREMATS.DiduraniumTrioxide, At                          );
			ores(OREMATS.TritaniumHexafluoride   , OREMATS.TritaniumDioxide  , FluoriteMagenta             );
			ores(OREMATS.TritaniumHexachloride   , OREMATS.TritaniumDioxide  , NaCl                        , KCl);
			ores(OREMATS.TritaniumHexabromide    , OREMATS.TritaniumDioxide  , OREMATS.Bromargyrite        );
			ores(OREMATS.TritaniumHexaiodide     , OREMATS.TritaniumDioxide  , KIO3                        );
			ores(OREMATS.TritaniumHexaastatide   , OREMATS.TritaniumDioxide  , At                          );
			ores(Ardite                          , OREMATS.Galena            , OREMATS.Stibnite            );
			ores(Aredrite                        , OREMATS.Galena            , OREMATS.Stibnite            );
			ores(Orichalcum                      , OREMATS.Tetrahedrite      , OREMATS.Kesterite           );
			ores(Alduorite                       , Cd                        );
			ores(Infuscolium                     , OREMATS.Malachite         );
			ores(Rubracium                       , OREMATS.Chromite          );
			ores(Meutoite                        , VanadiumPentoxide         );
			ores(Lemurite                        , MgCO3                     );
			ores(Ceruclase                       , OREMATS.Cobaltite         );
			ores(Atl                             , TiO2                      );
			ores(Oureclase                       , OREMATS.Pentlandite       );
			ores(Kalendrite                      , Os                        );
		}
		private static void init29() { // upstream MT.java:None-None
			ores(Carmot                          , Zn                        );
			ores(Sanguinite                      , Hg                        );
			ores(Vyroxeres                       , Ir                        );
			ores(Eximite                         , Pd                        );
			ores(Prometheum                      , OREMATS.Cobaltite         );
			ores(Ignatius                        , Se                        , In);
			ores(Vulcanite                       , OREMATS.Wolframite        );
			ores(DeepIron                        , Fe2O3                     );
			ores(ShadowIron                      , Fe2O3                     );
			ores(AstralSilver                    , Ag                        );
			ores(Midasium                        , Au                        );
			ores(Mithril                         , Pt                        );
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(Fe2O3                         , 5*U), stack(C                  , 1*U), stack(CaCO3, 1*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(OREMATS.Magnetite             ,14*U), stack(C                  , 3*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(OREMATS.BasalticMineralSand   ,14*U), stack(C                  , 3*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(OREMATS.GraniticMineralSand   ,14*U), stack(C                  , 3*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(OREMATS.Ferrovanadium         ,28*U), stack(C                  , 3*U)));
			Si                      .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(SiO2                          , 3*U), stack(C                  , 1*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(ShadowIron                    , 1*U), stack(Ignatius           , 1*U)));
			Fe                      .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(DeepIron                      , 1*U), stack(Prometheum         , 1*U)));
			BlackSteel              .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(DeepIron                      , 1*U), stack(Infuscolium        , 1*U)));
			Steel                   .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(WroughtIron                   , 1*U), stack(Air                , 1*U)));
			MeteoricSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(MeteoricIron                  , 1*U), stack(Air                , 1*U)));
			for (OreDictMaterial tMat : ANY.Glowstone.mToThis) if (tMat != Glowstone) { EnergeticAlloy          .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(InductiveAlloy                , 2*U), stack(tMat               , 1*U))); Lumium                  .addAlloyingRecipe(new OreDictConfigurationComponent( 4, stack(Sn                            , 3*U), stack(Ag                 , 1*U), stack(tMat, 4*U))); };
			ElectrumFlux            .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(InductiveAlloy                , 2*U), stack(Ag                 , 1*U), stack(Redstone, 3*U)));
			Ultimet                 .addAlloyingRecipe(new OreDictConfigurationComponent(36, stack(Co                            ,20*U), stack(Nichrome           , 5*U), stack(Cr, 7*U), stack(Mo, 4*U)));
			StainlessSteel          .addAlloyingRecipe(new OreDictConfigurationComponent(36, stack(WroughtIron                   ,24*U), stack(Nichrome           , 5*U), stack(Cr, 3*U), stack(Mn, 4*U)));
			TungstenSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(MeteoricSteel                 , 1*U), stack(W                  , 1*U)));
			TungstenSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(MeteoricSteel                 , 1*U), stack(TungstenSintered   , 1*U)));
			TungstenSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(Steel                         , 1*U), stack(TungstenSintered   , 1*U)));
			VanadiumSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(MeteoricSteel                 , 4*U), stack(V                  , 1*U)));
			ElectricalSteel         .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(MeteoricSteel                 , 1*U), stack(Si                 , 1*U)));
			ObsidianSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(MeteoricSteel                 , 1*U), stack(Lava               , 9*U)));
			ObsidianSteel           .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(Steel                         , 1*U), stack(Lava               , 9*U)));
			EndSteel                .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(ObsidianSteel                 , 1*U), stack(Endstone           , 1*U), stack(Lava, 9*U)));
			Alumite                 .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(Al                            , 5*U), stack(WroughtIron        , 2*U), stack(Lava,18*U)));
			Hepatizon               .addAlloyingRecipe(new OreDictConfigurationComponent(24, stack(Bronze                        , 8*U), stack(Sn                 , 1*U), stack(RoseGold,15*U)));
			RedAlloy                .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(Mingrade                      , 2*U), stack(Redstone           , 3*U)));
			RedAlloy                .addAlloyingRecipe(new OreDictConfigurationComponent( 1, stack(AnnealedCopper                , 1*U), stack(Redstone           , 4*U)));
			RoseGold                .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(AnnealedCopper                , 1*U), stack(Au                 , 4*U)));
		}
		private static void init30() { // upstream MT.java:None-None
			SterlingSilver          .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(AnnealedCopper                , 1*U), stack(Ag                 , 4*U)));
			AluminiumBrass          .addAlloyingRecipe(new OreDictConfigurationComponent( 4, stack(AnnealedCopper                , 1*U), stack(Al                 , 3*U)));
			Brass                   .addAlloyingRecipe(new OreDictConfigurationComponent( 4, stack(AnnealedCopper                , 3*U), stack(Zn                 , 1*U)));
			Bronze                  .addAlloyingRecipe(new OreDictConfigurationComponent( 4, stack(AnnealedCopper                , 3*U), stack(Sn                 , 1*U)));
			ArsenicCopper           .addAlloyingRecipe(new OreDictConfigurationComponent( 4, stack(AnnealedCopper                , 3*U), stack(As                 , 1*U)));
			ArsenicBronze           .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(ArsenicCopper                 , 4*U), stack(Sn                 , 1*U)));
			BlackBronze             .addAlloyingRecipe(new OreDictConfigurationComponent( 5, stack(AnnealedCopper                , 3*U), stack(Electrum           , 2*U)));
			BlackBronze             .addAlloyingRecipe(new OreDictConfigurationComponent(20, stack(Cu                            ,11*U), stack(RoseGold           , 5*U), stack(Ag, 4*U)));
			BlackBronze             .addAlloyingRecipe(new OreDictConfigurationComponent(20, stack(AnnealedCopper                ,11*U), stack(RoseGold           , 5*U), stack(Ag, 4*U)));
			BlackBronze             .addAlloyingRecipe(new OreDictConfigurationComponent(20, stack(Cu                            ,11*U), stack(SterlingSilver     , 5*U), stack(Au, 4*U)));
			BlackBronze             .addAlloyingRecipe(new OreDictConfigurationComponent(20, stack(AnnealedCopper                ,11*U), stack(SterlingSilver     , 5*U), stack(Au, 4*U)));
			Signalum                .addAlloyingRecipe(new OreDictConfigurationComponent( 8, stack(AnnealedCopper                , 1*U), stack(Ag                 , 2*U), stack(RedAlloy, 5*U)));
			Signalum                .addAlloyingRecipe(new OreDictConfigurationComponent(16, stack(Cu                            , 1*U), stack(SterlingSilver     , 5*U), stack(RedAlloy,10*U)));
			Signalum                .addAlloyingRecipe(new OreDictConfigurationComponent(16, stack(AnnealedCopper                , 1*U), stack(SterlingSilver     , 5*U), stack(RedAlloy,10*U)));
			Constantan              .addAlloyingRecipe(new OreDictConfigurationComponent( 2, stack(AnnealedCopper                , 1*U), stack(Ni                 , 1*U)));
			YttriumBariumCuprate    .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(AnnealedCopper                , 3*U), stack(Ba                 , 2*U), stack(Y, U)));
			YttriumBariumCuprate    .addAlloyingRecipe(new OreDictConfigurationComponent( 6, stack(Cu                            , 3*U), stack(Ba                 , 2*U), stack(Y, U)));
			Li2Fe2O4                .addAlloyingRecipe(new OreDictConfigurationComponent( 8, stack(Fe2O3                         , 5*U), stack(Li2O               , 3*U)));
			Aredrite.setGenerifying(Ardite);
			Orichalcum.setGenerifying(Brass);
			Oilshale.setGenerifying(MT.STONES.Shale);
			for (OreDictMaterial tMaterial : ANY.Ash.mToThis) tMaterial.setGenerifying(MT.Ash);
			for (OreDictMaterial tMaterial : ANY.Diamond.mToThis) tMaterial.setGenerifying(MT.Diamond);
			for (OreDictMaterial tMaterial : ANY.Sapphire.mToThis) tMaterial.setGenerifying(MT.Sapphire);
			for (OreDictMaterial tMaterial : ANY.Emerald.mToThis) tMaterial.setGenerifying(MT.Emerald);
			for (OreDictMaterial tMaterial : ANY.Jasper.mToThis) tMaterial.setGenerifying(MT.Jasper);
			for (OreDictMaterial tMaterial : ANY.TigerEye.mToThis) tMaterial.setGenerifying(MT.TigerEyeYellow);
			for (OreDictMaterial tMaterial : ANY.CaF2.mToThis) tMaterial.setGenerifying(MT.CaF2);
			for (OreDictMaterial tMaterial : ANY.Rubber.mToThis) tMaterial.setGenerifying(MT.Rubber);
			for (OreDictMaterial tMaterial : ANY.Plastic.mToThis) tMaterial.setGenerifying(MT.Plastic);
			for (OreDictMaterial tMaterial : ANY.Wood.mToThis) tMaterial.setGenerifying(MT.Wood);
			for (OreDictMaterial tMaterial : ANY.Wax.mToThis) tMaterial.setGenerifying(MT.Wax);
			for (OreDictMaterial tMaterial : ANY.Phosphorus.mToThis) tMaterial.setGenerifying(MT.Phosphorus);
			for (OreDictMaterial tMaterial : ANY.Clay.mToThis) tMaterial.setGenerifying(MT.Clay);
			for (OreDictMaterial tMaterial : ANY.Si.mToThis) tMaterial.setGenerifying(MT.Si);
			String tMakeSteel = "In order to make Steel you just need to melt Iron or Wrought Iron in a Smelting Crucible and apply Air to it using an Engine.";
			String tMakeWroughtIron = "Wrought Iron is created by heating up Iron until " + WroughtIron.mMeltingPoint + " Kelvin to dissolve most unwanted impurities.";
			String tMakeAnnealedCopper = "Annealed Copper is created by heating up Copper until " + AnnealedCopper.mMeltingPoint + " Kelvin to dissolve most unwanted impurities.";
			String tMakeAluminium = "Making Aluminium is a very complicated chemical Process. You will need an LV Electrolyzer, a Mixer, a Corrosion Resistant Crucible or a Smelter, Fluorite, Saltwater, Alumina and a bit more to do it.";
			String tKillWerewolf = "It is also very useful in order to kill Werewolves and alike, since everyone knows how Werewolves are allergic to Silver! It also works on Armor like a kind of Thorns (without the stupid extra armor damage)";
		}
		private static void init31() { // upstream MT.java:None-None
			String tKillSlime = "Somehow this Material dissolves Slimey substances and therefore causes severe damage to Slimes and similar Creatures!";
			Redstone.mDescription = new String[] { "Redstone consists out of many things and has a lot in common with Cinnabar, even though containing much more than just Mercury and Sulfur. Redstone is usually found at places with Rare Earths." };
			Ag.mDescription = new String[] { "Silver, the Material Endermen fear the most. Somehow this Material can interfere with the ability of Endermen to teleport, and even damages them dramatically, as if it were poisonous to them." , tKillWerewolf };
			Cu.mDescription = new String[] { "Copper is a Material, which is needed in almost every electrical Device. But not only that, it is also used to make Bronze, Brass, Cupronickel and some other Alloys." , tKillSlime , tMakeAnnealedCopper };
			AnnealedCopper.mDescription = new String[] { "Annealed Copper is just Copper cleaned from impurities. Therefore some of Coppers natural properties are better than in unpurified Copper." , tKillSlime , tMakeAnnealedCopper };
			Al.mDescription = new String[] { "Aluminium is a shiny rare Metal, that is extremely difficult to setup a production for. You will need to convert Alumina into Aluminium using Chemicals and an Electrolyzer." , tMakeAluminium };
			Al2O3.mDescription = new String[] { "Aluminas are the Materials used to create Aluminium out of. It is basically corroded Aluminium and removing said corrosion can be quite difficult and even requires electricity." , tMakeAluminium };
			OREMATS.Bauxite.mDescription = new String[] { "" , tMakeAluminium };
			Fe.mDescription = new String[] { "Iron, the most needed Material in Minecraft, unless you have an Iron Titan or something. Nearly every Mod needs it in order to proceed with its Machines. It exists in many Shapes and Forms, such as" , "Compressed Iron, Wrought Iron, Steel, HSLA Steel, Dark Iron, Deep Iron, Meteoric Iron, Conductive Iron, Electrical Steel, Pulsating Iron, Mutated Iron, Shadow Iron, Ironwood and many many more things." , "It is a very useful Material as it is required to make Steel, the Stuff almost everything Technological is made of. You can also make Wrought Iron, to craft Tools slightly better than Iron Tools." , tMakeWroughtIron , tMakeSteel };
			WroughtIron.mDescription = new String[] { "Wrought Iron, a Material that is worth as much as Iron. It is just a step between Iron itself and Steel, and is just a cleaner Version of Iron. It is also a slightly better Tool Material than Iron." , tMakeWroughtIron , tMakeSteel };
			Steel.mDescription = new String[] { "Steel, the Material Standard Machinery is made of. Cheap, abundant and common, as it only requires Iron and some source of Carbon. There are many different Ways of making Steel supplied by many Mods." , tMakeSteel };
			C.mDescription = new String[] { "Carbon. Not much to say about it. It is used to make Steel. Uhhm, maybe it has some other uses explained in a later Edition of this Book." , tMakeSteel };
			Diamond.mDescription = DiamondPink.mDescription = DiamondIndustrial.mDescription = new String[] { "Diamonds, the shiny Stuff every Minecrafter looks for. Some people may even create living Horses made of Diamonds." , tMakeSteel };
			Graphite.mDescription = new String[] { "Graphite is a Source of Carbon. It can also be used to make Steel directly." , tMakeSteel };
			DamascusSteel.mDescription = new String[] { "Damascus Steel is a very rare kind of Steel. Rare meaning actually nobody knows how to produce it anymore as they are all dead. Maybe ask a Zombie or a Skeleton on how to make it?" , "Because of that, it can only be found in old Structures such as Dungeons. Sometimes Villagers stumble upon this rare Steel or things made of it and take it home to give it to their Blacksmith." , "It is a very good Tool Material and even surpasses Blue Steel in its Quality. It has Vanadium and Tungsten impurities which improve the stability of the Carbides when Forging at higher Temperatures." };
			Craponite.mDescription = new String[] { "Craponite is a very exquisite Material used only in the fanciest Jewelry. It looks best in combination with Peridot. That is why Teleshopping Channels often sell Peridot Craponite Earrings and alike." };
			Ir.mDescription = new String[] { "Iridium, a very rare Metal used in only the most advanced Technology. Its properties are very versatile and used in many advanced Devices." , "Iridium is a Metal that can stabilise a Tesseract, so that it can transfer much larger Amounts of Matter and Energy." , "As Weapon it is very useful in order to kill Shapeshifters, since it is highly Toxic to them. Now, if there were Shapeshifters in our World this Information would be more useful." };
			Desh.mDescription = new String[] { "Desh is a Material that is very hard to alloy, which is typically only found naturally on Mars (the Planet, not the God, Pokemon Trainer or Candy Bar)." , "In order to create this Alloy, it is advised to insert Materials with a low Boiling Point such as Lithium very last and all at once into the Crucible." , "It is also advised to use a Crucible with a high Heat Capacity as well as drip feeding low Amounts of Material, so the Temperature does not go down too much upon insertion." , "Desh is a completely fictional Material made up by Galacticraft, and is only used by Galacticraft itself, it's Addons and also HBM's Mods." };
			Tc.mDescription = new String[] { "Technetium, named after Gregorius Techneticies, is the first Element of the Periodic System, which has no stable Isotope. Its usage is mostly limited to Medical Applications." , "Fun Fact:\nEvery Element, which has no Elemental Stats assigned to, will automatically default to the Elemental Stats of Technetium." };
			Mcg.mDescription = new String[] { "Mac-Guffium, the most useful Material of them all. It can be used for everything, as long as its underlying Science is too obscure to be understood." , "Possible Applications are: Time Travel, Infinite Impossibility Drives, World Peace, World Domination, World War, World War II, World War IV, Curing all Diseases, Stopping Global Warming," , "Controlling the Weather, Turning everything into Gold, Turning you into a God, Killing a God, Making Dark Matter an Energy Source, Tesseracts, Wormholes, Black Holes, Bending Space and Time, Tardis," , "Ascension to a higher Level of existence, Invisibility, Killing Chuck Norris, Cloning, Making Profit of collecting Underpants, Making the Impossible possible, Love Rays, Hate Rays, Perpetuum Mobiles," , "Making you able to hold and use Thors Hammer, and ofcourse baking delicious Cookies." };
			Nq.mDescription = new String[] { "This extremly rare Material is called Naquadah. Its properties make it very useful in highly advanced Technology." , "It is a Superconductor even at very high Temperatures, what makes it seeming Ideal for electric Wiring as long as the amount of transferred Electricity is not too large." , "Another property of it is that it emits Gamma Radiation when supplied with enough Electricity, due to creating Positrons out of said Energy, which then collide with Electrons very quickly." , "However that property makes it very unstable and can result in a Nuclear Explosion when not regulated properly, especially with molten Naquadah." };
			Nq_528.mDescription = new String[] { "This is a heavy Isotope of Naquadah mainly used to generate Energy, or to create Bombs." , "Due to being less stable than regular Naquadah, supplying this Isotope with Electricity results in a very strong Nuclear Reaction, which when regulated properly can result in a constant Energy Source." , "The instability increases when this Isotope is molten, resulting in a larger Energy Output when used in Reactors." , "Its instability is also often used to make Naquadah Bombs, which are much more destructive than the regular Plutonium Nukes. It doesn't make any difference to use the molten or the solid Isotope" , "for Bombs, since its explosion heats up the whole Bomb far above the melting Point anyways." };
			Nq_522.mDescription = new String[] { "Naquadria, a light and extremely unstable Isotope of Naquadah. It can be used for Reactors as well as Bombs and is much stronger than heavy Naquadah." , "It is created when Naquadah is receiving too large amounts of Gamma Radiation. During its creation it emits more Gamma Radiation turning adjacent Naquadah into Naquadria too." , "The Natural origin of this Isotope is a Gamma Ray Burst, which hits a Naquadah Vein directly. Planets to which that happens are then turning into a Desert due to the enormous Heat created by the Ray." , "Gamma Bombs which are exploding far enough away from a Naquadah Vein, or just a strong enough Gamma Ray, can also trigger a Chain Reaction turing Naquadah into Naquadria." , "However most of the time a Gamma Ray hits a Naquadah Vein, it just explodes, what is the main Factor of Naquadria being very rare. Also Naquadria turns back into Naquadah after a few thousand years." };
			Mauftrium.mHandleMaterial = Elvorium.mHandleMaterial = MuspelheimPower.mHandleMaterial = NiflheimPower.mHandleMaterial = ElvenElementium.mHandleMaterial = ElvenDragonstone.mHandleMaterial = Manasteel.mHandleMaterial = Terrasteel.mHandleMaterial = ManaDiamond.mHandleMaterial = Thaumium.mHandleMaterial = ANY.WoodMagical;
			GaiaSpirit.mHandleMaterial = ElvenElementium;
			FierySteel.mHandleMaterial = Fireleaf.mHandleMaterial = MeteoflameSteel.mHandleMaterial = MeteoflameBlackSteel.mHandleMaterial = MeteoflameBlueSteel.mHandleMaterial = MeteoflameRedSteel.mHandleMaterial = FlamascusSteel.mHandleMaterial = Firestone.mHandleMaterial = ANY.Blaze;
			Endium.mHandleMaterial = Endstone;
			SpectreIron.mHandleMaterial = Obsidian;
			EnderAmethyst.mHandleMaterial = Meteorite.mHandleMaterial = Kreknorite.mHandleMaterial = Sugilite.mHandleMaterial = ANY.Iron;
			DarkMatter.mHandleMaterial = Diamond;
			RedMatter.mHandleMaterial = DarkMatter;
			Desh.mHandleMaterial = Desh;
			Etx.mHandleMaterial = Etx;
			Vb.mHandleMaterial = VibraniumSteel.mHandleMaterial = VibraniumSilver.mHandleMaterial = VibraniumSteel;
			Vibramantium.mHandleMaterial = Vibramantium;
			VoidMetal.mHandleMaterial = InfusedAir.mHandleMaterial = InfusedBalance.mHandleMaterial = InfusedDull.mHandleMaterial = InfusedEarth.mHandleMaterial = InfusedEntropy.mHandleMaterial = InfusedFire.mHandleMaterial = InfusedOrder.mHandleMaterial = InfusedWater.mHandleMaterial = InfusedVis.mHandleMaterial = DarkThaumium.mHandleMaterial = ANY.MagicIron;
		}	}

	/** Upstream MT.DATA :3593-3695 is a pure OreDictItemData[]/OreDictPrefix (OP.dat) table set. OreDictItemData and OP belong to the oredict-manager/prefix tasks (cards 4/5), so this table is deferred; nothing in the Phase-1 material graph consumes it. */
	public static class DATA {
		// deferred: WIRES_01..CABLES_16, Dye_Materials, etc. (upstream MT.java:3595-3694)
	}

	public static class OREMATS {
		@Deprecated @SuppressWarnings("hiding")
		public static final OreDictMaterial Pyrolusite = MnO2, Rutile = TiO2, Hematite = Fe2O3, Magnesite = MgCO3, Gypsum = MT.Gypsum, Bentonite = MT.Bentonite, FullersEarth = Palygorskite, Kaolinite = MT.Kaolinite;
		
		public static final OreDictMaterial
		Cassiterite             = oredustelec( 9108, "Cassiterite"               , SET_METALLIC  , 220, 220, 220, 255, MORTAR, FURNACE, "CassiteriteSand"                                             ).setSmelting(Sn   , 3*U4).addSourceOf(Sn       ).setMcfg( 1, Sn             , 1*U, O                , 2*U)                                                                                                .heat(3 * Sn.mMeltingPoint / 2), CassiteriteSand = Cassiterite,
		Garnierite              = oredustelec( 9118, "Garnierite"                , SET_METALLIC  ,  50, 200,  70, 255, MORTAR, BLACKLISTED_SMELTER, MAGNETIC_PASSIVE, WASHING_PERSULFATE              ).setSmelting(Ni   , 3*U4).addSourceOf(Ni       ).setMcfg( 1, Ni             , 1*U, O                , 1*U)                                                                                                .qual(0),
		Uraninite               = oredustdcmp( 9134, "Uraninite"                 , SET_RAD       ,  35,  35,  35, 255, BLACKLISTED_SMELTER                                                            ).setSmelting(U_238,   U3).addSourceOf(U_238    ).setMcfg( 1, U_238          , 1*U, O                , 2*U)                                                                                                ,
		Magnetite               = oredustdcmp( 9122, "Magnetite"                 , SET_METALLIC  ,  30,  30,  30, 255, MORTAR, MELTING, MAGNETIC_PASSIVE                                              )                         .addSourceOf(Fe       ).setMcfg( 0, Fe             , 3*U, O                , 4*U)                                                                                                .qual(0).heat(Fe.mMeltingPoint),
		BasalticMineralSand     = oredustdcmp( 9003, "Basaltic Mineral Sand"     , SET_METALLIC  ,  40,  50,  40, 255, MORTAR, MELTING, MAGNETIC_PASSIVE                                              )                         .addSourceOf(Fe       ).setMcfg( 0, Fe             , 3*U, O                , 4*U)                                                                                                .qual(0).heat(Fe.mMeltingPoint),
		GraniticMineralSand     = oredustdcmp( 9004, "Granitic Mineral Sand"     , SET_METALLIC  ,  40,  60,  60, 255, MORTAR, MELTING, MAGNETIC_PASSIVE                                              )                         .addSourceOf(Fe       ).setMcfg( 0, Fe             , 3*U, O                , 4*U)                                                                                                .qual(0).heat(Fe.mMeltingPoint),
		
		Realgar                 = oredustdcmp( 9109, "Realgar"                   , SET_EMERALD   , 157,  33,  35, 255, G_GEM_ORES_TRANSPARENT, MORTAR, BRITTLE, FURNACE, CRYSTAL                      ).setSmelting(As   ,   U3).addSourceOf(As       ).uumMcfg( 0, As             , 1*U, S                , 1*U)                                                                                                .qual(0),
		Cinnabar                = oredustcent( 9114, "Cinnabar"                  , SET_REDSTONE  , 150,   0,   0, 255, G_GEM_ORES_TRANSPARENT, MORTAR, BRITTLE, CRYSTAL, PULVERIZING_CINNABAR         ).setSmelting(Hg   ,   U3).addSourceOf(Hg       ).uumMcfg( 0, Hg             , 1*U, S                , 1*U)                                                                                                ,
		Molybdenite             = oredustdcmp( 9123, "Molybdenite"               , SET_METALLIC  ,  25,  25,  25, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER, WASHING_FIRESTONE                     ).setSmelting(Mo   ,   U4).addSourceOf(Mo       ).uumMcfg( 0, Mo             , 1*U, S                , 2*U)                                                                                                ,
		Sphalerite              = oredustdcmp( 9130, "Sphalerite"                , SET_DULL      , 222, 222,   0, 255, G_GEM_ORES, MORTAR, FURNACE, WASHING_PERSULFATE                                ).setSmelting(Zn   ,   U3).addSourceOf(Zn       ).uumMcfg( 0, Zn             , 1*U, S                , 1*U)                                                                                                ,
		Stibnite                = oredustdcmp( 9131, "Stibnite"                  , SET_METALLIC  ,  70,  70,  70, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(Sb   ,   U4).addSourceOf(Sb       ).uumMcfg( 0, Sb             , 2*U, S                , 3*U)                                                                                                .heat(823),
		Pentlandite             = oredustdcmp( 9145, "Pentlandite"               , SET_DULL      , 165, 150,   5, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER, MAGNETIC_PASSIVE, WASHING_PERSULFATE  ).setSmelting(Ni   ,   U3).addSourceOf(Ni       ).uumMcfg( 0, Ni             , 9*U, S                , 8*U)                                                                                                .qual(0), // (Fe,Ni)9S8
		Chalcopyrite            = oredustdcmp( 9111, "Chalcopyrite"              , SET_DULL      , 160, 120,  40, 255, G_GEM_ORES, MORTAR, FURNACE                                                    ).setSmelting(Cu   , 2*U9).addSourceOf(Cu,Fe    ).uumMcfg( 0, Cu             , 1*U, Fe               , 1*U, S                , 2*U)                                                                        .qual(0),
		Arsenopyrite            = oredustdcmp( 9216, "Arsenopyrite"              , SET_CUBE_SHINY, 250, 240,  30, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(As   ,   U4).addSourceOf(Fe,As    ).uumMcfg( 0, Fe             , 1*U, As               , 1*U, S                , 1*U)                                                                        .qual(0),
		Cobaltite               = oredustdcmp( 9115, "Cobaltite"                 , SET_METALLIC  ,  80,  80, 250, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER, MAGNETIC_PASSIVE, WASHING_PERSULFATE  ).setSmelting(Co   ,   U4).addSourceOf(Co,As    ).uumMcfg( 0, Co             , 1*U, As               , 1*U, S                , 1*U)                                                                        .qual(0),
		Galena                  = oredustdcmp( 9117, "Galena"                    , SET_DULL      , 100,  60, 100, 255, G_GEM_ORES, MORTAR, FURNACE                                                    ).setSmelting(Pb   ,   U3).addSourceOf(Pb,Ag    ).uumMcfg( 0, Pb             , 3*U, Ag               , 3*U, S                , 2*U)                                                                        ,
		Cooperite               = oredustdcmp( 9116, "Cooperite"                 , SET_METALLIC  , 130, 160, 230, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER, WASHING_MERCURY, "Sheldonite"         ).setSmelting(Pt   ,   U3).addSourceOf(Pt,Ni,Pd ).uumMcfg( 0, Pt             , 3*U, Ni               , 1*U, Pd               , 1*U, S                , 1*U)                                                .setLocal("Sheldonite"),
		Tetrahedrite            = oredustdcmp( 9132, "Tetrahedrite"              , SET_DULL      , 200,  32,   0, 255, G_GEM_ORES, MORTAR, FURNACE, WASHING_PERSULFATE                                ).setSmelting(Cu   ,   U4).addSourceOf(Cu,Sb,Fe ).uumMcfg( 0, Cu             , 3*U, Sb               , 1*U, Fe               , 1*U, S                , 3*U)                                                , // Cu3SbS3 + x(Fe,Zn)6Sb2S9
		Kesterite               = oredustdcmp( 9213, "Kesterite"                 , SET_DULL      , 105, 155, 105, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(Cu   ,   U9).addSourceOf(Cu,Zn,Sn ).uumMcfg( 0, Cu             , 2*U, Zn               , 1*U, Sn               , 1*U, S                , 4*U)                                                ,
		Stannite                = oredustdcmp( 9214, "Stannite"                  , SET_METALLIC  , 155, 145,  55, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(Cu   ,   U9).addSourceOf(Cu,Fe,Sn ).uumMcfg( 0, Cu             , 2*U, Fe               , 1*U, Sn               , 1*U, S                , 4*U)                                                ,
		Barite                  = oredustelec( 9160, "Barite"                    , SET_DULL      , 230, 235, 255, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(Ba   ,   U9).addSourceOf(Ba       ).uumMcfg( 0, Ba             , 1*U, S                , 1*U, O                , 4*U)                                                                        .heat(1853),
		Celestine               = oredustelec( 9110, "Celestine"                 , SET_DULL      , 200, 205, 240, 255, G_GEM_ORES, MORTAR, BLACKLISTED_SMELTER                                        ).setSmelting(Sr   ,   U9).addSourceOf(Sr       ).uumMcfg( 0, Sr             , 1*U, S                , 1*U, O                , 4*U)                                                                        ,
		
		Scheelite               = oredustdcmp( 9128, "Scheelite"                 , SET_DULL      , 200, 140,  20, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE, "CalciumTungstate"                     )                         .addSourceOf(W        ).uumMcfg( 0, Ca             , 1*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		Wolframite              = oredustdcmp( 9217, "Wolframite"                , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                         )                         .addSourceOf(W        ).uumMcfg( 0, Mg             , 1*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		Ferberite               = oredustdcmp( 9194, "Ferberite"                 , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                         )                         .addSourceOf(W,Fe     ).uumMcfg( 0, Fe             , 1*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		Huebnerite              = oredustdcmp( 9195, "Huebnerite"                , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE, "Gyubnera"                             )                         .addSourceOf(W,Mn     ).uumMcfg( 0, Mn             , 1*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		Tungstate               = oredustdcmp( 9133, "Tungstate"                 , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                         )                         .addSourceOf(W,Li     ).uumMcfg( 0, Li             , 2*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		// TODO Actual Processing, but I don't know what could do it
		Stolzite                = oredustdcmp( 9193, "Stolzite"                  , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE, "Raspite"                              ).setSmelting(WO3  ,4* U6).addSourceOf(W,Pb     ).uumMcfg( 0, Pb             , 1*U, WO3              , 4*U, O                , 1*U)                                                                        .qual(3),
		Russellite              = oredustdcmp( 9196, "Russellite"                , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                         ).setSmelting(WO3  ,4* U9).addSourceOf(W,Bi     ).setMcfg( 0, Bi             , 2*U, WO3              , 4*U, O                , 3*U)                                                                        .qual(3),
		Pinalite                = oredustdcmp( 9197, "Pinalite"                  , SET_DULL      ,  55,  50,  35, 255, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                         ).setSmelting(WO3  ,4*U11).addSourceOf(W,Pb     ).uumMcfg( 0, Pb             , 3*U, WO3              , 4*U, Cl               , 2*U, O                , 2*U)                                                .qual(3), 
		
		Wollastonite            = oredustelec( 9164, "Wollastonite"              , SET_DULL      , 240, 240, 240, 255, BLACKLISTED_SMELTER                                                            )                                                .setMcfg( 0, Ca             , 1*U, SiO2             , 3*U, O                , 1*U)                                                                        , // CaSiO3
		
		Zeolite                 = oredustelec( 9165, "Zeolite"                   , SET_DULL      , 240, 230, 230, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Na,Al    ).setMcfg( 0, Al2O3          , 5*U, Na               , 2*U, SiO2             ,12*U, H2O              , 6*U, O                , 1*U)                        , // Na2Al2Si4O12 2H2O
		Pollucite               = oredustelec( 9147, "Pollucite"                 , SET_DULL      , 240, 210, 210, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Cs,Al    ).setMcfg( 0, Al2O3          , 5*U, Cs               , 2*U, SiO2             ,12*U, H2O              , 6*U, O                , 1*U)                        , // Cs2Al2Si4O12 2H2O (also a source of Rb)
		
		BrownLimonite           = oredustdcmp( 9106, "Brown Limonite"            , SET_METALLIC  , 200, 100,   0, 255, MORTAR, MELTING, MAGNETIC_PASSIVE                                              ).setSmelting(Fe2O3,   U2).addSourceOf(Fe       ).setMcfg( 0, Fe             , 1*U, H                , 1*U, O                , 2*U)                                                                        .qual(0).heat(1523), // FeO(OH)
		YellowLimonite          = oredustdcmp( 9137, "Yellow Limonite"           , SET_METALLIC  , 200, 200,   0, 255, MORTAR, MELTING, MAGNETIC_PASSIVE, "BogIron"                                   ).setSmelting(Fe2O3,   U2).addSourceOf(Fe       ).setMcfg( 0, Fe             , 1*U, H                , 1*U, O                , 2*U)                                                                        .qual(0).heat(1523), // FeO(OH) + a bit Ni and Co
		
		Ferrovanadium           = oredustcent( 9143, "Vanadium Magnetite"        , SET_METALLIC  ,  35,  35,  60, 255, MORTAR, MELTING, MOLTEN, MAGNETIC_PASSIVE, WASHING_FIRESTONE, "Ferrovanadium"  )                         .addSourceOf(V,Fe     ).setMcfg( 0, Magnetite      , 1*U, V2O5             , 1*U)                                                                                                , // Mixture of Fe3O4 and V2O5. Technically Ferrovanadium is an Alloy of Iron and Vanadium. I should not have blindly copied PFAA and assumed it was an Ore.
		Tantalite               = oredustelec( 9148, "Tantalite"                 , SET_METALLIC  , 145,  80,  40, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                 )                         .addSourceOf(Ta,Mn    ).setMcfg( 0, Ta2O5          , 7*U, MnO2             , 1*U)                                                                                                , // (Fe, Mn)Ta2O6
		Columbite               = oredustelec( 9246, "Columbite"                 , SET_METALLIC  ,  65,  77,  14, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                 )                         .addSourceOf(Nb,Mn    ).setMcfg( 0, Nb2O5          , 7*U, MnO2             , 1*U)                                                                                                , // (Fe, Mn)Nb2O6
		Coltan                  = oredustcent( 9247, "Coltan"                    , SET_METALLIC  , 105,  83,  66, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                 )                         .addSourceOf(Ta,Nb,Mn ).setMcfg( 0, Tantalite      , 1*U, Columbite        , 1*U)                                                                                                ,
		
		Ilmenite                = oredustdcmp( 9120, "Ilmenite"                  , SET_METALLIC  ,  70,  55,  50, 255, MORTAR, MELTING, MOLTEN, MAGNETIC_PASSIVE, WASHING_FIRESTONE, "Illmenite", "TitaniumIron")               .addSourceOf(Ti,Fe    ).uumMcfg( 0, Fe             , 1*U, Ti               , 1*U, O                , 3*U)                                                                        .qual(2),
		Bauxite                 = oredustdcmp( 9105, "Bauxite"                   , SET_DULL      , 200, 100,   0, 255, MORTAR, BLACKLISTED_SMELTER, APPROXIMATE                                       )                         .addSourceOf(Al,Ti    ).setMcfg( 0, TiO2           , 1*U, Ilmenite         , 2*U, Al2O3            , 2*U)                                                                        .qual(2).heat(2800),
		Chromite                = oredustelec( 9113, "Chromite"                  , SET_METALLIC  ,  35,  20,  15, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_FIRESTONE                                 ).setSmelting(Cr   , 2*U9).addSourceOf(Cr,Fe    ).setMcfg( 0, Fe             , 1*U, Cr               , 2*U, O                , 4*U)                                                                        .qual(0),
		Powellite               = oredustcent( 9124, "Powellite"                 , SET_DULL      , 255, 255,   0, 255, MORTAR, BLACKLISTED_SMELTER                                                    ).setSmelting(Mo   ,   U9).addSourceOf(Mo       ).setMcfg( 0, Ca             , 1*U, Mo               , 1*U, O                , 4*U)                                                                        ,
		Wulfenite               = oredustcent( 9136, "Wulfenite"                 , SET_DULL      , 255, 128,   0, 255, MORTAR, BLACKLISTED_SMELTER                                                    ).setSmelting(Mo   ,   U9).addSourceOf(Mo,Pb    ).setMcfg( 0, Pb             , 1*U, Mo               , 1*U, O                , 4*U)                                                                        ,
		Bastnasite              = oredustelec( 9144, "Bastnasite"                , SET_FINE      , 200, 110,  45, 255, MORTAR, BLACKLISTED_SMELTER                                                    ).setSmelting(Ce   ,   U9).addSourceOf(Ce,F     ).setMcfg( 0, Ce             , 1*U, C                , 1*U, F                , 1*U, O                , 3*U)                                                , // (Ce, La, Y)CO3F
		Pitchblende             = oredustcent( 9155, "Pitchblende"               , SET_RAD       , 100, 110,   0, 255, MORTAR, BLACKLISTED_SMELTER                                                    ).setSmelting(U_238,   U5).addSourceOf(U_238,Th ).setMcfg( 0, Uraninite      , 3*U, Th               , 1*U, Pb               , 1*U)                                                                        ,
		Malachite               = oredustelec( 9156, "Malachite"                 , SET_LAPIS     ,   5,  95,   5, 255, MORTAR, G_GEM_ORES, FURNACE, WASHING_PERSULFATE                                ).setSmelting(Cu   ,   U6).addSourceOf(Cu       ).setMcfg( 0, Cu             , 2*U, CO3              , 4*U, H                , 2*U, O                , 2*U)                                                , // Cu2CO3(OH)2
		Bromargyrite            = oredustelec( 9210, "Bromargyrite"              , SET_DULL      ,  90,  45,  10, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_MERCURY                                   ).setSmelting(Ag   ,   U3).addSourceOf(Ag,Br    ).setMcfg( 0, Ag             , 1*U, Br               , 1*U)                                                                                                ,
		Smithsonite             = oredustelec( 9211, "Smithsonite"               , SET_DULL      , 110, 223, 210, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_MERCURY, WASHING_PERSULFATE               ).setSmelting(Zn   ,   U6).addSourceOf(Zn       ).setMcfg( 0, Zn             , 1*U, C                , 1*U, O                , 3*U)                                                                        ,
		Sperrylite              = oredustelec( 9212, "Sperrylite"                , SET_SHINY     , 105, 105, 105, 255, MORTAR, BLACKLISTED_SMELTER, WASHING_MERCURY                                   ).setSmelting(Pt   ,   U4).addSourceOf(Pt,As    ).setMcfg( 0, Pt             , 1*U, As               , 2*U)                                                                                                ,
		
		Perlite                 = oredustdcmp( 9138, "Perlite"                   , SET_DULL      ,  30,  20,  30, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 1, Obsidian       , 1*U, H2O              , 1*U)                                                                                                ,
		Trona                   = oredustelec( 9159, "Trona"                     , SET_METALLIC  , 135, 135,  95, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 6, Na2CO3         , 6*U, H2O              , 6*U)                                                                                                ,
		Mirabilite              = oredustdcmp( 9157, "Mirabilite"                , SET_DULL      , 240, 250, 210, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 7, Na2SO4         , 7*U, H2O              ,30*U)                                                                                                ,
		Bischofite              = oredustdcmp( 9221, "Bischofite"                , SET_ROUGH     ,  99, 104, 118, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 3, MgCl2          , 3*U, H2O              , 6*U)                                                                                                ,
		
		Borax                   = oredustdcmp( 9139, "Borax"                     , SET_FINE      , 250, 250, 250, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(B,Na     ).setMcfg( 0, Na             , 2*U, B                , 4*U, H2O              ,30*U, O                , 7*U)                                                ,
		Diatomite               = oredustcent( 9001, "Diatomite"                 , SET_DULL      , 225, 225, 225, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 0, Flint          , 8*U, Fe2O3            , 1*U, Sapphire         , 1*U)                                                                        ,
		
		Spodumene               = oredustelec( 9146, "Spodumene"                 , SET_DULL      , 190, 170, 170, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al,Li    ).setMcfg( 0, Al2O3          , 5*U, Li               , 2*U, SiO2             ,12*U, O                , 1*U)                                                , // LiAl(SiO3)2
		Lepidolite              = oredustelec( 9149, "Lepidolite"                , SET_FINE      , 240,  50, 140, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al,K,Li,F).setMcfg( 0, Al2O3          ,10*U, K                , 1*U, Li               , 3*U, F                , 2*U, O                , 6*U)                        , // K(Li,Al,Rb)3(Al,Si)4O10(F,OH)2
		Glauconite              = oredustelec( 9150, "Glauconite"                , SET_DULL      , 130, 180,  60, 255, MORTAR, BLACKLISTED_SMELTER, "GlauconiteSand"                                  )                         .addSourceOf(Al,K     ).setMcfg( 0, Al2O3          ,10*U, K                , 1*U, Mg               , 2*U, H2O              , 3*U, O                , 7*U)                        , GlauconiteSand = Glauconite, // (K,Na)(Fe3+,Al,Mg)2(Si,Al)4O10(OH)2
//      GlauconiteSand          = oredustelec( 9151, "Glauconite Sand"           , SET_DULL      , 130, 180,  60, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al,K     ).setMcfg( 0, Al2O3          ,10*U, K                , 1*U, Mg               , 2*U, H2O              , 3*U, O                , 7*U)                        , // (K,Na)(Fe3+,Al,Mg)2(Si,Al)4O10(OH)2
		Vermiculite             = oredustelec( 9152, "Vermiculite"               , SET_METALLIC  , 200, 180,  15, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al       ).setMcfg( 0, Al2O3          ,10*U, Fe               , 3*U, SiO2             ,12*U, H2O              ,12*U, H                , 2*U)                        , // (Mg+2, Fe+2, Fe+3)3 [(AlSi)4O10] (OH)2 4H2O)
		Mica                    = oredustelec( 9158, "Mica"                      , SET_FINE      , 195, 195, 205, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al,K,F   ).setMcfg( 0, Al2O3          ,15*U, K                , 2*U, SiO2             ,18*U, F                , 4*U)                                                , // KAl2(AlSi3O10)(F,OH)2
		Kyanite                 = oredustelec( 9166, "Kyanite"                   , SET_FLINT     , 110, 110, 250, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al       ).setMcfg( 0, Al2O3          , 5*U, SiO2             , 3*U)                                                                                                , // Al2SiO5
		Alunite                 = oredustelec( 9162, "Alunite"                   , SET_METALLIC  , 225, 180,  65, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                         .addSourceOf(Al,K     ).setMcfg( 0, Al2O3          ,15*U, KOH              , 6*U, SO3              ,16*U, H2O              ,15*U, O                , 9*U)                        , // KAl3(SO4)2(OH)6
		
		GarnetSand              = oredustcent( 9005, "Garnet Sand"               , SET_SAND      , 200, 100,   0, 255, MORTAR, BLACKLISTED_SMELTER                                                    )                                                .setMcfg( 0, Almandine      , 1*U, Andradite        , 1*U, Grossular        , 1*U, Pyrope           , 1*U, Spessartine      , 1*U, Uvarovite        , 1*U),
		QuartzSand              = oredustcent( 9006, "Quartz Sand"               , SET_SAND      , 200, 200, 200, 255, MORTAR, BLACKLISTED_SMELTER, QUARTZ                                            ).setSmelting(SiO2 ,   U3)                       .setMcfg( 0, CertusQuartz   , 1*U, MilkyQuartz      , 1*U)                                                                                                ,
		
		DiduraniumTrioxide      = oredustelec( 9198, "Diduranium Trioxide"       , SET_DULL      ,  45, 145, 145, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn       ).setMcfg( 0, Dn             , 2*U, O                , 3*U)                                                                                                .qual(4),
		DuraniumHexafluoride    = oredustelec( 9199, "Duranium Hexafluoride"     , SET_DULL      ,  25, 175, 125, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn,F     ).setMcfg( 0, Dn             , 1*U, F                , 6*U)                                                                                                .qual(4),
		DuraniumHexachloride    = oredustelec( 9200, "Duranium Hexachloride"     , SET_DULL      ,  75, 175, 145, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn       ).setMcfg( 0, Dn             , 1*U, Cl               , 6*U)                                                                                                .qual(4),
		DuraniumHexabromide     = oredustelec( 9201, "Duranium Hexabromide"      , SET_DULL      ,  45, 125, 175, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn,Br    ).setMcfg( 0, Dn             , 1*U, Br               , 6*U)                                                                                                .qual(4),
		DuraniumHexaiodide      = oredustelec( 9202, "Duranium Hexaiodide"       , SET_DULL      ,  75, 125, 175, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn,I     ).setMcfg( 0, Dn             , 1*U, I                , 6*U)                                                                                                .qual(4),
		DuraniumHexaastatide    = oredustelec( 9203, "Duranium Hexaastatide"     , SET_DULL      ,  25, 145, 175, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Dn,At    ).setMcfg( 0, Dn             , 1*U, At               , 6*U)                                                                                                .qual(4),
		TritaniumDioxide        = oredustelec( 9204, "Tritanium Dioxide"         , SET_DULL      ,  25, 185, 125, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn       ).setMcfg( 0, Tn             , 1*U, O                , 2*U)                                                                                                .qual(4),
		TritaniumHexafluoride   = oredustelec( 9205, "Tritanium Hexafluoride"    , SET_DULL      ,  85, 125, 125, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn,F     ).setMcfg( 0, Tn             , 1*U, F                , 6*U)                                                                                                .qual(4),
		TritaniumHexachloride   = oredustelec( 9206, "Tritanium Hexachloride"    , SET_DULL      ,  55, 185, 155, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn       ).setMcfg( 0, Tn             , 1*U, Cl               , 6*U)                                                                                                .qual(4),
		TritaniumHexabromide    = oredustelec( 9207, "Tritanium Hexabromide"     , SET_DULL      ,  55, 125, 155, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn,Br    ).setMcfg( 0, Tn             , 1*U, Br               , 6*U)                                                                                                .qual(4),
		TritaniumHexaiodide     = oredustelec( 9208, "Tritanium Hexaiodide"      , SET_DULL      ,  85, 185, 185, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn,I     ).setMcfg( 0, Tn             , 1*U, I                , 6*U)                                                                                                .qual(4),
		TritaniumHexaastatide   = oredustelec( 9209, "Tritanium Hexaastatide"    , SET_DULL      ,  25, 125, 185, 255, BLACKLISTED_SMELTER                                                            )                         .addSourceOf(Tn,At    ).setMcfg( 0, Tn             , 1*U, At               , 6*U)                                                                                                .qual(4);
	}
	
	/** Had to move a chunk of Materials into its own Class due to space Issues... */
	public static class STONES {
		@SuppressWarnings("hiding")
		public static final OreDictMaterial
		SpaceRock    = stone    ( 8512, "Space Stone" , SET_SPACE ,  99,  99,  99, 255, MELTING, MOLTEN)                                                                                                                                                     .qual(1, 5.0, 32, 1).setGenerifying(Stone).addSourceOf(He,He_3).setLocal("Space"),
		MoonRock     = stone    ( 8513, "Moon Stone"              , 189, 189, 189, 255, MELTING, MOLTEN)                                                                                                                                                     .qual(1, 5.0, 32, 1).setGenerifying(Stone).setLocal("Moon"),
		MoonTurf     = stone    ( 8514, "Moon Turf"               , 207, 207, 207, 255)                                                                                                                                                                      .qual(1, 3.0, 16, 1).setGenerifying(Stone).addSourceOf(He,He_3),
		MarsRock     = stone    ( 8515, "Mars Stone"              , 189,  77,  77, 255, MELTING, MOLTEN)                                                                                                                                                     .qual(1, 5.0, 32, 1).setGenerifying(Stone).setLocal("Mars"),
		MarsSand     = stone    ( 8516, "Mars Sand"               , 207,  66,  66, 255)                                                                                                                                                                      .qual(1, 3.0, 16, 1).setGenerifying(Stone),
		SkyStone     = stonecent( 8528, "Sky Stone"               ,  81,  92,  96, 255)                                                            .setMcfg( 0, Peridot        , 2*U, RareEarth        , 1*U, MeteoricIron     , 1*U, Obsidian         , 5*U).qual(1, 5.0, 64, 2).setGenerifying(Stone).heat(2200),
		Holystone    = stone    ( 8522, "Holystone"               , 172, 172, 172, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).heat(2000),
		Livingrock   = stone    ( 8521, "Livingrock"              , 195, 205, 195, 255)                                                                                                                                                                      .qual(1, 5.0,128, 2).setGenerifying(Stone).heat(1800),
		Deadrock     = stone    ( 8523, "Deadrock"                , 153, 153, 168, 255, UNBURNABLE)                                                                                                                                                          .qual(1, 5.0,128, 2).setGenerifying(Stone).heat(1800),
		Betweenstone = stone    ( 8519, "Betweenstone"            , 100, 160, 110, 255)                                                                                                                                                                      .qual(1, 4.0, 32, 1).setGenerifying(Stone).heat(1000),
		Pitstone     = stone    ( 8520, "Pitstone"                ,  40,  50,  30, 255)                                                                                                                                                                      .qual(1, 4.0, 32, 1).setGenerifying(Stone).heat(1200),
		Cragrock     = stone    ( 8524, "Cragrock"                ,  93,  96, 107, 255)                                                                                                                                                                      .qual(1, 4.0, 32, 1).setGenerifying(Stone).heat(1400),
		Templerock   = brick    ( 8525, "Templerock"              , 171, 158, 106, 255, WITHER_PROOF)                                                                                                                                                        .qual(1, 5.0,128, 1).setGenerifying(Stone).heat(1600),
		Mazestone    = brick    ( 8526, "Mazestone"               , 110, 120, 110, 255, WITHER_PROOF)                                                                                                                                                        .qual(1, 5.0,128, 3).setGenerifying(Stone).heat(2000),
		Castlerock   = brick    ( 8527, "Castlerock"              , 198, 185, 186, 255, WITHER_PROOF)                                                                                                                                                        .qual(1, 5.0,128, 3).setGenerifying(Stone).heat(2000),
		Umber        = stone    ( 8517, "Umber"                   , 111,  77,  11, 255)                                                                                                                                                                      .qual(1, 3.0, 32, 1).setGenerifying(Stone).heat( 987).setLocal("Umberstone"),
		Shale        = stonecent( 9190, "Shale"                   , 142, 142, 168, 255)                                                            .setMcfg( 0, CaCO3          , 2*U, MilkyQuartz      , 1*U, Clay             , 1*U)                        .qual(1, 2.0, 16, 0).setGenerifying(Stone),
		Redrock      = stonecent( 8509, "Redrock"                 , 255,  80,  50, 255, "RedRock")                                                 .setMcfg( 0, CaCO3          , 2*U, Flint            , 1*U, ClayRed          , 1*U)                        .qual(1, 2.5, 16, 1).setGenerifying(Stone),
		Komatiite    = stonecent( 9177, "Komatiite"               , 190, 190, 105, 255, UNBURNABLE)                                                .setMcfg( 0, Peridot        , 1*U, MgCO3            , 2*U, Flint            , 6*U, DarkAsh          , 3*U).qual(1, 3.0, 32, 2).setGenerifying(Stone).heat(1673),
		Pumice       = stonecent( 9000, "Pumice"      , SET_DULL  , 220, 216, 127, 255, UNBURNABLE)                                                .setMcfg( 0, Peridot        , 3*U, MgCO3            , 2*U, Flint            , 4*U, DarkAsh          , 2*U).qual(1, 3.0, 32, 2).setGenerifying(Stone).heat(1673),
		Gabbro       = stonecent( 9176, "Gabbro"                  ,  65,  60,  60, 255, UNBURNABLE)                                                .setMcfg( 0, Peridot        , 1*U, CaCO3            , 3*U, Flint            , 8*U, DarkAsh          , 4*U).qual(1, 3.0, 32, 2).setGenerifying(Stone).heat(1673),
		Basalt       = stonecent( 8505, "Basalt"                  ,  60,  50,  50, 255, UNBURNABLE, UNRECYCLABLE)                                  .setMcfg( 0, Peridot        , 1*U, CaCO3            , 3*U, Flint            , 8*U, DarkAsh          , 4*U).qual(1, 3.0, 32, 2).setGenerifying(Stone).heat(1673),
		Marble       = stonecent( 8506, "Marble"                  , 200, 200, 200, 255)                                                            .setMcfg( 0, Mg             , 1*U, CaCO3            , 7*U)                                                .qual(1, 2.5, 16, 1).setGenerifying(Stone).setSmelting(CaCO3, 2*U3),
		Limestone    = stonecent( 9189, "Limestone"               , 230, 200, 130, 255, BETWEENLANDS)                                              .setMcfg( 0, CaCO3          , 1*U)                                                                        .qual(1, 2.5, 16, 1).setGenerifying(Stone).setSmelting(CaCO3, U2),
		Greenschist  = stone    ( 9171, "Greenschist"             , 105, 190, 105, 255, MD.UB)                                                                                                                                                               .qual(1, 2.0, 24, 1).setGenerifying(Stone).setLocal("Green Schist"),
		Blueschist   = stone    ( 9184, "Blueschist"              , 105, 105, 190, 255, MD.UB)                                                                                                                                                               .qual(1, 2.0, 24, 1).setGenerifying(Stone).setLocal("Blue Schist"),
		Grayschist   = stone    ( 9244, "Grayschist"              , 145, 140, 145, 255, MD.EB)                                                                                                                                                               .qual(1, 2.0, 24, 1).setGenerifying(Stone).setLocal("Gray Schist"),
		Pinkschist   = stone    ( 9245, "Pinkschist"              , 220, 195, 195, 255, MD.PFAA)                                                                                                                                                             .qual(1, 2.0, 24, 1).setGenerifying(Stone).setLocal("Pink Schist"),
		Gneiss       = stone    ( 9170, "Gneiss"                  , 255, 201, 134, 255)                                                                                                                                                                      .qual(1, 2.0, 24, 1).setGenerifying(Stone),
		Kimberlite   = stone    ( 9218, "Kimberlite"              , 100,  70,  10, 255)                                                                                                                                                                      .qual(1, 2.0, 24, 2).setGenerifying(Stone),
		Quartzite    = stone    ( 9180, "Quartzite"   , SET_QUARTZ, 230, 205, 205, 255, G_QUARTZ_ORES, CRYSTALLISABLE, QUARTZ, BLACKLISTED_SMELTER)                                                                                                          .qual(1, 1.7, 32, 1).setGenerifying(Stone).setSmelting(SiO2, U),
		GraniteRed   = stoneelec( 8507, "GraniteRed"              , 160,  60,  70, 255)                                                            .setMcfg( 0, Biotite        , 1*U, PotassiumFeldspar, 1*U, Flint            , 1*U)                        .qual(1, 3.0, 64, 3).setGenerifying(Stone).heat(1500).setLocal("Red Granite"),
		GraniteBlack = stoneelec( 8508, "GraniteBlack"            ,  20,  20,  20, 255)                                                            .setMcfg( 0, Biotite        , 1*U, PotassiumFeldspar, 1*U, Flint            , 1*U)                        .qual(1, 3.0, 64, 3).setGenerifying(Stone).heat(1500).setLocal("Black Granite"),
		Granite      = stoneelec( 8518, "Granite"                 , 160, 120, 130, 255)                                                            .setMcfg( 0, Biotite        , 1*U, PotassiumFeldspar, 1*U, Flint            , 1*U)                        .qual(1, 3.0, 64, 1).setGenerifying(Stone).heat(1500),
		Andesite     = stone    ( 9188, "Andesite"                , 191, 191, 191, 255)                                                                                                                                                                      .qual(1, 2.5, 16, 1).setGenerifying(Stone),
		Diorite      = stone    ( 8511, "Diorite"                 , 240, 240, 240, 255, UNBURNABLE)                                                                                                                                                          .qual(1, 2.5, 16, 1).setGenerifying(Stone),
		Blackstone   = brick    ( 9223, "Blackstone"              ,  30,  20,  20, 255, UNRECYCLABLE)                                                                                                                                                        .qual(1, 5.0, 64, 1).setGenerifying(Stone),
		Greywacke    = stone    ( 9173, "Greywacke"               , 176, 176, 176, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 1).setGenerifying(Stone),
		Siltstone    = stone    ( 9178, "Siltstone"               , 250, 205, 205, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 0).setGenerifying(Stone),
		Rhyolite     = stone    ( 9179, "Rhyolite"                , 121, 121, 121, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 1).setGenerifying(Stone),
		Migmatite    = stone    ( 9181, "Migmatite"               ,  70,  40,  40, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 1).setGenerifying(Stone),
		Chert        = stone    ( 9186, "Chert"                   , 105,  10,  10, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 0).setGenerifying(Stone),
		Dacite       = stone    ( 9187, "Dacite"                  , 131, 131, 131, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 1).setGenerifying(Stone),
		Slate        = stone    ( 9222, "Slate"                   , 148, 151, 156, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 0).setGenerifying(Stone),
		Deepslate    = stone    ( 9248, "Deepslate"               ,  57,  59,  61, 255)                                                                                                                                                                      .qual(1, 2.0, 32, 1).setGenerifying(Stone),
		Eclogite     = stone    ( 9191, "Eclogite"                ,  90,  40,  40, 255)                                                                                                                                                                      .qual(1, 2.0, 16, 1).setGenerifying(Stone),
		PhobosRock   = stone    ( 9249, "PhobosRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Phobos"   ),
		DeimosRock   = stone    ( 9250, "DeimosRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Deimos"   ),
		VenusRock    = stone    ( 9251, "VenusRock"               , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Venus"    ),
		MercuryRock  = stone    ( 9252, "MercuryRock"             , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Mercury"  ),
		CeresRock    = stone    ( 9253, "CeresRock"               , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Ceres"    ),
		JupiterRock  = stone    ( 9254, "JupiterRock"             , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Jupiter"  ),
		IoRock       = stone    ( 9255, "IoRock"                  , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Io"       ),
		EuropaRock   = stone    ( 9256, "EuropaRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Europa"   ),
		GanymedeRock = stone    ( 9257, "GanymedeRock"            , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Ganymede" ),
		CallistoRock = stone    ( 9258, "CallistoRock"            , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Callisto" ),
		SaturnRock   = stone    ( 9259, "SaturnRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Saturn"   ),
		RheaRock     = stone    ( 9260, "RheaRock"                , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Rhea"     ),
		TitanRock    = stone    ( 9261, "TitanRock"               , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Titan"    ),
		OberonRock   = stone    ( 9262, "OberonRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Oberon"   ),
		IapetusRock  = stone    ( 9263, "IapetusRock"             , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Iapetus"  ),
		UranusRock   = stone    ( 9264, "UranusRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Uranus"   ),
		TitaniaRock  = stone    ( 9265, "TitaniaRock"             , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Titania"  ),
		NeptuneRock  = stone    ( 9266, "NeptuneRock"             , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Neptune"  ),
		TritonRock   = stone    ( 9267, "TritonRock"              , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Triton"   ),
		PlutoRock    = stone    ( 9268, "PlutoRock"               , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Pluto"    ),
		ErisRock     = stone    ( 9269, "ErisRock"                , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Eris"     ),
		Kepler22bRock= stone    ( 9270, "Kepler22bRock"           , 189, 189, 189, 255)                                                                                                                                                                      .qual(1, 5.0,128, 1).setGenerifying(Stone).setLocal("Kepler22b");
	}
	
	/** Taking over Wood Materials from QwerTech, with major changes because damn that shit was broken. */
	public static class WOODS {
		@SuppressWarnings("hiding")
		public static final OreDictMaterial
		Oak                 = woodnormal( 9300, "Oak"                     , "Oak"                 , 180, 144,  90, 3.0, 32, MD.MC),
		Birch               = woodnormal( 9301, "Birch"                   , "Birch"               , 215, 204, 142, 2.5, 24, MD.MC),
		Spruce              = woodnormal( 9302, "Spruce"                  , "Spruce"              , 102,  79,  47, 3.0, 24, MD.MC),
		Jungle              = woodnormal( 9303, "Junglewood"              , "Junglewood"          , 177, 128,  92, 2.0, 16, MD.MC),
		Acacia              = woodnormal( 9304, "Acacia"                  , "Acacia"              , 186, 104,  59, 2.5, 24, MD.MC),
		DarkOak             = woodnormal( 9305, "DarkOak"                 , "Dark Oak"            ,  70,  45,  21, 3.5, 32, MD.MC),
		Crimson             = woodnormal( 9306, "Crimsonwood"             , "Crimsonwood"         , 180,  90, 106, 2.5, 24, MD.NeLi, UNBURNABLE),
		Warped              = woodnormal( 9307, "Warpedwood"              , "Warped Wood"         ,  42, 141, 133, 3.5, 32, MD.NeLi, UNBURNABLE),
		Foxfire             = woodnormal( 9408, "Foxfirewood"             , "Foxfire"             ,  51,  51, 101, 4.0, 24, MD.NeLi, UNBURNABLE),
		
		Compressed          = woodnormal( 9308, "WoodCompressed"          , "Compressed Wood"     ,  94,  60,  25, 1.5,  8, MD.GT).setPulver(Wood, U),
		Dead                = woodnormal( 9309, "WoodDead"                , "Dead Wood"           , 116, 108,  63, 1.5,  8, MD.BoP),
		Rotten              = woodnormal( 9310, "WoodRotten"              , "Rotten Wood"         ,  22,  44,  15, 1.0,  8, MD.GT),
		Mossy               = woodnormal( 9311, "WoodMossy"               , "Mossy Wood"          ,  29, 127,   0, 1.5,  8, MD.GT),
		Frozen              = woodnormal( 9312, "WoodFrozen"              , "Frozen Wood"         ,  84, 125, 125, 1.0,  8, MD.GT),
		Scorched            = woodnormal( 9404, "WoodScorched"            , "Scorched Wood"       ,  44,  44,  44, 1.0,  8, MD.ERE),
		Varnished           = woodnormal( 9405, "WoodVarnished"           , "Varnished Wood"      ,  73,  48,  16, 3.0, 32, MD.ERE),
		Bleached            = woodnormal( 9407, "WoodBleached"            , "Bleached Wood"       , 255, 255, 255, 2.0, 16, MD.ERE),
		Tainted             = woodnormal( 9406, "WoodTainted"             , "Tainted Wood"        ,  90,  23, 231, 1.0, 64, MD.TCFM, MAGICAL),
		
		Maple               = woodnormal( 9313, "Maple"                   , "Maple"               , 151,  26,  26, 3.0, 24, MD.FR),
		Willow              = woodnormal( 9314, "Willow"                  , "Willow"              ,  37, 150,   0, 2.0, 16, MD.FR),
		BlueMahoe           = woodnormal( 9315, "BlueMahoe"               , "Blue Mahoe"          ,  15, 103, 254, 3.0, 24, MD.FR),
		Hazel               = woodnormal( 9316, "Hazel"                   , "Hazel"               , 228, 175, 175, 2.5, 16, MD.BINNIE),
		Cinnamon            = woodnormal( 9317, "Cinnamonwood"            , "Cinnawood"           ,  65, 192, 192, 1.5, 16, MD.HaC),
		Coconut             = woodnormal( 9318, "Coconutwood"             , "Coconut"             , 255, 170,   0, 3.0, 16, MD.TROPIC),
		Rainbowood          = woodnormal( 9319, "Rainbowood"              , "Rainbowood"          , 200,  64, 245, 4.0, 64, MD.GT, MAGICAL, UNBURNABLE),
		BlueSpruce          = woodnormal( 9409, "BlueSpruce"              , "Blue Spruce"         , 213, 213, 217, 3.0, 24, MD.GT),
		
		Towerwood           = woodnormal( 9320, "Towerwood"               , "Towerwood"           , 166, 101,  58, 4.0, 64, MD.TF),
		Witchwood           = woodnormal( 9321, "Witchwood"               , "Witchwood"           , 118, 112, 142, 3.5, 48, MD.ARS, MAGICAL),
		Ogre                = woodnormal( 9322, "Ogrewood"                , "Ogrewood"            , 180,  90, 106, 4.0, 48, MD.MoCr),
		Wyvern              = woodnormal( 9323, "Wyvernwood"              , "Wyvernwood"          ,  77, 159, 158, 4.0, 48, MD.MoCr),
		Aspen               = woodnormal( 9324, "Aspen"                   , "Aspen"               ,  68,  65,  50, 2.0, 24, MD.TFC),
		DouglasFir          = woodnormal( 9325, "DouglasFir"              , "Douglas Fir"         , 249, 197, 154, 2.5, 24, MD.TFC),
		Sycamore            = woodnormal( 9326, "Sycamore"                , "Sycamore"            , 214, 155,  69, 3.0, 16, MD.TFC),
		WhiteCedar          = woodnormal( 9327, "WhiteCedar"              , "White Cedar"         , 219, 219, 205, 2.5, 24, MD.TFC),
		WhiteElm            = woodnormal( 9328, "WhiteElm"                , "White Elm"           , 162, 167, 103, 2.0, 32, MD.TFC),
		Thorntree           = woodnormal( 9329, "Thorntree"               , "Thorntree"           , 180, 144,  90, 3.0, 32, MD.EB),
		SilverPine          = woodnormal( 9330, "SilverPine"              , "Silver Pine"         ,  32,   7,  70, 3.0, 32, MD.EB),
		Alder               = woodnormal( 9331, "Alder"                   , "Alder"               , 177,  95,  87, 2.5, 32, MD.WTCH),
		Hawthorn            = woodnormal( 9332, "Hawthorn"                , "Hawthorn"            , 188, 182, 178, 3.0, 24, MD.WTCH),
		Rowan               = woodnormal( 9333, "Rowan"                   , "Rowan"               , 205, 172,  87, 3.5, 24, MD.WTCH),
		Mahogany            = woodnormal( 9356, "Mahogany"                , "Mahogany"            , 111,  61,  55, 4.0, 48, MD.TROPIC),
		Palm                = woodnormal( 9358, "Palm"                    , "Palm"                , 201, 124,  69, 3.0, 16, MD.TROPIC),
		
		Autumn              = woodnormal( 9334, "Autumnwood"              , "Autumn Wood"         , 191,  64,  35, 2.5, 24, MD.EBXL),
		Cypress             = woodnormal( 9336, "Cypress"                 , "Cypress"             , 185, 187, 181, 2.5, 16, MD.EBXL),
		Fir                 = woodnormal( 9337, "Fir"                     , "Fir"                 , 110, 106,  63, 2.0, 32, MD.EBXL),
		JapaneseMaple       = woodnormal( 9338, "JapaneseMaple"           , "Japanese Maple"      , 152,  76,  86, 3.0, 24, MD.EBXL),
		RainbowEucalyptus   = woodnormal( 9339, "RainbowEucalyptus"       , "Rainbow Eucalyptus"  , 116, 141, 198, 3.0, 32, MD.EBXL),
		Redwood             = woodnormal( 9340, "Redwood"                 , "Redwood"             , 163, 115,  70, 3.5, 24, MD.EBXL),
		Sakura              = woodnormal( 9341, "Sakura"                  , "Sakura"              , 250, 161, 122, 3.0, 32, MD.EBXL),
		
		Balsa               = woodnormal( 9342, "Balsa"                   , "Balsa"               , 165, 158, 151, 2.0, 16, MD.FR),
		Baobab              = woodnormal( 9343, "Baobab"                  , "Baobab"              , 136, 145,  95, 2.0, 16, MD.FR),
		Cherry              = woodnormal( 9344, "Cherrywood"              , "Cherrywood"          , 173, 124,  50, 2.5, 24, MD.FR),
		Chestnut            = woodnormal( 9345, "Chestnutwood"            , "Chestnutwood"        , 179, 162,  85, 3.0, 32, MD.FR),
		Citrus              = woodnormal( 9346, "Citruswood"              , "Citruswood"          , 152, 163,  28, 2.5, 24, MD.FR),
		Cocobolo            = woodnormal( 9347, "Cocobolowood"            , "Cocobolowood"        , 121,  18,   2, 3.0, 16, MD.FR),
		Ebony               = woodnormal( 9348, "Ebony"                   , "Ebony"               ,  58,  52,  46, 4.0, 48, MD.FR),
		Giganteum           = woodnormal( 9349, "Giganteumwood"           , "Giganteumwood"       , 102,  47,  39, 2.0, 16, MD.FR),
		Greenheart          = woodnormal( 9350, "Greenheart"              , "Greenheart"          ,  76, 118,  88, 2.5, 16, MD.FR),
		Ipe                 = woodnormal( 9351, "Ipe"                     , "Ipe"                 , 101,  58,  39, 2.0, 24, MD.FR),
		Kapok               = woodnormal( 9352, "Kapok"                   , "Kapok"               , 116, 108,  52, 2.0, 24, MD.FR),
		Larch               = woodnormal( 9353, "Larch"                   , "Larch"               , 215, 151, 133, 2.5, 16, MD.FR),
		Lime                = woodnormal( 9354, "Limewood"                , "Limewood"            , 206, 154, 104, 2.5, 24, MD.FR),
		Mahoe               = woodnormal( 9355, "Mahoe"                   , "Mahoe"               , 121, 147, 166, 3.0, 24, MD.FR),
		Padauk              = woodnormal( 9357, "Padauk"                  , "Padauk"              , 179,  99,  59, 2.0, 24, MD.FR, "Paduak"),
		Papaya              = woodnormal( 9359, "Papayawood"              , "Papayawood"          , 218, 200, 109, 3.0, 16, MD.FR),
		Plum                = woodnormal( 9360, "Plumwood"                , "Plumwood"            , 171,  99, 123, 2.5, 16, MD.FR),
		Poplar              = woodnormal( 9361, "Poplar"                  , "Poplar"              , 204, 204, 123, 2.5, 24, MD.FR),
		Sequoia             = woodnormal( 9362, "Sequoia"                 , "Sequoia"             , 142,  87,  84, 2.0, 24, MD.FR),
		Teak                = woodnormal( 9363, "Teak"                    , "Teak"                , 123, 115,  95, 3.0, 16, MD.FR),
		Walnut              = woodnormal( 9364, "Walnutwood"              , "Walnutwood"          ,  98,  78,  64, 3.0, 32, MD.FR),
		Wenge               = woodnormal( 9365, "Wenge"                   , "Wenge"               ,  88,  81,  70, 2.5, 16, MD.FR),
		Zebrawood           = woodnormal( 9366, "Zebrawood"               , "Zebrawood"           , 172, 139,  86, 2.0, 24, MD.FR),
		Pine                = woodnormal( 9335, "Pine"                    , "Pine"                , 187, 151,  77, 3.0, 32, MD.FR),
		
		Darkwood            = woodnormal( 9367, "Darkwood"                , "Darkwood"            ,  51,  45,  54, 2.5, 32, MD.BoP),
		Ethereal            = woodnormal( 9368, "Etherealwood"            , "Etherealwood"        ,  76, 150, 115, 3.0, 24, MD.BoP),
		Gold                = woodnormal( 9369, "Goldwood"                , "Goldwood"            , 210, 187, 151, 2.5, 24, MD.BoP),
		HellBark            = woodnormal( 9370, "Hellbark"                , "Hellbark"            , 200, 150, 100, 4.0, 16, MD.BoP),
		Jacaranda           = woodnormal( 9371, "Jacaranda"               , "Jacaranda"           , 201, 171, 162, 2.5, 16, MD.BoP),
		Mangrove            = woodnormal( 9372, "Mangrove"                , "Mangrove"            , 236, 228, 217, 2.0, 24, MD.BoP),
		SacredOak           = woodnormal( 9373, "SacredOak"               , "Sacred Oak"          , 159, 132,  77, 4.0, 48, MD.BoP),
		Magic               = woodnormal( 9374, "Magicwood"               , "Magicwood"           ,  90, 105, 180, 3.5, 32, MD.BoP, MAGICAL),
		
		Apple               = woodnormal( 9375, "Applewood"               , "Applewood"           ,  97,  49,  36, 2.0, 24, MD.BINNIE_TREE),
		Ash                 = woodnormal( 9376, "Ashwood"                 , "Ashwood"             , 244, 190,  90, 3.5, 16, MD.BINNIE_TREE),
		Beech               = woodnormal( 9377, "Beech"                   , "Beech"               , 226, 144,  68, 2.0, 32, MD.BINNIE_TREE),
		Box                 = woodnormal( 9378, "Boxwood"                 , "Boxwood"             , 253, 237, 192, 2.0, 24, MD.BINNIE_TREE),
		Brazilwood          = woodnormal( 9379, "Brazilwood"              , "Brazilwood"          , 112,  55,  84, 3.0, 16, MD.BINNIE_TREE),
		Butternut           = woodnormal( 9380, "Butternutwood"           , "Butternutwood"       , 237, 163, 112, 2.5, 16, MD.BINNIE_TREE),
		Cedar               = woodnormal( 9381, "Cedar"                   , "Cedar"               , 217,  88,  37, 2.0, 24, MD.BINNIE_TREE),
		Elder               = woodnormal( 9382, "Elderwood"               , "Elderwood"           , 189, 141, 115, 2.5, 16, MD.BINNIE_TREE),
		Elm                 = woodnormal( 9383, "Elm"                     , "Elm"                 , 243, 163,  90, 3.0, 24, MD.BINNIE_TREE),
		Eucalyptus          = woodnormal( 9384, "Eucalyptus"              , "Eucalyptus"          , 245, 164, 130, 2.5, 24, MD.BINNIE_TREE),
		Fig                 = woodnormal( 9385, "Figwood"                 , "Figwood"             , 202, 126,  27, 2.0, 16, MD.BINNIE_TREE),
		Gingko              = woodnormal( 9386, "Gingko"                  , "Gingko"              , 243, 226, 173, 2.0, 24, MD.BINNIE_TREE),
		Hemlock             = woodnormal( 9387, "Hemlock"                 , "Hemlock"             , 196, 174,  96, 3.0, 16, MD.BINNIE_TREE),
		Hickory             = woodnormal( 9388, "Hickory"                 , "Hickory"             , 218, 174, 134, 2.5, 16, MD.BINNIE_TREE),
		Holly               = woodnormal( 9389, "Holly"                   , "Holly"               , 248, 242, 226, 2.0, 24, MD.BINNIE_TREE),
		Hornbeam            = woodnormal( 9390, "Hornbeam"                , "Hornbeam"            , 195, 147,  87, 2.0, 16, MD.BINNIE_TREE),
		Iroko               = woodnormal( 9391, "Iroko"                   , "Iroko"               , 117,  47,   0, 3.0, 24, MD.BINNIE_TREE),
		Locust              = woodnormal( 9392, "Locust"                  , "Locust"              , 195, 140,  87, 2.0, 24, MD.BINNIE_TREE),
		Logwood             = woodnormal( 9393, "Logwood"                 , "Logwood"             , 166,  44,  34, 2.5, 24, MD.BINNIE_TREE),
		Maclura             = woodnormal( 9394, "Maclura"                 , "Maclura"             , 242, 168,  29, 2.0, 32, MD.BINNIE_TREE),
		Olive               = woodnormal( 9395, "Olivewood"               , "Olivewood"           , 174, 169, 129, 3.0, 16, MD.BINNIE_TREE),
		Pear                = woodnormal( 9396, "Pearwood"                , "Pearwood"            , 180, 127,  97, 2.5, 24, MD.BINNIE_TREE),
		PinkIvory           = woodnormal( 9397, "PinkIvory"               , "Pink Ivory"          , 234, 125, 148, 2.5, 24, MD.BINNIE_TREE),
		Purpleheart         = woodnormal( 9398, "Purpleheart"             , "Purpleheart"         ,  91,  22,  45, 2.0, 16, MD.BINNIE_TREE),
		Rosewood            = woodnormal( 9399, "Rosewood"                , "Rosewood"            , 128,  12,   0, 3.0, 16, MD.BINNIE_TREE),
		Sweetgum            = woodnormal( 9400, "Sweetgum"                , "Sweetgum"            , 215, 140,  74, 2.5, 16, MD.BINNIE_TREE),
		Syzgium             = woodnormal( 9401, "Syzgium"                 , "Syzgium"             , 221, 184, 183, 2.5, 24, MD.BINNIE_TREE),
		Whitebeam           = woodnormal( 9402, "Whitebeam"               , "Whitebeam"           , 192, 183, 174, 3.0, 16, MD.BINNIE_TREE),
		Yew                 = woodnormal( 9403, "Yew"                     , "Yew"                 , 226, 160, 114, 2.5, 32, MD.BINNIE_TREE);
	}
	
	/** The "I don't care" Section, everything I don't want to do anything with right now. Just to make the Material Finder shut up about them. But I do see potential uses in some of these Materials. */
	public static class UNUSED {
		public static final OreDictMaterial
		OsmiumTetroxide             = unused    ("Osmium Tetroxide"           ).setMcfg( 0, Os, 1*U, O, 4*U),
		SodiumPeroxide              = unused    ("Sodium Peroxide"            ).setMcfg( 0, Na, 2*U, O, 2*U), // Yellowish
		IridiumSodiumOxide          = unused    ("Iridium Sodium Oxide"       ),
		Iridiron                    = setPriorityPrefix(unused    ("IridiumIron"                ), 3).put(G_INGOT).setMcfg( 0, Ir, 1*U, Fe, 1*U).setLocal("Iridiron"),
		IridironReinforced          = setPriorityPrefix(unused    ("IridiumIronReinforced"      ), 3).put(G_INGOT).setMcfg( 0, Ir, 1*U, Fe, 1*U).setLocal("Reinforced Iridiron"),
		LimePure                    = unused    ("LimePure"                   ).setLocal("Pure Lime"),
		TNT                         = unused    ("TNT"                        ).setOriginalMod(MD.MC.mID).put(EXPLOSIVE,  FLAMMABLE),
		TerrasteelAlloyRaw          = setPriorityPrefix(unused    ("TerrasteelAlloyRaw"         ), 3).put(G_INGOT, MAGICAL, "RawTerrasteelAlloy").setLocal("Raw Terrasteel Alloy"),
		TerrasteelAlloyStrengthened = setPriorityPrefix(unused    ("TerrasteelAlloyStrengthened"), 3).put(G_INGOT, MAGICAL, "StrengthenedTerrasteelAlloy").setLocal("Strengthened Terrasteel Alloy"),
		Vis                         = unused    ("Vis"                        ).put(DECOMPOSABLE).setMcfg( 0, Ma, 1*U),
		Voidstone                   = unused    ("Voidstone"                  ),
		Mercassium                  = setPriorityPrefix(unused    ("Mercassium"                 ), 3).qual(3,  6.0,  64,  1).put(G_INGOT_ORES),
		Osmonium                    = setPriorityPrefix(unused    ("Osmonium"                   ), 3).qual(3,  6.0,  64,  1).put(G_INGOT_ORES),
		Phoenixite                  = setPriorityPrefix(unused    ("Phoenixite"                 ), 3).qual(3,  6.0,  64,  1).put(G_INGOT_ORES),
		Antimatter                  = unused    ("Antimatter"                 ).put(ANTIMATTER),
		Starconium                  = setPriorityPrefix(unused    ("Starconium"                 ), 3).put(G_INGOT_ORES),
		Thyrium                     = setPriorityPrefix(unused    ("Thyrium"                    ), 3).put(G_INGOT_ORES),
		Zectium                     = setPriorityPrefix(unused    ("Zectium"                    ), 3).put(G_INGOT_ORES),
		Draconic                    = setPriorityPrefix(deprecated("Draconic"                   ), 2).put(G_DUST),
		Teslatite                   = setPriorityPrefix(unused    ("InfusedTeslatite"           ), 2).put(G_DUST).setLocal("Teslatite"), // 1 Redstone + 1 Nikolite = 1 Teslatite; and 8 Teslatite + 1 Gold = 1 Purple Alloy;
		IrridantUranium             = setPriorityPrefix(unused    ("Irridant Uranium"           ), 3).put(G_INGOT),
		IrridantReinforced          = setPriorityPrefix(unused    ("IrridantReinforced"         ), 3).put(G_INGOT),
		IronSharp                   = setPriorityPrefix(unused    ("IronSharp"                  ), 3).put(G_INGOT).setLocal("Sharp Iron"),
		ObsidianFlux                = setPriorityPrefix(unused    ("Obsidian Flux"              ), 3).put(G_INGOT),
		CrystalFlux                 = setPriorityPrefix(unused    ("Crystal Flux"               ), 1).put(G_GEM, CRYSTAL, BRITTLE),
		Mimichite                   = setPriorityPrefix(unused    ("Mimichite"                  ), 1).put(G_GEM_ORES, CRYSTAL, BRITTLE),
		Infernal                    = unused    ("Infernal"                   ),
		Invisium                    = setPriorityPrefix(unused    ("Invisium"                   ), 2).put(G_DUST),
		Lodestone                   = setPriorityPrefix(unused    ("Lodestone"                  ), 2).put(G_DUST_ORES),
		Luminite                    = setPriorityPrefix(unused    ("Luminite"                   ), 2).put(G_DUST_ORES),
		Magma                       = unused    ("Magma"                      ),
		Mawsitsit                   = setPriorityPrefix(unused    ("Mawsitsit"                  ), 2).put(G_DUST),
		Nether                      = unused    ("Nether"                     ),
		Painite                     = unused    ("Painite"                    ),
		Petroleum                   = setPriorityPrefix(unused    ("Petroleum"                  ), 2).put(G_DUST_ORES),
		Pewter                      = unused    ("Pewter"                     ),
		Potash                      = unused    ("Potash"                     ),
		Randomite                   = setPriorityPrefix(unused    ("Randomite"                  ), 2).put(G_DUST_ORES),
		RyuDragonRyder              = unused    ("RyuDragonRyder"             ),
		Tar                         = unused    ("Tar"                        ),
		TarPitch                    = unused    ("Tar Pitch"                  ),
		Cavenium                    = unused    ("Cavenium"                   ),
		CaveniumRefined             = unused    ("CaveniumRefined"            ).put("RefinedCavenium").setLocal("Refined Cavenium"),
		Infitite                    = unused    ("Infitite"                   ),
		Magnite                     = unused    ("Magnite"                    ),
		Hexcite                     = unused    ("Hexcite"                    ),
		Tapazite                    = setPriorityPrefix(unused    ("Tapazite"                   ), 2).put(G_DUST),
		Tourmaline                  = setPriorityPrefix(unused    ("Tourmaline"                 ), 2).put(G_DUST),
		Turquoise                   = setPriorityPrefix(unused    ("Turquoise"                  ), 2).put(G_DUST),
		Wimalite                    = setPriorityPrefix(unused    ("Wimalite"                   ), 2).put(G_DUST_ORES),
		Adamite                     = setPriorityPrefix(unused    ("Adamite"                    ), 2).put(G_DUST_ORES),
		Adluorite                   = setPriorityPrefix(unused    ("Adluorite"                  ), 2).put(G_DUST_ORES),
		Agate                       = setPriorityPrefix(unused    ("Agate"                      ), 2).put(G_DUST),
		Ammonium                    = setPriorityPrefix(unused    ("Ammonium"                   ), 2).put(G_DUST),
		Bitumen                     = setPriorityPrefix(unused    ("Bitumen"                    ), 2).put(G_DUST_ORES),
		Bloodstone                  = setPriorityPrefix(unused    ("Bloodstone"                 ), 2).put(G_DUST),
		Citrine                     = setPriorityPrefix(unused    ("Citrine"                    ), 2).put(G_DUST),
		Coral                       = setPriorityPrefix(unused    ("Coral"                      ), 2).put(G_DUST),
		Chrysocolla                 = setPriorityPrefix(unused    ("Chrysocolla"                ), 2).put(G_DUST),
		DarkStone                   = setPriorityPrefix(unused    ("Dark Stone"                 ), 2).put(G_DUST),
		Demonite                    = setPriorityPrefix(unused    ("Demonite"                   ), 2).put(G_DUST),
		InfusedGold                 = setPriorityPrefix(unused    ("Infused Gold"               ), 3).put(G_INGOT),
		Daffergon                   = setPriorityPrefix(unused    ("Daffergon"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES), // Dellite Ore
		Reiium                      = setPriorityPrefix(unused    ("Reiium"                     ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES), // Reiite Ore
		Weidanium                   = setPriorityPrefix(unused    ("Weidanium"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES), // Weidite Ore
		Verticium                   = setPriorityPrefix(unused    ("Verticium"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES),
		Australium                  = setPriorityPrefix(unused    ("Australium"                 ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES),
		Schrabidium                 = setRGBa(setPriorityPrefix(unused    ("Schrabidium"                ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_ORES,  MAGNETIC_ACTIVE,  AUTO_COLLECTING,  MELTING,  MOLTEN),  50, 255, 255, 255),
		Starmetal                   = setPriorityPrefix(unused    ("Starmetal"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE_ORES),
		Unobtainium                 = setPriorityPrefix(unused    ("Unobtainium"                ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE_ORES),
		CMBSteel                    = setPriorityPrefix(unused    ("CMB Steel"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE),
		DuraSteel                   = setPriorityPrefix(unused    ("DuraSteel"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE).setLocal("High-Speed Steel"),
		AdvancedAlloy               = setPriorityPrefix(unused    ("Advanced Alloy"             ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE),
		Saturnite                   = setPriorityPrefix(unused    ("Saturnite"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE),
		Dineutronium                = setPriorityPrefix(unused    ("Dineutronium"               ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT_MACHINE),
		MagnetizedTungsten          = setPriorityPrefix(unused    ("Magnetized Tungsten"        ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT,  MAGNETIC_ACTIVE,  AUTO_COLLECTING),
		Euphemium                   = setRGBa(setPriorityPrefix(unused    ("Euphemium"                  ), 3).setOriginalMod(MD.HBM.mID).put(G_INGOT,  MELTING,  MOLTEN), 255, 150, 255, 255),
		Rupee                       = unused    ("Rupee"                      ).setOriginalMod(MD.DRPG.mID),
		Arlemite                    = unused    ("Arlemite"                   ).setOriginalMod(MD.DRPG.mID),
		Realmite                    = unused    ("Realmite"                   ).setOriginalMod(MD.DRPG.mID),
		Bloodgem                    = unused    ("Bloodgem"                   ).setOriginalMod(MD.DRPG.mID),
		Netheryte                   = unused    ("Netheryte"                  ).setOriginalMod(MD.DRPG.mID),
		Eden                        = unused    ("Eden"                       ).setOriginalMod(MD.DRPG.mID),
		Wildwood                    = unused    ("Wildwood"                   ).setOriginalMod(MD.DRPG.mID),
		Apalachia                   = unused    ("Apalachia"                  ).setOriginalMod(MD.DRPG.mID),
		Skythern                    = unused    ("Skythern"                   ).setOriginalMod(MD.DRPG.mID),
		Mortum                      = unused    ("Mortum"                     ).setOriginalMod(MD.DRPG.mID),
		Arcanium                    = unused    ("Arcanium"                   ).setOriginalMod(MD.DRPG.mID),
		Energized                   = unused    ("Energized"                  ),
		Reinforced                  = unused    ("Reinforced"                 ),
		Mud                         = unused    ("Mud"                        ).put(IGNORE_IN_COLOR_LOG),
		Cream                       = unused    ("Cream"                      ).put(IGNORE_IN_COLOR_LOG),
		Cluster                     = unused    ("Cluster"                    ),
		Sweet                       = unused    ("Sweet"                      ),
		Gelatine                    = unused    ("Gelatine"                   ),
		Satinspar                   = unused    ("Satinspar"                  ),
		Selenite                    = unused    ("Selenite"                   ),
		Jet                         = unused    ("Jet"                        ),
		Microcline                  = unused    ("Microcline"                 ),
		Serpentine                  = unused    ("Serpentine"                 ),// byproduct pietersite, which is a fake? Tiger eye
		Sylvite                     = unused    ("Sylvite"                    ),
		Goshen                      = unused    ("Goshen"                     ),
		Joshen                      = unused    ("Joshen"                     ),
		Itarius                     = unused    ("Itarius"                    ),
		Legendary                   = unused    ("Legendary"                  ),
		MutatedIron                 = unused    ("Mutated Iron"               ),
		Witheria                    = unused    ("Witheria"                   ),
		RubberTreeSap               = unused    ("Rubber Tree Sap"            ),
		GraveyardDirt               = unused    ("Graveyard Dirt"             ),
		Cocaine                     = unused    ("Cocaine"                    ),
		Vile                        = unused    ("Vile"                       ),
		Dull                        = unused    ("Dull"                       ),
		Dark                        = unused    ("Dark"                       ),
		Soulium                     = unused    ("Soulium"                    ),
		Tennantite                  = unused    ("Tennantite"                 ),
		Alfium                      = unused    ("Alfium"                     ),
		Ryu                         = unused    ("Ryu"                        ),
		Mutation                    = unused    ("Mutation"                   ),
		HOPGraphite                 = unused    ("HOPGraphite"                ),
		EnrichedCopper              = unused    ("Enriched Copper"            ),
		DiamondCopper               = unused    ("Diamond Copper"             ),
		Fairy                       = unused    ("Fairy"                      ),
		Pokefennium                 = unused    ("Pokefennium"                );
	}
}