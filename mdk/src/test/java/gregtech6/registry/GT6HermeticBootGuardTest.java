package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The census pin (task hermetic-pour-tests): every mdk test class boots its own static
 * state HERMETICALLY — the reset-FIRST bracket (the r11e house rule, commit 443aaf8f3) —
 * so no filtered/full-suite run depends on which fork (maxParallelForks=6) or which
 * alphabetically-earlier class primed the JVM.
 *
 * <p><b>The before census (the bare {@code init()} forms this rule retired, 41 classes +
 * 1 base)</b>: the material boot rode a bare {@code MT.init()}/{@code OP.init()} (often
 * behind a manual registry open/close) in MaterialStackNBTTest, GT6CrucibleContentSyncTest,
 * GTMultiBlockCrucibleInputTest (+ its shared ProbeBoot), TileEntitySmelteryOfflineTest,
 * GT6MoldMaskTest, GT6FaucetTest, GTMultiBlockCruciblePhysicsTest, GT6CrucibleBowlDatagenTest,
 * GT6CrucibleSolidFaceMatrixTest, GT6RecipesWelderRowTest, GT6MoldTintDatagenTest,
 * GTWireTintCensusTest, GT6RecipeMapCrucibleTest (+ its shared GTMaterialItemsBoot),
 * FileSawTest, BendingCylinderSmallTest, GT6CrucibleLadderCensusTest,
 * GT6MaterialToolJeiExtensionTest, DigLadderTest, ProspectorTest, GT6ItemDataTest,
 * GT6BladeLadderTest, ToolIdentityItemColorsCensusTest, MachineLadderTest,
 * GT6CrucibleProviderTest, GT6ItemDataNeoRegistrationTest, HammerWrenchTest; the recipe-map
 * boot rode a bare {@code GT6RecipeMaps.init()} in GT6P34MachineRowsTest, GT6HeatExchangerTest,
 * GT6MultiBlockConverterTest, GT6ChemicalGasTurbineRowTest, GT6LargeMachineTanksTest,
 * GTGeneratorLiquidBlockEntityTest, BurningBoxIgniteTest, GTGeneratorSolidBlockEntityTest,
 * BurningBoxRowTableTest, GT6QuMachinesRegistrationTest, GT6LargeMachineMUIPanelTest,
 * GT6SingleBlockFacingIntegrityTest, GT6RecipeMapLangTest, GT6MachineFamilyHoverTest and the
 * shared GTMachinesOfflineTestBase; and GT6HopperFamilyTest had NO boot at all (the
 * known_bug r11-hopper-mt-offline-latch solo red). After: 0 bare forms (the two pins below).
 *
 * <p><b>Guard shape</b>: a bytecode scan of the compiled mdk TEST classes (the same
 * mechanism as GT6RegistryStaticInitGuardTest, aimed at the test tree). Rule ①: a test
 * class never invokes {@code MT.init}/{@code OP.init} directly — the sanctioned material
 * boot is {@link GT6MaterialTestSupport#materials()} (reset FIRST, then the full
 * initMaterials refill; initMaterials itself lives in main code and is invisible here).
 * Rule ②: a test class that invokes {@code GT6RecipeMaps.init} must invoke
 * {@code GT6RecipeMaps.reset} somewhere in the class (the neo junit-fml boot pours the
 * static suite into the maps at modloading, so a reset-less init rides boot residue; the
 * Mortar/B1 empirics). The established init-at-start/reset-at-{@code @AfterEach} lifecycle
 * (GTMachinesOfflineTestBase) passes — the class-scoped letter, not per-method adjacency.
 */
class GT6HermeticBootGuardTest {

	private static final String MT = "gregapi/data/MT";
	private static final String OP = "gregapi/data/OP";
	private static final String RECIPE_MAPS = "gregtech6/recipes/GT6RecipeMaps";

	/** Rule ① hits: direct MT/OP init invokes anywhere in the class. */
	private static List<String> bareMaterialBoots(byte[] aClass) {
		ClassNode tNode = new ClassNode();
		new ClassReader(aClass).accept(tNode, 0);
		List<String> rHits = new ArrayList<>();
		for (MethodNode tMethod : tNode.methods) {
			for (AbstractInsnNode tInsn : tMethod.instructions) {
				if (tInsn instanceof MethodInsnNode tCall && tCall.getOpcode() == Opcodes.INVOKESTATIC
						&& (MT.equals(tCall.owner) || OP.equals(tCall.owner)) && "init".equals(tCall.name)) {
					rHits.add(tNode.name.replace('/', '.') + "." + tMethod.name + " -> " + tCall.owner + ".init()");
				}
			}
		}
		return rHits;
	}

	/** Rule ② hits: a class that inits the recipe maps but never participates in the reset lifecycle. */
	private static List<String> bareMapInits(byte[] aClass) {
		ClassNode tNode = new ClassNode();
		new ClassReader(aClass).accept(tNode, 0);
		boolean tInits = false, tResets = false;
		for (MethodNode tMethod : tNode.methods) {
			for (AbstractInsnNode tInsn : tMethod.instructions) {
				if (tInsn instanceof MethodInsnNode tCall && tCall.getOpcode() == Opcodes.INVOKESTATIC
						&& RECIPE_MAPS.equals(tCall.owner)) {
					if ("init".equals(tCall.name)) tInits = true;
					else if ("reset".equals(tCall.name)) tResets = true;
				}
			}
		}
		if (tInits && !tResets) {
			return List.of(tNode.name.replace('/', '.')
					+ " -> GT6RecipeMaps.init() with no GT6RecipeMaps.reset() anywhere in the class");
		}
		return List.of();
	}

	/** The mdk test output dir this very class was loaded from. */
	private static Path testClassesDir() throws Exception {
		return Path.of(GT6HermeticBootGuardTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	/** THE pin: zero bare material boots and zero reset-less map inits across the test tree. */
	@Test
	void noTestClassRidesABareInitBoot() throws Exception {
		List<String> tViolations = new ArrayList<>();
		try (Stream<Path> tWalk = Files.walk(testClassesDir())) {
			for (Path tFile : tWalk.filter(aPath -> aPath.toString().endsWith(".class")).toList()) {
				// the census letter covers the test classes only: on the neo unitTest layout the
				// scan dir also carries the MAIN classes (GT6RecipesAnvil & co — production walk
				// classes that legitimately init maps at mod construct, out of the rule's scope),
				// and inner fixtures ($...) ride their enclosing Test class's scan.
				String tName = tFile.getFileName().toString();
				if (tName.contains("$") || !(tName.endsWith("Test.class") || tName.endsWith("TestBase.class"))) continue;
				byte[] tBytes = Files.readAllBytes(tFile);
				tViolations.addAll(bareMaterialBoots(tBytes));
				tViolations.addAll(bareMapInits(tBytes));
			}
		}
		assertTrue(tViolations.isEmpty(),
				"every test class boots hermetically: the reset-FIRST bracket (GT6MaterialTestSupport.materials() for materials, "
				+ "GT6RecipeMaps.reset() before init() for maps) — a bare init() rides neo boot residue or dies on a cold forge "
				+ "fork (the maxParallelForks=6 lottery, the GT6HopperFamilyTest solo red). Violations:\n"
				+ String.join("\n", tViolations));
	}

	/**
	 * The scanner self-proof: hand-built method bodies carry the violation and the sanctioned
	 * shapes — reset-then-init, and the helper routing (initMaterials, not MT.init) — pass.
	 */
	@Test
	void theScanCatchesTheBareShapesAndSparesTheHermeticShapes() {
		MethodNode tBare = method("bare", aV -> aV.visitMethodInsn(Opcodes.INVOKESTATIC, MT, "init", "()V", false));
		MethodNode tBareMap = method("bareMap", aV -> aV.visitMethodInsn(Opcodes.INVOKESTATIC, RECIPE_MAPS, "init", "()V", false));
		MethodNode tHermeticMap = method("hermeticMap", aV -> {
			aV.visitMethodInsn(Opcodes.INVOKESTATIC, RECIPE_MAPS, "reset", "()V", false);
			aV.visitMethodInsn(Opcodes.INVOKESTATIC, RECIPE_MAPS, "init", "()V", false);
		});

		assertFalse(bareMaterialBoots(methodBytes(tBare)).isEmpty(), "the direct MT.init IS the rule-① violation");
		assertFalse(bareMapInits(methodBytes(tBareMap)).isEmpty(), "the reset-less map init IS the rule-② violation");
		assertTrue(bareMapInits(methodBytes(tHermeticMap)).isEmpty(), "reset FIRST then init is the sanctioned bracket");
		assertTrue(bareMaterialBoots(methodBytes(method("helper", aV ->
				aV.visitMethodInsn(Opcodes.INVOKESTATIC, "gregtech6/registry/GTMaterialItems", "initMaterials", "()V", false)
		))).isEmpty(), "the sanctioned initMaterials routing is not a bare boot");
	}

	private static MethodNode method(String aName, java.util.function.Consumer<MethodNode> aBody) {
		MethodNode rMethod = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, aName, "()V", null, null);
		aBody.accept(rMethod);
		rMethod.visitInsn(Opcodes.RETURN);
		rMethod.visitMaxs(0, 0);
		return rMethod;
	}

	private static byte[] methodBytes(MethodNode aMethod) {
		ClassNode tNode = new ClassNode();
		tNode.name = "Probe";
		tNode.methods.add(aMethod);
		org.objectweb.asm.ClassWriter tWriter = new org.objectweb.asm.ClassWriter(0);
		tNode.accept(tWriter);
		return tWriter.toByteArray();
	}
}
