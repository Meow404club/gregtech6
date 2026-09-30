package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

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
 * Offline gate for the boiler Jade face (task jade-boiler base, task jade-redesign-core
 * reface): the tag contract over the SAME static seam the live {@code appendServerData}
 * reads through (the public field shape mEnergy/mCapacity/mOutput/mEfficiency/mTanks —
 * GTBoilerTankBlockEntity.java:149-161, TileEntityLargeBoiler.java:170-186), plus the row
 * pure functions. Both BE arms run through real fixtures (the GTBoilerTankBlockEntityTest /
 * LargeBoilerSemanticsTest fixture forms); the BlockAccessor wrapper itself is live-only
 * (the GT6MachineProviderTest posture), and so is the {@code showDetails()} gate and the
 * bar element assembly.
 *
 * <p>The r8 pin set (task jade-boiler-burningbox grows the fluid-identity face): the EIGHT
 * wire keys survive verbatim plus the KEY_WATER_FLUID registry-name string (written only
 * when the tank holds a fluid — a dry tank shows the Empty word), the four-slot water bar
 * template LABEL / X / Y (Z%) with the label = the actual fluid's display name, the
 * three-slot bar template X / Y (Z%) on heat/steam, the
 * half-gate pair and the no-water warning RETIRED (the user ruling: the red empty bar IS
 * the warning face — the row itself stays unstyled) and the sneak band shrunk to the
 * calcification face only. The tooltip language VALUES (en/zh) are pinned by the datagen
 * faces + runData, not here.
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
				aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity(),
				GT6BoilerProvider.tankFluidName(aBoiler.mTanks[0]));
		return tTag;
	}

	/** The large-boiler arm of the same readback (the second instanceof). */
	private static CompoundTag syncOf(TileEntityLargeBoiler aBoiler) {
		CompoundTag tTag = new CompoundTag();
		GT6BoilerProvider.writeBoilerData(tTag, aBoiler.mEnergy, aBoiler.mCapacity, aBoiler.mOutput,
				aBoiler.mTanks[1].amount(), aBoiler.mTanks[1].capacity(),
				aBoiler.mEfficiency, aBoiler.mTanks[0].amount(), aBoiler.mTanks[0].capacity(),
				GT6BoilerProvider.tankFluidName(aBoiler.mTanks[0]));
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
		// the fluid identity face (task jade-boiler-burningbox): a filled water tank rides
		// its registry name over the wire
		assertEquals("minecraft:water", tTag.getString(GT6BoilerProvider.KEY_WATER_FLUID));
		assertEquals(Tag.TAG_STRING, tTag.getTagType(GT6BoilerProvider.KEY_WATER_FLUID));
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

	@Test
	public void theTankKeysStayOnTheDrainedBoiler() {
		// the r8 "always visible" ruling: the water/steam bars render for EVERY boiler, so
		// the tag carries the tank faces even at zero content (the drained boiler still
		// shows the red bar — the warning face) — and the FLUID IDENTITY KEY STAYS ABSENT
		// (an empty tank = the Empty/空罐 label word, task jade-boiler-burningbox).
		FixtureTankBoiler tBoiler = new FixtureTankBoiler(POS, Blocks.STONE.defaultBlockState());
		CompoundTag tTag = syncOf(tBoiler);
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_WATER));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_WATER_MAX));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_STEAM));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_STEAM_MAX));
		assertFalse(tTag.contains(GT6BoilerProvider.KEY_WATER_FLUID),
				"a dry tank writes no fluid identity — the client shows the empty word");
		assertEquals(0L, tTag.getLong(GT6BoilerProvider.KEY_WATER));
		// the empty-label row: the four-slot template with the empty word in the label slot
		TranslatableContents tEmpty = (TranslatableContents) GT6BoilerProvider
				.waterLine(Component.translatable(GT6BoilerProvider.LANG_WATER_EMPTY), 0, 4000).getContents();
		assertEquals(GT6BoilerProvider.LANG_WATER, tEmpty.getKey());
		assertEquals(GT6BoilerProvider.LANG_WATER_EMPTY,
				((TranslatableContents) ((Component) tEmpty.getArgs()[0]).getContents()).getKey());
	}

	// ------------------------------------------------------------------------------------
	// group ② the bar faces (the uniform three-slot X / Y (Z%) template)
	// ------------------------------------------------------------------------------------

	@Test
	public void ratioClampsLikeTheBarsNeed() {
		// the shared clamp on GT6JadeRows (the heat-bar semantics generalized)
		assertEquals(0.5F, GT6JadeRows.ratio(320000, 640000), 1e-6F);
		assertEquals(1.0F, GT6JadeRows.ratio(999999999, 640000), 1e-6F, "overheat clamps at full");
		assertEquals(0.0F, GT6JadeRows.ratio(-5, 640000), 1e-6F, "negative heat never paints progress");
		assertEquals(0.0F, GT6JadeRows.ratio(100, 0), 1e-6F, "a zero ceiling answers 0, never NaN");
	}

	@Test
	public void heatLineCarriesTheThermometerWordingPlusThePercent() {
		// the upstream thermometer readout verbatim shape (MultiTileEntityBoilerTank.java:182)
		// + the third percent slot (the uniform bar template)
		TranslatableContents tContents = (TranslatableContents) GT6BoilerProvider.heatLine(320000, 640000).getContents();
		assertEquals(GT6BoilerProvider.LANG_HEAT, tContents.getKey());
		assertEquals(3, tContents.getArgs().length);
		assertEquals(320000L, tContents.getArgs()[0]);
		assertEquals(640000L, tContents.getArgs()[1]);
		assertEquals(50L, tContents.getArgs()[2]);
	}

	@Test
	public void waterAndSteamLinesAreTheFourSlotTankFaces() {
		// the water bar (always visible — upgraded from the sneak band): the row itself is
		// UNSTYLED even when empty (the alarm face moved onto the red bar element); the
		// LABEL SLOT carries the resolved fluid name (task jade-boiler-burningbox — the
		// hardcoded "Water" retired)
		Component tLabel = Component.translatable("test.water.label");
		TranslatableContents tWater = (TranslatableContents) GT6BoilerProvider.waterLine(tLabel, 2000, 4000).getContents();
		assertEquals(GT6BoilerProvider.LANG_WATER, tWater.getKey());
		assertEquals(4, tWater.getArgs().length);
		assertSame(tLabel, tWater.getArgs()[0]);
		assertEquals(2000L, tWater.getArgs()[1]);
		assertEquals(4000L, tWater.getArgs()[2]);
		assertEquals(50L, tWater.getArgs()[3]);
		Component tEmptyWater = GT6BoilerProvider.waterLine(Component.translatable(GT6BoilerProvider.LANG_WATER_EMPTY), 0, 4000);
		assertFalse(TextColor.fromLegacyFormat(ChatFormatting.RED).equals(tEmptyWater.getStyle().getColor()),
				"the empty-tank red lives on the BAR color, the row stays plain");
		// the steam bar (three slots — the steam tank has no identity face)
		TranslatableContents tSteam = (TranslatableContents) GT6BoilerProvider.steamLine(160000, 640000).getContents();
		assertEquals(GT6BoilerProvider.LANG_STEAM, tSteam.getKey());
		assertEquals(160000L, tSteam.getArgs()[0]);
		assertEquals(640000L, tSteam.getArgs()[1]);
		assertEquals(25L, tSteam.getArgs()[2]);
	}

	@Test
	public void waterLabelFallsBackToTheEmptyWordOnGarbage() {
		// task jade-boiler-burningbox: the label slot = the fluid's own display name via
		// FluidStack.getDisplayName (the GTCEu RecipeOutputProvider posture) — the RESOLVED
		// branch is live-only (the forge vanilla FluidType RegistryObject is not bootstrapped
		// in the offline junit env — the GT6FluidProvider.readFluid posture: live faces get
		// behavior pins on runClient, the offline gate pins the pure fallback)
		TranslatableContents tFallback = (TranslatableContents) GT6BoilerProvider.waterLabel("gt6:not_a_fluid").getContents();
		assertEquals(GT6BoilerProvider.LANG_WATER_EMPTY, tFallback.getKey(),
				"an unresolvable name degrades to the Empty word, never a crash");
	}

	@Test
	public void tankFluidNameReadsTheIdentityAndRefusesEmpties() {
		// the server-side extraction seam (GT6FluidProvider.tankViews guards verbatim)
		FixtureTankBoiler tBoiler = new FixtureTankBoiler(POS, Blocks.STONE.defaultBlockState());
		assertEquals("", GT6BoilerProvider.tankFluidName(tBoiler.mTanks[0]), "a dry tank = the empty string");
		tBoiler.mTanks[0].add(2000, new FluidStack(Fluids.WATER, 2000));
		assertEquals("minecraft:water", GT6BoilerProvider.tankFluidName(tBoiler.mTanks[0]));
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
	// group ④ the sneak band (scale only — the water face moved to the always-on bar)
	// ------------------------------------------------------------------------------------

	@Test
	public void theSneakBandIsTheCalcificationFace() {
		// the r8 ruling: sneak = scale ONLY (the water warning is the red empty bar now)
		TranslatableContents tScale = (TranslatableContents) GT6BoilerProvider.scaleLine(9400).getContents();
		assertEquals(GT6BoilerProvider.LANG_SCALE, tScale.getKey());
		assertEquals(6, tScale.getArgs()[0], "(10000-9400)/100 — the upstream LH.percent face");
		// the clean face + the 5000 floor (the deepest possible scale, the :121/:185 clamp)
		assertEquals(GT6BoilerProvider.LANG_SCALE_CLEAN,
				((TranslatableContents) GT6BoilerProvider.scaleLine(10000).getContents()).getKey());
		assertEquals(0, ((TranslatableContents) GT6BoilerProvider.scaleLine(10000).getContents()).getArgs().length);
		assertEquals(50, ((TranslatableContents) GT6BoilerProvider.scaleLine(5000).getContents()).getArgs()[0]);
	}
}
