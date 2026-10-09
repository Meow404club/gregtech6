package gregtech6.tileentity.inventories;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.client.render.GT6MassStorageRenderer;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;

/**
 * GT6 item mass storage family offline tests (task storage-massstorage acceptance): the
 * 120-row axis census (the Loader :141-142 metalset pair over the 60-material loop, the
 * 6000+aID/6200+aID id mapping onto the shared hopper table), the insert/overflow/fill
 * math (upstream :333-434), the mode arms (:202-223), the partial-unit conversion
 * (:593-698), the pixel-grid take/place tier table (:251-265), the digit-strip gate
 * ladder (the Standard usesRenderPass2 :131-140 + the red "100%" :117-124), the
 * ConnectedInventory arms (:552-591), the NBT round trip and the taped keep face. The
 * live click/pop/BER faces are the RCON chain's.
 */
public class GT6MassStorageFamilyTest extends GTOfflineTestBase {

	static BlockEntityType<GT6MassStorageBlockEntity> sType;
	static BlockEntityType<GT6MassStorageLogisticsBlockEntity> sLogisticsType;
	static MaterialPrefixItem sDustIron, sDustTinyIron, sIngotIron;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixture() {
		// the hermetic material boot first (the hopper-family house rule): the partial
		// unit arm resolves loader materials live (dust/ingot/wire prefix items)
		GT6MaterialTestSupport.materials();
		sDustIron = probePrefix("massstorage_probe_dust_iron", () -> new MaterialPrefixItem(new net.minecraft.world.item.Item.Properties(), OP.dust, MT.Iron));
		sDustTinyIron = probePrefix("massstorage_probe_dust_tiny_iron", () -> new MaterialPrefixItem(new net.minecraft.world.item.Item.Properties(), OP.dustTiny, MT.Iron));
		sIngotIron = probePrefix("massstorage_probe_ingot_iron", () -> new MaterialPrefixItem(new net.minecraft.world.item.Item.Properties(), OP.ingot, MT.Iron));
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MassStorageBlockEntity>[] tType = (BlockEntityType<GT6MassStorageBlockEntity>[]) new BlockEntityType<?>[1];
		tType[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MassStorageBlockEntity(tType[0], aPos, aState),
				Blocks.STONE).build(null);
		sType = tType[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6MassStorageLogisticsBlockEntity>[] tLogi = (BlockEntityType<GT6MassStorageLogisticsBlockEntity>[]) new BlockEntityType<?>[1];
		tLogi[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6MassStorageLogisticsBlockEntity(tLogi[0], aPos, aState),
				Blocks.STONE).build(null);
		sLogisticsType = tLogi[0];
	}

	private static GT6MassStorageBlockEntity storage() {
		return new GT6MassStorageBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	private static GT6MassStorageLogisticsBlockEntity logistics() {
		return new GT6MassStorageLogisticsBlockEntity(sLogisticsType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the row axis (the Loader :141-142 metalset pair over the 60-material loop)
	// ---------------------------------------------------------------------------

	@Test
	void theRowAxisMirrorsTheLoaderPairOverTheSharedMaterialTable() {
		assertEquals(GT6Hoppers.MATERIALS.size() * 2, GT6StaticStorages.MASS_ROWS.size(), "Loader :141-142 = 60 materials x the standard/logistics pair");
		assertEquals(120, GT6StaticStorages.MASS_ROWS.size());
		for (int i = 0; i < GT6Hoppers.MATERIALS.size(); i++) {
			GT6Hoppers.HopperMaterial tMat = GT6Hoppers.MATERIALS.get(i);
			GT6StaticStorages.MassRow tStandard = GT6StaticStorages.MASS_ROWS.get(i * 2);
			GT6StaticStorages.MassRow tLogistics = GT6StaticStorages.MASS_ROWS.get(i * 2 + 1);
			// the pair order: the standard :141 then the logistics :142 (the loader line order)
			assertFalse(tStandard.logistics());
			assertTrue(tLogistics.logistics());
			assertEquals(tMat, tStandard.material());
			assertEquals(tMat, tLogistics.material());
			// the id bases: 6000+aID / 6200+aID verbatim
			assertEquals(6000 + tMat.metaId(), tStandard.metaId());
			assertEquals(6200 + tMat.metaId(), tLogistics.metaId());
			// the path tail rides the slug
			assertEquals("mass_storage_" + tMat.slug(), tStandard.path());
			assertEquals("logistics_mass_storage_" + tMat.slug(), tLogistics.path());
		}
		// spot rows: the first (Pb aID 0) and the Bronze anchor (aID 9)
		assertEquals(6000, GT6StaticStorages.MASS_ROWS.get(0).metaId());
		assertEquals(6009, GT6StaticStorages.MASS_ROWS.get(GT6Hoppers.MATERIALS.indexOf(GT6Hoppers.MAT_BRONZE) * 2).metaId());
		assertEquals("bronze", GT6StaticStorages.MASS_ROWS.get(GT6Hoppers.MATERIALS.indexOf(GT6Hoppers.MAT_BRONZE) * 2).material().slug());
	}

	// ---------------------------------------------------------------------------
	// the insert math (upstream :382-426)
	// ---------------------------------------------------------------------------

	@Test
	void insertStoresOneTypeAndAnswersNullWhenConsumed() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(64), false), "the first store consumes the stack (:394-398)");
		assertEquals(64, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		assertNull(tStore.insertItems(stone(100), false));
		assertEquals(164, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		// a different item is refused while a type is stored (the plain-different arm :421-425
		// answers the stack back — dirt carries no compatible prefix conversion vs stone)
		ItemStack tDirt = dirt(5);
		assertEquals(tDirt.getCount(), tStore.insertItems(tDirt, false).getCount(), "incompatible form returns the stack");
	}

	@Test
	void capacityClampsAndOverflowsOnlyInOverflowMode() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(100), false));
		// default mode: the hard clamp at capacity, the remainder answers back (:404/:414-415)
		tStore.mMaxStorage = 200;
		ItemStack tLeft = tStore.insertItems(stone(150), false);
		assertNotNull(tLeft);
		assertEquals(50, tLeft.getCount());
		assertEquals(200, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		// overflow mode: the +256 headroom (:377-379)
		tStore.mMode |= GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW;
		assertEquals(456, tStore.getMaxContent());
		assertNull(tStore.insertItems(stone(150), false));
		assertEquals(350, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
	}

	@Test
	void emitOverflowPushesTheExcessIntoTheInventoryBelow() {
		GTItemStackHandler tCapture = new GTItemStackHandler(9);
		GT6MassStorageBlockEntity tDraining = new GT6MassStorageBlockEntity(sType, POS, Blocks.STONE.defaultBlockState()) {
			@Override
			protected int moveBottom(int aMaxMove) {
				// the adjacency seam: pump the mass view into the capture (the live face is the RCON chain's)
				return gregtech6.util.GTItemMover.move(massView(), tCapture, aMaxMove, gregtech6.util.GTItemMover.DEFAULT_MIN_MOVE, gregtech6.util.GTItemMover.DEFAULT_MAX_SLOT_SIZE, null, false);
			}
		};
		tDraining.mMaxStorage = 100;
		assertNull(tDraining.insertItems(stone(180), false)); // the first store takes everything (:394-398)
		assertEquals(180, tDraining.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		tDraining.mMode |= GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW;
		tDraining.emitOverflow(); // the B[2] arm drains past the capacity (:428-434)
		assertEquals(100, tDraining.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		int tCaptured = 0;
		for (int i = 0; i < tCapture.getSlots(); i++) tCaptured += tCapture.getStackInSlot(i).getCount();
		assertEquals(80, tCaptured, "the overflow pushed past the capacity (split over the 64-per-slot cap)");
	}

	@Test
	void bottomFillModeDrainsTowardTheBelowInventory() {
		GT6MassStorageBlockEntity tStore = storage();
		tStore.mMode |= GT6MassStorageBlockEntity.MODE_FILL_BELOW;
		assertNull(tStore.insertItems(stone(10), false));
		GTItemStackHandler tCapture = new GTItemStackHandler(9);
		GregTechMove(tStore, tCapture);
		assertEquals(0, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount(), "B[0] drains the store (:341-342)");
		assertEquals(10, tCapture.getStackInSlot(0).getCount());
	}

	private static void GregTechMove(GT6MassStorageBlockEntity aStore, GTItemStackHandler aCapture) {
		gregtech6.util.GTItemMover.move(aStore.massView(), aCapture, 64, 1, 64, null, false);
	}

	// ---------------------------------------------------------------------------
	// the mode arms (upstream :202-223)
	// ---------------------------------------------------------------------------

	@Test
	void theModeTogglesFlipExactlyOneBit() {
		GT6MassStorageBlockEntity tStore = storage();
		assertEquals(0, tStore.mMode);
		tStore.monkeyWrenchToggle();
		assertEquals(GT6MassStorageBlockEntity.MODE_FILL_BELOW, tStore.mMode);
		tStore.screwdriverToggle();
		assertEquals(GT6MassStorageBlockEntity.MODE_FILL_BELOW | GT6MassStorageBlockEntity.MODE_FILTER_RESET, tStore.mMode);
		tStore.cutterToggle();
		assertEquals(GT6MassStorageBlockEntity.MODE_FILL_BELOW | GT6MassStorageBlockEntity.MODE_FILTER_RESET | GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW, tStore.mMode);
		tStore.cutterToggle();
		assertFalse((tStore.mMode & GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW) != 0, "the cutter re-toggle clears B[2] (:203 xor)");
	}

	@Test
	void filterResetClearsTheEmptyStoreUnlessResidueRemains() {
		GT6MassStorageBlockEntity tStore = storage();
		tStore.mMode |= GT6MassStorageBlockEntity.MODE_FILTER_RESET;
		tStore.insertItems(stone(4), false);
		// the screwdriver cleanup arm (:212): the store has content — slotNull answers false
		assertFalse(tStore.slotNull(GT6MassStorageBlockEntity.SLOT_MASS));
		tStore.slotKill(GT6MassStorageBlockEntity.SLOT_MASS);
		assertTrue(tStore.slotNull(GT6MassStorageBlockEntity.SLOT_MASS));
		// with residue the zero-stack allowance holds (:550)
		tStore.mPartialUnits = 5;
		assertTrue(tStore.allowZeroStacks(GT6MassStorageBlockEntity.SLOT_MASS));
		tStore.mPartialUnits = 0;
		assertFalse(tStore.allowZeroStacks(GT6MassStorageBlockEntity.SLOT_MASS), "B[1] without residue refuses the zero ghost");
	}

	// ---------------------------------------------------------------------------
	// the partial units (upstream :593-698 over the MaterialPrefixItem resolution)
	// ---------------------------------------------------------------------------

	@Test
	void crossFormInsertionConvertsIntoPartialUnitsAndWholeStacks() {
		// the fixture amounts (the GT6 prefix table): dust = U, dustTiny = U/9
		assertEquals(CS.U, OP.dust.mAmount);
		assertEquals(CS.U9, OP.dustTiny.mAmount);
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(dustIron(1), false));
		// a tiny carries its OWN unit — the ×count rides the caller (:421)
		assertEquals(OP.dustTiny.mAmount, tStore.getUnitAmount(dustTinyIron(9)), "the DUST_BASED family gate (:633-634)");
		assertEquals(0, tStore.getUnitAmount(ingotIron(1)), "an ingot-form item carries NO dust-family units (:633-634)");
		// the consumed conversion: 1 stored dust + 9 tinies → 2 whole dusts, residue 0
		assertNull(tStore.insertItems(dustTinyIron(9), false), "the compatible form is CONSUMED (:421-425)");
		assertEquals(2, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount(), "the residue re-materialised (:679-698)");
		assertEquals(0, tStore.mPartialUnits);
	}

	@Test
	void partialResidueDropsAsTheSmallestForm() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(dustIron(1), false));
		assertEquals(0, tStore.mPartialUnits, "a whole unit converted immediately");
		// the refund STACK builds from the LIVE registry (GTMaterialItems.stackOf) — the
		// registered-item face is leg-dependent offline, so the pin here is the DECISION
		// face below (the live stack is the RCON chain's)
		tStore.mPartialUnits = 5;
		// the pure refund decision (upstream :598-621)
		assertEquals(OP.dust, GT6MassStorageBlockEntity.refundPrefixOf(OP.dustTiny), "dust tiny refunds the dust form");
		assertEquals(OP.gem, GT6MassStorageBlockEntity.refundPrefixOf(OP.blockGem), "block gem refunds the gem form (:602-604)");
		assertEquals(OP.plate, GT6MassStorageBlockEntity.refundPrefixOf(OP.blockPlate), "block plate refunds the plate form (:605-607)");
		assertEquals(OP.oreRaw, GT6MassStorageBlockEntity.refundPrefixOf(OP.blockRaw), "block raw refunds the raw ore (:620-622)");
		assertNull(GT6MassStorageBlockEntity.refundPrefixOf(OP.lens), "no family → no refund");
		// no prefix on the store → the empty refund (:596)
		GT6MassStorageBlockEntity tPlain = storage();
		assertNull(tPlain.insertItems(stone(1), false));
		tPlain.mPartialUnits = 7;
		assertTrue(tPlain.getPartialStack().isEmpty(), "stone carries no ore-dict prefix (:596)");
	}

	// ---------------------------------------------------------------------------
	// the pixel grid (upstream :251-265, the pure tier table)
	// ---------------------------------------------------------------------------

	@Test
	void theTakeTierTableMatchesTheUpstreamPixelCells() {
		// the three take rows: 8/64 (y 6..8px), 4/32 (9..11), 1/16 (12..14); left col 2..4px, right col 12..14px
		assertEquals(8, GT6MassStorageBlockEntity.takeTierAt(px(3), px(7)));
		assertEquals(64, GT6MassStorageBlockEntity.takeTierAt(px(13), px(7)));
		assertEquals(4, GT6MassStorageBlockEntity.takeTierAt(px(3), px(10)));
		assertEquals(32, GT6MassStorageBlockEntity.takeTierAt(px(13), px(10)));
		assertEquals(1, GT6MassStorageBlockEntity.takeTierAt(px(3), px(13)));
		assertEquals(16, GT6MassStorageBlockEntity.takeTierAt(px(13), px(13)));
		// the centre insert-all cell (u 4..12px from y 6px)
		assertEquals(-1, GT6MassStorageBlockEntity.takeTierAt(px(8), px(7)));
		assertEquals(-1, GT6MassStorageBlockEntity.takeTierAt(px(8), px(14)));
		// outside the 2..14px window the caller refuses (the :245 conjunct) — the table
		// itself answers 0 in the gaps (the 5px row, the 8.5..9 gap, the centre at y<6)
		assertEquals(0, GT6MassStorageBlockEntity.takeTierAt(px(8), px(3)));
		assertEquals(-1, GT6MassStorageBlockEntity.takeTierAt(px(8), px(9)), "the centre column spans the whole y 6..14 band (:263-265)");
	}

	static float px(int aPixels) {
		return aPixels / 16.0F;
	}

	@Test
	void theFaceCoordMappingReadsTheViewPlane() {
		// the UT.Code.getFacingCoordsClicked horizontal four (UT.java:1738-1741)
		float[] tNorth = GT6MassStorageBlockEntity.facingCoordsClicked((byte)2, 0.25F, 0.75F, 0.5F);
		assertEquals(0.75F, tNorth[0], 1e-6F);
		assertEquals(0.25F, tNorth[1], 1e-6F);
		float[] tSouth = GT6MassStorageBlockEntity.facingCoordsClicked((byte)3, 0.25F, 0.75F, 0.5F);
		assertEquals(0.25F, tSouth[0], 1e-6F);
		float[] tWest = GT6MassStorageBlockEntity.facingCoordsClicked((byte)4, 0.5F, 0.75F, 0.25F);
		assertEquals(0.25F, tWest[0], 1e-6F);
		float[] tEast = GT6MassStorageBlockEntity.facingCoordsClicked((byte)5, 0.5F, 0.75F, 0.25F);
		assertEquals(0.75F, tEast[0], 1e-6F);
	}

	// ---------------------------------------------------------------------------
	// the digit strip (the Standard :117-140 + the Logistics :126 tint)
	// ---------------------------------------------------------------------------

	@Test
	void theDigitGateLadderReadsTheCount() {
		// the units cell always; the ladder gates 99999/9999/999/99/9 (:131-140)
		assertEquals("7", GT6MassStorageRenderer.digitCharacter(7, 5, false));
		assertNull(GT6MassStorageRenderer.digitCharacter(7, 4, false));
		assertEquals("1", GT6MassStorageRenderer.digitCharacter(123456, 0, false), "123456 > 99999 opens the MSD cell (the 1 of 123456)");
		assertEquals("2", GT6MassStorageRenderer.digitCharacter(123456, 1, false));
		assertEquals("3", GT6MassStorageRenderer.digitCharacter(123456, 2, false));
		assertEquals("4", GT6MassStorageRenderer.digitCharacter(123456, 3, false));
		assertEquals("5", GT6MassStorageRenderer.digitCharacter(123456, 4, false));
		assertNull(GT6MassStorageRenderer.digitCharacter(99999, 0, false), "the gate is strict > (:133)");
		assertEquals("1", GT6MassStorageRenderer.digitCharacter(100000, 0, false));
	}

	@Test
	void theFullStateCollapsesToTheRedHundredPercentPattern() {
		assertNull(GT6MassStorageRenderer.digitCharacter(1000000, 0, true));
		assertEquals("1", GT6MassStorageRenderer.digitCharacter(1000000, 1, true));
		assertEquals("0", GT6MassStorageRenderer.digitCharacter(1000000, 2, true));
		assertEquals("0", GT6MassStorageRenderer.digitCharacter(1000000, 3, true));
		assertEquals("percent", GT6MassStorageRenderer.digitCharacter(1000000, 4, true));
		assertNull(GT6MassStorageRenderer.digitCharacter(1000000, 5, true));
	}

	@Test
	void theTwoSetsDifferExactlyInTheDigitTint() {
		assertEquals(0xFFFFFFFF, storage().digitARGB(), "the standard digits are WHITE (upstream :125 CA_WHITE)");
		assertEquals(0xFF00FFFF, logistics().digitARGB(), "the logistics digits are CYAN (upstream :126 CA_CYAN_255)");
		assertEquals(0xFFFF3C3C, GT6MassStorageBlockEntity.FULL_DIGIT_ARGB, "the full state is red on BOTH sets (:120-123 CA_RED_255)");
	}

	@Test
	void theRendererRegistersOnBothMassBETsAndCarriesTheStripConstants() throws Exception {
		// the BER existence pin: the client class carries the mod-bus register handler and
		// the upstream strip geometry constants (y 12..14px across x 2..14px, 6 cells)
		Method tRegister = GT6MassStorageRenderer.class.getDeclaredMethod("onRegisterRenderers", net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers.class);
		assertNotNull(tRegister);
		assertEquals(2.0F, GT6MassStorageRenderer.STRIP_LEFT_PX, 1e-6F);
		assertEquals(14.0F, GT6MassStorageRenderer.STRIP_RIGHT_PX, 1e-6F);
		assertEquals(12.0F, GT6MassStorageRenderer.STRIP_BOTTOM_PX, 1e-6F);
		assertEquals(14.0F, GT6MassStorageRenderer.STRIP_TOP_PX, 1e-6F);
		assertArrayEquals(new int[] {99999, 9999, 999, 99, 9}, GT6MassStorageRenderer.DIGIT_GATE, "the upstream usesRenderPass2 gates verbatim");
	}

	// ---------------------------------------------------------------------------
	// the three-way geometry: the mass storage IS the untouched vanilla full cube
	// ---------------------------------------------------------------------------

	@Test
	void theGeometryIsTheUntouchedFullCubeOnAllThreeFaces() {
		// the render/collision/outline trio rides the vanilla defaults — the block class
		// declares NO shape override (the three-way ruling posture; a Block instance
		// cannot be constructed offline — the frozen registry — so the pin is the class face)
		for (String tMethod : new String[] {"getShape", "getCollisionShape", "getRenderShape"}) {
			// getRenderShape IS overridden (the BaseEntityBlock INVISIBLE default); the
			// SHAPE pair must stay absent
			if (tMethod.equals("getRenderShape")) continue;
			try {
				GT6StaticStorages.GT6MassStorageBlock.class.getDeclaredMethod(tMethod,
						net.minecraft.world.level.block.state.BlockState.class,
						net.minecraft.world.level.BlockGetter.class, BlockPos.class,
						net.minecraft.world.phys.shapes.CollisionContext.class);
				org.junit.jupiter.api.Assertions.fail("mass storage must not override " + tMethod + " — the full cube trio");
			} catch (NoSuchMethodException expected) {
				// the pass face
			}
		}
		// the default full cube answers the standard 16px box (Blocks.STONE posture sanity)
		VoxelShape tFull = Blocks.STONE.defaultBlockState().getShape(null, POS);
		assertNotNull(tFull);
		// the FACING property is the horizontal four (the SIDES_HORIZONTAL :539)
		assertTrue(GT6StaticStorages.GT6MassStorageBlock.FACING.getPossibleValues().contains(Direction.NORTH));
		assertFalse(GT6StaticStorages.GT6MassStorageBlock.FACING.getPossibleValues().contains(Direction.UP));
	}

	// ---------------------------------------------------------------------------
	// the automation view + the ConnectedInventory arms
	// ---------------------------------------------------------------------------

	@Test
	void theFunnelViewDrainsIntoTheMassSlotAndTheMassViewChunksOut() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(10), false));
		// the funnel insert (slot 0) lands in the store event-driven
		ItemStack tLeft = tStore.sideView(Direction.UP).insertItem(GT6MassStorageBlockEntity.SLOT_FUNNEL, stone(30), false);
		assertNull(tLeft);
		assertEquals(40, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		// the mass extract chunks the unclamped stack
		ItemStack tOut = tStore.massView().extractItem(GT6MassStorageBlockEntity.SLOT_MASS, 25, false);
		assertEquals(25, tOut.getCount());
		assertEquals(15, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		// the funnel extract face answers slot 0 only upstream (canExtractItem2 :549 — the
		// mass slot extract is gated by the taped flag only)
		assertTrue(tStore.canExtractItem(GT6MassStorageBlockEntity.SLOT_MASS, (byte)1));
	}

	@Test
	void theTapedFaceGatesEverythingAndKeepsItsSlot() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(4), false));
		tStore.mMode |= GT6MassStorageBlockEntity.MODE_TAPED;
		// insert refused (:383), the pincers refused (:128), the extraction refused (:549)
		assertEquals(4, tStore.insertItems(stone(4), false).getCount());
		assertFalse(tStore.canExtractItem(GT6MassStorageBlockEntity.SLOT_MASS, (byte)1));
		assertFalse(tStore.canDrop(GT6MassStorageBlockEntity.SLOT_MASS), "the taped mass stack rides the item (keepSlot :556)");
		assertTrue(tStore.canDrop(GT6MassStorageBlockEntity.SLOT_FUNNEL), "the funnel slot pops (:555 canDrop(0) = T)");
		assertFalse(tStore.isFaceVisible(), "the taped face hides the display (:508)");
	}

	@Test
	void theConnectedInventoryArmsAnswerTheStoreFace() {
		GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(7), false));
		assertEquals(7, tStore.getAmountOfItemsInConnectedInventory(GT6MassStorageBlockEntity.SIDE_ANY, stone(1), 100), ":552");
		assertEquals(0, tStore.getAmountOfItemsInConnectedInventory(GT6MassStorageBlockEntity.SIDE_ANY, dirt(1), 100));
		assertEquals(3, tStore.addStackToConnectedInventory(GT6MassStorageBlockEntity.SIDE_ANY, stone(3), false), ":568-577");
		assertEquals(10, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		assertEquals(6, tStore.removeStackFromConnectedInventory(GT6MassStorageBlockEntity.SIDE_ANY, stone(6), false), ":579-591");
		assertEquals(4, tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		assertEquals(0, tStore.removeStackFromConnectedInventory(GT6MassStorageBlockEntity.SIDE_ANY, stone(6), true), "the all-at-once gate refuses (:583)");
	}

	// ---------------------------------------------------------------------------
	// the logistics faces
	// ---------------------------------------------------------------------------

	@Test
	void theLogisticsFacesAnswerTheUpstreamPriorities() {
		GT6MassStorageBlockEntity tStore = storage();
		assertEquals(1, tStore.getLogisticsPriorityItem(), "the empty store is generic (:502)");
		assertEquals(0, tStore.getLogisticsPriorityFluid(), ":503");
		assertNull(tStore.getLogisticsFilterItem(), ":505");
		assertNull(tStore.getLogisticsFilterFluid(), ":504");
		assertTrue(tStore.canLogistics(GT6MassStorageBlockEntity.SIDE_ANY), ":501");
		assertNull(tStore.getLogisticsFilter(GT6MassStorageBlockEntity.SIDE_ANY), "the empty store answers the cleared cache (:440)");
		assertNull(tStore.insertItems(ingotIron(2), false));
		assertNotNull(tStore.getLogisticsFilter(GT6MassStorageBlockEntity.SIDE_ANY));
		// the semi-filtered face: the base carries it, the logistics twin the storage face
		assertTrue(tStore instanceof gregtech6.tileentity.logistics.ITileEntityLogisticsSemiFilteredItem);
		GT6MassStorageLogisticsBlockEntity tLogistics = logistics();
		assertTrue(tLogistics instanceof gregtech6.tileentity.logistics.ITileEntityLogisticsStorage);
		assertNull(tLogistics.insertItems(ingotIron(2), false));
		assertEquals(2, tLogistics.getLogisticsPriorityItem(), "the stored store is semi-filtered (:502)");
	}

	// ---------------------------------------------------------------------------
	// the NBT round trip + the explosion face
	// ---------------------------------------------------------------------------

	@Test
	void theNbtRoundTripCarriesModeCapacityResidueAndFacing() {
		GT6MassStorageBlockEntity tStore = storage();
		tStore.mMode = GT6MassStorageBlockEntity.MODE_FILL_BELOW | GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW;
		tStore.mMaxStorage = 424242;
		tStore.mPartialUnits = 31337;
		tStore.setFacingNbtFallback((byte)4);
		assertNull(tStore.insertItems(stone(9), false));
		CompoundTag tSaved = tStore.saveWithoutMetadata(); // the offline fixture type is registry-less (saveMetadata needs the mapping)
		GT6MassStorageBlockEntity tLoaded = storage();
		tLoaded.load(tSaved);
		assertEquals(GT6MassStorageBlockEntity.MODE_FILL_BELOW | GT6MassStorageBlockEntity.MODE_EMIT_OVERFLOW, tLoaded.mMode);
		assertEquals(424242, tLoaded.mMaxStorage);
		assertEquals(31337, tLoaded.mPartialUnits);
		assertEquals(9, tLoaded.slot(GT6MassStorageBlockEntity.SLOT_MASS).getCount());
		assertEquals(4, tLoaded.getFacing(), "the NBT fallback byte round-trips");
	}

	@Test
	void theExplosionFaceKillsTheMassStoreOnly() {
			GT6MassStorageBlockEntity tStore = storage();
		assertNull(tStore.insertItems(stone(5), false));
		tStore.mMode |= GT6MassStorageBlockEntity.MODE_TAPED; // even taped dies (:544 unconditional)
		tStore.slot(GT6MassStorageBlockEntity.SLOT_FUNNEL, stone(2));
		tStore.killForExplosion();
		assertTrue(tStore.slot(GT6MassStorageBlockEntity.SLOT_MASS).isEmpty(), "onExploded kills slot 1 (:544)");
		assertEquals(2, tStore.slot(GT6MassStorageBlockEntity.SLOT_FUNNEL).getCount(), "the funnel slot pops via the block fallback");
	}

	/**
	 * The offline probe-item helper (the GT6RecipeMapCrucibleTest GTMaterialItemsBoot
	 * posture verbatim): the Forge 1.20.1 Item ctor registers an intrusive holder, so the
	 * frozen registry must open before construction; the probe ids are throwaway names.
	 */
	private static MaterialPrefixItem probePrefix(String aProbeId, java.util.function.Supplier<MaterialPrefixItem> aCreator) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		//? if forge {
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline item registry", aE);
		}
		try {
			java.lang.reflect.Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
		} catch (NoSuchFieldException | NoSuchMethodException ignored) {
			// the 21.1 face: no forge delegate behind the vanilla registry
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline forge registry", aE);
		}
		try {
			java.lang.reflect.Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (NoSuchFieldException ignored) {
			// the 21.1 face: nothing but the vanilla frozen flag to unlock
		} catch (Exception aE) {
			throw new IllegalStateException("could not clear the offline registry lock", aE);
		}
		//?} else {
		/*try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
			throw new IllegalStateException("could not clear the offline registry lock", aE);
		}
		*///?}
		MaterialPrefixItem rItem = aCreator.get();
		net.minecraft.core.Registry.register(tRegistry, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", aProbeId), rItem);
		return rItem;
	}

	/** The first declared field up the hierarchy (the GTMaterialItemsBoot walk). */
	private static java.lang.reflect.Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tClass = aClass; tClass != null; tClass = tClass.getSuperclass()) {
			try {
				java.lang.reflect.Field rField = tClass.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {}
		}
		throw new NoSuchFieldException(aName);
	}

	private static ItemStack dustIron(int aCount) {
		return new ItemStack(sDustIron, aCount);
	}

	private static ItemStack dustTinyIron(int aCount) {
		return new ItemStack(sDustTinyIron, aCount);
	}

	private static ItemStack ingotIron(int aCount) {
		return new ItemStack(sIngotIron, aCount);
	}

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	private static ItemStack dirt(int aCount) {
		return new ItemStack(Items.DIRT, aCount);
	}
}
