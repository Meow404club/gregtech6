package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * Offline gate for task r5-jade-boiler acceptance ②: the boiler Jade face tag contract +
 * the display-line pure functions over the SAME static seam the live
 * {@code appendServerData} reads through (the public field shape mEnergy/mCapacity/mOutput
 * /mEfficiency/mTanks — GTBoilerTankBlockEntity.java:149-161, TileEntityLargeBoiler.java
 * :170-186). Both BE arms run through real fixtures (the GTBoilerTankBlockEntityTest /
 * LargeBoilerSemanticsTest fixture forms); the BlockAccessor wrapper itself is live-only
 * (the GT6MachineProviderTest posture), and so is the {@code showDetails()} gate — the
 * gated band itself is the pinned seam ({@link GT6BoilerProvider#detailLines}).
 *
 * <p>Six SPEC groups: the wire contract (both BE arms, 8 keys typed), the heat-bar ratio +
 * readout line, the demand line (mOutput/2), the output-gate two states (the half-boundary
 * pins the upstream strict {@code amount - capacity/2 > 0} verdict), the sneak-detail band
 * (two lines exactly — calcification %, water/warning) and the no-water RED styling. The
 * tooltip language VALUES (en/zh) are pinned by the datagen faces + runData, not here.
 */
public class GT6BoilerProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(3, 4, 5);

	static BlockEntityType<FixtureTankBoiler> sTankType;
	static BlockEntityType<FixtureLargeBoiler> sLargeType;

	/** The concrete test BE — the small boiler tank over a vanilla-block BET. */
	public static final class FixtureTankBoiler extends GTBoilerTankBlockEntity {
		public FixtureTankBoiler(BlockPos aPos, BlockState aState) {
			super(sTankType, aPos, aState);
		}
	}

	/** The concrete test BE — the large boiler controller over a vanilla-block BET. */
	public static final class FixtureLargeBoiler extends TileEntityLargeBoiler {
		public FixtureLargeBoiler(BlockPos aPos, BlockState aState) {
			super(sLargeType, aPos, aState);
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildOfflineFixtures() {
		BlockEntityType<FixtureTankBoiler>[] tTankHolder =
				(BlockEntityType<FixtureTankBoiler>[]) new BlockEntityType<?>[1];
		tTankHolder[0] = BlockEntityType.Builder.of(FixtureTankBoiler::new, Blocks.STONE).build(null);
		sTankType = tTankHolder[0];
		BlockEntityType<FixtureLargeBoiler>[] tLargeHolder =
				(BlockEntityType<FixtureLargeBoiler>[]) new BlockEntityType<?>[1];
		// two blocks — the base-3x3 cells and the wall ring read different fixture faces
		tLargeHolder[0] = BlockEntityType.Builder.of(FixtureLargeBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sLargeType = tLargeHolder[0];
	}

	/** The BE readback exactly as the live appendServerData performs it (the field shape). */
	private static CompoundTag syncOf(GTBoilerTankBlockEntity aBoiler) {
		CompoundTag tTag = new CompoundTag();
		GT6BoilerProvider.writeBoilerData(tTag, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
				aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
				aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity());
		return tTag;
	}

	/** The large-boiler arm of the same readback (the second instanceof). */
	private static CompoundTag syncOf(TileEntityLargeBoiler aBoiler) {
		CompoundTag tTag = new CompoundTag();
		GT6BoilerProvider.writeBoilerData(tTag, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
				aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
				aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity());
		return tTag;
	}

	// ------------------------------------------------------------------------------------
	// group ① the wire contract (both BE arms)
	// ------------------------------------------------------------------------------------

	@Test
	public void tankBoilerFieldsRideTheEightKeys() {
		FixtureTankBoiler tBoiler = new FixtureTankBoiler(POS, Blocks.STONE.defaultBlockState());
		tBoiler.mEnergy = 320000;
		tBoiler.mOutput = 64; // the :78 re-derivation pairs the steam tank with the HU ceiling
		assertEquals(640000L, tBoiler.mCapacity, "mOutput*10000 — the setOutput double assignment");
		tBoiler.mTanks[1].add(100000, new FluidStack(Fluids.LAVA, 100000)); // the offline steam stand-in
		tBoiler.mEfficiency = 9400;
		tBoiler.mTanks[0].add(2000, new FluidStack(Fluids.WATER, 2000));
		CompoundTag tTag = syncOf(tBoiler);
		assertEquals(320000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT));
		assertEquals(640000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT_MAX));
		assertEquals(32L, tTag.getLong(GT6BoilerProvider.KEY_DEMAND), "mOutput/2 — the :458-460 demand");
		assertEquals(100000L, tTag.getLong(GT6BoilerProvider.KEY_STEAM));
		assertEquals(640000L, tTag.getLong(GT6BoilerProvider.KEY_STEAM_MAX));
		assertEquals(9400L, tTag.getShort(GT6BoilerProvider.KEY_EFFICIENCY));
		assertEquals(2000L, tTag.getLong(GT6BoilerProvider.KEY_WATER));
		assertEquals(4000L, tTag.getLong(GT6BoilerProvider.KEY_WATER_MAX));
		// the key faces: longs except the ten-thousandths efficiency (short)
		for (String tKey : Arrays.asList(GT6BoilerProvider.KEY_HEAT, GT6BoilerProvider.KEY_HEAT_MAX,
				GT6BoilerProvider.KEY_DEMAND, GT6BoilerProvider.KEY_STEAM, GT6BoilerProvider.KEY_STEAM_MAX,
				GT6BoilerProvider.KEY_WATER, GT6BoilerProvider.KEY_WATER_MAX)) {
			assertEquals(Tag.TAG_LONG, tTag.getTagType(tKey), tKey + " rides the long face");
		}
		assertEquals(Tag.TAG_SHORT, tTag.getTagType(GT6BoilerProvider.KEY_EFFICIENCY));
	}

	@Test
	public void largeBoilerArmSpeaksTheSameTagShape() {
		FixtureLargeBoiler tBoiler = new FixtureLargeBoiler(POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(20480000L, tBoiler.mCapacity, "the class-default row (2048 SU → 20480000)");
		tBoiler.mEnergy = 5000000;
		CompoundTag tTag = syncOf(tBoiler);
		assertEquals(5000000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT));
		assertEquals(20480000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT_MAX));
		assertEquals(1024L, tTag.getLong(GT6BoilerProvider.KEY_DEMAND), "2048/2 — the :371-380 demand");
		assertEquals(128000L, tTag.getLong(GT6BoilerProvider.KEY_WATER_MAX));
		assertEquals(10000L, tTag.getShort(GT6BoilerProvider.KEY_EFFICIENCY), "the 10000 clean default");
	}

	// ------------------------------------------------------------------------------------
	// group ② the heat bar (ratio + readout line)
	// ------------------------------------------------------------------------------------

	@Test
	public void heatRatioIsTheClampedEnergyShare() {
		assertEquals(0.5F, GT6BoilerProvider.heatRatio(320000, 640000), 1e-6F);
		assertEquals(1.0F, GT6BoilerProvider.heatRatio(999999999, 640000), 1e-6F, "overheat clamps at full");
		assertEquals(0.0F, GT6BoilerProvider.heatRatio(-5, 640000), 1e-6F, "negative heat never paints progress");
		assertEquals(0.0F, GT6BoilerProvider.heatRatio(100, 0), 1e-6F, "a zero ceiling answers 0, never NaN");
	}

	@Test
	public void heatLineCarriesTheThermometerWording() {
		// the upstream thermometer readout verbatim shape (MultiTileEntityBoilerTank.java:182)
		TranslatableContents tContents = (TranslatableContents) GT6BoilerProvider.heatLine(320000, 640000).getContents();
		assertEquals(GT6BoilerProvider.LANG_HEAT, tContents.getKey());
		assertEquals(2, tContents.getArgs().length);
		assertEquals(320000L, tContents.getArgs()[0]);
		assertEquals(640000L, tContents.getArgs()[1]);
	}

	// ------------------------------------------------------------------------------------
	// group ③ the demand line
	// ------------------------------------------------------------------------------------

	@Test
	public void demandLineCarriesTheEnergyDemandedValue() {
		TranslatableContents tContents = (TranslatableContents) GT6BoilerProvider.demandLine(32).getContents();
		assertEquals(GT6BoilerProvider.LANG_DEMAND, tContents.getKey());
		assertEquals(1, tContents.getArgs().length);
		assertEquals(32L, tContents.getArgs()[0]);
	}

	// ------------------------------------------------------------------------------------
	// group ④ the output gate two states
	// ------------------------------------------------------------------------------------

	@Test
	public void gateBelowHalfExplainsTheSilence() {
		// at the half-tank mark EXACTLY the gate stays shut — the upstream verdict is the
		// strict tAmount = amount - capacity/2 > 0 (:139/:203)
		Component tBelow = GT6BoilerProvider.gateLine(320000, 640000);
		TranslatableContents tContents = (TranslatableContents) tBelow.getContents();
		assertEquals(GT6BoilerProvider.LANG_GATE_BELOW, tContents.getKey());
		assertEquals(2, tContents.getArgs().length);
		assertEquals(320000L, tContents.getArgs()[0]);
		assertEquals(640000L, tContents.getArgs()[1]);
	}

	@Test
	public void gateAboveHalfIsTheCompactOpenFace() {
		Component tAbove = GT6BoilerProvider.gateLine(320001, 640000);
		TranslatableContents tContents = (TranslatableContents) tAbove.getContents();
		assertEquals(GT6BoilerProvider.LANG_GATE_ABOVE, tContents.getKey());
		assertEquals(0, tContents.getArgs().length, "the compact face carries no slots");
	}

	// ------------------------------------------------------------------------------------
	// group ⑤ the sneak-detail band
	// ------------------------------------------------------------------------------------

	@Test
	public void detailBandIsExactlyScalePlusWater() {
		List<Component> tLines = GT6BoilerProvider.detailLines(9400, 2000, 4000);
		assertEquals(2, tLines.size(), "sneak shows the magnifyingglass pair — never more");
		TranslatableContents tScale = (TranslatableContents) tLines.get(0).getContents();
		assertEquals(GT6BoilerProvider.LANG_SCALE, tScale.getKey());
		assertEquals(6, tScale.getArgs()[0], "(10000-9400)/100 — the upstream LH.percent face");
		TranslatableContents tWater = (TranslatableContents) tLines.get(1).getContents();
		assertEquals(GT6BoilerProvider.LANG_WATER, tWater.getKey());
		assertEquals(2000L, tWater.getArgs()[0]);
		assertEquals(4000L, tWater.getArgs()[1]);
	}

	@Test
	public void cleanBoilerAnswersTheCleanFace() {
		List<Component> tLines = GT6BoilerProvider.detailLines(10000, 2000, 4000);
		TranslatableContents tScale = (TranslatableContents) tLines.get(0).getContents();
		assertEquals(GT6BoilerProvider.LANG_SCALE_CLEAN, tScale.getKey());
		assertEquals(0, tScale.getArgs().length);
		// the 5000 floor is the deepest possible scale (the :121/:185 clamp)
		assertEquals(50, ((TranslatableContents) GT6BoilerProvider.detailLines(5000, 0, 4000)
				.get(0).getContents()).getArgs()[0]);
	}

	@Test
	public void noWaterPaintsTheWarningRed() {
		Component tWarning = GT6BoilerProvider.waterLine(0, 4000);
		assertEquals(GT6BoilerProvider.LANG_NO_WATER, ((TranslatableContents) tWarning.getContents()).getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tWarning.getStyle().getColor(),
				"the upstream WARNING: NO WATER!!! alarm face");
		assertNotRed(GT6BoilerProvider.waterLine(1, 4000), "a wet boiler stays unstyled");
	}

	private static void assertNotRed(Component aLine, String aMessage) {
		assertTrue(!TextColor.fromLegacyFormat(ChatFormatting.RED).equals(aLine.getStyle().getColor()), aMessage);
	}
}
