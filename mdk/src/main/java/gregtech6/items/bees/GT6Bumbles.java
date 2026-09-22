/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.items.bees;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.maps.GT6RecipeMapBumblelyzer;

/**
 * The bumblebee ITEM domain (task p33-bees-lv3-a-items) — the port of the upstream
 * {@code MultiItemBumbles} 640-variant meta item (MultiItemBumbles.java:61-692).
 *
 * <p><b>The meta flattening</b> (the GT6SprayCans/GT6FoodCans ruling scaled to the bee
 * domain): upstream coded the whole bee identity in ONE meta,
 * {@code family*100 + tier*10 + type} with the type digit in {@code meta%10}
 * (MultiItemBumbles.java:512, {@code 0=Drone, 1=Princess, 2=Queen, 4=Dead,
 * 5-9 = the scanned forms} — IItemBumbleBee.java:51). The port keeps the
 * {@code family*100 + tier*10} code in the stack NBT ({@code gt.bumble.meta},
 * {@link GT6BumbleGenes}) and flattens the TYPE digit onto the item id: the 8-face
 * {@code type%5} fractal (Bumbliary slot checks, MultiTileEntityBumbliary.java:150/:197/
 * :202/:207) becomes 4 base + 4 scanned items ({@link #FACES}), so the breeding chain's
 * {@code bumbleType(stack)%5} folds to the item identity.
 *
 * <p><b>The species table</b> ({@link #SPECIES}): the 80 {@code family*100 + tier*10}
 * rows of {@code addItems()} (:70-178) in declaration order — the single source the
 * datagen providers and the tests walk (the {@code GT6BeeCombs.COMB_SPECS} shape).
 * The {@code bumbleProductStack} comb mapping and the flower-scan face
 * ({@code bumbleCanProduce}, :216-349 — the BoP/NeLi/EtFu compat block) are the card-C/
 * card-B faces and stay out (the card's 不做 list); the requirement WORDS ride the
 * tooltip ({@link #requirementOf}).
 *
 * <p><b>The mutation faces</b> ({@link #mutateChance}/{@link #mutateCode}): the
 * :459-478 verbatim ladder — chance 500/500/250/25 per tier (of 10000), the mutation
 * itself {@code meta±10} (tier 0 up-only, tier 3 down-only, tiers 1-2 either way).
 *
 * <p>The creative-tab face is CUT (the GT6UsbSticks ruling: /give + the crafting rows
 * cover the live faces; the tab walk is the creative-tab card's exclusive surface).
 *
 * <p>KJS surface (the card's declaration): REGISTRATION face only — the KubeJS typings
 * defer to the kjs binding card; the NBT semantics carry no KubeJS face.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Bumbles {

	// the type%5 fractal digits (IItemBumbleBee.java:51 verbatim)
	/** {@code meta%10 == 0} — the drone. */
	public static final byte TYPE_DRONE = 0;
	/** {@code meta%10 == 1} — the princess (the Bumbliary royal slot, type%5==1). */
	public static final byte TYPE_PRINCESS = 1;
	/** {@code meta%10 == 2} — the crowned queen. */
	public static final byte TYPE_QUEEN = 2;
	/** {@code meta%10 == 4} — the dead form. */
	public static final byte TYPE_DEAD = 4;
	/** The scan offset ({@code meta%10 >= 5} = scanned, MultiItemBumbles.java:564). */
	public static final byte SCAN_OFFSET = 5;

	/** The self-contained registration listener (the GT6BeeCombs shape). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gt6");

	/** One type-face row: the snake id fragment, the fractal digit, the scan flag. */
	public record FaceRow(String name, byte face, boolean scanned) {
		/** The gt6 registry path ({@code bumble_} prefix rides the register call). */
		public String itemId() {return "bumble_" + name;}
		/** The name-format lang key ({@code gt6.row.bumble.name.<name>}). */
		public String formatKey() {return "gt6.row.bumble.name." + name;}
	}

	/** The 8 faces in registration order (base 0/1/2/4, then the +5 scanned forms). */
	public static final List<FaceRow> FACES = List.of(
			new FaceRow("drone"          , TYPE_DRONE   , false),
			new FaceRow("princess"       , TYPE_PRINCESS, false),
			new FaceRow("queen"          , TYPE_QUEEN   , false),
			new FaceRow("dead"           , TYPE_DEAD    , false),
			new FaceRow("drone_scanned"  , TYPE_DRONE   , true),
			new FaceRow("princess_scanned", TYPE_PRINCESS, true),
			new FaceRow("queen_scanned"  , TYPE_QUEEN   , true),
			new FaceRow("dead_scanned"   , TYPE_DEAD    , true));

	/** The 8 registered items, index-aligned with {@link #FACES}. */
	public static final List<RegistryObject<Item>> BEE_ITEMS = FACES.stream()
			.map(tFace -> ITEMS.<Item>register(tFace.itemId(), () -> new GT6BumbleItem(new Item.Properties(), tFace.face(), tFace.scanned())))
			.toList();

	/** The face row of a stack, or null (foreign item). */
	@Nullable
	public static FaceRow faceOf(ItemStack aStack) {
		for (int i = 0; i < FACES.size(); i++) if (BEE_ITEMS.get(i).get() == aStack.getItem()) return FACES.get(i);
		return null;
	}

	// -------------------------------------------------------------------------
	// the species table — the addItems() rows (MultiItemBumbles.java:70-178, in order)
	// -------------------------------------------------------------------------

	/** One species row: the {@code family*100 + tier*10} code and the upstream display name. */
	public record SpeciesRow(int code, String display) {
		/** The species lang key ({@code gt6.row.bumble.<code>} — the dump's meta-keyed face). */
		public String langKey() {return "gt6.row.bumble." + code;}
	}

	/** The 80 species rows (20 families × 4 tiers), declaration order = the :70-178 order. */
	public static final List<SpeciesRow> SPECIES = List.of(
			new SpeciesRow(    0, "Wild Bumblebee"        ),
			new SpeciesRow(   10, "Captive Bumblebee"     ),
			new SpeciesRow(   20, "Common Bumblebee"      ),
			new SpeciesRow(   30, "Cultivated Bumblebee"  ),
			new SpeciesRow(  100, "Surfing Bumblebee"     ),
			new SpeciesRow(  110, "Swimming Bumblebee"    ),
			new SpeciesRow(  120, "Diving Bumblebee"      ),
			new SpeciesRow(  130, "Subnautic Bumblebee"   ),
			new SpeciesRow(  200, "Apprentice Bumblebee"  ),
			new SpeciesRow(  210, "Magician Bumblebee"    ),
			new SpeciesRow(  220, "Wizard Bumblebee"      ),
			new SpeciesRow(  230, "Dumblebee"             ),
			new SpeciesRow(  300, "Nether Bumblebee"      ),
			new SpeciesRow(  310, "Hellish Bumblebee"     ),
			new SpeciesRow(  320, "Impish Bumblebee"      ),
			new SpeciesRow(  330, "Demonic Bumblebee"     ),
			new SpeciesRow(  400, "End Bumblebee"         ),
			new SpeciesRow(  410, "Void Bumblebee"        ),
			new SpeciesRow(  420, "Alienated Bumblebee"   ),
			new SpeciesRow(  430, "Nihilistic Bumblebee"  ),
			new SpeciesRow(  500, "Stoned Bumblebee"      ),
			new SpeciesRow(  510, "Rocking Bumblebee"     ),
			new SpeciesRow(  520, "Hard Rock Bumblebee"   ),
			new SpeciesRow(  530, "Bumbelvis"             ),
			new SpeciesRow(  600, "Jungle Bumblebee"      ),
			new SpeciesRow(  610, "Jungle Bumblebee (T2)" ),
			new SpeciesRow(  620, "Jungle Bumblebee (T3)" ),
			new SpeciesRow(  630, "Bumblezan"             ),
			new SpeciesRow(  700, "Frosty Bumblebee"      ),
			new SpeciesRow(  710, "North Pole Bumblebee"  ),
			new SpeciesRow(  720, "Bumble Elf"            ),
			new SpeciesRow(  730, "Bumble Claus"          ),
			new SpeciesRow(  800, "Bumbleshroom"          ),
			new SpeciesRow(  810, "Bumble Toad"           ),
			new SpeciesRow(  820, "Bumble Bro"            ),
			new SpeciesRow(  830, "Bumble Peach"          ),
			new SpeciesRow(  900, "Sandy Bumblebee"       ),
			new SpeciesRow(  910, "Sandy Bumblebee (T2)"  ),
			new SpeciesRow(  920, "Sandy Bumblebee (T3)"  ),
			new SpeciesRow(  930, "Bumbobee"              ),
			new SpeciesRow(10000, "Creative Bumblebee"    ),
			new SpeciesRow(10010, "Builder Bumblebee"     ),
			new SpeciesRow(10020, "Masonic Bumblebee"     ),
			new SpeciesRow(10030, "Illuminumblebee"       ),
			new SpeciesRow(10100, "Industrial Bumblebee"  ),
			new SpeciesRow(10110, "Overseer Bumblebee"    ),
			new SpeciesRow(10120, "Bumble Tycoon"         ),
			new SpeciesRow(10130, "Monopolistic Bumblebee"),
			new SpeciesRow(10200, "Bumbleknight"          ),
			new SpeciesRow(10210, "Colonial Bumblebee"    ),
			new SpeciesRow(10220, "Royal Bumblebee"       ),
			new SpeciesRow(10230, "Bumblemonarch"         ),
			new SpeciesRow(10300, "Bumblegoth"            ),
			new SpeciesRow(10310, "Occult Bumblebee"      ),
			new SpeciesRow(10320, "Antichristumblebee"    ),
			new SpeciesRow(10330, "Satanic Bumblebee"     ),
			new SpeciesRow(10400, "Forgetful Bumblebee"   ),
			new SpeciesRow(10410, "Amnesic Bumblebee"     ),
			new SpeciesRow(10420, "Bumbleheimers"         ),
			new SpeciesRow(10430, "Bumble in Black"       ),
			new SpeciesRow(10500, "Private Bumble"        ),
			new SpeciesRow(10510, "Lt. Bumbleson"         ),
			new SpeciesRow(10520, "Colonel Bumble O'Beeill"),
			new SpeciesRow(10530, "General Bumblemond"    ),
			new SpeciesRow(20000, "Blazing Bumblebee"     ),
			new SpeciesRow(20010, "Flaming Bumblebee"     ),
			new SpeciesRow(20020, "Bumbletrantor"         ),
			new SpeciesRow(20030, "Pyro Bumble"           ),
			new SpeciesRow(20100, "Blizzful Bumblebee"    ),
			new SpeciesRow(20110, "Freezing Bumblebee"    ),
			new SpeciesRow(20120, "Mr. Bumblefreeze"      ),
			new SpeciesRow(20130, "Cryo Bumble"           ),
			new SpeciesRow(20200, "Blitzing Bumblebee"    ),
			new SpeciesRow(20210, "Storming Bumblebee"    ),
			new SpeciesRow(20220, "Bumbleaang"            ),
			new SpeciesRow(20230, "Aero Bumble"           ),
			new SpeciesRow(20300, "Basalzed Bumblebee"    ),
			new SpeciesRow(20310, "Quakeing Bumblebee"    ),
			new SpeciesRow(20320, "Earth Bound Bumblebee" ),
			new SpeciesRow(20330, "Tera Bumble"           ));

	/** The species row of a code, or null (unknown code). */
	@Nullable
	public static SpeciesRow speciesOf(int aCode) {
		for (SpeciesRow tRow : SPECIES) if (tRow.code() == aCode) return tRow;
		return null;
	}

	// -------------------------------------------------------------------------
	// the code algebra (the meta decimal digits, MultiItemBumbles.java:512)
	// -------------------------------------------------------------------------

	/** {@code meta/100} — the species family (the HiveKind.species trace, the comb key). */
	public static int familyOf(int aCode) {
		return aCode / 100;
	}

	/** {@code (meta/10)%10} — the tier 0-3 (the mutation and product ladders). */
	public static int tierOf(int aCode) {
		return (aCode / 10) % 10;
	}

	/** The bumbleEqual face (:567): same family AND tier — the type digit is not identity. */
	public static boolean sameSpecies(int aCodeA, int aCodeB) {
		return aCodeA / 10 == aCodeB / 10;
	}

	/**
	 * The bumbleMutateChance face (:459-467): 500/500/250/25 per tier of 10000, 0 beyond.
	 */
	public static int mutateChance(int aCode) {
		switch (tierOf(aCode)) {
		case 0: return 500;
		case 1: return 500;
		case 2: return 250;
		case 3: return 25;
		default: return 0;
		}
	}

	/**
	 * The bumbleMutate face (:470-478) on the code: tier 0 climbs +10, tier 3 falls -10,
	 * the middle tiers go either way, an out-of-table code copies. The item face rides the
	 * stack untouched (only princess/drone offspring ever take this walk).
	 */
	public static int mutateCode(int aCode, Random aRandom) {
		switch (tierOf(aCode)) {
		case 0: return aCode + 10;
		case 1: return aCode + (aRandom.nextBoolean() ? 10 : -10);
		case 2: return aCode + (aRandom.nextBoolean() ? 10 : -10);
		case 3: return aCode - 10;
		default: return aCode;
		}
	}

	/** The mutation walk on a stack's code (the Bumbliary :227-229 shape). */
	public static void mutate(ItemStack aStack, Random aRandom) {
		GT6BumbleGenes.setCode(aStack, mutateCode(GT6BumbleGenes.codeOf(aStack), aRandom));
	}

	/**
	 * The getFlowerTooltip words (:351-367), the vanilla+GT6-only trim the card declares —
	 * the mod-conditional wordings (:355/:356/:363) collapse to their base faces. Tooltip
	 * literals (the usb-stick ruling: the 1.7.10 dump carries no zh faces for them, so the
	 * lang surface stays at the name keys).
	 */
	public static String requirementOf(int aFamily) {
		switch (aFamily) {
		case 1:           return "Water";
		case 2:           return "Magical Biome";
		case 3: case 200: return "Netherwart";
		case 4: case 202: return "End Portal, End Biome or Dragon Egg";
		case 5: case 203: return "Stone, Cobble or Mossy";
		case 6:           return "Cocoa";
		case 7: case 201: return "Snow or Ice";
		case 8:           return "Mycelium or Mushrooms";
		case 9: case 105: return "Cacti";
		case 100:         return "Raw Clay Blocks";
		case 101:         return "Rubber Tree Resin Holes";
		case 103:         return "Soul Sand Blocks";
		default:          return "Flowers (even potted ones work)";
		}
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6BeeCombs shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	// -------------------------------------------------------------------------
	// the Bumblelyzer scan display stock (task p34-machines-bumblelyzer-crucible —
	// MultiItemBumbles.make :581-588, the RM.Bumblelyzer.addFakeRecipe walk): the fill
	// hook this card owns. The rows are DISPLAY/CENSUS only — the port RecipeMap.addRecipe
	// (:177, the upstream :293 "findRecipe wont find fake Recipes" javadoc) keeps fake rows
	// out of the findable stock, so they land in the map's {@code sFakeRecipes} list (the
	// JEI display bridge stays pooled, the P12 category card's surface).
	// -------------------------------------------------------------------------

	/**
	 * The fill/scan seams: the live {@code BEE_ITEMS}/honey-family lookups by default, fixtures
	 * injected offline (offline cannot touch the Forge registries — the GT6RecipesBees
	 * resolver convention; the {@link GT6RecipeMapBumblelyzer#sPaperResolver} paper seam
	 * rides the map class).
	 */
	public static java.util.function.IntFunction<net.minecraft.world.item.Item> sBeeItemResolver = aIndex -> BEE_ITEMS.get(aIndex).get();
	public static java.util.function.Function<String, net.minecraft.world.level.material.Fluid> sHoneyFluidResolver = aId -> gregtech6.fluid.GTFluids.liveFluidSource(aId);

	/**
	 * The {@code bumbleType} face (:568) as a seam: the type byte of a stack, or null (a
	 * foreign item) — the live form is the {@code instanceof GT6BumbleItem} walk, the offline
	 * fixtures alias vanilla stand-ins (the frozen registry forbids new mod items).
	 */
	public static java.util.function.Function<ItemStack, Byte> sBumbleType = aStack -> {
		net.minecraft.world.item.Item tItem = aStack.getItem();
		return tItem instanceof GT6BumbleItem tBee ? Byte.valueOf(tBee.typeOf()) : null;
	};

	/**
	 * The {@code bumbleScan} face (:564): the meta+5 copy — the stack flips to the SCANNED
	 * face of its own type with the NBT (the gt.bumble genes) carried over, count preserved.
	 * An already-scanned/foreign stack copies unchanged. Rides the seams above.
	 */
	public static ItemStack bumbleScan(ItemStack aBee) {
		Byte tType = sBumbleType.apply(aBee);
		if (tType == null) return aBee.copy();
		ItemStack rStack = new ItemStack(sBeeItemResolver.apply(faceIndexOf((byte)(tType % 5), true)), aBee.getCount());
		//? if forge {
		if (aBee.getTag() != null) rStack.setTag(aBee.getTag().copy());
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = aBee.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); // the GT6BumbleGenes:113 form
		if (!tTag.isEmpty()) rStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tTag));
		 *///?}
		return rStack;
	}

	/**
	 * Builds the scan display stock: one {@code {honey, honeydew} scan set + the pass-through
	 * duplicate pair} per TYPE face ({@code drone, princess, dead}) per SPECIES (the
	 * {@link #SPECIES} 80-row table = the upstream make() per-speciesID walk; the census is
	 * 80 × 3 × (2 diluents + 2 pass-throughs) = 960 rows).
	 *
	 * <p>The species identity rides the stack NBT ({@code gt.bumble.meta}, the meta
	 * flattening) — the display row for a code carries that code's tag, which is what keeps
	 * the upstream per-species row census meaningful under the flattening.
	 *
	 * <p>Idempotent per generation: an already-filled stock short-circuits (the
	 * upstream per-JVM load flag folded into the stock itself — the map's generation-reset
	 * hook clears it, so a fresh generation refills). Called from GT6RecipesBees.load (the
	 * FMLCommonSetup timing: the maps exist, the items/fluids are registered).
	 */
	public static synchronized void addScanFakeRecipes() {
		if (!GT6RecipeMapBumblelyzer.sFakeRecipes.isEmpty()) return;
		net.minecraft.world.item.Item tPaper = GT6RecipeMapBumblelyzer.sPaperResolver.get();
		List<Recipe> tRows = new java.util.ArrayList<>(SPECIES.size() * 3 * 4);
		for (SpeciesRow tSpecies : SPECIES) {
			for (byte tType : new byte[] {TYPE_DRONE, TYPE_PRINCESS, TYPE_DEAD}) {
				int tUnscanned = faceIndexOf(tType, false), tScanned = faceIndexOf(tType, true);
				if (tUnscanned < 0 || tScanned < 0) continue; // every fractal digit pair exists — unreachable
				net.minecraft.world.item.Item tBee = sBeeItemResolver.apply(tUnscanned), tScannedItem = sBeeItemResolver.apply(tScanned);
				// the :583-585 scan rows — one per honey diluent (the port accept set carries
				// honeydew as a member, so the HONEY loop + the separate Honeydew row fold)
				for (String tFluidId : GT6RecipeMapBumblelyzer.HONEY_ACCEPT) {
					net.minecraft.world.level.material.Fluid tFluid = sHoneyFluidResolver.apply(tFluidId);
					if (tFluid == null || tPaper == null) continue; // :583 FL.exists gate + the paper leg
					tRows.add(new Recipe(false,
							new ItemStack[] {beeStack(tBee, tSpecies), new ItemStack(tPaper, 1)},
							new ItemStack[] {beeStack(tScannedItem, tSpecies)},
							new net.minecraftforge.fluids.FluidStack[] {new net.minecraftforge.fluids.FluidStack(tFluid, GT6RecipeMapBumblelyzer.SCAN_FLUID_L)},
							null, GT6RecipeMapBumblelyzer.SCAN_DURATION, GT6RecipeMapBumblelyzer.SCAN_EUT, 0));
				}
				// the :586-587 pass-through pair — "Was already scanned, auto-skipping" (1 @ 16, no legs)
				for (int i = 0; i < 2; i++) {
					tRows.add(new Recipe(false,
							new ItemStack[] {beeStack(tScannedItem, tSpecies)},
							new ItemStack[] {beeStack(tScannedItem, tSpecies)},
							null, null, GT6RecipeMapBumblelyzer.PASS_DURATION, GT6RecipeMapBumblelyzer.PASS_EUT, 0));
				}
			}
		}
		GT6RecipeMapBumblelyzer.setFakeRecipes(tRows);
	}

	/** The FACES index of a face/scanned pair, or -1 (unreachable — the 8-face table is total). */
	private static int faceIndexOf(byte aFace, boolean aScanned) {
		for (int i = 0; i < FACES.size(); i++) {
			FaceRow tFace = FACES.get(i);
			if (tFace.face() == aFace && tFace.scanned() == aScanned) return i;
		}
		return -1;
	}

	/** One display stack of the species code (the {@code gt.bumble.meta} tag — the flattened meta). */
	private static ItemStack beeStack(net.minecraft.world.item.Item aItem, SpeciesRow aSpecies) {
		ItemStack rStack = new ItemStack(aItem, 1);
		GT6BumbleGenes.setCode(rStack, aSpecies.code());
		return rStack;
	}

	private GT6Bumbles() {
	}
}
