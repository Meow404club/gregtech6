package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Task robotics-chain — the robot-component registration census: the four compact
 * component ladders (MOTORS metas 12000-12009 / CONVEYERS 12040-12049 / PISTONS
 * 12060-12069 / ROBOT_ARMS 12080-12089, the {@code for (i < 10)} walk
 * MultiItemTechnological.java:45-54) + the 10 {@code Robot_Tip_*} items
 * (MultiItemRandomTools.java:470-479, metas 8000-8009) + the 10 {@code Tool_Token_*}
 * items (:492-501, metas 8500-8509) = 60 plain items. The tier words are the upstream
 * CS.VN[0..9] slice (CS.java:154), shared with {@link GT6Covers#TIER_NAMES}.
 *
 * <p>The offline surface follows the GT6ExtruderMoldsCensusTest discipline: registry
 * objects are unbound in this JVM, but the list walk order, the shipped asset tree
 * (generated resources ride the test classpath) and the reflective tab-join shape are
 * all readable — and every face here is exactly what the JEI/creative visibility, the
 * Robot_Tip crafting rows and the Boxinator rows consume.
 */
public class GT6RoboticsCensusTest {

	/** The upstream tier words CS.VN[0..9] (CS.java:154) — the snake suffix of every ladder id. */
	private static final List<String> TIERS = List.of("ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1");

	/** The tip/token kind words in upstream registration order (MultiItemRandomTools.java:470-479). */
	private static final List<String> KINDS = List.of("wrench", "screwdriver", "saw", "hammer", "cutter", "chisel", "rubber", "blade", "drill", "file");

	/** The per-list id walks in upstream meta order. */
	private static List<String> ladder(String aPrefix) {
		List<String> tIds = new ArrayList<>();
		for (String tTier : TIERS) tIds.add(aPrefix + "_" + tTier);
		return List.copyOf(tIds);
	}

	private static final List<String> MOTOR_IDS = ladder("compact_electric_motor");
	private static final List<String> CONVEYER_IDS = ladder("compact_electric_conveyor");
	private static final List<String> PISTON_IDS = ladder("compact_electric_piston");
	private static final List<String> ARM_IDS = ladder("compact_robot_arm");
	private static final List<String> TIP_IDS;
	private static final List<String> TOKEN_IDS;
	static {
		List<String> tTips = new ArrayList<>();
		List<String> tTokens = new ArrayList<>();
		for (String tKind : KINDS) {
			tTips.add("robot_arm_" + tKind + "_tip");
			tTokens.add("single_use_" + tKind);
		}
		TIP_IDS = List.copyOf(tTips);
		TOKEN_IDS = List.copyOf(tTokens);
	}

	/** The full 60-item census (40 ladder + 10 tips + 10 tokens). */
	private static final List<String> ALL_IDS;
	static {
		List<String> tAll = new ArrayList<>(60);
		tAll.addAll(MOTOR_IDS);
		tAll.addAll(CONVEYER_IDS);
		tAll.addAll(PISTON_IDS);
		tAll.addAll(ARM_IDS);
		tAll.addAll(TIP_IDS);
		tAll.addAll(TOKEN_IDS);
		ALL_IDS = List.copyOf(tAll);
	}

	/** The offline boot before the first registry-class touch (the GT6ExtruderMoldsCensusTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The census: each list holds its 10 ids in the upstream meta order. */
	@Test
	public void theLaddersAndTheTipTokenFamiliesWalkInUpstreamMetaOrder() {
		assertEquals(ids(GT6Robotics.MOTORS), MOTOR_IDS, "MOTORS 12000-12009 (MultiItemTechnological.java:49)");
		assertEquals(ids(GT6Robotics.CONVEYERS), CONVEYER_IDS, "CONVEYERS 12040-12049 (:51)");
		assertEquals(ids(GT6Robotics.PISTONS), PISTON_IDS, "PISTONS 12060-12069 (:52)");
		assertEquals(ids(GT6Robotics.ROBOT_ARMS), ARM_IDS, "ROBOT_ARMS 12080-12089 (:53)");
		assertEquals(ids(GT6Robotics.ROBOT_TIPS), TIP_IDS, "Robot_Tip 8000-8009 (MultiItemRandomTools.java:470-479)");
		assertEquals(ids(GT6Robotics.TOOL_TOKENS), TOKEN_IDS, "Tool_Token 8500-8509 (:492-501)");
		assertEquals(60, GT6Robotics.ALL.size(), "the full 60-item census");
		assertEquals(ALL_IDS, ids(GT6Robotics.ALL), "the aggregate walk = the five lists concatenated");
	}

	/** The asset face: every item has an item model JSON and its borrowed texture PNG. */
	@Test
	public void everyItemHasAnItemModelAndABorrowedTexture() {
		for (String tPath : ALL_IDS) {
			assertNotNull(read("assets/gt6/models/item/" + tPath + ".json"), "the item model rides the generated tree: " + tPath);
			assertNotNull(read("assets/gt6/textures/item/robotics/" + tPath + ".png"), "the borrowed sprite rides main resources: " + tPath);
		}
	}

	/** The lang face: en+zh names for all 60, tooltips only where the upstream desc is non-empty (the 20 tips/tokens). */
	@Test
	public void everyItemCarriesItsEnAndZhLangFaces() throws Exception {
		var tEn = JsonParser.parseString(read("assets/gt6/lang/en_us.json")).getAsJsonObject();
		var tZh = JsonParser.parseString(read("assets/gt6/lang/zh_cn.json")).getAsJsonObject();
		for (String tPath : ALL_IDS) {
			assertTrue(tEn.has("item.gt6." + tPath), "the en name: " + tPath);
			assertTrue(tZh.has("item.gt6." + tPath), "the zh name: " + tPath);
		}
		for (String tTip : TIP_IDS) {
			assertTrue(tEn.has("item.gt6." + tTip + ".tooltip"), "the tip desc: " + tTip);
			assertTrue(tZh.has("item.gt6." + tTip + ".tooltip"), "the tip desc zh: " + tTip);
		}
		for (String tToken : TOKEN_IDS) {
			assertTrue(tEn.has("item.gt6." + tToken + ".tooltip"), "the token desc: " + tToken);
			assertTrue(tZh.has("item.gt6." + tToken + ".tooltip"), "the token desc zh: " + tToken);
		}
		// the ladder rows carry empty upstream descs (dump .tooltip= rows) — no tooltip keys, the empty-desc ruling
		for (String tLadder : MOTOR_IDS) {
			assertTrue(!tEn.has("item.gt6." + tLadder + ".tooltip"), "no ladder tooltip key: " + tLadder);
		}
		// the dump-verbatim name spot faces (tmp/gregtech.lang:10397/:9984/:10004)
		assertEquals("Compact Electric Motor (ULV)", tEn.get("item.gt6.compact_electric_motor_ulv").getAsString());
		assertEquals("Robot Arm Wrench Tip", tEn.get("item.gt6.robot_arm_wrench_tip").getAsString());
		assertEquals("Single Use Wrench", tEn.get("item.gt6.single_use_wrench").getAsString());
		assertEquals("微型电机 (ULV)", tZh.get("item.gt6.compact_electric_motor_ulv").getAsString());
		assertEquals("扳手机械臂", tZh.get("item.gt6.robot_arm_wrench_tip").getAsString());
		assertEquals("一次性扳手", tZh.get("item.gt6.single_use_wrench").getAsString());
	}

	/** The tip identity face: the Boxinator rows' never-consumed predicate walks exactly the 10 tip holders. */
	@Test
	public void theTipIdentityFaceIsExactlyTheTenTipHolders() {
		assertEquals(10, GT6Robotics.ROBOT_TIPS.size(), "the identity walk = the tip family");
	}

	/** The creative-tab join (the GT6ExtruderMolds.onBuildTabContents form) exists and walks the full family. */
	@Test
	public void theTabJoinHandlerWalksTheWholeFamily() throws Exception {
		assertNotNull(GT6Robotics.class.getDeclaredMethod("onBuildTabContents", BuildCreativeModeTabContentsEvent.class),
				"the MACHINES_TAB join handler (registered-but-tab-less is invisible in JEI)");
	}

	/**
	 * The crafting face (the datagen band's committed output): 47 recipe files — the 10
	 * tips (MultiItemRandomTools.java:481-490, robot_tip/*) + the 10 motors (the 9 live
	 * rungs + the :407 SteelMagnetic twin, component/motor_*) + the 10 conveyers +
	 * the 10 pistons + the 7 arms (MultiItemTechnological.java:404-421, component/*).
	 * The CUT faces are the negative pins: motor_ulv absent (the :404-405 wireFine pair —
	 * OP.wireFine carries no port face, the compactComponent band's FIELD_GENERATORS ULV
	 * precedent) and robot_arm_{zpm,uv,puv1} absent (no #gt6:circuit7..9 tags). The PUMPS
	 * loop rows stay pooled — no pump_* files.
	 */
	@Test
	public void theCraftingFacePoursItsFortySevenRowsWithTheDeclaredCuts() {
		int tCount = 0;
		for (String tKind : KINDS) {
			assertNotNull(read("data/gt6/recipes/robot_tip/" + tKind + ".json"), "the tip row: " + tKind);
			tCount++;
		}
		for (int i = 1; i < TIERS.size(); i++) { // motor_ulv is the declared CUT
			assertNotNull(read("data/gt6/recipes/component/motor_" + TIERS.get(i) + ".json"), "the motor row: " + TIERS.get(i));
			tCount++;
		}
		assertNotNull(read("data/gt6/recipes/component/motor_lv_steel_magnetic.json"), "the :407 SteelMagnetic twin");
		tCount++;
		assertNull(read("data/gt6/recipes/component/motor_ulv.json"), "the wireFine CUT face (the :404-405 pair)");
		for (String tTier : TIERS) {
			assertNotNull(read("data/gt6/recipes/component/conveyor_" + tTier + ".json"), "the conveyer row: " + tTier);
			assertNotNull(read("data/gt6/recipes/component/piston_" + tTier + ".json"), "the piston row: " + tTier);
			tCount += 2;
		}
		for (String tTier : List.of("ulv", "lv", "mv", "hv", "ev", "iv", "luv")) { // rungs 7-9 are the circuit CUT
			assertNotNull(read("data/gt6/recipes/component/robot_arm_" + tTier + ".json"), "the arm row: " + tTier);
			tCount++;
		}
		for (String tTier : List.of("zpm", "uv", "puv1")) {
			assertNull(read("data/gt6/recipes/component/robot_arm_" + tTier + ".json"), "the circuit CUT face: " + tTier);
		}
		assertNull(read("data/gt6/recipes/component/pump_ulv.json"), "the PUMPS loop rows stay pooled");
		assertEquals(47, tCount, "the full live crafting face");
	}

	/** The tip-row verbatim face: the wrench row is the :481 grid/keys transcription. */
	@Test
	public void theWrenchTipRowIsTheUpstreamTranscription() throws Exception {
		var tRow = JsonParser.parseString(read("data/gt6/recipes/robot_tip/wrench.json")).getAsJsonObject();
		var tPattern = tRow.getAsJsonArray("pattern");
		assertEquals("wPh", tPattern.get(0).getAsString());
		assertEquals("CMC", tPattern.get(1).getAsString());
		assertEquals(" X ", tPattern.get(2).getAsString());
		var tKey = tRow.getAsJsonObject("key");
		assertEquals("gt6:plate_curved_steel_galvanized", tKey.getAsJsonObject("P").get("item").getAsString(), "the 'P' column");
		assertEquals("gt6:circuit3", tKey.getAsJsonObject("C").get("tag").getAsString(), "the 'C' column = OD_CIRCUITS[3] (the serializer carries the tag bare, no '#')");
		assertEquals("gt6:compact_electric_motor_hv", tKey.getAsJsonObject("M").get("item").getAsString(), "the 'M' column = MOTORS[3]");
		assertEquals("gt6:tool_head_wrench_chromium", tKey.getAsJsonObject("X").get("item").getAsString(), "the 'X' column");
		assertEquals("gt6:tools/wrench", tKey.getAsJsonObject("w").get("tag").getAsString(), "the 'w' tool letter");
		assertEquals("gt6:tools/hard_hammer", tKey.getAsJsonObject("h").get("tag").getAsString(), "the 'h' tool letter");
		assertFalse(tKey.has("f") || tKey.has("d") || tKey.has("D"), "no phantom tool keys on the plain row");
		assertEquals("gt6:robot_arm_wrench_tip", tRow.getAsJsonObject("result").get("item").getAsString(), "the result");
	}

	/** RegistryObject holders to id paths (the census walk face). */
	private static List<String> ids(List<RegistryObject<Item>> aHolders) {
		List<String> tPaths = new ArrayList<>();
		for (RegistryObject<Item> tHolder : aHolders) tPaths.add(tHolder.getId().getPath());
		return tPaths;
	}

	/** A classloader read of the shipped tree (null = absent). */
	private static String read(String aPath) {
		try (InputStream tStream = GT6RoboticsCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}
}
