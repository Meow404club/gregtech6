package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.registry.GTMachines;

/**
 * The machine paint tint baked INTO the quads (task render-embeddium-tint): the
 * baked models of the {@link GTMachines#paintableBlockArray()} machine domain are
 * wrapped here and {@link GTMachinePaintTint#tintARGB} is multiplied into the body
 * quads' VERTEX COLOURS at {@code getQuads} time — the colour rides the model instead
 * of the runtime {@code BlockColor} route.
 *
 * <p>WHY the seam moved (the p32 live evidence): the colour VALUES of the old
 * {@code BlockColor} half are correct — the pinned offline suite proves it — but the
 * rendered terrain vertices came out achromatic in a live client with BOTH the vanilla
 * chunk builder and Embeddium 0.3.31 (probe: the handler ran with the right state/BE and
 * returned {@code 0xFFD2823C}, while the visible machine pixels measured
 * {@code (178,178,178)} — the exact untinted {@code texture x AO}). The known_bugs
 * embeddium_tint_no_shader report rides the same flaw: with a shader pack the terrain
 * pipeline takes the legacy colour format and the tint shows, without it the
 * field-deployed look is the untinted grayscale. Baking the colour into the quads
 * removes the runtime lookup from the equation entirely — the tint cannot be dropped by
 * any chunk builder, threaded rebuild, snapshot cache or vertex writer downstream.
 *
 * <p>Semantics are byte-for-byte the old seam's: {@link GTMachinePaintTint#tintARGB} is
 * still the single colour source (painted PAINT snapshot wins, unpainted falls back to
 * the row material, {@code -1} = the no-tint identity for the material-less
 * registrations — the P23 barrel contract). The decal overlay elements (no tintindex,
 * the P22 split) pass through untouched. The retinted copies carry {@code tintIndex -1},
 * so a runtime colour lookup can never multiply a second time.
 *
 * <p>RED LINE (render-route ADR, unchanged): {@code getDynamicQuads} reads ONLY the
 * immutable {@link ModelData} the chunk build hands in — no live {@code BlockEntity}.
 *
 * <p>CLIENT-ONLY ({@code @OnlyIn(Dist.CLIENT)} — registered from the mod-construct event
 * under the dist guard, the {@link GTOvenClientListener} shape).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMachineTintModel extends GTDynamicBakedModel {

	/** Retinted-copy cache guard: more distinct spray colours than this clears the table. */
	private static final int CACHE_CAP = 256;

	private final Block mBlock;

	private final Map<Integer, Map<BakedQuad, BakedQuad>> mTintedQuads = new ConcurrentHashMap<>();

	private GTMachineTintModel(BakedModel aFallbackModel, Block aBlock) {
		super(aFallbackModel);
		mBlock = aBlock;
	}

	/**
	 * Every machine is tinted (the tint resolves from the snapshot's PAINT or the row
	 * material — never from a RENDER_SNAPSHOT-style property), so the dispatch gate is
	 * unconditional.
	 */
	@Override
	protected boolean supportsDynamicQuads(ModelData aModelData) {
		return true;
	}

	@Override
	protected List<BakedQuad> getDynamicQuads(@Nullable BlockState aState, @Nullable Direction aSide,
			RandomSource aRand, ModelData aModelData, @Nullable RenderType aRenderType) {
		// the tint source IS the ModelData the chunk build hands in: PAINT while painted
		// (the 03 base getModelData), the row material while unpainted, -1 for the
		// material-less registrations — and every paint change re-enters here through the
		// block-update rebuild (the GTRenderUpdates pair), so the retint follows live.
		// The part-family blocks (task issue8-multipart-tint) carry NO paint BE — their
		// ModelData is always empty, so the material fallback IS the upstream mRGBa, fixed
		// per block.
		return tintQuads(getFallbackModel().getQuads(aState, aSide, aRand),
				GTMachinePaintTint.tintARGB(aModelData, GTMachinePaintTint.tintMaterialOf(mBlock), 0),
				mTintedQuads);
	}

	/**
	 * The pure retint the tests drive (and {@link #getDynamicQuads} consumes): the tinted
	 * body quads ({@code tintIndex == 0}) swap for the cached retinted copies, the P22
	 * decal overlays ({@code tintIndex != 0}) pass through as the shared instances, and
	 * the {@code -1} tint identity returns the input list unchanged (the P23 barrel
	 * contract). {@code aCache} is the per-wrapper retinted-copy table.
	 */
	static List<BakedQuad> tintQuads(List<BakedQuad> aQuads, int aTint,
			Map<Integer, Map<BakedQuad, BakedQuad>> aCache) {
		if (aTint == -1) {
			return aQuads;
		}
		if (aCache.size() > CACHE_CAP) {
			aCache.clear(); // ponytail: adversarial unlimited spray colours reset the cache; an LRU if it ever matters
		}
		Map<BakedQuad, BakedQuad> tBySource = aCache.computeIfAbsent(aTint, tT -> new ConcurrentHashMap<>());
		List<BakedQuad> rOut = new ArrayList<>(aQuads.size());
		for (BakedQuad tQuad : aQuads) {
			if (tQuad.getTintIndex() != 0) {
				rOut.add(tQuad); // the P22 overlay decals: untinted by design, shared instance
				continue;
			}
			rOut.add(tBySource.computeIfAbsent(tQuad, tQuad1 -> retint(tQuad1, aTint)));
		}
		return rOut;
	}

	/** The tint multiplied into each vertex colour slot (IQuadTransformer COLOR = 3, stride 8). */
	private static BakedQuad retint(BakedQuad aQuad, int aTint) {
		return new BakedQuad(retintVertices(aQuad.getVertices(), aTint), -1, aQuad.getDirection(),
				aQuad.getSprite(), aQuad.isShade());
	}

	/**
	 * The pure recolour the tests drive (and {@link #retint} consumes): per-channel
	 * {@code (colour * tint + 255) >> 8} over every vertex of the baked vertex data.
	 *
	 * <p>ENCODING (issue #14 fix, the single point): the baked COLOR slot int stores its
	 * channels {@code A<<24|B<<16|G<<8|R} — byte order R,G,B,A, the layout vanilla's
	 * putBulkData consumes (VertexConsumer.java:90-92 reads bytes 12/13/14 = R/G/B) and
	 * Forge's own {@link QuadTransformers#toABGR} defines for {@code applyingColor} — while
	 * the tint arrives ARGB (the BlockColors ecosystem encoding every colour seam here
	 * resolves). Multiplying an ARGB tint in by byte position swapped R and B, so every
	 * warm colour rendered cool (copper blue, gold cyan; the R==B rows like tungsten
	 * 50,50,50 hid it — the #8 burning-box side observation). The tint is converted ONCE
	 * here, so every consumer domain (machines, kitchen, controllers, ores, the fluid
	 * spring) inherits the fix through the shared product.
	 */
	public static int[] retintVertices(int[] aVertices, int aTint) {
		int tTintABGR = QuadTransformers.toABGR(aTint);
		int[] rVertices = new int[aVertices.length];
		for (int v = 0; v * IQuadTransformer.STRIDE < aVertices.length; v++) {
			int tBase = v * IQuadTransformer.STRIDE;
			System.arraycopy(aVertices, tBase, rVertices, tBase, IQuadTransformer.STRIDE);
			rVertices[tBase + IQuadTransformer.COLOR] = mulColor(aVertices[tBase + IQuadTransformer.COLOR], tTintABGR);
		}
		return rVertices;
	}

	/** Per-channel {@code (c * t + 255) / 256} over the packed colour (alpha included) — full-value exact. */
	private static int mulColor(int aColour, int aTint) {
		int rResult = 0;
		for (int tShift = 0; tShift < 32; tShift += 8) {
			int tChannel = (((aColour >> tShift) & 255) * ((aTint >> tShift) & 255) + 255) >> 8;
			rResult |= (tChannel & 255) << tShift;
		}
		return rResult;
	}

	/**
	 * The baked-model replacement (the ModifyBakingResult hook, the
	 * {@link GTRenderModelListener} shape read directly off the event map): every
	 * paintable-array state swaps in this wrapper over its freshly baked model — the
	 * machine domain ({@code GTMachines.paintableBlockArray}), the part family
	 * ({@code GTMultiBlocks.partPaintableBlockArray}), since task
	 * c3-kitchen-tint-shape the kitchen family
	 * ({@code GT6Kitchen.paintableBlockArray} — the tintindex-0 faces resolve the carrier
	 * material, the #7 reservation closing) and, since task
	 * c2-controller-tint, the controller/energy domain (the 12 multiblock mains, the
	 * 15 EU-bridge rungs, the 10 laser rungs, the magic absorber); since task
	 * issue8-residual the #8 stragglers (the 25 tank valve controllers, the 8 dedicated
	 * crucible walls); since task beehive-tint (issue #15) the bee family (the hive
	 * + the Bumbliary pair — the family colour rides the PAINT model data, the pair's
	 * row material the {@code GT6BumbliaryBlock} carrier); since task
	 * r11-mains-tint-wrap the twelve {@code GTMultiBlocks} mains controllers ride the
	 * ONE {@code controllerPaintableBlockArray} census walk (the boilerPaintable row,
	 * the coke-oven single row and the five-block List.of retired into it). Blocks that
	 * already carry a dynamic model (the oven ladder's {@code GTOvenOverlayModel} chain)
	 * are skipped — they keep their own render route.
	 */
	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		for (Block tBlock : GTMachines.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GTMultiBlocks.partPaintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Kitchen.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Turbines.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6DynamoHousings.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : GTMachines.bridgePaintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Lasers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6MagicAbsorbers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// issue #18 (task 18-converter-tex-facing) — the converter family joins the
		// baked-tint domain: the nine electric transformer rows (the Electric_T[0..8]
		// casing ladder) and the ten dynamo rows (electric [0..5] / flux [1..5]) — the
		// tintindex-0 gray colored body is the seat, the overlay decals untinted
		for (Block tBlock : gregtech6.registry.GT6ElectricTransformers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6ElectricDynamos.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6FluxDynamos.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task issue8-residual — the #8 stragglers: the 25 tank valve controllers (the row
		// material rides the controller gate through GTTankValveBlock) and the 8 crucible
		// walls (the dedicated GTCrucibleWallBlock part carriers)
		for (Block tBlock : gregtech6.registry.GT6Tanks.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Crucibles.paintableWallBlockArray()) wrapStates(tBlock, aEvent);
		// issue #11 — the burning boxes join the baked-tint domain: every row carries
		// NBT_MATERIAL upstream (Loader :519-548/:619-704), the body cube is the
		// tintindex-0 seat, the colour comes off GTBasicMachineBlock.materialOf like
		// every other machine domain (the lit decals carry NO tintindex, so the
		// burning glow passes through untinted — the P22 decal contract)
		for (Block tBlock : gregtech6.registry.GT6BurningBoxes.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task world-tint-render-type — the C5 boiler clean-up: the 26 steam boiler
		// tanks join (every upstream row carries NBT_MATERIAL, Loader :553-579; the
		// shared model's body cube is the tintindex-0 seat since this card)
		for (Block tBlock : gregtech6.registry.GT6Boilers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task r11-mains-tint-wrap — the TWELVE GTMultiBlocks mains controllers ride the
		// ONE census array ({@code controllerPaintableBlockArray}: the coke oven, the five
		// large boilers, the lightning rod, implosion/VonDaGraagg/massfab/fusion/drill).
		// The former tex-large-boilers boilerPaintable walk (:1248-1252), the coke-oven
		// single row (:1193) and the tex-large-machines five-block List.of retire INTO it
		// — every row carries the upstream NBT_MATERIAL, the two-layer front-bearing body
		// cube is the tintindex-0 seat, the overlay/front decals untinted (the p38-c2
		// carrier gate + the largeControllerMaterialOf constants)
		for (Block tBlock : gregtech6.registry.GTMultiBlocks.controllerPaintableBlockArray()) wrapStates(tBlock, aEvent);
		// task tex-large-machines — the 17 large-controller domains join the same
		// baked-tint seat: every upstream row carries NBT_MATERIAL (Loader :1228-1283),
		// the body cube is the tintindex-0 seat (familyMachineModel trio + the two
		// boilerModel front pairs), the colour resolves through the GTMachinePaintTint
		// large-controller arm (the row meta id; the five mains moved to the census array)
		for (Block tBlock : gregtech6.registry.GT6LargeMachines.blockArray()) wrapStates(tBlock, aEvent);
		// task tex-multiblockmains — the other-class mains families join the same
		// two-layer front-bearing seat (every row carries NBT_MATERIAL upstream:
		// Loader :1270-1277 crucible eight, :1245 heat exchanger, :1281 logistics core;
		// the :1282 lightning rod moved to the census array above; the body cube is the
		// tintindex-0 seat, the front-pair overlay decals untinted; the colours resolve
		// through the GTMultiBlockControllerBlock carrier, the p38-c2 gate, and the HEX
		// through its own materialOf, the GT6DynamoBlock shape)
		for (var tHandle : gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.values()) wrapStates(tHandle.get(), aEvent);
		wrapStates(gregtech6.registry.GT6Logistics.LOGISTICS_CORE.get(), aEvent);
		wrapStates(gregtech6.registry.GT6HeatExchangers.HEAT_EXCHANGER_BLOCK.get(), aEvent);
		// issue #15 (task beehive-tint) — the bee family joins the baked-tint domain:
		// the hive's 15 worldgen family colours ride the BE PAINT model data (the worldgen
		// paints at placement) and the Bumbliary pair's row material rides the
		// GT6BumbliaryBlock.materialOf carrier — the runtime BlockColor registration the
		// family carried since p32 is GONE (the achromatic-in-live-client route this card
		// migrates away, the p32 bake ruling).
		for (Block tBlock : gregtech6.registry.GT6BeeHives.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task tex-pipe-textures — the three pipe connector families join the
		// baked-tint domain: the 280 fluid rows (task fluid-pipe-matrix — the per-row
		// material dispatch), the 18 item pipe rows and the single logistics wire over
		// their two-layer models (the body cube is the tintindex-0 seat, the overlay
		// bands untinted). The wire's material resolves NULL (upstream NBT_MATERIAL =
		// MT.NULL) — the white identity passes through unchanged.
		for (Block tBlock : gregtech6.registry.GTFluidPipes.blockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GTItemPipes.blockArray()) wrapStates(tBlock, aEvent);
		wrapStates(gregtech6.registry.GT6Logistics.LOGISTICS_WIRE.get(), aEvent);
		// task tex-composite-family — the composite-energy families join the baked-tint
		// domain: the 12 battery boxes + 20 crystal chargers + 2 ZPM dechargers + 5 LD
		// transformer endpoints (every row carries NBT_MATERIAL upstream — Loader
		// :894-:895/:970-:971/:1000-:1001/:909-:913; the two-layer body cube is the
		// tintindex-0 seat, the overlay decals untinted; the colour resolves through the
		// GT6BatteryBoxBlock / GT6ElectricTransformerBlock carriers)
		for (Block tBlock : gregtech6.registry.GT6Batteries.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6CrystalChargers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6ZpmDechargers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6LongDistanceTransformers.paintableBlockArray()) wrapStates(tBlock, aEvent);
		// task tex-bridge-kinetic — the kinetic engines join the baked-tint domain:
		// the 28 steam-engine rows (Loader :584-612 NBT_MATERIAL) and the 8 diesel rows
		// (:721-729) over their two-layer shells (the tintindex-0 gray colored body is
		// the seat, the overlay decals untinted), plus the single rotation transformer
		// (the WoodTreated row, :1668 — the material resolves through the GTMachinePaintTint
		// bySlug dispatch arm, the row records live in GT6Kinetics)
		for (Block tBlock : gregtech6.registry.GT6Kinetics.steamEngineBlockArray()) wrapStates(tBlock, aEvent);
		for (Block tBlock : gregtech6.registry.GT6Kinetics.dieselBlockArray()) wrapStates(tBlock, aEvent);
		wrapStates(gregtech6.registry.GT6Kinetics.TRANSFORMER_ROTATION.get(), aEvent);
		// task tint-coverage-batch — the four static-storage metal families (the
		// locker/drawer/safe pair kinds over the Bronze/Steel anchors, 8 blocks) and the
		// reactor core (the :738 Pb row) join the baked-tint domain: the borrowed grayscale
		// colored_* cube bodies are the tintindex-0 seat (the storageModel/addReactorCore
		// datagen change), the overlay decal plates untinted (the P22 contract); the
		// bookshelf/bottlecrate rows stay OUT (the vanilla-finished plank art, the stone
		// precedent — metalBlockArray carries the material rows only). The reactor's
		// DYNAMIC rod/fluid/axle stack stays with the rod-render-pool card — the wrap
		// guard below is order-safe against that card's own dynamic-model seat.
		for (Block tBlock : gregtech6.registry.GT6StaticStorages.metalBlockArray()) wrapStates(tBlock, aEvent);
		wrapStates(gregtech6.registry.GT6Reactors.REACTOR_CORE_2X2_BLOCK.get(), aEvent);
		// task small-tank-colored-tint — the four small-tank families join the baked-tint
		// domain: the borrowed grayscale colored_* elements are the tintindex-0 seat (the
		// addGasCylinders/addCells/addCup/addJug datagen change — the gas cylinder's
		// all-barometer arm included), the 0.01 overlay shells and the smeltery_content
		// fluid boxes untinted (the P22 contract; upstream BlockTextureFluid carries no
		// mRGBa). The cell rows carry the loader NBT_MATERIAL column (Loader :1770-1809),
		// the four gas cylinders theirs (:2101-2104), the cup Porcelain (:2094) and the
		// jug Ceramic (:2095) — the upstream BlockTextureDefault(colored, mRGBa) passes
		// (GasCylinder :141 / Cell :42-44 / Cup :68-73 / Jug :71-76); the four cards'
		// declared measuring-pot deviation retires.
		for (var tCylinder : gregtech6.registry.GT6GasCylinders.BLOCKS_IN_ORDER) wrapStates(tCylinder.get(), aEvent);
		for (var tCell : gregtech6.registry.GT6Cells.BLOCKS_IN_ORDER) wrapStates(tCell.get(), aEvent);
		wrapStates(gregtech6.registry.GT6Cups.PORCELAIN_CUP.get(), aEvent);
		wrapStates(gregtech6.registry.GT6Jugs.CERAMIC_JUG.get(), aEvent);
		// task act-charging-table-tint — the Advanced/Charging Crafting Table matrix
		// joins the baked-tint domain: the 120 rows carry their loader NBT_MATERIAL
		// (Loader :136-137, one column per plain/charging pair; the registration derives
		// NBT_COLOR = getRGBInt(fRGBaSolid), MultiTileEntityClassContainer.java:51), the
		// shared machineModel body cube is the tintindex-0 seat (the grayscale
		// advanced_colored_front × mRGBa, MultiTileEntityAdvancedCraftingTable.java
		// :659-662 getTexture2; the charging twin :61-64 the same shell), the 0.01 front
		// decal untinted (the P22 contract). The block class doc's former
		// "transitional, tint rides the ⑩B card" reservation retires for the TINT half
		// (the charging texture-family swap stays the ⑩B card).
		for (var tTableBlock : GTMachines.CRAFTING_TABLE_BLOCKS_BY_PATH.values()) wrapStates(tTableBlock.get(), aEvent);
		// task tint-chain-hopper-grindstone-sifting — the 120 storage-hopper rows join the
		// baked-tint domain (the census L1 closure): the shared funnel models' colored body
		// band is the tintindex-0 seat (the addHoppers datagen change), the 0.01 overlay
		// twins untinted (the P22 contract); the colour resolves through the
		// GT6HopperBlock row carrier (the upstream NBT_MATERIAL walk Loader:145-146 × the
		// MultiTileEntityHopper.java:281 BlockTextureMulti colored×mRGBa pass). The
		// Grindstone and Sifting Table stay OUT of this walk — their model seats are owned
		// by their NEI dynamic models (the GTDynamicBakedModel guard below skips them
		// anyway); those consume through their own self-tint arms over the
		// GTMachinePaintTint block-class rows, the GT6GrindstoneNeiModel.bodyQuads shape.
		for (var tHopper : gregtech6.registry.GT6Hoppers.BLOCKS_BY_PATH.values()) wrapStates(tHopper.get(), aEvent);
	}

	/**
	 * The small-tank paint tint, the INVENTORY half (task small-tank-colored-tint): the
	 * four families' BlockItems ride the shared {@link GTItemPaintTint} lambda through the
	 * combined {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab face
	 * (a BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93). The
	 * registration mirrors {@link #onRegisterValveWallPaintItemColors} (this class is the
	 * shared client tint seam).
	 */
	@SubscribeEvent
	public static void onRegisterSmallTankPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tItems = new ArrayList<>();
		for (var tCylinder : gregtech6.registry.GT6GasCylinders.BLOCKS_IN_ORDER) tItems.add(tCylinder.get().asItem());
		for (var tCell : gregtech6.registry.GT6Cells.BLOCKS_IN_ORDER) tItems.add(tCell.get().asItem());
		tItems.add(gregtech6.registry.GT6Cups.PORCELAIN_CUP.get().asItem());
		tItems.add(gregtech6.registry.GT6Jugs.CERAMIC_JUG.get().asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(Item[]::new));
	}

	/**
	 * The hopper/grindstone/sifting paint tint, the INVENTORY half (task
	 * tint-chain-hopper-grindstone-sifting — the census L1/L2/L3 closures): the 120
	 * storage-hopper BlockItems plus the two manual-tool BlockItems ride the shared
	 * {@link GTItemPaintTint} lambda through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab face (a
	 * BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93). The
	 * registration mirrors {@link #onRegisterSmallTankPaintItemColors} (this class is the
	 * shared client tint seam); the unpainted stacks resolve the row materials (the
	 * {@code GT6HopperBlock} carrier / the ANY.Steel carriers), a painted stack still wins
	 * through the NBT pair.
	 */
	@SubscribeEvent
	public static void onRegisterHopperToolPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tItems = new ArrayList<>();
		for (var tHopper : gregtech6.registry.GT6Hoppers.ITEMS_BY_PATH.values()) tItems.add(tHopper.get());
		tItems.add(gregtech6.registry.GT6Grindstones.GRINDSTONE_ITEM.get());
		tItems.add(gregtech6.registry.GT6SiftingTables.SIFTING_TABLE_ITEM.get());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(Item[]::new));
	}

	/**
	 * The kitchen paint tint, the INVENTORY half (task c3-kitchen-tint-shape): the
	 * family's BlockItems registered over the shared {@link GTItemPaintTint} lambda — the
	 * unpainted stacks resolve the carrier material through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch (the #8 part-registration mirror
	 * shape). It lives on this subscriber rather than GTClientHandlers because the kitchen
	 * card's FILES_SCOPE draws the client seam at client/render/; explicit registration is
	 * still mandatory — a BlockColor does NOT colour its BlockItem and vanilla
	 * {@code ItemColors.createDefault} has no BlockItem delegation (ItemColors.java:25-93).
	 */
	@SubscribeEvent
	public static void onRegisterKitchenPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<net.minecraft.world.item.Item> tItems = new java.util.ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Kitchen.paintableBlockArray()) tItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(net.minecraft.world.item.Item[]::new));
	}

	/**
	 * The controller/energy-domain paint tint, the INVENTORY half (task
	 * c2-controller-tint): the 38 new-domain blocks' BlockItems ride the shared
	 * {@link GTItemPaintTint} lambda through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab face (a
	 * BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93). The
	 * per-domain-listener registration is the {@code GT6TreeClientListener} shape (this
	 * class IS the domain's client seam; GTClientHandlers keeps the machine/part/barrel
	 * faces it already owns). The coke-oven bricks join the existing part registration
	 * automatically through {@code partPaintableBlockArray}.
	 */
	@SubscribeEvent
	public static void onRegisterControllerPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Turbines.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6DynamoHousings.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : GTMachines.bridgePaintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6Lasers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6MagicAbsorbers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		// issue #18 (task 18-converter-tex-facing) — the converter family's 19
		// BlockItems (the nine transformer + ten dynamo rows) join the same lambda
		for (Block tBlock : gregtech6.registry.GT6ElectricTransformers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6ElectricDynamos.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6FluxDynamos.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		// task r11-mains-tint-wrap — the twelve GTMultiBlocks mains controllers' 12
		// BlockItems ride the ONE census array (the former tex-large-boilers boiler
		// rows, the tex-multiblockmains lightning rod, the coke-oven-texture coke oven
		// and the tex-large-machines five-block List.of retire INTO it; the NBT_MATERIAL
		// columns ride the GTMultiBlockControllerBlock carrier + the
		// largeControllerMaterialOf constants)
		for (Block tBlock : gregtech6.registry.GTMultiBlocks.controllerPaintableBlockArray()) tPaintItems.add(tBlock.asItem());
		// task tex-multiblockmains — the other-class mains families' 10 BlockItems (the
		// eight crucible rows + the logistics core + the heat exchanger) join the same
		// lambda (the NBT_MATERIAL columns ride the GTMultiBlockControllerBlock carrier,
		// the HEX its own materialOf)
		for (var tHandle : gregtech6.registry.GT6Crucibles.CRUCIBLE_BLOCKS_BY_PATH.values()) tPaintItems.add(tHandle.get().asItem());
		tPaintItems.add(gregtech6.registry.GT6Logistics.LOGISTICS_CORE_ITEM.get());
		tPaintItems.add(gregtech6.registry.GT6HeatExchangers.HEAT_EXCHANGER_ITEM.get());
		// task tex-pipe-textures — the three pipe connector families' BlockItems (the
		// 280 fluid rows — task fluid-pipe-matrix, the 18 item pipe rows, the logistics
		// wire) join the same lambda; the wire's carrier resolves NULL (upstream
		// NBT_MATERIAL = MT.NULL) so its unpainted identity stays the white no-op.
		for (Block tBlock : gregtech6.registry.GTFluidPipes.blockArray()) tPaintItems.add(tBlock.asItem());
		// task tex-bridge-kinetic — the kinetic engines' 37 BlockItems (the 28 steam
		// rows, the 8 diesel rows, the rotation transformer) join the same lambda: the
		// two-layer shells carry tintindex 0 on the body, and an unregistered BlockItem
		// would render the creative-tab face untinted (the #18 converter-band note)
		for (Block tBlock : gregtech6.registry.GT6Kinetics.steamEngineBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6Kinetics.dieselBlockArray()) tPaintItems.add(tBlock.asItem());
		tPaintItems.add(gregtech6.registry.GT6Kinetics.TRANSFORMER_ROTATION.get().asItem());
		// the entry handles compile against both legs' RegistryObject/DeferredHolder
		// without a fork (the GT6BlockTags mineable-band lambda shape)
		gregtech6.registry.GTItemPipes.ITEMS_BY_PATH.values().forEach(tPipeItem -> tPaintItems.add(tPipeItem.get()));
		tPaintItems.add(gregtech6.registry.GT6Logistics.LOGISTICS_WIRE_ITEM.get());
		// task tex-composite-family — the composite-energy families' 39 BlockItems (the
		// 12 battery boxes + 20 crystal chargers + 2 ZPM dechargers + 5 LD endpoints) join
		// the same lambda
		for (Block tBlock : gregtech6.registry.GT6Batteries.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6CrystalChargers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6ZpmDechargers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6LongDistanceTransformers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		// task tex-large-machines — the 17 large-controller BlockItems join the same
		// lambda (the creative-tab face of the row-material colour, the world half above);
		// the five mains BlockItems moved to the census array loop above
		// (task r11-mains-tint-wrap)
		for (Block tBlock : gregtech6.registry.GT6LargeMachines.blockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/**
	 * The tank-valve + crucible-wall paint tint, the INVENTORY half (task issue8-residual):
	 * the #8 stragglers' BlockItems ride the shared {@link GTItemPaintTint} lambda through
	 * the combined {@code GTMachinePaintTint.tintMaterialOf} dispatch — the creative-tab
	 * face (a BlockItem is NOT coloured by any baked world tint, ItemColors.java:25-93).
	 * The registration mirrors {@link #onRegisterKitchenPaintItemColors} (this class is the
	 * shared client tint seam; GTClientHandlers keeps the faces it already owns).
	 */
	@SubscribeEvent
	public static void onRegisterValveWallPaintItemColors(RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<net.minecraft.world.item.Item> tItems = new java.util.ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Tanks.paintableBlockArray()) tItems.add(tBlock.asItem());
		for (Block tBlock : gregtech6.registry.GT6Crucibles.paintableWallBlockArray()) tItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tItems.toArray(net.minecraft.world.item.Item[]::new));
	}

	/**
	 * The burning-box paint tint, the INVENTORY half (issue #11 texture half): the 97
	 * BlockItems ride the shared {@link GTItemPaintTint} lambda through the combined
	 * {@code GTMachinePaintTint.tintMaterialOf} dispatch (which resolves the row
	 * material via the common {@code GTBasicMachineBlock.materialOf} gate, task p27) —
	 * the creative-tab face the world half cannot colour (a BlockItem is NOT coloured
	 * by any baked world tint, ItemColors.java:25-93). The per-domain-listener
	 * registration is the {@link #onRegisterControllerPaintItemColors} shape.
	 */
	@SubscribeEvent
	public static void onRegisterBurningBoxPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6BurningBoxes.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/**
	 * The boiler paint tint, the INVENTORY half (task world-tint-render-type, the C5
	 * clean-up): the 26 boiler BlockItems ride the shared {@link GTItemPaintTint} lambda
	 * through the combined {@code GTMachinePaintTint.tintMaterialOf} dispatch (the row
	 * material resolves through the common {@code GTBasicMachineBlock.materialOf} gate —
	 * the burning-box form, 43f48149b).
	 */
	@SubscribeEvent
	public static void onRegisterBoilerPaintItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item aEvent) {
		java.util.List<Item> tPaintItems = new ArrayList<>();
		for (Block tBlock : gregtech6.registry.GT6Boilers.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
		aEvent.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
	}

	/** One block's state walk (the shared swap body; the dynamic-model guard is order-safe against GTRenderModelListener's own hook). */
	private static void wrapStates(Block tBlock, ModelEvent.ModifyBakingResult aEvent) {
		for (BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
			var tKey = BlockModelShaper.stateToModelLocation(tState);
			BakedModel tBaked = aEvent.getModels().get(tKey);
			// the dynamic-model guard is order-safe against GTRenderModelListener's own
			// hook: whichever runs first, the dynamic model ends up the map value
			if (tBaked != null && !(tBaked instanceof GTDynamicBakedModel)) {
				aEvent.getModels().put(tKey, new GTMachineTintModel(tBaked, tBlock));
			}
		}
	}
}
