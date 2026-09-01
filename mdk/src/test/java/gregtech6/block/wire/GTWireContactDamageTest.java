/**
 * The offline truth tables of the wire contact damage (task p10-wire-contact-damage).
 * What is pinned HERE is the pure decision math the live hook applies: the
 * {@code tierMax(wattage) * 4} formula accessor ({@link GTWireBlockEntity#pendingContactDamage},
 * the upstream UT.Entities.applyElectricityDamage :3024-3031 expression), the
 * unpowered-wires-don't-bite gate (tierMax(0) == 0 over the transient mWattageLast), the
 * redstone family gate, the NBT_CONTACTDAMAGE carrier matrix (bare wires of the 28 shock
 * rows only) and the 2px collision inset that makes the 1.20.1 entityInside hook fire at
 * all (upstream :219). The LIVE chain (hurt amounts, creative immunity against a real
 * player, the invuln-frame throttle) is the RCON channel.
 */
package gregtech6.block.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;
import gregtech6.registry.GTWireSpecs.Row.Family;
import gregtech6.tileentity.connectors.GTWireBlockEntity;

public class GTWireContactDamageTest {

	static BlockEntityType<TestWire> sType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	/** Offline BE fixture: a non-null BET keeps the constructor off the registry-backed fallback. */
	public static class TestWire extends GTWireBlockEntity {
		public TestWire(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
	}

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GTMaterialItems.initMaterials(); // MT.Sn & co must be live before the carrier assertions
		@SuppressWarnings("unchecked")
		BlockEntityType<TestWire>[] tHolder = (BlockEntityType<TestWire>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestWire(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	/** Offline Block construction needs the block registry temporarily unfrozen (the UseLockTest form). */
	private static GTWireBlock block(OreDictMaterial aMaterial, boolean aInsulated, Family aFamily, long aVoltage, long aLoss) {
		try {
			Method tUnfreeze = BuiltInRegistries.BLOCK.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return new GTWireBlock(aVoltage, 1, aLoss, aMaterial, 1, aInsulated, 2, aFamily, BlockBehaviour.Properties.of());
	}

	// ---------------------------------------------------------------------------
	// the carrier matrix (NBT_CONTACTDAMAGE, GTWireSpecs row derived)
	// ---------------------------------------------------------------------------

	@Test
	public void bareShockRowsCarryTheFlag() {
		assertTrue(block(MT.Sn, false, Family.ELECTRIC, 32, 2).contactDamage(),
				"tin bare wire — contactDamageWire = T (Loader:1914)");
		assertTrue(block(MT.Cu, false, Family.ELECTRIC, 256, 2).contactDamage(),
				"copper bare wire — contactDamageWire = T (Loader:1918)");
	}

	@Test
	public void cablesAndPureWiresStayInert() {
		assertFalse(block(MT.Sn, true, Family.ELECTRIC, 32, 1).contactDamage(),
				"insulated cable — contactDamageCable = F on EVERY row (upstream :89-93)");
		assertFalse(block(MT.Graphene, false, Family.ELECTRIC, 128, 2).contactDamage(),
				"graphene pure wire — F, F (Loader:1948)");
		assertFalse(block(MT.Superconductor, false, Family.ELECTRIC, 32768, 1).contactDamage(),
				"superconductor pure wire — F, F (Loader:1950)");
	}

	@Test
	public void redstoneRowsAndRowlessBlocksAreInert() {
		assertFalse(block(MT.RedAlloy, false, Family.REDSTONE, 0, GTWireSpecs.MAX_RANGE / 16).contactDamage(),
				"red_alloy wire — the redstone registration carries no CONTACTDAMAGE at all (Loader:1893-1902)");
		assertFalse(block(null, false, Family.ELECTRIC, 32, 1).contactDamage(),
				"the p7 legacy pair has no row identity — the upstream field default F (:57, flag read only when present :64)");
	}

	// ---------------------------------------------------------------------------
	// the damage formula (tierMax(wattage) * 4, the :3024-3031 expression)
	// ---------------------------------------------------------------------------

	/** The full ladder: every V-table rung, 0 and the beyond-the-ladder overflow. */
	@Test
	public void damageFormulaIsTierMaxTimesFour() {
		GTWireBlock tBlock = block(MT.Sn, false, Family.ELECTRIC, 32, 2);
		GTWireBlockEntity tWire = sType.create(POS, tBlock.defaultBlockState());
		long[][] tTable = {
				{0L, 0}, // the unpowered gate — tierMax(0) = 0
				{8L, 0}, // V[0] = 8 -> tier 0 -> a ULV packet cannot shock
				{32L, 4}, // V[1] -> tier 1 (the card: 32EU -> 4)
				{128L, 8}, // V[2]
				{512L, 12}, // V[3]
				{2048L, 16}, // V[4] (the card: 2048EU -> 16)
				{8192L, 20}, // V[5]
				{32768L, 24}, // V[6]
				{131072L, 28}, // V[7]
				{524288L, 32}, // V[8]
				{2097152L, 36}, // V[9]
				{8388608L, 40}, // V[10]
				{33554432L, 44}, // V[11]
				{134217728L, 48}, // V[12]
				{536870912L, 52}, // V[13]
				{2147483648L, 56}, // V[14]
				{8589934592L, 60}, // V[15] (the card: 8G -> 60)
				{34359738368L, 64}, // beyond the ladder — tierMax returns V.length = 16
		};
		for (long[] tRung : tTable) {
			tWire.mWattageLast = tRung[0];
			assertEquals(tRung[1], tWire.pendingContactDamage(),
					"tierMax(" + tRung[0] + ") * 4");
		}
		tWire.mWattageLast = -32; // the negative form — tierMax abs's its input (upstream :1390)
		assertEquals(4, tWire.pendingContactDamage(), "tierMax(-32) * 4");
	}

	/** The :203 semantics: no transferred wattage, no bite. */
	@Test
	public void unpoweredWireNeverBites() {
		GTWireBlock tBlock = block(MT.Sn, false, Family.ELECTRIC, 32, 2);
		GTWireBlockEntity tWire = sType.create(POS, tBlock.defaultBlockState());
		assertEquals(0, tWire.mWattageLast, "a fresh BE never transferred (and the transient field reloads as 0 — the p7 NBT contract)");
		assertEquals(0, tWire.pendingContactDamage(), "mWattageLast = 0 -> tierMax = 0 -> the :203 shock body is a no-op");
	}

	/** The family gate: a redstone row mounts no shock semantics at all. */
	@Test
	public void redstoneFamilyNeverShocks() {
		GTWireBlock tRedstone = block(MT.RedAlloy, false, Family.REDSTONE, 0, GTWireSpecs.MAX_RANGE / 16);
		GTWireBlockEntity tWire = sType.create(POS, tRedstone.defaultBlockState());
		assertTrue(tWire.isRedstone(), "the fixture is a redstone-family BE");
		tWire.mWattageLast = 2048; // even WITH a live transfer booked, the gate refuses
		assertEquals(0, tWire.pendingContactDamage(), "the isRedstone() gate pins the family lock (spec 1)");
	}

	/** The immunity predicate's offline half: a non-player entity never qualifies. */
	@Test
	public void nonPlayerEntitiesNeverImmune() {
		assertFalse(GTWireBlockEntity.isContactImmune(null), "null (and any non-Player) — no immunity");
	}

	// ---------------------------------------------------------------------------
	// the collision inset (upstream :219 — the enabler of the entityInside hook)
	// ---------------------------------------------------------------------------

	@Test
	public void contactBlocksShrinkTheCollisionBox() {
		// the super path reads the state through a BlockGetter — the vanilla empty stub is enough offline
		net.minecraft.world.level.BlockGetter tLevel = net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
		GTWireBlock tContactBlock = block(MT.Sn, false, Family.ELECTRIC, 32, 2);
		VoxelShape tShape = tContactBlock
				.getCollisionShape(tContactBlock.defaultBlockState(), tLevel, POS, CollisionContext.empty());
		for (Direction.Axis tAxis : Direction.Axis.values()) {
			assertEquals(0.125, tShape.min(tAxis), 1.0e-9, "PX_P[2] inset on " + tAxis);
			assertEquals(0.875, tShape.max(tAxis), 1.0e-9, "PX_N[2] inset on " + tAxis);
		}
		GTWireBlock tCableBlock = block(MT.Sn, true, Family.ELECTRIC, 32, 1);
		VoxelShape tInert = tCableBlock
				.getCollisionShape(tCableBlock.defaultBlockState(), tLevel, POS, CollisionContext.empty());
		assertEquals(Shapes.block().min(Direction.Axis.Y), tInert.min(Direction.Axis.Y), 1.0e-9,
				"an inert cable keeps the full-cube collision box");
		assertEquals(Shapes.block().max(Direction.Axis.Y), tInert.max(Direction.Axis.Y), 1.0e-9);
	}
}
