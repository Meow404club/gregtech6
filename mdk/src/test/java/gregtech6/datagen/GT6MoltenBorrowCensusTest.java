package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Offline sha256 census pin for task r11b-crucible-molten-art — the upstream per-set
 * molten grayscale art borrowed BYTE-IDENTICAL into every port materialicons set folder
 * ({@code assets/gt6/textures/block/materialicons/<set>/molten.png} + {@code .png.mcmeta},
 * the bedrockdrill borrow rules, the p20/p30 borrow precedents). The upstream census
 * (tmp/gt6-1.7.10 {@code assets/gregtech/textures/blocks/materialicons/<SET>/}, 2026-10-03):
 * the upstream sets ship THREE distinct molten.png arts —
 * <ul>
 * <li>the STANDARD animated strip (16x320, 20 frames): 27 of the port's 38 sets;</li>
 * <li>the SOFT family (16x512, 32 frames): cube/cube_shiny/fine/food/glass/paper/powder/
 *     prismarine/sand/wood;</li>
 * <li>the RAD singleton (16x320): rad;</li>
 * </ul>
 * plus ONE shared animation mcmeta (frametime 2, the 38-entry ping-pong). The upstream
 * GAS/PLASMA static art (141 B, no mcmeta) has NO port set folder and is NOT borrowed —
 * those sets fall to rough/ at the dispatch (the declared deviation, GT6CrucibleDatagen).
 * Acceptance ⑤ pixel-level spot check (2026-10-03, PIL RGBA decode): metallic/glass/rad
 * decoded pixel-identical to the upstream files.
 */
public class GT6MoltenBorrowCensusTest {

	/** The standard 16x320 art — sha256 of the shared bytes (upstream md5 886502b3). */
	static final String STANDARD_SHA = "a308af60c281c5684966bc739a96034a7a4d7ba2e06befdeb1587dc2d571e87e";
	/** The soft 16x512 art (upstream md5 f2e2c906). */
	static final String SOFT_SHA = "9dfa1eb2f91d72863a4d042dd0784c8f459c918b13eebee0f873a458aa4a7f55";
	/** The RAD singleton art (upstream md5 a5042e5c). */
	static final String RAD_SHA = "684b25708938186381f89030fc867c9d1c026614ba808132c5c116dcc18f795b";
	/** The one shared animation mcmeta (upstream md5 2ed0c5a3). */
	static final String MCMETA_SHA = "6a19bef21d66aac8fdcd8032751057733e321446b304d38f5ee8ba35efe99a1d";

	private static final List<String> STANDARD_SETS = List.of(
			"brick", "copper", "diamond", "dull", "emerald", "fiery", "flint",
			"gem_horizontal", "gem_vertical", "hex", "lapis", "leaf", "lignite",
			"magnetic", "metallic", "netherstar", "none", "opal", "quartz",
			"redstone", "rough", "rubber", "ruby", "shards", "shiny", "space", "stone");
	private static final List<String> SOFT_SETS = List.of(
			"cube", "cube_shiny", "fine", "food", "glass", "paper", "powder",
			"prismarine", "sand", "wood");
	private static final List<String> RAD_SETS = List.of("rad");

	@Test
	public void everyPortSetFolderCarriesTheBorrowedMoltenArt() throws Exception {
		List<String> tAll = new ArrayList<>(STANDARD_SETS);
		tAll.addAll(SOFT_SETS);
		tAll.addAll(RAD_SETS);
		assertEquals(38, tAll.size(), "the census covers exactly the 38 port materialicons set folders");
		for (String tSet : tAll) {
			String tSha = sha256("assets/gt6/textures/block/materialicons/" + tSet + "/molten.png");
			String tExpected = STANDARD_SETS.contains(tSet) ? STANDARD_SHA : SOFT_SETS.contains(tSet) ? SOFT_SHA : RAD_SHA;
			assertEquals(tExpected, tSha, tSet + "/molten.png is the byte-identical upstream art");
			assertEquals(MCMETA_SHA, sha256("assets/gt6/textures/block/materialicons/" + tSet + "/molten.png.mcmeta"),
					tSet + "/molten.png.mcmeta is the shared upstream animation metadata");
		}
	}

	/** The PNG IHDR dimensions — the animated strips must keep their frame counts (320/8 = 20 frames, 512/8 = 32). */
	@Test
	public void theAnimatedStripsKeepTheirUpstreamFrameCounts() throws Exception {
		assertEquals(320, pngHeight("assets/gt6/textures/block/materialicons/metallic/molten.png"));
		assertEquals(320, pngHeight("assets/gt6/textures/block/materialicons/rad/molten.png"));
		assertEquals(512, pngHeight("assets/gt6/textures/block/materialicons/glass/molten.png"));
	}

	private static int pngHeight(String aPath) throws Exception {
		byte[] tPng = read(aPath);
		// the IHDR big-endian width/height sit at fixed offsets 16..24 of every PNG
		return ((tPng[20] & 255) << 24) | ((tPng[21] & 255) << 16) | ((tPng[22] & 255) << 8) | (tPng[23] & 255);
	}

	private static String sha256(String aPath) throws Exception {
		MessageDigest tDigest = MessageDigest.getInstance("SHA-256");
		tDigest.update(read(aPath));
		StringBuilder rHex = new StringBuilder();
		for (byte tByte : tDigest.digest()) rHex.append(String.format("%02x", tByte));
		return rHex.toString();
	}

	private static byte[] read(String aPath) throws Exception {
		try (InputStream tStream = GT6MoltenBorrowCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " must exist on the classpath (the borrow landed)");
			return tStream.readAllBytes();
		}
	}
}
