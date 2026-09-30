/*
 * Offline tests for the machine paint tint value mapping: the pure tintARGB seam over the
 * PAINT model data (task paintable-tint-render) and the ROW MATERIAL fallback (task
 * machine-material-tint-fidelity). ModelData/ModelProperty are pure data classes (Guava
 * only), offline-testable per GTOfflineRenderTestBase; the world-side BlockColor lambda is
 * covered on its null-guard arms (a live Level+BE needs a running client — the RCON visual
 * chain is the optional live check, not a gate).
 *
 * <p>THE SEMANTICS OF "UNPAINTED" CHANGED with the fidelity card (the expected regression
 * face, declared in the task): unpainted machines now render their NBT_MATERIAL row colour
 * (upstream MultiTileEntityClassContainer.java:51 derives NBT_COLOR from fRGBaSolid) — the
 * former white-default assertions here pin the MATERIAL-LESS arm only (MT.NULL rows,
 * vanilla states; the barrels LEFT that set in task tank-render-tint, their unpainted
 * face renders the row colour through the GTBarrelBlock carrier).
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraftforge.client.model.data.ModelData;

import gregtech6.registry.GTMaterialItems;

class GTMachinePaintTintTest extends GTOfflineRenderTestBase {

	/** A painted colour as card_A stores it (0xRRGGBB, the direct-storage ruling). */
	private static final int PAINT_RED = 0xFF0000;

	@BeforeAll
	static void bootMaterials() {
		// the row materials resolve through MT.init (the GTWireTintTest shape)
		GTMaterialItems.initMaterials();
	}

	/** A painted colour as card_A stores it (0xRRGGBB, the direct-storage ruling). */
	private static ModelData paintedData(int aRGB) {
		return GTModelProperties.derive(ModelData.EMPTY)
				.with(GTModelProperties.PAINT, Integer.valueOf(aRGB))
				.build();
	}

	/** Acceptance: a present PAINT property returns the paint as opaque ARGB (index 0) — the spray override wins over the row material. */
	@Test
	void paintedSnapshotTintsWithTheStoredColour() {
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 0),
				"red paint 0xFF0000 -> ARGB 0xFFFF0000");
		assertEquals(0xFF202020, GTMachinePaintTint.tintARGB(paintedData(0x202020), null, 0),
				"the CS DYE_Black row value tints dark gray");
		// the spray-paint override: even a material row renders the PAINT value
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), gregapi.data.MT.Cu, 0),
				"painted wins over the row material (upstream Paintable:85 override)");
	}

	/**
	 * Acceptance (the fidelity card): an UNPAINTED machine tints with its row material —
	 * the registration derivation of MultiTileEntityClassContainer.java:51
	 * (getRGBInt over fRGBaSolid, OreDictMaterial.java:111). The representative pair of
	 * research.p27-machine-tint-reresearch: Cu orange-red, Steel gray-white.
	 */
	@Test
	void unpaintedMachineTintsWithTheRowMaterial() {
		assertEquals(0xFFFF825A, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Cu, 0),
				"the Cu row renders orange-red 255,130,90 (the copper-machine observation)");
		assertEquals(0xFF828282, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Steel, 0),
				"the Steel row renders gray-white 130,130,130 (the steel-machine observation)");
		// the encoding derivation — every arm rides fRGBaSolid exactly
		for (gregapi.oredict.OreDictMaterial tMat : java.util.List.of(gregapi.data.MT.Cu, gregapi.data.MT.Steel, gregapi.data.MT.Invar)) {
			int tColor = GTMachinePaintTint.tintARGB(null, tMat, 0);
			assertEquals(0xFF000000, tColor & 0xFF000000, tMat.mNameInternal + " binds full alpha");
			assertEquals(tMat.fRGBaSolid[0], (tColor >> 16) & 0xFF, tMat.mNameInternal + " R");
			assertEquals(tMat.fRGBaSolid[1], (tColor >> 8) & 0xFF, tMat.mNameInternal + " G");
			assertEquals(tMat.fRGBaSolid[2], tColor & 0xFF, tMat.mNameInternal + " B");
		}
		// the null-SNAPSHOT arm carries the material too (the item-half read shape)
		assertEquals(0xFFFF825A, GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Cu, 0),
				"a null snapshot is the unpainted arm, not the no-tint arm");
	}

	/**
	 * The material-LESS arm keeps the white identity (the P21 contract): null material AND
	 * MT.NULL rows stay the vanilla -1 no-tint sentinel — the laser-style negative
	 * assertion of the task card. Task tank-render-tint: the barrels LEFT the material-less
	 * set (their unpainted face resolves the row colour through the GTBarrelBlock carrier),
	 * the white identity stays the MT.NULL / vanilla-state fallback only.
	 */
	@Test
	void materialLessArmsStayTheWhiteNoTintIdentity() {
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 0),
				"white 0xFFFFFF bound full-alpha = 0xFFFFFFFF (the material-less fallback)");
		assertEquals(0xFFFFFFFF, GTMachinePaintTint.tintARGB(null, null, 0),
				"the null-snapshot null-material guard is white too");
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 0),
				"0xFFFFFFFF == -1 — white and the no-tint sentinel are the same int");
		// MT.NULL rows: the material resolves white → the identity, never a colour
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.NULL, 0),
				"an MT.NULL material row stays the no-tint identity (the laser-style rows)");
	}
	/** Acceptance: a non-zero tint index is never tinted, painted or not. */
	@Test
	void nonZeroTintIndexIsNeverTinted() {
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 1));
		assertEquals(-1, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED), null, 3));
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, null, 1));
		assertEquals(-1, GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Cu, 1),
				"the row material never leaks onto an overlay index");
	}

	/**
	 * Task c3-kitchen-tint-shape: the kitchen carriers ride the combined dispatch —
	 * the tintindex-0 faces resolve the carrier material (the #7 reservation closing), the
	 * value math being the already-pinned fRGBaSolid derivation. The census blocks replay
	 * the registration payloads (the material column is what this pins).
	 */
	@Test
	void kitchenFamilyRidesTheCombinedDispatch() {
		assertSame(gregapi.data.MT.WoodTreated, GTMachinePaintTint.tintMaterialOf(kitchenBlock(gregapi.data.MT.WoodTreated)),
				"the wood pot tints the WoodTreated carrier colour");
		assertSame(gregapi.data.MT.StainlessSteel, GTMachinePaintTint.tintMaterialOf(kitchenBlock(gregapi.data.MT.StainlessSteel)),
				"the steel pot tints the StainlessSteel row colour");
		assertSame(gregapi.data.MT.Ceramic, GTMachinePaintTint.tintMaterialOf(kitchenBlock(gregapi.data.MT.Ceramic)),
				"the bowl/juicer tint the Ceramic row colour");
		// and the resolved material colours through the single decision site
		assertEquals(GTMachinePaintTint.tintARGB(null, gregapi.data.MT.StainlessSteel, 0),
				GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(kitchenBlock(gregapi.data.MT.StainlessSteel)), 0),
				"the steel pot's tint value is the fRGBaSolid derivation");
	}

	/** A kitchen carrier for the dispatch pin (properties irrelevant to the material gate). */
	private static gregtech6.block.tools.GTKitchenBlock kitchenBlock(gregapi.oredict.OreDictMaterial aMaterial) {
		// Block.<init> creates its intrusive holder past the bootstrap freeze — carry our own
		// write window (the GTWireBlockUseLockTest.block()/GT6CFoamFamilyTest shape, no
		// re-freeze): the class previously rode an earlier-alphabetical class's open window,
		// which fork partitioning (maxParallelForks, task maint-ci-perf) can't guarantee.
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		return new gregtech6.block.tools.GTKitchenBlock(8000, () -> aMaterial,
				gregtech6.block.tools.GTKitchenBlock.SHAPE_TUB, () -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	/**
	 * Task issue8-residual: the #8 stragglers ride the combined dispatch — the tank valve
	 * controller through the controller gate (the row's NBT_MATERIAL rides the
	 * {@code GTMultiBlockControllerBlock} carrier), the crucible wall through the part gate
	 * (the dedicated {@code GTCrucibleWallBlock} material-carrier ctor), and the
	 * material-less wall stays the null gate (the white identity).
	 */
	@Test
	void tankValveAndCrucibleWallRideTheCombinedDispatch() {
		unfreezeBlockRegistry();
		gregtech6.registry.GT6Tanks.TankValveRow tRow = gregtech6.registry.GT6Tanks.ROWS.stream()
				.filter(r -> r.path().equals("tank_small_tungstensteel")).findFirst().orElseThrow();
		assertSame(gregapi.data.MT.TungstenSteel, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.multiblock.GTTankValveBlock(tRow, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the tungstensteel valve tints the row material through the controller gate");
		assertSame(gregapi.data.MT.Steel, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.multiblock.GTCrucibleWallBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), () -> gregapi.data.MT.Steel)),
				"the steel crucible wall tints the carried material through the part gate");
		assertNull(GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.multiblock.GTCrucibleWallBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the material-less wall keeps the null gate (the white identity)");
	}

	/** The registry write window for direct block construction (the kitchenBlock helper shape). */
	private static void unfreezeBlockRegistry() {
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	/**
	 * Task tank-render-tint: the barrel family rides the combined dispatch through the new
	 * {@code GTBarrelBlock.materialOf} carrier — the four standalone rows' upstream
	 * NBT_MATERIAL columns (Loader :2140 WoodTreated, :2150 ANY.Plastic, :2151 MT.Bronze,
	 * :2171 ANY.W), one high-tier drum row's MetalDrumRow column, the steel valve arm
	 * through the controller gate (the tungstensteel pin above), and the PAINT override
	 * still beating the row colour on a barrel carrier (the P23 spray-paint face). The
	 * former P23 white identity retires to the material-less fallback only.
	 */
	@Test
	void barrelFamilyRidesTheCombinedDispatch() {
		unfreezeBlockRegistry();
		gregtech6.block.tank.GTBarrelBlock tWood = barrelBlock(() -> gregapi.data.MT.WoodTreated);
		gregtech6.block.tank.GTBarrelBlock tPlastic = barrelBlock(() -> gregapi.data.ANY.Plastic);
		gregtech6.block.tank.GTBarrelBlock tBronze = barrelBlock(() -> gregapi.data.MT.Bronze);
		gregtech6.block.tank.GTBarrelBlock tLogistics = barrelBlock(() -> gregapi.data.ANY.W);
		assertSame(gregapi.data.MT.WoodTreated, GTMachinePaintTint.tintMaterialOf(tWood),
				"the wood barrel tints the :2140 WoodTreated row (not the unported Cheap-row ANY.Wood)");
		assertSame(gregapi.data.ANY.Plastic, GTMachinePaintTint.tintMaterialOf(tPlastic),
				"the plastic canister tints the :2150 ANY.Plastic row");
		assertSame(gregapi.data.MT.Bronze, GTMachinePaintTint.tintMaterialOf(tBronze),
				"the bronze drum tints the :2151 row");
		assertSame(gregapi.data.ANY.W, GTMachinePaintTint.tintMaterialOf(tLogistics),
				"the logistics tank tints the :2171 ANY.W row");
		// a high-tier drum rides its MetalDrumRow column (the :2159-2170 ladder)
		gregtech6.registry.GTBarrels.MetalDrumRow tDrumRow = gregtech6.registry.GTBarrels.HIGH_TIER_METAL_DRUMS.stream()
				.filter(r -> r.path().equals("barrel_tungstensteel")).findFirst().orElseThrow();
		assertSame(gregapi.data.MT.TungstenSteel, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.tank.GTBarrelBlock(tDrumRow.capacityL(), tDrumRow.meltingPointK(), true,
								() -> null, tDrumRow.material(),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the tungstensteel drum tints its MetalDrumRow column");
		// the row colours resolve through the single decision site (the fRGBaSolid derivation)
		assertEquals(GTMachinePaintTint.tintARGB(null, gregapi.data.MT.WoodTreated, 0),
				GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tWood), 0),
				"the wood barrel's tint value is the fRGBaSolid derivation");
		assertEquals(GTMachinePaintTint.tintARGB(null, gregapi.data.ANY.W, 0),
				GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tLogistics), 0),
				"the logistics tank's tint value is the fRGBaSolid derivation");
		// the spray-paint override wins over the row colour (upstream Paintable:85)
		assertEquals(0xFFFF0000, GTMachinePaintTint.tintARGB(paintedData(PAINT_RED),
				GTMachinePaintTint.tintMaterialOf(tLogistics), 0), "painted wins over the barrel row colour");
		// the pairwise-distinct regression killer (the all-gray lesson): wood-brown vs steel-gray vs ANY.W
		int tWoodTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tWood), 0) & 0xFFFFFF;
		int tBronzeTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tBronze), 0) & 0xFFFFFF;
		int tLogisticsTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tLogistics), 0) & 0xFFFFFF;
		assertTrue(tWoodTint != tBronzeTint && tBronzeTint != tLogisticsTint && tWoodTint != tLogisticsTint,
				"the barrel row colours are pairwise distinct");
	}

	/** A bare barrel carrier for the dispatch pin (properties irrelevant to the material gate). */
	private static gregtech6.block.tank.GTBarrelBlock barrelBlock(java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		return new gregtech6.block.tank.GTBarrelBlock(16000, 340, false, () -> null, aMaterial,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	/**
	 * Task beehive-tint (issue #15): the Bumbliary pair rides the combined dispatch —
	 * the row NBT_MATERIAL of the upstream pair (ANY.Wood :2222; MT.StainlessSteel :2223,
	 * 200,200,220), the material-less hive block staying the null gate (its worldgen family
	 * colour rides the BE PAINT model data, not a material). The seam resolves MT.Wood
	 * DIRECTLY (the row's stats source; pre debt-anywood-flip it coincided with ANY.Wood's
	 * then-inverted looks 100,50,0 — upstream ANY.Wood looks is now Spruce 102,79,47, so
	 * the port body colour carries a recorded G+29/B+47 deviation vs the upstream alias).
	 */
	@Test
	void bumbliaryPairRidesTheCombinedDispatch() {
		unfreezeBlockRegistry();
		gregtech6.tileentity.bees.GT6BumbliaryBlock tPrimary = new gregtech6.tileentity.bees.GT6BumbliaryBlock(
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), false);
		gregtech6.tileentity.bees.GT6BumbliaryBlock tAdvanced = new gregtech6.tileentity.bees.GT6BumbliaryBlock(
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(), true);
		assertSame(gregapi.data.MT.Wood, GTMachinePaintTint.tintMaterialOf(tPrimary),
				"the Bumbliary resolves the MT.Wood representative of the ANY.Wood row, :2222");
		assertSame(gregapi.data.MT.StainlessSteel, GTMachinePaintTint.tintMaterialOf(tAdvanced),
				"the Advanced Bumbliary resolves the StainlessSteel row (:2223)");
		assertNull(GTMachinePaintTint.tintMaterialOf(
				new gregtech6.tileentity.bees.GT6BumbleHiveBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the hive block stays material-less (the family colour rides PAINT, not a material)");
		// the pinned values — the MT.Wood-direct seam derivation over fRGBaSolid
		// (MT.Wood 100,50,0 = 0x643200; StainlessSteel 200,200,220 = 0xC8C8DC)
		assertEquals(0xFF643200, GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tPrimary), 0),
				"the Bumbliary body tints wood-brown 100,50,0");
		assertEquals(0xFFC8C8DC, GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tAdvanced), 0),
				"the Advanced Bumbliary body tints steel-gray 200,200,220");
		// the encoding derivation — both rows ride fRGBaSolid exactly (the Cu/Steel pin shape)
		for (gregapi.oredict.OreDictMaterial tMat : java.util.List.of(gregapi.data.MT.Wood, gregapi.data.MT.StainlessSteel)) {
			int tColor = GTMachinePaintTint.tintARGB(null, tMat, 0);
			assertEquals(tMat.fRGBaSolid[0], (tColor >> 16) & 255, tMat.mNameInternal + " R");
			assertEquals(tMat.fRGBaSolid[1], (tColor >> 8) & 255, tMat.mNameInternal + " G");
			assertEquals(tMat.fRGBaSolid[2], tColor & 255, tMat.mNameInternal + " B");
		}
	}

	/** The BlockColor lambda's guard arms (null level/pos and a non-zero index return no tint). */
	@Test
	void blockColorLambdaGuardArms() {
		assertEquals(-1, GTMachinePaintTint.blockColor().getColor(null, null, null, 1),
				"a non-zero index short-circuits before any world access");
		assertEquals(-1, GTMachinePaintTint.blockColor().getColor(null, null, null, 0),
				"the null level/pos arm is the no-tint sentinel (== full-alpha white)");
	}

	/**
	 * Task tex-pipe-textures: the pipe connector carriers ride the combined dispatch —
	 * the fluid family through the MT.Wood gate (the addFluidPipes 26000 NBT_MATERIAL row,
	 * Loader :1846), the item family through its loader line's MT argument
	 * (MultiTileEntityPipeItem :76-82 registers NBT_MATERIAL + NBT_COLOR =
	 * getRGBInt(fRGBaSolid)), and the logistics wire the null gate on purpose (its
	 * NBT_MATERIAL is MT.NULL, Loader :1819 — the white identity, not a colour).
	 */
	@Test
	void pipeCarriersRideTheCombinedDispatch() {
		unfreezeBlockRegistry();
		// task fluid-pipe-matrix: the fluid rows carry PER-ROW materials now (the loader
		// MT.* column, :1846-1885) — the wood row keeps the W1 MT.Wood pin, a metal row
		// proves the dispatch leaves the W1 constant behind
		assertSame(gregapi.data.MT.Wood, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTFluidPipeBlock(gregtech6.registry.GTFluidPipes.rowByPath("wood_fluid_pipe_small"),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the wood rows ride the MT.Wood row");
		assertSame(gregapi.data.MT.Steel, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTFluidPipeBlock(gregtech6.registry.GTFluidPipes.rowByPath("steel_fluid_pipe_medium"),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the steel row carries its own material (the per-row dispatch)");
		assertSame(gregapi.data.MT.Brass, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTItemPipeBlock(gregtech6.registry.GTItemPipes.ROWS.get(0),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"row 0 = the brass medium pipe");
		assertSame(gregapi.data.MT.Constantan, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTItemPipeBlock(gregtech6.registry.GTItemPipes.ROWS.get(6),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"row 6 = the constantan medium pipe (material-major, 6 variants each)");
		assertSame(gregapi.data.MT.CobaltBrass, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTItemPipeBlock(gregtech6.registry.GTItemPipes.ROWS.get(12),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"row 12 = the cobalt-brass medium pipe");
		assertNull(GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.logistics.GTLogisticsWireBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())),
				"the logistics wire keeps the null gate (upstream MT.NULL)");
		// the brass-family values ride fRGBaSolid and stay pairwise distinct (the all-gray
		// regression killer, the #18 lesson); the wood pin = the 0x643200 body
		int tBrass = GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Brass, 0);
		int tConstantan = GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Constantan, 0);
		int tCobaltBrass = GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.CobaltBrass, 0);
		assertTrue(tBrass != tConstantan && tConstantan != tCobaltBrass && tBrass != tCobaltBrass,
				"the brass family rows are pairwise distinct");
		assertEquals(GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Wood, 0),
				GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(
						new gregtech6.block.pipe.GTFluidPipeBlock(gregtech6.registry.GTFluidPipes.rowByPath("wood_fluid_pipe_medium"),
								net.minecraft.world.level.block.state.BlockBehaviour.Properties.of())), 0),
				"the medium wood tier colours through the same seam");
	}

	/**
	 * Task render-embeddium-tint: the tint BAKED into the baked-quad vertex data —
	 * per-channel {@code (colour * tint + 255) >> 8} over the COLOR slot (stride 8, slot
	 * 3) of all four vertices, every other slot byte-identical. The retinted copies carry
	 * tintIndex -1 (set by the wrapper), so the runtime BlockColor can never double-multiply.
	 *
	 * <p>ISSUE #14 pin: the COLOR slot int stores its channels ABGR ({@code A<<24|B<<16|G<<8|R}
	 * — vanilla putBulkData reads bytes 12/13/14 = R/G/B, QuadTransformers.toABGR is the
	 * ecosystem's own converter), while the TINT is ARGB. The old pin asserted the raw ARGB
	 * int — the swapped convention itself, structurally unable to see the bug; these assert
	 * the byte-order-correct product, channel by channel off the slot layout.
	 */
	@Test
	void retintVerticesMultiplyTheBakedColours() {
		// one quad = 4 vertices x stride 8; vertex colours baked white (0xFFFFFFFF)
		int[] tVertices = new int[32];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		tVertices[0] = 123; // a non-colour slot keeps its value
		tVertices[31] = 456;
		int[] tOut = GTMachineTintModel.retintVertices(tVertices, 0xFFD2823C);
		assertEquals(32, tOut.length, "the vertex data length is preserved");
		assertEquals(123, tOut[0], "slot 0 untouched");
		assertEquals(456, tOut[31], "slot 31 untouched");
		for (int v = 0; v < 4; v++) {
			int tColour = tOut[v * 8 + 3];
			// the tint ARGB 0xFFD2823C = A255 R210 G130 B60; the slot is ABGR, so
			// R lives at bits 7-0, G at 15-8, B at 23-16: R=210, G=130, B=60, A=255
			assertEquals(210, tColour & 255, "vertex " + v + " R (ABGR slot 0)");
			assertEquals(130, (tColour >> 8) & 255, "vertex " + v + " G");
			assertEquals(60, (tColour >> 16) & 255, "vertex " + v + " B (ABGR slot 2)");
			assertEquals(255, (tColour >> 24) & 255, "vertex " + v + " A");
			// the warm pin: a warm tint keeps R > B in the slot layout (copper stays copper)
			assertTrue((tColour & 255) > ((tColour >> 16) & 255), "vertex " + v + " R > B (the #14 hue pin)");
		}
		// a mid-gray texture pixel half-tints: (128 * 210 + 255) >> 8 = 105-ish per channel
		int[] tMid = new int[32];
		java.util.Arrays.fill(tMid, 0xFF808080);
		int[] tMidOut = GTMachineTintModel.retintVertices(tMid, 0xFFD2823C);
		int tMidColour = tMidOut[3];
		assertEquals(105, tMidColour & 255, "mid-gray R (128 -> 105)");
		assertEquals(65, (tMidColour >> 8) & 255, "mid-gray G (128 -> 65)");
		assertEquals(30, (tMidColour >> 16) & 255, "mid-gray B (128 -> 30)");
	}
}
