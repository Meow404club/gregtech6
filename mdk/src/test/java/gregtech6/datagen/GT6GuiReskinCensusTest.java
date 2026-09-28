/**
 * Pin h (task r8-gui-reskin-amazawa) — the machine GUI reskin census. Every PNG under
 * {@code assets/gt6/textures/gui/machines/} was reskinned from the amazawa resource
 * pack (Modrinth "tfc-amazawa-light-gui" 1.0.5g, Apache-2.0, author 天沢香, chat
 * authorization shipped at docs/licenses/amazawa-gui-authorization.png — the
 * attribution trio lives in the assets/README.md reskin section). The MANIFEST table
 * below is the single source of truth for that wave: our lowercase target stem
 * (ResourceLocation path legality, ResourceLocation.java:213-232), the PascalCase
 * amazawa source file (matched case-insensitively; anvilbend folds amazawa
 * AnvilBendingBig.png per the assets/README.md Small/Big fold row), the reskinned
 * bytes' sha256, and the canvas size, which equals the replaced upstream size
 * per-file (256x256 on all 73, verified at match time — the sizes pin keeps any
 * future reskin honest the same way).
 *
 * <p>Like pin g (GT6TextureCensusTest) this is pure read-only JUnit, no vanilla
 * bootstrap, touching only committed trees. The (176,0) 16x16 spot pin guards the
 * vanilla-Screen-leg progress-arrow overlay, which blits that UV window from the same
 * background PNG (upstream ContainerClientBasicMachine.java:48-65 lineage) — a reskin
 * that drops the arrow strip would silently break every machine GUI's progress bar.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GT6GuiReskinCensusTest {

    /** The reskinned machine GUI folder, under the mdk root. */
    private static final Path GUI_MACHINES =
        Path.of("src", "main", "resources", "assets", "gt6", "textures", "gui", "machines");

    /**
     * The authorization screenshot (attribution trio, decisions.r8-gui-visual-source) —
     * repo-root docs/, a sibling of the mdk root.
     */
    private static final Path LICENSE_SHOT = Path.of("docs", "licenses", "amazawa-gui-authorization.png");

    /** ResourceLocation path legality, lowercase-only form (vanilla 1.20.1 [a-z0-9/._-]). */
    private static final Pattern LEGAL_STEM = Pattern.compile("[a-z0-9_-]+");

    /** The UV window the vanilla leg blits the progress arrow from (same background PNG). */
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_W = 16, ARROW_H = 16;

    /**
     * One row per reskinned PNG: {target stem, amazawa source file, sha256 of the
     * reskinned bytes, width, height}. 73 rows = the full intersection of our
     * gui/machines set with the pack's gregtech domain (anvilbend via the Big fold).
     */
    private static final String[][] MANIFEST = {
        {"alloying", "Alloying.png", "c93bb15e340b98314f0053acddda047e65fee4808b4aafe095f8b0a917ed365b", "256", "256"},
        {"anvil", "Anvil.png", "41fc7d5914113898a2b41632cec80e797060583d8aeee7c833e6a0dd4030ca9a", "256", "256"},
        {"anvilbend", "AnvilBendingBig.png", "41d60ba2f35458f4f083ad9f4d5b625cb97d7ddc54b21776271d449ed54cca39", "256", "256"},
        {"autoclave", "Autoclave.png", "35fa142291628237202338ac0294ad1af28b8efc71b9e79e580a6d3e07d46672", "256", "256"},
        {"bath", "Bath.png", "0bd23ee91c081dd5ac96401e09551ad17ffb0075ee767faafa11150eccdeae1d", "256", "256"},
        {"boxinator", "Boxinator.png", "611f06ff139214eade75ecf24a5519a4a4fdc43fdf1c6d4185b130b61f3599e2", "256", "256"},
        {"bumblelyzer", "Bumblelyzer.png", "62cee037e48102460c038dfc7a1d6b496608e0bc692caad09901387fe01afa31", "256", "256"},
        {"bumbliary", "Bumbliary.png", "d8273f498408a31a2cf1f5c3b515f38c50fde5ecf5c9ab7f5ab2317b76912ac8", "256", "256"},
        {"bumbliaryadvanced", "BumbliaryAdvanced.png", "6fb9c48274095e8ef7a9b4e610dd0d21551b1e99985a17715d42b414c7f114e1", "256", "256"},
        {"burnmixer", "BurnMixer.png", "99f2d031657383db8799bc67108592b9cf985456344379d591e9843b4507e88f", "256", "256"},
        {"canner", "Canner.png", "7420b4398af0471985cb0d4b13fbdc3f7e2bafcc174b210441f0b2be6ec2dc91", "256", "256"},
        {"catalyticcracking", "CatalyticCracking.png", "be0ff46d7bbf69cdcff899924095dc9753a1530c9057069945b6164b1912eade", "256", "256"},
        {"centrifuge", "Centrifuge.png", "b6f528a4cf7ef1ef364f25ee6ab998e88aedd82f2a226adb56d55999e4a7e953", "256", "256"},
        {"clustermill", "ClusterMill.png", "1268ae82dde623a6c4ed629b48975d73c41c86275e81ca4c1505d07f5079dce8", "256", "256"},
        {"coagulator", "Coagulator.png", "8042cf0762e1735f6d696d9a4105616ce30a341b0cad854c2e6198a61460d29b", "256", "256"},
        {"cokeoven", "CokeOven.png", "bdca3b3399173ca025771c598c57ac8ba6b6dd207263750ec693669dcbe0d03f", "256", "256"},
        {"compressor", "Compressor.png", "d5552e8bcf6851f0dea58dcfb866b6dcb4a63bc4ff0ba09781bc2b4f84df8b6c", "256", "256"},
        {"cooker", "Cooker.png", "625561bed59f0ba99bfc17839914a6330231565258d71692d1224d4c81034d0b", "256", "256"},
        {"crafting", "Crafting.png", "1650b226f9dbcd521ac0e7899dbb4668f01020e156a19cfcc6bc39fffcde0ab0", "256", "256"},
        {"crafting2by2", "Crafting2By2.png", "df9ab8cc4a492803bcc37665700a090d93b60c4212d57505156f2223e830965f", "256", "256"},
        {"crusher", "Crusher.png", "eafe386f4a77d1fa5d36a4177fc2cb55083aa41f419d1f35411ba061c745f4a1", "256", "256"},
        {"cryodistillationtower", "CryoDistillationTower.png", "1e69a7683c1dccb8823568a67ef2f497cc7290e771ecbcd960109b4760db48b2", "256", "256"},
        {"cryomixer", "CryoMixer.png", "03150282dce57762fa40bb10ead568bc3d45ed4309b38839ee299ef50494e11a", "256", "256"},
        {"crystallisationcrucible", "CrystallisationCrucible.png", "fdef7f7913aa3d647347706a9bfc80976adb1d2603c2404fd4d9f60429032aeb", "256", "256"},
        {"cutter", "Cutter.png", "cf6cd690c8e268b89db74f3812d9a478357f7eead0828d2466d9e90f1bb69cf4", "256", "256"},
        {"default", "Default.png", "918c84cfe2629d97156dc3f9416c6cd67a95b384608fd14be4278087eb9043dd", "256", "256"},
        {"distillationtower", "DistillationTower.png", "aaca08955d65bb34514a7325c0b4607aec749333aa8a713dfb2aa1d7941e9c85", "256", "256"},
        {"distillery", "Distillery.png", "cfb1839850b342436c2d999026e09666450dcec87f5128ec96d854c8328eeb45", "256", "256"},
        {"dryer", "Dryer.png", "d1fae3b57b7c56c01463cbd87d977eb0581cf14128ad4a65f1e95608f9fec7d7", "256", "256"},
        {"electrolyzer", "Electrolyzer.png", "bf22ca5b9103451bc096a550abbb85b14884fefb5638acfeae89b38ee1017959", "256", "256"},
        {"extruder", "Extruder.png", "86aa98eed02be7718369effa5f1b0c486aba3ae66f86f91b15c9004e5298f8cf", "256", "256"},
        {"fermenter", "Fermenter.png", "b063cecdc8e20e1ca5bd74558b7df3f9e9816b559f2a73888f62197da66f85e6", "256", "256"},
        {"freezer", "Freezer.png", "2796fd94a12ea018135b6f1e84852cfa55e32b2344d055ca3979abedaf3a0858", "256", "256"},
        {"fusion", "Fusion.png", "cddc841a3e215b8e15cffdd22d6b064e3efee65437e137f67b23e2cd208ce855", "256", "256"},
        {"generifier", "Generifier.png", "4fb357bf4c2b15ffd51ceacd31be9a852a8316a30478bca50e9a516e5e4069db", "256", "256"},
        {"hammer", "Hammer.png", "394f57f1f8c8eef92f7b8c9a5404bb5bd4d826b65352aa4a290804cd0a34898e", "256", "256"},
        {"implosioncompressor", "ImplosionCompressor.png", "0463764a848353a59bb64003933e63aaa9bcd357e8c3e4b7fe76500437aca773", "256", "256"},
        {"injector", "Injector.png", "4c46ca860b39b8bb114bb915285079110b350ad3ab1ea63cea8ee13f552c9427", "256", "256"},
        {"juicer", "Juicer.png", "4bca2c145dbdddec10b4c42b2a7c09e46b74d338468a28b6c6ce0e09dc4c65ac", "256", "256"},
        {"laminator", "Laminator.png", "ffe624eba09c9dca1b162148bd5381a57e6c26993b0202fe357419850ac61be0", "256", "256"},
        {"laserengraver", "LaserEngraver.png", "5756642cc611e201ac3f469d7ac614f26e2fcb79d48a69124ef32d91c2df6f58", "256", "256"},
        {"lathe", "Lathe.png", "4f65896953a794539558fde1938c08189c53d7aad1d483cc57b05cbe35691523", "256", "256"},
        {"lightning", "Lightning.png", "f3259691447fb25a6e7d11f0b669a06ca7160c8b85afff0d9b0666991424a910", "256", "256"},
        {"loom", "Loom.png", "cb08ae37481d90a21916909934a8d862006c2097d68fd57ebd9a84a1683e5cea", "256", "256"},
        {"magneticseparator", "MagneticSeparator.png", "6b0494c658355f754a27f9677e531c1e8750e2af9c5398eaf23b7d096b28efe1", "256", "256"},
        {"massfab", "Massfab.png", "8d9fa2f71d0aa69be3e083c681be74652bd52d7c0f2ac48c2a256691d32c10ca", "256", "256"},
        {"melter", "Melter.png", "e06a503906d0dcc621d6ee83aab307abfd5fb41ecddbec608cf9adcbfb3b46df", "256", "256"},
        {"mixer", "Mixer.png", "9e053f82c6dd8ab0f45d7f4cb021bbb16504d9699ce15705f2bd5605e86e233e", "256", "256"},
        {"mortar", "Mortar.png", "bc40d341fcfc2862ab5e8047c0a5a4c4a4c8ca261bc2ba1b5a3031e7e97ea07d", "256", "256"},
        {"oven", "Oven.png", "cc6f224752b815458c361911dd38456ecf3f5fdf2283cc650ed23c2dea1b099a", "256", "256"},
        {"plantalyzer", "Plantalyzer.png", "e188555f8d656ab35885673ea2c20719e1e75eeb765960a0a0295f1dfde2f801", "256", "256"},
        {"polarizer", "Polarizer.png", "c796167d91617dab6dc8dd2060eb420119a0596e3dcfa662e5c4f508530522b2", "256", "256"},
        {"press", "Press.png", "82dc06da002c1a0305a6cbf6dde738cc63fa205266a63416348dda310023ca0f", "256", "256"},
        {"pressurewasher", "PressureWasher.png", "422afd27781761776e9086ce70f06f2ab8155745f26775bb1588adf0904490eb", "256", "256"},
        {"printer", "Printer.png", "f642d762e8eafd8ea69d17d8c6dbe4185ed6071c56c36196e73d60ecdf7adf37", "256", "256"},
        {"replicator", "Replicator.png", "e23b4106a80bf0efc6b4c61820db9f391ab718fcb5673604963fae0cbb38ab55", "256", "256"},
        {"roaster", "Roaster.png", "dfea93d04a614964e933504cac276ae478a647f45c3443515f4881e6a53d5e29", "256", "256"},
        {"rollbender", "RollBender.png", "a66c3a4204ffeca771d9aba8339b8f7e00e3759dc0468a5fc013cc8ac5aedd86", "256", "256"},
        {"rollformer", "RollFormer.png", "8e47d06b55fa1a41d56c1eef0cdc0de9ea7129449be30a7696c8d721e11b4a51", "256", "256"},
        {"rollingmill", "RollingMill.png", "c8b8795d3a6d4e6f008c0767cedde4579c73a02d83c6665f5555c2727c30ea6a", "256", "256"},
        {"scannermolecular", "ScannerMolecular.png", "19e4cfb02533f25317f6b9528a723c19c382fc66cffd0717ee0589a102a11211", "256", "256"},
        {"scannervisuals", "ScannerVisuals.png", "ff247cf44f591fffd2ff92a0a2dc7544ffe3b5505ec10bdab786f4deba2809bb", "256", "256"},
        {"sharpener", "Sharpener.png", "183960d50eb46c20f51ca14518d46002c26593d3605dfa03aaa5933bb3a1e377", "256", "256"},
        {"shredder", "Shredder.png", "342a5c911b74d2d142f53de7c271ddd8647b3e2c969b9f16c629fde66efbe096", "256", "256"},
        {"sifter", "Sifter.png", "bd90d5a3529accf86f37bb2799e7ab3ce896c522b4c486e6b15c21932ef3b653", "256", "256"},
        {"slicer", "Slicer.png", "56ea38d4f81c5a4f216b37646d6e734ac38a8942c1c772fdd0ef4d7b17072292", "256", "256"},
        {"sluice", "Sluice.png", "2d252699d1ae42d8a040e4e0eeb1d799b77f64ba3f224fb1e011320cdc80f61f", "256", "256"},
        {"smelter", "Smelter.png", "e06a503906d0dcc621d6ee83aab307abfd5fb41ecddbec608cf9adcbfb3b46df", "256", "256"},
        {"squeezer", "Squeezer.png", "a18eaeeb7f203d1a23722412a0503fd8873a8549cfad3af783fa6e69c9d43b4b", "256", "256"},
        {"steamcracking", "SteamCracking.png", "847a49295932c86af69f051776bc8d6bcea939bcca1061b1924a65a70a78cad5", "256", "256"},
        {"unboxinator", "Unboxinator.png", "0dda888d5409026672f7efdbe887ff07cc2acf442a14d715ee5f369eccaaae30", "256", "256"},
        {"welder", "Welder.png", "a9558b22d7648adde07e6feb12132765e25ea15ec9973404186e0a86648eb33e", "256", "256"},
        {"wiremill", "Wiremill.png", "e9699da9e901cb541803fdc1b7c624310e3f55e14bfb2df186cfbaea285b1458", "256", "256"},
    };

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

    private static Path guiMachinesDir() {
        return mdkRoot().resolve(GUI_MACHINES);
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The wave pin: the on-disk gui/machines set is EXACTLY the manifest (bijective — a
     * file added without a row or deleted without one fails here, the pin never passes
     * vacuously), and every file matches its row: lowercase-legal stem, ImageIO canvas
     * size == the replaced upstream size, sha256 == the manifest digest.
     */
    @Test
    void everyReskinnedMachineGuiPngMatchesManifest() throws IOException {
        assertTrue(Files.isRegularFile(mdkRoot().getParent().resolve(LICENSE_SHOT)),
            "the amazawa authorization screenshot is missing — the attribution trio must ship");
        Set<String> onDisk = new HashSet<>();
        try (var walk = Files.list(guiMachinesDir())) {
            walk.filter(p -> p.getFileName().toString().endsWith(".png"))
                .forEach(p -> onDisk.add(p.getFileName().toString().replaceAll("\\.png$", "")));
        }
        Set<String> pinned = new HashSet<>();
        List<String> violations = new ArrayList<>();
        for (String[] row : MANIFEST) {
            String stem = row[0], source = row[1], sha = row[2];
            int w = Integer.parseInt(row[3]), h = Integer.parseInt(row[4]);
            pinned.add(stem);
            if (!LEGAL_STEM.matcher(stem).matches()) {
                violations.add(stem + " — ResourceLocation-illegal stem (from " + source + ")");
                continue;
            }
            Path file = guiMachinesDir().resolve(stem + ".png");
            if (!Files.isRegularFile(file)) {
                violations.add(stem + ".png — missing (reskin row " + source + " not applied)");
                continue;
            }
            BufferedImage img = ImageIO.read(file.toFile());
            if (img.getWidth() != w || img.getHeight() != h) {
                violations.add(stem + ".png — size " + img.getWidth() + "x" + img.getHeight()
                    + " != pinned upstream size " + w + "x" + h);
            }
            String actual = sha256(file);
            if (!actual.equals(sha)) {
                violations.add(stem + ".png — sha256 " + actual + " != manifest " + sha);
            }
        }
        assertEquals(pinned, onDisk,
            "gui/machines is not exactly the reskin manifest — add a row for a new file "
                + "or restore/delete the stray PNG (73/73 fully absorbed)");
        assertTrue(violations.isEmpty(), "reskin manifest violations: " + violations);
        assertTrue(MANIFEST.length >= 73,
            "only " + MANIFEST.length + " manifest rows — the wave pin must never shrink silently");
    }

    /**
     * The (176,0) 16x16 UV window of every reskinned background holds at least one
     * opaque pixel — the vanilla-Screen leg overlays the progress arrow from exactly
     * that window of the SAME PNG, so a reskin cropping it would blank every machine's
     * progress bar (spot pin from the design card open items; verified opaque on all
     * 73 amazawa files at wave time).
     */
    @Test
    void vanillaLegArrowRegionSurvivesReskin() throws IOException {
        List<String> blank = new ArrayList<>();
        for (String[] row : MANIFEST) {
            Path file = guiMachinesDir().resolve(row[0] + ".png");
            BufferedImage img = ImageIO.read(file.toFile());
            int opaque = 0;
            for (int y = ARROW_V; y < ARROW_V + ARROW_H; y++) {
                for (int x = ARROW_U; x < ARROW_U + ARROW_W; x++) {
                    if ((img.getRGB(x, y) >>> 24) != 0) {
                        opaque++;
                    }
                }
            }
            if (opaque == 0) {
                blank.add(row[0] + ".png");
            }
        }
        assertTrue(blank.isEmpty(), "reskinned PNGs lost the (176,0) arrow window: " + blank);
    }
}
