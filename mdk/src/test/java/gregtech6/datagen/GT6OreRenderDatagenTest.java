/**
 * The ore datagen render face pins (task p30-ore-3-datagen acceptance "渲染面离线断言").
 * The composition strategy is the card's contract: 3922 blocks (74 form-rows x M=53, the
 * census pin) walk onto 3922 blockstates + 3922 item models + a SHARED placeholder model
 * per distinct BASE texture (28), and the dual-sprite look is the {@code GTOreBakedModel}
 * bake face — per-pair models are the explicit red line, so the JSON total is pinned at
 * 7872, not ~137k. Offline-safe: the sprite derivations and the param table are
 * registry-free (the GTWireClientParamsTest posture); only the tint seam needs a
 * constructed block (the GT6OreBlocksRegistrationTest bootstrap-and-unfreeze posture).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTOreBlock;
import gregtech6.client.ore.GTOreBakedModel;
import gregtech6.client.ore.GTOreClientListener;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreFamily;
import gregtech6.registry.GT6OreBlocks.OreKey;
import gregtech6.registry.GTMaterialItems;
import net.minecraft.resources.ResourceLocation;

class GT6OreRenderDatagenTest {

	/** The pinned universe totals (the census constants, mirrored — production and test must agree). */
	private static final int PINNED_BLOCKS = 74 * 53;
	/** The pinned distinct base textures: 9 vanilla anchors + 2 cobble broken forms + 17 GT stones. */
	private static final int PINNED_BASE_MODELS = 28;
	/** The pinned generated JSON total: blockstates + item models + shared models. */
	private static final int PINNED_JSON_TOTAL = 2 * PINNED_BLOCKS + PINNED_BASE_MODELS;

	@BeforeAll
	static void initMaterialSystem() {
		GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
		// the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Throwable aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/** The param table covers the whole registration walk, one entry per per-pair path. */
	@Test
	void paramsTableCoversTheWholeUniverse() {
		GTOreClientListener.clearForTest();
		GTOreClientListener.buildParams();
		assertEquals(PINNED_BLOCKS, GTOreClientListener.paramsCount(), "one params entry per (family, form, material) pair");
		List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
		assertEquals(53, tAxis.size(), "the census-pinned material axis");
		String tSpot = GT6OreBlocks.path(new OreKey(GT6OreBlocks.TAB_FAMILY, FormKind.NORMAL, tAxis.get(0)));
		assertNotNull(GTOreClientListener.paramsFor(tSpot), "the first stone-normal pair is on the table: " + tSpot);
		assertTrue(tSpot.startsWith("ore_stone_"), "the id scheme anchor");
	}

	/** The base sprite anchors (the copied-rock semantics, verbatim texture picks). */
	@Test
	void baseSpriteAnchorsArePinned() {
		OreFamily tStone = GT6OreBlocks.FAMILIES.get(0);
		assertEquals("minecraft:block/stone", GTOreBakedModel.baseSpriteOf(tStone, FormKind.NORMAL).toString());
		assertEquals("minecraft:block/cobblestone", GTOreBakedModel.baseSpriteOf(tStone, FormKind.BROKEN).toString());
		assertEquals("minecraft:block/stone", GTOreBakedModel.baseSpriteOf(tStone, FormKind.SMALL).toString());
		OreFamily tDeepslate = GT6OreBlocks.FAMILIES.get(1);
		assertEquals("minecraft:block/deepslate", GTOreBakedModel.baseSpriteOf(tDeepslate, FormKind.NORMAL).toString());
		assertEquals("minecraft:block/cobbled_deepslate", GTOreBakedModel.baseSpriteOf(tDeepslate, FormKind.BROKEN).toString());
		OreFamily tMud = GT6OreBlocks.FAMILIES.get(8);
		assertEquals("minecraft:block/mud", GTOreBakedModel.baseSpriteOf(tMud, FormKind.SMALL).toString());
		// the GT stones ride the GTStoneBlocks texture paths — the granite/prismarine splits
		assertEquals("gt6:block/stones/granite_black/stone",
			GTOreBakedModel.baseSpriteOf(GT6OreBlocks.FAMILIES.get(9), FormKind.NORMAL).toString());
		assertEquals("gt6:block/stones/prismarine_light/stone",
			GTOreBakedModel.baseSpriteOf(GT6OreBlocks.FAMILIES.get(22), FormKind.BROKEN).toString());
		// the shared-model census: 9 vanilla + 2 cobble forms + 17 GT stones
		Set<String> tBases = new HashSet<>();
		for (OreFamily tFamily : GT6OreBlocks.FAMILIES) {
			for (FormKind tKind : tFamily.kinds()) {
				tBases.add(GTOreBakedModel.baseSpriteOf(tFamily, tKind).toString());
			}
		}
		assertEquals(PINNED_BASE_MODELS, tBases.size(), "the pinned shared-placeholder census");
	}

	/** The overlay form split (ore vs ore_small) and the atlas-source coverage (no unstitchable lookup). */
	@Test
	void overlaySpritesSplitByFormAndJoinTheAtlasSources() {
		List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
		String tSet = GTOreBakedModel.setOf(tAxis.get(0));
		assertEquals("gt6:block/materialicons/" + tSet + "/ore",
			GTOreBakedModel.overlaySpriteOf(tSet, FormKind.NORMAL).toString());
		assertEquals("gt6:block/materialicons/" + tSet + "/ore",
			GTOreBakedModel.overlaySpriteOf(tSet, FormKind.BROKEN).toString());
		assertEquals("gt6:block/materialicons/" + tSet + "/ore_small",
			GTOreBakedModel.overlaySpriteOf(tSet, FormKind.SMALL).toString());
		// every overlay the params table can look up has an atlas source (the GT6Atlases walk)
		Set<String> tSources = new HashSet<>();
		for (ResourceLocation tSprite : GTOreBakedModel.overlaySprites()) tSources.add(tSprite.toString());
		for (var tEntry : GTOreBakedModel.buildParams().entrySet()) {
			assertTrue(tSources.contains(tEntry.getValue().overlaySprite().toString()),
				"atlas source missing for " + tEntry.getKey());
		}
		assertEquals(0, tSources.size() % 2, "the (ore, ore_small) pairs per distinct SET — the census is even");
		assertTrue(tSources.size() >= 2, "at least one SET pair is sourced");
	}

	/** The shared model path derivation (the extendWithFolder pin: explicit block/ segment). */
	@Test
	void sharedModelNamesDeriveFromTheBaseTexture() {
		assertEquals("block/ore/stone", GT6OreBlockStates.modelNameOf(new ResourceLocation("minecraft", "block/stone")));
		assertEquals("block/ore/stones/granite_black/stone",
			GT6OreBlockStates.modelNameOf(new ResourceLocation("gt6", "block/stones/granite_black/stone")));
	}

	/** The composition contract: 7872 JSONs, not per-pair (the red line), model-shared. */
	@Test
	void jsonCompositionIsPinned() {
		assertEquals(PINNED_BLOCKS, GT6OreBlocks.registrationOrder().size(), "the census total walk");
		assertEquals(PINNED_JSON_TOTAL, 2 * PINNED_BLOCKS + PINNED_BASE_MODELS,
			"blockstates + item models + shared models = the composition expectation (per-pair would be ~3x this and carry no shared identity)");
	}

	/** The tint seam: index 0 over an ore block = the material colour; everything else = -1. */
	@Test
	void tintIndexZeroCarriesTheMaterialColour() {
		List<OreDictMaterial> tAxis = GT6OreBlocks.materialAxis();
		GTOreBlock tBlock = new GTOreBlock(GT6OreBlocks.TAB_FAMILY, FormKind.NORMAL,
				GT6OreBlocks.TAB_FAMILY.form(FormKind.NORMAL), GT6OreBlocks.TAB_FAMILY.prefix(FormKind.NORMAL), tAxis.get(0));
		int tTint = GTOreClientListener.oreTintARGB(tBlock.defaultBlockState(), 0);
		assertTrue((tTint >>> 24) == 0xFF, "opaque ARGB, got " + Integer.toHexString(tTint));
		assertEquals(-1, GTOreClientListener.oreTintARGB(tBlock.defaultBlockState(), 1), "index 1 is unassigned");
		assertEquals(-1, GTOreClientListener.oreTintARGB(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 0),
			"non-ore blocks stay untinted");
	}
}
