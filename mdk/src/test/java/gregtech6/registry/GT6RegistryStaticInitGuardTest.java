package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The rule pin (task p31-supplier-rule-pin): registry-side classes load at MOD CONSTRUCTION
 * (the {@code @EventBusSubscriber} scan, the {@code DeferredRegister} holders), which runs
 * BEFORE {@code MT.init()}/{@code OP.init()} in {@code FMLConstructModEvent.enqueueWork} (the
 * GT6Mod lifecycle javadoc). A direct {@code MT.X}/{@code OP.Y} read in a static row
 * initializer therefore resolves the pre-init fields — null — and freezes them for the whole
 * JVM generation (the GTWireSpecs:35 ruling; the lesson instances GTMachines.java:83-88 and
 * GT6OreBlocks.java:114-117 supplier-ized for exactly this; the live catch this task fixed:
 * GT6RecipesCompressor's eager {@code GEM_CHAIN_PREFIXES} froze four null prefixes and
 * silently dead-dropped the :217/:226 gem Nor gate). The sanctioned form is lazy resolution —
 * a {@code Supplier<OreDictMaterial>}/{@code Supplier<OreDictPrefix>} row or a call-time read
 * inside a method (the GTMachines.java:87 / GT6RecipesCompressor.table() forms).
 *
 * <p><b>Guard shape</b>: a bytecode scan of every compiled mdk main class — a
 * {@code GETSTATIC gregapi/data/MT.X} or {@code GETSTATIC gregapi/data/OP.Y} instruction
 * inside a {@code <clinit>} is the violation signature. Lambda bodies compile into synthetic
 * {@code lambda$...} methods, NOT into {@code <clinit>}, so the sanctioned supplier rows are
 * structurally invisible to the scan (pinned by the two controls below). The scope is the
 * whole mdk main tree, not just the {@code registry} package: any mdk class is reachable from
 * the mod-construct graph through the subscriber scan, and class-load order anywhere else is
 * the same lottery (the compressor loader classes live in {@code gregtech6.recipes} and load
 * at mod construct all the same). The root gregapi data classes are out of scope by design —
 * {@code MT}/{@code OP} own the registry itself, and method-body reads (post-init call time)
 * are never flagged. One declared exemption: the {@code gregtech6.datagen} package (see
 * {@link #exemptDatagenOnly}).
 *
 * <p><b>Form boundary</b>: only DIRECT {@code <clinit>} reads are the rule. A static
 * initializer that calls a method which reads {@code MT.X} is a reviewer's problem, not this
 * guard's — flow analysis past the instruction boundary is not the rule's letter
 * ("行初始化禁直读").
 */
class GT6RegistryStaticInitGuardTest {

	/** The data classes whose eager read the rule forbids (internal names). */
	private static final String MT = "gregapi/data/MT";
	private static final String OP = "gregapi/data/OP";

	private static final String MATERIAL_DESC = "Lgregapi/oredict/OreDictMaterial;";

	/** The MT.X/OP.Y members read directly inside the class file's {@code <clinit>}, if any. */
	private static List<String> clinitMaterialReads(byte[] aClass) {
		ClassNode tNode = new ClassNode();
		new ClassReader(aClass).accept(tNode, 0);
		List<String> rHits = new ArrayList<>();
		for (MethodNode tMethod : tNode.methods) {
			if (!"<clinit>".equals(tMethod.name)) continue;
			for (AbstractInsnNode tInsn : tMethod.instructions) {
				if (tInsn instanceof FieldInsnNode tField && tField.getOpcode() == Opcodes.GETSTATIC
						&& (MT.equals(tField.owner) || OP.equals(tField.owner))) {
					rHits.add(tField.owner + "." + tField.name);
				}
			}
		}
		return rHits;
	}

	/** The mdk main output dir this test JVM loaded its main classes from. */
	private static Path mainClassesDir() throws Exception {
		return Path.of(GT6OreBlocks.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	/** THE pin: no mdk main class touches MT/OP statics in its static row initialization. */
	@Test
	void noMainClassReadsMaterialsInItsStaticInitializer() throws Exception {
		List<String> tViolations = new ArrayList<>();
		try (Stream<Path> tWalk = Files.walk(mainClassesDir())) {
			for (Path tFile : tWalk.filter(aPath -> aPath.toString().endsWith(".class")).toList()) {
				if (exemptDatagenOnly(tFile)) continue;
				List<String> tHits = clinitMaterialReads(Files.readAllBytes(tFile));
				if (!tHits.isEmpty()) tViolations.add(tFile.getFileName() + " -> " + tHits);
			}
		}
		assertTrue(tViolations.isEmpty(),
				"static row initializers must not read MT.X/OP.Y directly — the registry classes load at mod construct, "
				+ "before MT.init()/OP.init() (the GTWireSpecs:35 ruling); resolve lazily instead, the Supplier form of "
				+ "GTMachines.java:87. Violations:\n" + String.join("\n", tViolations));
	}

	/**
	 * The declared exemption: the {@code gregtech6.datagen} package (only
	 * {@code GT6DataGenerators.onGatherData} loads it, AFTER the common-setup
	 * {@code MT.init()} in the datagen JVM — the GT6WorldgenDatagen/GT6BlockTags
	 * tables are live there, the bedrock-parity run is the proof). The exemption
	 * holds ONLY while production code never references the package: the first
	 * mod-construct-reachable consumer of a datagen class turns its clinit into
	 * this rule's violation — re-audit the package the moment such a reference
	 * lands (grep for the class name outside datagen first).
	 */
	private static boolean exemptDatagenOnly(Path aClassFile) {
		return aClassFile.toString().contains("gregtech6" + File.separator + "datagen" + File.separator);
	}

	/**
	 * The scanner self-proof: the violation shape (a direct {@code <clinit>} GETSTATIC) is
	 * caught, and the sanctioned lazy shapes are spared — the same read inside a plain method
	 * (the compiled form of a supplier lambda body) is not a {@code <clinit>} read.
	 */
	@Test
	void theScanCatchesTheViolationShapeAndSparesTheLazyShapes() throws Exception {
		byte[] tViolating = staticMethodClass("StaticInitGuardProbeViolating", "<clinit>", "()V", null,
				aMv -> {
					aMv.visitFieldInsn(Opcodes.GETSTATIC, MT, "Iron", MATERIAL_DESC);
					aMv.visitInsn(Opcodes.POP);
					aMv.visitInsn(Opcodes.RETURN);
				});
		assertEquals(List.of(MT + ".Iron"), clinitMaterialReads(tViolating), "the direct clinit read IS the violation");

		// the supplier-lambda shape: the read lives in a synthetic lambda$-style method, the <clinit>
		// only runs the invokedynamic — no MT/OP static appears in the static initializer itself
		byte[] tLazyShaped = staticMethodClass("StaticInitGuardProbeSupplierShaped", "material", "()" + MATERIAL_DESC, MATERIAL_DESC,
				aMv -> {
					aMv.visitFieldInsn(Opcodes.GETSTATIC, MT, "Iron", MATERIAL_DESC);
					aMv.visitInsn(Opcodes.ARETURN);
				});
		assertTrue(clinitMaterialReads(tLazyShaped).isEmpty(), "the lazy/supplier form is not a clinit read");
	}

	/** A minimal class with one static method emitting the given body (never loaded — parsed only). */
	private static byte[] staticMethodClass(String aName, String aMethod, String aDesc, String aReturnDesc,
			Consumer<MethodVisitor> aBody) {
		ClassWriter tWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		tWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, aName, null, "java/lang/Object", null);
		MethodVisitor tMethod = tWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, aMethod, aDesc, null, null);
		aBody.accept(tMethod);
		tMethod.visitMaxs(0, 0);
		tMethod.visitEnd();
		tWriter.visitEnd();
		return tWriter.toByteArray();
	}
}
