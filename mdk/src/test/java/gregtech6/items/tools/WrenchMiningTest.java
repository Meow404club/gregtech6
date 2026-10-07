/**
 * The wrench mining-face truth table (task wrench-mining-face, red-green pinned BEFORE
 * the face exists — every assertion here ran RED against the pure-Item wrench: the
 * vanilla {@code getDestroySpeed} default is 1.0F (Item.java:130-132) and
 * {@code isCorrectToolForDrops} false (Item.java:193-195), the 6× slowdown the user
 * reported on battery boxes, hardness 4.0 → 4·30/1.0 = 120 ticks = 6 s vs the upstream
 * 1 s). Upstream: MultiItemTool.getDigSpeed (MultiItemTool.java:472-484) =
 * {@code getMiningSpeed(isMinableBlock ? 1 : 0)} × speedMultiplier × mToolSpeed — the
 * port face is the tag check × {@link GT6ToolLadder#speed} at the form multiplier 1.0.
 *
 * <p>Rulings (the task card, all three): <b>A</b> — a non-mineable face digs at ZERO
 * (upstream {@code isMinableBlock ? 1 : 0}, the wrench cannot dig stone; NOT the
 * crowbar's 1.0F declared deviation); <b>B</b> — the machine blocks keep
 * mineable/pickaxe AND join the new {@code gt6:mineable/wrench} tag (no yielding);
 * <b>C</b> — {@code requiresCorrectToolForDrops} stays OFF (the punitive no-drop is the
 * defer pool, never requested).
 *
 * <p>Offline assertion surface: the probe item is the HammerWrenchTest reflection
 * bracket verbatim (the Forge registry write window — a mod Item cannot otherwise be
 * constructed past the intrusive-holder wall, Item.java:61). The tag drive is the
 * registry-level {@code bindTags} — the vanilla datapack-load face (MappedRegistry
 .java:374) — applied to the NINE VANILLA band members (GT_Tool_Wrench.java:72-81
 * unfolding: the piston material × 4, the redstoneLight lamp, the bars pane, hopper/
 * dispenser/dropper) so the runtime truth table needs no datapack. The GT6 machine
 * domain cannot come up in this frozen JVM, so its membership rides the JSON snapshot
 * face (the GT6TagsDatagenTest discipline): the committed tag file must carry the
 * whole {@link GTMachines} walk + the twelve battery boxes (the reported symptom
 * family, GT6Batteries.BATTERY_BOX_BLOCKS) + exactly those nine vanilla members.
 */
package gregtech6.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.components.OMComponentFaceTest;
import gregtech6.registry.GT6Batteries;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.registry.GT6LongDistPipes;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.registry.GT6QuantumEnergizers;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6Tanks;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GT6ZpmDechargers;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMachines;

public class WrenchMiningTest {

	/**
	 * The SAME location the {@link GTWrenchItem} constant carries (task implementation) —
	 * TagKey equality is by value, so binding THIS key drives the runtime face exactly:
	 * the passing speed assertion below is the behavioral pin of both the path and the
	 * membership check. Created in {@code @BeforeAll} — the registry-touching class
	 * must stay lazy (the ItemLatch lesson, GTOfflineTestBase javadoc).
	 */
	private static TagKey<Block> gWrenchMineable;

	/** The nine vanilla members (GT_Tool_Wrench.java:72-81 unfolding) — minecraft: paths. */
	private static final List<String> VANILLA_MEMBERS = List.of(
			"minecraft:piston", "minecraft:sticky_piston", "minecraft:piston_head", "minecraft:moving_piston",
			"minecraft:redstone_lamp", "minecraft:iron_bars",
			"minecraft:hopper", "minecraft:dispenser", "minecraft:dropper");

	private static GTWrenchItem gWrench;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GT6MaterialTestSupport.materials(); // the hermetic bracket (the MT.Steel fallback path)
		gWrenchMineable = TagKey.create(Registries.BLOCK, new ResourceLocation("gt6", "mineable/wrench"));
		gWrench = OMComponentFaceTest.probeItem("gt6", "mining_probe_wrench", p -> new GTWrenchItem(p.durability(512)));
		bindWrenchFace(holder(Blocks.HOPPER), holder(Blocks.DISPENSER), holder(Blocks.DROPPER),
				holder(Blocks.PISTON), holder(Blocks.STICKY_PISTON), holder(Blocks.PISTON_HEAD),
				holder(Blocks.MOVING_PISTON), holder(Blocks.REDSTONE_LAMP), holder(Blocks.IRON_BARS));
	}

	/**
	 * The offline stand-in for the datapack tag load — the registry-level bindTags (the
	 * vanilla MappedRegistry.java:374 load face) over the nine vanilla band members.
	 */
	@SafeVarargs
	private static void bindWrenchFace(Holder<Block>... aMembers) {
		// the explicit copy — List.of(aMembers) would pass the non-reifiable array to another
		// varargs callee, which [varargs] flags even under @SafeVarargs
		java.util.List<Holder<Block>> tMembers = new java.util.ArrayList<>(aMembers.length);
		for (Holder<Block> tMember : aMembers) tMembers.add(tMember);
		BuiltInRegistries.BLOCK.bindTags(java.util.Map.of(gWrenchMineable, List.copyOf(tMembers)));
	}

	private static Holder<Block> holder(Block aBlock) {
		return BuiltInRegistries.BLOCK.getHolderOrThrow(
				BuiltInRegistries.BLOCK.getResourceKey(aBlock).orElseThrow());
	}

	// ------------------------------------------------------- the runtime truth table

	/**
	 * THE RED PIN: the wrench-mineable face digs at the Steel mToolSpeed anchor 6.0
	 * (upstream MultiItemTool.java:483 with the wrench stats: isMinableBlock 1 × 1.0 ×
	 * Steel 6.0 — the battery box 4.0 hardness drops from 6 s to ~1 s) and authorizes
	 * drops. RED was 1.0F / false — the pure-Item defaults.
	 */
	@Test
	public void theWrenchFaceDigsAtTheMaterialSpeedAndAuthorizesDrops() {
		for (String tPath : VANILLA_MEMBERS) {
			BlockState tState = BuiltInRegistries.BLOCK.get(
					new ResourceLocation(tPath)).defaultBlockState();
			assertEquals(6.0F, gWrench.getDestroySpeed(ItemStack.EMPTY, tState),
					"the wrench-mineable surface digs at the Steel anchor (identity-less = the fallback): " + tPath);
			assertTrue(isCorrectTool(gWrench, tState), "the face authorizes the drops: " + tPath);
		}
	}

	/**
	 * Ruling A — a non-mineable face digs at ZERO (the upstream
	 * {@code isMinableBlock ? 1 : 0} semantics: the wrench cannot dig stone; the crowbar's
	 * 1.0F hand-speed deviation is deliberately NOT imitated) and drops stay unauthorized.
	 */
	@Test
	public void outsideTheFaceTheSpeedIsZero() {
		for (Block tBlock : List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT,
				Blocks.IRON_ORE, Blocks.OAK_PLANKS)) {
			BlockState tState = tBlock.defaultBlockState();
			assertEquals(0.0F, gWrench.getDestroySpeed(ItemStack.EMPTY, tState),
					"non-mineable = ZERO (upstream isMinableBlock?1:0, ruling A): " + tBlock);
			assertFalse(isCorrectTool(gWrench, tState), "no drops outside the face: " + tBlock);
		}
	}

	/** The anchor provenance — the ladder face at the form multiplier 1.0 over the Steel fallback. */
	@Test
	public void theLadderAnchorIsSteelSix() {
		assertEquals(6.0F, GT6ToolLadder.speed(1.0F, GT6ToolLadder.materialOf(ItemStack.EMPTY)),
				"MultiItemTool.java:483 Steel anchor (mToolSpeed 6.0 × multiplier 1.0)");
	}

	// ------------------------------------------------------- the tag snapshot face

	/**
	 * The committed tag file (runData product, the GT6TagsDatagenTest classpath
	 * discipline): the whole GTMachines register (ruling B — every landed machine row
	 * auto-joins, the whole-class walk), the twelve battery boxes (the reported symptom
	 * family, GT6Batteries.BATTERY_BOX_BLOCKS), exactly the nine vanilla members (the
	 * GT_Tool_Wrench.java:72-81 unfolding), zero optional entries.
	 */
	@Test
	public void theTagShipsTheMachinesTheBatteryBoxesAndTheVanillaSet() throws Exception {
		Set<String> tMembers = wrenchTagMembers();
		for (String tPath : VANILLA_MEMBERS) {
			assertTrue(tMembers.contains(tPath), "the vanilla band member must ride the tag: " + tPath);
		}
		for (String tPath : GT6Batteries.BATTERY_BOX_BLOCKS.keySet()) {
			assertTrue(tMembers.contains("gt6:" + tPath),
					"the battery box family (the reported symptom block) must ride the tag: " + tPath);
		}
		GTMachines.BLOCKS.getEntries().forEach(tEntry ->
				assertTrue(tMembers.contains(tEntry.getId().toString()),
						"the whole machine register rides the tag (ruling B): " + tEntry.getId()));
	}

	// ------------------------------------------------------- the band extension face

	/** The gt6-namespaced paths of one family register (the walk mirror — key space, no live registry needed offline). */
	private static Set<String> pathsOf(java.util.Map<String, ?> aMap) {
		Set<String> rPaths = new java.util.HashSet<>();
		for (String tPath : aMap.keySet()) rPaths.add("gt6:" + tPath);
		return rPaths;
	}

	/**
	 * The band-extension snapshot (task harvest-bands-wrench-machines — the whole upstream
	 * aMachine domain, Loader_MultiTileEntities aMachine column re-verified line-by-line,
	 * the provider javadoc table carries the per-family upstream rows). The family walk
	 * counts are pinned next to the membership so a register growth is a conscious number
	 * bump (the PINNED_PICKAXE_TOTAL discipline), and the file total ratchets the whole
	 * face. NEGATIVE pins carry the declared exclusions: the wood wall + the wood tank
	 * valve (aWooden, the axe-wood card), the wood fluid pipes (axe) + the rubber ones
	 * (the shears defer), the bookshelf/bottlecrate wooden storage ladders, the crucibles
	 * (aMetal pickaxe — the census card-split typo, no wrench crucible upstream).
	 */
	@Test
	public void theTagShipsTheBandExtensionFamilies() throws Exception {
		Set<String> tMembers = wrenchTagMembers();
		// GTMultiBlocks — the aMachine atomic parts (the wood wall excluded), the dense
		// walls, the Lightning Rod family trio, the five large boilers, the heat
		// transmitter, and the six wrench controllers
		Set<String> tRodFamily = pathsOf(gregtech6.registry.GTMultiBlocks.LIGHTNING_ROD_PART_BLOCKS_BY_PATH);
		for (String tPath : gregtech6.registry.GTMultiBlocks.NEW_PART_BLOCKS_BY_PATH.keySet()) {
			if (!tPath.equals("wood_wall")) tRodFamily.add("gt6:" + tPath); // the :1139 wood wall is aWooden
		}
		tRodFamily.addAll(pathsOf(gregtech6.registry.GTMultiBlocks.WALL_BLOCKS_BY_PATH));
		tRodFamily.addAll(pathsOf(gregtech6.registry.GTMultiBlocks.LARGE_BOILER_BLOCKS_BY_PATH));
		for (String tController : List.of("gt6:heat_transmitter", "gt6:multiblock_lightning_rod",
				"gt6:implosion_compressor", "gt6:large_massfab", "gt6:fusion_reactor",
				"gt6:von_da_graagg", "gt6:bedrock_drill")) {
			tRodFamily.add(tController);
		}
		assertEquals(54, tRodFamily.size(), "28 atomic parts + 3 rod family + 11 dense walls + 5 boilers + 7 singletons/controllers");
		assertTrue(tMembers.containsAll(tRodFamily), "the GTMultiBlocks aMachine domain rides the face");
		assertTrue(!tMembers.contains("gt6:wood_wall"), "the wood wall is aWooden — the axe-wood card");
		// the hoppers (60 materials x plain/queue, Loader :145-146) and the item pipes
		// (21 x 6, Loader :1823-1843)
		assertEquals(120, GT6Hoppers.BLOCKS_BY_PATH.size(), "60 plain + 60 queue hoppers");
		assertTrue(tMembers.containsAll(pathsOf(GT6Hoppers.BLOCKS_BY_PATH)), "the hopper family rides the face");
		assertEquals(126, GTItemPipes.BLOCKS_BY_PATH.size(), "21 materials x 6 variants");
		assertTrue(tMembers.containsAll(pathsOf(GTItemPipes.BLOCKS_BY_PATH)), "the item pipe family rides the face");
		// the fluid-pipe MACHINE subdomain (35 metals x 7, Loader :1851-1860) — the wood
		// 28 went axe, the rubber 7 stay pickaxe (the shears defer)
		Set<String> tMetalPipes = new java.util.HashSet<>();
		for (var tEntry : GTFluidPipes.BLOCKS_BY_PATH.entrySet()) {
			if (GTFluidPipes.rowByPath(tEntry.getKey()).material().blockFamily() == GTFluidPipes.PipeBlockFamily.MACHINE) {
				tMetalPipes.add("gt6:" + tEntry.getKey());
			}
		}
		assertEquals(245, tMetalPipes.size(), "35 metal materials x 7 variants");
		assertTrue(tMembers.containsAll(tMetalPipes), "the metal fluid pipes ride the face (the mislabel fix)");
		assertTrue(!tMembers.contains("gt6:wood_fluid_pipe_small"), "the wood pipes went axe");
		assertTrue(!tMembers.contains("gt6:rubber_fluid_pipe_tiny"), "the rubber pipes stay pickaxe (the shears defer)");
		// the tank main valves (24 metal, Loader :1196-1222; the wood valve :1195 stays out)
		Set<String> tMetalValves = new java.util.HashSet<>();
		for (var tRow : GT6Tanks.ROWS) {
			if (!tRow.flammable()) tMetalValves.add("gt6:" + tRow.path());
		}
		assertEquals(24, tMetalValves.size(), "small/dense/large/large-dense x 6 metals");
		assertTrue(tMembers.containsAll(tMetalValves), "the metal tank valves ride the face");
		assertTrue(!tMembers.contains("gt6:tank_wood"), "the wood tank valve is aWooden — the axe-wood card");
		// the machine-BE domain: turbines 8 (steam :1254-1257 + gas :1264-1267), the 2x2
		// reactor core (:738), the five quantum energizers (:962-966), the two ZPM
		// dechargers (:1000-1001), the magic absorber (:1005), the energy source + the two
		// test machines (the port-native BE-domain seat)
		assertEquals(8, GT6Turbines.BLOCKS_BY_PATH.size(), "4 steam + 4 gas turbine housings");
		assertTrue(tMembers.containsAll(pathsOf(GT6Turbines.BLOCKS_BY_PATH)), "the turbine housings ride the face");
		assertTrue(tMembers.contains("gt6:nuclear_reactor_core_2x2"), "the 2x2 reactor core rides the face");
		assertEquals(5, GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH.size(), "T1-T5");
		assertTrue(tMembers.containsAll(pathsOf(GT6QuantumEnergizers.QUANTUM_ENERGIZER_BLOCKS_BY_PATH)),
				"the quantum energizers ride the face");
		assertEquals(2, GT6ZpmDechargers.BLOCKS_BY_PATH.size(), "quantum + electric");
		assertTrue(tMembers.containsAll(pathsOf(GT6ZpmDechargers.BLOCKS_BY_PATH)), "the ZPM dechargers ride the face");
		assertTrue(tMembers.contains("gt6:magic_absorber"), "the magic field absorber rides the face");
		assertTrue(tMembers.contains("gt6:energy_source"), "the energy source rig rides the face");
		assertTrue(tMembers.contains("gt6:test_machine") && tMembers.contains("gt6:test_machine_idle"),
				"the machine-BE test pair rides the face");
		// the long-distance trio: 16 pipeline metas + the two endpoints (BlockLongDistPipe
		// .java:42 + Loader :906-907) and the five transformer endpoints (:909-913); the
		// long-distance WIRES stay OUT (the cutter expedient — pickaxe, GT6TagsDatagenTest)
		assertEquals(18, GT6LongDistPipes.WIRE_BLOCKS_BY_META.size() + 2, "16 pipeline metas + item/fluid endpoints");
		Set<String> tPipeMetas = new java.util.HashSet<>();
		for (Integer tMeta : GT6LongDistPipes.WIRE_BLOCKS_BY_META.keySet()) {
			tPipeMetas.add("gt6:" + GT6LongDistPipes.pathOf(tMeta));
		}
		assertTrue(tMembers.containsAll(tPipeMetas),
				"the long-distance pipeline metas ride the face");
		assertTrue(tMembers.contains("gt6:longdist_item_pipe") && tMembers.contains("gt6:longdist_fluid_pipe"),
				"the two pipeline endpoints ride the face");
		assertEquals(5, GT6LongDistanceTransformers.BLOCKS_BY_PATH.size(), "V4-V8 endpoints");
		assertTrue(tMembers.containsAll(pathsOf(GT6LongDistanceTransformers.BLOCKS_BY_PATH)),
				"the long-distance transformer endpoints ride the face");
		assertTrue(!tMembers.contains("gt6:long_dist_wire_0"), "the long-distance wires keep the cutter expedient (pickaxe)");
		// the static-storage aMachine ladders (2 materials x locker/drawer/2 safes, Loader
		// :134-140 + the 60-material METAL BOOKSHELF ladder :143; the wooden ladders stay
		// the axe-wood card and the METAL BOTTLECRATE :144 rides aUtilMetal = TOOL_pickaxe
		// (:107 carrier column) — the pickaxe band's tail, task
		// material-mc-b-storage-mass-shelf, the review-seat band-split seam)
		Set<String> tMetalStorage = new java.util.HashSet<>();
		Set<String> tMetalCrates = new java.util.HashSet<>();
		for (var tRow : GT6StaticStorages.ROWS) {
			if (tRow.material() == null) continue;
			(tRow.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE ? tMetalCrates : tMetalStorage)
					.add("gt6:" + tRow.path());
		}
		assertEquals(68, tMetalStorage.size(), "locker/drawer/safe x2 over bronze/steel + the 60 metal bookshelf rows (:143 aMachine)");
		assertTrue(tMembers.containsAll(tMetalStorage), "the aMachine storage ladder rides the face");
		assertEquals(60, tMetalCrates.size(), "the 60 metal bottlecrate rows (:144 aUtilMetal = pickaxe)");
		assertTrue(!tMembers.contains("gt6:bottlecrate_metal_bronze") && !tMembers.contains("gt6:bottlecrate_metal_lead"),
				"the metal bottlecrate ladder moved to the pickaxe band (upstream aUtilMetal, Loader :144)");
		assertTrue(!tMembers.contains("gt6:bookshelf_oak") && !tMembers.contains("gt6:bottlecrate_oak"),
				"the wooden storage ladders are the axe-wood card");
		// the whole-face ratchet: machines + battery boxes + 9 vanilla + the 989 extension
		// members, zero overlap (the tag dedups, so any overlap would silently shrink this).
		// +60 task block-family-32xxx-port reconciliation (the pre-existing-red shape, the
		// pickaxeBand seat-V precedent): the merged charging-locker ladder rode the canonical
		// tree without this pin's bump, measured 1076 -> 1136 on the mc-D rebase — the
		// storage family sum 68 -> 128 (the 60 charging lockers are aMachine, Loader :139).
		// +257 the tail extension (task harvest-bands-wrench-tail, the rebase union onto the
		// main 740: 740 -> 997 — 9 transformers + 82 kinetics metal (48 axles — the mc-D
		// powertrain card closed the Trinaquadalloy/Ad gap on main after this card's base —
		// + 26 steam engines + 8 diesel) + 10 lasers + 1 logistics core + 1 heat exchanger
		// + 2 distillation + 4 dynamo housings + 26 boilers + 96 burning boxes + 20 chargers
		// + 5 flux dynamos + 1 fe converter).
		Set<String> tExpected = new java.util.HashSet<>(tMembers);
		assertEquals(VANILLA_MEMBERS.size() + GT6Batteries.BATTERY_BOX_BLOCKS.size() + 997
				+ GTMachines.BLOCKS.getEntries().size(), tExpected.size(),
				"the extension adds 997 members (card 1: 54 multiblocks + 120 hoppers + 126 item pipes + 245 metal pipes "
				+ "+ 24 valves + 8 turbines + 1 reactor + 5 energizers + 2 dechargers + 1 absorber + 3 BE + 18 pipes + 5 transformers + 128 storage"
				+ "; tail: 9 transformers + 82 kinetics metal + 10 lasers + 1 logistics core + 1 heat exchanger + 2 distillation "
				+ "+ 4 dynamo housings + 26 boilers + 96 burning boxes + 20 chargers + 5 flux dynamos + 1 fe converter)");
	}

	/**
	 * The tail-extension snapshot (task harvest-bands-wrench-tail — the nine families the
	 * card-1 enumeration missed; every family re-verified line-by-line upstream BEFORE
	 * landing, the mc-A metal-chest lesson). The counts sit next to the membership so a
	 * register growth is a conscious bump. NEGATIVE pins carry the declared exclusions:
	 * the wooden kinetics subset (the wood axles :1662-1666, the IronWood steam engines
	 * :591/:606, the wooden gearbox :1669 / rotation transformer :1668 — aWooden, the
	 * axe-wood card), the brick burning box (:519 aStone — the pickaxe-stone card,
	 * census erratum) and the logistics wire (:1819 aMetalWires cutter — the pickaxe
	 * expedient).
	 */
	@Test
	public void theTagShipsTheTailExtensionFamilies() throws Exception {
		Set<String> tMembers = wrenchTagMembers();
		// electric transformers — 9 (:881-889 aMachine; the card's ":884-889" cite covers
		// :881-883 too, same column)
		assertEquals(9, gregtech6.registry.GT6ElectricTransformers.BLOCKS_BY_PATH.size(), "the :881-889 ladder");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6ElectricTransformers.BLOCKS_BY_PATH)),
				"the electric transformers ride the face");
		// kinetics metal — 40 axles + 26 steam engines + 8 diesel. The axle/diesel registry
		// maps fill on mod construct (the onModConstruct plain loop), so the offline mirror
		// derives the paths from the static SPEC tables — the same columns the provider
		// walk reads (the datagen JVM has the registry, the test JVM has the tables).
		Set<String> tMetalAxles = new java.util.HashSet<>();
		for (var tSpec : gregtech6.registry.GT6Kinetics.AXLE_SPECS) {
			if ("wood_treated".equals(tSpec.material())) continue; // the :1662-1666 wooden ladder
			for (int tSize = 0; tSize < gregtech6.registry.GT6Kinetics.AXLE_DIAMETERS.length; tSize++) {
				tMetalAxles.add("gt6:" + gregtech6.registry.GT6Kinetics.axleName(tSpec.material(), tSize));
			}
		}
		assertEquals(48, tMetalAxles.size(), "12 metal materials x 4 sizes (:1672-1766 aMachine; the Trinaquadalloy/Ad ladders landed on main after this card's base — the material-coverage gap closed, 40 -> 48 measured on the rebase)");
		assertTrue(tMembers.containsAll(tMetalAxles), "the metal axles ride the face");
		Set<String> tMetalEngines = new java.util.HashSet<>();
		for (var tRow : gregtech6.registry.GT6Kinetics.STEAM_ENGINES) {
			if (!tRow.wooden()) tMetalEngines.add("gt6:" + tRow.path());
		}
		assertEquals(26, tMetalEngines.size(), "the :584-612 aMachine steam engines (28 minus the IronWood pair)");
		assertTrue(tMembers.containsAll(tMetalEngines), "the metal steam engines ride the face");
		Set<String> tDiesel = new java.util.HashSet<>();
		for (var tSpec : gregtech6.registry.GT6Kinetics.DIESEL_SPECS) {
			tDiesel.add("gt6:" + gregtech6.registry.GT6Kinetics.dieselName(tSpec.material()));
		}
		assertEquals(8, tDiesel.size(), "the :721-729 diesel ladder");
		assertTrue(tMembers.containsAll(tDiesel), "the diesel engines ride the face");
		assertTrue(!tMembers.contains("gt6:axle_wood_treated_small") && !tMembers.contains("gt6:axle_wood_treated_huge"),
				"the wood axles are aWooden — the axe-wood card");
		assertTrue(!tMembers.contains("gt6:steam_engine_iron_wood") && !tMembers.contains("gt6:strong_steam_engine_iron_wood"),
				"the IronWood engines are aWooden — the axe-wood card");
		assertTrue(!tMembers.contains("gt6:gearbox") && !tMembers.contains("gt6:transformer_rotation"),
				"the wooden gearbox/rotation transformer are aWooden (:1668-1669) — the axe-wood card");
		// lasers — 5 CO2 (:930-934) + 5 absorbers (:976-980); the upstream laser FIBER wire
		// :1815 is aMetalWires — its port block rides the GTWires whole-class pickaxe walk,
		// so the wrench face takes only these ten machines
		assertEquals(5, gregtech6.registry.GT6Lasers.CO2_LASER_BLOCKS_BY_PATH.size(), "LV-IV CO2 lasers");
		assertEquals(5, gregtech6.registry.GT6Lasers.LASER_ABSORBER_BLOCKS_BY_PATH.size(), "LV-IV laser absorbers");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6Lasers.CO2_LASER_BLOCKS_BY_PATH))
				&& tMembers.containsAll(pathsOf(gregtech6.registry.GT6Lasers.LASER_ABSORBER_BLOCKS_BY_PATH)),
				"the laser machines ride the face");
		// logistics — the core (:1281) rides the face; the wire (:1819) is the cutter expedient
		assertTrue(tMembers.contains("gt6:logistics_core"), "the logistics core rides the face");
		assertTrue(!tMembers.contains("gt6:logistics_wire"),
				"the logistics wire is aMetalWires cutter (:1819) — the pickaxe expedient");
		// singletons and small ladders
		assertTrue(tMembers.contains("gt6:large_heat_exchanger"), "the large heat exchanger (:1245) rides the face");
		assertEquals(2, gregtech6.registry.GT6Distillation.TOWER_BLOCKS_BY_PATH.size(), "distillation + cryo (:1226-1227)");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6Distillation.TOWER_BLOCKS_BY_PATH)),
				"the distillation towers ride the face");
		assertEquals(4, gregtech6.registry.GT6DynamoHousings.BLOCKS_BY_PATH.size(), "the :1259-1262 housings");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6DynamoHousings.BLOCKS_BY_PATH)),
				"the large dynamo housings ride the face");
		// boilers — 26 steam boiler tanks (:553-565 + :567-579); the LARGE boiler mains
		// :1248-1252 already rode card 1 via GTMultiBlocks
		assertEquals(26, gregtech6.registry.GT6Boilers.BLOCKS_BY_PATH.size(), "13 standard + 13 strong");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6Boilers.BLOCKS_BY_PATH)),
				"the steam boiler tanks ride the face");
		// burning boxes — 96 metal (all four families aMachine), the brick row :519 aStone
		// OUT (the paths mirror the static allRows() — RegistryObject.get() resolves only
		// past the mod-construct event)
		Set<String> tMetalBoxes = new java.util.HashSet<>();
		for (var tRow : gregtech6.registry.GT6BurningBoxes.allRows()) {
			if (!tRow.stone()) tMetalBoxes.add("gt6:" + tRow.path());
		}
		assertEquals(96, tMetalBoxes.size(), "27 solid + 22 liquid + 22 gas + 26 fluidbed minus the brick row");
		assertTrue(tMembers.containsAll(tMetalBoxes), "the metal burning boxes ride the face");
		assertTrue(!tMembers.contains("gt6:brick_burning_box"),
				"the brick burning box is aStone (:519) — the pickaxe-stone card, census erratum");
		// crystal chargers — 20 (:969-972 loop)
		assertEquals(20, gregtech6.registry.GT6CrystalChargers.BLOCKS_BY_PATH.size(), "10 small + 10 large");
		assertTrue(tMembers.containsAll(pathsOf(gregtech6.registry.GT6CrystalChargers.BLOCKS_BY_PATH)),
				"the crystal chargers ride the face");
		// flux dynamos — 5, upstream-anchored :953-957 (the task card's "port-native" claim
		// is the erratum; the port javadoc cites the rows verbatim). The paths mirror off
		// the static ROWS (the registry handles resolve only past the mod-construct event)
		assertEquals(5, gregtech6.registry.GT6FluxDynamos.ROWS.size(), "the :953-957 ladder");
		for (var tRow : gregtech6.registry.GT6FluxDynamos.ROWS) {
			assertTrue(tMembers.contains("gt6:" + tRow.path()), "the flux dynamo ladder rides the face: " + tRow.path());
		}
		// fe converter — the TRUE port-native case (decisions.p28-eu-inbound-converter);
		// ruling per the card-1 port-native-machine precedent: it joins the machine face
		assertTrue(tMembers.contains("gt6:fe_converter"), "the fe converter rides the face (the port-native ruling)");
	}

	/** The wrench tag's committed values (the runData product) as a path set — both member forms (the strict string and the tag-residual-convergence optional object). */
	private static Set<String> wrenchTagMembers() throws Exception {
		InputStream tStream = WrenchMiningTest.class.getClassLoader()
				.getResourceAsStream("data/gt6/tags/blocks/mineable/wrench.json");
		assertNotNull(tStream, "the gt6:mineable/wrench tag must be on the classpath (the runData product)");
		var tArray = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonArray("values");
		Set<String> rMembers = new java.util.HashSet<>();
		for (var tEntry : tArray) {
			rMembers.add(tEntry.isJsonPrimitive() ? tEntry.getAsString()
			: tEntry.getAsJsonObject().get("id").getAsString());
		}
		return rMembers;
	}

	// ------------------------------------------------------- the leg fork + the probe

	/** The drop-authorization call — 1.20.1 reads the state, 21.1 joins the stack parameter. */
	private static boolean isCorrectTool(GTWrenchItem aWrench, BlockState aState) {
		//? if forge {
		return aWrench.isCorrectToolForDrops(aState);
		//?} else {
		/*return aWrench.isCorrectToolForDrops(ItemStack.EMPTY, aState);
		*///?}
	}

	// the probe-item bracket folded onto OMComponentFaceTest.probeItem (the gt6
	// namespace preserved, durability(512) at the call site — task probeitem-latch-hygiene).

}
