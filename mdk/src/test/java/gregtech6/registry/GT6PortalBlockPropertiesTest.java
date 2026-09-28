/**
 * Offline pin for task r4-23a-portal-frame (issue #23) — the miniature portal blocks are
 * noOcclusion: the hollow 12-beam frame model is only see-through while the neighbours
 * keep their faces (the default canOcclude makes isSolidRender true —
 * BlockBehaviour.java:582-588 — and every face abutting the portal culles into an x-ray
 * hole). Upstream surface semantics: isSurfaceSolid=F / isSurfaceOpaque=mActive
 * (MultiTileEntityMiniPortal.java:264-265); the vanilla end_portal_frame row is the
 * property precedent. The assertion rides the {@link GT6Portals#portalProperties}
 * factory — the single source of truth BOTH registration rows share, so dropping
 * .noOcclusion() there goes red here.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.portals.GTMiniPortalBlock;

class GT6PortalBlockPropertiesTest {

	@BeforeAll
	static void boot() {
		// the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap (the run-order lottery)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@Test
	void portalBlocksNeverOcclude() {
		// the GTWireContactDamageTest judging precedent: a Block CONSTRUCTION folds the
		// state into the built-in block registry, so the frozen FML boot registry must be
		// reopened before the fixture blocks come into existence
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		assertFalse(new GTMiniPortalBlock(() -> null, GT6Portals.portalProperties(3.0F))
				.defaultBlockState().canOcclude(), "the Nether portal is noOcclusion (the see-through cage premise)");
		assertFalse(new GTMiniPortalBlock(() -> null, GT6Portals.portalProperties(1.0F))
				.defaultBlockState().canOcclude(), "the End portal is noOcclusion (the see-through cage premise)");
	}
}
