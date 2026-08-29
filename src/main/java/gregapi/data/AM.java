/**
 * Ported from GregTech 6 (1.7.10), file gregapi/data/AM.java (upstream 485 lines),
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

import static gregapi.data.CS.U;

import gregapi.oredict.OreDictMaterial;

import static gregapi.data.TD.Atomic.*;
import static gregapi.data.TD.ItemGenerator.*;
import static gregapi.data.TD.Processing.*;
import static gregapi.data.TD.Properties.*;

/**
 * @author Gregorius Techneticies
 *
 * List of all Anti-Materials. The Short Name is for ease of overview and stands for "AntiMatter".
 *
 * Note: I wrote those shortcuts not only because of overview Reasons. I have hit the 65536 Limit of the static initialiser multiple times now. // upstream AM.java:36
 *
 * PORT (card gt-material-dataset): the upstream single giant clinit is split into
 * reg0000()..reg00NN() driven by {@link #init()} (MT.init calls it where upstream did
 * AM.Hydrogen.getClass(), MT.java:1896). MC-strip: put(ANTIMATTER, MD.GAPI) (:40) becomes
 * put(ANTIMATTER).setOriginalMod(MT.MD.GAPI.mID) (upstream put :1463-1466 dispatch);
 * .aspects(TC.PERDITIO, 1) and .visDefault() of :40 are ThaumCraft/visibility-manager
 * deferrals (Phase 2); TextureSet[] -> String[] name references (MT.SET_X); the
 * setRGBa/setRGBaLiquid/setTextures chains are folded into unqualified static calls that hit
 * the AM-local delegates below, which forward to the MT-local replications of the upstream
 * OreDictMaterial setters (:1024-1125).
 */
public class AM {
	/** Making the Table a little bit more overseeable */ // upstream AM.java:39
	static OreDictMaterial create       (int aID, String aNameOreDict) {return OreDictMaterial.createMaterial(aID, aNameOreDict, aNameOreDict).put(ANTIMATTER).setOriginalMod(MT.MD.GAPI.mID);} // upstream :40; aspects+visDefault deferred (Phase 2)
	static OreDictMaterial element      (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return MT.setTextures(MT.setRGBa(create(aID, aNameOreDict).setStats(aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter), aR, aG, aB, aA), aSets).put(ELEMENT);} // upstream :41
	static OreDictMaterial metal        (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return element     (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(METAL);} // upstream :42
	static OreDictMaterial metalloid    (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return element     (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(METALLOID);} // upstream :43
	static OreDictMaterial nonmetal     (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return element     (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(NONMETAL);} // upstream :44
	static OreDictMaterial diatomic     (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return nonmetal    (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(DIATOMIC_NONMETAL);} // upstream :45
	static OreDictMaterial polyatomic   (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return nonmetal    (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(POLYATOMIC_NONMETAL);} // upstream :46
	static OreDictMaterial noblegas     (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return nonmetal    (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(NOBLE_GAS);} // upstream :47
	static OreDictMaterial alkali       (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(ALKALI_METAL);} // upstream :48
	static OreDictMaterial alkaline     (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(ALKALINE_EARTH_METAL);} // upstream :49
	static OreDictMaterial lanthanide   (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(LANTHANIDE);} // upstream :50
	static OreDictMaterial actinide     (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(ACTINIDE);} // upstream :51
	static OreDictMaterial transmetal   (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(TRANSITION_METAL);} // upstream :52
	static OreDictMaterial posttrans    (int aID, String aNameOreDict, long aProtonsAndElectrons, long aNeutrons, long aMeltingPoint, long aBoilingPoint, double aGramPerCubicCentimeter, String[] aSets, long aR, long aG, long aB, long aA)   {return metal       (aID, aNameOreDict, aProtonsAndElectrons, aNeutrons, aMeltingPoint, aBoilingPoint, aGramPerCubicCentimeter, aSets, aR, aG, aB, aA).put(POST_TRANSITION_METAL);} // upstream :53

	/** MC-strip delegates: the folded field chains call these unqualified; they forward to the MT-local replications of the upstream OreDictMaterial setters (:1024-1125). */
	static OreDictMaterial setRGBa(OreDictMaterial aMaterial, long aR, long aG, long aB, long aA) {return MT.setRGBa(aMaterial, aR, aG, aB, aA);}
	static OreDictMaterial setRGBaLiquid(OreDictMaterial aMaterial, long aR, long aG, long aB, long aA) {return MT.setRGBaLiquid(aMaterial, aR, aG, aB, aA);}
	static OreDictMaterial setTextures(OreDictMaterial aMaterial, String... aSets) {return MT.setTextures(aMaterial, aSets);}
	public static OreDictMaterial y, Photon, v, Neutrino, n, Neutron, p, Proton;
	public static OreDictMaterial e, Positron, Electron, H, Hydrogen, D, Deuterium, H_2;
	public static OreDictMaterial T, Tritium, H_3, He, Helium, He_3, Helium3, Li;
	public static OreDictMaterial Lithium, Li_6, Lithium6, Be, Beryllium, B, Boron, C;
	public static OreDictMaterial Carbon, C_13, Carbon13, C_14, Carbon14, N, Nitrogen, O;
	public static OreDictMaterial Oxygen, F, Fluorine, Ne, Neon, Na, Sodium, Natrium;
	public static OreDictMaterial Mg, Magnesium, Al, Aluminium, Aluminum, Si, Silicon, P;
	public static OreDictMaterial Phosphor, S, Sulfur, Cl, Chlorine, Ar, Argon, K;
	public static OreDictMaterial Potassium, Kalium, Ca, Calcium, Sc, Scandium, Ti, Titanium;
	public static OreDictMaterial Titan, V, Vanadium, Cr, Chromium, Chrome, Mn, Manganese;
	public static OreDictMaterial Fe, Iron, Co, Cobalt, Co_60, Cobalt60, Ni, Nickel;
	public static OreDictMaterial Cu, Copper, Zn, Zinc, Ga, Gallium, Ge, Germanium;
	public static OreDictMaterial As, Arsenic, Se, Selenium, Br, Bromine, Kr, Krypton;
	public static OreDictMaterial Rb, Rubidium, Sr, Strontium, Y, Yttrium, Zr, Zirconium;
	public static OreDictMaterial Nb, Niobium, Mo, Molybdenum, Tc, Technetium, Gregorium, Ru;
	public static OreDictMaterial Ruthenium, Rh, Rhodium, Pd, Palladium, Ag, Silver, Cd;
	public static OreDictMaterial Cadmium, In, Indium, Sn, Tin, Sb, Antimony, Te;
	public static OreDictMaterial Tellurium, I, Iodine, Xe, Xenon, Cs, Caesium, Ba;
	public static OreDictMaterial Barium, La, Lanthanum, Ce, Cerium, Pr, Praseodymium, Nd;
	public static OreDictMaterial Neodymium, Pm, Promethium, Sm, Samarium, Eu, Europium, Gd;
	public static OreDictMaterial Gadolinium, Tb, Terbium, Dy, Dysprosium, Ho, Holmium, Er;
	public static OreDictMaterial Erbium, Tm, Thulium, Yb, Ytterbium, Lu, Lutetium, Hf;
	public static OreDictMaterial Hafnium, Ta, Tantalum, W, Tungsten, Wolframium, Wolfram, Re;
	public static OreDictMaterial Rhenium, Os, Osmium, Ir, Iridium, Pt, Platinum, Au;
	public static OreDictMaterial Gold, Hg, Mercury, Quicksilver, Tl, Thallium, Pb, Lead;
	public static OreDictMaterial Bi, Bismuth, Po, Polonium, At, Astatine, Rn, Radon;
	public static OreDictMaterial Fr, Francium, Ra, Radium, Ac, Actinium, Th, Thorium;
	public static OreDictMaterial Pa, Protactinium, U_238, Uranium, Uranium238, Uran, U_235, Uranium235;
	public static OreDictMaterial Np, Neptunium, Pu, Plutonium, Plutonium244, Pu_241, Plutonium241, Pu_243;
	public static OreDictMaterial Plutonium243, Am, Americium, Am_241, Americium241, Cm, Curium, Bk;
	public static OreDictMaterial Berkelium, Cf, Californium, Es, Einsteinium, Fm, Fermium, Md;
	public static OreDictMaterial Mendelevium, No, Nobelium, Lr, Lawrencium, Rf, Rutherfordium, Db;
	public static OreDictMaterial Dubnium, Sg, Seaborgium, Bh, Bohrium, Hs, Hassium, Mt;
	public static OreDictMaterial Meitnerium, Ds, Darmstadtium, Rg, Roentgenium, Cn, Copernicium, Uut;
	public static OreDictMaterial Ununtrium, Fl, Flerovium, Fl_298, Flerovium298, Uup, Ununpentium, Lv;
	public static OreDictMaterial Livermorium, Fa, Farnsium, Uus, Ununseptium, Uuo, Ununoctium, Uue;
	public static OreDictMaterial Ununennium, Ubn, Unbinilium, Ubu, Unbiunium, Ubb, Unbibium, Ubt;
	public static OreDictMaterial Unbitrium, Ubq, Unbiquadium, Ubp, Unbipentium, Ubh, Unbihexium, Ubs;
	public static OreDictMaterial Unbiseptium, Ubo, Unbioctium, Ube, Unbiennium, Utn, Untrinilium, Utu;
	public static OreDictMaterial Untriunium, Utb, Untribium, Utt, Untritrium, Utq, Untriquadium, Utp;
	public static OreDictMaterial Untripentium, Uth, Untrihexium, Uts, Untriseptium, Uto, Untrioctium, Ute;
	public static OreDictMaterial Untriennium, Uqn, Unquadnilium, Uqu, Unquadunium, Uqb, Unquadbium, Uqt;
	public static OreDictMaterial Unquadtrium, Uqq, Unquadquadium, Uqp, Unquadpentium, Uqh, Unquadhexium, Uqs;
	public static OreDictMaterial Unquadseptium, Uqo, Unquadoctium, Uqe, Unquadennium, Upn, Unpentnilium, Upu;
	public static OreDictMaterial Unpentunium, Vb, Vibranium, Upb, Unpentbium, Upt, Unpenttrium, Upq;
	public static OreDictMaterial Unpentquadium, Upp, Unpentpentium, Uph, Unpenthexium, Ups, Unpentseptium, Upo;
	public static OreDictMaterial Unpentoctium, Upe, Unpentennium, Uhn, Unhexnilium, Uhu, Unhexunium, Uhb;
	public static OreDictMaterial Unhexbium, Uht, Unhextrium, Uhq, Unhexquadium, Uhp, Unhexpentium, Uhh;
	public static OreDictMaterial Unhexhexium, Uhs, Unhexseptium, Uho, Unhexoctium, Uhe, Unhexennium, Usn;
	public static OreDictMaterial Unseptnilium, Usu, Unseptunium, Usb, Unseptbium, Ust, Unsepttrium, Nq;
	public static OreDictMaterial Naquadah, Usq, Unseptquadium, Nq_528, NaquadahEnriched, Nq_522, Naquadria, Usp;
	public static OreDictMaterial Unseptpentium, Ush, Unsepthexium, Uss, Unseptseptium, Uso, Unseptoctium, Use;
	public static OreDictMaterial Unseptennium, Uon, Unoctnilium, Uou, Unoctunium, Uob, Unoctbium, Uot;
	public static OreDictMaterial Unocttrium, Uoq, Unoctquadium, Uop, Unoctpentium, Uoh, Unocthexium, Uos;
	public static OreDictMaterial Unoctseptium, Uoo, Unoctoctium, Uoe, Unoctennium, Uen, Unennilium, Ueu;
	public static OreDictMaterial Unennunium, Ueb, Unennbium, Uet, Unenntrium, Ueq, Unennquadium, Uep;
	public static OreDictMaterial Unennpentium, Ueh, Unennhexium, Ues, Unennseptium, Ueo, Unennoctium, Uee;
	public static OreDictMaterial Unennennium, Bnn, Binilnilium, Bnu, Binilunium, Bnb, Binilbium, Bnt;
	public static OreDictMaterial Biniltrium, Bnq, Binilquadium, Bnp, Binilpentium, Bnh, Binilhexium, Bns;
	public static OreDictMaterial Binilseptium, Bno, Biniloctium, Bne, Binilennium, Bun, Biunnilium, Buu;
	public static OreDictMaterial Biununium, Bub, Biunbium, But, Biuntrium, Buq, Biunquadium, Bup;
	public static OreDictMaterial Biunpentium, Buh, Biunhexium, Bus, Biunseptium, Buo, Biunoctium, Bue;
	public static OreDictMaterial Biunennium, Bbn, Bibinilium, Bbu, Bibiunium, Ad, Adamantium, Bbb;
	public static OreDictMaterial Bibibium, Bbt, Bibitrium, Bbq, Bibiquadium, Bbp, Bibipentium, Bbh;
	public static OreDictMaterial Bibihexium, Bbs, Bibiseptium, Bbo, Bibioctium, Bbe, Bibiennium, Btn;
	public static OreDictMaterial Bitrinilium, Btu, Bitriunium, Btb, Bitribium, Btt, Bitritrium, Btq;
	public static OreDictMaterial Bitriquadium, Btp, Bitripentium, Bth, Bitrihexium, Bts, Bitriseptium, Bto;
	public static OreDictMaterial Bitrioctium, Mcg, MacGuffium, Bte, Bitriennium, Bqn, Biquadnilium, Bqu;
	public static OreDictMaterial Biquadunium, Bqb, Biquadbium, Bqt, Biquadtrium, Bqq, Biquadquadium, Bqp;
	public static OreDictMaterial Biquadpentium, Bqh, Biquadhexium, Bqs, Biquadseptium, Bqo, Biquadoctium, Bqe;
	public static OreDictMaterial Biquadennium, Bpn, Bipentnilium, Bpu, Bipentunium, Bpb, Bipentbium, Bpt;
	public static OreDictMaterial Bipenttrium, Bpq, Bipentquadium, Bpp, Bipentpentium, Bph, Bipenthexium, Bps;
	public static OreDictMaterial Bipentseptium, Bpo, Bipentoctium, Bpe, Bipentennium, Bhn, Bihexnilium, Bhu;
	public static OreDictMaterial Bihexunium, Bhb, Bihexbium, Bht, Bihextrium, Bhq, Bihexquadium, Bhp;
	public static OreDictMaterial Bihexpentium, Bhh, Bihexhexium, Bhs, Bihexseptium, Bho, Bihexoctium, Bhe;
	public static OreDictMaterial Bihexennium, Bsn, Biseptnilium, Bsu, Biseptunium, Bsb, Biseptbium, Bst;
	public static OreDictMaterial Bisepttrium, Bsq, Biseptquadium, Bsp, Biseptpentium, Bsh, Bisepthexium, Bss;
	public static OreDictMaterial Biseptseptium, Bso, Biseptoctium, Bse, Biseptennium, Bon, Bioctnilium, Bou;
	public static OreDictMaterial Bioctunium, Bob, Bioctbium, Bot, Biocttrium, Boq, Bioctquadium, Bop;
	public static OreDictMaterial Bioctpentium, Boh, Biocthexium, Bos, Bioctseptium, Boo, Bioctoctium, Boe;
	public static OreDictMaterial Bioctennium, Ben, Biennilium, Beu, Biennunium, Beb, Biennbium, Bet;
	public static OreDictMaterial Bienntrium, Beq, Biennquadium, Bep, Biennpentium, Beh, Biennhexium, Bes;
	public static OreDictMaterial Biennseptium, Beo, Biennoctium, Bee, Biennennium, Tnn, Trinilnilium, Tnu;
	public static OreDictMaterial Trinilunium, Tnb, Trinilbium, Tnt, Triniltrium, Tnq, Trinilquadium, Tnp;
	public static OreDictMaterial Trinilpentium, Tnh, Trinilhexium, Tns, Trinilseptium, Tno, Triniloctium, Tne;
	public static OreDictMaterial Trinilennium, Tun, Triunnilium, Tuu, Triununium, Tub, Triunbium, Tut;
	public static OreDictMaterial Triuntrium, Tuq, Triunquadium, Tup, Triunpentium, Tuh, Triunhexium, Tus;
	public static OreDictMaterial Triunseptium, Tuo, Triunoctium, Tue, Triunennium, Tbn, Tribinilium, Tbu;
	public static OreDictMaterial Tribiunium, Tbb, Tribibium, Tbt, Tribitrium, Tbq, Tribiquadium, Tbp;
	public static OreDictMaterial Tribipentium, Tbh, Tribihexium, Tbs, Tribiseptium, Tbo, Tribioctium, Tbe;
	public static OreDictMaterial Tribiennium, Ttn, Tritrinilium, Ttu, Tritriunium, Ttb, Tritribium, Ttt;
	public static OreDictMaterial Tritritrium, Ttq, Tritriquadium, Ttp, Tritripentium, Tth, Tritrihexium, Tts;
	public static OreDictMaterial Tritriseptium, Tto, Tritrioctium, Tte, Tritriennium, Tqn, Triquadnilium, Tqu;
	public static OreDictMaterial Triquadunium, Tqb, Triquadbium, Tqt, Triquadtrium, Tqq, Triquadquadium, Tqp;
	public static OreDictMaterial Triquadpentium, Tqh, Triquadhexium, Tqs, Triquadseptium, Tqo, Triquadoctium, Tqe;
	public static OreDictMaterial Triquadennium, Tpn, Tripentnilium, Tpu, Tripentunium, Tpb, Tripentbium, Tpt;
	public static OreDictMaterial Tripenttrium, Tpq, Tripentquadium, Tpp, Tripentpentium, Tph, Tripenthexium, Tps;
	public static OreDictMaterial Tripentseptium, Tpo, Tripentoctium, Tpe, Tripentennium, Thn, Trihexnilium, Thu;
	public static OreDictMaterial Trihexunium, Thb, Trihexbium, Tht, Trihextrium, Thq, Trihexquadium, Thp;
	public static OreDictMaterial Trihexpentium, Thh, Trihexhexium, Ths, Trihexseptium, Tho, Trihexoctium, The;
	public static OreDictMaterial Trihexennium, Tsn, Triseptnilium, Tsu, Triseptunium, Gt, Gravitonium, Tsb;
	public static OreDictMaterial Triseptbium, Tst, Trisepttrium, Tsq, Triseptquadium, Tsp, Triseptpentium, Tsh;
	public static OreDictMaterial Trisepthexium, Tss, Triseptseptium, Tso, Triseptoctium, Tse, Triseptennium, Ton;
	public static OreDictMaterial Trioctnilium, Tou, Trioctunium, Tob, Trioctbium, Tot, Triocttrium, Toq;
	public static OreDictMaterial Trioctquadium, Top, Trioctpentium, Toh, Triocthexium, Tos, Trioctseptium, Too;
	public static OreDictMaterial Trioctoctium, Toe, Trioctennium, Ten, Triennilium, Teu, Triennunium, Teb;
	public static OreDictMaterial Triennbium, Tet, Trienntrium, Teq, Triennquadium, Tep, Triennpentium, Teh;
	public static OreDictMaterial Triennhexium, Tes, Triennseptium, Teo, Triennoctium, Tee, Triennennium;
	private static void reg0000() { // upstream AM.java:57-91
		Photon                =   y       = setTextures(setRGBa(create( 4001, "Anti-Photon"   ).setStatsElement(0,0,0,0,0).heat(0,0,0),   0,   0,   0,   0), MT.SET_NONE).put(PARTICLE);
		Neutrino              =   v       = setTextures(setRGBa(create( 4002, "Anti-Neutrino" ).setStatsElement(0,0,0,0,0).heat(0,0,0),  75,  75,  75,   0), MT.SET_NONE).put(PARTICLE);
		Neutron               =   n       = setTextures(setRGBa(create( 4003, "Anti-Neutron"  ).setStatsElement(0,0,1,0,0).heat(0,0,0), 128, 128, 128,   0), MT.SET_NONE).put(PARTICLE);
		Proton                =   p       = setTextures(setRGBa(create( 4004, "Anti-Proton"   ).setStatsElement(1,0,0,0,0).heat(0,0,0),   0,   0, 255,   0), MT.SET_NONE).put(PARTICLE);
		Positron              =   e       = setTextures(setRGBa(create( 4005, "Positron"      ).setStatsElement(0,1,0,0,0).heat(0,0,0), 255,   0,   0,   0), MT.SET_NONE).put(PARTICLE);
		Electron = e.addIdenticalNames("AntiElectron");
		Hydrogen              =   H       = diatomic      ( 4010, "Anti-Hydrogen"         ,   1,   0,    14,    20,  0.00008988, MT.SET_DULL             ,   0,   0, 255,  15).put(  CONTAINERS_GAS              , UUM                                               );
		Deuterium             =   D       = diatomic      ( 4011, "Anti-Deuterium"        ,   1,   1,    14,    20,  0.00008988, MT.SET_SHINY            , 255, 255,   0,  15).put(  CONTAINERS_GAS                                                                  );
		H_2=D;
		Tritium               =   T       = diatomic      ( 4012, "Anti-Tritium"          ,   1,   2,    14,    20,  0.00008988, MT.SET_SHINY            , 255,   0,   0,  15).put(  CONTAINERS_GAS                                                                  );
		H_3=T;
		Helium                =   He      = noblegas      ( 4020, "Anti-Helium"           ,   2,   2,     1,     4,  0.0001785 , MT.SET_SHINY            , 255, 255, 120,  15).put(  CONTAINERS_GAS              , FUSION, UUM                                       );
		Helium3               =   He_3    = noblegas      ( 4021, "Anti-Helium-3"         ,   2,   1,     1,     4,  0.0001785 , MT.SET_SHINY            , 255, 255, 140,  15).put(  CONTAINERS_GAS              , FUSION                                            );
		Lithium               =   Li      = alkali        ( 4030, "Anti-Lithium"          ,   3,   4,   453,  1560,  0.534     , MT.SET_ROUGH            , 225, 220, 255, 255).put(  G_DUST_ORES                 , FUSION, UUM                                       );
		Lithium6              =   Li_6    = alkali        ( 4031, "Anti-Lithium-6"        ,   3,   3,   453,  1560,  0.534     , MT.SET_ROUGH            , 230, 225, 255, 255).put(  G_DUST_ORES                 , FUSION                                            );
		Beryllium             =   Be      = alkaline      ( 4040, "Anti-Beryllium"        ,   4,   5,  1560,  2742,  1.85      , MT.SET_METALLIC         , 100, 180, 100, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Boron                 =   B       = metalloid     ( 4050, "Anti-Boron"            ,   5,   5,  2349,  4200,  2.34      , MT.SET_DULL             , 250, 250, 250, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Carbon                =   C       = polyatomic    ( 4060, "Anti-Carbon"           ,   6,   6,  3800,  4300,  2.267     , MT.SET_DULL             ,  20,  20,  20, 255).put(  G_DUST_ORES                 , FUSION, UUM                                       );
		Carbon13              =   C_13    = polyatomic    ( 4061, "Anti-Carbon-13"        ,   6,   7,  3800,  4300,  2.267     , MT.SET_DULL             ,  25,  25,  25, 255).put(  G_DUST_ORES                 , FUSION                                            );
		Carbon14              =   C_14    = polyatomic    ( 4062, "Anti-Carbon-14"        ,   6,   8,  3800,  4300,  2.267     , MT.SET_DULL             ,  30,  30,  30, 255).put(  G_DUST_ORES                 , FUSION                                            );
		Nitrogen              =   N       = diatomic      ( 4070, "Anti-Nitrogen"         ,   7,   7,    63,    77,  0.0012506 , MT.SET_DULL             ,   0, 150, 200,  15).put(  CONTAINERS_GAS              , FUSION, UUM                                       );
		Oxygen                =   O       = diatomic      ( 4080, "Anti-Oxygen"           ,   8,   8,    54,    90,  0.001429  , MT.SET_DULL             ,   0, 100, 200,  15).put(  CONTAINERS_GAS              , FUSION, UUM                                       );
		Fluorine              =   F       = diatomic      ( 4090, "Anti-Fluorine"         ,   9,   9,    53,    85,  0.001696  , MT.SET_DULL             ,  64, 192,   0, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Neon                  =   Ne      = noblegas      ( 4100, "Anti-Neon"             ,  10,  10,    24,    27,  0.0008999 , MT.SET_SHINY            , 250, 180, 180,  15).put(  CONTAINERS_GAS              , FUSION, UUM                                       );
		Sodium                =   Na      = alkali        ( 4110, "Anti-Sodium"           ,  11,  11,   370,  1156,  0.971     , MT.SET_ROUGH            ,   0,   0, 150, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Natrium=Na.addIdenticalNames("Anti-Natrium");
		Magnesium             =   Mg      = alkaline      ( 4120, "Anti-Magnesium"        ,  12,  12,   923,  1363,  1.738     , MT.SET_METALLIC         , 255, 200, 200, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Aluminium             =   Al      = posttrans     ( 4130, "Anti-Aluminium"        ,  13,  13,   933,  2792,  2.698     , MT.SET_METALLIC         , 128, 200, 240, 255).put(  G_INGOT_MACHINE_ORES        , FUSION, UUM                                       );
		Aluminum=Al.addIdenticalNames("Anti-Aluminum");
		Silicon               =   Si      = metalloid     ( 4140, "Anti-Silicon"          ,  14,  14,  1687,  3538,  2.3296    , MT.SET_METALLIC         ,  60,  60,  80, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Phosphor              =   P       = polyatomic    ( 4150, "Anti-Phosphor"         ,  15,  15,   317,   550,  1.82      , MT.SET_ROUGH            , 255, 255,   0, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Sulfur                =   S       = polyatomic    ( 4160, "Anti-Sulfur"           ,  16,  16,   388,   717,  2.067     , MT.SET_ROUGH            , 200, 200,   0, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
	}
	private static void reg0001() { // upstream AM.java:92-119
		Chlorine              =   Cl      = diatomic      ( 4170, "Anti-Chlorine"         ,  17,  18,   171,   239,  0.003214  , MT.SET_DULL             ,   0, 240, 255, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Argon                 =   Ar      = noblegas      ( 4180, "Anti-Argon"            ,  18,  22,    83,    87,  0.0017837 , MT.SET_SHINY            ,   0, 255,   0,  15).put(  CONTAINERS_GAS              , FUSION, UUM                                       );
		Potassium             =   K       = alkali        ( 4190, "Anti-Potassium"        ,  19,  20,   336,  1032,  0.862     , MT.SET_ROUGH            , 250, 250, 250, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Kalium=K.addIdenticalNames("Anti-Kalium");
		Calcium               =   Ca      = alkaline      ( 4200, "Anti-Calcium"          ,  20,  20,  1115,  1757,  1.54      , MT.SET_METALLIC         , 255, 245, 245, 255).put(  G_CRYSTAL_ORES              , FUSION, UUM                                       );
		Scandium              =   Sc      = transmetal    ( 4210, "Anti-Scandium"         ,  21,  24,  1814,  3109,  2.989     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Titanium              =   Ti      = transmetal    ( 4220, "Anti-Titanium"         ,  22,  26,  1941,  3560,  4.54      , MT.SET_METALLIC         , 220, 160, 240, 255).put(  G_INGOT_MACHINE_ORES        , FUSION, UUM                                       );
		Titan=Ti.addIdenticalNames("Anti-Titan");
		Vanadium              =   V       = transmetal    ( 4230, "Anti-Vanadium"         ,  23,  28,  2183,  3680,  6.11      , MT.SET_METALLIC         ,  50,  50,  50, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Chromium              =   Cr      = transmetal    ( 4240, "Anti-Chromium"         ,  24,  28,  2180,  2944,  7.15      , MT.SET_SHINY            , 255, 230, 230, 255).put(  G_INGOT_MACHINE_ORES        , FUSION, UUM                                       );
		Chrome=Cr.addIdenticalNames("Anti-Chrome");
		Manganese             =   Mn      = transmetal    ( 4250, "Anti-Manganese"        ,  25,  30,  1519,  2334,  7.44      , MT.SET_DULL             , 250, 250, 250, 255).put(  G_INGOT_ORES                , FUSION, UUM                                       );
		Iron                  =   Fe      = transmetal    ( 4260, "Anti-Iron"             ,  26,  30,  1811,  3134,  7.874     , MT.SET_METALLIC         , 200, 200, 200, 255).put(  G_INGOT_MACHINE_ORES        , FUSION, UUM                                       );
		Cobalt                =   Co      = transmetal    ( 4270, "Anti-Cobalt"           ,  27,  32,  1768,  3200,  8.86      , MT.SET_METALLIC         ,  80,  80, 250, 255).put(  G_INGOT_ORES                , UUM                                               );
		Cobalt60              =   Co_60   = transmetal    ( 4278, "Anti-Cobalt-60"        ,  27,  43,  1768,  3200,  8.86      , MT.SET_SHINY            ,  90,  90, 250, 255).put(  G_INGOT_ORES                                                                    );
		Nickel                =   Ni      = transmetal    ( 4280, "Anti-Nickel"           ,  28,  30,  1728,  3186,  8.912     , MT.SET_METALLIC         , 200, 200, 250, 255).put(  G_INGOT_ORES                , UUM                                               );
		Copper                =   Cu      = transmetal    ( 4290, "Anti-Copper"           ,  29,  34,  1357,  2835,  8.96      , MT.SET_SHINY            , 255, 100,   0, 255).put(  G_INGOT_ORES                , UUM                                               );
		Zinc                  =   Zn      = transmetal    ( 4300, "Anti-Zinc"             ,  30,  35,   692,  1180,  7.134     , MT.SET_METALLIC         , 250, 240, 240, 255).put(  G_INGOT_ORES                , UUM                                               );
		Gallium               =   Ga      = posttrans     ( 4310, "Anti-Gallium"          ,  31,  39,   302,  2477,  5.907     , MT.SET_DULL             , 220, 220, 255, 255).put(  G_INGOT_ORES                , UUM                                               );
		Germanium             =   Ge      = metalloid     ( 4320, "Anti-Germanium"        ,  32,  40,  1211,  3106,  5.323     , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Arsenic               =   As      = metalloid     ( 4330, "Anti-Arsenic"          ,  33,  42,   887,  1090,  5.776     , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Selenium              =   Se      = polyatomic    ( 4340, "Anti-Selenium"         ,  34,  45,   453,   958,  4.809     , MT.SET_DULL             , 256, 256, 256, 255).put(  G_CRYSTAL_ORES              , UUM                                               );
		Bromine               =   Br      = diatomic      ( 4350, "Anti-Bromine"          ,  35,  45,   265,   332,  3.122     , MT.SET_DULL             , 256, 256, 256, 255).put(  G_CRYSTAL_ORES              , UUM                                               );
		Krypton               =   Kr      = noblegas      ( 4360, "Anti-Krypton"          ,  36,  48,   115,   119,  0.003733  , MT.SET_DIAMOND          , 128, 255, 128,  15).put(  CONTAINERS_GAS              , UUM                                               );
		Rubidium              =   Rb      = alkali        ( 4370, "Anti-Rubidium"         ,  37,  48,   312,   961,  1.532     , MT.SET_SHINY            , 240,  30,  30, 255).put(  G_INGOT_ORES                , UUM                                               );
		Strontium             =   Sr      = alkaline      ( 4380, "Anti-Strontium"        ,  38,  49,  1050,  1655,  2.64      , MT.SET_METALLIC         , 200, 200, 200, 255).put(  G_INGOT_ORES                , UUM                                               );
		Yttrium               =   Y       = transmetal    ( 4390, "Anti-Yttrium"          ,  39,  50,  1799,  3609,  4.469     , MT.SET_METALLIC         , 220, 250, 220, 255).put(  G_INGOT_ORES                , UUM                                               );
		Zirconium             =   Zr      = transmetal    ( 4400, "Anti-Zirconium"        ,  40,  51,  2128,  4682,  6.506     , MT.SET_DIAMOND          , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Niobium               =   Nb      = transmetal    ( 4410, "Anti-Niobium"          ,  41,  53,  2750,  5017,  8.57      , MT.SET_METALLIC         , 190, 180, 200, 255).put(  G_INGOT_ORES                , UUM                                               );
		Molybdenum            =   Mo      = transmetal    ( 4420, "Anti-Molybdenum"       ,  42,  53,  2896,  4912, 10.22      , MT.SET_SHINY            , 180, 180, 220, 255).put(  G_INGOT_ORES                , UUM                                               );
		Technetium            =   Tc      = transmetal    ( 4430, "Anti-Technetium"       ,  43,  55,  2430,  4538, 11.5       , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Gregorium=Tc.addIdenticalNames("Anti-Gregorium");
	}
	private static void reg0002() { // upstream AM.java:120-150
		Ruthenium             =   Ru      = transmetal    ( 4440, "Anti-Ruthenium"        ,  44,  57,  2607,  4423, 12.37      , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Rhodium               =   Rh      = transmetal    ( 4450, "Anti-Rhodium"          ,  45,  58,  2237,  3968, 12.41      , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Palladium             =   Pd      = transmetal    ( 4460, "Anti-Palladium"        ,  46,  60,  1828,  3236, 12.02      , MT.SET_SHINY            , 128, 128, 128, 255).put(  G_INGOT_ORES                , UUM                                               );
		Silver                =   Ag      = transmetal    ( 4470, "Anti-Silver"           ,  47,  60,  1234,  2435, 10.501     , MT.SET_SHINY            , 220, 220, 255, 255).put(  G_INGOT_ORES                , UUM                                               );
		Cadmium               =   Cd      = transmetal    ( 4480, "Anti-Cadmium"          ,  48,  64,   594,  1040,  8.69      , MT.SET_SHINY            ,  50,  50,  60, 255).put(  G_INGOT_ORES                , UUM                                               );
		Indium                =   In      = posttrans     ( 4490, "Anti-Indium"           ,  49,  65,   429,  2345,  7.31      , MT.SET_SHINY            ,  64,   0, 128, 255).put(  G_INGOT_ORES                , UUM                                               );
		Tin                   =   Sn      = posttrans     ( 4500, "Anti-Tin"              ,  50,  68,   505,  2875,  7.287     , MT.SET_DULL             , 220, 220, 220, 255).put(  G_INGOT_ORES                , UUM                                               );
		Antimony              =   Sb      = metalloid     ( 4510, "Anti-Antimony"         ,  51,  70,   903,  1860,  6.685     , MT.SET_SHINY            , 220, 220, 240, 255).put(  G_INGOT_ORES                , UUM                                               );
		Tellurium             =   Te      = metalloid     ( 4520, "Anti-Tellurium"        ,  52,  75,   722,  1261,  6.232     , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Iodine                =   I       = diatomic      ( 4530, "Anti-Iodine"           ,  53,  74,   386,   457,  4.93      , MT.SET_DULL             , 255, 240, 240, 255).put(  G_CRYSTAL_ORES              , UUM                                               );
		Xenon                 =   Xe      = noblegas      ( 4540, "Anti-Xenon"            ,  54,  77,   161,   165,  0.005887  , MT.SET_DULL             ,   0, 255, 255,  15).put(  CONTAINERS_GAS              , UUM                                               );
		Caesium               =   Cs      = alkali        ( 4550, "Anti-Caesium"          ,  55,  77,   301,   944,  1.873     , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Barium                =   Ba      = alkaline      ( 4560, "Anti-Barium"           ,  56,  81,  1000,  2170,  3.594     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Lanthanum             =   La      = lanthanide    ( 4570, "Anti-Lanthanum"        ,  57,  81,  1193,  3737,  6.145     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Cerium                =   Ce      = lanthanide    ( 4580, "Anti-Cerium"           ,  58,  82,  1068,  3716,  6.77      , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Praseodymium          =   Pr      = lanthanide    ( 4590, "Anti-Praseodymium"     ,  59,  81,  1208,  3793,  6.773     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Neodymium             =   Nd      = lanthanide    ( 4600, "Anti-Neodymium"        ,  60,  84,  1297,  3347,  7.007     , MT.SET_SHINY            , 100, 100, 100, 255).put(  G_INGOT_ORES                , UUM                                               );
		Promethium            =   Pm      = lanthanide    ( 4610, "Anti-Promethium"       ,  61,  83,  1315,  3273,  7.26      , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Samarium              =   Sm      = lanthanide    ( 4620, "Anti-Samarium"         ,  62,  88,  1345,  2067,  7.52      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Europium              =   Eu      = lanthanide    ( 4630, "Anti-Europium"         ,  63,  88,  1099,  1802,  5.243     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Gadolinium            =   Gd      = lanthanide    ( 4640, "Anti-Gadolinium"       ,  64,  93,  1585,  3546,  7.895     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Terbium               =   Tb      = lanthanide    ( 4650, "Anti-Terbium"          ,  65,  93,  1629,  3503,  8.229     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Dysprosium            =   Dy      = lanthanide    ( 4660, "Anti-Dysprosium"       ,  66,  96,  1680,  2840,  8.55      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Holmium               =   Ho      = lanthanide    ( 4670, "Anti-Holmium"          ,  67,  97,  1734,  2993,  8.795     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Erbium                =   Er      = lanthanide    ( 4680, "Anti-Erbium"           ,  68,  99,  1802,  3141,  9.066     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Thulium               =   Tm      = lanthanide    ( 4690, "Anti-Thulium"          ,  69,  99,  1818,  2223,  9.321     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Ytterbium             =   Yb      = lanthanide    ( 4700, "Anti-Ytterbium"        ,  70, 103,  1097,  1469,  6.965     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Lutetium              =   Lu      = lanthanide    ( 4710, "Anti-Lutetium"         ,  71, 103,  1925,  3675,  9.84      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Hafnium               =   Hf      = transmetal    ( 4720, "Anti-Hafnium"          ,  72, 106,  2506,  4876, 13.31      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Tantalum              =   Ta      = transmetal    ( 4730, "Anti-Tantalum"         ,  73, 107,  3290,  5731, 16.654     , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Tungsten              =   W       = transmetal    ( 4740, "Anti-Tungsten"         ,  74, 109,  3695,  5828, 19.25      , MT.SET_METALLIC         ,  50,  50,  50, 255).put(  G_INGOT_MACHINE_ORES        , UUM                                               );
		Wolframium=W.addIdenticalNames("Anti-Wolframium");
	}
	private static void reg0003() { // upstream AM.java:150-177
		Wolfram=W.addIdenticalNames("Anti-Wolfram");
		Rhenium               =   Re      = transmetal    ( 4750, "Anti-Rhenium"          ,  75, 111,  3459,  5869, 21.02      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Osmium                =   Os      = transmetal    ( 4760, "Anti-Osmium"           ,  76, 114,  3306,  5285, 22.61      , MT.SET_METALLIC         ,  50,  50, 255, 255).put(  G_INGOT_ORES                , UUM                                               );
		Iridium               =   Ir      = setRGBaLiquid(transmetal    ( 4770, "Anti-Iridium"          ,  77, 115,  2719,  4701, 22.56      , MT.SET_DULL             , 240, 240, 245, 255).put(  G_INGOT_MACHINE_ORES        , UUM                                               ), 255, 128, 200, 255);
		Platinum              =   Pt      = transmetal    ( 4780, "Anti-Platinum"         ,  78, 117,  2041,  4098, 21.46      , MT.SET_SHINY            , 255, 255, 200, 255).put(  G_INGOT_ORES                , UUM                                               );
		Gold                  =   Au      = transmetal    ( 4790, "Anti-Gold"             ,  79, 117,  1337,  3129, 19.282     , MT.SET_SHINY            , 255, 230,  80, 255).put(  G_INGOT_ORES                , UUM                                               );
		Mercury               =   Hg      = transmetal    ( 4800, "Anti-Mercury"          ,  80, 120,   234,   629, 13.5336    , MT.SET_SHINY            , 230, 220, 220, 255).put(  G_INGOT_ORES                , UUM                                               );
		Quicksilver=Hg.addIdenticalNames("Anti-Quicksilver").addIdenticalNames("Anti-QuickSilver");
		Thallium              =   Tl      = posttrans     ( 4810, "Anti-Thallium"         ,  81, 123,   577,  1746, 11.85      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                , UUM                                               );
		Lead                  =   Pb      = posttrans     ( 4820, "Anti-Lead"             ,  82, 125,   600,  2022, 11.342     , MT.SET_DULL             , 140, 100, 140, 255).put(  G_INGOT_ORES                , UUM                                               );
		Bismuth               =   Bi      = posttrans     ( 4830, "Anti-Bismuth"          ,  83, 125,   544,  1837,  9.807     , MT.SET_METALLIC         , 100, 160, 160, 255).put(  G_INGOT_ORES                                                                    );
		Polonium              =   Po      = posttrans     ( 4840, "Anti-Polonium"         ,  84, 124,   527,  1235,  9.32      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Astatine              =   At      = metalloid     ( 4850, "Anti-Astatine"         ,  85, 124,   575,   610,  7.0       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Radon                 =   Rn      = noblegas      ( 4860, "Anti-Radon"            ,  86, 134,   202,   211,  0.00973   , MT.SET_DULL             , 255,   0, 255,  15).put(  CONTAINERS_GAS                                                                  );
		Francium              =   Fr      = alkali        ( 4870, "Anti-Francium"         ,  87, 134,   300,   950,  1.87      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Radium                =   Ra      = alkaline      ( 4880, "Anti-Radium"           ,  88, 136,   973,  2010,  5.5       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Actinium              =   Ac      = actinide      ( 4890, "Anti-Actinium"         ,  89, 136,  1323,  3471, 10.07      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Thorium               =   Th      = actinide      ( 4900, "Anti-Thorium"          ,  90, 140,  2115,  5061, 11.72      , MT.SET_SHINY            ,   0,  30,   0, 255).put(  G_INGOT_ORES                                                                    );
		Protactinium          =   Pa      = actinide      ( 4910, "Anti-Protactinium"     ,  91, 138,  1841,  4300, 15.37      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Uranium               =   U_238   = actinide      ( 4920, "Anti-Uranium"          ,  92, 146,  1405,  4404, 18.95      , MT.SET_METALLIC         ,  50, 240,  50, 255).put(  G_INGOT_ORES                                                                    );
		Uranium238=U_238.addIdenticalNames("Anti-Uranium238");
		Uran=U_238.addIdenticalNames("Anti-Uran");
		Uranium235            =   U_235   = actinide      ( 4921, "Anti-Uranium-235"      ,  92, 143,  1405,  4404, 18.95      , MT.SET_SHINY            ,  70, 250,  70, 255).put(  G_INGOT_ORES                                                                    );
		Neptunium             =   Np      = actinide      ( 4930, "Anti-Neptunium"        ,  93, 144,   917,  4273, 20.45      , MT.SET_DULL             , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Plutonium             =   Pu      = actinide      ( 4940, "Anti-Plutonium"        ,  94, 150,   912,  3501, 19.84      , MT.SET_METALLIC         , 240,  50,  50, 255).put(  G_INGOT_ORES                                                                    );
		Plutonium244=Pu.addIdenticalNames("Anti-Plutonium244");
		Plutonium241          =   Pu_241  = actinide      ( 4943, "Anti-Plutonium-241"    ,  94, 147,   912,  3501, 19.84      , MT.SET_SHINY            , 245,  70,  70, 255).put(  G_INGOT_ORES                                                                    );
		Plutonium243          =   Pu_243  = actinide      ( 4945, "Anti-Plutonium-243"    ,  94, 149,   912,  3501, 19.84      , MT.SET_SHINY            , 250,  70,  70, 255).put(  G_INGOT_ORES                                                                    );
		Americium             =   Am      = actinide      ( 4950, "Anti-Americium"        ,  95, 150,  1449,  2880, 13.69      , MT.SET_METALLIC         , 200, 200, 200, 255).put(  G_INGOT_ORES                                                                    );
		Americium241          =   Am_241  = actinide      ( 4951, "Anti-Americium-241"    ,  95, 146,  1449,  2880, 13.69      , MT.SET_SHINY            , 210, 210, 210, 255).put(  G_INGOT_ORES                                                                    );
		Curium                =   Cm      = actinide      ( 4960, "Anti-Curium"           ,  96, 153,  1613,  3383, 13.51      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Berkelium             =   Bk      = actinide      ( 4970, "Anti-Berkelium"        ,  97, 152,  1259,  2900, 14.79      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
	}
	private static void reg0004() { // upstream AM.java:178-209
		Californium           =   Cf      = actinide      ( 4980, "Anti-Californium"      ,  98, 153,  1173,  1743, 15.1       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Einsteinium           =   Es      = actinide      ( 4990, "Anti-Einsteinium"      ,  99, 153,  1133,  1269,  8.84      , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Fermium               =   Fm      = actinide      ( 5000, "Anti-Fermium"          , 100, 157,  1125, 3000 ,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Mendelevium           =   Md      = actinide      ( 5010, "Anti-Mendelevium"      , 101, 157,  1100, 3000 ,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Nobelium              =   No      = actinide      ( 5020, "Anti-Nobelium"         , 102, 157,  1100, 3000 ,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Lawrencium            =   Lr      = actinide      ( 5030, "Anti-Lawrencium"       , 103, 159,  1900, 3000 ,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Rutherfordium         =   Rf      = transmetal    ( 5040, "Anti-Rutherfordium"    , 104, 161,  2400,  5800, 23.2       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Dubnium               =   Db      = transmetal    ( 5050, "Anti-Dubnium"          , 105, 163, 1000 , 3000 , 29.3       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Seaborgium            =   Sg      = transmetal    ( 5060, "Anti-Seaborgium"       , 106, 165, 1000 , 3000 , 35.0       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Bohrium               =   Bh      = transmetal    ( 5070, "Anti-Bohrium"          , 107, 163, 1000 , 3000 , 37.1       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Hassium               =   Hs      = transmetal    ( 5080, "Anti-Hassium"          , 108, 169, 1000 , 3000 , 40.7       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Meitnerium            =   Mt      = element       ( 5090, "Anti-Meitnerium"       , 109, 167, 1000 , 3000 , 37.4       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Darmstadtium          =   Ds      = element       ( 5100, "Anti-Darmstadtium"     , 110, 171, 1000 , 3000 , 34.8       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Roentgenium           =   Rg      = element       ( 5110, "Anti-Roentgenium"      , 111, 169, 1000 , 3000 , 28.7       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Copernicium           =   Cn      = transmetal    ( 5120, "Anti-Copernicium"      , 112, 173,  150 ,   357, 23.7       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Ununtrium             =   Uut     = element       ( 5130, "Anti-Ununtrium"        , 113, 171,   700,  1400, 16.0       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Flerovium             =   Fl      = posttrans     ( 5140, "Anti-Flerovium"        , 114, 175,   340,   420, 14.0       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Flerovium298          =   Fl_298  = posttrans     ( 5148, "Anti-Flerovium-298"    , 114, 184,   340,   420, 14.0       , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Ununpentium           =   Uup     = element       ( 5150, "Anti-Ununpentium"      , 115, 174,   700,  1400, 13.5       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Livermorium           =   Lv      = element       ( 5160, "Anti-Livermorium"      , 116, 177,   708,  1085, 12.9       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Farnsium              =   Fa      = element       ( 5170, "Anti-Farnsium"         , 117, 177,   673,   823,  7.2       , MT.SET_SHINY            , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Uus=Fa;
		Ununseptium=Fa.addIdenticalNames("Anti-Ununseptium");
		Ununoctium            =   Uuo     = element       ( 5180, "Anti-Ununoctium"       , 118, 176,   258,   263,  5.0       , MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Ununennium            =   Uue     = element       ( 5190, "Anti-Ununennium"       , 119, 178,   290,   903,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Unbinilium            =   Ubn     = element       ( 5200, "Anti-Unbinilium"       , 120, 180,   953,  1973,           0, MT.SET_METALLIC         , 256, 256, 256, 255).put(  G_INGOT_ORES                                                                    );
		Unbiunium             =   Ubu     = element       ( 5210, "Anti-Unbiunium"        , 121, 182,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbibium              =   Ubb     = element       ( 5220, "Anti-Unbibium"         , 122, 184,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbitrium             =   Ubt     = element       ( 5230, "Anti-Unbitrium"        , 123, 186,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbiquadium           =   Ubq     = element       ( 5240, "Anti-Unbiquadium"      , 124, 188,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbipentium           =   Ubp     = element       ( 5250, "Anti-Unbipentium"      , 125, 190,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbihexium            =   Ubh     = element       ( 5260, "Anti-Unbihexium"       , 126, 192,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0005() { // upstream AM.java:210-239
		Unbiseptium           =   Ubs     = element       ( 5270, "Anti-Unbiseptium"      , 127, 194,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbioctium            =   Ubo     = element       ( 5280, "Anti-Unbioctium"       , 128, 196,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unbiennium            =   Ube     = element       ( 5290, "Anti-Unbiennium"       , 129, 198,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untrinilium           =   Utn     = element       ( 5300, "Anti-Untrinilium"      , 130, 200,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untriunium            =   Utu     = element       ( 5310, "Anti-Untriunium"       , 131, 203,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untribium             =   Utb     = element       ( 5320, "Anti-Untribium"        , 132, 206,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untritrium            =   Utt     = element       ( 5330, "Anti-Untritrium"       , 133, 209,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untriquadium          =   Utq     = element       ( 5340, "Anti-Untriquadium"     , 134, 212,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untripentium          =   Utp     = element       ( 5350, "Anti-Untripentium"     , 135, 215,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untrihexium           =   Uth     = element       ( 5360, "Anti-Untrihexium"      , 136, 218,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untriseptium          =   Uts     = element       ( 5370, "Anti-Untriseptium"     , 137, 221,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untrioctium           =   Uto     = element       ( 5380, "Anti-Untrioctium"      , 138, 224,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Untriennium           =   Ute     = element       ( 5390, "Anti-Untriennium"      , 139, 227,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadnilium          =   Uqn     = element       ( 5400, "Anti-Unquadnilium"     , 140, 230,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadunium           =   Uqu     = element       ( 5410, "Anti-Unquadunium"      , 141, 233,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadbium            =   Uqb     = element       ( 5420, "Anti-Unquadbium"       , 142, 236,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadtrium           =   Uqt     = element       ( 5430, "Anti-Unquadtrium"      , 143, 239,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadquadium         =   Uqq     = element       ( 5440, "Anti-Unquadquadium"    , 144, 242,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadpentium         =   Uqp     = element       ( 5450, "Anti-Unquadpentium"    , 145, 245,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadhexium          =   Uqh     = element       ( 5460, "Anti-Unquadhexium"     , 146, 248,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadseptium         =   Uqs     = element       ( 5470, "Anti-Unquadseptium"    , 147, 251,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadoctium          =   Uqo     = element       ( 5480, "Anti-Unquadoctium"     , 148, 254,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unquadennium          =   Uqe     = element       ( 5490, "Anti-Unquadennium"     , 149, 257,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentnilium          =   Upn     = element       ( 5500, "Anti-Unpentnilium"     , 150, 260,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentunium           =   Upu     = element       ( 5510, "Anti-Unpentunium"      , 151, 263,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Vibranium             =   Vb      = element       ( 5520, "Anti-Vibranium"        , 152, 266,  4852,  9415,  3.23978365, MT.SET_EMERALD          , 200, 128, 255, 100).put(  G_GEM_ORES_TRANSPARENT                                                          );
		Upb = Vb;
		Unpentbium = Vb.addIdenticalNames("Anti-Unpentbium").qual(3, 1000.0F, 512, 15);
		Unpenttrium           =   Upt     = element       ( 5530, "Anti-Unpenttrium"      , 153, 269,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentquadium         =   Upq     = element       ( 5540, "Anti-Unpentquadium"    , 154, 272,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentpentium         =   Upp     = element       ( 5550, "Anti-Unpentpentium"    , 155, 276,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpenthexium          =   Uph     = element       ( 5560, "Anti-Unpenthexium"     , 156, 280,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0006() { // upstream AM.java:240-269
		Unpentseptium         =   Ups     = element       ( 5570, "Anti-Unpentseptium"    , 157, 284,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentoctium          =   Upo     = element       ( 1580, "Anti-Unpentoctium"     , 158, 288,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unpentennium          =   Upe     = element       ( 5590, "Anti-Unpentennium"     , 159, 292,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexnilium           =   Uhn     = element       ( 5600, "Anti-Unhexnilium"      , 160, 296,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexunium            =   Uhu     = element       ( 5610, "Anti-Unhexunium"       , 161, 300,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexbium             =   Uhb     = element       ( 5620, "Anti-Unhexbium"        , 162, 304,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhextrium            =   Uht     = element       ( 5630, "Anti-Unhextrium"       , 163, 308,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexquadium          =   Uhq     = element       ( 5640, "Anti-Unhexquadium"     , 164, 312,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexpentium          =   Uhp     = element       ( 5650, "Anti-Unhexpentium"     , 165, 316,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexhexium           =   Uhh     = element       ( 5660, "Anti-Unhexhexium"      , 166, 320,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexseptium          =   Uhs     = element       ( 5670, "Anti-Unhexseptium"     , 167, 324,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexoctium           =   Uho     = element       ( 5680, "Anti-Unhexoctium"      , 168, 328,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unhexennium           =   Uhe     = element       ( 5690, "Anti-Unhexennium"      , 169, 332,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptnilium          =   Usn     = element       ( 5700, "Anti-Unseptnilium"     , 170, 336,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptunium           =   Usu     = element       ( 5710, "Anti-Unseptunium"      , 171, 340,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptbium            =   Usb     = element       ( 5720, "Anti-Unseptbium"       , 172, 344,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unsepttrium           =   Ust     = element       ( 5730, "Anti-Unsepttrium"      , 173, 348,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Naquadah              =   Nq      = setRGBaLiquid(element       ( 5740, "Anti-Naquadah"         , 174, 352,   500,  2000, 21.0       , MT.SET_METALLIC         ,  50,  50,  50, 255).put(  G_INGOT_MACHINE_ORES                                                            ),   0, 255,   0, 255);
		Usq = Nq;
		Unseptquadium = Nq.addIdenticalNames("Anti-Unseptquadium");
		NaquadahEnriched      =   Nq_528  = setRGBaLiquid(element       ( 5741, "Anti-Naquadah-Enriched", 174, 354,   500,  2000, 22.0       , MT.SET_METALLIC         ,  60,  60,  60, 255).put(  G_INGOT_ORES                                                                    ),  64, 255,  64, 255).setLocal("Enriched Anti-Naquadah");
		Naquadria             =   Nq_522  = setRGBaLiquid(element       ( 5742, "Anti-Naquadria"        , 174, 348,   500,  2000, 20.0       , MT.SET_SHINY            ,  30,  30,  30, 255).put(  G_INGOT_ORES                                                                    ), 128, 255, 128, 255);
		Unseptpentium         =   Usp     = element       ( 5750, "Anti-Unseptpentium"    , 175, 356,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unsepthexium          =   Ush     = element       ( 5760, "Anti-Unsepthexium"     , 176, 360,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptseptium         =   Uss     = element       ( 5770, "Anti-Unseptseptium"    , 177, 364,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptoctium          =   Uso     = element       ( 5780, "Anti-Unseptoctium"     , 178, 368,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unseptennium          =   Use     = element       ( 5790, "Anti-Unseptennium"     , 179, 372,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctnilium           =   Uon     = element       ( 5800, "Anti-Unoctnilium"      , 180, 376,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctunium            =   Uou     = element       ( 5810, "Anti-Unoctunium"       , 181, 380,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctbium             =   Uob     = element       ( 5820, "Anti-Unoctbium"        , 182, 384,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unocttrium            =   Uot     = element       ( 5830, "Anti-Unocttrium"       , 183, 388,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctquadium          =   Uoq     = element       ( 5840, "Anti-Unoctquadium"     , 184, 392,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0007() { // upstream AM.java:270-301
		Unoctpentium          =   Uop     = element       ( 5850, "Anti-Unoctpentium"     , 185, 396,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unocthexium           =   Uoh     = element       ( 5860, "Anti-Unocthexium"      , 186, 400,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctseptium          =   Uos     = element       ( 5870, "Anti-Unoctseptium"     , 187, 405,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctoctium           =   Uoo     = element       ( 5880, "Anti-Unoctoctium"      , 188, 410,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unoctennium           =   Uoe     = element       ( 5890, "Anti-Unoctennium"      , 189, 415,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennilium            =   Uen     = element       ( 5900, "Anti-Unennilium"       , 190, 420,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennunium            =   Ueu     = element       ( 5910, "Anti-Unennunium"       , 191, 425,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennbium             =   Ueb     = element       ( 5920, "Anti-Unennbium"        , 192, 430,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unenntrium            =   Uet     = element       ( 5930, "Anti-Unenntrium"       , 193, 435,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennquadium          =   Ueq     = element       ( 5940, "Anti-Unennquadium"     , 194, 440,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennpentium          =   Uep     = element       ( 5950, "Anti-Unennpentium"     , 195, 445,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennhexium           =   Ueh     = element       ( 5960, "Anti-Unennhexium"      , 196, 450,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennseptium          =   Ues     = element       ( 5970, "Anti-Unennseptium"     , 197, 455,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennoctium           =   Ueo     = element       ( 5980, "Anti-Unennoctium"      , 198, 460,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Unennennium           =   Uee     = element       ( 5990, "Anti-Unennennium"      , 199, 465,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilnilium           =   Bnn     = element       ( 6000, "Anti-Binilnilium"      , 200, 470,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilunium            =   Bnu     = element       ( 6010, "Anti-Binilunium"       , 201, 475,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilbium             =   Bnb     = element       ( 6020, "Anti-Binilbium"        , 202, 480,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biniltrium            =   Bnt     = element       ( 6030, "Anti-Biniltrium"       , 203, 485,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilquadium          =   Bnq     = element       ( 6040, "Anti-Binilquadium"     , 204, 490,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilpentium          =   Bnp     = element       ( 6050, "Anti-Binilpentium"     , 205, 495,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilhexium           =   Bnh     = element       ( 6060, "Anti-Binilhexium"      , 206, 500,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilseptium          =   Bns     = element       ( 6070, "Anti-Binilseptium"     , 207, 505,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biniloctium           =   Bno     = element       ( 6080, "Anti-Biniloctium"      , 208, 510,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Binilennium           =   Bne     = element       ( 6090, "Anti-Binilennium"      , 209, 515,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunnilium            =   Bun     = element       ( 6100, "Anti-Biunnilium"       , 210, 520,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biununium             =   Buu     = element       ( 6110, "Anti-Biununium"        , 211, 525,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunbium              =   Bub     = element       ( 6120, "Anti-Biunbium"         , 212, 530,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biuntrium             =   But     = element       ( 6130, "Anti-Biuntrium"        , 213, 535,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunquadium           =   Buq     = element       ( 6140, "Anti-Biunquadium"      , 214, 540,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunpentium           =   Bup     = element       ( 6150, "Anti-Biunpentium"      , 215, 545,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunhexium            =   Buh     = element       ( 6160, "Anti-Biunhexium"       , 216, 550,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0008() { // upstream AM.java:302-329
		Biunseptium           =   Bus     = element       ( 6170, "Anti-Biunseptium"      , 217, 555,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunoctium            =   Buo     = element       ( 6180, "Anti-Biunoctium"       , 218, 560,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biunennium            =   Bue     = element       ( 6190, "Anti-Biunennium"       , 219, 565,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibinilium            =   Bbn     = element       ( 6200, "Anti-Bibinilium"       , 220, 570,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibiunium             =   Bbu     = element       ( 6210, "Anti-Bibiunium"        , 221, 575,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Adamantium            =   Ad      = element       ( 6220, "Anti-Adamantium"       , 222, 580,  5425, 14528, 13.35624762, MT.SET_SHINY            , 255, 255, 255, 255).put(G_INGOT_MACHINE_ORES, SMITHABLE, MELTING, MAGICAL                                 ).qual(3, 10.0F, 5120, 5).addIdenticalNames("Anti-Adamantine", "Anti-Adamant");
		Bbb = Ad;
		Bibibium = Ad.addIdenticalNames("Anti-Bibibium");
		Bibitrium             =   Bbt     = element       ( 6230, "Anti-Bibitrium"        , 223, 585,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibiquadium           =   Bbq     = element       ( 6240, "Anti-Bibiquadium"      , 224, 590,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibipentium           =   Bbp     = element       ( 6250, "Anti-Bibipentium"      , 225, 595,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibihexium            =   Bbh     = element       ( 6260, "Anti-Bibihexium"       , 226, 600,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibiseptium           =   Bbs     = element       ( 6270, "Anti-Bibiseptium"      , 227, 605,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibioctium            =   Bbo     = element       ( 6280, "Anti-Bibioctium"       , 228, 610,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bibiennium            =   Bbe     = element       ( 6290, "Anti-Bibiennium"       , 229, 615,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitrinilium           =   Btn     = element       ( 6300, "Anti-Bitrinilium"      , 230, 620,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitriunium            =   Btu     = element       ( 6310, "Anti-Bitriunium"       , 231, 625,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitribium             =   Btb     = element       ( 6320, "Anti-Bitribium"        , 232, 630,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitritrium            =   Btt     = element       ( 6330, "Anti-Bitritrium"       , 233, 635,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitriquadium          =   Btq     = element       ( 6340, "Anti-Bitriquadium"     , 234, 640,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitripentium          =   Btp     = element       ( 6350, "Anti-Bitripentium"     , 235, 645,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitrihexium           =   Bth     = element       ( 6360, "Anti-Bitrihexium"      , 236, 650,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitriseptium          =   Bts     = element       ( 6370, "Anti-Bitriseptium"     , 237, 655,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bitrioctium           =   Bto     = element       ( 6380, "Anti-Bitrioctium"      , 238, 660,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		MacGuffium            =   Mcg     = element       ( 6390, "Anti-Mac-Guffium"      , 239, 665,   200,  1000,  3.122     , MT.SET_SHINY            , 200,  50, 150, 255).put(  CONTAINERS                                                                      );
		Bte = Mcg;
		Bitriennium = Mcg.addIdenticalNames("Anti-Bitriennium");
		Biquadnilium          =   Bqn     = element       ( 6400, "Anti-Biquadnilium"     , 240, 670,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadunium           =   Bqu     = element       ( 6410, "Anti-Biquadunium"      , 241, 675,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadbium            =   Bqb     = element       ( 6420, "Anti-Biquadbium"       , 242, 680,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadtrium           =   Bqt     = element       ( 6430, "Anti-Biquadtrium"      , 243, 685,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadquadium         =   Bqq     = element       ( 6440, "Anti-Biquadquadium"    , 244, 690,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0009() { // upstream AM.java:330-361
		Biquadpentium         =   Bqp     = element       ( 6450, "Anti-Biquadpentium"    , 245, 695,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadhexium          =   Bqh     = element       ( 6460, "Anti-Biquadhexium"     , 246, 700,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadseptium         =   Bqs     = element       ( 6470, "Anti-Biquadseptium"    , 247, 705,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadoctium          =   Bqo     = element       ( 6480, "Anti-Biquadoctium"     , 248, 710,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biquadennium          =   Bqe     = element       ( 6490, "Anti-Biquadennium"     , 249, 715,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentnilium          =   Bpn     = element       ( 6500, "Anti-Bipentnilium"     , 250, 720,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentunium           =   Bpu     = element       ( 6510, "Anti-Bipentunium"      , 251, 725,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentbium            =   Bpb     = element       ( 6520, "Anti-Bipentbium"       , 252, 730,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipenttrium           =   Bpt     = element       ( 6530, "Anti-Bipenttrium"      , 253, 735,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentquadium         =   Bpq     = element       ( 6540, "Anti-Bipentquadium"    , 254, 740,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentpentium         =   Bpp     = element       ( 6550, "Anti-Bipentpentium"    , 255, 745,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipenthexium          =   Bph     = element       ( 6560, "Anti-Bipenthexium"     , 256, 750,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentseptium         =   Bps     = element       ( 6570, "Anti-Bipentseptium"    , 257, 755,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentoctium          =   Bpo     = element       ( 6580, "Anti-Bipentoctium"     , 258, 760,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bipentennium          =   Bpe     = element       ( 6590, "Anti-Bipentennium"     , 259, 765,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexnilium           =   Bhn     = element       ( 6600, "Anti-Bihexnilium"      , 260, 770,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexunium            =   Bhu     = element       ( 6610, "Anti-Bihexunium"       , 261, 775,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexbium             =   Bhb     = element       ( 6620, "Anti-Bihexbium"        , 262, 780,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihextrium            =   Bht     = element       ( 6630, "Anti-Bihextrium"       , 263, 785,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexquadium          =   Bhq     = element       ( 6640, "Anti-Bihexquadium"     , 264, 790,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexpentium          =   Bhp     = element       ( 6650, "Anti-Bihexpentium"     , 265, 795,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexhexium           =   Bhh     = element       ( 6660, "Anti-Bihexhexium"      , 266, 800,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexseptium          =   Bhs     = element       ( 6670, "Anti-Bihexseptium"     , 267, 805,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexoctium           =   Bho     = element       ( 6680, "Anti-Bihexoctium"      , 268, 810,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bihexennium           =   Bhe     = element       ( 6690, "Anti-Bihexennium"      , 269, 815,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptnilium          =   Bsn     = element       ( 6700, "Anti-Biseptnilium"     , 270, 820,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptunium           =   Bsu     = element       ( 6710, "Anti-Biseptunium"      , 271, 825,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptbium            =   Bsb     = element       ( 6720, "Anti-Biseptbium"       , 272, 830,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bisepttrium           =   Bst     = element       ( 6730, "Anti-Bisepttrium"      , 273, 835,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptquadium         =   Bsq     = element       ( 6740, "Anti-Biseptquadium"    , 274, 840,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptpentium         =   Bsp     = element       ( 6750, "Anti-Biseptpentium"    , 275, 845,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bisepthexium          =   Bsh     = element       ( 6760, "Anti-Bisepthexium"     , 276, 850,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0010() { // upstream AM.java:362-393
		Biseptseptium         =   Bss     = element       ( 6770, "Anti-Biseptseptium"    , 277, 855,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptoctium          =   Bso     = element       ( 6780, "Anti-Biseptoctium"     , 278, 860,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biseptennium          =   Bse     = element       ( 6790, "Anti-Biseptennium"     , 279, 865,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctnilium           =   Bon     = element       ( 6800, "Anti-Bioctnilium"      , 280, 870,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctunium            =   Bou     = element       ( 6810, "Anti-Bioctunium"       , 281, 875,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctbium             =   Bob     = element       ( 6820, "Anti-Bioctbium"        , 282, 880,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biocttrium            =   Bot     = element       ( 6830, "Anti-Biocttrium"       , 283, 885,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctquadium          =   Boq     = element       ( 6840, "Anti-Bioctquadium"     , 284, 890,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctpentium          =   Bop     = element       ( 6850, "Anti-Bioctpentium"     , 285, 895,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biocthexium           =   Boh     = element       ( 6860, "Anti-Biocthexium"      , 286, 900,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctseptium          =   Bos     = element       ( 6870, "Anti-Bioctseptium"     , 287, 905,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctoctium           =   Boo     = element       ( 6880, "Anti-Bioctoctium"      , 288, 910,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bioctennium           =   Boe     = element       ( 6890, "Anti-Bioctennium"      , 289, 915,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennilium            =   Ben     = element       ( 6900, "Anti-Biennilium"       , 290, 920,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennunium            =   Beu     = element       ( 6910, "Anti-Biennunium"       , 291, 925,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennbium             =   Beb     = element       ( 6920, "Anti-Biennbium"        , 292, 930,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Bienntrium            =   Bet     = element       ( 6930, "Anti-Bienntrium"       , 293, 935,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennquadium          =   Beq     = element       ( 6940, "Anti-Biennquadium"     , 294, 940,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennpentium          =   Bep     = element       ( 6950, "Anti-Biennpentium"     , 295, 945,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennhexium           =   Beh     = element       ( 6960, "Anti-Biennhexium"      , 296, 950,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennseptium          =   Bes     = element       ( 6970, "Anti-Biennseptium"     , 297, 955,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennoctium           =   Beo     = element       ( 6980, "Anti-Biennoctium"      , 298, 960,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Biennennium           =   Bee     = element       ( 6990, "Anti-Biennennium"      , 299, 965,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilnilium          =   Tnn     = element       ( 7000, "Anti-Trinilnilium"     , 300, 970,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilunium           =   Tnu     = element       ( 7010, "Anti-Trinilunium"      , 301, 975,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilbium            =   Tnb     = element       ( 7020, "Anti-Trinilbium"       , 302, 980,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triniltrium           =   Tnt     = element       ( 7030, "Anti-Triniltrium"      , 303, 985,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilquadium         =   Tnq     = element       ( 7040, "Anti-Trinilquadium"    , 304, 990,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilpentium         =   Tnp     = element       ( 7050, "Anti-Trinilpentium"    , 305, 995,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilhexium          =   Tnh     = element       ( 7060, "Anti-Trinilhexium"     , 306,1000,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trinilseptium         =   Tns     = element       ( 7070, "Anti-Trinilseptium"    , 307,1005,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triniloctium          =   Tno     = element       ( 7080, "Anti-Triniloctium"     , 308,1010,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0011() { // upstream AM.java:394-425
		Trinilennium          =   Tne     = element       ( 7090, "Anti-Trinilennium"     , 309,1015,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunnilium           =   Tun     = element       ( 7100, "Anti-Triunnilium"      , 310,1020,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triununium            =   Tuu     = element       ( 7110, "Anti-Triununium"       , 311,1025,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunbium             =   Tub     = element       ( 7120, "Anti-Triunbium"        , 312,1030,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triuntrium            =   Tut     = element       ( 7130, "Anti-Triuntrium"       , 313,1035,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunquadium          =   Tuq     = element       ( 7140, "Anti-Triunquadium"     , 314,1040,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunpentium          =   Tup     = element       ( 7150, "Anti-Triunpentium"     , 315,1045,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunhexium           =   Tuh     = element       ( 7160, "Anti-Triunhexium"      , 316,1050,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunseptium          =   Tus     = element       ( 7170, "Anti-Triunseptium"     , 317,1055,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunoctium           =   Tuo     = element       ( 7180, "Anti-Triunoctium"      , 318,1060,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triunennium           =   Tue     = element       ( 7190, "Anti-Triunennium"      , 319,1065,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribinilium           =   Tbn     = element       ( 7200, "Anti-Tribinilium"      , 320,1070,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribiunium            =   Tbu     = element       ( 7210, "Anti-Tribiunium"       , 321,1075,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribibium             =   Tbb     = element       ( 7220, "Anti-Tribibium"        , 322,1080,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribitrium            =   Tbt     = element       ( 7230, "Anti-Tribitrium"       , 323,1085,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribiquadium          =   Tbq     = element       ( 7240, "Anti-Tribiquadium"     , 324,1090,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribipentium          =   Tbp     = element       ( 7250, "Anti-Tribipentium"     , 325,1095,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribihexium           =   Tbh     = element       ( 7260, "Anti-Tribihexium"      , 326,1100,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribiseptium          =   Tbs     = element       ( 7270, "Anti-Tribiseptium"     , 327,1105,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribioctium           =   Tbo     = element       ( 7280, "Anti-Tribioctium"      , 328,1110,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tribiennium           =   Tbe     = element       ( 7290, "Anti-Tribiennium"      , 329,1115,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritrinilium          =   Ttn     = element       ( 7300, "Anti-Tritrinilium"     , 330,1120,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritriunium           =   Ttu     = element       ( 7310, "Anti-Tritriunium"      , 331,1125,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritribium            =   Ttb     = element       ( 7320, "Anti-Tritribium"       , 332,1130,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritritrium           =   Ttt     = element       ( 7330, "Anti-Tritritrium"      , 333,1135,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritriquadium         =   Ttq     = element       ( 7340, "Anti-Tritriquadium"    , 334,1140,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritripentium         =   Ttp     = element       ( 7350, "Anti-Tritripentium"    , 335,1145,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritrihexium          =   Tth     = element       ( 7360, "Anti-Tritrihexium"     , 336,1150,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritriseptium         =   Tts     = element       ( 7370, "Anti-Tritriseptium"    , 337,1155,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritrioctium          =   Tto     = element       ( 7380, "Anti-Tritrioctium"     , 338,1160,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tritriennium          =   Tte     = element       ( 7390, "Anti-Tritriennium"     , 339,1165,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadnilium         =   Tqn     = element       ( 7400, "Anti-Triquadnilium"    , 340,1170,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	private static void reg0012() { // upstream AM.java:426-457
		Triquadunium          =   Tqu     = element       ( 7410, "Anti-Triquadunium"     , 341,1175,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadbium           =   Tqb     = element       ( 7420, "Anti-Triquadbium"      , 342,1180,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadtrium          =   Tqt     = element       ( 7430, "Anti-Triquadtrium"     , 343,1185,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadquadium        =   Tqq     = element       ( 7440, "Anti-Triquadquadium"   , 344,1190,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadpentium        =   Tqp     = element       ( 7450, "Anti-Triquadpentium"   , 345,1195,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadhexium         =   Tqh     = element       ( 7460, "Anti-Triquadhexium"    , 346,1200,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadseptium        =   Tqs     = element       ( 7470, "Anti-Triquadseptium"   , 347,1205,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadoctium         =   Tqo     = element       ( 7480, "Anti-Triquadoctium"    , 348,1210,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triquadennium         =   Tqe     = element       ( 7490, "Anti-Triquadennium"    , 349,1215,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentnilium         =   Tpn     = element       ( 7500, "Anti-Tripentnilium"    , 350,1220,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentunium          =   Tpu     = element       ( 7510, "Anti-Tripentunium"     , 351,1225,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentbium           =   Tpb     = element       ( 7520, "Anti-Tripentbium"      , 352,1230,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripenttrium          =   Tpt     = element       ( 7530, "Anti-Tripenttrium"     , 353,1235,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentquadium        =   Tpq     = element       ( 7540, "Anti-Tripentquadium"   , 354,1240,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentpentium        =   Tpp     = element       ( 7550, "Anti-Tripentpentium"   , 355,1245,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripenthexium         =   Tph     = element       ( 7560, "Anti-Tripenthexium"    , 356,1250,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentseptium        =   Tps     = element       ( 7570, "Anti-Tripentseptium"   , 357,1255,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentoctium         =   Tpo     = element       ( 7580, "Anti-Tripentoctium"    , 358,1260,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Tripentennium         =   Tpe     = element       ( 7590, "Anti-Tripentennium"    , 359,1265,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexnilium          =   Thn     = element       ( 7600, "Anti-Trihexnilium"     , 360,1270,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexunium           =   Thu     = element       ( 7610, "Anti-Trihexunium"      , 361,1275,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexbium            =   Thb     = element       ( 7620, "Anti-Trihexbium"       , 362,1280,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihextrium           =   Tht     = element       ( 7630, "Anti-Trihextrium"      , 363,1285,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexquadium         =   Thq     = element       ( 7640, "Anti-Trihexquadium"    , 364,1290,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexpentium         =   Thp     = element       ( 7650, "Anti-Trihexpentium"    , 365,1295,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexhexium          =   Thh     = element       ( 7660, "Anti-Trihexhexium"     , 366,1300,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexseptium         =   Ths     = element       ( 7670, "Anti-Trihexseptium"    , 367,1305,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexoctium          =   Tho     = element       ( 7680, "Anti-Trihexoctium"     , 368,1310,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trihexennium          =   The     = element       ( 7690, "Anti-Trihexennium"     , 369,1315,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptnilium         =   Tsn     = element       ( 7700, "Anti-Triseptnilium"    , 370,1320,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptunium          =   Tsu     = element       ( 7710, "Anti-Triseptunium"     , 371,1325,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Gravitonium           =   Gt      = element       ( 7720, "Anti-Gravitonium"      , 372,1330,   112,  1275, 1768.866761, MT.SET_SHINY            ,   0,  50,   0, 255).put(  CONTAINERS                                                                      );
	}
	private static void reg0013() { // upstream AM.java:457-484
		Tsb = Gt;
		Triseptbium = Gt.addIdenticalNames("Anti-Triseptbium");
		Trisepttrium          =   Tst     = element       ( 7730, "Anti-Trisepttrium"     , 373,1335,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptquadium        =   Tsq     = element       ( 7740, "Anti-Triseptquadium"   , 374,1340,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptpentium        =   Tsp     = element       ( 7750, "Anti-Triseptpentium"   , 375,1345,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trisepthexium         =   Tsh     = element       ( 7760, "Anti-Trisepthexium"    , 376,1350,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptseptium        =   Tss     = element       ( 7770, "Anti-Triseptseptium"   , 377,1355,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptoctium         =   Tso     = element       ( 7780, "Anti-Triseptoctium"    , 378,1360,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triseptennium         =   Tse     = element       ( 7790, "Anti-Triseptennium"    , 379,1365,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctnilium          =   Ton     = element       ( 7800, "Anti-Trioctnilium"     , 380,1370,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctunium           =   Tou     = element       ( 7810, "Anti-Trioctunium"      , 381,1375,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctbium            =   Tob     = element       ( 7820, "Anti-Trioctbium"       , 382,1380,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triocttrium           =   Tot     = element       ( 7830, "Anti-Triocttrium"      , 383,1385,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctquadium         =   Toq     = element       ( 7840, "Anti-Trioctquadium"    , 384,1390,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctpentium         =   Top     = element       ( 7850, "Anti-Trioctpentium"    , 385,1395,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triocthexium          =   Toh     = element       ( 7860, "Anti-Triocthexium"     , 386,1400,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctseptium         =   Tos     = element       ( 7870, "Anti-Trioctseptium"    , 387,1405,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctoctium          =   Too     = element       ( 7880, "Anti-Trioctoctium"     , 388,1410,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trioctennium          =   Toe     = element       ( 7890, "Anti-Trioctennium"     , 389,1415,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennilium           =   Ten     = element       ( 7900, "Anti-Triennilium"      , 390,1420,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennunium           =   Teu     = element       ( 7910, "Anti-Triennunium"      , 391,1425,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennbium            =   Teb     = element       ( 7920, "Anti-Triennbium"       , 392,1430,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Trienntrium           =   Tet     = element       ( 7930, "Anti-Trienntrium"      , 393,1435,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennquadium         =   Teq     = element       ( 7940, "Anti-Triennquadium"    , 394,1440,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennpentium         =   Tep     = element       ( 7950, "Anti-Triennpentium"    , 395,1445,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennhexium          =   Teh     = element       ( 7960, "Anti-Triennhexium"     , 396,1450,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennseptium         =   Tes     = element       ( 7970, "Anti-Triennseptium"    , 397,1455,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennoctium          =   Teo     = element       ( 7980, "Anti-Triennoctium"     , 398,1460,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
		Triennennium          =   Tee     = element       ( 7990, "Anti-Triennennium"     , 399,1465,  1000,  3000,           0, MT.SET_SHINY            , 256, 256, 256, 255).put(                                                                                  );
	}
	/** Port-only entry (upstream loads AM via its class-init, MT.java:1896): drives the registration batches in upstream declaration order. */
	private static boolean INITIALIZED = false;

	public static void init() {
		if (INITIALIZED && !OreDictMaterial.MATERIAL_MAP.isEmpty() && OreDictMaterial.MATERIAL_MAP.get("Anti-Photon") == y) return;
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
	}
}
