/**
 * The lazy first-render bake contract of {@link GTOreBakedModel} (task
 * p33-fix-forge-ore-invisible). The forge-leg whole-ore invisibility root cause was the
 * EAGER constructor bake: {@code ModifyBakingResult} fires before the sprite upload and
 * forbids touching ModelManager (ModelEvent.java:40-43, ModelManager.java.patch), so the
 * static atlas lookup resolved against the empty atlas and baked 0 quads. These pins:
 * the constructor must not touch the sprite lookup at all, the first {@code getQuads}
 * resolves exactly once (both materials), and later queries ride the cache (identity).
 * A null-returning stub stands in for the atlas (real sprites need a live stitch).
 * Offline: the model body over a stub lookup touches no registry.
 */
package gregtech6.client.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.model.data.ModelData;

import gregtech6.client.ore.GTOreBakedModel.Params;

public class GTOreBakedModelLazyBakeTest {

	/** Counting null stub — every apply is one sprite lookup (3 per bake: base + pass-0 overlay + pass-1 outline). */
	private static GTOreBakedModel countedModel(AtomicInteger aLookups) {
		return new GTOreBakedModel(null,
				new Params(new ResourceLocation("gt6", "block/stones/granite/stone"),
						new ResourceLocation("gt6", "block/ore_copper"),
						new ResourceLocation("gt6", "block/ore_copper_overlay"), 0xFFA07828),
				aMaterial -> {
					aLookups.incrementAndGet();
					return null;
				});
	}

	/** The root-cause pin: construction happens at ModifyBakingResult time — zero lookups. */
	@Test
	public void constructorDoesNotResolveSprites() {
		AtomicInteger tLookups = new AtomicInteger();
		countedModel(tLookups);
		assertEquals(0, tLookups.get(), "the constructor must not touch the sprite lookup");
	}

	/** First getQuads bakes once (3 lookups); every later query is cache-identity stable. */
	@Test
	public void firstGetQuadsResolvesOnceThenCaches() {
		AtomicInteger tLookups = new AtomicInteger();
		GTOreBakedModel tModel = countedModel(tLookups);
		List<BakedQuad> tFirst = tModel.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
		assertEquals(3, tLookups.get(), "exactly the base + overlay + outline lookups, once");
		List<BakedQuad> tSecond = tModel.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
		assertEquals(3, tLookups.get(), "no re-resolution on later queries");
		assertSame(tFirst, tSecond, "the lazy bake result is cached by identity");
	}

	/** Per-side dispatch rides the same lazy list (null sprites → the atlas-gap empty shape). */
	@Test
	public void perSideQueryRidesTheLazyList() {
		AtomicInteger tLookups = new AtomicInteger();
		GTOreBakedModel tModel = countedModel(tLookups);
		List<BakedQuad> tUp = tModel.getQuads(null, net.minecraft.core.Direction.UP,
				RandomSource.create(), ModelData.EMPTY, null);
		assertEquals(3, tLookups.get(), "the side query triggers the one lazy bake");
		tModel.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
		assertEquals(3, tLookups.get(), "the null pass reuses the cache, no re-resolution");
		assertEquals(0, tUp.size(), "null-sprite atlas gap bakes no quad (the wire form)");
	}
}
