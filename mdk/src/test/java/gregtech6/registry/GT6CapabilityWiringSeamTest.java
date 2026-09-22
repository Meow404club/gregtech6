package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The 1.20.1-side pin for {@link GT6CapabilityWiring} — the wiring class itself is a
 * 1.21.1-only file (its 1.20.1 leg is empty), so its references cannot be read from here.
 * What CAN be pinned is the other half of every reference: the registry rows the wiring
 * resolves through its holder fields. If a row is renamed, this test fails here first.
 * (RegistryObject.getId() on 1.20.1 / DeferredHolder.getId() on 21.1 both read the name
 * field set at construction — no registry binding needed, the GT6ToolsCreativeTabTest
 * javadoc precedent.)
 */
public class GT6CapabilityWiringSeamTest extends GTOfflineTestBase {

	/**
	 * The BlockEntityType handles the 21.1 wiring dereferences, per registry row
	 * (the ninth is the 2026-09-04 multiblock-part relay wiring — the pipe-hole family's
	 * capability face, GT6CapabilityWiring MULTIBLOCK_PART_BE row; the tenth and eleventh
	 * are the 2026-09-05 dryer/distillery registrations — the p14/p16 families' item +
	 * fluid faces, GT6CapabilityWiring DRYER_BE/DISTILLERY_BE rows; the twelfth is the
	 * 2026-09-09 item pipe — the p26-pipe-item family's item face, the ITEM_PIPE_BE row;
	 * the thirteenth and fourteenth are the 2026-09-09 storage hoppers — the
	 * p26-storage-hopper-family's item-only faces, the HOPPER_BE/QUEUE_HOPPER_BE rows;
	 * the fifteenth through twentieth are the 2026-09-10 static storage batch — the
	 * p26-storage-static-batch's item-only faces, the LOCKER_BE/DRAWER_QUAD_BE/SAFE_BE/
	 * SAFE_KEYLOCKED_BE/BOOKSHELF_BE/BOTTLECRATE_BE rows).
	 */
	@Test
	public void beWiringPathsPinnedToRegistryRows() {
		assertEquals("shredder", GTMachines.SHREDDER_BE.getId().getPath());
		assertEquals("crusher", GTMachines.CRUSHER_BE.getId().getPath());
		assertEquals("lathe", GTMachines.LATHE_BE.getId().getPath());
		assertEquals("dryer", GTMachines.DRYER_BE.getId().getPath());
		assertEquals("distillery", GTMachines.DISTILLERY_BE.getId().getPath());
		assertEquals("oven", GTMachines.OVEN_BE.getId().getPath());
		assertEquals("steam_engine", GTBlockEntities.STEAM_ENGINE_BE.getId().getPath());
		assertEquals("boiler_tank", GTBlockEntities.BOILER_TANK_BE.getId().getPath());
		assertEquals("multiblock_large_boiler", GTMultiBlocks.LARGE_BOILER_BE.getId().getPath());
		assertEquals("fluid_pipe", GTFluidPipes.FLUID_PIPE_BE.getId().getPath());
		assertEquals("multiblock_part", GTMultiBlocks.MULTIBLOCK_PART_BE.getId().getPath());
		assertEquals("item_pipe", GTItemPipes.ITEM_PIPE_BE.getId().getPath());
		// task p26-kitchen-pot-bowl — the kitchen family joins (the pot pair's shared BET
		// + the bowl BET, item + fluid faces both, the machine-family shape)
		assertEquals("bathing_pot", GT6Kitchen.BATHING_POT_BE.getId().getPath());
		assertEquals("mixing_bowl", GT6Kitchen.MIXING_BOWL_BE.getId().getPath());
		assertEquals("hopper", GTBlockEntities.HOPPER_BE.getId().getPath());
		assertEquals("queue_hopper", GTBlockEntities.QUEUE_HOPPER_BE.getId().getPath());
		// task p26-storage-static-batch — the six item-only storage faces
		assertEquals("locker", GTBlockEntities.LOCKER_BE.getId().getPath());
		assertEquals("drawer_quad", GTBlockEntities.DRAWER_QUAD_BE.getId().getPath());
		assertEquals("safe_mechanical", GTBlockEntities.SAFE_BE.getId().getPath());
		assertEquals("safe_keylocked", GTBlockEntities.SAFE_KEYLOCKED_BE.getId().getPath());
		assertEquals("bookshelf", GTBlockEntities.BOOKSHELF_BE.getId().getPath());
		assertEquals("bottlecrate", GTBlockEntities.BOTTLECRATE_BE.getId().getPath());
	}

	/**
	 * The kitchen family's 21.1 capability registration surface (task
	 * p26-kitchen-pot-bowl, the BASIC_MACHINE_FAMILY_FACES shape): every
	 * {@code *_BE} field GT6Kitchen declares must serve item + fluid — the manual family
	 * has the full IInventory exposure upstream and both tank banks gated by the :302/
	 * :310 doors, so a missing 21.1 row would blind hopper pushes AND tank IO while the
	 * forge leg's override hides the gap (the ADR-P15-4 guard form).
	 */
	@Test
	public void kitchenFamilyBetsAllDeclareTheirWiringSurface() throws Exception {
		Set<String> tLive = new LinkedHashSet<>();
		for (Field tField : GT6Kitchen.class.getDeclaredFields()) {
			if (!tField.getName().endsWith("_BE")) continue;
			Object tHolder = tField.get(null);
			assertTrue(tHolder != null, tField.getName() + " must hold its eagerly built registry handle");
			Object tId = tHolder.getClass().getMethod("getId").invoke(tHolder);
			String tPath = (String) tId.getClass().getMethod("getPath").invoke(tId);
			tLive.add(tPath);
		}
		assertEquals(new LinkedHashSet<>(java.util.List.of("bathing_pot", "mixing_bowl", "juicer")), tLive,
				"the kitchen BET census drifted — declare the new family's item + fluid rows in "
				+ "GT6CapabilityWiring.registerKitchenFaces in the same change");
	}

	/**
	 * Task p26-storage-static-batch: the six storage BETs must stay over the SAME valid
	 * block set as their GT6StaticStorages kind rows (the ADR-P3-1 one-type-many-blocks
	 * invariant — a block registered into a row but not its kind array would mount a
	 * mismatched BE). Pinned off the row table, not the blocks: the row census keeps the
	 * 28-block universe visible on this leg.
	 */
	@Test
	public void staticStorageRowsMatchTheKindCensus() {
		long tMetal = GT6StaticStorages.ROWS.stream().filter(r -> r.material() != null).count();
		assertEquals(8, tMetal, "the Bronze/Steel metal ladder: locker/drawer/safe pair x2");
		assertEquals(10, GT6StaticStorages.ROWS.stream().filter(r -> r.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOOKSHELF).count(),
				"the vanilla-planks bookshelf subset (the 300-ladder fold)");
		assertEquals(10, GT6StaticStorages.ROWS.stream().filter(r -> r.kind() == gregtech6.registry.GT6StaticStorages.Kind.BOTTLECRATE).count(),
				"the vanilla-planks bottlecrate subset");
		assertEquals(28, GT6StaticStorages.ROWS.size(), "8 metal + 10 bookshelf + 10 bottlecrate");
	}

	/**
	 * The TileEntityBasicMachine family's 21.1 capability registration surface, the
	 * ADR-P15-4 third-instance guard: every family BET serves item + fluid (the external
	 * hopper push arm AND the cross-machine fluid auto-IO arm both walk the level
	 * capability query, so a missing row is invisible to the machines themselves and only
	 * shows as a live IO delta), the oven the declared item-only exception. The wiring
	 * rows themselves live in the 21.1-only GT6CapabilityWiring — this leg cannot compile
	 * against them, so the table + the census below turn "a new GTMachines family without
	 * its wiring rows" into a red 1.20.1 gate (the only gate that can see the gap)
	 * instead of an RCON failure two phases later.
	 */
	private static final Map<String, String> BASIC_MACHINE_FAMILY_FACES;
	static {
		Map<String, String> tFaces = new LinkedHashMap<>();
		tFaces.put("shredder", "item+fluid");
		tFaces.put("crusher", "item+fluid");
		tFaces.put("lathe", "item+fluid");
		tFaces.put("dryer", "item+fluid");
		tFaces.put("distillery", "item+fluid");
		tFaces.put("canner", "item+fluid"); // task p24-canner-machine — the Canner ladder joins
		tFaces.put("sifter", "item+fluid"); // task p26-w1-sifter-compressor-wiremill — the W1 Kinetic trio joins
		tFaces.put("compressor", "item+fluid"); // task p26-w1-sifter-compressor-wiremill
		tFaces.put("wiremill", "item+fluid"); // task p26-w1-sifter-compressor-wiremill — zero fluid recipes, NOT a zero fluid face
		tFaces.put("press", "item+fluid"); // task p26-w1-press-extruder-molds — the Press ladder joins
		tFaces.put("extruder", "item+fluid"); // task p26-w1-press-extruder-molds — the Extruder ladder joins
		tFaces.put("rollingmill", "item+fluid"); // task p28-c-ulv-machine-ladder — the Rolling Mill family joins (the ULV rows of the five existing families ride their family BETs)
		tFaces.put("rollbender", "item+fluid"); // task p29-w1-kinetic-roll-ladder — the roll ladders join
		tFaces.put("rollformer", "item+fluid"); // task p29-w1-kinetic-roll-ladder
		tFaces.put("clustermill", "item+fluid"); // task p29-w1-kinetic-roll-ladder
		tFaces.put("buzzsaw", "item+fluid"); // task p29-w1-kinetic-process-ladder — the six process families join (the coolant/water/juice masks make the fluid faces load-bearing)
		tFaces.put("squeezer", "item+fluid");
		tFaces.put("centrifuge", "item+fluid");
		tFaces.put("sluice", "item+fluid");
		tFaces.put("sanding_machine", "item+fluid"); // zero-fluid MASKS stay a data-only face (the sander rows carry no tank keys)
		tFaces.put("pressure_washer", "item+fluid");
		tFaces.put("mixer", "item+fluid"); // task p29-w1-eu-hu-families — the seven eu-hu families join
		tFaces.put("electricmixer", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("electricloom", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("electricsifter", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("boxinator", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("unboxinator", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("fermenter", "item+fluid"); // task p29-w1-eu-hu-families
		tFaces.put("autocrafter", "item+fluid"); // task p29-w2-eu-special — the eu-special families join (zero tank keys: the 127 all-sides defaults face)
		tFaces.put("lightning", "item+fluid"); // task p29-w2-eu-special — the U|L / R|D tank masks the output-tank fallback consumes through
		tFaces.put("laminator", "item+fluid"); // task p29-w2-eu-special
		tFaces.put("polarizer", "item+fluid"); // task p29-w2-exotic-energy — the six exotic-energy families join (the MU/LU/CU type gate rides the BE, not this face)
		tFaces.put("magnetic_separator", "item+fluid"); // task p29-w2-exotic-energy
		tFaces.put("laser_engraver", "item+fluid"); // task p29-w2-exotic-energy
		tFaces.put("laser_welder", "item+fluid"); // task p29-w2-exotic-energy — the no-tank-out row keeps the fluid FACE (the seam-② hard constraint)
		tFaces.put("freezer", "item+fluid"); // task p29-w2-exotic-energy
		tFaces.put("massfab_small", "item+fluid"); // task p31-massfab — the small Massfab 5-ladder joins (the QU type gate rides the BE)
		tFaces.put("molecular_scanner", "item+fluid"); // task p32-qu-scanner-replicator — the scanner single joins (the QU type gate rides the BE; the zero-tank masks stay a data-only face)
		tFaces.put("replicator", "item+fluid"); // task p32-qu-scanner-replicator — the replicator three-rung joins (the QU type gate rides the BE; the matter fluids are the live input face)
		tFaces.put("cryo_mixer", "item+fluid"); // task p29-w2-exotic-energy
		tFaces.put("electrolyzer", "item+fluid"); // task p29-w2-eu-core-5tier — the five eu-core families join (the first 5-tier ladders)
		tFaces.put("injector", "item+fluid"); // task p29-w2-eu-core-5tier
		tFaces.put("printer", "item+fluid"); // task p29-w2-eu-core-5tier — the tank-out 127 default stays a data-only face
		tFaces.put("scannervisuals", "item+fluid"); // task p29-w2-eu-core-5tier — the zero-fluid masks stay a data-only face
		tFaces.put("slicer", "item+fluid"); // task p29-w2-eu-core-5tier — the zero-fluid masks stay a data-only face
		tFaces.put("steamcracker", "item+fluid"); // task p29-w2-hu-tu-piggyback — the seven hu-tu families join
		tFaces.put("catalyticcracker", "item+fluid"); // task p29-w2-hu-tu-piggyback
		tFaces.put("coagulator", "item+fluid"); // task p29-w2-hu-tu-piggyback — the FLUID-ONLY map stays a live item face (the zero-item RECIPE is not a zero-item FACE)
		tFaces.put("generifier", "item+fluid"); // task p29-w2-hu-tu-piggyback
		tFaces.put("bath", "item+fluid"); // task p29-w2-hu-tu-piggyback — the P26 in-catalog BATH map, reused
		tFaces.put("autoclave", "item+fluid"); // task p29-w2-hu-tu-piggyback
		tFaces.put("loom", "item+fluid"); // task p29-w2-hu-tu-piggyback — the W1 in-catalog LOOM map, the kinetic rung
		tFaces.put("smelter", "item+fluid"); // task p29-w3-heat-smelter — the two heat families join (the ice row's output is the fluid face)
		tFaces.put("melter", "item+fluid"); // task p29-w3-heat-smelter
		tFaces.put("roasting_oven", "item+fluid"); // task p29-w4-eu-bridge — the Roasting ladder joins (the Boudouard row's CO2 input + CO output are the fluid face)
		tFaces.put("bumblelyzer", "item+fluid"); // task p34-machines-bumblelyzer-crucible — the scan arm drinks the honey tank leg (the 10 L diluent charge)
		tFaces.put("crystallisationcrucible", "item+fluid"); // task p34-machines-bumblelyzer-crucible — the noble-gas + molten legs are the live input face (the boule is the item output)
		// the CONVERTER exception (task p29-w4-eu-bridge): the three EU->X bridge families
		// carry NO capability face at all — no inventory, no tanks, pure TD.Energy push
		// over the ITileEntityEnergy face (the electric dynamo family posture; they live
		// in GTMachines as BridgeRow BETs, hence the table rows with the "none" marker)
		tFaces.put("electric_heater", "none (GT-energy converter — no item/fluid face)");
		tFaces.put("electric_engine", "none (GT-energy converter — no item/fluid face)");
		tFaces.put("electric_motor", "none (GT-energy converter — no item/fluid face)");
		tFaces.put("oven", "item"); // the exception: the gated item handler alone
		tFaces.put("advanced_crafting_table", "item"); // task p24-act-machine — the second item-only face (zero fluid tanks)
		BASIC_MACHINE_FAMILY_FACES = Collections.unmodifiableMap(tFaces);
	}

	/**
	 * The family-surface census: every {@code *_BE} field GTMachines declares must have a
	 * row in {@link #BASIC_MACHINE_FAMILY_FACES}, and every row must answer to a live
	 * field, so the table cannot rot. A failure here means: declare the family's faces
	 * here AND register its rows in GT6CapabilityWiring.registerMachineBlockEntities in
	 * the same change (item + fluid, the oven item-only shape) — without the wiring rows
	 * the family is capability-blind on 1.21.1 (hopper pushes, fluid auto-IO) while the
	 * forge leg's BE override hides the gap (the p17-2111-machine-io root cause, third
	 * ADR-P15-4 instance). The holder reflection is leg-agnostic on purpose: RegistryObject
	 * on 1.20.1, DeferredHolder on 21.1, both expose getId() and both hand back a
	 * ResourceLocation.
	 */
	@Test
	public void basicMachineFamilyBetsAllDeclareTheirWiringSurface() throws Exception {
		Set<String> tLive = new LinkedHashSet<>();
		for (Field tField : GTMachines.class.getDeclaredFields()) {
			if (!tField.getName().endsWith("_BE")) continue;
			Object tHolder = tField.get(null);
			assertTrue(tHolder != null, tField.getName() + " must hold its eagerly built registry handle");
			Object tId = tHolder.getClass().getMethod("getId").invoke(tHolder);
			String tPath = (String) tId.getClass().getMethod("getPath").invoke(tId);
			assertTrue(BASIC_MACHINE_FAMILY_FACES.containsKey(tPath),
					"GTMachines." + tField.getName() + " (gt6:" + tPath + ") has no row in "
					+ "BASIC_MACHINE_FAMILY_FACES — declare its capability faces here AND register "
					+ "its item + fluid rows in GT6CapabilityWiring.registerMachineBlockEntities "
					+ "(the oven item-only shape is the declared exception), or the family is "
					+ "capability-blind on 1.21.1 while this leg cannot see the gap");
			tLive.add(tPath);
		}
		assertEquals(BASIC_MACHINE_FAMILY_FACES.keySet(), tLive,
				"the family-surface table and the live GTMachines *_BE rows must match exactly");
		assertEquals("item", BASIC_MACHINE_FAMILY_FACES.get("oven"),
				"the oven exception is item-only — widening it is a separate decision, not drift");
	}

	/**
	 * The 21.1 wiring wires FLUID_HANDLER_ITEM to every GTBarrelBlockItem via a registry
	 * CLASS scan (mirroring the Forge initCapabilities face — the capability rides the item
	 * class, not an id list); pin the family membership and its registration paths.
	 */
	@Test
	public void barrelItemFamilyCoversTheCarrierSeam() {
		assertEquals("barrel_wood", GTBarrels.BARREL_ITEM.getId().getPath());
		assertEquals("barrel_plastic", GTBarrels.BARREL_PLASTIC_ITEM.getId().getPath());
		assertEquals("barrel_metal", GTBarrels.BARREL_METAL_ITEM.getId().getPath());
		assertEquals("barrel_logistics", GTBarrels.BARREL_LOGISTICS_ITEM.getId().getPath());
		assertEquals(12, GTBarrels.METAL_DRUM_ITEMS.size(), "the p7 high-tier drum ladder");
		for (String tPath : GTBarrels.METAL_DRUM_ITEMS.keySet()) {
			assertTrue(tPath.startsWith("barrel_"), "drum row id " + tPath + " must stay in the barrel_* path family the wiring scan covers");
		}
	}

	/**
	 * Task p26-eu-bridge-outbound (tail-append): the FE battery fixture is a capability
	 * PROVIDER on the 21.1 leg — the registerFeBattery row hands its BET to
	 * Capabilities.EnergyStorage.BLOCK with a provider reading energyStorage(). On this
	 * (1.20.1) leg the same BE answers through its getCapability override and cannot see
	 * the 21.1-only wiring file, so the row's BET path is pinned here, the wiring-side
	 * half of the two-seam model (the machines-family census above, same reason).
	 */
	@Test
	public void feBatteryFixtureBETPathPinned() {
		assertEquals("fe_battery", GT6FeBatteries.FE_BATTERY_BE.getId().getPath());
		assertEquals("fe_battery", GT6FeBatteries.FE_BATTERY.getId().getPath(), "the BET and its block share the registry path");
	}
}
