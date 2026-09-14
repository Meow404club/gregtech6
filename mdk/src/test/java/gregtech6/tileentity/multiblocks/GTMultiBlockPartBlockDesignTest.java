package gregtech6.tileentity.multiblocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import static org.junit.jupiter.api.Assertions.*;

import gregtech6.block.multiblock.GTMultiBlockPartBlock;

/**
 * The DESIGN render dimension tests (task p29-w3-nbtdesign-parts ① acceptance ②):
 * the property range follows the upstream NBT_DESIGNS census
 * ({@code mTextures[bind8(NBT_DESIGNS)+1][6]} — range 0..N inclusive,
 * MultiTileEntityMultiBlockPart.java:138-146), and the BE design write lands in the
 * blockstate (the upstream updateClientData :227 client-data push re-formed — the
 * checker's per-cell design write via Util.checkAndSetTarget becomes visible).
 */
public class GTMultiBlockPartBlockDesignTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos PART_POS = new BlockPos(50, 64, 50);

	static GTMultiBlockPartBlock sWall7;      // the metalwall/metalwalldense family (NBT_DESIGNS 7)
	static GTMultiBlockPartBlock sCoil1;      // the coil family (NBT_DESIGNS 1)
	static GTMultiBlockPartBlock sWheels3;    // crusherwheels/shredderblades (NBT_DESIGNS 3)
	static GTMultiBlockPartBlock sCentrifuge8;// centrifugeparts (NBT_DESIGNS 8)
	static GTMultiBlockPartBlock sSingle0;    // the DESIGNS-0 rows (ventilation, PU, wood wall, ...)
	static BlockEntityType<MultiBlockPartBlockEntity> sDesignPartType;

	@BeforeAll
	static void buildDesignFixtures() {
		// the BLOCK registry write window (the GT6SingleBlockFacingIntegrityTest recipe —
		// the Block ctor registers its intrusive holder, a frozen registry rejects it)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		BlockBehaviour.Properties tProps = BlockBehaviour.Properties.of();
		sWall7 = new GTMultiBlockPartBlock(tProps, 7);
		sCoil1 = new GTMultiBlockPartBlock(tProps, 1);
		sWheels3 = new GTMultiBlockPartBlock(tProps, 3);
		sCentrifuge8 = new GTMultiBlockPartBlock(tProps, 8);
		sSingle0 = new GTMultiBlockPartBlock(tProps);
		// the part BET with the design blocks in its valid list (the 21.1 BE ctor
		// validates the type/state pair — the facing-test fixture lesson)
		@SuppressWarnings("unchecked")
		BlockEntityType<MultiBlockPartBlockEntity>[] tHolder = (BlockEntityType<MultiBlockPartBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new MultiBlockPartBlockEntity(tHolder[0], aPos, aState),
				sWall7, sCoil1, sWheels3, sCentrifuge8, sSingle0).build(null);
		sDesignPartType = tHolder[0];
	}

	@Test
	void designRangeFollowsTheDesigsCensus() {
		// NBT_DESIGNS is the VARIANT COUNT (mTextures[bind8(n)+1][6]) — the property is 0..N inclusive
		assertNull(sSingle0.DESIGN, "DESIGNS 0 rows carry no property (a single-value IntegerProperty cannot exist)");
		assertTrue(sSingle0.defaultBlockState().getProperties().isEmpty(), "the DESIGNS-0 state is the property-less singleton");
		assertEquals(2, sCoil1.DESIGN.getPossibleValues().size(), "coil NBT_DESIGNS 1 = designs 0..1");
		assertEquals(4, sWheels3.DESIGN.getPossibleValues().size(), "wheels/blades NBT_DESIGNS 3 = designs 0..3");
		assertEquals(8, sWall7.DESIGN.getPossibleValues().size(), "walls NBT_DESIGNS 7 = designs 0..7 (dense design2 = the Dynamo emitter plate)");
		assertEquals(9, sCentrifuge8.DESIGN.getPossibleValues().size(), "centrifugeparts NBT_DESIGNS 8 = designs 0..8");
		assertEquals(0, sWall7.defaultBlockState().getValue(sWall7.DESIGN), "every part places at design 0");
		assertEquals(0, sSingle0.designOf(sSingle0.defaultBlockState()), "the property-less read is design 0");
		assertSame(new GTMultiBlockPartBlock(BlockBehaviour.Properties.of(), 7).DESIGN, sWall7.DESIGN,
				"same-range blocks share ONE property instance (the 21.1 identity lookup, GTBlockProperties lesson)");
	}

	@Test
	void setDesignFlipsTheBlockstateVariant() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		MultiBlockPartBlockEntity tPart = sDesignPartType.create(PART_POS, sWall7.defaultBlockState());
		tPart.setLevel(tLevel);
		tLevel.mStates.put(PART_POS, sWall7.defaultBlockState());
		tLevel.mBlockEntities.put(PART_POS, tPart);

		assertTrue(tPart.setDesign(2), "a new design reports the write");
		assertEquals(2, tLevel.getBlockState(PART_POS).getValue(sWall7.DESIGN), "mDesign lands in the blockstate (dense_wall design2 = the Dynamo emitter plate render slot)");
		assertEquals(2, tPart.mDesign);

		assertFalse(tPart.setDesign(2), "the same design is a no-op");
		assertEquals(2, tLevel.getBlockState(PART_POS).getValue(sWall7.DESIGN));
	}

	@Test
	void outOfRangeDesignsClampIntoThePropertyDomain() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		MultiBlockPartBlockEntity tPart = sDesignPartType.create(PART_POS, sCoil1.defaultBlockState());
		tPart.setLevel(tLevel);
		tLevel.mStates.put(PART_POS, sCoil1.defaultBlockState());
		tLevel.mBlockEntities.put(PART_POS, tPart);

		tPart.setDesign(255); // the NBT domain is 0..255 (UT.Code.bind8), the coil family tops at design 1
		assertEquals(255, tPart.mDesign, "mDesign keeps the full upstream 8-bit domain");
		assertEquals(1, tLevel.getBlockState(PART_POS).getValue(sCoil1.DESIGN), "the state clamps to the block's own range");
	}

	@Test
	void loadReappliesThePersistedDesignToTheState() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		MultiBlockPartBlockEntity tPart = sDesignPartType.create(PART_POS, sWall7.defaultBlockState());
		tPart.setLevel(tLevel);
		tLevel.mStates.put(PART_POS, sWall7.defaultBlockState());
		tLevel.mBlockEntities.put(PART_POS, tPart);

		// the save/load round-trip (the NBT leg already pinned by MultiBlockPartBlockEntityTest)
		tPart.setDesign(5);
		net.minecraft.nbt.CompoundTag tTag = tPart.saveWithoutMetadata();
		MultiBlockPartBlockEntity tReloaded = sDesignPartType.create(PART_POS, sWall7.defaultBlockState());
		tReloaded.setLevel(tLevel);
		tReloaded.load(tTag);
		assertEquals(5, tReloaded.mDesign);
		// onLoad re-lands the render slot (the fixture level never fires it — called like vanilla does)
		tReloaded.onLoad();
		assertEquals(5, tLevel.getBlockState(PART_POS).getValue(sWall7.DESIGN), "the persisted design re-lands in its blockstate");
	}

	@Test
	void nonPartBlocksKeepTheSyncSilent() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		// the pre-card fixture shape: a vanilla block carries no design property
		MultiBlockPartBlockEntity tPart = sPartType.create(PART_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mStates.put(PART_POS, Blocks.BRICKS.defaultBlockState());
		tLevel.mBlockEntities.put(PART_POS, tPart);

		assertTrue(tPart.setDesign(4), "the NBT write still happens for legacy states");
		assertEquals(4, tPart.mDesign);
		assertEquals(Blocks.BRICKS, tLevel.getBlockState(PART_POS).getBlock(), "a non-part blockstate is untouched");
	}

	@Test
	void unlevelledSyncIsASilentNoOp() {
		MultiBlockPartBlockEntity tPart = sDesignPartType.create(PART_POS, sWall7.defaultBlockState());
		assertDoesNotThrow(() -> tPart.setDesign(3), "no level = nothing to sync");
		assertEquals(3, tPart.mDesign);
	}

	@Test
	void designZeroClearanceReachesTheState() {
		// the upstream getTarget drop path (:208-209) resets the design — the flip must reach design 0
		MultiBlockLevel tLevel = new MultiBlockLevel();
		MultiBlockPartBlockEntity tPart = sDesignPartType.create(PART_POS, sWheels3.defaultBlockState());
		tPart.setLevel(tLevel);
		tLevel.mStates.put(PART_POS, sWheels3.defaultBlockState());
		tLevel.mBlockEntities.put(PART_POS, tPart);

		tPart.setDesign(3);
		assertEquals(3, tLevel.getBlockState(PART_POS).getValue(sWheels3.DESIGN));
		assertTrue(tPart.setDesign(0));
		assertEquals(0, tLevel.getBlockState(PART_POS).getValue(sWheels3.DESIGN), "the design-0 reset is a real variant flip");
	}
}
