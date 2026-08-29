/**
 * Copyright (c) 2025 GregTech-6 Team
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

import gregapi.oredict.OreDictMaterial;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;
import static gregapi.data.TD.Processing.*;
import static gregapi.data.TD.Properties.*;

/**
 * @author Gregorius Techneticies
 *
 * PORT (card gt-material-dataset): verbatim structure. The 46 alias-group materials keep their
 * upstream static-final + initializer form (tiny class-init, no red-line concern). init()
 * (upstream :92-157) is ported with the two OreDictMaterial methods that did not survive the
 * Phase-1 model port replicated as MT-local static helpers: steal (upstream OreDictMaterial
 * :1101-1106) and stealLooks (:1108-1125); CS.ZL_MT / CS.TICKS_PER_SMELT moved to MT (CS is
 * outside this card's FILES_SCOPE).
 */
public class ANY {
	private static OreDictMaterial any(String aNameOreDict) {return OreDictMaterial.createMaterial(-1, aNameOreDict, aNameOreDict).put(UNUSED_MATERIAL, INVALID_MATERIAL, IGNORE_IN_COLOR_LOG);}
	private static boolean CREATED = F;

	/** Technical Materials, which are only there for Recipes and such. */
	public static OreDictMaterial
	Glowstone,
	Diamond,
	Sapphire,
	Emerald,
	Amethyst,
	Garnet,
	Jasper,
	TigerEye,
	Aventurine,
	Amber,
	Fluorite,
	CaF2,
	Phosphorus,
	Blaze,
	Prismarine,
	Grains,
	Flour,
	FlourGrains,
	Wax,
	Stone,
	Calcite,
	Clay,
	Salt,
	Fe,
	Iron,
	Steel,
	BlackSteel,
	BlueSteel,
	RedSteel,
	MagicIron,
	Cu,
	Ash,
	C,
	Coal,
	Si,
	SiO2,
	Quartz,
	Sand,
	W,
	ThaumCrystal,
	Hexorium,
	Wood,
	WoodDefault,
	WoodNormal,
	WoodMagical,
	WoodTreated,
	WoodUntreated,
	WoodPlastic,
	Rubber,
	Plastic,
	PlasticHard,
	_Steel,
	_Bronze,
	_Metal;

	/** Creation phase. Upstream builds this table as the class-init of ANY (upstream ANY.java:44-98),
	 *  fired at the first helper reference during the MT flood (e.g. the ported MT.java:315 create() and
	 *  :609 diamond() helpers). As a class-init it ran exactly once per JVM, so after a registry reset
	 *  the 53 entries vanished from MATERIAL_MAP and never came back (the card-3 size gap). The port
	 *  pins the creation to the top of MT.init() with the same identity-check guard style as
	 *  MT.java:2694 and AM.java:655: after reset() the map lookup misses and the batch re-runs. */
	protected static void create() {
		if (CREATED && OreDictMaterial.MATERIAL_MAP.get("AnyGlowstone") == Glowstone) return;
		CREATED = T;
		Glowstone = any("Any Glowstone"       );
		Diamond = any("Any Diamond"         );
		Sapphire = any("Any Sapphire"        );
		Emerald = any("Any Emerald"         );
		Amethyst = any("Any Amethyst"        );
		Garnet = any("Any Garnet"          );
		Jasper = any("Any Jasper"          );
		TigerEye = any("Any Tiger Eye"       );
		Aventurine = any("Any Aventurine"      );
		Amber = any("Any Amber"           );
		Fluorite = any("Any Fluorite"        );
		CaF2 = Fluorite;
		Phosphorus = any("Any Phosphorus"      );
		Blaze = any("Any Blaze"           );
		Prismarine = any("Any Prismarine"      );
		Grains = any("Any Grains"          );
		Flour = any("Any Flour"           );
		FlourGrains = any("Any Flour Or Grains" );
		Wax = any("Any Wax"             );
		Stone = any("Any Stone"           );
		Calcite = any("Any Calcite"         );
		Clay = any("Any Clay"            );
		Salt = any("Any Salt"            );
		Fe = any("Any Iron"            );
		Iron = any("Any Iron Or Steel"   );
		Steel = any("Any Iron-Steel"      );
		BlackSteel = any("Any Black Steel"     );
		BlueSteel = any("Any Blue Steel"      );
		RedSteel = any("Any Red Steel"       );
		MagicIron = any("Any Magic Iron"      );
		Cu = any("Any Copper"          );
		Ash = any("Any Ashes"           );
		C = any("Any Carbon"          );
		Coal = any("Any Coal/Carbon"     );
		Si = any("Any Silicon"         );
		SiO2 = any("Any Silicon Dioxide" );
		Quartz = any("Quartz"              );
		Sand = any("Any Sand"            );
		W = any("Any Tungsten"        );
		ThaumCrystal = any("Any Thaumic Crystal" );
		Hexorium = any("Hexorium"            );
		Wood = any("Any Wood"            );
		WoodDefault = any("Any Default Wood"    );
		WoodNormal = any("Any Normal Wood"     );
		WoodMagical = any("Any Magical Wood"    );
		WoodTreated = any("Any Treated Wood"    );
		WoodUntreated = any("Any Untreated Wood"  );
		WoodPlastic = any("Any Wood Or Plastic" );
		Rubber = any("Any Rubber"          );
		Plastic = any("Any Plastic"         );
		PlasticHard = any("Any Hard Plastic"    );
		_Steel = any("Any Steel"           );
		_Bronze = any("Any Bronze"          );
		_Metal = any("Any Metal"           );
	}


	/**
	 * Wiring phase (upstream ANY.java:92-157): binds the alias groups to the current MT generation
	 * via steal/stealLooks/addReRegistrationToThis and wires the mTargetReversing pairs. Called
	 * once per MT.init() flood after the reg batches, so it re-binds whenever a reset re-created
	 * the MT targets. No early-return guard on purpose: every operation here is an idempotent
	 * setter or a Set-add, so a re-run on the same generation is a no-op in effect; the
	 * reset-aware identity check lives in {@link #create()}, which owns the registration side
	 * (same split as MT.java:2694 / AM.java:655 guarding the registration).
	 */
	protected static void init() {
		MT.stealLooks(MT.steal(Glowstone, MT.Glowstone), MT.Glowstone).setLocal("Glowstone").setAllToTheOutputOf(MT.Glowstone).put(CRYSTAL, GLOWING, LIGHTING);
		MT.stealLooks(MT.steal(Diamond, MT.Diamond), MT.Diamond).setLocal("Diamond").setAllToTheOutputOf(MT.Diamond).put(CRYSTAL, VALUABLE).addReRegistrationToThis(MT.Diamantine);
		MT.stealLooks(MT.steal(Sapphire, MT.BlueSapphire), MT.Sapphire).setLocal("Sapphire").setAllToTheOutputOf(MT.Sapphire).put(CRYSTAL, VALUABLE);
		MT.stealLooks(MT.steal(Emerald, MT.Emerald), MT.Emerald).setLocal("Emerald").setAllToTheOutputOf(MT.Emerald).put(CRYSTAL, VALUABLE).addReRegistrationToThis(MT.Emeradic);
		MT.stealLooks(MT.steal(Amethyst, MT.Amethyst), MT.Amethyst).setLocal("Amethyst").setAllToTheOutputOf(MT.Amethyst).put(CRYSTAL, VALUABLE).addReRegistrationToThis(MT.Amethyst, MT.EnderAmethyst);
		MT.stealLooks(MT.steal(Garnet, MT.Spessartine), MT.Spessartine).setLocal("Garnet").put(CRYSTAL, VALUABLE);
		MT.stealLooks(MT.steal(Jasper, MT.Jasper), MT.Jasper).setLocal("Jasper").put(CRYSTAL, VALUABLE);
		MT.stealLooks(MT.steal(TigerEye, MT.TigerEyeYellow), MT.TigerEyeYellow).setLocal("Tiger Eye").put(CRYSTAL, VALUABLE);
		MT.stealLooks(MT.steal(Aventurine, MT.AventurineGreen), MT.AventurineGreen).setLocal("Aventurine").put(CRYSTAL, VALUABLE);
		MT.stealLooks(MT.steal(Amber, MT.Amber), MT.Amber).setLocal("Amber").put(CRYSTAL, VALUABLE).addReRegistrationToThis(MT.AmberGolden, MT.AmberDominican);
		MT.stealLooks(MT.steal(Fluorite, MT.CaF2), MT.CaF2).setLocal("Fluorite").setAllToTheOutputOf(MT.CaF2).put(CRYSTAL, MORTAR, MELTING, BRITTLE, ACID);
		MT.stealLooks(MT.steal(Phosphorus, MT.Phosphorus), MT.Phosphorus).setLocal("Phosphorus").setAllToTheOutputOf(MT.Phosphorus).put(CRYSTAL, MORTAR, MELTING, BRITTLE, FLAMMABLE, EXPLOSIVE).addReRegistrationToThis(MT.Phosphorus, MT.PhosphorusBlue, MT.PhosphorusRed, MT.PhosphorusWhite);
		MT.stealLooks(MT.steal(Blaze, MT.Blaze), MT.Blaze).setLocal("Blaze").put(GLOWING, MAGICAL, BRITTLE, MORTAR);
		MT.stealLooks(MT.steal(Prismarine, MT.PrismarineLight), MT.PrismarineLight).setLocal("Prismarine").setAllToTheOutputOf(MT.PrismarineLight).put(CRYSTAL).addReRegistrationToThis(MT.PrismarineLight, MT.PrismarineDark);
		MT.stealLooks(MT.steal(Grains, MT.Wheat), MT.Wheat).setLocal("Grains").setAllToTheOutputOf(MT.Wheat).put(FOOD, MORTAR, FLAMMABLE);
		MT.stealLooks(MT.steal(Flour, MT.Wheat), MT.Wheat).setLocal("Flour").setAllToTheOutputOf(MT.Wheat).put(FOOD, MORTAR, FLAMMABLE).addReRegistrationToThis(MT.Wheat, MT.Rye, MT.Oat, MT.OatAbyssal, MT.Barley, MT.Potato, MT.Corn);
		MT.stealLooks(MT.steal(FlourGrains, MT.Wheat), MT.Wheat).setLocal("Flour and Grains").setAllToTheOutputOf(MT.Wheat).put(FOOD, MORTAR, FLAMMABLE).addReRegistrationToThis(MT.Potato);
		MT.stealLooks(MT.steal(Wax, MT.Wax), MT.Wax).setLocal("Wax").setAllToTheOutputOf(MT.Wax).put();
		MT.stealLooks(MT.steal(Stone, MT.Stone), MT.Stone).setLocal("Stone").setAllToTheOutputOf(MT.Stone).put(STONE, BRITTLE, UNRECYCLABLE);
		MT.stealLooks(MT.steal(Calcite, MT.CaCO3), MT.CaCO3).setLocal("Calcite").setAllToTheOutputOf(MT.CaCO3).put(STONE, BRITTLE, UNRECYCLABLE).addReRegistrationToThis(MT.CaCO3, MT.STONES.Marble, MT.Chalk, MT.STONES.Limestone, MT.Dolomite);
		MT.stealLooks(MT.steal(Clay, MT.ClayBrown), MT.Clay).setLocal("Clay").setAllToTheOutputOf(MT.Clay).put(MORTAR).addReRegistrationToThis(MT.Clay);
		MT.stealLooks(MT.steal(Salt, MT.NaCl), MT.NaCl).setLocal("Salt").setAllToTheOutputOf(MT.NaCl).put(BRITTLE).addReRegistrationToThis(MT.NaCl, MT.KCl, MT.LiCl, MT.MgCl2, MT.CaCl2);
		MT.stealLooks(MT.steal(Fe, MT.Fe), MT.Fe).setLocal("Iron").setAllToTheOutputOf(MT.Fe).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.Fe, MT.WroughtIron, MT.IronCast, MT.IronCompressed, MT.PigIron, MT.MeteoricIron, MT.Meteorite, MT.Enori);
		MT.stealLooks(MT.steal(Iron, MT.Fe), MT.Fe).setLocal("Iron").setAllToTheOutputOf(MT.Fe).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.Fe, MT.WroughtIron, MT.IronCast, MT.IronCompressed, MT.PigIron, MT.MeteoricIron, MT.Meteorite, MT.Enori, MT.Steel, MT.Knightmetal, MT.MeteoricSteel);
		MT.stealLooks(MT.steal(Steel, MT.Steel), MT.Steel).setLocal("Steel").setAllToTheOutputOf(MT.Steel).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.Steel, MT.Knightmetal, MT.MeteoricSteel);
		MT.stealLooks(MT.steal(BlackSteel, MT.BlackSteel), MT.BlackSteel).setLocal("Black Steel").setAllToTheOutputOf(MT.BlackSteel).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.BlackSteel, MT.MeteoricBlackSteel, MT.MeteoflameBlackSteel);
		MT.stealLooks(MT.steal(BlueSteel, MT.BlueSteel), MT.BlueSteel).setLocal("Blue Steel").setAllToTheOutputOf(MT.BlueSteel).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.BlueSteel, MT.MeteoricBlueSteel, MT.MeteoflameBlueSteel);
		MT.stealLooks(MT.steal(RedSteel, MT.RedSteel), MT.RedSteel).setLocal("Red Steel").setAllToTheOutputOf(MT.RedSteel).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.RedSteel, MT.MeteoricRedSteel, MT.MeteoflameRedSteel);
		MT.stealLooks(MT.steal(MagicIron, MT.Manasteel), MT.Manasteel).setLocal("Magic Iron").setAllToTheOutputOf(MT.Fe).put(SMITHABLE, MELTING, MAGICAL).addReRegistrationToThis(MT.Manasteel, MT.Thaumium, MT.DarkThaumium, MT.SpectreIron, MT.FierySteel, MT.MeteoflameSteel);
		MT.stealLooks(MT.steal(Cu, MT.Cu), MT.Cu).setLocal("Copper").setAllToTheOutputOf(MT.Cu).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.Cu, MT.AnnealedCopper);
		MT.stealLooks(MT.steal(Ash, MT.Ash), MT.Ash).setLocal("Ashes").setAllToTheOutputOf(MT.Ash).put(BRITTLE).addReRegistrationToThis(MT.Ash, MT.DarkAsh, MT.VolcanicAsh);
		MT.stealLooks(MT.steal(C, MT.C), MT.C).setLocal("Carbon").setAllToTheOutputOf(MT.C).put().addReRegistrationToThis(MT.C, MT.Graphite, MT.Graphene);
		MT.stealLooks(MT.steal(Coal, MT.C), MT.C).setLocal("Carbon").setAllToTheOutputOf(MT.C).put().addReRegistrationToThis(MT.C, MT.Graphite, MT.Graphene, MT.CoalCoke, MT.Coal, MT.Charcoal);
		MT.stealLooks(MT.steal(Si, MT.Si), MT.Si).setLocal("Silicon").setAllToTheOutputOf(MT.Si).put(SMITHABLE, MELTING).addReRegistrationToThis(MT.Si);
		MT.stealLooks(MT.steal(SiO2, MT.SiO2), MT.SiO2).setLocal("Silicon Dioxide").setAllToTheOutputOf(MT.SiO2).put(BRITTLE, MELTING).addReRegistrationToThis(MT.STONES.Quartzite, MT.SiO2, MT.Sand, MT.RedSand, MT.EndSandWhite, MT.EndSandBlack, MT.Glass, MT.Flint);
		MT.stealLooks(MT.steal(Quartz, MT.MilkyQuartz), MT.MilkyQuartz).setLocal("Quartz").setAllToTheOutputOf(MT.SiO2).put(BRITTLE, MELTING, QUARTZ).addReRegistrationToThis(MT.STONES.Quartzite);
		MT.stealLooks(MT.steal(Sand, MT.Sand), MT.Sand).setLocal("Sand").setAllToTheOutputOf(MT.Sand).put(BRITTLE, MELTING).addReRegistrationToThis(MT.Sand, MT.RedSand);
		MT.stealLooks(MT.steal(W, MT.W), MT.W).setLocal("Tungsten").setAllToTheOutputOf(MT.W).put(SMITHABLE, MELTING, UNBURNABLE).addReRegistrationToThis(MT.W, MT.TungstenSintered);
		MT.steal(MT.stealLooks(ThaumCrystal, MT.InfusedBalance), MT.InfusedDull).setLocal("Any Thaumic Crystal").put(DONT_SHOW_THIS_COMPONENT); // upstream :134
		MT.stealLooks(Hexorium, MT.HexoriumWhite).setLocal("Hexorium").put(DONT_SHOW_THIS_COMPONENT); // upstream :135
		MT.stealLooks(MT.steal(WoodDefault, MT.WOODS.Spruce), MT.Wood).setLocal("Normal Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE).addReRegistrationToThis(MT.Wood, MT.Peanutwood).setFurnaceBurnTime(MT.TICKS_PER_SMELT/2);
		MT.stealLooks(MT.steal(WoodNormal, MT.WOODS.Spruce), MT.Wood).setLocal("Normal Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE).addReRegistrationToThis(WoodDefault.mToThis.toArray(MT.ZL_MT)).addReRegistrationToThis(MT.WoodRubber, MT.Weedwood, MT.Skyroot, MT.Bamboo).setFurnaceBurnTime(MT.TICKS_PER_SMELT/2);
		MT.stealLooks(MT.steal(WoodMagical, MT.Greatwood), MT.Wood).setLocal("Magical Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE, MAGICAL).addReRegistrationToThis(MT.Greatwood, MT.Silverwood, MT.Livingwood, MT.Dreamwood, MT.Shimmerwood, MT.WOODS.Magic, MT.WOODS.Tainted, MT.WOODS.Witchwood, MT.WOODS.Rainbowood).setFurnaceBurnTime(MT.TICKS_PER_SMELT*2L);
		MT.stealLooks(MT.steal(WoodTreated, MT.WoodTreated), MT.Wood).setLocal("Treated Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE).addReRegistrationToThis(MT.WoodTreated, MT.WoodPolished).setFurnaceBurnTime(MT.TICKS_PER_SMELT/2);
		MT.stealLooks(MT.steal(WoodUntreated, MT.WOODS.Spruce), MT.Wood).setLocal("Untreated Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE).addReRegistrationToThis(WoodMagical.mToThis.toArray(MT.ZL_MT)).addReRegistrationToThis(WoodNormal.mToThis.toArray(MT.ZL_MT)).setFurnaceBurnTime(MT.TICKS_PER_SMELT/2);
		MT.stealLooks(MT.steal(Wood, MT.WOODS.Spruce), MT.Wood).setLocal("Wood").setAllToTheOutputOf(MT.Wood).put(WOOD, FLAMMABLE).setFurnaceBurnTime(MT.TICKS_PER_SMELT/2);
		MT.stealLooks(MT.steal(PlasticHard, MT.Polycarbonate), MT.Polycarbonate).setLocal("Hard Plastic").setAllToTheOutputOf(MT.Plastic).put().addReRegistrationToThis(MT.Polycarbonate, MT.PVC);
		MT.stealLooks(MT.steal(Plastic, MT.Plastic), MT.Plastic).setLocal("Plastic").setAllToTheOutputOf(MT.Plastic).put().addReRegistrationToThis(MT.Polycarbonate, MT.PVC, MT.Teflon, MT.Bakelite, MT.Plastic);
		MT.stealLooks(MT.steal(Rubber, MT.Rubber), MT.Rubber).setLocal("Rubber").setAllToTheOutputOf(MT.Rubber).put().addReRegistrationToThis(MT.Rubber);
		MT.stealLooks(MT.steal(WoodPlastic, MT.WOODS.Spruce), MT.Wood).setLocal("Any Wood Or Plastic").put(DONT_SHOW_THIS_COMPONENT).addReRegistrationToThis(Plastic, MT.PetrifiedWood);

		MT.stealLooks(_Steel, MT.Steel).put(DONT_SHOW_THIS_COMPONENT);
		MT.stealLooks(_Bronze, MT.Bronze).put(DONT_SHOW_THIS_COMPONENT);
		MT.stealLooks(_Metal, MT.Fe).put(DONT_SHOW_THIS_COMPONENT);

		MT.W.mTargetReversing = ANY.W;
		MT.Cu.mTargetReversing = ANY.Cu;
		MT.Fe.mTargetReversing = ANY.Fe.mTargetReversing = ANY.Iron;
		MT.Steel.mTargetReversing = ANY.Steel;
		MT.Stone.mTargetReversing = ANY.Stone;
		MT.Wood.mTargetReversing = ANY.Wood;
	}
}
