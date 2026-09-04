/**
 * The explicit {@code use = PASS} semantic lock (task p9-wire-family-w1 spec ④) — the
 * executable half of the RCON negative assertion: a right-click on a wire with ANY held item
 * must fall through (the upstream GUI-less wire outcome, TileEntityBase06Covers
 * .onBlockActivated2 :106-130 = nothing happens); the connection tool is the cutter on the
 * IBlockToolable chain, NOT the vanilla use chain (MultiTileEntityWireElectric.java:245).
 * The override ignores its arguments, so a bootstrapped offline instance with null context
 * exercises it fully — constructing the Block offline needs the block registry temporarily
 * unfrozen (the vanilla bootstrap froze it; ForgeRegistry.unfreeze/freeze bracket). Also pins
 * the W1 carrier getters (material/size/insulated/diameter).
 */
package gregtech6.block.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GTWireBlockUseLockTest extends GTOfflineTestBase {

	@BeforeAll
	public static void initMaterialSystem() {
		GTMaterialItems.initMaterials(); // MT.Sn must be live before the carrier assertions
	}

	private static GTWireBlock block() {
		// Block.<init> creates its intrusive holder past the bootstrap freeze — unfreeze the
		// vanilla block registry wrapper for the construction (the wrapper class is
		// package-private Forge-internal, so the public unfreeze() goes through reflection;
		// test-JVM-local, the lock test needs a live instance because use() is instance code).
		try {
			java.lang.reflect.Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return new GTWireBlock(32, 1, 2, MT.Sn, 1, false, 2, BlockBehaviour.Properties.of());
	}

	@Test
	public void useAlwaysPasses() {
		GTWireBlock tWire = block();
		// arguments are ignored by the lock — nulls are the honest offline form
		//? if forge {
		assertEquals(InteractionResult.PASS, tWire.use(null, null, null, null, null, null),
		//?} else {
		/*assertEquals(InteractionResult.PASS, tWire.useWithoutItem(null, null, null, null, null),
		*///?}
				"the wire must never consume a right-click (spec ④ semantic lock)");
	}

	@Test
	public void carrierExposesRowIdentity() {
		GTWireBlock tWire = block();
		assertEquals(32, tWire.voltageL());
		assertEquals(1, tWire.amperageL());
		assertEquals(2, tWire.lossL());
		assertEquals(MT.Sn, tWire.material());
		assertEquals(1, tWire.size());
		assertFalse(tWire.insulated());
		assertEquals(2, tWire.diameter());
		// the legacy p7 form carries no row identity
		GTWireBlock tLegacy = new GTWireBlock(32, 1, 1, BlockBehaviour.Properties.of());
		assertNull(tLegacy.material());
		assertTrue(!tLegacy.insulated());
		// token discipline holds on the live material system
		assertEquals("tin", GTMaterialItems.snakeCase(MT.Sn.mNameInternal));
	}
}
