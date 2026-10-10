/**
 * The blit-order regression pin (task crucible-jade-blit-order, the fourth "the crucible
 * Jade tank is pure blank" report): GuiGraphics' sprite overload is
 * {@code blit(x, y, z, w, h, sprite)} — z THIRD, width FOURTH, height FIFTH. Both legs
 * carry the same order, each proven on its own body: 1.20.1 GuiGraphics.java:300-302
 * delegates {@code innerBlit(atlas, x, x+w, y, y+h, z, …)} with the :346-357 vertex order
 * {@code vertex(x1, y1, z)} seating the 3rd arg into z; 1.21.1 (neoforge-21.1.209
 * minecraft-merged-mojang-patched.jar, javap -c -p) routes public
 * {@code blit(int×5, sprite)} into private {@code blitSprite(sprite, x, y, z, w, h)}
 * whose bytecode computes {@code x2 = x + 5th}, {@code y2 = y + 6th}, {@code z = 4th}
 * and early-returns on zero width/height. The old call fed (x, y, w, h, 0, sprite):
 * height landed in the 5th slot as 0 → a zero-height quad → nothing drawn, and the
 * int×5 overload can't flag it at compile time.
 *
 * <p>The machine proof runs the REAL {@code render()} end to end: a GuiGraphics subclass
 * that records the {@code blit(int×5, TextureAtlasSprite)} arguments instead of drawing
 * (allocated through the house Unsafe.allocateInstance form — GuiGraphics is not
 * offline-constructible; the GT6AnvilBlockEntityTest.BagPlayer shape), with the sprite
 * resolution faked by a blank Minecraft subclass answering the atlas lookup directly
 * (a blank ModelManager would class-init Sheets → FML's ModLoader and NPE offline, so
 * the whole ModelManager/AtlasSet/TextureAtlas leg is bypassed instead of faked).
 * Under the old argument order this suite is RED (z=64, w=12, h=0); after the fix GREEN.
 * The overlay is sized before rendering exactly as the official ProgressStyle chain does
 * (jade-1201-forge-src impl/ui/ProgressStyle.java:91-94 — size() then render()).
 */
package gregtech6.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GT6ContentFaceElementBlitOrderTest {

	/** The element's sprite seat (any block-atlas path — the resolution chain is faked). */
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("gt6",
			"block/materialicons/rough/block_raw");
	/** An opaque non-trivial tint so the color-wrap assertion can't pass on a degenerate value. */
	private static final int TINT = 0xFF8040C0;

	/** The stub sprite handed out by the faked atlas lookup (built after the vanilla boot). */
	private static TextureAtlasSprite sSprite;

	private static final float X = 3.0F;
	private static final float Y = 5.0F;
	private static final float W = 64.0F;
	private static final float H = 12.0F;

	/** The named atlas stub (the GT6SiftingTableNeiModelTest form — offline-safe sprite). */
	private static final class NamedSprite extends TextureAtlasSprite {
		private NamedSprite(ResourceLocation aName) {
			super(aName,
					new net.minecraft.client.renderer.texture.SpriteContents(aName,
							new FrameSize(16, 16),
							new com.mojang.blaze3d.platform.NativeImage(16, 16, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					1024, 1024, 0, 0);
		}
	}

	/**
	 * The recording double: overrides the sprite blit + setColor, records, never draws.
	 * Allocated by Unsafe — the private ctor chains the only ctor shared by both legs but
	 * never runs (GuiGraphics takes no offline-constructible dependencies).
	 */
	private static final class RecordingGraphics extends GuiGraphics {
		/** The recorded {@code blit(int×5, sprite)} argument tuple. */
		record BlitCall(int x, int y, int z, int w, int h, ResourceLocation sprite) {}

		static final List<Object> EVENTS = new ArrayList<>();

		private RecordingGraphics() {
			super(null, null); // never runs — the Unsafe allocation form
		}

		@Override
		public void blit(int aX, int aY, int aZ, int aW, int aH, TextureAtlasSprite aSprite) {
			EVENTS.add(new BlitCall(aX, aY, aZ, aW, aH, aSprite.contents().name()));
		}

		@Override
		public void setColor(float aR, float aG, float aB, float aA) {
			EVENTS.add(new float[] {aR, aG, aB, aA});
		}
	}

	@BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline (the GTOfflineRenderTestBase recipe)
		}
		sSprite = new NamedSprite(TEXTURE);
		injectOfflineAtlas();
	}

	@BeforeEach
	void clearRecording() {
		RecordingGraphics.EVENTS.clear();
	}

	/** THE pin: the recorded args are (x, y, 0=3rd, w=4th, h=5th) with h > 0. Red under the old order. */
	@Test
	void blitArgumentsCarryXYThenZeroZThenWidthThenHeight() {
		GT6ContentFaceElement tElement = new GT6ContentFaceElement(TEXTURE, TINT);
		tElement.size(new Vec2(W, H)); // the ProgressStyle.render official chain sizes first (:91-94)
		tElement.render(newRecordingGraphics(), X, Y, W, H);

		RecordingGraphics.BlitCall tBlit = soleBlit();
		assertEquals((int) X, tBlit.x(), "the 1st arg is x");
		assertEquals((int) Y, tBlit.y(), "the 2nd arg is y");
		assertEquals(0, tBlit.z(), "the 3rd arg is z=0 — the old order put the WIDTH here");
		assertEquals((int) W, tBlit.w(), "the 4th arg is width — the old order put the HEIGHT here");
		assertEquals((int) H, tBlit.h(), "the 5th arg is height — the old order fed 0 here (the blank tank)");
		assertTrue(tBlit.h() > 0, "a non-positive height draws a zero-height quad = pure blank");
		assertEquals(TEXTURE, tBlit.sprite(), "the resolved sprite rides through untouched");
	}

	/** The tint chain keeps its wrap: setColor(tint) → blit → setColor(reset). */
	@Test
	void theTintChainStillWrapsTheBlit() {
		GT6ContentFaceElement tElement = new GT6ContentFaceElement(TEXTURE, TINT);
		tElement.size(new Vec2(W, H));
		tElement.render(newRecordingGraphics(), X, Y, W, H);

		assertEquals(3, RecordingGraphics.EVENTS.size(), "exactly: tint color, blit, reset color");
		float[] tTint = (float[]) RecordingGraphics.EVENTS.get(0);
		assertEquals(0x80 / 255.0F, tTint[0], 1e-6, "the tint red channel is unpacked from ARGB");
		assertEquals(0x40 / 255.0F, tTint[1], 1e-6, "the tint green channel");
		assertEquals(0xC0 / 255.0F, tTint[2], 1e-6, "the tint blue channel");
		assertEquals(1.0F, tTint[3], 1e-6, "the tint alpha channel");
		assertTrue(RecordingGraphics.EVENTS.get(1) instanceof RecordingGraphics.BlitCall,
				"the blit sits between the tint and the reset");
		float[] tReset = (float[]) RecordingGraphics.EVENTS.get(2);
		assertEquals(1.0F, tReset[0], 1e-6, "the color is reset to white after the blit");
	}

	private static RecordingGraphics.BlitCall soleBlit() {
		List<RecordingGraphics.BlitCall> tBlits = RecordingGraphics.EVENTS.stream()
				.filter(aEvent -> aEvent instanceof RecordingGraphics.BlitCall)
				.map(aEvent -> (RecordingGraphics.BlitCall) aEvent)
				.toList();
		assertEquals(1, tBlits.size(), "exactly one sprite blit per render");
		return tBlits.get(0);
	}

	private static RecordingGraphics newRecordingGraphics() {
		try {
			return (RecordingGraphics) theUnsafe().allocateInstance(RecordingGraphics.class);
		} catch (InstantiationException tBlocked) {
			throw new AssertionError("the recording graphics could not be allocated", tBlocked);
		}
	}

	/**
	 * The offline sprite chain, one hop: a blank Minecraft subclass whose
	 * {@code getTextureAtlas} hands back the stub sprite directly, injected into the
	 * package-private static {@code instance} field. (A blank ModelManager is NOT injectable
	 * offline — its &lt;clinit&gt; loads Sheets whose Forge patch derefs FML's ModLoader and
	 * NPEs — so the whole ModelManager/AtlasSet/TextureAtlas leg is bypassed instead of
	 * faked; field names checked identical on both legs anyway.)
	 */
	private static void injectOfflineAtlas() {
		try {
			setStatic(Minecraft.class, "instance", theUnsafe().allocateInstance(StubMinecraft.class));
		} catch (ReflectiveOperationException tBroken) {
			throw new AssertionError("the offline sprite chain could not be injected", tBroken);
		}
	}

	/** The never-constructed client double (the BagPlayer Unsafe form, GameConfig ctor shared by both legs). */
	private static final class StubMinecraft extends Minecraft {
		private StubMinecraft() {
			super((net.minecraft.client.main.GameConfig) null); // never runs — the Unsafe allocation form
		}

		@Override
		public java.util.function.Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation aAtlas) {
			return aId -> sSprite;
		}
	}

	private static sun.misc.Unsafe theUnsafe() {
		try {
			Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			tTheUnsafe.setAccessible(true);
			return (sun.misc.Unsafe) tTheUnsafe.get(null);
		} catch (ReflectiveOperationException tBlocked) {
			throw new AssertionError("sun.misc.Unsafe unavailable", tBlocked);
		}
	}

	private static void setStatic(Class<?> aClass, String aName, Object aValue)
			throws ReflectiveOperationException {
		Field tField = aClass.getDeclaredField(aName);
		tField.setAccessible(true);
		tField.set(null, aValue);
	}
}
