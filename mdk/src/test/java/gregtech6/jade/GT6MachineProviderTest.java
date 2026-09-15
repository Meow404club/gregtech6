package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * Offline gate for task p27-machine-energy-display-fix acceptance ②: the energy-type short
 * code seam — {@code appendServerData} (the BlockAccessor wrapper, live-only) delegates the
 * machine-family write to {@link GT6MachineProvider#appendMachineData}, so the tag contract
 * is pinned here: {@code KEY_ENERGY_TYPE} carries the accepted-energy carrier
 * ({@code mEnergyTypeAccepted}, TileEntityBasicMachine.java:254) as its display short code,
 * next to the pre-existing energy/input keys (no key regressions).
 *
 * <p>The short codes themselves pin the {@code energyTypeShortCode} map: the P7 trio faces
 * (Shredder RU / Crusher KU) land the census verdicts of research.p27-shredder-wiring-census
 * into the tooltip face — "Energy: N (RU)" instead of the hardcoded "(RU/KU)" literal
 * (the old GT6MachineProvider.java:133 defect). The tooltip String.format itself stays
 * live-only (ITooltip needs a client element helper — the GT6FluidProviderTest posture).
 */
public class GT6MachineProviderTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityTypeHolder sHolder;

	/** Mutable BET holder (the GT6FluidProviderTest.machineType shape — the factory needs the type it is creating). */
	static final class BlockEntityTypeHolder {
		net.minecraft.world.level.block.entity.BlockEntityType<TileEntityBasicMachine> type;
	}

	@BeforeAll
	static void fixture() {
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
	}

	/**
	 * The fixture map lands in RecipeMap.RECIPE_MAPS and STAYS after the class — the
	 * p29-w4-f1-chemicals full-suite lesson (the GT6ChemicalRowsPourTest red): the residue
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
		assertEquals(net.minecraft.nbt.Tag.TAG_STRING, tTag.getTagType(GT6MachineProvider.KEY_ENERGY_TYPE),
				"the type key rides the string face");
		assertTrue(tTag.contains(GT6MachineProvider.KEY_PROGRESS));
		assertTrue(tTag.contains(GT6MachineProvider.KEY_INPUT_MAX));
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
}
