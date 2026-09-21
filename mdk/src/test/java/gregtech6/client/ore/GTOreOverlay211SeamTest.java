/**
 * The ore OVERLAY seam's 21.1-leg dispatch pins (task p33-ore-overlay-impl — the POC
 * harness checks 4/6 of research.p32-r-ore-overlay-render, now asserted against the
 * production wiring instead of a skeleton). The 1.20.1 leg pins the SAME key surface
 * through the toString parse the forge dispatch uses, so both legs assert "a bake key
 * maps to its params-table path" — the contract both dispatch forks must satisfy.
 * Offline: ModelResourceLocation and the params table are registry-free.
 */
package gregtech6.client.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreKey;
import gregtech6.registry.GTMaterialItems;

class GTOreOverlay211SeamTest {

	@BeforeAll
	static void initMaterialSystem() {
		GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	/** The representative bake key path (the same shape both legs' dispatch walks). */
	private static String samplePath() {
		return GT6OreBlocks.path(new OreKey(GT6OreBlocks.TAB_FAMILY, FormKind.NORMAL,
				GT6OreBlocks.materialAxis().get(0)));
	}

	/**
	 * POC check 4 — the 21.1 typed-key dispatch parse. 1.21.1 keys the bake map by the
	 * ModelResourceLocation record (id, variant); the production neo fork reads
	 * {@code key.id().getNamespace()/getPath()} (the record has no getNamespace of its
	 * own — compile-probe 2026-09-21). This pin constructs the record and asserts the
	 * id accessors yield exactly the params-table key.
	 */
	@Test
	void leg211TypedKeyYieldsTheParamsPath() {
		String tPath = samplePath();
		//? if forge {
		// 1.20.1: MRL IS a ResourceLocation ("gt6:path#variant" in one string); the
		// forge dispatch's toString-split contract must yield the table path.
		net.minecraft.client.resources.model.ModelResourceLocation tKey =
				new net.minecraft.client.resources.model.ModelResourceLocation(
						new net.minecraft.resources.ResourceLocation("gt6", tPath), "inventory");
		String tKeyString = tKey.toString();
		assertEquals("gt6:" + tPath + "#inventory", tKeyString);
		assertEquals(tPath, tKeyString.substring("gt6".length() + 1).split("#", 2)[0]);
		//? } else {
		/*
		net.minecraft.client.resources.model.ModelResourceLocation tKey =
				new net.minecraft.client.resources.model.ModelResourceLocation(
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath), "inventory");
		assertEquals("gt6", tKey.id().getNamespace());
		assertEquals(tPath, tKey.id().getPath());
		assertEquals("inventory", tKey.getVariant());
		*/
		//? }
	}

	/** POC check 6 — the params table still covers the universe behind both dispatch forks. */
	@Test
	void paramsTableServesTheDispatch() {
		GTOreClientListener.clearForTest();
		GTOreClientListener.buildParams();
		assertNotNull(GTOreClientListener.paramsFor(samplePath()), "the sample path is dispatchable");
	}
}
