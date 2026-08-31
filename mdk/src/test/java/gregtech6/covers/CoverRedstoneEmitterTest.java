package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import gregtech6.covers.covers.CoverRedstoneEmitter;
import gregtech6.util.UT6;

/**
 * The redstone emitter acceptance tables (task p9-redstone-cover-emitter, ADR
 * 2026-09-01-p9-redstone-cover-emitter acceptance ④). Offline and exhaustive:
 *
 * <ul>
 * <li>the emission truth table — tier 0..15 x weak/strong x the strong gate, with the
 *     machine-default argument IGNORED (the overrides return, never merge — upstream
 *     CoverRedstoneEmitter :55-62);</li>
 * <li>the six-direction OPOS table through the ICoverableTE exits (the direction fold
 *     is the host's, the emitter only answers its own face);</li>
 * <li>the keypad zone table (:72-109) with the 0↔15 wrap boundaries and the miss
 *     fall-through;</li>
 * <li>the cutter strong-gate toggle (:42-46) with the block-update count;</li>
 * <li>the incoming read staying the AbstractCoverDefault world pass-through (zero
 *     getRedstoneIn override — the upstream class has none);</li>
 * <li>the visuals persistence gate (needsVisualsSaved → the aIncludeVisuals=F write
 *     keeps the tier) and the NBT round-trip;</li>
 * <li>the inlined attachment flags (:35-40) and the tier sprite mapping.</li>
 * </ul>
 */
public class CoverRedstoneEmitterTest extends GTCoverTestBase {

	private static final byte FACE = 5; // EAST — the fixed mounting face

	/** A probe that counts the CoverData-triggered neighbour updates (the fake-TE double). */
	static class CountingProbe extends TileEntityOvenCoverProbe {
		public int mBlockUpdates;

		CountingProbe() {
			super(sCoverOvenType, COVER_POS, Blocks.BRICKS.defaultBlockState());
		}

		@Override
		public void sendBlockUpdateFromCover() {
			mBlockUpdates++;
		}
	}

	/** Mounts a fresh emitter on the given face and returns the probe + store pair. */
	private static TileEntityOvenCoverProbe emitterOven(byte aSide) {
		TileEntityOvenCoverProbe tOven = bareOven();
		CoverRegistry.put(Items.BRICK, new CoverRedstoneEmitter());
		assertTrue(tOven.setCoverItem(aSide, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		return tOven;
	}

	/** An emitter on a server-side stub level (the keypad visual write is server-gated). */
	private static TileEntityOvenCoverProbe leveledEmitterOven() {
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		tOven.setLevel(new MachineLevel(new TestRecipeManager()));
		return tOven;
	}

	/** The bare-hand click helper — ICoverableTE.onCoverUse passes aSide for both sides. */
	private static boolean clickKeypad(CoverData aData, float aHitX, float aHitY, float aHitZ) {
		return aData.mBehaviours[FACE].onCoverClickedRight(FACE, aData, null, FACE, aHitX, aHitY, aHitZ);
	}

	/** EAST-face texture coordinates: u = 1 - hitZ, v = 1 - hitY (UT.Code.getFacingCoordsClicked case 5). */
	private static boolean clickUV(CoverData aData, float aU, float aV) {
		return clickKeypad(aData, 0.5F, 1 - aV, 1 - aU);
	}

	// ---------------------------------------------------------------------------
	// the emission truth table (:55-62) — tier x weak/strong x the strong gate
	// ---------------------------------------------------------------------------

	@Test
	public void emissionTruthTableTierByWeakStrongAndGate() {
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		CoverData tData = tOven.getCovers();
		ICover tCover = tData.mBehaviours[FACE];
		for (int tTier = 0; tTier <= 15; tTier++) {
			tData.visual(FACE, (short) tTier, false);
			// weak = the tier lane, the machine default argument is IGNORED (feed 9)
			assertEquals(tTier, tCover.getRedstoneOutWeak(FACE, tData, (byte) 9),
					"tier " + tTier + ": the weak emission reads the visual lane, never the machine default");
			// strong gate OFF (mValues == 0): strong = 0, the machine default (9) is NOT merged
			assertEquals(0, tCover.getRedstoneOutStrong(FACE, tData, (byte) 9),
					"tier " + tTier + " gate off: strong stays 0 — :55-57 returns, never merges");
			// strong gate ON (mValues != 0): strong re-reads the weak hook
			tData.value(FACE, (short) 1, false);
			assertEquals(tTier, tCover.getRedstoneOutStrong(FACE, tData, (byte) 9),
					"tier " + tTier + " gate on: strong re-reads the weak emission");
			assertEquals(tTier, tCover.getRedstoneOutStrong(FACE, tData, (byte) 0),
					"the gate-on strong answer is likewise default-blind");
			tData.value(FACE, (short) 0, false);
		}
		// the visual lane is bind4-clamped on the read (UT.Code.bind4) — an out-of-domain
		// lane value still reads on the redstone scale
		tData.mVisuals[FACE] = 200;
		assertEquals(15, tCover.getRedstoneOutWeak(FACE, tData, (byte) 0), "bind4(200) == 15");
		tData.mVisuals[FACE] = -3;
		assertEquals(0, tCover.getRedstoneOutWeak(FACE, tData, (byte) 0), "bind4(-3) == 0");
	}

	@Test
	public void emissionCarriesTheNonZeroGateSemantics() {
		// upstream keeps the raw mValues != 0 verdict (any non-zero gates, not just bit 0)
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		CoverData tData = tOven.getCovers();
		ICover tCover = tData.mBehaviours[FACE];
		tData.visual(FACE, (short) 5, false);
		tData.value(FACE, (short) 2, false);
		assertEquals(5, tCover.getRedstoneOutStrong(FACE, tData, (byte) 0), "any non-zero mValues gates strong on");
	}

	// ---------------------------------------------------------------------------
	// the six-direction OPOS table — the host fold reaches the emitter's own face
	// ---------------------------------------------------------------------------

	@Test
	public void outgoingSixDirectionOposTable() {
		for (Direction tQuery : Direction.values()) {
			byte tEmissionFace = UT6.OPOS[tQuery.get3DDataValue()];
			TileEntityOvenCoverProbe tOven = emitterOven(tEmissionFace);
			CoverData tData = tOven.getCovers();
			tData.visual(tEmissionFace, (short) 7, false);
			byte tQuerySide = (byte) tQuery.get3DDataValue();
			assertEquals(7, tOven.getRedstoneOutWeak(tQuerySide, 0),
					"query " + tQuery + " folds onto face " + tEmissionFace + " — the emitter's weak tier");
			assertEquals(0, tOven.getRedstoneOutStrong(tQuerySide, 0), "gate off: the strong exit is 0");
			tData.value(tEmissionFace, (short) 1, false);
			assertEquals(7, tOven.getRedstoneOutStrong(tQuerySide, 0), "gate on: the strong exit carries the tier");
			// the neighbouring query from the SAME side the emitter sits on reads the bare face
			byte tBareQuery = (byte) tEmissionFace; // querying along the emission face folds to OPOS[face] != face
			byte tOtherFace = UT6.OPOS[tBareQuery];
			if (tOtherFace != tEmissionFace) {
				assertEquals(0, tOven.getRedstoneOutWeak(tBareQuery, 0), "the opposite query lands on the bare face");
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the keypad zone table (:72-109) — zones, misses, the 0↔15 wraps
	// ---------------------------------------------------------------------------

	@Test
	public void keypadZoneTruthTable() {
		TileEntityOvenCoverProbe tOven = leveledEmitterOven();
		CoverData tData = tOven.getCovers();
		// the minus cell (upper-left): tier-1 with the 0→15 wrap
		tData.visual(FACE, (short) 0, false);
		assertTrue(clickUV(tData, 0.15625F, 0.15625F), "a zone hit returns true (the click animation)");
		assertEquals(15, tData.mVisuals[FACE], "the minus cell wraps 0 → 15");
		// the plus cell (upper-right): tier+1 with the 15→0 wrap
		tData.visual(FACE, (short) 15, false);
		assertTrue(clickUV(tData, 0.84375F, 0.15625F), "the plus cell hits");
		assertEquals(0, tData.mVisuals[FACE], "the plus cell wraps 15 → 0");
		// the four bit cells of the lower row
		tData.visual(FACE, (short) 0, false);
		assertTrue(clickUV(tData, 0.21875F, 0.65625F));
		assertEquals(8, tData.mVisuals[FACE], "the leftmost cell toggles bit 8");
		assertTrue(clickUV(tData, 0.40625F, 0.65625F));
		assertEquals(12, tData.mVisuals[FACE], "the second cell toggles bit 4");
		assertTrue(clickUV(tData, 0.59375F, 0.65625F));
		assertEquals(14, tData.mVisuals[FACE], "the third cell toggles bit 2");
		assertTrue(clickUV(tData, 0.78125F, 0.65625F));
		assertEquals(15, tData.mVisuals[FACE], "the rightmost cell toggles bit 1");
		// the same cells toggle OFF
		assertTrue(clickUV(tData, 0.78125F, 0.65625F));
		assertEquals(14, tData.mVisuals[FACE], "bit 1 off");
		assertTrue(clickUV(tData, 0.21875F, 0.65625F));
		assertEquals(6, tData.mVisuals[FACE], "bit 8 off");
	}

	@Test
	public void keypadMissFallsThroughWithoutChangingTheTier() {
		TileEntityOvenCoverProbe tOven = leveledEmitterOven();
		CoverData tData = tOven.getCovers();
		tData.visual(FACE, (short) 9, false);
		// the upper-row middle gap (between the minus and plus cells)
		assertFalse(clickUV(tData, 0.5F, 0.15625F), "a keypad miss returns false — the host proceeds");
		assertEquals(9, tData.mVisuals[FACE], "the tier is unchanged on a miss");
		// the vertical band between the rows
		assertFalse(clickUV(tData, 0.5F, 0.4F), "between the rows is a miss");
		// the lower row, left of the bit-cell block
		assertFalse(clickUV(tData, 0.05F, 0.65625F), "left of the bit cells is a miss");
		// the lower row, right of the bit-cell block
		assertFalse(clickUV(tData, 0.95F, 0.65625F), "right of the bit cells is a miss");
		assertEquals(9, tData.mVisuals[FACE], "still unchanged");
	}

	@Test
	public void keypadClicksAreBlindToOtherFaces() {
		TileEntityOvenCoverProbe tOven = leveledEmitterOven();
		CoverData tData = tOven.getCovers();
		tData.visual(FACE, (short) 3, false);
		// aSide != aSideClicked → upstream returns F before touching anything (:73/:108)
		assertFalse(tData.mBehaviours[FACE].onCoverClickedRight(FACE, tData, null, (byte) 2, 0.15625F, 0.9375F, 0.84375F),
				"a click resolved on another face never tunes this plate");
		assertEquals(3, tData.mVisuals[FACE]);
	}

	// ---------------------------------------------------------------------------
	// the cutter strong-gate toggle (:42-46) + the block-update count
	// ---------------------------------------------------------------------------

	@Test
	public void cutterTogglesTheStrongGateAndCountsBlockUpdates() {
		CountingProbe tOven = new CountingProbe();
		CoverRegistry.put(Items.BRICK, new CoverRedstoneEmitter());
		assertTrue(tOven.setCoverItem(FACE, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		CoverData tData = tOven.getCovers();
		assertEquals(0, tData.mValues[FACE], "fresh install: the strong gate is off");
		int tUpdatesBefore = tOven.mBlockUpdates;
		assertEquals(1000, tOven.onCoverToolClick(CoverRedstoneEmitter.TOOL_CUTTER, null, ItemStack.EMPTY, FACE, false),
				":45 — the cutter relay answers 1000");
		assertEquals(1, tData.mValues[FACE], "bit 0 toggled on");
		assertEquals(tUpdatesBefore + 1, tOven.mBlockUpdates, "the value write fired sendBlockUpdateFromCover");
		assertEquals(1000, tOven.onCoverToolClick(CoverRedstoneEmitter.TOOL_CUTTER, null, ItemStack.EMPTY, FACE, false),
				"the toggle answers 1000 both ways");
		assertEquals(0, tData.mValues[FACE], "bit 0 toggled off");
		assertEquals(tUpdatesBefore + 2, tOven.mBlockUpdates);
		// a same-value write fires nothing (CoverData.value :130 change gate)
		tData.value(FACE, (short) 0, true);
		assertEquals(tUpdatesBefore + 2, tOven.mBlockUpdates, "no change, no update");
	}

	@Test
	public void emitterDoesNotAnswerTheScrewdriver() {
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		// the :51 onToolClick2 host-relay arm is not ported — non-cutter returns 0
		assertEquals(0, tOven.onCoverToolClick(ICover.TOOL_SCREWDRIVER, null, ItemStack.EMPTY, FACE, false),
				"the declared deviation: the emitter only answers the cutter");
		assertEquals(0, tOven.getCovers().mValues[FACE], "the relay attempt changed nothing");
	}

	// ---------------------------------------------------------------------------
	// the incoming read stays the world pass-through (no getRedstoneIn override)
	// ---------------------------------------------------------------------------

	@Test
	public void incomingReadIsNotOverridden() {
		TileEntityOvenCoverProbe tOven = bareOven();
		RedstoneHooksTest.RedstoneLevel tLevel = new RedstoneHooksTest.RedstoneLevel();
		tLevel.mSignal = 7;
		tOven.setLevel(tLevel);
		CoverRegistry.put(Items.BRICK, new CoverRedstoneEmitter());
		assertTrue(tOven.setCoverItem(FACE, new ItemStack(Items.BRICK), null, true, false), "install accepted");
		// upstream CoverRedstoneEmitter (136 lines) and the AbstractCoverAttachment chain
		// have no getRedstoneIn — the default world read answers the covered face
		assertEquals(7, tOven.getRedstoneIncoming(FACE), "the emitter face passes the neighbour signal through");
		assertEquals(1, tLevel.mQueriedDirs.size(), "the world query actually ran (no override sat in front of it)");
	}

	// ---------------------------------------------------------------------------
	// the visuals persistence gate (:114 → CoverData :87) + the NBT round-trip
	// ---------------------------------------------------------------------------

	@Test
	public void tierAndGateSurviveTheSaveRoundTrip() {
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		CoverData tData = tOven.getCovers();
		tData.visual(FACE, (short) 13, false);
		tData.value(FACE, (short) 1, false);
		// the aIncludeVisuals=F form — the emitter's needsVisualsSaved keeps the tier lane
		CompoundTag tTag = tData.writeToNBT(new CompoundTag(), false);
		assertEquals(13, tTag.getShort("r"), "the VISUAL_KEYS[5] lane rides the needsVisualsSaved gate");
		assertEquals(1, tTag.getShort("5"), "the VALUE_KEYS[5] strong gate persists");
		// rehydrate on a fresh host
		TileEntityOvenCoverProbe tSecond = bareOven();
		CoverData tRevived = CoverRegistry.coverdata(tSecond, tTag);
		assertNotNull(tRevived.mBehaviours[FACE], "the behaviour re-resolves from the id lane");
		assertEquals(13, tRevived.mVisuals[FACE], "the tier survives");
		assertEquals(1, tRevived.mValues[FACE], "the strong gate survives");
		assertEquals(13, tRevived.mBehaviours[FACE].getRedstoneOutWeak(FACE, tRevived, (byte) 0), "the revived emission reads the revived tier");
		assertEquals(13, tRevived.mBehaviours[FACE].getRedstoneOutStrong(FACE, tRevived, (byte) 0), "the revived gate re-reads the revived tier");
	}

	// ---------------------------------------------------------------------------
	// the inlined attachment flags (:35-40) + the tier sprite mapping (:111)
	// ---------------------------------------------------------------------------

	@Test
	public void attachmentFlagsAndSpriteMapping() {
		CoverRedstoneEmitter tEmitter = new CoverRedstoneEmitter();
		TileEntityOvenCoverProbe tOven = emitterOven(FACE);
		CoverData tData = tOven.getCovers();
		assertFalse(tEmitter.interceptClickLeft(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":37 — the left click falls through");
		assertFalse(tEmitter.interceptClickRight(FACE, tData, null, FACE, 0.5F, 0.5F, 0.5F), ":38 — a keypad miss falls through to the host");
		assertFalse(tEmitter.isOpaque(FACE, tData), ":39 — attachment plates are non-opaque");
		assertFalse(tEmitter.isSealable(FACE, tData), ":40 — attachment plates are non-sealable");
		assertTrue(tEmitter.needsVisualsSaved(FACE, tData), ":114 — the tier persists");
		for (int tTier = 0; tTier <= 15; tTier++) {
			tData.visual(FACE, (short) tTier, false);
			assertEquals("gt6:block/redstone_emitter/" + tTier, tEmitter.getCoverTextureSurface(FACE, tData).toString(),
					"the tier maps onto its sprite");
			assertEquals(new net.minecraft.resources.ResourceLocation("gt6", CoverRedstoneEmitter.SPRITE_PATH + tTier),
					CoverRedstoneEmitter.spriteForTier(tTier), "the pure sprite function");
		}
	}
}
