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

import gregtech6.client.wire.GTWireTextures;
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
 * plus the tint carrier block (the pipe/wire rows; the axles ride null — their tint
 * resolves consumer-side off the state's GTAxleBlock carrier, task axle-tint-arm). The paths come
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
 * <p>③ FLOW CHAIN (task pipe-flow-arrow-render-fix): the fluid-pipe per-state keys seat
 * the composed {@code Foam(Flow(Rod(baked)))} chain ({@link #replacementFor}) — this
 * listener is the SINGLE writer of those keys. The former GTPipeFlowClientListener
 * factory-table registration (deleted with the task) both registered the WRONG key form
 * (model-file ids {@code block/<path>} that never match a per-state bake key,
 * GTRenderModelListener class doc) and lost the last-wins race against this replacement —
 * arrows, foam and the cover snapshot never rendered on any build.
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

	/** One row's bake identity: the model params plus the tint carrier (null = the axle rows —
	 * the consumer-side axle arm keys on the state's GTAxleBlock carrier, task axle-tint-arm). */
	public record Entry(GTRodBakedModel.Params params, @Nullable Block block) {}

	private GTRodClientListener() {
	}

	/**
	 * The remaining dedicated rod art ids (the per-material SET pipe pairs live on
	 * {@link gregtech6.client.wire.GTWireTextures} since pipe-render-closeout — the
	 * shared dispatch seam with the datagen walk).
	 */
	private static final ResourceLocation PIPE_RESTRICTOR = ResourceLocation.fromNamespaceAndPath("gt6", "block/iconsets/pipe_restrictor");
	private static final ResourceLocation LOGISTICS_WIRE = ResourceLocation.fromNamespaceAndPath("gt6", "block/iconsets/logistics_wire");
	private static final ResourceLocation LOGISTICS_WIRE_OVERLAY = ResourceLocation.fromNamespaceAndPath("gt6", "block/iconsets/logistics_wire_overlay");
	private static final ResourceLocation AXLE = ResourceLocation.fromNamespaceAndPath("gt6", "block/axle");

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
			GTRodBakedModel tRod = tModels.computeIfAbsent(tPath,
					tP -> new GTRodBakedModel(tEntry.getValue(), tEntry2.params(), tEntry2.block()));
			tEntry.setValue(replacementFor(tKeyString, tRod));
		}
	}

	/**
	 * The flow-chain key test (task pipe-flow-arrow-render-fix): TRUE exactly on the
	 * FLUID-pipe per-state bake keys {@code gt6:<path>#connections=0..63} — the only keys
	 * whose BE can carry the FLOW/FOAM/RENDER snapshots (GTFluidPipeBlockEntity
	 * .getModelData). Every other rod key — the item keys, the item pipes, the logistics
	 * wire, the axles — renders the raw rod.
	 */
	static boolean isFlowChainKey(String aBakedKey) {
		int tHash = aBakedKey.indexOf('#');
		if (tHash < 0) return false;
		if (!aBakedKey.startsWith("connections=", tHash + 1)) return false;
		return GTFluidPipes.BLOCKS_BY_PATH.containsKey(
				aBakedKey.substring(GTRenderModelListener.MOD_ID.length() + 1, tHash));
	}

	/**
	 * The replacement value for one rod key (task pipe-flow-arrow-render-fix): the fluid
	 * per-state keys seat the composed {@code Foam(Flow(Rod(baked)))} chain — the foam
	 * outer (its any-snapshot+PAINT gate), the flow middle (arrows), the rod innermost
	 * (body + material/paint tint). This listener is the SINGLE writer of those keys (the
	 * dead GTPipeFlowClientListener registration is gone): the old form had
	 * GTRenderModelListener wrap the keys and this replacement overwrite the wrapper
	 * last-wins, burying arrows AND foam AND the cover snapshot under plain rod geometry.
	 */
	static net.minecraft.client.resources.model.BakedModel replacementFor(String aBakedKey, GTRodBakedModel aRod) {
		return isFlowChainKey(aBakedKey) ? GTFluidPipeFoamModel.over(aRod) : aRod;
	}

	/**
	 * The param table (idempotent, test-callable). Registry dereference is safe: mod
	 * loading finished long before client setup / the first bake.
	 *
	 * <p>Task pipe-render-closeout — the pipe rows ride the zero-parallel-table SET
	 * derivation ({@link GTWireTextures#pipeSideSprite}): the borrowed pipeSide art is
	 * byte-identical across every upstream texture set except WOOD and RUBBER, so the
	 * dispatch resolves wood rows to the wood art, the rubber row to the rubber art and
	 * everything else to the shared copper copy (the datagen pipeBlockstate walk over
	 * the same seam — the single dispatch source). The former wood-family/copper binary
	 * (the tex-pipe-textures declared transition) is retired. The connected arms ride
	 * the per-diameter arts ({@link GTWireTextures#pipeArmSprite}, the upstream selector
	 * TileEntityBase10ConnectorRendered :265 verbatim — tiny/small/medium/large/huge over
	 * the PX_P diameter); the logistics wire and the axles stay single-art (arm = base).
	 */
	public static synchronized void buildParams() {
		if (sBuilt) return;
		// the fluid pipe matrix (280 rows) — per-material SET art
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			PARAMS.put(tRow.path(), new Entry(new GTRodBakedModel.Params(
					GTWireTextures.pipeSideSprite(tRow.material().oreDictMaterial()),
					GTWireTextures.pipeArmSprite(tRow.material().oreDictMaterial(), tRow.variant().diameterPx),
					List.of(GTWireTextures.PIPE_SIDE_OVERLAY_SPRITE),
					tRow.variant().diameterPx),
					GTFluidPipes.BLOCKS_BY_PATH.get(tRow.path()).get()));
		}
		// the item pipe rows — per-material SET art; the restrictive variants stack the
		// upstream third pass (the PIPE_RESTRICTOR decal, PipeItem :280)
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
			List<ResourceLocation> tOverlays = tRow.variant().suffix.startsWith("restrictive")
					? List.of(GTWireTextures.PIPE_SIDE_OVERLAY_SPRITE, PIPE_RESTRICTOR)
					: List.of(GTWireTextures.PIPE_SIDE_OVERLAY_SPRITE);
			PARAMS.put(tRow.path(), new Entry(new GTRodBakedModel.Params(
					GTWireTextures.pipeSideSprite(tRow.material().oreDictMaterial()),
					GTWireTextures.pipeArmSprite(tRow.material().oreDictMaterial(), tRow.variant().diameterPx),
					tOverlays,
					tRow.variant().diameterPx),
					GTItemPipes.BLOCKS_BY_PATH.get(tRow.path()).get()));
		}
		// the logistics wire — the dedicated pair, upstream NBT_DIAMETER PX_P[6] (Loader :1819)
		PARAMS.put(GT6Logistics.WIRE_PATH, new Entry(new GTRodBakedModel.Params(
				LOGISTICS_WIRE, List.of(LOGISTICS_WIRE_OVERLAY), 6),
				GT6Logistics.LOGISTICS_WIRE.get()));
		// the 44 axles — the borrowed static axle sprite, no overlays; block = null routes
		// the model's consumer-side axle tint arm (task axle-tint-arm)
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
