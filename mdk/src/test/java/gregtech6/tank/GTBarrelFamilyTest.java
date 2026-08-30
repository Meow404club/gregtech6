package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import gregtech6.covers.CoverRegistry;
import gregtech6.covers.covers.CoverPump;
import gregtech6.covers.covers.CoverTextureSimple;
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
 * metal 64000 L/MAX) are exercised live by {@code /gt6tank stat} in the p6 RCON chain —
 * the same split the p4 card used for the 340 K ceiling (TileEntityBase08BarrelTest
 * header: a mod Block cannot be constructed after the offline boot, the Forge block
 * registries are intrusive-holder registries frozen by Bootstrap, and the offline vanilla
 * fixture blocks keep the BE ctor at the 16000 L / MAX_VALUE defaults).
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
}
