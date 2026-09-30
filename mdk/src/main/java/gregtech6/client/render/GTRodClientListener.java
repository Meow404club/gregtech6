package gregtech6.client.render;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Logistics;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;

/**
 * The client-side wiring of the rod connector family models (task rod-render-pool, the
 * {@link gregtech6.client.wire.GTWireClientListener} shape: card-local
 * {@code @EventBusSubscriber}, GT6Mod/GTModBusListener untouched; Dist.CLIENT — the
 * dedicated server never loads this class). Two jobs:
 *
 * <p>① PARAM TABLE (main-thread, at FMLClientSetupEvent — after registration, before the
 * first resource reload): every rod block registry path → its immutable
 * {@link GTRodBakedModel.Params} (borrowed texture set, PX_P diameter, overlay bands)
 * plus the tint carrier block (the pipe rows; the axles ride null — the paint-tint
 * coverage for the axle family is the tint-coverage-batch card's defer). The paths come
 * from the registration tables themselves ({@link GTFluidPipes#ROWS},
 * {@link GTItemPipes#ROWS}, {@link GT6Logistics#LOGISTICS_WIRE},
 * {@link GT6Kinetics#AXLE_BLOCKS}), so path drift is structurally impossible.
 *
 * <p>② BAKE DISPATCH ({@link ModelEvent.ModifyBakingResult}, the
 * {@link GTRenderModelListener} discipline): replaces the baked model of every rod key
 * with one cached {@link GTRodBakedModel} per block. The keys are the per-state
 * ModelResourceLocations ({@code gt6:<path>#connections=<0..63>} /
 * {@code gt6:<path>#axis=<x|y|z>}) AND the plain item-model id ({@code gt6:<path>}) —
 * the wire listener's walk (vanilla ModelBakery loads one top-level model per BLOCK
 * STATE plus a separate item model; the blockstate JSON's model-file paths never appear
 * as keys). Wrapping the item key too gives the inventory form the upstream
 * {@code worldObj == null} N-S segment. The JSON blockstate models stay as the fallback
 * carriers and the item parents (the addWireFamily posture — the datagen shape of the
 * pipe/axle rows is UNCHANGED).
 *
 * <p>Order safety with the {@link GTMachineTintModel} bake walk (both hooks legally
 * replace the same entries): this model extends GTDynamicBakedModel, so the tint walk's
 * {@code instanceof} guard skips it — and if the tint walk ran first, this
 * replacement's built-in tint half reproduces its semantics exactly (the tint dispatch
 * is still {@link GTMachinePaintTint#tintMaterialOf}, the single colour source).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTRodClientListener {

	/** Registry path → render params + tint carrier (built once, read on bake worker threads). */
	private static final Map<String, Entry> PARAMS = new ConcurrentHashMap<>();

	private static volatile boolean sBuilt = false;

	/** One row's bake identity: the model params plus the tint carrier (null = untinted). */
	public record Entry(GTRodBakedModel.Params params, @Nullable Block block) {}

	private GTRodClientListener() {
	}

	/** The borrowed pipe art families (the tex-pipe-textures pairs, the datagen shared models). */
	private static final ResourceLocation WOOD_PIPE = new ResourceLocation("gt6", "block/materialicons/wood/pipe_side");
	private static final ResourceLocation WOOD_PIPE_OVERLAY = new ResourceLocation("gt6", "block/materialicons/wood/pipe_side_overlay");
	private static final ResourceLocation COPPER_PIPE = new ResourceLocation("gt6", "block/materialicons/copper/pipe_side");
	private static final ResourceLocation COPPER_PIPE_OVERLAY = new ResourceLocation("gt6", "block/materialicons/copper/pipe_side_overlay");
	private static final ResourceLocation PIPE_RESTRICTOR = new ResourceLocation("gt6", "block/iconsets/pipe_restrictor");
	private static final ResourceLocation LOGISTICS_WIRE = new ResourceLocation("gt6", "block/iconsets/logistics_wire");
	private static final ResourceLocation LOGISTICS_WIRE_OVERLAY = new ResourceLocation("gt6", "block/iconsets/logistics_wire_overlay");
	private static final ResourceLocation AXLE = new ResourceLocation("gt6", "block/axle");

	@SubscribeEvent
	public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent aEvent) {
		buildParams();
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		if (!sBuilt) buildParams(); // ordering guard — client setup always ran first; belt and suspenders
		Map<String, GTRodBakedModel> tModels = new ConcurrentHashMap<>();
		// the wire listener's key walk: path = the segment before the optional '#' variant
		for (var tEntry : aEvent.getModels().entrySet()) {
			String tKeyString = tEntry.getKey().toString();
			if (!tKeyString.startsWith(GTRenderModelListener.MOD_ID + ":")) continue;
			String tPath = tKeyString.substring(GTRenderModelListener.MOD_ID.length() + 1).split("#", 2)[0];
			Entry tEntry2 = PARAMS.get(tPath);
			if (tEntry2 == null) continue;
			// one model instance per block — per-state keys and the item key share it
			tEntry.setValue(tModels.computeIfAbsent(tPath,
					tP -> new GTRodBakedModel(tEntry.getValue(), tEntry2.params(), tEntry2.block())));
		}
	}

	/**
	 * The param table (idempotent, test-callable). Registry dereference is safe: mod
	 * loading finished long before client setup / the first bake.
	 */
	public static synchronized void buildParams() {
		if (sBuilt) return;
		// the fluid pipe matrix — wood family rows ride the wood pair, everything else
		// the copper pair (the datagen pipeBlockstate dispatch verbatim)
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			boolean tWood = tRow.material().blockFamily() == GTFluidPipes.PipeBlockFamily.WOODEN;
		PARAMS.put(tRow.path(), new Entry(new GTRodBakedModel.Params(
					tWood ? WOOD_PIPE : COPPER_PIPE,
					List.of(tWood ? WOOD_PIPE_OVERLAY : COPPER_PIPE_OVERLAY),
					tRow.variant().diameterPx),
					GTFluidPipes.BLOCKS_BY_PATH.get(tRow.path()).get()));
		}
		// the item pipe rows — the copper pair; the restrictive variants stack the
		// upstream third pass (the PIPE_RESTRICTOR decal, PipeItem :280)
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
			List<ResourceLocation> tOverlays = tRow.variant().suffix.startsWith("restrictive")
					? List.of(COPPER_PIPE_OVERLAY, PIPE_RESTRICTOR) : List.of(COPPER_PIPE_OVERLAY);
			PARAMS.put(tRow.path(), new Entry(new GTRodBakedModel.Params(
					COPPER_PIPE, tOverlays, tRow.variant().diameterPx),
					GTItemPipes.BLOCKS_BY_PATH.get(tRow.path()).get()));
		}
		// the logistics wire — the dedicated pair, upstream NBT_DIAMETER PX_P[6] (Loader :1819)
		PARAMS.put(GT6Logistics.WIRE_PATH, new Entry(new GTRodBakedModel.Params(
				LOGISTICS_WIRE, List.of(LOGISTICS_WIRE_OVERLAY), 6),
				GT6Logistics.LOGISTICS_WIRE.get()));
		// the 44 axles — the borrowed static axle sprite, no overlays, NO tint (the
		// tint-coverage-batch card's declared defer; block = null keeps the model untinted)
		for (var tAxle : GT6Kinetics.AXLE_BLOCKS.values()) {
			PARAMS.put(tAxle.getId().getPath(), new Entry(new GTRodBakedModel.Params(
					AXLE, List.of(), tAxle.get().diameterPx), null));
		}
		sBuilt = true;
	}

	/** The params of one registry path (tests / diagnostics). */
	@Nullable
	public static Entry entryFor(String aRegistryPath) {
		return PARAMS.get(aRegistryPath);
	}

	/** The table size — 280 fluid + 18 item + 1 logistics + 44 axles (smoke assertion). */
	public static int paramsCount() {
		return PARAMS.size();
	}

	/** Test seam: clears the table (registry is JVM-global across tests). */
	public static void clearForTest() {
		PARAMS.clear();
		sBuilt = false;
	}
}
