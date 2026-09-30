package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * Offline gate for task jade-redesign-core (the machine face of the Jade redesign,
 * over the p27 energy-seam base): the machine-family tag contract — now written by BOTH
 * arms, the single-block {@link TileEntityBasicMachine} AND the multiblock controller
 * {@link TileEntityBase10MultiBlockMachine} (the field-isomorphic progress blind-spot
 * fix, TileEntityBase10MultiBlockMachine.java:166-175) — plus the row pure functions.
 *
 * <p>The r8 pin set: the KEY_SUCCESSFUL dead key is GONE (payload minimization), the
 * status line carries the Active/Inactive semantics (lifted off the bar coloring), the
 * progress text rides the uniform three-slot X / Y (Z%) template, the energy/input rows
 * are the text forms ("Stored: %s %s" / the four-slot input band) and the Malfunction
 * displacement (the constant RED face + the 64-char sneak detail) replaces the raw error
 * passthrough. The {@code appendTooltip} element assembly itself stays live-only (ITooltip
 * needs a client element helper — the GT6FluidProviderTest posture); the tag contract and
 * the keyed rows are the offline seams.
 */
public class GT6MachineProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityTypeHolder sHolder;
	static MultiBlockTypeHolder sMultiHolder;

	/** Mutable BET holder (the GT6FluidProviderTest.machineType shape — the factory needs the type it is creating). */
	static final class BlockEntityTypeHolder {
		net.minecraft.world.level.block.entity.BlockEntityType<TileEntityBasicMachine> type;
	}

	/** The multiblock twin of the same holder trick. */
	static final class MultiBlockTypeHolder {
		BlockEntityType<TestMultiMachine> type;
	}

	/** The minimal concrete multiblock machine — the r8 fixture arm (the abstract seams stubbed). */
	static class TestMultiMachine extends TileEntityBase10MultiBlockMachine {
		TestMultiMachine(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "gt6.multiblockmachine.test";
		}

		@Override
		public boolean isInsideStructure(int aX, int aY, int aZ) {
			return false; // the structure face is the p4 base's tests — never consulted here
		}

		@Override
		protected net.minecraftforge.fluids.capability.IFluidHandler getFluidOutputTarget(@Nullable Fluid aOutput) {
			return null;
		}
	}

	@BeforeAll
	static void fixture() {
		// the neo-leg FML test boot constructs the mod, so the PRODUCTION maps
		// (GT6RecipeMaps.java:979 "gt.recipe.shredder") already sit in RecipeMap.RECIPE_MAPS
		// when this fixture runs — a bare-JVM assumption the forge leg never exposed. Drop
		// the generation first (the same registry this class hands back clean via @AfterAll):
		// the fixture map registers into a clean, order- and boot-shape-independent registry.
		gregtech6.recipes.GT6RecipeMaps.reset();
		RecipeMap tMap = new RecipeMap(new HashSet<>(),
				"gt.recipe.shredder", "Shredder", null,
				0, 1,
				"gt6:textures/gui/machines/shredder",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,
				/*MIN=*/ 1,
				/*AMP=*/ 1);
		sHolder = new BlockEntityTypeHolder();
		sHolder.type = net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(sHolder.type, aPos, aState, tMap, 1, false, null),
				Blocks.BRICKS).build(null);
		sMultiHolder = new MultiBlockTypeHolder();
		sMultiHolder.type = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestMultiMachine(sMultiHolder.type, aPos, aState),
				Blocks.BRICKS).build(null);
	}

	/**
	 * The fixture map lands in RecipeMap.RECIPE_MAPS and STAYS after the class — the
	 * w4-f1-chemicals full-suite lesson (the GT6ChemicalRowsPourTest red): the residue
	 * collided with the NEXT init() whose class name sorts before jade's, so the class
	 * hands the registry back clean via {@link GT6RecipeMaps#reset}.
	 */
	@org.junit.jupiter.api.AfterAll
	static void handBackACleanRegistry() {
		gregtech6.recipes.GT6RecipeMaps.reset();
	}

	private static TileEntityBasicMachine makeMachine() {
		return sHolder.type.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static TestMultiMachine makeMultiMachine() {
		return sMultiHolder.type.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static CompoundTag syncOf(TileEntityBasicMachine aMachine) {
		CompoundTag tTag = new CompoundTag();
		GT6MachineProvider.appendMachineData(tTag, aMachine);
		return tTag;
	}

	@Test
	public void energyTypeRidesTheTagNextToTheLegacyKeys() {
		TileEntityBasicMachine tMachine = makeMachine();
		tMachine.mEnergyTypeAccepted = TD.Energy.RU; // the Shredder row (GTMachines.java:201)
		tMachine.mEnergy = 32;
		CompoundTag tTag = syncOf(tMachine);
		// the p27 contract: the TRUE type short code, not the "(RU/KU)"并列字面量
		assertEquals("RU", tTag.getString(GT6MachineProvider.KEY_ENERGY_TYPE));
		// the legacy keys stay verbatim (no sync-seam regression)
		assertEquals(32L, tTag.getLong(GT6MachineProvider.KEY_ENERGY));
		assertEquals(Tag.TAG_STRING, tTag.getTagType(GT6MachineProvider.KEY_ENERGY_TYPE),
				"the type key rides the string face");
		assertTrue(tTag.contains(GT6MachineProvider.KEY_PROGRESS));
		assertTrue(tTag.contains(GT6MachineProvider.KEY_INPUT_MAX));
	}

	@Test
	public void theSuccessfulDeadKeyIsNeverWritten() {
		// the r8 payload minimization: KEY_SUCCESSFUL was write-only (no reader ever) — the
		// constant is DELETED from the provider; the pin is the literal key name (a
		// reintroduction would put "GT6Successful" back on the wire and fail here).
		TileEntityBasicMachine tMachine = makeMachine();
		tMachine.mSuccessful = true;
		assertFalse(syncOf(tMachine).contains("GT6Successful"));
	}

	@Test
	public void theMultiblockArmSpeaksTheMachineTagShape() {
		// the r8 blind-spot fix: the multiblock controllers' fields are isomorphic
		// (TileEntityBase10MultiBlockMachine.java:166-175) — the same seam, the same keys,
		// so the large turbine/fusion/coke oven/distillation faces grow the progress bar.
		TestMultiMachine tMulti = makeMultiMachine();
		tMulti.mProgress = 700;
		tMulti.mMaxProgress = 2800;
		tMulti.mActive = true;
		tMulti.mRunning = true;
		tMulti.mEnergy = 512;
		tMulti.mEnergyTypeAccepted = TD.Energy.EU;
		CompoundTag tTag = new CompoundTag();
		GT6MachineProvider.appendMachineData(tTag, tMulti);
		assertEquals(700L, tTag.getLong(GT6MachineProvider.KEY_PROGRESS));
		assertEquals(2800L, tTag.getLong(GT6MachineProvider.KEY_MAX_PROGRESS));
		assertTrue(tTag.getBoolean(GT6MachineProvider.KEY_ACTIVE));
		assertTrue(tTag.getBoolean(GT6MachineProvider.KEY_RUNNING));
		assertEquals(512L, tTag.getLong(GT6MachineProvider.KEY_ENERGY));
		assertEquals("EU", tTag.getString(GT6MachineProvider.KEY_ENERGY_TYPE));
		// mParallel is registration-period static data — it rides the tooltip face, never the Jade tag
		assertFalse(tTag.contains("GT6Parallel"));
		assertFalse(tTag.contains("GT6Successful"), "the dead key stays dead on the multiblock arm too");
		// the bar text for the synced face is the keyed three-slot translatable
		TranslatableContents tContents = (TranslatableContents) GT6MachineProvider.progressLine(
				tTag.getLong(GT6MachineProvider.KEY_PROGRESS), tTag.getLong(GT6MachineProvider.KEY_MAX_PROGRESS)).getContents();
		assertEquals(GT6MachineProvider.LANG_PROGRESS_SECONDS, tContents.getKey());
	}

	@Test
	public void theP7TrioCarriersResolveTheirCensusCodes() {
		// research.p27-shredder-wiring-census: Shredder/Lathe RU (GTMachines.java:201/:216),
		// Crusher KU (:209) — the BE field IS the single truth the provider now relays.
		assertEquals("RU", GT6MachineProvider.energyTypeShortCode(TD.Energy.RU));
		assertEquals("KU", GT6MachineProvider.energyTypeShortCode(TD.Energy.KU));
		// the W1/HU families (dryer/oven/extruder HU, canner EU) and the :254 field default
		assertEquals("HU", GT6MachineProvider.energyTypeShortCode(TD.Energy.HU));
		assertEquals("EU", GT6MachineProvider.energyTypeShortCode(TD.Energy.EU));
		assertEquals("TU", GT6MachineProvider.energyTypeShortCode(TD.Energy.TU));
	}

	@Test
	public void everyCanonicalEnergyCarrierHasADisplayCode() {
		// the full TD.Energy roster the root registers (TD.java:81-185) — a display code each.
		assertEquals("CU", GT6MachineProvider.energyTypeShortCode(TD.Energy.CU));
		assertEquals("LU", GT6MachineProvider.energyTypeShortCode(TD.Energy.LU));
		assertEquals("MU", GT6MachineProvider.energyTypeShortCode(TD.Energy.MU));
		assertEquals("NU", GT6MachineProvider.energyTypeShortCode(TD.Energy.NU));
		assertEquals("QU", GT6MachineProvider.energyTypeShortCode(TD.Energy.QU));
		assertEquals("RF", GT6MachineProvider.energyTypeShortCode(TD.Energy.RF));
		assertEquals("MJ", GT6MachineProvider.energyTypeShortCode(TD.Energy.MJ));
		assertEquals("Steam", GT6MachineProvider.energyTypeShortCode(TD.Energy.STEAM));
		assertEquals("AU", GT6MachineProvider.energyTypeShortCode(TD.Energy.AU));
		assertEquals("Ordo", GT6MachineProvider.energyTypeShortCode(TD.Energy.VIS_ORDO));
		assertEquals("Perditio", GT6MachineProvider.energyTypeShortCode(TD.Energy.VIS_PERDITIO));
	}

	@Test
	public void unknownCarriersFallBackToTheFoldedNameStrip() {
		// a TagData outside the map: the "ENERGY." prefix strips off the folded mName —
		// uppercase, parameter-free (the port's fold), never the raw internal form.
		gregapi.code.TagData tExotic = gregapi.code.TagData.createTagData("ENERGY.EXOTIC");
		assertEquals("EXOTIC", GT6MachineProvider.energyTypeShortCode(tExotic));
		assertEquals("", GT6MachineProvider.energyTypeShortCode(null), "null carrier answers empty, never throws");
	}

	@Test
	public void progressBarTextCarriesThePercentSlot() {
		// the r8 uniform three-slot bar template X / Y (Z%) — the percent rides the TEXT
		// (Jade paints the component inside the bar; there is no native percent overlay).
		// seconds face: the >=20t fold with %.1f-preformatted slots (Locale.ROOT pinned)
		TranslatableContents tSeconds = (TranslatableContents) GT6MachineProvider.progressLine(40, 400).getContents();
		assertEquals(GT6MachineProvider.LANG_PROGRESS_SECONDS, tSeconds.getKey());
		assertEquals(3, tSeconds.getArgs().length);
		assertEquals("2.0", tSeconds.getArgs()[0]);
		assertEquals("20.0", tSeconds.getArgs()[1]);
		assertEquals(10L, tSeconds.getArgs()[2]);
		// ticks face below the fold: the raw longs + the percent
		TranslatableContents tTicks = (TranslatableContents) GT6MachineProvider.progressLine(7, 19).getContents();
		assertEquals(GT6MachineProvider.LANG_PROGRESS_TICKS, tTicks.getKey());
		assertEquals(7L, tTicks.getArgs()[0]);
		assertEquals(19L, tTicks.getArgs()[1]);
		assertEquals(36L, tTicks.getArgs()[2], "700/19 floors at 36 — integer percent, never rounded up");
	}

	@Test
	public void energyAndInputRowsAreTheTextForms() {
		// the energy buffer row: "Stored: %s %s" (amount + short code) — the TEXT form, no
		// bar (no true capacity is ported — the capacitor half is missing, the design ruling).
		TranslatableContents tEnergy = (TranslatableContents) GT6MachineProvider.energyLine(1234, "RU").getContents();
		assertEquals(GT6MachineProvider.LANG_ENERGY, tEnergy.getKey());
		assertEquals(1234L, tEnergy.getArgs()[0]);
		assertEquals("RU", tEnergy.getArgs()[1]);
		// the input band: min/in/max + the type short code in the FOURTH slot
		TranslatableContents tInput = (TranslatableContents) GT6MachineProvider.inputLine(16, 32, 64, "KU").getContents();
		assertEquals(GT6MachineProvider.LANG_INPUT, tInput.getKey());
		assertEquals(4, tInput.getArgs().length);
		assertEquals(16L, tInput.getArgs()[0]);
		assertEquals(32L, tInput.getArgs()[1]);
		assertEquals(64L, tInput.getArgs()[2]);
		assertEquals("KU", tInput.getArgs()[3]);
	}

	@Test
	public void statusLineCarriesTheActiveSemantics() {
		// the r8 lift: the green/red meaning moved OFF the bar coloring onto its own row —
		// Active (mActive&&mRunner conjunction at the call site) GREEN, Inactive RED.
		Component tActive = GT6JadeRows.statusLine(true);
		assertEquals(GT6JadeRows.LANG_STATUS_ACTIVE, ((TranslatableContents) tActive.getContents()).getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), tActive.getStyle().getColor());
		Component tInactive = GT6JadeRows.statusLine(false);
		assertEquals(GT6JadeRows.LANG_STATUS_INACTIVE, ((TranslatableContents) tInactive.getContents()).getKey());
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tInactive.getStyle().getColor());
	}

	@Test
	public void structureLineRidesTheRekeyedCommonFaces() {
		// the formed/incomplete pair survives, re-keyed onto gt6.jade.common.* (the wording
		// carried verbatim); the GREEN/RED styling is a single ternary in structureLine.
		Component tFormed = GT6JadeRows.structureLine(true);
		assertEquals(GT6JadeRows.LANG_STRUCTURE_FORMED, ((TranslatableContents) tFormed.getContents()).getKey());
		Component tIncomplete = GT6JadeRows.structureLine(false);
		assertEquals(GT6JadeRows.LANG_STRUCTURE_INCOMPLETE, ((TranslatableContents) tIncomplete.getContents()).getKey());
	}

	@Test
	public void malfunctionDisplacesTheRawError() {
		// the ERROR_MESSAGE only ever carries the tick-exception trap text (TicksAndSync
		// :223/228) — the panel shows the constant Malfunction face, never the raw string;
		// the raw text rides the SNEAK detail, truncated at 64 chars.
		Component tFace = GT6JadeRows.malfunctionLine();
		assertEquals(GT6JadeRows.LANG_MALFUNCTION, ((TranslatableContents) tFace.getContents()).getKey());
		assertEquals(0, ((TranslatableContents) tFace.getContents()).getArgs().length, "the constant face carries no slots");
		assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), tFace.getStyle().getColor());
		// the detail: a short raw passes through verbatim
		TranslatableContents tShort = (TranslatableContents) GT6JadeRows.malfunctionDetail("boom").getContents();
		assertEquals(GT6JadeRows.LANG_MALFUNCTION_DETAIL, tShort.getKey());
		assertEquals("boom", tShort.getArgs()[0]);
		// a long raw truncates at the MAX_DETAIL_CHARS boundary (no exception text flood)
		String tLong = "x".repeat(200);
		assertEquals(GT6JadeRows.MAX_DETAIL_CHARS,
				((TranslatableContents) GT6JadeRows.malfunctionDetail(tLong).getContents()).getArgs()[0].toString().length());
	}

	@Test
	public void ratioClampsLikeTheBarNeeds() {
		// the shared clamp (the retired GT6BoilerProvider.heatRatio semantics, now on rows)
		assertEquals(0.5F, GT6JadeRows.ratio(1, 2), 1e-6F);
		assertEquals(1.0F, GT6JadeRows.ratio(999999999, 640000), 1e-6F, "overhead clamps at full");
		assertEquals(0.0F, GT6JadeRows.ratio(-5, 640000), 1e-6F, "negative never paints progress");
		assertEquals(0.0F, GT6JadeRows.ratio(100, 0), 1e-6F, "a zero ceiling answers 0, never NaN");
	}
}
