package gregtech6.recipes;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.UT;
import gregtech6.components.IOreDictListenerRecyclable;
import gregtech6.components.OM;
import gregtech6.fluid.FluidBridge;
import gregtech6.fluid.FluidTankGT;
import gregtech6.registry.GTMaterialItems;

/**
 * The recycling recipe generator (task component-recycling-recipes, MS-2 symptom29): ported
 * from GregTech 6 (1.7.10) Loader_OreProcessing.java:238-294 — the ONLY
 * {@code IOreDictListenerRecyclable} — registered at Loader_OreProcessing.java:199. Every
 * recyclable component registration (the central face's write path,
 * gregtech6.components.OM.setItemData_ :666-670 semantics) pours Melter/Smelter melting rows
 * and the Paper/Bone Mortar arm for the registered stack; the central face replays past
 * registrations to late listeners (OM.addListener), so registering here is "live" against the
 * whole registration log — the ADR "addListener(OM) 即活" ruling
 * (docs/adr/2026-10-06-components-subsystem.md §1).
 *
 * <p><b>The upstream flow, verbatim</b>: prefix-less data grinds Paper (and Bone when the data
 * carries no byproducts) into the best-fitting dust ({@link #dust}, upstream OM.java:459-465)
 * once the amount reaches dustDiv72 (:246); every material whose smelt target exists
 * ({@code mTargetSmelting.mAmount > 0}), is MELTING-flagged and not BLACKLISTED_SMELTER
 * projects onto that target (:252/:257); prefixed ore-ish data (ORE/ORE_PROCESSING_DIRTY)
 * declines outright (:255); the projection list must resolve to EXACTLY ONE fluid-bearing
 * material (:263-287 — a second fluid-bearing entry nulls the whole melt); the FURNACE gate
 * (:289 — {@code contains(FURNACE)} or a CaCO3 smelt target) decides whether the Melter/
 * Smelter rows pour at all; the row duration is
 * {@code max(lavaArm, weight * (max(meltingPoint, fluidTemp) - 25°C) / 1600)} at 16 EU/t
 * (:290-291 — the plan card's "energy formula" wording is the third addRecipe1 argument =
 * the DURATION; EUt is the constant 16).
 *
 * <p><b>Idempotence</b> (the plan card: "/reload 重生成幂等，p32 教训"): the rows this class
 * produced are a tracked per-stack subset ({@link #sRows}) — a re-registration of the same
 * stack (a second OM.setItemData_ from the derivation-reload card, or a late-listener replay
 * of the registration log) REPLACES the previous subset instead of stacking: out with the
 * previous instances (identity — Recipe carries no equals), in with the fresh ones, exactly
 * the GT6CokeOvenTagListener.replaceLogRecipes / GT6RecipeMapJsonLoader.pourFile shape. The
 * size-neutral replace must NOT trust the size-drift self-heal (perf-recipe-hash-index P1):
 * {@link RecipeMap#invalidateIndex()} forces the next lookup to rebuild.
 *
 * <p><b>The phase window</b> (rm-phase-gate): a live /reload re-registration lands AFTER the
 * ServerStarted freeze, so every replace rides the {@link GT6RecipeMaps#reopenWindow()}
 * re-pour window the JSON loader uses — FROZEN → OPEN for the pour, re-frozen on exit
 * (freeze idempotent; an OPEN generation owes nothing, so offline pours never touch it).
 *
 * <p><b>DECLARED deviations / the negative account</b> (upstream semantics first, ADR red
 * line 4; each is a wave-2 fluid-module / unification-card re-open slot):
 * <ul>
 * <li><b>The molten-fluid seam</b>: upstream reads {@code mMaterial.mLiquid/mGas} FluidStacks
 *     bound onto the material (OreDictMaterial.java:314-315); the port deletes those fields
 *     (root red line: no net.minecraft in gregapi) and resolves through
 *     {@link FluidBridge#moltenFluidForMaterial} (the {@code gt6:<mat>_molten} registry walk).
 *     A material without a registered molten row produces NO melt rows — upstream had full
 *     bindings for every MELTING material. This is the card's main negative account: e.g.
 *     MT.Lava carriers (upstream: lava-output rows with the EU_PER_LAVA energy arm) and the
 *     whole gas domain.</li>
 * <li><b>The Aerotheum gas arm is cut</b> (:266-275): no gas fluid registry exists, so
 *     MT.Aerotheum rides the same molten seam as everything else and skips silently — the
 *     upstream-with-null-gas behavior for the v1 fluid universe (upstream: gas melt rows for
 *     Aerotheum-bearing items).</li>
 * <li><b>The lava arm stays formula-verbatim but is currently unreachable</b>: vanilla
 *     {@code Fluids.LAVA/FLOWING_LAVA} is checked (:290 FL.Lava.is) and EU_PER_LAVA = 80 rides
 *     here (upstream CS.java:216; the shared-root CS is outside this card's file scope) — the
 *     arm answers the moment the seam ever resolves a material to vanilla lava.</li>
 * <li><b>The fluid temperature arm parks</b> (the OM.carriesFluid environment-probe shape):
 *     {@code Fluid.getFluidType().getTemperature()} is a Forge-transformed face that dies in an
 *     untransformed (offline test) JVM; the one-time catch parks the arm for that JVM and the
 *     duration rides the melting point alone. For self-molten fluids (GTFluids binds the
 *     material's own melting point as the fluid temperature, GTFluids.java:239) this is the
 *     same number live — the {@code max()} only bites on exotic hotter carriers.</li>
 * <li><b>ST.ingredable collapses to the port-live arms</b> (upstream ST.java:838-851): not
 *     empty and no container item. The cut arms have no port candidates: IItemGTContainerTool
 *     (no port tool items), the IFluidContainerItem capacity arm (port containers carry
 *     container items), ItemsGT.CONTAINER_DURABILITY, the IL.Cell/Teapot exceptions.</li>
 * </ul>
 *
 * <p>KJS face (the card declaration): this class produces RM runtime rows — the RecipeMap
 * graph is deferred to the kjs-binding card; nothing here reads or writes datapack JSON.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecyclingProcessing implements IOreDictListenerRecyclable {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The default environment temperature (upstream CS.DEF_ENV_TEMP = C + 20 = 293, CS.java:135;
	 * the port TileEntityCrucible.DEF_ENV_TEMP form, TileEntityCrucible.java:145). */
	public static final long DEF_ENV_TEMP = 293;

	/** Upstream CS.EU_PER_LAVA (CS.java:216) — hosted here because the shared root CS is outside
	 * this card's file scope; moves with the fluid-module card if a CS home opens. */
	public static final long EU_PER_LAVA = 80;

	/** The one listener instance (upstream {@code new RecyclingProcessing()}, Loader_OreProcessing.java:199). */
	public static final GT6RecyclingProcessing INSTANCE = new GT6RecyclingProcessing();

	/**
	 * The molten-fluid seam (the Recipe.sTagTest / OM.sStackTags / GTMaterialItems.sLookup
	 * static-seam family): production answers the FluidBridge registry walk, offline tests
	 * inject a stub because the fluid registry never boots. Tests capture and restore.
	 */
	public static Function<OreDictMaterial, Fluid> sMoltenFluid = aMaterial -> FluidBridge.moltenFluidForMaterial(aMaterial.mNameInternal);

	/** The prefix-item seam for the dust ladder ({@code OP.<dust>.mat()} isomorph): production
	 * answers the registration index (GTMaterialItems.get), offline tests inject probe items. */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sPrefixItem = GT6RecyclingProcessing::prefixItem;

	/** The produced-row ledger. Keyed by the central face's public {@link OM.StackKey} (item +
	 * damage — the authoritative keying shape, OM.java:452/307; the third local mirror was
	 * folded into it, task om-hygiene-mini): ItemStack's equals is content based but its
	 * hashCode is NOT, so a bare HashMap&lt;ItemStack,…&gt; misses every lookup. The write
	 * path amortizes registrations to ONE-count copies (OM.setItemData_ :656), so damage is
	 * the only per-stack axis the ledger needs. */
	private static final Map<OM.StackKey, List<Row>> sRows = new HashMap<>();

	/** One produced row and the map it lives in (the subset-replace bookkeeping). */
	private record Row(RecipeMap map, Recipe recipe) {}

	// ------------------------------------------------------------------ the lifecycle

	/** The MOD-bus hook (the GT6RecipesImplosion form): registration pours at FMLCommonSetup,
	 * where the recipe-map generation is OPEN (the freeze lives at ServerStarted) and the
	 * central face replays every bootstrap registration into this listener. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		register();
	}

	/** The registration (the offline test entry — upstream OreDictManager.addListener :141-148
	 * semantics: registering late replays the whole registration log). */
	public static void register() {
		OM.addListener(INSTANCE);
		LOGGER.info("GT6 RecyclingProcessing registered — {} recyclable registration(s) replayed", OM.recyclingRegistrations().size());
	}

	// ------------------------------------------------------------------ the listener (upstream :240-293, verbatim flow)

	@Override
	public void onRecycleableRegistration(OreDictRecyclingContainer aEvent) {
		if (aEvent.mItemData == null || !ingredable(aEvent.mStack)) return; // :241

		List<Row> tRows = new ArrayList<>();
		List<OreDictMaterialStack> tList = new ArrayList<>();
		if (aEvent.mItemData.mPrefix == null) { // :244 the prefix-less arm: Mortar + melt projection
			for (OreDictMaterialStack tMaterial : aEvent.mItemData.getAllMaterialStacks()) {
				if (tMaterial.mAmount >= OP.dustDiv72.mAmount) { // :246
					if (tMaterial.mMaterial == MT.Paper) mortarRow(aEvent.mStack, tMaterial, tRows); // :247
					if (aEvent.mItemData.mByProducts.length <= 0) { // :248
						if (tMaterial.mMaterial == MT.Bone) mortarRow(aEvent.mStack, tMaterial, tRows); // :249
					}
				}
				meltTarget(tMaterial, tList); // :252
			}
		} else {
			if (aEvent.mItemData.mPrefix.containsAny(TD.Prefix.ORE_PROCESSING_DIRTY, TD.Prefix.ORE)) return; // :255
			for (OreDictMaterialStack tMaterial : aEvent.mItemData.getAllMaterialStacks()) meltTarget(tMaterial, tList); // :257
		}
		if (tList.isEmpty()) {replace(aEvent.mStack, tRows); return;} // :261 — the Mortar rows above already poured upstream; an empty list still replaces (a re-declared stack drops its stale rows)

		// :263-287 the single-fluid gate (the Aerotheum gas special-case rides the same seam —
		// see the class javadoc: no gas domain, the material simply answers null and skips).
		FluidStack tFluid = null;
		OreDictMaterialStack tMaterial = null;
		for (OreDictMaterialStack iMaterial : tList) {
			FluidStack tResolved = molten(iMaterial.mMaterial, iMaterial.mAmount);
			if (tResolved == null) continue;
			if (tFluid == null) {tMaterial = iMaterial; tFluid = tResolved;}
			else {tFluid = null; break;}
		}
		// :288 the positive-fluid gate, :289 the FURNACE/CaCO3 gate.
		if (tFluid == null || tFluid.getAmount() <= 0 || tMaterial == null) return;
		if (!tMaterial.mMaterial.contains(TD.Processing.FURNACE) && tMaterial.mMaterial.mTargetSmelting.mMaterial != MT.CaCO3) return;

		// :290-291 — the formula is the DURATION (addRecipe1 arg 3) at the constant 16 EU/t.
		long tHot = Math.max(tMaterial.mMaterial.mMeltingPoint, fluidTemperature(tFluid));
		double tWeight = weight(aEvent.mItemData.getAllMaterialStacks());
		long tDuration = (long)Math.max(
				isLava(tFluid) ? UT.Code.divup(tFluid.getAmount() * EU_PER_LAVA, 16) : 16,
				tWeight * (tHot - DEF_ENV_TEMP) / 1600);
		tRows.add(new Row(GT6RecipeMaps.MELTER , new Recipe(T, new ItemStack[] {aEvent.mStack}, new ItemStack[0], new FluidStack[0], new FluidStack[] {tFluid}, tDuration, 16, 0)));
		tRows.add(new Row(GT6RecipeMaps.SMELTER, new Recipe(T, new ItemStack[] {aEvent.mStack}, new ItemStack[0], new FluidStack[0], new FluidStack[] {tFluid}, tDuration, 16, 0)));
		replace(aEvent.mStack, tRows);
	}

	// ------------------------------------------------------------------ the pour (the idempotent subset replace)

	/**
	 * The tracked-subset replace (the CokeOven replaceLogRecipes / JSON pourFile shape, see the
	 * class javadoc): remove THIS stack's previous rows from their maps, pour the fresh ones
	 * through the {@link RecipeMap#addRecipe} funnel (the FROZEN guard + ghost guard + the
	 * incremental index), then invalidate the hash indexes — a re-registration can be
	 * SIZE-NEUTRAL, where the size-drift self-heal never fires and stale buckets would linger.
	 */
	private static void replace(ItemStack aKey, List<Row> aRows) {
		RecipeMap tMelter = GT6RecipeMaps.MELTER, tSmelter = GT6RecipeMaps.SMELTER, tMortar = GT6RecipeMaps.MORTAR;
		if (tMelter == null || tSmelter == null || tMortar == null) return; // no map generation — nothing to pour into (a broken lifecycle)
		boolean tReopened = GT6RecipeMaps.reopenWindow();
		try {
			List<Row> tPrevious = sRows.put(new OM.StackKey(aKey.getItem(), aKey.getDamageValue()), aRows);
			if (tPrevious != null) for (Row tRow : tPrevious) tRow.map().mRecipeList.remove(tRow.recipe());
			for (Row tRow : aRows) tRow.map().addRecipe(tRow.recipe());
			if (tPrevious != null && !tPrevious.isEmpty()) { // the P1 fix: a removed row must never be served from a stale bucket
				tMelter.invalidateIndex(); tSmelter.invalidateIndex(); tMortar.invalidateIndex();
			}
		} finally {
			if (tReopened) GT6RecipeMaps.freeze(); // a crashed pour must not leave the gate open
		}
	}

	private static void mortarRow(ItemStack aStack, OreDictMaterialStack aMaterial, List<Row> aRows) {
		ItemStack tDust = dust(aMaterial.mMaterial, aMaterial.mAmount);
		if (tDust == null) return; // the ghost-guard shape: a null-output row is no row (upstream ST.array(null) trimmed)
		aRows.add(new Row(GT6RecipeMaps.MORTAR, new Recipe(T, new ItemStack[] {aStack}, new ItemStack[] {tDust}, new FluidStack[0], new FluidStack[0], 16, 16, 0)));
	}

	/** :252/:257 — the melt projection ({@code OM.stack(UT.Code.units(...)).addToList}). */
	private static void meltTarget(OreDictMaterialStack aMaterial, List<OreDictMaterialStack> aList) {
		OreDictMaterialStack tTarget = aMaterial.mMaterial.mTargetSmelting;
		if (tTarget != null && tTarget.mAmount > 0 && aMaterial.mMaterial.contains(TD.Processing.MELTING) && !aMaterial.mMaterial.contains(TD.Processing.BLACKLISTED_SMELTER))
			aList.add(new OreDictMaterialStack(tTarget.mMaterial, UT.Code.units(aMaterial.mAmount, CS.U, tTarget.mAmount, F)));
	}

	/** The {@code mMaterial.liquid(amount, F)} isomorph: the seam's fluid scaled by the upstream
	 * units conversion against the 144 L-per-material-unit convention (upstream
	 * OreDictMaterial.liquid :1307-1312 with the FL.make("…molten", 144) template; the
	 * FluidBridge L_PER_MOLTEN_UNIT + bindInt faces). A sub-unit remainder floors to 0 and the
	 * caller's positive-amount gate (:288) rejects — upstream floor behavior verbatim. */
	@Nullable
	private static FluidStack molten(OreDictMaterial aMaterial, long aAmount) {
		Fluid tFluid = sMoltenFluid.apply(aMaterial);
		if (tFluid == null) return null;
		return new FluidStack(tFluid, FluidTankGT.bindInt(UT.Code.units(aAmount, aMaterial.mLiquidUnit, FluidBridge.L_PER_MOLTEN_UNIT, F)));
	}

	// ------------------------------------------------------------------ the small helpers (each names its upstream home)

	/** Upstream ST.ingredable collapsed to the port-live arms — see the class javadoc. */
	private static boolean ingredable(ItemStack aStack) {
		return !aStack.isEmpty() && !aStack.getItem().hasCraftingRemainingItem(aStack);
	}

	/** Upstream OM.weight(Iterable) (OM.java:324-330): the summed material weight, NULL skipped. */
	private static double weight(List<OreDictMaterialStack> aList) {
		double rWeight = 0;
		for (OreDictMaterialStack tStack : aList) if (tStack != null && tStack.mMaterial != MT.NULL) rWeight += tStack.weight();
		return rWeight;
	}

	/** :290 FL.Lava.is isomorph over the vanilla lava pair (the only lava identity both legs share). */
	private static boolean isLava(FluidStack aFluid) {
		return aFluid.getFluid().isSame(Fluids.LAVA) || aFluid.getFluid().isSame(Fluids.FLOWING_LAVA);
	}

	/** The fluid-temperature arm (:290/:291 {@code tFluid.getFluid().getTemperature()}) with the
	 * one-time environment probe (the OM.carriesFluid shape): an untransformed JVM parks the
	 * arm and MIN_VALUE lets the {@code max()} ride the melting point alone — see the class
	 * javadoc for why that equals the live self-molten value. */
	private static boolean sFluidTypeFaceLive = T;

	private static long fluidTemperature(FluidStack aFluid) {
		if (!sFluidTypeFaceLive) return Long.MIN_VALUE;
		try {
			return aFluid.getFluid().getFluidType().getTemperature();
		} catch (Throwable t) {
			sFluidTypeFaceLive = F;
			return Long.MIN_VALUE;
		}
	}

	/** Upstream OM.dust (OM.java:459-465), the biggest-fitting-dust ladder, verbatim conditions;
	 * {@code OP.<prefix>.mat(aMaterial, aCount)} reads through {@link #sPrefixItem} with the
	 * upstream null-drop ({@code if (rStack != null)}), bindStack is the upstream UT.java:1568
	 * clamp (no port UT.Code home). */
	@Nullable
	private static ItemStack dust(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount < CS.U72 || aMaterial == null) return null;
		ItemStack rStack;
		if (aMaterialAmount >= CS.U * 72) {rStack = prefixed(OP.blockDust, aMaterial, bindStack(aMaterialAmount / (CS.U * 9))); if (rStack != null) return rStack;}
		if (aMaterialAmount >= CS.U )  if (aMaterialAmount >= CS.U * 16 || (( aMaterialAmount % CS.U  == 0                        ))) {rStack = prefixed(OP.dust     , aMaterial, bindStack( aMaterialAmount       / CS.U  )); if (rStack != null) return rStack;}
		if (aMaterialAmount >= CS.U4)  if (aMaterialAmount >= CS.U *  8 || (( aMaterialAmount % CS.U4 <= aMaterialAmount % CS.U9 ))) {rStack = prefixed(OP.dustSmall, aMaterial, bindStack((aMaterialAmount * 4) / CS.U  )); if (rStack != null) return rStack;}
		if (aMaterialAmount >= CS.U9)  if (aMaterialAmount >= CS.U      || (( aMaterialAmount % CS.U9 <= aMaterialAmount % CS.U72))) {rStack = prefixed(OP.dustTiny , aMaterial, bindStack((aMaterialAmount * 9) / CS.U  )); if (rStack != null) return rStack;}
		return prefixed(OP.dustDiv72, aMaterial, bindStack((aMaterialAmount * 72) / CS.U));
	}

	@Nullable
	private static ItemStack prefixed(OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aCount) {
		Item tItem = sPrefixItem.apply(aPrefix, aMaterial);
		return tItem == null ? null : new ItemStack(tItem, (int)aCount);
	}

	private static long bindStack(long aCount) {
		return Math.max(1, Math.min(64, aCount));
	}

	//? if forge {
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		net.minecraftforge.registries.RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isPresent() ? null : tHandle.get();
	}
	//?} else {
	/*private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isBound() ? null : tHandle.get();
	}
	*///?}

	// ------------------------------------------------------------------ the audit/test faces

	/** The rows this class currently holds for a stack in a map (the acceptance/audit read). */
	public static List<Recipe> rows(RecipeMap aMap, ItemStack aStack) {
		List<Row> tRows = sRows.get(new OM.StackKey(aStack.getItem(), aStack.getDamageValue()));
		if (tRows == null) return List.of();
		List<Recipe> rList = new ArrayList<>(1);
		for (Row tRow : tRows) if (tRow.map() == aMap) rList.add(tRow.recipe());
		return rList;
	}

	/** Test/lifecycle seam: clears the ledger so a fresh generation starts empty. */
	public static void resetForTest() {
		sRows.clear();
	}
}
