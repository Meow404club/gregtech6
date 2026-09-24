package gregtech6.datagen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The upstream {@code loaders/c/Loader_Loot.java} loot rows as pure datagen data — task
 * p34-loot-injection. THREE faces, all computed through the REGISTRATION-FREE material system
 * (init + {@code registrationOrder()} + {@code itemIdOf} — the headless faces the pin tests
 * replay), so datagen and the offline tests see byte-identical tables:
 * <ul>
 * <li><b>structure injections</b> — the tail rows {@code :410-552}, one
 *     {@link InjectionRow} per ChestGenHooks category, the category → vanilla-table-id
 *     mapping verified against both jars (1.20.1 {@code data/minecraft/loot_tables/chests/},
 *     1.21.1 {@code loot_table/}: {@code DUNGEON_CHEST → chests/simple_dungeon},
 *     {@code MINESHAFT_CORRIDOR → chests/abandoned_mineshaft},
 *     {@code STRONGHOLD_LIBRARY/CROSSING/CORRIDOR → chests/stronghold_*},
 *     {@code PYRAMID_DESERT_CHEST → chests/desert_pyramid},
 *     {@code PYRAMID_JUNGLE_CHEST → chests/jungle_temple},
 *     {@code PYRAMID_JUNGLE_DISPENSER → chests/jungle_temple_dispenser},
 *     {@code VILLAGE_BLACKSMITH → chests/village/village_weaponsmith} — the 1.7.10
 *     blacksmith chest IS the modern weaponsmith shop; {@code BONUS_CHEST →
 *     chests/spawn_bonus_chest}); weight/stack columns verbatim;</li>
 * <li><b>the gt.flawless/gt.gems/gt.misc weight tables</b> ({@code :81-177}) — the upstream
 *     ChestGenHooks BAG categories, ported as the rollable {@code gt6:chests/gt_*} tables
 *     (the bag items themselves are NOT ported — the declared pool below); the 8..24 roll
 *     range is the {@code setMin(8)/setMax(24)} column ({@code :82-83/:107-108/:130-131});</li>
 * <li><b>the declared POOL</b> — rows whose item has no modern registration are NOT emitted
 *     (the upstream {@code addLoot :566-569} invalid-skip face, minus the stderr noise): the
 *     loot bags {@code IL.Bag_Loot_*}, the loot book {@code IL.Book_Loot_MatDict} (task
 *     p38-book-loot-first ruling: the Guide 32765 is registered and its four rows re-armed,
 *     but the MatDict 32766 stays cut — its {@code gt.matdicts} pool is the per-material
 *     DYNAMIC book generator {@code UT.java:769-880}, the p35 dynamic-book CUT class; a
 *     registered MatDict with no pool is a dead item, so the four MatDict rows
 *     {@code :443/:487/:513/:525} ride the same declared cut),
 *     the bottles {@code IL.Bottle_*}, the cans {@code IL.Food_Can_Undefined_6/Bread_6/Chum_4},
 *     {@code IL.Dynamite/Tool_MatchBox_Full/Tool_Lighter_* /Porcelain_Cup/Pill_Cure_All},
 *     the research papers {@code IL.Paper_Magic_Research_*}, the coins
 *     ({@code MultiTileEntityCoin} — the coin MTE is unported), and the whole
 *     {@code :42-49} {@code ChestGenHooksChestReplacer} GT-chest SWAP face (the task-card
 *     trim ruling: 换箱面不移植, 注入面移植). Registration-universe fallout (the
 *     resolver's skip face): the {@code gearGtSmall} bronze/brass rows (:499/:504 — the
 *     {@code gearGtSmall} condition gates on the big-gear prefix and the modern universe
 *     registers no {@code gear_gt_*} for them). Category-level fallout:
 *     {@code STRONGHOLD_CROSSING} (the crate rows are block-family, unported) keeps ZERO
 *     rows and gets no modifier JSON — {@code BONUS_CHEST} and {@code STRONGHOLD_LIBRARY}
 *     carried only book/paper rows before task p38 and now land their Guide rows.</li>
 * </ul>
 */
public final class GT6LootInjectionDatagen {

	/**
	 * One upstream addLoot row: item id, weight (aChance), [min,max] stack. The optional
	 * {@code tag} is the task-p36 artifact lane (the upstream rows never carried NBT; the
	 * ZPM dungeon face does — {@code DungeonData.zpm:306-310} spawns the artifact 2/3 FULL
	 * via the {@code gt.active.energy} store-as-full key).
	 */
	public record EntryRow(String item, int weight, int min, int max, net.minecraft.nbt.CompoundTag tag) {

		/** The NBT-less form every upstream row takes. */
		public EntryRow(String aItem, int aWeight, int aMin, int aMax) {
			this(aItem, aWeight, aMin, aMax, null);
		}
	}

	/** One structure injection: the GLM instance name, the target table id, the roll range, the rows. */
	public record InjectionRow(String name, String table, int rollMin, int rollMax, List<EntryRow> entries) {
	}

	/** One bag weight table ({@code gt6:chests/<name>}); the pool rolls are the fixed 8..24. */
	public record WeightRow(String name, List<EntryRow> entries) {
	}

	/** The roll range — the share-approximation documented on {@code GT6DungeonLootModifier}. */
	public static final int ROLL_MIN = 1, ROLL_MAX = 3;

	/** The bag-table pool rolls — the upstream {@code setMin(8)/setMax(24)} columns. */
	public static final int BAG_ROLL_MIN = 8, BAG_ROLL_MAX = 24;

	/**
	 * The datagen-era item resolution (the datagen JVM has live registries; fail-visible on a
	 * missing id — the rows above come from the registration-checked resolver, so a miss is a
	 * bug, not a skip).
	 */
	//? if forge {
	public static net.minecraft.world.item.Item resolveItem(String aId) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(aId));
	}
	//?} else {
	/*public static net.minecraft.world.item.Item resolveItem(String aId) {
		String[] tParts = aId.split(":", 2);
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(tParts[0], tParts[1]));
	}
	*///?}

	private GT6LootInjectionDatagen() {
	}

	/** The lazily-built registration universe (first use is post-initMaterials, test and datagen alike). */
	private static Set<GTMaterialItems.PrefixMaterial> sRegistered;

	private static Set<GTMaterialItems.PrefixMaterial> registered() {
		if (sRegistered == null) sRegistered = new HashSet<>(GTMaterialItems.registrationOrder());
		return sRegistered;
	}

	/**
	 * The GT material-pair resolver — the upstream {@code addLoot :566-569} skip face over the
	 * modern registration universe (the {@code GTMaterialItems.registrationOrder()} membership,
	 * alias-merged like the bridge). Returns {@code gt6:<prefix>_<material>} or null.
	 */
	private static EntryRow mat(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aWeight, int aMin, int aMax) {
		OreDictMaterial tTarget = aMaterial == null ? null : MaterialRegistry.INSTANCE.get(aMaterial);
		GTMaterialItems.PrefixMaterial tPair = tTarget == null ? null
				: new GTMaterialItems.PrefixMaterial(aPrefix, tTarget);
		if (tPair == null || !registered().contains(tPair)) {
			System.err.println("GT6 loot injection skipped unregistered pair: " + aPrefix.mNameInternal + " x "
					+ (aMaterial == null ? "null" : aMaterial.mNameInternal) + " (the upstream addLoot skip face)");
			return null;
		}
		return new EntryRow("gt6:" + GTMaterialItems.itemIdOf(aPrefix, tTarget), aWeight, aMin, aMax);
	}

	/** The vanilla item row ({@code ST.make(Items.x, ...)} upstream). */
	private static EntryRow van(String aId, int aWeight, int aMin, int aMax) {
		return new EntryRow("minecraft:" + aId, aWeight, aMin, aMax);
	}

	/** The four-metal ladder — {@code :418-433}: ingot/plate/stick [2,12]/toolHeadArrow [4,24]. */
	private static Stream<EntryRow> dungeonMetalLadder(int aCommon, int aDamascus) {
		return Stream.of(
				mat(OP.ingot, MT.Steel, aCommon, 1, 6), mat(OP.ingot, MT.Bronze, aCommon, 1, 6),
				mat(OP.ingot, MT.Brass, aCommon, 1, 6), mat(OP.ingot, MT.DamascusSteel, aDamascus, 1, 6),
				mat(OP.plate, MT.Steel, aCommon, 1, 6), mat(OP.plate, MT.Bronze, aCommon, 1, 6),
				mat(OP.plate, MT.Brass, aCommon, 1, 6), mat(OP.plate, MT.DamascusSteel, aDamascus, 1, 6),
				mat(OP.stick, MT.Steel, aCommon, 2, 12), mat(OP.stick, MT.Bronze, aCommon, 2, 12),
				mat(OP.stick, MT.Brass, aCommon, 2, 12), mat(OP.stick, MT.DamascusSteel, aDamascus, 2, 12),
				mat(OP.toolHeadArrow, MT.Steel, aCommon, 4, 24), mat(OP.toolHeadArrow, MT.Bronze, aCommon, 4, 24),
				mat(OP.toolHeadArrow, MT.Brass, aCommon, 4, 24), mat(OP.toolHeadArrow, MT.DamascusSteel, aDamascus, 4, 24));
	}

	/**
	 * The structure injections — the tail rows per category, weight/stack columns verbatim;
	 * null rows (unregistered pairs) filtered.
	 */
	public static List<InjectionRow> injections() {
		List<InjectionRow> rRows = new ArrayList<>();
		// BONUS_CHEST :413 — the Guide row (the bottles :410-412 stay pooled); task p38 re-arms
		// the book face, so the bonus chest gets its modifier JSON (chests/spawn_bonus_chest,
		// the mapping verified against both jars)
		rRows.add(new InjectionRow("dungeon_inject_spawn_bonus_chest", "minecraft:chests/spawn_bonus_chest",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(guideRow(10, 8, 16)))));
		// DUNGEON_CHEST :418-443 — the metal ladder + coins(40/20/10)/bags/bottle POOLED + the
		// Guide :442 + the task-p36 artifact row: the upstream obtainment face is the GT6
		// DUNGEON ROOM (DungeonChunkRoomLibraryNormal.java:57/59/71 — 1/16 per shelf seat,
		// DungeonData.zpm spawning the ZPM 2/3 FULL); the port has no GT6 dungeon carrier, so
		// the vanilla dungeon chest carries the artifact at the always-full collapse (an empty
		// ZPM is an unchargeable dead drop) — the declared deviation on the card face.
		// (the MatDict :443 stays pooled — the class javadoc ruling)
		rRows.add(new InjectionRow("dungeon_inject_simple_dungeon", "minecraft:chests/simple_dungeon",
				ROLL_MIN, ROLL_MAX, ladder(java.util.stream.Stream.concat(dungeonMetalLadder(12, 2),
						java.util.stream.Stream.of(guideRow(50, 2, 8), zpmArtifactRow())))));
		// PYRAMID_DESERT_CHEST :445-450 — holy water/coins/bags POOLED, the Nq arrow head lands
		rRows.add(new InjectionRow("dungeon_inject_desert_pyramid", "minecraft:chests/desert_pyramid",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(mat(OP.toolHeadArrow, MT.Nq, 1, 4, 16)))));
		// PYRAMID_JUNGLE_CHEST :452-461 — AsCu ladder :452-454 + the Ke arrow :455; coins/bags POOLED
		rRows.add(new InjectionRow("dungeon_inject_jungle_temple", "minecraft:chests/jungle_temple",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(
						mat(OP.ingot, MT.ArsenicCopper, 3, 4, 16), mat(OP.plate, MT.ArsenicCopper, 3, 4, 16),
						mat(OP.toolHeadArrow, MT.ArsenicCopper, 3, 16, 64), mat(OP.toolHeadArrow, MT.Ke, 1, 4, 16)))));
		// PYRAMID_JUNGLE_DISPENSER :463-465 — the fire charges + the wood arrows
		rRows.add(new InjectionRow("dungeon_inject_jungle_temple_dispenser", "minecraft:chests/jungle_temple_dispenser",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(
						van("fire_charge", 30, 2, 8),
						mat(OP.arrowGtWood, MT.DamascusSteel, 20, 8, 16), mat(OP.arrowGtWood, MT.Ke, 1, 8, 16)))));
		// MINESHAFT_CORRIDOR :468-487 — the ore-block rows :470-476 + the dig heads :477-482;
		// bottles/matchbox/coins/bags POOLED, the MatDict :487 stays pooled (the class javadoc)
		rRows.add(new InjectionRow("dungeon_inject_abandoned_mineshaft", "minecraft:chests/abandoned_mineshaft",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(
						van("coal_ore", 4, 16, 64), van("iron_ore", 4, 16, 64), van("gold_ore", 2, 8, 32),
						van("lapis_ore", 2, 8, 32), van("redstone_ore", 2, 8, 32),
						van("diamond_ore", 1, 4, 16), van("emerald_ore", 1, 4, 16),
						mat(OP.toolHeadShovel, MT.ArsenicBronze, 5, 1, 4), mat(OP.toolHeadShovel, MT.Steel, 3, 1, 4),
						mat(OP.toolHeadShovel, MT.DamascusSteel, 1, 1, 4),
						mat(OP.toolHeadPickaxe, MT.ArsenicBronze, 5, 1, 4), mat(OP.toolHeadRawPickaxe, MT.Steel, 3, 1, 4),
						mat(OP.toolHeadPickaxe, MT.DamascusSteel, 1, 1, 4)))));
		// VILLAGE_BLACKSMITH :489-513 — the smith ladder :491-506 + the Guide :512; bottles/
		// coins/bags POOLED, the MatDict :513 stays pooled (the class javadoc)
		rRows.add(new InjectionRow("dungeon_inject_village_weaponsmith", "minecraft:chests/village/village_weaponsmith",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(
						mat(OP.ingot, MT.Steel, 2, 4, 12), mat(OP.plate, MT.Steel, 2, 4, 12),
						mat(OP.stick, MT.Steel, 2, 8, 24), mat(OP.gearGtSmall, MT.Steel, 2, 4, 12),
						mat(OP.toolHeadArrow, MT.Steel, 2, 16, 48),
						mat(OP.ingot, MT.Bronze, 2, 4, 12), mat(OP.plate, MT.Bronze, 2, 4, 12),
						mat(OP.stick, MT.Bronze, 2, 8, 24), mat(OP.gearGtSmall, MT.Bronze, 2, 4, 12),
						mat(OP.toolHeadArrow, MT.Bronze, 2, 16, 48),
						mat(OP.ingot, MT.Brass, 2, 4, 12), mat(OP.plate, MT.Brass, 2, 4, 12),
						mat(OP.stick, MT.Brass, 2, 8, 24), mat(OP.gearGtSmall, MT.Brass, 2, 4, 12),
						mat(OP.toolHeadArrow, MT.Brass, 2, 16, 48),
						mat(OP.ingot, MT.DamascusSteel, 1, 4, 12),
						guideRow(40, 4, 8)))));
		// STRONGHOLD_LIBRARY :515-525 — the Guide :524; the research papers :515-523 stay
		// pooled, the MatDict :525 stays pooled (the class javadoc) — task p38 gives the
		// library its modifier JSON (chests/stronghold_library, both jars verified)
		rRows.add(new InjectionRow("dungeon_inject_stronghold_library", "minecraft:chests/stronghold_library",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(guideRow(40, 4, 8)))));
		// STRONGHOLD_CORRIDOR :546-552 — the weapon heads + the arrows; coins POOLED
		rRows.add(new InjectionRow("dungeon_inject_stronghold_corridor", "minecraft:chests/stronghold_corridor",
				ROLL_MIN, ROLL_MAX, ladder(Stream.of(
						mat(OP.toolHeadSword, MT.Steel, 12, 1, 4), mat(OP.toolHeadSword, MT.DamascusSteel, 6, 1, 4),
						mat(OP.toolHeadAxeDouble, MT.Steel, 12, 1, 4), mat(OP.toolHeadAxeDouble, MT.DamascusSteel, 6, 1, 4),
						mat(OP.arrowGtWood, MT.DamascusSteel, 6, 16, 48), mat(OP.arrowGtWood, MT.SterlingSilver, 6, 8, 24)))));
		return rRows;
	}

	/**
	 * The Dusty Guide Book row (task p38-book-loot-first) — the registered
	 * {@code gt6:book_loot_guide} carrier (MultiItemBooks.java:67 meta 32765); the
	 * weight/stack columns ride verbatim per table (:413/:442/:512/:524).
	 */
	private static EntryRow guideRow(int aWeight, int aMin, int aMax) {
		return new EntryRow("gt6:book_loot_guide", aWeight, aMin, aMax);
	}

	/**
	 * The ZPM artifact row — weight 2 [1,1] (the rare-roll posture) carrying the FULL tag:

	 * the {@code gt.active.energy} store-as-full key (the DungeonData.zpm active lane;
	 * the 2/3 dice collapse to always-full, the declared deviation above).
	 */
	private static EntryRow zpmArtifactRow() {
		net.minecraft.nbt.CompoundTag tTag = new net.minecraft.nbt.CompoundTag();
		tTag.putBoolean(gregtech6.item.energy.GT6BatteryItem.NBT_ACTIVE_ENERGY, true);
		return new EntryRow("gt6:zpm", 2, 1, 1, tTag);
	}

	/** The non-null filter keeping the upstream row order. */
	private static List<EntryRow> ladder(Stream<EntryRow> aRows) {
		return aRows.filter(aRow -> aRow != null).toList();
	}

	/**
	 * The three bag weight tables — {@code gt.flawless :81-103}, {@code gt.gems :106-126}
	 * (the {@code RANDOM_SMALL_GEM_ORE} material loop :122-126 included), {@code gt.misc
	 * :129-177} (records/discs/billets/redstone/skystone/zeolite/meteoric/blaze-sticks; the
	 * cans/dynamite/matchbox/lighters/cup/pill rows and {@code chemtube Mcg} left to the
	 * resolver's skip face when the modern universe lacks them).
	 */
	public static List<WeightRow> weightTables() {
		// gt.flawless :84-103 — every row [1,1]
		List<EntryRow> tFlawless = ladder(Stream.of(
				mat(OP.gemFlawless, MT.Diamond, 2160, 1, 1), mat(OP.gemFlawless, MT.DiamondPink, 144, 1, 1),
				mat(OP.gemFlawless, MT.Emerald, 1152, 1, 1), mat(OP.gemFlawless, MT.Aquamarine, 432, 1, 1),
				mat(OP.gemFlawless, MT.Morganite, 144, 1, 1), mat(OP.gemFlawless, MT.Heliodor, 144, 1, 1),
				mat(OP.gemFlawless, MT.Goshenite, 144, 1, 1), mat(OP.gemFlawless, MT.Bixbite, 144, 1, 1),
				mat(OP.gemFlawless, MT.Maxixe, 144, 1, 1), mat(OP.gemFlawless, MT.Ruby, 720, 1, 1),
				mat(OP.gemFlawless, MT.BlueSapphire, 576, 1, 1), mat(OP.gemFlawless, MT.GreenSapphire, 576, 1, 1),
				mat(OP.gemFlawless, MT.PurpleSapphire, 144, 1, 1), mat(OP.gemFlawless, MT.YellowSapphire, 144, 1, 1),
				mat(OP.gemFlawless, MT.OrangeSapphire, 144, 1, 1), mat(OP.gemFlawless, MT.Craponite, 144, 1, 1),
				mat(OP.gemFlawless, MT.Amethyst, 576, 1, 1), mat(OP.gemFlawless, MT.Amber, 576, 1, 1),
				mat(OP.gemFlawless, MT.Jade, 576, 1, 1), mat(OP.gemFlawless, MT.Redstone, 432, 1, 1)));
		// gt.gems :109-126 — the emerald/diamond families then the RANDOM_SMALL_GEM_ORE loop
		Stream<EntryRow> tGemsFixed = Stream.of(
				mat(OP.gem, MT.Emerald, 9216, 1, 4), mat(OP.gem, MT.Diamond, 2160, 1, 4),
				mat(OP.gemFlawed, MT.Diamond, 2160, 2, 8), mat(OP.gemChipped, MT.Diamond, 2160, 4, 16),
				mat(OP.gem, MT.DiamondPink, 144, 1, 4), mat(OP.gemFlawed, MT.DiamondPink, 144, 2, 8),
				mat(OP.gemChipped, MT.DiamondPink, 144, 4, 16),
				mat(OP.gem, MT.Craponite, 144, 1, 4), mat(OP.gemFlawed, MT.Craponite, 144, 2, 8),
				mat(OP.gemChipped, MT.Craponite, 144, 4, 16),
				mat(OP.gem, MT.Amber, 144, 1, 4), mat(OP.gemFlawed, MT.Amber, 144, 2, 8),
				mat(OP.gemChipped, MT.Amber, 144, 4, 16));
		List<EntryRow> tGems = ladder(Stream.concat(tGemsFixed, randomSmallGemLoop()));
		// gt.misc :132-177
		List<EntryRow> tMisc = ladder(Stream.concat(Stream.of(
				van("name_tag", 144, 1, 4), van("leather", 144, 2, 8), van("flint", 144, 2, 8),
				// the twelve music discs :135-146, each weight 13 [1,1]
				van("music_disc_13", 13, 1, 1), van("music_disc_cat", 13, 1, 1), van("music_disc_blocks", 13, 1, 1),
				van("music_disc_chirp", 13, 1, 1), van("music_disc_far", 13, 1, 1), van("music_disc_mall", 13, 1, 1),
				van("music_disc_mellohi", 13, 1, 1), van("music_disc_stal", 13, 1, 1), van("music_disc_strad", 13, 1, 1),
				van("music_disc_ward", 13, 1, 1), van("music_disc_11", 13, 1, 1), van("music_disc_wait", 13, 1, 1),
				// the billet ladder :147-158
				mat(OP.billet, MT.Nd, 144, 3, 12), mat(OP.billet, MT.Cr, 144, 3, 12), mat(OP.billet, MT.Mn, 144, 3, 12),
				mat(OP.billet, MT.Ni, 144, 3, 12), mat(OP.billet, MT.Sb, 144, 3, 12), mat(OP.billet, MT.Sn, 144, 3, 12),
				mat(OP.billet, MT.Zn, 144, 3, 12), mat(OP.billet, MT.Cu, 144, 3, 12), mat(OP.billet, MT.Ag, 144, 3, 12),
				mat(OP.billet, MT.Au, 144, 3, 12), mat(OP.billet, MT.Pt, 144, 3, 12), mat(OP.billet, MT.Pb, 144, 3, 12),
				// the dust/rock/ore rows :159-163
				mat(OP.dust, MT.Redstone, 144, 3, 12), mat(OP.rockGt, MT.STONES.SkyStone, 144, 3, 12),
				mat(OP.dust, MT.OREMATS.Zeolite, 144, 1, 2), mat(OP.rockGt, MT.MeteoricIron, 72, 1, 4),
				mat(OP.oreRaw, MT.MeteoricIron, 72, 1, 1),
				// the blaze sticks :164-167
				mat(OP.stick, MT.Blizz, 36, 2, 8), mat(OP.stick, MT.Blitz, 36, 2, 8),
				mat(OP.stick, MT.Basalz, 36, 2, 8), mat(OP.stick, MT.Breeze, 36, 2, 8)),
				// :177 chemtube Mcg — the resolver skips when the modern universe lacks it
				Stream.of(mat(OP.chemtube, MT.Mcg, 36, 1, 2))));
		return List.of(
				new WeightRow("gt_flawless", tFlawless),
				new WeightRow("gt_gems", tGems),
				new WeightRow("gt_misc", tMisc));
	}

	/**
	 * The {@code RANDOM_SMALL_GEM_ORE} material loop ({@code :122-126}): gem [1,4] / flawed
	 * [2,8] / chipped [4,16] at weight 144 per qualifying material, alias-merged, registration
	 * order.
	 */
	private static Stream<EntryRow> randomSmallGemLoop() {
		List<EntryRow> rRows = new ArrayList<>();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0 || !tMaterial.contains(TD.Properties.RANDOM_SMALL_GEM_ORE)) continue;
			tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge, MaterialRegistry.java:182-185
			if (tMaterial == null || tMaterial.mID < 0) continue;
			// per-prefix membership — the gem tiers gate per material (the gemFlawed
			// red_fluorite lesson: the gem pair can register while the flawed pair cannot)
			gemRow(rRows, OP.gem, tMaterial, 144, 1, 4); // :123
			gemRow(rRows, OP.gemFlawed, tMaterial, 144, 2, 8); // :124
			gemRow(rRows, OP.gemChipped, tMaterial, 144, 4, 16); // :125
		}
		return rRows.stream();
	}

	/** One loop row, gated on its own prefix x material membership. */
	private static void gemRow(List<EntryRow> aRows, OreDictPrefix aPrefix, OreDictMaterial aMaterial,
			int aWeight, int aMin, int aMax) {
		if (!registered().contains(new GTMaterialItems.PrefixMaterial(aPrefix, aMaterial))) return;
		aRows.add(new EntryRow("gt6:" + GTMaterialItems.itemIdOf(aPrefix, aMaterial), aWeight, aMin, aMax));
	}
}
