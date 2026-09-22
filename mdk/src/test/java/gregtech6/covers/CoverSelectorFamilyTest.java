package gregtech6.covers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.covers.covers.CoverSelectorButtonPanel;
import gregtech6.covers.covers.CoverSelectorManual;
import gregtech6.covers.covers.CoverSelectorRedstone;
import gregtech6.covers.covers.CoverSelectorTag;

/**
 * The selector cover family offline acceptance (task p34-covers-gameplay-10): the four
 * selectors drive the {@link gregtech6.tileentity.machines.ITileEntitySwitchableMode}
 * dial — the Tag selector pins its constructor mode, the Redstone selector mirrors the
 * face signal, the Manual and ButtonPanel selectors are click GUIs over the zone maps —
 * and the shared base resets the dial to 0 on removal (the
 * AbstractCoverAttachmentSelector :31-34 arm).
 */
public class CoverSelectorFamilyTest extends GTCoverTestBase {

	static final BlockPos DIAL_POS = new BlockPos(1, 2, 3);

	static BlockEntityType<TileEntityModeDialProbe> sDialType;

	@BeforeAll
	static void buildDialFixture() {
		sDialType = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityModeDialProbe(sDialType, aPos, aState),
				Blocks.BRICKS).build(null);
	}

	/** A leveled probe host with the given cover mounted on face 3 (the composition store form, the logistics test precedent). */
	static TileEntityModeDialProbe probeWith(ICover aCover) {
		TileEntityModeDialProbe tProbe = new TileEntityModeDialProbe(sDialType, DIAL_POS, Blocks.BRICKS.defaultBlockState());
		tProbe.setLevel(new MachineLevel(new TestRecipeManager()));
		tProbe.setCovers(new CoverData(tProbe));
		tProbe.getCovers().mBehaviours[3] = aCover;
		return tProbe;
	}

	// ------------------------------------------------------------------
	// the tag selector (upstream CoverSelectorTag)
	// ------------------------------------------------------------------

	@Test
	public void tagSelectorPlacementPinsItsConstructorMode() {
		CoverSelectorTag tTag = new CoverSelectorTag((byte) 5);
		TileEntityModeDialProbe tProbe = probeWith(tTag);
		tTag.onCoverPlaced((byte) 3, tProbe.getCovers(), null, ItemStack.EMPTY);
		assertEquals(5, tProbe.mMode, "placement asserts the constructor mode (upstream :44-47)");
	}

	@Test
	public void tagSelectorTickReassertsTheModeAfterDrift() {
		CoverSelectorTag tTag = new CoverSelectorTag((byte) 12);
		TileEntityModeDialProbe tProbe = probeWith(tTag);
		tProbe.mMode = 3; // an external dial drift (the host's own GUI)
		tTag.onTickPre((byte) 3, tProbe.getCovers(), 20, true, false, false);
		assertEquals(12, tProbe.mMode, "every server tick re-pins the mode (upstream :55-57)");
	}

	@Test
	public void tagSelectorBind4ClampsTheConstructorMode() {
		assertEquals(15, new CoverSelectorTag((byte) 99).mMode, "UT.Code.bind4 clamps high (upstream :39)");
		assertEquals(0, new CoverSelectorTag((byte) -3).mMode, "UT.Code.bind4 clamps low");
	}

	// ------------------------------------------------------------------
	// the shared removal reset (upstream AbstractCoverAttachmentSelector :31-34)
	// ------------------------------------------------------------------

	@Test
	public void removingAnySelectorResetsTheDialToZero() {
		CoverSelectorTag tTag = new CoverSelectorTag((byte) 7);
		TileEntityModeDialProbe tProbe = probeWith(tTag);
		tProbe.mMode = 7;
		tTag.onCoverRemove((byte) 3, tProbe.getCovers(), null);
		assertEquals(0, tProbe.mMode, "the removal resets the host dial (the base :31-34)");
	}

	// ------------------------------------------------------------------
	// the placement gate (upstream :41/:37/:37/:41 — the shared form)
	// ------------------------------------------------------------------

	@Test
	public void selectorsRefuseNonSwitchableHosts() {
		TileEntityOvenCoverProbe tOven = bareOven();
		tOven.setCovers(new CoverData(tOven));
		assertTrue(new CoverSelectorTag((byte) 1).interceptCoverPlacement((byte) 3, tOven.getCovers(), null),
				"the oven host implements no ITileEntitySwitchableMode — refused");
		assertFalse(new CoverSelectorTag((byte) 1).interceptCoverPlacement((byte) 3, probeWith(new CoverSelectorTag((byte) 1)).getCovers(), null),
				"the dial probe host is a switchable-mode host — admitted");
	}

	// ------------------------------------------------------------------
	// the redstone selector (upstream CoverSelectorRedstone)
	// ------------------------------------------------------------------

	@Test
	public void redstoneSelectorWritesTheFaceSignalIntoTheDial() {
		CoverSelectorRedstone tSelector = new CoverSelectorRedstone();
		TileEntityModeDialProbe tProbe = probeWith(tSelector);
		tProbe.mIncoming = 9;
		tSelector.onCoverPlaced((byte) 3, tProbe.getCovers(), null, ItemStack.EMPTY);
		assertEquals(9, tProbe.mMode, "placement drives the dial with the face signal (upstream :40-43)");
		assertEquals(9, tProbe.getCovers().mVisuals[3], "the setStateMode RETURN folds into the visual lane (:42)");
	}

	@Test
	public void redstoneSelectorBlockUpdateReMirrorsTheSignal() {
		CoverSelectorRedstone tSelector = new CoverSelectorRedstone();
		TileEntityModeDialProbe tProbe = probeWith(tSelector);
		tProbe.mIncoming = 14;
		tSelector.onBlockUpdate((byte) 3, tProbe.getCovers());
		assertEquals(14, tProbe.mMode, "a block update re-writes the dial (upstream :50-52)");
	}

	// ------------------------------------------------------------------
	// the manual selector (upstream CoverSelectorManual)
	// ------------------------------------------------------------------

	@Test
	public void manualSelectorIncrementZoneWrapsAtFifteen() {
		CoverSelectorManual tManual = new CoverSelectorManual();
		TileEntityModeDialProbe tProbe = probeWith(tManual);
		tProbe.getCovers().mVisuals[3] = 15;
		// the increment zone: x in [PX_N[4], PX_N[1]] = [0.75, 0.9375], y in [PX_P[1], PX_P[4]] = [0.0625, 0.25]
		boolean tHit = tManual.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.8F, 0.1F, 0.1F);
		assertTrue(tHit, "a zone hit consumes the click with the animation (:89-91)");
		assertEquals(0, tProbe.mMode, "15 + 1 wraps to 0 (upstream :67-69)");
		assertEquals(0, tProbe.getCovers().mVisuals[3], "the setStateMode RETURN mirrors into the visual lane (:88)");
	}

	@Test
	public void manualSelectorDecrementZoneAndBitButtons() {
		CoverSelectorManual tManual = new CoverSelectorManual();
		TileEntityModeDialProbe tProbe = probeWith(tManual);
		tProbe.getCovers().mVisuals[3] = 0;
		tManual.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.1F, 0.1F, 0.1F); // the decrement zone
		assertEquals(15, tProbe.mMode, "0 - 1 wraps to 15 (upstream :62-64)");
		tProbe.getCovers().mVisuals[3] = 0;
		tManual.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.6F, 0.7F, 0.1F); // the bit-2 button (x in (0.5, 0.6875])
		assertEquals(2, tProbe.mMode, "the lower band buttons flip bits (upstream :72-87)");
	}

	@Test
	public void manualSelectorMissFallsThroughToTheHost() {
		CoverSelectorManual tManual = new CoverSelectorManual();
		TileEntityModeDialProbe tProbe = probeWith(tManual);
		boolean tHit = tManual.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.5F, 0.5F, 0.5F); // the dead centre: no zone
		assertFalse(tHit, "a zone miss returns false — the click falls through (upstream :89-91)");
		assertEquals(0, tProbe.mMode, "the dial is untouched on a miss");
	}

	@Test
	public void manualSelectorLoadWritesTheSavedVisualIntoTheDial() {
		CoverSelectorManual tManual = new CoverSelectorManual();
		TileEntityModeDialProbe tProbe = probeWith(tManual);
		tProbe.getCovers().mVisuals[3] = 6;
		tManual.onCoverLoaded((byte) 3, tProbe.getCovers());
		assertEquals(6, tProbe.mMode, "a chunk load writes the SAVED lane back into the dial (upstream :45-48)");
	}

	// ------------------------------------------------------------------
	// the button panel selector (upstream CoverSelectorButtonPanel)
	// ------------------------------------------------------------------

	@Test
	public void buttonPanelCellMapsColumnPlusRowTimesFour() {
		CoverSelectorButtonPanel tPanel = new CoverSelectorButtonPanel();
		TileEntityModeDialProbe tProbe = probeWith(tPanel);
		// cell (col 1, row 2): x in [0.25, 0.5), y in [0.5, 0.75) → mode = 1 + 2*4 = 9
		boolean tHit = tPanel.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.3F, 0.6F, 0.1F);
		assertTrue(tHit, "a grid hit always consumes the click (:66)");
		assertEquals(9, tProbe.mMode, "mode = column + row*4 (upstream :62)");
	}

	@Test
	public void buttonPanelMomentaryWindowResetsTheDial() {
		CoverSelectorButtonPanel tPanel = new CoverSelectorButtonPanel();
		TileEntityModeDialProbe tProbe = probeWith(tPanel);
		// arm the momentary window (the screwdriver toggle, upstream :76-80)
		tPanel.onToolClick((byte) 3, tProbe.getCovers(), ICover.TOOL_SCREWDRIVER, 0, null, false, (byte) 3, 0, 0, 0);
		assertEquals(1, tProbe.getCovers().mValues[3], "the screwdriver arms the value lane (:77)");
		tPanel.onCoverClickedRight((byte) 3, tProbe.getCovers(), null, (byte) 3, 0.3F, 0.1F, 0.1F); // cell (col 1, row 0) = mode 1
		assertEquals(10, tProbe.getCovers().mValues[3], "an armed click starts the 10-tick window (:64)");
		assertEquals(1, tProbe.mMode, "the click drove the dial");
		for (long tTimer = 0; tTimer < 8; tTimer++) tPanel.onTickPost((byte) 3, tProbe.getCovers(), tTimer, true, false, false);
		assertEquals(2, tProbe.getCovers().mValues[3], "the window ticks down 10 -> 2 (:87-88)");
		tPanel.onTickPost((byte) 3, tProbe.getCovers(), 9, true, false, false);
		assertEquals(0, tProbe.mMode, "landing on 1 resets the dial (:89)");
	}

	@Test
	public void buttonPanelFoldsTheDialIntoTheLowVisualBits() {
		CoverSelectorButtonPanel tPanel = new CoverSelectorButtonPanel();
		TileEntityModeDialProbe tProbe = probeWith(tPanel);
		tProbe.getCovers().mVisuals[3] = 80; // the upper bits carry the (cut) chisel state; 80 = 0b1010000
		tProbe.mMode = 5;
		tPanel.onBlockUpdate((byte) 3, tProbe.getCovers());
		assertEquals(85, tProbe.getCovers().mVisuals[3], "(visual & ~15) | mode keeps the upper bits (upstream :55)");
	}
}
