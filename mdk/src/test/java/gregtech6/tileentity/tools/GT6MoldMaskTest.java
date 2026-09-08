package gregtech6.tileentity.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.maps.GT6RecipeMapCrucible;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The card-B mold truth tables (task p26-crucible-mold-faucet acceptance b): the FULL
 * {@link TileEntityMold#MOLD_RECIPES} universe — the 30 pre-carved ceramic shapes
 * (Loader_MultiTileEntities.java:391-420) map 30/30 onto their prefixes through the
 * ported :628-921 static block; the nugget single-center-bit falls back (:79-83); the
 * 25-bit U9 census (:234-243); the COOL2CRYSTAL plate→plateGem swap (:194-197/:250-253)
 * driving a full pour-and-solidify cycle on the offline BE fixture.
 */
public class GT6MoldMaskTest extends GTOfflineTestBase {

	/** The expected prefix NAME per ceramic row path (the Loader :391-420 reading). */
	private static final Map<String, String> EXPECTED = new LinkedHashMap<>();
	static {
		EXPECTED.put("mold_ceramic_ingot", "ingot");
		EXPECTED.put("mold_ceramic_billet", "billet");
		EXPECTED.put("mold_ceramic_chunk", "chunkGt");
		EXPECTED.put("mold_ceramic_plate", "plate");
		EXPECTED.put("mold_ceramic_tiny_plate", "plateTiny");
		EXPECTED.put("mold_ceramic_bolt", "bolt");
		EXPECTED.put("mold_ceramic_rod", "stick");
		EXPECTED.put("mold_ceramic_long_rod", "stickLong");
		EXPECTED.put("mold_ceramic_item_casing", "casingSmall");
		EXPECTED.put("mold_ceramic_ring", "ring");
		EXPECTED.put("mold_ceramic_gear", "gearGt");
		EXPECTED.put("mold_ceramic_small_gear", "gearGtSmall");
		EXPECTED.put("mold_ceramic_sword", "toolHeadRawSword");
		EXPECTED.put("mold_ceramic_pickaxe", "toolHeadRawPickaxe");
		EXPECTED.put("mold_ceramic_spade", "toolHeadRawSpade");
		EXPECTED.put("mold_ceramic_shovel", "toolHeadRawShovel");
		EXPECTED.put("mold_ceramic_universal_spade", "toolHeadRawUniversalSpade");
		EXPECTED.put("mold_ceramic_axe", "toolHeadRawAxe");
		EXPECTED.put("mold_ceramic_double_axe", "toolHeadRawAxeDouble");
		EXPECTED.put("mold_ceramic_saw", "toolHeadRawSaw");
		EXPECTED.put("mold_ceramic_hammer", "toolHeadHammer");
		EXPECTED.put("mold_ceramic_file", "toolHeadFile");
		EXPECTED.put("mold_ceramic_screwdriver", "toolHeadScrewdriver");
		EXPECTED.put("mold_ceramic_chisel", "toolHeadRawChisel");
		EXPECTED.put("mold_ceramic_arrow", "toolHeadRawArrow");
		EXPECTED.put("mold_ceramic_hoe", "toolHeadRawHoe");
		EXPECTED.put("mold_ceramic_sense", "toolHeadRawSense");
		EXPECTED.put("mold_ceramic_plow", "toolHeadRawPlow");
		EXPECTED.put("mold_ceramic_builderwand", "toolHeadBuilderwand");
		// the nugget row deliberately NOT in the table — the :82 fallback answers it
	}

	static BlockEntityType<TileEntityMold> sMoldType;
	static final BlockPos POS = new BlockPos(3, 4, 5);

	static MaterialPrefixItem PLATE_GEM_GLASS;

	@BeforeAll
	static void boot() {
		MaterialRegistry.INSTANCE.open();
		MT.init();
		gregapi.data.OP.init();
		MaterialRegistry.INSTANCE.close();
		BlockEntityType<TileEntityMold>[] tTypes = (BlockEntityType<TileEntityMold>[]) new BlockEntityType<?>[1];
		tTypes[0] = BlockEntityType.Builder.of((aPos, aState) -> new TileEntityMold(tTypes[0], aPos, aState), Blocks.STONE).build(null);
		sMoldType = tTypes[0];
		PLATE_GEM_GLASS = probe("p26mold_probe_plategem_glass", () -> new MaterialPrefixItem(new Item.Properties(), OP.plateGem, MT.Glass));
		GT6RecipeMapCrucible.sMatResolver = r -> {
			if (r.prefix() == OP.plateGem && r.material() == MT.Glass) {
				return r.count() < 1 ? null : new ItemStack(PLATE_GEM_GLASS, (int)Math.min(64, r.count()));
			}
			return null;
		};
	}

	@AfterAll
	static void restoreMatResolver() {
		GT6RecipeMapCrucible.sMatResolver = GT6RecipeMapCrucible.DEFAULT_MAT_RESOLVER;
	}

	/** The probe-item helper (the GT6RecipeMapCrucibleTest posture). */
	private static MaterialPrefixItem probe(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
		var tRegistry = BuiltInRegistries.ITEM;
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			java.lang.reflect.Field tDelegate = null;
			for (Class<?> tClass = tRegistry.getClass(); tClass != null && tDelegate == null; tClass = tClass.getSuperclass()) {
				try { tDelegate = tClass.getDeclaredField("delegate"); } catch (NoSuchFieldException ignored) {}
			}
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			java.lang.reflect.Field tLocked = null;
			for (Class<?> tClass = tRegistry.getClass(); tClass != null && tLocked == null; tClass = tClass.getSuperclass()) {
				try { tLocked = tClass.getDeclaredField("locked"); } catch (NoSuchFieldException ignored) {}
			}
			tLocked.setAccessible(true);
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry", aE);
		}
		MaterialPrefixItem rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, new ResourceLocation("gt6", aProbeId), rItem);
		return rItem;
	}

	// ------------------------------------------------------------------------------------
	// the 30/30 mask coverage + the fallback
	// ------------------------------------------------------------------------------------

	/** Every pre-carved ceramic row maps onto its upstream prefix through the ported table. */
	@Test
	public void all30CeramicShapesMapToTheirPrefixes() {
		assertEquals(30, GT6Molds.CERAMIC_ROWS.size(), "the Loader :391-420 row count");
		assertEquals(0, GT6Molds.CERAMIC_BLANK_ROW.preCarvedShape(), "the blank row ships uncarved");
		for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
			String tExpected = EXPECTED.get(tRow.path());
			OreDictPrefix tGot = TileEntityMold.getMoldRecipe(tRow.preCarvedShape());
			if (tExpected != null) {
				assertNotNull(tGot, "the table must answer " + tRow.path());
				assertEquals(tExpected, tGot.mNameInternal, "the " + tRow.path() + " mask");
			} else {
				// the nugget row: the single-center-bit mask is NOT in the table → OP.nugget
				assertSame(OP.nugget, tGot, "the " + tRow.path() + " mask rides the :82 fallback");
			}
		}
	}

	/** The table universe size (the upstream static block's 549 final rows under HashMap dedup). */
	@Test
	public void tableSizeMatchesTheUpstreamUniverse() {
		assertTrue(TileEntityMold.MOLD_RECIPES.size() >= 500,
				"the ported :628-921 block fills the universe, got " + TileEntityMold.MOLD_RECIPES.size());
	}

	/** The :79-83 fallback pair — zero shape answers null, the unknown non-zero shapes answer nugget. */
	@Test
	public void nuggetFallback() {
		assertNull(TileEntityMold.getMoldRecipe(0));
		// the all-25-bits shape IS the registered plate mask
		assertSame(OP.plate, TileEntityMold.getMoldRecipe(TileEntityMold.SHAPE_MASK));
		int tCenter = 1 << 12;
		assertSame(OP.nugget, TileEntityMold.getMoldRecipe(tCenter), "the single-center-bit nugget mask");
		assertSame(OP.nugget, TileEntityMold.getMoldRecipe((1 << 0) | (1 << 24)), "the opposite-corners mask is absent from the universe");
	}

	/** The A-card ingot bars survive the full-table fill (same keys, same value). */
	@Test
	public void ingotBarsStillMap() {
		for (int i = 0; i < 3; i++) assertSame(OP.ingot, TileEntityMold.getMoldRecipe(TileEntityMold.ingotShape(i)));
	}

	// ------------------------------------------------------------------------------------
	// the measurement (:234-243)
	// ------------------------------------------------------------------------------------

	/** The nugget census: each set bit costs one U9, the plate bar costs a full U. */
	@Test
	public void measurementU9CensusAndPrefixAmount() {
		TileEntityMold tMold = new TileEntityMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		tMold.mShape = 1 << 12; // one nugget cell
		assertEquals(CS.U9, tMold.getMoldRequiredMaterialUnits());
		tMold.mShape = (1 << 12) | (1 << 6); // two nugget cells
		assertEquals(2 * CS.U9, tMold.getMoldRequiredMaterialUnits());
		tMold.mShape = TileEntityMold.ingotShape(1);
		assertEquals(OP.ingot.mAmount, tMold.getMoldRequiredMaterialUnits());
		tMold.mShape = 0b0_11111_11111_11111_11111_11111; // the plate
		assertEquals(OP.plate.mAmount, tMold.getMoldRequiredMaterialUnits());
		tMold.mShape = 0;
		assertEquals(0, tMold.getMoldRequiredMaterialUnits());
	}

	// ------------------------------------------------------------------------------------
	// the COOL2CRYSTAL swap (:250-253)
	// ------------------------------------------------------------------------------------

	/** Glass carries COOL2CRYSTAL — the plate answers the gem plate; iron stays metallic. */
	@Test
	public void cool2CrystalSwap() {
		assertTrue(MT.Glass.contains(gregapi.data.TD.Processing.COOL2CRYSTAL), "precondition: glass cools crystalline");
		assertFalse(MT.Iron.contains(gregapi.data.TD.Processing.COOL2CRYSTAL), "precondition: iron cools metallic");
		assertSame(OP.plateGem, TileEntityMold.cool2CrystalSwap(OP.plate, MT.Glass));
		assertSame(OP.plateGemTiny, TileEntityMold.cool2CrystalSwap(OP.plateTiny, MT.Glass));
		assertSame(OP.ingot, TileEntityMold.cool2CrystalSwap(OP.ingot, MT.Glass));
		assertNull(TileEntityMold.cool2CrystalSwap(null, MT.Glass));
		assertSame(OP.plate, TileEntityMold.cool2CrystalSwap(OP.plate, MT.Iron));
	}

	/** The full pour-and-solidify cycle: hot glass into a plate mold cools into gem plates. */
	@Test
	public void pourAndSolidifyGlassIntoGemPlates() {
		TileEntityMold tMold = new TileEntityMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		tMold.mShape = 0b0_11111_11111_11111_11111_11111; // the plate shape
		OreDictMaterialStack tGlass = new OreDictMaterialStack(MT.Glass, CS.U * 2);
		long tTaken = tMold.fillMold(tGlass, 1300, TileEntityMold.SIDE_TOP); // above the 1200 K melt, below the 1875 K stone ceiling
		assertTrue(tTaken > 0, "the plate mold accepts the hot glass");
		assertNotNull(tMold.mContent);
		assertEquals(1300, tMold.mTemperature);
		// tick the cooldown — ±5 K per tick toward the 293 K offline environment
		for (long tTimer = 0; tMold.mInventory.isEmpty() && tTimer < 500; tTimer++) {
			tMold.onTick(tTimer, true);
		}
		assertFalse(tMold.mInventory.isEmpty(), "the mold pours out once below the melting point");
		ItemStack tOutput = tMold.mInventory.get();
		assertSame(PLATE_GEM_GLASS, tOutput.getItem(), "the COOL2CRYSTAL swap made the output a GEM plate");
		// the count = mContent / the LIVE plateGem stat (OP.java:1246 ships plateGem = U in this port)
		assertEquals((int)(CS.U / OP.plateGem.mAmount), tOutput.getCount(), "the payout divides the content by the live prefix stat");
		assertEquals(0, tMold.mContent.mAmount, "the content paid out");
	}

	// ------------------------------------------------------------------------------------
	// the wrench state machine (:337-355) + NBT round-trip
	// ------------------------------------------------------------------------------------

	/** The monkey wrench toggles per-side bits; the soft hammer clears everything. */
	@Test
	public void wrenchStateMachine() {
		TileEntityMold tMold = new TileEntityMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(0, tMold.mAutoPullDirections);
		assertEquals("Crucible Auto-Input: ON", tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.NORTH.get3DDataValue()));
		assertEquals(1 << 2, tMold.mAutoPullDirections);
		assertEquals("Crucible Auto-Input: ON", tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.EAST.get3DDataValue()));
		assertEquals((1 << 2) | (1 << 5), tMold.mAutoPullDirections);
		assertEquals("Crucible Auto-Input: OFF", tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.EAST.get3DDataValue()));
		assertEquals(1 << 2, tMold.mAutoPullDirections);
		assertEquals("Crucible Auto-Input: REDSTONE", tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.UP.get3DDataValue()));
		assertTrue(tMold.mUseRedstone);
		assertEquals("Crucible Auto-Input: OFF & NO REDSTONE", tMold.toolSoftHammer());
		assertEquals(0, tMold.mAutoPullDirections);
		assertFalse(tMold.mUseRedstone);
	}

	/** The auto-pull state rides the NBT (the :98/:106 pair, the trimmed port keys). */
	@Test
	public void autoPullNbtRoundTrip() {
		TileEntityMold tMold = new TestMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.SOUTH.get3DDataValue());
		tMold.toolMonkeyWrench((byte)net.minecraft.core.Direction.UP.get3DDataValue()); // the redstone mode
		CompoundTag tNBT = new CompoundTag();
		((TestMold)tMold).saveTag(tNBT);
		TileEntityMold tLoaded = new TileEntityMold(sMoldType, POS, Blocks.STONE.defaultBlockState());
		tLoaded.load(tNBT);
		assertEquals(tMold.mAutoPullDirections, tLoaded.mAutoPullDirections);
		assertEquals(tMold.mUseRedstone, tLoaded.mUseRedstone);
	}

	/** The saveAdditional/load pair is protected — the same-package subclass exposes it. */
	static final class TestMold extends TileEntityMold {
		TestMold(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}
		void saveTag(CompoundTag aNBT) {
			saveAdditional(aNBT);
		}
	}
}
