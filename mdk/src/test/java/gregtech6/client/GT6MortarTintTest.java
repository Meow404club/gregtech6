/**
 * The mortar family tint-dispatch pins (task mortar-family): tint index 0 answers the
 * constant Ceramic body column on every row (Loader_MultiTileEntities.java:2179-2183
 * {@code NBT_MATERIAL, MT.Ceramic}), index 1 answers the row's {@code NBT_DESIGN} pestle
 * of MORTAR_MATERIALS (MultiTileEntityMortar.java:59/:149) — FIVE pairwise-distinct
 * colours, the variants' visual distinctness — and every other combination answers the
 * {@code -1} no-tint sentinel (the P22 overlay decals pass untouched). The colour math is
 * the shared GTBasicMachineBlock.materialColor seam, asserted only against the seam's
 * own product (no hand-rolled RGB in the test).
 */
package gregtech6.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tools.GT6MortarBlock;

public class GT6MortarTintTest {

	private static final GT6MortarBlock[] sRows = new GT6MortarBlock[5];

	@BeforeAll
	static void buildOfflineFixtures() {
		// the GTOfflineRenderTestBase recipe — this suite sits outside both base packages
		// (the GTOreBakedModelTintTest form): without the boot, the FIRST BuiltInRegistries
		// touch poisons the class for every later suite in this JVM (the run-order lottery)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.registry.GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GT6MortarNeiModelTest recipe) — the Block
		// ctor registers its intrusive holder
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		// the five rows' pestle materials in DESIGN order (MultiTileEntityMortar.java:59)
		gregapi.oredict.OreDictMaterial[] tPestles = {
				gregapi.data.MT.Steel, gregapi.data.MT.Netherite, gregapi.data.MT.Sapphire,
				gregapi.data.MT.Diamond, gregapi.data.MT.Amethyst};
		for (int i = 0; i < 5; i++) {
			final gregapi.oredict.OreDictMaterial tPestle = tPestles[i];
			sRows[i] = new GT6MortarBlock(() -> tPestle, () -> null,
					net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		}
	}

	/** Index 0 = the Ceramic body on every row; index 1 = the five pairwise-distinct pestle colours; the rest = -1. */
	@Test
	void theDispatchAnswersBodyPestleAndSentinel() {
		int tBody = gregtech6.block.GTBasicMachineBlock.materialColor(gregtech6.registry.GT6Mortars.BODY_MATERIAL.get());
		Set<Integer> tPestleColours = new HashSet<>();
		for (GT6MortarBlock tRow : sRows) {
			assertEquals(tBody, GT6MortarTint.blockTintARGB(tRow, 0),
					"the body seat is the constant Ceramic column on every row");
			int tPestle = GT6MortarTint.blockTintARGB(tRow, 1);
			assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(tRow.pestle()), tPestle,
					"the pestle seat answers the row's NBT_DESIGN material");
			tPestleColours.add(tPestle);
			assertEquals(-1, GT6MortarTint.blockTintARGB(tRow, 2), "no third seat");
			assertEquals(-1, GT6MortarTint.blockTintARGB(tRow, -1), "the decal sentinel passes untouched");
		}
		assertEquals(5, tPestleColours.size(), "the five pestle colours are PAIRWISE DISTINCT — the variants' visual distinctness");
	}

	/** A non-mortar block is outside the dispatch (the family gate). */
	@Test
	void theDispatchGatesOnTheMortarFamily() {
		assertEquals(-1, GT6MortarTint.blockTintARGB(net.minecraft.world.level.block.Blocks.STONE, 0),
				"foreign blocks answer the sentinel on every seat");
		assertEquals(-1, GT6MortarTint.blockTintARGB(net.minecraft.world.level.block.Blocks.STONE, 1),
				"foreign blocks answer the sentinel on every seat");
	}
}
