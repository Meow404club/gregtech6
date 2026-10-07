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
 * loot-injection. THREE faces, all computed through the REGISTRATION-FREE material system
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
 *     book-loot-first ruling: the Guide 32765 is registered and its four rows re-armed,
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
 * <li><b>the Twilight Forest treasure injections</b> — task twilight-treasure-loot: the
 *     {@code Loader_Loot.java:56-78} TFTreasure mapping re-armed as GLM injections over the
 *     TF 1.20.x loot table ids (harvest {@code TFLootTables.java:34-63}, the 1:1 names).
 *     <b>Ruling chain</b>: p34 ruled the treasure face 换箱面不移植 (KG 3037) — SUPERSEDED
 *     2026-10-07 by the main session (the user's 2026-10-04 mod-adaptation ruling stands):
 *     the ported face is loot INJECTION into the TF tables, never the GT-chest swap (the
 *     {@code TwilightTreasureReplacer.generate(World..) :297-339} placeBlock face stays cut)
 *     and never a TF-table override. The upstream semantic per table is the vanilla-category
 *     EXTRA roll ({@code generate(IInventory) :355}, {@code ChestGenHooks.getOneItem(mVanillacategory)})
 *     — re-armed as the SAME rows the mapped vanilla table already carries, plus the
 *     per-index hand-tuned GT additions that survived the existence check. The TF-absent
 *     guard is the table-id seam itself: {@code GT6DungeonLootModifier} fires only when the
 *     queried table id matches, and without TF no roll ever queries a
 *     {@code twilightforest:} table — the JSONs load inert (conditions stay empty, the
 *     no-branded-keys contract). The dead-pool fact ({@code :346-357} rolls ONLY
 *     rare/uncommon/common + the TC bag + the category — the {@code useless} and
 *     {@code ultrarare} pools are never touched by the GT replacer) cuts every
 *     {@code useless.add} hand row (the tower-room nether junk, the ender pearls, the ink
 *     family) and the ultrarare Death Compass; {@link #twilightLedger()} carries the full
 *     22-row reconciliation including the three no-JSON rows.</li>
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
				ROLL_MIN, ROLL_MAX, bonusEntries()));
		// DUNGEON_CHEST :418-443 — the metal ladder + coins(40/20/10)/bags/bottle POOLED + the
		// Guide :442. The ZPM artifact row LEFT this vanilla face (task dungeon-library-zpm):
		// the port HAS the GT6 dungeon now, so the artifact rides the dungeon loot face — the
		// gt6:chests/dungeon_chest injection at the tail of this method.
		// (the MatDict :443 stays pooled — the class javadoc ruling)
		rRows.add(new InjectionRow("dungeon_inject_simple_dungeon", "minecraft:chests/simple_dungeon",
				ROLL_MIN, ROLL_MAX, dungeonChestEntries()));
		// PYRAMID_DESERT_CHEST :445-450 — holy water/coins/bags POOLED, the Nq arrow head lands
		rRows.add(new InjectionRow("dungeon_inject_desert_pyramid", "minecraft:chests/desert_pyramid",
				ROLL_MIN, ROLL_MAX, desertEntries()));
		// PYRAMID_JUNGLE_CHEST :452-461 — AsCu ladder :452-454 + the Ke arrow :455; coins/bags POOLED
		rRows.add(new InjectionRow("dungeon_inject_jungle_temple", "minecraft:chests/jungle_temple",
				ROLL_MIN, ROLL_MAX, jungleEntries()));
		// PYRAMID_JUNGLE_DISPENSER :463-465 — the fire charges + the wood arrows
		rRows.add(new InjectionRow("dungeon_inject_jungle_temple_dispenser", "minecraft:chests/jungle_temple_dispenser",
				ROLL_MIN, ROLL_MAX, dispenserEntries()));
		// MINESHAFT_CORRIDOR :468-487 — the ore-block rows :470-476 + the dig heads :477-482;
		// bottles/matchbox/coins/bags POOLED, the MatDict :487 stays pooled (the class javadoc)
		rRows.add(new InjectionRow("dungeon_inject_abandoned_mineshaft", "minecraft:chests/abandoned_mineshaft",
				ROLL_MIN, ROLL_MAX, mineshaftEntries()));
		// VILLAGE_BLACKSMITH :489-513 — the smith ladder :491-506 + the Guide :512; bottles/
		// coins/bags POOLED, the MatDict :513 stays pooled (the class javadoc)
		rRows.add(new InjectionRow("dungeon_inject_village_weaponsmith", "minecraft:chests/village/village_weaponsmith",
				ROLL_MIN, ROLL_MAX, smithEntries()));
		// STRONGHOLD_LIBRARY :515-525 — the Guide :524; the research papers :515-523 stay
		// pooled, the MatDict :525 stays pooled (the class javadoc) — task p38 gives the
		// library its modifier JSON (chests/stronghold_library, both jars verified)
		rRows.add(new InjectionRow("dungeon_inject_stronghold_library", "minecraft:chests/stronghold_library",
				ROLL_MIN, ROLL_MAX, libraryEntries()));
		// STRONGHOLD_CORRIDOR :546-552 — the weapon heads + the arrows; coins POOLED
		rRows.add(new InjectionRow("dungeon_inject_stronghold_corridor", "minecraft:chests/stronghold_corridor",
				ROLL_MIN, ROLL_MAX, corridorEntries()));
		// the GT6 dungeon loot face (task dungeon-library-zpm) — the ZPM artifact row's NEW
		// carrier: the upstream obtainment IS the GT6 dungeon Library room
		// (DungeonChunkRoomLibraryNormal.java:57/59/71/:85/:87/:99/:101 — 1/16 per trophy
		// seat, DungeonData.zpm :306-311 spawning 2/3 FULL), and the port has the dungeon
		// since dungeon-framework, so the artifact leaves the vanilla simple_dungeon
		// stopgap and rides the GT6 dungeon's own chests (gt6:chests/dungeon_chest — the
		// table the Library + storage chests bind). The rows mirror the carrier rows + the
		// artifact (a single-row ladder would fire 1..3 guaranteed ZPMs per chest — the
		// dilution IS the rarity); the tag lane stays on the GT6DungeonLootModifier codec,
		// keeping the plain loot-table JSON off the set_nbt seam (the framework ruling);
		// the 2/3 dice stay collapsed to always-full (the p34 declared deviation).
		rRows.add(new InjectionRow("dungeon_inject_gt6_dungeon_chest", "gt6:chests/dungeon_chest",
				ROLL_MIN, ROLL_MAX, ladder(java.util.stream.Stream.concat(dungeonChestEntries().stream(),
						java.util.stream.Stream.of(zpmArtifactRow())))));
		// the Twilight Forest treasure face (task twilight-treasure-loot) — the :56-78
		// TFTreasure → category mapping, tail-append order
		rRows.addAll(twilightInjections());
		return rRows;
	}

	/** The BONUS_CHEST category rows ({@code :413}) — the vanilla bonus chest + the TF hedge maze/tree cache. */
	public static List<EntryRow> bonusEntries() {
		return ladder(Stream.of(guideRow(10, 8, 16)));
	}

	/** The PYRAMID_DESERT_CHEST category rows ({@code :445-450}) — the vanilla desert pyramid + the TF tower room. */
	public static List<EntryRow> desertEntries() {
		return ladder(Stream.of(mat(OP.toolHeadArrow, MT.Nq, 1, 4, 16)));
	}

	/** The PYRAMID_JUNGLE_CHEST category rows ({@code :452-461}) — the vanilla jungle temple + the TF labyrinth rooms. */
	public static List<EntryRow> jungleEntries() {
		return ladder(Stream.of(
				mat(OP.ingot, MT.ArsenicCopper, 3, 4, 16), mat(OP.plate, MT.ArsenicCopper, 3, 4, 16),
				mat(OP.toolHeadArrow, MT.ArsenicCopper, 3, 16, 64), mat(OP.toolHeadArrow, MT.Ke, 1, 4, 16)));
	}

	/** The PYRAMID_JUNGLE_DISPENSER category rows ({@code :463-465}) — the vanilla dispenser + the TF darktower boss. */
	public static List<EntryRow> dispenserEntries() {
		return ladder(Stream.of(
				van("fire_charge", 30, 2, 8),
				mat(OP.arrowGtWood, MT.DamascusSteel, 20, 8, 16), mat(OP.arrowGtWood, MT.Ke, 1, 8, 16)));
	}

	/** The MINESHAFT_CORRIDOR category rows ({@code :468-487}) — the vanilla mineshaft + the TF hollow hills. */
	public static List<EntryRow> mineshaftEntries() {
		return ladder(Stream.of(
				van("coal_ore", 4, 16, 64), van("iron_ore", 4, 16, 64), van("gold_ore", 2, 8, 32),
				van("lapis_ore", 2, 8, 32), van("redstone_ore", 2, 8, 32),
				van("diamond_ore", 1, 4, 16), van("emerald_ore", 1, 4, 16),
				mat(OP.toolHeadShovel, MT.ArsenicBronze, 5, 1, 4), mat(OP.toolHeadShovel, MT.Steel, 3, 1, 4),
				mat(OP.toolHeadShovel, MT.DamascusSteel, 1, 1, 4),
				mat(OP.toolHeadPickaxe, MT.ArsenicBronze, 5, 1, 4), mat(OP.toolHeadRawPickaxe, MT.Steel, 3, 1, 4),
				mat(OP.toolHeadPickaxe, MT.DamascusSteel, 1, 1, 4)));
	}

	/** The VILLAGE_BLACKSMITH category rows ({@code :489-513}) — the vanilla weaponsmith + the TF smith-class vaults. */
	public static List<EntryRow> smithEntries() {
		return ladder(Stream.of(
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
				guideRow(40, 4, 8)));
	}

	/** The STRONGHOLD_LIBRARY category rows ({@code :515-525}) — the vanilla library + the TF lich tower library. */
	public static List<EntryRow> libraryEntries() {
		return ladder(Stream.of(guideRow(40, 4, 8)));
	}

	/** The STRONGHOLD_CORRIDOR category rows ({@code :546-552}) — the vanilla corridor + the TF darktower/stronghold caches. */
	public static List<EntryRow> corridorEntries() {
		return ladder(Stream.of(
				mat(OP.toolHeadSword, MT.Steel, 12, 1, 4), mat(OP.toolHeadSword, MT.DamascusSteel, 6, 1, 4),
				mat(OP.toolHeadAxeDouble, MT.Steel, 12, 1, 4), mat(OP.toolHeadAxeDouble, MT.DamascusSteel, 6, 1, 4),
				mat(OP.arrowGtWood, MT.DamascusSteel, 6, 16, 48), mat(OP.arrowGtWood, MT.SterlingSilver, 6, 8, 24)));
	}

	/**
	 * The DUNGEON_CHEST category rows (Loader_Loot.java:418-443 对位) — ONE source for
	 * the carriers: the vanilla {@code chests/simple_dungeon} injection (above) and the
	 * {@code gt6:chests/dungeon_chest} carrier table the GT6 dungeon structure's chests
	 * bind (task dungeon-framework), whose injection modifier now carries the
	 * artifact row too (task dungeon-library-zpm — the rows mirror + the ZPM tail). The
	 * plain-tag artifact row stays OUT of the table JSON itself (keeps this table off
	 * the set_nbt/set_custom_data dual-leg seam, the framework ruling).
	 */
	public static List<EntryRow> dungeonChestEntries() {
		return ladder(java.util.stream.Stream.concat(dungeonMetalLadder(12, 2),
				java.util.stream.Stream.of(guideRow(50, 2, 8))));
	}

	/**
	 * One row of the 22-class {@code Loader_Loot.java:56-78} TFTreasure ledger — the
	 * twilight-treasure-loot ACCEPTANCE reconciliation. {@code table} is the TF 1.20.x loot
	 * table id (harvest {@code TFLootTables.java:34-63}) or {@code null} for a no-JSON row
	 * (the {@code note} carries the disposition).
	 */
	public record TwilightRow(int index, String upstream, String category, String table, String note) {
	}

	/**
	 * The FULL 22-row reconciliation, upstream {@code :56-78} order — 19 injections + the
	 * three no-JSON rows. The category column is the upstream vanilla-ChestGenHooks mapping;
	 * the table ids verified against the TF 1.20.x harvest tree
	 * ({@code data/twilightforest/loot_tables/<name>.json} — {@code TFLootTables.register}
	 * prefixes with {@code twilightforest:} directly, no {@code structures/} segment).
	 * Non-mapped modern-only TF tables (the jackpot/with-lamp variants, hedge_cloth, wells,
	 * graveyard, ...) have no 1.7.10 counterpart and stay out by construction (SPEC: 不
	 * override TF 原表 — additions only, verbatim mapping).
	 */
	public static List<TwilightRow> twilightLedger() {
		return List.of(
				new TwilightRow(1, "hill1", "MINESHAFT_CORRIDOR", "twilightforest:hill_1", ""),
				new TwilightRow(2, "hill2", "MINESHAFT_CORRIDOR", "twilightforest:hill_2", ""),
				new TwilightRow(3, "hill3", "MINESHAFT_CORRIDOR", "twilightforest:hill_3", ""),
				new TwilightRow(4, "hedgemaze", "BONUS_CHEST", "twilightforest:hedge_maze", ""),
				new TwilightRow(14, "tree_cache", "BONUS_CHEST", "twilightforest:tree_cache", ""),
				// the mapped category kept zero rows (:528-544 the crates are the declared
				// pool) AND the hand rows are all cut (:176 the Guide book is the GT6Books cut,
				// :177 the loot bags are the declared pool) — no rows, no JSON
				new TwilightRow(9, "basement", "STRONGHOLD_CROSSING", null,
						"zero surviving rows: the crossing crates + the coins/bags are the declared pool, the Manual_Portal_TF + Bag_Loot_Misc hand rows cut"),
				new TwilightRow(5, "labyrinth_room", "PYRAMID_JUNGLE_CHEST", "twilightforest:labyrinth_room", ""),
				new TwilightRow(6, "labyrinth_deadend", "PYRAMID_JUNGLE_CHEST", "twilightforest:labyrinth_dead_end", ""),
				new TwilightRow(10, "labyrinth_vault", "VILLAGE_BLACKSMITH", "twilightforest:labyrinth_vault", ""),
				new TwilightRow(7, "tower_room", "PYRAMID_DESERT_CHEST", "twilightforest:tower_room", ""),
				new TwilightRow(8, "tower_library", "STRONGHOLD_LIBRARY", "twilightforest:tower_library", ""),
				new TwilightRow(11, "darktower_cache", "STRONGHOLD_CORRIDOR", "twilightforest:darktower_cache", ""),
				new TwilightRow(12, "darktower_key", "DUNGEON_CHEST", "twilightforest:darktower_key", ""),
				new TwilightRow(13, "darktower_boss", "PYRAMID_JUNGLE_DISPENSER", "twilightforest:darktower_boss", ""),
				new TwilightRow(15, "stronghold_cache", "STRONGHOLD_CORRIDOR", "twilightforest:stronghold_cache", ""),
				new TwilightRow(16, "stronghold_room", "DUNGEON_CHEST", "twilightforest:stronghold_room", ""),
				// no modern chest table: TFLootTables 1.20.x has cache/room only — the Knight
				// Phantom loot rides entities/knight_phantom_defeated (an entity-drop face, out
				// of the chest seam)
				new TwilightRow(17, "stronghold_boss", "VILLAGE_BLACKSMITH", null,
						"no modern chest table — the Knight Phantom loot rides twilightforest:entities/knight_phantom_defeated"),
				new TwilightRow(18, "aurora_cache", "DUNGEON_CHEST", "twilightforest:aurora_cache", ""),
				new TwilightRow(19, "aurora_room", "DUNGEON_CHEST", "twilightforest:aurora_room", ""),
				// upstream :75 commented out — "This one is actually empty and unused"
				new TwilightRow(20, "aurora_boss", "DUNGEON_CHEST", null,
						"upstream :75 commented out — the table is empty and unused"),
				new TwilightRow(21, "troll_garden", "DUNGEON_CHEST", "twilightforest:troll_garden", ""),
				new TwilightRow(22, "troll_vault", "VILLAGE_BLACKSMITH", "twilightforest:troll_vault", ""));
	}

	/**
	 * The twilight injections — one {@link InjectionRow} per ledger row with a table, in
	 * ledger order (upstream {@code :56-78} order). Rows = the mapped category's GT rows
	 * (the SAME lists the vanilla injections carry — the {@code :355 getOneItem} semantic)
	 * + the hand-tuned additions that survived the existence check, at weight 1 (the
	 * WeightedRandom pool-entry default; the merged-pool weighting is the standing
	 * share-approximation declared on {@code GT6DungeonLootModifier}). The hand-row audit
	 * ({@code TwilightTreasureReplacer} per index, the dead-pool rule first):
	 * <ul>
	 * <li>PORTED: hill2 rare stick-Basalz [4,4] :83; hedgemaze rare name-tag [4,4] / lead
	 *     [2,2] :119-120 + stick-Breeze [4,4] :123 + uncommon food-cinnamon [12,12] :116
	 *     ({@code gt6:food_cinnamon}, the GT6CropFoods row); troll_garden uncommon
	 *     stick-Blitz [4,4] :275; troll_vault common/uncommon crushed-AncientDebris [4,4]/[8,8]
	 *     :287-288 (the 1.7.10 IL.Ancient_Debris was null — the {@code get(n, crushed)}
	 *     fallback WAS the live face) + the wither-skeleton skull [2,2] :289;</li>
	 * <li>DEAD POOL (never rolled by the replacer, {@code :346-357}): every
	 *     {@code useless.add} row — the tower-room nether supplies :144-149, the
	 *     darktower quartz/end-stone :194-197, the ender pearls :210/:267, the ink
	 *     family :158-162, the debris :286 — TF's own tables carry the junk face now;</li>
	 * <li>CUT (no port registration — the declared pool, 勿硬造): the TC loot bag
	 *     (mLootBag, every table), the loot bags Bag_Loot_* (:117/:121/:177/:185/:233-234),
	 *     the Manual_Portal_TF book (:164/:176 — the GT6Books cut), the ultrarare
	 *     Compass_Death (:166 — unported AND the ultrarare pool is dead), the TC saplings
	 *     (:228-231), Dye_Cocoa/Resin (:113/:115), the ChocoCraft/Harvestcraft/AE/EtFu rows.</li>
	 * </ul>
	 */
	public static List<InjectionRow> twilightInjections() {
		List<InjectionRow> rRows = new ArrayList<>();
		for (TwilightRow tLedger : twilightLedger()) {
			if (tLedger.table() == null) continue;
			String tName = "dungeon_inject_tf_" + tLedger.table().substring(tLedger.table().indexOf(':') + 1);
			rRows.add(new InjectionRow(tName, tLedger.table(), ROLL_MIN, ROLL_MAX,
					ladder(twilightEntries(tLedger.upstream()))));
		}
		return rRows;
	}

	/** The per-table entry stream: the mapped category rows + the live hand rows. */
	private static Stream<EntryRow> twilightEntries(String aUpstream) {
		switch (aUpstream) {
		case "hill1": case "hill3":
			return mineshaftEntries().stream();
		case "hill2":
			return Stream.concat(mineshaftEntries().stream(), Stream.of(mat(OP.stick, MT.Basalz, 1, 4, 4))); // :83
		case "hedgemaze":
			return Stream.concat(bonusEntries().stream(), Stream.of( // :113-123
					van("name_tag", 1, 4, 4), van("lead", 1, 2, 2),
					mat(OP.stick, MT.Breeze, 1, 4, 4),
					new EntryRow("gt6:food_cinnamon", 1, 12, 12)));
		case "tree_cache":
			return bonusEntries().stream();
		case "labyrinth_room": case "labyrinth_deadend":
			return jungleEntries().stream();
		case "labyrinth_vault":
			return smithEntries().stream(); // :185 the Bag_Loot_Gems row is a bag cut
		case "tower_room":
			return desertEntries().stream(); // :144-149 the nether supplies are a dead-pool cut
		case "tower_library":
			return libraryEntries().stream(); // :164/:166 book + compass cuts
		case "darktower_cache":
			return corridorEntries().stream(); // :194-197 dead-pool cut
		case "darktower_key": case "stronghold_room": case "aurora_cache": case "aurora_room":
			return dungeonChestEntries().stream(); // :210 ender pearl dead-pool cut
		case "darktower_boss":
			return dispenserEntries().stream();
		case "stronghold_cache":
			return corridorEntries().stream();
		case "troll_garden":
			return Stream.concat(dungeonChestEntries().stream(), Stream.of(mat(OP.stick, MT.Blitz, 1, 4, 4))); // :275
		case "troll_vault":
			return Stream.concat(smithEntries().stream(), Stream.of( // :287-289
					mat(OP.crushed, MT.AncientDebris, 1, 4, 4), mat(OP.crushed, MT.AncientDebris, 1, 8, 8),
					van("wither_skeleton_skull", 1, 2, 2)));
		default:
			throw new IllegalArgumentException("unknown twilight treasure row: " + aUpstream);
		}
	}

	/**
	 * The Dusty Guide Book row (task book-loot-first) — the registered
	 * {@code gt6:book_loot_guide} carrier (MultiItemBooks.java:67 meta 32765); the
	 * weight/stack columns ride verbatim per table (:413/:442/:512/:524).
	 */
	private static EntryRow guideRow(int aWeight, int aMin, int aMax) {
		return new EntryRow("gt6:book_loot_guide", aWeight, aMin, aMax);
	}

	/**
	 * The ZPM artifact row — weight 2 [1,1] (the rare-roll posture) carrying the FULL tag:
	 * the {@code gt.active.energy} store-as-full key (the DungeonData.zpm active lane;
	 * the upstream 2/3 dice stay collapsed to always-full, the p34 declared deviation —
	 * the uncharged ZPM has no recharge face in this port). Carrier: the GT6 dungeon
	 * loot face since task dungeon-library-zpm (was the vanilla simple_dungeon stopgap).
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
