package gregtech6.client.ore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.ore.GTOreBlock;
import gregtech6.client.render.GTRenderModelListener;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.registry.GT6OreBlocks;

/**
 * The client-side wiring of the ore baked models (task p30-ore-3-datagen, the card-local
 * {@code @EventBusSubscriber} shape — {@code GTWireClientListener} for the bake dispatch +
 * {@code GTCFoamTintListener} for the colour halves; Dist.CLIENT, the dedicated server
 * never loads this class). Three jobs:
 *
 * <p>① PARAM TABLE ({@link FMLClientSetupEvent}, after registration): every ore registry
 * path → its immutable {@link GTOreBakedModel.Params} ({@code GTOreBakedModel.buildParams()},
 * the same registration walk the datagen face consumes, so path drift is structurally
 * impossible).
 *
 * <p>② BAKE DISPATCH ({@code ModelEvent.ModifyBakingResult}, the wire listener body): every
 * baked key whose path sits on the table — the per-state key {@code gt6:<path>#...} AND the
 * plain item key {@code gt6:<path>} — is replaced by one cached {@link GTOreBakedModel} per
 * path, so the inventory form renders the same dual-sprite model (the item display
 * transforms ride the shared placeholder ancestry through the static-property delegates).
 *
 * <p>③ TINT (the spec ④ fRGBa face): the WORLD half retired (task p38-issue2-ore-baked-tint)
 * — the colour is baked into the overlay quads' vertex data by {@link GTOreBakedModel} from
 * {@code Params.tintARGB()} at {@code tintARGBOf}, the p32 machine-domain ruling (the
 * runtime {@code BlockColor} route rendered achromatic in the live client; the baked-vertex
 * route cannot be dropped by any chunk builder). {@link #oreTintARGB} stays as the
 * unregistered pure seam the tests drive (the {@code GTMachinePaintTint.blockColor}
 * reference form). The ITEM half stays: the ore items ARE GTMaterialPrefixBlockItems — the
 * same colour seam the 3773 storage items use, registered here over the ore subset because
 * GTClientHandlers' roster predates the ore universe (inert on the baked quads — none
 * carries tintIndex 0 any more — but harmless, and the seam survives for them).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTOreClientListener {

	/** Registry path → render params (built once, read on bake worker threads). */
	private static final Map<String, GTOreBakedModel.Params> PARAMS = new ConcurrentHashMap<>();

	private static volatile boolean sBuilt = false;

	private GTOreClientListener() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent aEvent) {
		buildParams();
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		if (!sBuilt) buildParams(); // ordering guard — client setup always ran first; belt and suspenders
		Map<String, GTOreBakedModel> tModels = new ConcurrentHashMap<>();
		//? if forge {
		// 1.20.1: the map key is a ResourceLocation, read through its toString
		// ("ns:path[#variant]" — the GTWireClientListener parse), path = the segment
		// before the optional '#' separator; sprites ride the static block atlas.
		for (var tEntry : aEvent.getModels().entrySet()) {
			String tKeyString = tEntry.getKey().toString();
			if (!tKeyString.startsWith(GTRenderModelListener.MOD_ID + ":")) continue;
			String tPath = tKeyString.substring(GTRenderModelListener.MOD_ID.length() + 1).split("#", 2)[0];
			GTOreBakedModel.Params tParams = PARAMS.get(tPath);
			if (tParams == null) continue;
			// one model instance per block — per-state and item keys share the placeholder ancestry
			tEntry.setValue(tModels.computeIfAbsent(tPath,
					tP -> new GTOreBakedModel(tEntry.getValue(), tParams)));
		}
		//? } else {
		/*
		// 1.21.1 (the research.p32-r-ore-overlay-render deltas 3+4): the key is the typed
		// ModelResourceLocation record (id, variant) — the namespace/path ride the id()
		// ResourceLocation (the record has NO getNamespace/getPath of its own — compile-probe
		// 2026-09-21; getVariant() exists, unused here), no toString split; sprites ride the
		// event's bake-time getTextureGetter() (worker-thread-safe, no static Minecraft
		// dereference). ModelEvent.java:48-84 + ModelResourceLocation record, the 1.21.1 sources.
		Function<net.minecraft.client.resources.model.Material,
				net.minecraft.client.renderer.texture.TextureAtlasSprite> tGetter = aEvent.getTextureGetter();
		for (var tEntry : aEvent.getModels().entrySet()) {
			net.minecraft.client.resources.model.ModelResourceLocation tKey = tEntry.getKey();
			if (!tKey.id().getNamespace().equals(GTRenderModelListener.MOD_ID)) continue;
			String tPath = tKey.id().getPath(); // the params table is keyed by the registry path, variant-free
			GTOreBakedModel.Params tParams = PARAMS.get(tPath);
			if (tParams == null) continue;
			// one model instance per block — per-state and item keys share the placeholder ancestry
			tEntry.setValue(tModels.computeIfAbsent(tPath,
					tP -> new GTOreBakedModel(tEntry.getValue(), tParams, tGetter)));
		}
		*/
		//? }
	}

	/**
	 * The pure seam the tests drive (unregistered since p38-issue2-ore-baked-tint — the
	 * world route retired, the colour now baked into the overlay vertices): index 0 over an
	 * ore block = {@code fRGBa[prefix.mState]} (PrefixBlock.java:279-282), delegating to the
	 * single encode {@link GTOreBakedModel#tintARGBOf}.
	 */
	public static int oreTintARGB(@Nullable BlockState aState, int aTintIndex) {
		if (aTintIndex != 0 || aState == null || !(aState.getBlock() instanceof GTOreBlock tBlock)) return -1;
		return GTOreBakedModel.tintARGBOf(tBlock.material, tBlock.prefix);
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item aEvent) {
		// the ore items are GTMaterialPrefixBlockItems — the SHARED colour seam (spec ④
		// "既有 blockColor 复用"), registered over the ore subset (the storage roster in
		// GTClientHandlers predates this universe)
		ItemColor tColor = GTMaterialPrefixBlockItem.tintColor();
		aEvent.getItemColors().register(tColor,
				GT6OreBlocks.items().values().stream().map(h -> h.get()).toArray(net.minecraft.world.item.Item[]::new));
	}

	/** The param table (idempotent, test-callable — materials are post-MT.init at client setup). */
	public static synchronized void buildParams() {
		if (sBuilt) return;
		PARAMS.putAll(GTOreBakedModel.buildParams());
		sBuilt = true;
	}

	/** The params of one registry path (tests / diagnostics). */
	@Nullable
	public static GTOreBakedModel.Params paramsFor(String aRegistryPath) {
		return PARAMS.get(aRegistryPath);
	}

	/** The table size — 3922 ore paths (smoke assertion). */
	public static int paramsCount() {
		return PARAMS.size();
	}

	/** Test seam: clears the table (registry is JVM-global across tests). */
	public static void clearForTest() {
		PARAMS.clear();
		sBuilt = false;
	}
}
