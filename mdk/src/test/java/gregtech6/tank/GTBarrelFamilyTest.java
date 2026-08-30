package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.covers.CoverRegistry;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTBarrels.MetalDrumRow;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tank.GTBarrelBlockEntity;
import gregtech6.tileentity.tank.GTBarrelMetalBlockEntity;
import gregtech6.tileentity.tank.GTBarrelPlasticBlockEntity;

/**
 * The barrel family table (task p6-barrel-metal-plastic acceptance ①) — the offline half:
 * the te_name trio, the allowCover predicate table (wood×{plate=T,pump=F},
 * plastic×{plate=T,pump=F}, metal×{plate=T,pump=T}) and the setCoverItem install gate,
 * over the three BE classes (the upstream Wood :39 / Plastic :38 / Metal no-override shape).
 *
 * <p>The capacity/melting block-carrier rows (wood 16000 L/340 K, plastic 32000 L/370 K,
 * metal 64000 L/bridge) are exercised live by {@code /gt6tank stat} in the RCON chain —
 * the same split the p4 card used for the 340 K ceiling (TileEntityBase08BarrelTest
 * header: a mod Block cannot be constructed after the offline boot, the Forge block
 * registries are intrusive-holder registries frozen by Bootstrap, and the offline vanilla
 * fixture blocks keep the BE ctor at the 16000 L / MAX_VALUE defaults).
 *
 * <p>Task p7-barrel-high-tier-melt-bridge extends the offline half with the pure-data
 * surfaces that need no Block construction: the high-tier row truth table
 * (GTBarrels.HIGH_TIER_METAL_DRUMS — capacity/explicit-HU/display-name literals,
 * upstream Loader_MultiTileEntities.java:2159-2170) and the melting-point bridge formula
 * (the verbatim TileEntityBase08Barrel.readFromNBT2 :66 else-branch over the live dataset,
 * GTMaterialItems.initMaterials precedent for the offline material universe).
 */
public class GTBarrelFamilyTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(2, 3, 4);
	static final ResourceLocation TEST_SPRITE = new ResourceLocation("gt6", "block/cover/test_plate");

	static BlockEntityType<GTBarrelBlockEntity> sWoodType;
	static BlockEntityType<GTBarrelPlasticBlockEntity> sPlasticType;
	static BlockEntityType<GTBarrelMetalBlockEntity> sMetalType;

	static CoverTextureSimple sPlateCover;
	static CoverPump sPumpCover;

	@SuppressWarnings("unchecked")
	@BeforeAll
	static void buildOfflineFixtures() {
		// the offline material universe for the p7 bridge assertions (the
		// GT6RecipesCokeOvenTest:60 precedent; per-generation refill, idempotent by design)
		GTMaterialItems.initMaterials();

		BlockEntityType<GTBarrelBlockEntity>[] tWood = (BlockEntityType<GTBarrelBlockEntity>[]) new BlockEntityType<?>[1];
		tWood[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelBlockEntity(tWood[0], aPos, aState),
				Blocks.STONE, Blocks.DIRT).build(null);
		sWoodType = tWood[0];

		BlockEntityType<GTBarrelPlasticBlockEntity>[] tPlastic = (BlockEntityType<GTBarrelPlasticBlockEntity>[]) new BlockEntityType<?>[1];
		tPlastic[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelPlasticBlockEntity(tPlastic[0], aPos, aState),
				Blocks.STONE).build(null);
		sPlasticType = tPlastic[0];

		BlockEntityType<GTBarrelMetalBlockEntity>[] tMetal = (BlockEntityType<GTBarrelMetalBlockEntity>[]) new BlockEntityType<?>[1];
		tMetal[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBarrelMetalBlockEntity(tMetal[0], aPos, aState),
				Blocks.STONE).build(null);
		sMetalType = tMetal[0];

		sPlateCover = new CoverTextureSimple(TEST_SPRITE);
		sPumpCover = new CoverPump();
	}

	@AfterEach
	void clearCoverFixtures() {
		CoverRegistry.reset();
	}

	// ---------------------------------------------------------------------------
	// the three te_names (the upstream trio shape)
	// ---------------------------------------------------------------------------

	@Test
	public void tileEntityNamesMirrorTheBETPaths() {
		assertEquals("barrel_wood", new GTBarrelBlockEntity(sWoodType, POS, Blocks.STONE.defaultBlockState()).getTileEntityName());
		assertEquals("barrel_plastic", new GTBarrelPlasticBlockEntity(sPlasticType, POS, Blocks.STONE.defaultBlockState()).getTileEntityName());
		assertEquals("barrel_metal", new GTBarrelMetalBlockEntity(sMetalType, POS, Blocks.STONE.defaultBlockState()).getTileEntityName());
	}

	/**
	 * The vanilla-fixture default: no GTBarrelBlock under the state, so the BE keeps the
	 * base defaults (16000 L class tank, MAX_VALUE ceiling) — the block-carried rows
	 * themselves are the live /gt6tank stat assertions (see the class header).
	 */
	@Test
	public void offlineFixturesKeepTheBaseDefaults() {
		GTBarrelBlockEntity tWood = new GTBarrelBlockEntity(sWoodType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(16000, tWood.mTank.capacity(), "the class default FluidTankGT(16000) (TileEntityBase08Barrel :87)");
		assertEquals(Long.MAX_VALUE, tWood.mMeltingPoint, "no material bridge: MAX_VALUE default (:55)");

		GTBarrelPlasticBlockEntity tPlastic = new GTBarrelPlasticBlockEntity(sPlasticType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(16000, tPlastic.mTank.capacity());
		assertEquals(Long.MAX_VALUE, tPlastic.mMeltingPoint);

		GTBarrelMetalBlockEntity tMetal = new GTBarrelMetalBlockEntity(sMetalType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(16000, tMetal.mTank.capacity());
		assertEquals(Long.MAX_VALUE, tMetal.mMeltingPoint);

		// voidExcess off: the drum shape never voids — 100 L into a full 16000 L tank is refused
		assertEquals(16000, tMetal.mTank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
		assertEquals(0, tMetal.mTank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
	}

	// ---------------------------------------------------------------------------
	// the allowCover predicate table — the upstream Wood :39 / Plastic :38 /
	// Metal no-override shape, judged through the ICover.isDecorative interface
	// ---------------------------------------------------------------------------

	@Test
	public void allowCoverPredicateTable() {
		// fresh wood/plastic have a null store — the defensive guard refuses (metal: the base default admits)
		GTBarrelBlockEntity tWood = new GTBarrelBlockEntity(sWoodType, POS, Blocks.STONE.defaultBlockState());
		GTBarrelPlasticBlockEntity tPlastic = new GTBarrelPlasticBlockEntity(sPlasticType, POS, Blocks.STONE.defaultBlockState());
		GTBarrelMetalBlockEntity tMetal = new GTBarrelMetalBlockEntity(sMetalType, POS, Blocks.STONE.defaultBlockState());
		assertFalse(tWood.allowCover((byte) 2, sPlateCover), "wood null-store guard (defensive; upstream reaches the gate non-null)");
		assertFalse(tPlastic.allowCover((byte) 2, sPlateCover), "plastic null-store guard (same defensive shape)");
		assertTrue(tMetal.allowCover((byte) 2, sPlateCover), "metal takes the base default (no override, MultiTileEntityBarrelMetal :36-52)");

		// the live predicate with a built store — wood/plastic: decorative only; metal: everything
		tWood.setCovers(CoverRegistry.coverdata(tWood, null));
		tPlastic.setCovers(CoverRegistry.coverdata(tPlastic, null));
		tMetal.setCovers(CoverRegistry.coverdata(tMetal, null));

		assertTrue(tWood.allowCover((byte) 2, sPlateCover), "wood × plate = T (CoverTextureSimple.isDecorative=T)");
		assertFalse(tWood.allowCover((byte) 2, sPumpCover), "wood × pump = F (AbstractCoverDefault.isDecorative=F)");
		assertTrue(tPlastic.allowCover((byte) 2, sPlateCover), "plastic × plate = T (the same upstream :38 predicate)");
		assertFalse(tPlastic.allowCover((byte) 2, sPumpCover), "plastic × pump = F");
		assertTrue(tMetal.allowCover((byte) 2, sPlateCover), "metal × plate = T");
		assertTrue(tMetal.allowCover((byte) 2, sPumpCover), "metal × pump = T — the base default admits the functional covers");
	}

	/**
	 * The end-to-end install gate through {@code ICoverableTE.setCoverItem} (the :146
	 * allowCover door): the return value is the store-consistency verdict, the acceptance
	 * observable is the cover on the face. Vanilla-item cover mounts, the GTCoverTestBase
	 * precedent ({@code Items.BRICKS} plate / {@code Items.BRICK} pump).
	 */
	@Test
	public void installGateAcceptsPlatesAndRejectsPumpsOnWoodAndPlastic() {
		CoverRegistry.put(Items.BRICKS, sPlateCover);
		CoverRegistry.put(Items.BRICK, sPumpCover);

		GTBarrelBlockEntity tWood = new GTBarrelBlockEntity(sWoodType, POS, Blocks.STONE.defaultBlockState());
		assertTrue(tWood.setCoverItem((byte) 2, new ItemStack(Items.BRICKS), null, false, false),
				"the accepted plate install lands (boolean = the request landed, checkIfCoversEmptyAndDeleteIfNeeded always true)");
		assertFalse(tWood.getCoverItem((byte) 2).isEmpty(), "wood × plate install lands (hasCover)");
		assertFalse(tWood.setCoverItem((byte) 3, new ItemStack(Items.BRICK), null, false, false),
				"wood × pump request refused at the :146 allowCover door (the boolean verdict)");
		assertTrue(tWood.getCoverItem((byte) 3).isEmpty(), "wood × pump install REJECTED — nothing on the face");

		GTBarrelPlasticBlockEntity tPlastic = new GTBarrelPlasticBlockEntity(sPlasticType, POS, Blocks.STONE.defaultBlockState());
		assertTrue(tPlastic.setCoverItem((byte) 4, new ItemStack(Items.BRICKS), null, false, false));
		assertFalse(tPlastic.getCoverItem((byte) 4).isEmpty(), "plastic × plate install lands");
		assertFalse(tPlastic.setCoverItem((byte) 5, new ItemStack(Items.BRICK), null, false, false), "plastic × pump request refused");
		assertTrue(tPlastic.getCoverItem((byte) 5).isEmpty(), "plastic × pump install REJECTED");

		GTBarrelMetalBlockEntity tMetal = new GTBarrelMetalBlockEntity(sMetalType, POS, Blocks.STONE.defaultBlockState());
		assertTrue(tMetal.setCoverItem((byte) 2, new ItemStack(Items.BRICKS), null, false, false));
		assertFalse(tMetal.getCoverItem((byte) 2).isEmpty(), "metal × plate install lands");
		assertTrue(tMetal.setCoverItem((byte) 3, new ItemStack(Items.BRICK), null, false, false));
		assertFalse(tMetal.getCoverItem((byte) 3).isEmpty(), "metal × pump install lands — the p5 pump machinery rides the drum");
	}

	// ---------------------------------------------------------------------------
	// task p7-barrel-high-tier-melt-bridge — the high-tier row truth table and the
	// melting-point bridge, the offline (no Block construction) halves
	// ---------------------------------------------------------------------------

	/** The row lookup by registry path (the truth-table rows are keyed by it). */
	private static MetalDrumRow rowByPath(String aPath) {
		return GTBarrels.HIGH_TIER_METAL_DRUMS.stream().filter(aRow -> aRow.path().equals(aPath)).findFirst().orElseThrow();
	}

	private static void assertRow(String aPath, String aDisplay, long aCapacity, long aHU) {
		MetalDrumRow tRow = rowByPath(aPath);
		assertEquals(aDisplay, tRow.displayName(), aPath);
		assertEquals(aCapacity, tRow.capacityL(), aPath);
		assertEquals(aHU, tRow.explicitHU(), aPath);
		assertNotNull(tRow.material().get(), aPath + " — spec ③: the MT constant resolves (zero skip rows)");
	}

	/**
	 * The twelve upstream rows, verbatim (Loader_MultiTileEntities.java:2159-2170): the
	 * capacity ladder, the two explicit NBT_CAPACITY_HU rows, the display names and the
	 * material resolution (spec ③ — no skipped rows: every MT constant lives in the
	 * ported dataset).
	 */
	@Test
	public void highTierMetalDrumRowTruthTable() {
		assertEquals(12, GTBarrels.HIGH_TIER_METAL_DRUMS.size(), "128K×3 + 256K×3 + 512K + 1.024M + 4.096M×2 + 8.192M + 10B");
		assertRow("barrel_tungsten_alloy", "Tungsten Alloy Drum", 128000, -1);
		assertRow("barrel_titanium", "Titanium Drum", 128000, -1);
		assertRow("barrel_netherite", "Netherite Drum", 128000, -1);
		assertRow("barrel_tungstensteel", "Tungstensteel Drum", 256000, -1);
		assertRow("barrel_tungsten", "Tungsten Drum", 256000, -1);
		assertRow("barrel_void_metal", "Voidmetal Drum", 256000, -1);
		assertRow("barrel_tantalum_hafnium_carbide", "Tantalum Hafnium Carbide Drum", 512000, -1);
		assertRow("barrel_gaia_spirit", "Gaia Drum", 1024000, -1);
		assertRow("barrel_adamantium", "Adamantium Drum", 4096000, -1);
		assertRow("barrel_draconium", "Draconium Drum", 4096000, -1);
		assertRow("barrel_awakened_draconium", "Awakened Draconium Drum", 8192000, 10000);
		assertRow("barrel_infinity", "Infinity Drum", 10000000000L, 1000000000L);
	}

	/**
	 * The bridge else-branch (TileEntityBase08Barrel.java:66 {@code (long)(mMeltingPoint *
	 * 1.25)}) over the live dataset — literal spot checks where the material melting
	 * point is directly traceable, plus the per-row formula agreement (no hardcoded
	 * drift) and the 保 MAX clause.
	 */
	@Test
	public void meltingPointBridgeFormula() {
		// traceable literals: Bronze = Cu 1357 K (MT.java:1004 element, :1705 heat(Cu.mMeltingPoint))
		assertEquals(1696, GTBarrels.meltingPointK(MT.Bronze), "(long)(1357 * 1.25) = (long) 1696.25 — the bronze drum ceiling");
		// VoidMetal heat(3000, 5000) (MT.java:2508)
		assertEquals(3750, GTBarrels.meltingPointK(MT.VoidMetal), "(long)(3000 * 1.25)");
		// Ta4HfC5 heat(4263) (MT.java:2467)
		assertEquals(5328, GTBarrels.meltingPointK(MT.Ta4HfC5), "(long)(4263 * 1.25) = (long) 5328.75");
		// Draconium heat(4500) (MT.java:2585)
		assertEquals(5625, GTBarrels.meltingPointK(MT.Draconium), "(long)(4500 * 1.25)");
		// Ad element melt 5225 K (MT.java:1656)
		assertEquals(6531, GTBarrels.meltingPointK(MT.Ad), "(long)(5225 * 1.25) = (long) 6531.25");
		// 保 MAX: a MAX_VALUE material stays never-melting (the upstream double-cast
		// saturation reaches the same ceiling — the explicit branch just states it)
		assertEquals(Long.MAX_VALUE, GTBarrels.meltingPointK(Long.MAX_VALUE), "MAX melting material → MAX ceiling");
		// every formula row agrees with the live dataset — the bridge computes, never guesses
		for (MetalDrumRow tRow : GTBarrels.HIGH_TIER_METAL_DRUMS) {
			if (tRow.explicitHU() >= 0) continue;
			assertEquals((long)(tRow.material().get().mMeltingPoint * 1.25), tRow.meltingPointK(), tRow.path());
		}
	}

	/**
	 * The bridge branch 1 (upstream :66 {@code if (aNBT.hasKey(NBT_CAPACITY_HU))
	 * mMeltingPoint = aNBT.getLong(NBT_CAPACITY_HU)}) — the two rows carrying an explicit
	 * HU are transcribed verbatim and win over the material formula.
	 */
	@Test
	public void meltingPointBridgeExplicitHURows() {
		assertEquals(10000, rowByPath("barrel_awakened_draconium").meltingPointK(),
				"upstream NBT_CAPACITY_HU=10000 (Loader_MultiTileEntities.java:2169)");
		assertEquals(1000000000L, rowByPath("barrel_infinity").meltingPointK(),
				"upstream NBT_CAPACITY_HU=1000000000 (:2170) — the effectively-never-melting Infinity ceiling");
	}
}
