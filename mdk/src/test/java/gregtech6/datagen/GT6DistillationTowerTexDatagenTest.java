/*
 * Offline pinned-census tests for task distill-tower-face-3d (the user-named symptom④
 * close-out — "提了多少次一直没动"): the twin distillation tower controllers
 * (Loader_MultiTileEntities.java:1226-1227, NBT_TEXTURE "distillationtower" /
 * "cryodistillationtower", both NBT_MATERIAL MT.StainlessSteel) leave the w3-era
 * placeholder (the cube_all over the BORROWED distillation-tower-part side sprite and
 * its 2D item/generated icon, FACING-blind) for the real upstream art — the
 * familyMachineModel state trio grammar (the addMachine form):
 * <ul>
 * <li>the byte-verbatim 48-PNG wave: the task card's "single overlay group / 24 PNG"
 *     premise was disprobed — upstream MultiTileEntityBasicMachine.java:176-204 derives
 *     FOUR six-face groups from the NBT_TEXTURE name, the mTexturesInactive/Active/
 *     Running trio riding the {@code overlay/}/{@code overlay_active/}/
 *     {@code overlay_running/} dirs (each tower 24 PNGs). The three states differ ONLY
 *     in the front art (28ca413c / 29b4e009 / f0570c61); the other five faces are
 *     byte-identical across all three groups, and both towers' art is byte-identical
 *     throughout (the upstream authors shipped one set twice);</li>
 * <li>the tint seat: the colored body carries tintindex 0 and resolves the row
 *     NBT_MATERIAL through the GTDistillationTowerBlock ctor carrier (the
 *     c2-controller-tint gate), the six 0.01 state decals stay untinted (the P22
 *     contract), the models declare cutout (the issue #8 SOLID-layer fix);</li>
 * <li>the state switch: upstream getTexture2 :1014 picks
 *     {@code mActive||worldObj==null ? Active : mRunning ? Running : Inactive} — the
 *     port maps it onto the GTDistillationTowerBlock ACTIVE/RUNNING properties (the
 *     getVisualData :1010-1011 two-bit payload, the BE onTickChecked write), so the
 *     blockstate carries the full 4 FACING x 2 FORMED x 2 ACTIVE x 2 RUNNING = 32
 *     variants switching the trio; the BlockItems parent the {@code _active} model (the
 *     upstream {@code worldObj==null} item-form arm), the 3D block form.</li>
 * </ul>
 *
 * <p>Coverage: the 6 models keep the two-layer six-face grammar (the FACING_ROTATIONS
 * face→art table: north=front, south=back, west=right, east=left — CS.java:528-537);
 * the two blockstates pin exactly the 32 variants with the addLargeBoiler rotationY
 * band and the :1014 pick order; the item models ride the 3D block form over the
 * Active overlay (the item/generated 2D form must go red); the 48 borrows are
 * byte-verbatim upstream AND grounded in assets/README.md; the tint carrier resolves
 * StainlessSteel and the 32-state space exists; the BE visual write no-ops on the
 * property-less offline fixtures. KJS face: none — resources + datagen only.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.block.GTBlockProperties;
import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.registry.GT6Distillation;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

class GT6DistillationTowerTexDatagenTest extends GTOfflineTestBase {

	/** The six faces, the upstream basicmachines file naming. */
	private static final List<String> FACES = List.of("bottom", "top", "front", "back", "left", "right");

	/** The three overlay groups (the mTexturesInactive/Active/Running dirs, :183-203). */
	private static final List<String> OVERLAY_GROUPS = List.of("", "_active", "_running");

	/** The two towers, Loader order :1226-1227 — path (the model/block key) and the NBT_TEXTURE family (the PNG prefix). */
	private static final List<String[]> TOWERS = List.of(
			new String[] {"distillation_tower", "distillationtower"},
			new String[] {"cryo_distillation_tower", "cryodistillationtower"});

	/** facing → rotationY (the addLargeBoiler band; the model-space front is north; the blockstate keys use the lowercase property names). */
	private static final Map<String, Integer> ROT_Y = Map.of(
			"north", 0, "south", 180, "west", 270, "east", 90);

	/**
	 * The 8 distinct upstream digests behind the 48 borrows. The three overlay groups
	 * share the back/left/right (97ff2978), bottom (e8e039f3) and top (ab65e83b) art and
	 * differ ONLY in the front (the three-way trio below) — the upstream tri-state truth
	 * this card pins. The colored body is one shared grayscale PNG (db9560d3).
	 */
	private static final String SHA_COLORED = "db9560d38648fee70a0c8618e793d5c262cb269a408af0fdba99518343e4372e";
	private static final String SHA_SIDES = "97ff29785f50fdc3f8e703e67fb1e5b73ac066b7da9dba136406c6ace52f22a4";
	private static final String SHA_BOTTOM = "e8e039f30126aba855cf9ff7fab12d940c35f1a210c08e18b7884705dd92450c";
	private static final String SHA_TOP = "ab65e83b4133ba41994a0af9dd1718b1ab677e7f674d4d76fa6f105c60ddd1a1";
	private static final Map<String, String> SHA_FRONT_BY_GROUP = Map.of(
			"", "28ca413ca5f2d6684ecdbd361a78ed39d9c203fb642833f08061e3c9f0acab18",
			"_active", "29b4e009461fb5640035bb12fd97d07143dad723c8f0731facdcae4962096f44",
			"_running", "f0570c614c363d6f64073e0b75ceca17deec3775d55081be46ffbf1e4b2e7ea1");

	@BeforeAll
	static void bootOffline() {
		GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GT6QuantumEnergizerTexDatagenTest recipe —
		// the block ctor registers its intrusive holder, validateWrite rejects a frozen
		// registry)
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
	}

	private static JsonObject json(String aPath) throws Exception {
		try (InputStream tStream = GT6DistillationTowerTexDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	// ------------------------------------------------------------------
	// the model trio: the two-layer six-face grammar (the familyMachineModel form)
	// ------------------------------------------------------------------

	@Test
	public void modelTrioKeepsTheTwoLayerSixFaceGrammar() throws Exception {
		for (String[] tTower : TOWERS) {
			String tPath = tTower[0];
			String tFamily = tTower[1];
			for (String tGroup : OVERLAY_GROUPS) {
				JsonObject tModel = json("assets/gt6/models/block/" + tPath + tGroup + ".json");
				assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
						tPath + tGroup + ": the cube parent (the display transforms)");
				assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
						tPath + tGroup + ": the cutout declaration (the issue #8 SOLID-layer fix)");
				JsonObject tTextures = tModel.getAsJsonObject("textures");
				// the colored body — the FACING_ROTATIONS face→art table
				// (CS.java:528-537 north row: west=right / east=left), the familyMachineModel pin
				assertEquals("gt6:block/" + tFamily + "_colored_front", tTextures.get("north").getAsString(),
						tPath + tGroup + ": north = the colored front");
				assertEquals("gt6:block/" + tFamily + "_colored_back", tTextures.get("south").getAsString(),
						tPath + tGroup + ": south = the colored back");
				assertEquals("gt6:block/" + tFamily + "_colored_right", tTextures.get("west").getAsString(),
						tPath + tGroup + ": west = the RIGHT art (the FACING_ROTATIONS mirror)");
				assertEquals("gt6:block/" + tFamily + "_colored_left", tTextures.get("east").getAsString(),
						tPath + tGroup + ": east = the LEFT art");
				assertEquals("gt6:block/" + tFamily + "_colored_top", tTextures.get("up").getAsString());
				assertEquals("gt6:block/" + tFamily + "_colored_bottom", tTextures.get("down").getAsString());
				assertEquals("gt6:block/" + tFamily + "_colored_front", tTextures.get("particle").getAsString(),
						tPath + tGroup + ": the particle key (the r11-machine-particle-key rule)");
				// the state overlay trio — the group suffix swaps ALL six shells
				for (String tFace : FACES) {
					assertEquals("gt6:block/" + tFamily + "_overlay_" + tFace + tGroup,
							tTextures.get("overlay_" + tFace).getAsString(),
							tPath + tGroup + ": the overlay_" + tFace + " shell rides the " + (tGroup.isEmpty() ? "inactive" : tGroup.substring(1)) + " group");
				}
				// the elements: the tinted body + six untinted 0.01 decals
				var tElements = tModel.getAsJsonArray("elements");
				assertEquals(7, tElements.size(), tPath + tGroup + ": body + 6 decals");
				var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
				for (String tFace : List.of("down", "up", "north", "south", "west", "east")) {
					assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
							tPath + tGroup + ": body face " + tFace + " carries tintindex 0 (the StainlessSteel seat)");
				}
				for (int i = 1; i < 7; i++) {
					var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
					for (String tFace : tDecalFaces.keySet()) {
						assertFalse(tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
								tPath + tGroup + ": decal " + i + " face " + tFace + " stays untinted (the P22 overlay pass)");
					}
				}
			}
			// the tri-state fronts differ — the :1014 pick actually switches art
			JsonObject tInactive = json("assets/gt6/models/block/" + tPath + ".json");
			JsonObject tActive = json("assets/gt6/models/block/" + tPath + "_active.json");
			JsonObject tRunning = json("assets/gt6/models/block/" + tPath + "_running.json");
			assertFalse(tInactive.getAsJsonObject("textures").get("overlay_front").getAsString()
					.equals(tActive.getAsJsonObject("textures").get("overlay_front").getAsString()),
					tPath + ": the active front shell differs from the inactive one");
			assertFalse(tActive.getAsJsonObject("textures").get("overlay_front").getAsString()
					.equals(tRunning.getAsJsonObject("textures").get("overlay_front").getAsString()),
					tPath + ": the running front shell differs from the active one");
		}
	}

	// ------------------------------------------------------------------
	// the blockstates: exactly 32 variants, the rotationY band, the :1014 pick order
	// ------------------------------------------------------------------

	@Test
	public void blockstatesPinThe32VariantsAndThePickOrder() throws Exception {
		assertEquals(2, GT6Distillation.ROWS.size(), "the twin-tower walk");
		for (String[] tTower : TOWERS) {
			String tPath = tTower[0];
			var tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
			assertEquals(32, tVariants.size(), tPath + ": exactly the 4 FACING x 2 FORMED x 2 ACTIVE x 2 RUNNING variants");
			for (String tFacing : ROT_Y.keySet()) {
				for (boolean tFormed : new boolean[] {false, true}) {
					for (boolean tActive : new boolean[] {false, true}) {
						for (boolean tRunning : new boolean[] {false, true}) {
							String tKey = "active=" + tActive + ",facing=" + tFacing + ",formed=" + tFormed + ",running=" + tRunning;
							assertTrue(tVariants.has(tKey), tPath + ": the variant key " + tKey);
							JsonObject tVariant = tVariants.getAsJsonObject(tKey);
							// the getTexture2 :1014 pick order — mActive wins over mRunning
							String tExpectedModel = "gt6:block/" + tPath + (tActive ? "_active" : tRunning ? "_running" : "");
							assertEquals(tExpectedModel, tVariant.get("model").getAsString(),
									tPath + " " + tKey + ": the model");
							assertEquals(ROT_Y.get(tFacing).intValue(),
									tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
									tPath + " " + tKey + ": the rotationY band (the front follows FACING)");
						}
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// the item form: the 3D block model over the Active overlay (the worldObj==null arm)
	// ------------------------------------------------------------------

	@Test
	public void itemModelsRideThe3DBlockFormActiveOverlay() throws Exception {
		for (String[] tTower : TOWERS) {
			String tPath = tTower[0];
			JsonObject tModel = json("assets/gt6/models/item/" + tPath + ".json");
			assertEquals("gt6:block/" + tPath + "_active", tModel.get("parent").getAsString(),
					tPath + ": the _active block-model parent (the upstream worldObj==null Active arm, the 3D form)");
			assertFalse("minecraft:item/generated".equals(tModel.get("parent").getAsString()),
					tPath + ": the 2D item/generated form is the retired placeholder (must be red)");
			assertFalse(tModel.has("textures"), tPath + ": no local textures object (the flat-sprite pin is retired)");
		}
	}

	// ------------------------------------------------------------------
	// the 48 borrows: byte-verbatim upstream + README-grounded
	// ------------------------------------------------------------------

	@Test
	public void the48BorrowsAreByteVerbatimUpstreamAndLedgered() throws Exception {
		Path tMdkRoot = mdkRoot();
		Path tStatic = tMdkRoot.resolve("src/main/resources/assets/gt6/textures/block");
		String tReadme = Files.readString(tMdkRoot.resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		MessageDigest tSha256 = MessageDigest.getInstance("SHA-256");
		int tCount = 0;
		List<String> tViolations = new java.util.ArrayList<>();
		for (String[] tTower : TOWERS) {
			String tFamily = tTower[1];
			for (String tFace : FACES) {
				String[][] tEntries = {
						{tFamily + "_colored_" + tFace + ".png", SHA_COLORED},
						{tFamily + "_overlay_" + tFace + ".png",
								tFace.equals("front") ? SHA_FRONT_BY_GROUP.get("") : tFace.equals("bottom") ? SHA_BOTTOM : tFace.equals("top") ? SHA_TOP : SHA_SIDES},
						{tFamily + "_overlay_" + tFace + "_active.png",
								tFace.equals("front") ? SHA_FRONT_BY_GROUP.get("_active") : tFace.equals("bottom") ? SHA_BOTTOM : tFace.equals("top") ? SHA_TOP : SHA_SIDES},
						{tFamily + "_overlay_" + tFace + "_running.png",
								tFace.equals("front") ? SHA_FRONT_BY_GROUP.get("_running") : tFace.equals("bottom") ? SHA_BOTTOM : tFace.equals("top") ? SHA_TOP : SHA_SIDES}};
				for (String[] tEntry : tEntries) {
					tCount++;
					Path tPng = tStatic.resolve(tEntry[0]);
					assertTrue(Files.isRegularFile(tPng), tEntry[0] + " must exist on the static tree");
					String tHex = HexFormat.of().formatHex(tSha256.digest(Files.readAllBytes(tPng)));
					if (!tHex.equals(tEntry[1])) tViolations.add(tEntry[0] + " hashes " + tHex + ", expected the upstream " + tEntry[1]);
					if (!tReadme.contains(tEntry[0])) tViolations.add(tEntry[0] + " — filename absent from assets/README.md");
					if (!tReadme.contains(tEntry[1])) tViolations.add(tEntry[0] + " — digest absent from assets/README.md");
				}
			}
		}
		assertEquals(48, tCount, "the 48-PNG wave (2 towers x 6 faces x 4 groups)");
		assertTrue(tViolations.isEmpty(), String.join("; ", tViolations));
	}

	// ------------------------------------------------------------------
	// the tint carrier + the 32-state space + the visual-write guard
	// ------------------------------------------------------------------

	@Test
	public void tintCarrierResolvesStainlessSteelAndTheStateSpaceIs32() {
		GT6Distillation.GTDistillationTowerBlock tBlock = new GT6Distillation.GTDistillationTowerBlock(
				GT6Distillation.TOWER_ROW, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
		// both rows carry NBT_MATERIAL MT.StainlessSteel (Loader :1226-1227) — the
		// material-carrier arm of the ctor feeds the c2-controller-tint gate
		assertSame(gregapi.data.MT.StainlessSteel, tBlock.material(), "the carrier resolves the StainlessSteel column");
		assertSame(gregapi.data.MT.StainlessSteel, GTMultiBlockControllerBlock.materialOf(tBlock),
				"the controller dispatch resolves the carrier");
		assertNull(GTMultiBlockControllerBlock.materialOf(net.minecraft.world.level.block.Blocks.STONE),
				"a non-carrier stays null");
		// the visual pair rides the SHARED property objects (the GTBasicMachineBlock form)
		assertSame(GTBlockProperties.ACTIVE, GT6Distillation.GTDistillationTowerBlock.ACTIVE, "the shared ACTIVE object");
		assertSame(GTBlockProperties.RUNNING, GT6Distillation.GTDistillationTowerBlock.RUNNING, "the shared RUNNING object");
		// 4 FACING x 2 FORMED x 2 ACTIVE x 2 RUNNING
		assertEquals(32, tBlock.getStateDefinition().getPossibleStates().size(), "the 32-state space");
		// the tint-array face is runtime (RegistryObject resolution — the
		// GT6ControllerPaintRenderDatagenTest posture): the walk membership pins through
		// the row table (the wiringCensusStays46Blocks sum)
		assertEquals(2, GT6Distillation.ROWS.size(), "the twin-block tint array (the row table)");
	}

	@Test
	public void theVisualWriteNoOpsOnPropertyLessFixtures() {
		// the offline STONE-state fixture (no ACTIVE/RUNNING/FACING/FORMED properties) —
		// the onTickChecked guard order keeps the write a no-op without a level. The BE
		// ctor VALIDATES the state against the type (the GT6DistillationTowerTest :67-71
		// note), so the fixture binds a real BET over STONE (the GTOfflineTestBase
		// @BeforeAll already bootstrapped + unfroze the BET registry)
		BlockEntityType<GT6Distillation.TileEntityDistillationTower> tType =
				BlockEntityType.Builder.of((aPos, aState) -> new GT6Distillation.TileEntityDistillationTower(null, aPos, aState),
						net.minecraft.world.level.block.Blocks.STONE).build(null);
		GT6Distillation.TileEntityDistillationTower tTower =
				new GT6Distillation.TileEntityDistillationTower(tType, new net.minecraft.core.BlockPos(1, 2, 3),
						net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		tTower.mActive = true;
		tTower.mRunning = true;
		tTower.onTickChecked(1); // must not throw — the hasProperty/hasLevel guards
		assertFalse(tTower.getBlockState().hasProperty(GT6Distillation.GTDistillationTowerBlock.ACTIVE),
				"the STONE fixture carries no visual properties (the guard premise)");
	}
}
