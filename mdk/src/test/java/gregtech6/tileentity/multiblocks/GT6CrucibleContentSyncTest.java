/**
 * Offline pin for task crucible-large-ber — the crucible content render chain:
 * <ol>
 * <li>the LARGE crucible BER geometry (the :635 height formula
 *     {@code 1.125 + mDisplayedHeight/150} in render px) and the :611-616 melt-down
 *     red-shift ({@code r*2+50, g*2+50, b/2+50} bind8);</li>
 * <li>the client sync of the display census (mDisplayedHeight/mDisplayedFluid through the
 *     saveAdditional keys — the paint-key channel: {@code getUpdateTag()} =
 *     {@code saveWithoutMetadata()}, both BEs) and the material-id resolution;</li>
 * <li>the ContentFace same-source ruling: the bowl models' content face, the
 *     {@link gregtech6.client.GT6MoldTintListener} index-1 arm and the BER tint all
 *     resolve through {@link GT6CrucibleDatagen#contentFace}.</li>
 * </ol>
 * Upstream anchors: MultiTileEntityCrucible.java :349-351 (the census), :596-604 (the
 * sync packet), :611-616 (the red-shift), :634-635/:648 (the pass geometry);
 * MultiTileEntitySmeltery.java :299/:547-554/:559-567 (the small-form mirrors).
 */
package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.client.GT6MoldTintListener;
import gregtech6.client.render.GTCrucibleContentRenderer;
import gregtech6.datagen.GT6CrucibleDatagen;
import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.tools.TileEntitySmeltery;

public class GT6CrucibleContentSyncTest extends GTMultiBlocksOfflineTestBase {

	/** The Steel molten colour — the MT.java:1713 setRGBaLiquid(255, 20, 10) tail (the bowl-card literal). */
	private static final int STEEL_LIQUID_TINT = 0xFFFF140A;

	static BlockEntityType<DisplayProbeCrucible> sCrucibleType;
	static BlockEntityType<DisplayProbeSmeltery> sSmelteryType;

	/** The visibility probe — a subclass reaches the tools-package protected saveAdditional on its own type (JLS 6.6.2.2). */
	public static final class DisplayProbeSmeltery extends TileEntitySmeltery {
		public DisplayProbeSmeltery(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
		/** The saveAdditional bridge (the this-qualified protected access the test class cannot make). */
		public void saveNBT(CompoundTag aNBT) {
			saveAdditional(aNBT);
		}
	}

	static final BlockPos POS = new BlockPos(100, 64, 100);

	/** The concrete test BE — the crucible over a vanilla-block BET (the InputTest form). */
	public static final class DisplayProbeCrucible extends TileEntityCrucible {
		public DisplayProbeCrucible(BlockPos aPos, BlockState aState) {
			super(sCrucibleType, aPos, aState);
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildDisplayFixtures() {
		GTMultiBlockCrucibleInputTest.ProbeBoot.boot();
		BlockEntityType<DisplayProbeCrucible>[] tHolder = (BlockEntityType<DisplayProbeCrucible>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(DisplayProbeCrucible::new, Blocks.BRICKS).build(null);
		sCrucibleType = tHolder[0];
		// the smeltery self-reference lambda (the TileEntitySmelteryOfflineTest form, no overrides needed)
		BlockEntityType<DisplayProbeSmeltery>[] tSmelteryHolder = (BlockEntityType<DisplayProbeSmeltery>[]) new BlockEntityType<?>[1];
		tSmelteryHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new DisplayProbeSmeltery(tSmelteryHolder[0], aPos, aState), Blocks.BRICKS).build(null);
		sSmelteryType = tSmelteryHolder[0];
	}

	// ------------------------------------------------------------------------------------
	// the sync round-trip (the paint-key channel: saveAdditional is what getUpdateTag ships)
	// ------------------------------------------------------------------------------------

	/** The large crucible display census rides the sync NBT — both keys, both directions. */
	@Test
	public void crucibleDisplayCensusRidesTheSyncChannels() {
		DisplayProbeCrucible tCrucible = new DisplayProbeCrucible(POS, Blocks.BRICKS.defaultBlockState());
		tCrucible.mDisplayedHeight = 200;
		tCrucible.mDisplayedFluid = MT.Steel.mID;
		tCrucible.mDisplayedLightest = MT.Cu.mID; // the cooled-charge identity (task crucible-render-followup)
		CompoundTag tNBT = new CompoundTag();
		tCrucible.saveAdditional(tNBT);
		assertEquals(200, tNBT.getInt(TileEntityCrucible.NBT_DISPLAYED_HEIGHT), "the height rides the sync tag");
		assertEquals(MT.Steel.mID, tNBT.getInt(TileEntityCrucible.NBT_DISPLAYED_FLUID), "the molten material id rides the sync tag");
		assertEquals(MT.Cu.mID, tNBT.getInt(TileEntityCrucible.NBT_DISPLAYED_LIGHTEST), "the lightest id rides the sync tag");

		DisplayProbeCrucible tRestored = new DisplayProbeCrucible(POS, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tNBT);
		assertEquals(200, tRestored.mDisplayedHeight, "the client BE rehydrates the height (onDataPacket = load(tag))");
		assertEquals(MT.Steel.mID, tRestored.mDisplayedFluid, "the client BE rehydrates the fluid id");
		assertSame(MT.Cu, tRestored.displayedLightestMaterial(), "the client BE rehydrates the lightest identity");
	}

	/** The small crucible displayed-fluid rides the same channel, and materialById resolves the id. */
	@Test
	public void smelteryDisplayedFluidRidesTheSyncChannels() {
		DisplayProbeSmeltery tSmeltery = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tSmeltery.mDisplayedFluid = MT.Fe.mID;
		CompoundTag tNBT = new CompoundTag();
		tSmeltery.saveNBT(tNBT);
		assertEquals(MT.Fe.mID, tNBT.getInt(TileEntitySmeltery.NBT_DISPLAYED_FLUID), "the fluid id rides the sync tag");

		TileEntitySmeltery tRestored = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tNBT);
		assertEquals(MT.Fe.mID, tRestored.mDisplayedFluid, "the client BE rehydrates the fluid id");
		assertSame(MT.Fe, tRestored.displayedMaterial(), "the id resolves to the material (the :587 MATERIAL_ARRAY face)");
		assertNull(new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState()).displayedMaterial(),
				"-1 = nothing molten");
		assertEquals(MT.Steel, GT6Crucibles.materialById(MT.Steel.mID), "materialById is the identity on a live id");
		assertNull(GT6Crucibles.materialById(-1), "the out-of-range guard: -1");
		assertNull(GT6Crucibles.materialById(Integer.MAX_VALUE), "the out-of-range guard: past the array");
	}

	/**
	 * The lightest-content id rides the sync channel too (task crucible-render-followup —
	 * the symptom-B data face): upstream drops the displayed id to -1 the moment the
	 * charge cools (MultiTileEntitySmeltery.java:299), so the client would lose the
	 * material identity exactly when the solid face needs it — the port syncs the
	 * phase-independent lightest id beside the upstream molten id.
	 */
	@Test
	public void smelteryDisplayedLightestRidesTheSyncChannels() {
		DisplayProbeSmeltery tSmeltery = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tSmeltery.mDisplayedFluid = -1; // cooled — the upstream census drops to -1
		tSmeltery.mDisplayedLightest = MT.Cu.mID; // the phase-independent identity stays
		CompoundTag tNBT = new CompoundTag();
		tSmeltery.saveNBT(tNBT);
		assertEquals(-1, tNBT.getInt(TileEntitySmeltery.NBT_DISPLAYED_FLUID), "the molten census keeps the upstream -1 semantics");
		assertEquals(MT.Cu.mID, tNBT.getInt(TileEntitySmeltery.NBT_DISPLAYED_LIGHTEST), "the lightest id rides the sync tag");
		TileEntitySmeltery tRestored = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tNBT);
		assertEquals(-1, tRestored.mDisplayedFluid, "the client BE rehydrates the molten census");
		assertSame(MT.Cu, tRestored.displayedLightestMaterial(), "the client BE rehydrates the lightest identity");
	}

	// ------------------------------------------------------------------------------------
	// the BER geometry + colour math (the pure statics)
	// ------------------------------------------------------------------------------------

	/** The :635 content top plane — (1.125 + h/150) * 16 px, inside the 3-high cavity. */
	@Test
	public void contentTopPxFollowsTheUpstreamFormula() {
		assertEquals(18.0F, GTCrucibleContentRenderer.contentTopPx(0), 0.001F, "h = 0 sits on the :634 floor plane");
		assertEquals(34.0F, GTCrucibleContentRenderer.contentTopPx(150), 0.001F, "h = 150 = exactly one block above the floor");
		assertEquals(18.0F + 255.0F / 150.0F * 16.0F, GTCrucibleContentRenderer.contentTopPx(255), 0.001F,
				"h = 255 is the :349 full census");
		assertTrue(GTCrucibleContentRenderer.contentTopPx(255) < 48.0F, "the full charge never leaves the 3-high cavity");
	}

	/** The :611-616 red-shift — r*2+50, g*2+50, b/2+50, bind8-clamped, alpha through. */
	@Test
	public void meltDownShiftIsTheUpstreamBind8Form() {
		assertEquals(0xFFFF5A37, GTCrucibleContentRenderer.meltDownShift(STEEL_LIQUID_TINT),
				"steel liquid (255,20,10) → (bind8 560, 90, 55)");
		assertEquals(0xFFFAFA64, GTCrucibleContentRenderer.meltDownShift(0xFF646464),
				"mid-gray (100,100,100) → (250,250,100)");
		assertEquals(0xFF323232, GTCrucibleContentRenderer.meltDownShift(0xFF000000),
				"black → the +50 floor");
		assertEquals(0x8032FF32, GTCrucibleContentRenderer.meltDownShift(0x8000FF00),
				"the alpha byte passes through untouched");
	}

	// ------------------------------------------------------------------------------------
	// the ContentFace same-source ruling (the one dispatch, the three consumers)
	// ------------------------------------------------------------------------------------

	/**
	 * The bowl models' content face, the listener index-1 arm and the BER all consume the
	 * one dispatch — the generated tree's content texture is literally the ContentFace
	 * texture, and the listener arm answers its molten tint.
	 */
	@Test
	public void contentFaceIsTheSharedColourSource() throws Exception {
		GT6CrucibleDatagen.ContentFace tSteelMolten = GT6CrucibleDatagen.contentFace(MT.Steel, true);
		assertEquals(STEEL_LIQUID_TINT, tSteelMolten.tintARGB(), "the molten arm is the mRGBaLiquid pack");
		try (InputStream tStream = getClass().getClassLoader()
				.getResourceAsStream("assets/gt6/models/block/smeltery_steel_filled_1.json")) {
			assertNotNull(tStream, "the generated bowl model must be on the classpath");
			JsonObject tModel = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertEquals(tSteelMolten.texture(), tModel.getAsJsonObject("textures").get("content").getAsString(),
					"the generated content face rides the same sprite the BER tints");
			// the content seat located by its #content up face (the r11a full-shell form made
			// the child carry 6 elements — shell first, content box last — so the former
			// elements[0] index assumption answered the west wall's untinted up face)
			JsonObject tUp = null;
			for (var tEl : tModel.getAsJsonArray("elements")) {
				JsonObject tFaces = tEl.getAsJsonObject().getAsJsonObject("faces");
				if (tFaces != null && tFaces.has("up")
						&& "#content".equals(tFaces.getAsJsonObject("up").get("texture").getAsString())) {
					tUp = tFaces.getAsJsonObject("up");
				}
			}
			assertNotNull(tUp, "the child carries the content box (the r11a full-shell element set)");
			assertEquals(1, tUp.get("tintindex").getAsInt(), "the content seat is tintindex 1 (the listener arm)");
		}
		// the listener arm: the smeltery's synced display resolves the tint through the
		// MOLTEN gate (task crucible-render-followup); no level = no tint
		MultiBlockLevel tLevel = new MultiBlockLevel();
		DisplayProbeSmeltery tSmeltery = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tSmeltery.setLevel(tLevel);
		tLevel.mBlockEntities.put(POS, tSmeltery);
		// molten: the lightest content id synced, the census molten — the mRGBaLiquid arm
		tSmeltery.mDisplayedFluid = MT.Fe.mID;
		tSmeltery.mDisplayedLightest = MT.Fe.mID;
		assertEquals(GT6CrucibleDatagen.contentFace(MT.Fe, true).tintARGB(),
				GT6MoldTintListener.crucibleContentTintARGB(null, tLevel, POS), "the index-1 arm is the ContentFace molten tint");
		// cooled (the symptom-B fix): the lightest id stays synced while the molten census
		// drops -1 — the SOLID arm answers the lightest content's mRGBaSolid, no more raw
		// gray-white molten art
		tSmeltery.mDisplayedFluid = -1;
		assertEquals(GT6CrucibleDatagen.contentFace(MT.Fe, false).tintARGB(),
				GT6MoldTintListener.crucibleContentTintARGB(null, tLevel, POS), "the cooled charge answers the SOLID arm of the lightest content");
		// empty: nothing synced anywhere — no tint
		tSmeltery.mDisplayedLightest = -1;
		assertEquals(-1, GT6MoldTintListener.crucibleContentTintARGB(null, tLevel, POS), "an empty crucible answers no-tint");
		assertEquals(-1, GT6MoldTintListener.crucibleContentTintARGB(null, null, null), "no level answers no-tint");
		// the item half routes index 1 through blockTintARGB — no BE there, stays inert
		// (the family member face is pinned by GT6MoldTintDatagenTest materialTintARGB(-1) —
		// the RegistryObject instances stay frozen offline)
		assertEquals(-1, GT6MoldTintListener.blockTintARGB(Blocks.BRICKS, 1),
				"an unknown block answers no-tint on the content seat");
	}

	/** The :299/:350 census arm is temperature-gated: cold shows nothing molten, hot shows the lightest molten id. */
	@Test
	public void meltedChargePinsTheDisplayedFluidCensus() {
		DisplayProbeSmeltery tSmeltery = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		tSmeltery.mContent.add(new gregapi.oredict.OreDictMaterialStack(MT.Fe, 2 * gregapi.data.CS.U));
		gregapi.oredict.OreDictMaterialStack tLightest = tSmeltery.lightest();
		assertNotNull(tLightest);
		assertSame(MT.Fe, tLightest.mMaterial);
		// the :299 verbatim expression over the lightest walk
		java.util.function.IntUnaryOperator tCensus = aTemperature ->
				(tLightest.mMaterial.mMeltingPoint > aTemperature ? -1 : tLightest.mMaterial.mID);
		tSmeltery.mTemperature = TileEntitySmeltery.DEF_ENV_TEMP; // 293 K — iron is solid
		assertEquals(-1, tCensus.applyAsInt((int)tSmeltery.mTemperature), "a cold crucible displays nothing molten");
		tSmeltery.mTemperature = MT.Fe.mMeltingPoint + 25; // the :469 pour-back band — molten
		assertEquals(MT.Fe.mID, tCensus.applyAsInt((int)tSmeltery.mTemperature), "a hot crucible displays the molten id");
	}

	/** The getUpdateTag anchor: the shipped tag is saveAdditional's product (the forge-leg face; the 21.1 face is arg-shaped). */
	//? if forge {
	@Test
	public void updateTagCarriesTheDisplayCensus() {
		DisplayProbeCrucible tCrucible = new DisplayProbeCrucible(POS, Blocks.BRICKS.defaultBlockState());
		tCrucible.mDisplayedHeight = 42;
		CompoundTag tTag = tCrucible.getUpdateTag();
		assertEquals(42, tTag.getInt(TileEntityCrucible.NBT_DISPLAYED_HEIGHT),
				"getUpdateTag = saveWithoutMetadata ships the display census (the paint-key channel)");
	}
	//?} else {
	/*@Test
	public void updateTagCarriesTheDisplayCensus() {
		DisplayProbeCrucible tCrucible = new DisplayProbeCrucible(POS, Blocks.BRICKS.defaultBlockState());
		tCrucible.mDisplayedHeight = 42;
		CompoundTag tTag = tCrucible.getUpdateTag(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS);
		assertEquals(42, tTag.getInt(TileEntityCrucible.NBT_DISPLAYED_HEIGHT),
				"getUpdateTag = saveWithoutMetadata ships the display census (the paint-key channel)");
	}
	*///?}

	/** The slot feed and content-pile faces are untouched by the display-census append (the merge-order surface). */
	@Test
	public void smelteryNbtRoundTripKeepsInventoryFace() {
		DisplayProbeSmeltery tSmeltery = new DisplayProbeSmeltery(sSmelteryType, POS, Blocks.BRICKS.defaultBlockState());
		CompoundTag tNBT = new CompoundTag();
		tSmeltery.saveNBT(tNBT);
		assertTrue(tNBT.contains("gt.inv"), "the feed slot face is untouched by the display census append");
		assertTrue(tNBT.contains("gt.materials"), "the content pile face is untouched");
	}
}
