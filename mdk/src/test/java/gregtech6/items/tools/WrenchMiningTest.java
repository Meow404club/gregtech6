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
import gregtech6.registry.GT6MaterialTestSupport;
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
		InputStream tStream = WrenchMiningTest.class.getClassLoader()
				.getResourceAsStream("data/gt6/tags/blocks/mineable/wrench.json");
		assertNotNull(tStream, "the gt6:mineable/wrench tag must be on the classpath (the runData product)");
		var tArray = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonArray("values");
		Set<String> tMembers = new java.util.HashSet<>();
		for (var tEntry : tArray) {
			assertTrue(tEntry.isJsonPrimitive(),
					"zero optional members — a required:false TagEntry serializes as an object");
			tMembers.add(tEntry.getAsString());
		}
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
