/*
 * The fluid-pipe matrix census (task fluid-pipe-matrix, research.boiler-gauges ④a):
 * the port-side 2-row placeholder expands to the FULL upstream matrix — 40 material
 * lines (Loader_MultiTileEntities.java:1846-1885) × 7 variants (MultiTileEntityPipeFluid
 * .java:92-98) = 280 registered rows, each carrying the loader line's behaviour columns
 * (stat/capacity multipliers, tank counts, diameters, stack sizes, the four proofs,
 * contact damage, flammability, recipe, blocking, the max temperature — explicit
 * 340/340/370/350 for Wood/WoodTreated/Plastic/Rubber, else mMeltingPoint * 1.25,
 * MultiTileEntityPipeFluid.java:84).
 *
 * <p>Coverage:
 * <ul>
 * <li>the 280-row census + the 40/7 axis + path format/uniqueness (the
 *     {@code <mat>_fluid_pipe_<size>} arch ruling, material first);</li>
 * <li>the metaId zero-diff face: the 40 loader bases verbatim in loader order, every
 *     row = base + the variant offset (the BoilerRow zero-diff table form);</li>
 * <li>the variant table verbatim (:92-98 — capacity {1,2,6,12,24,6,2}×, tanks
 *     {1,1,1,1,1,4,9}, diameters PX_P[4/6/8/12/16/16/16], stacks {64,64,32,16,16,16,16});</li>
 * <li>three representative rows pinned against the full upstream parameter set (one
 *     explicit-temp line, one melt-derived line, one multi-tank line — the behaviour
 *     columns the task card names);</li>
 * <li>the W1 wood pair stays in-registry, the small tier CORRECTED to the upstream
 *     :93 capacity 100 L (it carried the TINY 50 L under a small name);</li>
 * <li>the registration face: blocks/items/BET/tab walk all mirror the 280 rows, and the
 *     composed display rides the family-scoped lang keys (the template + the mat word).</li>
 * </ul>
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.pipe.GTFluidPipeBlock;

public class GT6FluidPipeMatrixCensusTest {

	/** The 40 loader metaId bases, verbatim loader line order (:1846-1885). */
	private static final int[] LOADER_BASES = {
			26000, 26020, 26080, 26060, 26520, 26100, 26680, 26340, 26040, 26120,
			26400, 26140, 26620, 26280, 26360, 26720, 26260, 26160, 26700, 26180,
			26660, 26760, 26200, 26780, 26740, 26220, 26240, 26440, 26560, 26380,
			26420, 26600, 26300, 26320, 26460, 26540, 26480, 26580, 26500, 26640
	};

	@BeforeAll
	static void bootMaterials() {
		// the vanilla bootstrap + material system (the maxTemperature melt-derived arm
		// reads MT.*.mMeltingPoint — the census shape of GT6PipeTextureDatagenTest)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline
		}
		try {
			// the registry write window for the two direct GTFluidPipeBlock constructions
			// (the GTNoOcclusionCensusTest boot bracket)
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

	// ------------------------------------------------------------------
	// the census
	// ------------------------------------------------------------------

	@Test
	public void theMatrixIs280Rows() {
		assertEquals(40, GTFluidPipes.MATERIALS.size(), "the loader line count (:1846-1885)");
		assertEquals(7, GTFluidPipes.VARIANTS.size(), "the addFluidPipes variant count (:92-98)");
		assertEquals(280, GTFluidPipes.ROWS.size(), "40 materials x 7 sizes");
		assertEquals(280, GTFluidPipes.BLOCKS_BY_PATH.size(), "the block register face");
		assertEquals(280, GTFluidPipes.ITEMS_BY_PATH.size(), "the item register face");
		assertEquals(280, GTFluidPipes.BLOCKS.getEntries().size(), "the DeferredRegister block census");
	}

	@Test
	public void theWoodPairStaysInRegistry() {
		// the W1 named seams survive the row-walk migration (the cracker recipes + the
		// smoke line consumers keep their constants)
		assertNotNull(GTFluidPipes.rowByPath("wood_fluid_pipe_small"));
		assertNotNull(GTFluidPipes.rowByPath("wood_fluid_pipe_medium"));
		assertSame(GTFluidPipes.BLOCKS_BY_PATH.get("wood_fluid_pipe_small"), GTFluidPipes.WOOD_FLUID_PIPE_SMALL);
		assertSame(GTFluidPipes.BLOCKS_BY_PATH.get("wood_fluid_pipe_medium"), GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM);
		assertSame(GTFluidPipes.ITEMS_BY_PATH.get("wood_fluid_pipe_medium"), GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM);
	}

	@Test
	public void everyPathIsMatFirstAndUnique() {
		Set<String> tSeen = new HashSet<>();
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			String tPath = tRow.path();
			assertEquals(tRow.material().slug() + "_fluid_pipe_" + tRow.variant().suffix, tPath,
					"the arch ruling: material first (the wood_fluid_pipe_small order)");
			assertTrue(tSeen.add(tPath), "duplicate path " + tPath);
		}
		assertEquals(280, tSeen.size());
	}

	// ------------------------------------------------------------------
	// the zero-diff metaId table
	// ------------------------------------------------------------------

	@Test
	public void metaIdBasesRideTheLoaderColumnsInOrder() {
		assertEquals(LOADER_BASES.length, GTFluidPipes.MATERIALS.size());
		for (int i = 0; i < LOADER_BASES.length; i++) {
			assertEquals(LOADER_BASES[i], GTFluidPipes.MATERIALS.get(i).metaIdBase(),
					"the loader column order drifted at material index " + i
							+ " (" + GTFluidPipes.MATERIALS.get(i).slug() + ")");
		}
	}

	@Test
	public void everyRowMetaIdIsBasePlusVariantOffset() {
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			assertEquals(tRow.material().metaIdBase() + tRow.variant().metaOffset, tRow.metaId(),
					"aID + n (:92-98) for " + tRow.path());
		}
		// spot pins off the loader text (the 26000 wood tiny, the 26142 tab column sits
		// beside them; 26660+5 = the netherite quadruple)
		assertEquals(26000, GTFluidPipes.rowByPath("wood_fluid_pipe_tiny").metaId());
		assertEquals(26665, GTFluidPipes.rowByPath("netherite_fluid_pipe_quadruple").metaId());
	}

	// ------------------------------------------------------------------
	// the variant table (:92-98 verbatim)
	// ------------------------------------------------------------------

	@Test
	public void theVariantTableMatchesUpstream() {
		long[] tCapMul = {1, 2, 6, 12, 24, 6, 2};
		int[] tTanks = {1, 1, 1, 1, 1, 4, 9};
		int[] tDiameter = {4, 6, 8, 12, 16, 16, 16};
		int[] tStack = {64, 64, 32, 16, 16, 16, 16};
		String[] tSuffix = {"tiny", "small", "medium", "large", "huge", "quadruple", "nonuple"};
		for (int i = 0; i < 7; i++) {
			GTFluidPipes.FluidPipeVariant tVariant = GTFluidPipes.VARIANTS.get(i);
			assertEquals(tSuffix[i], tVariant.suffix, "the registration order (:92-98)");
			assertEquals(tCapMul[i], tVariant.capMul, tSuffix[i] + " capacity multiplier");
			assertEquals(tTanks[i], tVariant.tankCount, tSuffix[i] + " NBT_TANK_COUNT");
			assertEquals(tDiameter[i], tVariant.diameterPx, tSuffix[i] + " NBT_DIAMETER PX_P");
			assertEquals(tStack[i], tVariant.maxStack, tSuffix[i] + " the aStackSize column");
			assertEquals(i, tVariant.metaOffset, tSuffix[i] + " the aID+n offset");
		}
	}

	// ------------------------------------------------------------------
	// the behaviour parameter spot pins (3 representative rows)
	// ------------------------------------------------------------------

	@Test
	public void woodMediumCarriesTheExplicitTempLine() {
		// :1846 — addFluidPipes(26000, 26142, 50, F,F,F,F, T,T, F, T, aWooden, aClass, 340, MT.Wood)
		GTFluidPipes.FluidPipeRow tRow = GTFluidPipes.rowByPath("wood_fluid_pipe_medium");
		GTFluidPipes.FluidPipeMaterial tMat = tRow.material();
		assertEquals(300, tRow.capacity(), "aStat 50 x 6 (:94)");
		assertEquals(340, tMat.maxTemperature(), "the explicit 340 loader constant");
		assertEquals(1, tRow.variant().tankCount);
		assertEquals(8, tRow.variant().diameterPx);
		assertFalse(tMat.gasProof());
		assertFalse(tMat.acidProof());
		assertFalse(tMat.plasmaProof());
		assertFalse(tMat.magicProof());
		assertTrue(tMat.contactDamage());
		assertTrue(tMat.flammable());
		assertFalse(tMat.recipe());
		assertTrue(tMat.blocking());
		assertSame(GTFluidPipes.PipeBlockFamily.WOODEN, tMat.blockFamily());
		assertSame(gregapi.data.MT.Wood, tMat.oreDictMaterial());
	}

	@Test
	public void copperLargeCarriesTheMeltDerivedTempLine() {
		// :1851 — addFluidPipes(26100, 26142, 100, T, F, F, F, T, F, T, T, aMachine, aClass, MT.Cu)
		// the 11-arg overload -> (long)(mMeltingPoint * 1.25), MultiTileEntityPipeFluid.java:84
		GTFluidPipes.FluidPipeRow tRow = GTFluidPipes.rowByPath("copper_fluid_pipe_large");
		GTFluidPipes.FluidPipeMaterial tMat = tRow.material();
		assertEquals(1200, tRow.capacity(), "aStat 100 x 12 (:95)");
		assertEquals((long)(gregapi.data.MT.Cu.mMeltingPoint * 1.25), tMat.maxTemperature(), "the melt-derived overload");
		assertTrue(tMat.gasProof());
		assertFalse(tMat.acidProof());
		assertTrue(tMat.contactDamage());
		assertTrue(tMat.recipe());
		assertTrue(tMat.blocking());
		assertSame(GTFluidPipes.PipeBlockFamily.MACHINE, tMat.blockFamily());
		// the block properties face: the blocking pair 2.0/6.0 (:92 NBT_HARDNESS/NBT_RESISTANCE)
		// — the block MUST ride the registration pipeProperties chain (a bare Properties.of()
		// would carry no strength at all)
		GTFluidPipeBlock tBlock = new GTFluidPipeBlock(tRow, GTFluidPipes.pipeProperties(tRow));
		assertEquals(100, tBlock.capacityPerTank() / 12, "the row carrier rides the block (capacity seam)");
		// the public per-state face (the resistance half of the pair rides the same
		// blocking ternary one line below in pipeProperties — no public per-state getter
		// exists over this mapping, the getDestroySpeed nulls are its documented
		// pass-through args)
		assertEquals(2.0F, tBlock.defaultBlockState().getDestroySpeed(null, null), "blocking -> hardness 2.0 (the pipeProperties pair)");
	}

	@Test
	public void netheriteQuadrupleCarriesTheMultiTankProofLine() {
		// :1866 — addFluidPipes(26660, 26142, 300, T, T, T, T, F, F, T, T, aMachine, aClass, MT.Netherite)
		GTFluidPipes.FluidPipeRow tRow = GTFluidPipes.rowByPath("netherite_fluid_pipe_quadruple");
		GTFluidPipes.FluidPipeMaterial tMat = tRow.material();
		assertEquals(1800, tRow.capacity(), "aStat 300 x 6 (:97)");
		assertEquals(4, tRow.variant().tankCount, "the quadruple 4-tank row");
		assertEquals(16, tRow.variant().diameterPx);
		assertTrue(tMat.gasProof());
		assertTrue(tMat.acidProof());
		assertTrue(tMat.plasmaProof());
		assertTrue(tMat.magicProof());
		assertFalse(tMat.contactDamage());
		assertFalse(tMat.flammable());
		assertTrue(tMat.recipe());
		assertTrue(tMat.blocking());
	}

	@Test
	public void rubberHugeIsTheNonBlockingUtilWoolLine() {
		// :1850 — addFluidPipes(26520, 26142, 100, T, F, F, F, T, F, F, F, aUtilWool, aClass, 350, MT.Rubber)
		// the ONE non-blocking line of the matrix — the 1.0/2.0 properties pair + the WOOL sound axis
		GTFluidPipes.FluidPipeRow tRow = GTFluidPipes.rowByPath("rubber_fluid_pipe_huge");
		GTFluidPipes.FluidPipeMaterial tMat = tRow.material();
		assertEquals(2400, tRow.capacity(), "aStat 100 x 24 (:96)");
		assertEquals(350, tMat.maxTemperature(), "the explicit 350 loader constant");
		assertFalse(tMat.blocking());
		assertFalse(tMat.recipe());
		assertSame(GTFluidPipes.PipeBlockFamily.UTIL_WOOL, tMat.blockFamily());
		GTFluidPipeBlock tBlock = new GTFluidPipeBlock(tRow, GTFluidPipes.pipeProperties(tRow));
		assertEquals(1.0F, tBlock.defaultBlockState().getDestroySpeed(null, null), "non-blocking -> hardness 1.0");
	}

	// ------------------------------------------------------------------
	// the registration face
	// ------------------------------------------------------------------

	@Test
	public void theBetAndTabWalksMirrorTheRows() {
		// the registration face mirrors the rows 1:1 — WITHOUT resolving any
		// RegistryObject (.get() never leaves the frozen-registry wall, the
		// GT6KineticsTabCensusTest posture); the live multi-mount walks (the BET
		// supplier blockArray(), the tab display walk, the tint/foam listeners) all
		// iterate these same maps, so key-set parity is the offline mirror
		assertEquals(GTFluidPipes.ROWS.size(), GTFluidPipes.BLOCKS_BY_PATH.size(), "the block register face");
		assertEquals(GTFluidPipes.ROWS.size(), GTFluidPipes.ITEMS_BY_PATH.size(), "the item register face");
		// the walk order is the row order (registration order: material-major, tiny first)
		int i = 0;
		for (String tPath : GTFluidPipes.BLOCKS_BY_PATH.keySet()) {
			assertEquals(GTFluidPipes.ROWS.get(i++).path(), tPath, "the registration walk order");
		}
		// the tab icon seat + the W1 named seams resolve as RegistryObjects
		assertSame(GTFluidPipes.ITEMS_BY_PATH.get("wood_fluid_pipe_medium"), GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM_ITEM);
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			assertNotNull(GTFluidPipes.ITEMS_BY_PATH.get(tRow.path()), tRow.path());
		}
		// the unknown-path gate stays null (the blockByPath precedent)
		assertNull(GTFluidPipes.blockByPath("not_a_pipe"));
	}

	@Test
	public void theComposedDisplayRidesTheFamilyKeys() {
		// the template + the mat word — the family-scoped namespace (the axle family's
		// row.mat.wood_treated is the ADJECTIVE, this family the dump noun)
		assertEquals("gt6.row.fluid_pipe.display.small", GTFluidPipes.FluidPipeVariant.SMALL.displayKey());
		assertEquals("gt6.row.fluid_pipe.mat.wood", GTFluidPipes.MAT_WOOD.unitKey());
		net.minecraft.network.chat.contents.TranslatableContents tContent =
				(net.minecraft.network.chat.contents.TranslatableContents) GTFluidPipes
						.rowByPath("wood_fluid_pipe_small").displayName().getContents();
		assertEquals("gt6.row.fluid_pipe.display.small", tContent.getKey());
		assertTrue(tContent.getArgs().length == 1 && tContent.getArgs()[0] instanceof net.minecraft.network.chat.Component tArg
				&& ((net.minecraft.network.chat.contents.TranslatableContents) tArg.getContents()).getKey()
							.equals("gt6.row.fluid_pipe.mat.wood"),
					"the composed display substitutes the mat unit key");
		}
}
