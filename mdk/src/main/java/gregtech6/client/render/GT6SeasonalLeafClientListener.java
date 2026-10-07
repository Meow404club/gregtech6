package gregtech6.client.render;

import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.easter.GT6Calendars;

/**
 * The client bake swap of the Christmas + seasonal leaves (task easter-s3-xmas-seasonal,
 * the S3 card of research.easter-egg-census id1451). Upstream GT_API_Proxy_Client.java:133-167
 * repoints the icon arrays once at proxy-after-pre-init: the Blue Spruce leaves render the
 * LEAVES_BLUESPRUCE_XMAS art while an XMAS flag is up (XMAS_IN_JULY :133-137 OR
 * XMAS_IN_DECEMBER :138-142) and the Maple leaves swap their art per month (:144-167, the
 * BROWN/YELLOW/ORANGE/RED table — pure season, no flag). The port face is the same
 * once-per-launch static semantics at the modern anchor point: the art variant models are
 * baked alongside the plain ones (this listener's {@link ModelEvent.RegisterAdditional}) and
 * {@link ModelEvent.ModifyBakingResult} repoints the leaves block's per-state keys plus its
 * item key at the seasonal baked model — the {@link GTRodClientListener} string-key walk.
 *
 * <p>Why bake-time substitution instead of a tint (the coordinator ruling 2026-10-04): the
 * seasonal art is four/five INDEPENDENT pre-coloured PNGs (no shared grayscale base — pixel
 * census: the maple base alone carries 135 distinct colours), so a BlockColors multiply can
 * never reproduce it, and upstream's own mechanism is the icon-pointer swap, not a tint.
 * The flags/month are bound once at class load (GT6Calendars static init), strictly before
 * the first resource reload, so the swap is decided once per bake — the upstream
 * compute-once-at-init shape.
 *
 * <p>Dormant upstream faces: the OPAQUE twin icons (LEAVES_CD[8] / LEAVES_AB[9]) have no
 * port carrier (one cutout_mipped leaves block per species here); the CD/AB per-meta split
 * collapses to one kind each (Blue Spruce = CD:0, Maple = AB:1).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SeasonalLeafClientListener {

	/** The XMAS blue-spruce variant model (GT6BlockStates.addTreeBlocks seasonal face). */
	static final String XMAS_MODEL_PATH = "block/blue_spruce_leaves_xmas";

	/** The leaves swap prefixes: per-state keys {@code gt6:<path>#<props>} + item keys {@code gt6:<path>#inventory}. */
	static final String XMAS_LEAVES_PREFIX = "gt6:blue_spruce_leaves#";
	static final String MAPLE_LEAVES_PREFIX = "gt6:maple_leaves#";

	private GT6SeasonalLeafClientListener() {
	}

	/**
	 * Bakes the seasonal variant models that the live flags/month will consume. Registering
	 * only the active ones keeps the inactive seasonal art out of the atlas entirely (the
	 * model JSON still ships — the census pins it — it just never stitches).
	 */
	@SubscribeEvent
	public static void onRegisterAdditional(ModelEvent.RegisterAdditional aEvent) {
		if (GT6Calendars.XMAS_IN_JULY || GT6Calendars.XMAS_IN_DECEMBER) {
			// forge 1.20.1 takes the plain RL; neoforge 21.1 wraps it in an MRL (the
			// GTBoilerClientListener.modelId leg split)
			//? if forge {
			aEvent.register(ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID, XMAS_MODEL_PATH));
			//? } else {
			/*aEvent.register(new ModelResourceLocation(
					new ResourceLocation(GTRenderModelListener.MOD_ID, XMAS_MODEL_PATH), "standalone"));*/
			//? }
		}
		GT6Calendars.MapleSeason tSeason = GT6Calendars.mapleSeasonOfMonth(GT6Calendars.sMonth);
		if (tSeason != GT6Calendars.MapleSeason.NONE) {
			String tPath = "block/maple_leaves_" + tSeason.suffix();
			//? if forge {
			aEvent.register(ResourceLocation.fromNamespaceAndPath(GTRenderModelListener.MOD_ID, tPath));
			//? } else {
			/*aEvent.register(new ModelResourceLocation(
					new ResourceLocation(GTRenderModelListener.MOD_ID, tPath), "standalone"));*/
			//? }
		}
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		// the 1.20.1 event map keys by ResourceLocation, the 1.21.1 by ModelResourceLocation
		// (which no longer extends ResourceLocation there) — the generic-key walk stays leg-neutral
		var tModels = aEvent.getModels();
		if (GT6Calendars.XMAS_IN_JULY || GT6Calendars.XMAS_IN_DECEMBER) {
			BakedModel tXmas = findBaked(tModels, GTRenderModelListener.MOD_ID + ":" + XMAS_MODEL_PATH);
			if (tXmas != null) swapLeaves(tModels, XMAS_LEAVES_PREFIX, tXmas);
		}
		GT6Calendars.MapleSeason tSeason = GT6Calendars.mapleSeasonOfMonth(GT6Calendars.sMonth);
		if (tSeason != GT6Calendars.MapleSeason.NONE) {
			BakedModel tSeasonModel = findBaked(tModels,
					GTRenderModelListener.MOD_ID + ":block/maple_leaves_" + tSeason.suffix());
			if (tSeasonModel != null) swapLeaves(tModels, MAPLE_LEAVES_PREFIX, tSeasonModel);
		}
	}

	/**
	 * The seasonal baked model by leg-neutral string prefix — forge 1.20.1 keys additional
	 * models by the plain ResourceLocation, neoforge 21.1 by the MRL over it, and the
	 * {@code toString()} of both starts with {@code <modid>:<path>} (the GTRodClientListener
	 * key-walk posture).
	 */
	private static <K> BakedModel findBaked(Map<K, BakedModel> aModels, String aPrefix) {
		for (var tEntry : aModels.entrySet())
			if (tEntry.getKey().toString().startsWith(aPrefix)) return tEntry.getValue();
		return null;
	}

	/** Repoints every per-state key AND the item key of one leaves block at the seasonal model (the rod listener's shared-instance posture — one baked cube model serves both faces). */
	private static <K> void swapLeaves(Map<K, BakedModel> aModels, String aPrefix, BakedModel aModel) {
		for (var tEntry : aModels.entrySet())
			if (tEntry.getKey().toString().startsWith(aPrefix)) tEntry.setValue(aModel);
	}
}
