package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import snownee.jade.api.view.ViewGroup;

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.fluid.GTFluids;
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
 * <p>The r8 pin set (task jade-boiler-burningbox grows the fluid-identity face): the NINE
 * wire keys survive verbatim plus the KEY_WATER_FLUID registry-name string (written only
 * when the tank holds a fluid), the four-slot water bar
 * template LABEL / X / Y (Z%) with the label = the actual fluid's display name, the
 * three-slot bar template X / Y (Z%) on heat/steam, the
 * half-gate pair and the no-water warning RETIRED (the user ruling: the red empty bar IS
 * the warning face — the row itself stays unstyled) and the sneak band shrunk to the
 * calcification face only. Task boiler-jade-display grows the production face: the rate
 * key (the current steam production, the tick conversion formula mirrored) and the
 * efficiency key (the ten-thousandths heat-to-steam utilization as a percent), and
 * REVERSES the empty-water face per the newer user ruling (空就是空): a DRY tank rides NO
 * water row at all — the "Empty" word survives only as the unresolvable-identity label
 * fallback (the burningbox garbage pin). The tooltip language VALUES (en/zh) are pinned
 * by the datagen faces + runData, not here.
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
	public void tankBoilerFieldsRideTheNineKeys() {
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
		// the production face (task boiler-jade-display ③): min(640000/2560=250,
		// min(320000/80=4000, 2000)) = 250 conversions x 9400/10000 x 160 mB = 37600 mB/t
		assertEquals(37600L, tTag.getLong(GT6BoilerProvider.KEY_RATE), "the tick conversion formula mirrored server-side");
		// the fluid identity face (task jade-boiler-burningbox): a filled water tank rides
		// its registry name over the wire
		assertEquals("minecraft:water", tTag.getString(GT6BoilerProvider.KEY_WATER_FLUID));
		assertEquals(Tag.TAG_STRING, tTag.getTagType(GT6BoilerProvider.KEY_WATER_FLUID));
		// the key faces: longs except the ten-thousandths efficiency (short)
		for (String tKey : Arrays.asList(GT6BoilerProvider.KEY_HEAT, GT6BoilerProvider.KEY_HEAT_MAX,
				GT6BoilerProvider.KEY_DEMAND, GT6BoilerProvider.KEY_STEAM, GT6BoilerProvider.KEY_STEAM_MAX,
				GT6BoilerProvider.KEY_WATER, GT6BoilerProvider.KEY_WATER_MAX, GT6BoilerProvider.KEY_RATE)) {
			assertEquals(Tag.TAG_LONG, tTag.getTagType(tKey), tKey + " rides the long face");
		}
		assertEquals(Tag.TAG_SHORT, tTag.getTagType(GT6BoilerProvider.KEY_EFFICIENCY));
	}

	@Test
	public void largeBoilerArmSpeaksTheSameTagShape() {
		FixtureLargeBoiler tBoiler = new FixtureLargeBoiler(POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(20480000L, tBoiler.mCapacity, "the class-default row (2048 SU → 20480000)");
		tBoiler.mEnergy = 5000000;
		tBoiler.mTanks[0].add(2000, new FluidStack(Fluids.WATER, 2000));
		CompoundTag tTag = syncOf(tBoiler);
		assertEquals(5000000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT));
		assertEquals(20480000L, tTag.getLong(GT6BoilerProvider.KEY_HEAT_MAX));
		assertEquals(1024L, tTag.getLong(GT6BoilerProvider.KEY_DEMAND), "2048/2 — the :371-380 demand");
		assertEquals(128000L, tTag.getLong(GT6BoilerProvider.KEY_WATER_MAX));
		assertEquals(10000L, tTag.getShort(GT6BoilerProvider.KEY_EFFICIENCY), "the 10000 clean default");
		// the rate rides the SAME formula in both BEs (the tick bodies are verbatim twins):
		// min(tank 2048000/2560=800, min(5000000/80=62500, 2000)) = 800 conversions x 160 mB
		// — the pre-row field-init tank (the foreign-block fixture never ran setOutput),
		// which is the exact capacity face the tick itself reads (:392)
		assertEquals(128000L, tTag.getLong(GT6BoilerProvider.KEY_RATE));
	}

	@Test
	public void theTankKeysStayOnTheDrainedBoiler() {
		// the tag carries the tank faces even at zero content (the bar faces when filled),
		// and the FLUID IDENTITY KEY STAYS ABSENT on a dry tank — task boiler-jade-display ①
		// (空就是空): the dry tank rides NO water row at all (the empty-word face retired).
		FixtureTankBoiler tBoiler = new FixtureTankBoiler(POS, Blocks.STONE.defaultBlockState());
		CompoundTag tTag = syncOf(tBoiler);
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_WATER));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_WATER_MAX));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_STEAM));
		assertTrue(tTag.contains(GT6BoilerProvider.KEY_STEAM_MAX));
		assertFalse(tTag.contains(GT6BoilerProvider.KEY_WATER_FLUID),
				"a dry tank writes no fluid identity");
		assertFalse(GT6BoilerProvider.waterRowVisible(tTag),
				"empty = empty — the row gate answers false on the drained boiler");
		// the filled tank opens the gate (the two facts the gate reads are coupled server-side)
		tBoiler.mTanks[0].add(2000, new FluidStack(Fluids.WATER, 2000));
		assertTrue(GT6BoilerProvider.waterRowVisible(syncOf(tBoiler)));
		// defensive: an amount without an identity (cannot happen through writeBoilerData)
		// stays hidden rather than rendering a labelless bar
		CompoundTag tIdless = new CompoundTag();
		tIdless.putLong(GT6BoilerProvider.KEY_WATER, 2000);
		assertFalse(GT6BoilerProvider.waterRowVisible(tIdless));
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
		// the water bar (the filled-tank face — task boiler-jade-display ① hides it when
		// empty): the row itself is UNSTYLED (the label slot carries the resolved fluid
		// name, task jade-boiler-burningbox — the hardcoded "Water" retired)
		Component tLabel = Component.translatable("test.water.label");
		TranslatableContents tWater = (TranslatableContents) GT6BoilerProvider.waterLine(tLabel, 2000, 4000).getContents();
		assertEquals(GT6BoilerProvider.LANG_WATER, tWater.getKey());
		assertEquals(4, tWater.getArgs().length);
		assertSame(tLabel, tWater.getArgs()[0]);
		assertEquals(2000L, tWater.getArgs()[1]);
		assertEquals(4000L, tWater.getArgs()[2]);
		assertEquals(50L, tWater.getArgs()[3]);
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

	// ------------------------------------------------------------------------------------
	// group ⑤ the production face (task boiler-jade-display ③④ — rate + efficiency)
	// ------------------------------------------------------------------------------------

	@Test
	public void steamRateMirrorsTheTickConversionFormula() {
		// the BE tick pure mirror (:256-265 tank / :392-401 large — verbatim twins):
		// conversions = min(steamTank/2560, min(heat/80, water)); rate = units(conversions,
		// 10000, eff*160) — 80 HU + 1 L water -> 160 mB steam at full efficiency.
		assertEquals(40000L, GT6BoilerProvider.steamRate(320000, 640000, 2000, 10000),
				"min(250, min(4000, 2000)) = 250 conversions x 160 mB at full efficiency");
		assertEquals(37600L, GT6BoilerProvider.steamRate(320000, 640000, 2000, 9400),
				"the scale factor rides the same units() chain as the tick body");
		assertEquals(0L, GT6BoilerProvider.steamRate(0, 640000, 2000, 10000), "no heat = no production");
		assertEquals(0L, GT6BoilerProvider.steamRate(320000, 640000, 0, 10000), "no water = no production");
		assertEquals(0L, GT6BoilerProvider.steamRate(320000, 640000, 2000, 0),
				"a zero efficiency answers zero, never a crash");
		// the 2560 ceiling arm: a small steam tank caps the conversions before the heat does
		assertEquals(16000L, GT6BoilerProvider.steamRate(Long.MAX_VALUE / 4, 256000, Long.MAX_VALUE / 4, 10000),
				"256000/2560 = 100 conversions -> 16000 mB");
	}

	@Test
	public void rateLineCarriesTheCurrentProductionValue() {
		TranslatableContents tContents = (TranslatableContents) GT6BoilerProvider.rateLine(37600).getContents();
		assertEquals(GT6BoilerProvider.LANG_RATE, tContents.getKey());
		assertEquals(1, tContents.getArgs().length);
		assertEquals(37600L, tContents.getArgs()[0]);
	}

	@Test
	public void efficiencyLineIsTheHeatUtilizationPercent() {
		// the display convention (declared in the provider javadoc): mEfficiency is the
		// ten-thousandths heat-to-steam utilization (the scale-decay face, 5000 floor),
		// shown as a plain percent — 9400 -> 94%, 5000 -> 50%, pristine -> 100%.
		TranslatableContents tContents = (TranslatableContents) GT6BoilerProvider.efficiencyLine(9400).getContents();
		assertEquals(GT6BoilerProvider.LANG_EFFICIENCY, tContents.getKey());
		assertEquals(1, tContents.getArgs().length);
		assertEquals(94, tContents.getArgs()[0]);
		assertEquals(50, ((TranslatableContents) GT6BoilerProvider.efficiencyLine(5000).getContents()).getArgs()[0]);
		assertEquals(100, ((TranslatableContents) GT6BoilerProvider.efficiencyLine(10000).getContents()).getArgs()[0]);
	}

	// ------------------------------------------------------------------------------------
	// group ⑥ the native universal fluid face ban (task boiler-jade-display ②)
	// ------------------------------------------------------------------------------------

	@Test
	public void theNativeUniversalFluidFaceIsBannedOnBoilers() {
		// both boiler BEs expose the FLUID_HANDLER capability (GTBoilerTankBlockEntity
		// :492-502 / TileEntityLargeBoiler :780-787), so Jade's builtin universal
		// FluidStorageProvider renders their tanks a SECOND time beside the GT rows — the
		// user-visible double steam tank. The ban = a non-null EMPTY group list: the
		// universal chain is first-non-null-wins-then-stop (jade-1201
		// FluidStorageProvider.putData — ifnull->next / return-after-first-non-null
		// bytecode; jade-1211 CommonProxy.getServerExtensionData same shape), and the
		// client renders nothing for empty groups.
		FixtureTankBoiler tTank = new FixtureTankBoiler(POS, Blocks.STONE.defaultBlockState());
		List<ViewGroup<CompoundTag>> tTankGroups = GT6FluidProvider.groupsOfTarget(tTank);
		assertNotNull(tTankGroups, "null hands the face back to Jade's builtin capability provider — ban failed");
		assertTrue(tTankGroups.isEmpty(), "the empty list short-circuits the builtin and renders nothing");
		FixtureLargeBoiler tLarge = new FixtureLargeBoiler(POS, Blocks.BRICKS.defaultBlockState());
		List<ViewGroup<CompoundTag>> tLargeGroups = GT6FluidProvider.groupsOfTarget(tLarge);
		assertNotNull(tLargeGroups, "the large boiler arm is banned the same way");
		assertTrue(tLargeGroups.isEmpty());
	}

	// ------------------------------------------------------------------------------------
	// group ⑦ the bar colors + the overlay identities (task r11c-jade-bar-fluid-color)
	// ------------------------------------------------------------------------------------

	@Test
	public void theHeatBarPaintsTheOvenOrangeNotTheGreen() {
		// ② the hot arm rides the oven-fill orange (GTOvenScreen.java:31 COLOR_FILL — the
		// same heat semantics, the user-reported green read as "cold"), NOT the progress
		// green; the drained arm keeps COLOR_STALLED red (untouched face)
		assertEquals(0xFFFF8800, GT6JadeRows.COLOR_HEAT, "the oven orange");
		assertEquals(0xFF4CBB17, GT6JadeRows.COLOR_OK,
				"the green stays the progress face — a guard against the two semantics conflating back");
	}

	@Test
	public void theWaterBarOverlayFollowsTheWireIdentity() {
		// ① the water bar overlay = the official fluid element over the SAME registry name
		// the wire carries (KEY_WATER_FLUID); minecraft:water resolves offline through the
		// vanilla registry (the tankFluidName pin's mirror), and the unresolvable arm
		// answers null = the plain bar, never a crash (the crucible overlayElement guard posture)
		assertSame(Fluids.WATER, GT6FluidProvider.resolveFluid("minecraft:water"));
		assertNull(GT6BoilerProvider.fluidOverlay("gt6:not_a_fluid", 1),
				"garbage identity = no overlay, not a crash");
	}

	@Test
	public void theSteamBarOverlayIsTheSteamIdentity() {
		// ① the steam bar = the fixed gt6:steam source (GTFluids.STEAM) — the FluidType
		// client extensions declare still = the vanilla water_still over the spec's tint
		// (GTFluids engineFluid template), and the spec row is the offline-readable half
		// ("readable OFFLINE — the live FluidType carries the same numbers at registration")
		assertEquals("gt6:steam", GT6BoilerProvider.STEAM_FLUID);
		assertNotNull(GTFluids.engineSpec("steam"), "the steam spec row is live");
		assertEquals(0xFFC8C8C8, GTFluids.engineSpec("steam").tint(),
				"the light-grey steam tint — the tint half of the still+tint identity");
	}
}
