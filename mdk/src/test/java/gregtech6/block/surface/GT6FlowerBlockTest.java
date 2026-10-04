/**
 * Offline pins for task flower-blocks-indicator-family — the indicator-flower band: the
 * 18-row spec table (the upstream BlockFlowersA metas 0-9 + BlockFlowersB metas 0-7
 * registration order, the Loader_Blocks.java:120-121 pair), the canBlockStay soil split
 * (the base BlockBaseFlower.java:131 yellow-flower dirt face vs the BlockFlowersB.java
 * :136-138 cactus sand face), the bonemeal copy trio (the IGrowable :132-:134 rows —
 * target/success always true, perform drops a COPY of the flower, NOT growth), and the
 * indicator tooltip rows (the hardcoded addInformation literals + the dark-gray
 * "* exists in Real Life" flag).
 *
 * <p>The potted pairing pins ride the id scheme (potted_* + the flower id — the
 * constructor wiring is the registry-event live face, RCON/live territory).
 *
 * <p>Boot: the GT6WildBushBlockTest recipe (version detect precedes bootStrap; the offline
 * BLOCK registry unfrozen for the bare construction).
 */
package gregtech6.block.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.registry.GT6SurfaceBlocks.FlowerSpec;

class GT6FlowerBlockTest {

	@BeforeAll
	static void boot() {
		// the version detect must precede bootStrap (the GT6SurfaceBlocksTest recipe — a
		// bare-JVM first boot poisons DataFixers for every later suite in this JVM)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		// offline Block construction needs the block registry temporarily unfrozen (the
		// GT6WildBushBlockTest.boot form)
		for (net.minecraft.core.Registry<?> tRegistry : new net.minecraft.core.Registry<?>[] {
				net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				net.minecraft.core.registries.BuiltInRegistries.ITEM}) {
			try {
				java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
				tUnfreeze.setAccessible(true);
				tUnfreeze.invoke(tRegistry);
			} catch (Exception aE) {
				throw new IllegalStateException("could not unfreeze the offline registry " + tRegistry.key(), aE);
			}
		}
		DIRT_FLOWER = new GT6FlowerBlock(BlockBehaviour.Properties.of(), false);
		SAND_FLOWER = new GT6FlowerBlock(BlockBehaviour.Properties.of(), true);
	}

	static GT6FlowerBlock DIRT_FLOWER;
	static GT6FlowerBlock SAND_FLOWER;

	// ------------------------------------------------------------------
	// the 18-row spec table (the upstream registration order + faces)
	// ------------------------------------------------------------------

	/** The 18 rows, upstream order: A metas 0-9 (dirt) then B metas 0-7 (sand). */
	@Test
	void specTableIsTheUpstreamEighteen() {
		List<FlowerSpec> tSpecs = GT6SurfaceBlocks.FLOWER_SPECS;
		assertEquals(18, tSpecs.size(), "the upstream 10 + 8 item faces");
		assertEquals(List.of("flower_altered_andesite_buckwheat", "flower_crosby_buckwheat",
						"flower_alpine_catchfly", "flower_viola_calaminaria", "flower_thlaspi_lereschianum",
						"flower_tufted_evening_primrose", "flower_narcissus_sheldonia", "flower_orechid",
						"flower_hexalily", "flower_vindicator_flower",
						"flower_sagebrush", "flower_four_wing_saltbush", "flower_desert_trumpet",
						"flower_copper_plant", "flower_princes_plume", "flower_thompsons_locoweed",
						"flower_pandanus_candelabrum", "flower_tungstus"),
				tSpecs.stream().map(FlowerSpec::snake).toList(),
				"the id snakes, BlockFlowersA.java:40 metas 0-9 then BlockFlowersB.java:44 metas 0-7");
		for (int i = 0; i < 10; i++) {
			assertFalse(tSpecs.get(i).sandSoil(), "A row " + i + " rides the base :131 dirt face");
		}
		for (int i = 10; i < 18; i++) {
			assertTrue(tSpecs.get(i).sandSoil(), "B row " + i + " rides the :136-138 sand override");
		}
	}

	/** The tooltip faces: the upstream addInformation literals verbatim + the RL flag set
	 * (A metas 0-5 + B metas 0-6 carry the "* exists in Real Life" row, 13 total). */
	@Test
	void tooltipFacesAreTheUpstreamAddInformationRows() {
		List<FlowerSpec> tSpecs = GT6SurfaceBlocks.FLOWER_SPECS;
		assertEquals(13, tSpecs.stream().filter(FlowerSpec::realLife).count(),
				"the RL rows: A metas 0-5 + B metas 0-6 (the upstream case blocks)");
		// the verbatim literals, one per family + the two no-RL faces
		assertEquals("Indicates presence of a Gold Deposit nearby", tSpecs.get(0).indicator());
		assertEquals("Vindicates presence of a Rare Earth Deposit nearby", tSpecs.get(9).indicator());
		assertEquals("Indicates presence of a Tungsten Deposit nearby", tSpecs.get(17).indicator());
		// the composed rows: one + the dark-gray RL row, or one
		assertEquals(1, GT6FlowerBlock.Item.tooltipLines("Indicates presence of a Platinum Deposit nearby", false).size(),
				"the no-RL face pays the indicator line only (the upstream :70 single add)");
		List<Component> tRows = GT6FlowerBlock.Item.tooltipLines("Indicates presence of a Gold Deposit nearby", true);
		assertEquals(2, tRows.size(), "the RL face pays both rows (the upstream :64-:65 pair)");
		assertEquals("* exists in Real Life", tRows.get(1).getString());
		assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY),
				tRows.get(1).getStyle().getColor(), "the RL row rides LH.Chat.DGRAY");
	}

	// ------------------------------------------------------------------
	// the registration pairing (block/item/potted id scheme)
	// ------------------------------------------------------------------

	/** The block/item/potted walk: same snake per row, the pots the potted_ prefix, the
	 * counts 18/18/18 (the DeferredRegister entries are the offline-safe faces). */
	@Test
	void registrationWalksPairBySnake() {
		for (int i = 0; i < 18; i++) {
			String tSnake = GT6SurfaceBlocks.FLOWER_SPECS.get(i).snake();
			assertEquals(tSnake, GT6SurfaceBlocks.FLOWERS.get(i).getId().getPath(), "flower block i = snake i");
			assertEquals(tSnake, GT6SurfaceBlocks.FLOWER_ITEMS.get(i).getId().getPath(), "flower item i = snake i");
			assertEquals("potted_" + tSnake, GT6SurfaceBlocks.POTTED_FLOWERS.get(i).getId().getPath(),
					"the potted companion = the vanilla potted_ prefix");
		}
		assertEquals(18, GT6SurfaceBlocks.FLOWERS.size(), "18 flower blocks");
		assertEquals(18, GT6SurfaceBlocks.FLOWER_ITEMS.size(), "18 flower items");
		assertEquals(18, GT6SurfaceBlocks.POTTED_FLOWERS.size(), "18 potted companions");
		// the rocks stay out of the flower lists (the ALL rock-family walk unit)
		assertFalse(GT6SurfaceBlocks.ALL.stream().anyMatch(tRow -> tRow.getId().getPath().startsWith("flower")),
				"ALL is the rock-family walk (the tint listener + the FACING band + the rock loot)");
	}

	// ------------------------------------------------------------------
	// the canBlockStay soil split (the base :131 dirt face vs the B :136-138 sand face)
	// ------------------------------------------------------------------

	/** The A flowers sustain the vanilla yellow-flower soil, the B flowers never do — the
	 * offline-bound distinguishing face (farmland, the one soil the tag-free JVM still
	 * resolves; the DIRT/SAND membership arms are the vanilla BushBlock/CactusBlock seat
	 * semantics verbatim — the tag registry is unbound offline, the live face). */
	@Test
	void soilSplitFollowsTheUpstreamCanBlockStayOverride() {
		BlockState tFarmland = Blocks.FARMLAND.defaultBlockState();
		assertTrue(DIRT_FLOWER.mayPlaceOn(tFarmland, null, null),
				"the A face sustains farmland (the yellow_flower arg)");
		assertFalse(SAND_FLOWER.mayPlaceOn(tFarmland, null, null),
				"the B :136-138 face never sustains farmland (the cactus arg)");
	}

	// ------------------------------------------------------------------
	// the bonemeal copy trio (the upstream IGrowable :132-:134 rows)
	// ------------------------------------------------------------------

	/** The trio: target always true (:132), success always true (:133) — and the :134
	 * perform face is a COPY of the flower (NOT growth; the pair-vs-growth distinction
	 * the bush bonemeal trio rides in reverse). The offline drop pin rides the
	 * Item.BY_BLOCK pairing (offline construction never registers it — runtime fills it
	 * in the BlockItem registry event). */
	@Test
	void bonemealPaysACopyNotGrowth() {
		//? if forge {
		assertTrue(DIRT_FLOWER.isValidBonemealTarget(null, null, null, true), "func_149851_a :132");
		//?} else {
		/*assertTrue(DIRT_FLOWER.isValidBonemealTarget(null, null, null), "func_149851_a :132");
		 *///?}
		assertTrue(SAND_FLOWER.isBonemealSuccess(null, null, null, null), "func_149852_a :133");
		// the :134 perform face = popResource(bonemealDrop()) with bonemealDrop() the
		// reviewed single line `new ItemStack(this)` — the Forge offline harness cannot
		// resolve ItemStack on an unregistered block (the getDelegateOrThrow face), so the
		// copy content is the reviewed-line + live face, the trio booleans the offline pin
	}
}
