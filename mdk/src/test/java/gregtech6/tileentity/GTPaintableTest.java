package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTModelProperties;
import gregtech6.covers.CoverRegistry;
import gregtech6.covers.covers.CoverTextureSimple;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The paintable storage/NBT/sync/ModelData acceptance (task paintable-storage-sync):
 * the {@link IPaintableTE} numeric contract (upstream TileEntityBase07Paintable.java:83-86
 * paint/unpaint/getPaint/isPainted + the 04:227-235 mixPaint routing through the UT.java
 * :1576-1578 channel average, pinned to exact hex values), the gt.color/gt.painted NBT
 * round trip (CS.java:1161-1162 key names verbatim), and the PAINT ModelData supply with
 * the three-key oven coexistence (TileEntityOvenCoverTest shape).
 *
 * <p>All writes go through the public {@link IPaintableTE} API — the ADR ruling 1 face the
 * command arm drives; no fixture writes fields directly. The oven fixture reuses the
 * covers-test BET-over-vanilla-BRICKS shape (the registries freeze after boot, the
 * GTCoverTestBase precedent).
 */
public class GTPaintableTest extends GTOfflineTestBase {

	/** The CS.DYES_INT values the mix assertions pin (GTMachineCommand.DYES_INT mirrors it). */
	static final int DYE_RED = 0xFF0000; // CS.DYE_Red {255,0,0}
	static final int DYE_ORANGE = 0xFF8000; // CS.DYE_Orange {255,128,0}
	static final int DYE_BLACK = 0x202020; // CS.DYE_Black {32,32,32}

	static BlockEntityType<TileEntityOven> sPaintOvenType;

	static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void buildOvenFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sPaintOvenType = tHolder[0];
	}

	static TileEntityOven oven() {
		return new TileEntityOven(sPaintOvenType, POS, Blocks.BRICKS.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the IPaintableTE numeric contract (channel-average mix pinned)
	// ---------------------------------------------------------------------------

	@Test
	void unpaintedDefaultsAndPaintStoresDirectly() {
		TileEntityOven tOven = oven();
		assertFalse(tOven.isPainted(), "upstream :49 mIsPainted = F");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tOven.getPaint(), "upstream :50 mRGBa = UNCOLORED white");

		assertTrue(tOven.paint(DYE_RED), "upstream :85 — a colour change reports true");
		assertTrue(tOven.isPainted());
		assertEquals(DYE_RED, tOven.getPaint(), "direct 0xRRGGBB storage (ADR ruling 3)");

		assertFalse(tOven.paint(DYE_RED), "upstream :85 — the same-colour spray is the no-op");
		assertEquals(DYE_RED, tOven.getPaint());
	}

	@Test
	void mixPaintOnUnpaintedStoresTheColourUnmixed() {
		TileEntityOven tOven = oven();
		assertTrue(tOven.mixPaint(DYE_ORANGE), "the 04:229 unpainted half stores the colour directly");
		assertEquals(DYE_ORANGE, tOven.getPaint());
		assertTrue(tOven.isPainted());
	}

	@Test
	void mixPaintAveragesPerChannel() {
		// the UT.java:1576-1578 three-liner: ((ch1+ch2)>>1 per channel), pinned to exact hex
		TileEntityOven tOven = oven();
		tOven.mixPaint(DYE_RED); // 255,0,0

		assertTrue(tOven.mixPaint(DYE_ORANGE)); // 255,128,0
		assertEquals(0xFF4000, tOven.getPaint(), "mix(Red 255,0,0, Orange 255,128,0) = 255,64,0");

		assertTrue(tOven.mixPaint(DYE_BLACK)); // 32,32,32
		assertEquals(0x8F3010, tOven.getPaint(), "mix(255,64,0, Black 32,32,32) = 143,48,16");

		assertTrue(tOven.mixPaint(DYE_RED)); // 255,0,0
		assertEquals(0xC71808, tOven.getPaint(), "mix(143,48,16, Red 255,0,0) = 199,24,8");
	}

	@Test
	void mixPaintMasksTheAlphaByte() {
		// the 04:229 & ALL_NON_ALPHA_COLOR tail — an ARGB input stores its 0xRRGGBB half
		TileEntityOven tOven = oven();
		assertTrue(tOven.mixPaint(0x6155A030));
		assertEquals(0x55A030, tOven.getPaint());
	}

	@Test
	void unpaintRestoresTheMaterialDefaultAndSecondUnpaintIsNoOp() {
		TileEntityOven tOven = oven();
		tOven.mixPaint(DYE_RED);

		assertTrue(tOven.unpaint(), "upstream :83 — a painted machine unpaints true");
		assertFalse(tOven.isPainted());
		// task machine-material-tint-fidelity REVERTED the "no material reference"
		// deviation: unpaint now restores the row material's fRGBaSolid through the block
		// carrier (GTBasicMachineBlock.materialOf/materialColor) — the upstream Paintable:83
		// mRGBa = mMaterial.fRGBaSolid form. THIS fixture rides a vanilla BRICKS state, the
		// material-LESS arm: it restores UNCOLORED white (the materialColor(null) identity),
		// numerically the P21 contract; the row-material arm is pinned in
		// GTMachinePaintTintTest.unpaintedMachineTintsWithTheRowMaterial (0xFFFF825A Cu /
		// 0xFF828282 Steel) and the live rows ride the runServer registration gate.
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tOven.getPaint(), "the material-less arm restores UNCOLORED white");

		assertFalse(tOven.unpaint(), "upstream :83 — the unpainted machine is the no-op");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tOven.getPaint());
	}

	// ---------------------------------------------------------------------------
	// the NBT round trip (gt.color / gt.painted verbatim, ADR ruling 4)
	// ---------------------------------------------------------------------------

	@Test
	void paintRidesTheFullBESaveLoad() {
		TileEntityOven tOven = oven();
		tOven.mixPaint(DYE_ORANGE); // end 0xFF8000 through the mix path

		CompoundTag tSaved = tOven.saveWithoutMetadata();
		assertTrue(tSaved.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_INT), "gt.color as an Integer (upstream CS.java:1161)");
		assertTrue(tSaved.contains(TileEntityBase03TicksAndSync.NBT_PAINTED, Tag.TAG_BYTE), "gt.painted as a Boolean (upstream CS.java:1162)");
		assertEquals(0xFF8000, tSaved.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));
		assertTrue(tSaved.getBoolean(TileEntityBase03TicksAndSync.NBT_PAINTED));

		TileEntityOven tReloaded = oven();
		tReloaded.load(tSaved);
		assertTrue(tReloaded.isPainted(), "the load path rehydrates the painted flag");
		assertEquals(0xFF8000, tReloaded.getPaint(), "the load path rehydrates the colour");
	}

	@Test
	void unpaintedBECarriesNoPaintKeys() {
		TileEntityOven tOven = oven();
		CompoundTag tSaved = tOven.saveWithoutMetadata();
		assertFalse(tSaved.contains(TileEntityBase03TicksAndSync.NBT_COLOR), "unpainted → no gt.color written");
		assertFalse(tSaved.contains(TileEntityBase03TicksAndSync.NBT_PAINTED), "unpainted → no gt.painted written");

		// paint → unpaint round trip also unwrites (the fresh save carries no keys again)
		tOven.mixPaint(DYE_RED);
		tOven.unpaint();
		CompoundTag tAfter = tOven.saveWithoutMetadata();
		assertFalse(tAfter.contains(TileEntityBase03TicksAndSync.NBT_COLOR));
		assertFalse(tAfter.contains(TileEntityBase03TicksAndSync.NBT_PAINTED));

		// an empty tag load is the hasKey-guarded no-op on an unpainted BE
		tOven.load(new CompoundTag());
		assertFalse(tOven.isPainted());
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tOven.getPaint());
	}

	/**
	 * Symptom34 nail (task spray-paint-domain-two-way): the remover clears the SERVER BE,
	 * and the next update tag of the unpainted BE carries NO paint keys
	 * ({@code saveAdditional} writes them only while painted). Both sync channels are the
	 * FULL {@code saveWithoutMetadata()} tag, never a delta, so the key-less tag IS the
	 * unpainted state — the former contains-guard kept the client BE painted=true forever
	 * (Jade showed the stale colour, the PAINT ModelData kept the world tinted; a relog
	 * was the only cure). {@code gt.painted} therefore reads UNCONDITIONALLY.
	 */
	@Test
	void unpaintSyncTagClearsTheStaleClientPaint() {
		TileEntityOven tServer = oven();
		tServer.mixPaint(DYE_RED);

		// the client BE holds the painted state (the push landed earlier)
		TileEntityOven tClient = oven();
		tClient.load(tServer.saveWithoutMetadata());
		assertTrue(tClient.isPainted());
		assertEquals(DYE_RED, tClient.getPaint());
		assertTrue(tClient.getModelData().has(GTModelProperties.PAINT), "the stale snapshot still carries PAINT");

		// the server unpaints — the next update tag is key-less
		assertTrue(tServer.unpaint());
		CompoundTag tUnpaintedTag = tServer.saveWithoutMetadata();
		assertFalse(tUnpaintedTag.contains(TileEntityBase03TicksAndSync.NBT_PAINTED));
		assertFalse(tUnpaintedTag.contains(TileEntityBase03TicksAndSync.NBT_COLOR));

		tClient.load(tUnpaintedTag);
		assertFalse(tClient.isPainted(), "the key-less unpaint tag clears the stale client flag");
		assertEquals(TileEntityBase03TicksAndSync.UNCOLORED, tClient.getPaint());
		assertFalse(tClient.getModelData().has(GTModelProperties.PAINT),
				"PAINT leaves the snapshot — the tint arm falls back to the block's material carrier "
				+ "(GTMachinePaintTintTest.unpaintedMachineTintsWithTheRowMaterial pins that half)");
	}

	/**
	 * Symptom33's data half (task spray-paint-domain-two-way): the PAINT snapshot the
	 * chunk-rebuild task reads flows from the SYNC tag through {@link #load} — pin the
	 * sync-face rehydration (the tint consumption itself is pinned in
	 * GTMachinePaintTintTest/GTMachineTintModelTest; the rebuild trigger is the live
	 * RCON/dev-client leg — requestModelDataUpdate alone never schedules one, the pair
	 * in TileEntityBase03TicksAndSync.load does).
	 */
	@Test
	void paintSyncTagRetintsTheModelDataThroughTheLoadFace() {
		TileEntityOven tServer = oven();
		tServer.mixPaint(DYE_ORANGE);
		CompoundTag tPaintedTag = tServer.saveWithoutMetadata(); // the update-tag form

		TileEntityOven tClient = oven();
		tClient.load(tPaintedTag);
		assertTrue(tClient.isPainted(), "the sync tag rehydrates the painted flag");
		assertEquals(DYE_ORANGE, tClient.getPaint());
		assertTrue(tClient.getModelData().has(GTModelProperties.PAINT));
		assertEquals(DYE_ORANGE, tClient.getModelData().get(GTModelProperties.PAINT));
	}

	// ---------------------------------------------------------------------------
	// the PAINT ModelData supply + the oven three-key coexistence
	// ---------------------------------------------------------------------------

	@Test
	void unpaintedModelDataCarriesNoPaintProperty() {
		TileEntityOven tOven = oven();
		assertFalse(tOven.getModelData().has(GTModelProperties.PAINT), "unpainted → absent property = no tint (card_B falls back to white)");
	}

	@Test
	void paintedModelDataCarriesThePaintColour() {
		TileEntityOven tOven = oven();
		tOven.mixPaint(DYE_RED);
		assertTrue(tOven.getModelData().has(GTModelProperties.PAINT));
		assertEquals(DYE_RED, tOven.getModelData().get(GTModelProperties.PAINT));

		tOven.unpaint();
		assertFalse(tOven.getModelData().has(GTModelProperties.PAINT), "unpaint drops the property again");
	}

	@Test
	void ovenCarriesPaintOvenAndCoverKeysTogether() {
		// the TileEntityOvenCoverTest shape: the cover chain rides RENDER_SNAPSHOT, the oven
		// visuals OVEN_SNAPSHOT, the paint PAINT — three single-valued keys on one snapshot
		CoverRegistry.reset();
		CoverRegistry.put(Items.IRON_INGOT, new CoverTextureSimple(new net.minecraft.resources.ResourceLocation("gt6", "block/cover/test_plate")));

		TileEntityOven tOven = oven();
		assertFalse(tOven.getModelData().has(GTModelProperties.PAINT), "unpainted covered oven → no PAINT");

		assertTrue(tOven.setCoverItem((byte) 1, new net.minecraft.world.item.ItemStack(Items.IRON_INGOT), null, false, true));
		assertTrue(tOven.mixPaint(DYE_RED));

		net.minecraftforge.client.model.data.ModelData tData = tOven.getModelData();
		assertTrue(tData.has(GTModelProperties.PAINT), "the paint key");
		assertTrue(tData.has(GTModelProperties.OVEN_SNAPSHOT), "the oven visuals key");
		assertTrue(tData.has(GTModelProperties.RENDER_SNAPSHOT), "the cover key");
		assertEquals(DYE_RED, tData.get(GTModelProperties.PAINT), "all three keys on one derived snapshot");

		assertTrue(tOven.unpaint());
		assertFalse(tOven.getModelData().has(GTModelProperties.PAINT));
		assertTrue(tOven.getModelData().has(GTModelProperties.OVEN_SNAPSHOT), "the other two keys survive the unpaint derive");
		assertTrue(tOven.getModelData().has(GTModelProperties.RENDER_SNAPSHOT));

		CoverRegistry.reset();
	}

	@AfterEach
	void resetCoverFixtures() {
		CoverRegistry.reset();
	}
}
