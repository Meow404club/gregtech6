package gregtech6.client.ore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
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
 * <p>③ TINT (the spec ④ fRGBa face): the ore BlockColor over the ore block universe and the
 * EXISTING {@code GTMaterialPrefixBlockItem.tintColor()} ItemColor over the ore items (the
 * ore items ARE GTMaterialPrefixBlockItems — the same colour seam the 3773 storage items
 * use, registered here over the ore subset because GTClientHandlers' roster predates the
 * ore universe). Index 0 only, world half reads {@code material.fRGBa[prefix.mState]}
 * (PrefixBlock.java:279-282), item half reads mRGBa (PrefixBlockItem.java:103) — both
 * already ported; this listener only registers them over the ore walk.
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

	/** The world-side tint half: index 0 over an ore block = {@code fRGBa[prefix.mState]} (PrefixBlock.java:279-282). */
	public static BlockColor oreBlockColor() {
		return (aState, aLevel, aPos, aTintIndex) -> oreTintARGB(aState, aTintIndex);
	}

	/** The pure seam the tests drive: the opaque ARGB for one tint index over one ore state. */
	public static int oreTintARGB(@Nullable BlockState aState, int aTintIndex) {
		if (aTintIndex != 0 || aState == null || !(aState.getBlock() instanceof GTOreBlock tBlock)) return -1;
		return fRGBaARGB(tBlock.material.fRGBa[tBlock.prefix.mState]);
	}

	/** UT.Code.getRGBInt over the fRGBa triple (the GTMaterialPrefixBlock.tintARGB encoding). */
	private static int fRGBaARGB(short[] aRGBa) {
		return 0xFF000000 | (bind8(aRGBa[0]) << 16) | (bind8(aRGBa[1]) << 8) | bind8(aRGBa[2]);
	}

	/** Upstream UT.Code.bind8 semantics: clamp to 0-255. */
	private static int bind8(long aValue) {
		return (int) Math.max(0, Math.min(255, aValue));
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(oreBlockColor(),
				GT6OreBlocks.blocks().values().stream().map(h -> h.get()).toArray(net.minecraft.world.level.block.Block[]::new));
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
