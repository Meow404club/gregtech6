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
		gWrench = probeItem("mining_probe_wrench", GTWrenchItem::new);
		bindWrenchFace(holder(Blocks.HOPPER), holder(Blocks.DISPENSER), holder(Blocks.DROPPER),
				holder(Blocks.PISTON), holder(Blocks.STICKY_PISTON), holder(Blocks.PISTON_HEAD),
				holder(Blocks.MOVING_PISTON), holder(Blocks.REDSTONE_LAMP), holder(Blocks.IRON_BARS));
	}

	/**
	 * The offline stand-in for the datapack tag load — the registry-level bindTags (the
	 * vanilla MappedRegistry.java:374 load face) over the nine vanilla band members.
	 */
	private static void bindWrenchFace(Holder<Block>... aMembers) {
		BuiltInRegistries.BLOCK.bindTags(java.util.Map.of(gWrenchMineable, List.of(aMembers)));
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
		// the static-storage metal ladder (2 materials x locker/drawer/2 safes, Loader
		// :134-140 aMachine; the wooden bookshelf/bottlecrate ladders stay out)
		Set<String> tMetalStorage = new java.util.HashSet<>();
		for (var tRow : GT6StaticStorages.ROWS) {
			if (tRow.material() != null) tMetalStorage.add("gt6:" + tRow.path());
		}
		assertEquals(8, tMetalStorage.size(), "locker/drawer/safe x2 over bronze/steel");
		assertTrue(tMembers.containsAll(tMetalStorage), "the metal storage ladder rides the face");
		assertTrue(!tMembers.contains("gt6:bookshelf_oak") && !tMembers.contains("gt6:bottlecrate_oak"),
				"the wooden storage ladders are the axe-wood card");
		// the whole-face ratchet: machines + battery boxes + 9 vanilla + the 620 extension
		// members, zero overlap (the tag dedups, so any overlap would silently shrink this)
		Set<String> tExpected = new java.util.HashSet<>(tMembers);
		assertEquals(VANILLA_MEMBERS.size() + GT6Batteries.BATTERY_BOX_BLOCKS.size() + 620
				+ GTMachines.BLOCKS.getEntries().size(), tExpected.size(),
				"the extension adds 620 members (54 multiblocks + 120 hoppers + 126 item pipes + 245 metal pipes "
				+ "+ 24 valves + 8 turbines + 1 reactor + 5 energizers + 2 dechargers + 1 absorber + 3 BE + 18 pipes + 5 transformers + 8 storage)");
	}

	/** The wrench tag's committed values (the runData product) as a path set. */
	private static Set<String> wrenchTagMembers() throws Exception {
		InputStream tStream = WrenchMiningTest.class.getClassLoader()
				.getResourceAsStream("data/gt6/tags/blocks/mineable/wrench.json");
		assertNotNull(tStream, "the gt6:mineable/wrench tag must be on the classpath (the runData product)");
		var tArray = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonArray("values");
		Set<String> rMembers = new java.util.HashSet<>();
		for (var tEntry : tArray) {
			assertTrue(tEntry.isJsonPrimitive(),
					"zero optional members — a required:false TagEntry serializes as an object");
			rMembers.add(tEntry.getAsString());
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

	/**
	 * The HammerWrenchTest.probeItem bracket verbatim — the Forge registry write window
	 * (a mod Item cannot otherwise be constructed past the intrusive-holder wall).
	 */
	private static <I extends Item> I probeItem(String aProbeId, java.util.function.Function<Item.Properties, I> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// three locks: the vanilla frozen flag, the delegate ForgeRegistry.isFrozen,
			// the NamespacedWrapper.locked register gate (the HammerWrenchTest walk)
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
			// the 21.1 runtime shape: a single frozen flag guards both walls
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
		I rItem = aCreator.apply(new Item.Properties().durability(512));
		net.minecraft.core.Registry.register(tRegistry, aProbeId, rItem);
		return rItem;
	}

	/** getDeclaredField along the superclass chain (the HammerWrenchTest walker). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// keep walking up
			}
		}
		throw new NoSuchFieldException(aName);
	}
}
