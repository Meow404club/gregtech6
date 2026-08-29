/**
 * Ported from GregTech 6 (1.7.10), file gregapi/data/OP.java (upstream 740 lines), by task
 * gt-ore-prefix.
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

import gregapi.code.ICondition;
import gregapi.code.ICondition.And;
import gregapi.code.ICondition.Or;
import gregapi.code.TagData; // And/Or imports = upstream OP.java:23-24
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.PrefixRegistry;

/**
 * @author Gregorius Techneticies
 *
 * List of all OreDict Prefixes. The Short Name is for ease of overview and stands for "OrePrefix".
 *
 * Port notes (task gt-ore-prefix):
 * - BATCH REGISTRATION (red line: no giant static initializer block, cf. MT.java:45): upstream
 *   declares the prefixes as public static final fields initialized by class-init order
 *   (OP.java:54-563) plus one static block (:567-740). Here the fields are assigned by the
 *   private reg*() batches in exactly the upstream declaration order, driven by init() through
 *   OreDictPrefix.createPrefix (OP.java:49-51 helpers verbatim) gated by PrefixRegistry's
 *   open/closed state machine. reset() nulls the handles and re-opens the registry so tests and
 *   the Phase-2 datagen batches run cleanly one after another.
 * - THAUMCRAFT CUT: every .aspects(TC...) segment (OP.java:55-426 etc.) is removed per the
 *   MC-coupling policy; mAspects lives on Phase-2 OreDictPrefix work only.
 * - OreDictMaterialCondition NOT ported (outside FILES_SCOPE): the prefixes whose condition
 *   needs meltmin/qualmin/typemin are deferred, i.e. ingotHot (:166) and the toolHead* / tool*
 *   families (:232-276), 47 entries total.
 * - MT/ANY-dependent blocks of the static initializer are deferred to the dataset card
 *   (gt-material-dataset) or later: washing listeners (:569-572, MC items), bottle.mContainerItem
 *   (:574, ItemStack), disableItemGeneration/forceItemGeneration tables (:603-625, MT.*), and the
 *   whole mByProducts data block (:639-740, OM.stack(MT.* / ANY.*)). The prefix model (mByProducts,
 *   byproduct(int), disableItemGeneration, forceItemGeneration) is fully in place.
 * - The familiar-prefix loops (:578-580, :584-600) and the mPriorityPrefixIndex -> mPriorityPrefix
 *   mapping (:627-636) are pure data/logic and are ported (regFamiliarPrefixes, applyPriorityPrefixes).
 * - TD tag seam: the TD.Prefix / TD.ItemGenerator / TD.Properties / TD.Processing / TD.Creative
 *   constants referenced here are not ported yet (TD.java is outside this card's FILES_SCOPE), so
 *   they are created below with the exact upstream keys and display names via the idempotent
 *   TagData.createTagData factory (TagData.java:77-82); once the real TD groups land they unify
 *   into the very same instances (same pattern as OreDictMaterial.TDG from gt-material-model).
 */
public class OP {
	/** Phase-1 tag seam — upstream keys/display names from TD.java; see class javadoc. */
	private static final TagData ORES = TagData.createTagData("ITEMGENERATOR.ORES");
	private static final TagData ORE = TagData.createTagData("PREFIX.ORE", "Ore");
	private static final TagData TOOLTIP_ENCHANTS = TagData.createTagData("PREFIX.TOOLTIP_ENCHANTS", "Enchantment Tooltip");
	private static final TagData STANDARD_ORE = TagData.createTagData("PREFIX.STANDARD_ORE", "Standard Ore");
	private static final TagData BLOCK_BASED = TagData.createTagData("PREFIX.BLOCK_BASED", "Block Based");
	private static final TagData DUST_ORE = TagData.createTagData("PREFIX.DUST_ORE", "Dust Ore");
	private static final TagData DENSE_ORE = TagData.createTagData("PREFIX.DENSE_ORE", "Dense Ore");
	private static final TagData UNIFICATABLE = TagData.createTagData("PREFIX.UNIFICATABLE", "Unificatable");
	private static final TagData BURNABLE = TagData.createTagData("PREFIX.BURNABLE", "Burnable");
	private static final TagData RECYCLABLE = TagData.createTagData("PREFIX.RECYCLABLE", "Recyclable");
	private static final TagData ORE_PROCESSING_BASED = TagData.createTagData("PREFIX.ORE_PROCESSING_BASED", "Ore Processing Based");
	private static final TagData ORE_PROCESSING_DIRTY = TagData.createTagData("PREFIX.ORE_PROCESSING_DIRTY", "Ore Processing Dirty");
	private static final TagData ORE_PROCESSING_CLEAN = TagData.createTagData("PREFIX.ORE_PROCESSING_CLEAN", "Ore Processing Clean");
	private static final TagData ORE_PROCESSING_REFINED = TagData.createTagData("PREFIX.ORE_PROCESSING_REFINED", "Ore Processing Refined");
	private static final TagData STONE = TagData.createTagData("PROPERTIES.STONE", "Stone");
	private static final TagData DUSTS = TagData.createTagData("ITEMGENERATOR.DUSTS");
	private static final TagData DIRTY_DUSTS = TagData.createTagData("ITEMGENERATOR.DIRTY_DUSTS");
	private static final TagData SIMPLIFIABLE = TagData.createTagData("PREFIX.SIMPLIFIABLE", "Simplifiable");
	private static final TagData DUST_BASED = TagData.createTagData("PREFIX.DUST_BASED", "Dust");
	private static final TagData SCANNABLE = TagData.createTagData("PREFIX.SCANNABLE", "Scannable");
	private static final TagData EXTRUDER_FODDER = TagData.createTagData("PREFIX.EXTRUDER_FODDER", "Extruder Fodder");
	private static final TagData TOOLTIP_MATERIAL = TagData.createTagData("PREFIX.TOOLTIP_MATERIAL", "Material Tooltip");
	private static final TagData MULTIINGOTS = TagData.createTagData("ITEMGENERATOR.MULTIINGOTS");
	private static final TagData INGOTS = TagData.createTagData("ITEMGENERATOR.INGOTS");
	private static final TagData INGOT_BASED = TagData.createTagData("PREFIX.INGOT_BASED", "Ingot");
	private static final TagData GEMS = TagData.createTagData("ITEMGENERATOR.GEMS");
	private static final TagData SELF_REFERENCING = TagData.createTagData("PREFIX.SELF_REFERENCING", "Self Referencing");
	private static final TagData GEM_BASED = TagData.createTagData("PREFIX.GEM_BASED", "Gem");
	private static final TagData TRANSPARENT = TagData.createTagData("PROPERTIES.TRANSPARENT", "Transparent");
	private static final TagData CRYSTAL = TagData.createTagData("PROPERTIES.CRYSTAL", "Crystal");
	private static final TagData PEARL = TagData.createTagData("PROPERTIES.PEARL", "Pearl");
	private static final TagData LENSES = TagData.createTagData("ITEMGENERATOR.LENSES");
	private static final TagData HIDDEN = TagData.createTagData("NEI.HIDDEN", "Hidden");
	private static final TagData PLATES = TagData.createTagData("ITEMGENERATOR.PLATES");
	private static final TagData DENSEPLATES = TagData.createTagData("ITEMGENERATOR.DENSEPLATES");
	private static final TagData MULTIPLATES = TagData.createTagData("ITEMGENERATOR.MULTIPLATES");
	private static final TagData UNIFICATABLE_RECIPES = TagData.createTagData("PREFIX.UNIFICATABLE_RECIPES", "Recipe Unificatable");
	private static final TagData FOILS = TagData.createTagData("ITEMGENERATOR.FOILS");
	private static final TagData STICKS = TagData.createTagData("ITEMGENERATOR.STICKS");
	private static final TagData PARTS = TagData.createTagData("ITEMGENERATOR.PARTS");
	private static final TagData PROJECTILES = TagData.createTagData("ITEMGENERATOR.PROJECTILES");
	private static final TagData STRETCHY = TagData.createTagData("PROPERTIES.STRETCHY", "Stretchy");
	private static final TagData BOUNCY = TagData.createTagData("PROPERTIES.BOUNCY", "Bouncy");
	private static final TagData BRITTLE = TagData.createTagData("PROPERTIES.BRITTLE", "Brittle");
	private static final TagData WIRES = TagData.createTagData("ITEMGENERATOR.WIRES");
	private static final TagData SMITHABLE = TagData.createTagData("PROCESSING.SMITHABLE", "Smithable");
	private static final TagData RAILS = TagData.createTagData("ITEMGENERATOR.RAILS");
	private static final TagData REACTS_WITH_GLASS = TagData.createTagData("PROCESSING.REACTS_WITH_GLASS", "Reacts with Glass");
	private static final TagData IS_CONTAINER = TagData.createTagData("PREFIX.IS_CONTAINER", "Container");
	private static final TagData CONTAINERS = TagData.createTagData("ITEMGENERATOR.CONTAINERS");
	private static final TagData EMPTY = TagData.createTagData("ITEMGENERATOR.EMPTY");
	private static final TagData CONTAINERS_FLUID = TagData.createTagData("ITEMGENERATOR.CONTAINERS_FLUID");
	private static final TagData CONTAINERS_GAS = TagData.createTagData("ITEMGENERATOR.CONTAINERS_GAS");
	private static final TagData MATERIAL_BASED = TagData.createTagData("PREFIX.MATERIAL_BASED", "Material Based");
	private static final TagData AMMO_ALIKE = TagData.createTagData("PREFIX.AMMO_ALIKE", "Ammo");
	private static final TagData ARMORS = TagData.createTagData("ITEMGENERATOR.ARMORS");
	private static final TagData ARMOR_ALIKE = TagData.createTagData("PREFIX.ARMOR_ALIKE", "Armor");
	private static final TagData PIPES = TagData.createTagData("ITEMGENERATOR.PIPES");
	private static final TagData WIRE_BASED = TagData.createTagData("PREFIX.WIRE_BASED", "Wire");
	private static final TagData STORAGE_BASED = TagData.createTagData("PREFIX.STORAGE_BASED", "Storage Based");
	private static final TagData IS_CRATE = TagData.createTagData("PREFIX.IS_CRATE", "Crate");
	private static final TagData PLANTS = TagData.createTagData("ITEMGENERATOR.PLANTS");
	private static final TagData PLANT_DROP = TagData.createTagData("PREFIX.PLANT_DROP", "Plant Drop");
	private static final TagData NO_PREFIX_FILTERING = TagData.createTagData("PREFIX.NO_PREFIX_FILTERING", "Not Prefix Filterable");
	private static final TagData TOOL_ALIKE = TagData.createTagData("PREFIX.TOOL_ALIKE", "Tool");
	private static final TagData PREFIX_UNUSED = TagData.createTagData("PREFIX.PREFIX_UNUSED", "Unused Prefix");

	// Upstream :54-563 as non-final fields (assigned by the reg* batches, see class javadoc).
	public static OreDictPrefix ore;
	public static OreDictPrefix oreBlackgranite;
	public static OreDictPrefix oreRedgranite;
	public static OreDictPrefix oreVanillastone;
	public static OreDictPrefix oreVanillagranite;
	public static OreDictPrefix oreAndesite;
	public static OreDictPrefix oreDiorite;
	public static OreDictPrefix oreDeepslate;
	public static OreDictPrefix oreBlackstone;
	public static OreDictPrefix oreMoon;
	public static OreDictPrefix oreMars;
	public static OreDictPrefix oreSpace;
	public static OreDictPrefix orePhobos;
	public static OreDictPrefix oreDeimos;
	public static OreDictPrefix oreVenus;
	public static OreDictPrefix oreMercury;
	public static OreDictPrefix oreCeres;
	public static OreDictPrefix oreJupiter;
	public static OreDictPrefix oreIo;
	public static OreDictPrefix oreEuropa;
	public static OreDictPrefix oreGanymede;
	public static OreDictPrefix oreCallisto;
	public static OreDictPrefix oreSaturn;
	public static OreDictPrefix oreRhea;
	public static OreDictPrefix oreTitan;
	public static OreDictPrefix oreOberon;
	public static OreDictPrefix oreIapetus;
	public static OreDictPrefix oreUranus;
	public static OreDictPrefix oreTitania;
	public static OreDictPrefix oreNeptune;
	public static OreDictPrefix oreTriton;
	public static OreDictPrefix orePluto;
	public static OreDictPrefix oreEris;
	public static OreDictPrefix oreKepler22b;
	public static OreDictPrefix oreHolystone;
	public static OreDictPrefix oreLivingrock;
	public static OreDictPrefix oreDeadrock;
	public static OreDictPrefix oreBetweenstone;
	public static OreDictPrefix orePitstone;
	public static OreDictPrefix oreUmberstone;
	public static OreDictPrefix oreKomatiite;
	public static OreDictPrefix oreBasalt;
	public static OreDictPrefix oreMarble;
	public static OreDictPrefix oreLimestone;
	public static OreDictPrefix oreSiltstone;
	public static OreDictPrefix oreShale;
	public static OreDictPrefix oreSlate;
	public static OreDictPrefix oreGreenschist;
	public static OreDictPrefix oreBlueschist;
	public static OreDictPrefix orePinkschist;
	public static OreDictPrefix oreGrayschist;
	public static OreDictPrefix oreGneiss;
	public static OreDictPrefix oreLightprismarine;
	public static OreDictPrefix oreDarkprismarine;
	public static OreDictPrefix oreKimberlite;
	public static OreDictPrefix oreQuartzite;
	public static OreDictPrefix oreNetherrack;
	public static OreDictPrefix oreEndstone;
	public static OreDictPrefix oreSandstone;
	public static OreDictPrefix oreGravel;
	public static OreDictPrefix oreStrangesand;
	public static OreDictPrefix oreRedSand;
	public static OreDictPrefix oreSand;
	public static OreDictPrefix oreMud;
	public static OreDictPrefix oreBedrock;
	public static OreDictPrefix oreNether;
	public static OreDictPrefix oreDense;
	public static OreDictPrefix oreEnd;
	public static OreDictPrefix oreHee;
	public static OreDictPrefix oreRich;
	public static OreDictPrefix oreNormal;
	public static OreDictPrefix oreSmall;
	public static OreDictPrefix orePoor;
	public static OreDictPrefix oreRaw;
	public static OreDictPrefix crushed;
	public static OreDictPrefix crushedTiny;
	public static OreDictPrefix crushedPurified;
	public static OreDictPrefix crushedPurifiedTiny;
	public static OreDictPrefix crushedCentrifuged;
	public static OreDictPrefix crushedCentrifugedTiny;
	public static OreDictPrefix rockGt;
	public static OreDictPrefix rawOreChunk;
	public static OreDictPrefix clump;
	public static OreDictPrefix cluster;
	public static OreDictPrefix pebbles;
	public static OreDictPrefix rubble;
	public static OreDictPrefix chunk;
	public static OreDictPrefix crystalline;
	public static OreDictPrefix reduced;
	public static OreDictPrefix cleanGravel;
	public static OreDictPrefix dirtyGravel;
	public static OreDictPrefix dust;
	public static OreDictPrefix dustSmall;
	public static OreDictPrefix dustTiny;
	public static OreDictPrefix dustDiv72;
	public static OreDictPrefix dustImpure;
	public static OreDictPrefix dustPure;
	public static OreDictPrefix dustRefined;
	public static OreDictPrefix ingotQuintuple;
	public static OreDictPrefix ingotQuadruple;
	public static OreDictPrefix ingotTriple;
	public static OreDictPrefix ingotDouble;
	public static OreDictPrefix ingot;
	public static OreDictPrefix billet;
	public static OreDictPrefix chunkGt;
	public static OreDictPrefix nugget;
	public static OreDictPrefix gem;
	public static OreDictPrefix gemChipped;
	public static OreDictPrefix gemFlawed;
	public static OreDictPrefix gemFlawless;
	public static OreDictPrefix gemExquisite;
	public static OreDictPrefix gemLegendary;
	public static OreDictPrefix gemOre;
	public static OreDictPrefix gemRaw;
	public static OreDictPrefix gemUncut;
	public static OreDictPrefix gemPolished;
	public static OreDictPrefix bouleGt;
	public static OreDictPrefix crystalPure;
	public static OreDictPrefix crystal;
	public static OreDictPrefix lens;
	public static OreDictPrefix scrapGt;
	public static OreDictPrefix plateSteamcraft;
	public static OreDictPrefix plateDense;
	public static OreDictPrefix plateQuintuple;
	public static OreDictPrefix plateQuadruple;
	public static OreDictPrefix plateTriple;
	public static OreDictPrefix plateDouble;
	public static OreDictPrefix plate;
	public static OreDictPrefix plateGem;
	public static OreDictPrefix plateTiny;
	public static OreDictPrefix plateGemTiny;
	public static OreDictPrefix plateCurved;
	public static OreDictPrefix compressed;
	public static OreDictPrefix sheetGt;
	public static OreDictPrefix foil;
	public static OreDictPrefix stick;
	public static OreDictPrefix stickLong;
	public static OreDictPrefix bolt;
	public static OreDictPrefix screw;
	public static OreDictPrefix round;
	public static OreDictPrefix ring;
	public static OreDictPrefix chain;
	public static OreDictPrefix spring;
	public static OreDictPrefix springSmall;
	public static OreDictPrefix wireFine;
	public static OreDictPrefix minecartWheels;
	public static OreDictPrefix gearGt;
	public static OreDictPrefix gearGtSmall;
	public static OreDictPrefix railGt;
	public static OreDictPrefix casingSmall;
	public static OreDictPrefix casingMachine;
	public static OreDictPrefix casingMachineDouble;
	public static OreDictPrefix casingMachineQuadruple;
	public static OreDictPrefix casingMachineDense;
	public static OreDictPrefix rotor;
	public static OreDictPrefix chemtube;
	public static OreDictPrefix cell;
	public static OreDictPrefix bucket;
	public static OreDictPrefix bottle;
	public static OreDictPrefix capsule;
	public static OreDictPrefix bulletGtSmall;
	public static OreDictPrefix bulletGtMedium;
	public static OreDictPrefix bulletGtLarge;
	public static OreDictPrefix armorHelmet;
	public static OreDictPrefix armorChestplate;
	public static OreDictPrefix armorLeggings;
	public static OreDictPrefix armorBoots;
	public static OreDictPrefix armor;
	public static OreDictPrefix frameGt;
	public static OreDictPrefix capcellcon;
	public static OreDictPrefix pipeTiny;
	public static OreDictPrefix pipeSmall;
	public static OreDictPrefix pipeMedium;
	public static OreDictPrefix pipeLarge;
	public static OreDictPrefix pipeHuge;
	public static OreDictPrefix pipeQuadruple;
	public static OreDictPrefix pipeNonuple;
	public static OreDictPrefix pipeRestrictiveTiny;
	public static OreDictPrefix pipeRestrictiveSmall;
	public static OreDictPrefix pipeRestrictiveMedium;
	public static OreDictPrefix pipeRestrictiveLarge;
	public static OreDictPrefix pipeRestrictiveHuge;
	public static OreDictPrefix pipe;
	public static OreDictPrefix wireGt16;
	public static OreDictPrefix wireGt15;
	public static OreDictPrefix wireGt14;
	public static OreDictPrefix wireGt13;
	public static OreDictPrefix wireGt12;
	public static OreDictPrefix wireGt11;
	public static OreDictPrefix wireGt10;
	public static OreDictPrefix wireGt09;
	public static OreDictPrefix wireGt08;
	public static OreDictPrefix wireGt07;
	public static OreDictPrefix wireGt06;
	public static OreDictPrefix wireGt05;
	public static OreDictPrefix wireGt04;
	public static OreDictPrefix wireGt03;
	public static OreDictPrefix wireGt02;
	public static OreDictPrefix wireGt01;
	public static OreDictPrefix cableGt12;
	public static OreDictPrefix cableGt08;
	public static OreDictPrefix cableGt04;
	public static OreDictPrefix cableGt02;
	public static OreDictPrefix cableGt01;
	public static OreDictPrefix crateGtRaw;
	public static OreDictPrefix crateGtGem;
	public static OreDictPrefix crateGtDust;
	public static OreDictPrefix crateGtIngot;
	public static OreDictPrefix crateGtPlate;
	public static OreDictPrefix crateGtPlateGem;
	public static OreDictPrefix crateGt64Raw;
	public static OreDictPrefix crateGt64Gem;
	public static OreDictPrefix crateGt64Dust;
	public static OreDictPrefix crateGt64Ingot;
	public static OreDictPrefix crateGt64Plate;
	public static OreDictPrefix crateGt64PlateGem;
	public static OreDictPrefix blockRaw;
	public static OreDictPrefix blockGem;
	public static OreDictPrefix blockDust;
	public static OreDictPrefix blockIngot;
	public static OreDictPrefix blockPlate;
	public static OreDictPrefix blockPlateGem;
	public static OreDictPrefix blockSolid;
	public static OreDictPrefix orebush;
	public static OreDictPrefix oreberry;
	public static OreDictPrefix plantGtBerry;
	public static OreDictPrefix plantGtTwig;
	public static OreDictPrefix plantGtFiber;
	public static OreDictPrefix plantGtWart;
	public static OreDictPrefix plantGtBlossom;
	public static OreDictPrefix compressedCobblestone;
	public static OreDictPrefix compressedStone;
	public static OreDictPrefix compressedDirt;
	public static OreDictPrefix compressedGravel;
	public static OreDictPrefix compressedSand;
	public static OreDictPrefix blockBamboo;
	public static OreDictPrefix blockGlass;
	public static OreDictPrefix blockWool;
	public static OreDictPrefix block_;
	public static OreDictPrefix block;
	public static OreDictPrefix item_;
	public static OreDictPrefix item;
	public static OreDictPrefix glass;
	public static OreDictPrefix paneGlass;
	public static OreDictPrefix stainedClay;
	public static OreDictPrefix craftingTool;
	public static OreDictPrefix crafting;
	public static OreDictPrefix craft;
	public static OreDictPrefix slab;
	public static OreDictPrefix stair;
	public static OreDictPrefix fence;
	public static OreDictPrefix treeSapling;
	public static OreDictPrefix treeLeaves;
	public static OreDictPrefix tree;
	public static OreDictPrefix log;
	public static OreDictPrefix beam;
	public static OreDictPrefix plank;
	public static OreDictPrefix stoneCobble;
	public static OreDictPrefix stoneSmooth;
	public static OreDictPrefix stoneMossyBricks;
	public static OreDictPrefix stoneMossy;
	public static OreDictPrefix stoneBricks;
	public static OreDictPrefix stoneCracked;
	public static OreDictPrefix stoneChiseled;
	public static OreDictPrefix stonePolished;
	public static OreDictPrefix stone;
	public static OreDictPrefix cobblestone;
	public static OreDictPrefix rock;
	public static OreDictPrefix record;
	public static OreDictPrefix scraps;
	public static OreDictPrefix scrap;
	public static OreDictPrefix book;
	public static OreDictPrefix paper;
	public static OreDictPrefix dye;
	public static OreDictPrefix dyeMixable;
	public static OreDictPrefix dyeCeramic;
	public static OreDictPrefix batterySingleuse;
	public static OreDictPrefix battery;
	public static OreDictPrefix circuit;
	public static OreDictPrefix computer;
	public static OreDictPrefix shard;
	public static OreDictPrefix sand;
	public static OreDictPrefix wire;
	public static OreDictPrefix lamp;
	public static OreDictPrefix cloth;
	public static OreDictPrefix fabric;
	public static OreDictPrefix quartz;
	public static OreDictPrefix part;
	public static OreDictPrefix torch;
	public static OreDictPrefix skull;
	public static OreDictPrefix plating;
	public static OreDictPrefix dinosaur;
	public static OreDictPrefix travelgear;
	public static OreDictPrefix bauble;
	public static OreDictPrefix grafter;
	public static OreDictPrefix scoop;
	public static OreDictPrefix frame;
	public static OreDictPrefix tome;
	public static OreDictPrefix junk;
	public static OreDictPrefix bee;
	public static OreDictPrefix rod;
	public static OreDictPrefix dirt;
	public static OreDictPrefix grass;
	public static OreDictPrefix gravel;
	public static OreDictPrefix mushroom;
	public static OreDictPrefix wood;
	public static OreDictPrefix drop;
	public static OreDictPrefix fuel;
	public static OreDictPrefix panel;
	public static OreDictPrefix brick;
	public static OreDictPrefix seed;
	public static OreDictPrefix reed;
	public static OreDictPrefix sheetDouble;
	public static OreDictPrefix sheet;
	public static OreDictPrefix crop;
	public static OreDictPrefix plant;
	public static OreDictPrefix coin;
	public static OreDictPrefix lumar;
	public static OreDictPrefix ground;
	public static OreDictPrefix cable;
	public static OreDictPrefix component;
	public static OreDictPrefix pole;
	public static OreDictPrefix desert;
	public static OreDictPrefix jungle;
	public static OreDictPrefix savanna;
	public static OreDictPrefix beach;
	public static OreDictPrefix forest;
	public static OreDictPrefix mountain;
	public static OreDictPrefix plains;
	public static OreDictPrefix epiphyte;
	public static OreDictPrefix water;
	public static OreDictPrefix river;
	public static OreDictPrefix ocean;
	public static OreDictPrefix hanging;
	public static OreDictPrefix floating;
	public static OreDictPrefix wetlands;
	public static OreDictPrefix fern;
	public static OreDictPrefix vine;
	public static OreDictPrefix fungus;
	public static OreDictPrefix cactus;
	public static OreDictPrefix bud;
	public static OreDictPrefix immersed;
	public static OreDictPrefix bamboo;
	public static OreDictPrefix cones;
	public static OreDictPrefix consumable;
	public static OreDictPrefix leafy;
	public static OreDictPrefix leaf;
	public static OreDictPrefix shrub;
	public static OreDictPrefix berrybush;
	public static OreDictPrefix wax;
	public static OreDictPrefix wall;
	public static OreDictPrefix tube;
	public static OreDictPrefix list;
	public static OreDictPrefix food;
	public static OreDictPrefix gear;
	public static OreDictPrefix coral;
	public static OreDictPrefix flower;
	public static OreDictPrefix storage;
	public static OreDictPrefix material;
	public static OreDictPrefix plasma;
	public static OreDictPrefix element;
	public static OreDictPrefix molecule;
	public static OreDictPrefix wafer;
	public static OreDictPrefix orb;
	public static OreDictPrefix handle;
	public static OreDictPrefix blade;
	public static OreDictPrefix head;
	public static OreDictPrefix motor;
	public static OreDictPrefix bowl;
	public static OreDictPrefix bit;
	public static OreDictPrefix shears;
	public static OreDictPrefix turbine;
	public static OreDictPrefix fertilizer;
	public static OreDictPrefix chest;
	public static OreDictPrefix raw;
	public static OreDictPrefix stainedGlass;
	public static OreDictPrefix mystic;
	public static OreDictPrefix mana;
	public static OreDictPrefix rune;
	public static OreDictPrefix petal;
	public static OreDictPrefix pearl;
	public static OreDictPrefix powder;
	public static OreDictPrefix soulsand;
	public static OreDictPrefix obsidian;
	public static OreDictPrefix glowstone;
	public static OreDictPrefix beans;
	public static OreDictPrefix essence;
	public static OreDictPrefix alloy;
	public static OreDictPrefix cooking;
	public static OreDictPrefix gate;
	public static OreDictPrefix ladder;
	public static OreDictPrefix door;
	public static OreDictPrefix trapdoor;
	public static OreDictPrefix elven;
	public static OreDictPrefix reactor;
	public static OreDictPrefix mffs;
	public static OreDictPrefix projred;
	public static OreDictPrefix ganys;
	public static OreDictPrefix liquid;
	public static OreDictPrefix chipset;
	public static OreDictPrefix boule;
	public static OreDictPrefix lump;
	public static OreDictPrefix pellet;
	public static OreDictPrefix tiny;
	public static OreDictPrefix bars;
	public static OreDictPrefix bar;

	public static OreDictPrefix[] wireGt;
	public static OreDictPrefix[] array_dust_ingot;
	public static OreDictPrefix[] array_dust_ingot_plate;
	public static OreDictPrefix[] array_dust_ingot_plate_gem;
	public static OreDictPrefix[] array_ingot_plate;
	public static OreDictPrefix[] array_ingot_plate_gem;
	public static OreDictPrefix[] array_ingot_gem;

	private static boolean mInitialized = F;

	private static OreDictPrefix create(String aName, String aCategory, String aPreMaterial, String aPostMaterial) {return OreDictPrefix.createPrefix(aName).setCategoryName(aCategory).setLocalPrefixName(aCategory).setLocalItemName(aPreMaterial, aPostMaterial);} // :49
	private static OreDictPrefix create(String aName, String aCategory) {return OreDictPrefix.createPrefix(aName).setCategoryName(aCategory).setLocalPrefixName(aCategory);} // :50
	private static OreDictPrefix unused(String aName) {return OreDictPrefix.createPrefix(aName).add(PREFIX_UNUSED);} // :51

	/** Registration entry. Idempotent via mInitialized; requires PrefixRegistry to be open (upstream createPrefix gate at OreDictPrefix.java:93). */
	public static void init() {
		if (mInitialized) return;
		mInitialized = T;
		regOres(); regCrushed(); regDusts(); regIngots(); regGems(); regPlates(); regParts(); regContainers(); regArmor(); regPipes(); regWires(); regCrates(); regBlocks(); regPlants(); regMisc(); regDyes(); regElectric(); regUnused();
		regArrays();
		regFamiliarPrefixes();
		applyPriorityPrefixes();
	}

	/** Wipes the prefix handles and re-opens the underlying registry (upstream has no reset; the registry state machine is the Phase-1 replacement for GAPI.mStartedInit). */
	public static void reset() {
		mInitialized = F;
		ore = null;
		oreBlackgranite = null;
		oreRedgranite = null;
		oreVanillastone = null;
		oreVanillagranite = null;
		oreAndesite = null;
		oreDiorite = null;
		oreDeepslate = null;
		oreBlackstone = null;
		oreMoon = null;
		oreMars = null;
		oreSpace = null;
		orePhobos = null;
		oreDeimos = null;
		oreVenus = null;
		oreMercury = null;
		oreCeres = null;
		oreJupiter = null;
		oreIo = null;
		oreEuropa = null;
		oreGanymede = null;
		oreCallisto = null;
		oreSaturn = null;
		oreRhea = null;
		oreTitan = null;
		oreOberon = null;
		oreIapetus = null;
		oreUranus = null;
		oreTitania = null;
		oreNeptune = null;
		oreTriton = null;
		orePluto = null;
		oreEris = null;
		oreKepler22b = null;
		oreHolystone = null;
		oreLivingrock = null;
		oreDeadrock = null;
		oreBetweenstone = null;
		orePitstone = null;
		oreUmberstone = null;
		oreKomatiite = null;
		oreBasalt = null;
		oreMarble = null;
		oreLimestone = null;
		oreSiltstone = null;
		oreShale = null;
		oreSlate = null;
		oreGreenschist = null;
		oreBlueschist = null;
		orePinkschist = null;
		oreGrayschist = null;
		oreGneiss = null;
		oreLightprismarine = null;
		oreDarkprismarine = null;
		oreKimberlite = null;
		oreQuartzite = null;
		oreNetherrack = null;
		oreEndstone = null;
		oreSandstone = null;
		oreGravel = null;
		oreStrangesand = null;
		oreRedSand = null;
		oreSand = null;
		oreMud = null;
		oreBedrock = null;
		oreNether = null;
		oreDense = null;
		oreEnd = null;
		oreHee = null;
		oreRich = null;
		oreNormal = null;
		oreSmall = null;
		orePoor = null;
		oreRaw = null;
		crushed = null;
		crushedTiny = null;
		crushedPurified = null;
		crushedPurifiedTiny = null;
		crushedCentrifuged = null;
		crushedCentrifugedTiny = null;
		rockGt = null;
		rawOreChunk = null;
		clump = null;
		cluster = null;
		pebbles = null;
		rubble = null;
		chunk = null;
		crystalline = null;
		reduced = null;
		cleanGravel = null;
		dirtyGravel = null;
		dust = null;
		dustSmall = null;
		dustTiny = null;
		dustDiv72 = null;
		dustImpure = null;
		dustPure = null;
		dustRefined = null;
		ingotQuintuple = null;
		ingotQuadruple = null;
		ingotTriple = null;
		ingotDouble = null;
		ingot = null;
		billet = null;
		chunkGt = null;
		nugget = null;
		gem = null;
		gemChipped = null;
		gemFlawed = null;
		gemFlawless = null;
		gemExquisite = null;
		gemLegendary = null;
		gemOre = null;
		gemRaw = null;
		gemUncut = null;
		gemPolished = null;
		bouleGt = null;
		crystalPure = null;
		crystal = null;
		lens = null;
		scrapGt = null;
		plateSteamcraft = null;
		plateDense = null;
		plateQuintuple = null;
		plateQuadruple = null;
		plateTriple = null;
		plateDouble = null;
		plate = null;
		plateGem = null;
		plateTiny = null;
		plateGemTiny = null;
		plateCurved = null;
		compressed = null;
		sheetGt = null;
		foil = null;
		stick = null;
		stickLong = null;
		bolt = null;
		screw = null;
		round = null;
		ring = null;
		chain = null;
		spring = null;
		springSmall = null;
		wireFine = null;
		minecartWheels = null;
		gearGt = null;
		gearGtSmall = null;
		railGt = null;
		casingSmall = null;
		casingMachine = null;
		casingMachineDouble = null;
		casingMachineQuadruple = null;
		casingMachineDense = null;
		rotor = null;
		chemtube = null;
		cell = null;
		bucket = null;
		bottle = null;
		capsule = null;
		bulletGtSmall = null;
		bulletGtMedium = null;
		bulletGtLarge = null;
		armorHelmet = null;
		armorChestplate = null;
		armorLeggings = null;
		armorBoots = null;
		armor = null;
		frameGt = null;
		capcellcon = null;
		pipeTiny = null;
		pipeSmall = null;
		pipeMedium = null;
		pipeLarge = null;
		pipeHuge = null;
		pipeQuadruple = null;
		pipeNonuple = null;
		pipeRestrictiveTiny = null;
		pipeRestrictiveSmall = null;
		pipeRestrictiveMedium = null;
		pipeRestrictiveLarge = null;
		pipeRestrictiveHuge = null;
		pipe = null;
		wireGt16 = null;
		wireGt15 = null;
		wireGt14 = null;
		wireGt13 = null;
		wireGt12 = null;
		wireGt11 = null;
		wireGt10 = null;
		wireGt09 = null;
		wireGt08 = null;
		wireGt07 = null;
		wireGt06 = null;
		wireGt05 = null;
		wireGt04 = null;
		wireGt03 = null;
		wireGt02 = null;
		wireGt01 = null;
		cableGt12 = null;
		cableGt08 = null;
		cableGt04 = null;
		cableGt02 = null;
		cableGt01 = null;
		crateGtRaw = null;
		crateGtGem = null;
		crateGtDust = null;
		crateGtIngot = null;
		crateGtPlate = null;
		crateGtPlateGem = null;
		crateGt64Raw = null;
		crateGt64Gem = null;
		crateGt64Dust = null;
		crateGt64Ingot = null;
		crateGt64Plate = null;
		crateGt64PlateGem = null;
		blockRaw = null;
		blockGem = null;
		blockDust = null;
		blockIngot = null;
		blockPlate = null;
		blockPlateGem = null;
		blockSolid = null;
		orebush = null;
		oreberry = null;
		plantGtBerry = null;
		plantGtTwig = null;
		plantGtFiber = null;
		plantGtWart = null;
		plantGtBlossom = null;
		compressedCobblestone = null;
		compressedStone = null;
		compressedDirt = null;
		compressedGravel = null;
		compressedSand = null;
		blockBamboo = null;
		blockGlass = null;
		blockWool = null;
		block_ = null;
		block = null;
		item_ = null;
		item = null;
		glass = null;
		paneGlass = null;
		stainedClay = null;
		craftingTool = null;
		crafting = null;
		craft = null;
		slab = null;
		stair = null;
		fence = null;
		treeSapling = null;
		treeLeaves = null;
		tree = null;
		log = null;
		beam = null;
		plank = null;
		stoneCobble = null;
		stoneSmooth = null;
		stoneMossyBricks = null;
		stoneMossy = null;
		stoneBricks = null;
		stoneCracked = null;
		stoneChiseled = null;
		stonePolished = null;
		stone = null;
		cobblestone = null;
		rock = null;
		record = null;
		scraps = null;
		scrap = null;
		book = null;
		paper = null;
		dye = null;
		dyeMixable = null;
		dyeCeramic = null;
		batterySingleuse = null;
		battery = null;
		circuit = null;
		computer = null;
		shard = null;
		sand = null;
		wire = null;
		lamp = null;
		cloth = null;
		fabric = null;
		quartz = null;
		part = null;
		torch = null;
		skull = null;
		plating = null;
		dinosaur = null;
		travelgear = null;
		bauble = null;
		grafter = null;
		scoop = null;
		frame = null;
		tome = null;
		junk = null;
		bee = null;
		rod = null;
		dirt = null;
		grass = null;
		gravel = null;
		mushroom = null;
		wood = null;
		drop = null;
		fuel = null;
		panel = null;
		brick = null;
		seed = null;
		reed = null;
		sheetDouble = null;
		sheet = null;
		crop = null;
		plant = null;
		coin = null;
		lumar = null;
		ground = null;
		cable = null;
		component = null;
		pole = null;
		desert = null;
		jungle = null;
		savanna = null;
		beach = null;
		forest = null;
		mountain = null;
		plains = null;
		epiphyte = null;
		water = null;
		river = null;
		ocean = null;
		hanging = null;
		floating = null;
		wetlands = null;
		fern = null;
		vine = null;
		fungus = null;
		cactus = null;
		bud = null;
		immersed = null;
		bamboo = null;
		cones = null;
		consumable = null;
		leafy = null;
		leaf = null;
		shrub = null;
		berrybush = null;
		wax = null;
		wall = null;
		tube = null;
		list = null;
		food = null;
		gear = null;
		coral = null;
		flower = null;
		storage = null;
		material = null;
		plasma = null;
		element = null;
		molecule = null;
		wafer = null;
		orb = null;
		handle = null;
		blade = null;
		head = null;
		motor = null;
		bowl = null;
		bit = null;
		shears = null;
		turbine = null;
		fertilizer = null;
		chest = null;
		raw = null;
		stainedGlass = null;
		mystic = null;
		mana = null;
		rune = null;
		petal = null;
		pearl = null;
		powder = null;
		soulsand = null;
		obsidian = null;
		glowstone = null;
		beans = null;
		essence = null;
		alloy = null;
		cooking = null;
		gate = null;
		ladder = null;
		door = null;
		trapdoor = null;
		elven = null;
		reactor = null;
		mffs = null;
		projred = null;
		ganys = null;
		liquid = null;
		chipset = null;
		boule = null;
		lump = null;
		pellet = null;
		tiny = null;
		bars = null;
		bar = null;
		wireGt = null;
		array_dust_ingot = null;
		array_dust_ingot_plate = null;
		array_dust_ingot_plate_gem = null;
		array_ingot_plate = null;
		array_ingot_plate_gem = null;
		array_ingot_gem = null;
		PrefixRegistry.INSTANCE.reset();
	}

	private static void regOres() {
	ore = create("ore"                          , "Ores"                            , ""                                , " Ore").setCondition(ORES).add(ORE, TOOLTIP_ENCHANTS, STANDARD_ORE).setTextureSetName("ore").addIdenticalNames("oreGem"); // Regular Ore Prefix. Ore -> Material is a One-Way Operation! Introduced by Eloraam
	oreBlackgranite = create("oreBlackgranite"              , "Black Granite Ores"              , "Granite "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreRedgranite = create("oreRedgranite"                , "Red Granite Ores"                , "Granite "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreVanillastone = create("oreVanillastone"              , "Stone Ores"                      , "Stone "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreVanillagranite = create("oreVanillagranite"            , "Granite Ores"                    , "Granite "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreAndesite = create("oreAndesite"                  , "Andesite Ores"                   , "Andesite "                       , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreDiorite = create("oreDiorite"                   , "Diorite Ores"                    , "Diorite "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreDeepslate = create("oreDeepslate"                 , "Deepslate Ores"                  , "Deepslate "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreBlackstone = create("oreBlackstone"                , "Blackstone Ores"                 , "Blackstone "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreMoon = create("oreMoon"                      , "Moon Ores"                       , "Moon "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreMars = create("oreMars"                      , "Mars Ores"                       , "Mars "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreSpace = create("oreSpace"                     , "Space Ores"                      , "Space "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	orePhobos = create("orePhobos"                    , "Phobos Ores"                     , "Phobos "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreDeimos = create("oreDeimos"                    , "Deimos Ores"                     , "Deimos "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreVenus = create("oreVenus"                     , "Venus Ores"                      , "Venus "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreMercury = create("oreMercury"                   , "Mercury Ores"                    , "Mercury "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreCeres = create("oreCeres"                     , "Ceres Ores"                      , "Ceres "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreJupiter = create("oreJupiter"                   , "Jupiter Ores"                    , "Jupiter "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreIo = create("oreIo"                        , "Io Ores"                         , "Io "                             , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreEuropa = create("oreEuropa"                    , "Europa Ores"                     , "Europa "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreGanymede = create("oreGanymede"                  , "Ganymede Ores"                   , "Ganymede "                       , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreCallisto = create("oreCallisto"                  , "Callisto Ores"                   , "Callisto "                       , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreSaturn = create("oreSaturn"                    , "Saturn Ores"                     , "Saturn "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreRhea = create("oreRhea"                      , "Rhea Ores"                       , "Rhea "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreTitan = create("oreTitan"                     , "Titan Ores"                      , "Titan "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreOberon = create("oreOberon"                    , "Oberon Ores"                     , "Oberon "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreIapetus = create("oreIapetus"                   , "Iapetus Ores"                    , "Iapetus "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreUranus = create("oreUranus"                    , "Uranus Ores"                     , "Uranus "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreTitania = create("oreTitania"                   , "Titania Ores"                    , "Titania "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreNeptune = create("oreNeptune"                   , "Neptune Ores"                    , "Neptune "                        , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreTriton = create("oreTriton"                    , "Triton Ores"                     , "Triton "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	orePluto = create("orePluto"                     , "Pluto Ores"                      , "Pluto "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreEris = create("oreEris"                      , "Eris Ores"                       , "Eris "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreKepler22b = create("oreKepler22b"                 , "Kepler22b Ores"                  , "Kepler22b "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreHolystone = create("oreHolystone"                 , "Holystone Ores"                  , "Holystone "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreLivingrock = create("oreLivingrock"                , "Livingrock Ores"                 , "Livingrock "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreDeadrock = create("oreDeadrock"                  , "Deadrock Ores"                   , "Deadrock "                       , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreBetweenstone = create("oreBetweenstone"              , "Betweenstone Ores"               , "Betweenstone "                   , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	orePitstone = create("orePitstone"                  , "Pitstone Ores"                   , "Pitstone "                       , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreUmberstone = create("oreUmberstone"                , "Umberstone Ores"                 , "Umberstone "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreKomatiite = create("oreKomatiite"                 , "Komatiite Ores"                  , "Komatiite "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreBasalt = create("oreBasalt"                    , "Basalt Ores"                     , "Basalt "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreMarble = create("oreMarble"                    , "Marble Ores"                     , "Marble "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreLimestone = create("oreLimestone"                 , "Limestone Ores"                  , "Limestone "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreSiltstone = create("oreSiltstone"                 , "Siltstone Ores"                  , "Siltstone "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreShale = create("oreShale"                     , "Shale Ores"                      , "Shale "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreSlate = create("oreSlate"                     , "Slate Ores"                      , "Slate "                          , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreGreenschist = create("oreGreenschist"               , "Green Schist Ores"               , "Schist "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreBlueschist = create("oreBlueschist"                , "Blue Schist Ores"                , "Schist "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	orePinkschist = create("orePinkschist"                , "Pink Schist Ores"                , "Schist "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreGrayschist = create("oreGrayschist"                , "Gray Schist Ores"                , "Schist "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreGneiss = create("oreGneiss"                    , "Gneiss Ores"                     , "Gneiss "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreLightprismarine = create("oreLightprismarine"           , "Light Prismarine Ores"           , "Prismarine "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreDarkprismarine = create("oreDarkprismarine"            , "Dark Prismarine Ores"            , "Prismarine "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreKimberlite = create("oreKimberlite"                , "Kimberlite Ores"                 , "Kimberlite "                     , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreQuartzite = create("oreQuartzite"                 , "Quartzite Ores"                  , "Quartzite "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreNetherrack = create("oreNetherrack"                , "Netherrack Ores"                 , "Nether "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreEndstone = create("oreEndstone"                  , "Endstone Ores"                   , "End "                            , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreSandstone = create("oreSandstone"                 , "Sandstone Ores"                  , "Sandstone "                      , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, STANDARD_ORE).setTextureSetName("ore");
	oreGravel = create("oreGravel"                    , "Gravel Ores"                     , "Gravel "                         , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, DUST_ORE    ).setTextureSetName("oreDust");
	oreStrangesand = create("oreStrangesand"               , "Strange Sand Ores"               , "Strange Sand "                   , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, DUST_ORE    ).setTextureSetName("oreDust");
	oreRedSand = create("oreRedSand"                   , "Red Sand Ores"                   , "Sand "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, DUST_ORE    ).setTextureSetName("oreDust");
	oreSand = create("oreSand"                      , "Sand Ores"                       , "Sand "                           , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, DUST_ORE    ).setTextureSetName("oreDust");
	oreMud = create("oreMud"                       , "Mud Ores"                        , "Mud "                            , " Ore").setOreStats( 2*U ).add(BLOCK_BASED, DUST_ORE    ).setTextureSetName("oreDust");
	oreBedrock = create("oreBedrock"                   , "Bedrock Ores"                    , "Bedrock "                        , " Ore").setOreStats(64*U ).add(BLOCK_BASED              ).setTextureSetName("oreBedrock");
	oreNether = create("oreNether"                    , "Nether Ores"                     , "Nether "                         , " Ore").setOreStats( 4*U ).add(BLOCK_BASED, DENSE_ORE   ).setTextureSetName("oreDense"); // Prefix of the Nether-Ores Mod. Causes Ores to double. Ore -> Material is a Oneway Operation!
	oreDense = create("oreDense"                     , "Dense Ores"                      , "Dense "                          , " Ore").setOreStats( 4*U ).add(BLOCK_BASED, DENSE_ORE   ).setTextureSetName("oreDense").addIdenticalNames("denseore"); // Prefix of the Dense-Ores Mod. Causes Ores to double. Ore -> Material is a Oneway Operation!
	oreEnd = create("oreEnd"                       , "End Ores"                        , "End "                            , " Ore").setOreStats( 4*U ).add(BLOCK_BASED, DENSE_ORE   ).setTextureSetName("oreDense"); // In case of an End-Ores Mod. Ore -> Material is a Oneway Operation!
	oreHee = oreEndstone;
	oreRich = create("oreRich"                      , "Rich Ores"                       , "Rich "                           , " Ore").setOreStats(   U ).add(                         ); // Prefix of TFC
	}

	private static void regCrushed() {
	oreNormal = create("oreNormal"                    , "Normal Ores"                     , "Normal "                         , " Ore").setOreStats(   U2).add(                         ); // Prefix of TFC
	oreSmall = create("oreSmall"                     , "Small Ores"                      , "Small "                          , " Ore").setOreStats(   U3).add(                         ); // Prefix of TFC.
	orePoor = create("orePoor"                      , "Poor Ores"                       , "Poor "                           , " Ore").setOreStats(   U4).add(BLOCK_BASED              ); // Prefix of Railcraft.
	oreRaw = create("oreRaw"                       , "Raw Ores"                        , "Raw "                            , " Ore").setOreStats( 2*U ).add(STANDARD_ORE).addIdenticalNames("raw"); // The Vanilla "Raw Ore" Item from the Future.
	crushed = create("crushed"                      , "Crushed Ores"                    , "Crushed "                        , " Ore"                            ).setMaterialStats( 9*U8)     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  );
	crushedTiny = create("crushedTiny"                  , "Tiny Crushed Ores"               , "Tiny Crushed "                   , " Ore"                            ).setMaterialStats( 9*U72)    .setCondition(crushed)                                                                                      .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  );
	crushedPurified = create("crushedPurified"              , "Purified Ores"                   , "Purified "                       , " Ore"                            ).setMaterialStats(10*U8)     .setCondition(crushed)                                                                                      .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  );
	crushedPurifiedTiny = create("crushedPurifiedTiny"          , "Tiny Purified Ores"              , "Tiny Purified "                  , " Ore"                            ).setMaterialStats(10*U72)    .setCondition(crushedPurified)                                                                              .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  );
	crushedCentrifuged = create("crushedCentrifuged"           , "Refined Ores"                    , "Refined "                        , " Ore"                            ).setMaterialStats(11*U8)     .setCondition(crushedPurified)                                                                              .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_REFINED);
	crushedCentrifugedTiny = create("crushedCentrifugedTiny"       , "Tiny Refined Ores"               , "Tiny Refined "                   , " Ore"                            ).setMaterialStats(11*U72)    .setCondition(crushedCentrifuged)                                                                           .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_REFINED);
	rockGt = create("rockGt"                       , "Rocks"                           , ""                                , " bearing Rock"                   ).setMaterialStats( 9*U4)     .setCondition(new Or(ORES, STONE))                                                                          .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  );
	rawOreChunk = create("rawOreChunk"                  , "Raw Ore Chunks"                  , "Raw Chunk of "                   , " Ore"                            ).setMaterialStats(27*U72)    .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  ); // Prefix of Harder Ores
	clump = create("clump"                        , "Clumps"                          , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  );
	cluster = create("cluster"                      , "Native Clusters"                 , "Native "                         , " Cluster"                        ).setMaterialStats(U * 3)     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  ); // Introduced by Thaumcraft
	//  RoC?                        = create("RoC?"                         , "Flakes"                          , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_REFINED).aspects(TC.FABRICO, 1), // Introduced by RotaryCraft
	}

	private static void regDusts() {
	pebbles = create("pebbles"                      , "Pebbles"                         , ""                                , ""                                ).setMaterialStats(U * 3)     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  );
	rubble = create("rubble"                       , "Rubble"                          , ""                                , ""                                ).setMaterialStats(U * 2)     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  );
	chunk = create("chunk"                        , "Chunks"                          , ""                                , ""                                ).setMaterialStats(U * 2)     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  );
	crystalline = create("crystalline"                  , "Crystallised Metals"             , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_REFINED); // Introduced by Factorization
	reduced = create("reduced"                      , "Reduced Gravels"                 , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  ); // Introduced by Factorization
	cleanGravel = create("cleanGravel"                  , "Clean Gravels"                   , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN  ); // Introduced by Factorization
	dirtyGravel = create("dirtyGravel"                  , "Dirty Gravels"                   , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(ORES)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY  ); // Introduced by Factorization
	}

	private static void regIngots() {
	dust = create("dust"                         , "Dusts"                           , ""                                , " Dust"                           ).setMaterialStats(U    )     .setCondition(new Or(DUSTS, DIRTY_DUSTS))                                                                   .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, DUST_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64).addIdenticalNames("pulp", "itemDust"); // Pure Dust worth of one Ingot or Gem. Introduced by Alblaka.
	dustSmall = create("dustSmall"                    , "Small Dusts"                     , "Small Pile of "                  , " Dust"                           ).setMaterialStats(U4   )     .setCondition(dust)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, DUST_BASED           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 4); // 1/4th of a Dust.
	dustTiny = create("dustTiny"                     , "Tiny Dusts"                      , "Tiny Pile of "                   , " Dust"                           ).setMaterialStats(U9   )     .setCondition(dust)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, DUST_BASED           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 9); // 1/9th of a Dust.
	dustDiv72 = create("dustDiv72"                    , "1/72nd Dusts"                    , "1/72nd of a Pile of "            , " Dust"                           ).setMaterialStats(U72  )     .setCondition(dust)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, DUST_BASED           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 8); // 1/72nd of a Dust.
	dustImpure = create("dustImpure"                   , "Impure Dusts"                    , "Impure Pile of "                 , " Dust"                           ).setMaterialStats(U9*10)     .setCondition(DIRTY_DUSTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, ORE_PROCESSING_BASED, ORE_PROCESSING_DIRTY                                        ).setMinStacksize( 1).setTextureSetName("dust").addIdenticalNames("dustDirty"); // Dust with impurities. 1 Unit of Main Material and 1/9 - 1/4 Unit of secondary Material
	dustPure = create("dustPure"                     , "Purified Dusts"                  , "Purified Pile of "               , " Dust"                           ).setMaterialStats(U9*11)     .setCondition(DIRTY_DUSTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, ORE_PROCESSING_BASED, ORE_PROCESSING_CLEAN                                        ).setMinStacksize( 1).setTextureSetName("dust");
	dustRefined = create("dustRefined"                  , "Refined Dusts"                   , "Refined Pile of "                , " Dust"                           ).setMaterialStats(U9*12)     .setCondition(DIRTY_DUSTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, ORE_PROCESSING_BASED, ORE_PROCESSING_REFINED                                      ).setMinStacksize( 1).setTextureSetName("dust");
	}

	private static void regGems() {
	ingotQuintuple = create("ingotQuintuple"               , "5x Ingots"                       , "Quintuple "                      , " Ingot"                          ).setMaterialStats(U * 5)     .setCondition(MULTIINGOTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 2); // A quintuple Ingot.
	ingotQuadruple = create("ingotQuadruple"               , "4x Ingots"                       , "Quadruple "                      , " Ingot"                          ).setMaterialStats(U * 4)     .setCondition(MULTIINGOTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 2).addIdenticalNames("ingotQuad"); // A quadruple Ingot.
	ingotTriple = create("ingotTriple"                  , "3x Ingots"                       , "Triple "                         , " Ingot"                          ).setMaterialStats(U * 3)     .setCondition(MULTIINGOTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 3); // A triple Ingot.
	ingotDouble = create("ingotDouble"                  , "2x Ingots"                       , "Double "                         , " Ingot"                          ).setMaterialStats(U * 2)     .setCondition(MULTIINGOTS)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 5); // A double Ingot. Introduced by TerraFirmaCraft
	ingot = create("ingot"                        , "Ingots"                          , ""                                , " Ingot"                          ).setMaterialStats(U    )     .setCondition(INGOTS)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, INGOT_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64); // A regular Ingot. Introduced by Eloraam
	billet = create("billet"                       , "Billets"                         , ""                                , " Billet"                         ).setMaterialStats(U3* 2)     .setCondition(ingot)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, INGOT_BASED, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 3); // A small Ingot. Introduced by HBM
	chunkGt = create("chunkGt"                      , "Chunks"                          , ""                                , " Chunk"                          ).setMaterialStats(U4   )     .setCondition(ingot)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, INGOT_BASED, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 4); // A large Nugget.
	nugget = create("nugget"                       , "Nuggets"                         , ""                                , " Nugget"                         ).setMaterialStats(U9   )     .setCondition(ingot)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, INGOT_BASED, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 9); // A Nugget. Introduced by Eloraam
	gem = create("gem"                          , "Gemstones"                       , ""                                , ""                                ).setMaterialStats(U    )     .setCondition(GEMS)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64); // A regular Gem worth one Dust. Introduced by Eloraam
	gemChipped = create("gemChipped"                   , "Chipped Gemstones"               , "Chipped "                        , ""                                ).setMaterialStats(U4   )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED           , EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A regular Gem worth one small Dust. Introduced by TerraFirmaCraft
	gemFlawed = create("gemFlawed"                    , "Flawed Gemstones"                , "Flawed "                         , ""                                ).setMaterialStats(U2   )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED           , EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A regular Gem worth two small Dusts. Introduced by TerraFirmaCraft
	gemFlawless = create("gemFlawless"                  , "Flawless Gemstones"              , "Flawless "                       , ""                                ).setMaterialStats(U * 2)     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A regular Gem worth two Dusts. Introduced by TerraFirmaCraft
	}

	private static void regPlates() {
	gemExquisite = create("gemExquisite"                 , "Exquisite Gemstones"             , "Exquisite "                      , ""                                ).setMaterialStats(U * 4)     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A regular Gem worth four Dusts. Introduced by TerraFirmaCraft
	gemLegendary = create("gemLegendary"                 , "Legendary Gemstones"             , "Legendary "                      , ""                                ).setMaterialStats(U * 8)     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A regular Gem worth nine Dusts. Introduced by GregTech
	gemOre = create("gemOre"                       , "Gemstone Ores"                   , ""                                , " Ore"                            ).setMaterialStats(U    )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS).setMinStacksize(16); // Voltz Stuff
	gemRaw = create("gemRaw"                       , "Raw Gemstones"                   , "Raw "                            , ""                                ).setMaterialStats(U    )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS).setMinStacksize(16); // Voltz Stuff
	gemUncut = create("gemUncut"                     , "Uncut Gemstones"                 , "Uncut "                          , ""                                ).setMaterialStats(U    )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS).setMinStacksize(16); // Voltz Stuff
	gemPolished = create("gemPolished"                  , "Polished Gemstones"              , "Polished "                       , ""                                ).setMaterialStats(U    )     .setCondition(new And(gem, TRANSPARENT, CRYSTAL, PEARL.NOT))                                                .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS).setMinStacksize(16); // Voltz Stuff
	bouleGt = create("bouleGt"                      , "Boules"                          , ""                                , " Boule"                          ).setMaterialStats(U * 4)     .setCondition(ICondition.FALSE)                                                                             .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, SELF_REFERENCING, RECYCLABLE, SIMPLIFIABLE, GEM_BASED, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL); // A boule which can be used to cut gem stuff out of.
	crystalPure = create("crystalPure"                  , "Pure Crystals"                   , "Pure "                           , " Crystal"                        ).setMaterialStats(U2   )     .setCondition(GEMS)                                                                                         .add(              BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE);
	crystal = create("crystal"                      , "Crystals"                        , ""                                , " Crystal"                        ).setMaterialStats(U    )     .setCondition(GEMS)                                                                                         .add(              BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE);
	lens = create("lens"                         , "Lenses"                          , ""                                , " Lens"                           ).setMaterialStats(U4* 3)     .setCondition(LENSES)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, EXTRUDER_FODDER); // 3/4 of a Plate or Gem used to shape a Lense. Normally only used on Transparent Materials.
	scrapGt = create("scrapGt"                      , "Scrap"                           , ""                                , " Scrap"                          ).setMaterialStats(U9   )     .setCondition(new Or<>(dust, ingot, gem))                                                                   .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL, HIDDEN).setStacksize(18); // A piece of random Scrap. Basically just a container for 1/9th of a Material Unit of anything, which still has to be processed. Usually comes when breaking down Machines.
	}

	private static void regParts() {
	plateSteamcraft = create("plateSteamcraft"              , "Thin Plates"                     , "Thin "                           , " Plate"                          ).setMaterialStats(U3* 2)     .setCondition(PLATES)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	plateDense = create("plateDense"                   , "Dense Plates"                    , "Dense "                          , " Plate"                          ).setMaterialStats(U * 9)     .setCondition(DENSEPLATES)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // 9 Plates compressed into one.
	plateQuintuple = create("plateQuintuple"               , "5x Plates"                       , "Quintuple "                      , " Plate"                          ).setMaterialStats(U * 5)     .setCondition(MULTIPLATES)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	plateQuadruple = create("plateQuadruple"               , "4x Plates"                       , "Quadruple "                      , " Plate"                          ).setMaterialStats(U * 4)     .setCondition(MULTIPLATES)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16).addIdenticalNames("plateQuad");
	plateTriple = create("plateTriple"                  , "3x Plates"                       , "Triple "                         , " Plate"                          ).setMaterialStats(U * 3)     .setCondition(MULTIPLATES)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	plateDouble = create("plateDouble"                  , "2x Plates"                       , "Double "                         , " Plate"                          ).setMaterialStats(U * 2)     .setCondition(MULTIPLATES)                                                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	plate = create("plate"                        , "Plates"                          , ""                                , " Plate"                          ).setMaterialStats(U    )     .setCondition(new And(new Or(ingot, gem.NOT), PLATES))                                                      .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64); // Regular Plate made of one Ingot/Dust. Introduced by Calclavia
	plateGem = create("plateGem"                     , "Gem Plates"                      , "Crystalline "                    , " Plate"                          ).setMaterialStats(U    )     .setCondition(new And(new Or(gem, bouleGt), PLATES))                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64); // Regular Plate made of one Gem/Dust.
	plateTiny = create("plateTiny"                    , "Tiny Plates"                     , "Tiny "                           , " Plate"                          ).setMaterialStats(U9   )     .setCondition(plate)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Tiny Plate made of one ninth Ingot/Dust.
	plateGemTiny = create("plateGemTiny"                 , "Tiny Gem Plates"                 , "Tiny Crystalline "               , " Plate"                          ).setMaterialStats(U9   )     .setCondition(plateGem)                                                                                     .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Tiny Plate made of one ninth Gem/Dust.
	plateCurved = create("plateCurved"                  , "Curved Plates"                   , "Curved "                         , " Plate"                          ).setMaterialStats(U    )     .setCondition(plate)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(64); // Curved regular Plate.
	compressed = create("compressed"                   , "Compressed Materials"            , "Compressed "                     , ""                                ).setMaterialStats(U    )     .setCondition(PLATES)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL, UNIFICATABLE_RECIPES).setMinStacksize(16).addIdenticalNames("Compressed"); // Compressed Material, worth 1 Unit. Introduced by Galacticraft
	sheetGt = create("sheetGt"                      , "Flat Sheets"                     , ""                                , " Sheet"                          ).setMaterialStats(U    )     .setCondition(PLATES)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Flat Sheet, worth 1 Unit. Introduced by Advanced Rocketry, Prefix Name redirected by GT
	foil = create("foil"                         , "Foils"                           , ""                                , " Foil"                           ).setMaterialStats(U4   )     .setCondition(FOILS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Foil made of 1/4 Ingot/Dust.
	stick = create("stick"                        , "Sticks/Rods"                     , ""                                , " Rod"                            ).setMaterialStats(U2   )     .setCondition(STICKS)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Stick made of half an Ingot. Introduced by Eloraam
	stickLong = create("stickLong"                    , "Long Sticks/Rods"                , "Long "                           , " Rod"                            ).setMaterialStats(U    )     .setCondition(stick)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Stick made of an Ingot.
	bolt = create("bolt"                         , "Bolts"                           , ""                                , " Bolt"                           ).setMaterialStats(U8   )     .setCondition(stick)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 1/8 Ingot or 1/4 Stick.
	screw = create("screw"                        , "Screws"                          , ""                                , " Screw"                          ).setMaterialStats(U9   )     .setCondition(bolt)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of a Bolt.
	round = create("round"                        , "Rounds"                          , ""                                , " Round"                          ).setMaterialStats(U9   )     .setCondition(new Or(PARTS, new And(PROJECTILES, nugget)))                                                  .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of one Nugget.
	ring = create("ring"                         , "Rings"                           , ""                                , " Ring"                           ).setMaterialStats(U4   )     .setCondition(PARTS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 1/2 Stick.
	}

	private static void regContainers() {
	chain = create("chain"                        , "Chains"                          , ""                                , " Chain"                          ).setMaterialStats(U    )     .setCondition(ring)                                                                                         .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 4 Rings.
	spring = create("spring"                       , "Springs"                         , ""                                , " Spring"                         ).setMaterialStats(U    )     .setCondition(new And(PARTS, new Or(STRETCHY, BOUNCY, BRITTLE.NOT)))                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 2 Sticks.
	springSmall = create("springSmall"                  , "Small Springs"                   , "Small "                          , " Spring"                         ).setMaterialStats(U4   )     .setCondition(spring)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 1 Fine Wire.
	wireFine = create("wireFine"                     , "Fine Wires"                      , "Fine "                           , " Wire"                           ).setMaterialStats(U8   )     .setCondition(new Or(WIRES, new And(PARTS, SMITHABLE)))                                                     .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of 1/8 Ingot or 1/4 Wire.
	minecartWheels = create("minecartWheels"               , "Cart Wheels"                     , ""                                , " Cart Wheels"                    ).setMaterialStats(U    )     .setCondition(new And(PARTS, SMITHABLE))                                                                    .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16).setStacksize(16); // consisting out of 2 Rings and 1 Rod.
	gearGt = create("gearGt"                       , "Gears"                           , ""                                , " Gear"                           ).setMaterialStats(U * 4)     .setCondition(PARTS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // Introduced by me because BuildCraft has ruined the gear Prefix...
	gearGtSmall = create("gearGtSmall"                  , "Small Gears"                     , "Small "                          , " Gear"                           ).setMaterialStats(U    )     .setCondition(gearGt)                                                                                       .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	railGt = create("railGt"                       , "Single Rails"                    , ""                                , " Rail"                           ).setMaterialStats(U4   )     .setCondition(RAILS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16);
	}

	private static void regArmor() {
	casingSmall = create("casingSmall"                  , "Item Casings"                    , ""                                , " Item Casing"                    ).setMaterialStats(U2   )     .setCondition(PARTS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize(16); // consisting out of half a Metal Plate
	casingMachine = create("casingMachine"                , "Machine Casings"                 , ""                                , " Machine Casing"                 ).setMaterialStats(U * 8)     .setCondition(new And(PARTS, SMITHABLE))                                                                    .add(UNIFICATABLE, BURNABLE                  , RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 1).setStacksize(8);
	casingMachineDouble = create("casingMachineDouble"          , "Robust Machine Casings"          , "Robust "                         , " Machine Casing"                 ).setMaterialStats(U *14)     .setCondition(casingMachine)                                                                                .add(UNIFICATABLE, BURNABLE                  , RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 1).setStacksize(4);
	casingMachineQuadruple = create("casingMachineQuadruple"       , "Reinforced Machine Casings"      , "Reinforced "                     , " Machine Casing"                 ).setMaterialStats(U *26)     .setCondition(casingMachine)                                                                                .add(UNIFICATABLE, BURNABLE                  , RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 1).setStacksize(2);
	casingMachineDense = create("casingMachineDense"           , "Dense Machine Casings"           , "Dense "                          , " Machine Casing"                 ).setMaterialStats(U *56)     .setCondition(casingMachine)                                                                                .add(UNIFICATABLE, BURNABLE                  , RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, TOOLTIP_MATERIAL).setMinStacksize( 1).setStacksize(1);
	rotor = create("rotor"                        , "Rotors"                          , ""                                , " Rotor"                          ).setMaterialStats(U*4+U4)    .setCondition(PARTS)                                                                                        .add(UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, TOOLTIP_MATERIAL                 ).setMinStacksize(16).setStacksize(16); // consisting out of 4 Plates, 1 Ring.
	}

	private static void regPipes() {
	chemtube = create("chemtube"                     , "Glass Tubes"                     , "Glass Tube containing "          , ""                                ).setMaterialStats(U9   )     .setCondition(REACTS_WITH_GLASS.NOT)                                                                        .add(UNIFICATABLE, IS_CONTAINER, SELF_REFERENCING, RECYCLABLE, SCANNABLE, TOOLTIP_MATERIAL).setMinStacksize(64);
	cell = create("cell"                         , "Cells"                           , ""                                , " Cell"                           )                             .setCondition(new Or(CONTAINERS, EMPTY, CONTAINERS_FLUID, CONTAINERS_GAS))                                  .add(UNIFICATABLE, IS_CONTAINER, SELF_REFERENCING, MATERIAL_BASED, RECYCLABLE).setMinStacksize(16); // Regular Gas/Fluid Cell. Introduced by Calclavia
	bucket = create("bucket"                       , "Buckets"                         , ""                                , " Bucket"                         )                             .setCondition(new Or(CONTAINERS, EMPTY, CONTAINERS_FLUID))                                                  .add(              IS_CONTAINER, SELF_REFERENCING).setStacksize(16); // A Bucket filled with the Material.
	bottle = create("bottle"                       , "Bottles"                         , ""                                , " Bottle"                         )                             .setCondition(new Or(CONTAINERS, EMPTY, CONTAINERS_FLUID))                                                  .add(              IS_CONTAINER, SELF_REFERENCING, MATERIAL_BASED).setStacksize(16); // Glass Bottle containing a Fluid.
	capsule = create("capsule"                      , "Capsules"                        , ""                                , " Capsule"                        )                             .setCondition(new Or(CONTAINERS, EMPTY, CONTAINERS_FLUID, CONTAINERS_GAS))                                  .add(              IS_CONTAINER, SELF_REFERENCING, MATERIAL_BASED).setStacksize(16);
	bulletGtSmall = create("bulletGtSmall"                , "Small Bullets"                   , "Small "                          , " Bullet"                         ).setMaterialStats(U9   )     .setCondition(new Or(PROJECTILES, EMPTY))                                                                   .add(UNIFICATABLE, BURNABLE, AMMO_ALIKE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE              );
	bulletGtMedium = create("bulletGtMedium"               , "Medium Bullets"                  , "Medium "                         , " Bullet"                         ).setMaterialStats(2 *U9)     .setCondition(new Or(PROJECTILES, EMPTY))                                                                   .add(UNIFICATABLE, BURNABLE, AMMO_ALIKE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE              );
	bulletGtLarge = create("bulletGtLarge"                , "Large Bullets"                   , "Large "                          , " Bullet"                         ).setMaterialStats(U3   )     .setCondition(new Or(PROJECTILES, EMPTY))                                                                   .add(UNIFICATABLE, BURNABLE, AMMO_ALIKE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE              );
	}

	private static void regWires() {
	armorHelmet = create("armorHelmet"                  , "Helmets"                         , ""                                , ""                                ).setMaterialStats(U * 5)     .setCondition(ARMORS)                                                                                       .add(ARMOR_ALIKE, BURNABLE, RECYCLABLE).setStacksize( 1); // vanilly Helmet
	armorChestplate = create("armorChestplate"              , "Chestplates"                     , ""                                , ""                                ).setMaterialStats(U * 8)     .setCondition(ARMORS)                                                                                       .add(ARMOR_ALIKE, BURNABLE, RECYCLABLE).setStacksize( 1); // vanilly Chestplate
	armorLeggings = create("armorLeggings"                , "Leggings"                        , ""                                , ""                                ).setMaterialStats(U * 7)     .setCondition(ARMORS)                                                                                       .add(ARMOR_ALIKE, BURNABLE, RECYCLABLE).setStacksize( 1); // vanilly Pants
	armorBoots = create("armorBoots"                   , "Boots"                           , ""                                , ""                                ).setMaterialStats(U * 4)     .setCondition(ARMORS)                                                                                       .add(ARMOR_ALIKE, BURNABLE, RECYCLABLE).setStacksize( 1); // vanilly Boots
	armor = create("armor"                        , "Armor Parts"                     , ""                                , ""                                )                             .setCondition(ARMORS)                                                                                       .add(ARMOR_ALIKE                      ).setStacksize( 1);
	frameGt = create("frameGt"                      , "Frame Boxes"                     , ""                                , ""                                ).setMaterialStats(U * 2)     .setCondition(stick)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	capcellcon = create("capcellcon"                   , "Capsule Cell Containers"         , ""                                , " Capsule Cell Container"         ).setMaterialStats(U9   )     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER);
	pipeTiny = create("pipeTiny"                     , "Tiny Pipes"                      , "Tiny "                           , " Pipe"                           ).setMaterialStats(U2   )     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER);
	pipeSmall = create("pipeSmall"                    , "Small Pipes"                     , "Small "                          , " Pipe"                           ).setMaterialStats(U    )     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeMedium = create("pipeMedium"                   , "Medium Pipes"                    , "Medium "                         , " Pipe"                           ).setMaterialStats(U * 3)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeLarge = create("pipeLarge"                    , "Large pipes"                     , "Large "                          , " Pipe"                           ).setMaterialStats(U * 6)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeHuge = create("pipeHuge"                     , "Huge Pipes"                      , "Huge "                           , " Pipe"                           ).setMaterialStats(U *12)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeQuadruple = create("pipeQuadruple"                , "Quadruple Pipes"                 , "Quadruple "                      , " Pipe"                           ).setMaterialStats(U *12)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeNonuple = create("pipeNonuple"                  , "Nonuple Pipes"                   , "Nonuple "                        , " Pipe"                           ).setMaterialStats(U * 9)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER);
	pipeRestrictiveTiny = create("pipeRestrictiveTiny"          , "Tiny Restrictive Pipes"          , "Tiny Restrictive "               , " Pipe"                           ).setMaterialStats(U2   )     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE);
	pipeRestrictiveSmall = create("pipeRestrictiveSmall"         , "Small Restrictive Pipes"         , "Small Restrictive "              , " Pipe"                           ).setMaterialStats(U    )     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE);
	pipeRestrictiveMedium = create("pipeRestrictiveMedium"        , "Medium Restrictive Pipes"        , "Medium Restrictive "             , " Pipe"                           ).setMaterialStats(U * 3)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE);
	pipeRestrictiveLarge = create("pipeRestrictiveLarge"         , "Large Restrictive Pipes"         , "Large Restrictive "              , " Pipe"                           ).setMaterialStats(U * 6)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE);
	}

	private static void regCrates() {
	pipeRestrictiveHuge = create("pipeRestrictiveHuge"          , "Huge Restrictive Pipes"          , "Huge Restrictive "               , " Pipe"                           ).setMaterialStats(U *12)     .setCondition(PIPES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE);
	pipe = create("pipe"                         , "Pipes"                           , ""                                , " Pipe"                           )                             .setCondition(PIPES)                                                                                        .add();
	wireGt16 = create("wireGt16"                     , "16x Wires"                       , "16x "                            , " Wire"                           ).setMaterialStats(U2*16)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE           , EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt15 = create("wireGt15"                     , "15x Wires"                       , "15x "                            , " Wire"                           ).setMaterialStats(U2*15)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt14 = create("wireGt14"                     , "14x Wires"                       , "14x "                            , " Wire"                           ).setMaterialStats(U2*14)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt13 = create("wireGt13"                     , "13x Wires"                       , "13x "                            , " Wire"                           ).setMaterialStats(U2*13)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt12 = create("wireGt12"                     , "12x Wires"                       , "12x "                            , " Wire"                           ).setMaterialStats(U2*12)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt11 = create("wireGt11"                     , "11x Wires"                       , "11x "                            , " Wire"                           ).setMaterialStats(U2*11)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt10 = create("wireGt10"                     , "10x Wires"                       , "10x "                            , " Wire"                           ).setMaterialStats(U2*10)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt09 = create("wireGt09"                     , "9x Wires"                        , "9x "                             , " Wire"                           ).setMaterialStats(U2* 9)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	wireGt08 = create("wireGt08"                     , "8x Wires"                        , "8x "                             , " Wire"                           ).setMaterialStats(U2* 8)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 2);
	}

	private static void regBlocks() {
	wireGt07 = create("wireGt07"                     , "7x Wires"                        , "7x "                             , " Wire"                           ).setMaterialStats(U2* 7)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 4);
	wireGt06 = create("wireGt06"                     , "6x Wires"                        , "6x "                             , " Wire"                           ).setMaterialStats(U2* 6)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 4);
	wireGt05 = create("wireGt05"                     , "5x Wires"                        , "5x "                             , " Wire"                           ).setMaterialStats(U2* 5)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 4);
	wireGt04 = create("wireGt04"                     , "4x Wires"                        , "4x "                             , " Wire"                           ).setMaterialStats(U2* 4)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 4);
	wireGt03 = create("wireGt03"                     , "3x Wires"                        , "3x "                             , " Wire"                           ).setMaterialStats(U2* 3)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 8);
	wireGt02 = create("wireGt02"                     , "2x Wires"                        , "2x "                             , " Wire"                           ).setMaterialStats(U2* 2)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize( 8);
	wireGt01 = create("wireGt01"                     , "1x Wires"                        , "1x "                             , " Wire"                           ).setMaterialStats(U2* 1)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, WIRE_BASED).setMinStacksize(16);
	}

	private static void regPlants() {
	cableGt12 = create("cableGt12"                    , "12x Cables"                      , "12x "                            , " Cable"                          ).setMaterialStats(U * 6)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 2);
	cableGt08 = create("cableGt08"                    , "8x Cables"                       , "8x "                             , " Cable"                          ).setMaterialStats(U * 4)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 2);
	cableGt04 = create("cableGt04"                    , "4x Cables"                       , "4x "                             , " Cable"                          ).setMaterialStats(U * 2)     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 4);
	cableGt02 = create("cableGt02"                    , "2x Cables"                       , "2x "                             , " Cable"                          ).setMaterialStats(U    )     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize( 8);
	cableGt01 = create("cableGt01"                    , "1x Cables"                       , "1x "                             , " Cable"                          ).setMaterialStats(U2   )     .setCondition(WIRES)                                                                                        .add(UNIFICATABLE, BURNABLE, UNIFICATABLE_RECIPES, RECYCLABLE, SIMPLIFIABLE).setMinStacksize(16);
	}

	private static void regMisc() {
	crateGtRaw = create("crateGtRaw"                   , "Crates of Ore"                   , "Partial Crate of "               , " Ore"                            ).setMaterialStats(-1, U * 32).setCondition(oreRaw)                                                                                       .add(UNIFICATABLE, ORE             , BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64).addIdenticalNames("crateGtOre"); // consisting out of 16 Ores.
	crateGtGem = create("crateGtGem"                   , "Crates of Gems"                  , "Partial Crate of "               , " Gems"                           ).setMaterialStats(U *16)     .setCondition(gem)                                                                                          .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64); // consisting out of 16 Gems.
	crateGtDust = create("crateGtDust"                  , "Crates of Dust"                  , "Partial Crate of "               , " Dusts"                          ).setMaterialStats(U *16)     .setCondition(dust)                                                                                         .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64); // consisting out of 16 Dusts.
	crateGtIngot = create("crateGtIngot"                 , "Crates of Ingots"                , "Partial Crate of "               , " Ingots"                         ).setMaterialStats(U *16)     .setCondition(ingot)                                                                                        .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64); // consisting out of 16 Ingots.
	crateGtPlate = create("crateGtPlate"                 , "Crates of Plates"                , "Partial Crate of "               , " Plates"                         ).setMaterialStats(U *16)     .setCondition(plate)                                                                                        .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64); // consisting out of 16 Plates.
	crateGtPlateGem = create("crateGtPlateGem"              , "Crates of Gem Plates"            , "Partial Crate of "               , " Gem Plates"                     ).setMaterialStats(U *16)     .setCondition(plateGem)                                                                                     .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE, HIDDEN).setStacksize(64); // consisting out of 16 Gem Plates.
	crateGt64Raw = create("crateGt64Raw"                 , "Crates of Ore"                   , "Crate of "                       , " Ore"                            ).setMaterialStats(-1, U *128).setCondition(crateGtRaw)                                                                                   .add(UNIFICATABLE, ORE             , BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtRaw"     ).addIdenticalNames("crateGt64Ore"); // consisting out of 64 Ores.
	crateGt64Gem = create("crateGt64Gem"                 , "Crates of Gems"                  , "Crate of "                       , " Gems"                           ).setMaterialStats(U *64)     .setCondition(crateGtGem)                                                                                   .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtGem"     ); // consisting out of 64 Gems.
	crateGt64Dust = create("crateGt64Dust"                , "Crates of Dust"                  , "Crate of "                       , " Dusts"                          ).setMaterialStats(U *64)     .setCondition(crateGtDust)                                                                                  .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtDust"    ); // consisting out of 64 Dusts.
	crateGt64Ingot = create("crateGt64Ingot"               , "Crates of Ingots"                , "Crate of "                       , " Ingots"                         ).setMaterialStats(U *64)     .setCondition(crateGtIngot)                                                                                 .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtIngot"   ); // consisting out of 64 Ingots.
	crateGt64Plate = create("crateGt64Plate"               , "Crates of Plates"                , "Crate of "                       , " Plates"                         ).setMaterialStats(U *64)     .setCondition(crateGtPlate)                                                                                 .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtPlate"   ); // consisting out of 64 Plates.
	crateGt64PlateGem = create("crateGt64PlateGem"            , "Crates of Gem Plates"            , "Crate of "                       , " Gem Plates"                     ).setMaterialStats(U *64)     .setCondition(crateGtPlateGem)                                                                              .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, IS_CONTAINER, IS_CRATE).setStacksize(64).setTextureSetName("crateGtPlateGem"); // consisting out of 64 Gem Plates.
	blockRaw = create("blockRaw"                     , "Blocks of Ore"                   , "Block of "                       , " Ore"                            ).setMaterialStats(-1, U * 18).setCondition(oreRaw)                                                                                       .add(UNIFICATABLE, ORE             , BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED                                                                   ).setStacksize(64).addIdenticalNames("blockOre"); // New Vanilla Storage Block for Ores.
	blockGem = create("blockGem"                     , "Blocks of Gems"                  , "Block of "                       , ""                                ).setMaterialStats(U * 9)     .setCondition(gem)                                                                                          .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, GEM_BASED  ).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	blockDust = create("blockDust"                    , "Blocks of Dusts"                 , "Block of "                       , " Dust"                           ).setMaterialStats(U * 9)     .setCondition(dust)                                                                                         .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, DUST_BASED ).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	blockIngot = create("blockIngot"                   , "Blocks of Ingots"                , "Block of "                       , " Ingots"                         ).setMaterialStats(U * 9)     .setCondition(ingot)                                                                                        .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER, INGOT_BASED).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	blockPlate = create("blockPlate"                   , "Blocks of Plates"                , "Block of "                       , " Plates"                         ).setMaterialStats(U * 9)     .setCondition(plate)                                                                                        .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER             ).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	blockPlateGem = create("blockPlateGem"                , "Blocks of Gem Plates"            , "Block of "                       , " Gem Plates"                     ).setMaterialStats(U * 9)     .setCondition(plateGem)                                                                                     .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER             ).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	blockSolid = create("blockSolid"                   , "Blocks of Cast Metal"            , "Block of solid "                 , ""                                ).setMaterialStats(U * 9)     .setCondition(blockIngot)                                                                                   .add(UNIFICATABLE, TOOLTIP_MATERIAL, BURNABLE, TOOLTIP_ENCHANTS, STORAGE_BASED, BLOCK_BASED, RECYCLABLE, SIMPLIFIABLE, SCANNABLE, EXTRUDER_FODDER             ).setStacksize(64); // To finally get rid of the messy and unreliable Storage Block Code.
	orebush = create("orebush"                      , "Ore Bushes"                      , ""                                , " Bush"                           )                             .setCondition(PLANTS)                                                                                       .add(HIDDEN, PLANT_DROP).setMinStacksize(16);
	oreberry = create("oreberry"                     , "Ore Berries"                     , ""                                , " Berry"                          ).setMaterialStats(U9   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	plantGtBerry = create("plantGtBerry"                 , "Berries"                         , ""                                , " Berry"                          ).setMaterialStats(U9   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	plantGtTwig = create("plantGtTwig"                  , "Twigs"                           , ""                                , " Twig"                           ).setMaterialStats(U9   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	plantGtFiber = create("plantGtFiber"                 , "Fibers"                          , "Raw "                            , " Fiber"                          ).setMaterialStats(U9   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	plantGtWart = create("plantGtWart"                  , "Warts"                           , ""                                , " Wart"                           ).setMaterialStats(U4   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	plantGtBlossom = create("plantGtBlossom"               , "Blossoms"                        , ""                                , " Blossom"                        ).setMaterialStats(U9   )     .setCondition(PLANTS)                                                                                       .add(HIDDEN, UNIFICATABLE, BURNABLE, TOOLTIP_ENCHANTS, RECYCLABLE, PLANT_DROP).setMinStacksize(16);
	compressedCobblestone = create("compressedCobblestone"        , "9^X Compressed Cobblestones"     , ""                                , ""                                )                             .add();
	compressedStone = create("compressedStone"              , "9^X Compressed Stones"           , ""                                , ""                                )                             .add();
	compressedDirt = create("compressedDirt"               , "9^X Compressed Dirt"             , ""                                , ""                                )                             .add();
	compressedGravel = create("compressedGravel"             , "9^X Compressed Gravel"           , ""                                , ""                                )                             .add();
	compressedSand = create("compressedSand"               , "9^X Compressed Sand"             , ""                                , ""                                )                             .add();
	blockBamboo = create("blockBamboo"                  , "Bamboo Blocks"                   , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED);
	blockGlass = create("blockGlass"                   , "Glass Blocks"                    , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED);
	blockWool = create("blockWool"                    , "Wool Blocks"                     , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED);
	block_ = create("block_"                       , "Random Blocks"                   , ""                                , ""                                )                             .add(BLOCK_BASED, NO_PREFIX_FILTERING); // IGNORE
	block = create("block"                        , "Random Blocks"                   , ""                                , ""                                )                             .add(BLOCK_BASED, NO_PREFIX_FILTERING); // Storage Block consisting out of 9 Ingots/Gems/Dusts. Introduced by CovertJaguar, abused by too many people, and then deprecated by me, by adding a bunch of more detailed Prefixes, to finally get rid of the messy an unreliable Storage Block Code.
	item_ = create("item_"                        , "Items"                           , ""                                , ""                                )                             .add(NO_PREFIX_FILTERING); // IGNORE
	item = create("item"                         , "Items"                           , ""                                , ""                                )                             .add(NO_PREFIX_FILTERING); // Random Item. Introduced by Alblaka
	}

	private static void regDyes() {
	glass = create("glass"                        , "Glasses"                         , ""                                , ""                                )                             .add(SELF_REFERENCING, UNIFICATABLE_RECIPES);
	}

	private static void regElectric() {
	paneGlass = create("paneGlass"                    , "Glass Panes"                     , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED);
	stainedClay = create("stainedClay"                  , "Stained Clays"                   , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED); // Used for the 16 colors of Stained Clay. Introduced by Forge
	craftingTool = create("craftingTool"                 , "Crafting Tools"                  , ""                                , ""                                )                             .add(TOOL_ALIKE); // Special Prefix used mainly for the Crafting Handler.
	}

	private static void regUnused() {
	crafting = create("crafting"                     , "Crafting Ingredients"            , ""                                , ""                                )                             .add(NO_PREFIX_FILTERING); // Special Prefix used mainly for the Crafting Handler.
	craft = create("craft"                        , "Crafting Stuff?"                 , ""                                , ""                                )                             .add(); // Special Prefix used mainly for the Crafting Handler.
	slab = create("slab"                         , "Slabs"                           , ""                                , ""                                )                             .add(BLOCK_BASED); // Prefix used for Slabs. Usually as "slabWood" or "slabStone". Introduced by SirSengir
	stair = create("stair"                        , "Stairs"                          , ""                                , ""                                )                             .add(BLOCK_BASED); // Prefix used for Stairs. Usually as "stairWood" or "stairStone". Introduced by SirSengir
	fence = create("fence"                        , "Fences"                          , ""                                , ""                                )                             .add(); // Prefix used for Fences. Usually as "fenceWood". Introduced by Forge
	treeSapling = create("treeSapling"                  , "Saplings"                        , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED).addIdenticalNames("sapling").setMinStacksize(16); // Prefix for Saplings.
	treeLeaves = create("treeLeaves"                   , "Leaves"                          , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED).addIdenticalNames("leaves").setMinStacksize(16); // Prefix for Leaves.
	tree = create("tree"                         , "Tree Parts"                      , ""                                , ""                                )                             .add(); // Prefix for Tree Parts.
	log = create("log"                          , "Logs"                            , ""                                , ""                                )                             .add(BLOCK_BASED, UNIFICATABLE_RECIPES); // Prefix used for Logs. Usually as "logWood". Introduced by Eloraam
	beam = create("beam"                         , "Beams"                           , ""                                , ""                                )                             .add(BLOCK_BASED, UNIFICATABLE_RECIPES); // Prefix used for Beams.  Usually as "beamWood".
	plank = create("plank"                        , "Planks"                          , ""                                , ""                                )                             .add(BLOCK_BASED, UNIFICATABLE_RECIPES).setMinStacksize(16); // Prefix for Planks. Usually "plankWood". Introduced by Eloraam
	stoneCobble = create("stoneCobble"                  , "Cobblestones"                    , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Cobblestone Prefix for all Cobblestones.
	stoneSmooth = create("stoneSmooth"                  , "Smoothstones"                    , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Smoothstone Prefix.
	stoneMossyBricks = create("stoneMossyBricks"             , "mossy Stone Bricks"              , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ).addIdenticalNames("stoneBricksMossy"); // Mossy Stone Bricks.
	stoneMossy = create("stoneMossy"                   , "Mossy Stones"                    , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Mossy Cobble.
	stoneBricks = create("stoneBricks"                  , "Stone Bricks"                    , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ).addIdenticalNames("stoneBrick", "stonebrick"); // Stone Bricks.
	stoneCracked = create("stoneCracked"                 , "Cracked Stones"                  , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Cracked Bricks.
	stoneChiseled = create("stoneChiseled"                , "Chiseled Stones"                 , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Chiseled Stone.
	stonePolished = create("stonePolished"                , "Polished Stones"                 , ""                                , ""                                )                             .add(SELF_REFERENCING, BLOCK_BASED                                          ); // Polished Stone.
	stone = create("stone"                        , "Stones"                          , ""                                , ""                                )                             .add(MATERIAL_BASED, SELF_REFERENCING, BLOCK_BASED, UNIFICATABLE_RECIPES    ); // Prefix to determine which kind of Rock this is.
	cobblestone = create("cobblestone"                  , "Cobblestones"                    , ""                                , ""                                )                             .add(MATERIAL_BASED, SELF_REFERENCING, BLOCK_BASED                          );
	rock = create("rock"                         , "Rocks"                           , ""                                , ""                                )                             .add(MATERIAL_BASED, SELF_REFERENCING, BLOCK_BASED, UNIFICATABLE_RECIPES    ); // Prefix to determine which kind of Rock this is.
	record = create("record"                       , "Records"                         , ""                                , ""                                )                             .add(SELF_REFERENCING).setStacksize(64);
	scraps = create("scraps"                       , "Scraps"                          , ""                                , ""                                )                             .add(UNIFICATABLE, MATERIAL_BASED);
	scrap = create("scrap"                        , "Scraps"                          , ""                                , ""                                )                             .add();
	book = create("book"                         , "Books"                           , ""                                , ""                                )                             .add(); // Used for Books of any kind.
	paper = create("paper"                        , "Papers"                          , ""                                , ""                                )                             .add(); // Used for Papers of any kind.
	dye = create("dye"                          , "Dyes"                            , ""                                , ""                                )                             .add(SELF_REFERENCING); // Used for the 16 dyes. Introduced by Eloraam
	dyeMixable = create("dyeMixable"                   , "Mixable Dyes"                    , ""                                , ""                                )                             .add(SELF_REFERENCING); // Used for the dyes that can be mixed together.
	dyeCeramic = create("dyeCeramic"                   , "Ceramic Dyes"                    , ""                                , ""                                )                             .add(SELF_REFERENCING); // Used for the MFR dyes.
	/* Electric Components.
	 * 
	 * usual Materials for this are:
	 * Primitive (Tier 1)
	 * Basic (Tier 2) as used by UE as well : IC2 Circuit and RE-Battery
	 * Good (Tier 3)
	 * Advanced (Tier 4) as used by UE as well : Advanced Circuit, Advanced Battery and Lithium Battery
	 * Data (Tier 5) : Data Storage Circuit
	 * Elite (Tier 6) as used by UE as well : Energy Crystal and Data Control Circuit
	 * Master (Tier 7) : Energy Flow Circuit and Lapotron Crystal
	 * Ultimate (Tier 8) : Data Orb and Lapotronic Energy Orb
	 * Infinite (Cheaty)
	 */
	batterySingleuse = create("batterySingleuse"             , "Single Use Batteries"            );
	battery = create("battery"                      , "Reusable Batteries"              ); // Introduced by Calclavia
	circuit = create("circuit"                      , "Circuits"                        ); // Introduced by Calclavia
	computer = create("computer"                     , "Computers"                       ); // A whole Computer.
	// random known prefixes without special abilities.
	shard = unused("shard"                        ).setCategoryName("Crystallised Shards"             ); // Introduced by Mekanism, abused too much to be used...
	sand = unused("sand"                         ).setCategoryName("Sands"                           ).add(SELF_REFERENCING, BLOCK_BASED);
	wire = unused("wire"                         ).setCategoryName("Wires"                           ).add(UNIFICATABLE_RECIPES);
	lamp = unused("lamp"                         ).setCategoryName("Lamps");
	cloth = unused("cloth"                        ).setCategoryName("Cloth");
	fabric = unused("fabric"                       ).setCategoryName("Fabric");
	quartz = unused("quartz"                       ).setCategoryName("Quartzes");
	part = unused("part"                         ).setCategoryName("Parts"); // partBasicCircuit partAdvancedCircuit partPhotocell partMotor
	torch = unused("torch"                        ).setCategoryName("Torches");
	skull = unused("skull"                        ).setCategoryName("Skulls");
	plating = unused("plating"                      ).setCategoryName("Platings");
	dinosaur = unused("dinosaur"                     ).setCategoryName("Dinosaurs");
	travelgear = unused("travelgear"                   ).setCategoryName("Travel Gear");
	bauble = unused("bauble"                       ).setCategoryName("Baubles");
	grafter = unused("grafter"                      ).setCategoryName("Grafters");
	scoop = unused("scoop"                        ).setCategoryName("Scoops");
	frame = unused("frame"                        ).setCategoryName("Frames");
	tome = unused("tome"                         ).setCategoryName("Tomes");
	junk = unused("junk"                         ).setCategoryName("Junk");
	bee = unused("bee"                          ).setCategoryName("Bees");
	rod = unused("rod"                          ).setCategoryName("Rods");
	dirt = unused("dirt"                         ).setCategoryName("Dirts");
	grass = unused("grass"                        ).setCategoryName("Grasses");
	gravel = unused("gravel"                       ).setCategoryName("Gravels");
	mushroom = unused("mushroom"                     ).setCategoryName("Mushrooms");
	wood = unused("wood"                         ).setCategoryName("Woods");
	drop = unused("drop"                         ).setCategoryName("Drops");
	fuel = unused("fuel"                         ).setCategoryName("Fuels");
	panel = unused("panel"                        ).setCategoryName("Panels");
	brick = unused("brick"                        ).setCategoryName("Bricks");
	seed = unused("seed"                         ).setCategoryName("Seeds");
	reed = unused("reed"                         ).setCategoryName("Reeds");
	sheetDouble = unused("sheetDouble"                  ).setCategoryName("2x Sheets");
	sheet = unused("sheet"                        ).setCategoryName("Sheets");
	crop = unused("crop"                         ).setCategoryName("Crops");
	plant = unused("plant"                        ).setCategoryName("Plants");
	coin = unused("coin"                         ).setCategoryName("Coins");
	lumar = unused("lumar"                        ).setCategoryName("Lumars");
	ground = unused("ground"                       ).setCategoryName("Grounded Stuff");
	cable = unused("cable"                        ).setCategoryName("Cables");
	component = unused("component"                    ).setCategoryName("Components");
	pole = unused("pole"                         ).setCategoryName("Poles");
	desert = unused("desert"                       ).setCategoryName("Desert Stuff");
	jungle = unused("jungle"                       ).setCategoryName("Jungle Stuff");
	savanna = unused("savanna"                      ).setCategoryName("Savanna Stuff");
	beach = unused("beach"                        ).setCategoryName("Beach Stuff");
	forest = unused("forest"                       ).setCategoryName("Forest Stuff");
	mountain = unused("mountain"                     ).setCategoryName("Mountain Stuff");
	plains = unused("plains"                       ).setCategoryName("Plains Stuff");
	epiphyte = unused("epiphyte"                     ).setCategoryName("Epiphyte Stuff");
	water = unused("water"                        ).setCategoryName("Water Stuff");
	river = unused("river"                        ).setCategoryName("River Stuff");
	ocean = unused("ocean"                        ).setCategoryName("Ocean Stuff");
	hanging = unused("hanging"                      ).setCategoryName("Hanging Stuff");
	floating = unused("floating"                     ).setCategoryName("Floating Stuff");
	wetlands = unused("wetlands"                     ).setCategoryName("Wetland Stuff");
	fern = unused("fern"                         ).setCategoryName("Ferns");
	vine = unused("vine"                         ).setCategoryName("Vines");
	fungus = unused("fungus"                       ).setCategoryName("Fungi");
	cactus = unused("cactus"                       ).setCategoryName("Cacti");
	bud = unused("bud"                          ).setCategoryName("Buds");
	immersed = unused("immersed"                     ).setCategoryName("Immersed Stuff");
	bamboo = unused("bamboo"                       ).setCategoryName("Bamboo Stuff");
	cones = unused("cones"                        ).setCategoryName("Cones");
	consumable = unused("consumable"                   ).setCategoryName("Consumables");
	leafy = unused("leafy"                        ).setCategoryName("Leafy Stuff");
	leaf = unused("leaf"                         ).setCategoryName("Leaf Stuff");
	shrub = unused("shrub"                        ).setCategoryName("Shrubs");
	berrybush = unused("berrybush"                    ).setCategoryName("Berry Bushes");
	wax = unused("wax"                          ).setCategoryName("Waxes");
	wall = unused("wall"                         ).setCategoryName("Walls");
	tube = unused("tube"                         ).setCategoryName("Tubes");
	list = unused("list"                         ).setCategoryName("Lists").add(NO_PREFIX_FILTERING);
	food = unused("food"                         ).setCategoryName("Foods").add(NO_PREFIX_FILTERING);
	gear = unused("gear"                         ).setCategoryName("Gears"); // Introduced by SirSengir
	coral = unused("coral"                        ).setCategoryName("Corals");
	flower = unused("flower"                       ).setCategoryName("Flowers");
	storage = unused("storage"                      ).setCategoryName("Storages");
	material = unused("material"                     ).setCategoryName("Materials").add(NO_PREFIX_FILTERING);
	plasma = unused("plasma"                       ).setCategoryName("Plasmas");
	element = unused("element"                      ).setCategoryName("Elements");
	molecule = unused("molecule"                     ).setCategoryName("Molecules");
	wafer = unused("wafer"                        ).setCategoryName("Wafers");
	orb = unused("orb"                          ).setCategoryName("Orbs");
	handle = unused("handle"                       ).setCategoryName("Handles");
	blade = unused("blade"                        ).setCategoryName("Blades");
	head = unused("head"                         ).setCategoryName("Heads");
	motor = unused("motor"                        ).setCategoryName("Motors");
	bowl = unused("bowl"                         ).setCategoryName("Bowls");
	bit = unused("bit"                          ).setCategoryName("Bits");
	shears = unused("shears"                       ).setCategoryName("Shears");
	turbine = unused("turbine"                      ).setCategoryName("Turbines");
	fertilizer = unused("fertilizer"                   ).setCategoryName("Fertilizers");
	chest = unused("chest"                        ).setCategoryName("Chests");
	raw = unused("raw"                          ).setCategoryName("Raw Things");
	stainedGlass = unused("stainedGlass"                 ).setCategoryName("Stained Glass");
	mystic = unused("mystic"                       ).setCategoryName("Mystic Stuff");
	mana = unused("mana"                         ).setCategoryName("Mana Stuff");
	rune = unused("rune"                         ).setCategoryName("Runes");
	petal = unused("petal"                        ).setCategoryName("Petals");
	pearl = unused("pearl"                        ).setCategoryName("Pearls");
	powder = unused("powder"                       ).setCategoryName("Powders");
	soulsand = unused("soulsand"                     ).setCategoryName("Soulsands");
	obsidian = unused("obsidian"                     ).setCategoryName("Obsidians");
	glowstone = unused("glowstone"                    ).setCategoryName("Glowstones");
	beans = unused("beans"                        ).setCategoryName("Beans");
	essence = unused("essence"                      ).setCategoryName("Essences");
	alloy = unused("alloy"                        ).setCategoryName("Alloys");
	cooking = unused("cooking"                      ).setCategoryName("Cooked Things");
	gate = unused("gate"                         ).setCategoryName("Gates");
	ladder = unused("ladder"                       ).setCategoryName("Ladders");
	door = unused("door"                         ).setCategoryName("Doors");
	trapdoor = unused("trapdoor"                     ).setCategoryName("Trapdoors");
	elven = unused("elven"                        ).setCategoryName("Elven Stuff");
	reactor = unused("reactor"                      ).setCategoryName("Reactors");
	mffs = unused("mffs"                         ).setCategoryName("MFFS");
	projred = unused("projred"                      ).setCategoryName("Project Red");
	ganys = unused("ganys"                        ).setCategoryName("Ganys Stuff");
	liquid = unused("liquid"                       ).setCategoryName("Liquids");
	chipset = unused("chipset"                      ).setCategoryName("Chipsets");
	boule = unused("boule"                        ).setCategoryName("Boules");
	lump = unused("lump"                         ).setCategoryName("Lumps");
	pellet = unused("pellet"                       ).setCategoryName("Pellets");
	tiny = unused("tiny"                         ).setCategoryName("Tiny");
	bars = unused("bars"                         ).setCategoryName("Bars");
	bar = unused("bar"                          ).setCategoryName("Bars");
	}

	private static void regArrays() { // :564-565
		wireGt = new OreDictPrefix[] {wireGt01, wireGt02, wireGt03, wireGt04, wireGt05, wireGt06, wireGt07, wireGt08, wireGt09, wireGt10, wireGt11, wireGt12, wireGt13, wireGt14, wireGt15, wireGt16};
		array_dust_ingot = new OreDictPrefix[] {dust, ingot};
		array_dust_ingot_plate = new OreDictPrefix[] {dust, ingot, plate};
		array_dust_ingot_plate_gem = new OreDictPrefix[] {dust, ingot, plate, gem};
		array_ingot_plate = new OreDictPrefix[] {ingot, plate};
		array_ingot_plate_gem = new OreDictPrefix[] {ingot, plate, gem};
		array_ingot_gem = new OreDictPrefix[] {ingot, gem};
	}

	/** Upstream :578-580 (crushed family reversal) and :584-600 (tag/shape based familiar loops), verbatim logic. */
	private static void regFamiliarPrefixes() {
		crushed           .addFamiliarPrefixWithReversal(crushedTiny);
		crushedPurified   .addFamiliarPrefixWithReversal(crushedPurifiedTiny);
		crushedCentrifuged.addFamiliarPrefixWithReversal(crushedCentrifugedTiny);
		
		for (OreDictPrefix tPrefix1 : OreDictPrefix.VALUES) {
			if (tPrefix1.contains(STANDARD_ORE)) {{tPrefix1.addFamiliarPrefixWithReversal(blockRaw);}}
			for (OreDictPrefix tPrefix2 : OreDictPrefix.VALUES) {
			if (tPrefix1.contains(INGOT_BASED              ) && tPrefix2.contains(INGOT_BASED              )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.contains(DUST_BASED               ) && tPrefix2.contains(DUST_BASED               )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.contains(DUST_ORE                 ) && tPrefix2.contains(DUST_ORE                 )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.contains(DENSE_ORE                ) && tPrefix2.contains(DENSE_ORE                )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.contains(STANDARD_ORE             ) && tPrefix2.contains(STANDARD_ORE             )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.mNameInternal.startsWith("pipe"   ) && tPrefix2.mNameInternal.startsWith("pipe"   )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.mNameInternal.startsWith("wireGt" ) && tPrefix2.mNameInternal.startsWith("wireGt" )) tPrefix1.addFamiliarPrefix(tPrefix2);
			if (tPrefix1.mNameInternal.startsWith("cableGt") && tPrefix2.mNameInternal.startsWith("cableGt")) tPrefix1.addFamiliarPrefix(tPrefix2);
			}
		}
	}

	/** Upstream :627-636, verbatim: derives the actual Prefix from mPriorityPrefixIndex (0 = none, 1 = Gem, 2 = Dust, 3 = Ingot, 4 = Plate, 5 = Gem Plate). */
	public static void applyPriorityPrefixes() {
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_MAP.values()) {
			switch (tMaterial.mPriorityPrefixIndex) {
			case 1: tMaterial.mPriorityPrefix = gem; break;
			case 2: tMaterial.mPriorityPrefix = dust; break;
			case 3: tMaterial.mPriorityPrefix = ingot; break;
			case 4: tMaterial.mPriorityPrefix = plate; break;
			case 5: tMaterial.mPriorityPrefix = plateGem; break;
			}
		}
	}
}
